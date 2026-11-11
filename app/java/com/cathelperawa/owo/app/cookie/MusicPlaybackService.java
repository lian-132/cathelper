package com.cathelperawa.owo.app.cookie;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

public class MusicPlaybackService extends Service {

    private static final String CHANNEL_ID = "cathelper_music_play";
    private static final int NOTI_ID = 0x0CA7;

    private static String sTitle = "";
    private static String sArtist = "";
    private static boolean sPlaying = false;
    private static android.media.session.MediaSession sSession;

    

    
    public static void start(Context ctx, String title, String artist,
                             boolean playing, android.media.session.MediaSession session) {
        if (ctx == null) return;
        sTitle = title == null ? "" : title;
        sArtist = artist == null ? "" : artist;
        sPlaying = playing;
        sSession = session;
        try {
            Intent it = new Intent(ctx, MusicPlaybackService.class);
            if (Build.VERSION.SDK_INT >= 26) ctx.startForegroundService(it);
            else ctx.startService(it);
        } catch (Throwable t) {
            android.util.Log.w("CatMusic", "启动前台服务失败: " + t);
        }
    }

    public static void update(Context ctx, String title, String artist, boolean playing) {
        sTitle = title == null ? "" : title;
        sArtist = artist == null ? "" : artist;
        sPlaying = playing;
        if (ctx == null) return;
        try {
            NotificationManager nm = (NotificationManager)
                    ctx.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm == null) return;
            ensureChannel(ctx, nm);
            nm.notify(NOTI_ID, build(ctx));
        } catch (Throwable ignore) {
        }
    }

    public static void stop(Context ctx) {
        if (ctx == null) return;
        try {
            ctx.stopService(new Intent(ctx, MusicPlaybackService.class));
        } catch (Throwable ignore) {
        }
    }

    

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            NotificationManager nm = (NotificationManager)
                    getSystemService(NOTIFICATION_SERVICE);
            if (nm != null) {
                ensureChannel(this, nm);
                startForeground(NOTI_ID, build(this));
            }
        } catch (Throwable t) {
            android.util.Log.w("CatMusic", "startForeground 失败: " + t + "（检查通知权限）");
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        try {
            stopForeground(true);
        } catch (Throwable ignore) {
        }
        super.onDestroy();
    }

    

    private static void ensureChannel(Context ctx, NotificationManager nm) {
        if (Build.VERSION.SDK_INT < 26 || nm == null) return;
        try {
            if (nm.getNotificationChannel(CHANNEL_ID) != null) return;
            NotificationChannel ch = new NotificationChannel(CHANNEL_ID,
                    "音乐播放", NotificationManager.IMPORTANCE_LOW);
            ch.setDescription("聚合中心正在播放时显示的常驻通知");
            ch.setShowBadge(false);
            ch.setSound(null, null);
            nm.createNotificationChannel(ch);
        } catch (Throwable ignore) {
        }
    }

    private static Notification build(Context ctx) {
        Notification.Builder b;
        if (Build.VERSION.SDK_INT >= 26) {
            b = new Notification.Builder(ctx, CHANNEL_ID);
        } else {
            b = new Notification.Builder(ctx);
        }
        b.setSmallIcon(com.cathelperawa.owo.app.R.drawable.ic_module);
        b.setContentTitle(sTitle.length() == 0 ? "猫娘助手" : sTitle);
        b.setContentText(sArtist.length() == 0
                ? (sPlaying ? "正在播放" : "已暂停") : sArtist);
        b.setOngoing(sPlaying);
        b.setOnlyAlertOnce(true);
        b.setShowWhen(false);

        try {
            Intent open = new Intent(ctx, com.cathelperawa.owo.app.MainActivity.class);
            open.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            open.putExtra("cat_music_hub", true);
            PendingIntent pi = PendingIntent.getActivity(ctx, 1, open,
                    PendingIntent.FLAG_UPDATE_CURRENT
                            | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0));
            b.setContentIntent(pi);
        } catch (Throwable ignore) {
        }

        try {
            if (sSession != null) {
                b.setStyle(new Notification.MediaStyle()
                        .setMediaSession(sSession.getSessionToken())
                        .setShowActionsInCompactView());
                
                b.setColor(0xFFE85D9C);
            }
        } catch (Throwable ignore) {
        }
        return b.build();
    }
}
