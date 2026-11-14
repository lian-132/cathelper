package com.cathelperawa.owo.app.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.MotionEvent;
import android.view.View;

public class SkinSeek extends View {

    public interface OnChange {
        void onChanged(int value);
        void onFinished(int value);
    }

    private static final int[] RAINBOW = {
            0xFFFF4D4D, 0xFFFFB84D, 0xFFFFF04D, 0xFF4DD964,
            0xFF4DD9E8, 0xFF4D7BFF, 0xFFB04DFF, 0xFFFF4DD9
    };

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();

    private int max = 100;
    private int progress = 0;
    private boolean rainbow = false;

    private int lineColor = 0xFFF07AAE;
    private int trackColor = 0x2E7F90A6;

    private float lineH;
    private float thumbR;
    private int viewH;

    private OnChange onChange;

    public SkinSeek(Context c) {
        super(c);
        float d = c.getResources().getDisplayMetrics().density;
        lineH = Math.max(2f, 3f * d);
        thumbR = 7.5f * d;
        viewH = (int) (30 * d);
        setClickable(true);
    }

    public void setRange(int max) {
        this.max = Math.max(1, max);
        invalidate();
    }

    public void setValue(int v) {
        this.progress = Math.max(0, Math.min(max, v));
        invalidate();
    }

    public int getValue() {
        return progress;
    }

    public void setRainbow(boolean on) {
        rainbow = on;
        invalidate();
    }

    public void setLineColor(int color) {
        lineColor = color;
        invalidate();
    }

    public void setOnChange(OnChange l) {
        onChange = l;
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int w = resolveSize(getSuggestedMinimumWidth(), widthSpec);
        int h = resolveSize(viewH, heightSpec);
        setMeasuredDimension(w, h);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float cy = getHeight() / 2f;
        float left = thumbR;
        float right = getWidth() - thumbR;
        if (right <= left) return;
        float half = lineH / 2f;

        paint.setShader(null);
        if (rainbow) {
            paint.setShader(new LinearGradient(left, cy, right, cy, RAINBOW, null,
                    Shader.TileMode.CLAMP));
            rect.set(left, cy - half, right, cy + half);
            canvas.drawRoundRect(rect, half, half, paint);
            paint.setShader(null);
        } else {
            paint.setColor(trackColor);
            rect.set(left, cy - half, right, cy + half);
            canvas.drawRoundRect(rect, half, half, paint);

            float x = left + (right - left) * progress / (float) max;
            paint.setColor(lineColor);
            rect.set(left, cy - half, Math.max(left, x), cy + half);
            canvas.drawRoundRect(rect, half, half, paint);
        }

        float x = left + (right - left) * progress / (float) max;
        paint.setColor(rainbow ? 0xFFF07AAE : lineColor);
        canvas.drawCircle(x, cy, thumbR, paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE: {
                float left = thumbR;
                float right = getWidth() - thumbR;
                if (right <= left) return true;
                float ratio = (ev.getX() - left) / (right - left);
                ratio = Math.max(0f, Math.min(1f, ratio));
                int v = Math.round(ratio * max);
                if (v != progress) {
                    progress = v;
                    invalidate();
                    if (onChange != null) onChange.onChanged(progress);
                }
                getParent().requestDisallowInterceptTouchEvent(true);
                return true;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (onChange != null) onChange.onFinished(progress);
                return true;
            default:
                return super.onTouchEvent(ev);
        }
    }
}
