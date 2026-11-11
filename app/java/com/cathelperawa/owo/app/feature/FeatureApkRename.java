package com.cathelperawa.owo.app.feature;

import com.cathelperawa.owo.app.core.HookApi;
import com.cathelperawa.owo.app.core.Bg;
import com.cathelperawa.owo.app.core.HookConfig;
import com.cathelperawa.owo.app.core.HookItem;
import com.cathelperawa.owo.app.core.HookItemBase;
import com.cathelperawa.owo.app.core.HookLog;
import com.cathelperawa.owo.app.core.SelfCheckResult;
import com.cathelperawa.owo.app.util.Refl;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;

import de.robv.android.xposed.XC_MethodHook;

@HookItem(name = "上传APK重命名", path = "文件功能/发送", order = 81,
        description = "发 APK 时自动改成「应用名_版本号.apk」", defaultEnabled = false, hosts = {"tim", "qq"})
public class FeatureApkRename extends HookItemBase {

    public static final String K = "apk_rename";

    private static int dbgDump = 0;

    @Override
    public String switchKey() {
        return K;
    }

    @Override
    public void entry(ClassLoader cl) {
        hint("发送 APK 文件时触发");
        dbg("== entry 开始, 开关=" + safeOn());
        retrySend(0);
    }

    private void retrySend(final int attempt) {
        if (attempt >= 10) {
            dbg("重试 10 次仍未挂上 sendMsg");
            fail("sendMsg 挂载失败（重试 10 次）");
            return;
        }
        final long wait = attempt == 0 ? 3000L : 2500L;
        Bg.run(new Runnable() {
            @Override public void run() {
                try {
                    Thread.sleep(wait);
                } catch (Throwable ignore) {
                }
                if (hookSend()) {
                    hooked = true;
                    dbg("sendMsg 挂载成功（第 " + (attempt + 1) + " 次）");
                } else {
                    retrySend(attempt + 1);
                }
            }
        }, "cat-apkrename");
    }

    private boolean hookSend() {
        String[] classes = new String[]{
                "com.tencent.qqnt.msg.api.impl.MsgServiceImpl",
                "com.tencent.qqnt.msg.MsgService",
                "com.tencent.qqnt.kernel.api.impl.MsgService",
        };
        int n = 0;
        for (String cn : classes) {
            Class<?> cls = HookApi.findClass(cl, cn);
            if (cls == null) continue;
            for (final Method m : cls.getDeclaredMethods()) {
                if (!"sendMsg".equals(m.getName())) continue;
                boolean hasList = false;
                for (Class<?> p : m.getParameterTypes()) {
                    if (List.class.isAssignableFrom(p)) {
                        hasList = true;
                        break;
                    }
                }
                if (!hasList) continue;
                hookBefore(m, new HookApi.Action() {
                    @Override public void call(XC_MethodHook.MethodHookParam p) {
                        try {
                            onSend(p);
                        } catch (Throwable t) {
                            noteRuntimeError(t);
                        }
                    }
                });
                n++;
                dbg("挂 " + cls.getSimpleName() + ".sendMsg(" + m.getParameterTypes().length + "参)");
            }
        }
        return n > 0;
    }

    @SuppressWarnings("unchecked")
    private void onSend(XC_MethodHook.MethodHookParam p) {
        if (ctx == null || !safeOn()) return;
        if (p.args == null) return;
        List<Object> els = null;
        for (Object a : p.args) {
            if (a instanceof List) {
                els = (List<Object>) a;
                break;
            }
        }
        if (els == null || els.isEmpty()) return;

        for (Object el : els) {
            if (el == null) continue;
            Object fe = Refl.field(el, "fileElement");
            if (fe == null) {
                if (dbgDump < 6) {
                    dbgDump++;
                    dbg("元素无 fileElement: " + el.getClass().getSimpleName()
                            + " elementType=" + str(Refl.field(el, "elementType")));
                }
                continue;
            }

            String name = str(Refl.field(fe, "fileName"));
            String path = str(Refl.field(fe, "filePath"));
            String local = str(Refl.field(fe, "localPath"));

            if (dbgDump < 3) {
                dbgDump++;
                dumpFields(fe);
            }

            String from = path.length() > 0 ? path : local.length() > 0 ? local : name;
            String newName = buildApkName(from);
            if (newName == null) {
                if (name.toLowerCase(Locale.ROOT).endsWith(".apk")) {
                    dbg("是 APK 但解析不出包信息: name=" + name + " path=" + path + " local=" + local);
                }
                continue;
            }
            if (newName.equals(name)) continue;

            if (Refl.setField(fe, "fileName", newName)) {
                dbg("★ 改名: " + name + " → " + newName);
                fire("rename");
            } else {
                dbg("改不动 fileName 字段（可能不是这个名字）");
            }
        }
    }

    private void dumpFields(Object obj) {
        dbg("---- fileElement 结构 (" + obj.getClass().getName() + ") ----");
        try {
            Field[] fs = obj.getClass().getDeclaredFields();
            int n = 0;
            for (Field f : fs) {
                if (n >= 40) break;
                try {
                    f.setAccessible(true);
                    Object v = f.get(obj);
                    String s;
                    if (v == null) s = "null";
                    else if (v instanceof String || v instanceof Number || v instanceof Boolean) {
                        s = String.valueOf(v);
                        if (s.length() > 60) s = s.substring(0, 60) + "…";
                    } else {
                        s = v.getClass().getSimpleName();
                    }
                    dbg("  " + f.getType().getSimpleName() + " " + f.getName() + " = " + s);
                    n++;
                } catch (Throwable ignore) {
                }
            }
        } catch (Throwable t) {
            dbg("dump 失败: " + t);
        }
    }

    private static String str(Object o) {
        return o == null ? "" : String.valueOf(o);
    }

    public String buildApkName(String path) {
        if (path == null) return null;
        if (!path.toLowerCase(Locale.ROOT).endsWith(".apk")) return null;
        try {
            android.content.pm.PackageManager pm = ctx.getPackageManager();
            android.content.pm.PackageInfo pi = pm.getPackageArchiveInfo(path, 0);
            if (pi == null) return null;
            String label = null;
            if (pi.applicationInfo != null) {
                pi.applicationInfo.sourceDir = path;
                pi.applicationInfo.publicSourceDir = path;
                try {
                    CharSequence l = pm.getApplicationLabel(pi.applicationInfo);
                    if (l != null) label = l.toString();
                } catch (Throwable ignore) {
                }
            }
            if (label == null || label.length() == 0) label = pi.packageName;
            String ver = pi.versionName;
            if (ver == null || ver.length() == 0) ver = String.valueOf(pi.versionCode);
            String name = label + "_" + ver + ".APK";   
            name = name.replace('/', '_').replace('\0', '_').replace(':', '_');
            return name;
        } catch (Throwable t) {
            return null;
        }
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
            File f = new File(ctx.getFilesDir(), "apk_rename_debug.txt");
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

    private static volatile boolean hooked = false;

    @Override
    protected void check(SelfCheckResult.Checker c) {
        
        boolean found = false;
        int methods = 0;
        String[] classes = new String[]{
                "com.tencent.qqnt.msg.api.impl.MsgServiceImpl",
                "com.tencent.qqnt.msg.MsgService",
                "com.tencent.qqnt.kernel.api.impl.MsgService",
        };
        for (String cn : classes) {
            Class<?> cls = HookApi.findClass(cl, cn);
            if (cls == null) continue;
            found = true;
            for (Method m : cls.getDeclaredMethods()) {
                if (!"sendMsg".equals(m.getName())) continue;
                for (Class<?> p : m.getParameterTypes()) {
                    if (List.class.isAssignableFrom(p)) {
                        methods++;
                        break;
                    }
                }
            }
            if (methods > 0) break;
        }
        if (!found) {
            c.fail("MsgService", "三个候选类都不在（宿主改版？）");
        } else if (methods == 0) {
            c.fail("sendMsg", "类在，但没有带 List 参数的 sendMsg");
        } else {
            c.pass("sendMsg", "找到 " + methods + " 个重载");
        }

        c.pass("加载方式", hooked ? "已挂载" : "延迟重试中（发一次 APK 后生效）");
    }
}
