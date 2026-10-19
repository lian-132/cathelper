package com.cathelperawa.owo.app.api;

import com.cathelperawa.owo.app.bridge.GhostChat;

public final class GhostApi {

    private GhostApi() {}

    public static boolean send(String text) {
        return GhostChat.send(text);
    }

    public static boolean send(String text, String fakeUid) {
        return GhostChat.send(text, fakeUid);
    }

    public static boolean log(String tag, String body) {
        return GhostChat.log(tag, body);
    }

    public static String uid() {
        return GhostChat.GHOST_UID;
    }
}
