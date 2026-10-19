package com.cathelperawa.owo.app.api;

import android.content.Context;

import com.cathelperawa.owo.app.bridge.QQEnv;
import com.cathelperawa.owo.app.util.CatLog;

public final class UtilApi {

    private UtilApi() {}

    public static long now() { return System.currentTimeMillis(); }

    public static String formatTime(long ms, String pattern) {
        try {
            return new java.text.SimpleDateFormat(pattern, java.util.Locale.getDefault())
                    .format(new java.util.Date(ms));
        } catch (Throwable t) {
            CatLog.d("formatTime 失败: " + t);
            return String.valueOf(ms);
        }
    }

    public static void runAsync(final Runnable r) {
        new Thread(new Runnable() {
            @Override public void run() {
                try { r.run(); } catch (Throwable t) { CatLog.e("runAsync 异常", t); }
            }
        }, "cathelper-task").start();
    }

    public static void postDelayed(Runnable r, long delayMs) {
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(r, delayMs);
    }

    public static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (Throwable ignore) {}
    }

    public static String jsonPretty(String json) {
        try {
            String s = json.trim();
            if (s.startsWith("{")) return new org.json.JSONObject(s).toString(2);
            if (s.startsWith("[")) return new org.json.JSONArray(s).toString(2);
            return json;
        } catch (Throwable t) {
            CatLog.d("jsonPretty 失败: " + t);
            return json;
        }
    }

    public static long hostVersionCode() {
        try {
            Context ctx = QQEnv.appContext();
            if (ctx == null) return 0;
            return ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0).versionCode;
        } catch (Throwable t) {
            return 0;
        }
    }

    public static String hostVersionName() {
        try {
            Context ctx = QQEnv.appContext();
            if (ctx == null) return null;
            return ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0).versionName;
        } catch (Throwable t) {
            return null;
        }
    }
}
