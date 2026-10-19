package com.cathelperawa.owo.app.core;

import android.content.Context;
import android.widget.Toast;

import com.cathelperawa.owo.app.R;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.XposedHelpers;

final class SettingsInject {

    private static final String GROUP_TITLE = "^⌯𖥦⌯^ ੭";
    private static final String ITEM_TITLE = "猫娘助手";

    private SettingsInject() {}

    @SuppressWarnings("unchecked")
    static void inject(Context ctx, List<Object> result, Object owner, String pkg) {
        if (ctx == null || result == null || result.isEmpty()) return;
        for (Object o : result) {
            if (o != null && String.valueOf(o).contains(ITEM_TITLE)) return;
        }
        HookLog.d("设置列表 size=" + result.size()
                + " 元素0=" + result.get(0).getClass().getName());

        Object item = makeMenuItem(ctx);
        if (item == null) {
            HookLog.w("菜单项构造失败");
            return;
        }
        bindClick(item, ctx);

        Object group = makeGroup(result.get(0), item);
        if (group != null) {
            result.add(1, group);
            HookLog.i("已注入分组入口「" + GROUP_TITLE + "」");
        } else {
            result.add(1, item);
            HookLog.i("已注入单项入口");
        }
    }

    private static Object makeMenuItem(Context ctx) {
        int icon = injectModuleResources(ctx);
        ClassLoader cl = ctx.getClassLoader();
        String[] names = new String[]{
                "com.tencent.mobileqq.setting.processor.h",
                "com.tencent.mobileqq.setting.processor.g",
                "com.tencent.mobileqq.setting.processor.i",
                "com.tencent.mobileqq.setting.processor.j",
        };
        for (String cn : names) {
            Class<?> c = XposedHelpers.findClassIfExists(cn, cl);
            if (c == null) continue;
            for (Constructor<?> k : c.getDeclaredConstructors()) {
                Class<?>[] ps = k.getParameterTypes();
                try {
                    k.setAccessible(true);
                    Object o = null;
                    if (ps.length == 4 && ps[0] == Context.class && ps[2] == CharSequence.class) {
                        o = k.newInstance(ctx, 10, ITEM_TITLE, icon);
                    } else if (ps.length == 4 && ps[0] == Context.class && ps[1] == int.class
                            && ps[2] == String.class) {
                        o = k.newInstance(ctx, 10, ITEM_TITLE, icon);
                    } else if (ps.length == 3 && ps[0] == Context.class && ps[1] == int.class) {
                        o = k.newInstance(ctx, 10, ITEM_TITLE);
                    }
                    if (o != null) {
                        HookLog.i("菜单项命中 " + cn + " 参数=" + ps.length + " icon=" + icon);
                        return o;
                    }
                } catch (Throwable ignore) {
                }
            }
        }
        return null;
    }

    private static Object makeGroup(Object sample, Object item) {
        if (sample == null) return null;
        try {
            ArrayList<Object> list = new ArrayList<Object>(1);
            list.add(item);
            for (Constructor<?> k : sample.getClass().getDeclaredConstructors()) {
                Class<?>[] ps = k.getParameterTypes();
                try {
                    k.setAccessible(true);
                    if (ps.length == 3 && List.class.isAssignableFrom(ps[0])) {
                        return k.newInstance(list, GROUP_TITLE, "");
                    }
                    if (ps.length == 2 && List.class.isAssignableFrom(ps[0])) {
                        return k.newInstance(list, GROUP_TITLE);
                    }
                } catch (Throwable ignore) {
                }
            }
        } catch (Throwable ignore) {
        }
        return null;
    }

    private static void bindClick(Object item, final Context ctx) {
        ClassLoader cl = ctx.getClassLoader();
        Class<?> fn0 = XposedHelpers.findClassIfExists("kotlin.jvm.functions.Function0", cl);
        if (fn0 == null) {
            HookLog.w("无 Function0，跳过点击绑定");
            return;
        }
        for (Method m : item.getClass().getDeclaredMethods()) {
            Class<?>[] ps = m.getParameterTypes();
            if (m.getReturnType() != void.class || ps.length != 1) continue;
            if (!ps[0].isAssignableFrom(fn0) && !fn0.isAssignableFrom(ps[0])) continue;
            Object proxy = Proxy.newProxyInstance(cl, new Class<?>[]{ps[0]},
                    new java.lang.reflect.InvocationHandler() {
                        @Override
                        public Object invoke(Object p, Method method, Object[] args) {
                            if ("invoke".equals(method.getName())) openSelf(ctx);
                            return defaultReturn(method.getReturnType());
                        }
                    });
            try {
                m.setAccessible(true);
                m.invoke(item, proxy);
                HookLog.i("已绑点击: " + m.getName());
            } catch (Throwable ignore) {
            }
            return;
        }
    }

    private static void openSelf(Context ctx) {
        try {
            android.content.Intent it = new android.content.Intent();
            it.setClassName(ctx.getPackageName(), "com.cathelperawa.owo.app.ConsoleActivity");
            ctx.startActivity(it);
        } catch (Throwable t) {
            HookLog.e("打开控制台失败", t);
        }
    }

    private static final String SELF = "com.cathelperawa.owo.app";

    private static int injectModuleResources(Context ctx) {
        String apkPath;
        android.content.res.Resources res;
        try {
            apkPath = ctx.getPackageManager().getApplicationInfo(SELF, 0).sourceDir;
            res = ctx.getResources();
        } catch (Throwable t) {
            HookLog.e("拿 apk 路径失败", t);
            return android.R.drawable.ic_menu_manage;
        }

        try {
            Class<?> pfdC = XposedHelpers.findClass("android.os.ParcelFileDescriptor", null);
            Object pfd = pfdC.getMethod("open", java.io.File.class, int.class)
                    .invoke(null, new java.io.File(apkPath), 0x10000000);
            Class<?> provC = XposedHelpers.findClass("android.content.res.ResourcesProvider", null);
            Object provider = provC.getMethod("loadFromApk", pfdC).invoke(null, pfd);
            Class<?> loaderC = XposedHelpers.findClass("android.content.res.ResourcesLoader", null);
            Object loader = loaderC.getDeclaredConstructor().newInstance();
            loaderC.getMethod("addProvider", provC).invoke(loader, provider);
            Method addLoaders = android.content.res.Resources.class.getMethod("addLoaders", loaderC);
            addLoaders.invoke(res, (Object) new Object[]{loader});
            HookLog.i("模块资源注入(策略A)");
            return R.drawable.ic_module;
        } catch (Throwable t) {
            HookLog.w("策略A失败: " + t);
        }

        try {
            android.content.res.AssetManager am = res.getAssets();
            Method m = am.getClass().getMethod("addAssetPath", String.class);
            Object cookie = m.invoke(am, apkPath);
            HookLog.i("模块资源注入(策略B) cookie=" + cookie);
            return R.drawable.ic_module;
        } catch (Throwable t) {
            HookLog.e("策略B失败", t);
        }
        return android.R.drawable.ic_menu_manage;
    }

    private static Object defaultReturn(Class<?> t) {
        if (t == void.class || t == Void.class) return null;
        if (t == boolean.class) return Boolean.FALSE;
        if (t == int.class) return 0;
        if (t == long.class) return 0L;
        if (t == float.class) return 0f;
        if (t == double.class) return 0d;
        if (t == short.class) return (short) 0;
        if (t == byte.class) return (byte) 0;
        if (t == char.class) return (char) 0;
        return null;
    }
}
