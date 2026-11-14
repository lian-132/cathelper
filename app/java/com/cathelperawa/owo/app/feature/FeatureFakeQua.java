package com.cathelperawa.owo.app.feature;

import android.content.pm.PackageInfo;

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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.robv.android.xposed.XC_MethodHook;

@HookItem(name = "伪造客户端版本", path = "伪造功能/版本", order = 72,
        description = "自定义 QUA 里的版本名 / 版本号 / 下载渠道", defaultEnabled = false)
public class FeatureFakeQua extends HookItemBase {

    public static final String K = "fake_qua";

    public static final String CFG_VERSION = "fake_qua_version";
    public static final String CFG_CODE = "fake_qua_code";
    public static final String CFG_CHANNEL = "fake_qua_channel";

    public static final String CFG_SUBID = "fake_qua_subid";

    private static final int SUB_MIN = 0x20000000;
    private static final int SUB_MAX = 0x200FFFFF;

    private static final Pattern QUA = Pattern.compile(
            "^(V\\d+_[A-Z]+_[A-Z]+_)([^_]+)_(\\d+)_([^_]+)(_[A-Z]+)$");

    private static volatile String cVer = "";
    private static volatile String cCode = "";
    private static volatile String cChan = "";
    private static volatile String cSubId = "";
    private static volatile long cLoaded = 0;
    private static final long CACHE_MS = 1500;

    private String realVer = "";
    private int realCode = 0;

    private static int dbgHit = 0;

    @Override
    public String switchKey() {
        return K;
    }

    @Override
    public void entry(ClassLoader cl) {
        loadReal();
        hint("在宿主读取 QUA / 版本号时触发");
        dbg("== entry 开始, 开关=" + safeOn()
                + " | 真实: " + realVer + " (" + realCode + ")");

        int m = hookAllStringGetters(cl);
        int f = patchStaticFields(cl);
        int sub = patchSubId(cl);
        dbg("方法挂钩 " + m + " 个，字段改写 " + f + " 个，SubId 改了 " + sub + " 个");

        if (m == 0 && f == 0) {
            fail("AppSetting 里没有可挂的方法 / 可改的字段");
            return;
        }
        HookLog.i("版本伪装：方法=" + m + " 字段=" + f);
    }

    private void loadReal() {
        try {
            if (ctx == null) return;
            PackageInfo pi = ctx.getPackageManager()
                    .getPackageInfo(ctx.getPackageName(), 0);
            realVer = pi.versionName == null ? "" : pi.versionName;
            realCode = pi.versionCode;
        } catch (Throwable ignore) {
        }
    }

    private int hookAllStringGetters(ClassLoader cl) {
        Class<?> c = HookApi.findClass(cl, "com.tencent.common.config.AppSetting");
        if (c == null) {
            HookLog.w("版本伪装：未找到 AppSetting");
            dbg("未找到 AppSetting");
            return 0;
        }
        int n = 0;
        for (final Method m : c.getDeclaredMethods()) {
            try {
                if (!Modifier.isStatic(m.getModifiers())) continue;
                if (m.getParameterTypes().length != 0) continue;
                if (m.getReturnType() != String.class) continue;
                m.setAccessible(true);
            } catch (Throwable ignore) {
                continue;
            }
            if (HookApi.after(m, new HookApi.Action() {
                @Override public void call(XC_MethodHook.MethodHookParam p) {
                    try {
                        Object r = p.getResult();
                        if (!(r instanceof String)) return;
                        String out = rewrite((String) r);
                        if (out != null) p.setResult(out);
                    } catch (Throwable t) {
                        noteRuntimeError(t);
                    }
                }
            }) != null) {
                n++;
            }
        }
        dbg("  AppSetting 里 String getter 共 " + n + " 个");
        return n;
    }

    private int patchStaticFields(ClassLoader cl) {
        Class<?> c = HookApi.findClass(cl, "com.tencent.common.config.AppSetting");
        if (c == null) return 0;
        int n = 0;
        for (Field f : c.getDeclaredFields()) {
            try {
                if (f.getType() != String.class) continue;
                if (!Modifier.isStatic(f.getModifiers())) continue;
                f.setAccessible(true);
                String cur = (String) f.get(null);
                String out = rewrite(cur);
                if (out == null || out.equals(cur)) continue;
                try {
                    Field mods = Field.class.getDeclaredField("accessFlags");
                    mods.setAccessible(true);
                    mods.setInt(f, f.getModifiers() & ~Modifier.FINAL);
                } catch (Throwable ignore) {
                }
                f.set(null, out);
                n++;
                dbg("  字段 " + f.getName() + ": " + cur + " → " + out);
            } catch (Throwable ignore) {
            }
        }
        return n;
    }

    private int patchSubId(ClassLoader cl) {
        refresh();
        if (cSubId.isEmpty()) return 0;
        int target;
        try {
            target = Integer.parseInt(cSubId);
        } catch (Throwable t) {
            dbg("SubId 配置不是数字: " + cSubId);
            return 0;
        }
        Class<?> c = HookApi.findClass(cl, "com.tencent.common.config.AppSetting");
        if (c == null) return 0;

        int n = 0;
        for (Field f : c.getDeclaredFields()) {
            try {
                if (f.getType() != int.class) continue;
                if (!Modifier.isStatic(f.getModifiers())) continue;
                f.setAccessible(true);
                int cur = f.getInt(null);
                if (cur < SUB_MIN || cur > SUB_MAX) continue;
                try {
                    Field mods = Field.class.getDeclaredField("accessFlags");
                    mods.setAccessible(true);
                    mods.setInt(f, f.getModifiers() & ~Modifier.FINAL);
                } catch (Throwable ignore) {
                }
                f.setInt(null, target);
                n++;
                dbg("  SubId 字段 " + f.getName() + ": " + cur + " → " + target);
            } catch (Throwable ignore) {
            }
        }

        for (final Method m : c.getDeclaredMethods()) {
            try {
                if (!Modifier.isStatic(m.getModifiers())) continue;
                if (m.getParameterTypes().length != 0) continue;
                if (m.getReturnType() != int.class) continue;
                String mn = m.getName();
                if (mn.length() > 2) continue;
                m.setAccessible(true);
                Object probe = m.invoke(null);
                if (!(probe instanceof Integer)) continue;
                int v = (Integer) probe;
                if (v < SUB_MIN || v > SUB_MAX) continue;
                HookApi.after(m, new HookApi.Action() {
                    @Override public void call(XC_MethodHook.MethodHookParam p) {
                        String s = cSubId;
                        if (s == null || s.isEmpty()) return;
                        try {
                            p.setResult(Integer.parseInt(s));
                        } catch (Throwable ignore) {
                        }
                    }
                }, this);
                n++;
                dbg("  SubId 方法 " + mn + "() 已挂");
            } catch (Throwable ignore) {
            }
        }
        return n;
    }

    private String rewrite(String raw) {
        if (raw == null || raw.length() == 0 || raw.length() > 200) return null;
        refresh();
        if (cVer.isEmpty() && cCode.isEmpty() && cChan.isEmpty()) return null;

        Matcher mm = QUA.matcher(raw);
        if (mm.matches()) {
            String ver = cVer.isEmpty() ? mm.group(2) : cVer;
            String code = cCode.isEmpty() ? mm.group(3) : cCode;
            String chan = cChan.isEmpty() ? mm.group(4) : cChan;
            String out = mm.group(1) + ver + "_" + code + "_" + chan + mm.group(5);
            hit(raw, out);
            return out;
        }

        if (realVer.length() > 0 && raw.contains(realVer)) {
            String out = raw;
            if (!cVer.isEmpty()) out = out.replace(realVer, cVer);
            if (!cCode.isEmpty() && realCode > 0) {
                out = out.replace(String.valueOf(realCode), cCode);
            }
            if (!out.equals(raw)) {
                hit(raw, out);
                return out;
            }
        }
        return null;
    }

    private void hit(String from, String to) {
        if (dbgHit < 25) {
            dbgHit++;
            dbg("#" + dbgHit + " " + from + " → " + to);
        }
    }

    private void refresh() {
        long now = System.currentTimeMillis();
        if (now - cLoaded < CACHE_MS) return;
        cLoaded = now;
        try {
            if (ctx == null) return;
            cVer = nz(HookConfig.get(ctx, CFG_VERSION, ""));
            cCode = nz(HookConfig.get(ctx, CFG_CODE, ""));
            cChan = nz(HookConfig.get(ctx, CFG_CHANNEL, ""));
            cSubId = nz(HookConfig.get(ctx, CFG_SUBID, ""));
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
            java.io.File f = new java.io.File(ctx.getFilesDir(), "fake_qua_debug.txt");
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
            Class<?> app = HookApi.findClass(cl, "com.tencent.common.config.AppSetting");
            if (app == null) {
                c.fail("AppSetting", "类未找到");
                return;
            }
            c.pass("AppSetting", "存在");
            int n = 0;
            for (Method m : app.getDeclaredMethods()) {
                if (Modifier.isStatic(m.getModifiers())
                        && m.getParameterTypes().length == 0
                        && m.getReturnType() == String.class) n++;
            }
            c.pass("String getter", n + " 个");
        } catch (Throwable t) {
            c.fail("自检", t.toString());
        }
    }
}
