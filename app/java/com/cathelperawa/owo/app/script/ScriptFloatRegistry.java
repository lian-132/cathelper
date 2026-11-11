package com.cathelperawa.owo.app.script;

import com.cathelperawa.owo.app.core.HookLog;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class ScriptFloatRegistry {

    public static final class Ball {
        public final String script;
        public final String id;
        public final String text;
        final ScriptFloatHandler handler;

        int bgColor = 0x00000000;
        
        int borderColor = 0x00000000;
        
        int sizeDp = 42;
        
        boolean clickMenu = true;
        
        volatile String style = "auto";
        
        volatile boolean hidden = false;
        
        final java.util.List<MenuItem> menu = new java.util.concurrent.CopyOnWriteArrayList<MenuItem>();
        
        String icon = "ic_module";

        Ball(String script, String id, String text, ScriptFloatHandler handler) {
            this.script = script;
            this.id = id;
            this.text = text;
            this.handler = handler;
        }

        public String key() { return script + "." + id; }
    }

    public static final class MenuItem {
        
        public final String script;
        public final String id;
        public final String label;
        final ScriptFloatHandler handler;

        MenuItem(String script, String id, String label, ScriptFloatHandler h) {
            this.script = script;
            this.id = id;
            this.label = label;
            this.handler = h;
        }
    }

    private static final List<Ball> BALLS = new CopyOnWriteArrayList<Ball>();

    private ScriptFloatRegistry() {}

    public static String add(String script, String id, String text, ScriptFloatHandler h) {
        if (id == null || id.length() == 0 || id.length() > 24
                || id.indexOf('/') >= 0 || id.indexOf((char) 92) >= 0) {
            return "悬浮球 id 只能用英文/数字/下划线，1~24 字符：" + id;
        }
        if (h == null) return "悬浮球没给点击回调";
        
        String label = (text == null || text.length() == 0) ? id : text;

        Ball b = new Ball(script, id, label, h);
        for (int i = 0; i < BALLS.size(); i++) {
            Ball o = BALLS.get(i);
            if (o.script.equals(script) && o.id.equals(id)) {
                BALLS.set(i, b);
                ScriptFloat.refreshAll();
                return null;
            }
        }
        
        int mine = countOf(script);
        if (mine >= 5) return "一个脚本最多 5 个悬浮球";
        BALLS.add(b);
        HookLog.i("[悬浮球] " + script + " 注册「" + id + "」" + label);
        ScriptFloat.refreshAll();
        return null;
    }

    public static String setTheme(String script, String id, String spec) {
        Ball target = null;
        for (Ball b : BALLS) {
            if (b.script.equals(script) && b.id.equals(id)) { target = b; break; }
        }
        if (target == null) return "没有这个球（先 floatBall 注册）";
        if (spec == null || spec.length() == 0) return "样式串是空的";

        try {
            String[] parts = spec.split(",");
            for (int i = 0; i < parts.length; i++) {
                String kv = parts[i].trim();
                int eq = kv.indexOf('=');
                if (eq <= 0) continue;
                String k = kv.substring(0, eq).trim().toLowerCase();
                String v = kv.substring(eq + 1).trim();
                if ("bg".equals(k)) {
                    target.bgColor = "transparent".equalsIgnoreCase(v) ? 0 : safeColor(v);
                } else if ("border".equals(k)) {
                    target.borderColor = safeColor(v);
                } else if ("size".equals(k)) {
                    try { target.sizeDp = Math.max(24, Math.min(96, Integer.parseInt(v))); }
                    catch (Throwable ignore) { }
                } else if ("style".equals(k)) {
                    target.style = v.toLowerCase();
                } else if ("click".equals(k)) {
                    target.clickMenu = !"run".equalsIgnoreCase(v);
                } else if ("icon".equals(k)) {
                    if (v.matches("[a-zA-Z0-9_]{1,40}")) target.icon = v;
                }
            }
            ScriptFloat.refreshAll();
            return null;
        } catch (Throwable t) {
            return "解析失败：" + t;
        }
    }

    private static int safeColor(String v) {
        int c = parseColor(v);
        return (c == Integer.MIN_VALUE) ? 0 : c;
    }

    public static String addMenuItem(String script, String ballId, String itemId,
                                     String label, ScriptFloatHandler h) {
        Ball b = null;
        for (Ball o : BALLS) {
            if (o.script.equals(script) && o.id.equals(ballId)) { b = o; break; }
        }
        if (b == null) return "没有这个球：" + ballId;
        if (label == null || label.trim().length() == 0) return "菜单名不能为空";
        if (itemId == null || itemId.trim().length() == 0) return "菜单 id 不能为空";
        if (h == null) return "没给回调";
        String lb = label.trim();
        for (int i = 0; i < b.menu.size(); i++) {
            if (b.menu.get(i).id.equals(itemId)) {
                b.menu.set(i, new MenuItem(script, itemId, lb, h));
                return null;
            }
        }
        if (b.menu.size() >= 6) return "一个球最多 6 个自定义菜单项";
        b.menu.add(new MenuItem(script, itemId, lb, h));
        HookLog.i("[悬浮球] " + script + " 给球加了菜单项「" + lb + "」");
        return null;
    }

    public static int removeByScript(String script) {
        if (script == null) return 0;
        List<Ball> rm = new ArrayList<Ball>();
        for (Ball b : BALLS) if (b.script.equals(script)) rm.add(b);
        int n = 0;
        for (Ball b : rm) if (BALLS.remove(b)) n++;
        if (n > 0) ScriptFloat.refreshAll();
        return n;
    }

    public static void clear() {
        BALLS.clear();
        ScriptFloat.refreshAll();
    }

    public static Ball find(String script, String id) {
        for (Ball b : BALLS) {
            if (b.script.equals(script) && b.id.equals(id)) return b;
        }
        return null;
    }

    public static String setBg(String script, String id, String color) {
        Ball b = find(script, id);
        if (b == null) return "没有这个球：" + id;
        int c = parseColor(color);
        if (c == Integer.MIN_VALUE) return "颜色格式不对：" + color;
        b.bgColor = c;
        ScriptFloat.refreshAll();
        return null;
    }

    public static String setSize(String script, String id, int dp) {
        Ball b = find(script, id);
        if (b == null) return "没有这个球：" + id;
        if (dp < 20 || dp > 96) return "尺寸要在 20~96 之间";
        b.sizeDp = dp;
        ScriptFloat.refreshAll();
        return null;
    }

    static int parseColor(String v) {
        try {
            if (v == null) return Integer.MIN_VALUE;
            String s = v.trim();
            if (s.startsWith("#")) s = s.substring(1);
            long n = Long.parseLong(s, 16);
            if (s.length() == 6) n |= 0xFF000000L;      
            else if (s.length() == 8) {  }
            else if (s.length() == 3) {
                
                int r = (int) ((n >> 8) & 0xF), g = (int) ((n >> 4) & 0xF), b = (int) (n & 0xF);
                return 0xFF000000 | (r * 17 << 16) | (g * 17 << 8) | (b * 17);
            } else return Integer.MIN_VALUE;
            return (int) n;
        } catch (Throwable t) {
            return Integer.MIN_VALUE;
        }
    }

    public static List<Ball> all() { return new ArrayList<Ball>(BALLS); }

    public static int size() { return BALLS.size(); }

    public static int countOf(String script) {
        int n = 0;
        for (Ball b : BALLS) if (b.script.equals(script)) n++;
        return n;
    }
}
