package com.cathelperawa.owo.app.ui;

import android.graphics.Bitmap;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.cathelperawa.owo.app.feature.FeatureAntiDetect;

public final class AntiDetectPage extends Page {

    public AntiDetectPage(Page.Host host) {
        super(host);
    }

    private String stateText(boolean on) {
        return on ? "已开启" : "已关闭";
    }

    private void refreshHero(TextView pill) {
        boolean on = swOn(FeatureAntiDetect.K);
        pill.setText(stateText(on));
        try {
            GradientDrawable g = (GradientDrawable) pill.getBackground();
            g.setColor(on ? 0xFFC9EFDA : 0xFFFFFFFF);
        } catch (Throwable ignore) {}
        pill.setTextColor(on ? 0xFF3FA96B : 0xFF7F90A6);
    }

    @Override
    public View build() {
        View[] out = new View[2];
        LinearLayout root = pageRoot("防检测", "不留痕迹 · 掐断上报", out, true, 12);

        FrameLayout hero = new FrameLayout(ctx);
        GradientDrawable hg = new GradientDrawable();
        hg.setColor(0xFFFFFFFF);
        hg.setCornerRadius(dp(26));
        hg.setStroke(dp(1), 0x77FFFFFF);
        hero.setBackgroundDrawable(hg);
        hero.setClipToOutline(true);
        hero.setElevation(dp(7));

        Bitmap heroImg = image("art6");
        if (heroImg != null) {
            ImageView iv = new ImageView(ctx);
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            iv.setImageBitmap(heroImg);
            hero.addView(iv, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, dp(150)));
        }
        View veil = new View(ctx);
        veil.setBackgroundDrawable(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0x00FFFFFF, 0x99FFFFFF, 0xF8FFFFFF}));
        hero.addView(veil, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, dp(100), Gravity.BOTTOM));

        LinearLayout heroText = new LinearLayout(ctx);
        heroText.setOrientation(LinearLayout.VERTICAL);
        heroText.setPadding(dp(16), 0, dp(16), dp(14));

        final TextView pill = pill(stateText(swOn(FeatureAntiDetect.K)),
                0xFF7F90A6);
        refreshHero(pill);
        LinearLayout pillRow = new LinearLayout(ctx);
        pillRow.setOrientation(LinearLayout.HORIZONTAL);
        pillRow.addView(pill, wrapWrap());
        heroText.addView(pillRow, matchWrap(dp(7)));
        heroText.addView(text("防检测", 20, 0xFF4A5872, true, dp(3)));
        heroText.addView(text("崩溃 · 统计 · Beacon 一律不发出", 12, 0xFF8A99AC, false, 0));

        hero.addView(heroText, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM));
        root.addView(hero, matchWrap(dp(11)));

        FrameLayout card = whiteFrame(dp(20), 0x8CFFFFFF);
        card.setClipToOutline(true);
        LinearLayout txt = new LinearLayout(ctx);
        txt.setOrientation(LinearLayout.VERTICAL);
        txt.addView(text("掐断上报", 16, 0xFF43587A, true, dp(2)));
        txt.addView(text("崩溃堆栈 · 统计数据 · Beacon", 11, 0xFF8496AE, false, 0));
        FrameLayout.LayoutParams tp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.START | Gravity.CENTER_VERTICAL);
        tp.leftMargin = dp(14);
        card.addView(txt, tp);

        final SwitchBox box = new SwitchBox(ctx, swOn(FeatureAntiDetect.K));
        FrameLayout.LayoutParams bp = new FrameLayout.LayoutParams(
                dp(52), dp(30), Gravity.END | Gravity.CENTER_VERTICAL);
        bp.rightMargin = dp(13);
        card.addView(box, bp);

        host.bindSwitch(FeatureAntiDetect.K, box, new Runnable() {
            @Override public void run() { refreshHero(pill); }
        });

        View.OnClickListener toggle = new View.OnClickListener() {
            @Override public void onClick(View v) {
                boolean z = !swOn(FeatureAntiDetect.K);
                swSet(FeatureAntiDetect.K, z);
                box.setChecked(z);
                press(v);
                refreshHero(pill);
                toast("防检测" + (z ? " 开启喵～" : " 关闭喵～"));
            }
        };
        card.setOnClickListener(toggle);
        box.setOnClickListener(toggle);
        root.addView(card, matchWrap(dp(11)));

        LinearLayout info = whiteCard(dp(20), 0x8CFFFFFF);
        info.setPadding(dp(15), dp(13), dp(15), dp(13));
        info.addView(text("拦在哪", 14, 0xFF4F6485, true, dp(8)));
        String[][] rows = {
                {"崩溃上报", "QQCrashReportManager.initCrashReport"},
                {"统计上报", "StatisticCollector 的 report/commit 方法"},
                {"Beacon", "QQBeaconReport 上报入口"},
        };
        for (int i = 0; i < rows.length; i++) {
            LinearLayout rl = new LinearLayout(ctx);
            rl.setOrientation(LinearLayout.VERTICAL);
            rl.setPadding(0, dp(5), 0, dp(5));
            rl.addView(text(rows[i][0], 12.5f, 0xFF8A9CB5, false, dp(2)));
            rl.addView(text(rows[i][1], 11, 0xFFA8B4C4, false, 0));
            info.addView(rl);
        }
        info.addView(text("只拦「上报」语义的方法，不动宿主业务分支", 11, 0xFFA8B4C4, false, 0));
        root.addView(info, matchWrap(dp(11)));

        TextView tip = text("开关改动重启宿主后生效", 11, 0xFFA8B4C4, false, 0);
        tip.setGravity(Gravity.CENTER);
        LinearLayout tipRow = new LinearLayout(ctx);
        tipRow.setOrientation(LinearLayout.HORIZONTAL);
        tipRow.setGravity(Gravity.CENTER);
        tipRow.addView(tip);
        root.addView(tipRow, matchWrap(0));

        fadeIn(root);
        return out[0];
    }
}
