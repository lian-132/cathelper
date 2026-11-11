package com.cathelperawa.owo.app.core;

import android.app.AlertDialog;
import com.cathelperawa.owo.app.core.Bg;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.widget.ScrollView;
import android.widget.TextView;

import com.cathelperawa.owo.app.api.EventApi;
import com.cathelperawa.owo.app.api.NetApi;
import com.cathelperawa.owo.app.api.TaskApi;
import com.cathelperawa.owo.app.core.dex.ClassAudit;
import com.cathelperawa.owo.app.core.dex.ClassFinder;
import com.cathelperawa.owo.app.core.dex.ClassResolver;
import com.cathelperawa.owo.app.core.dex.DexIndex;
import com.cathelperawa.owo.app.core.dex.DexIndexLoader;

import java.util.List;

public final class InProcCheck {

    private InProcCheck() {}

    public static void show(Context ctx) {
        final Context app = ctx.getApplicationContext();
        final Handler ui = new Handler(Looper.getMainLooper());
        Bg.run(new Runnable() {
            @Override public void run() {
                final String report = build(app);
                ui.post(new Runnable() {
                    @Override public void run() {
                        try {
                            showDialog(app, report);
                        } catch (Throwable t) {
                            HookLog.e("体检弹窗失败", t);
                        }
                    }
                });
            }
        }, "cat-inproc-check");
    }

    public static String build(Context app) {
        StringBuilder sb = new StringBuilder();
        long t0 = System.currentTimeMillis();

        sb.append("══ 猫娘助手 · 全面体检 ══\n");
        sb.append("宿主 ").append(app.getPackageName())
          .append("  pid ").append(android.os.Process.myPid()).append('\n');
        Runtime rt = Runtime.getRuntime();
        sb.append("堆 ").append((rt.totalMemory() - rt.freeMemory()) / 1024 / 1024)
          .append("/").append(rt.maxMemory() / 1024 / 1024).append("MB\n\n");

        sb.append("── 功能自检 ──\n");
        try {
            List<HookItemBase> all = HookItemFactory.all();
            int ok = 0, bad = 0, none = 0;
            StringBuilder fails = new StringBuilder();
            StringBuilder nones = new StringBuilder();   
            for (HookItemBase it : all) {
                try {
                    SelfCheckResult r = it.runSelfCheck();
                    if (r == null || r.isEmpty()) {
                        none++;
                        nones.append("  · ").append(it.featureName()).append("\n");
                        continue;
                    }
                    if (r.failCount() == 0) { ok++; continue; }
                    bad++;
                    fails.append("  ✗ ").append(it.getClass().getSimpleName())
                         .append(": ").append(r.firstFailReason()).append('\n');
                } catch (Throwable t) {
                    none++;
                    nones.append("  · ").append(it.featureName()).append("（自检抛异常）\n");
                }
            }
            sb.append("功能 ").append(all.size())
              .append(" · 正常 ").append(ok)
              .append(" · 失败 ").append(bad)
              .append(" · 未自检 ").append(none).append('\n');
            sb.append(fails);
            if (none > 0) sb.append("  未自检的功能：\n").append(nones);

            try {
                sb.append(com.cathelperawa.owo.app.script.ScriptSelfTest.section());
            } catch (Throwable t) {
                sb.append("── 脚本子系统 ──\n✗ 体检失败 ").append(t).append("\n\n");
            }

            int deepBad = 0;
            StringBuilder deps = new StringBuilder();
            for (HookItemBase it : all) {
                try {
                    String p = it.deepProblem();
                    if (p == null) continue;
                    deepBad++;
                    deps.append("  ⚠ ").append(it.featureName()).append("：").append(p).append('\n');
                } catch (Throwable ignore) {
                }
            }
            if (deepBad > 0) {
                sb.append("依赖异常 ").append(deepBad).append(" 项：\n").append(deps);
            } else {
                sb.append("依赖检查：全部正常\n");
            }
        } catch (Throwable t) {
            sb.append("功能自检失败: ").append(t).append('\n');
        }

        sb.append("\n── API 总线 ──\n");
        try {
            sb.append("事件  监听 ").append(EventApi.listenerCount())
              .append(" · 队列 ").append(EventApi.queuedCount())
              .append(" · 派发 ").append(EventApi.dispatchedCount())
              .append(" · 投递 ").append(EventApi.deliveredCount())
              .append(" · 丢弃 ").append(EventApi.droppedCount())
              .append(" · 错误 ").append(EventApi.errorCount())
              .append(EventApi.isRunning() ? " · 线程运行中\n"
                      : " · 线程未启动（无订阅者时正常）\n");
            sb.append("网络  排队/活动 ").append(NetApi.pending())
              .append(" · 默认超时 ").append(NetApi.DEFAULT_TIMEOUT / 1000).append("s")
              .append(" · 响应上限 ").append(NetApi.MAX_BODY / 1024 / 1024).append("MB\n");
            sb.append("定时  存活 ").append(TaskApi.count())
              .append(" · 累计异常 ").append(TaskApi.errorCount()).append('\n');
        } catch (Throwable t) {
            sb.append("API 总线检查失败: ").append(t).append('\n');
        }

        sb.append("\n── 宿主进程 ──\n");
        try {
            sb.append("本进程 ").append(Ipc.currentProcName())
              .append("  掩码=").append(Ipc.currentProcMask()).append('\n');
            String others = Ipc.enumerateSummary(app, Ipc.PROC_ALL, 1200, "ping");
            sb.append("全进程应答：\n").append(others);
        } catch (Throwable t) {
            sb.append("跨进程问答失败: ").append(t).append('\n');
        }

        DexIndex ix = null;
        if (com.cathelperawa.owo.app.core.Host.isQQ()) {

            sb.append("\n── 宿主 dex ──\n");
            sb.append("（QQ 版跳过 dex 索引与类名体检，省时间）\n");
        } else {
            
            sb.append("\n── 宿主 dex ──\n");
            try {
                long a = System.currentTimeMillis();
                ix = DexIndexLoader.get(app);
                long b = System.currentTimeMillis();
                if (ix == null) {
                    sb.append("✗ 建索引失败\n");
                } else {
                    sb.append("类 ").append(ix.size())
                      .append("  载入 ").append(b - a).append("ms")
                      .append(DexIndexLoader.lastHitCache() ? "（缓存）" : "（现场构建）").append('\n');
                }
            } catch (Throwable t) {
                sb.append("索引异常: ").append(t).append('\n');
            }

            sb.append("\n── 类名体检 ──\n");
            try {
                if (ix != null) {
                    String audit = ClassAudit.run(app, ix);
                    
                    boolean inBroken = false;
                    for (String line : audit.split("\n")) {
                        if (line.startsWith("==") || line.isEmpty()) continue;
                        if (line.contains("调用点")) { sb.append(line).append('\n'); continue; }
                        if (line.startsWith("✗")) { inBroken = true; sb.append(line).append('\n'); continue; }
                        if (inBroken && line.startsWith("      ")) { sb.append(line).append('\n'); continue; }
                        if (line.startsWith("✓") || line.startsWith("──")) { inBroken = false; }
                        if (line.startsWith("✓")) sb.append(line).append('\n');
                    }
                } else {
                    sb.append("（无索引，跳过）\n");
                }
            } catch (Throwable t) {
                sb.append("体检异常: ").append(t).append('\n');
            }

            sb.append("\n── 隐私体检 ──\n");
            try {
                sb.append(PrivacyCheck.build(app));
            } catch (Throwable t) {
                sb.append("隐私体检失败: ").append(t).append('\n');
            }
        }

        sb.append("\n── 冒烟测试 ──\n");
        try {
            String g = com.cathelperawa.owo.app.bridge.GrayTip.selfTest(
                    app.getClassLoader());
            sb.append("灰字元素      ").append(g == null ? "✗ null" : "✓ " + simple(g)).append('\n');
        } catch (Throwable t) {
            sb.append("灰字元素      ✗ ").append(t).append('\n');
        }
        try {
            Object te = com.cathelperawa.owo.app.bridge.ElementFactory.text("自检");
            sb.append("文本元素      ").append(te == null ? "✗ null" : "✓ " + simple(te.getClass().getName())).append('\n');
        } catch (Throwable t) {
            sb.append("文本元素      ✗ ").append(t).append('\n');
        }
        try {
            Class<?> lay = ClassResolver.find(app.getClassLoader(), "menu.expandableLayout",
                    "com.tencent.qqnt.aio.menu.ui.QQCustomMenuExpandableLayout");
            sb.append("菜单布局      ").append(lay == null ? "✗ 缺失" : "✓ " + simple(lay.getName())).append('\n');
        } catch (Throwable t) {
            sb.append("菜单布局      ✗ ").append(t).append('\n');
        }

        sb.append("\n── 新功能专项 ──\n");
        ClassLoader hcl = app.getClassLoader();
        sb.append(probeClockIn(hcl)).append('\n');
        sb.append(probeMsgTime(hcl)).append('\n');
        sb.append(probeViewCard(hcl)).append('\n');
        sb.append(probeSendProto(hcl)).append('\n');

        
        
        if (ix != null && !com.cathelperawa.owo.app.core.Host.isQQ()) {
            sb.append("\n── 查询抽查 ──\n");
            ClassFinder f = new ClassFinder(ix);
            sb.append("View 子类+onDraw   ")
              .append(f.query().superClass("android.view.View").withMethod("onDraw").find().size())
              .append(" 个\n");
            List<String> sub = ix.subclassesOf("com.tencent.mobileqq.data.MessageRecord", false);
            sb.append("MessageRecord 子类 ").append(sub.size()).append(" 个\n");
        }

        sb.append("\n用时 ").append(System.currentTimeMillis() - t0).append("ms");
        return sb.toString();
    }

    private static String simple(String fqcn) {
        int i = fqcn.lastIndexOf('.');
        return i >= 0 ? fqcn.substring(i + 1) : fqcn;
    }

    private static void showDialog(Context app, String report) {
        Context ctx = app;
        android.app.Activity top = null;
        try {
            top = QQEnvTop.activity();
        } catch (Throwable ignore) {
        }
        if (top != null && !top.isFinishing()) ctx = top;

        TextView tv = new TextView(ctx);
        tv.setText(report);
        tv.setTextSize(11);
        tv.setTypeface(Typeface.MONOSPACE);
        int pad = (int) (12 * ctx.getResources().getDisplayMetrics().density);
        tv.setPadding(pad, pad, pad, pad);
        tv.setTextIsSelectable(true);

        ScrollView sv = new ScrollView(ctx);
        sv.addView(tv);

        new AlertDialog.Builder(ctx)
                .setTitle("全面体检")
                .setView(sv)
                .setPositiveButton("关闭", null)
                .show();
    }

    private static final class QQEnvTop {
        static android.app.Activity activity() {
            try {
                return com.cathelperawa.owo.app.bridge.QQEnv.topActivity();
            } catch (Throwable t) {
                return null;
            }
        }
    }

    

    
    private static String probeClockIn(ClassLoader cl) {
        String name = "com.tencent.mobileqq.troop.clockin.handler.TroopClockInHandler";
        Class<?> c = load(name, cl);
        if (c == null) return "群打卡        ✗ 缺 " + simple(name);
        int n = 0;
        for (java.lang.reflect.Method m : c.getDeclaredMethods()) {
            Class<?>[] ps = m.getParameterTypes();
            if (ps.length == 2 && ps[0] == String.class && ps[1] == String.class
                    && m.getReturnType() == void.class) n++;
        }
        if (n == 0) return "群打卡        ✗ 类在，但没有 (String,String)→void 方法";
        return "群打卡        ✓ " + simple(name) + " 打卡方法 " + n + " 个";
    }

    private static String probeMsgTime(ClassLoader cl) {
        String vb = "com.tencent.mobileqq.aio.msglist.holder.AIOBubbleMsgItemVB";
        Class<?> c = load(vb, cl);
        if (c == null) return "消息时间      ✗ 缺 " + simple(vb);
        int n = 0;
        for (java.lang.reflect.Method m : c.getDeclaredMethods()) {
            Class<?>[] ps = m.getParameterTypes();
            if (ps.length != 4) continue;
            if (ps[0] != int.class) continue;
            if (!java.util.List.class.isAssignableFrom(ps[2])) continue;
            if (!"android.os.Bundle".equals(ps[3].getName())) continue;
            if (m.getReturnType() != void.class) continue;
            n++;
        }
        boolean bubble = load("com.tencent.qqnt.aio.holder.template.BubbleLayoutCompatPress", cl) != null;
        if (n == 0) return "消息时间      ✗ 类在，但没找到 (int,msgItem,List,Bundle)→void";
        return "消息时间      ✓ bind 方法 " + n + " 个 · 气泡容器 " + (bubble ? "✓" : "✗");
    }

    private static String probeViewCard(ClassLoader cl) {
        String impl = "com.tencent.mobileqq.profilecard.api.impl.ProfileDataServiceImpl";
        String proc = "com.tencent.mobileqq.profilecard.processor.ProfileSecureProcessor";
        Class<?> a = load(impl, cl);
        if (a == null) return "异常资料卡    ✗ 缺 " + simple(impl);
        int n = 0;
        for (java.lang.reflect.Method m : a.getDeclaredMethods()) {
            String mn = m.getName();
            if (!"getProfileCard".equals(mn) && !"getProfileCardFromCache".equals(mn)) continue;
            if (m.getReturnType().getSimpleName().equals("Card")) n++;
        }
        boolean hasProc = false;
        Class<?> b = load(proc, cl);
        if (b != null) {
            for (java.lang.reflect.Method m : b.getDeclaredMethods()) {
                if ("processProfileCard".equals(m.getName())) { hasProc = true; break; }
            }
        }
        return "异常资料卡    " + (n > 0 ? "✓" : "✗") + " 取卡方法 " + n
                + " 个 · 安全处理器 " + (hasProc ? "✓" : "✗");
    }

    private static String probeSendProto(ClassLoader hcl) {
        Class<?> et = load("com.tencent.mobileqq.aio.input.edit.AIOEditText", hcl);
        String etState = et == null ? "✗ 缺 AIOEditText" : "✓ 输入框";

        Class<?> me = load("com.tencent.qqnt.kernel.nativeinterface.MsgElement", hcl);
        Class<?> ark = load("com.tencent.qqnt.kernel.nativeinterface.ArkElement", hcl);
        Class<?> contact = load("com.tencent.qqnt.kernelpublic.nativeinterface.Contact", hcl);

        int fields = 0;
        if (ark != null) {
            for (java.lang.reflect.Field f : ark.getFields()) {
                if ("bytesData".equals(f.getName())) fields++;
            }
        }

        String kern = (me != null && ark != null && contact != null)
                ? ("✓ 内核 3 件套 · bytesData " + (fields > 0 ? "有" : "缺"))
                : ("✗ 内核元素缺：" + (me == null ? "MsgElement " : "")
                    + (ark == null ? "ArkElement " : "")
                    + (contact == null ? "Contact" : ""));
        return "发送与协议    " + etState + " · " + kern;
    }

    private static Class<?> load(String name, ClassLoader cl) {
        try {
            return Class.forName(name, false, cl);
        } catch (Throwable t) {
            return null;
        }
    }

}
