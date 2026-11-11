package com.cathelperawa.owo.app.script;

import com.cathelperawa.owo.app.core.HookLog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PatchPoint {

    public static final int BUDGET_MS = 50;

    public interface Handler {
        void on(PatchCtx c);
    }

    public static final class PatchCtx {
        public final String point;
        private String text;
        
        private String extra;
        private boolean cancel;
        private boolean timedOut;
        
        private String elements = "";

        private java.util.List<String> dropSink;

        public PatchCtx(String point, String text) {
            this.point = point;
            this.text = text == null ? "" : text;
        }

        public PatchCtx(String point, String text, String extra) {
            this.point = point;
            this.text = text == null ? "" : text;
            this.extra = extra == null ? "" : extra;
        }

        public String extra() { return extra == null ? "" : extra; }

        public String text() { return text; }

        public void text(String t) { this.text = t == null ? "" : t; }

        public String elements() { return elements == null ? "" : elements; }

        public void elements(String e) { this.elements = e == null ? "" : e; }

        public void setDropSink(java.util.List<String> sink) { this.dropSink = sink; }

        public void dropElement(String kind) {
            if (dropSink == null || kind == null || kind.length() == 0) return;
            if (!dropSink.contains(kind)) dropSink.add(kind);
        }

        public void cancel() { this.cancel = true; }

        public boolean cancelled() { return cancel; }

        public boolean timedOut() { return timedOut; }
    }

    private static final class Entry {
        final String script;
        final Handler handler;

        Entry(String script, Handler handler) {
            this.script = script;
            this.handler = handler;
        }
    }

    private static final java.util.Set<String> SUPPORTED =
            new java.util.HashSet<String>(java.util.Arrays.asList(
                    "host.msg.recv",     
                    "host.send.text",    
                    "host.typing.text",  
                    "host.send.raw",     
                    "host.ai.reply",     
                    "host.ai.input",     
                    "host.script.load",  
                    "host.script.stop",  
                    "host.panel.open",   
                    "host.pat"           
            ));

    public static void emitAsync(String point, String text, String extra) {
        List<Entry> list;
        synchronized (MAP) {
            list = MAP.get(point);
            if (list == null || list.isEmpty()) return;
            list = new ArrayList<Entry>(list);
        }
        for (int i = 0; i < list.size(); i++) {
            final Entry e = list.get(i);
            final PatchCtx c = new PatchCtx(point, text, extra);
            ScriptExec.run(e.script, new Runnable() {
                @Override public void run() {
                    try {
                        e.handler.on(c);
                    } catch (Throwable t) {
                        ModHealth.record(e.script, "点位 " + c.point + "：" + t);
                        HookLog.e("[点位] " + e.script + " 在 " + c.point + " 出错", t);
                    }
                }
            });
        }
    }

    public static int countOf(String script) {
        if (script == null) return 0;
        int n = 0;
        synchronized (MAP) {
            for (java.util.Map.Entry<String, List<Entry>> e : MAP.entrySet()) {
                List<Entry> list = e.getValue();
                for (int i = 0; i < list.size(); i++) {
                    if (script.equals(list.get(i).script)) n++;
                }
            }
        }
        return n;
    }

    public static String supportedList() {
        StringBuilder sb = new StringBuilder();
        for (String p : SUPPORTED) {
            if (sb.length() > 0) sb.append(" / ");
            sb.append(p);
        }
        return sb.toString();
    }

    public static java.util.List<String> unsupported() {
        synchronized (UNSUPPORTED) {
            return new ArrayList<String>(UNSUPPORTED);
        }
    }

    public static boolean isSupported(String point) {
        return point != null && SUPPORTED.contains(point);
    }

    private static final java.util.Set<String> UNSUPPORTED = new java.util.HashSet<String>();

    private static final Map<String, List<Entry>> MAP = new LinkedHashMap<String, List<Entry>>();

    private PatchPoint() {}

    public static void register(String script, String point, Handler h) {
        if (point == null || h == null) return;
        if (!SUPPORTED.contains(point)) {

            HookLog.w("[点位] 宿主不支持「" + point + "」，已忽略。当前支持："
                    + supportedList());
            synchronized (UNSUPPORTED) {
                UNSUPPORTED.add(point);
            }
            return;
        }
        synchronized (MAP) {
            List<Entry> list = MAP.get(point);
            if (list == null) {
                list = new ArrayList<Entry>();
                MAP.put(point, list);
            }
            list.add(new Entry(script, h));
            HookLog.i("[点位] 脚本[" + script + "] 挂上 " + point);
        }
    }

    public static int removeByScript(String script) {
        if (script == null) return 0;
        int n = 0;
        synchronized (MAP) {
            List<String> empty = new ArrayList<String>();
            for (Map.Entry<String, List<Entry>> e : MAP.entrySet()) {
                List<Entry> list = e.getValue();
                for (int i = list.size() - 1; i >= 0; i--) {
                    if (script.equals(list.get(i).script)) {
                        list.remove(i);
                        n++;
                    }
                }
                if (list.isEmpty()) empty.add(e.getKey());
            }
            for (int i = 0; i < empty.size(); i++) MAP.remove(empty.get(i));
        }
        return n;
    }

    public static boolean has(String point) {
        if (point == null) return false;
        synchronized (MAP) {
            List<Entry> list = MAP.get(point);
            return list != null && !list.isEmpty();
        }
    }

    public static List<String> points() {
        synchronized (MAP) {
            return new ArrayList<String>(MAP.keySet());
        }
    }

    public static int count() {
        synchronized (MAP) {
            int n = 0;
            for (List<Entry> l : MAP.values()) n += l.size();
            return n;
        }
    }

    public static void emit(PatchCtx c) {
        if (c == null) return;
        List<Entry> list;
        synchronized (MAP) {
            list = MAP.get(c.point);
            if (list == null || list.isEmpty()) return;
            list = new ArrayList<Entry>(list);   
        }
        long deadline = System.currentTimeMillis() + BUDGET_MS;
        for (int i = 0; i < list.size(); i++) {
            if (System.currentTimeMillis() > deadline) {
                c.timedOut = true;
                HookLog.w("[点位] " + c.point + " 超出预算 " + BUDGET_MS + "ms，跳过剩余 "
                        + (list.size() - i) + " 个 handler");
                break;
            }
            Entry e = list.get(i);
            try {
                e.handler.on(c);
            } catch (Throwable t) {
                
                ModHealth.record(e.script, "点位 " + c.point + "：" + t);
                HookLog.e("[点位] " + e.script + " 在 " + c.point + " 出错", t);
            }
        }
    }

    public static int clearAll() {
        synchronized (MAP) {
            int n = 0;
            for (List<Entry> l : MAP.values()) n += l.size();
            MAP.clear();
            return n;
        }
    }
}
