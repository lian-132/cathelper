package com.cathelperawa.owo.app.ai;

import com.cathelperawa.owo.app.api.NetApi;
import com.cathelperawa.owo.app.core.ConfigStore;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public final class AiSearch {

    private AiSearch() {}

    public static final class Result {
        public String title = "";
        public String url = "";
        public String snippet = "";

        @Override public String toString() {
            return title + " — " + snippet + " (" + url + ")";
        }
    }

    public static boolean enabled() {
        try {
            return ConfigStore.getBool("ai_search_on");
        } catch (Throwable t) {
            return false;
        }
    }

    public static String url() {
        String s = ConfigStore.getString("ai_search_url");
        return s == null ? "" : s.trim();
    }

    public static String auth() {
        String s = ConfigStore.getString("ai_search_auth");
        if (s == null || s.trim().length() == 0) return "bearer";
        return s.trim();
    }

    public static String authHeader() {
        String s = ConfigStore.getString("ai_search_header");
        return s == null ? "" : s.trim();
    }

    public static String method() {
        String s = ConfigStore.getString("ai_search_method");
        if (s == null || s.trim().length() == 0) return "post";
        return s.trim();
    }

    public static int limit() {
        int v = ConfigStore.getInt("ai_search_limit");
        if (v <= 0) v = 5;
        return Math.min(v, 10);
    }

    public static void setKey(String plain) {
        String enc = "";
        try {
            enc = AiCrypto.encrypt(plain == null ? "" : plain.trim());
        } catch (Throwable ignore) {
        }
        AiConfig.setString("ai_search_key", enc);
    }

    public static String key() {
        try {
            String enc = ConfigStore.getString("ai_search_key");
            if (enc == null || enc.length() == 0) return "";
            return AiCrypto.decrypt(enc);
        } catch (Throwable t) {
            return "";
        }
    }

    public static boolean configured() {
        return url().length() > 0;
    }

    public static String statusText() {
        if (url().length() == 0) return "未配置 · 点进去填接口地址和 Key";
        String host = url();
        try {
            java.net.URL u = new java.net.URL(url());
            host = u.getHost();
        } catch (Throwable ignore) {
        }
        return host + " · " + method().toUpperCase() + " · 最多 " + limit() + " 条"
                + (key().length() > 0 ? "" : " · 无 Key");
    }

    public static volatile String lastError = "";

    public static List<Result> search(String query) {
        List<Result> out = new ArrayList<Result>();
        lastError = "";
        if (!configured()) {
            lastError = "没填接口地址";
            return out;
        }
        if (query == null || query.trim().length() == 0) {
            lastError = "查询为空";
            return out;
        }

        try {
            if (android.os.Looper.myLooper() != null
                    && android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) {
                lastError = "不能在主线程搜索（要放后台线程）";
                AiDebug.log("  · 搜索失败 " + lastError);
                return out;
            }
        } catch (Throwable ignore) {
        }
        try {
            String q = query.trim();
            String enc = URLEncoder.encode(q, "UTF-8");
            String base = url();
            String target = base;
            String body = null;

            boolean get = "get".equalsIgnoreCase(method());
            if (get) {
                if (base.contains("{query}")) {
                    target = base.replace("{query}", enc);
                } else if (!base.contains("?")) {
                    target = base + "?q=" + enc;
                } else {
                    target = base + "&q=" + enc;
                }
            } else {
                JSONObject b = new JSONObject();

                b.put("query", q);
                b.put("q", q);
                b.put("max_results", limit());
                b.put("count", limit());
                String k = key();
                if (k.length() > 0) b.put("api_key", k);
                body = b.toString();
            }

            JSONObject headers = new JSONObject();
            headers.put("Content-Type", "application/json");
            String k = key();
            String mode = auth();
            if (k.length() > 0) {
                if ("none".equalsIgnoreCase(mode)) {

                } else if ("x-sub".equalsIgnoreCase(mode)) {
                    headers.put("X-Subscription-Token", k);
                } else if ("custom".equalsIgnoreCase(mode)) {
                    String h = authHeader();
                    if (h.length() > 0) headers.put(h, k);
                } else {
                    headers.put("Authorization", "Bearer " + k);
                }
            }
            if (get) headers.remove("Content-Type");

            Resp r = http(target, "post".equalsIgnoreCase(method()) ? "POST" : "GET",
                    headers, body, 15000);
            if (r.code == 0) {
                lastError = "连不上：" + r.body;
                AiDebug.log("  · 搜索失败 " + lastError);
                return out;
            }
            if (r.code < 200 || r.code >= 300) {
                String d = r.body == null ? "" : r.body;
                if (d.length() > 200) d = d.substring(0, 200);
                lastError = "HTTP " + r.code + " " + d;
                AiDebug.log("  · 搜索失败 " + lastError);
                return out;
            }
            out = parse(r.body);
            AiDebug.log("  · 搜索: 「" + q + "」命中 " + out.size() + " 条");
            if (out.isEmpty()) lastError = "返回里没解析出结果";
            return out;
        } catch (Throwable t) {
            lastError = "异常: " + t;
            AiDebug.log("  · 搜索异常 " + t);
            return out;
        }
    }

    private static final class Resp {
        int code;
        String body = "";
    }

    private static Resp http(String url, String method, JSONObject headers,
                             String body, int timeoutMs) {
        Resp r = new Resp();
        java.net.HttpURLConnection c = null;
        try {
            c = (java.net.HttpURLConnection) new java.net.URL(url)
                    .openConnection(java.net.Proxy.NO_PROXY);
            c.setRequestMethod(method);
            c.setConnectTimeout(timeoutMs);
            c.setReadTimeout(timeoutMs);
            c.setInstanceFollowRedirects(true);
            c.setRequestProperty("User-Agent", "CatHelper/1.0 (Android)");
            c.setRequestProperty("Accept", "application/json");
            if (headers != null) {
                java.util.Iterator<String> it = headers.keys();
                while (it.hasNext()) {
                    String k = it.next();
                    c.setRequestProperty(k, headers.optString(k, ""));
                }
            }
            boolean hasBody = body != null && body.length() > 0
                    && !"GET".equalsIgnoreCase(method);
            if (hasBody) {
                byte[] data = body.getBytes("UTF-8");
                c.setDoOutput(true);
                c.setFixedLengthStreamingMode(data.length);
                java.io.OutputStream os = c.getOutputStream();
                os.write(data);
                os.flush();
                os.close();
            }
            r.code = c.getResponseCode();
            java.io.InputStream in = r.code >= 400 ? c.getErrorStream() : c.getInputStream();
            if (in != null) {
                java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
                byte[] buf = new byte[8192];
                int len;
                int total = 0;
                while ((len = in.read(buf)) > 0) {
                    if (total + len > 262144) {
                        bos.write(buf, 0, Math.max(0, 262144 - total));
                        break;
                    }
                    bos.write(buf, 0, len);
                    total += len;
                }
                in.close();
                r.body = new String(bos.toByteArray(), "UTF-8");
            }
        } catch (Throwable t) {
            r.code = 0;
            r.body = t.getClass().getSimpleName() + ": " + t.getMessage();
        } finally {
            if (c != null) {
                try { c.disconnect(); } catch (Throwable ignore) {}
            }
        }
        return r;
    }

    public interface Cb {
        void done(String msg, boolean ok);
    }

    public static void testAsync(final Cb cb) {
        Thread t = new Thread(new Runnable() {
            @Override public void run() {
                final String msg = test();
                final boolean ok = msg.startsWith("成功");
                if (cb != null) {
                    try {
                        cb.done(msg, ok);
                    } catch (Throwable ignore) {
                    }
                }
            }
        }, "cat-ai-search-test");
        t.setDaemon(true);
        try {
            t.start();
        } catch (Throwable t2) {
            if (cb != null) cb.done("起线程失败：" + t2, false);
        }
    }

    public static String test() {
        List<Result> r = search("今天天气");
        if (r.isEmpty()) return "失败：" + (lastError.length() == 0 ? "没结果" : lastError);
        StringBuilder sb = new StringBuilder("成功，拿到 " + r.size() + " 条：\n");
        for (int i = 0; i < Math.min(3, r.size()); i++) {
            Result x = r.get(i);
            String t = x.title.length() > 40 ? x.title.substring(0, 40) + "…" : x.title;
            sb.append(i + 1).append(". ").append(t).append('\n');
        }
        return sb.toString().trim();
    }

    private static List<Result> parse(String body) {
        List<Result> out = new ArrayList<Result>();
        if (body == null || body.length() == 0) return out;
        try {
            JSONObject root = new JSONObject(body);
            JSONArray arr = findArray(root, 0);
            if (arr == null) return out;
            for (int i = 0; i < arr.length() && out.size() < limit(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o == null) continue;
                Result r = new Result();
                r.title = first(o, new String[]{"title", "name", "heading"});
                r.url = first(o, new String[]{"url", "link", "href"});
                r.snippet = first(o, new String[]{"content", "snippet", "description", "text", "summary"});
                if (r.title.length() == 0 && r.url.length() == 0) continue;
                if (r.title.length() > 120) r.title = r.title.substring(0, 120);
                if (r.snippet.length() > 400) r.snippet = r.snippet.substring(0, 400) + "…";
                out.add(r);
            }
        } catch (Throwable ignore) {
        }
        return out;
    }

    private static JSONArray findArray(Object node, int depth) {
        if (node == null || depth > 4) return null;
        if (node instanceof JSONArray) {
            JSONArray a = (JSONArray) node;
            if (a.length() > 0 && a.optJSONObject(0) != null) return a;
            return null;
        }
        if (!(node instanceof JSONObject)) return null;
        JSONObject o = (JSONObject) node;
        String[] prefer = new String[]{"results", "organic", "items", "data", "web", "value"};
        for (int i = 0; i < prefer.length; i++) {
            JSONArray a = findArray(o.opt(prefer[i]), depth + 1);
            if (a != null) return a;
        }
        java.util.Iterator<String> it = o.keys();
        while (it.hasNext()) {
            String k = it.next();
            JSONArray a = findArray(o.opt(k), depth + 1);
            if (a != null) return a;
        }
        return null;
    }

    private static String first(JSONObject o, String[] keys) {
        for (int i = 0; i < keys.length; i++) {
            String v = o.optString(keys[i], "");
            if (v != null && v.trim().length() > 0) return v.trim();
        }
        return "";
    }

    public static String promptFor(List<Result> list) {
        if (list == null || list.isEmpty()) return "";
        StringBuilder sb = new StringBuilder("【刚搜到的资料（可能有错，不确定就说不确定）】");
        for (int i = 0; i < list.size(); i++) {
            Result r = list.get(i);
            sb.append("\n").append(i + 1).append(". ").append(r.title);
            if (r.snippet.length() > 0) sb.append("｜").append(r.snippet);
            if (r.url.length() > 0) sb.append("（").append(r.url).append("）");
        }
        return sb.toString();
    }
}
