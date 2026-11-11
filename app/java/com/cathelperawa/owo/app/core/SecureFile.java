package com.cathelperawa.owo.app.core;

import android.content.Context;

import com.cathelperawa.owo.app.bridge.QQEnv;
import com.cathelperawa.owo.app.cookie.CookieCipher;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public final class SecureFile {

    private SecureFile() {}

    public static final int MISSING = 0;
    
    public static final int PLAIN = 1;
    
    public static final int SEALED = 2;
    
    public static final int CORRUPT = 3;

    private static Context ctx() {
        try {
            return QQEnv.appContext();
        } catch (Throwable t) {
            return null;
        }
    }

    

    
    public static String readText(File f, int[] status) {
        if (f == null || !f.isFile()) {
            set(status, MISSING);
            return null;
        }
        String raw = slurp(f);
        if (raw == null || raw.length() == 0) {
            set(status, PLAIN);          
            return raw;
        }
        if (raw.startsWith(CookieCipher.PREFIX)) {
            String plain = CookieCipher.decrypt(ctx(),
                    raw.substring(CookieCipher.PREFIX.length()).trim());
            if (plain == null || plain.length() == 0) {
                set(status, CORRUPT);
                HookLog.w("密文解不开（换机/清数据？）: " + f.getName());
                return null;
            }
            set(status, SEALED);
            return plain;
        }
        set(status, PLAIN);
        return raw;
    }

    public static boolean writeLines(File f, String text) {
        if (f == null) return false;
        StringBuilder sb = new StringBuilder();
        String[] ls = (text == null ? "" : text).split("\n");
        for (String one : ls) {
            if (one.length() == 0) continue;
            sb.append(sealLine(one)).append('\n');
        }
        if (!writeRawAtomic(f, sb.toString())) return false;
        return true;
    }

    private static boolean writeRawAtomic(File f, String payload) {
        File tmp = new File(f.getAbsolutePath() + ".tmp");
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(tmp);
            out.write(payload.getBytes("UTF-8"));
            out.flush();
            out.close();
            out = null;
            if (f.exists() && !f.delete()) {
                writeRaw(f, payload);
                tmp.delete();
                return true;
            }
            if (!tmp.renameTo(f)) {
                writeRaw(f, payload);
                tmp.delete();
            }
            return true;
        } catch (Throwable t) {
            HookLog.w("写盘失败 " + (f == null ? "" : f.getName()) + ": " + t);
            try { if (out != null) out.close(); } catch (Throwable ignore) {}
            try { tmp.delete(); } catch (Throwable ignore) {}
            return false;
        }
    }

    public static boolean writeText(File f, String text) {
        if (!ENABLED) {
            
            try {
                File tmp = new File(f.getAbsolutePath() + ".tmp");
                java.io.FileWriter w = new java.io.FileWriter(tmp, false);
                w.write(text == null ? "" : text);
                w.flush();
                w.close();
                if (f.exists()) f.delete();
                return tmp.renameTo(f);
            } catch (Throwable t) {
                return false;
            }
        }
        if (f == null) return false;
        return writeRawAtomic(f, seal(text));
    }

    

    
    public static volatile boolean ENABLED = true;

    public static String sealLine(String plain) {
        if (plain == null) return "";
        if (!ENABLED) return plain;
        String enc = CookieCipher.encrypt(ctx(), plain);
        if (enc == null || enc.length() == 0) return plain;
        return CookieCipher.PREFIX + enc;
    }

    public static String openLine(String line) {
        if (line == null) return null;
        if (!line.startsWith(CookieCipher.PREFIX)) return line;   
        String plain = CookieCipher.decrypt(ctx(),
                line.substring(CookieCipher.PREFIX.length()));
        if (plain == null || plain.length() == 0) return null;
        return plain;
    }

    public static boolean migrateLines(File f) {
        if (f == null || !f.isFile() || f.length() == 0) return false;
        List<String> lines = readLines(f);
        if (lines.isEmpty()) return false;
        boolean hasPlain = false;
        for (String l : lines) {
            if (l != null && l.length() > 0 && !l.startsWith(CookieCipher.PREFIX)) {
                hasPlain = true;
                break;
            }
        }
        if (!hasPlain) return false;
        StringBuilder sb = new StringBuilder();
        int n = 0;
        for (String l : lines) {
            if (l == null) continue;
            sb.append(l.startsWith(CookieCipher.PREFIX) ? l : sealLine(l)).append('\n');
            n++;
        }

        
        if (!writeRawAtomic(f, sb.toString())) return false;
        HookLog.i("隐私迁移：" + f.getName() + " " + n + " 行 → 密文");
        return true;
    }

    public static List<String> readLines(File f) {
        List<String> out = new ArrayList<String>();
        if (f == null || !f.isFile()) return out;
        BufferedReader r = null;
        try {
            r = new BufferedReader(new InputStreamReader(new FileInputStream(f), "UTF-8"));
            String line;
            while ((line = r.readLine()) != null) {
                String plain = openLine(line);
                if (plain == null) continue;      
                out.add(plain);
            }
        } catch (Throwable t) {
            HookLog.w("读行失败 " + (f == null ? "" : f.getName()) + ": " + t);
        } finally {
            if (r != null) try { r.close(); } catch (Throwable ignore) {}
        }
        return out;
    }

    public static void appendLine(File f, String line, long max) {
        if (f == null || line == null) return;
        try {
            maybeTrim(f, max);
            FileOutputStream out = new FileOutputStream(f, true);
            out.write((sealLine(line) + "\n").getBytes("UTF-8"));
            out.flush();
            out.close();
        } catch (Throwable t) {
            HookLog.w("追加失败 " + f.getName() + ": " + t);
        }
    }

    public static int appendLines(File f, List<String> lines, long max) {
        if (f == null || lines == null || lines.isEmpty()) return 0;
        int n = 0;
        try {
            maybeTrim(f, max);
            StringBuilder sb = new StringBuilder(lines.size() * 96);
            for (String one : lines) {
                if (one == null || one.length() == 0) continue;
                sb.append(sealLine(one)).append('\n');
                n++;
            }
            if (n == 0) return 0;
            FileOutputStream out = new FileOutputStream(f, true);
            out.write(sb.toString().getBytes("UTF-8"));
            out.flush();
            out.close();
        } catch (Throwable t) {
            HookLog.w("批量追加失败 " + f.getName() + ": " + t);
        }
        return n;
    }

    private static final java.util.Map<String, long[]> TRIM_STATE =
            new java.util.concurrent.ConcurrentHashMap<String, long[]>();

    private static void maybeTrim(File f, long max) {
        if (f == null || max <= 0) return;
        String k = f.getAbsolutePath();
        long[] st = TRIM_STATE.get(k);
        long now = System.currentTimeMillis();
        if (st == null) {
            st = new long[]{0L, 0L};
            TRIM_STATE.put(k, st);
        }
        boolean due;
        synchronized (st) {
            st[1]++;
            due = (st[1] >= 50) || (now - st[0] > 5000L);
            if (due) {
                st[0] = now;
                st[1] = 0;
            }
        }
        if (due) trimTo(f, max);
    }

    

    
    public static void backupCorrupt(File f) {
        if (f == null || !f.isFile()) return;
        File bak = new File(f.getAbsolutePath() + ".corrupt");
        if (bak.exists()) return;
        try {
            copy(f, bak);
            HookLog.w("已备份解不开的文件 → " + bak.getName());
        } catch (Throwable ignore) {
        }
    }

    public static void trimTo(File f, long max) {
        try {
            if (f == null || !f.exists() || max <= 0 || f.length() <= max) return;
            java.io.RandomAccessFile raf = new java.io.RandomAccessFile(f, "rw");
            long keep = raf.length() / 2;
            byte[] buf = new byte[(int) keep];
            raf.seek(raf.length() - keep);
            raf.readFully(buf);
            raf.setLength(0);
            raf.write(buf);
            raf.close();
        } catch (Throwable ignore) {
        }
    }

    private static String seal(String text) {
        if (text == null) text = "";
        String enc = CookieCipher.encrypt(ctx(), text);
        if (enc == null || enc.length() == 0) return text;      
        return CookieCipher.PREFIX + enc;
    }

    private static String slurp(File f) {
        FileInputStream in = null;
        try {
            in = new FileInputStream(f);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) bos.write(buf, 0, n);
            return new String(bos.toByteArray(), "UTF-8");
        } catch (Throwable t) {
            return null;
        } finally {
            if (in != null) try { in.close(); } catch (Throwable ignore) {}
        }
    }

    private static void writeRaw(File f, String payload) {
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(f);
            out.write(payload.getBytes("UTF-8"));
            out.flush();
        } catch (Throwable ignore) {
        } finally {
            if (out != null) try { out.close(); } catch (Throwable ignore) {}
        }
    }

    private static void copy(File from, File to) throws Throwable {
        FileInputStream in = new FileInputStream(from);
        FileOutputStream out = new FileOutputStream(to);
        try {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            out.flush();
        } finally {
            try { in.close(); } catch (Throwable ignore) {}
            try { out.close(); } catch (Throwable ignore) {}
        }
    }

    private static void set(int[] status, int v) {
        if (status != null && status.length > 0) status[0] = v;
    }
}
