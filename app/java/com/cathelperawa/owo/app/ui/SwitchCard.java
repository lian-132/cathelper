package com.cathelperawa.owo.app.ui;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

public final class SwitchCard {

    private SwitchCard() {}

    public interface OnToggled {
        void run(boolean nowOn);
    }

    public static FrameLayout build(final Context c, final String key,
                                    final String title, final String desc,
                                    final boolean defOn, final int heightDp,
                                    final String artKey, final OnToggled onToggled) {
        FrameLayout card = UiKit.whiteFrame(c, UiKit.dp(c, 18), 0x8CFFFFFF);
        card.setClipToOutline(true);

        if (artKey != null) {
            android.graphics.Bitmap cb = Images.get(artKey);
            if (cb != null) {
                android.widget.ImageView iv = new android.widget.ImageView(c);
                iv.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);
                iv.setImageBitmap(cb);
                card.addView(iv, new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT, UiKit.dp(c, 72)));
                View sh = new View(c);
                sh.setBackgroundDrawable(new GradientDrawable(
                        GradientDrawable.Orientation.LEFT_RIGHT,
                        new int[]{0xF7FFFFFF, 0xA6FFFFFF, 0x33FFFFFF}));
                card.addView(sh, new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT, UiKit.dp(c, 72)));
            }
        }

        LinearLayout row = new LinearLayout(c);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(UiKit.dp(c, 14), 0, UiKit.dp(c, 14), 0);
        LinearLayout texts = new LinearLayout(c);
        texts.setOrientation(LinearLayout.VERTICAL);
        texts.addView(UiKit.text(c, title, 15, 0xFF43587A, true, UiKit.dp(c, 2)));
        texts.addView(UiKit.text(c, desc, 11, 0xFF8496AE, false, 0));
        row.addView(texts, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        final SwitchBox box = new SwitchBox(c, UiConfig.get(c, key, defOn));
        row.addView(box, new LinearLayout.LayoutParams(UiKit.dp(c, 52), UiKit.dp(c, 30)));

        int h = heightDp > 0 ? UiKit.dp(c, heightDp) : LinearLayout.LayoutParams.WRAP_CONTENT;
        card.addView(row, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, h));

        card.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                boolean now = !UiConfig.get(c, key, defOn);
                UiConfig.set(c, key, now);
                box.setChecked(now);
                UiKit.press(v);
                UiKit.catToast(c, now ? (title + " 开启喵～") : (title + " 关闭喵～"));
                if (onToggled != null) onToggled.run(now);
            }
        });
        return card;
    }

    public static FrameLayout build(Context c, String key, String title,
                                    String desc, boolean defOn, int heightDp) {
        return build(c, key, title, desc, defOn, heightDp, null, null);
    }

    public static SwitchBox findBox(View card) {
        if (card instanceof SwitchBox) return (SwitchBox) card;
        if (card instanceof android.view.ViewGroup) {
            android.view.ViewGroup g = (android.view.ViewGroup) card;
            for (int i = 0; i < g.getChildCount(); i++) {
                SwitchBox b = findBox(g.getChildAt(i));
                if (b != null) return b;
            }
        }
        return null;
    }
}
