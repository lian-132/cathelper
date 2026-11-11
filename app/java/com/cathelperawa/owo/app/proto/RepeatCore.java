package com.cathelperawa.owo.app.proto;

import com.cathelperawa.owo.app.api.CatApi;
import com.cathelperawa.owo.app.bridge.ContactFactory;
import com.cathelperawa.owo.app.bridge.MsgSender;
import com.cathelperawa.owo.app.core.HookLog;
import com.cathelperawa.owo.app.feature.FeatureMsgExtract;
import com.cathelperawa.owo.app.feature.FeatureMsfTap;
import com.cathelperawa.owo.app.protocol.MsgFetcher;
import com.cathelperawa.owo.app.util.Refl;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public final class RepeatCore {

    private RepeatCore() {}

    private static final long GRAB_TIMEOUT_MS = 4000;

    public static final class Outcome {
        public boolean ok;
        public String channel;   
        public String note;
        public String toString() {
            return (ok ? " " : " ") + (channel == null ? "-" : channel)
                    + (note == null ? "" : " · " + note);
        }
    }

    

    public static String kind(Object el) {
        if (Refl.field(el, "grayTipElement") != null) return "gray";
        if (Refl.field(el, "replyElement") != null) return "reply";
        if (Refl.field(el, "textElement") != null) return "text";
        if (Refl.field(el, "picElement") != null) return "pic";
        if (Refl.field(el, "arkElement") != null) return "ark";
        if (Refl.field(el, "multiForwardMsgElement") != null) return "fwd";
        if (Refl.field(el, "fileElement") != null) return "file";
        if (Refl.field(el, "videoElement") != null) return "video";
        if (Refl.field(el, "pttElement") != null) return "ptt";
        if (Refl.field(el, "faceElement") != null) return "face";
        if (Refl.field(el, "marketFaceElement") != null) return "market";
        if (Refl.field(el, "shareLocationElement") != null) return "loc";
        if (Refl.field(el, "textGiftElement") != null) return "gift";
        int et = Refl.i32(Refl.field(el, "elementType"), -1);
        switch (et) {
            case 1: return "text";
            case 2: return "pic";
            case 3: return "file";
            case 4: return "ptt";
            case 5: return "video";
            case 6: return "face";
            case 7: return "reply";
            case 8: return "gray";
            case 10: return "ark";
            case 16: return "fwd";
            default: return "other";
        }
    }

    public static List<Object> collect(Object record) {
        List<Object> out = new ArrayList<Object>();
        Object els = Refl.field(record, "elements");
        if (els instanceof List) out.addAll((List<?>) els);
        Object aux = Refl.field(record, "auxiliaryElements");
        if (aux instanceof List) {
            for (Object o : (List<?>) aux) {
                if (o != null && !out.contains(o)) out.add(o);
            }
        }
        return out;
    }

    public static List<Object> sendable(List<Object> els) {
        List<Object> out = new ArrayList<Object>();
        for (Object el : els) {
            if (el == null) continue;
            if (Refl.field(el, "grayTipElement") != null) continue;
            out.add(el);
        }
        return out;
    }

    

    
    public static Outcome repeat(Object record, int chatType, String peerUid) {
        Outcome out = new Outcome();
        try {
            if (record == null || peerUid == null || peerUid.length() == 0) {
                out.note = "没拿到消息或会话";
                return out;
            }

            

            

            

            List<Object> els = collect(record);
            List<Object> sendEls = sendable(els);

            String localText = plainText(record);
            boolean onlyText = !sendEls.isEmpty() && onlyTextElements(sendEls);
            if (onlyText && localText.length() > 0
                    && CatApi.sendText(chatType, peerUid, localText)) {
                out.ok = true;
                out.channel = "text";
                out.note = brief(localText);
                return out;
            }

            if (!sendEls.isEmpty()) {
                Object contact = ContactFactory.make(chatType, peerUid, "");
                if (contact != null && MsgSender.send(contact, sendEls)) {
                    out.ok = true;
                    out.channel = "kernel";
                    out.note = "元素 ×" + sendEls.size();
                    return out;
                }
                HookLog.w("复读[内核] 发送失败，转提取路线");
            }

            String rawElems = grabRawElements(record, chatType);
            if (rawElems != null && rawElems.length() > 2) {
                HookLog.i("复读[提取] " + brief(rawElems));
                String sendableJson = filterSendable(rawElems);
                HookLog.i("复读[过滤] 前=" + rawElems.length() + "B 后="
                        + (sendableJson == null ? "null" : sendableJson.length() + "B")
                        + " 内容=" + brief(sendableJson));
                if (sendableJson != null && sendableJson.length() > 2) {
                    
                    if (isOnlyText(sendableJson)) {
                        String plain = plainTextOfJson(sendableJson);
                        if (plain.length() > 0 && CatApi.sendText(chatType, peerUid, plain)) {
                            out.ok = true;
                            out.channel = "text";
                            out.note = brief(plain);
                            return out;
                        }
                    }
                    String ch = PbSender.route(sendableJson, chatType, peerUid);
                    if (ch != null) {
                        out.ok = true;
                        out.channel = "grab/" + ch;
                        out.note = sendableJson.length() + "B";
                        return out;
                    }
                } else {
                    HookLog.w("复读[提取] 过滤后无可用元素");
                }
            } else {
                HookLog.w("复读[提取] 拿不到原始 elements");
            }

            String plain = plainText(record);
            if (plain.length() > 0 && CatApi.sendText(chatType, peerUid, plain)) {
                out.ok = true;
                out.channel = "text";
                out.note = brief(plain);
                return out;
            }

            out.note = "这条复读不了（服务端事件，且无文本内容）";
            return out;
        } catch (Throwable t) {
            HookLog.e("复读异常", t);
            out.note = "异常: " + t;
            return out;
        }
    }

    public static String grabRawElements(Object record, int chatType) {
        try {
            
            final String wantCmd = (chatType == 1)
                    ? "MessageSvc.PbGetOneDayRoamMsg"
                    : (chatType == 2 ? "MessageSvc.PbGetGroupMsg" : null);
            if (wantCmd == null) return null;

            final CountDownLatch latch = new CountDownLatch(1);
            final String[] box = new String[1];

            final String ticket = FeatureMsfTap.expect(wantCmd, new FeatureMsfTap.Listener() {
                @Override
                public void onResp(String cmd, JSONObject content) {
                    try {
                        Object els = FeatureMsfTap.extractElements(content);
                        if (els != null) {
                            box[0] = (els instanceof JSONArray)
                                    ? ((JSONArray) els).toString()
                                    : els.toString();
                        }
                    } catch (Throwable ignore) {
                    } finally {
                        latch.countDown();
                    }
                }
            });

            String cmd;
            if (chatType == 1) cmd = MsgFetcher.pullC2C(record);
            else cmd = MsgFetcher.pullGroup(record);
            if (cmd == null) {
                FeatureMsfTap.cancel(ticket);
                return null;
            }

            if (!latch.await(GRAB_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                FeatureMsfTap.cancel(ticket);   
                HookLog.w("复读[提取] 超时 " + GRAB_TIMEOUT_MS + "ms");
                return null;
            }
            return box[0];
        } catch (Throwable t) {
            HookLog.e("grabRawElements 失败", t);
            return null;
        }
    }

    public static String filterSendable(String rawJson) {
        try {
            JSONArray arr = new JSONArray(rawJson.trim());
            JSONArray out = new JSONArray();
            for (int i = 0; i < arr.length(); i++) {
                Object elObj = arr.opt(i);
                if (!(elObj instanceof JSONObject)) continue;
                JSONObject el = (JSONObject) elObj;
                if (isSendableElement(el)) out.put(el);
            }
            return out.toString();
        } catch (Throwable t) {
            HookLog.w("filterSendable 失败: " + t);
            return rawJson;   
        }
    }

    private static boolean isSendableElement(JSONObject el) {

        
        if (el.has("1"))  return true;    
        if (el.has("2"))  return true;    
        if (el.has("3"))  return true;    
        if (el.has("4"))  return true;    
        if (el.has("5"))  return true;    
        if (el.has("6"))  return true;    
        if (el.has("8"))  return true;    
        if (el.has("10")) return true;    
        if (el.has("16")) return true;    
        if (el.has("37")) {

            JSONObject s = el.optJSONObject("37");
            if (s != null && (s.has("7") || s.has("6"))) return true;   
            return false;
        }

        if (el.has("53")) {
            try {
                JSONObject c = el.optJSONObject("53");
                if (c == null) return false;
                return c.optInt("1", -1) == 41;   
            } catch (Throwable t) {
                return false;
            }
        }

        return false;
    }

    private static boolean onlyTextElements(List<Object> els) {
        if (els == null || els.isEmpty()) return false;
        for (Object el : els) {
            if (el == null) return false;
            if (Refl.field(el, "textElement") == null) return false;
        }
        return true;
    }

    private static boolean isOnlyText(String json) {
        try {
            JSONArray arr = new JSONArray(json.trim());
            if (arr.length() == 0) return false;
            for (int i = 0; i < arr.length(); i++) {
                JSONObject el = arr.optJSONObject(i);
                if (el == null) return false;
                if (!el.has("1")) return false;   
            }
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private static String plainTextOfJson(String json) {
        StringBuilder sb = new StringBuilder();
        try {
            JSONArray arr = new JSONArray(json.trim());
            for (int i = 0; i < arr.length(); i++) {
                JSONObject el = arr.optJSONObject(i);
                if (el == null) continue;
                JSONObject t = el.optJSONObject("1");
                if (t == null) continue;
                String s = t.optString("1", "");
                sb.append(s);
            }
        } catch (Throwable ignore) {
        }
        return sb.toString();
    }

    public static String plainText(Object record) {
        StringBuilder sb = new StringBuilder();
        for (Object el : collect(record)) {
            Object tx = Refl.field(el, "textElement");
            if (tx == null) continue;
            Object c = Refl.field(tx, "content");
            if (c != null) sb.append(c);
        }
        return sb.toString();
    }

    public static String grabJson(Object record) {
        MsgGrab.Result r = MsgGrab.extract(record);
        if (r == null) return null;
        JSONObject o = r.toJson();
        return o == null ? null : o.toString();
    }

    private static String brief(Object s) {
        if (s == null) return "";
        String t = String.valueOf(s);
        return t.length() > 80 ? t.substring(0, 80) + "…" : t;
    }
}
