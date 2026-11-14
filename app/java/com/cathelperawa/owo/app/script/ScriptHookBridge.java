package com.cathelperawa.owo.app.script;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.cathelperawa.owo.app.bridge.QQEnv;
import com.cathelperawa.owo.app.core.HookLog;
import com.cathelperawa.owo.app.core.Proc;

public final class ScriptHookBridge {

    private static final String ACTION = "com.cathelperawa.owo.app.SCRIPT_HOOK";
    private static final String K_EVENT = "event";
    private static final String K_DATA = "data";

    private static volatile boolean registered;

    private ScriptHookBridge() {}

    public static void emit(String event, String data) {
        if (event == null) return;
        try {
            boolean slave = Proc.isSlave();
            if (!slave) {

                ScriptBusRegistry.emit(event, data);
                return;
            }

            Context c = QQEnv.appContext();
            if (c == null) return;
            Intent i = new Intent(ACTION);
            i.setPackage(c.getPackageName());
            i.putExtra(K_EVENT, event);
            i.putExtra(K_DATA, data == null ? "" : data);
            c.sendBroadcast(i);
        } catch (Throwable t) {
            HookLog.w("[脚本钩子] 转发失败 " + t);
        }
    }

    public static synchronized void register(Context ctx) {
        if (registered || ctx == null) return;
        try {
            if (Proc.isSlave()) return;
            BroadcastReceiver r = new BroadcastReceiver() {
                @Override public void onReceive(Context c, Intent i) {
                    try {
                        String e = i.getStringExtra(K_EVENT);
                        String d = i.getStringExtra(K_DATA);
                        if (e != null) ScriptBusRegistry.emit(e, d);
                    } catch (Throwable t) {
                        HookLog.w("[脚本钩子] 接收失败 " + t);
                    }
                }
            };
            IntentFilter f = new IntentFilter(ACTION);
            try {
                ctx.registerReceiver(r, f, Context.RECEIVER_NOT_EXPORTED);
            } catch (Throwable ignore) {
                ctx.registerReceiver(r, f);
            }
            registered = true;
            HookLog.i("[脚本钩子] 跨进程接收器已注册");
        } catch (Throwable t) {
            HookLog.e("[脚本钩子] 注册失败", t);
        }
    }
}
