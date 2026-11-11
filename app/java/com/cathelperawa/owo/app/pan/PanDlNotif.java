package com.cathelperawa.owo.app.pan;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class PanDlNotif {

    private PanDlNotif() {}

    public static final String CHANNEL_ID = "cathelper_pan_dl";
    
    public static final int SUMMARY_ID = 0x0DA1;
    
    private static final int BASE_ID = 0x0DA2;
    private static final int ID_SPAN = 4000;

    public static void ensureChannel(Context ctx) {
        if (Build.VERSION.SDK_INT < 26 || ctx == null) return;
        try {
            NotificationManager nm =
                    (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm == null || nm.getNotificationChannel(CHANNEL_ID) != null) return;
            NotificationChannel ch = new NotificationChannel(CHANNEL_ID,
                    "网盘下载", NotificationManager.IMPORTANCE_LOW);
            ch.setDescription("网盘下载进度、实时速度与续传按钮");
            ch.setShowBadge(false);
            ch.setSound(null, null);
            ch.enableVibration(false);
            nm.createNotificationChannel(ch);
        } catch (Throwable ignore) {
        }
    }

    private static NotificationManager nm(Context c) {
        return (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    public static int idOf(PanTask t) {
        int h = t == null || t.id == null ? 0 : t.id.hashCode();
        return BASE_ID + (Math.abs(h) % ID_SPAN);
    }

    
    public static Notification buildTask(Context ctx, PanTask t) {
        Notification.Builder b = builder(ctx);
        b.setSmallIcon(com.cathelperawa.owo.app.R.drawable.ic_module);
        b.setContentTitle(t.name == null || t.name.length() == 0 ? "网盘文件" : t.name);
        b.setOnlyAlertOnce(true);
        b.setShowWhen(false);
        b.setWhen(System.currentTimeMillis());

        boolean running = t.state == PanTask.RUN || t.state == PanTask.WAIT;

        if (running) {
            String line;
            if (t.state == PanTask.WAIT) {
                line = "排队中…";
            } else {
                line = percentText(t) + " · " + sizeText(t) + " · " + speedText(t);
            }
            b.setContentText(line);
            b.setSubText(t.state == PanTask.WAIT ? "等待前面的任务" : speedText(t));
            if (t.total > 0) {
                b.setProgress(100, t.percent(), false);
            } else {
                b.setProgress(0, 0, true);
            }
            b.setOngoing(true);
            b.setCategory(Notification.CATEGORY_PROGRESS);
            b.addAction(android.R.drawable.ic_menu_close_clear_cancel, "取消",
                    action(ctx, PanDlService.ACTION_CANCEL, t.id, idOf(t) + 1));
            b.setContentIntent(openApp(ctx, idOf(t) + 2));
        } else if (t.state == PanTask.FAIL) {
            b.setContentText("失败/未完成：" + (t.err == null || t.err.length() == 0 ? "未知原因" : t.err));
            b.setProgress(100, t.percent(), false);
            b.setOngoing(false);
            b.setAutoCancel(false);
            b.addAction(android.R.drawable.stat_notify_sync, "续传",
                    action(ctx, PanDlService.ACTION_RESUME, t.id, idOf(t) + 1));
            b.addAction(android.R.drawable.ic_menu_delete, "删除",
                    action(ctx, PanDlService.ACTION_DELETE, t.id, idOf(t) + 3));
            b.setContentIntent(openApp(ctx, idOf(t) + 2));
        } else if (t.state == PanTask.DONE) {
            b.setContentText("已完成 · " + PanItem.human(t.total > 0 ? t.total : t.done) + "，已保存到 Download/" + PanPrefs.subDir(ctx));
            b.setProgress(0, 0, false);
            b.setOngoing(false);
            b.setAutoCancel(true);
            if (t.publicUri != null && t.publicUri.length() > 0) {
                b.addAction(android.R.drawable.ic_menu_view, "打开", openTask(ctx, t.id, idOf(t) + 1));
            }
            b.addAction(android.R.drawable.ic_menu_delete, "删除",
                    action(ctx, PanDlService.ACTION_DELETE, t.id, idOf(t) + 3));
            b.setContentIntent(openApp(ctx, idOf(t) + 2));
        } else {
            return null;
        }
        return b.build();
    }

    private static String percentText(PanTask t) {
        if (t.total <= 0) return PanItem.human(t.done) + " / --";
        return t.percent() + "%";
    }

    private static String sizeText(PanTask t) {
        if (t.total <= 0) return "";
        return PanItem.human(t.done) + " / " + PanItem.human(t.total);
    }

    private static String speedText(PanTask t) {
        long s = t.speed;
        if (s <= 0) return "0 B/s";
        if (s < 1024) return s + " B/s";
        double k = s / 1024.0;
        if (k < 1024) return String.format(Locale.CHINA, "%.0f KB/s", k);
        return String.format(Locale.CHINA, "%.1f MB/s", k / 1024.0);
    }

    public static Notification buildSummary(Context ctx, List<PanTask> list) {
        int running = 0;
        long speed = 0;
        PanTask cur = null;
        for (PanTask t : list) {
            if (t.state == PanTask.RUN || t.state == PanTask.WAIT) {
                running++;
                speed += t.speed;
                if (cur == null || (cur.state != PanTask.RUN && t.state == PanTask.RUN)) cur = t;
            }
        }
        Notification.Builder b = builder(ctx);
        b.setSmallIcon(com.cathelperawa.owo.app.R.drawable.ic_module);
        b.setOnlyAlertOnce(true);
        b.setShowWhen(false);
        b.setOngoing(running > 0);
        if (running == 0) {
            b.setContentTitle("猫娘网盘 · 下载已结束");
            b.setContentText("点开看「下载目录」");
            b.setProgress(0, 0, false);
        } else if (cur == null) {
            b.setContentTitle("猫娘网盘 · 正在下载 " + running + " 个");
            b.setContentText("准备中…");
            b.setProgress(0, 0, true);
        } else {
            b.setContentTitle("猫娘网盘 · 正在下载 " + running + " 个");
            b.setContentText(cur.name + "  " + (cur.total > 0 ? cur.percent() + "%" : "")
                    + " · " + totalSpeed(speed));
            b.setSubText(speed > 0 ? "合计 " + totalSpeed(speed) : "");
            if (cur.total > 0) b.setProgress(100, cur.percent(), false);
            else b.setProgress(0, 0, true);
        }
        b.setContentIntent(openApp(ctx, SUMMARY_ID));
        return b.build();
    }

    private static String totalSpeed(long s) {
        if (s <= 0) return "0 B/s";
        double k = s / 1024.0;
        if (k < 1024) return String.format(Locale.CHINA, "%.0f KB/s", k);
        return String.format(Locale.CHINA, "%.1f MB/s", k / 1024.0);
    }

    
    public static int sync(Context ctx) {
        return sync(ctx, -1);
    }

    public static int sync(Context ctx, int skipId) {
        if (ctx == null) return 0;
        NotificationManager m = nm(ctx);
        if (m == null) return 0;
        ensureChannel(ctx);
        List<PanTask> list = PanDl.tasks();
        Set<Integer> live = new HashSet<Integer>();
        int running = 0;
        for (PanTask t : list) {
            if (t.state == PanTask.RUN || t.state == PanTask.WAIT) running++;
            Notification n;
            try {
                n = buildTask(ctx, t);
            } catch (Throwable e) {
                n = null;
            }
            if (n == null) continue;
            int id = idOf(t);
            live.add(id);
            if (id == skipId) continue;
            try {
                m.notify(id, n);
            } catch (Throwable ignore) {
            }
        }
        
        for (Integer id : sShown) {
            if (!live.contains(id)) {
                try {
                    m.cancel(id);
                } catch (Throwable ignore) {
                }
            }
        }
        sShown.clear();
        sShown.addAll(live);
        return running;
    }

    private static final Set<Integer> sShown = new HashSet<Integer>();

    public static void clearAll(Context ctx) {
        NotificationManager m = nm(ctx);
        if (m == null) return;
        for (Integer id : sShown) {
            try {
                m.cancel(id);
            } catch (Throwable ignore) {
            }
        }
        sShown.clear();
        try {
            m.cancel(SUMMARY_ID);
        } catch (Throwable ignore) {
        }
    }

    private static Notification.Builder builder(Context ctx) {
        if (Build.VERSION.SDK_INT >= 26) return new Notification.Builder(ctx, CHANNEL_ID);
        return new Notification.Builder(ctx);
    }

    private static PendingIntent openApp(Context ctx, int req) {
        Intent it = new Intent(ctx, PanActivity.class);
        it.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        it.putExtra(PanActivity.EXTRA_OPEN_DOWNLOADS, true);
        return PendingIntent.getActivity(ctx, req, it, piFlags());
    }

    private static PendingIntent openTask(Context ctx, String taskId, int req) {
        Intent it = new Intent(ctx, PanActivity.class);
        it.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        it.putExtra(PanActivity.EXTRA_OPEN_TASK, taskId);
        return PendingIntent.getActivity(ctx, req, it, piFlags());
    }

    private static PendingIntent action(Context ctx, String action, String taskId, int req) {
        Intent it = new Intent(ctx, PanDlService.class);
        it.setAction(action);
        it.putExtra(PanDlService.EXTRA_TASK_ID, taskId);
        return PendingIntent.getService(ctx, req, it, piFlags());
    }

    private static int piFlags() {
        return PendingIntent.FLAG_UPDATE_CURRENT
                | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0);
    }

    public static boolean openFile(Context ctx, PanTask t) {
        if (ctx == null || t == null || t.publicUri == null || t.publicUri.length() == 0) return false;
        try {
            Intent it;
            String u = t.publicUri;
            if (u.startsWith("content://")) {
                it = new Intent(Intent.ACTION_VIEW, Uri.parse(u));
            } else {
                it = new Intent(Intent.ACTION_VIEW, Uri.fromFile(new java.io.File(u)));
            }
            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (t.mime != null && t.mime.length() > 0) it.setDataAndType(it.getData(), t.mime);
            it.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            ctx.startActivity(it);
            return true;
        } catch (Throwable ignore) {
        }
        try {
            String u = t.publicUri == null ? "" : t.publicUri;
            android.widget.Toast.makeText(ctx, "没有应用能打开这个文件："
                    + (u.length() > 0 ? u : t.name), android.widget.Toast.LENGTH_LONG).show();
        } catch (Throwable ignore2) {
        }
        return false;
    }
}
