package com.cathelperawa.owo.app.core;

public final class UIScope {

    public static final String TIM = "tim";
    public static final String QQ = "qq";

    private static volatile String current = TIM;

    private UIScope() {}

    public static void set(String scope) {
        if (scope != null && scope.length() > 0) current = scope;
    }

    public static String current() {
        return current;
    }

    public static boolean isQQ() {
        return QQ.equals(current);
    }
}
