package com.cathelperawa.owo.app.ui;

import android.content.Context;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

public final class UiConfig {

    private UiConfig() {}

    private static final String FILE = "console";

    public static boolean get(Context c, String key, boolean def) {

        if ("cat_total".equals(key)) def = false;
        try {
            File f = new File(c.getFilesDir(), FILE);
            if (!f.exists()) return def;
            Properties p = new Properties();
            try (FileInputStream in = new FileInputStream(f)) {
                p.load(in);
            }
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

        return get(c, k, false);
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
