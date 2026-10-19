package com.cathelperawa.owo.app.proto;

import com.cathelperawa.owo.app.bridge.QQEnv;
import com.cathelperawa.owo.app.bridge.QQUtils;
import com.cathelperawa.owo.app.core.HookLog;
import com.cathelperawa.owo.app.ovo.FunProtoData;
import com.cathelperawa.owo.app.ovo.QQInterfaces;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicReference;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

public final class PbSender {

    private PbSender() {}

    private static final String CMD_UPLOAD = PbMsg.CMD_UPLOAD_LONG;

    private static final AtomicReference<String> resIdBox = new AtomicReference<String>();
    private static final Object SEND_LOCK = new Object();
    private static volatile boolean recvHooked = false;

    private static final PbMsg.UidResolver RESOLVER = new PbMsg.UidResolver() {
        @Override public String uidFromUin(String uin) {
            try {
                return QQUtils.uidFromUin(uin);
            } catch (Throwable t) {
                return null;
            }
        }
    };

    public static boolean sendCmd(String cmd, byte[] body) {
        try {
            QQInterfaces.update(QQEnv.classLoader());
            QQInterfaces.sendBuffer(cmd, true, body);
            HookLog.i("已发报文 " + cmd + " len=" + (body == null ? 0 : body.length));
            return true;
        } catch (Throwable t) {
            HookLog.e("sendCmd 失败: " + cmd, t);
            return false;
        }
    }

    public static boolean sendDirect(String contentJson, int chatType, String peerUin) {
        try {
            byte[] elems = Pb.elemBytes(contentJson);
            if (elems.length == 0) {
                HookLog.w("sendDirect: 元素为空");
                return false;
            }
            byte[] pb = PbMsg.buildDirect(chatType, peerUin, elems, QQEnv.currentUin(), RESOLVER);
            HookLog.i("礼物直发 PbSendMsg 长度=" + pb.length);
            return sendCmd(PbMsg.CMD_SEND_MSG, pb);
        } catch (Throwable t) {
            HookLog.e("sendDirect 失败", t);
            return false;
        }
    }

    public static boolean sendLong(String contentJson, int chatType, String peerUin) {
        try {
            final byte[] elems = Pb.elemBytes(contentJson);
            if (elems.length == 0) {
                HookLog.w("sendLong: 元素为空");
                return false;
            }
            final int ct = chatType;
            final String peer = peerUin;

            new Thread(new Runnable() {
                @Override public void run() {
                    longThread(elems, ct, peer);
                }
            }, "cat-pb-long").start();
            return true;
        } catch (Throwable t) {
            HookLog.e("sendLong 失败", t);
            return false;
        }
    }

    private static void longThread(byte[] elems, int chatType, String peerUin) {
        synchronized (SEND_LOCK) {
            try {
                setupRecvHook();
                byte[] multi = PbMsg.buildMultiMsg(elems);
                byte[] gz = Pb.gzip(multi);
                byte[] req = PbMsg.buildUploadReq(chatType, peerUin, gz, RESOLVER);

                resIdBox.set(null);
                sendCmd(CMD_UPLOAD, req);
                HookLog.i("长消息上传已发 len=" + req.length + " (gz=" + gz.length + ")");

                long deadline = System.currentTimeMillis() + 30000;
                String rid = null;
                while (rid == null && System.currentTimeMillis() < deadline) {
                    Object v = resIdBox.get();
                    if (v != null) { rid = String.valueOf(v); break; }
                    try { Thread.sleep(100); } catch (Throwable ignore) {}
                }

                if (rid == null) {
                    HookLog.w("长消息上传超时，没拿到 resId");
                    return;
                }

                byte[] ref = PbMsg.buildRef(chatType, peerUin, rid, RESOLVER);
                HookLog.i("长消息第二步 PbSendMsg resId=" + rid + " len=" + ref.length);
                sendCmd(PbMsg.CMD_SEND_MSG, ref);
            } catch (Throwable t) {
                HookLog.e("longThread 失败", t);
            }
        }
    }

    public static String lastResId() {
        return resIdBox.get();
    }

    public static String route(String contentJson, int chatType, String peerUin) {
        try {
            String c = contentJson == null ? "" : contentJson.trim();
            if (c.length() == 0) return null;

            if (c.startsWith("[")) new JSONArray(c);
            else new JSONObject(c);

            if (isArk(c)) {
                com.cathelperawa.owo.app.api.CatApi.sendCard(chatType, peerUin, c);
                return "card";
            }
            if (isGift(c)) {
                return sendDirect(c, chatType, peerUin) ? "direct" : null;
            }
            return sendLong(c, chatType, peerUin) ? "long" : null;
        } catch (Throwable t) {
            HookLog.e("route 失败", t);
            return null;
        }
    }

    public static boolean isArk(String c) {
        try {
            JSONObject o = new JSONObject(c);
            return o.has("app") && o.has("view");
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean isGift(String c) {
        try {
            JSONObject o = new JSONObject(c.trim());
            if (!o.has("53")) return false;
            return o.getJSONObject("53").optInt("1", -1) == 41;
        } catch (Throwable t) {
            return false;
        }
    }

    public static synchronized void setupRecvHook() {
        if (recvHooked) return;
        try {
            ClassLoader cl = QQEnv.classLoader();
            Class<?> appIf = Class.forName("com.tencent.common.app.AppInterface", true, cl);
            final Method recv = findMethod(appIf, "receiveToService");
            if (recv == null) {
                HookLog.w("PbSender: 未找到 receiveToService");
                return;
            }
            XposedBridge.hookMethod(recv, new XC_MethodHook() {
                @Override protected void afterHookedMethod(MethodHookParam param) {
                    onRecv(param.args);
                }
            });
            recvHooked = true;
            HookLog.i("PbSender: receiveToService 已挂（等 resId）");
        } catch (Throwable t) {
            HookLog.e("setupRecvHook 失败", t);
        }
    }

    private static Method findMethod(Class<?> cls, String name) {
        Class<?> c = cls;
        while (c != null && c != Object.class) {
            for (Method m : c.getDeclaredMethods()) {
                if (m.getName().equals(name) && m.getParameterTypes().length == 2) return m;
            }
            c = c.getSuperclass();
        }
        return null;
    }

    private static void onRecv(Object[] args) {
        try {
            if (args == null || args.length < 2) return;
            Object from = args[1];
            if (from == null) return;
            Method getCmd = from.getClass().getMethod("getServiceCmd");
            getCmd.setAccessible(true);
            String cmd = (String) getCmd.invoke(from);
            if (!CMD_UPLOAD.equals(cmd)) return;

            Method getBuf = from.getClass().getMethod("getWupBuffer");
            getBuf.setAccessible(true);
            byte[] buf = (byte[]) getBuf.invoke(from);
            if (buf == null || buf.length == 0) return;

            FunProtoData rd = new FunProtoData();
            rd.fromBytes(buf);
            JSONObject ro = rd.toJSON();
            String rid = pickResId(ro);
            if (rid != null && rid.length() > 0) {
                resIdBox.set(rid);
                HookLog.i("长消息 resId=" + rid);
            }
        } catch (Throwable t) {
            HookLog.w("PbSender 回包解析: " + t);
        }
    }

    private static String pickResId(Object node) {
        try {
            if (node instanceof JSONObject) {
                JSONObject o = (JSONObject) node;
                if (o.has("2") && o.opt("2") instanceof JSONObject) {
                    JSONObject n2 = o.getJSONObject("2");
                    if (n2.has("3")) {
                        Object v = n2.get("3");
                        if (v instanceof String && ((String) v).length() > 0) return (String) v;
                    }
                }
                for (java.util.Iterator<String> it = o.keys(); it.hasNext(); ) {
                    String k = it.next();
                    String r = pickResId(o.opt(k));
                    if (r != null && r.length() > 0) return r;
                }
            } else if (node instanceof JSONArray) {
                JSONArray a = (JSONArray) node;
                for (int i = 0; i < a.length(); i++) {
                    String r = pickResId(a.opt(i));
                    if (r != null && r.length() > 0) return r;
                }
            } else if (node instanceof String) {
                String s = (String) node;
                if (s.matches("[A-Za-z0-9_\\-]{16,}")) return s;
            }
        } catch (Throwable ignore) {
        }
        return null;
    }
}
