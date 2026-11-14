package com.cathelperawa.owo.app.script;

import com.cathelperawa.owo.app.core.HookLog;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public final class ScriptBusRegistry {

    static final long CALL_TIMEOUT_MS = 3000;

    public static final class Service {
        public final String name;
        public final String script;
        final ScriptServiceHandler handler;

        Service(String name, String script, ScriptServiceHandler h) {
            this.name = name;
            this.script = script;
            this.handler = h;
        }
    }

    private static final ConcurrentHashMap<String, Service> SERVICES =
            new ConcurrentHashMap<String, Service>();

    public static final class Listener {
        public final String event;
        public final String script;
        final ScriptBusHandler handler;

        Listener(String event, String script, ScriptBusHandler h) {
            this.event = event;
            this.script = script;
            this.handler = h;
        }
    }

    private static final List<Listener> LISTENERS = new CopyOnWriteArrayList<Listener>();

    private ScriptBusRegistry() {}

    public static String provide(String script, String name, ScriptServiceHandler h) {
        if (name == null || name.length() == 0 || name.length() > 32) {
            return "服务名 1~32 字符";
        }
        if (name.indexOf('/') >= 0 || name.indexOf((char) 92) >= 0 || name.indexOf("..") >= 0) {
            return "服务名不能包含路径字符";
        }
        if (h == null) return "没给服务实现";
        Service old = SERVICES.get(name);
        if (old != null && !old.script.equals(script)) {
            return "服务名「" + name + "」已被脚本「" + old.script + "」占用";
        }
        SERVICES.put(name, new Service(name, script, h));
        HookLog.i("[脚本服务] " + script + " 提供「" + name + "」");
        return null;
    }

    public static String call(String name, String args) {
        final Service s = SERVICES.get(name);
        if (s == null) return null;

        final String[] box = new String[1];
        final CountDownLatch latch = new CountDownLatch(1);
        Thread t = new Thread(new Runnable() {
            @Override public void run() {
                try {
                    ScriptToolResult out = new ScriptToolResult();

                    synchronized (s) {
                        s.handler.call(args == null ? "" : args, out);
                    }
                    box[0] = out.get();
                } catch (Throwable e) {
                    HookLog.e("[脚本服务] " + s.name + " 出错", e);
                } finally {
                    latch.countDown();
                }
            }
        }, "cat-script-svc");
        t.setDaemon(true);
        t.start();
        try {
            if (!latch.await(CALL_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                HookLog.w("[脚本服务] " + name + " 超时 " + CALL_TIMEOUT_MS + "ms");
                return "";
            }
        } catch (InterruptedException ignore) {
            return "";
        }
        return box[0] == null ? "" : box[0];
    }

    public static List<String> list() {
        return new ArrayList<String>(SERVICES.keySet());
    }

    public static String ownerOf(String name) {
        Service s = SERVICES.get(name);
        return s == null ? "" : s.script;
    }

    public static String on(String script, String event, ScriptBusHandler h) {
        if (event == null || event.length() == 0) return "事件名不能为空";
        if (h == null) return "没给回调";
        LISTENERS.add(new Listener(event, script, h));
        return null;
    }

    public static void emit(String event, String data) {
        if (event == null || LISTENERS.isEmpty()) return;
        for (Listener l : LISTENERS) {
            if (!l.event.equals(event)) continue;
            final Listener fl = l;
            final String fdata = data == null ? "" : data;
            try {
                ScriptExec.run(l.script, new Runnable() {
                    @Override public void run() {
                        try {
                            fl.handler.onEvent(fdata);
                        } catch (Throwable t) {
                            HookLog.e("[脚本总线] " + fl.script + " 处理 " + fdata + " 失败", t);
                        }
                    }
                });
            } catch (Throwable t) {
                HookLog.e("[脚本总线] " + l.script + " 投递失败", t);
            }
        }
    }

    public static int removeByScript(String script) {
        if (script == null) return 0;
        int n = 0;
        List<String> rmKeys = new ArrayList<String>();
        for (java.util.Map.Entry<String, Service> e : SERVICES.entrySet()) {
            if (e.getValue().script.equals(script)) rmKeys.add(e.getKey());
        }
        for (String k : rmKeys) if (SERVICES.remove(k) != null) n++;

        List<Listener> rm = new ArrayList<Listener>();
        for (Listener l : LISTENERS) if (l.script.equals(script)) rm.add(l);
        for (Listener l : rm) if (LISTENERS.remove(l)) n++;
        return n;
    }

    public static int serviceCount() { return SERVICES.size(); }

    public static int listenerCount() { return LISTENERS.size(); }

    public static void clear() {
        SERVICES.clear();
        LISTENERS.clear();
    }
}
