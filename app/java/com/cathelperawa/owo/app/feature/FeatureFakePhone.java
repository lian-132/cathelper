package com.cathelperawa.owo.app.feature;

import com.cathelperawa.owo.app.core.HookApi;
import com.cathelperawa.owo.app.core.HookConfig;
import com.cathelperawa.owo.app.core.HookItem;
import com.cathelperawa.owo.app.core.HookItemBase;
import com.cathelperawa.owo.app.core.HookLog;
import com.cathelperawa.owo.app.core.SelfCheckResult;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Locale;

import de.robv.android.xposed.XC_MethodHook;

@HookItem(name = "伪造手机号", path = "伪造功能/手机号", order = 72,
        description = "伪装「账号与安全」页面里显示的手机号码", defaultEnabled = false, hosts = {"tim", "qq"})
public class FeatureFakePhone extends HookItemBase {

    public static final String K = "fake_phone";

    public static final String CFG_PHONE = "fake_phone_value";
    public static final String DEFAULT_PHONE = "1145141919810";

    @Override
    public String switchKey() {
        return K;
    }

    @Override
    public void entry(ClassLoader cl) {
        hint("打开「账号管理 / 手机号码」页时触发");
        dbg("== entry 开关=" + safeOn() + " 号码=" + displayPhone());
        hookBundle();
    }

    private void hookBundle() {
        Class<?> base = HookApi.findClass(cl, "android.os.BaseBundle");
        if (base == null) {
            fail("未找到 android.os.BaseBundle");
            return;
        }
        int n = 0;
        for (Method m : base.getDeclaredMethods()) {
            String name = m.getName();
            boolean isStr = "getString".equals(name);
            boolean isChar = "getCharSequence".equals(name);
            if (!isStr && !isChar) continue;
            Class<?>[] ps = m.getParameterTypes();
            if (ps.length != 1 || ps[0] != String.class) continue;
            try {
                m.setAccessible(true);
            } catch (Throwable ignore) {
            }
            final boolean charSeq = isChar;
            hookAfter(m, new HookApi.Action() {
                @Override public void call(XC_MethodHook.MethodHookParam p) {
                    try {
                        if (!safeOn()) return;
                        if (p.args == null || p.args.length == 0) return;
                        Object k = p.args[0];
                        if (!(k instanceof String)) return;
                        if (!"phone".equals(k)) return;
                        String v = displayPhone();
                        if (charSeq) p.setResult(v);
                        else p.setResult(v);
                        if (hit < 20) {
                            hit++;
                            dbg("#" + hit + " Bundle." + (charSeq ? "getCharSequence" : "getString")
                                    + "(\"phone\") → " + v);
                        }
                    } catch (Throwable t) {
                        noteRuntimeError(t);
                    }
                }
            });
            n++;
        }
        dbg("BaseBundle 挂了 " + n + " 个读取方法");
        HookLog.i("伪造手机号：BaseBundle 挂了 " + n + " 个");
        if (n == 0) fail("BaseBundle.getString 未找到");
    }

    private static int hit = 0;

    private String displayPhone() {
        String v = "";
        try {
            if (ctx != null) v = HookConfig.get(ctx, CFG_PHONE, "");
        } catch (Throwable ignore) {
        }
        v = v == null ? "" : v.trim();
        return v.length() == 0 ? DEFAULT_PHONE : v;
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
            File f = new File(ctx.getFilesDir(), "fake_phone_debug.txt");
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
        Class<?> base = HookApi.findClass(cl, "android.os.BaseBundle");
        if (base == null) {
            c.fail("BaseBundle", "未找到");
        } else {
            boolean ok = false;
            for (Method m : base.getDeclaredMethods()) {
                if ("getString".equals(m.getName())
                        && m.getParameterTypes().length == 1
                        && m.getParameterTypes()[0] == String.class) ok = true;
            }
            if (ok) c.pass("Bundle.getString", "存在");
            else c.fail("Bundle.getString", "签名对不上");
        }
    }
}
