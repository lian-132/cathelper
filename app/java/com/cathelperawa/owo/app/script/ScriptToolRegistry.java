package com.cathelperawa.owo.app.script;

import com.cathelperawa.owo.app.core.HookLog;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

public final class ScriptToolRegistry {

    static final long TIMEOUT_MS = 3000;

    public static final class Tool {
        public final String script;
        public final String name;
        public final String description;
        final ScriptToolHandler handler;

        final AtomicLong calls = new AtomicLong();
        final AtomicLong errors = new AtomicLong();

        Tool(String script, String name, String description, ScriptToolHandler handler) {
            this.script = script;
            this.name = name;
            this.description = description;
            this.handler = handler;
        }

        public long calls() { return calls.get(); }
        public long errors() { return errors.get(); }
    }

    private static final List<Tool> TOOLS = new CopyOnWriteArrayList<Tool>();

    private ScriptToolRegistry() {}

    public static String add(String script, String name, String desc, ScriptToolHandler h) {
        if (name == null || !name.matches("[a-zA-Z0-9_-]{1,40}")) {
            return "工具名只能用英文/数字/下划线，1~40 字符：" + name;
        }
        if (h == null) return "工具没给处理函数";
        Tool t = new Tool(script, name, desc == null ? "" : desc, h);
        for (Tool o : TOOLS) {
            if (o.name.equals(name)) {
                if (o.script.equals(script)) {

                    TOOLS.remove(o);
                    break;
                }
                return "工具名「" + name + "」已被脚本「" + o.script + "」占用";
            }
        }
        TOOLS.add(t);
        HookLog.i("[脚本工具] " + script + " 注册「" + name + "」");
        return null;
    }

    public static int removeByScript(String script) {
        if (script == null) return 0;
        List<Tool> rm = new ArrayList<Tool>();
        for (Tool t : TOOLS) {
            if (t.script.equals(script)) rm.add(t);
        }
        int n = 0;
        for (Tool t : rm) {
            if (TOOLS.remove(t)) n++;
        }
        return n;
    }

    public static void clear() { TOOLS.clear(); }

    public static List<Tool> all() { return new ArrayList<Tool>(TOOLS); }

    public static int size() { return TOOLS.size(); }

    public static int countOf(String script) {
        int n = 0;
        for (Tool t : TOOLS) if (t.script.equals(script)) n++;
        return n;
    }

    public static Tool find(String name) {
        if (name == null) return null;
        for (Tool t : TOOLS) {
            if (t.name.equals(name)) return t;
        }
        return null;
    }

    public static JSONArray toolsJson() {
        JSONArray out = new JSONArray();
        for (Tool t : TOOLS) {
            try {
                JSONObject item = new JSONObject();
                item.put("type", "function");
                JSONObject fn = new JSONObject();
                fn.put("name", t.name);
                fn.put("description", t.description
                        + "\n（这个工具由脚本「" + t.script + "」提供）");
                JSONObject params = new JSONObject();
                params.put("type", "object");
                params.put("properties", new JSONObject());
                params.put("additionalProperties", true);
                fn.put("parameters", params);
                item.put("function", fn);
                out.put(item);
            } catch (Throwable ignore) {
            }
        }
        return out;
    }

    public static String invoke(String name, String args) {
        Tool t = find(name);
        if (t == null) return null;

        t.calls.incrementAndGet();
        final String a = (args == null || args.trim().length() == 0) ? "{}" : args;

        final ScriptToolResult out = new ScriptToolResult();
        final Throwable[] err = new Throwable[1];

        Thread th = new Thread(new Runnable() {
            @Override public void run() {
                try {
                    handler(t, a, out);
                } catch (Throwable e) {
                    err[0] = e;
                }
            }
        }, "cat-script-tool");
        th.setDaemon(true);
        th.start();
        try {
            th.join(TIMEOUT_MS);
        } catch (InterruptedException ignore) {
            Thread.currentThread().interrupt();
        }
        if (th.isAlive()) {

            th.interrupt();
            t.errors.incrementAndGet();
            HookLog.w("[脚本工具] " + name + " 超时 " + TIMEOUT_MS + "ms");
            return "（脚本工具 " + name + " 超时了，别再重试，直接告诉用户没查到）";
        }
        if (err[0] != null) {
            t.errors.incrementAndGet();
            HookLog.e("[脚本工具] " + name + " 出错", err[0]);
            return "（脚本工具 " + name + " 出错：" + err[0] + "）";
        }
        String r = out.get();
        return (r == null || r.length() == 0) ? "（脚本没返回内容）" : r;
    }

    private static void handler(final Tool t, final String args, final ScriptToolResult out) {
        boolean ok = ScriptExec.call(t.script, new Runnable() {
            @Override public void run() {
                t.handler.call(args, out);
            }
        }, ScriptExec.TIMEOUT_MS);
        if (!ok) out.set("（脚本 " + t.name + " 超时或已被判定卡死）");
    }
}
