package com.cathelperawa.owo.app.util;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class ModIcon {

    public static final String MODULE_PKG = "com.cathelperawa.owo.app";

    private static volatile Drawable cachedIc;
    private static volatile boolean triedIc;

    private ModIcon() {}

    public static Drawable ic(Context c) {
        Drawable d = cachedIc;
        if (d != null || triedIc) return d;
        synchronized (ModIcon.class) {
            if (cachedIc != null || triedIc) return cachedIc;
            triedIc = true;
            cachedIc = load(c, "ic_module");
            return cachedIc;
        }
    }

    public static Drawable load(Context c, String name) {
        if (c == null || name == null) return null;

        try {
            Context mc = c.createPackageContext(MODULE_PKG,
                    Context.CONTEXT_IGNORE_SECURITY | Context.CONTEXT_INCLUDE_CODE);
            int id = mc.getResources().getIdentifier(name, "drawable", MODULE_PKG);
            if (id != 0) {
                Drawable d = mc.getResources().getDrawable(id, null);
                if (d != null) return d;
            }
        } catch (Throwable ignore) {
        }

        try {
            ApplicationInfo ai = c.getPackageManager().getApplicationInfo(MODULE_PKG, 0);
            String apk = ai.sourceDir;
            ZipFile zf = null;
            try {
                zf = new ZipFile(apk);
                ZipEntry e = zf.getEntry("res/drawable/" + name + ".png");
                if (e == null) e = findEntry(zf, name);
                if (e == null) return null;
                try (InputStream in = zf.getInputStream(e)) {
                    BitmapFactory.Options o = new BitmapFactory.Options();
                    o.inPreferredConfig = Bitmap.Config.ARGB_8888;
                    Bitmap bm = BitmapFactory.decodeStream(in, null, o);
                    if (bm == null) return null;
                    BitmapDrawable bd = new BitmapDrawable(c.getResources(), bm);
                    bd.setTargetDensity(c.getResources().getDisplayMetrics().densityDpi);
                    return bd;
                }
            } finally {
                if (zf != null) {
                    try { zf.close(); } catch (Throwable ignore) {}
                }
            }
        } catch (Throwable ignore) {
        }
        return null;
    }

    private static ZipEntry findEntry(ZipFile zf, String name) {
        String want = "/" + name + ".png";
        java.util.Enumeration<? extends ZipEntry> en = zf.entries();
        while (en.hasMoreElements()) {
            ZipEntry e = en.nextElement();
            String n = e.getName();
            if (n.startsWith("res/") && n.endsWith(want)) return e;
        }
        return null;
    }
}
