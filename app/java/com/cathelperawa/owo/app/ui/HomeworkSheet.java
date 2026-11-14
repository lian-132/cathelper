package com.cathelperawa.owo.app.ui;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.cathelperawa.owo.app.bridge.CatToast;
import com.cathelperawa.owo.app.core.HookConfig;
import com.cathelperawa.owo.app.feature.OidbCard;

public final class HomeworkSheet {

    private HomeworkSheet() {
    }

    public static final class Params {
        public String title = "";
        public String desc = "";
        public String tag = "班级作业";
        public String jump = "";
        public String preview = "";
    }

    public interface OnDone {
        void run(Params p);
    }

    private static final int C_TITLE = 0xFF3C4E6B;
    private static final int C_SUB = 0xFF8A9CB5;

    private static final String K_TITLE = "hw_title";
    private static final String K_DESC = "hw_desc";
    private static final String K_TAG = "hw_tag";
    private static final String K_JUMP = "hw_jump";
    private static final String K_PREVIEW = "hw_preview";
    private static final String K_ICON_ONLY = "hw_icon_only";

    private static final String ICON_STYLE_PREVIEW =
            "http://p.qpic.cn/qqconadmin/0/8a0b8bcb4d512461014d527fc2520002/0";

    public static void open(final Activity act, final OnDone cb) {
        if (act == null) return;
        final float d = act.getResources().getDisplayMetrics().density;

        final Dialog dlg = new Dialog(act, android.R.style.Theme_Translucent_NoTitleBar);
        LinearLayout card = new LinearLayout(act);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding((int) (18 * d), (int) (16 * d), (int) (18 * d), (int) (14 * d));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFAFFFFFF);
        bg.setCornerRadius(24 * d);
        bg.setStroke((int) d, 0xA6FFFFFF);
        card.setBackgroundDrawable(bg);

        TextView title = new TextView(act);
        title.setText("发送班级作业卡");
        title.setTextSize(17);
        title.setTextColor(C_TITLE);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tlp.bottomMargin = (int) (4 * d);
        card.addView(title, tlp);

        TextView tip = new TextView(act);
        tip.setText("OIDB 0xdc2_34 · 不用签名 · 发到当前群（跳转/预览图留空会填默认值）");
        tip.setTextSize(11);
        tip.setTextColor(C_SUB);
        LinearLayout.LayoutParams tlp2 = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tlp2.bottomMargin = (int) (10 * d);
        card.addView(tip, tlp2);

        final EditText etTitle = field(act, d, "标题", get(act, K_TITLE, ""), false);
        final EditText etDesc = field(act, d, "简介", get(act, K_DESC, ""), false);
        final EditText etTag = field(act, d, "角标文字（默认「班级作业」）", get(act, K_TAG, "班级作业"), false);
        final EditText etJump = field(act, d, "跳转链接", get(act, K_JUMP, ""), false);
        final EditText etPreview = field(act, d, "预览图链接", get(act, K_PREVIEW, ""), false);

        card.addView(etTitle, fieldLp(d));
        card.addView(etDesc, fieldLp(d));
        card.addView(etTag, fieldLp(d));
        card.addView(etJump, fieldLp(d));
        card.addView(etPreview, fieldLp(d));

        boolean iconOnlyInit = false;
        try {
            iconOnlyInit = HookConfig.getBoolean(act, K_ICON_ONLY, false);
        } catch (Throwable ignore) {
        }
        LinearLayout iconRow = new LinearLayout(act);
        iconRow.setOrientation(LinearLayout.HORIZONTAL);
        iconRow.setGravity(Gravity.CENTER_VERTICAL);
        final TextView iconLabel = new TextView(act);
        iconLabel.setText(iconOnlyInit
                ? "应用图标样式（不显示预览图）· 开"
                : "应用图标样式（不显示预览图）· 关");
        iconLabel.setTextSize(12);
        iconLabel.setTextColor(C_TITLE);
        iconRow.addView(iconLabel, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        final com.cathelperawa.owo.app.ui.SwitchBox swIcon =
                new com.cathelperawa.owo.app.ui.SwitchBox(act, iconOnlyInit);
        swIcon.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                boolean now = !HookConfig.getBoolean(act, K_ICON_ONLY, false);
                try {
                    HookConfig.set(act, K_ICON_ONLY, now ? "1" : "0");
                } catch (Throwable ignore) {
                }
                swIcon.setChecked(now);
                iconLabel.setText(now
                        ? "应用图标样式（不显示预览图）· 开"
                        : "应用图标样式（不显示预览图）· 关");
            }
        });
        iconRow.addView(swIcon, new LinearLayout.LayoutParams(
                (int) (44 * d), (int) (26 * d)));
        LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ilp.bottomMargin = (int) (8 * d);
        card.addView(iconRow, ilp);

        LinearLayout row = new LinearLayout(act);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        TextView cancel = pill(act, d, "取消", C_SUB, 0x59FFFFFF);
        TextView send = pill(act, d, "发送", 0xFFFFFFFF, 0xFFF0709F);
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.45f);
        clp.rightMargin = (int) (8 * d);
        row.addView(cancel, clp);
        row.addView(send, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rlp.topMargin = (int) (6 * d);
        card.addView(row, rlp);

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                dlg.dismiss();
            }
        });
        send.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                Params p = new Params();
                p.title = text(etTitle);
                p.desc = text(etDesc);
                p.tag = text(etTag);
                p.jump = text(etJump);
                p.preview = text(etPreview);
                if (p.title.length() == 0) p.title = "班级作业";
                if (p.tag.length() == 0) p.tag = "班级作业";

                if (p.jump.length() == 0) p.jump = "https://qq.com";

                boolean iconOnly = false;
                try {
                    iconOnly = HookConfig.getBoolean(act, K_ICON_ONLY, false);
                } catch (Throwable ignore) {
                }
                if (iconOnly) {
                    p.preview = ICON_STYLE_PREVIEW;
                } else if (p.preview.length() == 0) {
                    p.preview = defaultPreview();
                }

                HookConfig.set(act, K_TITLE, p.title);
                HookConfig.set(act, K_DESC, p.desc);
                HookConfig.set(act, K_TAG, p.tag);
                HookConfig.set(act, K_JUMP, p.jump);
                HookConfig.set(act, K_PREVIEW, p.preview);
                dlg.dismiss();
                if (cb != null) cb.run(p);
            }
        });

        dlg.setContentView(card);
        dlg.setCanceledOnTouchOutside(true);
        dlg.show();
        Window w = dlg.getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowManager.LayoutParams lp = w.getAttributes();
            int sw = act.getResources().getDisplayMetrics().widthPixels;
            lp.width = (int) (sw * 0.90f);
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
            lp.gravity = Gravity.CENTER;
            lp.dimAmount = 0.26f;
            w.setAttributes(lp);
        }
    }

    public static String buildPb(Params p) {
        if (p == null) return null;
        long uin = OidbCard.currentGroupUin();
        return OidbCard.build(OidbCard.DEFAULT_APPID, uin, p.title, p.desc,
                p.tag, p.jump, p.preview);
    }

    private static String defaultPreview() {
        try {
            String uin = com.cathelperawa.owo.app.bridge.QQEnv.currentUin();
            if (uin != null && uin.length() > 0) {
                return "https://q1.qlogo.cn/g?b=qq&nk=" + uin + "&s=100";
            }
        } catch (Throwable ignore) {
        }
        return "https://tianxuan.gtimg.cn/47329_bd95e16e/assets/guild-icon.png";
    }

    private static String get(Activity act, String key, String def) {
        try {
            String v = HookConfig.get(act, key, null);
            return v == null ? def : v;
        } catch (Throwable t) {
            return def;
        }
    }

    private static String text(EditText e) {
        return e.getText() == null ? "" : e.getText().toString().trim();
    }

    private static EditText field(Activity act, float d, String hint, String value,
                                  boolean multi) {
        EditText et = new EditText(act);
        et.setHint(hint);
        et.setText(value == null ? "" : value);
        et.setTextSize(13);
        et.setTextColor(C_TITLE);
        et.setHintTextColor(0xFFB3C0D2);
        et.setSingleLine(!multi);
        if (!multi) et.setInputType(InputType.TYPE_CLASS_TEXT);
        GradientDrawable g = new GradientDrawable();
        g.setColor(0xB3FFFFFF);
        g.setCornerRadius(12 * d);
        g.setStroke((int) d, 0x66C9D6E8);
        et.setBackgroundDrawable(g);
        et.setPadding((int) (12 * d), (int) (9 * d), (int) (12 * d), (int) (9 * d));
        return et;
    }

    private static LinearLayout.LayoutParams fieldLp(float d) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = (int) (7 * d);
        return lp;
    }

    private static TextView pill(Activity act, float d, String label, int color, int bgColor) {
        TextView t = new TextView(act);
        t.setText(label);
        t.setTextSize(14);
        t.setTextColor(color);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setGravity(Gravity.CENTER);
        t.setPadding(0, (int) (12 * d), 0, (int) (12 * d));
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(14 * d);
        g.setColor(bgColor);
        g.setStroke((int) d, 0x66C9D6E8);
        t.setBackgroundDrawable(g);
        return t;
    }
}
