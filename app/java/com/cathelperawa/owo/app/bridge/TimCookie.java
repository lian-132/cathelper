package com.cathelperawa.owo.app.bridge;

import com.cathelperawa.owo.app.core.HookLog;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public final class TimCookie {

    private TimCookie() {}

    private static Class<?> CLS_PSKEY_MGR;
    private static Class<?> CLS_PSKEY_REQ;
    private static Method M_ON_CREATE;
    private static Method M_FROM_LOCAL;
    private static Method M_TO_RETURN;

    private static Object pskeyMgr;
    private static Object pskeyMgrOwner;

    private static boolean inited = false;

    private static void ensureInit() {
        if (inited) return;
        inited = true;
        try {
            ClassLoader cl = QQEnv.classLoader();
            CLS_PSKEY_MGR = findClass(cl, "com.tencent.mobileqq.pskey.api.impl.PskeyManagerImpl");
            CLS_PSKEY_REQ = findClass(cl, "com.tencent.mobileqq.pskey.a");
            if (CLS_PSKEY_MGR == null) {
                HookLog.w("TimCookie: 未找到 PskeyManagerImpl");
                return;
            }
            M_ON_CREATE = findMethod(CLS_PSKEY_MGR, "onCreate", 1);
            M_FROM_LOCAL = findMethod(CLS_PSKEY_MGR, "getPskeyFromLocal", 2);
            M_TO_RETURN = findMethod(CLS_PSKEY_MGR, "toReturnType", 2);
            HookLog.i("TimCookie 初始化: mgr=" + (CLS_PSKEY_MGR != null)
                    + " req=" + (CLS_PSKEY_REQ != null)
                    + " onCreate=" + (M_ON_CREATE != null)
                    + " fromLocal=" + (M_FROM_LOCAL != null)
                    + " toReturn=" + (M_TO_RETURN != null));
        } catch (Throwable t) {
            HookLog.e("TimCookie 初始化失败", t);
        }
    }

    private static Object pskeyManager() {
        ensureInit();
        if (CLS_PSKEY_MGR == null || M_ON_CREATE == null) return null;
        Object rt = QQEnv.appRuntime();
        if (rt == null) return null;
        try {
            if (pskeyMgr == null || pskeyMgrOwner != rt) {
                pskeyMgr = newInstance(CLS_PSKEY_MGR);
                if (pskeyMgr == null) return null;
                M_ON_CREATE.setAccessible(true);
                M_ON_CREATE.invoke(pskeyMgr, rt);
                pskeyMgrOwner = rt;
                HookLog.i("TimCookie: PskeyManagerImpl 已绑定 AppRuntime");
            }
            return pskeyMgr;
        } catch (Throwable t) {
            HookLog.e("pskeyManager 失败", t);
            return null;
        }
    }

    public static String getPskey(String url) {
        if (url == null || url.isEmpty()) return null;
        Object mgr = pskeyManager();
        if (mgr == null || M_FROM_LOCAL == null || M_TO_RETURN == null) return null;
        String uin = QQEnv.currentUin();
        if (uin == null) return null;
        try {

            Object req = null;
            if (CLS_PSKEY_REQ != null) {
                req = newInstance(CLS_PSKEY_REQ, "", "", Integer.valueOf(0));
            }
            Map<String, Object> ask = new HashMap<String, Object>();
            ask.put(url, req);

            M_FROM_LOCAL.setAccessible(true);
            Object raw = M_FROM_LOCAL.invoke(mgr, uin, ask);
            if (raw == null) return null;

            M_TO_RETURN.setAccessible(true);
            Object out = M_TO_RETURN.invoke(mgr, raw, Integer.valueOf(0));
            if (!(out instanceof Map)) return null;
            Map<?, ?> m = (Map<?, ?>) out;
            Object v = m.get(url);
            if (v == null) {

                String host = hostOf(url);
                if (host != null && !host.equals(url)) {
                    v = m.get(host);
                }
            }
            return v == null ? null : String.valueOf(v);
        } catch (Throwable t) {
            HookLog.e("getPskey 失败 url=" + url, t);
            return null;
        }
    }

    public static Map<String, String> getPskeyBatch(String... urls) {
        Map<String, String> result = new HashMap<String, String>();
        if (urls == null || urls.length == 0) return result;
        Object mgr = pskeyManager();
        if (mgr == null || M_FROM_LOCAL == null || M_TO_RETURN == null) return result;
        String uin = QQEnv.currentUin();
        if (uin == null) return result;
        try {
            Map<String, Object> ask = new HashMap<String, Object>();
            for (String u : urls) {
                if (u == null || u.isEmpty()) continue;
                Object req = (CLS_PSKEY_REQ != null) ? newInstance(CLS_PSKEY_REQ, "", "", Integer.valueOf(0)) : null;
                ask.put(u, req);
            }
            if (ask.isEmpty()) return result;
            M_FROM_LOCAL.setAccessible(true);
            Object raw = M_FROM_LOCAL.invoke(mgr, uin, ask);
            if (raw == null) return result;
            M_TO_RETURN.setAccessible(true);
            Object out = M_TO_RETURN.invoke(mgr, raw, Integer.valueOf(0));
            if (!(out instanceof Map)) return result;
            Map<?, ?> m = (Map<?, ?>) out;
            for (String u : urls) {
                Object v = m.get(u);
                if (v != null) result.put(u, String.valueOf(v));
            }
            return result;
        } catch (Throwable t) {
            HookLog.e("getPskeyBatch 失败", t);
            return result;
        }
    }

    private static Class<?> CLS_QQWALLET_API;
    private static Method M_HEX_GUID;
    private static Object qWalletApi;

    public static String hexGuid() {
        try {
            ClassLoader cl = QQEnv.classLoader();
            if (CLS_QQWALLET_API == null) {
                CLS_QQWALLET_API = findClass(cl,
                        "com.tencent.mobileqq.qwallet.api.impl.NewQWalletApiImpl");
                if (CLS_QQWALLET_API != null) {
                    M_HEX_GUID = findMethod(CLS_QQWALLET_API, "hexGuid", 0);
                }
            }
            if (CLS_QQWALLET_API == null || M_HEX_GUID == null) return null;
            if (qWalletApi == null) qWalletApi = newInstance(CLS_QQWALLET_API);
            if (qWalletApi == null) return null;
            M_HEX_GUID.setAccessible(true);
            Object v = M_HEX_GUID.invoke(qWalletApi);
            return v == null ? null : String.valueOf(v);
        } catch (Throwable t) {
            HookLog.e("hexGuid 失败", t);
            return null;
        }
    }

    private static Class<?> CLS_TICKET_MGR;
    private static Method M_SKEY_1;
    private static Method M_STWEB;
    private static Method M_PT4;
    private static Method M_REAL_SKEY;

    private static void ensureTicket() {
        try {
            ClassLoader cl = QQEnv.classLoader();
            if (CLS_TICKET_MGR == null) {
                CLS_TICKET_MGR = findClass(cl, "mqq.app.TicketManagerImpl");
                if (CLS_TICKET_MGR != null) {
                    M_SKEY_1 = findMethod(CLS_TICKET_MGR, "getSkey", 1);
                    M_STWEB = findMethod(CLS_TICKET_MGR, "getStweb", 0);
                    M_PT4 = findMethod(CLS_TICKET_MGR, "getPt4Token", 2);
                    M_REAL_SKEY = findMethod(CLS_TICKET_MGR, "getRealSkey", 1);
                }
            }
        } catch (Throwable t) {
            HookLog.e("ensureTicket 失败", t);
        }
    }

    private static Object ticketManager() {
        ensureTicket();
        if (CLS_TICKET_MGR == null) return null;
        Object rt = QQEnv.appRuntime();
        if (rt == null) return null;
        try {
            return newInstance(CLS_TICKET_MGR, rt);
        } catch (Throwable t) {
            return null;
        }
    }

    public static String getSkey() {
        Object tm = ticketManager();
        if (tm == null || M_SKEY_1 == null) return null;
        String uin = QQEnv.currentUin();
        if (uin == null) return null;
        try {
            M_SKEY_1.setAccessible(true);
            Object v = M_SKEY_1.invoke(tm, uin);
            return v == null ? null : String.valueOf(v);
        } catch (Throwable t) {
            return null;
        }
    }

    public static String getRealSkey() {
        Object tm = ticketManager();
        if (tm == null || M_REAL_SKEY == null) return null;
        String uin = QQEnv.currentUin();
        if (uin == null) return null;
        try {
            M_REAL_SKEY.setAccessible(true);
            Object v = M_REAL_SKEY.invoke(tm, uin);
            return v == null ? null : String.valueOf(v);
        } catch (Throwable t) {
            return null;
        }
    }

    public static String getStweb() {
        Object tm = ticketManager();
        if (tm == null || M_STWEB == null) return null;
        try {
            M_STWEB.setAccessible(true);
            Object v = M_STWEB.invoke(tm);
            return v == null ? null : String.valueOf(v);
        } catch (Throwable t) {
            return null;
        }
    }

    public static String getPt4Token(String url) {
        Object tm = ticketManager();
        if (tm == null || M_PT4 == null) return null;
        String uin = QQEnv.currentUin();
        if (uin == null || url == null) return null;
        try {
            M_PT4.setAccessible(true);
            Object v = M_PT4.invoke(tm, uin, url);
            return v == null ? null : String.valueOf(v);
        } catch (Throwable t) {
            return null;
        }
    }

    private static Class<?> CLS_TROOP_HW;
    private static Method M_BKN;

    public static long getBkn(String skey) {
        if (skey == null || skey.isEmpty()) return 0L;
        try {
            if (CLS_TROOP_HW == null) {
                CLS_TROOP_HW = findClass(QQEnv.classLoader(),
                        "com.tencent.mobileqq.troop.api.impl.TroopHWApiImpl");
                if (CLS_TROOP_HW != null) M_BKN = findMethod(CLS_TROOP_HW, "getBknBySkey", 1);
            }
            if (CLS_TROOP_HW == null || M_BKN == null) return 0L;
            Object impl = newInstance(CLS_TROOP_HW);
            if (impl == null) return 0L;
            M_BKN.setAccessible(true);
            Object v = M_BKN.invoke(impl, skey);
            return v instanceof Number ? ((Number) v).longValue() : 0L;
        } catch (Throwable t) {
            return 0L;
        }
    }

    public static String getGTK(String url) {
        String pskey = getPskey(url);
        if (pskey == null || pskey.isEmpty()) return null;
        int h = 5381;
        for (int i = 0; i < pskey.length(); i++) {
            h += (h << 5) + pskey.charAt(i);
        }
        return String.valueOf(Integer.MAX_VALUE & h);
    }

    public static long bknOf(String key) {
        if (key == null || key.isEmpty()) return 0L;
        long h = 5381L;
        for (int i = 0; i < key.length(); i++) {
            h += (h << 5) + key.charAt(i);
        }
        return h & 0x7FFFFFFFL;
    }

    public static String hostOf(String url) {
        if (url == null) return null;
        String s = url;
        int p = s.indexOf("://");
        if (p >= 0) s = s.substring(p + 3);
        int slash = s.indexOf('/');
        if (slash >= 0) s = s.substring(0, slash);
        int colon = s.indexOf(':');
        if (colon >= 0) s = s.substring(0, colon);
        return s.isEmpty() ? null : s;
    }

    private static Class<?> findClass(ClassLoader cl, String name) {
        try {
            return Class.forName(name, false, cl);
        } catch (Throwable ignore) {
        }
        try {
            return de.robv.android.xposed.XposedHelpers.findClassIfExists(name, cl);
        } catch (Throwable ignore) {
        }
        return null;
    }

    private static Method findMethod(Class<?> cls, String name, int argc) {
        if (cls == null) return null;
        Class<?> c = cls;
        while (c != null && c != Object.class) {
            for (Method m : c.getDeclaredMethods()) {
                if (m.getName().equals(name) && m.getParameterTypes().length == argc) return m;
            }
            c = c.getSuperclass();
        }
        return null;
    }

    private static Object newInstance(Class<?> cls, Object... args) {
        if (cls == null) return null;
        for (java.lang.reflect.Constructor<?> c : cls.getDeclaredConstructors()) {
            if (c.getParameterTypes().length != args.length) continue;
            try {
                c.setAccessible(true);
                return c.newInstance(args);
            } catch (Throwable ignore) {
            }
        }
        return null;
    }
}
