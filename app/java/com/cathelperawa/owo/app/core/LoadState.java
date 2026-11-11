package com.cathelperawa.owo.app.core;

public final class LoadState {

    public enum Stage {
        NONE("未开始"),
        ENTER("已进入模块"),
        CONTEXT("已拿到 Context"),
        HOOKS("功能已加载"),
        READY("就绪"),
        SAFE_MODE("安全模式");

        public final String label;

        Stage(String label) { this.label = label; }
    }

    private static volatile Stage sStage = Stage.NONE;
    private static volatile long sAt = 0;
    private static volatile boolean sSafeMode = false;
    
    private static volatile int sPending = 0;

    private LoadState() {}

    public static Stage stage() { return sStage; }

    public static String label() { return sStage.label; }

    public static synchronized void enter(Stage s) {
        if (s == null) return;
        if (sSafeMode && s != Stage.SAFE_MODE) return;   
        if (s.ordinal() <= sStage.ordinal() && s != Stage.SAFE_MODE) return;
        sStage = s;
        sAt = System.currentTimeMillis();
        HookLog.i("加载阶段 → " + s.label);
    }

    public static void safeMode() {
        sSafeMode = true;
        sStage = Stage.SAFE_MODE;
        sAt = System.currentTimeMillis();
    }

    public static boolean inSafeMode() { return sSafeMode; }

    public static long ageSec() {
        return sAt == 0 ? 0 : (System.currentTimeMillis() - sAt) / 1000;
    }

    public static void expect(int n) {
        if (n <= 0) return;
        synchronized (LoadState.class) {
            sPending += n;
        }
    }

    public static void completeOne() {
        boolean ready = false;
        synchronized (LoadState.class) {
            if (sPending > 0) sPending--;
            ready = sPending == 0;
        }
        if (ready) enter(Stage.READY);
    }

    public static int pending() { return sPending; }

    public static boolean isReady() {
        return sStage == Stage.READY && sPending == 0;
    }

    public static String summary() {
        int hooks = 0;
        try {
            hooks = HookItemFactory.all().size();
        } catch (Throwable ignore) {
        }
        return "stage=" + sStage.name() + "(" + sStage.label + ")"
                + " hooks=" + hooks
                + " pending=" + sPending
                + " age=" + ageSec() + "s";
    }
}
