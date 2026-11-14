package com.cathelperawa.owo.app.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Movie;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public final class GifView extends View {

    private final Handler ui = new Handler(Looper.getMainLooper());
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private Movie movie;
    private Bitmap still;
    private long startMs;
    private float ratio = 0f;
    private boolean loaded;
    private boolean dead;

    public GifView(Context c) {
        super(c);
    }

    public void load(final String url) {
        if (url == null || url.length() == 0) return;
        new Thread(new Runnable() {
            @Override public void run() {
                final byte[] b = fetch(url);
                if (b == null) return;
                Movie m = null;
                Bitmap bm = null;
                try {
                    m = Movie.decodeByteArray(b, 0, b.length);
                } catch (Throwable ignore) {
                }
                if (m == null || m.width() <= 0) {
                    m = null;
                    try {
                        bm = BitmapFactory.decodeByteArray(b, 0, b.length);
                    } catch (Throwable ignore) {
                    }
                }
                final Movie fm = m;
                final Bitmap fb = bm;
                ui.post(new Runnable() {
                    @Override public void run() {
                        if (dead) return;
                        if (fm != null) {
                            movie = fm;
                            try { movie.setTime(0); } catch (Throwable ignore) {}
                            ratio = (float) fm.height() / (float) fm.width();
                        } else if (fb != null) {
                            still = fb;
                            ratio = (float) fb.getHeight() / (float) fb.getWidth();
                        } else {

                            ratio = 0.5625f;
                        }
                        loaded = true;
                        startMs = System.currentTimeMillis();
                        requestLayout();
                        try {
                            if (getParent() instanceof android.view.View) {
                                ((android.view.View) getParent()).requestLayout();
                            }
                        } catch (Throwable ignore) {}
                        invalidate();
                    }
                });
            }
        }, "cat-gif").start();
    }

    @Override protected void onMeasure(int wSpec, int hSpec) {
        int w = MeasureSpec.getSize(wSpec);
        if (w <= 0) w = getResources().getDisplayMetrics().widthPixels;
        if (!loaded || ratio <= 0f) {
            setMeasuredDimension(w, (int) (150 * getResources().getDisplayMetrics().density));
            return;
        }
        float dens = getResources().getDisplayMetrics().density;
        int h = (int) (w * ratio);
        int maxH = (int) (getResources().getDisplayMetrics().heightPixels * 0.42f);
        if (h > maxH) h = maxH;
        if (h < (int) (60 * dens)) h = (int) (60 * dens);
        setMeasuredDimension(w, h);
    }

    @Override protected void onDraw(Canvas canvas) {
        if (!loaded) return;
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;
        if (movie != null) {
            int mw = movie.width();
            int mh = movie.height();
            if (mw <= 0 || mh <= 0) return;
            float scale = Math.min((float) w / mw, (float) h / mh);
            int dur = movie.duration();
            if (dur <= 0) dur = 1000;
            long t = System.currentTimeMillis() - startMs;
            try { movie.setTime((int) (t % dur)); } catch (Throwable ignore) {}
            int save = canvas.save();
            canvas.translate((w - mw * scale) / 2f, (h - mh * scale) / 2f);
            canvas.scale(scale, scale);
            try { movie.draw(canvas, 0, 0); } catch (Throwable ignore) {}
            canvas.restoreToCount(save);
            postInvalidateOnAnimation();
        } else if (still != null) {
            float scale = Math.min((float) w / still.getWidth(), (float) h / still.getHeight());
            int dw = (int) (still.getWidth() * scale);
            int dh = (int) (still.getHeight() * scale);
            canvas.drawBitmap(still, null,
                    new android.graphics.Rect((w - dw) / 2, (h - dh) / 2, (w + dw) / 2, (h + dh) / 2),
                    paint);
        }
    }

    @Override protected void onDetachedFromWindow() {
        dead = true;
        super.onDetachedFromWindow();
    }

    private static byte[] fetch(String url) {

        byte[] b = (url != null && url.contains("cdn.jsdelivr.net")) ? null : fetchOne(url);
        if (b != null) return b;
        String[] alts = mirrors(url);
        if (alts != null) {
            for (int i = 0; i < alts.length; i++) {
                b = fetchOne(alts[i]);
                if (b != null) return b;
            }
        }
        return fetchOne(url);
    }

    private static String[] mirrors(String u) {
        if (u == null) return null;
        try {
            String user, repo, ref, rest;
            String p;
            if (u.contains("raw.githubusercontent.com/")) {
                p = u.substring(u.indexOf("raw.githubusercontent.com/") + 26);
                int q = p.indexOf('?');
                if (q > 0) p = p.substring(0, q);
                String[] seg = p.split("/", 4);
                if (seg.length < 4) return null;
                user = seg[0]; repo = seg[1]; ref = seg[2]; rest = seg[3];
            } else if (u.contains("cdn.jsdelivr.net/gh/")) {
                p = u.substring(u.indexOf("cdn.jsdelivr.net/gh/") + 20);
                int q = p.indexOf('?');
                if (q > 0) p = p.substring(0, q);
                String[] seg = p.split("/", 3);
                if (seg.length < 3) return null;
                user = seg[0];
                int at = seg[1].indexOf('@');
                if (at <= 0) return null;
                repo = seg[1].substring(0, at);
                ref = seg[1].substring(at + 1);
                rest = seg[2];
            } else {
                return null;
            }
            String rawPath = user + "/" + repo + "/" + ref + "/" + rest;
            return new String[]{
                    "https://raw.githubusercontent.com/" + rawPath,
                    "https://ghproxy.net/https://raw.githubusercontent.com/" + rawPath,
                    "https://raw.gitmirror.com/" + rawPath,
                    "https://cdn.jsdelivr.net/gh/" + user + "/" + repo + "@" + ref + "/" + rest
            };
        } catch (Throwable ignore) {
        }
        return null;
    }

    private static byte[] fetchOne(String url) {

        HttpURLConnection c = null;
        try {
            c = (HttpURLConnection) new URL(url).openConnection();
            c.setConnectTimeout(8000);
            c.setReadTimeout(15000);
            c.setRequestMethod("GET");
            c.setRequestProperty("User-Agent", "cathelper-update");
            c.setInstanceFollowRedirects(true);
            if (c.getResponseCode() != 200) return null;
            InputStream in = c.getInputStream();
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) {
                bo.write(buf, 0, n);
                if (bo.size() > 6 * 1024 * 1024) break;
            }
            in.close();
            return bo.toByteArray();
        } catch (Throwable t) {
            return null;
        } finally {
            if (c != null) try { c.disconnect(); } catch (Throwable ignore) {}
        }
    }
}
