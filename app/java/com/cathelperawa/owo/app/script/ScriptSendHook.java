package com.cathelperawa.owo.app.script;

import com.cathelperawa.owo.app.bridge.QQEnv;
import com.cathelperawa.owo.app.core.HookApi;
import com.cathelperawa.owo.app.core.HookLog;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

public final class ScriptSendHook {

    public static final String POINT = "host.send.raw";

    private static volatile boolean installed = false;
    private static int retries = 0;

    private ScriptSendHook() {}

    private static String kindOf(Object el) {
        if (el == null) return null;
        try {
            if (com.cathelperawa.owo.app.util.Refl.field(el, "textElement") != null) return "text";
            if (com.cathelperawa.owo.app.util.Refl.field(el, "picElement") != null) return "pic";
            if (com.cathelperawa.owo.app.util.Refl.field(el, "atElement") != null) return "at";
            if (com.cathelperawa.owo.app.util.Refl.field(el, "arkElement") != null) return "ark";
            if (com.cathelperawa.owo.app.util.Refl.field(el, "pttElement") != null) return "ptt";
            if (com.cathelperawa.owo.app.util.Refl.field(el, "fileElement") != null) return "file";
            if (com.cathelperawa.owo.app.util.Refl.field(el, "faceElement") != null) return "face";
        } catch (Throwable ignore) {
        }
        return null;
    }

    private static String summarize(java.util.List<Object> list) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            Object el = list.get(i);
            if (el == null) continue;
            String kind = null;
            try {
                if (com.cathelperawa.owo.app.util.Refl.field(el, "textElement") != null) kind = "text";
                else if (com.cathelperawa.owo.app.util.Refl.field(el, "picElement") != null) kind = "pic";
                else if (com.cathelperawa.owo.app.util.Refl.field(el, "atElement") != null) kind = "at";
                else if (com.cathelperawa.owo.app.util.Refl.field(el, "arkElement") != null) kind = "ark";
                else if (com.cathelperawa.owo.app.util.Refl.field(el, "pttElement") != null) kind = "ptt";
                else if (com.cathelperawa.owo.app.util.Refl.field(el, "fileElement") != null) kind = "file";
                else if (com.cathelperawa.owo.app.util.Refl.field(el, "faceElement") != null) kind = "face";
            } catch (Throwable ignore) {
            }
            if (kind == null) continue;
            if (sb.indexOf(kind) >= 0) continue;
            if (sb.length() > 0) sb.append(",");
            sb.append(kind);
        }
        return sb.toString();
    }

    private static void dbg(String s) {
        try {
            java.io.File d = com.cathelperawa.owo.app.core.DataDir.global();
            if (d == null) return;
            if (!d.exists()) d.mkdirs();
            java.io.File f = new java.io.File(d, "send_hook_debug.txt");
            if (f.length() > 256 * 1024) f.delete();
            java.io.FileOutputStream fo = new java.io.FileOutputStream(f, true);
            fo.write((new java.text.SimpleDateFormat("HH:mm:ss")
                    .format(new java.util.Date()) + " " + s + "\n").getBytes("UTF-8"));
            fo.flush();
            fo.close();
        } catch (Throwable ignore) {
        }
    }

    public static void install() {
        if (installed) return;
        try {
            Object svc = QQEnv.kernelMsgService();
            if (svc == null) {
                scheduleRetry();
                return;
            }

            int hooked = 0;
            for (Method m : svc.getClass().getDeclaredMethods()) {
                if (!"sendMsg".equals(m.getName())) continue;
                try {
                    HookApi.before(m, new HookApi.Action() {
                        @Override public void call(
                                de.robv.android.xposed.XC_MethodHook.MethodHookParam p) {
                            try {
                                patchArgs(p.args);
                            } catch (Throwable t) {
                                HookLog.w("[发送拦截] 处理异常 " + t);
                            }
                        }
                    });
                    hooked++;
                    dbg("hooked " + m);
                } catch (Throwable t) {
                    dbg("hook 失败 " + m + " : " + t);
                }
            }
            if (hooked == 0) {
                dbg("没有可 hook 的 sendMsg（declaredMethods 里没有）");
                scheduleRetry();
                return;
            }
            installed = true;
            dbg("install ok，共 hook " + hooked + " 个 sendMsg，svc=" + svc.getClass().getName());
            HookLog.i("[发送拦截] 已挂 " + hooked + " 个 sendMsg");
        } catch (Throwable t) {
            HookLog.w("[发送拦截] 安装失败 " + t);
            scheduleRetry();
        }
    }

    private static void scheduleRetry() {
        if (retries++ > 6) return;
        try {
            com.cathelperawa.owo.app.core.Bg.run(new Runnable() {
                @Override public void run() {
                    try { Thread.sleep(5000); } catch (Throwable ignore) {}
                    install();
                }
            }, "cat-sendhook");
        } catch (Throwable ignore) {
        }
    }

    private static Method findSendMsg(Class<?> cls) {
        Method fallback = null;
        try {
            for (Method m : cls.getMethods()) {
                if (!"sendMsg".equals(m.getName())) continue;
                Class<?>[] ps = m.getParameterTypes();
                if (ps.length != 5) continue;
                if (ps[0] != long.class && ps[0] != Long.class) continue;
                if (ps[4].getName().contains("IMsgOperateCallback")) {
                    return m;
                }
                if (fallback == null) fallback = m;
            }
        } catch (Throwable ignore) {
        }
        return fallback;
    }

    private static void patchArgs(Object[] args) {
        if (args == null || args.length < 3) return;
        Object listObj = args[2];
        if (!(listObj instanceof List)) {
            dbg("args[2] 不是 List：" + (listObj == null ? "null" : listObj.getClass().getName()));
            return;
        }
        if (!PatchPoint.has(POINT)) {
            dbg("点位没脚本，跳过");
            return;
        }
        dbg("命中 sendMsg，elements=" + ((List) listObj).size());

        @SuppressWarnings("unchecked")
        List<Object> list = (List<Object>) listObj;
        boolean dumped = false;

        final java.util.List<String> drops = new java.util.ArrayList<String>();
        for (int i = list.size() - 1; i >= 0; i--) {
            Object el = list.get(i);
            if (el == null) continue;

            if (!dumped) {
                dumped = true;
                dbg("element[" + i + "] class=" + el.getClass().getName());
            }

            Object holder = null;
            Field valueField = null;

            Field teF = fieldNamed(el.getClass(), "textElement");
            if (teF != null) {
                try {
                    Object te = teF.get(el);
                    if (te != null) {
                        Field cf = fieldNamed(te.getClass(), "content");
                        if (cf != null) {
                            holder = te;
                            valueField = cf;
                        }
                    }
                } catch (Throwable t2) {
                    dbg("取 textElement 失败 " + t2);
                }
            }
            if (valueField == null) {
                Field cf = fieldNamed(el.getClass(), "content");
                if (cf != null) {
                    holder = el;
                    valueField = cf;
                }
            }
            if (valueField == null) {
                dbg("跳过（没有 content 字段）：" + el.getClass().getSimpleName());
                continue;
            }

            String old;
            try {
                Object v = valueField.get(holder);
                if (!(v instanceof String)) continue;
                old = (String) v;
            } catch (Throwable t2) {
                dbg("读 content 失败 " + t2);
                continue;
            }
            if (old.length() == 0) continue;

            PatchPoint.PatchCtx c = new PatchPoint.PatchCtx(POINT, old, "send");

            try {
                c.elements(summarize(list));
                c.setDropSink(drops);
            } catch (Throwable ignore) {
            }
            PatchPoint.emit(c);

            if (c.cancelled()) {
                dbg("CANCEL [" + old + "]");
                list.remove(i);
                continue;
            }
            String now = c.text();
            if (now != null && !now.equals(old)) {
                try {
                    valueField.set(holder, now);
                    dbg("改写 [" + old + "] → [" + now + "]");
                } catch (Throwable t2) {
                    dbg("写回失败（可能是 final 字段）：" + t2);

                    try {
                        valueField.setAccessible(true);
                        valueField.set(holder, now);
                        dbg("改写成功（final 兜底）：[" + now + "]");
                    } catch (Throwable t3) {
                        dbg("final 兜底也失败：" + t3);
                    }
                }
            }
        }
    }

    private static Field fieldNamed(Class<?> cls, String name) {
        Class<?> c = cls;
        for (int d = 0; c != null && d < 4; d++, c = c.getSuperclass()) {
            try {
                Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                return f;
            } catch (Throwable ignore) {
            }
        }
        return null;
    }

    private static Field textField(Class<?> cls) {
        Class<?> c = cls;
        for (int depth = 0; c != null && depth < 4; depth++, c = c.getSuperclass()) {
            try {
                for (Field f : c.getDeclaredFields()) {
                    if (f.getType() != String.class) continue;
                    String n = f.getName().toLowerCase();
                    if (n.equals("text") || n.equals("msg") || n.equals("content")
                            || n.contains("text")) {
                        f.setAccessible(true);
                        return f;
                    }
                }
            } catch (Throwable ignore) {
            }
        }
        return null;
    }

    private static String fieldsOf(Class<?> cls) {
        StringBuilder sb = new StringBuilder();
        try {
            Class<?> c = cls;
            for (int d = 0; c != null && d < 3; d++, c = c.getSuperclass()) {
                for (Field f : c.getDeclaredFields()) {
                    if (sb.length() > 0) sb.append(',');
                    sb.append(f.getName()).append(':').append(f.getType().getSimpleName());
                }
            }
        } catch (Throwable ignore) {
        }
        return sb.toString();
    }
}
