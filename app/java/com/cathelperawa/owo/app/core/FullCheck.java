package com.cathelperawa.owo.app.core;

import android.content.Context;
import com.cathelperawa.owo.app.core.Bg;
import android.content.pm.ApplicationInfo;
import android.os.Handler;
import android.os.Looper;

import java.io.InputStream;

public final class FullCheck {

    public static final String FILE = "dex_probe.txt";
    public static final String ACTION = "com.cathelperawa.owo.app.CMD";
    private static final String HOST_PKG = "com.tencent.tim";
    private static final int MAX_WAIT_SEC = 14;

    public interface Callback {
        
        void onDone(String message, String body);
    }

    private FullCheck() {}

    public static void runAsync(final Context ctx, final Callback cb) {
        Bg.run(new Runnable() {
            @Override public void run() {
                String msg;
                String body = null;
                try {
                    long before = stamp();
                    String how = trigger(ctx);
                    if (how == null) {
                        msg = "触发失败：TIM 没在跑，也拿不到 root";
                    } else {
                        for (int i = 0; i < MAX_WAIT_SEC; i++) {
                            Thread.sleep(1000);
                            if (stamp() != before) break;
                        }
                        body = StatusStore.readHostFile(FILE);
                        msg = body == null
                                ? "已通过" + how + "触发，但读不到报告（root 授权了吗？）"
                                : "体检完成（" + how + "触发）";
                    }
                } catch (Throwable t) {
                    msg = "体检失败：" + t;
                }
                final String fm = msg;
                final String fb = body;
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override public void run() { cb.onDone(fm, fb); }
                });
            }
        }, "full-check");
    }

    private static String trigger(Context ctx) {
        
        try {
            if (ModuleCommand.send(ctx, ModuleCommand.CMD_DEX_PROBE)) return "广播";
        } catch (Throwable ignore) {
        }
        
        try {
            int uid = hostUid(ctx);
            if (uid <= 0) return null;
            String inner = "am broadcast --user 0 -a " + ACTION + " --es cmd dex_probe";
            sh("su -c \"su " + uid + " sh -c '" + inner + "'\"", 8000);
            return "root";
        } catch (Throwable ignore) {
        }
        return null;
    }

    private static int hostUid(Context ctx) {
        try {
            ApplicationInfo ai = ctx.getPackageManager().getApplicationInfo(HOST_PKG, 0);
            if (ai != null && ai.uid > 0) return ai.uid;
        } catch (Throwable ignore) {
        }
        try {
            String out = sh("su -c \"stat -c %u /data/user/0/" + HOST_PKG + "\"", 5000);
            if (out != null) {
                String s = out.trim();
                if (s.matches("\\d+")) return Integer.parseInt(s);
            }
        } catch (Throwable ignore) {
        }
        return -1;
    }

    private static String sh(String cmd, int timeoutMs) {
        Process p = null;
        try {
            p = new ProcessBuilder("sh", "-c", cmd).redirectErrorStream(true).start();
            final Process fp = p;
            Bg.run(new Runnable() {
                @Override public void run() {
                    try {
                        Thread.sleep(timeoutMs);
                        try { fp.destroy(); } catch (Throwable ignore) {}
                    } catch (InterruptedException ignore) {
                    }
                }
            });
            InputStream in = p.getInputStream();
            java.io.ByteArrayOutputStream bo = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[2048];
            int n;
            while ((n = in.read(buf)) > 0) bo.write(buf, 0, n);
            return new String(bo.toByteArray(), "UTF-8");
        } catch (Throwable t) {
            return null;
        } finally {
            if (p != null) try { p.destroy(); } catch (Throwable ignore) {}
        }
    }

    private static long stamp() {
        try {
            String p = StatusStore.hostFiles() + "/" + FILE;
            String out = sh("su -c \"stat -c '%Y %s' '" + p + "'\"", 4000);
            if (out != null) {
                String s = out.trim();
                String[] parts = s.split("\\s+");
                if (parts.length >= 2 && parts[0].matches("\\d+") && parts[1].matches("\\d+")) {
                    return Long.parseLong(parts[0]) * 1000L + Long.parseLong(parts[1]);
                }
            }
        } catch (Throwable ignore) {
        }
        return 0;
    }
}
