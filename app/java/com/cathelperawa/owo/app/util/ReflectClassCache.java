package com.cathelperawa.owo.app.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ReflectClassCache {

    private ReflectClassCache() {}

    private static final int MAX = 512;

    private static final Map<String, Class<?>> HIT =
            new ConcurrentHashMap<String, Class<?>>();

    private static final Map<String, Boolean> MISS =
            new ConcurrentHashMap<String, Boolean>();

    private static volatile int boundVersion = -1;

    private static volatile int boundModuleVersion = -1;

    public static void bindModuleVersion(int moduleVersion) {
        if (moduleVersion <= 0 || boundModuleVersion == moduleVersion) return;
        synchronized (ReflectClassCache.class) {
            if (boundModuleVersion == moduleVersion) return;
            HIT.clear();
            MISS.clear();
            boundModuleVersion = moduleVersion;
        }
    }

    public static int moduleVersion() {
        return boundModuleVersion;
    }

    public static void bindVersion(int hostVersion) {
        if (hostVersion <= 0 || boundVersion == hostVersion) return;
        synchronized (ReflectClassCache.class) {
            if (boundVersion == hostVersion) return;
            HIT.clear();
            MISS.clear();
            boundVersion = hostVersion;
        }
    }

    public static Class<?> get(String key) {
        return HIT.get(key);
    }

    public static boolean isMiss(String key) {
        return MISS.containsKey(key);
    }

    public static void put(String key, Class<?> c) {
        if (key == null) return;
        if (c != null) {
            if (HIT.size() >= MAX) HIT.clear();
            HIT.put(key, c);
            MISS.remove(key);
        } else {
            if (MISS.size() >= MAX) MISS.clear();
            MISS.put(key, Boolean.TRUE);
        }
    }

    public static void clear() {
        HIT.clear();
        MISS.clear();
    }

    public static int size() {
        return HIT.size() + MISS.size();
    }
}
