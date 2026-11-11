package com.cathelperawa.owo.app.bridge;

import com.cathelperawa.owo.app.core.HookLog;
import com.cathelperawa.owo.app.util.Refl;

import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.LinkedHashMap;
import java.util.Map;

public final class TimHb {

    private TimHb() {}

    private static Class<?> CLS_REQ, CLS_TASK, CLS_CLIENT, CLS_CB, CLS_CMD, CLS_FUNC1;
    private static Method M_SEND;            
    private static boolean inited;

    private static volatile Object cachedCmd;
    private static volatile String cachedUrl;

    public interface Sink {
        
        void onResponse(boolean ok, String json, String url);
    }

    private static volatile Sink sink;

    public static void setResultSink(Sink s) { sink = s; }

    

    private static void ensureInit() {
        if (inited) return;
        inited = true;
        try {
            ClassLoader cl = QQEnv.classLoader();
            CLS_REQ = find(cl, "com.tenpay.sdk.net.sso.PaySsoRequest");
            CLS_TASK = find(cl, "com.tenpay.sdk.net.sso.PaySsoTask");
            CLS_CLIENT = find(cl, "com.tenpay.sdk.net.sso.HbProxyNetClient");
            CLS_CB = find(cl, "com.tenpay.sdk.net.core.callback.NetCallback");
            CLS_CMD = find(cl, "com.tenpay.sdk.net.sso.PaySsoCmd");
            CLS_FUNC1 = find(cl, "kotlin.jvm.functions.Function1");
            if (CLS_CLIENT != null) {
                for (Method m : CLS_CLIENT.getDeclaredMethods()) {
                    if ("sendRequest".equals(m.getName()) && m.getParameterTypes().length == 2) {
                        M_SEND = m;
                        break;
                    }
                }
            }
            HookLog.i("TimHb 初始化: req=" + (CLS_REQ != null) + " task=" + (CLS_TASK != null)
                    + " client=" + (CLS_CLIENT != null) + " cb=" + (CLS_CB != null)
                    + " cmd=" + (CLS_CMD != null) + " func1=" + (CLS_FUNC1 != null)
                    + " send=" + (M_SEND != null));
        } catch (Throwable t) {
            HookLog.e("TimHb 初始化失败", t);
        }
    }

    public static void install(ClassLoader cl) {
        ensureInit();
        if (CLS_CLIENT == null) return;
        try {
            int n = 0;
            for (final Method m : CLS_CLIENT.getDeclaredMethods()) {
                if (!"processResponse".equals(m.getName())) continue;
                if (m.getParameterTypes().length != 3) continue;
                de.robv.android.xposed.XposedBridge.hookMethod(m,
                        new de.robv.android.xposed.XC_MethodHook() {
                    @Override protected void beforeHookedMethod(MethodHookParam p) {
                        try {
                            Object[] a = p.args;
                            if (a == null || a.length < 2) return;
                            boolean ok = a[0] instanceof Boolean && (Boolean) a[0];
                            String json = a[1] instanceof String ? (String) a[1] : null;
                            HookLog.i("TimHb 回包: ok=" + ok + " len="
                                    + (json == null ? 0 : json.length()));
                            
                            if (json == null || json.isEmpty()) {
                                Object res = (a.length > 2) ? a[2] : null;
                                if (res != null) {
                                    HookLog.i("TimHb 空回包诊断: bizSucceed="
                                            + com.cathelperawa.owo.app.util.Refl.field(res, "isBizSucceed")
                                            + " connSucceed=" + com.cathelperawa.owo.app.util.Refl.field(res, "isConnectSucceed")
                                            + " url=" + com.cathelperawa.owo.app.util.Refl.field(res, "url")
                                            + " biz=" + com.cathelperawa.owo.app.util.Refl.field(res, "bizResponse")
                                            + " err=" + com.cathelperawa.owo.app.util.Refl.field(res, "throwable"));
                                    Object t = com.cathelperawa.owo.app.util.Refl.field(res, "throwable");
                                    if (t instanceof Throwable) HookLog.e("TimHb 请求异常", (Throwable) t);
                                }
                            }
                            Sink s = sink;
                            if (s != null) s.onResponse(ok, json, null);
                        } catch (Throwable ignore) {}
                    }
                });
                n++;
            }
            HookLog.i("TimHb: 已挂 processResponse × " + n);
        } catch (Throwable t) {
            HookLog.e("TimHb install 失败", t);
        }
    }

    public static void cacheFromNativeRequest(Object ssoRequest) {
        try {
            if (ssoRequest == null) return;
            Object cmd = Refl.field(ssoRequest, "paySsoCmd");
            Object url = Refl.field(ssoRequest, "originUrl");
            if (cmd != null && cachedCmd == null) {
                cachedCmd = cmd;
                cachedUrl = url == null ? null : String.valueOf(url);
                HookLog.i("TimHb: 已缓存原生 PaySsoCmd url=" + cachedUrl
                        + " toString=" + cmd);
            }
        } catch (Throwable ignore) {}
    }

    public static boolean ready() {
        ensureInit();
        return CLS_REQ != null && CLS_TASK != null && CLS_CLIENT != null && M_SEND != null;
    }

    

    
    
    private static final String[] TRY_CMD = {
            "trpc.qqhb.qqhb_proxy.Handler.sso_handle",
            "", "trpc.qqhb.qqhb_proxy.Handler.sso_handle",
            "trpc.qqhb.qqhb_proxy.Handler.sso_handle",
    };
    private static final boolean[] TRY_ENC = {true, true, false, true};
    private static final boolean[] TRY_ECDH = {true, true, false, false};

    public static int tryCount() { return TRY_CMD.length; }

    public static boolean send(String cgiName, String url, Map<String, String> params) {
        return send(cgiName, url, params, 0);
    }

    public static boolean send(String cgiName, String url, Map<String, String> params, int attempt) {
        ensureInit();
        if (!ready()) { HookLog.w("TimHb: 环境不全，发不了"); return false; }
        String uin = QQEnv.currentUin();
        if (uin == null) { HookLog.w("TimHb: 未登录"); return false; }
        try {
            Object client = newInstance(CLS_CLIENT);
            if (client == null) { HookLog.w("TimHb: HbProxyNetClient 构造失败"); return false; }

            int idx = Math.max(0, Math.min(attempt, TRY_CMD.length - 1));
            final String cmdStr = TRY_CMD[idx];
            final boolean enc = TRY_ENC[idx], ecdh = TRY_ECDH[idx];
            HookLog.i("TimHb 尝试#" + idx + " cmd=" + (cmdStr.isEmpty() ? "(空)" : cmdStr)
                    + " enc=" + enc + " ecdh=" + ecdh);
            Object cb = makeNetCallback(url);
            Object cmd = acquireCmd(cgiName, cmdStr);
            Object ctxRef = makeCtxRef(QQEnv.appContext());

            Map<String, String> ordered = (params instanceof LinkedHashMap)
                    ? params : new LinkedHashMap<String, String>(params);

            
            Object req = newInstanceLogged(CLS_REQ, "PaySsoRequest(url,uin)", url, uin,
                    Boolean.valueOf(enc), Boolean.valueOf(ecdh), ordered, cmd, cb, ctxRef);
            HookLog.i("TimHb fullUrl = " + fullUrlOf(req));
            if (!saneUrl(req)) {
                Object req2 = newInstanceLogged(CLS_REQ, "PaySsoRequest(uin,url)", uin, url,
                        Boolean.valueOf(enc), Boolean.valueOf(ecdh), ordered, cmd, cb, ctxRef);
                if (saneUrl(req2)) { req = req2; HookLog.i("TimHb 换序后 fullUrl = " + fullUrlOf(req2)); }
            }
            if (req == null) {
                HookLog.w("TimHb: PaySsoRequest 构造失败");
                return false;
            }

            Object task = newInstanceLogged(CLS_TASK, "PaySsoTask", client, req);
            if (task == null) return false;

            Object f1 = makeFunction1();
            M_SEND.setAccessible(true);
            M_SEND.invoke(client, task, f1);
            HookLog.i("TimHb 已发: url=" + url + " n=" + ordered.size() + " cmd=" + cmd);
            return true;
        } catch (Throwable t) {
            HookLog.e("TimHb.send 失败", t);
            return false;
        }
    }

    

    private static Object makeNetCallback(final String url) {
        if (CLS_CB == null) return null;
        try {
            return Proxy.newProxyInstance(CLS_CB.getClassLoader(), new Class<?>[]{CLS_CB},
                    new InvocationHandler() {
                @Override public Object invoke(Object proxy, Method method, Object[] args) {
                    String n = method.getName();
                    if ("toString".equals(n)) return "TimHbCallback";
                    if ("hashCode".equals(n)) return System.identityHashCode(proxy);
                    if ("equals".equals(n)) return proxy == (args != null && args.length > 0 ? args[0] : null);
                    try {
                        HookLog.i("TimHb 回调 " + n + " url=" + url);
                        if ("onNetSucceed".equals(n)) {
                            Object res = (args != null && args.length > 1) ? args[1] : null;
                            String json = extractJson(res);
                            Sink s = sink;
                            if (s != null) s.onResponse(true, json, url);
                        } else if ("onNetFailed".equals(n)) {
                            String json = extractJson(args != null && args.length > 1 ? args[1] : null);
                            Sink s = sink;
                            if (s != null) s.onResponse(false, json, url);
                        }
                    } catch (Throwable t) {
                        HookLog.e("TimHb 回调异常", t);
                    }
                    return null;
                }
            });
        } catch (Throwable t) {
            HookLog.e("makeNetCallback 失败", t);
            return null;
        }
    }

    private static String extractJson(Object netResult) {
        if (netResult == null) return null;
        try {
            Object biz = Refl.field(netResult, "bizResponse");
            if (biz instanceof String) return (String) biz;
            if (biz != null) return String.valueOf(biz);
            Object req = Refl.field(netResult, "httpRequest");
            return req == null ? null : null;
        } catch (Throwable t) {
            return null;
        }
    }

    private static Object makeFunction1() {
        if (CLS_FUNC1 == null) return null;
        try {
            return Proxy.newProxyInstance(CLS_FUNC1.getClassLoader(), new Class<?>[]{CLS_FUNC1},
                    new InvocationHandler() {
                @Override public Object invoke(Object proxy, Method method, Object[] args) {
                    String n = method.getName();
                    if ("toString".equals(n)) return "TimHbFunc1";
                    if ("hashCode".equals(n)) return System.identityHashCode(proxy);
                    if ("equals".equals(n)) return proxy == (args != null && args.length > 0 ? args[0] : null);
                    try {
                        HookLog.i("TimHb Function1." + n + " 被调用");
                        
                        ClassLoader cl = QQEnv.classLoader();
                        Class<?> u = find(cl, "kotlin.Unit");
                        if (u != null) {
                            try { return u.getField("INSTANCE").get(null); } catch (Throwable ignore) {}
                        }
                    } catch (Throwable ignore) {}
                    return null;
                }
            });
        } catch (Throwable t) {
            return null;
        }
    }

    

    private static Class<?> find(ClassLoader cl, String name) {
        try { return Class.forName(name, false, cl); } catch (Throwable ignore) {}
        try { return de.robv.android.xposed.XposedHelpers.findClassIfExists(name, cl); }
        catch (Throwable ignore) {}
        return null;
    }

    private static Object makeCtxRef(android.content.Context ctx) {
        try {
            Class<?> c = find(QQEnv.classLoader(), "mqq.util.WeakReference");
            if (c != null) {
                Object o = newInstanceLogged(c, "mqq.util.WeakReference", ctx);
                if (o != null) return o;
            }
        } catch (Throwable t) {
            HookLog.e("makeCtxRef 失败", t);
        }
        return new WeakReference<android.content.Context>(ctx);   
    }

    private static Object acquireCmd(String cgiName, String url) {
        
        Class<?> hb = null;
        try {
            for (Class<?> ic : CLS_CMD.getDeclaredClasses()) {
                if ("HbProxy".equals(ic.getSimpleName())) { hb = ic; break; }
            }
        } catch (Throwable ignore) {}
        if (hb != null) {
            
            String[] second = {
                    "trpc.qqhb.qqhb_proxy.Handler.sso_handle",   
                    cgiName,
                    "",
            };
            for (String s2 : second) {
                Object v = newInstanceLogged(hb, "HbProxy(" + cgiName + "," + s2 + ")", cgiName, s2);
                if (v != null) {
                    HookLog.i("TimHb: PaySsoCmd = HbProxy(" + cgiName + ", "
                            + (s2.length() > 30 ? "<url>" : s2) + ")");
                    return v;   
                }
            }
        }
        if (cachedCmd != null) {
            HookLog.i("TimHb: 退回原生缓存的 PaySsoCmd");
            return cachedCmd;
        }
        HookLog.w("TimHb: PaySsoCmd 拿不到");
        return null;
    }

    private static Object acquireCmdLegacy() {
        if (cachedCmd != null) return cachedCmd;
        if (CLS_CMD == null) { HookLog.w("TimHb: PaySsoCmd 类不存在"); return null; }
        
        Object c = newInstanceLogged(CLS_CMD, "PaySsoCmd");
        if (c != null) { cachedCmd = c; HookLog.i("TimHb: PaySsoCmd = new"); return c; }
        
        try {
            Class<?>[] inner = CLS_CMD.getDeclaredClasses();
            HookLog.i("TimHb: PaySsoCmd 嵌套类 " + inner.length + " 个");
            for (Class<?> ic : inner) {
                HookLog.i("TimHb:   嵌套 " + ic.getName()
                        + (java.lang.reflect.Modifier.isAbstract(ic.getModifiers()) ? " (抽象)" : ""));
                if (CLS_CMD.isAssignableFrom(ic)
                        && !java.lang.reflect.Modifier.isAbstract(ic.getModifiers())) {
                    
                    try {
                        for (java.lang.reflect.Field f : ic.getDeclaredFields()) {
                            if (!java.lang.reflect.Modifier.isStatic(f.getModifiers())) continue;
                            if (!CLS_CMD.isAssignableFrom(f.getType())) continue;
                            f.setAccessible(true);
                            Object v = f.get(null);
                            if (v != null) {
                                cachedCmd = v;
                                HookLog.i("TimHb: PaySsoCmd = " + ic.getSimpleName()
                                        + " 静态字段 " + f.getName());
                                return v;
                            }
                        }
                    } catch (Throwable t) {
                        HookLog.e("TimHb: 嵌套类静态字段失败", t);
                    }
                    
                    Object v = autoInstance(ic, "PaySsoCmd$" + ic.getSimpleName());
                    if (v != null) {
                        cachedCmd = v;
                        HookLog.i("TimHb: PaySsoCmd = 自动填参 " + ic.getName());
                        return v;
                    }
                }
            }
        } catch (Throwable t) {
            HookLog.e("TimHb: 嵌套类探测失败", t);
        }

        try {
            for (java.lang.reflect.Field f : CLS_CMD.getDeclaredFields()) {
                if (!java.lang.reflect.Modifier.isStatic(f.getModifiers())) continue;
                if (!CLS_CMD.isAssignableFrom(f.getType())) continue;
                f.setAccessible(true);
                Object v = f.get(null);
                if (v != null) {
                    cachedCmd = v;
                    HookLog.i("TimHb: PaySsoCmd = 静态字段 " + f.getName());
                    return v;
                }
            }
        } catch (Throwable t) {
            HookLog.e("TimHb: PaySsoCmd 静态字段失败", t);
        }
        
        try {
            Class<?> uc = Class.forName("sun.misc.Unsafe");
            java.lang.reflect.Field uf = uc.getDeclaredField("theUnsafe");
            uf.setAccessible(true);
            Object unsafe = uf.get(null);
            Method alloc = uc.getMethod("allocateInstance", Class.class);
            Object v = alloc.invoke(unsafe, CLS_CMD);
            if (v != null) {
                cachedCmd = v;
                HookLog.i("TimHb: PaySsoCmd = Unsafe.allocateInstance");
                return v;
            }
        } catch (Throwable t) {
            HookLog.e("TimHb: Unsafe 分配失败", t);
        }
        HookLog.w("TimHb: PaySsoCmd 全部获取方式失败，传 null");
        return null;
    }

    private static String fullUrlOf(Object req) {
        if (req == null) return "(null)";
        Object v = Refl.field(req, "fullUrl");
        return v == null ? "(null)" : String.valueOf(v);
    }

    private static boolean saneUrl(Object req) {
        String u = fullUrlOf(req);
        return u.startsWith("http") && u.indexOf("tenpay") > 0 && u.indexOf(".cgi") > 0;
    }

    private static Object autoInstance(Class<?> cls, String what) {
        if (cls == null) return null;
        for (Constructor<?> c : cls.getDeclaredConstructors()) {
            Class<?>[] ps = c.getParameterTypes();
            StringBuilder sig = new StringBuilder();
            Object[] a = new Object[ps.length];
            for (int i = 0; i < ps.length; i++) {
                if (i > 0) sig.append(", ");
                sig.append(ps[i].getSimpleName());
                a[i] = defaultOf(ps[i]);
            }
            try {
                c.setAccessible(true);
                Object o = c.newInstance(a);
                if (o != null) {
                    HookLog.i("TimHb: " + what + "(" + sig + ") ✓ 默认参成功");
                    return o;
                }
            } catch (Throwable t) {
                Throwable cause = (t.getCause() != null) ? t.getCause() : t;
                HookLog.w("TimHb: " + what + "(" + sig + ") ✗ "
                        + cause.getClass().getSimpleName() + ": " + cause.getMessage());
            }
        }
        return null;
    }

    private static Object defaultOf(Class<?> t) {
        if (!t.isPrimitive()) return null;
        if (t == boolean.class) return Boolean.FALSE;
        if (t == int.class) return Integer.valueOf(0);
        if (t == long.class) return Long.valueOf(0L);
        if (t == short.class) return Short.valueOf((short) 0);
        if (t == byte.class) return Byte.valueOf((byte) 0);
        if (t == char.class) return Character.valueOf(' ');
        if (t == float.class) return Float.valueOf(0f);
        if (t == double.class) return Double.valueOf(0d);
        return null;
    }

    private static Object newInstanceLogged(Class<?> cls, String what, Object... args) {
        if (cls == null) { HookLog.w("TimHb: " + what + " 类为 null"); return null; }
        Object[] a = (args == null) ? new Object[0] : args;
        StringBuilder tried = new StringBuilder();
        for (Constructor<?> c : cls.getDeclaredConstructors()) {
            if (c.getParameterTypes().length != a.length) continue;
            try {
                c.setAccessible(true);
                Object o = c.newInstance(a);
                if (o != null) return o;
            } catch (Throwable t) {
                Throwable cause = (t.getCause() != null) ? t.getCause() : t;
                tried.append(" | ").append(cause.getClass().getSimpleName())
                     .append(": ").append(cause.getMessage());
            }
        }
        HookLog.w("TimHb: " + what + " 构造失败 ->" + tried);
        return null;
    }

    private static Object newInstance(Class<?> cls, Object... args) {
        if (cls == null) return null;
        Object[] a = (args == null) ? new Object[0] : args;
        for (Constructor<?> c : cls.getDeclaredConstructors()) {
            if (c.getParameterTypes().length != a.length) continue;
            try {
                c.setAccessible(true);
                return c.newInstance(a);
            } catch (Throwable ignore) {}
        }
        return null;
    }
}
