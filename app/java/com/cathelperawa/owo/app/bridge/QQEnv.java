package com.cathelperawa.owo.app.bridge;

import com.cathelperawa.owo.app.core.dex.ClassResolver;
import android.app.Activity;
import android.content.Context;

import java.lang.reflect.Method;
import java.util.Map;

import de.robv.android.xposed.XposedHelpers;

public final class QQEnv {

    private static Context appCtx;

    private QQEnv() {}

    public static void setAppContext(Context ctx) { appCtx = ctx; }
    public static Context appContext() { return appCtx; }

    public static ClassLoader classLoader() {
        if (appCtx != null) return appCtx.getClassLoader();
        return QQEnv.class.getClassLoader();
    }

    public static Object appRuntime() {
        try {
            Class<?> bai = XposedHelpers.findClassIfExists(
                    "com.tencent.common.app.BaseApplicationImpl", classLoader());
            if (bai == null) return null;
            Object app = XposedHelpers.callStaticMethod(bai, "getApplication");
            if (app == null) return null;
            Method m = bai.getMethod("getRuntime");
            m.setAccessible(true);
            return m.invoke(app);
        } catch (Throwable t) { return null; }
    }

    public static String currentUin() {
        try {
            Object rt = appRuntime();
            if (rt == null) return null;
            Method m = rt.getClass().getMethod("getCurrentAccountUin");
            m.setAccessible(true);
            Object v = m.invoke(rt);
            return v == null ? null : String.valueOf(v);
        } catch (Throwable t) { return null; }
    }

    public static String currentUid() {
        try {

            String uin = currentUin();
            if (uin == null) return null;
            ClassLoader cl = classLoader();
            Class<?> iface = ClassResolver.find(cl, "relation.uinUidApi",
                    "com.tencent.relation.common.api.IRelationNTUinAndUidApi");
            Class<?> qroute = ClassResolver.find(cl, "qroute.di", "com.tencent.mobileqq.qroute.QRoute");
            Method api = qroute.getMethod("api", Class.class);
            api.setAccessible(true);
            Object impl = api.invoke(null, iface);
            if (impl != null) {
                Method g = iface.getMethod("getUidFromUin", String.class);
                g.setAccessible(true);
                Object v = g.invoke(impl, uin);
                if (v != null) return String.valueOf(v);
            }
        } catch (Throwable t) {

            com.cathelperawa.owo.app.core.HookLog.d("[QQEnv] currentUid QRoute 路径失败: " + t);
        }
        try {
            Object rt = appRuntime();
            if (rt == null) return null;
            String[] names = {"getCurrentUid", "getCurrentUinUid"};
            for (String n : names) {
                for (Method m : rt.getClass().getMethods()) {
                    if (!n.equals(m.getName()) || m.getParameterTypes().length != 0) continue;
                    try {
                        m.setAccessible(true);
                        Object v = m.invoke(rt);
                        if (v != null) return String.valueOf(v);
                    } catch (Throwable ignore) {}
                }
            }
        } catch (Throwable t) {
            com.cathelperawa.owo.app.core.HookLog.d("[QQEnv] currentUid AppRuntime 路径失败: " + t);
        }
        return null;
    }

    public static String currentNickname() {
        try {
            Object rt = appRuntime();
            if (rt == null) return null;
            String[] names = {"getCurrentNickname", "getCurrentNickName", "getNickName"};
            for (String n : names) {
                for (Method m : rt.getClass().getMethods()) {
                    if (!n.equals(m.getName()) || m.getParameterTypes().length != 0) continue;
                    try {
                        m.setAccessible(true);
                        Object v = m.invoke(rt);
                        if (v != null) return String.valueOf(v);
                    } catch (Throwable ignore) {}
                }
            }
        } catch (Throwable ignore) {}
        return null;
    }

    public static Object kernelMsgService() {
        try {
            Object rt = appRuntime();
            if (rt == null) return null;
            Class<?> iks = ClassResolver.find(classLoader(), "qqnt.kernelService",
                    "com.tencent.qqnt.kernel.api.IKernelService");
            if (iks == null) return null;
            Method getSvc = rt.getClass().getMethod("getRuntimeService", Class.class, String.class);
            getSvc.setAccessible(true);
            Object kernel = getSvc.invoke(rt, iks, "");
            if (kernel == null) return null;
            Object impl = callByName(kernel, "getMsgService");
            sMsgServiceImpl = impl;
            if (impl == null) return null;

            try {
                Object nativeSvc = callByName(impl, "getService");
                if (nativeSvc != null) return nativeSvc;
            } catch (Throwable ignore) {}
            return impl;
        } catch (Throwable t) { return null; }
    }

    public static Object kernelService() {
        try {
            Object rt = appRuntime();
            if (rt == null) return null;
            Class<?> iks = ClassResolver.find(classLoader(), "qqnt.kernelService",
                    "com.tencent.qqnt.kernel.api.IKernelService");
            if (iks == null) return null;
            Method m = rt.getClass().getMethod("getRuntimeService", Class.class, String.class);
            m.setAccessible(true);
            return m.invoke(rt, iks, "");
        } catch (Throwable t) {
            return null;
        }
    }

    public static Object buddyService() {
        try {
            Object kernel = kernelService();
            if (kernel == null) return null;
            for (Method m : kernel.getClass().getMethods()) {
                if (!"getBuddyService".equals(m.getName())) continue;
                if (m.getParameterTypes().length != 0) continue;
                m.setAccessible(true);
                Object o = m.invoke(kernel);
                if (o != null) return o;
            }
        } catch (Throwable ignore) {
        }
        return null;
    }

    private static volatile Object sMsgServiceImpl;

    public static Object msgServiceImpl() {
        if (sMsgServiceImpl == null) { try { kernelMsgService(); } catch (Throwable ignore) {} }
        return sMsgServiceImpl;
    }

    private static volatile Object wrapperSession;
    public static void setWrapperSession(Object s) { wrapperSession = s; }
    public static Object wrapperSession() { return wrapperSession; }

    public static Object nativeMsgService() {
        Object s = wrapperSession;
        if (s == null) return null;
        try {
            for (Method m : s.getClass().getMethods()) {
                if ("getMsgService".equals(m.getName()) && m.getParameterTypes().length == 0) {
                    m.setAccessible(true);
                    return m.invoke(s);
                }
            }
        } catch (Throwable ignore) {}
        return null;
    }

    public static Activity topActivity() {
        try {
            Class<?> atCls = Class.forName("android.app.ActivityThread");
            Object at = atCls.getMethod("currentActivityThread").invoke(null);
            java.lang.reflect.Field mActs = atCls.getDeclaredField("mActivities");
            mActs.setAccessible(true);
            Map<?, ?> acts = (Map<?, ?>) mActs.get(at);
            if (acts == null || acts.isEmpty()) return null;
            for (Object rec : acts.values()) {
                if (rec == null) continue;
                java.lang.reflect.Field paused = rec.getClass().getDeclaredField("paused");
                paused.setAccessible(true);
                if (Boolean.TRUE.equals(paused.get(rec))) continue;
                java.lang.reflect.Field act = rec.getClass().getDeclaredField("activity");
                act.setAccessible(true);
                Object a = act.get(rec);
                if (a instanceof Activity) {
                    Activity activity = (Activity) a;
                    if (!activity.isFinishing()) return activity;
                }
            }
        } catch (Throwable ignore) {}
        return null;
    }

    private static Object callByName(Object o, String name) {
        if (o == null) return null;
        Class<?> c = o.getClass();
        while (c != null) {
            for (Method m : c.getDeclaredMethods()) {
                if (!m.getName().equals(name) || m.getParameterTypes().length != 0) continue;
                try { m.setAccessible(true); return m.invoke(o); }
                catch (Throwable ignore) {}
            }
            c = c.getSuperclass();
        }
        return null;
    }
}
