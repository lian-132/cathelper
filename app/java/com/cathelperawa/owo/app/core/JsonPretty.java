package com.cathelperawa.owo.app.core;

import com.cathelperawa.owo.app.api.SendProtoApi;

public final class JsonPretty {

    private JsonPretty() {
    }

    public static final class Tidied {
        public final String text;
        public final String note;
        public final boolean ok;

        Tidied(String text, String note, boolean ok) {
            this.text = text;
            this.note = note;
            this.ok = ok;
        }
    }

    public static Tidied tidy(String input, boolean pbMode) {
        String s = input == null ? "" : input.trim();
        if (s.length() == 0) return new Tidied(null, "输入框是空的喵", false);

        if (looksHex(s)) {
            String js = SendProtoApi.decodeHex(s);
            if (js != null && js.trim().startsWith("{")) {
                return new Tidied(pretty(unescape(js)), "hex → 字段 JSON", true);
            }
            return new Tidied(s, "这段 hex 解不成 JSON", false);
        }

        if (looksBase64(s)) {
            String js = SendProtoApi.decodeBase64(s);
            if (js != null && js.trim().startsWith("{")) {
                return new Tidied(pretty(unescape(js)), "base64 → 字段 JSON", true);
            }
        }

        if (s.startsWith("{")) {
            String body = s;
            boolean wrapped = false;
            try {
                org.json.JSONObject o = new org.json.JSONObject(s);
                org.json.JSONObject e10 = o.optJSONObject("10");
                if (e10 != null) {
                    String inner = e10.optString("1", "");
                    if (inner.trim().startsWith("{")) {
                        body = inner;
                        wrapped = true;
                    }
                }
            } catch (Throwable t) {
                return new Tidied(s, "不是合法 JSON 喵", false);
            }
            String p = pretty(unescape(body));
            if (wrapped) {
                try {
                    org.json.JSONObject e10 = new org.json.JSONObject();
                    e10.put("1", p);
                    org.json.JSONObject root = new org.json.JSONObject();
                    root.put("10", e10);
                    return new Tidied(root.toString(), "整理好了（元素 10 包装）", true);
                } catch (Throwable ignore) {
                }
            }
            return new Tidied(p, "整理好了", true);
        }

        return new Tidied(s, pbMode ? "既不是 JSON 也不是 hex / base64" : "不是卡片 JSON",
                false);
    }

    public static String pretty(String json) {
        if (json == null) return "";
        StringBuilder out = new StringBuilder(json.length() + 256);
        int indent = 0;
        boolean inStr = false;
        boolean esc = false;
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            if (inStr) {
                out.append(c);
                if (esc) {
                    esc = false;
                } else if (c == '\\') {
                    esc = true;
                } else if (c == '"') {
                    inStr = false;
                }
                continue;
            }
            switch (c) {
                case '"':
                    inStr = true;
                    out.append(c);
                    break;
                case '{':
                case '[': {
                    char close = (c == '{') ? '}' : ']';
                    if (nextMeaningful(json, i + 1) == close) {
                        out.append(c);
                    } else {
                        out.append(c).append('\n');
                        indent++;
                        pad(out, indent);
                    }
                    break;
                }
                case '}':
                case ']': {
                    char open = (c == '}') ? '{' : '[';
                    if (lastMeaningful(out) == open) {
                        out.append(c);
                    } else {
                        out.append('\n');
                        indent--;
                        if (indent < 0) indent = 0;
                        pad(out, indent);
                        out.append(c);
                    }
                    break;
                }
                case ',':
                    out.append(",\n");
                    pad(out, indent);
                    break;
                case ':':
                    out.append(": ");
                    break;
                case ' ':
                case '\t':
                case '\r':
                case '\n':
                    break;
                default:
                    out.append(c);
            }
        }
        return out.toString();
    }

    public static String unescape(String s) {
        if (s == null) return "";
        return s.replace("\\/", "/");
    }

    public static String compact(String json) {
        if (json == null) return "";
        StringBuilder out = new StringBuilder(json.length());
        boolean inStr = false;
        boolean esc = false;
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            if (inStr) {
                out.append(c);
                if (esc) esc = false;
                else if (c == '\\') esc = true;
                else if (c == '"') inStr = false;
                continue;
            }
            if (c == '"') {
                inStr = true;
                out.append(c);
            } else if (c != ' ' && c != '\n' && c != '\r' && c != '\t') {
                out.append(c);
            }
        }
        return out.toString();
    }

    private static void pad(StringBuilder out, int indent) {
        for (int i = 0; i < indent; i++) out.append("  ");
    }

    private static char nextMeaningful(String s, int from) {
        for (int i = from; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c != ' ' && c != '\n' && c != '\r' && c != '\t') return c;
        }
        return 0;
    }

    private static char lastMeaningful(StringBuilder sb) {
        for (int i = sb.length() - 1; i >= 0; i--) {
            char c = sb.charAt(i);
            if (c != ' ' && c != '\n' && c != '\r' && c != '\t') return c;
        }
        return 0;
    }

    public static boolean looksHex(String s) {
        if (s == null) return false;
        int n = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == ' ' || c == '\n' || c == '\r' || c == '\t' || c == ':') continue;
            if ((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F')) {
                n++;
            } else if (c == 'x' || c == 'X') {

            } else {
                return false;
            }
        }
        return n >= 4 && n % 2 == 0;
    }

    public static boolean looksLikePb(String s) {
        if (s == null) return false;
        String t = s.trim();
        if (t.length() == 0) return false;
        if (looksHex(t)) return true;
        if (!t.startsWith("{")) return false;
        if (t.contains("\"app\"") && t.contains("\"view\"")) return false;
        try {
            new org.json.JSONObject(t);
            return true;
        } catch (Throwable e) {
            return false;
        }
    }

    public static boolean looksBase64(String s) {
        if (s == null || s.length() < 8 || s.length() % 4 != 0) return false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            boolean ok = (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z')
                    || (c >= '0' && c <= '9') || c == '+' || c == '/' || c == '=';
            if (!ok) return false;
        }
        return true;
    }

    public static String suggestName(String content, String cmd, boolean pbMode) {
        try {
            if (pbMode) {
                String c = cmd == null ? "" : cmd.trim();
                if (c.length() > 0) {
                    int dot = c.lastIndexOf('.');
                    return dot > 0 && dot < c.length() - 1 ? c.substring(dot + 1) : c;
                }
                return "PB" + (System.currentTimeMillis() % 10000);
            }
            if (content == null || content.trim().length() == 0) {
                return "卡片" + (System.currentTimeMillis() % 10000);
            }
            org.json.JSONObject o = new org.json.JSONObject(content.trim());
            org.json.JSONObject meta = o.optJSONObject("meta");
            org.json.JSONObject news = meta == null ? null : meta.optJSONObject("news");
            if (news != null) {
                String t = news.optString("title", "").trim();
                if (t.length() > 0) return cut(t, 16);
                String tag = news.optString("tag", "").trim();
                if (tag.length() > 0) return cut(tag, 16) + "卡";
            }
            org.json.JSONObject music = meta == null ? null : meta.optJSONObject("music");
            if (music != null) {
                String t = music.optString("title", "").trim();
                if (t.length() > 0) return cut(t, 16);
            }
            String prompt = o.optString("prompt", "").trim();
            if (prompt.length() > 0) return cut(prompt, 16);
        } catch (Throwable ignore) {
        }
        return pbMode ? "PB模板" : "卡片模板";
    }

    private static String cut(String s, int max) {
        if (s == null) return "";
        s = s.replace("\n", " ").trim();
        return s.length() > max ? s.substring(0, max) : s;
    }
}
