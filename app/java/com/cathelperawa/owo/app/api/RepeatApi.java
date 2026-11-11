package com.cathelperawa.owo.app.api;

import com.cathelperawa.owo.app.proto.PbSender;
import com.cathelperawa.owo.app.proto.RepeatCore;
import com.cathelperawa.owo.app.util.Refl;

public final class RepeatApi {

    private RepeatApi() {}

    public static RepeatCore.Outcome repeat(Object recordOrMsgItem) {
        Object record = CatApi.unwrapRecord(recordOrMsgItem);
        if (record == null) return null;
        int ct = Refl.i32(Refl.field(record, "chatType"), 0);
        String peer = CatApi.strOf(Refl.field(record, "peerUid"));
        if (peer == null) peer = CatApi.strOf(Refl.field(record, "peerUin"));
        return RepeatCore.repeat(record, ct, peer);
    }

    public static String elementKind(Object el) {
        return RepeatCore.kind(el);
    }

    public static String grab(Object recordOrMsgItem) {
        Object record = CatApi.unwrapRecord(recordOrMsgItem);
        return record == null ? null : RepeatCore.grabJson(record);
    }

    public static String sendCode(String contentJson, int chatType, String peerUid) {
        return PbSender.route(contentJson, chatType, peerUid);
    }

    public static boolean sendLongMsg(String contentJson, int chatType, String peerUid) {
        return PbSender.sendLong(contentJson, chatType, peerUid);
    }

    public static boolean sendDirectPb(String contentJson, int chatType, String peerUid) {
        return PbSender.sendDirect(contentJson, chatType, peerUid);
    }

    public static boolean sendRawCmd(String cmd, byte[] body) {
        return PbSender.sendCmd(cmd, body);
    }

    public static String lastResId() {
        return PbSender.lastResId();
    }
}
