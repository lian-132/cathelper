package com.cathelperawa.owo.app.cookie;

import android.content.Context;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class CookieStore {

    private CookieStore() {}

    private static final String FILE = "cookies.json";
    private static final Object LOCK = new Object();

    public static final class Entry {
        public String platform = "";
        public String cookie = "";
        public String label = "";
        public long savedAt = 0L;

        public boolean valid() {
            return cookie != null && cookie.length() > 0;
        }

        public String masked() {
            if (!valid()) return "（空）";
            String head = cookie.length() <= 4 ? "" : cookie.substring(0, 4);
            return cookie.length() + " 字符 · " + head + "…";
        }
    }

    private static File fileOf(Context c) {
        return new File(c.getFilesDir(), FILE);
    }

    public static Entry get(Context c, String platform) {
        if (c == null || platform == null || platform.length() == 0) return null;
        synchronized (LOCK) {
            JSONObject all = readAll(c);
            JSONObject o = all == null ? null : all.optJSONObject(platform);
            if (o == null) return null;
            Entry e = new Entry();
            e.platform = platform;
            e.cookie = o.optString("c", "");
            e.label = o.optString("l", "");
            e.savedAt = o.optLong("t", 0L);
            return e.valid() ? e : null;
        }
    }

    public static List<Entry> all(Context c) {
        List<Entry> out = new ArrayList<Entry>();
        synchronized (LOCK) {
            JSONObject all = readAll(c);
            if (all == null) return out;
            Iterator<String> it = all.keys();
            while (it.hasNext()) {
                String k = it.next();
                JSONObject o = all.optJSONObject(k);
                if (o == null) continue;
                Entry e = new Entry();
                e.platform = k;
                e.cookie = o.optString("c", "");
                e.label = o.optString("l", "");
                e.savedAt = o.optLong("t", 0L);
                if (e.valid()) out.add(e);
            }
        }
        return out;
    }

    public static boolean save(Context c, String platform, String cookie, String label) {
        if (c == null || platform == null || platform.length() == 0) return false;
        if (cookie == null || cookie.trim().length() == 0) return remove(c, platform);
        synchronized (LOCK) {
            try {
                JSONObject all = readAll(c);
                if (all == null) all = new JSONObject();
                JSONObject o = new JSONObject();
                o.put("c", cookie.trim());
                o.put("t", System.currentTimeMillis());
                o.put("l", label == null ? "" : label);
                all.put(platform, o);
                return writeAll(c, all);
            } catch (Throwable t) {
                return false;
            }
        }
    }

    public static boolean remove(Context c, String platform) {
        if (c == null || platform == null) return false;
        synchronized (LOCK) {
            try {
                JSONObject all = readAll(c);
                if (all == null) return true;
                all.remove(platform);
                return writeAll(c, all);
            } catch (Throwable t) {
                return false;
            }
        }
    }

    private static JSONObject readAll(Context c) {
        File f = fileOf(c);
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
                String plain = CookieCipher.decrypt(c, s.substring(CookieCipher.PREFIX.length()));
                if (plain == null || plain.length() == 0) {

                    return new JSONObject();
                }
                return new JSONObject(plain);
            }

            JSONObject legacy = new JSONObject(s);
            try {
                writeAll(c, legacy);
            } catch (Throwable ignore) {
            }
            return legacy;
        } catch (Throwable t) {

            return new JSONObject();
        } finally {
            try { if (in != null) in.close(); } catch (Throwable ignore) { }
        }
    }

    private static boolean writeAll(Context c, JSONObject all) {
        File f = fileOf(c);
        File tmp = new File(f.getParentFile(), FILE + ".tmp");
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(tmp);

            String plain = all.toString();
            String enc = CookieCipher.encrypt(c, plain);
            String payload = (enc != null && enc.length() > 0)
                    ? (CookieCipher.PREFIX + enc) : plain;
            out.write(payload.getBytes("UTF-8"));
            out.flush();
            out.close();
            out = null;
            if (f.exists() && !f.delete()) {

            }
            boolean ok = tmp.renameTo(f);
            if (!ok) {

                FileOutputStream o2 = new FileOutputStream(f);
                String p2 = all.toString();
                String e2 = CookieCipher.encrypt(c, p2);
                o2.write(((e2 != null && e2.length() > 0)
                        ? (CookieCipher.PREFIX + e2) : p2).getBytes("UTF-8"));
                o2.close();
                tmp.delete();
            }
            try { f.setReadable(false, false); f.setReadable(true, true); } catch (Throwable ignore) { }
            return true;
        } catch (Throwable t) {
            try { if (out != null) out.close(); } catch (Throwable ignore) { }
            tmp.delete();
            return false;
        }
    }
}
