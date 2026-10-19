package com.cathelperawa.owo.app.api;

import com.cathelperawa.owo.app.bridge.ContactFactory;
import com.cathelperawa.owo.app.bridge.ElementFactory;

public final class ElementApi {

    private ElementApi() {}

    public static Object text(String text) { return ElementFactory.text(text); }

    public static Object pic(String path) { return ElementFactory.pic(path); }

    public static Object ptt(String path) { return ElementFactory.ptt(path); }

    public static Object ark(String json) { return ElementFactory.ark(json); }

    public static Object face(int faceId) { return ElementFactory.face(faceId); }

    public static Object video(String path) { return ElementFactory.video(path); }

    public static Object file(String path) { return ElementFactory.file(path); }

    public static Object at(String uin, String display) { return ElementFactory.at(uin, display); }

    public static Object reply(long replyMsgId) { return ElementFactory.reply(replyMsgId); }

    public static Object contact(int chatType, String peerUid) {
        return ContactFactory.make(chatType, peerUid, "");
    }
}
