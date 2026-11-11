package com.cathelperawa.owo.app.core;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class Ipc {

    public static final String TAG = "CatIpc";

    public static final int PROC_MAIN = 1;
    public static final int PROC_MSF = 1 << 1;
    public static final int PROC_PEAK = 1 << 2;
    public static final int PROC_TOOL = 1 << 3;
    public static final int PROC_QZONE = 1 << 4;
    public static final int PROC_OTHER = 1 << 5;
    public static final int PROC_ALL = PROC_MAIN | PROC_MSF | PROC_PEAK
            | PROC_TOOL | PROC_QZONE | PROC_OTHER;

    public static final String ACTION_REQ = "com.cathelperawa.owo.app.IPC.REQ";
    public static final String ACTION_REP = "com.cathelperawa.owo.app.IPC.REP";

    private static final String K_SEQ = "seq";
    private static final String K_WHAT = "what";
    private static final String K_MASK = "mask";
    private static final String K_PID = "pid";
    private static final String K_PROC = "proc";
    private static final String K_TEXT = "text";

    public static final class Reply {
        public final int pid;
        public final String proc;
        public final int mask;
        public final String text;

        Reply(int pid, String proc, int mask, String text) {
            this.pid = pid;
            this.proc = proc;
            this.mask = mask;
            this.text = text;
        }

        @Override public String toString() {
            return proc + "#" + pid + "  " + text;
        }
    }

    private static volatile boolean sRegistered;
    private static volatile long sStartAt = System.currentTimeMillis();

    private Ipc() {}

    
    public static synchronized void register(final Context ctx) {
        if (ctx == null || sRegistered) return;
        try {
            BroadcastReceiver r = new BroadcastReceiver() {
                @Override public void onReceive(Context c, Intent i) {
                    try { onRequest(c, i); } catch (Throwable t) {
                        HookLog.e("Ipc 处理请求失败", t);
                    }
                }
            };
            IntentFilter f = new IntentFilter(ACTION_REQ);
            registerOn(ctx, r, f);
            sRegistered = true;
            HangLog("Ipc 已注册 " + currentProcName());
        } catch (Throwable t) {
            HookLog.e("Ipc 注册失败", t);
        }
    }

    private static void HangLog(String s) {
        HookLog.i(s);
    }

    private static void registerOn(Context ctx, BroadcastReceiver r, IntentFilter f) {
        if (Build.VERSION.SDK_INT >= 33) {
            ctx.registerReceiver(r, f, Context.RECEIVER_NOT_EXPORTED);
            return;
        }
        String perm = pickNotExportedPerm(ctx);
        if (perm != null) {
            try {
                ctx.registerReceiver(r, f, perm, null);
                HookLog.i("Ipc 注册（带权限 " + perm + "）");
                return;
            } catch (Throwable ignore) {
            }
        }
        ctx.registerReceiver(r, f);
        HookLog.i("Ipc 注册（裸注册，宿主未声明动态接收器权限）");
    }

    private static String pickNotExportedPerm(Context ctx) {
        try {
            String pkg = ctx.getPackageName();
            String[] cands = {
                    pkg + ".DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION",
                    pkg + ".permission.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION",
                    pkg + ".permission.RECEIVER_NOT_EXPORTED",
            };
            android.content.pm.PackageInfo pi = ctx.getPackageManager()
                    .getPackageInfo(pkg, android.content.pm.PackageManager.GET_PERMISSIONS);
            java.util.Set<String> declared = new java.util.HashSet<String>();
            if (pi != null && pi.requestedPermissions != null) {
                for (String p : pi.requestedPermissions) if (p != null) declared.add(p);
            }
            for (String c : cands) if (declared.contains(c)) return c;
        } catch (Throwable ignore) {
        }
        return null;
    }

    private static void onRequest(Context ctx, Intent in) {
        String what = in.getStringExtra(K_WHAT);
        if (what == null) return;
        int reqMask = in.getIntExtra(K_MASK, PROC_ALL);
        int me = currentProcMask();
        if ((me & reqMask) == 0) return;         

        long seq = in.getLongExtra(K_SEQ, 0);
        String answer = answer(what);
        if (answer == null) return;              

        Intent out = new Intent(ACTION_REP);
        out.putExtra(K_SEQ, seq);
        out.putExtra(K_PID, android.os.Process.myPid());
        out.putExtra(K_PROC, currentProcName());
        out.putExtra(K_MASK, me);
        out.putExtra(K_TEXT, answer);
        try {
            out.setPackage(ctx.getPackageName());   
            ctx.sendBroadcast(out);
        } catch (Throwable t) {
            HookLog.e("Ipc 回包失败", t);
        }
    }

    private static String answer(String what) {
        if ("ping".equals(what)) {
            return "uptime=" + (System.currentTimeMillis() - sStartAt) / 1000 + "s"
                    + " " + LoadState.summary();
        }
        if ("hooks".equals(what)) {
            try {
                int n = HookItemFactory.all().size();
                int bad = 0;
                for (HookItemBase it : HookItemFactory.all()) {
                    if (it.report() != null && it.report().status() == FeatureStatus.FAILED) bad++;
                }
                return "功能=" + n + " 失败=" + bad;
            } catch (Throwable t) {
                return "无法统计：" + t;
            }
        }
        if ("dex".equals(what)) {
            try {
                com.cathelperawa.owo.app.core.dex.DexIndex ix =
                        com.cathelperawa.owo.app.core.dex.DexIndexLoader.get(
                                HookItemFactory.context());
                return ix == null ? "未建" : "类=" + ix.size();
            } catch (Throwable t) {
                return "异常：" + t;
            }
        }
        if ("troops".equals(what)) {
            
            try {
                java.util.Map<String, String> m =
                        com.cathelperawa.owo.app.bridge.TroopApi.allTroopNames();
                StringBuilder sb = new StringBuilder();
                int n = 0;
                for (java.util.Map.Entry<String, String> e : m.entrySet()) {
                    if (n++ >= 300) break;                 
                    if (sb.length() > 0) sb.append(',');
                    sb.append(e.getKey()).append('|').append(e.getValue());
                }
                return sb.length() == 0 ? "EMPTY" : sb.toString();
            } catch (Throwable t) {
                return "ERR:" + t;
            }
        }
        if ("stage".equals(what)) {
            try {
                return LoadState.summary();
            } catch (Throwable t) {
                return "无法读取阶段：" + t;
            }
        }
        if ("errors".equals(what)) {
            try {
                return "错误种类=" + RuntimeErrors.kindCount()
                        + " 总数=" + RuntimeErrors.totalCount();
            } catch (Throwable t) {
                return "无法统计：" + t;
            }
        }
        return null;
    }

    
    public static List<Reply> enumerate(Context ctx, int mask, long timeoutMs, String what) {
        final List<Reply> out = new ArrayList<>();
        if (ctx == null || what == null) return out;
        final long seq = System.nanoTime() ^ (long) (Math.random() * 1_000_000);
        final Map<Integer, Boolean> seen = new ConcurrentHashMap<>();

        final BroadcastReceiver rx = new BroadcastReceiver() {
            @Override public void onReceive(Context c, Intent i) {
                if (i.getLongExtra(K_SEQ, -1) != seq) return;      
                int pid = i.getIntExtra(K_PID, 0);
                if (seen.put(pid, Boolean.TRUE) != null) return;    
                String proc = i.getStringExtra(K_PROC);
                String text = i.getStringExtra(K_TEXT);
                out.add(new Reply(pid, proc == null ? "?" : proc,
                        i.getIntExtra(K_MASK, 0), text == null ? "" : text));
            }
        };

        IntentFilter f = new IntentFilter(ACTION_REP);
        try {
            registerOn(ctx, rx, f);
        } catch (Throwable t) {
            HookLog.e("Ipc 注册回包接收器失败", t);
            return out;
        }

        try {
            Intent req = new Intent(ACTION_REQ);
            req.putExtra(K_SEQ, seq);
            req.putExtra(K_WHAT, what);
            req.putExtra(K_MASK, mask);
            req.setPackage(ctx.getPackageName());   
            ctx.sendBroadcast(req);
            long deadline = System.currentTimeMillis() + timeoutMs;
            while (System.currentTimeMillis() < deadline) {
                try { Thread.sleep(30); } catch (InterruptedException ignore) {}
            }
        } catch (Throwable t) {
            HookLog.e("Ipc 发问失败", t);
        } finally {
            try { ctx.unregisterReceiver(rx); } catch (Throwable ignore) {}
        }
        return out;
    }

    public static String enumerateSummary(Context ctx, int mask, long timeoutMs, String what) {
        List<Reply> rs = enumerate(ctx, mask, timeoutMs, what);
        if (rs.isEmpty()) return "（没有进程应答）";
        StringBuilder sb = new StringBuilder();
        for (Reply r : rs) {
            sb.append("  · ").append(r.proc).append("(").append(r.pid).append(") ")
              .append(r.text).append('\n');
        }
        return sb.toString();
    }

    
    public static String currentProcName() {
        try {
            BufferedReader r = new BufferedReader(new FileReader("/proc/self/cmdline"));
            String s = r.readLine();
            r.close();
            if (s != null && s.length() > 0) return s.trim();
        } catch (Throwable ignore) {
        }
        try {
            Context c = HookItemFactory.context();
            if (c != null) return c.getPackageName();
        } catch (Throwable ignore) {
        }
        return "?";
    }

    public static int currentProcMask() {
        String n = currentProcName();
        int colon = n.indexOf(':');
        if (colon < 0) return PROC_MAIN;
        String sub = n.substring(colon + 1).toLowerCase();
        if (sub.contains("msf")) return PROC_MSF;
        if (sub.contains("peak")) return PROC_PEAK;
        if (sub.contains("tool")) return PROC_TOOL;
        if (sub.contains("qzone")) return PROC_QZONE;
        return PROC_OTHER;
    }
}
