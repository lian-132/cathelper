package com.cathelperawa.owo.app.core;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;

public final class ModuleCommand {

    private ModuleCommand() {}

    public static final String ACTION = "com.cathelperawa.owo.app.CMD";

    public static final String CMD_RELOAD_CONFIG = "reload_config";
    public static final String CMD_FLUSH_STATUS = "flush_status";
    public static final String CMD_CLEAR_ERRORS = "clear_errors";
    public static final String CMD_EXPORT_REPORT = "export_report";
    public static final String CMD_SNAPSHOT_CONFIG = "snapshot_config";
    public static final String CMD_IMPORT_CONFIG = "import_config";

    private static final String EXTRA_CMD = "cmd";

    private static volatile Context lastCtx;

    private static volatile boolean registered = false;
    private static volatile long lastCmdAt = 0;
    private static volatile String lastCmd = "";

    public static synchronized void register(final Context ctx) {
        if (ctx == null) return;
        lastCtx = ctx;
        if (registered) return;
        try {
            android.content.BroadcastReceiver receiver = new android.content.BroadcastReceiver() {
                @Override
                public void onReceive(Context c, Intent intent) {
                    try {
                        dispatch(intent);
                    } catch (Throwable t) {
                        HookLog.e("ModuleCommand 处理失败", t);
                    }
                }
            };
            IntentFilter filter = new IntentFilter(ACTION);

            if (Build.VERSION.SDK_INT >= 33) {
                ctx.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED);
            } else {
                ctx.registerReceiver(receiver, filter);
            }
            registered = true;
            HookLog.i("ModuleCommand 已注册");
        } catch (Throwable t) {
            HookLog.e("ModuleCommand 注册失败", t);
        }
    }

    private static void dispatch(Intent intent) {
        if (intent == null) return;
        String cmd = intent.getStringExtra(EXTRA_CMD);
        if (cmd == null) return;
        lastCmd = cmd;
        lastCmdAt = System.currentTimeMillis();

        if (CMD_RELOAD_CONFIG.equals(cmd)) {
            HookConfig.reload();
            HookLog.i("收到命令：重载配置");
            return;
        }
        if (CMD_FLUSH_STATUS.equals(cmd)) {
            Context ctx = HookItemFactory.context();
            if (ctx != null) StatusStore.save(ctx);
            HookLog.i("收到命令：立即落盘状态");
            return;
        }
        if (CMD_CLEAR_ERRORS.equals(cmd)) {
            RuntimeErrors.clear();
            HookLog.i("收到命令：清空运行期错误");
            return;
        }
        if (CMD_SNAPSHOT_CONFIG.equals(cmd)) {
            Context c = HookItemFactory.context();
            if (c == null) c = lastCtx;
            if (c == null) { HookLog.w("收到命令：配置快照，但没有 Context"); return; }
            int n = ConfigBank.writeSnapshot(c);
            HookLog.i("收到命令：配置快照 已写 " + n + " 项");
            return;
        }
        if (CMD_IMPORT_CONFIG.equals(cmd)) {
            Context c = HookItemFactory.context();
            if (c == null) c = lastCtx;
            if (c == null) { HookLog.w("收到命令：导入配置，但没有 Context"); return; }
            ConfigBank.ImportResult r = ConfigBank.applyPendingImport(c);
            HookLog.i("收到命令：导入配置 " + (r.ok ? "成功 " + r.applied + " 项" : "失败 " + r.message));
            return;
        }
        if (CMD_EXPORT_REPORT.equals(cmd)) {

            Context c = HookItemFactory.context();
            if (c == null) c = lastCtx;
            if (c == null) { HookLog.w("收到命令：打包诊断包，但没有 Context"); return; }
            final Context fc = c;

            Thread t = new Thread(new Runnable() {
                @Override public void run() {
                    BugReportExporter.Result r = BugReportExporter.export(fc);
                    HookLog.i("诊断包：" + (r.ok() ? r.file.getAbsolutePath() + " " + r.message : r.message));
                }
            }, "CatHelper-Report");
            t.setDaemon(true);
            t.start();
            return;
        }

        HookLog.w("ModuleCommand 忽略未知命令: " + cmd);
    }

    public static boolean send(Context ctx, String cmd) {
        if (ctx == null || cmd == null) return false;
        if (!isKnown(cmd)) {
            HookLog.w("ModuleCommand 拒绝发送未知命令: " + cmd);
            return false;
        }
        try {
            Intent i = new Intent(ACTION);
            i.putExtra(EXTRA_CMD, cmd);

            i.setPackage(hostPackage());
            ctx.sendBroadcast(i);
            return true;
        } catch (Throwable t) {
            HookLog.e("ModuleCommand 发送失败: " + cmd, t);
            return false;
        }
    }

    private static boolean isKnown(String cmd) {
        return CMD_RELOAD_CONFIG.equals(cmd)
                || CMD_FLUSH_STATUS.equals(cmd)
                || CMD_CLEAR_ERRORS.equals(cmd)
                || CMD_EXPORT_REPORT.equals(cmd)
                || CMD_SNAPSHOT_CONFIG.equals(cmd)
                || CMD_IMPORT_CONFIG.equals(cmd);
    }

    private static String hostPackage() {
        try {
            Context c = HookItemFactory.context();
            if (c != null) return c.getPackageName();
        } catch (Throwable ignore) {
        }

        return "com.tencent.tim";
    }

    public static boolean isRegistered() {
        return registered;
    }

    public static String lastCommand() {
        return lastCmd;
    }

    public static long lastCommandAt() {
        return lastCmdAt;
    }
}
