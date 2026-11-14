package com.cathelperawa.owo.app.core;

import com.cathelperawa.owo.app.core.DataDir;

import android.content.Context;

import java.io.File;

public final class SafeMode {

    public static final String FLAG = "cathelper_safe_mode";

    private static final String PROGRESS = "loading";

    private static final int AUTO_SAFE_THRESHOLD = 2;

    private static volatile boolean sSafeThisBoot;
    private static volatile String sWhy = "";
    private static volatile int sCrashCount;

    private SafeMode() {}

    public static boolean active() { return sSafeThisBoot; }

    public static String why() { return sWhy; }

    public static int crashCount() { return sCrashCount; }

    public static synchronized boolean beginLoad(Context ctx) {
        sSafeThisBoot = false;
        sWhy = "";
        try {

            File manual = flagFile(ctx);
            if (manual != null && manual.exists()) {
                sSafeThisBoot = true;
                sWhy = "检测到安全模式标记文件：" + manual.getAbsolutePath();
                HookLog.w("进入安全模式（手动）→ 本次不加载任何功能 hook");
                return true;
            }

            File progress = progressFile(ctx);
            boolean mainProc = Ipc.currentProcMask() == Ipc.PROC_MAIN;
            sCrashCount = mainProc ? readCount(ctx) : 0;
            if (mainProc && progress != null && progress.exists()) {
                sCrashCount++;
                writeCount(ctx, sCrashCount);
                HookLog.w("上次加载未完成（连续 " + sCrashCount + " 次）");
            }

            if (sCrashCount >= AUTO_SAFE_THRESHOLD) {
                sSafeThisBoot = true;
                sWhy = "连续 " + sCrashCount + " 次启动未完成加载，自动进入安全模式";
                HookLog.w("进入安全模式（自动）→ 本次不加载任何功能 hook");
                return true;
            }

            if (mainProc && progress != null) {
                try {
                    File dir = progress.getParentFile();
                    if (dir != null && !dir.isDirectory()) dir.mkdirs();
                    progress.createNewFile();
                } catch (Throwable ignore) {
                }
            }
        } catch (Throwable t) {
            HookLog.e("SafeMode.beginLoad 失败", t);
        }
        return false;
    }

    public static synchronized void markReady(Context ctx) {

        if (Ipc.currentProcMask() != Ipc.PROC_MAIN) return;
        try {
            File progress = progressFile(ctx);
            if (progress != null && progress.exists()) progress.delete();
            writeCount(ctx, 0);
            sCrashCount = 0;
        } catch (Throwable ignore) {
        }
    }

    public static boolean enableForNextBoot(Context ctx) {

        boolean any = false;
        File[] all = flagFiles(ctx);
        for (int i = 0; i < all.length; i++) {
            try {
                File f = all[i];
                if (f == null) continue;
                File dir = f.getParentFile();
                if (dir != null && !dir.isDirectory()) dir.mkdirs();
                if (f.exists() || f.createNewFile()) any = true;
            } catch (Throwable t) {
                HookLog.w("写安全模式标记失败(" + all[i] + ")：" + t);
            }
        }
        if (!any) HookLog.e("写安全模式标记失败：三个位置都写不进去", null);
        return any;
    }

    private static volatile long sGestureAt = 0L;

    private static final long GESTURE_WINDOW_MS = 60000L;

    public static void armGesture() {
        sGestureAt = System.currentTimeMillis();
        HookLog.w("[安全模式] 手势闸门已通过（60 秒内有效）");
    }

    public static boolean gestureHeld() {
        return sGestureAt > 0L
                && (System.currentTimeMillis() - sGestureAt) < GESTURE_WINDOW_MS;
    }

    public static boolean disable(Context ctx) {

        boolean ok = true;
        File[] all = flagFiles(ctx);
        for (int i = 0; i < all.length; i++) {
            try {
                if (all[i] != null && all[i].exists() && !all[i].delete()) ok = false;
            } catch (Throwable t) {
                ok = false;
            }
        }
        try {
            writeCount(ctx, 0);
        } catch (Throwable ignore) {
        }
        sCrashCount = 0;
        return ok;
    }

    public static String summary(Context ctx) {
        File f = flagFile(ctx);
        return "安全模式：" + (sSafeThisBoot ? "本次已启用" : "否")
                + (sWhy.isEmpty() ? "" : "（" + sWhy + "）")
                + "\n连续未完成：" + sCrashCount
                + "\n标记文件：" + (f == null ? "不可用" : f.getAbsolutePath())
                + (f != null && f.exists() ? "（存在）" : "（不存在）");
    }

    private static File flagFile(Context ctx) {
        File[] all = flagFiles(ctx);
        for (int i = 0; i < all.length; i++) {
            if (all[i] != null && all[i].exists()) return all[i];
        }
        return all.length == 0 ? null : all[0];
    }

    private static File[] flagFiles(Context ctx) {
        if (ctx == null) return new File[0];
        java.util.ArrayList<File> out = new java.util.ArrayList<File>(3);
        try {
            File ext = ctx.getExternalFilesDir(null);
            if (ext != null) out.add(new File(ext, FLAG));
        } catch (Throwable ignore) {
        }
        try {
            out.add(new File(ctx.getFilesDir(), FLAG));
        } catch (Throwable ignore) {
        }
        try {

            File dir = new File(ctx.getFilesDir(), "safe");
            if (!dir.isDirectory()) dir.mkdirs();
            out.add(new File(dir, FLAG));
        } catch (Throwable ignore) {
        }
        return out.toArray(new File[0]);
    }

    private static File progressFile(Context ctx) {
        try {
            File dir = DataDir.account();
            if (!dir.isDirectory()) dir.mkdirs();
            return new File(dir, PROGRESS);
        } catch (Throwable t) {
            return null;
        }
    }

    private static File countFile(Context ctx) {
        try {
            File dir = DataDir.account();
            if (!dir.isDirectory()) dir.mkdirs();
            return new File(dir, "load_crash_count");
        } catch (Throwable t) {
            return null;
        }
    }

    private static int readCount(Context ctx) {

        int best = 0;
        File[] all = countFiles(ctx);
        for (int i = 0; i < all.length; i++) {
            int v = readOne(all[i]);
            if (v > best) best = v;
        }
        return best;
    }

    private static int readOne(File f) {
        try {
            if (f == null || !f.isFile()) return 0;
            byte[] b = new byte[(int) Math.min(f.length(), 16)];
            java.io.FileInputStream in = new java.io.FileInputStream(f);
            int n = in.read(b);
            in.close();
            String s = new String(b, 0, Math.max(n, 0)).trim();
            return Integer.parseInt(s);
        } catch (Throwable t) {
            return 0;
        }
    }

    private static void writeCount(Context ctx, int v) {
        File[] all = countFiles(ctx);
        for (int i = 0; i < all.length; i++) {
            try {
                if (all[i] == null) continue;
                File dir = all[i].getParentFile();
                if (dir != null && !dir.isDirectory()) dir.mkdirs();
                java.io.FileOutputStream out = new java.io.FileOutputStream(all[i]);
                out.write(String.valueOf(v).getBytes("UTF-8"));
                out.close();
            } catch (Throwable ignore) {
            }
        }
    }

    private static File[] countFiles(Context ctx) {
        java.util.ArrayList<File> out = new java.util.ArrayList<File>(3);
        try {
            File dir = DataDir.account();
            if (dir != null) out.add(new File(dir, "load_crash_count"));
        } catch (Throwable ignore) {
        }
        try {
            if (ctx != null) out.add(new File(ctx.getFilesDir(), "load_crash_count"));
        } catch (Throwable ignore) {
        }
        try {
            if (ctx != null) {
                File ext = ctx.getExternalFilesDir(null);
                if (ext != null) out.add(new File(ext, "load_crash_count"));
            }
        } catch (Throwable ignore) {
        }
        return out.toArray(new File[0]);
    }
}
