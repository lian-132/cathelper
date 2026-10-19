package com.cathelperawa.owo.app.bridge;

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
        apiCls = XposedHelpers.findClassIfExists(
                "com.tencent.qqnt.msg.api.impl.MsgUtilApiImpl", cl);
        jsonCls  = XposedHelpers.findClassIfExists("com.tencent.qqnt.msg.data.a", cl);
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
        try {
            init();
            Method mk = find(apiCls, "createArkElement");
            if (mk == null) throw new IllegalStateException("无 createArkElement");
            Object arg = json;
            if (jsonCls != null) {
                Method cj = find(jsonCls, "createJson", String.class);
                if (cj != null) {
                    Object jsonObj = XposedHelpers.newInstance(jsonCls);
                    cj.invoke(jsonObj, json);
                    arg = jsonObj;
                }
            }
            return mk.invoke(api, arg);
        } catch (Throwable t) { HookLog.e("createArkElement 失败", t); }
        return null;
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
        try {
            init();
            Method m = find(apiCls, "createPttElement");
            if (m == null) return null;
            int[] raw = new int[]{68,47,44,65,58,31,64,38,29,61,57,53,
                    28,60,51,36,34,57,62,49,72,29,28,27,70,56,61,45,70,67};
            ArrayList<Byte> list = new ArrayList<Byte>();
            for (int i : raw) list.add((byte) i);
            return m.invoke(api, path, 0, list);
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
                if (!"createAtElement".equals(m.getName())) continue;
                Class<?>[] p = m.getParameterTypes();
                if (p.length == 2 && p[0] == String.class && p[1] == String.class) {
                    m.setAccessible(true);
                    return m.invoke(api, uin, display == null ? "" : display);
                }
            }
        } catch (Throwable t) { HookLog.e("createAtElement 失败", t); }
        return null;
    }

    public static Object face(int faceId) {
        try {
            ClassLoader cl = QQEnv.classLoader();
            Class<?> fc = XposedHelpers.findClassIfExists(
                    "com.tencent.qqnt.kernel.nativeinterface.FaceElement", cl);
            Class<?> el = XposedHelpers.findClassIfExists(
                    "com.tencent.qqnt.kernel.nativeinterface.MsgElement", cl);
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
            Class<?> te = XposedHelpers.findClassIfExists(
                    "com.tencent.qqnt.kernel.nativeinterface.TextElement", cl);
            Class<?> el = XposedHelpers.findClassIfExists(
                    "com.tencent.qqnt.kernel.nativeinterface.MsgElement", cl);
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
