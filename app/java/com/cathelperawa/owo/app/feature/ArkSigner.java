package com.cathelperawa.owo.app.feature;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.webkit.CookieManager;
import bsh.org.objectweb.asm.signature.SignatureVisitor;
import com.cathelperawa.owo.app.api.NetApi;
import com.cathelperawa.owo.app.bridge.ContactFactory;
import com.cathelperawa.owo.app.bridge.CurrentChat;
import com.cathelperawa.owo.app.bridge.ElementFactory;
import com.cathelperawa.owo.app.bridge.MsgSender;
import com.cathelperawa.owo.app.bridge.QQEnv;
import com.cathelperawa.owo.app.core.Bg;
import com.cathelperawa.owo.app.core.HookApi;
import com.cathelperawa.owo.app.core.HookConfig;
import com.cathelperawa.owo.app.core.HookLog;
import com.cathelperawa.owo.app.core.Host;
import com.cathelperawa.owo.app.core.UIScope;
import com.cathelperawa.owo.app.proto.MediaVault;
import com.cathelperawa.owo.app.ui.MusicCardSheet;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.json.JSONArray;
import org.json.JSONObject;

public final class ArkSigner {
    private static final String API = "https://qun.qq.com/qunng/http2rpc/gotrpc/auth/trpc.group_pro.guild_activity.Components/GetArkMsgWithSign";
    private static final String BILI_APP = "com.tencent.miniapp_01";
    public static final String BILI_APPID = "1109937557";
    public static final String BUILTIN_WX_APPIDS = "1109840991|腾讯视频;1109937557|哔哩哔哩;1109907872|QQ安全中心;1109048181|腾讯客服;1109786902|订阅号;1109896843|老师助手;1109836759|火柴人神射手;1110112648|腾讯原创馆";
    public static final String DEFAULT_TAG_ICON = "https://tianxuan.gtimg.cn/47329_bd95e16e/assets/guild-icon.png";
    public static final String ERR_NO_PSKEY = "签名通道都没成功（天玄 + 备用），稍后再试";
    private static final String HOST_API = "https://freeimage.host/api/1/upload";
    private static final String HOST_KEY = "6d207e02198a847aa98d0a2a901485a5";
    private static final String HOST_YIBAI = "http://106.55.0.102:10087/";
    private static final String KEY_COOKIE = "ark_qun_cookie";
    public static final String K_MINI = "minishare_rewrite";
    public static final String K_MINI_APPID = "minishare_appid";
    public static final String K_MINI_APPNAME = "minishare_appname";
    public static final String K_MINI_BILI = "minishare_bili_only";
    public static final String K_MINI_DESC = "minishare_desc";
    public static final String K_MINI_JUMP = "minishare_jump";
    public static final String K_MINI_PIC = "minishare_pic";
    public static final String K_MINI_TITLE = "minishare_title";
    public static final String K_MINI_TPL = "minishare_tpl_id";
    public static final String K_MINI_VID = "minishare_vid";
    public static final String K_MINI_WEB = "minishare_web";
    public static final String K_SHARE = "share_hook";
    public static final String K_SHARE_DESC = "share_desc";
    public static final String K_SHARE_MUSIC = "share_music";
    public static final String K_SHARE_PIC = "share_pic";
    public static final String K_SHARE_TITLE = "share_title";
    public static final String K_SHARE_URL = "share_url";
    private static final String NEWS_SIGN = "http://106.55.0.102:10087/";
    public static final String PSKEY_PAGE = "https://qun.qq.com/";
    private static final String REFERER = "https://qun.qq.com/qunng/guild/tianxuan/p/53049_a53f0edz?traceTint=tianxuan_copy";
    public static final String SKIP = "__SKIP__";
    private static final String UA = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/139.0.7258.143 Mobile Safari/537.36 V1_AND_SQ_9.1.35_8708_YYB_D QQ/9.1.35.22670 NetType/WIFI";
    private static final Charset UTF8 = Charset.forName("UTF-8");
    private static final String HOST_SISI = "https://ss.xingzhige.com/music_card/card";
    private static final String[] MUSIC_SIGN = {HOST_SISI, "http://ss.xingzhige.com/music_card/card", "http://106.55.0.102:10087/"};
    public static final String[][] MUSIC_PLATFORMS = {new String[]{UIScope.QQ, "QQ音乐", MusicCardSheet.TAG_ICON_QQ}, new String[]{"163", "网易云音乐", MusicCardSheet.TAG_ICON_163}, new String[]{"kugou", "酷狗音乐", "https://open.gtimg.cn/open/app_icon/00/20/51/41/205141_100_m.png?t=1792029731"}, new String[]{"kuwo", "酷我音乐", "https://p.qpic.cn/qqconnect/0/app_100243533_1636374695/100?max-age=2592000&t=0"}, new String[]{"migu", "咪咕音乐", "https://open.gtimg.cn/open/app_icon/01/05/30/67/1101053067_100_m.png?t=1792056966"}, new String[]{"bilibili", "哔哩哔哩", "https://open.gtimg.cn/open/app_icon/00/95/17/76/100951776_100_m.png?t=1792658808"}};
    public static final String CH_AUTO = "auto";
    public static final String CH_TIANXUAN = "tianxuan";
    public static final String CH_YIBAI = "yibai";
    public static final String CH_SISI = "sisi";
    public static final String[] CH_IDS = {CH_AUTO, CH_TIANXUAN, CH_YIBAI, CH_SISI};
    public static final String[] CH_LABELS = {"自动 · 第三方服务优先，失败退天玄", "天玄 · 官方签名（角标可自定义，要 p_skey）", "yibai 备用 · 免 p_skey（角标固定 QQ音乐）", "思思音卡 · 免 p_skey（可换角标平台）"};
    private static volatile boolean miniHooked = false;
    private static volatile boolean shareHooked = false;
    private static final HookApi.Action SHARE_ACTION = new HookApi.Action() {
        @Override
        public void call(XC_MethodHook.MethodHookParam methodHookParam) {
            Intent intent;
            Bundle extras;
            try {
                if ((methodHookParam.thisObject instanceof Activity) && (intent = ((Activity) methodHookParam.thisObject).getIntent()) != null && (extras = intent.getExtras()) != null && !extras.isEmpty()) {
                    ArkSigner.onShareIntent(extras);
                }
            } catch (Throwable th) {
                HookLog.w("ShareHook: " + th);
            }
        }
    };

    public interface HostCb {
        void done(String str, String str2);
    }

    public interface SendCb {
        void done(boolean z, String str);
    }

    private ArkSigner() {
    }

    public static final class Result {
        public final String error;
        public final String json;

        Result(String str, String str2) {
            this.json = str;
            this.error = str2;
        }

        public boolean ok() {
            String str = this.json;
            return str != null && str.length() > 0;
        }
    }

    public static Result resign(String str) {
        String cachedCookie;
        String pskeyOf;
        String str2;
        String str3;
        String str4;
        String str5 = null;
        JSONObject jSONObject;
        String cookieFromDb;
        String pskeyOf2;
        try {
            JSONObject jSONObject2 = new JSONObject(str);
            JSONObject optJSONObject = jSONObject2.optJSONObject("meta");
            JSONObject optJSONObject2 = optJSONObject == null ? null : optJSONObject.optJSONObject("news");
            if (optJSONObject2 == null) {
                String optString = optJSONObject == null ? null : optJSONObject.optString("meta", null);
                if (optString != null && optString.length() > 2) {
                    optJSONObject2 = new JSONObject(optString).optJSONObject("news");
                }
            }
            if (optJSONObject2 == null) {
                return new Result(null, SKIP);
            }
            String optString2 = optJSONObject2.optString("title", "");
            String optString3 = optJSONObject2.optString("desc", "");
            String safeImage = safeImage(optJSONObject2.optString("preview", ""));
            String safeJump = safeJump(optJSONObject2.optString("jumpUrl", ""));
            String optString4 = optJSONObject2.optString("tag", "");
            String cleanUrl = cleanUrl(optJSONObject2.optString("tagIcon", ""));
            if (cleanUrl == null || cleanUrl.length() == 0) {
                cleanUrl = "https://tianxuan.gtimg.cn/47329_bd95e16e/assets/guild-icon.png";
            }
            if (optString2.length() == 0) {
                return new Result(null, "标题是空的");
            }
            String cookieFromManager = cookieFromManager();
            String pskeyOf3 = pskeyOf(cookieFromManager);
            if (pskeyOf3 == null && (pskeyOf2 = pskeyOf((cookieFromDb = cookieFromDb()))) != null) {
                HookLog.i("ArkSigner: p_skey 从数据库取到");
                cookieFromManager = cookieFromDb;
                pskeyOf3 = pskeyOf2;
            }
            if ((pskeyOf3 == null || pskeyOf3.length() == 0) && (pskeyOf = pskeyOf((cachedCookie = cachedCookie()))) != null && pskeyOf.length() > 0) {
                HookLog.i("ArkSigner: 用本地缓存的 p_skey");
                cookieFromManager = cachedCookie;
                pskeyOf3 = pskeyOf;
            }
            if (pskeyOf3 == null || pskeyOf3.length() == 0) {
                String cookieFromDb2 = (cookieFromManager == null || cookieFromManager.length() <= 0) ? cookieFromDb() : cookieFromManager;
                String autoPskey = autoPskey(cookieFromDb2);
                if (autoPskey != null && autoPskey.length() > 0) {
                    HookLog.i("ArkSigner: p_skey 自动获取成功");
                    str2 = "prompt";
                    str3 = cookieFromDb2;
                    str4 = autoPskey;
                    if (str4 != null) {
                        str5 = ERR_NO_PSKEY;
                    } else {
                        if (str4.length() != 0) {
                            int i = 5381;
                            int i2 = 0;
                            while (true) {
                                jSONObject = jSONObject2;
                                if (i2 >= str4.length()) {
                                    break;
                                }
                                i += (i << 5) + str4.charAt(i2);
                                i2++;
                                jSONObject2 = jSONObject;
                            }
                            int i3 = i & Integer.MAX_VALUE;
                            JSONObject jSONObject3 = new JSONObject();
                            jSONObject3.put("title", optString2);
                            jSONObject3.put("desc", optString3);
                            jSONObject3.put("tag", optString4);
                            jSONObject3.put("tagIcon", cleanUrl);
                            jSONObject3.put("preview", safeImage);
                            jSONObject3.put("jumpUrl", safeJump);
                            JSONObject jSONObject4 = new JSONObject();
                            jSONObject4.put("news", jSONObject3);
                            JSONObject jSONObject5 = new JSONObject();
                            jSONObject5.put("appId", "com.tencent.tuwen.lua");
                            jSONObject5.put("bizSrc", "guild.share");
                            String str6 = str2;
                            String optString5 = jSONObject.optString(str6, "");
                            if (optString5 != null && optString5.trim().length() != 0) {
                                optString2 = optString5;
                            }
                            jSONObject5.put(str6, optString2.trim());
                            jSONObject5.put("meta", jSONObject4.toString());
                            String jSONObject6 = jSONObject5.toString();
                            String str7 = str3;
                            String post = post("https://qun.qq.com/qunng/http2rpc/gotrpc/auth/trpc.group_pro.guild_activity.Components/GetArkMsgWithSign?bkn=" + i3, jSONObject6, str7);
                            if (post == null) {
                                return new Result(null, "请求失败（网络）");
                            }
                            JSONObject jSONObject7 = new JSONObject(post);
                            JSONObject optJSONObject3 = jSONObject7.optJSONObject("data");
                            String optString6 = optJSONObject3 == null ? null : optJSONObject3.optString("signed_ark", null);
                            if (optString6 != null && optString6.length() != 0) {
                                String str8 = new String(Base64.decode(optString6, 0), UTF8);
                                try {
                                    saveCookie(str7);
                                } catch (Throwable th) {
                                }
                                HookLog.i("ArkSigner: 成功 token=" + new JSONObject(str8).optJSONObject("config").optString("token"));
                                return new Result(str8, null);
                            }
                            int optInt = jSONObject7.optInt("retcode", -1);
                            String optString7 = jSONObject7.optString("message", "");
                            if (optInt == 4003) {
                                return new Result(null, ERR_NO_PSKEY);
                            }
                            return new Result(null, "接口返回 " + optInt + " " + optString7);
                        }
                        str5 = ERR_NO_PSKEY;
                    }
                    return new Result(null, str5 != null ? str5
                            : "没拿到 p_skey（Cookie 里没有，库里也没有）——先打开一次 qun.qq.com 再试");
                }
            }
            str3 = cookieFromManager;
            str2 = "prompt";
            str4 = pskeyOf3;
            if (str4 != null) {
            }
            return new Result(null, str5);
        } catch (Throwable th2) {
            HookLog.e("ArkSigner.resign 失败", th2);
            return new Result(null, String.valueOf(th2));
        }
    }

    public static Result signCustomArk(String str, String str2, String str3) {
        String str4;
        String str5 = "guild.share";
        try {
            Context appContext = QQEnv.appContext();
            if (appContext != null && (str4 = HookConfig.get(appContext, "ark_bizsrc", "")) != null && str4.trim().length() > 0) {
                str5 = str4.trim();
            }
        } catch (Throwable th) {
        }
        return signCustomArkBiz(str, str5, str2, str3);
    }

    public static Result signCustomArkBiz(String str, String str2, String str3, String str4) {
        return signCustomArkBiz(str, str2, str3, str4, null);
    }

    public static Result signCustomArkBiz(String str, String str2, String str3, String str4, String str5) {
        String cachedCookie;
        String pskeyOf;
        String cookieFromDb;
        String pskeyOf2;
        if (str != null) {
            try {
                if (str.trim().length() != 0) {
                    if (str4 != null && str4.trim().length() >= 2) {
                        String cookieFromManager = cookieFromManager();
                        String pskeyOf3 = pskeyOf(cookieFromManager);
                        if (pskeyOf3 == null && (pskeyOf2 = pskeyOf((cookieFromDb = cookieFromDb()))) != null) {
                            cookieFromManager = cookieFromDb;
                            pskeyOf3 = pskeyOf2;
                        }
                        if ((pskeyOf3 == null || pskeyOf3.length() == 0) && (pskeyOf = pskeyOf((cachedCookie = cachedCookie()))) != null && pskeyOf.length() > 0) {
                            cookieFromManager = cachedCookie;
                            pskeyOf3 = pskeyOf;
                        }
                        if (pskeyOf3 != null && pskeyOf3.length() != 0) {
                            int i = 5381;
                            for (int i2 = 0; i2 < pskeyOf3.length(); i2++) {
                                i += (i << 5) + pskeyOf3.charAt(i2);
                            }
                            int i3 = Integer.MAX_VALUE & i;
                            JSONObject jSONObject = new JSONObject();
                            jSONObject.put("appId", str);
                            if (str2 == null || str2.length() == 0) {
                                str2 = "guild.share";
                            }
                            jSONObject.put("bizSrc", str2);
                            if (str3 == null) {
                                str3 = "";
                            }
                            jSONObject.put("prompt", str3);
                            jSONObject.put("meta", str4);
                            if (str5 != null && str5.trim().length() > 2) {
                                try {
                                    JSONObject jSONObject2 = new JSONObject(str5);
                                    Iterator<String> keys = jSONObject2.keys();
                                    while (keys.hasNext()) {
                                        String next = keys.next();
                                        jSONObject.put(next, jSONObject2.opt(next));
                                    }
                                } catch (Throwable th) {
                                }
                            }
                            String post = post("https://qun.qq.com/qunng/http2rpc/gotrpc/auth/trpc.group_pro.guild_activity.Components/GetArkMsgWithSign?bkn=" + i3, jSONObject.toString(), cookieFromManager);
                            if (post == null) {
                                return new Result(null, "请求失败（网络）");
                            }
                            JSONObject jSONObject3 = new JSONObject(post);
                            JSONObject optJSONObject = jSONObject3.optJSONObject("data");
                            String optString = optJSONObject == null ? null : optJSONObject.optString("signed_ark", null);
                            if (optString != null && optString.length() != 0) {
                                String str6 = new String(Base64.decode(optString, 0), UTF8);
                                try {
                                    saveCookie(cookieFromManager);
                                } catch (Throwable th2) {
                                }
                                miniDbg("自定义签名 app=" + str + " -> " + str6);
                                return new Result(str6, null);
                            }
                            String str7 = jSONObject3.optInt("retcode", -1) + " " + jSONObject3.optString("message", "");
                            miniDbg("自定义签名失败 app=" + str + " -> " + str7);
                            return new Result(null, "接口返回 " + str7);
                        }
                        return new Result(null, ERR_NO_PSKEY);
                    }
                    return new Result(null, "meta 是空的");
                }
            } catch (Throwable th3) {
                miniDbg("自定义签名异常 " + th3);
                return new Result(null, String.valueOf(th3));
            }
        }
        return new Result(null, "appId 是空的");
    }

    public static void experimentBizSrc(final String str, final String str2, final String str3, final String[] strArr, final SendCb sendCb) {
        Bg.run(new Runnable() {
            @Override
            public void run() {
                String str4;
                String str5 = null;
                StringBuilder sb = new StringBuilder();
                for (String str6 : strArr) {
                    Result signCustomArkBiz = ArkSigner.signCustomArkBiz(str, str6, str2, str3);
                    if (signCustomArkBiz == null || signCustomArkBiz.json == null) {
                        sb.append(str6).append("  FAIL  ").append(signCustomArkBiz == null ? "null" : signCustomArkBiz.error).append('\n');
                    } else {
                        try {
                            JSONObject optJSONObject = new JSONObject(signCustomArkBiz.json).optJSONObject("meta");
                            JSONObject optJSONObject2 = optJSONObject == null ? null : optJSONObject.optJSONObject("news");
                            if (optJSONObject2 != null) {
                                str4 = optJSONObject2.optString("jumpUrl", "");
                                try {
                                    str5 = optJSONObject2.optString("tag", "");
                                } catch (Throwable th) {
                                    str5 = "";
                                    sb.append(str6).append("  OK  tag=").append(str5).append("\n     jump=").append(str4).append('\n');
                                }
                            } else {
                                str5 = "";
                                str4 = str5;
                            }
                        } catch (Throwable th2) {
                            str4 = "";
                        }
                        sb.append(str6).append("  OK  tag=").append(str5).append("\n     jump=").append(str4).append('\n');
                    }
                }
                String sb2 = sb.toString();
                ArkSigner.miniDbg("bizSrc 实验:\n" + sb2);
                sendCb.done(true, sb2);
            }
        }, "bizsrc-test");
    }

    public static void probeEgameSign(final String videoUrl, final String cover, final String title, final SendCb sendCb) {
        Bg.run(new Runnable() {
            @Override
            public void run() {
                StringBuilder sb = new StringBuilder("=== egameshare 签名探测 " + System.currentTimeMillis() + " ===\n");
                String cookie = cookieFromManager();
                String pskey = pskeyOf(cookie);
                if (pskey == null || pskey.length() == 0) {
                    String db = cookieFromDb();
                    String p2 = pskeyOf(db);
                    if (p2 != null && p2.length() > 0) {
                        cookie = db;
                        pskey = p2;
                    }
                }
                if (pskey == null || pskey.length() == 0) {
                    sb.append("!! 没有 p_skey，先登录 qun.qq.com\n");
                    sb.append(ERR_NO_PSKEY);
                    catLog(sb.toString());
                    sendCb.done(false, sb.toString());
                    return;
                }
                int bkn = 5381;
                for (int i = 0; i < pskey.length(); i++) {
                    bkn += (bkn << 5) + pskey.charAt(i);
                }
                bkn = Integer.MAX_VALUE & bkn;

                String[] bizList = new String[]{"guild.share"};
                String[] apps = new String[]{"com.tencent.tuwen.lua", "com.tencent.eventshare.lua"};
                java.util.ArrayList<String[]> comboList = new java.util.ArrayList<>();
                for (String a : apps) {
                    for (String b : bizList) {
                        comboList.add(new String[]{a, b});
                    }
                }
                String[][] combos = comboList.toArray(new String[0][]);

                String cov = cover == null ? "" : cover.trim();
                String ttl = (title == null || title.trim().length() == 0) ? "内容" : title.trim();
                String pic = cov.length() > 0 ? cov : "https://picsum.photos/seed/ev/600/338";
                String jump = "https://example.com/";
                String[] metas = new String[]{
                        "{\"eventshare\":{\"title\":\"" + ttl + "\",\"preview\":\"" + pic + "\",\"jumpURL\":\"" + jump + "\",\"tag\":\"标签\",\"buttonNum\":1,\"button1title\":\"点我\",\"button1URL\":\"" + jump + "\"}}",
                        "{\"news\":{\"title\":\"" + ttl + "\",\"desc\":\"描述\",\"preview\":\"" + pic + "\",\"jumpUrl\":\"" + jump + "\",\"videoUrl\":\"" + jump + "\",\"type\":\"video\"}}",
                        "{\"news\":{\"title\":\"" + ttl + "\",\"desc\":\"描述\",\"preview\":\"" + pic + "\",\"jumpUrl\":\"" + jump + "\",\"buttons\":[{\"text\":\"TA的战绩\",\"url\":\"" + jump + "\"},{\"text\":\"点我\",\"url\":\"" + jump + "\"}]}}"
                };

                for (int ci = 0; ci < combos.length; ci++) {
                    for (int mi = 0; mi < metas.length; mi++) {
                        String appId = combos[ci][0];
                        String bizSrc = combos[ci][1];
                        String meta = metas[mi];
                        String tag = "[c" + ci + ".m" + mi + "] " + appId + " / bizSrc=" + (bizSrc.length() == 0 ? "(空)" : bizSrc);
                        try {
                            JSONObject req = new JSONObject();
                            req.put("appId", appId);
                            req.put("bizSrc", bizSrc);
                            req.put("prompt", "群主女装照片");
                            req.put("meta", meta);
                            req.put("oidbApplyID", 1553084);
                            req.put("oidb_apply_id", 1553084);
                            String resp = post(API + "?bkn=" + bkn, req.toString(), cookie);
                            if (resp == null) {
                                sb.append(tag).append(" -> 网络失败\n");
                                continue;
                            }
                            JSONObject jo = new JSONObject(resp);
                            int retcode = jo.optInt("retcode", -1);
                            String message = jo.optString("message", "");
                            JSONObject data = jo.optJSONObject("data");
                            String signed = data == null ? null : data.optString("signed_ark", null);
                            if (signed != null && signed.length() > 0) {
                                String ark = new String(Base64.decode(signed, 0), UTF8);
                                sb.append(tag).append(" -> ★★ 成功！\n     ").append(clip(ark, 400)).append("\n");
                            } else {
                                boolean notFound = message.contains("ark not found");
                                String mark = notFound ? "" : "◆ 白名单命中(非not found) ";
                                sb.append(tag).append(" -> ").append(mark).append("retcode=").append(retcode).append(" msg=").append(message).append("\n");
                                if (!notFound) {
                                    sb.append("     raw=").append(clip(resp, 300)).append("\n");
                                }
                            }
                        } catch (Throwable th) {
                            sb.append(tag).append(" -> 异常 ").append(th).append("\n");
                        }
                    }
                }
                sb.append("=== 探测结束 ===\n");
                String report = sb.toString();
                catLog(report);
                miniDbg(report);
                sendCb.done(true, report);
            }
        }, "egame-probe");
    }

    public static void probeArkPaths(final SendCb sendCb) {
        Bg.run(new Runnable() {
            @Override
            public void run() {
                StringBuilder sb = new StringBuilder("=== Ark 接口路径枚举 " + System.currentTimeMillis() + " ===\n");
                String cookie = cookieFromManager();
                String pskey = pskeyOf(cookie);
                if (pskey == null || pskey.length() == 0) {
                    String db = cookieFromDb();
                    String p2 = pskeyOf(db);
                    if (p2 != null && p2.length() > 0) {
                        cookie = db;
                        pskey = p2;
                    }
                }
                if (pskey == null || pskey.length() == 0) {
                    sb.append("!! 没有 p_skey\n");
                    catLog(sb.toString());
                    miniDbg(sb.toString());
                    sendCb.done(false, sb.toString());
                    return;
                }
                int bkn = 5381;
                for (int i = 0; i < pskey.length(); i++) {
                    bkn += (bkn << 5) + pskey.charAt(i);
                }
                bkn = Integer.MAX_VALUE & bkn;

                String meta = "{\"shareData\":{\"appid\":\"1104466820\",\"content\":\"内容\",\"gameName\":\"名称\",\"scene\":\"8\",\"type\":\"video\",\"url\":\"https://example.com/a.mp4\"}}";
                JSONObject req = new JSONObject();
                try {
                    req.put("appId", "com.tencent.gamecenter.egameshare");
                    req.put("bizSrc", "adelie.agent_list");
                    req.put("prompt", "群主女装照片");
                    req.put("meta", meta);
                } catch (Throwable th) {
                }

                String base = "https://qun.qq.com/qunng/http2rpc/gotrpc/auth/";
                String[] paths = new String[]{
                        "trpc.group_pro.guild_activity.Components/GetArkMsgWithSign",
                        "trpc.group_pro.guild_activity.Components/GetArkSign",
                        "trpc.group_pro.guild_activity.Components/SignArk",
                        "trpc.group_pro.guild_activity.Components/GetArk",
                        "trpc.group_pro.guild_activity.Components/GenArk",
                        "trpc.group_pro.guild_activity.Components/GenTemplateArk",
                        "trpc.group_pro.guild_activity.Components/GetShareArk",
                        "trpc.group_pro.guild_activity.Components/ShareArk",
                        "trpc.group_pro.guild_main.Main/GetArkMsgWithSign",
                        "trpc.group_pro.guild_share.Share/GetArkMsgWithSign",
                        "trpc.group_pro.guild_share.Share/GetArkSign",
                        "trpc.lplan.interact_svr.Interact/SsoGetArkSign",
                        "trpc.lplan.interact_svr.Interact/GetArkSign",
                        "trpc.gamecenter.ark.ArkSvr/GetArkSign",
                        "trpc.gamecenter.ark.ArkSvr/GetArkMsgWithSign",
                        "trpc.gamecenter.game_share.GameShare/GetShareArk",
                        "trpc.gamecenter.game_share.GameShare/GetArkSign",
                        "trpc.qqva.ark_sign.ArkSign/Sign",
                        "trpc.qqva.ark_sign.ArkSign/GetArk",
                        "trpc.tianxuan.activity.Activity/GetArkMsgWithSign",
                        "trpc.tianxuan.activity.Activity/GetArkSign"
                };
                for (int i = 0; i < paths.length; i++) {
                    String url = base + paths[i] + "?bkn=" + bkn;
                    try {
                        String resp = post(url, req.toString(), cookie);
                        if (resp == null) {
                            sb.append("[").append(i).append("] ").append(paths[i]).append(" -> 网络失败\n");
                            continue;
                        }
                        String r = resp.length() > 180 ? resp.substring(0, 180) : resp;
                        sb.append("[").append(i).append("] ").append(paths[i]).append("\n     ").append(r).append("\n");
                    } catch (Throwable th) {
                        sb.append("[").append(i).append("] ").append(paths[i]).append(" -> 异常 ").append(th).append("\n");
                    }
                }
                sb.append("=== 枚举结束 ===\n");
                String report = sb.toString();
                catLog(report);
                miniDbg(report);
                sendCb.done(true, report);
            }
        }, "ark-path-probe");
    }

    private static String shortName(String appId) {
        if (appId == null) {
            return "null";
        }
        int i = appId.lastIndexOf('.');
        return i < 0 ? appId : appId.substring(i + 1);
    }

    private static String clip(String s, int n) {
        if (s == null) {
            return "null";
        }
        return s.length() <= n ? s : s.substring(0, n) + "…(" + s.length() + ")";
    }

    private static void catLog(String s) {
        try {
            android.util.Log.e("CATHELPER", s);
        } catch (Throwable th) {
        }
    }

    public static String buildShareDataMeta(String str, String str2, String str3, String str4, String str5, String str6) {
        return buildShareDataMeta(str, str2, str3, str4, str5, str6, 0, 0, null, null);
    }

    public static String buildShareDataMeta(String str, String str2, String str3, String str4,
                                            String str5, String str6,
                                            int width, int height,
                                            org.json.JSONArray buttons, String jumpUrl) {
        try {
            JSONObject jSONObject = new JSONObject();
            if (str == null) {
                str = "";
            }
            jSONObject.put("appid", str);
            if (str2 == null) {
                str2 = "";
            }
            jSONObject.put("gameName", str2);
            if (str3 == null) {
                str3 = "";
            }
            jSONObject.put("title", str3);
            if (str4 == null) {
                str4 = "";
            }
            jSONObject.put("content", str4);
            jSONObject.put("scene", "8");
            jSONObject.put("type", MediaVault.T_VIDEO);
            if (str5 != null && str5.length() > 0) {
                jSONObject.put("url", str5);
                jSONObject.put("videoUrl", str5);
            }
            if (str6 != null && str6.length() > 0) {
                jSONObject.put("cover", str6);
                jSONObject.put("picUrl", str6);
            }
            jSONObject.put("duration", 15);
            if (width > 0 && height > 0) {
                jSONObject.put("width", width);
                jSONObject.put("height", height);
            }
            if (buttons != null && buttons.length() > 0) {
                jSONObject.put("buttons", buttons);
            }
            if (jumpUrl != null && jumpUrl.trim().length() > 0) {
                jSONObject.put("jumpUrl", jumpUrl.trim());
            }
            JSONObject jSONObject2 = new JSONObject();
            jSONObject2.put("shareData", jSONObject);
            return jSONObject2.toString();
        } catch (Throwable th) {
            return null;
        }
    }

    public static String platformTag(String str) {
        if (str != null) {
            for (String[] strArr : MUSIC_PLATFORMS) {
                if (strArr[0].equals(str)) {
                    return strArr[1];
                }
            }
            return "QQ音乐";
        }
        return "QQ音乐";
    }

    public static String platformIcon(String str) {
        if (str != null) {
            for (String[] strArr : MUSIC_PLATFORMS) {
                if (strArr[0].equals(str)) {
                    return strArr[2];
                }
            }
        }
        return MUSIC_PLATFORMS[0][2];
    }

    public static String safeJump(String str) {
        String cleanUrl = cleanUrl(str);
        if (cleanUrl.length() == 0) {
            return "";
        }
        String lowerCase = cleanUrl.toLowerCase();
        if (lowerCase.startsWith("http://") || lowerCase.startsWith("https://")) {
            return cleanUrl;
        }
        if (lowerCase.startsWith("mqqapi://") || lowerCase.startsWith("mqq://") || lowerCase.startsWith("mqqwpa://") || lowerCase.startsWith("mqqvoip://") || lowerCase.startsWith("qqmail://") || lowerCase.startsWith("weixin://") || lowerCase.startsWith("tencent://") || lowerCase.startsWith("qzone://")) {
            return cleanUrl;
        }
        if (lowerCase.startsWith("//")) {
            return "https:" + cleanUrl;
        }
        if (lowerCase.startsWith("ttps://")) {
            return "https://" + cleanUrl.substring(7);
        }
        if (lowerCase.startsWith("http:/") && !lowerCase.startsWith("http://")) {
            return "https://" + cleanUrl.substring(6).replaceFirst("^/+", "");
        }
        if (cleanUrl.indexOf("://") > 0) {
            return cleanUrl;
        }
        if (cleanUrl.indexOf(32) < 0 && cleanUrl.indexOf(46) > 0) {
            return "https://" + cleanUrl;
        }
        return cleanUrl;
    }

    public static String safeImage(String str) {
        String cleanUrl = cleanUrl(str);
        if (cleanUrl.length() == 0) {
            return "";
        }
        if (cleanUrl.startsWith("//")) {
            return "https:" + cleanUrl;
        }
        String lowerCase = cleanUrl.toLowerCase();
        if (lowerCase.startsWith("http://") && (lowerCase.contains("qlogo.cn") || lowerCase.contains("qpic.cn") || lowerCase.contains("gtimg.cn") || lowerCase.contains("qq.com"))) {
            return "https://" + cleanUrl.substring(7);
        }
        return cleanUrl;
    }

    public static String cleanUrl(String str) {
        if (str == null) {
            return "";
        }
        String trim = str.trim();
        if (trim.length() == 0) {
            return "";
        }
        String trim2 = trim.replace("\\/", "/").replace("\\:", ":").replace("\\=", "=").replace("\\&", "&").replace("\\", "").replaceAll("[\\s\\u00A0\\u3000\\u200B\\uFEFF\\u2007\\u202F]", "").trim();
        int indexOf = trim2.indexOf(34);
        if (indexOf > 0) {
            trim2 = trim2.substring(0, indexOf);
        }
        return trim2.trim();
    }

    public static String signNewsAuto(String str) {
        boolean z;
        if (str == null) {
            return null;
        }
        try {
            JSONObject optJSONObject = new JSONObject(str.trim()).optJSONObject("meta");
            JSONObject optJSONObject2 = optJSONObject == null ? null : optJSONObject.optJSONObject("news");
            if (optJSONObject2 == null) {
                return null;
            }
            String optString = optJSONObject2.optString("title", "");
            String optString2 = optJSONObject2.optString("desc", "");
            String safeImage = safeImage(optJSONObject2.optString("preview", ""));
            String safeJump = safeJump(optJSONObject2.optString("jumpUrl", ""));
            if (optString.length() == 0) {
                return null;
            }
            if (!str.contains("com.tencent.music.lua") && !str.contains("\"musicUrl\"")) {
                z = false;
                if (z) {
                    Result resign = resign(str);
                    if (resign.ok()) {
                        return resign.json;
                    }
                    return null;
                }
                Result signNewsFallback = signNewsFallback(optString, optString2, safeImage, safeJump);
                if (signNewsFallback != null) {
                    return signNewsFallback.json;
                }
                Result resign2 = resign(str);
                if (resign2.ok()) {
                    return resign2.json;
                }
                return null;
            }
            z = true;
            if (z) {
            }
        } catch (Throwable th) {
            HookLog.w("ArkSigner.signNewsAuto: " + th);
            return null;
        }
        return null;
    }

    public static Result signNewsFallback(String str, String str2, String str3, String str4) {
        try {
            JSONObject jSONObject = new JSONObject();
            jSONObject.put("type", "custom");
            jSONObject.put("kind", "news");
            jSONObject.put("title", str);
            String str5 = "";
            if (str2 == null) {
                str2 = "";
            }
            jSONObject.put("singer", str2);
            jSONObject.put("url", safeJump(str4));
            jSONObject.put("image", safeImage(str3));
            String postPlain = postPlain("http://106.55.0.102:10087/", jSONObject.toString());
            if (postPlain == null) {
                return null;
            }
            String trim = postPlain.trim();
            if (trim.startsWith("\"")) {
                trim = new JSONObject("{\"x\":" + trim + "}").getString("x");
            }
            if (trim.length() != 0 && trim.charAt(0) == '{') {
                JSONObject jSONObject2 = new JSONObject(trim);
                if (!"com.tencent.tuwen.lua".equals(jSONObject2.optString("app"))) {
                    return null;
                }
                JSONObject optJSONObject = jSONObject2.optJSONObject("config");
                if (optJSONObject != null) {
                    str5 = optJSONObject.optString("token");
                }
                if (str5.length() == 0) {
                    return null;
                }
                HookLog.i("ArkSigner: news 卡降级签名成功 token=" + str5);
                return new Result(jSONObject2.toString(), null);
            }
            return null;
        } catch (Throwable th) {
            HookLog.w("ArkSigner.signNewsFallback: " + th);
            return null;
        }
    }

    public static String signLinkCard(String str, String str2, String str3, String str4, String str5) {
        if (str != null) {
            try {
                if (str.length() != 0) {
                    JSONObject jSONObject = new JSONObject();
                    jSONObject.put("type", "custom");
                    jSONObject.put("title", str);
                    if (str2 == null) {
                        str2 = "";
                    }
                    jSONObject.put("singer", str2);
                    jSONObject.put("image", safeImage(str3));
                    jSONObject.put("url", safeJump(str4));
                    jSONObject.put("audio", "");
                    if (str5 != null && str5.trim().length() > 0 && !"custom".equals(str5.trim())) {
                        jSONObject.put("platform", str5.trim());
                    }
                    dumpErr("");
                    for (String str6 : MUSIC_SIGN) {
                        String postPlain = postPlain(str6, jSONObject.toString());
                        dumpErr("== " + str6 + "\n");
                        if (postPlain != null) {
                            dumpErr("返回: " + (postPlain.length() > 300 ? postPlain.substring(0, 300) : postPlain) + "\n");
                            String trim = postPlain.trim();
                            if (trim.length() != 0) {
                                if (trim.charAt(0) == '\"') {
                                    trim = new JSONObject("{\"x\":" + trim + "}").getString("x");
                                }
                                if (trim.charAt(0) == '{') {
                                    JSONObject jSONObject2 = new JSONObject(trim);
                                    String optString = jSONObject2.optString("app", "");
                                    if ("com.tencent.tuwen.lua".equals(optString) || "com.tencent.music.lua".equals(optString)) {
                                        JSONObject optJSONObject = jSONObject2.optJSONObject("config");
                                        String optString2 = optJSONObject == null ? "" : optJSONObject.optString("token");
                                        if (optString2.length() != 0) {
                                            HookLog.i("ArkSigner: 只跳转卡签名成功 app=" + optString + " token=" + optString2);
                                            return jSONObject2.toString();
                                        }
                                    }
                                }
                            }
                        } else {
                            dumpErr("返回 null（网络/异常）\n");
                        }
                    }
                    HookLog.w("ArkSigner: 只跳转卡签名全部失败");
                    dumpErr("=> 全部失败\n");
                    return null;
                }
            } catch (Throwable th) {
                HookLog.e("ArkSigner.signLinkCard 失败", th);
                return null;
            }
        }
        return null;
    }

    public static String signMusic(String str, String str2, String str3, String str4, String str5) {
        return signMusic(str, str2, str3, str4, str5, null, null, null);
    }

    public static String signMusic(String str, String str2, String str3, String str4, String str5, String str6, String str7, String str8) {
        if (str != null) {
            try {
                if (str.length() != 0) {
                    String cleanUrl = cleanUrl(str4);
                    String safeImage = safeImage(str3);
                    String safeJump = safeJump(str5);
                    if (cleanUrl.length() == 0) {
                        return null;
                    }
                    if (safeJump == null || safeJump.trim().length() == 0) {
                        safeJump = "https://music.163.com/";
                    }
                    JSONObject jSONObject = new JSONObject();
                    jSONObject.put("type", "custom");
                    jSONObject.put("title", str);
                    if (str2 == null) {
                        str2 = "";
                    }
                    jSONObject.put("singer", str2);
                    if (safeImage == null) {
                        safeImage = "";
                    }
                    jSONObject.put("image", safeImage);
                    jSONObject.put("audio", cleanUrl);
                    jSONObject.put("url", safeJump);
                    if (str6 != null && str6.trim().length() > 0) {
                        jSONObject.put("platform", str6.trim());
                    }
                    dumpErr("");
                    for (String str9 : MUSIC_SIGN) {
                        String postPlain = postPlain(str9, jSONObject.toString());
                        dumpErr("== " + str9 + "\n");
                        if (postPlain != null) {
                            dumpErr("返回: " + (postPlain.length() > 300 ? postPlain.substring(0, 300) : postPlain) + "\n");
                            String trim = postPlain.trim();
                            if (trim.length() != 0) {
                                if (trim.charAt(0) == '\"') {
                                    trim = new JSONObject("{\"x\":" + trim + "}").getString("x");
                                }
                                if (trim.charAt(0) == '{') {
                                    JSONObject jSONObject2 = new JSONObject(trim);
                                    if ("com.tencent.music.lua".equals(jSONObject2.optString("app"))) {
                                        JSONObject optJSONObject = jSONObject2.optJSONObject("config");
                                        String optString = optJSONObject == null ? "" : optJSONObject.optString("token");
                                        if (optString.length() != 0) {
                                            HookLog.i("ArkSigner: 音乐卡签名成功 token=" + optString + "（" + (str9.contains("xingzhige") ? "思思" : CH_YIBAI) + "）");
                                            if (str7 != null && str7.trim().length() > 0) {
                                                try {
                                                    JSONObject jSONObject3 = jSONObject2.getJSONObject("meta").getJSONObject("music");
                                                    jSONObject3.put("tag", str7.trim());
                                                    if (str8 != null && str8.trim().length() > 0) {
                                                        jSONObject3.put("tagIcon", cleanUrl(str8));
                                                    }
                                                    HookLog.w("ArkSigner: 角标被本地改成「" + str7.trim() + "」（实验，服务端可能不认）");
                                                } catch (Throwable th) {
                                                }
                                            }
                                            return jSONObject2.toString();
                                        }
                                    }
                                }
                            }
                        } else {
                            dumpErr("返回 null（网络/异常）\n");
                        }
                    }
                    HookLog.w("ArkSigner: 音乐卡签名全部失败");
                    dumpErr("=> 全部失败\n");
                    return null;
                }
            } catch (Throwable th2) {
                HookLog.e("ArkSigner.signMusic 失败", th2);
                return null;
            }
        }
        return null;
    }

    public static String resignMusic(String str) {
        JSONObject optJSONObject;
        JSONObject optJSONObject2;
        String str2;
        if (str == null) {
            return null;
        }
        try {
            String trim = str.trim();
            if (trim.length() < 2 || trim.charAt(0) != '{' || (optJSONObject = new JSONObject(trim).optJSONObject("meta")) == null || (optJSONObject2 = optJSONObject.optJSONObject("music")) == null) {
                return null;
            }
            String optString = optJSONObject2.optString("title", "");
            String optString2 = optJSONObject2.optString("desc", "");
            String cleanUrl = cleanUrl(optJSONObject2.optString("preview", ""));
            String cleanUrl2 = cleanUrl(optJSONObject2.optString("musicUrl", "").length() > 0 ? optJSONObject2.optString("musicUrl", "") : optJSONObject2.optString("musicurl", ""));
            String safeJump = safeJump(optJSONObject2.optString("jumpUrl", ""));
            if (cleanUrl2.length() != 0) {
                str2 = cleanUrl2;
            } else {
                str2 = optJSONObject2.optString("musicurl", "");
            }
            if (optString.length() != 0 && str2.length() != 0) {
                String optString3 = optJSONObject2.optString("tag", "");
                String cleanUrl3 = cleanUrl(optJSONObject2.optString("tagIcon", ""));
                for (String[] strArr : MUSIC_PLATFORMS) {
                    if (strArr[1].equals(optString3)) {
                        return signMusic(optString, optString2, cleanUrl, str2, safeJump, strArr[0], null, null);
                    }
                }
                if (optString3.length() > 0) {
                    return signMusic(optString, optString2, cleanUrl, str2, safeJump, null, optString3, cleanUrl3);
                }
                return signMusic(optString, optString2, cleanUrl, str2, safeJump);
            }
            return null;
        } catch (Throwable th) {
            HookLog.w("ArkSigner.resignMusic: " + th);
            return null;
        }
    }

    public static String channelLabel(String str) {
        int i = 0;
        while (true) {
            String[] strArr = CH_IDS;
            if (i >= strArr.length) {
                return CH_LABELS[0];
            }
            if (strArr[i].equals(str)) {
                return CH_LABELS[i];
            }
            i++;
        }
    }

    public static String channelShort(String str) {
        return CH_TIANXUAN.equals(str) ? "天玄" : CH_YIBAI.equals(str) ? "yibai 备用" : CH_SISI.equals(str) ? "思思音卡" : "自动";
    }

    public static boolean isServerSignedCard(String str) {
        String trim;
        if (str == null) {
            trim = "";
        } else {
            try {
                trim = str.trim();
            } catch (Throwable th) {
                return false;
            }
        }
        if (!trim.startsWith("{")) {
            return false;
        }
        JSONObject jSONObject;
        try {
            jSONObject = new JSONObject(trim);
        } catch (Throwable th) {
            return false;
        }
        String optString = jSONObject.optString("app", "");
        if (optString.length() != 0 && !"com.tencent.tuwen.lua".equals(optString) && !"com.tencent.music.lua".equals(optString)) {
            if (jSONObject.optString("token", "").length() > 0) {
                return true;
            }
            JSONObject optJSONObject = jSONObject.optJSONObject("config");
            if (optJSONObject != null) {
                return optJSONObject.optString("token", "").length() > 0;
            }
            return false;
        }
        return false;
    }

    public static boolean isMusicCard(String str) {
        String trim;
        if (str == null) {
            trim = "";
        } else {
            try {
                trim = str.trim();
            } catch (Throwable th) {
                return true;
            }
        }
        if (trim.length() >= 2 && trim.charAt(0) == '{') {
            JSONObject metaOf;
            try {
                metaOf = metaOf(new JSONObject(trim));
            } catch (Throwable th) {
                return false;
            }
            if (metaOf != null && metaOf.optJSONObject("music") != null) {
                return true;
            }
            return trim.contains("\"musicUrl\"");
        }
        return false;
    }

    private static JSONObject metaOf(JSONObject jSONObject) {
        JSONObject optJSONObject = jSONObject.optJSONObject("meta");
        if (optJSONObject != null) {
            return optJSONObject;
        }
        String optString = jSONObject.optString("meta", null);
        if (optString != null && optString.length() > 2) {
            try {
                return new JSONObject(optString);
            } catch (Throwable th) {
            }
        }
        return null;
    }

    public static Result signByChannel(String str, String str2) {
        String trim;
        String str3;
        if (str == null) {
            trim = "";
        } else {
            try {
                trim = str.trim();
            } catch (Throwable th) {
                HookLog.e("ArkSigner.signByChannel 失败", th);
                return new Result(null, String.valueOf(th));
            }
        }
        if (trim.length() >= 2 && trim.charAt(0) == '{') {
            if (isServerSignedCard(trim)) {
                HookLog.i("ArkSigner: 服务端签过的卡（structmsg），原样发");
                return new Result(null, SKIP);
            }
            if (str2 != null && str2.trim().length() != 0) {
                str3 = str2.trim();
                boolean isMusicCard = isMusicCard(trim);

                if (CH_SISI.equals(str3)) {
                    return signVia(trim, HOST_SISI, isMusicCard, false);
                }
                if (CH_YIBAI.equals(str3)) {

                    return signVia(trim, HOST_SISI, isMusicCard, false);
                }
                if (CH_TIANXUAN.equals(str3)) {
                    if (isMusicCard) {
                        return new Result(null, "天玄不签音乐卡，换「思思音卡」通道");
                    }
                    return resign(trim);
                }

                if (isMusicCard) {
                    Result r1 = signVia(trim, HOST_SISI, true, true);
                    if (r1.ok()) return r1;
                    return new Result(null, "音乐卡签名失败（思思）：" + r1.error);
                }
                Result resign = resign(trim);
                if (resign.ok() || SKIP.equals(resign.error)) return resign;
                return new Result(null, "签名失败（天玄）：" + resign.error);
            }
            str3 = CH_AUTO;
            boolean isMusicCard2 = isMusicCard(trim);
            if (!CH_TIANXUAN.equals(str3)) {
            }
        }
        return new Result(null, "不是合法的卡片 JSON");
    }

    private static String enc(String str) {
        if (str == null) {
            str = "";
        }
        try {
            return URLEncoder.encode(str, "UTF-8");
        } catch (Throwable th) {
            return "";
        }
    }

    private static String getPlain(String str) {
        HttpURLConnection httpURLConnection;
        try {
            httpURLConnection = (HttpURLConnection) new URL(str).openConnection(Proxy.NO_PROXY);
            try {
                httpURLConnection.setRequestMethod("GET");
                httpURLConnection.setConnectTimeout(8000);
                httpURLConnection.setReadTimeout(NetApi.DEFAULT_TIMEOUT);
                httpURLConnection.setRequestProperty("User-Agent", UA);
                InputStream errorStream = httpURLConnection.getResponseCode() >= 400 ? httpURLConnection.getErrorStream() : httpURLConnection.getInputStream();
                if (errorStream == null) {
                    return null;
                }
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                byte[] bArr = new byte[4096];
                while (true) {
                    int read = errorStream.read(bArr);
                    if (read <= 0) {
                        break;
                    }
                    byteArrayOutputStream.write(bArr, 0, read);
                }
                errorStream.close();
                String byteArrayOutputStream2 = byteArrayOutputStream.toString("UTF-8");
                if (httpURLConnection != null) {
                    httpURLConnection.disconnect();
                }
                return byteArrayOutputStream2;
            } catch (Throwable th) {

                try {
                    HookLog.w("getPlain 失败: " + th);
                    if (httpURLConnection != null) {
                        httpURLConnection.disconnect();
                    }
                    return null;
                } finally {
                    if (httpURLConnection != null) {
                        httpURLConnection.disconnect();
                    }
                }
            }
        } catch (Throwable th2) {
            Throwable th = th2;
            httpURLConnection = null;
        }
        return null;
    }

    private static Result signVia(String str, String str2, boolean z, boolean z2) {
        String str3;
        String substring;
        String str4 = "";
        try {
            JSONObject metaOf = metaOf(new JSONObject(str));
            if (metaOf == null) {
                return new Result(null, "卡里没有 meta");
            }
            JSONObject optJSONObject = z ? metaOf.optJSONObject("music") : metaOf.optJSONObject("news");
            if (optJSONObject == null) {
                optJSONObject = metaOf.optJSONObject("news");
            }
            if (optJSONObject == null) {
                return new Result(null, "卡里找不到 news/music 内容");
            }
            String optString = optJSONObject.optString("title", "");
            if (optString.length() == 0) {
                return new Result(null, "标题是空的");
            }
            String optString2 = optJSONObject.optString("desc", "");
            String cleanUrl = cleanUrl(optJSONObject.optString("preview", ""));
            String optString3 = optJSONObject.optString("jumpUrl", "");
            if (!z) {
                str3 = "";
            } else {
                str3 = cleanUrl(optJSONObject.optString("musicUrl", ""));
                if (str3.length() == 0) {
                    str3 = cleanUrl(optJSONObject.optString("musicurl", ""));
                }
                if (str3.length() == 0) {
                    str3 = cleanUrl(optJSONObject.optString("audio", ""));
                }
                if (str3.length() == 0) {
                    return new Result(null, "音乐卡的音频地址是空的");
                }
            }
            JSONObject jSONObject = new JSONObject();
            jSONObject.put("type", "custom");
            jSONObject.put("title", optString);
            jSONObject.put("singer", optString2);
            jSONObject.put("image", safeImage(cleanUrl));
            jSONObject.put("audio", str3);
            jSONObject.put("url", safeJump(optString3));
            String optString4 = optJSONObject.optString("tag", "");
            String[][] strArr = MUSIC_PLATFORMS;
            int length = strArr.length;
            int i = 0;
            while (true) {
                if (i >= length) {
                    break;
                }
                String[] strArr2 = strArr[i];
                if (!strArr2[1].equals(optString4)) {
                    i++;
                } else {
                    jSONObject.put("platform", strArr2[0]);
                    break;
                }
            }
            if (!z2) {
                dumpErr("== " + str2 + "\n");
            }
            String postPlain = postPlain(str2, jSONObject.toString());
            if (!z2) {
                StringBuilder append = new StringBuilder().append("返回: ");
                if (postPlain == null) {
                    substring = "null";
                } else {
                    substring = postPlain.length() > 300 ? postPlain.substring(0, 300) : postPlain;
                }
                dumpErr(append.append(substring).append("\n").toString());
            }
            if (postPlain == null) {
                return new Result(null, "网络不通");
            }
            String trim = postPlain.trim();
            if (trim.length() == 0) {
                return new Result(null, "服务返回空");
            }
            if (trim.charAt(0) == '\"') {
                trim = new JSONObject("{\"x\":" + trim + "}").getString("x");
            }
            if (trim.length() != 0 && trim.charAt(0) == '{') {
                JSONObject jSONObject2 = new JSONObject(trim);
                String optString5 = jSONObject2.optString("app", "");
                if (!"com.tencent.tuwen.lua".equals(optString5) && !"com.tencent.music.lua".equals(optString5)) {
                    return new Result(null, "服务回的 app 不对：" + optString5);
                }
                if (z && !"com.tencent.music.lua".equals(optString5)) {
                    return new Result(null, "服务把音乐卡回成了 news 卡");
                }
                JSONObject optJSONObject2 = jSONObject2.optJSONObject("config");
                if (optJSONObject2 != null) {
                    str4 = optJSONObject2.optString("token");
                }
                if (str4.length() == 0) {
                    return new Result(null, "没拿到 token");
                }
                HookLog.i("ArkSigner: 签名成功 app=" + optString5 + " host=" + str2 + " token=" + str4);
                return new Result(jSONObject2.toString(), null);
            }
            if (trim.length() > 40) {
                trim = trim.substring(0, 40);
            }
            return new Result(null, "服务拒绝：" + trim);
        } catch (Throwable th) {
            HookLog.w("ArkSigner.signVia(" + str2 + "): " + th);
            return new Result(null, String.valueOf(th));
        }
    }

    public static final class PskeyResult {
        public final String msg;
        public final boolean ok;
        public final String pskey;

        PskeyResult(boolean z, String str, String str2) {
            this.ok = z;
            this.pskey = str;
            this.msg = str2;
        }
    }

    public static boolean openPskeyPage(Context context) {
        if (context == null) {
            return false;
        }
        try {
            Intent intent = new Intent("android.intent.action.VIEW", Uri.parse(PSKEY_PAGE));
            intent.addFlags(268435456);
            try {
                intent.setPackage(context.getPackageName());
            } catch (Throwable th) {
            }
            context.startActivity(intent);
            HookLog.i("ArkSigner: 已打开 https://qun.qq.com/");
            return true;
        } catch (Throwable th2) {
            HookLog.w("ArkSigner.openPskeyPage: " + th2);
            try {
                Intent intent2 = new Intent("android.intent.action.VIEW", Uri.parse(PSKEY_PAGE));
                intent2.setClassName(context.getPackageName(), "com.tencent.mobileqq.activity.JumpActivity");
                intent2.addFlags(268435456);
                context.startActivity(intent2);
                HookLog.i("ArkSigner: 用 JumpActivity 打开");
                return true;
            } catch (Throwable th3) {
                HookLog.w("ArkSigner.openPskeyPage 兜底也失败: " + th3);
                return false;
            }
        }
    }

    public static PskeyResult capturePskey() {
        return readPskey(true);
    }

    public static PskeyResult readPskey(boolean z) {
        try {
            String cookieFromManager = cookieFromManager();
            String pskeyOf = pskeyOf(cookieFromManager);
            String str = "内存 cookie";
            if (pskeyOf == null || pskeyOf.length() == 0) {
                cookieFromManager = cookieFromDb();
                pskeyOf = pskeyOf(cookieFromManager);
                str = "webview 数据库";
            }
            if (z && (pskeyOf == null || pskeyOf.length() == 0)) {
                String cookieFromDb = (cookieFromManager == null || cookieFromManager.length() <= 0) ? cookieFromDb() : cookieFromManager;
                String autoPskey = autoPskey(cookieFromDb);
                if (autoPskey != null && autoPskey.length() > 0) {
                    str = "自动补";
                    cookieFromManager = cookieFromDb;
                    pskeyOf = autoPskey;
                }
            }
            if (pskeyOf != null && pskeyOf.length() != 0) {
                saveCookie(cookieFromManager);
                HookLog.i("ArkSigner: 抓到 p_skey，来源=" + str);
                return new PskeyResult(true, pskeyOf, "已拿到 p_skey（" + str + " " + head16(pskeyOf) + "）");
            }
            String pskeyOf2 = pskeyOf(cachedCookie());
            if (pskeyOf2 != null && pskeyOf2.length() > 0) {
                return new PskeyResult(true, pskeyOf2, "用的本地缓存 p_skey（" + head16(pskeyOf2) + "）");
            }
            return new PskeyResult(false, null, "没拿到 p_skey —— 先点「打开页面」进一次 qun.qq.com");
        } catch (Throwable th) {
            HookLog.w("ArkSigner.readPskey: " + th);
            return new PskeyResult(false, null, String.valueOf(th));
        }
    }

    public static PskeyResult pskeyStatus() {
        try {
            String pskeyOf = pskeyOf(cachedCookie());
            if (pskeyOf != null && pskeyOf.length() != 0) {
                return new PskeyResult(true, pskeyOf, "有缓存 p_skey（" + head16(pskeyOf) + "，可能已过期）");
            }
            String pskeyOf2 = pskeyOf(cookieFromDb());
            if (pskeyOf2 != null && pskeyOf2.length() > 0) {
                return new PskeyResult(true, pskeyOf2, "有 p_skey（webview " + head16(pskeyOf2) + "）");
            }
            return new PskeyResult(false, null, "本地还没有 p_skey");
        } catch (Throwable th) {
            return new PskeyResult(false, null, String.valueOf(th));
        }
    }

    private static String minimalNewsCard() {
        return "{\"app\":\"com.tencent.tuwen.lua\",\"view\":\"news\",\"meta\":{\"news\":{\"title\":\"p_skey 校验\",\"desc\":\"签名通道自检\",\"tag\":\"QQ音乐\",\"tagIcon\":\"https://tianxuan.gtimg.cn/47329_bd95e16e/assets/guild-icon.png\",\"preview\":\"https://tianxuan.gtimg.cn/47329_bd95e16e/assets/guild-icon.png\",\"jumpUrl\":\"https://qq.com\"}}}";
    }

    public static PskeyResult verifyPskey() {
        try {
            String pskeyOf = pskeyOf(cachedCookie());
            if (pskeyOf == null || pskeyOf.length() == 0) {
                pskeyOf = pskeyOf(cookieFromDb());
            }
            if (pskeyOf == null || pskeyOf.length() == 0) {
                pskeyOf = pskeyOf(cookieFromManager());
            }
            if (pskeyOf != null && pskeyOf.length() != 0) {
                Result resign = resign(minimalNewsCard());
                if (resign.ok()) {
                    return new PskeyResult(true, pskeyOf, "p_skey 有效（" + head16(pskeyOf) + "，能签）");
                }
                if (SKIP.equals(resign.error)) {
                    return new PskeyResult(false, pskeyOf, "校验卡被当成非 news 卡，跳过");
                }
                String str = resign.error == null ? "" : resign.error;
                if (!str.contains("4003") && !str.contains("4002") && !str.contains("invalid") && !str.equals(ERR_NO_PSKEY)) {
                    if (!str.contains("10009") && !str.contains("validate") && !str.contains("GenTemplateArk")) {
                        return new PskeyResult(false, pskeyOf, "校验没通过：" + str);
                    }
                    return new PskeyResult(true, pskeyOf, "p_skey 有效（" + head16(pskeyOf) + "）");
                }
                return new PskeyResult(false, pskeyOf, "p_skey 已失效（" + head16(pskeyOf) + "），点「一键打开」重新拿");
            }
            return new PskeyResult(false, null, "本地没有 p_skey，先点「打开页面」拿一个");
        } catch (Throwable th) {
            return new PskeyResult(false, null, String.valueOf(th));
        }
    }

    public static void clearPskey() {
        try {
            saveCookie("");
            Context appContext = QQEnv.appContext();
            if (appContext != null) {
                HookConfig.set(appContext, KEY_COOKIE, "");
            }
        } catch (Throwable th) {
        }
    }

    private static String head16(String str) {
        return str == null ? "" : str.length() > 8 ? str.substring(0, 8) + "…" : str;
    }

    public static synchronized void dumpSent(String str, String str2) {
        Context appContext = null;
        synchronized (ArkSigner.class) {
            try {
                appContext = QQEnv.appContext();
            } catch (Throwable th) {
            }
            if (appContext == null) {
                return;
            }
            File file = new File(appContext.getFilesDir().getParent(), "files/cathelper");
            if (!file.exists()) {
                file.mkdirs();
            }
            File file2 = new File(file, "music_card_sent.txt");
            if (file2.length() > 60000) {
                file2.delete();
            }
            String str3 = "";
            String str4 = "";
            String str5 = "";
            try {
                JSONObject jSONObject = new JSONObject(str2);
                JSONObject optJSONObject = jSONObject.optJSONObject("config");
                str3 = optJSONObject == null ? "" : optJSONObject.optString("token", "");
                JSONObject optJSONObject2 = jSONObject.optJSONObject("meta");
                JSONObject optJSONObject3 = optJSONObject2 == null ? null : optJSONObject2.optJSONObject("music");
                if (optJSONObject3 != null) {
                    str4 = optJSONObject3.optString("title", "");
                    str5 = optJSONObject3.optString("musicUrl", "");
                }
            } catch (Throwable th2) {
            }
            StringBuilder append = new StringBuilder().append(new SimpleDateFormat(FeatureMsgTime.DEF_FORMAT, Locale.US).format(new Date())).append(" [").append(str).append("] token=");
            if (str3.length() > 8) {
                str3 = str3.substring(0, 8) + "…";
            } else if (str3.length() == 0) {
                str3 = "无";
            }
            StringBuilder append2 = append.append(str3).append(" title=").append(str4).append(" audio=");
            if (str5.length() > 60) {
                str5 = str5.substring(0, 60) + "…";
            }
            String sb = append2.append(str5).append("\n").toString();
            try {
                FileOutputStream fileOutputStream = new FileOutputStream(file2, true);
                fileOutputStream.write(sb.getBytes(UTF8));
                fileOutputStream.flush();
                fileOutputStream.close();
            } catch (Throwable th) {
            }
        }
    }

    private static synchronized void dumpErr(String str) {
        Context appContext = null;
        synchronized (ArkSigner.class) {
            try {
                appContext = QQEnv.appContext();
            } catch (Throwable th) {
            }
            if (appContext == null) {
                return;
            }
            File file = new File(appContext.getFilesDir().getParent(), "files/cathelper");
            if (!file.exists()) {
                file.mkdirs();
            }
            File file2 = new File(file, "music_sign_err.txt");
            if (str != null && str.length() != 0) {
                try {
                    FileOutputStream fileOutputStream = new FileOutputStream(file2, true);
                    fileOutputStream.write(str.getBytes(UTF8));
                    fileOutputStream.flush();
                    fileOutputStream.close();
                } catch (Throwable th) {
                }
                return;
            }
            if (file2.exists()) {
                file2.delete();
            }
        }
    }

    private static String postPlain(String str, String str2) {
        HttpURLConnection httpURLConnection;
        try {
            httpURLConnection = (HttpURLConnection) new URL(str).openConnection(Proxy.NO_PROXY);
            try {
                httpURLConnection.setRequestMethod("POST");
                httpURLConnection.setConnectTimeout(8000);
                httpURLConnection.setReadTimeout(NetApi.DEFAULT_TIMEOUT);
                httpURLConnection.setDoInput(true);
                httpURLConnection.setDoOutput(true);
                httpURLConnection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                httpURLConnection.setRequestProperty("User-Agent", UA);

                try {
                    httpURLConnection.setRequestProperty("Accept", "application/json, text/plain, */*");
                    httpURLConnection.setRequestProperty("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
                    httpURLConnection.setRequestProperty("Origin", "https://ss.xingzhige.com");
                    httpURLConnection.setRequestProperty("Referer", "https://ss.xingzhige.com/");
                    httpURLConnection.setRequestProperty("Connection", "keep-alive");
                } catch (Throwable ignore) {
                }
                OutputStream outputStream = httpURLConnection.getOutputStream();
                outputStream.write(str2.getBytes(UTF8));
                outputStream.flush();
                outputStream.close();
                InputStream errorStream = httpURLConnection.getResponseCode() >= 400 ? httpURLConnection.getErrorStream() : httpURLConnection.getInputStream();
                if (errorStream == null) {
                    return null;
                }
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                byte[] bArr = new byte[4096];
                while (true) {
                    int read = errorStream.read(bArr);
                    if (read <= 0) {
                        break;
                    }
                    byteArrayOutputStream.write(bArr, 0, read);
                }
                errorStream.close();
                String byteArrayOutputStream2 = byteArrayOutputStream.toString("UTF-8");
                if (httpURLConnection != null) {
                    httpURLConnection.disconnect();
                }
                return byteArrayOutputStream2;
            } catch (Throwable th) {

                try {
                    HookLog.w("ArkSigner.postPlain(" + str + "): " + th);
                    dumpErr("postPlain " + str + " 抛异常: " + th + "\n");
                    if (httpURLConnection != null) {
                        httpURLConnection.disconnect();
                    }
                    return null;
                } finally {
                    if (httpURLConnection != null) {
                        httpURLConnection.disconnect();
                    }
                }
            }
        } catch (Throwable th2) {
            Throwable th = th2;
            httpURLConnection = null;
        }
        return null;
    }

    public static String autoPskey(String str) {
        HttpURLConnection httpURLConnection;
        int indexOf;
        if (str == null || str.length() == 0) {
            return null;
        }
        String str2 = PSKEY_PAGE;
        for (int i = 0; i < 6 && str2 != null; i++) {
            try {
                try {
                    httpURLConnection = (HttpURLConnection) new URL(str2).openConnection(Proxy.NO_PROXY);
                    try {
                        httpURLConnection.setInstanceFollowRedirects(false);
                        httpURLConnection.setConnectTimeout(7000);
                        httpURLConnection.setReadTimeout(7000);
                        httpURLConnection.setRequestProperty("User-Agent", UA);
                        httpURLConnection.setRequestProperty("Cookie", str);
                        int responseCode = httpURLConnection.getResponseCode();
                        List<String> list = httpURLConnection.getHeaderFields().get("Set-Cookie");
                        if (list == null) {
                            list = httpURLConnection.getHeaderFields().get("set-cookie");
                        }
                        if (list != null) {
                            for (String str3 : list) {
                                if (str3 != null && (indexOf = str3.indexOf("p_skey=")) >= 0) {
                                    int indexOf2 = str3.indexOf(59, indexOf);
                                    String substring = indexOf2 > 0 ? str3.substring(indexOf + 7, indexOf2) : str3.substring(indexOf + 7);
                                    if (substring != null && substring.trim().length() > 0) {
                                        dumpErr("autoPskey 成功（第 " + (i + 1) + " 跳）\n");
                                        String trim = substring.trim();
                                        if (httpURLConnection != null) {
                                            try {
                                                httpURLConnection.disconnect();
                                            } catch (Throwable th) {
                                            }
                                        }
                                        return trim;
                                    }
                                }
                            }
                        }
                        String headerField = httpURLConnection.getHeaderField("Location");
                        if (responseCode >= 300 && responseCode < 400 && headerField != null) {
                            str2 = new URL(new URL(str2), headerField).toString();
                        } else {
                            str2 = null;
                        }
                        if (httpURLConnection != null) {
                            try {
                                httpURLConnection.disconnect();
                            } catch (Throwable th2) {
                            }
                        }
                        if (str2 == null) {
                            break;
                        }
                    } catch (Throwable th3) {
                        Throwable th = th3;
                        if (httpURLConnection != null) {
                            try {
                                httpURLConnection.disconnect();
                            } catch (Throwable th4) {
                            }
                        }
                        throw th;
                    }
                } catch (Throwable th5) {
                    Throwable th = th5;
                    httpURLConnection = null;
                }
            } catch (Throwable th6) {
                dumpErr("autoPskey 异常: " + th6 + "\n");
            }
        }
        dumpErr("autoPskey 没拿到（可能未登录/被重定向到登录页）\n");
        return null;
    }

    private static String cachedCookie() {
        String str;
        try {
            Context appContext = QQEnv.appContext();
            if (appContext == null || (str = HookConfig.get(appContext, KEY_COOKIE, "")) == null) {
                return null;
            }
            if (str.length() > 0) {
                return str;
            }
            return null;
        } catch (Throwable th) {
            return null;
        }
    }

    private static void saveCookie(String str) {
        try {
            Context appContext = QQEnv.appContext();
            if (appContext != null && str != null && str.length() != 0) {
                HookConfig.set(appContext, KEY_COOKIE, str);
            }
        } catch (Throwable th) {
        }
    }

    public static String cookieFromManager() {
        try {
            CookieManager cookieManager = CookieManager.getInstance();
            String cookie = cookieManager.getCookie(PSKEY_PAGE);
            return (cookie == null || cookie.length() == 0) ? cookieManager.getCookie("https://qun.qq.com") : cookie;
        } catch (Throwable th) {
            HookLog.w("ArkSigner: CookieManager 不可用 " + th);
            return null;
        }
    }

    public static String pskeyOf(String str) {
        if (str == null) {
            return null;
        }
        for (String str2 : str.split(";")) {
            String trim = str2.trim();
            int indexOf = trim.indexOf(61);
            if (indexOf > 0 && "p_skey".equals(trim.substring(0, indexOf))) {
                return trim.substring(indexOf + 1);
            }
        }
        return null;
    }

    public static String cookieFromDb() {
        String parent;
        SQLiteDatabase sQLiteDatabase;
        Cursor cursor;
        Context appContext = QQEnv.appContext();
        if (appContext == null || (parent = appContext.getFilesDir().getParent()) == null) {
            return null;
        }
        String[] strArr = {"app_webview_tool", "app_webview", "app_webview_mini", "app_webview_mini1", "app_webview_qqfav"};
        LinkedHashMap linkedHashMap = new LinkedHashMap();
        String[] strArr2 = {".qun.qq.com", ".qq.com"};
        for (int i = 0; i < 2; i++) {
            String str = strArr2[i];
            for (int i2 = 0; i2 < 5; i2++) {
                File file = new File(parent + "/" + strArr[i2] + "/Default/Cookies");
                if (file.exists() && file.canRead()) {
                    try {
                        sQLiteDatabase = SQLiteDatabase.openDatabase(file.getAbsolutePath(), null, 1);
                        try {
                            cursor = sQLiteDatabase.rawQuery("select name,value from cookies where host_key=? and value!=''", new String[]{str});
                            while (cursor.moveToNext()) {
                                try {
                                    String string = cursor.getString(0);
                                    String string2 = cursor.getString(1);
                                    if (string != null && string2 != null && !linkedHashMap.containsKey(string)) {
                                        linkedHashMap.put(string, string2);
                                    }
                                } catch (Throwable th) {

                                    try {
                                        HookLog.w("ArkSigner: 读 " + file.getName() + " 失败 " + th);
                                        if (cursor != null) {
                                            try {
                                                cursor.close();
                                            } catch (Throwable th2) {
                                            }
                                        }
                                        if (sQLiteDatabase != null) {
                                            sQLiteDatabase.close();
                                        }
                                    } finally {
                                    }
                                }
                            }
                            if (cursor != null) {
                                try {
                                    cursor.close();
                                } catch (Throwable th3) {
                                }
                            }
                            if (sQLiteDatabase != null) {
                                try {
                                    sQLiteDatabase.close();
                                } catch (Throwable th4) {
                                }
                            }
                        } catch (Throwable th5) {
                            Throwable th = th5;
                            cursor = null;
                        }
                    } catch (Throwable th6) {
                        Throwable th = th6;
                        sQLiteDatabase = null;
                        cursor = null;
                    }
                }
            }
        }
        if (linkedHashMap.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (Object entryObj : linkedHashMap.entrySet()) {
            Map.Entry entry = (Map.Entry) entryObj;
            if (sb.length() > 0) {
                sb.append("; ");
            }
            sb.append((String) entry.getKey()).append(SignatureVisitor.INSTANCEOF).append((String) entry.getValue());
        }
        return sb.toString();
    }

    private static String post(String str, String str2, String str3) {
        HttpURLConnection httpURLConnection;
        try {
            httpURLConnection = (HttpURLConnection) new URL(str).openConnection(Proxy.NO_PROXY);
            try {
                httpURLConnection.setRequestMethod("POST");
                httpURLConnection.setConnectTimeout(8000);
                httpURLConnection.setReadTimeout(12000);
                httpURLConnection.setDoInput(true);
                httpURLConnection.setDoOutput(true);
                httpURLConnection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                httpURLConnection.setRequestProperty("X-Request-With", Host.PKG_QQ);
                httpURLConnection.setRequestProperty("x-oidb", "{\"uint32_service_type\":2,\"uint32_command\":\"0x9064\"}");
                httpURLConnection.setRequestProperty("Referer", REFERER);
                httpURLConnection.setRequestProperty("Cookie", str3);
                httpURLConnection.setRequestProperty("User-Agent", UA);
                OutputStream outputStream = httpURLConnection.getOutputStream();
                outputStream.write(str2.getBytes(UTF8));
                outputStream.flush();
                outputStream.close();
                InputStream errorStream = httpURLConnection.getResponseCode() >= 400 ? httpURLConnection.getErrorStream() : httpURLConnection.getInputStream();
                if (errorStream == null) {
                    return null;
                }
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                byte[] bArr = new byte[4096];
                while (true) {
                    int read = errorStream.read(bArr);
                    if (read <= 0) {
                        break;
                    }
                    byteArrayOutputStream.write(bArr, 0, read);
                }
                errorStream.close();
                String byteArrayOutputStream2 = byteArrayOutputStream.toString("UTF-8");
                if (httpURLConnection != null) {
                    httpURLConnection.disconnect();
                }
                return byteArrayOutputStream2;
            } catch (Throwable th) {

                try {
                    HookLog.w("ArkSigner.post: " + th);
                    if (httpURLConnection != null) {
                        httpURLConnection.disconnect();
                    }
                    return null;
                } finally {
                    if (httpURLConnection != null) {
                        httpURLConnection.disconnect();
                    }
                }
            }
        } catch (Throwable th2) {
            Throwable th = th2;
            httpURLConnection = null;
        }
        return null;
    }

    private static String appIdOf() {
        String str;
        try {
            Context appContext = QQEnv.appContext();
            return (appContext == null || (str = HookConfig.get(appContext, K_MINI_APPID, "")) == null || str.trim().length() <= 0) ? BILI_APPID : str.trim();
        } catch (Throwable th) {
            return BILI_APPID;
        }
    }

    public static String qAppId() {
        String str;
        try {
            Context appContext = QQEnv.appContext();
            return (appContext == null || (str = HookConfig.get(appContext, "wxmini_qappid", "")) == null || str.trim().length() <= 0) ? BILI_APPID : str.trim();
        } catch (Throwable th) {
            return BILI_APPID;
        }
    }

    private static String appNameOf() {
        String str;
        try {
            Context appContext = QQEnv.appContext();
            return (appContext == null || (str = HookConfig.get(appContext, K_MINI_APPNAME, "")) == null || str.trim().length() <= 0) ? "哔哩哔哩" : str.trim();
        } catch (Throwable th) {
            return "哔哩哔哩";
        }
    }

    public static synchronized void installMiniShare(ClassLoader classLoader) {
        synchronized (ArkSigner.class) {
            if (miniHooked || classLoader == null) {
                return;
            }
            try {
                int hookMiniShare = hookMiniShare("com.tencent.mobileqq.mini.share.MiniProgramOpenSdkUtil", "buildShareInfoRequest", classLoader) + 0 + hookMiniShare("com.tencent.mobileqq.mini.share.MiniProgramShareUtils", "newShareInfoRequest", classLoader);
                miniHooked = hookMiniShare > 0;
                miniDbg("install ok=" + hookMiniShare);
                hookMiniRsp(classLoader);
                HookLog.i("MiniShare: 签名请求构造点挂了 " + hookMiniShare + " 个");
            } catch (Throwable th) {
                HookLog.w("MiniShare.install: " + th);
            }
        }
    }

    private static int hookMiniShare(String str, String str2, ClassLoader classLoader) {
        boolean z;
        Class<?> findClass = HookApi.findClass(classLoader, str);
        if (findClass == null) {
            miniDbg("找不到 " + str);
            return 0;
        }
        int i = 0;
        for (Method method : findClass.getDeclaredMethods()) {
            if (str2.equals(method.getName()) && method.getReturnType().getName().endsWith("StAdaptShareInfoReq")) {
                Class<?>[] parameterTypes = method.getParameterTypes();
                int length = parameterTypes.length;
                int i2 = 0;
                while (true) {
                    if (i2 >= length) {
                        z = false;
                        break;
                    }
                    if (!parameterTypes[i2].getName().endsWith("MiniArkShareModel")) {
                        i2++;
                    } else {
                        z = true;
                        break;
                    }
                }
                if (z) {
                    try {
                        method.setAccessible(true);
                    } catch (Throwable th) {
                    }
                    if (HookApi.after(method, new HookApi.Action() {
                        @Override
                        public void call(XC_MethodHook.MethodHookParam methodHookParam) {
                            try {
                                ArkSigner.onMiniShareReq(methodHookParam.getResult());
                            } catch (Throwable th2) {
                                HookLog.w("MiniShare: " + th2);
                            }
                        }
                    }) != null) {
                        i++;
                        miniDbg("挂上 " + str + "." + str2 + "(" + method.getParameterTypes().length + "参)");
                    }
                }
            }
        }
        return i;
    }

    private static void hookMiniRsp(ClassLoader classLoader) {
        String[] strArr = {"com.tencent.mobileqq.mini.share.b", "com.tencent.mobileqq.mini.share.MiniProgramOpenSdkUtil$3"};
        int i = 0;
        for (int i2 = 0; i2 < 2; i2++) {
            String str = strArr[i2];
            Class<?> findClass = HookApi.findClass(classLoader, str);
            if (findClass != null) {
                for (Method method : findClass.getDeclaredMethods()) {
                    Class<?>[] parameterTypes = method.getParameterTypes();
                    if (parameterTypes.length >= 2 && parameterTypes[parameterTypes.length - 1] == JSONObject.class) {
                        try {
                            method.setAccessible(true);
                        } catch (Throwable th) {
                        }
                        if (HookApi.before(method, new HookApi.Action() {
                            @Override
                            public void call(XC_MethodHook.MethodHookParam methodHookParam) {
                                try {
                                    Object obj = methodHookParam.args[methodHookParam.args.length - 1];
                                    if (obj instanceof JSONObject) {
                                        ArkSigner.onMiniRsp((JSONObject) obj);
                                    }
                                } catch (Throwable th2) {
                                    HookLog.w("MiniShare.rsp: " + th2);
                                }
                            }
                        }) != null) {
                            i++;
                            HookLog.i("MiniShare: 响应钩子挂上 " + str + "#" + method.getName());
                        }
                    }
                }
            } else {
                HookLog.w("MiniShare: 找不到 " + str);
            }
        }
        miniDbg("响应钩子挂上 " + i + " 个");
    }

    public static void onMiniRsp(JSONObject jSONObject) {
        String str;
        if (jSONObject == null) {
            return;
        }
        miniDbg("RSP " + jSONObject.toString());
        try {
            Context appContext = QQEnv.appContext();
            if (appContext != null && HookConfig.getBoolean(appContext, K_MINI, false) && (str = HookConfig.get(appContext, K_MINI_WEB, "")) != null && str.length() != 0) {
                JSONObject optJSONObject = jSONObject.optJSONObject("meta");
                if (optJSONObject == null) {
                    optJSONObject = jSONObject.optJSONObject("metaData");
                }
                if (optJSONObject == null) {
                    return;
                }
                Iterator<String> keys = optJSONObject.keys();
                while (keys.hasNext()) {
                    JSONObject optJSONObject2 = optJSONObject.optJSONObject(keys.next());
                    if (optJSONObject2 != null && optJSONObject2.has("url")) {
                        String optString = optJSONObject2.optString("url", "");
                        optJSONObject2.put("url", stripScheme(str));
                        if (optJSONObject2.has("qqdocurl")) {
                            optJSONObject2.put("qqdocurl", str);
                        }
                        miniDbg("改 ark url: " + optString + " -> " + stripScheme(str));
                    }
                }
            }
        } catch (Throwable th) {
            miniDbg("onMiniRsp 异常: " + th);
        }
    }

    public static void onMiniShareReq(Object obj) {
        Context appContext = null;
        if (obj == null || (appContext = QQEnv.appContext()) == null || !HookConfig.getBoolean(appContext, K_MINI, false)) {
            return;
        }
        String pbGet = pbGet(obj, "appid");
        String pbGet2 = pbGet(obj, "title");
        String pbGet3 = pbGet(obj, "desc");
        String pbGet4 = pbGet(obj, "picUrl");
        String pbGet5 = pbGet(obj, "jumpUrl");
        boolean objArr = BILI_APPID.equals(pbGet) || (pbGet2 != null && pbGet2.contains("哔哩哔哩")) || (pbGet2 != null && pbGet2.contains("bilibili"));
        if (HookConfig.getBoolean(appContext, K_MINI_BILI, true) && objArr == false) {
            miniDbg("跳过（非B站）appid=" + pbGet + " title=" + pbGet2);
        } else {
            miniDbg("改写 " + ((pbSet(obj, "title", HookConfig.get(appContext, K_MINI_TITLE, "")) ? 1 : 0) + 0 + (pbSet(obj, "desc", HookConfig.get(appContext, K_MINI_DESC, "")) ? 1 : 0) + (pbSet(obj, "picUrl", stripScheme(HookConfig.get(appContext, K_MINI_PIC, ""))) ? 1 : 0) + (pbSet(obj, "webURL", HookConfig.get(appContext, K_MINI_WEB, "")) ? 1 : 0)) + " 项 | 原: title=" + pbGet2 + " desc=" + pbGet3 + " pic=" + pbGet4 + " jump=" + pbGet5);
            miniDbg("    → 新: title=" + HookConfig.get(appContext, K_MINI_TITLE, "") + " desc=" + HookConfig.get(appContext, K_MINI_DESC, "") + " pic=" + HookConfig.get(appContext, K_MINI_PIC, "") + " web=" + HookConfig.get(appContext, K_MINI_WEB, ""));
        }
    }

    private static String pbGet(Object obj, String str) {
        Object callMethod;
        try {
            Object objectField = XposedHelpers.getObjectField(obj, str);
            if (objectField != null && (callMethod = XposedHelpers.callMethod(objectField, "get", new Object[0])) != null) {
                return String.valueOf(callMethod);
            }
            return null;
        } catch (Throwable th) {
            return null;
        }
    }

    public static boolean pbSet(Object obj, String str, String str2) {
        if (str2 == null || str2.length() == 0) {
            return false;
        }
        try {
            Object objectField = XposedHelpers.getObjectField(obj, str);
            if (objectField == null) {
                return false;
            }
            XposedHelpers.callMethod(objectField, "set", new Class[]{String.class}, new Object[]{str2});
            return true;
        } catch (Throwable th) {
            miniDbg("写 " + str + " 失败: " + th);
            return false;
        }
    }

    public static String stripScheme(String str) {
        if (str == null) {
            return "";
        }
        String trim = str.trim();
        return trim.startsWith("https://") ? trim.substring(8) : trim.startsWith("http://") ? trim.substring(7) : trim.startsWith("//") ? trim.substring(2) : trim;
    }

    public static String uploadUguu(byte[] bArr) {
        HttpURLConnection httpURLConnection;
        String str = null;
        ByteArrayOutputStream byteArrayOutputStream = null;
        try {
            str = "----catuguu" + System.currentTimeMillis();
            byteArrayOutputStream = new ByteArrayOutputStream();
            Charset charset = UTF8;
            byteArrayOutputStream.write(("--" + str + "\r\nContent-Disposition: form-data; name=\"files[]\"; filename=\"a.jpg\"\r\n").getBytes(charset));
            byteArrayOutputStream.write("Content-Type: image/jpeg\r\n\r\n".getBytes(charset));
            byteArrayOutputStream.write(bArr);
            byteArrayOutputStream.write(("\r\n--" + str + "--\r\n").getBytes(charset));
            httpURLConnection = (HttpURLConnection) new URL("https://uguu.se/upload.php").openConnection(Proxy.NO_PROXY);
        } catch (Throwable th) {

            httpURLConnection = null;
        }
        try {
            httpURLConnection.setRequestMethod("POST");
            httpURLConnection.setDoOutput(true);
            httpURLConnection.setConnectTimeout(12000);
            httpURLConnection.setReadTimeout(45000);
            httpURLConnection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + str);
            httpURLConnection.setRequestProperty("User-Agent", "Mozilla/5.0");
            OutputStream outputStream = httpURLConnection.getOutputStream();
            outputStream.write(byteArrayOutputStream.toByteArray());
            outputStream.flush();
            outputStream.close();
            InputStream errorStream = httpURLConnection.getResponseCode() >= 400 ? httpURLConnection.getErrorStream() : httpURLConnection.getInputStream();
            if (errorStream == null) {
                return null;
            }
            ByteArrayOutputStream byteArrayOutputStream2 = new ByteArrayOutputStream();
            byte[] bArr2 = new byte[4096];
            while (true) {
                int read = errorStream.read(bArr2);
                if (read <= 0) {
                    break;
                }
                byteArrayOutputStream2.write(bArr2, 0, read);
            }
            errorStream.close();
            String byteArrayOutputStream3 = byteArrayOutputStream2.toString("UTF-8");
            miniDbg("uguu 响应 " + byteArrayOutputStream3);
            JSONArray optJSONArray = new JSONObject(byteArrayOutputStream3).optJSONArray("files");
            if (optJSONArray != null && optJSONArray.length() != 0) {
                String optString = optJSONArray.getJSONObject(0).optString("url", "");
                String str2 = optString.length() > 0 ? optString : null;
                if (httpURLConnection != null) {
                    httpURLConnection.disconnect();
                }
                return str2;
            }
            return null;
        } catch (Throwable th2) {
            Throwable th = th2;
            try {
                miniDbg("uguu 异常 " + th);
                if (httpURLConnection != null) {
                    httpURLConnection.disconnect();
                }
                return null;
            } finally {
                if (httpURLConnection != null) {
                    httpURLConnection.disconnect();
                }
            }
        }
    }

    public static void hostImage(final String str, final HostCb hostCb) {
        if (hostCb == null) {
            return;
        }
        Bg.run(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection httpURLConnection;
                try {
                    File file = new File(str);
                    if (!file.exists()) {
                        hostCb.done(null, "文件不见了");
                        return;
                    }
                    if (file.length() > 12582912) {
                        hostCb.done(null, "图太大（>12MB）");
                        return;
                    }
                    int length = (int) file.length();
                    byte[] bArr = new byte[length];
                    FileInputStream fileInputStream = new FileInputStream(file);
                    int i = 0;
                    while (i < length) {
                        int read = fileInputStream.read(bArr, i, length - i);
                        if (read <= 0) {
                            break;
                        } else {
                            i += read;
                        }
                    }
                    fileInputStream.close();
                    String uploadUguu = ArkSigner.uploadUguu(bArr);
                    if (uploadUguu != null) {
                        hostCb.done(uploadUguu, null);
                        return;
                    }
                    String str2 = "----catimg" + System.currentTimeMillis();
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    byteArrayOutputStream.write(("--" + str2 + "\r\nContent-Disposition: form-data; name=\"key\"\r\n\r\n" + ArkSigner.HOST_KEY + "\r\n").getBytes(ArkSigner.UTF8));
                    byteArrayOutputStream.write(("--" + str2 + "\r\nContent-Disposition: form-data; name=\"format\"\r\n\r\njson\r\n").getBytes(ArkSigner.UTF8));
                    byteArrayOutputStream.write(("--" + str2 + "\r\nContent-Disposition: form-data; name=\"source\"; filename=\"a.jpg\"\r\n").getBytes(ArkSigner.UTF8));
                    byteArrayOutputStream.write("Content-Type: image/jpeg\r\n\r\n".getBytes(ArkSigner.UTF8));
                    byteArrayOutputStream.write(bArr);
                    byteArrayOutputStream.write(("\r\n--" + str2 + "--\r\n").getBytes(ArkSigner.UTF8));
                    HttpURLConnection httpURLConnection2 = (HttpURLConnection) new URL(ArkSigner.HOST_API).openConnection(Proxy.NO_PROXY);
                    try {
                        httpURLConnection2.setRequestMethod("POST");
                        httpURLConnection2.setDoOutput(true);
                        httpURLConnection2.setConnectTimeout(NetApi.DEFAULT_TIMEOUT);
                        httpURLConnection2.setReadTimeout(60000);
                        httpURLConnection2.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + str2);
                        OutputStream outputStream = httpURLConnection2.getOutputStream();
                        outputStream.write(byteArrayOutputStream.toByteArray());
                        outputStream.flush();
                        outputStream.close();
                        InputStream errorStream = httpURLConnection2.getResponseCode() >= 400 ? httpURLConnection2.getErrorStream() : httpURLConnection2.getInputStream();
                        if (errorStream == null) {
                            hostCb.done(null, "图床没响应");
                            if (httpURLConnection2 != null) {
                                httpURLConnection2.disconnect();
                                return;
                            }
                            return;
                        }
                        ByteArrayOutputStream byteArrayOutputStream2 = new ByteArrayOutputStream();
                        byte[] bArr2 = new byte[4096];
                        while (true) {
                            int read2 = errorStream.read(bArr2);
                            if (read2 <= 0) {
                                break;
                            } else {
                                byteArrayOutputStream2.write(bArr2, 0, read2);
                            }
                        }
                        errorStream.close();
                        String byteArrayOutputStream3 = byteArrayOutputStream2.toString("UTF-8");
                        ArkSigner.miniDbg("图床响应 " + byteArrayOutputStream3);
                        JSONObject optJSONObject = new JSONObject(byteArrayOutputStream3).optJSONObject("image");
                        String optString = optJSONObject != null ? optJSONObject.optString("url", "") : "";
                        if (optString.length() > 0) {
                            hostCb.done(optString, null);
                        } else {
                            hostCb.done(null, "图床没返回链接");
                        }
                        if (httpURLConnection2 != null) {
                            httpURLConnection2.disconnect();
                        }
                    } catch (Throwable th) {
                        httpURLConnection = httpURLConnection2;

                        try {
                            hostCb.done(null, String.valueOf(th));
                        } finally {
                            if (httpURLConnection != null) {
                                httpURLConnection.disconnect();
                            }
                        }
                    }
                } catch (Throwable th2) {
                    Throwable th = th2;
                    httpURLConnection = null;
                }
            }
        }, "mini-host");
    }

    private static boolean reachable(String str) {
        boolean z = false;
        HttpURLConnection httpURLConnection = null;
        try {
            HttpURLConnection httpURLConnection2 = (HttpURLConnection) new URL(str).openConnection(Proxy.NO_PROXY);
            try {
                httpURLConnection2.setConnectTimeout(6000);
                httpURLConnection2.setReadTimeout(6000);
                httpURLConnection2.setInstanceFollowRedirects(true);
                if (httpURLConnection2.getResponseCode() == 200) {
                    InputStream inputStream = httpURLConnection2.getInputStream();
                    int read = inputStream.read(new byte[256]);
                    inputStream.close();
                    if (read > 32) {
                        z = true;
                    }
                    if (httpURLConnection2 != null) {
                        httpURLConnection2.disconnect();
                    }
                    return z;
                }
                if (httpURLConnection2 != null) {
                    httpURLConnection2.disconnect();
                }
                return false;
            } catch (Throwable th) {
                httpURLConnection = httpURLConnection2;
                if (httpURLConnection != null) {
                    httpURLConnection.disconnect();
                }
                return false;
            }
        } catch (Throwable th2) {
        }
        return false;
    }

    public static void sendMiniCard(String str, String str2, String str3, String str4, SendCb sendCb) {
        sendMiniCard(str, str2, str3, str4, null, null, sendCb);
    }

    public static void sendMiniCard(final String str, final String str2, final String str3, final String str4, final String str5, final String str6, final SendCb sendCb) {
        Bg.run(new Runnable() {
            @Override
            public void run() {
                Method method;
                String str7;
                String str8;
                String str9;
                try {
                    ClassLoader classLoader = QQEnv.classLoader();
                    Class findClassIfExists = XposedHelpers.findClassIfExists("com.tencent.mobileqq.mini.reuse.MiniAppCmdUtil", classLoader);
                    if (findClassIfExists == null) {
                        sendCb.done(false, "找不到 MiniAppCmdUtil");
                        return;
                    }
                    Object callStaticMethod = XposedHelpers.callStaticMethod(findClassIfExists, "getInstance", new Object[0]);
                    if (callStaticMethod == null) {
                        try {
                            callStaticMethod = findClassIfExists.newInstance();
                        } catch (Throwable th) {
                        }
                    }
                    if (callStaticMethod == null) {
                        sendCb.done(false, "MiniAppCmdUtil 实例化失败");
                        return;
                    }
                    Method[] declaredMethods = findClassIfExists.getDeclaredMethods();
                    int length = declaredMethods.length;
                    int i = 0;
                    while (true) {
                        if (i >= length) {
                            method = null;
                            break;
                        }
                        method = declaredMethods[i];
                        if ("getShareInfo".equals(method.getName()) && method.getParameterTypes().length == 2) {
                            break;
                        }
                        i++;
                    }
                    if (method == null) {
                        sendCb.done(false, "找不到 getShareInfo");
                        return;
                    }
                    Class findClass = XposedHelpers.findClass("NS_MINI_SHARE.MiniProgramShare$StAdaptShareInfoReq", classLoader);
                    Object newInstance = findClass.newInstance();
                    ArkSigner.pbSet(newInstance, "appid", ArkSigner.BILI_APPID);
                    ArkSigner.pbSet(newInstance, "title", "哔哩哔哩");
                    ArkSigner.pbSet(newInstance, "desc", str);
                    ArkSigner.pbSet(newInstance, "picUrl", ArkSigner.stripScheme(str2));
                    String str10 = str4;
                    if (str10 != null && str10.length() != 0) {
                        str7 = str4;
                        ArkSigner.pbSet(newInstance, "jumpUrl", str7);
                        ArkSigner.pbSet(newInstance, "webURL", str3);
                        ArkSigner.pbSet(newInstance, "iconUrl", "https://open.gtimg.cn/open/app_icon/00/95/17/76/100951776_100_m.png");
                        ArkSigner.pbSetInt(newInstance, "scene", 1036);
                        ArkSigner.pbSetInt(newInstance, "shareType", 0);
                        ArkSigner.pbSetInt(newInstance, "templetType", 1);
                        ArkSigner.pbSetInt(newInstance, "businessType", 0);
                        ArkSigner.pbSetInt(newInstance, "verType", 3);
                        str8 = str5;
                        if (str8 != null && str8.length() > 0) {
                            ArkSigner.pbSet(newInstance, "vidUrl", str5);
                            ArkSigner.miniDbg("写 vidUrl = " + str5);
                        }
                        str9 = str6;
                        if (str9 != null && str9.length() > 0) {
                            try {
                                Object newInstance2 = XposedHelpers.findClass("NS_MINI_SHARE.MiniProgramShare$StTemplateInfo", classLoader).newInstance();
                                ArkSigner.pbSet(newInstance2, "templateId", str6);
                                Field field = findClass.getField("template");
                                field.setAccessible(true);
                                field.set(newInstance, newInstance2);
                                ArkSigner.miniDbg("写 template.templateId = " + str6);
                            } catch (Throwable th2) {
                                ArkSigner.miniDbg("写 template 失败: " + th2);
                            }
                        }
                        Class<?> cls = method.getParameterTypes()[1];
                        Object newProxyInstance = java.lang.reflect.Proxy.newProxyInstance(cls.getClassLoader(), new Class[]{cls}, new InvocationHandler() {
                            @Override
                            public Object invoke(Object obj, Method method2, Object[] objArr) {
                                JSONObject jSONObject;
                                try {
                                    if ("onCmdListener".equals(method2.getName()) && objArr != null && objArr.length >= 2) {
                                        boolean equals = Boolean.TRUE.equals(objArr[0]);
                                        Object obj2 = objArr[1];
                                        if (!(obj2 instanceof JSONObject)) {
                                            jSONObject = null;
                                        } else {
                                            jSONObject = (JSONObject) obj2;
                                        }
                                        ArkSigner.miniDbg("直发回调 ok=" + equals + " js=" + jSONObject);
                                        if (jSONObject != null) {
                                            ArkSigner.sendArkNow(ArkSigner.finishArk(jSONObject, str3), sendCb);
                                        } else {
                                            sendCb.done(false, equals ? "回调没带卡片数据" : "服务端拒绝签名");
                                        }
                                    }
                                } catch (Throwable th3) {
                                    sendCb.done(false, String.valueOf(th3));
                                }
                                return null;
                            }
                        });
                        method.setAccessible(true);
                        method.invoke(callStaticMethod, newInstance, newProxyInstance);
                        ArkSigner.miniDbg("直发：已提交签名请求");
                    }
                    str7 = "pages/index/index";
                    ArkSigner.pbSet(newInstance, "jumpUrl", str7);
                    ArkSigner.pbSet(newInstance, "webURL", str3);
                    ArkSigner.pbSet(newInstance, "iconUrl", "https://open.gtimg.cn/open/app_icon/00/95/17/76/100951776_100_m.png");
                    ArkSigner.pbSetInt(newInstance, "scene", 1036);
                    ArkSigner.pbSetInt(newInstance, "shareType", 0);
                    ArkSigner.pbSetInt(newInstance, "templetType", 1);
                    ArkSigner.pbSetInt(newInstance, "businessType", 0);
                    ArkSigner.pbSetInt(newInstance, "verType", 3);
                    str8 = str5;
                    if (str8 != null) {
                        ArkSigner.pbSet(newInstance, "vidUrl", str5);
                        ArkSigner.miniDbg("写 vidUrl = " + str5);
                    }
                    str9 = str6;
                    if (str9 != null) {
                        Object newInstance22 = XposedHelpers.findClass("NS_MINI_SHARE.MiniProgramShare$StTemplateInfo", classLoader).newInstance();
                        ArkSigner.pbSet(newInstance22, "templateId", str6);
                        Field field2 = findClass.getField("template");
                        field2.setAccessible(true);
                        field2.set(newInstance, newInstance22);
                        ArkSigner.miniDbg("写 template.templateId = " + str6);
                    }
                    Class<?> cls2 = method.getParameterTypes()[1];
                    Object newProxyInstance2 = java.lang.reflect.Proxy.newProxyInstance(cls2.getClassLoader(), new Class[]{cls2}, new InvocationHandler() {
                        @Override
                        public Object invoke(Object obj, Method method2, Object[] objArr) {
                            JSONObject jSONObject;
                            try {
                                if ("onCmdListener".equals(method2.getName()) && objArr != null && objArr.length >= 2) {
                                    boolean equals = Boolean.TRUE.equals(objArr[0]);
                                    Object obj2 = objArr[1];
                                    if (!(obj2 instanceof JSONObject)) {
                                        jSONObject = null;
                                    } else {
                                        jSONObject = (JSONObject) obj2;
                                    }
                                    ArkSigner.miniDbg("直发回调 ok=" + equals + " js=" + jSONObject);
                                    if (jSONObject != null) {
                                        ArkSigner.sendArkNow(ArkSigner.finishArk(jSONObject, str3), sendCb);
                                    } else {
                                        sendCb.done(false, equals ? "回调没带卡片数据" : "服务端拒绝签名");
                                    }
                                }
                            } catch (Throwable th3) {
                                sendCb.done(false, String.valueOf(th3));
                            }
                            return null;
                        }
                    });
                    method.setAccessible(true);
                    method.invoke(callStaticMethod, newInstance, newProxyInstance2);
                    ArkSigner.miniDbg("直发：已提交签名请求");
                } catch (Throwable th3) {
                    ArkSigner.miniDbg("直发异常 " + th3);
                    sendCb.done(false, String.valueOf(th3));
                }
            }
        }, "mini-send");
    }

    public static void sendWxMiniCard(final String str, final String str2, final String str3, final String str4, final String str5, final String str6, final int i, final SendCb sendCb) {
        Bg.run(new Runnable() {
            @Override
            public void run() {

                try {
                    String tag = "wxmini-send-" + System.currentTimeMillis();
                    if (sWxSending) {
                        sendCb.done(false, "上一张还在签，别连点喵");
                        return;
                    }
                    sWxSending = true;
                    try {
                        doSendWxMiniCard(str, str2, str3, str4, str5, str6, i, sendCb);
                    } finally {
                        sWxSending = false;
                    }
                } catch (Throwable th) {
                    ArkSigner.miniDbg("wx直签异常 " + th);
                    sWxSending = false;
                    sendCb.done(false, String.valueOf(th));
                }
            }
        }, "wxmini-send");
    }

    private static volatile boolean sWxSending = false;

    private static void doSendWxMiniCard(String str, String str2, String str3, String str4, String str5, String str6, int i, final SendCb sendCb) throws Throwable {
        ClassLoader classLoader = QQEnv.classLoader();
        Class findClassIfExists = XposedHelpers.findClassIfExists("com.tencent.mobileqq.mini.reuse.MiniAppCmdUtil", classLoader);
        if (findClassIfExists == null) {
            sendCb.done(false, "找不到 MiniAppCmdUtil");
            return;
        }
        Object callStaticMethod = XposedHelpers.callStaticMethod(findClassIfExists, "getInstance", new Object[0]);
        if (callStaticMethod == null) {
            try {
                callStaticMethod = findClassIfExists.newInstance();
            } catch (Throwable th) {
            }
        }
        if (callStaticMethod == null) {
            sendCb.done(false, "MiniAppCmdUtil 实例化失败");
            return;
        }
        Method method = null;
        Method[] declaredMethods = findClassIfExists.getDeclaredMethods();
        for (Method m : declaredMethods) {
            if ("getShareInfo".equals(m.getName()) && m.getParameterTypes().length == 2) {
                method = m;
                break;
            }
        }
        if (method == null) {
            sendCb.done(false, "找不到 getShareInfo");
            return;
        }

        Object newInstance = XposedHelpers.findClass("NS_MINI_SHARE.MiniProgramShare$StAdaptShareInfoReq", classLoader).newInstance();
        String appid = str6;
        String appidRich = "";
        int tpl = 1;
        if (i == 1) {
            appid = ArkSigner.qAppId();
            appidRich = str6;
        } else if (i == 2) {
            appidRich = str6;
            tpl = 6;
        }
        ArkSigner.pbSet(newInstance, "appid", appid);
        String title = (str == null || str.length() == 0) ? "微信小程序" : str;
        ArkSigner.pbSet(newInstance, "title", title);
        ArkSigner.pbSet(newInstance, "desc", str2 == null ? "" : str2);
        ArkSigner.pbSet(newInstance, "picUrl", ArkSigner.stripScheme(str3));
        String jumpUrl = (str5 == null || str5.length() == 0) ? "pages/index/index" : str5;
        ArkSigner.pbSet(newInstance, "jumpUrl", jumpUrl);
        ArkSigner.pbSet(newInstance, "webURL", str4);
        ArkSigner.pbSet(newInstance, "iconUrl", "https://miniapp.gtimg.cn/public/miniwx.png");
        if (appidRich.length() > 0) {
            ArkSigner.pbSet(newInstance, "appidRich", appidRich);
        }
        ArkSigner.pbSetInt(newInstance, "scene", 1036);
        ArkSigner.pbSetInt(newInstance, "shareType", 0);
        ArkSigner.pbSetInt(newInstance, "businessType", 2);
        ArkSigner.pbSetInt(newInstance, "templetType", tpl);
        ArkSigner.pbSetInt(newInstance, "verType", 3);
        ArkSigner.miniDbg("wx直签 profile=" + i + " appid=" + appid + " rich=" + appidRich + " biz=2 tpl=" + tpl);

        Class<?> cls = method.getParameterTypes()[1];
        Object proxy = java.lang.reflect.Proxy.newProxyInstance(cls.getClassLoader(), new Class[]{cls}, new InvocationHandler() {
            @Override
            public Object invoke(Object obj, Method method2, Object[] objArr) {
                try {
                    if ("onCmdListener".equals(method2.getName()) && objArr != null && objArr.length >= 2) {
                        boolean ok = Boolean.TRUE.equals(objArr[0]);
                        Object data = objArr[1];
                        JSONObject js = (data instanceof JSONObject) ? (JSONObject) data : null;
                        ArkSigner.miniDbg("wx直签回调 ok=" + ok + " js=" + js);
                        if (js != null) {
                            ArkSigner.sendArkNow(ArkSigner.finishArk(js, str4), sendCb);
                        } else {
                            sendCb.done(false, ok ? "回调没带卡片数据" : "服务端拒绝签名");
                        }
                    }
                } catch (Throwable th2) {
                    sendCb.done(false, String.valueOf(th2));
                }
                return null;
            }
        });
        method.setAccessible(true);
        method.invoke(callStaticMethod, newInstance, proxy);
    }

    public static void sendGameCard(final String str, final String str2, final String str3, final String str4, final String str5, final String str6, final int i, final int i2, final SendCb sendCb) {
        Bg.run(new Runnable() {
            @Override
            public void run() {
                Method method;
                String str7;
                try {
                    ClassLoader classLoader = QQEnv.classLoader();
                    Class findClassIfExists = XposedHelpers.findClassIfExists("com.tencent.mobileqq.mini.reuse.MiniAppCmdUtil", classLoader);
                    if (findClassIfExists == null) {
                        sendCb.done(false, "找不到 MiniAppCmdUtil");
                        return;
                    }
                    Object callStaticMethod = XposedHelpers.callStaticMethod(findClassIfExists, "getInstance", new Object[0]);
                    if (callStaticMethod == null) {
                        try {
                            callStaticMethod = findClassIfExists.newInstance();
                        } catch (Throwable th) {
                        }
                    }
                    if (callStaticMethod == null) {
                        sendCb.done(false, "MiniAppCmdUtil 实例化失败");
                        return;
                    }
                    Method[] declaredMethods = findClassIfExists.getDeclaredMethods();
                    int length = declaredMethods.length;
                    int i3 = 0;
                    while (true) {
                        if (i3 >= length) {
                            method = null;
                            break;
                        }
                        method = declaredMethods[i3];
                        if ("getShareInfo".equals(method.getName()) && method.getParameterTypes().length == 2) {
                            break;
                        } else {
                            i3++;
                        }
                    }
                    if (method == null) {
                        sendCb.done(false, "找不到 getShareInfo");
                        return;
                    }
                    Object newInstance = XposedHelpers.findClass("NS_MINI_SHARE.MiniProgramShare$StAdaptShareInfoReq", classLoader).newInstance();
                    ArkSigner.pbSet(newInstance, "appid", str5);
                    String str8 = str6;
                    if (str8 != null && str8.length() > 0) {
                        ArkSigner.pbSet(newInstance, "appidRich", str6);
                    }
                    String str9 = str;
                    if (str9 != null && str9.length() != 0) {
                        str7 = str;
                        ArkSigner.pbSet(newInstance, "title", str7);
                        ArkSigner.pbSet(newInstance, "desc", str2);
                        ArkSigner.pbSet(newInstance, "picUrl", ArkSigner.stripScheme(str3));
                        ArkSigner.pbSet(newInstance, "jumpUrl", str4);
                        ArkSigner.pbSet(newInstance, "webURL", "");
                        ArkSigner.pbSet(newInstance, "iconUrl", "");
                        ArkSigner.pbSetInt(newInstance, "scene", i2);
                        ArkSigner.pbSetInt(newInstance, "shareType", 0);
                        ArkSigner.pbSetInt(newInstance, "businessType", 2);
                        ArkSigner.pbSetInt(newInstance, "templetType", i);
                        ArkSigner.pbSetInt(newInstance, "verType", 3);
                        ArkSigner.miniDbg("游戏卡直签 appid=" + str5 + " rich=" + str6 + " tpl=" + i + " scene=" + i2 + " title=" + str + " jump=" + str4);
                        Class<?> cls = method.getParameterTypes()[1];
                        Object newProxyInstance = java.lang.reflect.Proxy.newProxyInstance(cls.getClassLoader(), new Class[]{cls}, new InvocationHandler() {
                            @Override
                            public Object invoke(Object obj, Method method2, Object[] objArr) {
                                JSONObject jSONObject;
                                try {
                                    if ("onCmdListener".equals(method2.getName()) && objArr != null && objArr.length >= 2) {
                                        boolean equals = Boolean.TRUE.equals(objArr[0]);
                                        Object obj2 = objArr[1];
                                        if (!(obj2 instanceof JSONObject)) {
                                            jSONObject = null;
                                        } else {
                                            jSONObject = (JSONObject) obj2;
                                        }
                                        ArkSigner.miniDbg("游戏卡回调 ok=" + equals + " js=" + jSONObject);
                                        if (!equals) {
                                            sendCb.done(false, "服务端拒绝：" + (jSONObject == null ? "无返回" : jSONObject.toString()));
                                            return null;
                                        }
                                        if (jSONObject != null) {
                                            ArkSigner.sendArkNow(ArkSigner.finishArk(jSONObject, ""), sendCb);
                                        } else {
                                            sendCb.done(false, "回调没带卡片数据");
                                        }
                                    }
                                } catch (Throwable th2) {
                                    sendCb.done(false, String.valueOf(th2));
                                }
                                return null;
                            }
                        });
                        method.setAccessible(true);
                        method.invoke(callStaticMethod, newInstance, newProxyInstance);
                    }
                    str7 = "游戏卡";
                    ArkSigner.pbSet(newInstance, "title", str7);
                    ArkSigner.pbSet(newInstance, "desc", str2);
                    ArkSigner.pbSet(newInstance, "picUrl", ArkSigner.stripScheme(str3));
                    ArkSigner.pbSet(newInstance, "jumpUrl", str4);
                    ArkSigner.pbSet(newInstance, "webURL", "");
                    ArkSigner.pbSet(newInstance, "iconUrl", "");
                    ArkSigner.pbSetInt(newInstance, "scene", i2);
                    ArkSigner.pbSetInt(newInstance, "shareType", 0);
                    ArkSigner.pbSetInt(newInstance, "businessType", 2);
                    ArkSigner.pbSetInt(newInstance, "templetType", i);
                    ArkSigner.pbSetInt(newInstance, "verType", 3);
                    ArkSigner.miniDbg("游戏卡直签 appid=" + str5 + " rich=" + str6 + " tpl=" + i + " scene=" + i2 + " title=" + str + " jump=" + str4);
                    Class<?> cls2 = method.getParameterTypes()[1];
                    Object newProxyInstance2 = java.lang.reflect.Proxy.newProxyInstance(cls2.getClassLoader(), new Class[]{cls2}, new InvocationHandler() {
                        @Override
                        public Object invoke(Object obj, Method method2, Object[] objArr) {
                            JSONObject jSONObject;
                            try {
                                if ("onCmdListener".equals(method2.getName()) && objArr != null && objArr.length >= 2) {
                                    boolean equals = Boolean.TRUE.equals(objArr[0]);
                                    Object obj2 = objArr[1];
                                    if (!(obj2 instanceof JSONObject)) {
                                        jSONObject = null;
                                    } else {
                                        jSONObject = (JSONObject) obj2;
                                    }
                                    ArkSigner.miniDbg("游戏卡回调 ok=" + equals + " js=" + jSONObject);
                                    if (!equals) {
                                        sendCb.done(false, "服务端拒绝：" + (jSONObject == null ? "无返回" : jSONObject.toString()));
                                        return null;
                                    }
                                    if (jSONObject != null) {
                                        ArkSigner.sendArkNow(ArkSigner.finishArk(jSONObject, ""), sendCb);
                                    } else {
                                        sendCb.done(false, "回调没带卡片数据");
                                    }
                                }
                            } catch (Throwable th2) {
                                sendCb.done(false, String.valueOf(th2));
                            }
                            return null;
                        }
                    });
                    method.setAccessible(true);
                    method.invoke(callStaticMethod, newInstance, newProxyInstance2);
                } catch (Throwable th2) {
                    ArkSigner.miniDbg("游戏卡直签异常 " + th2);
                    sendCb.done(false, String.valueOf(th2));
                }
            }
        }, "gamecard-send");
    }

    public static void probeMiniAppIds(final String str, final SendCb sendCb) {
        Bg.run(new Runnable() {
            @Override
            public void run() {
                Method method;
                try {
                    ClassLoader classLoader = QQEnv.classLoader();
                    Class findClassIfExists = XposedHelpers.findClassIfExists("com.tencent.mobileqq.mini.reuse.MiniAppCmdUtil", classLoader);
                    if (findClassIfExists == null) {
                        sendCb.done(false, "找不到 MiniAppCmdUtil");
                        return;
                    }
                    Object callStaticMethod = XposedHelpers.callStaticMethod(findClassIfExists, "getInstance", new Object[0]);
                    if (callStaticMethod == null) {
                        try {
                            callStaticMethod = findClassIfExists.newInstance();
                        } catch (Throwable th) {
                        }
                    }
                    Method[] declaredMethods = findClassIfExists.getDeclaredMethods();
                    int length = declaredMethods.length;
                    int i = 0;
                    while (true) {
                        if (i >= length) {
                            method = null;
                            break;
                        }
                        method = declaredMethods[i];
                        if ("getShareInfo".equals(method.getName()) && method.getParameterTypes().length == 2) {
                            break;
                        } else {
                            i++;
                        }
                    }
                    if (callStaticMethod != null && method != null) {
                        ArkSigner.probeStep(classLoader, callStaticMethod, method, XposedHelpers.findClass("NS_MINI_SHARE.MiniProgramShare$StAdaptShareInfoReq", classLoader), str.split("[,，\\s]+"), 0, new StringBuilder(), new StringBuilder(), sendCb);
                        return;
                    }
                    sendCb.done(false, "接口不可用");
                } catch (Throwable th2) {
                    sendCb.done(false, String.valueOf(th2));
                }
            }
        }, "mini-probe");
    }

    public static void probeStep(final ClassLoader classLoader, final Object obj, final Method method, final Class<?> cls, final String[] strArr, final int i, final StringBuilder sb, final StringBuilder sb2, final SendCb sendCb) {
        final String str;
        if (i >= strArr.length) {
            String sb3 = sb.toString();
            HookLog.d("miniapp probe:\n" + sb3);
            try {
                Context appContext = QQEnv.appContext();
                if (appContext != null) {
                    FileOutputStream fileOutputStream = new FileOutputStream(new File(appContext.getFilesDir(), "miniapp_probe.txt"), false);
                    fileOutputStream.write(sb3.getBytes("UTF-8"));
                    fileOutputStream.close();
                }
            } catch (Throwable th) {
            }
            try {
                Context appContext2 = QQEnv.appContext();
                if (appContext2 != null && sb2.length() > 0) {
                    String str2 = HookConfig.get(appContext2, "wxmini_extra", "");
                    HashSet hashSet = new HashSet();
                    for (String str3 : str2.split(";")) {
                        String[] split = str3.split("\\|");
                        if (split.length == 2) {
                            hashSet.add(split[0].trim());
                        }
                    }
                    StringBuilder sb4 = new StringBuilder();
                    for (String str4 : sb2.toString().split(";")) {
                        String[] split2 = str4.split("\\|");
                        if (split2.length == 2 && !hashSet.contains(split2[0].trim())) {
                            hashSet.add(split2[0].trim());
                            sb4.append(str4).append(';');
                        }
                    }
                    if (sb4.length() > 0) {
                        HookConfig.set(appContext2, "wxmini_extra", str2.length() == 0 ? sb4.toString() : str2 + ";" + ((Object) sb4));
                        HookLog.d("miniapp probe 新增 " + ((Object) sb4));
                    }
                }
            } catch (Throwable th2) {
            }
            sendCb.done(true, sb3);
            return;
        }
        final String trim = strArr[i].trim();
        if (trim.length() == 0) {
            probeStep(classLoader, obj, method, cls, strArr, i + 1, sb, sb2, sendCb);
            return;
        }
        final AtomicBoolean atomicBoolean = new AtomicBoolean(false);
        try {
            Object newInstance = cls.newInstance();
            pbSet(newInstance, "appid", trim);
            pbSet(newInstance, "title", "探测");
            pbSet(newInstance, "desc", "probe");
            pbSet(newInstance, "jumpUrl", "pages/index/index");
            pbSet(newInstance, "webURL", "https://qq.com");
            pbSetInt(newInstance, "scene", 1036);
            pbSetInt(newInstance, "businessType", 2);
            pbSetInt(newInstance, "templetType", 3);
            pbSetInt(newInstance, "verType", 3);
            miniDbg("探测 appid=" + trim + " tpl=3 scene=1036");
            Class<?> cls2 = method.getParameterTypes()[1];
            str = trim;
            try {
                Object newProxyInstance = java.lang.reflect.Proxy.newProxyInstance(cls2.getClassLoader(), new Class[]{cls2}, new InvocationHandler() {

                    @Override

                    public Object invoke(Object obj2, Method method2, Object[] objArr) {
                        String str5;
                        String str6 = null;
                        if ("onCmdListener".equals(method2.getName()) && objArr != null && objArr.length >= 2) {
                            boolean equals = Boolean.TRUE.equals(objArr[0]);
                            Object obj3 = objArr[1];
                            JSONObject jSONObject = obj3 instanceof JSONObject ? (JSONObject) obj3 : null;
                            if (jSONObject != null) {
                                try {
                                    str5 = jSONObject.optString("appName", "");
                                    try {
                                        JSONObject optJSONObject = jSONObject.optJSONObject("metaData");
                                        if (optJSONObject == null) {
                                            optJSONObject = jSONObject.optJSONObject("meta");
                                        }
                                        if (optJSONObject != null) {
                                            JSONObject optJSONObject2 = optJSONObject.optJSONObject("detail_1");
                                            if (optJSONObject2 == null) {
                                                optJSONObject2 = optJSONObject.optJSONObject("miniapp");
                                            }
                                            if (optJSONObject2 != null) {
                                                String optString = optJSONObject2.optString("title", "");
                                                if (optString.length() > 0) {
                                                    str5 = optString;
                                                }
                                            }
                                        }
                                        str6 = jSONObject.optString("retCode", jSONObject.optString("errCode", ""));
                                    } catch (Throwable th3) {
                                        str6 = "";
                                        if (equals) {
                                            sb2.append(trim).append('|').append(str5.length() != 0 ? "未命名" : str5).append(';');
                                        }
                                        sb.append(trim).append((equals || jSONObject == null) ? "  FAIL " : "  OK   ").append(str5).append(str6.length() > 0 ? "  [" + str6 + "]" : "").append('\n');
                                        if (atomicBoolean.compareAndSet(false, true)) {
                                        }
                                        return null;
                                    }
                                } catch (Throwable th4) {
                                    str5 = "";
                                }
                            } else {
                                str5 = "";
                                str6 = str5;
                            }
                            if (equals && jSONObject != null) {
                                sb2.append(trim).append('|').append(str5.length() != 0 ? "未命名" : str5).append(';');
                            }
                            sb.append(trim).append((equals || jSONObject == null) ? "  FAIL " : "  OK   ").append(str5).append(str6.length() > 0 ? "  [" + str6 + "]" : "").append('\n');
                            if (atomicBoolean.compareAndSet(false, true)) {
                                ArkSigner.probeStep(classLoader, obj, method, cls, strArr, i + 1, sb, sb2, sendCb);
                            }
                        }
                        return null;
                    }
                });
                method.setAccessible(true);
                try {
                    method.invoke(obj, newInstance, newProxyInstance);
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Thread.sleep(8000L);
                            } catch (Throwable th3) {
                            }
                            if (atomicBoolean.compareAndSet(false, true)) {
                                sb.append(str).append("  TIMEOUT\n");
                                ArkSigner.probeStep(classLoader, obj, method, cls, strArr, i + 1, sb, sb2, sendCb);
                            }
                        }
                    }, "probe-wd").start();
                } catch (Throwable th3) {
                    Throwable th = th3;
                    sb.append(str).append("  EX ").append(th).append('\n');
                    probeStep(classLoader, obj, method, cls, strArr, i + 1, sb, sb2, sendCb);
                }
            } catch (Throwable th4) {
                Throwable th = th4;
            }
        } catch (Throwable th5) {
        }
    }

    public static String finishArk(JSONObject jSONObject, String str) {
        try {
            JSONObject optJSONObject = jSONObject.optJSONObject("meta");
            if (optJSONObject == null) {
                optJSONObject = jSONObject.optJSONObject("metaData");
            }
            JSONObject jSONObject2 = new JSONObject();
            String optString = jSONObject.optString("appName", "");
            if (optString.length() == 0) {
                optString = jSONObject.optString("app", BILI_APP);
            }
            String optString2 = jSONObject.optString("appView", "");
            if (optString2.length() == 0) {
                optString2 = jSONObject.optString("view", "");
            }
            jSONObject2.put("app", optString);
            jSONObject2.put("view", optString2);
            jSONObject2.put("ver", jSONObject.optString("ver", "1.0.0.19"));
            jSONObject2.put("prompt", jSONObject.optString("prompt", ""));
            String optString3 = jSONObject.optString("desc", "");
            if (optString3.length() > 0) {
                jSONObject2.put("desc", optString3);
            }
            if (optJSONObject != null) {
                jSONObject2.put("meta", optJSONObject);
            }
            JSONObject optJSONObject2 = jSONObject.optJSONObject("config");
            if (optJSONObject2 != null) {
                jSONObject2.put("config", optJSONObject2);
            }
            if (jSONObject.has("needShareCallBack")) {
                jSONObject2.put("needShareCallBack", jSONObject.optBoolean("needShareCallBack"));
            }
            miniDbg("直发 ark -> " + jSONObject2);
            return jSONObject2.toString();
        } catch (Throwable th) {
            miniDbg("finishArk 异常 " + th);
            return jSONObject.toString();
        }
    }

    public static void sendArkNow(String str, SendCb sendCb) {
        try {
            Object ark = ElementFactory.ark(str);
            if (ark == null) {
                sendCb.done(false, "卡片元素构造失败");
                return;
            }
            Object make = ContactFactory.make(CurrentChat.chatType(), CurrentChat.peerUid(), "");
            if (make == null) {
                sendCb.done(false, "目标会话构造失败");
            } else {
                boolean send = MsgSender.send(make, Collections.singletonList(ark));
                sendCb.done(send, send ? "已发出" : "发送失败");
            }
        } catch (Throwable th) {
            sendCb.done(false, String.valueOf(th));
        }
    }

    public static void pbSetInt(Object obj, String str, int i) {
        try {
            Field field = obj.getClass().getField(str);
            field.setAccessible(true);
            Object obj2 = field.get(obj);
            if (obj2 == null) {
                miniDbg("写 " + str + ": 字段值为 null");
                return;
            }
            try {
                Method method = obj2.getClass().getMethod("set", Integer.TYPE);
                method.setAccessible(true);
                method.invoke(obj2, Integer.valueOf(i));
            } catch (NoSuchMethodException e) {
                Method method2 = obj2.getClass().getMethod("setValue", Integer.TYPE);
                method2.setAccessible(true);
                method2.invoke(obj2, Integer.valueOf(i));
            }
            miniDbg("写 " + str + " = " + i + " ok");
        } catch (Throwable th) {
            miniDbg("写 " + str + " 失败: " + th);
        }
    }

    public static synchronized void installShareProbe(ClassLoader classLoader) {
        Class<?> findClass = null;
        synchronized (ArkSigner.class) {
            if (shareHooked || classLoader == null) {
                return;
            }
            try {
                findClass = HookApi.findClass(classLoader, "com.tencent.mobileqq.activity.JumpActivity");
            } catch (Throwable th) {
                HookLog.e("ShareHook.install 失败", th);
            }
            if (findClass == null) {
                HookLog.w("ShareHook: 找不到 JumpActivity");
                return;
            }
            Method method = null;
            for (Class<?> cls = findClass; cls != null && cls != Object.class && method == null; cls = cls.getSuperclass()) {
                Method[] declaredMethods = cls.getDeclaredMethods();
                int length = declaredMethods.length;
                for (int i = 0; i < length; i++) {
                    Method method2 = declaredMethods[i];
                    if ("onCreate".equals(method2.getName())) {
                        Class<?>[] parameterTypes = method2.getParameterTypes();
                        if (parameterTypes.length == 1 && parameterTypes[0] == Bundle.class) {
                            method = method2;
                            break;
                        }
                    }
                }
            }
            try {
                method.setAccessible(true);
            } catch (Throwable th2) {
            }
            HookApi.Action action = SHARE_ACTION;
            HookApi.before(method, action);
            HookApi.after(method, action);
            try {
                for (Method method3 : findClass.getDeclaredMethods()) {
                    if ("onNewIntent".equals(method3.getName())) {
                        method3.setAccessible(true);
                        HookApi.Action action2 = SHARE_ACTION;
                        HookApi.before(method3, action2);
                        HookApi.after(method3, action2);
                    }
                }
            } catch (Throwable th3) {
            }
            try {
                Class<?> findClass2 = HookApi.findClass(classLoader, "com.tencent.mobileqq.utils.JumpActionLegacy");
                if (findClass2 != null) {
                    int i2 = 0;
                    for (Method method4 : findClass2.getDeclaredMethods()) {
                        Class<?>[] parameterTypes2 = method4.getParameterTypes();
                        if (parameterTypes2.length == 4 && parameterTypes2[0] == Bundle.class && parameterTypes2[1] == HashMap.class && parameterTypes2[2] == Integer.TYPE && parameterTypes2[3] == Integer.TYPE) {
                            method4.setAccessible(true);
                            HookApi.before(method4, new HookApi.Action() {
                                @Override
                                public void call(XC_MethodHook.MethodHookParam methodHookParam) {
                                    try {
                                        Object obj = methodHookParam.args[1];
                                        if (obj instanceof Map) {
                                            ArkSigner.onShareMap((Map) obj);
                                        }
                                    } catch (Throwable th4) {
                                        HookLog.w("ShareHook.map: " + th4);
                                    }
                                }
                            });
                            i2++;
                        }
                    }
                    miniDbg("JumpActionLegacy 挂上 " + i2 + " 个 HashMap 入口");
                } else {
                    miniDbg("找不到 JumpActionLegacy");
                }
            } catch (Throwable th4) {
                miniDbg("挂 JumpActionLegacy 失败: " + th4);
            }
            shareHooked = true;
            HookLog.i("ShareHook: JumpActivity 已挂");
        }
    }

    public static void onShareIntent(Bundle bundle) {
        Context appContext = QQEnv.appContext();
        if (appContext == null) {
            return;
        }
        boolean z = HookConfig.getBoolean(appContext, K_SHARE, false);
        StringBuilder sb = new StringBuilder();
        try {
            for (String str : bundle.keySet()) {
                Object obj = bundle.get(str);
                if (obj != null) {
                    String valueOf = String.valueOf(obj);
                    if (valueOf.length() > 160) {
                        valueOf = valueOf.substring(0, 160) + "…";
                    }
                    sb.append(str).append(" = ").append(valueOf).append("\n");
                }
            }
        } catch (Throwable th) {
        }
        miniDbg("=== 分享 Intent ===\n" + ((Object) sb));
        if (z) {
            miniDbg("分享改写命中 " + rewriteBundle(bundle, HookConfig.get(appContext, K_SHARE_TITLE, ""), HookConfig.get(appContext, K_SHARE_DESC, ""), HookConfig.get(appContext, K_SHARE_PIC, ""), HookConfig.get(appContext, K_SHARE_URL, ""), HookConfig.get(appContext, K_SHARE_MUSIC, ""), 0) + " 处");
        }
    }

    private static String b64d(String str) {
        if (str == null || str.length() < 4 || str.length() % 4 != 0) {
            return str;
        }
        try {
            String str2 = new String(Base64.decode(str, 0), UTF8);
            for (int i = 0; i < str2.length(); i++) {
                char charAt = str2.charAt(i);
                if (charAt < ' ' && charAt != '\n' && charAt != '\r' && charAt != '\t') {
                    return str;
                }
            }
            return str2;
        } catch (Throwable th) {
            return str;
        }
    }

    private static String b64e(String str) {
        try {
            return Base64.encodeToString(str.getBytes(UTF8), 2);
        } catch (Throwable th) {
            return str;
        }
    }

    private static void dumpShareMap(Map<?, ?> map) {

        try {
            Object forced = HookConfig.get(QQEnv.appContext(), "share_force_appid", "0");
            if (forced != null && !forced.toString().equals("0") && map instanceof Map) {
                Map m = (Map) map;
                m.put("share_id", forced.toString());
                m.put("mini_program_appid", forced.toString());
            }
        } catch (Throwable th) {
        }
        try {
            Context ctx = QQEnv.appContext();
            if (ctx == null) {
                return;
            }
            StringBuilder sb = new StringBuilder();
            sb.append("=== shareMap ").append(System.currentTimeMillis()).append(" size=").append(map.size()).append("\n");
            for (Object o : map.entrySet()) {
                Map.Entry e = (Map.Entry) o;
                Object k = e.getKey();
                Object v = e.getValue();
                String vs = String.valueOf(v);
                if (vs.length() > 300) {
                    vs = vs.substring(0, 300) + "...";
                }
                String plain = vs;
                try {
                    String d = b64d(vs);
                    if (!d.equals(vs)) {
                        plain = d;
                    }
                } catch (Throwable th) {
                }
                sb.append(k).append("  [").append(v == null ? "null" : v.getClass().getSimpleName()).append("]\n");
                sb.append("   raw=").append(vs).append("\n");
                if (!plain.equals(vs)) {
                    sb.append("   b64=").append(plain.length() > 300 ? plain.substring(0, 300) : plain).append("\n");
                }
            }
            File f = new File(ctx.getFilesDir(), "share_dump.txt");
            if (f.length() > 400000) {
                f.delete();
            }
            FileOutputStream fos = new FileOutputStream(f, true);
            fos.write(sb.toString().getBytes(UTF8));
            fos.close();
        } catch (Throwable th) {
        }
    }

    public static void onShareMap(Map<?, ?> map) {
        int i = 0;
        String str = null;
        Context appContext = QQEnv.appContext();
        if (appContext == null) {
            return;
        }
        dumpShareMap(map);
        int i2 = 0;
        if (HookConfig.getBoolean(appContext, K_SHARE, false)) {
            String str2 = HookConfig.get(appContext, K_SHARE_TITLE, "");
            String str3 = HookConfig.get(appContext, K_SHARE_DESC, "");
            String str4 = HookConfig.get(appContext, K_SHARE_PIC, "");
            String str5 = HookConfig.get(appContext, K_SHARE_URL, "");
            String str6 = HookConfig.get(appContext, K_SHARE_MUSIC, "");
            StringBuilder sb = new StringBuilder();
            try {
                Iterator it = new ArrayList(map.entrySet()).iterator();
                i = 0;
                while (it.hasNext()) {
                    try {
                        Map.Entry entry = (Map.Entry) it.next();
                        Object key = entry.getKey();
                        Object value = entry.getValue();
                        if ((key instanceof String) && (value instanceof String)) {
                            String str7 = (String) key;
                            String str8 = (String) value;
                            String b64d = b64d(str8);
                            boolean z = !b64d.equals(str8);
                            if (sb.length() < 900) {
                                sb.append(str7).append(" = ").append(b64d).append(" | ");
                            }
                            String lowerCase = str7.toLowerCase();
                            if (looksUrl(b64d) && str6.length() > 0 && (lowerCase.contains("music") || lowerCase.contains("audio") || lowerCase.contains("voice"))) {
                                str = str6;
                            } else if (looksUrl(b64d) && str4.length() > 0 && (lowerCase.contains("image") || lowerCase.contains("img") || lowerCase.contains(MediaVault.T_PIC) || lowerCase.contains("thumb") || lowerCase.contains("cover") || looksImage(str8))) {
                                str = str4;
                            } else if (looksUrl(b64d) && str5.length() > 0 && (lowerCase.contains("url") || lowerCase.contains("link") || lowerCase.contains("jump") || lowerCase.contains("target"))) {
                                str = str5;
                            } else if (str2.length() > 0 && lowerCase.contains("title")) {
                                str = str2;
                            } else if (str3.length() > 0 && (lowerCase.contains("desc") || lowerCase.contains("summary") || lowerCase.contains("brief"))) {
                                str = str3;
                            } else {
                                str = null;
                            }
                            if (str != null && !str.equals(b64d)) {
                                if (z) {
                                    str = b64e(str);
                                }
                                ((Map) map).put(key, str);
                                i++;
                            }
                        }
                    } catch (Throwable th) {
                        i2 = i;
                        i = i2;
                        miniDbg("分享 HashMap 改写 " + i + " 处 | " + ((Object) sb));
                    }
                }
            } catch (Throwable th2) {
            }
            miniDbg("分享 HashMap 改写 " + i + " 处 | " + ((Object) sb));
        }
    }

    private static int rewriteBundle(Bundle bundle, String str, String str2, String str3, String str4, int i) {
        return rewriteBundle(bundle, str, str2, str3, str4, "", i);
    }

    private static int rewriteBundle(Bundle bundle, String str, String str2, String str3, String str4, String str5, int i) {
        String str6 = null;
        int i2 = 0;
        if (bundle == null || i > 3) {
            return 0;
        }
        try {
            Iterator it = new ArrayList(bundle.keySet()).iterator();
            while (it.hasNext()) {
                String str7 = (String) it.next();
                Object obj = bundle.get(str7);
                if (obj instanceof Bundle) {
                    i2 += rewriteBundle((Bundle) obj, str, str2, str3, str4, str5, i + 1);
                } else if (obj instanceof String) {
                    String str8 = (String) obj;
                    String lowerCase = str7.toLowerCase();
                    if (looksUrl(str8) && str5.length() > 0 && (lowerCase.contains("musicurl") || lowerCase.contains("audio") || lowerCase.contains("voice") || lowerCase.contains("song") || lowerCase.contains(".mp3") || lowerCase.contains("music"))) {
                        str6 = str5;
                    } else if (looksUrl(str8) && str3.length() > 0 && (lowerCase.contains("image") || lowerCase.contains("img") || lowerCase.contains(MediaVault.T_PIC) || lowerCase.contains("thumb") || lowerCase.contains("cover") || looksImage(str8))) {
                        str6 = str3;
                    } else if (looksUrl(str8) && str4.length() > 0 && (lowerCase.contains("url") || lowerCase.contains("link") || lowerCase.contains("jump") || lowerCase.contains("target") || lowerCase.contains("shareurl"))) {
                        str6 = str4;
                    } else {
                        if (str.length() <= 0 || (!lowerCase.equals("title") && !lowerCase.contains("title") && !lowerCase.equals("subject"))) {
                            if (str2.length() > 0 && (lowerCase.contains("desc") || lowerCase.contains("summary") || lowerCase.contains("content"))) {
                                str6 = str2;
                            } else if (str.length() <= 0 || (!lowerCase.contains("brief") && !lowerCase.contains("prompt") && !lowerCase.equals("name"))) {
                                str6 = null;
                            }
                        }
                        str6 = str;
                    }
                    if (str6 != null && !str6.equals(str8)) {
                        bundle.putString(str7, str6);
                        i2++;
                    }
                }
            }
        } catch (Throwable th) {
        }
        return i2;
    }

    private static boolean looksUrl(String str) {
        return str.startsWith("http://") || str.startsWith("https://");
    }

    private static boolean looksImage(String str) {
        String lowerCase = str.toLowerCase();
        return lowerCase.contains(".jpg") || lowerCase.contains(".jpeg") || lowerCase.contains(".png") || lowerCase.contains(".gif") || lowerCase.contains(".webp") || lowerCase.contains("image");
    }

    public static void miniDbg(String str) {
        String str2;
        try {
            str2 = new SimpleDateFormat("MM-dd HH:mm:ss", Locale.US).format(new Date()) + " " + str + "\n";
        } catch (Throwable th) {
            str2 = str + "\n";
        }

        try {
            Log.e("CATHELPER", "[sign] " + str);
        } catch (Throwable th7) {
        }

        try {
            Context appContext = QQEnv.appContext();
            if (appContext != null) {
                File file = new File(appContext.getFilesDir(), "minishare_debug.txt");
                if (file.length() > 204800) {
                    file.delete();
                }
                FileOutputStream fileOutputStream2 = new FileOutputStream(file, true);
                fileOutputStream2.write(str2.getBytes(UTF8));
                fileOutputStream2.close();
            }
        } catch (Throwable th5) {
        }

    }

    public static final String SHADOW_CMD = "trpc.ecom.api_gateway.ApiGateway.SsoForward";

    private static String digArk(org.json.JSONObject o) {
        if (o == null) return null;
        String[] keys = {"arKMsg", "ark", "signed_ark", "content", "arkMsg"};
        for (String k : keys) {
            Object v = o.opt(k);
            if (v instanceof String) {
                String t = (String) v;
                if (t.trim().startsWith("{")) return t.trim();
                try {
                    String dec = new String(android.util.Base64.decode(t, 0), "UTF-8");
                    if (dec.trim().startsWith("{")) return dec.trim();
                } catch (Throwable ignore) {}
            }
            if (v instanceof org.json.JSONObject) {
                String t = ((org.json.JSONObject) v).toString();
                if (t.contains("\"app\"") || t.contains("\"view\"")) return t;
                String d = digArk((org.json.JSONObject) v);
                if (d != null) return d;
            }
        }
        String all = o.toString();
        if (all.contains("\"app\"") && all.contains("\"view\"")) return all;
        return null;
    }

    private static String cut(String s) {
        if (s == null) return "null";
        return s.length() > 300 ? s.substring(0, 300) + "…" : s;
    }

}
