package com.cathelperawa.owo.app.script;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class DexClassManager extends bsh.classpath.ClassManagerImpl {

    private final ClassLoader parent;

    private volatile ClassLoader chain;

    private static final java.util.Map<String, Boolean> SEEN =
            new java.util.concurrent.ConcurrentHashMap<String, Boolean>();

    private static final java.util.Map<String, String> FAILED =
            new java.util.concurrent.ConcurrentHashMap<String, String>();


    public DexClassManager(ClassLoader parent) {
        this.parent = parent != null ? parent : DexClassManager.class.getClassLoader();
        this.chain = this.parent;
    }

    @Override
    public Class<?> defineClass(String name, byte[] bytes) {
        if (name == null || bytes == null) return null;
        String why = FAILED.get(name);
        if (why != null) {
            com.cathelperawa.owo.app.core.HookLog.w("[bsh类] " + name + " 之前失败过，跳过（" + why + "）");
            return null;
        }
        try {
            ClassLoader loader = ScriptJar.dexLoader(bytes, name, chain);
            Class<?> c = null;
            if (loader != null) {
                try {
                    c = Class.forName(name, false, loader);
                } catch (Throwable nf) {
                }
            }
            if (c != null) {
                chain = loader;
                cacheClassInfo(name, c);
                if (SEEN.put(name, Boolean.TRUE) == null) {
                    com.cathelperawa.owo.app.core.HookLog.i(
                            "[bsh类] 已定义 " + name + "（" + (bytes.length / 1024) + "KB 字节 → dx）");
                }
                return c;
            }
            FAILED.put(name, "dx 返回 null");
        } catch (Throwable t) {
            FAILED.put(name, String.valueOf(t));
            com.cathelperawa.owo.app.core.HookLog.w("[bsh类] dx 定义失败 " + name + "：" + t);
        }

        try {
            return super.defineClass(name, bytes);
        } catch (Throwable t) {
            com.cathelperawa.owo.app.core.HookLog.w("[bsh类] 原生 defineClass 也失败 " + name + "：" + t);
            return null;
        }
    }

    public static void install(bsh.Interpreter interp) {
        if (interp == null) return;
        try {
            bsh.NameSpace ns = interp.getNameSpace();
            bsh.BshClassManager old = ns.getClassManager();
            if (old instanceof DexClassManager) return;

            DexClassManager dm = new DexClassManager(DexClassManager.class.getClassLoader());

            if (old != null) {
                try {
                    Field ef = bsh.BshClassManager.class.getDeclaredField("externalClassLoader");
                    ef.setAccessible(true);
                    Object ext = ef.get(old);
                    if (ext instanceof ClassLoader) dm.setClassLoader((ClassLoader) ext);
                } catch (Throwable ignore) {
                }
                try {
                    Field cf = bsh.BshClassManager.class.getDeclaredField("absoluteClassCache");
                    cf.setAccessible(true);
                    Object m2 = cf.get(old);
                    if (m2 instanceof java.util.Map) dm.absoluteClassCache.putAll((java.util.Map) m2);
                } catch (Throwable ignore) {
                }
                try {
                    Field nf = bsh.BshClassManager.class.getDeclaredField("absoluteNonClasses");
                    nf.setAccessible(true);
                    Object s2 = nf.get(old);
                    if (s2 instanceof java.util.Set) dm.absoluteNonClasses.addAll((java.util.Set) s2);
                } catch (Throwable ignore) {
                }
            }

            Method m = bsh.NameSpace.class.getDeclaredMethod(
                    "setClassManager", bsh.BshClassManager.class);
            m.setAccessible(true);
            m.invoke(ns, dm);
            com.cathelperawa.owo.app.core.HookLog.i("[bsh类] ClassManager 已换成 dx 版");
        } catch (Throwable t) {
            com.cathelperawa.owo.app.core.HookLog.w("[bsh类] 安装失败（脚本里不能写 class）：" + t);
        }
    }

    public static String selfTest() {
        StringBuilder sb = new StringBuilder();
        sb.append("DexClassManager 自检\n");
        sb.append("  parent = ").append(String.valueOf(DexClassManager.class.getClassLoader())).append('\n');
        try {
            sb.append("  dx DexFile = ").append(String.valueOf(
                    Class.forName("com.android.dx.dex.file.DexFile", false,
                            DexClassManager.class.getClassLoader()))).append('\n');
        } catch (Throwable t) {
            sb.append("  dx 不可用：").append(t).append('\n');
        }
        sb.append("  已定义类 = ").append(SEEN.keySet()).append('\n');
        sb.append("  失败类 = ").append(FAILED).append('\n');
        return sb.toString();
    }
}
