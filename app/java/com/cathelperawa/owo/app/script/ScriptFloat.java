package com.cathelperawa.owo.app.script;

import android.app.Activity;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ImageView;

import com.cathelperawa.owo.app.bridge.QQEnv;
import com.cathelperawa.owo.app.core.Bg;
import com.cathelperawa.owo.app.core.ConfigStore;
import com.cathelperawa.owo.app.core.HookLog;

public final class ScriptFloat {

    private static final String BALL_TAG = "cathelper_script_ball";

    static volatile View ballView;
    private static volatile ViewGroup ballParent;
    private static volatile boolean hooked = false;
    private static volatile boolean watching = false;
    private static volatile boolean lastChat = false;
    
    static volatile boolean hubHidden = false;
    
    private static volatile Activity lastResumed;

    static int currentX = 0, currentY = 0;
    private static float initialTouchX, initialTouchY;
    static int sizePx;
    private static int clickThreshold;
    private static boolean isDragging = false;

    private ScriptFloat() {}

    

    public static synchronized void hookOnce() {
        if (hooked) return;
        hooked = true;          

        
        
        ensureWatcher();

        try {
            android.content.Context c = QQEnv.appContext();
            if (c instanceof android.app.Application) {
                ((android.app.Application) c).registerActivityLifecycleCallbacks(
                        new android.app.Application.ActivityLifecycleCallbacks() {
                            @Override public void onActivityCreated(Activity a, android.os.Bundle b) {}
                            @Override public void onActivityStarted(Activity a) {}
                            @Override public void onActivityResumed(Activity a) {
                                lastResumed = a;
                                ensureWatcher();
                            }
                            @Override public void onActivityPaused(Activity a) {}
                            @Override public void onActivityStopped(Activity a) {}
                            @Override public void onActivitySaveInstanceState(Activity a, android.os.Bundle b) {}
                            @Override public void onActivityDestroyed(Activity a) { dismiss(); }
                        });
                HookLog.i("[悬浮球] 生命周期已挂 + 轮询已启动");
                return;
            }
        } catch (Throwable t) {
            HookLog.w("[悬浮球] 注册生命周期失败（轮询照样跑）: " + t);
        }
        HookLog.i("[悬浮球] 只靠轮询");
    }

    static void refreshAll() {
        dismiss();
        lastChat = false;
        hubHidden = false;
        ensureWatcher();
    }

    

    private static synchronized void ensureWatcher() {
        if (watching) return;
        watching = true;
        final android.os.Handler h = new android.os.Handler(android.os.Looper.getMainLooper());
        h.postDelayed(new Runnable() {
            @Override public void run() {
                try {
                    tick();
                } catch (Throwable t) {
                    HookLog.w("[悬浮球] tick 异常 " + t);
                }
                h.postDelayed(this, 800);
            }
        }, 800);
    }

    private static void tick() {
        if (ScriptFloatRegistry.size() == 0 && !ScriptHub.enabled()) {
            if (ballView != null) dismiss();
            lastChat = false;
            return;
        }
        
        if (hubHidden && ScriptHub.enabled()) {
            if (ballView != null) dismiss();
            return;
        }

        
        Activity a = lastResumed;
        if (a == null || a.isFinishing()) a = QQEnv.topActivity();
        if (a == null) return;

        if (a.getClass().getName().startsWith("com.cathelperawa.owo.app")) {
            if (ballView != null) dismiss();
            lastChat = false;
            return;
        }

        
        
        String curPeer = "";
        long curAt = 0;
        try {
            curPeer = com.cathelperawa.owo.app.bridge.CurrentChat.peerUid();
            curAt = com.cathelperawa.owo.app.bridge.CurrentChat.updatedAt();
        } catch (Throwable ignore) {
        }
        long age = curAt <= 0 ? -1 : (System.currentTimeMillis() - curAt);

        

        

        
        String simpleName = a.getClass().getSimpleName();
        boolean chat;
        if ("ChatActivity".equals(simpleName)) {
            chat = true;
        } else if ("SplashActivity".equals(simpleName)) {
            
            View rootView = a.findViewById(android.R.id.content);
            boolean mainNav = rootView != null
                    && hasText(rootView, "联系人") && hasText(rootView, "我的");
            chat = !mainNav;
        } else {
            chat = false;
        }

        android.view.ViewGroup curParent = null;
        try {
            android.view.View cv = a.findViewById(android.R.id.content);
            curParent = (cv instanceof android.view.ViewGroup)
                    ? (android.view.ViewGroup) cv
                    : (android.view.ViewGroup) a.getWindow().getDecorView();
        } catch (Throwable ignore) {
        }
        boolean have = (ballView != null && ballView.getParent() != null
                && curParent != null && ballView.getParent() == curParent);

        
        
        try {
            java.util.List<ScriptFloatRegistry.Ball> allBalls = ScriptFloatRegistry.all();
            boolean allHidden = !allBalls.isEmpty();
            for (int i = 0; i < allBalls.size(); i++) {
                if (!allBalls.get(i).hidden) { allHidden = false; break; }
            }
            if (allHidden) {
                if (ballView != null) dismiss();
                return;
            }
        } catch (Throwable ignore) {
        }

        boolean needShow = chat && !have;
        boolean needHide = !chat && have;

        HookLog.i("[悬浮球] tick chat=" + chat + " have=" + have
                + " show=" + needShow + " hide=" + needHide
                + " act=" + a.getClass().getSimpleName());

        if (needShow) {
            build(a);
        } else if (needHide) {
            dismiss();
        }
        lastChat = chat;
    }

    

    private static void build(final Activity activity) {
        final ScriptFloatRegistry.Ball ball = ballAt(0);
        
        final boolean hubMode = ScriptHub.enabled();
        if (ball == null && !hubMode) return;
        try {
            dismiss();

            float density = activity.getResources().getDisplayMetrics().density;
            
            ScriptFloatRegistry.Ball ballCfg = (ball == null) ? null
                    : ScriptFloatRegistry.find(ball.script, ball.id);
            int wantDp = hubMode ? ScriptHub.ballSizeDp()
                    : ((ballCfg == null) ? 42 : ballCfg.sizeDp);
            sizePx = (int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, wantDp, activity.getResources().getDisplayMetrics());
            clickThreshold = (int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, 10, activity.getResources().getDisplayMetrics());

            final FrameLayout root = new FrameLayout(activity);
            root.setTag(BALL_TAG);

            ScriptFloatRegistry.Ball c = (ball == null) ? null
                    : ScriptFloatRegistry.find(ball.script, ball.id);
            int bgColor = (c == null) ? 0x00000000 : c.bgColor;
            if ((bgColor >>> 24) != 0) {
                GradientDrawable bg = new GradientDrawable();
                bg.setShape(GradientDrawable.OVAL);
                bg.setColor(bgColor);
                root.setBackground(bg);
            }

            ImageView icon = new ImageView(activity);
            icon.setScaleType(ImageView.ScaleType.CENTER_CROP);
            
            try {
                icon.setClipToOutline(true);
                icon.setOutlineProvider(new android.view.ViewOutlineProvider() {
                    @Override public void getOutline(View v, android.graphics.Outline o) {
                        o.setOval(0, 0, v.getWidth(), v.getHeight());
                    }
                });
            } catch (Throwable ignore) {
            }
            android.graphics.drawable.Drawable d = moduleIcon(activity,
                    hubMode ? ScriptHub.ballIcon()
                            : ((ball == null) ? "ic_module" : ball.icon));
            if (d != null) icon.setImageDrawable(d);
            int pad = (int) (2 * density);
            FrameLayout.LayoutParams ip = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT);
            ip.setMargins(pad, pad, pad, pad);
            root.addView(icon, ip);

            final FrameLayout.LayoutParams fp = new FrameLayout.LayoutParams(sizePx, sizePx);
            fp.gravity = Gravity.TOP | Gravity.START;

            
            View cv = activity.findViewById(android.R.id.content);
            ViewGroup decor = (cv instanceof ViewGroup)
                    ? (ViewGroup) cv
                    : (ViewGroup) activity.getWindow().getDecorView();
            int dw = decor.getWidth() > 0 ? decor.getWidth() : 1080;
            int dh = decor.getHeight() > 0 ? decor.getHeight() : 1920;

            int sx = loadX(), sy = loadY();
            currentX = (sx > 0) ? sx : (dw - sizePx - (int) (10 * density));
            currentY = (sy > 0) ? Math.max(minY(), sy) : (int) (dh * 0.18f);
            fp.leftMargin = currentX;
            fp.topMargin = currentY;

            root.setOnTouchListener(new View.OnTouchListener() {
                private long downAt;
                private float downX, downY;

                @Override public boolean onTouch(View v, MotionEvent e) {
                    switch (e.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            downAt = System.currentTimeMillis();
                            downX = e.getRawX();
                            downY = e.getRawY();
                            initialTouchX = downX;
                            initialTouchY = downY;
                            return true;
                        case MotionEvent.ACTION_MOVE:
                            if (Math.abs(e.getRawX() - downX) > clickThreshold
                                    || Math.abs(e.getRawY() - downY) > clickThreshold) {
                                isDragging = true;
                            }
                            if (isDragging) {
                                float nx = Math.max(0, currentX + (e.getRawX() - initialTouchX));
                                float ny = Math.max(minY(), currentY + (e.getRawY() - initialTouchY));
                                fp.leftMargin = (int) nx;
                                fp.topMargin = (int) ny;
                                root.setLayoutParams(fp);
                                currentX = (int) nx;
                                currentY = (int) ny;
                                initialTouchX = e.getRawX();
                                initialTouchY = e.getRawY();
                            }
                            return true;
                        case MotionEvent.ACTION_UP:
                        case MotionEvent.ACTION_CANCEL:
                            if (!isDragging && (System.currentTimeMillis() - downAt) < 500) {
                                Activity act = QQEnv.topActivity();
                                
                                if (act != null && ScriptHub.enabled()) {
                                    ScriptHubPanel.show(act);
                                } else if (act != null) {
                                    ScriptFloatRegistry.Ball b = ballAt(0);
                                    if (b != null && !b.clickMenu) {
                                        onTap(act);
                                    } else if (b != null) {
                                        java.util.List<ScriptPanelRegistry.Panel> ps =
                                                ScriptPanelRegistry.ofScript(b.script);
                                        String style = b.style == null ? "auto" : b.style;
                                        boolean useSheet = "sheet".equals(style)
                                                || ("auto".equals(style) && !ps.isEmpty());
                                        if (useSheet && !ps.isEmpty()) {
                                            ScriptSheet.show(act, ps.get(0).title,
                                                    ps.get(0).spec, ps.get(0));
                                        } else {
                                            showBallMenu(act);
                                        }
                                    } else {
                                        showBallMenu(act);
                                    }
                                }
                            }
                            isDragging = false;
                            if (e.getAction() == MotionEvent.ACTION_UP) savePos();
                            return true;
                        default:
                            return false;
                    }
                }
            });

            
            
            root.setLayoutParams(fp);
            try {
                root.measure(
                        View.MeasureSpec.makeMeasureSpec(sizePx, View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(sizePx, View.MeasureSpec.EXACTLY));
                root.layout(0, 0, sizePx, sizePx);
            } catch (Throwable ignore) {
            }
            decor.addView(root, fp);
            try {
                root.requestLayout();
                decor.requestLayout();
                decor.invalidate();
            } catch (Throwable ignore) {
            }
            ballView = root;
            ballParent = decor;
            if (hubMode) ScriptHub.diag("build 官方球 pages=" + ScriptHub.size()
                    + " icon=" + ScriptHub.ballIcon() + " size=" + sizePx);
            HookLog.i("[悬浮球] addView ok x=" + currentX + " y=" + currentY
                    + " size=" + sizePx + " parent=" + decor.getClass().getSimpleName()
                    + " " + dw + "x" + dh);

            root.postDelayed(new Runnable() {
                @Override public void run() {
                    try {
                        boolean same = (ballView == root);
                        HookLog.i("[悬浮球] 实际 w=" + root.getWidth() + " h=" + root.getHeight()
                                + " vis=" + root.getVisibility()
                                + " x=" + root.getX() + " y=" + root.getY()
                                + " shown=" + root.isShown()
                                + " 是当前球=" + same
                                + " 父=" + (root.getParent() == null ? "无" : "有"));
                    } catch (Throwable ignore) {
                    }
                }
            }, 800);
        } catch (Throwable t) {
            HookLog.e("[悬浮球] build 失败", t);
            ballView = null;
        }
    }

    static void dismiss() {

        
        
        try {
            Class<?> atC = Class.forName("android.app.ActivityThread");
            Object at = atC.getMethod("currentActivityThread").invoke(null);
            java.lang.reflect.Field f = atC.getDeclaredField("mActivities");
            f.setAccessible(true);
            Object mapObj = f.get(at);
            if (mapObj instanceof java.util.Map) {
                for (Object rec : new java.util.ArrayList<Object>(
                        ((java.util.Map<?, ?>) mapObj).values())) {
                    if (rec == null) continue;
                    try {
                        java.lang.reflect.Field af = rec.getClass().getDeclaredField("activity");
                        af.setAccessible(true);
                        Object ao = af.get(rec);
                        if (!(ao instanceof Activity)) continue;
                        Activity act = (Activity) ao;
                        View cv = act.findViewById(android.R.id.content);
                        if (cv instanceof ViewGroup) removeTagged((ViewGroup) cv);
                        View decor = act.getWindow() == null ? null : act.getWindow().getDecorView();
                        if (decor instanceof ViewGroup && decor != cv) removeTagged((ViewGroup) decor);
                    } catch (Throwable ignore) {
                    }
                }
            }
        } catch (Throwable ignore) {
        }
        try {
            if (ballView != null) {
                android.view.ViewParent p = ballView.getParent();
                if (p instanceof ViewGroup) ((ViewGroup) p).removeView(ballView);
            }
        } catch (Throwable ignore) {
        }
        ballView = null;
        ballParent = null;
    }

    private static void removeTagged(ViewGroup g) {
        try {
            for (int i = g.getChildCount() - 1; i >= 0; i--) {
                View c = g.getChildAt(i);
                if (c != null && BALL_TAG.equals(c.getTag())) {
                    g.removeView(c);
                }
            }
        } catch (Throwable ignore) {
        }
    }

    

    
    private static void showBallMenu(final Activity act) {
        try {
            ScriptFloatRegistry.Ball ball = ballAt(0);
            String script = (ball == null) ? "" : ball.script;
            
            ScriptHub.Page p = new ScriptHub.Page(script,
                    script.length() == 0 ? "脚本" : script,
                    (ball == null) ? null : ball.handler);
            if (ball != null) {
                for (int i = 0; i < ball.menu.size(); i++) {
                    ScriptFloatRegistry.MenuItem mi = ball.menu.get(i);
                    p.items.add(new ScriptHub.Item(mi.script, mi.id, mi.label, mi.handler));
                }
            }
            ScriptHubPanel.showSingle(act, p);
        } catch (Throwable t) {
            HookLog.w("[悬浮球] 弹菜单失败 " + t);
        }
    }

    static void runHandler(final String script, final String itemId,
                           final String label, final ScriptFloatHandler h) {
        Activity act = QQEnv.topActivity();
        String peer = "", peerName = "";
        boolean group = false;
        if (act != null) {
            String[] c = chatFromIntent(act);
            if (c != null) {
                group = "2".equals(c[0]);
                peer = c[1];
                peerName = c[2];
            }
        }
        final ScriptFloatInfo info = new ScriptFloatInfo(itemId, label, peer, peerName, group);
        final ScriptFloatHandler handler = h;
        Bg.run(new Runnable() {
            @Override public void run() {
                try {
                    ScriptExec.run(script, new Runnable() {
                        @Override public void run() {
                            if (handler != null) handler.onTap(info);
                        }
                    });
                } catch (Throwable t) {
                    HookLog.e("[悬浮球] 菜单项 " + label + " 失败", t);
                }
            }
        }, "cat-script-float-item");
    }

    static void hideHubBall() {
        hubHidden = true;
        try {
            java.util.List<ScriptFloatRegistry.Ball> all = ScriptFloatRegistry.all();
            for (int i = 0; i < all.size(); i++) all.get(i).hidden = true;
        } catch (Throwable ignore) {
        }
        dismiss();
    }

    private static void tapMenuItem(final ScriptFloatRegistry.MenuItem mi) {
        Activity act = QQEnv.topActivity();
        String peer = "", peerName = "";
        boolean group = false;
        if (act != null) {
            String[] c = chatFromIntent(act);
            if (c != null) {
                group = "2".equals(c[0]);
                peer = c[1];
                peerName = c[2];
            }
        }
        final ScriptFloatInfo info = new ScriptFloatInfo(mi.id, mi.label, peer, peerName, group);
        Bg.run(new Runnable() {
            @Override public void run() {
                try {
                    ScriptExec.run(mi.script, new Runnable() {
                        @Override public void run() {
                            mi.handler.onTap(info);
                        }
                    });
                } catch (Throwable t) {
                    HookLog.e("[悬浮球] 菜单项 " + mi.label + " 失败", t);
                }
            }
        }, "cat-script-float-item");
    }

    private static android.widget.PopupWindow ballMenu;

    private static void closeBallMenu() {
        try {
            if (ballMenu != null) ballMenu.dismiss();
        } catch (Throwable ignore) {
        }
        ballMenu = null;
    }

    private static View menuItem(Activity act, String text, int color, final Runnable onClick) {
        TextView tv = new TextView(act);
        tv.setText(text);
        tv.setTextSize(14);
        tv.setTextColor(color);
        tv.setPadding(dp(act, 14), dp(act, 11), dp(act, 14), dp(act, 11));
        tv.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                try {
                    onClick.run();
                } catch (Throwable ignore) {
                }
            }
        });
        return tv;
    }

    static void openPanel(Activity act, ScriptPanelRegistry.Panel pn) {
        try {
            android.content.Intent it = new android.content.Intent();
            it.setClassName(act.getPackageName(),
                    "com.cathelperawa.owo.app.ConsoleActivity");
            it.putExtra("cat_page", "script");
            it.putExtra("script_panel", pn.key());
            act.startActivity(it);
        } catch (Throwable t) {
            HookLog.w("[悬浮球] 打不开面板 " + t);
        }
    }

    

    private static void onTap(Activity act) {
        String[] chat = chatFromIntent(act);
        String peer = "", peerName = "";
        boolean group = false;
        if (chat != null) {
            group = "2".equals(chat[0]);
            peer = chat[1];
            peerName = chat[2];
        }
        if (peer.length() == 0) {
            try {
                peer = com.cathelperawa.owo.app.bridge.CurrentChat.peerUid();
                peerName = com.cathelperawa.owo.app.bridge.CurrentChat.peerName();
                group = com.cathelperawa.owo.app.bridge.CurrentChat.chatType() == 2;
            } catch (Throwable ignore) {
            }
        }
        final ScriptFloatInfo info = new ScriptFloatInfo("ball", "球", peer, peerName, group);

        Bg.run(new Runnable() {
            @Override public void run() {
                try {
                    java.util.List<ScriptFloatRegistry.Ball> balls = ScriptFloatRegistry.all();
                    for (int i = 0; i < balls.size(); i++) {
                        final ScriptFloatRegistry.Ball b = balls.get(i);
                        ScriptExec.run(b.script, new Runnable() {
                            @Override public void run() {
                                b.handler.onTap(info);
                            }
                        });
                    }
                } catch (Throwable t) {
                    HookLog.e("[悬浮球] 点击回调失败", t);
                }
            }
        }, "cat-script-float");
    }

    

    static boolean looksLikeChatPage(Activity a) {
        try {
            View root = a.findViewById(android.R.id.content);
            if (root == null) return false;
            if (hasText(root, "联系人") && hasText(root, "我的", 0)) return false;
            if (hasText(root, "账号管理") || hasText(root, "账号安全")) return false;
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private static boolean hasText(View v, String target) {
        return hasText(v, target, 0);
    }

    private static boolean hasText(View v, String target, int depth) {
        if (v == null || depth > 16) return false;
        try {
            if (v instanceof android.widget.TextView) {

                
                android.widget.TextView tv = (android.widget.TextView) v;
                if (tv.isShown()) {
                    CharSequence t = tv.getText();
                    if (t != null && t.toString().contains(target)) return true;
                }
            }
            if (v instanceof ViewGroup) {
                ViewGroup g = (ViewGroup) v;
                for (int i = 0; i < g.getChildCount(); i++) {
                    if (hasText(g.getChildAt(i), target, depth + 1)) return true;
                }
            }
        } catch (Throwable ignore) {
        }
        return false;
    }

    static String[] chatFromIntent(Activity a) {
        try {
            android.os.Bundle b = a.getIntent() == null ? null : a.getIntent().getExtras();
            if (b == null) return null;
            int ct = b.getInt("key_chat_type", 0);
            String peer = b.getString("key_peerId");
            if (peer == null || peer.length() == 0) {
                long uin = b.getLong("key_peerUin", 0);
                if (uin > 0) peer = String.valueOf(uin);
            }
            String name = b.getString("key_chat_name");
            if (peer == null || peer.length() == 0) return null;
            return new String[]{String.valueOf(ct == 0 ? 2 : ct), peer, name == null ? "" : name};
        } catch (Throwable t) {
            return null;
        }
    }

    

    private static ScriptFloatRegistry.Ball ballAt(int index) {
        try {
            java.util.List<ScriptFloatRegistry.Ball> all = ScriptFloatRegistry.all();
            return (index < all.size()) ? all.get(index) : null;
        } catch (Throwable t) {
            return null;
        }
    }

    static android.graphics.drawable.Drawable moduleIcon(android.content.Context c, String name) {
        try {
            
            if (name != null) {
                String p = name.trim();
                if (p.startsWith("file:")) p = p.substring(5);
                if (p.startsWith("/")) {
                    android.graphics.Bitmap bm =
                            android.graphics.BitmapFactory.decodeFile(p);
                    if (bm != null) {
                        return new android.graphics.drawable.BitmapDrawable(c.getResources(), bm);
                    }
                    HookLog.w("[悬浮球] 读图失败，退回默认图标 " + p);
                }
            }
            android.content.pm.PackageManager pm = c.getPackageManager();
            android.content.res.Resources res = pm.getResourcesForApplication("com.cathelperawa.owo.app");
            if (res == null) return null;
            String n = (name == null || name.length() == 0) ? "ic_module" : name;
            int id = res.getIdentifier(n, "drawable", "com.cathelperawa.owo.app");
            if (id == 0) id = res.getIdentifier("ic_module", "drawable", "com.cathelperawa.owo.app");
            if (id == 0) return null;
            return res.getDrawable(id, null);
        } catch (Throwable t) {
            HookLog.w("[悬浮球] 取图标失败 " + t);
            return null;
        }
    }

    private static void savePos() {
        try {
            ConfigStore.setInt("float_x_v4", currentX);
            ConfigStore.setInt("float_y_v4", currentY);
        } catch (Throwable ignore) {
        }
    }

    private static int loadX() {
        try { return ConfigStore.getInt("float_x_v4"); } catch (Throwable t) { return 0; }
    }

    private static int loadY() {
        try { return ConfigStore.getInt("float_y_v4"); } catch (Throwable t) { return 0; }
    }

    private static int minY() {
        try {
            android.content.Context c = QQEnv.appContext();
            if (c == null) return 120;
            int id = c.getResources().getIdentifier("status_bar_height", "dimen", "android");
            int sb = id > 0 ? c.getResources().getDimensionPixelSize(id) : 80;
            return sb + 10;
        } catch (Throwable t) {
            return 120;
        }
    }

    private static int dp(Activity a, int v) {
        try {
            return (int) (v * a.getResources().getDisplayMetrics().density + 0.5f);
        } catch (Throwable t) {
            return v * 3;
        }
    }
}