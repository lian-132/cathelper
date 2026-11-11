package com.cathelperawa.owo.app.util;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public final class Synthetic {

    private Synthetic() {}

    public static Class<?> outerOf(Class<?> inner) {
        if (inner == null) return null;
        
        try {
            Class<?> e = inner.getEnclosingClass();
            if (e != null) return e;
        } catch (Throwable ignore) {
        }
        
        try {
            for (Field f : inner.getDeclaredFields()) {
                String n = f.getName();
                if ("this$0".equals(n) || (n.startsWith("this$") && n.length() <= 8)) {
                    Class<?> t = f.getType();
                    if (t != null && t != inner) return t;
                }
            }
        } catch (Throwable ignore) {
        }
        return null;
    }

    public static Field staticFieldOfType(Class<?> holder, Class<?> type) {
        if (holder == null || type == null) return null;
        Field best = null;
        try {
            for (Field f : holder.getDeclaredFields()) {
                if (!Modifier.isStatic(f.getModifiers())) continue;
                if (!type.isAssignableFrom(f.getType())) continue;
                
                if (f.getType() == type) return f;
                if (best == null) best = f;
            }
        } catch (Throwable ignore) {
        }
        return best;
    }

    public static Object staticValueOfType(Class<?> holder, Class<?> type) {
        Field f = staticFieldOfType(holder, type);
        if (f == null) return null;
        try {
            f.setAccessible(true);
            return f.get(null);
        } catch (Throwable t) {
            return null;
        }
    }
}
