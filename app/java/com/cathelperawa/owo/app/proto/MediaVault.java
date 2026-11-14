package com.cathelperawa.owo.app.proto;

import android.content.Context;

import com.cathelperawa.owo.app.core.HookConfig;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public final class MediaVault {

    private MediaVault() {
    }

    private static final String K = "fake_media_list";
    private static final int MAX = 12;

    public static final String T_PIC = "pic";
    public static final String T_FACE = "face";
    public static final String T_PTT = "ptt";
    public static final String T_VIDEO = "video";
    public static final String T_ARK = "ark";
    public static final String T_FILE = "file";
    public static final String T_OTHER = "other";

    public static List<String[]> all(Context c) {
        List<String[]> out = new ArrayList<String[]>();
        if (c == null) return out;
        try {
            String raw = HookConfig.get(c, K, "");
            if (raw == null || raw.length() == 0) return out;
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                String type = o.optString("type", "");
                String js = o.optString("json", "");

                if (type.length() == 0 || T_OTHER.equals(type)) {
                    String g = guessType(js);
                    if (!T_OTHER.equals(g)) type = g;
                }
                out.add(new String[]{o.optString("name", "?"), js, type});
            }
        } catch (Throwable ignore) {
        }
        return out;
    }

    public static void add(Context c, String json) {
        add(c, json, null, null);
    }

    public static void add(Context c, String json, String name) {
        add(c, json, name, null);
    }

    public static void add(Context c, String json, String name, String type) {
        if (type == null) type = guessType(json);
        if (c == null || json == null || json.length() < 10) return;
        try {
            List<String[]> lib = all(c);

            for (int i = 0; i < lib.size(); i++) {
                if (json.equals(lib.get(i)[1])) return;
            }
            JSONArray arr = new JSONArray();
            JSONObject n = new JSONObject();
            n.put("name", name == null ? guess(json) : name);
            n.put("json", json);
            n.put("type", type);
            arr.put(n);
            for (int i = 0; i < lib.size() && i < MAX - 1; i++) {
                JSONObject o = new JSONObject();
                o.put("name", lib.get(i)[0]);
                o.put("json", lib.get(i)[1]);
                o.put("type", lib.get(i).length > 2
                        ? lib.get(i)[2] : guessType(lib.get(i)[1]));
                arr.put(o);
            }
            HookConfig.set(c, K, arr.toString());
        } catch (Throwable ignore) {
        }
    }

    public static void remove(Context c, int index) {
        if (c == null) return;
        try {
            List<String[]> lib = all(c);
            if (index < 0 || index >= lib.size()) return;
            lib.remove(index);
            JSONArray arr = new JSONArray();
            for (int i = 0; i < lib.size(); i++) {
                JSONObject o = new JSONObject();
                o.put("name", lib.get(i)[0]);
                o.put("json", lib.get(i)[1]);
                o.put("type", lib.get(i).length > 2 ? lib.get(i)[2] : guessType(lib.get(i)[1]));
                arr.put(o);
            }
            HookConfig.set(c, K, arr.toString());
        } catch (Throwable ignore) {
        }
    }

    public static void rename(Context c, int index, String name) {
        if (c == null) return;
        try {
            List<String[]> lib = all(c);
            if (index < 0 || index >= lib.size()) return;
            lib.get(index)[0] = name == null || name.trim().length() == 0
                    ? lib.get(index)[0] : name.trim();
            JSONArray arr = new JSONArray();
            for (int i = 0; i < lib.size(); i++) {
                JSONObject o = new JSONObject();
                o.put("name", lib.get(i)[0]);
                o.put("json", lib.get(i)[1]);
                o.put("type", lib.get(i).length > 2 ? lib.get(i)[2] : guessType(lib.get(i)[1]));
                arr.put(o);
            }
            HookConfig.set(c, K, arr.toString());
        } catch (Throwable ignore) {
        }
    }

    public static List<String[]> byType(Context c, String type) {
        List<String[]> out = new ArrayList<String[]>();
        List<String[]> all = all(c);
        for (int i = 0; i < all.size(); i++) {
            String[] it = all.get(i);
            String t = it.length > 2 ? it[2] : guessType(it[1]);
            if (type == null || type.equals(t)) {
                out.add(new String[]{it[0], it[1], t, String.valueOf(i)});
            }
        }
        return out;
    }

    public static List<String[]> byTypes(Context c, String[] types) {
        List<String[]> out = new ArrayList<String[]>();
        List<String[]> all = all(c);
        for (int i = 0; i < all.size(); i++) {
            String[] it = all.get(i);
            String t = it.length > 2 ? it[2] : guessType(it[1]);
            boolean hit = (types == null);
            if (!hit) {
                for (int k = 0; k < types.length; k++) {
                    if (types[k] != null && types[k].equals(t)) {
                        hit = true;
                        break;
                    }
                }
            }
            if (hit) out.add(new String[]{it[0], it[1], t, String.valueOf(i)});
        }
        return out;
    }

    public static String typeName(String type) {
        if (T_PIC.equals(type)) return "图片";
        if (T_FACE.equals(type)) return "表情";
        if (T_PTT.equals(type)) return "语音";
        if (T_VIDEO.equals(type)) return "视频";
        if (T_ARK.equals(type)) return "卡片";
        if (T_FILE.equals(type)) return "文件";
        return "素材";
    }

    public static String guessType(String json) {
        if (json == null) return T_OTHER;
        try {
            String t = json.trim();
            JSONObject obj;
            if (t.startsWith("[")) {
                JSONArray arr = new JSONArray(t);
                obj = null;
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.optJSONObject(i);
                    if (o == null) continue;
                    String g = typeOf(o);
                    if (!T_OTHER.equals(g)) return g;
                    if (obj == null) obj = o;
                }
                if (obj == null) return T_OTHER;
            } else {
                obj = new JSONObject(t);
            }
            return typeOf(obj);
        } catch (Throwable t) {
            return guessByString(json);
        }
    }

    private static String guessByString(String json) {
        if (json == null) return T_OTHER;
        if (json.contains("\"51\"")) return T_ARK;
        if (json.contains("\"53\"")) {
            String l = json.toLowerCase();
            if (l.contains(".amr") || l.contains(".silk") || l.contains(".slk")) {
                return T_PTT;
            }
            return T_FACE;
        }
        if (json.contains("\"5\"")) return T_VIDEO;
        if (json.contains("\"7\"")) return T_FILE;
        if (json.contains("\"2\"")) return T_PIC;
        if (json.contains("\"10\"")) return T_ARK;
        if (json.contains("\"4\"")) return T_PTT;
        return T_OTHER;
    }

    private static String typeOf(JSONObject o) {
        if (o.has("51")) return T_ARK;
        if (o.has("53")) {

            String s53 = o.toString().toLowerCase();
            if (s53.contains(".amr") || s53.contains(".silk") || s53.contains(".slk")) {
                return T_PTT;
            }
            return T_FACE;
        }
        if (o.has("2")) return T_PIC;
        if (o.has("10")) return T_ARK;
        if (o.has("4")) return T_PTT;
        if (o.has("5")) return T_VIDEO;
        if (o.has("7")) return T_FILE;
        if (o.has("3")) return T_FILE;
        return T_OTHER;
    }

    public static String fromElement(Object el) {
        try {
            if (el == null) return null;
            byte[] pb = elemPb(el);
            if (pb == null || pb.length == 0) return null;
            com.cathelperawa.owo.app.ovo.FunProtoData pd =
                    new com.cathelperawa.owo.app.ovo.FunProtoData();
            pd.fromBytes(pb);
            return pd.toJSON().toString();
        } catch (Throwable t) {
            return null;
        }
    }

    private static byte[] elemPb(Object el) {
        String[] names = {"toByteArray", "serialize", "toProto", "encode"};
        for (int i = 0; i < names.length; i++) {
            try {
                java.lang.reflect.Method m = findMethod(el.getClass(), names[i]);
                if (m == null) continue;
                m.setAccessible(true);
                Object r = m.getParameterTypes().length == 0 ? m.invoke(el) : null;
                if (r instanceof byte[]) return (byte[]) r;
            } catch (Throwable ignore) {
            }
        }
        return null;
    }

    private static java.lang.reflect.Method findMethod(Class<?> c, String name) {
        while (c != null && c != Object.class) {
            for (java.lang.reflect.Method m : c.getDeclaredMethods()) {
                if (!m.getName().equals(name)) continue;
                if (m.getParameterTypes().length != 0) continue;
                return m;
            }
            c = c.getSuperclass();
        }
        return null;
    }

    public static byte[] toPb(String json) {
        try {
            String t = json == null ? "" : json.trim();
            if (t.startsWith("[")) {
                JSONArray arr = new JSONArray(t);
                byte[] out = new byte[0];
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.optJSONObject(i);
                    if (o == null || o.length() == 0) continue;
                    com.cathelperawa.owo.app.ovo.FunProtoData pd =
                            new com.cathelperawa.owo.app.ovo.FunProtoData();
                    pd.fromJSON(o);
                    byte[] b = pd.toBytes();
                    if (b == null || b.length == 0) continue;
                    out = com.cathelperawa.owo.app.proto.Pb.cat(
                            out, com.cathelperawa.owo.app.proto.Pb.ld(2, b));
                }
                return out.length > 0 ? out : null;
            }
            JSONObject obj = new JSONObject(t);
            com.cathelperawa.owo.app.ovo.FunProtoData pd =
                    new com.cathelperawa.owo.app.ovo.FunProtoData();
            pd.fromJSON(obj);
            byte[] b = pd.toBytes();
            if (b == null || b.length == 0) return null;
            return com.cathelperawa.owo.app.proto.Pb.ld(2, b);
        } catch (Throwable t2) {
            return null;
        }
    }

    private static String guess(String json) {
        return niceName(json, guessType(json));
    }

    public static String briefOf(String json, String type) {
        String t = type == null ? guessType(json) : type;
        if (T_PIC.equals(t)) return "[图片]";
        if (T_PTT.equals(t)) return "[语音]";
        if (T_VIDEO.equals(t)) return "[视频]";
        if (T_FACE.equals(t)) {

            String n = grab(json, "\"([^\"]{2,12})\"\\s*,\\s*\"1001\"");
            if (n == null) {
                n = grab(json, "\"9\"\\s*:\\s*\"([^\"]{2,12})\"");
            }
            if (n == null) {
                n = grab(json, "\"8\"\\s*:\\s*\"([^\"]{2,12})\"");
            }
            return n == null ? "[表情]" : n;
        }
        if (T_ARK.equals(t)) {

            String v = grab(json, "\"prompt\"\\s*:\\s*\"([^\"]{1,48})\"");
            if (v == null) v = grab(json, "\"title\"\\s*:\\s*\"([^\"]{1,48})\"");
            if (v == null) v = grab(json, "\"desc\"\\s*:\\s*\"([^\"]{1,48})\"");
            if (v == null) v = grab(json, "\"text\"\\s*:\\s*\"([^\"]{1,48})\"");
            return v == null ? "[卡片]" : v;
        }
        if (T_FILE.equals(t)) {
            String fn = grab(json, "\"fileName\"\\s*:\\s*\"([^\"]{1,30})\"");
            if (fn == null) {
                fn = grab(json, "\"4\"\\s*:\\s*\"([^\"]{1,30})\"");
            }
            return fn == null ? "[文件]" : "[文件] " + fn;
        }
        return "[消息]";
    }

    public static String niceName(String json, String type) {
        String base = typeName(type);
        if (json == null) return base;
        try {

            String[] keys = {"title", "desc", "prompt", "fileName", "name", "text"};
            for (int i = 0; i < keys.length; i++) {
                String v = grab(json, "\"" + keys[i] + "\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.){1,24})");
                if (v != null) return base + " · " + v;
            }

            String fn = grab(json, "\\d+\\s*:\\s*\"([^\"]{1,28}\\.(?:jpg|png|gif|jpeg|mp4|amr|silk|txt|zip|apk|pdf))\"");
            if (fn != null) return base + " · " + fn;
        } catch (Throwable ignore) {
        }
        return base + " " + time();
    }

    private static String grab(String s, String regex) {
        try {

            String t = s.replace("\\\"", "\"");
            java.util.regex.Matcher m = java.util.regex.Pattern
                    .compile(regex, java.util.regex.Pattern.CASE_INSENSITIVE).matcher(t);
            if (m.find()) {
                String v = m.group(1);
                return v == null || v.trim().length() == 0 ? null : v.trim();
            }
        } catch (Throwable ignore) {
        }
        return null;
    }

    private static String time() {
        java.util.Calendar c = java.util.Calendar.getInstance();
        return String.format("%02d:%02d", c.get(java.util.Calendar.HOUR_OF_DAY),
                c.get(java.util.Calendar.MINUTE));
    }
}
