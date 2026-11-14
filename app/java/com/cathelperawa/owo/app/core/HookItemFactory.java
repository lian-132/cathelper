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

        LoadState.expect(deferred + background);
        if (deferred > 0) scheduleDeferred(ctx, cl, deferredList);
        if (background > 0) scheduleBackground(ctx, cl, backgroundList);

        if (deferred == 0 && background == 0) LoadState.enter(LoadState.Stage.READY);

        if (deferred > 0 || background > 0) {
            HookLog.i("四档调度: 同步 " + (ordered.size() - deferred - background)
                    + " / 首帧后 " + deferred + " / 后台 " + background);
        }
    }

    private static boolean postToIdle(final Runnable task) {
        final android.os.Looper l = android.os.Looper.getMainLooper();
        if (l == null) return false;
        android.os.MessageQueue q;
        try {
            q = l.getQueue();
        } catch (Throwable t) {
            return false;
        }
        if (q == null) return false;
        final java.util.concurrent.atomic.AtomicBoolean once =
                new java.util.concurrent.atomic.AtomicBoolean(false);
        q.addIdleHandler(new android.os.MessageQueue.IdleHandler() {
            @Override public boolean queueIdle() {
                if (!once.compareAndSet(false, true)) return false;
                try {
                    task.run();
                } catch (Throwable t) {
                    HookLog.e("DEFERRED 空闲批执行失败", t);
                }
                return false;
            }
        });
        return true;
    }

    private static void saveStatusQuietly(Context ctx) {
        try {
            if (ctx != null) StatusStore.save(ctx);
        } catch (Throwable ignore) {
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
                BootTiming.mark("延迟批开始");
                LoadState.enter(LoadState.Stage.HOOKS);
                for (Class<? extends HookItemBase> cls : list) {
                    try {
                        loadOne(ctx, cl, cls);
                    } catch (Throwable t) {
                        HookLog.e("延迟加载失败: " + cls.getSimpleName(), t);
                    } finally {
                        LoadState.completeOne();
                    }
                }
                HookLog.i("首帧后批次完成 " + list.size() + " 个");

                BootTiming.mark("延迟批结束");
                saveStatusQuietly(ctx);
            }
        };

        try {
            if (postToIdle(task)) {
                HookLog.i("DEFERRED 批次已挂到主线程空闲回调");
                return;
            }
        } catch (Throwable t) {
            HookLog.w("空闲回调不可用，退回首帧方案：" + t);
        }

        try {
            if (StartupGate.arm(cl, "DEFERRED", task)) return;
        } catch (Throwable t) {
            HookLog.w("启动闸门异常，退回首帧方案：" + t);
        }
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
                        } finally {
                            LoadState.completeOne();
                        }
                    }
                    saveStatusQuietly(ctx);
                }
            }, "CatHelper-BG-Load");
            t.setDaemon(true);
            t.setPriority(Thread.MIN_PRIORITY);
            t.start();
        } catch (Throwable e) {
            HookLog.e("后台线程创建失败，BACKGROUND 改同步执行", e);
            for (Class<? extends HookItemBase> cls : list) {
                try {
                    loadOne(ctx, cl, cls);
                } finally {
                    LoadState.completeOne();
                }
            }
        }
    }

    static LoadPriority priority(Class<? extends HookItemBase> cls) {
        HookItem m = cls.getAnnotation(HookItem.class);
        return m == null ? LoadPriority.EARLY : m.priority();
    }

    private static void loadOne(Context ctx, ClassLoader cl, Class<? extends HookItemBase> cls) {

            HookItem mmEarly = cls.getAnnotation(HookItem.class);
            if (mmEarly != null) {
                String want0 = com.cathelperawa.owo.app.core.Host.isQQ() ? "qq" : "tim";
                boolean ok0 = false;
                for (String h : mmEarly.hosts()) {
                    if (want0.equalsIgnoreCase(h)) { ok0 = true; break; }
                }
                if (!ok0) {
                    HookLog.d("跳过(非本宿主): " + name(cls) + " @ " + want0);
                    return;
                }
            }
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

            try {
                if (item.mainProcessOnly() && Proc.isSlave()) {
                    HookLog.i("跳过(子进程): " + name(cls) + " @ " + Proc.label());
                    return;
                }
            } catch (Throwable ignore) {
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

            if (mm != null) {
                String want = com.cathelperawa.owo.app.core.Host.isQQ() ? "qq" : "tim";
                boolean allowed = false;
                for (String h : mm.hosts()) {
                    if (want.equalsIgnoreCase(h)) { allowed = true; break; }
                }
                if (!allowed) {
                    HookLog.i("跳过(非本宿主): " + name(cls) + " @ " + want);
                    return;
                }
            }
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

    public static HookItemBase byName(String name) {
        if (name == null) return null;
        for (HookItemBase it : INSTANCES) {
            try {
                if (name.equals(it.featureName())) return it;
            } catch (Throwable ignore) {
            }
        }
        return null;
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
        HookItem meta = cls.getAnnotation(HookItem.class);
        return meta == null ? cls.getSimpleName() : meta.name();
    }
}
