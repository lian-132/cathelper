package com.cathelperawa.owo.app.core;

import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class BugReportExporter {

    private BugReportExporter() {}

    public static final class Result {
        public final File file;
        public final long bytes;
        public final String message;
        public Result(File file, long bytes, String message) {
            this.file = file;
            this.bytes = bytes;
            this.message = message;
        }
        public boolean ok() { return file != null && bytes > 0; }
    }

    public static Result export(Context ctx) {
        return export(ctx, BundleSource.fromContext(ctx));
    }

    public static Result export(Context ctx, BundleSource src) {
        if (src == null) return new Result(null, 0, "没有数据来源");
        File outDir = null;
        try {
            outDir = src.outDir();
            if (outDir == null) return new Result(null, 0, "无法创建输出目录");
            if (!outDir.exists() && !outDir.mkdirs()) return new Result(null, 0, "无法创建输出目录");

            File zip = new File(outDir, "cathelper-report-" + stamp() + ".zip");
            ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zip));
            try {
                putText(zos, "info.txt", info(ctx, src));
                putText(zos, "features.txt", HookItemFactory.snapshot());
                putFile(zos, "errors.log", new File(src.filesRoot(), "errors.log"));

                ConfigBackup.ExportResult cfg = ConfigBackup.exportDir(src.filesRoot(),
                        com.cathelperawa.owo.app.BuildInfo.VERSION_CODE);
                if (cfg.count > 0) {
                    putText(zos, "console", cfg.text);
                } else {

                    putText(zos, "console", "# 本机未读到配置（跨进程时配置在宿主私有目录）\n"
                            + "# 完整配置请用「跨进程打包」后的包，或手动查看 "
                            + "/data/user/0/com.tencent.tim/files/console\n");
                }
                putText(zos, "README.txt",
                        "这是猫娘助手的诊断包。\n"
                                + "包含：环境信息、功能状态、运行期错误、已过滤敏感项的配置。\n"
                                + "不包含：任何聊天记录、账号信息。\n");
            } finally {
                zos.close();
            }
            long len = zip.length();

            if (ctx != null) {
                try {
                    File marker = latestMarker(ctx);
                    if (marker != null) {
                        File p = marker.getParentFile();
                        if (p != null && !p.exists()) p.mkdirs();
                        java.io.FileOutputStream mo = new java.io.FileOutputStream(marker);
                        try {
                            mo.write(zip.getAbsolutePath().getBytes("UTF-8"));
                            mo.flush();
                        } finally {
                            mo.close();
                        }
                    }
                } catch (Throwable ignore) {
                }
            }
            return new Result(zip, len, "已打包 " + len / 1024 + " KB");
        } catch (Throwable t) {
            return new Result(null, 0, "打包失败：" + t);
        }
    }

    public abstract static class BundleSource {

        public abstract File filesRoot();

        public abstract File outDir();

        public int hostVersion() { return 0; }

        public String hostVersionName() { return "?"; }

        public static BundleSource fromContext(final Context ctx) {
            return new BundleSource() {
                @Override public File filesRoot() {
                    try {
                        return new File(ctx.getFilesDir(), "cathelper");
                    } catch (Throwable t) {
                        return null;
                    }
                }
                @Override public File outDir() {

                    return externalReportsDir(ctx);
                }
                @Override public int hostVersion() {
                    try { return HostVersion.code(ctx); } catch (Throwable t) { return 0; }
                }
                @Override public String hostVersionName() {
                    try { return HostVersion.name(ctx); } catch (Throwable t) { return "?"; }
                }
            };
        }
    }

    public static final String EXTERNAL_DIR = "Reports";

    public static File externalReportsDir(Context ctx) {

        try {
            File base = ctx.getExternalFilesDir(null);
            if (base != null) return new File(base, EXTERNAL_DIR);
        } catch (Throwable ignore) {
        }
        try {
            File[] dirs = ctx.getExternalFilesDirs(null);
            if (dirs != null && dirs.length > 0 && dirs[0] != null) {
                return new File(dirs[0], EXTERNAL_DIR);
            }
        } catch (Throwable ignore) {
        }

        try {
            return new File(ctx.getFilesDir(), EXTERNAL_DIR);
        } catch (Throwable t) {
            return null;
        }
    }

    private static String info(Context ctx, BundleSource src) {
        StringBuilder sb = new StringBuilder(512);
        sb.append("══ 猫娘助手诊断包 ══\n");
        sb.append("生成时间 : ").append(new java.text.SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss", java.util.Locale.ROOT).format(new java.util.Date())).append('\n');
        sb.append("模块版本 : ").append(com.cathelperawa.owo.app.BuildInfo.VERSION_NAME)
                .append(" (").append(com.cathelperawa.owo.app.BuildInfo.VERSION_CODE).append(")\n");
        sb.append("宿主版本 : ").append(src.hostVersionName())
                .append(" (").append(src.hostVersion()).append(")\n");
        sb.append("进程     : ").append(safeProcessName()).append('\n');
        sb.append("错误统计 : 共 ").append(RuntimeErrors.totalCount())
                .append(" 次 / ").append(RuntimeErrors.kindCount()).append(" 类")
                .append("；落盘失败 ").append(RuntimeErrors.flushFailures()).append(" 次\n");
        return sb.toString();
    }

    private static String safeProcessName() {
        try {
            return "pid=" + android.os.Process.myPid();
        } catch (Throwable t) {
            return "?";
        }
    }

    private static void putText(ZipOutputStream zos, String name, String text) throws Exception {        if (text == null) text = "";
        zos.putNextEntry(new ZipEntry(name));
        zos.write(text.getBytes("UTF-8"));
        zos.closeEntry();
    }

    private static void putFile(ZipOutputStream zos, String name, File f) {
        if (f == null || !f.isFile()) return;
        java.io.FileInputStream in = null;
        try {
            zos.putNextEntry(new ZipEntry(name));
            in = new java.io.FileInputStream(f);
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) zos.write(buf, 0, n);
            zos.closeEntry();
        } catch (Throwable ignore) {

        } finally {
            if (in != null) try { in.close(); } catch (Throwable ignore) { }
        }
    }

    private static String stamp() {
        return new java.text.SimpleDateFormat("yyyyMMdd-HHmmss", java.util.Locale.ROOT)
                .format(new java.util.Date());
    }

    public static File exchangeDir(Context ctx) {
        return externalReportsDir(ctx);
    }

    public static File latestMarker(Context ctx) {
        File d = exchangeDir(ctx);
        return d == null ? null : new File(d, "latest.txt");
    }

    public static Result exportAndShare(Context ctx) {
        Result r = export(ctx);
        if (!r.ok()) return r;
        try {
            android.os.StrictMode.VmPolicy oldPolicy = null;
            try {
                oldPolicy = android.os.StrictMode.getVmPolicy();
                android.os.StrictMode.setVmPolicy(
                        new android.os.StrictMode.VmPolicy.Builder().build());
            } catch (Throwable ignore) {
            }

            android.net.Uri uri = android.net.Uri.fromFile(r.file);
            android.content.Intent send =
                    new android.content.Intent(android.content.Intent.ACTION_SEND);
            send.setType("application/zip");
            send.putExtra(android.content.Intent.EXTRA_STREAM, uri);
            send.putExtra(android.content.Intent.EXTRA_SUBJECT, "猫娘助手诊断包");
            send.putExtra(android.content.Intent.EXTRA_TEXT,
                    "模块 v" + com.cathelperawa.owo.app.BuildInfo.VERSION_NAME
                            + "，已过滤敏感配置，不含聊天记录。\n"
                            + "文件位置：" + r.file.getAbsolutePath());
            android.content.Intent chooser =
                    android.content.Intent.createChooser(send, "发送诊断包");
            chooser.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(chooser);

            if (oldPolicy != null) {
                try { android.os.StrictMode.setVmPolicy(oldPolicy); } catch (Throwable ignore) { }
            }
        } catch (Throwable t) {
            return new Result(r.file, r.bytes, r.message + "（分享失败：" + t + "）");
        }
        return new Result(r.file, r.bytes,
                r.message + " → " + r.file.getAbsolutePath());
    }
}
