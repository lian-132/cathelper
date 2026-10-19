package com.cathelperawa.owo.app;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;

import de.robv.android.xposed.XposedBridge;

public class ModLog {

    private static final String TAG = "猫娘助手";
    private static volatile Context appCtx;

    public static void setContext(Context ctx) {
        if (ctx != null && appCtx == null) appCtx = ctx.getApplicationContext();
    }

    public static void log(String msg) {

        try {
            XposedBridge.log("[" + TAG + "] " + msg);
        } catch (Throwable ignore) {
        }

        try {
            com.cathelperawa.owo.app.util.CatLog.i(msg);
        } catch (Throwable ignore) {
        }
    }

    public static void log(String msg, Throwable t) {
        log(msg + " -> " + (t == null ? "null" : t.toString()));
    }
}
