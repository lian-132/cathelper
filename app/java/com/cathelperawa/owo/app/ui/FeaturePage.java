package com.cathelperawa.owo.app.ui;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public final class FeaturePage extends Page {

    private static final String[][] CHAT_ITEMS = {
            {"chat_revoke", "防撤回", "拦截撤回指令并提示"},
            {"chat_repeat", "长按复读", "长按消息出现「复读」按钮"},
            {"chat_customdice", "自定义点数", "发骰子/猜拳时自选结果"},
            {"chat_emojiimg", "表情看图", "以图片的方式打开表情"},
            {"chat_flashpic", "闪照破解", "以图片的方式打开闪照"},
            {"chat_edittext", "改本地文本", "长按消息改显示文本（仅本地）"},

            {"chat_noat", "去除回复自动艾特", "回复消息时不再自动 @ 对方"},
            {"chat_showat", "显示艾特对象", "群聊里把 @ 的对象显示出来"},
            {"chat_mixsend", "文字表情复合消息", "文字和表情可以一起发"},
    };

    private final String page;
    private final String titleText;

    public FeaturePage(Host host, String page, String titleText) {
        super(host);
        this.page = page;
        this.titleText = titleText;
    }

    @Override
    public View build() {
        final boolean isChat = PAGE_CHAT.equals(page);
        final String[][] items = CHAT_ITEMS;

        String[] arts = new String[]{"c_revoke", "c_repeat", "c_dice", "c_emoji", "c_flash", "c_edittext",
                                     "p_noat", "p_showat", "p_mix"};

        View[] out = new View[3];

        LinearLayout root = pageRoot(titleText, "猫娘助手 · " + titleText, out, true, 12);

        FrameLayout hero = new FrameLayout(ctx);
        GradientDrawable hs = new GradientDrawable();
        hs.setColor(0xFFFFFFFF);
        hs.setCornerRadius(dp(26));
        hs.setStroke(dp(1), 0x77FFFFFF);
        hero.setBackgroundDrawable(hs);
        hero.setClipToOutline(true);
        hero.setElevation(dp(7));
        Bitmap hb = image(isChat ? "h_chat" : "h_rp");
        if (hb != null) {
            ImageView iv = new ImageView(ctx);
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            iv.setImageBitmap(hb);
            hero.addView(iv, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, dp(150)));
        }
        View hshade = new View(ctx);
        hshade.setBackgroundDrawable(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0x00FFFFFF, 0x99FFFFFF, 0xFAFFFFFF}));
        hero.addView(hshade, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, dp(100), Gravity.BOTTOM));

        LinearLayout ht = new LinearLayout(ctx);
        ht.setOrientation(LinearLayout.VERTICAL);
        ht.setPadding(dp(16), 0, dp(16), dp(14));

        int onCount = 0;
        for (String[] it : items) if (swOn(it[0])) onCount++;
        final String heroTxt = onCount == 0 ? "全部关闭"
                : (onCount == items.length ? "已全部开启" : "已开启 " + onCount + " / " + items.length);
        final TextView heroState = pill(heroTxt, onCount == 0 ? 0xFF7FA5E6 : 0xFFF07CA8);
        LinearLayout hw = new LinearLayout(ctx);
        hw.setOrientation(LinearLayout.HORIZONTAL);
        hw.addView(heroState, wrapWrap());
        ht.addView(hw, matchWrap(dp(7)));
        ht.addView(text(titleText, 20, 0xFF4F6485, true, dp(3)));
        ht.addView(text("让 TIM 聊天更顺手",
                12, 0xFF8A9CB5, false, 0));
        hero.addView(ht, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM));
        root.addView(hero, matchWrap(dp(11)));

        for (int i = 0; i < items.length; i++) {
            final String key = items[i][0];
            final String name = items[i][1];
            final String descText = items[i][2];
            final String artKey = arts[Math.min(i, arts.length - 1)];

            FrameLayout card = whiteFrame(dp(20), 0x8CFFFFFF);
            card.setClipToOutline(true);
            Bitmap cb = image(artKey);
            if (cb != null) {
                ImageView iv = new ImageView(ctx);
                iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                iv.setImageBitmap(cb);
                card.addView(iv, new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT, dp(78)));
                View sh = new View(ctx);
                sh.setBackgroundDrawable(new GradientDrawable(
                        GradientDrawable.Orientation.LEFT_RIGHT,
                        new int[]{0xF7FFFFFF, 0xA6FFFFFF, 0x33FFFFFF}));
                card.addView(sh, new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT, dp(78)));
            }
            LinearLayout ct = new LinearLayout(ctx);
            ct.setOrientation(LinearLayout.VERTICAL);
            ct.addView(text(name, 16, 0xFF43587A, true, dp(2)));
            ct.addView(text(descText, 11, 0xFF8496AE, false, 0));
            FrameLayout.LayoutParams ctp = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.START | Gravity.CENTER_VERTICAL);
            ctp.leftMargin = dp(14);
            card.addView(ct, ctp);

            final SwitchBox sw = new SwitchBox(ctx, swOn(key));
            FrameLayout.LayoutParams swp = new FrameLayout.LayoutParams(
                    dp(52), dp(30), Gravity.END | Gravity.CENTER_VERTICAL);
            swp.rightMargin = dp(13);
            card.addView(sw, swp);

            host.bindSwitch(key, sw, new Runnable() {
                @Override public void run() { refreshHero(heroState, items); }
            });

            sw.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    boolean now = !swOn(key);
                    swSet(key, now);
                    sw.setChecked(now);
                    press(v);
                    refreshHero(heroState, items);
                    toast(now ? (name + " 开启喵～") : (name + " 关闭喵～"));
                }
            });
            card.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    boolean now = !swOn(key);
                    swSet(key, now);
                    sw.setChecked(now);
                    press(v);
                    refreshHero(heroState, items);
                    toast(now ? (name + " 开启喵～") : (name + " 关闭喵～"));
                }
            });
            root.addView(card, matchWrap(dp(11)));
        }

        root.setAlpha(0f);
        root.setTranslationY(dp(16));
        root.animate().alpha(1f).translationY(0f).setDuration(420)
                .setInterpolator(new android.view.animation.DecelerateInterpolator()).start();

        return out[0];
    }

    private void refreshHero(TextView heroState, String[][] items) {
        int oc = 0;
        for (String[] it : items) if (swOn(it[0])) oc++;
        heroState.setText(oc == 0 ? "全部关闭"
                : (oc == items.length ? "已全部开启" : "已开启 " + oc + " / " + items.length));
        ((GradientDrawable) heroState.getBackground())
                .setColor(oc == 0 ? 0xFFFFFFFF : 0xFFFFF0F6);
        heroState.setTextColor(oc == 0 ? 0xFF7FA5E6 : 0xFFF07CA8);
    }
}
