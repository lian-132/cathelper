package com.cathelperawa.owo.app.ui;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

import com.cathelperawa.owo.app.bridge.QQEnv;
import com.cathelperawa.owo.app.core.HookConfig;

public final class QqPanel {

    private QqPanel() {}

    public static void show(Context ctx) {
        Activity a = QQEnv.topActivity();
        if (a == null && ctx instanceof Activity) a = (Activity) ctx;
        if (a == null) return;
        final Activity act = a;
        final float d = act.getResources().getDisplayMetrics().density;

        Dialog dlg = new Dialog(act, android.R.style.Theme_DeviceDefault_Light_NoActionBar);
        LinearLayout root = new LinearLayout(act);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFFF3F6FB);

        LinearLayout bar = new LinearLayout(act);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setBackgroundColor(0xFFFFFFFF);
        int hp = (int) (14 * d);
        bar.setPadding(hp, (int) (14 * d), hp, (int) (14 * d));

        TextView back = new TextView(act);
        back.setText("‹");
        back.setTextSize(TypedValue.COMPLEX_UNIT_SP, 26);
        back.setTextColor(0xFF2F3E56);
        back.setPadding(0, 0, (int) (12 * d), 0);
        back.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { dlg.dismiss(); }
        });
        bar.addView(back);

        TextView title = new TextView(act);
        title.setText("猫娘助手");
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        title.setTextColor(0xFF2F3E56);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);
        bar.addView(title, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        
        TextView spacer = new TextView(act);
        spacer.setText("  ");
        spacer.setTextSize(TypedValue.COMPLEX_UNIT_SP, 26);
        bar.addView(spacer);
        root.addView(bar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        ScrollView sv = new ScrollView(act);
        LinearLayout body = new LinearLayout(act);
        body.setOrientation(LinearLayout.VERTICAL);
        int bp = (int) (16 * d);
        body.setPadding(bp, bp, bp, bp);

        TextView sec = new TextView(act);
        sec.setText("功能");
        sec.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        sec.setTextColor(0xFF98A6BC);
        sec.setPadding((int) (6 * d), 0, 0, (int) (8 * d));
        body.addView(sec);

        LinearLayout card = new LinearLayout(act);
        card.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable cg = new GradientDrawable();
        cg.setColor(0xFFFFFFFF);
        cg.setCornerRadius(14 * d);
        card.setBackgroundDrawable(cg);
        card.setPadding(bp, (int) (4 * d), bp, (int) (4 * d));

        addSwitch(act, card, d, "抓卡 · 长按提取", "hook_capture", true, 0);
        addSwitch(act, card, d, "发卡 · 发送与协议", "hook_send_proto", true, 1);
        addSwitch(act, card, d, "防风控 · 掐断上报", "anti_detect", true, 2);

        body.addView(card);
        sv.addView(body);
        root.addView(sv, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        dlg.setContentView(root);
        Window w = dlg.getWindow();
        if (w != null) {
            w.setLayout(WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT);
        }
        try {
            dlg.show();
        } catch (Throwable t) {
            com.cathelperawa.owo.app.core.HookLog.w("QqPanel.show 失败 " + t);
        }
    }

    private static void addSwitch(Activity act, LinearLayout parent, final float d,
                                  String label, final String key, boolean def, int idx) {
        LinearLayout row = new LinearLayout(act);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, (int) (14 * d), 0, (int) (14 * d));

        TextView t = new TextView(act);
        t.setText(label);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        t.setTextColor(0xFF2F3E56);
        row.addView(t, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        final Switch sw = new Switch(act);
        boolean on;
        try {
            on = HookConfig.getBoolean(act, key, def);
        } catch (Throwable e) {
            on = def;
        }
        sw.setChecked(on);
        sw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton b, boolean v) {
                try {
                    HookConfig.set(act, key, v ? "1" : "0");
                    HookConfig.reload();
                } catch (Throwable ignore) {
                }
            }
        });
        row.addView(sw);
        parent.addView(row);

        if (idx < 2) {
            View line = new View(act);
            GradientDrawable g = new GradientDrawable();
            g.setColor(0xFFEDF2FA);
            line.setBackgroundDrawable(g);
            parent.addView(line, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, (int) d)));
        }
    }
}
