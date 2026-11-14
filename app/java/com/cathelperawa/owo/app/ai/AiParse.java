package com.cathelperawa.owo.app.ai;

import com.cathelperawa.owo.app.api.MsgApi;
import com.cathelperawa.owo.app.api.ParseApi;
import com.cathelperawa.owo.app.core.ConfigStore;
import com.cathelperawa.owo.app.core.HookLog;

import org.json.JSONArray;
import org.json.JSONObject;

public final class AiParse {

    private AiParse() { }

    public static final String K_ON = "ai_parse_on";

    public static boolean enabled() {
        try {
            return ConfigStore.getBool(K_ON);
        } catch (Throwable t) {
            return false;
        }
    }

    public static void setEnabled(boolean on) {
        try {
            ConfigStore.setBool(K_ON, on);
        } catch (Throwable ignore) {
        }
    }

    public static String music(int chatType, String peerUid, String query, String mode) {
        try {
            if (chatType <= 0 || peerUid == null || peerUid.length() == 0) {
                return "（拿不到当前会话，没发）";
            }
            if (query == null || query.trim().length() == 0) return "（歌名是空的）";

            String m = mode == null ? "" : mode.trim().toLowerCase();
            if (m.length() == 0) m = "card";

            JSONArray arr = new JSONArray(ParseApi.searchMusic(query.trim(), 5));
            if (arr.length() == 0) {
                return "（没搜到「" + query + "」，换个写法：歌名 + 歌手）";
            }
            JSONObject first = arr.optJSONObject(0);
            if (first == null) return "（搜索结果为空）";

            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o == null) continue;
                String nm = o.optString("name", "");
                if (nm.contains("伴奏") || nm.contains("片段") || nm.contains("纯音乐")
                        || nm.contains("铃声")) continue;
                first = o;
                break;
            }

            String id = first.optString("id", "");
            String name = first.optString("name", "");
            String artist = first.optString("artist", "");
            String pic = first.optString("pic", "");

            boolean ok;
            String what;
            if ("card".equals(m)) {

                ok = ParseApi.sendMusicCard(chatType, peerUid, id, name, artist, pic);
                what = "音乐卡";
                if (!ok) {
                    HookLog.w("发歌: 音乐卡失败，退回图片卡「" + name + "」");
                    ok = ParseApi.sendMusicImage(chatType, peerUid, id, name, artist);
                    what = "图片卡（音源拿不到）";
                }
            } else if ("file".equals(m)) {
                ok = ParseApi.sendMusicFile(chatType, peerUid, id, name, artist);
                what = "音频文件";
            } else if ("image".equals(m)) {
                ok = ParseApi.sendMusicImage(chatType, peerUid, id, name, artist);
                what = "图片卡片";
            } else {
                ok = ParseApi.sendMusicVoice(chatType, peerUid, id, name, artist, pic, 0);
                what = "语音条";
            }
            AiDebug.log("  · 发歌: 「" + name + " - " + artist + "」→ " + what
                    + (ok ? " 成功" : " 失败"));

            if (!ok) {
                String why = com.cathelperawa.owo.app.feature.MusicApi.lastError;
                boolean noLogin = false;
                try {
                    noLogin = com.cathelperawa.owo.app.cookie.CookieBridge.get("netease") == null;
                } catch (Throwable ignore) {
                }
                String body = "（找到「" + name + " - " + artist + "」但发送失败了"
                        + (why != null && why.length() > 0 ? "：" + why : "，可能没版权或网络问题")
                        + "）";
                if (noLogin) {
                    body += "\n提示：网易云还没登录，VIP 歌只能拿到 30 秒试听；"
                            + "让用户到「网页登录 Cookie」登录一次就能放完整版。";
                }
                return body;
            }
            return "已发出：「" + name + " - " + artist + "」（" + what + "）"
                    + "。另外还有 " + (arr.length() - 1) + " 个同名/相似结果。";
        } catch (Throwable t) {
            HookLog.e("AiParse.music 异常", t);
            return "（发歌出错：" + t + "）";
        }
    }

    public static String video(int chatType, String peerUid, String url, String mode) {
        try {
            if (chatType <= 0 || peerUid == null || peerUid.length() == 0) {
                return "（拿不到当前会话，没发）";
            }
            if (url == null || url.trim().length() == 0) return "（链接是空的）";

            String m = mode == null ? "" : mode.trim().toLowerCase();
            if (!"voice".equals(m) && !"video".equals(m)) m = "video";

            String infoJson = ParseApi.biliInfo(url.trim());
            JSONObject info = new JSONObject(infoJson);
            String title = info.optString("title", "");
            String err = info.optString("error", "");
            if (err.length() > 0 && !info.has("videoUrl")) {
                return "（解析失败：" + err + "。目前只支持 B站 的公开视频）";
            }

            boolean ok;
            if ("voice".equals(m)) {
                ok = ParseApi.biliSendVoice(chatType, peerUid, url.trim(), 0);
            } else {
                ok = ParseApi.biliSendVideo(chatType, peerUid, url.trim(), 0);
            }
            AiDebug.log("  · 解析视频: 「" + title + "」→ " + m + (ok ? " 成功" : " 失败"));

            if (!ok) {
                return "（解析到「" + title + "」但发送失败，可能文件太大或没有音轨）";
            }
            return "已发出：「" + title + "」（" + ("voice".equals(m) ? "语音" : "视频") + "）";
        } catch (Throwable t) {
            HookLog.e("AiParse.video 异常", t);
            return "（解析视频出错：" + t + "）";
        }
    }
}
