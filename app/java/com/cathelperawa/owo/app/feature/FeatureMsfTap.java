package com.cathelperawa.owo.app.feature;

import com.cathelperawa.owo.app.core.LoadPriority;
import com.cathelperawa.owo.app.core.SelfCheckResult;
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

@HookItem(name = "MSF响应监听", path = "开发者/MSF响应监听", description = "照抄ovo拦MSF响应", order = 30,
        priority = LoadPriority.BACKGROUND, hosts = {"tim", "qq"})
public class FeatureMsfTap extends HookItemBase {

    @Override
    protected void check(SelfCheckResult.Checker c) {
        c.cls("MSF 响应处理器", "mqq.app.msghandle.MsgRespHandler");
    }

    public static final String[] CMDS = new String[]{
            "MessageSvc.PbGetGroupMsg",
            "MessageSvc.PbGetOneDayRoamMsg",
            "trpc.msg.olpush.OlPushService.MsgPush",
            "MessageSvc.PbSendMsg",
    };

    public interface Listener {
        void onResp(String cmd, JSONObject content);
    }

    private static final class Ticket {
        final String cmd;
        final Listener listener;
        final long since;
        Ticket(String cmd, Listener listener) {
            this.cmd = cmd;
            this.listener = listener;
            this.since = System.currentTimeMillis();
        }
    }

    private static final java.util.ArrayList<Ticket> PENDINGS = new java.util.ArrayList<Ticket>();
    private static final Object LOCK = new Object();

    private static final long WAIT_TTL_MS = 12000L;

    public static String expect(String cmd, Listener l) {
        if (l == null) return null;
        Ticket t = new Ticket(cmd, l);
        synchronized (LOCK) {

            long now = System.currentTimeMillis();
            for (int i = PENDINGS.size() - 1; i >= 0; i--) {
                if (now - PENDINGS.get(i).since > WAIT_TTL_MS) PENDINGS.remove(i);
            }
            PENDINGS.add(t);
        }
        return String.valueOf(System.identityHashCode(t));
    }

    public static void cancel() {
        synchronized (LOCK) {
            PENDINGS.clear();
        }
    }

    public static void cancel(String ticketId) {
        if (ticketId == null) return;
        synchronized (LOCK) {
            for (int i = PENDINGS.size() - 1; i >= 0; i--) {
                if (ticketId.equals(String.valueOf(System.identityHashCode(PENDINGS.get(i))))) {
                    PENDINGS.remove(i);
                }
            }
        }
    }

    public static int waiting() {
        synchronized (LOCK) {
            return PENDINGS.size();
        }
    }

    private static Ticket takeTicket(String cmd) {
        synchronized (LOCK) {
            long now = System.currentTimeMillis();
            for (int i = 0; i < PENDINGS.size(); i++) {
                Ticket t = PENDINGS.get(i);
                if (now - t.since > WAIT_TTL_MS) {
                    PENDINGS.remove(i);
                    i--;
                    continue;
                }
                if (t.cmd == null || t.cmd.equals(cmd)) {
                    PENDINGS.remove(i);
                    return t;
                }
            }
            return null;
        }
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
            Class<?> h = com.cathelperawa.owo.app.core.dex.ClassResolver.find(
                    cl, "msf.respHandler", "mqq.app.msghandle.MsgRespHandler");
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

    private void onResp(Object[] args) {
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
            HookLog.e("onResp 异常", t);
        }
    }

    private static boolean isWanted(String cmd) {
        for (String c : CMDS) {
            if (c.equals(cmd)) return true;
        }

        synchronized (LOCK) {
            for (int i = 0; i < PENDINGS.size(); i++) {
                if (PENDINGS.get(i).cmd.equals(cmd)) return true;
            }
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

    private void handle(String cmd, byte[] buf) {
        try {

            if (cmd != null && cmd.contains("OlPushService.MsgPush")) {
                try {
                    FeatureRedPacket.notePushTime(System.currentTimeMillis());
                } catch (Throwable ignore) {
                }
            }

            Ticket t = takeTicket(cmd);
            if (t == null) {
                lastCmd = cmd;
                return;
            }

            FunProtoData d = new FunProtoData();
            d.fromBytes(FunProtoData.getUnpPackage(buf));
            JSONObject content = d.toJSON();
            lastCmd = cmd;
            lastJson = content.toString(2);

            MsgStore.add("【MSF响应】cmd=" + cmd + " len=" + buf.length
                    + " 等待中=" + waiting());

            t.listener.onResp(cmd, content);
        } catch (Throwable t) {
            HookLog.e("handle 失败 cmd=" + cmd, t);
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
            Object arr = o1.opt("2");
            return arr;
        } catch (Throwable t) {
            return null;
        }
    }
}
