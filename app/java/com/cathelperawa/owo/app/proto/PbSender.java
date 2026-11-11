package com.cathelperawa.owo.app.proto;

import com.cathelperawa.owo.app.core.dex.ClassResolver;
import com.cathelperawa.owo.app.core.Bg;
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

    public static int sendDirectWait(String contentJson, int chatType, String peerUin, int timeoutMs) {
        try {
            byte[] elems = Pb.elemBytes(contentJson);
            if (elems.length == 0) {
                HookLog.w("sendDirectWait: 元素为空");
                return -2;
            }
            byte[] pb = PbMsg.buildDirect(chatType, peerUin, elems, QQEnv.currentUin(), RESOLVER);
            HookLog.i("sendDirectWait PbSendMsg 长度=" + pb.length + " chatType=" + chatType
                    + " peer=" + peerUin + " elems=" + elems.length);
            dumpPb(pb, "guild_pb.hex");
            int rc = sendNativeAndWait(PbMsg.CMD_SEND_MSG, pb, timeoutMs);
            HookLog.i("sendDirectWait rc=" + rc + " ack=" + peekAck(PbMsg.CMD_SEND_MSG));
            return rc;
        } catch (Throwable t) {
            HookLog.e("sendDirectWait 失败", t);
            return -1;
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
            
            Bg.run(new Runnable() {
                @Override public void run() {
                    longThread(elems, ct, peer);
                }
            }, "cat-pb-long");
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

    

    private static boolean sendHooked = false;

    public static synchronized void setupSendDump() {
        if (sendHooked) return;
        try {
            ClassLoader cl = QQEnv.classLoader();
            Class<?> appIf = ClassResolver.find(cl, "app.interface",
                    "com.tencent.common.app.AppInterface");
            diag("setupSendDump: AppInterface=" + (appIf == null ? "null" : appIf.getName()));
            if (appIf == null) return;
            int n = 0;
            Class<?> c = appIf;
            while (c != null && c != Object.class) {
                for (final Method m : c.getDeclaredMethods()) {
                    if (!m.getName().toLowerCase().contains("sendtoservice")) continue;
                    if (m.getParameterTypes().length < 1) continue;
                    try {
                        XposedBridge.hookMethod(m, new XC_MethodHook() {
                            @Override protected void afterHookedMethod(MethodHookParam p) {
                                dumpLongReq(p.args);
                            }
                        });
                        n++;
                    } catch (Throwable ignore) {
                    }
                }
                c = c.getSuperclass();
            }
            sendHooked = n > 0;
            diag("sendToService hooked=" + n);
            HookLog.i("PbSender: sendToService 挂了 " + n + " 个");
            setupMsgHook();
        } catch (Throwable t) {
            HookLog.w("setupSendDump: " + t);
        }
    }

    
    private static int diagCmd = 0;
    static void diag(String msg) {
        try {
            android.content.Context ctx = QQEnv.appContext();
            if (ctx == null) return;
            java.io.File dir = new java.io.File(ctx.getFilesDir().getParent(), "files/cathelper");
            if (!dir.exists()) dir.mkdirs();
            java.io.File f = new java.io.File(dir, "send_diag.txt");
            if (f.length() > 512 * 1024) f.delete();
            java.io.FileOutputStream o = new java.io.FileOutputStream(f, true);
            try {
                String t = new java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US)
                        .format(new java.util.Date());
                o.write((t + " " + msg + "\n").getBytes("UTF-8"));
            } finally {
                try { o.close(); } catch (Throwable ignore) {}
            }
        } catch (Throwable ignore) {
        }
    }

    private static void dumpSendJson(byte[] body) {
        try {
            if (body == null || body.length == 0) return;
            String js = com.cathelperawa.owo.app.api.SendProtoApi.decodeBytes(body);
            if (js == null || js.length() == 0) return;
            android.content.Context ctx = QQEnv.appContext();
            if (ctx == null) return;
            java.io.File dir = new java.io.File(ctx.getFilesDir().getParent(), "files/cathelper");
            if (!dir.exists()) dir.mkdirs();
            java.io.File f = new java.io.File(dir, "send_dump.txt");
            if (f.length() > 2 * 1024 * 1024) f.delete();
            java.io.FileOutputStream o = new java.io.FileOutputStream(f, true);
            try {
                o.write(("===== " + System.currentTimeMillis()
                        + " len=" + body.length + " =====\n").getBytes("UTF-8"));
                o.write(js.getBytes("UTF-8"));
                o.write("\n\n".getBytes("UTF-8"));
            } finally {
                try { o.close(); } catch (Throwable ignore) {}
            }
        } catch (Throwable ignore) {
        }
    }

    private static boolean wupHooked = false;

    private static volatile String capturePrefix = null;
    private static volatile String capturedHex = null;
    private static volatile String capturedCmd = null;
    
    private static volatile String recvHex = null;
    private static volatile String recvCmd = null;

    private static volatile boolean captureAll = false;

    public static void startCaptureAll() {
        clearAll();
        captureAll = true;
        setupMsgHook();
        HookLog.i("全量抓包开始");
    }

    public static void stopCaptureAll() {
        captureAll = false;
        HookLog.i("全量抓包停止");
    }

    public static boolean captureAllArmed() {
        return captureAll;
    }

    private static synchronized void clearAll() {
        try {
            android.content.Context ctx = QQEnv.appContext();
            if (ctx == null) return;
            java.io.File dir = new java.io.File(ctx.getFilesDir().getParent(),
                    "files/cathelper");
            if (!dir.exists()) dir.mkdirs();
            java.io.File f = new java.io.File(dir, "capture_all.txt");
            if (f.exists()) f.delete();
        } catch (Throwable ignore) {
        }
    }

    private static volatile boolean arkCapInit = false;

    private static synchronized void appendArk(String cmd, byte[] buf, Object msg) {
        try {
            android.content.Context ctx = QQEnv.appContext();
            if (ctx == null) return;
            java.io.File dir = new java.io.File(ctx.getFilesDir().getParent(), "files/cathelper");
            if (!dir.exists()) dir.mkdirs();
            java.io.File f = new java.io.File(dir, "ark_capture.txt");
            if (!arkCapInit) {
                if (f.exists()) f.delete();
                arkCapInit = true;
            }
            if (f.length() > 3000000L) return;
            byte[] b = stripWup(buf);
            int n = Math.min(b.length, 12000);
            StringBuilder sb = new StringBuilder();
            sb.append("### ").append(cmd).append(" len=").append(b.length)
              .append(" t=").append(System.currentTimeMillis() / 1000).append("\n");
            for (int i = 0; i < n; i++) {
                int v = b[i] & 0xFF;
                if (v < 16) sb.append('0');
                sb.append(Integer.toHexString(v));
            }
            sb.append("\n");
            java.io.FileOutputStream fo = new java.io.FileOutputStream(f, true);
            fo.write(sb.toString().getBytes("UTF-8"));
            fo.flush();
            fo.close();
        } catch (Throwable ignore) {
        }
    }

    private static boolean isArkCmd(String cmd) {
        if (cmd == null) return false;
        return cmd.contains("0xb77") || cmd.contains("SsoArkSign")
                || cmd.contains("ArkShare") || cmd.contains("ArkSign");
    }

    private static synchronized void appendAll(String cmd, byte[] buf, Object msg) {
        try {
            android.content.Context ctx = QQEnv.appContext();
            if (ctx == null) return;
            java.io.File dir = new java.io.File(ctx.getFilesDir().getParent(),
                    "files/cathelper");
            if (!dir.exists()) dir.mkdirs();
            java.io.File f = new java.io.File(dir, "capture_all.txt");
            if (f.length() > 2000000L) return;   
            byte[] b = stripWup(buf);
            
            boolean full = cmd != null && (cmd.contains("PbSendMsg")
                    || cmd.contains("long_msg") || cmd.contains("MultiMsg")
                    || cmd.contains("0xb77") || cmd.contains("SsoArkSign"));
            int n = full ? Math.min(b.length, 12000) : Math.min(b.length, 400);
            StringBuilder sb = new StringBuilder();
            int st = -1;
            try {
                java.lang.reflect.Field sf = null;
                Class<?> c2 = msg.getClass();
                while (c2 != null && sf == null) {
                    try {
                        sf = c2.getDeclaredField("serviceType");
                    } catch (Throwable ignore) {
                        c2 = c2.getSuperclass();
                    }
                }
                if (sf != null) {
                    sf.setAccessible(true);
                    st = sf.getInt(msg);
                }
            } catch (Throwable ignore) {
            }
            sb.append("### ").append(cmd).append(" len=").append(b.length)
                    .append(" st=").append(st).append("\n");
            for (int i = 0; i < n; i++) {
                int v = b[i] & 0xFF;
                if (v < 16) sb.append('0');
                sb.append(Integer.toHexString(v));
            }
            sb.append("\n");
            java.io.FileOutputStream fo = new java.io.FileOutputStream(f, true);
            fo.write(sb.toString().getBytes("UTF-8"));
            fo.flush();
            fo.close();
        } catch (Throwable ignore) {
        }
    }

    public static void startCapture(String cmdPrefix) {
        recvHex = null;
        recvCmd = null;
        capturePrefix = cmdPrefix;
        capturedHex = null;
        capturedCmd = null;
        setupMsgHook();
        HookLog.i("抓包开始: " + cmdPrefix);
    }

    public static void stopCapture() {
        capturePrefix = null;
        HookLog.i("抓包停止");
    }

    public static boolean captureArmed() {
        return capturePrefix != null;
    }

    public static String[] peekRecv() {
        String h = recvHex;
        if (h == null) return null;
        return new String[]{recvCmd, h};
    }

    public static String[] peekCapture() {
        String h = capturedHex;
        if (h == null) return null;
        return new String[]{capturedCmd, h};
    }

    private static String hex(byte[] b) {
        if (b == null) return "";
        StringBuilder sb = new StringBuilder(b.length * 2);
        for (byte x : b) {
            int v = x & 0xFF;
            if (v < 16) sb.append('0');
            sb.append(Integer.toHexString(v));
        }
        return sb.toString();
    }

    private static void saveCapture(String cmd, String c, String h) {
        try {
            android.content.Context ctx = QQEnv.appContext();
            if (ctx == null) return;
            java.io.File dir = new java.io.File(ctx.getFilesDir().getParent(),
                    "files/cathelper");
            if (!dir.exists()) dir.mkdirs();
            java.io.File f = new java.io.File(dir, "oidb_capture.txt");
            java.io.FileOutputStream fo = new java.io.FileOutputStream(f, false);
            fo.write(("cmd=" + cmd + "\n" + h + "\n").getBytes("UTF-8"));
            fo.flush();
            fo.close();
        } catch (Throwable ignore) {
        }
    }

    public static synchronized void setupMsgHook() {
        if (wupHooked) return;
        try {
            ClassLoader cl = QQEnv.classLoader();
            Class<?> cls = ClassResolver.find(cl, "remote",
                    "com.tencent.qphone.base.remote.ToServiceMsg");
            if (cls == null) {
                HookLog.w("setupMsgHook: 没找到 ToServiceMsg");
                return;
            }
            int n = 0;
            Class<?> c = cls;
            while (c != null && c != Object.class) {
                for (final Method m : c.getDeclaredMethods()) {
                    if (!m.getName().toLowerCase().contains("wupbuffer")) continue;
                    if (m.getParameterTypes().length != 1) continue;
                    if (m.getParameterTypes()[0] != byte[].class) continue;
                    try {
                        XposedBridge.hookMethod(m, new XC_MethodHook() {
                            @Override protected void afterHookedMethod(MethodHookParam p) {
                                try {
                                    Object self = p.thisObject;
                                    Method gc = self.getClass()
                                            .getMethod("getServiceCmd");
                                    String cmd = (String) gc.invoke(self);
                                    if (cmd == null) return;
                                    Object a = p.args == null || p.args.length == 0
                                            ? null : p.args[0];
                                    if (!(a instanceof byte[])) return;
                                    
                                    if (isArkCmd(cmd)) {
                                        appendArk(cmd, (byte[]) a, self);
                                    }
                                    if (captureAll) {
                                        appendAll(cmd, (byte[]) a, self);
                                        return;
                                    }
                                    String cp = capturePrefix;
                                    if (cp != null && cmd.contains(cp)) {
                                        capturedCmd = cmd;
                                        capturedHex = hex((byte[]) a);
                                        HookLog.i("CAPTURE " + cmd + " "
                                                + (((byte[]) a).length) + "B");
                                        saveCapture(cmd, capturedCmd, capturedHex);
                                        return;
                                    }
                                    byte[] buf = stripWup((byte[]) a);
                                    if (cmd.contains("PbSendMsg")) {
                                        grabPicElem(buf);
                                        return;
                                    }
                                    
                                    if (cmd.contains("long_msg")) {
                                        HookLog.i("LONGDUMP2 " + cmd + " -> "
                                                + gunzipHex(buf));
                                    }
                                } catch (Throwable ignore) {
                                }
                            }
                        });
                        n++;
                    } catch (Throwable ignore) {
                    }
                }
                
                for (final Method bm : c.getDeclaredMethods()) {
                    if (!bm.getName().toLowerCase().contains("wupbuffer")) continue;
                    if (bm.getParameterTypes().length != 1) continue;
                    if (bm.getParameterTypes()[0] != byte[].class) continue;
                    try {
                        XposedBridge.hookMethod(bm, new XC_MethodHook() {
                            @Override protected void beforeHookedMethod(MethodHookParam p) {
                                try {
                                    Object self = p.thisObject;
                                    Method gc = self.getClass().getMethod("getServiceCmd");
                                    String cmd = (String) gc.invoke(self);
                                    if (cmd == null || !cmd.contains("PbSendMsg")) return;
                                    if (p.args == null || p.args.length == 0) return;
                                    Object a = p.args[0];
                                    if (!(a instanceof byte[])) return;
                                    byte[] np = com.cathelperawa.owo.app.feature.SharePatch
                                            .onSend(cmd, (byte[]) a);
                                    if (np != null) p.args[0] = np;
                                } catch (Throwable ignore) {
                                }
                            }
                        });
                    } catch (Throwable ignore) {
                    }
                }
                c = c.getSuperclass();
            }
            wupHooked = n > 0;
            HookLog.i("PbSender: putWupBuffer 挂了 " + n + " 个");
        } catch (Throwable t) {
            HookLog.w("setupMsgHook: " + t);
        }
    }

    public static volatile String lastPicElemJson = null;

    private static void dumpLongReq(Object[] args) {
        try {
            if (args == null) return;
            for (int i = 0; i < args.length; i++) {
                Object a = args[i];
                if (a == null) continue;
                if (!a.getClass().getName().contains("ToServiceMsg")) continue;
                Method gc = a.getClass().getMethod("getServiceCmd");
                String cmd = (String) gc.invoke(a);
                if (cmd == null) continue;
                if (diagCmd < 200 && (cmd.contains("Send") || cmd.contains("send"))) {
                    diagCmd++;
                    diag("OUT " + cmd);
                }
                
                if (cmd.equals(PbMsg.CMD_SEND_MSG)) {
                    try {
                        Method gb2 = a.getClass().getMethod("getWupBuffer");
                        byte[] b2 = (byte[]) gb2.invoke(a);
                        byte[] body2 = stripWup(b2);
                        grabPicElem(body2);
                        
                        dumpSendJson(body2);
                    } catch (Throwable ignore) {
                    }
                    continue;
                }
                if (!cmd.contains("long_msg")) continue;
                Method gb = a.getClass().getMethod("getWupBuffer");
                byte[] buf = (byte[]) gb.invoke(a);
                HookLog.i("LONGDUMP " + cmd + " -> " + gunzipHex(stripWup(buf)));
            }
        } catch (Throwable ignore) {
        }
    }

    private static void grabPicElem(byte[] body) {
        try {
            if (body == null || body.length < 60) return;
            FunProtoData rd = new FunProtoData();
            rd.fromBytes(body);
            org.json.JSONObject ro = rd.toJSON();
            org.json.JSONObject body3 = ro.optJSONObject("3");
            if (body3 == null) return;
            org.json.JSONObject rich = body3.optJSONObject("1");
            if (rich == null) return;
            org.json.JSONArray elems = rich.optJSONArray("2");
            if (elems == null) return;
            for (int i = 0; i < elems.length(); i++) {
                Object e = elems.opt(i);
                if (e == null) continue;
                if (!(e instanceof org.json.JSONObject)) continue;
                org.json.JSONObject eo = (org.json.JSONObject) e;

                if (!(eo.has("2") || eo.has("53"))) continue;
                String js = eo.toString();
                lastPicElemJson = js;
                HookLog.i("抓图元素: " + js.length() + " 字节");
                try {
                    MediaVault.add(QQEnv.appContext(), lastPicElemJson, "刚发的图");
                } catch (Throwable ignore) {
                }
                return;
            }
        } catch (Throwable t) {
            HookLog.w("grabPicElem: " + t);
        }
    }

    private static String gunzipHex(byte[] b) {
        try {
            if (b == null || b.length < 12) return "null";
            int idx = -1;
            for (int i = 0; i < b.length - 1; i++) {
                if ((b[i] & 0xFF) == 0x1F && (b[i + 1] & 0xFF) == 0x8B) {
                    idx = i;
                    break;
                }
            }
            if (idx < 0) return "nogz len=" + b.length;
            java.util.zip.GZIPInputStream in = new java.util.zip.GZIPInputStream(
                    new java.io.ByteArrayInputStream(b, idx, b.length - idx));
            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
            byte[] tmp = new byte[4096];
            int n;
            while ((n = in.read(tmp)) > 0) bos.write(tmp, 0, n);
            in.close();
            byte[] raw = bos.toByteArray();
            StringBuilder sb = new StringBuilder("raw=" + raw.length + " hex=");
            int max = Math.min(raw.length, 900);
            for (int i = 0; i < max; i++) {
                sb.append(HEX[(raw[i] >> 4) & 0xF]).append(HEX[raw[i] & 0xF]);
            }
            return sb.toString();
        } catch (Throwable t) {
            return "unzip fail " + t;
        }
    }

    private static final char[] HEX = "0123456789abcdef".toCharArray();

    public static String uploadMultiMsg(byte[] multiPb, int chatType, String peerUin) {
        synchronized (SEND_LOCK) {
            try {
                if (multiPb == null || multiPb.length == 0) return "ERR:MultiMsg 为空";
                setupRecvHook();
                setupSendDump();
                setupMsgHook();
                byte[] gz = Pb.gzip(multiPb);
                byte[] req = PbMsg.buildUploadReq(chatType, peerUin, gz, RESOLVER);
                resIdBox.set(null);
                boolean sent = sendCmd(CMD_UPLOAD, req);
                HookLog.i("伪记录上传已发 ok=" + sent + " len=" + req.length
                        + " gz=" + gz.length);
                if (!sent) return "ERR:sendCmd 返回 false";
                long deadline = System.currentTimeMillis() + 25000;
                while (System.currentTimeMillis() < deadline) {
                    Object v = resIdBox.get();
                    if (v != null) return String.valueOf(v);
                    try {
                        Thread.sleep(100);
                    } catch (Throwable ignore) {
                    }
                }
                return null;
            } catch (Throwable t) {
                return "ERR:" + t;
            }
        }
    }

    public static String probeUpload(int chatType, String peerUin, String text) {
        synchronized (SEND_LOCK) {
            try {
                setupRecvHook();
                String json = "{\"1\":{\"1\":" + org.json.JSONObject.quote(
                        text == null ? "probe" : text) + "}}";
                byte[] elems = Pb.elemBytes(json);
                if (elems.length == 0) return "ERR:元素为空";
                byte[] multi = PbMsg.buildMultiMsg(elems);
                byte[] gz = Pb.gzip(multi);
                byte[] req = PbMsg.buildUploadReq(chatType, peerUin, gz, RESOLVER);
                resIdBox.set(null);
                boolean sent = sendCmd(CMD_UPLOAD, req);
                HookLog.i("探针: 上传已发 ok=" + sent + " len=" + req.length
                        + " gz=" + gz.length + " cmd=" + CMD_UPLOAD);
                long deadline = System.currentTimeMillis() + 20000;
                while (System.currentTimeMillis() < deadline) {
                    Object v = resIdBox.get();
                    if (v != null) return String.valueOf(v);
                    try {
                        Thread.sleep(100);
                    } catch (Throwable ignore) {
                    }
                }
                return null;
            } catch (Throwable t) {
                return "ERR:" + t;
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
            Class<?> appIf = ClassResolver.find(cl, "app.interface",
                    "com.tencent.common.app.AppInterface");
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

    private static byte[] stripWup(byte[] b) {
        try {
            if (b == null || b.length < 5) return b;
            long len = ((long) (b[0] & 0xFF) << 24) | ((b[1] & 0xFF) << 16)
                    | ((b[2] & 0xFF) << 8) | (b[3] & 0xFF);
            if (len == b.length - 4) {
                byte[] r = new byte[b.length - 4];
                System.arraycopy(b, 4, r, 0, r.length);
                return r;
            }
        } catch (Throwable ignore) {
        }
        return b;
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

    private static final java.util.concurrent.ConcurrentHashMap<String, byte[]> RESP =
            new java.util.concurrent.ConcurrentHashMap<String, byte[]>();

    public static byte[] sendAndGetResp(String cmd, byte[] body, int timeoutMs) {
        return sendAndGetResp(cmd, body, 0, timeoutMs);
    }

    public static byte[] sendAndGetResp(String cmd, byte[] body, int serviceType,
                                        int timeoutMs) {
        try {
            setupRecvHook();
            ACK.remove(cmd);
            RESP.remove(cmd);
            if (serviceType > 0) {
                QQInterfaces.update(QQEnv.classLoader());
                QQInterfaces.sendBuffer(cmd, true, body, serviceType);
            } else {
                sendCmd(cmd, body);
            }
            long deadline = System.currentTimeMillis() + Math.max(1000, timeoutMs);
            while (System.currentTimeMillis() < deadline) {
                byte[] r = RESP.get(cmd);
                if (r != null) return r;
                try {
                    Thread.sleep(80);
                } catch (Throwable ignore) {
                }
            }
            return null;
        } catch (Throwable t) {
            HookLog.e("sendAndGetResp 失败", t);
            return null;
        }
    }

    private static final java.util.concurrent.ConcurrentHashMap<String, Integer> ACK =
            new java.util.concurrent.ConcurrentHashMap<String, Integer>();

    private static void recordAck(String cmd, Object from) {
        try {
            if (cmd == null) return;
            Method getBuf = from.getClass().getMethod("getWupBuffer");
            getBuf.setAccessible(true);
            byte[] buf = (byte[]) getBuf.invoke(from);
            if (buf == null || buf.length == 0) return;
            buf = stripWup(buf);
            FunProtoData rd = new FunProtoData();
            rd.fromBytes(buf);
            JSONObject o = rd.toJSON();
            int code = -1;

            boolean isSend = cmd != null && cmd.contains("PbSendMsg");
            Object pick = o.opt(isSend ? "1" : "3");
            if (pick instanceof Number) {
                code = ((Number) pick).intValue();
            } else if (pick instanceof String) {
                try {
                    code = Integer.parseInt((String) pick);
                } catch (Throwable ignore) {
                }
            }
            if (isSend) {
                try {
                    HookLog.i("PbSendMsg 回包 JSON: " + o.toString());
                    dumpPb(buf, "send_resp.hex");
                } catch (Throwable ignore) {
                }
            }
            ACK.put(cmd, code);
            RESP.put(cmd, buf);   
            HookLog.i("回包 " + cmd + " retcode=" + code + " len=" + buf.length);
        } catch (Throwable ignore) {
        }
    }

    public static int sendOidbAndWait(String cmd, byte[] body, int timeoutMs) {
        try {
            setupRecvHook();
            ACK.remove(cmd);
            sendCmd(cmd, body);
            long deadline = System.currentTimeMillis() + Math.max(500, timeoutMs);
            while (System.currentTimeMillis() < deadline) {
                Integer v = ACK.get(cmd);
                if (v != null) return v;
                try {
                    Thread.sleep(80);
                } catch (Throwable ignore) {
                }
            }
            return -999;
        } catch (Throwable t) {
            HookLog.e("sendOidbAndWait 失败", t);
            return -1;
        }
    }

    public static int sendNativeAndWait(String cmd, byte[] pb, int timeoutMs) {
        try {
            setupRecvHook();
            ACK.remove(cmd);
            RESP.remove(cmd);
            boolean ok = QQInterfaces.sendBuffer(cmd, true, pb);
            if (!ok) return -2;
            long deadline = System.currentTimeMillis() + Math.max(800, timeoutMs);
            while (System.currentTimeMillis() < deadline) {
                Integer v = ACK.get(cmd);
                if (v != null) return v;
                try {
                    Thread.sleep(80);
                } catch (Throwable ignore) {
                }
            }
            return -999;
        } catch (Throwable t) {
            HookLog.e("sendNativeAndWait 失败", t);
            return -1;
        }
    }

    private static void dumpPb(byte[] pb, String name) {
        try {
            android.content.Context ctx = QQEnv.appContext();
            if (ctx == null || pb == null) return;
            java.io.File dir = new java.io.File(ctx.getFilesDir().getParent(), "files/cathelper");
            if (!dir.exists()) dir.mkdirs();
            java.io.File f = new java.io.File(dir, name);
            StringBuilder sb = new StringBuilder();
            sb.append("# len=").append(pb.length).append("\n");
            for (byte x : pb) sb.append(String.format("%02x", x & 0xFF));
            sb.append("\n");
            java.io.FileOutputStream fo = new java.io.FileOutputStream(f, false);
            fo.write(sb.toString().getBytes("UTF-8"));
            fo.flush(); fo.close();
        } catch (Throwable ignore) {
        }
    }

    public static Integer peekAck(String cmd) {
        return cmd == null ? null : ACK.get(cmd);
    }

    public static byte[] peekResp(String cmd) {
        return cmd == null ? null : RESP.get(cmd);
    }

    private static void onRecv(Object[] args) {
        try {
            if (args == null || args.length < 2) return;
            Object from = args[1];
            if (from == null) return;
            Method getCmd = from.getClass().getMethod("getServiceCmd");
            getCmd.setAccessible(true);
            String cmd = (String) getCmd.invoke(from);
            
            recordAck(cmd, from);
            
            try {
                String cp = capturePrefix;
                if (cp != null && cmd != null && cmd.contains(cp)) {
                    Method gb0 = from.getClass().getMethod("getWupBuffer");
                    gb0.setAccessible(true);
                    byte[] rb0 = (byte[]) gb0.invoke(from);
                    if (rb0 != null && rb0.length > 0) {
                        recvCmd = cmd;
                        recvHex = hex(rb0);
                        HookLog.i("RECV " + cmd + " " + rb0.length + "B");
                    }
                }
            } catch (Throwable ignore) {
            }
            
            try {
                if (isArkCmd(cmd)) {
                    Method gb = from.getClass().getMethod("getWupBuffer");
                    gb.setAccessible(true);
                    byte[] rb = (byte[]) gb.invoke(from);
                    if (rb != null && rb.length > 0) {
                        appendArk("<<RECV>> " + cmd, rb, from);
                    }
                }
            } catch (Throwable ignore) {
            }
            if (captureAll) {
                try {
                    Method gb = from.getClass().getMethod("getWupBuffer");
                    gb.setAccessible(true);
                    byte[] rb = (byte[]) gb.invoke(from);
                    if (rb != null && rb.length > 0) {
                        appendAll("<<RECV>> " + cmd, rb, from);
                    }
                } catch (Throwable ignore) {
                }
            }
            if (!CMD_UPLOAD.equals(cmd)) return;

            Method getBuf = from.getClass().getMethod("getWupBuffer");
            getBuf.setAccessible(true);
            byte[] buf = (byte[]) getBuf.invoke(from);
            if (buf == null || buf.length == 0) return;
            buf = stripWup(buf);   

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
