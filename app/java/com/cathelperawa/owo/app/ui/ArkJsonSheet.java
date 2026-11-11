package com.cathelperawa.owo.app.ui;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.view.View;
import android.view.Window;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.cathelperawa.owo.app.bridge.CatToast;
import com.cathelperawa.owo.app.bridge.CurrentChat;
import com.cathelperawa.owo.app.core.Bg;
import com.cathelperawa.owo.app.feature.CardKit;

public final class ArkJsonSheet {

    private static final String PREF = "ark_json_pref";
    private static final int C_TEXT = 0xFF2A2F3A;
    private static final int C_SUB = 0xFF8A8FA0;
    private static final int C_CARD = 0xFFF6F4FF;
    private static final int C_PINK = 0xFFF45FA0;
    private static final String DEF_REFER = "https://x.jq.qq.com/";

    private ArkJsonSheet() {}

    public static void show(final Activity activity, final CardKit.Spec spec) {
        if (activity == null) return;
        final float f = activity.getResources().getDisplayMetrics().density;
        final SharedPreferences sp = activity.getSharedPreferences(PREF, Context.MODE_PRIVATE);

        final Dialog dialog = new Dialog(activity, android.R.style.Theme_Translucent_NoTitleBar);
        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(1);
        root.setBackgroundColor(0xB3000000);
        int pad = (int) (f * 16);
        root.setPadding(pad, (int) (f * 54), pad, (int) (f * 24));

        LinearLayout card = new LinearLayout(activity);
        card.setOrientation(1);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(C_CARD);
        bg.setCornerRadius(f * 24);
        card.setBackgroundDrawable(bg);
        card.setPadding(pad, pad, pad, (int) (f * 14));

        LinearLayout head = new LinearLayout(activity);
        head.setOrientation(0);
        head.setGravity(android.view.Gravity.CENTER_VERTICAL);
        View bar = new View(activity);
        GradientDrawable barBg = new GradientDrawable();
        barBg.setColor(C_PINK);
        barBg.setCornerRadius(f * 2);
        bar.setBackgroundDrawable(barBg);
        LinearLayout.LayoutParams barLp = new LinearLayout.LayoutParams((int) (f * 4), (int) (f * 20));
        barLp.rightMargin = (int) (f * 10);
        head.addView(bar, barLp);
        TextView title = new TextView(activity);
        title.setText(spec == null ? "超级 ARK 工具箱" : spec.name);
        title.setTextSize(17.0f);
        title.setTextColor(C_TEXT);
        title.getPaint().setFakeBoldText(true);
        head.addView(title);
        card.addView(head);

        TextView sub = new TextView(activity);
        int ct = 0;
        String peer = "";
        try {
            ct = CurrentChat.chatType();
            peer = CurrentChat.peerUid();
        } catch (Throwable ignore) {
        }
        sub.setText("当前会话：" + (peer == null || peer.length() == 0 ? "未知" : peer)
                + (ct == 2 ? " | 群聊" : ct == 1 ? " | 好友" : "")
                + "\n接口：trpc.gamecenter.sendmsg.Sendmsg.SsoArkSign");
        sub.setTextSize(11.0f);
        sub.setTextColor(C_SUB);
        sub.setPadding(0, (int) (f * 10), 0, (int) (f * 6));
        card.addView(sub);

        card.addView(label(activity, f, "ARK JSON"));
        final EditText input = edit(activity, f, true);
        final String[] variants = CardKit.templates(spec);
        final int[] vi = {0};
        String saved = spec == null ? null : sp.getString("k_" + spec.key, null);
        input.setText(saved != null ? saved : variants[0]);
        card.addView(input, new LinearLayout.LayoutParams(
                -1, (int) (activity.getResources().getDisplayMetrics().heightPixels * 0.26f)));

        card.addView(label(activity, f, "Refer URL"));
        final EditText refer = edit(activity, f, false);
        refer.setSingleLine(true);
        refer.setHint("例如：" + DEF_REFER);
        String savedRefer = sp.getString("refer", "");
        refer.setText(savedRefer.length() > 0 ? savedRefer : DEF_REFER);
        card.addView(refer, new LinearLayout.LayoutParams(-1, (int) (f * 46)));

        card.addView(label(activity, f, "Cookie"));
        final EditText cookie = edit(activity, f, true);
        cookie.setHint("可留空；留空自动取 uin/skey/p_skey");
        cookie.setText(sp.getString("cookie", ""));
        card.addView(cookie, new LinearLayout.LayoutParams(-1, (int) (f * 76)));

        card.addView(label(activity, f, "Option JSON（可选）"));
        final EditText option = edit(activity, f, true);
        option.setHint("可留空；留空用真机设备指纹");
        option.setText(sp.getString("option", ""));
        card.addView(option, new LinearLayout.LayoutParams(-1, (int) (f * 76)));

        final CheckBox dry = check(activity, f, "仅查看签名响应（不发送）", false);
        card.addView(dry);
        final CheckBox clearAfter = check(activity, f, "发送后自动清空 ARK 输入框", false);
        card.addView(clearAfter);

        final TextView status = new TextView(activity);
        status.setText("改完点「请求 SSOARKSIGN」；ARK 留空则按模板发");
        status.setTextSize(11.0f);
        status.setTextColor(C_SUB);
        status.setPadding(0, (int) (f * 8), 0, 0);
        card.addView(status);

        int h = (int) (f * 44);
        LinearLayout row0 = new LinearLayout(activity);
        row0.setOrientation(0);
        row0.setPadding(0, (int) (f * 10), 0, 0);

        final TextView reset = btn(activity, f,
                variants.length > 1 ? "形态 1/" + variants.length : "重置",
                0xFFEDEBF7, 0xFF7B7F8E, false);
        reset.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (variants.length > 1) {
                    vi[0] = (vi[0] + 1) % variants.length;
                    input.setText(variants[vi[0]]);
                    reset.setText("形态 " + (vi[0] + 1) + "/" + variants.length);
                } else {
                    input.setText(variants[0]);
                    CatToast.show("已恢复模板");
                }
            }
        });
        row0.addView(reset, new LinearLayout.LayoutParams(0, h, 1.4f));

        TextView clear = btn(activity, f, "清空 ARK", 0xFFEDEBF7, 0xFF7B7F8E, false);
        clear.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { input.setText(""); }
        });
        LinearLayout.LayoutParams clrp = new LinearLayout.LayoutParams(0, h, 1.0f);
        clrp.leftMargin = (int) (f * 8);
        row0.addView(clear, clrp);
        card.addView(row0);

        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(0);
        row.setPadding(0, (int) (f * 8), 0, 0);

        TextView cancel = btn(activity, f, "关闭", 0xFFEDEBF7, 0xFF7B7F8E, false);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { dialog.dismiss(); }
        });
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(0, h, 1.0f);
        clp.leftMargin = (int) (f * 8);
        row.addView(cancel, clp);

        final TextView send = btn(activity, f, "请求 SSOARKSIGN", C_PINK, 0xFFFFFFFF, true);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(0, h, 1.8f);
        slp.leftMargin = (int) (f * 8);
        row.addView(send, slp);
        card.addView(row);

        send.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { doSend(activity, f, input, refer, cookie, option,
                    dry, clearAfter, status, send, sp,
                    variants, vi); }
        });

        ScrollView scroll = new ScrollView(activity);
        scroll.addView(card);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1.0f));
        dialog.setContentView(root);
        Window w = dialog.getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(0));
            w.setLayout(-1, -1);
        }
        dialog.show();
    }

    

    private static void doSend(final Activity activity, final float f,
                               final EditText input, final EditText refer,
                               final EditText cookie, final EditText option,
                               final CheckBox dry, final CheckBox clearAfter,
                               final TextView status, final TextView send,
                               final SharedPreferences sp,
                               final String[] variants, final int[] vi) {
        final String ark = input.getText().toString().trim();
        if (ark.length() == 0) {
            CatToast.show("先粘一段 ARK JSON");
            return;
        }
        sp.edit().putString("refer", refer.getText().toString().trim())
                .putString("cookie", cookie.getText().toString().trim())
                .putString("option", option.getText().toString().trim()).apply();

        send.setText("请求中…");
        send.setAlpha(0.6f);
        send.setEnabled(false);
        status.setTextColor(C_SUB);
        status.setText("正在请求 SsoArkSign…");

        Bg.run(new Runnable() {
            @Override public void run() {
                final String[] msg = new String[1];
                final boolean[] good = {false};
                try {
                    com.cathelperawa.owo.app.feature.SsoArkSign.Result r =
                            com.cathelperawa.owo.app.feature.SsoArkSign.sign(
                                    ark,
                                    refer.getText().toString().trim(),
                                    cookie.getText().toString().trim(),
                                    option.getText().toString().trim());
                    if (r != null && r.ok()) {
                        if (dry.isChecked()) {
                            msg[0] = "签名成功，未发送（已复制到状态栏下方日志）";
                            com.cathelperawa.owo.app.core.HookLog.i("ArkSheet\n" + r.json);
                        } else {
                            SendPanel.sendArkJson(activity, r.json);
                            msg[0] = "已签名并发送";
                        }
                        good[0] = true;
                    } else {
                        msg[0] = "失败：" + (r == null ? "无返回" : r.error);
                        if (r != null && r.raw != null && r.raw.length() > 0) {
                            com.cathelperawa.owo.app.core.HookLog.i("ArkSheet.raw\n" + r.raw);
                        }
                    }
                } catch (Throwable t) {
                    msg[0] = "失败：" + t;
                }
                final boolean okk = good[0];
                activity.runOnUiThread(new Runnable() {
                    @Override public void run() {
                        send.setText("请求 SSOARKSIGN");
                        send.setAlpha(1.0f);
                        send.setEnabled(true);
                        if (okk) {
                            status.setTextColor(0xFF17A673);
                            status.setText("✓ " + msg[0]);
                            if (clearAfter.isChecked() && !dry.isChecked()) {
                                input.setText("");
                            }
                        } else {
                            status.setTextColor(0xFFE5484D);
                            status.setText("✗ " + msg[0]);
                        }
                    }
                });
            }
        });
    }

    

    private static TextView label(Activity a, float f, String s) {
        TextView t = new TextView(a);
        t.setText(s);
        t.setTextSize(11.5f);
        t.setTextColor(C_SUB);
        t.setPadding(0, (int) (f * 8), 0, (int) (f * 5));
        return t;
    }

    private static EditText edit(Activity a, float f, boolean multi) {
        EditText e = new EditText(a);
        e.setTextSize(12.0f);
        e.setTextColor(0xFF2C3140);
        e.setTypeface(android.graphics.Typeface.MONOSPACE);
        e.setGravity(android.view.Gravity.TOP | android.view.Gravity.START);
        if (multi) {
            e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        } else {
            e.setInputType(InputType.TYPE_CLASS_TEXT);
        }
        e.setPadding((int) (f * 12), (int) (f * 10), (int) (f * 12), (int) (f * 10));
        GradientDrawable ib = new GradientDrawable();
        ib.setColor(0xFFFFFFFF);
        ib.setCornerRadius(f * 14);
        e.setBackgroundDrawable(ib);
        return e;
    }

    private static CheckBox check(Activity a, float f, String s, boolean def) {
        CheckBox c = new CheckBox(a);
        c.setText(s);
        c.setTextSize(11.5f);
        c.setTextColor(0xFF5B6479);
        c.setChecked(def);
        c.setPadding(0, (int) (f * 4), 0, (int) (f * 4));
        return c;
    }

    private static TextView btn(Activity a, float f, String text, int color, int textColor, boolean bold) {
        TextView tv = new TextView(a);
        tv.setText(text);
        tv.setTextSize(14.0f);
        tv.setTextColor(textColor);
        tv.setGravity(android.view.Gravity.CENTER);
        if (bold) tv.getPaint().setFakeBoldText(true);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(color);
        bg.setCornerRadius(f * 15);
        tv.setBackgroundDrawable(bg);
        return tv;
    }
}
