package com.cathelperawa.owo.app.bili;

import android.content.Context;

import com.cathelperawa.owo.app.cookie.CookieStore;
import com.cathelperawa.owo.app.core.HookLog;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

public final class BiliApiExt {

    private BiliApiExt() {}

    private static final int[] MIXIN_KEY_ENC_TAB = {
            46, 47, 18, 2, 53, 8, 23, 32, 15, 50, 10, 31, 58, 3, 45, 35, 27, 43, 5, 49,
            33, 9, 42, 19, 29, 28, 14, 39, 12, 38, 41, 13
    };

    private static volatile String mixinKey = null;
    private static volatile int mixinDay = -1;
    private static volatile boolean buvidTried = false;

    private static int dayKey() {
        Calendar c = Calendar.getInstance();
        return c.get(Calendar.YEAR) * 1000 + c.get(Calendar.DAY_OF_YEAR);
    }

    public static String mixinKey(Context ctx) {
        int day = dayKey();
        String cached = mixinKey;
        if (cached != null && mixinDay == day) return cached;
        synchronized (BiliApiExt.class) {
            if (mixinKey != null && mixinDay == day) return mixinKey;
            try {
                JSONObject root = BiliApi.get(ctx, "https://api.bilibili.com/x/web-interface/nav");
                if (root == null) return "";
                JSONObject data = root.optJSONObject("data");
                JSONObject img = data == null ? null : data.optJSONObject("wbi_img");
                if (img == null) return "";
                String a = fileName(img.optString("img_url", ""));
                String b = fileName(img.optString("sub_url", ""));
                String raw = a + b;
                if (raw.length() < 32) return "";
                StringBuilder sb = new StringBuilder();
                for (int idx : MIXIN_KEY_ENC_TAB) {
                    if (idx < raw.length()) sb.append(raw.charAt(idx));
                }
                mixinKey = sb.toString();
                mixinDay = day;
                return mixinKey;
            } catch (Throwable t) {
                HookLog.w("bili.mixinKey " + t);
                return "";
            }
        }
    }

    private static String fileName(String url) {
        if (url == null) return "";
        int slash = url.lastIndexOf('/');
        String name = slash >= 0 ? url.substring(slash + 1) : url;
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }

    public static String sign(Context ctx, String url) {
        String key = mixinKey(ctx);
        if (key == null || key.length() == 0) return url;
        try {
            int q = url.indexOf('?');
            String base = q < 0 ? url : url.substring(0, q);
            String query = q < 0 ? "" : url.substring(q + 1);

            LinkedHashMap<String, String> params = new LinkedHashMap<String, String>();
            if (query.length() > 0) {
                for (String kv : query.split("&")) {
                    int eq = kv.indexOf('=');
                    if (eq > 0) params.put(kv.substring(0, eq), kv.substring(eq + 1));
                }
            }
            params.put("wts", String.valueOf(System.currentTimeMillis() / 1000));

            List<String> keys = new ArrayList<String>(params.keySet());
            Collections.sort(keys);
            StringBuilder sb = new StringBuilder();
            for (String k : keys) {
                String v = params.get(k);
                if (v == null) v = "";
                v = v.replaceAll("[!'()*]", "");
                if (sb.length() > 0) sb.append('&');
                sb.append(enc(k)).append('=').append(enc(v));
            }
            String wRid = md5(sb.toString() + key);
            return base + "?" + sb + "&w_rid=" + wRid;
        } catch (Throwable t) {
            return url;
        }
    }

    private static String enc(String s) {
        try {
            return URLEncoder.encode(s == null ? "" : s, "UTF-8").replace("+", "%20");
        } catch (UnsupportedEncodingException e) {
            return s;
        }
    }

    private static String md5(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] b = md.digest(s.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte x : b) sb.append(String.format("%02x", x));
            return sb.toString();
        } catch (Throwable t) {
            return "";
        }
    }

    
    public static void ensureBuvid(Context ctx) {
        if (buvidTried) return;
        buvidTried = true;
        try {
            String ck = BiliApi.cookie(ctx);
            if (ck != null && ck.contains("buvid3=")) return;
            JSONObject root = BiliApi.get(ctx, "https://api.bilibili.com/x/frontend/finger/spi");
            if (root == null) return;
            JSONObject d = root.optJSONObject("data");
            if (d == null) return;
            String b3 = d.optString("b_3", d.optString("buvid3", ""));
            String b4 = d.optString("b_4", d.optString("buvid4", ""));
            if (b3.length() == 0) return;
            String merged = (ck == null ? "" : ck);
            if (merged.length() > 0 && !merged.endsWith(";")) merged += "; ";
            merged += "buvid3=" + b3 + "; buvid4=" + b4;
            CookieStore.Entry e = CookieStore.get(ctx, "bilibili");
            CookieStore.save(ctx, "bilibili", merged, e == null ? "" : e.label);
            HookLog.i("bili.buvid 已补齐 " + b3.substring(0, Math.min(8, b3.length())) + "…");
        } catch (Throwable t) {
            HookLog.w("bili.buvid " + t);
        }
    }

    public static final class Reply {
        public String uname = "";
        public String avatar = "";
        public String content = "";
        public long like = 0L;
        public long ctime = 0L;
        public int rcount = 0;
        public List<Reply> children = new ArrayList<Reply>();
    }

    public static List<Reply> replies(Context ctx, long aid, int page, int sort) {
        List<Reply> out = new ArrayList<Reply>();
        if (aid <= 0) return out;
        String url = "https://api.bilibili.com/x/v2/reply?type=1&oid=" + aid
                + "&pn=" + page + "&ps=20&sort=" + sort;
        JSONObject root = BiliApi.get(ctx, sign(ctx, url));
        if (root == null) return out;
        if (root.optInt("code", -1) != 0) {
            HookLog.w("bili.replies code=" + root.optInt("code"));
            return out;
        }
        JSONObject data = root.optJSONObject("data");
        if (data == null) return out;
        JSONArray arr = data.optJSONArray("replies");
        if (arr == null) return out;
        for (int i = 0; i < arr.length(); i++) {
            Reply r = parseReply(arr.optJSONObject(i));
            if (r != null) out.add(r);
        }
        return out;
    }

    public static int replyCount(Context ctx, long aid) {
        String url = "https://api.bilibili.com/x/v2/reply?type=1&oid=" + aid + "&pn=1&ps=1&sort=2";
        JSONObject root = BiliApi.get(ctx, sign(ctx, url));
        if (root == null) return 0;
        JSONObject page = root.optJSONObject("data") == null ? null
                : root.optJSONObject("data").optJSONObject("page");
        return page == null ? 0 : page.optInt("count", 0);
    }

    private static Reply parseReply(JSONObject o) {
        if (o == null) return null;
        Reply r = new Reply();
        JSONObject member = o.optJSONObject("member");
        if (member != null) {
            r.uname = member.optString("uname", "");
            r.avatar = BiliApi.normalizePic(member.optString("avatar", ""));
        }
        JSONObject content = o.optJSONObject("content");
        r.content = content == null ? "" : content.optString("message", "");
        r.like = o.optLong("like", 0);
        r.ctime = o.optLong("ctime", 0);
        r.rcount = o.optInt("rcount", 0);
        JSONArray children = o.optJSONArray("replies");
        if (children != null) {
            for (int i = 0; i < children.length() && i < 3; i++) {
                Reply c = parseReply(children.optJSONObject(i));
                if (c != null) r.children.add(c);
            }
        }
        return r;
    }

    public static String ago(long ctime) {
        if (ctime <= 0) return "";
        long diff = System.currentTimeMillis() / 1000 - ctime;
        if (diff < 60) return "刚刚";
        if (diff < 3600) return (diff / 60) + "分钟前";
        if (diff < 86400) return (diff / 3600) + "小时前";
        if (diff < 86400 * 30) return (diff / 86400) + "天前";
        java.text.SimpleDateFormat f = new java.text.SimpleDateFormat("MM-dd", java.util.Locale.CHINA);
        return f.format(new java.util.Date(ctime * 1000));
    }

    
    public static String online(Context ctx, long aid, long cid) {
        if (aid <= 0 || cid <= 0) return "";
        String url = "https://api.bilibili.com/x/player/online/total?aid=" + aid
                + "&cid=" + cid + "&bvid=";
        JSONObject root = BiliApi.get(ctx, sign(ctx, url));
        if (root == null || root.optInt("code", -1) != 0) return "";
        JSONObject d = root.optJSONObject("data");
        if (d == null) return "";
        String total = d.optString("total", "");
        return total.length() > 0 ? total + " 人在看" : "";
    }

    
    public static List<BiliApi.Item> ranking(Context ctx) {
        List<BiliApi.Item> out = new ArrayList<BiliApi.Item>();
        JSONObject root = BiliApi.get(ctx,
                "https://api.bilibili.com/x/web-interface/ranking/v2?rid=0&type=all");
        if (root == null || root.optInt("code", -1) != 0) return out;
        JSONArray arr = root.optJSONObject("data") == null ? null
                : root.optJSONObject("data").optJSONArray("list");
        if (arr == null) return out;
        for (int i = 0; i < arr.length(); i++) {
            BiliApi.Item it = BiliApi.itemFrom(arr.optJSONObject(i));
            if (it != null) out.add(it);
        }
        return out;
    }

    public static List<BiliApi.Item> weekly(Context ctx) {
        List<BiliApi.Item> out = new ArrayList<BiliApi.Item>();
        try {
            JSONObject list = BiliApi.get(ctx,
                    "https://api.bilibili.com/x/web-interface/popular/series/list");
            int number = 0;
            if (list != null && list.optInt("code", -1) == 0) {
                JSONArray arr = list.optJSONObject("data") == null ? null
                        : list.optJSONObject("data").optJSONArray("list");
                if (arr != null && arr.length() > 0) {
                    number = arr.optJSONObject(0).optInt("number", 0);
                }
            }
            if (number <= 0) return out;
            JSONObject one = BiliApi.get(ctx,
                    "https://api.bilibili.com/x/web-interface/popular/series/one?number=" + number);
            if (one == null || one.optInt("code", -1) != 0) return out;
            JSONArray arr = one.optJSONObject("data") == null ? null
                    : one.optJSONObject("data").optJSONArray("list");
            if (arr == null) return out;
            for (int i = 0; i < arr.length(); i++) {
                BiliApi.Item it = BiliApi.itemFrom(arr.optJSONObject(i));
                if (it != null) out.add(it);
            }
        } catch (Throwable t) {
            HookLog.w("bili.weekly " + t);
        }
        return out;
    }

    public static List<String> suggest(Context ctx, String term) {
        List<String> out = new ArrayList<String>();
        if (term == null || term.length() == 0) return out;
        try {
            String url = "https://api.bilibili.com/x/web-interface/suggest?term=" + enc(term);
            JSONObject root = BiliApi.get(ctx, url);
            if (root == null) return out;
            JSONObject data = root.optJSONObject("data");
            if (data == null) return out;
            JSONArray tags = data.optJSONArray("tag");
            if (tags != null) {
                for (int i = 0; i < tags.length() && out.size() < 8; i++) {
                    JSONObject t = tags.optJSONObject(i);
                    String v = t == null ? "" : t.optString("name", t.optString("value", ""));
                    if (v.length() > 0) out.add(v);
                }
            }
            if (out.isEmpty()) {
                JSONArray value = data.optJSONArray("value");
                if (value != null) {
                    for (int i = 0; i < value.length() && out.size() < 8; i++) {
                        String v = value.optString(i, "");
                        if (v.length() > 0) out.add(v);
                    }
                }
            }
        } catch (Throwable t) {
            HookLog.w("bili.suggest " + t);
        }
        return out;
    }

    public static List<String> hotwords(Context ctx) {
        List<String> out = new ArrayList<String>();
        try {
            JSONObject root = BiliApi.get(ctx, "https://s.search.bilibili.com/main/hotword?limit=20");
            if (root == null) return out;
            JSONObject data = root.optJSONObject("data");
            JSONArray list = null;
            if (data != null) list = data.optJSONArray("list");
            if (list == null) list = root.optJSONArray("list");
            if (list == null) return out;
            for (int i = 0; i < list.length() && out.size() < 20; i++) {
                JSONObject o = list.optJSONObject(i);
                if (o == null) continue;
                String kw = o.optString("keyword", o.optString("show_name", ""));
                if (kw.length() > 0) out.add(kw);
            }
        } catch (Throwable t) {
            HookLog.w("bili.hotword " + t);
        }
        return out;
    }
}
