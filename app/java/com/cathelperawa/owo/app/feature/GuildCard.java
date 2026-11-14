package com.cathelperawa.owo.app.feature;

import com.cathelperawa.owo.app.core.HookLog;

import org.json.JSONArray;
import org.json.JSONObject;

import java.security.MessageDigest;
import java.util.Random;

public final class GuildCard {

    public static final String APP = "com.tencent.forum";
    public static final String VIEW = "rank";
    public static final String DESC = "频道帖子";

    public static final String JUMP = "mqqguild://guild/openfeeddetail?";

    public static final class Fields {
        public String title = "";
        public String content = "";
        public String guildName = "";
        public String guildId = "";
        public String guildIcon = "";
        public String channelName = "帖子广场";
        public String channelId = "0";
        public String posterNick = "";
        public String posterAvatar = "";
        public String posterTinyId = "0";
        public String feedId = "";
        public String jumpUrl = "";
        public String token = "";
        public long ctime = 0;
        public String emojiId = "";

        public String cover = "";
        public int picW = 400;
        public int picH = 500;
        public int picNum = 1;
        public String feedCreateTime = "";
        public String shareUrl = "";
    }

    private GuildCard() {
    }

    public static String build(Fields f) {
        if (f == null) return null;
        String title = f.title == null ? "" : f.title.trim();
        if (title.length() == 0) return null;

        long ctime = f.ctime > 0 ? f.ctime : System.currentTimeMillis() / 1000L;
        String token = nz(f.token);
        if (token.length() == 0) token = md5("forum:" + ctime + ":" + title + ":" + new Random().nextInt());
        String feedId = nz(f.feedId);
        if (feedId.length() == 0) feedId = genFeedId();
        String jump = nz(f.jumpUrl);
        if (jump.length() == 0) jump = JUMP;
        String content = nz(f.content).length() == 0 ? title : f.content;

        try {
            JSONObject channel = new JSONObject();
            channel.put("channel_id", toLong(f.channelId));
            channel.put("channel_name", nz(f.channelName).length() == 0 ? "帖子广场" : f.channelName);
            channel.put("guild_icon", nz(f.guildIcon));
            channel.put("guild_id", toLong(f.guildId));
            channel.put("guild_name", nz(f.guildName));
            channel.put("str_guild_id", nz(f.guildId));

            JSONArray blocks = new JSONArray();
            blocks.put(new JSONObject()
                    .put("text_content", new JSONObject().put("text", content))
                    .put("type", 1));
            if (nz(f.emojiId).length() > 0) {
                blocks.put(new JSONObject()
                        .put("emoji_content", new JSONObject().put("id", f.emojiId).put("type", "1"))
                        .put("pattern_id", "1")
                        .put("type", 4));
            }

            JSONArray titleBlocks = new JSONArray();
            titleBlocks.put(new JSONObject()
                    .put("text_content", new JSONObject().put("text", title))
                    .put("type", 1));

            JSONObject feed = new JSONObject();
            feed.put("contents", new JSONObject().put("contents", blocks));
            feed.put("create_time", ctime);
            feed.put("title", new JSONObject().put("contents", titleBlocks));

            if (nz(f.cover).length() > 0) {
                JSONArray imgs = new JSONArray();
                imgs.put(new JSONObject()
                        .put("pic_url", f.cover)
                        .put("width", f.picW <= 0 ? 300 : f.picW)
                        .put("height", f.picH <= 0 ? 300 : f.picH)
                        .put("pattern_id", "1"));
                feed.put("images", imgs);
            }

            JSONObject poster = new JSONObject();
            poster.put("avatar", nz(f.posterAvatar));
            poster.put("nick", nz(f.posterNick));
            poster.put("str_tiny_id", nz(f.posterTinyId).length() == 0 ? "0" : f.posterTinyId);
            poster.put("tiny_id", toLong(f.posterTinyId));

            JSONObject detail = new JSONObject();
            detail.put("channel_info", channel);
            detail.put("create_at", ctime);
            detail.put("duration", 86400000000000L);
            detail.put("feed", feed);
            detail.put("feed_id", feedId);
            detail.put("jump_url", jump);
            detail.put("poster", poster);
            detail.put("token", String.valueOf(ctime));

            JSONObject config = new JSONObject();
            config.put("ctime", ctime);
            config.put("extendAutoSize", 1);
            config.put("token", token);
            config.put("type", "normal");

            JSONObject ark = new JSONObject();
            ark.put("app", APP);
            ark.put("config", config);
            ark.put("desc", DESC);
            ark.put("meta", new JSONObject().put("detail", detail));
            ark.put("prompt", "[频道帖子]" + title);
            ark.put("ver", "1.0.0.1");
            ark.put("view", VIEW);
            return ark.toString();
        } catch (Throwable t) {
            HookLog.e("GuildCard.build 失败", t);
            return null;
        }
    }

    public static boolean send(Fields f) {
        String ark = build(f);
        if (ark == null) {
            HookLog.w("GuildCard.send: 标题空了");
            return false;
        }
        autoCapStart();
        boolean ok = ArkSmart.rawSend(ark);
        autoCapDump();
        return ok;
    }

    private static void autoCapStart() {
        try {
            com.cathelperawa.owo.app.proto.PbSender.startCaptureAll();
            HookLog.i("GuildCard: 自动抓包开始");
        } catch (Throwable t) {
            HookLog.w("autoCapStart 失败: " + t);
        }
    }

    private static void autoCapDump() {
        new Thread(new Runnable() {
            @Override public void run() {
                try {
                    Thread.sleep(1500);
                    com.cathelperawa.owo.app.proto.PbSender.stopCaptureAll();
                    android.content.Context c =
                            com.cathelperawa.owo.app.bridge.QQEnv.appContext();
                    if (c == null) return;
                    java.io.File src = new java.io.File(
                            new java.io.File(c.getFilesDir().getParent(), "files/cathelper"),
                            "capture_all.txt");
                    if (!src.exists()) return;
                    java.io.File dstDir = c.getExternalFilesDir(null);
                    if (dstDir == null) return;
                    java.io.File dst = new java.io.File(dstDir, "guild_cap.txt");
                    java.io.FileInputStream in = new java.io.FileInputStream(src);
                    java.io.FileOutputStream out = new java.io.FileOutputStream(dst);
                    byte[] buf = new byte[8192];
                    int n;
                    while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
                    out.flush(); out.close(); in.close();
                    dst.setReadable(true, false);
                    HookLog.i("GuildCard: 抓包已存 " + dst.getAbsolutePath());
                } catch (Throwable t) {
                    HookLog.w("autoCapDump 失败: " + t);
                }
            }
        }, "guild-autocap").start();
    }

    private static final long SHELL_APPID = 100951776L;
    private static final String SHELL_PKG = "com.tencent.mobileqq";
    private static final String SHELL_SIGN = "a6b745bf24a2c277527716f6f36eb68d";
    private static final long MAGIC_ID = 7538753094889419000L;

    public static final class Result {
        public boolean ok;
        public int rc = -1;
        public String error = "";
        @Override public String toString() { return ok ? "ok" : ("失败：" + error); }
    }

    public static String describeRc(int rc) {
        if (rc == 0) return "服务端接受";
        if (rc == -1) return "回包解析失败";
        if (rc == -2) return "通道未就绪";
        if (rc == -999) return "已投递但无回包（可能被静默丢弃）";
        return "服务端拒绝 rc=" + rc;
    }

    public static Fields sample() {
        Fields f = new Fields();
        f.title = "何意味";
        f.content = "何意味";
        f.emojiId = "337";
        f.guildName = "示例频道";
        f.guildId = "100000000";
        f.guildIcon = "";
        f.channelName = "帖子广场";
        f.channelId = "745336840";
        f.posterNick = "\uFFFC";
        f.posterAvatar = "";
        f.posterTinyId = "0";
        f.feedId = "B_0000000000000000";
        f.token = "";
        f.ctime = 1794130160L;
        return f;
    }

    public static Fields sampleFeed() {
        Fields f = new Fields();
        f.title = "#张元英 ";
        f.content = "";
        f.cover = "https://channel.qpic.cn/psc?/channel/NaDwC23LjvXqrn3RH.9z8dX.EHNKzA5DYtZAy49armmGChWzpa7IPFUTEd*sgKQOhzhPF4Bv2x7Yja3wb6luGyEuQO2*x4aS7OpmJN4SkME!/c=&bo=oAUIB6AFCAcWADA!&ek=1&tl=1";
        f.picW = 400;
        f.picH = 500;
        f.picNum = 1;
        f.feedId = "B_0000000000000001";
        f.posterTinyId = "0";
        f.token = "";
        f.ctime = 1794131263L;
        f.shareUrl = "";
        return f;
    }

    public static final String FEED_APP = "com.tencent.feed.lua";
    public static final String FEED_VIEW = "feed";
    public static final String FEED_BIZSRC = "pindao.picforum";
    public static final String TAG_ICON =
            "http://downv6.qq.com/innovate/guild/tail_guild_logo.png";

    public static String buildFeed(Fields f) {
        if (f == null) return null;
        String title = nz(f.title).trim();
        if (title.length() == 0) return null;

        long ctime = f.ctime > 0 ? f.ctime : System.currentTimeMillis() / 1000L;
        String token = nz(f.token);
        if (token.length() == 0)
            token = md5("feed:" + ctime + ":" + title + ":" + new Random().nextInt());
        String feedId = nz(f.feedId);
        if (feedId.length() == 0) feedId = genFeedId();

        try {
            JSONObject shareBiz = new JSONObject();
            shareBiz.put("feed_id", feedId);
            shareBiz.put("creat_time", ctime);
            shareBiz.put("poster_tiny_id", toLong(nz(f.posterTinyId)));
            JSONObject busiData = new JSONObject();
            busiData.put("share_biz_data", shareBiz);

            String jump = nz(f.shareUrl);
            if (jump.length() == 0)
                jump = "https://pd.qq.com/qqweb/qunpro/hs?_wv=3&_wwv=128"
                        + "&appChannel=share&businessType=2&openSource=2";

            JSONObject feed = new JSONObject();
            feed.put("cover", nz(f.cover));
            feed.put("title", title);
            feed.put("tagIcon", TAG_ICON);
            feed.put("tagName", "腾讯频道");
            feed.put("picNum", f.picNum <= 0 ? 1 : f.picNum);
            if (nz(f.cover).length() > 0) {
                JSONObject info = new JSONObject();
                info.put("width", f.picW <= 0 ? 400 : f.picW);
                info.put("height", f.picH <= 0 ? 500 : f.picH);
                feed.put("imageInfo", info);
            }
            feed.put("busiData", busiData.toString());
            feed.put("jumpUrl", jump);
            feed.put("ark_reserved1", "1");
            feed.put("ark_reserved2", "1");
            feed.put("ark_reserved3", feedId);

            JSONObject config = new JSONObject();
            config.put("autosize", 0);
            config.put("collect", 1);
            config.put("ctime", ctime);
            config.put("forward", 1);
            config.put("reply", 1);
            config.put("round", 1);
            config.put("token", token);
            config.put("type", "normal");

            JSONObject ark = new JSONObject();
            ark.put("app", FEED_APP);
            ark.put("desc", "");
            ark.put("view", FEED_VIEW);
            ark.put("bizsrc", FEED_BIZSRC);
            ark.put("ver", "0.0.0.1");
            ark.put("prompt", "[分享帖子] " + title);
            ark.put("appID", "");
            ark.put("sourceName", "");
            ark.put("actionData", "");
            ark.put("actionData_A", "");
            ark.put("sourceUrl", "");
            ark.put("meta", new JSONObject().put("feed", feed));
            ark.put("config", config);
            ark.put("text", "");
            ark.put("extraApps", new JSONArray());
            ark.put("sourceAd", "");
            ark.put("extra", "");
            return ark.toString();
        } catch (Throwable t) {
            HookLog.e("GuildCard.buildFeed 失败", t);
            return null;
        }
    }

    public static boolean sendFeed(Fields f) {
        String ark = buildFeed(f);
        if (ark == null) {
            HookLog.w("GuildCard.sendFeed: 标题空了");
            return false;
        }
        return ArkSmart.rawSend(ark);
    }

    public static int sendPb(Fields f) {

        return sendReplayPatched(f);
    }

    public static int sendReplayRepack() {
        try {
            byte[] tmpl = hexToBytes(RAW_PB_HEX);
            if (tmpl == null) return -6;
            byte[] arkPayload = SharePatch.extractArk(tmpl);
            if (arkPayload == null) return -7;
            String json = SharePatch.inflateArk(arkPayload);
            if (json == null) return -7;
            byte[] packed = SharePatch.packArk(json);
            if (packed == null) return -4;
            HookLog.i("sendReplayRepack 原始ARK=" + arkPayload.length + " 重压=" + packed.length);
            byte[] pb = SharePatch.replaceArkPayload(tmpl, packed);
            if (pb == null) return -5;
            byte[] rs = SharePatch.reseed(pb);
            if (rs != null) pb = rs;
            HookLog.i("sendReplayRepack 包 " + tmpl.length + " -> " + pb.length);
            return com.cathelperawa.owo.app.proto.PbSender
                    .sendNativeAndWait("MessageSvc.PbSendMsg", pb, 8000);
        } catch (Throwable t) {
            HookLog.e("sendReplayRepack", t);
            return -1;
        }
    }

    public static Result sendChannelSigned(Fields f) {
        Result r = new Result();
        try {
            String title = nz(f == null ? "" : f.title).trim();
            if (title.length() == 0) {
                r.error = "标题空了";
                return r;
            }
            String desc = nz(f.content).length() == 0 ? title : f.content;
            String jump = nz(f.shareUrl);
            if (jump.length() == 0) {
                jump = "https://qun.qq.com/qunng/guild/tianxuan/p/53049_a53f0edz?traceTint=tianxuan_copy";
            }
            org.json.JSONObject news = new org.json.JSONObject();
            news.put("title", title);
            news.put("desc", desc);
            String tag = nz(f.channelName).length() == 0 ? "QQ频道" : f.channelName;
            String tagIcon = nz(f.guildIcon).length() == 0
                    ? "https://tianxuan.gtimg.cn/47329_bd95e16e/assets/guild-icon.png" : f.guildIcon;
            news.put("tag", tag);
            news.put("tagIcon", tagIcon);
            news.put("preview", nz(f.cover));
            news.put("jumpUrl", jump);
            String meta = new org.json.JSONObject().put("news", news).toString();

            ArkSigner.Result sr = ArkSigner.signCustomArkBiz(
                    "com.tencent.tuwen.lua", "guild.share", title, meta);
            if (sr == null || sr.json == null || sr.json.length() == 0) {
                r.error = sr == null ? "签名无返回" : sr.error;
                HookLog.w("sendChannelSigned 签名失败: " + r.error);
                return r;
            }
            HookLog.i("sendChannelSigned 已签名 len=" + sr.json.length());
            boolean ok = ArkSmart.rawSend(sr.json);
            r.ok = ok;
            if (!ok) r.error = "签名成功但发送失败";
            return r;
        } catch (Throwable t) {
            HookLog.e("sendChannelSigned", t);
            r.error = String.valueOf(t);
            return r;
        }
    }

    public static Result sendForumSigned(Fields f) {
        Result r = new Result();
        try {
            String arkJson = build(f);
            if (arkJson == null) {
                r.error = "标题空了";
                return r;
            }
            org.json.JSONObject ark = new org.json.JSONObject(arkJson);
            org.json.JSONObject metaObj = ark.optJSONObject("meta");
            if (metaObj == null) {
                r.error = "meta 为空";
                return r;
            }
            String prompt = ark.optString("prompt", "");
            ArkSigner.Result sr = ArkSigner.signCustomArkBiz(
                    "com.tencent.forum", "guild.share", prompt, metaObj.toString());
            if (sr == null || sr.json == null || sr.json.length() == 0) {
                r.error = sr == null ? "签名无返回" : sr.error;
                HookLog.w("sendForumSigned 失败: " + r.error);
                return r;
            }
            HookLog.i("sendForumSigned 已签名 len=" + sr.json.length());
            boolean ok = ArkSmart.rawSend(sr.json);
            r.ok = ok;
            if (!ok) r.error = "签名成功但发送失败";
            return r;
        } catch (Throwable t) {
            HookLog.e("sendForumSigned", t);
            r.error = String.valueOf(t);
            return r;
        }
    }

    public static int sendGuildClean(Fields f) {
        try {
            String ark = build(f);
            if (ark == null) return -3;
            byte[] packed = SharePatch.packArk(ark);
            if (packed == null) return -4;
            byte[] tmpl = hexToBytes(RAW_PB_HEX);
            if (tmpl == null) return -6;
            byte[] trimmed = SharePatch.keepOnlyFirstElem(tmpl);
            if (trimmed == null) {
                HookLog.w("sendGuildClean: 删元素失败");
                return -9;
            }
            byte[] pb = SharePatch.replaceArkPayload(trimmed, packed);
            if (pb == null) return -5;
            byte[] rs = SharePatch.reseed(pb);
            if (rs != null) pb = rs;
            HookLog.i("sendGuildClean " + tmpl.length + " -> " + pb.length);
            return com.cathelperawa.owo.app.proto.PbSender
                    .sendNativeAndWait("MessageSvc.PbSendMsg", pb, 8000);
        } catch (Throwable t) {
            HookLog.e("sendGuildClean", t);
            return -1;
        }
    }

    public static int sendReplayPatched(Fields f) {
        try {
            byte[] tmpl = hexToBytes(RAW_PB_HEX);
            if (tmpl == null) return -6;
            byte[] arkPayload = SharePatch.extractArk(tmpl);
            if (arkPayload == null) {
                HookLog.w("sendReplayPatched: 模板里没抓到 ARK");
                return -7;
            }
            String json = SharePatch.inflateArk(arkPayload);
            if (json == null) return -7;
            String changed = SharePatch.patchText(json,
                    f == null ? "" : nz(f.title), f == null ? "" : nz(f.cover));
            if (changed == null) {
                HookLog.w("sendReplayPatched: 没有可改的字段");
                return -8;
            }
            byte[] packed = SharePatch.packArk(changed);
            if (packed == null) return -4;
            byte[] pb = SharePatch.replaceArkPayload(tmpl, packed);
            if (pb == null) return -5;
            byte[] rs = SharePatch.reseed(pb);
            if (rs != null) pb = rs;
            HookLog.i("sendReplayPatched " + tmpl.length + " -> " + pb.length);
            dumpBytes(packed, "patch_ark.bin");
            dumpBytes(pb, "patch_pb.bin");
            return com.cathelperawa.owo.app.proto.PbSender
                    .sendNativeAndWait("MessageSvc.PbSendMsg", pb, 8000);
        } catch (Throwable t) {
            HookLog.e("sendReplayPatched", t);
            return -1;
        }
    }

    public static int sendReplayRaw() {
        try {
            byte[] pb = hexToBytes(RAW_PB_HEX);
            if (pb == null) return -6;

            byte[] ark = SharePatch.extractArk(pb);
            if (ark != null) {
                String json = SharePatch.inflateArk(ark);
                if (json != null) {
                    byte[] repack = SharePatch.packArk(json);
                    if (repack != null) {
                        byte[] re = SharePatch.replaceArkPayload(pb, repack);
                        if (re != null) {
                            HookLog.i("重压缩 " + ark.length + " -> " + repack.length
                                    + "，包 " + pb.length + " -> " + re.length);
                            pb = re;
                        }
                    }
                }
            }
            byte[] rs = SharePatch.reseed(pb);
            if (rs != null) pb = rs;
            HookLog.i("GuildCard.sendReplayRaw len=" + pb.length);
            return com.cathelperawa.owo.app.proto.PbSender
                    .sendNativeAndWait("MessageSvc.PbSendMsg", pb, 8000);
        } catch (Throwable t) {
            HookLog.e("sendReplayRaw", t);
            return -1;
        }
    }

    private static void dumpBytes(byte[] b, String name) {
        try {
            android.content.Context c = com.cathelperawa.owo.app.bridge.QQEnv.appContext();
            if (c == null || b == null) return;
            java.io.File dir = new java.io.File(c.getFilesDir().getParent(), "files/cathelper");
            if (!dir.exists()) dir.mkdirs();
            StringBuilder sb = new StringBuilder();
            sb.append("# len=").append(b.length).append("\n");
            for (byte x : b) sb.append(String.format("%02x", x & 0xFF));
            sb.append("\n");
            java.io.FileOutputStream fo = new java.io.FileOutputStream(new java.io.File(dir, name), false);
            fo.write(sb.toString().getBytes("UTF-8"));
            fo.flush(); fo.close();
        } catch (Throwable ignore) {
        }
    }

    private static byte[] hexToBytes(String h) {
        try {
            if (h == null) return null;
            String c = h.trim();
            int n = c.length() / 2;
            byte[] out = new byte[n];
            for (int i = 0; i < n; i++) {
                out[i] = (byte) Integer.parseInt(c.substring(i * 2, i * 2 + 2), 16);
            }
            return out;
        } catch (Throwable t) {
            return null;
        }
    }

    private static final String RAW_PB_HEX = "";

    public static byte[] packArk(String arkJson) {
        try {
            byte[] raw = arkJson.getBytes("UTF-8");
            java.util.zip.Deflater def =
                    new java.util.zip.Deflater(java.util.zip.Deflater.DEFAULT_COMPRESSION);
            def.setInput(raw);
            def.finish();
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            while (!def.finished()) {
                int n = def.deflate(buf);
                if (n <= 0) break;
                out.write(buf, 0, n);
            }
            def.end();
            byte[] gz = out.toByteArray();
            byte[] res = new byte[gz.length + 1];
            res[0] = 0x01;
            System.arraycopy(gz, 0, res, 1, gz.length);
            return res;
        } catch (Throwable t) {
            HookLog.e("packArk 失败", t);
            return null;
        }
    }

    public static String hexOf(byte[] b) {
        StringBuilder sb = new StringBuilder(b.length * 2);
        for (byte x : b) sb.append(String.format("%02x", x & 0xFF));
        return sb.toString();
    }

    public static final String RAW_FEED_SAMPLE =
            "{\"app\":\"com.tencent.feed.lua\",\"desc\":\"\",\"view\":\"feed\","
                    + "\"bizsrc\":\"pindao.picforum\",\"ver\":\"0.0.0.1\",\"prompt\":\"[分享帖子] #张元英 \","
                    + "\"appID\":\"\",\"sourceName\":\"\",\"actionData\":\"\",\"actionData_A\":\"\",\"sourceUrl\":\"\","
                    + "\"meta\":{\"feed\":{\"cover\":\"https://channel.qpic.cn/psc?/channel/NaDwC23LjvXqrn3RH.9z8dX.EHNKzA5DYtZAy49armmGChWzpa7IPFUTEd*sgKQOhzhPF4Bv2x7Yja3wb6luGyEuQO2*x4aS7OpmJN4SkME!/c=\","
                    + "\"title\":\"#张元英 \",\"tagIcon\":\"http://downv6.qq.com/innovate/guild/tail_guild_logo.png\","
                    + "\"tagName\":\"腾讯频道\",\"picNum\":1,\"imageInfo\":{\"width\":400,\"height\":500},"
                    + "\"busiData\":\"{\\\"share_biz_data\\\":{\\\"feed_id\\\":\\\"B_0000000000000001\\\",\\\"creat_time\\\":1791755549,\\\"poster_tiny_id\\\":0}}\","
                    + "\"jumpUrl\":\"https://pd.qq.com/qqweb/qunpro/hs?_wv=3&_wwv=128&appChannel=share&businessType=2&openSource=2\","
                    + "\"ark_reserved1\":\"1\",\"ark_reserved2\":\"1\",\"ark_reserved3\":\"B_0000000000000001\"}},"
                    + "\"config\":{\"autosize\":0,\"collect\":1,\"ctime\":1794131263,\"forward\":1,\"reply\":1,\"round\":1,"
                    + "\"token\":\"\",\"type\":\"normal\"},"
                    + "\"text\":\"\",\"extraApps\":[],\"sourceAd\":\"\",\"extra\":\"\"}";

    public static Result sendVia77(Fields f) {
        Result r = new Result();
        String ark = build(f);
        if (ark == null) {
            r.error = "标题空了";
            return r;
        }
        try {
            int ct = com.cathelperawa.owo.app.bridge.CurrentChat.chatType();
            int chatFlag = ct >= 1 ? ct - 1 : 0;
            long peer = targetUin();
            if (peer <= 0) {
                r.error = "取不到对方号";
                return r;
            }
            String title = f.title == null ? "" : f.title.trim();
            String desc = nz(f.content).length() == 0 ? title : f.content;

            String tpl = "{\"1\":2935,\"2\":9,\"4\":{"
                    + "\"1\":" + SHELL_APPID + ",\"2\":1,"
                    + "\"5\":{\"1\":2,\"2\":\"0.0.0\","
                    + "\"3\":" + JSONObject.quote(SHELL_PKG) + ","
                    + "\"4\":" + JSONObject.quote(SHELL_SIGN) + "},"
                    + "\"16\":{\"1\":" + SHELL_APPID + ",\"2\":1,"
                    + "\"10\":" + JSONObject.quote(ark) + "},"
                    + "\"7\":{\"15\":" + MAGIC_ID + "},"
                    + "\"10\":" + chatFlag + ","
                    + "\"11\":" + peer + ","
                    + "\"12\":{"
                    + "\"10\":" + JSONObject.quote("腾讯频道") + ","
                    + "\"11\":" + JSONObject.quote(title) + ","
                    + "\"12\":" + JSONObject.quote(desc) + ","
                    + "\"13\":" + JSONObject.quote(JUMP) + ","
                    + "\"14\":" + JSONObject.quote(nz(f.guildIcon))
                    + "}},"
                    + "\"6\":\"android 9.1.35\"}";

            byte[] pb = com.cathelperawa.owo.app.ovo.QPacketHelper
                    .buildMessage(new JSONObject(tpl).toString());
            if (pb == null || pb.length == 0) {
                r.error = "组包失败";
                return r;
            }

            int rc = com.cathelperawa.owo.app.proto.PbSender.sendNativeAndWait(
                    PlainNewsCard.CMD, pb, 8000);
            r.rc = rc;
            if (rc == 0) {
                r.ok = true;
            } else {
                r.error = describeRc(rc);
            }
            return r;
        } catch (Throwable t) {
            HookLog.e("GuildCard.sendVia77 失败", t);
            r.error = String.valueOf(t);
            return r;
        }
    }

    private static long targetUin() {
        int ct = com.cathelperawa.owo.app.bridge.CurrentChat.chatType();
        String raw = com.cathelperawa.owo.app.bridge.CurrentChat.peerRaw();
        long v = toLong(raw);
        if (v > 0) return v;
        v = toLong(com.cathelperawa.owo.app.bridge.CurrentChat.peerUid());
        if (v > 0) return v;
        logStep("targetUin: chatType=" + ct + " raw=" + raw);
        return 0L;
    }

    private static void logStep(String s) {
        try {
            HookLog.i("GuildCard " + s);
        } catch (Throwable ignore) {
        }
    }

    private static String genFeedId() {
        Random r = new Random();
        StringBuilder sb = new StringBuilder("B_");
        String hex = "0123456789abcdef";
        for (int i = 0; i < 32; i++) sb.append(hex.charAt(r.nextInt(16)));
        sb.append('X').append(10 + r.nextInt(90));
        return sb.toString();
    }

    public static String md5(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] b = md.digest(s.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte x : b) sb.append(String.format("%02x", x & 0xFF));
            return sb.toString();
        } catch (Throwable t) {
            return "00000000000000000000000000000000";
        }
    }

    private static long toLong(String s) {
        if (s == null) return 0L;
        s = s.trim();
        if (s.length() == 0) return 0L;
        try {
            return Long.parseLong(s);
        } catch (Throwable t) {
            return 0L;
        }
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}
