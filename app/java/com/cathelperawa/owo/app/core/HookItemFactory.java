package com.cathelperawa.owo.app.core;

import android.content.Context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class HookItemFactory {

    private static final List<Class<? extends HookItemBase>> REGISTRY =
            new ArrayList<Class<? extends HookItemBase>>();

    private static final List<HookItemBase> INSTANCES =
            new ArrayList<HookItemBase>();

    private static Context appCtx;

    private HookItemFactory() {}

    public static void register(Class<? extends HookItemBase> cls) {
        if (!REGISTRY.contains(cls)) REGISTRY.add(cls);
    }

    public static void init(Context ctx, ClassLoader cl) {
        appCtx = ctx;
        List<Class<? extends HookItemBase>> ordered = sortAll();

        int deferred = 0, background = 0;
        List<Class<? extends HookItemBase>> deferredList = new ArrayList<Class<? extends HookItemBase>>();
        List<Class<? extends HookItemBase>> backgroundList = new ArrayList<Class<? extends HookItemBase>>();

        for (Class<? extends HookItemBase> cls : ordered) {
            LoadPriority pr = priority(cls);
            if (pr == LoadPriority.MANUAL) continue;
            if (pr == LoadPriority.DEFERRED) { deferredList.add(cls); deferred++; continue; }
            if (pr == LoadPriority.BACKGROUND) { backgroundList.add(cls); background++; continue; }
            loadOne(ctx, cl, cls);
        }

        if (deferred > 0) scheduleDeferred(ctx, cl, deferredList);
        if (background > 0) scheduleBackground(ctx, cl, backgroundList);

        if (deferred > 0 || background > 0) {
            HookLog.i("四档调度: 同步 " + (ordered.size() - deferred - background)
                    + " / 首帧后 " + deferred + " / 后台 " + background);
        }
    }

    private static List<Class<? extends HookItemBase>> sortAll() {
        List<Class<? extends HookItemBase>> ordered =
                new ArrayList<Class<? extends HookItemBase>>(REGISTRY);
        Collections.sort(ordered, new Comparator<Class<? extends HookItemBase>>() {
            @Override
            public int compare(Class<? extends HookItemBase> a, Class<? extends HookItemBase> b) {
                return order(a) - order(b);
            }
        });
        return ordered;
    }

    private static void scheduleDeferred(final Context ctx, final ClassLoader cl,
                                         final List<Class<? extends HookItemBase>> list) {
        Runnable task = new Runnable() {
            @Override public void run() {
                for (Class<? extends HookItemBase> cls : list) {
                    try {
                        loadOne(ctx, cl, cls);
                    } catch (Throwable t) {
                        HookLog.e("延迟加载失败: " + cls.getSimpleName(), t);
                    }
                }
            }
        };
        try {
            android.os.Handler h = new android.os.Handler(android.os.Looper.getMainLooper());
            h.post(new Runnable() {
                @Override public void run() {
                    try {
                        android.view.Choreographer.getInstance()
                                .postFrameCallback(new android.view.Choreographer.FrameCallback() {
                                    @Override public void doFrame(long frameTimeNanos) {
                                        task.run();
                                    }
                                });
                    } catch (Throwable t) {

                        HookLog.w("首帧回调不可用，DEFERRED 立即执行: " + t);
                        task.run();
                    }
                }
            });
        } catch (Throwable t) {
            HookLog.w("主线程不可用，DEFERRED 立即执行: " + t);
            task.run();
        }
    }

    private static void scheduleBackground(final Context ctx, final ClassLoader cl,
                                           final List<Class<? extends HookItemBase>> list) {
        try {
            Thread t = new Thread(new Runnable() {
                @Override public void run() {
                    for (Class<? extends HookItemBase> cls : list) {
                        try {
                            loadOne(ctx, cl, cls);
                        } catch (Throwable e) {
                            HookLog.e("后台加载失败: " + cls.getSimpleName(), e);
                        }
                    }
                }
            }, "CatHelper-BG-Load");
            t.setDaemon(true);
            t.setPriority(Thread.MIN_PRIORITY);
            t.start();
        } catch (Throwable e) {
            HookLog.e("后台线程创建失败，BACKGROUND 改同步执行", e);
            for (Class<? extends HookItemBase> cls : list) loadOne(ctx, cl, cls);
        }
    }

    static LoadPriority priority(Class<? extends HookItemBase> cls) {
        HookItem m = cls.getAnnotation(HookItem.class);
        return m == null ? LoadPriority.EARLY : m.priority();
    }

    private static void loadOne(Context ctx, ClassLoader cl, Class<? extends HookItemBase> cls) {
            HookItemBase item = null;
            try {
                item = cls.newInstance();
                item.bind(ctx, cl);
                INSTANCES.add(item);
            } catch (Throwable t) {
                HookLog.e("实例化功能项失败: " + cls.getName(), t);

                try {
                    item = new BrokenItem(cls, t);
                    INSTANCES.add(item);
                } catch (Throwable ignore) {
                }
                return;
            }

            FeatureReport rep = item.report();
            boolean enabled = false;
            try {
                enabled = item.isEnabled();
            } catch (Throwable t) {
                HookLog.e("读取开关失败: " + name(cls), t);
            }
            rep.begin();
            long t0 = System.currentTimeMillis();

            HookItem mm = cls.getAnnotation(HookItem.class);
            int minV = mm == null ? 0 : mm.minVersion();
            int maxV = mm == null ? 0 : mm.maxVersion();
            int hostV = HostVersion.code(ctx);
            if (minV > 0 && hostV > 0 && hostV < minV) {
                rep.markUnsupported(minV);
                HookLog.i("跳过: " + name(cls) + " 需宿主 ≥ " + HostVersion.format(minV)
                        + "，当前 " + HostVersion.format(hostV));
                return;
            }
            if (maxV > 0 && hostV > 0 && hostV > maxV) {
                rep.markUnsupported(-maxV);
                HookLog.i("跳过: " + name(cls) + " 仅适用于宿主 ≤ " + HostVersion.format(maxV)
                        + "（当前 " + HostVersion.format(hostV) + "，新版可能已内置）");
                return;
            }

            try {
                item.entry(cl);
                if (!enabled) rep.markDisabled();
                else rep.done();
            } catch (Throwable t) {
                rep.markError(t);
                HookLog.e("加载功能项失败: " + name(cls), t);
            }

            com.cathelperawa.owo.app.core.SelfCheckResult sc = null;
            try {
                sc = item.runSelfCheck();
            } catch (Throwable t) {
                HookLog.e("自检失败: " + name(cls), t);
            }
            HookLog.i("已加载: " + name(cls) + " 开关=" + enabled
                    + " 状态=" + rep.status().label
                    + " " + rep.summary()
                    + (sc != null && !sc.isEmpty() ? " 自检=" + sc.summary() : "")
                    + " (" + (System.currentTimeMillis() - t0) + "ms)");
    }

    private static final class BrokenItem extends HookItemBase {
        private final Class<?> cls;
        private final Throwable err;

        BrokenItem(Class<?> cls, Throwable err) {
            this.cls = cls;
            this.err = err;
            report().setStatus(FeatureStatus.ERROR);
            report().fail("实例化失败", err);
        }

        @Override
        public void entry(ClassLoader cl) {

        }
    }

    public static List<HookItemBase> all() {
        return INSTANCES;
    }

    public static String snapshot() {
        StringBuilder sb = new StringBuilder();
        int[] cnt = new int[FeatureStatus.values().length];
        sb.append("══ 功能体检 ══\n");
        for (HookItemBase item : INSTANCES) {
            FeatureReport r = item.report();
            if (r != null) cnt[r.status().ordinal()]++;
        }
        sb.append("功能 ").append(INSTANCES.size());
        for (FeatureStatus s : FeatureStatus.values()) {
            if (s == FeatureStatus.UNKNOWN) continue;
            sb.append(" · ").append(s.label).append(' ').append(cnt[s.ordinal()]);
        }
        sb.append('\n');
        return sb.toString();
    }

    public static int count(FeatureStatus s) {
        int n = 0;
        for (HookItemBase item : INSTANCES) {
            FeatureReport r = item.report();
            if (r != null && r.status() == s) n++;
        }
        return n;
    }

    public static int problemCount() {
        return count(FeatureStatus.FAILED) + count(FeatureStatus.ERROR);
    }

    public static Context context() {
        return appCtx;
    }

    private static int order(Class<?> cls) {
        HookItem meta = cls.getAnnotation(HookItem.class);
        return meta == null ? 100 : meta.order();
    }

    private static String name(Class<?> cls) {
        try {
            java.lang.reflect.Field f = cls.getField("META_name");
            Object v = f.get(null);
            if (v instanceof String && ((String) v).length() > 0) return (String) v;
        } catch (Throwable ignore) {
        }
        HookItem meta = cls.getAnnotation(HookItem.class);
        return meta == null || meta.name().length() == 0 ? cls.getSimpleName() : meta.name();
    }
}
