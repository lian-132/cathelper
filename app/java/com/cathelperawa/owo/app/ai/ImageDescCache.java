package com.cathelperawa.owo.app.ai;

import com.cathelperawa.owo.app.core.DataDir;

import com.cathelperawa.owo.app.bridge.QQEnv;
import com.cathelperawa.owo.app.core.HookLog;
import com.cathelperawa.owo.app.core.SecureFile;
import com.cathelperawa.owo.app.protocol.ImageGrabber;

import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

public final class ImageDescCache {

    private ImageDescCache() {}

    private static final String FILE_NAME = "ai_img_desc.json";

    private static final int MAX = 200;

    private static boolean loaded;
    private static volatile boolean saveQueued;

    private static JSONObject data;

    public static String get(Object record) {
        if (!AiConfig.imgCacheOn()) return null;
        String key = keyOf(record);
        if (key == null) return null;
        ensureLoaded();
        try {
            JSONObject o = data.optJSONObject(key);
            return o == null ? null : o.optString("d", null);
        } catch (Throwable t) {
            return null;
        }
    }

    public static void put(Object record, String desc) {
        if (desc == null || desc.trim().length() == 0) return;
        String key = keyOf(record);
        if (key == null) return;
        ensureLoaded();
        try {
            JSONObject o = new JSONObject();
            o.put("d", desc.trim());
            o.put("t", System.currentTimeMillis());
            data.put(key, o);
            trim();
            saveAsync();
        } catch (Throwable t) {
            HookLog.w("图片描述缓存写入失败 " + t);
        }
    }

    public static int size() {
        ensureLoaded();
        return data.length();
    }

    public static void clear() {
        ensureLoaded();
        data = new JSONObject();
        saveNow();
    }

    static String keyOf(Object record) {
        try {
            ImageGrabber.Source src = ImageGrabber.fromRecord(record);
            if (src == null) return null;
            String k = firstNonEmpty(src.md5, src.key, src.path, src.url);
            if (k == null) return null;
            return k.length() > 120 ? k.substring(k.length() - 120) : k;
        } catch (Throwable t) {
            return null;
        }
    }

    private static String firstNonEmpty(String... vals) {
        if (vals == null) return null;
        for (int i = 0; i < vals.length; i++) {
            String v = vals[i];
            if (v != null && v.trim().length() > 0 && !"none".equals(v)) return v.trim();
        }
        return null;
    }

    private static File file() {
        try {
            File d = DataDir.account("ai");
            if (!d.exists()) d.mkdirs();
            return new File(d, FILE_NAME);
        } catch (Throwable t) {
            return null;
        }
    }

    private static void ensureLoaded() {
        if (loaded) return;
        loaded = true;
        data = new JSONObject();
        try {
            File f = file();
            if (f == null || !f.exists() || f.length() == 0 || f.length() > 2 * 1024 * 1024) return;
            int[] st = new int[1];
            String text = SecureFile.readText(f, st);
            if (text == null || text.length() == 0) return;
            data = new JSONObject(text);
        } catch (Throwable t) {
            HookLog.w("图片描述缓存读取失败 " + t);
        }
    }

    private static void trim() {
        try {
            if (data.length() <= MAX) return;
            List<String> keys = new ArrayList<String>();
            Iterator<String> it = data.keys();
            while (it.hasNext()) keys.add(it.next());
            Collections.sort(keys, new Comparator<String>() {
                @Override public int compare(String a, String b) {
                    JSONObject oa = data.optJSONObject(a);
                    JSONObject ob = data.optJSONObject(b);
                    long ta = oa == null ? 0 : oa.optLong("t", 0);
                    long tb = ob == null ? 0 : ob.optLong("t", 0);
                    return ta < tb ? -1 : (ta > tb ? 1 : 0);
                }
            });
            for (int i = 0; i < keys.size() - MAX; i++) data.remove(keys.get(i));
        } catch (Throwable ignore) {
        }
    }

    private static void saveAsync() {
        if (saveQueued) return;
        saveQueued = true;
        try {
            com.cathelperawa.owo.app.core.Bg.run(new Runnable() {
                @Override public void run() {
                    try { Thread.sleep(1200); } catch (Throwable ignore) {}
                    saveQueued = false;
                    saveNow();
                }
            }, "cat-img-cache");
        } catch (Throwable t) {
            saveQueued = false;
        }
    }

    public static void saveNow() {
        try {
            File f = file();
            if (f == null || data == null) return;
            String json;
            synchronized (ImageDescCache.class) {
                json = data.toString();
            }
            SecureFile.writeText(f, json);
        } catch (Throwable t) {
            HookLog.w("图片描述缓存写盘失败 " + t);
        }
    }
}
