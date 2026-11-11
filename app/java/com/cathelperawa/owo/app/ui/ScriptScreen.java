package com.cathelperawa.owo.app.ui;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.cathelperawa.owo.app.core.HookLog;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ScriptScreen {

    public interface Handler {
        void build(ScriptUi ui, Result out);
    }

    public static final class Result {
        private View v;
        
        private String bgPath;
        
        private int veil = 0xB0;
        
        private int imgAlpha = 0xFF;

        public void content(View root) { this.v = root; }
        public View get() { return v; }

        public void background(String path) { background(path, 70); }

        public void background(String path, int veilPercent) {
            this.bgPath = path;
            int p = veilPercent < 0 ? 0 : (veilPercent > 100 ? 100 : veilPercent);
            this.veil = (int) (255f * p / 100f);
        }

        public String bgPath() { return bgPath; }
        public int bgVeil() { return veil; }
        public int bgImageAlpha() { return imgAlpha; }

        public void backgroundAlpha(int percent) {
            int p = percent < 0 ? 0 : (percent > 100 ? 100 : percent);
            this.imgAlpha = (int) (255f * p / 100f);
        }
    }

    private static final class Entry {
        final String script;
        final Handler handler;
        Entry(String script, Handler handler) {
            this.script = script;
            this.handler = handler;
        }
    }

    private static final Map<String, Entry> MAP = new LinkedHashMap<String, Entry>();

    private ScriptScreen() {}

    public static void register(String script, String pageKey, Handler h) {
        if (pageKey == null || h == null) return;
        synchronized (MAP) {
            MAP.put(pageKey, new Entry(script, h));
            HookLog.i("[整页] 脚本[" + script + "] 接管页面 " + pageKey);
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
            return del.size();
        }
    }

    public static boolean has(String pageKey) {
        if (pageKey == null) return false;
        synchronized (MAP) {
            return MAP.containsKey(pageKey);
        }
    }

    public static boolean any() {
        synchronized (MAP) {
            return !MAP.isEmpty();
        }
    }

    public static List<String> keys() {
        synchronized (MAP) {
            return new ArrayList<String>(MAP.keySet());
        }
    }

    

    private static final class PageEntry {
        final String script;
        final String title;
        final String sub;
        final Handler handler;

        PageEntry(String script, String title, String sub, Handler handler) {
            this.script = script;
            this.title = title;
            this.sub = sub;
            this.handler = handler;
        }
    }

    private static final Map<String, PageEntry> PAGES = new LinkedHashMap<String, PageEntry>();

    public static void registerPage(String script, String title, String sub,
                                    String key, Handler h) {
        if (key == null || key.length() == 0 || h == null) return;
        synchronized (PAGES) {
            PageEntry old = PAGES.get(key);
            if (old != null && !script.equals(old.script)) {
                HookLog.w("[页面] " + script + " 想占用页面键 " + key
                        + "（已被 " + old.script + " 占用），本次忽略");
                return;
            }
            PAGES.put(key, new PageEntry(script, title == null ? "脚本页面" : title,
                    sub == null ? "" : sub, h));
            HookLog.i("[页面] 脚本[" + script + "] 新增页面 " + key + "「" + title + "」");
        }
    }

    public static int removePagesByScript(String script) {
        if (script == null) return 0;
        int n = 0;
        synchronized (PAGES) {
            List<String> del = new ArrayList<String>();
            for (Map.Entry<String, PageEntry> e : PAGES.entrySet()) {
                if (script.equals(e.getValue().script)) del.add(e.getKey());
            }
            for (int i = 0; i < del.size(); i++) {
                PAGES.remove(del.get(i));
                n++;
            }
        }
        return n;
    }

    public static boolean hasPage(String key) {
        if (key == null) return false;
        synchronized (PAGES) {
            return PAGES.containsKey(key);
        }
    }

    public static java.util.List<String[]> declaredPages() {
        java.util.ArrayList<String[]> out = new java.util.ArrayList<String[]>();
        synchronized (PAGES) {
            for (Map.Entry<String, PageEntry> e : PAGES.entrySet()) {
                out.add(new String[]{e.getKey(), e.getValue().title, e.getValue().sub});
            }
        }
        return out;
    }

    public static View buildDeclared(Context c, String key) {
        PageEntry e;
        synchronized (PAGES) {
            e = PAGES.get(key);
        }
        if (e == null) return null;
        try {
            ScriptUi ui = new ScriptUi(c);
            Result r = new Result();
            e.handler.build(ui, r);
            View v = r.get();
            return v == null ? errorPage(c, "脚本没有返回界面") : v;
        } catch (Throwable t) {
            HookLog.e("[页面] 脚本页面构建失败 " + key, t);
            return errorPage(c, "脚本页面出错了：" + rootCause(t));
        }
    }

    public static String owner(String pageKey) {
        if (pageKey == null) return null;
        synchronized (MAP) {
            Entry e = MAP.get(pageKey);
            return e == null ? null : e.script;
        }
    }

    public static boolean removePage(String pageKey) {
        if (pageKey == null) return false;
        synchronized (MAP) {
            Entry e = MAP.remove(pageKey);
            if (e != null) {
                HookLog.w("[整页] 用户强制收回页面 " + pageKey + "（原接管者：" + e.script + "）");
                return true;
            }
            return false;
        }
    }

    public static View build(Context c, String pageKey) {
        Entry e;
        synchronized (MAP) {
            e = MAP.get(pageKey);
        }
        if (e == null) return null;
        try {
            ScriptUi ui = new ScriptUi(c);
            Result r = new Result();
            e.handler.build(ui, r);
            View v = r.get();
            if (v == null) return errorPage(c, "脚本没有返回界面");
            return v;
        } catch (Throwable t) {
            HookLog.e("[整页] 脚本页面构建失败 " + pageKey, t);
            return errorPage(c, "脚本页面出错了：" + rootCause(t));
        }
    }

    public static String rootCause(Throwable t) {
        if (t == null) return "未知错误";
        Throwable cur = t;
        int guard = 0;
        while (cur.getCause() != null && cur.getCause() != cur && guard++ < 12) {
            cur = cur.getCause();
        }
        String m = cur.getMessage();
        if (m == null || m.length() == 0) return cur.toString();

        int cut = m.indexOf("...");
        if (m.startsWith("Target Error") || m.contains("at Line:")) {
            int nl = m.indexOf('\n');
            String first = nl > 0 ? m.substring(0, nl) : m;
            String where = "";
            int l = m.indexOf("at Line:");
            if (l < 0) l = m.indexOf(": at Line:");
            if (l >= 0) {
                int end = m.indexOf(',', l);
                where = " · " + m.substring(l, end > 0 ? end : Math.min(m.length(), l + 40));
            }
            return first + where;
        }
        if (cut > 0) m = m.substring(0, cut);
        return cur.getClass().getSimpleName() + ": " + m;
    }

    private static View errorPage(Context c, String msg) {
        try {
            ScriptUi ui = new ScriptUi(c);
            LinearLayout col = ui.column();
            col.setPadding(ui.dp(24), ui.dp(80), ui.dp(24), 0);
            col.addView(ui.text("页面加载失败", 20, "#C0392B", true));
            col.addView(ui.space(10));
            TextView d = ui.text(msg, 12, "#6B7C93", false);
            col.addView(d);
            col.addView(ui.space(18));
            col.addView(ui.text("修复脚本后重新启用即可恢复。", 12, "#8D9CB3", false));
            return ui.page(col, "#F7F9FF");
        } catch (Throwable t) {
            LinearLayout l = new LinearLayout(c);
            l.setGravity(Gravity.CENTER);
            l.setBackgroundColor(Color.WHITE);
            return l;
        }
    }

    public static int clearAll() {
        synchronized (MAP) {
            int n = MAP.size();
            MAP.clear();
            return n;
        }
    }
}
