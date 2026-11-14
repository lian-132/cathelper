package com.cathelperawa.owo.app.pan;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PanHttp {

    private PanHttp() {}

    public static final int TIMEOUT = 20000;

    public static final class Resp {
        public final int code;
        public final String body;
        public final List<String> setCookies;

        Resp(int code, String body, List<String> setCookies) {
            this.code = code;
            this.body = body == null ? "" : body;
            this.setCookies = setCookies == null ? new ArrayList<String>() : setCookies;
        }
    }

    public static Map<String, String> headers(String... kv) {
        Map<String, String> m = new LinkedHashMap<String, String>();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            if (kv[i] != null && kv[i + 1] != null) m.put(kv[i], kv[i + 1]);
        }
        return m;
    }

    public static Resp get(String url, Map<String, String> headers) throws IOException {
        return call(url, "GET", null, headers);
    }

    public static Resp post(String url, String json, Map<String, String> headers) throws IOException {
        return call(url, "POST", json, headers);
    }

    public static Resp call(String url, String method, String jsonBody, Map<String, String> headers)
            throws IOException {
        HttpURLConnection c = null;
        try {
            c = (HttpURLConnection) new URL(url).openConnection();
            c.setConnectTimeout(TIMEOUT);
            c.setReadTimeout(TIMEOUT);
            c.setRequestMethod(method);
            c.setInstanceFollowRedirects(true);

            c.setRequestProperty("Accept-Encoding", "identity");
            c.setRequestProperty("Accept", "application/json, text/plain, */*");
            if (headers != null) {
                for (Map.Entry<String, String> e : headers.entrySet()) {
                    if (e.getKey() != null && e.getValue() != null) {
                        c.setRequestProperty(e.getKey(), e.getValue());
                    }
                }
            }
            if ("POST".equals(method) && jsonBody != null) {
                byte[] body = jsonBody.getBytes("UTF-8");
                c.setDoOutput(true);
                if (c.getRequestProperty("Content-Type") == null) {
                    c.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
                }
                c.setFixedLengthStreamingMode(body.length);
                OutputStream os = c.getOutputStream();
                os.write(body);
                os.flush();
                os.close();
            }
            int code = c.getResponseCode();
            List<String> setCookies = new ArrayList<String>();
            for (Map.Entry<String, List<String>> e : c.getHeaderFields().entrySet()) {
                if (e.getKey() != null && "Set-Cookie".equalsIgnoreCase(e.getKey()) && e.getValue() != null) {
                    setCookies.addAll(e.getValue());
                }
            }
            InputStream in = null;
            try {
                in = code >= 400 ? c.getErrorStream() : c.getInputStream();
            } catch (IOException ignore) {
                in = c.getErrorStream();
            }
            return new Resp(code, readAll(in), setCookies);
        } finally {
            if (c != null) {
                try {
                    c.disconnect();
                } catch (Throwable ignore) {
                }
            }
        }
    }

    private static String readAll(InputStream in) {
        if (in == null) return "";
        try {
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) bo.write(buf, 0, n);
            return new String(bo.toByteArray(), "UTF-8");
        } catch (Throwable t) {
            return "";
        } finally {
            try {
                in.close();
            } catch (Throwable ignore) {
            }
        }
    }

    public static String pickSetCookie(List<String> setCookies, String name) {
        if (setCookies == null) return "";
        String prefix = name + "=";
        for (String sc : setCookies) {
            if (sc == null) continue;
            String first = sc.trim();
            int semi = first.indexOf(';');
            if (semi >= 0) first = first.substring(0, semi);
            if (first.startsWith(prefix) && first.length() > prefix.length()) return first;
        }
        return "";
    }

    public static String merge(String original, List<String> setCookies) {
        if (setCookies == null || setCookies.isEmpty()) return original == null ? "" : original;
        Map<String, String> jar = new LinkedHashMap<String, String>();
        List<String> order = new ArrayList<String>();
        if (original != null) {
            for (String part : original.split(";")) {
                String p = part.trim();
                if (p.length() == 0) continue;
                int eq = p.indexOf('=');
                if (eq <= 0) continue;
                String k = p.substring(0, eq);
                if (!jar.containsKey(k)) order.add(k);
                jar.put(k, p.substring(eq + 1));
            }
        }
        for (String sc : setCookies) {
            if (sc == null) continue;
            String first = sc.trim();
            int semi = first.indexOf(';');
            if (semi >= 0) first = first.substring(0, semi);
            int eq = first.indexOf('=');
            if (eq <= 0) continue;
            String k = first.substring(0, eq);
            String v = first.substring(eq + 1);
            if (v.length() == 0) continue;
            if (!jar.containsKey(k)) order.add(k);
            jar.put(k, v);
        }
        StringBuilder sb = new StringBuilder();
        for (String k : order) {
            if (sb.length() > 0) sb.append("; ");
            sb.append(k).append('=').append(jar.get(k));
        }
        return sb.toString();
    }

    public static boolean hasCookie(String cookie, String name) {
        if (cookie == null || cookie.length() == 0) return false;
        String prefix = name + "=";
        for (String part : cookie.split(";")) {
            String p = part.trim();
            if (p.startsWith(prefix) && p.length() > prefix.length()) return true;
        }
        return false;
    }
}
