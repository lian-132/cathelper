package com.cathelperawa.owo.app.core;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.ParcelFileDescriptor;

import com.cathelperawa.owo.app.R;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

public final class SettingsInjectQQ {

    private static final String GROUP_TITLE = "^⌯𖥦⌯^ ੭";
    private static final String ITEM_TITLE = "猫娘助手";
    private static final String AI_TITLE = "猫娘 AI";
    private static final String SCRIPT_TITLE = "脚本";

    private static final String[] PROVIDERS = {
            "com.tencent.mobileqq.setting.main.NewSettingConfigProvider",
            "com.tencent.mobileqq.setting.main.MainSettingConfigProvider",
            "com.tencent.mobileqq.setting.main.b",
    };

    private static final String[] ITEM_CLASSES = {
            "com.tencent.mobileqq.setting.processor.g",
            "com.tencent.mobileqq.setting.processor.h",
            "com.tencent.mobileqq.setting.processor.i",
            "com.tencent.mobileqq.setting.processor.j",
    };

    private SettingsInjectQQ() {}

    

    private static volatile boolean installed = false;
    private static volatile int tries = 0;

    public static void install(ClassLoader hostCl) {
        if (installed) return;
        ClassLoader cl = hostCl != null ? hostCl : classLoaderOfHost();
        if (cl == null) return;
        tries++;
        boolean any = false;
        for (String pn : PROVIDERS) {
            Class<?> c = HookApi.findClass(cl, pn);
            if (c == null) continue;
            if (hookProvider(c)) any = true;
        }
        if (any) {
            installed = true;
            HookLog.i("QQ设置: 注入器已安装（第 " + tries + " 次尝试）");
            return;
        }
        HookLog.d("QQ设置: 第 " + tries + " 次没找到 provider，挂重试探针");
        installRetryProbe(cl);
    }

    private static volatile boolean retryHooked = false;

    private static void installRetryProbe(final ClassLoader cl) {
        if (retryHooked) return;
        retryHooked = true;
        try {
            Class<?> act = HookApi.findClass(cl, "android.app.Activity");
            if (act == null) return;
            Method m = act.getDeclaredMethod("onResume");
            HookApi.before(m, new HookApi.Action() {
                @Override public void call(XC_MethodHook.MethodHookParam p) {
                    if (installed || tries > 80) return;
                    Object self = p.thisObject;
                    if (self == null) return;
                    String cn = self.getClass().getName().toLowerCase();
                    
                    if (cn.contains("setting") || cn.contains("main") || cn.contains("qzone")
                            || cn.contains("launch")) {
                        install(cl);
                    }
                }
            });
            HookLog.i("QQ设置: 重试探针已挂");
        } catch (Throwable t) {
            HookLog.w("QQ设置: 重试探针挂载失败 " + t);
        }
    }

    private static ClassLoader classLoaderOfHost() {
        try {
            Context c = com.cathelperawa.owo.app.bridge.QQEnv.appContext();
            return c != null ? c.getClassLoader() : null;
        } catch (Throwable t) {
            return null;
        }
    }

    private static boolean hookProvider(Class<?> c) {
        boolean any = false;
        for (final Method m : c.getDeclaredMethods()) {
            Class<?>[] ps = m.getParameterTypes();
            if (!List.class.isAssignableFrom(m.getReturnType())) continue;
            if (ps.length != 1 || ps[0] != Context.class) continue;
            try {
                m.setAccessible(true);
                Object h = HookApi.after(m, new HookApi.Action() {
                    @Override public void call(XC_MethodHook.MethodHookParam p) {
                        try {
                            Object r = p.getResult();
                            if (!(r instanceof List)) return;
                            @SuppressWarnings("unchecked")
                            List<Object> list = (List<Object>) r;
                            inject((Context) p.args[0], list);
                        } catch (Throwable t) {
                            HookLog.w("QQ设置: 注入失败 " + t);
                        }
                    }
                });
                if (h != null) {
                    any = true;
                    HookLog.i("QQ设置: 挂上 " + c.getSimpleName() + "." + m.getName());
                }
            } catch (Throwable t) {
                HookLog.w("QQ设置: hook 失败 " + m.getName() + " " + t);
            }
        }
        return any;
    }

    

    
    private static Object lastGroup;
    private static final ArrayList<Object> lastItems = new ArrayList<Object>();

    private static void inject(Context ctx, List<Object> result) {
        if (ctx == null || result == null || result.isEmpty()) return;
        if (lastGroup != null) {
            try { result.remove(lastGroup); } catch (Throwable ignore) {}
        }

        Class<?> itemCls = pickItemClass(ctx);
        if (itemCls == null) {
            HookLog.w("QQ设置: 没找到条目类");
            return;
        }

        boolean resOk = injectModuleResources(ctx);
        int icModule = resOk ? R.drawable.ic_module : android.R.drawable.ic_menu_manage;
        int icAi = resOk ? R.drawable.ic_ai : android.R.drawable.ic_menu_manage;
        int icScript = resOk ? R.drawable.ic_script : android.R.drawable.ic_menu_manage;

        ArrayList<Object> items = new ArrayList<Object>(3);

        Object a = makeItem(ctx, itemCls, ITEM_TITLE, icModule);
        if (a != null && bindClick(a, new Runnable() {
            @Override public void run() { openConsole("home"); }
        })) items.add(a);

        

        if (items.isEmpty()) {
            HookLog.w("QQ设置: 条目全没造出来");
            return;
        }
        Object group = makeGroup(result.get(0), items);
        if (group == null) {
            HookLog.w("QQ设置: 分组构造失败");
            return;
        }
        result.add(1, group);
        lastGroup = group;
        HookLog.i("QQ设置: 已注入「" + GROUP_TITLE + "」共 " + items.size() + " 项");
    }

    private static Class<?> pickItemClass(Context ctx) {
        ClassLoader cl = ctx.getClassLoader();
        for (String cn : ITEM_CLASSES) {
            Class<?> c = HookApi.findClass(cl, cn);
            if (c == null) continue;
            try {
                if (c.getDeclaredMethods().length > 2) return c;
            } catch (Throwable ignore) {}
        }
        
        for (String cn : ITEM_CLASSES) {
            Class<?> c = HookApi.findClass(cl, cn);
            if (c != null) return c;
        }
        return null;
    }

    private static Object makeItem(Context ctx, Class<?> itemCls, String title, int icon) {
        try {
            Constructor<?> k5 = itemCls.getDeclaredConstructor(
                    Context.class, int.class, CharSequence.class, int.class, String.class);
            k5.setAccessible(true);
            return k5.newInstance(ctx, 10, title, icon, null);
        } catch (Throwable ignore) {}
        try {
            Constructor<?> k4 = itemCls.getDeclaredConstructor(
                    Context.class, int.class, CharSequence.class, int.class);
            k4.setAccessible(true);
            return k4.newInstance(ctx, 10, title, icon);
        } catch (Throwable t) {
            HookLog.w("QQ设置: 造条目失败 " + title + " " + t);
        }
        return null;
    }

    private static boolean bindClick(final Object item, final Runnable action) {
        ClassLoader cl = item.getClass().getClassLoader();
        Class<?> fn0;
        Object unit;
        try {
            fn0 = Class.forName("kotlin.jvm.functions.Function0", false, cl);
            unit = Class.forName("kotlin.Unit", false, cl).getField("INSTANCE").get(null);
        } catch (Throwable t) {
            HookLog.w("QQ设置: 没有 kotlin.Function0/Unit " + t);
            return false;
        }
        for (Method m : item.getClass().getDeclaredMethods()) {
            Class<?>[] ps = m.getParameterTypes();
            if (m.getReturnType() != void.class || ps.length != 1) continue;
            if (!ps[0].isAssignableFrom(fn0) && !fn0.isAssignableFrom(ps[0])) continue;
            final Object theUnit = unit;
            Object proxy = Proxy.newProxyInstance(cl, new Class<?>[]{ps[0]},
                    new java.lang.reflect.InvocationHandler() {
                        @Override public Object invoke(Object p, Method mm, Object[] a) {
                            if ("invoke".equals(mm.getName())) {
                                try { action.run(); } catch (Throwable t) {
                                    HookLog.e("QQ设置: 点击回调失败", t);
                                }
                            }
                            return theUnit;
                        }
                    });
            try {
                m.setAccessible(true);
                m.invoke(item, proxy);
                return true;
            } catch (Throwable t) {
                HookLog.w("QQ设置: 绑点击失败 " + m.getName() + " " + t);
            }
        }
        return false;
    }

    private static Object makeGroup(Object sample, ArrayList<Object> items) {
        if (sample == null) return null;
        Class<?> g = sample.getClass();
        try {
            Constructor<?> k3 = g.getDeclaredConstructor(
                    List.class, CharSequence.class, CharSequence.class);
            k3.setAccessible(true);
            return k3.newInstance(items, GROUP_TITLE, "");
        } catch (Throwable ignore) {}
        try {
            Class<?> marker = Class.forName("kotlin.jvm.internal.DefaultConstructorMarker",
                    false, g.getClassLoader());
            Constructor<?> k5 = g.getDeclaredConstructor(
                    List.class, CharSequence.class, CharSequence.class, int.class, marker);
            k5.setAccessible(true);
            
            return k5.newInstance(items, GROUP_TITLE, "", 6, null);
        } catch (Throwable t) {
            HookLog.w("QQ设置: 分组构造失败 " + t);
        }
        return null;
    }

    

    
    private static void openConsole(String page) {
        try {

            Context ctx = com.cathelperawa.owo.app.bridge.QQEnv.topActivity();
            if (ctx == null) ctx = com.cathelperawa.owo.app.bridge.QQEnv.appContext();
            if (ctx == null) {
                HookLog.w("QQ设置: 没有可用 Context，打不开控制台");
                return;
            }
            com.cathelperawa.owo.app.ui.QqConsole.show(ctx, page);
        } catch (Throwable t) {
            HookLog.e("QQ设置: 打开控制台失败", t);
        }
    }

    

    private static final String SELF = "com.cathelperawa.owo.app";
    private static Boolean sResInjected;

    public static synchronized boolean injectModuleResources(Context ctx) {
        if (sResInjected != null) return sResInjected;
        boolean ok = doInject(ctx);
        sResInjected = ok;
        return ok;
    }

    private static boolean doInject(Context ctx) {
        if (ctx == null) return false;
        String apk;
        Resources res;
        try {
            apk = ctx.getPackageManager().getApplicationInfo(SELF, 0).sourceDir;
            
            res = ctx.getResources();
        } catch (Throwable t) {
            HookLog.w("QQ设置: 拿 APK 路径失败 " + t);
            return false;
        }
        
        try {
            Class<?> pfdC = XposedHelpers.findClass("android.os.ParcelFileDescriptor", null);
            Object pfd = pfdC.getMethod("open", java.io.File.class, int.class)
                    .invoke(null, new java.io.File(apk), 0x10000000);
            Class<?> provC = XposedHelpers.findClass(
                    "android.content.res.ResourcesProvider", null);
            Object provider = provC.getMethod("loadFromApk", pfdC).invoke(null, pfd);
            Class<?> loaderC = XposedHelpers.findClass(
                    "android.content.res.ResourcesLoader", null);
            Object loader = loaderC.getDeclaredConstructor().newInstance();
            loaderC.getMethod("addProvider", provC).invoke(loader, provider);
            Method add = Resources.class.getMethod("addLoaders", loaderC);
            add.invoke(res, (Object) new Object[]{loader});
            HookLog.i("QQ设置: 资源注入(策略A)");
            return true;
        } catch (Throwable t) {
            HookLog.d("QQ设置: 策略A失败 " + t);
        }
        
        try {
            android.content.res.AssetManager am = res.getAssets();
            Method m = am.getClass().getMethod("addAssetPath", String.class);
            Object cookie = m.invoke(am, apk);
            HookLog.i("QQ设置: 资源注入(策略B) cookie=" + cookie);
            return true;
        } catch (Throwable t) {
            HookLog.w("QQ设置: 策略B失败 " + t);
        }
        return false;
    }
}
