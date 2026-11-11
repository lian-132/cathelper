package com.cathelperawa.owo.app.model;

import com.cathelperawa.owo.app.bridge.ContactApi;
import com.cathelperawa.owo.app.bridge.ContactFactory;
import com.cathelperawa.owo.app.bridge.QQEnv;
import com.cathelperawa.owo.app.bridge.QQUtils;

public abstract class CatPeer {

    public abstract ChatType chatType();

    public abstract String uid();

    public abstract String uin();

    

    
    public static CatPeer group(String groupUin) {
        if (isBlank(groupUin)) throw new IllegalArgumentException("群号不能为空");
        return new GroupPeer(groupUin);
    }

    public static CatPeer friend(String uinOrUid) {
        if (isBlank(uinOrUid)) throw new IllegalArgumentException("好友 uid/uin 不能为空");
        return new FriendPeer(uinOrUid);
    }

    public static CatPeer self() {
        return new SelfPeer();
    }

    public static CatPeer of(int chatTypeCode, String peerUid) {
        ChatType t = ChatType.of(chatTypeCode);
        switch (t) {
            case GROUP:  return group(peerUid);
            case FRIEND: return friend(peerUid);
            default:
                throw new IllegalArgumentException(
                        "不支持的会话类型 code=" + chatTypeCode + "（peer=" + peerUid + "）");
        }
    }

    

    private volatile String cachedNick;
    private volatile boolean nickLoaded;

    public String nick() {
        if (nickLoaded) return cachedNick == null ? "" : cachedNick;
        synchronized (this) {
            if (nickLoaded) return cachedNick == null ? "" : cachedNick;
            String n = null;
            try {
                if (chatType() == ChatType.GROUP) {
                    n = null;   
                } else {
                    n = ContactApi.getBuddyNameByUid(uid());
                    if (isBlank(n)) n = ContactApi.getBuddyName(uin());
                }
            } catch (Throwable t) {
                com.cathelperawa.owo.app.core.HookLog.w("[CatPeer] nick 查询失败: " + t);
            }
            cachedNick = n == null ? "" : n;
            nickLoaded = true;
            return cachedNick;
        }
    }

    public boolean isFriend() {
        try {
            if (chatType() != ChatType.FRIEND) return false;
            String u = uin();
            return !isBlank(u) && QQUtils.isFriend(u);
        } catch (Throwable t) {
            return false;
        }
    }

    

    
    public Object raw() {
        return ContactFactory.make(chatType().code, uid(), null);
    }

    

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CatPeer)) return false;
        CatPeer p = (CatPeer) o;
        return chatType() == p.chatType()
                && str(uid()).equals(str(p.uid()));
    }

    @Override
    public int hashCode() {
        return chatType().code * 31 + str(uid()).hashCode();
    }

    @Override
    public String toString() {
        return chatType().name() + "(" + uid() + ")";
    }

    

    
    private static final class GroupPeer extends CatPeer {
        private final String g;
        GroupPeer(String g) { this.g = g; }
        @Override public ChatType chatType() { return ChatType.GROUP; }
        @Override public String uid() { return g; }
        @Override public String uin() { return g; }
    }

    private static final class FriendPeer extends CatPeer {
        private final String uid;   
        private final String uin;

        FriendPeer(String uinOrUid) {
            if (uinOrUid.startsWith("u_")) {
                this.uid = uinOrUid;
                String u = null;
                try {
                    u = QQUtils.uinFromUid(uinOrUid);
                } catch (Throwable t) {
                    com.cathelperawa.owo.app.core.HookLog.w("[CatPeer] uinFromUid 失败: " + t);
                }
                this.uin = u == null ? "" : u;
            } else {
                this.uin = uinOrUid;
                String u = null;
                try {
                    u = QQUtils.uidFromUin(uinOrUid);
                } catch (Throwable t) {
                    com.cathelperawa.owo.app.core.HookLog.w("[CatPeer] uidFromUin 失败: " + t);
                }
                
                this.uid = (u == null || u.isEmpty()) ? uinOrUid : u;
            }
        }

        @Override public ChatType chatType() { return ChatType.FRIEND; }
        @Override public String uid() { return uid; }
        @Override public String uin() { return uin; }
    }

    private static final class SelfPeer extends CatPeer {
        @Override public ChatType chatType() { return ChatType.FRIEND; }
        @Override public String uid() {
            try {
                String u = QQEnv.currentUid();
                return u == null ? "" : u;
            } catch (Throwable t) {
                return "";
            }
        }
        @Override public String uin() {
            try {
                String u = QQEnv.currentUin();
                return u == null ? "" : u;
            } catch (Throwable t) {
                return "";
            }
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static String str(String s) {
        return s == null ? "" : s;
    }
}
