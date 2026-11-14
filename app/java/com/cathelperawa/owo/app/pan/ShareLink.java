package com.cathelperawa.owo.app.pan;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ShareLink {

    public final String platform;

    public final String shareId;

    public final String pwd;

    public final String url;

    private ShareLink(String platform, String shareId, String pwd, String url) {
        this.platform = platform;
        this.shareId = shareId;
        this.pwd = pwd == null ? "" : pwd;
        this.url = url == null ? "" : url;
    }

    private static final Pattern URL = Pattern.compile("https?://[^\\s]+");
    private static final Pattern QUARK = Pattern.compile("pan\\.quark\\.cn/s/([A-Za-z0-9]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern UC = Pattern.compile("drive\\.uc\\.cn/s/([A-Za-z0-9]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern XUNLEI = Pattern.compile("pan\\.xunlei\\.com/s/([A-Za-z0-9_-]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern BAIDU = Pattern.compile("pan\\.baidu\\.com/s/(1[A-Za-z0-9_-]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern C139 = Pattern.compile("yun\\.139\\.com/shareweb/.*?/w/i/([A-Za-z0-9_-]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern PAN123 = Pattern.compile("123(?:865|pan)\\.(?:com|cn)/s/([A-Za-z0-9]+-[A-Za-z0-9]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern PAN123_SUB = Pattern.compile("share\\.123pan\\.cn/123pan/([A-Za-z0-9-]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern PAN123_SRR = Pattern.compile("api/srr\\?sk=([A-Za-z0-9-]+)", Pattern.CASE_INSENSITIVE);

    private static final Pattern LANZOU = Pattern.compile(
            "(?:[a-z0-9-]+\\.)*(?:lanzou[a-z]?|lan[zs]o[ux])\\.(?:com|net|org|cn)/",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern PWD_IN_URL = Pattern.compile("[?&]pwd=([A-Za-z0-9]+)");
    private static final Pattern PWD_IN_TEXT = Pattern.compile("(?:提取码|访问码|密码)[：:]\\s*([A-Za-z0-9]{4,8})");

    private static final char[] TRIM = {'。', '，', ',', '；', ';', ')', ']', '}', '"', '\''};

    public static ShareLink parse(String text) {
        if (text == null) return null;
        String raw = text.trim();
        if (raw.length() == 0) return null;
        Matcher um = URL.matcher(raw);
        if (!um.find()) return null;
        String url = um.group();
        for (char c : TRIM) {
            while (url.length() > 0 && url.charAt(url.length() - 1) == c) {
                url = url.substring(0, url.length() - 1);
            }
        }
        String pwd = pwdOf(url, raw);

        if (LANZOU.matcher(url).find()) {
            return new ShareLink("lanzou", url, pwd, url);
        }

        String[] ids = {
                match(QUARK, url), match(UC, url), match(XUNLEI, url), match(C139, url),
                match(PAN123, url), match(PAN123_SUB, url), match(PAN123_SRR, url), match(BAIDU, url)
        };
        String[] platforms = {"quark", "uc", "xunlei", "c139", "pan123", "pan123", "pan123", "baidu"};
        for (int i = 0; i < ids.length; i++) {
            if (ids[i] == null) continue;
            String sid = ids[i];

            if ("baidu".equals(platforms[i])) sid = sid.startsWith("1") ? sid.substring(1) : sid;
            return new ShareLink(platforms[i], sid, pwd, url);
        }
        return null;
    }

    private static String pwdOf(String url, String text) {
        Matcher m = PWD_IN_URL.matcher(url);
        if (m.find()) return m.group(1);
        m = PWD_IN_TEXT.matcher(text);
        if (m.find()) return m.group(1);
        return "";
    }

    private static String match(Pattern p, String url) {
        Matcher m = p.matcher(url);
        return m.find() ? m.group(1) : null;
    }
}
