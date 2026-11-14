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

    public static final String K_VER = "chat_msgtime_version";

    public static final String K_VER_LAYOUT = "chat_msgtime_ver_layout";

    public static final String K_VER_SIZE = "chat_msgtime_ver_size";

    public static final String K_MANUAL = "chat_msgtime_manual";

    private static final java.util.Map<Long, String> MANUAL =
            new java.util.concurrent.ConcurrentHashMap<Long, String>();
    private static boolean manualLoaded = false;

    private static final java.util.Map<View, Boolean> LONG_HOOKED =
            java.util.Collections.synchronizedMap(new java.util.WeakHashMap<View, Boolean>());

    public static final String DEF_COLOR = String.valueOf(0xFF0000FF);
    public static final String DEF_FORMAT = "HH:mm:ss";
    public static final String DEF_SIZE = "10";

    private static boolean pbDumped = false;

    private static final java.util.Map<String, Long> SUB_CACHE =
            java.util.Collections.synchronizedMap(
                    new java.util.LinkedHashMap<String, Long>(512, 0.75f, false) {
                        @Override
                        protected boolean removeEldestEntry(
                                java.util.Map.Entry<String, Long> e) {
                            return size() > 4000;
                        }
                    });
    private static boolean recDumped = false;
    private static boolean diagDone = false;

    private static final String TAG_WRAP = "cathelper_time_wrap";
    private static final String TAG_CONTENT = "cathelper_time_content";
    private static final String TAG_T = "cathelper_time_t";
    private static final String TAG_V = "cathelper_time_v";

    private static final java.util.Map<View, String> BOX_MODE =
            java.util.Collections.synchronizedMap(new java.util.WeakHashMap<View, String>());
    private static final String TAG_TIME = "cathelper_time_text";

    private static final String CLS_VB = "com.tencent.mobileqq.aio.msglist.holder.AIOBubbleMsgItemVB";
    private static final String CLS_BUBBLE = "com.tencent.qqnt.aio.holder.template.BubbleLayoutCompatPress";

    public boolean mainProcessOnly() { return true; }

    @Override

    public String switchKey() {
        return K;
    }

    private void hookMsgPush(ClassLoader cl) {
        try {
            Class<?> h = HookApi.findClass(cl, "mqq.app.msghandle.MsgRespHandler");
            if (h == null) {
                HookLog.i("版本号: 未找到 MsgRespHandler");
                return;
            }
            int n = 0;
            for (final Method m : h.getDeclaredMethods()) {
                if (!"dispatchRespMsg".equals(m.getName())) continue;
                hookBefore(m, new HookApi.Action() {
                    @Override
                    public void call(de.robv.android.xposed.XC_MethodHook.MethodHookParam p) {
                        try {
                            if (!verOn()) return;
                            Object fromMsg = null;
                            Object[] args = p.args;
                            if (args != null && args.length >= 2 && args[1] != null) {
                                fromMsg = Refl.field(args[1], "fromServiceMsg");
                            }
                            if (fromMsg == null && args != null) {
                                for (int i = 0; i < args.length; i++) {
                                    Object a = args[i];
                                    if (a != null
                                            && a.getClass().getName().contains("FromServiceMsg")) {
                                        fromMsg = a;
                                        break;
                                    }
                                }
                            }
                            if (fromMsg == null) return;
                            Object cmdO = Refl.call(fromMsg, "getServiceCmd");
                            String cmd = cmdO instanceof String ? (String) cmdO : null;
                            if (cmd == null || !cmd.contains("MsgPush")) return;
                            Object wb = Refl.call(fromMsg, "getWupBuffer");
                            if (!(wb instanceof byte[])) return;
                            parsePush((byte[]) wb);
                        } catch (Throwable t) {

                        }
                    }
                });
                n++;
            }
            HookLog.i("版本号: MsgPush 钩子 × " + n);
        } catch (Throwable t) {
            HookLog.e("版本号: 挂 MsgPush 失败", t);
        }
    }

    private static void parsePush(byte[] raw) {
        try {
            com.cathelperawa.owo.app.ovo.FunProtoData d =
                    new com.cathelperawa.owo.app.ovo.FunProtoData();
            d.fromBytes(com.cathelperawa.owo.app.ovo.FunProtoData.getUnpPackage(raw));
            org.json.JSONObject j = d.toJSON();
            if (!pbDumped) {
                pbDumped = true;
                String js = j.toString(2);
                HookLog.i("PBDUMP[" + js.length() + "] "
                        + js.substring(0, Math.min(2800, js.length())));
            }
            collect(j);
        } catch (Throwable t) {
            HookLog.e("版本号: 解析推送失败", t);
        }
    }

    private static void collect(org.json.JSONObject j) {
        if (j == null) return;
        try {
            org.json.JSONObject head = j.optJSONObject("1");
            org.json.JSONObject info = j.optJSONObject("2");
            if (head != null && info != null && head.has("4")) {
                emit(head, info);
            }
            org.json.JSONArray names = j.names();
            if (names == null) return;
            for (int i = 0; i < names.length(); i++) {
                Object child = j.opt(names.optString(i));
                if (child instanceof org.json.JSONObject) {
                    collect((org.json.JSONObject) child);
                } else if (child instanceof org.json.JSONArray) {
                    org.json.JSONArray a = (org.json.JSONArray) child;
                    for (int m = 0; m < a.length(); m++) {
                        collect(a.optJSONObject(m));
                    }
                }
            }
        } catch (Throwable ignore) {
        }
    }

    private static void emit(org.json.JSONObject head, org.json.JSONObject info) {
        try {
            long subId = head.optLong("4", -1);
            if (subId <= 0) return;
            long uin = head.optLong("1", -1);
            long random = info.optLong("4", -1);

            long[] seqs = {info.optLong("5", -1), info.optLong("11", -1)};

            if (uin > 0) {
                for (int i = 0; i < seqs.length; i++) {
                    long seq = seqs[i];
                    if (seq <= 0) continue;
                    SUB_CACHE.put(uin + ":" + seq + ":" + random, subId);
                    SUB_CACHE.put(uin + ":" + seq, subId);
                }
            }
        } catch (Throwable ignore) {
        }
    }

    public static Long subIdOf(Object record) {
        try {

            long seq = num(Refl.call(record, "getMsgSeq"));
            long random = num(Refl.call(record, "getMsgRandom"));
            long uin = num(Refl.call(record, "getSenderUin"));

            if (seq <= 0) seq = Refl.fieldLong(record, "msgSeq", 0);
            if (random <= 0) random = Refl.fieldLong(record, "msgRandom", 0);
            if (uin <= 0) uin = Refl.fieldLong(record, "senderUin", 0);

            if (!diagDone) {
                diagDone = true;
                HookLog.i("版本号诊断: seq=" + seq + " random=" + random + " uin=" + uin
                        + " cls=" + record.getClass().getSimpleName()
                        + " 缓存=" + SUB_CACHE.size());
            }

            if (seq <= 0 || uin <= 0) return null;

            Long v = SUB_CACHE.get(uin + ":" + seq + ":" + random);
            if (v == null) v = SUB_CACHE.get(uin + ":" + seq);
            return v;
        } catch (Throwable t) {
            return null;
        }
    }

    private static long num(Object o) {
        return o instanceof Number ? ((Number) o).longValue() : 0L;
    }

    private static String versionOf(Object msgItem) {
        try {
            Object rec = findRecord(msgItem.getClass(), msgItem, 0);
            if (rec == null) return null;

            Long sid = subIdOf(rec);
            if (sid == null || sid <= 0) return null;
            loadManual();
            String manual = MANUAL.get(sid);
            if (manual != null && manual.length() > 0) return manual;
            FeatureSenderVersion.loadTableStatic();
            String v = FeatureSenderVersion.lookup(sid);
            return (v == null || v.length() == 0) ? null : v;
        } catch (Throwable t) {
            return null;
        }
    }

    private boolean verOn() {
        try {
            return ctx != null && HookConfig.getBoolean(ctx, K_VER, false);
        } catch (Throwable t) {
            return false;
        }
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
        hookMsgPush(cl);

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

        String timeText = formatTime(extractMsgTime(msgItem));
        String verText = verOn() ? versionOf(msgItem) : null;

        ViewGroup wrap = (ViewGroup) findByTag(rootView, TAG_WRAP);
        if (wrap != null) {
            LinearLayout box = (LinearLayout) findByTag(wrap, TAG_TIME);
            String mode = layoutMode();
            String want = mode + "|" + (verText == null ? "0" : "1");
            if (box != null && want.equals(BOX_MODE.get(box))) {
                TextView t = (TextView) findByTag(box, TAG_T);
                TextView v = (TextView) findByTag(box, TAG_V);
                if (t != null) {
                    t.setText(timeText);
                    applyStyle(t, false);
                }
                if (v != null) {
                    boolean show = verText != null && verText.length() > 0;
                    v.setVisibility(show ? View.VISIBLE : View.GONE);
                    if (show) {
                        v.setText("inline".equals(mode) ? "· " + verText : verText);
                        applyStyle(v, true);
                    }
                }
                return;
            }

            if (box != null) wrap.removeView(box);
            fill((LinearLayout) wrap, timeText, verText);
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

        LinearLayout wrapper = new LinearLayout(bubbleGroup.getContext());
        wrapper.setOrientation(LinearLayout.VERTICAL);
        wrapper.setTag(TAG_WRAP);

        LinearLayout content = new LinearLayout(bubbleGroup.getContext());
        content.setOrientation(LinearLayout.VERTICAL);
        content.setTag(TAG_CONTENT);
        while (bubbleGroup.getChildCount() > 0) {
            View c = bubbleGroup.getChildAt(0);
            bubbleGroup.removeViewAt(0);
            content.addView(c);
        }
        wrapper.addView(content, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        fill(wrapper, timeText, verText);

        bubbleGroup.addView(wrapper, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT));

        try {
            if (manualOn()) bindManual(bubbleGroup, msgItem);
        } catch (Throwable ignore) {
        }
    }

    public static boolean manualOnStatic(android.content.Context c) {
        try {
            return c != null && HookConfig.getBoolean(c, K_MANUAL, false);
        } catch (Throwable t) {
            return false;
        }
    }

    private boolean manualOn() {
        try {
            return ctx != null && HookConfig.getBoolean(ctx, K_MANUAL, false);
        } catch (Throwable t) {
            return false;
        }
    }

    private void bindManual(final View bubble, final Object msgItem) {
        if (LONG_HOOKED.containsKey(bubble)) return;
        LONG_HOOKED.put(bubble, Boolean.TRUE);

        bubble.setOnTouchListener(new View.OnTouchListener() {
            private long downAt;
            private float dx, dy;
            private Runnable pending;

            private void cancel(View v) {
                if (pending != null) {
                    v.removeCallbacks(pending);
                    pending = null;
                }
            }

            @Override
            public boolean onTouch(final View v, android.view.MotionEvent e) {
                int a = e.getActionMasked();
                if (a == android.view.MotionEvent.ACTION_DOWN) {
                    downAt = android.os.SystemClock.uptimeMillis();
                    dx = e.getX();
                    dy = e.getY();
                    cancel(v);
                    pending = new Runnable() {
                        @Override
                        public void run() {
                            pending = null;
                            try {
                                showManualDialog(v.getContext(), msgItem);
                            } catch (Throwable t) {
                                HookLog.e("版本标注: 弹窗失败", t);
                            }
                        }
                    };
                    v.postDelayed(pending, 620);
                } else if (a == android.view.MotionEvent.ACTION_MOVE) {
                    if (Math.abs(e.getX() - dx) > 34 || Math.abs(e.getY() - dy) > 34) cancel(v);
                } else {
                    cancel(v);
                }
                return false;
            }
        });
    }

    public static void showManualDialogPublic(android.content.Context c, Object msgItem) {
        new FeatureMsgTime().showManualDialog(c, msgItem);
    }

    private void showManualDialog(final android.content.Context c, final Object msgItem) {
        Object rec = findRecord(msgItem.getClass(), msgItem, 0);
        final Long sid = rec == null ? null : subIdOf(rec);
        String note = (sid == null)
                ? "这条消息没抓到 SubId，标不了（可以先在聊天里翻一下再看）"
                : "SubId " + sid + " · 保存后同一个 SubId 的消息都会显示你写的内容";
        com.cathelperawa.owo.app.ui.CatSheet.inputPair(c, "标注发送者版本",
                "QQ 版本，例如 9.3.80.42775", "渠道，例如 小米（可留空）", note,
                new com.cathelperawa.owo.app.ui.CatSheet.OnPair() {
                    @Override public void run(String ver, String ch) {
                        try {
                            if (sid == null || ver == null || ver.length() == 0) return;
                            String label = "QQ " + ver
                                    + (ch != null && ch.length() > 0 ? "（" + ch + "渠道）" : "");
                            MANUAL.put(sid, label);
                            saveManual();
                            com.cathelperawa.owo.app.bridge.CatToast.show("已标注：" + label);
                        } catch (Throwable ignore) {
                        }
                    }
                });
    }

    private static LinearLayout.LayoutParams lpB(int bottomMargin) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        p.bottomMargin = bottomMargin;
        return p;
    }

    private static java.io.File manualFile() {
        try {
            return new java.io.File(com.cathelperawa.owo.app.bridge.QQEnv
                    .appContext().getFilesDir(), "msgtime_manual.json");
        } catch (Throwable t) {
            return null;
        }
    }

    private static void loadManual() {
        if (manualLoaded) return;
        manualLoaded = true;
        try {
            java.io.File f = manualFile();
            if (f == null || !f.exists()) return;
            java.io.FileInputStream in = new java.io.FileInputStream(f);
            java.io.ByteArrayOutputStream bo = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) > 0) bo.write(buf, 0, n);
            in.close();
            org.json.JSONObject j = new org.json.JSONObject(bo.toString("UTF-8"));
            org.json.JSONArray ks = j.names();
            if (ks == null) return;
            for (int i = 0; i < ks.length(); i++) {
                String k = ks.optString(i);
                try {
                    MANUAL.put(Long.parseLong(k), j.optString(k, ""));
                } catch (Throwable ignore) {
                }
            }
        } catch (Throwable ignore) {
        }
    }

    private static void saveManual() {
        try {
            java.io.File f = manualFile();
            if (f == null) return;
            org.json.JSONObject j = new org.json.JSONObject();
            for (Long k : MANUAL.keySet()) j.put(String.valueOf(k), MANUAL.get(k));
            java.io.FileOutputStream o = new java.io.FileOutputStream(f, false);
            o.write(j.toString().getBytes("UTF-8"));
            o.close();
        } catch (Throwable ignore) {
        }
    }

    private void fill(LinearLayout content, String timeText, String verText) {
        android.content.Context c = content.getContext();
        String mode = layoutMode();
        boolean hasVer = verText != null && verText.length() > 0;
        boolean stack = hasVer && !"inline".equals(mode);

        LinearLayout box = new LinearLayout(c);
        box.setTag(TAG_TIME);
        box.setOrientation(stack ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
        box.setGravity(stack ? Gravity.END : Gravity.CENTER_VERTICAL);

        TextView t = new TextView(c);
        t.setTag(TAG_T);
        t.setText(timeText);
        applyStyle(t, false);

        TextView v = new TextView(c);
        v.setTag(TAG_V);
        if (hasVer) {
            v.setText(stack ? verText : "· " + verText);
            applyStyle(v, true);
        } else {
            v.setVisibility(View.GONE);
        }

        LinearLayout.LayoutParams lp1 = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        if (stack) {
            lp1.gravity = Gravity.END;
            lp2.gravity = Gravity.END;
        }

        if (stack && "ver_top".equals(mode)) {
            box.addView(v, lp2);
            box.addView(t, lp1);
        } else {
            box.addView(t, lp1);
            box.addView(v, lp2);
        }
        BOX_MODE.put(box, mode + "|" + (hasVer ? "1" : "0"));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.END;
        lp.topMargin = dp(2);
        content.addView(box, lp);
    }

    private static String layoutMode() {
        try {
            String m = com.cathelperawa.owo.app.core.HookConfig.get(
                    com.cathelperawa.owo.app.bridge.QQEnv.appContext(), K_VER_LAYOUT, "inline");
            return m == null || m.length() == 0 ? "inline" : m;
        } catch (Throwable t) {
            return "inline";
        }
    }

    private float verSize() {
        try {
            String v = com.cathelperawa.owo.app.core.HookConfig.get(
                    ctx, K_VER_SIZE, "");
            if (v != null && v.length() > 0) return Float.parseFloat(v);
        } catch (Throwable ignore) {
        }
        return size();
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

    private void applyStyle(TextView tv, boolean isVer) {
        try {
            tv.setTextColor(color());
        } catch (Throwable ignore) {
        }
        try {
            tv.setTextSize(isVer ? verSize() : size());
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
