package com.cathelperawa.owo.app.bili;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.cathelperawa.owo.app.ui.CatIcon;

public final class BiliUi {

    private BiliUi() {}

    public static final int MAIN = 0xFFEAF1FB;
    public static final int SUB = 0xFF9AA9BC;
    public static final int ACCENT = 0xFF6EA6FF;
    public static final int OK = 0xFF37C28D;
    
    public static final int CARD_SUB = 0xFFC3CEDC;
    public static final int PINK = 0xFFFB7299;

    public static final int CARD_BG = 0x38FFFFFF;      
    public static final int CARD_LINE = 0x33FFFFFF;    
    public static final int DIM = 0x66000000;

    public static int dp(Context c, float v) {
        return (int) (v * c.getResources().getDisplayMetrics().density + 0.5f);
    }

    public static TextView text(Context c, String s, float sp, int color, boolean bold) {
        TextView t = new TextView(c);
        t.setText(s == null ? "" : s);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setIncludeFontPadding(false);
        t.setLineSpacing(dp(c, 2), 1f);
        return t;
    }

    
    public static final int SCRIM = 0x8C0B0F16;

    public static View scrim(Context c) {
        View v = new View(c);
        v.setBackgroundColor(SCRIM);
        v.setClickable(false);
        v.setFocusable(false);
        return v;
    }

    public static LinearLayout card(Context c, float radiusDp) {
        LinearLayout v = new LinearLayout(c);
        v.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable g = new GradientDrawable();
        g.setColor(CARD_BG);
        g.setCornerRadius(dp(c, radiusDp));
        g.setStroke(Math.max(1, dp(c, 0.6f)), CARD_LINE);
        v.setBackground(g);
        return v;
    }

    public static LinearLayout pill(Context c, int bg, int stroke, float radiusDp) {
        LinearLayout v = new LinearLayout(c);
        v.setOrientation(LinearLayout.HORIZONTAL);
        v.setGravity(Gravity.CENTER_VERTICAL);
        GradientDrawable g = new GradientDrawable();
        g.setColor(bg);
        g.setCornerRadius(dp(c, radiusDp));
        if (stroke != 0) g.setStroke(Math.max(1, dp(c, 0.8f)), stroke);
        v.setBackground(g);
        return v;
    }

    public static CatIcon icon(Context c, int glyph, int dpSize, int color) {
        return new CatIcon(c, glyph, dpSize, color);
    }

    public static LinearLayout iconAction(Context c, int glyph, String label,
                                          int color, float iconDp, float textSp) {
        LinearLayout row = new LinearLayout(c);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        CatIcon ic = icon(c, glyph, (int) iconDp, color);
        row.addView(ic, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        TextView t = text(c, label, textSp, color, false);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.leftMargin = dp(c, 5);
        row.addView(t, lp);
        return row;
    }

    public static LinearLayout iconActionColumn(Context c, int glyph, String label,
                                                int color, float iconDp, float textSp) {
        LinearLayout col = new LinearLayout(c);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(Gravity.CENTER);
        col.addView(icon(c, glyph, (int) iconDp, color));
        TextView t = text(c, label, textSp, color, false);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(c, 4);
        col.addView(t, lp);
        return col;
    }

    public static View line(Context c, float alpha) {
        View v = new View(c);
        v.setBackgroundColor(Color.argb((int) (255 * alpha), 255, 255, 255));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, dp(c, 0.5f)));
        v.setLayoutParams(lp);
        return v;
    }

    public static void applyBackground(Activity a, View root) {
        if (a == null || root == null) return;
        android.content.SharedPreferences sp =
                a.getSharedPreferences(com.cathelperawa.owo.app.AppConfig.PREF, Context.MODE_PRIVATE);
        String style = sp.getString("uiStyle", "深色（默认）");
        String bgPath = sp.getString("customBg", "");
        boolean applied = false;
        if (bgPath != null && bgPath.length() > 0) {
            android.graphics.drawable.Drawable d =
                    android.graphics.drawable.Drawable.createFromPath(bgPath);
            if (d != null) {
                int alphaPct = sp.getInt("bgAlpha", 100);
                if (alphaPct >= 100) {
                    root.setBackground(d);
                } else {
                    int overlay = (int) ((100 - alphaPct) / 100.0 * 255);
                    root.setBackground(new android.graphics.drawable.LayerDrawable(
                            new android.graphics.drawable.Drawable[]{
                                    d, new android.graphics.drawable.ColorDrawable(
                                    Color.argb(overlay, 15, 19, 26))}));
                }
                applied = true;
            }
        }
        if (!applied) {
            root.setBackgroundColor(Color.parseColor(
                    "深蓝".equals(style) ? "#0C1625" : "#0F131A"));
        }
    }

    public static int topbarColor(Activity a) {
        android.content.SharedPreferences sp =
                a.getSharedPreferences(com.cathelperawa.owo.app.AppConfig.PREF, Context.MODE_PRIVATE);
        String style = sp.getString("uiStyle", "深色（默认）");
        return Color.parseColor("深蓝".equals(style) ? "#8011233C" : "#800F131A");
    }
}
