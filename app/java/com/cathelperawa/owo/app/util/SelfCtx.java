package com.cathelperawa.owo.app.util;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Resources;

import java.io.File;
import java.lang.reflect.Method;

public final class SelfCtx {

    private SelfCtx() {}

    public static final String PKG = "com.cathelperawa.owo.app";

    private static volatile Context self = null;
    private static volatile boolean tried = false;

    public static Context get(Context host) {
        if (self != null) return self;
        if (tried) return null;
        tried = true;
        if (host == null) return null;
        try {
            Context c = host.createPackageContext(PKG, Context.CONTEXT_IGNORE_SECURITY);
            if (c != null) {
                self = c;
                CatLog.d("SelfCtx 获取成功");
                return self;
            }
        } catch (Throwable t) {
            CatLog.w("SelfCtx 获取失败: " + t);
        }
        return null;
    }

    public static Context forToast(Context host) {
        Context c = get(host);
        return c != null ? c : host;
    }

    public static boolean injectResources(Context host) {
        if (host == null) return false;
        try {
            String apkPath = host.getPackageManager()
                    .getApplicationInfo(PKG, 0).sourceDir;
            Resources res = host.getResources();
            AssetManager am = res.getAssets();
            Method m = am.getClass().getMethod("addAssetPath", String.class);
            Object cookie = m.invoke(am, apkPath);
            boolean ok = cookie != null && !Integer.valueOf(0).equals(cookie);
            if (ok) CatLog.d("模块资源注入成功 cookie=" + cookie);
            return ok;
        } catch (Throwable t) {
            CatLog.w("模块资源注入失败: " + t);
            return false;
        }
    }

    public static String apkPath(Context host) {
        if (host == null) return null;
        try {
            return host.getPackageManager().getApplicationInfo(PKG, 0).sourceDir;
        } catch (Throwable t) {
            return null;
        }
    }
}
