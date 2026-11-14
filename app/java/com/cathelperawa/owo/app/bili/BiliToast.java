package com.cathelperawa.owo.app.bili;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

public final class BiliToast {

    private BiliToast() {}

    private static View current;

    public static void show(ViewGroup parent, String text) {
        show(parent, 0, text);
    }

    public static void show(ViewGroup parent, int glyph, String text) {
        if (parent == null) return;
        try {
            if (current != null && current.getParent() instanceof ViewGroup) {
                ((ViewGroup) current.getParent()).removeView(current);
            }

            Context c = parent.getContext();
            LinearLayout pill = new LinearLayout(c);
            pill.setOrientation(LinearLayout.HORIZONTAL);
            pill.setGravity(Gravity.CENTER_VERTICAL);
            pill.setPadding(BiliUi.dp(c, 16), BiliUi.dp(c, 10),
                    BiliUi.dp(c, 16), BiliUi.dp(c, 10));

            GradientDrawable g = new GradientDrawable();
            g.setColor(0xE60B0F16);
            g.setCornerRadius(BiliUi.dp(c, 22));
            g.setStroke(Math.max(1, BiliUi.dp(c, 0.8f)), 0x33FFFFFF);
            pill.setBackground(g);

            if (glyph != 0) {
                pill.addView(BiliUi.icon(c, glyph, 16, BiliUi.MAIN));
                TextView t = BiliUi.text(c, text, 12.5f, BiliUi.MAIN, false);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                lp.leftMargin = BiliUi.dp(c, 8);
                pill.addView(t, lp);
            } else {
                pill.addView(BiliUi.text(c, text, 12.5f, BiliUi.MAIN, false));
            }

            FrameLayout.LayoutParams flp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM);
            flp.bottomMargin = BiliUi.dp(c, 92);
            parent.addView(pill, flp);
            current = pill;

            pill.setAlpha(0f);
            pill.animate().alpha(1f).setDuration(120).start();
            pill.postDelayed(new Runnable() {
                @Override public void run() {
                    pill.animate().alpha(0f).setDuration(220)
                            .withEndAction(new Runnable() {
                                @Override public void run() {
                                    try {
                                        ViewGroup vg = (ViewGroup) pill.getParent();
                                        if (vg != null) vg.removeView(pill);
                                        if (current == pill) current = null;
                                    } catch (Throwable ignore) {
                                    }
                                }
                            }).start();
                }
            }, 1200);
        } catch (Throwable ignore) {
        }
    }
}
