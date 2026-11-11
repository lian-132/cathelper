package com.cathelperawa.owo.app.script;

import com.cathelperawa.owo.app.core.Bg;
import com.cathelperawa.owo.app.core.HookLog;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ScriptTagRegistry {

    static final long TIMEOUT_MS = 3000;

    private static final Pattern TAG = Pattern.compile(
            "\\[([A-Za-z0-9_\\u4e00-\\u9fa5]{1,12})(?:\\s+([^\\]\\[]*))?\\]");

    public static final class Tag {
        public final String script;
        public final String name;
        final ScriptTagHandler handler;

        Tag(String script, String name, ScriptTagHandler h) {
            this.script = script;
            this.name = name;
            this.handler = h;
        }
    }

    private static final List<Tag> TAGS = new CopyOnWriteArrayList<Tag>();

    private ScriptTagRegistry() {}

    public static String add(String script, String name, ScriptTagHandler h) {
        if (name == null || !name.matches("[A-Za-z0-9_\\u4e00-\\u9fa5]{1,12}")) {
            return "标签名只能用中英文/数字/下划线，1~12 字符";
        }
        if (h == null) return "没给处理函数";
        Tag t = new Tag(script, name, h);
        for (int i = 0; i < TAGS.size(); i++) {
            Tag o = TAGS.get(i);
            if (o.script.equals(script) && o.name.equals(name)) {
                TAGS.set(i, t);
                return null;
            }
        }
        for (Tag o : TAGS) {
            if (o.name.equals(name)) return "标签「" + name + "」已被脚本「" + o.script + "」占用";
        }
        TAGS.add(t);
        HookLog.i("[脚本标签] " + script + " 注册「" + name + "」");
        return null;
    }

    public static int removeByScript(String script) {
        if (script == null) return 0;
        List<Tag> rm = new ArrayList<Tag>();
        for (Tag t : TAGS) if (t.script.equals(script)) rm.add(t);
        int n = 0;
        for (Tag t : rm) if (TAGS.remove(t)) n++;
        return n;
    }

    public static void clear() { TAGS.clear(); }

    public static int size() { return TAGS.size(); }

    public static Tag find(String name) {
        for (Tag t : TAGS) if (t.name.equals(name)) return t;
        return null;
    }

    public static String process(String text) {
        if (text == null || text.length() == 0 || TAGS.isEmpty()) return text;
        if (text.indexOf('[') < 0) return text;
        try {
            Matcher m = TAG.matcher(text);
            StringBuffer sb = new StringBuffer();
            while (m.find()) {
                String name = m.group(1);
                String arg = m.group(2) == null ? "" : m.group(2);
                Tag t = find(name);
                if (t == null) {
                    
                    m.appendReplacement(sb, Matcher.quoteReplacement(m.group(0)));
                    continue;
                }
                String repl = invoke(t, arg);
                m.appendReplacement(sb, Matcher.quoteReplacement(repl));
            }
            m.appendTail(sb);
            return sb.toString();
        } catch (Throwable t) {
            HookLog.w("[脚本标签] 处理失败 " + t);
            return text;
        }
    }

    private static String invoke(Tag t, String arg) {
        final ScriptToolResult out = new ScriptToolResult();
        try {
            boolean ok = ScriptExec.call(t.script, new Runnable() {
                @Override public void run() {
                    t.handler.call(arg, out);
                }
            }, ScriptExec.TIMEOUT_MS);
            if (!ok) return "";
        } catch (Throwable e) {
            HookLog.e("[脚本标签] " + t.name + " 失败", e);
            return "";
        }
        String r = out.get();
        return r == null ? "" : r;
    }
}
