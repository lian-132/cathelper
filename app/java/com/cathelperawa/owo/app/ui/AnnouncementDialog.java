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

import java.util.List;

public final class AnnouncementDialog {

    private static final int C_TITLE = 0xFF43587A;
    private static final int C_PINK = 0xFFF07AAE;

    private AnnouncementDialog() {}

    public interface OnOk {
        void run();
    }

    public static void show(final Activity a, String title, String body,
                            String okLabel, final OnOk onOk) {
        show(a, title, body, null, okLabel, onOk);
    }

    public static void show(final Activity a, String title, String body, String imageUrl,
                            String okLabel, final OnOk onOk) {
        show(a, title, body, imageUrl, okLabel, onOk, null);
    }

    public static void show(final Activity a, String title, String body, String imageUrl,
                            String okLabel, final OnOk onOk, final Runnable onClose) {
        java.util.ArrayList<NoticePagerDialog.Btn> one =
                new java.util.ArrayList<NoticePagerDialog.Btn>();
        NoticePagerDialog.Btn b = new NoticePagerDialog.Btn();
        b.label = okLabel == null || okLabel.length() == 0 ? "去看看" : okLabel;
        if (onOk != null) {
            b.action = new Runnable() {
                @Override public void run() {
                    try { onOk.run(); } catch (Throwable ignore) {}
                }
            };
        }
        one.add(b);
        showWithButtons(a, title, body, imageUrl, one, onClose);
    }

    public static void showWithButtons(final Activity a, String title, String body,
                                       String imageUrl,
                                       final List<NoticePagerDialog.Btn> buttons,
                                       final Runnable onClose) {
        if (a == null || a.isFinishing()) return;
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
        card.setPadding(dp(d, 22), dp(d, 20), dp(d, 22), dp(d, 16));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFFFFFFFF);
        bg.setCornerRadius(dp(d, 24));
        card.setBackgroundDrawable(bg);
        card.setElevation(dp(d, 10));

        LinearLayout head = new LinearLayout(a);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);

        TextView badge = new TextView(a);
        badge.setText("公告");
        badge.setTextSize(11);
        badge.setTextColor(0xFFFFFFFF);
        badge.setTypeface(Typeface.DEFAULT_BOLD);
        badge.setGravity(Gravity.CENTER);
        GradientDrawable bgBadge = new GradientDrawable();
        bgBadge.setColor(C_PINK);
        bgBadge.setCornerRadius(dp(d, 9));
        badge.setBackgroundDrawable(bgBadge);
        head.addView(badge, new LinearLayout.LayoutParams(dp(d, 40), dp(d, 24)));

        TextView tvTitle = new TextView(a);
        tvTitle.setText(title == null ? "" : title);
        tvTitle.setTextSize(17);
        tvTitle.setTextColor(C_TITLE);
        tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(0, -2, 1f);
        tlp.leftMargin = dp(d, 10);
        head.addView(tvTitle, tlp);
        card.addView(head);

        View line = new View(a);
        GradientDrawable lg = new GradientDrawable();
        lg.setColor(0x1F8A9CB5);
        line.setBackgroundDrawable(lg);
        LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(-1, dp(d, 1));
        llp.topMargin = dp(d, 14);
        llp.bottomMargin = dp(d, 12);
        card.addView(line, llp);

        if (imageUrl != null && imageUrl.length() > 0) {
            final GifView gv = new GifView(a);
            android.graphics.drawable.GradientDrawable ig = new android.graphics.drawable.GradientDrawable();
            ig.setColor(0x0F8A9CB5);
            ig.setCornerRadius(dp(d, 12));
            gv.setBackgroundDrawable(ig);
            gv.setClipToOutline(true);
            LinearLayout.LayoutParams glp = new LinearLayout.LayoutParams(-1, -2);
            glp.bottomMargin = dp(d, 12);
            card.addView(gv, glp);
            gv.load(imageUrl);
        }

        TextView tvBody = new TextView(a);
        tvBody.setText(body == null ? "" : body);
        tvBody.setTextSize(14);
        tvBody.setTextColor(0xFF5E7090);
        tvBody.setLineSpacing(dp(d, 5), 1f);
        tvBody.setTextIsSelectable(true);

        ScrollView sv = new ScrollView(a) {
            @Override protected void onMeasure(int w, int h) {
                super.onMeasure(w, h);
                int max = (int) (getResources().getDisplayMetrics().heightPixels * 0.38f);
                if (getMeasuredHeight() > max) {
                    setMeasuredDimension(getMeasuredWidth(), max);
                }
            }
        };
        sv.setVerticalScrollBarEnabled(false);
        sv.addView(tvBody, new FrameLayout.LayoutParams(-1, -2));
        card.addView(sv, new LinearLayout.LayoutParams(-1, -2));

        int n = buttons == null ? 0 : buttons.size();
        final LinearLayout btns = new LinearLayout(a);
        btns.setOrientation(n <= 1 ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);

        TextView cancel = new TextView(a);
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

        if (n <= 1) {
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(0, -2,
                    n == 1 ? 1f : 2f);
            if (n == 1) cp.rightMargin = dp(d, 6);
            btns.addView(cancel, cp);
        }

        for (int i = 0; i < n; i++) {
            final NoticePagerDialog.Btn src = buttons.get(i);
            if (src == null) continue;
            TextView go = new TextView(a);
            go.setText(src.label == null || src.label.length() == 0 ? "打开" : src.label);
            go.setTextSize(14);
            go.setTypeface(Typeface.DEFAULT_BOLD);
            go.setTextColor(0xFFFFFFFF);
            go.setGravity(Gravity.CENTER);
            go.setPadding(0, dp(d, 11), 0, dp(d, 11));
            GradientDrawable gbg = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                    new int[]{0xFFF58FBC, 0xFFF07AAE});
            gbg.setCornerRadius(dp(d, 14));
            go.setBackgroundDrawable(gbg);
            go.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    try { dlg.dismiss(); } catch (Throwable ignore) {}
                    if (src.action != null) {
                        try { src.action.run(); } catch (Throwable ignore) {}
                    }
                }
            });
            LinearLayout.LayoutParams gp;
            if (n <= 1) {
                gp = new LinearLayout.LayoutParams(0, -2, 1f);
                gp.leftMargin = dp(d, 6);
            } else {
                gp = new LinearLayout.LayoutParams(-1, -2);
                gp.bottomMargin = dp(d, 8);
            }
            btns.addView(go, gp);
        }

        if (n > 1) {
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, -2);
            cp.topMargin = dp(d, 2);
            btns.addView(cancel, cp);
        }

        LinearLayout.LayoutParams btlp = new LinearLayout.LayoutParams(-1, -2);
        btlp.topMargin = dp(d, 18);
        card.addView(btns, btlp);

        FrameLayout.LayoutParams clp = new FrameLayout.LayoutParams(
                (int) (a.getResources().getDisplayMetrics().widthPixels * 0.86f), -2);
        clp.gravity = Gravity.CENTER;
        root.addView(card, clp);

        dlg.setContentView(root);
        dlg.setOnDismissListener(new android.content.DialogInterface.OnDismissListener() {
            @Override public void onDismiss(android.content.DialogInterface di) {
                if (onClose != null) {
                    try { onClose.run(); } catch (Throwable ignore) {}
                }
            }
        });
        dlg.setCanceledOnTouchOutside(false);
        dlg.setCancelable(false);
        try { dlg.show(); } catch (Throwable ignore) {}
    }

    private static int dp(float density, int v) {
        return (int) (v * density + 0.5f);
    }
}
