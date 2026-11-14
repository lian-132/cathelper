package com.cathelperawa.owo.app.feature;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;

import com.cathelperawa.owo.app.core.HookApi;
import com.cathelperawa.owo.app.core.HookItem;
import com.cathelperawa.owo.app.core.HookItemBase;
import com.cathelperawa.owo.app.core.HookLog;
import com.cathelperawa.owo.app.core.SelfCheckResult;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import de.robv.android.xposed.XC_MethodHook;

@HookItem(name = "去你大爷的QQ浏览器", path = "去除限制/浏览器", order = 63,
        description = "网页强制用系统默认浏览器打开，不走 TIM 内置浏览器", defaultEnabled = false)
public class FeatureQqBrowser extends HookItemBase {

    public static final String K = "fxxk_qq_browser";

    private static final String[] URL_KEYS = {
            "url", "u", "targetUrl", "real_url", "extra_url", "android.intent.extra.TEXT"
    };

    private static final String[] WHITE_DOMAINS = {
            "qq.com", "tencent.com", "tenpay.com", "weixin.qq.com", "wechat.com",
            "cdn-go.cn", "gtimg.cn", "url.cn", "t.cn", "myapp.com", "qcloud.com"
    };

    private static final Map<String, Boolean> LOGGED = new HashMap<String, Boolean>();
    private static final int LOG_LIMIT = 40;
    private static int logged = 0;

    private static int dbgSeen = 0;

    @Override
    public String switchKey() {
        return K;
    }

    @Override
    public void entry(ClassLoader cl) {
        hint("在 TIM 里点开网页时触发");
        dbg("== entry 开始, 开关=" + safeOn());
        Class<?> inst = HookApi.findClass(cl, "android.app.Instrumentation");
        if (inst == null) {
            fail("未找到 android.app.Instrumentation");
            dbg("!! 找不到 Instrumentation 类");
            return;
        }
        int ok = 0;
        try {

            for (Method m : inst.getDeclaredMethods()) {
                if (!"execStartActivity".equals(m.getName())) continue;
                Class<?>[] ps = m.getParameterTypes();
                if (ps.length < 7 || ps.length > 8) continue;
                if (ps[4] != Intent.class) continue;
                dbg("候选: " + sig(m));
                try {
                    m.setAccessible(true);
                } catch (Throwable ignore) {
                }
                if (hookBefore(m, new HookApi.Action() {
                    @Override public void call(XC_MethodHook.MethodHookParam p) {
                        try {
                            onStart(p);
                        } catch (Throwable t) {

                            noteRuntimeError(t);
                        }
                    }
                }) != null) {
                    ok++;
                    dbg("已挂: " + sig(m));
                }
            }

            if (ok == 0) {
                fail("Instrumentation.execStartActivity 未找到（同名方法一个都没匹配上）");
                dbg("!! 一个都没挂上");
                return;
            }
            HookLog.i("去QQ浏览器：已挂 " + ok + " 个 execStartActivity 重载");
            dbg("== 已挂 " + ok + " 个重载");
        } catch (Throwable t) {
            dbg("!! entry 异常 " + t);
            throw t;
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

    private boolean safeOn() {
        try {
            return ctx != null && com.cathelperawa.owo.app.core.HookConfig.getBoolean(ctx, K, false);
        } catch (Throwable t) {
            return false;
        }
    }

    private void dbg(String s) {
        try {
            if (ctx == null) return;
            java.io.File f = new java.io.File(ctx.getFilesDir(), "qqbrowser_debug.txt");
            if (f.length() > 200 * 1024) return;
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

    private void onStart(XC_MethodHook.MethodHookParam p) {
        if (p.args == null || p.args.length <= 4) return;
        Object raw = p.args[4];
        if (!(raw instanceof Intent)) return;

        if (ctx == null || !com.cathelperawa.owo.app.core.HookConfig.getBoolean(ctx, K, false)) return;

        Intent intent = (Intent) raw;

        if (dbgSeen < 30) {
            dbgSeen++;
            ComponentName cn0 = intent.getComponent();
            dbg("#" + dbgSeen + " 触发 comp=" + (cn0 == null ? "null" : cn0.getShortClassName())
                    + " url=" + trim(pickUrl(intent), 80));
        }

        String url = pickUrl(intent);
        if (url == null) return;
        url = url.trim();
        String lower = url.toLowerCase(Locale.ROOT);
        if (lower.length() < 5) return;
        boolean hasScheme = lower.startsWith("http://") || lower.startsWith("https://");
        if (!hasScheme) {

            if (!looksLikeBareDomain(lower)) return;
            url = "http://" + url;
            lower = url.toLowerCase(Locale.ROOT);
        }

        ComponentName cn = intent.getComponent();
        String shortName = null;
        try {
            shortName = cn == null ? null : cn.getShortClassName();
        } catch (Throwable ignore) {
        }
        if (!isBrowserTarget(cn, shortName)) {

            sample(cn, shortName, lower);
            return;
        }

        String host = hostOf(lower);
        if (host == null || hostHit(host)) {
            logOnce("white:" + host, "白名单放行", lower);
            dbg("白名单放行 " + host);
            return;
        }

        Context c = ctx;
        if (c == null && p.args[0] instanceof Context) c = (Context) p.args[0];
        if (c == null) return;

        try {
            Intent view = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            view.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            c.startActivity(view);
        } catch (Throwable t) {
            logOnce("fail:" + host, "拉起系统浏览器失败，放行原调用", t.toString());
            return;
        }

        p.setResult(null);
        fire("hijack");
        logOnce("hit:" + host, "已劫持到系统浏览器(" + shortName + ")", lower);
        dbg("★ 已劫持 comp=" + shortName + " host=" + host);
    }

    private boolean isBrowserTarget(ComponentName cn, String shortName) {
        if (cn == null || shortName == null) return false;
        String pkg = cn.getPackageName();
        if (pkg == null || ctx == null) return false;
        if (!pkg.equals(ctx.getPackageName())) return false;
        if (shortName.contains("QQBrowser")) return true;
        String lc = shortName.toLowerCase(Locale.ROOT);
        if (lc.contains("browser")) return true;
        if (lc.contains("webview")) return true;
        return false;
    }

    private void sample(ComponentName cn, String shortName, String url) {
        if (cn == null || shortName == null) {
            dbg("未识别（组件为空）url=" + trim(url, 80));
            return;
        }
        logOnce("sample:" + shortName, "未识别目标(候选)", shortName + " pkg=" + cn.getPackageName()
                + " url=" + trim(url, 70));
        dbg("未识别 comp=" + shortName + " url=" + trim(url, 70));
    }

    private static String pickUrl(Intent intent) {
        for (String k : URL_KEYS) {
            try {
                String v = intent.getStringExtra(k);
                if (v != null && v.length() > 0) return v;
            } catch (Throwable ignore) {
            }
        }
        try {
            Uri d = intent.getData();
            if (d != null && d.toString().length() > 0) return d.toString();
        } catch (Throwable ignore) {
        }
        return null;
    }

    private static boolean looksLikeBareDomain(String s) {
        int sp = s.indexOf(' ');
        if (sp >= 0) return false;
        int slash = s.indexOf('/');
        String hostPart = slash < 0 ? s : s.substring(0, slash);
        int dot = hostPart.lastIndexOf('.');
        if (dot <= 0 || dot == hostPart.length() - 1) return false;
        String tld = hostPart.substring(dot + 1);
        if (tld.length() < 2 || tld.length() > 6) return false;
        for (int i = 0; i < tld.length(); i++) {
            if (!Character.isLetter(tld.charAt(i))) return false;
        }
        return true;
    }

    private static String hostOf(String url) {
        try {
            String h = Uri.parse(url).getHost();
            return h == null ? null : h.toLowerCase(Locale.ROOT);
        } catch (Throwable t) {
            return null;
        }
    }

    private static boolean hostHit(String host) {
        for (String d : WHITE_DOMAINS) {
            if (host.equals(d) || host.endsWith("." + d)) return true;
        }
        return false;
    }

    private static void logOnce(String key, String what, String detail) {
        if (key == null) return;
        synchronized (LOGGED) {
            if (LOGGED.containsKey(key)) return;
            if (logged >= LOG_LIMIT) return;
            LOGGED.put(key, Boolean.TRUE);
            logged++;
        }
        HookLog.i("[浏览器] " + what + "：" + trim(detail, 120));
    }

    private static String trim(String s, int n) {
        if (s == null) return "";
        return s.length() <= n ? s : s.substring(0, n) + "…";
    }

    private static String describe(Intent it) {
        try {
            String act = it.getAction();
            String data = it.getDataString();
            String url = pickUrl(it);
            return "action=" + act + " data=" + trim(data, 60) + " url=" + trim(url, 60);
        } catch (Throwable t) {
            return "?";
        }
    }

    @Override
    protected void check(SelfCheckResult.Checker c) {
        try {
            Class<?> inst = HookApi.findClass(cl, "android.app.Instrumentation");
            if (inst == null) {
                c.fail("Instrumentation", "类未加载");
                return;
            }
            int n = 0;
            for (Method m : inst.getDeclaredMethods()) {
                if (!"execStartActivity".equals(m.getName())) continue;
                Class<?>[] ps = m.getParameterTypes();
                if (ps.length < 7 || ps.length > 8) continue;
                if (ps[4] != Intent.class) continue;
                n++;
            }
            if (n > 0) c.pass("execStartActivity", "找到 " + n + " 个重载");
            else c.fail("execStartActivity", "一个都没找到");
        } catch (Throwable t) {
            c.fail("自检", t.toString());
        }
    }
}
