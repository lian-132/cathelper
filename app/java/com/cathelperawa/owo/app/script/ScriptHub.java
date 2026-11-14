package com.cathelperawa.owo.app.script;

import com.cathelperawa.owo.app.core.HookLog;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class ScriptHub {

    public static final class Item {
        public final String script;
        public final String id;
        public final String label;
        final ScriptFloatHandler handler;

        Item(String script, String id, String label, ScriptFloatHandler h) {
            this.script = script;
            this.id = id;
            this.label = label;
            this.handler = h;
        }
    }

    public static final class Page {
        public final String script;
        public String title;

        public String icon = "ic_module";

        public boolean vertical = false;
        final ScriptFloatHandler onRun;
        public final List<Item> items = new CopyOnWriteArrayList<Item>();

        Page(String script, String title, ScriptFloatHandler onRun) {
            this.script = script;
            this.title = title;
            this.onRun = onRun;
        }
    }

    private static volatile String ballIcon = "ic_module";
    private static volatile int ballSizeDp = 42;

    private static final List<Page> PAGES = new CopyOnWriteArrayList<Page>();

    private ScriptHub() {}

    static void diag(String s) {
        try {
            android.content.Context c = com.cathelperawa.owo.app.bridge.QQEnv.appContext();
            if (c == null) return;
            java.io.File f = new java.io.File(c.getFilesDir(), "hub_diag.txt");
            if (f.length() > 200 * 1024) {

                f.delete();
            }
            java.io.FileWriter w = new java.io.FileWriter(f, true);
            w.write(new java.text.SimpleDateFormat("MM-dd HH:mm:ss",
                    java.util.Locale.US).format(new java.util.Date()) + " " + s + "\n");
            w.close();
        } catch (Throwable ignore) {
        }
    }

    public static boolean enabled() {
        return !PAGES.isEmpty();
    }

    public static List<Page> all() {
        return new ArrayList<Page>(PAGES);
    }

    public static List<Page> horizontal() {
        List<Page> out = new ArrayList<Page>();
        for (Page p : PAGES) if (!p.vertical) out.add(p);
        return out;
    }

    public static List<Page> vertical() {
        List<Page> out = new ArrayList<Page>();
        for (Page p : PAGES) if (p.vertical) out.add(p);
        return out;
    }

    public static int size() {
        return PAGES.size();
    }

    public static Page find(String script) {
        for (Page p : PAGES) if (p.script.equals(script)) return p;
        return null;
    }

    public static int indexOf(String script) {
        for (int i = 0; i < PAGES.size(); i++) {
            if (PAGES.get(i).script.equals(script)) return i;
        }
        return -1;
    }

    public static String join(String script, String title, String icon, ScriptFloatHandler h) {
        return join(script, title, icon, h, false);
    }

    public static String join(String script, String title, String icon,
                              ScriptFloatHandler h, boolean vertical) {
        if (script == null || script.length() == 0) return "脚本名是空的";
        if (h == null) return "接入官方球必须给点击回调";
        String t = (title == null || title.trim().length() == 0) ? script : title.trim();
        if (t.length() > 14) t = t.substring(0, 14);
        diag("join 请求 script=" + script + " title=" + t);

        Page old = find(script);
        if (old != null) {
            old.title = t;
            old.vertical = vertical;
            if (icon != null && icon.length() > 0) old.icon = icon;
            PAGES.set(PAGES.indexOf(old), old);
            ScriptFloat.refreshAll();
            return null;
        }
        if (PAGES.size() >= 12) return "官方球最多接 12 个脚本";
        Page p = new Page(script, t, h);
        p.vertical = vertical;
        if (icon != null && icon.length() > 0) p.icon = icon;
        PAGES.add(p);
        diag("join 成功 script=" + script + " 页数=" + PAGES.size());
        HookLog.i("[官方球] " + script + " 接入「" + t + "」，现在 " + PAGES.size() + " 页");
        ScriptFloat.refreshAll();
        return null;
    }

    public static String joinFront(String script, String title, String icon, ScriptFloatHandler h) {
        String err = join(script, title, icon, h);
        if (err != null) return err;
        Page p = find(script);
        if (p != null) {
            PAGES.remove(p);
            PAGES.add(0, p);
            diag("joinFront script=" + script + " 现在首页=" + PAGES.get(0).title);
            ScriptFloat.refreshAll();
        }
        return null;
    }

    public static String joinVertical(String script, String title, String icon,
                                      ScriptFloatHandler h) {
        String err = join(script, title, icon, h, true);
        if (err != null) return err;
        Page p = find(script);
        if (p != null) {
            PAGES.remove(p);
            PAGES.add(p);
            diag("joinVertical script=" + script + " 上下组=" + vertical().size());
            ScriptFloat.refreshAll();
        }
        return null;
    }

    public static boolean leave(String script) {
        Page p = find(script);
        diag("leave script=" + script + " 有页=" + (p != null));
        if (p == null) return false;
        PAGES.remove(p);
        ScriptFloat.refreshAll();
        return true;
    }

    public static String addItem(String script, String itemId, String label, ScriptFloatHandler h) {
        Page p = find(script);
        if (p == null) return "还没接入官方球（先 ctx.hub(...)）";
        if (label == null || label.trim().length() == 0) return "菜单名不能为空";
        if (h == null) return "没给回调";
        String id = (itemId == null || itemId.trim().length() == 0)
                ? ("i" + label.hashCode()) : itemId.trim();
        String lb = label.trim();
        for (int i = 0; i < p.items.size(); i++) {
            if (p.items.get(i).id.equals(id)) {
                p.items.set(i, new Item(script, id, lb, h));
                return null;
            }
        }
        if (p.items.size() >= 20) return "一页最多 20 个菜单项";
        p.items.add(new Item(script, id, lb, h));
        return null;
    }

    public static String setIcon(String script, String icon) {
        Page p = find(script);
        if (p == null) return "还没接入官方球";
        if (icon == null || icon.trim().length() == 0) return "图标名是空的";
        p.icon = icon.trim();
        ScriptFloat.refreshAll();
        return null;
    }

    public static String setBallIcon(String icon) {
        if (icon == null || icon.trim().length() == 0) return "图标名是空的";
        ballIcon = icon.trim();
        ScriptFloat.refreshAll();
        return null;
    }

    public static String ballIcon() {
        return ballIcon;
    }

    public static int ballSizeDp() {
        return ballSizeDp;
    }

    public static int removeByScript(String script) {
        if (script == null) return 0;
        Page p = find(script);
        if (p == null) return 0;
        PAGES.remove(p);
        ScriptFloat.refreshAll();
        return 1;
    }

    public static void clear() {
        PAGES.clear();
        ScriptFloat.refreshAll();
    }
}
