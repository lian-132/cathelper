package com.cathelperawa.owo.app.feature;

import android.content.Context;

import com.cathelperawa.owo.app.bili.BiliApi;
import com.cathelperawa.owo.app.bridge.CurrentChat;
import com.cathelperawa.owo.app.bridge.QQEnv;
import com.cathelperawa.owo.app.core.HookLog;
import com.cathelperawa.owo.app.ovo.QPacketHelper;
import com.cathelperawa.owo.app.ovo.QQInterfaces;

import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class BiliVideoCard {

    public static final String CMD = "OidbSvc.0xb77_9";

    private static final long APPID = 1105198412L;
    
    private static final long MSG_SEQ = 74515265555000L;

    private static final Pattern BV = Pattern.compile("BV[0-9A-Za-z]{10}");
    private static final Pattern LINK = Pattern.compile("https?://[^\\s\"'<>，。]+");

    private BiliVideoCard() { }

    public static final class Result {
        public boolean ok;
        public String error = "";
        public String title = "";
        public String bvid = "";
    }

    
    
    public interface Progress {
        void on(String step);
    }

    public static Result send(Context ctx, String input, String prompt, String jumpUrl) { return null; }

    public static Result send(Context ctx, String input, String prompt, String jumpUrl, Progress cb) { return null; }

    public static Result send(Context ctx, String input, String prompt, String jumpUrl,
                              int width, int height, org.json.JSONArray buttons,
                              String coverOverride, Progress cb) { return null; }

    private static Result sendGeneric(Context ctx, String input, String prompt,
                                      String jumpUrl, int width, int height,
                                      org.json.JSONArray buttons, String coverOverride,
                                      Progress cb) { return null; }

    
    public static String extractBvid(String input) { return null; }

    private static String followRedirect(String url) { return null; }

    private static String pickJump(String input, String bvid, String jumpUrl) { return null; }

    private static void dumpArk(String ark) {  }

    private static String buildArk(String title, String up, String cover, String mp4,
                                   String display) throws Exception { return null; }

    private static String buildArk(String title, String up, String cover, String mp4,
                                   String display, int width, int height,
                                   org.json.JSONArray buttons, String jumpUrl,
                                   String coverOverride) throws Exception { return null; }

    private static String buildPacket(String ark, String title, String cover, String share)
            throws Exception { return null; }

    private static long targetUin() { return 0L; }

    private static long parseLong(String s) { return 0L; }

    private static long uin() { return 0L; }
}
