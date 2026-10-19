package com.cathelperawa.owo.app.ui;

import android.graphics.Bitmap;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.cathelperawa.owo.app.MsgStore;

public final class HookPage extends Page {

    private static final String[][] HOOK_SWITCHES = {
            {"hook_capture", "长按提取", "长按消息出现「提取」按钮（原始协议数据）"},
            {"hook_autocap", "自动抓取消息", "收发消息自动记录到控制台"},
    };

    public HookPage(Host host) { super(host); }

    @Override
    public View build() {
        View[] out = new View[2];
        LinearLayout root = pageRoot("Hook 功能", "抓取 · 长按菜单", out);

        FrameLayout hero = new FrameLayout(ctx);
        GradientDrawable hs = new GradientDrawable();
        hs.setColor(0xFFFFFFFF);
        hs.setCornerRadius(dp(26));
        hs.setStroke(dp(1), 0x77FFFFFF);
        hero.setBackgroundDrawable(hs);
        hero.setClipToOutline(true);
        hero.setElevation(dp(7));
        Bitmap hb = image("h_hook");
        if (hb != null) {
            ImageView iv = new ImageView(ctx);
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            iv.setImageBitmap(hb);
            hero.addView(iv, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, dp(150)));
        }
        View hsh = new View(ctx);
        hsh.setBackgroundDrawable(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0x00FFFFFF, 0x99FFFFFF, 0xFAFFFFFF}));
        hero.addView(hsh, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, dp(100), Gravity.BOTTOM));
        LinearLayout ht = new LinearLayout(ctx);
        ht.setOrientation(LinearLayout.VERTICAL);
        ht.setPadding(dp(16), 0, dp(16), dp(14));
        boolean anyOn = cfgGet("hook_capture", false) || cfgGet("hook_autocap", false);
        LinearLayout hw = new LinearLayout(ctx);
        hw.setOrientation(LinearLayout.HORIZONTAL);
        final TextView heroTag = pill(anyOn ? "运行中" : "已关闭",
                anyOn ? 0xFFF07CA8 : 0xFF7FA5E6);
        hw.addView(heroTag, wrapWrap());
        ht.addView(hw, matchWrap(dp(6)));
        ht.addView(text("Hook 功能", 19, 0xFF4F6485, true, dp(2)));
        ht.addView(text("把 TIM 里的消息抓下来", 12, 0xFF8A9CB5, false, 0));
        hero.addView(ht, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM));
        root.addView(hero, matchWrap(dp(11)));

        String[] hookArts = {"art1", "art2"};
        final Runnable refreshHero = new Runnable() {
            @Override public void run() {
                boolean on = swOn("hook_capture") || swOn("hook_autocap");
                heroTag.setText(on ? "运行中" : "已关闭");
                heroTag.setTextColor(on ? 0xFFF07CA8 : 0xFF7FA5E6);
                ((GradientDrawable) heroTag.getBackground())
                        .setColor(on ? 0xFFFFF0F6 : 0xFFFFFFFF);
            }
        };
        for (int hi = 0; hi < HOOK_SWITCHES.length; hi++) {
            final String[] sw = HOOK_SWITCHES[hi];
            FrameLayout card = whiteFrame(dp(18), 0x8CFFFFFF);
            card.setClipToOutline(true);

            Bitmap cb = image(hookArts[Math.min(hi, hookArts.length - 1)]);
            if (cb != null) {
                ImageView iv = new ImageView(ctx);
                iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                iv.setImageBitmap(cb);
                card.addView(iv, new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT, dp(72)));
                View sh = new View(ctx);
                sh.setBackgroundDrawable(new GradientDrawable(
                        GradientDrawable.Orientation.LEFT_RIGHT,
                        new int[]{0xF7FFFFFF, 0xA6FFFFFF, 0x33FFFFFF}));
                card.addView(sh, new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT, dp(72)));
            }
            LinearLayout row = new LinearLayout(ctx);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(14), dp(0), dp(14), dp(0));
            LinearLayout texts = new LinearLayout(ctx);
            texts.setOrientation(LinearLayout.VERTICAL);
            texts.addView(text(sw[1], 15, 0xFF43587A, true, dp(2)));
            texts.addView(text(sw[2], 11, 0xFF8496AE, false, 0));
            row.addView(texts, new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            final SwitchBox box = new SwitchBox(ctx, cfgGet(sw[0], true));
            row.addView(box, new LinearLayout.LayoutParams(dp(52), dp(30)));
            FrameLayout.LayoutParams rp2 = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, dp(72));
            card.addView(row, rp2);
            final String key = sw[0];
            final String name = sw[1];

            host.bindSwitch(key, box, refreshHero);
            card.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    boolean now = !swOn(key);
                    swSet(key, now);
                    box.setChecked(now);
                    press(v);
                    refreshHero.run();
                    toast(now ? (name + " 开启喵～") : (name + " 关闭喵～"));
                }
            });
            root.addView(card, matchWrap(dp(9)));
        }

        FrameLayout msgCard = whiteFrame(dp(18), 0x8CFFFFFF);
        msgCard.setClipToOutline(true);
        LinearLayout mc = new LinearLayout(ctx);
        mc.setOrientation(LinearLayout.HORIZONTAL);
        mc.setGravity(Gravity.CENTER_VERTICAL);
        mc.setPadding(dp(14), dp(13), dp(14), dp(13));
        LinearLayout mt = new LinearLayout(ctx);
        mt.setOrientation(LinearLayout.VERTICAL);
        mt.addView(text("抓取记录", 15, 0xFF43587A, true, dp(2)));
        mt.addView(text("已抓 " + MsgStore.all().size() + " 条", 11, 0xFF8496AE, false, 0));
        mc.addView(mt, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        mc.addView(text("›", 22, 0xFFB0BDD1, true, 0));
        msgCard.addView(mc);
        msgCard.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                press(v);
                host.open(PAGE_MSG, "抓取记录");
            }
        });
        root.addView(msgCard, matchWrap(dp(9)));

        fadeIn(root);
        return out[0];
    }
}
