package com.cathelperawa.owo.app.bridge;

import com.cathelperawa.owo.app.core.HookLog;

public final class GhostChat {

    private GhostChat() {}

    public static final String GHOST_UID = "0";

    public static final int GHOST_CHAT_TYPE = 2;

    public static boolean send(String text) {
        return send(text, GHOST_UID);
    }

    public static boolean send(String text, String fakeUid) {
        if (text == null) return false;
        try {
            String uid = (fakeUid == null || fakeUid.isEmpty()) ? GHOST_UID : fakeUid;
            Object contact = ContactFactory.make(GHOST_CHAT_TYPE, uid, "");
            if (contact == null) { HookLog.w("GhostChat: Contact 构造失败"); return false; }
            java.util.List<Object> els = java.util.Collections.singletonList(
                    ElementFactory.text(text));
            boolean ok = MsgSender.send(contact, els);
            HookLog.i("幽灵会话: uid=" + uid + " ok=" + ok + " text=" + text);
            return ok;
        } catch (Throwable t) {
            HookLog.e("GhostChat 发送异常", t);
            return false;
        }
    }

    public static boolean log(String tag, String body) {
        String ts = new java.text.SimpleDateFormat("MM-dd HH:mm:ss",
                java.util.Locale.US).format(new java.util.Date());
        return send("[" + ts + "] " + tag + " | " + body);
    }
}
