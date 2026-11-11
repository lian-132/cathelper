package com.cathelperawa.owo.app.feature;

import com.cathelperawa.owo.app.bili.BiliApi;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class VideoSource {

    public String url = "";
    public String title = "";
    public String cover = "";
    public String share = "";
    public String error = "";

    private static final Pattern LINK = Pattern.compile("https?://[^\\s\"'<>，。]+");

    private static final Pattern P_PLAY = Pattern.compile(
            "\"(?:playAddr|play_addr|video_url|videoUrl|main_url|masterUrl|playUrl)\"\\s*:\\s*\"([^\"]{10,800})\"",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern P_OG_VIDEO = Pattern.compile(
            "<meta[^>]+property=[\"']og:video(?::secure_url|:url)?[\"'][^>]*content=[\"']([^\"']+)[\"']",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern P_OG_VIDEO_REV = Pattern.compile(
            "<meta[^>]+content=[\"']([^\"']+)[\"'][^>]*property=[\"']og:video",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern P_OG_IMAGE = Pattern.compile(
            "<meta[^>]+property=[\"']og:image[\"'][^>]*content=[\"']([^\"']+)[\"']",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern P_OG_TITLE = Pattern.compile(
            "<meta[^>]+property=[\"']og:title[\"'][^>]*content=[\"']([^\"']+)[\"']",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern P_TITLE = Pattern.compile(
            "<title>([^<]{1,150})</title>", Pattern.CASE_INSENSITIVE);
    private static final Pattern P_VIDEO_SRC = Pattern.compile(
            "<video[^>]+src=[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);

    private VideoSource() {
    }

    public static VideoSource resolve(String input, String ua) {
        VideoSource r = new VideoSource();
        try {
            Matcher m = LINK.matcher(input == null ? "" : input);
            if (!m.find()) {
                r.error = "没找到链接";
                return r;
            }
            r.share = m.group();

            if (r.share.contains("douyin.com") || r.share.contains("iesdouyin.com")) {
                VideoSource dy = resolveDouyin(r.share, ua);
                if (dy.url.length() > 0) return dy;
                r.error = dy.error;
                return r;
            }

            if (looksLikeDirectVideo(r.share)) {
                r.url = r.share;
                r.title = fileNameTitle(r.share);
                return r;
            }

            String real = followRedirect(r.share, ua);
            if (real == null) real = r.share;

            String ct = contentType(real, ua);
            if (ct != null && (ct.startsWith("video/") || ct.contains("octet-stream"))) {
                r.url = real;
                r.title = fileNameTitle(real);
                r.share = real;
                return r;
            }

            String html = getText(real, ua);
            if (html == null || html.length() == 0) {
                r.error = "页面打不开";
                return r;
            }

            r.title = pick(P_OG_TITLE, html);
            if (r.title.length() == 0) r.title = pick(P_TITLE, html);
            r.cover = pick(P_OG_IMAGE, html);

            String v = pick(P_PLAY, html);
            if (v.length() == 0) v = pick(P_OG_VIDEO, html);
            if (v.length() == 0) v = pick(P_OG_VIDEO_REV, html);
            if (v.length() == 0) v = pick(P_VIDEO_SRC, html);

            v = unescape(v);
            if (v.length() == 0) {
                r.error = "页面里没找到视频地址（可能需要登录或该平台不支持）";
                return r;
            }
            if (v.startsWith("//")) v = "https:" + v;
            r.url = v;
            if (r.title.length() == 0) r.title = fileNameTitle(v);
            return r;
        } catch (Throwable t) {
            r.error = String.valueOf(t);
            return r;
        }
    }

    
    private static VideoSource resolveDouyin(String link, String ua) {
        VideoSource r = new VideoSource();
        r.share = link;
        try {
            String api = "https://api.xingzhige.com/API/douyin/?url="
                    + java.net.URLEncoder.encode(link, "UTF-8");
            String text = getText(api, ua);
            if (text == null || text.length() == 0) {
                r.error = "抖音解析服务无响应";
                return r;
            }
            org.json.JSONObject o = new org.json.JSONObject(text);
            if (o.optInt("code", -1) != 0) {
                r.error = "抖音解析失败：" + o.optString("msg", "未知");
                return r;
            }
            org.json.JSONObject data = o.optJSONObject("data");
            if (data == null) {
                r.error = "抖音解析返回空";
                return r;
            }
            org.json.JSONObject item = data.optJSONObject("item");
            if (item == null) {
                r.error = "抖音解析缺少 item";
                return r;
            }
            r.url = unescape(item.optString("url", ""));
            r.title = item.optString("title", "");
            r.cover = unescape(item.optString("cover", ""));
            if (r.title.length() == 0) {
                org.json.JSONObject author = data.optJSONObject("author");
                if (author != null) r.title = author.optString("name", "");
            }
            if (r.url.length() == 0) {
                r.error = "抖音没解析出地址";
            }
            return r;
        } catch (Throwable t) {
            r.error = "抖音解析异常：" + t;
            return r;
        }
    }

    
    private static boolean looksLikeDirectVideo(String u) {
        if (u == null || u.length() == 0) return false;
        String s = u.toLowerCase();
        if (s.contains("multimedia.nt.qq.com.cn")) return true;
        if (s.contains(".mp4")) return true;
        if (s.contains(".m3u8")) return true;
        if (s.contains("douyinvod.com")) return true;
        if (s.contains("bilivideo.com")) return true;
        if (s.contains("qq.com/download?")) return true;
        if (s.contains("gchat.qpic.cn/download")) return true;
        return false;
    }

    private static String pick(Pattern p, String s) {
        try {
            Matcher m = p.matcher(s);
            if (m.find()) {
                String v = m.group(1);
                return v == null ? "" : v.trim();
            }
        } catch (Throwable ignore) {
        }
        return "";
    }

    private static String unescape(String s) {
        if (s == null) return "";
        return s.replace("\\u002F", "/")
                .replace("\\u002f", "/")
                .replace("\\/", "/")
                .replace("\\u0026", "&")
                .replace("&amp;", "&");
    }

    private static String fileNameTitle(String url) {
        try {
            String s = url;
            int q = s.indexOf('?');
            if (q > 0) s = s.substring(0, q);
            int slash = s.lastIndexOf('/');
            if (slash >= 0) s = s.substring(slash + 1);
            int dot = s.lastIndexOf('.');
            if (dot > 0) s = s.substring(0, dot);
            return s.length() > 60 ? s.substring(0, 60) : s;
        } catch (Throwable t) {
            return "";
        }
    }

    private static String followRedirect(String url, String ua) {
        String cur = url;
        for (int i = 0; i < 4; i++) {
            HttpURLConnection c = null;
            try {
                c = (HttpURLConnection) new URL(cur).openConnection();
                c.setInstanceFollowRedirects(false);
                c.setConnectTimeout(8000);
                c.setReadTimeout(8000);
                c.setRequestProperty("User-Agent", ua);
                int code = c.getResponseCode();
                String loc = c.getHeaderField("Location");
                if (code >= 300 && code < 400 && loc != null) {
                    cur = loc.startsWith("http") ? loc : new URL(new URL(cur), loc).toString();
                    continue;
                }
                return cur;
            } catch (Throwable t) {
                return null;
            } finally {
                if (c != null) c.disconnect();
            }
        }
        return cur;
    }

    private static String contentType(String url, String ua) {
        HttpURLConnection c = null;
        try {
            c = (HttpURLConnection) new URL(url).openConnection();
            c.setRequestMethod("HEAD");
            c.setConnectTimeout(8000);
            c.setReadTimeout(8000);
            c.setInstanceFollowRedirects(true);
            c.setRequestProperty("User-Agent", ua);
            return c.getContentType();
        } catch (Throwable t) {
            return null;
        } finally {
            if (c != null) c.disconnect();
        }
    }

    private static String getText(String url, String ua) {
        HttpURLConnection c = null;
        try {
            c = (HttpURLConnection) new URL(url).openConnection();
            c.setConnectTimeout(10000);
            c.setReadTimeout(15000);
            c.setInstanceFollowRedirects(true);
            c.setRequestProperty("User-Agent", ua);
            c.setRequestProperty("Accept", "text/html,application/xhtml+xml,*/*");
            c.setRequestProperty("Accept-Language", "zh-CN,zh;q=0.9");
            InputStream in = c.getInputStream();
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n, total = 0;
            while ((n = in.read(buf)) > 0) {
                bos.write(buf, 0, n);
                total += n;
                if (total > 600 * 1024) break;   
            }
            in.close();
            byte[] b = bos.toByteArray();
            String cs = null;
            try {
                cs = c.getContentType();
            } catch (Throwable ignore) {
            }
            if (cs != null && cs.toLowerCase().contains("gbk")) {
                return new String(b, "GBK");
            }
            return new String(b, "UTF-8");
        } catch (Throwable t) {
            return null;
        } finally {
            if (c != null) c.disconnect();
        }
    }
}
