package com.cathelperawa.owo.app.proto;

public final class PbMsg {

    private PbMsg() {}

    public static final String CMD_SEND_MSG = "MessageSvc.PbSendMsg";
    
    public static final String CMD_UPLOAD_LONG = "trpc.group.long_msg_interface.MsgService.SsoSendLongMsg";

    public static byte[] buildDirect(int chatType, String peerUin, byte[] elemsPB,
                                     String selfUin, UidResolver resolver) {
        return buildDirect(chatType, peerUin, elemsPB, selfUin, resolver, 1, 0);
    }

    public static byte[] buildDirect(int chatType, String peerUin, byte[] elemsPB,
                                     String selfUin, UidResolver resolver,
                                     int msgType, int subMsgType) {
        byte[] richText = Pb.ld(1, elemsPB);
        byte[] msgBody = Pb.ld(3, richText);

        byte[] routing;
        byte[] extra6 = new byte[0];
        if (chatType == 2) {
            
            routing = Pb.ld(1, Pb.ld(2, Pb.vf(1, parseLong(peerUin))));
        } else {
            
            String uid = toUid(peerUin, resolver);
            routing = Pb.ld(1, Pb.ld(1, Pb.strField(2, uid)));
            extra6 = Pb.ld(6, Pb.vf(1, System.currentTimeMillis() / 1000L));
        }
        byte[] contentHead = Pb.ld(2, Pb.cat(
                Pb.vf(1, msgType), Pb.vf(2, subMsgType), Pb.vf(3, 0)));

        long rnd = System.currentTimeMillis();
        byte[] seqF = Pb.vf(4, rnd & 0xFFFF);
        byte[] randF = Pb.vf(5, (rnd >>> 16) & 0xFFFFFFFFL);

        return Pb.cat(routing, contentHead, msgBody, seqF, randF, extra6);
    }

    public static byte[] buildRef(int chatType, String peerUin, String resId, UidResolver resolver) {
        byte[] e19 = Pb.cat(Pb.vf(15, 0), Pb.vf(31, 0), Pb.vf(41, 0));
        byte[] elem37 = Pb.ld(37, Pb.cat(
                Pb.vf(6, 1), Pb.strField(7, resId), Pb.vf(17, 0), Pb.ld(19, e19)));
        byte[] richText = Pb.ld(1, Pb.ld(2, elem37));
        byte[] msgBody = Pb.ld(3, richText);

        byte[] routing;
        byte[] extra6 = new byte[0];
        if (chatType == 2) {
            routing = Pb.ld(1, Pb.ld(2, Pb.vf(1, parseLong(peerUin))));
        } else {
            String uid = toUid(peerUin, resolver);
            routing = Pb.ld(1, Pb.ld(1, Pb.strField(2, uid)));
            extra6 = Pb.ld(6, Pb.vf(1, System.currentTimeMillis() / 1000L));
        }
        byte[] contentHead = Pb.ld(2, Pb.cat(
                Pb.vf(1, 1), Pb.vf(2, 0), Pb.vf(3, 0)));

        long rnd = System.currentTimeMillis();
        byte[] seqF = Pb.vf(4, rnd & 0xFFFF);
        byte[] randF = Pb.vf(5, (rnd >>> 16) & 0xFFFFFFFFL);
        return Pb.cat(routing, contentHead, msgBody, seqF, randF, extra6);
    }

    

    
    public static byte[] buildMultiMsg(byte[] elemsPB) {
        byte[] richText = Pb.ld(1, elemsPB);
        byte[] body = Pb.ld(3, richText);
        byte[] items = Pb.ld(1, body);
        byte[] inner = Pb.cat(Pb.strField(1, "MultiMsg"), Pb.ld(2, items));
        return Pb.ld(2, inner);
    }

    public static final class FakeItem {
        public long uin;
        public String nick;
        public String text;
        
        public byte[] raw;
        
        public String type;
        
        public String brief;

        public FakeItem(long uin, String nick, String text) {
            this.uin = uin;
            this.nick = nick;
            this.text = text;
        }

        public FakeItem(long uin, String nick, byte[] raw) {
            this.uin = uin;
            this.nick = nick;
            this.text = "";
            this.raw = raw;
        }

        public FakeItem(long uin, String nick, byte[] raw, String type) {
            this.uin = uin;
            this.nick = nick;
            this.text = "";
            this.raw = raw;
            this.type = type;
        }
    }

    public static byte[] buildFakeMultiMsg(java.util.List<FakeItem> items) {
        return buildFakeMultiMsg(items, System.currentTimeMillis() / 1000L);
    }

    public static byte[] buildFakeMultiMsg(java.util.List<FakeItem> items, long base) {
        byte[] list = new byte[0];
        for (int i = 0; i < items.size(); i++) {
            FakeItem it = items.get(i);

            String uid = "";
            try {
                if (it.uin > 0) {
                    String u = com.cathelperawa.owo.app.bridge.QQUtils
                            .uidFromUin(String.valueOf(it.uin));
                    if (u != null && u.length() > 0) uid = u;
                }
            } catch (Throwable ignore) {
            }
            byte[] sender = Pb.ld(1, Pb.cat(
                    Pb.vf(1, it.uin),
                    uid.length() > 0 ? Pb.strField(2, uid) : new byte[0],
                    Pb.ld(8, Pb.cat(
                            Pb.vf(1, it.uin > 0 ? it.uin : 10001),
                            Pb.strField(4, it.nick == null ? "" : it.nick),
                            Pb.vf(5, 2)))));
            long t = base + i * 60L;
            
            long r4 = 1000000000L + (base % 900000000L) + i * 7L;
            long r5 = 3000L + i;
            long r6 = t;
            byte[] e15 = Pb.ld(15, Pb.cat(
                    Pb.vf(1, 0), Pb.vf(2, 0), Pb.vf(3, 0),
                    Pb.strField(4, ""), Pb.strField(5, "")));
            byte[] head = Pb.ld(2, Pb.cat(
                    Pb.vf(1, 82),
                    Pb.vf(4, r4), Pb.vf(5, r5), Pb.vf(6, r6),
                    Pb.vf(7, 1), Pb.vf(8, 0), Pb.vf(9, 0), e15));
            byte[] elems;
            if (it.raw != null && it.raw.length > 0) {
                elems = it.raw;                    
            } else {
                elems = Pb.elemBytes("{\"1\":{\"1\":"
                        + org.json.JSONObject.quote(it.text == null ? "" : it.text) + "}}");
            }
            if (elems.length == 0) continue;
            byte[] body = Pb.ld(3, Pb.ld(1, elems));
            list = Pb.cat(list, Pb.ld(1, Pb.cat(sender, head, body)));
        }
        byte[] inner = Pb.cat(Pb.strField(1, "MultiMsg"), Pb.ld(2, list));
        return Pb.ld(2, inner);
    }

    public static byte[] buildUploadReq(int chatType, String peerUin, byte[] gz, UidResolver resolver) {
        byte[] settings = Pb.cat(Pb.vf(1, 4), Pb.vf(2, 2), Pb.vf(3, 9), Pb.vf(4, 0));
        if (chatType == 1) {
            String uid = toUid(peerUin, resolver);
            byte[] info = Pb.cat(Pb.vf(1, 1), Pb.ld(2, Pb.strField(2, uid)), Pb.ld(4, gz));
            return Pb.cat(Pb.ld(2, info), Pb.ld(15, settings));
        }
        byte[] info = Pb.cat(Pb.vf(1, 3), Pb.ld(2, Pb.vf(2, parseLong(peerUin))), Pb.ld(4, gz));
        return Pb.cat(Pb.ld(2, info), Pb.ld(15, settings));
    }

    

    
    public interface UidResolver {
        String uidFromUin(String uin);
    }

    private static String toUid(String peerUin, UidResolver r) {
        if (peerUin == null) return "";
        if (!peerUin.matches("[0-9]+")) return peerUin;   
        if (r == null) return peerUin;
        try {
            String u = r.uidFromUin(peerUin);
            if (u != null && u.length() > 0) return u;
        } catch (Throwable ignore) {
        }
        return peerUin;
    }

    private static long parseLong(String s) {
        try {
            return Long.parseLong(s);
        } catch (Throwable t) {
            return 0L;
        }
    }
}
