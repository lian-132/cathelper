package com.cathelperawa.owo.app.ovo;

import com.cathelperawa.owo.app.core.dex.ClassResolver;
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
            appCls = ClassResolver.find(cl, "app.interface", "com.tencent.common.app.AppInterface");
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
                    Class<?> mqCls = ClassResolver.find(cl, "mqq.mobileQQ", "mqq.app.MobileQQ");
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

    public static String diag() {
        try {
            update(com.cathelperawa.owo.app.bridge.QQEnv.classLoader());
            StringBuilder sb = new StringBuilder();
            sb.append("AppInterface=").append(app == null ? "无" : "有").append(" · ");
            sb.append("ToServiceMsg=").append(tsmCls == null ? "无" : "有").append(" · ");
            if (app != null) {
                try {
                    Object uin = appCls.getMethod("getCurrentAccountUin").invoke(app);
                    sb.append("uin=").append(uin == null ? "null" : uin).append(" · ");
                } catch (Throwable t) {
                    sb.append("uin取不到(").append(t.getClass().getSimpleName()).append(") · ");
                }
            }
            if (app != null && tsmCls != null) {
                Object msg = createToServiceMsg("dev.test");
                sb.append("构造=").append(msg == null ? "失败" : "成功");
            }
            return sb.toString();
        } catch (Throwable t) {
            return "自检异常: " + t;
        }
    }

    
    public static void sendBuffer(String cmd, boolean z, byte[] bArr, int serviceType) {
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
            
            if (serviceType > 0) {
                try {
                    java.lang.reflect.Field f = null;
                    Class<?> c2 = msg.getClass();
                    while (c2 != null && f == null) {
                        try {
                            f = c2.getDeclaredField("serviceType");
                        } catch (Throwable ignore) {
                            c2 = c2.getSuperclass();
                        }
                    }
                    if (f != null) {
                        f.setAccessible(true);
                        f.setInt(msg, serviceType);
                    } else {
                        Method st = tsmCls.getMethod("setServiceType", int.class);
                        st.invoke(msg, serviceType);
                    }
                } catch (Throwable t) {
                    com.cathelperawa.owo.app.core.HookLog.w("serviceType 设置失败: " + t);
                }
            }
            Method send = appCls.getMethod("sendToService", tsmCls);
            send.setAccessible(true);
            send.invoke(app, new Object[]{msg});
        } catch (Throwable t) {
            com.cathelperawa.owo.app.core.HookLog.e("QQInterfaces.sendBuffer(st) 失败", t);
        }
    }

    public static boolean sendBuffer(String cmd, boolean z, byte[] bArr) {
        try {
            if (app == null) {
                
                update(com.cathelperawa.owo.app.bridge.QQEnv.classLoader());
            }
            if (app == null) {
                com.cathelperawa.owo.app.core.HookLog.w("sendBuffer: AppInterface 为空");
                return false;
            }
            if (tsmCls == null) {
                com.cathelperawa.owo.app.core.HookLog.w("sendBuffer: ToServiceMsg 类没找到");
                return false;
            }
            Object msg = createToServiceMsg(cmd);
            if (msg == null) {
                com.cathelperawa.owo.app.core.HookLog.w("sendBuffer: 构造 ToServiceMsg 失败");
                return false;
            }
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
            return true;
        } catch (Throwable t) {
            com.cathelperawa.owo.app.core.HookLog.e("sendBuffer 失败", t);
            return false;
        }
    }
}
