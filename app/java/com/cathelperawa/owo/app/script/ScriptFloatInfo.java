package com.cathelperawa.owo.app.script;

public final class ScriptFloatInfo {

    public final String peer;
    
    public final String peerName;
    
    public final boolean group;
    
    public final String floatId;
    
    public final String label;

    ScriptFloatInfo(String floatId, String label, String peer, String peerName, boolean group) {
        this.floatId = floatId == null ? "" : floatId;
        this.label = label == null ? "" : label;
        this.peer = peer == null ? "" : peer;
        this.peerName = peerName == null ? "" : peerName;
        this.group = group;
    }

    public String kind() { return group ? "group" : "friend"; }

    @Override public String toString() {
        return "ScriptFloatInfo{" + floatId + " @ " + peer + "}";
    }
}
