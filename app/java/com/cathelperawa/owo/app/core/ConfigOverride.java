package com.cathelperawa.owo.app.core;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class ConfigOverride {

    private static final String FILE = "config_override.json";
    private static JSONObject sData;

    private ConfigOverride() {}

    private static File file() {
        try {
            File d = DataDir.global();
            if (d == null) return null;
            if (!d.exists()) d.mkdirs();
            return new File(d, FILE);
        } catch (Throwable t) {
            return null;
        }
    }

    private static synchronized JSONObject data() {
        if (sData != null) return sData;
        JSONObject o = new JSONObject();
        try {
            File f = file();
            if (f != null && f.exists()) {
                RandomAccessFile r = new RandomAccessFile(f, "r");
                try {
                    byte[] b = new byte[(int) Math.min(r.length(), 64 * 1024)];
                    r.readFully(b);
                    o = new JSONObject(new String(b, "UTF-8"));
                } finally {
                    try { r.close(); } catch (Throwable ignore) {}
                }
            }
        } catch (Throwable t) {
            
        }
        sData = o;
        return sData;
    }

    private static synchronized void save() {
        try {
            File f = file();
            if (f == null) return;
            FileOutputStream fo = new FileOutputStream(f);
            fo.write(data().toString().getBytes("UTF-8"));
            fo.flush();
            fo.close();
        } catch (Throwable t) {
            HookLog.w("配置覆盖写入失败 " + t);
        }
    }

    private static JSONObject values() {
        JSONObject o = data().optJSONObject("values");
        if (o == null) {
            o = new JSONObject();
            try { data().put("values", o); } catch (Throwable ignore) {}
        }
        return o;
    }

    private static JSONObject owners() {
        JSONObject o = data().optJSONObject("owner");
        if (o == null) {
            o = new JSONObject();
            try { data().put("owner", o); } catch (Throwable ignore) {}
        }
        return o;
    }

    public static String get(String key) {
        if (key == null) return null;
        try {
            JSONObject v = data().optJSONObject("values");
            if (v == null) return null;
            String s = v.optString(key, "");
            return s.length() == 0 ? null : s;
        } catch (Throwable t) {
            return null;
        }
    }

    public static synchronized void set(String owner, String key, String value) {
        if (key == null || value == null) return;
        try {
            values().put(key, value);
            owners().put(key, owner == null ? "" : owner);
            save();
            HookLog.i("配置覆盖：" + key + " = " + value + "（" + owner + "）");
        } catch (Throwable t) {
            HookLog.w("配置覆盖设置失败 " + t);
        }
    }

    public static synchronized void clear(String owner, String key) {
        if (key == null) return;
        try {
            values().remove(key);
            owners().remove(key);
            save();
        } catch (Throwable t) {
            HookLog.w("配置覆盖撤销失败 " + t);
        }
    }

    public static synchronized int removeByScript(String script) {
        if (script == null) return 0;
        int n = 0;
        try {
            JSONObject own = owners();
            List<String> keys = new ArrayList<String>();
            Iterator<String> it = own.keys();
            while (it.hasNext()) keys.add(it.next());
            for (int i = 0; i < keys.size(); i++) {
                String k = keys.get(i);
                if (!script.equals(own.optString(k))) continue;
                values().remove(k);
                own.remove(k);
                n++;
            }
            if (n > 0) {
                save();
                HookLog.i("配置覆盖：已撤回脚本[" + script + "]的 " + n + " 项");
            }
        } catch (Throwable t) {
            HookLog.w("配置覆盖撤回失败 " + t);
        }
        return n;
    }

    public static synchronized int prune(List<String> enabled) {
        int n = 0;
        try {
            JSONObject own = owners();
            List<String> keys = new ArrayList<String>();
            Iterator<String> it = own.keys();
            while (it.hasNext()) keys.add(it.next());
            for (int i = 0; i < keys.size(); i++) {
                String k = keys.get(i);
                String owner = own.optString(k);
                if (owner.length() == 0) continue;
                if (enabled != null && enabled.contains(owner)) continue;
                values().remove(k);
                own.remove(k);
                n++;
            }
            if (n > 0) {
                save();
                HookLog.i("配置覆盖：清理了 " + n + " 项残留（脚本未启用）");
            }
        } catch (Throwable t) {
            HookLog.w("配置覆盖清理失败 " + t);
        }
        return n;
    }

    public static JSONObject dump() {
        return data();
    }

    public static int size() {
        try {
            JSONObject v = data().optJSONObject("values");
            return v == null ? 0 : v.length();
        } catch (Throwable t) {
            return 0;
        }
    }

    public static synchronized void clearAll() {
        try {
            sData = new JSONObject();
            save();
            HookLog.i("配置覆盖：已全部清空");
        } catch (Throwable t) {
            HookLog.w("清空配置覆盖失败 " + t);
        }
    }
}
