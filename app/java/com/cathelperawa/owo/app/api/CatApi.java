package com.cathelperawa.owo.app.api;

import android.app.Activity;
import android.content.Context;

import com.cathelperawa.owo.app.util.Refl;

import java.util.List;

public final class CatApi {

    private CatApi() {}

    @Deprecated
    public static String catify(String input) {
        return CatifyApi.process(input);
    }

    @Deprecated
    public static boolean catifyEnabled() {
        return CatifyApi.enabled();
    }

    @Deprecated
    public static Context ctx()           { return IdentityApi.appContext(); }

    @Deprecated
    public static String currentUin()     { return IdentityApi.currentUin(); }

    @Deprecated
    public static Activity topActivity()  { return IdentityApi.topActivity(); }

    @Deprecated
    public static ClassLoader classLoader() { return IdentityApi.classLoader(); }

    @Deprecated
    public static boolean isCaptureEnabled() { return ConfigApi.isCaptureEnabled(); }

    @Deprecated
    public static void setCaptureEnabled(boolean on) { ConfigApi.setCaptureEnabled(on); }

    @Deprecated
    public static boolean isAutoCaptureEnabled() { return ConfigApi.isAutoCaptureEnabled(); }

    @Deprecated
    public static void setAutoCaptureEnabled(boolean on) { ConfigApi.setAutoCaptureEnabled(on); }

    @Deprecated
    public static boolean isCatifyTotalEnabled() { return ConfigApi.isCatifyTotalEnabled(); }

    @Deprecated
    public static void setCatifyTotalEnabled(boolean on) { ConfigApi.setCatifyTotalEnabled(on); }

    @Deprecated
    public static boolean isFeatureEnabled(String key) { return ConfigApi.isFeatureEnabled(key); }

    @Deprecated
    public static void setFeatureEnabled(String key, boolean on) { ConfigApi.setFeatureEnabled(key, on); }

    @Deprecated
    public static String cfgGet(String key, String def) { return ConfigApi.get(key, def); }

    @Deprecated
    public static void cfgSet(String key, String value) { ConfigApi.set(key, value); }

    @Deprecated
    public static boolean cfgGetBool(String key, boolean def) { return ConfigApi.getBool(key, def); }

    @Deprecated
    public static void cfgSetBool(String key, boolean val) { ConfigApi.setBool(key, val); }

    @Deprecated
    public static void log(String msg)      { LogApi.info(msg); }

    @Deprecated
    public static void logErr(String m, Throwable t) { LogApi.error(m, t); }

    @Deprecated
    public static void logWarn(String msg)  { LogApi.warn(msg); }

    @Deprecated
    public static void logDebug(String msg) { LogApi.debug(msg); }

    static Object unwrapRecord(Object o) {
        if (o == null) return null;
        try {
            if (Refl.field(o, "elements") != null) return o;
        } catch (Throwable ignore) {}
        try {
            java.lang.reflect.Method g = o.getClass().getMethod("getMsgRecord");
            g.setAccessible(true);
            Object r = g.invoke(o);
            if (r != null) return r;
        } catch (Throwable ignore) {}
        return null;
    }

    @Deprecated
    public static com.cathelperawa.owo.app.proto.RepeatCore.Outcome repeat(Object recordOrMsgItem) {
        return RepeatApi.repeat(recordOrMsgItem);
    }

    @Deprecated
    public static String elementKind(Object el) { return RepeatApi.elementKind(el); }

    @Deprecated
    public static String grab(Object recordOrMsgItem) { return RepeatApi.grab(recordOrMsgItem); }

    @Deprecated
    public static String sendCode(String contentJson, int chatType, String peerUid) {
        return RepeatApi.sendCode(contentJson, chatType, peerUid);
    }

    @Deprecated
    public static boolean sendLongMsg(String contentJson, int chatType, String peerUid) {
        return RepeatApi.sendLongMsg(contentJson, chatType, peerUid);
    }

    @Deprecated
    public static boolean sendDirectPb(String contentJson, int chatType, String peerUid) {
        return RepeatApi.sendDirectPb(contentJson, chatType, peerUid);
    }

    @Deprecated
    public static boolean sendRawCmd(String cmd, byte[] body) {
        return RepeatApi.sendRawCmd(cmd, body);
    }

    @Deprecated
    public static String lastResId() { return RepeatApi.lastResId(); }

    @Deprecated
    public static byte[] elemJsonToPb(String json) { return PbApi.elemJsonToPb(json); }

    @Deprecated
    public static byte[] pbLd(int field, byte[] data) { return PbApi.ld(field, data); }

    @Deprecated
    public static byte[] pbVf(int field, long v) { return PbApi.vf(field, v); }

    @Deprecated
    public static byte[] pbStr(int field, String s) { return PbApi.strField(field, s); }

    @Deprecated
    public static byte[] pbCat(byte[]... arrs) { return PbApi.cat(arrs); }

    static String strOf(Object v) {
        return v == null ? null : String.valueOf(v);
    }

    public static com.cathelperawa.owo.app.model.CatResult sendText(
            com.cathelperawa.owo.app.model.CatPeer peer, String text) {
        return MsgApi.sendTextResult(peer, text);
    }

    public static boolean sendText(int chatType, String peerUid, String text) {
        return com.cathelperawa.owo.app.api.MsgApi.sendText(chatType, peerUid, text);
    }

    public static boolean sendCard(int chatType, String peerUid, String arkJson) {
        return com.cathelperawa.owo.app.api.MsgApi.sendCard(chatType, peerUid, arkJson);
    }

public static com.cathelperawa.owo.app.model.CatResult sendElements(
            com.cathelperawa.owo.app.model.CatPeer peer, List<Object> elements) {
        return MsgApi.sendElementsResult(peer, elements);
    }

    public static com.cathelperawa.owo.app.model.CatPeer peer(int chatType, String peerUid) {
        try {
            return com.cathelperawa.owo.app.model.CatPeer.of(chatType, peerUid);
        } catch (Throwable t) {
            com.cathelperawa.owo.app.core.HookLog.w("[API] peer 解析失败: " + t);
            return null;
        }
    }

    public static com.cathelperawa.owo.app.model.CatMsg msg(Object recordOrMsgItem) {
        return com.cathelperawa.owo.app.model.CatMsg.of(recordOrMsgItem);
    }

    public static com.cathelperawa.owo.app.model.ChatType chatTypeOf(int chatType) {
        return com.cathelperawa.owo.app.model.ChatType.of(chatType);
    }

    public static boolean mixOn() {
        return ChatMixApi.isOn();
    }

    public static boolean mixAlways() {
        return ChatMixApi.isAlwaysMode();
    }

    public static void mixAlways(boolean on) {
        ChatMixApi.setAlwaysMode(on);
    }

    public static boolean mixInsertPic(String path) {
        return ChatMixApi.insertPic(path);
    }

    public static boolean mixInsertPic(String path, boolean isGif) {
        return ChatMixApi.insertPic(path, isGif);
    }

    public static boolean mixInsertText(String text) {
        return ChatMixApi.insertInput(text);
    }

    public static String mixInputText() {
        return ChatMixApi.inputText();
    }

    public static int mixPicCount() {
        return ChatMixApi.inputPicCount();
    }

    public static boolean mixClear() {
        return ChatMixApi.clearInput();
    }

    public static boolean mixFullScreen() {
        return ChatMixApi.openFullScreen();
    }
}
