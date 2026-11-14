package com.cathelperawa.owo.app.protocol;

import com.cathelperawa.owo.app.api.IoApi;

import org.json.JSONObject;

import java.util.Iterator;
import java.util.LinkedHashMap;

public final class PttIndex {

    private PttIndex() {}

    private static final String FILE = "ptt_index.json";
    private static final int MAX = 500;

    private static LinkedHashMap<String, String> MAP = null;

    private static java.io.File file() {
        try {
            java.io.File d = IoApi.dataDir();
            return d == null ? null : new java.io.File(d, FILE);
        } catch (Throwable t) {
            return null;
        }
    }

    private static synchronized void ensure() {
        if (MAP != null) return;
        MAP = new LinkedHashMap<String, String>();
        try {
            int[] st = new int[1];
            String raw = com.cathelperawa.owo.app.core.SecureFile.readText(file(), st);
            if (st[0] == com.cathelperawa.owo.app.core.SecureFile.CORRUPT) {

                com.cathelperawa.owo.app.core.SecureFile.backupCorrupt(file());
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

    private static LinkedHashMap<String, String> BY_SEQ = null;

    public static synchronized void put(long msgId, long msgSeq,
                                        String path, String md5, String fid) {
        if (msgId == 0) return;
        if ((path == null || path.length() == 0)
                && (md5 == null || md5.length() == 0)
                && (fid == null || fid.length() == 0)) return;
        ensure();
        StringBuilder sb = new StringBuilder();
        sb.append(path == null ? "" : path).append('\u0001');
        sb.append(md5 == null ? "" : md5).append('\u0001');
        sb.append(fid == null ? "" : fid);
        MAP.put(String.valueOf(msgId), sb.toString());
        if (msgSeq != 0) {
            if (BY_SEQ == null) BY_SEQ = new LinkedHashMap<String, String>();
            BY_SEQ.put(String.valueOf(msgSeq), sb.toString());
            while (BY_SEQ.size() > MAX) {
                Iterator<String> it = BY_SEQ.keySet().iterator();
                if (it.hasNext()) { it.next(); it.remove(); }
            }
        }
        while (MAP.size() > MAX) {
            Iterator<String> it = MAP.keySet().iterator();
            if (it.hasNext()) {
                it.next();
                it.remove();
            }
        }
        save();
    }

    public static synchronized String[] get(long msgId) {
        if (msgId == 0) return null;
        ensure();
        String v = MAP.get(String.valueOf(msgId));
        if (v == null || v.length() == 0) return null;
        String[] arr = v.split("\u0001", -1);
        if (arr.length < 3) return null;
        return arr;
    }

    public static synchronized String[] getBySeq(long seq) {
        if (seq == 0) return null;
        ensure();
        if (BY_SEQ == null) return null;
        String v = BY_SEQ.get(String.valueOf(seq));
        if (v == null) return null;
        String[] arr = v.split("\u0001", -1);
        return arr.length >= 3 ? arr : null;
    }

    public static synchronized String[] getByMd5(String md5) {
        if (md5 == null || md5.length() < 8) return null;
        ensure();
        for (String k : MAP.keySet()) {
            String v = MAP.get(k);
            if (v == null) continue;
            if (v.toUpperCase().contains(md5.toUpperCase())) {
                String[] arr = v.split("\u0001", -1);
                if (arr.length >= 3) return arr;
            }
        }
        return null;
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
