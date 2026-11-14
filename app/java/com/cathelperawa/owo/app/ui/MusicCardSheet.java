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

public final class MusicCardSheet {

    private MusicCardSheet() {}

    public static final class Params {

        public String mode = "play";
        public String title = "";
        public String singer = "";
        public String image = "";
        public String audio = "";
        public String jump = "";
        public String tag = "";
        public String tagIcon = "";

        public String platform = "qq";
    }

    public interface OnDone {
        void run(Params p);
    }

    private static final int C_TITLE = 0xFF3C4E6B;
    private static final int C_SUB = 0xFF8A9CB5;

    public static final String AUDIO_TPL =
            "http://music.163.com/song/media/outer/url?id=SONGID&sc=wm&tn=";

    public static final String TAG_ICON_QQ =
            "https://p.qpic.cn/qqconnect/0/app_100497308_1626060999/100?max-age=2592000&t=0";

    public static final String TAG_ICON_163 =
            "https://i.gtimg.cn/open/app_icon/00/49/50/85/100495085_100_m.png";

    public static void open(Context anyCtx, final OnDone cb) {
        final Activity act = activityOf(anyCtx);
        if (act == null) return;

        final float d = act.getResources().getDisplayMetrics().density;

        LinearLayout root = new LinearLayout(act);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding((int) (20 * d), (int) (18 * d), (int) (20 * d), (int) (14 * d));
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(24 * d);
        bg.setColor(0xFFFDFDFF);
        root.setBackgroundDrawable(bg);

        TextView title = new TextView(act);
        title.setText("自定义音乐卡");
        title.setTextSize(18);
        title.setTextColor(C_TITLE);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        root.addView(title);

        TextView sub = new TextView(act);
        sub.setText("填完自动签名 · 别人可见");
        sub.setTextSize(11);
        sub.setTextColor(C_SUB);
        sub.setPadding(0, (int) (3 * d), 0, (int) (10 * d));
        root.addView(sub);

        root.addView(label(act, "形态", d));
        LinearLayout modeRow = new LinearLayout(act);
        modeRow.setOrientation(LinearLayout.HORIZONTAL);
        final String[] mode = {restoreStr(act, "mcf_mode", "play")};
        final TextView[] pills = new TextView[2];
        pills[0] = pill(act, "♪ 能播放", true, d);
        pills[1] = pill(act, "只跳转", false, d);
        LinearLayout.LayoutParams p0 = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        p0.rightMargin = (int) (6 * d);
        modeRow.addView(pills[0], p0);
        LinearLayout.LayoutParams p1 = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        p1.leftMargin = (int) (6 * d);
        modeRow.addView(pills[1], p1);

        LinearLayout.LayoutParams mr = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        mr.bottomMargin = (int) (6 * d);
        root.addView(modeRow, mr);

        final TextView modeTip = new TextView(act);
        modeTip.setTextSize(10);
        modeTip.setTextColor(0xFFA9B7CA);
        modeTip.setPadding(0, 0, 0, (int) (8 * d));
        root.addView(modeTip);

        final LinearLayout platWrap = new LinearLayout(act);
        platWrap.setOrientation(LinearLayout.VERTICAL);
        root.addView(platWrap);

        final EditText etTitle = field(act, root, "歌名",
                "卡片上那行大字，例如「晴天」", d, false);
        final EditText etSinger = field(act, root, "歌手",
                "灰色那行，例如「周杰伦」", d, true);
        final EditText etImage = field(act, root, "封面图地址",
                "公网能访问的图片直链；留空用默认图", d, false);
        final EditText etAudio = field(act, root, "音频地址",
                "能播放形态必填：http://music.163.com/song/media/outer/url?id=歌曲ID", d, false);
        final EditText etJump = field(act, root, "点击跳转地址",
                "留空跳网易云；也可以填任何网址", d, false);
        final EditText etTag = field(act, root, "角标文字",
                "只跳转形态可改，例如「网易云音乐」「QQ音乐」", d, false);
        final EditText etIcon = field(act, root, "角标图标地址",
                "只跳转形态可改，留空用默认图标", d, false);

        restore(act, etTitle, "mcf_title");
        restore(act, etSinger, "mcf_singer");
        restore(act, etImage, "mcf_image");
        restore(act, etAudio, "mcf_audio");
        restore(act, etJump, "mcf_jump");
        restore(act, etTag, "mcf_tag");
        restore(act, etIcon, "mcf_icon");

        watch(act, etTitle, "mcf_title");
        watch(act, etSinger, "mcf_singer");
        watch(act, etImage, "mcf_image");
        watch(act, etAudio, "mcf_audio");
        watch(act, etJump, "mcf_jump");
        watch(act, etTag, "mcf_tag");
        watch(act, etIcon, "mcf_icon");

        final String[] platform = {restoreStr(act, "mcf_platform", "qq")};
        final Runnable[] applyModeRef = new Runnable[1];
        platWrap.addView(label(act, "角标平台", d));
        android.widget.HorizontalScrollView platHsv =
                new android.widget.HorizontalScrollView(act);
        platHsv.setHorizontalScrollBarEnabled(false);
        LinearLayout platRow = new LinearLayout(act);
        platRow.setOrientation(LinearLayout.HORIZONTAL);
        platHsv.addView(platRow);
        LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        plp.bottomMargin = (int) (8 * d);
        platWrap.addView(platHsv, plp);

        int nPlat = com.cathelperawa.owo.app.feature.ArkSigner.MUSIC_PLATFORMS.length + 1;
        final TextView[] platChips = new TextView[nPlat];
        final String[] platVals = new String[nPlat];
        for (int i = 0; i < nPlat; i++) {
            boolean custom = i == nPlat - 1;
            platVals[i] = custom ? "custom"
                    : com.cathelperawa.owo.app.feature.ArkSigner.MUSIC_PLATFORMS[i][0];
            String text = custom ? "自定义"
                    : com.cathelperawa.owo.app.feature.ArkSigner.MUSIC_PLATFORMS[i][1];
            final TextView chip = pill(act, text, false, d);
            chip.setTextSize(11);
            chip.setPadding((int) (12 * d), (int) (7 * d), (int) (12 * d), (int) (7 * d));
            LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            clp.rightMargin = (int) (6 * d);
            platRow.addView(chip, clp);
            final int idx = i;
            chip.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    platform[0] = platVals[idx];
                    saveRaw(act, "mcf_platform", platform[0]);
                    if (applyModeRef[0] != null) applyModeRef[0].run();
                }
            });
            platChips[i] = chip;
        }
        final android.widget.HorizontalScrollView platHsvF = platHsv;

        final TextView tip = new TextView(act);
        tip.setTextSize(10);
        tip.setTextColor(0xFFA9B7CA);
        tip.setPadding(0, (int) (8 * d), 0, 0);
        root.addView(tip);

        final Runnable applyMode = new Runnable() {
            @Override public void run() {
                boolean play = !"link".equals(mode[0]);
                boolean custom = "custom".equals(platform[0]);
                paint(pills[0], play, d);
                paint(pills[1], !play, d);

                platWrap.setVisibility(View.VISIBLE);
                for (int i = 0; i < platChips.length; i++) {
                    paint(platChips[i], platVals[i].equals(platform[0]), d);
                    platChips[i].setTextSize(11);
                }
                boolean tagEditable = custom;
                etTag.setEnabled(tagEditable);
                etIcon.setEnabled(tagEditable);
                etTag.setAlpha(tagEditable ? 1f : 0.5f);
                etIcon.setAlpha(tagEditable ? 1f : 0.5f);
                etAudio.setAlpha(1f);
                if (!custom) {
                    etTag.setText(com.cathelperawa.owo.app.feature.ArkSigner
                            .platformTag(platform[0]));
                    etIcon.setText(com.cathelperawa.owo.app.feature.ArkSigner
                            .platformIcon(platform[0]));
                }
                if (play) {
                    modeTip.setText("♪ 能播放：带播放按钮，点了会出声；角标从下面 6 个平台里选");
                    if (custom) {
                        tip.setText("自定义角标 = 签名后再改文字/图标，token 与内容绑定，"
                                + "别人可能看不到（发小号试）");
                    } else {
                        tip.setText("音频地址里的 id 就是网易云歌曲 ID；VIP/无版权会点了没声音。"
                                + "跳转地址随便填，会自动补 https://");
                    }
                } else {
                    modeTip.setText("只跳转：没有播放按钮，点开跳链接（角标平台任选）");
                    if (custom) {
                        tip.setText("自定义角标走天玄签（要 p_skey），选了平台就不用手填。");
                    } else {
                        tip.setText("走音卡服务签 news 卡，不用天玄也能发；音频地址可以不填。");
                    }
                }
            }
        };
        applyModeRef[0] = applyMode;
        pills[0].setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                mode[0] = "play";
                saveRaw(act, "mcf_mode", "play");
                applyMode.run();
            }
        });
        pills[1].setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                mode[0] = "link";
                saveRaw(act, "mcf_mode", "link");
                applyMode.run();
            }
        });
        applyMode.run();

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

        TextView ok = pill(act, "签名并发送", true, d);
        ok.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                Params p = new Params();
                p.mode = mode[0];
                p.title = str(etTitle);
                p.singer = str(etSinger);
                p.image = str(etImage);
                p.audio = str(etAudio);
                p.jump = str(etJump);
                p.tag = str(etTag);
                p.tagIcon = str(etIcon);

                if (p.title.length() == 0) { CatToast.show("歌名不能空喵"); return; }
                p.platform = platform[0];
                if ("play".equals(p.mode)) {
                    if (p.audio.length() == 0) { CatToast.show("能播放形态要填音频地址喵"); return; }
                    if (!p.audio.startsWith("http")) { CatToast.show("音频地址要以 http 开头"); return; }
                    if (!"custom".equals(p.platform)) {

                        p.tag = com.cathelperawa.owo.app.feature.ArkSigner
                                .platformTag(p.platform);
                        p.tagIcon = com.cathelperawa.owo.app.feature.ArkSigner
                                .platformIcon(p.platform);
                    } else if (p.tag.length() == 0) {
                        CatToast.show("自定义角标要填角标文字喵");
                        return;
                    }
                } else {
                    if (p.image.length() == 0) { CatToast.show("只跳转形态要填封面图喵"); return; }
                    if (!"custom".equals(p.platform)) {
                        p.tag = com.cathelperawa.owo.app.feature.ArkSigner
                                .platformTag(p.platform);
                        p.tagIcon = com.cathelperawa.owo.app.feature.ArkSigner
                                .platformIcon(p.platform);
                    } else {
                        if (p.tag.length() == 0) { CatToast.show("自定义角标要填角标文字喵"); return; }
                        if (p.tagIcon.length() == 0) p.tagIcon = TAG_ICON_163;
                    }
                }
                if (p.image.length() == 0) p.image = TAG_ICON_163;

                if (p.jump.length() == 0) p.jump = "https://music.163.com/";

                save(act, "mcf_mode", p.mode);
                save(act, "mcf_title", p.title);
                save(act, "mcf_singer", p.singer);
                save(act, "mcf_image", p.image);
                save(act, "mcf_audio", p.audio);
                save(act, "mcf_jump", p.jump);
                save(act, "mcf_tag", p.tag);
                save(act, "mcf_icon", p.tagIcon);
                save(act, "mcf_platform", p.platform);
                dlg.dismiss();
                if (cb != null) cb.run(p);
            }
        });
        LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lp2.leftMargin = (int) (6 * d);
        btns.addView(ok, lp2);

        root.addView(btns);

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

    private static TextView label(Activity act, String text, float d) {
        TextView lb = new TextView(act);
        lb.setText(text);
        lb.setTextSize(11);
        lb.setTextColor(C_SUB);
        lb.setPadding(0, (int) (6 * d), 0, (int) (4 * d));
        return lb;
    }

    private static Activity activityOf(Context c) {
        if (c instanceof Activity) return (Activity) c;
        if (c != null && c instanceof android.content.ContextWrapper) {
            Context b = ((android.content.ContextWrapper) c).getBaseContext();
            if (b instanceof Activity) return (Activity) b;
        }
        try {
            if (c != null) {
                Context b = c.getApplicationContext();
                if (b instanceof Activity) return (Activity) b;
            }
        } catch (Throwable ignore) {
        }
        return null;
    }

    private static EditText field(Activity act, LinearLayout root, String label,
                                  String hint, float d, boolean multi) {
        TextView lb = new TextView(act);
        lb.setText(label);
        lb.setTextSize(11);
        lb.setTextColor(C_SUB);
        lb.setPadding(0, (int) (6 * d), 0, (int) (2 * d));
        root.addView(lb);

        EditText et = new EditText(act);
        et.setHint(hint);
        et.setHintTextColor(0xFFC2CCDA);
        et.setTextSize(13);
        et.setTextColor(C_TITLE);
        et.setInputType(InputType.TYPE_CLASS_TEXT
                | (multi ? InputType.TYPE_TEXT_FLAG_MULTI_LINE : 0));
        et.setSingleLine(!multi);
        et.setBackgroundDrawable(boxBg(d));
        et.setPadding((int) (12 * d), (int) (9 * d), (int) (12 * d), (int) (9 * d));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = (int) (4 * d);
        root.addView(et, lp);
        return et;
    }

    private static GradientDrawable boxBg(float d) {
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(13 * d);
        g.setColor(0xFFF4F6FB);
        g.setStroke((int) d, 0x22000000);
        return g;
    }

    private static TextView pill(Activity act, String text, boolean primary, float d) {
        TextView v = new TextView(act);
        v.setText(text);
        v.setTextSize(13);
        v.setGravity(Gravity.CENTER);
        v.setPadding(0, (int) (11 * d), 0, (int) (11 * d));
        v.setBackgroundDrawable(pillBg(primary, d));
        v.setTextColor(primary ? 0xFFFFFFFF : C_TITLE);
        v.setTypeface(null, android.graphics.Typeface.BOLD);
        return v;
    }

    private static void paint(TextView v, boolean on, float d) {
        v.setBackgroundDrawable(pillBg(on, d));
        v.setTextColor(on ? 0xFFFFFFFF : C_TITLE);
    }

    private static GradientDrawable pillBg(boolean primary, float d) {
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(22 * d);
        if (primary) {
            g.setColor(0xFFFF8FC0);
        } else {
            g.setColor(0xFFFFFFFF);
            g.setStroke((int) d, 0x22000000);
        }
        return g;
    }

    private static String str(EditText e) {
        return e == null || e.getText() == null ? "" : e.getText().toString().trim();
    }

    private static void save(Activity act, String key, String val) {
        try {
            com.cathelperawa.owo.app.core.HookConfig.set(act, key, val == null ? "" : val);
        } catch (Throwable ignore) {
        }
    }

    private static String restoreStr(Activity act, String key, String def) {
        try {
            String v = com.cathelperawa.owo.app.core.HookConfig.get(act, key, def);
            return (v == null || v.length() == 0) ? def : v;
        } catch (Throwable t) {
            return def;
        }
    }

    private static void watch(final Activity act, final EditText e, final String key) {
        if (e == null) return;
        e.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(android.text.Editable s) {
                saveRaw(act, key, s == null ? "" : s.toString());
            }
        });
    }

    private static void saveRaw(Activity act, String key, String val) {
        try {
            com.cathelperawa.owo.app.core.HookConfig.set(act, key, val == null ? "" : val);
        } catch (Throwable ignore) {
        }
    }

    private static void restore(Activity act, EditText e, String key) {
        try {
            String v = com.cathelperawa.owo.app.core.HookConfig.get(act, key, "");
            if (v != null && v.length() > 0) e.setText(v);
        } catch (Throwable ignore) {
        }
    }
}
