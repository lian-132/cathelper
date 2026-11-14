package com.cathelperawa.owo.app.script;

import java.util.ArrayList;
import java.util.List;

final class MockRoutes {

    private static final List<String> LAST = new ArrayList<String>();

    private MockRoutes() {}

    static synchronized void add(String script, String trigger) {
        LAST.add(script + " → " + trigger);
    }

    static synchronized List<String> take() {
        List<String> out = new ArrayList<String>(LAST);
        LAST.clear();
        return out;
    }

    static synchronized void clear() {
        LAST.clear();
    }
}
