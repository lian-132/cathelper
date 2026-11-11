package com.cathelperawa.owo.app.bili;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class BiliThread {

    private BiliThread() {}

    private static final ExecutorService IO = Executors.newFixedThreadPool(4);
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    public static void io(Runnable r) {
        IO.execute(r);
    }

    public static void ui(Runnable r) {
        MAIN.post(r);
    }

    public static void ui(Runnable r, long delayMs) {
        MAIN.postDelayed(r, delayMs);
    }
}
