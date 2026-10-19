package com.cathelperawa.owo.app.proto;

import com.cathelperawa.owo.app.util.Refl;

import org.json.JSONObject;

import java.util.List;

public final class MsgGrab {

    private MsgGrab() {}

    public static final class Result {
        public String kind;
        public String content;
        public String extra;
        public JSONObject toJson() {
            JSONObject o = new JSONObject();
            try {
                o.put("kind", kind == null ? "msg" : kind);
                o.put("content", content == null ? "" : content);
                o.put("extra", extra == null ? "" : extra);
            } catch (Throwable ignore) {
            }
            return o;
        }
    }

    public static Result extract(Object record) {
        if (record == null) return null;
        Object els = Refl.field(record, "elements");
        if (!(els instanceof List)) return null;
        List<?> list = (List<?>) els;
        if (list.isEmpty()) return null;

        for (Object el : list) {
            Object ark = Refl.field(el, "arkElement");
            if (ark == null) continue;
            String bd = firstString(ark, "bytesData", "data", "bytes", "json", "content");
            if (bd != null && bd.length() > 0) {
                Result r = new Result();
                r.kind = "card";
                r.content = bd;
                r.extra = "";
                return r;
            }
        }

        for (Object el : list) {
            Object mf = Refl.field(el, "multiForwardMsgElement");
            if (mf == null) continue;
            String resId = strOf(Refl.field(mf, "resId"));
            if (resId == null || resId.length() == 0 || "null".equals(resId)) continue;
            String xml = strOf(Refl.field(mf, "xmlContent"));
            String fn = strOf(Refl.field(mf, "fileName"));
            Result r = new Result();
            r.kind = "card";
            r.content = buildMultiArk(xml, resId, fn);
            r.extra = "";
            return r;
        }

        StringBuilder sb = new StringBuilder();
        for (Object el : list) {
            Object tx = Refl.field(el, "textElement");
            if (tx == null) continue;
            Object ct = Refl.field(tx, "content");
            if (ct == null) ct = Refl.field(tx, "text");
            if (ct != null) sb.append(ct);
        }
        if (sb.length() > 0) {
            try {
                JSONObject inner = new JSONObject();
                inner.put("1", sb.toString());
                JSONObject root = new JSONObject();
                root.put("1", inner);
                Result r = new Result();
                r.kind = "msg";
                r.content = root.toString();
                r.extra = "{\"isLongMsgMode\":true}";
                return r;
            } catch (Throwable ignore) {
            }
        }
        return null;
    }

    public static String buildMultiArk(String xml, String resId, String fn) {
        String brief = "[聊天记录]";
        int tSum = 1;
        try {
            if (xml != null) {
                int i1 = xml.indexOf("brief=\"");
                if (i1 >= 0) {
                    int i2 = xml.indexOf("\"", i1 + 7);
                    if (i2 > i1) brief = xmlUnescape(xml.substring(i1 + 7, i2));
                }
                int s1 = xml.indexOf("tSum=\"");
                if (s1 >= 0) {
                    int s2 = xml.indexOf("\"", s1 + 6);
                    if (s2 > s1) tSum = Integer.parseInt(xml.substring(s1 + 6, s2));
                }
            }
        } catch (Throwable ignore) {
        }
        try {
            JSONObject o = new JSONObject();
            o.put("app", "com.tencent.multimsg");
            JSONObject cfg = new JSONObject();
            cfg.put("autosize", 1);
            cfg.put("forward", 1);
            cfg.put("round", 1);
            cfg.put("type", "normal");
            cfg.put("width", 300);
            o.put("config", cfg);
            o.put("desc", brief);

            JSONObject meta = new JSONObject();
            JSONObject detail = new JSONObject();
            detail.put("resid", resId);
            detail.put("source", "聊天记录");
            detail.put("summary", "查看" + tSum + "条转发消息");
            detail.put("uniseq", fn);
            org.json.JSONArray news = new org.json.JSONArray();
            JSONObject n1 = new JSONObject();
            n1.put("text", brief);
            news.put(n1);
            detail.put("news", news);
            meta.put("detail", detail);
            o.put("meta", meta);
            o.put("prompt", brief);
            o.put("ver", "0.0.0.5");
            o.put("view", "contact");
            return o.toString();
        } catch (Throwable t) {
            return null;
        }
    }

    public static String xmlUnescape(String s) {
        String r = s;
        try {
            r = r.replace("&quot;", "\"").replace("&apos;", "'")
                 .replace("&lt;", "<").replace("&gt;", ">").replace("&amp;", "&");
        } catch (Throwable ignore) {
        }
        return r;
    }

    private static String firstString(Object o, String... names) {
        for (String n : names) {
            Object v = Refl.field(o, n);
            if (v instanceof String && ((String) v).trim().length() > 0) return ((String) v).trim();
            if (v instanceof byte[]) {
                try {
                    String s = new String((byte[]) v, "UTF-8").trim();
                    if (s.length() > 0) return s;
                } catch (Throwable ignore) {
                }
            }
        }
        return null;
    }

    private static String strOf(Object o) {
        if (o == null) return null;
        String s = String.valueOf(o);
        return "null".equals(s) ? null : s;
    }
}
