package com.cathelperawa.owo.app.api;

import com.cathelperawa.owo.app.core.HookApi;
import com.cathelperawa.owo.app.bridge.QQEnv;
import com.cathelperawa.owo.app.util.CatLog;
import com.cathelperawa.owo.app.util.Hex;
import com.cathelperawa.owo.app.util.Refl;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class ReflectApi {

    private ReflectApi() {}

    public static Class<?> findClass(String className) {
        return HookApi.findClass(QQEnv.classLoader(), className);
    }

    public static Object hookBefore(String className, String methodName, HookApi.Action action) {
        Class<?> c = findClass(className);
        if (c == null) { CatLog.w("找不到类 " + className); return null; }
        Method m = HookApi.findMethod(c, methodName);
        if (m == null) { CatLog.w("找不到方法 " + methodName); return null; }
        return HookApi.before(m, action);
    }

    public static Object hookAfter(String className, String methodName, HookApi.Action action) {
        Class<?> c = findClass(className);
        if (c == null) { CatLog.w("找不到类 " + className); return null; }
        Method m = HookApi.findMethod(c, methodName);
        if (m == null) { CatLog.w("找不到方法 " + methodName); return null; }
        return HookApi.after(m, action);
    }

    public static int hookAll(String className, String methodName, int paramCount, HookApi.Action action) {
        Class<?> c = findClass(className);
        if (c == null) return 0;
        int n = 0;
        for (Method m : c.getDeclaredMethods()) {
            if (!m.getName().equals(methodName)) continue;
            if (paramCount >= 0 && m.getParameterTypes().length != paramCount) continue;
            HookApi.before(m, action);
            n++;
        }
        return n;
    }

    public static Object hookReplace(String className, String methodName, HookApi.Action action) {
        try {
            Class<?> c = findClass(className);
            Method m = HookApi.findMethod(c, methodName);
            return m == null ? null : HookApi.replace(m, action);
        } catch (Throwable t) {
            CatLog.e("hookReplace 失败", t);
            return null;
        }
    }

    public static Object hookReturn(String className, String methodName, Object value) {
        try {
            Class<?> c = findClass(className);
            Method m = HookApi.findMethod(c, methodName);
            return m == null ? null : HookApi.returnConstant(m, value);
        } catch (Throwable t) {
            CatLog.e("hookReturn 失败", t);
            return null;
        }
    }

    public static void unhook(Object handle) { HookApi.unhook(handle); }

    public static Method findMethodByParams(Class<?> cls, Class<?> ret, Class<?>... params) {
        return HookApi.findMethodByParams(cls, ret, params);
    }

    public static java.util.List<Method> findMethodsByParams(Class<?> cls, int paramCount) {
        return HookApi.findMethodsByParams(cls, paramCount);
    }

    public static Object field(Object o, String name) {
        return Refl.field(o, name);
    }
    public static boolean setField(Object o, String name, Object value) {
        return Refl.setField(o, name, value);
    }
    public static Field findField(Class<?> cls, String name) {
        return Refl.findField(cls, name);
    }

    public static Field findFieldByType(Object o, Class<?> type) {
        return Refl.findFieldByType(o, type);
    }

    public static Field findFieldByType(Class<?> cls, Class<?> type, Class<?> inParent) {
        return Refl.findFieldByType(cls, type, inParent);
    }

    public static Object fieldByType(Object o, Class<?> type) {
        return Refl.fieldByType(o, type);
    }

    public static boolean setFieldByType(Object o, Class<?> type, Object value) {
        return Refl.setFieldByType(o, type, value);
    }
    public static Object call(Object o, String method, Object... args) {
        return Refl.call(o, method, args);
    }
    public static String str(Object o) {
        return Refl.str(o);
    }

    public static String hexEncode(byte[] b) { return Hex.encode(b); }
    public static byte[] hexDecode(String s) { return Hex.decode(s); }
    public static boolean isHex(String s)    { return Hex.isHex(s); }
}
