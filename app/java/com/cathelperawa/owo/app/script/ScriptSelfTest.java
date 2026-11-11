package com.cathelperawa.owo.app.script;

import java.io.File;

public final class ScriptSelfTest {

    private ScriptSelfTest() {}

    public static String section() {
        StringBuilder sb = new StringBuilder();
        int bad = 0;

        sb.append("── 脚本子系统 ──\n");

        try {
            int all = ScriptHost.list().size();
            int on = ScriptHost.enabledNames().size();
            sb.append("脚本 ").append(all).append(" 个 · 启用 ").append(on).append(" 个\n");
        } catch (Throwable t) {
            sb.append("脚本数量读取失败 ").append(t).append('\n');
            bad++;
        }

        try {
            Object r1 = new BshEngine().eval("1+1");
            ScriptEngine re = new RhinoEngine(null);
            Object r2 = re.eval("1+1");
            re.close();
            boolean ok1 = String.valueOf(r1).startsWith("2");
            boolean ok2 = String.valueOf(r2).startsWith("2");
            sb.append("引擎 BeanShell=").append(ok1 ? "正常" : "异常")
              .append(" · Rhino=").append(ok2 ? "正常" : "异常").append('\n');
            if (!ok1) bad++;
            if (!ok2) bad++;
        } catch (Throwable t) {
            sb.append("✗ 引擎检查失败 ").append(t).append('\n');
            bad++;
        }

        try {
            sb.append("注册表 命令 ").append(RouteRegistry.size())
              .append(" 条 · AI 工具 ").append(ScriptToolRegistry.size()).append(" 个\n");
        } catch (Throwable ignore) {
        }
        try {
            String pts = PatchPoint.supportedList();
            int n = pts.split("/").length;
            sb.append("点位 ").append(n).append(" 个 · ").append(pts).append('\n');
        } catch (Throwable ignore) {
        }

        
        try {
            sb.append(envLine());
        } catch (Throwable t) {
            sb.append("★ 能力检测失败 ").append(t).append('\n');
            bad++;
        }

        try {
            File d = ScriptHost.dir();
            File sd = ScriptHost.stateDir();
            File ex = ModPack.exportDir();
            sb.append("目录 脚本=").append(d == null ? "无" : "可读")
              .append(" · .state=").append(sd == null ? "无" : "可用")
              .append(" · 导出=").append(ex == null ? "无" : "可用").append('\n');
            if (d == null) bad++;
        } catch (Throwable t) {
            sb.append("✗ 目录检查失败 ").append(t).append('\n');
            bad++;
        }

        try {
            boolean safe = com.cathelperawa.owo.app.core.SafeMode.active();
            String last = BootGuard.lastState();
            sb.append("启动 安全模式=").append(safe ? "是（功能被裁剪）" : "否")
              .append(" · 上次加载=").append(last.length() == 0 ? "未知" : last).append('\n');
            if (safe) {
                sb.append("  原因：").append(com.cathelperawa.owo.app.core.SafeMode.why()).append('\n');
                bad++;
            }
        } catch (Throwable ignore) {
        }

        try {
            String sum = ScriptExec.summary();
            if (sum != null && sum.trim().length() > 0) {
                String[] lines = sum.split("\n");
                sb.append("运行统计 ").append(lines.length).append(" 个脚本有记录\n");
                int show = Math.min(lines.length, 3);
                for (int i = 0; i < show; i++) sb.append("  · ").append(lines[i]).append('\n');
            } else {
                sb.append("运行统计 还没有执行记录\n");
            }
        } catch (Throwable ignore) {
        }

        try {
            int errs = 0;
            StringBuilder names = new StringBuilder();
            java.util.List<String> all = ScriptHost.list();
            for (int i = 0; i < all.size(); i++) {
                String n = all.get(i);
                java.util.List<String> risks = null;
                try {
                    String e = ScriptHost.errorText(n);
                    if (e != null && e.trim().length() > 0) {
                        errs++;
                        if (names.length() < 120) names.append("  · ").append(n).append('\n');
                    }
                } catch (Throwable ignore) {
                }
            }
            if (errs > 0) {
                sb.append("有报错的脚本 ").append(errs).append(" 个：\n").append(names);
            }
        } catch (Throwable ignore) {
        }

        if (bad > 0) sb.append("✗ 脚本子系统有 ").append(bad).append(" 项异常\n");
        sb.append('\n');
        return sb.toString();
    }

    public static String envLine() {
        StringBuilder b = new StringBuilder();
        int bad = 0;
        
        boolean imp;
        try {
            ScriptEngine it = new BshEngine();
            ScriptEnv.apply(it);
            Object r = it.eval("new JSONObject().put(\"a\",1).toString().length() > 0");
            it.close();
            imp = Boolean.TRUE.equals(r);
        } catch (Throwable t) {
            imp = false;
        }
        if (!imp) bad++;

        boolean glb;
        try {
            ScriptEngine it = new BshEngine();
            ScriptEnv.bindGlobals(it, new MockScriptApi("__probe"));
            Object r1 = it.eval("getString(\"k\",\"d\") != null");
            Object r2 = it.eval("getString(\"k\") == null || getString(\"k\") != null");
            it.close();
            glb = Boolean.TRUE.equals(r1) && Boolean.TRUE.equals(r2);
        } catch (Throwable t) {
            glb = false;
        }
        if (!glb) bad++;

        boolean dex;
        try {
            dex = com.cathelperawa.owo.app.script.ScriptJar.class != null
                    && dxReady();
        } catch (Throwable t) {
            dex = false;
        }
        if (!dex) bad++;

        boolean prb;
        try {
            ClassLoader cl = com.cathelperawa.owo.app.bridge.QQEnv.classLoader();
            String r = ScriptReflect.probe(cl, "com.tencent.common.app.BaseApplicationImpl", "getApplication");
            prb = r != null && r.indexOf("找不到类") < 0;
        } catch (Throwable t) {
            prb = false;
        }
        if (!prb) bad++;

        b.append("★ 脚本能力 import ").append(imp ? "✓" : "✗")
         .append(" · 全局函数 ").append(glb ? "✓" : "✗")
         .append(" · dx转 dex ").append(dex ? "✓" : "✗")
         .append(" · 宿主探查 ").append(prb ? "✓" : "✗")
         .append("\n");
        return b.toString();
    }

    private static boolean dxReady() {
        try {
            Class.forName("com.android.dx.dex.file.DexFile", false,
                    ScriptSelfTest.class.getClassLoader());
            Class.forName("com.android.dx.dex.cf.CfTranslator", false,
                    ScriptSelfTest.class.getClassLoader());
            return true;
        } catch (Throwable t) {
            return false;
        }
    }
}
