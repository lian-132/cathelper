package com.cathelperawa.owo.app.core;

import java.util.ArrayList;
import java.util.List;

public final class BootTiming {

    private static final int MAX = 16;

    private static final long T0 = System.nanoTime();

    private static final List<String> MARKS = new ArrayList<String>();
    private static final List<Long> MS = new ArrayList<Long>();

    private BootTiming() {}

    public static void mark(String what) {
        try {
            if (what == null) return;
            if (MARKS.size() >= MAX) return;
            if (MARKS.contains(what)) return;
            MARKS.add(what);
            MS.add((System.nanoTime() - T0) / 1000000L);
        } catch (Throwable ignore) {
        }
    }

    public static long totalMs() {
        try {
            return (System.nanoTime() - T0) / 1000000L;
        } catch (Throwable t) {
            return 0;
        }
    }

    public static String summary() {
        try {
            if (MARKS.isEmpty()) return "";
            StringBuilder sb = new StringBuilder(128);

            long bootMs = MS.isEmpty() ? totalMs() : MS.get(MS.size() - 1);
            sb.append("总 ").append(bootMs).append("ms");
            for (int i = 0; i < MARKS.size(); i++) {
                sb.append(" | ").append(MARKS.get(i)).append(' ')
                        .append(MS.get(i)).append("ms");
            }
            return sb.toString();
        } catch (Throwable t) {
            return "";
        }
    }

    public static long delta(String from, String to) {
        try {
            int a = MARKS.indexOf(from);
            int b = MARKS.indexOf(to);
            if (a < 0 || b < 0) return -1;
            return MS.get(b) - MS.get(a);
        } catch (Throwable t) {
            return -1;
        }
    }
}
