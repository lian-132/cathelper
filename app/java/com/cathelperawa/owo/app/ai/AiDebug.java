package com.cathelperawa.owo.app.ai;

import com.cathelperawa.owo.app.api.IoApi;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class AiDebug {

    private AiDebug() {}

    private static final String FILE = "ai_debug.log";
    private static final int MAX_LINES = 2000;   
    
    private static final long MAX_BYTES = 512 * 1024;

    private static final SimpleDateFormat FMT =
            new SimpleDateFormat("HH:mm:ss", Locale.US);

    private static volatile boolean enabled = true;

    private static volatile java.io.File CACHED;
    private static volatile long CACHED_AT;
    private static final java.util.concurrent.atomic.AtomicInteger SINCE_TRIM =
            new java.util.concurrent.atomic.AtomicInteger();

    public static void setEnabled(boolean on) { enabled = on; }

    private static java.io.File file() {
        long now = System.currentTimeMillis();
        java.io.File f = CACHED;
        if (f != null && now - CACHED_AT < 60000L) return f;
        try {
            
            java.io.File d = com.cathelperawa.owo.app.core.DataDir.account("log");
            f = d == null ? null : new java.io.File(d, FILE);
        } catch (Throwable t) {
            f = null;
        }
        CACHED = f;
        CACHED_AT = now;
        return f;
    }

    public static void log(String s) {
        if (!enabled || s == null) return;
        try {
            String line = "[" + FMT.format(new Date()) + "] " + s;
            com.cathelperawa.owo.app.core.SecureFile.appendLine(file(), line, MAX_BYTES);

            if ((SINCE_TRIM.incrementAndGet() & 31) == 0) trim();
        } catch (Throwable ignore) {
        }
    }

    private static void trim() {
        try {
            java.io.File f = file();
            if (f == null || !f.isFile() || f.length() <= MAX_BYTES) return;
            java.util.List<String> all = com.cathelperawa.owo.app.core.SecureFile.readLines(f);
            if (all.size() <= MAX_LINES) return;
            int keep = Math.max(MAX_LINES / 2, all.size() / 2);
            StringBuilder sb = new StringBuilder();
            for (int i = all.size() - keep; i < all.size(); i++) {
                sb.append(all.get(i)).append('\n');
            }
            com.cathelperawa.owo.app.core.SecureFile.writeLines(f, sb.toString());
        } catch (Throwable ignore) {
        }
    }

    public static void clear() {
        try {
            com.cathelperawa.owo.app.core.SecureFile.writeLines(file(), "");
        } catch (Throwable ignore) {
        }
    }

    public static String read() {
        try {
            java.util.List<String> ls = com.cathelperawa.owo.app.core.SecureFile.readLines(file());
            StringBuilder sb = new StringBuilder();
            for (String l : ls) sb.append(l).append('\n');
            return sb.toString();
        } catch (Throwable t) {
            return "";
        }
    }
}
