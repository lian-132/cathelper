package com.cathelperawa.owo.app.util;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public final class CatLog {

    private CatLog() {}

    public static final int DEBUG = 0;
    public static final int INFO  = 1;
    public static final int WARN  = 2;
    public static final int ERROR = 3;

    private static volatile int minLevel = DEBUG;

    private static final int QUEUE_CAP = 1024;
    private static final BlockingQueue<String> QUEUE = new ArrayBlockingQueue<String>(QUEUE_CAP);

    private static final String TAG = "猫娘助手";

    private static volatile Context appCtx;
    private static volatile boolean workerStarted = false;

    private static final java.util.concurrent.atomic.AtomicLong DROPPED =
            new java.util.concurrent.atomic.AtomicLong(0);

    public static void setContext(Context ctx) {
        if (ctx != null && appCtx == null) {
            appCtx = ctx.getApplicationContext();
            startWorker();
        }
    }

    public static void setMinLevel(int level) {
        minLevel = level;
    }

    public static void d(String msg) { log(DEBUG, msg, null); }
    public static void i(String msg) { log(INFO, msg, null); }
    public static void w(String msg) { log(WARN, msg, null); }
    public static void e(String msg) { log(ERROR, msg, null); }
    public static void e(String msg, Throwable t) { log(ERROR, msg, t); }

    public static void log(String msg) { log(INFO, msg, null); }

    public static void log(int level, String msg, Throwable t) {
        if (level < minLevel) return;

        String line = format(level, msg, t);

        try {
            de.robv.android.xposed.XposedBridge.log("[" + TAG + "] " + line);
        } catch (Throwable ignore) {
            
        }

        if (!QUEUE.offer(line)) {
            DROPPED.incrementAndGet();
        }
    }

    public static void enqueue(int level, String msg, Throwable t) {
        if (level < minLevel) return;
        String line = format(level, msg, t);
        if (!QUEUE.offer(line)) {
            DROPPED.incrementAndGet();
        }
    }

    private static String format(int level, String msg, Throwable t) {
        StringBuilder sb = new StringBuilder(96);
        sb.append(LEVEL_TAG[level]).append(' ').append(msg);
        if (t != null) {
            sb.append(" -> ").append(t.getClass().getSimpleName())
              .append(": ").append(t.getMessage());
        }
        return sb.toString();
    }

    private static final String[] LEVEL_TAG = {"D", "I", "W", "E"};

    private static void startWorker() {
        if (workerStarted) return;
        synchronized (CatLog.class) {
            if (workerStarted) return;
            workerStarted = true;

            Thread t = new Thread(new Runnable() {
                @Override public void run() {
                    final java.util.ArrayList<String> batch = new java.util.ArrayList<String>(32);
                    while (true) {
                        try {
                            batch.clear();
                            
                            String first = QUEUE.take();
                            batch.add(first);
                            
                            QUEUE.drainTo(batch, 31);

                            long dropped = DROPPED.getAndSet(0);
                            if (dropped > 0) {
                                batch.add("[CatLog] 队列满，丢弃 " + dropped + " 条");
                            }

                            flush(batch);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            return;
                        } catch (Throwable ignore) {
                            
                        }
                    }
                }
            }, "CatLog-Worker");
            t.setDaemon(true);
            t.start();
        }
    }

    private static void flush(java.util.ArrayList<String> batch) {
        if (batch.isEmpty()) return;

        

        

        

        

        
        
        try {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < batch.size(); i++) {
                sb.append(batch.get(i)).append('\n');
            }
            com.cathelperawa.owo.app.MsgStore.appendLog(sb.toString());
        } catch (Throwable ignore) {
            
        }

        final Context ctx = appCtx;
        if (ctx == null) return;
        try {
            ContentValues[] arr = new ContentValues[batch.size()];
            for (int i = 0; i < batch.size(); i++) {
                ContentValues v = new ContentValues();
                v.put("tag", TAG);
                v.put("msg", batch.get(i));
                arr[i] = v;
            }
            
            ctx.getContentResolver().bulkInsert(
                    Uri.parse("content://com.cathelperawa.owo.app.logs"), arr);
        } catch (Throwable ignore) {
            
        }
    }
}
