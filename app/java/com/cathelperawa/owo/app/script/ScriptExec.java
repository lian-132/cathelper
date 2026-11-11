package com.cathelperawa.owo.app.script;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public final class ScriptExec {

    private ScriptExec() {}

    public static final long TIMEOUT_MS = 3000;
    
    public static final int STUCK_HITS = 2;
    
    public static final int QUEUE_CAP = 256;
    
    private static final int DOG_PERIOD = 2;

    private static final Map<String, Worker> WORKERS = new HashMap<String, Worker>();
    private static final Object MAP = new Object();
    private static volatile ScheduledExecutorService DOG;

    public static final class Stat {
        public long calls;
        public long totalMs;
        public long maxMs;
        public long errors;
        public long timeouts;
        public long dropped;
        public String lastError;
    }

    public static final class Worker implements Runnable {
        final String name;
        final ThreadPoolExecutor pool;
        final Stat stat = new Stat();

        volatile long startAt;
        volatile boolean running;
        volatile Thread owner;     
        volatile int hits;          
        volatile boolean stuck;     

        Worker(String name) {
            this.name = name;
            final String full = "cat-script-" + name;
            ThreadFactory tf = new ThreadFactory() {
                @Override public Thread newThread(Runnable r) {
                    Thread t = new Thread(r, full.length() > 26 ? full.substring(0, 26) : full);
                    t.setDaemon(true);
                    return t;
                }
            };
            this.pool = new ThreadPoolExecutor(1, 1, 30, TimeUnit.SECONDS,
                    new ArrayBlockingQueue<Runnable>(QUEUE_CAP), tf,
                    new ThreadPoolExecutor.DiscardPolicy());
        }

        @Override public void run() { }
    }

    private static Worker worker(String script) {
        String key = (script == null || script.length() == 0) ? "<anon>" : script;
        synchronized (MAP) {
            Worker w = WORKERS.get(key);
            if (w == null) {
                w = new Worker(key);
                WORKERS.put(key, w);
                ensureDog();
            }
            return w;
        }
    }

    private static void ensureDog() {
        if (DOG != null) return;
        synchronized (ScriptExec.class) {
            if (DOG != null) return;
            try {
                ScheduledExecutorService d = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
                    @Override public Thread newThread(Runnable r) {
                        Thread t = new Thread(r, "cat-script-dog");
                        t.setDaemon(true);
                        return t;
                    }
                });
                d.scheduleWithFixedDelay(new Runnable() {
                    @Override public void run() { scan(); }
                }, DOG_PERIOD, DOG_PERIOD, TimeUnit.SECONDS);
                DOG = d;
            } catch (Throwable ignore) {
            }
        }
    }

    private static void scan() {
        long now = System.currentTimeMillis();
        java.util.List<Worker> all;
        synchronized (MAP) {
            all = new java.util.ArrayList<Worker>(WORKERS.values());
        }
        for (int i = 0; i < all.size(); i++) {
            Worker w = all.get(i);
            if (!w.running) continue;
            long cost = now - w.startAt;
            if (cost < TIMEOUT_MS) continue;
            w.stat.timeouts++;
            w.hits++;
            if (w.hits >= STUCK_HITS && !w.stuck) {
                w.stuck = true;
                com.cathelperawa.owo.app.core.HookLog.w("[脚本] " + w.name
                        + " 连续 " + w.hits + " 次超过 " + TIMEOUT_MS + "ms，判定疑似卡死，已停止派发事件"
                        + "（点「重载」或「停用」可恢复）");
                try {
                    ModHealth.record(w.name, "回调超过 " + TIMEOUT_MS + "ms，判定卡死");
                } catch (Throwable ignore) {
                }
            } else {
                com.cathelperawa.owo.app.core.HookLog.w("[脚本] " + w.name
                        + " 本次回调已跑 " + cost + "ms（阈值 " + TIMEOUT_MS + "ms）");
            }
        }
    }

    public static void run(String script, Runnable task) {
        if (task == null) return;
        final Worker w = worker(script);
        if (w.stuck) {
            w.stat.dropped++;
            return;
        }
        final Runnable body = task;
        try {
            w.pool.execute(new Runnable() {
                @Override public void run() {
                    final long t0 = System.currentTimeMillis();
                    w.startAt = t0;
                    w.running = true;
                    w.owner = Thread.currentThread();
                    try {
                        body.run();
                        w.hits = 0;
                    } catch (Throwable t) {
                        w.stat.errors++;
                        w.stat.lastError = String.valueOf(t);
                        try {
                            ModHealth.record(w.name, "运行异常：" + t);
                        } catch (Throwable ignore) {
                        }
                        com.cathelperawa.owo.app.core.HookLog.e(
                                "[脚本] " + w.name + " 回调异常", t);
                    } finally {
                        w.owner = null;
                        w.running = false;
                        long d = System.currentTimeMillis() - t0;
                        w.stat.calls++;
                        w.stat.totalMs += d;
                        if (d > w.stat.maxMs) w.stat.maxMs = d;
                    }
                }
            });
        } catch (Throwable t) {
            w.stat.dropped++;
        }
    }

    public static boolean call(String script, Runnable task, long timeoutMs) {
        if (task == null) return false;
        final Worker w = worker(script);
        if (Thread.currentThread() == w.owner) {
            try {
                task.run();
                return true;
            } catch (Throwable t) {
                w.stat.errors++;
                w.stat.lastError = String.valueOf(t);
                return false;
            }
        }
        if (w.stuck) {
            w.stat.dropped++;
            return false;
        }
        final java.util.concurrent.FutureTask<Void> ft =
                new java.util.concurrent.FutureTask<Void>(new java.util.concurrent.Callable<Void>() {
                    @Override public Void call() throws Exception {
                        final long t0 = System.currentTimeMillis();
                        w.startAt = t0;
                        w.running = true;
                        w.owner = Thread.currentThread();
                        try {
                            task.run();
                            w.hits = 0;
                        } catch (Throwable t) {
                            w.stat.errors++;
                            w.stat.lastError = String.valueOf(t);
                            try {
                                ModHealth.record(w.name, "运行异常：" + t);
                            } catch (Throwable ignore) {
                            }
                            com.cathelperawa.owo.app.core.HookLog.e(
                                    "[脚本] " + w.name + " 回调异常", t);
                        } finally {
                            w.owner = null;
                            w.running = false;
                            long d = System.currentTimeMillis() - t0;
                            w.stat.calls++;
                            w.stat.totalMs += d;
                            if (d > w.stat.maxMs) w.stat.maxMs = d;
                        }
                        return null;
                    }
                });
        try {
            w.pool.execute(ft);
        } catch (Throwable t) {
            w.stat.dropped++;
            return false;
        }
        try {
            ft.get(timeoutMs, TimeUnit.MILLISECONDS);
            return true;
        } catch (Throwable t) {
            w.stat.timeouts++;
            ft.cancel(true);
            return false;
        }
    }

    public static void runSync(String script, Runnable task) {
        if (task == null) return;
        final Worker w = worker(script);
        w.startAt = System.currentTimeMillis();
        w.running = true;
        long t0 = w.startAt;
        try {
            task.run();
            w.hits = 0;
        } catch (Throwable t) {
            w.stat.errors++;
            w.stat.lastError = String.valueOf(t);
        } finally {
            w.running = false;
            long d = System.currentTimeMillis() - t0;
            w.stat.calls++;
            w.stat.totalMs += d;
            if (d > w.stat.maxMs) w.stat.maxMs = d;
        }
    }

    public static Stat stats(String script) {
        synchronized (MAP) {
            Worker w = WORKERS.get(script);
            return w == null ? new Stat() : w.stat;
        }
    }

    public static boolean isStuck(String script) {
        synchronized (MAP) {
            Worker w = WORKERS.get(script);
            return w != null && w.stuck;
        }
    }

    public static void reset(String script) {
        synchronized (MAP) {
            Worker w = WORKERS.get(script);
            if (w == null) return;
            w.stuck = false;
            w.hits = 0;
        }
    }

    public static void shutdown(String script) {
        Worker w;
        synchronized (MAP) {
            w = WORKERS.remove(script);
        }
        if (w == null) return;
        try {
            w.pool.shutdownNow();
        } catch (Throwable ignore) {
        }
    }

    public static String summary() {
        StringBuilder sb = new StringBuilder();
        synchronized (MAP) {
            java.util.List<String> keys = new java.util.ArrayList<String>(WORKERS.keySet());
            java.util.Collections.sort(keys);
            for (int i = 0; i < keys.size(); i++) {
                String k = keys.get(i);
                Worker w = WORKERS.get(k);
                Stat s = w.stat;
                if (s.calls == 0) continue;
                sb.append(k).append("：").append(s.calls).append(" 次")
                        .append(" / 均 ").append(s.calls == 0 ? 0 : s.totalMs / s.calls).append("ms")
                        .append(" / 峰 ").append(s.maxMs).append("ms");
                if (s.errors > 0) sb.append(" / 错 ").append(s.errors);
                if (s.timeouts > 0) sb.append(" / 超时 ").append(s.timeouts);
                if (s.dropped > 0) sb.append(" / 丢弃 ").append(s.dropped);
                if (w.stuck) sb.append(" / ⚠卡死");
                sb.append('\n');
            }
        }
        return sb.toString();
    }
}
