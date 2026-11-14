package com.cathelperawa.owo.app.core;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class KernelWhitelist {

    private static final String FILE = "kernel_whitelist.json";
    private static JSONObject sData;

    private KernelWhitelist() {}

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
                    byte[] b = new byte[(int) Math.min(r.length(), 32 * 1024)];
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
            HookLog.w("内核白名单写入失败 " + t);
        }
    }

    public static boolean allowed(String script) {
        if (script == null || script.length() == 0) return false;
        try {
            return data().has(script);
        } catch (Throwable t) {
            return false;
        }
    }

    public static synchronized void grant(String script) {
        if (script == null) return;
        try {
            data().put(script, System.currentTimeMillis());
            save();
            HookLog.i("内核白名单：已授权 [" + script + "]");
        } catch (Throwable t) {
            HookLog.w("内核白名单授权失败 " + t);
        }
    }

    public static synchronized void revoke(String script) {
        if (script == null) return;
        try {
            data().remove(script);
            save();
            HookLog.i("内核白名单：已收回 [" + script + "]");
        } catch (Throwable t) {
            HookLog.w("内核白名单收回失败 " + t);
        }
    }

    public static List<String> list() {
        List<String> out = new ArrayList<String>();
        try {
            JSONObject d = data();
            Iterator<String> it = d.keys();
            while (it.hasNext()) out.add(it.next());
        } catch (Throwable ignore) {
        }
        return out;
    }

    public static synchronized void purge(String script) {
        if (script == null) return;
        try {
            if (data().has(script)) {
                data().remove(script);
                save();
            }
        } catch (Throwable ignore) {
        }
    }
}
