package com.cathelperawa.owo.app.feature;

import com.cathelperawa.owo.app.bridge.CurrentChat;
import com.cathelperawa.owo.app.core.HookLog;

import org.json.JSONObject;

import java.util.Map;

public final class CardKit {

    public static final int CH_NEWS = 0;
    public static final int CH_MINI = 1;
    public static final int CH_EGAME = 2;
    public static final int CH_SIGN = 3;   

    public static final class Field {
        public String key, label, hint, def;
        public boolean required;

        public Field(String k, String l, String h, String d, boolean r) {
            this.key = k; this.label = l; this.hint = h; this.def = d; this.required = r;
        }
    }

    public interface Builder {
        String build(Map<String, String> v) throws Exception;
    }

    public static final class Spec {
        public final String key, name, desc, group;
        public final int channel;
        public final String cmd;
        public final Field[] fields;
        public final Builder builder;
        public final String tpl;   

        Spec(String key, String name, String desc, String group, int channel, String cmd,
             Field[] fields, Builder builder) {
            this(key, name, desc, group, channel, cmd, fields, builder, null);
        }

        Spec(String key, String name, String desc, String group, int channel, String cmd,
             Field[] fields, Builder builder, String tpl) {
            this.key = key; this.name = name; this.desc = desc; this.group = group;
            this.channel = channel; this.cmd = cmd;
            this.fields = fields; this.builder = builder;
            this.tpl = tpl;
        }
    }

    private CardKit() { }

    public static long peer() { return 0L; }

    private static long parseLong(String s) { return 0L; }

    private static boolean nz(String s) { return false; }

    private static String encode(String url) { return null; }

    private static final String DEF_PIC =
            "";
    private static Field[] basic() { return null; }

    private static Field[] newsFields() { return null; }

    private static Field[] miniFields() { return null; }

    private static Spec[] SPECS;

    public static synchronized Spec[] all() { return null; }

    public static Spec find(String key) { return null; }

    private static Spec news(String key, String name, String desc, String group) { return null; }

    private static Spec news(String key, String name, String desc, String group, Field[] f) { return null; }

    private static Spec mini(String key, String name, String desc, String group) { return null; }

    private static Spec mini(String key, String name, String desc, String group, Field[] f) { return null; }

    private static Spec egame(String key, String name, String desc, String group) { return null; }

    private static Spec card(String key, String name, String desc, String group, Field[] f) { return null; }

    private static Spec card(String key, String name, String desc, String group, Field[] f, String tpl) { return null; }

    private static final Field[] F_BASE = {
            new Field("title", "标题", "卡片标题", "卡片", false),
            new Field("desc", "描述", "副标题", "看看这个", false),
            new Field("pic", "封面图", "QQ 图床最稳", DEF_PIC, false),
            new Field("jump", "点击跳转", "https://...", "https://baidu.com", false),
    };

    private static final Field[] F_TAG = {
            new Field("title", "标题", "卡片标题", "卡片", false),
            new Field("tag", "标签名", "左上角标签", "热乎推荐", false),
            new Field("tagIcon", "标签图标", "图标直链，留空用默认", "", false),
            new Field("desc", "描述", "副标题", "看看这个", false),
            new Field("pic", "封面图", "QQ 图床最稳", DEF_PIC, false),
            new Field("jump", "点击跳转", "https://...", "https://baidu.com", false),
    };

    private static final Field[] F_BOT = {
            new Field("botUin", "机器人 QQ", "机器人的号", "10006", false),
            new Field("botName", "机器人名称", "显示名字", "小助手", false),
            new Field("botDesc", "机器人描述", "简介", "在线", false),
            new Field("botAvatar", "机器人头像", "图片直链", DEF_PIC, false),
            new Field("appid", "机器人 AppId", "留空用默认", "", false),
            new Field("prompt", "外显文案", "列表里显示的字", "", false),
    };

    private static final Field[] F_BOTBTN = {
            new Field("prompt", "外显", "列表里显示的字", "", false),
            new Field("botUin", "机器人 UIN", "默认 10006", "10006", false),
            new Field("botName", "机器人名称", "默认 游戏问答助手", "游戏问答助手", false),
            new Field("botDesc", "机器人简介", "可空", "", false),
            new Field("botAvatar", "机器人头像", "图片直链", DEF_PIC, false),
            new Field("botTag", "机器人标签", "默认 机器人", "机器人", false),
            new Field("btnText", "按钮文字", "默认 添加", "添加", false),
            new Field("appid", "机器人 APP ID", "默认 101984213", "101984213", false),
            new Field("bottomText", "左下角小文本", "默认 QQ机器人", "QQ机器人", false),
            new Field("bottomPic", "左下角小图片", "可空", "", false),
    };

    private static final Field[] F_MUSIC = {
            new Field("title", "歌名", "歌曲名称", "夜曲", false),
            new Field("desc", "歌手", "演唱者", "周杰伦", false),
            new Field("appid", "音乐 AppId", "留空用默认", "", false),
            new Field("pkg", "包名", "留空用默认", "com.tencent.qqmusic", false),
            new Field("sign", "包签名", "留空用默认", "", false),
            new Field("pic", "封面图", "专辑封面直链", DEF_PIC, false),
    };

    private static final Field[] F_RTC = {
            new Field("title", "群名", "通话标题", "语音通话", false),
            new Field("desc", "邀请人", "谁发起的", "我", false),
            new Field("peer", "群号/QQ", "留空用当前会话", "", false),
    };

    private static final Field[] F_PICARK = {
            new Field("md5", "图片 MD5", "32 位小写", "", false),
            new Field("prompt", "外显文案", "列表里显示的字", "", false),
            new Field("size", "图片大小", "字节数", "0", false),
            new Field("width", "图片宽度", "像素", "0", false),
    };

    private static final Field[] F_GAME = {
            new Field("title", "标题", "卡片标题", "一起来玩", false),
            new Field("desc", "描述", "副标题", "点击查看", false),
            new Field("prompt", "外显文案", "列表里显示的字", "", false),
            new Field("pic", "图片直链", "QQ 图床最稳", DEF_PIC, false),
            new Field("jump", "点击跳转", "https://...", "https://baidu.com", false),
            new Field("btn1t", "按钮1 文案", "留空不显示", "", false),
            new Field("btn1u", "按钮1 链接", "https://...", "", false),
            new Field("btn2t", "按钮2 文案", "留空不显示", "", false),
            new Field("btn2u", "按钮2 链接", "https://...", "", false),
            new Field("wh", "宽高", "如 600x330，留空自动", "", false),
    };

    private static final Field[] F_NEWSNV = {
            new Field("appid", "App Id", "默认哔哩哔哩 100951776", "100951776", false),
            new Field("tag", "标签", "卡片左上角标签", "热乎推荐", false),
            new Field("title", "标题", "必填", "标题", false),
            new Field("desc", "简介", "必填", "简介", false),
            new Field("prompt", "外显", "必填", "外显", false),
            new Field("jump", "跳转链接", "必填", "https://baidu.com", false),
            new Field("pic", "预览图链接", "必填", DEF_PIC, false),
    };

    private static final Field[] F_MINIPIC = {
            new Field("pic", "图片链接", "可选", DEF_PIC, false),
            new Field("jump", "跳转地址", "可选", "https://baidu.com", false),
            new Field("appid", "APPID", "可选", "1109937557", false),
            new Field("title", "标题", "可选", "小程序", false),
            new Field("desc", "简介", "可选", "看看这个", false),
            new Field("icon", "图标", "可选", "", false),
            new Field("web", "页面路径", "可选", "pages/index/index", false),
    };

    private static final Field[] F_RTC2 = {
            new Field("groupName", "群名称", "必填", "语音通话", false),
            new Field("inviter", "邀请人名称", "必填", "我", false),
            new Field("groupId", "群号", "默认当前群号，留空自动", "", false),
    };

    private static final Field[] F_GCEX = {
            new Field("title", "标题", "卡片标题", "道具互换", false),
            new Field("pic", "图片链接", "图片直链", DEF_PIC, false),
            new Field("jump", "跳转链接", "https://...", "https://baidu.com", false),
    };

    private static final Field[] F_GCRANK = {
            new Field("type", "排名类型", "如 好友榜 / 全服榜", "好友榜", false),
            new Field("desc", "简介", "说明文字", "来看看排名", false),
            new Field("prompt", "外显", "列表里显示的字", "排行榜", false),
            new Field("jump", "跳转链接", "https://...", "https://baidu.com", false),
            new Field("pic", "封面图", "图片直链", DEF_PIC, false),
    };

    private static final Field[] F_BUTTONARK = {
            new Field("title", "标题", "必填", "标题", false),
            new Field("prompt", "外显", "默认同标题", "", false),
            new Field("jump", "跳转链接", "必填", "https://baidu.com", false),
            new Field("pic", "预览图链接", "必填", DEF_PIC, false),
            new Field("tagName", "Tag Name", "默认 QQ频道", "QQ频道", false),
            new Field("tagIcon", "Tag Icon", "图标直链", "", false),
            new Field("btnTitle", "按钮标题", "必填", "立即前往", false),
            new Field("btnJump", "按钮跳转链接", "默认同 JumpUrl", "", false),
            new Field("btnDisable", "按钮是否禁用", "true / false，默认 false", "false", false),
            new Field("btnState", "按钮状态", "默认 primary", "primary", false),
    };

    private static final Field[] F_CHANNEL = {
            new Field("title", "标题", "必填", "标题", false),
            new Field("desc", "简介", "必填", "简介", false),
            new Field("prompt", "外显", "默认同标题", "", false),
            new Field("jump", "跳转链接", "必填", "https://baidu.com", false),
            new Field("pic", "预览图链接", "必填", DEF_PIC, false),
            new Field("tagName", "Tag Name", "默认 QQ频道", "QQ频道", false),
            new Field("tagIcon", "Tag Icon", "图标直链", "", false),
    };

    private static final Field[] F_MINIARK = {
            new Field("title", "Title", "必填", "小程序", false),
            new Field("desc", "Desc", "可选", "看看这个", false),
            new Field("pic", "图片链接", "Pic URL", DEF_PIC, false),
            new Field("jump", "跳转地址", "Jump URL", "https://baidu.com", false),
            new Field("autoPlay", "哔哩哔哩自动播放卡", "填 1 开启，留空关闭", "", false),
            new Field("icon", "图标地址", "Icon URL", "", false),
            new Field("web", "Web 地址", "Web URL", "pages/index/index", false),
            new Field("vid", "Vid 地址", "Vid URL", "", false),
            new Field("appid", "APPID", "可选", "1109937557", false),
            new Field("scene", "Scene", "场景号", "", false),
            new Field("templateId", "Template ID", "模板 id", "", false),
            new Field("templateData", "Template Data", "模板数据", "", false),
    };

    private static final Field[] F_MUSIC2 = {
            new Field("appid", "App Id", "默认哔哩哔哩 100495085", "100495085", false),
            new Field("pkg", "应用包名", "默认 QQ 音乐", "com.tencent.qqmusic", false),
            new Field("sign", "安装包签名 MD5", "默认 QQ 音乐", "", false),
            new Field("title", "音乐名称", "必填", "夜曲", false),
            new Field("desc", "音乐歌手", "必填", "周杰伦", false),
            new Field("prompt", "外显", "必填", "[分享]夜曲", false),
            new Field("jump", "跳转链接", "必填", "https://y.qq.com/", false),
            new Field("pic", "预览图链接", "必填", DEF_PIC, false),
            new Field("musicUrl", "音乐链接", "必填", "", false),
    };

    private static final Field[] F_NEWSARK = {
            new Field("appName", "应用名称", "添加预设用，正常可忽略", "", false),
            new Field("appid", "App Id", "默认哔哩哔哩 100951776", "100951776", false),
            new Field("pkg", "应用包名", "默认哔哩哔哩", "tv.danmaku.bili", false),
            new Field("sign", "安装包签名 MD5", "默认哔哩哔哩", "7194d531cbe7960a22007b9f6bdaa38b", false),
            new Field("title", "标题", "必填", "标题", false),
            new Field("desc", "简介", "必填", "简介", false),
            new Field("prompt", "外显", "必填", "外显", false),
            new Field("jump", "跳转链接", "必填", "https://baidu.com", false),
            new Field("pic", "预览图链接", "必填", DEF_PIC, false),
    };

    private static final Field[] F_NEWSMAX = {
            new Field("title", "标题", "必填", "标题", false),
            new Field("desc", "简介", "必填", "简介", false),
            new Field("prompt", "外显", "必填", "外显", false),
            new Field("jump", "跳转链接", "必填", "https://baidu.com", false),
            new Field("pic", "预览图链接", "必填", DEF_PIC, false),
            new Field("tagIcon", "TAG ICON", "必填", "", false),
            new Field("tagName", "TAG NAME", "必填", "热乎推荐", false),
    };

    private static final Field[] F_NEWSPLUS = {
            new Field("appid", "App Id", "默认哔哩哔哩 100951776", "100951776", false),
            new Field("title", "标题", "必填", "标题", false),
            new Field("desc", "简介", "必填", "简介", false),
            new Field("prompt", "外显", "必填", "外显", false),
            new Field("jump", "跳转链接", "必填", "https://baidu.com", false),
            new Field("pic", "预览图链接", "必填", DEF_PIC, false),
    };

    private static final Field[] F_PICARK2 = {
            new Field("pic", "图片链接", "必填", DEF_PIC, false),
            new Field("prompt", "外显", "默认 [图片]", "[图片]", false),
            new Field("length", "图片长度", "默认 1080", "1080", false),
            new Field("width", "图片宽度", "默认 1080", "1080", false),
    };

    private static final Field[] F_QQSIGN = {
            new Field("title", "标题", "必填", "标题", false),
            new Field("desc", "简介", "必填", "简介", false),
            new Field("jump", "跳转链接", "必填", "https://baidu.com", false),
            new Field("pic", "预览图链接", "必填", DEF_PIC, false),
    };

    private static final Field[] F_DZDP = {
            new Field("title", "店名", "商户名称", "椰灰狼是嘿壳", false),
            new Field("desc", "分店名", "branchName", "我是废物", false),
            new Field("prompt", "外显文案", "列表里显示的字", "大众点评", false),
            new Field("pic", "店铺图片", "图片直链", DEF_PIC, false),
            new Field("price", "人均价格", "数字", "114514", false),
            new Field("dist", "距离文本", "如 1.2km", "嘿壳", false),
            new Field("review", "评价数文本", "如 臭", "臭", false),
            new Field("addr", "地址", "店铺地址", "你是嘿壳", false),
    };

    private static final Field[] F_KAPU = {
            new Field("title", "标题", "卡片标题", "卡噗", false),
            new Field("desc", "描述", "describe", "测试卡普", false),
            new Field("prompt", "外显文案", "列表里显示的字", "测试外显", false),
            new Field("pic", "头像", "avatarHead 图片直链", DEF_PIC, false),
            new Field("jump", "点击跳转", "mqqapi:// 或 https", "mqqapi://openhalfscreenweb/?url=www.qq.com", false),
            new Field("sub", "副文本", "subText", "102630409", false),
    };

    private static final Field[] F_TEAMUP = {
            new Field("title", "标题", "报名标题", "组队报名", false),
            new Field("desc", "描述", "说明", "点击参与", false),
            new Field("time", "时间", "开始时间", "", false),
            new Field("pic", "封面图", "图片直链", DEF_PIC, false),
    };

    private static final Field[] F_CONTACT = {
            new Field("uin", "QQ 号", "要推荐的联系人", "", false),
            new Field("title", "标题", "卡片标题", "推荐联系人", false),
            new Field("scheme", "跳转 scheme", "mqqapi://...", "", false),
    };

    private static final Builder NEWS_BUILDER = null;

    private static final Builder MINI_BUILDER = null;

    private static final Builder EGAME_BUILDER = null;

    private static final String PL_DZDP =
        "{\"app\":\"com.dianping.dpscope\",\"bizsrc\":\"\","
        + "\"config\":{\"autosize\":1,\"ctime\":__T__,\"forward\":1,\"token\":\"\",\"type\":\"normal\"},"
        + "\"desc\":\"大众点评\",\"extra\":{\"app_type\":1,\"appid\":200002,\"uin\":0},"
        + "\"meta\":{\"Restaurant.Search\":{\"Restaurant.ShopData\":{"
        + "\"avgPrice\":\"__PRICE__\",\"branchName\":\"__DESC__\",\"defaultPic\":\"__PIC__\","
        + "\"distance\":\"__DIST__\",\"picList\":[\"__PIC__\"],\"reviewCnt\":\"__REVIEW__\","
        + "\"shopAddress\":\"__ADDR__\",\"shopId\":\"111\",\"shopName\":\"__TITLE__\",\"shopPower\":60}}},"
        + "\"prompt\":\"__PROMPT__\",\"ver\":\"0.0.0.1\",\"view\":\"RestaurantList\"}";

    private static final String PL_DNF =
        "{\"app\":\"com.tencent.gamecenter.mdnf\",\"bizsrc\":\"\","
        + "\"config\":{\"ctime\":__T__,\"forward\":1,\"token\":\"\",\"type\":\"normal\"},"
        + "\"desc\":\"游戏分享\",\"extra\":{\"app_type\":1,\"appid\":1104294055,\"uin\":0},"
        + "\"meta\":{\"shareData\":{\"appid\":\"1104294055\","
        + "\"openid\":\"C57F23B90C963E79E97A5CBB459F4EFA\","
        + "\"roleInfo\":{\"avatar\":\"__PIC__\",\"partition\":\"__DESC__\",\"roleName\":\"__TITLE__\"},"
        + "\"scene\":\"69aab638-e5d1-43df-9684-116ed8506025\","
        + "\"textBoxData\":{\"desc\":\"__DESC__\",\"pic\":\"\",\"title\":\"__TITLE__\"},"
        + "\"textGameData\":{\"data\":[{\"num\":\"__DESC__\",\"text\":\"__DESC__\"}],\"title\":\"__DESC__\"},"
        + "\"url\":\"__PIC__\"}},\"prompt\":\"__PROMPT__\",\"ver\":\"0.0.0.1\",\"view\":\"commonView\"}";

    private static final String PL_KAPU =
        "{\"app\":\"com.tencent.release.kapu\",\"bizsrc\":\"adelie.agent_list\","
        + "\"config\":{\"ctime\":__T__,\"tips\":\"\",\"token\":\"\",\"type\":\"card\"},"
        + "\"desc\":\"\",\"extra\":{\"app_type\":1,\"appid\":101538890,\"uin\":0},"
        + "\"meta\":{\"data\":{\"actionType\":1,\"avatarHead\":\"__PIC__\",\"describe\":\"__DESC__\","
        + "\"jumpUrl\":\"__JUMP__\",\"remarksLogo\":\"\",\"subText\":\"__SUB__\"}},"
        + "\"prompt\":\"__PROMPT__\",\"ver\":\"1.0.0.5\",\"view\":\"CMAvatar3D\"}";

    private static final String SY_HEAD =
        "{\"app\":\"com.tencent.gamecenter.qqsy\",\"bizsrc\":\"\","
        + "\"extra\":{\"app_type\":1,\"appid\":1106040804,\"uin\":0},";

    private static final String SY_1 = SY_HEAD
        + "\"config\":{\"ctime\":__T__,\"forward\":1,\"token\":\"\",\"type\":\"normal\"},"
        + "\"desc\":\"__DESC__\",\"meta\":{\"shareData\":{\"actid\":\"10267755\","
        + "\"bgImg\":\"__PIC__\",\"btns\":[{\"text\":\"__BTN1T__\",\"url\":\"__BTN1U__\"}],"
        + "\"inviterKey\":\"666\",\"needTeamId\":1,\"title\":\"__TITLE__\"}},"
        + "\"prompt\":\"__PROMPT__\",\"ver\":\"1.0.0.1\",\"view\":\"mieTeamView\"}";

    private static final String SY_2 = SY_HEAD
        + "\"config\":{\"ctime\":__T__,\"forward\":1,\"token\":\"\",\"type\":\"normal\"},"
        + "\"desc\":\"__DESC__\",\"meta\":{\"shareData\":{\"appid\":\"102755421\","
        + "\"desc\":\"__DESC__\",\"preview\":\"__PIC__\",\"title\":\"__TITLE__\"}},"
        + "\"prompt\":\"__PROMPT__\",\"ver\":\"0.0.0.1\",\"view\":\"picView2\"}";

    private static final String SY_3 = SY_HEAD
        + "\"config\":{\"autoSize\":0,\"ctime\":__T__,\"forward\":1,\"height\":40,"
        + "\"token\":\"\",\"type\":\"normal\",\"width\":900},"
        + "\"desc\":\"__DESC__\",\"meta\":{\"button\":{\"btnText\":\"__BTN1T__\",\"disable\":false,"
        + "\"funName\":\"btn1Jump\",\"marginLeft\":\"0\",\"marginRight\":\"0\",\"type\":\"0\"}},"
        + "\"prompt\":\"__PROMPT__\",\"ver\":\"0.0.0.1\",\"view\":\"button\"}";

    private static final String SY_4 =
        "{\"app\":\"com.tencent.gamecenter.lolm\",\"bizsrc\":\"\","
        + "\"config\":{\"autosize\":1,\"ctime\":__T__,\"forward\":1,\"token\":\"\",\"type\":\"normal\"},"
        + "\"desc\":\"__DESC__\",\"extra\":{\"app_type\":1,\"appid\":1106040804,\"uin\":0},"
        + "\"meta\":{\"shareData\":{\"appid\":\"1106040804\",\"areaId\":\"2\",\"clientType\":\"pub\","
        + "\"clientVersion\":\"5.1.0.7895\",\"getIndex\":\"114514\","
        + "\"idipOpenId\":\"14745867356422430086\",\"name\":\"__TITLE__\",\"photo\":\"__PIC__\","
        + "\"platId\":1,\"position\":3,\"queryId\":1009303,\"queryType\":2}},"
        + "\"prompt\":\"__PROMPT__\",\"ver\":\"0.0.0.1\",\"view\":\"hero\"}";

    private static final String SY_5 =
        "{\"app\":\"com.tencent.gamecenter.lolm\",\"bizsrc\":\"\","
        + "\"config\":{\"autosize\":1,\"ctime\":__T__,\"forward\":1,\"token\":\"\",\"type\":\"normal\"},"
        + "\"desc\":\"\",\"extra\":{\"app_type\":1,\"appid\":1106040804,\"uin\":0},"
        + "\"meta\":{\"shareData\":{\"appid\":\"com.example.app\","
        + "\"buttons\":[{\"text\":\"__BTN1T__\",\"url\":\"__BTN1U__\"},{\"text\":\"__BTN2T__\",\"url\":\"__BTN2U__\"}],"
        + "\"desc\":\"__DESC__\",\"jumpUrl\":\"__JUMP__\",\"sourceName\":\"示例应用\","
        + "\"title\":\"__TITLE__\",\"url\":\"__PIC__\"}},"
        + "\"prompt\":\"__PROMPT__\",\"ver\":\"0.0.0.1\",\"view\":\"shareA\"}";

    private static final String SY_6 =
        "{\"app\":\"com.tencent.gamecenter.qqsy\",\"bizsrc\":\"adelie.agent_list\","
        + "\"config\":{\"autosize\":1,\"collect\":1,\"ctime\":__T__,\"forward\":1,\"reply\":1,\"round\":1,"
        + "\"token\":\"\",\"type\":\"normal\"},"
        + "\"desc\":\"\",\"extra\":{\"app_type\":1,\"appid\":1106040804,\"uin\":0},"
        + "\"meta\":{\"shareData\":{\"appicon\":\"__PIC__\",\"appid\":\"0\",\"appname\":\"tag\","
        + "\"button1URL\":\"__BTN1U__\",\"button1disable\":false,\"button1title\":\"__BTN1T__\","
        + "\"button2URL\":\"__BTN2U__\",\"button2disable\":false,\"button2title\":\"__BTN2T__\","
        + "\"buttonNum\":2,\"from\":\"adelie.agent_list\",\"jumpURL\":\"__JUMP__\","
        + "\"preview\":\"__PIC__\",\"previewType\":\"video\",\"title\":\"__TITLE__\"}},"
        + "\"prompt\":\"__PROMPT__\",\"ver\":\"0.0.0.1\",\"view\":\"picView2\"}";

    private static final String SY_7 =
        "{\"app\":\"com.tencent.gamecenter.qqsy\",\"desc\":\"__DESC__\",\"bizsrc\":\"\","
        + "\"view\":\"bottomTag\",\"ver\":\"0.0.0.1\",\"prompt\":\"__PROMPT__\","
        + "\"meta\":{\"bottomTag\":{\"appid\":\"1112198072\",\"tagIcon\":\"__PIC__\","
        + "\"tagName\":\"__BTN1T__\",\"title\":\"__TITLE__\"}},"
        + "\"config\":{\"autoSize\":0,\"ctime\":__T__,\"forward\":1,\"height\":20,"
        + "\"token\":\"\",\"type\":\"normal\",\"width\":90}}";

    private static final String SY_8 = SY_HEAD
        + "\"config\":{\"ctime\":__T__,\"forward\":1,\"token\":\"\",\"type\":\"normal\"},"
        + "\"desc\":\"__DESC__\",\"meta\":{\"shareData\":{\"actid\":\"10306671\","
        + "\"bgImg\":\"__PIC__\",\"btns\":[{\"text\":\"__BTN1T__\",\"url\":\"__BTN1U__\"}],"
        + "\"inviterKey\":\"IlP2IHavRMcSZdNRSc/K7g==\",\"needTeamId\":1,\"title\":\"__TITLE__\"}},"
        + "\"prompt\":\"__PROMPT__\",\"ver\":\"1.0.0.1\",\"view\":\"mieTeamView\"}";

    private static final String EG_HEAD =
        "{\"app\":\"com.tencent.gamecenter.egameshare\",\"bizsrc\":\"adelie.agent_list\","
        + "\"config\":{\"autosize\":1,\"ctime\":__T__,\"forward\":1,\"tips\":\"Signed By Arona\",\"token\":\"\",\"type\":\"normal\"},"
        + "\"desc\":\"游戏分享\",\"extra\":{\"app_type\":1,\"appid\":1105198412,\"msg_seq\":74515265555000,\"uin\":0},";

    private static String egViews(int videoType, int dataCount, String gdTitle) { return null; }

    private static final String EG_1 = egViews(0, 3, "新买英雄");
    
    private static final String EG_2 = egViews(1, 3, "新买英雄");
    
    private static final String EG_3 = egViews(0, 1, "英雄展示");
    
    private static final String EG_4 = egViews(1, 2, "双英雄");
    
    private static final String EG_5 = egViews(0, 5, "我的战绩");

    private static final String WZ_HEAD =
        "{\"app\":\"com.tencent.gamecenter.gameshare_sgame\",";
    private static final String WZ_EXTRA =
        "\"extra\":{\"app_type\":1,\"appid\":1104466820,\"uin\":0},";

    private static final String WZ_1 = WZ_HEAD
        + "\"bizsrc\":\"\",\"config\":{\"autoSize\":0,\"ctime\":__T__,\"forward\":1,\"height\":300,"
        + "\"token\":\"\",\"type\":\"normal\",\"width\":300},\"desc\":\"__DESC__\"," + WZ_EXTRA
        + "\"meta\":{\"shareData\":{\"appid\":\"1104466820\",\"height\":9930,\"jumpUrl\":\"__JUMP__\","
        + "\"roleInfo\":{\"avatar\":\"__PIC__\",\"partition\":\"__DESC__\",\"roleName\":\"__TITLE__\"},"
        + "\"title\":\"__TITLE__\",\"type\":\"video\",\"url\":\"__JUMP__\",\"weight\":9930,\"width\":601}},"
        + "\"prompt\":\"__PROMPT__\",\"ver\":\"0.0.0.19\",\"view\":\"dataView\"}";

    private static final String WZ_2 =
        "{\"actionData\":\"\",\"actionData_A\":\"\",\"app\":\"com.tencent.gamecenter.gameshare_sgame\",\"appID\":\"\","
        + "\"config\":{\"ctime\":__T__,\"forward\":1,\"token\":\"\",\"type\":\"normal\"},"
        + "\"desc\":\"__DESC__\"," + WZ_EXTRA
        + "\"meta\":{\"shareData\":{\"appid\":\"1104466820\",\"cardTexts\":\"\",\"cardUrl\":\"__PIC__\","
        + "\"extData\":{\"appid\":\"1104466820\",\"heroid\":111,\"skinid\":11107},\"hashcode\":\"7574098\","
        + "\"openId\":\"97DF1448208011E6146C538C7BD1FA30\",\"scene\":\"234\","
        + "\"textBoxData\":{\"desc\":\"__DESC__\",\"pic\":\"__PIC__\",\"title\":\"__TITLE__\"},"
        + "\"url\":\"__PIC__\"}},\"prompt\":\"__PROMPT__\",\"view\":\"commonView\"}";

    private static final String WZ_3 =
        WZ_HEAD + "\"bizsrc\":\"\",\"config\":{\"autoSize\":0,\"ctime\":__T__,\"forward\":1,\"height\":300,"
        + "\"token\":\"\",\"type\":\"normal\",\"width\":300},\"desc\":\"__DESC__\"," + WZ_EXTRA
        + "\"meta\":{\"shareData\":{\"appid\":\"1104466820\","
        + "\"cardTexts\":[\"__TITLE__\",\"__DESC__\"],\"cardUrl\":\"__PIC__\","
        + "\"extData\":{\"appid\":\"1104466820\",\"heroid\":144,\"skinid\":14401},\"hashcode\":\"7693875\","
        + "\"openId\":\"4EDA720AAFB59522D829FAE25ED7A285\",\"scene\":\"12\","
        + "\"textBoxData\":{\"desc\":\"__DESC__\",\"pic\":\"__PIC__\",\"title\":\"__TITLE__\"},"
        + "\"url\":\"__PIC__\"}},\"prompt\":\"__PROMPT__\",\"ver\":\"0.0.0.19\",\"view\":\"commonView\"}";

    private static final String WZ_4 =
        WZ_HEAD + "\"bizsrc\":\"\",\"config\":{\"ctime\":__T__,\"forward\":1,\"token\":\"\",\"type\":\"normal\"},"
        + "\"desc\":\"__DESC__\"," + WZ_EXTRA
        + "\"meta\":{\"shareData\":{\"appid\":\"1104466820\","
        + "\"cardTexts\":[\"__TITLE__\"],\"cardUrl\":\"__PIC__\","
        + "\"extData\":{\"appid\":\"1104466820\",\"heroid\":174,\"skinid\":17408},\"hashcode\":\"6883934\","
        + "\"openId\":\"926269BB56AD4BD934080A43BA25F265\",\"scene\":\"12\","
        + "\"textBoxData\":{\"desc\":\"\",\"pic\":\"__PIC__\",\"title\":\"\"},"
        + "\"url\":\"__PIC__\"}},\"prompt\":\"__PROMPT__\",\"ver\":\"0.0.0.1\",\"view\":\"commonView\"}";

    private static final String WZ_5 =
        WZ_HEAD + "\"bizsrc\":\"\",\"config\":{\"ctime\":__T__,\"forward\":1,\"token\":\"\",\"type\":\"normal\"},"
        + "\"desc\":\"__DESC__\"," + WZ_EXTRA
        + "\"meta\":{\"shareData\":{\"appid\":\"1104466820\","
        + "\"cardTexts\":[\"__TITLE__\",\"__DESC__\"],\"cardUrl\":\"__PIC__\","
        + "\"extData\":{\"appid\":\"1104466820\",\"heroid\":144,\"skinid\":14401},\"hashcode\":\"7693875\","
        + "\"openId\":\"4EDA720AAFB59522D829FAE25ED7A285\",\"scene\":\"12\","
        + "\"textBoxData\":{\"desc\":\"__DESC__\",\"pic\":\"__PIC__\",\"title\":\"__TITLE__\"},"
        + "\"url\":\"__PIC__\"}},\"prompt\":\"__PROMPT__\",\"ver\":\"0.0.0.1\",\"view\":\"commonView\"}";

    private static final String WZ_6 =
        WZ_HEAD + "\"bizsrc\":\"\",\"config\":{\"autoSize\":0,\"ctime\":__T__,\"forward\":1,\"height\":300,"
        + "\"token\":\"\",\"type\":\"normal\",\"width\":300},\"desc\":\"__DESC__\"," + WZ_EXTRA
        + "\"meta\":{\"baseInfo\":{\"from\":\"game\",\"gamedata\":\"ShareTeam_0\",\"iArea\":\"1\","
        + "\"iPartition\":\"__DESC__\",\"iPlatId\":\"1\",\"iTeamBusId\":\"0\","
        + "\"iTeamId\":\"__TITLE__\",\"sAppid\":\"1104466820\",\"sOpenId\":\"3D4C2A5495DC007F3275653D45C73976\","
        + "\"teamLimit\":[],\"time\":\"0\"}},\"prompt\":\"__PROMPT__\",\"ver\":\"0.0.0.19\",\"view\":\"teamView\"}";

    private static final String WZ_7 =
        WZ_HEAD + "\"config\":{\"ctime\":__T__,\"forward\":1,\"token\":\"\",\"type\":\"normal\"},"
        + "\"desc\":\"__DESC__\"," + WZ_EXTRA
        + "\"meta\":{\"shareData\":{\"appid\":\"1104466820\",\"cardUrl\":\"__PIC__\","
        + "\"extData\":{\"gameSeq\":\"0\",\"gameSvrEntity\":\"0\",\"heroid\":\"116\","
        + "\"openId\":\"7C03E0196FC5DB35C6DDF0C07E6C7A76\",\"playerNum\":\"2\",\"relaySvrEntity\":\"0\"},"
        + "\"openId\":\"7C03E0196FC5DB35C6DDF0C07E6C7A76\","
        + "\"roleInfo\":{\"avatar\":\"__PIC__\",\"partition\":\"__DESC__\",\"roleName\":\"__TITLE__\"},"
        + "\"scene\":\"14\",\"textBoxData\":{\"desc\":\"__DESC__\",\"pic\":\"\",\"tips\":\"\",\"title\":\"__TITLE__\"},"
        + "\"textGameData\":{\"data\":[{\"num\":\"2\",\"text\":\"击杀数\"},{\"num\":\"8\",\"text\":\"死亡数\"},{\"num\":\"0\",\"text\":\"助攻数\"}],\"title\":\"我的战绩\"},"
        + "\"url\":\"__PIC__\"}},\"prompt\":\"__PROMPT__\",\"ver\":\"0.0.0.19\",\"view\":\"commonView\"}";

    private static final String WZ_8 =
        WZ_HEAD + "\"bizsrc\":\"\",\"config\":{\"autoSize\":0,\"ctime\":__T__,\"forward\":1,\"height\":300,"
        + "\"token\":\"\",\"type\":\"normal\",\"width\":300},\"desc\":\"__DESC__\"," + WZ_EXTRA
        + "\"meta\":{\"shareData\":{\"appid\":\"1104466820\",\"cardUrl\":\"__PIC__\","
        + "\"extData\":{\"gameSeq\":\"0\",\"gameSvrEntity\":\"0\",\"heroid\":\"116\","
        + "\"openId\":\"7C03E0196FC5DB35C6DDF0C07E6C7A76\",\"playerNum\":\"10\",\"relaySvrEntity\":\"0\"},"
        + "\"openId\":\"7C03E0196FC5DB35C6DDF0C07E6C7A76\","
        + "\"roleInfo\":{\"avatar\":\"__PIC__\",\"partition\":\"__DESC__\",\"roleName\":\"__TITLE__\"},"
        + "\"scene\":\"33\",\"textBoxData\":{\"desc\":\"__DESC__\",\"pic\":\"\",\"tips\":\"\",\"title\":\"__TITLE__\"},"
        + "\"textGameData\":{\"data\":[{\"num\":\"21\",\"text\":\"击杀数\"},{\"num\":\"2\",\"text\":\"死亡数\"},{\"num\":\"0\",\"text\":\"助攻数\"}],\"title\":\"我的战绩\"},"
        + "\"url\":\"__PIC__\"}},\"prompt\":\"__PROMPT__\",\"ver\":\"0.0.0.19\",\"view\":\"commonView\"}";

    private static final String PL_GAMESHARE =
        "{\"app\":\"com.tencent.gamecenter.gameshare\",\"view\":\"noDataView\","
        + "\"desc\":\"__DESC__\",\"ver\":\"1.0.0.1\",\"prompt\":\"__PROMPT__\","
        + "\"config\":{\"ctime\":__T__,\"forward\":\"0\",\"token\":\"\",\"type\":\"normal\"},"
        + "\"meta\":{\"shareData\":{\"appid\":\"1\","
        + "\"buttons\":[{\"text\":\"__BTN1T__\",\"url\":\"__BTN1U__\"},{\"text\":\"__BTN2T__\",\"url\":\"__BTN2U__\"}],"
        + "\"cover\":\"__PIC__\",\"jumpUrl\":\"__JUMP__\",\"scene\":10127757,"
        + "\"type\":\"image\",\"url\":\"__PIC__\",\"width\":__W__,\"height\":__H__}}}";

    private static final String PL_CF =
        "{\"app\":\"com.tencent.gamecenter.gameshare_cfm\",\"bizsrc\":\"\","
        + "\"config\":{\"ctime\":__T__,\"forward\":1,\"token\":\"\",\"type\":\"normal\"},"
        + "\"desc\":\"__DESC__\",\"extra\":{\"app_type\":1,\"appid\":1104512706,\"uin\":0},"
        + "\"meta\":{\"shareData\":{\"appid\":\"1104512706\",\"extData\":{},"
        + "\"openId\":\"4348ECE1E686F3C2FFE841875E7FB784\","
        + "\"roleInfo\":{\"avatar\":\"__PIC__\",\"partition\":\"__DESC__\",\"roleName\":\"__DESC__\"},"
        + "\"scene\":\"324\",\"textBoxData\":{\"desc\":\"__DESC__\",\"pic\":\"__PIC__\",\"title\":\"__TITLE__\"},"
        + "\"textGameData\":{\"data\":[],\"title\":\"\"},"
        + "\"url\":\"__PIC__\"}},\"prompt\":\"__PROMPT__\",\"ver\":\"0.0.0.1\",\"view\":\"commonView\"}";

    private static final String PL_WZRY =
        "{\"app\":\"com.tencent.gamecenter.gameshare_sgame\",\"bizsrc\":\"\","
        + "\"config\":{\"autoSize\":0,\"ctime\":__T__,\"forward\":1,\"height\":300,\"token\":\"\",\"type\":\"normal\",\"width\":300},"
        + "\"desc\":\"__DESC__\",\"extra\":{\"app_type\":1,\"appid\":1104466820,\"uin\":0},"
        + "\"meta\":{\"shareData\":{\"appid\":\"1104466820\","
        + "\"extData\":{\"gameSeq\":\"0\",\"gameSvrEntity\":\"0\",\"heroid\":\"116\",\"openId\":\"7C03E0196FC5DB35C6DDF0C07E6C7A76\",\"playerNum\":\"10\",\"relaySvrEntity\":\"0\"},"
        + "\"openId\":\"7C03E0196FC5DB35C6DDF0C07E6C7A76\","
        + "\"roleInfo\":{\"avatar\":\"__PIC__\",\"partition\":\"__DESC__\",\"roleName\":\"__TITLE__\"},"
        + "\"scene\":\"33\",\"textBoxData\":{\"desc\":\"__DESC__\",\"pic\":\"\",\"tips\":\"\",\"title\":\"__TITLE__\"},"
        + "\"textGameData\":{\"data\":[],\"title\":\"我的战绩\"},"
        + "\"url\":\"__PIC__\"}},\"prompt\":\"__PROMPT__\",\"ver\":\"0.0.0.19\",\"view\":\"commonView\"}";

    private static final String PL_QQSY =
        "{\"app\":\"com.tencent.gamecenter.qqsy\",\"bizsrc\":\"\","
        + "\"config\":{\"ctime\":__T__,\"forward\":1,\"token\":\"\",\"type\":\"normal\"},"
        + "\"desc\":\"__DESC__\",\"extra\":{\"app_type\":1,\"appid\":1106040804,\"uin\":0},"
        + "\"meta\":{\"shareData\":{\"appid\":\"102755421\",\"desc\":\"__DESC__\",\"preview\":\"__PIC__\",\"title\":\"__TITLE__\"}},"
        + "\"prompt\":\"__PROMPT__\",\"ver\":\"0.0.0.1\",\"view\":\"picView2\"}";

    private static final String PL_MUSIC =
        "{\"app\":\"com.tencent.structmsg\","
        + "\"config\":{\"ctime\":__T__,\"forward\":1,\"token\":\"\",\"type\":\"normal\"},"
        + "\"desc\":\"音乐分享\","
        + "\"meta\":{\"music\":{\"appid\":100495085,\"title\":\"__TITLE__\",\"desc\":\"__DESC__\","
        + "\"jumpUrl\":\"__JUMP__\",\"preview\":\"__PIC__\",\"tag\":\"QQ音乐\"}},"
        + "\"prompt\":\"[分享]__TITLE__\",\"view\":\"musicView\"}";

    private static final String PL_EGAME =
        "{\"app\":\"com.tencent.gamecenter.egameshare\",\"bizsrc\":\"adelie.agent_list\","
        + "\"config\":{\"autosize\":1,\"ctime\":__T__,\"forward\":1,\"token\":\"\",\"type\":\"normal\"},"
        + "\"desc\":\"游戏分享\",\"extra\":{\"app_type\":1,\"appid\":1105198412,\"msg_seq\":74515265555000,\"uin\":0},"
        + "\"meta\":{\"shareData\":{\"appid\":\"1104466820\",\"content\":\"__PROMPT__\",\"gameName\":\"__TITLE__\","
        + "\"scene\":\"8\",\"type\":\"image\",\"url\":\"__PIC__\",\"cover\":\"__PIC__\"}},"
        + "\"prompt\":\"__PROMPT__\",\"ver\":\"0.0.0.1\",\"view\":\"dataView\"}";

    public static String template(Spec spec) { return null; }

    private static final String EGAME_TPL =
        "{\n" +
        "  \"app\": \"com.tencent.gamecenter.egameshare\",\n" +
        "  \"bizsrc\": \"adelie.agent_list\",\n" +
        "  \"config\": {\"autosize\": 1, \"ctime\": __T__, \"forward\": 1, \"tips\": \"Signed By Arona\", \"token\": \"\", \"type\": \"normal\"},\n" +
        "  \"desc\": \"游戏分享\",\n" +
        "  \"extra\": {\"app_type\": 1, \"appid\": 1105198412, \"msg_seq\": 74515265555000, \"uin\": 0},\n" +
        "  \"meta\": {\n" +
        "    \"shareData\": {\n" +
        "      \"appid\": \"1105198412\",\n" +
        "      \"content\": \"外显文案\",\n" +
        "      \"gameName\": \"卡片标题\",\n" +
        "      \"scene\": \"8\",\n" +
        "      \"type\": \"video\",\n" +
        "      \"cover\": \"http%3A%2F%2Fp.qlogo.cn%2Fgdynamic%2FMIwbbVhjoVxibNJPGsv8JgNUvNicibdgnRe73ygUKmb104%2F0\",\n" +
        "      \"url\": \"https://upos-sz-mirrorcoso1.bilivideo.com/xxx.mp4\"\n" +
        "    }\n" +
        "  },\n" +
        "  \"prompt\": \"外显文案\",\n" +
        "  \"ver\": \"0.0.0.1\",\n" +
        "  \"view\": \"noDataView\"\n" +
        "}";

    private static final String WZRY_TPL =
        "{\n" +
        "  \"app\": \"com.tencent.gamecenter.gameshare_sgame\",\n" +
        "  \"bizsrc\": \"\",\n" +
        "  \"config\": {\"autoSize\": 0, \"ctime\": __T__, \"forward\": 1, \"height\": 300, \"tips\": \"Signed By 普拉娜\", \"token\": \"\", \"type\": \"normal\", \"width\": 300},\n" +
        "  \"desc\": \"游戏分享\",\n" +
        "  \"extra\": {\"app_type\": 1, \"appid\": 1104466820, \"uin\": 0},\n" +
        "  \"meta\": {\n" +
        "    \"shareData\": {\n" +
        "      \"appid\": \"1104466820\",\n" +
        "      \"cardUrl\": \"http%3a%2f%2fimage.smoba.qq.com%2fBanner%2fimg%2fQQArk%2fsharehero%2f116.jpg\",\n" +
        "      \"extData\": {\"gameSeq\": \"0\", \"gameSvrEntity\": \"0\", \"heroid\": \"116\", \"openId\": \"7C03E0196FC5DB35C6DDF0C07E6C7A76\", \"playerNum\": \"10\", \"relaySvrEntity\": \"0\"},\n" +
        "      \"openId\": \"7C03E0196FC5DB35C6DDF0C07E6C7A76\",\n" +
        "      \"roleInfo\": {\"avatar\": \"http%3a%2f%2fimage.smoba.qq.com%2fBanner%2fimg%2fQQArk%2fheroicon%2f116.png\", \"partition\": \"21杀/0助攻/2死亡\", \"roleName\": \"3)Lyko8-(5v5组队)\"},\n" +
        "      \"scene\": \"33\",\n" +
        "      \"textBoxData\": {\"desc\": \"21杀/0助攻/2死亡\", \"pic\": \"\", \"tips\": \"\", \"title\": \"超神\"},\n" +
        "      \"textGameData\": {\"data\": [{\"num\": \"21\", \"text\": \"击杀数\"}, {\"num\": \"2\", \"text\": \"死亡数\"}, {\"num\": \"0\", \"text\": \"助攻数\"}], \"title\": \"我的战绩\"},\n" +
        "      \"url\": \"http%3a%2f%2fimgcache.gtimg.cn%2fvipstyle%2fgame%2fact%2fnorth%2fark_gc843%2fwzry_game_share%2fbg.png\"\n" +
        "    }\n" +
        "  },\n" +
        "  \"prompt\": \"王者荣耀组队邀请\",\n" +
        "  \"ver\": \"0.0.0.19\",\n" +
        "  \"view\": \"commonView\"\n" +
        "}";

    private static final String QQSY_TPL =
        "{\n" +
        "  \"app\": \"com.tencent.gamecenter.qqsy\",\n" +
        "  \"bizsrc\": \"\",\n" +
        "  \"config\": {\"ctime\": __T__, \"forward\": 1, \"tips\": \"Signed By Atri\", \"token\": \"\", \"type\": \"normal\"},\n" +
        "  \"desc\": \"游戏分享\",\n" +
        "  \"extra\": {\"app_type\": 1, \"appid\": 1106040804, \"uin\": 0},\n" +
        "  \"meta\": {\"shareData\": {\"appid\": \"102755421\", \"desc\": \"描述\", \"preview\": \"http%3A%2F%2Fp.qlogo.cn%2Fgdynamic%2FMIwbbVhjoVxibNJPGsv8JgNUvNicibdgnRe73ygUKmb104%2F0\", \"title\": \"标题\"}},\n" +
        "  \"prompt\": \"\",\n" +
        "  \"ver\": \"0.0.0.1\",\n" +
        "  \"view\": \"picView2\"\n" +
        "}";

    private static final String MUSIC_TPL =
        "{\n" +
        "  \"app\": \"com.tencent.structmsg\",\n" +
        "  \"config\": {\"ctime\": __T__, \"forward\": 1, \"token\": \"\", \"type\": \"normal\"},\n" +
        "  \"desc\": \"音乐分享\",\n" +
        "  \"meta\": {\"music\": {\"appid\": 100495085, \"title\": \"歌名\", \"desc\": \"歌手\", \"jumpUrl\": \"https://y.qq.com/\", \"preview\": \"http%3A%2F%2Fp.qlogo.cn%2Fgdynamic%2FMIwbbVhjoVxibNJPGsv8JgNUvNicibdgnRe73ygUKmb104%2F0\", \"tag\": \"QQ音乐\", \"musicUrl\": \"https://ws.stream.qqmusic.qq.com/C400001Qu4I30eVFYb.m4a\"}},\n" +
        "  \"prompt\": \"[分享]歌名\",\n" +
        "  \"view\": \"musicView\"\n" +
        "}";

    public static String[] shapeNames(Spec spec) { return null; }

    public static String[] templates(Spec spec) { return null; }

    

    private static final String GS1 =
        "{\"app\":\"com.tencent.gamecenter.gameshare_sgame\",\"bizsrc\":\"\","
        + "\"config\":{\"autoSize\":0,\"ctime\":__T__,\"forward\":1,\"height\":300,"
        + "\"tips\":\"Signed By 猫娘\",\"token\":\"\",\"type\":\"normal\",\"width\":300},"
        + "\"desc\":\"__DESC__\",\"extra\":{\"app_type\":1,\"appid\":1104466820,\"uin\":0},"
        + "\"meta\":{\"baseInfo\":{\"from\":\"game\","
        + "\"gamedata\":\"ShareTeam_1716428535_1751_3132629816_1326187009_6271081750349731_0_1_1_1_20011_2_11_2_5_0_0_0_0_0_0\","
        + "\"iArea\":\"1\",\"iPartition\":\"1331\",\"iPlatId\":\"1\",\"iTeamBusId\":\"0\","
        + "\"iTeamId\":\"1326187009_1751_3132629816\",\"sAppid\":\"1104466820\","
        + "\"sOpenId\":\"3D4C2A5495DC007F3275653D45C73976\",\"teamLimit\":[],\"time\":\"1570795695\"}},"
        + "\"prompt\":\"__PROMPT__\",\"ver\":\"0.0.0.19\",\"view\":\"teamView\"}";

    private static final String GS2 =
        "{\"app\":\"com.tencent.gamecenter.gameshare_cfm\",\"bizsrc\":\"\","
        + "\"config\":{\"ctime\":__T__,\"forward\":1,\"tips\":\"Signed By 猫娘\",\"token\":\"\",\"type\":\"normal\"},"
        + "\"desc\":\"__DESC__\",\"extra\":{\"app_type\":1,\"appid\":1104512706,\"uin\":0},"
        + "\"meta\":{\"shareData\":{\"appid\":\"1104512706\",\"extData\":{},"
        + "\"openId\":\"4348ECE1E686F3C2FFE841875E7FB784\","
        + "\"roleInfo\":{\"avatar\":\"__PICU__\",\"partition\":\"__DESC__\",\"roleName\":\"__TITLE__\"},"
        + "\"scene\":\"324\","
        + "\"textBoxData\":{\"desc\":\"__DESC__\",\"pic\":\"__PICU__\",\"title\":\"__TITLE__\"},"
        + "\"textGameData\":{\"data\":[{\"num\":\"\",\"text\":\"\"}],\"title\":\"__TITLE__\"},"
        + "\"url\":\"__PICU__\"}},"
        + "\"prompt\":\"__PROMPT__\",\"ver\":\"0.0.0.1\",\"view\":\"commonView\"}";

    private static final String GS3 =
        "{\"app\":\"com.tencent.gamecenter.mdnf\",\"bizsrc\":\"\","
        + "\"config\":{\"ctime\":__T__,\"forward\":1,\"tips\":\"Signed By 猫娘\",\"token\":\"\",\"type\":\"normal\"},"
        + "\"desc\":\"游戏分享\",\"extra\":{\"app_type\":1,\"appid\":1104294055,\"uin\":0},"
        + "\"meta\":{\"shareData\":{\"appid\":\"1104294055\","
        + "\"openid\":\"C57F23B90C963E79E97A5CBB459F4EFA\","
        + "\"roleInfo\":{\"avatar\":\"__PICU__\",\"partition\":\"__DESC__\",\"roleName\":\"__TITLE__\"},"
        + "\"scene\":\"69aab638-e5d1-43df-9684-116ed8506025\","
        + "\"textBoxData\":{\"desc\":\"__DESC__\",\"pic\":\"\",\"title\":\"__TITLE__\"},"
        + "\"textGameData\":{\"data\":[{\"num\":\"__DESC__\",\"text\":\"__DESC__\"}],\"title\":\"__DESC__\"},"
        + "\"url\":\"__PICU__\"}},"
        + "\"prompt\":\"__PROMPT__\",\"ver\":\"0.0.0.1\",\"view\":\"commonView\"}";

    private static final String GS4 = PL_DZDP;

    private static final String CF_TPL =
        "{\n" +
        "  \"app\": \"com.tencent.gamecenter.gameshare_cfm\",\n" +
        "  \"bizsrc\": \"\",\n" +
        "  \"config\": {\"ctime\": __T__, \"forward\": 1, \"token\": \"\", \"type\": \"normal\"},\n" +
        "  \"desc\": \"游戏分享\",\n" +
        "  \"extra\": {\"app_type\": 1, \"appid\": 1104512706, \"uin\": 0},\n" +
        "  \"meta\": {\"shareData\": {\n" +
        "    \"appid\": \"1104512706\",\n" +
        "    \"extData\": {},\n" +
        "    \"openId\": \"4348ECE1E686F3C2FFE841875E7FB784\",\n" +
        "    \"roleInfo\": {\"avatar\": \"https://p.qlogo.cn/gdynamic/8DtZUpxHseiaRLpDrsgT9D74bar7hxmEsAC03dw1ceoU/0/10005\", \"partition\": \"卡片签名\", \"roleName\": \"卡片签名\"},\n" +
        "    \"scene\": \"324\",\n" +
        "    \"textBoxData\": {\"desc\": \"卡片签名\", \"pic\": \"http://p.qlogo.cn/homework/0/hw_h_bgbo5crclgoo046718ed0d69f6b/0/10005\", \"title\": \"穿越火线\"},\n" +
        "    \"textGameData\": {\"data\": [{\"num\": \"\", \"text\": \"\"}, {\"num\": \"\", \"text\": \"\"}, {\"num\": \"卡\", \"text\": \"\"}, {\"num\": \"片\", \"text\": \"\"}, {\"num\": \"\", \"text\": \"\"}, {\"num\": \"\", \"text\": \"\"}, {\"num\": \"签\", \"text\": \"\"}, {\"num\": \"名\", \"text\": \"\"}], \"title\": \"\"},\n" +
        "    \"url\": \"http%3a%2f%2fqgsk.yximg.cn%2fieg%2f296058%2f1588157240_99.jpeg\"\n" +
        "  }},\n" +
        "  \"prompt\": \"ark签名模块\",\n" +
        "  \"ver\": \"0.0.0.1\",\n" +
        "  \"view\": \"commonView\"\n" +
        "}";

    public static String compose(Spec spec, Map<String, String> v) { return null; }

    public static String composeWith(Spec spec, String tpl, Map<String, String> v) { return null; }

    private static String tokenFor(String key) { return null; }

    public static String urlEnc(String s) { return null; }

    private static String escapeJson(String s) { return null; }

    
    public static volatile String lastError = null;

    public static volatile int sendTimeoutMs = 4000;

    public static String jsonError(String s) { return null; }

    private static String where(String s, int pos, String why) { return null; }

    private static void skipWs(String s, int[] p) {  }

    private static void parseValue(String s, int[] p, int depth) {  }

    private static void expect(String s, int[] p, String lit) {  }

    private static void parseObject(String s, int[] p, int depth) {  }

    private static void parseArray(String s, int[] p, int depth) {  }

    private static void parseString(String s, int[] p) {  }

    private static void parseNumber(String s, int[] p) {  }

    public static String send(Spec spec, Map<String, String> values, android.app.Activity activity) { return null; }

    public static String sendWith(Spec spec, String tpl, Map<String, String> values,
                                  android.app.Activity activity) { return null; }

    private static String sendMini(Map<String, String> v) throws Exception { return null; }

    private static String sendEgame(Map<String, String> v, android.app.Activity activity) throws Exception { return null; }

    private static String egamePacket(String title, String prompt, String pic, String jump) throws Exception { return null; }

    
    private static String injectUin(String arkJson) { return null; }

    
    public static String sendNews(String tag, String title, String desc, String jump, String pic) { return null; }

    public static String sendNews(String tag, String title, String desc, String jump,
                                  String pic, long appid) { return null; }

    private static String nzs(String s) { return null; }

    public static String sendArk77(String arkJson) { return null; }

    private static String sendVia77(String arkJson) throws Exception { return null; }

    private static String deviceInfo() { return null; }

    private static String account() { return null; }

    private static void dump(String payload) {  }

    private static void dumpTo(String name, String payload) {  }

    private static void dumpBad(String key, String out) {  }
}
