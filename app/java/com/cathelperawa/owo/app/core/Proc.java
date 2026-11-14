package com.cathelperawa.owo.app.core;

import java.io.FileInputStream;

public final class Proc {

    private Proc() {}

    private static volatile String cached;
    private static volatile boolean looked;

    public static String name() {
        if (looked) return cached;
        looked = true;
        String n = null;

        try {
            FileInputStream in = new FileInputStream("/proc/self/cmdline");
            try {
                byte[] buf = new byte[256];
                int len = in.read(buf);
                if (len > 0) {
                    int end = 0;
                    while (end < len && buf[end] != 0) end++;
                    if (end > 0) n = new String(buf, 0, end, "UTF-8");
                }
            } finally {
                try { in.close(); } catch (Throwable ignore) {}
            }
        } catch (Throwable ignore) {
        }

        if (n == null) {
            try {
                Class<?> at = Class.forName("android.app.ActivityThread");
                Object cur = at.getMethod("currentActivityThread").invoke(null);
                if (cur != null) {
                    Object pn = at.getMethod("getProcessName").invoke(cur);
                    if (pn instanceof String) n = (String) pn;
                }
            } catch (Throwable ignore) {
            }
        }
        cached = n;
        return n;
    }

    public static boolean isSlave() {
        String n = name();
        return n != null && n.indexOf(':') >= 0;
    }

    public static String label() {
        String n = name();
        if (n == null) return "未知进程";
        return isSlave() ? ("子进程 " + n) : ("主进程 " + n);
    }
}
