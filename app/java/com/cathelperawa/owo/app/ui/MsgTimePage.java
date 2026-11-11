package com.cathelperawa.owo.app.ui;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.cathelperawa.owo.app.feature.FeatureMsgTime;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class MsgTimePage extends Page {

    private static final int[] COLORS = {
            0xFF0000FF, 0xFF7F7F7F, 0xFFF07AAE, 0xFF3E9A6B, 0xFF8B6CE8, 0xFF222222};
    private static final String[] COLOR_NAMES = {"蓝", "灰", "粉", "绿", "紫", "黑"};

    private static final String[][] FORMATS = {
            {"HH:mm:ss", "时:分:秒"},
            {"HH:mm", "时:分"},
            {"MM-dd HH:mm", "月-日 时:分"},
            {"dd日 HH:mm:ss", "日 时:分:秒"},
    };

    private static final float[] SIZES = {8f, 10f, 12f, 14f};

    private static final String SAMPLE_VER = "QQ 9.3.35.39695 · 应用宝";

    private TextView previewTime;
    private TextView previewVer;
    private LinearLayout previewBox;
    
    private int color;
    private String format;
    private float size;

    public MsgTimePage(Host host) { super(host); }

    @Override
    public View build() {
        color = cfgInt(FeatureMsgTime.K_COLOR, 0xFF0000FF);
        format = cfgGetS(FeatureMsgTime.K_FORMAT, FeatureMsgTime.DEF_FORMAT);
        size = cfgFloat(FeatureMsgTime.K_SIZE, 10f);

        View[] out = new View[2];
        LinearLayout root = pageRoot("显示消息时间", "每条消息下面标一个收到时间", out, true, 12);

        LinearLayout preview = whiteCard(dp(20), 0xD9FFFFFF);
        preview.setPadding(dp(16), dp(14), dp(16), dp(14));
        preview.addView(text("预览", 11, 0xFF7F90A6, true, dp(8)));

        LinearLayout bubbleRow = new LinearLayout(ctx);
        bubbleRow.setOrientation(LinearLayout.HORIZONTAL);
        bubbleRow.setGravity(Gravity.START);

        LinearLayout bubble = new LinearLayout(ctx);
        bubble.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFFFFFFFF);
        bg.setCornerRadius(dp(14));
        bg.setStroke(dp(1), 0x33FFFFFF);
        bubble.setBackgroundDrawable(bg);
        bubble.setPadding(dp(12), dp(9), dp(12), dp(6));
        bubble.addView(text("这就是效果喵～", 14, 0xFF43587A, false, dp(2)));

        previewBox = new LinearLayout(ctx);
        previewBox.setOrientation(LinearLayout.VERTICAL);
        previewTime = new TextView(ctx);
        previewTime.setGravity(Gravity.START);
        previewVer = new TextView(ctx);
        previewVer.setGravity(Gravity.START);
        bubble.addView(previewBox, wrapWrap());
        bubbleRow.addView(bubble, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        preview.addView(bubbleRow, matchWrap(dp(4)));
        preview.addView(text("预览只是示意，真实效果在 " + com.cathelperawa.owo.app.core.Host.name() + " 聊天里看",
                10, 0xFF97A9C0, false, 0));
        root.addView(preview, matchWrap(dp(11)));

        root.addView(sectionTitle("颜色", "左右拖，右边色块就是当前颜色"));

        final float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        if (hsv[1] < 0.30f) hsv[1] = 0.85f;      
        if (hsv[2] < 0.20f) hsv[2] = 0.80f;

        final View swatch = new View(ctx);
        final Runnable apply = new Runnable() {
            @Override public void run() {
                color = Color.HSVToColor(hsv);
                paintDot(swatch, color, true);
                refreshPreview();
            }
        };

        SkinSeek hueBar = catSeek(360, Math.round(hsv[0]), true, new SkinSeek.OnChange() {
            @Override public void onChanged(int v) {
                hsv[0] = v;
                apply.run();
            }
            @Override public void onFinished(int v) {
                cfgSetS(FeatureMsgTime.K_COLOR, String.valueOf(color));
            }
        });

        SkinSeek valBar = catSeek(100, Math.round(hsv[2] * 100), false, new SkinSeek.OnChange() {
            @Override public void onChanged(int v) {
                hsv[2] = Math.max(0.15f, v / 100f);
                apply.run();
            }
            @Override public void onFinished(int v) {
                cfgSetS(FeatureMsgTime.K_COLOR, String.valueOf(color));
            }
        });

        LinearLayout hueRow = new LinearLayout(ctx);
        hueRow.setOrientation(LinearLayout.HORIZONTAL);
        hueRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams hl = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        hueRow.addView(hueBar, hl);
        LinearLayout.LayoutParams swLp = new LinearLayout.LayoutParams(dp(30), dp(30));
        swLp.leftMargin = dp(12);
        hueRow.addView(swatch, swLp);
        root.addView(hueRow, matchWrap(dp(6)));

        LinearLayout valRow = new LinearLayout(ctx);
        valRow.setOrientation(LinearLayout.HORIZONTAL);
        valRow.setGravity(Gravity.CENTER_VERTICAL);
        valRow.addView(text("深浅", 10, 0xFF97A9C0, false, 0));
        LinearLayout.LayoutParams vl = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        vl.leftMargin = dp(10);
        valRow.addView(valBar, vl);
        root.addView(valRow, matchWrap(dp(11)));
        paintDot(swatch, color, true);

        root.addView(sectionTitle("时间格式", "四种常见样式，选一个就好"));
        final TextView[] fmtChips = new TextView[FORMATS.length];
        LinearLayout fmtRow1 = new LinearLayout(ctx);
        fmtRow1.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout fmtRow2 = new LinearLayout(ctx);
        fmtRow2.setOrientation(LinearLayout.HORIZONTAL);
        for (int i = 0; i < FORMATS.length; i++) {
            final int idx = i;
            TextView chip = text("", 12, 0xFF7F90A6, true, 0);
            chip.setGravity(Gravity.CENTER);
            chip.setPadding(0, dp(9), 0, dp(9));
            chip.setText(FORMATS[i][0] + "\n" + FORMATS[i][1]);
            chip.setLineSpacing(dp(1), 1f);
            chip.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    press(v);
                    format = FORMATS[idx][0];
                    cfgSetS(FeatureMsgTime.K_FORMAT, format);
                    paintChips(fmtChips, formatIndex());
                    refreshPreview();
                }
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            lp.rightMargin = dp(8);
            chip.setLayoutParams(lp);
            fmtChips[i] = chip;
            (i < 2 ? fmtRow1 : fmtRow2).addView(chip);
        }
        root.addView(fmtRow1, matchWrap(dp(8)));
        root.addView(fmtRow2, matchWrap(dp(11)));
        paintChips(fmtChips, formatIndex());

        root.addView(sectionTitle("字号", "左右拖，预览里立刻能看到"));
        LinearLayout sizeRow = new LinearLayout(ctx);
        sizeRow.setOrientation(LinearLayout.HORIZONTAL);
        sizeRow.setGravity(Gravity.CENTER_VERTICAL);
        final TextView sizeLabel = text(((int) size) + " 号", 13, 0xFFE85D9C, true, 0);
        sizeLabel.setMinWidth(dp(52));
        sizeRow.addView(sizeLabel);
        SkinSeek sizeBar = catSeek(14, Math.round(size) - 6, false, new SkinSeek.OnChange() {
            @Override public void onChanged(int v) {
                size = 6 + v;
                sizeLabel.setText(((int) size) + " 号");
                refreshPreview();
            }
            @Override public void onFinished(int v) {
                cfgSetS(FeatureMsgTime.K_SIZE, String.valueOf(size));
            }
        });
        LinearLayout.LayoutParams sl = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        sl.leftMargin = dp(12);
        sizeRow.addView(sizeBar, sl);
        root.addView(sizeRow, matchWrap(dp(11)));

        root.addView(sectionTitle("版本排版", "版本号和时间的上下 / 左右关系"));
        final String[][] LAYOUTS = {
                {"inline", "同行", "时间 · 版本"},
                {"ver_top", "版本在上", "版本\n时间"},
                {"time_top", "时间在上", "时间\n版本"},
        };
        LinearLayout layRow = new LinearLayout(ctx);
        layRow.setOrientation(LinearLayout.HORIZONTAL);
        final TextView[] layChips = new TextView[LAYOUTS.length];
        String curLay = cfgGetS(FeatureMsgTime.K_VER_LAYOUT, "inline");
        for (int i = 0; i < LAYOUTS.length; i++) {
            final int idx = i;
            TextView chip = text(LAYOUTS[i][1] + "\n" + LAYOUTS[i][2], 12,
                    LAYOUTS[i][0].equals(curLay) ? 0xFFE85D9C : 0xFF7F90A6, true, 0);
            chip.setGravity(Gravity.CENTER);
            chip.setPadding(0, dp(9), 0, dp(9));
            chip.setLineSpacing(dp(1), 1f);
            chip.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    press(v);
                    cfgSetS(FeatureMsgTime.K_VER_LAYOUT, LAYOUTS[idx][0]);
                    for (int k = 0; k < layChips.length; k++) {
                        layChips[k].setTextColor(k == idx ? 0xFFE85D9C : 0xFF7F90A6);
                    }
                    refreshPreview();
                }
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            lp.rightMargin = dp(8);
            chip.setLayoutParams(lp);
            layChips[i] = chip;
            layRow.addView(chip);
        }
        root.addView(layRow, matchWrap(dp(11)));

        root.addView(sectionTitle("版本字号", "只影响版本号那一行，时间字号在上面"));
        LinearLayout vsRow = new LinearLayout(ctx);
        vsRow.setOrientation(LinearLayout.HORIZONTAL);
        vsRow.setGravity(Gravity.CENTER_VERTICAL);
        final float[] vSize = {verSize()};
        final TextView vsLabel = text(((int) vSize[0]) + " 号", 13, 0xFFE85D9C, true, 0);
        vsLabel.setMinWidth(dp(52));
        vsRow.addView(vsLabel);
        SkinSeek vsBar = catSeek(14, Math.round(vSize[0]) - 6, false, new SkinSeek.OnChange() {
            @Override public void onChanged(int v) {
                vSize[0] = 6 + v;
                vsLabel.setText(((int) vSize[0]) + " 号");
                refreshPreview();
            }
            @Override public void onFinished(int v) {
                cfgSetS(FeatureMsgTime.K_VER_SIZE, String.valueOf(vSize[0]));
            }
        });
        LinearLayout.LayoutParams vsl = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        vsl.leftMargin = dp(12);
        vsRow.addView(vsBar, vsl);
        root.addView(vsRow, matchWrap(dp(11)));

        root.addView(sectionTitle("附加信息", "版本号沿用上面的颜色和字号"));
        LinearLayout verCard = whiteCard(dp(18), 0xD9FFFFFF);
        LinearLayout verRow = new LinearLayout(ctx);
        verRow.setOrientation(LinearLayout.HORIZONTAL);
        verRow.setGravity(Gravity.CENTER_VERTICAL);
        verRow.setPadding(dp(16), dp(12), dp(14), dp(12));

        LinearLayout verBox = new LinearLayout(ctx);
        verBox.setOrientation(LinearLayout.VERTICAL);
        verBox.addView(text("同时显示发送者版本", 15, 0xFF43587A, true, dp(2)));
        verBox.addView(text("在时间后面追一句对方用的 QQ / TIM 版本号", 11, 0xFF8496AE, false, 0));
        verRow.addView(verBox, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        final SwitchBox verSw = new SwitchBox(ctx,
                "true".equalsIgnoreCase(cfgGetS(FeatureMsgTime.K_VER, "false")));
        LinearLayout.LayoutParams vp = new LinearLayout.LayoutParams(dp(52), dp(30));
        vp.leftMargin = dp(10);
        verRow.addView(verSw, vp);
        verSw.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                boolean z = !"true".equalsIgnoreCase(cfgGetS(FeatureMsgTime.K_VER, "false"));
                cfgSetS(FeatureMsgTime.K_VER, String.valueOf(z));
                verSw.setChecked(z);
                refreshPreview();
            }
        });
        verCard.addView(verRow);
        root.addView(verCard, matchWrap(dp(11)));

        LinearLayout manCard = whiteCard(dp(18), 0xD9FFFFFF);
        LinearLayout manRow = new LinearLayout(ctx);
        manRow.setOrientation(LinearLayout.HORIZONTAL);
        manRow.setGravity(Gravity.CENTER_VERTICAL);
        manRow.setPadding(dp(16), dp(12), dp(14), dp(12));

        LinearLayout manBox = new LinearLayout(ctx);
        manBox.setOrientation(LinearLayout.VERTICAL);
        manBox.addView(text("长按消息手动标注", 15, 0xFF43587A, true, dp(2)));
        manBox.addView(text("遇到认不出的版本，长按消息填版本和渠道", 11, 0xFF8496AE, false, 0));
        manRow.addView(manBox, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        final SwitchBox manSw = new SwitchBox(ctx,
                "true".equalsIgnoreCase(cfgGetS(FeatureMsgTime.K_MANUAL, "false")));
        LinearLayout.LayoutParams mp = new LinearLayout.LayoutParams(dp(52), dp(30));
        mp.leftMargin = dp(10);
        manRow.addView(manSw, mp);
        manSw.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                boolean z = !"true".equalsIgnoreCase(cfgGetS(FeatureMsgTime.K_MANUAL, "false"));
                cfgSetS(FeatureMsgTime.K_MANUAL, String.valueOf(z));
                manSw.setChecked(z);
                toast(z ? "已打开，长按任意消息可标注" : "已关闭");
            }
        });
        manCard.addView(manRow);
        root.addView(manCard, matchWrap(dp(11)));

        TextView tip = text("改完直接去聊天里看，不用重启 " + com.cathelperawa.owo.app.core.Host.name(), 11, 0xFFA8B4C4, false, 0);
        tip.setGravity(Gravity.CENTER);
        LinearLayout tipRow = new LinearLayout(ctx);
        tipRow.setGravity(Gravity.CENTER);
        tipRow.addView(tip);
        root.addView(tipRow, matchWrap(0));

        refreshPreview();
        fadeIn(root);
        return out[0];
    }

    

    
    
    private SkinSeek catSeek(int max, int value, boolean rainbow, SkinSeek.OnChange cb) {
        SkinSeek sk = new SkinSeek(ctx);
        sk.setRange(max);
        sk.setRainbow(rainbow);
        sk.setValue(value);
        sk.setOnChange(cb);
        return sk;
    }

    private void paintDot(View v, int c, boolean outline) {
        if (v == null) return;
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.OVAL);
        d.setColor(c);
        d.setStroke(dp(outline ? 2 : 1), outline ? 0x99FFFFFF : 0x55FFFFFF);
        v.setBackgroundDrawable(d);
    }

    private float verSize() {
        try {
            String v = cfgGetS(FeatureMsgTime.K_VER_SIZE, "");
            if (v != null && v.length() > 0) return Float.parseFloat(v);
        } catch (Throwable ignore) {
        }
        return cfgFloat(FeatureMsgTime.K_SIZE, 10f);
    }

    private void refreshPreview() {
        if (previewBox == null || previewTime == null) return;

        String body;
        try {
            body = new SimpleDateFormat(format, Locale.getDefault()).format(new Date());
        } catch (Throwable t) {
            body = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        }

        boolean showVer = "true".equalsIgnoreCase(cfgGetS(FeatureMsgTime.K_VER, "false"));
        String mode = cfgGetS(FeatureMsgTime.K_VER_LAYOUT, "inline");
        boolean stack = showVer && !"inline".equals(mode);

        previewTime.setTextColor(color);
        previewTime.setTextSize(size);
        previewTime.setText(body);

        previewVer.setTextColor(color);
        previewVer.setTextSize(verSize());
        previewVer.setText(stack ? SAMPLE_VER : "· " + SAMPLE_VER);

        previewBox.removeAllViews();
        previewBox.setOrientation(stack ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);

        if (!showVer) {
            previewBox.addView(previewTime, wrapWrap());
            return;
        }
        if (stack && "ver_top".equals(mode)) {
            previewBox.addView(previewVer, wrapWrap());
            previewBox.addView(previewTime, wrapWrap());
        } else {
            previewBox.addView(previewTime, wrapWrap());
            previewBox.addView(previewVer, wrapWrap());
        }
    }

    private void paintColors(View[] dots, int sel) {
        for (int i = 0; i < dots.length; i++) {
            boolean on = COLORS[i] == sel;
            GradientDrawable dg = new GradientDrawable();
            dg.setShape(GradientDrawable.OVAL);
            dg.setColor(COLORS[i]);
            dg.setStroke(dp(on ? 2 : 1), on ? 0xFFF07CA8 : 0x33FFFFFF);
            dots[i].setBackgroundDrawable(dg);
            LinearLayout.LayoutParams lp =
                    (LinearLayout.LayoutParams) dots[i].getLayoutParams();
            int side = dp(on ? 34 : 30);
            lp.width = side;
            lp.height = side;
            dots[i].setLayoutParams(lp);
        }
    }

    private void paintChips(TextView[] chips, int sel) {
        for (int i = 0; i < chips.length; i++) {
            boolean on = i == sel;
            GradientDrawable g = new GradientDrawable();
            g.setCornerRadius(dp(14));
            g.setColor(on ? 0xFFFFC2DF : 0x66FFFFFF);
            g.setStroke(dp(1), on ? 0xFFFF9FCB : 0x33FFFFFF);
            chips[i].setBackgroundDrawable(g);
            chips[i].setTextColor(on ? 0xFFE85D9C : 0xFF7F90A6);
        }
    }

    private int formatIndex() {
        for (int i = 0; i < FORMATS.length; i++) {
            if (FORMATS[i][0].equals(format)) return i;
        }
        return 0;
    }

    private int sizeIndex() {
        for (int i = 0; i < SIZES.length; i++) {
            if (Math.abs(SIZES[i] - size) < 0.01f) return i;
        }
        return 1;
    }

    private TextView sectionTitle(String title, String hint) {
        LinearLayout col = new LinearLayout(ctx);
        col.setOrientation(LinearLayout.VERTICAL);
        col.addView(text(title, 12, 0xFF6F86A8, true, dp(1)));
        col.addView(text(hint, 10, 0xFF97A9C0, false, dp(6)));

        return text(title + "　" + hint, 12, 0xFF6F86A8, true, dp(6));
    }

    

    private int cfgInt(String key, int def) {
        try {
            return (int) Long.parseLong(cfgGetS(key, String.valueOf(def)));
        } catch (Throwable t) {
            return def;
        }
    }

    private float cfgFloat(String key, float def) {
        try {
            return Float.parseFloat(cfgGetS(key, String.valueOf(def)));
        } catch (Throwable t) {
            return def;
        }
    }
}
