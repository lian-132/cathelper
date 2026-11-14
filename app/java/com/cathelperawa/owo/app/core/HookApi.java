package com.cathelperawa.owo.app.core;

import android.content.Context;

import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public final class HookApi {

    private HookApi() {}

    public interface Action {
        void call(XC_MethodHook.MethodHookParam p) throws Throwable;
    }

    public static Object before(Member m, final Action a) {
        return before(m, a, null);
    }

    public static Object before(Member m, final Action a, final HookItemBase owner) {
        if (m == null) return null;
        return XposedBridge.hookMethod(m, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam p) {
                try {
                    a.call(p);
                } catch (Throwable t) {
                    reportFailure("before", m, owner, t);
                }
            }
        });
    }

    public static Object after(Member m, final Action a) {
        return after(m, a, null);
    }

    public static Object after(Member m, final Action a, final HookItemBase owner) {
        if (m == null) return null;
        return XposedBridge.hookMethod(m, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam p) {
                try {
                    a.call(p);
                } catch (Throwable t) {
                    reportFailure("after", m, owner, t);
                }
            }
        });
    }

    private static void reportFailure(String phase, Member m,
                                      HookItemBase owner, Throwable raw) {
        String site = phase + ":" + (m == null ? "?" : m.getName());
        String feature = owner == null ? null : owner.featureName();

        Throwable t = unwrap(raw);
        RuntimeErrors.record(feature, site, t);
        try {
            if (owner != null) owner.noteRuntimeError(t);
        } catch (Throwable ignore) {

        }
        HookLog.e(site + " 运行期异常", t);
    }

    private static Throwable unwrap(Throwable t) {
        Throwable cur = t;
        for (int i = 0; i < 8 && cur != null; i++) {
            if (!(cur instanceof java.lang.reflect.InvocationTargetException)
                    && !(cur instanceof java.lang.reflect.UndeclaredThrowableException)) {
                return cur;
            }
            Throwable c = cur.getCause();
            if (c == null || c == cur) return cur;
            cur = c;
        }
        return cur == null ? t : cur;
    }

    public static Object replace(Member m, final Action a) {
        if (m == null) return null;
        return XposedBridge.hookMethod(m, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam p) {
                try {
                    p.setResult(null);
                    a.call(p);
                } catch (Throwable t) {
                    HookLog.e("replace hook 异常", t);
                }
            }
        });
    }

    public static Object returnConstant(Member m, final Object value) {
        return replace(m, new Action() {
            @Override
            public void call(XC_MethodHook.MethodHookParam p) {
                p.setResult(value);
            }
        });
    }

    public static void unhook(Object handle) {
        if (handle instanceof XC_MethodHook.Unhook) {
            ((XC_MethodHook.Unhook) handle).unhook();
        }
    }

    public static Class<?> findClass(ClassLoader cl, String... names) {
        for (String n : names) {
            Class<?> c = XposedHelpers.findClassIfExists(n, cl);
            if (c != null) return c;
        }

        for (String n : names) {
            if (n == null || n.indexOf('$') < 0) continue;
            int cut = n.lastIndexOf('$');
            if (cut <= 0) continue;
            String outerName = n.substring(0, cut);
            if (outerName.endsWith("$")) continue;
            try {
                Class<?> outer = XposedHelpers.findClassIfExists(outerName, cl);
                if (outer != null) {
                    hookLogInfo("findClass 兜底：合成内部类 " + n + " → 外部类 " + outerName);
                    return outer;
                }
            } catch (Throwable ignore) {
            }
        }
        return null;
    }

    private static void hookLogInfo(String msg) {
        try {
            HookLog.i(msg);
        } catch (Throwable ignore) {
        }
    }

    public static Class<?> findClassCached(ClassLoader cl, String... names) {
        if (names == null || names.length == 0) return null;
        StringBuilder sb = new StringBuilder("C:");
        for (String n : names) sb.append(n).append(';');
        String ck = sb.toString();
        Class<?> hit = com.cathelperawa.owo.app.util.ReflectClassCache.get(ck);
        if (hit != null) return hit;
        if (com.cathelperawa.owo.app.util.ReflectClassCache.isMiss(ck)) return null;
        Class<?> c = findClass(cl, names);
        com.cathelperawa.owo.app.util.ReflectClassCache.put(ck, c);
        return c;
    }

    public static Method findMethod(Class<?> cls, String name) {
        if (cls == null) return null;
        for (Method m : cls.getDeclaredMethods()) {
            if (m.getName().equals(name)) return m;
        }
        return null;
    }

    public static Method findMethodByParams(Class<?> cls, Class<?> ret, Class<?>... params) {
        if (cls == null) return null;

        String ck = com.cathelperawa.owo.app.util.ReflectCache
                .key("M", cls, ret, params);
        Method hit = com.cathelperawa.owo.app.util.ReflectCache.method(ck);
        if (hit != null) return hit;

        if (com.cathelperawa.owo.app.util.ReflectCache.isMethodMiss(ck)) return null;

        try {
            com.cathelperawa.owo.app.util.MethodPicker p =
                    com.cathelperawa.owo.app.util.MethodPicker.forClass(cls)
                            .params(params)
                            .returns(ret)

                            .scope(com.cathelperawa.owo.app.util.SearchScope.SUPERCLASSES);
            Method m = p.pick();
            if (m != null) {
                return com.cathelperawa.owo.app.util.ReflectCache.putMethod(ck, m);
            }

        } catch (Throwable t) {
            HookLog.w("MethodPicker 打分失败，回退老逻辑: " + t);
        }
        Method legacy = findMethodByParamsLegacy(cls, ret, params);

        if (legacy != null) {
            return com.cathelperawa.owo.app.util.ReflectCache.putMethod(ck, legacy);
        }
        com.cathelperawa.owo.app.util.ReflectCache.putMiss(ck);
        return null;
    }

    private static Method findMethodByParamsLegacy(Class<?> cls, Class<?> ret, Class<?>... params) {
        if (cls == null) return null;
        for (Method m : cls.getDeclaredMethods()) {
            if (ret != null && !ret.isAssignableFrom(m.getReturnType())) continue;
            Class<?>[] ps = m.getParameterTypes();
            if (ps.length != params.length) continue;
            boolean ok = true;
            for (int i = 0; i < ps.length; i++) {
                if (params[i] != null && !params[i].isAssignableFrom(ps[i])) {

                    if (!ps[i].isAssignableFrom(params[i])) {
                        ok = false;
                        break;
                    }
                }
            }
            if (ok) return m;
        }
        return null;
    }

    public static List<Method> findMethodsByParams(Class<?> cls, int paramCount) {
        List<Method> out = new ArrayList<Method>();
        if (cls == null) return out;
        for (Method m : cls.getDeclaredMethods()) {
            if (m.getParameterTypes().length == paramCount) out.add(m);
        }
        return out;
    }

    public static Constructor<?> findCtor(Class<?> cls, Class<?>... params) {
        if (cls == null) return null;
        for (Constructor<?> c : cls.getDeclaredConstructors()) {
            Class<?>[] ps = c.getParameterTypes();
            if (ps.length != params.length) continue;
            boolean ok = true;
            for (int i = 0; i < ps.length; i++) {
                if (params[i] != null && !params[i].isAssignableFrom(ps[i]) && !ps[i].isAssignableFrom(params[i])) {
                    ok = false;
                    break;
                }
            }
            if (ok) return c;
        }
        return null;
    }
}
