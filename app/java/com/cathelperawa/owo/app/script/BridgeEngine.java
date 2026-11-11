package com.cathelperawa.owo.app.script;

import com.cathelperawa.owo.app.core.HookLog;

import java.lang.reflect.Method;

public final class BridgeEngine implements ScriptEngine {

    private static final ThreadLocal<int[]> DEPTH = new ThreadLocal<int[]>();

    private final String provider;
    private final String func;
    private final String suffix;

    BridgeEngine(String provider, String func, String suffix) {
        this.provider = provider;
        this.func = func;
        this.suffix = suffix;
    }

    @Override public String engineName() {
        return "桥接 " + suffix + "（" + provider + "）";
    }

    @Override public Object eval(String code) {
        int[] d = DEPTH.get();
        if (d == null) {
            d = new int[]{0};
            DEPTH.set(d);
        }
        if (d[0] >= 2) {
            HookLog.w("[桥接] " + suffix + " 嵌套过深（≥2），拒绝本次 eval");
            return null;
        }
        d[0]++;
        try {
            return ScriptHost.callScriptFunction(provider, func, new Object[]{code});
        } finally {
            d[0]--;
        }
    }

    @Override public void set(String name, Object value) {
        ScriptHost.callScriptFunction(provider, "__engineSet", new Object[]{name, value});
    }

    @Override public Object get(String name) {
        return ScriptHost.callScriptFunction(provider, "__engineGet", new Object[]{name});
    }

    @Override public void bindMethod(String name, Object target, Method method) {
        
    }

    @Override public void close() {
        
    }

    @Override public Object callFunction(String name, Object[] args) {
        return ScriptHost.callScriptFunction(provider, name, args);
    }
}
