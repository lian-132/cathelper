package com.cathelperawa.owo.app.api;

import android.app.Activity;
import com.cathelperawa.owo.app.core.Bg;
import android.content.Context;

import com.cathelperawa.owo.app.util.Refl;

import java.util.List;

public final class CatApi {

    private CatApi() {}

    public static String aiAsk(String text) { return AiApi.ask(text); }

    public static String aiAsk(String peerUid, String text) { return AiApi.ask(peerUid, text); }

    public static String aiSay(int chatType, String peerUid, String prompt) {
        return AiApi.say(chatType, peerUid, prompt);
    }

    public static boolean aiEmoji(int chatType, String peerUid, String label) {
        return AiApi.sendEmoji(chatType, peerUid, label);
    }

    public static String aiDescribeImage(String path) { return AiApi.describeImage(path); }

    public static boolean sendElementAuto(String elementsJson) {
        return SendProtoApi.sendElementAuto(elementsJson);
    }

    public static boolean sendLongAuto(String elementsJson) {
        return SendProtoApi.sendLongAuto(elementsJson);
    }

    public static boolean sendRaw(String cmd, byte[] body) {
        return SendProtoApi.sendRaw(cmd, body);
    }

    public static boolean sendRawHex(String cmd, String hex) {
        return SendProtoApi.sendRawHex(cmd, hex);
    }

    public static String pbBytesToJson(byte[] raw) {
        return SendProtoApi.decodeBytes(raw);
    }

    public static String hexToJson(String hex) {
        return SendProtoApi.decodeHex(hex);
    }

    public static byte[] jsonToPbBytes(String json) {
        return SendProtoApi.encodeJson(json);
    }

    public static String jsonToHex(String json) {
        return SendProtoApi.encodeJsonHex(json);
    }

    public static boolean refreshChat() {
        return SendProtoApi.refreshChat();
    }

    public static String chatDesc() {
        return SendProtoApi.chatDesc();
    }

    public static int chatType() {
        return SendProtoApi.chatType();
    }

    public static String chatPeerUid() {
        return SendProtoApi.peerUid();
    }

    public static String templatesJson() {
        return SendProtoApi.templatesJson();
    }

    public static String templateContent(String name) {
        return SendProtoApi.templateContent(name);
    }

    public static boolean saveTemplate(String name, int mode, String content) {
        return SendProtoApi.saveTemplate(name, mode, content);
    }

    public static boolean removeTemplate(String name) {
        return SendProtoApi.removeTemplate(name);
    }

    public static boolean renameTemplate(String oldName, String newName) {
        return SendProtoApi.renameTemplate(oldName, newName);
    }

    public static String exportTemplates() {
        return SendProtoApi.exportTemplates();
    }

    public static boolean forceLong() {
        return SendProtoApi.forceLong();
    }

    public static void setForceLong(boolean on) {
        SendProtoApi.setForceLong(on);
    }

    @Deprecated

    public static boolean sendText(int chatType, String peerUid, String text) {
        return MsgApi.sendText(chatType, peerUid, text);
    }

    @Deprecated

    public static boolean sendPic(int chatType, String peerUid, String path) {
        return MsgApi.sendPic(chatType, peerUid, path);
    }

    @Deprecated

    public static boolean sendPtt(int chatType, String peerUid, String path) {
        return MsgApi.sendPtt(chatType, peerUid, path);
    }

    @Deprecated

    public static boolean sendElements(int chatType, String peerUid, List<Object> elements) {
        return MsgApi.sendElements(chatType, peerUid, elements);
    }

    @Deprecated
    public static void extract(Object recordOrMsgItem) { MsgApi.extract(recordOrMsgItem); }

    @Deprecated
    public static String lastExtractResult() { return MsgApi.lastExtractResult(); }

    @Deprecated
    public static Object lastExtractRecord() { return MsgApi.lastExtractRecord(); }

    @Deprecated
    public static String textOf(Object recordOrMsgItem) { return MsgApi.textOf(recordOrMsgItem); }

    @Deprecated
    public static String cardOf(Object recordOrMsgItem) { return MsgApi.cardOf(recordOrMsgItem); }

    @Deprecated
    public static String senderUin(Object recordOrMsgItem) { return MsgApi.senderUin(recordOrMsgItem); }

    @Deprecated
    public static String senderName(Object recordOrMsgItem) { return MsgApi.senderName(recordOrMsgItem); }

    @Deprecated
    public static long msgTime(Object recordOrMsgItem) { return MsgApi.msgTime(recordOrMsgItem); }

    @Deprecated
    public static int chatTypeOf(Object recordOrMsgItem) { return MsgApi.chatTypeOf(recordOrMsgItem); }

    @Deprecated
    public static String peerUidOf(Object recordOrMsgItem) { return MsgApi.peerUidOf(recordOrMsgItem); }

    @Deprecated
    public static long msgIdOf(Object recordOrMsgItem) { return MsgApi.msgIdOf(recordOrMsgItem); }

    @Deprecated
    public static long msgSeqOf(Object recordOrMsgItem) { return MsgApi.msgSeqOf(recordOrMsgItem); }

    @Deprecated
    public static int capturedCount()  { return MsgApi.capturedCount(); }

    @Deprecated
    public static List<String> capturedAll() { return MsgApi.capturedAll(); }

    @Deprecated
    public static void capturedClear()  { MsgApi.capturedClear(); }

    @Deprecated
    public static java.util.Map<String, String> capturedPeers() { return MsgApi.capturedPeers(); }

    @Deprecated
    public static boolean isPeerAllowed(String uid) { return MsgApi.isPeerAllowed(uid); }

    @Deprecated
    public static boolean recall(Object recordOrMsgItem) { return MsgApi.recall(recordOrMsgItem); }

    @Deprecated

    public static boolean recallMsg(int chatType, String peerUid, long msgId) {
        return MsgApi.recallMsg(chatType, peerUid, msgId);
    }

    @Deprecated

    public static boolean deleteMsg(int chatType, String peerUid, long msgId) {
        return MsgApi.deleteMsg(chatType, peerUid, msgId);
    }

    @Deprecated
    public static long generateMsgId(int type) { return MsgApi.generateMsgId(type); }

    @Deprecated

    public static Object getMsgsByMsgId(int chatType, String peerUid, long msgId) {
        return MsgApi.getMsgsByMsgId(chatType, peerUid, msgId);
    }

    @Deprecated
    public static boolean forward(long msgId, int srcType, String srcPeer,
                                  int dstType, String dstPeer) {
        return MsgApi.forward(msgId, srcType, srcPeer, dstType, dstPeer);
    }

    @Deprecated

    public static boolean sendReply(int chatType, String peerUid, long replyMsgId, String text) {
        return MsgApi.sendReply(chatType, peerUid, replyMsgId, text);
    }

    @Deprecated

    public static boolean sendVideo(int chatType, String peerUid, String path) {
        return MsgApi.sendVideo(chatType, peerUid, path);
    }

    @Deprecated

    public static boolean sendFile(int chatType, String peerUid, String path) {
        return MsgApi.sendFile(chatType, peerUid, path);
    }

    @Deprecated

    public static boolean sendFace(int chatType, String peerUid, int faceId) {
        return MsgApi.sendFace(chatType, peerUid, faceId);
    }

    @Deprecated

    public static boolean sendMixed(int chatType, String peerUid, String tagged) {
        return MsgApi.sendMixed(chatType, peerUid, tagged);
    }

    @Deprecated
    public static boolean sendEmojiLike(int chatType, String peerUid, long msgSeq,
                                        String emojiId, boolean add) {
        return MsgApi.sendEmojiLike(chatType, peerUid, msgSeq, emojiId, add);
    }

    @Deprecated
    public static Object elText(String text) { return ElementApi.text(text); }

    @Deprecated
    public static Object elPic(String path) { return ElementApi.pic(path); }

    @Deprecated
    public static Object elPtt(String path) { return ElementApi.ptt(path); }

    @Deprecated
    public static Object elFace(int faceId) { return ElementApi.face(faceId); }

    @Deprecated
    public static Object elVideo(String path) { return ElementApi.video(path); }

    @Deprecated
    public static Object elFile(String path) { return ElementApi.file(path); }

    @Deprecated
    public static Object elAt(String uin, String display) { return ElementApi.at(uin, display); }

    @Deprecated
    public static Object elReply(long replyMsgId) { return ElementApi.reply(replyMsgId); }

    @Deprecated
    public static Object contact(int chatType, String peerUid) { return ElementApi.contact(chatType, peerUid); }

    @Deprecated

    public static boolean ghostSend(String text) {
        return GhostApi.send(text);
    }

    @Deprecated

    public static boolean ghostSend(String text, String fakeUid) {
        return GhostApi.send(text, fakeUid);
    }

    @Deprecated

    public static boolean ghostLog(String tag, String body) {
        return GhostApi.log(tag, body);
    }

    @Deprecated

    public static String ghostUid() {
        return GhostApi.uid();
    }

    @Deprecated

    public static String tts2Silk(String text, String outPath) {
        return VoiceApi.tts2Silk(text, outPath);
    }

    @Deprecated
    public static String voiceUpload(String slk, String json) { return VoiceApi.upload(slk, json); }

    @Deprecated
    public static boolean voiceUploadOk(String slk, String json) { return VoiceApi.uploadOk(slk, json); }

    @Deprecated
    public static String voiceSend(String text, String json) { return VoiceApi.send(text, json); }

    @Deprecated
    public static String buddyName(String uin) { return IdentityApi.buddyName(uin); }

    @Deprecated
    public static String buddyNameByUid(String uid) { return IdentityApi.buddyNameByUid(uid); }

    @Deprecated
    public static String uidFromUin(String uin) { return IdentityApi.uidFromUin(uin); }

    @Deprecated
    public static String uinFromUid(String uid) { return IdentityApi.uinFromUid(uid); }

    @Deprecated
    public static boolean isFriend(String uin) { return IdentityApi.isFriend(uin); }

    @Deprecated
    public static String troopName(String troopUin) { return IdentityApi.troopName(troopUin); }

    @Deprecated

    public static java.util.Map<String, String> allTroopNames() {
        return IdentityApi.allTroopNames();
    }

    @Deprecated

    public static java.util.Map<String, String> allBuddies() {
        return IdentityApi.allBuddies();
    }

    @Deprecated
    public static String currentNick() { return IdentityApi.currentNick(); }

    @Deprecated
    public static String currentUid() { return IdentityApi.currentUid(); }

    @Deprecated

    public static Object msgServiceImpl() {
        return IdentityApi.msgServiceImpl();
    }

    @Deprecated

    public static Object nativeMsgService() {
        return IdentityApi.nativeMsgService();
    }

    @Deprecated

    public static Object wrapperSession() {
        return IdentityApi.wrapperSession();
    }

    @Deprecated

    public static boolean hbSend(String cgiName, String url, java.util.Map<String, String> params) {
        return RedPacketApi.send(cgiName, url, params);
    }

    @Deprecated
    public static boolean hbSend(String cgiName, String url,
                                 java.util.Map<String, String> params, int attempt) {
        return RedPacketApi.send(cgiName, url, params, attempt);
    }

    @Deprecated
    public static int hbTryCount() { return RedPacketApi.tryCount(); }

    @Deprecated
    public static boolean hbReady() { return RedPacketApi.ready(); }

    @Deprecated
    public static void redPacketSink(com.cathelperawa.owo.app.bridge.TimHb.Sink s) { RedPacketApi.sink(s); }

    @Deprecated
    public static boolean netRequest(String url, java.util.Map<String, String> params,
                                     com.cathelperawa.owo.app.bridge.TimNet.Sink sink) {
        return RedPacketApi.netRequest(url, params, sink);
    }

    @Deprecated
    public static boolean netReady() { return RedPacketApi.netReady(); }

    @Deprecated
    public static void netDumpStructures() { RedPacketApi.netDumpStructures(); }

    @Deprecated

    public static boolean grayTip(int chatType, String peerUid, String json) {
        return GrayTipApi.add(chatType, peerUid, json);
    }

    @Deprecated

    public static boolean grayTipText(int chatType, String peerUid, String text) {
        return GrayTipApi.text(chatType, peerUid, text);
    }

    @Deprecated
    public static boolean grayTipTextRef(int chatType, String peerUid,
                                         String text, String refText, int msgSeq) {
        return GrayTipApi.textRef(chatType, peerUid, text, refText, msgSeq);
    }

    @Deprecated
    public static String grayTipJson(String text) { return GrayTipApi.json(text); }

    @Deprecated

    public static boolean grayTipContact(int busiId, Object contact, String json) {
        return GrayTipApi.toContact(busiId, contact, json);
    }

    @Deprecated
    public static String tipText(String txt) { return GrayTipApi.itemText(txt); }

    @Deprecated

    public static String tipUser(String nick, String uid, String uin) {
        return GrayTipApi.itemUser(nick, uid, uin);
    }

    @Deprecated
    public static String tipRef(String txt, int msgSeq) { return GrayTipApi.itemRef(txt, msgSeq); }

    @Deprecated
    public static String tipCompose(String... items) { return GrayTipApi.compose(items); }

    @Deprecated

    public static boolean grayTipItems(int chatType, String peerUid, String... items) {
        return GrayTipApi.items(chatType, peerUid, items);
    }

    @Deprecated

    public static String troopMemberName(String troopUin, String uin) {
        return IdentityApi.troopMemberName(troopUin, uin);
    }

    @Deprecated

    public static Object getSingleMsg(int chatType, String peerUid, long msgSeq) {
        return MsgApi.getSingleMsg(chatType, peerUid, msgSeq);
    }

    @Deprecated
    public static void toast(String msg) { UiApi.toast(msg); }

    @Deprecated
    public static void runOnUi(Runnable r) { UiApi.runOnUi(r); }

    @Deprecated

    public static void copyToClipboard(String label, String text) {
        UiApi.copyToClipboard(label, text);
    }

    @Deprecated
    public static String readClipboard() { return UiApi.readClipboard(); }

    @Deprecated
    public static void panelShow(android.content.Context c, String badge, String title,
                                 String sub, android.view.View body, String okLabel,
                                 PanelApi.OnOk onOk) {
        PanelApi.show(c, badge, title, sub, body, okLabel, onOk);
    }

    @Deprecated

    public static android.widget.LinearLayout panelFieldBox(android.content.Context c) {
        return PanelApi.fieldBox(c);
    }

    @Deprecated
    public static android.widget.LinearLayout panelField(android.content.Context c, int idx,
                                                          String hint, String value, String label) {
        return PanelApi.field(c, idx, hint, value, label);
    }

    @Deprecated

    public static android.widget.EditText panelEdit(android.view.View fieldBox) {
        return PanelApi.fieldEdit(fieldBox);
    }

    @Deprecated

    public static android.view.View panelEmpty(android.content.Context c, String msg) {
        return PanelApi.emptyState(c, msg);
    }

    @Deprecated
    public static android.widget.TextView panelButton(android.content.Context c,
                                                      String label, boolean primary) {
        return PanelApi.pillButton(c, label, primary);
    }

    @Deprecated
    public static java.io.File dataDir() { return IoApi.dataDir(); }

    @Deprecated
    public static boolean writeFile(String name, String content) { return IoApi.writeFile(name, content); }

    @Deprecated
    public static String readFile(String name) { return IoApi.readFile(name); }

    @Deprecated
    public static void logFile(String tag, String msg) { IoApi.logFile(tag, msg); }

    @Deprecated
    public static List<String> listElementFactories() { return IoApi.listElementFactories(); }

    @Deprecated
    public static boolean appendFile(String name, String content) { return IoApi.appendFile(name, content); }

    @Deprecated
    public static boolean fileExists(String name) { return IoApi.fileExists(name); }

    @Deprecated
    public static boolean deleteFile(String name) { return IoApi.deleteFile(name); }

    @Deprecated
    public static long now() { return UtilApi.now(); }

    @Deprecated
    public static String formatTime(long ms, String pattern) { return UtilApi.formatTime(ms, pattern); }

    @Deprecated
    public static void runAsync(final Runnable r) { UtilApi.runAsync(r); }

    @Deprecated
    public static void postDelayed(Runnable r, long delayMs) { UtilApi.postDelayed(r, delayMs); }

    @Deprecated
    public static void sleep(long ms) { UtilApi.sleep(ms); }

    @Deprecated
    public static String jsonPretty(String json) { return UtilApi.jsonPretty(json); }

    @Deprecated
    public static long hostVersionCode() { return UtilApi.hostVersionCode(); }

    @Deprecated
    public static String hostVersionName() { return UtilApi.hostVersionName(); }

    @Deprecated
    public static Class<?> findClass(String className) { return ReflectApi.findClass(className); }

    @Deprecated
    public static Object hookBefore(String className, String methodName,
                                    com.cathelperawa.owo.app.core.HookApi.Action action) {
        return ReflectApi.hookBefore(className, methodName, action);
    }

    @Deprecated
    public static Object hookAfter(String className, String methodName,
                                   com.cathelperawa.owo.app.core.HookApi.Action action) {
        return ReflectApi.hookAfter(className, methodName, action);
    }

    @Deprecated
    public static int hookAll(String className, String methodName, int paramCount,
                              com.cathelperawa.owo.app.core.HookApi.Action action) {
        return ReflectApi.hookAll(className, methodName, paramCount, action);
    }

    @Deprecated
    public static Object kernelMsgService() { return ProtoApi.kernelMsgService(); }

    @Deprecated
    public static Object appRuntime() { return ProtoApi.appRuntime(); }

    @Deprecated

    public static String fetchViaKernel(Object recordOrMsgItem, long msgId) {
        return ProtoApi.fetchViaKernel(recordOrMsgItem, msgId);
    }

    @Deprecated
    public static void sendProtocol(String cmd, byte[] payload) { ProtoApi.sendProtocol(cmd, payload); }

    @Deprecated
    public static byte[] jsonToPb(String json) { return ProtoApi.jsonToPb(json); }

    @Deprecated
    public static String pbToJson(byte[] pb) { return ProtoApi.pbToJson(pb); }

    @Deprecated
    public static void expectResponse(String cmd,
            com.cathelperawa.owo.app.feature.FeatureMsfTap.Listener listener) {
        ProtoApi.expectResponse(cmd, listener);
    }

    @Deprecated
    public static void cancelExpect() { ProtoApi.cancelExpect(); }

    @Deprecated
    public static String lastRawResponse() { return ProtoApi.lastRawResponse(); }

    @Deprecated
    public static String[] msfCmds() { return ProtoApi.msfCmds(); }

    @Deprecated
    public static String msfDecode(byte[] buf) { return ProtoApi.msfDecode(buf); }

    @Deprecated

    public static Object msfExtractElements(org.json.JSONObject content) {
        return ProtoApi.msfExtractElements(content);
    }

    @Deprecated

    public static List<Object> collectElements(Object recordOrMsgItem) {
        return ProtoApi.collectElements(recordOrMsgItem);
    }

    @Deprecated

    public static List<Object> sendableElements(List<Object> els) {
        return ProtoApi.sendableElements(els);
    }

    @Deprecated

    public static String grabRawElements(Object recordOrMsgItem, int chatType) {
        return ProtoApi.grabRawElements(recordOrMsgItem, chatType);
    }

    @Deprecated

    public static String filterSendable(String rawJson) {
        return ProtoApi.filterSendable(rawJson);
    }

    @Deprecated

    public static String plainText(Object recordOrMsgItem) {
        return ProtoApi.plainText(recordOrMsgItem);
    }

    @Deprecated
    public static String jsonPrettyOf(Object o) { return ProtoApi.jsonPrettyOf(o); }

    @Deprecated

    public static Object walletOf(Object recordOrMsgItem) {
        return ProtoApi.walletOf(recordOrMsgItem);
    }

    @Deprecated

    public static String pullMsg(Object recordOrMsgItem) {
        return ProtoApi.pullMsg(recordOrMsgItem);
    }

    @Deprecated

    public static String pullC2C(Object recordOrMsgItem) {
        return ProtoApi.pullC2C(recordOrMsgItem);
    }

    @Deprecated

    public static String pullGroup(Object recordOrMsgItem) {
        return ProtoApi.pullGroup(recordOrMsgItem);
    }

    @Deprecated

    public static String grabCode(Object recordOrMsgItem) {
        return ProtoApi.grabCode(recordOrMsgItem);
    }

    @Deprecated
    public static String xmlUnescape(String s) { return ProtoApi.xmlUnescape(s); }

    @Deprecated

    public static byte[] pbMsgDirect(int chatType, String peerUin, byte[] elemsPB, String selfUin) {
        return ProtoApi.pbMsgDirect(chatType, peerUin, elemsPB, selfUin);
    }

    @Deprecated

    public static byte[] pbMsgRef(int chatType, String peerUin, String resId) {
        return ProtoApi.pbMsgRef(chatType, peerUin, resId);
    }

    @Deprecated
    public static byte[] pbMsgMulti(byte[] elemsPB) { return ProtoApi.pbMsgMulti(elemsPB); }

    @Deprecated

    public static byte[] pbMsgUpload(int chatType, String peerUin, byte[] gz) {
        return ProtoApi.pbMsgUpload(chatType, peerUin, gz);
    }

    @Deprecated
    public static byte[] pbVint(long v) { return ProtoApi.pbVint(v); }

    @Deprecated
    public static byte[] pbGzip(byte[] data) { return ProtoApi.pbGzip(data); }

    @Deprecated

    public static java.util.List<org.json.JSONObject> pbElemList(String json) {
        return ProtoApi.pbElemList(json);
    }

    @Deprecated

    public static boolean ovoSend(String elementsJson, String peer, boolean isGroup) {
        return ProtoApi.ovoSend(elementsJson, peer, isGroup);
    }

    @Deprecated
    public static byte[] ovoBuild(String json) { return ProtoApi.ovoBuild(json); }

    @Deprecated

    public static boolean ovoSendBuffer(String cmd, byte[] body) {
        return ProtoApi.ovoSendBuffer(cmd, body);
    }

    @Deprecated
    public static Object field(Object o, String name) { return ReflectApi.field(o, name); }

    @Deprecated

    public static boolean setField(Object o, String name, Object value) {
        return ReflectApi.setField(o, name, value);
    }

    @Deprecated

    public static java.lang.reflect.Field findField(Class<?> cls, String name) {
        return ReflectApi.findField(cls, name);
    }

    @Deprecated

    public static Object call(Object o, String method, Object... args) {
        return ReflectApi.call(o, method, args);
    }

    @Deprecated
    public static String str(Object o) { return ReflectApi.str(o); }

    @Deprecated
    public static String hexEncode(byte[] b) { return ReflectApi.hexEncode(b); }

    @Deprecated
    public static byte[] hexDecode(String s) { return ReflectApi.hexDecode(s); }

    @Deprecated
    public static boolean isHex(String s) { return ReflectApi.isHex(s); }

    @Deprecated
    public static Object hookReplace(String className, String methodName,
                                     com.cathelperawa.owo.app.core.HookApi.Action action) {
        return ReflectApi.hookReplace(className, methodName, action);
    }

    @Deprecated

    public static Object hookReturn(String className, String methodName, Object value) {
        return ReflectApi.hookReturn(className, methodName, value);
    }

    @Deprecated
    public static void unhook(Object handle) { ReflectApi.unhook(handle); }

    @Deprecated
    public static java.lang.reflect.Method findMethodByParams(Class<?> cls, Class<?> ret,
                                                              Class<?>... params) {
        return ReflectApi.findMethodByParams(cls, ret, params);
    }

    @Deprecated

    public static List<java.lang.reflect.Method> findMethodsByParams(Class<?> cls, int paramCount) {
        return ReflectApi.findMethodsByParams(cls, paramCount);
    }

    @Deprecated

    public static void notePeer(String uid, String name, boolean group) {
        ScopeApi.notePeer(uid, name, group);
    }

    @Deprecated

    public static boolean scopeAll() {
        return ScopeApi.all();
    }

    @Deprecated

    public static void setScopeAll(boolean v) {
        ScopeApi.setAll(v);
    }

    @Deprecated

    public static boolean isSelected(String uid) {
        return ScopeApi.isSelected(uid);
    }

    @Deprecated

    public static void toggleSelected(String uid) {
        ScopeApi.toggleSelected(uid);
    }

    @Deprecated
    public static boolean scopeAllows(String uid) { return ScopeApi.allows(uid); }

    @Deprecated
    public static void toggleUnderAll(String uid) { ScopeApi.toggleUnderAll(uid); }

    @Deprecated
    public static boolean isUnderAllOff(String uid) { return ScopeApi.isUnderAllOff(uid); }

    @Deprecated
    public static java.util.Map<String, String> scopePeers() { return ScopeApi.peers(); }

    @Deprecated
    public static String getPskey(String url) { return CookieApi.pskey(url); }

    @Deprecated

    public static java.util.Map<String, String> getPskeyBatch(String... urls) {
        return CookieApi.pskeyBatch(urls);
    }

    @Deprecated
    public static String hexGuid() { return CookieApi.hexGuid(); }

    @Deprecated
    public static String getSkey() { return CookieApi.skey(); }

    @Deprecated
    public static String getRealSkey() { return CookieApi.realSkey(); }

    @Deprecated
    public static String getStweb() { return CookieApi.stweb(); }

    @Deprecated
    public static String getPt4Token(String url) { return CookieApi.pt4Token(url); }

    @Deprecated
    public static long getBkn(String skey) { return CookieApi.bkn(skey); }

    @Deprecated
    public static long bknOf(String key) { return CookieApi.bknOf(key); }

    @Deprecated
    public static String getGTK(String url) { return CookieApi.gtk(url); }

    @Deprecated
    public static String hostOf(String url) { return CookieApi.hostOf(url); }

    @Deprecated
    public static String walletEncrypt(String uin, String salt, int randomInt,
                                       String formString, String psKey, String hexGuid) {
        return WalletApi.encrypt(uin, salt, randomInt, formString, psKey, hexGuid);
    }

    @Deprecated

    public static String walletDecrypt(String uin, String cgiName, int randomInt, String encStr) {
        return WalletApi.decrypt(uin, cgiName, randomInt, encStr);
    }

    @Deprecated
    public static java.util.Map<String, String> walletProcess(String url, String secCmd,
                                                              java.util.Map<String, String> form) {
        return WalletApi.process(url, secCmd, form);
    }

    @Deprecated

    public static String walletPsKey(String url) {
        return WalletApi.psKey(url);
    }

    @Deprecated

    public static String formString(java.util.Map<String, String> map) {
        return WalletApi.formString(map);
    }

    @Deprecated

    public static byte[] add4Byte(byte[] body) {
        return WalletApi.add4Byte(body);
    }

    public static boolean inMultiSelect() {
        return MultiSelectApi.isInMultiSelect();
    }

    public static List<Object> selectedMsgs() {
        return MultiSelectApi.selectedItems();
    }

    public static List<Long> selectedMsgIds() {
        return MultiSelectApi.selectedMsgIds();
    }

    public static int selectedCount() {
        return MultiSelectApi.selectedCount();
    }

    public static boolean recallSync(Object msg, long timeoutMs) {
        return MsgApi.recallSync(msg, timeoutMs);
    }

    public static int[] recallBatch(List<?> msgs, long gapMs) {
        return MsgApi.recallBatch(msgs, gapMs);
    }

    public static int recallSelected() {
        final List<Object> sel = MultiSelectApi.selectedItems();
        if (sel.isEmpty()) return 0;
        Bg.run(new Runnable() { @Override public void run() {
            MsgApi.recallBatch(sel, 400);
        } }, "cat-recall-selected");
        return sel.size();
    }

    public interface ServletCallback extends ServletApi.Callback {}

    @Deprecated

    public static boolean sendServlet(String cmd, byte[] data, ServletCallback cb) {
        return ServletApi.send(cmd, data, cb);
    }

    @Deprecated

    public static boolean sendServlet(String cmd, byte[] data, long timeout, ServletCallback cb) {
        return ServletApi.send(cmd, data, timeout, cb);
    }

    @Deprecated

    public static String servletClassName() {
        return ServletApi.className();
    }

    @Deprecated

    public static boolean setServletClass(String className) {
        return ServletApi.setClassName(className);
    }

    @Deprecated

    public static byte[] servletBytes(android.os.Bundle bundle) {
        return ServletApi.bytes(bundle);
    }

    @Deprecated

    public static String dumpBundle(android.os.Bundle bundle) {
        return ServletApi.dumpBundle(bundle);
    }

    @Deprecated
    public static String redPacketProbe() { return RedPacketApi.probe(); }

    @Deprecated
    public static boolean isRedPacketManualEnabled() { return RedPacketApi.manualEnabled(); }

    @Deprecated
    public static void setRedPacketManualEnabled(boolean on) { RedPacketApi.setManualEnabled(on); }

    @Deprecated
    public static boolean isRedPacketAutoEnabled() { return RedPacketApi.autoEnabled(); }

    @Deprecated
    public static void setRedPacketAutoEnabled(boolean on) { RedPacketApi.setAutoEnabled(on); }

    @Deprecated
    public static void redPacketGrab(Object msgItemOrRecord) { RedPacketApi.grab(msgItemOrRecord); }

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

    public static com.cathelperawa.owo.app.model.CatResult sendElements(
            com.cathelperawa.owo.app.model.CatPeer peer, List<Object> elements) {
        return MsgApi.sendElementsResult(peer, elements);
    }

    public static com.cathelperawa.owo.app.model.CatResult grab(
            com.cathelperawa.owo.app.model.CatMsg msg) {
        if (msg == null) return com.cathelperawa.owo.app.model.CatResult.badArgs("msg 为空");
        return RedPacketApi.grabResult(msg.raw());
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

    public static boolean clockInOn() { return FeatureApi.clockInOn(); }
    public static void setClockInOn(boolean on) { FeatureApi.setClockInOn(on); }

    public static List<String> clockInGroups() { return FeatureApi.clockInGroups(); }
    public static void setClockInGroups(List<String> groups) { FeatureApi.setClockInGroups(groups); }
    public static boolean addClockInGroup(String troopUin) { return FeatureApi.addClockInGroup(troopUin); }
    public static boolean removeClockInGroup(String troopUin) { return FeatureApi.removeClockInGroup(troopUin); }

    public static List<String> clockInPending() { return FeatureApi.clockInPending(); }
    public static List<String> clockInDone() { return FeatureApi.clockInDone(); }

    public static boolean clockInReady() { return FeatureApi.clockInReady(); }

    public static boolean clockInNow() { return FeatureApi.clockInNow(); }

    public static int clockInConcurrency() { return FeatureApi.clockInConcurrency(); }
    public static void setClockInConcurrency(int n) { FeatureApi.setClockInConcurrency(n); }
    public static int clockInDeadlineSec() { return FeatureApi.clockInDeadlineSec(); }
    public static void setClockInDeadlineSec(int sec) { FeatureApi.setClockInDeadlineSec(sec); }

    public static void clearClockInLedger() { FeatureApi.clearClockInLedger(); }

    public static boolean viewCardOn() { return FeatureApi.viewCardOn(); }
    public static void setViewCardOn(boolean on) { FeatureApi.setViewCardOn(on); }

    public static boolean msgTimeOn() { return FeatureApi.msgTimeOn(); }
    public static void setMsgTimeOn(boolean on) { FeatureApi.setMsgTimeOn(on); }
    public static String msgTimeFormat() { return FeatureApi.msgTimeFormat(); }
    public static void setMsgTimeFormat(String pattern) { FeatureApi.setMsgTimeFormat(pattern); }

    public static int msgTimeColor() { return FeatureApi.msgTimeColor(); }
    public static void setMsgTimeColor(int argb) { FeatureApi.setMsgTimeColor(argb); }

    public static int msgTimeSize() { return FeatureApi.msgTimeSize(); }
    public static void setMsgTimeSize(int sp) { FeatureApi.setMsgTimeSize(sp); }

    public static boolean picSummaryOn() { return FeatureApi.picSummaryOn(); }
    public static void setPicSummaryOn(boolean on) { FeatureApi.setPicSummaryOn(on); }
    public static String picSummaryText() { return FeatureApi.picSummaryText(); }
    public static void setPicSummaryText(String text) { FeatureApi.setPicSummaryText(text); }

    public static boolean fakePhoneOn() { return FeatureApi.fakePhoneOn(); }
    public static void setFakePhoneOn(boolean on) { FeatureApi.setFakePhoneOn(on); }
    public static String fakePhone() { return FeatureApi.fakePhone(); }
    public static void setFakePhone(String number) { FeatureApi.setFakePhone(number); }

    public static boolean fakeNetworkOn() { return FeatureApi.fakeNetworkOn(); }
    public static void setFakeNetworkOn(boolean on) { FeatureApi.setFakeNetworkOn(on); }

    public static int fakeNetworkMode() { return FeatureApi.fakeNetworkMode(); }
    public static void setFakeNetworkMode(int mode) { FeatureApi.setFakeNetworkMode(mode); }
    public static String fakeNetworkModeName(int mode) { return FeatureApi.fakeNetworkModeName(mode); }

    public static boolean customDeviceOn() { return FeatureApi.customDeviceOn(); }
    public static void setCustomDeviceOn(boolean on) { FeatureApi.setCustomDeviceOn(on); }
    public static String customDevice() { return FeatureApi.customDevice(); }
    public static String customModel() { return FeatureApi.customModel(); }
    public static String customManufacturer() { return FeatureApi.customManufacturer(); }

    public static void setCustomDevice(String device, String model, String manufacturer) {
        FeatureApi.setCustomDevice(device, model, manufacturer);
    }

    public static boolean apkRenameOn() { return FeatureApi.apkRenameOn(); }
    public static void setApkRenameOn(boolean on) { FeatureApi.setApkRenameOn(on); }

    public static String apkNameOf(String path) { return FeatureApi.apkNameOf(path); }

    public static boolean qqBrowserOn() { return FeatureApi.qqBrowserOn(); }
    public static void setQqBrowserOn(boolean on) { FeatureApi.setQqBrowserOn(on); }

    public static boolean isTroopUin(String s) { return FeatureApi.isTroopUin(s); }

    public static String joinTroops(List<String> groups) { return FeatureApi.joinTroops(groups); }

    public static EventApi.Listener onMessage(EventApi.Listener l) { return EventApi.onMessage(l); }

    public static boolean offMessage(EventApi.Listener l) { return EventApi.offMessage(l); }

    public static int listenerCount() { return EventApi.listenerCount(); }

    public static String eventStats() { return EventApi.statsJson(); }

    public static void clearEventListeners() { EventApi.clearListeners(); }

    public static void resetEventStats() { EventApi.reset(); }

    public static boolean httpGet(String url, String headersJson, NetApi.Callback cb) {
        NetApi.get(url, headersJson, cb);
        return true;
    }

    public static boolean httpPost(String url, String headersJson, String body, NetApi.Callback cb) {
        NetApi.post(url, headersJson, body, cb);
        return true;
    }

    public static boolean httpPostJson(String url, String headersJson, String json, NetApi.Callback cb) {
        NetApi.postJson(url, headersJson, json, cb);
        return true;
    }

    public static String httpGetSync(String url, String headersJson, int timeoutMs) {
        return NetApi.getSync(url, headersJson, timeoutMs).toString();
    }

    public static String httpPostSync(String url, String headersJson, String body, int timeoutMs) {
        return NetApi.postSync(url, headersJson, body, timeoutMs).toString();
    }

    public static int netPending() { return NetApi.pending(); }

    public static Object after(long delayMs, Runnable body) { return TaskApi.after(delayMs, body); }

    public static Object every(long intervalMs, Runnable body) { return TaskApi.every(intervalMs, body); }

    public static boolean cancelTask(Object token) { return TaskApi.cancel(token); }

    public static void cancelAllTasks() { TaskApi.cancelAll(); }

    public static int taskCount() { return TaskApi.count(); }

    public static String taskStats() { return TaskApi.statsJson(); }

    public static List<String> taskList() { return TaskApi.list(); }

    public static boolean isOwnerOrAdmin(String groupUin) {
        return GroupManageApi.isOwnerOrAdmin(groupUin);
    }

    public static boolean modifyMemberCard(String group, String uid, String card) {
        return GroupManageApi.modifyCard(group, uid, card);
    }

    public static boolean muteMember(String group, String uid, int seconds) {
        return GroupManageApi.mute(group, uid, seconds);
    }

    public static boolean unmuteMember(String group, String uid) {
        return GroupManageApi.unmute(group, uid);
    }

    public static boolean kickMember(String group, String uid) {
        return GroupManageApi.kick(group, uid);
    }

    public static void parseSetAutoConvert(boolean on) {
        ParseApi.setAutoConvert(on);
    }

    public static boolean parseGetAutoConvert() {
        return ParseApi.isAutoConvert();
    }

    public static void parseSetDisplaySec(int seconds) {
        ParseApi.setDisplayDuration(seconds);
    }

    public static int parseGetDisplaySec() {
        return ParseApi.getDisplayDuration();
    }

    public static String parseStatus() {
        return ParseApi.status();
    }

    public static String parseSearchMusic(String keyword, int limit) {
        return ParseApi.searchMusic(keyword, limit);
    }

    public static String parseMusicUrl(String songId, String name, String artist, int br) {
        return ParseApi.musicUrl(songId, name, artist, br);
    }

    public static boolean parseMusicByKeyword(int chatType, String peerUid,
                                              String keyword, int maxSeconds) {
        return ParseApi.sendMusicByKeyword(chatType, peerUid, keyword, maxSeconds);
    }

    public static boolean parseSendMusicVoice(int chatType, String peerUid,
                                              String songId, String name, String artist,
                                              String pic, int maxSeconds) {
        return ParseApi.sendMusicVoice(chatType, peerUid, songId, name, artist, pic, maxSeconds);
    }

    public static boolean parseSendMusicMp3(int chatType, String peerUid,
                                            String songId, String name, String artist) {
        return ParseApi.sendMusicMp3(chatType, peerUid, songId, name, artist);
    }

    public static boolean parseSendMusicFile(int chatType, String peerUid,
                                             String songId, String name, String artist) {
        return ParseApi.sendMusicFile(chatType, peerUid, songId, name, artist);
    }

    public static boolean parseSendMusicImage(int chatType, String peerUid,
                                              String songId, String name, String artist) {
        return ParseApi.sendMusicImage(chatType, peerUid, songId, name, artist);
    }

    public static boolean parseSendPtt(int chatType, String peerUid,
                                       String path, int durationMs) {
        return ParseApi.sendPtt(chatType, peerUid, path, durationMs);
    }

    public static boolean parseSendAudioAsPtt(int chatType, String peerUid,
                                              String audioPath, int maxSeconds) {
        return ParseApi.sendAudioAsPtt(chatType, peerUid, audioPath, maxSeconds);
    }

    public static boolean fakeChat(String text) {
        return fakeChatAt(text, 0);
    }

    public static boolean fakeChatAt(String text, long baseSec) {
        try {
            final android.content.Context ctx =
                    com.cathelperawa.owo.app.bridge.QQEnv.appContext();
            if (ctx == null) return false;
            final java.util.List<com.cathelperawa.owo.app.proto.PbMsg.FakeItem> items =
                    com.cathelperawa.owo.app.proto.FakeChat.parse(text);
            if (items.isEmpty()) return false;
            long base = baseSec > 0 ? baseSec : System.currentTimeMillis() / 1000L;
            com.cathelperawa.owo.app.proto.FakeChat.build(ctx, items, base,
                    new com.cathelperawa.owo.app.proto.FakeChat.OnArk() {
                        @Override public void run(String arkJson) {
                            com.cathelperawa.owo.app.ui.SendPanel.sendArkJson(ctx, arkJson);
                        }
                    },
                    new com.cathelperawa.owo.app.proto.FakeChat.OnFail() {
                        @Override public void run(String reason) {
                            com.cathelperawa.owo.app.core.HookLog.w("API fakeChat: " + reason);
                        }
                    });
            return true;
        } catch (Throwable t) {
            com.cathelperawa.owo.app.core.HookLog.e("API fakeChat", t);
            return false;
        }
    }

    public static String mediaList() {
        try {
            android.content.Context ctx =
                    com.cathelperawa.owo.app.bridge.QQEnv.appContext();
            java.util.List<String[]> lib =
                    com.cathelperawa.owo.app.proto.MediaVault.all(ctx);
            org.json.JSONArray arr = new org.json.JSONArray();
            for (int i = 0; i < lib.size(); i++) {
                org.json.JSONObject o = new org.json.JSONObject();
                o.put("index", i);
                o.put("name", lib.get(i)[0]);
                o.put("json", lib.get(i)[1]);
                arr.put(o);
            }
            return arr.toString();
        } catch (Throwable t) {
            return "[]";
        }
    }

    public static boolean mediaAdd(String json) {
        try {
            com.cathelperawa.owo.app.proto.MediaVault.add(
                    com.cathelperawa.owo.app.bridge.QQEnv.appContext(), json);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean mediaRemove(int index) {
        try {
            com.cathelperawa.owo.app.proto.MediaVault.remove(
                    com.cathelperawa.owo.app.bridge.QQEnv.appContext(), index);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean mediaRename(int index, String name) {
        try {
            com.cathelperawa.owo.app.proto.MediaVault.rename(
                    com.cathelperawa.owo.app.bridge.QQEnv.appContext(), index, name);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    public static String mediaPbHex(String json) {
        try {
            byte[] b = com.cathelperawa.owo.app.proto.MediaVault.toPb(json);
            if (b == null || b.length == 0) return "";
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < b.length; i++) {
                sb.append(String.format("%02x", b[i]));
            }
            return sb.toString();
        } catch (Throwable t) {
            return "";
        }
    }

    public static String locationTemplate() {
        return com.cathelperawa.owo.app.ui.SendPanel.locationTemplate();
    }

    public static String parseBiliInfo(String linkOrBvid) {
        return ParseApi.biliInfo(linkOrBvid);
    }

    public static boolean parseBiliSendVideo(int chatType, String peerUid,
                                             String linkOrBvid, int maxMB) {
        return ParseApi.biliSendVideo(chatType, peerUid, linkOrBvid, maxMB);
    }

    public static boolean parseBiliSendVoice(int chatType, String peerUid,
                                             String linkOrBvid, int maxSeconds) {
        return ParseApi.biliSendVoice(chatType, peerUid, linkOrBvid, maxSeconds);
    }

}
