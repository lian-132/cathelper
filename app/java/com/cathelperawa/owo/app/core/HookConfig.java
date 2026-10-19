package com.cathelperawa.owo.app.core;

import android.content.Context;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

public final class HookConfig {

    private static final String FILE = "console";
    private static volatile Properties props;
    private static long lastLoad = 0;

    private HookConfig() {}

    private static Properties p(Context ctx) {

        long now = System.currentTimeMillis();
        if (props == null || now - lastLoad > 2000) {
            Properties np = new Properties();
            if (ctx != null) {
                File f = new File(ctx.getFilesDir(), FILE);
                if (f.exists()) {

                    try (FileInputStream in = new FileInputStream(f)) {
                        np.load(in);
                    } catch (Throwable ignore) {
                    }
                }
            }
            props = np;
            lastLoad = now;
        }
        return props;
    }

    public static void reload() {
        props = null;
        lastLoad = 0;
    }

    public static String get(Context ctx, String key, String def) {        try {
            return p(ctx).getProperty(key, def);
        } catch (Throwable t) {
            return def;
        }
    }

    public static boolean getBoolean(Context ctx, String key, boolean def) {
        try {
            String v = p(ctx).getProperty(key);
            if (v == null) return def;
            return "true".equalsIgnoreCase(v) || "1".equals(v);
        } catch (Throwable t) {
            return def;
        }
    }

    public static void set(Context ctx, String key, String value) {
        try {
            Properties np = new Properties();
            File f = new File(ctx.getFilesDir(), FILE);
            if (f.exists()) {
                try (FileInputStream in = new FileInputStream(f)) {
                    np.load(in);
                } catch (Throwable ignore) {
                }
            }
            np.setProperty(key, value);
            try (FileOutputStream out = new FileOutputStream(f)) {
                np.store(out, "cathelper config");
            }
            lastLoad = 0;
        } catch (Throwable ignore) {
        }
    }

    public static boolean isEnabled(Context ctx, HookItemBase item) {

        return getBoolean(ctx, item.switchKey(), false);
    }

    public static void setEnabled(Context ctx, HookItemBase item, boolean on) {
        set(ctx, item.switchKey(), String.valueOf(on));
    }
}
