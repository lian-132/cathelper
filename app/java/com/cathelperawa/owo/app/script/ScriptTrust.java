package com.cathelperawa.owo.app.script;

public final class ScriptTrust {

    private static final String KEY = "script_full_trust";

    private ScriptTrust() {}

    public static boolean full() {
        try {
            return com.cathelperawa.owo.app.api.ConfigApi.getBool(KEY, false);
        } catch (Throwable t) {
            return false;
        }
    }

    private static final String KEY_KERNEL = "script_kernel_trust";

    public static boolean kernelTrust() {
        try {
            return com.cathelperawa.owo.app.api.ConfigApi.getBool(KEY_KERNEL, false);
        } catch (Throwable t) {
            return false;
        }
    }

    public static void setKernelTrust(boolean on) {
        try {
            com.cathelperawa.owo.app.api.ConfigApi.setFeatureEnabled(KEY_KERNEL, on);
        } catch (Throwable ignore) {
        }
    }

    public static int tier() {
        if (!full()) return 0;
        return kernelTrust() ? 2 : 1;
    }

    public static void setFull(boolean on) {
        try {
            com.cathelperawa.owo.app.api.ConfigApi.setFeatureEnabled(KEY, on);
        } catch (Throwable ignore) {
        }
    }
}
