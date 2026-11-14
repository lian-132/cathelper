package com.cathelperawa.owo.app.core;

import de.robv.android.xposed.XC_MethodHook;

import java.util.concurrent.atomic.AtomicBoolean;

public final class StartupGate {

    private static final AtomicBoolean FIRED = new AtomicBoolean(false);

    private static volatile boolean sArmed;

    private static final String[] ANCHOR_CLASSES = {

            "com.tencent.mobileqq.startup.step.a",

            "com.tencent.mobileqq.startup.step.LoadDex",
            "com.tencent.mobileqq.startup.step.LoadDexTask",

            "com.tencent.mobileqq.startup.step.InitMemoryCache",
            "com.tencent.mobileqq.startup.step.SetSplash",
    };

    private static final String[] ANCHOR_METHODS = {"run", "doStep"};

    private StartupGate() {}

    public static boolean arm(ClassLoader cl, String tag, final Runnable task) {
        if (cl == null || task == null) return false;
        if (sArmed) return false;

        for (String clsName : ANCHOR_CLASSES) {
            Class<?> c = HookApi.findClass(cl, clsName);
            if (c == null) continue;

            java.lang.reflect.Method m = pickAnchor(c);
            if (m == null) continue;

            try {

                Object h = HookApi.after(m, new HookApi.Action() {
                    @Override public void call(XC_MethodHook.MethodHookParam p) {
                        fire(tag, task);
                    }
                }, null);
                if (h != null) {
                    sArmed = true;
                    HookLog.i("启动闸门已挂到 " + clsName + "#" + m.getName() + "（" + tag + "）");
                    return true;
                }
            } catch (Throwable t) {
                HookLog.w("启动闸门挂 " + clsName + " 失败：" + t);
            }
        }

        if (armSystemAnchor(tag, task)) {
            sArmed = true;
            return true;
        }
        HookLog.i("启动闸门未找到可用锚点（" + tag + "），退回首帧方案");
        return false;
    }

    private static boolean armSystemAnchor(String tag, final Runnable task) {
        try {
            Class<?> instr = android.app.Instrumentation.class;
            java.lang.reflect.Method m = instr.getDeclaredMethod("callActivityOnCreate",
                    android.app.Activity.class, android.os.Bundle.class);
            m.setAccessible(true);
            Object h = HookApi.after(m, new HookApi.Action() {
                @Override public void call(XC_MethodHook.MethodHookParam p) {
                    fire(tag + "/首Activity", task);
                }
            }, null);
            if (h != null) {
                HookLog.i("启动闸门已挂到系统锚点 Instrumentation.callActivityOnCreate（" + tag + "）");
                return true;
            }
        } catch (Throwable t) {
            HookLog.w("系统锚点不可用：" + t);
        }
        return false;
    }

    private static java.lang.reflect.Method pickAnchor(Class<?> c) {
        for (String name : ANCHOR_METHODS) {
            try {
                for (java.lang.reflect.Method m : c.getDeclaredMethods()) {
                    if (!m.getName().equals(name)) continue;
                    if (m.getParameterTypes().length != 0) continue;
                    Class<?> r = m.getReturnType();
                    if (r == boolean.class || r == int.class || r == void.class) {
                        m.setAccessible(true);
                        return m;
                    }
                }
            } catch (Throwable ignore) {
            }
        }
        return null;
    }

    private static void fire(String tag, Runnable task) {
        if (!FIRED.compareAndSet(false, true)) return;
        try {
            HookLog.i("启动闸门触发：" + tag);
            task.run();
        } catch (Throwable t) {
            HookLog.e("启动闸门执行失败：" + tag, t);
        }
    }

    public static boolean armed() {
        return sArmed;
    }

    public static boolean fired() {
        return FIRED.get();
    }
}
