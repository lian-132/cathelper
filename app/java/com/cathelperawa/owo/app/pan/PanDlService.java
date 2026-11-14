package com.cathelperawa.owo.app.pan;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;

import java.util.List;

public class PanDlService extends Service {

    public static final String ACTION_START = "com.cathelperawa.owo.app.PAN_DL_START";
    public static final String ACTION_CANCEL = "com.cathelperawa.owo.app.PAN_DL_CANCEL";
    public static final String ACTION_RESUME = "com.cathelperawa.owo.app.PAN_DL_RESUME";
    public static final String ACTION_DELETE = "com.cathelperawa.owo.app.PAN_DL_DELETE";
    public static final String ACTION_OPEN = "com.cathelperawa.owo.app.PAN_DL_OPEN";
    public static final String EXTRA_TASK_ID = "task_id";

    private static final long TICK_MS = 900L;

    private final Handler h = new Handler(Looper.getMainLooper());
    private PowerManager.WakeLock wake;
    private boolean ticking = false;

    private static final long IDLE_EXIT_MS = 8000L;
    private long idleSince = 0;

    public static void ensure(Context ctx) {
        if (ctx == null) return;
        try {
            Intent it = new Intent(ctx, PanDlService.class);
            it.setAction(ACTION_START);
            if (Build.VERSION.SDK_INT >= 26) ctx.startForegroundService(it);
            else ctx.startService(it);
        } catch (Throwable t) {
            android.util.Log.w("PanDlService", "启动前台服务失败：" + t);
        }
    }

    @Override public IBinder onBind(Intent i) {
        return null;
    }

    @Override public void onCreate() {
        super.onCreate();
        PanDl.init(this);
        PanDlNotif.ensureChannel(this);
        try {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (pm != null) {
                wake = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "cathelper:pan_dl");
                wake.setReferenceCounted(false);
            }
        } catch (Throwable ignore) {
        }
        startTicker();
        autoResume();
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        PanDl.init(this);

        goForeground();
        String action = intent == null ? null : intent.getAction();
        String tid = intent == null ? null : intent.getStringExtra(EXTRA_TASK_ID);
        try {
            if (ACTION_CANCEL.equals(action)) {
                PanTask t = find(tid);
                if (t != null) PanDl.cancel(t);
            } else if (ACTION_RESUME.equals(action)) {
                PanTask t = find(tid);
                if (t != null) PanDl.resume(this, t);
            } else if (ACTION_DELETE.equals(action)) {
                PanTask t = find(tid);

                if (t != null) PanDl.remove(this, t, t.state == PanTask.DONE);
            } else if (ACTION_OPEN.equals(action)) {
                PanTask t = find(tid);
                if (t != null) PanDlNotif.openFile(this, t);
            }
        } catch (Throwable e) {
            android.util.Log.w("PanDlService", "处理动作失败：" + e);
        }
        startTicker();
        return START_STICKY;
    }

    @Override public void onTaskRemoved(Intent rootIntent) {

        super.onTaskRemoved(rootIntent);
    }

    @Override public void onDestroy() {
        stopTicker();
        try {
            if (wake != null && wake.isHeld()) wake.release();
        } catch (Throwable ignore) {
        }
        try {
            stopForeground(true);
        } catch (Throwable ignore) {
        }

        super.onDestroy();
    }

    private void goForeground() {
        try {
            android.app.Notification n = PanDlNotif.buildSummary(this, PanDl.tasks());
            if (Build.VERSION.SDK_INT >= 29) {
                startForeground(PanDlNotif.SUMMARY_ID, n, 0x00000008  );
            } else {
                startForeground(PanDlNotif.SUMMARY_ID, n);
            }
            if (wake != null && !wake.isHeld()) wake.acquire();
        } catch (Throwable t) {
            android.util.Log.w("PanDlService", "startForeground 失败：" + t);
        }
    }

    private void startTicker() {
        if (ticking) return;
        ticking = true;
        h.post(tick);
    }

    private void stopTicker() {
        ticking = false;
        h.removeCallbacksAndMessages(null);
    }

    private final Runnable tick = new Runnable() {
        @Override public void run() {
            try {
                step();
            } catch (Throwable e) {
                android.util.Log.w("PanDlService", "tick 出错：" + e);
            }
            if (ticking) h.postDelayed(this, TICK_MS);
        }
    };

    private void step() {
        PanDl.sampleSpeeds();
        List<PanTask> list = PanDl.tasks();
        android.app.NotificationManager m = null;
        try {
            m = (android.app.NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        } catch (Throwable ignore) {
        }

        int running = 0;
        PanTask only = null;
        for (PanTask t : list) {
            if (t.state != PanTask.RUN && t.state != PanTask.WAIT) continue;
            running++;
            if (running == 1) only = t;
        }
        if (running != 1) only = null;

        if (only != null) {

            int id = PanDlNotif.idOf(only);
            try {
                startForeground(id, PanDlNotif.buildTask(this, only));
            } catch (Throwable e) {
                android.util.Log.w("PanDlService", "startForeground(单任务) 失败：" + e);
            }
            if (m != null) m.cancel(PanDlNotif.SUMMARY_ID);
            PanDlNotif.sync(this, id);
            idleSince = 0;
            return;
        }

        if (running > 1) {

            try {
                startForeground(PanDlNotif.SUMMARY_ID, PanDlNotif.buildSummary(this, list));
            } catch (Throwable e) {
                android.util.Log.w("PanDlService", "startForeground(汇总) 失败：" + e);
            }
            PanDlNotif.sync(this, -1);
            idleSince = 0;
            return;
        }

        PanDlNotif.sync(this, -1);
        try {
            stopForeground(false);
        } catch (Throwable ignore) {
        }
        if (m != null) m.cancel(PanDlNotif.SUMMARY_ID);

        long now = System.currentTimeMillis();
        if (idleSince == 0) idleSince = now;
        if (now - idleSince >= IDLE_EXIT_MS) {
            try {
                if (wake != null && wake.isHeld()) wake.release();
            } catch (Throwable ignore) {
            }
            stopTicker();
            stopSelf();
        }
    }

    private void autoResume() {
        try {
            if (!PanPrefs.autoResume(this)) return;
            List<PanTask> list = PanDl.tasks();
            int n = 0;
            for (PanTask t : list) {
                if (t.state != PanTask.FAIL || t.cancel) continue;
                if (t.partPath == null || t.partPath.length() == 0) continue;
                if (!new java.io.File(t.partPath).exists()) continue;
                if (t.retry >= 3) continue;
                t.retry++;
                PanDl.resume(this, t);
                n++;
            }
            if (n > 0) android.util.Log.i("PanDlService", "自动续传 " + n + " 个任务");
        } catch (Throwable ignore) {
        }
    }

    private PanTask find(String id) {
        if (id == null) return null;
        for (PanTask t : PanDl.tasks()) {
            if (id.equals(t.id)) return t;
        }
        return null;
    }
}
