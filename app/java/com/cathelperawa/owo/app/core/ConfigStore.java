package com.cathelperawa.owo.app.core;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashMap;
import java.util.Map;

public final class ConfigStore {

    private ConfigStore() {}

    private static final Map<String, Object> DEFAULTS = new HashMap<String, Object>();

    static {

        put("cat_total",              false);
        put("cat_base",               false);
        put("cat_wave",               false);
        put("cat_paw",                false);
        put("cat_catNameSwitch",      false);
        put("cat_masterNameSwitch",   false);
        put("cat_spoiled",            false);
        put("cat_endMeow",            false);
        put("cat_emoticon",           false);
        put("cat_autoSuffix",         false);
        put("cat_keywordReply",       false);
        put("cat_randomCute",         false);
        put("cat_trainSwitch",        false);
        put("cat_triggerOnPaste",     false);

        putS("cat_catName",           "猫猫");
        putS("cat_masterName",        "主人");
        putS("cat_trainPrefix",       "");
        putS("cat_trainSuffix",       "");
        putS("cat_trainKeywords",     "");
        putS("cat_customReplyText",   "");

        put("cap_enabled",            false);
        put("cap_auto",               false);
        put("cap_scopeAll",           false);

        put("hb_auto",                false);
        put("hb_reply",               false);

        putI("float_size",            34);
        put("float_enabled",          false);

        putS("ui_style",              "default");
    }

    private static void put(String key, boolean v)  { DEFAULTS.put(key, Boolean.valueOf(v)); }
    private static void putS(String key, String v)  { DEFAULTS.put(key, v); }
    private static void putI(String key, int v)     { DEFAULTS.put(key, Integer.valueOf(v)); }

    private static volatile SharedPreferences sp;
    private static volatile Context appCtx;

    public static void init(Context ctx) {
        if (ctx == null) return;
        appCtx = ctx.getApplicationContext();
        if (sp == null) {
            sp = appCtx.getSharedPreferences("cathelper_console", Context.MODE_PRIVATE);
        }
    }

    private static final Map<String, Object> MEMORY = new HashMap<String, Object>();

    public static boolean getBool(String key) {
        Object def = DEFAULTS.get(key);
        boolean d = (def instanceof Boolean) ? (Boolean) def : false;
        SharedPreferences p = sp;
        if (p != null) return p.getBoolean(key, d);
        Object m = MEMORY.get(key);
        return m instanceof Boolean ? (Boolean) m : d;
    }

    public static int getInt(String key) {
        Object def = DEFAULTS.get(key);
        int d = (def instanceof Integer) ? (Integer) def : 0;
        SharedPreferences p = sp;
        if (p != null) return p.getInt(key, d);
        Object m = MEMORY.get(key);
        return m instanceof Integer ? (Integer) m : d;
    }

    public static String getString(String key) {
        Object def = DEFAULTS.get(key);
        String d = (def instanceof String) ? (String) def : "";
        SharedPreferences p = sp;
        if (p != null) return p.getString(key, d);
        Object m = MEMORY.get(key);
        return m instanceof String ? (String) m : d;
    }

    public static void setBool(String key, boolean v) {
        SharedPreferences p = sp;
        if (p != null) { p.edit().putBoolean(key, v).apply(); return; }
        MEMORY.put(key, Boolean.valueOf(v));
    }

    public static void setInt(String key, int v) {
        SharedPreferences p = sp;
        if (p != null) { p.edit().putInt(key, v).apply(); return; }
        MEMORY.put(key, Integer.valueOf(v));
    }

    public static void setString(String key, String v) {
        SharedPreferences p = sp;
        if (p != null) { p.edit().putString(key, v).apply(); return; }
        MEMORY.put(key, v);
    }

    public static boolean isRegistered(String key) {
        return DEFAULTS.containsKey(key);
    }

    public static java.util.Set<String> registeredKeys() {
        return java.util.Collections.unmodifiableSet(DEFAULTS.keySet());
    }
}
