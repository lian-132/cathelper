package com.cathelperawa.owo.app.feature;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;

import com.cathelperawa.owo.app.core.HookLog;

public final class MusicCardImage {

    private MusicCardImage() {}

    private static final int W = 720;
    private static final int H = 320;
    private static final int PAD = 28;
    private static final int COVER = 224;

    public static Bitmap render(String title, String artist, String album,
                                String duration, byte[] cover) {
        try {
            Bitmap out = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(out);
            Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

            p.setColor(0xFFF7F3F6);
            c.drawRoundRect(new RectF(0, 0, W, H), 36, 36, p);
            p.setColor(0xFFFFE9F3);
            c.drawRoundRect(new RectF(0, 0, W, H / 2f), 36, 36, p);

            p.setColor(0xFFF7F3F6);
            c.drawRect(new RectF(0, H / 2f - 40, W, H), p);

            float cx = PAD;
            float cy = (H - COVER) / 2f;
            RectF coverRect = new RectF(cx, cy, cx + COVER, cy + COVER);
            p.setColor(0xFFE6DFE4);
            c.drawRoundRect(coverRect, 22, 22, p);
            Bitmap bmp = null;
            if (cover != null && cover.length > 0) {
                try {
                    bmp = BitmapFactory.decodeByteArray(cover, 0, cover.length);
                } catch (Throwable ignore) {
                }
            }
            if (bmp != null) {

                c.save();
                android.graphics.Path clip = new android.graphics.Path();
                clip.addRoundRect(coverRect, 22, 22, android.graphics.Path.Direction.CW);
                c.clipPath(clip);

                float sw = bmp.getWidth(), sh = bmp.getHeight();
                float scale = Math.max(COVER / sw, COVER / sh);
                float dw = sw * scale, dh = sh * scale;
                RectF dst = new RectF(coverRect.left + (COVER - dw) / 2f,
                        coverRect.top + (COVER - dh) / 2f,
                        coverRect.left + (COVER - dw) / 2f + dw,
                        coverRect.top + (COVER - dh) / 2f + dh);
                c.drawBitmap(bmp, null, dst, new Paint(Paint.FILTER_BITMAP_FLAG));
                c.restore();
            }

            float tx = cx + COVER + 26;
            float maxW = W - tx - PAD;

            p.setTypeface(Typeface.DEFAULT_BOLD);
            p.setColor(0xFF2E3A4E);
            p.setTextSize(38);
            float ty = cy + 56;
            ty = drawWrapped(c, title, tx, ty, maxW, p, 2, 46);

            if (artist != null && artist.length() > 0) {
                p.setTypeface(Typeface.DEFAULT);
                p.setColor(0xFF6E93D6);
                p.setTextSize(26);
                ty += 34;
                c.drawText(ellipsize(artist, p, maxW), tx, ty, p);
            }

            StringBuilder sub = new StringBuilder();
            if (album != null && album.length() > 0) sub.append(album);
            if (duration != null && duration.length() > 0) {
                if (sub.length() > 0) sub.append("  ·  ");
                sub.append(duration);
            }
            if (sub.length() > 0) {
                p.setTypeface(Typeface.DEFAULT);
                p.setColor(0xFF9AAABC);
                p.setTextSize(22);
                ty += 32;
                c.drawText(ellipsize(sub.toString(), p, maxW), tx, ty, p);
            }

            p.setColor(0xFFB9C3D0);
            p.setTextSize(20);
            c.drawText("网易云音乐", cx + 4, H - PAD + 2, p);

            return out;
        } catch (Throwable t) {
            HookLog.e("MusicCardImage.render 失败", t);
            return null;
        }
    }

    private static float drawWrapped(Canvas c, String text, float x, float y,
                                     float maxW, Paint p, int maxLines, float lineH) {
        if (text == null || text.length() == 0) return y;
        int start = 0;
        int lines = 0;
        float cy = y;
        while (start < text.length() && lines < maxLines) {
            int count = p.breakText(text, start, text.length(), true, maxW, null);
            if (count <= 0) break;
            String line = text.substring(start, start + count);
            if (start + count < text.length() && lines == maxLines - 1) {
                line = line.substring(0, Math.max(1, line.length() - 1)) + "…";
            }
            c.drawText(line, x, cy, p);
            start += count;
            lines++;
            if (start < text.length()) cy += lineH;
        }
        return cy;
    }

    private static String ellipsize(String s, Paint p, float maxW) {
        if (s == null) return "";
        if (p.measureText(s) <= maxW) return s;
        String cur = s;
        while (cur.length() > 1 && p.measureText(cur + "…") > maxW) {
            cur = cur.substring(0, cur.length() - 1);
        }
        return cur + "…";
    }
}
