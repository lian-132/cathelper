package com.cathelperawa.owo.app.bili;

import android.content.Context;

import com.cathelperawa.owo.app.cookie.CookieStore;
import com.cathelperawa.owo.app.core.HookLog;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public final class BiliApi {

    private BiliApi() {}

    public static final String UA =
            "Mozilla/5.0 (Linux; Android 13; SM-G991B) AppleWebKit/537.36 "
                    + "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36";
    public static final String REFERER = "https://www.bilibili.com/";

    public static final class Item {
        public String bvid = "";
        public long cid = 0L;
        public String title = "";
        public String cover = "";
        public String up = "";
        public long duration = 0L;      
        public long play = 0L;          
        public long danmaku = 0L;       
        public long pubdate = 0L;
        
        public long mid = 0L;
    }

    public static final class Detail {
        public Item base = new Item();
        
        public long aid = 0L;
        public String desc = "";
        public long like = 0L, coin = 0L, fav = 0L, share = 0L;
        public long followers = 0L;
        
        public String ownerFace = "";
        public List<Item> pages = new ArrayList<Item>();
    }

    public static final class Me {
        public boolean login = false;
        public String uname = "";
        public String face = "";
        public int level = 0;
        public int coin = 0;
        public int exp = 0, expNext = 0;
        public int following = 0, follower = 0, dynamic = 0;
    }

    
    public static List<Item> feed(Context ctx, int idx) {
        String url = "https://api.bilibili.com/x/web-interface/wbi/index/top/feed/rcmd"
                + "?ps=20&fresh_type=4&feed_version=V8&fresh_idx=" + idx
                + "&fresh_idx_1h=" + idx + "&brush=" + idx + "&homepage_ver=1";
        List<Item> out = new ArrayList<Item>();
        JSONObject root = get(ctx, BiliApiExt.sign(ctx, url));
        if (root == null) return out;
        if (root.optInt("code", -1) != 0) {
            HookLog.w("Bili.feed code=" + root.optInt("code") + " " + root.optString("message"));
            return out;
        }
        JSONArray arr = root.optJSONObject("data") == null ? null
                : root.optJSONObject("data").optJSONArray("item");
        if (arr == null) return out;
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o == null) continue;
            String goto_ = o.optString("goto", "");
            if (!"av".equals(goto_)) continue;   
            Item it = new Item();
            it.bvid = o.optString("bvid", "");
            it.cid = o.optLong("cid", 0);
            it.title = o.optString("title", "");
            it.cover = normalizePic(o.optString("pic", ""));
            it.duration = o.optLong("duration", 0);
            it.pubdate = o.optLong("pubdate", 0);
            JSONObject owner = o.optJSONObject("owner");
            if (owner != null) {
                it.up = owner.optString("name", "");
                it.mid = owner.optLong("mid", 0);
            }
            JSONObject stat = o.optJSONObject("stat");
            if (stat != null) {
                it.play = stat.optLong("view", stat.optLong("play", 0));
                it.danmaku = stat.optLong("danmaku", 0);
            }
            if (it.bvid.length() > 3) out.add(it);
        }
        return out;
    }

    public static List<Item> popular(Context ctx, int page) {
        String url = "https://api.bilibili.com/x/web-interface/popular?ps=20&pn=" + page;
        List<Item> out = new ArrayList<Item>();
        JSONObject root = get(ctx, url);
        if (root == null || root.optInt("code", -1) != 0) return out;
        JSONArray arr = root.optJSONObject("data") == null ? null
                : root.optJSONObject("data").optJSONArray("list");
        if (arr == null) return out;
        for (int i = 0; i < arr.length(); i++) {
            Item it = parseVideoItem(arr.optJSONObject(i));
            if (it != null) out.add(it);
        }
        return out;
    }

    public static List<Item> search(Context ctx, String keyword, int page) {
        List<Item> out = new ArrayList<Item>();
        try {
            String url = "https://api.bilibili.com/x/web-interface/wbi/search/type"
                    + "?search_type=video&page=" + page
                    + "&keyword=" + URLEncoder.encode(keyword, "UTF-8");
            JSONObject root = get(ctx, BiliApiExt.sign(ctx, url));
            if (root == null) return out;
            if (root.optInt("code", -1) != 0) {
                HookLog.w("Bili.search code=" + root.optInt("code"));
                return out;
            }
            JSONArray arr = root.optJSONObject("data") == null ? null
                    : root.optJSONObject("data").optJSONArray("result");
            if (arr == null) return out;
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o == null) continue;
                Item it = new Item();
                it.bvid = o.optString("bvid", "");
                it.title = stripEm(o.optString("title", ""));
                it.cover = normalizePic(o.optString("pic", ""));
                it.up = o.optString("author", "");
                it.mid = o.optLong("mid", 0);
                it.duration = parseClock(o.optString("duration", "0:0"));
                it.play = num(o.optString("play", "0"));
                it.danmaku = num(o.optString("video_review", "0"));
                if (it.bvid.length() > 3) out.add(it);
            }
        } catch (Throwable t) {
            HookLog.e("Bili.search", t);
        }
        return out;
    }

    
    public static Detail detail(Context ctx, String bvid) {
        Detail d = new Detail();
        JSONObject root = get(ctx, BiliApiExt.sign(ctx,
                "https://api.bilibili.com/x/web-interface/view?bvid=" + bvid));
        if (root == null || root.optInt("code", -1) != 0) return d;
        JSONObject o = root.optJSONObject("data");
        if (o == null) return d;
        d.aid = o.optLong("aid", 0);
        d.base.bvid = o.optString("bvid", bvid);
        d.base.cid = o.optLong("cid", 0);
        d.base.title = o.optString("title", "");
        d.base.cover = normalizePic(o.optString("pic", ""));
        d.base.duration = o.optLong("duration", 0);
        d.base.pubdate = o.optLong("pubdate", 0);
        d.desc = o.optString("desc", "");
        JSONObject owner = o.optJSONObject("owner");
        if (owner != null) {
            d.base.up = owner.optString("name", "");
            d.base.mid = owner.optLong("mid", 0);
            d.ownerFace = normalizePic(owner.optString("face", ""));
        }
        JSONObject stat = o.optJSONObject("stat");
        if (stat != null) {
            d.base.play = stat.optLong("view", 0);
            d.base.danmaku = stat.optLong("danmaku", 0);
            d.like = stat.optLong("like", 0);
            d.coin = stat.optLong("coin", 0);
            d.fav = stat.optLong("favorite", 0);
            d.share = stat.optLong("share", 0);
        }
        JSONArray pages = o.optJSONArray("pages");
        if (pages != null) {
            for (int i = 0; i < pages.length(); i++) {
                JSONObject p = pages.optJSONObject(i);
                if (p == null) continue;
                Item pi = new Item();
                pi.cid = p.optLong("cid", 0);
                pi.title = p.optString("part", "");
                pi.duration = p.optLong("duration", 0);
                d.pages.add(pi);
            }
        }
        return d;
    }

    public static List<Item> related(Context ctx, String bvid) {
        List<Item> out = new ArrayList<Item>();
        JSONObject root = get(ctx, "https://api.bilibili.com/x/web-interface/archive/related?bvid=" + bvid);
        if (root == null || root.optInt("code", -1) != 0) return out;
        JSONArray arr = root.optJSONArray("data");
        if (arr == null) return out;
        for (int i = 0; i < arr.length() && i < 12; i++) {
            Item it = parseVideoItem(arr.optJSONObject(i));
            if (it != null) out.add(it);
        }
        return out;
    }

    public static String playUrl(Context ctx, String bvid, long cid, int qn) {
        String url = "https://api.bilibili.com/x/player/playurl?bvid=" + bvid
                + "&cid=" + cid + "&qn=" + qn + "&fnval=1&fnver=0&fourk=0"
                + "&platform=html5&high_quality=1";
        JSONObject root = get(ctx, BiliApiExt.sign(ctx, url));
        if (root == null) return null;
        if (root.optInt("code", -1) != 0) {
            HookLog.w("Bili.playUrl code=" + root.optInt("code") + " " + root.optString("message"));
            return null;
        }
        JSONObject data = root.optJSONObject("data");
        if (data == null) return null;
        JSONArray durl = data.optJSONArray("durl");
        if (durl == null || durl.length() == 0) return null;
        JSONObject u0 = durl.optJSONObject(0);
        if (u0 == null) return null;
        String primary = u0.optString("url", "");
        if (primary.length() > 0) return primary;
        JSONArray backup = u0.optJSONArray("backup_url");
        if (backup != null && backup.length() > 0) return backup.optString(0, null);
        return null;
    }

    public static String dashUrl(Context ctx, String bvid, long cid) {
        String url = "https://api.bilibili.com/x/player/playurl?bvid=" + bvid
                + "&cid=" + cid + "&qn=64&fnval=16&fnver=0&fourk=0";
        JSONObject root = get(ctx, url);
        if (root == null || root.optInt("code", -1) != 0) return null;
        JSONObject dash = root.optJSONObject("data") == null ? null
                : root.optJSONObject("data").optJSONObject("dash");
        if (dash == null) return null;
        JSONArray vids = dash.optJSONArray("video");
        if (vids == null || vids.length() == 0) return null;
        JSONObject v0 = vids.optJSONObject(0);
        return v0 == null ? null : v0.optString("baseUrl", v0.optString("base_url", ""));
    }

    
    public static volatile Me cachedMe;

    public static Me me(Context ctx) {
        Me m = new Me();
        JSONObject root = get(ctx, "https://api.bilibili.com/x/web-interface/nav");
        if (root == null) return m;
        JSONObject d = root.optJSONObject("data");
        if (d == null) return m;
        m.login = d.optBoolean("isLogin", false);
        m.uname = d.optString("uname", "");
        m.face = normalizePic(d.optString("face", ""));
        m.coin = d.optInt("money", 0);
        JSONObject lv = d.optJSONObject("level_info");
        if (lv != null) {
            m.level = lv.optInt("current_level", 0);
            m.exp = lv.optInt("current_exp", 0);
            m.expNext = lv.optInt("next_exp", 0);
        }
        m.following = d.optInt("following", 0);
        m.follower = d.optInt("follower", 0);
        m.dynamic = d.optInt("dynamic", 0);
        if (m.login) cachedMe = m;
        return m;
    }

    
    public static JSONObject get(Context ctx, String url) {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setConnectTimeout(12000);
            conn.setReadTimeout(15000);
            conn.setRequestProperty("User-Agent", UA);
            conn.setRequestProperty("Referer", REFERER);
            conn.setRequestProperty("Accept", "application/json, text/plain, */*");
            String ck = cookie(ctx);
            if (ck != null && ck.length() > 0) {
                conn.setRequestProperty("Cookie", ck);
            }
            int code = conn.getResponseCode();
            InputStream in = code >= 400 ? conn.getErrorStream() : conn.getInputStream();
            if (in == null) return null;
            String body = read(in, 4 * 1024 * 1024);
            if (body == null || body.length() == 0) return null;
            return new JSONObject(body);
        } catch (Throwable t) {
            HookLog.w("Bili.get 失败 " + t.getClass().getSimpleName() + ": " + t.getMessage());
            return null;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    public static String cookie(Context ctx) {
        if (ctx == null) return "";
        try {
            CookieStore.Entry e = CookieStore.get(ctx, "bilibili");
            return e == null ? "" : e.cookie;
        } catch (Throwable t) {
            return "";
        }
    }

    static String read(InputStream in, int max) {
        try {
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n, total = 0;
            while ((n = in.read(buf)) > 0) {
                total += n;
                if (total > max) break;
                bo.write(buf, 0, n);
            }
            return new String(bo.toByteArray(), "UTF-8");
        } catch (Throwable t) {
            return null;
        } finally {
            try { in.close(); } catch (Throwable ignore) { }
        }
    }

    
    public static Item itemFrom(JSONObject o) {
        return parseVideoItem(o);
    }

    private static Item parseVideoItem(JSONObject o) {
        if (o == null) return null;
        Item it = new Item();
        it.bvid = o.optString("bvid", "");
        it.cid = o.optLong("cid", 0);
        it.title = o.optString("title", "");
        it.cover = normalizePic(o.optString("pic", ""));
        it.duration = o.optLong("duration", 0);
        it.pubdate = o.optLong("pubdate", 0);
        JSONObject owner = o.optJSONObject("owner");
        if (owner != null) {
            it.up = owner.optString("name", "");
            it.mid = owner.optLong("mid", 0);
        }
        JSONObject stat = o.optJSONObject("stat");
        if (stat != null) {
            it.play = stat.optLong("view", 0);
            it.danmaku = stat.optLong("danmaku", 0);
        }
        return it.bvid.length() > 3 ? it : null;
    }

    static String normalizePic(String s) {
        if (s == null) return "";
        if (s.startsWith("//")) return "https:" + s;
        if (s.startsWith("http://")) return "https://" + s.substring(7);
        return s;
    }

    private static String stripEm(String s) {
        return s == null ? "" : s.replaceAll("</?em[^>]*>", "");
    }

    private static long num(String s) {
        if (s == null || s.length() == 0) return 0;
        try {
            if (s.endsWith("万")) return (long) (Float.parseFloat(s.replace("万", "")) * 10000);
            if (s.endsWith("亿")) return (long) (Float.parseFloat(s.replace("亿", "")) * 100000000);
            return Long.parseLong(s.replaceAll("[^0-9]", ""));
        } catch (Throwable t) {
            return 0;
        }
    }

    private static long parseClock(String s) {
        if (s == null) return 0;
        try {
            String[] parts = s.split(":");
            long v = 0;
            for (String p : parts) v = v * 60 + Long.parseLong(p.trim());
            return v;
        } catch (Throwable t) {
            return 0;
        }
    }

    public static String clock(long sec) {
        if (sec <= 0) return "";
        long h = sec / 3600, m = (sec % 3600) / 60, s = sec % 60;
        if (h > 0) return String.format("%d:%02d:%02d", h, m, s);
        return String.format("%02d:%02d", m, s);
    }

    public static String count(long n) {
        if (n >= 100000000L) return String.format("%.1f亿", n / 100000000.0);
        if (n >= 10000L) return String.format("%.1f万", n / 10000.0);
        return String.valueOf(n);
    }
}
