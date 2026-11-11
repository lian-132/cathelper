package com.cathelperawa.owo.app.bridge;

import com.cathelperawa.owo.app.core.HookLog;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public final class NtMsgApi {

    private NtMsgApi() {}

    private static Object svc() { return QQEnv.kernelMsgService(); }

    public static boolean recall(Object contact, long msgId) {
        return recallSync(contact, msgId, 1500);
    }

    public static boolean recallSync(Object contact, long msgId, long timeoutMs) {
        try {
            Object s = svc();
            if (s == null) return false;
            ArrayList<Long> ids = new ArrayList<Long>();
            ids.add(msgId);

            Method recall = null;
            for (Method m : s.getClass().getMethods()) {
                if (!"recallMsg".equals(m.getName())) continue;
                Class<?>[] p = m.getParameterTypes();
                if (p.length >= 2 && List.class.isAssignableFrom(p[1])) {
                    recall = m;
                    break;
                }
            }
            if (recall == null) return false;

            Class<?>[] ps = recall.getParameterTypes();
            if (ps.length < 3) {                     
                recall.setAccessible(true);
                recall.invoke(s, contact, ids);
                return true;
            }

            Class<?> cbCls = ps[ps.length - 1];
            final boolean[] result = new boolean[1];
            final boolean[] done = new boolean[1];
            Object cb = java.lang.reflect.Proxy.newProxyInstance(
                    s.getClass().getClassLoader(), new Class<?>[]{cbCls},
                    new java.lang.reflect.InvocationHandler() {
                        @Override public Object invoke(Object proxy, Method m, Object[] a) {
                            try {
                                if ("onResult".equals(m.getName()) && a != null && a.length >= 1) {
                                    
                                    Object code = a[0];
                                    if (code instanceof Number) {
                                        int c = ((Number) code).intValue();
                                        result[0] = (c == 0 || c == 1);
                                    }
                                    done[0] = true;
                                }
                            } catch (Throwable ignore) {
                            }
                            Class<?> rt = m.getReturnType();
                            if (rt == boolean.class) return Boolean.FALSE;
                            if (rt == int.class) return 0;
                            if (rt == long.class) return 0L;
                            return null;
                        }
                    });

            recall.setAccessible(true);
            recall.invoke(s, contact, ids, cb);
            
            long deadline = System.currentTimeMillis() + timeoutMs;
            while (!done[0] && System.currentTimeMillis() < deadline) {
                Thread.sleep(20);
            }
            HookLog.i("撤回 msgId=" + msgId + " done=" + done[0] + " ok=" + result[0]);
            return result[0];
        } catch (Throwable t) {
            HookLog.e("撤回失败", t);
        }
        return false;
    }

    public static boolean recall(int chatType, String peerUid, long msgId) {
        Object c = ContactFactory.make(chatType, peerUid, "");
        return c != null && recall(c, msgId);
    }

    public static boolean delete(Object contact, long msgId) {
        try {
            Object s = svc();
            if (s == null) return false;
            ArrayList<Long> ids = new ArrayList<Long>();
            ids.add(msgId);
            for (Method m : s.getClass().getMethods()) {
                if (!"deleteMsg".equals(m.getName())) continue;
                Class<?>[] p = m.getParameterTypes();
                if (p.length >= 2 && List.class.isAssignableFrom(p[1])) {
                    m.setAccessible(true);
                    if (p.length == 3) m.invoke(s, contact, ids, null);
                    else m.invoke(s, contact, ids);
                    return true;
                }
            }
        } catch (Throwable t) {
            HookLog.e("删除失败", t);
        }
        return false;
    }

    public static long generateMsgId(int type) {
        try {
            Object s = svc();
            if (s == null) return 0L;
            for (Method m : s.getClass().getMethods()) {
                if (!"getMsgUniqueId".equals(m.getName())) continue;
                m.setAccessible(true);
                Object v = m.invoke(s, type);
                if (v instanceof Number) return ((Number) v).longValue();
            }
        } catch (Throwable ignore) {
        }
        return 0L;
    }

    public static Object getMsgsByMsgId(Object contact, long msgId) {
        try {
            final Object s = svc();
            if (s == null) return null;
            Method target = null;
            for (Method m : s.getClass().getMethods()) {
                if ("getMsgsByMsgId".equals(m.getName())
                        && m.getParameterTypes().length == 3) {
                    target = m;
                    break;
                }
            }
            if (target == null) return null;
            
            StringBuilder sig = new StringBuilder("[getMsgsByMsgId 签名] ");
            for (Class<?> pt : target.getParameterTypes()) sig.append(pt.getName()).append(" | ");
            HookLog.i(sig.toString());
            com.cathelperawa.owo.app.MsgStore.add(sig.toString());
            Class<?> cbCls = target.getParameterTypes()[2];
            final Object[] holder = new Object[1];
            Object cb = java.lang.reflect.Proxy.newProxyInstance(
                    s.getClass().getClassLoader(), new Class<?>[]{cbCls},
                    new java.lang.reflect.InvocationHandler() {
                        @Override public Object invoke(Object p, Method m, Object[] a) {
                            if ("onResult".equals(m.getName()) && a != null) {

                                for (int i = a.length - 1; i >= 0; i--) {
                                    if (a[i] instanceof java.util.List) { holder[0] = a[i]; break; }
                                }
                            }
                            Class<?> rt = m.getReturnType();
                            if (rt == boolean.class) return Boolean.FALSE;
                            if (rt == int.class) return 0;
                            if (rt == long.class) return 0L;
                            return null;
                        }
                    });
            ArrayList<Long> ids = new ArrayList<Long>();
            ids.add(msgId);
            target.setAccessible(true);
            target.invoke(s, contact, ids, cb);
            for (int i = 0; i < 100 && holder[0] == null; i++) Thread.sleep(10);
            
            Object h = holder[0];
            if (h instanceof java.util.List) {
                java.util.List<?> l = (java.util.List<?>) h;
                return l.isEmpty() ? null : l.get(0);
            }
            return h;
        } catch (Throwable t) {
            HookLog.e("getMsgsByMsgId 失败", t);
            return null;
        }
    }

    public static Object getSingleMsg(Object contact, long msgSeq) {
        try {
            final Object s = svc();
            if (s == null) return null;
            Method target = null;
            for (Method m : s.getClass().getMethods()) {
                if ("getSingleMsg".equals(m.getName())
                        && m.getParameterTypes().length == 3) { target = m; break; }
            }
            if (target == null) return null;
            Class<?> cbCls = target.getParameterTypes()[2];
            final Object[] holder = new Object[1];
            Object cb = java.lang.reflect.Proxy.newProxyInstance(
                    s.getClass().getClassLoader(), new Class<?>[]{cbCls},
                    new java.lang.reflect.InvocationHandler() {
                        @Override public Object invoke(Object p, Method m, Object[] a) {
                            if ("onResult".equals(m.getName()) && a != null && a.length >= 3 && a[2] != null) {
                                Object v = a[2];
                                if (v instanceof List) {
                                    List<?> l = (List<?>) v;
                                    if (!l.isEmpty()) holder[0] = l.get(0);
                                } else {
                                    holder[0] = v;
                                }
                            }
                            Class<?> rt = m.getReturnType();
                            if (rt == boolean.class) return Boolean.FALSE;
                            if (rt == int.class) return 0;
                            if (rt == long.class) return 0L;
                            return null;
                        }
                    });
            target.setAccessible(true);
            target.invoke(s, contact, msgSeq, cb);
            for (int i = 0; i < 100 && holder[0] == null; i++) Thread.sleep(10);
            return holder[0];
        } catch (Throwable t) {
            HookLog.e("getSingleMsg 失败", t);
            return null;
        }
    }

    public static List<?> getMsgsBySeqAndCount(Object contact, long seq, int count) {
        try {
            final Object s = svc();
            if (s == null) return null;
            Method target = null;
            for (Method m : s.getClass().getMethods()) {
                if ("getMsgsBySeqAndCount".equals(m.getName())) { target = m; break; }
            }
            if (target == null) return null;
            Class<?>[] ps = target.getParameterTypes();
            Class<?> cbCls = ps[ps.length - 1];
            final Object[] holder = new Object[1];
            Object cb = java.lang.reflect.Proxy.newProxyInstance(
                    s.getClass().getClassLoader(), new Class<?>[]{cbCls},
                    new java.lang.reflect.InvocationHandler() {
                        @Override public Object invoke(Object p, Method m, Object[] a) {
                            if (a != null) {
                                for (int i = a.length - 1; i >= 0; i--) {
                                    if (a[i] instanceof java.util.List) { holder[0] = a[i]; break; }
                                }
                            }
                            Class<?> rt = m.getReturnType();
                            if (rt == boolean.class) return Boolean.FALSE;
                            if (rt == int.class) return 0;
                            if (rt == long.class) return 0L;
                            return null;
                        }
                    });
            target.setAccessible(true);
            Object[] args = new Object[ps.length];
            boolean seqUsed = false;
            for (int i = 0; i < ps.length; i++) {
                Class<?> t = ps[i];
                if (i == ps.length - 1) { args[i] = cb; break; }
                if (t == long.class) { args[i] = seqUsed ? 1L : seq; seqUsed = true; }
                else if (t == int.class) args[i] = count;
                else if (t == boolean.class) args[i] = Boolean.TRUE;
                else args[i] = contact;
            }
            target.invoke(s, args);
            for (int i = 0; i < 100 && holder[0] == null; i++) Thread.sleep(10);
            return (List<?>) holder[0];
        } catch (Throwable t) {
            HookLog.e("getMsgsBySeqAndCount 失败", t);
            return null;
        }
    }

    public static boolean forward(List<Long> msgIds, Object srcContact, List<Object> dstContacts) {
        try {
            Object s = svc();
            if (s == null) return false;
            for (Method m : s.getClass().getMethods()) {
                if (!"forwardMsg".equals(m.getName())) continue;
                Class<?>[] p = m.getParameterTypes();
                if (p.length == 5) {
                    m.setAccessible(true);
                    m.invoke(s, new ArrayList<Long>(msgIds), srcContact,
                            new ArrayList<Object>(dstContacts),
                            new HashMap<Integer, Object>(), null);
                    return true;
                }
            }
        } catch (Throwable t) {
            HookLog.e("转发失败", t);
        }
        return false;
    }
}
