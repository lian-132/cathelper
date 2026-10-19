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

import java.util.Map;

public final class CatPage extends Page {

    private static final String[][] CAT_SWITCHES = {
            {"cat_total", "总开关", "猫娘化的总闸"},
            {"cat_base", "基础猫娘化", "什么→什喵、你→您"},
            {"cat_spoiled", "撒娇模式", "短句套预设回复"},
            {"cat_endmeow", "结尾加喵", "句尾补一个喵"},
            {"cat_emoticon", "加颜文字", "随机附赠颜文字"},
            {"cat_cute", "随机卖萌", "蹭蹭、尾巴摇摇"},
            {"cat_wave", "波浪号", "句尾加 ~"},
            {"cat_catname", "称呼替换", "把「我」换成猫猫名"},
            {"cat_mastername", "主人称呼", "把「你」换成主人名"},
    };

    public CatPage(Host host) { super(host); }

    private final java.util.Map<String, SwitchBox> boxes =
            new java.util.LinkedHashMap<String, SwitchBox>();
    private TextView heroTagRef;

    private void syncSiblings() {
        for (java.util.Map.Entry<String, SwitchBox> e : boxes.entrySet()) {
            try {
                e.getValue().setChecked(swOn(e.getKey()));
            } catch (Throwable ignore) {
            }
        }
        if (heroTagRef != null) paintHero(swOn("cat_total"));
    }

    private void paintHero(boolean on) {
        heroTagRef.setText(on ? "运行中" : "已关闭");
        heroTagRef.setTextColor(on ? 0xFFF07CA8 : 0xFF7FA5E6);
        ((GradientDrawable) heroTagRef.getBackground())
                .setColor(on ? 0xFFFFF0F6 : 0xFFFFFFFF);
    }

    @Override
    public View build() {
        View[] out = new View[2];
        LinearLayout root = pageRoot("猫娘化引擎", "配置 · 作用范围", out);

        FrameLayout hero = new FrameLayout(ctx);
        GradientDrawable hs = new GradientDrawable();
        hs.setColor(0xFFFFFFFF);
        hs.setCornerRadius(dp(26));
        hs.setStroke(dp(1), 0x77FFFFFF);
        hero.setBackgroundDrawable(hs);
        hero.setClipToOutline(true);
        hero.setElevation(dp(7));
        Bitmap hb = image("hero1");
        if (hb != null) {
            ImageView iv = new ImageView(ctx);
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            iv.setImageBitmap(hb);
            hero.addView(iv, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, dp(140)));
        }
        View hsh = new View(ctx);
        hsh.setBackgroundDrawable(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0x00FFFFFF, 0x99FFFFFF, 0xFAFFFFFF}));
        hero.addView(hsh, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, dp(96), Gravity.BOTTOM));
        LinearLayout ht = new LinearLayout(ctx);
        ht.setOrientation(LinearLayout.VERTICAL);
        ht.setPadding(dp(16), 0, dp(16), dp(14));
        final TextView heroTag = pill(cfgGet("cat_total", false) ? "运行中" : "已关闭",
                cfgGet("cat_total", false) ? 0xFFF07CA8 : 0xFF7FA5E6);
        heroTagRef = heroTag;
        LinearLayout hw = new LinearLayout(ctx);
        hw.setOrientation(LinearLayout.HORIZONTAL);
        hw.addView(heroTag, wrapWrap());
        ht.addView(hw, matchWrap(dp(6)));
        ht.addView(text("猫娘化引擎", 19, 0xFF4F6485, true, dp(2)));
        ht.addView(text("发送前自动把文字变猫娘", 12, 0xFF8A9CB5, false, 0));
        hero.addView(ht, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM));
        root.addView(hero, matchWrap(dp(11)));

        for (final String[] sw : CAT_SWITCHES) {
            final boolean isTotal = "cat_total".equals(sw[0]);

            SwitchCard.OnToggled cb = new SwitchCard.OnToggled() {
                @Override public void run(boolean nowOn) {
                    syncSiblings();
                }
            };
            FrameLayout card = SwitchCard.build(ctx, sw[0], sw[1], sw[2], true, 72, null, cb);
            root.addView(card, matchWrap(dp(9)));

            SwitchBox box = SwitchCard.findBox(card);
            if (box != null) {
                boxes.put(sw[0], box);
                host.bindSwitch(sw[0], box, new Runnable() {
                    @Override public void run() { syncSiblings(); }
                });
            }
        }

        FrameLayout scopeCard = whiteFrame(dp(18), 0x8CFFFFFF);
        scopeCard.setClipToOutline(true);
        LinearLayout sc = new LinearLayout(ctx);
        sc.setOrientation(LinearLayout.HORIZONTAL);
        sc.setGravity(Gravity.CENTER_VERTICAL);
        sc.setPadding(dp(14), dp(13), dp(14), dp(13));
        LinearLayout st = new LinearLayout(ctx);
        st.setOrientation(LinearLayout.VERTICAL);
        st.addView(text("作用范围", 15, 0xFF43587A, true, dp(2)));
        Map<String, String> ps = MsgStore.peers();
        st.addView(text(MsgStore.isScopeAll() ? "全部会话（已记录 " + ps.size() + " 个）"
                : "已选 " + ps.size() + " 个会话中的部分", 11, 0xFF8496AE, false, 0));
        sc.addView(st, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        sc.addView(text("›", 22, 0xFFB0BDD1, true, 0));
        scopeCard.addView(sc);
        scopeCard.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                press(v);
                host.open(PAGE_SCOPE, "作用范围");
            }
        });
        root.addView(scopeCard, matchWrap(dp(9)));

        fadeIn(root);
        return out[0];
    }
}
