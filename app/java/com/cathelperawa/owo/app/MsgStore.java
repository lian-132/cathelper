package com.cathelperawa.owo.app;

import com.cathelperawa.owo.app.core.DataDir;

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
    private static volatile File msgFile;

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
                rootDir = DataDir.account();
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

            if (curUin != null) flushNow();
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
                for (String line : com.cathelperawa.owo.app.core.SecureFile.readLines(msgFile)) {
                    if (BUFFER.size() >= MAX) BUFFER.remove(0);
                    BUFFER.add(line);
                }
                com.cathelperawa.owo.app.core.SecureFile.migrateLines(msgFile);
            }
            if (peerFile.exists()) {
                for (String line : com.cathelperawa.owo.app.core.SecureFile.readLines(peerFile)) {
                    int i = line.indexOf('|');
                    if (i > 0) PEERS.put(line.substring(0, i), line.substring(i + 1));
                }
                com.cathelperawa.owo.app.core.SecureFile.migrateLines(peerFile);
            }
            if (scopeFile.exists()) {
                for (String line : com.cathelperawa.owo.app.core.SecureFile.readLines(scopeFile)) {
                    line = line.trim();
                    if (line.startsWith("all=")) {
                        scopeAll = "1".equals(line.substring(4));
                    } else if (line.startsWith("p=")) {
                        SELECTED.add(line.substring(2));
                    } else if (line.startsWith("x=")) {
                        UNDER_ALL_OFF.add(line.substring(2));
                    }
                }
                com.cathelperawa.owo.app.core.SecureFile.migrateLines(scopeFile);
            }
            if (logFile != null && logFile.exists()) {
                com.cathelperawa.owo.app.core.SecureFile.migrateLines(logFile);
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

            java.util.List<String> batch = new java.util.ArrayList<String>();
            String[] lines = text.split("\n");
            for (String one : lines) {
                if (one.length() == 0) continue;
                batch.add(one);
            }
            com.cathelperawa.owo.app.core.SecureFile.appendLines(logFile, batch, LOG_MAX);
        } catch (Throwable ignore) {
        }
    }

    public static List<String> logs(int maxLines) {
        List<String> cached = logsIfReady(maxLines);
        if (cached != null) return cached;
        synchronized (LOG_LOCK) {
            List<String> again = logsIfReady(maxLines);
            if (again != null) return again;
            List<String> out = new ArrayList<String>();
            try {
                File f = logFile;
                if (f != null && f.exists()) {
                    for (String line : com.cathelperawa.owo.app.core.SecureFile.readLines(f)) {
                        if (out.size() >= maxLines) out.remove(0);
                        out.add(line);
                    }
                }
            } catch (Throwable ignore) {
            }
            LOG_CACHE = out;
            LOG_CACHE_AT = System.currentTimeMillis();
            LOG_CACHE_MAX = maxLines;
            return out;
        }
    }

    private static final Object LOG_LOCK = new Object();
    private static volatile List<String> LOG_CACHE;
    private static volatile long LOG_CACHE_AT;
    private static volatile int LOG_CACHE_MAX;
    private static volatile boolean LOG_LOADING;

    public static List<String> logsIfReady(int maxLines) {
        List<String> c = LOG_CACHE;
        if (c == null) return null;
        if (System.currentTimeMillis() - LOG_CACHE_AT > 2000L) return null;
        if (LOG_CACHE_MAX < maxLines) return null;
        return c;
    }

    public static void preloadLogs(final int maxLines, final Runnable onDone) {
        if (LOG_LOADING) return;
        LOG_LOADING = true;
        com.cathelperawa.owo.app.core.Bg.run(new Runnable() {
            @Override public void run() {
                try {
                    logs(maxLines);
                } catch (Throwable ignore) {
                }
                LOG_LOADING = false;
                if (onDone != null) {
                    try { onDone.run(); } catch (Throwable ignore) {}
                }
            }
        });
    }

    public static synchronized void clearLogs() {
        try {
            File f = logFile;
            if (f != null && f.exists()) f.delete();
        } catch (Throwable ignore) {
        }
        LOG_CACHE = null;
        LOG_CACHE_AT = 0L;
    }

    public static synchronized void add(String text) {
        ensureAccount();

        if (curUin == null) return;
        String line = now() + "  " + text;
        if (BUFFER.size() >= MAX) BUFFER.remove(0);
        BUFFER.add(line);

        scheduleWrite(line);
    }

    private static final java.util.ArrayList<String> PENDING = new java.util.ArrayList<String>();
    private static final Object WRITE_LOCK = new Object();
    private static Thread writerThread;

    private static void scheduleWrite(String line) {
        synchronized (WRITE_LOCK) {
            if (PENDING.size() >= 5000) PENDING.remove(0);
            PENDING.add(line);
            if (writerThread == null || !writerThread.isAlive()) {
                writerThread = new Thread(new Runnable() {
                    @Override public void run() { writerLoop(); }
                }, "cat-msgstore-writer");
                writerThread.setDaemon(true);
                writerThread.start();
            }
            WRITE_LOCK.notifyAll();
        }
    }

    private static void writerLoop() {
        while (true) {
            java.util.List<String> batch;
            synchronized (WRITE_LOCK) {
                if (PENDING.isEmpty()) {
                    try {
                        WRITE_LOCK.wait(1000L);
                    } catch (InterruptedException ignore) {
                    }
                    if (PENDING.isEmpty()) continue;
                }
                batch = new java.util.ArrayList<String>(PENDING);
                PENDING.clear();
            }
            try {
                File target = msgFile;
                if (target != null) {
                    com.cathelperawa.owo.app.core.SecureFile.appendLines(target, batch, MSG_MAX);
                }
            } catch (Throwable ignore) {
            }

            try {
                Thread.sleep(400L);
            } catch (InterruptedException ignore) {
            }
        }
    }

    public static void flushNow() {
        java.util.List<String> batch;
        synchronized (WRITE_LOCK) {
            if (PENDING.isEmpty()) return;
            batch = new java.util.ArrayList<String>(PENDING);
            PENDING.clear();
        }
        try {
            File target = msgFile;
            if (target != null) {
                com.cathelperawa.owo.app.core.SecureFile.appendLines(target, batch, MSG_MAX);
            }
        } catch (Throwable ignore) {
        }
    }

    public static synchronized List<String> all() {
        return new ArrayList<String>(BUFFER);
    }

    public static synchronized void clear() {
        BUFFER.clear();
        synchronized (WRITE_LOCK) {
            PENDING.clear();
        }
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
                com.cathelperawa.owo.app.core.SecureFile.writeLines(peerFile, sb.toString());
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
            com.cathelperawa.owo.app.core.SecureFile.writeLines(scopeFile, sb.toString());
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

        com.cathelperawa.owo.app.core.SecureFile.appendLine(f, line, MSG_MAX);
    }

    private static String now() {
        return new java.text.SimpleDateFormat("HH:mm:ss",
                java.util.Locale.getDefault()).format(new java.util.Date());
    }
}
