package com.cathelperawa.owo.app.pan;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public final class PanSheet {

    private PanSheet() {}

    static final int C_CARD = 0xF21A2230;
    static final int C_TITLE = 0xFFEAF1FB;
    static final int C_SUB = 0xFF9AA9BC;
    static final int C_DIM = 0xFF7C8CA0;
    static final int C_ACCENT = 0xFF6EA6FF;
    static final int C_ERR = 0xFFFF8A80;
    static final int C_LINE = 0x1FFFFFFF;

    private static final Handler H = new Handler(Looper.getMainLooper());

    private static int dp(Activity a, float v) {
        return Math.max(1, (int) (v * a.getResources().getDisplayMetrics().density + 0.5f));
    }

    private static GradientDrawable card(Activity a, float[] radii) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(C_CARD);
        g.setCornerRadii(radii);
        g.setStroke(dp(a, 1), 0x26FFFFFF);
        return g;
    }

    private static float[] topRadii(Activity a) {
        float r = dp(a, 20);
        return new float[]{r, r, r, r, 0, 0, 0, 0};
    }

    private static float[] allRadii(Activity a) {
        float r = dp(a, 18);
        return new float[]{r, r, r, r, r, r, r, r};
    }

    private static TextView text(Activity a, String s, float size, int color, boolean bold) {
        TextView tv = new TextView(a);
        tv.setText(s);
        tv.setTextSize(size);
        tv.setTextColor(color);
        if (bold) tv.setTypeface(null, android.graphics.Typeface.BOLD);
        return tv;
    }

    private static void styleWindow(Dialog d, Activity a, int gravity, int width) {
        Window w = d.getWindow();
        if (w == null) return;
        w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        WindowManager.LayoutParams lp = w.getAttributes();
        lp.dimAmount = 0.55f;
        lp.gravity = gravity;
        lp.width = width;
        lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        w.setAttributes(lp);
        
        if (gravity == Gravity.BOTTOM) {
            w.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    

    public interface Act {
        void run();
    }

    public static void menu(final Activity a, String title, String sub,
                            final String[] labels, final boolean[] danger, final Act[] acts) {
        if (a == null || a.isFinishing()) return;
        final Dialog d = new Dialog(a);
        LinearLayout sheet = new LinearLayout(a);
        sheet.setOrientation(LinearLayout.VERTICAL);
        sheet.setBackground(card(a, topRadii(a)));
        int pad = dp(a, 18);
        sheet.setPadding(pad, dp(a, 10), pad, dp(a, 16));

        View handle = new View(a);
        GradientDrawable hg = new GradientDrawable();
        hg.setCornerRadius(dp(a, 2));
        hg.setColor(0x33FFFFFF);
        handle.setBackground(hg);
        LinearLayout.LayoutParams hlp = new LinearLayout.LayoutParams(dp(a, 34), dp(a, 4));
        hlp.gravity = Gravity.CENTER_HORIZONTAL;
        hlp.bottomMargin = dp(a, 16);
        sheet.addView(handle, hlp);

        TextView t = text(a, title == null ? "" : title, 16f, C_TITLE, true);
        t.setMaxLines(2);
        t.setEllipsize(TextUtils.TruncateAt.MIDDLE);
        sheet.addView(t);
        if (sub != null && sub.length() > 0) {
            TextView s = text(a, sub, 12f, C_DIM, false);
            LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            slp.topMargin = dp(a, 5);
            slp.bottomMargin = dp(a, 6);
            sheet.addView(s, slp);
        }

        for (int i = 0; i < labels.length; i++) {
            final int idx = i;
            View line = new View(a);
            line.setBackgroundColor(C_LINE);
            LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, dp(a, 0.7f)));
            llp.topMargin = dp(a, 6);
            sheet.addView(line, llp);

            TextView row = text(a, labels[i], 15f,
                    danger != null && danger[i] ? C_ERR : C_TITLE, false);
            row.setPadding(dp(a, 2), dp(a, 15), dp(a, 2), dp(a, 15));
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    d.dismiss();
                    final Act act = acts == null ? null : acts[idx];
                    if (act != null) {
                        
                        H.postDelayed(new Runnable() {
                            @Override public void run() { act.run(); }
                        }, 90L);
                    }
                }
            });
            sheet.addView(row);
        }

        TextView cancel = text(a, "取消", 14f, C_SUB, false);
        cancel.setGravity(Gravity.CENTER);
        cancel.setPadding(0, dp(a, 14), 0, dp(a, 4));
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { d.dismiss(); }
        });
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        clp.topMargin = dp(a, 8);
        sheet.addView(cancel, clp);

        d.setContentView(sheet);
        styleWindow(d, a, Gravity.BOTTOM, ViewGroup.LayoutParams.MATCH_PARENT);
        try {
            d.show();
        } catch (Throwable ignore) {
        }
    }

    

    public static void confirm(final Activity a, String title, String message,
                               String okLabel, boolean dangerOk, final Act onOk) {
        if (a == null || a.isFinishing()) return;
        final Dialog d = new Dialog(a);
        LinearLayout box = new LinearLayout(a);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackground(card(a, allRadii(a)));
        int pad = dp(a, 20);
        box.setPadding(pad, pad, pad, dp(a, 12));

        box.addView(text(a, title, 16f, C_TITLE, true));
        if (message != null && message.length() > 0) {
            TextView m = text(a, message, 13f, C_SUB, false);
            m.setLineSpacing(dp(a, 4), 1f);
            LinearLayout.LayoutParams mlp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            mlp.topMargin = dp(a, 10);
            box.addView(m, mlp);
        }

        LinearLayout btnRow = new LinearLayout(a);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setGravity(Gravity.END);
        LinearLayout.LayoutParams brp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        brp.topMargin = dp(a, 18);
        box.addView(btnRow, brp);

        TextView cancel = text(a, "取消", 14f, C_SUB, false);
        cancel.setPadding(dp(a, 14), dp(a, 10), dp(a, 14), dp(a, 10));
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { d.dismiss(); }
        });
        btnRow.addView(cancel);

        TextView ok = text(a, okLabel, 14f, dangerOk ? C_ERR : C_ACCENT, true);
        ok.setPadding(dp(a, 16), dp(a, 10), dp(a, 4), dp(a, 10));
        ok.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                d.dismiss();
                if (onOk != null) H.postDelayed(new Runnable() {
                    @Override public void run() { onOk.run(); }
                }, 90L);
            }
        });
        btnRow.addView(ok);

        d.setContentView(box);
        Window w = d.getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowManager.LayoutParams lp = w.getAttributes();
            lp.dimAmount = 0.55f;
            lp.gravity = Gravity.CENTER;
            lp.width = a.getResources().getDisplayMetrics().widthPixels - dp(a, 56);
            w.setAttributes(lp);
        }
        try {
            d.show();
        } catch (Throwable ignore) {
        }
    }

    

    public interface TextAct {
        void run(String text);
    }

    public static void input(final Activity a, String title, String hint, String initial,
                             final TextAct onOk) {
        if (a == null || a.isFinishing()) return;
        final Dialog d = new Dialog(a);
        LinearLayout box = new LinearLayout(a);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackground(card(a, allRadii(a)));
        int pad = dp(a, 20);
        box.setPadding(pad, pad, pad, dp(a, 12));

        box.addView(text(a, title, 16f, C_TITLE, true));

        final EditText input = new EditText(a);
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(initial == null ? "" : initial);
        input.setHint(hint == null ? "" : hint);
        input.setTextColor(C_TITLE);
        input.setHintTextColor(C_DIM);
        input.setTextSize(14f);
        if (initial != null) input.setSelection(initial.length());
        GradientDrawable eb = new GradientDrawable();
        eb.setCornerRadius(dp(a, 10));
        eb.setColor(0x33FFFFFF);
        eb.setStroke(dp(a, 1), 0x33FFFFFF);
        input.setBackground(eb);
        input.setPadding(dp(a, 12), dp(a, 10), dp(a, 12), dp(a, 10));
        LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ilp.topMargin = dp(a, 14);
        box.addView(input, ilp);

        LinearLayout btnRow = new LinearLayout(a);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setGravity(Gravity.END);
        LinearLayout.LayoutParams brp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        brp.topMargin = dp(a, 16);
        box.addView(btnRow, brp);

        TextView cancel = text(a, "取消", 14f, C_SUB, false);
        cancel.setPadding(dp(a, 14), dp(a, 10), dp(a, 14), dp(a, 10));
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { d.dismiss(); }
        });
        btnRow.addView(cancel);

        TextView ok = text(a, "确定", 14f, C_ACCENT, true);
        ok.setPadding(dp(a, 16), dp(a, 10), dp(a, 4), dp(a, 10));
        ok.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(final View v) {
                final String v2 = input.getText() == null ? "" : input.getText().toString().trim();
                d.dismiss();
                if (onOk != null) H.postDelayed(new Runnable() {
                    @Override public void run() { onOk.run(v2); }
                }, 90L);
            }
        });
        btnRow.addView(ok);

        d.setContentView(box);
        Window w = d.getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowManager.LayoutParams lp = w.getAttributes();
            lp.dimAmount = 0.55f;
            lp.gravity = Gravity.CENTER;
            lp.width = a.getResources().getDisplayMetrics().widthPixels - dp(a, 56);
            w.setAttributes(lp);
            w.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }
        try {
            d.show();
            input.requestFocus();
        } catch (Throwable ignore) {
        }
    }

    static ScrollView scrollWrap(Activity a, View v) {
        ScrollView sv = new ScrollView(a);
        sv.addView(v);
        return sv;
    }
}
