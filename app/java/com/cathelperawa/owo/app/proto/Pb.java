package com.cathelperawa.owo.app.proto;

import com.cathelperawa.owo.app.ovo.FunProtoData;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.zip.GZIPOutputStream;

public final class Pb {

    private Pb() {}

    public static byte[] vint(long v) {
        byte[] tmp = new byte[10];
        int i = 0;
        do {
            int b = (int) (v & 0x7F);
            v >>>= 7;
            if (v != 0) b |= 0x80;
            tmp[i] = (byte) b;
            i++;
        } while (v != 0);
        byte[] r = new byte[i];
        for (int j = 0; j < i; j++) r[j] = tmp[j];
        return r;
    }

    public static byte[] cat(byte[]... arrs) {
        int n = 0;
        for (byte[] a : arrs) n += a.length;
        byte[] r = new byte[n];
        int p = 0;
        for (byte[] a : arrs) {
            System.arraycopy(a, 0, r, p, a.length);
            p += a.length;
        }
        return r;
    }

    public static byte[] ld(int field, byte[] data) {
        if (data == null) data = new byte[0];
        return cat(vint((long) (field << 3) | 2), vint(data.length), data);
    }

    public static byte[] vf(int field, long v) {
        return cat(vint((long) (field << 3)), vint(v));
    }

    public static byte[] strField(int field, String s) {
        byte[] d;
        try {
            d = (s == null ? "" : s).getBytes("UTF-8");
        } catch (Throwable t) {
            d = new byte[0];
        }
        return ld(field, d);
    }

    public static byte[] gzip(byte[] data) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (GZIPOutputStream gz = new GZIPOutputStream(bos)) {
            gz.write(data);
        } catch (Throwable ignore) {
        }
        return bos.toByteArray();
    }

    

    
    public static byte[] elemBytes(String json) {
        try {
            String c = json == null ? "" : json.trim();
            JSONArray arr = new JSONArray();
            if (c.startsWith("[")) {
                arr = new JSONArray(c);
            } else {
                arr.put(new JSONObject(c));
            }
            byte[] out = new byte[0];
            for (int i = 0; i < arr.length(); i++) {
                FunProtoData pd = new FunProtoData();
                pd.fromJSON(arr.getJSONObject(i));
                out = cat(out, ld(2, pd.toBytes()));
            }
            return out;
        } catch (Throwable t) {
            return new byte[0];
        }
    }

    public static java.util.List<JSONObject> elemList(String json) {
        java.util.List<JSONObject> out = new java.util.ArrayList<JSONObject>();
        try {
            String c = json == null ? "" : json.trim();
            if (c.startsWith("[")) {
                JSONArray arr = new JSONArray(c);
                for (int i = 0; i < arr.length(); i++) out.add(arr.getJSONObject(i));
            } else if (c.length() > 0) {
                out.add(new JSONObject(c));
            }
        } catch (Throwable ignore) {
        }
        return out;
    }
}
