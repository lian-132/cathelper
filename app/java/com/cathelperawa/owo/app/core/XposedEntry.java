package com.cathelperawa.owo.app.core;

import android.content.Context;

import com.cathelperawa.owo.app.Parasitics;
import com.cathelperawa.owo.app.XposedStatus;
import com.cathelperawa.owo.app.bridge.QQEnv;
import com.cathelperawa.owo.app.feature.AntiRevoke;
import com.cathelperawa.owo.app.feature.FeatureCat;
import com.cathelperawa.owo.app.feature.FeatureChatMix;
import com.cathelperawa.owo.app.feature.FeatureChatTools;
import com.cathelperawa.owo.app.feature.FeatureMsgExtract;
import com.cathelperawa.owo.app.feature.FeatureMsfTap;
import com.cathelperawa.owo.app.feature.FeatureRecorder;
import com.cathelperawa.owo.app.feature.FeatureUnlockForward;
import com.cathelperawa.owo.app.feature.FeatureUnlockMultiRecall;
import com.cathelperawa.owo.app.feature.FeatureUnlockPatPat;

import java.lang.reflect.Method;
import java.util.List;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class XposedEntry implements IXposedHookLoadPackage {

    private static final String TAG = "猫娘助手:";
    private static boolean parasiticsInited = false;

    private static void registerFeatures() {

        HookItemFactory.register(AntiRevoke.class);
        loadManual(AntiRevoke.class);

        HookItemFactory.register(FeatureCat.class);
        HookItemFactory.register(FeatureRecorder.class);
        HookItemFactory.register(FeatureMsfTap.class);
        HookItemFactory.register(FeatureMsgExtract.class);
        HookItemFactory.register(FeatureChatTools.class);
        HookItemFactory.register(FeatureUnlockPatPat.class);
        HookItemFactory.register(FeatureUnlockForward.class);
        HookItemFactory.register(FeatureUnlockMultiRecall.class);
        HookItemFactory.register(FeatureChatMix.class);
    }

    private static void loadManual(Class<? extends HookItemBase> cls) {
        try {
            HookItemBase item = cls.newInstance();
            item.bind(appCtx, appCl);
            item.entry(appCl);
            HookLog.i("手动加载: " + cls.getSimpleName() + " 开关=" + item.isEnabled());
        } catch (Throwable t) {
            HookLog.e("手动加载失败: " + cls.getSimpleName(), t);
        }
    }

    private static volatile android.content.Context appCtx;
    private static volatile ClassLoader appCl;

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lp) {
        try {
            if ("com.cathelperawa.owo.app".equals(lp.packageName)) {
                hookSelf(lp.classLoader);
                return;
            }

            if ("com.tencent.tim".equals(lp.packageName)) {
                hookHost(lp.classLoader, lp.packageName);
            }
        } catch (Throwable t) {
            XposedBridge.log(TAG + " handleLoadPackage 异常: " + t);
        }
    }

    private void hookHost(final ClassLoader cl, final String pkg) {
        HookLog.i("注入宿主 " + pkg);

        Class<?> app = HookApi.findClass(cl, "com.tencent.common.app.BaseApplicationImpl");
        if (app == null) {
            HookLog.w("未找到 BaseApplicationImpl");
            return;
        }
        XposedHelpers.findAndHookMethod(app, "attachBaseContext", Context.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        Context ctx = (Context) param.thisObject;
                        onHostReady(ctx, cl, pkg);
                    }
                });
        XposedBridge.log(TAG + pkg + " 已挂 BaseApplicationImpl");
    }

    private void onHostReady(Context ctx, ClassLoader cl, String pkg) {

        appCtx = ctx;
        appCl = cl;

        try {
            QQEnv.setAppContext(ctx);

            int hv = HostVersion.code(ctx);
            com.cathelperawa.owo.app.util.ReflectCache.bindVersion(hv);
            com.cathelperawa.owo.app.util.ReflectClassCache.bindVersion(hv);

            int mv = com.cathelperawa.owo.app.BuildInfo.VERSION_CODE;
            com.cathelperawa.owo.app.util.ReflectCache.bindModuleVersion(mv);
            com.cathelperawa.owo.app.util.ReflectClassCache.bindModuleVersion(mv);
            HookLog.i("宿主 " + HostVersion.name(ctx) + "（" + hv + "）缓存已绑定，模块 v" + mv);

            try {
                boolean realtime = HookConfig.getBoolean(ctx, "debug_error_realtime", false);
                java.io.File logDir = new java.io.File(ctx.getFilesDir(), "cathelper");
                RuntimeErrors.autoFlushTo(logDir, realtime);
                HookLog.i("运行期错误落盘已开启" + (realtime ? "（实时模式）" : "") + " → " + logDir);
            } catch (Throwable t) {
                HookLog.e("错误落盘初始化失败", t);
            }

        } catch (Throwable t) {
            HookLog.e("环境初始化失败", t);
        }

        try {
            if (!parasiticsInited) {
                parasiticsInited = true;
                Parasitics.init(ctx);
            }
        } catch (Throwable t) {
            HookLog.e("Parasitics 初始化失败", t);
        }

        try {
            com.cathelperawa.owo.app.util.CatLog.setContext(ctx);
            com.cathelperawa.owo.app.ModLog.setContext(ctx);
        } catch (Throwable t) {
            HookLog.e("日志上下文初始化失败", t);
        }

        try {
            hookSettings(cl, pkg);
        } catch (Throwable t) {
            HookLog.e("设置页注入失败", t);
        }

        try {
            com.cathelperawa.owo.app.core.ModuleCommand.register(ctx);
        } catch (Throwable t) {
            HookLog.e("命令通道注册失败", t);
        }

        try {
            com.cathelperawa.owo.app.MsgStore.init(ctx);
        } catch (Throwable t) {
            HookLog.e("MsgStore 初始化失败", t);
        }

        try {
            registerFeatures();
        } catch (Throwable t) {
            HookLog.e("功能注册失败", t);
        }
        try {
            HookItemFactory.init(ctx, cl);
        } catch (Throwable t) {
            HookLog.e("功能项加载失败", t);
        }

        try {
            com.cathelperawa.owo.app.core.StatusStore.save(ctx);
            HookLog.i("功能状态已落盘: " + HookItemFactory.snapshot().replace('\n', ' '));

            com.cathelperawa.owo.app.core.StatusStore.startAutoRefresh(ctx);
        } catch (Throwable t) {
            HookLog.e("状态落盘失败", t);
        }
    }

    private static volatile boolean settingsHooked = false;

    private void hookSettings(ClassLoader cl, String pkg) {
        if (settingsHooked) return;
        Class<?> c = HookApi.findClass(cl,
                "com.tencent.mobileqq.setting.main.MainSettingConfigProvider");
        if (c == null) {
            HookLog.w("未找到 MainSettingConfigProvider");
            return;
        }
        for (final Method m : c.getDeclaredMethods()) {
            Class<?>[] ps = m.getParameterTypes();
            if (!List.class.isAssignableFrom(m.getReturnType())) continue;
            if (ps.length != 1 || ps[0] != Context.class) continue;
            HookApi.after(m, new HookApi.Action() {
                @Override
                public void call(XC_MethodHook.MethodHookParam param) {
                    try {
                        SettingsInject.inject((Context) param.args[0],
                                (List<Object>) param.getResult(), param.thisObject, pkg);
                    } catch (Throwable t) {
                        HookLog.e("设置页注入异常", t);
                    }
                }
            });
            settingsHooked = true;
            HookLog.i("设置页 hook 已挂: " + m.getName());
        }
    }

    private static volatile boolean selfHooked = false;

    private void hookSelf(final ClassLoader cl) {
        if (selfHooked) return;
        selfHooked = true;
        try {
            Class<?> target = XposedHelpers.findClassIfExists(
                    "com.cathelperawa.owo.app.XposedStatus", cl);
            if (target == null) {
                XposedBridge.log(TAG + " 未找到 XposedStatus 类");
                return;
            }
            final String info = buildActiveInfo();
            XposedHelpers.findAndHookMethod(target, "queryActive",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            param.setResult(info);
                        }
                    });
            XposedBridge.log(TAG + " 已挂自身（激活状态回传）: " + info);
        } catch (Throwable t) {
            XposedBridge.log(TAG + " 挂自身失败: " + t);
        }
    }

    private static String buildActiveInfo() {
        int api = 0;
        String fw = "";
        try {
            api = XposedBridge.getXposedVersion();
        } catch (Throwable ignore) {
        }
        try {
            Class<?> at = Class.forName("android.app.ActivityThread");
            Object ctx = at.getMethod("currentApplication").invoke(null);
            if (ctx instanceof Context) {
                android.content.pm.PackageInfo pi = ((Context) ctx).getPackageManager()
                        .getPackageInfo("org.lsposed.manager", 0);
                if (pi != null && pi.versionName != null) fw = "LSPosed " + pi.versionName;
            }
        } catch (Throwable ignore) {
        }
        return "1|" + api + "|" + fw;
    }
}
