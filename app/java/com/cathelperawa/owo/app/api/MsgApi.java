package com.cathelperawa.owo.app.api;

import com.cathelperawa.owo.app.MsgStore;
import com.cathelperawa.owo.app.bridge.ContactFactory;
import com.cathelperawa.owo.app.bridge.ElementFactory;
import com.cathelperawa.owo.app.bridge.MsgSender;
import com.cathelperawa.owo.app.bridge.NtMsgApi;
import com.cathelperawa.owo.app.bridge.QQUtils;
import com.cathelperawa.owo.app.feature.FeatureMsgExtract;
import com.cathelperawa.owo.app.feature.FeatureRecorder;
import com.cathelperawa.owo.app.util.CatLog;
import com.cathelperawa.owo.app.util.Refl;

import java.util.List;

public final class MsgApi {

    private MsgApi() {}

    private static String applySendPoint(String text, String where) {
        try {
            if (!com.cathelperawa.owo.app.script.PatchPoint.has("host.send.text")) return text;
            com.cathelperawa.owo.app.script.PatchPoint.PatchCtx pc =
                    new com.cathelperawa.owo.app.script.PatchPoint.PatchCtx(
                            "host.send.text", text, where);
            com.cathelperawa.owo.app.script.PatchPoint.emit(pc);
            if (pc.cancelled()) {
                dbgPatch("CANCEL in=[" + text + "] where=" + where);
                return null;
            }
            dbgPatch("IN=[" + text + "] OUT=[" + pc.text() + "] where=" + where);
            return pc.text();
        } catch (Throwable t) {
            return text;
        }
    }

    public static boolean sendText(int chatType, String peerUid, String text) {
        text = applySendPoint(text, (chatType == 2 ? "group:" : "friend:") + peerUid);
        if (text == null) return false;
        return sendElements(chatType, peerUid,
                java.util.Collections.singletonList(ElementFactory.text(text)));
    }

    public static boolean sendCard(int chatType, String peerUid, String arkJson) {
        return sendElements(chatType, peerUid,
                java.util.Collections.singletonList(ElementFactory.ark(arkJson)));
    }

    public static boolean sendPic(int chatType, String peerUid, String path) {
        return sendElements(chatType, peerUid,
                java.util.Collections.singletonList(ElementFactory.pic(path)));
    }

    public static boolean sendPtt(int chatType, String peerUid, String path) {
        return sendElements(chatType, peerUid,
                java.util.Collections.singletonList(ElementFactory.ptt(path)));
    }

    public static boolean sendPtt(int chatType, String peerUid, String path, int durationMs) {
        return sendElements(chatType, peerUid,
                java.util.Collections.singletonList(ElementFactory.ptt(path, durationMs)));
    }

    public static boolean sendElements(int chatType, String peerUid, List<Object> elements) {
        Object contact = ContactFactory.make(chatType, peerUid, "");
        if (contact == null) { CatLog.w("sendElements: Contact 构造失败"); return false; }
        return MsgSender.send(contact, elements);
    }

    public static com.cathelperawa.owo.app.model.CatResult sendElementsResult(
            int chatType, String peerUid, List<Object> elements) {
        if (elements == null || elements.isEmpty()) {
            return com.cathelperawa.owo.app.model.CatResult.badArgs("元素列表为空");
        }
        Object contact = ContactFactory.make(chatType, peerUid, "");
        if (contact == null) {
            CatLog.w("sendElements: Contact 构造失败");
            return com.cathelperawa.owo.app.model.CatResult.hostMissing(
                    "ContactFactory.make(" + chatType + "," + peerUid + ")");
        }
        boolean ok = MsgSender.send(contact, elements);
        if (ok) {
            return com.cathelperawa.owo.app.model.CatResult.ok(
                    com.cathelperawa.owo.app.model.CatChannel.KERNEL);
        }
        return com.cathelperawa.owo.app.model.CatResult.rejected("内核 sendMsg 返回失败");
    }

    public static com.cathelperawa.owo.app.model.CatResult sendElementsResult(
            com.cathelperawa.owo.app.model.CatPeer peer, List<Object> elements) {
        if (peer == null) return com.cathelperawa.owo.app.model.CatResult.badArgs("peer 为空");
        return sendElementsResult(peer.chatType().code, peer.uid(), elements);
    }

    public static com.cathelperawa.owo.app.model.CatResult sendTextResult(
            int chatType, String peerUid, String text) {
        return sendElementsResult(chatType, peerUid,
                java.util.Collections.singletonList(ElementFactory.text(text)));
    }

    public static com.cathelperawa.owo.app.model.CatResult sendTextResult(
            com.cathelperawa.owo.app.model.CatPeer peer, String text) {
        if (peer == null) return com.cathelperawa.owo.app.model.CatResult.badArgs("peer 为空");
        return sendTextResult(peer.chatType().code, peer.uid(), text);
    }

    public static com.cathelperawa.owo.app.model.CatResult sendPicResult(
            int chatType, String peerUid, String path) {
        return sendElementsResult(chatType, peerUid,
                java.util.Collections.singletonList(ElementFactory.pic(path)));
    }

    public static com.cathelperawa.owo.app.model.CatResult sendCardResult(
            int chatType, String peerUid, String arkJson) {
        return sendElementsResult(chatType, peerUid,
                java.util.Collections.singletonList(ElementFactory.ark(arkJson)));
    }

    public static void extract(Object recordOrMsgItem) {
        Object record = CatApi.unwrapRecord(recordOrMsgItem);
        if (record == null) { CatLog.e("extract: 无法取到 MsgRecord", null); return; }
        FeatureMsgExtract.showExtract(record);
    }

    public static String lastExtractResult() { return FeatureMsgExtract.result(); }

    public static Object lastExtractRecord() { return FeatureMsgExtract.lastRecord(); }

    public static String textOf(Object recordOrMsgItem) {
        Object record = CatApi.unwrapRecord(recordOrMsgItem);
        return record == null ? "" : FeatureRecorder.extractText(record);
    }

    public static String cardOf(Object recordOrMsgItem) {
        Object record = CatApi.unwrapRecord(recordOrMsgItem);
        return record == null ? null : FeatureMsgExtract.extractCardJson(record);
    }

    public static String senderUin(Object recordOrMsgItem) {
        Object record = CatApi.unwrapRecord(recordOrMsgItem);
        return record == null ? null : Refl.str(Refl.field(record, "senderUin"));
    }

    public static String senderName(Object recordOrMsgItem) {
        Object record = CatApi.unwrapRecord(recordOrMsgItem);
        return record == null ? null : Refl.str(Refl.field(record, "sendNickName"));
    }

    public static long msgTime(Object recordOrMsgItem) {
        Object record = CatApi.unwrapRecord(recordOrMsgItem);
        return record == null ? 0 : Refl.lng(Refl.field(record, "msgTime"), 0);
    }

    public static int chatTypeOf(Object recordOrMsgItem) {
        Object record = CatApi.unwrapRecord(recordOrMsgItem);
        return record == null ? 0 : Refl.i32(Refl.field(record, "chatType"), 0);
    }

    public static String peerUidOf(Object recordOrMsgItem) {
        Object record = CatApi.unwrapRecord(recordOrMsgItem);
        return record == null ? null : Refl.str(Refl.field(record, "peerUid"));
    }

    public static long msgIdOf(Object recordOrMsgItem) {
        Object record = CatApi.unwrapRecord(recordOrMsgItem);
        return record == null ? 0 : Refl.lng(Refl.field(record, "msgId"), 0);
    }

    public static long msgSeqOf(Object recordOrMsgItem) {
        Object record = CatApi.unwrapRecord(recordOrMsgItem);
        return record == null ? 0 : Refl.lng(Refl.field(record, "msgSeq"), 0);
    }

    public static int capturedCount()                    { return MsgStore.all().size(); }
    public static List<String> capturedAll()             { return MsgStore.all(); }
    public static void capturedClear()                   { MsgStore.clear(); }
    public static java.util.Map<String, String> capturedPeers() { return MsgStore.peers(); }
    public static boolean isPeerAllowed(String uid)      { return MsgStore.allows(uid); }

    public static boolean recall(Object recordOrMsgItem) {
        Object r = CatApi.unwrapRecord(recordOrMsgItem);
        if (r == null) return false;
        return NtMsgApi.recall(chatTypeOf(r), peerUidOf(r), msgIdOf(r));
    }

    public static boolean recallMsg(int chatType, String peerUid, long msgId) {
        return NtMsgApi.recall(chatType, peerUid, msgId);
    }

    public static boolean recallSync(Object recordOrMsgItem, long timeoutMs) {
        Object r = CatApi.unwrapRecord(recordOrMsgItem);
        if (r == null) return false;
        Object contact = ContactFactory.make(chatTypeOf(r), peerUidOf(r), "");
        return contact != null && NtMsgApi.recallSync(contact, msgIdOf(r), timeoutMs);
    }

    public static int[] recallBatch(List<?> items, long gapMs) {
        int ok = 0, fail = 0;
        if (items == null) return new int[]{0, 0};
        for (Object it : items) {
            try {
                if (recallSync(it, 1500)) ok++; else fail++;
            } catch (Throwable t) {
                fail++;
            }
            if (gapMs > 0) {
                try { Thread.sleep(gapMs); } catch (Throwable ignore) {}
            }
        }
        return new int[]{ok, fail};
    }

    public static boolean deleteMsg(int chatType, String peerUid, long msgId) {
        Object c = ContactFactory.make(chatType, peerUid, "");
        return c != null && NtMsgApi.delete(c, msgId);
    }

    public static long generateMsgId(int type) { return NtMsgApi.generateMsgId(type); }

    public static Object getMsgsByMsgId(int chatType, String peerUid, long msgId) {
        Object c = ContactFactory.make(chatType, peerUid, "");
        return c == null ? null : NtMsgApi.getMsgsByMsgId(c, msgId);
    }

    public static Object getSingleMsg(int chatType, String peerUid, long msgSeq) {
        Object c = ContactFactory.make(chatType, peerUid, "");
        return c == null ? null : NtMsgApi.getSingleMsg(c, msgSeq);
    }

    public static boolean forward(long msgId, int srcType, String srcPeer,
                                  int dstType, String dstPeer) {
        Object src = ContactFactory.make(srcType, srcPeer, "");
        Object dst = ContactFactory.make(dstType, dstPeer, "");
        if (src == null || dst == null) return false;
        List<Long> ids = new java.util.ArrayList<Long>();
        ids.add(msgId);
        List<Object> dsts = new java.util.ArrayList<Object>();
        dsts.add(dst);
        return NtMsgApi.forward(ids, src, dsts);
    }

    public static boolean sendReply(int chatType, String peerUid, long replyMsgId, String text) {
        text = applySendPoint(text, (chatType == 2 ? "group:" : "friend:") + peerUid);
        if (text == null) return false;
        List<Object> els = new java.util.ArrayList<Object>();
        Object rp = ElementFactory.reply(replyMsgId);
        if (rp != null) els.add(rp);
        Object tx = ElementFactory.text(text);
        if (tx != null) els.add(tx);
        return sendElements(chatType, peerUid, els);
    }

    public static boolean sendVideo(int chatType, String peerUid, String path) {
        return sendElements(chatType, peerUid,
                java.util.Collections.singletonList(ElementFactory.video(path)));
    }

    public static boolean sendFile(int chatType, String peerUid, String path) {
        return sendElements(chatType, peerUid,
                java.util.Collections.singletonList(ElementFactory.file(path)));
    }

    public static boolean sendFace(int chatType, String peerUid, int faceId) {
        return sendElements(chatType, peerUid,
                java.util.Collections.singletonList(ElementFactory.face(faceId)));
    }

    public static boolean sendMixed(int chatType, String peerUid, String tagged) {
        List<Object> els = new java.util.ArrayList<Object>();
        if (tagged != null) {
            java.util.regex.Matcher m = java.util.regex.Pattern
                    .compile("\\[(atUin|pic|face)=([^\\]]*)\\]").matcher(tagged);
            int last = 0;
            while (m.find()) {
                if (m.start() > last) {
                    Object t = ElementFactory.text(tagged.substring(last, m.start()));
                    if (t != null) els.add(t);
                }
                String kind = m.group(1), v = m.group(2);
                Object el = null;
                if ("atUin".equals(kind)) {
                    String uin = QQUtils.uinFromUid(v);
                    el = ElementFactory.at(v, uin != null ? uin : v);
                    if (el == null) el = ElementFactory.text("@" + v);
                } else if ("pic".equals(kind)) {
                    el = ElementFactory.pic(v);
                } else if ("face".equals(kind)) {
                    try { el = ElementFactory.face(Integer.parseInt(v)); }
                    catch (Throwable t) { CatLog.d("sendMixed: face 解析失败 " + v); }
                }
                if (el != null) els.add(el);
                last = m.end();
            }
            if (last < tagged.length()) {
                Object t = ElementFactory.text(tagged.substring(last));
                if (t != null) els.add(t);
            }
        }
        if (els.isEmpty()) return false;
        return sendElements(chatType, peerUid, els);
    }

    public static boolean sendEmojiLike(int chatType, String peerUid, long msgSeq,
                                        String emojiId, boolean add) {
        try {
            org.json.JSONObject inner = new org.json.JSONObject();
            org.json.JSONObject e1 = new org.json.JSONObject();
            e1.put("1", emojiId);
            inner.put("1", e1);
            org.json.JSONObject pb = new org.json.JSONObject();
            pb.put("1", 1);
            pb.put("2", 0);
            pb.put("3", add ? 1 : 0);
            org.json.JSONObject f2 = new org.json.JSONObject();
            f2.put("1", inner);
            f2.put("2", new org.json.JSONArray());
            pb.put("2", f2);
            org.json.JSONObject el = new org.json.JSONObject();
            el.put("53", pb);

            org.json.JSONArray arr = new org.json.JSONArray();
            arr.put(el);
            org.json.JSONObject base = new org.json.JSONObject();
            org.json.JSONObject t1 = new org.json.JSONObject();
            if (chatType == 2) t1.put("2", Long.parseLong(peerUid));
            else t1.put("1", peerUid);
            base.put("1", t1);
            org.json.JSONObject t2 = new org.json.JSONObject();
            t2.put("1", 1); t2.put("2", 0); t2.put("3", 0);
            base.put("2", t2);
            org.json.JSONObject in3 = new org.json.JSONObject();
            in3.put("2", arr);
            org.json.JSONObject t3 = new org.json.JSONObject();
            t3.put("1", in3);
            base.put("3", t3);
            base.put("4", (long) (Math.random() * 4294967296L) & 0xFFFFFFFFL);
            base.put("5", (long) (Math.random() * 4294967296L) & 0xFFFFFFFFL);

            byte[] payload = ProtoApi.jsonToPb(base.toString());
            if (payload == null || payload.length == 0) return false;
            ProtoApi.sendProtocol("MessageSvc.PbSendMsg", payload);
            return true;
        } catch (Throwable t) {
            CatLog.e("sendEmojiLike 失败", t);
            return false;
        }
    }

    private static void dbgPatch(String s) {
        try {
            java.io.File d = com.cathelperawa.owo.app.core.DataDir.global();
            if (d == null) return;
            if (!d.exists()) d.mkdirs();
            java.io.File f = new java.io.File(d, "patch_debug.txt");
            if (f.length() > 200 * 1024) f.delete();
            java.io.FileOutputStream fo = new java.io.FileOutputStream(f, true);
            fo.write((new java.text.SimpleDateFormat("HH:mm:ss")
                    .format(new java.util.Date()) + " " + s + "\n").getBytes("UTF-8"));
            fo.flush();
            fo.close();
        } catch (Throwable ignore) {
        }
    }
}
