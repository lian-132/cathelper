package com.cathelperawa.owo.app.script;

import com.cathelperawa.owo.app.core.Bg;
import com.cathelperawa.owo.app.core.HookLog;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class ScriptPanelRegistry {

    public static final class Panel {
        public final String script;
        public final String title;
        
        public final String spec;
        
        final ScriptPanelHandler handler;
        
        final com.cathelperawa.owo.app.ui.ScriptScreen.Handler view;

        Panel(String script, String title, String spec, ScriptPanelHandler h,
              com.cathelperawa.owo.app.ui.ScriptScreen.Handler v) {
            this.script = script;
            this.title = title;
            this.spec = spec;
            this.handler = h;
            this.view = v;
        }

        public boolean custom() { return view != null; }

        public String key() { return script + "." + title; }
    }

    private static final List<Panel> PANELS = new CopyOnWriteArrayList<Panel>();

    private ScriptPanelRegistry() {}

    public static String add(String script, String title, String spec, ScriptPanelHandler h) {
        if (title == null || title.trim().length() == 0) return "面板标题不能为空";
        if (title.length() > 10) return "标题最长 10 个字";
        if (h == null) return "没给事件回调";
        if (spec == null || spec.trim().length() == 0) return "界面描述是空的";

        Panel p = new Panel(script, title.trim(), spec.trim(), h, null);
        for (int i = 0; i < PANELS.size(); i++) {
            Panel o = PANELS.get(i);
            if (o.script.equals(script) && o.title.equals(p.title)) {
                PANELS.set(i, p);
                return null;
            }
        }
        if (countOf(script) >= 3) return "一个脚本最多 3 个面板";
        PANELS.add(p);
        HookLog.i("[脚本面板] " + script + " 注册「" + p.title + "」");
        return null;
    }

    public static String addCustom(String script, String title,
                                   com.cathelperawa.owo.app.ui.ScriptScreen.Handler v) {
        if (title == null || title.trim().length() == 0) return "面板标题不能为空";
        if (title.length() > 16) return "标题最长 16 个字";
        if (v == null) return "没给页面构建器";

        Panel p = new Panel(script, title.trim(), null, null, v);
        for (int i = 0; i < PANELS.size(); i++) {
            Panel o = PANELS.get(i);
            if (o.script.equals(script) && o.title.equals(p.title)) {
                PANELS.set(i, p);
                return null;
            }
        }
        if (countOf(script) >= 3) return "一个脚本最多 3 个面板";
        PANELS.add(p);
        HookLog.i("[脚本面板] " + script + " 注册自绘页「" + p.title + "」");
        return null;
    }

    public static int removeByScript(String script) {
        if (script == null) return 0;
        List<Panel> rm = new ArrayList<Panel>();
        for (Panel p : PANELS) if (p.script.equals(script)) rm.add(p);
        int n = 0;
        for (Panel p : rm) if (PANELS.remove(p)) n++;
        return n;
    }

    public static void clear() { PANELS.clear(); }

    public static List<Panel> all() { return new ArrayList<Panel>(PANELS); }

    public static int size() { return PANELS.size(); }

    public static List<Panel> ofScript(String script) {
        List<Panel> out = new ArrayList<Panel>();
        for (Panel p : PANELS) if (p.script.equals(script)) out.add(p);
        return out;
    }

    public static int countOf(String script) {
        return ofScript(script).size();
    }

    public static Panel find(String key) {
        for (Panel p : PANELS) if (p.key().equals(key)) return p;
        return null;
    }

    
    static void fire(final Panel p, final String id, final String value,
                     final ScriptPanelResult out) {
        final java.util.concurrent.CountDownLatch latch =
                new java.util.concurrent.CountDownLatch(1);
        Bg.run(new Runnable() {
            @Override public void run() {
                try {
                    ScriptExec.call(p.script, new Runnable() {
                        @Override public void run() {
                            p.handler.onEvent(id, value, out);
                        }
                    }, ScriptExec.TIMEOUT_MS);
                } catch (Throwable t) {
                    HookLog.e("[脚本面板] " + p.title + " 事件失败", t);
                } finally {
                    latch.countDown();
                }
            }
        }, "cat-script-panel");
        try {
            latch.await(2000, java.util.concurrent.TimeUnit.MILLISECONDS);
        } catch (InterruptedException ignore) {
        }
    }
}
