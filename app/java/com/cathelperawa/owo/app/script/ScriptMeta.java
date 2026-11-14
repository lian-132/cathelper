package com.cathelperawa.owo.app.script;

public final class ScriptMeta {

    public static final int API_VERSION = 3;

    public String require = "";
    public int needApi = 0;
    public String author = "";
    public String version = "";
    public String desc = "";

    public String trust = "";

    public boolean aiOnly = false;

    public String patch = "";

    public String id = "";

    public String needs = "";

    public String source = "";

    public String engines = "";

    public boolean isExternal() {
        return "external".equalsIgnoreCase(source == null ? "" : source.trim());
    }

    private ScriptMeta() {}

    public static ScriptMeta parseProp(String text) {
        ScriptMeta m = new ScriptMeta();
        if (text == null) return m;
        String[] lines = text.split("\n");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.length() == 0 || line.startsWith("#")) continue;
            int eq = line.indexOf('=');
            if (eq <= 0) continue;
            String key = line.substring(0, eq).trim().toLowerCase();
            String val = line.substring(eq + 1).trim();
            if ("id".equals(key)) m.id = val;
            else if ("author".equals(key) || "作者".equals(key)) m.author = val;
            else if ("version".equals(key) || "versionname".equals(key)) m.version = val;
            else if ("desc".equals(key) || "description".equals(key)) m.desc = val;
            else if ("require".equals(key)) { m.require = val; m.needApi = parseApi(val); }
            else if ("trust".equals(key)) m.trust = val;
            else if ("needs".equals(key)) m.needs = val;
            else if ("patch".equals(key)) m.patch = val;
            else if ("engines".equals(key) || "引擎".equals(key)) m.engines = val;
            else if ("source".equals(key) || "来源".equals(key)) m.source = val;
        }
        return m;
    }

    public static ScriptMeta merge(ScriptMeta main, ScriptMeta fallback) {
        if (main == null) return fallback == null ? new ScriptMeta() : fallback;
        if (fallback == null) return main;
        if (main.id.length() == 0) main.id = fallback.id;
        if (main.author.length() == 0) main.author = fallback.author;
        if (main.version.length() == 0) main.version = fallback.version;
        if (main.desc.length() == 0) main.desc = fallback.desc;
        if (main.require.length() == 0) { main.require = fallback.require; main.needApi = fallback.needApi; }
        if (main.trust.length() == 0) main.trust = fallback.trust;
        if (main.needs.length() == 0) main.needs = fallback.needs;
        if (main.patch.length() == 0) main.patch = fallback.patch;
        if (main.source.length() == 0) main.source = fallback.source;
        if (main.engines.length() == 0) main.engines = fallback.engines;
        return main;
    }

    public static ScriptMeta parse(String code) {
        ScriptMeta m = new ScriptMeta();
        if (code == null) return m;
        String head = code.length() > 2000 ? code.substring(0, 2000) : code;
        String[] lines = head.split("\n");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();

            String body = null;
            if (line.startsWith("//")) body = line.substring(2).trim();
            else if (line.startsWith("--")) body = line.substring(2).trim();
            else if (line.startsWith("#")) body = line.substring(1).trim();
            if (body == null) continue;
            int colon = body.indexOf(':');
            if (colon <= 0) continue;
            String key = body.substring(0, colon).trim().toLowerCase();
            String val = body.substring(colon + 1).trim();

            if ("require".equals(key)) {
                m.require = val;
                m.needApi = parseApi(val);
            } else if ("author".equals(key) || "作者".equals(key)) {
                m.author = val;
            } else if ("trust".equals(key)) {
                m.trust = val;
            } else if ("ai".equals(key)) {
                m.aiOnly = "only".equalsIgnoreCase(val) || "yes".equalsIgnoreCase(val)
                        || "true".equalsIgnoreCase(val) || "专属".equals(val);
            } else if ("id".equals(key)) {
                m.id = val;
            } else if ("needs".equals(key) || "依赖".equals(key)) {
                m.needs = val;
            } else if ("patch".equals(key)) {
                m.patch = val;
            } else if ("engines".equals(key) || "引擎".equals(key)) {
                m.engines = val;
            } else if ("source".equals(key) || "来源".equals(key)) {
                m.source = val;
            } else if ("version".equals(key) || "版本".equals(key)) {
                m.version = val;
            } else if ("desc".equals(key) || "说明".equals(key)) {
                m.desc = val;
            }
        }
        return m;
    }

    private static int parseApi(String v) {
        try {
            if (v == null) return 0;
            String s = v.toLowerCase().replace("api", "").replace(">", "")
                    .replace("=", "").replace(" ", "");
            if (s.length() == 0) return 0;
            int dot = s.indexOf('.');
            if (dot > 0) s = s.substring(0, dot);
            return Integer.parseInt(s);
        } catch (Throwable t) {
            return 0;
        }
    }

    public boolean apiOk() {
        return needApi <= API_VERSION;
    }

    public String apiError() {
        return "这个脚本要求 api>=" + needApi + "，当前宿主只提供 api" + API_VERSION
                + "（升级模块，或让作者改脚本）";
    }
}
