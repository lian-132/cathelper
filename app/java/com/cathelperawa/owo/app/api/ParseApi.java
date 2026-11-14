package com.cathelperawa.owo.app.api;

import android.content.Context;
import android.graphics.Bitmap;

import com.cathelperawa.owo.app.bridge.QQEnv;
import com.cathelperawa.owo.app.bridge.VoiceSilk;
import com.cathelperawa.owo.app.core.HookLog;
import com.cathelperawa.owo.app.feature.MusicApi;
import com.cathelperawa.owo.app.feature.MusicCardImage;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ParseApi {

    private ParseApi() { }

    private static final String UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                    + "(KHTML, like Gecko) Chrome/124.0 Safari/537.36";

    private static final Pattern P_BV = Pattern.compile("(BV[0-9A-Za-z]{10})");

    private static final String K_CONV = "pk_music_conv";
    private static final String K_FAKE_SEC = "pk_music_fake_sec";

    public static void setAutoConvert(boolean on) {
        try {
            android.content.Context c = QQEnv.appContext();
            if (c == null) return;
            com.cathelperawa.owo.app.core.HookConfig.set(c, K_CONV, on ? "1" : "0");
        } catch (Throwable t) {
            HookLog.e("ParseApi.setAutoConvert 异常", t);
        }
    }

    public static boolean isAutoConvert() {
        try {
            android.content.Context c = QQEnv.appContext();
            if (c == null) return true;
            return com.cathelperawa.owo.app.core.HookConfig.getBoolean(c, K_CONV, true);
        } catch (Throwable t) {
            return true;
        }
    }

    public static void setDisplayDuration(int seconds) {
        try {
            android.content.Context c = QQEnv.appContext();
            if (c == null) return;
            int v = seconds < 0 ? 0 : seconds;
            com.cathelperawa.owo.app.core.HookConfig.set(c, K_FAKE_SEC, String.valueOf(v));
        } catch (Throwable t) {
            HookLog.e("ParseApi.setDisplayDuration 异常", t);
        }
    }

    public static int getDisplayDuration() {
        try {
            android.content.Context c = QQEnv.appContext();
            if (c == null) return 0;
            String s = com.cathelperawa.owo.app.core.HookConfig.get(c, K_FAKE_SEC, "0");
            int v = Integer.parseInt(s.trim());
            return v < 0 ? 0 : v;
        } catch (Throwable t) {
            return 0;
        }
    }

    public static String searchMusic(String keyword, int limit) {
        try {
            List<MusicApi.Song> list = MusicApi.search(keyword, limit);
            JSONArray arr = new JSONArray();
            if (list != null) {
                for (MusicApi.Song s : list) {
                    JSONObject o = new JSONObject();
                    o.put("id", s.id);
                    o.put("name", s.name);
                    o.put("artist", s.artist);
                    o.put("album", s.album);
                    o.put("pic", s.pic);
                    o.put("durationMs", s.durationMs);
                    o.put("source", s.source);
                    arr.put(o);
                }
            }
            return arr.toString();
        } catch (Throwable t) {
            HookLog.e("ParseApi.searchMusic 异常", t);
            return "[]";
        }
    }

    public static String musicUrl(String songId, String name, String artist, int br) {
        try {
            String u = MusicApi.resolveUrl(songId, name, artist, br);
            return u == null ? "" : u;
        } catch (Throwable t) {
            return "";
        }
    }

    private static String urlFor(String songId, String name, String artist) {
        String url = MusicApi.resolveUrl(songId, name, artist, 320000);
        if (url == null) url = MusicApi.resolveUrl(songId, name, artist, 128000);
        if (url == null) {
            String kw = (name == null ? "" : name)
                    + (artist == null || artist.length() == 0 ? "" : " " + artist);
            MusicApi.Pick p = MusicApi.pick(kw.trim(), 320000);
            if (p != null && p.url != null && p.url.length() > 0) {
                url = p.url;
                HookLog.w("音乐: 换用「" + p.name + " - " + p.artist + "」来源 " + p.source
                        + (p.trial ? "（仅试听 30s）" : ""));
            }
        }
        return url;
    }

    public static boolean sendMusicByKeyword(int chatType, String peerUid,
                                             String keyword, int maxSeconds) {
        try {
            List<MusicApi.Song> list = MusicApi.search(keyword, 1);
            if (list == null || list.isEmpty()) return false;
            MusicApi.Song s = list.get(0);

            String mode = com.cathelperawa.owo.app.core.ConfigStore.getString("pk_music_send");
            if (mode == null || mode.length() == 0) mode = "card";
            if ("card".equals(mode)) {
                if (sendMusicCard(chatType, peerUid, s.id, s.name, s.artist, s.pic)) return true;

                return sendMusicImage(chatType, peerUid, s.id, s.name, s.artist);
            }
            if ("file".equals(mode)) {
                return sendMusicFile(chatType, peerUid, s.id, s.name, s.artist);
            }
            if ("image".equals(mode)) {
                return sendMusicImage(chatType, peerUid, s.id, s.name, s.artist);
            }

            if (!isAutoConvert()) {
                return sendMusicMp3(chatType, peerUid, s.id, s.name, s.artist);
            }
            return sendMusicVoice(chatType, peerUid, s.id, s.name, s.artist, s.pic, maxSeconds);
        } catch (Throwable t) {
            HookLog.e("ParseApi.sendMusicByKeyword 异常", t);
            return false;
        }
    }

    public static boolean sendMusicVoice(int chatType, String peerUid,
                                         String songId, String name, String artist,
                                         String pic, int maxSeconds) {
        Context c = QQEnv.appContext();
        File mp3 = null, silk = null;
        try {
            if (c == null) return false;
            String url = urlFor(songId, name, artist);
            if (url == null || url.length() == 0) return false;

            File dir = tmpDir(c);
            long ts = System.currentTimeMillis();
            mp3 = new File(dir, "api_" + ts + ".mp3");
            if (MusicApi.download(url, mp3, 80L * 1024 * 1024) <= 0) return false;

            silk = new File(dir, "api_" + ts + ".silk");
            String out = VoiceSilk.audioToSilk(c, mp3, silk.getAbsolutePath(), maxSeconds);
            if (out == null) return false;

            int real = VoiceSilk.lastDurationMs;
            int fake = getDisplayDuration() * 1000;
            int dur = fake > 0 ? fake : real;
            boolean ok = MsgApi.sendPtt(chatType, peerUid, out, dur);
            keepLater(mp3, silk);
            return ok;
        } catch (Throwable t) {
            HookLog.e("ParseApi.sendMusicVoice 异常", t);
            return false;
        } finally {

        }
    }

    public static boolean sendMusicMp3(int chatType, String peerUid,
                                       String songId, String name, String artist) {
        File mp3 = null;
        try {
            String url = urlFor(songId, name, artist);
            if (url == null || url.length() == 0) return false;
            Context c = QQEnv.appContext();
            if (c == null) return false;
            File dir = tmpDir(c);
            mp3 = new File(dir, safe(name) + ".mp3");
            long got = MusicApi.download(url, mp3, 80L * 1024 * 1024);
            if (got <= 0) return false;
            int real = (int) Math.min(Integer.MAX_VALUE, got * 8L / 320);
            int fake = getDisplayDuration() * 1000;
            int dur = fake > 0 ? fake : Math.max(real, 1000);
            boolean ok = MsgApi.sendPtt(chatType, peerUid, mp3.getAbsolutePath(), dur);
            keepLater(mp3, null);
            return ok;
        } catch (Throwable t) {
            HookLog.e("ParseApi.sendMusicMp3 异常", t);
            return false;
        }
    }

    public static boolean sendMusicFile(int chatType, String peerUid,
                                        String songId, String name, String artist) {
        File mp3 = null;
        try {
            String url = urlFor(songId, name, artist);
            if (url == null || url.length() == 0) return false;
            Context c = QQEnv.appContext();
            if (c == null) return false;
            File dir = tmpDir(c);
            mp3 = new File(dir, safe(name) + ".mp3");
            if (MusicApi.download(url, mp3, 80L * 1024 * 1024) <= 0) return false;
            boolean ok = MsgApi.sendFile(chatType, peerUid, mp3.getAbsolutePath());
            keepLater(mp3, null);
            return ok;
        } catch (Throwable t) {
            HookLog.e("ParseApi.sendMusicFile 异常", t);
            return false;
        }
    }

    public static boolean sendMusicCard(int chatType, String peerUid,
                                        String songId, String name, String artist, String pic) {
        try {
            if (songId == null || songId.length() == 0) return false;
            if (chatType <= 0 || peerUid == null || peerUid.length() == 0) return false;
            String audio = "http://music.163.com/song/media/outer/url?id=" + songId + "&sc=wm&tn=";
            if (!musicPlayable(audio)) {
                HookLog.w("音乐卡: 这首没有可播音源（VIP/无版权） " + songId + " " + name);
                return false;
            }
            String cover = pic == null ? "" : pic;
            if (cover.length() == 0) {
                cover = "https://p1.music.126.net/me6QV0CallEOlOP6Zb0b3w==/109951165770805050.jpg";
            }
            String card = com.cathelperawa.owo.app.feature.ArkSigner.signMusic(
                    name == null ? "" : name,
                    artist == null ? "" : artist,
                    cover,
                    audio,
                    "https://music.163.com/#/song?id=" + songId);
            if (card == null) {
                HookLog.w("音乐卡: 签名失败");
                return false;
            }
            boolean ok = SendProtoApi.sendArk(chatType, peerUid, card);
            HookLog.i("音乐卡: 「" + name + " - " + artist + "」" + (ok ? "已发出" : "发送失败"));
            return ok;
        } catch (Throwable t) {
            HookLog.e("ParseApi.sendMusicCard 异常", t);
            return false;
        }
    }

    private static boolean musicPlayable(String url) {
        HttpURLConnection c = null;
        try {
            c = (HttpURLConnection) new URL(url).openConnection(java.net.Proxy.NO_PROXY);
            c.setInstanceFollowRedirects(false);
            c.setConnectTimeout(6000);
            c.setReadTimeout(6000);
            c.setRequestProperty("User-Agent", "Mozilla/5.0");
            c.getResponseCode();
            String loc = c.getHeaderField("Location");
            return loc != null && loc.contains(".mp3");
        } catch (Throwable t) {
            return false;
        } finally {
            if (c != null) try { c.disconnect(); } catch (Throwable ignore) {}
        }
    }

    public static boolean sendMusicImage(int chatType, String peerUid,
                                         String songId, String name, String artist) {
        File png = null;
        try {
            Context c = QQEnv.appContext();
            if (c == null) return false;
            MusicApi.Song s = new MusicApi.Song();
            s.id = songId == null ? "" : songId;
            s.name = name == null ? "" : name;
            s.artist = artist == null ? "" : artist;
            MusicApi.detail(s);
            byte[] cover = MusicApi.cover(s.pic);
            Bitmap bmp = MusicCardImage.render(s.name, s.artist, s.album,
                    s.durationText(), cover);
            if (bmp == null) return false;
            File dir = tmpDir(c);
            png = new File(dir, "card_" + System.currentTimeMillis() + ".png");
            FileOutputStream fo = new FileOutputStream(png);
            bmp.compress(Bitmap.CompressFormat.PNG, 92, fo);
            fo.flush();
            fo.close();
            boolean ok = MsgApi.sendPic(chatType, peerUid, png.getAbsolutePath());
            keepLater(png, null);
            return ok;
        } catch (Throwable t) {
            HookLog.e("ParseApi.sendMusicImage 异常", t);
            return false;
        }
    }

    public static boolean sendPtt(int chatType, String peerUid, String path, int durationMs) {
        try {
            return MsgApi.sendPtt(chatType, peerUid, path, durationMs);
        } catch (Throwable t) {
            HookLog.e("ParseApi.sendPtt 异常", t);
            return false;
        }
    }

    public static boolean sendAudioAsPtt(int chatType, String peerUid,
                                         String audioPath, int maxSeconds) {
        try {
            Context c = QQEnv.appContext();
            if (c == null) return false;
            File src = new File(audioPath);
            if (!src.exists()) return false;
            File dir = tmpDir(c);
            File silk = new File(dir, "api_" + System.currentTimeMillis() + ".silk");
            String out = VoiceSilk.audioToSilk(c, src, silk.getAbsolutePath(), maxSeconds);
            if (out == null) return false;
            int fake = getDisplayDuration() * 1000;
            int dur = fake > 0 ? fake : VoiceSilk.lastDurationMs;
            boolean ok = MsgApi.sendPtt(chatType, peerUid, out, dur);
            keepLater(silk, null);
            return ok;
        } catch (Throwable t) {
            HookLog.e("ParseApi.sendAudioAsPtt 异常", t);
            return false;
        }
    }

    public static String biliInfo(String linkOrBvid) {
        JSONObject out = new JSONObject();
        try {
            String bvid = pickBvid(linkOrBvid);
            if (bvid == null) {
                out.put("error", "认不出 BV 号");
                return out.toString();
            }
            String body = httpGet("https://api.bilibili.com/x/web-interface/view?bvid=" + bvid);
            JSONObject root = new JSONObject(body);
            if (root.optInt("code", -1) != 0) {
                out.put("error", "接口 " + root.optInt("code") + " " + root.optString("message"));
                return out.toString();
            }
            JSONObject d = root.optJSONObject("data");
            if (d == null) {
                out.put("error", "没有 data");
                return out.toString();
            }
            out.put("bvid", d.optString("bvid", bvid));
            out.put("title", d.optString("title", ""));
            out.put("desc", d.optString("desc", ""));
            out.put("cover", d.optString("pic", ""));
            out.put("duration", d.optLong("duration", 0));
            JSONObject owner = d.optJSONObject("owner");
            if (owner != null) out.put("author", owner.optString("name", ""));
            JSONObject stat = d.optJSONObject("stat");
            if (stat != null) {
                out.put("view", stat.optLong("view", 0));
                out.put("like", stat.optLong("like", 0));
            }
            long cid = d.optLong("cid", 0);
            String pu = biliPlayUrl(bvid, cid);
            if (pu.length() > 0) out.put("videoUrl", pu);
            return out.toString();
        } catch (Throwable t) {
            HookLog.e("ParseApi.biliInfo 异常", t);
            try { out.put("error", String.valueOf(t)); } catch (Throwable ignore) { }
            return out.toString();
        }
    }

    public static String status() {
        JSONObject o = new JSONObject();
        try {
            o.put("autoConvert", isAutoConvert());
            o.put("displaySec", getDisplayDuration());
            o.put("musicSource", "网易云官方 → gdstudio → 酷我");
            o.put("videoSource", "B站官方（无需 Cookie）");
            o.put("context", QQEnv.appContext() != null);
        } catch (Throwable t) {
            try { o.put("error", String.valueOf(t)); } catch (Throwable ignore) { }
        }
        return o.toString();
    }

    public static boolean biliSendVoice(int chatType, String peerUid,
                                        String linkOrBvid, int maxSeconds) {
        File src = null, silk = null;
        try {
            Context c = QQEnv.appContext();
            if (c == null) return false;
            String bvid = pickBvid(linkOrBvid);
            if (bvid == null) return false;
            String body = httpGet("https://api.bilibili.com/x/web-interface/view?bvid=" + bvid);
            JSONObject d = new JSONObject(body).optJSONObject("data");
            if (d == null) return false;
            long cid = d.optLong("cid", 0);

            String au = biliAudioUrl(bvid, cid);
            String url = au.length() > 0 ? au : biliPlayUrl(bvid, cid);
            if (url.length() == 0) return false;

            File dir = tmpDir(c);
            long ts = System.currentTimeMillis();
            src = new File(dir, "api_" + ts + (au.length() > 0 ? ".m4s" : ".mp4"));
            if (download(url, src, 80L * 1024 * 1024) <= 0) return false;

            silk = new File(dir, "api_" + ts + ".silk");
            String out = VoiceSilk.audioToSilk(c, src, silk.getAbsolutePath(), maxSeconds);
            if (out == null) return false;

            int real = VoiceSilk.lastDurationMs;
            int fake = getDisplayDuration() * 1000;
            int dur = fake > 0 ? fake : real;
            boolean ok = MsgApi.sendPtt(chatType, peerUid, out, dur);
            keepLater(src, silk);
            return ok;
        } catch (Throwable t) {
            HookLog.e("ParseApi.biliSendVoice 异常", t);
            return false;
        }
    }

    private static String biliAudioUrl(String bvid, long cid) {
        if (bvid == null || bvid.length() == 0 || cid <= 0) return "";
        try {
            String u = "https://api.bilibili.com/x/player/playurl?bvid=" + bvid
                    + "&cid=" + cid + "&fnval=16&fnver=0&fourk=0";
            JSONObject root = new JSONObject(httpGet(u));
            JSONObject d = root.optJSONObject("data");
            if (d == null) return "";
            JSONObject dash = d.optJSONObject("dash");
            if (dash == null) return "";
            JSONArray audio = dash.optJSONArray("audio");
            if (audio == null || audio.length() == 0) return "";
            String best = "";
            long bestBw = -1;
            for (int i = 0; i < audio.length(); i++) {
                JSONObject a = audio.optJSONObject(i);
                if (a == null) continue;
                long bw = a.optLong("bandwidth", 0);
                String bu = a.optString("baseUrl", "");
                if (bu.length() == 0) bu = a.optString("base_url", "");
                if (bu.length() > 0 && bw >= bestBw) {
                    bestBw = bw;
                    best = bu;
                }
            }
            return best;
        } catch (Throwable t) {
            return "";
        }
    }

    public static boolean biliSendVideo(int chatType, String peerUid,
                                        String linkOrBvid, int maxMB) {
        File f = null;
        try {
            Context c = QQEnv.appContext();
            if (c == null) return false;
            String bvid = pickBvid(linkOrBvid);
            if (bvid == null) return false;
            String body = httpGet("https://api.bilibili.com/x/web-interface/view?bvid=" + bvid);
            JSONObject d = new JSONObject(body).optJSONObject("data");
            if (d == null) return false;
            String url = biliPlayUrl(bvid, d.optLong("cid", 0));
            if (url.length() == 0) return false;

            File dir = tmpDir(c);
            f = new File(dir, "v_" + System.currentTimeMillis() + ".mp4");
            long max = (maxMB <= 0 ? 100 : maxMB) * 1024L * 1024L;
            long got = download(url, f, max);
            if (got <= 0) return false;
            boolean ok = MsgApi.sendVideo(chatType, peerUid, f.getAbsolutePath());
            keepLater(f, null);
            return ok;
        } catch (Throwable t) {
            HookLog.e("ParseApi.biliSendVideo 异常", t);
            return false;
        }
    }

    private static String pickBvid(String s) {
        if (s == null) return null;
        Matcher m = P_BV.matcher(s);
        if (m.find()) return m.group(1);
        if (s.startsWith("http")) {
            try {
                String real = follow(s);
                Matcher m2 = P_BV.matcher(real == null ? "" : real);
                if (m2.find()) return m2.group(1);
            } catch (Throwable ignore) {
            }
        }
        return null;
    }

    private static String biliPlayUrl(String bvid, long cid) {
        try {
            String u = "https://api.bilibili.com/x/player/playurl?bvid=" + bvid
                    + "&cid=" + cid + "&qn=64&fnval=1&platform=html5&high_quality=1";
            JSONObject root = new JSONObject(httpGet(u));
            JSONObject d = root.optJSONObject("data");
            if (d == null) return "";
            JSONArray durl = d.optJSONArray("durl");
            if (durl == null || durl.length() == 0) return "";
            JSONObject u0 = durl.optJSONObject(0);
            return u0 == null ? "" : u0.optString("url", "");
        } catch (Throwable t) {
            return "";
        }
    }

    private static File tmpDir(Context c) {
        File dir = new File(c.getFilesDir(), "api_tmp");
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    private static String safe(String s) {
        if (s == null) s = "audio";
        return s.replaceAll("[\\\\/:*?\"<>|\\s]+", "_");
    }

    private static void keepLater(final File a, final File b) {
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(
                new Runnable() {
                    @Override public void run() {
                        try { if (a != null) a.delete(); } catch (Throwable ignore) { }
                        try { if (b != null) b.delete(); } catch (Throwable ignore) { }
                    }
                }, 3 * 60 * 1000L);
    }

    private static String httpGet(String url) throws Exception {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);
            conn.setRequestProperty("User-Agent", UA);
            conn.setRequestProperty("Referer", "https://www.bilibili.com");
            conn.setInstanceFollowRedirects(true);
            int code = conn.getResponseCode();
            InputStream in = code >= 400 ? conn.getErrorStream() : conn.getInputStream();
            if (in == null) return "";
            java.io.ByteArrayOutputStream bo = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n, total = 0;
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

    private static String follow(String url) {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);
            conn.setRequestProperty("User-Agent", UA);
            conn.setInstanceFollowRedirects(false);
            conn.getResponseCode();
            String loc = conn.getHeaderField("Location");
            return loc == null ? url : loc;
        } catch (Throwable t) {
            return url;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private static long download(String url, File out, long maxBytes) {
        HttpURLConnection conn = null;
        FileOutputStream fo = null;
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(30000);
            conn.setRequestProperty("User-Agent", UA);
            conn.setRequestProperty("Referer", "https://www.bilibili.com");
            if (conn.getResponseCode() >= 400) return 0;
            long total = conn.getContentLengthLong();
            if (total > maxBytes) return 0;
            InputStream in = conn.getInputStream();
            if (in == null) return 0;
            fo = new FileOutputStream(out);
            byte[] buf = new byte[64 * 1024];
            long got = 0;
            int n;
            while ((n = in.read(buf)) > 0) {
                got += n;
                if (got > maxBytes) {
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
            return 0;
        } finally {
            try { if (fo != null) fo.close(); } catch (Throwable ignore) { }
            if (conn != null) conn.disconnect();
        }
    }
}
