package com.cathelperawa.owo.app.bili;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.cathelperawa.owo.app.ui.CatIcon;

public final class BiliCards {

    private BiliCards() {}

    public static View videoCard(Context c, final BiliApi.Item it, int widthPx,
                                 final View.OnClickListener onClick) {
        LinearLayout card = BiliUi.card(c, 12);
        card.setClipToOutline(true);
        int coverH = Math.max(1, widthPx * 9 / 16);

        FrameLayout cover = new FrameLayout(c);
        ImageView iv = new ImageView(c);
        iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
        cover.addView(iv, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        BiliImage.attach(iv, it.cover, 12);
        iv.setBackgroundColor(0x22000000);

        if (it.duration > 0) {
            TextView dur = BiliUi.text(c, BiliApi.clock(it.duration), 10f, 0xFFFFFFFF, false);
            GradientDrawable g = new GradientDrawable();
            g.setColor(0x9E000000);
            g.setCornerRadius(BiliUi.dp(c, 4));
            dur.setBackground(g);
            dur.setPadding(BiliUi.dp(c, 5), BiliUi.dp(c, 1),
                    BiliUi.dp(c, 5), BiliUi.dp(c, 1));
            FrameLayout.LayoutParams dlp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.END | Gravity.BOTTOM);
            dlp.rightMargin = BiliUi.dp(c, 6);
            dlp.bottomMargin = BiliUi.dp(c, 6);
            cover.addView(dur, dlp);
        }
        card.addView(cover, new LinearLayout.LayoutParams(widthPx, coverH));

        LinearLayout info = new LinearLayout(c);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setPadding(BiliUi.dp(c, 9), BiliUi.dp(c, 8),
                BiliUi.dp(c, 9), BiliUi.dp(c, 10));

        TextView title = BiliUi.text(c, it.title, 13f, BiliUi.MAIN, false);
        title.setMaxLines(2);
        title.setEllipsize(android.text.TextUtils.TruncateAt.END);
        title.setMinHeight(BiliUi.dp(c, 37));
        info.addView(title);

        LinearLayout meta = new LinearLayout(c);
        meta.setOrientation(LinearLayout.HORIZONTAL);
        meta.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams metaLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        metaLp.topMargin = BiliUi.dp(c, 7);
        info.addView(meta, metaLp);

        meta.addView(BiliUi.icon(c, CatIcon.PLAY, 10, BiliUi.CARD_SUB));
        TextView play = BiliUi.text(c, BiliApi.count(it.play), 10.5f, BiliUi.CARD_SUB, false);
        LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        plp.leftMargin = BiliUi.dp(c, 3);
        plp.rightMargin = BiliUi.dp(c, 9);
        meta.addView(play, plp);

        meta.addView(BiliUi.icon(c, CatIcon.DANMAKU, 10, BiliUi.CARD_SUB));
        TextView dm = BiliUi.text(c, BiliApi.count(it.danmaku), 10.5f, BiliUi.CARD_SUB, false);
        LinearLayout.LayoutParams dlp2 = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dlp2.leftMargin = BiliUi.dp(c, 3);
        meta.addView(dm, dlp2);

        LinearLayout upRow = new LinearLayout(c);
        upRow.setOrientation(LinearLayout.HORIZONTAL);
        upRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams upLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        upLp.topMargin = BiliUi.dp(c, 5);
        info.addView(upRow, upLp);

        TextView up = BiliUi.text(c, it.up, 10.5f, BiliUi.CARD_SUB, false);
        up.setSingleLine(true);
        up.setEllipsize(android.text.TextUtils.TruncateAt.END);
        upRow.addView(up, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        upRow.addView(BiliUi.icon(c, CatIcon.MORE, 13, BiliUi.CARD_SUB));

        card.addView(info, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        if (onClick != null) card.setOnClickListener(onClick);
        card.setTag(it);
        return card;
    }

    public static View wideCard(Context c, final BiliApi.Item it,
                                final View.OnClickListener onClick) {
        LinearLayout row = new LinearLayout(c);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.TOP);
        row.setPadding(BiliUi.dp(c, 10), BiliUi.dp(c, 10),
                BiliUi.dp(c, 10), BiliUi.dp(c, 10));
        GradientDrawable g = new GradientDrawable();
        g.setColor(BiliUi.CARD_BG);
        g.setCornerRadius(BiliUi.dp(c, 12));
        g.setStroke(Math.max(1, BiliUi.dp(c, 0.6f)), BiliUi.CARD_LINE);
        row.setBackground(g);

        int w = BiliUi.dp(c, 128);
        int h = w * 9 / 16;
        FrameLayout box = new FrameLayout(c);
        ImageView iv = new ImageView(c);
        iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
        box.addView(iv, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        BiliImage.attach(iv, it.cover, 8);
        iv.setBackgroundColor(0x22000000);
        if (it.duration > 0) {
            TextView dur = BiliUi.text(c, BiliApi.clock(it.duration), 9.5f, 0xFFFFFFFF, false);
            GradientDrawable g2 = new GradientDrawable();
            g2.setColor(0x9E000000);
            g2.setCornerRadius(BiliUi.dp(c, 4));
            dur.setBackground(g2);
            dur.setPadding(BiliUi.dp(c, 4), 0, BiliUi.dp(c, 4), 0);
            FrameLayout.LayoutParams dlp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.END | Gravity.BOTTOM);
            dlp.rightMargin = BiliUi.dp(c, 4);
            dlp.bottomMargin = BiliUi.dp(c, 4);
            box.addView(dur, dlp);
        }
        row.addView(box, new LinearLayout.LayoutParams(w, h));

        LinearLayout info = new LinearLayout(c);
        info.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        ilp.leftMargin = BiliUi.dp(c, 10);
        row.addView(info, ilp);

        TextView title = BiliUi.text(c, it.title, 13f, BiliUi.MAIN, false);
        title.setMaxLines(2);
        title.setEllipsize(android.text.TextUtils.TruncateAt.END);
        info.addView(title);

        LinearLayout meta = new LinearLayout(c);
        meta.setOrientation(LinearLayout.HORIZONTAL);
        meta.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams mlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        mlp.topMargin = BiliUi.dp(c, 8);
        info.addView(meta, mlp);
        meta.addView(BiliUi.icon(c, CatIcon.PLAY, 10, BiliUi.CARD_SUB));
        TextView play = BiliUi.text(c, BiliApi.count(it.play), 10.5f, BiliUi.CARD_SUB, false);
        LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        plp.leftMargin = BiliUi.dp(c, 3);
        meta.addView(play, plp);

        TextView up = BiliUi.text(c, it.up, 10.5f, BiliUi.CARD_SUB, false);
        LinearLayout.LayoutParams ulp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ulp.topMargin = BiliUi.dp(c, 6);
        info.addView(up, ulp);

        if (onClick != null) row.setOnClickListener(onClick);
        row.setTag(it);
        return row;
    }

    public static View relatedRow(Context c, final BiliApi.Item it,
                                  final View.OnClickListener onClick) {
        LinearLayout row = new LinearLayout(c);
        row.setOrientation(LinearLayout.HORIZONTAL);
        int w = BiliUi.dp(c, 120);
        int h = w * 9 / 16;

        FrameLayout box = new FrameLayout(c);
        ImageView iv = new ImageView(c);
        iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
        box.addView(iv, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        BiliImage.attach(iv, it.cover, 8);
        iv.setBackgroundColor(0x22000000);
        row.addView(box, new LinearLayout.LayoutParams(w, h));

        LinearLayout info = new LinearLayout(c);
        info.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        ilp.leftMargin = BiliUi.dp(c, 10);
        row.addView(info, ilp);

        TextView title = BiliUi.text(c, it.title, 12.5f, BiliUi.MAIN, false);
        title.setMaxLines(2);
        title.setEllipsize(android.text.TextUtils.TruncateAt.END);
        info.addView(title);

        TextView meta = BiliUi.text(c, BiliApi.count(it.play) + "播放 · " + it.up,
                10f, BiliUi.CARD_SUB, false);
        LinearLayout.LayoutParams mlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        mlp.topMargin = BiliUi.dp(c, 6);
        info.addView(meta, mlp);

        if (onClick != null) row.setOnClickListener(onClick);
        return row;
    }

    public static TextView loading(Context c, String s) {
        TextView t = BiliUi.text(c, s, 11.5f, BiliUi.SUB, false);
        t.setGravity(Gravity.CENTER);
        t.setPadding(0, BiliUi.dp(c, 14), 0, BiliUi.dp(c, 14));
        return t;
    }

    public static TextView badge(Context c, String s, int bg, int color) {
        TextView t = BiliUi.text(c, s, 9.5f, color, false);
        GradientDrawable g = new GradientDrawable();
        g.setColor(bg);
        g.setCornerRadius(BiliUi.dp(c, 4));
        t.setBackground(g);
        t.setPadding(BiliUi.dp(c, 5), BiliUi.dp(c, 1),
                BiliUi.dp(c, 5), BiliUi.dp(c, 1));
        return t;
    }

    static int screenWidth(Context c) {
        return c.getResources().getDisplayMetrics().widthPixels;
    }

    static int colorMix(int a, int b, float t) {
        int ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        return Color.rgb((int) (ar + (br - ar) * t), (int) (ag + (bg - ag) * t),
                (int) (ab + (bb - ab) * t));
    }

    static android.view.ViewGroup.LayoutParams matchWrap() {
        return new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }
}
