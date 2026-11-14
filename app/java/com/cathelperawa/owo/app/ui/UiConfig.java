package com.cathelperawa.owo.app.ui;

import android.content.Context;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

public final class UiConfig {

    private UiConfig() {}

    private static final String FILE = "console";

    private static volatile Properties cache;
    private static volatile long cacheAt = 0L;
    private static final long CACHE_TTL = 1000L;

    private static Properties propsOf(Context c) {
        long now = System.currentTimeMillis();
        Properties p = cache;
        if (p != null && now - cacheAt < CACHE_TTL) return p;
        Properties np = new Properties();
        try {
            File f = new File(c.getFilesDir(), FILE);
            if (f.exists()) {
                try (FileInputStream in = new FileInputStream(f)) {
                    np.load(in);
                }
            }
        } catch (Throwable ignore) {
        }
        cache = np;
        cacheAt = now;
        return np;
    }

    public static void invalidate() {
        cache = null;
        cacheAt = 0L;
    }

    public static boolean get(Context c, String key, boolean def) {

        if ("cat_total".equals(key)) def = false;

        try {
            String ov = com.cathelperawa.owo.app.core.ConfigOverride.get(key);
            if (ov != null) {
                return "1".equals(ov) || "true".equalsIgnoreCase(ov);
            }
        } catch (Throwable ignore) {
        }
        try {
            Properties p = propsOf(c);
            return "1".equals(p.getProperty(key, def ? "1" : "0"));
        } catch (Throwable t) {
            return def;
        }
    }

    public static void set(Context c, String key, boolean v) {
        try {
            File f = new File(c.getFilesDir(), FILE);
            Properties p = new Properties();
            if (f.exists()) {
                try (FileInputStream in = new FileInputStream(f)) {
                    p.load(in);
                } catch (Throwable ignore) {
                }
            }
            p.setProperty(key, v ? "1" : "0");
            try (FileOutputStream out = new FileOutputStream(f)) {
                p.store(out, null);
            }
            invalidate();
            com.cathelperawa.owo.app.core.HookLog.i("[UI] cfgSet " + key + "=" + v
                    + " path=" + f.getAbsolutePath());
        } catch (Throwable t) {
            com.cathelperawa.owo.app.core.HookLog.e("[UI] cfgSet 失败", t);
        }
    }

    public static String cfgKey(String uiKey) {
        if ("master".equals(uiKey) || "cat".equals(uiKey)) return "cat_total";
        if ("revoke".equals(uiKey)) return "chat_revoke";
        return uiKey;
    }

    public static boolean swOn(Context c, String uiKey) {
        String k = cfgKey(uiKey);

        boolean def = "chat_revoke".equals(k)
                || "hook_capture".equals(k)
                || "hook_autocap".equals(k)
                || "chat_customdice".equals(k)
                || "chat_emojiimg".equals(k)
                || "chat_flashpic".equals(k)
                || "chat_edittext".equals(k)
                || "cat_total".equals(k)

                || "u_forward".equals(k)
                || "u_patpat".equals(k);
        return get(c, k, def);
    }

    public static void swSet(Context c, String uiKey, boolean v) {
        set(c, cfgKey(uiKey), v);
    }

    public static String getString(Context c, String key, String def) {
        try {
            File f = new File(c.getFilesDir(), FILE);
            if (!f.exists()) return def;
            Properties p = new Properties();
            try (FileInputStream in = new FileInputStream(f)) {
                p.load(in);
            }
            return p.getProperty(key, def);
        } catch (Throwable t) {
            return def;
        }
    }

    public static void setString(Context c, String key, String v) {
        try {
            File f = new File(c.getFilesDir(), FILE);
            Properties p = new Properties();
            if (f.exists()) {
                FileInputStream in = new FileInputStream(f);
                p.load(in); in.close();
            }
            p.setProperty(key, v == null ? "" : v);
            FileOutputStream out = new FileOutputStream(f);
            p.store(out, null); out.close();
        } catch (Throwable t) {
            com.cathelperawa.owo.app.core.HookLog.e("[UI] cfgSetS 失败", t);
        }
    }
}
