package com.cathelperawa.owo.app.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;

public class RefreshIcon extends View {

    private final Paint arcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF oval = new RectF();
    private final Path arrow = new Path();

    private final float density;

    public RefreshIcon(Context c) {
        super(c);
        density = c.getResources().getDisplayMetrics().density;
        arcPaint.setStyle(Paint.Style.STROKE);
        arcPaint.setStrokeWidth(2.4f * density);
        arcPaint.setStrokeCap(Paint.Cap.ROUND);
        arcPaint.setColor(0xFF9FC0F5);

        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setColor(0xFF9FC0F5);
    }

    public void setIconColor(int color) {
        arcPaint.setColor(color);
        fillPaint.setColor(color);
        invalidate();
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int w = resolveSize((int) (20 * density), widthSpec);
        int h = resolveSize((int) (20 * density), heightSpec);
        setMeasuredDimension(w, h);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth();
        float h = getHeight();
        float cx = w / 2f;
        float cy = h / 2f;
        float stroke = arcPaint.getStrokeWidth();

        float r = Math.min(w, h) / 2f - stroke / 2f - 2f * density;
        if (r <= 0) return;

        oval.set(cx - r, cy - r, cx + r, cy + r);

        final float start = 120f;
        final float sweep = 270f;
        canvas.drawArc(oval, start, sweep, false, arcPaint);

        double rad = Math.toRadians(start);
        float ex = cx + (float) (Math.cos(rad) * r);
        float ey = cy + (float) (Math.sin(rad) * r);

        float tx = (float) -Math.sin(rad);
        float ty = (float) Math.cos(rad);

        float nx = (float) Math.cos(rad);
        float ny = (float) Math.sin(rad);

        float len = 5.2f * density;
        float wid = 3.4f * density;

        arrow.reset();
        arrow.moveTo(ex + tx * len, ey + ty * len);
        arrow.lineTo(ex + nx * wid, ey + ny * wid);
        arrow.lineTo(ex - nx * wid, ey - ny * wid);
        arrow.close();
        canvas.drawPath(arrow, fillPaint);
    }
}
