package com.cathelperawa.owo.app.script;

import org.json.JSONObject;

import java.io.File;

public final class ScriptRisk {

    private static final String FILE = "riskok.json";

    private ScriptRisk() {}

    public static java.util.List<String> scan(String code) {
        java.util.ArrayList<String> out = new java.util.ArrayList<String>();
        if (code == null || code.length() == 0) return out;

        has(out, code, "ctx.method(", "替换宿主方法体");
        has(out, code, "<init>", "改构造器 / 静态初始化");
        has(out, code, "<clinit>", "改静态初始化块");
        has(out, code, "ctx.patch(", "挂宿主点位（改收发链路）");
        has(out, code, "ctx.hook(", "订阅宿主事件");
        has(out, code, "configForce", "强制改写模块开关");
        has(out, code, "skinReset", "改模块外观");
        has(out, code, "setAccessible(true)", "反射访问私有成员");
        has(out, code, "getDeclaredMethod", "反射调用隐藏方法");
        has(out, code, "Runtime.getRuntime", "执行系统命令");
        has(out, code, "ProcessBuilder", "启动外部进程");
        has(out, code, "fileDelete(", "删除文件");
        has(out, code, "ctx.exec(", "执行 shell 命令");
        has(out, code, "msg.raw", "直接操作宿主消息对象");
        return out;
    }

    private static void has(java.util.List<String> out, String code, String key, String desc) {
        if (code.indexOf(key) >= 0) out.add(desc);
    }

    public static String describe(java.util.List<String> risks) {
        if (risks == null || risks.isEmpty()) return "无";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < risks.size(); i++) {
            if (i > 0) sb.append("、");
            sb.append(risks.get(i));
        }
        return sb.toString();
    }

    private static File file() {
        try {
            File sd = ScriptHost.stateDir();
            return sd == null ? null : new File(sd, FILE);
        } catch (Throwable t) {
            return null;
        }
    }

    private static JSONObject load() {
        try {
            File f = file();
            if (f == null || !f.exists()) return new JSONObject();
            java.io.FileInputStream in = new java.io.FileInputStream(f);
            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
            try {
                byte[] b = new byte[4096];
                int n;
                while ((n = in.read(b)) > 0) bos.write(b, 0, n);
            } finally {
                in.close();
            }
            return new JSONObject(new String(bos.toByteArray(), "UTF-8"));
        } catch (Throwable t) {
            return new JSONObject();
        }
    }

    private static void save(JSONObject o) {
        try {
            File f = file();
            if (f == null) return;
            java.io.FileOutputStream out = new java.io.FileOutputStream(f);
            try {
                out.write(o.toString().getBytes("UTF-8"));
            } finally {
                out.close();
            }
        } catch (Throwable ignore) {
        }
    }

    public static boolean confirmed(String name, long mtime) {
        if (name == null) return false;
        try {
            return load().optLong(name, -1L) == mtime;
        } catch (Throwable t) {
            return false;
        }
    }

    public static void confirm(String name, long mtime) {
        if (name == null) return;
        try {
            JSONObject o = load();
            o.put(name, mtime);
            save(o);
        } catch (Throwable ignore) {
        }
    }

    public static void reset(String name) {
        try {
            JSONObject o = load();
            o.remove(name);
            save(o);
        } catch (Throwable ignore) {
        }
    }
}
