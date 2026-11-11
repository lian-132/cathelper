package com.cathelperawa.owo.app.ai;

import android.content.Context;
import android.util.Base64;
import com.cathelperawa.owo.app.ai.AiStore;
import com.cathelperawa.owo.app.api.CatApi;
import com.cathelperawa.owo.app.api.EventApi;
import com.cathelperawa.owo.app.api.IdentityApi;
import com.cathelperawa.owo.app.api.NetApi;
import com.cathelperawa.owo.app.api.TaskApi;
import com.cathelperawa.owo.app.bridge.ContactApi;
import com.cathelperawa.owo.app.bridge.QQEnv;
import com.cathelperawa.owo.app.bridge.SilkDecode;
import com.cathelperawa.owo.app.bridge.VoiceAsr;
import com.cathelperawa.owo.app.bridge.VoiceStt;
import com.cathelperawa.owo.app.bridge.VoiceTextCache;
import com.cathelperawa.owo.app.core.HookLog;
import com.cathelperawa.owo.app.protocol.ImageGrabber;
import com.cathelperawa.owo.app.protocol.MsgCache;
import com.cathelperawa.owo.app.protocol.PttGrabber;
import com.cathelperawa.owo.app.protocol.PttIndex;
import com.cathelperawa.owo.app.protocol.RKeyHolder;
import com.cathelperawa.owo.app.util.Refl;
import java.io.File;
import java.io.FileOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONObject;

public final class AiEngine {
    private static final long HOUR_MS = 3600000;
    private static ThreadPoolExecutor POOL = null;
    public static final String TAG_VOICE = "⟦语音⟧";
    private static final Map<String, Long> LAST = new HashMap();
    private static final Map<String, String> LAST_UID = new HashMap();
    private static final Map<String, long[]> HOUR = new HashMap();
    private static final Map<String, Long> PROACT_LAST = new HashMap();
    private static final Map<String, Object[]> PENDING = new HashMap();
    
    private static final Map<String, Long> AFF_LAST = new HashMap();
    private static final Pattern P_EMOJI_BRACKET = Pattern.compile("[\\[【［]\\s*表情(?:包)?\\s*[:：]\\s*([^\\]】］\\n]{1,12})\\s*[\\]】］]");
    private static final Pattern P_EMOJI_NAMED = Pattern.compile("[（(]\\s*[^（()）\\n]{0,14}?表情(?:包)?[^（()）\\n:：]{0,10}?[:：]\\s*([^（()）\\n]{1,12}?)\\s*[）)]");
    private static final Pattern P_EMOJI_NAKED = Pattern.compile("[（(]\\s*[^（()）\\n]{0,14}?表情(?:包)?[^（()）\\n]{0,10}?[）)]");

    private AiEngine() {
    }

    private static boolean askedEmoji(String text) {
        if (text == null) return false;
        return text.contains("发个表情") || text.contains("来个表情")
                || text.contains("发表情") || text.contains("表情包")
                || text.contains("发张图") || text.contains("发个图")
                || text.contains("斗图");
    }

    private static boolean askingAboutEmoji(String text) {
        if (text == null || text.length() == 0) return false;
        boolean aboutPic = text.contains("表情") || text.contains("图") || text.contains("猫")
                || text.contains("这张") || text.contains("那张");
        if (!aboutPic) return false;
        return text.contains("刚才") || text.contains("刚刚") || text.contains("上一张")
                || text.contains("那个") || text.contains("这个") || text.contains("一样")
                || text.contains("长什么样") || text.contains("什么表情") || text.contains("看看");
    }

    private static String asrTextOf(EventApi.Message m) {
        if (!AiConfig.pttAsr()) return "";
        try {
            PttGrabber.Source src = PttGrabber.fromRecord(m.raw);
            if (src == null || (src.raw == null && "none".equals(src.kind))) {
                AiDebug.log("  · 语音: 连语音源都没找到");
                return "";
            }

            try {
                PttIndex.put(m.msgId, m.msgSeq, src.path, src.md5, src.fid);
                AiDebug.log("  · PttIndex 记录 msgId=" + m.msgId + " seq=" + m.msgSeq
                        + " md5=" + src.md5 + " 共 " + PttIndex.size() + " 条");
            } catch (Throwable ignore) {
            }

            String key = cacheKey(src);
            String cached = VoiceTextCache.get(key);
            if (cached.length() > 0) {
                AiDebug.log("  · 语音: 命中本地文字缓存「" + cached + "」");
                return cached;
            }

            byte[] audio = null;
            for (int attempt = 1; attempt <= 3; attempt++) {
                audio = PttGrabber.bytes(src);
                if (audio != null && audio.length > 64) break;
                AiDebug.log("  · 语音: 第 " + attempt + " 次取音频失败，等一下再试");
                try { Thread.sleep(600); } catch (Throwable ignore) {}
            }
            if (audio == null || audio.length <= 64) {
                AiDebug.log("  · 语音: 始终拿不到音频字节");
                return "";
            }
            AiDebug.log("  · 语音: 拿到音频 " + (audio.length / 1024) + "KB");

            File pcmFile = buildVadPcmFrom(audio);
            if (pcmFile == null) {
                AiDebug.log("  · 语音: PCM 生成失败");
                return "";
            }

            String peerUin = "";
            try { peerUin = IdentityApi.uinFromUid(m.peerUid); } catch (Throwable ignore) {}
            if (peerUin == null || peerUin.length() == 0) peerUin = m.peerUid;
            String text = VoiceStt.toText(QQEnv.appContext(), pcmFile.getAbsolutePath(),
                    m.group ? 2 : 1, peerUin, 12000L);
            if (text != null && text.length() > 0) {
                VoiceTextCache.put(key, text);
                AiDebug.log("  · 语音: 已写入本地文字缓存（共 " + VoiceTextCache.size() + " 条）");
                return text;
            }
            AiDebug.log("  · 语音: TIM 通道没有结果");
            return "";
        } catch (Throwable t) {
            AiDebug.log("  · 语音: 本地识别异常 " + t);
            return "";
        }
    }

    private static File buildVadPcmFrom(byte[] audio) {
        try {
            android.content.Context ctx = QQEnv.appContext();
            Object[] dec = SilkDecode.anyToPcm(ctx, audio);
            if (dec == null) return null;
            byte[] pcm = VoiceAsr.to16kMono((byte[]) dec[0], (Integer) dec[1], (Integer) dec[2]);
            if (pcm == null || pcm.length < 320) return null;
            File f = new File(ctx.getCacheDir(), "vad_" + System.currentTimeMillis() + ".pcm");
            java.io.FileOutputStream fo = new java.io.FileOutputStream(f);
            fo.write(pcm);
            fo.close();
            AiDebug.log("  · 语音: VAD 用 PCM " + (pcm.length / 1024) + "KB → " + f.getName());
            return f;
        } catch (Throwable t) {
            AiDebug.log("  · 语音: 生成 PCM 失败 " + t);
            return null;
        }
    }

    private static File buildVadPcm(PttGrabber.Source source) {
        try {
            byte[] bytes = PttGrabber.bytes(source);
            if (bytes != null && bytes.length >= 64) {
                Context appContext = QQEnv.appContext();
                Object[] anyToPcm = SilkDecode.anyToPcm(appContext, bytes);
                if (anyToPcm == null) {
                    AiDebug.log("  · 语音: PCM 解码失败");
                    return null;
                }
                byte[] bArr = VoiceAsr.to16kMono((byte[]) anyToPcm[0], ((Integer) anyToPcm[1]).intValue(), ((Integer) anyToPcm[2]).intValue());
                if (bArr != null && bArr.length >= 320) {
                    File file = new File(appContext.getCacheDir(), "vad_" + System.currentTimeMillis() + ".pcm");
                    FileOutputStream fileOutputStream = new FileOutputStream(file);
                    fileOutputStream.write(bArr);
                    fileOutputStream.close();
                    AiDebug.log("  · 语音: 已生成 VAD 用 PCM " + (bArr.length / 1024) + "KB → " + file.getName());
                    return file;
                }
                AiDebug.log("  · 语音: PCM 太短");
                return null;
            }
            AiDebug.log("  · 语音: 取不到音频字节");
            return null;
        } catch (Throwable th) {
            AiDebug.log("  · 语音: 生成 PCM 失败 " + th);
            return null;
        }
    }

    private static String cacheKey(PttGrabber.Source src) {
        if (src == null) return "";
        if (src.md5 != null && src.md5.length() >= 8) return src.md5.toLowerCase();
        if (src.fid != null && src.fid.length() >= 8) return src.fid;
        if (src.path == null) return "";
        int slash = src.path.lastIndexOf('/');
        return slash > 0 ? src.path.substring(slash + 1) : src.path;
    }

    private static String callModel(AiProvider aiProvider, JSONArray jSONArray) {
        String result;
        try {
            JSONObject jSONObject = new JSONObject();
            jSONObject.put("model", aiProvider.model);
            jSONObject.put("messages", jSONArray);
            jSONObject.put("temperature", aiProvider.temperature);
            jSONObject.put("max_tokens", aiProvider.maxTokens);
            JSONObject jSONObject2 = new JSONObject();
            jSONObject2.put("Authorization", "Bearer " + aiProvider.apiKey);
            jSONObject2.put("Content-Type", "application/json");
            String[] chatUrls = chatUrls(aiProvider.baseUrl);
            NetApi.Result result2 = null;

            
            for (String str : chatUrls) {
                result2 = NetApi.postSync(str, jSONObject2.toString(), jSONObject.toString(), 45000);
                if (result2 == null) continue;
                if (result2.is2xx()) break;
                if (result2.code != 404 && result2.code != 405) break;
            }
            if (result2 != null && result2.code >= 400 && result2.code < 500) {
                try {
                    JSONObject jSONObject3 = new JSONObject(jSONObject.toString());
                    JSONArray optJSONArray = jSONObject3.optJSONArray("messages");
                    if (optJSONArray != null) {
                        for (int i = 0; i < optJSONArray.length(); i++) {
                            JSONObject optJSONObject = optJSONArray.optJSONObject(i);
                            if (optJSONObject != null) {
                                Object opt = optJSONObject.opt("content");
                                if (opt instanceof JSONArray) {
                                    StringBuilder sb = new StringBuilder();
                                    boolean hasImg = false;
                                    boolean hasAudio = false;
                                    JSONArray jSONArray2 = (JSONArray) opt;
                                    for (int i2 = 0; i2 < jSONArray2.length(); i2++) {
                                        JSONObject optJSONObject2 = jSONArray2.optJSONObject(i2);
                                        if (optJSONObject2 == null) continue;
                                        String tp = optJSONObject2.optString("type");
                                        if ("text".equals(tp)) {
                                            sb.append(optJSONObject2.optString("text", ""));
                                        } else if ("image_url".equals(tp)) {
                                            hasImg = true;
                                        } else if (tp.contains("audio")) {
                                            hasAudio = true;
                                        }
                                    }
                                    
                                    String note = "";
                                    if (hasImg) {
                                        note = "\n（对方发来了一张图片，但当前模型看不了图。"
                                                + "别装作看到了，也别猜内容，就说图没显示出来、让他说说拍了啥）";
                                    } else if (hasAudio) {
                                        note = "\n（对方发来的语音这版模型听不了，别装作听懂了，"
                                                + "直接说没听清、让他再说一遍或打字）";
                                    }
                                    String base = sb.length() > 0 ? sb.toString() : "";
                                    optJSONObject.put("content", (base + note).trim());
                                }
                            }
                        }
                    }
                    HookLog.w("AI 带图/带音请求被拒(" + result2.code + ")，降级纯文本重试");
                    AiDebug.log("  · 多模态被拒(" + result2.code + ")，降级纯文本：" + jSONObject3.toString().length() + " 字节");
                    for (String str2 : chatUrls) {
                        result2 = NetApi.postSync(str2, jSONObject2.toString(), jSONObject3.toString(), 45000);
                        if (result2.is2xx()) {
                            break;
                        }
                        if (result2.code != 404 && result2.code != 405) {
                            break;
                        }
                    }
                } catch (Throwable th) {
                }
            }
            if (result2 != null && !result2.is2xx()) {
                AiDebug.log("  ✗ 模型请求失败 " + result2.code + " "
                        + (result2.body == null ? "" : result2.body.substring(0,
                        Math.min(160, result2.body.length()))));
            }
            if (result2 != null && result2.is2xx()) {
                
                try { AiUsage.recordBody(aiProvider.model, result2.body); } catch (Throwable ignore) {}
                String parseContent = parseContent(result2.body);
                AiDebug.log("  ✓ 模型返回 " + (parseContent != null ? parseContent.length() + " 字" : "null"));
                return parseContent;
            }
            StringBuilder append = new StringBuilder().append("  ✗ 模型请求失败: ");
            String failText = result2 == null ? "null" : result2.toString();
            AiDebug.log(append.append(failText).toString());
            return null;
        } catch (Throwable th2) {
            HookLog.e("AI 调模型异常", th2);
            return null;
        }
    }

    

    
    private static JSONArray toolsJson() {
        JSONArray tools = new JSONArray();
        try {
            JSONObject t = new JSONObject();
            t.put("type", "function");
            JSONObject fn = new JSONObject();
            fn.put("name", "web_search");
            fn.put("description",
                    "联网搜索。只在需要实时信息、新闻、价格、天气、最新事件，"
                            + "或者你不确定、怕记错的时候用。闲聊、情绪对话、骂人、"
                            + "你本来就知道的常识，不要调用。");
            JSONObject params = new JSONObject();
            params.put("type", "object");
            JSONObject props = new JSONObject();
            JSONObject q = new JSONObject();
            q.put("type", "string");
            q.put("description", "搜索关键词，尽量具体（带上时间、地名、完整名称）");
            props.put("query", q);
            params.put("properties", props);
            JSONArray req = new JSONArray();
            req.put("query");
            params.put("required", req);
            fn.put("parameters", params);
            t.put("function", fn);
            tools.put(t);
        } catch (Throwable ignore) {
        }

        if (AiParse.enabled()) {
            try {
                tools.put(toolPlayMusic());
                tools.put(toolParseVideo());
                tools.put(toolCheckLogin());
            } catch (Throwable ignore) {
            }
        }

        if (AiScript.enabled()) {
            try {
                JSONArray st = AiScript.tools();
                for (int i = 0; i < st.length(); i++) tools.put(st.get(i));
            } catch (Throwable ignore) {
            }
        }

        
        if (scriptToolsOn()) {
            try {
                JSONArray st2 = com.cathelperawa.owo.app.script.ScriptToolRegistry.toolsJson();
                for (int i = 0; i < st2.length(); i++) tools.put(st2.get(i));
            } catch (Throwable ignore) {
            }
        }
        return tools;
    }

    static boolean scriptToolsOn() {
        try {
            return com.cathelperawa.owo.app.core.ConfigStore.getBool("ai_script_tool");
        } catch (Throwable t) {
            return true;
        }
    }

    private static JSONObject toolCheckLogin() throws Exception {
        JSONObject t = new JSONObject();
        t.put("type", "function");
        JSONObject fn = new JSONObject();
        fn.put("name", "check_login");
        fn.put("description",
                "查网易云音乐 / B 站的登录状态。\n"
                        + "【什么时候用】用户问「你登录了吗」「能不能放 VIP 歌」「B 站登录没」；"
                        + "或者发歌/解析失败，怀疑是没登录导致的。\n"
                        + "【什么时候别用】正常聊天、用户没提到登录、你自己也知道没登录。\n"
                        + "【注意】只说「已登录 / 没登录」。不要报账号、昵称、Cookie 内容、"
                        + "字符数这些东西，也不要猜用户用的是哪个号。");
        JSONObject props = new JSONObject();
        JSONObject p1 = new JSONObject();
        p1.put("type", "string");
        p1.put("enum", new JSONArray().put("netease").put("bilibili"));
        p1.put("description", "netease=网易云音乐；bilibili=B 站");
        props.put("platform", p1);
        JSONObject params = new JSONObject();
        params.put("type", "object");
        params.put("properties", props);
        params.put("required", new JSONArray().put("platform"));
        fn.put("parameters", params);
        t.put("function", fn);
        return t;
    }

    private static JSONObject toolPlayMusic() throws Exception {
        JSONObject t = new JSONObject();
        t.put("type", "function");
        JSONObject fn = new JSONObject();
        fn.put("name", "play_music");
        fn.put("description",
                "搜歌并发到当前聊天。\n"
                        + "【什么时候用】用户明确想听：放首歌 / 来首周杰伦 / 唱个晴天 / 这首你会吗（想听） / "
                        + "刚才那首歌叫什么（只要名字，别调用）。\n"
                        + "【什么时候别用】只是聊天聊到歌名、在跟别人说话、没让你放、你已经放过一首了对方没再要。\n"
                        + "【拿不准怎么办】可以先问一句「要我放出来吗」，别默认就发。发歌会占几秒到十几秒，别乱来。\n"
                        + "默认 voice（语音条）；用户明确说要文件/卡片才换 mode。\n"
                        + "VIP 歌要账号登录才拿得到完整版；没登录只能试听，别硬说放不出来。");
        JSONObject props = new JSONObject();
        JSONObject q = new JSONObject();
        q.put("type", "string");
        q.put("description", "歌名，最好带歌手，例如「晴天 周杰伦」");
        props.put("query", q);
        JSONObject m = new JSONObject();
        m.put("type", "string");
        m.put("enum", new JSONArray().put("card").put("voice").put("file").put("image"));
        m.put("description", "card=音乐卡片（默认，带播放按钮，点了能播）；voice=语音条；file=mp3 文件；image=图片卡片");
        props.put("mode", m);
        JSONObject params = new JSONObject();
        params.put("type", "object");
        params.put("properties", props);
        params.put("required", new JSONArray().put("query"));
        fn.put("parameters", params);
        t.put("function", fn);
        return t;
    }

    private static JSONObject toolParseVideo() throws Exception {
        JSONObject t = new JSONObject();
        t.put("type", "function");
        JSONObject fn = new JSONObject();
        fn.put("name", "parse_video");
        fn.put("description",
                "解析视频链接并发到当前聊天（只支持 B站 / b23.tv / BV 号）。\n"
                        + "【什么时候用】用户把链接丢给你并且意思是让你处理：发出来 / 下载 / 听听里面说了什么 / "
                        + "这视频讲的啥（想让你看内容）。\n"
                        + "【什么时候别用】只是路过分享、在跟别人聊、你已经发过一次同一个链接、"
                        + "或者在吐槽链接里的内容而不是要你下载。\n"
                        + "【拿不准怎么办】先问一句「要我把这个发出来吗」，别直接下几十 MB。\n"
                        + "mode 自己判断：用户想「看」→ video；想「听」或要语音 → voice。"
                        + "抖音/快手/小红书解析不了，直接说做不到。");
        JSONObject props = new JSONObject();
        JSONObject u = new JSONObject();
        u.put("type", "string");
        u.put("description", "视频链接或 BV 号");
        props.put("url", u);
        JSONObject m = new JSONObject();
        m.put("type", "string");
        m.put("enum", new JSONArray().put("video").put("voice"));
        m.put("description", "video=发视频文件；voice=只把音频发成语音条");
        props.put("mode", m);
        JSONObject params = new JSONObject();
        params.put("type", "object");
        params.put("properties", props);
        params.put("required", new JSONArray().put("url"));
        fn.put("parameters", params);
        t.put("function", fn);
        return t;
    }

    private static String callWithTools(AiProvider p, JSONArray msgs,
                                        int chatType, String peerUid,
                                        String userText, String senderUid) {

        
        AiUsage.setSource("reply");
        JSONObject msg = callModelJson(p, msgs, toolsJson());
        AiUsage.clearSource();
        if (msg == null) {
            AiDebug.log("  · 工具: 原始请求失败，退回普通请求");
            return callModel(p, msgs);
        }
        boolean didMusic = false;
        for (int round = 0; round < 2; round++) {
            JSONArray calls = msg.optJSONArray("tool_calls");
            if (calls == null || calls.length() == 0) break;
            try {
                JSONObject asst = new JSONObject();
                asst.put("role", "assistant");
                String pre = msg.optString("content", "");
                asst.put("content", pre == null ? "" : pre);
                asst.put("tool_calls", calls);
                msgs.put(asst);

                int total = Math.min(calls.length(), 2);
                for (int i = 0; i < total; i++) {
                    JSONObject call = calls.optJSONObject(i);
                    if (call == null) continue;
                    JSONObject fn = call.optJSONObject("function");
                    String name = fn == null ? "" : fn.optString("name", "");
                    String args = fn == null ? "" : fn.optString("arguments", "");
                    String q = args;
                    String mode = "";
                    try {
                        q = new JSONObject(args).optString("query", args);
                    } catch (Throwable ignore) {
                    }
                    try {
                        JSONObject a = new JSONObject(args);
                        mode = a.optString("mode", "");
                        if (("parse_video".equals(name)) && a.has("url")) {
                            q = a.optString("url", q);
                        }
                        if ("check_login".equals(name)) {
                            q = a.optString("platform", "netease");
                        }
                    } catch (Throwable ignore) {
                    }
                    String result;
                    if ("play_music".equals(name)) {
                        didMusic = true;
                        
                        result = AiParse.enabled()
                                ? AiParse.music(chatType, peerUid, q, mode)
                                : "（发歌功能没开，别假装发了）";
                        AiDebug.log("  · 点歌: 「" + q + "」");
                    } else if ("parse_video".equals(name)) {
                        result = AiParse.enabled()
                                ? AiParse.video(chatType, peerUid, q, mode)
                                : "（解析功能没开，别假装发了）";
                        AiDebug.log("  · 解析: " + (q.length() > 40 ? q.substring(0, 40) : q));
                    } else if ("check_login".equals(name)) {
                        String plat = q == null || q.trim().length() == 0 ? "netease" : q.trim();
                        String label = "netease".equals(plat) ? "网易云音乐"
                                : ("bilibili".equals(plat) ? "B 站" : plat);
                        String ck = null;
                        try {
                            ck = com.cathelperawa.owo.app.cookie.CookieBridge.get(plat);
                        } catch (Throwable ignore) {
                        }
                        if (ck != null && ck.length() > 0) {
                            result = "已登录 " + label;   
                        } else {
                            result = "没登录 " + label
                                    + "（VIP / 高清内容拿不到；让用户到「网页登录 Cookie」登录一次）";
                        }
                        AiDebug.log("  · 查登录: " + plat + " → "
                                + (ck != null && ck.length() > 0 ? "已登录" : "未登录"));
                    } else if ("web_search".equals(name)) {
                        java.util.List<AiSearch.Result> list = AiSearch.search(q);
                        if (list.isEmpty()) {
                            result = "（搜索没成功：" + (AiSearch.lastError.length() == 0
                                    ? "没结果" : AiSearch.lastError)
                                    + "。别编，就照实说没查到）";
                            AiDebug.log("  · 搜索失败: " + AiSearch.lastError);
                        } else {
                            result = AiSearch.promptFor(list);
                            AiDebug.log("  · 搜索: 「" + q + "」命中 " + list.size() + " 条");
                        }
                    } else if ("script_write".equals(name)) {
                        
                        String nm = "", code = "";
                        boolean en = false;
                        try {
                            JSONObject a = new JSONObject(args);
                            nm = a.optString("name", "");
                            code = a.optString("code", "");
                            en = a.optBoolean("enable", false);
                        } catch (Throwable ignore) {
                        }
                        if (!AiScript.enabled()) {
                            result = "（AI 写脚本的功能被关掉了，让用户到「设置 → 脚本」打开开关）";
                        } else if (!AiGuard.allowUid(senderUid, "写脚本")) {
                            result = "（没有权限：只有号主或管理员能让 AI 写脚本）";
                        } else {
                            result = AiScript.write(nm, code, en);
                            AiDebug.log("  · 写脚本: " + nm + " " + code.length() + "B");
                        }
                    } else if ("script_run".equals(name)) {
                        String nm = "";
                        boolean en = true;
                        try {
                            JSONObject a = new JSONObject(args);
                            nm = a.optString("name", "");
                            en = a.optBoolean("enable", true);
                        } catch (Throwable ignore) {
                        }
                        if (!AiScript.enabled()) {
                            result = "（AI 写脚本的功能被关掉了）";
                        } else if (!AiGuard.allowUid(senderUid, "运行脚本")) {
                            result = "（没有权限：只有号主或管理员能启停脚本）";
                        } else {
                            result = AiScript.run(nm, en);
                            AiDebug.log("  · 脚本" + (en ? "启用" : "停用") + ": " + nm);
                        }
                    } else if ("script_list".equals(name)) {
                        result = AiScript.list();
                        AiDebug.log("  · 列脚本");
                    } else {
                        
                        String viaScript = null;
                        if (AiEngine.scriptToolsOn()) {
                            viaScript = com.cathelperawa.owo.app.script.ScriptToolRegistry
                                    .invoke(name, args);
                        }
                        if (viaScript != null) {
                            result = viaScript;
                            AiDebug.log("  · 脚本工具: " + name);
                        } else {
                            result = "（没有这个工具：" + name + "）";
                        }
                    }
                    JSONObject toolMsg = new JSONObject();
                    toolMsg.put("role", "tool");
                    toolMsg.put("tool_call_id", call.optString("id", ""));
                    toolMsg.put("content", result);
                    msgs.put(toolMsg);
                }
                AiUsage.setSource("tool");
                JSONObject next = callModelJson(p, msgs, toolsJson());
                AiUsage.clearSource();
                if (next == null) break;
                msg = next;
                AiDebug.log("  · 工具轮 #" + (round + 2));
            } catch (Throwable t) {
                AiDebug.log("  ✗ 工具循环异常 " + t);
                break;
            }
        }
        String content = msg.optString("content", "");

        

        if (!didMusic && AiParse.enabled() && looksLikeSongRequest(userText)
                && !asksBack(content)) {
            String q = songQuery(userText);
            if (q.length() > 0) {
                AiDebug.log("  · 点歌兜底（模型没调用工具）: 「" + q + "」");
                String r = AiParse.music(chatType, peerUid, q, "card");
                AiDebug.log("  · 点歌兜底结果: " + (r.length() > 60 ? r.substring(0, 60) : r));
            }
        }
        return content;
    }

    private static boolean looksLikeSongRequest(String t) {
        if (t == null) return false;
        String s = t.trim();
        if (s.length() == 0 || s.length() > 40) return false;
        String[] verbs = {"来一首", "来首", "来一个", "放一首", "放首", "放一下",
                "播放", "唱一首", "唱个", "唱一个", "点歌", "点一首", "给我放", "给我来"};
        for (int i = 0; i < verbs.length; i++) {
            if (s.contains(verbs[i])) return true;
        }
        return false;
    }

    private static boolean asksBack(String content) {
        if (content == null) return false;
        String s = content;
        if (s.indexOf('?') >= 0 || s.indexOf('？') >= 0) return true;
        String[] words = {"要不要", "要我", "用不用", "需要我"};
        for (int i = 0; i < words.length; i++) {
            if (s.contains(words[i])) return true;
        }
        return false;
    }

    private static String songQuery(String t) {
        String s = t == null ? "" : t.trim();
        s = s.replaceAll("^(麻烦|请|帮我|给我|能不能|可以)+", "");
        s = s.replaceAll("^(来|放|播放|唱|点)(一|两|几)?(首|个|下|曲)?", "");
        s = s.replaceAll("^(的|那首|这首)", "");
        s = s.replaceAll("(吧|呗|好吗|好不好|可以吗|谢谢|嘛)+$", "");
        s = s.replaceAll("[，。！？!.~～\\s]+$", "");
        s = s.trim();
        if (s.equals("歌") || s.equals("歌曲") || s.equals("音乐")) return "";
        return s;
    }

    private static JSONObject callModelJson(AiProvider p, JSONArray msgs, JSONArray tools) {
        try {
            JSONObject body = new JSONObject();
            body.put("model", p.model);
            body.put("messages", msgs);
            body.put("temperature", p.temperature);
            body.put("max_tokens", p.maxTokens);
            if (tools != null && tools.length() > 0) {
                body.put("tools", tools);
                body.put("tool_choice", "auto");
            }
            JSONObject headers = new JSONObject();
            headers.put("Authorization", "Bearer " + p.apiKey);
            headers.put("Content-Type", "application/json");

            NetApi.Result r = null;
            String[] urls = chatUrls(p.baseUrl);
            for (int i = 0; i < urls.length; i++) {
                r = NetApi.postSync(urls[i], headers.toString(), body.toString(), 45000);
                if (r != null && r.is2xx()) break;
                if (r != null && r.code != 404 && r.code != 405) break;
            }
            if (r == null || !r.is2xx()) {
                AiDebug.log("  · 工具请求失败 " + (r == null ? "null" : ("code=" + r.code)));
                return null;
            }
            JSONObject root = new JSONObject(r.body);
            
            try { AiUsage.record(p.model, root); } catch (Throwable ignore) {}
            JSONArray choices = root.optJSONArray("choices");
            if (choices == null || choices.length() == 0) return null;
            JSONObject c0 = choices.optJSONObject(0);
            if (c0 == null) return null;
            return c0.optJSONObject("message");
        } catch (Throwable t) {
            AiDebug.log("  ✗ 工具请求异常 " + t);
            return null;
        }
    }

    private static String[] chatUrls(String base) {
        String b = base == null ? "" : base.trim();
        while (b.endsWith("/")) b = b.substring(0, b.length() - 1);
        if (b.endsWith("/chat/completions")) return new String[]{b};
        if (b.endsWith("/v1") || b.endsWith("/v1beta") || b.endsWith("/openai")) {
            return new String[]{b + "/chat/completions"};
        }
        return new String[]{b + "/v1/chat/completions", b + "/chat/completions"};
    }

    private static String cleanReply(String s) {
        if (s == null) return "";
        String t = s;
        
        int cut = t.indexOf("##");
        if (cut > 4) t = t.substring(0, cut);

        t = t.replace("**", "").replace("__", "").replace("`", "");
        t = t.replaceAll("(?m)^\\s*#{1,6}\\s*", "");      
        t = t.replaceAll("(?m)^\\s*[-*]\\s+", "");        
        t = t.replaceAll("(?m)^\\s*\\d+\\.\\s+", "");  
        t = t.replaceAll("\\[([^\\]]*)\\]\\([^)]*\\)", "$1");   
        t = t.replaceAll("[ \\t]+", " ");
        t = t.replaceAll("\\n{2,}", "\n");
        return t.trim();
    }

    private static String cleanUserText(String raw) {
        if (raw == null) return "";
        String text = raw;
        String nick = null;
        String card = null;
        try { nick = QQEnv.currentNickname(); } catch (Throwable ignore) {}
        try { card = IdentityApi.troopMemberName("", ""); } catch (Throwable ignore) {}
        for (String name : new String[]{nick, card}) {
            if (name != null && name.trim().length() > 0) {
                text = text.replace("@" + name, " ");
            }
        }
        
        text = text.replaceAll("\\[atUin=[^\\]]*\\]", " ");
        text = text.replaceAll("@[^\\s，,。.！!？?]+", " ");
        text = text.replaceAll("[ \\t]{2,}", " ");
        return text.trim();
    }

    public static int contextPeers() {
        return AiStore.peerCount();
    }

    private static String crossMemoryOf(EventApi.Message message) {
        if (message.group || !AiConfig.cross()) {
            return "";
        }
        try {
            List<AiStore.Msg> bySpeaker = AiStore.bySpeaker(message.senderUid, message.peerUid, 5);
            if (bySpeaker != null && !bySpeaker.isEmpty()) {
                StringBuilder sb = new StringBuilder("【你记得「" + speakerTagOf(message) + "」以前在群里说过这些（是他本人，能自然接话就用，别生硬复述）】");
                for (int i = 0; i < bySpeaker.size(); i++) {
                    String str = bySpeaker.get(i).content;
                    if (str != null && str.length() != 0) {
                        if (str.startsWith("「")) {
                            int indexOf = str.indexOf("」");
                            if (indexOf > 0 && indexOf < 30) {
                                str = str.substring(indexOf + 1).trim();
                            }
                            if (str.startsWith("：") || str.startsWith(":")) {
                                str = str.substring(1).trim();
                            }
                        }
                        if (str.length() > 80) {
                            str = str.substring(0, 80) + "…";
                        }
                        sb.append("\n· ").append(str);
                    }
                }
                return sb.toString();
            }
            return "";
        } catch (Throwable th) {
            return "";
        }
    }

    private static void diagDownload(String url) {
        HttpURLConnection c = null;
        try {
            c = (HttpURLConnection) new URL(url).openConnection();
            c.setConnectTimeout(8000);
            c.setReadTimeout(10000);
            c.setRequestProperty("User-Agent",
                    "Mozilla/5.0 (Linux; Android 16) AppleWebKit/537.36");
            String shortUrl = url.length() > 110 ? url.substring(0, 110) + "…" : url;
            AiDebug.log("  · 图片直连 " + shortUrl + " → HTTP " + c.getResponseCode());
        } catch (Throwable t) {
            AiDebug.log("  · 图片直连异常 " + t);
        } finally {
            if (c != null) {
                try { c.disconnect(); } catch (Throwable ignore) {}
            }
        }
    }

    private static void dumpFields(Object obj, String tag) {
        if (obj == null) return;
        try {
            for (java.lang.reflect.Field f : obj.getClass().getDeclaredFields()) {
                try {
                    f.setAccessible(true);
                    Object v = f.get(obj);
                    String vs = String.valueOf(v);
                    if (vs.length() > 80) vs = vs.substring(0, 80) + "…";
                    AiDebug.log("    " + tag + "." + f.getName() + " = " + vs);
                } catch (Throwable ignore) {
                }
            }
        } catch (Throwable ignore) {
        }
    }

    private static String snifffmt(byte[] b) {
        try {
            if (b.length > 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8) return "jpg";
            if (b.length > 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P') return "png";
            if (b.length > 4 && b[0] == 'G' && b[1] == 'I' && b[2] == 'F') return "gif";
            if (b.length > 12 && b[0] == 'R' && b[1] == 'I' && b[8] == 'W') return "webp";
            String head = new String(b, 0, Math.min(60, b.length), "UTF-8");
            if (head.startsWith("{") || head.startsWith("<") || head.contains("retcode")) return "badtxt";
        } catch (Throwable ignore) {
        }
        return "bin";
    }

    private static String srcDesc(ImageGrabber.Source s) {
        try {
            if (s == null) return "null";
            StringBuilder sb = new StringBuilder(s.kind);
            if (s.path != null) sb.append(" path=").append(s.path);
            if (s.url != null) sb.append(" url=").append(s.url.length() > 80
                    ? s.url.substring(0, 80) + "…" : s.url);
            return sb.toString();
        } catch (Throwable t) {
            return "?";
        }
    }

    private static AiProvider visionProvider() {
        try {
            String id = AiConfig.visionProviderId();
            if (id != null && id.length() > 0) {
                AiProvider v = AiProvider.find(id);
                if (v != null) return v;
            }
        } catch (Throwable ignore) {
        }
        return AiProvider.current();
    }

    private static String imageDescribe(AiProvider vp, Object record) {
        try {
            ImageGrabber.Source src = ImageGrabber.fromRecord(record);
            if (src == null || "none".equals(src.kind)) return null;
            byte[] raw = ImageGrabber.bytes(src);
            if (raw == null || raw.length < 100) return null;
            if (raw.length > 3 * 1024 * 1024) {
                byte[] small = shrinkImage(raw);
                if (small != null) raw = small;
            }
            String b64 = Base64.encodeToString(raw, 2);

            JSONArray parts = new JSONArray();
            JSONObject t = new JSONObject();
            t.put("type", "text");
            t.put("text", "用一句中文说清这张图里是什么（谁/什么东西/什么动作/上面的字）。"
                    + "不超过 40 字，只回答内容本身，不要任何客套话。");
            parts.put(t);
            JSONObject iu = new JSONObject();
            iu.put("url", "data:image/jpeg;base64," + b64);
            JSONObject wrap = new JSONObject();
            wrap.put("type", "image_url");
            wrap.put("image_url", iu);
            parts.put(wrap);

            JSONObject msg = new JSONObject();
            msg.put("role", "user");
            msg.put("content", parts);
            JSONArray msgs = new JSONArray();
            msgs.put(msg);

            JSONObject body = new JSONObject();
            body.put("model", vp.model);
            body.put("messages", msgs);
            body.put("max_tokens", 1024);
            body.put("temperature", 0.3);

            JSONObject h = new JSONObject();
            h.put("Authorization", "Bearer " + vp.apiKey);
            h.put("Content-Type", "application/json");

            String[] urls = chatUrls(vp.baseUrl);
            NetApi.Result r = null;
            for (int i = 0; i < urls.length; i++) {
                r = NetApi.postSync(urls[i], h.toString(), body.toString(), 45000);
                if (r != null && r.is2xx()) break;
                if (r != null && r.code != 404 && r.code != 405) break;
            }
            if (r == null || !r.is2xx()) {
                AiDebug.log("  ✗ 识图请求失败 " + (r == null ? "null" : (r.code + " " + r.body)));
                return null;
            }
            JSONObject o = new JSONObject(r.body);
            
            try { AiUsage.record(vp == null ? "vision" : vp.model, o); } catch (Throwable ignore) {}
            JSONArray ch = o.optJSONArray("choices");
            if (ch == null || ch.length() == 0) return null;
            JSONObject m0 = ch.optJSONObject(0).optJSONObject("message");
            if (m0 == null) return null;
            String text = m0.optString("content", "");
            text = text.replace("\n", " ").trim();
            if (text.length() > 80) text = text.substring(0, 80) + "…";
            return text.length() == 0 ? null : text;
        } catch (Throwable t) {
            AiDebug.log("  ✗ 识图异常 " + t);
            return null;
        }
    }

    private static byte[] shrinkImage(byte[] raw) {
        try {
            android.graphics.BitmapFactory.Options o = new android.graphics.BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            android.graphics.BitmapFactory.decodeByteArray(raw, 0, raw.length, o);
            if (o.outWidth <= 0 || o.outHeight <= 0) return null;
            int scale = 1;
            int maxSide = Math.max(o.outWidth, o.outHeight);
            while (maxSide / scale > 1024) scale *= 2;
            android.graphics.BitmapFactory.Options o2 = new android.graphics.BitmapFactory.Options();
            o2.inSampleSize = scale;
            android.graphics.Bitmap bm = android.graphics.BitmapFactory
                    .decodeByteArray(raw, 0, raw.length, o2);
            if (bm == null) return null;
            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
            bm.compress(android.graphics.Bitmap.CompressFormat.JPEG, 82, bos);
            bm.recycle();
            return bos.toByteArray();
        } catch (Throwable t) {
            return null;
        }
    }

    private static void dumpPic(Object obj) {
        try {
            Object field = Refl.field(obj, "elements");
            if (field instanceof List) {
                List list = (List) field;
                for (int i = 0; i < list.size(); i++) {
                    Object obj2 = list.get(i);
                    if (obj2 != null) {
                        Object field2 = Refl.field(obj2, "picElement");
                        if (field2 == null) {
                            Object field3 = Refl.field(obj2, "marketFaceElement");
                            if (field3 != null) {
                                AiDebug.log("  DUMP marketFace 字段:");
                                dumpFields(field3, "    ");
                            }
                        } else {
                            AiDebug.log("  DUMP picElement 字段 (" + field2.getClass().getName() + "):");
                            dumpFields(field2, "    ");
                        }
                    }
                }
            }
        } catch (Throwable th) {
            AiDebug.log("  DUMP 失败 " + th);
        }
    }

    private static String emojiDataUrl(String str) {
        byte[] modelBytes;
        try {
            File dir = AiEmoji.dir();
            if (dir != null && str != null) {
                File file = new File(dir, str);
                if (file.isFile() && (modelBytes = AiEmoji.modelBytes(file)) != null && modelBytes.length >= 100) {
                    return "data:image/jpeg;base64," + Base64.encodeToString(modelBytes, 2);
                }
                return null;
            }
            return null;
        } catch (Throwable th) {
            return null;
        }
    }

    private static String envLine(EventApi.Message message) {
        String str;
        String str2;
        try {
            StringBuilder sb = new StringBuilder("【环境信息（你是知道这些的，不要说「看不到」「不知道」；回答时也不要复述）】");

            java.util.Date nowD = new Date();
            if (AiConfig.cacheOpt()) {
                java.util.Calendar cal = java.util.Calendar.getInstance();
                cal.setTime(nowD);
                cal.set(java.util.Calendar.MINUTE, cal.get(java.util.Calendar.MINUTE) / 10 * 10);
                cal.set(java.util.Calendar.SECOND, 0);
                cal.set(java.util.Calendar.MILLISECOND, 0);
                nowD = cal.getTime();
            }
            sb.append("现在 ").append(new SimpleDateFormat("MM月dd日 HH:mm EEEE", Locale.CHINA).format(nowD));
            sb.append("；");
            try {
                str = IdentityApi.currentNick();
            } catch (Throwable th) {
                str = "";
            }
            if (str == null || str.trim().length() == 0) {
                try {
                    str = QQEnv.currentNickname();
                } catch (Throwable th2) {
                }
            }
            if (str != null && str.trim().length() > 0) {
                sb.append("你自己的昵称是「").append(str.trim()).append("」；");
            }
            if (message.group) {
                sb.append("这是群聊（每条消息前面的「名字(QQ号)」是说话人；同名不同号是不同的人，同一个号才是同一个人），群号 ").append(message.peerUid);
                try {
                    String troopName = IdentityApi.troopName(message.peerUid);
                    if (troopName != null && troopName.length() > 0) {
                        sb.append("（群名「").append(troopName).append("」）");
                    }
                } catch (Throwable th3) {
                }
            } else {
                sb.append("这是私聊");
            }
            sb.append("；");
            sb.append("对面是「").append(peerNameOf(message)).append("」");
            try {
                str2 = IdentityApi.uinFromUid(message.senderUid);
            } catch (Throwable th4) {
                str2 = null;
            }
            if (str2 != null && str2.length() > 0) {
                sb.append("（QQ ").append(str2).append("）");
            }
            return sb.toString();
        } catch (Throwable th5) {
            return "";
        }
    }

    private static boolean atOthers(EventApi.Message m) {
        try {
            Object els = Refl.field(m.raw, "elements");
            if (!(els instanceof List)) return false;
            String me = null;
            try {
                me = IdentityApi.currentUid();
            } catch (Throwable ignore) {
            }
            List<?> list = (List<?>) els;
            for (int i = 0; i < list.size(); i++) {
                Object te = Refl.field(list.get(i), "textElement");
                if (te == null) continue;
                Object atUid = Refl.field(te, "atNtUid");
                String uid = atUid == null ? "" : String.valueOf(atUid);
                if (uid.length() == 0 || "0".equals(uid)) continue;
                if (me == null || me.length() == 0) return true;
                if (!me.equals(uid)) return true;
            }
        } catch (Throwable ignore) {
        }
        return false;
    }

    private static boolean hasImage(Object record) {
        try {
            ImageGrabber.Source src = ImageGrabber.fromRecord(record);
            return src != null && !"none".equals(src.kind);
        } catch (Throwable ignore) {
            return false;
        }
    }

    private static boolean hasReply(Object record) {
        Object els = null;
        try { els = Refl.field(record, "elements"); } catch (Throwable ignore) {}
        if (!(els instanceof List)) return false;
        for (Object el : (List<?>) els) {
            if (el != null && Refl.field(el, "replyElement") != null) return true;
        }
        return false;
    }

    private static boolean hasTopVoice(Object record) {
        Object els = null;
        try { els = Refl.field(record, "elements"); } catch (Throwable ignore) {}
        if (!(els instanceof List)) return false;
        for (Object el : (List<?>) els) {
            if (el != null && Refl.field(el, "pttElement") != null) return true;
        }
        return false;
    }

    private static boolean hasVoice(Object record) {
        try {
            PttGrabber.Source src = PttGrabber.fromRecord(record);
            if (src == null) return false;
            return src.raw != null || !"none".equals(src.kind);
        } catch (Throwable ignore) {
            return false;
        }
    }

    private static String imageDataUrl(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            ImageGrabber.Source fromRecord = ImageGrabber.fromRecord(obj);
            if (fromRecord == null || "none".equals(fromRecord.kind)) {
                
                AiDebug.log("  · 图片: 元素里没找到图（fromRecord=" + fromRecord + "）");
                dumpPic(obj);
                return null;
            }
            byte[] bArr = null;
            for (int i = 0; i < 2; i++) {
                try {
                    bArr = ImageGrabber.bytes(fromRecord);
                } catch (Throwable th) {
                    AiDebug.log("  · 图片: bytes 异常 " + th);
                }
                if (bArr != null && bArr.length >= 100) {
                    break;
                }
                if (i == 0) {
                    try {
                        Thread.sleep(900L);
                    } catch (Throwable th2) {
                    }
                }
            }
            if (bArr != null && bArr.length >= 100) {
                AiDebug.log("  · 图片: 已读出 " + (bArr.length / 1024) + "KB");

                try {
                    String fmt = snifffmt(bArr);
                    AiDebug.log("  · 图片: 格式=" + fmt + " 来源=" + srcDesc(fromRecord));
                    java.io.File dbg = new java.io.File(
                            com.cathelperawa.owo.app.core.DataDir.account("image"),
                            "ai_last_image." + fmt);
                    java.io.FileOutputStream fo = new java.io.FileOutputStream(dbg);
                    fo.write(bArr);
                    fo.flush();
                    fo.close();
                    AiDebug.log("  · 图片: 已存档 " + dbg.getName());
                } catch (Throwable ignore) {
                }
                if (bArr.length <= 3145728) {
                    return "data:image/jpeg;base64," + Base64.encodeToString(bArr, 2);
                }
                HookLog.w("AI：图片超过 3MB，跳过");
                return null;
            }
            AiDebug.log("  · 图片: bytes=" + (bArr == null ? "null" : Integer.valueOf(bArr.length)) + "（拿不到）");
            AiDebug.log("  · " + RKeyHolder.status());
            if (fromRecord.url != null && fromRecord.url.length() > 0) {
                diagDownload(fromRecord.url);
            }
            dumpPic(obj);
            return null;
        } catch (Throwable th3) {
            return null;
        }
    }

    private static boolean isFollowUp(EventApi.Message m) {
        if (m == null || m.senderUid == null || m.senderUid.length() == 0) return false;
        Long lastAt = LAST.get(m.peerUid);
        if (lastAt == null) return false;
        long elapsed = System.currentTimeMillis() - lastAt;
        if (elapsed <= 0 || elapsed > AiConfig.followUpSec() * 1000L) return false;
        String lastUid = LAST_UID.get(m.peerUid);
        return lastUid != null && lastUid.equals(m.senderUid);
    }

    

    
    
    private static boolean judgeWantsReply(EventApi.Message m, String text) {
        AiProvider p = AiProvider.current();
        if (p == null) return false;

        
        try {
            String lastUid = LAST_UID.get(m.peerUid);
            if (lastUid != null && lastUid.length() > 0
                    && !lastUid.equals(m.senderUid)) {
                AiDebug.log("  · 判定: 换人了（uid 不同）→ 不接");
                return false;
            }
        } catch (Throwable ignore) {
        }

        
        try {
            if (isFollowUp(m)) {
                String t = text == null ? "" : text.trim();
                boolean img = t.length() == 0 || t.startsWith("[图片");
                if (img || t.length() <= 12
                        || t.contains("?") || t.contains("？")) {
                    AiDebug.log("  · 判定: 刚聊过的人发来短句/图 → 直接接");
                    return true;
                }
            }
        } catch (Throwable ignore) {
        }

        try {
            JSONArray msgs = new JSONArray();
            JSONObject sys = new JSONObject();
            sys.put("role", "system");
            
            String lastUid = LAST_UID.get(m.peerUid);
            String partner = "";
            if (lastUid != null && lastUid.length() > 0) {
                try {
                    String uin = com.cathelperawa.owo.app.api.IdentityApi.uinFromUid(lastUid);
                    String nm = null;
                    if (uin != null && uin.length() > 0) {
                        nm = com.cathelperawa.owo.app.api.IdentityApi
                                .troopMemberName(m.peerUid, uin);
                    }
                    if (nm == null || nm.trim().length() == 0) nm = peerNameOf(m);
                    partner = nm + "(" + (uin == null || uin.length() == 0 ? lastUid : uin) + ")";
                } catch (Throwable ignore) {
                }
            }
            String sender = speakerTagOf(m);
            String who = "";
            if (partner.length() > 0) {
                
                who = "【重要】正在跟 AI 对话的人是「" + partner + "」，"
                        + "最新这条的发送者是「" + sender + "」，"
                        + "程序按 uid 判定：就是同一个人（别管名字像不像）。";
            }

            sys.put("content", "你是「要不要接话」的判定器。下面是群聊记录（assistant 是 AI 自己）。\n"
                    + who + "\n"
                    + "回答 YES 的情况：\n"
                    + "1. 最新这条在回应 AI 刚说的话（「真的嘛」「嗯」「然后呢」「你说啥」「怎么这么久不回」）；\n"
                    + "2. 接着跟 AI 聊天、向 AI 提要求或提问；\n"
                    + "3. AI 刚回完这个人，他又发了一句（哪怕很短、只发了张图）。\n"
                    + "回答 NO 的情况（优先级比 YES 高）：\n"
                    + "1. 最新这条 @ 了别人、或者明显是在回复别人（跟第三个人对骂、互相调侃）；\n"
                    + "2. 群友之间在互相聊，明显没在指 AI；\n"
                    + "3. 自言自语、新闻转发、跟 AI 完全无关的话题。\n"
                    + "拿不准就回答 NO —— 抢别人的话比少接一句更尴尬。\n"
                    + "只回答 YES 或 NO，不要任何其它字。");
            msgs.put(sys);

            java.util.List<AiStore.Msg> ctx = AiStore.recent(m.peerUid, 12);
            for (int i = 0; i < ctx.size(); i++) {
                JSONObject o = new JSONObject();
                o.put("role", ctx.get(i).role);
                o.put("content", ctx.get(i).content);
                msgs.put(o);
            }
            JSONObject cur = new JSONObject();
            cur.put("role", "user");
            cur.put("content", "最新一条（" + peerNameOf(m) + "）：" + text + "\n是不是在跟 AI 说？YES / NO");
            msgs.put(cur);

            AiUsage.setSource("judge");
            String r = callModel(p, msgs);
            AiUsage.clearSource();
            if (r == null || r.trim().length() == 0) return false;
            String up = r.trim().toUpperCase();
            if (up.startsWith("NO")) return false;
            if (up.startsWith("YES")) return true;
            if (up.contains("YES")) return true;
            if (up.contains("NO")) return false;
            return false;
        } catch (Throwable t) {
            return false;
        }
    }

    private static String lastEmojiDataUrl(String str) {
        File dir;
        byte[] readFile;
        try {
            String lastSentFile = AiEmoji.lastSentFile(str);
            if (lastSentFile == null || (dir = AiEmoji.dir()) == null) {
                return null;
            }
            File file = new File(dir, lastSentFile);
            if (file.isFile() && (readFile = ImageGrabber.readFile(file.getAbsolutePath())) != null && readFile.length >= 100) {
                if (readFile.length <= 3145728) {
                    return "data:image/jpeg;base64," + Base64.encodeToString(readFile, 2);
                }
                AiDebug.log("  · 追问表情: 图太大（" + (readFile.length / 1024) + "KB），跳过");
                return null;
            }
            return null;
        } catch (Throwable th) {
            return null;
        }
    }

    public static void maybeProactive(final EventApi.Message message) {
        if (message != null) {
            try {
                if (message.recv && !message.self && AiConfig.proactiveOn() && AiConfig.peerAllowed(message.peerUid)) {
                    long currentTimeMillis = System.currentTimeMillis();
                    Map<String, Long> map = PROACT_LAST;
                    Long l = map.get(message.peerUid);
                    if (l == null || currentTimeMillis - l.longValue() >= AiConfig.proactiveGapSec() * 1000) {
                        Long l2 = LAST.get(message.peerUid);
                        if (l2 == null || currentTimeMillis - l2.longValue() >= 20000) {

                            double urgency = computeUrgency(message);
                            int proactivePct = AiConfig.proactivePct();
                            
                            double p = Math.min(urgency, proactivePct / 100.0d);
                            AiDebug.log("  · 主动对话: 紧迫感 " + String.format(java.util.Locale.CHINA, "%.2f", urgency)
                                    + " / 上限 " + proactivePct + "%");
                            if (Math.random() >= p) {
                                return;
                            }
                            map.put(message.peerUid, Long.valueOf(currentTimeMillis));
                            long random = ((long) (Math.random() * 6000.0d)) + 1000;
                            AiDebug.log("  · 主动对话: 概率 " + proactivePct + "% 命中，" + (random / 1000) + "s 后插一句");
                            TaskApi.after(random, new Runnable() { 
                                @Override 
                                public void run() {
                                    try {
                                        AiEngine.proactiveReply(message);
                                    } catch (Throwable th) {
                                        HookLog.e("主动对话失败", th);
                                    }
                                }
                            });
                        }
                    }
                }
            } catch (Throwable th) {
                HookLog.w("主动对话判断异常 " + th);
            }
        }
    }

    private static String normalizeEmojiMarks(String str) {
        if (str == null || str.length() == 0) {
            return str;
        }
        try {
            return P_EMOJI_NAKED.matcher(P_EMOJI_NAMED.matcher(P_EMOJI_BRACKET.matcher(str).replaceAll("[表情:$1]")).replaceAll("[表情:$1]")).replaceAll("").replaceAll("[ \\t]{2,}", " ").replaceAll("\\n{2,}", "\n").trim();
        } catch (Throwable th) {
            return str;
        }
    }

    public static void onMessage(EventApi.Message message) {
        boolean z;
        if (message == null) {
            return;
        }

        try {
            final ChatLog.Line line = ChatLog.record(message);

            
            if (line != null && hasImage(message.raw)) {
                try {
                    com.cathelperawa.owo.app.core.Bg.run(new Runnable() {
                        @Override public void run() {
                            try {
                                if (line.desc == null) {
                                    String d = describeCached(line.raw);
                                    if (d != null) {
                                        line.desc = d;
                                        AiDebug.log("  · 预热识图：" + d);
                                    }
                                }
                            } catch (Throwable ignore) {
                            }
                        }
                    }, "cat-img");
                } catch (Throwable ignore) {
                }
            }
        } catch (Throwable ignore) {
        }
        try {

            

            

            if (AiConfig.adminWakeOn()) {
                String wakeText = cleanUserText(message.text);

                boolean echo = message.self && isRecentlySent(wakeText);
                AiRoute.Hit wakeHit = echo ? null : AiRouteRegistry.match(wakeText);
                boolean isAdminWord = wakeHit != null
                        && AiRouteRegistry.ID_WAKE_ADMIN.equals(wakeHit.route.id);
                boolean isNormWord = wakeHit != null
                        && AiRouteRegistry.ID_WAKE.equals(wakeHit.route.id);
                String wakeArg = (isAdminWord || isNormWord) ? wakeHit.arg : null;
                if (wakeArg != null) {

                    
                    
                    if (!AiGuard.allow(message, "唤醒词")) {
                        AiDebug.log("  → 唤醒词被拒：不是管理员/号主");
                        return;
                    }
                    
                    if (!AiLimit.check(message.senderUid,
                            isAdminWord ? AiRouteRegistry.ID_WAKE_ADMIN : AiRouteRegistry.ID_WAKE)) {
                        AiDebug.log("  → 唤醒被限额拦下");
                        return;
                    }
                    if (!passWakeCooldown(message.peerUid)) {
                        AiDebug.log("  → 唤醒词冷却中（" + AiConfig.wakeCooldownSec() + "s），忽略");
                        return;
                    }
                    String body = wakeArg.trim();
                    if (body.length() == 0) body = "（叫你出来了，随便说点什么）";
                    AiDebug.log("  ✓ 唤醒词命中 → 直接回复：「" + body
                            + "」（" + (isAdminWord ? "管理员特殊词" : "普通词") + "·不看白名单"
                            + "｜总开关 " + (AiConfig.enable() ? "开" : "关") + "，唤醒不受它约束）");
                    submitWake(message, body);
                    return;
                }
            }

            

            if (AiConfig.ttsCmdOn()) {
                AiRoute.Hit ttsHit = AiRouteRegistry.match(cleanUserText(message.text));
                if (ttsHit != null && AiRouteRegistry.ID_TTS.equals(ttsHit.route.id)) {
                    final String say = ttsHit.arg == null ? "" : ttsHit.arg.trim();
                    if (say.length() == 0) {
                        return;
                    }
                    if (!AiGuard.allow(message, "语音合成")) {
                        AiDebug.log("  → 语音合成被拒：不是管理员/号主");
                        return;
                    }
                    if (!AiLimit.check(message.senderUid, AiRouteRegistry.ID_TTS)) {
                        AiDebug.log("  → 语音合成被限额拦下");
                        return;
                    }
                    AiDebug.log("  ✓ 语音合成：「" + say + "」");
                    final EventApi.Message ttsMsg = message;
                    try {
                        com.cathelperawa.owo.app.core.Bg.run(new Runnable() {
                            @Override public void run() {
                                try {

                                    String base = com.cathelperawa.owo.app.api.IoApi.dataDir()
                                            + "/tts_" + System.currentTimeMillis();
                                    java.io.File mp3 = new java.io.File(base + ".mp3");
                                    AiDebug.log("  · TTS 通道：" + TtsEngine.channelName());
                                    if (!TtsEngine.synth(say, mp3)) {
                                        AiDebug.log("  ✗ 语音合成失败（接口和免费通道都没拿到音频）");
                                        return;
                                    }
                                    String slk = com.cathelperawa.owo.app.bridge.VoiceSilk
                                            .audioToSilk(com.cathelperawa.owo.app.bridge.QQEnv.appContext(),
                                                    mp3, base + ".silk", 60);
                                    try { mp3.delete(); } catch (Throwable ignore) {}
                                    if (slk == null) {
                                        AiDebug.log("  ✗ mp3 转 SILK 失败");
                                        return;
                                    }
                                    int dur = com.cathelperawa.owo.app.bridge.VoiceSilk.lastDurationMs;
                                    boolean ok = com.cathelperawa.owo.app.api.MsgApi
                                            .sendPtt(ttsMsg.chatType, ttsMsg.peerUid, slk, dur);
                                    AiDebug.log(ok ? "  ✓ 语音已发送（" + dur + "ms）" : "  ✗ 语音发送失败");
                                } catch (Throwable t) {
                                    AiDebug.log("  ✗ 语音合成异常 " + t);
                                }
                            }
                        }, "cat-tts");
                    } catch (Throwable t) {
                        AiDebug.log("  ✗ 语音任务提交失败 " + t);
                    }
                    return;
                }
            }
            
            if (message.recv) {
                AiRoute.Hit helpHit = AiRouteRegistry.match(cleanUserText(message.text));
                if (helpHit != null && AiRouteRegistry.ID_HELP.equals(helpHit.route.id)) {
                    if (AiGuard.allow(message, "帮助")) {
                        sendTextQuiet(message, helpText());
                        AiDebug.log("  ✓ 帮助命令已回复");
                    }
                    return;
                }
            }
            if (!AiConfig.enable()) {
                AiDebug.log("  → 跳过：总开关 ai_enable 关着");
                return;
            }
            if (!message.self && message.recv) {
                if (!AiConfig.peerAllowed(message.peerUid)) {
                    AiDebug.log("  → 跳过：不在允许名单（ai_targets=" + AiConfig.targetsRaw() + "）");
                    return;
                }

                if (message.group && !message.atMe && atOthers(message)) {
                    AiDebug.log("  → 跳过：这条 @ 的是别人，不接");
                    return;
                }
                boolean hasImage = hasImage(message.raw);
                boolean hasVoice = hasVoice(message.raw);
                if (hasVoice) {
                    AiDebug.log("  · 语音: 收到一条语音消息");
                }
                String cleanUserText = cleanUserText(message.text);

                boolean hasQuote = hasReply(message.raw);
                if (cleanUserText.trim().length() == 0 && !hasImage && !hasVoice && !hasQuote) {
                    AiDebug.log("  → 跳过：剥掉 @ 之后没有内容（原文 " + message.text + "）");
                    return;
                }
                if (message.group) {
                    boolean z2 = true;
                    boolean z3 = AiConfig.atReply() && message.atMe;
                    boolean z4 = AiConfig.keywordReply() && AiConfig.matchKeyword(message.text);
                    if (!z3 && !z4 && AiConfig.selfJudge()) {
                        if (AiConfig.groupAllowed(message.peerUid)) {
                            submitMerged(message, cleanUserText, true);
                            return;
                        } else {
                            AiDebug.log("  → 跳过：这个群不在群白名单");
                            return;
                        }
                    }
                    boolean z5 = AiConfig.followUpOn() && isFollowUp(message);
                    if ((!z3 && !z4 && !z5) || !AiConfig.groupAllowed(message.peerUid)) {
                        z2 = false;
                    }
                    if (z5 && !z3 && !z4) {
                        AiDebug.log("  · 连续对话：对方在接着跟它说话，不用 @");
                    }
                    z = z2;
                } else {
                    z = AiConfig.privateReply();
                }
                if (!z) {
                    AiDebug.log("  → 跳过：触发条件不满足（group=" + message.group + " atMe=" + message.atMe + " private=" + AiConfig.privateReply() + " at=" + AiConfig.atReply() + " kw=" + AiConfig.keywordReply() + "）");
                    return;
                }

                submitMerged(message, cleanUserText, false);
            }
        } catch (Throwable th) {
            HookLog.e("AI onMessage 异常", th);
        }
    }

    private static String parseContent(String body) {
        try {
            JSONArray choices = new JSONObject(body).optJSONArray("choices");
            if (choices == null || choices.length() == 0) return null;
            JSONObject first = choices.optJSONObject(0);
            if (first == null) return null;
            JSONObject msg = first.optJSONObject("message");
            if (msg != null) return msg.optString("content", null);
            return first.optString("text", null);   
        } catch (Throwable ignore) {
            return null;
        }
    }

    
    public static boolean passCooldown(String str) {
        int cooldownSec = AiConfig.cooldownSec();
        if (cooldownSec <= 0) {
            return true;
        }
        Map<String, Long> map = LAST;
        Long l = map.get(str);
        long currentTimeMillis = System.currentTimeMillis();
        if (l != null) {
            long j = cooldownSec;
            if (currentTimeMillis - l.longValue() < j * 1000) {
                long longValue = j - ((currentTimeMillis - l.longValue()) / 1000);
                HookLog.i("AI 冷却中（" + ((currentTimeMillis - l.longValue()) / 1000) + "s < " + cooldownSec + "s）");
                AiDebug.log("  → 跳过：冷却中，还剩 " + Math.max(0L, longValue) + "s");
                return false;
            }
        }
        map.put(str, Long.valueOf(currentTimeMillis));
        return true;
    }

    
    public static boolean passHourly(String str) {
        int hourlyLimit = AiConfig.hourlyLimit();
        if (hourlyLimit <= 0) {
            return true;
        }
        long currentTimeMillis = System.currentTimeMillis();
        Map<String, long[]> map = HOUR;
        long[] jArr = map.get(str);
        if (jArr == null || currentTimeMillis - jArr[1] > HOUR_MS) {
            jArr = new long[]{0, currentTimeMillis};
            map.put(str, jArr);
        }
        long j = jArr[0];
        if (j >= hourlyLimit) {
            HookLog.i("AI 本小时已达上限 " + hourlyLimit);
            return false;
        }
        jArr[0] = j + 1;
        return true;
    }

    

    
    private static final java.util.Map<String, Long> FORCED =
            new java.util.concurrent.ConcurrentHashMap<String, Long>();

    private static final java.util.Map<String, Long> SENT =
            new java.util.concurrent.ConcurrentHashMap<String, Long>();

    private static boolean isForcedPeer(String peer) {
        if (peer == null) return false;
        Long until = FORCED.get(peer);
        if (until == null) return false;
        if (System.currentTimeMillis() > until.longValue()) {
            FORCED.remove(peer);
            return false;
        }
        return true;
    }

    private static void markSent(String text) {
        if (text == null) return;
        String k = text.trim();
        if (k.length() == 0) return;
        long now = System.currentTimeMillis();
        SENT.put(k.length() > 60 ? k.substring(0, 60) : k, Long.valueOf(now));
        if (SENT.size() > 60) {                      
            java.util.Iterator<java.util.Map.Entry<String, Long>> it = SENT.entrySet().iterator();
            while (it.hasNext()) {
                if (now - it.next().getValue().longValue() > 180000L) it.remove();
            }
        }
    }

    
    private static String describeCached(Object record) {
        try {
            String c = ImageDescCache.get(record);
            if (c != null && c.length() > 0) return c;
        } catch (Throwable ignore) {
        }
        String d = describeQuiet(record);
        if (d != null) {
            try {
                ImageDescCache.put(record, d);
            } catch (Throwable ignore) {
            }
        }
        return d;
    }

    private static String describeQuiet(Object record) {
        try {
            AiProvider vp = visionProvider();
            if (vp == null) return null;
            String d = imageDescribe(vp, record);
            if (d == null) return null;
            d = d.replace("\n", " ").trim();
            if (d.length() > 60) d = d.substring(0, 60) + "…";
            return d.length() == 0 ? null : d;
        } catch (Throwable t) {
            return null;
        }
    }

    private static String helpText() {
        StringBuilder b = new StringBuilder("我现在能做的事：\n");
        b.append("· 叫醒我：");
        String w = "";
        try {
            w = AiConfig.wakeWordsRaw();
        } catch (Throwable ignore) {
        }
        b.append(w.length() > 0 ? w : "猫娘").append(" 在吗（任何群都行）\n");
        if (AiConfig.ttsCmdOn()) b.append("· 语音：语音 你好呀\n");
        if (AiConfig.enable()) {
            b.append("· 自动回话：");
            if (AiConfig.privateReply()) b.append("私聊 ");
            if (AiConfig.atReply()) b.append("@我 ");
            if (AiConfig.keywordReply()) b.append("关键词 ");
            b.append("\n");
        } else {
            b.append("· 自动回话：现在关着（只能用唤醒词叫我）\n");
        }
        b.append("· AI 设置 / 意识 / 白名单：控制台 → AI");
        return b.toString();
    }

    private static void sendTextQuiet(EventApi.Message m, String text) {
        try {
            com.cathelperawa.owo.app.api.MsgApi.sendText(m.chatType, m.peerUid, text);
        } catch (Throwable t) {
            AiDebug.log("  ✗ 发送失败 " + t);
        }
    }

    static boolean isRecentlySentPublic(String text) {
        return isRecentlySent(text);
    }

    private static boolean isRecentlySent(String text) {
        if (text == null) return false;
        String k = text.trim();
        if (k.length() == 0) return false;
        if (k.length() > 60) k = k.substring(0, 60);
        Long t = SENT.get(k);
        return t != null && System.currentTimeMillis() - t.longValue() < 180000L;
    }

    

    
    private static String ownerLine() {
        String nick = "";
        String uin = "";
        try {
            nick = com.cathelperawa.owo.app.api.IdentityApi.currentNick();
        } catch (Throwable ignore) {
        }
        try {
            String uid = com.cathelperawa.owo.app.api.IdentityApi.currentUid();
            uin = com.cathelperawa.owo.app.api.IdentityApi.uinFromUid(uid);
        } catch (Throwable ignore) {
        }
        if (nick == null) nick = "";
        if (uin == null) uin = "";
        if (nick.length() == 0 && uin.length() == 0) return "";
        StringBuilder sb = new StringBuilder("\n\n【你的主人】");
        sb.append("这个号是「").append(nick.length() > 0 ? nick : uin).append("」的");
        if (uin.length() > 0) sb.append("（QQ ").append(uin).append("）");
        sb.append("。他是真正的主人，你替他说话、替他做事；")
          .append("对他可以亲近、少客套，但别把他当成普通群友。");
        int n = 0;
        try {
            n = AiConfig.adminCount();
        } catch (Throwable ignore) {
        }
        if (n > 0) {
            sb.append("\n另外有 ").append(n).append(" 位他授权的管理员，")
              .append("管理员能让你做危险操作，但不是主人，别搞混。");
        }

        try {
            String wake = AiConfig.wakeWordsRaw();
            String adminWake = AiConfig.adminWakeWordsRaw();
            StringBuilder names = new StringBuilder();
            if (wake != null && wake.trim().length() > 0) names.append(wake.trim());
            if (adminWake != null && adminWake.trim().length() > 0) {
                if (names.length() > 0) names.append(",");
                names.append(adminWake.trim());
            }
            if (names.length() > 0) {
                String[] arr = names.toString().split("[,，、\\s]+");
                StringBuilder disp = new StringBuilder();
                int cnt = 0;
                for (int i = 0; i < arr.length; i++) {
                    String w = arr[i] == null ? "" : arr[i].trim();
                    if (w.length() == 0) continue;
                    if (cnt++ > 0) disp.append("」「");
                    disp.append(w);
                    if (cnt >= 6) break;
                }
                if (disp.length() > 0) {
                    sb.append("\n【别人怎么叫你】「").append(disp).append("」")
                      .append("这些都是在叫你（你的名字/昵称），有人这么喊就是在跟你说话。")
                      .append("别人问「X 是谁」时，如果 X 是这里面任何一个，答案是「我」。");
                }
            }
        } catch (Throwable ignore) {
        }
        return sb.toString();
    }

    private static String wakerHint(EventApi.Message m) {
        if (m == null) return "";
        String name = peerNameOf(m);
        boolean isOwner = false;
        try {
            isOwner = AiConfig.isSelfUid(m.senderUid);
        } catch (Throwable ignore) {
        }
        if (isOwner) {
            return "【这次叫你的是主人】是「" + name + "」把你叫出来的 —— "
                    + "他是这个号的主人本人。直接接话，别问「你是谁」，别客套。";
        }
        boolean isAdmin = false;
        try {
            String uin = com.cathelperawa.owo.app.api.IdentityApi.uinFromUid(m.senderUid);
            isAdmin = AiConfig.isAdminUin(uin) || AiConfig.isAdminUid(m.senderUid);
        } catch (Throwable ignore) {
        }
        if (isAdmin) {
            return "【这次叫你的是管理员】是「" + name + "」把你叫出来的 —— "
                    + "他是主人授权的管理员，可以让你办事；对他客气点，但他是管理员，不是主人。";
        }
        return "";
    }

    

    private static final java.util.Map<String, Integer> PROACT_TODAY =
            new java.util.concurrent.ConcurrentHashMap<String, Integer>();
    private static volatile int PROACT_DAY = 0;

    private static double computeUrgency(EventApi.Message m) {
        try {
            int h = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
            double energy = (h >= 9 && h < 23) ? 1.0 : 0.3;

            double social = 0.6;

            long last = 0;
            Long l = LAST.get(m.peerUid);
            if (l != null) last = l.longValue();
            double minutes = last <= 0 ? 60 : (System.currentTimeMillis() - last) / 60000.0;
            double timeFactor = Math.min(1.0, minutes / 30.0);

            double intimacy = 0.5;
            try {
                AiAffinity.Profile p = AiAffinity.get(m.senderUid);
                int score = p == null ? 50 : p.score;
                intimacy = 0.3 + Math.min(1.0, score / 100.0) * 0.7;
            } catch (Throwable ignore) {
            }

            double u = social * 0.35 + energy * 0.15 + timeFactor * 0.30 + intimacy * 0.20;
            
            try {
                u += AiPersonality.urgencyMod();
            } catch (Throwable ignore) {
            }

            int day = Integer.parseInt(new java.text.SimpleDateFormat("yyyyMMdd",
                    java.util.Locale.CHINA).format(new java.util.Date()));
            if (day != PROACT_DAY) {
                PROACT_DAY = day;
                PROACT_TODAY.clear();
            }
            Integer n = PROACT_TODAY.get(m.peerUid);
            int today = n == null ? 0 : n.intValue();
            double penalty = Math.max(0.2, 1.0 - today * 0.15);
            PROACT_TODAY.put(m.peerUid, Integer.valueOf(today + 1));

            u *= penalty;
            return u < 0 ? 0 : (u > 1 ? 1 : u);
        } catch (Throwable t) {
            return 0.3;
        }
    }

    public static int proactiveTodayTotal() {
        int n = 0;
        try {
            for (Integer v : PROACT_TODAY.values()) {
                if (v != null) n += v.intValue();
            }
        } catch (Throwable ignore) {
        }
        return n;
    }

    private static final java.util.Map<String, Long> WAKE_LAST =
            new java.util.concurrent.ConcurrentHashMap<String, Long>();

    private static boolean passWakeCooldown(String peer) {
        if (peer == null) return true;
        int sec = AiConfig.wakeCooldownSec();
        long now = System.currentTimeMillis();
        Long last = WAKE_LAST.get(peer);
        if (last != null && now - last.longValue() < sec * 1000L) return false;
        WAKE_LAST.put(peer, Long.valueOf(now));
        return true;
    }

    private static String peerNameOf(EventApi.Message m) {
        try {
            String nick = m.senderName == null ? "" : m.senderName.trim();
            if (nick.length() > 0 && !nick.startsWith("u_")) return nick;

            String uin = null;
            try { uin = IdentityApi.uinFromUid(m.senderUid); } catch (Throwable ignore) {}

            if (m.group && uin != null && uin.length() > 0) {
                try {
                    String card = IdentityApi.troopMemberName(m.peerUid, uin);
                    if (card != null && card.trim().length() > 0) return card.trim();
                } catch (Throwable ignore) {}
            }
            try {
                String buddy = ContactApi.getBuddyNameByUid(m.senderUid);
                if (buddy != null && buddy.trim().length() > 0) return buddy.trim();
            } catch (Throwable ignore) {}

            if (uin != null && uin.length() > 0) return uin;
            return "对方";
        } catch (Throwable ignore) {
            return "对方";
        }
    }

    private static ThreadPoolExecutor pool() {
        ThreadPoolExecutor threadPoolExecutor = POOL;
        if (threadPoolExecutor != null) {
            return threadPoolExecutor;
        }
        synchronized (AiEngine.class) {
            ThreadPoolExecutor threadPoolExecutor2 = POOL;
            if (threadPoolExecutor2 != null) {
                return threadPoolExecutor2;
            }

            
            ThreadPoolExecutor threadPoolExecutor3 = new ThreadPoolExecutor(2, 4, 60L, TimeUnit.SECONDS, new ArrayBlockingQueue(32), new ThreadFactory() { 
                @Override 
    
                public Thread newThread(Runnable runnable) {
                    Thread thread = new Thread(runnable, "cat-ai-reply");
                    thread.setDaemon(true);
                    return thread;
                }
            });
            POOL = threadPoolExecutor3;
            return threadPoolExecutor3;
        }
    }

    
    public static void proactiveReply(EventApi.Message message) {
        if (!AiConfig.enable()) return;
        String str;
        AiProvider current = AiProvider.current();
        if (current == null) {
            return;
        }
        JSONArray jSONArray = new JSONArray();
        try {
            String persona = AiConfig.persona();
            try {
                str = IdentityApi.currentNick();
            } catch (Throwable th) {
                str = "";
            }
            String peerNameOf = peerNameOf(message);
            if (persona != null) {
                if (str == null || str.length() == 0) {
                    str = "我";
                }
                persona = persona.replace("{{me}}", str).replace("{{peer}}", peerNameOf);
            }
            if (persona == null) {
                persona = "";
            }
            String str2 = persona + "\n\n【这次不是有人问你】你在" + (message.group ? "群里" : "私聊里") + "听完上面这些聊天，自己想插一句就说，不想说就只回一个词：[不说]。说的时候短一点（5~20 字），别 @ 谁，别解释你在干嘛，别像客服。";
            JSONObject jSONObject = new JSONObject();
            jSONObject.put("role", "system");
            jSONObject.put("content", str2);
            jSONArray.put(jSONObject);
            List<AiStore.Msg> recent = AiStore.recent(message.peerUid, AiConfig.historyLimit());
            boolean z = false;
            for (int i = 0; i < recent.size(); i++) {
                JSONObject jSONObject2 = new JSONObject();
                jSONObject2.put("role", recent.get(i).role);
                jSONObject2.put("content", recent.get(i).content);
                jSONArray.put(jSONObject2);
            }
            JSONObject jSONObject3 = new JSONObject();
            jSONObject3.put("role", "user");
            jSONObject3.put("content", "（系统提示：现在没人叫你，你想插一句就说，不想说就只回 [不说]）");
            jSONArray.put(jSONObject3);
            AiUsage.setSource("proactive");
            String callModel = callModel(current, jSONArray);
            AiUsage.clearSource();
            if (callModel == null) {
                return;
            }
            if (callModel.contains("[不说]")) {
                AiDebug.log("  · 主动对话: 它选择不说");
                return;
            }
            String cleanReply = cleanReply(callModel);
            if (cleanReply.trim().length() == 0) {
                return;
            }
            String trim = cleanReply.replaceAll("\\[\\s*看图[^\\]]*\\]", "").trim();
            if (trim.length() == 0) {
                return;
            }
            try {
                z = CatApi.sendText(message.chatType, message.peerUid, trim);
            } catch (Throwable th2) {
            }
            AiDebug.log("  " + (z ? "✓ 主动发送" : "✗ 主动发送失败") + " → " + trim);
            if (z) {
                try {
                    AiStore.add(message.peerUid, "assistant", trim);
                } catch (Throwable th3) {
                }
            }
        } catch (Throwable th4) {
        }
    }

    private static String pttTextOf(EventApi.Message message) {
        String readPttText = readPttText(message.raw);
        if (readPttText.length() > 0) {
            return readPttText;
        }
        if (!hasVoice(message.raw)) {
            return "";
        }
        try {
            Thread.sleep(400L);
        } catch (Throwable th) {
        }
        Object obj = MsgCache.get(message.msgId);
        if (obj != null) {
            String readPttText2 = readPttText(obj);
            if (readPttText2.length() > 0) {
                return readPttText2;
            }
        }
        return readPttText(message.raw);
    }

    public static int queueSize() {
        ThreadPoolExecutor threadPoolExecutor = POOL;
        if (threadPoolExecutor == null) {
            return 0;
        }
        return threadPoolExecutor.getQueue().size();
    }

    private static String readPttText(Object obj) {
        try {
            PttGrabber.Source fromRecord = PttGrabber.fromRecord(obj);
            if (fromRecord != null && fromRecord.text != null) {
                return fromRecord.text;
            }
            return "";
        } catch (Throwable th) {
            return "";
        }
    }

    

    

    

    

    
    
    private static final java.util.Map<String, java.util.concurrent.locks.ReentrantLock> PEER_LOCK
            = new java.util.concurrent.ConcurrentHashMap<String, java.util.concurrent.locks.ReentrantLock>();

    private static java.util.concurrent.locks.ReentrantLock peerLock(String peer) {
        if (peer == null) return null;
        java.util.concurrent.locks.ReentrantLock l = PEER_LOCK.get(peer);
        if (l != null) return l;
        synchronized (PEER_LOCK) {
            l = PEER_LOCK.get(peer);
            if (l == null) {
                l = new java.util.concurrent.locks.ReentrantLock(true);   
                PEER_LOCK.put(peer, l);
            }
            return l;
        }
    }

    private static void reply(EventApi.Message m, String userText) {
        reply(m, userText, false);
    }

    private static void reply(EventApi.Message m, String userText, boolean forced) {
        java.util.concurrent.locks.ReentrantLock lk = peerLock(m == null ? null : m.peerUid);
        if (lk == null) {
            replyInner(m, userText, forced);
            return;
        }
        if (lk.isLocked()) {
            AiDebug.log("  · 这个会话还在回上一条，排队等锁");
        }
        lk.lock();
        try {
            replyInner(m, userText, forced);
        } finally {
            lk.unlock();
        }
    }

    private static void replyInner(EventApi.Message m, String userText, boolean forced) {
        if (!forced && !AiConfig.enable()) {
            AiDebug.log("  · 开关已关，放弃本轮回复");
            return;
        }

        boolean allowEmoji = false;
        try {
            if (AiConfig.emojiOn()) {
                if (askedEmoji(userText)) allowEmoji = true;
                else allowEmoji = Math.random() * 100 < AiConfig.emojiPct();
            }
        } catch (Throwable ignore) {
        }
        
        try {
            if (allowEmoji && AiConfig.affinityOn() && AiConfig.affinityTone()) {
                AiAffinity.Profile ap = AiAffinity.get(m.senderUid);
                if (ap != null && ap.score < AiConfig.affinityTier2()) {
                    allowEmoji = false;
                    AiDebug.log("  · 表情: 关系还生（" + ap.score + " 分），这次不发");
                }
            }
        } catch (Throwable ignore) {
        }

        try {
            if (com.cathelperawa.owo.app.script.PatchPoint.has("host.ai.input")) {
                com.cathelperawa.owo.app.script.PatchPoint.PatchCtx pc =
                        new com.cathelperawa.owo.app.script.PatchPoint.PatchCtx(
                                "host.ai.input", userText == null ? "" : userText, m.peerUid);
                com.cathelperawa.owo.app.script.PatchPoint.emit(pc);
                if (pc.cancelled()) {
                    AiDebug.log("  · 脚本忽略了这条输入");
                    return;
                }
                String now = pc.text();
                if (now != null && now.length() > 0) userText = now;
            }
        } catch (Throwable ignore) {
        }

        String storeUser = userText == null ? "" : userText;
        AiProvider p = AiProvider.current();
        if (p == null) {
            AiDebug.log("  ✗ 没有启用的提供商");
            return;
        }
        AiDebug.log("  · 用 " + p.displayName() + " / " + p.model
                + " 请求 " + p.baseUrl);

        JSONArray msgs = new JSONArray();
        try {
            
            String persona = AiConfig.persona();
            
            if (persona != null && persona.length() > 0) {
                String me = "";
                try {
                    me = com.cathelperawa.owo.app.bridge.QQEnv.currentNickname();
                } catch (Throwable ignore) {
                }
                if (me == null || me.length() == 0) me = "我";
                String peerName = peerNameOf(m);
                persona = persona.replace("{{me}}", me).replace("{{peer}}", peerName);
                AiDebug.log("  · 人设: me=" + me + " peer=" + peerName);
            }

            
            boolean cacheOpt = AiConfig.cacheOpt();
            String sysText = persona == null ? "" : persona;
            try {
                if (AiConfig.emojiOn()) {
                    String ep = cacheOpt
                            ? com.cathelperawa.owo.app.ai.AiEmoji.catalogFor()
                            : com.cathelperawa.owo.app.ai.AiEmoji.promptFor(allowEmoji, m.peerUid);
                    if (ep.length() > 0) sysText = sysText + ep;
                    AiDebug.log("  · 表情: 本次" + (allowEmoji ? "允许" : "不允许")
                            + "（概率 " + AiConfig.emojiPct() + "%）"
                            + (cacheOpt ? " · 清单进 system，本轮指令进末尾" : " · 旧布局"));
                }
            } catch (Throwable ignore) {
            }
            
            if (AiConfig.personalityOn()) {
                try {
                    String pt = AiPersonality.describe();
                    if (pt.length() > 0) sysText = sysText + "\n\n【你的性格】" + pt;
                } catch (Throwable ignore) {
                }
            }
            
            try {
                String ownerLine = ownerLine();
                if (ownerLine.length() > 0) sysText = sysText + ownerLine;
            } catch (Throwable ignore) {
            }
            if (sysText.trim().length() > 0) {
                JSONObject sys = new JSONObject();
                sys.put("role", "system");
                sys.put("content", sysText);
                msgs.put(sys);
            }
            
            int limit = AiConfig.historyLimit();
            List<AiStore.Msg> ctx;
            if (cacheOpt) {

                int win = Math.max(8, Math.min(limit, 60));
                int total = AiStore.size(m.peerUid);

                

                int q = Math.max(4, win / 4);
                int start = Math.max(0, ((total - win) / q) * q);
                ctx = AiStore.slice(m.peerUid, start, total);
                AiDebug.log("  · 历史: 块对齐 [" + start + "," + total + ") 共 "
                        + ctx.size() + " 条（窗口 " + win + "，步长 " + q + "）");
            } else {
                ctx = AiStore.recent(m.peerUid, limit);
            }

            
            if (forced) {
                try {
                    List<ChatLog.Line> flow = ChatLog.recent(m.peerUid, 24);

                    
                    if (!flow.isEmpty()) {
                        List<AiStore.Msg> merged = new ArrayList<AiStore.Msg>();
                        int imgBudget = 5;          
                        for (int i = 0; i < flow.size(); i++) {
                            ChatLog.Line e = flow.get(i);
                            String content = (e.who.length() == 0 ? "" : "「" + e.who + "」：") + e.text;

                            
                            try {
                                if (e.raw != null && hasImage(e.raw)) {
                                    if (e.desc == null && imgBudget > 0) {
                                        imgBudget--;
                                        e.desc = describeCached(e.raw);
                                        if (e.desc != null && e.desc.length() > 0) {
                                            AiDebug.log("  · 历史图识别：" + e.desc);
                                        }
                                    }
                                    if (e.desc != null && e.desc.length() > 0) {
                                        if (content.indexOf("[图片") >= 0) {
                                            content = content.replace("[图片]", "[图片：" + e.desc + "]");
                                        } else {
                                            content = content + "〔图：" + e.desc + "〕";
                                        }
                                    }
                                }
                            } catch (Throwable ignore) {
                            }
                            merged.add(new AiStore.Msg(e.self ? "assistant" : "user",
                                    content, e.at / 1000L, e.self ? null : e.uid));
                        }
                        ctx = merged;
                        AiDebug.log("  · 唤醒上下文改用聊天流水 " + ctx.size()
                                + " 条（AiStore 只有 " + AiStore.size(m.peerUid) + " 条）");
                    }
                } catch (Throwable t) {
                    AiDebug.log("  · 聊天流水读取失败 " + t);
                }
            }
            for (int i = 0; i < ctx.size(); i++) {
                JSONObject o = new JSONObject();
                o.put("role", ctx.get(i).role);
                o.put("content", ctx.get(i).content);
                msgs.put(o);
            }
            JSONObject cur = new JSONObject();
            cur.put("role", "user");
            
            String env = envLine(m);
            
            String cross = crossMemoryOf(m);
            if (cross.length() > 0) env = env + "\n" + cross;

            try {
                if (AiConfig.affinityOn()) {
                    AiAffinity.touch(m.senderUid, peerNameOf(m), uinOf(m));
                    String aff = AiAffinity.promptFor(m.senderUid);
                    if (aff.length() > 0) env = env + "\n" + aff;
                }
            } catch (Throwable ignore) {
            }
            
            String body = userText;
            if (m.group) {
                String who = speakerTagOf(m);       
                if (userText == null || userText.trim().length() == 0) {
                    body = "「" + who + "」发了条消息";
                } else {
                    body = "「" + who + "」： " + userText;
                }
            }
            storeUser = body;
            
            if (forced) {
                try {
                    String who = wakerHint(m);
                    if (who.length() > 0) env = env + "\n" + who;
                } catch (Throwable ignore) {
                }
            }
            
            if (AiConfig.personalityOn()) {
                try {
                    AiPersonality.observe(userText, null);
                } catch (Throwable ignore) {
                }
            }
            if (AiConfig.moodOn()) {
                try {
                    AiMood.touch(userText);
                    env = env + "\n【你现在的心情】" + AiMood.describe();
                    
                    try {
                        AiMood.touchUser(m.senderUid, userText);
                        String uh = AiMood.userMoodHint(m.senderUid);
                        if (uh.length() > 0) env = env + "\n【对方的状态】" + uh;
                    } catch (Throwable ignore) {
                    }
                } catch (Throwable ignore) {
                }
            }

            try {
                List<AiStore.Msg> found = AiStore.search(m.peerUid, userText, 4, ctx);
                if (found != null && found.size() > 0) {
                    StringBuilder mem = new StringBuilder("\n【相关记忆（更早聊过的）】");
                    for (int i = 0; i < found.size(); i++) {
                        AiStore.Msg f = found.get(i);
                        mem.append("\n· ").append(f.content.length() > 120
                                ? f.content.substring(0, 120) + "…" : f.content);
                    }
                    mem.append("\n（这些是更早的对话，跟当前这条有关就参考，别硬提）");
                    env = env + mem.toString();
                    AiDebug.log("  · 相关记忆命中 " + found.size() + " 条");
                }
            } catch (Throwable t) {
                AiDebug.log("  · 相关记忆检索失败 " + t);
            }
            
            if (cacheOpt && AiConfig.emojiOn()) {
                try {
                    String th = com.cathelperawa.owo.app.ai.AiEmoji.turnHint(allowEmoji, m.peerUid);
                    if (th.length() > 0) env = env + th;
                } catch (Throwable ignore) {
                }
            }
            String withEnv = env.length() == 0 ? body : (env + "\n" + body);

            String img = null;
            if (AiConfig.vision()) {
                AiProvider vp = visionProvider();
                boolean other = vp != null && p != null && vp.id != null && !vp.id.equals(p.id);
                if (other) {
                    String desc = imageDescribe(vp, m.raw);
                    if (desc != null && desc.length() > 0) {
                        withEnv = withEnv + "\n（对方发来一张图，内容：" + desc + "）";
                        AiDebug.log("  · 识图: 由 " + vp.displayName() + " 描述 → " + desc);
                    } else {
                        AiDebug.log("  · 识图: 识图模型没给出描述");
                    }
                } else {
                    img = imageDataUrl(m.raw);
                }
            }

            
            
            String pttText = AiConfig.pttText() ? pttTextOf(m) : "";
            String audio = null;
            AiDebug.log("  · 语音诊断: hasVoice=" + hasVoice(m.raw)
                    + " hasReply=" + hasReply(m.raw)
                    + " pttTextLen=" + pttText.length()
                    + " src=" + com.cathelperawa.owo.app.protocol.PttGrabber.fromRecord(m.raw));
            if (pttText != null && pttText.length() > 0) {
                AiDebug.log("  · 语音: TIM 已转成文字「" + pttText + "」");
                withEnv = withEnv + "\n（对方这条语音，TIM 已经转成文字了：「"
                        + pttText + "」，直接按这个回就行）";
            } else if (AiConfig.pttText() && AiConfig.pttAsr() && hasVoice(m.raw)) {

                AiDebug.log("  · 语音: TIM 没给文字，走本地识别");
                pttText = asrTextOf(m);
                if (pttText != null && pttText.length() > 0) {
                    AiDebug.log("  · 语音: 识别结果「" + pttText + "」");
                    boolean quoted = hasReply(m.raw) && !hasTopVoice(m.raw);
                    if (quoted) {
                        withEnv = withEnv + "\n（他引用的那条语音，我已经转成文字了：「"
                                + pttText + "」——就是在问这条，直接照着回）";
                    } else {
                        withEnv = withEnv + "\n（对方这条语音，转成文字是：「"
                                + pttText + "」，直接按这个回就行）";
                    }
                }
            }
            if ((pttText == null || pttText.length() == 0) && AiConfig.voice()) {
                audio = voiceDataUrl(m.raw, m.text);
            } else if ((pttText == null || pttText.length() == 0)
                    && !AiConfig.voice() && hasVoice(m.raw)) {
                AiDebug.log("  · 语音: 没有转文字结果，且「听语音」关着 → 只能说不懂");
                withEnv = withEnv + "\n（对方发的是语音，但这版没法听到里面的内容，"
                        + "别装作听懂了，直接说没听清、让他再说一遍或打字）";
            }
            if (audio != null) {
                
                AiDebug.log("  · 语音: 纯语音消息，正文不作为文字发给模型");
            }

            if (pttText != null && pttText.length() > 0) {
                
                if (storeUser.indexOf("[语音]") >= 0) {
                    storeUser = storeUser.replace("[语音]", TAG_VOICE);
                }
                if (storeUser.indexOf(TAG_VOICE) >= 0) {
                    storeUser = storeUser.replace(TAG_VOICE, TAG_VOICE + " " + pttText);
                } else if (storeUser.trim().length() == 0) {
                    storeUser = TAG_VOICE + " " + pttText;
                } else {
                    storeUser = storeUser + "（" + TAG_VOICE + " " + pttText + "）";
                }
                AiDebug.log("  · 语音: 已并入对话记忆 → " + storeUser);
            }

            
            
            try {
                com.cathelperawa.owo.app.protocol.ReplyInfo.Info ri =
                        com.cathelperawa.owo.app.protocol.ReplyInfo.of(m.raw, m.group, m.peerUid);
                if (ri.present) {
                    String line = com.cathelperawa.owo.app.protocol.ReplyInfo.describe(
                            ri, speakerTagOf(m), img != null || audio != null);
                    if (line.length() > 0) {
                        withEnv = withEnv + "\n" + line;
                        AiDebug.log("  · 引用: " + line);
                    }
                }
            } catch (Throwable t) {
                AiDebug.log("  · 引用解析失败 " + t);
            }

            if (img != null || audio != null) {
                
                JSONArray parts = new JSONArray();
                JSONObject t = new JSONObject();
                t.put("type", "text");
                String cap;
                if (userText == null || userText.trim().length() == 0) {
                    cap = audio != null ? "（对方发来一条语音，听一下再说）" : "看看这张图";
                } else {
                    cap = withEnv;
                }
                t.put("text", cap);
                parts.put(t);
                if (img != null) {
                    JSONObject iu = new JSONObject();
                    iu.put("url", img);
                    JSONObject iuWrap = new JSONObject();
                    iuWrap.put("type", "image_url");
                    iuWrap.put("image_url", iu);
                    parts.put(iuWrap);
                }
                if (audio != null) {

                    
                    JSONObject au = new JSONObject();
                    if (audio.startsWith("data:")) {
                        au.put("url", audio);
                    } else {
                        au.put("data", audio);
                        au.put("format", "mp3");
                    }
                    JSONObject aw = new JSONObject();
                    aw.put("type", "input_audio");
                    aw.put("input_audio", au);
                    parts.put(aw);
                }
                cur.put("content", parts);
            } else {

                cur.put("content", withEnv);
            }
            msgs.put(cur);
        } catch (Throwable t) {
            HookLog.e("AI 组上下文失败", t);
            return;
        }

        String content = callWithTools(p, msgs, m.chatType, m.peerUid, userText, m.senderUid);

        String seeIdx = seeRequest(content);
        if (seeIdx != null) {
            try {
                JSONArray msgs2 = new JSONArray(msgs.toString());
                JSONObject u = new JSONObject();
                u.put("role", "user");
                JSONArray parts = new JSONArray();
                JSONObject tx = new JSONObject();
                tx.put("type", "text");
                tx.put("text", "（这是你要看的图，看完直接回答对方，别再要图）");
                parts.put(tx);

                java.util.List<String> recent =
                        com.cathelperawa.owo.app.ai.AiEmoji.recentSent(m.peerUid);
                String[] idxs = seeIdx.split("[,，\\s]+");
                int added = 0;
                for (int i = 0; i < idxs.length && added < 3; i++) {
                    int n = 0;
                    try { n = Integer.parseInt(idxs[i].trim()); } catch (Throwable ignore) {}
                    if (n <= 0 || n > recent.size()) continue;
                    String du = emojiDataUrl(recent.get(n - 1));
                    if (du == null) continue;
                    JSONObject iu = new JSONObject();
                    iu.put("url", du);
                    JSONObject wrap = new JSONObject();
                    wrap.put("type", "image_url");
                    wrap.put("image_url", iu);
                    parts.put(wrap);
                    added++;
                }
                if (added > 0) {
                    u.put("content", parts);
                    msgs2.put(u);
                    AiDebug.log("  · 它要求看 " + added + " 张图，重新请求");
                    AiUsage.setSource("vision");
                    String again = callModel(p, msgs2);
                    AiUsage.clearSource();
                    if (again != null && again.trim().length() > 0) content = again;
                }
            } catch (Throwable t) {
                AiDebug.log("  ✗ 看图二次请求失败 " + t);
            }
        }
        if (content == null || content.trim().length() == 0) {
            HookLog.w("AI：模型没给内容");
            return;
        }

        content = cleanReply(content);

        try {
            content = com.cathelperawa.owo.app.script.ScriptTagRegistry.process(content);
        } catch (Throwable ignore) {
        }

        content = normalizeEmojiMarks(content);
        if (content.length() == 0) {
            AiDebug.log("  ✗ 清洗后为空");
            return;
        }

        String emojiFile = null;
        String emojiLabel = null;
        java.util.List<String> emojiFiles = new java.util.ArrayList<String>();
        try {
            if (!AiConfig.emojiOn()) {
                
                content = content.replaceAll("\\[表情[:：][^\\]]*\\]", "").trim();
            } else {

            java.util.List<String> labels = new java.util.ArrayList<String>();
            while (true) {
                int p1 = content.lastIndexOf("[表情:");
                int p2b = content.lastIndexOf("[表情：");
                int idx = Math.max(p1, p2b);
                if (idx < 0) break;
                int end = content.indexOf("]", idx);
                if (end <= idx) break;
                String label = content.substring(idx + 4, end).trim();
                content = (content.substring(0, idx) + content.substring(end + 1)).trim();
                if (label.length() > 0) labels.add(0, label);   
                if (labels.size() >= 3) break;
            }
            if (!labels.isEmpty()) {
                java.util.List<String> files = new java.util.ArrayList<String>();
                for (String label : labels) {
                    com.cathelperawa.owo.app.ai.AiEmoji.Item it =
                            com.cathelperawa.owo.app.ai.AiEmoji.pick(label, m.peerUid);
                    if (it == null) {
                        AiDebug.log("  · 表情: 没找到「" + label + "」");
                        continue;
                    }
                    java.io.File f = com.cathelperawa.owo.app.ai.AiEmoji.fileOf(it);
                    if (f != null && f.exists()) {
                        files.add(f.getAbsolutePath());
                        AiDebug.log("  · 表情: 命中「" + it.label + "」");
                    }
                }
                if (!files.isEmpty()) {
                    emojiFile = files.get(0);
                    emojiLabel = labels.get(0);
                }
                emojiFiles = files;
            }
            
            if (emojiFile != null && !allowEmoji) {
                AiDebug.log("  · 表情: 本次不允许，标记丢掉");
                emojiFile = null;
            }
            }
        } catch (Throwable ignore) {
        }
        
        if (content.trim().length() == 0 && emojiFile == null) {
            AiDebug.log("  ✗ 清洗后为空");
            return;
        }

        AiStore.add(m.peerUid, "user", storeUser, m.senderUid);
        AiStore.add(m.peerUid, "assistant", content);

        
        try {
            affinitySummarizeAsync(m, userText, content);
        } catch (Throwable th) {
            AiDebug.log("  x 好感度调用异常 " + th);
        }

        
        
        int segN = 1;
        if (AiConfig.segmentEnabled()) {
            segN = pickSegCount(content);
        }
        java.util.List<String> parts;
        if (segN <= 1) {

            
            String one = joinSingle(content);
            if (one.length() > 40) {
                int splitN = one.length() > 70 ? 3 : 2;
                if (splitN > Math.min(AiConfig.segParts(), 3)) {
                    splitN = Math.max(1, Math.min(AiConfig.segParts(), 3));
                }
                parts = splitSegments(content, splitN);
                for (int i = 0; i < parts.size(); i++) {
                    parts.set(i, joinSingle(parts.get(i)));
                }
            } else {
                parts = new java.util.ArrayList<String>();
                parts.add(one);
            }
        } else {
            parts = splitSegments(content, segN);
            for (int i = 0; i < parts.size(); i++) {
                parts.set(i, joinSingle(parts.get(i)));
            }
        }

        AiDebug.log("  · 分段 " + parts.size() + " 条（原文 " + content.length()
                + " 字，上限 " + segN + "）→ " + parts);

        long delay = 0;
        for (int i = 0; i < parts.size(); i++) {
            final String seg = parts.get(i);
            final EventApi.Message msg = m;
            final boolean quote = (i == 0);      
            if (i == 0 && delay == 0) {
                sendOne(msg, seg, quote);
            } else {
                com.cathelperawa.owo.app.api.TaskApi.after(delay, new Runnable() {
                    @Override public void run() { sendOne(msg, seg, quote); }
                });
            }
            int lo = AiConfig.segMin();
            int hi = Math.max(lo, AiConfig.segMax());
            long gap = (lo + (long) (Math.random() * (hi - lo + 1))) * 1000L;
            delay += gap;
        }

        
        final java.util.List<String> sendList = emojiFiles.isEmpty()
                ? (emojiFile == null ? new java.util.ArrayList<String>()
                    : java.util.Collections.singletonList(emojiFile))
                : emojiFiles;
        for (int k = 0; k < sendList.size(); k++) {
            final String path = sendList.get(k);
            final EventApi.Message msg = m;
            final long wait = delay + k * 900L;
            Runnable sendEmoji = new Runnable() {
                @Override public void run() {
                    
                    if (!AiConfig.enable() && !isForcedPeer(msg.peerUid)) {
                        AiDebug.log("  · 开关已关，取消发表情 → " + path);
                        return;
                    }
                    try {
                        boolean ok = CatApi.sendPic(msg.chatType, msg.peerUid, path);
                        AiDebug.log("  " + (ok ? "✓ 表情已发" : "✗ 表情发送失败") + " → " + path);
                        if (ok) {
                            try {
                                com.cathelperawa.owo.app.ai.AiEmoji.markUsed(
                                        new java.io.File(path).getName());
                            } catch (Throwable ignore) {
                            }
                            try {
                                AiStore.add(msg.peerUid, "assistant",
                                        "（发了个表情包）", null);
                            } catch (Throwable ignore) {
                            }
                        }
                    } catch (Throwable t) {
                        AiDebug.log("  ✗ 表情发送异常 " + t);
                    }
                }
            };
            if (wait <= 0) sendEmoji.run();
            else com.cathelperawa.owo.app.api.TaskApi.after(wait, sendEmoji);
        }
    }

    public static void resetLimits() {
        LAST.clear();
        HOUR.clear();
    }

    
    public static void schedulePending(final EventApi.Message message, final String str) {
        final String str2 = message.peerUid;
        final long currentTimeMillis = System.currentTimeMillis();
        Map<String, Object[]> map = PENDING;
        synchronized (map) {
            map.put(str2, new Object[]{message, Long.valueOf(currentTimeMillis)});
        }
        int cooldownSec = AiConfig.cooldownSec();
        Long l = LAST.get(str2);
        long longValue = ((cooldownSec * 1000) - (l == null ? cooldownSec * 1000 : currentTimeMillis - l.longValue())) + 300;
        long j = longValue >= 300 ? longValue : 300L;
        AiDebug.log("  · 冷却中，排队 " + (j / 1000) + "s 后补回: " + str);
        try {
            TaskApi.after(j, new Runnable() { 
                @Override 
                public void run() {
                    synchronized (AiEngine.PENDING) {
                        Object[] objArr = (Object[]) AiEngine.PENDING.get(str2);
                        if (objArr != null && objArr[0] == message) {
                            AiEngine.PENDING.remove(str2);
                            Long l2 = (Long) AiEngine.LAST.get(str2);
                            if (l2 == null || l2.longValue() <= currentTimeMillis) {
                                AiDebug.log("  ↻ 冷却结束，补回: " + str);
                                try {
                                    AiEngine.onMessage(message);
                                } catch (Throwable th) {
                                    HookLog.e("AI 补回失败", th);
                                }
                            }
                        }
                    }
                }
            });
        } catch (Throwable th) {
            HookLog.e("AI 冷却排队失败", th);
        }
    }

    
    public static void probeToolsIfNeeded() {
        try {
            File f = new File(com.cathelperawa.owo.app.core.DataDir.global("diag"), "tools_probe.txt");
            if (f.exists() && f.length() > 0) return;
            probeTools();
        } catch (Throwable ignore) {
        }
    }

    public static void probeTools() {
        final AiProvider p = AiProvider.current();
        if (p == null) {
            AiDebug.log("  ✗ 工具探测：没有可用模型");
            return;
        }
        Thread t = new Thread(new Runnable() {
            @Override public void run() {
                String out = "";
                try {
                    JSONObject body = new JSONObject();
                    body.put("model", p.model);
                    JSONArray msgs = new JSONArray();
                    JSONObject u = new JSONObject();
                    u.put("role", "user");
                    u.put("content", "帮我查一下今天北京的天气，用工具查。");
                    msgs.put(u);
                    body.put("messages", msgs);

                    JSONArray tools = new JSONArray();
                    JSONObject tool = new JSONObject();
                    tool.put("type", "function");
                    JSONObject fn = new JSONObject();
                    fn.put("name", "web_search");
                    fn.put("description", "搜索互联网，返回若干条结果摘要");
                    JSONObject params = new JSONObject();
                    params.put("type", "object");
                    JSONObject props = new JSONObject();
                    JSONObject q = new JSONObject();
                    q.put("type", "string");
                    q.put("description", "搜索关键词");
                    props.put("query", q);
                    params.put("properties", props);
                    JSONArray req = new JSONArray();
                    req.put("query");
                    params.put("required", req);
                    fn.put("parameters", params);
                    tool.put("function", fn);
                    tools.put(tool);
                    body.put("tools", tools);
                    body.put("tool_choice", "auto");

                    JSONObject headers = new JSONObject();
                    headers.put("Authorization", "Bearer " + p.apiKey);
                    headers.put("Content-Type", "application/json");

                    NetApi.Result r = null;
                    String[] urls = chatUrls(p.baseUrl);
                    for (String one : urls) {
                        r = NetApi.postSync(one, headers.toString(), body.toString(), 30000);
                        if (r != null && r.is2xx()) break;
                        if (r != null && r.code != 404 && r.code != 405) break;
                    }
                    out = (r == null ? "result=null" : ("code=" + r.code + "\n" + r.body));
                    AiDebug.log("  · 工具探测: " + (r == null ? "null"
                            : ("code=" + r.code + " len=" + (r.body == null ? 0 : r.body.length()))));
                } catch (Throwable th) {
                    out = "exception: " + th;
                    AiDebug.log("  ✗ 工具探测异常 " + th);
                }
                try {
                    File f = new java.io.File(com.cathelperawa.owo.app.core.DataDir.global("diag"), "tools_probe.txt");
                    if (f.getParentFile() != null) f.getParentFile().mkdirs();
                    FileOutputStream fo = new FileOutputStream(f);
                    fo.write(("model=" + p.model + "\nurl=" + p.baseUrl + "\n" + out)
                            .getBytes("UTF-8"));
                    fo.close();
                    AiDebug.log("  · 工具探测结果已写 tools_probe.txt");
                } catch (Throwable ignore) {
                }
            }
        }, "cat-ai-tools-probe");
        t.setDaemon(true);
        try {
            t.start();
        } catch (Throwable ignore) {
        }
    }

    private static String uinOf(EventApi.Message m) {
        try {
            String s = IdentityApi.uinFromUid(m.senderUid);
            return s == null ? "" : s;
        } catch (Throwable t) {
            return "";
        }
    }

    private static void affinitySummarizeAsync(final EventApi.Message m,
                                               final String userText,
                                               final String answer) {
        if (!AiConfig.affinityOn()) {
            AiDebug.log("  · 好感度: 跳过（总开关关着）");
            return;
        }
        if (!AiConfig.affinityAuto()) {
            AiDebug.log("  · 好感度: 跳过（自动调节关着）");
            return;
        }
        final String uid = m.senderUid;
        if (uid == null || uid.length() == 0) {
            AiDebug.log("  · 好感度: 跳过（消息没有 senderUid）");
            return;
        }

        
        
        int need = AiConfig.affinityEvery();
        if (m.group) need = Math.max(need * 2, 4);
        int pending = AiAffinity.pending(uid);
        long now = System.currentTimeMillis();
        long gapMs = AiConfig.affinityGapSec() * 1000L;
        Long lastObj;
        synchronized (AFF_LAST) { lastObj = AFF_LAST.get(uid); }
        long lastAt = lastObj == null ? 0L : lastObj.longValue();
        boolean dueByCount = pending >= need;
        boolean dueByTime = pending >= 1 && lastAt > 0 && (now - lastAt) >= gapMs;
        AiDebug.log("  · 好感度: 待归纳 " + pending + "/" + need
                + "，距上次 " + (lastAt == 0 ? "∞" : ((now - lastAt) / 1000) + "s")
                + (dueByCount || dueByTime ? " → 归纳" : " → 先攒着"));
        if (!dueByCount && !dueByTime) return;
        final AiProvider p = AiProvider.current();
        if (p == null) {
            AiDebug.log("  · 好感度: 跳过（没有可用模型）");
            return;
        }
        synchronized (AFF_LAST) { AFF_LAST.put(uid, Long.valueOf(now)); }
        Thread t = new Thread(new Runnable() {
            @Override public void run() {
                try {
                    summarizeAffinity(p, m, uid);
                } catch (Throwable th) {
                    AiDebug.log("  ✗ 好感度归纳异常 " + th);
                }
            }
        }, "cat-ai-affinity");
        t.setDaemon(true);
        try {
            t.start();
        } catch (Throwable ignore) {
        }
    }

    private static void summarizeAffinity(AiProvider p, EventApi.Message m, String uid) throws Throwable {
        AiAffinity.Profile pf = AiAffinity.get(uid);
        if (pf == null) return;
        List<String> turns = new ArrayList<String>();
        List<AiStore.Msg> ctx = AiStore.recent(m.peerUid, 6);
        for (int i = 0; i < ctx.size(); i++) {
            AiStore.Msg it = ctx.get(i);
            String c = it.content == null ? "" : it.content.replace('\n', ' ');
            if (c.length() > 120) c = c.substring(0, 120) + "…";
            turns.add(("user".equals(it.role) ? "- 对方：" : "- 我：") + c);
        }
        summarizeWith(p, uid, peerNameOf(m), turns);
    }

    public static void refreshAffinity(final String uid) {
        if (!AiConfig.affinityOn()) return;
        final AiProvider p = AiProvider.current();
        final AiAffinity.Profile pf = AiAffinity.get(uid);
        if (p == null || pf == null) return;
        Thread t = new Thread(new Runnable() {
            @Override public void run() {
                try {
                    List<String> turns = new ArrayList<String>();
                    List<AiStore.Msg> old = AiStore.recentBySpeaker(uid, 8);
                    for (int i = 0; i < old.size(); i++) {
                        String c = old.get(i).content;
                        if (c == null || c.length() == 0) continue;
                        if (c.length() > 120) c = c.substring(0, 120) + "…";
                        turns.add("- 对方：" + c.replace('\n', ' '));
                    }
                    if (turns.isEmpty()) {
                        turns.add("- 对方：（没有更多记录，只能按现有印象微调）");
                    }
                    summarizeWith(p, uid, pf.displayName(), turns);
                } catch (Throwable th) {
                    AiDebug.log("  ✗ 重新评价失败 " + th);
                }
            }
        }, "cat-ai-affinity-refresh");
        t.setDaemon(true);
        try {
            t.start();
        } catch (Throwable ignore) {
        }
    }

    private static void summarizeWith(AiProvider p, String uid, String name,
                                      List<String> turns) throws Throwable {
        AiAffinity.Profile pf = AiAffinity.get(uid);
        if (pf == null) return;

        JSONArray msgs = new JSONArray();
        JSONObject sys = new JSONObject();
        sys.put("role", "system");
        sys.put("content",
                "你在帮一个聊天 AI 记录它对某人的好感度。读下面这段对话，判断对方的言行"
                        + "让好感度怎么变，并把「对这个人现在的整体印象」重写一句。\n"
                        + "只输出 JSON，不要解释、不要 markdown 代码块：\n"
                        + "{\"delta\": 整数, \"summary\": \"不超过60个字的印象\", \"reason\": \"不超过15个字\"}\n"
                        + "规则：友善、有趣、真诚分享 → 加分；辱骂、无理取闹、骚扰 → 扣分；"
                        + "普通寒暄没信息量 → 0。delta 在 -10 到 10 之间，别大起大落。\n"
                        + "summary 写「他是什么样的人 / 聊得来吗 / 有什么特点」，不要复述原话。");
        msgs.put(sys);

        JSONObject u = new JSONObject();
        u.put("role", "user");
        StringBuilder sb = new StringBuilder();
        sb.append("聊天对象：").append(name);
        if (pf.uin != null && pf.uin.length() > 0) {
            sb.append("（QQ ").append(pf.uin).append("）");
        }
        sb.append("\n当前好感度：").append(pf.score).append("/100（").append(pf.tier()).append("）");
        sb.append("\n当前印象：").append(pf.summary.length() == 0 ? "（还没有）" : pf.summary);
        sb.append("\n累计聊过：").append(pf.chats).append(" 条");
        if (!pf.log.isEmpty()) {
            sb.append("\n最近的分数变化（别用同样的理由重复加分）：");
            int from = Math.max(0, pf.log.size() - 3);
            for (int i = from; i < pf.log.size(); i++) {
                AiAffinity.Change c = pf.log.get(i);
                sb.append("\n- ").append(c.delta > 0 ? "+" : "").append(c.delta);
                if (c.reason.length() > 0) sb.append(" ").append(c.reason);
            }
        }
        if (!turns.isEmpty()) {
            sb.append("\n材料：");
            for (int i = 0; i < turns.size(); i++) {
                sb.append("\n").append(turns.get(i));
            }
        }
        u.put("content", sb.toString());
        msgs.put(u);

        AiUsage.setSource("affinity");
        String out = callModel(p, msgs);
        AiUsage.clearSource();
        if (out == null || out.length() == 0) return;
        int a = out.indexOf('{');
        int b = out.lastIndexOf('}');
        if (a < 0 || b <= a) {
            AiDebug.log("  ✗ 好感度: 模型没给 JSON → " + out);
            return;
        }
        JSONObject o = new JSONObject(out.substring(a, b + 1));
        int real = AiAffinity.apply(uid, o.optInt("delta", 0),
                o.optString("summary", ""), o.optString("reason", ""));
        AiAffinity.Profile after = AiAffinity.get(uid);
        String why = o.optString("reason", "");
        AiDebug.log("  · 好感度: " + name + " " + (real > 0 ? "+" : "") + real
                + " → " + (after == null ? "?" : String.valueOf(after.score)) + "/100"
                + (why.length() > 0 ? "（" + why + "）" : ""));
    }

    private static String seeRequest(String str) {
        if (str == null) {
            return null;
        }
        try {
            Matcher matcher = Pattern.compile("\\[\\s*看图\\s*[:：]\\s*([0-9,，\\s]+)\\]").matcher(str);
            if (matcher.find()) {
                return matcher.group(1);
            }
        } catch (Throwable th) {
        }
        return null;
    }

    

    
    public static void sendOne(EventApi.Message m, String text, boolean quote) {

        
        
        if (!AiConfig.enable() && !isForcedPeer(m.peerUid)) {
            AiDebug.log("  · 开关已关，取消发送 → " + text);
            return;
        }
        try {
            if (m.senderUid != null && m.senderUid.length() > 0) {
                LAST_UID.put(m.peerUid, m.senderUid);
            }
        } catch (Throwable ignore) {
        }
        
        try {
            if (com.cathelperawa.owo.app.script.PatchPoint.has("host.ai.reply")) {
                com.cathelperawa.owo.app.script.PatchPoint.PatchCtx pc =
                        new com.cathelperawa.owo.app.script.PatchPoint.PatchCtx(
                                "host.ai.reply", text, m.peerUid);
                com.cathelperawa.owo.app.script.PatchPoint.emit(pc);
                if (pc.cancelled()) {
                    AiDebug.log("  · 脚本拦下了这条回复");
                    return;
                }
                String now = pc.text();
                if (now != null && now.length() > 0 && !now.equals(text)) {
                    AiDebug.log("  · 脚本改写：" + text + " → " + now);
                    text = now;
                }
            }
        } catch (Throwable ignore) {
        }
        
        markSent(text);
        try {
            boolean ok;
            if (quote && AiConfig.quoteReply() && m.msgId != 0) {
                ok = CatApi.sendReply(m.chatType, m.peerUid, m.msgId, text);
                if (!ok) {
                    ok = CatApi.sendText(m.chatType, m.peerUid, text);
                }
            } else {
                ok = CatApi.sendText(m.chatType, m.peerUid, text);
            }
            AiDebug.log("  " + (ok ? "✓ 发送" : "✗ 发送失败") + " → " + text);
        } catch (Throwable t) {
            AiDebug.log("  ✗ 发送异常 " + t);
        }
    }

    private static String speakerTagOf(EventApi.Message m) {
        String name = peerNameOf(m);
        String uin = null;
        try { uin = IdentityApi.uinFromUid(m.senderUid); } catch (Throwable ignore) {}
        if (uin == null || uin.length() == 0) uin = m.senderUid;
        if (uin == null || uin.length() == 0 || name.equals(uin)) return name;
        return name + "(" + uin + ")";
    }

    
    private static String joinSingle(String text) {
        if (text == null) return "";
        String[] lines = text.replace("\r", "").split("\n+");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            String l = lines[i].trim();
            if (l.length() == 0) continue;
            if (sb.length() > 0) {
                char prev = sb.charAt(sb.length() - 1);
                if ("。！？!?～~…，,；;：:、".indexOf(prev) < 0) sb.append("，");
            }
            sb.append(l);
        }
        return sb.toString().trim();
    }

    private static int pickSegCount(String content) {
        int cap = Math.min(AiConfig.segParts(), 3);
        if (cap < 1) cap = 1;
        int len = content == null ? 0 : content.trim().length();
        if (len <= 18) return 1;                       

        double r = Math.random();
        int n;
        if (len <= 34) {
            n = r < 0.70 ? 1 : 2;                      
        } else if (len <= 64) {
            n = r < 0.40 ? 1 : (r < 0.88 ? 2 : 3);     
        } else {
            n = r < 0.15 ? 1 : (r < 0.70 ? 2 : 3);     
        }
        
        int minByLen = len <= 34 ? 1 : (len <= 64 ? 2 : 3);
        if (n < minByLen) n = minByLen;
        if (n > cap) n = cap;
        return Math.max(1, n);
    }

    private static List<String> splitSegments(String text, int max) {
        List<String> out = new ArrayList<String>();
        if (text == null || text.trim().length() == 0) return out;
        if (max <= 1) {
            out.add(text.trim());
            return out;
        }
        String t = text.trim();
        StringBuilder cur = new StringBuilder();
        for (int i = 0; i < t.length(); i++) {
            char c = t.charAt(i);
            cur.append(c);
            if ("。！？!?～~…\n".indexOf(c) < 0) continue;
            String seg = cur.toString().trim();
            if (seg.length() > 0) {
                if (seg.length() >= 4 || out.isEmpty()) {
                    out.add(seg);
                } else {
                    int last = out.size() - 1;
                    out.set(last, out.get(last) + seg);
                }
            }
            cur.setLength(0);
        }
        String tail = cur.toString().trim();
        if (tail.length() > 0) {
            if (tail.length() >= 4 || out.isEmpty()) {
                out.add(tail);
            } else {
                int last = out.size() - 1;
                out.set(last, out.get(last) + tail);
            }
        }

        while (out.size() > max) {
            int best = 0;
            int bestLen = Integer.MAX_VALUE;
            for (int i = 0; i + 1 < out.size(); i++) {
                int len = out.get(i).length() + out.get(i + 1).length();
                if (len < bestLen) {
                    bestLen = len;
                    best = i;
                }
            }
            out.set(best, out.get(best) + out.get(best + 1));
            out.remove(best + 1);
        }
        return out;
    }

    

    
    private static final Map<String, Merge> MERGE = new HashMap();

    private static final class Merge {
        final List<String> texts = new ArrayList<String>();
        EventApi.Message last;
        int seq = 0;
    }

    private static void submitMerged(final EventApi.Message m, final String text,
                                     final boolean needJudge) {
        final String peer = m.peerUid;
        final Merge buf;
        final int mySeq;
        synchronized (MERGE) {
            Merge cur = MERGE.get(peer);
            if (cur == null) {
                cur = new Merge();
                MERGE.put(peer, cur);
            }
            if (cur.texts.isEmpty() || !text.equals(cur.texts.get(cur.texts.size() - 1))) {
                cur.texts.add(text);
            } else {
                AiDebug.log("  · 连着又发了一条一样的，合并");
            }
            while (cur.texts.size() > 4) cur.texts.remove(0);
            cur.last = m;
            cur.seq++;
            mySeq = cur.seq;
            buf = cur;
        }

        int wait = AiConfig.mergeMs();
        if (wait <= 0) {
            runMerged(buf, mySeq, peer, needJudge);   
            return;
        }
        try {
            com.cathelperawa.owo.app.api.TaskApi.after(wait, new Runnable() {
                @Override public void run() {
                    runMerged(buf, mySeq, peer, needJudge);
                }
            });
        } catch (Throwable t) {
            runMerged(buf, mySeq, peer, needJudge);
        }
    }

    private static void runMerged(Merge buf, int mySeq, String peer, boolean needJudge) {
        String merged;
        EventApi.Message msg;
        int count;
        synchronized (MERGE) {
            if (buf.seq != mySeq) return;               
            if (MERGE.get(peer) != buf) return;
            MERGE.remove(peer);
            count = buf.texts.size();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < buf.texts.size(); i++) {
                if (i > 0) sb.append("\n");
                sb.append(buf.texts.get(i));
            }
            merged = sb.toString();
            msg = buf.last;
            buf.texts.clear();
        }
        if (msg == null || merged == null || merged.trim().length() == 0) return;
        if (count > 1) {
            AiDebug.log("  · 合并 " + count + " 条为一次回复: " + merged.replace("\n", " / "));
        }
        if (needJudge) {
            submitJudge(msg, merged);
            return;
        }
        if (!passCooldown(peer)) {
            schedulePending(msg, merged);
        } else if (passHourly(peer)) {
            AiDebug.log("  ✓ 触发，进入队列: " + merged);
            submit(msg, merged);
        }
    }

    private static void submit(final EventApi.Message message, final String str) {
        try {
            pool().execute(new Runnable() { 
                @Override 
                public void run() {
                    try {
                        AiEngine.reply(message, str);
                    } catch (Throwable th) {
                        HookLog.e("AI 回复失败", th);
                    }
                }
            });
        } catch (Throwable th) {
            HookLog.e("AI 排队失败", th);
        }
    }

    private static void submitWake(final EventApi.Message message, final String str) {
        try {
            
            FORCED.put(message.peerUid, Long.valueOf(System.currentTimeMillis() + 60000L));
            pool().execute(new Runnable() {
                @Override public void run() {
                    try {
                        AiEngine.reply(message, str, true);
                    } catch (Throwable th) {
                        HookLog.e("AI 唤醒回复失败", th);
                    }
                }
            });
        } catch (Throwable th) {
            HookLog.e("AI 唤醒排队失败", th);
        }
    }

    private static void submitJudge(final EventApi.Message message, final String str) {
        try {
            pool().execute(new Runnable() { 
                @Override 
                public void run() {
                    try {
                        if (!AiEngine.judgeWantsReply(message, str)) {
                            AiDebug.log("  → 它判断不用接（" + str + "）");
                            return;
                        }
                        AiDebug.log("  ✓ 它判断要接（" + str + "）");
                        if (!AiEngine.passCooldown(message.peerUid)) {
                            AiEngine.schedulePending(message, str);
                        } else if (AiEngine.passHourly(message.peerUid)) {
                            AiDebug.log("  ✓ 触发，进入队列: " + str);
                            AiEngine.reply(message, str);
                        }
                    } catch (Throwable th) {
                        HookLog.e("AI 判定失败", th);
                    }
                }
            });
        } catch (Throwable th) {
            HookLog.e("AI 判定排队失败", th);
        }
    }

    private static String voiceDataUrl(Object obj, String str) {
        if (obj == null) {
            return null;
        }
        try {
            PttGrabber.Source fromRecord = PttGrabber.fromRecord(obj);
            if (fromRecord == null || "none".equals(fromRecord.kind)) {
                return null;
            }
            byte[] bArr = null;
            for (int i = 0; i < 2; i++) {
                try {
                    bArr = PttGrabber.bytes(fromRecord);
                } catch (Throwable th) {
                    AiDebug.log("  · 语音: bytes 异常 " + th);
                }
                if (bArr != null && bArr.length > 64) {
                    break;
                }
                if (i == 0) {
                    try {
                        Thread.sleep(900L);
                    } catch (Throwable th2) {
                    }
                }
            }
            if (bArr != null && bArr.length > 64) {
                AiDebug.log("  · 语音: 已取出 " + (bArr.length / 1024) + "KB，准备解码");
                String mp3 = SilkDecode.toMp3(QQEnv.appContext(), bArr, QQEnv.appContext().getFilesDir() + "/catptt_" + System.currentTimeMillis() + ".mp3");
                if (mp3 == null) {
                    AiDebug.log("  · 语音: SILK→MP3 失败（看控制台 SilkDecode 行）");
                    return null;
                }
                byte[] readFile = ImageGrabber.readFile(mp3);
                try {
                    new File(mp3).delete();
                } catch (Throwable th3) {
                }
                if (readFile != null && readFile.length >= 100) {
                    if (readFile.length > 6291456) {
                        AiDebug.log("  · 语音: 超过 6MB，跳过");
                        return null;
                    }
                    AiDebug.log("  · 语音: MP3 " + (readFile.length / 1024) + "KB → 发给模型");
                    return "data:audio/mp3;base64," + Base64.encodeToString(readFile, 2);
                }
                AiDebug.log("  · 语音: MP3 读不出");
                return null;
            }
            AiDebug.log("  · 语音: 拿不到字节（" + fromRecord + "）");
            AiDebug.log("  · " + PttGrabber.dump(fromRecord));
            return null;
        } catch (Throwable th4) {
            AiDebug.log("  · 语音: 异常 " + th4);
            return null;
        }
    }
}
