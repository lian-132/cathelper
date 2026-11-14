package com.cathelperawa.owo.app.feature;

import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.cathelperawa.owo.app.core.HookApi;
import com.cathelperawa.owo.app.core.HookConfig;
import com.cathelperawa.owo.app.core.HookItem;
import com.cathelperawa.owo.app.core.HookItemBase;
import com.cathelperawa.owo.app.core.HookLog;
import com.cathelperawa.owo.app.core.LoadPriority;
import com.cathelperawa.owo.app.core.SelfCheckResult;
import com.cathelperawa.owo.app.util.Refl;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import de.robv.android.xposed.XC_MethodHook;

@HookItem(name = "显示消息时间", path = "聊天/消息时间", order = 66,
        description = "每条消息右下角显示收到时间（格式/颜色可调）", defaultEnabled = false,
        priority = LoadPriority.DEFERRED, hosts = {"tim", "qq"})
public class FeatureMsgTime extends HookItemBase {

    public static final String K = "chat_msgtime";

    public static final String K_COLOR = "chat_msgtime_color";

    public static final String K_FORMAT = "chat_msgtime_format";

    public static final String K_SIZE = "chat_msgtime_size";

    public static final String DEF_COLOR = String.valueOf(0xFF0000FF);
    public static final String DEF_FORMAT = "HH:mm:ss";
    public static final String DEF_SIZE = "10";

    private static final String TAG_WRAP = "cathelper_time_wrap";
    private static final String TAG_TIME = "cathelper_time_text";

    private static final String CLS_VB = "com.tencent.mobileqq.aio.msglist.holder.AIOBubbleMsgItemVB";
    private static final String CLS_BUBBLE = "com.tencent.qqnt.aio.holder.template.BubbleLayoutCompatPress";

    public boolean mainProcessOnly() { return true; }

    @Override

    public String switchKey() {
        return K;
    }

    private boolean on() {
        try {
            return ctx != null && HookConfig.getBoolean(ctx, K, false);
        } catch (Throwable t) {
            return false;
        }
    }

    @Override
    public void entry(ClassLoader cl) {
        hint("发一条消息 / 打开聊天后触发");

        Class<?> vb = HookApi.findClass(cl, CLS_VB,
                "com.tencent.mobileqq.aio.msglist.holder.AIOBubbleMsgItemVB",
                "com.tencent.qqnt.aio.holder.AIOBubbleMsgItemVB",
                "com.tencent.mobileqq.aio.msglist.holder.BaseMsgItemVB");
        if (vb == null) {
            dumpProbe(cl);
            fail("未找到 " + CLS_VB);
            return;
        }
        Method bind = findBind(vb);
        if (bind == null) {

            int n = 0;
            for (final Method m : vb.getDeclaredMethods()) {
                Class<?>[] ps = m.getParameterTypes();
                if (ps.length != 1 || m.getReturnType() != void.class) continue;
                if (!ps[0].getName().contains("AIOMsgItem")) continue;
                hookAfter(m, new HookApi.Action() {
                    @Override public void call(XC_MethodHook.MethodHookParam p) {
                        try {
                            if (!on()) return;
                            Object it = p.args == null || p.args.length == 0 ? null : p.args[0];
                            if (it == null) return;
                            decorate(p.thisObject, new Object[]{null, it});
                        } catch (Throwable t) {
                            noteRuntimeError(t);
                        }
                    }
                });
                n++;
                HookLog.i("消息时间：已挂 " + m.getName() + "(AIOMsgItem)");
            }
            if (n == 0) {
                dumpProbe(cl);
                fail("AIOBubbleMsgItemVB 里没有可用的消息绑定方法");
                return;
            }
            hit(n);
            return;
        }
        HookLog.i("消息时间：已定位 " + vb.getSimpleName() + "#" + bind.getName()
                + "(" + bind.getParameterTypes().length + " 参)");

        hookAfter(bind, new HookApi.Action() {
            @Override public void call(XC_MethodHook.MethodHookParam p) {
                try {
                    if (!on()) return;
                    decorate(p.thisObject, p.args);
                } catch (Throwable t) {
                    noteRuntimeError(t);
                }
            }
        });
        hit();
    }

    private void dumpProbe(ClassLoader cl) {
        StringBuilder sb = new StringBuilder();
        Class<?> c = HookApi.findClass(cl, CLS_VB);
        int dep = 0;
        while (c != null && c != Object.class && dep++ < 5) {
            sb.append("== ").append(c.getName()).append('\n');
            for (Method m : c.getDeclaredMethods()) {
                if (m.getReturnType() != void.class) continue;
                StringBuilder ps = new StringBuilder();
                for (Class<?> p : m.getParameterTypes()) ps.append(p.getSimpleName()).append(',');
                sb.append("   ").append(m.getName()).append('(').append(ps).append(")\n");
            }
            c = c.getSuperclass();
        }
        try {
            java.io.File f = new java.io.File(ctx.getFilesDir(), "msgtime_probe.txt");
            java.io.FileOutputStream o = new java.io.FileOutputStream(f, false);
            o.write(sb.toString().getBytes("UTF-8"));
            o.close();
        } catch (Throwable ignore) {
        }
        HookLog.i("消息时间探测已落盘");
    }

    private static Method findBind(Class<?> vb) {

        Class<?> k = vb;
        int dep = 0;
        while (k != null && k != Object.class && dep++ < 4) {
            Method m = findBindIn(k);
            if (m != null) return m;
            k = k.getSuperclass();
        }
        return findBindIn(vb);
    }

    private static Method findBindIn(Class<?> vb) {

        for (Method m : vb.getDeclaredMethods()) {
            Class<?>[] ps = m.getParameterTypes();
            if (ps.length != 4) continue;
            if (m.getReturnType() != void.class) continue;
            if (ps[0] != int.class) continue;
            if (!List.class.isAssignableFrom(ps[2])) continue;
            if (!"android.os.Bundle".equals(ps[3].getName())) continue;
            try { m.setAccessible(true); } catch (Throwable ignore) {}
            return m;
        }
        return null;
    }

    private void decorate(Object holder, Object[] args) {
        if (holder == null || args == null || args.length < 2) return;

        Object msgItem = pickMsgItem(args);
        if (msgItem == null) msgItem = pickMsgItemFrom(holder);
        if (msgItem == null) { dbg("msgItem=null holder=" + (holder == null ? "null" : holder.getClass().getSimpleName())); return; }
        String itemCls = msgItem.getClass().getName();
        if (itemCls.contains("GrayTips") || itemCls.contains("SystemMsg")) return;

        View holderView = (View) Refl.fieldByType(holder, View.class);
        if (!(holderView instanceof ViewGroup)) {
            dbg("holderView=" + (holderView == null ? "null" : holderView.getClass().getName()));
            return;
        }
        ViewGroup rootView = (ViewGroup) holderView;

        String text = formatTime(extractMsgTime(msgItem));

        FrameLayout wrap = (FrameLayout) findByTag(rootView, TAG_WRAP);
        if (wrap != null) {
            TextView exist = (TextView) findByTag(wrap, TAG_TIME);
            if (exist == null) return;
            exist.setText(text);
            applyStyle(exist);
            return;
        }

        View bubble = findByClass(rootView, CLS_BUBBLE);
        if (!(bubble instanceof ViewGroup)) {
            dbg("bubble=" + (bubble == null ? "null" : bubble.getClass().getName())
                    + " root=" + rootView.getClass().getSimpleName());
            return;
        }
        ViewGroup bubbleGroup = (ViewGroup) bubble;
        if (bubbleGroup.getChildCount() == 0) return;

        FrameLayout wrapper = new FrameLayout(bubbleGroup.getContext());
        wrapper.setTag(TAG_WRAP);

        LinearLayout content = new LinearLayout(bubbleGroup.getContext());
        content.setOrientation(LinearLayout.VERTICAL);
        while (bubbleGroup.getChildCount() > 0) {
            View c = bubbleGroup.getChildAt(0);
            bubbleGroup.removeViewAt(0);
            content.addView(c);
        }
        wrapper.addView(content, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT));

        TextView tv = new TextView(bubbleGroup.getContext());
        tv.setTag(TAG_TIME);
        tv.setText(text);
        applyStyle(tv);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.RIGHT | Gravity.BOTTOM;
        lp.rightMargin = dp(4);
        lp.bottomMargin = dp(2);
        wrapper.addView(tv, lp);

        bubbleGroup.addView(wrapper, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT));
    }

    private static View findByTag(ViewGroup root, String tag) {
        for (int i = 0; i < root.getChildCount(); i++) {
            View c = root.getChildAt(i);
            if (c == null) continue;
            if (tag.equals(String.valueOf(c.getTag()))) return c;
            if (c instanceof ViewGroup) {
                View r = findByTag((ViewGroup) c, tag);
                if (r != null) return r;
            }
        }
        return null;
    }

    private void applyStyle(TextView tv) {
        try {
            tv.setTextColor(color());
        } catch (Throwable ignore) {
        }
        try {
            tv.setTextSize(size());
        } catch (Throwable ignore) {
        }
    }

    private static View findByClass(ViewGroup root, String className) {
        for (int i = 0; i < root.getChildCount(); i++) {
            View c = root.getChildAt(i);
            if (c == null) continue;
            if (className.equals(c.getClass().getName())) return c;
            if (c instanceof ViewGroup) {
                View r = findByClass((ViewGroup) c, className);
                if (r != null) return r;
            }
        }
        return null;
    }

    private void dbg(String s) {
        try {
            if (ctx == null) return;
            java.io.File f = new java.io.File(ctx.getFilesDir(), "msgtime_debug.txt");
            if (f.length() > 128 * 1024) return;
            java.io.FileOutputStream o = new java.io.FileOutputStream(f, true);
            try {
                String t = new java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US)
                        .format(new java.util.Date());
                o.write((t + " " + s + "\n").getBytes("UTF-8"));
            } finally {
                o.close();
            }
        } catch (Throwable ignore) {
        }
    }

    private static Object pickMsgItem(Object[] args) {
        if (args == null) return null;
        for (Object a : args) {
            if (a == null) continue;
            String n = a.getClass().getName();
            if (n.contains("AIOMsgItem") || n.contains("MsgRecord")) return a;
        }
        return null;
    }

    private static Object pickMsgItemFrom(Object holder) {
        if (holder == null) return null;
        Class<?> c = holder.getClass();
        int dep = 0;
        while (c != null && c != Object.class && dep++ < 6) {
            for (java.lang.reflect.Field f : c.getDeclaredFields()) {
                String tn = f.getType().getName();
                if (!tn.contains("AIOMsgItem") && !tn.contains("MsgRecord")) continue;
                try {
                    f.setAccessible(true);
                    Object v = f.get(holder);
                    if (v != null) return v;
                } catch (Throwable ignore) {
                }
            }
            c = c.getSuperclass();
        }
        return null;
    }

    private static long extractMsgTime(Object msgItem) {
        try {
            Object record = findRecord(msgItem.getClass(), msgItem, 0);
            if (record == null) return 0L;
            Object t = Refl.field(record, "msgTime");
            if (t instanceof Number) return ((Number) t).longValue();

            Object byType = Refl.fieldByType(record, Long.class);
            return byType instanceof Number ? ((Number) byType).longValue() : 0L;
        } catch (Throwable t) {
            return 0L;
        }
    }

    private static Object findRecord(Class<?> cls, Object obj, int depth) {
        if (cls == null || depth > 4) return null;
        for (Field f : cls.getDeclaredFields()) {
            String tn = f.getType().getName();
            if (!tn.endsWith("MsgRecord")) continue;
            try {
                f.setAccessible(true);
                Object v = f.get(obj);
                if (v != null) return v;
            } catch (Throwable ignore) {
            }
        }
        return findRecord(cls.getSuperclass(), obj, depth + 1);
    }

    private String formatTime(long seconds) {
        long ms = seconds <= 0 ? System.currentTimeMillis() : seconds * 1000L;
        String fmt = DEF_FORMAT;
        try {
            fmt = HookConfig.get(ctx, K_FORMAT, DEF_FORMAT);
        } catch (Throwable ignore) {
        }
        if (fmt == null || fmt.trim().length() == 0) fmt = DEF_FORMAT;
        try {
            return new SimpleDateFormat(fmt, Locale.getDefault()).format(new Date(ms));
        } catch (Throwable t) {
            return new SimpleDateFormat(DEF_FORMAT, Locale.getDefault()).format(new Date(ms));
        }
    }

    private int color() {
        try {
            String raw = HookConfig.get(ctx, K_COLOR, DEF_COLOR);
            return (int) Long.parseLong(raw);
        } catch (Throwable t) {
            return 0xFF0000FF;
        }
    }

    private float size() {
        try {
            return Float.parseFloat(HookConfig.get(ctx, K_SIZE, DEF_SIZE));
        } catch (Throwable t) {
            return 10f;
        }
    }

    private int dp(float v) {

        try {
            return com.cathelperawa.owo.app.ui.UiKit.dp(ctx, v);
        } catch (Throwable t) {
            return (int) v;
        }
    }

    @Override
    protected void check(SelfCheckResult.Checker c) {
        Class<?> vb = HookApi.findClass(cl, CLS_VB);
        if (vb == null) {
            c.fail("AIOBubbleMsgItemVB", "类未找到");
            return;
        }
        c.pass("AIOBubbleMsgItemVB", vb.getName());
        Method m = findBind(vb);
        if (m != null) c.pass("bind", "找到 " + m.getName() + "(int,msgItem,List,Bundle)");
        else c.fail("bind", "没找到 4 参 bind 方法");
    }
}
