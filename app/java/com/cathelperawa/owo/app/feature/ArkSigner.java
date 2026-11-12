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
    
    private static final String BILI_APP = "com.tencent.miniapp_01";
    public static final String BILI_APPID = "1109937557";
    public static final String BUILTIN_WX_APPIDS = "1109840991|腾讯视频;1109937557|哔哩哔哩;1109907872|QQ安全中心;1109048181|腾讯客服;1109786902|订阅号;1109896843|老师助手;1109836759|火柴人神射手;1110112648|腾讯原创馆";
    public static final String DEFAULT_TAG_ICON = "";
    public static final String ERR_NO_PSKEY = "签名通道都没成功（天玄 + 备用），稍后再试";

    
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
    
    public static final String PSKEY_PAGE = "";
    
    public static final String SKIP = "__SKIP__";
    private static final String UA = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/139.0.7258.143 Mobile Safari/537.36 V1_AND_SQ_9.1.35_8708_YYB_D QQ/9.1.35.22670 NetType/WIFI";

    private static final Charset UTF8 = Charset.forName("UTF-8");
    
    private static final String[] MUSIC_SIGN = {};
    public static final String[][] MUSIC_PLATFORMS = {new String[]{UIScope.QQ, "QQ音乐", MusicCardSheet.TAG_ICON_QQ}, new String[]{"163", "网易云音乐", MusicCardSheet.TAG_ICON_163}, new String[]{"kugou", "酷狗音乐", "https://open.gtimg.cn/open/app_icon/00/20/51/41/205141_100_m.png?t=1792029731"}, new String[]{"kuwo", "酷我音乐", "https://p.qpic.cn/qqconnect/0/app_100243533_1636374695/100?max-age=2592000&t=0"}, new String[]{"migu", "咪咕音乐", "https://open.gtimg.cn/open/app_icon/01/05/30/67/1101053067_100_m.png?t=1792056966"}, new String[]{"bilibili", "哔哩哔哩", "https://open.gtimg.cn/open/app_icon/00/95/17/76/100951776_100_m.png?t=1792658808"}};
    public static final String CH_AUTO = "auto";
    public static final String CH_TIANXUAN = "tianxuan";
    public static final String CH_YIBAI = "yibai";
    public static final String CH_SISI = "sisi";
    public static final String CH_XIANYU = "xianyu";
    public static final String[] CH_IDS = {CH_AUTO, CH_TIANXUAN, CH_YIBAI, CH_SISI};
    public static final String[] CH_LABELS = {"自动 · 第三方服务优先，失败退天玄", "天玄 · 官方签名（角标可自定义，要 p_skey）", "yibai 备用 · 免 p_skey（角标固定 QQ音乐）", "思思音卡 · 免 p_skey（可换角标平台）"};
    private static volatile boolean miniHooked = false;
    private static volatile boolean shareHooked = false;
    private static final HookApi.Action SHARE_ACTION = new HookApi.Action() { 
        @Override 
        public void call(XC_MethodHook.MethodHookParam methodHookParam) {  }
    };

    public interface HostCb {
        void done(String str, String str2);
    }

    public interface SendCb {
        void done(boolean z, String str);
    }

    private ArkSigner() { }

    public static final class Result {
        public final String error;
        public final String json;

        Result(String str, String str2) {
            this.json = str;
            this.error = str2;
        }

        public boolean ok() { return false; }
    }

    
    
    public static Result resign(String str) { return null; }

    public static Result signCustomArk(String str, String str2, String str3) { return null; }

    public static Result signCustomArkBiz(String str, String str2, String str3, String str4) { return null; }

    public static Result signCustomArkBiz(String str, String str2, String str3, String str4, String str5) { return null; }

    public static void experimentBizSrc(final String str, final String str2, final String str3, final String[] strArr, final SendCb sendCb) {  }

    public static void probeEgameSign(final String videoUrl, final String cover, final String title, final SendCb sendCb) {  }

    public static void probeArkPaths(final SendCb sendCb) {  }

    private static String shortName(String appId) { return null; }

    private static String clip(String s, int n) { return null; }

    private static void catLog(String s) {  }

    public static String buildShareDataMeta(String str, String str2, String str3, String str4, String str5, String str6) { return null; }

    public static String buildShareDataMeta(String str, String str2, String str3, String str4,
                                            String str5, String str6,
                                            int width, int height,
                                            org.json.JSONArray buttons, String jumpUrl) { return null; }

    public static String platformTag(String str) { return null; }

    public static String platformIcon(String str) { return null; }

    public static String safeJump(String str) { return null; }

    public static String safeImage(String str) { return null; }

    public static String cleanUrl(String str) { return null; }

    
    
    public static String signNewsAuto(String str) { return null; }

    public static Result signNewsFallback(String str, String str2, String str3, String str4) { return null; }

    public static String signLinkCard(String str, String str2, String str3, String str4, String str5) { return null; }

    public static String signMusic(String str, String str2, String str3, String str4, String str5) { return null; }

    public static String signMusic(String str, String str2, String str3, String str4, String str5, String str6, String str7, String str8) { return null; }

    public static String resignMusic(String str) { return null; }

    public static String channelLabel(String str) { return null; }

    public static String channelShort(String str) { return null; }

    public static boolean isServerSignedCard(String str) { return false; }

    public static boolean isMusicCard(String str) { return false; }

    private static JSONObject metaOf(JSONObject jSONObject) { return null; }

    
    
    public static Result signByChannel(String str, String str2) { return null; }

    private static Result signXianyu(String str) { return null; }

    private static String enc(String str) { return null; }

    private static String getPlain(String str) { return null; }

    private static Result signVia(String str, String str2, boolean z, boolean z2) { return null; }

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

    public static boolean openPskeyPage(Context context) { return false; }

    public static PskeyResult capturePskey() { return new PskeyResult(false, "", "未配置签名"); }

    public static PskeyResult readPskey(boolean z) { return new PskeyResult(false, "", "未配置签名"); }

    public static PskeyResult pskeyStatus() { return new PskeyResult(false, "", "未配置签名"); }

    private static String minimalNewsCard() { return null; }

    public static PskeyResult verifyPskey() { return new PskeyResult(false, "", "未配置签名"); }

    public static void clearPskey() {  }

    private static String head16(String str) { return null; }

    public static synchronized void dumpSent(String str, String str2) {  }

    private static synchronized void dumpErr(String str) {  }

    private static String postPlain(String str, String str2) { return null; }

    public static String autoPskey(String str) { return null; }

    private static String cachedCookie() { return null; }

    private static void saveCookie(String str) {  }

    public static String cookieFromManager() { return null; }

    public static String pskeyOf(String str) { return null; }

    public static String cookieFromDb() { return null; }

    private static String post(String str, String str2, String str3) { return null; }

    private static String appIdOf() { return null; }

    public static String qAppId() { return null; }

    private static String appNameOf() { return null; }

    public static synchronized void installMiniShare(ClassLoader classLoader) {  }

    private static int hookMiniShare(String str, String str2, ClassLoader classLoader) { return 0; }

    private static void hookMiniRsp(ClassLoader classLoader) {  }

    public static void onMiniRsp(JSONObject jSONObject) {  }

    
    public static void onMiniShareReq(Object obj) {  }

    private static String pbGet(Object obj, String str) { return null; }

    public static boolean pbSet(Object obj, String str, String str2) { return false; }

    public static String stripScheme(String str) { return null; }

    
    
    public static String uploadUguu(byte[] bArr) { return null; }

    public static void hostImage(final String str, final HostCb hostCb) {  }

    private static boolean reachable(String str) { return false; }

    public static void sendMiniCard(String str, String str2, String str3, String str4, SendCb sendCb) {  }

    public static void sendMiniCard(final String str, final String str2, final String str3, final String str4, final String str5, final String str6, final SendCb sendCb) {  }

    public static void sendWxMiniCard(final String str, final String str2, final String str3, final String str4, final String str5, final String str6, final int i, final SendCb sendCb) {  }

    private static volatile boolean sWxSending = false;

    private static void doSendWxMiniCard(String str, String str2, String str3, String str4, String str5, String str6, int i, final SendCb sendCb) throws Throwable {  }

    public static void sendGameCard(final String str, final String str2, final String str3, final String str4, final String str5, final String str6, final int i, final int i2, final SendCb sendCb) {  }

    public static void probeMiniAppIds(final String str, final SendCb sendCb) {  }

    public static void probeStep(final ClassLoader classLoader, final Object obj, final Method method, final Class<?> cls, final String[] strArr, final int i, final StringBuilder sb, final StringBuilder sb2, final SendCb sendCb) {  }

    public static String finishArk(JSONObject jSONObject, String str) { return null; }

    public static void sendArkNow(String str, SendCb sendCb) {  }

    public static void pbSetInt(Object obj, String str, int i) {  }

    
    
    public static synchronized void installShareProbe(ClassLoader classLoader) {  }

    public static void onShareIntent(Bundle bundle) {  }

    private static String b64d(String str) { return null; }

    private static String b64e(String str) { return null; }

    
    private static void dumpShareMap(Map<?, ?> map) {  }

    public static void onShareMap(Map<?, ?> map) {  }

    private static int rewriteBundle(Bundle bundle, String str, String str2, String str3, String str4, int i) { return 0; }

    private static int rewriteBundle(Bundle bundle, String str, String str2, String str3, String str4, String str5, int i) { return 0; }

    private static boolean looksUrl(String str) { return false; }

    private static boolean looksImage(String str) { return false; }

    public static void miniDbg(String str) {  }

    

    public static final String SHADOW_CMD = "trpc.ecom.api_gateway.ApiGateway.SsoForward";

    public static Result signShadow(final String arkJson) { return null; }

    private static String digArk(org.json.JSONObject o) { return null; }

    private static String cut(String s) { return null; }

}
