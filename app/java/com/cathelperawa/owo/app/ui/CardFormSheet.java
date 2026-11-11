package com.cathelperawa.owo.app.ui;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
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
import android.widget.ScrollView;
import android.widget.TextView;

import com.cathelperawa.owo.app.bridge.CatToast;

import org.json.JSONObject;

public final class CardFormSheet {

    private CardFormSheet() {}

    public static volatile boolean noSign = false;

    public static volatile com.cathelperawa.owo.app.feature.CardTemplates.Tpl pickedTpl = null;

    public static volatile String pendingArk = null;

    public interface OnDone {
        void run(String title, String desc, String pic, String jump, String tag, String tagIcon);
    }

    private static final int C_TITLE = 0xFF3C4E6B;
    private static final int C_SUB = 0xFF8A9CB5;

    public static void open(Context anyCtx, final OnDone cb) {
        final Activity act = activityOf(anyCtx);
        if (act == null) return;

        float d = act.getResources().getDisplayMetrics().density;

        LinearLayout root = new LinearLayout(act);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding((int) (20 * d), (int) (18 * d), (int) (20 * d), (int) (14 * d));
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(24 * d);
        bg.setColor(0xFFFDFDFF);
        root.setBackgroundDrawable(bg);

        TextView title = new TextView(act);
        title.setText("新建卡片");
        title.setTextSize(18);
        title.setTextColor(C_TITLE);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        root.addView(title);

        TextView sub = new TextView(act);
        sub.setText("填完直接签名发送 · 别人也能看到");
        sub.setTextSize(11);
        sub.setTextColor(C_SUB);
        sub.setPadding(0, (int) (3 * d), 0, (int) (12 * d));
        root.addView(sub);

        final LinearLayout tplBox = new LinearLayout(act);
        tplBox.setOrientation(LinearLayout.VERTICAL);
        tplBox.setPadding(0, 0, 0, (int) (6 * d));
        root.addView(tplBox);

        final EditText etTitle = field(act, root, "标题", "在这里填卡片标题", d, false);
        final EditText etDesc = field(act, root, "描述", "在这里填灰色的副标题", d, true);
        final EditText etPic = field(act, root, "图片 URL",
                "在这里填图片直链（公网可访问的 https 地址）", d, false);
        final EditText etJump = field(act, root, "跳转链接",
                "在这里填点击卡片后打开的网址", d, false);
        final EditText etTag = field(act, root, "标签", "可留空，会显示在卡片底部", d, false);
        final EditText etIcon = field(act, root, "标签小图标",
                "留空用默认频道图标；也可以填自己的图片直链", d, false);

        TextView tplLab = new TextView(act);
        tplLab.setText("选模板（点一下自动填）");
        tplLab.setTextSize(11);
        tplLab.setTextColor(C_SUB);
        tplLab.setPadding(0, (int) (10 * d), 0, (int) (4 * d));
        tplBox.addView(tplLab);

        android.widget.HorizontalScrollView hsv = new android.widget.HorizontalScrollView(act);
        hsv.setHorizontalScrollBarEnabled(false);
        final LinearLayout chips = new LinearLayout(act);
        chips.setOrientation(LinearLayout.HORIZONTAL);
        hsv.addView(chips, new android.widget.FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        tplBox.addView(hsv);

        final java.util.List<TextView> chipViews = new java.util.ArrayList<TextView>();
        final java.util.List<com.cathelperawa.owo.app.feature.CardTemplates.Tpl> tpls =
                com.cathelperawa.owo.app.feature.CardTemplates.all();
        for (int i = 0; i < tpls.size(); i++) {
            final com.cathelperawa.owo.app.feature.CardTemplates.Tpl t = tpls.get(i);
            final TextView chip = new TextView(act);
            chip.setText(t.name);
            chip.setTextSize(12);
            chip.setGravity(Gravity.CENTER);
            chip.setPadding((int) (14 * d), (int) (7 * d), (int) (14 * d), (int) (7 * d));
            chip.setTextColor(0xFF6B7E99);
            GradientDrawable cg = new GradientDrawable();
            cg.setCornerRadius(20 * d);
            cg.setColor(0xFFF1F5FA);
            cg.setStroke((int) (1 * d), 0xFFE2EAF3);
            chip.setBackgroundDrawable(cg);
            LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(-2, -2);
            clp.rightMargin = (int) (6 * d);
            chips.addView(chip, clp);
            chipViews.add(chip);

            chip.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    pickedTpl = t;
                    pendingArk = null;
                    etTitle.setText(t.title == null ? "" : t.title);
                    etDesc.setText(t.desc == null ? "" : t.desc);
                    etPic.setText(t.pic == null ? "" : t.pic);
                    etJump.setText(t.jump == null ? "" : t.jump);
                    etTag.setText(t.tag == null ? "" : t.tag);
                    etIcon.setText(t.tagIcon == null ? "" : t.tagIcon);
                    for (int k = 0; k < chipViews.size(); k++) {
                        TextView c = chipViews.get(k);
                        GradientDrawable g2 = new GradientDrawable();
                        g2.setCornerRadius(20 * d);
                        if (c == chip) {
                            g2.setColor(0xFFFFE3EF);
                            g2.setStroke((int) (1 * d), 0xFFFF9CC4);
                            c.setTextColor(0xFFE0538A);
                        } else {
                            g2.setColor(0xFFF1F5FA);
                            g2.setStroke((int) (1 * d), 0xFFE2EAF3);
                            c.setTextColor(0xFF6B7E99);
                        }
                        c.setBackgroundDrawable(g2);
                    }
                    CatToast.show("已填充：" + t.name + (t.baseArk == null ? "（现拼）" : "（带完整 ARK）"));
                }
            });
        }

        TextView ok77 = pill(act, "直发测试（0xb77_9 免签）", false, d);
        LinearLayout.LayoutParams lp77 = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp77.topMargin = (int) (10 * d);
        tplBox.addView(ok77, lp77);
        ok77.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                final String t = str(etTitle), de = str(etDesc), p = str(etPic),
                        j = str(etJump), g = str(etTag);
                if (t.length() == 0) { CatToast.show("标题不能空喵"); return; }
                new Thread(new Runnable() {
                    @Override public void run() {
                        final long aid = pickedTpl == null ? 100951776L : pickedTpl.appid;
                        String r = com.cathelperawa.owo.app.feature.CardKit.sendNews(
                                g, t, de, j, p, aid);
                        if (!"ok".equals(r) && aid != 100951776L) {
                            
                            String r2 = com.cathelperawa.owo.app.feature.CardKit.sendNews(
                                    g, t, de, j, p, 100951776L);
                            r = "appid" + aid + "→" + r + "；回退B站壳→" + r2;
                        }
                        final String msg = r;
                        act.runOnUiThread(new Runnable() {
                            @Override public void run() {
                                CatToast.show("News 卡返回：" + msg);
                            }
                        });
                    }
                }, "card-news").start();
            }
        });

        restore(act, etTitle, "cft_title");
        restore(act, etDesc, "cft_desc");
        restore(act, etPic, "cft_pic");
        restore(act, etJump, "cft_jump");
        restore(act, etTag, "cft_tag");
        restore(act, etIcon, "cft_icon");

        android.widget.CheckBox cbNoSign = new android.widget.CheckBox(act);
        cbNoSign.setText("走 shadow 通道（图片和跳转都正常）");
        cbNoSign.setTextSize(12);
        cbNoSign.setTextColor(C_SUB);
        cbNoSign.setChecked(noSign);
        cbNoSign.setOnCheckedChangeListener(new android.widget.CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(android.widget.CompoundButton b, boolean checked) {
                noSign = checked;
            }
        });
        LinearLayout.LayoutParams lpCb = new LinearLayout.LayoutParams(-1, -2);
        lpCb.topMargin = (int) (6 * d);
        root.addView(cbNoSign, lpCb);

        LinearLayout btns = new LinearLayout(act);
        btns.setOrientation(LinearLayout.HORIZONTAL);
        btns.setPadding(0, (int) (12 * d), 0, 0);

        final Dialog dlg = new Dialog(act, android.R.style.Theme_Translucent_NoTitleBar);

        TextView cancel = pill(act, "取消", false, d);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { dlg.dismiss(); }
        });
        LinearLayout.LayoutParams lp1 = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lp1.rightMargin = (int) (6 * d);
        btns.addView(cancel, lp1);

        TextView ok = pill(act, "发送", true, d);
        ok.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                String t = str(etTitle), de = str(etDesc), p = str(etPic),
                        j = str(etJump), g = str(etTag), ic = str(etIcon);
                if (t.length() == 0) { CatToast.show("标题不能空喵"); return; }
                if (p.length() == 0) { CatToast.show("图片 URL 不能空喵"); return; }
                
                save(act, etTitle, "cft_title");
                save(act, etDesc, "cft_desc");
                save(act, etPic, "cft_pic");
                save(act, etJump, "cft_jump");
                save(act, etTag, "cft_tag");
                save(act, etIcon, "cft_icon");
                dlg.dismiss();
                if (cb != null) cb.run(t, de, p, j, g, ic);
            }
        });
        LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lp2.leftMargin = (int) (6 * d);
        btns.addView(ok, lp2);

        root.addView(btns);

        
        TextView okChan = pill(act, "发频道卡（服务端签名）", true, d);
        LinearLayout.LayoutParams lp3 = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp3.topMargin = (int) (8 * d);
        root.addView(okChan, lp3);
        okChan.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                String t = str(etTitle), de = str(etDesc), p = str(etPic),
                        j = str(etJump), g = str(etTag), ic = str(etIcon);
                if (t.length() == 0) { CatToast.show("标题不能空喵"); return; }
                save(act, etTitle, "cft_title");
                save(act, etDesc, "cft_desc");
                save(act, etPic, "cft_pic");
                save(act, etJump, "cft_jump");
                save(act, etTag, "cft_tag");
                save(act, etIcon, "cft_icon");
                final com.cathelperawa.owo.app.feature.GuildCard.Fields f =
                        new com.cathelperawa.owo.app.feature.GuildCard.Fields();
                f.title = t;
                f.content = de;
                f.cover = p;
                f.shareUrl = j;
                f.channelName = g;
                f.guildIcon = ic;
                dlg.dismiss();
                new Thread(new Runnable() {
                    @Override public void run() {
                        final com.cathelperawa.owo.app.feature.GuildCard.Result r =
                                com.cathelperawa.owo.app.feature.GuildCard.sendChannelSigned(f);
                        act.runOnUiThread(new Runnable() {
                            @Override public void run() {
                                CatToast.show(r != null && r.ok
                                        ? "频道卡已发出 · 看对方有没有"
                                        : ("失败：" + (r == null ? "无返回" : r.error)));
                            }
                        });
                    }
                }, "guild-chan-sign").start();
            }
        });

        
        ScrollView sv = new ScrollView(act);
        GradientDrawable svbg = new GradientDrawable();
        svbg.setCornerRadius(24 * d);
        svbg.setColor(0xFFFDFDFF);
        sv.setBackgroundDrawable(svbg);
        sv.setClipToOutline(true);
        sv.setVerticalScrollBarEnabled(false);
        sv.addView(root);

        dlg.setContentView(sv, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        Window w = dlg.getWindow();
        if (w != null) {
            
            w.setBackgroundDrawable(new ColorDrawable(0x00000000));
            w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowManager.LayoutParams wa = w.getAttributes();
            wa.dimAmount = 0.45f;
            w.setAttributes(wa);
            w.setLayout((int) (act.getResources().getDisplayMetrics().widthPixels * 0.92f),
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            w.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
        dlg.show();
    }

    public static final String DEFAULT_TAG_ICON =
            "https://tianxuan.gtimg.cn/47329_bd95e16e/assets/guild-icon.png";

    public static String build(String title, String desc, String pic, String jump,
                               String tag, String tagIcon) {
        try {
            JSONObject news = new JSONObject();
            news.put("title", title);
            news.put("desc", desc);
            news.put("tag", tag == null ? "" : tag);
            news.put("tagIcon",
                    (tagIcon == null || tagIcon.length() == 0) ? DEFAULT_TAG_ICON : tagIcon);
            news.put("preview", pic);
            news.put("jumpUrl", jump);

            JSONObject meta = new JSONObject();
            meta.put("news", news);

            JSONObject o = new JSONObject();
            o.put("app", "com.tencent.tuwen.lua");
            o.put("view", "news");
            o.put("ver", "0.0.0.1");
            o.put("prompt", title);
            o.put("bizsrc", "guild.share");
            o.put("meta", meta);
            return o.toString();
        } catch (Throwable t) {
            return null;
        }
    }

    private static String str(EditText e) {
        return e == null || e.getText() == null ? "" : e.getText().toString().trim();
    }

    private static void save(Activity act, EditText e, String key) {
        try {
            com.cathelperawa.owo.app.core.HookConfig.set(act, key, str(e));
        } catch (Throwable ignore) {}
    }

    private static void restore(Activity act, EditText e, String key) {
        try {
            String v = com.cathelperawa.owo.app.core.HookConfig.get(act, key, "");
            if (v != null && v.length() > 0 && e.getText().length() == 0) e.setText(v);
        } catch (Throwable ignore) {}
    }

    private static EditText field(Activity act, LinearLayout parent, String label,
                                  String hint, float d, boolean multi) {
        TextView lab = new TextView(act);
        lab.setText(label);
        lab.setTextSize(11);
        lab.setTextColor(C_SUB);
        lab.setPadding(0, (int) (8 * d), 0, (int) (4 * d));
        parent.addView(lab);

        EditText e = new EditText(act);
        e.setTextSize(13);
        e.setTextColor(C_TITLE);
        
        e.setHint(hint);
        e.setHintTextColor(0xFFBFC9D8);
        e.setSingleLine(!multi);
        e.setInputType(InputType.TYPE_CLASS_TEXT
                | (multi ? InputType.TYPE_TEXT_FLAG_MULTI_LINE : 0));
        e.setPadding((int) (12 * d), (int) (9 * d), (int) (12 * d), (int) (9 * d));
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(12 * d);
        g.setColor(0xFFF4F7FB);
        g.setStroke((int) (1 * d), 0xFFE2EAF3);
        e.setBackgroundDrawable(g);
        parent.addView(e, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return e;
    }

    private static TextView pill(Activity act, String text, boolean primary, float d) {
        TextView t = new TextView(act);
        t.setText(text);
        t.setTextSize(14);
        t.setGravity(Gravity.CENTER);
        t.setPadding(0, (int) (12 * d), 0, (int) (12 * d));
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(16 * d);
        if (primary) {
            g.setColors(new int[]{0xFFFF8FBE, 0xFFF0709F});
            g.setOrientation(GradientDrawable.Orientation.LEFT_RIGHT);
            t.setTextColor(0xFFFFFFFF);
        } else {
            g.setColor(0xFFF1F5FA);
            t.setTextColor(0xFF6B7E99);
        }
        t.setBackgroundDrawable(g);
        return t;
    }

    private static Activity activityOf(Context c) {
        while (c instanceof android.content.ContextWrapper) {
            if (c instanceof Activity) return (Activity) c;
            c = ((android.content.ContextWrapper) c).getBaseContext();
        }
        return null;
    }
}
