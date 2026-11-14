package com.cathelperawa.owo.app.bridge;

import com.cathelperawa.owo.app.api.IoApi;

import org.json.JSONObject;

import java.io.File;
import java.util.Iterator;
import java.util.LinkedHashMap;

public final class VoiceTextCache {

    private VoiceTextCache() {}

    private static final String FILE = "voice_text.json";
    private static final int MAX = 500;

    private static LinkedHashMap<String, String> MAP = null;

    private static File file() {
        try {
            File d = IoApi.dataDir();
            return d == null ? null : new File(d, FILE);
        } catch (Throwable t) {
            return null;
        }
    }

    private static synchronized void ensure() {
        if (MAP != null) return;
        MAP = new LinkedHashMap<String, String>();
        try {
            File f = file();
            int[] st = new int[1];
            String raw = com.cathelperawa.owo.app.core.SecureFile.readText(f, st);
            if (st[0] == com.cathelperawa.owo.app.core.SecureFile.CORRUPT) {
                com.cathelperawa.owo.app.core.SecureFile.backupCorrupt(f);
                return;
            }
            if (raw != null && raw.trim().length() > 0) {
                JSONObject o = new JSONObject(raw);
                Iterator<String> it = o.keys();
                while (it.hasNext()) {
                    String k = it.next();
                    String v = o.optString(k, "");
                    if (k.length() > 0 && v.length() > 0) MAP.put(k, v);
                }

                if (st[0] == com.cathelperawa.owo.app.core.SecureFile.PLAIN) save();
            }
        } catch (Throwable ignore) {
        }
    }

    public static synchronized String get(String key) {
        if (key == null || key.length() < 6) return "";
        ensure();
        String v = MAP.get(key);
        return v == null ? "" : v;
    }

    public static synchronized void put(String key, String text) {
        if (key == null || key.length() < 6) return;
        if (text == null || text.trim().length() == 0) return;
        ensure();
        MAP.put(key, text.trim());
        while (MAP.size() > MAX) {
            Iterator<String> it = MAP.keySet().iterator();
            if (it.hasNext()) {
                it.next();
                it.remove();
            }
        }
        save();
    }

    private static void save() {
        try {
            JSONObject o = new JSONObject();
            for (String k : MAP.keySet()) o.put(k, MAP.get(k));
            com.cathelperawa.owo.app.core.SecureFile.writeText(file(), o.toString());
        } catch (Throwable ignore) {
        }
    }

    public static synchronized int size() {
        ensure();
        return MAP.size();
    }
}
