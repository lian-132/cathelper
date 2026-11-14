package com.cathelperawa.owo.app.ui;

import com.cathelperawa.owo.app.bridge.QQEnv;

import java.lang.reflect.Method;

public final class HostInfo {

    private HostInfo() {}

    public static String uin() {
        try {
            String v = QQEnv.currentUin();
            return empty(v) ? null : v;
        } catch (Throwable t) {
            return null;
        }
    }

    public static String nick() {
        try {
            String v = QQEnv.currentNickname();
            if (!empty(v)) return v;
        } catch (Throwable ignore) {
        }

        try {
            Object rt = QQEnv.appRuntime();
            if (rt == null) return null;
            String[] names = {"getCurrentNickname", "getCurrentNickName",
                    "getNickName", "getCurrentUinName", "getCurrentUserState"};
            for (Method m : rt.getClass().getMethods()) {
                if (m.getParameterTypes().length != 0) continue;
                boolean hit = false;
                for (String n : names) {
                    if (n.equals(m.getName())) { hit = true; break; }
                }
                if (!hit) continue;
                try {
                    m.setAccessible(true);
                    Object v = m.invoke(rt);
                    if (v != null && !empty(String.valueOf(v))) return String.valueOf(v);
                } catch (Throwable ignore2) {
                }
            }
        } catch (Throwable ignore) {
        }
        return null;
    }

    public static String avatarUrl() {
        String uin = uin();
        if (empty(uin)) return null;
        return "https://q1.qlogo.cn/g?b=qq&nk=" + uin + "&s=640";
    }

    public static String hostVersion() {

        String name = null;
        try {
            Object rt = QQEnv.appRuntime();
            if (rt != null) {
                String[] names = {"getAppVersion", "getVersionName", "getAppVer"};
                for (Method m : rt.getClass().getMethods()) {
                    if (m.getParameterTypes().length != 0) continue;
                    boolean hit = false;
                    for (String n : names) {
                        if (n.equals(m.getName())) { hit = true; break; }
                    }
                    if (!hit) continue;
                    try {
                        m.setAccessible(true);
                        Object v = m.invoke(rt);
                        if (v != null && !empty(String.valueOf(v))) {
                            name = String.valueOf(v);
                            break;
                        }
                    } catch (Throwable ignore) {
                    }
                }
            }
        } catch (Throwable ignore) {
        }

        Integer code = null;
        try {
            Object rt = QQEnv.appRuntime();
            if (rt != null) {
                String[] names = {"getAppVersionCode", "getVersionCode"};
                for (Method m : rt.getClass().getMethods()) {
                    if (m.getParameterTypes().length != 0) continue;
                    boolean hit = false;
                    for (String n : names) {
                        if (n.equals(m.getName())) { hit = true; break; }
                    }
                    if (!hit) continue;
                    try {
                        m.setAccessible(true);
                        Object v = m.invoke(rt);
                        if (v instanceof Integer) { code = (Integer) v; break; }
                        if (v instanceof Long) { code = ((Long) v).intValue(); break; }
                    } catch (Throwable ignore) {
                    }
                }
            }
        } catch (Throwable ignore) {
        }

        if (name == null) {
            try {
                android.content.Context c = QQEnv.appContext();
                if (c != null) {
                    String pkg = c.getPackageName();
                    android.content.pm.PackageInfo pi =
                            c.getPackageManager().getPackageInfo(pkg, 0);
                    if (pi != null) {
                        if (pi.versionName != null) name = pi.versionName;
                        if (code == null) code = pi.versionCode;
                    }
                }
            } catch (Throwable ignore) {
            }
        }

        if (empty(name)) return null;
        return code == null || code <= 0 ? name : name + "(" + code + ")";
    }

    public static String loader() {
        boolean xposed = false;
        try {
            int v = de.robv.android.xposed.XposedBridge.getXposedVersion();
            xposed = v > 0;
        } catch (Throwable ignore) {
        }
        if (!xposed) return "未激活";

        ClassLoader cl = null;
        try {
            cl = QQEnv.classLoader();
        } catch (Throwable ignore) {
        }

        String maps = selfMaps();
        String base = identifyFramework(cl, maps);
        String inject = injectionType(maps);
        return inject == null ? base : (base + " · " + inject);
    }

    private static String identifyFramework(ClassLoader cl, String maps) {

        if (maps != null) {
            String m = maps.toLowerCase();
            if (m.contains("lsposed") || m.contains("lspd")) return "LSPosed";
            if (m.contains("edxposed")) return "EdXposed";
            if (m.contains("taichi")) return "TAICHI";
        }

        if (classExists("org.meowcat.edxposed.manager.XposedApp", cl)
                || classExists("de.robv.android.xposed.XposedBridgeEdXposed", cl)
                || classExists("com.elderdrivers.riru.edxp.config.EdXpConfigGlobal", cl)) {
            return "EdXposed";
        }
        if (classExists("me.weishu.exp.helper.VirtualConfig", cl)
                || classExists("me.weishu.exposed.ExposedBridge", cl)) {
            return "TAICHI";
        }

        String clName = bridgeClassLoaderName();
        if (clName != null && clName.toLowerCase().contains("inmemorydex")) {
            return "LSPosed";
        }

        if (classExists("de.robv.android.xposed.XposedBridge", cl)) return "Xposed";
        return "Xposed(未知实现)";
    }

    private static String bridgeClassLoaderName() {
        try {
            ClassLoader xbCl = de.robv.android.xposed.XposedBridge.class.getClassLoader();
            return xbCl == null ? "BOOT" : xbCl.getClass().getName();
        } catch (Throwable ignore) {
        }
        return null;
    }

    private static String injectionType(String maps) {
        if (maps == null) return null;
        String m = maps.toLowerCase();
        if (m.contains("zygisk")) return "Zygisk";
        if (m.contains("libriru") || m.contains("riru")) return "Riru";
        return null;
    }

    private static String selfMaps() {
        java.io.FileInputStream in = null;
        try {
            in = new java.io.FileInputStream("/proc/self/maps");
            byte[] buf = new byte[65536];
            StringBuilder sb = new StringBuilder();
            int n;
            int total = 0;
            while ((n = in.read(buf)) > 0 && total < 4 * 1024 * 1024) {
                sb.append(new String(buf, 0, n, "UTF-8"));
                total += n;
            }
            return sb.toString();
        } catch (Throwable ignore) {
            return null;
        } finally {
            try { if (in != null) in.close(); } catch (Throwable ignore) {}
        }
    }

    private static boolean classExists(String name, ClassLoader cl) {
        try {
            if (cl != null) {
                Class.forName(name, false, cl);
                return true;
            }
        } catch (Throwable ignore) {
        }
        try {
            Class.forName(name, false, ClassLoader.getSystemClassLoader());
            return true;
        } catch (Throwable ignore) {
        }
        return false;
    }

    public static String frameworkVersion() {
        try {
            android.content.Context c = QQEnv.appContext();
            if (c == null) return null;
            android.content.pm.PackageInfo pi = c.getPackageManager()
                    .getPackageInfo("org.lsposed.manager", 0);
            if (pi != null && !empty(pi.versionName)) return pi.versionName;
        } catch (Throwable ignore) {
        }
        return null;
    }

    public static String activeScope() {
        try {
            android.content.Context c = QQEnv.appContext();
            if (c == null) {
                c = currentAppContext();
            }
            if (c == null) return null;
            String pkg = c.getPackageName();
            if (pkg == null || pkg.length() == 0) return null;
            return friendlyHostName(pkg);
        } catch (Throwable ignore) {
        }
        return null;
    }

    private static android.content.Context currentAppContext() {
        try {
            Class<?> at = Class.forName("android.app.ActivityThread");
            Method cur = at.getMethod("currentApplication");
            cur.setAccessible(true);
            Object app = cur.invoke(null);
            return (android.content.Context) app;
        } catch (Throwable ignore) {
            return null;
        }
    }

    private static String friendlyHostName(String pkg) {
        if (pkg == null) return null;
        if (pkg.contains("com.tencent.tim")) return "TIM";
        if (pkg.contains("com.tencent.mobileqq")) return "QQ";
        if (pkg.contains("com.cathelperawa.owo.app")) return "本模块";

        return pkg;
    }

    private static boolean empty(String s) {
        return s == null || s.length() == 0 || "null".equals(s);
    }
}
