package com.cathelperawa.owo.app.bridge;

import com.cathelperawa.owo.app.core.HookLog;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public final class MsgSender {

    private MsgSender() {}

    private static volatile long lastSentMsgId = 0L;

    public static long lastSentMsgId() { return lastSentMsgId; }

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
            target.invoke(svc, msgId, contact, list, new HashMap<Integer, Object>(), null);
            HookLog.i("已发: n=" + list.size() + " msgId=" + msgId);
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
            target.invoke(svc, msgId, contact, list, new HashMap<Integer, Object>(), null);
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
                invokeRet = target.invoke(sender, msgId, contact, list, new HashMap<Integer, Object>(), cb);
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
