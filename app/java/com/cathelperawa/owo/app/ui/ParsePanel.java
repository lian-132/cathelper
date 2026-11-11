package com.cathelperawa.owo.app.ui;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.cathelperawa.owo.app.core.HookConfig;

public final class ParsePanel {

    private ParsePanel() {}

    private static final int C_TITLE = 0xFF3C4E6B;
    private static final int C_SUB = 0xFF8A9CB5;
    private static final int C_OFF = 0xFFB6C0CE;

    private static final String K_MUSIC_ON = "pk_music_on";
    private static final String K_VIDEO_ON = "pk_video_on";

    public static boolean musicOn(Context c) {
        try {
            return HookConfig.getBoolean(c, K_MUSIC_ON, false);
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean videoOn(Context c) {
        try {
            return HookConfig.getBoolean(c, K_VIDEO_ON, false);
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean anyOn(Context c) {
        return musicOn(c) || videoOn(c);
    }

    public static void show(final Context anyCtx) {
        final Activity act = activityOf(anyCtx);
        if (act == null) return;
        act.runOnUiThread(new Runnable() {
            @Override public void run() {
                try {
                    build(act).show();
                } catch (Throwable t) {
                    com.cathelperawa.owo.app.core.HookLog.e("ParsePanel 构建失败", t);
                }
            }
        });
    }

    private static Dialog build(final Activity act) {
        final Dialog dlg = new Dialog(act, android.R.style.Theme_Translucent_NoTitleBar);
        final float d = act.getResources().getDisplayMetrics().density;
        DisplayMetrics dm = act.getResources().getDisplayMetrics();
        final int screenW = dm.widthPixels;

        final FrameLayout root = new FrameLayout(act);
        root.setBackgroundColor(Color.TRANSPARENT);

        LinearLayout card = new LinearLayout(act);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding((int) (18 * d), (int) (16 * d), (int) (18 * d), (int) (14 * d));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xCCFFFFFF);
        bg.setCornerRadius(26 * d);
        bg.setStroke((int) d, 0xA6FFFFFF);
        card.setBackgroundDrawable(bg);
        card.setElevation(12 * d);

        LinearLayout head = new LinearLayout(act);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout titles = new LinearLayout(act);
        titles.setOrientation(LinearLayout.VERTICAL);
        titles.addView(tv(act, "解析发送", 19, C_TITLE, true, d));
        final boolean pMOn = musicOn(act);
        final boolean pVOn = videoOn(act);
        titles.addView(tv(act, (pMOn || pVOn)
                ? "丢链接 / 报歌名，直接发到当前会话"
                : "语音库存 / 导 / 转，直接发到当前会话", 11, C_SUB, false, d));
        head.addView(titles, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView close = tv(act, "✕", 15, 0xFF9AAABF, true, d);
        close.setPadding((int) (12 * d), (int) (4 * d), (int) (4 * d), (int) (4 * d));
        close.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { dlg.dismiss(); }
        });
        head.addView(close);

        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hp.bottomMargin = (int) (14 * d);
        card.addView(head, hp);

        final boolean mOn = pMOn;
        final boolean vOn = pVOn;

        java.util.ArrayList<View> rows = new java.util.ArrayList<View>();

        if (mOn) {
            rows.add(row(act, d, dlg, "音乐",
                    "搜歌 · 卡片 / 语音 / 文件",
                    true, 0xFFF07AAE, new Runnable() {
                        @Override public void run() {
                            askSong(act);
                        }
                    }));
        }

        if (vOn) {
            rows.add(row(act, d, dlg, "视频",
                    "贴链接 · 解析后直接发文件",
                    true, 0xFF6E93D6, new Runnable() {
                        @Override public void run() {
                            askLink(act);
                        }
                    }));
        }

        rows.add(row(act, d, dlg, "语音库",
                "存 / 导 / 转 · 也是长按语音的落脚点",
                true, 0xFF57AE83, new Runnable() {
                    @Override public void run() {
                        VoiceVaultSheet.show(act);
                    }
                }));

        for (int i = 0; i < rows.size(); i++) {
            LinearLayout.LayoutParams lp = rowLp(d, false);
            if (i < rows.size() - 1) lp.bottomMargin = (int) (9 * d);
            card.addView(rows.get(i), lp);
        }

        if (!mOn && !vOn) {
            TextView tip = tv(act, "音乐 / 视频在设置里关着 · 语音库照常用", 10, 0xFFAAB6C6, false, d);
            tip.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            tp.topMargin = (int) (10 * d);
            card.addView(tip, tp);
        }

        FrameLayout.LayoutParams clp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM);
        clp.leftMargin = (int) (screenW * 0.03f);
        clp.rightMargin = (int) (screenW * 0.03f);
        clp.bottomMargin = (int) (14 * d);
        root.addView(card, clp);

        dlg.setContentView(root);
        dlg.setCanceledOnTouchOutside(true);
        dlg.show();

        Window w = dlg.getWindow();
        if (w == null) return dlg;
        w.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
        w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        WindowManager.LayoutParams lp = w.getAttributes();
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.MATCH_PARENT;
        lp.dimAmount = 0.20f;
        w.setAttributes(lp);
        return dlg;
    }

    private static void askSong(final Activity act) {
        final float d = act.getResources().getDisplayMetrics().density;
        final Dialog dlg = new Dialog(act, android.R.style.Theme_Translucent_NoTitleBar);

        LinearLayout card = new LinearLayout(act);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding((int) (18 * d), (int) (16 * d), (int) (18 * d), (int) (14 * d));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xE6FFFFFF);
        bg.setCornerRadius(26 * d);
        bg.setStroke((int) d, 0xA6FFFFFF);
        card.setBackgroundDrawable(bg);
        card.setElevation(12 * d);

        card.addView(tv(act, "发音乐", 18, C_TITLE, true, d));
        LinearLayout.LayoutParams tp0 = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tp0.bottomMargin = (int) (12 * d);
        card.addView(tv(act, "报歌名，带歌手名更准", 11, C_SUB, false, d), tp0);

        final android.widget.EditText et = new android.widget.EditText(act);
        et.setHint("例如：晴天 周杰伦");
        et.setTextSize(13);
        et.setSingleLine(true);
        et.setTextColor(0xFF43587A);
        et.setHintTextColor(0xFFAAB6C6);
        et.setPadding((int) (12 * d), (int) (11 * d), (int) (12 * d), (int) (11 * d));
        GradientDrawable eg = new GradientDrawable();
        eg.setCornerRadius(16 * d);
        eg.setColor(0xB3FFFFFF);
        eg.setStroke((int) d, 0x59FFFFFF);
        et.setBackgroundDrawable(eg);

        LinearLayout.LayoutParams etLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        etLp.bottomMargin = (int) (14 * d);
        card.addView(et, etLp);

        LinearLayout btns = new LinearLayout(act);
        btns.setOrientation(LinearLayout.HORIZONTAL);

        TextView cancel = btn(act, d, "取消", false);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { dlg.dismiss(); }
        });
        TextView go = btn(act, d, "搜索", true);
        go.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                String kw = et.getText() == null ? "" : et.getText().toString().trim();
                if (kw.length() == 0) {
                    com.cathelperawa.owo.app.bridge.CatToast.show("先说个歌名喵");
                    return;
                }
                dlg.dismiss();
                com.cathelperawa.owo.app.feature.FeatureParse.startMusicFromPanel(act, kw);
            }
        });
        LinearLayout.LayoutParams bl = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        bl.rightMargin = (int) (5 * d);
        LinearLayout.LayoutParams br = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        br.leftMargin = (int) (5 * d);
        btns.addView(cancel, bl);
        btns.addView(go, br);
        card.addView(btns);

        dlg.setContentView(card);
        dlg.setCanceledOnTouchOutside(true);
        dlg.show();
        Window w = dlg.getWindow();
        if (w == null) return;
        w.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
        w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        WindowManager.LayoutParams lp = w.getAttributes();
        int sw = act.getResources().getDisplayMetrics().widthPixels;
        lp.width = (int) (sw * 0.90f);
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        lp.gravity = Gravity.CENTER;
        lp.dimAmount = 0.24f;
        w.setAttributes(lp);
    }

    private static void askLink(final Activity act) {
        final float d = act.getResources().getDisplayMetrics().density;
        final Dialog dlg = new Dialog(act, android.R.style.Theme_Translucent_NoTitleBar);

        LinearLayout card = new LinearLayout(act);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding((int) (18 * d), (int) (16 * d), (int) (18 * d), (int) (14 * d));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xE6FFFFFF);
        bg.setCornerRadius(26 * d);
        bg.setStroke((int) d, 0xA6FFFFFF);
        card.setBackgroundDrawable(bg);
        card.setElevation(12 * d);

        card.addView(tv(act, "发视频", 18, C_TITLE, true, d));
        LinearLayout.LayoutParams tp0 = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tp0.bottomMargin = (int) (12 * d);
        card.addView(tv(act, "粘贴 B站 / 抖音 链接，解析完直接发到当前会话", 11, C_SUB, false, d), tp0);

        final android.widget.EditText et = new android.widget.EditText(act);
        et.setHint("https://b23.tv/…　或　抖音分享文案");
        et.setTextSize(13);
        et.setSingleLine(false);
        et.setTextColor(0xFF43587A);
        et.setHintTextColor(0xFFAAB6C6);
        et.setPadding((int) (12 * d), (int) (11 * d), (int) (12 * d), (int) (11 * d));
        GradientDrawable eg = new GradientDrawable();
        eg.setCornerRadius(16 * d);
        eg.setColor(0xB3FFFFFF);
        eg.setStroke((int) d, 0x59FFFFFF);
        et.setBackgroundDrawable(eg);

        String clip = clipboardText(act);
        if (clip != null) {
            String c2 = clip.trim();
            
            if (c2.startsWith("http") || c2.contains("douyin.com")) {
                if (c2.length() > 400) c2 = c2.substring(0, 400);
                et.setText(c2);
            }
        }
        et.setSelection(et.getText().length());

        LinearLayout.LayoutParams etLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        etLp.bottomMargin = (int) (14 * d);
        card.addView(et, etLp);

        LinearLayout btns = new LinearLayout(act);
        btns.setOrientation(LinearLayout.HORIZONTAL);

        TextView cancel = btn(act, d, "取消", false);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { dlg.dismiss(); }
        });
        TextView go = btn(act, d, "解析", true);
        go.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                String u = et.getText() == null ? "" : et.getText().toString().trim();
                if (u.length() == 0) {
                    com.cathelperawa.owo.app.bridge.CatToast.show("先贴个链接喵");
                    return;
                }
                dlg.dismiss();
                com.cathelperawa.owo.app.feature.FeatureParse.startFromPanel(act, u);
            }
        });
        LinearLayout.LayoutParams bl = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        bl.rightMargin = (int) (5 * d);
        LinearLayout.LayoutParams br = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        br.leftMargin = (int) (5 * d);
        btns.addView(cancel, bl);
        btns.addView(go, br);
        card.addView(btns);

        dlg.setContentView(card);
        dlg.setCanceledOnTouchOutside(true);
        dlg.show();
        Window w = dlg.getWindow();
        if (w == null) return;
        w.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
        w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        WindowManager.LayoutParams lp = w.getAttributes();
        int sw = act.getResources().getDisplayMetrics().widthPixels;
        lp.width = (int) (sw * 0.90f);
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        lp.gravity = Gravity.CENTER;
        lp.dimAmount = 0.24f;
        w.setAttributes(lp);
    }

    private static TextView btn(Activity act, float d, String label, boolean primary) {
        TextView t = new TextView(act);
        t.setText(label);
        t.setTextSize(14);
        t.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        t.setGravity(Gravity.CENTER);
        t.setPadding(0, (int) (12 * d), 0, (int) (12 * d));
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(16 * d);
        if (primary) {
            g.setColor(0xFFFFC2DF);
            g.setStroke((int) d, 0xFFFF9FCB);
            t.setTextColor(0xFFE85D9C);
        } else {
            g.setColor(0x59FFFFFF);
            g.setStroke((int) d, 0x66FFFFFF);
            t.setTextColor(0xFF8A9CB5);
        }
        t.setBackgroundDrawable(g);
        return t;
    }

    private static String clipboardText(Context c) {
        try {
            android.content.ClipboardManager cm = (android.content.ClipboardManager)
                    c.getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm == null || !cm.hasPrimaryClip()) return null;
            android.content.ClipData cd = cm.getPrimaryClip();
            if (cd == null || cd.getItemCount() == 0) return null;
            CharSequence s = cd.getItemAt(0).coerceToText(c);
            return s == null ? null : s.toString().trim();
        } catch (Throwable t) {
            return null;
        }
    }

    private static View row(final Activity act, final float d, final Dialog dlg,
                            final String title, final String sub, final boolean on,
                            final int accent, final Runnable onClick) {
        LinearLayout row = new LinearLayout(act);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding((int) (14 * d), (int) (13 * d), (int) (14 * d), (int) (13 * d));

        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(18 * d);
        g.setColor(on ? 0x99FFFFFF : 0x33FFFFFF);
        g.setStroke((int) d, on ? 0x66FFFFFF : 0x22FFFFFF);
        row.setBackgroundDrawable(g);

        View bar = new View(act);
        GradientDrawable bg2 = new GradientDrawable();
        bg2.setCornerRadius((int) (2 * d));
        bg2.setColor(on ? accent : 0xFFCBD3DF);
        bar.setBackgroundDrawable(bg2);
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(
                (int) (4 * d), (int) (26 * d));
        blp.rightMargin = (int) (11 * d);
        row.addView(bar, blp);

        LinearLayout texts = new LinearLayout(act);
        texts.setOrientation(LinearLayout.VERTICAL);
        TextView t1 = tv(act, title, 16, on ? C_TITLE : C_OFF, true, d);
        TextView t2 = tv(act, sub, 11, on ? C_SUB : C_OFF, false, d);
        texts.addView(t1);
        texts.addView(t2);
        row.addView(texts, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView arrow = tv(act, "›", 18, on ? 0xFF9FB0C4 : 0xFFD5DBE4, true, d);
        row.addView(arrow);

        if (on) {
            row.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    dlg.dismiss();
                    try {
                        onClick.run();
                    } catch (Throwable ignore) {
                    }
                }
            });
            com.cathelperawa.owo.app.ui.UiKit.press(row);
        } else {
            row.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    com.cathelperawa.owo.app.bridge.CatToast.show(title + "还没开喵～");
                }
            });
        }
        return row;
    }

    private static LinearLayout.LayoutParams rowLp(float d, boolean first) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        if (first) lp.bottomMargin = (int) (9 * d);
        return lp;
    }

    private static TextView tv(Activity act, String s, float size, int color,
                               boolean bold, float d) {
        TextView t = new TextView(act);
        t.setText(s);
        t.setTextSize(size);
        t.setTextColor(color);
        if (bold) t.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        return t;
    }

    private static Activity activityOf(Context c) {
        try {
            Context x = c;
            while (x instanceof ContextWrapper) {
                if (x instanceof Activity) return (Activity) x;
                x = ((ContextWrapper) x).getBaseContext();
                if (x == null) break;
            }
            if (c instanceof Activity) return (Activity) c;
        } catch (Throwable ignore) {
        }
        return null;
    }
}
