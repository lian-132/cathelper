package com.cathelperawa.owo.app.script;

import com.cathelperawa.owo.app.core.HookLog;

import java.util.ArrayList;
import java.util.List;

public final class ScriptTimer {

    public interface Handler {
        void onTick(String id);
    }

    private static final class Task {
        final String id;
        final String script;
        final long intervalMs;
        final int atMinuteOfDay;
        final Handler handler;
        long nextAt;
        int runs;
        int errors;

        Task(String id, String script, long intervalMs, int atMinuteOfDay, Handler h) {
            this.id = id;
            this.script = script;
            this.intervalMs = intervalMs;
            this.atMinuteOfDay = atMinuteOfDay;
            this.handler = h;
        }
    }

    private static final List<Task> TASKS = new ArrayList<Task>();
    private static Thread worker;
    private static int seq = 0;

    private ScriptTimer() {}

    public static String every(String script, int seconds, Handler h) {
        if (h == null || seconds < 5) return null;
        long ms = seconds * 1000L;
        String id = "t" + (++seq) + "@" + script;
        synchronized (TASKS) {
            Task t = new Task(id, script, ms, -1, h);
            t.nextAt = System.currentTimeMillis() + ms;
            TASKS.add(t);
        }
        ensureWorker();
        HookLog.i("[定时] " + script + " 每 " + seconds + " 秒（" + id + "）");
        return id;
    }

    public static String at(String script, String hhmm, Handler h) {
        if (h == null || hhmm == null) return null;
        int minute = parseHhmm(hhmm);
        if (minute < 0) {
            HookLog.w("[定时] 时间格式要 HH:mm：" + hhmm);
            return null;
        }
        String id = "a" + (++seq) + "@" + script;
        synchronized (TASKS) {
            Task t = new Task(id, script, -1, minute, h);
            t.nextAt = nextOccurrence(minute);
            TASKS.add(t);
        }
        ensureWorker();
        HookLog.i("[定时] " + script + " 每天 " + hhmm + "（" + id + "）");
        return id;
    }

    public static boolean cancel(String id) {
        if (id == null) return false;
        synchronized (TASKS) {
            for (int i = 0; i < TASKS.size(); i++) {
                if (TASKS.get(i).id.equals(id)) {
                    TASKS.remove(i);
                    return true;
                }
            }
        }
        return false;
    }

    public static int clearScript(String script) {
        if (script == null) return 0;
        synchronized (TASKS) {
            int n = 0;
            for (int i = TASKS.size() - 1; i >= 0; i--) {
                if (script.equals(TASKS.get(i).script)) {
                    TASKS.remove(i);
                    n++;
                }
            }
            return n;
        }
    }

    public static int count() {
        synchronized (TASKS) {
            return TASKS.size();
        }
    }

    public static List<String> list() {
        List<String> out = new ArrayList<String>();
        synchronized (TASKS) {
            for (int i = 0; i < TASKS.size(); i++) {
                Task t = TASKS.get(i);
                out.add(t.id + (t.intervalMs > 0
                        ? "（每 " + (t.intervalMs / 1000) + "s）"
                        : "（每天 " + fmt(t.atMinuteOfDay) + "）"));
            }
        }
        return out;
    }

    private static void ensureWorker() {
        if (worker != null) return;
        synchronized (ScriptTimer.class) {
            if (worker != null) return;
            Thread t = new Thread(new Runnable() {
                @Override public void run() {
                    while (true) {
                        try {
                            Thread.sleep(1000);
                            tick();
                        } catch (Throwable ignore) {
                        }
                    }
                }
            }, "cat-script-timer");
            t.setDaemon(true);
            t.setPriority(Thread.MIN_PRIORITY);
            t.start();
            worker = t;
            HookLog.i("[定时] 调度线程已启动");
        }
    }

    private static void tick() {
        List<Task> due = new ArrayList<Task>();
        long now = System.currentTimeMillis();
        synchronized (TASKS) {
            for (int i = 0; i < TASKS.size(); i++) {
                Task t = TASKS.get(i);
                if (t.nextAt <= now) due.add(t);
            }
        }
        for (int i = 0; i < due.size(); i++) {
            final Task t = due.get(i);

            synchronized (TASKS) {
                if (!TASKS.contains(t)) continue;
                if (t.intervalMs > 0) {
                    t.nextAt = now + t.intervalMs;
                } else {
                    t.nextAt = nextOccurrence(t.atMinuteOfDay);
                }
            }
            try {

                ScriptExec.run(t.script, new Runnable() {
                    @Override public void run() {
                        try {
                            t.handler.onTick(t.id);
                        } catch (Throwable e) {
                            ModHealth.record(t.script, "定时 " + t.id + "：" + e);
                            HookLog.e("[定时] " + t.id + " 出错", e);
                        }
                    }
                });
                t.runs++;
            } catch (Throwable e) {
                t.errors++;
                ModHealth.record(t.script, "定时 " + t.id + "：" + e);
                HookLog.e("[定时] " + t.id + " 出错", e);
                if (t.errors >= 5) {
                    cancel(t.id);
                    HookLog.w("[定时] " + t.id + " 连续出错 5 次，已自动取消");
                }
            }
        }
    }

    private static long nextOccurrence(int minuteOfDay) {
        java.util.Calendar c = java.util.Calendar.getInstance();
        c.set(java.util.Calendar.HOUR_OF_DAY, minuteOfDay / 60);
        c.set(java.util.Calendar.MINUTE, minuteOfDay % 60);
        c.set(java.util.Calendar.SECOND, 0);
        c.set(java.util.Calendar.MILLISECOND, 0);
        long at = c.getTimeInMillis();
        if (at <= System.currentTimeMillis()) at += 24 * 3600 * 1000L;
        return at;
    }

    private static int parseHhmm(String s) {
        try {
            String[] p = s.trim().split(":");
            if (p.length != 2) return -1;
            int h = Integer.parseInt(p[0].trim());
            int m = Integer.parseInt(p[1].trim());
            if (h < 0 || h > 23 || m < 0 || m > 59) return -1;
            return h * 60 + m;
        } catch (Throwable t) {
            return -1;
        }
    }

    private static String fmt(int minuteOfDay) {
        return String.format("%02d:%02d", minuteOfDay / 60, minuteOfDay % 60);
    }
}
