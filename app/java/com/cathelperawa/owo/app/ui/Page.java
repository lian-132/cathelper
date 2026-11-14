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

    private static final java.util.WeakHashMap<View, Page> REG =
            new java.util.WeakHashMap<View, Page>();

    protected static void bindPage(View v, Page page) {
        if (v == null || page == null) return;
        REG.put(v, page);
    }

    public static Page pageOf(View v) {
        return v == null ? null : REG.get(v);
    }

    public boolean onBack() {
        return false;
    }

    public static final String PAGE_CHAT = "chat";

    public static final String PAGE_GROUP = "group";

    public static final String PAGE_UNLOCK = "unlock";

    public static final String PAGE_PARSE = "parse";

    public static final String PAGE_FILE = "file";

    public static final String PAGE_FAKE = "fake";
    public static final String PAGE_REDPACKET = "redpacket";
    public static final String PAGE_MSG = "msg";
    public static final String PAGE_CAT = "cat_engine";
    public static final String PAGE_SCOPE = "scope";
    public static final String PAGE_HOOK = "hook";

    public static final String PAGE_STATUS = "status";

    public static final String PAGE_FULLCHECK = "fullcheck";

    public static final String PAGE_CONFIG = "config";
    public static final String PAGE_RP_AUTO = "rp_auto_set";
    public static final String PAGE_RP_MANUAL = "rp_manual_set";

    public static final String PAGE_CLOCKIN = "clockin_set";

    public static final String PAGE_MSGTIME = "msgtime_set";

    public static final String PAGE_SEND = "send";

    public static final String PAGE_ANTI = "anti_detect_set";

    public interface Host {

        Context context();

        void close();

        void closeWithFade();

        void open(String page, String title);

        void setPageView(View v);

        void pushPage(View v);

        void bindSwitch(String uiKey, SwitchBox box, Runnable onSynced);

        void setHeroTimer(java.util.Timer t);

        void setHomeScroll(ScrollView sv);

        void openTerminal();

        void showApiDialog();
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
        return pageRoot(titleText, subText, out, fadeBack, topMarginDp, "bg");
    }

    protected LinearLayout pageRoot(String titleText, String subText, View[] out,
                                    final boolean fadeBack, int topMarginDp, String bgName) {
        return pageRoot(titleText, subText, out, fadeBack, topMarginDp, bgName, 0xC7);
    }

    protected LinearLayout pageRoot(String titleText, String subText, View[] out,
                                    final boolean fadeBack, int topMarginDp,
                                    String bgName, int veilAlpha) {
        FrameLayout pageRoot = new FrameLayout(ctx);

        String bgOverride = com.cathelperawa.owo.app.core.Skin.bg(bgName);
        Bitmap bgBmp = bgOverride != null
                ? android.graphics.BitmapFactory.decodeFile(bgOverride)
                : image(bgName);
        if (bgBmp != null) {
            ImageView bgv = new ImageView(ctx);
            bgv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            bgv.setImageBitmap(bgBmp);
            pageRoot.addView(bgv, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
            View veil = new View(ctx);
            int a = veilAlpha & 0xFF;
            veil.setBackgroundDrawable(new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                    new int[]{(a << 24) | 0xE4EFFF,
                              (a << 24) | 0xF4EBFF,
                              (Math.min(0xFF, a + 5) << 24) | 0xFFE9F1}));
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

                if (onBack()) return;
                if (fadeBack) host.closeWithFade(); else host.close();
            }
        });
        LinearLayout tb = new LinearLayout(ctx);
        tb.setOrientation(LinearLayout.VERTICAL);
        tb.addView(text(titleText, 24, 0xFF6F86A8, true, dp(1)));
        if (subText != null && subText.length() > 0) {
            tb.addView(text(subText, 11, 0xFFA9B7C9, false, 0));
        }
        topbar.addView(tb, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        root.addView(topbar, matchWrap(dp(topMarginDp)));

        pageRoot.addView(scroll, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        out[0] = pageRoot;
        out[1] = root;
        bindPage(pageRoot, this);
        return root;
    }

    protected void fadeIn(View root) {
        root.setAlpha(0f);
        root.animate().alpha(1f).setDuration(380).start();
    }

    protected void scrollIntoView(final View target, final View from) {
        target.post(new Runnable() {
            @Override public void run() {
                try {
                    ScrollView sv = findScroll(from);
                    if (sv == null || sv.getChildCount() == 0) return;
                    View content = sv.getChildAt(0);
                    int y = 0;
                    View v = target;
                    while (v != null && v != content && v != sv) {
                        y += v.getTop();
                        if (!(v.getParent() instanceof View)) break;
                        v = (View) v.getParent();
                    }
                    int bottom = y + target.getHeight();
                    int visibleH = sv.getHeight() - sv.getPaddingTop() - sv.getPaddingBottom();
                    if (visibleH <= 0) return;
                    int margin = dp(18);
                    int want = bottom + margin - visibleH;
                    if (bottom + margin > sv.getScrollY() + visibleH) {
                        sv.smoothScrollTo(0, Math.max(0, want));
                    }
                } catch (Throwable ignore) {
                }
            }
        });
    }

    protected ScrollView findScroll(View from) {
        View p = from;
        while (p != null) {
            if (p instanceof ScrollView) return (ScrollView) p;
            if (!(p.getParent() instanceof View)) return null;
            p = (View) p.getParent();
        }
        return null;
    }

    protected void showKeyboard(View v) {
        try {
            android.view.inputmethod.InputMethodManager imm =
                    (android.view.inputmethod.InputMethodManager)
                            ctx.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(v, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
            }
        } catch (Throwable ignore) {
        }
    }

    protected void hideKeyboard(View v) {
        try {
            android.view.inputmethod.InputMethodManager imm =
                    (android.view.inputmethod.InputMethodManager)
                            ctx.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
        } catch (Throwable ignore) {
        }
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
