package com.cathelperawa.owo.app.feature;

import com.cathelperawa.owo.app.bridge.QQEnv;
import com.cathelperawa.owo.app.core.HookLog;
import com.cathelperawa.owo.app.proto.PbSender;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class CardForge {

    private CardForge() {
    }

    private static final String CMD = "OidbSvcTrpcTcp.0x11ca_0";

    private static final int SERVICE_TYPE = 4790;

    private static final String CLS_REQ =
            "tencent.im.oidb.cmd0x11b2.oidb_0x11b2$BusinessCardV3Req";
    private static final String CLS_RSP =
            "tencent.im.oidb.cmd0x11b2.oidb_0x11b2$BusinessCardV3Rsp";

    public static final class Result {
        public final String ark;
        public final String error;

        Result(String ark, String error) {
            this.ark = ark;
            this.error = error;
        }

        public boolean ok() {
            return ark != null && ark.length() > 0;
        }
    }

    public static Result forge(long uin, String jumpUrl) {
        try {
            if (uin <= 0) return new Result(null, "uin 不合法");
            ClassLoader cl = QQEnv.classLoader();

            Class<?> reqCls = Class.forName(CLS_REQ, true, cl);
            Class<?> rspCls = Class.forName(CLS_RSP, true, cl);

            Object req = reqCls.newInstance();
            setField(req, reqCls, "uin", uin);

            String jump = (jumpUrl == null || jumpUrl.trim().length() == 0)
                    ? "mqqapi://card/show_pslcard?src_type=internal&source=sharecard"
                      + "&version=1&uin=" + uin
                    : jumpUrl.trim();
            setField(req, reqCls, "jump_url", jump);

            Method toBytes = null;
            Class<?> c = reqCls;
            while (c != null && toBytes == null) {
                try {
                    toBytes = c.getMethod("toByteArray");
                } catch (Throwable ignore) {
                    c = c.getSuperclass();
                }
            }
            if (toBytes == null) return new Result(null, "找不到 toByteArray");
            byte[] inner = (byte[]) toBytes.invoke(req);
            if (inner == null || inner.length == 0) return new Result(null, "请求体为空");

            java.io.ByteArrayOutputStream oidb = new java.io.ByteArrayOutputStream();
            writeVarint(oidb, (1 << 3) | 0);
            writeVarint(oidb, SERVICE_TYPE);
            writeVarint(oidb, (2 << 3) | 0);
            writeVarint(oidb, 0);
            writeVarint(oidb, (4 << 3) | 2);
            writeVarint(oidb, inner.length);
            oidb.write(inner);
            byte[] wrapped = oidb.toByteArray();

            byte[] body = wrapped;
            HookLog.i("CardForge: 请求 " + body.length + "B uin=" + uin);

            byte[] resp = PbSender.sendAndGetResp(CMD, body, SERVICE_TYPE, 8000);
            if (resp == null) return new Result(null, "没收到回包（8 秒）");

            Method merge = null;
            c = rspCls;
            while (c != null && merge == null) {
                try {
                    merge = c.getMethod("mergeFrom", byte[].class);
                } catch (Throwable ignore) {
                    c = c.getSuperclass();
                }
            }
            if (merge == null) return new Result(null, "找不到 mergeFrom");

            String ssoErr = ssoError(resp);
            if (ssoErr != null) return new Result(null, ssoErr);

            java.util.List<byte[]> candidates = new java.util.ArrayList<byte[]>();
            candidates.add(resp);
            collectBodies(resp, candidates);

            dumpHex("forge_resp.hex", resp);

            String ark = null;
            String lastErr = null;
            for (byte[] cand : candidates) {
                try {
                    Object rsp = rspCls.newInstance();
                    merge.invoke(rsp, (Object) cand);
                    String v = getPbString(rsp, rspCls, "signed_ark_msg");
                    if (v != null && v.length() > 0) {
                        ark = v;
                        break;
                    }

                    v = scanStrings(rsp, rspCls);
                    if (v != null && v.length() > 40 && v.contains("\"app\"")) {
                        ark = v;
                        break;
                    }
                } catch (Throwable t) {
                    lastErr = String.valueOf(t);
                }
            }
            if (ark == null) {
                return new Result(null, "服务端没给 signed_ark_msg（回包 "
                        + resp.length + "B，试了 " + candidates.size() + " 层"
                        + (lastErr == null ? "" : "，" + lastErr) + "）");
            }
            if (ark == null || ark.length() == 0) {
                return new Result(null, "服务端没给 signed_ark_msg（回包 "
                        + resp.length + "B）");
            }
            HookLog.i("CardForge: 拿到签名卡 " + ark.length() + " 字符");
            return new Result(ark, null);
        } catch (Throwable t) {
            HookLog.e("CardForge.forge 失败", t);
            return new Result(null, String.valueOf(t));
        }
    }

    private static String ssoError(byte[] buf) {
        try {
            int i = 0;
            int code = -1;
            String msg = null;
            while (i < buf.length) {
                long tag = 0;
                int shift = 0;
                while (i < buf.length) {
                    int b = buf[i++] & 0xFF;
                    tag |= ((long) (b & 0x7F)) << shift;
                    if ((b & 0x80) == 0) break;
                    shift += 7;
                }
                int f = (int) (tag >>> 3);
                int w = (int) (tag & 7);
                if (w == 0) {
                    long v = 0;
                    shift = 0;
                    while (i < buf.length) {
                        int b = buf[i++] & 0xFF;
                        v |= ((long) (b & 0x7F)) << shift;
                        if ((b & 0x80) == 0) break;
                        shift += 7;
                    }
                    if (f == 3) code = (int) v;
                } else if (w == 2) {
                    long len = 0;
                    shift = 0;
                    while (i < buf.length) {
                        int b = buf[i++] & 0xFF;
                        len |= ((long) (b & 0x7F)) << shift;
                        if ((b & 0x80) == 0) break;
                        shift += 7;
                    }
                    if (i + len > buf.length) break;
                    if (f == 5) {
                        msg = new String(buf, i, (int) len, "UTF-8");
                    }
                    i += (int) len;
                } else if (w == 5) {
                    i += 4;
                } else if (w == 1) {
                    i += 8;
                } else {
                    break;
                }
            }
            if (code > 0) {
                String t = msg == null ? "" : msg.trim();
                String human = "";
                if (t.contains("uins are unregistered")) {

                    human = "\n→ 这个号在 Ark 平台查不到资料。换 10001 或你自己的号再试。";
                }
                return "服务端返回 " + code + (t.length() > 0 ? "：" + t : "") + human;
            }
            return null;
        } catch (Throwable t) {
            return null;
        }
    }

    private static void writeVarint(java.io.ByteArrayOutputStream b, long v) {
        while (true) {
            if ((v & ~0x7FL) == 0) {
                b.write((int) v);
                return;
            }
            b.write((int) ((v & 0x7F) | 0x80));
            v >>>= 7;
        }
    }

    private static void collectBodies(byte[] buf, java.util.List<byte[]> out) {
        collectBodies(buf, out, 0);
    }

    private static void collectBodies(byte[] buf, java.util.List<byte[]> out, int depth) {
        if (depth > 2 || buf == null || buf.length == 0) return;
        try {
            int i = 0;
            while (i < buf.length) {
                long tag = 0;
                int shift = 0;
                while (true) {
                    if (i >= buf.length) return;
                    int b = buf[i++] & 0xFF;
                    tag |= ((long) (b & 0x7F)) << shift;
                    if ((b & 0x80) == 0) break;
                    shift += 7;
                    if (shift > 63) return;
                }
                int f = (int) (tag >>> 3);
                int w = (int) (tag & 7);
                if (w == 0) {
                    while (i < buf.length && (buf[i] & 0x80) != 0) i++;
                    i++;
                } else if (w == 2) {
                    long len = 0;
                    shift = 0;
                    while (true) {
                        if (i >= buf.length) return;
                        int b = buf[i++] & 0xFF;
                        len |= ((long) (b & 0x7F)) << shift;
                        if ((b & 0x80) == 0) break;
                        shift += 7;
                        if (shift > 63) return;
                    }
                    if (len < 0 || i + len > buf.length) return;
                    byte[] sub = new byte[(int) len];
                    System.arraycopy(buf, i, sub, 0, (int) len);
                    i += (int) len;
                    out.add(sub);
                    collectBodies(sub, out, depth + 1);
                } else if (w == 5) {
                    i += 4;
                } else if (w == 1) {
                    i += 8;
                } else {
                    return;
                }
            }
        } catch (Throwable ignore) {
        }
    }

    private static String scanStrings(Object msg, Class<?> cls) {
        Class<?> c = cls;
        while (c != null && c != Object.class) {
            for (Field f : c.getDeclaredFields()) {
                try {
                    f.setAccessible(true);
                    Object holder = f.get(msg);
                    if (holder == null) continue;
                    Method get = holder.getClass().getMethod("get");
                    get.setAccessible(true);
                    Object v = get.invoke(holder);
                    if (v instanceof String) {
                        String sv = (String) v;
                        if (sv.length() > 40 && sv.contains("\"app\"")) return sv;
                    }
                } catch (Throwable ignore) {
                }
            }
            c = c.getSuperclass();
        }
        return null;
    }

    private static void dumpHex(String fn, byte[] b) {
        try {
            android.content.Context ctx = QQEnv.appContext();
            if (ctx == null) return;
            java.io.File dir = new java.io.File(ctx.getFilesDir().getParent(),
                    "files/cathelper");
            if (!dir.exists()) dir.mkdirs();
            StringBuilder sb = new StringBuilder();
            for (byte x : b) {
                int v = x & 0xFF;
                if (v < 16) sb.append('0');
                sb.append(Integer.toHexString(v));
            }
            java.io.FileOutputStream fo =
                    new java.io.FileOutputStream(new java.io.File(dir, fn), false);
            fo.write(sb.toString().getBytes("UTF-8"));
            fo.flush();
            fo.close();
        } catch (Throwable ignore) {
        }
    }

    private static void setField(Object msg, Class<?> cls, String name, Object value) {
        try {
            Field f = findField(cls, name);
            if (f == null) {
                HookLog.w("CardForge: 没有字段 " + name);
                return;
            }
            f.setAccessible(true);
            Object holder = f.get(msg);
            if (holder == null) return;
            Class<?> vc = value.getClass();
            Method set = null;
            Class<?> pt = null;
            for (Method m : holder.getClass().getMethods()) {
                if (!m.getName().equals("set") || m.getParameterTypes().length != 1) continue;
                Class<?> p = m.getParameterTypes()[0];
                if (p.isAssignableFrom(vc) || (p == int.class && vc == Long.class)
                        || (p == long.class && vc == Long.class)
                        || (p == String.class && vc != String.class)) {
                    set = m;
                    pt = p;
                    break;
                }
            }
            if (set == null) {
                HookLog.w("CardForge: " + name + " 找不到 set(" + vc.getSimpleName() + ")");
                return;
            }
            set.setAccessible(true);

            Object arg = value;
            if (pt == int.class && value instanceof Long) arg = Integer.valueOf(((Long) value).intValue());
            else if (pt == long.class && value instanceof Integer) arg = Long.valueOf(((Integer) value).longValue());
            else if (pt == String.class) arg = String.valueOf(value);
            try {
                set.invoke(holder, arg);
            } catch (Throwable t) {
                HookLog.w("CardForge.setField(" + name + ") invoke 失败 pt=" + pt + " v=" + value + ": " + t);
            }
        } catch (Throwable t) {
            HookLog.w("CardForge.setField(" + name + "): " + t);
        }
    }

    private static String getPbString(Object msg, Class<?> cls, String name) {
        try {
            Field f = findField(cls, name);
            if (f == null) return null;
            f.setAccessible(true);
            Object holder = f.get(msg);
            if (holder == null) return null;
            Method get = holder.getClass().getMethod("get");
            get.setAccessible(true);
            Object v = get.invoke(holder);
            return v == null ? null : String.valueOf(v);
        } catch (Throwable t) {
            HookLog.w("CardForge.getPbString(" + name + "): " + t);
            return null;
        }
    }

    private static Field findField(Class<?> cls, String name) {
        Class<?> c = cls;
        while (c != null && c != Object.class) {
            try {
                return c.getDeclaredField(name);
            } catch (Throwable ignore) {
                c = c.getSuperclass();
            }
        }
        return null;
    }
}
