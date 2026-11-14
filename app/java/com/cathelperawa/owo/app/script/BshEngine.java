package com.cathelperawa.owo.app.script;

import java.lang.reflect.Method;

import bsh.Interpreter;

public final class BshEngine implements ScriptEngine {

    private final Interpreter interp;

    public BshEngine() {
        this.interp = new Interpreter();
        DexClassManager.install(interp);
    }

    public Interpreter interpreter() {
        return interp;
    }

    @Override public String engineName() {
        return "BeanShell";
    }

    @Override public Object eval(String code) throws Exception {
        return eval(code, null);
    }

    public Object eval(String code, String sourceName) throws Exception {
        try {

            if (sourceName == null) return interp.eval(code);
            String text = code.endsWith(";") ? code : code + ";";
            return interp.eval(new java.io.StringReader(text), interp.getNameSpace(), sourceName);
        } catch (Throwable t) {

            if (t instanceof bsh.ParseException) {
                bsh.ParseException pe = (bsh.ParseException) t;
                int line = 0;
                try {
                    if (pe.currentToken != null) line = pe.currentToken.beginLine;
                } catch (Throwable ignore) {
                }
                String where = (sourceName == null ? "" : (sourceName + " "));

                throw new Exception("语法错误 " + where
                        + (line > 0 ? ("第 " + line + " 行附近") : "")
                        + "：" + cleanMsg(pe.getMessage()), pe);
            }
            if (t instanceof bsh.EvalError) {
                throw new Exception("运行错误：" + cleanMsg(t.getMessage()), t);
            }
            if (t instanceof Exception) throw (Exception) t;
            throw new Exception(String.valueOf(t), t);
        }
    }

    private static String cleanMsg(String m) {
        if (m == null) return "";
        int nl = m.indexOf('\n');
        if (nl > 0) m = m.substring(0, nl);
        return m.length() > 160 ? m.substring(0, 160) + "…" : m;
    }

    @Override public void set(String name, Object value) {
        try {
            interp.set(name, value);
        } catch (Throwable ignore) {
        }
    }

    @Override public Object get(String name) {
        try {
            return interp.get(name);
        } catch (Throwable t) {
            return null;
        }
    }

    @Override public void bindMethod(String name, Object target, Method method) {

        try {

            StringBuilder sig = new StringBuilder(name).append('#');
            Class<?>[] sigP = method.getParameterTypes();
            for (int i = 0; i < sigP.length; i++) {
                sig.append(sigP[i].getName()).append(',');
            }
            String bridge = "__br_" + Math.abs(sig.toString().hashCode());
            interp.set(bridge, target);
            Class<?>[] ps = method.getParameterTypes();
            StringBuilder args = new StringBuilder();
            for (int i = 0; i < ps.length; i++) {
                if (i > 0) args.append(", ");
                args.append("a").append(i);
            }
            String code = name + "(" + args + ") { return " + bridge + "." + method.getName()
                    + "(" + args + "); }";
            interp.eval(code);
        } catch (Throwable t) {
            com.cathelperawa.owo.app.core.HookLog.w("[引擎] bsh 绑定方法失败 " + name + "：" + t);
        }
    }

    @Override public Object callFunction(String name, Object[] args) {
        try {
            Object[] a = (args == null) ? new Object[0] : args;

            return interp.getNameSpace().invokeMethod(name, a, interp);
        } catch (Throwable t) {
            com.cathelperawa.owo.app.core.HookLog.w("[引擎] bsh 调用函数失败 " + name + "：" + t);
            return null;
        }
    }

    @Override public void close() {

    }
}
