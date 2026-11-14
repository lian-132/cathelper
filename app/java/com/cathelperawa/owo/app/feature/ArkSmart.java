package com.cathelperawa.owo.app.feature;

import android.content.Context;

import com.cathelperawa.owo.app.bridge.CatToast;
import com.cathelperawa.owo.app.bridge.CurrentChat;
import com.cathelperawa.owo.app.core.Bg;
import com.cathelperawa.owo.app.core.HookLog;

import org.json.JSONObject;

public final class ArkSmart {

    public static final int CH_RAW = 0;

    public static final int CH_SSO = 1;

    public static final int CH_MINI = 2;

    public static final class Kind {
        public final String name;
        public final String channel;
        public final int ch;
        public final String refer;

        Kind(String name, String channel, int ch, String refer) {
            this.name = name;
            this.channel = channel;
            this.ch = ch;
            this.refer = refer;
        }

        @Override public String toString() { return name + " · " + channel; }
    }

    public interface OnResult {

        void run(boolean ok, String msg);
    }

    private ArkSmart() {}

    public static Kind classify(String arkJson) {
        String app = "", view = "", desc = "", tips = "";
        String shareType = "", shareUrl = "";
        try {
            JSONObject a = new JSONObject(arkJson);
            app = a.optString("app", "");
            view = a.optString("view", "");
            desc = a.optString("desc", "");
            tips = a.optString("tips", "");
            Object metaRaw = a.opt("meta");
            JSONObject meta = null;
            if (metaRaw instanceof JSONObject) {
                meta = (JSONObject) metaRaw;
            } else if (metaRaw instanceof String) {
                String s = ((String) metaRaw).trim();
                if (s.startsWith("{")) meta = new JSONObject(s);
            }
            if (meta != null) {
                JSONObject sd = meta.optJSONObject("shareData");
                if (sd != null) {
                    shareType = sd.optString("type", "").toLowerCase();
                    shareUrl = sd.optString("url", "").toLowerCase();
                }
                if (view.length() == 0) view = meta.optString("view", "");
            }
        } catch (Throwable ignore) {
        }

        String appL = app.toLowerCase();
        String viewL = view.toLowerCase();

        if (appL.contains("tencent.forum") || appL.equals("com.tencent.forum")
                || (viewL.equals("rank") && desc.contains("频道"))
                || desc.contains("频道帖子")) {
            return new Kind("频道帖子卡（forum/rank）", "原生免签", CH_RAW, "");
        }

        if (appL.contains("miniapp") || appL.contains("mini_app") || viewL.contains("miniapp")
                || viewL.equals("miniapp") || desc.contains("小程序")) {
            if (viewL.contains("video") || shareType.equals("video")) {
                return new Kind("小程序视频卡", "SsoArkSign", CH_SSO, "https://m.q.qq.com/");
            }
            return new Kind("小程序模板卡片", "SsoArkSign", CH_SSO, "https://m.q.qq.com/");
        }

        if (shareType.equals("video") || shareUrl.endsWith(".mp4")
                || viewL.contains("videoview") || desc.contains("视频")) {
            return new Kind("音视频卡", "SsoArkSign", CH_SSO, "https://x.jq.qq.com/");
        }

        if (appL.contains("egameshare") || appL.contains("gamecenter") || desc.contains("游戏分享")) {
            return new Kind("游戏分享卡（egameshare）", "SsoArkSign", CH_SSO, "https://x.jq.qq.com/");
        }

        if (appL.contains("gdt") || appL.contains("news") || viewL.contains("picview")
                || viewL.contains("tuwen") || viewL.contains("newsview")
                || desc.contains("图文") || tips.contains("News")) {
            return new Kind("图文 News 卡", "0xb77_9 免签", CH_RAW, "https://x.jq.qq.com/");
        }

        if (appL.contains("qqsign") || arkJson.indexOf("\"sign\"") >= 0) {
            return new Kind("QQSign 卡片", "SsoArkSign", CH_SSO, "https://x.jq.qq.com/");
        }

        if (appL.contains("contact") || viewL.contains("contact")) {
            return new Kind("名片卡", "SsoArkSign", CH_SSO, "https://x.jq.qq.com/");
        }
        if (viewL.contains("announce") || desc.contains("群公告")) {
            return new Kind("群公告卡", "SsoArkSign", CH_SSO, "https://x.jq.qq.com/");
        }

        if (viewL.contains("dataview") || shareType.length() > 0) {
            return new Kind("游戏分享卡（dataView）", "SsoArkSign", CH_SSO, "https://x.jq.qq.com/");
        }
        return new Kind("通用 ARK 卡片", "SsoArkSign", CH_SSO, "https://x.jq.qq.com/");
    }

    public static String badge(String arkJson) {
        Kind k = classify(arkJson);
        return "已识别：" + k.name + " · 走 " + k.channel;
    }

    public static void send(final Context c, final String arkJson, final OnResult cb) {
        if (arkJson == null || arkJson.trim().length() == 0) {
            done(cb, false, "ARK 是空的喵");
            return;
        }
        if (!CurrentChat.has()) {
            done(cb, false, "先在聊天里点开一个会话");
            return;
        }
        final Kind kind = classify(arkJson);
        CatToast.show("已识别：" + kind.name + " · 正在签名…");
        Bg.run(new Runnable() {
            @Override public void run() {
                String out = null;
                String err = null;
                try {
                    if (kind.ch == CH_RAW) {
                        out = arkJson;
                    } else {

                        SsoArkSign.Result r = SsoArkSign.sign(arkJson, kind.refer);
                        if (r != null && r.ok()) {
                            out = r.json;
                        } else {
                            err = r == null ? "无返回" : r.error;

                            try {
                                ArkSigner.Result r2 = ArkSigner.signByChannel(arkJson, "auto");
                                if (r2 != null && r2.ok()) {
                                    out = r2.json;
                                    err = null;
                                } else if (r2 != null) {
                                    err = err + " / " + r2.error;
                                }
                            } catch (Throwable t) {
                                HookLog.e("ArkSmart.fallback", t);
                            }
                        }
                    }
                } catch (Throwable t) {
                    HookLog.e("ArkSmart.sign", t);
                    err = String.valueOf(t);
                }

                boolean signed = out != null;
                String payload = signed ? out : arkJson;
                boolean sent = rawSend(payload);
                final String kname = kind.name, kch = kind.channel;
                if (sent) {
                    if (signed) {
                        done(cb, true, "已识别 " + kname + "，签名后发出（" + kch + "）");
                    } else {
                        done(cb, true, "已识别 " + kname + "，未签上（" + err + "），原样发出");
                    }
                } else {
                    done(cb, false, "已识别 " + kname + "，签名" + (signed ? "成功" : "失败：" + err)
                            + "，但发送失败");
                }
            }
        }, "ark-smart-send");
    }

    public static boolean rawSend(String arkJson) {
        try {
            int ct = CurrentChat.chatType();
            String peer = CurrentChat.peerUid();
            if (com.cathelperawa.owo.app.ui.SendPanel.sendArkNative(arkJson, ct, peer)) return true;
            return com.cathelperawa.owo.app.proto.PbSender.sendDirect(
                    com.cathelperawa.owo.app.ui.SendPanel.wrapArk(arkJson), ct, peer);
        } catch (Throwable t) {
            HookLog.e("ArkSmart.rawSend", t);
            return false;
        }
    }

    private static void done(final OnResult cb, final boolean ok, final String msg) {
        try {
            CatToast.show(msg);
        } catch (Throwable ignore) {
        }
        if (cb != null) {
            try {
                new android.os.Handler(android.os.Looper.getMainLooper())
                        .post(new Runnable() {
                            @Override public void run() { cb.run(ok, msg); }
                        });
            } catch (Throwable ignore) {
            }
        }
    }
}
