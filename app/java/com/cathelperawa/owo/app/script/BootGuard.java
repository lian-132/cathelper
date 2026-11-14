package com.cathelperawa.owo.app.script;

import com.cathelperawa.owo.app.core.DataDir;
import com.cathelperawa.owo.app.core.HookLog;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;

public final class BootGuard {

    private static final String FILE = "boot_state.json";
    private static final long SUSPICIOUS_WINDOW_MS = 120_000L;

    private BootGuard() {}

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

    private static JSONObject load() {
        JSONObject o = new JSONObject();
        try {
            File f = file();
            if (f == null || !f.exists()) return o;
            RandomAccessFile r = new RandomAccessFile(f, "r");
            try {
                byte[] b = new byte[(int) Math.min(r.length(), 4096)];
                r.readFully(b);
                o = new JSONObject(new String(b, "UTF-8"));
            } finally {
                try { r.close(); } catch (Throwable ignore) {}
            }
        } catch (Throwable ignore) {
        }
        return o;
    }

    private static void save(String state, int fails) {
        try {
            File f = file();
            if (f == null) return;
            JSONObject o = new JSONObject();
            o.put("state", state);
            o.put("ts", System.currentTimeMillis());
            o.put("fails", fails);
            FileOutputStream fo = new FileOutputStream(f);
            fo.write(o.toString().getBytes("UTF-8"));
            fo.flush();
            fo.close();
        } catch (Throwable t) {
            HookLog.w("启动状态写入失败 " + t);
        }
    }

    public static synchronized boolean beginLoad() {
        long now = System.currentTimeMillis();
        JSONObject o = load();
        long ts = o.optLong("ts", 0);
        String state = o.optString("state", "");
        int fails = o.optInt("fails", 0);

        boolean suspicious = "loading".equals(state) && ts > 0
                && (now - ts) < SUSPICIOUS_WINDOW_MS;
        if (suspicious) {
            fails++;
            HookLog.w("[启动守卫] 上次启动没走完（" + ((now - ts) / 1000) + " 秒前），"
                    + "判定为脚本导致 → 本次跳过全部脚本加载");
        } else {
            fails = 0;
        }
        save("loading", fails);

        if (suspicious && fails < 2) {
            HookLog.w("[启动守卫] 上次启动没走完（第 " + fails + " 次）→ 先照常加载，连续两次才拦");
            return false;
        }
        return suspicious;
    }

    public static synchronized void markRecovered() {
        save("recovered", 0);
    }

    public static synchronized void markLoaded() {
        save("ok", 0);
    }

    public static String lastState() {
        JSONObject o = load();
        return o.optString("state", "");
    }
}
