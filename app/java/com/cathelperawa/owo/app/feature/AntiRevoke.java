package com.cathelperawa.owo.app.feature;

import com.cathelperawa.owo.app.bridge.ContactApi;
import com.cathelperawa.owo.app.bridge.GrayTip;
import com.cathelperawa.owo.app.bridge.QQEnv;
import com.cathelperawa.owo.app.bridge.QQUtils;
import com.cathelperawa.owo.app.bridge.TroopApi;
import com.cathelperawa.owo.app.core.HookApi;
import com.cathelperawa.owo.app.core.HookItem;
import com.cathelperawa.owo.app.core.HookItemBase;
import com.cathelperawa.owo.app.core.HookLog;
import com.google.protobuf.ByteString;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;

import top.artmoe.inao.entries.InfoSyncPushOuterClass$InfoSyncPush;
import top.artmoe.inao.entries.MsgPushOuterClass$MsgPush;
import top.artmoe.inao.entries.QQMessageOuterClass$QQMessage;

@HookItem(name = "防撤回", path = "聊天与消息/防撤回",
        description = "拦截撤回推送，保留消息并灰字提示", order = 20)
public class AntiRevoke extends HookItemBase {

    private static final boolean EMPTY_PACK = true;

    private static final String CMD_MSG_PUSH  = "trpc.msg.olpush.OlPushService.MsgPush";
    private static final String CMD_INFO_SYNC = "trpc.msg.register_proxy.RegisterProxy.InfoSyncPush";

    @Override
    public String switchKey() { return "chat_revoke"; }

    @Override
    public void entry(ClassLoader cl) {
        hint("收到消息推送时");
        Class<?> cpp = HookApi.findClass(cl,
                "com.tencent.qqnt.kernel.nativeinterface.IQQNTWrapperSession$CppProxy");
        if (cpp == null) { fail("防撤回：未找到 CppProxy"); return; }

        int n = 0;
        for (final Method m : cpp.getDeclaredMethods()) {
            if (!"onMsfPush".equals(m.getName())) continue;
            hookBefore(m, new HookApi.Action() {
                @Override
                public void call(XC_MethodHook.MethodHookParam p) {

                    com.cathelperawa.owo.app.bridge.QQEnv.setWrapperSession(p.thisObject);
                    onPush(p);
                }
            });
            n++;
        }

        try {
            de.robv.android.xposed.XposedBridge.hookAllConstructors(cpp, new XC_MethodHook() {
                @Override protected void afterHookedMethod(MethodHookParam p) {
                    com.cathelperawa.owo.app.bridge.QQEnv.setWrapperSession(p.thisObject);
                }
            });
        } catch (Throwable ignore) {}
        HookLog.i("防撤回：已挂 onMsfPush × " + n);
        com.cathelperawa.owo.app.MsgStore.add("[防撤回] 已挂 onMsfPush × " + n);
    }

    @Override
    protected void check(com.cathelperawa.owo.app.core.SelfCheckResult.Checker c) {
        Class<?> cpp = c.cls("CppProxy",
                "com.tencent.qqnt.kernel.nativeinterface.IQQNTWrapperSession$CppProxy");
        c.method("onMsfPush", cpp, "onMsfPush");

        c.assertThat("撤回命令常量已配置",
                CMD_MSG_PUSH != null && CMD_MSG_PUSH.length() > 0, CMD_MSG_PUSH);
    }

    private void onPush(XC_MethodHook.MethodHookParam p) {
        try {
            if (!isEnabled()) return;
            Object[] a = p.args;
            if (a == null || a.length < 2) return;
            if (!(a[0] instanceof String) || !(a[1] instanceof byte[])) return;
            String cmd = (String) a[0];
            byte[] payload = (byte[]) a[1];

            if (CMD_MSG_PUSH.equals(cmd)) {
                byte[] np = rewriteMsgPush(payload);
                if (np != null) { a[1] = np; HookLog.i("防撤回：MsgPush 已拦截"); com.cathelperawa.owo.app.MsgStore.add("[防撤回] 已拦截一条撤回推送"); fire("拦下撤回"); }
            } else if (CMD_INFO_SYNC.equals(cmd)) {
                byte[] np = rewriteInfoSync(payload);
                if (np != null) { a[1] = np; HookLog.i("防撤回：InfoSyncPush 已拦截"); com.cathelperawa.owo.app.MsgStore.add("[防撤回] 已拦截一条多端同步撤回"); fire("多端同步撤回"); }
            }
        } catch (Throwable t) {
            HookLog.e("防撤回 onPush 异常", t);
        }
    }

    private byte[] rewriteMsgPush(byte[] payload) throws Exception {
        MsgPushOuterClass$MsgPush push = MsgPushOuterClass$MsgPush.parseFrom(payload);
        QQMessageOuterClass$QQMessage qq = push.getQqMessage();

        int type = qq.getMessageContentInfo().getMsgType();
        int sub  = qq.getMessageContentInfo().getMsgSubType();
        boolean group = (type == 732 && sub == 17);
        boolean c2c   = (type == 528 && sub == 138);
        if (!group && !c2c) return null;

        byte[] op = qq.getMessageBody().getOperationInfo().toByteArray();
        byte[] newOp = group ? rewriteGroupOp(op, qq) : rewriteC2COp(op, qq);
        if (newOp == null) return null;

        if (EMPTY_PACK && !group) return new byte[0];

        MsgPushOuterClass$MsgPush.Builder pb = push.toBuilder();
        QQMessageOuterClass$QQMessage.Builder qb = pb.getQqMessage().toBuilder();
        QQMessageOuterClass$QQMessage.MessageBody.Builder mb = qb.getMessageBody().toBuilder();
        mb.setOperationInfo(ByteString.copyFrom(newOp));
        qb.setMessageBody(mb.build());
        pb.setQqMessage(qb.build());
        return pb.build().toByteArray();
    }

    private byte[] rewriteGroupOp(byte[] op, QQMessageOuterClass$QQMessage qq) throws Exception {
        if (op == null || op.length <= 7) return null;
        byte[] head = Arrays.copyOfRange(op, 0, 7);
        QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo info =
                QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo
                        .parseFrom(Arrays.copyOfRange(op, 7, op.length));

        int msgSeq = info.getMsgSeq();
        int msgSeqInner = info.getInfo().getMsgInfo().getMsgSeq();
        String peerId = String.valueOf(info.getPeerId());
        String operatorUid = info.getInfo().getOperatorUid();
        String senderUid = info.getInfo().getMsgInfo().getSenderUid();
        HookLog.i("防撤回[群] peer=" + peerId + " seq=" + msgSeq
                + " op=" + operatorUid + " sender=" + senderUid);
        com.cathelperawa.owo.app.MsgStore.add("[防撤回] 群 " + peerId
                + " seq=" + msgSeq + "（内层" + msgSeqInner + "）");

        byte[] out = new byte[0];
        if (!EMPTY_PACK) {
            QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo.Builder b = info.toBuilder();
            b.setMsgSeq(1);
            QQMessageOuterClass$QQMessage.MessageBody.GroupRecallOperationInfo.Info.Builder ib =
                    b.getInfo().toBuilder();
            ib.setMsgInfo(ib.getMsgInfo().toBuilder().setMsgSeq(1).build());
            b.setInfo(ib.build());
            byte[] body = b.build().toByteArray();
            out = new byte[head.length + body.length];
            System.arraycopy(head, 0, out, 0, head.length);
            System.arraycopy(body, 0, out, head.length, body.length);
        }

        final String gp = peerId, ou = operatorUid, su = senderUid;
        final int ms = msgSeq;
        String selfUidForGroup = uidFromUinSafe(QQEnv.currentUin());
        HookLog.i("防撤回[群] 新逻辑 self=" + selfUidForGroup + " sender=" + su
                + (su != null && su.equals(selfUidForGroup) ? " 【自己撤自己的→静默】" : " 【撤别人的→发灰字】"));
        com.cathelperawa.owo.app.MsgStore.add("[防撤回] 群撤回 peer=" + gp
                + " 撤回者本人=" + (su != null && su.equals(selfUidForGroup) ? "是" : "否"));
        if (su != null && su.equals(selfUidForGroup)) {
            com.cathelperawa.owo.app.MsgStore.add("[防撤回] 群·自己撤回自己的 → 不发灰字");
        } else {
        new Thread(new Runnable() { @Override public void run() {
            String[] op = resolveName(ou, gp);
            String[] sd = resolveName(su, gp);

            int realSeq = EMPTY_PACK ? ms : resolveRealSeq(2, gp, ms);
            HookLog.i("防撤回灰字 op=" + op[0] + "/" + ou + " uin=" + op[1] + " sender=" + sd[0] + "/" + su
                    + " seq=" + ms + "→" + realSeq);
            String tip = GrayTip.buildRecallTip(op[0], ou, op[1], sd[0], su, sd[1], realSeq);
            if (tip != null) GrayTip.add(2, gp, tip);
        } }).start();
        }
        return EMPTY_PACK ? new byte[0] : out;
    }

    private static String uidFromUinSafe(String uin) {
        if (uin == null || uin.trim().isEmpty()) return null;
        try { return QQUtils.uidFromUin(uin); } catch (Throwable t) { return null; }
    }

    private int resolveRealSeq(int chatType, String peerUid, int msgSeq) {
        return resolveRealSeq(chatType, peerUid, msgSeq, 0);
    }

    private int resolveRealSeq(int chatType, String peerUid, int msgSeq, long msgId) {
        try {
            Object contact = com.cathelperawa.owo.app.bridge.ContactFactory.make(chatType, peerUid, "");
            if (contact == null) return 0;
            Object record = null;
            if (msgSeq > 0) {
                record = com.cathelperawa.owo.app.bridge.NtMsgApi.getSingleMsg(contact, msgSeq);
            }
            if (record == null && msgId > 0) {
                record = com.cathelperawa.owo.app.bridge.NtMsgApi.getMsgsByMsgId(contact, msgId);
                if (record != null) {
                    HookLog.d("[防撤回] 按 msgId=" + msgId + " 反查命中");
                } else {
                    HookLog.d("[防撤回] getMsgsByMsgId 返回空");
                }
            }

            if (record == null) {
                try {

                    List<?> recent = com.cathelperawa.owo.app.bridge.NtMsgApi
                            .getMsgsBySeqAndCount(contact, Long.MAX_VALUE, 40);
                    if (recent == null || recent.isEmpty()) {
                        recent = com.cathelperawa.owo.app.bridge.NtMsgApi
                                .getMsgsBySeqAndCount(contact, 0, 40);
                    }
                    HookLog.d("[防撤回] 兜底取最近消息 size="
                            + (recent == null ? "null" : recent.size()));
                    if (recent != null) {
                        StringBuilder list = new StringBuilder("[防撤回] 最近消息: ");
                        for (Object r : recent) {
                            long rid = com.cathelperawa.owo.app.util.Refl.lng(
                                    com.cathelperawa.owo.app.util.Refl.field(r, "msgId"), 0);
                            int rsq = com.cathelperawa.owo.app.util.Refl.i32(
                                    com.cathelperawa.owo.app.util.Refl.field(r, "msgSeq"), 0);
                            long rtm = com.cathelperawa.owo.app.util.Refl.lng(
                                    com.cathelperawa.owo.app.util.Refl.field(r, "msgTime"), 0);
                            long rnd = com.cathelperawa.owo.app.util.Refl.lng(
                                    com.cathelperawa.owo.app.util.Refl.field(r, "msgRandom"), 0);
                            list.append("{id=").append(rid).append(",seq=").append(rsq)
                                .append(",t=").append(rtm).append(",rnd=").append(rnd).append("} ");
                            if (record == null && rnd > 0 && (rnd & 0xFFFFFFFFL) == (msgId & 0xFFFFFFFFL)) {
                                record = r;
                                com.cathelperawa.owo.app.core.HookLog.d("[防撤回] 按 random 命中 seq=" + rsq);
                            }
                        }
                        HookLog.d(list.toString());
                    }
                } catch (Throwable t) {
                    com.cathelperawa.owo.app.core.HookLog.d("[防撤回] 兜底异常 " + t);
                }
            }
            if (record == null) {
                HookLog.i("防撤回: 本地查不到 seq=" + msgSeq + " msgId=" + msgId + " → 不发可点引用");
                com.cathelperawa.owo.app.core.HookLog.d("[防撤回] 查不到 seq=" + msgSeq + " msgId=" + msgId);
                return 0;
            }

            int msgSeqV  = com.cathelperawa.owo.app.util.Refl.i32(com.cathelperawa.owo.app.util.Refl.field(record, "msgSeq"), 0);
            int clientSeq = com.cathelperawa.owo.app.util.Refl.i32(com.cathelperawa.owo.app.util.Refl.field(record, "clientSeq"), 0);
            long recMsgId = com.cathelperawa.owo.app.util.Refl.lng(com.cathelperawa.owo.app.util.Refl.field(record, "msgId"), 0);
            long msgTime = com.cathelperawa.owo.app.util.Refl.lng(com.cathelperawa.owo.app.util.Refl.field(record, "msgTime"), 0);
            int chatTypeV = com.cathelperawa.owo.app.util.Refl.i32(com.cathelperawa.owo.app.util.Refl.field(record, "chatType"), 0);
            HookLog.i("防撤回诊断: 协议seq=" + msgSeq + " | 记录 msgSeq=" + msgSeqV
                    + " clientSeq=" + clientSeq + " recMsgId=" + recMsgId
                    + " msgTime=" + msgTime + " chatType=" + chatTypeV);

            StringBuilder dump = new StringBuilder("[防撤回记录] type="
                    + record.getClass().getName() + " | ");
            java.lang.reflect.Field[] fsAll = record.getClass().getFields();
            if (fsAll.length == 0) fsAll = record.getClass().getDeclaredFields();
            for (java.lang.reflect.Field f : fsAll) {
                f.setAccessible(true);
                try {
                    Class<?> t = f.getType();
                    if (t == long.class || t == int.class || t == java.lang.String.class) {
                        dump.append(f.getName()).append('=').append(f.get(record)).append(' ');
                    }
                } catch (Throwable ignore) {}
            }
            HookLog.i(dump.toString());

            if (msgSeqV > 0) return msgSeqV;
            if (clientSeq > 0) return clientSeq;
            return 0;
        } catch (Throwable t) {
            HookLog.w("防撤回: resolveRealSeq 失败 " + t);
            return 0;
        }
    }

    private byte[] rewriteC2COp(byte[] op, QQMessageOuterClass$QQMessage qq) throws Exception {
        if (op == null || op.length == 0) return null;
        QQMessageOuterClass$QQMessage.MessageBody.C2CRecallOperationInfo info =
                QQMessageOuterClass$QQMessage.MessageBody.C2CRecallOperationInfo.parseFrom(op);

        long outer2 = extractFieldVarint(op, 2, 0);
        outer2 = outerTopLevel(op, 2);
        long outer3 = outerTopLevel(op, 3);
        HookLog.d("[防撤回] 私聊外层 #2=" + outer2 + " #3=" + outer3);
        long f5 = extractFieldVarint(op, 1, 5);
        long f6 = extractFieldVarint(op, 1, 6);
        long f4 = extractFieldVarint(op, 1, 4);

        final long realMsgId = f4 > 0 ? f4 : ((f5 << 32) | (f6 & 0xFFFFFFFFL));
        int msgSeq = info.getInfo().getMsgSeq();
        String operatorUid = info.getInfo().getOperatorUid();
        HookLog.i("防撤回[私] seq=" + msgSeq + " msgId=" + realMsgId + " op=" + operatorUid);

        QQMessageOuterClass$QQMessage.MessageBody.C2CRecallOperationInfo.Builder b = info.toBuilder();
        QQMessageOuterClass$QQMessage.MessageBody.C2CRecallOperationInfo.Info.Builder ib =
                b.getInfo().toBuilder();
        if (!EMPTY_PACK) {
            ib.setMsgSeq(1);
            b.setInfo(ib.build());
        }

        final String ou = operatorUid;
        final String receiver = info.getInfo().getReceiverUid();
        final String selfUid = QQUtils.uidFromUin(QQEnv.currentUin());

        final String peerForQuery =
                (selfUid != null && selfUid.equals(ou)) ? receiver
                : (ou != null && !ou.isEmpty() ? ou : receiver);
        final int ms = msgSeq;
        final long msgTimeFromPkt = f5;

        if (peerForQuery == null || peerForQuery.isEmpty()
                || (selfUid != null && selfUid.equals(ou))) {
            com.cathelperawa.owo.app.MsgStore.add("[防撤回] 私聊·自己撤回自己的 → 不发灰字");
            return new byte[0];
        }
        Thread th = new Thread(new Runnable() { @Override public void run() {
            try {
                String[] op = resolveName(ou, null);

                int realSeq = (peerForQuery == null || peerForQuery.isEmpty()) ? 0
                        : findSeqByTime(1, peerForQuery, msgTimeFromPkt);
                HookLog.i("防撤回灰字 op=" + op[0] + "/" + ou + " peer=" + peerForQuery
                        + " seq=" + ms + "→" + realSeq);
                com.cathelperawa.owo.app.MsgStore.add("[防撤回] 对方 " + op[0]
                        + " 撤回，seq=" + realSeq);
                String tip = GrayTip.buildRecallTip(op[0], ou, op[1], null, null, null, realSeq);
                if (tip != null) GrayTip.add(1, peerForQuery, tip);
            } catch (Throwable t) {
                HookLog.e("防撤回私聊异常", t);
            }
        } }, "cat-anti-revoke");
        th.start();
        return EMPTY_PACK ? new byte[0] : b.build().toByteArray();
    }

    private long extractFieldVarint(byte[] data, int outer, int inner) {
        try {
            int i = 0;
            while (i < data.length) {
                long key = 0; int shift = 0;
                while (true) {
                    int b = data[i++] & 0xff;
                    key |= (long) (b & 0x7f) << shift;
                    if ((b & 0x80) == 0) break;
                    shift += 7;
                }
                int field = (int) (key >>> 3);
                int wt = (int) (key & 7);
                if (field == outer && wt == 2) {
                    long len = 0; shift = 0;
                    while (true) {
                        int b = data[i++] & 0xff;
                        len |= (long) (b & 0x7f) << shift;
                        if ((b & 0x80) == 0) break;
                        shift += 7;
                    }
                    return scanVarint(data, i, (int) len, inner);
                }
                int skip = skipField(data, i, wt);
                if (skip < 0) return 0;
                i = skip;
            }
        } catch (Throwable t) {
            HookLog.w("extractFieldVarint 失败 " + t);
        }
        return 0;
    }

    private int findSeqByTime(int chatType, String peerUid, long pktTime) {
        if (pktTime <= 0) return 0;
        try {
            Object contact = com.cathelperawa.owo.app.bridge.ContactFactory.make(chatType, peerUid, "");
            if (contact == null) return 0;
            java.util.List<?> recent = com.cathelperawa.owo.app.bridge.NtMsgApi
                    .getMsgsBySeqAndCount(contact, Long.MAX_VALUE, 60);
            if (recent == null || recent.isEmpty()) return 0;
            int best = 0;
            long bestDiff = Long.MAX_VALUE;
            for (Object r : recent) {
                long t = com.cathelperawa.owo.app.util.Refl.lng(
                        com.cathelperawa.owo.app.util.Refl.field(r, "msgTime"), 0);
                int sq = com.cathelperawa.owo.app.util.Refl.i32(
                        com.cathelperawa.owo.app.util.Refl.field(r, "msgSeq"), 0);
                if (sq <= 0) continue;
                long diff = Math.abs(t - pktTime);

                if (diff < bestDiff && diff <= 60) { bestDiff = diff; best = sq; }
            }
            com.cathelperawa.owo.app.core.HookLog.d("[防撤回] 最近消息 " + recent.size()
                    + " 条，最佳匹配 seq=" + best + " 误差=" + bestDiff + "s");

            StringBuilder dbg = new StringBuilder("[防撤回] 列表: ");
            int cnt = 0;
            for (Object r : recent) {
                if (cnt++ >= 8) break;
                long t = com.cathelperawa.owo.app.util.Refl.lng(
                        com.cathelperawa.owo.app.util.Refl.field(r, "msgTime"), -1);
                int sq = com.cathelperawa.owo.app.util.Refl.i32(
                        com.cathelperawa.owo.app.util.Refl.field(r, "msgSeq"), -1);
                long ts = com.cathelperawa.owo.app.util.Refl.lng(
                        com.cathelperawa.owo.app.util.Refl.field(r, "timeStamp"), -1);
                dbg.append("{t=").append(t).append(",ts=").append(ts).append(",seq=").append(sq).append("} ");
            }
            HookLog.d(dbg.toString());
            return best;
        } catch (Throwable t) {
            com.cathelperawa.owo.app.core.HookLog.d("[防撤回] findSeqByTime 异常 " + t);
            return 0;
        }
    }

    private long outerTopLevel(byte[] d, int target) {
        try {
            int i = 0;
            while (i < d.length) {
                long k = 0; int sh = 0;
                while (i < d.length) {
                    int b = d[i++] & 0xff;
                    k |= (long) (b & 0x7f) << sh;
                    if ((b & 0x80) == 0) break;
                    sh += 7;
                }
                int f = (int) (k >>> 3), wt = (int) (k & 7);
                if (wt == 0) {
                    long v = 0; sh = 0;
                    while (i < d.length) {
                        int b = d[i++] & 0xff;
                        v |= (long) (b & 0x7f) << sh;
                        if ((b & 0x80) == 0) break;
                        sh += 7;
                    }
                    if (f == target) return v;
                } else {
                    int nx = skipField(d, i, wt);
                    if (nx < 0) return 0;
                    i = nx;
                }
            }
        } catch (Throwable ignore) { }
        return 0;
    }

    private long scanVarint(byte[] d, int off, int len, int target) {
        int i = off, end = off + len;
        while (i < end) {
            long k = 0; int sh = 0;
            while (i < end) {
                int b = d[i++] & 0xff;
                k |= (long) (b & 0x7f) << sh;
                if ((b & 0x80) == 0) break;
                sh += 7;
            }
            int f = (int) (k >>> 3), wt = (int) (k & 7);
            if (wt == 0) {
                long v = 0; sh = 0;
                while (i < end) {
                    int b = d[i++] & 0xff;
                    v |= (long) (b & 0x7f) << sh;
                    if ((b & 0x80) == 0) break;
                    sh += 7;
                }
                if (f == target) return v;
            } else {
                int nx = skipField(d, i, wt);
                if (nx < 0 || nx > end) return 0;
                i = nx;
            }
        }
        return 0;
    }

    private int skipField(byte[] d, int i, int wt) {
        try {
            if (wt == 2) {
                long len = 0; int sh = 0;
                while (true) {
                    int b = d[i++] & 0xff;
                    len |= (long) (b & 0x7f) << sh;
                    if ((b & 0x80) == 0) break;
                    sh += 7;
                }
                return (int) (i + len);
            } else if (wt == 0) {
                while ((d[i++] & 0x80) != 0) { }
                return i;
            } else if (wt == 5) {
                return i + 4;
            } else if (wt == 1) {
                return i + 8;
            }
        } catch (Throwable ignore) { }
        return -1;
    }

    private String[] resolveName(String uid, String troopUin) {
        if (uid == null || uid.isEmpty()) return new String[]{"对方", ""};
        try {
            String selfUid = QQUtils.uidFromUin(QQEnv.currentUin());
            if (uid.equals(selfUid)) return new String[]{"你", QQUtils.uinFromUid(uid)};
            String uin = QQUtils.uinFromUid(uid);

            if (troopUin != null && uin != null && !uin.isEmpty()) {
                String n = TroopApi.getMemberName(troopUin, uin);
                if (n != null && !n.isEmpty()) return new String[]{n, uin};
            }

            String buddy = (uin != null && !uin.isEmpty())
                    ? ContactApi.getBuddyName(uin)
                    : ContactApi.getBuddyNameByUid(uid);
            if (buddy != null && !buddy.isEmpty()) return new String[]{buddy, uin == null ? "" : uin};

            String cached = nameFromStore(uid);
            if (cached == null) cached = nameFromStore(uin);
            if (cached != null) return new String[]{cached, uin == null ? "" : uin};
            return new String[]{"对方", uin == null ? "" : uin};
        } catch (Throwable ignore) {
            return new String[]{"对方", ""};
        }
    }

    private static String nameFromStore(String key) {
        if (key == null || key.isEmpty()) return null;
        try {
            java.util.Map<String, String> all = com.cathelperawa.owo.app.MsgStore.peers();

            String v = all.get(key);

            if (v == null) {
                for (java.util.Map.Entry<String, String> e : all.entrySet()) {
                    if (e.getKey() != null && e.getKey().contains(key)) { v = e.getValue(); break; }
                }
            }
            if (v == null || v.isEmpty()) return null;
            int i = v.lastIndexOf(']');
            String name = (i >= 0 ? v.substring(i + 1) : v).trim();

            if (name.isEmpty() || name.startsWith("u_") || name.matches("\\d+")) return null;
            return name;
        } catch (Throwable t) {
            return null;
        }
    }

    private byte[] rewriteInfoSync(byte[] payload) throws Exception {
        InfoSyncPushOuterClass$InfoSyncPush push = InfoSyncPushOuterClass$InfoSyncPush.parseFrom(payload);
        InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo rc = push.getSyncRecallContent();
        int cnt = rc.getSyncInfoBodyCount();
        if (cnt <= 0) return null;

        boolean hit = false;
        for (int i = 0; i < cnt && !hit; i++) {
            for (QQMessageOuterClass$QQMessage msg : rc.getSyncInfoBody(i).getMsgList()) {
                int t = msg.getMessageContentInfo().getMsgType();
                int s = msg.getMessageContentInfo().getMsgSubType();
                if ((t == 732 && s == 17) || (t == 528 && s == 138)) { hit = true; break; }
            }
        }
        if (!hit) return null;

        InfoSyncPushOuterClass$InfoSyncPush.Builder pb = push.toBuilder();
        InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.Builder rb =
                pb.getSyncRecallContent().toBuilder();
        for (int i = 0; i < rb.getSyncInfoBodyCount(); i++) {
            InfoSyncPushOuterClass$InfoSyncPush.SyncRecallOperateInfo.SyncInfoBody.Builder bb =
                    rb.getSyncInfoBody(i).toBuilder();
            bb.clearMsg();
            rb.setSyncInfoBody(i, bb.build());
        }
        pb.setSyncRecallContent(rb.build());
        return pb.build().toByteArray();
    }
}
