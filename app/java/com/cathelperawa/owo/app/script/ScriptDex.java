package com.cathelperawa.owo.app.script;

import java.io.File;
import java.lang.reflect.Method;

public final class ScriptDex {

    private ScriptDex() {}

    private static final java.util.HashMap<String, ClassLoader> CACHE =
            new java.util.HashMap<String, ClassLoader>();

    public static ClassLoader load(String rel) {
        if (rel == null || rel.trim().length() == 0) return null;
        File f = resolve(rel);
        if (f == null) {
            com.cathelperawa.owo.app.core.HookLog.w("[dex] 路径不在脚本目录里，拒绝加载：" + rel);
            return null;
        }
        if (!f.isFile()) {
            com.cathelperawa.owo.app.core.HookLog.w("[dex] 文件不存在：" + f);
            return null;
        }
        String key = f.getAbsolutePath() + "#" + f.lastModified() + "#" + f.length();
        synchronized (CACHE) {
            ClassLoader hit = CACHE.get(key);
            if (hit != null) return hit;
        }
        ClassLoader cl = create(f);
        if (cl != null) {
            synchronized (CACHE) {
                CACHE.put(key, cl);
            }
        }
        return cl;
    }

    private static ClassLoader create(File f) {
        try {
            ClassLoader parent = hostLoader();
            
            try {
                Class<?> c = Class.forName("dalvik.system.DexClassLoader");
                java.lang.reflect.Constructor<?> ctor = c.getConstructor(
                        String.class, String.class, String.class, ClassLoader.class);
                File dir = f.getParentFile();
                File opt = new File(dir, ".dexopt");
                if (!opt.exists()) opt.mkdirs();
                ClassLoader cl = (ClassLoader) ctor.newInstance(
                        f.getAbsolutePath(), opt.getAbsolutePath(), null, parent);
                com.cathelperawa.owo.app.core.HookLog.i("[dex] 已加载 " + f.getName());
                return cl;
            } catch (Throwable t) {
                com.cathelperawa.owo.app.core.HookLog.w("[dex] DexClassLoader 失败：" + t);
            }
            
            try {
                Class<?> c = Class.forName("dalvik.system.DexClassLoader");
                java.lang.reflect.Constructor<?> ctor = c.getConstructor(
                        String.class, String.class, String.class, ClassLoader.class);
                return (ClassLoader) ctor.newInstance(f.getAbsolutePath(), null, null, parent);
            } catch (Throwable t) {
                com.cathelperawa.owo.app.core.HookLog.w("[dex] 兜底加载也失败：" + t);
                return null;
            }
        } catch (Throwable t) {
            com.cathelperawa.owo.app.core.HookLog.w("[dex] 加载异常：" + t);
            return null;
        }
    }

    public static Class<?> loadClass(String rel, String className) {
        ClassLoader cl = load(rel);
        if (cl == null) return null;
        try {
            return Class.forName(className, false, cl);
        } catch (Throwable t) {
            com.cathelperawa.owo.app.core.HookLog.w("[dex] 找不到类 " + className + "：" + t);
            return null;
        }
    }

    static File resolve(String rel) {
        try {
            File root = ScriptHost.dir();
            if (root == null) return null;
            File f = new File(root, rel);
            String canonRoot = root.getCanonicalPath();
            String canonFile = f.getCanonicalPath();
            if (!canonFile.startsWith(canonRoot + File.separator)) {
                return null;
            }
            return f;
        } catch (Throwable t) {
            return null;
        }
    }

    public static void clear() {
        synchronized (CACHE) {
            CACHE.clear();
        }
    }

    private static ClassLoader hostLoader() {
        try {
            ClassLoader cl = com.cathelperawa.owo.app.bridge.QQEnv.classLoader();
            if (cl != null) return cl;
        } catch (Throwable ignore) {
        }
        return ScriptDex.class.getClassLoader();
    }
}
