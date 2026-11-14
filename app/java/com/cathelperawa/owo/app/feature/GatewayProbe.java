package com.cathelperawa.owo.app.feature;

import com.cathelperawa.owo.app.MsgStore;
import com.cathelperawa.owo.app.core.HookApi;
import com.cathelperawa.owo.app.core.HookLog;

import java.lang.reflect.Method;

public final class GatewayProbe {

    private GatewayProbe() {}

    private static final char[] HEXC = "0123456789abcdef".toCharArray();

    private static void installEncryptProbe(ClassLoader cl) {
        try {
            Class<?> c = HookApi.findClass(cl, "com.tenpay.sdk.basebl.EncryptRequest");
            if (c == null) { HookLog.w("EncryptProbe: 找不到 EncryptRequest"); return; }
            int n = 0;
            for (final Method m : c.getDeclaredMethods()) {
                if (!m.getName().equals("encypt")) continue;
                if (m.getParameterTypes().length != 6) continue;
                HookApi.before(m, new HookApi.Action() {
                    @Override public void call(
                            de.robv.android.xposed.XC_MethodHook.MethodHookParam p) {
                        try {
                            Object[] a = p.args;
                            String salt = (a[1] == null) ? null : String.valueOf(a[1]);
                            String form = (a[3] == null) ? null : String.valueOf(a[3]);
                            if (form != null && (form.contains("hb_pre_grap")
                                    || form.contains("listid"))) {
                                HookLog.i("EncryptProbe ENC salt=" + salt + " rnd=" + a[2]
                                        + " form=" + form);
                                MsgStore.add("ENCFORM salt=" + salt + " rnd=" + a[2] + " " + form);
                            }
                        } catch (Throwable t) { HookLog.e("EncryptProbe 异常", t); }
                    }
                });
                n++;
            }

            for (final Method m : c.getDeclaredMethods()) {
                if (!m.getName().equals("decypt")) continue;
                HookLog.i("EncryptProbe: decypt 参数数=" + m.getParameterTypes().length);
                n++;
            }
            HookLog.i("EncryptProbe: encypt 挂了 " + n + " 个");

            try {
                Class<?> pc = HookApi.findClass(cl, "com.tenpay.sdk.net.core.processor.EncryptProcessor");
                if (pc != null) {
                    int pn = 0;
                    for (final Method pm : pc.getDeclaredMethods()) {
                        if (!pm.getName().equals("processEncrypt")) continue;
                        if (pm.getParameterTypes().length != 6) continue;
                        HookApi.before(pm, new HookApi.Action() {
                            @Override public void call(
                                    de.robv.android.xposed.XC_MethodHook.MethodHookParam p) {
                                try {
                                    String url = String.valueOf(p.args[0]);
                                    Object mp = p.args[2];
                                    String s = (mp instanceof java.util.Map)
                                            ? mp.toString() : String.valueOf(mp);
                                    if (url != null && (url.contains("grap") || url.contains("hongbao")
                                            || s.contains("listid"))) {
                                        HookLog.i("EncryptProbe PROC url=" + url + " params=" + s);
                                        MsgStore.add("ENCPROC url=" + url + " " + s);
                                    }
                                } catch (Throwable t) { HookLog.e("ProcProbe 异常", t); }
                            }
                        });
                        pn++;
                    }
                    HookLog.i("EncryptProbe: processEncrypt 挂了 " + pn + " 个");
                }
            } catch (Throwable t) { HookLog.e("ProcProbe 失败", t); }
        } catch (Throwable t) { HookLog.e("EncryptProbe 挂载失败", t); }
    }

    private static void installSendProbe(ClassLoader cl) {
        try {
            Class<?> c = HookApi.findClass(cl,
                    "com.tencent.qqnt.kernel.api.impl.MsgService");
            if (c == null) { HookLog.w("SendProbe: 找不到 MsgService"); return; }
            int n = 0;
            for (final Method m : c.getDeclaredMethods()) {
                if (!"sendMsg".equals(m.getName())) continue;
                HookLog.i("SendProbe: sendMsg 候选 argc=" + m.getParameterTypes().length);
                HookApi.before(m, new HookApi.Action() {
                    @Override public void call(
                            de.robv.android.xposed.XC_MethodHook.MethodHookParam p) {
                        try {
                            Object[] a = p.args;
                            StringBuilder sb = new StringBuilder("SENDMSG args=");
                            for (int i = 0; i < a.length; i++) {
                                Object o = a[i];
                                String s = (o == null) ? "null" : String.valueOf(o);
                                if (s.length() > 3000) s = s.substring(0, 3000) + "...";
                                sb.append("[").append(i).append("]=").append(s).append(" ");
                            }
                            HookLog.i(sb.toString());
                            MsgStore.add(sb.toString());
                        } catch (Throwable t) { HookLog.e("SendProbe 异常", t); }
                    }
                });
                n++;
            }
            HookLog.i("SendProbe: sendMsg 挂了 " + n + " 个");
        } catch (Throwable t) { HookLog.e("SendProbe 失败", t); }
    }

    public static void install(ClassLoader cl) {
        installEncryptProbe(cl);
        installSendProbe(cl);

        try {
            Class<?> c = HookApi.findClass(cl, "com.tenpay.sdk.net.gateway.QWalletGatewayServlet");
            if (c == null) {
                HookLog.w("GatewayProbe: 找不到 QWalletGatewayServlet");
            } else {
                int n = 0;
                for (final Method m : c.getDeclaredMethods()) {
                    if (!"sendRequest".equals(m.getName())) continue;
                    if (m.getParameterTypes().length != 8) continue;
                    m.setAccessible(true);
                    HookApi.before(m, new HookApi.Action() {
                        @Override public void call(
                                de.robv.android.xposed.XC_MethodHook.MethodHookParam p) {
                            try {
                                StringBuilder sb = new StringBuilder();
                                sb.append("[Gateway] sendRequest8");

                                Object[] a = p.args;
                                if (a != null) {
                                    sb.append(" service=").append(a[0]);
                                    sb.append(" method=").append(a[1]);
                                    sb.append(" n=").append(a.length);
                                    for (int i = 2; i < a.length; i++) {
                                        Object v = a[i];
                                        sb.append(" |a").append(i).append("=")
                                                .append(v == null ? "null"
                                                        : v.getClass().getSimpleName());
                                    }
                                }
                                try {
                                    Object req = a[2];
                                    if (req != null) {
                                        Method tb = req.getClass().getMethod("toByteArray");
                                        byte[] raw = (byte[]) tb.invoke(req);
                                        sb.append("\n  reqHex=").append(hex(raw));
                                    }
                                } catch (Throwable t) {
                                    sb.append(" [req dump 失败 ").append(t).append("]");
                                }
                                HookLog.i(sb.toString());
                                try { MsgStore.add(sb.toString()); } catch (Throwable ignore) {}
                            } catch (Throwable t) {
                                HookLog.e("GatewayProbe sendRequest8 异常", t);
                            }
                        }
                    });
                    n++;
                }
                HookLog.i("GatewayProbe: sendRequest8 挂了 " + n + " 个");
            }
        } catch (Throwable t) {
            HookLog.e("GatewayProbe: 挂 QWalletGatewayServlet 失败", t);
        }

        try {
            Class<?> c = HookApi.findClass(cl, "com.tenpay.sdk.net.gateway.QWalletGatewayBaseServlet");
            if (c == null) {
                HookLog.w("GatewayProbe: 找不到 QWalletGatewayBaseServlet");
                return;
            }
            int n = 0;
            for (final Method m : c.getDeclaredMethods()) {
                if (!"sendRequest".equals(m.getName())) continue;
                if (m.getParameterTypes().length != 7) continue;
                m.setAccessible(true);
                HookApi.before(m, new HookApi.Action() {
                    @Override public void call(
                            de.robv.android.xposed.XC_MethodHook.MethodHookParam p) {
                        try {
                            Object[] a = p.args;
                            StringBuilder sb = new StringBuilder();
                            sb.append("[Gateway] base.sendRequest7");
                            if (a != null) {
                                sb.append(" service=").append(a[0]);
                                sb.append(" method=").append(a[1]);
                                sb.append(" needLogin=").append(a[3]);
                                sb.append(" needEncrypt=").append(a[4]);
                                if (a[2] instanceof byte[]) {
                                    sb.append("\n  payloadHex=").append(hex((byte[]) a[2]));
                                }
                            }
                            HookLog.i(sb.toString());
                            try { MsgStore.add(sb.toString()); } catch (Throwable ignore) {}
                        } catch (Throwable t) {
                            HookLog.e("GatewayProbe base 异常", t);
                        }
                    }
                });
                n++;
            }
            HookLog.i("GatewayProbe: base.sendRequest7 挂了 " + n + " 个");
        } catch (Throwable t) {
            HookLog.e("GatewayProbe: 挂 BaseServlet 失败", t);
        }
    }

    private static String hex(byte[] d) {
        if (d == null) return "null";
        StringBuilder h = new StringBuilder();
        int lim = Math.min(d.length, 600);
        for (int k = 0; k < lim; k++) {
            int bi = d[k] & 0xFF;
            h.append(HEXC[bi >>> 4]).append(HEXC[bi & 0xF]);
        }
        if (d.length > lim) h.append("...(共").append(d.length).append("B)");
        return h.toString();
    }
}
