package com.cathelperawa.owo.app.script;

import com.cathelperawa.owo.app.core.HookApi;
import com.cathelperawa.owo.app.core.HookLog;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class MethodHook {

    private static final String[] BLOCKED = {
            "java.", "javax.", "sun.", "dalvik.", "libcore.",
            "android.", "androidx.", "com.android.",
            "com.google.android.", "kotlin."
    };

    private static final String[] PROTECTED = {
            "com.cathelperawa.owo.app.core.SafeMode",
            "com.cathelperawa.owo.app.core.XposedEntry",
            "com.cathelperawa.owo.app.script.BootGuard",
            "com.cathelperawa.owo.app.script.MethodHook",
            "com.cathelperawa.owo.app.script.ScriptRisk",
            "com.cathelperawa.owo.app.script.ScriptTrust",
    };

    public interface Handler {
        void call(Ctx c);
    }

    public static final class Ctx {
        public final String target;
        private final Object[] args;
        private final Object thiz;
        private Object result;
        private boolean cancelled;
        private boolean resultSet;

        Ctx(String target, Object[] args, Object thiz) {
            this.target = target;
            this.args = args;
            this.thiz = thiz;
        }

        public Object thisObject() { return thiz; }

        public int argc() { return args == null ? 0 : args.length; }

        public Object arg(int i) {
            if (args == null || i < 0 || i >= args.length) return null;
            return args[i];
        }

        public String argStr(int i) {
            Object v = arg(i);
            return v == null ? null : String.valueOf(v);
        }

        public void arg(int i, Object v) {
            if (args == null || i < 0 || i >= args.length) return;
            args[i] = v;
        }

        public void cancel(Object result) {
            this.cancelled = true;
            this.result = result;
        }

        public Object result() { return result; }

        public boolean cancelled() { return cancelled; }

        public void setResult(Object v) {
            this.result = v;
            this.resultSet = true;
        }

        public boolean resultSet() { return resultSet; }

        void seed(Object r) {
            if (!resultSet) this.result = r;
        }
    }

    private static final class Entry {
        final String script;
        final Handler handler;

        final String mode;

        Entry(String script, Handler handler, String mode) {
            this.script = script;
            this.handler = handler;
            this.mode = mode == null ? "before" : mode;
        }
    }

    private static final Map<String, List<Entry>> MAP =
            new LinkedHashMap<String, List<Entry>>();

    private MethodHook() {}

    public static String install(String script, String target, Handler h) {
        return install(script, target, "before", h);
    }

    public static String install(String script, String target, String mode, Handler h) {
        if (target == null || h == null) return "参数为空";
        if (mode == null || mode.length() == 0) mode = "before";
        mode = mode.toLowerCase();
        if (mode.indexOf("both") >= 0) mode = "before after";
        if (mode.indexOf("after") < 0) mode = "before" + (mode.indexOf("before") < 0 ? "" : "");
        int hash = target.indexOf('#');
        if (hash <= 0) return "格式要写成 类名#方法名";

        String clsName = target.substring(0, hash).trim();
        String methodName = target.substring(hash + 1).trim();
        for (int i = 0; i < BLOCKED.length; i++) {
            if (clsName.startsWith(BLOCKED[i])) {
                return "不能改系统层（" + BLOCKED[i] + "*）。TIM 和我们自己的类都可以，系统不行";
            }
        }

        for (int i = 0; i < PROTECTED.length; i++) {
            String p = PROTECTED[i];
            if (clsName.equals(p) || clsName.startsWith(p + "$")) {
                boolean doubleOpen = false;
                try {
                    doubleOpen = ScriptTrust.full() && ScriptTrust.kernelTrust();
                } catch (Throwable ignore) {
                }
                if (!doubleOpen) {
                    return "「" + p + "」属于受保护的核心类（安全模式 / 加载守卫 / 闸门）。"
                            + "要改得同时打开「完全信任」和「允许改脚本系统」";
                }
                HookLog.w("[方法重写] ⚠ 脚本[" + script + "] 接管保护区 "
                        + clsName + "#" + methodName + "（已留痕）");
            }
        }

        if ("<init>".equals(methodName) || "<clinit>".equals(methodName)) {
            if (!ModLevel.dangerAllowed()) {
                return "改构造器/静态初始化属于危险操作（改错会让模块黑屏起不来）。"
                        + "确实要用的话，先去脚本页打开「允许改构造器」开关";
            }
        }

        try {
            Class<?> cls = Class.forName(clsName, false,
                    MethodHook.class.getClassLoader());
            List<Method> found = new ArrayList<Method>();
            for (Method m : cls.getDeclaredMethods()) {
                if (m.getName().equals(methodName)) found.add(m);
            }
            if (found.isEmpty()) {
                return "类 " + clsName + " 里没有方法 " + methodName;
            }

            synchronized (MAP) {
                List<Entry> list = MAP.get(target);
                if (list == null) {
                    list = new ArrayList<Entry>();
                    MAP.put(target, list);
                }
                list.add(new Entry(script, h, mode));
            }

            final String m2 = mode;
            for (int i = 0; i < found.size(); i++) {
                final Method m = found.get(i);
                try {
                    if (m2.indexOf("before") >= 0) {
                        HookApi.before(m, new HookApi.Action() {
                            @Override public void call(
                                    de.robv.android.xposed.XC_MethodHook.MethodHookParam p) {
                                try {
                                    Ctx c = new Ctx(target, p.args, p.thisObject);
                                    runAll(target, c);
                                    if (c.cancelled()) p.setResult(c.result());
                                } catch (Throwable t) {
                                    ModHealth.record("method:" + target, String.valueOf(t));
                                    HookLog.e("[方法重写] " + target + " 出错", t);
                                }
                            }
                        }, null);
                    }
                    if (m2.indexOf("after") >= 0) {
                        HookApi.after(m, new HookApi.Action() {
                            @Override public void call(
                                    de.robv.android.xposed.XC_MethodHook.MethodHookParam p) {
                                try {
                                    Ctx c = new Ctx(target, p.args, p.thisObject);
                                    c.seed(p.getResult());
                                    runAll(target, c);
                                    if (c.resultSet() || c.cancelled()) p.setResult(c.result());
                                } catch (Throwable t) {
                                    ModHealth.record("method:" + target, String.valueOf(t));
                                    HookLog.e("[方法重写] " + target + " 出错", t);
                                }
                            }
                        }, null);
                    }
                } catch (Throwable t) {
                    HookLog.w("[方法重写] hook 失败 " + m + "：" + t);
                }
            }
            HookLog.i("[方法重写] " + script + " 接管 " + target
                    + "（" + found.size() + " 个重载）");
            return null;
        } catch (ClassNotFoundException e) {
            return "找不到类：" + clsName;
        } catch (Throwable t) {
            return "挂载失败：" + t;
        }
    }

    private static void runAll(String target, Ctx c) {
        List<Entry> hs;
        synchronized (MAP) {
            List<Entry> l = MAP.get(target);
            hs = l == null ? null : new ArrayList<Entry>(l);
        }
        if (hs == null) return;
        long deadline = System.currentTimeMillis() + 50;
        for (int k = 0; k < hs.size(); k++) {
            if (System.currentTimeMillis() > deadline) {
                HookLog.w("[方法重写] " + target + " 超预算，跳过剩余");
                break;
            }
            hs.get(k).handler.call(c);
        }
    }

    public static int removeByScript(String script) {
        if (script == null) return 0;
        int n = 0;
        synchronized (MAP) {
            List<String> empty = new ArrayList<String>();
            for (Map.Entry<String, List<Entry>> e : MAP.entrySet()) {
                List<Entry> list = e.getValue();
                for (int i = list.size() - 1; i >= 0; i--) {
                    if (script.equals(list.get(i).script)) {
                        list.remove(i);
                        n++;
                    }
                }
                if (list.isEmpty()) empty.add(e.getKey());
            }
            for (int i = 0; i < empty.size(); i++) MAP.remove(empty.get(i));
        }
        return n;
    }

    public static List<String> targets() {
        synchronized (MAP) {
            return new ArrayList<String>(MAP.keySet());
        }
    }

    public static int count() {
        synchronized (MAP) {
            int n = 0;
            for (List<Entry> l : MAP.values()) n += l.size();
            return n;
        }
    }

    public static int clearAll() {
        synchronized (MAP) {
            int n = 0;
            for (List<Entry> l : MAP.values()) n += l.size();
            MAP.clear();
            return n;
        }
    }
}
