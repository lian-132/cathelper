package com.cathelperawa.owo.app.feature;

import android.content.Context;

import com.cathelperawa.owo.app.core.HookLog;

import java.lang.reflect.Method;
import java.util.List;

public final class MenuBridge {

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
        try {
            ClassLoader cl = ctx.getClassLoader();
            Class<?> itemCls = null;
            for (String n : new String[]{
                    "com.tencent.qqnt.aio.menu.d",
                    "com.tencent.mobileqq.utils.dialogutils.QQCustomMenuItem"}) {
                try {
                    itemCls = Class.forName(n, false, cl);
                    break;
                } catch (Throwable ignore) {
                }
            }
            if (itemCls == null) {
                HookLog.w("未找到菜单项类");
                return null;
            }
            Object item = null;
            for (java.lang.reflect.Constructor<?> c : itemCls.getDeclaredConstructors()) {
                Class<?>[] ps = c.getParameterTypes();
                if (ps.length == 2 && ps[0] == Context.class) {
                    try {
                        c.setAccessible(true);
                        item = c.newInstance(ctx, null);
                        break;
                    } catch (Throwable ignore) {
                    }
                }
            }
            if (item == null) return null;

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
}
