package com.cathelperawa.owo.app.pan;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public final class PanTask {

    public static final int WAIT = 0;
    public static final int RUN = 1;
    public static final int DONE = 2;
    public static final int FAIL = 3;
    public static final int CANCELED = 4;

    public String id = "";
    public String url = "";
    public String name = "";
    public String ua = "";
    public String cookie = "";
    public String referer = "";
    public String mime = "";

    public long total = 0;
    public volatile long done = 0;
    public volatile int state = WAIT;
    public String err = "";

    public String partPath = "";

    public String publicUri = "";

    public transient long lastDone = 0;
    public transient long lastAt = 0;
    public transient long speed = 0;

    public int retry = 0;

    public transient volatile boolean cancel = false;

    public transient volatile long segLimit = 0;

    public boolean running() {
        return state == WAIT || state == RUN;
    }

    public int percent() {
        if (total <= 0) return 0;
        long d = done;
        int p = (int) (d * 100 / total);
        return Math.max(0, Math.min(100, p));
    }

    public String speedText() {
        if (!running()) return "";
        return PanItem.human(speed) + "/s";
    }

    public JSONObject toJson(android.content.Context ctx) {
        try {
            JSONObject o = new JSONObject();
            o.put("id", id);
            o.put("url", enc(ctx, url));
            o.put("name", name);
            o.put("ua", ua);

            o.put("cookie", enc(ctx, cookie));
            o.put("referer", enc(ctx, referer));
            o.put("mime", mime);
            o.put("total", total);
            o.put("done", done);
            o.put("state", state);
            o.put("err", err);
            o.put("part", partPath);
            o.put("uri", publicUri);
            o.put("retry", retry);
            return o;
        } catch (Throwable t) {
            return new JSONObject();
        }
    }

    public static PanTask fromJson(android.content.Context ctx, JSONObject o) {
        PanTask t = new PanTask();
        t.id = o.optString("id");
        t.url = dec(ctx, o.optString("url"));
        t.name = o.optString("name");
        t.ua = o.optString("ua");
        t.cookie = dec(ctx, o.optString("cookie"));
        t.referer = dec(ctx, o.optString("referer"));
        t.mime = o.optString("mime");
        t.total = o.optLong("total");
        t.done = o.optLong("done");
        t.state = o.optInt("state");
        t.err = o.optString("err");
        t.partPath = o.optString("part");
        t.publicUri = o.optString("uri");
        t.retry = o.optInt("retry");
        return t;
    }

    private static String enc(android.content.Context ctx, String plain) {
        return Secure.enc(ctx, plain);
    }

    private static String dec(android.content.Context ctx, String stored) {
        return Secure.dec(ctx, stored);
    }

    public static void saveAll(File f, java.util.List<PanTask> list, android.content.Context ctx) {
        try {
            JSONArray arr = new JSONArray();
            for (PanTask t : list) arr.put(t.toJson(ctx));
            java.io.FileOutputStream os = new java.io.FileOutputStream(f);
            os.write(arr.toString().getBytes("UTF-8"));
            os.flush();
            os.close();
        } catch (Throwable ignore) {
        }
    }

    public static List<PanTask> loadAll(File f, android.content.Context ctx) {
        List<PanTask> out = new ArrayList<PanTask>();
        if (f == null || !f.exists()) return out;
        try {
            java.io.FileInputStream in = new java.io.FileInputStream(f);
            java.io.ByteArrayOutputStream bo = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) bo.write(buf, 0, n);
            in.close();
            JSONArray arr = new JSONArray(new String(bo.toByteArray(), "UTF-8"));
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o != null) out.add(fromJson(ctx, o));
            }
        } catch (Throwable ignore) {
        }
        return out;
    }
}
