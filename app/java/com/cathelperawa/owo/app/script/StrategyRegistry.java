package com.cathelperawa.owo.app.script;

import com.cathelperawa.owo.app.core.HookLog;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class StrategyRegistry {

    public static final int BUDGET_MS = 30;

    public static final int FAIL_LIMIT = 3;

    public interface Handler {
        String pick(String input, String def);
    }

    private static final class Entry {
        final String script;
        final String name;
        final Handler handler;
        int calls;
        int fails;
        long totalMs;
        boolean disabled;
        String lastError = "";

        Entry(String script, String name, Handler h) {
            this.script = script;
            this.name = name;
            this.handler = h;
        }
    }

    private static final Map<String, Entry> MAP = new LinkedHashMap<String, Entry>();

    private StrategyRegistry() {}

    public static void register(String script, String name, Handler h) {
        if (name == null || h == null) return;
        synchronized (MAP) {
            MAP.put(name, new Entry(script, name, h));
        }
        HookLog.i("[策略] " + script + " 接管 " + name);
    }

    public static int removeByScript(String script) {
        if (script == null) return 0;
        synchronized (MAP) {
            List<String> del = new ArrayList<String>();
            for (Map.Entry<String, Entry> e : MAP.entrySet()) {
                if (script.equals(e.getValue().script)) del.add(e.getKey());
            }
            for (int i = 0; i < del.size(); i++) MAP.remove(del.get(i));
            return del.size();
        }
    }

    public static boolean has(String name) {
        synchronized (MAP) {
            Entry e = MAP.get(name);
            return e != null && !e.disabled;
        }
    }

    public static String pick(String name, String input, String def) {
        Entry e;
        synchronized (MAP) {
            e = MAP.get(name);
        }
        if (e == null || e.disabled) return def;

        long t0 = System.currentTimeMillis();
        try {

            final String[] box = new String[]{null};
            final String fin = (input == null) ? "" : input;
            ScriptExec.call(e.script, new Runnable() {
                @Override public void run() {
                    box[0] = e.handler.pick(fin, def);
                }
            }, BUDGET_MS * 2L);
            String out = box[0];
            long cost = System.currentTimeMillis() - t0;
            e.calls++;
            e.totalMs += cost;
            if (cost > BUDGET_MS) {
                e.fails++;
                e.lastError = "超时 " + cost + "ms";
                HookLog.w("[策略] " + name + " 超预算 " + cost + "ms，本次用官方值");
                checkDisable(e);
            }
            return out == null ? def : out;
        } catch (Throwable t) {
            e.fails++;
            e.lastError = String.valueOf(t);
            HookLog.e("[策略] " + name + " 出错，本次用官方值", t);
            checkDisable(e);
            return def;
        }
    }

    private static void checkDisable(Entry e) {
        if (e.fails >= FAIL_LIMIT && !e.disabled) {
            e.disabled = true;
            HookLog.w("[策略] " + e.name + " 连续失败 " + e.fails + " 次，已自动回退官方实现");
            try {
                ModHealth.record(e.script, "策略 " + e.name + " 连续失败，已熔断");
            } catch (Throwable ignore) {
            }
        }
    }

    public static List<String> status() {
        List<String> out = new ArrayList<String>();
        synchronized (MAP) {
            for (Map.Entry<String, Entry> me : MAP.entrySet()) {
                Entry e = me.getValue();
                long avg = e.calls == 0 ? 0 : e.totalMs / e.calls;
                out.add(e.name + " ← " + e.script
                        + "（调用 " + e.calls + " · 失败 " + e.fails
                        + " · 平均 " + avg + "ms"
                        + (e.disabled ? " · 已熔断" : "") + "）");
            }
        }
        return out;
    }

    public static int count() {
        synchronized (MAP) {
            return MAP.size();
        }
    }
}
