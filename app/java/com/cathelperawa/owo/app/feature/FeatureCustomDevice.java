package com.cathelperawa.owo.app.feature;

import android.os.Build;

import com.cathelperawa.owo.app.core.HookApi;
import com.cathelperawa.owo.app.core.HookConfig;
import com.cathelperawa.owo.app.core.HookItem;
import com.cathelperawa.owo.app.core.HookItemBase;
import com.cathelperawa.owo.app.core.HookLog;
import com.cathelperawa.owo.app.core.SelfCheckResult;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Locale;

import de.robv.android.xposed.XC_MethodHook;

@HookItem(name = "自定义设备信息", path = "伪造功能/设备", order = 71,
        description = "自定义宿主读取的 device / model / manufacturer", defaultEnabled = false, hosts = {"tim", "qq"})
public class FeatureCustomDevice extends HookItemBase {

    public static final String K = "custom_device";

    public static final String CFG_DEVICE = "custom_device_device";
    public static final String CFG_MODEL = "custom_device_model";
    public static final String CFG_MFR = "custom_device_manufacturer";

    private static volatile String cDevice = "";
    private static volatile String cModel = "";
    private static volatile String cMfr = "";
    private static volatile long cLoaded = 0;
    private static final long CACHE_MS = 1500;

    private static int dbgHit = 0;

    @Override
    public String switchKey() {
        return K;
    }

    @Override
    public void entry(ClassLoader cl) {
        hint("在 TIM 读取机型信息时触发");
        dbg("== entry 开始, 开关=" + safeOn()
                + " | 当前真实: device=" + Build.DEVICE
                + " model=" + Build.MODEL
                + " mfr=" + Build.MANUFACTURER);

        int sp = hookSystemProperties(cl);
        dbg("SystemProperties 挂了 " + sp + " 个 get 重载");

        boolean dim = hookDeviceInfoMonitor(cl);
        dbg("DeviceInfoMonitor = " + (dim ? "已挂" : "未找到/未挂"));

        boolean bf = patchBuildFields();
        dbg("Build 静态字段 = " + (bf ? "已改" : "跳过（反射失败或为空）"));

        if (sp == 0 && !dim && !bf) {
            fail("三层都没挂上");
            return;
        }
        HookLog.i("自定义设备：SystemProperties=" + sp + " DeviceInfoMonitor=" + dim + " Build字段=" + bf);
    }

    private int hookSystemProperties(ClassLoader cl) {
        Class<?> sp = HookApi.findClass(cl, "android.os.SystemProperties");
        if (sp == null) {
            fail("未找到 android.os.SystemProperties");
            return 0;
        }
        int n = 0;
        for (final Method m : sp.getDeclaredMethods()) {

            if (!"get".equals(m.getName())) continue;
            if (m.getReturnType() != String.class) continue;
            if (m.getParameterTypes().length < 1) continue;
            if (m.getParameterTypes()[0] != String.class) continue;
            try {
                m.setAccessible(true);
            } catch (Throwable ignore) {
            }
            if (hookBefore(m, new HookApi.Action() {
                @Override public void call(XC_MethodHook.MethodHookParam p) {
                    try {
                        onSysProp(p);
                    } catch (Throwable t) {
                        noteRuntimeError(t);
                    }
                }
            }) != null) {
                n++;
                dbg("  sp: " + sig(m));
            }
        }
        return n;
    }

    private void onSysProp(XC_MethodHook.MethodHookParam p) {
        if (p.args == null || p.args.length == 0) return;
        Object k0 = p.args[0];
        if (!(k0 instanceof String)) return;
        String key = (String) k0;
        int kind = classify(key);
        if (kind == 0) return;

        refreshIfStale();
        String v = kind == 1 ? cDevice : kind == 2 ? cModel : cMfr;
        if (v == null || v.length() == 0) return;
        p.setResult(v);
        if (dbgHit < 20) {
            dbgHit++;
            dbg("#" + dbgHit + " " + key + " → " + v);
        }
    }

    private static int classify(String key) {
        if (key == null) return 0;
        if (key.equals("ro.build.product")) return 1;
        if (!key.startsWith("ro.product")) return 0;
        if (key.endsWith(".device") || key.equals("ro.product.device")) return 1;
        if (key.endsWith(".model") || key.equals("ro.product.model")) return 2;
        if (key.endsWith(".manufacturer")) return 3;
        return 0;
    }

    private boolean hookDeviceInfoMonitor(ClassLoader cl) {
        Class<?> c = HookApi.findClass(cl,
                "com.tencent.qmethod.pandoraex.monitor.DeviceInfoMonitor");
        if (c == null) {
            HookLog.w("自定义设备：未找到 DeviceInfoMonitor");
            return false;
        }
        int n = 0;
        for (final Method m : c.getDeclaredMethods()) {
            String name = m.getName();
            if (!"getModel".equals(name) && !"getDevice".equals(name)
                    && !"getManufacturer".equals(name)) continue;
            if (m.getReturnType() != String.class) continue;
            try {
                m.setAccessible(true);
            } catch (Throwable ignore) {
            }
            final int kind = "getModel".equals(name) ? 2
                    : "getDevice".equals(name) ? 1 : 3;
            if (hookBefore(m, new HookApi.Action() {
                @Override public void call(XC_MethodHook.MethodHookParam p) {
                    try {
                        refreshIfStale();
                        String v = kind == 1 ? cDevice : kind == 2 ? cModel : cMfr;
                        if (v != null && v.length() > 0) p.setResult(v);
                    } catch (Throwable t) {
                        noteRuntimeError(t);
                    }
                }
            }) != null) {
                n++;
                dbg("  dim: " + sig(m));
            }
        }
        return n > 0;
    }

    private boolean patchBuildFields() {
        boolean any = false;
        any |= setFinalStatic("MODEL", cModel);
        any |= setFinalStatic("DEVICE", cDevice);
        any |= setFinalStatic("MANUFACTURER", cMfr);
        return any;
    }

    private boolean setFinalStatic(String field, String value) {
        if (value == null || value.length() == 0) return false;
        try {
            Field f = Build.class.getDeclaredField(field);
            f.setAccessible(true);
            try {
                Field mods = Field.class.getDeclaredField("accessFlags");
                mods.setAccessible(true);
                mods.setInt(f, f.getModifiers() & ~Modifier.FINAL);
            } catch (Throwable ignore) {

            }
            f.set(null, value);

            Object back = f.get(null);
            dbg("  Build." + field + " = " + value + " (读回=" + back + ")");
            return true;
        } catch (Throwable t) {
            dbg("  Build." + field + " 改不动: " + t);
            return false;
        }
    }

    private void refreshIfStale() {
        long now = System.currentTimeMillis();
        if (now - cLoaded < CACHE_MS) return;
        cLoaded = now;
        try {
            if (ctx == null) return;
            String nd = nz(HookConfig.get(ctx, CFG_DEVICE, ""));
            String nm = nz(HookConfig.get(ctx, CFG_MODEL, ""));
            String nf = nz(HookConfig.get(ctx, CFG_MFR, ""));
            boolean changed = !nd.equals(cDevice) || !nm.equals(cModel) || !nf.equals(cMfr);
            cDevice = nd;
            cModel = nm;
            cMfr = nf;

            if (changed) {
                dbg("配置变化: device=" + nd + " model=" + nm + " mfr=" + nf);
                patchBuildFields();
            }
        } catch (Throwable ignore) {
        }
    }

    private static String nz(String s) {
        return s == null ? "" : s.trim();
    }

    private boolean safeOn() {
        try {
            return ctx != null && HookConfig.getBoolean(ctx, K, false);
        } catch (Throwable t) {
            return false;
        }
    }

    private void dbg(String s) {
        try {
            if (ctx == null) return;
            java.io.File f = new java.io.File(ctx.getFilesDir(), "custom_device_debug.txt");
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

    @Override
    protected void check(SelfCheckResult.Checker c) {
        try {
            Class<?> sp = HookApi.findClass(cl, "android.os.SystemProperties");
            if (sp == null) {
                c.fail("SystemProperties", "类未找到");
            } else {
                int n = 0;
                for (Method m : sp.getDeclaredMethods()) {
                    if ("get".equals(m.getName()) && m.getReturnType() == String.class) n++;
                }
                if (n > 0) c.pass("SystemProperties.get", n + " 个重载");
                else c.fail("SystemProperties.get", "没有返回 String 的重载");
            }
            Class<?> dim = HookApi.findClass(cl,
                    "com.tencent.qmethod.pandoraex.monitor.DeviceInfoMonitor");
            if (dim != null) c.pass("DeviceInfoMonitor", "存在");
            else c.fail("DeviceInfoMonitor", "未找到");
        } catch (Throwable t) {
            c.fail("自检", t.toString());
        }
    }
}
