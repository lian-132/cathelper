package com.cathelperawa.owo.app.bridge;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class CatToast {

    private CatToast() {}

    private static final Handler UI = new Handler(Looper.getMainLooper());
    private static volatile Bitmap icon;
    private static volatile boolean iconTried;

    private static synchronized Bitmap icon() {
        if (iconTried && icon != null) return icon;
        try {
            Context ctx = QQEnv.appContext();
            if (ctx == null) return null;
            String apk = ctx.getPackageManager()
                    .getApplicationInfo("com.cathelperawa.owo.app", 0).sourceDir;
            ZipFile zf = new ZipFile(apk);
            try {
                ZipEntry e = zf.getEntry("assets/console/assets/cat_toast.png");
                if (e == null) return null;
                InputStream in = zf.getInputStream(e);
                Bitmap b = BitmapFactory.decodeStream(in);
                in.close();
                icon = b;
            } finally {
                zf.close();
            }
            iconTried = (icon != null);
        } catch (Throwable t) {
            icon = null;
            iconTried = false;
        }
        return icon;
    }

    private static int dp(Context c, float v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v,
                c.getResources().getDisplayMetrics());
    }

    public static void show(final String msg) {
        UI.post(new Runnable() {
            @Override public void run() { showNow(msg); }
        });
    }

    private static void showNow(String msg) {
        Context c = QQEnv.appContext();
        if (c == null) return;
        try {
            Bitmap bmp = icon();
            if (bmp == null) {
                Toast.makeText(c, msg, Toast.LENGTH_SHORT).show();
                return;
            }

            float d = c.getResources().getDisplayMetrics().density;

            LinearLayout box = new LinearLayout(c);
            box.setOrientation(LinearLayout.HORIZONTAL);
            box.setGravity(Gravity.CENTER_VERTICAL);
            GradientDrawable g = new GradientDrawable();
            g.setColor(0xF7FFFFFF);
            g.setCornerRadius(dp(c, 24));
            g.setStroke((int) (1 * d), 0x44F07CA8);
            box.setBackgroundDrawable(g);
            box.setPadding(dp(c, 12), dp(c, 8), dp(c, 18), dp(c, 8));

            ImageView iv = new ImageView(c);
            iv.setImageDrawable(new BitmapDrawable(c.getResources(), bmp));
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            box.addView(iv, new LinearLayout.LayoutParams(dp(c, 26), dp(c, 26)));

            TextView tv = new TextView(c);
            tv.setText(msg);
            tv.setTextSize(13);
            tv.setTextColor(0xFF43587A);
            tv.setTypeface(Typeface.DEFAULT_BOLD, Typeface.BOLD);
            LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            tp.leftMargin = dp(c, 9);
            box.addView(tv, tp);

            Toast t = new Toast(c);
            t.setView(box);
            t.setDuration(Toast.LENGTH_SHORT);
            t.setGravity(Gravity.BOTTOM, 0, dp(c, 110));
            t.show();
        } catch (Throwable t) {

            try { Toast.makeText(c, msg, Toast.LENGTH_SHORT).show(); } catch (Throwable ignore) {}
        }
    }
}
