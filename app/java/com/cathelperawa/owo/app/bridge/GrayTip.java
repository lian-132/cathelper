package com.cathelperawa.owo.app.bridge;

import com.cathelperawa.owo.app.core.HookLog;
import com.cathelperawa.owo.app.core.dex.ClassResolver;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public final class GrayTip {

    public static final int AIO_AV_C2C_NOTICE = 2021;
    public static final int AIO_AV_GROUP_NOTICE = 2022;

    private GrayTip() {}

    public static boolean add(int chatType, String peerUid, String json) {
        int busiId = (chatType == 2) ? AIO_AV_GROUP_NOTICE : AIO_AV_C2C_NOTICE;
        Object contact = ContactFactory.make(chatType, peerUid, "");
        if (contact == null) { HookLog.w("灰字：Contact 构造失败"); return false; }
        return add(busiId, contact, json);
    }

    public static boolean add(int busiId, Object contact, String json) {
        try {

            Object svc = QQEnv.nativeMsgService();
            if (svc == null) svc = QQEnv.kernelMsgService();
            if (svc == null) { HookLog.w("灰字：内核服务为空"); return false; }

            Method target = null;
            for (Method m : svc.getClass().getMethods()) {
                if ("addLocalJsonGrayTipMsg".equals(m.getName())) { target = m; break; }
            }
            if (target == null) { HookLog.w("灰字：内核无 addLocalJsonGrayTipMsg"); return false; }

            ClassLoader cl = QQEnv.classLoader();
            Object element = makeJsonElement(cl, busiId, json);
            if (element == null) { HookLog.w("灰字：JsonGrayElement 构造失败"); return false; }

            Class<?>[] ps = target.getParameterTypes();
            Object cb = proxy(ps[ps.length - 1], cl);
            target.setAccessible(true);
            target.invoke(svc, contact, element, Boolean.TRUE, Boolean.TRUE, cb);
            HookLog.i("灰字已发送 busiId=" + busiId);
            return true;
        } catch (Throwable t) {
            HookLog.e("灰字发送失败", t);
            return false;
        }
    }

    private static Object makeJsonElement(ClassLoader cl, long busiId, String json) {

        for (String cn : new String[]{
                "com.tencent.qqnt.kernelpublic.nativeinterface.JsonGrayElement",
                "com.tencent.qqnt.kernel.nativeinterface.JsonGrayElement"}) {
            Class<?> c = null;
            try { c = Class.forName(cn, false, cl); } catch (Throwable ignore) {}
            if (c == null) continue;
            Object made = instantiate(c, busiId, json);
            if (made != null) return made;
        }

        try {
            android.content.Context ctx = QQEnv.appContext();
            if (ctx != null) {
                Class<?> c = ClassResolver.resolve(ctx, cl, JSON_GRAY_SPEC);
                if (c != null) {
                    HookLog.w("JsonGrayElement 靠自愈找到：" + c.getName());
                    return instantiate(c, busiId, json);
                }
            }
        } catch (Throwable t) {
            HookLog.e("JsonGrayElement 自愈失败", t);
        }
        return null;
    }

    private static final ClassResolver.Spec JSON_GRAY_SPEC =
            new ClassResolver.Spec("graytip.jsonElement")
                    .candidates("com.tencent.qqnt.kernelpublic.nativeinterface.JsonGrayElement",
                                "com.tencent.qqnt.kernel.nativeinterface.JsonGrayElement")
                    .nameMatches("JsonGrayElement$")
                    .validator(new ClassResolver.Validator() {
                        @Override public boolean accept(Class<?> c) {
                            for (Constructor<?> ct : c.getDeclaredConstructors()) {
                                Class<?>[] ps = ct.getParameterTypes();
                                if (ps.length == 5 && ps[0] == long.class && ps[1] == String.class
                                        && ps[2] == String.class && ps[3] == boolean.class) return true;
                            }
                            return false;
                        }
                    });

    public static String selfTest(ClassLoader cl) {
        try {
            Object o = makeJsonElement(cl, 1L, "{\"icon\":\"\",\"content\":\"\"}");
            return o == null ? null : o.getClass().getName();
        } catch (Throwable t) {
            return null;
        }
    }

    private static Object instantiate(Class<?> c, long busiId, String json) {
        for (Constructor<?> ct : c.getDeclaredConstructors()) {
            Class<?>[] ps = ct.getParameterTypes();
            if (ps.length == 5 && ps[0] == long.class && ps[1] == String.class
                    && ps[2] == String.class && ps[3] == boolean.class) {
                try {
                    ct.setAccessible(true);
                    return ct.newInstance(busiId, json, "", Boolean.FALSE, null);
                } catch (Throwable t) {
                    HookLog.e("JsonGrayElement 实例化失败 " + c.getName(), t);
                }
            }
        }
        return null;
    }

    private static Object proxy(Class<?> cbCls, ClassLoader cl) {
        return Proxy.newProxyInstance(cl, new Class<?>[]{cbCls}, new InvocationHandler() {
            @Override public Object invoke(Object p, Method m, Object[] a) {
                if ("onResult".equals(m.getName()) && a != null && a.length > 0
                        && !(a[0] instanceof Number && ((Number) a[0]).intValue() == 0)) {
                    HookLog.w("灰字回调 onResult " + a[0]);
                }
                Class<?> rt = m.getReturnType();
                if (rt == boolean.class) return Boolean.FALSE;
                if (rt == int.class) return 0;
                if (rt == long.class) return 0L;
                return null;
            }
        });
    }

    public static String buildTextTip(String text) {
        try {
            JSONObject root = new JSONObject();
            root.put("align", "center");
            JSONArray items = new JSONArray();
            items.put(textItem(text));
            root.put("items", items);
            return root.toString();
        } catch (Throwable t) {
            HookLog.e("灰字 JSON 构造失败", t);
            return null;
        }
    }

    public static String buildTextRefTip(String text, String refText, int msgSeq) {
        try {
            JSONObject root = new JSONObject();
            root.put("align", "center");
            JSONArray items = new JSONArray();
            items.put(textItem(text));
            items.put(msgRefItem(refText, msgSeq));
            root.put("items", items);
            return root.toString();
        } catch (Throwable t) {
            HookLog.e("灰字 JSON 构造失败", t);
            return null;
        }
    }

    public static String itemText(String txt) {
        try { return textItem(txt).toString(); } catch (Throwable t) { return null; }
    }

    public static String itemUser(String nick, String uid, String uin) {
        try { return userItem(nick, uid, uin).toString(); } catch (Throwable t) { return null; }
    }

    public static String itemRef(String txt, int msgSeq) {
        try { return msgRefItem(txt, msgSeq).toString(); } catch (Throwable t) { return null; }
    }

    public static String compose(String... items) {
        try {
            JSONObject root = new JSONObject();
            root.put("align", "center");
            JSONArray arr = new JSONArray();
            for (String it : items) if (it != null) arr.put(new JSONObject(it));
            root.put("items", arr);
            return root.toString();
        } catch (Throwable t) {
            HookLog.e("灰字 JSON 组合失败", t);
            return null;
        }
    }

    public static boolean addItems(int chatType, String peerUid, String... items) {
        String json = compose(items);
        return json != null && add(chatType, peerUid, json);
    }

    public static String buildRecallTip(String opNick, String opUid, String opUin,
                                        String senderNick, String senderUid, String senderUin,
                                        int msgSeq) {
        try {
            JSONObject root = new JSONObject();
            root.put("align", "center");
            JSONArray items = new JSONArray();
            items.put(userItem(opNick, opUid, opUin));
            items.put(textItem(" 已撤回 "));

            if (senderUid != null && !senderUid.isEmpty() && !senderUid.equals(opUid)
                    && senderNick != null && !senderNick.isEmpty()) {
                items.put(userItem(senderNick, senderUid, senderUin));
                items.put(textItem("的 "));
            }
            if (msgSeq > 0) {
                items.put(msgRefItem("一条消息", msgSeq));
            } else {

                items.put(textItem("一条消息"));
            }
            items.put(textItem("喵~"));
            root.put("items", items);
            return root.toString();
        } catch (Throwable t) {
            HookLog.e("灰字 JSON 构造失败", t);
            return null;
        }
    }

    private static JSONObject userItem(String nick, String uid, String uin) throws Exception {
        JSONObject o = new JSONObject();
        o.put("type", "qq");

        String nm = (nick == null) ? "" : nick.trim();
        if (nm.isEmpty() || nm.startsWith("u_") || nm.matches("\\d+")) {
            nm = "对方";
        }
        o.put("nm", nm);
        o.put("col", "3");
        o.put("tp", "0");
        if (uid != null && uid.length() > 0) { o.put("uid", uid); o.put("jp", uid); }
        if (uin != null && uin.length() > 0 && !"0".equals(uin)) o.put("uin", uin);
        return o;
    }

    private static JSONObject textItem(String txt) throws Exception {
        JSONObject o = new JSONObject();
        o.put("txt", txt);
        o.put("type", "nor");
        return o;
    }

    private static JSONObject msgRefItem(String txt, int msgSeq) throws Exception {
        JSONObject o = new JSONObject();
        o.put("type", "url");
        o.put("txt", txt);
        o.put("col", "3");
        o.put("local_jp", 58);
        JSONObject param = new JSONObject();
        param.put("seq", msgSeq);
        o.put("param", param);
        return o;
    }
}
