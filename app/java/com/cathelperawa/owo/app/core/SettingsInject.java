package com.cathelperawa.owo.app.core;

import android.content.Context;
import android.widget.Toast;

import com.cathelperawa.owo.app.R;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

public final class SettingsInject {

    private static final String GROUP_TITLE = "^⌯𖥦⌯^ ੭";
    private static final String ITEM_TITLE = "猫娘助手";

    private static final String AI_TITLE = "猫娘 AI";

    private static final String SCRIPT_TITLE = "脚本";

    private SettingsInject() {}

    @SuppressWarnings("unchecked")
    static void inject(Context ctx, List<Object> result, Object owner, String pkg) {
        if (ctx == null || result == null || result.isEmpty()) return;

        removeInjected(result);
        installResumeHook();

        HookLog.d("设置列表 size=" + result.size()
                + " 元素0=" + result.get(0).getClass().getName());

        final Context fctx = ctx;

        sLastResult = result;
        sLastCtx = ctx;
        sLastOwner = owner;

        Object item = makeMenuItem(ctx, ITEM_TITLE, R.drawable.ic_module);
        if (item == null) {
            HookLog.w("菜单项构造失败");
            return;
        }
        bindClick(item, new Runnable() {
            @Override public void run() { openSelf(fctx); }
        });

        ArrayList<Object> items = new ArrayList<Object>(3);
        items.add(item);
        Object ai = makeMenuItem(ctx, AI_TITLE, R.drawable.ic_ai);
        if (ai != null) {
            bindClick(ai, new Runnable() {
                @Override public void run() { openAi(fctx); }
            });
            items.add(ai);
        } else {
            HookLog.w("AI 菜单项构造失败（猫娘助手入口不受影响）");
        }

        Object script = makeMenuItem(ctx, SCRIPT_TITLE, R.drawable.ic_script);
        if (script != null) {
            bindClick(script, new Runnable() {
                @Override public void run() { openScript(fctx); }
            });
            items.add(script);
        } else {
            HookLog.w("脚本菜单项构造失败（其它入口不受影响）");
        }

        try {
            java.util.List<com.cathelperawa.owo.app.core.ScriptSetting.Entry> extra =
                    com.cathelperawa.owo.app.core.ScriptSetting.list();
            for (int i = 0; i < extra.size(); i++) {
                final String pageKey = extra.get(i).page;
                Object mi = makeMenuItem(ctx, extra.get(i).title, R.drawable.ic_script);
                if (mi == null) continue;
                bindClick(mi, new Runnable() {
                    @Override public void run() { openPage(fctx, pageKey); }
                });
                items.add(mi);
                HookLog.i("脚本入口已注入：" + pageKey);
            }
        } catch (Throwable t) {
            HookLog.w("脚本入口注入失败 " + t);
        }

        Object group = makeGroupOf(result.get(0), items);
        if (group != null) {
            result.add(1, group);
            HookLog.i("已注入分组入口「" + GROUP_TITLE + "」，共 "
                    + items.size() + " 项");
        } else {
            result.add(1, item);
            HookLog.i("已注入单项入口");
        }
    }

    private static void removeInjected(List<Object> result) {
        try {
            java.util.Iterator<Object> it = result.iterator();
            int n = 0;
            while (it.hasNext()) {
                Object o = it.next();
                if (o == null) continue;
                String s = String.valueOf(o);
                if (s.contains(ITEM_TITLE) || s.contains(GROUP_TITLE)) {
                    it.remove();
                    n++;
                }
            }
            if (n > 0) HookLog.d("已清理旧注入项 " + n + " 个");
        } catch (Throwable t) {
            HookLog.w("清理旧注入项失败 " + t);
        }
    }

    private static Object makeMenuItem(Context ctx) {
        return makeMenuItem(ctx, ITEM_TITLE, R.drawable.ic_module);
    }

    private static Object makeMenuItem(Context ctx, String title) {
        return makeMenuItem(ctx, title, R.drawable.ic_module);
    }

    private static Object makeMenuItem(Context ctx, String title, int iconRes) {
        int icon = injectModuleResources(ctx) ? iconRes : android.R.drawable.ic_menu_manage;
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
                        o = k.newInstance(ctx, 10, title, icon);
                    } else if (ps.length == 4 && ps[0] == Context.class && ps[1] == int.class
                            && ps[2] == String.class) {
                        o = k.newInstance(ctx, 10, title, icon);
                    } else if (ps.length == 3 && ps[0] == Context.class && ps[1] == int.class) {
                        o = k.newInstance(ctx, 10, title);
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
        ArrayList<Object> list = new ArrayList<Object>(1);
        list.add(item);
        return makeGroupOf(sample, list);
    }

    private static Object makeGroupOf(Object sample, ArrayList<Object> list) {
        if (sample == null) return null;
        try {
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

    private static void bindClick(Object item, final Runnable action) {
        ClassLoader cl = item.getClass().getClassLoader();
        Class<?> fn0 = null;
        try {
            fn0 = Class.forName("kotlin.jvm.functions.Function0", false, cl);
        } catch (Throwable ignore) {
        }
        if (fn0 == null) return;
        for (Method m : item.getClass().getDeclaredMethods()) {
            Class<?>[] ps = m.getParameterTypes();
            if (m.getReturnType() != void.class || ps.length != 1) continue;
            if (!ps[0].isAssignableFrom(fn0) && !fn0.isAssignableFrom(ps[0])) continue;
            Object proxy = Proxy.newProxyInstance(cl, new Class<?>[]{ps[0]},
                    new java.lang.reflect.InvocationHandler() {
                        @Override public Object invoke(Object p, Method mm, Object[] a) {
                            try { action.run(); } catch (Throwable t) {
                                HookLog.e("菜单点击回调失败", t);
                            }
                            return null;
                        }
                    });
            try {
                m.setAccessible(true);
                m.invoke(item, proxy);
                return;
            } catch (Throwable ignore) {
            }
        }
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

    private static java.util.List<Object> sLastResult;
    private static Context sLastCtx;
    private static Object sLastOwner;

    public static void rebuildNow() {
        final java.util.List<Object> result = sLastResult;
        final Context ctx = sLastCtx;
        if (result == null || ctx == null) {
            refreshVisible();
            return;
        }
        try {
            android.os.Handler h = new android.os.Handler(android.os.Looper.getMainLooper());
            h.post(new Runnable() {
                @Override public void run() {
                    try {
                        inject(ctx, result, sLastOwner, null);
                        notifyAdapters(settingDecor(), 0);
                        HookLog.i("设置入口已热更新（v"
                                + com.cathelperawa.owo.app.core.ScriptSetting.version() + "）");
                    } catch (Throwable t) {
                        HookLog.w("入口热更新失败 " + t);
                        refreshVisible();
                    }
                }
            });
        } catch (Throwable t) {
            HookLog.w("rebuildNow 调度失败 " + t);
            refreshVisible();
        }
    }

    private static android.view.View settingDecor() {
        try {
            Class<?> atCls = Class.forName("android.app.ActivityThread");
            Object at = atCls.getMethod("currentActivityThread").invoke(null);
            java.lang.reflect.Field f = atCls.getDeclaredField("mActivities");
            f.setAccessible(true);
            java.util.Map<?, ?> acts = (java.util.Map<?, ?>) f.get(at);
            if (acts == null) return null;
            for (Object rec : acts.values()) {
                if (rec == null) continue;
                try {
                    java.lang.reflect.Field actF = rec.getClass().getDeclaredField("activity");
                    actF.setAccessible(true);
                    android.app.Activity a = (android.app.Activity) actF.get(rec);
                    if (a == null) continue;
                    if (!a.getClass().getName().toLowerCase().contains("setting")) continue;
                    return a.getWindow().getDecorView();
                } catch (Throwable ignore) {
                }
            }
        } catch (Throwable ignore) {
        }
        return null;
    }

    private static void notifyAdapters(android.view.View v, int depth) {
        if (v == null || depth > 40) return;
        try {
            java.lang.reflect.Method getAdapter = v.getClass().getMethod("getAdapter");
            Object ad = getAdapter.invoke(v);
            if (ad != null) {

                boolean done = false;
                try {
                    java.lang.reflect.Method submit = ad.getClass().getMethod("submitList",
                            java.util.List.class);
                    if (sLastResult != null) {
                        submit.invoke(ad, new java.util.ArrayList<Object>(sLastResult));
                        done = true;
                    }
                } catch (Throwable ignore) {
                }
                if (!done) {
                    try {
                        ad.getClass().getMethod("notifyDataSetChanged").invoke(ad);
                    } catch (Throwable ignore) {
                    }
                }
            }
        } catch (Throwable ignore) {
        }
        if (v instanceof android.view.ViewGroup) {
            android.view.ViewGroup g = (android.view.ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) {
                notifyAdapters(g.getChildAt(i), depth + 1);
            }
        }
    }

    private static int sLastVersion = -1;
    private static boolean sResumeHooked = false;

    static void installResumeHook() {
        if (sResumeHooked) return;
        sResumeHooked = true;
        try {
            Class<?> actCls = Class.forName("android.app.Activity", false,
                    SettingsInject.class.getClassLoader());
            java.lang.reflect.Method m = actCls.getDeclaredMethod("onResume");
            com.cathelperawa.owo.app.core.HookApi.before(m,
                    new com.cathelperawa.owo.app.core.HookApi.Action() {
                @Override public void call(XC_MethodHook.MethodHookParam p) {
                    try {
                        Object self = p.thisObject;
                        if (self == null) return;
                        String cn = self.getClass().getName().toLowerCase();
                        if (!cn.contains("setting")) return;
                        final int v = com.cathelperawa.owo.app.core.ScriptSetting.version();
                        if (v == sLastVersion) return;
                        sLastVersion = v;
                        final android.app.Activity a = (android.app.Activity) self;
                        a.runOnUiThread(new Runnable() {
                            @Override public void run() {
                                try {
                                    a.recreate();
                                    HookLog.i("设置页已刷新（脚本入口 v" + v + "）");
                                } catch (Throwable t) {
                                    HookLog.w("设置页刷新失败 " + t);
                                }
                            }
                        });
                    } catch (Throwable ignore) {
                    }
                }
            });
            HookLog.i("设置页 onResume 监听已装");
        } catch (Throwable t) {
            HookLog.w("onResume 监听安装失败 " + t);
        }
    }

    public static void refreshVisible() {
        try {
            Class<?> atCls = Class.forName("android.app.ActivityThread");
            Object at = atCls.getMethod("currentActivityThread").invoke(null);
            java.lang.reflect.Field f = atCls.getDeclaredField("mActivities");
            f.setAccessible(true);
            java.util.Map<?, ?> acts = (java.util.Map<?, ?>) f.get(at);
            if (acts == null || acts.isEmpty()) return;
            for (Object rec : acts.values()) {
                if (rec == null) continue;
                try {
                    java.lang.reflect.Field actF = rec.getClass().getDeclaredField("activity");
                    actF.setAccessible(true);
                    final android.app.Activity a = (android.app.Activity) actF.get(rec);
                    if (a == null) continue;
                    String cn = a.getClass().getName().toLowerCase();
                    if (!cn.contains("setting")) continue;
                    a.runOnUiThread(new Runnable() {
                        @Override public void run() {
                            try {
                                a.recreate();
                                HookLog.i("设置页已自动刷新（脚本入口变更）");
                            } catch (Throwable t) {
                                HookLog.w("设置页刷新失败 " + t);
                            }
                        }
                    });
                    return;
                } catch (Throwable ignore) {
                }
            }
        } catch (Throwable t) {
            HookLog.d("refreshVisible 跳过：" + t);
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

    private static void openAi(Context ctx) {
        try {
            android.content.Intent it = new android.content.Intent();
            it.setClassName(ctx.getPackageName(), "com.cathelperawa.owo.app.ai.AiActivity");
            ctx.startActivity(it);
        } catch (Throwable t) {
            HookLog.e("打开 AI 控制台失败", t);
        }
    }

    private static void openPage(Context ctx, String page) {
        try {
            android.content.Intent it = new android.content.Intent();
            it.setClassName(ctx.getPackageName(), "com.cathelperawa.owo.app.ConsoleActivity");
            it.putExtra("cat_page", page);
            ctx.startActivity(it);
        } catch (Throwable t) {
            HookLog.e("打开脚本页面失败 " + page, t);
        }
    }

    private static void openScript(Context ctx) {
        try {
            android.content.Intent it = new android.content.Intent();
            it.setClassName(ctx.getPackageName(), "com.cathelperawa.owo.app.ConsoleActivity");
            it.putExtra("cat_page", "script");
            ctx.startActivity(it);
        } catch (Throwable t) {
            HookLog.e("打开脚本控制台失败", t);
        }
    }

    private static final String SELF = "com.cathelperawa.owo.app";

    private static Boolean sResInjected;

    public static synchronized boolean injectModuleResources(Context ctx) {
        if (sResInjected != null) return sResInjected;
        boolean ok = doInjectModuleResources(ctx);
        sResInjected = ok;
        return ok;
    }

    private static boolean doInjectModuleResources(Context ctx) {
        String apkPath;
        android.content.res.Resources res;
        try {
            apkPath = ctx.getPackageManager().getApplicationInfo(SELF, 0).sourceDir;
            res = ctx.getResources();
        } catch (Throwable t) {
            HookLog.e("拿 apk 路径失败", t);
            return false;
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
            return true;
        } catch (Throwable t) {
            HookLog.w("策略A失败: " + t);
        }

        try {
            android.content.res.AssetManager am = res.getAssets();
            Method m = am.getClass().getMethod("addAssetPath", String.class);
            Object cookie = m.invoke(am, apkPath);
            HookLog.i("模块资源注入(策略B) cookie=" + cookie);
            return true;
        } catch (Throwable t) {
            HookLog.e("策略B失败", t);
        }
        return false;
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
