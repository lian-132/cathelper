package com.cathelperawa.owo.app.script;

import java.lang.reflect.Method;

public interface ScriptEngine {

    String engineName();

    Object eval(String code) throws Exception;

    void set(String name, Object value);

    Object get(String name);

    void bindMethod(String name, Object target, Method method);

    void close();

    Object callFunction(String name, Object[] args);
}
