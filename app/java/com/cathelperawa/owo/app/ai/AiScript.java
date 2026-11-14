package com.cathelperawa.owo.app.ai;

import com.cathelperawa.owo.app.core.HookLog;
import com.cathelperawa.owo.app.script.RouteRegistry;
import com.cathelperawa.owo.app.script.ScriptHost;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.List;

final class AiScript {

    private AiScript() {}

    static boolean enabled() {
        try {

            if (!com.cathelperawa.owo.app.core.ConfigStore.getBool("ai_script_tool")) {
                return false;
            }
            return com.cathelperawa.owo.app.core.ConfigStore.getBool("ai_script");
        } catch (Throwable t) {
            return true;
        }
    }

    private static final int MAX_CODE = 64 * 1024;

    static JSONArray tools() throws Exception {
        JSONArray out = new JSONArray();
        out.put(toolWrite());
        out.put(toolRun());
        out.put(toolList());
        return out;
    }

    private static JSONObject toolWrite() throws Exception {
        JSONObject t = new JSONObject();
        t.put("type", "function");
        JSONObject fn = new JSONObject();
        fn.put("name", "script_write");
        fn.put("description",
                "写一个脚本（BeanShell，Java 语法），可选直接启用。\n"
              + "【什么时候用】用户明确要「做个命令」「加个功能」「发某句话就……」，"
              + "而且内置功能做不到的时候。\n"
              + "【什么时候别用】普通聊天、用户没要求新功能、你自己也说不清楚要做什么。\n"
              + "【脚本怎么写得对】\n"
              + "  1) 注册命令用 ctx.route(id, 触发词数组, handler)：\n"
              + "     ctx.route(\"today\", new String[]{\"今日\"}, new ScriptHandler() {\n"
              + "         public void handle(ScriptMsg m, String arg) {\n"
              + "             ctx.sendText(m.kind(), m.peer, \"今天 \" + new java.text.SimpleDateFormat(\"yyyy-MM-dd\").format(new java.util.Date()));\n"
              + "         }\n"
              + "     });\n"
              + "  2) 给 AI 加工具用 ctx.tool(name, 说明, handler)：\n"
              + "     ctx.tool(\"get_time\", \"查现在时间\", new ScriptToolHandler() {\n"
              + "         public void call(String args, ScriptToolResult out) {\n"
              + "             out.set(new java.util.Date().toString());\n"
              + "         }\n"
              + "     });\n"
              + "  3) handler 的方法签名按接口来（handle/call 里的参数就是给你的上下文）；\n"
              + "     工具结果用 out.set(文字) 交出去\n"
              + "  4) 发消息必须在文件头写 // capability: send，否则 sendText 抛异常\n"
              + "  5) ctx 上能用：log/me/myName/now/ai/route/tool/sendText/sendPic/get/set\n"
              + "  6) 还能 import 模块的 API 直接调，比如\n"
              + "     import com.cathelperawa.owo.app.api.CatApi;\n"
              + "     import com.cathelperawa.owo.app.api.IdentityApi;\n"
              + "  7) 不能用 package 语句；不要写 main 方法，顶层代码按顺序执行\n"
              + "  8) 语法基本是 Java 8：lambda、?.、?? 都能用；但 try-with-resources、\n"
              + "     多 catch（catch (A | B e)）、箭头 switch、字符串模板这些会解析失败\n"
              + "  9) 可以直接定义 class（class Helper { ... }），成员会自动当静态用\n"
              + " 10) 如果要写比较复杂的东西（很多类、方法引用 ::、泛型嵌套），\n"
              + "     在文件最前面加一行 // engine: java，会改用完整 Java 编译执行\n"
              + "     （编译器发现语法错了会自动帮你试这个模式，你不用硬猜）\n"
              + "【写完会自动试跑】语法错会把错误原文返回给你，你改完再调一次。");
        JSONObject props = new JSONObject();
        JSONObject n = new JSONObject();
        n.put("type", "string");
        n.put("description", "脚本名（中文或英文都行，不要带 / 和 ..），也是文件名");
        props.put("name", n);
        JSONObject c = new JSONObject();
        c.put("type", "string");
        c.put("description", "完整源码");
        props.put("code", c);
        JSONObject e = new JSONObject();
        e.put("type", "boolean");
        e.put("description", "true=试跑通过后立刻启用；false=只保存，等用户自己在脚本页开");
        props.put("enable", e);
        JSONObject params = new JSONObject();
        params.put("type", "object");
        params.put("properties", props);
        params.put("required", new JSONArray().put("name").put("code"));
        fn.put("parameters", params);
        t.put("function", fn);
        return t;
    }

    private static JSONObject toolRun() throws Exception {
        JSONObject t = new JSONObject();
        t.put("type", "function");
        JSONObject fn = new JSONObject();
        fn.put("name", "script_run");
        fn.put("description",
                "启用或停用一个已有脚本。\n"
              + "【什么时候用】用户说「把那个脚本关了」「再打开刚才那个」；"
              + "或者你写完脚本但当时 enable=false，用户现在要用了。\n"
              + "【注意】启用后群里发注册的触发词就会执行。");
        JSONObject props = new JSONObject();
        JSONObject n = new JSONObject();
        n.put("type", "string");
        n.put("description", "脚本名（不带 .qqs）");
        props.put("name", n);
        JSONObject on = new JSONObject();
        on.put("type", "boolean");
        on.put("description", "true=启用，false=停用");
        props.put("enable", on);
        JSONObject params = new JSONObject();
        params.put("type", "object");
        params.put("properties", props);
        params.put("required", new JSONArray().put("name").put("enable"));
        fn.put("parameters", params);
        t.put("function", fn);
        return t;
    }

    private static JSONObject toolList() throws Exception {
        JSONObject t = new JSONObject();
        t.put("type", "function");
        JSONObject fn = new JSONObject();
        fn.put("name", "script_list");
        fn.put("description",
                "列出所有脚本、它们是否启用、各注册了几条命令。\n"
              + "【什么时候用】用户问「有哪些脚本」「那个脚本还开着吗」，"
              + "或者你要确认某个脚本名再调用 script_run。");
        JSONObject params = new JSONObject();
        params.put("type", "object");
        JSONObject props = new JSONObject();
        params.put("properties", props);
        fn.put("parameters", params);
        t.put("function", fn);
        return t;
    }

    static String write(String name, String code, boolean enable) {
        String clean = safeName(name);
        if (clean == null) return "失败：脚本名只能用中英文、数字、下划线、横线，别带路径";
        if (code == null || code.trim().length() == 0) return "失败：源码是空的";
        if (code.length() > MAX_CODE) return "失败：源码太长（上限 " + (MAX_CODE / 1024) + "KB）";

        try {
            File dir = ScriptHost.dir();
            if (dir == null) return "失败：脚本目录不可用";
            File f = new File(dir, clean + ".qqs");

            boolean existed = f.isFile();
            if (existed) {
                try {
                    File bak = new File(dir, clean + ".qqs.bak");
                    copy(f, bak);
                } catch (Throwable ignore) {
                }
            }

            writeText(f, code);

            try {
                f.setReadable(true, false);
                f.setWritable(true, false);
            } catch (Throwable ignore) {
            }
            HookLog.i("[AI脚本] 写入 " + f.getName() + " " + code.length() + " 字节");

            ScriptHost.Result dry = ScriptHost.dryRun(clean);

            boolean switched = false;
            if (!dry.ok && !hasJavaHeader(code) && looksLikeSyntax(dry.message)) {
                String withHeader = "// engine: java\n" + code;
                writeText(f, withHeader);
                ScriptHost.Result dry2 = ScriptHost.dryRun(clean);
                if (dry2.ok) {
                    dry = dry2;
                    code = withHeader;
                    switched = true;
                } else {
                    dry = dry2;
                }
            }

            if (!dry.ok) {
                String hint = lookedLikeBshProblem(dry.message)
                        ? "\n（这段像是 BeanShell 的语法限制，重写时避开 ::、try-with-resources、多 catch 试试）"
                        : "";
                return "已保存为「" + clean + "」，但试跑失败了，改一下再调我：\n"
                        + trim(dry.message, 800) + hint;
            }

            if (!enable) {
                return "已保存「" + clean + "」（试跑通过" + (switched ? "，已自动切编译模式" : "") + "）。还没启用 —— "
                        + "需要用户到 设置 → 脚本 里打开开关，或者你调 script_run。";
            }

            ScriptHost.Result r = ScriptHost.enable(clean);
            if (!r.ok) {
                return "试跑通过但启用失败：\n" + r.message;
            }
            return "「" + clean + "」已启用 · " + r.message
                    + (switched ? "（自动切到编译模式）" : "")
                    + (existed ? "（原脚本已备份为 " + clean + ".qqs.bak）" : "")
                    + "\n现在群里发触发词就会执行了。";
        } catch (Throwable t) {
            HookLog.e("[AI脚本] 写入失败", t);
            return "写入失败：" + t;
        }
    }

    static String run(String name, boolean enable) {
        String clean = safeName(name);
        if (clean == null) return "失败：脚本名不合法";
        ScriptHost.Result r = enable ? ScriptHost.enable(clean) : ScriptHost.disable(clean);
        return (r.ok ? "✓ " : "✗ ") + r.message;
    }

    static String list() {
        try {
            List<String> names = ScriptHost.list();
            if (names.isEmpty()) return "现在一个脚本都没有。";
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < names.size(); i++) {
                String n = names.get(i);
                boolean on = ScriptHost.isEnabled(n);
                int routes = RouteRegistry.countOf(n);
                sb.append(on ? "● " : "○ ").append(n)
                  .append(on ? "（启用中，命令 " + routes + " 条）" : "（未启用）")
                  .append('\n');
            }
            return sb.toString().trim();
        } catch (Throwable t) {
            return "读取失败：" + t;
        }
    }

    private static String safeName(String name) {
        if (name == null) return null;
        String s = name.trim();
        if (s.endsWith(".qqs")) s = s.substring(0, s.length() - 4);
        if (s.length() == 0 || s.length() > 40) return null;
        if (!s.matches("[\\w\\u4e00-\\u9fa5.-]+")) return null;
        if (s.contains("..")) return null;
        return s;
    }

    private static void writeText(File f, String text) throws Exception {
        java.io.FileOutputStream out = new java.io.FileOutputStream(f);
        try {
            out.write(text.getBytes("UTF-8"));
        } finally {
            out.close();
        }
    }

    private static void copy(File from, File to) throws Exception {
        java.io.FileInputStream in = new java.io.FileInputStream(from);
        java.io.FileOutputStream out = new java.io.FileOutputStream(to);
        try {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        } finally {
            in.close();
            out.close();
        }
    }

    private static boolean hasJavaHeader(String code) {
        if (code == null) return false;
        String head = code.length() > 400 ? code.substring(0, 400) : code;
        return head.contains("engine: java") || head.contains("engine:java");
    }

    private static boolean looksLikeSyntax(String msg) {
        if (msg == null) return false;
        String m = msg.toLowerCase();
        return m.contains("parse") || m.contains("syntax") || m.contains("unexpected")
                || m.contains("encountered") || m.contains("line ") || m.contains("lexical");
    }

    private static boolean lookedLikeBshProblem(String msg) {
        if (msg == null) return false;
        return msg.contains("::") || msg.contains("Encountered: <") || msg.contains("Encountered: :");
    }

    private static String trim(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }
}
