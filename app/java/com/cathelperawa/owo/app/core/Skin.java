package com.cathelperawa.owo.app.core;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public final class Skin {

    private static final Object LOCK = new Object();
    private static final String FILE = "skin.json";

    private static JSONObject sData;

    private static final Map<Integer, Integer> sColorCache = new HashMap<Integer, Integer>();
    private static final Map<String, Bitmap> sIconCache = new HashMap<String, Bitmap>();

    private Skin() {}

    private static File file() {
        try {
            File d = DataDir.global();
            if (d == null) return null;
            if (!d.exists()) d.mkdirs();
            return new File(d, FILE);
        } catch (Throwable t) {
            return null;
        }
    }

    private static JSONObject data() {
        if (sData != null) return sData;
        synchronized (LOCK) {
            if (sData != null) return sData;
            JSONObject o = new JSONObject();
            try {
                File f = file();
                if (f != null && f.exists()) {
                    String s = read(f);
                    if (s != null && s.length() > 0) o = new JSONObject(s);
                }
            } catch (Throwable t) {
                HookLog.w("皮肤配置读取失败 " + t);
            }
            sData = o;
            return sData;
        }
    }

    private static void save() {
        synchronized (LOCK) {
            sColorCache.clear();
            try {
                File f = file();
                if (f == null) return;
                File tmp = new File(f.getAbsolutePath() + ".tmp");
                FileOutputStream fo = new FileOutputStream(tmp);
                fo.write(sData.toString().getBytes("UTF-8"));
                fo.flush();
                fo.close();
                if (!tmp.renameTo(f)) {

                    FileOutputStream d = new FileOutputStream(f);
                    d.write(sData.toString().getBytes("UTF-8"));
                    d.flush();
                    d.close();
                }
            } catch (Throwable t) {
                HookLog.w("皮肤配置保存失败 " + t);
            }
        }
    }

    private static String read(File f) {
        RandomAccessFile r = null;
        try {
            r = new RandomAccessFile(f, "r");
            byte[] b = new byte[(int) r.length()];
            r.readFully(b);
            return new String(b, "UTF-8");
        } catch (Throwable t) {
            return null;
        } finally {
            try { if (r != null) r.close(); } catch (Throwable ignore) {}
        }
    }

    public static boolean active() {
        JSONObject d = data();
        return d.optJSONObject("colors") != null
                || d.optJSONObject("icons") != null
                || d.optJSONObject("bgs") != null
                || d.optJSONArray("hidden") != null
                || d.optJSONObject("texts") != null
                || d.optJSONObject("styles") != null;
    }

    public static int color(int c) {
        if (!active()) return c;
        Integer hit = sColorCache.get(c);
        if (hit != null) return hit < 0 ? c : hit;
        int out = c;
        try {
            JSONObject m = data().optJSONObject("colors");
            if (m != null) {
                String key = hex(c);
                String v = m.optString(key, "");
                if (v.length() > 0) {
                    Integer parsed = parseColor(v);
                    if (parsed != null) out = parsed;
                }
                sColorCache.put(c, out);
            } else {
                sColorCache.put(c, -1);
            }
        } catch (Throwable ignore) {
        }
        return out;
    }

    public static String bg(String name) {
        try {
            JSONObject m = data().optJSONObject("bgs");
            if (m == null) return null;
            String v = m.optString(name, "");
            return v.length() == 0 ? null : v;
        } catch (Throwable t) {
            return null;
        }
    }

    public static boolean hidden(String key) {
        try {
            JSONArray a = data().optJSONArray("hidden");
            if (a == null) return false;
            for (int i = 0; i < a.length(); i++) {
                if (key.equals(a.optString(i))) return true;
            }
        } catch (Throwable ignore) {
        }
        return false;
    }

    public static String text(String key, String def) {
        try {
            JSONObject m = data().optJSONObject("texts");
            if (m == null) return def;
            String v = m.optString(key, "");
            return v.length() == 0 ? def : v;
        } catch (Throwable t) {
            return def;
        }
    }

    public static JSONObject style(String key) {
        try {
            JSONObject m = data().optJSONObject("styles");
            if (m == null) return new JSONObject();
            JSONObject o = m.optJSONObject(key);
            return o == null ? new JSONObject() : o;
        } catch (Throwable t) {
            return new JSONObject();
        }
    }

    public static void setStyle(String owner, String key, String json) {
        try {
            JSONObject d = data();
            JSONObject m = d.optJSONObject("styles");
            if (m == null) m = new JSONObject();
            m.put(key, new JSONObject(json));
            d.put("styles", m);
            ownerOf().put("styles|" + key, owner);
            save();
        } catch (Throwable t) {
            HookLog.w("样式写入失败 " + t);
        }
    }

    public static Drawable icon(Context ctx, String key) {
        try {
            JSONObject m = data().optJSONObject("icons");
            if (m == null) return null;
            String v = m.optString(key, "");
            if (v.length() == 0) return null;
            return decode(ctx, v, 48);
        } catch (Throwable t) {
            return null;
        }
    }

    public static void setColor(String owner, String from, String to) {
        put(owner, "colors", from.toUpperCase(), to);
    }

    public static void setBg(String owner, String name, String value) {
        put(owner, "bgs", name, value);
    }

    public static void setIcon(String owner, String key, String value) {
        synchronized (LOCK) {
            sIconCache.remove(key);
        }
        put(owner, "icons", key, value);
    }

    public static void setText(String owner, String key, String value) {
        put(owner, "texts", key, value);
    }

    public static void hide(String owner, String key) {
        synchronized (LOCK) {
            try {
                JSONObject d = data();
                JSONArray a = d.optJSONArray("hidden");
                if (a == null) a = new JSONArray();
                boolean exists = false;
                for (int i = 0; i < a.length(); i++) {
                    if (key.equals(a.optString(i))) exists = true;
                }
                if (!exists) a.put(key);
                d.put("hidden", a);
                ownerOf().put("hidden|" + key, owner);
                save();
            } catch (Throwable t) {
                HookLog.w("隐藏入口失败 " + t);
            }
        }
    }

    public static int removeByScript(String owner) {
        if (owner == null || owner.length() == 0) return 0;
        synchronized (LOCK) {
            int n = 0;
            try {
                JSONObject d = data();
                JSONObject own = ownerOf();
                List<String> keys = new ArrayList<String>();
                Iterator<String> it = own.keys();
                while (it.hasNext()) keys.add(it.next());
                for (int i = 0; i < keys.size(); i++) {
                    String k = keys.get(i);
                    if (!owner.equals(own.optString(k))) continue;
                    int bar = k.indexOf('|');
                    if (bar <= 0) continue;
                    String group = k.substring(0, bar);
                    String id = k.substring(bar + 1);
                    if ("hidden".equals(group)) {
                        JSONArray a = d.optJSONArray("hidden");
                        if (a != null) {
                            JSONArray b = new JSONArray();
                            for (int j = 0; j < a.length(); j++) {
                                if (!id.equals(a.optString(j))) b.put(a.optString(j));
                            }
                            d.put("hidden", b);
                        }
                    } else {
                        JSONObject m = d.optJSONObject(group);
                        if (m != null) m.remove(id);
                    }
                    own.remove(k);
                    n++;
                }
                sIconCache.clear();
                if (n > 0) {
                    save();
                    HookLog.i("皮肤：已撤回脚本[" + owner + "]的 " + n + " 项覆盖");
                }
            } catch (Throwable t) {
                HookLog.w("撤回皮肤覆盖失败 " + t);
            }
            return n;
        }
    }

    public static int prune(java.util.Collection<String> alive) {
        synchronized (LOCK) {
            int n = 0;
            try {
                JSONObject d = data();
                JSONObject own = ownerOf();
                List<String> keys = new ArrayList<String>();
                Iterator<String> it = own.keys();
                while (it.hasNext()) keys.add(it.next());
                for (int i = 0; i < keys.size(); i++) {
                    String k = keys.get(i);
                    String o = own.optString(k);
                    if (o == null || o.length() == 0) continue;
                    if (alive != null && alive.contains(o)) continue;
                    int bar = k.indexOf('|');
                    if (bar <= 0) continue;
                    String group = k.substring(0, bar);
                    String id = k.substring(bar + 1);
                    if ("hidden".equals(group)) {
                        JSONArray a = d.optJSONArray("hidden");
                        if (a != null) {
                            JSONArray b = new JSONArray();
                            for (int j = 0; j < a.length(); j++) {
                                if (!id.equals(a.optString(j))) b.put(a.optString(j));
                            }
                            d.put("hidden", b);
                        }
                    } else {
                        JSONObject m = d.optJSONObject(group);
                        if (m != null) m.remove(id);
                    }
                    own.remove(k);
                    n++;
                }
                if (n > 0) {
                    sIconCache.clear();
                    save();
                    HookLog.i("皮肤：清理了 " + n + " 项来自已停用脚本的覆盖");
                }
            } catch (Throwable t) {
                HookLog.w("清理皮肤覆盖失败 " + t);
            }
            return n;
        }
    }

    public static void clear() {
        synchronized (LOCK) {
            sData = new JSONObject();
            sIconCache.clear();
            save();
        }
    }

    public static JSONObject dump() {
        return data();
    }

    private static void put(String owner, String group, String key, String value) {
        synchronized (LOCK) {
            try {
                JSONObject d = data();
                JSONObject m = d.optJSONObject(group);
                if (m == null) m = new JSONObject();
                m.put(key, value);
                d.put(group, m);
                ownerOf().put(group + "|" + key, owner);
                save();
            } catch (Throwable t) {
                HookLog.w("皮肤写入失败 " + t);
            }
        }
    }

    private static JSONObject ownerOf() {
        JSONObject d = data();
        JSONObject o = d.optJSONObject("owner");
        if (o == null) {
            o = new JSONObject();
            try { d.put("owner", o); } catch (Throwable ignore) {}
        }
        return o;
    }

    private static String hex(int c) {
        return String.format("#%06X", 0xFFFFFF & c);
    }

    private static Integer parseColor(String v) {
        try {
            return Color.parseColor(v.startsWith("#") ? v : "#" + v);
        } catch (Throwable t) {
            return null;
        }
    }

    private static Drawable decode(Context ctx, String v, int sizeDp) {
        try {
            if (v.startsWith("emoji:")) {
                Bitmap bm = sIconCache.get(v);
                if (bm == null) {
                    int px = (int) (sizeDp * ctx.getResources().getDisplayMetrics().density);
                    bm = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888);
                    Canvas cv = new Canvas(bm);
                    Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
                    p.setTextSize(px * 0.8f);
                    p.setTextAlign(Paint.Align.CENTER);
                    p.setTypeface(Typeface.DEFAULT);
                    String e = v.substring(6);
                    cv.drawText(e, px / 2f, px / 2f - (p.descent() + p.ascent()) / 2f, p);
                    sIconCache.put(v, bm);
                }
                return new BitmapDrawable(ctx.getResources(), bm);
            }
            if (v.startsWith("#")) {
                Integer c = parseColor(v);
                if (c == null) return null;
                GradientDrawable g = new GradientDrawable();
                g.setShape(GradientDrawable.OVAL);
                g.setColor(c);
                return g;
            }
            Bitmap bm = sIconCache.get(v);
            if (bm == null) {
                bm = BitmapFactory.decodeFile(v);
                if (bm == null) return null;
                sIconCache.put(v, bm);
            }
            return new BitmapDrawable(ctx.getResources(), bm);
        } catch (Throwable t) {
            return null;
        }
    }
}
