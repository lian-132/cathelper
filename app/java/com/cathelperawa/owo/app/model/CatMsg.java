package com.cathelperawa.owo.app.model;

import com.cathelperawa.owo.app.util.Refl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class CatMsg {

    private final Object raw;   

    private CatMsg(Object raw) {
        this.raw = raw;
    }

    public static CatMsg of(Object recordOrMsgItem) {
        Object r = unwrap(recordOrMsgItem);
        return r == null ? null : new CatMsg(r);
    }

    public Object raw() {
        return raw;
    }

    

    
    public ChatType chatType() {
        return ChatType.of(Refl.i32(Refl.field(raw, "chatType"), 0));
    }

    public String peerUid() {
        String s = Refl.str(Refl.field(raw, "peerUid"));
        return s == null ? "" : s;
    }

    public String peerUin() {
        String s = Refl.str(Refl.field(raw, "peerUin"));
        return s == null ? "" : s;
    }

    public CatPeer peer() {
        ChatType t = chatType();
        String uid = peerUid();
        if (!t.isKnown() || uid.isEmpty()) {
            
            String uin = peerUin();
            if (uin.isEmpty()) return null;
            try {
                return CatPeer.of(t.code, uin);
            } catch (Throwable ex) {
                return null;
            }
        }
        try {
            return CatPeer.of(t.code, uid);
        } catch (Throwable ex) {
            com.cathelperawa.owo.app.core.HookLog.w("[CatMsg] peer 构造失败: " + ex);
            return null;
        }
    }

    public String senderUin() {
        String s = Refl.str(Refl.field(raw, "senderUin"));
        return s == null ? "" : s;
    }

    public String senderUid() {
        String s = Refl.str(Refl.field(raw, "senderUid"));
        return s == null ? "" : s;
    }

    public String senderName() {
        String s = Refl.str(Refl.field(raw, "sendNickName"));
        return s == null ? "" : s;
    }

    public long time() {
        return Refl.lng(Refl.field(raw, "msgTime"), 0);
    }

    public long msgId() {
        return Refl.lng(Refl.field(raw, "msgId"), 0);
    }

    public long msgSeq() {
        return Refl.lng(Refl.field(raw, "msgSeq"), 0);
    }

    

    
    @SuppressWarnings("unchecked")
    public List<Object> elements() {
        Object els = Refl.field(raw, "elements");
        if (els instanceof List) return (List<Object>) els;
        return Collections.emptyList();
    }

    public List<CatElement> catElements() {
        List<Object> raw = elements();
        if (raw.isEmpty()) return Collections.emptyList();
        List<CatElement> out = new ArrayList<CatElement>(raw.size());
        for (Object o : raw) out.add(CatElement.of(o));
        return out;
    }

    public String text() {
        for (CatElement e : catElements()) {
            if (e.kind() == CatElement.Kind.TEXT) {
                Object te = Refl.field(e.raw(), "textElement");
                if (te != null) {
                    String c = Refl.str(Refl.field(te, "content"));
                    if (c != null) return c;
                }
            }
        }
        return "";
    }

    

    
    public boolean isGroup() {
        return chatType().isGroup();
    }

    public boolean isFriend() {
        return chatType().isFriend();
    }

    public boolean valid() {
        return raw != null && chatType().isKnown();
    }

    

    
    private static Object unwrap(Object o) {
        if (o == null) return null;
        try {
            if (Refl.field(o, "elements") != null) return o;
        } catch (Throwable t) {
            com.cathelperawa.owo.app.core.HookLog.d("[CatMsg] elements 探测失败: " + t);
        }
        try {
            java.lang.reflect.Method g = o.getClass().getMethod("getMsgRecord");
            g.setAccessible(true);
            Object r = g.invoke(o);
            if (r != null) return r;
        } catch (Throwable t) {
            com.cathelperawa.owo.app.core.HookLog.d("[CatMsg] getMsgRecord 探测失败: " + t);
        }
        return null;
    }

    @Override
    public String toString() {
        return "CatMsg(" + chatType().name() + " peer=" + peerUid()
                + " from=" + senderUin() + " seq=" + msgSeq() + ")";
    }
}
