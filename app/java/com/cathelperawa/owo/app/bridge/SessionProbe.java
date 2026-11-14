package com.cathelperawa.owo.app.bridge;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class SessionProbe {

    private SessionProbe() {
    }

    private static final int MAX = 64 * 1024;

    public static void probe(Activity act, EditText et) {
        try {
            log("==== probe ==== activity=" + (act == null ? "null" : act.getClass().getName()));
            if (act == null) return;

            bundle(act);
            climb(et);
        } catch (Throwable t) {
            log("probe 异常 " + t);
        }
    }

    private static void bundle(Activity act) {
        try {
            Intent it = act.getIntent();
            if (it == null) {
                log("Intent = null");
                return;
            }
            Bundle b = it.getExtras();
            if (b == null) {
                log("Intent.extras = null");
                return;
            }
            log("Intent.extras keys=" + b.keySet());
            for (String k : b.keySet()) {
                Object v = b.get(k);
                String cls = v == null ? "null" : v.getClass().getName();
                log("  extra[" + k + "] = " + cls + " :: " + trim(String.valueOf(v)));
            }
        } catch (Throwable t) {
            log("读 Intent 失败 " + t);
        }
    }

    private static void climb(EditText et) {
        if (et == null) {
            log("输入框 = null");
            return;
        }
        View v = et;
        int level = 0;
        while (v != null && level < 10) {
            log("↑" + level + " " + v.getClass().getName()
                    + (v.getId() == View.NO_ID ? "" : (" id=" + Integer.toHexString(v.getId()))));
            fields(v, 3);
            if (v.getParent() instanceof View) v = (View) v.getParent();
            else break;
            level++;
        }
    }

    private static void fields(Object o, int depth) {
        try {
            Class<?> c = o.getClass();
            int n = 0;
            while (c != null && c != Object.class && n++ < 200) {
                for (Field f : c.getDeclaredFields()) {
                    String tn = f.getType().getName();
                    boolean interesting = tn.contains("AIO") || tn.contains("Session")
                            || tn.contains("Contact") || tn.contains("Chat") || tn.contains("Param");
                    if (!interesting) continue;
                    try {
                        f.setAccessible(true);
                        Object val = f.get(o);
                        log("   ." + f.getName() + " : " + tn + " = " + trim(String.valueOf(val)));
                        if (depth > 1 && val != null && !(val instanceof String)
                                && !(val instanceof Number)) {
                            deep(val, depth - 1);
                        }
                    } catch (Throwable ignore) {
                    }
                }
                c = c.getSuperclass();
            }
        } catch (Throwable ignore) {
        }
    }

    private static void deep(Object o, int depth) {
        try {
            Class<?> c = o.getClass();
            int n = 0;
            while (c != null && c != Object.class && n++ < 120) {
                for (Field f : c.getDeclaredFields()) {
                    String fn = f.getName().toLowerCase(Locale.US);
                    if (!(fn.contains("peer") || fn.contains("chattype") || fn.contains("uin")
                            || fn.contains("sessionid") || fn.contains("contactid"))) continue;
                    try {
                        f.setAccessible(true);
                        Object val = f.get(o);
                        log("     " + o.getClass().getSimpleName() + "." + f.getName()
                                + " = " + trim(String.valueOf(val)));
                        feed(fn, val);
                    } catch (Throwable ignore) {
                    }
                }
                c = c.getSuperclass();
            }
        } catch (Throwable ignore) {
        }
    }

    private static void feed(String fieldName, Object val) {
        try {
            if (val == null) return;
            if (fieldName.contains("chattype")) {
                int ct = Integer.parseInt(String.valueOf(val));
                if (ct == 1 || ct == 2) CurrentChat.set(ct, CurrentChat.peerUid(), null);
            } else if (fieldName.contains("peer") && val instanceof String) {
                String s = (String) val;
                if (s.length() >= 4) CurrentChat.set(CurrentChat.chatType(), s, null);
            }
        } catch (Throwable ignore) {
        }
    }

    private static void log(String s) {
        try {
            Activity a = QQEnv.topActivity();
            File dir = a != null ? a.getFilesDir()
                    : new File("/data/data/com.tencent.tim/files");
            File f = new File(dir, "send_proto_debug.txt");
            if (f.length() > MAX) return;
            FileOutputStream o = new FileOutputStream(f, true);
            try {
                String t = new SimpleDateFormat("HH:mm:ss", Locale.US).format(new Date());
                o.write((t + " " + s + "\n").getBytes("UTF-8"));
            } finally {
                o.close();
            }
        } catch (Throwable ignore) {
        }
    }

    private static String trim(String s) {
        if (s == null) return "null";
        s = s.replace("\n", " ");
        return s.length() > 260 ? s.substring(0, 260) + "…" : s;
    }

    static View findByTag(View root, String tag) {
        if (root == null) return null;
        if (tag.equals(String.valueOf(root.getTag()))) return root;
        if (root instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) root;
            for (int i = 0; i < g.getChildCount(); i++) {
                View r = findByTag(g.getChildAt(i), tag);
                if (r != null) return r;
            }
        }
        return null;
    }
}
