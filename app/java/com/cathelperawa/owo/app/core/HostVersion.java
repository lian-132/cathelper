package com.cathelperawa.owo.app.core;

import android.content.Context;
import android.content.pm.PackageManager;

public final class HostVersion {

    private HostVersion() {}

    private static volatile int cached = -1;
    private static volatile long cachedAt = 0;
    private static final long TTL = 60000L;

    public static int code(Context ctx) {
        long now = System.currentTimeMillis();
        if (cached > 0 && now - cachedAt < TTL) return cached;
        int v = 0;
        if (ctx != null) {
            try {
                v = ctx.getPackageManager()
                        .getPackageInfo(ctx.getPackageName(), 0).versionCode;
            } catch (PackageManager.NameNotFoundException e) {
                HookLog.w("HostVersion：取不到宿主版本 " + e);
            } catch (Throwable t) {
                HookLog.w("HostVersion：读版本异常 " + t);
            }
        }
        cached = v;
        cachedAt = now;
        return v;
    }

    public static String name(Context ctx) {
        if (ctx == null) return "?";
        try {
            return ctx.getPackageManager()
                    .getPackageInfo(ctx.getPackageName(), 0).versionName;
        } catch (Throwable t) {
            return "?";
        }
    }

    public static boolean isAtLeast(Context ctx, int required) {
        if (required <= 0) return true;
        int v = code(ctx);

        if (v <= 0) return true;
        return v >= required;
    }

    public static String format(int code) {
        if (code <= 0) return "?";
        int a = code / 1000;
        int b = (code / 100) % 10;
        int c = (code / 10) % 10;
        int d = code % 10;
        StringBuilder sb = new StringBuilder();
        sb.append(a).append('.').append(b).append('.').append(c);
        if (d != 0) sb.append('.').append(d);
        return sb.toString();
    }
}
