package com.cathelperawa.owo.app.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;

public final class CatIcon extends View {

    public static final int SEARCH = 1;
    public static final int USER = 2;
    public static final int HOME = 3;
    public static final int DYNAMIC = 4;
    public static final int MORE = 5;
    public static final int PLAY = 6;
    public static final int PAUSE = 7;
    public static final int BACK = 8;
    public static final int THUMB = 9;
    public static final int COIN = 10;
    public static final int STAR = 11;
    public static final int SHARE = 12;
    public static final int DOWNLOAD = 13;
    public static final int HISTORY = 14;
    public static final int SUBSCRIBE = 15;
    public static final int LATER = 16;
    public static final int SETTINGS = 17;
    public static final int MOON = 18;
    public static final int MSG = 19;
    public static final int FULLSCREEN = 20;
    public static final int DANMAKU = 21;
    public static final int CHEVRON = 22;
    public static final int CLOSE = 23;
    public static final int REFRESH = 24;
    public static final int EYE = 25;
    public static final int COMMENT = 26;

    private final float density;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final RectF rect = new RectF();

    private int glyph;
    private int color = 0xFF9AA9BC;
    private int pxSize;
    private float strokeWidth = 2f;

    private boolean filled = false;

    public CatIcon(Context c, int glyph, int dpSize, int color) {
        super(c);
        this.density = c.getResources().getDisplayMetrics().density;
        this.glyph = glyph;
        this.pxSize = Math.max(1, (int) (dpSize * density + 0.5f));
        this.color = color;
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
    }

    public CatIcon setGlyph(int g) {
        if (glyph != g) {
            glyph = g;
            invalidate();
        }
        return this;
    }

    public CatIcon setSizeDp(int dp) {
        int px = Math.max(1, (int) (dp * density + 0.5f));
        if (px != pxSize) {
            pxSize = px;
            requestLayout();
            invalidate();
        }
        return this;
    }

    public CatIcon setIconColor(int c) {
        if (color != c) {
            color = c;
            invalidate();
        }
        return this;
    }

    public CatIcon setStrokeWidth(float w) {
        strokeWidth = w;
        invalidate();
        return this;
    }

    private int pillColor = 0;
    private float pillPad = 0f;

    public CatIcon setPill(int color, float padDp) {
        pillColor = color;
        pillPad = padDp * density;
        invalidate();
        return this;
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int pad = (int) (pillPad * 2);
        setMeasuredDimension(resolveSize(pxSize + pad, widthSpec),
                resolveSize(pxSize + pad, heightSpec));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getWidth();
        int h = getHeight();

        if (pillColor != 0) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(pillColor);
            rect.set(0, 0, w, h);
            canvas.drawRoundRect(rect, h / 2f, h / 2f, paint);
        }

        int side = Math.min(w, h) - (int) (pillPad * 2);
        if (side <= 0) return;
        float scale = side / 24f;
        float offX = (w - side) / 2f;
        float offY = (h - side) / 2f;

        canvas.save();
        canvas.translate(offX, offY);
        canvas.scale(scale, scale);

        path.reset();
        filled = false;
        build(glyph);

        paint.setColor(color);
        if (filled) {
            paint.setStyle(Paint.Style.FILL);
            paint.setStrokeWidth(0);
            canvas.drawPath(path, paint);
        } else {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(strokeWidth);
            canvas.drawPath(path, paint);

            drawExtra(canvas);
        }
        canvas.restore();
    }

    private void line(float x1, float y1, float x2, float y2) {
        path.moveTo(x1, y1);
        path.lineTo(x2, y2);
    }

    private void roundRect(float l, float t, float r, float b, float rad) {
        rect.set(l, t, r, b);
        path.addRoundRect(rect, rad, rad, Path.Direction.CW);
    }

    private void build(int g) {
        switch (g) {
            case SEARCH:
                path.addCircle(10.6f, 10.6f, 6.4f, Path.Direction.CW);
                line(15.4f, 15.4f, 20.4f, 20.4f);
                break;

            case USER:
                path.addCircle(12f, 8.2f, 3.7f, Path.Direction.CW);
                path.moveTo(4.8f, 20.2f);
                path.cubicTo(4.8f, 16.6f, 8f, 14.7f, 12f, 14.7f);
                path.cubicTo(16f, 14.7f, 19.2f, 16.6f, 19.2f, 20.2f);
                break;

            case HOME:
                line(3f, 11f, 12f, 4f);
                line(12f, 4f, 21f, 11f);
                path.moveTo(5.2f, 10f);
                line(5.2f, 10f, 5.2f, 19.6f);
                line(5.2f, 19.6f, 18.8f, 19.6f);
                line(18.8f, 19.6f, 18.8f, 10f);
                path.moveTo(10f, 19.6f);
                line(10f, 19.6f, 10f, 14.4f);
                line(10f, 14.4f, 14f, 14.4f);
                line(14f, 14.4f, 14f, 19.6f);
                break;

            case DYNAMIC:
                path.addCircle(12f, 12f, 8.2f, Path.Direction.CW);
                line(12f, 7.4f, 12f, 12f);
                line(12f, 12f, 15.2f, 13.8f);
                break;

            case MORE:

                break;

            case PLAY:
                path.moveTo(8.4f, 5.6f);
                line(8.4f, 5.6f, 18.6f, 12f);
                line(18.6f, 12f, 8.4f, 18.4f);
                path.close();
                filled = true;
                break;

            case PAUSE:
                roundRect(7f, 5f, 10.2f, 19f, 1.2f);
                roundRect(13.8f, 5f, 17f, 19f, 1.2f);
                filled = true;
                break;

            case BACK:
                line(15f, 5f, 8f, 12f);
                line(8f, 12f, 15f, 19f);
                break;

            case CHEVRON:
                line(9.5f, 5.5f, 16f, 12f);
                line(16f, 12f, 9.5f, 18.5f);
                break;

            case CLOSE:
                line(6f, 6f, 18f, 18f);
                line(18f, 6f, 6f, 18f);
                break;

            case THUMB:

                path.moveTo(7f, 10.6f);
                line(7f, 10.6f, 10.4f, 4.4f);
                path.cubicTo(12.4f, 4.4f, 13.2f, 6f, 12.9f, 7.8f);
                path.cubicTo(12.8f, 8.5f, 12.7f, 9f, 12.7f, 9.4f);
                line(12.7f, 9.4f, 17.4f, 9.4f);
                path.cubicTo(18.9f, 9.4f, 19.8f, 10.7f, 19.4f, 12.1f);
                line(19.4f, 12.1f, 18.2f, 17.4f);
                path.cubicTo(18f, 18.3f, 17.2f, 19f, 16.2f, 19f);
                line(16.2f, 19f, 7f, 19f);
                line(7f, 19f, 7f, 10.6f);
                path.moveTo(7f, 10.6f);
                line(7f, 10.6f, 4.2f, 10.6f);
                line(4.2f, 10.6f, 4.2f, 19.6f);
                line(4.2f, 19.6f, 7f, 19.6f);
                break;

            case COIN:
                path.addCircle(12f, 12f, 8.2f, Path.Direction.CW);
                line(9.3f, 9.2f, 12f, 13.9f);
                line(12f, 13.9f, 14.7f, 9.2f);
                line(9.6f, 12.2f, 14.4f, 12.2f);
                line(8.8f, 15.2f, 15.2f, 15.2f);
                break;

            case STAR:
                starPath(12f, 12.4f, 8.3f, 3.9f, 5);
                break;

            case SHARE:
                path.addCircle(17.6f, 6.2f, 2.6f, Path.Direction.CW);
                path.addCircle(6.4f, 12f, 2.6f, Path.Direction.CW);
                path.addCircle(17.6f, 17.8f, 2.6f, Path.Direction.CW);
                line(9f, 10.7f, 15f, 7.5f);
                line(9f, 13.3f, 15f, 16.5f);
                break;

            case DOWNLOAD:
                path.moveTo(4f, 7.4f);
                line(4f, 7.4f, 9f, 7.4f);
                line(9f, 7.4f, 10.4f, 9.4f);
                line(10.4f, 9.4f, 20f, 9.4f);
                line(20f, 9.4f, 20f, 18.4f);
                line(20f, 18.4f, 4f, 18.4f);
                path.close();
                line(12f, 11.4f, 12f, 16.4f);
                line(9.8f, 14.3f, 12f, 16.5f);
                line(14.2f, 14.3f, 12f, 16.5f);
                break;

            case HISTORY:

                path.addArc(3.8f, 3.8f, 20.2f, 20.2f, -62f, -250f);
                line(4.6f, 4.4f, 4.4f, 8.2f);
                line(4.4f, 8.2f, 8.2f, 8.2f);
                line(12f, 8.6f, 12f, 12f);
                line(12f, 12f, 14.6f, 13.6f);
                break;

            case SUBSCRIBE:
                roundRect(3.6f, 5.4f, 20.4f, 18.4f, 2.6f);
                line(3.6f, 8.8f, 20.4f, 8.8f);
                path.moveTo(10.2f, 11.4f);
                line(10.2f, 11.4f, 15.2f, 13.9f);
                line(15.2f, 13.9f, 10.2f, 16.4f);
                path.close();
                break;

            case LATER:
                path.addCircle(12f, 12f, 8.2f, Path.Direction.CW);
                line(12f, 7.6f, 12f, 12f);
                line(12f, 12f, 15f, 13.8f);
                break;

            case SETTINGS:
                path.addCircle(12f, 12f, 3.1f, Path.Direction.CW);
                gearPath();
                break;

            case MOON:

                path.moveTo(19.4f, 14.6f);
                path.cubicTo(18.2f, 18.4f, 14.4f, 20.6f, 10.4f, 19.4f);
                path.cubicTo(6.4f, 18.2f, 4.2f, 14.4f, 5.4f, 10.4f);
                path.cubicTo(6.6f, 6.4f, 10.4f, 4.2f, 14.4f, 5.4f);
                path.cubicTo(11.6f, 7.2f, 10.4f, 10.6f, 11.4f, 13.6f);
                path.cubicTo(12.2f, 16.2f, 15.2f, 17.6f, 19.4f, 14.6f);
                break;

            case MSG:
                path.moveTo(4f, 5.4f);
                line(4f, 5.4f, 20f, 5.4f);
                line(20f, 5.4f, 20f, 16.2f);
                line(20f, 16.2f, 9.4f, 16.2f);
                line(9.4f, 16.2f, 5.4f, 19.8f);
                line(5.4f, 19.8f, 5.4f, 16.2f);
                line(5.4f, 16.2f, 4f, 16.2f);
                path.close();
                line(8f, 9.4f, 16f, 9.4f);
                line(8f, 12.4f, 13f, 12.4f);
                break;

            case COMMENT:
                roundRect(3.6f, 4.6f, 20.4f, 15.4f, 3.2f);
                line(7.4f, 15.4f, 7.4f, 20.2f);
                line(7.4f, 20.2f, 11.6f, 15.4f);
                break;

            case FULLSCREEN:
                line(4f, 9.4f, 4f, 4.8f);
                line(4f, 4.8f, 8.8f, 4.8f);
                line(20f, 9.4f, 20f, 4.8f);
                line(20f, 4.8f, 15.2f, 4.8f);
                line(4f, 14.6f, 4f, 19.2f);
                line(4f, 19.2f, 8.8f, 19.2f);
                line(20f, 14.6f, 20f, 19.2f);
                line(20f, 19.2f, 15.2f, 19.2f);
                break;

            case DANMAKU:
                roundRect(3.6f, 5.2f, 20.4f, 17.2f, 3f);
                line(7.4f, 9.4f, 13.4f, 9.4f);
                line(9.6f, 13f, 16.6f, 13f);
                break;

            case REFRESH:
                path.addArc(4.2f, 4.2f, 19.8f, 19.8f, -48f, 270f);
                line(17.3f, 3.9f, 17.2f, 7.9f);
                line(17.2f, 7.9f, 13.3f, 7.7f);
                break;

            case EYE:
                path.moveTo(3.4f, 12f);
                path.cubicTo(6.2f, 7.2f, 9f, 5.4f, 12f, 5.4f);
                path.cubicTo(15f, 5.4f, 17.8f, 7.2f, 20.6f, 12f);
                path.cubicTo(17.8f, 16.8f, 15f, 18.6f, 12f, 18.6f);
                path.cubicTo(9f, 18.6f, 6.2f, 16.8f, 3.4f, 12f);
                path.close();
                path.addCircle(12f, 12f, 2.9f, Path.Direction.CW);
                break;

            default:
                path.addCircle(12f, 12f, 8f, Path.Direction.CW);
        }
    }

    private void gearPath() {
        final int teeth = 8;
        final float cx = 12f, cy = 12f;
        final float rOut = 8.6f, rIn = 6.6f;
        for (int i = 0; i < teeth; i++) {
            float a0 = (float) (i * 2 * Math.PI / teeth - Math.PI / 2);
            float a1 = a0 + (float) (Math.PI / teeth * 0.62);
            float a2 = a0 + (float) (Math.PI / teeth * 1.38);
            float a3 = a0 + (float) (Math.PI / teeth * 2.0);
            if (i == 0) {
                path.moveTo(cx + rIn * (float) Math.cos(a0), cy + rIn * (float) Math.sin(a0));
            } else {
                path.lineTo(cx + rIn * (float) Math.cos(a0), cy + rIn * (float) Math.sin(a0));
            }
            path.lineTo(cx + rOut * (float) Math.cos(a0), cy + rOut * (float) Math.sin(a0));
            path.lineTo(cx + rOut * (float) Math.cos(a1), cy + rOut * (float) Math.sin(a1));
            path.lineTo(cx + rIn * (float) Math.cos(a2), cy + rIn * (float) Math.sin(a2));
            path.lineTo(cx + rIn * (float) Math.cos(a3), cy + rIn * (float) Math.sin(a3));
        }
        path.close();
    }

    private void starPath(float cx, float cy, float rOut, float rIn, int points) {
        for (int i = 0; i < points * 2; i++) {
            double ang = -Math.PI / 2 + i * Math.PI / points;
            float r = (i % 2 == 0) ? rOut : rIn;
            float x = cx + r * (float) Math.cos(ang);
            float y = cy + r * (float) Math.sin(ang);
            if (i == 0) path.moveTo(x, y);
            else path.lineTo(x, y);
        }
        path.close();
    }

    private void drawExtra(Canvas canvas) {
        if (glyph != MORE) return;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        canvas.drawCircle(12f, 5.4f, 1.5f, paint);
        canvas.drawCircle(12f, 12f, 1.5f, paint);
        canvas.drawCircle(12f, 18.6f, 1.5f, paint);
    }
}
