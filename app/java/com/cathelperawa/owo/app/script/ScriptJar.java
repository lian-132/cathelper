package com.cathelperawa.owo.app.script;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class ScriptJar {

    private ScriptJar() {}

    private static final Object LOCK = new Object();

    private static final Map<String, ClassLoader> CACHE = new HashMap<String, ClassLoader>();

    public static ClassLoader load(String rel) {
        File src = ScriptDex.resolve(rel);
        if (src == null || !src.isFile()) {
            com.cathelperawa.owo.app.core.HookLog.w("[jar] 文件不存在或路径越界：" + rel);
            return null;
        }
        String key = src.getAbsolutePath() + "#" + src.lastModified() + "#" + src.length();
        synchronized (CACHE) {
            ClassLoader hit = CACHE.get(key);
            if (hit != null) return hit;
        }
        try {
            File dex;
            if (rel.toLowerCase().endsWith(".dex")) {
                dex = src;
            } else {
                dex = compile(src);
                if (dex == null) return null;
            }
            ClassLoader parent = hostLoader();
            ClassLoader cl = new dalvik.system.PathClassLoader(dex.getAbsolutePath(), parent);
            synchronized (CACHE) {
                CACHE.put(key, cl);
            }
            com.cathelperawa.owo.app.core.HookLog.i("[jar] 已加载 " + src.getName()
                    + "（父加载器=" + (parent == null ? "null" : "宿主") + "）");
            return cl;
        } catch (Throwable t) {
            com.cathelperawa.owo.app.core.HookLog.e("[jar] 加载失败 " + src.getName(), t);
            return null;
        }
    }

    static File compile(File jar) {
        synchronized (LOCK) {
            try {
                com.android.dx.dex.DexOptions dexOpt = new com.android.dx.dex.DexOptions();
                com.android.dx.dex.file.DexFile dexFile =
                        new com.android.dx.dex.file.DexFile(dexOpt);
                com.android.dx.command.dexer.DxContext dxCtx =
                        new com.android.dx.command.dexer.DxContext();
                com.android.dx.dex.cf.CfOptions cfOpt = new com.android.dx.dex.cf.CfOptions();

                int ok = 0, bad = 0;
                InputStream in = new java.io.FileInputStream(jar);
                ZipInputStream zin = new ZipInputStream(in);
                ZipEntry ze;
                while ((ze = zin.getNextEntry()) != null) {
                    String name = ze.getName();
                    if (!name.endsWith(".class")) continue;
                    if (name.startsWith("META-INF/")) continue;
                    ByteArrayOutputStream bo = new ByteArrayOutputStream();
                    byte[] buf = new byte[8192];
                    int n;
                    while ((n = zin.read(buf)) > 0) bo.write(buf, 0, n);
                    byte[] code = bo.toByteArray();
                    try {
                        com.android.dx.cf.direct.DirectClassFile cf =
                                new com.android.dx.cf.direct.DirectClassFile(code, name, true);
                        cf.setAttributeFactory(
                                com.android.dx.cf.direct.StdAttributeFactory.THE_ONE);
                        dexFile.add(com.android.dx.dex.cf.CfTranslator.translate(
                                dxCtx, cf, code, cfOpt, dexOpt, dexFile));
                        ok++;
                    } catch (Throwable t) {
                        bad++;
                        com.cathelperawa.owo.app.core.HookLog.w("[jar] 跳过 " + name + "：" + t);
                    }
                }
                zin.close();
                in.close();

                ByteArrayOutputStream dest = new ByteArrayOutputStream();
                dexFile.writeTo(dest, null, false);

                File outDir = new File(jar.getParentFile(), ".dexout");
                if (!outDir.exists()) outDir.mkdirs();
                File out = new File(outDir, jar.getName() + ".dex");
                FileOutputStream fos = new FileOutputStream(out);
                fos.write(dest.toByteArray());
                fos.close();
                com.cathelperawa.owo.app.core.HookLog.i("[jar] " + jar.getName()
                        + " 编译完成：成功 " + ok + " 个类，跳过 " + bad
                        + "，dex " + (dest.size() / 1024) + "KB");
                return out;
            } catch (Throwable t) {
                com.cathelperawa.owo.app.core.HookLog.e("[jar] 编译失败 " + jar.getName(), t);
                return null;
            }
        }
    }

    public static ClassLoader dexLoader(byte[] classBytes, String className, ClassLoader parent) {
        if (classBytes == null) return null;
        synchronized (LOCK) {
            try {
                com.android.dx.dex.DexOptions dexOpt = new com.android.dx.dex.DexOptions();
                com.android.dx.dex.file.DexFile dexFile =
                        new com.android.dx.dex.file.DexFile(dexOpt);
                com.android.dx.command.dexer.DxContext dxCtx =
                        new com.android.dx.command.dexer.DxContext();
                com.android.dx.dex.cf.CfOptions cfOpt = new com.android.dx.dex.cf.CfOptions();

                String internal = (className == null ? "Generated" : className)
                        .replace('.', '/') + ".class";
                com.android.dx.cf.direct.DirectClassFile cf =
                        new com.android.dx.cf.direct.DirectClassFile(classBytes, internal, true);
                cf.setAttributeFactory(com.android.dx.cf.direct.StdAttributeFactory.THE_ONE);
                dexFile.add(com.android.dx.dex.cf.CfTranslator.translate(
                        dxCtx, cf, classBytes, cfOpt, dexOpt, dexFile));

                ByteArrayOutputStream bo = new ByteArrayOutputStream();
                dexFile.writeTo(bo, null, false);

                return new dalvik.system.InMemoryDexClassLoader(
                        java.nio.ByteBuffer.wrap(bo.toByteArray()),
                        parent != null ? parent : hostLoader());
            } catch (Throwable t) {
                com.cathelperawa.owo.app.core.HookLog.w("[dx] 转换失败 " + className + "：" + t);
                return null;
            }
        }
    }

    public static Class<?> defineClass(String className, byte[] classBytes) {
        return defineClassFor(className, classBytes, null);
    }

    public static Class<?> defineClassFor(String className, byte[] classBytes, ClassLoader parent) {
        if (className == null || classBytes == null) return null;
        synchronized (LOCK) {
            try {
                com.android.dx.dex.DexOptions dexOpt = new com.android.dx.dex.DexOptions();
                com.android.dx.dex.file.DexFile dexFile =
                        new com.android.dx.dex.file.DexFile(dexOpt);
                com.android.dx.command.dexer.DxContext dxCtx =
                        new com.android.dx.command.dexer.DxContext();
                com.android.dx.dex.cf.CfOptions cfOpt = new com.android.dx.dex.cf.CfOptions();

                String internal = className.replace('.', '/') + ".class";
                com.android.dx.cf.direct.DirectClassFile cf =
                        new com.android.dx.cf.direct.DirectClassFile(classBytes, internal, true);
                cf.setAttributeFactory(com.android.dx.cf.direct.StdAttributeFactory.THE_ONE);
                dexFile.add(com.android.dx.dex.cf.CfTranslator.translate(
                        dxCtx, cf, classBytes, cfOpt, dexOpt, dexFile));

                ByteArrayOutputStream bo = new ByteArrayOutputStream();
                dexFile.writeTo(bo, null, false);

                dalvik.system.InMemoryDexClassLoader loader =
                        new dalvik.system.InMemoryDexClassLoader(
                                java.nio.ByteBuffer.wrap(bo.toByteArray()),
                                parent != null ? parent : hostLoader());
                return Class.forName(className, true, loader);
            } catch (Throwable t) {
                com.cathelperawa.owo.app.core.HookLog.e("[jar] defineClass 失败 " + className, t);
                return null;
            }
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
        return ScriptJar.class.getClassLoader();
    }
}
