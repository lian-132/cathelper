package com.cathelperawa.owo.app.ui;

import android.graphics.Bitmap;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public final class GroupPage extends Page {

    public static final String K_OWNER_MENU = "group_owner_menu";

    public static final String K_EXTRACT = "group_menu_extract";

    private static final String[][] ITEMS = {
            {K_EXTRACT, "长按头像提取",
                    "不看身份，长按任何人头像都能提取消息 / 卡片", "h_group_card"},
            {K_OWNER_MENU, "群主便捷菜单",
                    "在群聊里长按成员头像，弹出群主管理面板（可踢人）", "h_group_card"},
    };

    private TextView pill;

    public GroupPage(Page.Host host) {
        super(host);
    }

    @Override
    public View build() {
        View[] out = new View[2];
        LinearLayout root = pageRoot("群管理", "群主 / 管理员专用", out, true, 12);

        root.addView(hero(), matchWrap(dp(12)));
        for (int i = 0; i < ITEMS.length; i++) {
            root.addView(itemCard(ITEMS[i]), matchWrap(dp(11)));
        }
        root.addView(tip(), matchWrap(0));
        return out[0];
    }

    private View hero() {
        FrameLayout hero = new FrameLayout(ctx);
        GradientDrawable hg = new GradientDrawable();
        hg.setColor(0xFFFFFFFF);
        hg.setCornerRadius(dp(26));
        hg.setStroke(dp(1), 0x77FFFFFF);
        hero.setBackgroundDrawable(hg);
        hero.setClipToOutline(true);
        hero.setElevation(dp(7));

        Bitmap bmp = image("h_group");
        if (bmp != null) {
            ImageView iv = new ImageView(ctx);
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            iv.setImageBitmap(bmp);
            hero.addView(iv, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, dp(150)));
        }
        View veil = new View(ctx);
        veil.setBackgroundDrawable(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0x00FFFFFF, 0x99FFFFFF, 0xF8FFFFFF}));
        hero.addView(veil, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, dp(100), Gravity.BOTTOM));

        LinearLayout box = new LinearLayout(ctx);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(16), 0, dp(16), dp(14));

        pill = new TextView(ctx);
        pill.setTextSize(11);
        pill.setPadding(dp(10), dp(4), dp(10), dp(4));
        GradientDrawable pg = new GradientDrawable();
        pg.setCornerRadius(dp(10));
        pg.setColor(0xFFFFFFFF);
        pill.setBackgroundDrawable(pg);
        box.addView(pill, wrapWrap());

        box.addView(text("群管理", 21, 0xFF43587A, true, dp(3)), matchWrap(dp(6)));
        box.addView(text("给群主用的快捷入口", 11, 0xFF8496AE, false, 0), matchWrap(0));

        hero.addView(box, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM));

        refreshPill();
        return hero;
    }

    private void refreshPill() {
        if (pill == null) return;
        boolean on = swOn(K_OWNER_MENU);
        pill.setText(on ? "已开启" : "已关闭");
        GradientDrawable bg = (GradientDrawable) pill.getBackground();
        bg.setColor(on ? 0xFFFFC2DF : 0xFFFFFFFF);
        pill.setTextColor(on ? 0xFFF07AAE : 0xFF7F90A6);
    }

    private View itemCard(final String[] it) {
        final String key = it[0];
        final String title = it[1];

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
            sh.setBackgroundDrawable(new GradientDrawable(
                    GradientDrawable.Orientation.LEFT_RIGHT,
                    new int[]{0xF7FFFFFF, 0xA6FFFFFF, 0x33FFFFFF}));
            card.addView(sh, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, dp(78)));
        }

        LinearLayout txt = new LinearLayout(ctx);
        txt.setOrientation(LinearLayout.VERTICAL);
        txt.addView(text(title, 16, 0xFF43587A, true, dp(2)), matchWrap(0));
        txt.addView(text(it[2], 11, 0xFF8496AE, false, 0), matchWrap(0));
        FrameLayout.LayoutParams tp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.START | Gravity.CENTER_VERTICAL);
        tp.leftMargin = dp(14);
        tp.rightMargin = dp(70);
        card.addView(txt, tp);

        final SwitchBox box = new SwitchBox(ctx, swOn(key));
        FrameLayout.LayoutParams bp = new FrameLayout.LayoutParams(
                dp(52), dp(30), Gravity.END | Gravity.CENTER_VERTICAL);
        bp.rightMargin = dp(13);
        card.addView(box, bp);

        host.bindSwitch(key, box, new Runnable() {
            @Override public void run() {
                refreshPill();
            }
        });

        box.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                boolean z = !swOn(key);
                swSet(key, z);
                box.setChecked(z);
                press(v);
                refreshPill();
                toast(title + (z ? " 开启喵～" : " 关闭喵～"));
            }
        });

        card.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                press(v);
                boolean z = !swOn(key);
                swSet(key, z);
                box.setChecked(z);
                refreshPill();
            }
        });

        return card;
    }

    private View tip() {
        TextView t = text("开启后立即生效，无需重启 " + com.cathelperawa.owo.app.core.Host.name(), 11, 0xFFA8B4C4, false, 0);
        t.setGravity(Gravity.CENTER);
        return t;
    }
}
