package com.cathelperawa.owo.app.feature;

import com.cathelperawa.owo.app.core.LoadPriority;
import com.cathelperawa.owo.app.core.Bg;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.cathelperawa.owo.app.MsgStore;
import com.cathelperawa.owo.app.R;
import com.cathelperawa.owo.app.bridge.QQEnv;
import com.cathelperawa.owo.app.core.HookApi;
import com.cathelperawa.owo.app.core.HookItem;
import com.cathelperawa.owo.app.core.HookItemBase;
import com.cathelperawa.owo.app.core.HookLog;
import com.cathelperawa.owo.app.protocol.MsgFetcher;
import com.cathelperawa.owo.app.util.Refl;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

@HookItem(name = "长按提取", path = "开发者/长按提取", description = "长按消息提取原始协议数据", order = 40, hosts = {"tim", "qq"})
public class FeatureMsgExtract extends HookItemBase {

    public boolean mainProcessOnly() { return true; }

    @Override

    public String switchKey() { return "hook_capture"; }

    private boolean repeatEnabled() {
        return com.cathelperawa.owo.app.core.HookConfig.getBoolean(ctx, "chat_repeat", true);
    }

    private boolean extractEnabled() {
        return com.cathelperawa.owo.app.core.HookConfig.getBoolean(ctx, "hook_capture", true);
    }

    private boolean redpacketEnabled() {
        return com.cathelperawa.owo.app.feature.FeatureRedPacket.manualOn(ctx);
    }

    private boolean anyMenuOn() {
        return repeatEnabled() || extractEnabled() || redpacketEnabled();
    }

    private static final String TITLE = "提取";

    private static final String TITLE_REPEAT = "复读";

    private static final String TITLE_REDPACKET = com.cathelperawa.owo.app.feature.FeatureRedPacket.TITLE;

    private static final String[] OUR_TITLES = {TITLE, TITLE_REPEAT, TITLE_REDPACKET};

    private static Context hostCtx;
    private static ClassLoader hostCl;

    private static final Map<Object, Object[]> OUR = new IdentityHashMap<Object, Object[]>();

    private static final java.util.ArrayDeque<Object> OUR_ORDER = new java.util.ArrayDeque<Object>();
    private static final int OUR_MAX = 40;

    private static volatile String lastResult = null;
    private static volatile long lastCapture = 0;

    private static volatile String lastCaptureKey = null;

    private static volatile String lastPeerUid = null;
    private static volatile int lastChatType = 0;

    private static volatile String lastElements = null;

    private static volatile Object lastRecord = null;

    public static Object lastRecord() { return lastRecord; }

    public static String result() {
        return lastResult;
    }

    @Override
    public void entry(ClassLoader cl) {
        hint("长按消息弹出菜单时");
        hostCtx = ctx;
        hostCl = cl;
        hookSetMenu(cl);
        hookRender(cl);
    }

    @Override
    protected void check(com.cathelperawa.owo.app.core.SelfCheckResult.Checker c) {
        Class<?> lay = c.cls("菜单布局类",
                "com.tencent.qqnt.aio.menu.ui.QQCustomMenuExpandableLayout",
                "com.tencent.qqnt.aio.menu.ui.QQCustomMenuNoIconLayout");
        c.method("setMenu", lay, "setMenu");
    }

    private void hookSetMenu(ClassLoader cl) {
        Class<?> expand = HookApi.findClass(cl,
                "com.tencent.qqnt.aio.menu.ui.QQCustomMenuExpandableLayout");
        if (expand == null) {
            fail("未找到 QQCustomMenuExpandableLayout(ui)");
            return;
        }
        Method setMenu = HookApi.findMethod(expand, "setMenu");
        if (setMenu == null) {
            fail("未找到 setMenu 方法");
            return;
        }
        final Method target = setMenu;
        hookBefore(target, new HookApi.Action() {
            @Override
            public void call(de.robv.android.xposed.XC_MethodHook.MethodHookParam p) {
                fire("弹出菜单");
                onMenu(p);
            }
        });
        HookLog.i("已挂 setMenu ");
    }

    private void onMenu(de.robv.android.xposed.XC_MethodHook.MethodHookParam param) {
        try {
            if (!anyMenuOn()) return;
            Object uiE = param.args[0];
            if (uiE == null) return;
            List<Object> items = findList(uiE);
            if (items == null || items.isEmpty()) return;

            if (com.cathelperawa.owo.app.core.Host.isQQ()) {
                dumpMenuOnce(items);
            }

            for (Object o : items) {
                if (o == null) continue;
                String t = titleOf(o);
                for (String want : OUR_TITLES) {
                    if (want.equals(t)) return;
                }
            }
            Object aioMsg = findMsg(items.get(0));
            if (aioMsg == null) return;

            Context c = hostCtx;
            if (param.thisObject instanceof View) {
                Context c2 = ((View) param.thisObject).getContext();
                if (c2 != null) c = c2;
            }

            Class<?> menuD = HookApi.findClassCached(hostCl, "com.tencent.qqnt.aio.menu.d");

            if (repeatEnabled()) insertItem(menuD, c, aioMsg, items, TITLE_REPEAT);
            if (extractEnabled()) insertItem(menuD, c, aioMsg, items, TITLE);

            if (redpacketEnabled() && isRedPacketMsg(aioMsg)) {
                insertItem(menuD, c, aioMsg, items, TITLE_REDPACKET);
            }
            HookLog.i("已插入菜单项，共 " + items.size() + " 项");
        } catch (Throwable t) {
            HookLog.e("onMenu 异常", t);
        }
    }

    private static volatile boolean menuDumped = false;

    private void dumpMenuOnce(List<Object> items) {
        if (menuDumped) return;
        menuDumped = true;
        StringBuilder sb = new StringBuilder();
        try {
            Object it0 = items.get(0);
            Class<?> ic = it0.getClass();
            sb.append("元素类 = ").append(ic.getName())
              .append(" 共 ").append(items.size()).append(" 项\n");
            Class<?> sup = ic;
            int dep = 0;
            while (sup != null && sup != Object.class && dep++ < 4) {
                sb.append("[类] ").append(sup.getName()).append('\n');
                for (java.lang.reflect.Constructor<?> ct : sup.getDeclaredConstructors()) {
                    sb.append("  ctor ").append(ct.toGenericString()).append('\n');
                }
                for (java.lang.reflect.Field f : sup.getDeclaredFields()) {
                    sb.append("  f ").append(f.getType().getSimpleName())
                      .append(' ').append(f.getName()).append('\n');
                }
                sup = sup.getSuperclass();
            }
            for (Class<?> i : ic.getInterfaces()) {
                sb.append("implements ").append(i.getName()).append('\n');
            }
            Class<?> uiE = HookApi.findClass(hostCl, "com.tencent.qqnt.aio.menu.ui.e");
            if (uiE != null) {
                sb.append("ui.e 存在，抽象=")
                  .append(java.lang.reflect.Modifier.isAbstract(uiE.getModifiers()))
                  .append(" 接口=").append(uiE.isInterface()).append('\n');
                for (java.lang.reflect.Method m : uiE.getDeclaredMethods()) {
                    sb.append("  ui.e# ").append(m.getName()).append('\n');
                }
            } else {
                sb.append("ui.e 不存在\n");
            }
            sb.append("title(0)=").append(titleOf(it0)).append('\n');

            try {
                for (java.lang.reflect.Field f : items.getClass().getDeclaredFields()) {
                    sb.append("list field ").append(f.getName()).append('\n');
                }
            } catch (Throwable ignore) {}
        } catch (Throwable t) {
            sb.append("探测失败 ").append(t).append('\n');
        }
        String txt = sb.toString();
        HookLog.i("QQ菜单探测\n" + txt);
        try {
            java.io.File f = new java.io.File(ctx.getFilesDir(), "qqmenu.txt");
            java.io.FileOutputStream out = new java.io.FileOutputStream(f, false);
            out.write(txt.getBytes("UTF-8"));
            out.close();
        } catch (Throwable t) {
            HookLog.w("QQ菜单探测落盘失败 " + t);
        }
    }

    private boolean isRedPacketMsg(Object aioMsg) {
        try {
            Object rec = aioMsg;
            try {
                Method g = aioMsg.getClass().getMethod("getMsgRecord");
                g.setAccessible(true);
                rec = g.invoke(aioMsg);
            } catch (Throwable ignore) {}
            return com.cathelperawa.owo.app.feature.FeatureRedPacket.walletOf(rec) != null;
        } catch (Throwable t) {
            return false;
        }
    }

    private void insertItem(Class<?> menuD, Context c, Object aioMsg, List<Object> items, String title) {
        try {

            Object item = com.cathelperawa.owo.app.core.Host.isQQ()
                    ? null : buildItem(menuD, c, aioMsg);
            if (item == null) item = cloneItem(items.get(0));
            if (item == null) return;
            if (menuD != null) {
                setFieldByType(item, boolean.class, false);
                setFieldByType(item, int.class, 0);
            }
            setFieldByType(item, String.class, title);
            synchronized (OUR) {
                OUR.put(item, new Object[]{aioMsg, title});
                OUR_ORDER.addLast(item);
                while (OUR_ORDER.size() > OUR_MAX) {
                    Object old = OUR_ORDER.pollFirst();
                    if (old != null) OUR.remove(old);
                }
            }
            items.add(0, item);
        } catch (Throwable t) {
            HookLog.e("插入菜单项失败: " + title, t);
        }
    }

    private void hookRender(ClassLoader cl) {
        String[] hosts = new String[]{
                "com.tencent.qqnt.aio.menu.ui.QQCustomMenuNoIconLayout",
                "com.tencent.qqnt.aio.menu.ui.QQCustomMenuExpandableLayout",
        };
        int hooked = 0;
        for (String hn : hosts) {
            Class<?> c = HookApi.findClass(cl, hn);
            if (c == null) continue;
            for (final Method m : c.getDeclaredMethods()) {
                Class<?>[] p = m.getParameterTypes();
                if (p.length != 4) continue;
                if (p[0] != int.class || p[2] != boolean.class || p[3] != float[].class) continue;
                if (!View.class.isAssignableFrom(m.getReturnType())) continue;
                hookBefore(m, new HookApi.Action() {
                    @Override
                    public void call(de.robv.android.xposed.XC_MethodHook.MethodHookParam param) {
                        try {
                            Object f = param.args[1];
                            if (f == null) return;
                            Object[] rec;
                            synchronized (OUR) {
                                rec = OUR.get(f);
                            }
                            if (rec == null) return;

                            String title = (String) rec[1];
                            if (TITLE_REPEAT.equals(title) && !repeatEnabled()) return;
                            if (TITLE.equals(title) && !extractEnabled()) return;
                            if (TITLE_REDPACKET.equals(title) && !redpacketEnabled()) return;
                            Context c = hostCtx;
                            if (param.thisObject instanceof View) {
                                c = ((View) param.thisObject).getContext();
                            }
                            param.setResult(makeOurView(c, rec[0], title));
                        } catch (Throwable t) {
                            HookLog.e("渲染替换失败", t);
                        }
                    }
                });
                hooked++;
            }
        }
        HookLog.i("渲染方法挂了 " + hooked + " 个");
    }

    private View makeOurView(final Context ctx, final Object msg, final String title) {
        float d = ctx.getResources().getDisplayMetrics().density;
        LinearLayout box = new LinearLayout(ctx);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setPadding((int) (6 * d), (int) (6 * d), (int) (6 * d), (int) (6 * d));

        ImageView iv = new ImageView(ctx);
        try {
            Drawable dr = com.cathelperawa.owo.app.util.ModIcon.ic(ctx);
            if (dr != null) iv.setImageDrawable(dr);
        } catch (Throwable ignore) {
        }
        box.addView(iv, new LinearLayout.LayoutParams((int) (26 * d), (int) (26 * d)));

        TextView tv = new TextView(ctx);
        tv.setText(title == null ? TITLE : title);
        tv.setTextColor(0xFF9AA7B8);
        tv.setTextSize(11);
        tv.setSingleLine(true);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        tp.topMargin = (int) (3 * d);
        box.addView(tv, tp);

        box.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (TITLE_REPEAT.equals(title)) {
                    doRepeat(msg);
                } else if (TITLE_REDPACKET.equals(title)) {
                    com.cathelperawa.owo.app.feature.FeatureRedPacket.grabFromMsgItem(msg);
                } else {
                    doCapture(msg);
                }
            }
        });
        box.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        return box;
    }

    private void doRepeat(Object msgItem) {
        final Object record = recordOf(msgItem);
        if (record == null) {
            MsgStore.add("【复读】没取到 MsgRecord");
            toast2("没拿到消息");
            return;
        }
        final int chatType = Refl.i32(Refl.field(record, "chatType"), 0);
        final String peerUid = Refl.strOr(Refl.field(record, "peerUid"),
                Refl.strOr(Refl.field(record, "peerUin"), ""));
        MsgStore.add("【复读】开始 chatType=" + chatType + " peer=" + peerUid);
        Bg.run(new Runnable() {
            @Override public void run() {
                long t0 = System.currentTimeMillis();
                final com.cathelperawa.owo.app.proto.RepeatCore.Outcome oc =
                        com.cathelperawa.owo.app.proto.RepeatCore.repeat(record, chatType, peerUid);
                long cost = System.currentTimeMillis() - t0;

                MsgStore.add("【复读】" + oc + " · 耗时 " + cost + "ms");
                toast2(oc.ok ? "复读成功喵～" : "复读失败：" + oc.note);
            }
        }, "cat-repeat");
    }

    private static void toast2(final String msg) {
        try {
            com.cathelperawa.owo.app.bridge.CatToast.show(msg);
        } catch (Throwable ignore) {
        }
    }

    private void doCapture(Object msgItem) {
        try {
            Object record = null;
            if (msgItem != null) {
                try {
                    Method g = msgItem.getClass().getMethod("getMsgRecord");
                    g.setAccessible(true);
                    record = g.invoke(msgItem);
                } catch (Throwable t) {
                    record = deepFind(msgItem, "MsgRecord");
                }
            }
            if (record == null) {
                MsgStore.add("【提取】没取到 MsgRecord");
                return;
            }

            long now = System.currentTimeMillis();
            String key = msgKeyOf(record);
            if (key != null) {
                if (key.equals(lastCaptureKey) && now - lastCapture < 1200L) return;
            } else if (now - lastCapture < 800L) {
                return;
            }
            lastCaptureKey = key;
            lastCapture = now;
            startExtractStatic(record);
        } catch (Throwable t) {
            HookLog.e("提取失败", t);
            MsgStore.add("【提取】失败 " + t);
        }
    }

    private static String msgKeyOf(Object record) {
        try {
            for (String f : new String[]{"msgId", "msgSeq", "clientSeq", "uniseq"}) {
                Object v = Refl.field(record, f);
                if (v != null) {
                    String s = String.valueOf(v);
                    if (s.length() > 0 && !"0".equals(s)) return f + ":" + s;
                }
            }
            Object el = Refl.call(record, "getMsgId");
            if (el != null) return "id:" + el;
        } catch (Throwable ignore) {
        }
        return null;
    }

    private static void startExtractStatic(final Object record) {
        lastRecord = record;
        final long msgId = Refl.lng(Refl.field(record, "msgId"), 0);
        final int chatType = Refl.i32(Refl.field(record, "chatType"), 0);

        lastPeerUid = Refl.strOr(Refl.field(record, "peerUid"),
                Refl.strOr(Refl.field(record, "peerUin"), ""));
        lastChatType = chatType;

        final String wantCmd = (chatType == 1)
                ? "MessageSvc.PbGetOneDayRoamMsg"
                : (chatType == 2 ? "MessageSvc.PbGetGroupMsg" : null);

        final String javaDump = com.cathelperawa.owo.app.MsgDump.dump(record);
        android.os.Handler h = new android.os.Handler(android.os.Looper.getMainLooper());
        h.post(new Runnable() {
            @Override
            public void run() {
                showPanel(javaDump, null, record);
            }
        });

        final String ticket = FeatureMsfTap.expect(wantCmd, new FeatureMsfTap.Listener() {
            @Override
            public void onResp(String cmd, org.json.JSONObject content) {
                Object els = FeatureMsfTap.extractElements(content);
                final String text = (els != null) ? prettyJson(els) : prettyJson(content);
                lastElements = (els != null) ? text : null;
                lastResult = text;
                MsgStore.add("[提取·响应] cmd=" + cmd);
                saveLast(text);
                updateCardTab(text);
            }
        });

        String cmd;
        if (chatType == 1) {
            cmd = MsgFetcher.pullC2C(record);
        } else if (chatType == 2) {
            cmd = MsgFetcher.pullGroup(record);
        } else {
            cmd = null;
        }
        if (cmd == null) {
            FeatureMsfTap.cancel(ticket);
        } else {
            MsgStore.add("【提取】已发拉取 " + cmd + "，等响应…");
        }
    }

    private static void saveLast(String text) {
        try {
            android.content.Context c = com.cathelperawa.owo.app.bridge.QQEnv.topActivity();
            if (c == null || text == null) return;
            java.io.File f = new java.io.File(c.getFilesDir(), "extract_last.json");
            java.io.FileOutputStream o = new java.io.FileOutputStream(f, false);
            try {
                o.write(text.getBytes("UTF-8"));
            } finally {
                o.close();
            }
        } catch (Throwable ignore) {
        }
    }

    public interface CardSink { void set(String text); }

    private static volatile CardSink openCardSink;

    public static void updateCardTab(final String text) {
        final CardSink s = openCardSink;
        if (s == null) return;
        new android.os.Handler(android.os.Looper.getMainLooper()).post(new Runnable() {
            @Override public void run() { s.set(text); }
        });
    }

    public static void showExtract(Object record) {
        if (record == null) return;
        startExtractStatic(record);
    }

    public static String fetchViaKernel(Object record, long msgId) {
        try {
            Object svc = QQEnv.kernelMsgService();
            if (svc == null) return null;
            ClassLoader cl = record.getClass().getClassLoader();
            Object peerUid = Refl.field(record, "peerUid");
            int chatType = Refl.i32(Refl.field(record, "chatType"), 2);
            Object contact = com.cathelperawa.owo.app.bridge.ContactFactory.make(chatType, String.valueOf(peerUid), "");
            if (contact == null) return null;

            Method target = null;
            for (Method m : svc.getClass().getMethods()) {
                if (!"getMsgsByMsgId".equals(m.getName())) continue;
                if (m.getParameterTypes().length == 3) {
                    target = m;
                    break;
                }
            }
            if (target == null) return null;

            Class<?> cbCls = target.getParameterTypes()[2];
            final Object[] holder = new Object[1];
            Object cb = java.lang.reflect.Proxy.newProxyInstance(cl,
                    new Class<?>[]{cbCls}, new java.lang.reflect.InvocationHandler() {
                        @Override
                        public Object invoke(Object proxy, Method method, Object[] a) {
                            if ("onResult".equals(method.getName()) && a != null && a.length > 0) {
                                holder[0] = a[0];
                            }
                            Class<?> rt = method.getReturnType();
                            if (rt == boolean.class) return Boolean.FALSE;
                            if (rt == int.class) return 0;
                            if (rt == long.class) return 0L;
                            return null;
                        }
                    });
            java.util.ArrayList<Long> ids = new java.util.ArrayList<Long>();
            ids.add(msgId);
            target.setAccessible(true);
            target.invoke(svc, contact, ids, cb);
            for (int i = 0; i < 100 && holder[0] == null; i++) Thread.sleep(10);
            if (holder[0] == null) return null;

            Object r = holder[0];
            StringBuilder sb = new StringBuilder("// getMsgsByMsgId 返回 ")
                    .append(r.getClass().getSimpleName()).append("\n");
            if (r instanceof List) {
                List<?> l = (List<?>) r;
                sb.append("// 条数: ").append(l.size()).append("\n");
                for (Object o : l) sb.append(com.cathelperawa.owo.app.MsgDump.dump(o)).append("\n");
            } else {
                sb.append(com.cathelperawa.owo.app.MsgDump.dump(r));
            }
            return sb.toString();
        } catch (Throwable t) {
            HookLog.e("内核查询失败", t);
            return null;
        }
    }

    public static String prettyJson(Object o) {
        if (o == null) return null;
        try {
            if (o instanceof org.json.JSONObject) {
                return ((org.json.JSONObject) o).toString(2);
            }
            if (o instanceof org.json.JSONArray) {
                return ((org.json.JSONArray) o).toString(2);
            }

            String s = String.valueOf(o).trim();
            if (s.startsWith("{") || s.startsWith("[")) {
                Object parsed = new org.json.JSONTokener(s).nextValue();
                if (parsed instanceof org.json.JSONObject) {
                    return ((org.json.JSONObject) parsed).toString(2);
                }
                if (parsed instanceof org.json.JSONArray) {
                    return ((org.json.JSONArray) parsed).toString(2);
                }
            }
            return s;
        } catch (Throwable t) {
            return String.valueOf(o);
        }
    }

    public static String extractCardJson(Object record) {
        try {
            if (record == null) return null;
            Object elsObj = Refl.field(record, "elements");
            if (!(elsObj instanceof java.util.List)) return null;
            for (Object el : (java.util.List<?>) elsObj) {
                Object ark = Refl.field(el, "arkElement");

                if (ark == null) {
                    for (Field f : el.getClass().getDeclaredFields()) {
                        String fn = f.getName().toLowerCase();
                        if (!fn.contains("ark")) continue;
                        try {
                            f.setAccessible(true);
                            Object v = f.get(el);
                            if (v != null) { ark = v; break; }
                        } catch (Throwable ignore) {
                        }
                    }
                }
                if (ark == null) continue;
                for (String fn : new String[]{"bytesData", "data", "bytes", "json", "content"}) {
                    Object v = Refl.field(ark, fn);
                    if (v instanceof String && ((String) v).trim().length() > 0) {
                        return ((String) v).trim();
                    }
                    if (v instanceof byte[]) {
                        try { return new String((byte[]) v, "UTF-8"); } catch (Throwable ignore) {}
                    }
                }
            }
        } catch (Throwable t) {
            HookLog.e("extractCardJson 失败", t);
        }

        try {
            if (com.cathelperawa.owo.app.core.Host.isQQ()) {
                android.content.Context c0 =
                        com.cathelperawa.owo.app.bridge.QQEnv.appContext();
                if (c0 != null) {
                    String dump = com.cathelperawa.owo.app.MsgDump.dump(record);
                    java.io.File f = new java.io.File(c0.getFilesDir(), "qqcard.txt");
                    java.io.FileOutputStream o = new java.io.FileOutputStream(f, false);
                    o.write(dump.getBytes("UTF-8"));
                    o.close();
                    HookLog.i("QQ 卡片探测已落盘 " + dump.length() + " 字符");
                }
            }
        } catch (Throwable ignore) {
        }
        return null;
    }

    private static Object recordOf(Object msgItem) {
        if (msgItem == null) return null;
        try {
            Method g = msgItem.getClass().getMethod("getMsgRecord");
            g.setAccessible(true);
            return g.invoke(msgItem);
        } catch (Throwable t) {
            return deepFind(msgItem, "MsgRecord");
        }
    }

    private static String currentTextOf(Object record) {
        Object els = Refl.field(record, "elements");
        if (!(els instanceof java.util.List)) return "";
        for (Object el : (java.util.List<?>) els) {
            Object te = Refl.field(el, "textElement");
            if (te != null) {
                Object c = Refl.field(te, "content");
                if (c instanceof String) return (String) c;
            }
        }
        return "";
    }

    private static String shorten(String s) {
        return s.length() > 60 ? s.substring(0, 60) + "…" : s;
    }

    private static String firstString(org.json.JSONObject o) {
        java.util.Iterator<String> it = o.keys();
        while (it.hasNext()) {
            Object v = o.opt(it.next());
            if (v instanceof String && ((String) v).length() > 0) return (String) v;
        }
        return null;
    }

    private static String numberOrString(String v) {
        if (v == null) return "0";
        if (v.matches("-?\\d+")) return v;
        return "\"" + v.replace("\"", "\\\"") + "\"";
    }

    private static void showPanel(final String javaText) {
        showPanel(javaText, null, null);
    }

    private static String dumpMeta(Object rec) {
        if (rec == null) return "（没有消息对象。多半是「卡片消息」走的是协议 JSON，\n这条消息没落到 Java 对象上）";
        StringBuilder sb = new StringBuilder();
        Class<?> c = rec.getClass();
        sb.append("对象: ").append(c.getName()).append("\n");
        sb.append("──────────────────\n【字段】\n");
        for (Class<?> k = c; k != null && k != Object.class; k = k.getSuperclass()) {
            for (java.lang.reflect.Field f : k.getDeclaredFields()) {
                if (java.lang.reflect.Modifier.isStatic(f.getModifiers())) continue;
                try {
                    f.setAccessible(true);
                    sb.append(f.getName()).append(" = ")
                            .append(fmtVal(f.get(rec))).append("\n");
                } catch (Throwable ignore) {
                }
            }
        }
        sb.append("──────────────────\n【getter】\n");
        for (java.lang.reflect.Method m : c.getMethods()) {
            try {
                if (m.getParameterTypes().length != 0) continue;
                if (m.getDeclaringClass() == Object.class) continue;
                String n = m.getName();
                if (!n.startsWith("get") && !n.startsWith("is")) continue;
                if ("getClass".equals(n)) continue;
                sb.append(n).append("() = ")
                        .append(fmtVal(m.invoke(rec))).append("\n");
            } catch (Throwable ignore) {
            }
        }
        return sb.toString();
    }

    private static String fmtVal(Object v) {
        return fmtDeep(v, 0);
    }

    private static String fmtDeep(Object v, int depth) {
        if (v == null) return "null";
        if (depth > 3) return "…(深)";
        if (v instanceof byte[]) {
            byte[] b = (byte[]) v;
            if (b.length == 0) return "byte[0] (空)";
            int n = Math.min(b.length, 128);
            StringBuilder h = new StringBuilder("byte[" + b.length + "] ");
            for (int i = 0; i < n; i++) {
                h.append(String.format("%02x", b[i]));
                if (i < n - 1 && (i + 1) % 32 == 0) h.append("\n    ");
            }
            if (b.length > n) h.append("…");
            return h.toString();
        }
        if (v instanceof Object[]) {
            Object[] a = (Object[]) v;
            if (a.length == 0) return "[]";
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < a.length; i++) {
                if (i > 0) sb.append(", ");
                sb.append("\n  ").append(fmtDeep(a[i], depth + 1));
            }
            return sb.append("\n]").toString();
        }
        if (v instanceof java.util.Collection) {
            java.util.Collection<?> col = (java.util.Collection<?>) v;
            if (col.isEmpty()) return "[]";
            StringBuilder sb = new StringBuilder("[");
            for (Object o : col) sb.append("\n  ").append(fmtDeep(o, depth + 1));
            return sb.append("\n]").toString();
        }
        if (v instanceof java.util.Map) {
            java.util.Map<?, ?> mp = (java.util.Map<?, ?>) v;
            if (mp.isEmpty()) return "{}";
            StringBuilder sb = new StringBuilder("{");
            for (java.util.Map.Entry<?, ?> e : mp.entrySet()) {
                sb.append("\n  ").append(e.getKey()).append(" = ")
                        .append(fmtDeep(e.getValue(), depth + 1));
            }
            return sb.append("\n}").toString();
        }
        String cls = v.getClass().getName();
        if (cls.startsWith("java.") || cls.startsWith("android.")
                || cls.startsWith("kotlin.")) {
            String s = String.valueOf(v);
            return s.length() > 200 ? s.substring(0, 200) + "…" : s;
        }

        StringBuilder sb = new StringBuilder(cls.substring(cls.lastIndexOf('.') + 1)).append("{");
        for (Class<?> k = v.getClass(); k != null && k != Object.class; k = k.getSuperclass()) {
            for (java.lang.reflect.Field f : k.getDeclaredFields()) {
                try {
                    if (java.lang.reflect.Modifier.isStatic(f.getModifiers())) continue;
                    f.setAccessible(true);
                    Object fv = f.get(v);
                    sb.append("\n    ").append(f.getName()).append(" = ")
                            .append(fmtDeep(fv, depth + 1));
                } catch (Throwable ignore) {
                }
            }
        }
        return sb.append("}").toString();
    }

    private static void showPanel(final String javaText, final String protoJson) {
        showPanel(javaText, protoJson, null);
    }

    private static void showPanel(final String javaText, final String protoJson, final Object record) {
        try {
            Context c = QQEnv.topActivity();
            if (c == null) c = hostCtx;
            if (c == null) return;
            final Context ctx = c;
            final float den = ctx.getResources().getDisplayMetrics().density;

            final String[] cardHolder = new String[1];
            if (record != null) {
                cardHolder[0] = prettyJson(extractCardJson(record));
            }
            if (cardHolder[0] == null && protoJson != null) {
                cardHolder[0] = protoJson;
            }

            final android.app.Dialog d = new android.app.Dialog(ctx,
                    android.R.style.Theme_Translucent_NoTitleBar);
            LinearLayout root = new LinearLayout(ctx);
            root.setOrientation(LinearLayout.VERTICAL);
            root.setBackgroundColor(0xB3303A4D);
            root.setPadding((int) (20 * den), (int) (60 * den), (int) (20 * den), (int) (30 * den));

            LinearLayout panel = new LinearLayout(ctx);
            panel.setOrientation(LinearLayout.VERTICAL);
            android.graphics.drawable.GradientDrawable bg =
                    new android.graphics.drawable.GradientDrawable();
            bg.setColor(0xFFF4F8FF);
            bg.setCornerRadius(22 * den);
            panel.setBackgroundDrawable(bg);
            panel.setPadding((int) (16 * den), (int) (16 * den), (int) (16 * den), (int) (16 * den));

            LinearLayout tabBar = new LinearLayout(ctx);
            tabBar.setOrientation(LinearLayout.HORIZONTAL);
            final int[] curTab = {0};
            final String[] pbHolder = new String[1];
            final TextView tabJava = mkTab(ctx, "Java 消息", den, true);
            final TextView tabCard = mkTab(ctx, "卡片消息", den, false);
            final TextView tabPb = mkTab(ctx, "PB 报文", den, false);
            final TextView tabMeta = mkTab(ctx, "元数据", den, false);
            final String[] metaHolder = new String[1];
            tabBar.addView(tabJava, new LinearLayout.LayoutParams(0, -2, 1f));
            tabBar.addView(tabCard, new LinearLayout.LayoutParams(0, -2, 1f));
            tabBar.addView(tabPb, new LinearLayout.LayoutParams(0, -2, 1f));
            tabBar.addView(tabMeta, new LinearLayout.LayoutParams(0, -2, 1f));
            panel.addView(tabBar);

            final android.widget.EditText body = new android.widget.EditText(ctx);
            body.setTextSize(12);
            body.setTextColor(0xFF2F3E56);
            body.setGravity(android.view.Gravity.TOP | android.view.Gravity.START);
            body.setBackgroundColor(0x00000000);
            body.setPadding(0, (int) (10 * den), 0, 0);
            body.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                    | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
                    | android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
            body.setHorizontallyScrolling(false);

            body.setPadding(0, (int) (10 * den), (int) (6 * den), (int) (6 * den));
            android.widget.ScrollView sv = new android.widget.ScrollView(ctx);
            sv.addView(body);

            sv.setVerticalScrollBarEnabled(false);
            sv.setHorizontalScrollBarEnabled(false);
            sv.setOverScrollMode(View.OVER_SCROLL_NEVER);
            sv.setClipToPadding(false);

            LinearLayout bodyWrap = new LinearLayout(ctx);
            bodyWrap.setOrientation(LinearLayout.HORIZONTAL);
            bodyWrap.addView(sv, new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.MATCH_PARENT, 1f));
            final View barThumb = new View(ctx);
            android.graphics.drawable.GradientDrawable tbg =
                    new android.graphics.drawable.GradientDrawable();
            tbg.setColor(0x557FA5E6);
            tbg.setCornerRadius(50 * den);
            barThumb.setBackgroundDrawable(tbg);
            barThumb.setAlpha(0f);
            LinearLayout barCol = new LinearLayout(ctx);
            barCol.setOrientation(LinearLayout.VERTICAL);
            barCol.addView(barThumb, new LinearLayout.LayoutParams((int) (3 * den), 0));
            LinearLayout.LayoutParams bcp = new LinearLayout.LayoutParams(
                    (int) (3 * den), LinearLayout.LayoutParams.MATCH_PARENT);
            bcp.leftMargin = (int) (6 * den);
            bodyWrap.addView(barCol, bcp);
            panel.addView(bodyWrap, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
            com.cathelperawa.owo.app.ui.UiKit.attachSlimScrollbar(sv, barThumb, barCol, den);

            body.setText(javaText == null ? "" : javaText);

            tabJava.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    curTab[0] = 0;
                    setTabStyle(tabJava, true, den);
                    setTabStyle(tabCard, false, den);
                    setTabStyle(tabPb, false, den);
                    setTabStyle(tabMeta, false, den);
                    body.setText(javaText == null ? "" : javaText);
                }
            });

            tabCard.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    curTab[0] = 1;
                    setTabStyle(tabJava, false, den);
                    setTabStyle(tabCard, true, den);
                    setTabStyle(tabPb, false, den);
                    setTabStyle(tabMeta, false, den);

                    String txt = cardHolder[0];
                    if (txt == null && record != null) {
                        txt = prettyJson(extractCardJson(record));
                        cardHolder[0] = txt;
                    }
                    body.setText(txt == null ? "（没有卡片数据）" : txt);
                }
            });

            tabPb.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    curTab[0] = 2;
                    setTabStyle(tabJava, false, den);
                    setTabStyle(tabCard, false, den);
                    setTabStyle(tabPb, true, den);
                    setTabStyle(tabMeta, false, den);
                    if (pbHolder[0] == null) {
                        String card = cardHolder[0];
                        if ((card == null || card.trim().length() == 0) && record != null) {
                            card = prettyJson(extractCardJson(record));
                            cardHolder[0] = card;
                        }
                        String pb = OidbCard.toDc2(card, 0);
                        pbHolder[0] = (pb != null) ? pb
                                : "（这张卡转不成 0xdc2_34 报文，或者它不是新闻卡）";
                    }
                    body.setText(pbHolder[0]);
                }
            });

            tabMeta.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    curTab[0] = 3;
                    setTabStyle(tabJava, false, den);
                    setTabStyle(tabCard, false, den);
                    setTabStyle(tabPb, false, den);
                    setTabStyle(tabMeta, true, den);
                    if (metaHolder[0] == null) {
                        metaHolder[0] = MsgMetaSummary.build(record)
                                + "\n\n【原始字段】\n" + dumpMeta(record);
                    }
                    body.setText(metaHolder[0]);
                }
            });

            openCardSink = new CardSink() {
                @Override public void set(String text) {
                    if (cardHolder[0] != null) return;
                    cardHolder[0] = text;

                    String javaNow = javaText == null ? "" : javaText;
                    if (body.getText() != null && !body.getText().toString().equals(javaNow)) {
                        body.setText(text);
                    }
                }
            };

            LinearLayout actions = new LinearLayout(ctx);
            actions.setOrientation(LinearLayout.HORIZONTAL);

            final TextView saveTpl = mkBtn(ctx, "存为模板", den, false);
            saveTpl.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    final String content = body.getText() == null
                            ? "" : body.getText().toString().trim();
                    MsgStore.add("[提取] 点存为模板 len=" + content.length());
                    if (content.length() == 0) {
                        com.cathelperawa.owo.app.bridge.CatToast.show("内容是空的喵");
                        return;
                    }

                    final boolean pbMode = curTab[0] == 2 || looksLikePb(content);
                    final String pbCmd = pbMode ? OidbCard.CMD : "";
                    final String suggest = com.cathelperawa.owo.app.core.JsonPretty
                            .suggestName(content, "", pbMode);
                    com.cathelperawa.owo.app.ui.CatSheet.input(ctx,
                            pbMode ? "PB 模板名字" : "卡片模板名字", "", suggest,
                            new com.cathelperawa.owo.app.ui.CatSheet.OnText() {
                                @Override public void run(String text) {
                                    String name = text == null ? "" : text.trim();
                                    if (name.length() == 0) name = suggest;
                                    final String fname = name;
                                    final Runnable doSave = new Runnable() {
                                        @Override public void run() {
                                            boolean ok = com.cathelperawa.owo.app.core.TemplateStore
                                                    .save(ctx, fname, pbMode ? 1 : 0, content, pbCmd);
                                            com.cathelperawa.owo.app.bridge.CatToast.show(ok
                                                    ? ("已存进" + (pbMode ? "PB" : "卡片")
                                                    + "模板库：" + fname
                                                    + "（发送面板点「模板」能看到）")
                                                    : "存模板失败喵");
                                            MsgStore.add("[提取] 存模板 " + (ok ? "成功: " : "失败: ")
                                                    + fname);
                                        }
                                    };
                                    boolean exists = false;
                                    for (com.cathelperawa.owo.app.core.TemplateStore.Item it
                                            : com.cathelperawa.owo.app.core.TemplateStore.all(ctx)) {
                                        if (it.name.equals(fname)) {
                                            exists = true;
                                            break;
                                        }
                                    }
                                    if (exists) {
                                        com.cathelperawa.owo.app.ui.CatSheet.confirm(ctx,
                                                "已有同名模板",
                                                "「" + fname + "」已经存在，覆盖它吗？",
                                                "覆盖", false, doSave);
                                    } else {
                                        doSave.run();
                                    }
                                }
                            });
                }
            });

            boolean sendOn = true;
            try {
                sendOn = com.cathelperawa.owo.app.core.HookConfig
                        .getBoolean(ctx, "hook_send_proto", true);
            } catch (Throwable ignore) {
            }
            if (sendOn) {
                TextView toSend = mkBtn(ctx, "导入发送框", den, false);
                toSend.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        String content = body.getText() == null
                                ? "" : body.getText().toString().trim();
                        if (content.length() == 0) return;
                        d.dismiss();
                        if (curTab[0] == 2) {

                            com.cathelperawa.owo.app.ui.SendPanel.setPendingPb(
                                    content, OidbCard.CMD);
                        } else {
                            com.cathelperawa.owo.app.ui.SendPanel.setPending(content);
                        }
                        com.cathelperawa.owo.app.ui.SendPanel.show(ctx);
                    }
                });
                LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(0,
                        (int) (40 * den), 1f);
                tp.rightMargin = (int) (6 * den);
                actions.addView(toSend, tp);
            }

            LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(0,
                    (int) (40 * den), 1f);
            sp.rightMargin = (int) (6 * den);
            actions.addView(saveTpl, sp);

            final TextView saveMedia = mkBtn(ctx, "存素材", den, false);
            saveMedia.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {

                    String content = lastElements;
                    if (content == null || content.trim().length() == 0) {
                        content = body.getText() == null
                                ? "" : body.getText().toString().trim();
                    }
                    if (content.length() == 0) {
                        com.cathelperawa.owo.app.bridge.CatToast.show("内容是空的");
                        return;
                    }
                    String type = com.cathelperawa.owo.app.proto.MediaVault.guessType(content);
                    com.cathelperawa.owo.app.proto.MediaVault.add(ctx, content,
                            com.cathelperawa.owo.app.proto.MediaVault.niceName(content, type),
                            type);
                    com.cathelperawa.owo.app.bridge.CatToast.show(
                            "已存进素材库 · " + com.cathelperawa.owo.app.proto.MediaVault
                                    .typeName(type));
                    MsgStore.add("[提取] 存素材 " + type);
                }
            });
            actions.addView(saveMedia, new LinearLayout.LayoutParams(0,
                    (int) (40 * den), 1f));

            LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            ap.topMargin = (int) (12 * den);
            panel.addView(actions, ap);

            TextView ok = mkBtn(ctx, "关闭", den, true);
            LinearLayout.LayoutParams op = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, (int) (40 * den));
            op.topMargin = (int) (8 * den);
            panel.addView(ok, op);
            ok.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { d.dismiss(); }
            });
            d.setOnDismissListener(new android.content.DialogInterface.OnDismissListener() {
                @Override public void onDismiss(android.content.DialogInterface di) {
                    openCardSink = null;
                }
            });

            root.addView(panel, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
            d.setContentView(root);
            d.show();
            android.view.Window w = d.getWindow();
            if (w != null) {
                w.setLayout(LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT);
            }
        } catch (Throwable t) {
            HookLog.e("面板失败", t);
        }
    }

    private static boolean looksLikePb(String s) {
        return com.cathelperawa.owo.app.core.JsonPretty.looksLikePb(s);
    }

    private static TextView mkTab(Context ctx, String label, float den, boolean active) {
        TextView tv = new TextView(ctx);
        tv.setText(label);
        tv.setTextSize(13);
        tv.setGravity(android.view.Gravity.CENTER);
        tv.setPadding(0, (int) (10 * den), 0, (int) (10 * den));
        setTabStyle(tv, active, den);
        return tv;
    }

    private static void setTabStyle(TextView tv, boolean active, float den) {
        tv.setTextColor(active ? 0xFFFFFFFF : 0xFF43587A);
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setColor(active ? 0xFF7FA5E6 : 0x00000000);
        bg.setCornerRadius(18 * den);
        tv.setBackgroundDrawable(bg);
    }

    private static TextView mkBtn(Context ctx, String label, float den, boolean primary) {        TextView tv = new TextView(ctx);
        tv.setText(label);
        tv.setTextSize(14);
        tv.setTextColor(primary ? 0xFFFFFFFF : 0xFF43587A);
        tv.setGravity(Gravity.CENTER);
        tv.setBackgroundDrawable(btnBg(primary ? 0xFF7FA5E6 : 0xFFE3ECF8, 20 * den));
        return tv;
    }

    private static android.graphics.drawable.GradientDrawable btnBg(int color, float radius) {
        android.graphics.drawable.GradientDrawable g =
                new android.graphics.drawable.GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(radius);
        return g;
    }

    private static Object cloneItem(Object proto) {
        if (proto == null) return null;
        Class<?> c = proto.getClass();
        Object copy = null;
        try {
            Class<?> uc = Class.forName("sun.misc.Unsafe");
            java.lang.reflect.Field uf = uc.getDeclaredField("theUnsafe");
            uf.setAccessible(true);
            Object unsafe = uf.get(null);
            Method alloc = uc.getMethod("allocateInstance", Class.class);
            copy = alloc.invoke(unsafe, c);
        } catch (Throwable t) {
            HookLog.w("克隆菜单项失败(Unsafe) " + t);
            return null;
        }
        Class<?> k = c;
        int dep = 0;
        while (k != null && k != Object.class && dep++ < 6) {
            for (Field f : k.getDeclaredFields()) {
                if (java.lang.reflect.Modifier.isStatic(f.getModifiers())) continue;
                if (f.getName().contains("redirector")) continue;
                try {
                    f.setAccessible(true);
                    f.set(copy, f.get(proto));
                } catch (Throwable ignore) {
                }
            }
            k = k.getSuperclass();
        }
        return copy;
    }

    private static Object buildItem(Class<?> menuD, Context ctx, Object aioMsg) {
        try {
            Constructor<?> k = menuD.getDeclaredConstructor(Context.class, aioMsg.getClass());
            k.setAccessible(true);
            return k.newInstance(ctx, aioMsg);
        } catch (Throwable ignore) {
        }
        for (Constructor<?> k : menuD.getDeclaredConstructors()) {
            try {
                k.setAccessible(true);
                Class<?>[] ps = k.getParameterTypes();
                Object[] as = new Object[ps.length];
                for (int i = 0; i < ps.length; i++) {
                    if (ps[i].isInstance(aioMsg)) as[i] = aioMsg;
                    else if (ctx != null && ps[i].isInstance(ctx)) as[i] = ctx;
                    else if (ps[i] == String.class) as[i] = TITLE;
                    else if (ps[i] == boolean.class) as[i] = false;
                    else if (ps[i] == int.class) as[i] = 0;
                    else as[i] = null;
                }
                return k.newInstance(as);
            } catch (Throwable ignore) {
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static List<Object> findList(Object uiE) {
        Class<?> c = uiE.getClass();
        while (c != null && c != Object.class) {
            for (Field f : c.getDeclaredFields()) {
                if (!List.class.isAssignableFrom(f.getType())) continue;
                try {
                    f.setAccessible(true);
                    Object v = f.get(uiE);
                    if (v instanceof List && !((List<?>) v).isEmpty()) return (List<Object>) v;
                } catch (Throwable ignore) {
                }
            }
            c = c.getSuperclass();
        }
        return null;
    }

    private static Object findMsg(Object item) {
        if (item == null) return null;
        Class<?> c = item.getClass();
        while (c != null && c != Object.class) {
            for (Field f : c.getDeclaredFields()) {
                if (!f.getType().getName().contains("AIOMsgItem")) continue;
                try {
                    f.setAccessible(true);
                    Object v = f.get(item);
                    if (v != null) return v;
                } catch (Throwable ignore) {
                }
            }
            c = c.getSuperclass();
        }
        return null;
    }

    private static void setFieldByType(Object o, Class<?> type, Object val) {
        Class<?> c = o.getClass();
        while (c != null && c != Object.class) {
            for (Field f : c.getDeclaredFields()) {
                if (f.getType() != type) continue;
                try {
                    f.setAccessible(true);
                    f.set(o, val);
                    return;
                } catch (Throwable ignore) {
                }
            }
            c = c.getSuperclass();
        }
    }

    private static String titleOf(Object item) {
        for (String n : new String[]{"a", "e", "f", "title"}) {
            Object v = Refl.call(item, n);
            if (v instanceof String) return (String) v;
        }
        return null;
    }

    private static Object deepFind(Object root, String key) {
        if (root == null) return null;
        if (root.getClass().getName().contains(key)) return root;
        Class<?> c = root.getClass();
        while (c != null && c != Object.class) {
            for (Field f : c.getDeclaredFields()) {
                try {
                    f.setAccessible(true);
                    Object v = f.get(root);
                    if (v != null && v.getClass().getName().contains(key)) return v;
                } catch (Throwable ignore) {
                }
            }
            c = c.getSuperclass();
        }
        return null;
    }

    private static String shortOf(String s) {
        if (s == null) return "";
        return s.length() > 100 ? s.substring(0, 100) + "…" : s;
    }
}
