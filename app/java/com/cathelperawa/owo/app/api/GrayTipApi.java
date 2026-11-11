package com.cathelperawa.owo.app.api;

import com.cathelperawa.owo.app.bridge.GrayTip;

public final class GrayTipApi {

    private GrayTipApi() {}

    public static boolean add(int chatType, String peerUid, String json) {
        return GrayTip.add(chatType, peerUid, json);
    }

    public static boolean text(int chatType, String peerUid, String text) {
        String j = GrayTip.buildTextTip(text);
        return j != null && GrayTip.add(chatType, peerUid, j);
    }

    public static boolean textRef(int chatType, String peerUid, String text, String refText, int msgSeq) {
        String j = GrayTip.buildTextRefTip(text, refText, msgSeq);
        return j != null && GrayTip.add(chatType, peerUid, j);
    }

    public static String json(String text) { return GrayTip.buildTextTip(text); }

    public static boolean toContact(int busiId, Object contact, String json) {
        return GrayTip.add(busiId, contact, json);
    }

    
    public static String itemText(String txt) { return GrayTip.itemText(txt); }

    public static String itemUser(String nick, String uid, String uin) {
        return GrayTip.itemUser(nick, uid, uin);
    }

    public static String itemRef(String txt, int msgSeq) { return GrayTip.itemRef(txt, msgSeq); }

    public static String compose(String... items) { return GrayTip.compose(items); }

    public static boolean items(int chatType, String peerUid, String... items) {
        return GrayTip.addItems(chatType, peerUid, items);
    }
}
