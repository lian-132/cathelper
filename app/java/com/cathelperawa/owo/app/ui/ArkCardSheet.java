package com.cathelperawa.owo.app.ui;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.cathelperawa.owo.app.bridge.CatToast;
import com.cathelperawa.owo.app.core.Bg;
import com.cathelperawa.owo.app.feature.CardKit;

import java.util.HashMap;
import java.util.Map;

public final class ArkCardSheet {

    private static final String PREF = "ark_card_pref";
    private static final int C_TEXT = 0xFF2A2F3A;
    private static final int C_SUB = 0xFF8A8FA0;
    private static final int C_LABEL = 0xFF9AA0B0;
    private static final int C_CARD = 0xFFF6F4FF;
    private static final int C_INPUT = 0xFFFFFFFF;
    private static final int C_PINK = 0xFFF45FA0;

    private ArkCardSheet() {}

    public static void show(final Activity activity, final CardKit.Spec spec) {
        if (activity == null || spec == null) return;
        final float f = activity.getResources().getDisplayMetrics().density;
        final SharedPreferences sp = activity.getSharedPreferences(PREF, Context.MODE_PRIVATE);

        final Dialog dialog = new Dialog(activity, android.R.style.Theme_Translucent_NoTitleBar);
        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(1);
        root.setBackgroundColor(0xB3000000);
        int pad = dp(f, 16);
        root.setPadding(pad, dp(f, 60), pad, dp(f, 28));

        LinearLayout card = new LinearLayout(activity);
        card.setOrientation(1);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(C_CARD);
        bg.setCornerRadius(dp(f, 24));
        card.setBackgroundDrawable(bg);
        card.setPadding(dp(f, 20), dp(f, 20), dp(f, 20), dp(f, 18));

        LinearLayout head = new LinearLayout(activity);
        head.setOrientation(0);
        head.setGravity(android.view.Gravity.CENTER_VERTICAL);
        View bar = new View(activity);
        GradientDrawable barBg = new GradientDrawable();
        barBg.setColor(C_PINK);
        barBg.setCornerRadius(dp(f, 2));
        bar.setBackgroundDrawable(barBg);
        LinearLayout.LayoutParams barLp = new LinearLayout.LayoutParams(dp(f, 4), dp(f, 20));
        barLp.rightMargin = dp(f, 10);
        head.addView(bar, barLp);
        TextView title = new TextView(activity);
        title.setText(spec.name);
        title.setTextSize(17.0f);
        title.setTextColor(C_TEXT);
        title.getPaint().setFakeBoldText(true);
        head.addView(title);
        card.addView(head);

        TextView sub = new TextView(activity);
        sub.setText(spec.desc + " · " + spec.cmd);
        sub.setTextSize(11.0f);
        sub.setTextColor(C_SUB);
        sub.setPadding(dp(f, 14), dp(f, 7), 0, dp(f, 4));
        card.addView(sub);

        final Map<String, EditText> inputs = new HashMap<String, EditText>();
        for (CardKit.Field fd : spec.fields) {
            TextView lb = new TextView(activity);
            lb.setText(fd.label + (fd.required ? " *" : ""));
            lb.setTextSize(11.0f);
            lb.setTextColor(C_LABEL);
            lb.setPadding(dp(f, 2), dp(f, 10), 0, dp(f, 5));
            card.addView(lb);

            EditText et = new EditText(activity);
            String saved = sp.getString(spec.key + "_" + fd.key, null);
            et.setText(saved != null ? saved : fd.def);
            et.setHint(fd.hint);
            et.setHintTextColor(0xFFB4B9C7);
            et.setTextSize(13.5f);
            et.setTextColor(0xFF2C3140);
            et.setSingleLine(true);
            et.setInputType(InputType.TYPE_CLASS_TEXT);
            GradientDrawable ib = new GradientDrawable();
            ib.setColor(C_INPUT);
            ib.setCornerRadius(dp(f, 14));
            et.setBackgroundDrawable(ib);
            et.setPadding(dp(f, 14), dp(f, 11), dp(f, 14), dp(f, 11));
            card.addView(et);
            inputs.put(fd.key, et);
        }

        final String[] variants = CardKit.templates(spec);
        final String[] shapeNames = CardKit.shapeNames(spec);
        final int[] vi = {0};
        if (variants.length > 1) {
            final TextView shape = new TextView(activity);
            shape.setText("形态 1/" + variants.length + " · " + shapeNames[0]);
            shape.setTextSize(12.0f);
            shape.setTextColor(0xFFE85D9C);
            shape.setGravity(android.view.Gravity.CENTER);
            GradientDrawable sb = new GradientDrawable();
            sb.setColor(0xFFFFF0F6);
            sb.setCornerRadius(dp(f, 12));
            shape.setBackgroundDrawable(sb);
            shape.setPadding(dp(f, 10), dp(f, 8), dp(f, 10), dp(f, 8));
            LinearLayout.LayoutParams shp = new LinearLayout.LayoutParams(-1, -2);
            shp.topMargin = dp(f, 10);
            card.addView(shape, shp);
            shape.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    vi[0] = (vi[0] + 1) % variants.length;
                    shape.setText("形态 " + (vi[0] + 1) + "/" + variants.length
                            + " · " + shapeNames[Math.min(vi[0], shapeNames.length - 1)]);
                }
            });
        }

        final TextView status = new TextView(activity);
        status.setText("填好点发送，直接发到当前会话");
        status.setTextSize(11.0f);
        status.setTextColor(C_SUB);
        status.setPadding(0, dp(f, 12), 0, 0);
        card.addView(status);

        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(0);
        row.setPadding(0, dp(f, 14), 0, 0);
        int h = dp(f, 46);

        TextView cancel = btn(activity, f, "关闭", 0xFFEDEBF7, 0xFF7B7F8E, false);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                saveDraft(sp, spec, inputs);
                dialog.dismiss();
            }
        });
        row.addView(cancel, new LinearLayout.LayoutParams(0, h, 1.0f));

        final TextView check = btn(activity, f, "校验", 0xFFEDEBF7, 0xFF7B7F8E, false);
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(0, h, 1.15f);
        clp.leftMargin = dp(f, 10);
        row.addView(check, clp);
        check.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                String tpl = variants.length > 0 ? variants[Math.min(vi[0], variants.length - 1)] : spec.tpl;
                if (tpl == null) {
                    status.setTextColor(C_SUB);
                    status.setText("这张卡没有真实模板");
                    return;
                }
                String ark = CardKit.composeWith(spec, tpl, collect(spec, inputs));
                if (ark == null) {
                    status.setTextColor(0xFFE5484D);
                    status.setText("✗ " + (CardKit.lastError == null ? "模板为空" : CardKit.lastError));
                } else {
                    status.setTextColor(0xFF17A673);
                    status.setText("✓ JSON 合法 · " + ark.length() + " 字符 · 形态 "
                            + (vi[0] + 1) + "/" + Math.max(1, variants.length));
                }
            }
        });

        final TextView send = btn(activity, f, "发送卡片", C_PINK, 0xFFFFFFFF, true);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, h, 1.9f);
        lp.leftMargin = dp(f, 10);
        row.addView(send, lp);
        card.addView(row);

        send.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                final Map<String, String> values = collect(spec, inputs);
                SharedPreferences.Editor ed = sp.edit();
                for (CardKit.Field fd : spec.fields) {
                    ed.putString(spec.key + "_" + fd.key, values.get(fd.key));
                }
                ed.apply();

                send.setText("发送中…");
                send.setAlpha(0.6f);
                send.setEnabled(false);
                status.setTextColor(C_SUB);
                status.setText("正在组包…");

                Bg.run(new Runnable() {
                    @Override public void run() {
                        final String r = CardKit.sendWith(spec, variants[vi[0]], values, activity);
                        try {
                            java.io.File f = new java.io.File(
                                    com.cathelperawa.owo.app.bridge.QQEnv.appContext().getFilesDir(),
                                    "card_result.txt");
                            java.io.FileWriter w = new java.io.FileWriter(f, false);
                            w.write("spec=" + spec.key + "\nchannel=" + spec.channel
                                    + "\nresult=" + r
                                    + "\npeers=" + com.cathelperawa.owo.app.feature.CardKit.peer()
                                    + "\nchatType=" + com.cathelperawa.owo.app.bridge.CurrentChat.chatType()
                                    + "\ndiag=" + com.cathelperawa.owo.app.ovo.QQInterfaces.diag());
                            w.close();
                        } catch (Throwable ignore) {
                        }
                        activity.runOnUiThread(new Runnable() {
                            @Override public void run() {
                                send.setText("发送卡片");
                                send.setAlpha(1.0f);
                                send.setEnabled(true);
                                if (r != null && r.startsWith("ok")) {
                                    status.setTextColor(0xFF17A673);
                                    status.setText("✓ 已发送");
                                    dialog.dismiss();
                                    CatToast.show("已发送");
                                } else {
                                    status.setTextColor(0xFFE5484D);
                                    status.setText("✗ " + r);
                                }
                            }
                        });
                    }
                });
            }
        });

        ScrollView scroll = new ScrollView(activity);
        scroll.addView(card);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1.0f));
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override public void onDismiss(DialogInterface d) {
                saveDraft(sp, spec, inputs);
            }
        });
        dialog.setContentView(root);
        Window w = dialog.getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(0));
            w.setLayout(-1, -1);
        }
        dialog.show();
    }

    
    private static Map<String, String> collect(CardKit.Spec spec, Map<String, EditText> inputs) {
        Map<String, String> values = new HashMap<String, String>();
        for (CardKit.Field fd : spec.fields) {
            EditText et = inputs.get(fd.key);
            String val = et == null || et.getText() == null ? "" : et.getText().toString().trim();
            if (val.length() == 0) {
                val = fd.def != null && fd.def.length() > 0 ? fd.def : defaultFor(fd.key, spec);
            }
            values.put(fd.key, val);
        }
        return values;
    }

    private static String defaultFor(String key, CardKit.Spec spec) {
        if ("title".equals(key)) return spec.name;
        if ("desc".equals(key)) return " ";
        if ("prompt".equals(key)) return spec.name;
        if ("pic".equals(key)) return "https://p.qlogo.cn/gdynamic/MIwbbVhjoVxibNJPGsv8JgNUvNicibdgnRe73ygUKmb104/0";
        if ("appid".equals(key)) return "1109937557";
        if ("web".equals(key)) return "pages/index/index";
        return "";
    }

    private static void saveDraft(SharedPreferences sp, CardKit.Spec spec, Map<String, EditText> inputs) {
        try {
            SharedPreferences.Editor ed = sp.edit();
            for (CardKit.Field fd : spec.fields) {
                EditText et = inputs.get(fd.key);
                if (et != null && et.getText() != null) {
                    ed.putString(spec.key + "_" + fd.key, et.getText().toString());
                }
            }
            ed.apply();
        } catch (Throwable ignore) {
        }
    }

    private static int dp(float f, int v) { return (int) (f * v); }

    private static TextView btn(Activity a, float f, String text, int color, int textColor, boolean bold) {
        TextView tv = new TextView(a);
        tv.setText(text);
        tv.setTextSize(14.5f);
        tv.setTextColor(textColor);
        tv.setGravity(android.view.Gravity.CENTER);
        if (bold) tv.getPaint().setFakeBoldText(true);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(color);
        bg.setCornerRadius(dp(f, 15));
        tv.setBackgroundDrawable(bg);
        return tv;
    }
}
