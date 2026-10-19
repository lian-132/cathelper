package com.cathelperawa.owo.app;

import android.content.Context;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MsgStore {

    private static final int MAX = 300;

    private static final List<String> BUFFER = new ArrayList<String>();
    private static File msgFile;

    private static final Map<String, String> PEERS = new LinkedHashMap<String, String>();
    private static File peerFile;

    private static final Set<String> SELECTED = new LinkedHashSet<String>();
    private static boolean scopeAll = true;
    private static File scopeFile;

    private static String curUin = null;

    private static File rootDir;

    public static synchronized void init(Context ctx) {
        try {
            if (rootDir == null) {
                rootDir = new File(ctx.getFilesDir(), "cathelper");
                if (!rootDir.exists()) rootDir.mkdirs();
            }

            String uin = null;
            try {
                uin = com.cathelperawa.owo.app.bridge.QQEnv.currentUin();
            } catch (Throwable ignore) {
            }

            if (uin == null || uin.trim().isEmpty() || "_unknown".equals(uin)) {
                return;
            }

            if (uin.equals(curUin) && msgFile != null) return;
            curUin = uin;

            BUFFER.clear();
            PEERS.clear();
            SELECTED.clear();
            UNDER_ALL_OFF.clear();
            scopeAll = true;

            File dir = new File(rootDir, uin);
            if (!dir.exists()) dir.mkdirs();
            msgFile = new File(dir, "msg.txt");
            peerFile = new File(dir, "peers.txt");
            scopeFile = new File(dir, "scope.txt");
            logFile = new File(dir, "log.txt");

            if (msgFile.exists()) {
                try (BufferedReader r = new BufferedReader(new FileReader(msgFile))) {
                    String line;
                    while ((line = r.readLine()) != null) {
                        if (BUFFER.size() >= MAX) BUFFER.remove(0);
                        BUFFER.add(line);
                    }
                }
            }
            if (peerFile.exists()) {
                try (BufferedReader r = new BufferedReader(new FileReader(peerFile))) {
                    String line;
                    while ((line = r.readLine()) != null) {
                        int i = line.indexOf('|');
                        if (i > 0) PEERS.put(line.substring(0, i), line.substring(i + 1));
                    }
                }
            }
            if (scopeFile.exists()) {
                try (BufferedReader r = new BufferedReader(new FileReader(scopeFile))) {
                    String line;
                    while ((line = r.readLine()) != null) {
                        line = line.trim();
                        if (line.startsWith("all=")) {
                            scopeAll = "1".equals(line.substring(4));
                        } else if (line.startsWith("p=")) {
                            SELECTED.add(line.substring(2));
                        } else if (line.startsWith("x=")) {
                            UNDER_ALL_OFF.add(line.substring(2));
                        }
                    }
                }
            }
        } catch (Throwable ignore) {
        }
    }

    public static synchronized String currentUin() {
        return curUin;
    }

    private static long lastCheckMs = 0L;

    private static void ensureAccount() {
        try {
            long nowMs = System.currentTimeMillis();
            if (nowMs - lastCheckMs < 5000L) return;
            lastCheckMs = nowMs;
            String uin = com.cathelperawa.owo.app.bridge.QQEnv.currentUin();
            if (uin == null || uin.trim().isEmpty() || uin.equals(curUin)) return;

            Context ctx = com.cathelperawa.owo.app.bridge.QQEnv.appContext();
            if (ctx != null) init(ctx);
        } catch (Throwable ignore) {
        }
    }

    public static synchronized boolean syncAccount(Context ctx) {
        try {
            String uin = com.cathelperawa.owo.app.bridge.QQEnv.currentUin();
            if (uin == null || uin.trim().isEmpty()) return false;
            if (uin.equals(curUin)) return false;
            init(ctx);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private static File logFile;

    private static final long LOG_MAX = 256 * 1024;

    public static synchronized void appendLog(String text) {
        ensureAccount();
        if (curUin == null || text == null || text.length() == 0) return;
        try {
            if (logFile == null) return;
            trimIfOversize(logFile, LOG_MAX);
            try (FileWriter w = new FileWriter(logFile, true)) {
                w.write(text);
                w.flush();
            }
        } catch (Throwable ignore) {
        }
    }

    public static synchronized List<String> logs(int maxLines) {
        List<String> out = new ArrayList<String>();
        try {
            if (logFile == null || !logFile.exists()) return out;
            try (BufferedReader r = new BufferedReader(new FileReader(logFile))) {
                String line;
                while ((line = r.readLine()) != null) {
                    if (out.size() >= maxLines) out.remove(0);
                    out.add(line);
                }
            }
        } catch (Throwable ignore) {
        }
        return out;
    }

    public static synchronized void clearLogs() {
        try {
            if (logFile != null && logFile.exists()) logFile.delete();
        } catch (Throwable ignore) {
        }
    }

    public static synchronized void add(String text) {
        ensureAccount();

        if (curUin == null) return;
        String line = now() + "  " + text;
        if (BUFFER.size() >= MAX) BUFFER.remove(0);
        BUFFER.add(line);
        append(msgFile, line);
    }

    public static synchronized List<String> all() {
        return new ArrayList<String>(BUFFER);
    }

    public static synchronized void clear() {
        BUFFER.clear();
        try {
            if (msgFile != null && msgFile.exists()) msgFile.delete();
        } catch (Throwable ignore) {
        }
    }

    public static synchronized void notePeer(String uid, String name, boolean group) {
        ensureAccount();

        if (curUin == null) return;
        if (uid == null || uid.length() == 0 || "null".equals(uid)) return;
        String label = (group ? "[群] " : "[友] ") + (name == null || name.length() == 0 ? uid : name);
        String old = PEERS.get(uid);
        if (label.equals(old)) return;
        PEERS.put(uid, label);

        try {
            if (peerFile != null) {
                StringBuilder sb = new StringBuilder();
                for (Map.Entry<String, String> e : PEERS.entrySet()) {
                    sb.append(e.getKey()).append('|').append(e.getValue()).append('\n');
                }
                FileWriter w = new FileWriter(peerFile, false);
                w.write(sb.toString());
                w.flush();
                w.close();
            }
        } catch (Throwable ignore) {
        }
    }

    public static synchronized Map<String, String> peers() {
        return new LinkedHashMap<String, String>(PEERS);
    }

    public static synchronized boolean isScopeAll() {
        return scopeAll;
    }

    public static synchronized void setScopeAll(boolean v) {
        scopeAll = v;
        if (v) {

            UNDER_ALL_OFF.clear();
        } else {

            SELECTED.clear();
        }
        saveScope();
    }

    public static synchronized boolean isSelected(String uid) {
        return SELECTED.contains(uid);
    }

    public static synchronized void toggleSelected(String uid) {
        if (SELECTED.contains(uid)) SELECTED.remove(uid);
        else SELECTED.add(uid);
        saveScope();
    }

    public static synchronized boolean allows(String uid) {
        if (uid == null) return false;
        if (scopeAll) return !UNDER_ALL_OFF.contains(uid);
        return SELECTED.contains(uid);
    }

    private static final Set<String> UNDER_ALL_OFF = new LinkedHashSet<String>();

    public static synchronized void toggleUnderAll(String uid) {
        if (UNDER_ALL_OFF.contains(uid)) UNDER_ALL_OFF.remove(uid);
        else UNDER_ALL_OFF.add(uid);
        saveScope();
    }

    public static synchronized boolean isUnderAllOff(String uid) {
        return UNDER_ALL_OFF.contains(uid);
    }

    private static void saveScope() {
        try {
            if (scopeFile == null) return;
            StringBuilder sb = new StringBuilder();
            sb.append("all=").append(scopeAll ? "1" : "0").append('\n');
            for (String s : SELECTED) sb.append("p=").append(s).append('\n');

            for (String s : UNDER_ALL_OFF) sb.append("x=").append(s).append('\n');
            FileWriter w = new FileWriter(scopeFile, false);
            w.write(sb.toString());
            w.flush();
            w.close();
        } catch (Throwable ignore) {
        }
    }

    private static final long MSG_MAX = 512 * 1024;

    static void trimIfOversize(File f, long max) {
        try {
            if (f == null || !f.exists() || f.length() <= max) return;
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

    private static void append(File f, String line) {
        try {
            if (f == null) return;

            trimIfOversize(f, MSG_MAX);
            FileWriter w = new FileWriter(f, true);
            w.write(line + "\n");
            w.flush();
            w.close();
        } catch (Throwable ignore) {
        }
    }

    private static String now() {
        return new java.text.SimpleDateFormat("HH:mm:ss",
                java.util.Locale.getDefault()).format(new java.util.Date());
    }
}
