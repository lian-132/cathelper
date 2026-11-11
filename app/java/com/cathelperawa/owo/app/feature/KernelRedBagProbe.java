package com.cathelperawa.owo.app.feature;

import com.cathelperawa.owo.app.core.dex.ClassResolver;
import com.cathelperawa.owo.app.MsgStore;
import com.cathelperawa.owo.app.core.HookApi;
import com.cathelperawa.owo.app.core.HookLog;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public final class KernelRedBagProbe {

    private KernelRedBagProbe() {}

    private static final char[] HEXC = "0123456789abcdef".toCharArray();

    private static final String CLS_SERVICE =
            "com.tencent.qqnt.kernel.nativeinterface.IKernelMsgService";
    private static final String CLS_CPPPROXY =
            "com.tencent.qqnt.kernel.nativeinterface.IKernelMsgService$CppProxy";
    private static final String CLS_REQ =
            "com.tencent.qqnt.kernel.nativeinterface.GrabRedBagReq";
    private static final String CLS_CB =
            "com.tencent.qqnt.kernel.nativeinterface.IGrabRedBagCallback";

    private static volatile Object lastReq;

    

    public static void install(ClassLoader cl) {
        int n = 0;
        n += hookCppProxy(cl);
        n += hookCppProxyRedBagRsp(cl);
        n += dumpInterface(cl);
        HookLog.i("KernelRedBagProbe: 挂 " + n + " 个点");
        try {
            MsgStore.add("[内核红包探针] 已加载（挂 " + n + " 个点）");
        } catch (Throwable ignore) {}
    }

    private static int hookCppProxy(ClassLoader cl) {
        try {
            Class<?> cp = HookApi.findClass(cl, CLS_CPPPROXY);
            if (cp == null) { HookLog.w("KernelRedBagProbe: 找不到 CppProxy"); return 0; }
            int n = 0;
            for (final Method m : cp.getDeclaredMethods()) {
                String nm = m.getName();
                if (!("grabRedBag".equals(nm) || "selectPasswordRedBag".equals(nm)
                        || "packRedBag".equals(nm))) continue;
                HookApi.before(m, new HookApi.Action() {
                    @Override public void call(
                            de.robv.android.xposed.XC_MethodHook.MethodHookParam p) {
                        try {
                            StringBuilder sb = new StringBuilder("[内核红包★] ")
                                    .append(m.getName()).append(" 被调用\n");
                            if (p.args != null && p.args.length > 0 && p.args[0] != null
                                    && !(p.args[0] instanceof Long)) {
                                lastReq = p.args[0];
                                sb.append(dumpReq(p.args[0]));
                            } else if (p.args != null && p.args.length > 1 && p.args[1] != null) {
                                lastReq = p.args[1];
                                sb.append(dumpReq(p.args[1]));
                            }
                            MsgStore.add(sb.toString());
                        } catch (Throwable t) {
                            HookLog.e("KernelRedBagProbe 异常", t);
                        }
                    }
                });
                n++;
            }
            return n > 0 ? 1 : 0;
        } catch (Throwable t) {
            HookLog.e("KernelRedBagProbe.hookCppProxy 失败", t);
            return 0;
        }
    }

    private static int hookCppProxyRedBagRsp(ClassLoader cl) {
        try {
            Class<?> cls = HookApi.findClass(cl,
                    "com.tencent.qqnt.kernel.nativeinterface.GrabRedBagRsp");
            if (cls == null) return 0;
            
            StringBuilder sb = new StringBuilder("[内核红包] GrabRedBagRsp 字段: ");
            for (Field f : cls.getFields()) sb.append(f.getName()).append(' ');
            MsgStore.add(sb.toString());
            return 1;
        } catch (Throwable ignore) {}
        return 0;
    }

    private static int dumpInterface(ClassLoader cl) {
        try {
            Class<?> c = HookApi.findClass(cl, CLS_SERVICE);
            if (c == null) return 0;
            StringBuilder sb = new StringBuilder("[内核红包] IKernelMsgService 红包相关方法:\n");
            for (Method m : c.getMethods()) {
                String nm = m.getName().toLowerCase();
                if (nm.contains("redbag") || nm.contains("red_pack")
                        || nm.contains("password")) {
                    sb.append("  · ").append(m.getName()).append('(');
                    Class<?>[] ts = m.getParameterTypes();
                    for (int i = 0; i < ts.length; i++) {
                        if (i > 0) sb.append(", ");
                        sb.append(ts[i].getSimpleName());
                    }
                    sb.append(")\n");
                }
            }
            MsgStore.add(sb.toString());
            return 1;
        } catch (Throwable t) {
            return 0;
        }
    }

    

    private static String dumpReq(Object req) {
        StringBuilder sb = new StringBuilder();
        sb.append("  类=").append(req.getClass().getSimpleName()).append('\n');
        for (String fn : new String[]{"pcBody", "index", "name", "wishing",
                "recvUin", "peerUid", "recvType", "msgSeq"}) {
            try {
                Field f = findField(req.getClass(), fn);
                if (f == null) { sb.append("  ").append(fn).append(" = <无此字段>\n"); continue; }
                f.setAccessible(true);
                Object v = f.get(req);
                sb.append("  ").append(fn).append(" = ");
                if (v instanceof byte[]) {
                    byte[] b = (byte[]) v;
                    sb.append("byte[").append(b.length).append(']');
                    if (b.length > 0) {
                        sb.append(" hex=");
                        int lim = Math.min(b.length, 160);
                        for (int i = 0; i < lim; i++) sb.append(HEXC[(b[i] >> 4) & 0xF])
                                .append(HEXC[b[i] & 0xF]);
                        if (b.length > lim) sb.append("...");
                    }
                } else {
                    sb.append(String.valueOf(v));
                }
                sb.append('\n');
            } catch (Throwable t) {
                sb.append("  ").append(fn).append(" = <读取失败 ").append(t).append(">\n");
            }
        }
        return sb.toString();
    }

    private static Field findField(Class<?> c, String name) {
        while (c != null) {
            try { return c.getDeclaredField(name); }
            catch (Throwable ignore) { c = c.getSuperclass(); }
        }
        return null;
    }

    

    
    public static String tryGrab(Object svc, ClassLoader cl, String peerUid,
                                 long recvUin, int recvType, String wishing, long msgSeq) {
        try {
            Class<?> clsReq = ClassResolver.find(cl, "rp.grabReq", CLS_REQ);
            Class<?> clsCb = ClassResolver.find(cl, "rp.grabCb", CLS_CB);

            Object cb = Proxy.newProxyInstance(cl, new Class<?>[]{clsCb},
                    new java.lang.reflect.InvocationHandler() {
                        @Override public Object invoke(Object proxy, Method method, Object[] args) {
                            try {
                                String nm = method.getName();
                                if ("toString".equals(nm)) return "CatKernelRedBagCb";
                                if ("hashCode".equals(nm)) return System.identityHashCode(proxy);
                                if ("equals".equals(nm)) return proxy == (args == null ? null : args[0]);
                                MsgStore.add("[内核红包回调] " + nm + " 参数="
                                        + (args == null ? "null" : java.util.Arrays.toString(args)));
                                if (args != null) {
                                    for (Object a : args) {
                                        if (a != null && a.getClass().getSimpleName()
                                                .contains("GrabRedBagRsp")) {
                                            MsgStore.add("[内核红包回调] 响应:\n" + dumpReq(a));
                                        }
                                    }
                                }
                            } catch (Throwable t) {
                                HookLog.e("KernelRedBagProbe 回调异常", t);
                            }
                            return null;
                        }
                    });

            java.lang.reflect.Constructor<?> ctor = clsReq.getConstructor(
                    byte[].class, byte[].class, String.class, String.class,
                    long.class, String.class, int.class, long.class);
            ctor.setAccessible(true);
            Object req = ctor.newInstance(
                    new byte[0], new byte[0],
                    com.cathelperawa.owo.app.bridge.QQEnv.currentNickname(),
                    wishing == null ? "" : wishing,
                    recvUin,
                    peerUid == null ? "" : peerUid,
                    recvType,
                    msgSeq);
            MsgStore.add("[内核红包试用] 请求构造完成:\n" + dumpReq(req));

            Method mg = null;
            for (Method m : svc.getClass().getMethods()) {
                if ("grabRedBag".equals(m.getName()) && m.getParameterTypes().length == 2) {
                    mg = m; break;
                }
            }
            if (mg == null) {
                
                Class<?> isvc = ClassResolver.find(cl, "rp.service", CLS_SERVICE);
                mg = isvc.getMethod("grabRedBag", clsReq, clsCb);
            }
            mg.setAccessible(true);
            mg.invoke(svc, req, cb);
            MsgStore.add("[内核红包试用] grabRedBag 已发起（异步等回调）");
            return "sent";
        } catch (Throwable t) {
            MsgStore.add("[内核红包试用-W] 失败: " + t);
            HookLog.e("KernelRedBagProbe.tryGrab 异常", t);
            return "err:" + t;
        }
    }
}
