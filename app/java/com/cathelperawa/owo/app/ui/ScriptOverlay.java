package com.cathelperawa.owo.app.ui;

import android.app.Activity;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.cathelperawa.owo.app.bridge.QQEnv;
import com.cathelperawa.owo.app.core.HookLog;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ScriptOverlay {

    private static final Map<String, View> MAP = new LinkedHashMap<String, View>();

    private ScriptOverlay() {}

    private static void dbg(String s) {
        try {
            java.io.File d = com.cathelperawa.owo.app.core.DataDir.global();
            if (d == null) return;
            if (!d.exists()) d.mkdirs();
            java.io.File f = new java.io.File(d, "overlay_debug.txt");
            java.io.FileOutputStream fo = new java.io.FileOutputStream(f, true);
            fo.write((new java.text.SimpleDateFormat("HH:mm:ss")
                    .format(new java.util.Date()) + " " + s + "\n").getBytes("UTF-8"));
            fo.flush();
            fo.close();
        } catch (Throwable ignore) {
        }
    }

    private static String key(String script, String id) {
        return (script == null ? "" : script) + "|" + (id == null ? "" : id);
    }

    public static void show(final String script, final String id,
                            final ScriptScreen.Handler h) {
        if (h == null) return;
        final Activity a = topActivity();
        dbg("show id=" + id + " activity=" + (a == null ? "null" : a.getClass().getName()));
        if (a == null) {
            HookLog.w("[浮层] 拿不到 Activity（TIM 不在前台），本次不显示");
            return;
        }
        a.runOnUiThread(new Runnable() {
            @Override public void run() {
                try {
                    hide(script, id);
                    ScriptUi ui = new ScriptUi(a);
                    ScriptScreen.Result r = new ScriptScreen.Result();
                    h.build(ui, r);
                    final View content = r.get();
                    dbg("build done content=" + (content == null ? "null"
                            : content.getClass().getSimpleName()));
                    if (content == null) return;

                    View decor = a.getWindow().getDecorView();
                    if (!(decor instanceof ViewGroup)) {
                        HookLog.w("[浮层] decorView 不是 ViewGroup，放弃");
                        return;
                    }
                    ViewGroup parent = (ViewGroup) decor;

                    int maxW = (int) (a.getResources().getDisplayMetrics().widthPixels * 0.8f);
                    ViewGroup.LayoutParams lp;
                    if (parent instanceof FrameLayout) {
                        lp = new FrameLayout.LayoutParams(maxW,
                                FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
                    } else {
                        lp = new ViewGroup.LayoutParams(maxW,
                                ViewGroup.LayoutParams.WRAP_CONTENT);
                    }

                    FrameLayout wrap = new FrameLayout(a);
                    wrap.addView(content, new FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.WRAP_CONTENT));
                    wrap.setLayoutParams(lp);
                    parent.addView(wrap, lp);
                    try {
                        parent.requestLayout();
                        parent.invalidate();
                    } catch (Throwable ignore) {
                    }
                    synchronized (MAP) {
                        MAP.put(key(script, id), wrap);
                    }
                    dbg("mounted id=" + id + " maxW=" + maxW + " parent="
                            + parent.getClass().getSimpleName() + " wrap w=" + wrap.getWidth());
                    HookLog.i("[浮层] 已显示 " + id + " width=" + maxW
                            + " parent=" + parent.getClass().getSimpleName());

                    content.postDelayed(new Runnable() {
                        @Override public void run() {
                            try {
                                dbg("measured wrap=" + wrap.getWidth() + "x" + wrap.getHeight()
                                        + " content=" + content.getWidth() + "x" + content.getHeight());
                                HookLog.i("[浮层] 实际 w=" + wrap.getWidth()
                                        + " h=" + wrap.getHeight());
                            } catch (Throwable ignore) {
                            }
                        }
                    }, 500);
                } catch (Throwable t) {
                    dbg("FAILED " + id + " " + t);
                    HookLog.e("[浮层] 显示失败 " + id, t);
                }
            }
        });
    }

    public static void hide(final String script, final String id) {
        final String k = key(script, id);
        View v;
        synchronized (MAP) {
            v = MAP.remove(k);
        }
        if (v == null) return;
        try {
            final View fv = v;
            Activity a = topActivity();
            Runnable rm = new Runnable() {
                @Override public void run() {
                    try {
                        ViewGroup p = (ViewGroup) fv.getParent();
                        if (p != null) p.removeView(fv);
                    } catch (Throwable ignore) {
                    }
                }
            };
            if (a != null) a.runOnUiThread(rm);
            else rm.run();
        } catch (Throwable t) {
            HookLog.w("[浮层] 关闭失败 " + k);
        }
    }

    public static int removeByScript(String script) {
        if (script == null) return 0;
        List<String> ids = new ArrayList<String>();
        synchronized (MAP) {
            for (String k : MAP.keySet()) {
                if (k.startsWith(script + "|")) ids.add(k.substring(script.length() + 1));
            }
        }
        for (int i = 0; i < ids.size(); i++) hide(script, ids.get(i));
        return ids.size();
    }

    public static boolean has(String script, String id) {
        synchronized (MAP) {
            return MAP.containsKey(key(script, id));
        }
    }

    private static Activity topActivity() {
        try {
            return QQEnv.topActivity();
        } catch (Throwable t) {
            return null;
        }
    }
}
