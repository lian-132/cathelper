package com.cathelperawa.owo.app.ui;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.cathelperawa.owo.app.bridge.QQEnv;
import com.cathelperawa.owo.app.core.HookLog;

public final class ScriptDialog {

    private ScriptDialog() {}

    public static void show(final String title, final ScriptScreen.Handler h) {
        if (h == null) return;
        final Activity a = topActivity();
        if (a == null) {
            HookLog.w("[弹窗] 拿不到 Activity（TIM 不在前台），本次不弹");
            return;
        }
        a.runOnUiThread(new Runnable() {
            @Override public void run() {
                try {
                    buildAndShow(a, title, h);
                } catch (Throwable t) {
                    HookLog.e("[弹窗] 构建失败", t);
                }
            }
        });
    }

    private static Activity topActivity() {
        try {
            return QQEnv.topActivity();
        } catch (Throwable t) {
            return null;
        }
    }

    private static void buildAndShow(final Activity a, String title,
                                     ScriptScreen.Handler h) {
        final Dialog d = new Dialog(a, android.R.style.Theme_Translucent_NoTitleBar);
        ScriptUi ui = new ScriptUi(a);

        FrameLayout mask = new FrameLayout(a);
        mask.setBackgroundColor(0x99000000);
        mask.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                try { d.dismiss(); } catch (Throwable ignore) {}
            }
        });

        LinearLayout card = new LinearLayout(a);
        card.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable g = new GradientDrawable();
        g.setColor(0xF7FFFFFF);
        g.setCornerRadius(ui.dp(22));
        card.setBackgroundDrawable(g);
        card.setPadding(ui.dp(18), ui.dp(16), ui.dp(18), ui.dp(16));

        card.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { }
        });

        LinearLayout bar = new LinearLayout(a);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        TextView t = ui.text(title == null || title.length() == 0 ? "" : title,
                18, "#1F2A3A", true);
        bar.addView(t, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        TextView x = ui.text("✕", 17, "#9AA8BC", false);
        x.setPadding(ui.dp(12), ui.dp(4), ui.dp(4), ui.dp(4));
        x.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                try { d.dismiss(); } catch (Throwable ignore) {}
            }
        });
        bar.addView(x);
        card.addView(bar, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        View line = new View(a);
        line.setBackgroundColor(0x14000000);
        LinearLayout.LayoutParams lineLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, Math.max(1, ui.dp(0.6f)));
        lineLp.topMargin = ui.dp(10);
        lineLp.bottomMargin = ui.dp(10);
        card.addView(line, lineLp);

        ScriptScreen.Result r = new ScriptScreen.Result();
        h.build(ui, r);
        View content = r.get();
        if (content != null) {
            ScrollView sv = new ScrollView(a);
            sv.setVerticalScrollBarEnabled(false);
            sv.addView(content, new ScrollView.LayoutParams(
                    ScrollView.LayoutParams.MATCH_PARENT,
                    ScrollView.LayoutParams.WRAP_CONTENT));
            card.addView(sv, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
        }

        int screenW = a.getResources().getDisplayMetrics().widthPixels;
        int screenH = a.getResources().getDisplayMetrics().heightPixels;
        FrameLayout.LayoutParams cardLp = new FrameLayout.LayoutParams(
                (int) (screenW * 0.86f), FrameLayout.LayoutParams.WRAP_CONTENT);
        cardLp.gravity = Gravity.CENTER;
        card.setLayoutParams(cardLp);
        mask.addView(card);

        d.setContentView(mask, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        d.setCanceledOnTouchOutside(true);
        d.show();
    }
}
