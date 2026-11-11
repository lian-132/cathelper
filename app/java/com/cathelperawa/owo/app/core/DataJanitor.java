package com.cathelperawa.owo.app.core;

import android.content.Context;

import com.cathelperawa.owo.app.api.IoApi;

import java.io.File;

public final class DataJanitor {

    private DataJanitor() {}

    private static final long DAY = 24L * 60 * 60 * 1000;

    private static volatile long lastRun = 0L;
    private static volatile boolean scheduled = false;

    public static void schedule(final Context ctx) {
        if (ctx == null || scheduled) return;
        scheduled = true;
        Thread t = new Thread(new Runnable() {
            @Override public void run() {
                try { Thread.sleep(60000); } catch (Throwable ignore) {}
                runIfDue(ctx);
            }
        }, "cat-data-janitor");
        t.setDaemon(true);
        t.start();
    }

    public static void runIfDue(Context ctx) {
        long now = System.currentTimeMillis();
        if (now - lastRun < 6L * 60 * 60 * 1000) return;
        lastRun = now;
        try {
            int n = runOnce(ctx);
            if (n > 0) HookLog.i("数据保洁：清掉 " + n + " 个过期文件");
        } catch (Throwable t) {
            HookLog.w("数据保洁失败: " + t);
        }
    }

    public static int runOnce(Context ctx) {
        if (ctx == null) return 0;
        int n = 0;

        File legacy = null;
        try {
            legacy = IoApi.dataDir();
        } catch (Throwable ignore) {
        }
        if (legacy != null && legacy.isDirectory()) {
            n += migratePrivacy(legacy);
            n += purge(new File(legacy, "rkey"), 7 * DAY, true);
            n += purgeFile(new File(legacy, "rkey_dump.bin"), 1 * DAY);
            n += purgeFile(new File(legacy, "atdiag.txt"), 14 * DAY);
            n += purgeFile(new File(legacy, "mixdiag.txt"), 14 * DAY);
            n += purgeFile(new File(legacy, "mixkernel.txt"), 14 * DAY);
            n += purgeFile(new File(legacy, "mixkernel.txt.1"), 14 * DAY);
            n += purgeFile(new File(legacy, "parse_diag.txt"), 14 * DAY);
            n += purgeFile(new File(legacy, "oidb_capture.txt"), 14 * DAY);
            n += purgeFile(new File(legacy, "ai_debug.log"), 7 * DAY);
        }

        
        File root = null;
        try {
            root = DataDir.root();
        } catch (Throwable ignore) {
        }
        if (root == null) return n;

        
        try {
            DataDir.absorbPending();
        } catch (Throwable ignore) {
        }

        File g = null;
        try {
            g = DataDir.global();
        } catch (Throwable ignore) {
        }
        if (g != null) {
            n += purge(new File(g, "diag"), 14 * DAY, true);
            n += purgeFile(new File(new File(g, "diag"), "rkey_dump.bin"), 1 * DAY);
            File cache = new File(g, "cache");
            n += purge(new File(cache, "rkey"), 7 * DAY, true);
            n += purge(new File(cache, "parse"), 30 * DAY, true);
            n += purgeFile(new File(cache, "rkey_dump.bin"), 1 * DAY);
            n += purgeFile(new File(g, "ark_probe.bin"), 3 * DAY);
        }

        File[] accounts = root.listFiles();
        if (accounts != null) {
            for (int i = 0; i < accounts.length; i++) {
                File a = accounts[i];
                if (!a.isDirectory() || !a.getName().matches("\\d{4,}")) continue;
                n += purgeFile(new File(a, "log/ai_debug.log"), 7 * DAY);
                n += purgeFile(new File(a, "log.txt"), 14 * DAY);
                n += purge(new File(a, "state"), 60 * DAY, true);
            }
        }

        return n;
    }

    private static int purgeFile(File f, long ttl) {
        if (f == null || !f.isFile()) return 0;
        try {
            if (System.currentTimeMillis() - f.lastModified() > ttl && f.delete()) return 1;
        } catch (Throwable ignore) {
        }
        return 0;
    }

    private static int migratePrivacy(File root) {
        int n = 0;
        long now = System.currentTimeMillis();

        String[] jsons = {"ai_history.json", "ai_affinity.json", "voice_text.json", "ptt_index.json"};
        for (String name : jsons) {
            File f = new File(root, name);
            if (!f.isFile() || f.length() == 0) continue;
            if (now - f.lastModified() < 30000L) continue;      
            int[] st = new int[1];
            String text = SecureFile.readText(f, st);
            if (st[0] == SecureFile.PLAIN && text != null && text.length() > 0) {
                if (SecureFile.writeText(f, text)) n++;
            }
        }

        String uin = null;
        try {
            uin = com.cathelperawa.owo.app.MsgStore.currentUin();
        } catch (Throwable ignore) {
        }
        if (uin != null && uin.length() > 0 && uin.matches("\\d{4,}")) {
            File dir = new File(root, uin);
            String[] lines = {"msg.txt", "peers.txt", "scope.txt"};
            for (String name : lines) {
                File f = new File(dir, name);
                if (!f.isFile() || f.length() == 0) continue;
                if (now - f.lastModified() < 60000L) continue;
                if (SecureFile.migrateLines(f)) n++;
            }

            n += migrateBusyLog(new File(dir, "log.txt"));
        }
        n += migrateBusyLog(new File(root, "ai_debug.log"));
        return n;
    }

    private static int migrateBusyLog(File f) {
        if (f == null || !f.isFile() || f.length() == 0) return 0;
        try {
            return SecureFile.migrateLines(f) ? 1 : 0;
        } catch (Throwable t) {
            return 0;
        }
    }

    private static int purge(File dir, long ttl, boolean recursiveOneLevel) {
        if (dir == null || !dir.isDirectory()) return 0;
        int n = 0;
        File[] fs = dir.listFiles();
        if (fs == null) return 0;
        for (File f : fs) {
            if (f.isDirectory()) {
                if (recursiveOneLevel) n += purge(f, ttl, false);
                continue;
            }
            n += purgeFile(f, ttl);
        }
        return n;
    }
}
