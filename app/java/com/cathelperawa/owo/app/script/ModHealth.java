package com.cathelperawa.owo.app.script;

import com.cathelperawa.owo.app.core.HookLog;

import java.io.File;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

public final class ModHealth {

    public static final int LIMIT = 5;

    private static final String FILE = "_health.json";
    private static final Map<String, Integer> CACHE = new HashMap<String, Integer>();

    private ModHealth() {}

    private static File fileOf(String name) {
        try {
            File d = ScriptHost.dataDir(name);
            if (d == null) return null;
            if (!d.exists()) d.mkdirs();
            return new File(d, FILE);
        } catch (Throwable t) {
            return null;
        }
    }

    private static JSONObject load(String name) {
        JSONObject o = new JSONObject();
        try {
            File f = fileOf(name);
            if (f == null || !f.exists()) return o;
            RandomAccessFile r = new RandomAccessFile(f, "r");
            try {
                byte[] b = new byte[(int) Math.min(r.length(), 4096)];
                r.readFully(b);
                o = new JSONObject(new String(b, "UTF-8"));
            } finally {
                try { r.close(); } catch (Throwable ignore) {}
            }
        } catch (Throwable t) {

        }
        return o;
    }

    private static void save(String name, JSONObject o) {
        try {
            File f = fileOf(name);
            if (f == null) return;
            FileOutputStream fo = new FileOutputStream(f);
            fo.write(o.toString().getBytes("UTF-8"));
            fo.flush();
            fo.close();
        } catch (Throwable t) {
            HookLog.w("健康记录写入失败 " + t);
        }
    }

    public static int crashes(String name) {
        try {
            Integer c = CACHE.get(name);
            if (c != null) return c;
            int n = load(name).optInt("crashes", 0);
            CACHE.put(name, n);
            return n;
        } catch (Throwable t) {
            return 0;
        }
    }

    public static String lastReason(String name) {
        try {
            return load(name).optString("last", "");
        } catch (Throwable t) {
            return "";
        }
    }

    public static boolean banned(String name) {
        if (devMode(name)) return false;
        return crashes(name) >= LIMIT;
    }

    private static File devFile() {
        try {

            java.io.File sd = ScriptHost.stateDir();
            if (sd == null) return null;
            java.io.File f = new java.io.File(sd, "devmode");
            if (f.exists()) return f;

            java.io.File old = new java.io.File(ScriptHost.dir(), ".devmode");
            return old.exists() ? old : f;
        } catch (Throwable t) {
            return null;
        }
    }

    public static boolean devMode(String name) {
        if (name == null) return false;
        try {
            File f = devFile();
            if (f == null || !f.exists()) return false;
            java.io.BufferedReader br = new java.io.BufferedReader(
                    new java.io.InputStreamReader(new java.io.FileInputStream(f), "UTF-8"));
            try {
                String line;
                while ((line = br.readLine()) != null) {
                    if (name.equals(line.trim())) return true;
                }
            } finally {
                br.close();
            }
        } catch (Throwable ignore) {
        }
        return false;
    }

    public static boolean setDevMode(String name, boolean on) {
        if (name == null) return false;
        try {
            File f = devFile();
            if (f == null) return false;
            java.util.List<String> list = new java.util.ArrayList<String>();
            if (f.exists()) {
                java.io.BufferedReader br = new java.io.BufferedReader(
                        new java.io.InputStreamReader(new java.io.FileInputStream(f), "UTF-8"));
                try {
                    String line;
                    while ((line = br.readLine()) != null) {
                        String n = line.trim();
                        if (n.length() > 0 && !n.equals(name)) list.add(n);
                    }
                } finally {
                    br.close();
                }
            }
            if (on) list.add(name);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < list.size(); i++) sb.append(list.get(i)).append('\n');
            java.io.FileOutputStream fo = new java.io.FileOutputStream(f);
            try {
                fo.write(sb.toString().getBytes("UTF-8"));
            } finally {
                fo.close();
            }
            if (on) reset(name);
            return true;
        } catch (Throwable t) {
            HookLog.w("设置开发模式失败 " + t);
            return false;
        }
    }

    public static void record(String name, String reason) {
        try {
            JSONObject o = load(name);
            if (devMode(name)) {
                o.put("last", reason == null ? "" : abbreviate(reason, 300));
                o.put("time", System.currentTimeMillis());
                save(name, o);
                HookLog.w("脚本[" + name + "] 失败（开发模式，不计数）：" + reason);
                return;
            }
            int n = o.optInt("crashes", 0) + 1;
            o.put("crashes", n);
            o.put("last", reason == null ? "" : abbreviate(reason, 300));
            o.put("time", System.currentTimeMillis());
            save(name, o);
            CACHE.put(name, n);
            if (n >= LIMIT) {
                HookLog.w("脚本[" + name + "] 连续失败 " + n + " 次，已熔断（不再自动加载）");
            } else {
                HookLog.w("脚本[" + name + "] 失败 " + n + "/" + LIMIT + "：" + reason);
            }
        } catch (Throwable t) {
            HookLog.w("记录脚本失败出错 " + t);
        }
    }

    public static void reset(String name) {
        try {
            JSONObject o = load(name);
            o.put("crashes", 0);
            o.put("last", "");
            save(name, o);
            CACHE.put(name, 0);
            HookLog.i("脚本[" + name + "] 失败计数已重置");
        } catch (Throwable t) {
            HookLog.w("重置失败计数出错 " + t);
        }
    }

    public static void onSuccess(String name) {
        if (crashes(name) > 0) reset(name);
    }

    private static String abbreviate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }
}
