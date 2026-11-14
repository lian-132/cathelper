package com.cathelperawa.owo.app.feature;

import com.cathelperawa.owo.app.bridge.QQEnv;
import com.cathelperawa.owo.app.core.HookLog;

import org.json.JSONObject;

public final class SsoArkSign {

    public static final String CMD = "trpc.gamecenter.sendmsg.Sendmsg.SsoArkSign";

    private SsoArkSign() {}

    public static final class Result {
        public String json;
        public String raw;
        public String error;
        public boolean ok() { return json != null && json.length() > 2; }
    }

    public static Result sign(String arkJson, String refer) {
        return sign(arkJson, refer, null, null);
    }

    public static Result sign(String arkJson, String refer, String cookieOverride, String optionOverride) {
        Result r = new Result();
        try {
            JSONObject ark = new JSONObject(arkJson);

            JSONObject cfg = ark.optJSONObject("config");
            if (cfg == null) {
                Object rawCfg = ark.opt("config");
                if (rawCfg instanceof String) {
                    String t = ((String) rawCfg).trim();
                    if (t.startsWith("{")) cfg = new JSONObject(t);
                }
            }
            if (cfg == null) cfg = new JSONObject();

            cfg.remove("token");
            cfg.remove("ctime");
            ark.put("config", cfg.toString());

            try {
                String myU = com.cathelperawa.owo.app.bridge.QQEnv.currentUin();
                if (myU != null && myU.startsWith("o")) myU = myU.substring(1);
                long my = Long.parseLong(myU == null ? "0" : myU.trim());
                JSONObject ex = ark.optJSONObject("extra");
                if (ex != null) {
                    ex.put("uin", my);
                }
            } catch (Throwable ignore) {
            }

            Object metaObj = ark.opt("meta");
            if (metaObj instanceof JSONObject || metaObj instanceof org.json.JSONArray) {
                ark.put("meta", metaObj.toString());
            }

            String rawUin = com.cathelperawa.owo.app.bridge.QQEnv.currentUin();
            if (rawUin == null) rawUin = "";
            rawUin = rawUin.trim();
            if (rawUin.startsWith("o")) rawUin = rawUin.substring(1);
            String oUin = "o" + rawUin;

            String ps = null;
            String psFrom = "";
            String[] domains = {"gamecenter.qq.com", "youxi.gamecenter.qq.com",
                    "qzone.qq.com", "qun.qq.com", "qq.com"};
            for (String d : domains) {
                ps = ticketPskey(d);
                if (ps != null && ps.length() > 0) { psFrom = "ticket:" + d; break; }
                try { ps = com.cathelperawa.owo.app.bridge.TimCookie.getPskey(d); } catch (Throwable ignore) {}
                if (ps != null && ps.length() > 0) { psFrom = "tim:" + d; break; }
            }
            if (ps != null && ps.length() > 0) dump("pskey", psFrom + " => " + ps);

            String skey = null;
            try { skey = com.cathelperawa.owo.app.bridge.TimCookie.getRealSkey(); } catch (Throwable ignore) {}
            if (skey == null || skey.length() == 0) {
                String c0 = ArkSigner.cookieFromManager();
                if (c0 == null || c0.length() < 5) c0 = ArkSigner.cookieFromDb();
                skey = ArkSigner.pskeyOf(c0);
            }
            if (skey == null) skey = "";

            String cookie;
            if (cookieOverride != null && cookieOverride.trim().length() > 0) {

                cookie = cookieOverride.trim();
                psFrom = "manual";
            } else if (ps != null && ps.length() > 0) {
                cookie = "uin=" + oUin + "; p_uin=" + oUin + "; skey=" + skey + "; p_skey=" + ps;
            } else {
                String c0 = ArkSigner.cookieFromManager();
                if (c0 == null || c0.length() < 10) c0 = ArkSigner.cookieFromDb();
                cookie = c0 == null ? "" : c0;
                r.error = "拿不到 p_skey";
            }
            if (refer == null || refer.trim().length() == 0) refer = "https://x.jq.qq.com/";

            JSONObject body = new JSONObject();
            body.put("content", ark);
            body.put("context", new JSONObject().put("refer", refer));
            body.put("Cookie", cookie);

            JSONObject option;
            if (optionOverride != null && optionOverride.trim().length() > 0) {
                option = new JSONObject(optionOverride.trim());
            } else {
                option = new JSONObject();
                option.put("fingerprint", android.os.Build.FINGERPRINT);
                option.put("model", android.os.Build.MODEL);
                option.put("manufacturer", android.os.Build.MANUFACTURER);
                option.put("brand", android.os.Build.BRAND);
                option.put("device", android.os.Build.DEVICE);
                option.put("product", android.os.Build.PRODUCT);
                option.put("id", android.os.Build.ID);
                option.put("level", android.os.Build.VERSION.SDK_INT);
                String[] abis = android.os.Build.SUPPORTED_ABIS;
                option.put("cpu_abi", (abis == null || abis.length <= 0) ? "" : abis[0]);
                option.put("cpu_abi2", (abis == null || abis.length <= 1) ? "" : abis[1]);
            }
            body.put("option", option);

            JSONObject head = new JSONObject();
            head.put("1", 109);
            head.put("2", "14");
            head.put("3", appVersion());
            JSONObject req = new JSONObject();
            req.put("1", head);
            req.put("2", body.toString());

            String packet = req.toString();
            dump("req", packet);

            byte[] pb = com.cathelperawa.owo.app.ovo.QPacketHelper.buildMessage(packet);
            if (pb == null || pb.length == 0) {
                r.error = "组包失败";
                return r;
            }
            com.cathelperawa.owo.app.proto.PbSender.setupRecvHook();
            com.cathelperawa.owo.app.proto.PbSender.startCapture("trpc.gamecenter.sendmsg");

            boolean sent = com.cathelperawa.owo.app.ovo.QQInterfaces.sendBuffer(CMD, true, pb);
            if (!sent) {
                com.cathelperawa.owo.app.proto.PbSender.stopCapture();
                r.error = "通道未就绪";
                return r;
            }

            String cap = null;
            for (int i = 0; i < 30; i++) {
                try { Thread.sleep(100); } catch (Throwable ignore) {}
                String[] pk = com.cathelperawa.owo.app.proto.PbSender.peekRecv();
                cap = pk == null ? null : pk[1];
                if (cap != null && cap.length() > 0) break;
            }
            com.cathelperawa.owo.app.proto.PbSender.stopCapture();
            r.raw = cap;
            dump("resp", cap == null ? "(no response)" : cap);

            if (cap == null || cap.length() == 0) {
                r.error = "3 秒内没回包";
                return r;
            }

            String signed = extractArk(cap);
            if (signed == null) {

                String msg = decodeErrMsg(cap);
                r.error = msg == null ? "回包里没找到 ARK" : msg;
                return r;
            }
            r.json = signed;
            return r;
        } catch (Throwable t) {
            HookLog.e("SsoArkSign", t);
            r.error = String.valueOf(t);
            return r;
        }
    }

    private static String extractArk(String raw) {
        String text = raw;

        if (text.matches("^[0-9a-fA-F]+$") && text.length() > 40) {
            try {

                int n = text.length() / 2;
                byte[] buf = new byte[n];
                for (int k = 0; k < n; k++) {
                    buf[k] = (byte) Integer.parseInt(text.substring(k * 2, k * 2 + 2), 16);
                }
                text = new String(buf, "UTF-8");
            } catch (Throwable ignore) {
            }
        }
        int i = text.indexOf("{\"app\"");
        if (i < 0) i = text.indexOf("{\"ark\"");
        if (i >= 0) {
            int depth = 0;
            for (int j = i; j < text.length(); j++) {
                char c = text.charAt(j);
                if (c == '{') depth++;
                else if (c == '}') {
                    depth--;
                    if (depth == 0) return text.substring(i, j + 1);
                }
            }
        }
        return null;
    }

    public static String ticketPskey(String domain) {
        try {
            Object app = com.cathelperawa.owo.app.bridge.QQEnv.appRuntime();
            if (app == null) return null;
            java.lang.reflect.Method gm = null;
            Class<?> c = app.getClass();
            while (c != null && gm == null) {
                try { gm = c.getMethod("getManager", int.class); }
                catch (Throwable ignore) { c = c.getSuperclass(); }
            }
            if (gm == null) return null;
            Object mgr = gm.invoke(app, 2);
            if (mgr == null) return null;
            java.lang.reflect.Method gp = null;
            Class<?> m = mgr.getClass();
            while (m != null && gp == null) {
                try { gp = m.getDeclaredMethod("getPskey", String.class, String.class); }
                catch (Throwable ignore) { m = m.getSuperclass(); }
            }
            if (gp == null) return null;
            gp.setAccessible(true);
            String uin = com.cathelperawa.owo.app.bridge.QQEnv.currentUin();
            Object v = gp.invoke(mgr, uin, domain);
            return v instanceof String ? (String) v : null;
        } catch (Throwable t) {
            HookLog.w("ticketPskey " + domain + ": " + t);
            return null;
        }
    }

    public static String probe() {
        String[][] combos = {
                {"com.tencent.gamecenter.egameshare", "noDataView"},
                {"com.tencent.gamecenter.egameshare", "dataView"},
                {"com.tencent.gamecenter.gameshare", "noDataView"},
                {"com.tencent.gamecenter.gameshare_sgame", "commonView"},
                {"com.tencent.gamecenter.qqsy", "picView2"},
                {"com.tencent.gamecenter.qqsy", "mieTeamView"},
                {"com.tencent.tuwen.lua", "news"},
                {"com.tencent.miniapp", "picView2"},
                {"com.tencent.structmsg", "musicView"},
        };
        StringBuilder sb = new StringBuilder();
        for (String[] c : combos) {
            String probeArk = "{\"app\":\"" + c[0] + "\",\"view\":\"" + c[1] + "\","
                    + "\"desc\":\"probe\",\"ver\":\"1.0.0.1\",\"prompt\":\"probe\","
                    + "\"config\":{\"type\":\"normal\",\"forward\":1},"
                    + "\"meta\":{\"shareData\":{\"appid\":\"1105198412\","
                    + "\"title\":\"probe\",\"desc\":\"probe\","
                    + "\"url\":\"https://p.qlogo.cn/gdynamic/MIwbbVhjoVxibNJPGsv8JgNUvNicibdgnRe73ygUKmb104/0\"}}}";
            Result r = sign(probeArk, "https://gamecenter.qq.com/");
            sb.append(c[0].replace("com.tencent.", "")).append(" / ").append(c[1])
              .append("  =>  ").append(r.ok() ? "OK" : r.error).append("\n");
            try { Thread.sleep(600); } catch (Throwable ignore) {}
        }
        dump("probe", sb.toString());
        return sb.toString();
    }

    private static String decodeErrMsg(String hexStr) {
        try {
            int n = hexStr.length() / 2;
            byte[] b = new byte[n];
            for (int i = 0; i < n; i++) {
                b[i] = (byte) Integer.parseInt(hexStr.substring(i * 2, i * 2 + 2), 16);
            }

            StringBuilder sb = new StringBuilder();
            for (byte x : b) {
                int c = x & 0xFF;
                if (c >= 32 && c < 127) sb.append((char) c);
                else if (sb.length() > 0) break;
            }
            String t = sb.toString();
            return t.length() > 3 ? t : null;
        } catch (Throwable t) {
            return null;
        }
    }

    private static String appVersion() {
        try {
            String v = com.cathelperawa.owo.app.core.HostVersion.name(QQEnv.appContext());
            if (v != null && v.length() > 0) return v.trim();
        } catch (Throwable ignore) {
        }
        return "9.2.0";
    }

    private static void dump(String tag, String text) {
        try {
            java.io.File dir = QQEnv.appContext().getFilesDir();
            java.io.FileWriter w = new java.io.FileWriter(
                    new java.io.File(dir, "sso_" + tag + ".txt"), false);
            w.write(text == null ? "" : text);
            w.close();
        } catch (Throwable ignore) {
        }
    }
}
