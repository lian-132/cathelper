package com.cathelperawa.owo.app.feature;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DouyinWeb {

    public interface Callback {
        void done(String url, String title, String cover, String error);
    }

    public static final class Result {
        public String url = "";
        public String title = "";
        public String cover = "";
        public String error = "";

        public String apiUrl = "";
        public boolean ok() { return url.length() > 0 && error.length() == 0; }
    }

    public static Result resolveSync(final Activity act, final String link, long timeoutMs) {
        final Result r = new Result();
        if (act == null) {
            r.error = "没有界面上下文";
            return r;
        }
        final java.util.concurrent.CountDownLatch latch =
                new java.util.concurrent.CountDownLatch(1);
        Callback cb = new Callback() {
            @Override public void done(String url, String title, String cover, String error) {
                r.url = nz(url);
                r.title = nz(title);
                r.cover = nz(cover);
                r.error = nz(error);
                latch.countDown();
            }
        };
        try {
            act.runOnUiThread(new Runnable() {
                @Override public void run() {
                    try {
                        resolve(act, link, cb);
                    } catch (Throwable t) {
                        r.error = "抖音解析异常：" + t;
                        latch.countDown();
                    }
                }
            });
            if (!latch.await(timeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS)) {
                r.error = "抖音解析超时（页面没给出播放地址）";
            }
        } catch (Throwable t) {
            r.error = "抖音解析异常：" + t;
        }
        if (r.ok()) {
            String[] p = pickPlay(r.url);
            if (p != null) {
                r.url = p[0];
                r.apiUrl = p[1];
            }
        }
        return r;
    }

    private static String nz(String s) { return s == null ? "" : s; }

    private static final String UA =
            "Mozilla/5.0 (Linux; Android 13; Pixel 6) AppleWebKit/537.36 "
            + "(KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36";

    private static final Pattern P_VIDEO = Pattern.compile("/video/(\\d{6,})");
    private static final Pattern P_MODAL = Pattern.compile("modal_id=(\\d{6,})");
    private static final Pattern P_BARE_ID = Pattern.compile("(?<!\\d)(\\d{19})(?!\\d)");
    private static final Pattern P_TITLE_SUFFIX = Pattern.compile("\\s*-\\s*抖音\\s*$");

    private static final Pattern P_URL = Pattern.compile("https?://[^\\s\"'<>，。,]+");

    private static final Pattern P_BARE_HOST =
            Pattern.compile("(?:[a-z0-9]+\\.)?(?:d)?ouyin\\.com/([A-Za-z0-9_\\-]{5,})");

    private static final String JS =
            "(function(){try{"
            + "var h=(document.documentElement.outerHTML||'');"
            + "var m=h.match(/\"play_addr\":\\{\"uri\":\"([^\"]+)\"/);"
            + "if(m&&m[1])return 'VM'+m[1];"
            + "var v=document.querySelector('video');"
            + "if(v&&v.src&&v.src.indexOf('blob:')<0)return 'SRC'+v.src;"
            + "var v3=h.match(/(v0[0-9a-z]{10,})/);"
            + "if(v3&&v3[1])return 'VM'+v3[1];"
            + "var d=h.match(/https?:[^\"'<>\\s]{10,220}(?:douyinvod\\.com|\\.mp4)/);"
            + "if(d&&d[0])return 'RAW'+d[0].replace(/\\\\u002F/g,'/').replace(/\\\\\\//g,'/');"
            + "}catch(e){}return '';})()";

    private static final String JS_DIAG =
            "(function(){try{"
            + "var h=(document.documentElement.outerHTML||'');"
            + "var t=document.title||'';"
            + "var b=(document.body&&document.body.innerText?document.body.innerText:'').replace(/\\s+/g,' ').slice(0,60);"
            + "return (location.href||'')+' | t='+t+' | len='+h.length+' | pa='+h.indexOf('play_addr')+' | '+b;"
            + "}catch(e){return 'diag-fail';}})()";

    private static final String JS_COVER =
            "(function(){try{var h=document.documentElement.innerHTML;"
            + "var c=h.match(/\"cover\":\\{\"uri\":\"[^\"]*\",\"url_list\":\\[\"([^\"]+)\"/);"
            + "if(c&&c[1])return c[1].replace(/\\\\u002F/g,'/').replace(/\\\\\\//g,'/');"
            + "}catch(e){}return '';})()";

    private static String[] pickPlay(String url) {
        if (url == null || url.indexOf("aweme/v1/play/") < 0) return null;
        Matcher m = Pattern.compile("video_id=([^&]+)").matcher(url);
        if (!m.find()) return null;
        String uri = clean2(m.group(1));
        String[] ratios = {"1080p", "720p", "540p"};
        for (String r : ratios) {
            String u = "https://aweme.snssdk.com/aweme/v1/play/?video_id=" + uri
                    + "&ratio=" + r + "&line=0";
            String fin = probeFinal(u);
            if (fin != null && fin.length() > 0) return new String[]{fin, u};
        }
        return null;
    }

    private static String probeFinal(String url) {
        HttpURLConnection c = null;
        java.io.InputStream in = null;
        try {
            c = (HttpURLConnection) new URL(url).openConnection();
            c.setInstanceFollowRedirects(true);
            c.setConnectTimeout(6000);
            c.setReadTimeout(6000);
            c.setRequestMethod("GET");
            c.setRequestProperty("User-Agent", UA);
            c.setRequestProperty("Referer", "https://www.douyin.com/");
            c.setRequestProperty("Range", "bytes=0-0");
            int code = c.getResponseCode();
            if (code < 200 || code >= 400) return null;
            String ct = c.getContentType();
            if (ct != null && ct.indexOf("video") < 0 && ct.indexOf("octet") < 0
                    && ct.indexOf("mp4") < 0) {
                return null;
            }
            in = c.getInputStream();
            String fin = c.getURL() == null ? url : c.getURL().toString();
            return fin;
        } catch (Throwable t) {
            return null;
        } finally {
            try { if (in != null) in.close(); } catch (Throwable ignored) {}
            if (c != null) try { c.disconnect(); } catch (Throwable ignored) {}
        }
    }

    private static String clean2(String s) {
        if (s == null) return "";
        return s.replace("\\u002F", "/").replace("\\/", "/");
    }

    private DouyinWeb() {
    }

    public static boolean looksDouyin(String s) {
        return s != null && (s.contains("douyin.com") || s.contains("iesdouyin.com"));
    }

    public static void resolve(final Activity act, final String link, final Callback cb) {
        if (act == null) {
            cb.done("", "", "", "没有界面上下文");
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                String id;
                try {
                    id = findId(link);
                } catch (Throwable t) {
                    id = null;
                }
                if (id == null) {
                    final String msg = "没从链接里找到视频 id";
                    act.runOnUiThread(new Runnable() {
                        @Override public void run() { cb.done("", "", "", msg); }
                    });
                    return;
                }
                final String vid = id;
                act.runOnUiThread(new Runnable() {
                    @Override public void run() {
                        start(act, vid, cb);
                    }
                });
            }
        }, "dy-id").start();
    }

    private static String findId(String link) {
        if (link == null) return null;

        String url = null;
        Matcher mu = P_URL.matcher(link);
        if (mu.find()) {
            url = mu.group();
        } else {
            Matcher mb = P_BARE_HOST.matcher(link);
            if (mb.find()) {
                url = "https://v.douyin.com/" + mb.group(1) + "/";
            }
        }
        if (url != null) {
            Matcher m = P_VIDEO.matcher(url);
            if (m.find()) return m.group(1);
            m = P_MODAL.matcher(url);
            if (m.find()) return m.group(1);
            String loc = follow(url);
            if (loc != null) {
                m = P_VIDEO.matcher(loc);
                if (m.find()) return m.group(1);
                m = P_MODAL.matcher(loc);
                if (m.find()) return m.group(1);
                Matcher m2 = P_BARE_ID.matcher(loc);
                if (m2.find()) return m2.group(1);
            }
        }

        Matcher m = P_BARE_ID.matcher(link);
        if (m.find()) return m.group(1);
        return null;
    }

    private static String follow(String link) {
        HttpURLConnection c = null;
        try {
            URL u = new URL(link);
            c = (HttpURLConnection) u.openConnection();
            c.setInstanceFollowRedirects(false);
            c.setConnectTimeout(8000);
            c.setReadTimeout(10000);
            c.setRequestProperty("User-Agent", UA);
            c.setRequestProperty("Accept", "*/*");
            int code = c.getResponseCode();
            String loc = c.getHeaderField("Location");
            if (loc != null) {
                if (loc.startsWith("//")) loc = "https:" + loc;
                if (!loc.startsWith("http")) {

                    return null;
                }

                if (code >= 300 && code < 400 && !P_VIDEO.matcher(loc).find()) {
                    String loc2 = follow(loc);
                    if (loc2 != null) return loc2;
                }
                return loc;
            }
        } catch (Throwable ignored) {
        } finally {
            if (c != null) try { c.disconnect(); } catch (Throwable ignored) {}
        }
        return null;
    }

    private static void start(final Activity act, final String vid, final Callback cb) {
        final Handler ui = new Handler(Looper.getMainLooper());
        final WebView wv;
        try {
            wv = new WebView(act);
        } catch (Throwable t) {
            cb.done("", "", "", "WebView 起不来：" + t);
            return;
        }
        try {
            WebSettings s = wv.getSettings();
            s.setJavaScriptEnabled(true);
            s.setDomStorageEnabled(true);
            s.setDatabaseEnabled(true);
            s.setJavaScriptCanOpenWindowsAutomatically(true);
            s.setUseWideViewPort(true);
            s.setLoadWithOverviewMode(true);
            s.setMediaPlaybackRequiresUserGesture(false);
            s.setBlockNetworkImage(false);
            s.setLoadsImagesAutomatically(true);
            s.setCacheMode(WebSettings.LOAD_NO_CACHE);
            s.setUserAgentString(UA);
            try {
                android.webkit.CookieManager.getInstance().setAcceptCookie(true);
                android.webkit.CookieManager.getInstance().setAcceptThirdPartyCookies(wv, true);
            } catch (Throwable ignore) {
            }
        } catch (Throwable ignored) {
        }

        try {
            wv.setAlpha(0f);
            android.widget.FrameLayout.LayoutParams lp =
                    new android.widget.FrameLayout.LayoutParams(1, 1);
            act.addContentView(wv, lp);
        } catch (Throwable ignored) {
        }
        wv.setWebViewClient(new WebViewClient());

        final boolean[] fired = {false};
        final int[] tries = {0};
        final Runnable[] pollRef = new Runnable[1];

        final Runnable poll = new Runnable() {
            @Override
            public void run() {
                if (fired[0]) return;
                tries[0]++;
                try {
                    wv.evaluateJavascript(JS, new android.webkit.ValueCallback<String>() {
                        @Override
                        public void onReceiveValue(String value) {
                            if (fired[0]) return;
                            String v = clean(value);
                            if (v.length() > 2) {
                                String url;
                                if (v.startsWith("VM")) {
                                    String uri = v.substring(2);
                                    url = "https://aweme.snssdk.com/aweme/v1/play/?video_id="
                                            + uri + "&ratio=1080p&line=0";
                                } else if (v.startsWith("SRC")) {
                                    url = v.substring(3);
                                } else if (v.startsWith("RAW")) {
                                    url = v.substring(3);
                                } else {
                                    return;
                                }
                                fired[0] = true;
                                finish(wv, url, act, cb);
                                return;
                            }
                            if (tries[0] < 18) {
                                ui.postDelayed(pollRef[0], 800);
                            } else {
                                fired[0] = true;
                                try {
                                    wv.evaluateJavascript(JS_DIAG,
                                            new android.webkit.ValueCallback<String>() {
                                        @Override public void onReceiveValue(String d) {
                                            destroy(wv);
                                            cb.done("", "", "", "页面没数据 [" + clean(d) + "]");
                                        }
                                    });
                                } catch (Throwable t) {
                                    destroy(wv);
                                    cb.done("", "", "", "抖音页面没解析出地址");
                                }
                            }
                        }
                    });
                } catch (Throwable t) {
                    if (!fired[0]) {
                        fired[0] = true;
                        destroy(wv);
                        cb.done("", "", "", "抖音解析异常：" + t);
                    }
                }
            }
        };

        try {
            wv.loadUrl("https://m.douyin.com/share/video/" + vid);
        } catch (Throwable t) {
            destroy(wv);
            cb.done("", "", "", "抖音页面打不开：" + t);
            return;
        }
        pollRef[0] = poll;
        ui.postDelayed(poll, 1500);
    }

    private static void finish(final WebView wv, final String url, Activity act,
                               final Callback cb) {
        String title = "";
        try {
            title = wv.getTitle();
        } catch (Throwable ignored) {
        }
        title = title == null ? "" : P_TITLE_SUFFIX.matcher(title).replaceAll("").trim();
        final String t2 = title;
        String cover = "";
        try {
            wv.evaluateJavascript(JS_COVER, new android.webkit.ValueCallback<String>() {
                @Override
                public void onReceiveValue(String value) {
                    String cv = clean(value);
                    destroy(wv);
                    cb.done(url, t2, cv, "");
                }
            });
            return;
        } catch (Throwable ignored) {
        }
        destroy(wv);
        cb.done(url, t2, cover, "");
    }

    private static void destroy(WebView wv) {
        try {
            android.view.ViewGroup p = (android.view.ViewGroup) wv.getParent();
            if (p != null) p.removeView(wv);
        } catch (Throwable ignored) {
        }
        try {
            wv.stopLoading();
            wv.loadUrl("about:blank");
            wv.destroy();
        } catch (Throwable ignored) {
        }
    }

    private static String clean(String json) {
        if (json == null) return "";
        String s = json.trim();
        if (s.length() >= 2 && s.charAt(0) == '"' && s.charAt(s.length() - 1) == '"') {
            s = s.substring(1, s.length() - 1);
        }
        s = s.replace("\\u002F", "/").replace("\\/", "/").replace("\\\"", "\"");
        if ("null".equals(s)) return "";
        return s;
    }
}
