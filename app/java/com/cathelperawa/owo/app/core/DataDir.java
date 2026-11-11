package com.cathelperawa.owo.app.core;

import com.cathelperawa.owo.app.bridge.QQEnv;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public final class DataDir {

    private DataDir() {}

    public static final String DIR_NAME = "猫娘助手";
    
    public static final String GLOBAL = "global";
    
    private static final String LEGACY = "cathelper";

    private static volatile File ROOT;
    private static volatile boolean migrated;
    
    private static volatile boolean pendingAbsorbed;
    
    private static volatile String LAST_UIN;

    private static final String K_LAST_UIN = "last_uin";

    private static void saveLastUin(File root, String uin) {
        try {
            File g = new File(root, GLOBAL);
            ensure(g);
            File f = new File(g, K_LAST_UIN);
            java.io.FileOutputStream out = new java.io.FileOutputStream(f);
            try {
                out.write(uin.getBytes("UTF-8"));
            } finally {
                try { out.close(); } catch (Throwable ignore) {}
            }
        } catch (Throwable ignore) {
        }
    }

    private static String loadLastUin(File root) {
        try {
            File f = new File(new File(root, GLOBAL), K_LAST_UIN);
            if (!f.isFile() || f.length() <= 0 || f.length() > 32) return null;
            java.io.FileInputStream in = new java.io.FileInputStream(f);
            byte[] buf = new byte[(int) f.length()];
            int n;
            try {
                n = in.read(buf);
            } finally {
                try { in.close(); } catch (Throwable ignore) {}
            }
            if (n <= 0) return null;
            String s = new String(buf, 0, n, "UTF-8").trim();
            return s.matches("\\d{4,}") ? s : null;
        } catch (Throwable t) {
            return null;
        }
    }
    
    private static final long BOOT_MS = System.currentTimeMillis();

    

    
    public static File root() {
        File r = ROOT;
        if (r != null && r.exists()) return r;
        synchronized (DataDir.class) {
            if (ROOT != null && ROOT.exists()) return ROOT;
            File d = pickRoot();
            if (d != null) {
                ensure(d);
                ensure(new File(d, GLOBAL));

                
                ROOT = d;
                migrated = false;
                try {
                    migrate(d);
                } catch (Throwable ignore) {
                }
            } else {
                ROOT = d;
            }
            return ROOT;
        }
    }

    public static File global() {
        File r = root();
        if (r == null) return null;
        File g = new File(r, GLOBAL);
        ensure(g);
        return g;
    }

    public static File account() {
        File r = root();
        if (r == null) return null;
        String uin = "";
        try {
            uin = QQEnv.currentUin();
        } catch (Throwable ignore) {
        }
        if (uin == null) uin = "";
        uin = uin.trim();
        if (uin.length() > 0 && !uin.matches("\\d{4,}")) uin = "";

        if (uin.length() > 0) {
            
            if (!uin.equals(LAST_UIN)) {
                LAST_UIN = uin;
                saveLastUin(r, uin);
            }
        } else {

            
            String last = LAST_UIN;
            if (last == null || last.length() == 0) last = loadLastUin(r);
            if (last != null && last.matches("\\d{4,}")) uin = last;
        }

        
        if (uin.length() == 0) {
            File p = new File(r, "_pending");
            ensure(p);
            return p;
        }
        File a = new File(r, uin);
        ensure(a);

        
        if (!pendingAbsorbed) {
            pendingAbsorbed = true;
            final File fRoot = r;
            final File fAcct = a;
            try {
                Bg.run(new Runnable() {
                    @Override public void run() {

                        try { Thread.sleep(20000L); } catch (Throwable ignore) {
                        }
                        absorbPending(fRoot, fAcct);
                    }
                }, "cat-absorb-pending");
            } catch (Throwable ignore) {
            }
        }
        return a;
    }

    public static File account(String sub) {
        File a = account();
        if (a == null) return null;
        if (sub == null || sub.length() == 0) return a;
        File d = new File(a, sub);
        ensure(d);
        return d;
    }

    public static File global(String sub) {
        File g = global();
        if (g == null) return null;
        if (sub == null || sub.length() == 0) return g;
        File d = new File(g, sub);
        ensure(d);
        return d;
    }

    private static void cleanPending(File root) {
        if (root == null) return;
        File p = new File(root, "_pending");
        if (!p.exists()) return;
        long cutoff = BOOT_MS - 60 * 1000L;

        int n = dropDiag(p, cutoff);
        if (n > 0) HookLog.i("_pending 已清 " + n + " 项诊断残留");
    }

    private static int deleteOlderThan(File dir, long cutoff) {
        int n = 0;
        try {
            File[] fs = dir.listFiles();
            if (fs == null) return 0;
            for (int i = 0; i < fs.length; i++) {
                File f = fs[i];
                if (f.isDirectory()) {
                    n += deleteOlderThan(f, cutoff);
                    try {
                        String[] left = f.list();
                        if (left != null && left.length == 0 && f.delete()) n++;
                    } catch (Throwable ignore) {
                    }
                } else if (f.lastModified() < cutoff) {
                    if (f.delete()) n++;
                }
            }
        } catch (Throwable ignore) {
        }
        return n;
    }

    private static void absorbPending(File root, File acct) {
        if (root == null || acct == null) return;
        File p = new File(root, "_pending");
        if (!p.exists()) return;

        
        
        long cutoff = BOOT_MS - 5000L;
        int n = 0;
        try {
            File g = global("cache");
            n += moveOlder(new File(p, "rkey"), g == null ? null : new File(g, "rkey"), cutoff);
            n += moveOlder(new File(p, "log"), new File(acct, "log"), cutoff);
            n += moveOlder(new File(p, "state"), new File(acct, "state"), cutoff);
            n += moveOlder(new File(p, "ai"), new File(acct, "ai"), cutoff);
            n += moveOlder(new File(p, "image"), new File(acct, "image"), cutoff);
            
            n += moveOlder(new File(p, acct.getName()), new File(acct, "log"), cutoff);
            
            n += dropDiag(p, cutoff);
        } catch (Throwable ignore) {
        }
        if (n > 0) HookLog.i("_pending 回收 " + n + " 项");
    }

    public static void absorbPending() {
        try {
            File r = root();
            if (r == null) return;
            String uin = QQEnv.currentUin();
            if (uin == null || uin.trim().length() == 0) return;
            File acct = new File(r, uin.trim());
            if (!acct.exists()) return;
            absorbPending(r, acct);
        } catch (Throwable ignore) {
        }
    }

    private static int moveOlder(File src, File dst, long cutoff) {
        if (src == null || dst == null || !src.isDirectory()) return 0;
        int n = 0;
        File[] fs = src.listFiles();
        if (fs == null) return 0;
        for (int i = 0; i < fs.length; i++) {
            File s = fs[i];
            File d = new File(dst, s.getName());
            if (s.isDirectory()) {
                if (!d.exists()) d.mkdirs();
                n += moveOlder(s, d, cutoff);
                try {
                    String[] left = s.list();
                    if (left != null && left.length == 0) s.delete();
                } catch (Throwable ignore) {
                }
                continue;
            }
            if (s.lastModified() >= cutoff) continue;   
            try {
                boolean ok;
                if (!d.exists() || s.lastModified() > d.lastModified()) {
                    ok = copy(s, d);
                    if (ok) n++;
                } else {
                    ok = true;      
                }
                
                if (ok) s.delete();
            } catch (Throwable ignore) {
            }
        }
        return n;
    }

    private static int dropDiag(File p, long cutoff) {
        if (p == null) return 0;

        long cut = Math.min(cutoff, BOOT_MS);
        String[] names = {"atdiag.txt", "mixdiag.txt", "mixkernel.txt", "mixkernel.txt.1",
                "tools_probe.txt", "rkey_dump.bin", "parse_diag.txt", "oidb_capture.txt",
                "ai_debug.log", "load_crash_count"};
        int n = 0;
        for (int i = 0; i < names.length; i++) {
            File f = new File(p, names[i]);
            try {
                if (f.isFile() && f.lastModified() < cut && f.delete()) n++;
            } catch (Throwable ignore) {
            }
        }
        return n;
    }

    public static void tidy(String uinHint) {
        try {
            File a = account();
            if (a == null) return;
            File[] fs = a.listFiles();
            if (fs == null) return;
            for (int i = 0; i < fs.length; i++) {
                File f = fs[i];
                if (!f.isFile()) continue;
                String n = f.getName();
                String sub = null;
                if (n.startsWith("ai_history") || n.startsWith("ai_affinity")
                        || n.startsWith("ai_mood") || n.startsWith("ai_usage")
                        || n.startsWith("ai_img_desc")) {
                    sub = "ai";
                } else if (n.startsWith("ai_last_image")) {
                    sub = "image";
                } else if (n.startsWith("ai_debug") || n.equals("log.txt")
                        || n.equals("msg.txt") || n.equals("peers.txt")) {
                    sub = "log";
                } else if (n.equals("cookie_cache.json") || n.equals("group_marks.txt")
                        || n.equals("load_crash_count")) {
                    sub = "state";
                }
                if (sub == null) continue;
                File dir = new File(a, sub);
                ensure(dir);
                File dst = new File(dir, n);
                if (!dst.exists()) {
                    try {
                        if (!f.renameTo(dst)) {
                            
                            if (copy(f, dst)) f.delete();
                        }
                    } catch (Throwable ignore) {
                    }
                }
            }
        } catch (Throwable ignore) {
        }
    }

    public static File privateRoot() {
        try {
            return new File(QQEnv.appContext().getFilesDir(), LEGACY);
        } catch (Throwable t) {
            return null;
        }
    }

    public static String displayPath() {
        File r = root();
        if (r == null) return "（不可用）";
        String p = r.getAbsolutePath();
        if (p.contains("/Android/data/") && !p.contains("/files/" + DIR_NAME)) return "外部数据根";
        if (p.contains("/files/" + DIR_NAME)) return "外部 files";
        return "私有目录";
    }

    

    private static File pickRoot() {
        
        try {
            File ext = QQEnv.appContext().getExternalFilesDir(null);
            if (ext != null) {
                File base = ext.getParentFile();          
                if (base != null) {
                    File d = new File(base, DIR_NAME);
                    if (ensure(d)) return d;
                }
                
                File d2 = new File(ext, DIR_NAME);
                if (ensure(d2)) return d2;
            }
        } catch (Throwable ignore) {
        }
        
        try {
            return privateRoot();
        } catch (Throwable t) {
            return null;
        }
    }

    private static boolean ensure(File d) {
        try {
            if (d == null) return false;
            if (!d.exists() && !d.mkdirs()) return false;
            if (!d.isDirectory()) return false;
            File probe = new File(d, ".probe");
            if (probe.createNewFile()) {
                probe.delete();
                return true;
            }
            return probe.exists();
        } catch (Throwable t) {
            return false;
        }
    }

    private static void migrate(File newRoot) {
        if (migrated) return;
        migrated = true;
        if (newRoot == null) return;

        
        
        File stamp = new File(newRoot, ".migrate_done");
        if (stamp.exists()) return;

        try {
            File acc = account();
            File glo = global();

            try {
                File ext = QQEnv.appContext().getExternalFilesDir(null);
                if (ext != null) {
                    File old = new File(ext, DIR_NAME);
                    if (old.exists() && !old.getAbsolutePath().equals(newRoot.getAbsolutePath())) {
                        copyTree(old, acc, glo);
                    }
                }
            } catch (Throwable ignore) {
            }

            

            

            
            try {
                File old = privateRoot();
                if (old != null && old.exists()
                        && !old.getAbsolutePath().equals(newRoot.getAbsolutePath())) {
                    copyTree(old, acc, glo);
                }
            } catch (Throwable ignore) {
            }
            try {
                tidy(null);
            } catch (Throwable ignore) {
            }
            try {
                cleanPending(newRoot);
            } catch (Throwable ignore) {
            }
            try {
                stamp.createNewFile();
            } catch (Throwable ignore) {
            }
        } catch (Throwable t) {
            HookLog.w("数据目录迁移失败 " + t);
        }
    }

    private static boolean isDiagJunk(String name) {
        return "atdiag.txt".equals(name) || "mixdiag.txt".equals(name)
                || "mixkernel.txt".equals(name) || "mixkernel.txt.1".equals(name)
                || "tools_probe.txt".equals(name) || "rkey_dump.bin".equals(name)
                || "parse_diag.txt".equals(name) || "oidb_capture.txt".equals(name)
                || "ai_debug.log".equals(name) || "load_crash_count".equals(name);
    }

    private static void copyTree(File old, File acc, File glo) {
        File[] fs = old.listFiles();
        if (fs == null) return;
        int n = 0;
        for (int i = 0; i < fs.length; i++) {
            File src = fs[i];
            String nm = src.getName();
            if (isDiagJunk(nm)) continue;   
            if (src.isDirectory()) {
                File dstDir = isGlobalDir(nm) ? glo : acc;
                if (dstDir == null) continue;
                File dst = new File(dstDir, nm);
                if (!dst.exists()) dst.mkdirs();
                n += copyDir(src, dst);
            } else {
                File dstDir = isGlobalFile(nm) ? glo : acc;
                if (dstDir == null) continue;
                File dst = new File(dstDir, nm);
                if (!dst.exists() && copy(src, dst)) n++;
            }
        }
        if (n > 0) HookLog.i("数据目录整理完成：" + n + " 项（" + displayPath() + "）");
    }

    private static boolean isGlobalDir(String name) {
        return "emoji".equals(name) || "scripts".equals(name) || "script".equals(name)
                || "persona".equals(name) || "backup".equals(name);
    }

    private static boolean isGlobalFile(String name) {
        return "ai_providers.json".equals(name) || "ai_persona.txt".equals(name)
                || "pan_prefs.json".equals(name) || "config_backup.json".equals(name);
    }

    private static int copyDir(File srcDir, File dstDir) {
        int n = 0;
        File[] fs = srcDir.listFiles();
        if (fs == null) return 0;
        for (int i = 0; i < fs.length; i++) {
            File s = fs[i];
            File d = new File(dstDir, s.getName());
            if (s.isDirectory()) {
                if (!d.exists()) d.mkdirs();
                n += copyDir(s, d);
            } else if (!d.exists() && copy(s, d)) {
                n++;
            }
        }
        return n;
    }

    private static boolean copy(File src, File dst) {
        FileInputStream in = null;
        FileOutputStream out = null;
        try {
            in = new FileInputStream(src);
            out = new FileOutputStream(dst);
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            return true;
        } catch (Throwable t) {
            return false;
        } finally {
            try { if (in != null) in.close(); } catch (Throwable ignore) {}
            try { if (out != null) out.close(); } catch (Throwable ignore) {}
        }
    }
}
