package com.cathelperawa.owo.app.ui;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.ColorDrawable;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ScrollView;

import com.cathelperawa.owo.app.bridge.QQEnv;
import com.cathelperawa.owo.app.core.HookLog;

import java.util.ArrayList;
import java.util.Timer;

public final class QqConsole implements Page.Host {

    private static QqConsole instance;

    private final Activity act;
    private final Dialog dlg;
    private final FrameLayout holder;

    private final ArrayList<String> stack = new ArrayList<String>();
    private final ArrayList<Object[]> switches = new ArrayList<Object[]>();
    private Timer heroTimer;
    private ScrollView homeScroll;
    private String current = "home";

    private QqConsole(Activity act) {
        this.act = act;
        this.dlg = new Dialog(act, android.R.style.Theme_DeviceDefault_Light_NoActionBar);
        this.holder = new FrameLayout(act);
        holder.setBackgroundColor(0xFFFFFFFF);
        holder.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    public static void show(Context ctx, String page) {
        if (page == null || page.length() == 0) page = "home";
        Activity a = ctx instanceof Activity ? (Activity) ctx : QQEnv.topActivity();
        if (a == null) {
            HookLog.w("QqConsole: 拿不到 Activity");
            return;
        }
        if (instance != null) {

            if (instance.dlg.isShowing()) {
                try {
                    instance.go(page);
                    return;
                } catch (Throwable ignore) {
                }
            }
            instance = null;
        }
        QqConsole c = new QqConsole(a);
        instance = c;
        c.open0(page);
    }

    private void open0(String page) {
        try {

            try {
                Images.load(act);
            } catch (Throwable t) {
                HookLog.w("QqConsole: 图片加载失败 " + t);
            }

            com.cathelperawa.owo.app.core.UIScope.set(com.cathelperawa.owo.app.core.UIScope.QQ);

            dlg.setContentView(holder);
            Window w = dlg.getWindow();
            if (w != null) {
                w.setBackgroundDrawable(new ColorDrawable(0xFFFFFFFF));
                w.setLayout(WindowManager.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.MATCH_PARENT);

                w.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
                w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            }
            dlg.setOnKeyListener(new android.content.DialogInterface.OnKeyListener() {
                @Override public boolean onKey(android.content.DialogInterface d, int code, KeyEvent e) {
                    if (code == KeyEvent.KEYCODE_BACK && e.getAction() == KeyEvent.ACTION_UP) {
                        close();
                        return true;
                    }
                    return false;
                }
            });
            dlg.show();
            go(page);
        } catch (Throwable t) {
            HookLog.e("QqConsole: 打不开", t);
        }
    }

    private void go(String page) {
        current = page;
        setPageView(applyPage(page));
    }

    static void dismissIfOpen() {
        if (instance != null) {
            try { instance.dlg.dismiss(); } catch (Throwable ignore) {}
            instance = null;
        }
    }

    private View applyPage(String page) {
        if (page == null) page = "home";
        try {
            if ("home".equals(page)) {
                View v = new HomePage(this).build();
                return v;
            }
            if (Page.PAGE_CHAT.equals(page)) return new FeaturePage(this, Page.PAGE_CHAT, "聊天功能").build();
            if (Page.PAGE_GROUP.equals(page)) return new GroupPage(this).build();
            if (Page.PAGE_FILE.equals(page)) return new FilePage(this).build();
            if (Page.PAGE_FAKE.equals(page)) return new FakePage(this).build();
            if (Page.PAGE_STATUS.equals(page)) return new StatusPage(this).build();
            if (Page.PAGE_CONFIG.equals(page)) return new ConfigPage(this).build();
            if (Page.PAGE_ANTI.equals(page)) return new AntiDetectPage(this).build();
            if (Page.PAGE_CAT.equals(page)) return new CatPage(this).build();
            if (Page.PAGE_HOOK.equals(page)) return new HookPage(this).build();
            if (Page.PAGE_MSG.equals(page)) return new MsgPage(this).build();
            if (Page.PAGE_SEND.equals(page)) return new SendPage(this).build();
            if (Page.PAGE_FULLCHECK.equals(page)) return new CheckPage(this).build();
            if (Page.PAGE_MSGTIME.equals(page)) return new MsgTimePage(this).build();
            if (Page.PAGE_SCOPE.equals(page)) return new ScopePage(this).build();
            if ("script".equals(page)) return new com.cathelperawa.owo.app.script.ScriptPage(this).build();
        } catch (Throwable t) {
            HookLog.w("QqConsole: 页面 " + page + " 构建失败 " + t);
            return placeholder(page, "这个页面在 QQ 里还没接完喵…");
        }
        return placeholder(page, "QQ 版还在移植，先在 TIM 里用喵");
    }

    private View placeholder(String page, String msg) {
        View[] out = new View[2];
        String title = titleOf(page);
        android.widget.LinearLayout root = new Page0(this).pageRoot(title, "QQ · 猫娘助手", out);
        android.widget.TextView t = new android.widget.TextView(act);
        t.setText(msg);
        t.setTextSize(13);
        t.setTextColor(0xFF8A9CB5);
        t.setPadding(0, UiKit.dp(act, 10), 0, 0);
        root.addView(t);
        return out[0];
    }

    private String titleOf(String page) {
        if ("home".equals(page)) return "猫娘助手";
        if ("ai".equals(page)) return "猫娘 AI";
        if ("script".equals(page)) return "脚本";
        if (Page.PAGE_CHAT.equals(page)) return "聊天功能";
        if (Page.PAGE_GROUP.equals(page)) return "群管理";
        if (Page.PAGE_FILE.equals(page)) return "文件功能";
        if (Page.PAGE_FAKE.equals(page)) return "伪造功能";
        if (Page.PAGE_STATUS.equals(page)) return "功能状态";
        if (Page.PAGE_CONFIG.equals(page)) return "配置备份";
        if (Page.PAGE_ANTI.equals(page)) return "防检测";
        if (Page.PAGE_CAT.equals(page)) return "猫娘化引擎";
        if (Page.PAGE_FULLCHECK.equals(page)) return "全面体检";
        if (Page.PAGE_MSGTIME.equals(page)) return "显示消息时间";
        if (Page.PAGE_SCOPE.equals(page)) return "作用范围";
        return "猫娘助手";
    }

    private static final class Page0 extends Page {
        Page0(Page.Host host) { super(host); }
        @Override public View build() { return null; }
    }

    @Override public Context context() { return act; }

    @Override public void close() {
        if (!stack.isEmpty()) {
            String prev = stack.remove(stack.size() - 1);
            current = prev;
            setPageView(applyPage(prev));
            return;
        }
        try { dlg.dismiss(); } catch (Throwable ignore) {}
        instance = null;
    }

    @Override public void closeWithFade() { close(); }

    @Override public void open(String page, String title) {
        stack.add(current);
        current = page;
        setPageView(applyPage(page));
        restoreScroll(page);
    }

    @Override public void pushPage(View v) {
        stack.add(current);
        current = "__sub";
        setPageView(v);
    }

    @Override public void setPageView(View v) {
        if (v == null) return;
        try {
            holder.removeAllViews();
            holder.addView(v, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT));
            syncSwitches();
        } catch (Throwable t) {
            HookLog.w("QqConsole: setPageView 失败 " + t);
        }
    }

    @Override public void bindSwitch(String uiKey, SwitchBox box, Runnable onSynced) {
        if (box == null) return;
        switches.add(new Object[]{uiKey, box, onSynced});
        try { box.setChecked(UiConfig.swOn(act, uiKey)); } catch (Throwable ignore) {}
    }

    private void syncSwitches() {
        for (int i = 0; i < switches.size(); i++) {
            Object[] s = switches.get(i);
            try {
                ((SwitchBox) s[1]).setChecked(UiConfig.swOn(act, (String) s[0]));
                if (s[2] != null) ((Runnable) s[2]).run();
            } catch (Throwable ignore) {}
        }
    }

    @Override public void setHeroTimer(Timer t) {
        if (heroTimer != null) {
            try { heroTimer.cancel(); } catch (Throwable ignore) {}
        }
        heroTimer = t;
    }

    @Override public void setHomeScroll(ScrollView sv) { homeScroll = sv; }

    private void restoreScroll(String page) {
        if (!"home".equals(page) || homeScroll == null) return;
        final ScrollView sv = homeScroll;
        sv.post(new Runnable() {
            @Override public void run() {
                try { sv.scrollTo(0, sv.getScrollY()); } catch (Throwable ignore) {}
            }
        });
    }

    @Override public void openTerminal() {
        try {
            TerminalPanel.show(act);
        } catch (Throwable t) {
            HookLog.w("QqConsole: 终端打不开 " + t);
        }
    }

    @Override public void showApiDialog() {
        try {
            TerminalPanel.show(act);
        } catch (Throwable ignore) {}
    }

    static Bitmap bgOf() { return Images.get("bg"); }
}
