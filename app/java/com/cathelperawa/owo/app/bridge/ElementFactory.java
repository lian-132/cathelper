package com.cathelperawa.owo.app.bridge;

import com.cathelperawa.owo.app.core.dex.ClassResolver;
import com.cathelperawa.owo.app.core.HookLog;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.XposedHelpers;

public final class ElementFactory {

    private static Object api;
    private static Class<?> apiCls;
    private static Class<?> jsonCls;

    private ElementFactory() {}

    private static synchronized void init() throws Exception {
        if (api != null) return;
        ClassLoader cl = QQEnv.classLoader();
        apiCls = ClassResolver.find(cl, "qqnt.msgUtilApiImpl",
                "com.tencent.qqnt.msg.api.impl.MsgUtilApiImpl");

        
        jsonCls  = ClassResolver.find(cl, "qqnt.jsonElement",
                "com.tencent.qqnt.msg.a.b",
                "com.tencent.qqnt.msg.data.a");
        if (apiCls == null) throw new IllegalStateException("未找到 MsgUtilApiImpl");
        api = XposedHelpers.newInstance(apiCls);
    }

    public static Object text(String content) {
        try {
            init();
            Method m = find(apiCls, "createTextElement", String.class);
            if (m != null) return m.invoke(api, content);
        } catch (Throwable t) { HookLog.e("createTextElement 失败", t); }
        return textFallback(content);
    }

    public static Object ark(String json) {
        Object nativeEl = arkNative(json);
        if (nativeEl != null) return nativeEl;
        try {
            init();
            Method mk = null;
            for (Method m : apiCls.getDeclaredMethods()) {
                if ("createArkElement".equals(m.getName()) && m.getParameterTypes().length == 1) {
                    mk = m;
                    break;
                }
            }
            if (mk == null) throw new IllegalStateException("无 createArkElement");
            mk.setAccessible(true);

            Class<?> argType = mk.getParameterTypes()[0];
            Object arg = null;

            if (argType != String.class && argType != byte[].class) {
                arg = buildJsonObj(argType, json);
            }
            
            if (arg == null) arg = json;

            return mk.invoke(api, arg);
        } catch (Throwable t) { HookLog.e("createArkElement 失败", t); }
        return null;
    }

    private static Object buildJsonObj(Class<?> c, String json) {
        try {
            
            for (Constructor<?> k : c.getDeclaredConstructors()) {
                Class<?>[] ps = k.getParameterTypes();
                if (ps.length == 1 && ps[0] == String.class) {
                    k.setAccessible(true);
                    return k.newInstance(json);
                }
            }
            
            Object o = null;
            for (Constructor<?> k : c.getDeclaredConstructors()) {
                if (k.getParameterTypes().length == 0) {
                    k.setAccessible(true);
                    o = k.newInstance();
                    break;
                }
            }
            if (o != null) {
                for (Method m : c.getDeclaredMethods()) {
                    Class<?>[] ps = m.getParameterTypes();
                    if (ps.length == 1 && ps[0] == String.class && m.getReturnType() == void.class) {
                        m.setAccessible(true);
                        m.invoke(o, json);
                        return o;
                    }
                }
            }
            
            for (Method m : c.getDeclaredMethods()) {
                Class<?>[] ps = m.getParameterTypes();
                if (ps.length == 1 && ps[0] == String.class && c.isAssignableFrom(m.getReturnType())) {
                    m.setAccessible(true);
                    return m.invoke(null, json);
                }
            }
        } catch (Throwable ignore) {
        }
        return null;
    }

    public static Object arkNative(String arkJson) {
        try {
            ClassLoader cl = QQEnv.classLoader();
            Class<?> meCls = ClassResolver.find(cl, "qqnt.msgelement",
                    "com.tencent.qqnt.kernel.nativeinterface.MsgElement");
            Class<?> arkCls = ClassResolver.find(cl, "qqnt.arkelement",
                    "com.tencent.qqnt.kernel.nativeinterface.ArkElement");
            if (meCls == null || arkCls == null) {
                HookLog.w("arkNative: 找不到 MsgElement / ArkElement");
                return null;
            }

            Object ark = null;
            try {
                java.lang.reflect.Constructor<?> best = null;
                for (java.lang.reflect.Constructor<?> c : arkCls.getDeclaredConstructors()) {
                    Class<?>[] pt = c.getParameterTypes();
                    if (pt.length >= 1 && pt[0] == String.class) { best = c; break; }
                }
                if (best != null) {
                    Class<?>[] pt = best.getParameterTypes();
                    Object[] args = new Object[pt.length];
                    args[0] = arkJson;
                    for (int i = 1; i < pt.length; i++) {
                        args[i] = pt[i].isPrimitive() ? (pt[i] == int.class ? (Object) 0
                                : pt[i] == long.class ? (Object) 0L
                                : pt[i] == boolean.class ? (Object) false : null) : null;
                    }
                    best.setAccessible(true);
                    ark = best.newInstance(args);
                    HookLog.i("arkNative: 用构造器 " + best);
                }
            } catch (Throwable t) {
                HookLog.w("arkNative 构造器失败，退回 set: " + t);
            }
            if (ark == null) {
                ark = XposedHelpers.newInstance(arkCls);
                XposedHelpers.setObjectField(ark, "bytesData", arkJson);
            }
            Object me = XposedHelpers.newInstance(meCls);
            XposedHelpers.setIntField(me, "elementType", 10);
            XposedHelpers.setObjectField(me, "arkElement", ark);
            
            Object back = XposedHelpers.getObjectField(me, "arkElement");
            int et = XposedHelpers.getIntField(me, "elementType");
            HookLog.i("arkNative: elementType=" + et + " ark=" + (back != null)
                    + " len=" + (arkJson == null ? 0 : arkJson.length()));
            return me;
        } catch (Throwable t) {
            HookLog.e("arkNative 失败", t);
            return null;
        }
    }

    public static Object fileNative(String path, String name, long size) {
        try {
            ClassLoader cl = QQEnv.classLoader();
            Class<?> meCls = ClassResolver.find(cl, "qqnt.msgelement",
                    "com.tencent.qqnt.kernel.nativeinterface.MsgElement");
            Class<?> feCls = ClassResolver.find(cl, "qqnt.fileelement",
                    "com.tencent.qqnt.kernel.nativeinterface.FileElement");
            if (meCls == null || feCls == null) {
                HookLog.w("fileNative: 找不到 MsgElement / FileElement");
                return null;
            }
            Object fe = XposedHelpers.newInstance(feCls);
            XposedHelpers.setObjectField(fe, "fileName", name);
            XposedHelpers.setLongField(fe, "fileSize", size);
            XposedHelpers.setObjectField(fe, "filePath", path);
            Object me = XposedHelpers.newInstance(meCls);
            XposedHelpers.setIntField(me, "elementType", 3);
            XposedHelpers.setObjectField(me, "fileElement", fe);
            HookLog.i("fileNative: elementType=3 name=" + name + " size=" + size
                    + " path=" + path);
            return me;
        } catch (Throwable t) {
            HookLog.e("fileNative 失败", t);
            return null;
        }
    }

    public static Object pic(String path) {
        try {
            init();
            Method m = find(apiCls, "createPicElement", String.class, boolean.class, int.class);
            if (m != null) return m.invoke(api, path, Boolean.TRUE, 0);
        } catch (Throwable t) { HookLog.e("createPicElement 失败", t); }
        return null;
    }

    public static Object ptt(String path) {
        return ptt(path, 0);
    }

    public static Object ptt(String path, int durationMs) {
        int dur = durationMs > 0 ? Math.max(durationMs, 1000) : 0;

        if (dur > 0) {
            Object el = pttWithWave(path, dur);
            if (el != null) return el;
        }
        
        Object el = pttRaw(path);
        
        if (el != null && dur > 0) {
            setIntFieldDeep(el, "duration", dur);
            Object inner = getField(el, "pttElement");
            if (inner != null) setIntFieldDeep(inner, "duration", dur);
        }
        return el;
    }

    private static java.util.ArrayList<Byte> waveFor(int durationMs) {
        int n = durationMs / 40;
        if (n < 20) n = 20;
        if (n > 120) n = 120;
        java.util.ArrayList<Byte> list = new java.util.ArrayList<Byte>(n);
        for (int i = 0; i < n; i++) {
            list.add((byte) ((i * 7) % 70 + 15));
        }
        return list;
    }

    private static Object pttWithWave(String path, int dur) {
        try {
            init();
            java.util.ArrayList<Byte> wave = waveFor(dur);
            for (Method m : apiCls.getDeclaredMethods()) {
                if (!"createPttElement".equals(m.getName())) continue;
                Class<?>[] ps = m.getParameterTypes();
                if (ps.length != 3 || ps[0] != String.class) continue;
                try {
                    m.setAccessible(true);
                    Object r = m.invoke(api, path, dur, wave);
                    if (r != null) {
                        HookLog.i("createPttElement(3参) 成功 duration=" + dur
                                + " wave=" + wave.size());
                        return r;
                    }
                } catch (Throwable t) {
                    HookLog.w("createPttElement(3参) 失败 " + t);
                }
            }
        } catch (Throwable t) {
            HookLog.w("pttWithWave 异常 " + t);
        }
        return null;
    }

    private static Object getField(Object obj, String name) {
        try {
            Class<?> c = obj.getClass();
            while (c != null && c != Object.class) {
                for (java.lang.reflect.Field f : c.getDeclaredFields()) {
                    if (!f.getName().equals(name)) continue;
                    f.setAccessible(true);
                    return f.get(obj);
                }
                c = c.getSuperclass();
            }
        } catch (Throwable ignore) {
        }
        return null;
    }

    private static void setIntFieldDeep(Object obj, String name, int value) {
        try {
            Class<?> c = obj.getClass();
            while (c != null && c != Object.class) {
                for (java.lang.reflect.Field f : c.getDeclaredFields()) {
                    if (!f.getName().equals(name)) continue;
                    try {
                        f.setAccessible(true);
                        if (f.getType() == int.class) f.setInt(obj, value);
                        else if (f.getType() == long.class) f.setLong(obj, (long) value);
                        else continue;
                    } catch (Throwable ignore) {
                    }
                }
                c = c.getSuperclass();
            }
        } catch (Throwable ignore) {
        }
    }

    private static Object pttRaw(String path) {
        try {
            init();

            
            
            for (Method m : apiCls.getDeclaredMethods()) {
                if (!"createPttElement".equals(m.getName())) continue;
                Class<?>[] ps = m.getParameterTypes();
                try {
                    m.setAccessible(true);
                    if (ps.length == 2 && ps[0] == String.class) {
                        Object r = m.invoke(api, path, 0);
                        if (r != null) return r;
                    }
                } catch (Throwable ignore) {
                }
            }
            int[] raw = new int[]{68,47,44,65,58,31,64,38,29,61,57,53,
                    28,60,51,36,34,57,62,49,72,29,28,27,70,56,61,45,70,67};
            ArrayList<Byte> list = new ArrayList<Byte>();
            for (int i : raw) list.add((byte) i);
            for (Method m : apiCls.getDeclaredMethods()) {
                if (!"createPttElement".equals(m.getName())) continue;
                Class<?>[] ps = m.getParameterTypes();
                if (ps.length == 3 && ps[0] == String.class) {
                    try {
                        m.setAccessible(true);
                        return m.invoke(api, path, 0, list);
                    } catch (Throwable t) {
                        HookLog.e("createPttElement(3参) 失败", t);
                    }
                }
            }
        } catch (Throwable t) { HookLog.e("createPttElement 失败", t); }
        return null;
    }

    public static Object video(String path) {
        try {
            init();
            Method m = find(apiCls, "createVideoElement", String.class);
            if (m != null) return m.invoke(api, path);
        } catch (Throwable t) { HookLog.e("createVideoElement 失败", t); }
        return null;
    }

    public static Object file(String path) {
        try {
            init();
            Method m = find(apiCls, "createFileElement", String.class);
            if (m != null) return m.invoke(api, path);
        } catch (Throwable t) { HookLog.e("createFileElement 失败", t); }
        return null;
    }

    public static Object reply(long replyMsgId) {
        try {
            init();
            Method m = find(apiCls, "createReplyElement", long.class);
            if (m == null) m = find(apiCls, "createReplyElement", Long.class);
            if (m != null) return m.invoke(api, replyMsgId);
        } catch (Throwable t) { HookLog.e("createReplyElement 失败", t); }
        return null;
    }

    public static Object at(String uin, String display) {
        try {
            init();

            
            for (Method m : apiCls.getDeclaredMethods()) {
                if (!"createAtTextElement".equals(m.getName())) continue;
                Class<?>[] p = m.getParameterTypes();
                if (p.length == 3 && p[0] == String.class && p[1] == String.class
                        && p[2] == int.class) {
                    m.setAccessible(true);
                    return m.invoke(api, uin, display == null ? "" : display, 0);
                }
            }
            
            for (Method m : apiCls.getDeclaredMethods()) {
                if (!"createAtElement".equals(m.getName())) continue;
                Class<?>[] p = m.getParameterTypes();
                if (p.length == 2 && p[0] == String.class && p[1] == String.class) {
                    m.setAccessible(true);
                    return m.invoke(api, uin, display == null ? "" : display);
                }
            }
        } catch (Throwable t) { HookLog.e("createAtTextElement 失败", t); }
        return null;
    }

    public static Object face(int faceId) {
        try {
            ClassLoader cl = QQEnv.classLoader();
            Class<?> fc = ClassResolver.find(cl, "qqnt.faceElement",
                    "com.tencent.qqnt.kernel.nativeinterface.FaceElement");
            Class<?> el = ClassResolver.find(cl, "qqnt.msgElement",
                    "com.tencent.qqnt.kernel.nativeinterface.MsgElement");
            if (fc == null || el == null) return null;
            Object f = XposedHelpers.newInstance(fc);
            try { fc.getMethod("setFaceIndex", int.class).invoke(f, faceId); } catch (Throwable ignore) {}
            Object e = XposedHelpers.newInstance(el);
            el.getMethod("setElementType", int.class).invoke(e, 2);
            el.getMethod("setFaceElement", fc).invoke(e, f);
            return e;
        } catch (Throwable t) { HookLog.e("face 元素失败", t); return null; }
    }

    public static Object call(String method, Object... args) {
        try {
            init();
            for (Method m : apiCls.getDeclaredMethods()) {
                if (!m.getName().equals(method)) continue;
                if (args != null && m.getParameterTypes().length != args.length) continue;
                m.setAccessible(true);
                return m.invoke(api, args);
            }
        } catch (Throwable t) { HookLog.e("call " + method + " 失败", t); }
        return null;
    }

    public static List<String> listFactories() {
        List<String> out = new ArrayList<String>();
        try {
            init();
            for (Method m : apiCls.getDeclaredMethods()) {
                if (m.getName().startsWith("create")) {
                    StringBuilder sb = new StringBuilder(m.getName()).append("(");
                    Class<?>[] p = m.getParameterTypes();
                    for (int i = 0; i < p.length; i++) {
                        if (i > 0) sb.append(", ");
                        sb.append(p[i].getSimpleName());
                    }
                    out.add(sb.append(")").toString());
                }
            }
        } catch (Throwable ignore) {
        }
        return out;
    }

    private static Object textFallback(String content) {
        try {
            ClassLoader cl = QQEnv.classLoader();
            Class<?> te = ClassResolver.find(cl, "qqnt.textElement",
                    "com.tencent.qqnt.kernel.nativeinterface.TextElement");
            Class<?> el = ClassResolver.find(cl, "qqnt.msgElement",
                    "com.tencent.qqnt.kernel.nativeinterface.MsgElement");
            if (te == null || el == null) return null;
            Object t = XposedHelpers.newInstance(te);
            te.getMethod("setContent", String.class).invoke(t, content);
            Object e = XposedHelpers.newInstance(el);
            el.getMethod("setElementType", int.class).invoke(e, 1);
            el.getMethod("setTextElement", te).invoke(e, t);
            return e;
        } catch (Throwable t) { HookLog.e("文本直构失败", t); return null; }
    }

    private static Method find(Class<?> cls, String name, Class<?>... params) {
        if (cls == null) return null;
        for (Method m : cls.getDeclaredMethods()) {
            if (!m.getName().equals(name)) continue;
            if (params.length == 0) return m;
            Class<?>[] ps = m.getParameterTypes();
            if (ps.length != params.length) continue;
            boolean ok = true;
            for (int i = 0; i < ps.length; i++) {
                if (params[i] != null && !params[i].isAssignableFrom(ps[i])
                        && !ps[i].isAssignableFrom(params[i])) { ok = false; break; }
            }
            if (ok) return m;
        }
        return null;
    }
}
