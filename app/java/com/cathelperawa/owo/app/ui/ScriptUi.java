package com.cathelperawa.owo.app.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public final class ScriptUi {

    private final Context c;
    private final float density;

    public ScriptUi(Context c) {
        this.c = c;
        this.density = c.getResources().getDisplayMetrics().density;
    }

    public Context context() { return c; }

    public int dp(float v) { return (int) (v * density + 0.5f); }

    public int color(String hex, int def) {
        try {
            if (hex == null || hex.length() == 0) return def;
            return Color.parseColor(hex.startsWith("#") ? hex : "#" + hex);
        } catch (Throwable t) {
            return def;
        }
    }

    public LinearLayout page(View content, String bgHex) {
        LinearLayout wrap = new LinearLayout(c);
        wrap.setOrientation(LinearLayout.VERTICAL);
        wrap.setBackgroundColor(color(bgHex, 0xFFF4F6FB));
        wrap.setPadding(dp(15), dp(44), dp(15), dp(26));
        if (content != null) {
            wrap.addView(content, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        }
        return wrap;
    }

    public LinearLayout topBar(String title, String titleColorHex) {
        LinearLayout r = row();
        TextView back = text("‹", 30, "#7FA5E6", true);
        back.setPadding(dp(4), 0, dp(14), 0);
        back.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                try {
                    if (c instanceof android.app.Activity) ((android.app.Activity) c).finish();
                } catch (Throwable ignore) {
                }
            }
        });
        r.addView(back);
        r.addView(text(title, 19, titleColorHex, true));
        return r;
    }

    public LinearLayout column() {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    public LinearLayout row() {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    public ScrollView scroll(View content) {
        ScrollView sv = new ScrollView(c);
        sv.setFillViewport(true);
        sv.setOverScrollMode(View.OVER_SCROLL_NEVER);
        sv.setVerticalScrollBarEnabled(false);
        if (content != null) {
            sv.addView(content, new ScrollView.LayoutParams(
                    ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.WRAP_CONTENT));
        }
        return sv;
    }

    public LinearLayout card(int radiusDp, String bgHex) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable g = new GradientDrawable();
        g.setColor(color(bgHex, 0xEEFFFFFF));
        g.setCornerRadius(dp(radiusDp));
        l.setBackgroundDrawable(g);
        l.setPadding(dp(16), dp(14), dp(16), dp(14));
        l.setElevation(dp(3));
        return l;
    }

    public LinearLayout gridCell(LinearLayout content) {
        content.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        return content;
    }

    public TextView text(String s, float size, String colorHex, boolean bold) {
        TextView t = new TextView(c);
        t.setText(s == null ? "" : s);
        t.setTextSize(size);
        t.setTextColor(color(colorHex, 0xFF2B3A55));
        if (bold) t.setTypeface(null, android.graphics.Typeface.BOLD);
        return t;
    }

    public ImageView image(String path, int heightDp) {
        ImageView iv = new ImageView(c);
        iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
        try {
            Bitmap b = BitmapFactory.decodeFile(path);
            if (b != null) iv.setImageBitmap(b);
        } catch (Throwable ignore) {
        }
        int h = heightDp > 0 ? dp(heightDp) : LinearLayout.LayoutParams.WRAP_CONTENT;
        iv.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, h));
        return iv;
    }

    public ImageView roundImage(String path, int heightDp, int radiusDp) {
        ImageView iv = image(path, heightDp);
        try {
            GradientDrawable g = new GradientDrawable();
            g.setColor(0xFF222233);
            g.setCornerRadius(dp(radiusDp));
            iv.setBackgroundDrawable(g);
            iv.setClipToOutline(true);
        } catch (Throwable ignore) {
        }
        return iv;
    }

    public TextView button(String label, String bgHex, String fgHex) {
        return button(label, bgHex, fgHex, 14);
    }

    public TextView button(String label, String bgHex, String fgHex, int radiusDp) {
        TextView b = text(label, 14, fgHex, true);
        b.setGravity(Gravity.CENTER);
        GradientDrawable g = new GradientDrawable();
        g.setColor(color(bgHex, 0xFF7FA5E6));
        g.setCornerRadius(dp(radiusDp));
        b.setBackgroundDrawable(g);
        b.setPadding(dp(14), dp(9), dp(14), dp(9));
        return b;
    }

    public View bg(View v, int radiusDp, String bgHex, String strokeHex, int strokeDp) {
        if (v == null) return null;
        GradientDrawable g = new GradientDrawable();
        g.setColor(color(bgHex, 0xFFFFFFFF));
        g.setCornerRadius(dp(radiusDp));
        if (strokeHex != null && strokeHex.length() > 0) {
            g.setStroke(dp(strokeDp <= 0 ? 1 : strokeDp), color(strokeHex, 0xFFE6ECF5));
        }
        v.setBackgroundDrawable(g);
        return v;
    }

    public TextView chip(String label, String bgHex, String fgHex) {
        TextView t = text(label, 11, fgHex, true);
        GradientDrawable g = new GradientDrawable();
        g.setColor(color(bgHex, 0x22000000));
        g.setCornerRadius(dp(10));
        t.setBackgroundDrawable(g);
        t.setPadding(dp(9), dp(3), dp(9), dp(3));
        return t;
    }

    public android.widget.EditText input(String hint, String def) {
        android.widget.EditText e = new android.widget.EditText(c);
        e.setHint(hint == null ? "" : hint);
        if (def != null) e.setText(def);
        e.setTextSize(14f);
        e.setTextColor(0xFF2B3A55);
        e.setHintTextColor(0xFF9AA9BC);
        e.setBackgroundColor(0x00000000);
        e.setPadding(dp(12), dp(10), dp(12), dp(10));
        GradientDrawable g = new GradientDrawable();
        g.setColor(0xFFFFFFFF);
        g.setCornerRadius(dp(12));
        g.setStroke(dp(1), 0xFFE4ECF7);
        e.setBackgroundDrawable(g);
        return e;
    }

    public LinearLayout switchRow(String label, boolean on) {
        LinearLayout row = new LinearLayout(c);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setPadding(dp(4), dp(10), dp(4), dp(10));

        TextView t = text(label, 14, "#2B3A55", false);
        row.addView(t, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        ScriptSwitch sw = new ScriptSwitch(c);
        sw.setChecked(on);
        sw.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                ((ScriptSwitch) v).toggle();
            }
        });
        row.addView(sw);
        return row;
    }

    public ScriptSwitch switchOf(LinearLayout row) {
        if (row == null) return null;
        for (int i = 0; i < row.getChildCount(); i++) {
            View v = row.getChildAt(i);
            if (v instanceof ScriptSwitch) return (ScriptSwitch) v;
        }
        return null;
    }

    public LinearLayout listItem(String title, String sub, View.OnClickListener onClick) {
        LinearLayout item = new LinearLayout(c);
        item.setOrientation(LinearLayout.HORIZONTAL);
        item.setGravity(android.view.Gravity.CENTER_VERTICAL);
        item.setPadding(dp(14), dp(12), dp(14), dp(12));
        GradientDrawable g = new GradientDrawable();
        g.setColor(0xFFFFFFFF);
        g.setCornerRadius(dp(14));
        item.setBackgroundDrawable(g);

        LinearLayout left = new LinearLayout(c);
        left.setOrientation(LinearLayout.VERTICAL);
        left.addView(text(title, 15, "#2B3A55", true));
        if (sub != null && sub.length() > 0) {
            left.addView(text(sub, 11, "#8496AE", false));
        }
        item.addView(left, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        item.addView(text("›", 20, "#B0BDD1", true));
        if (onClick != null) item.setOnClickListener(onClick);
        return item;
    }

    public TextView badge(String label, String bgHex, String fgHex) {
        TextView t = text(label, 9.5f, fgHex == null ? "#FFFFFF" : fgHex, true);
        GradientDrawable g = new GradientDrawable();
        g.setColor(color(bgHex, 0xFF2E7DD1));
        g.setCornerRadius(dp(6));
        t.setBackgroundDrawable(g);
        t.setPadding(dp(6), dp(1), dp(6), dp(1));
        return t;
    }

    public ImageView avatar(String path, int sizeDp) {
        ImageView iv = image(path, sizeDp);
        iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
        GradientDrawable g = new GradientDrawable();
        g.setColor(0xFFE4ECF7);
        g.setCornerRadius(dp(10));
        iv.setBackgroundDrawable(g);
        iv.setClipToOutline(true);
        return iv;
    }

    public View space(int dp) {
        View v = new View(c);
        v.setLayoutParams(new LinearLayout.LayoutParams(1, dp(dp)));
        return v;
    }

    public View divider() {
        View v = new View(c);
        v.setBackgroundColor(0x14000000);
        v.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, Math.max(1, dp(0.6f))));
        return v;
    }

    public View gap(int dp) {
        View v = new View(c);
        v.setLayoutParams(new LinearLayout.LayoutParams(dp(dp), 1));
        return v;
    }

    public View pad(View v, int left, int top, int right, int bottom) {
        if (v == null) return null;
        v.setPadding(dp(left), dp(top), dp(right), dp(bottom));
        return v;
    }

    public View alpha(View v, int percent) {
        if (v == null) return null;
        int p = percent < 0 ? 0 : (percent > 100 ? 100 : percent);
        v.setAlpha(p / 100f);
        return v;
    }

    public int tint(String hex, int percent) {
        int p = percent < 0 ? 0 : (percent > 100 ? 100 : percent);
        int rgb = color(hex, 0xFFFFFFFF) & 0x00FFFFFF;
        return ((int) (255f * p / 100f) << 24) | rgb;
    }
}
