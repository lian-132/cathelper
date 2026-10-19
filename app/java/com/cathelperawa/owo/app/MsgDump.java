package com.cathelperawa.owo.app;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MsgDump {

    private static final int MAX_DEPTH = 6;
    private static final int MAX_LIST = 200;
    private static final int MAX_STR = 3000;
    private static final int MAX_HEX = 64;
    private static final int MAX_FIELDS = 200;

    public static String dump(Object root) {
        StringBuilder sb = new StringBuilder();
        try {
            dumpValue(sb, root, 0);
        } catch (Throwable t) {
            sb.append("(dump失败 ").append(t).append(")");
        }
        return sb.toString();
    }

    private static void ind(StringBuilder sb, int d) {
        for (int i = 0; i < d; i++) sb.append("  ");
    }

    private static void dumpValue(StringBuilder sb, Object o, int depth) {
        if (o == null) { sb.append("null"); return; }
        Class<?> c = o.getClass();

        if (o instanceof String) {
            sb.append('"').append(esc((String) o)).append('"');
            return;
        }
        if (o instanceof Number || o instanceof Boolean || o instanceof Character) {
            sb.append(o.toString());
            return;
        }
        if (o instanceof Enum) {
            sb.append('"').append(((Enum<?>) o).name()).append('"');
            return;
        }
        if (c == byte[].class) {
            byte[] b = (byte[]) o;
            sb.append("\"hex[").append(b.length).append("]:")
              .append(hexOf(b, MAX_HEX)).append(b.length > MAX_HEX ? "..." : "").append('"');
            return;
        }
        if (c.isArray()) {
            int n = Array.getLength(o);
            if (n == 0) { sb.append("[]"); return; }
            sb.append("[\n");
            for (int i = 0; i < n && i < MAX_LIST; i++) {
                ind(sb, depth + 1);
                dumpValue(sb, Array.get(o, i), depth + 1);
                sb.append(",\n");
            }
            if (n > MAX_LIST) { ind(sb, depth + 1); sb.append("\"...(共").append(n).append("项)\"\n"); }
            ind(sb, depth); sb.append(']');
            return;
        }
        if (o instanceof List) {
            List<?> l = (List<?>) o;
            if (l.isEmpty()) { sb.append("[]"); return; }
            sb.append("[\n");
            for (int i = 0; i < l.size() && i < MAX_LIST; i++) {
                ind(sb, depth + 1);
                dumpValue(sb, l.get(i), depth + 1);
                sb.append(",\n");
            }
            if (l.size() > MAX_LIST) { ind(sb, depth + 1); sb.append("\"...(共").append(l.size()).append("项)\"\n"); }
            ind(sb, depth); sb.append(']');
            return;
        }
        if (o instanceof Map) {
            Map<?, ?> m = (Map<?, ?>) o;
            if (m.isEmpty()) { sb.append("{}"); return; }
            sb.append("{\n");
            int i = 0;
            for (Map.Entry<?, ?> e : m.entrySet()) {
                if (i >= MAX_FIELDS) { ind(sb, depth + 1); sb.append("\"...(共").append(m.size()).append("项)\"\n"); break; }
                i++;
                ind(sb, depth + 1);
                sb.append('"').append(esc(String.valueOf(e.getKey()))).append("\": ");
                dumpValue(sb, e.getValue(), depth + 1);
                sb.append(",\n");
            }
            ind(sb, depth); sb.append('}');
            return;
        }

        if (depth >= MAX_DEPTH) {
            sb.append('"').append(c.getSimpleName()).append('"');
            return;
        }
        List<Object[]> items = new ArrayList<Object[]>();
        Class<?> cc = c;
        while (cc != null && cc != Object.class) {
            for (Field f : cc.getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers()) || f.isSynthetic()) continue;
                String fn = f.getName();
                if (fn.startsWith("$") || fn.startsWith("shadow$") || "serialVersionUID".equals(fn)) continue;
                try {
                    f.setAccessible(true);
                    Object v = f.get(o);
                    if (v == null) continue;
                    if (v instanceof String && ((String) v).length() == 0) continue;
                    items.add(new Object[]{fn, v});
                } catch (Throwable ignore) {
                }
            }
            cc = cc.getSuperclass();
        }
        if (items.isEmpty()) { sb.append("{}"); return; }
        sb.append("{\n");
        int i = 0;
        for (Object[] it : items) {
            if (i >= MAX_FIELDS) { ind(sb, depth + 1); sb.append("\"...(共").append(items.size()).append("字段)\"\n"); break; }
            ind(sb, depth + 1);
            sb.append('"').append(it[0]).append("\": ");
            dumpValue(sb, it[1], depth + 1);
            sb.append(",\n");
        }
        ind(sb, depth); sb.append('}');
    }

    private static final char[] HEX = "0123456789ABCDEF".toCharArray();

    private static String hexOf(byte[] b, int max) {
        int n = Math.min(b.length, max);
        StringBuilder sb = new StringBuilder(n * 2);
        for (int i = 0; i < n; i++) {
            int v = b[i] & 0xFF;
            sb.append(HEX[v >> 4]).append(HEX[v & 0xF]);
        }
        return sb.toString();
    }

    private static String esc(String s) {
        if (s == null) return "";
        if (s.length() > MAX_STR) s = s.substring(0, MAX_STR) + "...(" + s.length() + "字)";
        StringBuilder sb = new StringBuilder(s.length() + 8);
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            switch (ch) {
                case '"':  sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (ch < 0x20) sb.append(String.format("\\u%04x", (int) ch));
                    else sb.append(ch);
            }
        }
        return sb.toString();
    }
}
