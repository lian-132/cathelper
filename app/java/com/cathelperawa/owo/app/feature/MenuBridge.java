package com.cathelperawa.owo.app.feature;

import android.content.Context;

import com.cathelperawa.owo.app.core.dex.ClassResolver;

import com.cathelperawa.owo.app.core.HookLog;

import java.lang.reflect.Method;
import java.util.List;

public final class MenuBridge {

    private static final ClassResolver.Spec MENU_ITEM_SPEC =
            new ClassResolver.Spec("menu.item")
                    .candidates("com.tencent.qqnt.aio.menu.d",
                                "com.tencent.mobileqq.utils.dialogutils.QQCustomMenuItem")
                    .nameMatches("(QQCustomMenuItem|\\baio\\.menu\\.[a-z]$)")
                    .validator(new ClassResolver.Validator() {
                        @Override public boolean accept(Class<?> c) {
                            for (java.lang.reflect.Constructor<?> ct : c.getDeclaredConstructors()) {
                                Class<?>[] ps = ct.getParameterTypes();
                                if (ps.length == 2 && ps[0] == Context.class) return true;
                            }
                            return false;
                        }
                    });

    private static volatile Context pendingCtx;
    private static volatile Object lastMsgItem;

    private MenuBridge() {}

    public static void markPending(Context ctx) {
        pendingCtx = ctx;
    }

    public static Context pending() {
        return pendingCtx;
    }

    public static Object createItem(Context ctx, String title, int resId, Runnable onClick) {
        return createItem(ctx, null, title, resId, onClick);
    }

    public static Object createItem(Context ctx, Object aioMsgItem,
                                    String title, int resId, Runnable onClick) {
        try {
            ClassLoader cl = ctx.getClassLoader();

            Class<?> itemCls = ClassResolver.resolve(ctx, cl, MENU_ITEM_SPEC);
            if (itemCls == null) {
                HookLog.w("未找到菜单项类（已尝试候选名 + 特征搜索）");
                return null;
            }
            Object item = null;

            for (java.lang.reflect.Constructor<?> c : itemCls.getDeclaredConstructors()) {
                Class<?>[] ps = c.getParameterTypes();
                if (!containsType(ps, Context.class)) continue;
                item = tryNew(c, ps, ctx, aioMsgItem);
                if (item != null) break;
            }

            if (item == null) {
                for (java.lang.reflect.Constructor<?> c : itemCls.getDeclaredConstructors()) {
                    Class<?>[] ps = c.getParameterTypes();
                    if (containsType(ps, Context.class)) continue;
                    item = tryNew(c, ps, ctx, aioMsgItem);
                    if (item != null) break;
                }
            }
            if (item == null) {
                HookLog.w("菜单项构造全部失败：" + itemCls.getName()
                        + " ctors=" + itemCls.getDeclaredConstructors().length);
                return null;
            }

            setField(item, "title", title);
            setField(item, "a", title);
            installClick(item, onClick);
            return item;
        } catch (Throwable t) {
            HookLog.e("构造菜单项失败", t);
            return null;
        }
    }

    private static void setField(Object item, String name, Object value) {
        try {
            java.lang.reflect.Field f = item.getClass().getDeclaredField(name);
            if (f.getType() == String.class) {
                f.setAccessible(true);
                f.set(item, value);
            }
        } catch (Throwable ignore) {
        }
    }

    private static void installClick(final Object item, final Runnable onClick) {
        try {
            for (Method m : item.getClass().getDeclaredMethods()) {
                Class<?>[] ps = m.getParameterTypes();
                if (m.getReturnType() != void.class || ps.length != 1) continue;
                String pn = ps[0].getName();
                if (!pn.contains("Function") && !pn.contains("Runnable")) continue;
                ClassLoader cl = ps[0].getClassLoader();
                Object proxy = java.lang.reflect.Proxy.newProxyInstance(cl,
                        new Class<?>[]{ps[0]}, new java.lang.reflect.InvocationHandler() {
                            @Override
                            public Object invoke(Object p, Method method, Object[] args) {
                                if ("invoke".equals(method.getName()) || "run".equals(method.getName())) {
                                    onClick.run();
                                }
                                Class<?> rt = method.getReturnType();
                                if (rt == Object.class) return kotlinUnit(cl);
                                if (rt == boolean.class) return Boolean.FALSE;
                                if (rt == int.class) return 0;
                                return null;
                            }
                        });
                m.setAccessible(true);
                m.invoke(item, proxy);
                return;
            }
        } catch (Throwable ignore) {
        }
    }

    private static Object kotlinUnit(ClassLoader cl) {
        try {
            Class<?> u = Class.forName("kotlin.Unit", false, cl);
            return u.getField("INSTANCE").get(null);
        } catch (Throwable t) {
            return null;
        }
    }

    public static String dump(List<?> items) {
        StringBuilder sb = new StringBuilder("菜单项 ").append(items.size()).append(" 个:\n");
        for (Object o : items) {
            if (o == null) continue;
            sb.append("  ").append(o.getClass().getName()).append(" :: ");
            sb.append(com.cathelperawa.owo.app.util.Refl.str(com.cathelperawa.owo.app.util.Refl.field(o, "title")));
            sb.append("\n");
        }
        return sb.toString();
    }

    private static boolean containsType(Class<?>[] ps, Class<?> t) {
        for (Class<?> p : ps) if (p == t) return true;
        return false;
    }

    private static Object tryNew(java.lang.reflect.Constructor<?> c, Class<?>[] ps,
                                 Context ctx, Object aioMsgItem) {
        Object[] args = new Object[ps.length];
        for (int i = 0; i < ps.length; i++) {
            Class<?> p = ps[i];
            if (p == Context.class) args[i] = ctx;
            else if (p == int.class) args[i] = 0;
            else if (p == long.class) args[i] = 0L;
            else if (p == boolean.class) args[i] = Boolean.FALSE;
            else if (p == float.class) args[i] = 0f;
            else if (p == double.class) args[i] = 0d;
            else if (p == short.class) args[i] = (short) 0;
            else if (p == byte.class) args[i] = (byte) 0;
            else if (p == char.class) args[i] = '\0';
            else if (p.isPrimitive()) return null;
            else if (aioMsgItem != null && p.isInstance(aioMsgItem)) args[i] = aioMsgItem;
            else args[i] = null;
        }
        try {
            c.setAccessible(true);
            return c.newInstance(args);
        } catch (Throwable t) {
            HookLog.w("菜单项构造尝试失败 " + c + " :: " + t);
            return null;
        }
    }
}
