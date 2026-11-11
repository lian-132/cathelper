package com.cathelperawa.owo.app.script;

public final class ScriptToolResult {

    private volatile String value;

    public ScriptToolResult() {}

    public void set(String s) {
        this.value = s;
    }

    public void append(String s) {
        this.value = (this.value == null ? "" : this.value) + (s == null ? "" : s);
    }

    public String get() {
        return value;
    }

    public boolean isEmpty() {
        return value == null || value.length() == 0;
    }
}
