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
        put("cat_base",               true);
        put("cat_wave",               false);
        put("cat_paw",                false);
        put("cat_catNameSwitch",      false);
        put("cat_masterNameSwitch",   false);
        put("cat_spoiled",            true);
        put("cat_endMeow",            true);
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

        put("ai_enable",              false);
        put("ai_private",             true);
        put("ai_at",                  true);
        put("ai_keyword",             false);
        putS("ai_keywords",           "");
        putI("ai_cooldown",           8);
        putI("ai_hourly",             30);
        putS("ai_groups",             "");
        putS("ai_targets",            "");
        putS("ai_current_id",         "");

        putS("ai_persona", "你是「{{me}}」，一个在 QQ 上和人聊天的猫娘。\\n"
                + "正在和你说话的人叫「{{peer}}」。\\n\\n"
                + "说话规则：\\n"
                + "1. 短，像真人打字：一般 5~20 个字，最多不超过 40 字\\n"
                + "2. 口语化，可以用「呀 / 啦 / 嘛 / 哦 / 诶」这样的语气词\\n"
                + "3. 不要书面语（确实、诚然、不妨、颇为）\\n"
                + "4. 不要客服腔（请问、有什么可以帮您、感谢您的反馈）\\n"
                + "5. 不要心理咨询腔（我理解你的感受、听起来你、这很正常）\\n"
                + "6. 不要说「作为 AI」「我是 AI 助手」这类话\\n"
                + "7. 不要用 markdown、不要分点、不要列清单、不要加粗\\n"
                + "8. 一条最多一个 emoji，也可以不用\\n"
                + "9. 不要复述对方的话，不要重复聊天记录里出现过的句式\\n"
                + "10. 就这么自然聊，别每句都问「还需要什么帮助吗」");
        putI("ai_merge_ms",           1500);
        put("ai_segment",             true);
        putI("ai_seg_min",            1);
        putI("ai_seg_max",            3);
        putI("ai_seg_parts",          4);
        put("ai_quote",               false);

        put("ai_affinity",            true);
        put("ai_affinity_auto",       true);
        putI("ai_affinity_every",     2);
        putI("ai_affinity_gap",       90);
        putI("ai_affinity_step",      10);
        putI("ai_affinity_daily",     25);
        putI("ai_affinity_init",      50);
        putI("ai_affinity_log",       5);
        putI("ai_affinity_t1",        25);
        putI("ai_affinity_t2",        45);
        putI("ai_affinity_t3",        65);
        putI("ai_affinity_t4",        85);
        putS("ai_admins",             "");

        put("ai_search_on",           false);
        putS("music_cookie",          "");
        putS("ai_search_url",         "");
        putS("ai_search_key",         "");
        putS("ai_search_auth",        "bearer");
        putS("ai_search_header",      "");
        putS("ai_search_method",      "post");
        putI("ai_search_limit",       5);
        put("ai_affinity_tone",       true);
        put("ai_affinity_decay",      true);
        putI("ai_affinity_decay_days", 14);
        put("ai_script",              true);
        put("ai_script_tool",         true);

        put("group_owner_menu",       false);
        put("group_menu_extract", false);

        put("hb_auto",                false);
        put("hb_reply",               true);

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

    private static SharedPreferences ensure() {
        if (sp != null) return sp;
        try {
            Context c = com.cathelperawa.owo.app.bridge.QQEnv.appContext();
            if (c != null) init(c);
        } catch (Throwable ignore) {
        }
        return sp;
    }

    private static final Map<String, Object> MEMORY = new HashMap<String, Object>();

    public static boolean getBool(String key) {
        Object def = DEFAULTS.get(key);
        boolean d = (def instanceof Boolean) ? (Boolean) def : false;
        SharedPreferences p = ensure();
        if (p != null) return p.getBoolean(key, d);
        Object m = MEMORY.get(key);
        return m instanceof Boolean ? (Boolean) m : d;
    }

    public static int getInt(String key) {
        Object def = DEFAULTS.get(key);
        int d = (def instanceof Integer) ? (Integer) def : 0;
        SharedPreferences p = ensure();
        if (p != null) return p.getInt(key, d);
        Object m = MEMORY.get(key);
        return m instanceof Integer ? (Integer) m : d;
    }

    public static String getString(String key) {
        Object def = DEFAULTS.get(key);
        String d = (def instanceof String) ? (String) def : "";
        SharedPreferences p = ensure();
        if (p != null) return p.getString(key, d);
        Object m = MEMORY.get(key);
        return m instanceof String ? (String) m : d;
    }

    public static void setBool(String key, boolean v) {
        SharedPreferences p = ensure();
        if (p != null) { p.edit().putBoolean(key, v).apply(); return; }
        MEMORY.put(key, Boolean.valueOf(v));
    }

    public static void setInt(String key, int v) {
        SharedPreferences p = ensure();
        if (p != null) { p.edit().putInt(key, v).apply(); return; }
        MEMORY.put(key, Integer.valueOf(v));
    }

    public static void setString(String key, String v) {
        SharedPreferences p = ensure();
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
