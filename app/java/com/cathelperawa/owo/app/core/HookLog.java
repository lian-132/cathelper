package com.cathelperawa.owo.app.core;

import android.content.Context;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class HookLog {

    private static final String TAG = "CatHelper";
    private static final int MAX_BUFFER = 300;

    private static final Map<String, String> BUFFER =
            Collections.synchronizedMap(new LinkedHashMap<String, String>() {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
                    return size() > MAX_BUFFER;
                }
            });

    private static final Map<String, Long> LAST =
            new java.util.concurrent.ConcurrentHashMap<String, Long>();

    private static final ThreadLocal<SimpleDateFormat> FMT =
            new ThreadLocal<SimpleDateFormat>() {
                @Override protected SimpleDateFormat initialValue() {
                    return new SimpleDateFormat("HH:mm:ss", Locale.US);
                }
            };

    

    private static final Object GATE = new Object();
    private static long gateWindow = 0L;
    private static int gateCount = 0;
    private static final int GATE_PER_SEC = 30;
    private static final Map<String, Long> GATE_DUP =
            new java.util.concurrent.ConcurrentHashMap<String, Long>();

    private static boolean gateAllow(String level, String msg) {
        if ("W".equals(level) || "E".equals(level)) return true;
        long now = System.currentTimeMillis();
        Long dup = GATE_DUP.get(msg);
        if (dup != null && now - dup < 300L) return false;
        GATE_DUP.put(msg, now);
        if (GATE_DUP.size() > 500) GATE_DUP.clear();
        synchronized (GATE) {
            if (now - gateWindow >= 1000L) {
                gateWindow = now;
                gateCount = 0;
            }
            if (gateCount >= GATE_PER_SEC) return false;
            gateCount++;
            return true;
        }
    }

    private HookLog() {}

    public static void d(String msg) {
        record("D", msg);
    }

    public static void i(String msg) {
        record("I", msg);
    }

    public static void w(String msg) {
        record("W", msg);
    }

    public static void e(String msg, Throwable t) {
        String fingerprint = msg + " | " + firstFrame(t);
        long now = System.currentTimeMillis();
        Long last = LAST.get(fingerprint);
        if (last != null && now - last < 1000) return;   
        LAST.put(fingerprint, now);
        record("E", msg + (t == null ? "" : " :: " + t));
    }

    private static void record(String level, String msg) {
        String line = FMT.get().format(new java.util.Date()) + " [" + level + "] " + msg;

        boolean allow = gateAllow(level, msg);
        if (allow) {
            try {
                Log.i(TAG, line);
            } catch (Throwable ignore) {
            }
            
            try {
                de.robv.android.xposed.XposedBridge.log(TAG + " " + line);
            } catch (Throwable ignore) {
            }
            
            BUFFER.put(System.nanoTime() + "-" + msg.hashCode(), line);
        }

        
        
        if (!allow) return;   
        try {

            com.cathelperawa.owo.app.util.CatLog.enqueue(
                    level.equals("D") ? com.cathelperawa.owo.app.util.CatLog.DEBUG
                    : level.equals("W") ? com.cathelperawa.owo.app.util.CatLog.WARN
                    : level.equals("E") ? com.cathelperawa.owo.app.util.CatLog.ERROR
                    : com.cathelperawa.owo.app.util.CatLog.INFO,
                    msg, null);
        } catch (Throwable ignore) {
        }
    }

    private static String firstFrame(Throwable t) {
        if (t == null) return "";
        StackTraceElement[] st = t.getStackTrace();
        if (st == null || st.length == 0) return t.getClass().getSimpleName();
        for (StackTraceElement e : st) {
            String cn = e.getClassName();
            if (cn != null && cn.startsWith("com.cathelperawa.owo")) {
                return cn + "#" + e.getMethodName();
            }
        }
        return st[0].toString();
    }

    

    public static java.util.List<String> snapshot() {
        synchronized (BUFFER) {
            return new java.util.ArrayList<String>(BUFFER.values());
        }
    }

    public static void clear() {
        BUFFER.clear();
    }
}
