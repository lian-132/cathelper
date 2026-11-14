package com.cathelperawa.owo.app.bridge;

import android.content.Context;
import com.cathelperawa.owo.app.MsgStore;
import com.cathelperawa.owo.app.core.HookApi;
import com.cathelperawa.owo.app.core.HookLog;
import com.cathelperawa.owo.app.feature.FeatureMsgTime;
import com.cathelperawa.owo.app.util.Refl;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public final class QqMsgTap {
    private static volatile boolean installed = false;

    private QqMsgTap() {
    }

    public static synchronized void install() {
        ClassLoader classLoader = null;
        Object msgService = null;
        synchronized (QqMsgTap.class) {
            if (installed) {
                return;
            }
            try {
                classLoader = QQEnv.classLoader();
                msgService = getMsgService(classLoader);
            } catch (Throwable th) {
                note("install EX " + th);
                HookLog.w("QqMsgTap 注册失败 " + th);
            }
            if (msgService == null) {
                note("no MsgService");
                HookLog.w("QqMsgTap: 拿不到 MsgService");
                return;
            }
            Class<?> findClass = HookApi.findClass(classLoader, "com.tencent.qqnt.kernel.nativeinterface.IKernelMsgListener");
            if (findClass == null) {
                note("no IKernelMsgListener");
                HookLog.w("QqMsgTap: 没有 IKernelMsgListener");
                return;
            }
            Object newProxyInstance = Proxy.newProxyInstance(classLoader, new Class[]{findClass}, new InvocationHandler() {
                @Override
                public Object invoke(Object obj, Method method, Object[] objArr) {
                    if (objArr != null) {
                        try {
                            for (Object obj2 : objArr) {
                                QqMsgTap.harvest(obj2, 0);
                            }
                        } catch (Throwable th2) {
                        }
                    }
                    return QqMsgTap.defaultOf(method.getReturnType());
                }
            });
            try {
                Method method = msgService.getClass().getMethod("addMsgListener", findClass);
                method.setAccessible(true);
                method.invoke(msgService, newProxyInstance);
            } catch (Throwable th) {
                HookLog.w("QqMsgTap 注册 addMsgListener 失败 " + th);
                return;
            }
            installed = true;
            note("install ok iface=" + findClass.getName());
            HookLog.i("QqMsgTap: 监听器已注册");
        }
    }

    private static Object getMsgService(ClassLoader classLoader) {
        Object obj;
        Object invoke = null;
        Class<?> findClass;
        Object invoke2;
        try {
            Object kernelMsgService = QQEnv.kernelMsgService();
            if (kernelMsgService != null) {
                note("svc via QQEnv");
                return kernelMsgService;
            }
        } catch (Throwable th) {
        }
        try {
            Object appRuntime = QQEnv.appRuntime();
            if (appRuntime == null && (findClass = HookApi.findClass(classLoader, "com.tencent.common.app.BaseApplicationImpl")) != null && (invoke2 = findClass.getMethod("getApplication", new Class[0]).invoke(null, new Object[0])) != null) {
                appRuntime = findClass.getMethod("getRuntime", new Class[0]).invoke(invoke2, new Object[0]);
            }
            if (appRuntime != null) {
                for (Method method : appRuntime.getClass().getMethods()) {
                    if (method.getParameterTypes().length == 0 && method.getReturnType().getName().contains("MsgService")) {
                        try {
                            method.setAccessible(true);
                            Object invoke3 = method.invoke(appRuntime, new Object[0]);
                            if (invoke3 != null) {
                                note("svc via runtime." + method.getName());
                                return invoke3;
                            }
                            continue;
                        } catch (Throwable th2) {
                        }
                    }
                }
                note("runtime 上没找到 MsgService 方法，runtime=" + appRuntime.getClass().getName());
            } else {
                note("appRuntime=null");
            }
        } catch (Throwable th3) {
            note("runtime 路径异常 " + th3);
        }
        Class<?> findClass2 = HookApi.findClass(classLoader, "com.tencent.qqnt.kernel.api.impl.MsgService");
        if (findClass2 == null) {
            note("MsgService 类不存在");
            return null;
        }
        try {
            obj = QQEnv.appRuntime();
        } catch (Throwable th4) {
            obj = null;
        }
        for (Method method2 : findClass2.getMethods()) {
            String name = method2.getName();
            if ("getService".equals(name) || "INSTANCE".equals(name) || "getInstance".equals(name)) {
                Class<?>[] parameterTypes = method2.getParameterTypes();
                try {
                    method2.setAccessible(true);
                    if (parameterTypes.length == 0) {
                        invoke = method2.invoke(null, new Object[0]);
                    } else if (parameterTypes.length == 1 && obj != null && parameterTypes[0].isInstance(obj)) {
                        invoke = method2.invoke(null, obj);
                    }
                    if (invoke != null) {
                        note("svc via " + name + "(" + parameterTypes.length + ")");
                        return invoke;
                    }
                    continue;
                } catch (Throwable th5) {
                }
            }
        }
        note("MsgService 实例拿不到");
        return null;
    }

    public static void harvest(Object obj, int i) {
        if (obj == null || i > 4) {
            return;
        }
        if (obj instanceof List) {
            Iterator it = ((List) obj).iterator();
            while (it.hasNext()) {
                harvest(it.next(), i + 1);
            }
            return;
        }
        String name = obj.getClass().getName();
        if (name.contains("MsgInfo") || name.contains("MsgRecord")) {
            record(obj);
            return;
        }
        if (name.contains("Msg")) {
            try {
                Object field = Refl.field(obj, "msgRecord");
                if (field != null) {
                    record(field);
                }
            } catch (Throwable th) {
            }
        }
    }

    private static void record(Object obj) {
        try {
            String str = str(obj, "peerUid", "peerUin");
            int i32 = i32(obj, "chatType");
            String str2 = str(obj, "peerName", "sendNickName", "sendMemberName");
            if (str != null && str.length() != 0) {
                if (str2 == null) {
                    str2 = "";
                }
                MsgStore.notePeer(str, str2, i32 == 2);
                note("peer " + str + " | " + str2 + " | type=" + i32);
            }
        } catch (Throwable th) {
        }
    }

    private static void note(String str) {
        try {
            Context appContext = QQEnv.appContext();
            if (appContext == null) {
                try {
                    appContext = (Context) Class.forName("android.app.ActivityThread").getMethod("currentApplication", new Class[0]).invoke(null, new Object[0]);
                } catch (Throwable th) {
                }
            }
            if (appContext == null) {
                return;
            }
            File file = new File(appContext.getFilesDir(), "msgtap.txt");
            if (file.length() > 32768) {
                return;
            }
            FileOutputStream fileOutputStream = new FileOutputStream(file, true);
            try {
                fileOutputStream.write((new SimpleDateFormat(FeatureMsgTime.DEF_FORMAT, Locale.US).format(new Date()) + " " + str + "\n").getBytes("UTF-8"));
                fileOutputStream.close();
            } catch (Throwable th2) {
                fileOutputStream.close();
                throw th2;
            }
        } catch (Throwable th3) {
        }
    }

    private static String str(Object obj, String... strArr) {
        for (String str : strArr) {
            try {
                Object field = Refl.field(obj, str);
                if (field != null && String.valueOf(field).trim().length() > 0) {
                    return String.valueOf(field);
                }
            } catch (Throwable th) {
            }
        }
        return null;
    }

    private static int i32(Object obj, String str) {
        try {
            Object field = Refl.field(obj, str);
            if (field instanceof Number) {
                return ((Number) field).intValue();
            }
            return 0;
        } catch (Throwable th) {
            return 0;
        }
    }

    public static Object defaultOf(Class<?> cls) {
        if (cls == Void.TYPE || cls == Void.class) {
            return null;
        }
        if (cls == Boolean.TYPE) {
            return Boolean.FALSE;
        }
        if (cls == Integer.TYPE) {
            return 0;
        }
        if (cls == Long.TYPE) {
            return 0L;
        }
        if (cls == Short.TYPE) {
            return (short) 0;
        }
        if (cls == Byte.TYPE) {
            return (byte) 0;
        }
        if (cls == Character.TYPE) {
            return (char) 0;
        }
        if (cls == Float.TYPE) {
            return Float.valueOf(0.0f);
        }
        if (cls == Double.TYPE) {
            return Double.valueOf(0.0d);
        }
        return null;
    }
}
