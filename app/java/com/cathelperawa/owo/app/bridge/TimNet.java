package com.cathelperawa.owo.app.bridge;

import com.cathelperawa.owo.app.MsgStore;
import com.cathelperawa.owo.app.core.HookLog;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.LinkedHashMap;
import java.util.Map;

public final class TimNet {

    private TimNet() {}

    private static Class<?> CLS_SENDER;
    private static Class<?> CLS_LISTENER;
    private static volatile Method M_WITH;
    private static boolean inited;

    public interface Sink {
        void onResult(boolean ok, String json, String err);
    }

    private static volatile Sink globalSink;
    public static void setSink(Sink s) { globalSink = s; }

    private static void ensureInit() {
        if (inited) return;
        inited = true;
        try {
            ClassLoader cl = QQEnv.classLoader();
            CLS_SENDER = find(cl, "com.tenpay.sdk.net.NetSender");
            CLS_LISTENER = find(cl, "com.tenpay.sdk.paynet.Net$NetListener");
            if (CLS_LISTENER == null) CLS_LISTENER = find(cl, "com.tenpay.sdk.net.NetSender$NetListener");
            HookLog.i("TimNet 初始化: sender=" + (CLS_SENDER != null)
                    + " listener=" + (CLS_LISTENER != null));
        } catch (Throwable t) {
            HookLog.e("TimNet 初始化失败", t);
        }
    }

    public static void dumpStructures() {
        ensureInit();
        try {
            StringBuilder sb = new StringBuilder("【TimNet 结构】\n");
            sb.append(describe(CLS_SENDER));
            sb.append(describe(CLS_LISTENER));
            HookLog.i(sb.toString());
        } catch (Throwable t) {
            HookLog.e("TimNet dump 失败", t);
        }
    }

    private static String describe(Class<?> c) {
        if (c == null) return "  (null)\n";
        StringBuilder sb = new StringBuilder();
        sb.append("=== ").append(c.getName()).append(" ===\n");
        for (Method m : c.getDeclaredMethods()) {
            if (java.lang.reflect.Modifier.isStatic(m.getModifiers())) {
                sb.append("  static ");
            } else sb.append("         ");
            sb.append(m.getReturnType().getSimpleName()).append(" ")
              .append(m.getName()).append("(");
            Class<?>[] ps = m.getParameterTypes();
            for (int i = 0; i < ps.length; i++) {
                if (i > 0) sb.append(", ");
                sb.append(ps[i].getSimpleName());
            }
            sb.append(")\n");
        }
        return sb.toString();
    }

    public static boolean ready() {
        ensureInit();
        return CLS_SENDER != null && CLS_LISTENER != null;
    }

    public static void prewarm() {
        ensureInit();
        if (CLS_SENDER == null) return;
        if (M_WITH == null) {
            Method m = findMethod(CLS_SENDER, "with", 4);
            if (m != null) { m.setAccessible(true); M_WITH = m; }
        }
        HookLog.i("TimNet 预热: with=" + (M_WITH != null) + " listener=" + (CLS_LISTENER != null));
    }

    public static boolean request(String url, Map<String, String> params, final Sink sink) {
        ensureInit();
        if (!ready()) { HookLog.w("TimNet: 环境不全"); return false; }
        String uin = QQEnv.currentUin();
        if (uin == null) { HookLog.w("TimNet: 未登录"); return false; }
        try {
            Map<String, String> p = (params instanceof LinkedHashMap)
                    ? params : new LinkedHashMap<String, String>(params);

            Method with = M_WITH;
            if (with == null) {
                with = findMethod(CLS_SENDER, "with", 4);
                if (with != null) { with.setAccessible(true); M_WITH = with; }
            }
            if (with == null) { HookLog.w("TimNet: 没有 with/4"); return false; }
            Object sender = with.invoke(null, QQEnv.appContext(), url, p, uin);
            if (sender == null) { HookLog.w("TimNet: with 返回 null"); return false; }
            HookLog.i("TimNet sender = " + sender.getClass().getName());

            sender = callChain(sender, "comeFrom", "2");
            sender = callChain(sender, "encrypt", Boolean.TRUE);
            sender = callChain(sender, "tokenID", (String) null);

            Object listener = makeListener(sink != null ? sink : globalSink);
            Method req = findMethodAny(sender.getClass(), "request");
            if (req == null) { HookLog.w("TimNet: 没有 request"); return false; }
            req.setAccessible(true);
            req.invoke(sender, listener);
            HookLog.i("TimNet 已发: url=" + url + " n=" + p.size());
            return true;
        } catch (Throwable t) {
            HookLog.e("TimNet.request 失败", t);
            return false;
        }
    }

    private static Object callChain(Object o, String name, Object arg) {
        if (o == null) return null;
        try {
            for (Method m : o.getClass().getMethods()) {
                if (!m.getName().equals(name)) continue;
                if (m.getParameterTypes().length != 1) continue;
                m.setAccessible(true);
                Object r = m.invoke(o, arg);
                return r == null ? o : r;
            }
            for (Method m : o.getClass().getDeclaredMethods()) {
                if (!m.getName().equals(name)) continue;
                if (m.getParameterTypes().length != 1) continue;
                m.setAccessible(true);
                Object r = m.invoke(o, arg);
                return r == null ? o : r;
            }
            HookLog.w("TimNet: 没有链式方法 " + name);
        } catch (Throwable t) {
            HookLog.e("TimNet 链式失败 " + name, t);
        }
        return o;
    }

    private static Object makeListener(final Sink sink) {
        try {
            return Proxy.newProxyInstance(CLS_LISTENER.getClassLoader(),
                    new Class<?>[]{CLS_LISTENER}, new InvocationHandler() {
                @Override public Object invoke(Object proxy, Method method, Object[] args) {
                    String n = method.getName();
                    if ("toString".equals(n)) return "TimNetListener";
                    if ("hashCode".equals(n)) return System.identityHashCode(proxy);
                    if ("equals".equals(n)) return proxy == (args != null && args.length > 0 ? args[0] : null);
                    try {
                        String s1 = (args != null && args.length > 0 && args[0] != null)
                                ? String.valueOf(args[0]) : null;
                        Object j = (args != null && args.length > 1) ? args[1] : null;
                        String js = (j == null) ? null : String.valueOf(j);
                        HookLog.i("TimNet 回调 " + n + " : " + s1);
                        if (!"onSuccess".equals(n)) {

                            try { MsgStore.add("[TimNet失败] " + n + "\n  arg0=" + s1
                                    + "\n  arg1=" + js); } catch (Throwable ignore) {}
                        }
                        if ("onSuccess".equals(n)) {
                            if (sink != null) sink.onResult(true, js, null);
                        } else {
                            if (sink != null) sink.onResult(false, js, n + ":" + s1);
                        }
                    } catch (Throwable t) {
                        HookLog.e("TimNet 回调异常", t);
                    }
                    return null;
                }
            });
        } catch (Throwable t) {
            HookLog.e("TimNet makeListener 失败", t);
            return null;
        }
    }

    private static Class<?> find(ClassLoader cl, String name) {
        try { return Class.forName(name, false, cl); } catch (Throwable ignore) {}
        try { return de.robv.android.xposed.XposedHelpers.findClassIfExists(name, cl); }
        catch (Throwable ignore) {}
        return null;
    }

    private static Method findMethod(Class<?> cls, String name, int argc) {
        if (cls == null) return null;
        for (Method m : cls.getDeclaredMethods()) {
            if (m.getName().equals(name) && m.getParameterTypes().length == argc) return m;
        }
        return null;
    }

    private static Method findMethodAny(Class<?> cls, String name) {
        if (cls == null) return null;
        Class<?> c = cls;
        while (c != null && c != Object.class) {
            for (Method m : c.getDeclaredMethods()) {
                if (m.getName().equals(name)) return m;
            }
            c = c.getSuperclass();
        }
        return null;
    }
}
