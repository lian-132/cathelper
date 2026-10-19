package com.cathelperawa.owo.app.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ReflectCache {

    private ReflectCache() {}

    private static final int MAX = 512;

    private static final Member MISS = new MemberSentinel();

    private static final class MemberSentinel implements Member {
        @Override public Class<?> getDeclaringClass() { return null; }
        @Override public String getName() { return "<miss>"; }
        @Override public int getModifiers() { return 0; }
        @Override public boolean isSynthetic() { return false; }
    }

    private static final Map<String, Member> MEMBERS =
            new ConcurrentHashMap<String, Member>();

    private static volatile int boundVersion = -1;

    private static volatile int boundModuleVersion = -1;

    public static void bindVersion(int hostVersion) {
        if (hostVersion <= 0) return;
        if (boundVersion == hostVersion) return;
        synchronized (ReflectCache.class) {
            if (boundVersion == hostVersion) return;
            MEMBERS.clear();
            boundVersion = hostVersion;
        }
    }

    public static int boundVersion() {
        return boundVersion;
    }

    public static void bindModuleVersion(int moduleVersion) {
        if (moduleVersion <= 0) return;
        if (boundModuleVersion == moduleVersion) return;
        synchronized (ReflectCache.class) {
            if (boundModuleVersion == moduleVersion) return;
            MEMBERS.clear();
            boundModuleVersion = moduleVersion;
        }
    }

    public static int boundModuleVersion() {
        return boundModuleVersion;
    }

    public static Method method(String key) {
        Member m = MEMBERS.get(key);
        return m instanceof Method ? (Method) m : null;
    }

    public static boolean isMethodMiss(String key) {
        return MEMBERS.get(key) == MISS;
    }

    public static Method putMethod(String key, Method m) {
        put(key, m);
        return m;
    }

    public static void putMiss(String key) {
        if (key == null) return;
        if (MEMBERS.size() >= MAX) MEMBERS.clear();
        MEMBERS.put(key, MISS);
    }

    public static Constructor<?> constructor(String key) {
        Member m = MEMBERS.get(key);
        return m instanceof Constructor ? (Constructor<?>) m : null;
    }

    public static Constructor<?> putConstructor(String key, Constructor<?> c) {
        put(key, c);
        return c;
    }

    public static Field field(String key) {
        Member m = MEMBERS.get(key);
        return m instanceof Field ? (Field) m : null;
    }

    public static Field putField(String key, Field f) {
        put(key, f);
        return f;
    }

    private static void put(String key, Member m) {
        if (key == null || m == null) return;
        if (MEMBERS.size() >= MAX) {

            MEMBERS.clear();
        }
        MEMBERS.put(key, m);
    }

    public static void clear() {
        MEMBERS.clear();
    }

    public static int size() {
        return MEMBERS.size();
    }

    public static String key(String kind, Class<?> cls,
                             Class<?> ret, Class<?>... params) {
        StringBuilder sb = new StringBuilder(64);
        sb.append(kind).append(':');
        sb.append(cls == null ? "?" : cls.getName());
        sb.append(':');
        sb.append(ret == null ? "*" : ret.getName());
        sb.append(':');
        if (params != null) {
            for (int i = 0; i < params.length; i++) {
                if (i > 0) sb.append(',');
                sb.append(params[i] == null ? "*" + i : params[i].getName());
            }
        }
        return sb.toString();
    }

    public static String keyName(String kind, Class<?> cls, String name) {
        return kind + ':' + (cls == null ? "?" : cls.getName()) + ':' + name;
    }
}
