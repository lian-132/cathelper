package com.cathelperawa.owo.app.script;

import com.cathelperawa.owo.app.core.Bg;
import com.cathelperawa.owo.app.core.HookLog;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

public final class ScriptMessageRegistry {

    static final long SLOW_MS = 500;

    public static final class Sub {
        public final String script;
        final ScriptMessageHandler handler;
        final AtomicLong calls = new AtomicLong();
        final AtomicLong errors = new AtomicLong();

        Sub(String script, ScriptMessageHandler handler) {
            this.script = script;
            this.handler = handler;
        }

        public long calls() { return calls.get(); }
        public long errors() { return errors.get(); }
    }

    private static final List<Sub> SUBS = new CopyOnWriteArrayList<Sub>();

    private ScriptMessageRegistry() {}

    public static String add(String script, ScriptMessageHandler h) {
        if (h == null) return "没给回调";
        Sub s = new Sub(script, h);
        for (int i = 0; i < SUBS.size(); i++) {
            if (SUBS.get(i).script.equals(script)) {
                SUBS.set(i, s);
                return null;
            }
        }
        if (SUBS.size() >= 16) return "最多 16 个脚本同时监听消息";
        SUBS.add(s);
        HookLog.i("[脚本消息] " + script + " 开始监听全部消息");
        return null;
    }

    public static int removeByScript(String script) {
        if (script == null) return 0;
        List<Sub> rm = new ArrayList<Sub>();
        for (Sub s : SUBS) if (s.script.equals(script)) rm.add(s);
        int n = 0;
        for (Sub s : rm) if (SUBS.remove(s)) n++;
        return n;
    }

    public static void clear() { SUBS.clear(); }

    public static int size() { return SUBS.size(); }

    public static void dispatch(final ScriptMsg m) {
        if (m == null || SUBS.isEmpty()) return;

        if (m.self && SentGuard.isRecent(m.text)) return;

        java.util.List<Sub> snapshot = new java.util.ArrayList<Sub>(SUBS);
        for (int i = 0; i < snapshot.size(); i++) {
            final Sub s = snapshot.get(i);
            ScriptExec.run(s.script, new Runnable() {
                @Override public void run() {
                    long t0 = System.currentTimeMillis();
                    try {
                        s.handler.onMessage(m);
                        s.calls.incrementAndGet();
                    } catch (Throwable t) {
                        s.errors.incrementAndGet();
                        HookLog.e("[脚本消息] " + s.script + " 回调失败", t);
                        return;
                    }
                    long cost = System.currentTimeMillis() - t0;
                    if (cost > SLOW_MS) {
                        HookLog.w("[脚本消息] " + s.script + " 处理一条花了 " + cost + "ms（偏慢）");
                    }
                }
            });
        }
    }
}
