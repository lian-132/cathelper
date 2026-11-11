package com.cathelperawa.owo.app.bili;

import android.animation.ValueAnimator;
import android.content.Context;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.cathelperawa.owo.app.ui.CatIcon;

public final class BiliRefreshView extends LinearLayout {

    private final android.view.View iconView;
    private final TextView label;
    private int trigger = 200;
    private ValueAnimator spin;

    public BiliRefreshView(Context c) {
        super(c);
        setOrientation(VERTICAL);
        setGravity(Gravity.CENTER);

        int resId = c.getResources().getIdentifier(
                "bili_refresh_cat", "drawable", c.getPackageName());
        if (resId != 0) {
            ImageView iv = new ImageView(c);
            iv.setImageResource(resId);
            iconView = iv;
        } else {
            
            iconView = BiliUi.icon(c, CatIcon.REFRESH, 28, BiliUi.ACCENT);
        }
        int size = BiliUi.dp(c, 34);
        addView(iconView, new LinearLayout.LayoutParams(size, size));

        label = BiliUi.text(c, "下拉刷新", 10.5f, BiliUi.SUB, false);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = BiliUi.dp(c, 4);
        addView(label, lp);

        setHeight(0);
    }

    public void setTrigger(int px) {
        trigger = Math.max(1, px);
    }

    public void setPull(int dy) {
        stopSpin();
        setHeight(dy);
        float p = Math.min(1f, dy / (float) trigger);
        iconView.setRotation(p * 360f);
        label.setText(p >= 1f ? "松开刷新" : "下拉刷新");
    }

    public void startSpin() {
        setHeight(trigger);
        label.setText("刷新中…");
        stopSpin();
        spin = ValueAnimator.ofFloat(0f, 360f);
        spin.setDuration(900);
        spin.setRepeatCount(ValueAnimator.INFINITE);
        spin.setInterpolator(new LinearInterpolator());
        spin.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override public void onAnimationUpdate(ValueAnimator a) {
                iconView.setRotation((Float) a.getAnimatedValue());
            }
        });
        spin.start();
    }

    public void stop() {
        stopSpin();
        iconView.setRotation(0f);
        label.setText("下拉刷新");
        setHeight(0);
    }

    private void stopSpin() {
        if (spin != null) {
            spin.cancel();
            spin = null;
        }
    }

    private void setHeight(int px) {
        ViewGroup.LayoutParams lp = getLayoutParams();
        if (lp == null) return;
        if (lp.height != px) {
            lp.height = Math.max(0, px);
            setLayoutParams(lp);
        }
    }
}
