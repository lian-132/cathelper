package com.cathelperawa.owo.app.feature;

import com.cathelperawa.owo.app.bridge.CurrentChat;
import com.cathelperawa.owo.app.core.JsonPretty;

public final class OidbCard {

    private OidbCard() {
    }

    public static final long DEFAULT_APPID = 101846357L;

    public static final String CMD = "OidbSvc.0xdc2_34";

    public static long currentGroupUin() {
        try {
            String raw = CurrentChat.peerRaw();
            if (raw == null || raw.length() == 0) raw = CurrentChat.peerUid();
            if (raw == null) return 0L;
            String s = raw;
            if (s.startsWith("u_")) s = s.substring(2);
            int at = s.indexOf('@');
            if (at > 0) s = s.substring(0, at);
            StringBuilder d = new StringBuilder();
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                if (c >= '0' && c <= '9') d.append(c);
            }
            if (d.length() == 0) return 0L;
            return Long.parseLong(d.toString());
        } catch (Throwable t) {
            return 0L;
        }
    }

    public static String toDc2(String arkJson, long groupUin) {
        try {
            if (arkJson == null) return null;
            String src = arkJson.trim();
            if (src.length() < 2) return null;
            if (!src.startsWith("{")) return null;

            org.json.JSONObject o = new org.json.JSONObject(src);

            org.json.JSONObject e10 = o.optJSONObject("10");
            if (e10 != null) {
                String inner = e10.optString("1", "");
                if (inner.trim().startsWith("{")) {
                    o = new org.json.JSONObject(inner);
                }
            }
            org.json.JSONObject meta = o.optJSONObject("meta");
            if (meta == null) {
                String ms = o.optString("meta", "");
                if (ms.length() > 2) meta = new org.json.JSONObject(ms);
            }
            if (meta == null) return null;
            org.json.JSONObject news = meta.optJSONObject("news");
            if (news == null) news = meta.optJSONObject("music");
            if (news == null) return null;

            long uin = groupUin > 0 ? groupUin : currentGroupUin();
            long appid = news.optLong("appid", 0L);
            if (appid <= 0) appid = DEFAULT_APPID;

            String title = news.optString("title", "");
            String desc = news.optString("desc", "");
            String tag = news.optString("tag", "");
            if (tag.length() == 0) tag = o.optString("prompt", "");
            String jump = JsonPretty.unescape(news.optString("jumpUrl", ""));
            String preview = JsonPretty.unescape(news.optString("preview", ""));

            StringBuilder sb = new StringBuilder(512);
            sb.append("{\n");
            sb.append("  \"1\": 3522,\n");
            sb.append("  \"2\": 34,\n");
            sb.append("  \"4\": {\n");
            sb.append("    \"1\": {\n");
            sb.append("      \"1\": ").append(appid).append(",\n");
            sb.append("      \"2\": 1,\n");
            sb.append("      \"3\": 0,\n");
            sb.append("      \"5\": {\n        \"1\": 1\n      },\n");
            sb.append("      \"11\": ").append(uin).append(",\n");
            sb.append("      \"12\": {\n");
            sb.append("        \"1\": 1,\n");
            sb.append("        \"10\": ").append(q(title)).append(",\n");
            sb.append("        \"11\": ").append(q(desc)).append(",\n");
            sb.append("        \"12\": ").append(q(tag)).append(",\n");
            sb.append("        \"13\": ").append(q(jump)).append(",\n");
            sb.append("        \"14\": ").append(q(preview)).append("\n");
            sb.append("      }\n");
            sb.append("    },\n");
            sb.append("    \"2\": {\n");
            sb.append("      \"1\": 1,\n");
            sb.append("      \"2\": ").append(uin).append("\n");
            sb.append("    }\n");
            sb.append("  },\n");
            sb.append("  \"6\": \"android 9.1.35\"\n");
            sb.append("}");
            return sb.toString();
        } catch (Throwable t) {
            return null;
        }
    }

    private static String q(String s) {
        if (s == null) return "\"\"";
        try {
            return org.json.JSONObject.quote(s);
        } catch (Throwable t) {
            return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
        }
    }

    public static final class Fields {
        public long appid = DEFAULT_APPID;
        public long uin = 0L;
        public String title = "";
        public String desc = "";
        public String tag = "";
        public String jump = "";
        public String preview = "";
    }

    public static String buildForSend(String title, String desc, String icon,
                                      String prompt, String url) {
        return buildForSend(title, desc, icon, prompt, url, DEFAULT_APPID);
    }

    public static String buildForSend(String title, String desc, String icon,
                                      String prompt, String url, long appid) {
        try {
            long uin = currentGroupUin();
            if (uin <= 0) return null;
            String p = (prompt == null || prompt.trim().length() == 0) ? title : prompt;
            StringBuilder sb = new StringBuilder(512);
            sb.append("{\n");
            sb.append("  \"1\": 3522,\n");
            sb.append("  \"2\": 34,\n");
            sb.append("  \"4\": {\n");
            sb.append("    \"1\": {\n");
            sb.append("      \"1\": ").append(appid).append(",\n");
            sb.append("      \"2\": 1,\n");
            sb.append("      \"3\": 0,\n");
            sb.append("      \"5\": {\"1\": 1},\n");
            sb.append("      \"11\": ").append(uin).append(",\n");
            sb.append("      \"12\": {\n");
            sb.append("        \"1\": 1,\n");
            sb.append("        \"10\": ").append(q(title)).append(",\n");
            sb.append("        \"11\": ").append(q(desc)).append(",\n");
            sb.append("        \"12\": ").append(q(icon == null ? "" : icon)).append(",\n");
            sb.append("        \"13\": ").append(q(p)).append(",\n");
            sb.append("        \"14\": ").append(q(url == null ? "" : url)).append("\n");
            sb.append("      }\n");
            sb.append("    },\n");
            sb.append("    \"2\": {\"1\": 1, \"2\": ").append(uin).append("}\n");
            sb.append("  },\n");
            sb.append("  \"6\": \"android 9.2.60\"\n");
            sb.append("}");
            return sb.toString();
        } catch (Throwable t) {
            return null;
        }
    }

    public static Fields parse(String pbJson) {
        try {
            if (pbJson == null) return null;
            String t = pbJson.trim();
            if (!t.startsWith("{")) return null;
            org.json.JSONObject o = new org.json.JSONObject(t);
            org.json.JSONObject n4 = o.optJSONObject("4");
            if (n4 == null) return null;
            org.json.JSONObject n41 = n4.optJSONObject("1");
            if (n41 == null) return null;
            Fields f = new Fields();
            f.appid = n41.optLong("1", DEFAULT_APPID);
            if (f.appid <= 0) f.appid = DEFAULT_APPID;
            f.uin = n41.optLong("11", 0L);
            org.json.JSONObject n12 = n41.optJSONObject("12");
            if (n12 != null) {
                f.title = n12.optString("10", "");
                f.desc = n12.optString("11", "");
                f.tag = n12.optString("12", "");
                f.jump = JsonPretty.unescape(n12.optString("13", ""));
                f.preview = JsonPretty.unescape(n12.optString("14", ""));
            }
            return f;
        } catch (Throwable t) {
            return null;
        }
    }

    public static String toFakeArk(String pbJson) {
        Fields f = parse(pbJson);
        if (f == null) return null;
        try {
            org.json.JSONObject news = new org.json.JSONObject();
            news.put("title", f.title);
            news.put("desc", f.desc);
            news.put("tag", f.tag);
            news.put("jumpUrl", f.jump);
            news.put("preview", f.preview);
            org.json.JSONObject meta = new org.json.JSONObject();
            meta.put("news", news);
            org.json.JSONObject root = new org.json.JSONObject();
            root.put("app", "com.tencent.tuwen.lua");
            root.put("view", "news");
            root.put("meta", meta);
            return root.toString();
        } catch (Throwable t) {
            return null;
        }
    }

    public static String rebuild(String newArk, String oldPb) {
        try {
            Fields base = parse(oldPb);
            long appid = base != null ? base.appid : DEFAULT_APPID;
            long uin = base != null ? base.uin : 0L;

            String title = "", desc = "", tag = "", jump = "", preview = "";
            if (newArk != null && newArk.trim().startsWith("{")) {
                org.json.JSONObject o = new org.json.JSONObject(newArk.trim());
                org.json.JSONObject meta = o.optJSONObject("meta");
                org.json.JSONObject news = meta == null ? null : meta.optJSONObject("news");
                if (news != null) {
                    title = news.optString("title", "");
                    desc = news.optString("desc", "");
                    tag = news.optString("tag", "");
                    jump = JsonPretty.unescape(news.optString("jumpUrl", ""));
                    preview = JsonPretty.unescape(news.optString("preview", ""));
                }
            }
            return build(appid, uin, title, desc, tag, jump, preview);
        } catch (Throwable t) {
            return null;
        }
    }

    public static String retarget(String pbJson, long uin) {
        if (uin <= 0) return pbJson;
        try {
            org.json.JSONObject o = new org.json.JSONObject(pbJson);
            org.json.JSONObject n4 = o.optJSONObject("4");
            if (n4 == null) return pbJson;
            boolean changed = false;
            org.json.JSONObject n41 = n4.optJSONObject("1");
            if (n41 != null && n41.optLong("11", 0L) != uin) {
                n41.put("11", uin);
                changed = true;
            }
            org.json.JSONObject n42 = n4.optJSONObject("2");
            if (n42 != null && n42.optLong("2", 0L) != uin) {
                n42.put("2", uin);
                changed = true;
            }

            return changed ? o.toString() : pbJson;
        } catch (Throwable t) {
            return pbJson;
        }
    }

    public static String build(long appid, long uin, String title, String desc,
                               String tag, String jump, String preview) {
        StringBuilder sb = new StringBuilder(512);
        sb.append("{\n");
        sb.append("  \"1\": 3522,\n");
        sb.append("  \"2\": 34,\n");
        sb.append("  \"4\": {\n");
        sb.append("    \"1\": {\n");
        sb.append("      \"1\": ").append(appid).append(",\n");
        sb.append("      \"2\": 1,\n");
        sb.append("      \"3\": 0,\n");
        sb.append("      \"5\": {\n        \"1\": 1\n      },\n");
        sb.append("      \"11\": ").append(uin).append(",\n");
        sb.append("      \"12\": {\n");
        sb.append("        \"1\": 1,\n");
        sb.append("        \"10\": ").append(q(title)).append(",\n");
        sb.append("        \"11\": ").append(q(desc)).append(",\n");
        sb.append("        \"12\": ").append(q(tag)).append(",\n");
        sb.append("        \"13\": ").append(q(jump)).append(",\n");
        sb.append("        \"14\": ").append(q(preview)).append("\n");
        sb.append("      }\n");
        sb.append("    },\n");
        sb.append("    \"2\": {\n");
        sb.append("      \"1\": 1,\n");
        sb.append("      \"2\": ").append(uin).append("\n");
        sb.append("    }\n");
        sb.append("  },\n");
        sb.append("  \"6\": \"android 9.1.35\"\n");
        sb.append("}");
        return sb.toString();
    }
}
