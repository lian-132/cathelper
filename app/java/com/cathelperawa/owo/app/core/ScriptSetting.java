package com.cathelperawa.owo.app.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ScriptSetting {

    public static final class Entry {
        public final String script;
        public final String title;
        public final String page;
        public final String icon;

        Entry(String script, String title, String page, String icon) {
            this.script = script;
            this.title = title;
            this.page = page;
            this.icon = icon;
        }
    }

    private static final Map<String, Entry> MAP = new LinkedHashMap<String, Entry>();

    private static int sVersion = 0;

    private ScriptSetting() {}

    public static void register(String script, String title, String page, String icon) {
        if (script == null || title == null || title.length() == 0) return;
        String key = page == null || page.length() == 0 ? "mod_" + script : page;
        synchronized (MAP) {
            MAP.put(title, new Entry(script, title, key, icon == null ? "" : icon));
            sVersion++;
            HookLog.i("[设置入口] 脚本[" + script + "] 新增「" + title + "」→ " + key);
        }
    }

    public static int removeByScript(String script) {
        if (script == null) return 0;
        synchronized (MAP) {
            List<String> del = new ArrayList<String>();
            for (Map.Entry<String, Entry> e : MAP.entrySet()) {
                if (script.equals(e.getValue().script)) del.add(e.getKey());
            }
            for (int i = 0; i < del.size(); i++) MAP.remove(del.get(i));
            if (del.size() > 0) sVersion++;
            return del.size();
        }
    }

    public static List<Entry> list() {
        synchronized (MAP) {
            return new ArrayList<Entry>(MAP.values());
        }
    }

    public static int version() { return sVersion; }

    public static boolean any() {
        synchronized (MAP) {
            return !MAP.isEmpty();
        }
    }

    public static int clearAll() {
        synchronized (MAP) {
            int n = MAP.size();
            MAP.clear();
            sVersion++;
            return n;
        }
    }
}
