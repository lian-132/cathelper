package com.cathelperawa.owo.app.script;

import org.codehaus.janino.SimpleCompiler;
import org.codehaus.janino.util.ClassFile;

import java.io.ByteArrayOutputStream;
import java.io.StringReader;

public final class JavaToolchain {

    private JavaToolchain() {}

    public static final class Result {
        public final Class<?> master;
        public final long millis;
        public final Throwable error;

        Result(Class<?> master, long millis, Throwable error) {
            this.master = master;
            this.millis = millis;
            this.error = error;
        }

        public boolean ok() {
            return master != null;
        }
    }

    public static Result compile(String source) {
        long t0 = System.currentTimeMillis();
        try {
            SimpleCompiler c = new SimpleCompiler();
            c.setParentClassLoader(JavaToolchain.class.getClassLoader());
            c.setDebuggingInformation(false, false, false);
            c.cook(new StringReader(source));
            ClassFile[] files = c.getClassFiles();
            if (files == null || files.length == 0) {
                throw new IllegalStateException("编译没有产出任何类");
            }
            ByteArrayOutputStream dexBuf = new ByteArrayOutputStream();
            buildDex(files, dexBuf);
            ClassLoader loader = new dalvik.system.InMemoryDexClassLoader(
                    java.nio.ByteBuffer.wrap(dexBuf.toByteArray()),
                    JavaToolchain.class.getClassLoader());
            Class<?> m = Class.forName("scripts.Master", false, loader);
            return new Result(m, System.currentTimeMillis() - t0, null);
        } catch (Throwable t) {
            return new Result(null, System.currentTimeMillis() - t0, t);
        }
    }

    private static void buildDex(ClassFile[] files, ByteArrayOutputStream out) throws Exception {
        com.android.dx.dex.DexOptions dexOpt = new com.android.dx.dex.DexOptions();
        com.android.dx.dex.file.DexFile dexFile = new com.android.dx.dex.file.DexFile(dexOpt);
        com.android.dx.command.dexer.DxContext dxCtx = new com.android.dx.command.dexer.DxContext();
        com.android.dx.dex.cf.CfOptions cfOpt = new com.android.dx.dex.cf.CfOptions();
        int n = 0;
        String firstErr = null;
        for (ClassFile cf0 : files) {
            byte[] code = cf0.toByteArray();

            String name = cf0.getThisClassName();
            if (name == null || name.length() == 0) name = "scripts.Master";
            name = name.replace('.', '/');
            if (!name.endsWith(".class")) name = name + ".class";
            try {
                com.android.dx.cf.direct.DirectClassFile cf =
                        new com.android.dx.cf.direct.DirectClassFile(code, name, true);
                cf.setAttributeFactory(com.android.dx.cf.direct.StdAttributeFactory.THE_ONE);
                dexFile.add(com.android.dx.dex.cf.CfTranslator.translate(
                        dxCtx, cf, code, cfOpt, dexOpt, dexFile));
                n++;
            } catch (Throwable t) {
                if (firstErr == null) firstErr = name + " -> " + t;
                com.cathelperawa.owo.app.core.HookLog.w("[java] 跳过 " + name + "：" + t);
            }
        }
        if (n == 0) throw new IllegalStateException("没有可转换的类：" + firstErr);
        dexFile.writeTo(out, null, false);
    }

    public static String humanize(Throwable t, int injectedLines) {
        if (t == null) return "";
        String msg = String.valueOf(t.getMessage());
        String lower = msg.toLowerCase();
        if (lower.contains("line ") && lower.contains("column ")) {
            java.util.regex.Matcher m = java.util.regex.Pattern
                    .compile("Line (\\d+), Column (\\d+)").matcher(msg);
            if (m.find()) {
                int line = Integer.parseInt(m.group(1));
                int scriptLine = line - injectedLines;
                String detail = msg.substring(m.end()).trim();
                if (detail.startsWith(":")) detail = detail.substring(1).trim();
                return "脚本第 " + Math.max(1, scriptLine) + " 行：" + detail;
            }
        }
        return msg.length() > 400 ? msg.substring(0, 400) + "…" : msg;
    }
}
