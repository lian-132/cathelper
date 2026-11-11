package com.cathelperawa.owo.app.script;

import com.cathelperawa.owo.app.core.HookLog;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;

public final class ModPack {

    private static final String OLD_EXPORT_DIR =
            "/storage/emulated/0/Android/data/com.tencent.tim/files/脚本导出";

    private ModPack() {}

    public static File exportDir() {
        try {
            File root = com.cathelperawa.owo.app.core.DataDir.root();
            if (root != null) {
                File d = new File(root, "导出");
                if (!d.exists()) d.mkdirs();
                if (d.isDirectory()) return d;
            }
        } catch (Throwable ignore) {
        }
        File d = new File(OLD_EXPORT_DIR);
        if (!d.exists()) d.mkdirs();
        return d;
    }

    public static File oldExportDir() {
        return new File(OLD_EXPORT_DIR);
    }

    public static String exportOneError(String name) {
        try {
            File src = ScriptHost.dir();
            if (src == null) return "脚本目录不可用";
            if (name == null) return "脚本名为空";
            
            if (ScriptHost.isPackage(name)) return exportPackage(name);
            File f = new File(src, name.endsWith(".qqs") ? name : name + ".qqs");
            if (!f.exists()) return "源文件不存在：" + f.getPath();
            File dst = exportDir();
            if (!dst.isDirectory() && !dst.mkdirs()) return "无法创建导出目录：" + dst.getPath();
            File out = new File(dst, f.getName());
            copy(f, out);
            if (!out.exists() || out.length() == 0) return "写入失败：" + out.getPath();
            HookLog.i("[导出] " + f.getName() + " → " + out.getPath());
            return null;
        } catch (Throwable t) {
            HookLog.e("[导出] 失败", t);
            return "导出异常：" + t;
        }
    }

    public static boolean exportOne(String name) {
        return exportOneError(name) == null;
    }

    public static int exportAll() {
        int n = 0;
        try {
            File src = ScriptHost.dir();
            if (src == null) return 0;
            File[] fs = src.listFiles();
            if (fs == null) return 0;
            File dst = exportDir();
            for (int i = 0; i < fs.length; i++) {
                if (!fs[i].isFile() || !fs[i].getName().endsWith(".qqs")) continue;
                copy(fs[i], new File(dst, fs[i].getName()));
                n++;
            }
        } catch (Throwable t) {
            HookLog.e("[导出] 全部导出失败", t);
        }
        return n;
    }

    
    public static String importNewDetail() {
        int[] r = importNewStat();
        if (r[0] == 0 && r[1] == 0) return "导出目录是空的";
        StringBuilder sb = new StringBuilder();
        sb.append("导入 ").append(r[0]).append(" 项");
        if (r[1] > 0) sb.append("，跳过 ").append(r[1]).append(" 项（脚本目录已有同名，需先删掉原脚本再导入）");
        return sb.toString();
    }

    public static int[] importNewStat() {
        int imported = 0, skipped = 0;
        try {
            File src = exportDir();
            File dst = ScriptHost.dir();
            if (dst == null) return new int[]{0, 0};
            if (!dst.exists()) dst.mkdirs();
            File[] fs = src.listFiles();
            if (fs == null) return new int[]{0, 0};
            for (int i = 0; i < fs.length; i++) {
                File f = fs[i];
                if (!f.isFile()) continue;
                String nm = f.getName();
                if (nm.toLowerCase().endsWith(".zip")) {
                    String err = importPackageFile(f);
                    if (err == null) imported++;
                    else HookLog.w("[导入] 包 " + nm + "：" + err);
                    continue;
                }
                if (!(nm.endsWith(".qqs") || nm.endsWith(".txt"))) continue;
                File target = new File(dst, nm);
                if (target.exists()) { skipped++; continue; }   
                copy(f, target);
                imported++;
                HookLog.i("[导入] " + nm);
            }
        } catch (Throwable t) {
            HookLog.e("[导入] 失败", t);
        }
        return new int[]{imported, skipped};
    }

    public static int importNew() {
        return importNewStat()[0];
    }

    public static boolean importOne(String fileName) {
        try {
            File src = exportDir();
            File dst = ScriptHost.dir();
            if (dst == null || fileName == null) return false;
            File f = new File(src, fileName);
            if (!f.exists()) return false;
            File target = new File(dst, fileName);
            int i = 0;
            while (target.exists() && i < 20) {
                i++;
                String base = fileName.endsWith(".qqs")
                        ? fileName.substring(0, fileName.length() - 4) : fileName;
                target = new File(dst, base + "-导入" + i + (fileName.endsWith(".qqs") ? ".qqs" : ""));
            }
            copy(f, target);
            HookLog.i("[导入] " + fileName + " → " + target.getName());
            return true;
        } catch (Throwable t) {
            HookLog.e("[导入] 失败", t);
            return false;
        }
    }

    public static String[] listExport() {
        try {
            File[] fs = exportDir().listFiles();
            if (fs == null) return new String[0];
            java.util.List<String> out = new java.util.ArrayList<String>();
            for (int i = 0; i < fs.length; i++) {
                if (fs[i].isFile()) out.add(fs[i].getName());
            }
            java.util.Collections.sort(out);
            return out.toArray(new String[0]);
        } catch (Throwable t) {
            return new String[0];
        }
    }

    private static void copy(File from, File to) throws Exception {
        File dir = to.getParentFile();
        if (dir != null && !dir.isDirectory()) dir.mkdirs();
        InputStream in = new FileInputStream(from);
        try {
            FileOutputStream out = new FileOutputStream(to);
            try {
                byte[] buf = new byte[8192];
                int r;
                while ((r = in.read(buf)) > 0) out.write(buf, 0, r);
            } finally {
                out.close();
            }
        } finally {
            in.close();
        }
    }

    

    
    public static String exportPackage(String name) {
        try {
            File pkg = ScriptHost.packageDir(name);
            if (pkg == null) return "不是包脚本：" + name;
            File dst = exportDir();
            if (!dst.isDirectory() && !dst.mkdirs()) return "无法创建导出目录：" + dst.getPath();
            File out = new File(dst, name + "-包.zip");
            java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(
                    new java.io.FileOutputStream(out));
            try {
                String root = pkg.getAbsolutePath();
                addZipTree(zos, pkg, pkg.getName());
                HookLog.i("[导出] 包 " + name + " → " + out.getPath());
                return null;
            } finally {
                zos.close();
            }
        } catch (Throwable t) {
            HookLog.e("[导出] 包失败", t);
            return "导出包异常：" + t;
        }
    }

    private static void addZipTree(java.util.zip.ZipOutputStream zos, File dir, String prefix)
            throws Exception {
        File[] fs = dir.listFiles();
        if (fs == null) return;
        java.util.Arrays.sort(fs);
        for (int i = 0; i < fs.length; i++) {
            File f = fs[i];
            String entryName = prefix.length() == 0 ? f.getName() : prefix + "/" + f.getName();
            if (f.isDirectory()) {
                addZipTree(zos, f, entryName);
            } else {
                zos.putNextEntry(new java.util.zip.ZipEntry(entryName));
                java.io.FileInputStream in = new java.io.FileInputStream(f);
                try {
                    byte[] buf = new byte[8192];
                    int n;
                    while ((n = in.read(buf)) > 0) zos.write(buf, 0, n);
                } finally {
                    in.close();
                }
                zos.closeEntry();
            }
        }
    }

    
    public static String importPackageFile(File zipFile) {
        if (zipFile == null || !zipFile.isFile()) return "文件不存在";
        try {
            java.io.FileInputStream in = new java.io.FileInputStream(zipFile);
            try {
                return importPackageStream(in, zipFile.getName());
            } finally {
                in.close();
            }
        } catch (Throwable t) {
            return "导入包异常：" + t;
        }
    }

    public static String importPackageStream(java.io.InputStream raw, String displayName) {
        String base = displayName == null ? "导入包" : displayName;
        if (base.toLowerCase().endsWith(".zip")) base = base.substring(0, base.length() - 4);
        File root = ScriptHost.dir();
        if (root == null) return "脚本目录不可用";
        File target = new File(root, base);
        int n = 1;
        while (target.exists() && n < 50) {
            target = new File(root, base + "-" + (++n));
        }
        try {
            if (!target.mkdirs()) return "无法创建目录：" + target.getPath();
            if (raw == null) return "打不开这个压缩包";
            java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(raw);
            String rootPath = target.getCanonicalPath();
            try {
                java.util.zip.ZipEntry e;
                byte[] buf = new byte[8192];
                while ((e = zis.getNextEntry()) != null) {
                    String en = e.getName();
                    if (en.indexOf("..") >= 0 || en.startsWith("/")
                            || en.indexOf('\\') >= 0) {
                        HookLog.w("[导入包] 跳过可疑条目 " + en);
                        continue;
                    }
                    File f = new File(target, en);
                    String canon = f.getCanonicalPath();
                    if (!canon.equals(rootPath) && !canon.startsWith(rootPath + File.separator)) {
                        HookLog.w("[导入包] 越界条目 " + en);
                        continue;
                    }
                    if (e.isDirectory()) {
                        f.mkdirs();
                        continue;
                    }
                    File parent = f.getParentFile();
                    if (parent != null && !parent.exists()) parent.mkdirs();
                    java.io.FileOutputStream out = new java.io.FileOutputStream(f);
                    try {
                        int len;
                        while ((len = zis.read(buf)) > 0) out.write(buf, 0, len);
                    } finally {
                        out.close();
                    }
                }
            } finally {
                zis.close();
            }

            
            if (!new File(target, "main.qqs").isFile()
                    && !new File(target, "main.java").isFile()) {
                File[] cs = target.listFiles();
                if (cs != null && cs.length == 1 && cs[0].isDirectory()) {
                    File inner = cs[0];
                    boolean hasEntry = new File(inner, "main.qqs").isFile()
                            || new File(inner, "main.java").isFile();
                    if (hasEntry) {
                        File[] items = inner.listFiles();
                        if (items != null) {
                            for (int i = 0; i < items.length; i++) {
                                File to = new File(target, items[i].getName());
                                if (!items[i].renameTo(to)) {
                                    HookLog.w("[导入包] 上移失败：" + items[i].getName());
                                }
                            }
                        }
                        inner.delete();
                        HookLog.i("[导入包] 已剥掉顶层目录 " + inner.getName());
                    }
                }
            }
            if (!new File(target, "main.qqs").isFile()
                    && !new File(target, "main.java").isFile()) {
                deleteTree(target);
                return "这个 zip 里没有 main.qqs，不是脚本包";
            }
            HookLog.i("[导入包] " + base + " → " + target.getPath());
            return null;
        } catch (Throwable t) {
            HookLog.e("[导入包] 失败", t);
            try { deleteTree(target); } catch (Throwable ignore) {}
            return "导入包异常：" + t;
        }
    }

    private static void deleteTree(File f) {
        if (f == null || !f.exists()) return;
        if (f.isDirectory()) {
            File[] cs = f.listFiles();
            if (cs != null) for (int i = 0; i < cs.length; i++) deleteTree(cs[i]);
        }
        f.delete();
    }

    public static String importFromUri(android.content.Context ctx, android.net.Uri uri) {
        if (ctx == null || uri == null) return "没有拿到文件";
        try {
            
            String name = null;
            try {
                android.database.Cursor c = ctx.getContentResolver().query(uri, null, null, null, null);
                if (c != null) {
                    try {
                        int idx = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                        if (idx >= 0 && c.moveToFirst()) name = c.getString(idx);
                    } finally {
                        c.close();
                    }
                }
            } catch (Throwable ignore) {
            }
            if (name == null) name = uri.getLastPathSegment();
            if (name == null) return "拿不到文件名";
            int slash = name.lastIndexOf('/');
            if (slash >= 0) name = name.substring(slash + 1);
            
            if (name.toLowerCase().endsWith(".zip")) {
                java.io.InputStream zin = ctx.getContentResolver().openInputStream(uri);
                if (zin == null) return "打不开这个压缩包";
                try {
                    return importPackageStream(zin, name);
                } finally {
                    zin.close();
                }
            }
            if (!name.endsWith(".qqs") && !name.endsWith(".txt")) {
                return "只能导入 .qqs / .txt 文件，或 .zip 脚本包";
            }

            File dst = ScriptHost.dir();
            if (dst == null) return "脚本目录不可用";
            if (!dst.exists()) dst.mkdirs();
            File target = new File(dst, name);
            int i = 0;
            while (target.exists() && i < 20) {
                i++;
                String base = name.endsWith(".qqs")
                        ? name.substring(0, name.length() - 4) : name;
                target = new File(dst, base + "-导入" + i + (name.endsWith(".qqs") ? ".qqs" : ""));
            }

            java.io.InputStream in = ctx.getContentResolver().openInputStream(uri);
            if (in == null) return "打不开这个文件";
            try {
                java.io.FileOutputStream out = new java.io.FileOutputStream(target);
                try {
                    byte[] buf = new byte[8192];
                    int r;
                    while ((r = in.read(buf)) > 0) out.write(buf, 0, r);
                } finally {
                    out.close();
                }
            } finally {
                in.close();
            }

            
            try {
                target.setReadable(true, false);
                target.setWritable(true, false);
                target.setExecutable(false, false);
            } catch (Throwable ignore) {
            }
            HookLog.i("[导入] " + name + " → " + target.getName());
            return null;
        } catch (Throwable t) {
            HookLog.e("[导入] 失败", t);
            return "导入异常：" + t;
        }
    }
}
