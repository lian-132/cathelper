package com.cathelperawa.owo.app.cookie;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public final class NeteaseApi {

    private NeteaseApi() {}

    private static final String BASE = "https://music.163.com";
    private static final String UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                    + "(KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36";

    public static final class Song {
        public String id = "";
        public String name = "";
        public String artist = "";
        public String cover = "";
        public long durationMs = 0;

        public String durText() {
            if (durationMs <= 0) return "";
            long s = durationMs / 1000;
            return (s / 60) + ":" + (s % 60 < 10 ? "0" : "") + (s % 60);
        }
    }

    public static final class Account {
        public long uid = 0;
        public String nickname = "";

        public int vipType = 0;
        public int level = 0;
        public String avatar = "";

        public String vipText() {
            return vipType > 0 ? "VIP" : "普通用户";
        }
    }

    public static Account account(Context ctx) {
        CookieStore.Entry e = CookieStore.get(ctx, "netease");
        if (e == null) return null;
        try {
            JSONObject root = getJson(ctx, BASE + "/api/nuser/account/get", null);
            if (root == null) return null;
            JSONObject acc = root.optJSONObject("account");
            if (acc == null) return null;
            Account a = new Account();
            a.uid = acc.optLong("id", 0);
            JSONObject prof = root.optJSONObject("profile");
            if (prof != null) {
                a.nickname = prof.optString("nickname", "");
                a.vipType = prof.optInt("vipType", 0);
                a.level = prof.optInt("level", 0);
                a.avatar = prof.optString("avatarUrl", "");
            }
            return a;
        } catch (Throwable t) {
            return null;
        }
    }

    public static List<Song> recommendNew(Context ctx, int limit) {
        List<Song> out = new ArrayList<Song>();
        try {
            JSONObject root = getJson(ctx,
                    BASE + "/api/personalized/newsong?limit=" + Math.max(1, Math.min(limit, 50)),
                    null);
            if (root == null) return out;
            JSONArray arr = root.optJSONArray("result");
            if (arr == null) return out;
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o == null) continue;
                JSONObject song = o.optJSONObject("song");
                if (song == null) song = o;
                Song s = parseSong(song);
                if (s.id.length() > 0) out.add(s);
            }
        } catch (Throwable ignore) {
        }
        return out;
    }

    public static List<Song> likedSongs(Context ctx, long uid, int limit) {
        List<Song> out = new ArrayList<Song>();
        if (uid <= 0) return out;
        try {
            JSONObject root = getJson(ctx,
                    BASE + "/api/song/like?uid=" + uid + "&limit="
                            + Math.max(1, Math.min(limit, 100)) + "&offset=0", null);
            if (root == null) return out;
            JSONArray arr = root.optJSONArray("songs");
            if (arr == null) return out;
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o == null) continue;
                Song s = parseSong(o);
                if (s.id.length() > 0) out.add(s);
            }
        } catch (Throwable ignore) {
        }
        return out;
    }

    public static String lyric(Context ctx, String id) {
        if (id == null || id.length() == 0) return "";
        try {
            JSONObject root = getJson(ctx,
                    BASE + "/api/song/lyric?id=" + id + "&lv=1&kv=1&tv=-1", null);
            if (root == null) return "";
            JSONObject lrc = root.optJSONObject("lrc");
            if (lrc == null) return "";
            String s = lrc.optString("lyric", "");
            return s == null ? "" : s;
        } catch (Throwable t) {
            return "";
        }
    }

    public static List<Song> similarSongs(Context ctx, String songId, int limit) {
        List<Song> out = new ArrayList<Song>();
        if (songId == null || songId.length() == 0) return out;
        try {
            JSONObject root = getJson(ctx,
                    BASE + "/api/v1/discovery/simi/song?songid=" + songId
                            + "&limit=" + Math.max(1, Math.min(limit, 50)), null);
            if (root == null) return out;
            JSONArray arr = root.optJSONArray("songs");
            if (arr == null) {
                JSONObject data = root.optJSONObject("data");
                arr = data == null ? null : data.optJSONArray("songs");
            }
            if (arr == null) return out;
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o == null) continue;
                Song s = parseSong(o);
                if (s.id.length() > 0) out.add(s);
            }
        } catch (Throwable ignore) {
        }
        return out;
    }

    public static List<Song> dailyRecommend(Context ctx) {
        List<Song> out = new ArrayList<Song>();
        try {
            JSONObject root = getJson(ctx, BASE + "/api/v3/discovery/recommend/songs", null);
            if (root == null) return out;
            JSONObject data = root.optJSONObject("data");
            JSONArray arr = data == null ? null : data.optJSONArray("dailySongs");
            if (arr == null) return out;
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o == null) continue;
                Song s = parseSong(o);
                if (s.id.length() > 0) out.add(s);
            }
        } catch (Throwable ignore) {
        }
        return out;
    }

    public static List<Song> radioGet(Context ctx) {
        List<Song> out = new ArrayList<Song>();
        try {
            JSONObject root = getJson(ctx, BASE + "/api/v1/radio/get?limit=20", null);
            if (root == null) return out;
            JSONArray arr = root.optJSONArray("data");
            if (arr == null) return out;
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o == null) continue;
                Song s = parseSong(o);
                if (s.id.length() > 0) out.add(s);
            }
        } catch (Throwable ignore) {
        }

        if (out.isEmpty()) out.addAll(recommendNew(ctx, 30));
        return out;
    }

    public static final class Playlist {
        public String id = "";
        public String name = "";
        public int trackCount = 0;

        public int specialType = 0;
    }

    public static List<Playlist> playlists(Context ctx, long uid) {
        List<Playlist> out = new ArrayList<Playlist>();
        if (uid <= 0) return out;
        try {
            JSONObject root = getJson(ctx,
                    BASE + "/api/user/playlist?uid=" + uid + "&limit=100&offset=0", null);
            if (root == null) return out;
            JSONArray arr = root.optJSONArray("playlist");
            if (arr == null) return out;
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o == null) continue;
                Playlist p = new Playlist();
                p.id = String.valueOf(o.optLong("id", 0));
                p.name = o.optString("name", "");
                p.trackCount = o.optInt("trackCount", 0);
                p.specialType = o.optInt("specialType", 0);
                if (p.id.length() > 0 && p.name.length() > 0) out.add(p);
            }
        } catch (Throwable ignore) {
        }
        return out;
    }

    public static List<Song> playlistTracks(Context ctx, String playlistId, int limit) {
        List<Song> out = new ArrayList<Song>();
        if (playlistId == null || playlistId.length() == 0) return out;
        try {
            JSONObject root = getJson(ctx,
                    BASE + "/api/v6/playlist/detail?id=" + playlistId + "&n=" + Math.max(1, limit), null);
            if (root == null) return out;
            JSONObject pl = root.optJSONObject("playlist");
            if (pl == null) return out;
            JSONArray arr = pl.optJSONArray("tracks");
            if (arr == null) return out;
            for (int i = 0; i < arr.length() && out.size() < limit; i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o == null) continue;
                Song s = parseSong(o);
                if (s.id.length() > 0) out.add(s);
            }
        } catch (Throwable ignore) {
        }
        return out;
    }

    public static List<Song> search(Context ctx, String keyword, int limit) {
        List<Song> out = new ArrayList<Song>();
        if (keyword == null || keyword.trim().length() == 0) return out;
        try {
            JSONObject root = getJson(ctx,
                    BASE + "/api/cloudsearch/pc?s=" + URLEncoder.encode(keyword.trim(), "UTF-8")
                            + "&type=1&limit=" + Math.max(1, Math.min(limit, 50)), null);
            if (root == null) return out;
            JSONObject result = root.optJSONObject("result");
            JSONArray arr = result == null ? null : result.optJSONArray("songs");
            if (arr == null) return out;
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o == null) continue;
                Song s = parseSong(o);
                if (s.id.length() > 0) out.add(s);
            }
        } catch (Throwable ignore) {
        }
        return out;
    }

    public static String songUrl(Context ctx, String id) {
        if (id == null || id.length() == 0) return null;
        try {
            JSONObject root = getJson(ctx,
                    BASE + "/api/song/enhance/player/url?ids=%5B" + id + "%5D&br=320000", null);
            if (root == null) return null;
            JSONArray data = root.optJSONArray("data");
            if (data == null || data.length() == 0) return null;
            JSONObject o = data.optJSONObject(0);
            if (o == null) return null;
            String u = o.optString("url", "");
            if (u == null || u.length() == 0 || "null".equals(u)) return null;
            return u;
        } catch (Throwable t) {
            return null;
        }
    }

    private static Song parseSong(JSONObject o) {
        Song s = new Song();
        s.id = String.valueOf(o.optLong("id", 0));
        if ("0".equals(s.id)) s.id = o.optString("id", "");
        s.name = o.optString("name", o.optString("title", ""));
        s.durationMs = o.optLong("dt", o.optLong("duration", 0));

        JSONArray ar = o.optJSONArray("ar");
        if (ar == null) ar = o.optJSONArray("artists");
        StringBuilder sb = new StringBuilder();
        if (ar != null) {
            for (int i = 0; i < ar.length(); i++) {
                JSONObject a = ar.optJSONObject(i);
                if (a == null) continue;
                String n = a.optString("name", "");
                if (n.length() == 0) continue;
                if (sb.length() > 0) sb.append("/");
                sb.append(n);
            }
        }
        s.artist = sb.toString();

        JSONObject al = o.optJSONObject("al");
        if (al == null) al = o.optJSONObject("album");
        if (al != null) s.cover = al.optString("picUrl", "");
        if (s.cover.length() == 0) {
            JSONObject pic = o.optJSONObject("pic");
            if (pic != null) s.cover = pic.optString("url", "");
        }
        return s;
    }

    private static JSONObject getJson(Context ctx, String url, String referer) {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.setRequestProperty("User-Agent", UA);
            conn.setRequestProperty("Referer", referer == null ? BASE + "/" : referer);
            CookieStore.Entry e = CookieStore.get(ctx, "netease");
            if (e != null && e.cookie.length() > 0) {
                conn.setRequestProperty("Cookie", e.cookie);
            }
            int code = conn.getResponseCode();
            if (code != 200) return null;
            InputStream in = conn.getInputStream();
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            int total = 0;
            while ((n = in.read(buf)) > 0) {
                total += n;
                if (total > 2 * 1024 * 1024) break;
                bo.write(buf, 0, n);
            }
            try { in.close(); } catch (Throwable ignore) { }
            String body = new String(bo.toByteArray(), "UTF-8");
            if (body.length() == 0) return null;
            return new JSONObject(body);
        } catch (Throwable t) {
            return null;
        } finally {
            try { if (conn != null) conn.disconnect(); } catch (Throwable ignore) { }
        }
    }
}
