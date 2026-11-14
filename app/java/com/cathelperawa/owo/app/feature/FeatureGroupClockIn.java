package com.cathelperawa.owo.app.feature;

import com.cathelperawa.owo.app.bridge.QQEnv;
import com.cathelperawa.owo.app.core.HookApi;
import com.cathelperawa.owo.app.core.HookConfig;
import com.cathelperawa.owo.app.core.HookItem;
import com.cathelperawa.owo.app.core.HookItemBase;
import com.cathelperawa.owo.app.core.HookLog;
import com.cathelperawa.owo.app.core.LoadPriority;
import com.cathelperawa.owo.app.core.SelfCheckResult;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@HookItem(name = "群打卡", path = "聊天/群打卡", order = 65,
        description = "每天自动给选中的群打卡（零点抢签 + 漏了明天补）", defaultEnabled = false,
        priority = LoadPriority.BACKGROUND)
public class FeatureGroupClockIn extends HookItemBase {

    public static final String K = "chat_clockin";

    public static final String K_GROUPS = "chat_clockin_groups";

    public static final String K_DEADLINE = "chat_clockin_deadline_sec";

    public static final String K_CONC = "chat_clockin_concurrency";

    public static final String K_DONE_DATE = "chat_clockin_done_date";

    public static final String K_DONE = "chat_clockin_done";

    private static final String[] CLASS_CANDIDATES = {
            "com.tencent.mobileqq.troop.clockin.handler.TroopClockInHandler",
            "com.tencent.mobileqq.troop.clockin.handler.ClockInHandler",
            "com.tencent.mobileqq.troop.clockin.handler.n",
    };

    private static final int BURST_THREADS_MAX = 256;

    private static final long GAP_IDLE_MS = 2500L;

    private static final long DAWN_WINDOW_MS = 10L * 60 * 1000;

    private static final long PREPARE_MS = 3L * 60 * 1000;

    private static final long AFTER_LOGIN_DELAY_MS = 90L * 1000;

    private static final long IDLE_RETRY_GAP_MS = 10L * 60 * 1000;

    private static volatile Object[] prewarmedPool = null;
    private static volatile int prewarmedCount = 0;

    private static volatile boolean running = false;

    private static volatile long lastIdleRun = 0;

    private static final java.util.concurrent.atomic.AtomicBoolean busy =
            new java.util.concurrent.atomic.AtomicBoolean(false);

    private static volatile int dawnRounds = 0;

    private static volatile String dawnRoundsDay = "";

    private static volatile FeatureGroupClockIn INSTANCE = null;

    private static volatile java.lang.Class<?> sHandlerCls = null;

    private static volatile Method sClockInMethod = null;

    @Override
    public String switchKey() {
        return K;
    }

    private boolean on() {
        try {
            return ctx != null && HookConfig.getBoolean(ctx, K, false);
        } catch (Throwable t) {
            return false;
        }
    }

    @Override
    public void entry(ClassLoader cl) {
        hint("零点自动抢签；漏的群在上号后补打");

        final Class<?> handler = findHandlerClass(cl);
        if (handler == null) {
            fail("未找到 TroopClockInHandler（宿主改版？）");
            return;
        }
        final Method clockIn = findClockInMethod(handler);
        if (clockIn == null) {
            fail("TroopClockInHandler 里没有 (String,String) 方法");
            return;
        }
        if (findCtor(handler) == null) {
            fail("TroopClockInHandler 没有可用构造器");
            return;
        }
        HookLog.i("群打卡：目标 " + handler.getName() + "#" + clockIn.getName());
        INSTANCE = this;
        sHandlerCls = handler;
        sClockInMethod = clockIn;

        if (running) return;
        running = true;

        Thread boot = new Thread(new Runnable() {
            @Override public void run() {
                try {
                    Thread.sleep(AFTER_LOGIN_DELAY_MS);
                } catch (InterruptedException e) {
                    return;
                }
                try {
                    if (!on()) return;
                    List<String> todo = pendingToday();
                    if (todo.isEmpty()) {
                        HookLog.i("群打卡：上号检查 —— 今天已经打完了");
                        return;
                    }
                    HookLog.i("群打卡：上号检查 —— 还有 " + todo.size() + " 个群没打，补上");
                    runOnce(handler, clockIn, todo, false);
                } catch (Throwable t) {
                    noteRuntimeError(t);
                }
            }
        }, "cat-clockin-boot");
        boot.setDaemon(true);
        boot.start();

        Thread t = new Thread(new Runnable() {
            @Override public void run() {
                loop(handler, clockIn);
            }
        }, "cat-clockin");
        t.setDaemon(true);
        t.start();
        hit();
    }

    private static Class<?> findHandlerClass(ClassLoader cl) {
        for (String n : CLASS_CANDIDATES) {
            Class<?> c = HookApi.findClass(cl, n);
            if (c != null) return c;
        }
        return null;
    }

    private static Method findClockInMethod(Class<?> c) {
        for (Method m : c.getDeclaredMethods()) {
            Class<?>[] ps = m.getParameterTypes();
            if (ps.length != 2) continue;
            if (ps[0] != String.class || ps[1] != String.class) continue;
            if (m.getReturnType() != void.class) continue;
            try {
                m.setAccessible(true);
            } catch (Throwable ignore) {
            }
            return m;
        }
        return null;
    }

    private static Constructor<?> findCtor(Class<?> c) {
        Object app = null;
        try {
            app = QQEnv.appRuntime();
        } catch (Throwable ignore) {
        }
        Constructor<?> fallback = null;
        for (Constructor<?> k : c.getDeclaredConstructors()) {
            Class<?>[] ps = k.getParameterTypes();
            if (ps.length != 1) continue;
            if (fallback == null) fallback = k;
            if (app != null && ps[0].isInstance(app)) {
                try {
                    k.setAccessible(true);
                } catch (Throwable ignore) {
                }
                return k;
            }
        }
        if (fallback != null) {
            try {
                fallback.setAccessible(true);
            } catch (Throwable ignore) {
            }
        }
        return fallback;
    }

    private void loop(Class<?> handlerCls, Method clockIn) {
        while (true) {
            long sleep = 30L * 1000;
            try {

                String d = today();
                if (!d.equals(dawnRoundsDay)) {
                    dawnRoundsDay = d;
                    dawnRounds = 0;
                    lastIdleRun = 0;
                }
                if (!on()) {
                    Thread.sleep(sleep);
                    continue;
                }
                List<String> todo = pendingToday();
                if (todo.isEmpty()) {

                    Thread.sleep(5L * 60 * 1000);
                    continue;
                }

                long sinceDawn = sinceDawnMs();
                long toDawn = toNextDawnMs();

                if (sinceDawn >= 0 && sinceDawn < DAWN_WINDOW_MS) {

                    if (dawnRounds < 3) {
                        dawnRounds++;
                        runOnce(handlerCls, clockIn, todo, true);
                    } else {
                        sleep = 60L * 1000;
                    }
                } else if (toDawn > 0 && toDawn <= PREPARE_MS) {

                    if (toDawn > 80) Thread.sleep(toDawn - 50);
                    long dawn = todayDawnMs() + 24L * 60 * 60 * 1000;
                    while (System.currentTimeMillis() < dawn) {
                        Thread.yield();
                    }

                    List<String> todoDawn = pendingToday();
                    prewarm(handlerCls, todoDawn.size());
                    runOnce(handlerCls, clockIn, todoDawn, true);
                } else if (System.currentTimeMillis() - lastIdleRun >= IDLE_RETRY_GAP_MS) {

                    HookLog.i("群打卡：补打 " + todo.size() + " 个群（慢速）");
                    lastIdleRun = System.currentTimeMillis();
                    runOnce(handlerCls, clockIn, todo, false);
                }
            } catch (InterruptedException e) {
                return;
            } catch (Throwable t) {
                noteRuntimeError(t);
                HookLog.e("群打卡：循环出错", t);
            }
            try {
                Thread.sleep(Math.max(500, sleep));
            } catch (InterruptedException e) {
                return;
            }
        }
    }

    private long deadlineMs() {
        try {
            int sec = Integer.parseInt(HookConfig.get(ctx, K_DEADLINE, "3"));
            return sec <= 0 ? 0 : sec * 1000L;
        } catch (Throwable t) {
            return 3000L;
        }
    }

    private int concurrency(int total) {
        try {
            int c = Integer.parseInt(HookConfig.get(ctx, K_CONC, "64"));
            if (c <= 0) return total;
            return Math.min(BURST_THREADS_MAX, Math.min(c, Math.max(1, total)));
        } catch (Throwable t) {
            return Math.min(64, Math.max(1, total));
        }
    }

    private static long sinceDawnMs() {
        return System.currentTimeMillis() - todayDawnMs();
    }

    private static long toNextDawnMs() {
        return todayDawnMs() + 24L * 60 * 60 * 1000 - System.currentTimeMillis();
    }

    private static long todayDawnMs() {
        java.util.Calendar c = java.util.Calendar.getInstance();
        c.set(java.util.Calendar.HOUR_OF_DAY, 0);
        c.set(java.util.Calendar.MINUTE, 0);
        c.set(java.util.Calendar.SECOND, 0);
        c.set(java.util.Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    private void runOnce(Class<?> handlerCls, Method clockIn, List<String> groups, boolean fast) {
        if (groups == null || groups.isEmpty()) return;
        if (!busy.compareAndSet(false, true)) {
            HookLog.i("群打卡：上一轮还在跑，跳过这次");
            return;
        }
        try {
            runOnceLocked(handlerCls, clockIn, groups, fast);
        } finally {
            busy.set(false);
        }
    }

    private void runOnceLocked(Class<?> handlerCls, Method clockIn, List<String> groups, boolean fast) {
        final String self = currentUinOrNull();
        if (self == null) {
            HookLog.w("群打卡：拿不到自己的 QQ 号，本轮跳过");
            return;
        }

        long t0 = System.currentTimeMillis();
        final List<String> failed = new ArrayList<String>();
        List<String> okList = new ArrayList<String>();

        if (fast) {
            okList = runBurst(handlerCls, clockIn, groups, self, failed);
        } else {
            for (String g : groups) {
                if (!on()) return;
                if (invokeOne(handlerCls, clockIn, g, self)) {
                    okList.add(g);
                    HookLog.i("群打卡：" + g + " 已提交");
                } else {
                    failed.add(g);
                }
                try {
                    Thread.sleep(GAP_IDLE_MS);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }

        if (!okList.isEmpty()) markDone(okList);
        fire("clockIn", okList.size());
        long cost = System.currentTimeMillis() - t0;
        if (failed.isEmpty()) {
            HookLog.i("群打卡：完成 " + okList.size() + " 个群，耗时 " + cost + "ms，今天的账已记上");
        } else {
            HookLog.w("群打卡：成功 " + okList.size() + " / 失败 " + failed.size()
                    + "，耗时 " + cost + "ms（失败的会再补）");
        }
    }

    private List<String> runBurst(Class<?> handlerCls, Method clockIn, List<String> groups,
                                  final String self, List<String> failed) {
        final Class<?> hc = handlerCls;
        final Method cm = clockIn;
        final int n = groups.size();
        int conc = concurrency(n);
        int batches = (n + conc - 1) / conc;
        long budget = deadlineMs();

        long gap = (budget > 0 && batches > 1) ? Math.max(0L, budget / batches) : 0L;

        java.util.concurrent.ExecutorService pool =
                java.util.concurrent.Executors.newFixedThreadPool(conc);
        List<java.util.concurrent.Future<String[]>> futures =
                new ArrayList<java.util.concurrent.Future<String[]>>();
        final Object[] warm = prewarmedPool;
        long start = System.currentTimeMillis();

        for (int b = 0; b < batches; b++) {

            if (b > 0 && gap > 0) {
                long target = start + b * gap;
                long wait = target - System.currentTimeMillis();
                duringSleep(wait);
            }
            int from = b * conc;
            int to = Math.min(n, from + conc);
            for (int i = from; i < to; i++) {
                final String g = groups.get(i);
                final Object ready = (warm != null && i < warm.length) ? warm[i] : null;
                try {
                    futures.add(pool.submit(new java.util.concurrent.Callable<String[]>() {
                        @Override public String[] call() {
                            return new String[]{g, invokeWith(hc, cm, g, self, ready) ? "1" : "0"};
                        }
                    }));
                } catch (Throwable t) {
                    failed.add(g);
                }
            }
        }

        pool.shutdown();
        try {
            pool.awaitTermination(90, java.util.concurrent.TimeUnit.SECONDS);
        } catch (InterruptedException ignore) {
        }
        prewarmedPool = null;
        List<String> ok = new ArrayList<String>();

        for (int i = 0; i < futures.size(); i++) {
            try {
                String[] r = futures.get(i).get();
                if (r == null) continue;
                if ("1".equals(r[1])) ok.add(r[0]);
                else failed.add(r[0]);
            } catch (Throwable t) {

            }
        }
        return ok;
    }

    private void duringSleep(long ms) {
        if (ms <= 0) return;
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void prewarm(Class<?> handlerCls, int n) {
        if (n <= 0) return;
        try {
            Object app = QQEnv.appRuntime();
            if (app == null) return;
            Constructor<?> k = findCtor(handlerCls);
            if (k == null) return;
            if (k.getParameterTypes().length != 1) return;
            Object[] pool = new Object[n];
            for (int i = 0; i < n; i++) {
                try {
                    pool[i] = k.newInstance(app);
                } catch (Throwable ignore) {
                    pool[i] = null;
                }
            }
            prewarmedPool = pool;
            prewarmedCount++;
        } catch (Throwable ignore) {
        }
    }

    private boolean invokeOne(Class<?> handlerCls, Method clockIn, String group, String self) {
        return invokeWith(handlerCls, clockIn, group, self, null);
    }

    private boolean invokeWith(Class<?> handlerCls, Method clockIn, String group, String self,
                               Object ready) {
        try {
            if (ready != null) {
                clockIn.invoke(ready, group, self);
                return true;
            }
            Constructor<?> k = findCtor(handlerCls);
            if (k == null) return false;
            Class<?>[] ps = k.getParameterTypes();
            Object handler;
            if (ps.length == 1) {
                Object app = QQEnv.appRuntime();
                if (app == null) return false;
                handler = k.newInstance(app);
            } else {
                handler = k.newInstance();
            }
            clockIn.invoke(handler, group, self);
            return true;
        } catch (Throwable t) {
            HookLog.w("群打卡：" + group + " 失败：" + shortOf(t));
            return false;
        }
    }

    private List<String> pendingToday() {
        return pendingTodayOf(ctx);
    }

    private List<String> pendingGroups() {
        return pendingGroupsOf(ctx);
    }

    private Set<String> doneTodaySet() {
        return doneTodaySetOf(ctx);
    }

    private static List<String> pendingTodayOf(android.content.Context c) {
        Set<String> done = doneTodaySetOf(c);
        List<String> out = new ArrayList<String>();
        for (String g : pendingGroupsOf(c)) {
            if (!done.contains(g)) out.add(g);
        }
        return out;
    }

    private static List<String> pendingGroupsOf(android.content.Context c) {
        List<String> out = new ArrayList<String>();
        try {
            String raw = HookConfig.get(c, K_GROUPS, "");
            if (raw == null) return out;
            for (String p : raw.split(",")) {
                String s = p.trim();
                if (s.length() >= 5 && !out.contains(s)) out.add(s);
            }
        } catch (Throwable ignore) {
        }
        return out;
    }

    private static Set<String> doneTodaySetOf(android.content.Context c) {
        Set<String> s = new LinkedHashSet<String>();
        try {
            String date = HookConfig.get(c, K_DONE_DATE, "");
            if (!today().equals(date)) return s;
            String raw = HookConfig.get(c, K_DONE, "");
            if (raw == null) return s;
            for (String p : raw.split(",")) {
                String t = p.trim();
                if (t.length() > 0) s.add(t);
            }
        } catch (Throwable ignore) {
        }
        return s;
    }

    private void markDone(List<String> okGroups) {
        try {
            Set<String> done = doneTodaySet();
            done.addAll(okGroups);
            StringBuilder sb = new StringBuilder();
            for (String g : done) {
                if (sb.length() > 0) sb.append(',');
                sb.append(g);
            }
            HookConfig.set(ctx, K_DONE_DATE, today());
            HookConfig.set(ctx, K_DONE, sb.toString());
        } catch (Throwable t) {
            HookLog.w("群打卡：记账失败 " + t);
        }
    }

    private static String today() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    }

    private static String currentUinOrNull() {
        try {
            String u = QQEnv.currentUin();
            return (u == null || u.length() == 0) ? null : u;
        } catch (Throwable t) {
            return null;
        }
    }

    private static String shortOf(Throwable t) {
        if (t == null) return "?";
        Throwable c = t;
        if (c.getCause() != null) c = c.getCause();
        String m = c.getMessage();
        return c.getClass().getSimpleName() + (m == null ? "" : (": " + m));
    }

    @Override
    protected void check(SelfCheckResult.Checker c) {
        Class<?> h = findHandlerClass(cl);
        if (h == null) {
            c.fail("TroopClockInHandler", "类未找到");
        } else {
            c.pass("TroopClockInHandler", h.getName());
            Method m = findClockInMethod(h);
            if (m != null) c.pass("clockIn", "找到 " + m.getName());
            else c.fail("clockIn", "没有 (String,String)→void 的方法");
        }
        int total = pendingGroups().size();
        if (total == 0) {
            c.fail("已选群", "还没在控制台里选群");
        } else {
            c.pass("已选群", total + " 个");
            int left = pendingToday().size();
            if (left == 0) c.pass("今日进度", "已全部打完");
            else c.pass("今日进度", "还剩 " + left + " 个未打");
        }
    }

    private static android.content.Context appCtx() {
        try {
            return QQEnv.appContext();
        } catch (Throwable t) {
            return null;
        }
    }

    public static List<String> apiSelectedGroups() {
        return pendingGroupsOf(appCtx());
    }

    public static List<String> apiPendingGroups() {
        return pendingTodayOf(appCtx());
    }

    public static List<String> apiDoneGroups() {
        return new ArrayList<String>(doneTodaySetOf(appCtx()));
    }

    public static boolean apiReady() {
        return sHandlerCls != null && sClockInMethod != null && INSTANCE != null;
    }

    public static boolean apiTriggerNow() {
        final FeatureGroupClockIn it = INSTANCE;
        final Class<?> h = sHandlerCls;
        final Method m = sClockInMethod;
        if (it == null || h == null || m == null) return false;
        final List<String> todo = pendingTodayOf(appCtx());
        if (todo.isEmpty()) return false;
        Thread t = new Thread(new Runnable() {
            @Override public void run() {
                try {
                    it.runOnce(h, m, todo, false);
                } catch (Throwable e) {
                    it.noteRuntimeError(e);
                }
            }
        }, "cat-clockin-manual");
        t.setDaemon(true);
        t.start();
        return true;
    }
}
