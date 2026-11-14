package com.cathelperawa.owo.app.bridge;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.cathelperawa.owo.app.core.HookLog;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public final class TimServlet {

    private TimServlet() {}

    public interface Callback {

        void onReceive(int type, boolean success, Bundle data);
    }

    private static final String[] SERVLET_CANDIDATES = {

            "com.tencent.mobileqq.qwallet.e.f",
            "com.tencent.mobileqq.qwallet.impl.servlet.QWalletCommonServlet",
            "com.tencent.mobileqq.qwallet.impl.servlet.QWalletTempServlet",
            "com.tencent.mobileqq.activity.qwallet.QWalletCommonServlet",
            "com.tenpay.sdk.net.gateway.QWalletGatewayServlet",
            "com.tencent.mobileqq.portal.RedPacketServlet",

            "com.tencent.mobileqq.qwallet.servlet.p",
            "com.tencent.mobileqq.qwallet.servlet.q",
            "com.tencent.mobileqq.qwallet.e.p",
            "pl2.p", "lm2.p", "co2.q", "ce2.q", "de2.q",
            "g72.q", "nt2.q", "fy2.q",
    };

    private static String servletOverride() {
        try {
            android.content.Context c = QQEnv.appContext();
            if (c == null) return null;
            String v = com.cathelperawa.owo.app.core.HookConfig.get(c, "rp_servlet", "");
            return (v == null || v.trim().isEmpty()) ? null : v.trim();
        } catch (Throwable t) {
            return null;
        }
    }

    private static Class<?> servletClass;
    private static String servletName;

    public static Class<?> servletClass() {

        String ov = servletOverride();
        if (ov != null) {
            try {
                Class<?> c = Class.forName(ov, false, QQEnv.classLoader());
                if (c != null) {
                    if (!ov.equals(servletName)) {
                        servletClass = c;
                        servletName = ov;
                        HookLog.i("TimServlet: 手动指定 " + ov);
                    }
                    return c;
                }
            } catch (Throwable t) {
                HookLog.w("TimServlet: 指定的类不存在 " + ov);
            }
        }
        if (servletClass != null) return servletClass;
        ClassLoader cl = QQEnv.classLoader();
        for (String n : SERVLET_CANDIDATES) {
            try {
                Class<?> c = Class.forName(n, false, cl);
                if (c != null) {
                    servletClass = c;
                    servletName = n;
                    HookLog.i("TimServlet: 命中 Servlet = " + n);
                    return c;
                }
            } catch (Throwable ignore) {
            }
        }
        HookLog.w("TimServlet: 没找到任何 Servlet 候选类");

        try {
            Class<?> healed = findByString(cl);
            if (healed != null) {
                servletClass = healed;
                servletName = healed.getName();
                HookLog.i("TimServlet: 靠字符串常量自愈 → " + servletName);
                return healed;
            }
        } catch (Throwable t) {
            HookLog.e("TimServlet: 字符串自愈失败", t);
        }
        return null;
    }

    private static Class<?> findByString(ClassLoader cl) {
        android.content.Context ctx = QQEnv.appContext();
        if (ctx == null) return null;
        com.cathelperawa.owo.app.core.dex.DexIndex ix =
                com.cathelperawa.owo.app.core.dex.DexIndexLoader.get(ctx, true);
        if (ix == null) return null;
        java.util.List<String> hits = ix.classesUsingString("qwallet_pb_handle_trpc_error");
        for (String n : hits) {
            try {
                Class<?> c = Class.forName(n, false, cl);

                for (Class<?> k = c; k != null && k != Object.class; k = k.getSuperclass()) {
                    if (k.getName().endsWith("MSFServlet")) return c;
                }
            } catch (Throwable ignore) {
            }
        }
        return null;
    }

    public static String servletClassName() {
        if (servletName == null) servletClass();
        return servletName;
    }

    public static boolean setServletClass(String className) {
        try {
            Class<?> c = Class.forName(className, false, QQEnv.classLoader());
            if (c == null) return false;
            servletClass = c;
            servletName = className;
            HookLog.i("TimServlet: 手动指定 " + className);
            return true;
        } catch (Throwable t) {
            HookLog.e("setServletClass 失败 " + className, t);
            return false;
        }
    }

    public static final long DEFAULT_TIMEOUT = 30000L;

    public static boolean send(String cmd, byte[] data, Callback cb) {
        return send(cmd, data, DEFAULT_TIMEOUT, cb);
    }

    public static boolean send(String cmd, byte[] data, long timeout, Callback cb) {
        Class<?> servlet = servletClass();
        if (servlet == null) return false;
        return send(cmd, data, servlet, timeout, cb);
    }

    public static boolean send(String cmd, byte[] data, Class<?> servlet,
                               long timeout, Callback cb) {
        if (cmd == null || data == null || servlet == null) {
            HookLog.w("TimServlet.send: 缺参");
            return false;
        }
        Context ctx = QQEnv.appContext();
        Object app = QQEnv.appRuntime();
        if (ctx == null || app == null) {
            HookLog.w("TimServlet.send: 拿不到 Context / AppRuntime");
            return false;
        }
        try {

            Class<?> clsNewIntent = CLS_NEW_INTENT;
            if (clsNewIntent == null) {
                clsNewIntent = findClass("mqq.app.NewIntent");
                CLS_NEW_INTENT = clsNewIntent;
            }
            if (clsNewIntent == null) { HookLog.w("没找到 mqq.app.NewIntent"); return false; }
            Object intent = newInstance(clsNewIntent, ctx, servlet);
            if (!(intent instanceof Intent)) { HookLog.w("NewIntent 构造失败"); return false; }
            Intent it = (Intent) intent;

            it.putExtra("cmd", cmd);
            it.putExtra("data", data);
            it.putExtra("timeout", timeout);

            if (cb != null) {
                Object obs = makeObserver(cb);
                if (obs != null) {
                    Method setObserver = M_SET_OBSERVER;
                    if (setObserver == null) {
                        setObserver = findMethod(it.getClass(), "setObserver", 1);
                        if (setObserver != null) { setObserver.setAccessible(true); M_SET_OBSERVER = setObserver; }
                    }
                    if (setObserver != null) {
                        setObserver.invoke(it, obs);
                    } else {
                        HookLog.w("NewIntent 没有 setObserver");
                    }
                }
            }

            Method startServlet = M_START_SERVLET;
            if (startServlet == null) {
                startServlet = findMethod(app.getClass(), "startServlet", 1);
                if (startServlet == null) {

                    startServlet = findMethodAny(app.getClass(), "startServlet");
                }
                if (startServlet != null) { startServlet.setAccessible(true); M_START_SERVLET = startServlet; }
            }
            if (startServlet == null) { HookLog.w("AppRuntime 没有 startServlet"); return false; }
            startServlet.invoke(app, it);
            HookLog.i("TimServlet 已发: cmd=" + cmd + " len=" + data.length
                    + " servlet=" + servlet.getName());
            return true;
        } catch (Throwable t) {
            HookLog.e("TimServlet.send 失败", t);
            return false;
        }
    }

    private static volatile Class<?> CLS_NEW_INTENT;
    private static volatile Method M_SET_OBSERVER;
    private static volatile Method M_START_SERVLET;

    private static volatile Class<?> clsObserver;

    private static Class<?> observerClass() {
        if (clsObserver != null) return clsObserver;
        clsObserver = findClass("mqq.observer.BusinessObserver");
        return clsObserver;
    }

    private static Object makeObserver(final Callback cb) {
        Class<?> iface = observerClass();
        if (iface == null) { HookLog.w("没找到 mqq.observer.BusinessObserver"); return null; }
        ClassLoader cl = iface.getClassLoader();
        try {
            return Proxy.newProxyInstance(cl, new Class<?>[]{iface}, new InvocationHandler() {
                @Override
                public Object invoke(Object proxy, Method method, Object[] args) {
                    String name = method.getName();

                    if ("toString".equals(name)) return "TimServletObserver";
                    if ("hashCode".equals(name)) return System.identityHashCode(proxy);
                    if ("equals".equals(name)) return proxy == (args != null && args.length > 0 ? args[0] : null);
                    if ("onReceive".equals(name)) {
                        try {
                            int type = 0;
                            boolean ok = false;
                            Bundle bundle = null;
                            if (args != null) {

                                if (args.length > 0 && args[0] instanceof Number) {
                                    type = ((Number) args[0]).intValue();
                                }
                                if (args.length > 1 && args[1] instanceof Boolean) {
                                    ok = ((Boolean) args[1]).booleanValue();
                                }
                                for (Object a : args) {
                                    if (a instanceof Bundle) { bundle = (Bundle) a; break; }
                                }
                            }
                            cb.onReceive(type, ok, bundle);
                        } catch (Throwable t) {
                            HookLog.e("Servlet 回调异常", t);
                        }
                    }

                    return null;
                }
            });
        } catch (Throwable t) {
            HookLog.e("makeObserver 失败", t);
            return null;
        }
    }

    public static byte[] responseBytes(Bundle bundle) {
        if (bundle == null) return null;
        String[] keys = {"rsp_bytes", "data", "rsp", "bytes", "resp"};
        for (String k : keys) {
            try {
                byte[] b = bundle.getByteArray(k);
                if (b != null && b.length > 0) return b;
            } catch (Throwable ignore) {
            }
        }
        return null;
    }

    public static String dumpBundle(Bundle bundle) {
        if (bundle == null) return "(null)";
        StringBuilder sb = new StringBuilder("Bundle{");
        try {
            for (String k : bundle.keySet()) {
                Object v = bundle.get(k);
                sb.append("\n  ").append(k).append(" = ");
                if (v instanceof byte[]) {
                    sb.append("byte[").append(((byte[]) v).length).append("]");
                } else {
                    sb.append(v);
                }
            }
        } catch (Throwable t) {
            sb.append(" dump失败:").append(t);
        }
        return sb.append("\n}").toString();
    }

    private static Class<?> findClass(String name) {
        ClassLoader cl = QQEnv.classLoader();
        try {
            return Class.forName(name, false, cl);
        } catch (Throwable ignore) {
        }
        try {
            return de.robv.android.xposed.XposedHelpers.findClassIfExists(name, cl);
        } catch (Throwable ignore) {
        }
        return null;
    }

    private static Method findMethod(Class<?> cls, String name, int argc) {
        Class<?> c = cls;
        while (c != null && c != Object.class) {
            for (Method m : c.getDeclaredMethods()) {
                if (m.getName().equals(name) && m.getParameterTypes().length == argc) return m;
            }
            c = c.getSuperclass();
        }
        return null;
    }

    private static Method findMethodAny(Class<?> cls, String name) {
        Class<?> c = cls;
        while (c != null && c != Object.class) {
            for (Method m : c.getDeclaredMethods()) {
                if (m.getName().equals(name)) return m;
            }
            c = c.getSuperclass();
        }
        return null;
    }

    private static Object newInstance(Class<?> cls, Object... args) {
        if (cls == null) return null;
        Object[] a = (args == null) ? new Object[0] : args;
        for (java.lang.reflect.Constructor<?> c : cls.getDeclaredConstructors()) {
            if (c.getParameterTypes().length != a.length) continue;
            try {
                c.setAccessible(true);
                return c.newInstance(a);
            } catch (Throwable ignore) {
            }
        }
        return null;
    }
}
