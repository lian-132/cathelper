package com.cathelperawa.owo.app.script;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.Map;

public final class JavaEngine implements ScriptEngine {

    private final Map<String, Object> vars = new LinkedHashMap<String, Object>();
    private Class<?> master;
    private ClassLoader loader;
    private ScriptApi api;

    @Override
    public String engineName() {
        return "Java 编译";
    }

    @Override
    public void set(String name, Object value) {
        vars.put(name, value);
        if ("ctx".equals(name) && value instanceof ScriptApi) api = (ScriptApi) value;
    }

    @Override
    public Object get(String name) {
        if (master != null) {
            try {
                Field f = master.getField(name);
                f.setAccessible(true);
                return f.get(null);
            } catch (Throwable ignore) {
            }
        }
        return vars.get(name);
    }

    @Override
    public void bindMethod(String name, Object target, Method method) {

        vars.put("__bind." + name, method);
    }

    @Override
    public Object eval(String code) throws Exception {
        android.content.Context ctx = pickContext();
        if (ctx == null) throw new IllegalStateException("编译模式拿不到 Context");

        String dir = str(vars.get("scriptDir"));
        if (dir.length() == 0) throw new IllegalStateException("编译模式缺少 scriptDir");

        ScriptJavaWrapper.Wrapped w = ScriptJavaWrapper.wrap(code, null);
        JavaToolchain.Result r = JavaToolchain.compile(w.source);
        if (!r.ok()) {
            throw new IllegalStateException("编译失败（" + r.millis + "ms）："
                    + JavaToolchain.humanize(r.error, 44));
        }
        master = r.master;
        com.cathelperawa.owo.app.core.HookLog.i("[java] " + dir + " 编译完成 "
                + r.millis + "ms");

        Object apiObj = (api != null) ? (Object) api : vars.get("ctx");
        ApiBridge.push(apiObj);
        inject("ctx", apiObj);
        inject("context", vars.get("context"));
        inject("classLoader", vars.get("classLoader"));
        inject("pluginPath", vars.get("pluginPath"));
        inject("scriptDir", vars.get("scriptDir"));
        inject("pluginID", vars.get("pluginID"));
        inject("myUin", vars.get("myUin"));


        try {
            Class.forName("scripts.Master", true, master.getClassLoader());
        } catch (Throwable t) {
            Throwable root = t;
            while (root.getCause() != null && root.getCause() != root) root = root.getCause();
            throw new IllegalStateException("顶层语句执行失败：" + root, root);
        } finally {
            ApiBridge.pop();
        }

        Object ret = callIfExists("onLoad", new Object[0]);
        if (ret == null) callIfExists("main", new Object[0]);
        return ret;
    }

    private android.content.Context pickContext() {
        Object c = vars.get("context");
        if (c instanceof android.content.Context) return (android.content.Context) c;
        Object a = vars.get("ctx");
        if (a instanceof ScriptApi) {
            try {
                Object v = ((ScriptApi) a).context();
                if (v instanceof android.content.Context) return (android.content.Context) v;
            } catch (Throwable ignore) {
            }
        }
        return null;
    }

    private void inject(String name, Object value) {
        if (master == null) { diag("inject " + name + " 跳过：master==null"); return; }
        if (value == null) { diag("inject " + name + " 跳过：value==null"); return; }
        try {
            Field f = master.getField(name);
            f.setAccessible(true);
            if (f.getType().isInstance(value)) {
                f.set(null, value);
            } else {
                diag("inject " + name + " 类型不符 field=" + f.getType().getName()
                        + " loader=" + f.getType().getClassLoader()
                        + " | value=" + value.getClass().getName()
                        + " loader=" + value.getClass().getClassLoader());
            }
        } catch (Throwable t) {
            diag("inject " + name + " 异常：" + t);
        }
    }

    private void diag(String msg) {
        try {
            Object d = vars.get("scriptDir");
            if (d == null) return;
            java.io.File f = new java.io.File(String.valueOf(d), ".jbuild_diag.txt");
            java.io.FileWriter w = new java.io.FileWriter(f, true);
            w.write(msg + "\n");
            w.close();
        } catch (Throwable ignore) {
        }
    }

    private Object callIfExists(String name, Object[] args) {
        try {
            Method m = master.getMethod(name);
            m.setAccessible(true);
            return m.invoke(null);
        } catch (Throwable t) {
            return null;
        }
    }

    @Override
    public Object callFunction(String name, Object[] args) {
        if (master == null) return null;
        try {
            for (Method m : master.getMethods()) {
                if (!m.getName().equals(name)) continue;
                if (m.getParameterTypes().length != (args == null ? 0 : args.length)) continue;
                m.setAccessible(true);
                return m.invoke(null, args);
            }
        } catch (Throwable t) {
            com.cathelperawa.owo.app.core.HookLog.w("[javac] 调用 " + name + " 失败：" + t);
        }
        return null;
    }

    @Override
    public void close() {
        master = null;
        loader = null;
    }

    private static String str(Object o) {
        return o == null ? "" : String.valueOf(o);
    }

}
