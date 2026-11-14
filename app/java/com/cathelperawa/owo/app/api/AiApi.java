package com.cathelperawa.owo.app.api;

import com.cathelperawa.owo.app.ai.AiConfig;
import com.cathelperawa.owo.app.ai.AiDebug;
import com.cathelperawa.owo.app.ai.AiEmoji;
import com.cathelperawa.owo.app.ai.AiProvider;
import com.cathelperawa.owo.app.ai.AiStore;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.List;

public final class AiApi {

    private AiApi() {}

    public static String ask(String text) {
        return ask(null, text);
    }

    public static String ask(String peerUid, String text) {
        AiProvider p = AiProvider.current();
        if (p == null || text == null || text.trim().length() == 0) return "";
        try {
            JSONArray msgs = new JSONArray();

            String persona = AiConfig.persona();
            if (persona != null && persona.trim().length() > 0) {
                JSONObject sys = new JSONObject();
                sys.put("role", "system");
                sys.put("content", persona);
                msgs.put(sys);
            }
            if (peerUid != null && peerUid.length() > 0) {
                List<AiStore.Msg> ctx = AiStore.recent(peerUid, AiConfig.historyLimit());
                for (int i = 0; i < ctx.size(); i++) {
                    JSONObject o = new JSONObject();
                    o.put("role", ctx.get(i).role);
                    o.put("content", ctx.get(i).content);
                    msgs.put(o);
                }
            }
            JSONObject cur = new JSONObject();
            cur.put("role", "user");
            cur.put("content", text);
            msgs.put(cur);

            return chat(p, msgs, 1024);
        } catch (Throwable t) {
            AiDebug.log("AiApi.ask 失败 " + t);
            return "";
        }
    }

    public static String say(int chatType, String peerUid, String prompt) {
        return say(chatType, peerUid, prompt, false);
    }

    public static String say(int chatType, String peerUid, String prompt, boolean remember) {
        String reply = ask(peerUid, prompt);
        if (reply == null || reply.trim().length() == 0) return null;
        boolean ok = MsgApi.sendText(chatType, peerUid, reply);
        if (ok && remember) {
            try {
                AiStore.add(peerUid, "user", prompt);
                AiStore.add(peerUid, "assistant", reply);
            } catch (Throwable ignore) {
            }
        }
        return ok ? reply : null;
    }

    public static String describeImage(String path) {
        try {
            if (path == null) return "";
            File f = new File(path);
            if (!f.isFile()) return "";
            byte[] raw = AiEmoji.modelBytes(f);
            if (raw == null || raw.length < 100) return "";
            String b64 = android.util.Base64.encodeToString(raw, android.util.Base64.NO_WRAP);
            return describeBase64(b64, "这张图里是什么？一句话说清（谁/什么东西/上面的字），不超过 40 字。");
        } catch (Throwable t) {
            return "";
        }
    }

    public static String describeBase64(String b64, String question) {
        AiProvider p = visionProvider();
        if (p == null || b64 == null) return "";
        try {
            JSONArray parts = new JSONArray();
            JSONObject t = new JSONObject();
            t.put("type", "text");
            t.put("text", question == null ? "描述这张图" : question);
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
            return chat(p, msgs, 1024);
        } catch (Throwable t) {
            return "";
        }
    }

    public static AiProvider visionProvider() {
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

    public static String status() {
        AiProvider p = AiProvider.current();
        if (p == null) return "没有可用提供商";
        return "provider=" + p.displayName() + " model=" + p.model
                + " 识图" + (AiConfig.vision() ? "开" : "关")
                + " 表情" + (AiConfig.emojiOn() ? ("开 " + AiConfig.emojiPct() + "%") : "关")
                + " 主动" + (AiConfig.proactiveOn() ? ("开 " + AiConfig.proactivePct() + "%") : "关")
                + " 记忆 " + AiStore.peerCount() + " 会话";
    }

    public static void setEnabled(boolean on) {
        AiConfig.setBool("ai_enable", on);
    }

    public static boolean enabled() {
        return AiConfig.enable();
    }

    public static String getPersona() {
        return AiConfig.persona();
    }

    public static void setPersona(String text) {
        AiConfig.setString("ai_persona", text == null ? "" : text);
    }

    public static String[] providers() {
        List<AiProvider> list = AiProvider.loadAll();
        String[] out = new String[list.size()];
        for (int i = 0; i < list.size(); i++) {
            AiProvider p = list.get(i);
            out[i] = p.displayName() + " | " + p.model + " | " + p.id;
        }
        return out;
    }

    public static boolean useProvider(String id) {
        if (id == null || id.length() == 0) return false;
        if (AiProvider.find(id) == null) return false;
        AiProvider.setCurrent(id);
        return true;
    }

    public static String[] memoryPeers() {
        List<String> list = AiStore.peers();
        return list.toArray(new String[0]);
    }

    public static int memorySize(String peerUid) {
        return AiStore.size(peerUid);
    }

    public static int memoryTotal() {
        return AiStore.totalCount();
    }

    public static void clearMemory(String peerUid) {
        AiStore.clear(peerUid);
    }

    public static void clearAllMemory() {
        AiStore.clearAll();
    }

    public static void remember(String peerUid, String role, String text) {
        AiStore.add(peerUid, role, text);
    }

    public static String[] memoryDump(String peerUid) {
        List<AiStore.Msg> list = AiStore.messages(peerUid);
        String[] out = new String[list.size()];
        for (int i = 0; i < list.size(); i++) {
            out[i] = list.get(i).role + ": " + list.get(i).content;
        }
        return out;
    }

    public static String searchMemory(String peerUid, String query, int n) {
        try {
            java.util.List<com.cathelperawa.owo.app.ai.AiStore.Msg> list =
                    com.cathelperawa.owo.app.ai.AiStore.search(peerUid, query, n, null);
            org.json.JSONArray arr = new org.json.JSONArray();
            for (int i = 0; i < list.size(); i++) {
                org.json.JSONObject o = new org.json.JSONObject();
                o.put("role", list.get(i).role);
                o.put("content", list.get(i).content);
                o.put("at", list.get(i).at);
                arr.put(o);
            }
            return arr.toString();
        } catch (Throwable t) {
            return "[]";
        }
    }

    public static int emojiCount() {
        return AiEmoji.count();
    }

    public static String[] emojiList() {
        List<AiEmoji.Item> list = AiEmoji.load();
        String[] out = new String[list.size()];
        for (int i = 0; i < list.size(); i++) {
            out[i] = list.get(i).label + " | " + list.get(i).file;
        }
        return out;
    }

    public static boolean sendEmoji(int chatType, String peerUid, String label) {
        AiEmoji.Item it = AiEmoji.pick(label, peerUid);
        if (it == null) return false;
        File f = AiEmoji.fileOf(it);
        if (f == null || !f.exists()) return false;
        boolean ok = MsgApi.sendPic(chatType, peerUid, f.getAbsolutePath());
        if (ok) AiEmoji.markUsed(it.file);
        return ok;
    }

    public static void classifyEmoji() {
        AiEmoji.startClassify();
    }

    private static String chat(AiProvider p, JSONArray msgs, int maxTokens) {
        try {
            JSONObject body = new JSONObject();
            body.put("model", p.model);
            body.put("messages", msgs);
            body.put("temperature", (double) p.temperature);
            body.put("max_tokens", maxTokens);

            JSONObject h = new JSONObject();
            h.put("Authorization", "Bearer " + p.apiKey);
            h.put("Content-Type", "application/json");

            String[] urls = AiEmoji.chatUrls(p.baseUrl);
            NetApi.Result r = null;
            for (int i = 0; i < urls.length; i++) {
                r = NetApi.postSync(urls[i], h.toString(), body.toString(), 60000);
                if (r == null) continue;
                if (r.is2xx()) break;
                if (r.code != 404 && r.code != 405) break;
            }
            if (r == null || !r.is2xx()) {
                AiDebug.log("AiApi.chat 失败 " + (r == null ? "null" : (r.code + " " + r.body)));
                return "";
            }
            JSONObject o = new JSONObject(r.body);
            JSONArray ch = o.optJSONArray("choices");
            if (ch == null || ch.length() == 0) return "";
            JSONObject m0 = ch.optJSONObject(0).optJSONObject("message");
            if (m0 == null) return "";
            String text = m0.optString("content", "");
            return text == null ? "" : text.trim();
        } catch (Throwable t) {
            AiDebug.log("AiApi.chat 异常 " + t);
            return "";
        }
    }
}
