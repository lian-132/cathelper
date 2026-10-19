package com.cathelperawa.owo.app.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class Refl {

    private Refl() {}

    public static Object field(Object o, String name) {
        if (o == null) return null;
        Class<?> c = o.getClass();
        while (c != null) {
            try {
                Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                return f.get(o);
            } catch (NoSuchFieldException e) {
                c = c.getSuperclass();
            } catch (Throwable t) {
                return null;
            }
        }
        return null;
    }

    public static boolean setField(Object o, String name, Object value) {
        if (o == null) return false;
        Class<?> c = o.getClass();
        while (c != null) {
            try {
                Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                f.set(o, value);
                return true;
            } catch (NoSuchFieldException e) {
                c = c.getSuperclass();
            } catch (Throwable t) {
                return false;
            }
        }
        return false;
    }

    public static Field findField(Class<?> cls, String name) {
        while (cls != null) {
            try {
                return cls.getDeclaredField(name);
            } catch (Throwable t) {
                cls = cls.getSuperclass();
            }
        }
        return null;
    }

    public static Field findFieldByType(Object o, Class<?> type) {
        return o == null ? null : findFieldByType(o.getClass(), type, null);
    }

    public static Field findFieldByType(Class<?> cls, Class<?> type, Class<?> inParent) {
        while (cls != null) {
            if (inParent == null || cls == inParent) {
                for (Field f : cls.getDeclaredFields()) {

                    if (f.getType() == type || type.isAssignableFrom(f.getType())) {
                        try {
                            f.setAccessible(true);
                            return f;
                        } catch (Throwable ignore) {
                        }
                    }
                }
            }
            cls = cls.getSuperclass();
        }
        return null;
    }

    public static Object fieldByType(Object o, Class<?> type) {
        Field f = findFieldByType(o, type);
        if (f == null) return null;
        try {
            return f.get(o);
        } catch (Throwable t) {
            return null;
        }
    }

    public static boolean setFieldByType(Object o, Class<?> type, Object value) {
        Field f = findFieldByType(o, type);
        if (f == null) return false;
        try {
            f.set(o, value);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    public static Object call(Object o, String method, Object... args) {
        if (o == null) return null;
        Class<?> c = o.getClass();
        while (c != null) {
            for (Method m : c.getDeclaredMethods()) {
                if (!m.getName().equals(method)) continue;
                if (m.getParameterTypes().length != args.length) continue;
                try {
                    m.setAccessible(true);
                    return m.invoke(o, args);
                } catch (Throwable ignore) {
                }
            }
            c = c.getSuperclass();
        }
        return null;
    }

    public static Object callByType(Object o, Class<?>[] params, Object[] args) {
        if (o == null || params == null || args == null) return null;
        if (params.length != args.length) return null;
        Class<?> c = o.getClass();
        while (c != null) {
            for (Method m : c.getDeclaredMethods()) {
                Class<?>[] ps = m.getParameterTypes();
                if (ps.length != params.length) continue;
                boolean ok = true;
                for (int i = 0; i < ps.length; i++) {
                    if (params[i] == null) continue;

                    if (!params[i].isAssignableFrom(ps[i]) && !ps[i].isAssignableFrom(params[i])) {
                        ok = false;
                        break;
                    }
                }
                if (!ok) continue;
                try {
                    m.setAccessible(true);
                    return m.invoke(o, args);
                } catch (Throwable ignore) {
                }
            }
            c = c.getSuperclass();
        }
        return null;
    }

    public static Method findMethodByType(Class<?> cls, Class<?> ret, Class<?>... params) {
        return findMethodByType(SearchScope.DEFAULT, cls, ret, params);
    }

    public static Method findMethodByType(SearchScope scope, Class<?> cls,
                                          Class<?> ret, Class<?>... params) {
        try {
            return findMethodByTypeInner(scope == null ? SearchScope.DEFAULT : scope, cls, ret, params);
        } catch (Throwable t) {
            return null;
        }
    }

    private static Method findMethodByTypeInner(SearchScope scope, Class<?> cls,
                                                Class<?> ret, Class<?>... params) {
        Method m = searchInChain(cls, scope, ret, params);
        if (m != null || !scope.includesInterfaces()) return m;

        java.util.Set<Class<?>> seen = new java.util.HashSet<Class<?>>();
        Class<?> c = cls;
        while (c != null) {
            for (Class<?> itf : c.getInterfaces()) {
                Method found = searchInterfaces(itf, ret, params, seen);
                if (found != null) return found;
            }
            c = c.getSuperclass();
        }
        return null;
    }

    private static Method searchInChain(Class<?> cls, SearchScope scope,
                                        Class<?> ret, Class<?>... params) {
        while (cls != null) {
            for (Method m : cls.getDeclaredMethods()) {
                if (matches(m, ret, params)) {
                    m.setAccessible(true);
                    return m;
                }
            }
            cls = scope.includesSuper() ? cls.getSuperclass() : null;
        }
        return null;
    }

    private static Method searchInterfaces(Class<?> itf, Class<?> ret,
                                           Class<?>[] params, java.util.Set<Class<?>> seen) {
        if (itf == null || !seen.add(itf)) return null;
        for (Method m : itf.getDeclaredMethods()) {
            if (matches(m, ret, params)) {
                try { m.setAccessible(true); } catch (Throwable ignore) { }
                return m;
            }
        }
        for (Class<?> sup : itf.getInterfaces()) {
            Method found = searchInterfaces(sup, ret, params, seen);
            if (found != null) return found;
        }
        return null;
    }

    private static boolean matches(Method m, Class<?> ret, Class<?>[] params) {
        if (ret != null && !ret.isAssignableFrom(m.getReturnType())) return false;
        Class<?>[] ps = m.getParameterTypes();
        if (ps.length != params.length) return false;
        for (int i = 0; i < ps.length; i++) {
            if (params[i] == null) continue;
            if (!params[i].isAssignableFrom(ps[i]) && !ps[i].isAssignableFrom(params[i])) {
                return false;
            }
        }
        return true;
    }

    public static String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    public static String strOr(Object o, String def) {
        String s = str(o);
        return (s == null || s.length() == 0 || "null".equals(s)) ? def : s;
    }

    public static long lng(Object o, long def) {
        return o instanceof Number ? ((Number) o).longValue() : def;
    }

    public static int i32(Object o, int def) {
        return o instanceof Number ? ((Number) o).intValue() : def;
    }
}
