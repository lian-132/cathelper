package com.cathelperawa.owo.app.feature;

import com.cathelperawa.owo.app.core.HookLog;
import com.cathelperawa.owo.app.util.Refl;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;

public final class VideoPlayUrlFetcher {

    public interface Callback {
        
        void onUrl(String url, String error);
    }

    private VideoPlayUrlFetcher() {
    }

    public static void fetch(Object msgItem, ClassLoader cl, final Callback cb) {
        try {
            if (msgItem == null || cl == null) {
                cb.onUrl("", "参数为空");
                return;
            }
            Object rec = Refl.call(msgItem, "getMsgRecord");
            if (rec == null) rec = Refl.field(msgItem, "msgRecord");
            if (rec == null) {
                cb.onUrl("", "拿不到 MsgRecord");
                return;
            }

            long msgId = toLong(Refl.field(rec, "msgId"));
            int chatType = (int) toLong(Refl.field(rec, "chatType"));
            String peerUid = Refl.strOr(Refl.field(rec, "peerUid"), "");
            if (peerUid.length() == 0) peerUid = Refl.strOr(Refl.field(rec, "peerUin"), "");

            long elemId = 0;
            Object codec = null;
            Object els = Refl.field(rec, "elements");
            if (!(els instanceof List)) els = Refl.call(rec, "getElements");
            if (els instanceof List) {
                for (Object el : (List<?>) els) {
                    if (el == null) continue;
                    Object v = Refl.field(el, "videoElement");
                    if (v == null) continue;
                    elemId = toLong(Refl.field(el, "elementId"));
                    codec = Refl.field(v, "sourceVideoCodecFormat");
                    break;
                }
            }
            if (elemId == 0) {
                cb.onUrl("", "没有 videoElement");
                return;
            }
            if (chatType == 0) chatType = peerUid.matches("\\d+") ? 2 : 1;

            Class<?> contactCls = Class.forName(
                    "com.tencent.qqnt.kernelpublic.nativeinterface.Contact", false, cl);
            Object contact = null;
            try {
                Constructor<?> ck = contactCls.getConstructor(
                        int.class, String.class, String.class);
                contact = ck.newInstance(chatType, peerUid, "");
            } catch (Throwable ignore) {
            }
            if (contact == null) {
                
                try {
                    Constructor<?> ck2 = contactCls.getConstructor();
                    contact = ck2.newInstance();
                    java.lang.reflect.Field f1 = null, f2 = null;
                    try {
                        f1 = contactCls.getField("chatType");
                    } catch (Throwable ignore) {
                    }
                    try {
                        f2 = contactCls.getField("peerUid");
                    } catch (Throwable ignore) {
                    }
                    if (f1 != null) f1.set(contact, chatType);
                    if (f2 != null) f2.set(contact, peerUid);
                } catch (Throwable t2) {
                    cb.onUrl("", "构造 Contact 失败：" + t2);
                    return;
                }
            }

            Class<?> svcCls = Class.forName(
                    "com.tencent.qqnt.msg.api.IRichMediaService", false, cl);
            Object svc = qrouteApi(cl, svcCls);
            if (svc == null) {
                cb.onUrl("", "拿不到 IRichMediaService");
                return;
            }

            Class<?> cbCls = Class.forName(
                    "com.tencent.qqnt.kernel.nativeinterface.IVideoPlayUrlCallback", false, cl);

            final Callback out = cb;
            Object proxy = Proxy.newProxyInstance(cl, new Class<?>[]{cbCls},
                    new InvocationHandler() {
                        @Override public Object invoke(Object p, Method m, Object[] a) {
                            String name = m.getName();
                            if ("onResult".equals(name) && a != null && a.length >= 3) {
                                String url = "";
                                String err = "";
                                try {
                                    int code = a[0] instanceof Integer ? (Integer) a[0] : -1;
                                    err = a[1] == null ? "" : String.valueOf(a[1]);
                                    url = pickUrl(a[2]);
                                    if (url.length() == 0) {
                                        err = "回调里没有 url (code=" + code + " " + err + ")";
                                    }
                                } catch (Throwable t) {
                                    err = "解析回调失败：" + t;
                                }
                                HookLog.w("VideoPlayUrl: " + (url.length() > 0 ? url : err));
                                out.onUrl(url, err);
                            }
                            return defaultValue(m.getReturnType());
                        }
                    });

            Method call = null;
            for (Method m : svcCls.getMethods()) {
                if (!m.getName().startsWith("getVideoPlayUrl")) continue;
                Class<?>[] ps = m.getParameterTypes();
                if (ps.length == 5) {
                    call = m;
                    break;
                }
            }
            if (call == null) {
                cb.onUrl("", "找不到 getVideoPlayUrl");
                return;
            }
            call.setAccessible(true);
            call.invoke(svc, contact, msgId, elemId, codec, proxy);
            
        } catch (Throwable t) {
            HookLog.e("VideoPlayUrlFetcher 失败", t);
            cb.onUrl("", String.valueOf(t));
        }
    }

    private static Object qrouteApi(ClassLoader cl, Class<?> iface) {
        String[] names = {
                "com.tencent.mobileqq.qroute.QRoute",
                "com.tencent.mobileqq.qroute.n"
        };
        for (String n : names) {
            try {
                Class<?> c = Class.forName(n, false, cl);
                for (Method m : c.getDeclaredMethods()) {
                    if (m.getParameterTypes().length != 1) continue;
                    if (m.getParameterTypes()[0] != Class.class) continue;
                    m.setAccessible(true);
                    Object r = m.invoke(null, iface);
                    if (r != null && iface.isInstance(r)) return r;
                }
            } catch (Throwable ignore) {
            }
        }
        return null;
    }

    private static String pickUrl(Object result) {
        if (result == null) return "";
        for (String f : new String[]{"domainUrl", "v4IpUrl", "v6IpUrl"}) {
            Object list = Refl.field(result, f);
            if (!(list instanceof List)) continue;
            List<?> l = (List<?>) list;
            if (l.isEmpty()) continue;
            Object info = l.get(0);
            String u = Refl.strOr(Refl.field(info, "url"), "");
            if (u != null && u.startsWith("http")) return u;
        }
        return "";
    }

    private static long toLong(Object o) {
        if (o instanceof Number) return ((Number) o).longValue();
        try {
            return Long.parseLong(String.valueOf(o));
        } catch (Throwable t) {
            return 0L;
        }
    }

    private static Object defaultValue(Class<?> t) {
        if (!t.isPrimitive()) return null;
        if (t == boolean.class) return Boolean.FALSE;
        if (t == int.class) return 0;
        if (t == long.class) return 0L;
        if (t == short.class) return (short) 0;
        if (t == byte.class) return (byte) 0;
        if (t == float.class) return 0f;
        if (t == double.class) return 0d;
        if (t == char.class) return (char) 0;
        return null;
    }
}
