package com.cathelperawa.owo.app.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class Refl {

    private Refl() {}

    

    

    

    

    private static final Object MISS = new Object();

    private static final java.util.concurrent.ConcurrentHashMap<
            Class<?>, java.util.concurrent.ConcurrentHashMap<String, Object>> FIELD_CACHE =
            new java.util.concurrent.ConcurrentHashMap<Class<?>,
                    java.util.concurrent.ConcurrentHashMap<String, Object>>();

    private static final java.util.concurrent.ConcurrentHashMap<
            Class<?>, java.util.concurrent.ConcurrentHashMap<String, Method[]>> METHODS_CACHE =
            new java.util.concurrent.ConcurrentHashMap<Class<?>,
                    java.util.concurrent.ConcurrentHashMap<String, Method[]>>();

    private static java.util.concurrent.ConcurrentHashMap<String, Object> fcache(Class<?> c) {
        java.util.concurrent.ConcurrentHashMap<String, Object> m = FIELD_CACHE.get(c);
        if (m == null) {
            m = new java.util.concurrent.ConcurrentHashMap<String, Object>();
            java.util.concurrent.ConcurrentHashMap<String, Object> prev = FIELD_CACHE.putIfAbsent(c, m);
            if (prev != null) m = prev;
        }
        return m;
    }

    private static Field fieldCached(Class<?> cls, String name) {
        java.util.concurrent.ConcurrentHashMap<String, Object> cache = fcache(cls);
        Object hit = cache.get(name);
        if (hit != null) return hit == MISS ? null : (Field) hit;
        Field f = null;
        Class<?> c = cls;
        while (c != null) {
            try {
                f = c.getDeclaredField(name);
                try { f.setAccessible(true); } catch (Throwable ignore) { }
                break;
            } catch (NoSuchFieldException e) {
                c = c.getSuperclass();
            } catch (Throwable t) {
                break;
            }
        }
        cache.put(name, f == null ? MISS : f);
        return f;
    }

    private static Method[] methodsNamed(Class<?> cls, String name, int argc) {
        java.util.concurrent.ConcurrentHashMap<String, Method[]> cache = METHODS_CACHE.get(cls);
        if (cache == null) {
            cache = new java.util.concurrent.ConcurrentHashMap<String, Method[]>();
            java.util.concurrent.ConcurrentHashMap<String, Method[]> prev = METHODS_CACHE.putIfAbsent(cls, cache);
            if (prev != null) cache = prev;
        }
        String key = name + "/" + argc;
        Method[] hit = cache.get(key);
        if (hit != null) return hit;
        java.util.ArrayList<Method> out = new java.util.ArrayList<Method>();
        Class<?> c = cls;
        while (c != null) {
            try {
                for (Method m : c.getDeclaredMethods()) {
                    if (!m.getName().equals(name)) continue;
                    if (m.getParameterTypes().length != argc) continue;
                    try { m.setAccessible(true); } catch (Throwable ignore) { }
                    out.add(m);
                }
            } catch (Throwable ignore) {
            }
            c = c.getSuperclass();
        }
        Method[] arr = out.toArray(new Method[0]);
        cache.put(key, arr);
        return arr;
    }

    public static Object field(Object o, String name) {
        if (o == null || name == null) return null;
        Field f = fieldCached(o.getClass(), name);
        if (f == null) return null;
        try {
            return f.get(o);
        } catch (Throwable t) {
            return null;
        }
    }

    public static boolean setField(Object o, String name, Object value) {
        if (o == null || name == null) return false;
        Field f = fieldCached(o.getClass(), name);
        if (f == null) return false;
        try {
            f.set(o, value);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    public static Field findField(Class<?> cls, String name) {
        return cls == null ? null : fieldCached(cls, name);
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
        if (o == null || method == null) return null;
        if (args == null) args = EMPTY;
        Method[] cands = methodsNamed(o.getClass(), method, args.length);
        for (Method m : cands) {
            try {
                return m.invoke(o, args);
            } catch (Throwable ignore) {
            }
        }
        return null;
    }

    private static final Object[] EMPTY = new Object[0];

    

    
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

    

    
    public static boolean notBlank(String s) {
        return s != null && s.trim().length() > 0;
    }

    public static String firstStr(Object o, String... names) {
        if (o == null || names == null) return null;
        for (String n : names) {
            if (n == null) continue;
            String v = str(field(o, n));
            if (notBlank(v) && !"null".equals(v)) return v;
        }
        return null;
    }

    public static String firstStrOr(Object o, String def, String... names) {
        String v = firstStr(o, names);
        return v == null ? def : v;
    }

    public static long fieldLong(Object o, String name, long def) {
        Object v = field(o, name);
        if (v instanceof Number) return ((Number) v).longValue();
        if (v instanceof String) {
            try {
                return Long.parseLong(((String) v).trim());
            } catch (Throwable ignore) {
            }
        }
        return def;
    }

    public static int fieldInt(Object o, String name, int def) {
        return (int) fieldLong(o, name, def);
    }

    public static boolean fieldBool(Object o, String name, boolean def) {
        Object v = field(o, name);
        if (v instanceof Boolean) return ((Boolean) v).booleanValue();
        if (v instanceof String) {
            String s = ((String) v).trim();
            if ("true".equalsIgnoreCase(s) || "1".equals(s)) return true;
            if ("false".equalsIgnoreCase(s) || "0".equals(s)) return false;
        }
        return def;
    }

    public static boolean callBool(Object o, String method, boolean def, Object... args) {
        Object v = call(o, method, args);
        if (v instanceof Boolean) return ((Boolean) v).booleanValue();
        return def;
    }

    public static String callStr(Object o, String method, String def, Object... args) {
        Object v = call(o, method, args);
        String s = str(v);
        return notBlank(s) ? s : def;
    }
}
