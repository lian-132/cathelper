package com.cathelperawa.owo.app.api;

import android.content.Context;

import com.cathelperawa.owo.app.bridge.QQEnv;
import com.cathelperawa.owo.app.feature.FeatureMsgExtract;
import com.cathelperawa.owo.app.feature.FeatureMsfTap;
import com.cathelperawa.owo.app.feature.FeatureRedPacket;
import com.cathelperawa.owo.app.ovo.FunProtoData;
import com.cathelperawa.owo.app.ovo.QPacketHelper;
import com.cathelperawa.owo.app.ovo.QQInterfaces;
import com.cathelperawa.owo.app.protocol.MsgFetcher;
import com.cathelperawa.owo.app.proto.MsgGrab;
import com.cathelperawa.owo.app.proto.Pb;
import com.cathelperawa.owo.app.proto.PbMsg;
import com.cathelperawa.owo.app.proto.RepeatCore;
import com.cathelperawa.owo.app.util.CatLog;

import org.json.JSONObject;

import java.util.List;

public final class ProtoApi {

    private ProtoApi() {}

    public static Object kernelMsgService() { return QQEnv.kernelMsgService(); }

    public static Object appRuntime()        { return QQEnv.appRuntime(); }

    public static String fetchViaKernel(Object recordOrMsgItem, long msgId) {
        Object record = CatApi.unwrapRecord(recordOrMsgItem);
        if (record == null) return null;
        return FeatureMsgExtract.fetchViaKernel(record, msgId);
    }

    public static void sendProtocol(String cmd, byte[] payload) {
        QQInterfaces.sendBuffer(cmd, true, payload);
    }

    public static byte[] jsonToPb(String json) {
        return QPacketHelper.buildMessage(json);
    }

    public static String pbToJson(byte[] pb) {
        try {
            FunProtoData d = new FunProtoData();
            d.fromBytes(FunProtoData.getUnpPackage(pb));
            return d.toJSON().toString(2);
        } catch (Throwable t) {
            CatLog.e("pbToJson 失败", t);
            return null;
        }
    }

    public static void expectResponse(String cmd, FeatureMsfTap.Listener listener) {
        FeatureMsfTap.expect(cmd, listener);
    }

    public static void cancelExpect()             { FeatureMsfTap.cancel(); }

    public static String lastRawResponse()        { return FeatureMsfTap.lastRaw(); }

    public static String[] msfCmds()              { return FeatureMsfTap.CMDS.clone(); }

    public static String msfDecode(byte[] buf)    { return FeatureMsfTap.decode(buf); }

    public static Object msfExtractElements(JSONObject content) {
        return FeatureMsfTap.extractElements(content);
    }

    public static List<Object> collectElements(Object recordOrMsgItem) {
        Object r = CatApi.unwrapRecord(recordOrMsgItem);
        return r == null ? null : RepeatCore.collect(r);
    }

    public static List<Object> sendableElements(List<Object> els) {
        return RepeatCore.sendable(els);
    }

    public static String grabRawElements(Object recordOrMsgItem, int chatType) {
        Object r = CatApi.unwrapRecord(recordOrMsgItem);
        return r == null ? null : RepeatCore.grabRawElements(r, chatType);
    }

    public static String filterSendable(String rawJson) {
        return RepeatCore.filterSendable(rawJson);
    }

    public static String plainText(Object recordOrMsgItem) {
        Object r = CatApi.unwrapRecord(recordOrMsgItem);
        return r == null ? null : RepeatCore.plainText(r);
    }

    public static String jsonPrettyOf(Object o) {
        return FeatureMsgExtract.prettyJson(o);
    }

    public static Object walletOf(Object recordOrMsgItem) {
        Object r = CatApi.unwrapRecord(recordOrMsgItem);
        return r == null ? null : FeatureRedPacket.walletOf(r);
    }

    public static String pullMsg(Object recordOrMsgItem) {
        Object r = CatApi.unwrapRecord(recordOrMsgItem);
        return r == null ? null : MsgFetcher.pull(r);
    }

    public static String pullC2C(Object recordOrMsgItem) {
        Object r = CatApi.unwrapRecord(recordOrMsgItem);
        return r == null ? null : MsgFetcher.pullC2C(r);
    }

    public static String pullGroup(Object recordOrMsgItem) {
        Object r = CatApi.unwrapRecord(recordOrMsgItem);
        return r == null ? null : MsgFetcher.pullGroup(r);
    }

    public static String grabCode(Object recordOrMsgItem) {
        Object r = CatApi.unwrapRecord(recordOrMsgItem);
        if (r == null) return null;
        MsgGrab.Result res = MsgGrab.extract(r);
        return res == null ? null : res.toJson().toString();
    }

    public static String buildMultiArk(String xml, String resId, String fn) {
        return MsgGrab.buildMultiArk(xml, resId, fn);
    }

    public static String xmlUnescape(String s) {
        return MsgGrab.xmlUnescape(s);
    }

    public static byte[] pbMsgDirect(int chatType, String peerUin, byte[] elemsPB, String selfUin) {
        return PbMsg.buildDirect(chatType, peerUin, elemsPB, selfUin, null);
    }

    public static byte[] pbMsgRef(int chatType, String peerUin, String resId) {
        return PbMsg.buildRef(chatType, peerUin, resId, null);
    }

    public static byte[] pbMsgMulti(byte[] elemsPB) {
        return PbMsg.buildMultiMsg(elemsPB);
    }

    public static byte[] pbMsgUpload(int chatType, String peerUin, byte[] gz) {
        return PbMsg.buildUploadReq(chatType, peerUin, gz, null);
    }

    public static byte[] pbVint(long v)              { return Pb.vint(v); }
    public static byte[] pbGzip(byte[] data)         { return Pb.gzip(data); }
    public static List<JSONObject> pbElemList(String json) { return Pb.elemList(json); }

    public static boolean ovoSend(String elementsJson, String peer, boolean isGroup) {
        return QPacketHelper.sendMessage(elementsJson, peer, isGroup);
    }

    public static byte[] ovoBuild(String json) {
        return QPacketHelper.buildMessage(json);
    }

    public static boolean ovoSendBuffer(String cmd, byte[] body) {
        try {
            QQInterfaces.sendBuffer(cmd, false, body);
            return true;
        } catch (Throwable t) {
            CatLog.e("ovoSendBuffer 失败", t);
            return false;
        }
    }
}
