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

    @Override
    public void setContentView(android.view.View v) {
        super.setContentView(v);
        try {
            com.cathelperawa.owo.app.ui.Theme.apply(v);
        } catch (Throwable ignore) {
        }
    }

    private SharedPreferences sp;
    private java.util.Timer heroTimer;
    private boolean isHome = false;

    private String currentPage = null;

    private final java.util.List<String> pageStack = new java.util.ArrayList<String>();

    private ScrollView homeScroll;

    private static final int HOME_SCROLL_ID = 0x7f0f0001;
    public static final String PAGE_CHAT = "chat";

    public static final String MODULE_PKG = "com.cathelperawa.owo.app";
    public static final String PAGE_REDPACKET = "redpacket";
    public static final String EXTRA_PAGE = "cat_page";
    public static final String PAGE_MSG = "msg";
    public static final String PAGE_CAT = "cat_engine";

    public static final String PAGE_GROUP = "group";

    public static final String PAGE_UNLOCK = "unlock";

    public static final String PAGE_PARSE = "parse";

    public static final String PAGE_FILE = "file";

    public static final String PAGE_FAKE = "fake";
    public static final String PAGE_SCOPE = "scope";
    public static final String PAGE_HOOK = "hook";

    public static final String PAGE_RP_AUTO = "rp_auto_set";
    public static final String PAGE_RP_MANUAL = "rp_manual_set";

    public static final String PAGE_CLOCKIN = "clockin_set";

    public static final String PAGE_MSGTIME = "msgtime_set";

    public static final String PAGE_SEND = "send";

    public static final String PAGE_STATUS = "status";

    public static final String PAGE_ANTI = "anti_detect_set";

    public static final String PAGE_FULLCHECK = "fullcheck";

    public static final String PAGE_CONFIG = "config";

    public static final String PAGE_SCRIPT = "script";

    public static final String PAGE_AUDIT = "audit";

    public static final int REQ_IMPORT_SCRIPT = 1001;

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
    private static final String[][] REDPACKET_ITEMS = {
            {"rp_manual", "手动打开红包", "为长按菜单添加打开红包按钮"},
            {"rp_auto", "自动领取红包", "点击可设置参数，回复语支持脚本图文消息"},
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

        try {
            String from = getIntent() == null ? null : getIntent().getStringExtra("from_host");
            if (from != null && from.length() > 0) {
                com.cathelperawa.owo.app.core.UIScope.set(from);
            }
        } catch (Throwable ignore) {
        }
        sp = getSharedPreferences("console", MODE_PRIVATE);

        try {
            com.cathelperawa.owo.app.script.PatchPoint.emitAsync("host.panel.open", "console", "console");
        } catch (Throwable ignore) {
        }

        applyEdgeToEdge();

        getWindow().setStatusBarColor(Color.TRANSPARENT);

        getWindow().setNavigationBarColor(Color.TRANSPARENT);
        try {
            if (android.os.Build.VERSION.SDK_INT >= 29) {
                getWindow().setNavigationBarContrastEnforced(false);
            }
            if (android.os.Build.VERSION.SDK_INT >= 27) {
                getWindow().setNavigationBarDividerColor(Color.TRANSPARENT);
            }
        } catch (Throwable ignore) {
        }

        getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(0xFFE4EFFF));

        loadImages();
        installImeInsets();

        try {
            String panelKey = getIntent().getStringExtra("script_panel");
            if (panelKey != null && panelKey.length() > 0) {
                com.cathelperawa.owo.app.script.ScriptPanelRegistry.Panel pn =
                        com.cathelperawa.owo.app.script.ScriptPanelRegistry.find(panelKey);
                if (pn != null) {
                    pageStack.clear();
                    pageStack.add("script");
                    currentPage = "script";
                    setContentView(new com.cathelperawa.owo.app.script.ScriptPanelPage(this, pn).build());
                    setTitleOf(pn.title);
                    return;
                }
            }
        } catch (Throwable ignore) {
        }

        String page = getIntent().getStringExtra(EXTRA_PAGE);

        if (page == null || page.length() == 0 || "home".equals(page)) {
            pageStack.clear();
            applyPage(null);
        } else {

            pageStack.clear();
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
        if (requestCode == REQ_IMPORT_SCRIPT) {
            if (resultCode != RESULT_OK || data == null) return;
            android.net.Uri uri = data.getData();
            if (uri == null) return;
            final String err = com.cathelperawa.owo.app.script.ModPack.importFromUri(this, uri);
            String msg = err == null ? "脚本已导入，回脚本页就能看到" : ("导入失败：" + err);
            try {
                com.cathelperawa.owo.app.bridge.CatToast.show(msg);
            } catch (Throwable ignore) {
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

    private View currentContent;

    private int contentBasePadBottom = -1;

    private int imePad = -1;

    private void installImeInsets() {
        try {
            final View decor = getWindow().getDecorView();
            if (android.os.Build.VERSION.SDK_INT >= 30) {
                decor.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
                    @Override
                    public android.view.WindowInsets onApplyWindowInsets(
                            View v, android.view.WindowInsets insets) {
                        int ime = insets.getInsets(android.view.WindowInsets.Type.ime()).bottom;
                        applyImePad(ime);
                        return insets;
                    }
                });
            } else {
                decor.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
                    @Override
                    public android.view.WindowInsets onApplyWindowInsets(
                            View v, android.view.WindowInsets insets) {

                        int ime = Math.max(0, insets.getSystemWindowInsetBottom()
                                - getNavigationBarHeight());
                        applyImePad(ime);
                        return insets;
                    }
                });
            }
            decor.requestApplyInsets();
        } catch (Throwable t) {
            com.cathelperawa.owo.app.core.HookLog.w("[UI] installImeInsets 失败: " + t);
        }
    }

    private View contentView() {
        try {
            android.view.ViewGroup root = findViewById(android.R.id.content);
            if (root != null && root.getChildCount() > 0) return root.getChildAt(0);
        } catch (Throwable ignore) {
        }
        return null;
    }

    private void applyImePad(int ime) {
        View v = contentView();
        if (v == null) return;
        if (v != currentContent) {

            currentContent = v;
            contentBasePadBottom = v.getPaddingBottom();
            imePad = -1;
        }
        if (imePad == ime) return;
        imePad = ime;
        int want = contentBasePadBottom + Math.max(0, ime);
        if (v.getPaddingBottom() != want) {
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), want);
        }
    }

    private int getNavigationBarHeight() {
        try {
            android.util.DisplayMetrics dm = getResources().getDisplayMetrics();
            int id = getResources().getIdentifier("navigation_bar_height", "dimen", "android");
            return id > 0 ? getResources().getDimensionPixelSize(id) : (int) (48 * dm.density);
        } catch (Throwable t) {
            return 0;
        }
    }

    @Override
    public void onContentChanged() {
        super.onContentChanged();

        currentContent = null;
        contentBasePadBottom = -1;
        imePad = -1;
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

            boolean samePage = (prev == null) ? (subFromPage == null)
                    : prev.equals(subFromPage);
            if (samePage && subFromRoot != null) {
                View back = subFromRoot;
                subFromRoot = null;
                subFromPage = null;
                diag("closePage 复用 " + (prev == null ? "@home" : prev));
                rememberScroll();
                currentPage = prev;
                currentRootView = back;
                isHome = (prev == null) || "home".equals(prev);
                setContentView(back);
                restoreScroll(prev);
            } else {
                diag("closePage 重建 " + (prev == null ? "@home" : prev));
                applyPage(prev);
            }
            if (withFade) fadePageIn();
            setTitleOf(titleOf(prev));
        } else {
            finish();
            if (withFade) overridePendingTransition(
                    android.R.anim.fade_in, android.R.anim.fade_out);
        }
    }

    private android.view.View wrapScriptPage(final String pageKey, String owner,
                                             android.view.View inner) {
        float den = getResources().getDisplayMetrics().density;
        android.widget.LinearLayout root = new android.widget.LinearLayout(this);
        root.setOrientation(android.widget.LinearLayout.VERTICAL);

        final android.widget.TextView bar = new android.widget.TextView(this);
        String who = (owner == null || owner.length() == 0) ? "某个脚本" : owner;
        bar.setText("本页由脚本 " + who + " 接管 · 按住 5 秒恢复官方页面");
        bar.setTextSize(11f);
        bar.setTextColor(0xFF8D6E00);
        bar.setBackgroundColor(0x1AFFC107);
        int px = (int) (8 * den);
        bar.setPadding(px, px, px, px);
        bar.setGravity(android.view.Gravity.CENTER);
        bar.setOnTouchListener(new android.view.View.OnTouchListener() {
            private long downAt;
            @Override public boolean onTouch(android.view.View v, android.view.MotionEvent e) {
                int act = e.getAction();
                if (act == android.view.MotionEvent.ACTION_DOWN) {
                    downAt = System.currentTimeMillis();
                    bar.setText("继续按住…松手取消");
                    return true;
                }
                if (act == android.view.MotionEvent.ACTION_UP
                        || act == android.view.MotionEvent.ACTION_CANCEL) {
                    long held = System.currentTimeMillis() - downAt;
                    if (act != android.view.MotionEvent.ACTION_CANCEL && held >= 5000) {
                        com.cathelperawa.owo.app.ui.ScriptScreen.removePage(pageKey);
                        android.widget.Toast.makeText(ConsoleActivity.this,
                                "已收回该页面，官方实现恢复",
                                android.widget.Toast.LENGTH_SHORT).show();
                        applyPage(pageKey);
                    } else {
                        String who2 = com.cathelperawa.owo.app.ui.ScriptScreen.owner(pageKey);
                        bar.setText("本页由脚本 " + (who2 == null ? "某个脚本" : who2)
                                + " 接管 · 按住 5 秒恢复官方页面");
                    }
                    return true;
                }
                return true;
            }
        });
        root.addView(bar, new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT));
        root.addView(inner, new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        return root;
    }

    private final java.util.HashMap<String, Integer> scrollMemo =
            new java.util.HashMap<String, Integer>();

    private View currentRootView;

    private View subFromRoot;
    private String subFromPage;

    private ScrollView findScrollView(View v) {
        if (v == null) return null;
        if (v instanceof ScrollView) return (ScrollView) v;
        if (v instanceof android.view.ViewGroup) {
            android.view.ViewGroup g = (android.view.ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) {
                ScrollView s = findScrollView(g.getChildAt(i));
                if (s != null) return s;
            }
        }
        return null;
    }

    private void diag(String msg) {
        try {
            java.io.File f = new java.io.File(getFilesDir(), "scroll_diag.txt");
            if (f.length() > 64 * 1024) f.delete();
            java.io.FileWriter w = new java.io.FileWriter(f, true);
            w.write(new java.text.SimpleDateFormat("HH:mm:ss.SSS")
                    .format(new java.util.Date()) + " " + msg + "\n");
            w.close();
        } catch (Throwable ignore) {
        }
    }

    private void rememberScroll() {
        try {
            if ("__sub".equals(currentPage)) { diag("记：正在二级页，跳过"); return; }
            final String key = currentPage == null ? "@home" : currentPage;
            View content = findViewById(android.R.id.content);
            ScrollView sv = findScrollView(content);
            if (sv != null) {
                scrollMemo.put(key, sv.getScrollY());
                int ch = sv.getChildCount() > 0 ? sv.getChildAt(0).getHeight() : -1;
                diag("记 " + key + " = " + sv.getScrollY()
                        + " sv=" + sv.hashCode() + " h=" + sv.getHeight() + " 内容h=" + ch
                        + " 根=" + (content == null ? "?" : content.getClass().getSimpleName()));
            } else {
                diag("记 " + key + " 失败：找不到 ScrollView 根="
                        + (content == null ? "?" : content.getClass().getSimpleName()));
            }
        } catch (Throwable t) {
            diag("记异常 " + t);
        }
    }

    private void restoreScroll(final String page) {
        try {
            final String key = page == null ? "@home" : page;
            final Integer yBox = scrollMemo.get(key);
            if (yBox == null || yBox <= 0) {
                diag("恢复 " + key + " 没有记忆，跳过");
                return;
            }
            final int y = yBox;
            final int[] tries = new int[]{0};
            final Runnable apply = new Runnable() {
                @Override public void run() {
                    try {
                        ScrollView sv = findScrollView(findViewById(android.R.id.content));
                        if (sv == null) return;
                        sv.scrollTo(0, y);
                        int now = sv.getScrollY();
                        diag("恢复 " + (page == null ? "@home" : page) + " 目标 " + y + " 实际 " + now
                                + " 第 " + tries[0] + " 次 sv=" + sv.hashCode());

                        if (Math.abs(now - y) > 2 && tries[0] < 8) {
                            tries[0]++;
                            sv.postDelayed(this, 50);
                        }
                    } catch (Throwable ignore) {
                    }
                }
            };
            findViewById(android.R.id.content).post(apply);
        } catch (Throwable ignore) {
        }
    }

    private void applyPage(String page) {
        diag("applyPage " + page + " ← " + currentPage);
        rememberScroll();
        applyPageInner(page);
        cacheRootView();
        restoreScroll(page);
    }

    private void cacheRootView() {
        try {
            View content = findViewById(android.R.id.content);
            if (content instanceof android.view.ViewGroup
                    && ((android.view.ViewGroup) content).getChildCount() > 0) {
                currentRootView = ((android.view.ViewGroup) content).getChildAt(0);
            }
        } catch (Throwable ignore) {
        }
    }

    private void applyPageInner(String page) {

        cancelHeroTimer();
        switchBindings.clear();
        homeScroll = null;
        currentPage = page;
        isHome = (page == null) || "home".equals(page);
        updateSystemBars(page);

        try {
            if (com.cathelperawa.owo.app.ui.ScriptScreen.hasPage(page)) {
                android.view.View pv = com.cathelperawa.owo.app.ui.ScriptScreen
                        .buildDeclared(this, page);
                if (pv != null) {
                    setContentView(wrapScriptPage(page, "脚本页面", pv));
                    return;
                }
            }
        } catch (Throwable ignore) {
        }

        try {
            final String pkey = page == null ? "home" : page;
            if (com.cathelperawa.owo.app.ui.ScriptScreen.has(pkey)) {
                android.view.View modView = com.cathelperawa.owo.app.ui.ScriptScreen.build(this, pkey);
                if (modView != null) {
                    setContentView(wrapScriptPage(pkey,
                            com.cathelperawa.owo.app.ui.ScriptScreen.owner(pkey), modView));
                    return;
                }
            }
        } catch (Throwable ignore) {
        }

        if (PAGE_AUDIT.equals(page)) {
            setContentView(new com.cathelperawa.owo.app.ui.AuditPage(this).build());
            return;
        }
        if (PAGE_CHAT.equals(page)) {
            setContentView(buildFeaturePage(PAGE_CHAT, "聊天功能"));
        } else if (PAGE_REDPACKET.equals(page)) {
            setContentView(buildFeaturePage(PAGE_REDPACKET, "红包功能"));
        } else if (PAGE_GROUP.equals(page)) {
            setContentView(new com.cathelperawa.owo.app.ui.GroupPage(this).build());
        } else if (PAGE_UNLOCK.equals(page)) {
            setContentView(buildUnlockPage());
        } else if (PAGE_PARSE.equals(page)) {
            setContentView(new com.cathelperawa.owo.app.ui.ParsePage(this).build());
        } else if (PAGE_FILE.equals(page)) {
            setContentView(new com.cathelperawa.owo.app.ui.FilePage(this).build());
        } else if (PAGE_FAKE.equals(page)) {
            setContentView(new com.cathelperawa.owo.app.ui.FakePage(this).build());
        } else if (PAGE_MSG.equals(page)) {
            setContentView(buildMsgPage());
        } else if (PAGE_CAT.equals(page)) {
            setContentView(buildCatPage());
        } else if (PAGE_SCOPE.equals(page)) {
            setContentView(buildScopePage());
        } else if (PAGE_RP_AUTO.equals(page)) {
            setContentView(buildRedPacketDetail(true));
        } else if (PAGE_RP_MANUAL.equals(page)) {
            setContentView(buildRedPacketDetail(false));
        } else if (PAGE_CLOCKIN.equals(page)) {
            setContentView(new com.cathelperawa.owo.app.ui.ClockInPage(this).build());
        } else if (PAGE_MSGTIME.equals(page)) {
            setContentView(new com.cathelperawa.owo.app.ui.MsgTimePage(this).build());
        } else if (PAGE_SEND.equals(page)) {
            setContentView(new com.cathelperawa.owo.app.ui.SendPage(this).build());
        } else if (PAGE_CONFIG.equals(page)) {
            configPage = new com.cathelperawa.owo.app.ui.ConfigPage(this);
            setContentView(configPage.build());
        } else if (PAGE_FULLCHECK.equals(page)) {
            setContentView(new com.cathelperawa.owo.app.ui.CheckPage(this).build());
        } else if (PAGE_STATUS.equals(page)) {
            setContentView(new com.cathelperawa.owo.app.ui.StatusPage(this).build());
        } else if (PAGE_ANTI.equals(page)) {
            setContentView(new com.cathelperawa.owo.app.ui.AntiDetectPage(this).build());
        } else if (PAGE_HOOK.equals(page)) {
            setContentView(buildHookPage());
        } else if (PAGE_SCRIPT.equals(page)) {

            setContentView(new com.cathelperawa.owo.app.script.ScriptPage(this).build());
        } else {
            setContentView(buildHome());
        }
    }

    private void updateSystemBars(String page) {
        try {
            android.view.Window w = getWindow();
            w.setNavigationBarColor(Color.TRANSPARENT);

            if (android.os.Build.VERSION.SDK_INT >= 29) {
                w.setNavigationBarContrastEnforced(false);
            }
            if (android.os.Build.VERSION.SDK_INT >= 27) {
                w.setNavigationBarDividerColor(Color.TRANSPARENT);
            }
        } catch (Throwable ignore) {
        }
    }

    private static String titleOf(String page) {
        if (PAGE_CHAT.equals(page)) return "聊天功能";
        if (PAGE_GROUP.equals(page)) return "群管理";
        if (PAGE_REDPACKET.equals(page)) return "红包功能";
        if (PAGE_MSG.equals(page)) return "抓取记录";
        if (PAGE_CAT.equals(page)) return "猫娘化引擎";
        if (PAGE_SCOPE.equals(page)) return "作用范围";
        if (PAGE_HOOK.equals(page)) return "Hook 功能";
        if (PAGE_RP_AUTO.equals(page)) return "自动领取红包";
        if (PAGE_RP_MANUAL.equals(page)) return "手动打开红包";
        if (PAGE_CLOCKIN.equals(page)) return "群打卡";
        if (PAGE_MSGTIME.equals(page)) return "显示消息时间";
        if (PAGE_SEND.equals(page)) return "发送与协议";
        if (PAGE_PARSE.equals(page)) return "解析功能";
        if (PAGE_STATUS.equals(page)) return "功能状态";
        if (PAGE_FULLCHECK.equals(page)) return "全面体检";
        if (PAGE_CONFIG.equals(page)) return "配置备份";
        if (PAGE_SCRIPT.equals(page)) return "脚本";
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

        try {
            if (com.cathelperawa.owo.app.script.ScriptConsolePanel.closeCurrent()) return;
        } catch (Throwable ignore) {
        }
        try {
            if (com.cathelperawa.owo.app.ui.TerminalPanel.closeCurrent()) return;
        } catch (Throwable ignore) {
        }

        try {
            if (com.cathelperawa.owo.app.script.ScriptPage.backToMain()) return;
        } catch (Throwable ignore) {
        }
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
            com.cathelperawa.owo.app.ui.TerminalPanel.show(this);
        } catch (Throwable t) {
            toast("终端没打开喵…（细节看日志）");
        }
    }

    @Override
    public void showApiDialog() {
        showApiDialogImpl();
    }

    @Override
    public void open(String page, String title) {

        rememberScroll();
        subFromRoot = currentRootView;
        subFromPage = currentPage;
        pageStack.add(currentPage);
        openPage(page, title);
    }

    @Override
    public void pushPage(View v) {

        diag("pushPage ← " + currentPage + " root=" + (currentRootView == null ? "null" : "ok"));
        rememberScroll();
        subFromRoot = currentRootView;
        subFromPage = currentPage;
        pageStack.add(currentPage);
        currentPage = "__sub";
        setContentView(v);
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

    private static final String[][] RP_AUTO_ITEMS = {
            {"rp_average", "平均金额阈值", "低于该值不抢 · 单位「分」，0 = 不限"},
            {"rp_keywords", "屏蔽关键词", "标题含这些词就不抢 · 英文逗号分隔"},
            {"rp_replys",   "抢到后回复", "随机挑一条发出 · 英文逗号分隔"},
            {"rp_delay",    "随机延迟(ms)", "0 ~ 该值内随机等待，削弱机器特征"},
            {"rp_servlet",  "Servlet 类名", "留空=自动探测；换候选不用重编译"},
    };
    private static final String[][] RP_MANUAL_ITEMS = {
            {"rp_replys", "抢到后回复", "长按「开红包」成功后的回复语 · 英文逗号分隔"},
    };

    private View buildRedPacketDetail(final boolean auto) {
        return new com.cathelperawa.owo.app.ui.RedPacketDetailPage(this, auto).build();
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

    private String buildApiDoc() {
        StringBuilder sb = new StringBuilder();
        try {
            Class<?> api = Class.forName("com.cathelperawa.owo.app.api.CatApi");
            java.lang.reflect.Method[] ms = api.getDeclaredMethods();
            java.util.Arrays.sort(ms, new java.util.Comparator<java.lang.reflect.Method>() {
                @Override public int compare(java.lang.reflect.Method a, java.lang.reflect.Method b) {
                    return a.getName().compareTo(b.getName());
                }
            });

            String[][] groups = {
                {"卡片签名", "signCard sendSignedCard signLinkCard sendLinkCard"},
                {"音乐卡", "signMusicCard sendMusicCard sendMusicCardBySongId parseSendMusicCard"},
                {"发送", "sendText sendCard sendPic sendPtt sendVideo sendFile sendFace sendReply sendMixed sendEmojiLike sendElements"},
                {"幽灵会话", "ghostSend ghostLog ghostUid"},
                {"语音", "tts2Silk voiceUpload voiceUploadOk voiceSend"},
                {"昵称", "buddyName buddyNameByUid troopMemberName troopName allTroopNames allBuddies"},
                {"灰字", "grayTip grayTipText grayTipTextRef grayTipJson grayTipContact grayTipItems tipText tipUser tipRef tipCompose"},
                {"消息操作", "recall recallMsg deleteMsg generateMsgId getMsgsByMsgId getSingleMsg forward"},
                {"提取", "extract lastExtractResult lastExtractRecord textOf cardOf"},
                {"复读内核", "repeat collectElements sendableElements grabRawElements filterSendable plainText grab elementKind walletOf"},
                {"消息拉取", "pullMsg pullC2C pullGroup fetchViaKernel"},
                {"抠码", "grabCode buildMultiArk xmlUnescape"},
                {"报文构造", "pbMsgDirect pbMsgRef pbMsgMulti pbMsgUpload"},
                {"ovo 通道", "ovoSend ovoBuild ovoSendBuffer"},
                {"反射工具", "field setField findField call str"},
                {"Hex", "hexEncode hexDecode isHex"},
                {"Hook 增强", "hookReplace hookReturn unhook findMethodByParams findMethodsByParams findClass hookBefore hookAfter hookAll"},
                {"捕获作用域", "notePeer scopeAll setScopeAll isSelected toggleSelected scopeAllows toggleUnderAll isUnderAllOff scopePeers"},
                {"消息读取", "senderUin senderName msgTime msgIdOf msgSeqOf chatTypeOf peerUidOf"},
                {"身份/群", "uidFromUin uinFromUid isFriend currentUin currentNick currentUid troopMemberName"},
                {"元素构造", "elText elPic elPtt elArk elFace elVideo elFile elAt elReply contact fetchViaKernel listElementFactories"},
                {"协议", "sendProtocol jsonToPb pbToJson expectResponse cancelExpect lastRawResponse msfCmds msfDecode msfExtractElements"},
                {"红包原生通道", "hbSend hbTryCount hbReady redPacketSink"},
                {"TimNet 通道", "netRequest netReady netDumpStructures"},
                {"内核直取", "msgServiceImpl nativeMsgService wrapperSession kernelMsgService appRuntime ctx topActivity classLoader"},
                {"tenpay 加密", "walletEncrypt walletDecrypt walletProcess walletPsKey formString add4Byte"},
                {"Servlet 发包", "sendServlet servletClassName setServletClass servletBytes dumpBundle"},
                {"票据", "getPskey getPskeyBatch hexGuid getSkey getRealSkey getStweb getPt4Token getBkn bknOf getGTK hostOf"},
                {"UI", "toast runOnUi copyToClipboard readClipboard"},
                {"猫娘弹窗", "panelShow panelField panelFieldBox panelEdit panelEmpty panelButton"},
                {"文件", "dataDir writeFile readFile appendFile fileExists deleteFile logFile"},
                {"开关", "isCaptureEnabled setCaptureEnabled isAutoCaptureEnabled setAutoCaptureEnabled isCatifyTotalEnabled setCatifyTotalEnabled isFeatureEnabled setFeatureEnabled"},
                {"配置", "cfgGet cfgSet cfgGetBool cfgSetBool"},
                {"猫娘化", "catify catifyEnabled"},
                {"记录", "capturedCount capturedAll capturedClear capturedPeers isPeerAllowed"},
                {"工具", "now formatTime runAsync postDelayed sleep jsonPretty jsonPrettyOf hostVersionCode hostVersionName"},
                {"反射/Hook", "findClass hookBefore hookAfter hookAll"},
                {"PB 原语", "elemJsonToPb pbLd pbVf pbStr pbCat sendCode sendLongMsg sendDirectPb sendRawCmd lastResId"},
                {"伪记录", "fakeChat fakeChatAt mediaList mediaAdd mediaRemove mediaRename mediaPbHex locationTemplate"},
                        };
            int total = 0;
            java.util.Set<String> used = new java.util.HashSet<String>();
            for (String[] g : groups) {
                String title = g[0];
                String[] names = g[1].split(" ");
                StringBuilder section = new StringBuilder();
                for (java.lang.reflect.Method m : ms) {
                    if (!java.lang.reflect.Modifier.isPublic(m.getModifiers())) continue;
                    if (!java.lang.reflect.Modifier.isStatic(m.getModifiers())) continue;
                    if (!contains(names, m.getName())) continue;
                    if (used.contains(m.getName())) continue;
                    used.add(m.getName());
                    section.append("  ").append(m.getName()).append("(");
                    Class<?>[] ps = m.getParameterTypes();
                    for (int i = 0; i < ps.length; i++) {
                        if (i > 0) section.append(", ");
                        section.append(simple(ps[i]));
                    }
                    section.append(")\n");
                    total++;
                }
                if (section.length() > 0) {
                    sb.append("◆ ").append(title).append("\n").append(section).append("\n");
                }
            }

            StringBuilder rest = new StringBuilder();
            for (java.lang.reflect.Method m : ms) {
                if (!java.lang.reflect.Modifier.isPublic(m.getModifiers())) continue;
                if (!java.lang.reflect.Modifier.isStatic(m.getModifiers())) continue;
                if (used.contains(m.getName())) continue;
                used.add(m.getName());
                rest.append("  ").append(m.getName()).append("(");
                Class<?>[] ps = m.getParameterTypes();
                for (int i = 0; i < ps.length; i++) {
                    if (i > 0) rest.append(", ");
                    rest.append(simple(ps[i]));
                }
                rest.append(")\n");
                total++;
            }
            if (rest.length() > 0) sb.append("◆ 其他\n").append(rest).append("\n");
            sb.insert(0, "CatApi 已内置 —— 共 " + total + " 个方法，任何功能项 / 脚本可直接调：\n\n");
        } catch (Throwable t) {
            sb.append("读取 API 失败: ").append(t);
        }
        sb.append("\n包：com.cathelperawa.owo.app.api.CatApi");
        return sb.toString();
    }

    private static boolean contains(String[] arr, String s) {
        for (String a : arr) if (a.equals(s)) return true;
        return false;
    }

    private static String simple(Class<?> c) {
        String n = c.getSimpleName();
        return n.isEmpty() ? c.getName() : n;
    }

    private void showApiDialogImpl() {
        final float den = getResources().getDisplayMetrics().density;
        final android.app.Dialog d = new android.app.Dialog(this,
                android.R.style.Theme_Translucent_NoTitleBar);
        LinearLayout rootV = new LinearLayout(this);
        rootV.setOrientation(LinearLayout.VERTICAL);
        rootV.setBackgroundColor(0xB3303A4D);
        rootV.setPadding((int) (20 * den), (int) (60 * den), (int) (20 * den), (int) (30 * den));

        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFFF4F8FF);
        bg.setCornerRadius(22 * den);
        panel.setBackgroundDrawable(bg);
        panel.setPadding((int) (16 * den), (int) (16 * den), (int) (16 * den), (int) (16 * den));

        Bitmap apiArt = Images.get("art1");
        if (apiArt != null) {
            ImageView banner = new ImageView(this);
            banner.setScaleType(ImageView.ScaleType.CENTER_CROP);
            banner.setImageBitmap(apiArt);
            panel.setClipToOutline(true);
            panel.addView(banner, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, (int) (110 * den)));
        }

        TextView title = text("内置 API 调用", 17, 0xFF43587A, true, 0);
        LinearLayout.LayoutParams tp2 = matchWrap((int) (6 * den));
        tp2.topMargin = (int) (10 * den);
        panel.addView(title, tp2);

        final String apiText = buildApiDoc();
        TextView body = new TextView(this);
        body.setTextSize(12);
        body.setTextColor(0xFF2F3E56);
        body.setText(apiText);
        body.setTextIsSelectable(true);

        body.setPadding(0, (int) (10 * den), (int) (6 * den), (int) (6 * den));
        android.widget.ScrollView sv = new android.widget.ScrollView(this);
        sv.addView(body);

        sv.setVerticalScrollBarEnabled(false);
        sv.setHorizontalScrollBarEnabled(false);
        sv.setOverScrollMode(View.OVER_SCROLL_NEVER);
        sv.setClipToPadding(false);

        final android.widget.LinearLayout bodyWrap = new LinearLayout(this);
        bodyWrap.setOrientation(LinearLayout.HORIZONTAL);
        bodyWrap.addView(sv, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.MATCH_PARENT, 1f));
        final View barThumb = new View(this);
        GradientDrawable tbg = new GradientDrawable();
        tbg.setColor(0x557FA5E6);
        tbg.setCornerRadius(50 * den);
        barThumb.setBackgroundDrawable(tbg);
        barThumb.setAlpha(0f);
        LinearLayout barCol = new LinearLayout(this);
        barCol.setOrientation(LinearLayout.VERTICAL);
        barCol.addView(barThumb, new LinearLayout.LayoutParams((int) (3 * den), 0));
        LinearLayout.LayoutParams bcp = new LinearLayout.LayoutParams(
                (int) (3 * den), LinearLayout.LayoutParams.MATCH_PARENT);
        bcp.leftMargin = (int) (6 * den);
        bodyWrap.addView(barCol, bcp);
        panel.addView(bodyWrap, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        com.cathelperawa.owo.app.ui.UiKit.attachSlimScrollbar(sv, barThumb, barCol, den);

        TextView ok = text("知道了", 14, 0xFFFFFFFF, true, 0);
        ok.setGravity(Gravity.CENTER);
        GradientDrawable obg = new GradientDrawable();
        obg.setColor(0xFF7FA5E6);
        obg.setCornerRadius(20 * den);
        ok.setBackgroundDrawable(obg);
        LinearLayout.LayoutParams op = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, (int) (42 * den));
        op.topMargin = (int) (12 * den);
        panel.addView(ok, op);
        ok.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { d.dismiss(); }
        });

        rootV.addView(panel, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        d.setContentView(rootV);
        d.show();
        android.view.Window w = d.getWindow();
        if (w != null) {
            w.setLayout(LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT);
        }
    }

    private int dp(float v) {
        return com.cathelperawa.owo.app.ui.UiKit.dp(this, v);
    }

}
