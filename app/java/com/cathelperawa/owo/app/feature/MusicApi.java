package com.cathelperawa.owo.app.feature;

import com.cathelperawa.owo.app.core.HookLog;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public final class MusicApi {

    private MusicApi() {}

    private static final String BASE = "https://music.163.com";
    private static final String UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                    + "(KHTML, like Gecko) Chrome/124.0 Safari/537.36";
    private static final String REFERER = "https://music.163.com/";

    public static final class Song {
        public String id = "";
        public String name = "";
        public String artist = "";
        public String album = "";
        public String pic = "";
        public long durationMs = 0;
        
        public boolean trial = false;
        
        public String directUrl = "";
        
        public String source = "网易云";

        public String durationText() {
            if (durationMs <= 0) return "";
            long s = durationMs / 1000;
            return (s / 60) + ":" + (s % 60 < 10 ? "0" : "") + (s % 60);
        }

        @Override public String toString() {
            return name + (artist.length() > 0 ? " - " + artist : "");
        }
    }

    

    
    public static List<Song> search(String keyword, int limit) {
        List<Song> out = new ArrayList<Song>();
        if (keyword == null || keyword.trim().length() == 0) return out;
        if (limit <= 0) limit = 10;
        if (limit > 30) limit = 30;
        try {
            String enc = URLEncoder.encode(keyword.trim(), "UTF-8");
            String url = BASE + "/api/cloudsearch/pc?s=" + enc + "&type=1&limit=" + limit;
            String body = httpGet(url, 15000);
            if (body == null || body.length() == 0) return out;
            JSONObject root = new JSONObject(body);
            JSONObject result = root.optJSONObject("result");
            if (result == null) return out;
            JSONArray songs = result.optJSONArray("songs");
            if (songs == null) return out;

            for (int i = 0; i < songs.length(); i++) {
                JSONObject o = songs.optJSONObject(i);
                if (o == null) continue;
                Song s = new Song();
                s.id = o.optString("id", "");
                s.name = o.optString("name", "");
                s.durationMs = o.optLong("dt", 0);
                if (s.durationMs <= 0) s.durationMs = o.optLong("duration", 0);

                JSONArray ar = o.optJSONArray("ar");
                if (ar == null) ar = o.optJSONArray("artists");
                if (ar != null && ar.length() > 0) {
                    JSONObject a0 = ar.optJSONObject(0);
                    if (a0 != null) s.artist = a0.optString("name", "");
                }
                JSONObject al = o.optJSONObject("al");
                if (al == null) al = o.optJSONObject("album");
                if (al != null) {
                    s.album = al.optString("name", "");
                    s.pic = al.optString("picUrl", "");
                }
                if (s.id.length() > 0 && s.name.length() > 0) out.add(s);
            }
        } catch (Throwable t) {
            HookLog.e("MusicApi.search 异常", t);
        }
        return out;
    }

    

    
    public static volatile String lastError = "";

    public static String musicCookie() {
        try {
            String c = com.cathelperawa.owo.app.cookie.CookieBridge.get("netease");
            if (c != null && c.length() > 0) return c;
        } catch (Throwable ignore) {
        }
        try {
            String c = com.cathelperawa.owo.app.core.ConfigStore.getString("music_cookie");
            return c == null ? "" : c.trim();
        } catch (Throwable t) {
            return "";
        }
    }

    public static String source() {
        try {
            String v = com.cathelperawa.owo.app.core.HookConfig.get(
                    com.cathelperawa.owo.app.bridge.QQEnv.appContext(),
                    "pk_music_source", "public");
            return v == null ? "public" : v.trim();
        } catch (Throwable t) {
            return "public";
        }
    }

    public static boolean useAccount() {
        return "account".equals(source());
    }

    private static List<String[]> customApis() {
        List<String[]> out = new ArrayList<String[]>();
        try {
            String raw = com.cathelperawa.owo.app.core.HookConfig.get(
                    com.cathelperawa.owo.app.bridge.QQEnv.appContext(),
                    "pk_music_apis", "");
            if (raw == null || raw.trim().length() == 0) return out;
            org.json.JSONArray arr = new org.json.JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                org.json.JSONObject o = arr.optJSONObject(i);
                if (o == null) continue;
                String n = o.optString("n", "接口" + (i + 1));
                String u = o.optString("u", "");
                if (u.trim().length() == 0) continue;
                out.add(new String[]{n, u.trim()});
            }
        } catch (Throwable ignore) {
        }
        return out;
    }

    

    
    public static String resolveUrl(String id, String name, String artist, int br) {
        lastError = "";

        int[] p = probe(id, br);
        boolean trial = p != null && p[2] == 1;
        String trialUrl = null;
        if (!trial) {
            String u = mp3Url(id, br);
            if (u != null) return u;
            u = mp3Url(id, 128000);
            if (u != null) return u;
        } else {

            trialUrl = mp3Url(id, 128000);
            lastError = trialUrl != null ? "只给试听（30 秒）" : "只给试听";
        }
        String u;

        u = gdstudio(id, br);
        if (u != null) return u;
        u = gdstudio(id, 128000);
        if (u != null) return u;

        u = kuwo(name, artist);
        if (u != null) return u;

        if (trialUrl != null) return trialUrl;

        if (lastError.length() == 0) lastError = "所有源都拿不到";
        return null;
    }

    

    
    public static final class Pick {
        public String id = "";
        public String name = "";
        public String artist = "";
        public String url = "";
        public String source = "";
        
        public boolean trial = false;
        @Override public String toString() { return name + " - " + artist + " [" + source + "]"; }
    }

    private static boolean badVersion(String n) {
        if (n == null) return true;
        String[] bad = {"伴奏", "片段", "铃声", "DJ", "dj", "纯音乐", "KTV", "ktv",
                "cover", "Cover", "翻自", "降调", "升调", "remix", "Remix", "8D", "加速版", "慢速"};
        for (int i = 0; i < bad.length; i++) if (n.contains(bad[i])) return true;
        return false;
    }

    public static Pick pick(String keyword, int br) {
        
        if ("custom".equals(source())) {
            for (String[] api : customApis()) {
                try {
                    List<Song> r = searchCustom(api[0], api[1], keyword, 6);
                    for (Song s2 : r) {
                        if (s2.directUrl != null && s2.directUrl.length() > 0) {
                            Pick p = new Pick();
                            p.id = s2.id;
                            p.name = s2.name;
                            p.artist = s2.artist;
                            p.url = s2.directUrl;
                            p.source = api[0];
                            return p;
                        }
                    }
                } catch (Throwable t) {
                    lastError = "自定义接口 " + api[0] + " 失败：" + t;
                }
            }
        }

        Pick trialPick = null;
        List<Song> list = search(keyword, 30);
        int tried = 0;
        for (int i = 0; i < list.size(); i++) {
            Song s = list.get(i);
            if (badVersion(s.name)) continue;
            if (++tried > 20) break;

            String[] o = officialUrl(s.id, br);
            if (o[0] != null && o[0].length() > 0) {
                Pick p = new Pick();
                p.id = s.id;
                p.name = s.name;
                p.artist = s.artist;
                p.url = o[0];
                p.source = "网易云";
                p.trial = "1".equals(o[1]);
                if (!p.trial) return p;
                if (trialPick == null) trialPick = p;
                continue;
            }
            String g = gdstudio(s.id, br);
            if (g != null) {
                Pick p = new Pick();
                p.id = s.id; p.name = s.name; p.artist = s.artist;
                p.url = g; p.source = "gdstudio";
                return p;
            }
            String k = kuwoPick(s.name, s.artist);
            if (k != null) {
                Pick p = new Pick();
                p.id = s.id; p.name = s.name; p.artist = s.artist;
                p.url = k; p.source = "酷我";
                return p;
            }
        }
        return trialPick;
    }

    private static String[] officialUrl(String id, int br) {
        if (id == null || id.length() == 0) return new String[]{null, "0"};
        try {
            String url = BASE + "/api/song/enhance/player/url?ids=%5B" + id + "%5D&br=" + br;
            String body = httpGet(url, 15000);
            if (body == null || body.length() == 0) return new String[]{null, "0"};
            JSONArray data = new JSONObject(body).optJSONArray("data");
            if (data == null || data.length() == 0) return new String[]{null, "0"};
            JSONObject x = data.optJSONObject(0);
            if (x == null) return new String[]{null, "0"};
            String u = x.optString("url", "");
            if (u == null || u.length() == 0) return new String[]{null, "0"};
            boolean t = x.opt("freeTrialInfo") != null && !JSONObject.NULL.equals(x.opt("freeTrialInfo"));
            return new String[]{u, t ? "1" : "0"};
        } catch (Throwable ignore) {
            return new String[]{null, "0"};
        }
    }

    private static String kuwoPick(String name, String artist) {
        if (name == null || name.trim().length() == 0) return null;
        try {
            String kw = URLEncoder.encode(name.trim(), "UTF-8");
            String search = "http://search.kuwo.cn/r.s?all=" + kw
                    + "&ft=music&itemset=web_2013&client=kt&pn=0&rn=8"
                    + "&rformat=json&encoding=utf8";
            String body = httpGet(search, 15000);
            if (body == null || body.length() < 20) return null;

            String want = norm(name);
            String who = artist == null ? "" : norm(artist);
            
            String[] items = body.split("\\},\\{");
            for (int i = 0; i < items.length && i < 8; i++) {
                String it = items[i];
                String sn = jsonField(it, "SONGNAME");
                String ar = jsonField(it, "ARTIST");
                String rid = jsonField(it, "MUSICRID");
                if (sn.length() == 0 || rid.length() == 0) continue;
                sn = sn.replace("&nbsp;", " ").trim();
                ar = ar.replace("&nbsp;", " ").trim();
                if (badVersion(sn)) continue;
                if (!norm(sn).contains(want) && !want.contains(norm(sn))) continue;
                if (who.length() > 0 && !norm(ar).contains(who) && !who.contains(norm(ar))) continue;
                rid = rid.replace("MUSIC_", "").replaceAll("[^0-9]", "");
                if (rid.length() == 0) continue;
                String anti = "http://antiserver.kuwo.cn/anti.s?type=convert_url"
                        + "&rid=MUSIC_" + rid + "&format=mp3&response=url";
                String u = httpGet(anti, 12000);
                if (u != null) {
                    u = u.trim();
                    if (u.startsWith("http")) return u;
                }
            }
        } catch (Throwable t) {
            HookLog.w("MusicApi.kuwoPick 失败 " + t);
        }
        return null;
    }

    private static String jsonField(String item, String key) {
        int i = item.indexOf("'" + key + "'");
        if (i < 0) return "";
        int c = item.indexOf(':', i);
        if (c < 0) return "";
        int q1 = item.indexOf('\'', c + 1);
        if (q1 < 0) return "";
        int q2 = item.indexOf('\'', q1 + 1);
        if (q2 < 0) return "";
        return item.substring(q1 + 1, q2);
    }

    private static String norm(String s) {
        if (s == null) return "";
        String t = s.toLowerCase(java.util.Locale.ROOT);
        t = t.replaceAll("\\(.*?\\)|（.*?）|\\[.*?\\]", "");
        t = t.replaceAll("[\\s\\-_/&,，。.、'\"!！?？+—~·]", "");
        return t.trim();
    }

    private static String gdstudio(String id, int br) {
        if (id == null || id.length() == 0) return null;
        try {
            String b = br >= 320000 ? "320" : (br >= 192000 ? "192" : "128");
            String url = "https://music-api.gdstudio.xyz/api.php?types=url&source=netease"
                    + "&id=" + id + "&br=" + b;
            String body = httpGet(url, 15000);
            if (body == null || body.length() < 10) return null;
            JSONObject o = new JSONObject(body);
            String u = o.optString("url", "");
            if (u.startsWith("http")) {
                lastError = "";
                return u;
            }
        } catch (Throwable t) {
            HookLog.w("MusicApi.gdstudio 失败 " + t);
        }
        return null;
    }

    private static String kuwo(String name, String artist) {
        if (name == null || name.length() == 0) return null;
        try {
            String kw = URLEncoder.encode(name.trim(), "UTF-8");
            String search = "http://search.kuwo.cn/r.s?all=" + kw
                    + "&ft=music&itemset=web_2013&client=kt&pn=0&rn=3"
                    + "&rformat=json&encoding=utf8";
            String body = httpGet(search, 15000);
            if (body == null || body.length() < 20) return null;
            
            java.util.regex.Matcher m = java.util.regex.Pattern
                    .compile("MUSICRID['\"]?\\s*[:=]\\s*['\"]?(?:MUSIC_)?(\\d+)")
                    .matcher(body);
            while (m.find()) {
                String rid = m.group(1);
                String anti = "http://antiserver.kuwo.cn/anti.s?type=convert_url"
                        + "&rid=MUSIC_" + rid + "&format=mp3&response=url";
                String u = httpGet(anti, 12000);
                if (u != null) {
                    u = u.trim();
                    if (u.startsWith("http")) {
                        lastError = "";
                        return u;
                    }
                }
            }
        } catch (Throwable t) {
            HookLog.w("MusicApi.kuwo 失败 " + t);
        }
        return null;
    }

    public static String mp3Url(String id, int br) {
        lastError = "";
        if (id == null || id.length() == 0) {
            lastError = "没给 id";
            return null;
        }
        if (br <= 0) br = 320000;
        try {
            String url = BASE + "/api/song/enhance/player/url?ids=%5B" + id + "%5D&br=" + br;
            String body = httpGet(url, 15000);
            if (body == null || body.length() == 0) {
                lastError = "接口没返回";
                return null;
            }
            JSONObject root = new JSONObject(body);
            JSONArray data = root.optJSONArray("data");
            if (data == null || data.length() == 0) {
                lastError = "data 为空";
                return null;
            }
            JSONObject x = data.optJSONObject(0);
            if (x == null) {
                lastError = "data[0] 为空";
                return null;
            }
            String u = x.optString("url", "");
            if (u == null || u.length() == 0) {
                int code = x.optInt("code", 0);
                lastError = code == 404 ? "无版权" : ("拿不到直链 code=" + code);
                return null;
            }
            return u;
        } catch (Throwable t) {
            lastError = "异常 " + t;
            HookLog.e("MusicApi.mp3Url 异常", t);
            return null;
        }
    }

    public static void detail(Song s) {
        if (s == null || s.id == null || s.id.length() == 0) return;
        try {
            String url = BASE + "/api/song/detail?ids=%5B" + s.id + "%5D";
            String body = httpGet(url, 15000);
            if (body == null || body.length() == 0) return;
            JSONObject root = new JSONObject(body);
            JSONArray songs = root.optJSONArray("songs");
            if (songs == null || songs.length() == 0) return;
            JSONObject o = songs.optJSONObject(0);
            if (o == null) return;
            if (s.pic.length() == 0) {
                JSONObject al = o.optJSONObject("al");
                if (al == null) al = o.optJSONObject("album");
                if (al != null) {
                    s.pic = al.optString("picUrl", "");
                    if (s.album.length() == 0) s.album = al.optString("name", "");
                }
            }
            if (s.artist.length() == 0) {
                JSONArray ar = o.optJSONArray("ar");
                if (ar == null) ar = o.optJSONArray("artists");
                if (ar != null && ar.length() > 0) {
                    JSONObject a0 = ar.optJSONObject(0);
                    if (a0 != null) s.artist = a0.optString("name", "");
                }
            }
            if (s.durationMs <= 0) {
                s.durationMs = o.optLong("dt", 0);
                if (s.durationMs <= 0) s.durationMs = o.optLong("duration", 0);
            }
        } catch (Throwable t) {
            HookLog.w("MusicApi.detail 失败 " + t);
        }
    }

    public static int[] probe(String id, int br) {
        try {
            String url = BASE + "/api/song/enhance/player/url?ids=%5B" + id + "%5D&br=" + br;
            String body = httpGet(url, 15000);
            JSONObject root = new JSONObject(body);
            JSONArray data = root.optJSONArray("data");
            if (data == null || data.length() == 0) return null;
            JSONObject x = data.optJSONObject(0);
            if (x == null) return null;
            int realBr = x.optInt("br", 0);
            long size = x.optLong("size", 0);
            boolean trial = x.opt("freeTrialInfo") != null
                    && !JSONObject.NULL.equals(x.opt("freeTrialInfo"));
            return new int[]{realBr, (int) Math.min(size, Integer.MAX_VALUE), trial ? 1 : 0};
        } catch (Throwable t) {
            return null;
        }
    }

    

    public static String httpGet(String url, int timeoutMs) throws Exception {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setConnectTimeout(timeoutMs);
            conn.setReadTimeout(timeoutMs);
            conn.setRequestProperty("User-Agent", UA);
            conn.setRequestProperty("Referer", REFERER);
            
            String ck = musicCookie();
            if (useAccount() && ck.length() > 0 && url != null && url.contains("music.163.com")) {
                conn.setRequestProperty("Cookie", ck);
            }
            conn.setInstanceFollowRedirects(true);
            int code = conn.getResponseCode();
            InputStream in = code >= 400 ? conn.getErrorStream() : conn.getInputStream();
            if (in == null) return "";
            java.io.ByteArrayOutputStream bo = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            int total = 0;
            while ((n = in.read(buf)) > 0) {
                total += n;
                if (total > 4 * 1024 * 1024) break;
                bo.write(buf, 0, n);
            }
            try { in.close(); } catch (Throwable ignore) { }
            return new String(bo.toByteArray(), "UTF-8");
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    public static long download(String url, File out, long maxBytes) {
        HttpURLConnection conn = null;
        FileOutputStream fo = null;
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(30000);
            conn.setRequestProperty("User-Agent", UA);
            conn.setRequestProperty("Referer", REFERER);
            int code = conn.getResponseCode();
            if (code >= 400) return 0;
            long total = conn.getContentLengthLong();
            if (maxBytes > 0 && total > maxBytes) return 0;
            InputStream in = conn.getInputStream();
            if (in == null) return 0;
            fo = new FileOutputStream(out);
            byte[] buf = new byte[64 * 1024];
            long got = 0;
            int n;
            while ((n = in.read(buf)) > 0) {
                got += n;
                if (maxBytes > 0 && got > maxBytes) {
                    try { fo.close(); } catch (Throwable ignore) { }
                    fo = null;
                    out.delete();
                    return 0;
                }
                fo.write(buf, 0, n);
            }
            fo.flush();
            fo.close();
            fo = null;
            return got;
        } catch (Throwable t) {
            HookLog.e("MusicApi.download 异常", t);
            return 0;
        } finally {
            try { if (fo != null) fo.close(); } catch (Throwable ignore) { }
            if (conn != null) conn.disconnect();
        }
    }

    

    
    public static List<Song> searchCustom(String sourceName, String api, String keyword, int limit) {
        List<Song> out = new ArrayList<Song>();
        if (api == null || api.trim().length() == 0) return out;
        if (keyword == null || keyword.trim().length() == 0) return out;
        if (limit <= 0) limit = 10;
        try {
            String enc = URLEncoder.encode(keyword.trim(), "UTF-8");
            String base = api.trim();
            String target;
            if (base.contains("{query}")) target = base.replace("{query}", enc);
            else if (base.contains("{kw}")) target = base.replace("{kw}", enc);
            else if (base.contains("%s")) target = String.format(base, enc);
            else target = base + (base.contains("?") ? "&q=" + enc : "?q=" + enc);

            String body = httpGet(target, 15000);
            if (body == null || body.length() < 4) return out;
            body = body.trim();
            Object root;
            if (body.startsWith("{")) root = new JSONObject(body);
            else if (body.startsWith("[")) root = new JSONArray(body);
            else return out;

            collectSongs(root, out, limit, sourceName, 0);
        } catch (Throwable t) {
            HookLog.w("MusicApi.searchCustom 失败 " + sourceName + " " + t);
        }
        return out;
    }

    private static void collectSongs(Object o, List<Song> out, int limit, String src, int depth) {
        if (o == null || out.size() >= limit || depth > 7) return;
        try {
            if (o instanceof JSONArray) {
                JSONArray a = (JSONArray) o;
                for (int i = 0; i < a.length() && out.size() < limit; i++) {
                    collectSongs(a.opt(i), out, limit, src, depth + 1);
                }
            } else if (o instanceof JSONObject) {
                JSONObject jo = (JSONObject) o;
                Song s = pickSong(jo, src);
                if (s != null) {
                    out.add(s);
                    return;   
                }
                java.util.Iterator<String> it = jo.keys();
                while (it.hasNext() && out.size() < limit) {
                    collectSongs(jo.opt(it.next()), out, limit, src, depth + 1);
                }
            }
        } catch (Throwable ignore) {
        }
    }

    private static Song pickSong(JSONObject o, String src) {
        try {
            
            String name = firstStr(o, "name", "title", "song", "songname", "songName", "musicName");
            if (name == null) return null;
            
            String id = firstStr(o, "id", "songid", "songId", "songmid", "songMid", "rid", "mid");
            String url = firstStr(o, "url", "play_url", "playUrl", "mp3", "src", "audio", "link");
            if (url != null && url.startsWith("/")) url = null;   
            if (id == null && url == null) return null;

            Song s = new Song();
            s.name = name;
            s.id = id == null ? "" : id;
            s.directUrl = url == null ? "" : url;
            s.source = src;

            String artist = firstStr(o, "artist", "singer", "author", "artists");
            if (artist == null) {
                Object ar = o.opt("ar");
                if (ar == null) ar = o.opt("artists");
                if (ar == null) ar = o.opt("singer");
                if (ar instanceof JSONArray && ((JSONArray) ar).length() > 0) {
                    Object a0 = ((JSONArray) ar).opt(0);
                    if (a0 instanceof JSONObject) artist = ((JSONObject) a0).optString("name", null);
                    else if (a0 != null) artist = String.valueOf(a0);
                } else if (ar instanceof JSONObject) {
                    artist = ((JSONObject) ar).optString("name", null);
                }
            }
            s.artist = artist == null ? "" : artist;

            String pic = firstStr(o, "pic", "cover", "picUrl", "albumPic", "img");
            if (pic == null) {
                Object al = o.opt("al");
                if (al == null) al = o.opt("album");
                if (al instanceof JSONObject) pic = ((JSONObject) al).optString("picUrl", null);
            }
            s.pic = pic == null ? "" : pic;

            long dur = o.optLong("duration", 0);
            if (dur <= 0) dur = o.optLong("dt", 0);
            if (dur <= 0) dur = o.optLong("interval", 0);
            
            if (dur > 0 && dur < 10000) dur *= 1000;
            s.durationMs = dur;

            return s;
        } catch (Throwable t) {
            return null;
        }
    }

    private static String firstStr(JSONObject o, String... keys) {
        for (String k : keys) {
            try {
                Object v = o.opt(k);
                if (v == null || JSONObject.NULL.equals(v)) continue;
                if (v instanceof String) {
                    String s = ((String) v).trim();
                    if (s.length() > 0) return s;
                } else if (v instanceof Number) {
                    return String.valueOf(v);
                }
            } catch (Throwable ignore) {
            }
        }
        return null;
    }

    public static String urlCustom(String api, Song song) {
        if (song == null) return null;
        if (song.directUrl != null && song.directUrl.startsWith("http")) return song.directUrl;
        if (api == null || api.trim().length() == 0 || song.id.length() == 0) return null;
        try {
            String base = api.trim();
            String target;
            if (base.contains("{id}")) target = base.replace("{id}", URLEncoder.encode(song.id, "UTF-8"));
            else if (base.contains("%s")) target = String.format(base, URLEncoder.encode(song.id, "UTF-8"));
            else target = base + (base.contains("?") ? "&id=" + URLEncoder.encode(song.id, "UTF-8")
                    : "?id=" + URLEncoder.encode(song.id, "UTF-8"));
            String body = httpGet(target, 15000);
            if (body == null || body.length() < 4) return null;
            body = body.trim();
            if (body.startsWith("http")) return body;     
            Object root;
            if (body.startsWith("{")) root = new JSONObject(body);
            else return null;
            String u = findUrl(root, 0);
            return (u != null && u.startsWith("http")) ? u : null;
        } catch (Throwable t) {
            HookLog.w("MusicApi.urlCustom 失败 " + t);
            return null;
        }
    }

    private static String findUrl(Object o, int depth) {
        if (o == null || depth > 6) return null;
        try {
            if (o instanceof JSONArray) {
                JSONArray a = (JSONArray) o;
                for (int i = 0; i < Math.min(a.length(), 8); i++) {
                    String u = findUrl(a.opt(i), depth + 1);
                    if (u != null) return u;
                }
            } else if (o instanceof JSONObject) {
                JSONObject jo = (JSONObject) o;
                String[] keys = {"url", "play_url", "playUrl", "src", "mp3", "audio", "link"};
                for (String k : keys) {
                    String v = jo.optString(k, "");
                    if (v.startsWith("http")) return v;
                }
                java.util.Iterator<String> it = jo.keys();
                while (it.hasNext()) {
                    String u = findUrl(jo.opt(it.next()), depth + 1);
                    if (u != null) return u;
                }
            }
        } catch (Throwable ignore) {
        }
        return null;
    }

    public static byte[] cover(String picUrl) {
        if (picUrl == null || picUrl.length() == 0) return null;
        try {
            String u = picUrl.contains("?") ? picUrl + "&param=300y300" : picUrl + "?param=300y300";
            HttpURLConnection conn = (HttpURLConnection) new URL(u).openConnection();
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);
            conn.setRequestProperty("User-Agent", UA);
            conn.setRequestProperty("Referer", REFERER);
            if (conn.getResponseCode() >= 400) return null;
            InputStream in = conn.getInputStream();
            if (in == null) return null;
            java.io.ByteArrayOutputStream bo = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) {
                if (bo.size() > 3 * 1024 * 1024) break;
                bo.write(buf, 0, n);
            }
            try { in.close(); } catch (Throwable ignore) { }
            conn.disconnect();
            return bo.toByteArray();
        } catch (Throwable t) {
            return null;
        }
    }
}
