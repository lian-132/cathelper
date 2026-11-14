package com.cathelperawa.owo.app.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public final class UiKit {

    private UiKit() {}

    public static int dp(Context c, float v) {
        return (int) (v * c.getResources().getDisplayMetrics().density + 0.5f);
    }

    public static TextView text(Context c, String s, float size, int color,
                                boolean bold, int bottomPad) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(size);
        t.setTextColor(Theme.c(color));
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD, Typeface.BOLD);
        t.setPadding(0, 0, 0, bottomPad);
        return t;
    }

    public static TextView pill(Context c, String s, int color) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(11);
        t.setTextColor(Theme.c(color));
        t.setTypeface(Typeface.DEFAULT_BOLD, Typeface.BOLD);
        GradientDrawable g = new GradientDrawable();
        g.setColor(Theme.c(0xEEFFFFFF));
        g.setCornerRadius(dp(c, 12));
        t.setBackgroundDrawable(g);
        t.setPadding(dp(c, 10), dp(c, 3), dp(c, 10), dp(c, 3));
        return t;
    }

    public static EditText pastelEdit(Context c, String value, String hint) {
        EditText et = new EditText(c);
        et.setText(value == null ? "" : value);
        et.setHint(hint);
        et.setTextSize(13);
        et.setTextColor(0xFF43587A);
        et.setHintTextColor(0xFFB3C0D2);
        et.setSingleLine(true);
        GradientDrawable eg = new GradientDrawable();
        eg.setColor(0xF2FFFFFF);
        eg.setCornerRadius(dp(c, 12));
        eg.setStroke(dp(c, 1), 0x55C9D6E8);
        et.setBackgroundDrawable(eg);
        et.setPadding(dp(c, 12), dp(c, 9), dp(c, 12), dp(c, 9));
        return et;
    }

    public static void innerScroll(final EditText et) {
        if (et == null) return;

        try {
            et.setVerticalScrollBarEnabled(false);
            et.setHorizontalScrollBarEnabled(false);
        } catch (Throwable ignore) {
        }

        et.setOnTouchListener(new View.OnTouchListener() {
            @Override public boolean onTouch(View v, android.view.MotionEvent e) {
                switch (e.getActionMasked()) {
                    case android.view.MotionEvent.ACTION_DOWN:
                    case android.view.MotionEvent.ACTION_MOVE:
                        if (v.getParent() != null) {
                            v.getParent().requestDisallowInterceptTouchEvent(true);
                        }
                        break;
                    case android.view.MotionEvent.ACTION_UP:
                    case android.view.MotionEvent.ACTION_CANCEL:
                        if (v.getParent() != null) {
                            v.getParent().requestDisallowInterceptTouchEvent(false);
                        }
                        break;
                    default:
                        break;
                }
                return false;
            }
        });
    }

    public static LinearLayout whiteCard(Context c, int radius, int color) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable g = new GradientDrawable();
        g.setColor(Theme.c(color));
        g.setCornerRadius(radius);
        g.setStroke(dp(c, 1), 0x66FFFFFF);
        l.setBackgroundDrawable(g);
        l.setElevation(dp(c, 4));
        return l;
    }

    public static FrameLayout whiteFrame(Context c, int radius, int color) {
        FrameLayout f = new FrameLayout(c);
        GradientDrawable g = new GradientDrawable();
        g.setColor(Theme.c(color));
        g.setCornerRadius(radius);
        g.setStroke(dp(c, 1), 0x66FFFFFF);
        f.setBackgroundDrawable(g);
        f.setElevation(dp(c, 4));
        return f;
    }

    public static GradientDrawable circleBg(int color, int radius) {
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        g.setColor(Theme.c(color));
        return g;
    }

    public static LinearLayout.LayoutParams matchWrap(Context c, int bottomMargin) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        p.bottomMargin = bottomMargin;
        return p;
    }

    public static LinearLayout.LayoutParams wrapWrap() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    public static LinearLayout.LayoutParams matchFixed(Context c, int heightDp, int bottomDp) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(c, heightDp));
        p.bottomMargin = dp(c, bottomDp);
        return p;
    }

    public static void press(final View v) {
        v.animate().scaleX(0.97f).scaleY(0.97f).setDuration(85)
                .withEndAction(new Runnable() {
                    public void run() {
                        v.animate().scaleX(1f).scaleY(1f).setDuration(150)
                                .setInterpolator(new DecelerateInterpolator()).start();
                    }
                }).start();
    }

    public static void spin(final View v) {
        if (v == null) return;
        try {
            v.animate().cancel();
            if (v.getWidth() > 0 && v.getHeight() > 0) {
                v.setPivotX(v.getWidth() / 2f);
                v.setPivotY(v.getHeight() / 2f);
            }
            v.setRotation(0f);
            v.animate().rotationBy(360f).setDuration(450)
                    .setInterpolator(new DecelerateInterpolator()).start();
        } catch (Throwable ignore) {
        }
    }

    public static void switchHero(final ImageView iv, final String[] names, final int[] cur,
                                  int next, final View[] dots, int dir) {
        if (next == cur[0]) return;
        cur[0] = next;
        final float d = iv.getContext().getResources().getDisplayMetrics().density;
        final Bitmap b = Images.get(names[next]);
        iv.animate().alpha(0f).translationX(-d * 24 * dir).setDuration(120)
                .withEndAction(new Runnable() {
                    public void run() {
                        if (b != null) iv.setImageBitmap(b);
                        iv.setTranslationX(d * 24 * dir);
                        iv.animate().alpha(1f).translationX(0f)
                                .setDuration(220).setInterpolator(new DecelerateInterpolator()).start();
                    }
                }).start();
        for (int i = 0; i < dots.length; i++) {
            if (dots[i] == null) continue;
            ((GradientDrawable) dots[i].getBackground())
                    .setColor(i == cur[0] ? 0xFF8FB8FF : 0x558FB8FF);
            dots[i].animate().scaleX(i == cur[0] ? 1.15f : 1f)
                    .scaleY(i == cur[0] ? 1.15f : 1f).setDuration(160).start();
        }
    }

    public static TextView chip(Context c, String label) {
        TextView tv = text(c, label, 12, 0xFF7FA5E6, true, 0);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xE6FFFFFF);
        bg.setCornerRadius(dp(c, 14));
        tv.setBackgroundDrawable(bg);
        tv.setPadding(dp(c, 12), dp(c, 6), dp(c, 12), dp(c, 6));
        return tv;
    }

    public static void catToast(final Context c, final String msg) {

        if (c != null && android.os.Looper.myLooper() == null) {
            try {
                new android.os.Handler(android.os.Looper.getMainLooper()).post(
                        new Runnable() {
                            @Override public void run() {
                                catToast(c, msg);
                            }
                        });
                return;
            } catch (Throwable ignore) {
            }
        }
        try {
            Toast t = new Toast(c);
            LinearLayout box = new LinearLayout(c);
            box.setOrientation(LinearLayout.HORIZONTAL);
            box.setGravity(Gravity.CENTER_VERTICAL);
            GradientDrawable g = new GradientDrawable();
            g.setColor(0xF7FFFFFF);
            g.setCornerRadius(dp(c, 24));
            g.setStroke(dp(c, 1), 0x44F07CA8);
            box.setBackgroundDrawable(g);
            box.setPadding(dp(c, 14), dp(c, 9), dp(c, 19), dp(c, 9));
            box.setElevation(dp(c, 8));

            ImageView iv = new ImageView(c);
            try {
                iv.setImageResource(com.cathelperawa.owo.app.R.drawable.cat_toast);
            } catch (Throwable ignore) {}
            box.addView(iv, new LinearLayout.LayoutParams(dp(c, 24), dp(c, 24)));

            TextView tv = new TextView(c);
            tv.setText(msg);
            tv.setTextColor(0xFF43587A);
            tv.setTextSize(13);
            tv.setPadding(dp(c, 9), 0, 0, 0);
            box.addView(tv);

            t.setView(box);
            t.setDuration(Toast.LENGTH_SHORT);
            t.setGravity(Gravity.BOTTOM, 0, dp(c, 100));
            t.show();
        } catch (Throwable e) {
            Toast.makeText(c, msg, Toast.LENGTH_SHORT).show();
        }
    }

    public static void attachSlimScrollbar(final android.widget.ScrollView sv,
                                           final View thumb,
                                           final View track,
                                           final float den) {
        final int minH = (int) (36 * den);
        thumb.setAlpha(0f);

        final android.os.Handler h =
                new android.os.Handler(android.os.Looper.getMainLooper());
        final Runnable fade = new Runnable() {
            @Override public void run() {
                thumb.animate().alpha(0f).setDuration(300).start();
            }
        };
        final Runnable update = new Runnable() {
            @Override public void run() {
                View content = sv.getChildCount() > 0 ? sv.getChildAt(0) : null;
                int viewH = sv.getHeight();
                int trackH = track.getHeight();
                if (content == null || viewH <= 0 || trackH <= 0) return;
                int contentH = content.getHeight();
                if (contentH <= viewH) {
                    track.setVisibility(View.INVISIBLE);
                    thumb.setAlpha(0f);
                    return;
                }
                track.setVisibility(View.VISIBLE);
                int th = Math.max(minH, (int) ((float) trackH * viewH / contentH));
                int range = contentH - viewH;
                int maxTop = trackH - th;
                int top = range <= 0 ? 0 : Math.max(0, Math.min(maxTop,
                        (int) ((float) sv.getScrollY() / range * maxTop)));
                LinearLayout.LayoutParams lp =
                        (LinearLayout.LayoutParams) thumb.getLayoutParams();
                lp.height = th;
                lp.topMargin = top;
                thumb.setLayoutParams(lp);
            }
        };

        sv.getViewTreeObserver().addOnScrollChangedListener(
                new android.view.ViewTreeObserver.OnScrollChangedListener() {
            @Override public void onScrollChanged() {
                h.post(new Runnable() {
                    @Override public void run() {
                        update.run();
                        if (track.getVisibility() != View.VISIBLE) return;
                        thumb.setAlpha(0.55f);
                        h.removeCallbacks(fade);
                        h.postDelayed(fade, 1100);
                    }
                });
            }
        });
        sv.getViewTreeObserver().addOnGlobalLayoutListener(
                new android.view.ViewTreeObserver.OnGlobalLayoutListener() {
            @Override public void onGlobalLayout() { h.post(update); }
        });
        h.postDelayed(update, 80);
    }
}
