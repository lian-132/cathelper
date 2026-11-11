package com.cathelperawa.owo.app.script;

import android.app.Activity;
import android.os.Bundle;

import com.cathelperawa.owo.app.core.HookApi;
import com.cathelperawa.owo.app.core.HookLog;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ActivityBus {

    public interface Handler {
        void onActivity(Activity a);
    }

    private static final Map<String, List<Handler>> MAP =
            new LinkedHashMap<String, List<Handler>>();

    private static volatile boolean installed;

    private ActivityBus() {}

    public static void add(String script, Handler h) {
        add(script, h, null);
    }

    public static void add(String script, Handler h, Activity current) {
        if (script == null || h == null) return;
        ensureInstalled();
        synchronized (MAP) {
            List<Handler> l = MAP.get(script);
            if (l == null) {
                l = new ArrayList<Handler>();
                MAP.put(script, l);
            }
            l.add(h);
        }
        HookLog.i("[Activity] 脚本[" + script + "] 订阅了界面回调"
                + (current == null ? "" : "（立即补发当前页面 " + current.getClass().getName() + "）"));

        if (current != null) fireOne(h, current);
        java.util.List<Activity> alive = aliveActivities();
        for (int i = 0; i < alive.size(); i++) {
            Activity a = alive.get(i);
            if (a != null && a != current) fireOne(h, a);
        }
    }

    public static int removeByScript(String script) {
        synchronized (MAP) {
            List<Handler> l = MAP.remove(script);
            return l == null ? 0 : l.size();
        }
    }

    public static void clear() {
        synchronized (MAP) {
            MAP.clear();
        }
    }

    private static void ensureInstalled() {
        if (installed) return;
        synchronized (ActivityBus.class) {
            if (installed) return;
            try {
                java.lang.reflect.Method m = Activity.class.getDeclaredMethod(
                        "onCreate", Bundle.class);
                HookApi.after(m, new HookApi.Action() {
                    @Override public void call(
                            de.robv.android.xposed.XC_MethodHook.MethodHookParam p) {
                        try {
                            if (p.thisObject instanceof Activity) {
                                fire((Activity) p.thisObject);
                            }
                        } catch (Throwable ignore) {
                        }
                    }
                }, null);
                
                try {
                    java.lang.reflect.Method r = Activity.class.getDeclaredMethod("onResume");
                    HookApi.after(r, new HookApi.Action() {
                        @Override public void call(
                                de.robv.android.xposed.XC_MethodHook.MethodHookParam p) {
                            try {
                                if (p.thisObject instanceof Activity) {
                                    fire((Activity) p.thisObject);
                                }
                            } catch (Throwable ignore) {
                            }
                        }
                    }, null);
                } catch (Throwable ignore) {
                }
                installed = true;
                HookLog.i("[Activity] 界面回调总线已装（onCreate + onResume）");
            } catch (Throwable t) {
                HookLog.e("[Activity] 装界面回调失败", t);
            }
        }
    }

    private static void fireOne(final Handler h, final Activity a) {
        if (a == null) return;
        Runnable r = new Runnable() {
            @Override public void run() {
                try {
                    h.onActivity(a);
                } catch (Throwable t) {
                    HookLog.w("[Activity] 补发回调出错：" + t);
                }
            }
        };
        try {

            new android.os.Handler(android.os.Looper.getMainLooper()).post(r);
        } catch (Throwable t) {
            try {
                a.runOnUiThread(r);
            } catch (Throwable ignore) {
            }
        }
    }

    private static java.util.List<Activity> aliveActivities() {
        java.util.ArrayList<Activity> out = new java.util.ArrayList<Activity>();
        try {
            Class<?> at = Class.forName("android.app.ActivityThread");
            Object thread = at.getMethod("currentActivityThread").invoke(null);
            java.lang.reflect.Field f = at.getDeclaredField("mActivities");
            f.setAccessible(true);
            Object mapObj = f.get(thread);
            if (!(mapObj instanceof java.util.Map)) return out;
            java.util.Map<?, ?> map = (java.util.Map<?, ?>) mapObj;
            for (Object rec : map.values()) {
                try {
                    java.lang.reflect.Field af = rec.getClass().getDeclaredField("activity");
                    af.setAccessible(true);
                    Object a = af.get(rec);
                    if (a instanceof Activity) out.add((Activity) a);
                } catch (Throwable ignore) {
                }
            }
        } catch (Throwable t) {
            HookLog.w("[Activity] 取存活列表失败：" + t);
        }
        HookLog.i("[Activity] 存活列表 " + out.size() + " 个");
        return out;
    }

    private static void fire(final Activity a) {

        try {
            a.runOnUiThread(new Runnable() {
                @Override public void run() {
                    fireNow(a);
                }
            });
        } catch (Throwable t) {
            fireNow(a);
        }
    }

    private static void fireNow(Activity a) {
        try {
            HookLog.i("[Activity] 回调 " + a.getClass().getName());
        } catch (Throwable ignore) {
        }
        List<Handler> snapshot;
        synchronized (MAP) {
            if (MAP.isEmpty()) return;
            snapshot = new ArrayList<Handler>();
            for (List<Handler> l : MAP.values()) snapshot.addAll(l);
        }
        for (int i = 0; i < snapshot.size(); i++) {
            try {
                snapshot.get(i).onActivity(a);
            } catch (Throwable t) {
                HookLog.w("[Activity] 回调出错：" + t);
            }
        }
    }
}
