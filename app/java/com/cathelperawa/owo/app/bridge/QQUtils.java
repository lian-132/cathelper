package com.cathelperawa.owo.app.bridge;

import com.cathelperawa.owo.app.core.dex.ClassResolver;
import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.cathelperawa.owo.app.core.HookLog;

import java.lang.reflect.Method;

import de.robv.android.xposed.XposedHelpers;

public final class QQUtils {

    private static final Handler UI = new Handler(Looper.getMainLooper());

    private QQUtils() {}

    private static volatile Object sRelation;

    private static final String[] IMPL_CANDIDATES = {
            "com.tencent.relation.common.api.impl.RelationNTUinAndUidApiImpl",
            "com.tencent.qqnt.msg.api.impl.RelationNTUinAndUidApiImpl",
    };

    private static Object relationImpl() {
        if (sRelation != null) return sRelation;
        ClassLoader cl = QQEnv.classLoader();

        try {
            Class<?> iface = Class.forName(
                    "com.tencent.relation.common.api.IRelationNTUinAndUidApi", false, cl);
            Class<?> qroute = ClassResolver.find(cl, "qroute.di", "com.tencent.mobileqq.qroute.QRoute");
            Method api = qroute.getMethod("api", Class.class);
            api.setAccessible(true);
            Object impl = api.invoke(null, iface);
            if (impl != null) { sRelation = impl; return impl; }
        } catch (Throwable ignore) {}

        try {
            Class<?> c = null;
            for (String impl : IMPL_CANDIDATES) {
                c = XposedHelpers.findClassIfExists(impl, cl);
                if (c != null) break;
            }
            if (c != null) { sRelation = XposedHelpers.newInstance(c); return sRelation; }
        } catch (Throwable t) { HookLog.e("RelationApi 实例化失败", t); }
        return null;
    }

    private static String call1(String method, String arg) {
        if (arg == null || arg.isEmpty()) return null;
        try {
            Object impl = relationImpl();
            if (impl == null) return null;
            for (Method m : impl.getClass().getMethods()) {
                Class<?>[] ps = m.getParameterTypes();
                if (m.getName().equals(method) && ps.length == 1 && ps[0] == String.class) {
                    m.setAccessible(true);
                    Object v = m.invoke(impl, arg);
                    return v == null ? null : String.valueOf(v);
                }
            }
        } catch (Throwable t) { HookLog.e(method + " 失败", t); }
        return null;
    }

    public static String uidFromUin(String uin) { return call1("getUidFromUin", uin); }

    public static String uinFromUid(String uid) { return call1("getUinFromUid", uid); }

    public static boolean isFriend(String uin) {
        try {
            Object rt = QQEnv.appRuntime();
            if (rt == null) return false;
            Class<?> impl = XposedHelpers.findClassIfExists(
                    "com.tencent.mobileqq.friend.api.impl.FriendDataServiceImpl",
                    QQEnv.classLoader());
            if (impl == null) return false;
            Method getService = findMethod(impl, "getService", 1);
            if (getService == null) return false;
            getService.setAccessible(true);
            Object svc = getService.invoke(null, rt);
            if (svc == null) return false;
            Method isFriend = findMethod(svc.getClass(), "isFriend", 1);
            if (isFriend == null) return false;
            isFriend.setAccessible(true);
            Object v = isFriend.invoke(svc, uin);
            return Boolean.TRUE.equals(v);
        } catch (Throwable t) {
            return false;
        }
    }

    public static void toast(final String msg) {

        com.cathelperawa.owo.app.bridge.CatToast.show(msg);
    }

    public static void runOnUi(Runnable r) { UI.post(r); }

    private static Method findMethod(Class<?> cls, String name, int argc) {
        if (cls == null) return null;
        while (cls != null && cls != Object.class) {
            for (Method m : cls.getDeclaredMethods()) {
                if (m.getName().equals(name) && m.getParameterTypes().length == argc) return m;
            }
            cls = cls.getSuperclass();
        }
        return null;
    }
}
