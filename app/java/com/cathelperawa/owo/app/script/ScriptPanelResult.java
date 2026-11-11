package com.cathelperawa.owo.app.script;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ScriptPanelResult {

    private final Map<String, String> updates = new LinkedHashMap<String, String>();
    private String toastText;

    public ScriptPanelResult() {}

    public void set(String id, String value) {
        if (id != null) updates.put(id, value == null ? "" : value);
    }

    public void toast(String s) {
        this.toastText = s;
    }

    Map<String, String> updates() { return updates; }

    String toastText() { return toastText; }
}
