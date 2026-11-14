package com.cathelperawa.owo.app.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;

public class CheckDot extends View {

    private boolean checked;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

    public CheckDot(Context c, boolean checked) {
        super(c);
        this.checked = checked;
    }

    public void setChecked(boolean v) {
        checked = v;
        invalidate();
    }

    public boolean isChecked() { return checked; }

    @Override
    protected void onDraw(Canvas canvas) {
        float s = Math.min(getWidth(), getHeight());
        float r = s / 2f - UiKit.dp(getContext(), 2);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(UiKit.dp(getContext(), 2));
        p.setColor(checked ? 0xFF7FA5E6 : 0xFFC3D0E0);
        canvas.drawCircle(s / 2f, s / 2f, r, p);
        if (checked) {
            p.setStyle(Paint.Style.FILL);
            p.setColor(0xFF7FA5E6);
            canvas.drawCircle(s / 2f, s / 2f, r - UiKit.dp(getContext(), 4), p);
        }
    }
}
