package com.cathelperawa.owo.app.script;

public final class ScriptAbility {

    private ScriptAbility() {}

    public static String capabilities(String code) {
        if (code == null || code.length() == 0) return "";
        StringBuilder sb = new StringBuilder();
        add(sb, code, new String[]{"ctx.sendText(", "ctx.sendPic(", "ctx.sendFile(",
                "ctx.sendVoice(", "ctx.sendCard("}, "发消息");
        add(sb, code, new String[]{"ctx.fileWrite(", "ctx.fileRead(", "ctx.fileAppend(",
                "ctx.fileDelete("}, "读写文件");
        add(sb, code, new String[]{"ctx.http", "HttpURLConnection", "new URL("}, "网络");
        add(sb, code, new String[]{"ctx.patch(", "ctx.hook("}, "挂点位");
        add(sb, code, new String[]{"ctx.method("}, "改宿主方法");
        add(sb, code, new String[]{"ctx.strategy("}, "换算法");
        add(sb, code, new String[]{"ctx.tool("}, "AI 工具");
        add(sb, code, new String[]{"ctx.menu("}, "长按菜单");
        add(sb, code, new String[]{"ctx.tag("}, "AI 标签");
        add(sb, code, new String[]{"ctx.every(", "ctx.at("}, "定时");
        add(sb, code, new String[]{"ctx.dialog(", "ctx.screen(", "ctx.overlay(",
                "ctx.panel("}, "自绘界面");
        add(sb, code, new String[]{"ctx.floatBall("}, "悬浮球");
        add(sb, code, new String[]{"ctx.provide("}, "对外服务");
        add(sb, code, new String[]{"configForce", "ctx.skin", "ctx.theme("}, "改模块");
        add(sb, code, new String[]{"ctx.toast("}, "提示");
        return sb.toString();
    }

    private static void add(StringBuilder sb, String code, String[] keys, String label) {
        for (int i = 0; i < keys.length; i++) {
            if (code.indexOf(keys[i]) >= 0) {
                if (sb.length() > 0) sb.append(" · ");
                sb.append(label);
                return;
            }
        }
    }

    public static String wired(String name) {
        StringBuilder sb = new StringBuilder();
        try {
            int r = RouteRegistry.countOf(name);
            if (r > 0) sb.append(r).append(" 条命令");
        } catch (Throwable ignore) {
        }
        try {
            int t = ScriptToolRegistry.countOf(name);
            if (t > 0) {
                if (sb.length() > 0) sb.append(" · ");
                sb.append(t).append(" 个 AI 工具");
            }
        } catch (Throwable ignore) {
        }
        try {
            int p = PatchPoint.countOf(name);
            if (p > 0) {
                if (sb.length() > 0) sb.append(" · ");
                sb.append(p).append(" 个点位");
            }
        } catch (Throwable ignore) {
        }
        return sb.toString();
    }
}
