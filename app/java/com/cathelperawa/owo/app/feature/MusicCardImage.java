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

    private MusicCardImage() { }

    private static final int W = 720;
    private static final int H = 320;
    private static final int PAD = 28;
    private static final int COVER = 224;

    public static Bitmap render(String title, String artist, String album,
                                String duration, byte[] cover) { return null; }

    private static float drawWrapped(Canvas c, String text, float x, float y,
                                     float maxW, Paint p, int maxLines, float lineH) { return 0; }

    private static String ellipsize(String s, Paint p, float maxW) { return null; }
}
