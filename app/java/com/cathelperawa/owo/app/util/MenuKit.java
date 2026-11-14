package com.cathelperawa.owo.app.util;

import android.content.Context;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class MenuKit {

    private MenuKit() {}

    public static Object item(ClassLoader cl, Class<?> menuD, Context ctx,
                              Object msgItem, Object proto, String title) {
        if (com.cathelperawa.owo.app.core.Host.isQQ()) {
            Object it = clone(proto);
            if (it != null) {
                setTitle(it, title);
                return it;
            }

            return null;
        }
        Object it = build(menuD, ctx, msgItem);
        if (it != null) setTitle(it, title);
        return it;
    }

    public static Object clone(Object proto) {
        if (proto == null) return null;
        Class<?> c = proto.getClass();
        Object copy;
        try {
            Class<?> uc = Class.forName("sun.misc.Unsafe");
            Field uf = uc.getDeclaredField("theUnsafe");
            uf.setAccessible(true);
            Object unsafe = uf.get(null);
            Method alloc = uc.getMethod("allocateInstance", Class.class);
            copy = alloc.invoke(unsafe, c);
        } catch (Throwable t) {
            return null;
        }
        Class<?> k = c;
        int dep = 0;
        while (k != null && k != Object.class && dep++ < 6) {
            for (Field f : k.getDeclaredFields()) {
                if (java.lang.reflect.Modifier.isStatic(f.getModifiers())) continue;
                if (f.getName().contains("redirector")) continue;
                try {
                    f.setAccessible(true);
                    f.set(copy, f.get(proto));
                } catch (Throwable ignore) {
                }
            }
            k = k.getSuperclass();
        }
        return copy;
    }

    public static void setTitle(Object item, String v) {
        if (item == null) return;
        Class<?> c = item.getClass();
        int dep = 0;
        while (c != null && c != Object.class && dep++ < 6) {
            for (Field f : c.getDeclaredFields()) {
                if (f.getType() != String.class) continue;
                if (java.lang.reflect.Modifier.isStatic(f.getModifiers())) continue;
                try {
                    f.setAccessible(true);
                    f.set(item, v);
                    return;
                } catch (Throwable ignore) {
                }
            }
            c = c.getSuperclass();
        }
    }

    private static Object build(Class<?> menuD, Context ctx, Object msgItem) {
        if (menuD == null) return null;
        try {
            java.lang.reflect.Constructor<?> k =
                    menuD.getDeclaredConstructor(Context.class, msgItem.getClass());
            k.setAccessible(true);
            return k.newInstance(ctx, msgItem);
        } catch (Throwable ignore) {
        }
        for (java.lang.reflect.Constructor<?> k : menuD.getDeclaredConstructors()) {
            try {
                k.setAccessible(true);
                Class<?>[] ps = k.getParameterTypes();
                Object[] as = new Object[ps.length];
                for (int i = 0; i < ps.length; i++) {
                    if (ps[i].isInstance(msgItem)) as[i] = msgItem;
                    else if (ctx != null && ps[i].isInstance(ctx)) as[i] = ctx;
                    else if (ps[i] == String.class) as[i] = "";
                    else if (ps[i] == boolean.class) as[i] = false;
                    else if (ps[i] == int.class) as[i] = 0;
                    else if (ps[i] == long.class) as[i] = 0L;
                    else as[i] = null;
                }
                return k.newInstance(as);
            } catch (Throwable ignore) {
            }
        }
        return null;
    }
}
