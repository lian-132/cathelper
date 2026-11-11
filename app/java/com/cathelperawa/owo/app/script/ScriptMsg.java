package com.cathelperawa.owo.app.script;

public final class ScriptMsg {

    public final String peer;
    
    public final String peerName;
    
    public final String sender;
    
    public final String senderName;
    
    public final String text;
    
    public final boolean group;
    
    public final boolean atMe;
    
    public final boolean self;
    public final long msgId;
    public final long time;
    
    public final String[] atList;
    
    public final String[] picList;
    
    public final long replyTo;
    
    public final Object raw;

    ScriptMsg(String peer, String peerName, String sender, String senderName,
              String text, boolean group, boolean atMe, boolean self,
              long msgId, long time) {
        this(peer, peerName, sender, senderName, text, group, atMe, self, msgId, time,
                new String[0], new String[0], 0L, null);
    }

    ScriptMsg(String peer, String peerName, String sender, String senderName,
              String text, boolean group, boolean atMe, boolean self,
              long msgId, long time, String[] atList, String[] picList, long replyTo) {
        this(peer, peerName, sender, senderName, text, group, atMe, self, msgId, time,
                atList, picList, replyTo, null);
    }

    ScriptMsg(String peer, String peerName, String sender, String senderName,
              String text, boolean group, boolean atMe, boolean self,
              long msgId, long time, String[] atList, String[] picList, long replyTo,
              Object raw) {
        this.peer = peer == null ? "" : peer;
        this.peerName = peerName == null ? "" : peerName;
        this.sender = sender == null ? "" : sender;
        this.senderName = senderName == null ? "" : senderName;
        this.text = text == null ? "" : text;
        this.group = group;
        this.atMe = atMe;
        this.self = self;
        this.msgId = msgId;
        this.time = time;
        this.atList = atList == null ? new String[0] : atList;
        this.picList = picList == null ? new String[0] : picList;
        this.replyTo = replyTo;
        this.raw = raw;
    }

    public String kind() { return group ? "group" : "friend"; }

    @Override public String toString() {
        return "ScriptMsg{" + (group ? "群" : "好友") + ":" + peerName
                + " " + senderName + ": " + text + "}";
    }
}
