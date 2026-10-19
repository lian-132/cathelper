package com.cathelperawa.owo.app.feature;

import com.cathelperawa.owo.app.MsgStore;
import com.cathelperawa.owo.app.core.HookApi;
import com.cathelperawa.owo.app.core.HookItem;
import com.cathelperawa.owo.app.core.HookItemBase;
import com.cathelperawa.owo.app.core.HookLog;
import com.cathelperawa.owo.app.ovo.FunProtoData;
import com.cathelperawa.owo.app.util.Refl;

import org.json.JSONObject;

import java.lang.reflect.Method;

import de.robv.android.xposed.XposedHelpers;

@HookItem(name = "MSF响应监听", path = "开发者/MSF响应监听", description = "照抄ovo拦MSF响应", order = 30)
public class FeatureMsfTap extends HookItemBase {

    public FeatureMsfTap() {}

    public static final String[] CMDS = new String[]{
            "MessageSvc.PbGetGroupMsg",
            "MessageSvc.PbGetOneDayRoamMsg",
            "trpc.msg.olpush.OlPushService.MsgPush",
            "MessageSvc.PbSendMsg",
    };

    public interface Listener {
        void onResp(String cmd, JSONObject content);
    }

    private static volatile Listener pending;
    private static volatile long pendingSince = 0;

    private static volatile String pendingCmd;

    public static void expect(String cmd, Listener l) {
        pending = l;
        pendingCmd = cmd;
        pendingSince = System.currentTimeMillis();
    }

    public static void cancel() {
        pending = null;
        pendingCmd = null;
    }

    private static volatile String lastCmd;
    private static volatile String lastJson;

    public static String lastRaw() {
        return lastJson;
    }

    @Override
    public void entry(ClassLoader cl) {
        hint("MSF 收包时");
        noInstrument();
        try {
            Class<?> h = XposedHelpers.findClassIfExists(
                    "mqq.app.msghandle.MsgRespHandler", cl);
            if (h == null) {
                fail("未找到 MsgRespHandler");
                return;
            }
            int n = 0;
            for (final Method m : h.getDeclaredMethods()) {
                if (!"dispatchRespMsg".equals(m.getName())) continue;
                hookBefore(m, new HookApi.Action() {
                    @Override
                    public void call(de.robv.android.xposed.XC_MethodHook.MethodHookParam p) {
                        onResp(p.args);
                    }
                });
                n++;
            }
            HookLog.i("已挂 dispatchRespMsg × " + n);
        } catch (Throwable t) {
            HookLog.e("挂 dispatchRespMsg 失败", t);
        }
    }

    private static void onResp(Object[] args) {
        try {
            if (args == null || args.length < 2 || args[1] == null) return;
            Object holder = args[1];
            Object fromMsg = Refl.field(holder, "fromServiceMsg");
            if (fromMsg == null) {

                for (Object a : args) {
                    if (a != null && a.getClass().getName().contains("FromServiceMsg")) {
                        fromMsg = a;
                        break;
                    }
                }
            }
            if (fromMsg == null) return;

            String cmd = Refl.strOr(Refl.call(fromMsg, "getServiceCmd"), null);
            if (cmd == null) cmd = Refl.strOr(Refl.field(fromMsg, "serviceCmd"), null);
            if (cmd == null) return;

            byte[] buf = null;
            Object wb = Refl.call(fromMsg, "getWupBuffer");
            if (wb instanceof byte[]) buf = (byte[]) wb;
            if (buf == null) {
                Object f = Refl.field(fromMsg, "wupBuffer");
                if (f instanceof byte[]) buf = (byte[]) f;
            }
            if (buf == null || buf.length < 8) return;

            if (!isWanted(cmd)) return;
            handle(cmd, buf);
        } catch (Throwable t) {
            HookLog.e("MsfTap.onResp 异常", t);
        }
    }

    private static boolean isWanted(String cmd) {
        for (String c : CMDS) {
            if (c.equals(cmd)) return true;
        }
        return false;
    }

    public static String decode(byte[] buf) {
        try {
            FunProtoData d = new FunProtoData();
            d.fromBytes(FunProtoData.getUnpPackage(buf));
            return d.toJSON().toString(2);
        } catch (Throwable t) {
            return null;
        }
    }

    private static void handle(String cmd, byte[] buf) {
        try {
            FunProtoData d = new FunProtoData();
            d.fromBytes(FunProtoData.getUnpPackage(buf));
            JSONObject content = d.toJSON();
            lastCmd = cmd;
            lastJson = content.toString(2);

            MsgStore.add("【MSF响应】cmd=" + cmd + " len=" + buf.length);

            Listener l = pending;
            if (l != null && cmd.equals(pendingCmd)
                    && System.currentTimeMillis() - pendingSince < 10000) {
                pending = null;
                pendingCmd = null;
                l.onResp(cmd, content);
            }
        } catch (Throwable t) {
            HookLog.e("MsfTap.handle 失败 cmd=" + cmd, t);
        }
    }

    public static Object extractElements(JSONObject content) {
        if (content == null) return null;
        try {
            JSONObject o6 = content.optJSONObject("6");
            if (o6 == null) return null;
            JSONObject o3 = o6.optJSONObject("3");
            if (o3 == null) return null;
            JSONObject o1 = o3.optJSONObject("1");
            if (o1 == null) return null;
            return o1.opt("2");
        } catch (Throwable t) {
            return null;
        }
    }
}
