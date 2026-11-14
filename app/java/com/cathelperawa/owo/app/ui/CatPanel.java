package com.cathelperawa.owo.app.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public final class CatPanel {

    private CatPanel() {}

    public interface OnOk { void run(); }

    public static final int C_TITLE = 0xFF43587A;

    public static final int C_BODY = 0xFF43587A;

    public static final int C_HINT = 0xFF9AAABC;

    public static final int C_PINK = 0xFFF07CA8;

    public static final int C_BLUE = 0xFF7FA5E6;

    public static void show(final Context ctx, final String badge, final String title,
                            final String sub, final View body,
                            final String okLabel, final OnOk onOk) {
        final Activity a = act(ctx);
        if (a == null) return;
        final float den = a.getResources().getDisplayMetrics().density;

        a.runOnUiThread(new Runnable() { @Override public void run() {
            try {

                LinearLayout shell = new LinearLayout(a);
                shell.setOrientation(LinearLayout.VERTICAL);
                GradientDrawable sg = new GradientDrawable();
                sg.setColor(0xFDFEFFFF);
                sg.setCornerRadius(dp(den, 24));
                sg.setStroke(dp(den, 1), 0x66C9D6E8);
                shell.setBackgroundDrawable(sg);
                shell.setElevation(dp(den, 10));

                LinearLayout head = new LinearLayout(a);
                head.setOrientation(LinearLayout.HORIZONTAL);
                head.setGravity(Gravity.CENTER_VERTICAL);
                head.setPadding(dp(den, 16), dp(den, 14), dp(den, 16), dp(den, 8));

                android.graphics.Bitmap cat = Images.get("cat_toast");
                if (cat != null) {
                    ImageView iv = new ImageView(a);
                    iv.setImageBitmap(cat);
                    iv.setAdjustViewBounds(true);
                    head.addView(iv, new LinearLayout.LayoutParams(
                            dp(den, 30), dp(den, 27)));
                }
                LinearLayout hcol = new LinearLayout(a);
                hcol.setOrientation(LinearLayout.VERTICAL);
                LinearLayout.LayoutParams hcp = new LinearLayout.LayoutParams(
                        0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                hcp.leftMargin = cat == null ? 0 : dp(den, 9);
                hcol.addView(UiKit.text(a, title == null ? "" : title,
                        17, C_TITLE, true, 0));
                if (sub != null && sub.length() > 0) {
                    TextView st = UiKit.text(a, sub, 11, C_HINT, false, 0);
                    st.setPadding(0, dp(den, 3), 0, 0);
                    hcol.addView(st);
                }
                head.addView(hcol, hcp);
                if (badge != null && badge.length() > 0) {
                    head.addView(UiKit.pill(a, badge, C_PINK));
                }
                shell.addView(head, new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));

                View line = new View(a);
                GradientDrawable lg = new GradientDrawable();
                lg.setColor(0x1F8FB0D8);
                line.setBackgroundDrawable(lg);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, dp(den, 1)));
                lp.leftMargin = dp(den, 16);
                lp.rightMargin = dp(den, 16);
                shell.addView(line, lp);

                ScrollView sv = new ScrollView(a);
                sv.setVerticalScrollBarEnabled(false);
                sv.setOverScrollMode(View.OVER_SCROLL_NEVER);
                sv.setClipToPadding(false);
                if (body != null) {
                    sv.addView(body, new ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT));
                }
                LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
                shell.addView(sv, sp);

                LinearLayout row = new LinearLayout(a);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding(dp(den, 16), dp(den, 10), dp(den, 16), dp(den, 14));

                final AlertDialog[] holder = new AlertDialog[1];

                TextView cancel = pillButton(a, "取消", false);
                cancel.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        UiKit.press(v);
                        if (holder[0] != null) holder[0].dismiss();
                    }
                });
                LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(
                        0, dp(den, 44), 1f);
                cp.rightMargin = dp(den, 9);
                row.addView(cancel, cp);

                TextView ok = pillButton(a, okLabel == null ? "确定" : okLabel, true);
                ok.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        UiKit.press(v);
                        if (holder[0] != null) holder[0].dismiss();
                        try { if (onOk != null) onOk.run(); }
                        catch (Throwable t) {
                            com.cathelperawa.owo.app.core.HookLog.e("CatPanel 回调异常", t);
                        }
                    }
                });
                row.addView(ok, new LinearLayout.LayoutParams(0, dp(den, 44), 1f));
                shell.addView(row, new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));

                AlertDialog d = new AlertDialog.Builder(a, AlertDialog.THEME_DEVICE_DEFAULT_LIGHT)
                        .setView(shell)
                        .create();
                holder[0] = d;
                if (d.getWindow() != null) {
                    d.getWindow().setBackgroundDrawable(
                            new android.graphics.drawable.ColorDrawable(0x00000000));
                    android.view.WindowManager.LayoutParams wl = d.getWindow().getAttributes();
                    wl.width = (int) (a.getResources().getDisplayMetrics().widthPixels * 0.88f);
                    d.getWindow().setAttributes(wl);
                }
                d.setCanceledOnTouchOutside(true);
                d.show();
            } catch (Throwable t) {
                com.cathelperawa.owo.app.core.HookLog.e("CatPanel 弹窗失败", t);
            }
        } });
    }

    public static TextView pillButton(Context c, String label, boolean primary) {
        TextView tv = new TextView(c);
        tv.setText(label);
        tv.setTextSize(15);
        tv.setTypeface(android.graphics.Typeface.DEFAULT_BOLD, android.graphics.Typeface.BOLD);
        tv.setGravity(Gravity.CENTER);
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(dp(c.getResources().getDisplayMetrics().density, 22));
        if (primary) {
            g.setColors(new int[]{0xFF8FB8FF, 0xFF7FA5E6});
            g.setOrientation(GradientDrawable.Orientation.LEFT_RIGHT);
            tv.setTextColor(0xFFFFFFFF);
        } else {
            g.setColor(0xFFFFFFFF);
            g.setStroke(dp(c.getResources().getDisplayMetrics().density, 1), 0x66C9D6E8);
            tv.setTextColor(0xFF7C90AA);
        }
        tv.setBackgroundDrawable(g);
        return tv;
    }

    public static LinearLayout field(Context c, int index, String hint, String value,
                                     String label) {
        float den = c.getResources().getDisplayMetrics().density;
        LinearLayout col = new LinearLayout(c);
        col.setOrientation(LinearLayout.VERTICAL);

        LinearLayout tagRow = new LinearLayout(c);
        tagRow.setOrientation(LinearLayout.HORIZONTAL);
        tagRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView num = new TextView(c);
        num.setText(String.valueOf(index));
        num.setTextSize(10);
        num.setTextColor(0xFFFFFFFF);
        num.setGravity(Gravity.CENTER);
        num.setTypeface(android.graphics.Typeface.DEFAULT_BOLD,
                android.graphics.Typeface.BOLD);
        GradientDrawable ng = new GradientDrawable();
        ng.setShape(GradientDrawable.OVAL);
        ng.setColor(C_BLUE);
        num.setBackgroundDrawable(ng);
        tagRow.addView(num, new LinearLayout.LayoutParams(
                dp(den, 17), dp(den, 17)));
        if (label != null && label.length() > 0) {
            TextView lt = UiKit.text(c, label, 11, C_HINT, false, 0);
            LinearLayout.LayoutParams ltp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            ltp.leftMargin = dp(den, 6);
            tagRow.addView(lt, ltp);
        }
        LinearLayout.LayoutParams trp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        trp.bottomMargin = dp(den, 5);
        col.addView(tagRow, trp);

        android.widget.EditText et = new android.widget.EditText(c);
        et.setText(value == null ? "" : value);
        et.setHint(hint == null ? "" : hint);
        et.setTextSize(14);
        et.setTextColor(C_BODY);
        et.setHintTextColor(0xFFB3C0D2);
        et.setSingleLine(false);
        et.setMaxLines(3);
        et.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
                | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        et.setPadding(dp(den, 12), dp(den, 10), dp(den, 12), dp(den, 10));
        et.setTag("catField");
        setFieldBg(et, false);
        et.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override public void onFocusChange(View v, boolean has) {
                setFieldBg(v, has);
            }
        });
        col.addView(et, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        return col;
    }

    public static android.widget.EditText fieldEdit(View fieldBox) {
        if (fieldBox instanceof android.widget.EditText)
            return (android.widget.EditText) fieldBox;
        if (fieldBox instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) fieldBox;
            for (int i = 0; i < g.getChildCount(); i++) {
                android.widget.EditText e = fieldEdit(g.getChildAt(i));
                if (e != null) return e;
            }
        }
        return null;
    }

    private static void setFieldBg(View v, boolean focus) {
        float den = v.getContext().getResources().getDisplayMetrics().density;
        GradientDrawable g = new GradientDrawable();
        g.setColor(focus ? 0xFFFFFFFF : 0xF2F6FAFF);
        g.setCornerRadius(dp(den, 12));
        g.setStroke(dp(den, focus ? 2 : 1),
                focus ? C_PINK : 0x55C9D6E8);
        v.setBackgroundDrawable(g);
    }

    public static LinearLayout fieldBox(Context c) {
        float den = c.getResources().getDisplayMetrics().density;
        LinearLayout box = new LinearLayout(c);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(den, 16), dp(den, 12), dp(den, 16), dp(den, 4));
        return box;
    }

    public static View emptyState(Context c, String msg) {
        float den = c.getResources().getDisplayMetrics().density;
        LinearLayout box = new LinearLayout(c);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setPadding(dp(den, 16), dp(den, 30), dp(den, 16), dp(den, 30));
        android.graphics.Bitmap cat = Images.get("cat_toast");
        if (cat != null) {
            ImageView iv = new ImageView(c);
            iv.setImageBitmap(cat);
            iv.setAdjustViewBounds(true);
            iv.setAlpha(0.55f);
            box.addView(iv, new LinearLayout.LayoutParams(dp(den, 46), dp(den, 41)));
        }
        TextView tv = UiKit.text(c, msg, 13, C_HINT, false, 0);
        tv.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        tp.topMargin = dp(den, 8);
        box.addView(tv, tp);
        return box;
    }

    private static Activity act(Context c) {
        if (c instanceof Activity) {
            Activity a = (Activity) c;
            if (!a.isFinishing()) return a;
        }
        Activity top = com.cathelperawa.owo.app.bridge.QQEnv.topActivity();
        if (top != null && !top.isFinishing()) return top;
        return null;
    }

    private static int dp(float den, float v) {
        return (int) (v * den + 0.5f);
    }
}
