package com.cathelperawa.owo.app.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RuntimeErrors {

    public static final int MAX_KINDS = 64;

    public static final int MAX_SNAPSHOTS = 3;

    public static final class Entry {
        public final String feature;    
        public final String site;       
        public final String type;       
        public final String message;    
        public volatile int count;      
        public final long firstAt;      
        public volatile long lastAt;    
        private String stack;           

        private final String[] snapshots = new String[MAX_SNAPSHOTS];
        
        private final String[] shapes = new String[MAX_SNAPSHOTS];
        private int snapCount = 0;

        Entry(String feature, String site, Throwable t) {
            this.feature = feature;
            this.site = site;
            this.type = t.getClass().getSimpleName();
            String m = t.getMessage();
            if (m == null) m = "";
            this.message = m.length() > 300 ? m.substring(0, 300) + "…" : m;
            this.count = 1;
            this.firstAt = System.currentTimeMillis();
            this.lastAt = this.firstAt;
            this.stack = stackOf(t);
        }

        void bump() {
            count++;
            lastAt = System.currentTimeMillis();
        }

        void snapshot(Throwable t) {
            if (t == null) return;
            String shape = shapeOf(t);
            if (shape.isEmpty()) return;
            int n = Math.min(snapCount, MAX_SNAPSHOTS);
            for (int i = 0; i < n; i++) {
                if (shape.equals(shapes[i])) return;   
            }
            String s = stackOf(t);
            int idx = snapCount % MAX_SNAPSHOTS;
            snapshots[idx] = s;
            shapes[idx] = shape;
            snapCount++;
        }

        public java.util.List<String> recentStacks() {
            java.util.ArrayList<String> out = new java.util.ArrayList<String>();
            int n = Math.min(snapCount, MAX_SNAPSHOTS);
            for (int i = 1; i <= n; i++) {
                String s = snapshots[((snapCount - i) % MAX_SNAPSHOTS + MAX_SNAPSHOTS) % MAX_SNAPSHOTS];
                if (s != null) out.add(s);
            }
            return out;
        }

        
        public String stack() {
            return stack;
        }

        public String summary() {
            return type + (message.isEmpty() ? "" : ": " + message);
        }

        public String line() {
            StringBuilder sb = new StringBuilder();
            if (feature != null && !feature.isEmpty()) {
                sb.append('[').append(feature).append("] ");
            }
            if (site != null && !site.isEmpty()) {
                sb.append(site).append(" → ");
            }
            sb.append(summary());
            if (count > 1) sb.append("  ×").append(count);
            return sb.toString();
        }
    }

    private static final Map<String, Entry> ERRORS =
            Collections.synchronizedMap(new LinkedHashMap<String, Entry>());

    private static final java.util.concurrent.atomic.AtomicInteger TOTAL =
            new java.util.concurrent.atomic.AtomicInteger(0);

    
    
    private static final long FLUSH_INTERVAL_MS = 5000L;

    private RuntimeErrors() {}

    

    
    public static void record(String feature, String site, Throwable t) {
        if (t == null) return;
        TOTAL.incrementAndGet();
        String key = (feature == null ? "" : feature) + '|'
                + (site == null ? "" : site) + '|' + t.getClass().getName();
        synchronized (ERRORS) {
            Entry e = ERRORS.get(key);
            if (e != null) {
                e.bump();
                e.snapshot(t);      
                return;
            }
            if (ERRORS.size() >= MAX_KINDS) {
                
                Entry other = ERRORS.get("|OTHER|");
                if (other != null) {
                    other.bump();
                } else {
                    ERRORS.put("|OTHER|", new Entry(null, "OTHER",
                            new IllegalStateException("错误种类超过上限，后续合并计数")));
                }
                return;
            }
            Entry ne = new Entry(feature, site, t);
            ne.snapshot(t);   
            ERRORS.put(key, ne);
            maybeAutoFlush(key);
        }
    }

    public static void record(Throwable t) {
        record(null, null, t);
    }

    

    
    public static List<Entry> all() {
        synchronized (ERRORS) {
            return new ArrayList<Entry>(ERRORS.values());
        }
    }

    
    public static int flushFailures() {
        return FLUSH_FAILS.get();
    }

    public static int totalCount() {
        return TOTAL.get();
    }

    public static int kindCount() {
        synchronized (ERRORS) {
            return ERRORS.size();
        }
    }

    public static List<Entry> forFeature(String feature) {
        List<Entry> out = new ArrayList<Entry>();
        if (feature == null) return out;
        synchronized (ERRORS) {
            for (Entry e : ERRORS.values()) {
                if (feature.equals(e.feature)) out.add(e);
            }
        }
        return out;
    }

    public static void clear() {
        synchronized (ERRORS) {
            ERRORS.clear();
        }
        TOTAL.set(0);
    }

    

    

    

    
    private static final class FlushState {
        final java.io.File dir;
        
        final boolean immediate;
        volatile long at;
        volatile int total;
        
        final java.util.Set<String> flushed = java.util.Collections.synchronizedSet(
                new java.util.HashSet<String>());
        FlushState(java.io.File dir, boolean immediate) {
            this.dir = dir;
            this.immediate = immediate;
            this.at = 0L;
            this.total = -1;      
        }
    }

    private static volatile FlushState flushState = null;
    
    private static final java.util.concurrent.atomic.AtomicInteger FLUSH_FAILS =
            new java.util.concurrent.atomic.AtomicInteger(0);

    public static void autoFlushTo(java.io.File dir) {
        autoFlushTo(dir, false);
    }

    public static void autoFlushTo(java.io.File dir, boolean immediate) {
        flushState = (dir == null ? null : new FlushState(dir, immediate));
    }

    private static void maybeAutoFlush() {
        maybeAutoFlush(null);
    }

    private static void maybeAutoFlush(String key) {
        FlushState st = flushState;      
        if (st == null) return;
        if (st.dir == null) return;
        long now = System.currentTimeMillis();

        if (st.immediate) {
            if (key != null && st.flushed.add(key)) {
                st.at = now;
                st.total = TOTAL.get();
                if (dumpToDir(st.dir) == null) FLUSH_FAILS.incrementAndGet();
                return;
            }
        }
        if (now - st.at < FLUSH_INTERVAL_MS) return;
        int totalNow = TOTAL.get();
        if (totalNow == st.total) return;   

        st.at = now;
        st.total = totalNow;
        if (dumpToDir(st.dir) == null) FLUSH_FAILS.incrementAndGet();
    }

    public static java.io.File flushNow() {
        FlushState st = flushState;
        if (st == null || st.dir == null) return null;
        int totalNow = TOTAL.get();
        st.at = System.currentTimeMillis();
        st.total = totalNow;
        java.io.File out = dumpToDir(st.dir);
        if (out == null) FLUSH_FAILS.incrementAndGet();
        return out;
    }

    

    
    public static final long MAX_LOG_BYTES = 2L * 1024 * 1024;

    public static final int LOG_KEEP_DAYS = 7;

    private static final String ROTATE_PREFIX = "errors_";
    private static final String LOG_NAME = "errors.log";

    public static java.io.File dumpToDir(java.io.File dir) {
        if (dir == null) return null;
        java.io.File out = null;
        try {
            if (!dir.exists()) dir.mkdirs();
            if (!dir.isDirectory() || !dir.canWrite()) return null;

            java.io.File f = new java.io.File(dir, LOG_NAME);
            if (f.exists() && f.length() > MAX_LOG_BYTES) {
                rotate(f);
            }
            out = f;
            java.io.FileOutputStream fos = new java.io.FileOutputStream(f, true);
            try {
                byte[] bytes = dump().getBytes("UTF-8");
                fos.write(bytes);
                fos.flush();
                fos.getFD().sync();     
            } finally {
                fos.close();
            }
            cleanOld(dir);
        } catch (Throwable ignore) {
            
        }
        return out;
    }

    private static void rotate(java.io.File f) {
        try {
            java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
            java.io.File bak = new java.io.File(f.getParentFile(),
                    ROTATE_PREFIX + fmt.format(new java.util.Date()) + ".log");
            if (bak.exists()) bak.delete();
            if (!f.renameTo(bak)) f.delete();   
        } catch (Throwable ignore) {
            try { f.delete(); } catch (Throwable ignore2) { }
        }
    }

    private static void cleanOld(java.io.File dir) {
        try {
            java.io.File[] files = dir.listFiles();
            if (files == null) return;
            long deadline = System.currentTimeMillis() - LOG_KEEP_DAYS * 24L * 60 * 60 * 1000;
            for (java.io.File f : files) {
                if (f == null || !f.isFile()) continue;
                String name = f.getName();
                if (!name.startsWith(ROTATE_PREFIX)) continue;
                if (f.lastModified() < deadline) f.delete();
            }
        } catch (Throwable ignore) {
        }
    }

    public static String dump() {
        StringBuilder sb = new StringBuilder();
        sb.append("══ 运行期错误 ══\n");
        sb.append("共 ").append(TOTAL.get()).append(" 次 / ").append(kindCount()).append(" 类\n");
        if (TOTAL.get() == 0) {
            sb.append("（无）\n");
            return sb.toString();
        }
        int i = 0;
        for (Entry e : all()) {
            i++;
            sb.append('\n').append('#').append(i).append(' ').append(e.line()).append('\n');
            if (e.stack != null && !e.stack.isEmpty()) {
                sb.append("[首次] ").append(e.stack).append('\n');
            }
            
            List<String> recent = e.recentStacks();
            for (int k = 0; k < recent.size(); k++) {
                sb.append("[最近").append(k + 1).append("] ")
                        .append(recent.get(k)).append('\n');
            }
        }
        return sb.toString();
    }

    

    private static String stackOf(Throwable t) {
        try {
            java.io.StringWriter sw = new java.io.StringWriter();
            java.io.PrintWriter pw = new java.io.PrintWriter(sw);
            t.printStackTrace(pw);
            pw.flush();
            String s = sw.toString();
            return s.length() > 4000 ? s.substring(0, 4000) + "…(truncated)" : s;
        } catch (Throwable ignore) {
            return "";
        }
    }

    static String shapeOf(Throwable t) {
        try {
            java.lang.StringBuilder sb = new java.lang.StringBuilder(256);
            StackTraceElement[] st = t.getStackTrace();
            int n = Math.min(st == null ? 0 : st.length, 40);
            for (int i = 0; i < n; i++) {
                sb.append(st[i].getClassName()).append('.')
                        .append(st[i].getMethodName()).append('\n');
            }
            return sb.toString();
        } catch (Throwable ignore) {
            return "";
        }
    }
}
