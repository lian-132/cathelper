package com.cathelperawa.owo.app.script;

import com.cathelperawa.owo.app.core.HookLog;

import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public final class LuaProxy {

    private LuaProxy() {}

    public static Object make(Globals g, String ifaceName, LuaValue table) {
        if (ifaceName == null || table == null || !table.istable()) return null;
        try {
            Class<?> iface = loadIface(ifaceName);
            if (!iface.isInterface()) return null;
            ClassLoader cl = iface.getClassLoader();
            if (cl == null) cl = LuaProxy.class.getClassLoader();
            return Proxy.newProxyInstance(cl, new Class[]{iface}, new H(g, table));
        } catch (Throwable t) {
            return null;
        }
    }

    private static Class<?> loadIface(String name) throws ClassNotFoundException {
        try {
            return Class.forName(name, false, LuaProxy.class.getClassLoader());
        } catch (Throwable t) {
            return Class.forName(name, false, ScriptEngines.hostClassLoader());
        }
    }

    private static final class H implements InvocationHandler {
        private final Globals g;
        private final LuaValue table;

        H(Globals g, LuaValue table) {
            this.g = g;
            this.table = table;
        }

        @Override public Object invoke(Object proxy, Method m, Object[] args) {
            try {

                if ("toString".equals(m.getName())) return "LuaProxy(" + table + ")";
                if ("hashCode".equals(m.getName())) return System.identityHashCode(proxy);
                if ("equals".equals(m.getName())) return proxy == (args == null ? null : args[0]);

                LuaValue f = table.get(m.getName());
                if (f.isnil()) return null;
                int n = m.getParameterTypes().length;
                LuaValue[] a = new LuaValue[n];
                for (int i = 0; i < n; i++) {
                    Object v = (args == null || i >= args.length) ? null : args[i];
                    a[i] = CoerceJavaToLua.coerce(v);
                }
                f.invoke(a);
                return null;
            } catch (Throwable t) {

                HookLog.e("[LuaProxy] 回调 " + m.getName() + " 出错", t);
                return null;
            }
        }
    }

    public static final class Helper {
        private final Globals g;

        Helper(Globals g) {
            this.g = g;
        }

        public Object handler(String ifaceName, LuaValue table) {
            return make(g, ifaceName, table);
        }
    }
}
