package com.cathelperawa.owo.app.feature;

import android.content.Context;

import com.cathelperawa.owo.app.bridge.CurrentChat;
import com.cathelperawa.owo.app.bridge.QQEnv;
import com.cathelperawa.owo.app.core.HookLog;
import com.cathelperawa.owo.app.ovo.QPacketHelper;
import com.cathelperawa.owo.app.ovo.QQInterfaces;

import org.json.JSONObject;

public final class PlainNewsCard {

    public static final String CMD = "OidbSvc.0xb77_9";

    private static final long APPID = 100951776L;
    private static final String FAKE_PKG = "tv.danmaku.bili";
    private static final String FAKE_SIGN = "7194d531cbe7960a22007b9f6bdaa38b";
    private static final long MAGIC_ID = 7538753094889419000L;

    private PlainNewsCard() {
    }

    public static final class Result {
        public boolean ok;
        public String error = "";
    }

    public static Result send(Context ctx, String tag, String title, String desc,
                              String jump, String preview) {
        Result r = new Result();
        try {
            if (ctx instanceof android.app.Activity) {
                try {
                    CurrentChat.fromActivity((android.app.Activity) ctx);
                } catch (Throwable ignore) {
                }
            }
            if (title == null) title = "";
            title = title.trim();
            if (title.length() == 0) {
                r.error = "标题不能空";
                return r;
            }

            int ct = CurrentChat.chatType();
            int chatFlag = ct >= 1 ? ct - 1 : 0;
            long peer = targetUin();

            org.json.JSONObject news = new org.json.JSONObject();
            news.put("title", title);
            news.put("desc", nz(desc));
            news.put("preview", nz(preview));
            news.put("jumpUrl", nz(jump));
            news.put("tag", nz(tag));
            if (nz(tag).length() > 0) {
                news.put("tagIcon",
                        "https://tianxuan.gtimg.cn/47329_bd95e16e/assets/guild-icon.png");
            }
            org.json.JSONObject meta = new org.json.JSONObject();
            meta.put("news", news);
            org.json.JSONObject ark = new org.json.JSONObject();
            ark.put("app", "com.tencent.tuwen.lua");
            ark.put("view", "news");
            ark.put("meta", meta);
            ark.put("config", new org.json.JSONObject().put("forward", 1).put("type", "normal"));
            ark.put("ver", "0.0.0.1");
            ark.put("prompt", title);

            String tpl = "{\"1\":2935,\"2\":9,\"4\":{"
                    + "\"1\":" + APPID + ",\"2\":1,"
                    + "\"5\":{\"1\":2,\"2\":\"0.0.0\","
                    + "\"3\":" + JSONObject.quote(FAKE_PKG) + ","
                    + "\"4\":" + JSONObject.quote(FAKE_SIGN) + "},"
                    + "\"16\":{\"1\":" + APPID + ",\"2\":1,"
                    + "\"10\":" + JSONObject.quote(ark.toString()) + "},"
                    + "\"7\":{\"15\":" + MAGIC_ID + "},"
                    + "\"10\":" + chatFlag + ","
                    + "\"11\":" + peer + ","
                    + "\"12\":{"
                    + "\"10\":" + JSONObject.quote(nz(tag)) + ","
                    + "\"11\":" + JSONObject.quote(title) + ","
                    + "\"12\":" + JSONObject.quote(nz(desc)) + ","
                    + "\"13\":" + JSONObject.quote(nz(jump)) + ","
                    + "\"14\":" + JSONObject.quote(nz(preview))
                    + "}},"
                    + "\"6\":\"android 9.1.35\"}";

            byte[] pb = QPacketHelper.buildMessage(new JSONObject(tpl).toString());
            if (pb == null || pb.length == 0) {
                r.error = "组包失败";
                return r;
            }
            boolean sent = QQInterfaces.sendBuffer(CMD, true, pb);
            if (!sent) {
                r.error = "通道未就绪";
                return r;
            }
            r.ok = true;
            return r;
        } catch (Throwable t) {
            HookLog.e("PlainNewsCard 失败", t);
            r.error = String.valueOf(t);
            return r;
        }
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    private static long targetUin() {
        int ct = CurrentChat.chatType();
        if (ct == 1) {
            try {
                long v = parseLong(com.cathelperawa.owo.app.bridge.QQUtils
                        .uinFromUid(CurrentChat.peerUid()));
                if (v > 0) return v;
            } catch (Throwable ignore) {
            }
            long v = parseLong(CurrentChat.peerRaw());
            if (v > 0) return v;
        } else {
            long v = parseLong(CurrentChat.peerRaw());
            if (v > 0) return v;
            v = parseLong(CurrentChat.peerUid());
            if (v > 0) return v;
        }
        long v = parseLong(CurrentChat.peerRaw());
        if (v > 0) return v;
        v = parseLong(CurrentChat.peerUid());
        if (v > 0) return v;
        return parseLong(QQEnv.currentUin());
    }

    private static long parseLong(String s) {
        if (s == null) return 0L;
        s = s.trim();
        if (s.length() == 0) return 0L;
        try {
            return Long.parseLong(s);
        } catch (Throwable t) {
            return 0L;
        }
    }
}
