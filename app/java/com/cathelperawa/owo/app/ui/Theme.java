package com.cathelperawa.owo.app.ui;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.cathelperawa.owo.app.core.Skin;

public final class Theme {

    private static final int MAX_DEPTH = 40;

    private Theme() {}

    public static int c(int color) {
        return Skin.color(color);
    }

    public static void apply(View root) {
        if (root == null || !Skin.active()) return;
        try {
            walk(root, 0);
        } catch (Throwable t) {
            
        }
    }

    private static void walk(View v, int depth) {
        if (v == null || depth > MAX_DEPTH) return;
        try {
            if (v instanceof TextView) {
                TextView t = (TextView) v;
                int old = t.getCurrentTextColor();
                int now = Skin.color(old);
                if (now != old) t.setTextColor(now);
            }
            mapBackground(v);
            if (v instanceof ViewGroup) {
                ViewGroup g = (ViewGroup) v;
                for (int i = 0; i < g.getChildCount(); i++) {
                    walk(g.getChildAt(i), depth + 1);
                }
            }
        } catch (Throwable ignore) {
        }
    }

    private static void mapBackground(View v) {
        try {
            Drawable bg = v.getBackground();
            if (bg == null) return;
            if (bg instanceof ColorDrawable) {
                int old = ((ColorDrawable) bg).getColor();
                int now = Skin.color(old);
                if (now != old) v.setBackgroundColor(now);
                return;
            }
            if (bg instanceof GradientDrawable) {
                int old = solidColor((GradientDrawable) bg);
                if (old == 0) return;
                int now = Skin.color(old);
                if (now != old) {
                    ((GradientDrawable) bg).setColor(now);
                    v.setBackgroundDrawable(bg);
                }
            }
        } catch (Throwable ignore) {
        }
    }

    private static int solidColor(GradientDrawable g) {
        try {
            Object cs = GradientDrawable.class.getMethod("getColor").invoke(g);
            if (cs == null) return 0;
            if (cs instanceof android.content.res.ColorStateList) {
                return ((android.content.res.ColorStateList) cs).getDefaultColor();
            }
        } catch (Throwable ignore) {
        }
        return 0;
    }

    public static int parse(String s, int def) {
        try {
            return s == null || s.length() == 0 ? def : Color.parseColor(s);
        } catch (Throwable t) {
            return def;
        }
    }
}
