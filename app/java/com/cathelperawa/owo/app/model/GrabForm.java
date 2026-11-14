package com.cathelperawa.owo.app.model;

import java.util.LinkedHashMap;
import java.util.Map;

public final class GrabForm {

    private GrabForm() {}

    public static final int CHANNEL_NORMAL = 1;
    public static final int CHANNEL_COMMAND = 32;
    public static final int CHANNEL_VOICE = 65536;

    public static final class Command {
        public final String answer;
        public final long msgRandom;
        public final long seq;
        public Command(String answer, long msgRandom, long seq) {
            this.answer = answer;
            this.msgRandom = msgRandom;
            this.seq = seq;
        }
    }

    public static Map<String, String> preGrab(CatPeer peer, String billNo, String authkey,
                                              int channel, String uin, String userUin,
                                              String name, int transSeq) {
        Map<String, String> m = new LinkedHashMap<String, String>();
        boolean isGroup = peer.chatType().isGroup();
        boolean isCommand = (channel == CHANNEL_COMMAND);

        m.put("listid", billNo);
        m.put("channel", String.valueOf(channel));
        m.put("groupid", isGroup ? peer.uin() : uin);
        m.put("grouptype", isGroup ? "1" : "0");
        m.put("groupuin", userUin);
        m.put("name", name);
        m.put("pay_flag", (isGroup && !isCommand) ? "0" : "1");
        m.put("authkey", authkey);
        m.put("uin", uin);
        if (!isGroup) m.put("senderuin", userUin);
        m.put("trans_seq", String.valueOf(transSeq));
        m.put("hb_from", "0");
        return m;
    }

    public static final class Builder {

        private final CatPeer peer;
        private final String billNo;
        private final String authkey;
        private final String uin;
        private final String userUin;
        private final String name;

        private int channel = CHANNEL_NORMAL;
        private String token;
        private boolean asPrivate = false;
        private Command command;

        private Builder(CatPeer peer, String billNo, String authkey,
                        String uin, String userUin, String name) {
            this.peer = peer;
            this.billNo = billNo;
            this.authkey = authkey;
            this.uin = uin;
            this.userUin = userUin;
            this.name = name;
        }

        public Builder channel(int c) { this.channel = c; return this; }

        public Builder token(String t) { this.token = t; return this; }

        public Builder asPrivate(boolean v) { this.asPrivate = v; return this; }

        public Builder command(Command c) { this.command = c; return this; }

        public Map<String, String> build() {
            Map<String, String> m = new LinkedHashMap<String, String>();
            boolean isGroup = peer.chatType().isGroup();
            boolean isCommand = (channel == CHANNEL_COMMAND);
            boolean isVoice = (channel == CHANNEL_VOICE);

            boolean effGroup = isGroup && !asPrivate;

            m.put("channel", String.valueOf(channel));
            m.put("listid", billNo);
            m.put("groupid", effGroup ? peer.uin() : uin);
            m.put("grouptype", effGroup ? "1" : "0");
            m.put("groupuin", userUin);
            m.put("name", name);

            m.put("pay_flag", (isVoice || (effGroup && !isCommand)) ? "0" : "1");
            m.put("authkey", authkey);
            m.put("uin", uin);
            if (!effGroup && !isVoice) m.put("senderuin", userUin);
            m.put("hb_from", "0");
            m.put("agreement", "0");

            if (isCommand && effGroup && command != null
                    && command.answer != null && !command.answer.isEmpty()) {
                m.put("answer", command.answer);
                m.put("msg_id", String.valueOf(command.msgRandom));
                m.put("msg_md5", md5(command.answer));
                m.put("msg_seq", String.valueOf(command.seq));
            }

            if (token != null && !token.isEmpty()) m.put("pre_grap_token", token);
            return m;
        }
    }

    public static Builder builder(CatPeer peer, String billNo, String authkey,
                                  String uin, String userUin, String name) {
        return new Builder(peer, billNo, authkey, uin, userUin, name);
    }

    static String md5(String s) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] d = md.digest(s.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder(d.length * 2);
            for (byte b : d) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (Throwable t) {
            return "";
        }
    }
}
