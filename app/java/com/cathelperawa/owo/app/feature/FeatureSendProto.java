package com.cathelperawa.owo.app.feature;

import android.app.Activity;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;

import com.cathelperawa.owo.app.core.HookApi;
import com.cathelperawa.owo.app.core.HookConfig;
import com.cathelperawa.owo.app.core.HookItem;
import com.cathelperawa.owo.app.core.HookItemBase;
import com.cathelperawa.owo.app.core.HookLog;
import com.cathelperawa.owo.app.core.SelfCheckResult;
import com.cathelperawa.owo.app.ui.SendPanel;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;

import de.robv.android.xposed.XC_MethodHook;

@HookItem(name = "发送与协议", path = "Hook 功能/发送", order = 82,
        description = "长按输入栏的「+」弹出发送面板（卡片 / 消息代码 / PB）",
        defaultEnabled = true, hosts = {"tim", "qq"})
public class FeatureSendProto extends HookItemBase {

    public static final String K = "hook_send_proto";

    private static final String CLS_EDIT_TEXT = "com.tencent.mobileqq.aio.input.edit.AIOEditText";

    private static final Handler UI = new Handler(Looper.getMainLooper());

    private static int seen = 0;

    private static final java.util.List<View> WATCH =
            java.util.Collections.synchronizedList(new java.util.ArrayList<View>());
    private static final Map<View, Long> DOWN_AT =
            java.util.Collections.synchronizedMap(new java.util.WeakHashMap<View, Long>());
    private static volatile boolean dispHooked = false;

    private static final Map<View, Boolean> HOOKED =
            java.util.Collections.synchronizedMap(new WeakHashMap<View, Boolean>());

    public boolean mainProcessOnly() { return true; }

    @Override

    public String switchKey() {
        return K;
    }

    @Override
    public void entry(ClassLoader cl) {
        hint("长按输入栏右边的「+」");
        try {
            Class<?> cls = HookApi.findClass(cl, CLS_EDIT_TEXT);
            if (cls == null) {
                fail("未找到 AIOEditText");
                return;
            }
            int n = 0;
            for (java.lang.reflect.Constructor<?> ctor : cls.getDeclaredConstructors()) {
                try {
                    HookApi.after(ctor, new HookApi.Action() {
                        @Override public void call(XC_MethodHook.MethodHookParam p) {
                            try {
                                Object o = p.thisObject;
                                if (o instanceof EditText) armPlus((EditText) o);
                            } catch (Throwable ignore) {
                            }
                        }
                    }, this);
                    n++;
                } catch (Throwable ignore) {
                }
            }
            dbg("AIOEditText 构造器已挂 " + n + " 个");
            hookSession(cl);
            if (n > 0) expect(1);
            else fail("AIOEditText 构造器一个都没挂上");
        } catch (Throwable t) {
            fail("entry 异常: " + t, t);
        }
    }

    

    @Override
    protected void check(SelfCheckResult.Checker c) {
        
        Class<?> et = c.cls("输入框", CLS_EDIT_TEXT);

        c.cls("消息项", "com.tencent.mobileqq.aio.msglist.holder.AIOBubbleMsgItemVB");

        c.cls("内核 MsgElement", "com.tencent.qqnt.kernel.nativeinterface.MsgElement");
        c.cls("内核 ArkElement", "com.tencent.qqnt.kernel.nativeinterface.ArkElement");
        c.cls("内核 Contact", "com.tencent.qqnt.kernelpublic.nativeinterface.Contact",
                "com.tencent.qqnt.kernel.nativeinterface.Contact");

        int hooked = 0;
        synchronized (HOOKED) {
            hooked = HOOKED.size();
        }
        c.pass("长按挂载", hooked > 0 ? ("已挂 " + hooked + " 个「+」") : "等进聊天时自动挂");

        try {
            com.cathelperawa.owo.app.bridge.CurrentChat.fromActivity(
                    com.cathelperawa.owo.app.bridge.QQEnv.topActivity());
        } catch (Throwable ignore) {
        }
        c.pass("当前会话", com.cathelperawa.owo.app.bridge.CurrentChat.describe());
    }

    

    
    private void hookSession(ClassLoader cl) {
        try {
            Class<?> vb = HookApi.findClass(cl,
                    "com.tencent.mobileqq.aio.msglist.holder.AIOBubbleMsgItemVB");
            if (vb == null) {
                dbg("未找到 AIOBubbleMsgItemVB，会话捕获不可用");
                return;
            }
            java.lang.reflect.Method bind = null;
            for (java.lang.reflect.Method m : vb.getDeclaredMethods()) {
                Class<?>[] ps = m.getParameterTypes();
                if (ps.length != 4) continue;
                if (ps[0] != int.class) continue;
                if (!java.util.List.class.isAssignableFrom(ps[2])) continue;
                if (!"android.os.Bundle".equals(ps[3].getName())) continue;
                if (m.getReturnType() != void.class) continue;
                bind = m;
                break;
            }
            if (bind == null) {
                dbg("AIOBubbleMsgItemVB 没找到 4 参 bind");
                return;
            }
            HookApi.after(bind, new HookApi.Action() {
                @Override public void call(XC_MethodHook.MethodHookParam p) {
                    try {
                        if (p.args == null || p.args.length < 2) return;
                        Object item = p.args[1];
                        if (item == null) return;
                        if (seen++ < 3) {
                            dbg("bind 命中 item=" + item.getClass().getName());
                        }
                        com.cathelperawa.owo.app.bridge.CurrentChat.fromRecord(item);
                        if (seen <= 3) {
                            dbg("   → 会话=" + com.cathelperawa.owo.app.bridge.CurrentChat.describe());
                        }
                    } catch (Throwable t) {
                        if (seen < 3) dbg("fromRecord 异常 " + t);
                    }
                }
            }, this);
            dbg("会话捕获已挂: " + bind.getName());
        } catch (Throwable t) {
            dbg("hookSession 失败 " + t);
        }
    }

    

    private void armPlus(final EditText et) {
        if (!on()) return;
        UI.postDelayed(new Runnable() {
            @Override public void run() {
                tryArm(et, 0);
            }
        }, 300);
        watchLayout(et);
    }

    private static final java.util.Set<EditText> WATCHED =
            java.util.Collections.newSetFromMap(new java.util.WeakHashMap<EditText, Boolean>());
    
    private static final Map<View, Boolean> LONG_FIRED =
            java.util.Collections.synchronizedMap(new java.util.WeakHashMap<View, Boolean>());

    private static final Map<View, View.OnLongClickListener> OURS =
            java.util.Collections.synchronizedMap(new java.util.WeakHashMap<View, View.OnLongClickListener>());
    private static final long SCAN_GAP_MS = 250;
    private static long sLastScan = 0;

    private void watchLayout(final EditText et) {
        if (!WATCHED.add(et)) return;
        UI.post(new Runnable() {
            @Override public void run() {
                try {
                    final View root = et.getRootView();
                    if (root == null) return;
                    root.getViewTreeObserver().addOnGlobalLayoutListener(
                            new android.view.ViewTreeObserver.OnGlobalLayoutListener() {
                                @Override public void onGlobalLayout() {
                                    try {
                                        if (!on()) return;
                                        long now = System.currentTimeMillis();
                                        if (now - sLastScan < SCAN_GAP_MS) return;
                                        sLastScan = now;
                                        if (et.getWindowToken() == null) return;
                                        View plus = findPlus(et);
                                        if (plus != null) arm(plus);
                                    } catch (Throwable ignore) {
                                    }
                                }
                            });
                } catch (Throwable t) {
                    dbg("watchLayout 失败 " + t);
                }
            }
        });
    }

    private void tryArm(final EditText et, final int attempt) {
        try {
            if (!on()) return;
            if (et.getWindowToken() == null) {
                if (attempt < 8) retry(et, attempt);
                return;
            }
            View plus = findPlus(et);
            if (plus == null) {
                if (attempt == 0 || attempt == 7) dumpCandidates(et, attempt);
                if (attempt < 8) retry(et, attempt);
                else dbg("8 次都没找到 + 号");
                return;
            }
            arm(plus);
        } catch (Throwable t) {
            dbg("tryArm 异常 " + t);
        }
    }

    private void retry(final EditText et, final int attempt) {
        UI.postDelayed(new Runnable() {
            @Override public void run() {
                tryArm(et, attempt + 1);
            }
        }, 800);
    }

    private void hookDispatch() {
        // 彻底停用全局分发层 touch 兜底，避免与 HyperOS 传送门及全局手势冲突产生无界误触
    }

    private void handleTouch(Object host, Object[] args) {
        try {
            if (!on()) return;
            if (args == null || args.length == 0) return;
            Object ao = args[0];
            if (!(ao instanceof android.view.MotionEvent)) return;
            android.view.MotionEvent e = (android.view.MotionEvent) ao;
            int act = e.getActionMasked();
            if (act != android.view.MotionEvent.ACTION_DOWN
                    && act != android.view.MotionEvent.ACTION_UP) return;
            long now = System.currentTimeMillis();
            float x = e.getRawX(), y = e.getRawY();
            synchronized (WATCH) {
                for (int i = WATCH.size() - 1; i >= 0; i--) {
                    android.view.View v = WATCH.get(i);
                    if (v == null) { WATCH.remove(i); continue; }
                    android.graphics.Rect r = new android.graphics.Rect();
                    if (!v.getGlobalVisibleRect(r)) {
                        dbg2("WATCH 不可见 " + describe(v));
                        continue;
                    }
                    if (!r.contains((int) x, (int) y)) continue;
                    if (act == android.view.MotionEvent.ACTION_DOWN) {
                        DOWN_AT.put(v, now);
                        dbg2("按下命中 + 号 " + describe(v) + " host=" + host.getClass().getSimpleName());
                    } else {
                        Long d = DOWN_AT.remove(v);
                        long dt = d == null ? -1 : now - d;
                        dbg2("抬起 dt=" + dt + " 命中 " + describe(v));
                        if (d != null && dt >= 600) {
                            if (LONG_FIRED.put(v, Boolean.TRUE) != null) continue;
                            fire();
                            SendPanel.show(v.getContext());
                        }
                    }
                }
            }
        } catch (Throwable ignore) {
        }
    }

    private static int sTouchLog = 0;
    private void dbg2(String s) {
        if (sTouchLog > 120) return;
        sTouchLog++;
        dbg(s);
    }

    private static boolean isUnder(android.view.View parent, android.view.View child) {
        android.view.View p = child;
        int dep = 0;
        while (p != null && dep++ < 12) {
            if (p == parent) return true;
            android.view.ViewParent vp = p.getParent();
            p = (vp instanceof android.view.View) ? (android.view.View) vp : null;
        }
        return false;
    }

    private void arm(final View plus) {
        View.OnLongClickListener l = OURS.get(plus);
        if (l == null) {
            l = new View.OnLongClickListener() {
                @Override public boolean onLongClick(View v) {
                    try {
                        if (!on()) return false;
                        LONG_FIRED.put(v, Boolean.TRUE);   
                        fire();
                        SendPanel.show(v.getContext());
                        return true;    
                    } catch (Throwable t) {
                        HookLog.e("SendPanel 打开失败", t);
                        dbg("SendPanel 异常 " + t);
                    }
                    return false;
                }
            };
            OURS.put(plus, l);
        }

        synchronized (HOOKED) {
            if (HOOKED.containsKey(plus)) return;
            HOOKED.put(plus, Boolean.TRUE);
        }
        plus.setOnLongClickListener(l);

        try {
            plus.setOnTouchListener(new View.OnTouchListener() {
                private long downAt;
                private float downX, downY;
                private boolean moved;

                @Override public boolean onTouch(View v, android.view.MotionEvent e) {
                    switch (e.getActionMasked()) {
                        case android.view.MotionEvent.ACTION_DOWN:
                            downAt = System.currentTimeMillis();
                            downX = e.getX();
                            downY = e.getY();
                            moved = false;
                            return false;   
                        case android.view.MotionEvent.ACTION_MOVE:
                            if (Math.hypot(e.getX() - downX, e.getY() - downY) > 25) {
                                moved = true;
                            }
                            return false;
                        case android.view.MotionEvent.ACTION_UP:
                            if (LONG_FIRED.remove(v) != null) return true;
                            long dt = System.currentTimeMillis() - downAt;
                            // 必须无滑动且长按达到 800ms
                            if (!moved && dt >= 800) {
                                try {
                                    if (!on()) return false;
                                    fire();
                                    SendPanel.show(v.getContext());
                                    return true;
                                } catch (Throwable t) {
                                    HookLog.e("SendPanel 打开失败(touch)", t);
                                }
                            }
                            return false;
                        default:
                            return false;
                    }
                }
            });
        } catch (Throwable t) {
            dbg("挂触摸长按失败 " + t);
        }
        hit();
        dbg("已给 + 号挂上长按: " + describe(plus));
    }

    

    
    private View findPlus(EditText et) {
        View root = et.getRootView();
        if (root == null) return null;

        Rect etRect = new Rect();
        et.getGlobalVisibleRect(etRect);

        List<View> all = new ArrayList<View>();
        collect(root, all);

        // 1. 优先匹配明确是加号/更多/扩展的按钮，并严密排除发送/语音/表情/相册
        for (int i = 0; i < all.size(); i++) {
            View v = all.get(i);
            if (!clickable(v)) continue;
            CharSequence cd = v.getContentDescription();
            if (cd == null) continue;
            String s = cd.toString().toLowerCase(Locale.US);
            
            if (isForbiddenButton(s, v)) {
                continue;
            }
            if (s.contains("更多") || s.contains("more") || s.contains("plus")
                    || s.contains("加号") || s.contains("加") || s.contains("扩展")) {
                return v;
            }
        }

        // 2. 兜底匹配最右侧候选，但必须严格杜绝命中发送键/语音键/表情键
        View best = null;
        int bestLeft = -1;
        for (int i = 0; i < all.size(); i++) {
            View v = all.get(i);
            if (v == et || !clickable(v)) continue;
            if (!(v instanceof ImageView) && !isButtonLike(v)) continue;

            CharSequence cd = v.getContentDescription();
            String s = cd == null ? "" : cd.toString().toLowerCase(Locale.US);
            if (isForbiddenButton(s, v)) {
                continue;
            }

            Rect r = new Rect();
            if (!v.getGlobalVisibleRect(r)) continue;
            if (r.width() < dp(18) || r.height() < dp(18)) continue;
            if (r.width() > dp(96) || r.height() > dp(96)) continue;
            if (r.left < etRect.right - dp(10)) continue;   

            if (r.left > bestLeft) {
                bestLeft = r.left;
                best = v;
            }
        }
        return best;
    }

    private boolean isForbiddenButton(String desc, View v) {
        if (desc != null && (desc.contains("发送") || desc.contains("send") || desc.contains("语音")
                || desc.contains("voice") || desc.contains("表情") || desc.contains("相册")
                || desc.contains("拍照") || desc.contains("红包") || desc.contains("图片"))) {
            return true;
        }
        int id = v.getId();
        if (id != View.NO_ID && v.getResources() != null) {
            try {
                String resName = v.getResources().getResourceEntryName(id).toLowerCase(Locale.US);
                if (resName.contains("send") || resName.contains("ptt") || resName.contains("voice")
                        || resName.contains("face") || resName.contains("emotion")) {
                    return true;
                }
            } catch (Throwable ignore) {}
        }
        return false;
    }

    private boolean isButtonLike(View v) {
        String n = v.getClass().getName();
        return n.contains("Image") || n.contains("Button") || n.contains("Icon")
                || v.isClickable();
    }

    private boolean clickable(View v) {
        return v.isShown() && v.isClickable() && v.getVisibility() == View.VISIBLE;
    }

    private void collect(View v, List<View> out) {
        out.add(v);
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) {
                View c = g.getChildAt(i);
                if (c != null) collect(c, out);
            }
        }
    }

    private int dp(int v) {
        try {
            return (int) (v * ctx.getResources().getDisplayMetrics().density);
        } catch (Throwable t) {
            return v * 3;
        }
    }

    private String describe(View v) {
        try {
            Rect r = new Rect();
            v.getGlobalVisibleRect(r);
            return v.getClass().getSimpleName()
                    + " [" + r.left + "," + r.top + "," + r.right + "," + r.bottom + "]"
                    + " id=" + (v.getId() == View.NO_ID ? "-" : Integer.toHexString(v.getId()))
                    + " desc=" + v.getContentDescription();
        } catch (Throwable t) {
            return String.valueOf(v);
        }
    }

    private void dumpCandidates(EditText et, int attempt) {
        try {
            View root = et.getRootView();
            if (root == null) return;
            List<View> all = new ArrayList<View>();
            collect(root, all);
            Rect etRect = new Rect();
            et.getGlobalVisibleRect(etRect);
            dbg("---- dump #" + attempt + " 输入框=" + describe(et)
                    + " 全树 " + all.size() + " 个 View ----");
            for (int i = 0; i < all.size(); i++) {
                View v = all.get(i);
                if (!v.isShown()) continue;
                if (!v.isClickable() && !(v instanceof ImageView)) continue;
                dbg("  " + describe(v) + " clickable=" + v.isClickable());
            }
        } catch (Throwable t) {
            dbg("dump 失败 " + t);
        }
    }

    

    private boolean on() {
        try {
            return ctx != null && HookConfig.getBoolean(ctx, K, true);
        } catch (Throwable t) {
            return false;
        }
    }

    private void dbg(String s) {
        try {
            if (ctx == null) return;
            File f = new File(ctx.getFilesDir(), "send_proto_debug.txt");
            if (f.length() > 200 * 1024) return;
            java.io.FileOutputStream o = new java.io.FileOutputStream(f, true);
            try {
                String t = new java.text.SimpleDateFormat("HH:mm:ss", Locale.US)
                        .format(new java.util.Date());
                o.write((t + " " + s + "\n").getBytes("UTF-8"));
            } finally {
                try { o.close(); } catch (Throwable ignore) {
                }
            }
        } catch (Throwable ignore) {
        }
    }
}
