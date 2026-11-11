package com.cathelperawa.owo.app.script;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;

public final class ScriptSelfCheck {

    private ScriptSelfCheck() {}

    private static String outPath() {
        try {
            File dir = ScriptHost.dir();
            if (dir != null) return new File(dir, "selfcheck.txt").getAbsolutePath();
        } catch (Throwable ignore) {
        }
        return "";
    }

    public static String run() {
        StringBuilder b = new StringBuilder();
        b.append("脚本环境自检\n");
        b.append("时间：").append(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss",
                java.util.Locale.CHINA).format(new java.util.Date())).append('\n');
        b.append("──────────────\n");

        ScriptEngine it = null;
        try {
            
            ScriptApi api = new MockScriptApi("__selfcheck");
            it = new BshEngine();
            it.set("ctx", api);
            ScriptEnv.apply(it);
            ScriptEnv.bindGlobals(it, api);

            check(b, it, "import: JSONObject",
                    "new JSONObject().put(\"a\",1).toString().equals(\"{\\\"a\\\":1}\")");
            check(b, it, "import: Color",
                    "android.graphics.Color.RED == Color.RED");
            check(b, it, "import: StringBuilder 冲突检查",
                    "new StringBuilder().append(\"x\").toString().equals(\"x\")");
            
            check(b, it, "全局: getString 两参",
                    "getString(\"k\",\"d\").equals(\"d\")");

            
            
            check(b, it, "全局: getString 一参",
                    "getString(\"k\") == null || getString(\"k\") != null");
            check(b, it, "全局: getInt",
                    "getInt(\"k\", 7) == 7");
            check(b, it, "全局: getBoolean",
                    "getBoolean(\"k\", true)");
            check(b, it, "全局: sendMsg 两参",
                    "sendMsg(\"p\",\"t\")");
            check(b, it, "全局: sendMsg 三参",
                    "sendMsg(2,\"p\",\"t\")");
            check(b, it, "全局: getClassLoader",
                    "getClassLoader() != null");
            check(b, it, "全局: getNowActivity",
                    "getNowActivity() == null");   

            checkDex(b);
            checkLoadJar(b);

            try {
                String p = new MockScriptApi("__selfcheck").probe(
                        "com.tencent.common.app.BaseApplicationImpl", "getApplication");
                if (p != null && p.indexOf("找不到类") < 0) {
                    b.append("OK|probe 宿主类|").append(oneLine(p)).append('\n');
                } else {
                    b.append("NO|probe 宿主类|").append(oneLine(p)).append('\n');
                }
            } catch (Throwable t) {
                b.append("NO|probe 宿主类|").append(t).append('\n');
            }

        } catch (Throwable t) {
            b.append("NO|环境装配|").append(t.getClass().getSimpleName())
                    .append(": ").append(t.getMessage()).append('\n');
        } finally {
            if (it != null) {
                try { it.close(); } catch (Throwable ignore) { }
            }
        }

        b.append("──────────────\n");
        String text = b.toString();
        try {
            com.cathelperawa.owo.app.core.HookLog.i("[自检]\n" + text);
        } catch (Throwable ignore) {
        }
        try {
            String p = outPath();
            if (p.length() > 0) {
                FileOutputStream fos = new FileOutputStream(p, false);
                OutputStreamWriter w = new OutputStreamWriter(fos, "UTF-8");
                w.write(text);
                w.close();
                fos.close();
            }
        } catch (Throwable ignore) {
        }
        return text;
    }

    private static void check(StringBuilder b, ScriptEngine it, String name, String expr) {
        if (expr == null) {          
            try {
                Object r = it.eval("getString(\"k\")");
                b.append("OK|").append(name).append("|返回=").append(r)
                        .append(" (").append(r == null ? "null" : r.getClass().getSimpleName()).append(')')
                        .append('\n');
            } catch (Throwable t) {
                b.append("NO|").append(name).append('|').append(t.getClass().getSimpleName())
                        .append(": ").append(t.getMessage()).append('\n');
            }
            return;
        }
        try {
            Object r = it.eval(expr);
            boolean ok = (r instanceof Boolean) ? ((Boolean) r).booleanValue() : (r != null);
            b.append(ok ? "OK|" : "NO|").append(name).append('|')
                    .append(ok ? "通过" : "结果=" + r).append('\n');
        } catch (Throwable t) {
            String m = String.valueOf(t.getMessage());
            if (m.length() > 160) m = m.substring(0, 160) + "…";
            b.append("NO|").append(name).append('|')
                    .append(t.getClass().getSimpleName()).append(": ").append(m).append('\n');
        }
    }

    private static String oneLine(String s) {
        if (s == null) return "null";
        String r = s.replace('\n', ' ').trim();
        return r.length() > 200 ? r.substring(0, 200) + "…" : r;
    }

    private static ClassLoader hostLoader() {
        try {
            ClassLoader cl = com.cathelperawa.owo.app.bridge.QQEnv.classLoader();
            if (cl != null) return cl;
        } catch (Throwable ignore) {
        }
        return ScriptSelfCheck.class.getClassLoader();
    }

    private static final byte[] SELFCHECK_CLASS = b64("yv66vgAAADQAGQoACAAOBwAPCgACAA4IABAKAAIAEQoAAgASBwATBwAUAQAGPGluaXQ+AQADKClWAQAEQ29kZQEABHBpbmcBACYoTGphdmEvbGFuZy9TdHJpbmc7KUxqYXZhL2xhbmcvU3RyaW5nOwwACQAKAQAXamF2YS9sYW5nL1N0cmluZ0J1aWxkZXIBAAVwb25nOgwAFQAWDAAXABgBAA5TZWxmQ2hlY2tQcm9iZQEAEGphdmEvbGFuZy9PYmplY3QBAAZhcHBlbmQBAC0oTGphdmEvbGFuZy9TdHJpbmc7KUxqYXZhL2xhbmcvU3RyaW5nQnVpbGRlcjsBAAh0b1N0cmluZwEAFCgpTGphdmEvbGFuZy9TdHJpbmc7ACEABwAIAAAAAAACAAEACQAKAAEACwAAABEAAQABAAAABSq3AAGxAAAAAAAJAAwADQABAAsAAAAgAAIAAQAAABS7AAJZtwADEgS2AAUqtgAFtgAGsAAAAAAAAA==");

    private static byte[] b64(String s) {
        try {
            return android.util.Base64.decode(s, android.util.Base64.NO_WRAP);
        } catch (Throwable t) {
            return null;
        }
    }

    private static void checkDex(StringBuilder b) {
        try {
            Class<?> c = ScriptJar.defineClass("SelfCheckProbe", SELFCHECK_CLASS);
            if (c == null) {
                b.append("NO|dx 转 dex|defineClass 返回 null（看 logcat 的 [jar] 日志）\n");
                return;
            }
            java.lang.reflect.Method m = c.getMethod("ping", String.class);
            Object r = m.invoke(null, "ok");
            b.append("OK|dx 转 dex|").append(String.valueOf(r)).append('\n');
        } catch (Throwable t) {
            b.append("NO|dx 转 dex|").append(t.getClass().getSimpleName())
                    .append(": ").append(t.getMessage()).append('\n');
        }
    }

    private static void checkLoadJar(StringBuilder b) {
        try {
            java.io.File f = ScriptDex.resolve("lib/testlib.jar");
            if (f == null || !f.isFile()) {
                b.append("--|loadJar|跳过（没有 lib/testlib.jar）\n");
                return;
            }
            long t0 = System.currentTimeMillis();
            ClassLoader cl = ScriptJar.load("lib/testlib.jar");
            long cost = System.currentTimeMillis() - t0;
            if (cl == null) {
                b.append("NO|loadJar|ClassLoader 为 null（看 logcat 的 [jar]）\n");
                return;
            }
            Class<?> c = cl.loadClass("com.me.Tool");
            java.lang.reflect.Method m = c.getMethod("hello", String.class);
            Object r = m.invoke(null, "minis");
            b.append("OK|loadJar|").append(r).append(" (").append(cost).append("ms)\n");
        } catch (Throwable t) {
            b.append("NO|loadJar|").append(t.getClass().getSimpleName())
                    .append(": ").append(t.getMessage()).append('\n');
        }
    }
}
