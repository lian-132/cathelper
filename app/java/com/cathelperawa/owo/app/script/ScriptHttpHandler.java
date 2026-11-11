package com.cathelperawa.owo.app.script;

public interface ScriptHttpHandler {

    void onResponse(int code, String body, String error);
}
