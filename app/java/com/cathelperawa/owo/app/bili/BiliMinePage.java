package com.cathelperawa.owo.app.bili;

import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.cathelperawa.owo.app.cookie.CookieStore;
import com.cathelperawa.owo.app.ui.CatIcon;

import java.util.List;

public final class BiliMinePage extends BiliPage {

    private LinearLayout body;
    private boolean loading = false;

    public BiliMinePage(BiliActivity host) {
        super(host);
    }

    @Override
    public View onCreate() {
        ScrollView scroll = new ScrollView(host);
        scroll.setFillViewport(true);
        body = new LinearLayout(host);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(BiliUi.dp(host, 16), BiliUi.dp(host, 4),
                BiliUi.dp(host, 16), BiliUi.dp(host, 24));
        scroll.addView(body, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        render(null);
        load();
        return scroll;
    }

    @Override
    public void onShow() {
        load();
    }

    private void load() {
        if (loading) return;
        loading = true;
        BiliThread.io(new Runnable() {
            @Override public void run() {
                final BiliApi.Me m = BiliApi.me(host);
                BiliThread.ui(new Runnable() {
                    @Override public void run() {
                        loading = false;
                        render(m);
                    }
                });
            }
        });
    }

    private void render(BiliApi.Me m) {
        body.removeAllViews();

        LinearLayout tools = new LinearLayout(host);
        tools.setOrientation(LinearLayout.HORIZONTAL);
        tools.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        tools.setPadding(0, BiliUi.dp(host, 8), 0, BiliUi.dp(host, 14));
        int[] glyphs = {CatIcon.MSG, CatIcon.COIN, CatIcon.MOON, CatIcon.SETTINGS};
        final String[] names = {"消息", "硬币", "夜间模式", "设置"};
        for (int i = 0; i < glyphs.length; i++) {
            final String name = names[i];
            
            CatIcon ic = BiliUi.icon(host, glyphs[i], 18, BiliUi.CARD_SUB);
            ic.setPill(0x2EFFFFFF, 7);
            LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            ilp.leftMargin = BiliUi.dp(host, 14);
            ic.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    Toast.makeText(host, name + " 还没接", Toast.LENGTH_SHORT).show();
                }
            });
            tools.addView(ic, ilp);
        }
        body.addView(tools);

        LinearLayout prof = new LinearLayout(host);
        prof.setOrientation(LinearLayout.HORIZONTAL);
        prof.setGravity(Gravity.CENTER_VERTICAL);

        FrameLayout avaBox = new FrameLayout(host);
        GradientDrawable avaBg = new GradientDrawable();
        avaBg.setColor(0x33FFFFFF);
        avaBg.setShape(GradientDrawable.OVAL);
        avaBox.setBackground(avaBg);
        ImageView avatar = new ImageView(host);
        avaBox.addView(avatar, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        prof.addView(avaBox, new LinearLayout.LayoutParams(
                BiliUi.dp(host, 58), BiliUi.dp(host, 58)));

        LinearLayout texts = new LinearLayout(host);
        texts.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        tlp.leftMargin = BiliUi.dp(host, 12);
        prof.addView(texts, tlp);

        boolean login = (m != null && m.login);
        LinearLayout nameRow = new LinearLayout(host);
        nameRow.setOrientation(LinearLayout.HORIZONTAL);
        nameRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView name = BiliUi.text(host, login ? m.uname : "点击登录", 17f, BiliUi.MAIN, true);
        nameRow.addView(name);
        if (login) {
            TextView lv = BiliCards.badge(host, "LV" + m.level, 0x2EFB7299, BiliUi.PINK);
            LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            llp.leftMargin = BiliUi.dp(host, 7);
            nameRow.addView(lv, llp);
        } else {
            TextView lv = BiliCards.badge(host, "LV0", 0x2EFB7299, BiliUi.PINK);
            LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            llp.leftMargin = BiliUi.dp(host, 7);
            nameRow.addView(lv, llp);
        }
        texts.addView(nameRow);

        String meta = login
                ? ("硬币 " + m.coin + " · 经验 " + m.exp + "/" + m.expNext)
                : "硬币 - · 经验 -/-";
        TextView metaTv = BiliUi.text(host, meta, 11f, BiliUi.CARD_SUB, false);
        LinearLayout.LayoutParams mlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        mlp.topMargin = BiliUi.dp(host, 6);
        texts.addView(metaTv, mlp);

        FrameLayout expTrack = new FrameLayout(host);
        GradientDrawable trackBg = new GradientDrawable();
        trackBg.setColor(0x29FFFFFF);
        trackBg.setCornerRadius(BiliUi.dp(host, 2));
        expTrack.setBackground(trackBg);
        View expFill = new View(host);
        GradientDrawable fillBg = new GradientDrawable();
        fillBg.setColor(BiliUi.ACCENT);
        fillBg.setCornerRadius(BiliUi.dp(host, 2));
        expFill.setBackground(fillBg);
        float ratio = 0f;
        if (login && m.expNext > 0) ratio = Math.min(1f, (float) m.exp / m.expNext);
        LinearLayout.LayoutParams elp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, BiliUi.dp(host, 2));
        elp.topMargin = BiliUi.dp(host, 8);
        expTrack.setLayoutParams(elp);
        FrameLayout.LayoutParams flp = new FrameLayout.LayoutParams(
                0, BiliUi.dp(host, 2));
        flp.width = 0;
        expFill.setLayoutParams(flp);
        expTrack.addView(expFill);
        final View fillRef = expFill;
        final float ratioRef = ratio;
        expTrack.post(new Runnable() {
            @Override public void run() {
                ViewGroup.LayoutParams p = fillRef.getLayoutParams();
                p.width = (int) (((View) fillRef.getParent()).getWidth() * ratioRef);
                fillRef.setLayoutParams(p);
            }
        });
        texts.addView(expTrack);

        if (login && m.face != null && m.face.length() > 0) {
            BiliImage.attachAvatar(avatar, m.face);
        } else {
            avatar.setImageDrawable(null);
            CatIcon ghost = BiliUi.icon(host, CatIcon.USER, 26, 0x88FFFFFF);
            FrameLayout.LayoutParams glp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER);
            avaBox.addView(ghost, glp);
        }

        if (!login) {
            prof.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { host.openLogin(); }
            });
        }
        body.addView(prof);

        LinearLayout nums = new LinearLayout(host);
        nums.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams nlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        nlp.topMargin = BiliUi.dp(host, 22);
        body.addView(nums, nlp);
        addNum(nums, login ? String.valueOf(m.dynamic) : "-", "动态");
        addNum(nums, login ? String.valueOf(m.following) : "-", "关注");
        addNum(nums, login ? String.valueOf(m.follower) : "-", "粉丝");

        LinearLayout card = BiliUi.card(host, 14);
        card.setPadding(BiliUi.dp(host, 4), BiliUi.dp(host, 14),
                BiliUi.dp(host, 4), BiliUi.dp(host, 14));
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        clp.topMargin = BiliUi.dp(host, 18);
        body.addView(card, clp);

        LinearLayout entries = new LinearLayout(host);
        entries.setOrientation(LinearLayout.HORIZONTAL);
        card.addView(entries);

        int[] eg = {CatIcon.DOWNLOAD, CatIcon.HISTORY, CatIcon.SUBSCRIBE, CatIcon.LATER};
        String[] en = {"离线缓存", "观看记录", "我的订阅", "稍后再看"};
        for (int i = 0; i < 4; i++) {
            final String label = en[i];
            LinearLayout col = BiliUi.iconActionColumn(host, eg[i], label,
                    BiliUi.CARD_SUB, 24, 11f);
            entries.addView(col, new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            col.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    Toast.makeText(host, label + " 还没接", Toast.LENGTH_SHORT).show();
                }
            });
        }

        TextView tip = BiliUi.text(host, login
                        ? "登录态来自「网页登录」，过期了回聚合中心重新登录"
                        : "点上面的头像去登录（和网易云用的是同一套 Cookie 通道）",
                10.5f, BiliUi.SUB, false);
        tip.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams tlp2 = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tlp2.topMargin = BiliUi.dp(host, 16);
        body.addView(tip, tlp2);
    }

    private void addNum(LinearLayout row, String value, String label) {
        LinearLayout col = new LinearLayout(host);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(Gravity.CENTER);
        TextView v = BiliUi.text(host, value, 16f, BiliUi.MAIN, true);
        v.setGravity(Gravity.CENTER);
        col.addView(v);
        TextView l = BiliUi.text(host, label, 11f, BiliUi.CARD_SUB, false);
        l.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        llp.topMargin = BiliUi.dp(host, 4);
        col.addView(l, llp);
        row.addView(col, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
    }
}
