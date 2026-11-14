package com.cathelperawa.owo.app.script;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class ScriptReflect {

    private static final int MAX_MEMBERS = 80;

    private ScriptReflect() {}

    public static String probe(ClassLoader loader, String target, String member) {
        if (target == null || target.trim().length() == 0) {
            return "用法：ctx.probe(\"完整类名\" [, \"方法名\"])";
        }
        String t = target.trim();
        if (t.indexOf('*') >= 0) {
            return findClasses(loader, t);
        }
        Class<?> c;
        try {
            c = Class.forName(t, false, loader);
        } catch (Throwable e) {
            return "找不到类：" + t
                    + "\n  原因：" + e.getClass().getSimpleName()
                    + "\n  试试：ctx.probe(\"*" + lastSegment(t) + "*\") 按名字模糊搜";
        }
        StringBuilder b = new StringBuilder();
        b.append("类 ").append(c.getName()).append('\n');
        b.append("  修饰：").append(Modifier.toString(c.getModifiers()))
                .append(c.isInterface() ? " interface" : " class");
        if (c.getSuperclass() != null) b.append("\n  父类：").append(c.getSuperclass().getName());
        appendInterfaces(b, c);
        appendConstructors(b, c, member);
        appendFields(b, c, member);
        appendMethods(b, c, member);
        return b.toString();
    }

    private static void appendInterfaces(StringBuilder b, Class<?> c) {
        try {
            Class<?>[] is = c.getInterfaces();
            if (is != null && is.length > 0) {
                b.append("\n  接口：");
                for (int i = 0; i < is.length; i++) {
                    if (i > 0) b.append(", ");
                    b.append(is[i].getSimpleName());
                }
            }
        } catch (Throwable ignore) {
        }
    }

    private static void appendConstructors(StringBuilder b, Class<?> c, String member) {
        if (member != null && member.length() > 0) return;
        try {
            Constructor<?>[] cs = c.getDeclaredConstructors();
            if (cs == null || cs.length == 0) return;
            b.append("\n  构造器（").append(cs.length).append("）：");
            int n = 0;
            for (int i = 0; i < cs.length && n < MAX_MEMBERS; i++, n++) {
                b.append("\n    ").append(simple(c.getSimpleName())).append('(');
                b.append(params(cs[i].getParameterTypes())).append(')');
            }
        } catch (Throwable ignore) {
        }
    }

    private static void appendFields(StringBuilder b, Class<?> c, String member) {
        try {
            Field[] fs = c.getDeclaredFields();
            if (fs == null || fs.length == 0) return;
            List<String> lines = new ArrayList<String>();
            for (int i = 0; i < fs.length && lines.size() < MAX_MEMBERS; i++) {
                Field f = fs[i];
                if (member != null && member.length() > 0 && !f.getName().equals(member)) continue;
                StringBuilder s = new StringBuilder();
                s.append("    ").append(staticMark(f.getModifiers()))
                        .append(simple(f.getType().getName())).append(' ')
                        .append(f.getName());
                if (Modifier.isFinal(f.getModifiers())) s.append("  [final]");
                lines.add(s.toString());
            }
            if (lines.isEmpty()) return;
            b.append("\n  字段（").append(lines.size()).append("）：");
            for (int i = 0; i < lines.size(); i++) b.append('\n').append(lines.get(i));
        } catch (Throwable ignore) {
        }
    }

    private static void appendMethods(StringBuilder b, Class<?> c, String member) {
        try {
            Method[] ms = c.getDeclaredMethods();
            if (ms == null || ms.length == 0) return;
            List<String> lines = new ArrayList<String>();
            int skipped = 0;
            for (int i = 0; i < ms.length; i++) {
                Method m = ms[i];
                if (member != null && member.length() > 0 && !m.getName().equals(member)) continue;
                if (lines.size() >= MAX_MEMBERS) { skipped++; continue; }
                StringBuilder s = new StringBuilder();
                s.append("    ").append(staticMark(m.getModifiers()))
                        .append(simple(m.getReturnType().getName())).append(' ')
                        .append(m.getName()).append('(')
                        .append(params(m.getParameterTypes())).append(')');
                if (m.getExceptionTypes().length > 0) s.append(" throws");
                lines.add(s.toString());
            }
            if (lines.isEmpty()) {
                if (member != null && member.length() > 0) {
                    b.append("\n  找不到方法：").append(member)
                            .append("（用 ctx.probe(\"").append(c.getName())
                            .append("\") 看全部）");
                }
                return;
            }
            b.append("\n  方法（").append(lines.size()).append("）：");
            for (int i = 0; i < lines.size(); i++) b.append('\n').append(lines.get(i));
            if (skipped > 0) b.append("\n    …还有 ").append(skipped).append(" 个未列出");
        } catch (Throwable ignore) {
        }
    }

    private static String findClasses(ClassLoader loader, String pattern) {
        String p = pattern.replace("*", "");
        if (p.length() < 3) {
            return "通配符至少要带 3 个字符（避免扫出几千个类）";
        }
        String[] prefixes = {
                "com.tencent.mobileqq.",
                "com.tencent.qqnt.",
                "com.tencent.mqq.",
                "mqq.app.",
                "com.tencent.qphone.",
        };
        List<String> hits = new ArrayList<String>();
        long deadline = System.currentTimeMillis() + 4000;
        for (int i = 0; i < prefixes.length && hits.size() < 40; i++) {
            scanDir(loader, prefixes[i], p.toLowerCase(), hits, "", 0, deadline);
            if (System.currentTimeMillis() > deadline) break;
        }
        if (hits.isEmpty()) {
            return "没有匹配 \"" + pattern + "\" 的类（只搜了 " + prefixes.length + " 个常见包）";
        }
        StringBuilder b = new StringBuilder();
        b.append("匹配 \"").append(pattern).append("\" 的类（").append(hits.size()).append("）：");
        for (int i = 0; i < hits.size(); i++) b.append("\n  ").append(hits.get(i));
        b.append("\n\n挑一个完整类名再 ctx.probe(类名) 看方法");
        return b.toString();
    }

    private static void scanDir(ClassLoader loader, String pkg, String key,
                                List<String> hits, String ignored, int depth, long deadline) {
        if (System.currentTimeMillis() > deadline) return;

        String[] tails = {key, capitalize(key), key + "Utils", key + "Helper", key + "Manager"};
        for (int i = 0; i < tails.length; i++) {
            String full = pkg + tails[i];
            try {
                Class<?> c = Class.forName(full, false, loader);
                if (c != null && !hits.contains(c.getName())) hits.add(c.getName());
            } catch (Throwable ignore) {
            }
        }

        String[] subs = {"utils.", "utils.reflect.", "activity.", "helper.", "manager.", "api."};
        for (int i = 0; i < subs.length; i++) {
            String full = pkg + subs[i] + capitalize(key);
            try {
                Class<?> c = Class.forName(full, false, loader);
                if (c != null && !hits.contains(c.getName())) hits.add(c.getName());
            } catch (Throwable ignore) {
            }
        }
    }

    public static Object call(ClassLoader loader, Class<?> owner, String className,
                              String method, Object[] args) {
        Result r = callEx(loader, owner, className, method, args);
        return r.ok ? r.value : null;
    }

    public static Result callEx(ClassLoader loader, Class<?> owner,
                                String className, String method, Object[] args) {
        Result r = new Result();
        if (method == null || method.length() == 0) {
            r.err = "方法名是空的";
            return r;
        }
        Class<?> c = owner;
        if (c == null) {
            if (className == null || className.length() == 0) {
                r.err = "类名是空的";
                return r;
            }
            try {
                c = Class.forName(className, false, loader);
            } catch (Throwable e) {
                r.err = "找不到类 " + className;
                return r;
            }
        }
        Object[] a = (args == null) ? new Object[0] : args;

        Method best = pick(c.getDeclaredMethods(), method, a, true);
        Object recv = null;
        if (best == null) {
            best = pick(c.getDeclaredMethods(), method, a, false);
            if (best != null) {
                recv = instanceOf(c, r);
                if (recv == null) {
                    r.err = "方法是实例方法，但没能取到 " + c.getSimpleName() + " 的实例"
                            + "（可先用 ctx.probe 看有没有静态替代）";
                    return r;
                }
            }
        }
        if (best == null) {
            r.err = "没有能匹配这些参数的重载：" + method
                    + "(" + types(a) + ")";
            r.tried.add("已尝试与参数个数相同的重载，类型均不匹配");
            return r;
        }
        try {
            best.setAccessible(true);
            r.sig = signature(best);
            r.tried.add(r.sig);
            r.value = best.invoke(recv, a);
            r.ok = true;
            return r;
        } catch (java.lang.reflect.InvocationTargetException ite) {
            Throwable cause = ite.getCause() == null ? ite : ite.getCause();
            r.err = "调用抛异常：" + cause.getClass().getSimpleName() + ": " + cause.getMessage();
            return r;
        } catch (Throwable e) {
            r.err = "调用失败：" + e.getClass().getSimpleName() + ": " + e.getMessage();
            return r;
        }
    }

    private static Method pick(Method[] ms, String name, Object[] args, boolean wantStatic) {
        if (ms == null) return null;

        for (int pass = 0; pass < 3; pass++) {
            for (int i = 0; i < ms.length; i++) {
                Method m = ms[i];
                if (!m.getName().equals(name)) continue;
                if (Modifier.isStatic(m.getModifiers()) != wantStatic) continue;
                Class<?>[] ps = m.getParameterTypes();
                if (ps.length != args.length) continue;
                if (accepts(ps, args, pass)) return m;
            }
        }
        return null;
    }

    private static boolean accepts(Class<?>[] ps, Object[] args, int pass) {
        for (int i = 0; i < ps.length; i++) {
            Object a = args[i];
            Class<?> p = ps[i];
            if (pass == 0) {
                if (a == null) { if (p.isPrimitive()) return false; continue; }
                if (a.getClass() != p) return false;
            } else if (pass == 1) {
                if (a == null) { if (p.isPrimitive()) return false; continue; }
                if (!p.isAssignableFrom(a.getClass())) return false;
            } else {
                if (!unboxMatch(p, a)) return false;
            }
        }
        return true;
    }

    private static boolean unboxMatch(Class<?> p, Object a) {
        if (a == null) return !p.isPrimitive();
        Class<?> boxed = box(a.getClass());
        if (boxed == null) return false;
        if (p == boxed) return true;
        if (!p.isPrimitive()) return p.isAssignableFrom(boxed);
        Class<?> pb = box(p);
        if (pb == null) return false;

        if (Number.class.isAssignableFrom(pb) && Number.class.isAssignableFrom(boxed)) return true;
        return false;
    }

    private static Class<?> box(Class<?> c) {
        if (c == null) return null;
        if (c == int.class) return Integer.class;
        if (c == long.class) return Long.class;
        if (c == boolean.class) return Boolean.class;
        if (c == double.class) return Double.class;
        if (c == float.class) return Float.class;
        if (c == short.class) return Short.class;
        if (c == byte.class) return Byte.class;
        if (c == char.class) return Character.class;
        return c;
    }

    private static Object instanceOf(Class<?> c, Result r) {
        try {
            Field f = c.getDeclaredField("INSTANCE");
            f.setAccessible(true);
            return f.get(null);
        } catch (Throwable ignore) {
        }
        try {
            Method m = c.getDeclaredMethod("getInstance");
            m.setAccessible(true);
            if (Modifier.isStatic(m.getModifiers())) return m.invoke(null);
        } catch (Throwable ignore) {
        }
        return null;
    }

    public static final class Result {
        public boolean ok;
        public Object value;
        public String sig;
        public String err;
        public final List<String> tried = new ArrayList<String>();
    }

    static String signature(Method m) {
        return simple(m.getReturnType().getName()) + " " + m.getName()
                + "(" + params(m.getParameterTypes()) + ")";
    }

    static String params(Class<?>[] ps) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < ps.length; i++) {
            if (i > 0) b.append(", ");
            b.append(simple(ps[i].getName()));
        }
        return b.toString();
    }

    static String types(Object[] a) {
        if (a == null || a.length == 0) return "";
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < a.length; i++) {
            if (i > 0) b.append(", ");
            b.append(a[i] == null ? "null" : a[i].getClass().getSimpleName());
        }
        return b.toString();
    }

    static String simple(String name) {
        if (name == null) return "?";
        int i = name.lastIndexOf('.');
        return i < 0 ? name : name.substring(i + 1);
    }

    static String staticMark(int mod) {
        return Modifier.isStatic(mod) ? "[static] " : "";
    }

    static String lastSegment(String t) {
        int i = t.lastIndexOf('.');
        return i < 0 ? t : t.substring(i + 1);
    }

    static String capitalize(String s) {
        if (s == null || s.length() == 0) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    static String esc(String s) {
        if (s == null) return "";
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"' || c == '\\') b.append('\\').append(c);
            else if (c == '\n') b.append("\\n");
            else if (c == '\r') b.append("\\r");
            else if (c == '\t') b.append("\\t");
            else if (c < 0x20) b.append(' ');
            else b.append(c);
        }
        return b.toString();
    }

    static String valueToJson(Object v) {
        if (v == null) return "null";
        if (v instanceof Number || v instanceof Boolean) return String.valueOf(v);
        if (v instanceof CharSequence) return "\"" + esc(v.toString()) + "\"";
        return "\"" + esc(simple(v.getClass().getName())) + "@" + esc(String.valueOf(v)) + "\"";
    }

    public static String toJson(Result r) {
        StringBuilder b = new StringBuilder();
        b.append("{\"ok\":").append(r.ok);
        b.append(",\"value\":").append(valueToJson(r.value));
        b.append(",\"sig\":\"").append(esc(r.sig)).append('"');
        if (r.err != null) b.append(",\"err\":\"").append(esc(r.err)).append('"');
        b.append(",\"tried\":[");
        for (int i = 0; i < r.tried.size(); i++) {
            if (i > 0) b.append(',');
            b.append('"').append(esc(r.tried.get(i))).append('"');
        }
        b.append("]}");
        return b.toString();
    }
}
