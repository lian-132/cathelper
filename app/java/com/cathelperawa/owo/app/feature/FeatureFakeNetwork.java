package com.cathelperawa.owo.app.feature;

import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;

import com.cathelperawa.owo.app.core.HookApi;
import com.cathelperawa.owo.app.core.HookConfig;
import com.cathelperawa.owo.app.core.HookItem;
import com.cathelperawa.owo.app.core.HookItemBase;
import com.cathelperawa.owo.app.core.HookLog;
import com.cathelperawa.owo.app.core.SelfCheckResult;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Locale;

import de.robv.android.xposed.XC_MethodHook;

@HookItem(name = "伪装网络状态", path = "伪造功能/网络", order = 70,
        description = "把网络类型伪装成 WIFI / 5G / 4G", defaultEnabled = false, hosts = {"tim", "qq"})
public class FeatureFakeNetwork extends HookItemBase {

    public static final String K = "fake_network";

    public static final String CFG_MODE = "fake_network_mode";

    public static final int MODE_WIFI = 1;
    public static final int MODE_5G = 2;
    public static final int MODE_4G = 3;

    private static volatile int cMode = MODE_WIFI;
    private static volatile long cLoaded = 0;
    private static final long CACHE_MS = 1500;

    private static int dbgHit = 0;

    @Override
    public String switchKey() {
        return K;
    }

    @Override
    public void entry(ClassLoader cl) {
        hint("在 TIM 查询网络类型时触发");
        dbg("== entry 开始, 开关=" + safeOn() + " 模式=" + modeName(readMode()));

        int n = 0;
        n += hookConnectivity();
        n += hookCapabilities();
        n += hookTelephony(cl);
        dbg("总共挂了 " + n + " 个点");
        if (n == 0) {
            fail("一个都没挂上");
            return;
        }
        HookLog.i("伪装网络：已挂 " + n + " 个点");
    }

    private int hookConnectivity() {
        try {
            Class<?> cm = HookApi.findClass(cl, "android.net.ConnectivityManager");
            if (cm == null) {
                fail("未找到 ConnectivityManager");
                return 0;
            }
            Method m = null;
            for (Method x : cm.getDeclaredMethods()) {
                if ("getActiveNetworkInfo".equals(x.getName()) && x.getParameterTypes().length == 0) {
                    m = x;
                    break;
                }
            }
            if (m == null) {
                dbg("getActiveNetworkInfo 未找到（Android 16 可能已移除）");
                return 0;
            }
            dbg("挂: " + sig(m));
            hookAfter(m, new HookApi.Action() {
                @Override public void call(XC_MethodHook.MethodHookParam p) {
                    try {
                        Object r = p.getResult();
                        if (r instanceof NetworkInfo) {
                            applyNetworkInfo((NetworkInfo) r);
                            p.setResult(r);
                        }
                    } catch (Throwable t) {
                        noteRuntimeError(t);
                    }
                }
            });
            return 1;
        } catch (Throwable t) {
            dbg("hookConnectivity 失败 " + t);
            return 0;
        }
    }

    private void applyNetworkInfo(NetworkInfo ni) {
        if (!safeOn()) return;
        int mode = readMode();
        int type, subtype;
        String typeName, subtypeName;
        switch (mode) {
            case MODE_WIFI:
                type = 1;
                subtype = 0;
                typeName = "WIFI";
                subtypeName = "";
                break;
            case MODE_5G:
                type = 0;
                subtype = 20;
                typeName = "MOBILE";
                subtypeName = "NR";
                break;
            case MODE_4G:
                type = 0;
                subtype = 13;
                typeName = "MOBILE";
                subtypeName = "LTE";
                break;
            default:
                return;
        }
        setField(ni, "mNetworkType", Integer.valueOf(type));
        setField(ni, "mSubtype", Integer.valueOf(subtype));
        setField(ni, "mTypeName", typeName);
        setField(ni, "mSubtypeName", subtypeName);
        if (dbgHit < 20) {
            dbgHit++;
            dbg("#" + dbgHit + " NetworkInfo → " + typeName
                    + (subtypeName.length() > 0 ? "/" + subtypeName : ""));
        }
    }

    private int hookCapabilities() {
        int n = 0;
        try {
            Class<?> cm = HookApi.findClass(cl, "android.net.ConnectivityManager");
            Class<?> network = HookApi.findClass(cl, "android.net.Network");
            if (cm == null) return 0;
            for (final Method x : cm.getDeclaredMethods()) {
                String name = x.getName();
                boolean target = "getNetworkCapabilities".equals(name)
                        && x.getParameterTypes().length == 1;
                boolean target2 = "getDefaultNetworkCapabilitiesForActiveNetwork".equals(name);
                if (!target && !target2) continue;
                if (x.getReturnType() != NetworkCapabilities.class) continue;
                try {
                    x.setAccessible(true);
                } catch (Throwable ignore) {
                }
                dbg("挂: " + sig(x));
                hookAfter(x, new HookApi.Action() {
                    @Override public void call(XC_MethodHook.MethodHookParam p) {
                        try {
                            Object r = p.getResult();
                            if (r instanceof NetworkCapabilities) {
                                applyCapabilities((NetworkCapabilities) r);
                                p.setResult(r);
                            }
                        } catch (Throwable t) {
                            noteRuntimeError(t);
                        }
                    }
                });
                n++;
            }
        } catch (Throwable t) {
            dbg("hookCapabilities 失败 " + t);
        }
        return n;
    }

    private void applyCapabilities(NetworkCapabilities nc) {
        if (!safeOn()) return;
        boolean wifi = readMode() == MODE_WIFI;
        try {
            Field tf = NetworkCapabilities.class.getDeclaredField("mTransportTypes");
            tf.setAccessible(true);

            tf.setLong(nc, wifi ? 2L : 1L);
        } catch (Throwable t) {
            dbg("mTransportTypes 改不动: " + t);
        }
        try {
            Field cf = NetworkCapabilities.class.getDeclaredField("mNetworkCapabilities");
            cf.setAccessible(true);
            long v = cf.getLong(nc);

            cf.setLong(nc, wifi ? (2048L | v) : (-2049L & v));
        } catch (Throwable t) {
            dbg("mNetworkCapabilities 改不动: " + t);
        }
    }

    private int hookTelephony(ClassLoader cl) {
        int n = 0;
        try {
            Class<?> tm = HookApi.findClass(cl, "android.telephony.TelephonyManager");
            if (tm == null) {
                dbg("未找到 TelephonyManager");
                return 0;
            }
            for (Method x : tm.getDeclaredMethods()) {
                String name = x.getName();
                if (!name.startsWith("getNetworkType") && !name.startsWith("getDataNetworkType")) continue;
                if (x.getReturnType() != int.class) continue;
                try {
                    x.setAccessible(true);
                } catch (Throwable ignore) {
                }
                hookBefore(x, new HookApi.Action() {
                    @Override public void call(XC_MethodHook.MethodHookParam p) {
                        try {
                            if (!safeOn()) return;
                            p.setResult(Integer.valueOf(telephonyType(readMode())));
                            if (dbgHit < 20) {
                                dbgHit++;
                                dbg("#" + dbgHit + " Telephony → " + telephonyType(readMode()));
                            }
                        } catch (Throwable t) {
                            noteRuntimeError(t);
                        }
                    }
                });
                n++;
            }
            dbg("TelephonyManager 挂了 " + n + " 个");
        } catch (Throwable t) {
            dbg("hookTelephony 失败 " + t);
        }
        return n;
    }

    private static int telephonyType(int mode) {
        switch (mode) {
            case MODE_5G: return 20;
            case MODE_4G: return 13;
            default: return 0;
        }
    }

    private static void setField(Object target, String name, Object value) {
        try {
            Field f = target.getClass().getDeclaredField(name);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Throwable ignore) {

        }
    }

    private int readMode() {
        long now = System.currentTimeMillis();
        if (now - cLoaded < CACHE_MS) return cMode;
        cLoaded = now;
        try {
            if (ctx == null) return cMode;
            String v = HookConfig.get(ctx, CFG_MODE, "1");
            int m = 1;
            try {
                m = Integer.parseInt(v.trim());
            } catch (Throwable ignore) {
            }
            if (m < 1 || m > 3) m = 1;
            if (m != cMode) dbg("模式变化 → " + modeName(m));
            cMode = m;
        } catch (Throwable ignore) {
        }
        return cMode;
    }

    public static String modeName(int m) {
        switch (m) {
            case MODE_5G: return "5G";
            case MODE_4G: return "4G";
            default: return "WIFI";
        }
    }

    private boolean safeOn() {
        try {
            return ctx != null && HookConfig.getBoolean(ctx, K, false);
        } catch (Throwable t) {
            return false;
        }
    }

    private static String sig(Method m) {
        StringBuilder sb = new StringBuilder(m.getReturnType().getSimpleName());
        sb.append(" ").append(m.getName()).append("(");
        Class<?>[] ps = m.getParameterTypes();
        for (int i = 0; i < ps.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(ps[i].getSimpleName());
        }
        return sb.append(")").toString();
    }

    private void dbg(String s) {
        try {
            if (ctx == null) return;
            java.io.File f = new java.io.File(ctx.getFilesDir(), "fake_network_debug.txt");
            if (f.length() > 120 * 1024) return;
            java.io.FileOutputStream o = new java.io.FileOutputStream(f, true);
            try {
                String t = new java.text.SimpleDateFormat("HH:mm:ss", Locale.US)
                        .format(new java.util.Date());
                o.write((t + " " + s + "\n").getBytes("UTF-8"));
            } finally {
                try { o.close(); } catch (Throwable ignore) {}
            }
        } catch (Throwable ignore) {
        }
    }

    @Override
    protected void check(SelfCheckResult.Checker c) {
        try {
            Class<?> cm = HookApi.findClass(cl, "android.net.ConnectivityManager");
            if (cm == null) {
                c.fail("ConnectivityManager", "类未找到");
            } else {
                boolean info = false;
                int caps = 0;
                for (Method m : cm.getDeclaredMethods()) {
                    if ("getActiveNetworkInfo".equals(m.getName())) info = true;
                    if (m.getReturnType() == NetworkCapabilities.class) caps++;
                }
                if (info) c.pass("getActiveNetworkInfo", "存在");
                else c.fail("getActiveNetworkInfo", "已移除（Android 16）");
                if (caps > 0) c.pass("NetworkCapabilities", caps + " 个入口");
                else c.fail("NetworkCapabilities", "没有返回该类型的方法");
            }
            Class<?> tm = HookApi.findClass(cl, "android.telephony.TelephonyManager");
            if (tm != null) c.pass("TelephonyManager", "存在");
            else c.fail("TelephonyManager", "未找到");
        } catch (Throwable t) {
            c.fail("自检", t.toString());
        }
    }
}
