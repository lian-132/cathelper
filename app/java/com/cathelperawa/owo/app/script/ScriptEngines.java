package com.cathelperawa.owo.app.script;

import com.cathelperawa.owo.app.core.HookLog;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ScriptEngines {

    private static final Map<String, Spec> REG = new LinkedHashMap<String, Spec>();

    static final class Spec {
        final String provider;
        final String func;
        final String suffix;
        
        final boolean allowFile;
        
        final boolean allowDir;

        Spec(String provider, String func, String suffix, boolean file, boolean dir) {
            this.provider = provider;
            this.func = func;
            this.suffix = suffix;
            this.allowFile = file;
            this.allowDir = dir;
        }
    }

    private ScriptEngines() {}

    public static String register(String suffix, String scriptName, String funcName) {
        return register(suffix, scriptName, funcName, "both");
    }

    public static String register(String suffix, String scriptName, String funcName, String forms) {
        if (suffix == null || suffix.trim().length() == 0) return "后缀不能为空";
        String s = suffix.trim().toLowerCase();
        if (s.charAt(0) != '.') s = "." + s;
        if (".qqs".equals(s) || ".js".equals(s) || ".java".equals(s)) {
            return "不能给 " + s + " 注册（会自指）";
        }
        if (scriptName == null || scriptName.length() == 0) return "提供者脚本不能为空";

        
        java.io.File pf = ScriptHost.entryOf(scriptName);
        if (pf == null || !pf.exists()) return "找不到提供者脚本「" + scriptName + "」";
        String f = (funcName == null || funcName.trim().length() == 0) ? "engineEval" : funcName.trim();
        String fm = forms == null ? "both" : forms.trim().toLowerCase();
        boolean force = fm.indexOf("force") >= 0 || fm.indexOf("覆盖") >= 0;
        boolean file = !"dir".equals(fm) && !"folder".equals(fm) && !"目录".equals(fm);
        boolean dir = !"file".equals(fm) && !"单文件".equals(fm);
        if (!file && !dir) { file = true; dir = true; }
        synchronized (REG) {
            Spec old = REG.get(s);
            if (old != null && !scriptName.equals(old.provider)) {

                if (!force) {
                    HookLog.w("[引擎] 拒绝注册 " + s + "：已被脚本[" + old.provider + "]占用"
                            + "（" + (old.allowDir && !old.allowFile ? "文件夹" : "文件/文件夹") + "）");
                    return s + " 已被脚本「" + old.provider + "」占用。"
                            + "要强制接管，forms 里加 force";
                }
                HookLog.w("[引擎] 脚本[" + scriptName + "] 强制接管 " + s
                        + "（原占用者：" + old.provider + "）");
            }
            REG.put(s, new Spec(scriptName, f, s, file, dir));
        }
        HookLog.i("[引擎] 脚本[" + scriptName + "] 注册引擎 " + s + " → " + f + "(String)"
                + " 形态：" + (file ? "文件" : "") + (dir ? "文件夹" : ""));
        return null;
    }

    public static String unregister(String suffix) {
        if (suffix == null) return "后缀为空";
        String s = suffix.trim().toLowerCase();
        if (s.length() > 0 && s.charAt(0) != '.') s = "." + s;
        synchronized (REG) {
            return REG.remove(s) == null ? "没有注册过 " + s : null;
        }
    }

    public static int removeByScript(String script) {
        if (script == null) return 0;
        int n = 0;
        synchronized (REG) {
            java.util.List<String> del = new java.util.ArrayList<String>();
            for (Map.Entry<String, Spec> e : REG.entrySet()) {
                if (script.equals(e.getValue().provider)) del.add(e.getKey());
            }
            for (int i = 0; i < del.size(); i++) {
                REG.remove(del.get(i));
                n++;
            }
        }
        if (n > 0) HookLog.w("[引擎] 脚本[" + script + "] 停用，下线 " + n + " 个桥接引擎");
        return n;
    }

    public static java.util.List<String> knownSuffixes() {
        java.util.ArrayList<String> out = new java.util.ArrayList<String>();
        out.add(".qqs");
        out.add(".java");
        out.add(".js");
        out.add(".lua");
        synchronized (REG) {
            for (String k : REG.keySet()) if (!out.contains(k)) out.add(k);
        }
        return out;
    }

    public static String enginesProvidedBy(String script) {
        if (script == null) return "";
        StringBuilder sb = new StringBuilder();
        synchronized (REG) {
            for (java.util.Map.Entry<String, Spec> e : REG.entrySet()) {
                if (script.equals(e.getValue().provider)) {
                    if (sb.length() > 0) sb.append(' ');
                    sb.append(e.getKey());
                }
            }
        }
        return sb.toString();
    }

    public static void registerPlaceholder(String suffix, String provider, String forms) {
        if (suffix == null || provider == null) return;
        String s = suffix.trim().toLowerCase();
        if (s.length() == 0) return;
        if (s.charAt(0) != '.') s = "." + s;
        if (".qqs".equals(s) || ".js".equals(s) || ".java".equals(s) || ".lua".equals(s)) return;
        synchronized (REG) {
            if (REG.containsKey(s)) return;   
            boolean dir = forms != null && forms.toLowerCase().indexOf("dir") >= 0;
            boolean file = !dir || forms.toLowerCase().indexOf("file") >= 0;
            REG.put(s, new Spec(provider, "engineEval", s, file, dir));
            HookLog.i("[引擎] 占位声明 " + s + "（提供者：" + provider + "，未启用）");
        }
    }

    public static boolean isPlaceholder(String suffix) {
        String s = norm(suffix);
        synchronized (REG) {
            Spec sp = REG.get(s);
            return sp != null && !ScriptHost.isEnabled(sp.provider);
        }
    }

    public static boolean allowsFile(String ext) {
        String s = norm(ext);
        synchronized (REG) {
            Spec sp = REG.get(s);
            return sp == null || sp.allowFile;
        }
    }

    public static boolean allowsDir(String ext) {
        String s = norm(ext);
        synchronized (REG) {
            Spec sp = REG.get(s);
            return sp == null || sp.allowDir;
        }
    }

    public static java.util.List<String> suffixesProvidedBy(String script) {
        java.util.ArrayList<String> out = new java.util.ArrayList<String>();
        if (script == null) return out;
        synchronized (REG) {
            for (java.util.Map.Entry<String, Spec> e : REG.entrySet()) {
                if (script.equals(e.getValue().provider)) out.add(e.getKey());
            }
        }
        return out;
    }

    private static String norm(String ext) {
        if (ext == null) return "";
        String s = ext.trim().toLowerCase();
        if (s.length() > 0 && s.charAt(0) != '.') s = "." + s;
        return s;
    }

    public static java.util.List<String> registeredSuffixes() {
        java.util.ArrayList<String> out = new java.util.ArrayList<String>();
        synchronized (REG) {
            out.addAll(REG.keySet());
        }
        return out;
    }

    public static String providerOf(String suffix) {
        if (suffix == null) return null;
        String s = suffix.trim().toLowerCase();
        if (s.length() > 0 && s.charAt(0) != '.') s = "." + s;
        synchronized (REG) {
            Spec sp = REG.get(s);
            return sp == null ? null : sp.provider;
        }
    }

    public static String list() {
        StringBuilder sb = new StringBuilder();
        synchronized (REG) {
            for (Map.Entry<String, Spec> e : REG.entrySet()) {
                if (sb.length() > 0) sb.append(' ');
                sb.append(e.getKey()).append("→").append(e.getValue().provider);
            }
        }
        return sb.toString();
    }

    public static ScriptEngine create(File entry) {
        String name = entry == null ? "" : entry.getName().toLowerCase();
        int dot = name.lastIndexOf('.');
        String ext = dot >= 0 ? name.substring(dot) : "";
        synchronized (REG) {
            Spec sp = REG.get(ext);
            if (sp != null) return new BridgeEngine(sp.provider, sp.func, sp.suffix);
        }
        if (name.endsWith(".js")) {
            return new RhinoEngine(hostClassLoader());
        }
        if (name.endsWith(".lua")) {
            return new LuaEngine();
        }
        return new BshEngine();
    }

    public static ScriptEngine createFor(String lang) {
        String l = lang == null ? "" : lang.trim().toLowerCase();
        if ("js".equals(l) || "javascript".equals(l) || "rhino".equals(l)) {
            return new RhinoEngine(hostClassLoader());
        }
        if ("lua".equals(l)) {
            return new LuaEngine();
        }
        if ("bsh".equals(l) || "beanshell".equals(l) || "java".equals(l) || l.length() == 0) {
            return new BshEngine();
        }
        synchronized (REG) {
            Spec sp = REG.get(l.charAt(0) == '.' ? l : "." + l);
            if (sp != null) return new BridgeEngine(sp.provider, sp.func, sp.suffix);
        }
        return null;
    }

    public static String engineLabel(File entry) {
        String name = entry == null ? "" : entry.getName().toLowerCase();
        int dot = name.lastIndexOf('.');
        String ext = dot >= 0 ? name.substring(dot) : "";
        synchronized (REG) {
            Spec sp = REG.get(ext);
            if (sp != null) return "桥接 " + sp.suffix;
        }
        if (name.endsWith(".lua")) return "Lua";
        return name.endsWith(".js") ? "Rhino" : "BeanShell";
    }

    public static ClassLoader hostClassLoader() {
        try {
            return com.cathelperawa.owo.app.bridge.QQEnv.classLoader();
        } catch (Throwable t) {
            return ScriptEngines.class.getClassLoader();
        }
    }
}
