package com.cathelperawa.owo.app.api;

import android.content.Context;

import com.cathelperawa.owo.app.bridge.QQEnv;
import com.cathelperawa.owo.app.core.HookConfig;

public final class ConfigApi {

    private ConfigApi() {}

    
    public static boolean isCaptureEnabled() { return getBool("hook_capture", true); }

    public static void setCaptureEnabled(boolean on) { setBool("hook_capture", on); }

    public static boolean isAutoCaptureEnabled() { return getBool("hook_autocap", true); }

    public static void setAutoCaptureEnabled(boolean on) { setBool("hook_autocap", on); }

    public static boolean isCatifyTotalEnabled() { return getBool("cat_total", false); }

    public static void setCatifyTotalEnabled(boolean on) { setBool("cat_total", on); }

    public static boolean isFeatureEnabled(String key) { return getBool(key, true); }

    public static void setFeatureEnabled(String key, boolean on) { setBool(key, on); }

    
    public static String get(String key, String def) {
        Context ctx = QQEnv.appContext();
        return ctx == null ? def : HookConfig.get(ctx, key, def);
    }

    public static void set(String key, String value) {
        Context ctx = QQEnv.appContext();
        if (ctx != null) HookConfig.set(ctx, key, value);
    }

    public static boolean getBool(String key, boolean def) {
        Context ctx = QQEnv.appContext();
        return ctx == null ? def : HookConfig.getBoolean(ctx, key, def);
    }

    public static void setBool(String key, boolean val) { set(key, String.valueOf(val)); }
}
