package com.cathelperawa.owo.app.feature;

import com.cathelperawa.owo.app.core.HookApi;
import com.cathelperawa.owo.app.core.HookItem;
import com.cathelperawa.owo.app.core.HookConfig;
import com.cathelperawa.owo.app.core.HookItemBase;
import com.cathelperawa.owo.app.core.HookLog;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import de.robv.android.xposed.XC_MethodHook;

@HookItem(name = "防风控·掐断上报", path = "安全/防风控", order = 1,
        description = "禁用崩溃日志与统计上报，防止模块信息被带出去",
        defaultEnabled = true, hosts = {"tim", "qq"})
public class FeatureAntiDetect extends HookItemBase {

    public static final String K = "anti_detect";

    private boolean safeOn() {
        try {
            return ctx != null && HookConfig.getBoolean(ctx, K, false);
        } catch (Throwable t) {
            return false;
        }
    }

    private void dbg(String s) {
        HookLog.i("防风控: " + s);
    }

    @Override
    public String switchKey() {
        return K;
    }

    @Override
    public void entry(ClassLoader cl) {
        hint("模块自身的类名一旦进崩溃堆栈就会被上报");

        if (com.cathelperawa.owo.app.core.Host.isQQ()) {
            probeQQ(cl);
        }
        int n = 0;
        n += hookCrashReport(cl);
        n += hookStatistic(cl);
        n += hookBeacon(cl);
        dbg("抛出阻断点 " + n + " 个");
        if (n == 0) {
            fail("一个都没挂上");
            return;
        }
        hit(n);
        HookLog.i("防风控：已挂 " + n + " 个上报阻断点");
    }

    @Override
    protected void check(com.cathelperawa.owo.app.core.SelfCheckResult.Checker c) {
        String[][] groups = {
                {"com.tencent.qqperf.monitor.crash.QQCrashReportManager",
                 "com.tencent.qqperf.monitor.crash.CrashReportManager"},
                {"com.tencent.mobileqq.statistics.StatisticCollector",
                 "com.tencent.qqperf.monitor.stat.StatisticCollector"},
                {"com.tencent.mobileqq.statistics.QQBeaconReport",
                 "com.tencent.beacon.event.UserAction"},
        };
        String[] labels = {"崩溃上报", "统计上报", "Beacon"};
        int found = 0, hookable = 0;
        StringBuilder detail = new StringBuilder();
        for (int i = 0; i < groups.length; i++) {
            Class<?> k = probe(groups[i]);
            if (k == null) {
                detail.append(labels[i]).append("=缺失 ");
                continue;
            }
            found++;
            int cnt = 0;
            for (Method m : k.getDeclaredMethods()) {
                String nm = m.getName();
                if (isReportName(nm) || "initCrashReport".equals(nm) || "initBugly".equals(nm)) cnt++;
            }
            if (cnt > 0) hookable++;
            detail.append(labels[i]).append('=').append(cnt).append(' ');
        }
        c.assertThat("上报入口类", found > 0,
                "命中 " + found + "/3 · " + detail);
        c.assertThat("可拦的上报方法", hookable > 0,
                "有上报方法的入口 " + hookable + "/3");
    }

    private Class<?> probe(String[] names) {
        ClassLoader l = cl;
        if (l == null) l = Thread.currentThread().getContextClassLoader();
        for (String n : names) {
            try {
                Class<?> k = Class.forName(n, false, l);
                if (k != null) return k;
            } catch (Throwable ignore) {
            }
        }
        return null;
    }

    private int hookCrashReport(ClassLoader cl) {
        Class<?> k = HookApi.findClass(cl, "com.tencent.qqperf.monitor.crash.QQCrashReportManager");
        if (k == null) {
            dbg("没有 QQCrashReportManager（版本差异）");
            return 0;
        }
        int n = 0;
        for (Method m : k.getDeclaredMethods()) {
            String nm = m.getName();
            if (!"initCrashReport".equals(nm) && !"initBugly".equals(nm)
                    && !"init".equals(nm)) {
                continue;
            }

            if (hookBlock(m, "崩溃上报")) n++;
            break;
        }
        return n;
    }

    private int hookStatistic(ClassLoader cl) {
        Class<?> k = HookApi.findClass(cl, "com.tencent.mobileqq.statistics.StatisticCollector");
        if (k == null) {
            dbg("没有 StatisticCollector");
            return 0;
        }
        int n = 0;
        for (Method m : k.getDeclaredMethods()) {

            if (!isReportName(m.getName())) continue;
            if (hookBlock(m, "统计上报")) n++;
        }
        dbg("StatisticCollector 拦了 " + n + " 个");
        return n > 0 ? 1 : 0;
    }

    private int hookBeacon(ClassLoader cl) {
        String[] names = {
                "com.tencent.mobileqq.statistics.QQBeaconReport",
                "com.tencent.beacon.event.UserAction"
        };
        int n = 0;
        for (String nm : names) {
            Class<?> k = HookApi.findClass(cl, nm);
            if (k == null) continue;
            for (Method m : k.getDeclaredMethods()) {
                if (!Modifier.isStatic(m.getModifiers())) continue;
                if (!isReportName(m.getName())) continue;
                if (hookBlock(m, "Beacon")) n++;
            }
        }
        return n > 0 ? 1 : 0;
    }

    private void probeQQ(ClassLoader cl) {
        String[] probe = {
                "com.tencent.qqperf.monitor.crash.QQCrashReportManager",
                "com.tencent.qqperf.monitor.crash.CrashReportManager",
                "com.tencent.qqperf.monitor.crash.QQCrashReportManagerImpl",
                "com.tencent.mobileqq.statistics.StatisticCollector",
                "com.tencent.mobileqq.statistics.StatisticHitRateCollector",
                "com.tencent.mobileqq.statistics.QQBeaconReport",
                "com.tencent.beacon.event.UserAction",
                "com.tencent.qqperf.monitor.stat.StatisticCollector",
                "com.tencent.qqperf.monitor.base.QQPerfManager",
        };
        StringBuilder sb = new StringBuilder();
        for (String p : probe) {
            Class<?> c = HookApi.findClass(cl, p);
            sb.append(p).append(" → ").append(c != null).append('\n');
            if (c == null) continue;
            int cnt = 0;
            StringBuilder ms = new StringBuilder();
            for (Method m : c.getDeclaredMethods()) {
                if (!isReportName(m.getName())) continue;
                if (cnt++ < 12) ms.append(m.getName()).append(' ');
            }
            sb.append("   report 方法 ").append(cnt).append(" 个: ").append(ms).append('\n');
            HookLog.i("防风控探测: " + p + " report=" + cnt + " " + ms);
        }
        try {
            java.io.File f = new java.io.File(ctx.getFilesDir(), "anti_probe.txt");
            java.io.FileOutputStream o = new java.io.FileOutputStream(f, false);
            o.write(sb.toString().getBytes("UTF-8"));
            o.close();
        } catch (Throwable ignore) {
        }
    }

    private boolean isReportName(String s) {
        if (s == null) return false;
        String l = s.toLowerCase();
        return l.contains("report") || l.contains("commitdb") || l.contains("upload")
                || l.contains("doreport");
    }

    private boolean hookBlock(final Method m, final String tag) {
        try {
            m.setAccessible(true);
            Object r = HookApi.before(m, new HookApi.Action() {
                @Override public void call(XC_MethodHook.MethodHookParam p) {
                    if (!safeOn()) return;

                    Class<?> rt = m.getReturnType();
                    if (rt == void.class) {
                        p.setResult(null);
                    } else if (rt == boolean.class) {
                        p.setResult(Boolean.FALSE);
                    } else if (rt == int.class) {
                        p.setResult(0);
                    } else if (rt == long.class) {
                        p.setResult(0L);
                    } else if (rt == short.class) {
                        p.setResult((short) 0);
                    } else if (rt == byte.class) {
                        p.setResult((byte) 0);
                    } else if (rt == char.class) {
                        p.setResult('\0');
                    } else if (rt == float.class) {
                        p.setResult(0f);
                    } else if (rt == double.class) {
                        p.setResult(0d);
                    } else {
                        p.setResult(null);
                    }
                }
            });
            if (r != null) {
                dbg("拦: " + tag + " " + m.getName());
                return true;
            }
        } catch (Throwable t) {
            dbg("拦失败 " + m.getName() + ": " + t);
        }
        return false;
    }
}
