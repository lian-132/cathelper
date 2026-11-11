package com.cathelperawa.owo.app.core;

import com.cathelperawa.owo.app.core.DataDir;

import android.content.Context;

import java.io.File;

public final class ConfigBank {

    private ConfigBank() {}

    public static final String SNAPSHOT_NAME = "config-snapshot.txt";

    public static File exchangeDir(Context ctx) {
        return BugReportExporter.externalReportsDir(ctx);
    }

    public static File snapshotFile(Context ctx) {
        File d = exchangeDir(ctx);
        return d == null ? null : new File(d, SNAPSHOT_NAME);
    }

    

    
    public static final class Result {
        public final File file;
        public final int count;
        public final String message;
        public Result(File file, int count, String message) {
            this.file = file;
            this.count = count;
            this.message = message;
        }
        public boolean ok() { return file != null && file.isFile() && count > 0; }
    }

    public static Result export(Context ctx) {
        if (ctx == null) return new Result(null, 0, "环境没准备好，先在 TIM 里开一下控制台");
        File dir = exchangeDir(ctx);
        if (dir == null) return new Result(null, 0, "找不到放文件的地方…");
        if (!dir.exists() && !dir.mkdirs()) return new Result(null, 0, "放文件的文件夹建不出来，检查下存储权限");

        
        try {
            ModuleCommand.send(ctx, ModuleCommand.CMD_SNAPSHOT_CONFIG);
            Thread.sleep(500);      
        } catch (Throwable ignore) {
        }

        String text = null;
        int count = 0;
        try {
            File snap = snapshotFile(ctx);
            if (snap != null && snap.isFile()) {
                text = readAll(snap);
                count = countEntries(text);
            }
        } catch (Throwable ignore) {
        }
        if (text == null || count == 0) {
            try {
                ConfigBackup.ExportResult r = ConfigBackup.exportDir(
                        DataDir.global(), BuildInfoVer());
                text = r.text;
                count = r.count;
            } catch (Throwable ignore) {
            }
        }
        if (text == null || count == 0) {
            return new Result(null, 0, "一条配置都没读到，TIM 开着吗？");
        }

        String name = "cathelper-config-"
                + new java.text.SimpleDateFormat("yyyyMMdd-HHmmss", java.util.Locale.ROOT)
                .format(new java.util.Date()) + ".txt";
        File out = new File(dir, name);
        try {
            writeAll(out, text);
        } catch (Throwable t) {
            return new Result(null, 0, "文件写不进去…" + t);
        }
        return new Result(out, count, "导出完成！共 " + count + " 项～");
    }

    

    
    public static Result importText(Context ctx, String text) {
        if (ctx == null) return new Result(null, 0, "环境没准备好，先在 TIM 里开一下控制台");
        if (text == null || text.trim().isEmpty()) {
            return new Result(null, 0, "粘进来的内容好像是空的诶");
        }
        int count = countEntries(text);
        if (count == 0) {
            return new Result(null, 0, "这段文本里没找到配置，是不是复制漏了？");
        }
        File dir = exchangeDir(ctx);
        if (dir == null) return new Result(null, 0, "找不到放文件的地方…");
        if (!dir.exists() && !dir.mkdirs()) return new Result(null, 0, "放文件的文件夹建不出来，检查下存储权限");

        File pending = new File(dir, "config-import.txt");
        try {
            writeAll(pending, text);
        } catch (Throwable t) {
            return new Result(null, 0, "中转文件写不进去…" + t);
        }
        try {
            ModuleCommand.send(ctx, ModuleCommand.CMD_IMPORT_CONFIG);
        } catch (Throwable t) {
            return new Result(null, 0, "喊不动 TIM 那边，它可能没运行：" + t);
        }
        return new Result(pending, count, "交给 TIM 处理啦，共 " + count + " 项");
    }

    public static Result importFrom(Context ctx, File src) {
        if (ctx == null || src == null || !src.isFile()) {
            return new Result(null, 0, "这个文件不见了");
        }
        File dir = exchangeDir(ctx);
        if (dir == null) return new Result(null, 0, "找不到放文件的地方…");
        if (!dir.exists() && !dir.mkdirs()) return new Result(null, 0, "放文件的文件夹建不出来，检查下存储权限");

        int count;
        try {
            String text = readAll(src);
            count = countEntries(text);
            if (count == 0) return new Result(null, 0, "文件里没找到可导入的配置诶");
            
            writeAll(new File(dir, "config-import.txt"), text);
        } catch (Throwable t) {
            return new Result(null, 0, "文件读不进来…" + t);
        }
        try {
            ModuleCommand.send(ctx, ModuleCommand.CMD_IMPORT_CONFIG);
        } catch (Throwable t) {
            return new Result(null, 0, "喊不动 TIM 那边，它可能没运行：" + t);
        }
        return new Result(src, count, "交给 TIM 处理啦，共 " + count + " 项");
    }

    public static ImportResult applyPendingImport(Context hostCtx) {
        if (hostCtx == null) return new ImportResult(false, 0, "没有宿主上下文");
        File dir = exchangeDir(hostCtx);
        if (dir == null) return new ImportResult(false, 0, "找不到放文件的地方…");
        File pending = new File(dir, "config-import.txt");
        if (!pending.isFile()) return new ImportResult(false, 0, "没有待导入文件");
        ConfigBackup.ImportResult r = ConfigBackup.importDirFile(
                hostCtx.getFilesDir(), pending, BuildInfoVer());
        try { pending.delete(); } catch (Throwable ignore) { }
        return new ImportResult(r.ok, r.applied, r.message);
    }

    public static int writeSnapshot(Context hostCtx) {
        if (hostCtx == null) return 0;
        File dir = exchangeDir(hostCtx);
        if (dir == null) return 0;
        if (!dir.exists() && !dir.mkdirs()) return 0;
        try {
            ConfigBackup.ExportResult r = ConfigBackup.exportDir(
                    hostCtx.getFilesDir(), BuildInfoVer());
            writeAll(new File(dir, SNAPSHOT_NAME), r.text);
            return r.count;
        } catch (Throwable t) {
            return 0;
        }
    }

    public static final class ImportResult {
        public final boolean ok;
        public final int applied;
        public final String message;
        public ImportResult(boolean ok, int applied, String message) {
            this.ok = ok;
            this.applied = applied;
            this.message = message;
        }
    }

    

    
    public static int countEntries(String text) {
        if (text == null) return 0;
        int n = 0;
        for (String line : text.split("\n")) {
            String s = line.trim();
            if (s.isEmpty() || s.startsWith("#") || s.startsWith("__")) continue;
            if (s.indexOf('=') > 0) n++;
        }
        return n;
    }

    private static String readAll(File f) throws Exception {
        byte[] buf = new byte[(int) f.length()];
        java.io.FileInputStream in = new java.io.FileInputStream(f);
        try {
            int off = 0, n;
            while (off < buf.length && (n = in.read(buf, off, buf.length - off)) > 0) off += n;
            return new String(buf, 0, off, "UTF-8");
        } finally {
            in.close();
        }
    }

    private static void writeAll(File f, String text) throws Exception {
        java.io.FileOutputStream out = new java.io.FileOutputStream(f);
        try {
            out.write(text.getBytes("UTF-8"));
            out.flush();
        } finally {
            out.close();
        }
    }

    private static int BuildInfoVer() {
        try {
            return com.cathelperawa.owo.app.BuildInfo.VERSION_CODE;
        } catch (Throwable t) {
            return 0;
        }
    }
}
