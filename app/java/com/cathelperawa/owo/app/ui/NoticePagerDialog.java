package com.cathelperawa.owo.app.ui;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public final class NoticePagerDialog {

    private static final int C_TITLE = 0xFF43587A;
    private static final int C_SUB = 0xFF8A9CB5;
    private static final int C_PINK = 0xFFF07AAE;
    private static final int C_PINK_DEEP = 0xFFE85D9C;

    public static final class Btn {
        public String label = "打开";
        public Runnable action;
        public Btn() {}
        public Btn(String l, Runnable a) { label = l; action = a; }
    }

    public static final class Page {
        public final java.util.List<Btn> buttons = new java.util.ArrayList<Btn>();
        public String badge = "公告";
        public String title = "";
        public String sub = "";
        public String body = "";
        public String image = "";
        public String actionLabel = "知道了";
        public Runnable action;

        public Page() {}

        public Page(String badge, String title, String sub, String body,
                    String image, String actionLabel, Runnable action) {
            this.badge = badge;
            this.title = title;
            this.sub = sub;
            this.body = body;
            this.image = image;
            this.actionLabel = actionLabel;
            this.action = action;
        }
    }

    private NoticePagerDialog() {}

    public static void show(final Activity a, final List<Page> pages, final Runnable onClose) {
        if (a == null || a.isFinishing() || pages == null || pages.isEmpty()) return;
        final float d = a.getResources().getDisplayMetrics().density;
        final Dialog dlg = new Dialog(a, android.R.style.Theme_Translucent_NoTitleBar);
        Window w = dlg.getWindow();
        if (w != null) {
            w.setBackgroundDrawableResource(android.R.color.transparent);
            w.setLayout(WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT);
        }

        FrameLayout root = new FrameLayout(a);
        root.setBackgroundColor(0x99000000);

        LinearLayout card = new LinearLayout(a);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(d, 22), dp(d, 18), dp(d, 22), dp(d, 16));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFFFFFFFF);
        bg.setCornerRadius(dp(d, 24));
        card.setBackgroundDrawable(bg);
        card.setElevation(dp(d, 10));

        LinearLayout head = new LinearLayout(a);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);

        final TextView badge = new TextView(a);
        badge.setTextSize(11);
        badge.setTextColor(0xFFFFFFFF);
        badge.setTypeface(Typeface.DEFAULT_BOLD);
        badge.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(-2, dp(d, 24));
        blp.rightMargin = dp(d, 10);
        head.addView(badge, blp);

        LinearLayout headText = new LinearLayout(a);
        headText.setOrientation(LinearLayout.VERTICAL);
        final TextView title = new TextView(a);
        title.setTextSize(17);
        title.setTextColor(C_TITLE);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        headText.addView(title);
        final TextView sub = new TextView(a);
        sub.setTextSize(12);
        sub.setTextColor(C_PINK_DEEP);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(-2, -2);
        slp.topMargin = dp(d, 3);
        headText.addView(sub, slp);
        head.addView(headText, new LinearLayout.LayoutParams(0, -2, 1f));

        final TextView counter = new TextView(a);
        counter.setTextSize(12);
        counter.setTextColor(C_SUB);
        counter.setTypeface(Typeface.DEFAULT_BOLD);
        head.addView(counter);

        card.addView(head);

        View line = new View(a);
        GradientDrawable lg = new GradientDrawable();
        lg.setColor(0x1F8A9CB5);
        line.setBackgroundDrawable(lg);
        LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(-1, dp(d, 1));
        llp.topMargin = dp(d, 12);
        llp.bottomMargin = dp(d, 12);
        card.addView(line, llp);

        final LinearLayout imageBox = new LinearLayout(a);
        imageBox.setOrientation(LinearLayout.VERTICAL);
        card.addView(imageBox, new LinearLayout.LayoutParams(-1, -2));

        final TextView tvBody = new TextView(a);
        tvBody.setTextSize(14);
        tvBody.setTextColor(0xFF5E7090);
        tvBody.setLineSpacing(dp(d, 5), 1f);
        tvBody.setTextIsSelectable(true);

        ScrollView sv = new ScrollView(a) {
            @Override protected void onMeasure(int wSpec, int hSpec) {
                super.onMeasure(wSpec, hSpec);
                int max = (int) (getResources().getDisplayMetrics().heightPixels * 0.34f);
                if (getMeasuredHeight() > max) {
                    setMeasuredDimension(getMeasuredWidth(), max);
                }
            }
        };
        sv.setVerticalScrollBarEnabled(false);
        sv.addView(tvBody, new FrameLayout.LayoutParams(-1, -2));
        card.addView(sv, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout btns = new LinearLayout(a);
        btns.setOrientation(LinearLayout.HORIZONTAL);
        btns.setGravity(Gravity.CENTER_VERTICAL);

        final TextView prev = arrow(a, d, "◀");
        final TextView next = arrow(a, d, "▶");

        final LinearLayout actionRow = new LinearLayout(a);
        actionRow.setOrientation(LinearLayout.HORIZONTAL);
        actionRow.setPadding(0, 0, 0, dp(d, 6));
        card.addView(actionRow, new LinearLayout.LayoutParams(-1, -2));

        final TextView cancel = new TextView(a);
        cancel.setText("取消");
        cancel.setTextSize(14);
        cancel.setTypeface(Typeface.DEFAULT_BOLD);
        cancel.setTextColor(0xFF8A9CB5);
        cancel.setGravity(Gravity.CENTER);
        cancel.setPadding(0, dp(d, 11), 0, dp(d, 11));
        GradientDrawable cbg = new GradientDrawable();
        cbg.setColor(0xFFF3F6FB);
        cbg.setCornerRadius(dp(d, 14));
        cancel.setBackgroundDrawable(cbg);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                try { dlg.dismiss(); } catch (Throwable ignore) {}
            }
        });

        final TextView go = new TextView(a);
        go.setTextSize(14);
        go.setTypeface(Typeface.DEFAULT_BOLD);
        go.setTextColor(0xFFFFFFFF);
        go.setGravity(Gravity.CENTER);
        go.setPadding(0, dp(d, 11), 0, dp(d, 11));
        GradientDrawable gbg = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{0xFFF58FBC, 0xFFF07AAE});
        gbg.setCornerRadius(dp(d, 14));
        go.setBackgroundDrawable(gbg);

        LinearLayout.LayoutParams alp = new LinearLayout.LayoutParams(dp(d, 38), dp(d, 40));
        alp.rightMargin = dp(d, 6);
        btns.addView(prev, alp);

        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(0, -2, 1f);
        clp.rightMargin = dp(d, 6);
        btns.addView(cancel, clp);


        LinearLayout.LayoutParams nlp = new LinearLayout.LayoutParams(dp(d, 38), dp(d, 40));
        btns.addView(next, nlp);

        LinearLayout.LayoutParams btlp = new LinearLayout.LayoutParams(-1, -2);
        btlp.topMargin = dp(d, 16);
        card.addView(btns, btlp);

        FrameLayout.LayoutParams clp2 = new FrameLayout.LayoutParams(
                (int) (a.getResources().getDisplayMetrics().widthPixels * 0.88f), -2);
        clp2.gravity = Gravity.CENTER;
        root.addView(card, clp2);

        final int[] idx = {0};

        final Runnable render = new Runnable() {
            @Override public void run() {
                final Page p = pages.get(idx[0]);
                badge.setText(p.badge == null || p.badge.length() == 0 ? "公告" : p.badge);
                GradientDrawable b = new GradientDrawable();
                b.setColor("新".equals(p.badge) ? C_PINK : 0xFF8FB4E8);
                b.setCornerRadius(dp(d, 9));
                badge.setBackgroundDrawable(b);
                badge.setPadding(dp(d, 9), 0, dp(d, 9), 0);

                title.setText(p.title == null ? "" : p.title);
                if (p.sub == null || p.sub.length() == 0) {
                    sub.setVisibility(View.GONE);
                } else {
                    sub.setVisibility(View.VISIBLE);
                    sub.setText(p.sub);
                }
                tvBody.setText(p.body == null || p.body.trim().length() == 0
                        ? (p.sub == null || p.sub.length() == 0 ? "" : p.sub) : p.body);

                imageBox.removeAllViews();
                if (p.image != null && p.image.length() > 0) {
                    GifView gv = new GifView(a);
                    GradientDrawable ig = new GradientDrawable();
                    ig.setColor(0x0F8A9CB5);
                    ig.setCornerRadius(dp(d, 12));
                    gv.setBackgroundDrawable(ig);
                    gv.setClipToOutline(true);
                    LinearLayout.LayoutParams gp = new LinearLayout.LayoutParams(-1, -2);
                    gp.bottomMargin = dp(d, 12);
                    imageBox.addView(gv, gp);
                    gv.load(p.image);
                }

                buildActions(actionRow, p, d);
                counter.setText((idx[0] + 1) + "/" + pages.size());
                go.setText(p.actionLabel == null || p.actionLabel.length() == 0 ? "知道了" : p.actionLabel);
                if (pages.size() <= 1) {
                    prev.setVisibility(View.GONE);
                    next.setVisibility(View.GONE);
                } else {
                    prev.setAlpha(idx[0] == 0 ? 0.3f : 1f);
                    next.setAlpha(idx[0] >= pages.size() - 1 ? 0.3f : 1f);
                }
            }
        };

        prev.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (idx[0] <= 0) return;
                idx[0]--;
                render.run();
            }
        });
        next.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (idx[0] >= pages.size() - 1) return;
                idx[0]++;
                render.run();
            }
        });
        go.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                final Page p = pages.get(idx[0]);
                try { dlg.dismiss(); } catch (Throwable ignore) {}
                if (p.action != null) {
                    try { p.action.run(); } catch (Throwable ignore) {}
                }
            }
        });

        dlg.setOnDismissListener(new android.content.DialogInterface.OnDismissListener() {
            @Override public void onDismiss(android.content.DialogInterface di) {
                if (onClose != null) {
                    try { onClose.run(); } catch (Throwable ignore) {}
                }
            }
        });

        dlg.setContentView(root);
        dlg.setCanceledOnTouchOutside(false);
        dlg.setCancelable(false);
        render.run();
        try { dlg.show(); } catch (Throwable ignore) {}
    }

    private static TextView arrow(Activity a, float d, String label) {
        TextView tv = new TextView(a);
        tv.setText(label);
        tv.setTextSize(13);
        tv.setTextColor(0xFF7FA5E6);
        tv.setGravity(Gravity.CENTER);
        GradientDrawable g = new GradientDrawable();
        g.setColor(0xFFF3F6FB);
        g.setCornerRadius(dp(d, 12));
        tv.setBackgroundDrawable(g);
        return tv;
    }

    private static void buildActions(final LinearLayout row, Page p, final float d) {
        row.removeAllViews();
        if (p.buttons != null && p.buttons.size() > 0) {
            int n = Math.min(p.buttons.size(), 3);
            for (int i = 0; i < n; i++) {
                final Btn b = p.buttons.get(i);
                TextView t = new TextView(row.getContext());
                t.setText(b.label == null || b.label.length() == 0 ? "打开" : b.label);
                t.setTextSize(14);
                t.setTypeface(Typeface.DEFAULT_BOLD);
                t.setTextColor(0xFFFFFFFF);
                t.setGravity(Gravity.CENTER);
                t.setPadding(0, dp(d, 11), 0, dp(d, 11));
                GradientDrawable g = new GradientDrawable(
                        GradientDrawable.Orientation.LEFT_RIGHT,
                        new int[]{0xFFF58FBC, 0xFFF07AAE});
                g.setCornerRadius(dp(d, 14));
                t.setBackgroundDrawable(g);
                t.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        if (b.action != null) {
                            try { b.action.run(); } catch (Throwable ignore) {}
                        }
                    }
                });
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1f);
                if (i > 0) lp.leftMargin = dp(d, 6);
                row.addView(t, lp);
            }
            return;
        }
        if (p.action == null) return;
        TextView t = new TextView(row.getContext());
        t.setText(p.actionLabel == null || p.actionLabel.length() == 0 ? "打开" : p.actionLabel);
        t.setTextSize(14);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setTextColor(0xFFFFFFFF);
        t.setGravity(Gravity.CENTER);
        t.setPadding(0, dp(d, 11), 0, dp(d, 11));
        GradientDrawable g = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{0xFFF58FBC, 0xFFF07AAE});
        g.setCornerRadius(dp(d, 14));
        t.setBackgroundDrawable(g);
        t.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (p.action != null) {
                    try { p.action.run(); } catch (Throwable ignore) {}
                }
            }
        });
        row.addView(t, new LinearLayout.LayoutParams(0, -2, 1f));
    }

    private static int dp(float density, int v) {
        return (int) (v * density + 0.5f);
    }
}
