package com.cathelperawa.owo.app.bridge;

import com.cathelperawa.owo.app.MsgStore;
import com.cathelperawa.owo.app.core.dex.ClassResolver;
import com.cathelperawa.owo.app.core.HookLog;
import com.cathelperawa.owo.app.util.Refl;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public final class TroopApi {

    private TroopApi() {}

    public static String getMemberName(String troopUin, String uin) {
        if (troopUin == null || uin == null || uin.isEmpty()) return uin;
        try {
            ClassLoader cl = QQEnv.classLoader();
            Class<?> repoCls = ClassResolver.find(cl, "troop.memberListRepo",
                    "com.tencent.qqnt.troopmemberlist.TroopMemberListRepo");
            java.lang.reflect.Constructor<?> ctor = repoCls.getDeclaredConstructor();
            ctor.setAccessible(true);
            Object repo = ctor.newInstance();

            Method fetch = null;
            for (Method m : repoCls.getMethods()) {
                if ("fetchTroopMemberInfo".equals(m.getName())
                        && m.getParameterTypes().length == 6) { fetch = m; break; }
            }
            if (fetch == null) return uin;

            final Class<?> memberCls = ClassResolver.find(cl, "troop.memberInfo",
                    "com.tencent.mobileqq.data.troop.TroopMemberInfo");
            final Class<?> cbCls = fetch.getParameterTypes()[5];
            final CompletableFuture<Object> fut = new CompletableFuture<Object>();
            Object cb = Proxy.newProxyInstance(cl, new Class<?>[]{cbCls}, new InvocationHandler() {
                @Override public Object invoke(Object p, Method m, Object[] a) {
                    if (m.getReturnType() == void.class && a != null && a.length >= 1
                            && a[0] != null && memberCls.isInstance(a[0])) {
                        fut.complete(a[0]);
                        return null;
                    }
                    Class<?> rt = m.getReturnType();
                    if (rt == boolean.class) return Boolean.FALSE;
                    if (rt == int.class) return 0;
                    if (rt == long.class) return 0L;
                    return null;
                }
            });
            fetch.setAccessible(true);
            fetch.invoke(repo, troopUin, uin, Boolean.TRUE, null, "", cb);

            Object info = fut.get(3, TimeUnit.SECONDS);
            String name = Refl.str(Refl.field(info, "troopnick"));
            if (name == null || name.trim().isEmpty()) name = Refl.str(Refl.field(info, "autoremark"));
            if (name == null || name.trim().isEmpty()) name = Refl.str(Refl.field(info, "friendnick"));
            return (name == null || name.trim().isEmpty()) ? uin : name;
        } catch (Throwable t) {
            HookLog.w("取群昵称失败: " + t);
            return uin;
        }
    }

    public static Map<String, String> allTroopNames() {
        synchronized (TROOP_NAMES) {
            if (troopNamesLoaded) return new java.util.HashMap<String, String>(TROOP_NAMES);
        }
        try {
            ClassLoader cl = QQEnv.classLoader();
            Class<?> impl = ClassResolver.find(cl, "troop.listRepoImpl",
                    "com.tencent.qqnt.troop.impl.TroopListRepoApiImpl");

            Object repo = null;
            for (String gn : new String[]{"getInstance", "getService", "INSTANCE"}) {
                try {
                    Method g = impl.getMethod(gn);
                    g.setAccessible(true);
                    Object v = g.invoke(null);
                    if (v != null) { repo = v; break; }
                } catch (Throwable ignore) {
                }
            }
            if (repo == null) {
                java.lang.reflect.Constructor<?> ctor = impl.getDeclaredConstructor();
                ctor.setAccessible(true);
                repo = ctor.newInstance();
            }

            Method getList = null;
            String[] wantM = {
                    "getSortedJoinedTroopInfoFromCache",
                    "getTroopListFromCache",
                    "getJoinedTroopInfoFromCache",
                    "getSortedJoinedValidTroopInfoFromCache",
            };
            for (String wn : wantM) {
                for (Method m : impl.getMethods()) {
                    if (!wn.equals(m.getName())) continue;
                    if (m.getParameterTypes().length != 0) continue;
                    getList = m;
                    break;
                }
                if (getList != null) break;
            }
            if (getList == null) {
                HookLog.w("群名：未找到 getSortedJoinedTroopInfoFromCache");
                return new java.util.HashMap<String, String>();
            }
            getList.setAccessible(true);
            Object res = getList.invoke(repo);
            if (res instanceof List) {
                for (Object ti : (List<?>) res) {
                    if (ti == null) continue;
                    
                    String uin = firstStr(ti, "troopuin", "troopUin", "troopcode", "uin", "groupCode");
                    String nm  = firstStr(ti, "troopname", "troopName", "troopNameFromNT", "name");
                    if (uin == null || uin.trim().isEmpty()) continue;
                    if (nm == null) nm = "";
                    TROOP_NAMES.put(uin.trim(), nm.trim());
                }
            }

            if (!TROOP_NAMES.isEmpty()) troopNamesLoaded = true;
            HookLog.i("群名：已缓存 " + TROOP_NAMES.size() + " 个群名");
            MsgStore.add("[群名] 已缓存 " + TROOP_NAMES.size() + " 个群名");
        } catch (Throwable t) {
            HookLog.w("群名：批量获取失败 " + t);
            MsgStore.add("[群名] 批量获取失败 " + t);
        }
        synchronized (TROOP_NAMES) {
            return new java.util.HashMap<String, String>(TROOP_NAMES);
        }
    }

    private static final Map<String, String> TROOP_NAMES =
            new java.util.HashMap<String, String>();
    private static volatile boolean troopNamesLoaded = false;

    public static String lookupTroopName(String troopUin) {
        if (troopUin == null || troopUin.isEmpty()) return null;
        synchronized (TROOP_NAMES) {
            String n = TROOP_NAMES.get(troopUin);
            if (n != null) return n;
        }
        allTroopNames();
        synchronized (TROOP_NAMES) {
            return TROOP_NAMES.get(troopUin);
        }
    }

    public static String getTroopName(String troopUin) {
        
        String cached = lookupTroopName(troopUin);
        if (notBlank(cached)) return cached;
        if (troopUin == null || troopUin.isEmpty()) return null;
        ClassLoader cl = QQEnv.classLoader();
        
        try {
            Object info = troopInfoOf(cl, troopUin);
            String n = troopNameOf(info);
            if (notBlank(n)) return n;
        } catch (Throwable ignore) {
        }
        
        try {
            Object rt = QQEnv.appRuntime();
            if (rt != null) {
                String n = callGetTroopName(rt, troopUin);
                if (notBlank(n)) return n;
            }
        } catch (Throwable ignore) {
        }
        return null;
    }

    private static String callGetTroopName(Object target, String troopUin) {
        if (target == null) return null;
        for (Method m : target.getClass().getMethods()) {
            String nm = m.getName();
            if (!nm.contains("TroopName") && !nm.contains("getTroopName")) continue;
            Class<?>[] ps = m.getParameterTypes();
            if (ps.length != 1 || ps[0] != String.class) continue;
            if (!CharSequence.class.isAssignableFrom(m.getReturnType())
                    && m.getReturnType() != String.class) continue;
            try {
                m.setAccessible(true);
                Object v = m.invoke(target, troopUin);
                if (v != null && notBlank(String.valueOf(v))) return String.valueOf(v);
            } catch (Throwable ignore) {
            }
        }
        return null;
    }

    private static Object troopInfoOf(ClassLoader cl, String troopUin) throws Throwable {
        String[] hosts = new String[]{
                "com.tencent.mobileqq.troop.api.ITroopInfoService",
                "com.tencent.mobileqq.troop.troopmanager.api.ITroopManagerBizHandler",
                "com.tencent.qqnt.troop.ITroopInfoRepoApi",
        };
        for (String hn : hosts) {
            Class<?> c = Class.forName(hn, false, cl);
            Object svc = serviceOf(cl, c);
            if (svc == null) continue;
            for (Method m : svc.getClass().getMethods()) {
                if (m.getParameterTypes().length != 1) continue;
                if (m.getParameterTypes()[0] != String.class) continue;
                String nm = m.getName();
                if (!nm.contains("getTroopInfo") && !nm.contains("TroopInfoSync")
                        && !nm.contains("GetTroopInfo")) continue;
                try {
                    m.setAccessible(true);
                    Object v = m.invoke(svc, troopUin);
                    if (v != null) return v;
                } catch (Throwable ignore) {
                }
            }
        }
        return null;
    }

    private static String troopNameOf(Object info) {
        if (info == null) return null;
        for (String f : new String[]{"troopname", "troopName", "mTroopName", "name"}) {
            String v = Refl.str(Refl.field(info, f));
            if (notBlank(v)) return v;
        }
        for (String mn : new String[]{"getTroopName", "getTroopDisplayName"}) {
            try {
                Method m = info.getClass().getMethod(mn);
                m.setAccessible(true);
                Object v = m.invoke(info);
                if (v != null && notBlank(String.valueOf(v))) return String.valueOf(v);
            } catch (Throwable ignore) {
            }
        }
        return null;
    }

    private static final ClassResolver.Spec DI_SPEC =
            new ClassResolver.Spec("troop.di")
                    .candidates("com.tencent.mobileqq.qroute.QRoute",
                                "com.tencent.qqnt.kernel.api.QQNTServiceFactory")
                    .nameMatches("\\bQRoute$")
                    .validator(new ClassResolver.Validator() {
                        @Override public boolean accept(Class<?> c) {
                            try {
                                java.lang.reflect.Method m = c.getMethod("api", Class.class);
                                return java.lang.reflect.Modifier.isStatic(m.getModifiers());
                            } catch (Throwable t) {
                                return false;
                            }
                        }
                    });

    private static android.content.Context diContext() {
        try {
            return QQEnv.appContext();
        } catch (Throwable t) {
            return null;
        }
    }

    private static final String[] DI_CANDIDATES = {
            "com.tencent.mobileqq.qroute.QRoute",
            "com.tencent.qqnt.kernel.api.QQNTServiceFactory",
    };

    private static Object serviceOf(ClassLoader cl, Class<?> iface) {
        
        try {
            Class<?> qRoute = ClassResolver.resolve(diContext(), cl, DI_SPEC);
            if (qRoute != null) {
                Method api = qRoute.getMethod("api", Class.class);
                api.setAccessible(true);
                Object v = api.invoke(null, iface);
                if (v != null) return v;
            }
        } catch (Throwable ignore) {
        }
        
        for (int i = 1; i < DI_CANDIDATES.length; i++) {
            try {
                Class<?> factory = Class.forName(DI_CANDIDATES[i], false, cl);
                for (Method m : factory.getMethods()) {
                    if (m.getParameterTypes().length == 1
                            && m.getParameterTypes()[0].isAssignableFrom(iface)) {
                        m.setAccessible(true);
                        Object v = m.invoke(null, iface);
                        if (v != null) return v;
                    }
                }
            } catch (Throwable ignore) {
            }
        }
        try {
            Object rt = QQEnv.appRuntime();
            if (rt != null) {
                for (Method m : rt.getClass().getMethods()) {
                    if (m.getParameterTypes().length != 1) continue;
                    if (!m.getParameterTypes()[0].isAssignableFrom(iface)) continue;
                    m.setAccessible(true);
                    Object v = m.invoke(rt, iface);
                    if (v != null) return v;
                }
            }
        } catch (Throwable ignore) {
        }
        return null;
    }

    private static boolean notBlank(String s) {
        return s != null && s.trim().length() > 0;
    }

    private static String firstStr(Object obj, String... names) {
        for (String n : names) {
            try {
                String v = Refl.str(Refl.field(obj, n));
                if (v != null && !v.trim().isEmpty()) return v;
            } catch (Throwable ignore) {
            }
        }
        return null;
    }
}
