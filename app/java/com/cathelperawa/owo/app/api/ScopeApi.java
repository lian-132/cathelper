package com.cathelperawa.owo.app.api;

import com.cathelperawa.owo.app.MsgStore;

public final class ScopeApi {

    private ScopeApi() {}

    public static void notePeer(String uid, String name, boolean group) {
        MsgStore.notePeer(uid, name, group);
    }

    public static boolean all() { return MsgStore.isScopeAll(); }

    public static void setAll(boolean v) { MsgStore.setScopeAll(v); }

    public static boolean isSelected(String uid) { return MsgStore.isSelected(uid); }

    public static void toggleSelected(String uid) { MsgStore.toggleSelected(uid); }

    
    public static boolean allows(String uid) { return MsgStore.allows(uid); }

    public static void toggleUnderAll(String uid) { MsgStore.toggleUnderAll(uid); }

    public static boolean isUnderAllOff(String uid) { return MsgStore.isUnderAllOff(uid); }

    public static java.util.Map<String, String> peers() { return MsgStore.peers(); }
}
