package com.cathelperawa.owo.app.cookie;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;

import com.cathelperawa.owo.app.bridge.QQEnv;
import com.cathelperawa.owo.app.core.HookLog;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public final class CookieBridge {

    private CookieBridge() {}

    public static final String AUTHORITY = "com.cathelperawa.owo.app.cookie";
    public static final String HOST_PKG = "com.cathelperawa.owo.app";

    private static final long TTL_OK = 60_000L;
    private static final long TTL_MISS = 5_000L;
    
    private static final long CACHE_TTL = 7L * 24 * 3600 * 1000;
    
    private static final long WAKE_THROTTLE = 5 * 60 * 1000L;
    
    private static final long WAKE_WAIT_MS = 1200L;

    private static final String CACHE_DIR = "cathelper";
    private static final String CACHE_FILE = "cookie_cache.json";

    private static final class Cached {
        final String value;
        final long at;

        Cached(String v, long t) {
            value = v;
            at = t;
        }
    }

    private static final Map<String, Cached> CACHE = new HashMap<String, Cached>();
    private static volatile long lastWakeAt = 0L;

    

    
    public static String get(String platform) {
        if (platform == null || platform.length() == 0) return null;

        long now = System.currentTimeMillis();
        synchronized (CACHE) {
            Cached c = CACHE.get(platform);
            if (c != null) {
                long ttl = (c.value != null && c.value.length() > 0) ? TTL_OK : TTL_MISS;
                if (now - c.at < ttl) return c.value;
            }
        }

        String v = queryProvider(platform);
        if (v != null && v.length() > 0) {
            saveCache(platform, v);
            synchronized (CACHE) {
                CACHE.put(platform, new Cached(v, now));
            }
            return v;
        }

        String c2 = loadCache(platform);
        if (c2 != null && c2.length() > 0) {
            synchronized (CACHE) {
                CACHE.put(platform, new Cached(c2, now));
            }
            return c2;
        }

        if (wakeHost()) {
            String c3 = queryProvider(platform);
            if (c3 != null && c3.length() > 0) {
                saveCache(platform, c3);
                synchronized (CACHE) {
                    CACHE.put(platform, new Cached(c3, now));
                }
                return c3;
            }
        }

        synchronized (CACHE) {
            CACHE.put(platform, new Cached(null, now));
        }
        return null;
    }

    public static String forceFetch(String platform) {
        if (platform == null) return null;
        invalidate();
        wakeHostForce();
        String v = queryProvider(platform);
        if (v != null && v.length() > 0) {
            saveCache(platform, v);
            synchronized (CACHE) {
                CACHE.put(platform, new Cached(v, System.currentTimeMillis()));
            }
        }
        return v;
    }

    public static void invalidate() {
        synchronized (CACHE) {
            CACHE.clear();
        }
    }

    public static boolean available() {
        Context ctx = QQEnv.appContext();
        if (ctx == null) return false;
        Cursor cur = null;
        try {
            cur = ctx.getContentResolver().query(
                    Uri.parse("content://" + AUTHORITY + "/list"),
                    null, null, null, null);
            return cur != null;
        } catch (Throwable t) {
            return false;
        } finally {
            try { if (cur != null) cur.close(); } catch (Throwable ignore) { }
        }
    }

    public static boolean hasCache(String platform) {
        String c = loadCache(platform);
        return c != null && c.length() > 0;
    }

    

    private static String queryProvider(String platform) {
        Context ctx = QQEnv.appContext();
        if (ctx == null) return null;
        Cursor cur = null;
        try {
            Uri u = Uri.parse("content://" + AUTHORITY + "/get/" + Uri.encode(platform));
            cur = ctx.getContentResolver().query(u, null, null, null, null);
            if (cur == null || !cur.moveToFirst()) return null;
            int idx = cur.getColumnIndex("cookie");
            if (idx < 0) return null;
            String s = cur.getString(idx);
            if (s == null || s.trim().length() == 0) return null;
            return s.trim();
        } catch (Throwable t) {
            
            HookLog.w("CookieBridge 查询失败 " + platform + "：" + t);
            return null;
        } finally {
            try { if (cur != null) cur.close(); } catch (Throwable ignore) { }
        }
    }

    

    private static boolean wakeHost() {
        long now = System.currentTimeMillis();
        if (now - lastWakeAt < WAKE_THROTTLE) return false;
        lastWakeAt = now;
        return doWake();
    }

    private static boolean wakeHostForce() {
        lastWakeAt = System.currentTimeMillis();
        return doWake();
    }

    private static boolean doWake() {
        try {
            Context ctx = QQEnv.appContext();
            if (ctx == null) return false;
            Intent it = new Intent();
            it.setClassName(HOST_PKG, HOST_PKG + ".cookie.CookieWarmupActivity");
            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(it);
            try {
                Thread.sleep(WAKE_WAIT_MS);
            } catch (Throwable ignore) {
            }
            return true;
        } catch (Throwable t) {
            HookLog.w("CookieBridge 唤醒本体失败：" + t);
            return false;
        }
    }

    

    private static File cacheFile(Context ctx) {
        File dir = com.cathelperawa.owo.app.core.DataDir.account();
        if (!dir.exists()) {
            try { dir.mkdirs(); } catch (Throwable ignore) { }
        }
        return new File(dir, CACHE_FILE);
    }

    private static JSONObject readCacheFile(Context ctx) {
        File f = cacheFile(ctx);
        if (!f.exists()) return new JSONObject();
        InputStream in = null;
        try {
            in = new FileInputStream(f);
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) > 0) bo.write(buf, 0, n);
            String s = new String(bo.toByteArray(), "UTF-8").trim();
            if (s.length() == 0) return new JSONObject();
            if (s.startsWith(CookieCipher.PREFIX)) {
                String plain = CookieCipher.decrypt(ctx, s.substring(CookieCipher.PREFIX.length()));
                if (plain == null || plain.length() == 0) return new JSONObject();
                return new JSONObject(plain);
            }
            return new JSONObject(s);
        } catch (Throwable t) {
            return new JSONObject();
        } finally {
            try { if (in != null) in.close(); } catch (Throwable ignore) { }
        }
    }

    private static void writeCacheFile(Context ctx, JSONObject all) {
        FileOutputStream out = null;
        File f = cacheFile(ctx);
        File tmp = new File(f.getParentFile(), CACHE_FILE + ".tmp");
        try {
            out = new FileOutputStream(tmp);
            String plain = all.toString();
            String enc = CookieCipher.encrypt(ctx, plain);
            out.write(((enc != null && enc.length() > 0)
                    ? (CookieCipher.PREFIX + enc) : plain).getBytes("UTF-8"));
            out.flush();
            out.close();
            out = null;
            if (f.exists() && !f.delete()) {
                
            }
            if (!tmp.renameTo(f)) {
                FileOutputStream o2 = new FileOutputStream(f);
                String plain2 = all.toString();
                String enc2 = CookieCipher.encrypt(ctx, plain2);
                o2.write(((enc2 != null && enc2.length() > 0)
                        ? (CookieCipher.PREFIX + enc2) : plain2).getBytes("UTF-8"));
                o2.close();
                tmp.delete();
            }
        } catch (Throwable t) {
            try { if (out != null) out.close(); } catch (Throwable ignore) { }
            try { tmp.delete(); } catch (Throwable ignore) { }
        }
    }

    private static String loadCache(String platform) {
        Context ctx = QQEnv.appContext();
        if (ctx == null) return null;
        try {
            JSONObject all = readCacheFile(ctx);
            JSONObject o = all.optJSONObject(platform);
            if (o == null) return null;
            long t = o.optLong("t", 0);
            if (t > 0 && System.currentTimeMillis() - t > CACHE_TTL) return null;
            String c = o.optString("c", "");
            return c.length() == 0 ? null : c;
        } catch (Throwable t) {
            return null;
        }
    }

    private static void saveCache(String platform, String cookie) {
        Context ctx = QQEnv.appContext();
        if (ctx == null || cookie == null || cookie.length() == 0) return;
        try {
            JSONObject all = readCacheFile(ctx);
            JSONObject o = new JSONObject();
            o.put("c", cookie);
            o.put("t", System.currentTimeMillis());
            all.put(platform, o);
            writeCacheFile(ctx, all);
        } catch (Throwable ignore) {
        }
    }
}
