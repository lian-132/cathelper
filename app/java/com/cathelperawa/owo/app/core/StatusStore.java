package com.cathelperawa.owo.app.core;

import android.content.Context;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

public final class StatusStore {

    private static final String FILE = "status.txt";

    private StatusStore() {}

    public static String hostFiles() {
        java.io.File dir = hostFilesDir();
        return dir == null ? null : new java.io.File(dir, FILE).getAbsolutePath();
    }

    public static java.io.File hostFilesDir() {
        try {
            android.content.Context ctx = HookItemFactory.context();
            if (ctx != null) {
                java.io.File d = ctx.getFilesDir();
                if (d != null) return d;
            }
        } catch (Throwable ignore) {
        }
        return new java.io.File(userDataRoot(), TIM_PKG + "/files");
    }

    public static String hostFilesMsf() {
        java.io.File dir = hostFilesDir();
        return dir == null ? null
                : new java.io.File(new java.io.File(dir, "cathelper"), FILE).getAbsolutePath();
    }

    private static final String TIM_PKG = "com.tencent.tim";

    private static String userDataRoot() {
        try {
            int uid = android.os.Process.myUid();
            int userId = uid / 100000;
            if (userId == 0) return "/data/data";
            return "/data/user/" + userId;
        } catch (Throwable t) {
            return "/data/data";
        }
    }

    @Deprecated
    public static final String HOST_FILES = "/data/data/" + TIM_PKG + "/files/" + FILE;

    public static void startAutoRefresh(final Context ctx) {
        if (ctx == null || timer != null) return;
        timer = new java.util.Timer("status-refresh", true);
        timer.schedule(new java.util.TimerTask() {
            @Override public void run() {
                try {
                    int now = totalFireCount();
                    if (now == lastSavedFireTotal) return;
                    lastSavedFireTotal = now;
                    saveQuiet(ctx);
                } catch (Throwable ignore) {
                }
            }
        }, 60000L, 60000L);
    }

    public static void stopAutoRefresh() {
        try {
            if (timer != null) timer.cancel();
        } catch (Throwable ignore) {
        }
        timer = null;
    }

    private static int totalFireCount() {
        int n = 0;
        try {
            for (HookItemBase item : HookItemFactory.all()) {
                FeatureReport r = item.report();
                if (r != null) n += r.fireCount();
            }
        } catch (Throwable ignore) {
        }
        return n;
    }

    private static void saveQuiet(Context ctx) {
        try {
            buildAndWrite(ctx);
        } catch (Throwable ignore) {
        }
    }

    private static java.util.Timer timer;
    private static int lastSavedFireTotal = -1;

    public static void save(Context ctx) {
        if (ctx == null) return;
        buildAndWrite(ctx);
    }

    private static void buildAndWrite(Context ctx) {
        StringBuilder sb = new StringBuilder();
        sb.append("# 猫娘助手 功能状态 · ")
                .append(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss",
                        java.util.Locale.US).format(new java.util.Date()))
                .append('\n');
        try {
            java.util.List<HookItemBase> all = HookItemFactory.all();
            for (HookItemBase item : all) {
                HookItem meta = item.rawMeta();
                FeatureReport rep = item.report();
                if (meta == null || rep == null) continue;
                com.cathelperawa.owo.app.core.SelfCheckResult sc = item.selfCheck();
                sb.append("F\t")
                        .append(safe(item.switchKey())).append('\t')
                        .append(safe(item.displayName())).append('\t')
                        .append(safe(item.displayPath())).append('\t')
                        .append(rep.status().name()).append('\t')
                        .append(safe(rep.summary())).append('\t')
                        .append(safe(rep.reason())).append('\t')

                        .append(rep.fireCount()).append('\t')
                        .append(safe(rep.fireSummary())).append('\t')

                        .append(rep.isInstrumented() ? "1" : "0").append('\t')

                        .append(sc == null || sc.isEmpty() ? "" :
                                (sc.passCount() + "/" + sc.total())).append('\t')
                        .append(sc == null || sc.isEmpty() ? "" :
                                safe(sc.firstFailReason())).append('\t')

                        .append(rep.runtimeErrorCount()).append('\n');
            }
        } catch (Throwable t) {
            HookLog.e("StatusStore.save 收集失败", t);
        }
        write(new File(ctx.getFilesDir(), FILE), sb.toString());
    }

    public static String readHostStatus() {

        java.util.List<String> paths = new java.util.ArrayList<String>();
        String p1 = hostFiles();
        String p2 = hostFilesMsf();
        if (p1 != null) paths.add(p1);
        if (p2 != null) paths.add(p2);
        for (String p : paths) {
            try {
                File f = new File(p);
                if (!f.exists()) continue;
                String s = read(f);
                if (s != null && s.trim().length() > 0) return s;
            } catch (Throwable ignore) {
            }
        }

        for (String p : paths) {
            String s = readViaSu(p);
            if (s != null && s.trim().length() > 0) return s;
        }
        return null;
    }

    private static String readViaSu(String path) {
        java.lang.Process proc = null;
        try {

            proc = new ProcessBuilder("su", "-c", "cat '" + path + "'").redirectErrorStream(true).start();
            final java.lang.Process fp = proc;
            final boolean[] done = {false};
            Thread watchdog = new Thread(new Runnable() {
                @Override public void run() {
                    try {
                        Thread.sleep(3000);
                        if (!done[0]) {
                            try { fp.destroy(); } catch (Throwable ignore) {}
                        }
                    } catch (InterruptedException ignore) {
                    }
                }
            });
            watchdog.setDaemon(true);
            watchdog.start();

            java.io.InputStream in = proc.getInputStream();
            java.io.ByteArrayOutputStream bo = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) > 0) bo.write(buf, 0, n);
            done[0] = true;
            try { proc.waitFor(); } catch (Throwable ignore) {}
            String s = new String(bo.toByteArray(), "UTF-8");

            return s.contains("F\t") ? s : null;
        } catch (Throwable t) {
            return null;
        } finally {
            try { if (proc != null) proc.destroy(); } catch (Throwable ignore) {}
        }
    }

    public static String read(File f) {
        FileInputStream in = null;
        try {
            in = new FileInputStream(f);
            byte[] buf = new byte[(int) Math.min(f.length(), 64 * 1024)];
            int n = in.read(buf);
            return n <= 0 ? "" : new String(buf, 0, n, "UTF-8");
        } catch (Throwable t) {
            return null;
        } finally {
            try { if (in != null) in.close(); } catch (Throwable ignore) {}
        }
    }

    private static void write(File f, String content) {
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(f);
            out.write(content.getBytes("UTF-8"));
        } catch (Throwable t) {
            HookLog.e("StatusStore 写盘失败 " + f, t);
        } finally {
            try { if (out != null) out.close(); } catch (Throwable ignore) {}
        }
    }

    private static String safe(String s) {
        if (s == null) return "";
        return s.replace('\t', ' ').replace('\n', ' ');
    }

    @SuppressWarnings("unused")
    private static void writeProps(File f, Properties p) {
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(f);
            p.store(out, "cathelper status");
        } catch (Throwable ignore) {
        } finally {
            try { if (out != null) out.close(); } catch (Throwable ignore) {}
        }
    }
}
