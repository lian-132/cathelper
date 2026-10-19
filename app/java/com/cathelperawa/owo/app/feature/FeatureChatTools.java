package com.cathelperawa.owo.app.feature;

import android.content.Context;
import android.os.Bundle;
import android.view.MotionEvent;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.UnderlineSpan;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.cathelperawa.owo.app.MsgStore;
import com.cathelperawa.owo.app.bridge.CatToast;
import com.cathelperawa.owo.app.bridge.QQEnv;
import com.cathelperawa.owo.app.bridge.QQUtils;
import com.cathelperawa.owo.app.core.HookApi;
import com.cathelperawa.owo.app.core.HookConfig;
import com.cathelperawa.owo.app.core.HookItem;
import com.cathelperawa.owo.app.core.HookItemBase;
import com.cathelperawa.owo.app.core.HookLog;
import com.cathelperawa.owo.app.ui.CatDialog;
import com.cathelperawa.owo.app.ui.CatPanel;
import com.cathelperawa.owo.app.util.Refl;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import de.robv.android.xposed.XC_MethodHook;

@HookItem(name = "聊天小工具", path = "聊天与消息/聊天小工具",
        description = "改本地文本 · 闪照破解 · 自定义点数 · 表情看图", order = 45)
public class FeatureChatTools extends HookItemBase {

    public static final String K_EDIT  = "chat_edittext";
    public static final String K_FLASH = "chat_flashpic";
    public static final String K_DICE  = "chat_customdice";
    public static final String K_EMOJI = "chat_emojiimg";

    public static final String K_NOAT  = "chat_noat";
    public static final String K_SHOWAT = "chat_showat";
    public static final String K_MIX   = "chat_mixsend";

    @Override
    public String switchKey() { return K_EDIT; }

    public static boolean editOn(Context c)  { return HookConfig.getBoolean(c, K_EDIT,  true); }
    public static boolean flashOn(Context c) { return HookConfig.getBoolean(c, K_FLASH, true); }
    public static boolean diceOn(Context c)  { return HookConfig.getBoolean(c, K_DICE,  true); }
    public static boolean emojiOn(Context c) { return HookConfig.getBoolean(c, K_EMOJI, true); }

    public static boolean noAtOn(Context c)  { return HookConfig.getBoolean(c, K_NOAT,  false); }

    public static boolean showAtOn(Context c) { return HookConfig.getBoolean(c, K_SHOWAT, false); }

    private static boolean anyOn(Context c) {
        return editOn(c) || flashOn(c) || diceOn(c) || emojiOn(c);
    }

    private static final boolean DIAG = true;

    private static final boolean DIAG_VERBOSE = false;

    private static final String CLS_MSG_ITEM = "com.tencent.mobileqq.aio.msg.AIOMsgItem";

    private static final int SUB_FLASH = 8194;
    private static final int FLASH_BIT = 8192;

    private static final String FLASH_MARK = "cat_flash";

    private static final String FLASH_TIP = "这是闪照喵~";

    private static final String FLASH_TIP_TAG = "cat_flash_tip";

    private static final String FLASH_WRAP_TAG = "cat_flash_hook";

    private static final String CLS_BUBBLE_VB =
            "com.tencent.mobileqq.aio.msglist.holder.AIOBubbleMsgItemVB";

    private static final String CLS_INPUT_REPLY = "com.tencent.mobileqq.aio.input.reply.c";

    private static final Map<Long, Map<Integer, String>> EDIT_TABLE =
            new LinkedHashMap<Long, Map<Integer, String>>();

    private static final String T_EDIT = "文本修改";
    private static Map<Object, Object[]> MENU_ITEMS;

    @Override
    public void entry(ClassLoader cl) {
        MsgStore.add("[小工具] ★ entry 开始");
        hint("长按消息 / 改过它的文本时");
        MENU_ITEMS = new java.util.IdentityHashMap<Object, Object[]>();
        int ok = 0;
        ok += hookRecord(cl) ? 1 : 0;
        ok += hookBubble(cl) ? 1 : 0;
        ok += hookMenu(cl) ? 1 : 0;
        ok += hookEmojiAsPic(cl) ? 1 : 0;
        ok += hookNoAt(cl) ? 1 : 0;
        ok += hookShowAt(cl) ? 1 : 0;
        if (!hookSend()) {
            retrySend(0);
        } else {
            ok += 1;
        }
        HookLog.i("聊天小工具：安装完成 " + ok + "/4 条路径");
        MsgStore.add("[小工具] 安装完成 " + ok + "/4 条路径");
    }

    private void retrySend(final int attempt) {
        if (attempt >= 12) {
            MsgStore.add("[小工具] sendMsg 重试 12 次仍未拿到服务，点数不可用");
            return;
        }
        final long wait = attempt == 0 ? 3000L : 2500L;
        new Thread(new Runnable() { @Override public void run() {
            try { Thread.sleep(wait); } catch (Throwable ignore) {}
            if (hookSend()) {
                MsgStore.add("[小工具] sendMsg 延迟第 " + (attempt + 1) + " 次挂载成功");
            } else {
                retrySend(attempt + 1);
            }
        } }, "cat-tools-retry").start();
    }

    @Override
    protected void check(com.cathelperawa.owo.app.core.SelfCheckResult.Checker c) {
        Class<?> item = c.cls("AIOMsgItem", CLS_MSG_ITEM);
        c.method("getMsgRecord", item, "getMsgRecord");

        Class<?> reply = c.cls("输入回复类", CLS_INPUT_REPLY);
    }

    private boolean hookRecord(ClassLoader cl) {
        Class<?> itemCls = HookApi.findClass(cl, CLS_MSG_ITEM);
        if (itemCls == null) {
            fail("小工具：未找到 " + CLS_MSG_ITEM);
            MsgStore.add("[小工具] 未找到 " + CLS_MSG_ITEM);
            return false;
        }
        Method get = HookApi.findMethod(itemCls, "getMsgRecord");
        if (get == null) {
            fail("小工具：未找到 getMsgRecord");
            return false;
        }
        hookAfter(get, new HookApi.Action() {
            @Override public void call(XC_MethodHook.MethodHookParam p) {

                try {
                    Object rec = p.getResult();
                    if (rec == null) return;

                    if (DIAG_VERBOSE) {
                        int sub = Refl.i32(Refl.field(rec, "subMsgType"), 0);
                        long id = Refl.lng(Refl.field(rec, "msgId"), 0);
                        boolean hasEdit;
                        synchronized (EDIT_TABLE) { hasEdit = EDIT_TABLE.containsKey(id); }
                        MsgStore.add("[小工具·钩子] getMsgRecord 进来了 sub=" + sub
                                + " msgId=" + id + " 有改文本=" + hasEdit);
                    }
                    if (flashOn(ctx)) cutFlash(rec);
                    if (editOn(ctx)) applyEdit(rec);
                } catch (Throwable t) {
                    HookLog.e("getMsgRecord 钩子异常", t);
                }
            }
        });
        HookLog.i("小工具：getMsgRecord 已挂 " + itemCls.getName());
        return true;
    }

    private void cutFlash(Object rec) {
        int sub = Refl.i32(Refl.field(rec, "subMsgType"), 0);
        if ((sub & FLASH_BIT) == 0) return;
        Refl.setField(rec, "subMsgType", sub & ~FLASH_BIT);

        try { Refl.setField(rec, "guildName", FLASH_MARK); } catch (Throwable ignore) {}
        MsgStore.add("[闪照] 已清位 " + sub + " → " + (sub & ~FLASH_BIT) + "，已打标记");
    }

    private void applyEdit(Object rec) {
        long id = Refl.lng(Refl.field(rec, "msgId"), 0);
        if (id == 0) return;
        Map<Integer, String> items;
        synchronized (EDIT_TABLE) { items = EDIT_TABLE.get(id); }
        if (items == null || items.isEmpty()) return;

        fire("改文本");
        Object elsObj = Refl.field(rec, "elements");
        if (!(elsObj instanceof List)) return;
        List<?> els = (List<?>) elsObj;
        for (Integer idx : items.keySet()) {
            if (idx == null || idx < 0 || idx >= els.size()) continue;
            Object te = Refl.field(els.get(idx), "textElement");
            if (te == null) continue;
            Refl.setField(te, "content", items.get(idx));
        }
    }

    private boolean hookBubble(ClassLoader cl) {
        Class<?> vb = HookApi.findClass(cl, CLS_BUBBLE_VB);
        if (vb == null) {
            fail("小工具：未找到 " + CLS_BUBBLE_VB);
            MsgStore.add("[小工具] 未找到气泡VB");
            return false;
        }
        Method target = HookApi.findMethodByParams(vb, void.class,
                int.class, null, List.class, Bundle.class);
        if (target == null) {
            for (Method m : vb.getDeclaredMethods()) {
                Class<?>[] ps = m.getParameterTypes();
                if (ps.length != 4) continue;
                if (ps[0] != int.class || ps[2] != List.class || ps[3] != Bundle.class) continue;
                target = m;
                break;
            }
        }
        if (target == null) {
            fail("小工具：未找到气泡视图更新方法");
            MsgStore.add("[小工具] 未找到气泡视图更新方法");
            return false;
        }
        HookLog.i("小工具：气泡视图更新 = " + target.getName());
        MsgStore.add("[小工具] 气泡VB方法=" + target.getName());
        hookAfter(target, new HookApi.Action() {
            @Override public void call(XC_MethodHook.MethodHookParam p) {
                try {
                    if (ctx == null || !flashOn(ctx)) return;
                    if (p.args == null || p.args.length < 2) return;

                    Object msgItem = p.args[1];
                    if (msgItem == null) return;
                    Object rec = recordOf(msgItem);
                    if (rec == null) return;
                    if (!isFlashMarked(rec)) return;

                    View msgView = findViewField(p.thisObject);
                    if (msgView == null) {
                        MsgStore.add("[闪照] 没在 thisObject 里找到 View 字段");
                        return;
                    }
                    addFlashOverlay(msgView, rec);
                } catch (Throwable t) {
                    HookLog.e("闪照提示异常", t);
                }
            }
        });
        return true;
    }

    private static View findViewField(Object holder) {
        if (holder == null) return null;
        if (holder instanceof View) return (View) holder;
        Class<?> c = holder.getClass();
        int depth = 0;
        while (c != null && c != Object.class && depth++ < 8) {
            for (java.lang.reflect.Field f : c.getDeclaredFields()) {
                if (!View.class.isAssignableFrom(f.getType())) continue;
                try {
                    f.setAccessible(true);
                    Object v = f.get(holder);
                    if (v instanceof View) return (View) v;
                } catch (Throwable ignore) {
                }
            }
            c = c.getSuperclass();
        }
        return null;
    }

    private static boolean isFlashMarked(Object rec) {
        try {
            Object g = Refl.field(rec, "guildName");
            return g != null && FLASH_MARK.equals(String.valueOf(g));
        } catch (Throwable t) {
            return false;
        }
    }

    private void addFlashOverlay(View msgView, Object rec) {
        try {
            if (!(msgView instanceof ViewGroup)) return;
            ViewGroup root = (ViewGroup) msgView;

            ViewGroup bubble = null;
            for (int i = 0; i < root.getChildCount(); i++) {
                View ch = root.getChildAt(i);
                String cn = ch.getClass().getName();
                if (cn.contains("BubbleLayoutCompatPress")) {
                    if (ch instanceof ViewGroup) bubble = (ViewGroup) ch;
                    break;
                }
            }
            if (bubble == null) {

                for (int i = 0; i < root.getChildCount(); i++) {
                    View ch = root.getChildAt(i);
                    if (ch.getClass().getName().contains("Bubble")
                            && ch instanceof ViewGroup) {
                        bubble = (ViewGroup) ch;
                        break;
                    }
                }
            }
            if (bubble == null || bubble.getChildCount() == 0) {
                MsgStore.add("[闪照] 没找到气泡本体");
                return;
            }

            Context c = bubble.getContext();
            float d = c.getResources().getDisplayMetrics().density;

            FrameLayout frame;
            View first = bubble.getChildAt(0);
            if (first instanceof FrameLayout && FLASH_WRAP_TAG.equals(first.getTag())) {
                frame = (FrameLayout) first;
            } else {
                frame = new FrameLayout(c);
                frame.setTag(FLASH_WRAP_TAG);
                LinearLayout wrap = new LinearLayout(c);
                wrap.setOrientation(LinearLayout.VERTICAL);
                bubble.removeAllViews();
                wrap.addView(first, new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));
                frame.addView(wrap, new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));
                bubble.addView(frame, new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));
            }

            if (findByTag(frame, FLASH_TIP_TAG) != null) return;

            TextView tip = new TextView(c);
            tip.setTag(FLASH_TIP_TAG);
            tip.setText(FLASH_TIP);
            tip.setTextSize(13);
            tip.setTextColor(0xFFFFFFFF);
            tip.setTypeface(android.graphics.Typeface.DEFAULT_BOLD,
                    android.graphics.Typeface.BOLD);
            tip.setSingleLine(true);
            tip.setGravity(Gravity.CENTER);
            tip.setPadding((int) (14 * d), (int) (7 * d), (int) (14 * d), (int) (7 * d));
            android.graphics.drawable.GradientDrawable bg =
                    new android.graphics.drawable.GradientDrawable();

            bg.setColors(new int[]{0xFF8FB8FF, 0xFFF07CA8});
            bg.setOrientation(android.graphics.drawable.GradientDrawable.Orientation.LEFT_RIGHT);
            bg.setCornerRadius(999f);
            bg.setStroke((int) Math.max(1, d), 0x66FFFFFF);
            tip.setBackgroundDrawable(bg);
            tip.setAlpha(0f);
            tip.animate().alpha(0.92f).setDuration(220).start();

            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.gravity = Gravity.CENTER;
            frame.addView(tip, lp);
            MsgStore.add("[闪照] 已在图上叠提示");
        } catch (Throwable t) {
            HookLog.e("闪照叠字失败", t);
        }
    }

    private static View findByTag(View v, String tag) {
        if (tag.equals(v.getTag())) return v;
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) {
                View hit = findByTag(g.getChildAt(i), tag);
                if (hit != null) return hit;
            }
        }
        return null;
    }

    private boolean hookSend() {

        String[] classes = new String[]{
                "com.tencent.qqnt.msg.api.impl.MsgServiceImpl",
                "com.tencent.qqnt.msg.MsgService",
                "com.tencent.qqnt.kernel.api.impl.MsgService",
        };
        int n = 0;
        for (String cn : classes) {
            Class<?> cls = HookApi.findClass(cl, cn);
            if (cls == null) continue;
            for (final Method m : cls.getDeclaredMethods()) {
                if (!"sendMsg".equals(m.getName())) continue;
                Class<?>[] ps = m.getParameterTypes();

                boolean hasList = false;
                for (Class<?> p : ps) {
                    if (List.class.isAssignableFrom(p)) { hasList = true; break; }
                }
                if (!hasList) continue;
                hookBefore(m, new HookApi.Action() {
                    @Override public void call(XC_MethodHook.MethodHookParam p) { onSend(p); }
                });
                n++;
                MsgStore.add("[小工具] 挂 " + cls.getSimpleName() + "."
                        + m.getName() + "(" + ps.length + "参)");
            }
        }
        HookLog.i("小工具：sendMsg 挂了 " + n + " 个");
        return n > 0;
    }

    @SuppressWarnings("unchecked")
    private void onSend(XC_MethodHook.MethodHookParam p) {
        try {
            if (ctx == null || !diceOn(ctx)) return;
            if (p.args == null) return;

            Object peerArg = null;
            List<Object> els = null;
            for (Object a : p.args) {
                if (a instanceof List && els == null) {
                    els = (List<Object>) a;
                } else if (a != null && peerArg == null
                        && a.getClass().getName().contains("Contact")) {
                    peerArg = a;
                }
            }
            if (els == null || els.isEmpty() || peerArg == null) return;

            if (DIAG) {
                Object fe = Refl.field(els.get(0), "faceElement");
                Object ft = fe == null ? null : Refl.field(fe, "faceText");
                MsgStore.add("[小工具·发送] sendMsg 进来了 元素0="
                        + els.get(0).getClass().getSimpleName() + " faceText=" + ft);
            }

            int chatType = Refl.i32(Refl.field(peerArg, "chatType"), 0);
            if (chatType == 100) return;

            Object faceEl = Refl.field(els.get(0), "faceElement");
            if (faceEl == null) return;
            Object ftRaw = Refl.field(faceEl, "faceText");
            if (ftRaw == null) return;
            final String faceText = String.valueOf(ftRaw);

            final String[] labels;
            final String title;
            final boolean isDice = "/骰子".equals(faceText);
            final int max;
            if (isDice) {
                max = 6;
                labels = new String[]{"1点", "2点", "3点", "4点", "5点", "6点", "随机"};
                title = "自定义骰子";
            } else if ("/包剪锤".equals(faceText)) {
                max = 3;
                labels = new String[]{"布", "剪刀", "石头", "随机"};
                title = "自定义猜拳";
            } else {
                return;
            }

            p.setResult(null);
            final String peerUid = Refl.strOr(Refl.field(peerArg, "peerUid"), "");
            final int ct = chatType;
            MsgStore.add("[点数] 命中 " + faceText + " chatType=" + ct + " peer=" + peerUid);

            ui(new Runnable() { @Override public void run() {
                Context c = QQEnv.topActivity() != null ? QQEnv.topActivity() : ctx;
                CatDialog.pick(c, title, labels, new CatDialog.OnPick() {
                    @Override public void run(int idx, String label) {

                        int val = (idx >= max)
                                ? (1 + new java.util.Random().nextInt(max))
                                : (idx + 1);
                        boolean ok = sendRandom(faceText, val, peerUid, ct);
                        String show = (idx >= max)
                                ? ("随机 → " + (isDice ? (val + "点") : rpsName(val)))
                                : label;
                        MsgStore.add("[点数] 选=" + label + " 值=" + val + " 发送=" + ok);
                        toast(ok ? ("发出去啦：" + show) : "发送失败喵…看日志");
                    }
                });
            } });
        } catch (Throwable t) {
            HookLog.e("点数钩子异常", t);
        }
    }

    private static boolean sendRandom(String faceText, int value,
                                      String peerUid, int chatType) {
        try {
            boolean isDice = "/骰子".equals(faceText);
            if (isDice) {
                if (value < 1 || value > 6) value = 1;
            } else {
                if (value < 1 || value > 3) value = 1;
            }
            String peer = (chatType == 1) ? ("\"" + peerUid + "\"") : peerUid;
            String v1 = isDice ? "33" : "34";
            String v2 = isDice ? "358" : "359";
            long r1 = Math.abs(new java.util.Random().nextInt());
            long r2 = Math.abs(new java.util.Random().nextInt());

            String json = PB_TEMPLATE
                    .replace("__TYPE1__", String.valueOf(chatType))
                    .replace("__TYPE2__", String.valueOf(3 - chatType))
                    .replace("__PEER__", peer)
                    .replace("__V1__", v1)
                    .replace("__V2__", v2)
                    .replace("__RESULT__", String.valueOf(value))
                    .replace("__FACETEXT__", faceText)
                    .replace("__RND1__", String.valueOf(r1))
                    .replace("__RND2__", String.valueOf(r2));

            MsgStore.add("[点数] 报文 " + json);

            org.json.JSONObject obj = new org.json.JSONObject(json);
            com.cathelperawa.owo.app.ovo.FunProtoData data = new com.cathelperawa.owo.app.ovo.FunProtoData();
            data.fromJSON(obj);
            byte[] bytes = data.toBytes();
            if (bytes == null || bytes.length == 0) {
                MsgStore.add("[点数] 组包失败（字节为空）");
                return false;
            }
            MsgStore.add("[点数] 组包 " + bytes.length + "B");
            return com.cathelperawa.owo.app.proto.PbSender.sendCmd("MessageSvc.PbSendMsg", bytes);
        } catch (Throwable t) {
            HookLog.e("sendRandom 失败", t);
            MsgStore.add("[点数] sendRandom 异常 " + t);
            return false;
        }
    }

    private static String rpsName(int v) {
        if (v == 1) return "\u5e03";
        if (v == 2) return "\u526a\u5200";
        if (v == 3) return "\u77f3\u5934";
        return "";
    }

    private static final String PB_TEMPLATE =
            "{"
            + "\"1\":{\"__TYPE1__\":{\"__TYPE2__\":__PEER__}},"
            + "\"3\":{\"1\":{\"2\":{\"53\":{"
            +   "\"1\":37,"
            +   "\"2\":{"
            +     "\"1\":\"1\","
            +     "\"2\":\"__V1__\","
            +     "\"3\":__V2__,"
            +     "\"4\":1,"
            +     "\"5\":2,"
            +     "\"6\":\"__RESULT__\","
            +     "\"7\":\"__FACETEXT__\","
            +     "\"9\":1"
            +   "},"
            +   "\"3\":20"
            + "}}}},"
            + "\"4\":__RND1__,"
            + "\"5\":__RND2__"
            + "}";

    private boolean hookNoAt(ClassLoader cl) {
        Class<?> cls = HookApi.findClass(cl, CLS_INPUT_REPLY);
        if (cls == null) {
            fail("小工具：未找到 " + CLS_INPUT_REPLY);
            MsgStore.add("[去@] 未找到回复VM类");
            return false;
        }
        Class<?> itemCls = HookApi.findClass(cl, CLS_MSG_ITEM);
        if (itemCls == null) {
            fail("小工具：未找到 " + CLS_MSG_ITEM);
            MsgStore.add("[去@] 未找到 AIOMsgItem");
            return false;
        }

        Method target = null;
        int cand = 0;
        for (Method m : cls.getDeclaredMethods()) {
            Class<?>[] ps = m.getParameterTypes();
            if (m.getReturnType() != void.class || ps.length != 1) continue;
            if (!ps[0].isAssignableFrom(itemCls)) continue;
            cand++;
            if (target == null) target = m;
        }
        if (target == null) {
            HookLog.w("小工具：未找到 void(AIOMsgItem)");
            MsgStore.add("[去@] 未找到 r() 方法");
            return false;
        }
        HookLog.i("小工具：去自动@ 目标 = " + target.getName() + "（候选 " + cand + "）");
        MsgStore.add("[去@] 目标=" + target.getName() + " 候选=" + cand);

        HookApi.before(target, new HookApi.Action() {
            @Override public void call(XC_MethodHook.MethodHookParam p) {
                try {
                    if (ctx == null || !noAtOn(ctx)) return;
                    p.setResult(null);
                    if (DIAG) MsgStore.add("[去@] 已拦截一次自动@");
                } catch (Throwable t) {
                    HookLog.e("去自动@ 钩子异常", t);
                }
            }
        });
        HookLog.i("小工具：去自动@ 已挂 " + cls.getName() + "#" + target.getName());
        return true;
    }

    private static final int AT_COLOR = 0xFF2B4BE8;

    private static final Map<Spannable, Boolean> AT_DONE =
            java.util.Collections.synchronizedMap(
                    new java.util.WeakHashMap<Spannable, Boolean>());

    private boolean hookShowAt(ClassLoader cl) {

        Class<?> vb = HookApi.findClass(cl, CLS_BUBBLE_VB);
        if (vb == null) {
            HookLog.w("小工具：未找到 " + CLS_BUBBLE_VB);
            diag("[@高亮] 未找到气泡VB");
            return false;
        }
        Method target = null;
        StringBuilder cands = new StringBuilder();
        for (Method m : vb.getDeclaredMethods()) {
            if (m.getReturnType() != void.class) continue;
            Class<?>[] ps = m.getParameterTypes();
            if (ps.length != 1) continue;
            if (!ps[0].getName().endsWith("IMsgItemMviUIState")) continue;
            target = m;
            break;
        }
        if (target == null) {

            for (Method m : vb.getDeclaredMethods()) {
                if (m.getReturnType() != void.class) continue;
                Class<?>[] ps = m.getParameterTypes();
                if (ps.length == 1) {
                    cands.append(m.getName()).append("(")
                            .append(ps[0].getSimpleName()).append(") ");
                }
            }
            diag("[@高亮] 未找到 k2。单参void=" + cands);
            return false;
        }
        diag("[@高亮] 已挂 VB 方法=" + target.getName());
        HookApi.after(target, new HookApi.Action() {
            @Override public void call(XC_MethodHook.MethodHookParam p) {
                handler.post(new Runnable() {
                    @Override public void run() {
                        try {
                            if (ctx == null || !showAtOn(ctx)) return;
                            View vv = findViewField(p.thisObject);
                            if (!(vv instanceof ViewGroup)) return;
                            ViewGroup root = (ViewGroup) vv;
                            Object item = findMsgItem(p.thisObject, p.args);
                            if (item == null) return;
                            applyAtToViews(root, item);
                        } catch (Throwable t) {
                            diag("[@高亮] VB 钩子异常 " + t);
                        }
                    }
                });
            }
        });
        return true;
    }

    private void applyAtToViews(ViewGroup root, Object item) {
        Object rec = Refl.call(item, "getMsgRecord");
        if (rec == null) return;
        int chatType = Refl.i32(Refl.field(rec, "chatType"), 0);
        int msgType = Refl.i32(Refl.field(rec, "msgType"), 0);

        if (chatType != 2) return;
        if (msgType != 2 && msgType != 9) return;

        List<?> els = null;
        Object m2 = Refl.call(item, "m2");
        if (m2 instanceof List) els = (List<?>) m2;
        if (els == null) {
            Object e = Refl.field(rec, "elements");
            if (e instanceof List) els = (List<?>) e;
        }
        if (els == null || els.isEmpty()) return;

        java.util.ArrayList<String> ats = new java.util.ArrayList<String>();
        java.util.ArrayList<String> atUids = new java.util.ArrayList<String>();
        for (Object el : els) {
            if (el == null) continue;
            Object te = Refl.field(el, "textElement");
            if (te == null) continue;
            if (Refl.i32(Refl.field(te, "atType"), 0) != 2) continue;
            String content = Refl.strOr(Refl.field(te, "content"), "");
            if (content.length() < 2) continue;
            Object nt = Refl.field(te, "atNtUid");
            String uid = nt == null ? "" : String.valueOf(nt);
            if ("null".equals(uid)) uid = "";
            ats.add(content);
            atUids.add(uid);
        }
        if (ats.isEmpty()) return;

        final String peerUinStr = String.valueOf(Refl.lng(Refl.field(rec, "peerUin"), 0L));

        java.util.ArrayList<TextView> tvs = new java.util.ArrayList<TextView>();
        findAllTextViews(root, tvs);
        if (tvs.isEmpty()) return;

        for (int ai = 0; ai < ats.size(); ai++) {
            String at = ats.get(ai);
            String atUid = atUids.get(ai);
            for (TextView tv : tvs) {
                CharSequence text = tv.getText();
                if (!(text instanceof Spannable)) continue;
                String plain = text.toString();
                int start = plain.indexOf(at);
                if (start < 0) continue;
                int end = start + at.length();
                Spannable sp = (Spannable) text;

                for (ClickableSpan old : sp.getSpans(start, end, ClickableSpan.class)) {
                    sp.removeSpan(old);
                }
                sp.setSpan(new ForegroundColorSpan(AT_COLOR), start, end,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                sp.setSpan(new UnderlineSpan(), start, end,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                sp.setSpan(new AtClickSpan(at, atUid, peerUinStr), start, end, 33);
                if (!(tv.getMovementMethod()
                        instanceof android.text.method.LinkMovementMethod)) {
                    tv.setMovementMethod(
                            android.text.method.LinkMovementMethod.getInstance());
                }
            }
        }
    }

    private static void findAllTextViews(View v, List<TextView> out) {
        if (v instanceof TextView) {
            out.add((TextView) v);
        } else if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) {
                findAllTextViews(g.getChildAt(i), out);
            }
        }
    }

    private static Object findMsgItem(Object holder, Object[] args) {
        if (args != null) {
            for (Object a : args) {
                if (a == null) continue;
                if (a.getClass().getName().endsWith("AIOMsgItem")) return a;
            }
        }
        if (holder == null) return null;
        Class<?> c = holder.getClass();
        int d = 0;
        while (c != null && d++ < 6) {
            for (java.lang.reflect.Field f : c.getDeclaredFields()) {
                if (!f.getType().getName().endsWith("AIOMsgItem")) continue;
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

    private static final android.os.Handler handler =
            new android.os.Handler(android.os.Looper.getMainLooper());

    private static void onAtClick(Context c, String nick, String uid, String peerUinStr) {
        if (c == null) return;
        try {

            String pureNick = nick == null ? "" : nick.trim();
            while (pureNick.startsWith("@")) pureNick = pureNick.substring(1).trim();

            String uin = null;
            if (uid != null && uid.length() > 0) {
                uin = QQUtils.uinFromUid(uid);
            }
            diag("[@高亮] 跳资料卡 nick=" + pureNick + " uid=" + uid + " uin=" + uin);

            ClassLoader cl = QQEnv.classLoader();
            Class<?> iface = Class.forName(
                    "com.tencent.qqnt.aio.adapter.api.IContactApi", false, cl);
            Class<?> qroute = Class.forName(
                    "com.tencent.mobileqq.qroute.QRoute", false, cl);
            Method api = qroute.getMethod("api", Class.class);
            api.setAccessible(true);
            Object impl = api.invoke(null, iface);
            if (impl == null) { diag("[@高亮] IContactApi 实例为空"); return; }

            Context actx = c;
            try {
                android.app.Activity top = QQEnv.topActivity();
                if (top != null) actx = top;
            } catch (Throwable ignore) {
            }
            diag("[@高亮] 用 Context=" + actx.getClass().getName());

            Class<?> recCls = Class.forName(
                    "com.tencent.qqnt.kernel.nativeinterface.MsgRecord", false, cl);
            Object rec = recCls.newInstance();
            long uinL = 0L;
            try { uinL = uin == null ? 0L : Long.parseLong(uin); } catch (Throwable ignore) {}
            if (uinL == 0L) { diag("[@高亮] uin 无效，放弃"); return; }

            long peerUin = 0L;
            try { peerUin = Long.parseLong(peerUinStr); } catch (Throwable ignore) {}

            setField(recCls, rec, "senderUin", uinL);
            setField(recCls, rec, "peerUin", peerUin);
            setField(recCls, rec, "chatType", 2);

            Class<?> itemCls = Class.forName(
                    "com.tencent.mobileqq.aio.msg.AIOMsgItem", false, cl);
            Object msgItem = itemCls.getConstructor(recCls).newInstance(rec);

            for (Method m : impl.getClass().getMethods()) {
                if (!"openProfileCard".equals(m.getName())) continue;
                Class<?>[] ps = m.getParameterTypes();
                if (ps.length != 2) continue;
                if (!ps[1].isInstance(msgItem)) continue;
                m.setAccessible(true);
                m.invoke(impl, actx, msgItem);
                diag("[@高亮] 已用 openProfileCard 打开");
                return;
            }
            diag("[@高亮] 没找到 openProfileCard");
        } catch (Throwable t) {
            diag("[@高亮] 打开资料卡失败 " + t);
            HookLog.e("@高亮 打开资料卡失败", t);
        }
    }

    private static TextView findTextView(Object holder) {
        if (holder == null) return null;

        Class<?> c = holder.getClass();
        int depth = 0;
        while (c != null && depth++ < 6) {
            for (java.lang.reflect.Field f : c.getDeclaredFields()) {
                if (!TextView.class.isAssignableFrom(f.getType())) continue;
                try {
                    f.setAccessible(true);
                    Object v = f.get(holder);
                    if (v instanceof TextView) return (TextView) v;
                } catch (Throwable ignore) {
                }
            }
            c = c.getSuperclass();
        }

        Class<?> c2 = holder.getClass();
        int d2 = 0;
        while (c2 != null && d2++ < 6) {
            for (java.lang.reflect.Field f : c2.getDeclaredFields()) {
                if (!View.class.isAssignableFrom(f.getType())) continue;
                try {
                    f.setAccessible(true);
                    Object v = f.get(holder);
                    TextView tv = deepFind(v);
                    if (tv != null) return tv;
                } catch (Throwable ignore) {
                }
            }
            c2 = c2.getSuperclass();
        }
        return null;
    }

    private static TextView deepFind(Object v) {
        if (!(v instanceof View)) return null;
        if (v instanceof TextView) return (TextView) v;
        if (!(v instanceof ViewGroup)) return null;
        ViewGroup g = (ViewGroup) v;
        for (int i = 0; i < g.getChildCount(); i++) {
            TextView t = deepFind(g.getChildAt(i));
            if (t != null) return t;
        }
        return null;
    }

    private static boolean isMsgItem(Class<?> c) {
        int depth = 0;
        while (c != null && depth++ < 12) {
            if ("AIOMsgItem".equals(c.getSimpleName())) return true;
            if (c.getName().equals("java.lang.Object")) return false;
            c = c.getSuperclass();
        }
        return false;
    }

    private static void diag(String s) {
        try {
            Context c = QQEnv.appContext();
            if (c == null) return;
            java.io.File f = new java.io.File(c.getFilesDir(), "cathelper/atdiag.txt");
            f.getParentFile().mkdirs();
            java.io.FileWriter w = new java.io.FileWriter(f, true);
            w.write(new java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US)
                    .format(new java.util.Date()) + "  " + s + "\n");
            w.close();
        } catch (Throwable ignore) {
        }
    }

    private static void setField(Class<?> cls, Object obj, String name, Object val) {
        Class<?> c = cls;
        while (c != null) {
            try {
                java.lang.reflect.Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                f.set(obj, val);
                return;
            } catch (Throwable t) {
                c = c.getSuperclass();
            }
        }
    }

    private static final class AtClickSpan extends ClickableSpan {

        private final String target;

        private final String uid;

        private final String peerUin;

        AtClickSpan(String nick, String uid, String peerUin) {
            this.target = nick; this.uid = uid; this.peerUin = peerUin;
        }

        @Override public void onClick(View widget) {
            try {
                diag("[@高亮] 点了 " + target + " uid=" + uid);
                onAtClick(widget.getContext(), target, uid, peerUin);
            } catch (Throwable e) {
                diag("[@高亮] onClick 异常 " + e);
            }
        }
    }

    private boolean hookEmojiAsPic(ClassLoader cl) {
        Class<?> cls = HookApi.findClass(cl,
                "com.tencent.qqnt.aio.adapter.api.impl.RichMediaBrowserApiImpl");
        if (cls == null) {
            HookLog.w("小工具：未找到 RichMediaBrowserApiImpl");
            MsgStore.add("[小工具] 未找到 RichMediaBrowserApiImpl");
            return false;
        }
        Method target = null;
        for (Method m : cls.getDeclaredMethods()) {
            if (!"checkIsFavPicAndShowPreview".equals(m.getName())) continue;
            target = m;
            break;
        }
        if (target == null) {

            target = HookApi.findMethodByParams(cls, boolean.class,
                    null, null, View.class, List.class);
        }
        if (target == null) {
            HookLog.w("小工具：未找到 checkIsFavPicAndShowPreview");
            return false;
        }
        HookLog.i("小工具：表情看图 = " + target);
        MsgStore.add("[小工具] 表情看图方法=" + target.getName());
        hookBefore(target, new HookApi.Action() {
            @Override public void call(XC_MethodHook.MethodHookParam p) {
                try {
                    if (ctx == null || !emojiOn(ctx)) return;
                    p.setResult(Boolean.FALSE);
                } catch (Throwable t) {
                    HookLog.e("表情看图钩子异常", t);
                }
            }
        });
        return true;
    }

    private boolean hookMenu(ClassLoader cl) {
        Class<?> expand = HookApi.findClass(cl,
                "com.tencent.qqnt.aio.menu.ui.QQCustomMenuExpandableLayout");
        if (expand == null) {
            HookLog.w("小工具：未找到菜单布局");
            return false;
        }
        Method setMenu = HookApi.findMethod(expand, "setMenu");
        if (setMenu == null) {
            HookLog.w("小工具：未找到 setMenu");
            return false;
        }
        hookBefore(setMenu, new HookApi.Action() {
            @Override public void call(XC_MethodHook.MethodHookParam p) { onMenu(p); }
        });
        hookMenuRender(cl);
        HookLog.i("小工具：菜单已挂");
        return true;
    }

    private void onMenu(XC_MethodHook.MethodHookParam p) {
        try {
            Context c = ctx;
            if (c == null || !anyOn(c)) return;
            Object uiE = p.args[0];
            if (uiE == null) return;
            List<Object> items = findList(uiE);
            if (items == null || items.isEmpty()) return;
            Object msgItem = findMsg(items.get(0));
            if (msgItem == null) return;
            Object rec = recordOf(msgItem);
            if (rec == null) return;
            Class<?> menuD = HookApi.findClass(cl, "com.tencent.qqnt.aio.menu.d");
            if (menuD == null) return;
            if (p.thisObject instanceof View) {
                Context c2 = ((View) p.thisObject).getContext();
                if (c2 != null) c = c2;
            }

            if (editOn(c) && hasEditableText(rec)) insertMenu(menuD, c, msgItem, items, T_EDIT);
        } catch (Throwable t) {
            HookLog.e("菜单注入异常", t);
        }
    }

    private void insertMenu(Class<?> menuD, Context c, Object msgItem,
                            List<Object> items, String title) {
        try {
            for (Object o : items) {
                if (o != null && title.equals(titleOf(o))) return;
            }
            Object item = buildMenuItem(menuD, c, msgItem);
            if (item == null) {
                MsgStore.add("[插入·" + title + "] buildMenuItem 返回 null");
                return;
            }
            setFieldByType(item, boolean.class, false);
            setFieldByType(item, int.class, 0);
            setFieldByType(item, String.class, title);
            synchronized (MENU_ITEMS) {
                MENU_ITEMS.put(item, new Object[]{msgItem, title});
            }
            items.add(0, item);
            MsgStore.add("[插入·" + title + "] 成功，列表现有 " + items.size() + " 项");
        } catch (Throwable t) {
            MsgStore.add("[插入·" + title + "] 异常 " + t);
            HookLog.e("插入菜单项失败 " + title, t);
        }
    }

    private void hookMenuRender(ClassLoader cl) {
        String[] hosts = new String[]{
                "com.tencent.qqnt.aio.menu.ui.QQCustomMenuNoIconLayout",
                "com.tencent.qqnt.aio.menu.ui.QQCustomMenuExpandableLayout",
        };
        for (String hn : hosts) {
            Class<?> c = HookApi.findClass(cl, hn);
            if (c == null) continue;
            for (final Method m : c.getDeclaredMethods()) {
                Class<?>[] ps = m.getParameterTypes();
                if (ps.length != 4) continue;
                if (ps[0] != int.class || ps[2] != boolean.class || ps[3] != float[].class) continue;
                if (!View.class.isAssignableFrom(m.getReturnType())) continue;
                hookBefore(m, new HookApi.Action() {
                    @Override public void call(XC_MethodHook.MethodHookParam p) {
                        try {
                            Object f = p.args[1];
                            if (f == null) return;
                            Object[] rec;
                            synchronized (MENU_ITEMS) { rec = MENU_ITEMS.get(f); }
                            if (rec == null) return;
                            Context c = ctx;
                            if (p.thisObject instanceof View) {
                                c = ((View) p.thisObject).getContext();
                            }
                            String title = (String) rec[1];
                            p.setResult(makeMenuView(c, rec[0], title));
                        } catch (Throwable t) {
                            HookLog.e("菜单渲染失败", t);
                        }
                    }
                });
            }
        }
    }

    private View makeMenuView(final Context c, final Object msgItem, final String title) {
        float d = c.getResources().getDisplayMetrics().density;
        LinearLayout box = new LinearLayout(c);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setPadding((int) (6 * d), (int) (6 * d), (int) (6 * d), (int) (6 * d));
        android.widget.ImageView iv = new android.widget.ImageView(c);
        try {
            iv.setImageDrawable(c.getResources().getDrawable(
                    com.cathelperawa.owo.app.R.drawable.ic_module, null));
        } catch (Throwable ignore) {
        }
        box.addView(iv, new LinearLayout.LayoutParams((int) (26 * d), (int) (26 * d)));
        TextView tv = new TextView(c);
        tv.setText(title);
        tv.setTextColor(0xFF9AA7B8);
        tv.setTextSize(11);
        tv.setSingleLine(true);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        tp.topMargin = (int) (3 * d);
        box.addView(tv, tp);
        box.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (T_EDIT.equals(title)) openEditDialog(c, msgItem);
            }
        });
        box.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        return box;
    }

    private void openEditDialog(final Context c, Object msgItem) {
        try {
            final Object rec = recordOf(msgItem);
            if (rec == null) { toast("没拿到消息"); return; }
            final long msgId = Refl.lng(Refl.field(rec, "msgId"), 0);
            Object elsObj = Refl.field(rec, "elements");
            if (!(elsObj instanceof List)) { toast("这条没有文本"); return; }
            List<?> els = (List<?>) elsObj;

            final Map<Integer, String> items = new LinkedHashMap<Integer, String>();
            for (int i = 0; i < els.size(); i++) {
                Object te = Refl.field(els.get(i), "textElement");
                if (te == null) continue;
                int atType = Refl.i32(Refl.field(te, "atType"), 0);
                Object content = Refl.field(te, "content");
                if (atType == 0 && content != null) items.put(i, String.valueOf(content));
            }

            final float d = c.getResources().getDisplayMetrics().density;
            LinearLayout container = CatPanel.fieldBox(c);
            final Integer[] keys = items.keySet().toArray(new Integer[0]);
            final EditText[] edits = new EditText[keys.length];

            if (items.isEmpty()) {
                container.addView(CatPanel.emptyState(c, "这条消息没有可修改的文本喵～"));
            }
            for (int i = 0; i < keys.length; i++) {
                String orig = items.get(keys[i]);

                String label = keys.length > 1 ? ("第 " + (i + 1) + " 段") : null;

                LinearLayout box = CatPanel.field(c, i + 1, orig, "", label);
                EditText et = CatPanel.fieldEdit(box);
                edits[i] = et;
                LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                if (i > 0) bp.topMargin = (int) (12 * d);
                container.addView(box, bp);
            }

            if (keys.length == 1) {
                android.widget.TextView note = new android.widget.TextView(c);
                note.setText("原文： " + items.get(keys[0]));
                note.setTextSize(11);
                note.setTextColor(CatPanel.C_HINT);
                note.setPadding(0, (int) (10 * d), 0, 0);
                note.setMaxLines(2);
                note.setEllipsize(android.text.TextUtils.TruncateAt.END);
                container.addView(note);
            }

            CatPanel.show(c, "仅本地生效", "改本地文本",
                    items.isEmpty() ? "长按消息即可改写显示内容"
                            : "留空则保持原文 · 滑动或重进聊天生效",
                    container, "确定", new CatPanel.OnOk() {
                        @Override public void run() {
                            if (items.isEmpty()) return;
                            Map<Integer, String> result = new LinkedHashMap<Integer, String>();
                            for (int i = 0; i < edits.length; i++) {
                                String s = edits[i].getText() == null
                                        ? "" : edits[i].getText().toString();
                                if (s.length() > 0) result.put(keys[i], s);
                            }
                            if (result.isEmpty()) return;
                            synchronized (EDIT_TABLE) { EDIT_TABLE.put(msgId, result); }
                            MsgStore.add("[改文本] msgId=" + msgId + " 改 " + result.size() + " 段");
                            toast("改好啦～滑动或重进聊天刷新喵");
                        }
                    });
        } catch (Throwable t) {
            HookLog.e("改文本对话框失败", t);
        }
    }

    private static String shorten(String s) {
        if (s == null) return "";
        return s.length() <= 42 ? s : s.substring(0, 39) + "...";
    }

    private static boolean hasEditableText(Object rec) {
        Object elsObj = Refl.field(rec, "elements");
        if (!(elsObj instanceof List)) return false;
        for (Object el : (List<?>) elsObj) {
            Object te = Refl.field(el, "textElement");
            if (te != null && Refl.i32(Refl.field(te, "atType"), 0) == 0) return true;
        }
        return false;
    }

    private static Object recordOf(Object msgItem) {
        if (msgItem == null) return null;
        try {
            Method g = msgItem.getClass().getMethod("getMsgRecord");
            g.setAccessible(true);
            return g.invoke(msgItem);
        } catch (Throwable t) {
            return null;
        }
    }

    private static void toast(String s) {
        try { CatToast.show(s); } catch (Throwable ignore) {}
    }

    private static void ui(Runnable r) {
        try {
            new android.os.Handler(android.os.Looper.getMainLooper()).post(r);
        } catch (Throwable t) {
            r.run();
        }
    }

    private static Object buildMenuItem(Class<?> menuD, Context c, Object msgItem) {
        try {
            java.lang.reflect.Constructor<?> k =
                    menuD.getDeclaredConstructor(Context.class, msgItem.getClass());
            k.setAccessible(true);
            return k.newInstance(c, msgItem);
        } catch (Throwable ignore) {
        }
        for (java.lang.reflect.Constructor<?> k : menuD.getDeclaredConstructors()) {
            try {
                k.setAccessible(true);
                Class<?>[] ps = k.getParameterTypes();
                Object[] as = new Object[ps.length];
                for (int i = 0; i < ps.length; i++) {
                    if (ps[i].isInstance(msgItem)) as[i] = msgItem;
                    else if (c != null && ps[i].isInstance(c)) as[i] = c;
                    else if (ps[i] == String.class) as[i] = T_EDIT;
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
            for (java.lang.reflect.Field f : c.getDeclaredFields()) {
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
            for (java.lang.reflect.Field f : c.getDeclaredFields()) {
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
            for (java.lang.reflect.Field f : c.getDeclaredFields()) {
                if (f.getType() != type) continue;
                try {
                    f.setAccessible(true);
                    f.set(o, val);

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
}