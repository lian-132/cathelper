package com.cathelperawa.owo.app.ui;

import android.graphics.Bitmap;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.cathelperawa.owo.app.feature.FeatureQqBrowser;
import com.cathelperawa.owo.app.feature.FeatureUnlockForward;
import com.cathelperawa.owo.app.feature.FeatureUnlockMultiRecall;
import com.cathelperawa.owo.app.feature.FeatureUnlockPatPat;

public final class UnlockPage extends Page {

    private static final String[][] ITEMS = {
            {FeatureUnlockForward.K, "去除转发限制", "解除转发时的人数上限", "u_forward"},
            {FeatureUnlockPatPat.K, "去除拍一拍限制", "拍一拍不限次数", "u_patpat"},
            {FeatureUnlockMultiRecall.K, "多选撤回", "长按进多选，批量撤回自己的消息", "u_revoke"},
            {FeatureQqBrowser.K, "去你大爷的QQ浏览器", "网页一律交给系统浏览器打开", "u_browser"},

            {"u_card", "允许查看异常资料卡", "忽略账号异常状态，正常查看资料卡", "u_card"},

            {com.cathelperawa.owo.app.feature.FeatureBypassArkLimit.K,
                    "绕过TIM卡片点击限制",
                    "解除TIM中部分卡片无法点击的问题",
                    "u_card"},

            {com.cathelperawa.owo.app.feature.FeaturePttAutoText.K,
                    "语音消息自动转文字",
                    "打开 " + com.cathelperawa.owo.app.core.Host.name() + " 的语音转文字，AI 才能听懂语音（重启 " + com.cathelperawa.owo.app.core.Host.name() + " 生效）",
                    "u_card"}};

    private static final class ParamPanel {
        LinearLayout view;
        EditText field;
        TextView state;
        boolean open;
    }

    public UnlockPage(Page.Host host) {
        super(host);
    }

    private void refreshHero(TextView tv) {
        int on = 0;
        for (String[] it : ITEMS) {
            if (swOn(it[0])) on++;
        }
        tv.setText(stateText(on));
        ((GradientDrawable) tv.getBackground()).setColor(on == 0 ? 0xFFFFFFFF : 0xFFFFC2DF);
        tv.setTextColor(on == 0 ? 0xFF7F90A6 : 0xFFF07AAE);
    }

    private static String stateText(int on) {
        if (on == 0) return "全部关闭";
        return on == ITEMS.length ? "已全部开启" : "已开启 " + on + " / " + ITEMS.length;
    }

    @Override
    public View build() {
        View[] out = new View[2];
        LinearLayout root = pageRoot("去除限制", "让 " + com.cathelperawa.owo.app.core.Host.name() + " 少管一点", out, true, 12);
        final View pageRoot = out[0];

        FrameLayout hero = new FrameLayout(ctx);
        GradientDrawable hg = new GradientDrawable();
        hg.setColor(0xFFFFFFFF);
        hg.setCornerRadius(dp(26));
        hg.setStroke(dp(1), 0x77FFFFFF);
        hero.setBackgroundDrawable(hg);
        hero.setClipToOutline(true);
        hero.setElevation(dp(7));

        Bitmap heroImg = image("h_unlock");
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

        int onCount = 0;
        for (String[] it : ITEMS) {
            if (swOn(it[0])) onCount++;
        }
        final TextView pill = pill(stateText(onCount), onCount == 0 ? 0xFF7F90A6 : 0xFFF07AAE);
        if (onCount > 0) {
            ((GradientDrawable) pill.getBackground()).setColor(0xFFFFC2DF);
        }
        LinearLayout pillRow = new LinearLayout(ctx);
        pillRow.setOrientation(LinearLayout.HORIZONTAL);
        pillRow.addView(pill, wrapWrap());
        heroText.addView(pillRow, matchWrap(dp(7)));
        heroText.addView(text("去除限制", 20, 0xFF4A5872, true, dp(3)));
        heroText.addView(text("被 TIM 藏起来的能力，帮你拿回来", 12, 0xFF8A99AC, false, 0));

        hero.addView(heroText, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM));
        root.addView(hero, matchWrap(dp(11)));

        for (int i = 0; i < ITEMS.length; i++) {
            final String[] it = ITEMS[i];
            final String key = it[0];
            final String title = it[1];
            final boolean hasPanel = false;

            FrameLayout card = whiteFrame(dp(20), 0x8CFFFFFF);
            card.setClipToOutline(true);

            Bitmap bmp = image(it[3]);
            if (bmp != null) {
                ImageView iv = new ImageView(ctx);
                iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                iv.setImageBitmap(bmp);
                card.addView(iv, new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT, dp(78)));
                View sh = new View(ctx);
                sh.setBackgroundDrawable(new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                        new int[]{0xF7FFFFFF, 0xA6FFFFFF, 0x33FFFFFF}));
                card.addView(sh, new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT, dp(78)));
            }

            LinearLayout txt = new LinearLayout(ctx);
            txt.setOrientation(LinearLayout.VERTICAL);
            txt.addView(text(title, 16, 0xFF43587A, true, dp(2)));
            txt.addView(text(it[2], 11, 0xFF8496AE, false, 0));
            FrameLayout.LayoutParams tp = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    Gravity.START | Gravity.CENTER_VERTICAL);
            tp.leftMargin = dp(14);
            card.addView(txt, tp);

            final SwitchBox box = new SwitchBox(ctx, swOn(key));
            FrameLayout.LayoutParams bp = new FrameLayout.LayoutParams(
                    dp(52), dp(30), Gravity.END | Gravity.CENTER_VERTICAL);
            bp.rightMargin = dp(13);
            card.addView(box, bp);

            final LinearLayout holder = new LinearLayout(ctx);
            holder.setOrientation(LinearLayout.VERTICAL);
            holder.addView(card, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));

            host.bindSwitch(key, box, new Runnable() {
                @Override public void run() {
                    refreshHero(pill);
                }
            });

            box.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    boolean z = !swOn(key);
                    swSet(key, z);
                    box.setChecked(z);

                    if (com.cathelperawa.owo.app.feature.FeaturePttAutoText.K.equals(key)) {

                        toast(z ? "已打开，重启 TIM 生效" : "已关闭，重启 TIM 生效");
                    }
                    press(v);
                    refreshHero(pill);
                    toast(title + (z ? " 开启喵～" : " 关闭喵～"));
                }
            });

            card.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    press(v);
                    boolean z = !swOn(key);
                    swSet(key, z);
                    box.setChecked(z);
                    refreshHero(pill);
                }
            });

            root.addView(holder, matchWrap(dp(11)));
        }

        TextView tip = text("开启后立即生效，无需重启 TIM", 11, 0xFFA8B4C4, false, 0);
        tip.setGravity(Gravity.CENTER);
        LinearLayout tipRow = new LinearLayout(ctx);
        tipRow.setOrientation(LinearLayout.HORIZONTAL);
        tipRow.setGravity(Gravity.CENTER);
        tipRow.addView(tip);
        root.addView(tipRow, matchWrap(0));

        fadeIn(root);
        return pageRoot;
    }

}
