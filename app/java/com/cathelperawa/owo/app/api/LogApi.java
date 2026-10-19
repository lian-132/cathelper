package com.cathelperawa.owo.app.api;

import com.cathelperawa.owo.app.core.HookLog;

public final class LogApi {

    private LogApi() {}

    public static void info(String msg) { HookLog.i("[API] " + msg); }

    public static void error(String msg, Throwable t) { HookLog.e("[API] " + msg, t); }

    public static void warn(String msg) { HookLog.w("[API] " + msg); }

    public static void debug(String msg) { HookLog.d("[API] " + msg); }
}
