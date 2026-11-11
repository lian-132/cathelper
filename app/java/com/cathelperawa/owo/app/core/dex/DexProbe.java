package com.cathelperawa.owo.app.core.dex;

import android.content.Context;

import com.cathelperawa.owo.app.core.HookLog;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.List;

public final class DexProbe {

    private static final String OUT = "dex_probe.txt";
    private static volatile boolean running;

    private DexProbe() {}

    public static void runAsync(final Context ctx) {
        if (running) {
            HookLog.w("DexProbe 已在跑，忽略重复请求");
            return;
        }
        running = true;
        final Context app = ctx.getApplicationContext();
        Thread t = new Thread(new Runnable() {
            @Override public void run() {
                StringBuilder sb = new StringBuilder();
                try {
                    probe(app, sb);
                } catch (Throwable e) {
                    sb.append("\n!! 探针异常: ").append(e).append('\n');
                    HookLog.e("DexProbe 失败", e);
                } finally {
                    write(app, sb.toString());
                    running = false;
                }
            }
        }, "cat-dexprobe");
        t.setDaemon(true);
        t.start();
    }

    private static void probe(Context app, StringBuilder sb) {
        Runtime rt = Runtime.getRuntime();
        sb.append("== CatHelper DexProbe ==\n");
        sb.append("宿主包名: ").append(app.getPackageName()).append('\n');
        sb.append("进程号: ").append(android.os.Process.myPid()).append('\n');
        sb.append("堆上限: ").append(rt.maxMemory() / 1024 / 1024).append("MB\n\n");

        long t0 = System.currentTimeMillis();
        DexIndex ix = DexIndexLoader.get(app);
        long t1 = System.currentTimeMillis();
        if (ix == null) {
            sb.append("✗ 建索引失败（宿主 APK 读不到？）\n");
            HookLog.w("DexProbe 建索引失败：宿主 APK 读不到");
            return;
        }
        sb.append("✓ 索引完成: ").append(t1 - t0).append("ms  ").append(ix.stats())
          .append("  来源=").append(DexIndexLoader.lastHitCache() ? "磁盘缓存" : "现场构建").append('\n');
        sb.append("当前堆: ").append((rt.totalMemory() - rt.freeMemory()) / 1024 / 1024).append("MB\n\n");

        ClassFinder f = new ClassFinder(ix);

        sb.append("── 查询 1：View 子类中声明了 onDraw 的 ──\n");
        timeQuery(sb, f.query().superClass("android.view.View").withMethod("onDraw").find());

        sb.append("\n── 查询 2：MessageRecord 直接子类 ──\n");
        timeQuery(sb, f.query()
                .superClass("com.tencent.mobileqq.data.MessageRecord").find());

        sb.append("\n── 查询 3：消息服务接口实现 ──\n");
        timeQuery(sb, f.query()
                .implementing("com.tencent.qqnt.msg.api.IMsgService").find());

        sb.append("\n── 查询 4：猜错类名后自愈（只给模糊名，靠索引捞回来）──\n");
        long tg = System.currentTimeMillis();
        java.util.List<String> guess = ix.grep(java.util.regex.Pattern.compile("IMsgService$"));
        sb.append("按后缀正则捞到: ").append(guess)
          .append("  耗时 ").append(System.currentTimeMillis() - tg).append("ms\n");

        sb.append("\n── 查询 5：字符串常量反查（DexKit 式，需带字符串的索引）──\n");
        long t2 = System.currentTimeMillis();
        DexIndex deep = DexIndexLoader.get(app, true);
        if (deep == null) {
            sb.append("✗ 带字符串的索引建失败\n");
        } else {
            sb.append("索引: ").append(deep.stringRefCount())
              .append(" 条指纹  耗时 ").append(System.currentTimeMillis() - t2).append("ms")
              .append("  来源=").append(DexIndexLoader.lastHitCache() ? "磁盘缓存" : "现场构建").append('\n');
            if (DexIndexLoader.lastError() != null)
                sb.append("  上次缓存异常: ").append(DexIndexLoader.lastError()).append('\n');
            for (String lit : new String[]{"qwallet_pb_handle_trpc_error", "FlashPicHelper"}) {
                long q = System.currentTimeMillis();
                java.util.List<String> c = deep.classesUsingString(lit);
                sb.append("  \"").append(lit).append("\" → ").append(c)
                  .append("  ").append(System.currentTimeMillis() - q).append("ms\n");
            }
        }

        sb.append("\n── 解析自检（ClassResolver 真实链路）──\n");

        ClassLoader cl = com.cathelperawa.owo.app.bridge.QQEnv.classLoader();
        if (cl == null) cl = DexProbe.class.getClassLoader();
        
        sb.append("menu.item     → ")
          .append(ClassResolver.resolveName(app, cl, new ClassResolver.Spec("menu.item")
                  .candidates("com.tencent.qqnt.aio.menu.d",
                              "com.tencent.mobileqq.utils.dialogutils.QQCustomMenuItem")
                  .nameMatches("(QQCustomMenuItem|\\baio\\.menu\\.[a-z]$)")
                  .allowAmbiguous()))
          .append('\n');
        sb.append("graytip       → ")
          .append(ClassResolver.resolveName(app, cl, new ClassResolver.Spec("graytip.jsonElement")
                  .candidates("com.tencent.qqnt.kernelpublic.nativeinterface.JsonGrayElement")
                  .nameMatches("JsonGrayElement$")
                  .allowAmbiguous()))
          .append('\n');
        sb.append("troop.di      → ")
          .append(ClassResolver.resolveName(app, cl, new ClassResolver.Spec("troop.di")
                  .candidates("com.tencent.mobileqq.qroute.QRoute")
                  .nameMatches("\\bQRoute$")
                  .allowAmbiguous()))
          .append('\n');
        
        long ts = System.currentTimeMillis();
        String healed = ClassResolver.resolveName(app, cl, new ClassResolver.Spec("selftest.heal")
                .candidates("com.cathelper.definitely.NotHere")
                .nameMatches("JsonGrayElement$")
                .allowAmbiguous());
        sb.append("自愈测试(假候选) → ").append(healed)
          .append("  ").append(System.currentTimeMillis() - ts).append("ms\n");

        sb.append("findName(qqnt.contact) → ")
          .append(ClassResolver.findName(cl, "qqnt.contact",
                  "com.tencent.qqnt.kernelpublic.nativeinterface.Contact",
                  "com.tencent.qqnt.kernel.nativeinterface.Contact")).append('\n');
        sb.append("findName(aio.event.setSelectMode) → ")
          .append(ClassResolver.findName(cl, "aio.event.setSelectMode",
                  "com.tencent.mobileqq.aio.event.MultiSelectEvent$SetSelectMode")).append('\n');
        sb.append("findName(mvi.baseVM) → ")
          .append(ClassResolver.findName(cl, "mvi.baseVM", "com.tencent.mvi.mvvm.BaseVM")).append('\n');

        sb.append("\n── 冒烟测试（真实构造）──\n");
        try {
            Class<?> mc = ClassResolver.find(cl, "menu.item",
                    "com.tencent.qqnt.aio.menu.d",
                    "com.tencent.mobileqq.utils.dialogutils.QQCustomMenuItem");
            if (mc != null) {
                sb.append("菜单类构造器: ");
                for (java.lang.reflect.Constructor<?> c : mc.getDeclaredConstructors()) {
                    sb.append(c.toString()).append(" | ");
                }
                sb.append('\n');
            }
            
            DexIndex ix2 = DexIndexLoader.get(app);
            if (ix2 != null) {
                java.util.List<String> subs =
                        ix2.subclassesOf("com.tencent.qqnt.aio.menu.ui.f", true);
                sb.append("菜单项子类 ").append(subs.size()).append(" 个，可用单参构造的:\n");
                int shown = 0;
                for (String n : subs) {
                    if (shown >= 8) break;
                    try {
                        Class<?> c = Class.forName(n, false, cl);
                        for (java.lang.reflect.Constructor<?> ct : c.getDeclaredConstructors()) {
                            Class<?>[] ps = ct.getParameterTypes();
                            if (ps.length == 1 && ps[0] == android.content.Context.class) {
                                sb.append("   ✓ ").append(n).append("  (Context)\n");
                                shown++;
                                break;
                            }
                        }
                    } catch (Throwable ignore) {
                    }
                }
                if (shown == 0) sb.append("   （没有单参构造的子类）\n");
            }
        } catch (Throwable t) {
            sb.append("列构造器失败: ").append(t).append('\n');
        }
        
        try {
            Class<?> layout = ClassResolver.find(cl, "menu.expandableLayout",
                    "com.tencent.qqnt.aio.menu.ui.QQCustomMenuExpandableLayout");
            sb.append("菜单布局类（生产路径依赖）→ ")
              .append(layout == null ? "✗ 缺失" : "✓ " + layout.getName()).append('\n');
            Class<?> setMenuOwner = ClassResolver.find(cl, "menu.noIconLayout",
                    "com.tencent.qqnt.aio.menu.ui.QQCustomMenuNoIconLayout");
            sb.append("无图标菜单布局 → ")
              .append(setMenuOwner == null ? "✗ 缺失" : "✓ " + setMenuOwner.getName()).append('\n');
        } catch (Throwable t) {
            sb.append("菜单布局检查失败: ").append(t).append('\n');
        }
        try {
            String g = com.cathelperawa.owo.app.bridge.GrayTip.selfTest(cl);
            sb.append("灰字 JsonGrayElement → ").append(g == null ? "✗ null" : "✓ " + g).append('\n');
        } catch (Throwable t) {
            sb.append("灰字 → 抛异常 ").append(t).append('\n');
        }
        try {
            
            Object te = com.cathelperawa.owo.app.bridge.ElementFactory.text("自检");
            sb.append("文本元素 text() → ").append(te == null ? "✗ null" : "✓ " + te.getClass().getName()).append('\n');
        } catch (Throwable t) {
            sb.append("文本元素 text() → 抛异常 ").append(t).append('\n');
        }

        sb.append("\n── 群列表 ──\n");
        try {
            java.util.Map<String, String> tm =
                    com.cathelperawa.owo.app.bridge.TroopApi.allTroopNames();
            sb.append("取到 ").append(tm.size()).append(" 个群\n");
            int k = 0;
            for (java.util.Map.Entry<String, String> e : tm.entrySet()) {
                if (k++ >= 5) break;
                sb.append("  · ").append(e.getKey()).append(" ").append(e.getValue()).append('\n');
            }
            
            try {
                Class<?> impl = ClassResolver.find(cl, "troop.listRepoImpl",
                        "com.tencent.qqnt.troop.impl.TroopListRepoApiImpl");
                sb.append("类: ").append(impl == null ? "未找到" : impl.getName()).append('\n');
                if (impl != null) {
                    sb.append("构造器: ");
                    for (java.lang.reflect.Constructor<?> c : impl.getDeclaredConstructors())
                        sb.append(c.toString()).append(" | ");
                    sb.append('\n');
                    Object repo = null;
                    for (java.lang.reflect.Constructor<?> c : impl.getDeclaredConstructors()) {
                        Class<?>[] ps = c.getParameterTypes();
                        Object[] args = new Object[ps.length];
                        boolean ok = true;
                        for (int i = 0; i < ps.length; i++) {
                            if (ps[i] == android.content.Context.class) args[i] = app;
                            else if (ps[i].isPrimitive()) { ok = false; break; }
                            else args[i] = null;
                        }
                        if (!ok) continue;
                        try { c.setAccessible(true); repo = c.newInstance(args); break; } catch (Throwable e) { sb.append("  实例化失败:").append(e).append('\n'); }
                    }
                    if (repo != null) {
                        java.lang.reflect.Method m = impl.getMethod("getSortedJoinedTroopInfoFromCache");
                        Object res = m.invoke(repo);
                        sb.append("返回: ").append(res == null ? "null" : res.getClass().getName()).append('\n');
                        if (res instanceof java.util.List) {
                            java.util.List<?> l = (java.util.List<?>) res;
                            sb.append("size=").append(l.size()).append('\n');
                            if (!l.isEmpty()) {
                                Object o = l.get(0);
                                sb.append("元素类: ").append(o.getClass().getName()).append('\n');
                                int k2 = 0;
                                for (java.lang.reflect.Field fl : o.getClass().getFields()) {
                                    if (k2++ >= 12) break;
                                    try { sb.append("  ").append(fl.getName()).append("=")
                                            .append(String.valueOf(fl.get(o))).append('\n'); } catch (Throwable ignore) {}
                                }
                            }
                        }
                    }
                }
            } catch (Throwable t) {
                sb.append("详细诊断失败: ").append(t).append('\n');
            }
            java.io.File dir = app.getExternalFilesDir(null);
            sb.append("外部目录: ").append(dir == null ? "null" : dir.getAbsolutePath()).append('\n');
        } catch (Throwable t) {
            sb.append("取群列表失败: ").append(t).append('\n');
        }

        sb.append("\n── 宿主进程（跨进程问答）──\n");
        try {
            sb.append("本进程 ").append(com.cathelperawa.owo.app.core.Ipc.currentProcName())
              .append("  掩码=").append(com.cathelperawa.owo.app.core.Ipc.currentProcMask()).append('\n');
            sb.append(com.cathelperawa.owo.app.core.Ipc.enumerateSummary(
                    app, com.cathelperawa.owo.app.core.Ipc.PROC_ALL, 1200, "ping"));
        } catch (Throwable t) {
            sb.append("跨进程问答失败: ").append(t).append('\n');
        }

        sb.append("\n── 调度自检 ──\n");
        try {
            int before = com.cathelperawa.owo.app.core.LoadState.pending();
            com.cathelperawa.owo.app.core.LoadState.expect(2);
            int after = com.cathelperawa.owo.app.core.LoadState.pending();
            com.cathelperawa.owo.app.core.LoadState.completeOne();
            int mid = com.cathelperawa.owo.app.core.LoadState.pending();
            com.cathelperawa.owo.app.core.LoadState.completeOne();
            int end = com.cathelperawa.owo.app.core.LoadState.pending();
            sb.append("pending: ").append(before).append(" → 登记2后 ").append(after)
              .append(" → 完成1 ").append(mid).append(" → 完成2 ").append(end).append('\n');
            sb.append(end == before ? "✓ 计数闭环正常\n" : "✗ 计数没回到原值\n");
        } catch (Throwable t) {
            sb.append("调度自检失败: ").append(t).append('\n');
        }

        sb.append("\n── 功能自检（全部 HookItem.runSelfCheck）──\n");
        try {
            java.util.List<com.cathelperawa.owo.app.core.HookItemBase> all =
                    com.cathelperawa.owo.app.core.HookItemFactory.all();
            int okCnt = 0, badCnt = 0, skipped = 0;
            StringBuilder bad = new StringBuilder();
            for (com.cathelperawa.owo.app.core.HookItemBase it : all) {
                try {
                    com.cathelperawa.owo.app.core.SelfCheckResult r = it.runSelfCheck();
                    if (r == null || r.isEmpty()) { skipped++; continue; }
                    int fail = r.failCount();
                    String label = it.getClass().getSimpleName();
                    if (fail == 0) {
                        okCnt++;
                    } else {
                        badCnt++;
                        bad.append("   ✗ ").append(label).append("  失败 ").append(fail)
                           .append(" 项: ");
                        for (com.cathelperawa.owo.app.core.SelfCheckResult.Item i : r.items()) {
                            if (!i.ok) bad.append('[').append(i.name).append("] ");
                        }
                        bad.append('\n');
                    }
                } catch (Throwable t) {
                    skipped++;
                }
            }
            sb.append("功能 ").append(all.size()).append(" 个 · 正常 ").append(okCnt)
              .append(" · 有失败 ").append(badCnt).append(" · 未实现自检 ").append(skipped).append('\n');
            sb.append(bad);

            int deepBad2 = 0;
            StringBuilder dsb = new StringBuilder();
            for (com.cathelperawa.owo.app.core.HookItemBase it : all) {
                try {
                    String p = it.deepProblem();
                    if (p == null) continue;
                    deepBad2++;
                    dsb.append("  ⚠ ").append(it.featureName()).append("：").append(p).append('\n');
                } catch (Throwable ignore) {
                }
            }
            
            try {
                com.cathelperawa.owo.app.core.HealthRegistry.register("selftest.bad",
                        new com.cathelperawa.owo.app.core.HealthRegistry.Probe() {
                            @Override public String probe() { return "这是故意制造的问题"; }
                        });
                com.cathelperawa.owo.app.core.HealthRegistry.invalidate();
                String t = com.cathelperawa.owo.app.core.HealthRegistry.check("selftest.bad");
                sb.append("依赖链路自测：").append(t == null ? "✗ 没抓到（链路有问题）" : "✓ 抓到「" + t + "」").append('\n');
            } catch (Throwable t) {
                sb.append("依赖链路自测异常：").append(t).append('\n');
            }

            sb.append(deepBad2 == 0 ? "依赖检查：全部正常\n"
                    : "依赖异常 " + deepBad2 + " 项：\n" + dsb);
        } catch (Throwable t) {
            sb.append("跑自检失败: ").append(t).append('\n');
        }

        sb.append("\n── 类名体检 ──\n");
        try {
            sb.append(ClassAudit.run(app, ix));
        } catch (Throwable t) {
            sb.append("体检失败: ").append(t).append('\n');
        }

        sb.append("\n── 索引健康度 ──\n");
        sb.append("sendMsg 频次: ").append(ix.methodFrequency("sendMsg")).append('\n');
        sb.append("类名含 Notify 的: ")
          .append(ix.grep(java.util.regex.Pattern.compile("Notify")).size()).append('\n');
    }

    private static void timeQuery(StringBuilder sb, List<ClassFinder.Result> rs) {
        long t = System.currentTimeMillis();
        sb.append("命中 ").append(rs.size()).append(" 个\n");
        for (int i = 0; i < Math.min(6, rs.size()); i++) {
            sb.append("  · ").append(rs.get(i)).append('\n');
        }
        sb.append("查询耗时 ").append(System.currentTimeMillis() - t).append("ms\n");
    }

    private static void write(Context app, String text) {
        File f = new File(app.getFilesDir(), OUT);
        FileOutputStream os = null;
        try {
            os = new FileOutputStream(f);
            OutputStreamWriter w = new OutputStreamWriter(os, "UTF-8");
            w.write(text);
            w.flush();
        } catch (Throwable t) {
            HookLog.e("DexProbe 写文件失败", t);
        } finally {
            if (os != null) try { os.close(); } catch (Throwable ignored) {}
        }
        HookLog.i("DexProbe 完成 → " + f.getAbsolutePath());
    }
}
