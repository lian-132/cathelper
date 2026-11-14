package com.cathelperawa.owo.app.core;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public final class Bg {

    private Bg() {}

    private static final AtomicInteger SEQ = new AtomicInteger(0);

    private static final ThreadFactory FACTORY = new ThreadFactory() {
        @Override public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "cat-bg-" + SEQ.incrementAndGet());
            t.setDaemon(true);
            t.setPriority(Thread.NORM_PRIORITY - 1);
            return t;
        }
    };

    private static volatile ExecutorService pool;

    private static ExecutorService pool() {
        ExecutorService p = pool;
        if (p != null) return p;
        synchronized (Bg.class) {
            if (pool == null) {
                pool = new ThreadPoolExecutor(
                        0, Integer.MAX_VALUE,
                        30L, TimeUnit.SECONDS,
                        new SynchronousQueue<Runnable>(),
                        FACTORY);
            }
            return pool;
        }
    }

    public static void run(Runnable r) {
        if (r == null) return;
        try {
            pool().execute(r);
        } catch (Throwable t) {
            try {
                Thread th = FACTORY.newThread(r);
                th.start();
            } catch (Throwable ignore) {
            }
        }
    }

    public static void run(Runnable r, String name) {
        run(r);
    }

    public static void run(String name, Runnable r) {
        run(r);
    }

    public static int activeCount() {
        try {
            ExecutorService p = pool;
            if (p instanceof ThreadPoolExecutor) return ((ThreadPoolExecutor) p).getPoolSize();
        } catch (Throwable ignore) {
        }
        return 0;
    }
}
