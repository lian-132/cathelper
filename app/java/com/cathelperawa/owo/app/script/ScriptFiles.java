package com.cathelperawa.owo.app.script;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;

public final class ScriptFiles {

    private ScriptFiles() {}

    private static final long MAX_READ = 128L * 1024 * 1024;

    private static File resolve(File base, String name) {
        if (base == null || name == null) return null;
        String clean = name.trim();
        if (clean.length() == 0) return null;
        if (clean.indexOf('\0') >= 0) return null;
        try {
            File root = base.getCanonicalFile();
            File f = new File(clean).isAbsolute() ? new File(clean) : new File(root, clean);
            File real = f.getCanonicalFile();
            String rp = root.getPath();
            String fp = real.getPath();
            if (!fp.equals(rp) && !fp.startsWith(rp + File.separator)) {
                com.cathelperawa.owo.app.core.HookLog.w(
                        "[脚本文件] 拒绝越界访问：" + name);
                return null;
            }
            return real;
        } catch (Throwable t) {
            return null;
        }
    }

    public static boolean write(File base, String name, String content) {
        File f = resolve(base, name);
        if (f == null) return false;
        try {
            File dir = f.getParentFile();
            if (dir != null && !dir.exists() && !dir.mkdirs()) return false;
            File tmp = new File(f.getPath() + ".tmp");
            FileOutputStream out = new FileOutputStream(tmp);
            try {
                out.write((content == null ? "" : content).getBytes("UTF-8"));
                out.flush();
                out.getFD().sync();
            } finally {
                out.close();
            }
            if (f.exists() && !f.delete()) {

            }
            if (!tmp.renameTo(f)) {

                FileInputStream in = new FileInputStream(tmp);
                FileOutputStream o2 = new FileOutputStream(f);
                try {
                    byte[] buf = new byte[8192];
                    int n;
                    while ((n = in.read(buf)) > 0) o2.write(buf, 0, n);
                } finally {
                    try { in.close(); } catch (Throwable ignore) {}
                    try { o2.close(); } catch (Throwable ignore) {}
                }
                tmp.delete();
            }
            return true;
        } catch (Throwable t) {
            com.cathelperawa.owo.app.core.HookLog.w("[脚本文件] 写入失败 " + name + "：" + t);
            return false;
        }
    }

    public static boolean append(File base, String name, String content) {
        File f = resolve(base, name);
        if (f == null) return false;
        try {
            File dir = f.getParentFile();
            if (dir != null && !dir.exists() && !dir.mkdirs()) return false;
            FileWriter w = new FileWriter(f, true);
            try {
                w.write(content == null ? "" : content);
            } finally {
                w.close();
            }
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    public static String read(File base, String name) {
        File f = resolve(base, name);
        if (f == null || !f.exists() || !f.isFile()) return "";
        try {
            if (f.length() > MAX_READ) return "";
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            FileInputStream in = new FileInputStream(f);
            try {
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) > 0) bos.write(buf, 0, n);
            } finally {
                in.close();
            }
            return new String(bos.toByteArray(), "UTF-8");
        } catch (Throwable t) {
            return "";
        }
    }

    public static boolean exists(File base, String name) {
        File f = resolve(base, name);
        return f != null && f.exists();
    }

    public static boolean delete(File base, String name) {
        File f = resolve(base, name);
        if (f == null || !f.exists()) return false;
        return deleteTree(f);
    }

    private static boolean deleteTree(File f) {
        if (f.isDirectory()) {
            File[] cs = f.listFiles();
            if (cs != null) for (int i = 0; i < cs.length; i++) deleteTree(cs[i]);
        }
        return f.delete();
    }

    public static boolean mkdir(File base, String name) {
        File f = resolve(base, name);
        return f != null && (f.exists() || f.mkdirs());
    }

    public static boolean rename(File base, String from, String to) {
        File a = resolve(base, from);
        File b = resolve(base, to);
        if (a == null || b == null || !a.exists()) return false;
        try {
            File dir = b.getParentFile();
            if (dir != null && !dir.exists() && !dir.mkdirs()) return false;
            if (b.exists()) deleteTree(b);
            return a.renameTo(b);
        } catch (Throwable t) {
            return false;
        }
    }

    public static String[] list(File base, String name) {
        File d = (name == null || name.trim().length() == 0) ? base : resolve(base, name);
        if (d == null || !d.exists() || !d.isDirectory()) return new String[0];
        File[] fs = d.listFiles();
        if (fs == null) return new String[0];
        java.util.Arrays.sort(fs);
        String[] out = new String[fs.length];
        for (int i = 0; i < fs.length; i++) {
            out[i] = fs[i].getName() + (fs[i].isDirectory() ? "/" : "");
        }
        return out;
    }

    public static long size(File base, String name) {
        File f = resolve(base, name);
        return (f != null && f.exists()) ? f.length() : -1;
    }
}
