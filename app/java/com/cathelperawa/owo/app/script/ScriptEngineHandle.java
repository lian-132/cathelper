package com.cathelperawa.owo.app.script;

import com.cathelperawa.owo.app.core.HookLog;

public final class ScriptEngineHandle {

    private final String owner;
    private final ScriptEngine engine;
    private volatile boolean closed;

    ScriptEngineHandle(String owner, ScriptEngine engine) {
        this.owner = owner;
        this.engine = engine;
    }

    public String lang() {
        try {
            return engine == null ? "" : engine.engineName();
        } catch (Throwable t) {
            return "";
        }
    }

    public boolean alive() {
        return !closed && engine != null;
    }

    public String eval(String code) {
        if (closed || engine == null) return "";

        final Object[] box = new Object[1];
        final Throwable[] err = new Throwable[1];
        Thread t = new Thread(new Runnable() {
            @Override public void run() {
                try {
                    box[0] = engine.eval(code);
                } catch (Throwable e) {
                    err[0] = e;
                }
            }
        }, "subengine-" + owner);
        t.setDaemon(true);

        long t0 = System.currentTimeMillis();
        t.start();
        try {
            t.join(ScriptExec.TIMEOUT_MS);
        } catch (InterruptedException ignore) {
        }

        if (t.isAlive()) {
            t.interrupt();
            HookLog.w("[子引擎] " + owner + " eval 超过 " + ScriptExec.TIMEOUT_MS
                    + "ms，已放弃等待（线程可能还在跑）");
            try {
                ModHealth.record("engine:" + owner,
                        "子引擎 eval 超过 " + ScriptExec.TIMEOUT_MS + "ms 未返回");
            } catch (Throwable ignore) {
            }
            return "";
        }

        long cost = System.currentTimeMillis() - t0;
        if (cost > ScriptExec.TIMEOUT_MS / 2) {
            try {
                ModHealth.record("engine:" + owner, "子引擎 eval 耗时 " + cost + "ms");
            } catch (Throwable ignore) {
            }
        }
        if (err[0] != null) {
            HookLog.w("[子引擎] " + owner + " eval 失败：" + err[0]);
            return "";
        }
        return box[0] == null ? "" : String.valueOf(box[0]);
    }

    public void set(String name, Object value) {
        if (closed || engine == null) return;
        try {
            engine.set(name, value);
        } catch (Throwable t) {
            HookLog.w("[子引擎] " + owner + " set(" + name + ") 失败：" + t);
        }
    }

    public String get(String name) {
        if (closed || engine == null) return "";
        try {
            Object v = engine.get(name);
            return v == null ? "" : String.valueOf(v);
        } catch (Throwable t) {
            return "";
        }
    }

    public String call(String func, String... args) {
        if (closed || engine == null) return "";
        try {
            Object r = engine.callFunction(func, args);
            return r == null ? "" : String.valueOf(r);
        } catch (Throwable t) {
            HookLog.w("[子引擎] " + owner + " call(" + func + ") 失败：" + t);
            return "";
        }
    }

    public void close() {
        if (closed) return;
        closed = true;
        try {
            if (engine != null) engine.close();
        } catch (Throwable ignore) {
        }
    }
}
