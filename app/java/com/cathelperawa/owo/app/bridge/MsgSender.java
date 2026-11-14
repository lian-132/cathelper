package com.cathelperawa.owo.app.bridge;

import com.cathelperawa.owo.app.core.HookLog;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public final class MsgSender {

    private MsgSender() {}

    @SuppressWarnings("unchecked")
    private static HashMap<Integer, Object> buildAttrs() {
        HashMap<Integer, Object> map = new HashMap<Integer, Object>();
        try {
            ClassLoader cl = com.cathelperawa.owo.app.bridge.QQEnv.classLoader();
            Object plate;
            try {
                plate = newInstance(cl, "com.tencent.qqnt.kernel.nativeinterface.VASMsgNamePlate",
                        new Class[]{int.class, int.class, int.class, int.class, int.class,
                                int.class, int.class, int.class, java.util.ArrayList.class, int.class, int.class},
                        new Object[]{258, 64, 0, 0, 0, 0, 258, 0, new java.util.ArrayList(), 0, 0});
            } catch (Throwable e1) {
                plate = newInstance(cl, "com.tencent.qqnt.kernel.nativeinterface.VASMsgNamePlate",
                        new Class[]{int.class, int.class, int.class, int.class, int.class,
                                int.class, int.class, int.class, java.util.ArrayList.class, int.class, int.class, int.class},
                        new Object[]{258, 64, 0, 0, 0, 0, 258, 0, new java.util.ArrayList(), 0, 0, 0});
            }
            Object bubble = newInstance(cl, "com.tencent.qqnt.kernel.nativeinterface.VASMsgBubble",
                    new Class[]{int.class, int.class, int.class, int.class}, new Object[]{0, 0, 0, 0});
            Object pendant = newInstance(cl, "com.tencent.qqnt.kernel.nativeinterface.VASMsgAvatarPendant",
                    new Class[]{}, new Object[]{});
            Object font = newInstance(cl, "com.tencent.qqnt.kernel.nativeinterface.VASMsgFont",
                    new Class[]{int.class, long.class, int.class, int.class, int.class},
                    new Object[]{65536, 0L, 0, 0, 0});
            Object ice = newInstance(cl, "com.tencent.qqnt.kernel.nativeinterface.VASMsgIceBreak",
                    new Class[]{Integer.class, Integer.class}, new Object[]{null, null});
            Object vas = newInstance(cl, "com.tencent.qqnt.kernel.nativeinterface.VASMsgElement",
                    new Class[]{
                            Class.forName("com.tencent.qqnt.kernel.nativeinterface.VASMsgNamePlate", true, cl),
                            Class.forName("com.tencent.qqnt.kernel.nativeinterface.VASMsgBubble", true, cl),
                            Class.forName("com.tencent.qqnt.kernel.nativeinterface.VASMsgAvatarPendant", true, cl),
                            Class.forName("com.tencent.qqnt.kernel.nativeinterface.VASMsgFont", true, cl),
                            Class.forName("com.tencent.qqnt.kernel.nativeinterface.VASMsgIceBreak", true, cl)},
                    new Object[]{plate, bubble, pendant, font, ice});
            Class<?> infoC = Class.forName("com.tencent.qqnt.kernel.nativeinterface.MsgAttributeInfo", true, cl);
            for (java.lang.reflect.Constructor<?> c : infoC.getDeclaredConstructors()) {
                Class<?>[] pt = c.getParameterTypes();
                if (pt.length < 1) continue;
                Object[] args = new Object[pt.length];
                for (int i = 0; i < pt.length; i++) {
                    if (i == 0 || i == 1) args[i] = 0;
                    else if (i == 2) args[i] = vas;
                    else args[i] = null;
                }
                c.setAccessible(true);
                map.put(0, c.newInstance(args));
                break;
            }
        } catch (Throwable t) {
            com.cathelperawa.owo.app.core.HookLog.w("buildAttrs 失败: " + t);
        }
        return map;
    }

    private static Object newInstance(ClassLoader cl, String cls, Class<?>[] sig, Object[] args) throws Exception {
        Class<?> c = Class.forName(cls, true, cl);
        java.lang.reflect.Constructor<?> ctor = c.getConstructor(sig);
        ctor.setAccessible(true);
        return ctor.newInstance(args);
    }

    private static volatile long lastSentMsgId = 0L;

    public static long lastSentMsgId() { return lastSentMsgId; }

    private static volatile int lastResult = -999;
    public static int lastResult() { return lastResult; }

    private static volatile Class<?> cachedSendMsgOwner;
    private static volatile Method cachedSendMsg;

    public static boolean send(Object contact, List<Object> elements) {
        try {
            Object svc = QQEnv.kernelMsgService();
            if (svc == null) { HookLog.w("拿不到 MsgService"); return false; }
            if (elements == null || elements.isEmpty()) { HookLog.w("元素为空"); return false; }

            long msgId = 0L;

            for (Method m : svc.getClass().getMethods()) {
                if (m.getName().toLowerCase().contains("uniqueid")) {
                    HookLog.i("UniqueId 候选: " + m);
                    try { com.cathelperawa.owo.app.MsgStore.add("UNIQ " + m); } catch (Throwable ignore) {}
                }
            }

            try {
                java.util.TreeSet<String> names = new java.util.TreeSet<String>();
                for (Method m : svc.getClass().getMethods()) names.add(m.getName());
                com.cathelperawa.owo.app.MsgStore.add("MSGSVC_METHODS " + names);
            } catch (Throwable ignore) {}

            for (Method m : svc.getClass().getMethods()) {
                if (!"generateMsgUniqueId".equals(m.getName())) continue;
                if (m.getParameterTypes().length != 2) continue;
                try {
                    m.setAccessible(true);
                    Object v = m.invoke(svc, 2, System.currentTimeMillis());
                    if (v instanceof Number) msgId = ((Number) v).longValue();
                } catch (Throwable ignore) {}
                if (msgId != 0L) break;
            }
            if (msgId == 0L) {
                Method uniq = findMethod(svc.getClass(), "getMsgUniqueId", int.class);
                if (uniq != null) {
                    uniq.setAccessible(true);
                    Object v = uniq.invoke(svc, 2);
                    if (v instanceof Number) msgId = ((Number) v).longValue();
                }
            }

            long eidBase = 0L;
            try {
                for (Method m : svc.getClass().getMethods()) {
                    if (!"generateMsgUniqueId".equals(m.getName())) continue;
                    if (m.getParameterTypes().length != 2) continue;
                    m.setAccessible(true);
                    Object v = m.invoke(svc, 2, System.currentTimeMillis());
                    if (v instanceof Number && ((Number) v).longValue() != 0L) {
                        eidBase = ((Number) v).longValue();
                        break;
                    }
                }
            } catch (Throwable ignore) {}
            if (eidBase == 0L) eidBase = msgId;

            Method target = null;
            Method fallback = null;
            for (Method m : svc.getClass().getMethods()) {
                if (!"sendMsg".equals(m.getName())) continue;
                Class<?>[] ps = m.getParameterTypes();
                if (ps.length != 5) continue;
                if (ps[0] != long.class && ps[0] != Long.class) continue;
                String cbName = ps[4].getName();
                if (cbName.contains("IMsgOperateCallback")) { target = m; break; }
                if (fallback == null) fallback = m;
            }
            if (target == null) target = fallback;
            if (target == null) { HookLog.w("找不到 sendMsg"); return false; }
            HookLog.i("sendMsg 签名: " + target);

            ArrayList<Object> list = new ArrayList<Object>(elements);
            target.setAccessible(true);

            try {
                for (int i = 0; i < list.size(); i++) {
                    Object el = list.get(i);
                    if (el == null) continue;
                    java.lang.reflect.Field f = findField(el.getClass(), "elementId");
                    if (f != null && f.getLong(el) == 0L) {
                        f.setLong(el, eidBase + i);
                        HookLog.i("elementId 补: " + f.getLong(el));
                    }
                }
            } catch (Throwable t) { HookLog.w("补 elementId 失败: " + t); }

            Object cb = null;
            try {
                Class<?> cbCls = target.getParameterTypes()[4];
                if (cbCls != null && cbCls.isInterface()) {
                    cb = java.lang.reflect.Proxy.newProxyInstance(
                            cbCls.getClassLoader(), new Class<?>[]{cbCls},
                            new java.lang.reflect.InvocationHandler() {
                                @Override public Object invoke(Object p, Method m, Object[] a) {
                                    String s = "sendMsg回调 " + m.getName() + " "
                                            + (a == null ? "[]" : java.util.Arrays.toString(a));
                                    if ("onResult".equals(m.getName()) && a != null && a.length > 0
                                            && a[0] instanceof Number) {
                                        lastResult = ((Number) a[0]).intValue();
                                    }
                                    HookLog.i(s);
                                    try { com.cathelperawa.owo.app.MsgStore.add("[send] " + s); }
                                    catch (Throwable ignore) {}
                                    Class<?> rt = m.getReturnType();
                                    if (rt == void.class) return null;
                                    if (rt == boolean.class) return Boolean.FALSE;
                                    if (rt == int.class) return 0;
                                    if (rt == long.class) return 0L;
                                    return null;
                                }
                            });
                }
            } catch (Throwable t) { HookLog.w("回调代理失败: " + t); }

            lastResult = -998;
            target.invoke(svc, msgId, contact, list, buildAttrs(), cb);
            HookLog.i("已发: n=" + list.size() + " msgId=" + msgId + " cb=" + (cb != null));
            return true;
        } catch (Throwable t) {
            HookLog.e("发送失败", t);
            return false;
        }
    }

    public static long sendReturnId(Object contact, List<Object> elements) {
        try {
            Object svc = QQEnv.msgServiceImpl();
            if (svc == null) svc = QQEnv.kernelMsgService();
            if (svc == null) return 0L;
            if (elements == null || elements.isEmpty()) return 0L;

            long msgId = 0L;
            Object idSvc = QQEnv.kernelMsgService();
            if (idSvc != null) {
                for (Method m : idSvc.getClass().getMethods()) {
                    if (!"generateMsgUniqueId".equals(m.getName())) continue;
                    if (m.getParameterTypes().length != 2) continue;
                    try {
                        m.setAccessible(true);
                        Object v = m.invoke(idSvc, 2, System.currentTimeMillis());
                        if (v instanceof Number) msgId = ((Number) v).longValue();
                    } catch (Throwable ignore) {}
                    if (msgId != 0L) break;
                }
            }
            if (msgId == 0L) return 0L;

            Method target = null;
            for (Method m : svc.getClass().getMethods()) {
                if (!"sendMsg".equals(m.getName())) continue;
                Class<?>[] ps = m.getParameterTypes();
                if (ps.length == 5 && (ps[0] == long.class || ps[0] == Long.class)) {
                    target = m; break;
                }
            }
            if (target == null) return 0L;
            target.setAccessible(true);
            ArrayList<Object> list = new ArrayList<Object>(elements);
            target.invoke(svc, msgId, contact, list, buildAttrs(), null);
            HookLog.i("sendReturnId: msgId=" + msgId + " cls=" + svc.getClass().getName());
            return msgId;
        } catch (Throwable t) {
            HookLog.e("sendReturnId 失败", t);
            return 0L;
        }
    }

    public static Object sendAndAwait(Object contact, List<Object> elements, long timeoutMs) {
        try {
            Object svc = QQEnv.kernelMsgService();
            if (svc == null) { HookLog.w("拿不到 MsgService"); return null; }
            if (elements == null || elements.isEmpty()) return null;

            long msgId = 0L;

            for (Method m : svc.getClass().getMethods()) {
                if (!"generateMsgUniqueId".equals(m.getName())) continue;
                if (m.getParameterTypes().length != 2) continue;
                try {
                    m.setAccessible(true);
                    Object v = m.invoke(svc, 2, System.currentTimeMillis());
                    if (v instanceof Number) msgId = ((Number) v).longValue();
                } catch (Throwable ignore) {}
                if (msgId != 0L) break;
            }
            if (msgId == 0L) {
                Method uniq = findMethod(svc.getClass(), "getMsgUniqueId", int.class);
                if (uniq != null) {
                    uniq.setAccessible(true);
                    Object v = uniq.invoke(svc, 2);
                    if (v instanceof Number) msgId = ((Number) v).longValue();
                }
            }

            Object sender = QQEnv.msgServiceImpl();
            if (sender == null) sender = svc;

            Class<?> senderCls = sender.getClass();
            if (cachedSendMsgOwner != senderCls) {
                Method found = null;
                Method fb = null;
                for (Method m : senderCls.getMethods()) {
                    if (!"sendMsg".equals(m.getName())) continue;
                    Class<?>[] ps = m.getParameterTypes();
                    if (ps.length != 5) continue;
                    if (ps[0] != long.class && ps[0] != Long.class) continue;
                    String cbName = ps[4].getName();
                    if (cbName.contains("IMsgOperateCallback")) { found = m; break; }
                    if (fb == null) fb = m;
                }
                if (found == null) found = fb;
                cachedSendMsgOwner = senderCls;
                cachedSendMsg = found;
            }
            Method target = cachedSendMsg;
            if (target == null) { HookLog.w("找不到 sendMsg"); return null; }

            Class<?> cbCls = target.getParameterTypes()[4];
            final Object[] holder = new Object[1];
            Object cb = java.lang.reflect.Proxy.newProxyInstance(
                    svc.getClass().getClassLoader(), new Class<?>[]{cbCls},
                    new java.lang.reflect.InvocationHandler() {
                        @Override public Object invoke(Object p, Method m, Object[] a) {
                            try {
                                if (a != null) {
                                    for (Object o : a) {
                                        if (o == null) continue;

                                        if (o.getClass().getName().contains("MsgRecord")) {
                                            holder[0] = o;
                                        } else if (o instanceof java.util.List) {

                                            java.util.List<?> l = (java.util.List<?>) o;
                                            if (!l.isEmpty()) holder[0] = l.get(0);
                                        }
                                    }
                                }
                            } catch (Throwable ignore) {}
                            Class<?> rt = m.getReturnType();
                            if (rt == boolean.class) return Boolean.FALSE;
                            if (rt == int.class) return 0;
                            if (rt == long.class) return 0L;
                            return null;
                        }
                    });

            ArrayList<Object> list = new ArrayList<Object>(elements);
            target.setAccessible(true);
            Object invokeRet = null;
            Throwable invokeErr = null;
            try {
                target.setAccessible(true);
                invokeRet = target.invoke(sender, msgId, contact, list, buildAttrs(), cb);
            } catch (Throwable t) { invokeErr = t; }
            HookLog.i("已发(await): n=" + list.size() + " msgId=" + msgId
                    + " ret=" + invokeRet + " err=" + invokeErr);
            try {
                com.cathelperawa.owo.app.MsgStore.add("AWAIT_INVOKE msgId=" + msgId
                        + " ret=" + invokeRet + " err=" + invokeErr
                        + " cbCls=" + cbCls.getName());
            } catch (Throwable ignore) {}
            try {
                com.cathelperawa.owo.app.MsgStore.add("AWAIT_SEND msgId=" + msgId
                        + " sig=" + target + " cbCls=" + cbCls);
            } catch (Throwable ignore) {}

            lastSentMsgId = msgId;
            HookLog.i("sendAndAwait: 已发 msgId=" + msgId + "（不等待回调）");
            return null;
        } catch (Throwable t) {
            HookLog.e("sendAndAwait 失败", t);
            return null;
        }
    }

    private static java.lang.reflect.Field findField(Class<?> cls, String name) {
        Class<?> c = cls;
        while (c != null && c != Object.class) {
            try {
                java.lang.reflect.Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                return f;
            } catch (Throwable ignore) {}
            c = c.getSuperclass();
        }
        return null;
    }

    private static Method findMethod(Class<?> cls, String name, Class<?>... params) {
        if (cls == null) return null;
        for (Method m : cls.getDeclaredMethods()) {
            if (!m.getName().equals(name)) continue;
            if (params.length == 0) return m;
            Class<?>[] ps = m.getParameterTypes();
            if (ps.length != params.length) continue;
            boolean ok = true;
            for (int i = 0; i < ps.length; i++) {
                if (params[i] != null && !params[i].isAssignableFrom(ps[i])
                        && !ps[i].isAssignableFrom(params[i])) { ok = false; break; }
            }
            if (ok) return m;
        }
        return null;
    }
}
