package com.cathelperawa.owo.app.ovo;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import de.robv.android.xposed.XposedHelpers;

public final class QQInterfaces {

    private static Object app;
    private static Class<?> appCls;
    private static Class<?> tsmCls;

    private QQInterfaces() {}

    public static Object getApp() {
        return app;
    }

    public static void update(ClassLoader cl) {
        if (app != null) return;
        try {
            appCls = XposedHelpers.findClassIfExists("com.tencent.common.app.AppInterface", cl);
            tsmCls = XposedHelpers.findClassIfExists(
                    "com.tencent.qphone.base.remote.ToServiceMsg", cl);

            try {
                Class<?> bai = XposedHelpers.findClassIfExists(
                        "com.tencent.common.app.BaseApplicationImpl", cl);
                if (bai != null) {
                    Object base = XposedHelpers.callStaticMethod(bai, "getApplication");
                    if (base != null) {
                        Method g = bai.getMethod("getRuntime");
                        g.setAccessible(true);
                        Object rt = g.invoke(base);
                        if (rt != null) app = rt;
                    }
                }
            } catch (Throwable ignore) {
            }

            if (app == null) {
                try {
                    Class<?> mqCls = XposedHelpers.findClassIfExists("mqq.app.MobileQQ", cl);
                    if (mqCls != null) {
                        Object mqq = XposedHelpers.callStaticMethod(mqCls, "getMobileQQ");
                        if (mqq != null) {
                            for (Method m : mqCls.getMethods()) {
                                if (!"waitAppRuntime".equals(m.getName())) continue;
                                try {
                                    m.setAccessible(true);
                                    Class<?>[] ps = m.getParameterTypes();
                                    Object rt = ps.length == 0 ? m.invoke(mqq)
                                            : m.invoke(mqq, new Object[]{null});
                                    if (rt != null) {
                                        app = rt;
                                        break;
                                    }
                                } catch (Throwable ignore) {
                                }
                            }
                        }
                    }
                } catch (Throwable ignore) {
                }
            }
        } catch (Throwable t) {
            com.cathelperawa.owo.app.core.HookLog.e("QQInterfaces.update 失败", t);
        }
        com.cathelperawa.owo.app.core.HookLog.i("QQInterfaces.app = "
                + (app == null ? "null" : app.getClass().getName()));
    }

    private static Object createToServiceMsg(String cmd) throws Exception {
        Constructor<?> k = tsmCls.getConstructor(String.class, String.class, String.class);
        Method getUin = appCls.getMethod("getCurrentAccountUin");
        String uin = (String) getUin.invoke(app);
        return k.newInstance("mobileqq.service", uin, cmd);
    }

    public static void sendBuffer(String cmd, boolean z, byte[] bArr) {
        try {
            if (app == null) {

                update(com.cathelperawa.owo.app.bridge.QQEnv.classLoader());
            }
            if (app == null || tsmCls == null) {
                com.cathelperawa.owo.app.core.HookLog.w("QQInterfaces 未初始化，sendBuffer 放弃");
                return;
            }
            Object msg = createToServiceMsg(cmd);
            Method putWup = tsmCls.getMethod("putWupBuffer", byte[].class);
            putWup.invoke(msg, (Object) bArr);
            Method addAttr = tsmCls.getMethod("addAttribute", String.class, Object.class);
            addAttr.invoke(msg, "req_pb_protocol_flag", Boolean.valueOf(z));

            try {
                Method needCb = tsmCls.getMethod("setNeedCallback", boolean.class);
                needCb.invoke(msg, Boolean.TRUE);
            } catch (Throwable ignore) {
            }
            Method send = appCls.getMethod("sendToService", tsmCls);
            send.setAccessible(true);
            send.invoke(app, msg);
        } catch (Throwable t) {
            com.cathelperawa.owo.app.core.HookLog.e("sendBuffer 失败", t);
        }
    }
}
