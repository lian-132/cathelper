package com.cathelperawa.owo.app.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

public final class ScriptSwitch extends View {

    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint thumbPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();

    private boolean checked;
    private boolean pressed;

    private final int w;
    private final int h;

    public ScriptSwitch(Context c) {
        super(c);
        float d = c.getResources().getDisplayMetrics().density;
        w = (int) (46 * d);
        h = (int) (26 * d);
        thumbPaint.setColor(Color.WHITE);
        setClickable(true);
        setFocusable(true);
    }

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean v) {
        if (checked != v) {
            checked = v;
            invalidate();
        }
    }

    public void toggle() {
        checked = !checked;
        invalidate();
    }

    @Override protected void onMeasure(int wSpec, int hSpec) {
        setMeasuredDimension(w + getPaddingLeft() + getPaddingRight(),
                h + getPaddingTop() + getPaddingBottom());
    }

    @Override protected void onDraw(Canvas canvas) {
        float d = getResources().getDisplayMetrics().density;
        float radius = h / 2f;
        rect.set(0, 0, w, h);

        trackPaint.setColor(checked ? 0xFF34C759 : 0xFFD8E0EA);
        canvas.drawRoundRect(rect, radius, radius, trackPaint);

        float margin = 3 * d;
        float thumbR = radius - margin;
        float cx = checked ? (w - radius) : radius;

        float r = pressed ? thumbR * 0.92f : thumbR;
        canvas.drawCircle(cx, h / 2f, r, thumbPaint);
    }

    @Override public boolean performClick() {
        toggle();
        return super.performClick();
    }

    @Override public void setPressed(boolean p) {
        if (pressed != p) {
            pressed = p;
            invalidate();
        }
        super.setPressed(p);
    }
}
