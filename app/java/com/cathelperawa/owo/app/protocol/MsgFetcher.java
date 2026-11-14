package com.cathelperawa.owo.app.protocol;

import com.cathelperawa.owo.app.ovo.QQInterfaces;
import com.cathelperawa.owo.app.core.HookLog;
import com.cathelperawa.owo.app.util.Refl;

public final class MsgFetcher {

    private MsgFetcher() {}

    public static String pull(Object record) {
        if (record == null) return null;
        int chatType = Refl.i32(Refl.field(record, "chatType"), 0);
        try {
            if (chatType == 1) {
                return pullC2C(record);
            } else if (chatType == 2) {
                return pullGroup(record);
            } else {
                HookLog.w("不支持的聊天类型: " + chatType);
                return null;
            }
        } catch (Throwable t) {
            HookLog.e("拉取消息失败", t);
            return null;
        }
    }

    public static String pullC2C(Object record) {
        String peerUin = Refl.strOr(Refl.field(record, "peerUin"),
                Refl.strOr(Refl.field(record, "peerUid"), ""));
        long msgTime = Refl.lng(Refl.field(record, "msgTime"), 0);
        String json = "{\"1\": " + num(peerUin) + ", \"2\": " + msgTime + ", \"3\": 0, \"4\": 1}";
        HookLog.i("拉取 C2C: " + json);
        ovoSend("MessageSvc.PbGetOneDayRoamMsg", json);
        return "MessageSvc.PbGetOneDayRoamMsg";
    }

    public static String pullGroup(Object record) {
        String peerUid = Refl.strOr(Refl.field(record, "peerUid"), "");
        long msgSeq = Refl.lng(Refl.field(record, "msgSeq"), 0);
        String json = "{\"1\": " + num(peerUid) + ", \"2\": " + msgSeq + ", \"3\": " + msgSeq + ", \"6\": 0}";
        HookLog.i("拉取群: " + json);
        ovoSend("MessageSvc.PbGetGroupMsg", json);
        return "MessageSvc.PbGetGroupMsg";
    }

    private static void ovoSend(String cmd, String json) {
        byte[] payload = com.cathelperawa.owo.app.ovo.QPacketHelper.buildMessage(json);
        if (payload != null) QQInterfaces.sendBuffer(cmd, true, payload);
    }

    private static String num(String v) {
        if (v == null) return "0";
        if (v.matches("-?\\d+")) return v;
        return "\"" + v.replace("\"", "\\\"") + "\"";
    }
}
