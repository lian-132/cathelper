package com.cathelperawa.owo.app.pan;

import android.content.Context;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;

public final class PanPrefs {

    private PanPrefs() {}

    private static final String FILE = "pan_prefs.json";
    private static final Object LOCK = new Object();
    private static JSONObject cache;

    private static JSONObject load(Context c) {
        if (cache != null) return cache;
        JSONObject o = null;
        File f = new File(c.getFilesDir(), FILE);
        if (f.exists()) {
            InputStream in = null;
            try {
                in = new FileInputStream(f);
                ByteArrayOutputStream bo = new ByteArrayOutputStream();
                byte[] buf = new byte[4096];
                int n;
                while ((n = in.read(buf)) > 0) bo.write(buf, 0, n);
                o = new JSONObject(new String(bo.toByteArray(), "UTF-8"));
            } catch (Throwable ignore) {
            } finally {
                try {
                    if (in != null) in.close();
                } catch (Throwable ignore) {
                }
            }
        }
        if (o == null) o = new JSONObject();
        cache = o;
        return cache;
    }

    private static void save(Context c, JSONObject o) {
        synchronized (LOCK) {
            cache = o;
            File f = new File(c.getFilesDir(), FILE);
            File tmp = new File(c.getFilesDir(), FILE + ".tmp");
            FileOutputStream os = null;
            try {
                os = new FileOutputStream(tmp);
                os.write(o.toString().getBytes("UTF-8"));
                os.flush();
                os.close();
                os = null;
                if (!tmp.renameTo(f)) {
                    f.delete();
                    tmp.renameTo(f);
                }
            } catch (Throwable ignore) {
                try {
                    if (os != null) os.close();
                } catch (Throwable ignore2) {
                }
            }
        }
    }

    private static String getStr(Context c, String k, String def) {
        String v = load(c).optString(k, def);
        return v == null || v.length() == 0 ? def : v;
    }

    private static void put(Context c, String k, Object v) {
        synchronized (LOCK) {
            try {
                JSONObject o = new JSONObject(load(c).toString());
                o.put(k, v);
                save(c, o);
            } catch (Throwable ignore) {
            }
        }
    }

    public static String platform(Context c) {
        return getStr(c, "platform", "quark");
    }

    public static void setPlatform(Context c, String v) {
        put(c, "platform", v);
    }

    public static String subDir(Context c) {
        String v = getStr(c, "subDir", PanDownloader.FOLDER).trim();
        return v.length() == 0 ? PanDownloader.FOLDER : v;
    }

    public static void setSubDir(Context c, String v) {
        String t = v == null ? "" : v.trim().replace("/", "_").replace("\\", "_");
        put(c, "subDir", t.length() == 0 ? PanDownloader.FOLDER : t);
    }

    public static boolean autoClipboard(Context c) {
        return load(c).optBoolean("autoClip", true);
    }

    public static void setAutoClipboard(Context c, boolean v) {
        put(c, "autoClip", v);
    }

    public static boolean autoEnterDir(Context c) {
        return load(c).optBoolean("autoDir", true);
    }

    public static void setAutoEnterDir(Context c, boolean v) {
        put(c, "autoDir", v);
    }

    public static int retry(Context c) {
        return Math.max(0, Math.min(5, load(c).optInt("retry", 1)));
    }

    public static void setRetry(Context c, int v) {
        put(c, "retry", Math.max(0, Math.min(5, v)));
    }

    public static int threads(Context c) {
        int v = load(c).optInt("threads", 8);
        return Math.max(1, v);
    }

    public static void setThreads(Context c, int v) {
        put(c, "threads", Math.max(1, v));
    }

    public static int maxConcurrent(Context c) {
        int v = load(c).optInt("maxTask", 3);
        return Math.max(1, v);
    }

    public static void setMaxConcurrent(Context c, int v) {
        put(c, "maxTask", Math.max(1, v));
    }

    public static boolean autoResume(android.content.Context c) {
        return "1".equals(getString(c, "auto_resume", "1"));
    }

    public static void setAutoResume(android.content.Context c, boolean v) {
        setString(c, "auto_resume", v ? "1" : "0");
    }

    public static int speedLimitKb(Context c) {
        return Math.max(0, load(c).optInt("speedKb", 0));
    }

    public static void setSpeedLimitKb(Context c, int v) {
        put(c, "speedKb", Math.max(0, v));
    }

    public static String getString(Context c, String k, String def) {
        return getStr(c, k, def);
    }

    public static void setString(Context c, String k, String v) {
        put(c, k, v);
    }

    public static boolean wipeWebViewCookies(Context c) {
        return load(c).optBoolean("wipeWebView", false);
    }

    public static void setWipeWebViewCookies(Context c, boolean v) {
        put(c, "wipeWebView", v);
    }

    public static boolean askGuest(Context c) {
        return load(c).optBoolean("askGuest", true);
    }

    public static void setAskGuest(Context c, boolean v) {
        put(c, "askGuest", v);
    }
}
