package com.cathelperawa.owo.app.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;

public class SwitchBox extends View {

    private boolean checked;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

    public SwitchBox(Context c, boolean checked) {
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
        float w = getWidth(), h = getHeight(), r = h / 2f;
        p.setColor(checked ? 0xFF5BD18A : 0x55A9B6C9);
        canvas.drawRoundRect(0, 0, w, h, r, r, p);
        float knobR = r - UiKit.dp(getContext(), 3);
        float cx = checked ? (w - r) : r;
        p.setColor(0xFFFFFFFF);
        canvas.drawCircle(cx, r, knobR, p);
    }
}
