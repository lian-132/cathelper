package com.cathelperawa.owo.app.script;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public final class ApiBridge {

    private ApiBridge() {}

    private static final ThreadLocal<Object> CURRENT = new ThreadLocal<Object>();

    public static Object current() {
        return CURRENT.get();
    }

    public static void push(Object api) {
        CURRENT.set(api);
    }

    public static void pop() {
        CURRENT.remove();
    }

    private static final ConcurrentHashMap<String, Method> CACHE =
            new ConcurrentHashMap<String, Method>();

    private static final ConcurrentHashMap<String, Object> CFG =
            new ConcurrentHashMap<String, Object>();

    private static Method m(Object api, String name, Class<?>... types) {
        if (api == null) return null;
        String key = api.getClass().getName() + "#" + name + "#" + types.length;
        Method cached = CACHE.get(key);
        if (cached != null) return cached;
        try {
            Method mm = api.getClass().getMethod(name, types);
            CACHE.put(key, mm);
            return mm;
        } catch (Throwable t) {
            return null;
        }
    }

    private static Object call(Object api, String name, Class<?>[] types, Object[] args) {
        try {
            Method mm = m(api, name, types);
            if (mm == null) return null;
            return mm.invoke(api, args);
        } catch (Throwable t) {
            return null;
        }
    }

    private static String key(String file, String k) {
        return (file == null || file.length() == 0) ? k : file + "." + k;
    }

    public static int getInt(Object api, String file, String k, int def) {
        try {
            Object v = call(api, "getInt", new Class[]{String.class, int.class},
                    new Object[]{key(file, k), def});
            return v instanceof Integer ? (Integer) v : def;
        } catch (Throwable t) {
            return def;
        }
    }

    public static boolean getBoolean(Object api, String file, String k, boolean def) {
        try {
            Object v = call(api, "getBool", new Class[]{String.class, boolean.class},
                    new Object[]{key(file, k), def});
            return v instanceof Boolean ? (Boolean) v : def;
        } catch (Throwable t) {
            return def;
        }
    }

    public static String getString(Object api, String file, String k, String def) {
        try {
            Object v = call(api, "get", new Class[]{String.class, String.class},
                    new Object[]{key(file, k), def});
            return v instanceof String ? (String) v : def;
        } catch (Throwable t) {
            return def;
        }
    }

    public static long getLong(Object api, String file, String k, long def) {
        try {
            String s = getString(api, file, k, null);
            return s == null ? def : Long.parseLong(s.trim());
        } catch (Throwable t) {
            return def;
        }
    }

    public static void putInt(Object api, String file, String k, int v) {
        call(api, "set", new Class[]{String.class, String.class},
                new Object[]{key(file, k), String.valueOf(v)});
    }

    public static void putBoolean(Object api, String file, String k, boolean v) {
        call(api, "set", new Class[]{String.class, String.class},
                new Object[]{key(file, k), String.valueOf(v)});
    }

    public static void putString(Object api, String file, String k, String v) {
        call(api, "set", new Class[]{String.class, String.class},
                new Object[]{key(file, k), v});
    }

    public static void putLong(Object api, String file, String k, long v) {
        putString(api, file, k, String.valueOf(v));
    }

    private static String kind(int type) {
        return type == 2 ? "group" : "c2c";
    }

    public static boolean sendMsg(Object api, String peer, String text, int type) {
        Object r = call(api, "sendText", new Class[]{String.class, String.class, String.class},
                new Object[]{kind(type), peer, text});
        return r instanceof Boolean && (Boolean) r;
    }

    public static boolean sendPic(Object api, String peer, String path, int type) {
        Object r = call(api, "sendPic", new Class[]{String.class, String.class, String.class},
                new Object[]{kind(type), peer, path});
        return r instanceof Boolean && (Boolean) r;
    }

    public static boolean sendPtt(Object api, String peer, String path, int type) {
        Object r = call(api, "sendPtt", new Class[]{String.class, String.class, String.class},
                new Object[]{kind(type), peer, path});
        return r instanceof Boolean && (Boolean) r;
    }

    public static boolean sendFile(Object api, String peer, String path, int type) {
        Object r = call(api, "sendFile", new Class[]{String.class, String.class, String.class},
                new Object[]{kind(type), peer, path});
        return r instanceof Boolean && (Boolean) r;
    }

    public static boolean sendReply(Object api, String peer, long id, String text, int type) {
        Object r = call(api, "sendReply", new Class[]{String.class, String.class, long.class, String.class},
                new Object[]{kind(type), peer, id, text});
        return r instanceof Boolean && (Boolean) r;
    }

    public static void toast(Object api, String s) {
        call(api, "toast", new Class[]{String.class}, new Object[]{s});
    }

    public static void log(Object api, String s) {
        call(api, "log", new Class[]{String.class}, new Object[]{s});
    }

    public static android.app.Activity activity(Object api) {
        Object r = call(api, "getNowActivity", new Class[]{}, new Object[]{});
        return r instanceof android.app.Activity ? (android.app.Activity) r : null;
    }

    public static android.content.Context context(Object api) {
        Object r = call(api, "context", new Class[]{}, new Object[]{});
        return r instanceof android.content.Context ? (android.content.Context) r : null;
    }

    public static void shutUp(Object api, String group, String uid, long sec) {
        call(api, "mute", new Class[]{String.class, String.class, int.class},
                new Object[]{group, uid, (int) sec});
    }

    public static void kick(Object api, String group, String uid) {
        call(api, "kick", new Class[]{String.class, String.class}, new Object[]{group, uid});
    }

    public static void recall(Object api, String peer, long id, int type) {
        call(api, "recall", new Class[]{String.class, String.class, long.class},
                new Object[]{kind(type), peer, id});
    }

    public static List groupMembers(Object api, String group) {
        Object r = call(api, "groupMembers", new Class[]{String.class}, new Object[]{group});
        return r instanceof List ? (List) r : new ArrayList();
    }

    public static String memberInfo(Object api, String group, String uid) {
        Object r = call(api, "memberInfo", new Class[]{String.class, String.class},
                new Object[]{group, uid});
        return r == null ? "" : String.valueOf(r);
    }

    public static String skey(Object api) {
        try {
            Class<?> c = Class.forName("com.cathelperawa.owo.app.api.CookieApi");
            return String.valueOf(c.getMethod("skey").invoke(null));
        } catch (Throwable t) {
            return "";
        }
    }

    public static String pskey(Object api, String url) {
        try {
            Class<?> c = Class.forName("com.cathelperawa.owo.app.api.CookieApi");
            return String.valueOf(c.getMethod("pskey", String.class).invoke(null, url));
        } catch (Throwable t) {
            return "";
        }
    }
}
