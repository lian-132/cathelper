package com.cathelperawa.owo.app.bili;

import android.app.Activity;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.cathelperawa.owo.app.AppConfig;
import com.cathelperawa.owo.app.ui.CatIcon;

import java.util.ArrayList;
import java.util.List;

public final class BiliActivity extends Activity {

    private static final int MAX_STACK = 4;

    private FrameLayout root;
    private FrameLayout content;
    private LinearLayout tabBar;
    private LinearLayout[] tabItems = new LinearLayout[3];
    private CatIcon[] tabIcons = new CatIcon[3];
    private TextView[] tabLabels = new TextView[3];

    private final List<BiliPage> stack = new ArrayList<BiliPage>();
    private final BiliPage[] roots = new BiliPage[3];
    private int currentTab = 0;

    private int insetTop, insetBottom;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        if (Build.VERSION.SDK_INT >= 21) {
            getWindow().setStatusBarColor(Color.TRANSPARENT);
            getWindow().setNavigationBarColor(Color.TRANSPARENT);
        }
        try {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        } catch (Throwable ignore) {
        }

        root = new FrameLayout(this);
        BiliUi.applyBackground(this, root);

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);

        content = new FrameLayout(this);
        col.addView(content, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        tabBar = buildTabBar();
        col.addView(tabBar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        root.addView(BiliUi.scrim(this), new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        root.addView(col, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        setContentView(root);

        applyInsets();
        switchTab(0);
    }

    private void applyInsets() {
        if (Build.VERSION.SDK_INT < 20) return;
        root.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            @Override
            public WindowInsets onApplyWindowInsets(View v, WindowInsets insets) {
                int top = insets.getSystemWindowInsetTop();
                int bottom = insets.getSystemWindowInsetBottom();
                if (top != insetTop || bottom != insetBottom) {
                    insetTop = top;
                    insetBottom = bottom;
                    if (tabBar.getVisibility() == View.VISIBLE) {
                        content.setPadding(0, top, 0, 0);
                    }
                    tabBar.setPadding(0, 0, 0, bottom);
                }
                return insets;
            }
        });
    }

    public int insetTop() { return insetTop; }
    public int insetBottom() { return insetBottom; }

    private LinearLayout buildTabBar() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER);
        bar.setBackgroundColor(Color.argb(190, 11, 15, 22));

        int[] glyphs = new int[]{CatIcon.HOME, CatIcon.DYNAMIC, CatIcon.USER};
        String[] labels = new String[]{"首页", "动态", "我的"};

        LinearLayout.LayoutParams itemLp = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        for (int i = 0; i < 3; i++) {
            final int idx = i;
            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setGravity(Gravity.CENTER);
            item.setPadding(BiliUi.dp(this, 14), BiliUi.dp(this, 8),
                    BiliUi.dp(this, 14), BiliUi.dp(this, 8));

            CatIcon ic = new CatIcon(this, glyphs[i], 21, BiliUi.SUB);
            TextView tv = BiliUi.text(this, labels[i], 10.5f, BiliUi.SUB, false);
            LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            tlp.topMargin = BiliUi.dp(this, 3);
            tv.setLayoutParams(tlp);

            item.addView(ic);
            item.addView(tv);
            item.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { switchTab(idx); }
            });

            LinearLayout wrap = new LinearLayout(this);
            wrap.setGravity(Gravity.CENTER);
            wrap.setPadding(0, BiliUi.dp(this, 4), 0, BiliUi.dp(this, 4));
            wrap.addView(item, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            bar.addView(wrap, itemLp);

            tabItems[i] = item;
            tabIcons[i] = ic;
            tabLabels[i] = tv;
        }
        paintTabs();
        return bar;
    }

    private void paintTabs() {
        for (int i = 0; i < 3; i++) {
            boolean on = (i == currentTab);
            if (tabIcons[i] != null) tabIcons[i].setIconColor(on ? BiliUi.MAIN : BiliUi.SUB);
            if (tabLabels[i] != null) {
                tabLabels[i].setTextColor(on ? BiliUi.MAIN : BiliUi.SUB);
            }
            if (tabItems[i] != null) {
                if (on) {
                    android.graphics.drawable.GradientDrawable g =
                            new android.graphics.drawable.GradientDrawable();
                    g.setColor(0x2E6EA6FF);
                    g.setCornerRadius(BiliUi.dp(this, 16));
                    tabItems[i].setBackground(g);
                } else {
                    tabItems[i].setBackground(null);
                }
            }
        }
    }

    public void switchTab(int tab) {
        if (tab < 0 || tab > 2) return;
        currentTab = tab;
        if (roots[tab] == null) {
            roots[tab] = createRoot(tab);
        }
        stack.clear();
        stack.add(roots[tab]);
        showTop();
        paintTabs();
        setTabBarVisible(true);
    }

    private BiliPage createRoot(int tab) {
        if (tab == 0) return new BiliFeedPage(this);
        if (tab == 1) return new BiliDynamicPage(this);
        return new BiliMinePage(this);
    }

    public void push(BiliPage page) {
        if (page == null) return;
        BiliPage cur = current();
        if (cur != null) cur.onHide();
        stack.add(page);
        trimStack();
        showTop();
        setTabBarVisible(false);
    }

    public boolean pop() {
        if (stack.size() <= 1) return false;
        BiliPage top = stack.remove(stack.size() - 1);
        try { top.onDestroy(); } catch (Throwable ignore) { }
        showTop();
        setTabBarVisible(stack.size() <= 1);
        return true;
    }

    private void trimStack() {
        if (stack.size() <= MAX_STACK) return;

        for (int i = stack.size() - 3; i >= 1; i--) {
            BiliPage p = stack.remove(i);
            try { p.onDestroy(); } catch (Throwable ignore) { }
        }
    }

    private BiliPage current() {
        return stack.isEmpty() ? null : stack.get(stack.size() - 1);
    }

    private void showTop() {
        content.removeAllViews();
        BiliPage top = current();
        if (top == null) return;
        View v = top.view();
        if (v == null) return;
        ViewGroup parent = (ViewGroup) v.getParent();
        if (parent != null) parent.removeView(v);
        content.addView(v, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        top.onShow();
    }

    public void setTabBarVisible(boolean visible) {
        if (tabBar != null) tabBar.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    public int topbarHeight() {
        return BiliUi.dp(this, 46);
    }

    @Override
    public void onBackPressed() {
        BiliPage top = current();
        if (top != null) {
            try {
                if (top.onBack()) return;
            } catch (Throwable ignore) {
            }
        }
        if (pop()) return;
        if (currentTab != 0) {
            switchTab(0);
            return;
        }
        finish();
    }

    @Override
    public void onConfigurationChanged(android.content.res.Configuration cfg) {
        super.onConfigurationChanged(cfg);

        try {
            if (root != null) root.requestApplyInsets();
        } catch (Throwable ignore) {
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (root != null) BiliUi.applyBackground(this, root);
    }

    @Override
    protected void onDestroy() {
        for (BiliPage p : stack) {
            try { p.onDestroy(); } catch (Throwable ignore) { }
        }
        stack.clear();
        for (int i = 0; i < roots.length; i++) roots[i] = null;
        BiliImage.clearMemory();
        super.onDestroy();
    }

    public void setImmersive(boolean on) {
        if (Build.VERSION.SDK_INT < 19) return;

        if (content != null) content.setPadding(0, on ? 0 : insetTop, 0, 0);
        try {
            View dv = getWindow().getDecorView();
            if (on) {
                dv.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
            } else {
                dv.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
            }
        } catch (Throwable ignore) {
        }
    }

    public void refreshBackground() {
        if (root != null) BiliUi.applyBackground(this, root);
        if (tabBar != null) tabBar.setBackgroundColor(Color.argb(190, 11, 15, 22));
        paintTabs();
    }

    public android.content.SharedPreferences prefs() {
        return getSharedPreferences(AppConfig.PREF, MODE_PRIVATE);
    }

    public void openLogin() {
        try {
            android.content.Intent it = new android.content.Intent(this,
                    com.cathelperawa.owo.app.cookie.CookieLoginActivity.class);
            it.putExtra(com.cathelperawa.owo.app.cookie.CookieLoginActivity.EXTRA_PLATFORM, "bilibili");
            startActivity(it);
        } catch (Throwable t) {
            android.widget.Toast.makeText(this, "打开登录页失败：" + t, android.widget.Toast.LENGTH_SHORT).show();
        }
    }
}
