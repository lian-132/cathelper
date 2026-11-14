package com.cathelperawa.owo.app.api;

import android.os.Looper;

import com.cathelperawa.owo.app.core.HookLog;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;

public final class NetApi {

    private NetApi() {}

    public static final int DEFAULT_TIMEOUT = 15000;

    public static final int MIN_TIMEOUT = 1000;
    public static final int MAX_TIMEOUT = 120000;

    public static final int MAX_BODY = 4 * 1024 * 1024;

    public static final class Result {

        public final boolean ok;

        public final int code;

        public final String body;

        public final String err;

        public final long costMs;

        Result(boolean ok, int code, String body, String err, long costMs) {
            this.ok = ok;
            this.code = code;
            this.body = body == null ? "" : body;
            this.err = err;
            this.costMs = costMs;
        }

        public boolean is2xx() { return ok && code >= 200 && code < 300; }

        public JSONObject json() {
            try {
                return new JSONObject(body);
            } catch (Throwable t) {
                return null;
            }
        }

        @Override
        public String toString() {
            return (ok ? "OK" : "ERR") + " code=" + code
                    + " " + costMs + "ms"
                    + (err != null ? " err=" + err : "")
                    + " body=" + (body.length() > 200 ? body.substring(0, 200) + "…" : body);
        }
    }

    public interface Callback {
        void onResult(Result r);
    }

    private static volatile ThreadPoolExecutor POOL;

    private static ThreadPoolExecutor pool() {
        if (POOL != null) return POOL;
        synchronized (NetApi.class) {
            if (POOL != null) return POOL;
            ThreadFactory tf = new ThreadFactory() {
                private int n = 1;
                @Override
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r, "cat-net-" + (n++));
                    t.setDaemon(true);
                    return t;
                }
            };
            ThreadPoolExecutor ex = new ThreadPoolExecutor(
                    2, 4, 60, TimeUnit.SECONDS,
                    new ArrayBlockingQueue<Runnable>(32), tf);
            POOL = ex;
            return ex;
        }
    }

    public static void get(String url, String headersJson, Callback cb) {
        request("GET", url, headersJson, null, DEFAULT_TIMEOUT, cb);
    }

    public static void get(String url, String headersJson, int timeoutMs, Callback cb) {
        request("GET", url, headersJson, null, timeoutMs, cb);
    }

    public static void post(String url, String headersJson, String body, Callback cb) {
        request("POST", url, headersJson, body, DEFAULT_TIMEOUT, cb);
    }

    public static void postJson(String url, String headersJson, String json, Callback cb) {
        request("POST", url, headersJson, json, DEFAULT_TIMEOUT, cb);
    }

    public static void request(final String method, final String url,
                               final String headersJson, final String body,
                               final int timeoutMs, final Callback cb) {
        final String bad = checkUrl(url);
        if (bad != null) {
            if (cb != null) cb.onResult(new Result(false, 0, "", bad, 0));
            return;
        }
        final int to = clampTimeout(timeoutMs);
        try {
            pool().execute(new Runnable() {
                @Override
                public void run() {
                    Result r = doRequest(method, url, headersJson, body, to);
                    if (cb != null) {
                        try {
                            cb.onResult(r);
                        } catch (Throwable t) {
                            HookLog.e("[网络] 回调异常", t);
                        }
                    }
                }
            });
        } catch (Throwable t) {
            HookLog.e("[网络] 提交任务失败", t);
            if (cb != null) cb.onResult(new Result(false, 0, "", "线程池拒绝: " + t, 0));
        }
    }

    public static Result getSync(String url, String headersJson, int timeoutMs) {
        String bad = checkUrl(url);
        if (bad != null) return new Result(false, 0, "", bad, 0);
        return doRequest("GET", url, headersJson, null, clampTimeout(timeoutMs));
    }

    public static Result postSync(String url, String headersJson, String body, int timeoutMs) {
        String bad = checkUrl(url);
        if (bad != null) return new Result(false, 0, "", bad, 0);
        return doRequest("POST", url, headersJson, body, clampTimeout(timeoutMs));
    }

    private static Result doRequest(String method, String url, String headersJson,
                                    String body, int timeoutMs) {
        long t0 = System.currentTimeMillis();

        try {
            if (Looper.myLooper() != null && Looper.myLooper() == Looper.getMainLooper()) {
                return new Result(false, 0, "", "禁止在主线程发起同步请求（用异步版）", 0);
            }
        } catch (Throwable ignore) {
        }
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            String m = (method == null || method.isEmpty()) ? "GET" : method.toUpperCase();
            try {
                conn.setRequestMethod(m);
            } catch (java.net.ProtocolException pe) {

                conn.setRequestMethod("POST");
                conn.setRequestProperty("X-HTTP-Method-Override", m);
            }
            conn.setConnectTimeout(timeoutMs);
            conn.setReadTimeout(timeoutMs);
            conn.setInstanceFollowRedirects(true);
            conn.setRequestProperty("User-Agent", "CatHelper/1.0 (Android)");
            conn.setRequestProperty("Accept-Encoding", "gzip");

            boolean hasContentType = false;
            if (headersJson != null && headersJson.trim().length() > 0) {
                JSONObject h = new JSONObject(headersJson);
                Iterator<String> it = h.keys();
                while (it.hasNext()) {
                    String k = it.next();
                    String v = h.optString(k, "");
                    conn.setRequestProperty(k, v);
                    if ("Content-Type".equalsIgnoreCase(k)) hasContentType = true;
                }
            }

            boolean hasBody = body != null
                    && !"GET".equalsIgnoreCase(m)
                    && !"HEAD".equalsIgnoreCase(m);

            if (hasBody) {
                if (!hasContentType) {
                    conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                }
                byte[] data = body.getBytes("UTF-8");
                conn.setDoOutput(true);
                conn.setFixedLengthStreamingMode(data.length);
                OutputStream os = conn.getOutputStream();
                try {
                    os.write(data);
                    os.flush();
                } finally {
                    try { os.close(); } catch (Throwable ignore) {}
                }
            } else {
                conn.setDoOutput(false);
            }

            int code = conn.getResponseCode();
            InputStream in = (code >= 400) ? conn.getErrorStream() : conn.getInputStream();
            if (in != null) {
                String enc = conn.getContentEncoding();
                if (enc != null && enc.toLowerCase().contains("gzip")) {
                    in = new GZIPInputStream(in);
                }
            }
            String text = readAll(in);
            long cost = System.currentTimeMillis() - t0;
            return new Result(code > 0, code, text, null, cost);
        } catch (Throwable t) {
            long cost = System.currentTimeMillis() - t0;
            return new Result(false, 0, "", t.getClass().getSimpleName() + ": " + t.getMessage(), cost);
        } finally {
            if (conn != null) {
                try { conn.disconnect(); } catch (Throwable ignore) {}
            }
        }
    }

    private static String readAll(InputStream in) throws Exception {
        if (in == null) return "";
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        int total = 0;
        boolean truncated = false;
        try {
            while ((n = in.read(buf)) > 0) {
                if (total + n > MAX_BODY) {
                    bos.write(buf, 0, Math.max(0, MAX_BODY - total));
                    truncated = true;
                    break;
                }
                bos.write(buf, 0, n);
                total += n;
            }
        } finally {
            try { in.close(); } catch (Throwable ignore) {}
        }
        String s = new String(bos.toByteArray(), "UTF-8");
        return truncated ? s + "\n[CatHelper] 响应超过 " + MAX_BODY + " 字节，已截断" : s;
    }

    private static String checkUrl(String url) {
        if (url == null || url.trim().isEmpty()) return "url 为空";
        String u = url.trim().toLowerCase();
        if (!u.startsWith("http://") && !u.startsWith("https://")) {
            return "只支持 http/https";
        }
        return null;
    }

    private static int clampTimeout(int ms) {
        if (ms <= 0) return DEFAULT_TIMEOUT;
        if (ms < MIN_TIMEOUT) return MIN_TIMEOUT;
        if (ms > MAX_TIMEOUT) return MAX_TIMEOUT;
        return ms;
    }

    public static Map<String, String> parseHeaders(String headersJson) {
        Map<String, String> map = new java.util.LinkedHashMap<String, String>();
        if (headersJson == null || headersJson.trim().isEmpty()) return map;
        try {
            JSONObject h = new JSONObject(headersJson);
            Iterator<String> it = h.keys();
            while (it.hasNext()) {
                String k = it.next();
                map.put(k, h.optString(k, ""));
            }
        } catch (Throwable ignore) {
        }
        return map;
    }

    public static int pending() {
        ThreadPoolExecutor ex = POOL;
        return ex == null ? 0 : ex.getQueue().size() + ex.getActiveCount();
    }
}
