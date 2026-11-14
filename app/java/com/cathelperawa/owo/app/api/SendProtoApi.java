package com.cathelperawa.owo.app.api;

import android.app.Activity;

import com.cathelperawa.owo.app.bridge.ContactFactory;
import com.cathelperawa.owo.app.bridge.CurrentChat;
import com.cathelperawa.owo.app.bridge.ElementFactory;
import com.cathelperawa.owo.app.bridge.MsgSender;
import com.cathelperawa.owo.app.bridge.QQEnv;
import com.cathelperawa.owo.app.bridge.QQUtils;
import com.cathelperawa.owo.app.core.HookConfig;
import com.cathelperawa.owo.app.core.HookLog;
import com.cathelperawa.owo.app.core.TemplateStore;
import com.cathelperawa.owo.app.ovo.FunProtoData;
import com.cathelperawa.owo.app.proto.PbSender;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

public final class SendProtoApi {

    private SendProtoApi() {
    }

    public static final String KEY_FORCE_LONG = "send_force_long";

    public static boolean refreshChat() {
        try {
            Activity a = QQEnv.topActivity();
            return a != null && CurrentChat.fromActivity(a);
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean hasChat() {
        return CurrentChat.has();
    }

    public static int chatType() {
        return CurrentChat.chatType();
    }

    public static String peerUid() {
        return CurrentChat.peerUid();
    }

    public static String peerName() {
        return CurrentChat.peerName();
    }

    public static String chatDesc() {
        return CurrentChat.describe();
    }

    public static boolean sendArk(int chatType, String peerUid, String arkJson) {
        try {
            Object el = ElementFactory.arkNative(arkJson);
            if (el == null) return false;
            java.util.List<Object> one = new java.util.ArrayList<Object>();
            one.add(el);
            Object contact = ContactFactory.make(chatType, peerUid, "");
            if (contact != null && MsgSender.send(contact, one)) return true;

            String raw = CurrentChat.peerRaw();
            if (chatType == 1 && raw.length() > 0 && !raw.equals(peerUid)) {
                Object c2 = ContactFactory.make(chatType, raw, "");
                return c2 != null && MsgSender.send(c2, one);
            }
            return false;
        } catch (Throwable t) {
            HookLog.e("SendProtoApi.sendArk", t);
            return false;
        }
    }

    public static boolean sendArkAuto(String arkJson) {
        if (!refreshChat()) return false;
        return sendArk(CurrentChat.chatType(), CurrentChat.peerUid(), arkJson);
    }

    public static boolean sendElement(int chatType, String peerUid, String elementsJson) {
        return PbSender.sendDirect(elementsJson, chatType, peerUid);
    }

    public static boolean sendElementAuto(String elementsJson) {
        if (!refreshChat()) return false;
        return sendElement(CurrentChat.chatType(), CurrentChat.peerUid(), elementsJson);
    }

    public static boolean sendLong(int chatType, String peerUid, String elementsJson) {
        return PbSender.sendLong(elementsJson, chatType, peerUid);
    }

    public static boolean sendLongAuto(String elementsJson) {
        if (!refreshChat()) return false;
        return sendLong(CurrentChat.chatType(), CurrentChat.peerUid(), elementsJson);
    }

    public static boolean sendRaw(String cmd, byte[] body) {
        if (cmd == null || cmd.length() == 0 || body == null || body.length == 0) return false;
        return PbSender.sendCmd(cmd, body);
    }

    public static boolean sendRawHex(String cmd, String hex) {
        byte[] b = hexToBytes(hex);
        return b != null && sendRaw(cmd, b);
    }

    public static String decodeBytes(byte[] raw) {
        try {
            if (raw == null || raw.length == 0) return "";
            byte[] body = stripWup(raw);
            FunProtoData pd = new FunProtoData();
            pd.fromBytes(body);
            JSONObject o = pd.toJSON();
            if (o.length() == 0) return "";
            return o.toString(2);
        } catch (Throwable t) {
            return "";
        }
    }

    public static String decodeHex(String hex) {
        return decodeBytes(hexToBytes(hex));
    }

    public static String decodeBase64(String b64) {
        try {
            return decodeBytes(android.util.Base64.decode(b64, android.util.Base64.DEFAULT));
        } catch (Throwable t) {
            return "";
        }
    }

    public static byte[] encodeJson(String json) {
        try {
            if (json == null) return null;
            String t = json.trim();
            if (!t.startsWith("{")) return null;
            JSONObject o = new JSONObject(t);
            FunProtoData pd = new FunProtoData();
            pd.fromJSON(o);
            byte[] b = pd.toBytes();
            return (b == null || b.length == 0) ? null : b;
        } catch (Throwable t) {
            return null;
        }
    }

    public static String encodeJsonHex(String json) {
        byte[] b = encodeJson(json);
        return b == null ? "" : bytesToHex(b, true);
    }

    public static String templatesJson() {
        try {
            List<TemplateStore.Item> list = TemplateStore.all(QQEnv.topActivity());
            JSONArray arr = new JSONArray();
            for (TemplateStore.Item it : list) {
                JSONObject o = new JSONObject();
                o.put("name", it.name);
                o.put("mode", it.mode);
                o.put("size", it.content == null ? 0 : it.content.length());
                arr.put(o);
            }
            return arr.toString(2);
        } catch (Throwable t) {
            return "[]";
        }
    }

    public static String templateContent(String name) {
        try {
            for (TemplateStore.Item it : TemplateStore.all(QQEnv.topActivity())) {
                if (it.name.equals(name)) return it.content;
            }
        } catch (Throwable ignore) {
        }
        return "";
    }

    public static boolean saveTemplate(String name, int mode, String content) {
        try {
            return TemplateStore.save(QQEnv.topActivity(), name, mode, content);
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean removeTemplate(String name) {
        try {
            return TemplateStore.remove(QQEnv.topActivity(), name);
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean renameTemplate(String oldName, String newName) {
        try {
            return TemplateStore.rename(QQEnv.topActivity(), oldName, newName);
        } catch (Throwable t) {
            return false;
        }
    }

    public static String exportTemplates() {
        try {
            return TemplateStore.export(QQEnv.topActivity());
        } catch (Throwable t) {
            return "{\"items\":[]}";
        }
    }

    public static boolean forceLong() {
        try {
            return HookConfig.getBoolean(QQEnv.topActivity(), KEY_FORCE_LONG, false);
        } catch (Throwable t) {
            return false;
        }
    }

    public static void setForceLong(boolean on) {
        try {
            HookConfig.set(QQEnv.topActivity(), KEY_FORCE_LONG, on ? "1" : "0");
        } catch (Throwable ignore) {
        }
    }

    public static byte[] hexToBytes(String hex) {
        try {
            if (hex == null) return null;
            String h = hex.replaceAll("0x", "").replaceAll("[^0-9a-fA-F]", "");
            if (h.length() == 0 || h.length() % 2 != 0) return null;
            byte[] out = new byte[h.length() / 2];
            for (int i = 0; i < out.length; i++) {
                out[i] = (byte) Integer.parseInt(h.substring(i * 2, i * 2 + 2), 16);
            }
            return out;
        } catch (Throwable t) {
            return null;
        }
    }

    public static String bytesToHex(byte[] b, boolean grouped) {
        if (b == null) return "";
        StringBuilder sb = new StringBuilder(b.length * 3);
        for (int i = 0; i < b.length; i++) {
            sb.append(String.format("%02X", b[i] & 0xFF));
            if (grouped) {
                if (i % 16 == 15) sb.append('\n');
                else if (i % 4 == 3) sb.append(' ');
            }
        }
        return sb.toString().trim();
    }

    public static byte[] stripWup(byte[] b) {
        try {
            if (b == null || b.length < 5) return b;
            long len = ((long) (b[0] & 0xFF) << 24) | ((b[1] & 0xFF) << 16)
                    | ((b[2] & 0xFF) << 8) | (b[3] & 0xFF);
            if (len == b.length - 4) {
                byte[] r = new byte[b.length - 4];
                System.arraycopy(b, 4, r, 0, r.length);
                return r;
            }
        } catch (Throwable ignore) {
        }
        return b;
    }

    public static String uinToUid(String uin) {
        try {
            String u = QQUtils.uidFromUin(uin);
            return u == null ? "" : u;
        } catch (Throwable t) {
            return "";
        }
    }
}
