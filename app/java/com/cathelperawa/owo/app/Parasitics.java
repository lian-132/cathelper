package com.cathelperawa.owo.app;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Parcelable;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.List;

import de.robv.android.xposed.XposedBridge;

public class Parasitics {

    private static final String STUB = "com.tencent.mobileqq.activity.photo.CameraPreviewActivity";
    private static final String EXTRA_PROXY = "ACTIVITY_PROXY_INTENT";

    private static final List<String> INJECT = Arrays.asList(
            "com.cathelperawa.owo.app.ConsoleActivity");

    private static ClassLoader moduleLoader = Parasitics.class.getClassLoader();

    public static void init(Context ctx) {
        try {
            Class<?> at = Class.forName("android.app.ActivityThread");
            Method cur = at.getDeclaredMethod("currentActivityThread");
            cur.setAccessible(true);
            Object thread = cur.invoke(null);

            Field fInst = at.getDeclaredField("mInstrumentation");
            fInst.setAccessible(true);
            Instrumentation base = (Instrumentation) fInst.get(thread);
            fInst.set(thread, new ProxyInstrumentation(base));

            Field fH = at.getDeclaredField("mH");
            fH.setAccessible(true);
            Handler handler = (Handler) fH.get(thread);
            Field fCb = Handler.class.getDeclaredField("mCallback");
            fCb.setAccessible(true);
            Handler.Callback curCb = (Handler.Callback) fCb.get(handler);
            if (curCb == null || !curCb.getClass().getName().equals(
                    ProxyHandlerCallback.class.getName())) {
                fCb.set(handler, new ProxyHandlerCallback(curCb));
            }

            boolean amsOk = false;
            String[][] cands = {
                    {"android.app.ActivityTaskManager", "IActivityTaskManagerSingleton",
                            "android.app.IActivityTaskManager"},
                    {"android.app.ActivityManager", "IActivityTaskManagerSingleton",
                            "android.app.IActivityTaskManager"},
                    {"android.app.ActivityManager", "IActivityManagerSingleton",
                            "android.app.IActivityManager"},
            };
            for (String[] cand : cands) {
                if (amsOk) break;
                try {
                    Class<?> holder = Class.forName(cand[0]);
                    Field fSingleton = holder.getDeclaredField(cand[1]);
                    fSingleton.setAccessible(true);
                    Object singleton = fSingleton.get(null);
                    if (singleton == null) continue;
                    Class<?> singletonClass = Class.forName("android.util.Singleton");
                    Field fInstance = singletonClass.getDeclaredField("mInstance");
                    fInstance.setAccessible(true);

                    try {
                        singletonClass.getMethod("get").invoke(singleton);
                    } catch (Throwable ignore) {
                    }
                    Object origin = fInstance.get(singleton);
                    if (origin == null) continue;
                    Object proxy = Proxy.newProxyInstance(moduleLoader,
                            new Class[]{Class.forName(cand[2])}, new AmsHandler(origin));
                    fInstance.set(singleton, proxy);
                    amsOk = true;
                    ModLog.log("代理已挂: " + cand[0] + "." + cand[1]);
                } catch (Throwable t) {
                    ModLog.log("试挂 " + cand[0] + "." + cand[1] + " 失败: " + t);
                }
            }
            if (!amsOk) ModLog.log("⚠ 所有 AMS 代理都失败，页面无法注入");

            Class<?> at2 = Class.forName("android.app.ActivityThread");
            Method cur2 = at2.getDeclaredMethod("currentActivityThread");
            cur2.setAccessible(true);
            Object thread2 = cur2.invoke(null);
            Field fPm = at2.getDeclaredField("sPackageManager");
            fPm.setAccessible(true);
            Object pmOrigin = fPm.get(thread2);
            Class<?> iPmClass = Class.forName("android.content.pm.IPackageManager");
            PackageManager pm = ctx.getPackageManager();
            Field fPmImpl = pm.getClass().getDeclaredField("mPM");
            fPmImpl.setAccessible(true);
            Object pmProxy = Proxy.newProxyInstance(iPmClass.getClassLoader(),
                    new Class[]{iPmClass}, new PmHandler(pmOrigin));
            fPm.set(thread2, pmProxy);
            fPmImpl.set(pm, pmProxy);
            ModLog.log("Parasitics 初始化完成");
        } catch (Throwable t) {
            ModLog.log("Parasitics 初始化失败: " + t);
        }
    }

    private static class AmsHandler implements InvocationHandler {
        private final Object origin;

        AmsHandler(Object origin) {
            this.origin = origin;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if ("startActivity".equals(method.getName())) {
                for (int i = 0; args != null && i < args.length; i++) {
                    if (args[i] instanceof Intent) {
                        Intent raw = (Intent) args[i];
                        ComponentName cn = raw.getComponent();
                        if (cn != null && INJECT.contains(cn.getClassName())) {
                            Intent wrapper = new Intent();
                            wrapper.setClassName(cn.getPackageName(), STUB);
                            wrapper.putExtra(EXTRA_PROXY, raw);
                            args[i] = wrapper;
                            ModLog.log("startActivity 已替换为桩: " + cn.getClassName());
                        }
                        break;
                    }
                }
            }
            try {
                return method.invoke(origin, args);
            } catch (InvocationTargetException ite) {
                throw ite.getTargetException();
            }
        }
    }

    private static class ProxyHandlerCallback implements Handler.Callback {
        private final Handler.Callback next;

        ProxyHandlerCallback(Handler.Callback next) {
            this.next = next;
        }

        @Override
        public boolean handleMessage(Message msg) {
            if (msg.what == 100) {
                handleLaunchActivity(msg);
            } else if (msg.what == 159) {
                handleTransaction(msg);
            }
            return next != null && next.handleMessage(msg);
        }

        private void handleLaunchActivity(Message msg) {
            try {
                Object record = msg.obj;
                Field fIntent = record.getClass().getDeclaredField("intent");
                fIntent.setAccessible(true);
                Intent intent = (Intent) fIntent.get(record);
                swapIntent(intent, new Swap() {
                    @Override
                    public void set(Object holder, Field f, Intent real) throws Exception {
                        f.set(holder, real);
                    }
                }, record, fIntent);
            } catch (Throwable ignore) {
            }
        }

        private interface Swap {
            void set(Object holder, Field f, Intent real) throws Exception;
        }

        private void swapIntent(Intent wrapper, Swap swap, Object holder, Field f) {
            try {
                if (wrapper == null || !wrapper.hasExtra(EXTRA_PROXY)) return;
                Intent clone = new Intent(wrapper);
                Field fExtras = Intent.class.getDeclaredField("mExtras");
                fExtras.setAccessible(true);
                Object bundle = fExtras.get(clone);
                if (bundle != null) {
                    fExtras.set(clone, bundle.getClass().getMethod("clone").invoke(bundle));
                }
                Parcelable real = clone.getParcelableExtra(EXTRA_PROXY);
                if (real instanceof Intent) {
                    swap.set(holder, f, (Intent) real);
                }
            } catch (Throwable ignore) {
            }
        }

        private void handleTransaction(Message msg) {
            try {
                Object tx = msg.obj;
                if (tx == null) return;
                Method getCallbacks = Class.forName(
                        "android.app.servertransaction.ClientTransaction")
                        .getDeclaredMethod("getCallbacks");
                getCallbacks.setAccessible(true);
                List<?> items = (List<?>) getCallbacks.invoke(tx);
                if (items == null) return;
                for (Object item : items) {
                    if (item.getClass().getName().contains("LaunchActivityItem")) {
                        Field fIntent = item.getClass().getDeclaredField("mIntent");
                        fIntent.setAccessible(true);
                        Intent wrapper = (Intent) fIntent.get(item);
                        if (wrapper == null || !wrapper.hasExtra(EXTRA_PROXY)) continue;
                        Intent clone = (Intent) wrapper.clone();
                        Field fExtras = Intent.class.getDeclaredField("mExtras");
                        fExtras.setAccessible(true);
                        Object bundle = fExtras.get(clone);
                        if (bundle != null) {
                            clone.setExtrasClassLoader(moduleLoader);
                        }
                        Parcelable real = clone.getParcelableExtra(EXTRA_PROXY);
                        if (real instanceof Intent) {
                            fIntent.set(item, real);

                            if (Build.VERSION.SDK_INT >= 31) {
                                try {
                                    IBinder token = (IBinder) tx.getClass()
                                            .getMethod("getActivityToken").invoke(tx);
                                    Class<?> at = Class.forName("android.app.ActivityThread");
                                    Method cur = at.getDeclaredMethod("currentActivityThread");
                                    cur.setAccessible(true);
                                    Object thread = cur.invoke(null);
                                    Object acr = at.getMethod(
                                            "getLaunchingActivity", IBinder.class).invoke(thread, token);
                                    if (acr != null) {
                                        Field fAcr = acr.getClass().getDeclaredField("intent");
                                        fAcr.setAccessible(true);
                                        fAcr.set(acr, real);
                                    }
                                } catch (Throwable ignore) {
                                }
                            }
                        }
                    }
                }
            } catch (Throwable ignore) {
            }
        }
    }

    private static class ProxyInstrumentation extends Instrumentation {
        private final Instrumentation base;

        ProxyInstrumentation(Instrumentation base) {
            this.base = base;
        }

        @Override
        public Activity newActivity(ClassLoader cl, String className, Intent intent)
                throws InstantiationException, IllegalAccessException, ClassNotFoundException {
            try {
                return base.newActivity(cl, className, intent);
            } catch (Exception e) {
                return (Activity) moduleLoader.loadClass(className).newInstance();
            }
        }

        @Override
        public void callActivityOnCreate(Activity activity, Bundle icicle) {
            if (icicle != null && INJECT.contains(activity.getClass().getName())) {
                icicle.setClassLoader(moduleLoader);
            }
            base.callActivityOnCreate(activity, icicle);
        }
    }

    private static class PmHandler implements InvocationHandler {
        private final Object origin;

        PmHandler(Object origin) {
            this.origin = origin;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            try {

                if ("resolveContentProvider".equals(method.getName())
                        && args != null && args.length >= 1
                        && "com.cathelperawa.owo.app.logs".equals(args[0])) {
                    Object info = method.invoke(origin, args);
                    if (info == null) {

                        try {
                            Class<?> piCls = Class.forName(
                                    "android.content.pm.ProviderInfo");
                            Object fake = piCls.getDeclaredConstructor().newInstance();
                            java.lang.reflect.Field fName = piCls.getField("name");
                            fName.set(fake, "com.cathelperawa.owo.app.LogProvider");
                            java.lang.reflect.Field fPkg = piCls.getField("packageName");
                            fPkg.set(fake, "com.cathelperawa.owo.app");
                            java.lang.reflect.Field fAuth = piCls.getField("authority");
                            fAuth.set(fake, "com.cathelperawa.owo.app.logs");
                            java.lang.reflect.Field fExp = piCls.getField("exported");

                            fExp.set(fake, true);
                            ModLog.log("已伪造 ProviderInfo 供 TIM 发现");
                            return fake;
                        } catch (Throwable ignore) {
                        }
                    }
                    return info;
                }
                if ("getActivityInfo".equals(method.getName())) {
                    Object result = method.invoke(origin, args);
                    if (result != null) return result;
                    ComponentName component = (ComponentName) args[0];
                    if (INJECT.contains(component.getClassName())) {
                        ActivityInfo stubInfo = (ActivityInfo) method.invoke(origin,
                                new Object[]{new ComponentName(component.getPackageName(), STUB),
                                        args[1]});
                        if (stubInfo != null) {
                            ActivityInfo mine = new ActivityInfo(stubInfo);
                            mine.name = component.getClassName();
                            mine.packageName = component.getPackageName();
                            mine.exported = true;
                            mine.enabled = true;
                            return mine;
                        }
                    }
                    return null;
                }
                return method.invoke(origin, args);
            } catch (InvocationTargetException ite) {
                throw ite.getTargetException();
            }
        }
    }
}
