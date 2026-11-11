package com.cathelperawa.owo.app.protocol;

import com.cathelperawa.owo.app.api.IdentityApi;
import com.cathelperawa.owo.app.util.Refl;

import java.util.List;

public final class ReplyInfo {

    private ReplyInfo() {}

    public static final class Info {
        public boolean present;         
        public String origUid;          
        public String origName;         
        public boolean origIsSelf;      
        public String origKind = "";    
        public String origText = "";    
        public boolean hasMedia;        
    }

    public static Info of(Object record, boolean group, String peer) {
        Info info = new Info();
        if (record == null) return info;
        Object els = Refl.field(record, "elements");
        if (!(els instanceof List)) return info;

        Object reply = null;
        for (Object el : (List<?>) els) {
            if (el == null) continue;
            Object r = Refl.field(el, "replyElement");
            if (r != null) { reply = r; break; }
        }
        if (reply == null) return info;
        info.present = true;

        info.origUid = Refl.firstStr(reply, "senderUidStr", "senderUid");
        info.origName = nameOfUid(info.origUid, group, peer);
        info.origIsSelf = isSelf(info.origUid);
        String st = Refl.firstStr(reply, "sourceMsgText");
        info.origText = (st == null) ? "" : st.trim();

        applyRecords(record, info, 0);

        
        if (info.origKind.length() == 0 || info.origText.length() == 0) {
            long srcId = Refl.fieldLong(reply, "sourceMsgIdInRecords", 0);
            Object orig = MsgCache.get(srcId);
            if (orig != null) applyMessage(orig, info);
        }

        if (info.origKind.length() == 0) {
            String kind = absKindOf(reply);
            if (kind != null) {
                info.origKind = kind;
                info.hasMedia = !"文字".equals(kind);
            }
        }
        
        if (info.origKind == null) info.origKind = "";
        if (info.origText == null) info.origText = "";
        return info;
    }

    private static void applyMessage(Object msg, Info info) {
        if (msg == null) return;
        Object els = Refl.field(msg, "elements");
        if (!(els instanceof List)) return;
        for (Object el : (List<?>) els) {
            if (el == null) continue;
            String k = kindOfElement(el);
            if (k == null) continue;
            if (info.origKind.length() == 0) {
                info.origKind = k;
                info.hasMedia = !"文字".equals(k) && !"表情".equals(k);
            }
            if ("文字".equals(k) && info.origText.length() == 0) {
                Object te = Refl.field(el, "textElement");
                String t = Refl.firstStr(te, "content");
                if (t != null) info.origText = t.trim();
            }
            return;
        }
    }

    private static void applyRecords(Object record, Info info, int depth) {
        if (record == null || depth > 1) return;
        Object recs = Refl.field(record, "records");
        if (!(recs instanceof List)) return;
        for (Object r : (List<?>) recs) {
            if (r == null) continue;
            Object els = Refl.field(r, "elements");
            if (els instanceof List) {
                boolean got = false;
                for (Object el : (List<?>) els) {
                    if (el == null) continue;
                    String k = kindOfElement(el);
                    if (k == null) continue;
                    info.origKind = k;
                    info.hasMedia = !"文字".equals(k) && !"表情".equals(k);
                    got = true;
                    if ("文字".equals(k)) {
                        Object te = Refl.field(el, "textElement");
                        String t = Refl.firstStr(te, "content");
                        if (t != null && t.trim().length() > 0) info.origText = t.trim();
                    }
                    break;
                }
                if (got) {
                    
                    String uid = Refl.firstStr(r, "senderUid");
                    if (uid != null && uid.length() > 0) {
                        info.origUid = uid;
                        info.origIsSelf = isSelf(uid);
                    }
                    return;
                }
            }
            applyRecords(r, info, depth + 1);
            if (info.origKind.length() > 0) return;
        }
    }

    private static String kindOfElement(Object el) {
        if (Refl.field(el, "picElement") != null) return "图片";
        if (Refl.field(el, "pttElement") != null) return "语音";
        if (Refl.field(el, "marketFaceElement") != null) return "表情";
        if (Refl.field(el, "videoElement") != null) return "视频";
        if (Refl.field(el, "fileElement") != null) return "文件";
        if (Refl.field(el, "faceElement") != null) return "表情";
        if (Refl.field(el, "textElement") != null) return "文字";
        return null;
    }

    private static String absKindOf(Object reply) {
        Object elems = Refl.field(reply, "sourceMsgTextElems");
        if (!(elems instanceof List)) return null;
        for (Object e : (List<?>) elems) {
            if (e == null) continue;
            Object pic = Refl.field(e, "picElem");
            if (pic != null) return "图片";
            Object ptt = Refl.field(e, "pttElem");
            if (ptt != null) return "语音";
            int t = Refl.fieldInt(e, "replyAbsElemType", 0);
            switch (t) {
                case 1: return "文字";
                case 2: return "表情";
                case 3: return "图片";
                case 4: return "语音";
                case 5: return "视频";
                case 6: return "文件";
                default: break;
            }
        }
        return null;
    }

    public static String describe(Info info, String replierName, boolean mediaAttached) {
        if (info == null || !info.present) return "";
        StringBuilder sb = new StringBuilder("【引用】「");
        sb.append(replierName == null || replierName.length() == 0 ? "对方" : replierName);
        sb.append("」引用了「");
        sb.append(info.origIsSelf ? "你自己" : (info.origName == null ? "对方" : info.origName));
        sb.append("」发的");
        String kind = info.origKind.length() == 0 ? "一条消息" : ("一条" + info.origKind + "消息");
        sb.append(kind);

        if (info.origText != null && info.origText.length() > 0
                && !info.origText.equals(info.origKind)) {
            String t = info.origText.trim();
            if (t.length() > 80) t = t.substring(0, 80) + "…";
            sb.append("，内容是「").append(t).append("」");
        }

        if (info.hasMedia) {
            if (mediaAttached) {
                sb.append("。原图/原语音我已经一并附上，直接看/听就行，别再说看不到");
            } else {
                sb.append("。原图/原语音这次没取到——看不到就直说看不到，别猜内容");
            }
        }
        return sb.toString();
    }

    private static boolean isSelf(String uid) {
        try {
            String self = IdentityApi.currentUid();
            return self != null && self.equals(uid);
        } catch (Throwable t) {
            return false;
        }
    }

    private static String nameOfUid(String uid, boolean group, String peer) {
        if (uid == null || uid.length() == 0) return null;
        try {
            if (isSelf(uid)) return "你";
            String uin = IdentityApi.uinFromUid(uid);
            if (uin != null && uin.length() > 0) {
                if (group) {
                    String n = IdentityApi.troopMemberName(peer, uin);
                    if (n != null && n.trim().length() > 0) return n;
                }
                String n2 = IdentityApi.buddyNameByUid(uid);
                if (n2 != null && n2.trim().length() > 0) return n2;
                return uin;
            }
        } catch (Throwable ignore) {
        }
        return null;
    }
}
