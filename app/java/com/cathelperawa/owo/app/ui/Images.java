package com.cathelperawa.owo.app.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class Images {

    private Images() {}

    private static final Map<String, Bitmap> CACHE = new HashMap<String, Bitmap>();

    private static final String MODULE_PKG = "com.cathelperawa.owo.app";

    private static final String[] NAMES = {
            "hero1", "hero2", "hero3", "bg", "art1", "art2", "art3", "art4", "art5",
            "rp_auto", "rp_manual",
            "h_chat", "h_rp", "h_hook", "h_group", "h_group_card",

            "c_revoke", "c_repeat", "c_dice", "c_emoji", "c_flash", "c_edittext",

            "p_noat", "p_showat", "p_mix",

            "cat_toast",

            "p_picsummary",

            "h_unlock", "u_forward", "u_patpat", "u_revoke", "u_browser",

            "c_clockin", "u_card",

            "c_msgtime",

            "art7", "h_fake", "f_net", "f_device", "f_phone",

            "art8", "h_file", "f_apk",

            "art6",

            "bg_ai",

            "art9", "h_parse", "pa_music", "pa_video",

            "bg_script",

            "f_qua"
    };
    private static final String[] EXT = {
            "png", "png", "png", "jpg", "png", "png", "png", "png", "png", "png", "png",

            "png", "png", "png", "png", "png",
            "png", "png", "png", "png", "png", "png",
            "png", "png", "png",
            "png",
            "png",

            "png", "png", "png", "png", "png",

            "png", "png",

            "png",

            "png", "png", "png", "png", "png",

            "png", "png", "png",

            "png",

            "jpg",

            "png", "png", "png", "png",

            "jpg", "png"
    };

    public static void load(Context c) {
        if (!CACHE.isEmpty()) return;
        try {
            String apkPath = c.getPackageManager().getApplicationInfo(MODULE_PKG, 0).sourceDir;
            try (ZipFile zf = new ZipFile(apkPath)) {
                for (int ni = 0; ni < NAMES.length; ni++) {
                    String n = NAMES[ni];
                    ZipEntry e = zf.getEntry("assets/console/assets/" + n + "." + EXT[ni]);
                    if (e == null) continue;
                    BitmapFactory.Options o = new BitmapFactory.Options();

                    o.inSampleSize = 1;
                    o.inPreferredConfig = Bitmap.Config.ARGB_8888;

                    try (InputStream in = zf.getInputStream(e)) {
                        CACHE.put(n, BitmapFactory.decodeStream(in, null, o));
                    }
                }
            }
        } catch (Throwable t) {
            android.util.Log.w("CatHelper", "load images failed", t);
        }
    }

    public static Bitmap get(String name) {
        return CACHE.get(name);
    }
}
