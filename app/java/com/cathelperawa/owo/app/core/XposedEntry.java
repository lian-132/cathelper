package com.cathelperawa.owo.app.core;

import com.cathelperawa.owo.app.core.DataDir;

import android.content.Context;

import com.cathelperawa.owo.app.Parasitics;
import com.cathelperawa.owo.app.XposedStatus;
import com.cathelperawa.owo.app.bridge.QQEnv;
import com.cathelperawa.owo.app.feature.AntiRevoke;
import com.cathelperawa.owo.app.feature.FeatureCat;
import com.cathelperawa.owo.app.feature.FeatureChatMix;
import com.cathelperawa.owo.app.feature.FeatureChatTools;
import com.cathelperawa.owo.app.feature.FeatureApkRename;
import com.cathelperawa.owo.app.feature.FeatureGroupMenu;
import com.cathelperawa.owo.app.ai.AiHook;
import com.cathelperawa.owo.app.feature.FeatureSendProto;
import com.cathelperawa.owo.app.feature.FeatureParse;
import com.cathelperawa.owo.app.feature.FeatureGroupClockIn;
import com.cathelperawa.owo.app.feature.FeatureMsgTime;
import com.cathelperawa.owo.app.feature.FeatureSenderVersion;
import com.cathelperawa.owo.app.feature.FeatureViewCard;
import com.cathelperawa.owo.app.feature.FeatureCustomDevice;
import com.cathelperawa.owo.app.feature.FeatureAntiDetect;
import com.cathelperawa.owo.app.feature.QQSettingEntry;
import com.cathelperawa.owo.app.feature.FeatureFakeNetwork;
import com.cathelperawa.owo.app.feature.FeatureFakePhone;
import com.cathelperawa.owo.app.feature.FeatureFakeQua;
import com.cathelperawa.owo.app.feature.FeatureProtoTap;
import com.cathelperawa.owo.app.feature.FeatureMsgExtract;
import com.cathelperawa.owo.app.feature.FeaturePicSummary;
import com.cathelperawa.owo.app.feature.FeatureQqBrowser;
import com.cathelperawa.owo.app.feature.FeatureMsfTap;
import com.cathelperawa.owo.app.feature.FeatureRecorder;
import com.cathelperawa.owo.app.feature.FeatureRedPacket;
import com.cathelperawa.owo.app.feature.FeatureUnlockForward;
import com.cathelperawa.owo.app.feature.FeatureUnlockMultiRecall;
import com.cathelperawa.owo.app.feature.FeatureBypassArkLimit;
import com.cathelperawa.owo.app.feature.FeaturePttAutoText;
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
        HookItemFactory.register(FeatureRedPacket.class);
        HookItemFactory.register(FeatureUnlockPatPat.class);
        HookItemFactory.register(FeaturePttAutoText.class);
        HookItemFactory.register(FeatureUnlockForward.class);
        HookItemFactory.register(FeatureUnlockMultiRecall.class);
        HookItemFactory.register(FeatureBypassArkLimit.class);
        HookItemFactory.register(FeatureChatMix.class);
        HookItemFactory.register(FeaturePicSummary.class);

        HookItemFactory.register(FeatureQqBrowser.class);
        HookItemFactory.register(FeatureAntiDetect.class);
        HookItemFactory.register(QQSettingEntry.class);
        HookItemFactory.register(FeatureFakeNetwork.class);
        HookItemFactory.register(FeatureCustomDevice.class);
        HookItemFactory.register(FeatureFakePhone.class);
        HookItemFactory.register(FeatureFakeQua.class);
        HookItemFactory.register(FeatureProtoTap.class);
        HookItemFactory.register(FeatureViewCard.class);
        HookItemFactory.register(FeatureGroupClockIn.class);
        HookItemFactory.register(FeatureMsgTime.class);
        HookItemFactory.register(FeatureSenderVersion.class); // 显示发送者版本号
        HookItemFactory.register(FeatureApkRename.class);
        HookItemFactory.register(FeatureGroupMenu.class);
        HookItemFactory.register(AiHook.class);
        HookItemFactory.register(FeatureSendProto.class);
        HookItemFactory.register(FeatureParse.class);
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

            if (com.cathelperawa.owo.app.core.Host.isHost(lp.packageName)) {
                com.cathelperawa.owo.app.core.Host.set(lp.packageName);
                if (com.cathelperawa.owo.app.core.Host.PKG_QQ.equals(lp.packageName)) {
                    try {
                        com.cathelperawa.owo.app.bridge.QqDbKey.install(lp.classLoader);
                        com.cathelperawa.owo.app.bridge.QqDb.install(lp.classLoader);
                    } catch (Throwable t) {
                        HookLog.w("QqDbKey 早期安装失败 " + t);
                    }
                }
                hookHost(lp.classLoader, lp.packageName);
            }
        } catch (Throwable t) {
            XposedBridge.log(TAG + " handleLoadPackage 异常: " + t);
        }
    }

    private static final java.util.concurrent.atomic.AtomicBoolean hostReady =
            new java.util.concurrent.atomic.AtomicBoolean(false);

    private void hookHost(final ClassLoader cl, final String pkg) {
        HookLog.i("注入宿主 " + pkg);

        Class<?> qfix = HookApi.findClass(cl,
                "com.tencent.common.app.QFixApplicationImplProxy",
                "com.tencent.common.app.QFixApplicationImpl",
                "com.tencent.mobileqq.qfix.QFixApplication");
        if (qfix != null) {
            boolean hooked = false;
            for (java.lang.reflect.Method m : qfix.getDeclaredMethods()) {
                if (!"attachBaseContext".equals(m.getName())) continue;
                try {
                    m.setAccessible(true);
                    Object r = HookApi.after(m, new HookApi.Action() {
                        @Override public void call(XC_MethodHook.MethodHookParam p) {
                            fireHostReady(ctxOf(p), pkg);
                        }
                    });
                    if (r != null) {
                        hooked = true;
                        HookLog.i("挂: QFixApplicationImplProxy.attachBaseContext");
                    }
                } catch (Throwable t) {
                    HookLog.w("挂 QFix 失败: " + t);
                }
            }
            if (!hooked) HookLog.w("QFix 类在，但 attachBaseContext 没挂上");
        } else {
            HookLog.i("没有 QFixApplicationImplProxy（TIM 正常情况）");
        }

        Class<?> app = HookApi.findClass(cl, "com.tencent.common.app.BaseApplicationImpl");
        if (app == null) {
            HookLog.w("未找到 BaseApplicationImpl");
            return;
        }
        int n = 0;
        for (java.lang.reflect.Method m : app.getDeclaredMethods()) {
            String nm = m.getName();
            if (!"attachBaseContext".equals(nm) && !"onCreate".equals(nm)) continue;
            try {
                m.setAccessible(true);
                Object r = HookApi.after(m, new HookApi.Action() {
                    @Override public void call(XC_MethodHook.MethodHookParam p) {
                        fireHostReady(ctxOf(p), pkg);
                    }
                });
                if (r != null) {
                    n++;
                    HookLog.i("挂: BaseApplicationImpl." + nm + "(" + m.getParameterTypes().length + "参)");
                }
            } catch (Throwable t) {
                HookLog.w("挂 " + nm + " 失败: " + t);
            }
        }
        XposedBridge.log(TAG + pkg + " 已挂 BaseApplicationImpl x" + n);
    }

    private static Context ctxOf(XC_MethodHook.MethodHookParam p) {
        if (p.args != null) {
            for (Object a : p.args) {
                if (a instanceof Context) return (Context) a;
            }
        }
        return p.thisObject instanceof Context ? (Context) p.thisObject : null;
    }

    private void fireHostReady(Context ctx, String pkg) {
        if (ctx == null) return;
        if (!hostReady.compareAndSet(false, true)) return;
        ClassLoader loader = ctx.getClassLoader();
        HookLog.i("宿主就绪（" + pkg + "）loader="
                + (loader == null ? "null" : loader.getClass().getSimpleName()));
        try {
            onHostReady(ctx, loader == null ? XposedEntry.class.getClassLoader() : loader, pkg);
        } catch (Throwable t) {
            HookLog.e("onHostReady 失败", t);
        }
    }

    private void onHostReady(Context ctx, ClassLoader cl, String pkg) {

        appCtx = ctx;
        appCl = cl;
        BootTiming.mark("入口"); HookLog.i("[BOOT] 入口");

        try {
            com.cathelperawa.owo.app.feature.ArkSigner.installMiniShare(cl);
        } catch (Throwable t) { HookLog.e("挂 MiniShare 失败", t); }

        try {
            com.cathelperawa.owo.app.proto.PbSender.setupMsgHook();
        } catch (Throwable t) { HookLog.e("挂 MsgHook 失败", t); }

        try {
            com.cathelperawa.owo.app.proto.PbSender.setupSendDump();
        } catch (Throwable t) { HookLog.e("挂 SendDump 失败", t); }

        try {
            com.cathelperawa.owo.app.feature.PicLinkPick.install(cl);
        } catch (Throwable t) { HookLog.e("挂 PicLinkPick 失败", t); }

        try {
            com.cathelperawa.owo.app.core.UpdateChecker.installHostHook(cl);
        } catch (Throwable t) { HookLog.e("挂 UpdateChecker 失败", t); }

        try {
            com.cathelperawa.owo.app.feature.ArkSigner.installShareProbe(cl);
        } catch (Throwable t) { HookLog.e("挂 ShareHook 失败", t); }

        LoadState.enter(LoadState.Stage.ENTER);
        boolean safe = false;
        try {
            safe = SafeMode.beginLoad(ctx);
            if (safe) LoadState.safeMode();
            else LoadState.enter(LoadState.Stage.CONTEXT);
        } catch (Throwable t) {
            HookLog.e("安全模式判定失败", t);
        }
        BootTiming.mark("安全模式"); HookLog.i("[BOOT] 安全模式");

        try {
            QQEnv.setAppContext(ctx);
            probeCookieBridge();

            try {
                com.cathelperawa.owo.app.script.ScriptHookBridge.register(ctx);
            } catch (Throwable ignore) {
            }

            if (!safe) {
                try {
                    com.cathelperawa.owo.app.script.ScriptHost.bootDelayed();
                } catch (Throwable ignore) {
                }
            } else {
                HookLog.w("[脚本] 安全模式：脚本引擎本次不启动");
            }

            com.cathelperawa.owo.app.core.DataJanitor.schedule(ctx);

            com.cathelperawa.owo.app.core.dex.ClassResolver.installContextProvider(
                    new com.cathelperawa.owo.app.core.dex.ClassResolver.ContextProvider() {
                        @Override public Context get() { return appCtx; }
                    });

            int hv = HostVersion.code(ctx);
            com.cathelperawa.owo.app.util.ReflectCache.bindVersion(hv);
            com.cathelperawa.owo.app.util.ReflectClassCache.bindVersion(hv);

            int mv = com.cathelperawa.owo.app.BuildInfo.VERSION_CODE;
            com.cathelperawa.owo.app.util.ReflectCache.bindModuleVersion(mv);
            com.cathelperawa.owo.app.util.ReflectClassCache.bindModuleVersion(mv);
            HookLog.i("宿主 " + HostVersion.name(ctx) + "（" + hv + "）缓存已绑定，模块 v" + mv);

            try {
                String lvl = HookConfig.get(ctx, "debug_log_level", "debug");
                int l = com.cathelperawa.owo.app.util.CatLog.DEBUG;
                if ("info".equalsIgnoreCase(lvl)) l = com.cathelperawa.owo.app.util.CatLog.INFO;
                else if ("warn".equalsIgnoreCase(lvl)) l = com.cathelperawa.owo.app.util.CatLog.WARN;
                else if ("error".equalsIgnoreCase(lvl)) l = com.cathelperawa.owo.app.util.CatLog.ERROR;
                com.cathelperawa.owo.app.util.CatLog.setMinLevel(l);
            } catch (Throwable t) {

            }

            try {
                boolean realtime = HookConfig.getBoolean(ctx, "debug_error_realtime", false);
                java.io.File logDir = DataDir.account();
                RuntimeErrors.autoFlushTo(logDir, realtime);
                HookLog.i("运行期错误落盘已开启" + (realtime ? "（实时模式）" : "") + " → " + logDir);
            } catch (Throwable t) {
                HookLog.e("错误落盘初始化失败", t);
            }

        } catch (Throwable t) {
            HookLog.e("环境初始化失败", t);
        }

        try {
            if (com.cathelperawa.owo.app.core.Host.isQQ()) {
                com.cathelperawa.owo.app.bridge.QqDbKey.install(cl);
            }
        } catch (Throwable t) {
            HookLog.w("QqDbKey 安装失败 " + t);
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

        BootTiming.mark("环境"); HookLog.i("[BOOT] 环境");

        try {
            hookSettings(cl, pkg);
        } catch (Throwable t) {
            HookLog.e("设置页注入失败", t);
        }
        BootTiming.mark("设置页"); HookLog.i("[BOOT] 设置页");

        try {
            com.cathelperawa.owo.app.core.ModuleCommand.register(ctx);

            Ipc.register(ctx);

            HealthRegistry.registerBuiltins();
        } catch (Throwable t) {
            HookLog.e("命令通道注册失败", t);
        }
        BootTiming.mark("命令通道"); HookLog.i("[BOOT] 命令通道");

        try {
            com.cathelperawa.owo.app.MsgStore.init(ctx);
        } catch (Throwable t) {
            HookLog.e("MsgStore 初始化失败", t);
        }
        BootTiming.mark("消息存储"); HookLog.i("[BOOT] 消息存储");

        try {
            if (com.cathelperawa.owo.app.core.Host.isQQ()) {
                com.cathelperawa.owo.app.bridge.QqMsgTap.install();
                com.cathelperawa.owo.app.bridge.GameShareTap.install(cl);
            }
        } catch (Throwable t) {
            HookLog.w("QqMsgTap 安装失败 " + t);
        }

        if (safe) {
            HookLog.w("安全模式：跳过全部功能加载（" + SafeMode.why() + "）");
        } else {
            try {
                registerFeatures();
            } catch (Throwable t) {
                HookLog.e("功能注册失败", t);
            }
            try {
                HookItemFactory.init(ctx, cl);
                LoadState.enter(LoadState.Stage.HOOKS);
                BootTiming.mark("功能加载"); HookLog.i("[BOOT] 功能加载");
            } catch (Throwable t) {
                HookLog.e("功能项加载失败", t);
            }
        }

        BootTiming.mark("功能结束"); HookLog.i("[BOOT] 功能结束");
        try {
            com.cathelperawa.owo.app.core.StatusStore.save(ctx);
            HookLog.i("功能状态已落盘: " + HookItemFactory.snapshot().replace('\n', ' '));

            com.cathelperawa.owo.app.core.StatusStore.startAutoRefresh(ctx);
        } catch (Throwable t) {
            HookLog.e("状态落盘失败", t);
        }

        try {
            dumpTroopsForConsole(ctx);
        } catch (Throwable t) {
            HookLog.w("群列表落盘失败: " + t);
        }

        try {
            if (safe) {
                HookLog.w("本次为安全模式，未清除加载标记（下次仍进安全模式，直到手动解除）");
            } else {
                SafeMode.markReady(ctx);

                if (LoadState.pending() == 0) LoadState.enter(LoadState.Stage.READY);
                else HookLog.i("还有 " + LoadState.pending() + " 个功能在延迟/后台批次里加载");
            }
        } catch (Throwable t) {
            HookLog.e("加载完成标记失败", t);
        }
    }

    private void probeCookieBridge() {
        try {
            Thread t = new Thread(new Runnable() {
                @Override public void run() {
                    try { Thread.sleep(6000); } catch (Throwable ignore) { }
                    try {
                        String n = com.cathelperawa.owo.app.cookie.CookieBridge.get("netease");
                        String b = com.cathelperawa.owo.app.cookie.CookieBridge.get("bilibili");
                        int nl = (n == null) ? -1 : n.length();
                        int bl = (b == null) ? -1 : b.length();
                        String msg = "Cookie 通道: netease=" + nl + " bilibili=" + bl;
                        HookLog.i(msg);
                        try {
                            com.cathelperawa.owo.app.ai.AiDebug.log("  · " + msg);
                        } catch (Throwable ignore) {
                        }
                    } catch (Throwable e) {
                        HookLog.w("Cookie 通道自检失败: " + e);
                    }
                }
            }, "cat-cookie-probe");
            t.setDaemon(true);
            t.start();
        } catch (Throwable ignore) {
        }
    }

    private static volatile boolean settingsHooked = false;

    private void hookSettings(ClassLoader cl, String pkg) {
        if (settingsHooked) return;

        if (Host.isQQ()) {
            settingsHooked = true;
            try {
                SettingsInjectQQ.install(cl);
                HookLog.i("设置页：QQ 专用注入器已启动");
            } catch (Throwable t) {
                HookLog.e("QQ 设置页注入失败", t);
            }
            return;
        }
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

    private static void dumpTroopsForConsole(final Context ctx) {

        if (Ipc.currentProcMask() != Ipc.PROC_MAIN) return;
        Thread t = new Thread(new Runnable() {
            @Override public void run() {
                try {
                    java.util.Map<String, String> m =
                            com.cathelperawa.owo.app.bridge.TroopApi.allTroopNames();

                    for (int i = 0; i < 3 && m.isEmpty(); i++) {
                        Thread.sleep(4000);
                        m = com.cathelperawa.owo.app.bridge.TroopApi.allTroopNames();
                    }
                    if (m.isEmpty()) {
                        HookLog.w("群列表为空（群缓存未初始化？），跳过落盘");
                        return;
                    }
                    java.io.File dir = ctx.getExternalFilesDir(null);
                    if (dir == null) return;
                    java.io.File f = new java.io.File(dir, "rp_troops.txt");
                    java.io.FileOutputStream out = new java.io.FileOutputStream(f);
                    StringBuilder sb = new StringBuilder();
                    for (java.util.Map.Entry<String, String> e : m.entrySet()) {
                        sb.append(e.getKey()).append('|').append(e.getValue()).append('\n');
                    }
                    out.write(sb.toString().getBytes("UTF-8"));
                    out.close();
                    HookLog.i("群列表已落盘 " + m.size() + " 个 → " + f.getAbsolutePath());
                } catch (Throwable t) {
                    HookLog.w("群列表落盘异常: " + t);
                }
            }
        }, "cat-troops-dump");
        t.setDaemon(true);
        t.start();
    }
}
