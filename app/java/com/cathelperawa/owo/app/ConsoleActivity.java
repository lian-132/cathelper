package com.cathelperawa.owo.app;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.cathelperawa.owo.app.ui.Images;
import com.cathelperawa.owo.app.ui.SwitchBox;

import java.util.HashMap;
import java.util.Map;

public class ConsoleActivity extends Activity implements com.cathelperawa.owo.app.ui.Page.Host {

    private SharedPreferences sp;
    private java.util.Timer heroTimer;
    private boolean isHome = false;

    private String currentPage = null;

    private final java.util.List<String> pageStack = new java.util.ArrayList<String>();

    private ScrollView homeScroll;

    private static final int HOME_SCROLL_ID = 0x7f0f0001;
    public static final String PAGE_CHAT = "chat";

    public static final String MODULE_PKG = "com.cathelperawa.owo.app";
    public static final String EXTRA_PAGE = "cat_page";
    public static final String PAGE_MSG = "msg";
    public static final String PAGE_CAT = "cat_engine";

    public static final String PAGE_UNLOCK = "unlock";
    public static final String PAGE_SCOPE = "scope";
    public static final String PAGE_HOOK = "hook";

    public static final String PAGE_STATUS = "status";

    public static final String PAGE_CONFIG = "config";

    private static final int REQ_PICK_CONFIG = 0x0C1F;

    private com.cathelperawa.owo.app.ui.ConfigPage configPage;

    private static final String[][] HOOK_SWITCHES = {
            {"hook_capture", "长按提取", "长按消息出现「提取」按钮（原始协议数据）"},
            {"hook_autocap", "自动抓取消息", "收发消息自动记录到控制台"},
    };

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

    private final java.util.List<SwitchBinding> switchBindings =
            new java.util.ArrayList<SwitchBinding>();

    private static final class SwitchBinding {
        final String uiKey;
        final SwitchBox box;
        final Runnable onSynced;
        SwitchBinding(String uiKey, SwitchBox box, Runnable onSynced) {
            this.uiKey = uiKey; this.box = box; this.onSynced = onSynced;
        }
    }

    private boolean cfgGet(String key, boolean def) {
        return com.cathelperawa.owo.app.ui.UiConfig.get(this, key, def);
    }

    private void cfgSet(String key, boolean v) {
        com.cathelperawa.owo.app.ui.UiConfig.set(this, key, v);
    }

    private boolean swOn(String uiKey) {
        return com.cathelperawa.owo.app.ui.UiConfig.swOn(this, uiKey);
    }

    private void swSet(String uiKey, boolean v) {
        com.cathelperawa.owo.app.ui.UiConfig.swSet(this, uiKey, v);
    }

    private static final String[][] CHAT_ITEMS = {
            {"chat_revoke", "防撤回", "拦截撤回指令并提示"},
            {"chat_repeat", "长按复读", "长按消息出现「复读」按钮"},
    };

    private static final String[] KEYS = {"master", "revoke", "cat", "tts", "auto"};
    private static final String[] NAMES = {"总开关", "防撤回", "猫娘化", "语音转文字", "自动转换"};
    private static final String[] DESCS = {
            "猫娘化的总闸", "拦截撤回并提示", "输入框内容猫娘化", "ptt 转文字（待接线）", "自动触发（待接线）"
    };
    private static final String[] ICOS = {"", "", "", "", ""};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sp = getSharedPreferences("console", MODE_PRIVATE);

        applyEdgeToEdge();

        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(0xFFFFEAF1);

        getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(0xFFE4EFFF));

        loadImages();
        String page = getIntent().getStringExtra(EXTRA_PAGE);

        if (page == null || page.length() == 0 || "home".equals(page)) {
            pageStack.clear();
            applyPage(null);
        } else {
            pageStack.clear();
            pageStack.add(null);
            applyPage(page);
        }
        setTitleOf(titleOf(currentPage));
    }

    @Override
    protected void onResume() {
        super.onResume();

        syncSwitches();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, android.content.Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_PICK_CONFIG) {
            if (resultCode == RESULT_OK && data != null && data.getData() != null
                    && configPage != null) {
                configPage.onPicked(data.getData());
            }
            return;
        }
    }

    private void applyEdgeToEdge() {
        try {
            android.view.Window w = getWindow();
            if (android.os.Build.VERSION.SDK_INT >= 30) {

                w.setDecorFitsSystemWindows(false);
                android.view.WindowInsetsController c = w.getInsetsController();
                if (c != null) {

                    c.setSystemBarsAppearance(
                            android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                            android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS);
                }
            } else {

                w.setFlags(android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                        android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

                if (android.os.Build.VERSION.SDK_INT >= 23) {
                    View dv = w.getDecorView();
                    int flags = dv.getSystemUiVisibility()
                            | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                    dv.setSystemUiVisibility(flags);
                }
            }

            if (android.os.Build.VERSION.SDK_INT >= 28) {
                android.view.WindowManager.LayoutParams lp = w.getAttributes();
                lp.layoutInDisplayCutoutMode =
                        android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
                w.setAttributes(lp);
            }
        } catch (Throwable t) {
            com.cathelperawa.owo.app.core.HookLog.w("[UI] applyEdgeToEdge 失败: " + t);
        }
    }

    @Override
    protected void onDestroy() {
        cancelHeroTimer();
        super.onDestroy();
    }

    private void syncSwitches() {
        for (SwitchBinding b : switchBindings) {
            try {
                b.box.setChecked(swOn(b.uiKey));
            } catch (Throwable t) {
                com.cathelperawa.owo.app.core.HookLog.w("[UI] syncSwitches 失败 " + b.uiKey + ": " + t);
            }
        }

        for (SwitchBinding b : switchBindings) {
            if (b.onSynced == null) continue;
            try {
                b.onSynced.run();
            } catch (Throwable t) {
                com.cathelperawa.owo.app.core.HookLog.w("[UI] syncCallback 失败 " + b.uiKey + ": " + t);
            }
        }
    }

    private void bindSwitch(String uiKey, SwitchBox box) {
        bindSwitch(uiKey, box, null);
    }

    private View buildHome() {
        return new com.cathelperawa.owo.app.ui.HomePage(this).build();
    }

    private long lastNav = 0;

    private void openPage(final String page, final String title) {
        long now = System.currentTimeMillis();
        if (now - lastNav < 600) return;
        lastNav = now;
        applyPage(page);
        fadePageIn();
        setTitleOf(title);
    }

    private void closePage(boolean withFade) {
        if (!pageStack.isEmpty()) {
            String prev = pageStack.remove(pageStack.size() - 1);
            applyPage(prev);
            if (withFade) fadePageIn();
            setTitleOf(titleOf(prev));
        } else {
            finish();
            if (withFade) overridePendingTransition(
                    android.R.anim.fade_in, android.R.anim.fade_out);
        }
    }

    private void applyPage(String page) {

        cancelHeroTimer();
        switchBindings.clear();
        homeScroll = null;
        currentPage = page;
        isHome = (page == null) || "home".equals(page);

        if (PAGE_CHAT.equals(page)) {
            setContentView(buildFeaturePage(PAGE_CHAT, "聊天功能"));
        } else if (PAGE_UNLOCK.equals(page)) {
            setContentView(buildUnlockPage());
        } else if (PAGE_MSG.equals(page)) {
            setContentView(buildMsgPage());
        } else if (PAGE_CAT.equals(page)) {
            setContentView(buildCatPage());
        } else if (PAGE_SCOPE.equals(page)) {
            setContentView(buildScopePage());
        } else if (PAGE_CONFIG.equals(page)) {
            configPage = new com.cathelperawa.owo.app.ui.ConfigPage(this);
            setContentView(configPage.build());
        } else if (PAGE_STATUS.equals(page)) {
            setContentView(new com.cathelperawa.owo.app.ui.StatusPage(this).build());
        } else if (PAGE_HOOK.equals(page)) {
            setContentView(buildHookPage());
        } else {
            setContentView(buildHome());
        }
    }

    private static String titleOf(String page) {
        if (PAGE_CHAT.equals(page)) return "聊天功能";
        if (PAGE_MSG.equals(page)) return "抓取记录";
        if (PAGE_CAT.equals(page)) return "猫娘化引擎";
        if (PAGE_SCOPE.equals(page)) return "作用范围";
        if (PAGE_HOOK.equals(page)) return "Hook 功能";
        if (PAGE_STATUS.equals(page)) return "功能状态";
        if (PAGE_CONFIG.equals(page)) return "配置备份";
        return "猫娘助手";
    }

    private void setTitleOf(String t) {
        try {
            setTitle(t);
        } catch (Throwable ignore) {
        }
    }

    private void fadePageIn() {
        View rootV = findViewById(android.R.id.content);
        if (rootV == null) return;
        rootV.setAlpha(0f);
        rootV.animate().alpha(1f).setDuration(240).start();
    }

    @Override
    public void onBackPressed() {
        if (!pageStack.isEmpty()) {
            closePage(false);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public Context context() { return this; }

    @Override
    public void close() { closePage(false); }

    @Override
    public void closeWithFade() { closePage(true); }

    @Override
    public void bindSwitch(String uiKey, com.cathelperawa.owo.app.ui.SwitchBox box, Runnable onSynced) {
        if (box == null) return;
        switchBindings.add(new SwitchBinding(uiKey, box, onSynced));

        try {
            box.setChecked(swOn(uiKey));
        } catch (Throwable ignore) {
        }

    }

    @Override
    public void setHeroTimer(java.util.Timer t) {
        cancelHeroTimer();
        heroTimer = t;
    }

    @Override
    public void setHomeScroll(ScrollView sv) {
        this.homeScroll = sv;
    }

    private void cancelHeroTimer() {
        if (heroTimer != null) {
            try { heroTimer.cancel(); } catch (Throwable ignore) {}
            heroTimer = null;
        }
    }

    @Override
    public void openTerminal() {
        try {
            android.content.Intent it = new android.content.Intent();
            it.setClassName("com.cathelperawa.owo.app",
                    "com.cathelperawa.owo.app.terminal.TerminalActivity");
            it.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(it);
        } catch (Throwable t) {
            toast("终端没打开喵…（细节看日志）");
        }
    }

    @Override
    public void open(String page, String title) {

        pageStack.add(currentPage);
        openPage(page, title);
    }

    @Override
    public void setPageView(View v) { setContentView(v); }

    private void press(final View v) {
        com.cathelperawa.owo.app.ui.UiKit.press(v);
    }

    private View buildHookPage() {
        return new com.cathelperawa.owo.app.ui.HookPage(this).build();
    }

    private View buildCatPage() {
        return new com.cathelperawa.owo.app.ui.CatPage(this).build();
    }

    private View buildScopePage() {
        return new com.cathelperawa.owo.app.ui.ScopePage(this).build();
    }

    private View buildMsgPage() {
        return new com.cathelperawa.owo.app.ui.MsgPage(this).build();
    }

    private View buildFeaturePage(final String page, String titleText) {
        return new com.cathelperawa.owo.app.ui.FeaturePage(this, page, titleText).build();
    }

    private View buildUnlockPage() {
        return new com.cathelperawa.owo.app.ui.UnlockPage(this).build();
    }

    private void loadImages() {
        com.cathelperawa.owo.app.ui.Images.load(this);
    }

    private FrameLayout whiteFrame(int radius, int color) {
        return com.cathelperawa.owo.app.ui.UiKit.whiteFrame(this, radius, color);
    }

    private LinearLayout whiteCard(int radius, int color) {
        return com.cathelperawa.owo.app.ui.UiKit.whiteCard(this, radius, color);
    }

    private TextView pill(String s, int color) {
        return com.cathelperawa.owo.app.ui.UiKit.pill(this, s, color);
    }

    private TextView text(String s, float size, int color, boolean bold, int bottomPad) {
        return com.cathelperawa.owo.app.ui.UiKit.text(this, s, size, color, bold, bottomPad);
    }

    private LinearLayout.LayoutParams matchWrap(int bottomMargin) {
        return com.cathelperawa.owo.app.ui.UiKit.matchWrap(this, bottomMargin);
    }

    private LinearLayout.LayoutParams wrapWrap() {
        return com.cathelperawa.owo.app.ui.UiKit.wrapWrap();
    }

    private void toast(String s) {
        catToast(s);
    }

    private void catToast(String msg) {
        com.cathelperawa.owo.app.ui.UiKit.catToast(this, msg);
    }

    private static boolean contains(String[] arr, String s) {
        for (String a : arr) if (a.equals(s)) return true;
        return false;
    }

    private static String simple(Class<?> c) {
        String n = c.getSimpleName();
        return n.isEmpty() ? c.getName() : n;
    }

    private int dp(float v) {
        return com.cathelperawa.owo.app.ui.UiKit.dp(this, v);
    }
}
