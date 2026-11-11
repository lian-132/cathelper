package com.cathelperawa.owo.app.bridge;

import com.cathelperawa.owo.app.util.Refl;

public final class CurrentChat {

    private CurrentChat() {
    }

    private static volatile int chatType = 0;       
    private static volatile String peerUid = "";    
    private static volatile String peerName = "";
    
    private static volatile String peerRaw = "";
    private static volatile long updatedAt = 0L;

    public static void set(int ct, String uid, String name) {
        if (uid == null || uid.length() == 0) return;
        if (ct != 1 && ct != 2) return;
        chatType = ct;
        peerUid = uid;
        if (name != null && name.length() > 0) peerName = name;
        updatedAt = System.currentTimeMillis();
    }

    public static void fromRecord(Object recordOrItem) {
        try {
            Object rec = findRecord(recordOrItem);
            if (rec == null) return;
            int ct = Refl.i32(Refl.field(rec, "chatType"), 0);
            String uid = Refl.str(Refl.field(rec, "peerUid"));
            if (uid == null || uid.length() == 0) uid = Refl.str(Refl.field(rec, "peerUin"));
            String name = Refl.str(Refl.field(rec, "peerName"));
            if (uid != null && uid.length() > 0) {
                
                if (ct != 1 && ct != 2) ct = uid.startsWith("u_") ? 1 : 2;
                set(ct, uid, name);
            }
        } catch (Throwable ignore) {
        }
    }

    private static Object findRecord(Object item) {
        if (item == null) return null;
        Class<?> cls = item.getClass();
        int depth = 0;
        while (cls != null && depth <= 4) {
            for (java.lang.reflect.Field f : cls.getDeclaredFields()) {
                if (!f.getType().getName().endsWith("MsgRecord")) continue;
                try {
                    f.setAccessible(true);
                    Object v = f.get(item);
                    if (v != null) return v;
                } catch (Throwable ignore) {
                }
            }
            cls = cls.getSuperclass();
            depth++;
        }
        
        Object rec = Refl.field(item, "data");
        if (rec == null) rec = Refl.field(item, "msgRecord");
        return rec != null ? rec : item;
    }

    public static boolean fromActivity(android.app.Activity act) {
        try {
            if (act == null || act.getIntent() == null) return false;
            android.os.Bundle b = act.getIntent().getExtras();
            if (b == null) return false;

            int ct = 0;
            Object ctObj = b.get("key_chat_type");
            if (ctObj instanceof Number) ct = ((Number) ctObj).intValue();

            String uid = null;
            Object idObj = b.get("key_peerId");
            if (idObj instanceof String) uid = (String) idObj;
            if (uid == null || uid.length() == 0) {
                Object uinObj = b.get("key_peerUin");
                if (uinObj != null && !"0".equals(String.valueOf(uinObj))) {
                    uid = String.valueOf(uinObj);
                }
            }
            String name = null;
            Object nObj = b.get("key_chat_name");
            if (nObj instanceof String) name = (String) nObj;

            if (uid == null || uid.length() == 0) return false;
            if (ct != 1 && ct != 2) ct = uid.startsWith("u_") ? 1 : 2;
            
            peerRaw = uid;
            if (ct == 1 && uid.matches("[0-9]+")) {
                String real = uinToUid(uid);
                if (real != null && real.length() > 0) uid = real;
            }
            set(ct, uid, name);
            return has();
        } catch (Throwable t) {
            return false;
        }
    }

    private static String uinToUid(String uin) {
        try {
            String u = QQUtils.uidFromUin(uin);
            if (u != null && u.length() > 0) return u;
        } catch (Throwable ignore) {
        }
        return null;
    }

    public static boolean has() {
        return chatType != 0 && peerUid != null && peerUid.length() > 0;
    }

    public static int chatType() {
        return chatType;
    }

    public static String peerUid() {
        return peerUid;
    }

    public static String peerRaw() {
        return peerRaw == null ? "" : peerRaw;
    }

    public static String peerName() {
        return peerName;
    }

    public static long updatedAt() {
        return updatedAt;
    }

    public static String describe() {
        if (!has()) return "未识别当前会话";
        String kind = chatType == 2 ? "群聊" : "好友";
        String n = peerName == null || peerName.length() == 0 ? "" : (peerName + " · ");
        return n + kind + " · " + shortOf(peerUid);
    }

    private static String shortOf(String s) {
        if (s == null) return "";
        if (s.length() <= 12) return s;
        return s.substring(0, 6) + "…" + s.substring(s.length() - 4);
    }
}
