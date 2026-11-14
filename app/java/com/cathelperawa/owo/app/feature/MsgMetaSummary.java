package com.cathelperawa.owo.app.feature;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;

final class MsgMetaSummary {

    private MsgMetaSummary() {}

    private static Object call(Object o, String name) {
        if (o == null) return null;
        try {
            Method m = o.getClass().getMethod(name);
            m.setAccessible(true);
            return m.invoke(o);
        } catch (Throwable t) {
            return null;
        }
    }

    private static String s(Object o, String name) {
        Object v = call(o, name);
        if (v == null) return "";
        String t = String.valueOf(v);
        return t.equals("null") ? "" : t;
    }

    private static long l(Object o, String name) {
        Object v = call(o, name);
        if (v instanceof Number) return ((Number) v).longValue();
        return -1;
    }

    private static String ts(long sec) {
        if (sec <= 0) return "";
        try {
            java.text.SimpleDateFormat f =
                    new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US);
            return f.format(new java.util.Date(sec * 1000L));
        } catch (Throwable t) {
            return String.valueOf(sec);
        }
    }

    private static String chatTypeName(long v) {
        switch ((int) v) {
            case 1: return "私聊";
            case 2: return "群聊";
            case 3: return "临时会话";
            default: return "未知(" + v + ")";
        }
    }

    private static String msgTypeName(long v) {
        switch ((int) v) {
            case 1:  return "系统提示";
            case 2:  return "文本消息";
            case 3:  return "图片消息";
            case 4:  return "文件消息";
            case 5:  return "语音消息";
            case 6:  return "视频消息";
            case 7:  return "表情消息";
            case 9:  return "引用/回复";
            case 10: return "合并转发";
            case 11: return "小程序";
            default: return "类型" + v;
        }
    }

    private static String elementTypeName(long v) {
        switch ((int) v) {
            case 0: case 1: return "文本";
            case 2: return "图片";
            case 3: return "文件";
            case 4: return "语音";
            case 5: return "视频";
            case 6: return "表情";
            case 7: return "引用";
            case 9: return "卡片(ark)";
            case 10: return "合并转发";
            case 11: return "Markdown";
            default: return "元素" + v;
        }
    }

    static String build(Object rec) {
        if (rec == null) {
            return "【关键信息】\n（没有消息对象。这条消息走的是协议 JSON，没有落到 Java 对象上）\n";
        }
        StringBuilder b = new StringBuilder();
        b.append("【关键信息】\n──────────────────\n");

        String nick = s(rec, "getSendNickName");
        String remark = s(rec, "getSendRemarkName");
        String member = s(rec, "getSendMemberName");
        String uin = s(rec, "getSenderUin");
        String uid = s(rec, "getSenderUid");
        long sendType = l(rec, "getSendType");

        b.append("◆ 发送者\n");
        b.append("  昵称：").append(nick.isEmpty() ? "（无）" : nick).append("\n");
        if (!remark.isEmpty()) b.append("  备注：").append(remark).append("\n");
        if (!member.isEmpty()) b.append("  群名片：").append(member).append("\n");
        b.append("  QQ 号：").append(uin.isEmpty() ? "-" : uin).append("\n");
        b.append("  内部 UID：").append(uid.isEmpty() ? "-" : uid).append("\n");
        b.append("  方向：").append(sendType == 1 ? "我自己发的" : "别人发的").append("\n\n");

        b.append("◆ 会话\n");
        long chatType = l(rec, "getChatType");
        b.append("  类型：").append(chatType > 0 ? chatTypeName(chatType) : "-").append("\n");
        String peerName = s(rec, "getPeerName");
        String peerUin = s(rec, "getPeerUin");
        if (!peerName.isEmpty() || !peerUin.isEmpty()) {
            b.append("  对方：").append(peerName.isEmpty() ? peerUin : peerName);
            if (!peerName.isEmpty() && !peerUin.isEmpty()) b.append("（").append(peerUin).append("）");
            b.append("\n");
        }
        b.append("\n");

        b.append("◆ 消息\n");
        long msgType = l(rec, "getMsgType");
        long subType = l(rec, "getSubMsgType");
        b.append("  类型：").append(msgType > 0 ? msgTypeName(msgType) : "-")
                .append("  (msgType=").append(msgType).append(", subMsgType=").append(subType).append(")\n");
        b.append("  时间：").append(ts(l(rec, "getMsgTime"))).append("\n");
        b.append("  消息 ID：").append(l(rec, "getMsgId")).append("\n");
        b.append("  序号：").append(l(rec, "getMsgSeq")).append("\n");
        b.append("  随机数：").append(l(rec, "getMsgRandom")).append("\n");
        long editable = call(rec, "getEditable") instanceof Boolean
                ? (((Boolean) call(rec, "getEditable")) ? 1 : 0) : -1;
        if (editable >= 0) b.append("  可编辑：").append(editable == 1 ? "能" : "不能").append("\n");

        Object els = call(rec, "getElements");
        if (els instanceof Collection) {
            Collection<?> col = (Collection<?>) els;
            b.append("  元素（").append(col.size()).append(" 个）：\n");
            int i = 0;
            for (Object e : col) {
                long et = l(e, "getElementType");
                String desc = elementTypeName(et);
                Object txt = call(e, "getTextElement");
                if (txt != null) {
                    String content = s(txt, "getContent");
                    if (!content.isEmpty()) {
                        if (content.length() > 60) content = content.substring(0, 60) + "…";
                        desc += "：「" + content + "」";
                    }
                }
                Object ark = call(e, "getArkElement");
                if (ark != null) desc += "（卡片数据 " + s(ark, "getBytesData").length() + " 字节）";
                b.append("    ").append(++i).append(". ").append(desc)
                        .append("  [elementType=").append(et).append("]\n");
            }
        }
        b.append("\n");

        Object attrs = call(rec, "getMsgAttrs");
        if (attrs instanceof Map) {
            StringBuilder vas = new StringBuilder();
            for (Object v : ((Map<?, ?>) attrs).values()) {
                Object vm = call(v, "getVasMsgInfo");
                if (vm == null) continue;
                Object bubble = call(vm, "getBubbleInfo");
                long bubbleId = bubble == null ? 0 : l(bubble, "getBubbleId");
                if (bubbleId > 0) vas.append("  气泡 ID：").append(bubbleId).append("\n");
                Object font = call(vm, "getVasFont");
                long fontId = font == null ? 0 : l(font, "getFontId");
                if (fontId > 0) vas.append("  字体 ID：").append(fontId).append("\n");
                Object plate = call(vm, "getMsgNamePlateInfo");
                if (plate != null) {
                    long pt = l(plate, "getNamePlateType");
                    long vipType = l(plate, "getMsgVipType");
                    long vipLv = l(plate, "getMsgVipLevel");
                    if (pt > 0) vas.append("  名片类型：").append(pt).append("\n");
                    if (vipType > 0 || vipLv > 0) {
                        vas.append("  VIP：类型 ").append(vipType)
                                .append(" · 等级 ").append(vipLv).append("\n");
                    }
                }
                Object pend = call(vm, "getAvatarPendantInfo");
                long pendId = pend == null ? 0 : l(pend, "getPendantId");
                if (pendId > 0) vas.append("  头像挂件：").append(pendId).append("\n");
                Object ice = call(vm, "getIceBreakInfo");
                if (ice != null && Boolean.TRUE.equals(call(ice, "getIsIceBreakMsg"))) {
                    vas.append("  破冰消息：是\n");
                }
            }
            if (vas.length() > 0) {
                b.append("◆ 装扮 / 增值（对方花钱买的那套）\n").append(vas).append("\n");
            }
        }

        Object records = call(rec, "getRecords");
        if (records instanceof Collection) {
            int n = ((Collection<?>) records).size();
            b.append("◆ 嵌套记录\n");
            b.append("  ").append(n == 0 ? "无（不是合并转发）" : (n + " 条合并转发内容")).append("\n\n");
        }

        b.append("──────────────────\n");
        return b.toString();
    }
}
