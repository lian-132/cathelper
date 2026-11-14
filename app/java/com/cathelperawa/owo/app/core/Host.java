package com.cathelperawa.owo.app.core;

public final class Host {

    public static final String PKG_TIM = "com.tencent.tim";
    public static final String PKG_QQ = "com.tencent.mobileqq";

    private static volatile String pkg = PKG_TIM;

    private Host() {}

    public static void set(String packageName) {
        if (packageName != null && packageName.length() > 0) pkg = packageName;
    }

    public static String pkg() {
        return pkg;
    }

    public static boolean isQQ() {
        return PKG_QQ.equals(pkg);
    }

    public static String name() {
        return isQQ() ? "QQ" : "TIM";
    }

    public static boolean isTim() {
        return PKG_TIM.equals(pkg);
    }

    public static boolean isHost(String packageName) {
        return PKG_TIM.equals(packageName) || PKG_QQ.equals(packageName);
    }
}
