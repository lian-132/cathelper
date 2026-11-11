package com.cathelperawa.owo.app.script;

import android.app.Activity;
import android.content.Context;
import android.graphics.PixelFormat;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;

import com.cathelperawa.owo.app.core.HookLog;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ScriptWindow {

    private static final Map<String, Entry> MAP = new LinkedHashMap<String, Entry>();

    private static final class Entry {
        final String script;
        final View view;
        final WindowManager wm;

        Entry(String script, View view, WindowManager wm) {
            this.script = script;
            this.view = view;
            this.wm = wm;
        }
    }

    private ScriptWindow() {}

    public static String add(String script, Activity a, View v, int gravity, int x, int y) {
        if (a == null || v == null) return null;
        try {
            WindowManager wm = (WindowManager) a.getSystemService(Context.WINDOW_SERVICE);
            if (wm == null) return null;
            WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_APPLICATION,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                            | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    PixelFormat.TRANSLUCENT);
            lp.gravity = gravity <= 0 ? (Gravity.BOTTOM | Gravity.END) : gravity;
            lp.x = x;
            lp.y = y;

            try {
                android.os.IBinder token = a.getWindow().getDecorView().getWindowToken();
                if (token != null) lp.token = token;
            } catch (Throwable ignore) {
            }
            wm.addView(v, lp);
            String id = script + ":" + System.currentTimeMillis();
            synchronized (MAP) {
                MAP.put(id, new Entry(script, v, wm));
            }
            HookLog.i("[浮层] 脚本[" + script + "] 加了浮层 " + id);
            return id;
        } catch (Throwable t) {
            HookLog.e("[浮层] 添加失败", t);
            return null;
        }
    }

    public static boolean close(String id) {
        if (id == null) return false;
        Entry e;
        synchronized (MAP) {
            e = MAP.remove(id);
        }
        if (e == null) return false;
        try {
            e.wm.removeView(e.view);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    public static int removeByScript(String script) {
        if (script == null) return 0;
        java.util.List<String> ids = new java.util.ArrayList<String>();
        synchronized (MAP) {
            for (Map.Entry<String, Entry> e : MAP.entrySet()) {
                if (script.equals(e.getValue().script)) ids.add(e.getKey());
            }
        }
        int n = 0;
        for (int i = 0; i < ids.size(); i++) {
            if (close(ids.get(i))) n++;
        }
        return n;
    }

    public static void clear() {
        java.util.List<String> ids = new java.util.ArrayList<String>();
        synchronized (MAP) {
            ids.addAll(MAP.keySet());
        }
        for (int i = 0; i < ids.size(); i++) close(ids.get(i));
    }

    public static int count() {
        synchronized (MAP) {
            return MAP.size();
        }
    }
}
