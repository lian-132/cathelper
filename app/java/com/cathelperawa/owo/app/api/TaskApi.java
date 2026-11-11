package com.cathelperawa.owo.app.api;

import com.cathelperawa.owo.app.core.HookLog;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public final class TaskApi {

    private TaskApi() {}

    public static final long MIN_MS = 10;

    public static final class Token {
        final long id;
        final boolean repeat;
        final long intervalMs;
        final Runnable body;
        volatile boolean cancelled;
        volatile ScheduledFuture<?> future;
        final AtomicLong runs = new AtomicLong();
        final AtomicLong errors = new AtomicLong();
        volatile String lastError;

        Token(long id, boolean repeat, long intervalMs, Runnable body) {
            this.id = id;
            this.repeat = repeat;
            this.intervalMs = intervalMs;
            this.body = body;
        }

        public boolean isRepeat() { return repeat; }
        public long intervalMs() { return intervalMs; }
        public long runs() { return runs.get(); }
        public long errors() { return errors.get(); }
        public String lastError() { return lastError; }
        public boolean isCancelled() { return cancelled; }
        public boolean isAlive() { return !cancelled && future != null && !future.isCancelled(); }

        @Override
        public String toString() {
            return "Task#" + id + (repeat ? " every " + intervalMs + "ms" : " once")
                    + " runs=" + runs.get() + (cancelled ? " (cancelled)" : "");
        }
    }

    private static final AtomicLong ID = new AtomicLong();
    private static final Set<Token> ALIVE =
            Collections.newSetFromMap(new ConcurrentHashMap<Token, Boolean>());
    private static final AtomicLong TOTAL_ERRORS = new AtomicLong();
    private static volatile ScheduledThreadPoolExecutor POOL;

    

    
    public static Token after(long delayMs, Runnable body) {
        return submit(false, Math.max(MIN_MS, delayMs), body);
    }

    public static Token every(long intervalMs, Runnable body) {
        return submit(true, Math.max(MIN_MS, intervalMs), body);
    }

    public static Token delay(long delayMs, Runnable body) {
        return after(delayMs, body);
    }

    private static Token submit(boolean repeat, final long ms, final Runnable body) {
        if (body == null) return null;
        final Token t = new Token(ID.incrementAndGet(), repeat, ms, body);
        Runnable wrapped = new Runnable() {
            @Override
            public void run() {
                if (t.cancelled) return;
                try {
                    body.run();
                    t.runs.incrementAndGet();
                } catch (Throwable e) {
                    t.errors.incrementAndGet();
                    t.lastError = e.getClass().getSimpleName() + ": " + e.getMessage();
                    TOTAL_ERRORS.incrementAndGet();
                    HookLog.e("[定时] 任务 #" + t.id + " 抛异常（不会停止）", e);
                } finally {
                    if (!repeat) {
                        ALIVE.remove(t);
                    }
                }
            }
        };
        try {
            ScheduledThreadPoolExecutor pool = pool();
            if (repeat) {
                t.future = pool.scheduleWithFixedDelay(wrapped, ms, ms, TimeUnit.MILLISECONDS);
            } else {
                t.future = pool.schedule(wrapped, ms, TimeUnit.MILLISECONDS);
            }
            ALIVE.add(t);
            return t;
        } catch (Throwable e) {
            HookLog.e("[定时] 提交失败", e);
            return null;
        }
    }

    

    
    public static boolean cancel(Object token) {
        if (!(token instanceof Token)) return false;
        Token t = (Token) token;
        t.cancelled = true;
        ScheduledFuture<?> f = t.future;
        boolean ok = f != null && f.cancel(false);
        ALIVE.remove(t);
        return ok;
    }

    public static void cancelAll() {
        List<Token> snapshot = new ArrayList<Token>(ALIVE);
        for (int i = 0; i < snapshot.size(); i++) {
            cancel(snapshot.get(i));
        }
        ALIVE.clear();
    }

    

    public static int count() { return ALIVE.size(); }

    public static long errorCount() { return TOTAL_ERRORS.get(); }

    public static List<String> list() {
        List<String> out = new ArrayList<String>();
        for (Token t : ALIVE) out.add(t.toString());
        return out;
    }

    public static JSONObject stats() {
        try {
            JSONObject o = new JSONObject();
            o.put("alive", ALIVE.size());
            o.put("errors", TOTAL_ERRORS.get());
            ScheduledThreadPoolExecutor p = POOL;
            o.put("queued", p == null ? 0 : p.getQueue().size());
            return o;
        } catch (Throwable t) {
            return null;
        }
    }

    public static String statsJson() {
        JSONObject o = stats();
        return o == null ? "{}" : o.toString();
    }

    

    private static ScheduledThreadPoolExecutor pool() {
        if (POOL != null) return POOL;
        synchronized (TaskApi.class) {
            if (POOL != null) return POOL;
            ThreadFactory tf = new ThreadFactory() {
                private int n = 1;
                @Override
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r, "cat-task-" + (n++));
                    t.setDaemon(true);
                    return t;
                }
            };
            ScheduledThreadPoolExecutor ex = new ScheduledThreadPoolExecutor(2, tf);
            ex.setRemoveOnCancelPolicy(true);
            ex.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
            POOL = ex;
            return ex;
        }
    }
}
