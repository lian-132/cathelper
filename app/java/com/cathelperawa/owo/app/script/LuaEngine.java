package com.cathelperawa.owo.app.script;

import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.VarArgFunction;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;
import org.luaj.vm2.lib.jse.JsePlatform;

import java.lang.reflect.Method;

public final class LuaEngine implements ScriptEngine {

    private final Globals g;
    private volatile boolean closed;

    private volatile long deadline;

    public LuaEngine() {
        Globals globals;
        try {
            globals = JsePlatform.standardGlobals();
        } catch (Throwable t) {
            globals = null;
        }
        this.g = globals;
        if (globals != null) installHook();

        if (globals != null) {
            try {
                globals.set("__handlerImpl", CoerceJavaToLua.coerce(new LuaProxy.Helper(globals)));

                globals.load("function __handler(iface, tbl) "
                        + "return __handlerImpl:handler(iface, tbl) end", "bootstrap").call();
            } catch (Throwable ignore) {
            }
        }
    }

    public final class Guard {
        public void tick() {
            if (System.currentTimeMillis() > deadline) {
                throw new LuaError("超过时间预算（" + ScriptExec.TIMEOUT_MS + "ms），已中断");
            }
        }
    }

    private void installHook() {
        if (g == null) return;
        try {
            g.set("__guard", CoerceJavaToLua.coerce(new Guard()));
            g.load("if debug and debug.sethook then "
                    + "debug.sethook(function() __guard:tick() end, 'count', 200) end",
                    "hook").call();
        } catch (Throwable t) {
            com.cathelperawa.owo.app.core.HookLog.w("[Lua] 装超时钩子失败：" + t);
        }
    }

    @Override public String engineName() {
        return "Lua";
    }

    @Override public Object eval(String code) throws Exception {
        if (closed || g == null) return null;
        deadline = System.currentTimeMillis() + ScriptExec.TIMEOUT_MS;
        installHook();
        LuaValue chunk = g.load(code == null ? "" : code, "script");
        LuaValue r = chunk.call();
        if (r == null || r.isnil()) return null;
        return r.tojstring();
    }

    @Override public void set(String name, Object value) {
        if (closed || g == null) return;
        try {
            g.set(name, CoerceJavaToLua.coerce(value));
        } catch (Throwable ignore) {
        }
    }

    @Override public Object get(String name) {
        if (closed || g == null) return null;
        try {
            LuaValue v = g.get(name);
            return (v == null || v.isnil()) ? null : v.tojstring();
        } catch (Throwable t) {
            return null;
        }
    }

    @Override public void bindMethod(String name, Object target, Method method) {

    }

    @Override public void close() {
        closed = true;
    }

    @Override public Object callFunction(String name, Object[] args) {
        if (closed || g == null) return null;
        try {
            LuaValue f = g.get(name);
            if (f == null || f.isnil()) return null;
            int n = args == null ? 0 : args.length;
            LuaValue[] a = new LuaValue[n];
            for (int i = 0; i < n; i++) a[i] = CoerceJavaToLua.coerce(args[i]);
            LuaValue r = f.invoke(a).arg1();
            return (r == null || r.isnil()) ? null : r.tojstring();
        } catch (Throwable t) {
            com.cathelperawa.owo.app.core.HookLog.e("[Lua] callFunction(" + name + ") 出错", t);
            return null;
        }
    }
}
