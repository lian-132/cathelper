package com.cathelperawa.owo.app.ui;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public abstract class Page {

    public static final String PAGE_CHAT = "chat";

    public static final String PAGE_UNLOCK = "unlock";
    public static final String PAGE_MSG = "msg";
    public static final String PAGE_CAT = "cat_engine";
    public static final String PAGE_SCOPE = "scope";
    public static final String PAGE_HOOK = "hook";

    public static final String PAGE_STATUS = "status";

    public static final String PAGE_CONFIG = "config";

    public interface Host {

        Context context();

        void close();

        void closeWithFade();

        void open(String page, String title);

        void setPageView(View v);

        void bindSwitch(String uiKey, SwitchBox box, Runnable onSynced);

        void setHeroTimer(java.util.Timer t);

        void setHomeScroll(ScrollView sv);

        void openTerminal();
    }

    protected final Host host;
    protected final Context ctx;

    protected Page(Host host) {
        this.host = host;
        this.ctx = host.context();
    }

    public abstract View build();

    protected int dp(float v) { return UiKit.dp(ctx, v); }

    protected TextView text(String s, float size, int color, boolean bold, int bottomPad) {
        return UiKit.text(ctx, s, size, color, bold, bottomPad);
    }

    protected TextView pill(String s, int color) { return UiKit.pill(ctx, s, color); }

    protected LinearLayout whiteCard(int radius, int color) {
        return UiKit.whiteCard(ctx, radius, color);
    }

    protected FrameLayout whiteFrame(int radius, int color) {
        return UiKit.whiteFrame(ctx, radius, color);
    }

    protected LinearLayout.LayoutParams matchWrap(int bottomMargin) {
        return UiKit.matchWrap(ctx, bottomMargin);
    }

    protected LinearLayout.LayoutParams wrapWrap() { return UiKit.wrapWrap(); }

    protected LinearLayout.LayoutParams matchFixed(int heightDp, int bottomDp) {
        return UiKit.matchFixed(ctx, heightDp, bottomDp);
    }

    protected void press(View v) { UiKit.press(v); }

    protected void spin(View v) { UiKit.spin(v); }

    protected void toast(String msg) { UiKit.catToast(ctx, msg); }

    protected Bitmap bg() { return Images.get("bg"); }

    protected Bitmap image(String name) { return Images.get(name); }

    protected LinearLayout pageRoot(String titleText, String subText, View[] out) {
        return pageRoot(titleText, subText, out, false, 13);
    }

    protected LinearLayout pageRoot(String titleText, String subText, View[] out,
                                    final boolean fadeBack) {
        return pageRoot(titleText, subText, out, fadeBack, 13);
    }

    protected LinearLayout pageRoot(String titleText, String subText, View[] out,
                                    final boolean fadeBack, int topMarginDp) {
        FrameLayout pageRoot = new FrameLayout(ctx);
        Bitmap bgBmp = bg();
        if (bgBmp != null) {
            ImageView bgv = new ImageView(ctx);
            bgv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            bgv.setImageBitmap(bgBmp);
            pageRoot.addView(bgv, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
            View veil = new View(ctx);
            veil.setBackgroundDrawable(new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                    new int[]{0xC7E4EFFF, 0xC7F4EBFF, 0xCCFFE9F1}));
            pageRoot.addView(veil, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        } else {
            pageRoot.setBackgroundDrawable(new GradientDrawable(
                    GradientDrawable.Orientation.TL_BR,
                    new int[]{0xFFE4EFFF, 0xFFF4EBFF, 0xFFFFE9F1}));
        }

        ScrollView scroll = new ScrollView(ctx);
        scroll.setFillViewport(true);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        scroll.setVerticalScrollBarEnabled(false);

        LinearLayout root = new LinearLayout(ctx);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(15), dp(40), dp(15), dp(26));
        scroll.addView(root, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.WRAP_CONTENT));

        LinearLayout topbar = new LinearLayout(ctx);
        topbar.setOrientation(LinearLayout.HORIZONTAL);
        topbar.setGravity(Gravity.CENTER_VERTICAL);
        TextView back = text("‹", 30, 0xFF7FA5E6, true, 0);
        back.setGravity(Gravity.CENTER);
        topbar.addView(back, new LinearLayout.LayoutParams(dp(34), dp(40)));
        back.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                press(v);
                if (fadeBack) host.closeWithFade(); else host.close();
            }
        });
        LinearLayout tb = new LinearLayout(ctx);
        tb.setOrientation(LinearLayout.VERTICAL);
        tb.addView(text(titleText, 24, 0xFF6F86A8, true, dp(1)));
        tb.addView(text(subText, 12, 0xFF9FB0C7, false, 0));
        topbar.addView(tb, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        root.addView(topbar, matchWrap(dp(topMarginDp)));

        pageRoot.addView(scroll, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        out[0] = pageRoot;
        out[1] = root;
        return root;
    }

    protected void fadeIn(View root) {
        root.setAlpha(0f);
        root.animate().alpha(1f).setDuration(380).start();
    }

    protected LinearLayout pageRootEx(String titleText, String subText, View[] out) {
        LinearLayout root = pageRoot(titleText, subText, out);

        LinearLayout topbar = (LinearLayout) root.getChildAt(0);

        LinearLayout tb = (LinearLayout) topbar.getChildAt(1);
        TextView sub = (TextView) tb.getChildAt(1);
        if (out.length < 4) {

            throw new IllegalArgumentException(
                    "pageRootEx 需要 out 至少 4 个槽位，实际 " + out.length);
        }
        out[2] = topbar;
        out[3] = sub;
        return root;
    }

    protected Activity activity() {
        return (Activity) ctx;
    }

    protected boolean cfgGet(String key, boolean def) {
        return UiConfig.get(ctx, key, def);
    }

    protected void cfgSet(String key, boolean v) {
        UiConfig.set(ctx, key, v);
    }

    protected boolean swOn(String uiKey) {
        return UiConfig.swOn(ctx, uiKey);
    }

    protected void swSet(String uiKey, boolean v) {
        UiConfig.swSet(ctx, uiKey, v);
    }

    protected String cfgGetS(String key, String def) {
        return UiConfig.getString(ctx, key, def);
    }

    protected void cfgSetS(String key, String v) {
        UiConfig.setString(ctx, key, v);
    }
}
