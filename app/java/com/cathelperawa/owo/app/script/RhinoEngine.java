package com.cathelperawa.owo.app.script;

import java.lang.reflect.Method;

import org.mozilla.javascript.ClassShutter;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

public final class RhinoEngine implements ScriptEngine {

    private final ClassLoader appLoader;

    private static final class BridgeLoader extends ClassLoader {
        private final ClassLoader host;

        BridgeLoader(ClassLoader module, ClassLoader host) {
            super(module == null ? BridgeLoader.class.getClassLoader() : module);
            this.host = host;
        }

        @Override protected Class<?> loadClass(String name, boolean resolve)
                throws ClassNotFoundException {
            try {
                return super.loadClass(name, resolve);
            } catch (ClassNotFoundException ignored) {
            }
            if (host != null) return host.loadClass(name);
            throw new ClassNotFoundException(name);
        }
    }
    private Scriptable scope;
    private boolean closed;

    public RhinoEngine(ClassLoader hostLoader) {
        this.appLoader = new BridgeLoader(RhinoEngine.class.getClassLoader(), hostLoader);
        initScope();
    }

    private void initScope() {
        Context cx = Context.enter();
        try {
            cx.setLanguageVersion(Context.VERSION_ES6);
            cx.setOptimizationLevel(-1);          
            if (appLoader != null) cx.setApplicationClassLoader(appLoader);
            cx.setClassShutter(new ClassShutter() {
                @Override public boolean visibleToScripts(String fullClassName) {
                    if (fullClassName == null) return false;

                    
                    if (ScriptTrust.full()) return true;
                    return fullClassName.startsWith("java.")
                            || fullClassName.startsWith("javax.")
                            || fullClassName.startsWith("android.")
                            || fullClassName.startsWith("com.cathelperawa.");
                }
            });
            scope = cx.initStandardObjects();

            try {
                ScriptableObject.putProperty(scope, "__console",
                        Context.javaToJS(new Console(), scope));
                cx.evaluateString(scope,
                        "var console = {"
                                + " log: function(){ __console.log(join(arguments)); },"
                                + " warn: function(){ __console.warn(join(arguments)); },"
                                + " error: function(){ __console.error(join(arguments)); },"
                                + "};"
                                + "function join(a){ var s=''; for (var i=0;i<a.length;i++){"
                                + " if (i>0) s+=' '; s += String(a[i]); } return s; }",
                        "console.js", 1, null);
            } catch (Throwable ignore) {
            }
        } finally {
            Context.exit();
        }
    }

    @Override public String engineName() {
        return "Rhino";
    }

    @Override public Object eval(String code) throws Exception {
        if (closed) throw new Exception("引擎已关闭");
        Context cx = Context.enter();
        try {
            cx.setOptimizationLevel(-1);
            if (appLoader != null) cx.setApplicationClassLoader(appLoader);
            Object r = cx.evaluateString(scope, code, "script.js", 1, null);
            
            if (r == null || r == org.mozilla.javascript.Undefined.instance
                    || r == Scriptable.NOT_FOUND) {
                return null;
            }
            if (r instanceof String || r instanceof Number || r instanceof Boolean) return r;
            return Context.toString(r);
        } finally {
            Context.exit();
        }
    }

    @Override public void set(String name, Object value) {
        if (closed || scope == null || name == null) return;
        Context cx = Context.enter();
        try {
            ScriptableObject.putProperty(scope, name,
                    value == null ? null : Context.javaToJS(value, scope));
        } catch (Throwable t) {
            com.cathelperawa.owo.app.core.HookLog.w("[Rhino] set " + name + " 失败：" + t);
        } finally {
            Context.exit();
        }
    }

    @Override public Object get(String name) {
        if (closed || scope == null || name == null) return null;
        Context cx = Context.enter();
        try {
            Object v = ScriptableObject.getProperty(scope, name);
            return (v == Scriptable.NOT_FOUND) ? null : v;
        } catch (Throwable t) {
            return null;
        } finally {
            Context.exit();
        }
    }

    @Override public void bindMethod(String name, Object target, Method method) {
        if (closed || scope == null) return;
        try {
            MethodBinder binder = (MethodBinder) get("__binder");
            if (binder == null) {
                binder = new MethodBinder();
                set("__binder", binder);
            }
            binder.put(name, target, method);
            eval("function " + name + "(a1, a2) {"
                    + "  return __binder.invoke('" + name + "', a1, a2);"
                    + "}");
        } catch (Throwable t) {
            com.cathelperawa.owo.app.core.HookLog.w("[Rhino] 绑定方法失败 " + name + "：" + t);
        }
    }

    @Override public Object callFunction(String name, Object[] args) {
        if (closed || scope == null || name == null) return null;
        Context cx = Context.enter();
        try {
            Object fn = ScriptableObject.getProperty(scope, name);
            if (!(fn instanceof Function)) return null;
            Scriptable thisObj = scope;
            Object[] a = args == null ? new Object[0] : args;
            Object r = ((Function) fn).call(cx, scope, thisObj, a);
            if (r == null || r == org.mozilla.javascript.Undefined.instance) return null;
            return r;
        } catch (Throwable t) {
            com.cathelperawa.owo.app.core.HookLog.w("[Rhino] 调用函数失败 " + name + "：" + t);
            return null;
        } finally {
            Context.exit();
        }
    }

    @Override public void close() {
        closed = true;
        scope = null;
    }

    public static final class Console {
        public void log(String s) {
            com.cathelperawa.owo.app.core.HookLog.i("[JS] " + s);
        }

        public void warn(String s) {
            com.cathelperawa.owo.app.core.HookLog.w("[JS] " + s);
        }

        public void error(String s) {
            com.cathelperawa.owo.app.core.HookLog.e("[JS] " + s, null);
        }
    }

    public static final class MethodBinder {
        private final java.util.Map<String, Object[]> map =
                new java.util.HashMap<String, Object[]>();

        void put(String name, Object target, Method m) {
            map.put(name, new Object[]{target, m});
        }

        public Object invoke(String name, Object a1, Object a2) throws Exception {
            Object[] e = map.get(name);
            if (e == null) throw new Exception("没有绑定的方法：" + name);
            Method m = (Method) e[1];
            Object target = e[0];
            Class<?>[] ps = m.getParameterTypes();
            Object[] args = new Object[ps.length];
            Object[] in = new Object[]{a1, a2};
            for (int i = 0; i < ps.length; i++) {
                Object v = i < in.length ? in[i] : null;
                if (v == null || v == org.mozilla.javascript.Undefined.instance) {
                    args[i] = null;
                } else if (ps[i] == String.class) {
                    args[i] = (v instanceof String) ? v : String.valueOf(v);
                } else {
                    args[i] = org.mozilla.javascript.Context.jsToJava(v, ps[i]);
                }
            }
            Object r = m.invoke(target, args);
            return r instanceof Function ? null : r;
        }
    }
}
