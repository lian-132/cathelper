package com.cathelperawa.owo.app.script;

import com.cathelperawa.owo.app.bridge.QQEnv;
import com.cathelperawa.owo.app.core.HookLog;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import bsh.Interpreter;

public final class ScriptHost {

    private ScriptHost() {}

    private static final String DIR = "scripts";

    public static final String SAFE_BOOT_FLAG = "safe_boot_once";

    public static boolean armSafeBoot() {
        try {
            java.io.File d = com.cathelperawa.owo.app.core.DataDir.global();
            if (d == null) return false;
            if (!d.exists()) d.mkdirs();
            java.io.File f = new java.io.File(d, SAFE_BOOT_FLAG);
            java.io.FileOutputStream fo = new java.io.FileOutputStream(f);
            fo.write(("armed " + System.currentTimeMillis()).getBytes("UTF-8"));
            fo.flush();
            fo.close();
            HookLog.w("[脚本] 已设置安全启动：下次打开 TIM 不加载任何脚本");
            return true;
        } catch (Throwable t) {
            HookLog.w("设置安全启动失败 " + t);
            return false;
        }
    }

    public static void persistAllDisabled() {
        try {
            java.io.File f = stateFile();
            if (f != null) {
                java.io.FileOutputStream fo = new java.io.FileOutputStream(f);
                fo.write(new byte[0]);
                fo.flush();
                fo.close();
            }
            HookLog.w("[脚本] 已清空全部启用状态");
        } catch (Throwable t) {
            HookLog.w("清空启用状态失败 " + t);
        }
    }

    public static boolean armModuleSafeMode() {
        try {
            java.io.File f = new java.io.File(
                    "/storage/emulated/0/Android/data/com.tencent.tim/files/cathelper_safe_mode");
            java.io.File dir = f.getParentFile();
            if (dir != null && !dir.isDirectory()) dir.mkdirs();
            boolean ok = f.exists() || f.createNewFile();
            HookLog.w("[脚本] 已写入模块安全模式标记：" + ok);
            return ok;
        } catch (Throwable t) {
            HookLog.w("写安全模式标记失败 " + t);
            return false;
        }
    }

    private static boolean consumeSafeBoot() {
        try {
            java.io.File d = com.cathelperawa.owo.app.core.DataDir.global();
            if (d == null) return false;
            java.io.File f = new java.io.File(d, SAFE_BOOT_FLAG);
            if (!f.exists()) return false;
            f.delete();
            HookLog.w("[脚本] 安全启动：本次不加载任何脚本（标记已清除，下次正常）");
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private static final int MAX_BYTES = 0;

    private static final Map<String, Loaded> LOADED = new LinkedHashMap<String, Loaded>();

    private static final Map<String, Boolean> EXTERNAL = new LinkedHashMap<String, Boolean>();

    public static Object callScriptFunction(String name, String func, Object[] args) {
        if (name == null || func == null) return null;
        Loaded l;
        synchronized (LOADED) {
            l = LOADED.get(name);
        }
        if (l == null) return null;
        try {
            return l.interp.callFunction(func, args);
        } catch (Throwable t) {
            HookLog.e("[桥接] 调用 " + name + "#" + func + " 失败", t);
            return null;
        }
    }

    public static boolean isExternal(String name) {
        if (name == null) return false;
        synchronized (EXTERNAL) {
            return EXTERNAL.containsKey(name);
        }
    }

    static void setExternal(String name, boolean ext) {
        if (name == null) return;
        synchronized (EXTERNAL) {
            if (ext) EXTERNAL.put(name, Boolean.TRUE);
            else EXTERNAL.remove(name);
        }
    }

    public static final class Result {
        public final String name;
        public final boolean ok;
        public final String message;
        public final long costMs;

        Result(String name, boolean ok, String message, long costMs) {
            this.name = name;
            this.ok = ok;
            this.message = message == null ? "" : message;
            this.costMs = costMs;
        }
    }

    static final class Loaded {
        final String name;
        final ScriptEngine interp;
        final Set<String> caps;

        final int scope;
        final long loadedAt;

        final File dataDir;

        Loaded(String name, ScriptEngine interp, Set<String> caps, int scope) {
            this(name, interp, caps, scope, null);
        }

        Loaded(String name, ScriptEngine interp, Set<String> caps, int scope, File dataDir) {
            this.name = name;
            this.interp = interp;
            this.caps = caps;
            this.scope = scope;
            this.dataDir = dataDir;
            this.loadedAt = System.currentTimeMillis();
        }
    }

    public static boolean isAutoLoad(String name) {
        try {
            return com.cathelperawa.owo.app.core.ConfigStore.getBool("script_auto_" + name);
        } catch (Throwable t) {
            return false;
        }
    }

    public static void setAutoLoad(String name, boolean on) {
        try {
            com.cathelperawa.owo.app.core.ConfigStore.setBool("script_auto_" + name, on);
        } catch (Throwable ignore) {
        }
    }

    public static Result delete(String name) {
        if (name == null) return new Result("", false, "名字为空", 0);
        synchronized (LOADED) {
            disableLocked(name);
        }
        try {
            File pkg = packageDir(name);
            File f = pkg != null ? pkg : fileOf(name);
            if (f != null && f.exists()) {
                if (f.isDirectory()) deleteTree(f);
                else if (!f.delete()) return new Result(name, false, "文件删不掉（可能被占用）", 0);
            }
            if (pkg == null) {
                File f2 = fileOf(name);
                if (f2 != null && f2.exists() && !f2.delete()) {
                    return new Result(name, false, "文件删不掉（可能被占用）", 0);
                }
            }
            try {
                File dd = new File(new File(dir(), "data"), name);
                deleteTree(dd);
            } catch (Throwable ignore) {
            }
            setAutoLoad(name, false);
            HookLog.i("脚本[" + name + "] 已删除");
            return new Result(name, true, "已删除", 0);
        } catch (Throwable t) {
            return new Result(name, false, "删除失败：" + t, 0);
        }
    }

    private static void deleteTree(File f) {
        if (f == null || !f.exists()) return;
        if (f.isDirectory()) {
            File[] cs = f.listFiles();
            if (cs != null) for (int i = 0; i < cs.length; i++) deleteTree(cs[i]);
        }
        f.delete();
    }

    public static String resolveName(String nameOrId) {
        if (nameOrId == null || nameOrId.length() == 0) return null;
        try {
            java.util.List<String> all = list();

            for (int i = 0; i < all.size(); i++) {
                ScriptMeta m = metaOf(all.get(i));
                if (m != null && nameOrId.equals(m.id)) return all.get(i);
            }

            for (int i = 0; i < all.size(); i++) {
                if (nameOrId.equals(all.get(i))) return all.get(i);
            }
            if (isEnabled(nameOrId)) return nameOrId;
        } catch (Throwable ignore) {
        }
        return null;
    }

    public static String dependenciesOk(String name) {
        try {
            ScriptMeta m = metaOf(name);
            if (m == null || m.needs == null || m.needs.trim().length() == 0) return null;
            String[] deps = m.needs.split("[,，\\s]+");
            for (int i = 0; i < deps.length; i++) {
                String d = deps[i].trim();
                if (d.length() == 0) continue;
                String depName = d;
                String minVer = "";
                int gt = d.indexOf(">=");
                if (gt > 0) {
                    depName = d.substring(0, gt).trim();
                    minVer = d.substring(gt + 2).trim();
                }
                String real = resolveName(depName);
                if (real == null) {
                    return "找不到依赖脚本「" + depName + "」（文件名或 // id 都要对得上）";
                }
                if (!isEnabled(real)) {
                    return "缺少依赖脚本「" + depName + "」——先去启用它";
                }
                if (minVer.length() > 0) {
                    String actual = metaOf(real) == null ? "" : metaOf(real).version;
                    if (!verAtLeast(actual, minVer)) {
                        return "依赖「" + depName + "」需要 v" + minVer
                                + "，当前是 v" + (actual.length() == 0 ? "?" : actual);
                    }
                }
            }
            return null;
        } catch (Throwable t) {
            return "依赖检查出错：" + t;
        }
    }

    public static java.util.List<String> dependents(String name) {
        java.util.List<String> out = new java.util.ArrayList<String>();
        try {
            java.util.List<String> all = list();
            for (int i = 0; i < all.size(); i++) {
                if (all.get(i).equals(name)) continue;
                ScriptMeta m = metaOf(all.get(i));
                if (m == null || m.needs == null) continue;

                String[] deps = m.needs.split("[,，\\s]+");
                boolean hit = false;
                for (int j = 0; j < deps.length && !hit; j++) {
                    String d = deps[j].trim();
                    if (d.length() == 0) continue;
                    int gt = d.indexOf(">=");
                    if (gt > 0) d = d.substring(0, gt).trim();
                    String real = resolveName(d);
                    if (name.equals(real) || name.equals(d)) hit = true;
                }
                if (hit) out.add(all.get(i));
            }
        } catch (Throwable ignore) {
        }
        return out;
    }

    public static boolean verAtLeast(String actual, String required) {
        try {
            int[] a = parseVer(actual);
            int[] r = parseVer(required);
            for (int i = 0; i < 3; i++) {
                if (a[i] > r[i]) return true;
                if (a[i] < r[i]) return false;
            }
            return true;
        } catch (Throwable t) {
            return true;
        }
    }

    private static int[] parseVer(String v) {
        int[] out = new int[]{0, 0, 0};
        if (v == null) return out;
        String[] p = v.trim().split("\\.");
        for (int i = 0; i < p.length && i < 3; i++) {
            try {
                out[i] = Integer.parseInt(p[i].replaceAll("[^0-9]", ""));
            } catch (Throwable ignore) {
            }
        }
        return out;
    }

    public static ScriptMeta metaOf(String name) {
        try {

            File f = entryOf(name);
            File prop = infoPropOf(name);
            if (prop != null) {
                ScriptMeta pm = ScriptMeta.parseProp(read(prop));
                if (pm != null) {

                    ScriptMeta hm = (f != null && f.exists())
                            ? ScriptMeta.parse(read(f)) : ScriptMeta.parse("");
                    return ScriptMeta.merge(pm, hm);
                }
            }
            if (f == null || !f.exists()) return ScriptMeta.parse("");
            return ScriptMeta.parse(read(f));
        } catch (Throwable t) {
            return ScriptMeta.parse("");
        }
    }

    public static int scopeOf(String name) {
        synchronized (LOADED) {
            Loaded l = LOADED.get(name);
            return l == null ? ScriptScope.ALL : l.scope;
        }
    }

    public static File dir() {
        File cached = DIR_CACHE;
        if (cached != null && cached.exists()) return cached;
        try {

            File ext = new File(android.os.Environment.getExternalStorageDirectory(),
                    "猫娘助手/脚本");
            if (ensure(ext)) {
                DIR_CACHE = ext;
                return ext;
            }
        } catch (Throwable ignore) {
        }
        try {

            File root = com.cathelperawa.owo.app.core.DataDir.root();
            if (root != null) {
                File d = new File(root, "脚本");
                if (ensure(d)) {
                    DIR_CACHE = d;
                    return d;
                }
            }
        } catch (Throwable ignore) {
        }
        try {

            File appExt = QQEnv.appContext().getExternalFilesDir(null);
            if (appExt != null) {
                File d = new File(appExt, "脚本");
                if (ensure(d)) {
                    DIR_CACHE = d;
                    return d;
                }
            }
        } catch (Throwable ignore) {
        }
        try {

            File g = com.cathelperawa.owo.app.core.DataDir.global();
            File d = new File(g == null
                    ? new File(QQEnv.appContext().getFilesDir(), "cathelper/scripts")
                    : g, DIR);
            ensure(d);
            DIR_CACHE = d;
            return d;
        } catch (Throwable t) {
            return null;
        }
    }

    private static volatile File DIR_CACHE;

    private static boolean ensure(File d) {
        try {
            if (!d.exists() && !d.mkdirs()) return false;
            if (!d.isDirectory()) return false;
            File probe = new File(d, ".probe");
            if (probe.createNewFile()) {
                probe.delete();
                return true;
            }
            return probe.exists();
        } catch (Throwable t) {
            return false;
        }
    }

    public static File dataDir(String name) {
        try {

            File pkg = packageDir(name);
            if (pkg != null) {
                File dd = new File(pkg, "data");
                if (!dd.exists()) dd.mkdirs();
                return dd;
            }
            File d = dir();
            if (d == null) return null;
            File dd = new File(new File(d, "data"), name);
            if (!dd.exists()) dd.mkdirs();
            return dd;
        } catch (Throwable t) {
            return null;
        }
    }

    public static final String KEY_EXTRA_DIR = "script_extra_dir";

    public static String extraDirPath() {
        try {
            Object v = com.cathelperawa.owo.app.core.ConfigStore
                    .getString(KEY_EXTRA_DIR);
            return v == null ? "" : String.valueOf(v);
        } catch (Throwable t) {
            return "";
        }
    }

    public static void setExtraDirPath(String path) {
        try {
            com.cathelperawa.owo.app.core.ConfigStore
                    .setString(KEY_EXTRA_DIR, path == null ? "" : path.trim());
        } catch (Throwable ignore) {
        }
    }

    public static boolean fromExtraDir(File f) {
        try {
            String p = extraDirPath();
            if (p.length() == 0 || f == null) return false;
            return f.getAbsolutePath().startsWith(new File(p).getAbsolutePath());
        } catch (Throwable t) {
            return false;
        }
    }

    private static java.util.List<File> searchDirs() {
        java.util.ArrayList<File> out = new java.util.ArrayList<File>();
        File d = dir();
        if (d != null) out.add(d);
        try {
            String p = extraDirPath();
            if (p != null && p.trim().length() > 0) {
                File ex = new File(p.trim());
                if (ex.isDirectory()) out.add(ex);
            }
        } catch (Throwable ignore) {
        }
        return out;
    }

    public static List<String> list() {
        List<String> out = new ArrayList<String>();
        java.util.List<File> dirs = searchDirs();
        for (int di = 0; di < dirs.size(); di++) {
            listOne(dirs.get(di), out);
        }
        java.util.Collections.sort(out);
        return out;
    }

    private static void listOne(File d, List<String> out) {
        try {
            if (d == null) return;
            File[] fs = d.listFiles();
            if (fs == null) return;
            for (int i = 0; i < fs.length; i++) {
                String n = fs[i].getName();
                boolean known = n.endsWith(".qqs") || n.endsWith(".js") || n.endsWith(".lua");
                boolean child = false;
                if (!known && fs[i].isFile()) {

                    java.util.List<String> exts = ScriptEngines.registeredSuffixes();
                    String low = n.toLowerCase();
                    for (int k = 0; k < exts.size(); k++) {
                        if (low.endsWith(exts.get(k))
                                && ScriptEngines.allowsFile(exts.get(k))) {

                            child = true;
                            break;
                        }
                    }
                }
                if (fs[i].isFile() && known && !child) {
                    int dot = n.lastIndexOf('.');
                    if (!out.contains(n.substring(0, dot))) out.add(n.substring(0, dot));
                } else if (fs[i].isDirectory()) {

                    boolean pkg = false;
                    java.util.List<String> exts2 = ScriptEngines.knownSuffixes();
                    for (int k = 0; k < exts2.size(); k++) {
                        String ext2 = exts2.get(k);
                        if (!ScriptEngines.allowsDir(ext2)) continue;
                        if (new File(fs[i], "main" + ext2).isFile()) { pkg = true; break; }
                    }
                    if (pkg && !out.contains(n)) out.add(n);
                }
            }
        } catch (Throwable t) {
            HookLog.w("脚本列表读取失败 " + t);
        }
    }

    public static File writeExample() {
        try {
            File d = dir();
            if (d == null) return null;
            File f = new File(d, "示例-骰子.qqs");

            if (f.exists()) {
                String old = "";
                try {
                    old = read(f);
                } catch (Throwable ignore) {
                }
                if (old.contains("ctx.route")) return f;
                f.delete();
            }
            String code =
                    "// capability: send\n"
                    + "// 最简单的一个命令脚本：群里发「骰子」就回一个数\n"
                    + "// 保存后到脚本页点「启用」，然后在群里试试\n"
                    + "\n"
                    + "ctx.route(\"dice\", new String[]{\"骰子\", \"dice\"}, new ScriptHandler() {\n"
                    + "    public void handle(ScriptMsg m, String arg) {\n"
                    + "        int n = (int) (Math.random() * 6) + 1;\n"
                    + "        ctx.sendText(m.kind(), m.peer, \"点数 \" + n);\n"
                    + "        ctx.log(\"掷了一下：\" + n);\n"
                    + "    }\n"
                    + "});\n"
                    + "\n"
                    + "ctx.log(\"骰子脚本已就绪\");\n";
            java.io.FileOutputStream fos = new java.io.FileOutputStream(f);
            fos.write(code.getBytes("UTF-8"));
            fos.close();

            File toolFile = new File(d, "示例-天气工具.qqs");
            boolean needTool = true;
            if (toolFile.exists()) {
                try {
                    needTool = !read(toolFile).contains("ctx.tool");
                } catch (Throwable ignore) {
                }
            }
            if (needTool) {
                String toolCode =
                        "// 给 AI 加一个工具：AI 需要在对话里调它才能查到天气\n"
                        + "import org.json.JSONObject;\n"
                        + "\n"
                        + "// handler 接口规定返回 void，结果用 out.set(...) 交出去\n"
                        + "//   结果用 out.set(...) 交出去\n"
                        + "ctx.tool(\"get_weather\",\n"
                        + "    \"查天气。用户问某个城市今天天气、要不要带伞时用。\"\n"
                        + "    + \"参数是 JSON，形如 {\\\"city\\\":\\\"北京\\\"}\",\n"
                        + "    new ScriptToolHandler() {\n"
                        + "        public void call(String args, ScriptToolResult out) {\n"
                        + "            String city = \"北京\";\n"
                        + "            try {\n"
                        + "                city = new JSONObject(args).optString(\"city\", \"北京\");\n"
                        + "            } catch (Throwable ignore) {\n"
                        + "            }\n"
                        + "            // 这里演示用固定值。真要接真实天气，可以在脚本里\n"
                        + "            // import com.cathelperawa.owo.app.api.NetApi 自己请求\n"
                        + "            int t = 20 + (int) (Math.random() * 10);\n"
                        + "            out.set(city + \" 今天 \" + t + \"℃，晴，微风。数据来自脚本\");\n"
                        + "        }\n"
                        + "    });\n"
                        + "\n"
                        + "ctx.log(\"天气工具已注册：AI 现在能调用 get_weather\");\n";
                java.io.FileOutputStream fos2 = new java.io.FileOutputStream(toolFile);
                fos2.write(toolCode.getBytes("UTF-8"));
                fos2.close();
            }
            return f;
        } catch (Throwable t) {
            HookLog.w("示例脚本写入失败 " + t);
            return null;
        }
    }

    private static File stateFile() {
        return statePath("enabled", true);
    }

    private static void persist() {
        try {
            File f = stateFile();
            if (f == null) return;
            StringBuilder sb = new StringBuilder();
            synchronized (LOADED) {
                for (String n : LOADED.keySet()) sb.append(n).append('\n');
            }
            java.io.FileOutputStream out = new java.io.FileOutputStream(f);
            try {
                out.write(sb.toString().getBytes("UTF-8"));
            } finally {
                out.close();
            }
        } catch (Throwable t) {
            HookLog.w("保存脚本启用状态失败 " + t);
        }
    }

    private static final java.util.concurrent.ConcurrentHashMap<String, Object> LOAD_LOCKS =
            new java.util.concurrent.ConcurrentHashMap<String, Object>();

    private static Object loadLockOf(String name) {
        Object o = LOAD_LOCKS.get(name);
        if (o == null) {
            Object nw = new Object();
            o = LOAD_LOCKS.putIfAbsent(name, nw);
            if (o == null) o = nw;
        }
        return o;
    }

    private static void dumpBshError(String name, Throwable t) {
        try {
            java.io.File d = com.cathelperawa.owo.app.core.DataDir.global();
            if (d == null) return;
            if (!d.exists()) d.mkdirs();
            java.io.File f = new java.io.File(d, "script_error.txt");
            if (f.length() > 64 * 1024) f.delete();
            StringBuilder sb = new StringBuilder();
            sb.append("==== ").append(new java.text.SimpleDateFormat("HH:mm:ss")
                    .format(new java.util.Date())).append(" ").append(name).append(" ====\n");
            sb.append("class=").append(t.getClass().getName()).append("\n");
            try {
                java.lang.reflect.Field fl = t.getClass().getField("errorLineNumber");
                sb.append("line=").append(fl.get(t)).append("\n");
            } catch (Throwable ignore) {
                sb.append("line=?\n");
            }
            try {
                java.lang.reflect.Method m = t.getClass().getMethod("getTarget");
                Object tv = m.invoke(t);
                sb.append("target=").append(tv == null ? "null" : tv.toString()).append("\n");
            } catch (Throwable ignore) {
            }
            try {
                java.lang.reflect.Method m2 = t.getClass().getMethod("getErrorText");
                sb.append("text=").append(m2.invoke(t)).append("\n");
            } catch (Throwable ignore) {
            }
            try {
                StackTraceElement[] st = t.getStackTrace();
                int lim = Math.min(st.length, 12);
                for (int i = 0; i < lim; i++) sb.append("  at ").append(st[i]).append("\n");
            } catch (Throwable ignore) {
            }
            sb.append("\n");
            java.io.FileOutputStream fo = new java.io.FileOutputStream(f, true);
            fo.write(sb.toString().getBytes("UTF-8"));
            fo.flush();
            fo.close();
        } catch (Throwable ignore) {
        }
    }

    public static String safeMsg(Throwable t) {
        if (t == null) return "未知错误";
        String cn = t.getClass().getName();
        try {
            if (cn.startsWith("bsh.")) {
                String line = "?";
                try {
                    java.lang.reflect.Field f = t.getClass().getField("errorLineNumber");
                    Object v = f.get(t);
                    if (v != null) line = String.valueOf(v);
                } catch (Throwable ignore) {
                }
                if (cn.contains("TargetError")) {

                    return "脚本调用了不存在的方法或类（第 " + line + " 行）"
                            + " —— 多半是宿主还没这个 API，重启 TIM 或升级模块";
                }
                if (cn.contains("ParseException")) {
                    return "脚本语法错误（第 " + line + " 行附近）"
                            + " —— 注意 BeanShell 不支持泛型 <T>";
                }
                if (cn.contains("EvalError")) {
                    return "脚本运行出错（第 " + line + " 行）";
                }
                return "脚本出错（第 " + line + " 行）：" + cn;
            }
            String m = t.getClass().getSimpleName();
            String msg = t.getMessage();
            if (msg != null && msg.length() > 0) {
                m = m + ": " + (msg.length() > 200 ? msg.substring(0, 200) : msg);
            }
            return m;
        } catch (Throwable ignore) {
            return cn;
        }
    }

    public static File stateDir() {
        try {
            File d = dir();
            if (d == null) return null;
            File sd = new File(d, ".state");
            if (!sd.exists()) sd.mkdirs();
            return sd;
        } catch (Throwable t) {
            return null;
        }
    }

    private static void migrateStateFiles() {
        try {
            File d = dir();
            File sd = stateDir();
            if (d == null || sd == null) return;
            String[] names = new String[]{"wanted", "enabled", "devmode"};
            for (int i = 0; i < names.length; i++) {
                File old = new File(d, "." + names[i]);
                File now = new File(sd, names[i]);
                if (!old.exists()) continue;
                if (now.exists()) {

                    old.delete();
                    continue;
                }
                byte[] buf = new byte[(int) Math.min(old.length(), 64 * 1024)];
                java.io.FileInputStream in = new java.io.FileInputStream(old);
                int n;
                try {
                    n = in.read(buf);
                } finally {
                    in.close();
                }
                java.io.FileOutputStream out = new java.io.FileOutputStream(now);
                try {
                    out.write(buf, 0, Math.max(0, n));
                } finally {
                    out.close();
                }
                if (now.exists() && old.delete()) {
                    HookLog.i("[脚本] 状态文件 " + old.getName() + " 已迁到 .state/");
                }
            }
        } catch (Throwable t) {
            HookLog.w("[脚本] 状态文件迁移失败（不影响运行）" + t);
        }
    }

    private static File statePath(String fileName, boolean forWrite) {
        try {
            File sd = stateDir();
            if (sd != null) {
                File f = new File(sd, fileName);
                if (forWrite) return f;
                if (f.exists()) return f;
            }
            File d = dir();
            if (d == null) return null;
            File old = new File(d, "." + fileName);
            return old.exists() ? old : (sd != null ? new File(sd, fileName) : old);
        } catch (Throwable t) {
            return null;
        }
    }

    private static File wantedFile() {
        return statePath("wanted", true);
    }

    public static List<String> loadWanted() {
        List<String> out = new ArrayList<String>();
        try {
            File f = wantedFile();
            if (f == null || !f.exists()) return out;
            String s = read(f);
            if (s == null) return out;
            String[] lines = s.split("\n");
            for (int i = 0; i < lines.length; i++) {
                String n = lines[i].trim();
                if (n.length() > 0) out.add(n);
            }
        } catch (Throwable t) {
            HookLog.w("读 .wanted 失败 " + t);
        }
        return out;
    }

    private static void markWanted(String name, boolean on) {
        try {
            File f = wantedFile();
            if (f == null) return;
            List<String> list = loadWanted();
            if (on) {
                if (!list.contains(name)) list.add(name);
            } else {
                list.remove(name);
            }
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < list.size(); i++) sb.append(list.get(i)).append("\n");
            java.io.FileOutputStream fo = new java.io.FileOutputStream(f);
            fo.write(sb.toString().getBytes("UTF-8"));
            fo.flush();
            fo.close();
        } catch (Throwable t) {
            HookLog.w("写 .wanted 失败 " + t);
        }
    }

    public static List<String> loadEnabled() {
        List<String> out = new ArrayList<String>();
        try {
            File f = stateFile();
            if (f == null || !f.exists()) return out;
            String s = read(f);
            String[] lines = s.split("\n");
            for (int i = 0; i < lines.length; i++) {
                String n = lines[i].trim();
                if (n.length() > 0) out.add(n);
            }
        } catch (Throwable t) {
            HookLog.w("读取脚本启用状态失败 " + t);
        }
        return out;
    }

    public static void scanEngineDeclarations() {
        try {
            File d = dir();
            if (d == null) return;
            File[] fs = d.listFiles();
            if (fs == null) return;
            for (int i = 0; i < fs.length; i++) {
                String nm = fs[i].getName();
                if (fs[i].isDirectory()) {

                    String base = nm;
                    ScriptMeta m = metaOf(base);
                    if (m != null && m.engines != null && m.engines.length() > 0) {
                        applyEngines(base, m.engines);
                    }
                    continue;
                }
                boolean known = false;
                java.util.List<String> exts = ScriptEngines.knownSuffixes();
                for (int k = 0; k < exts.size(); k++) {
                    if (nm.endsWith(exts.get(k))) { known = true; break; }
                }
                if (!known) continue;
                int dot = nm.lastIndexOf('.');
                if (dot <= 0) continue;
                String base = nm.substring(0, dot);
                ScriptMeta m = metaOf(base);
                if (m != null && m.engines != null && m.engines.length() > 0) {
                    applyEngines(base, m.engines);
                }
            }
        } catch (Throwable t) {
            HookLog.w("[引擎] 声明扫描失败 " + t);
        }
    }

    private static void applyEngines(String provider, String decl) {
        String[] parts = decl.trim().split("\\s+");
        for (int i = 0; i < parts.length; i++) {
            String p = parts[i];
            if (p == null || p.length() == 0) continue;
            String forms = "";
            int lb = p.indexOf('[');
            int rb = p.indexOf(']');
            if (lb > 0 && rb > lb) {
                forms = p.substring(lb + 1, rb);
                p = p.substring(0, lb);
            }
            ScriptEngines.registerPlaceholder(p, provider, forms);
        }
    }

    public static void bootDelayed() {
        new Thread(new Runnable() {
            @Override public void run() {
                try {
                    Thread.sleep(10000);
                } catch (Throwable ignore) {
                }
                try {

                    if (com.cathelperawa.owo.app.core.SafeMode.active()) {
                        HookLog.w("[脚本] 安全模式生效中，跳过全部脚本加载"
                                + "（" + com.cathelperawa.owo.app.core.SafeMode.why() + "）");
                        return;
                    }

                    if (consumeSafeBoot()) {
                        HookLog.w("[脚本] 已跳过本次全部脚本加载（手动安全启动）");
                        return;
                    }

                    migrateStateFiles();

                    try {
                        scanEngineDeclarations();
                    } catch (Throwable ignore) {
                    }

                    if (BootGuard.beginLoad()) {

                        HookLog.w("[启动守卫] 连续两次启动未完成 → 本次不加载脚本（模块本身照常）");
                        persistAllDisabled();
                        BootGuard.markRecovered();
                        try {
                            com.cathelperawa.owo.app.bridge.CatToast.show(
                                    "上次脚本可能把启动卡住了：本次已跳过全部脚本，其他功能不受影响");
                        } catch (Throwable ignore) {
                        }
                        return;
                    }

                    List<String> names = list();
                    List<String> wanted = loadWanted();
                    int ok = 0, total = 0;
                    for (int i = 0; i < names.size(); i++) {

                        if (!isAutoLoad(names.get(i)) && !wanted.contains(names.get(i))) continue;
                        if (ModHealth.banned(names.get(i))) {
                            HookLog.w("[脚本] 跳过已熔断的 " + names.get(i));
                            continue;
                        }
                        total++;
                        if (enable(names.get(i)).ok) ok++;
                    }
                    if (total > 0) HookLog.i("[脚本] 开机自动加载 " + ok + "/" + total + " 个");

                    try {
                        ScriptSendHook.install();
                    } catch (Throwable ignore) {
                    }

                    try {
                        com.cathelperawa.owo.app.core.Skin.prune(enabledNames());
                    } catch (Throwable ignore) {
                    }

                    BootGuard.markLoaded();

                    try {
                        com.cathelperawa.owo.app.core.ConfigOverride.prune(enabledNames());
                    } catch (Throwable ignore) {
                    }
                } catch (Throwable t) {
                    HookLog.e("脚本开机恢复失败", t);
                }
            }
        }, "cat-script-boot").start();
    }

    public static Result enable(String name) {
        long t0 = System.currentTimeMillis();

        try {
            if (com.cathelperawa.owo.app.core.SafeMode.active()) {
                return new Result(name, false,
                        "模块处于安全模式，本次启动不加载任何脚本（重启并解除后再试）", 0);
            }
        } catch (Throwable ignore) {
        }
        File f = entryOf(name);
        if (f == null || !f.exists()) {
            return new Result(name, false, "脚本不存在", 0);
        }
        String code;
        try {
            code = read(f);
        } catch (Throwable t) {
            return new Result(name, false, "读取失败：" + t, System.currentTimeMillis() - t0);
        }

        ScriptMeta meta = ScriptMeta.parse(code);

        try {

            setExternal(name, meta.isExternal() || fromExtraDir(f));
            if (meta.isExternal()) {
                HookLog.w("[脚本] " + name + " 标记为外部来源 → 隔离档运行"
                        + "（无 exec / method / patch / configForce / 内核 API）");
            }
        } catch (Throwable ignore) {
        }
        if (!meta.apiOk()) {
            HookLog.w("脚本[" + name + "] 拒绝加载：" + meta.apiError());
            return new Result(name, false, meta.apiError(), System.currentTimeMillis() - t0);
        }

        String depErr = dependenciesOk(name);
        if (depErr != null) {
            return new Result(name, false, depErr, 0);
        }

        if (ModHealth.banned(name)) {
            return new Result(name, false,
                    "连续失败 " + ModHealth.crashes(name) + " 次已熔断。"
                            + "修好脚本后在卡片上点「重置失败计数」再启用", 0);
        }

        int level = ModLevel.of(name);
        if (ScriptTrust.full()) {

            level = ModLevel.AI_ONLY == level ? level : level;
        }
        if (!ScriptTrust.full() && ModLevel.isKernel(level) && !ModLevel.kernelAllowed()) {
            return new Result(name, false,
                    "内核脚本已被总开关关闭（设置里打开「允许内核脚本」后再试）", 0);
        }

        if (!ScriptTrust.full() && ModLevel.isKernel(level)
                && !com.cathelperawa.owo.app.core.KernelWhitelist.allowed(name)) {
            return new Result(name, false,
                    "这个脚本还没被授权使用内核能力。展开内核分区，点它卡片上的「授予内核权限」", 0);
        }

        try {
            java.util.List<String> risks = ScriptRisk.scan(code);
            if (!risks.isEmpty() && !ScriptRisk.confirmed(name, f.lastModified())) {
                return new Result(name, false,
                        "需要确认风险后才能启用：" + ScriptRisk.describe(risks)
                                + "（点卡片上的「确认风险并启用」）", 0);
            }
        } catch (Throwable ignore) {
        }

        try {
            ScriptExec.reset(name);
        } catch (Throwable ignore) {
        }

        Set<String> caps = parseCaps(code);
        int scope = ScriptScope.parse(code);
        File dataDir = dataDir(name);

        synchronized (loadLockOf(name)) {

            synchronized (LOADED) {
                disableLocked(name);
            }
            try {
                ScriptEngine it = ScriptEngines.create(f, code);
                ScriptApiImpl api = new ScriptApiImpl(name, caps, dataDir, scope);
                api.setEngine(it);
                it.set("ctx", api);

                ScriptEnv.apply(it);

                ScriptEnv.bindGlobals(it, api);

                bindLoader(it, name, f);

                bindVars(it, name);

                if (it instanceof BshEngine) {
                    ((BshEngine) it).eval(code, f == null ? null : f.getName());
                } else {
                    it.eval(code);
                }

                synchronized (LOADED) {
                    LOADED.put(name, new Loaded(name, it, caps, scope, dataDir));
                }
                ScriptRouter.install();
                ScriptFloat.hookOnce();

                ModHealth.onSuccess(name);
                markWanted(name, true);

                try {
                    com.cathelperawa.owo.app.core.SettingsInject.rebuildNow();
                } catch (Throwable ignore) {
                }

                try {
                    PatchPoint.emitAsync("host.script.load", name, name);
                } catch (Throwable ignore) {
                }
                int routes = RouteRegistry.countOf(name);
                int tools = ScriptToolRegistry.countOf(name);
                persist();
                long cost = System.currentTimeMillis() - t0;
                HookLog.i("脚本[" + name + "] 已启用，命令 " + routes
                        + " 条 / AI 工具 " + tools + " 个，耗时 " + cost + "ms");
                return new Result(name, true,
                        "已启用 · " + routes + " 条命令"
                                + (tools > 0 ? " · " + tools + " 个 AI 工具" : ""),
                        cost);
            } catch (Throwable t) {

                RouteRegistry.removeByScript(name);
                ScriptToolRegistry.removeByScript(name);
                synchronized (LOADED) {
                    LOADED.remove(name);
                }
                String err = safeMsg(t);
                dumpBshError(name, t);
                ModHealth.record(name, err);
                HookLog.e("脚本[" + name + "] 启用失败：" + err, null);
                return new Result(name, false, err,
                        System.currentTimeMillis() - t0);
            }
        }
    }

    public static Result disable(String name) {
        java.util.List<String> deps = dependents(name);

        for (int i = 0; i < deps.size(); i++) {
            String d = deps.get(i);
            if (isEnabled(d)) {
                synchronized (LOADED) {
                    disableLocked(d);
                }
                HookLog.w("[脚本] " + d + " 依赖 " + name + "，已一并停用");
            }
        }
        String extraMsg = deps.isEmpty() ? ""
                : "（同时停用了依赖它的 " + deps.size() + " 个脚本）";
        synchronized (LOADED) {
            int n = disableLocked(name);
            persist();

            try {
                com.cathelperawa.owo.app.core.SettingsInject.rebuildNow();
            } catch (Throwable ignore) {
            }
            return new Result(name, true,
                    (n > 0 ? ("已停用 · 摘掉 " + n + " 条命令") : "已停用") + extraMsg, 0);
        }
    }

    private static int disableLocked(String name) {
        markWanted(name, false);

        Loaded old = LOADED.get(name);
        if (old != null && old.interp != null) {
            try {
                Object api = old.interp.get("ctx");
                if (api instanceof ScriptApiImpl) ((ScriptApiImpl) api).fireStop();
            } catch (Throwable ignore) {
            }
        }

        try {
            com.cathelperawa.owo.app.core.Skin.removeByScript(name);
        } catch (Throwable ignore) {
        }
        try {
            com.cathelperawa.owo.app.ui.ScriptScreen.removeByScript(name);
            com.cathelperawa.owo.app.ui.ScriptScreen.removePagesByScript(name);
            com.cathelperawa.owo.app.script.ActivityBus.removeByScript(name);
            com.cathelperawa.owo.app.script.ScriptWindow.removeByScript(name);
        } catch (Throwable ignore) {
        }
        try {
            com.cathelperawa.owo.app.core.ScriptSetting.removeByScript(name);
        } catch (Throwable ignore) {
        }

        try {
            ScriptEngines.removeByScript(name);
            scanEngineDeclarations();
        } catch (Throwable ignore) {
        }
        try {
            com.cathelperawa.owo.app.ui.ScriptOverlay.removeByScript(name);
        } catch (Throwable ignore) {
        }
        try {
            com.cathelperawa.owo.app.core.ConfigOverride.removeByScript(name);
        } catch (Throwable ignore) {
        }
        try {
            ScriptTimer.clearScript(name);
        } catch (Throwable ignore) {
        }
        try {
            StrategyRegistry.removeByScript(name);
        } catch (Throwable ignore) {
        }
        try {
            PatchPoint.removeByScript(name);
        } catch (Throwable ignore) {
        }
        try {
            MethodHook.removeByScript(name);
        } catch (Throwable ignore) {
        }
        ScriptMessageRegistry.removeByScript(name);
        LOADED.remove(name);
        ScriptToolRegistry.removeByScript(name);
        ScriptFloatRegistry.removeByScript(name);
        ScriptHub.removeByScript(name);
        ScriptMenuRegistry.removeByScript(name);
        ScriptTagRegistry.removeByScript(name);
        ScriptPanelRegistry.removeByScript(name);
        ScriptBusRegistry.removeByScript(name);

        try {
            PatchPoint.emitAsync("host.script.stop", name, name);
        } catch (Throwable ignore) {
        }

        try {
            ScriptExec.shutdown(name);
        } catch (Throwable ignore) {
        }
        int removed = RouteRegistry.removeByScript(name);

        try {
            int left = RouteRegistry.countOf(name)
                    + ScriptToolRegistry.countOf(name)
                    + PatchPoint.countOf(name);
            if (left > 0) {
                HookLog.w("[脚本] " + name + " 停用后仍有 " + left + " 项注册残留，强制再清一次");
                RouteRegistry.removeByScript(name);
                ScriptToolRegistry.removeByScript(name);
                PatchPoint.removeByScript(name);
            }
        } catch (Throwable ignore) {
        }
        return removed;
    }

    public static String evalIn(String name, String code) {
        if (code == null || code.trim().length() == 0) return "（没有输入）";
        final Loaded l;
        synchronized (LOADED) {
            l = LOADED.get(name);
        }
        if (l == null || l.interp == null) return "这个脚本没在运行（先启用它）";
        final String[] out = new String[]{""};
        boolean ok = ScriptExec.call(name, new Runnable() {
            @Override public void run() {
                try {
                    Object r = l.interp.eval(code);
                    out[0] = (r == null) ? "（无返回值）" : String.valueOf(r);
                } catch (Throwable t) {
                    String m = String.valueOf(t);
                    out[0] = "错误：" + m;
                    try {
                        ScriptFiles.append(l.dataDir, "console.txt",
                                "[" + new java.text.SimpleDateFormat("MM-dd HH:mm:ss")
                                        .format(new java.util.Date()) + "] " + m + "\n");
                    } catch (Throwable ignore) {
                    }
                }
            }
        }, 8000);
        if (!ok) return "执行超时 / 被丢弃（脚本可能卡死，点「重载」恢复）";
        return out[0];
    }

    public static String errorText(String name) {
        try {
            File d = dataDir(name);
            if (d == null) return "";
            File f = new File(d, "error.txt");
            if (!f.exists() || f.length() == 0) return "";
            java.io.RandomAccessFile r = new java.io.RandomAccessFile(f, "r");
            try {
                long len = Math.min(r.length(), 8192);
                r.seek(Math.max(0, r.length() - len));
                byte[] b = new byte[(int) len];
                r.readFully(b);
                return new String(b, "UTF-8");
            } finally {
                try { r.close(); } catch (Throwable ignore) {}
            }
        } catch (Throwable t) {
            return "";
        }
    }

    public static boolean clearError(String name) {
        try {
            File d = dataDir(name);
            if (d == null) return false;
            File f = new File(d, "error.txt");
            return !f.exists() || f.delete();
        } catch (Throwable t) {
            return false;
        }
    }

    public static String apiDoc() {
        StringBuilder sb = new StringBuilder();
        try {
            java.lang.reflect.Method[] ms = ScriptApi.class.getDeclaredMethods();
            java.util.Arrays.sort(ms, new java.util.Comparator<java.lang.reflect.Method>() {
                @Override public int compare(java.lang.reflect.Method a, java.lang.reflect.Method b) {
                    return a.getName().compareTo(b.getName());
                }
            });

            String[][] groups = new String[][]{
                    {"发消息", "send"},
                    {"文件", "file"},
                    {"网络", "http"},
                    {"注册 / 回调", "route"},
                    {"注册 / 回调", "tool"},
                    {"注册 / 回调", "menu"},
                    {"注册 / 回调", "tag"},
                    {"注册 / 回调", "screen"},
                    {"注册 / 回调", "dialog"},
                    {"注册 / 回调", "overlay"},
                    {"注册 / 回调", "panel"},
                    {"注册 / 回调", "float"},
                    {"注册 / 回调", "provide"},
                    {"注册 / 回调", "onMessage"},
                    {"注册 / 回调", "onStop"},
                    {"注册 / 回调", "strategy"},
                    {"底层", "method"},
                    {"底层", "patch"},
                    {"底层", "exec"},

                    {"载入类", "loadDex"},
                    {"载入类", "loadDexClass"},
                    {"载入类", "loadJar"},
                    {"载入类", "defineClass"},
                    {"载入类", "probe"},
                    {"载入类", "tryCall"},
                    {"底层", "findClasses"},
                    {"底层", "methodsOf"},
                    {"底层", "fieldsOf"},
                    {"底层", "ctorsOf"},
                    {"底层", "activity"},
                    {"底层", "context"},
                    {"配置 / 杂项", "get"},
                    {"配置 / 杂项", "set"},
                    {"配置 / 杂项", "can"},
                    {"配置 / 杂项", "toast"},
                    {"配置 / 杂项", "log"},
                    {"配置 / 杂项", "dataDir"},
                    {"配置 / 杂项", "config"},
                    {"配置 / 杂项", "theme"},
                    {"配置 / 杂项", "style"},
                    {"配置 / 杂项", "text"},
                    {"配置 / 杂项", "icon"},
                    {"配置 / 杂项", "bg"},
                    {"配置 / 杂项", "hide"},
                    {"配置 / 杂项", "setting"},
                    {"配置 / 杂项", "skinReset"}
            };

            java.util.LinkedHashMap<String, java.util.List<String>> map =
                    new java.util.LinkedHashMap<String, java.util.List<String>>();
            java.util.List<String> others = new java.util.ArrayList<String>();
            for (int i = 0; i < ms.length; i++) {
                java.lang.reflect.Method m = ms[i];
                String name = m.getName();
                String group = null;
                for (int g = 0; g < groups.length; g++) {
                    if (name.startsWith(groups[g][1])) { group = groups[g][0]; break; }
                }
                if (group == null) { others.add(name); continue; }
                java.util.List<String> list = map.get(group);
                if (list == null) { list = new java.util.ArrayList<String>(); map.put(group, list); }
                if (!list.contains(name)) list.add(name);
            }

            sb.append("【ctx API 速查】").append(ms.length).append(" 个方法，按用途分组\n\n");
            for (java.util.Map.Entry<String, java.util.List<String>> e : map.entrySet()) {
                sb.append("【").append(e.getKey()).append("】\n");
                java.util.List<String> list = e.getValue();
                for (int i = 0; i < list.size(); i++) {
                    sb.append("  ").append(list.get(i));
                    if (i < list.size() - 1) sb.append(" / ");
                    if (i % 3 == 2 || i == list.size() - 1) sb.append("\n");
                }
                sb.append("\n");
            }
            if (!others.isEmpty()) {
                sb.append("【其它】\n  ");
                for (int i = 0; i < others.size(); i++) {
                    sb.append(others.get(i));
                    if (i < others.size() - 1) sb.append(" / ");
                    if (i % 3 == 2 || i == others.size() - 1) sb.append("\n");
                }
                sb.append("\n");
            }
            sb.append("── 模块 API（465 个方法，31 个分域）──\n");
            sb.append("  CatApi / AiApi / NetApi / IoApi / MsgApi / VoiceApi / GroupManageApi ...\n");
            sb.append("  用法：import com.cathelperawa.owo.app.api.*; 然后 CatApi.sendText(...)\n");
            sb.append("\n完整签名：在控制台里敲  ctx.getClass()  或看 API速查.md\n");
        } catch (Throwable t) {
            sb.append("生成失败：").append(t);
        }
        return sb.toString();
    }

    public static List<String> childrenOf(String provider) {
        List<String> out = new ArrayList<String>();
        try {
            java.util.List<String> exts = ScriptEngines.suffixesProvidedBy(provider);
            if (exts.isEmpty()) return out;
            File d = dir();
            if (d == null) return out;
            File[] fs = d.listFiles();
            if (fs == null) return out;
            for (int i = 0; i < fs.length; i++) {
                String n = fs[i].getName();
                if (fs[i].isFile()) {
                    String low = n.toLowerCase();
                    for (int k = 0; k < exts.size(); k++) {
                        if (low.endsWith(exts.get(k))) {
                            out.add(n.substring(0, n.length() - exts.get(k).length()));
                            break;
                        }
                    }
                } else if (fs[i].isDirectory()) {
                    for (int k = 0; k < exts.size(); k++) {
                        if (new File(fs[i], "main" + exts.get(k)).isFile()) {
                            out.add(n);
                            break;
                        }
                    }
                }
            }
            java.util.Collections.sort(out);
        } catch (Throwable t) {
            HookLog.w("子脚本扫描失败 " + t);
        }
        return out;
    }

    public static boolean isEnabled(String name) {
        synchronized (LOADED) {
            return LOADED.containsKey(name);
        }
    }

    public static List<String> enabledNames() {
        synchronized (LOADED) {
            return new ArrayList<String>(LOADED.keySet());
        }
    }

    public static int disableAll() {
        synchronized (LOADED) {
            List<String> names = new ArrayList<String>(LOADED.keySet());
            int n = 0;
            for (int i = 0; i < names.size(); i++) n += disableLocked(names.get(i));
            ScriptRouter.uninstall();
            return n;
        }
    }

    public static int reloadAll() {
        List<String> names = enabledNames();
        int ok = 0;
        for (int i = 0; i < names.size(); i++) {
            Result r = enable(names.get(i));
            if (r.ok) ok++;
        }
        return ok;
    }

    public static Result dryRun(String name) {
        long t0 = System.currentTimeMillis();
        File f = entryOf(name);
        if (f == null || !f.exists()) {
            return new Result(name, false, "脚本不存在", 0);
        }
        String code;
        try {
            code = read(f);
        } catch (Throwable t) {
            return new Result(name, false, "读取失败：" + t, 0);
        }
        MockRoutes.clear();
        try {
            ScriptEngine it = ScriptEngines.create(f, code);
            MockScriptApi mock = new MockScriptApi(name);
            it.set("ctx", mock);

            ScriptEnv.apply(it);
            ScriptEnv.bindGlobals(it, mock);
            it.eval(code);
            long cost = System.currentTimeMillis() - t0;
            List<String> rs = MockRoutes.take();
            HookLog.i("脚本[" + name + "] 试跑通过 " + cost + "ms");
            return new Result(name, true,
                    "试跑通过" + (rs.isEmpty() ? "" : " · 会注册 " + rs.size() + " 条命令"), cost);
        } catch (Throwable t) {
            long cost = System.currentTimeMillis() - t0;
            HookLog.e("脚本[" + name + "] 试跑失败", t);
            return new Result(name, false, String.valueOf(t), cost);
        }
    }

    static Set<String> parseCaps(String code) {
        Set<String> caps = new HashSet<String>();
        if (code == null) return caps;
        String head = code.length() > 2000 ? code.substring(0, 2000) : code;
        int i = head.indexOf("capability:");
        if (i < 0) return caps;
        int end = i + "capability:".length();
        int lineEnd = head.indexOf('\n', end);
        if (lineEnd < 0) lineEnd = head.length();
        String body = head.substring(end, lineEnd);

        body = body.replace("*/", "").replace("//", "").trim();
        String[] parts = body.split(",");
        for (int p = 0; p < parts.length; p++) {
            String c = parts[p].trim();
            if (c.length() > 0) caps.add(c);
        }
        return caps;
    }

    public static boolean isPackage(String name) {
        File d = packageDir(name);
        return d != null && d.isDirectory();
    }

    public static File packageDir(String name) {
        try {
            File root = dir();
            if (root == null || name == null || name.length() == 0) return null;
            String n = name.endsWith(".qqs") ? name.substring(0, name.length() - 4) : name;
            File d = new File(root, n);
            if (!d.isDirectory()) return null;
            if (new File(d, "main.qqs").isFile()) return d;
            if (new File(d, "main.java").isFile()) return d;
            if (new File(d, "main.js").isFile()) return d;
            return null;
        } catch (Throwable t) {
            return null;
        }
    }

    public static File entryOf(String name) {
        File pkg = packageDir(name);
        if (pkg != null) {

            java.util.List<String> exts = ScriptEngines.knownSuffixes();
            File first = null;
            int hits = 0;
            for (int k = 0; k < exts.size(); k++) {
                File f = new File(pkg, "main" + exts.get(k));
                if (f.isFile()) {
                    if (first == null) first = f;
                    hits++;
                }
            }
            if (hits > 1) {
                HookLog.w("[脚本] 包脚本「" + name + "」有 " + hits + " 个入口，用 "
                        + first.getName() + "（其它后缀忽略）");
            }
            if (first != null) return first;
        }
        return fileOf(name);
    }

    public static int packageFileCount(String name) {
        File pkg = packageDir(name);
        if (pkg == null) return 0;
        return countFiles(pkg);
    }

    private static int countFiles(File d) {
        int n = 0;
        File[] fs = d.listFiles();
        if (fs == null) return 0;
        for (int i = 0; i < fs.length; i++) {
            if (fs[i].isDirectory()) n += countFiles(fs[i]);
            else n++;
        }
        return n;
    }

    private static File infoPropOf(String name) {
        try {
            File pkg = packageDir(name);
            if (pkg == null) return null;
            File f = new File(pkg, "info.prop");
            return f.isFile() ? f : null;
        } catch (Throwable t) {
            return null;
        }
    }

    private static File fileOf(String name) {
        try {
            File d = dir();
            if (d == null || name == null) return null;

            java.util.List<String> exts = ScriptEngines.knownSuffixes();
            String low = name.toLowerCase();
            for (int k = 0; k < exts.size(); k++) {
                if (low.endsWith(exts.get(k))) {
                    name = name.substring(0, name.length() - exts.get(k).length());
                    break;
                }
            }
            java.util.List<File> dirs = searchDirs();
            for (int di = 0; di < dirs.size(); di++) {
                for (int k = 0; k < exts.size(); k++) {
                    File f = new File(dirs.get(di), name + exts.get(k));
                    if (f.exists()) return f;
                }
            }
            return new File(d, name + ".qqs");
        } catch (Throwable t) {
            return null;
        }
    }

    private static void bindVars(ScriptEngine it, String script) {
        try {
            if (it.get("context") == null) {
                Object c = com.cathelperawa.owo.app.bridge.QQEnv.appContext();
                if (c != null) it.set("context", c);
            }
        } catch (Throwable ignore) {
        }
        try {
            ClassLoader cl = com.cathelperawa.owo.app.bridge.QQEnv.classLoader();
            if (cl != null) it.set("classLoader", cl);
        } catch (Throwable ignore) {
        }
        try {
            File pkg = packageDir(script);
            File root = dir();
            String path = pkg != null ? pkg.getAbsolutePath()
                    : (root == null ? "" : root.getAbsolutePath());
            it.set("pluginPath", path);
            it.set("scriptDir", path);
        } catch (Throwable ignore) {
        }
        try {
            ScriptMeta m = metaOf(script);
            it.set("pluginID", (m != null && m.id != null && m.id.length() > 0) ? m.id : script);
        } catch (Throwable ignore) {
        }
        try {
            String uin = com.cathelperawa.owo.app.bridge.QQEnv.currentUin();
            if (uin != null) it.set("myUin", uin);
        } catch (Throwable ignore) {
        }
    }

    private static void bindLoader(ScriptEngine it, String script, File self) {
        try {

            File pkg = packageDir(script);
            File base = pkg != null ? pkg : dir();
            it.set("scriptDir", base == null ? "" : base.getAbsolutePath());
            it.set("scriptFile", self == null ? "" : self.getAbsolutePath());
            Loader ld = new Loader(it, base);
            java.lang.reflect.Method m = Loader.class.getDeclaredMethod("loadJava", String.class);
            it.bindMethod("loadJava", ld, m);
            it.bindMethod("source", ld, m);
        } catch (Throwable t) {
            HookLog.w("[脚本] 分文件加载绑定失败：" + t);
        }
    }

    public static final class Loader {
        private final ScriptEngine it;
        private final File base;

        Loader(ScriptEngine it, File base) {
            this.it = it;
            this.base = base;
        }

        public void loadJava(String rel) throws Exception {
            if (rel == null || rel.trim().length() == 0) {
                throw new Exception("loadJava：路径为空");
            }
            String path = rel.trim();
            File f = new File(path);
            if (!f.isAbsolute() && base != null) {
                File direct = new File(base, path);
                if (direct.exists()) {
                    f = direct;
                } else {

                    File inLib = new File(new File(base, "lib"), path);
                    f = inLib.exists() ? inLib : direct;
                }
            }
            if (!f.exists()) throw new Exception("loadJava：找不到 " + path);
            if (f.isDirectory()) throw new Exception("loadJava：这是目录 " + path);

            String canon = f.getCanonicalPath();
            if (base != null) {
                String root = base.getCanonicalPath();
                if (!canon.equals(root) && !canon.startsWith(root + File.separator)) {
                    throw new Exception("loadJava：只能加载脚本目录内的文件（" + path + "）");
                }
            }
            if (canon.endsWith(".qqs")) {
                throw new Exception("loadJava：子文件别用 .qqs 后缀（会变成独立脚本），改用 .bsh");
            }
            HookLog.i("[脚本] loadJava " + new File(canon).getName()
                    + "（" + new File(canon).length() + "B）");
            String code = read(new File(canon));
            it.eval(code);
        }
    }

    private static String read(File f) throws Exception {
        if (MAX_BYTES > 0 && f.length() > MAX_BYTES) {
            throw new Exception("脚本太大（>" + (MAX_BYTES / 1024 / 1024) + "MB）");
        }
        BufferedReader br = new BufferedReader(new InputStreamReader(
                new FileInputStream(f), "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line).append('\n');
        }
        br.close();
        return sb.toString();
    }
}
