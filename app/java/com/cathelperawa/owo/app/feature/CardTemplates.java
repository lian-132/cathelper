package com.cathelperawa.owo.app.feature;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public final class CardTemplates {

    public static final class Tpl {
        public String name = "";
        public String note = "";
        public String title = "";
        public String desc = "";
        public String pic = "";
        public String jump = "";
        public String tag = "";
        public String tagIcon = "";
        
        public String baseArk;
        
        public String route = "news77";
        
        public long appid = 100951776L;
    }

    private CardTemplates() { }

    public static final String ARK_LOVER =
            "{\"app\":\"com.tencent.tuwen.lua\",\"desc\":\"\",\"view\":\"news\","
            + "\"bizsrc\":\"qqconnect.sdkshare\",\"ver\":\"0.0.0.1\","
            + "\"prompt\":\"\\u200b\\u200d\\u200c\\u200b\\u200e\\u2060\\u200c\\u200c\\u200b\\u200b想和你建立情侣关系\","
            + "\"appID\":\"\",\"sourceName\":\"\",\"actionData\":\"\",\"actionData_A\":\"\",\"sourceUrl\":\"\","
            + "\"meta\":{\"news\":{\"app_type\":1,\"appid\":1106076350,\"ctime\":{{TIME}},"
            + "\"desc\":\"和我成为情侣，让QQ帮我们记录每日点滴\","
            + "\"jumpUrl\":\"{{JUMP}}\","
            + "\"preview\":\"{{PIC}}\","
            + "\"tag\":\"恋人空间\","
            + "\"tagIcon\":\"{{PIC}}\","
            + "\"title\":\"{{TITLE}}\",\"uin\":{{UIN}}}},"
            + "\"config\":{\"ctime\":{{TIME}},\"forward\":1,"
            + "\"token\":\"cdd93336c44a315d41b8e8b7512e454d\",\"type\":\"normal\"},"
            + "\"text\":\"\",\"extraApps\":[],\"sourceAd\":\"\",\"extra\":\"\"}";

    public static final String WL_PIC =
            "";
    public static final String WL_JUMP = "";
    public static final String ARK_MUSIC =
            "{\"app\":\"com.tencent.structmsg\",\"desc\":\"音乐\",\"view\":\"music\","
            + "\"ver\":\"0.0.0.1\",\"prompt\":\"[分享]{{TITLE}}\",\"appID\":\"\","
            + "\"meta\":{\"music\":{\"app_type\":1,\"appid\":100495085,\"ctime\":{{TIME}},"
            + "\"desc\":\"{{DESC}}\",\"jumpUrl\":\"{{JUMP}}\",\"preview\":\"{{PIC}}\","
            + "\"tag\":\"QQ音乐\",\"tagIcon\":\"https://i.gtimg.cn/open/app_icon/00/49/50/85/100495085_100_m.png\","
            + "\"title\":\"{{TITLE}}\",\"uin\":{{UIN}}}},"
            + "\"config\":{\"ctime\":{{TIME}},\"forward\":1,\"type\":\"normal\","
            + "\"token\":\"{{TOKEN}}\"},\"text\":\"\"}";

    public static final String ARK_MINIAPP =
            "{\"app\":\"com.tencent.miniapp_01\",\"desc\":\"\",\"view\":\"viewMultiMsg\","
            + "\"ver\":\"0.0.0.1\",\"prompt\":\"[分享]{{TITLE}}\",\"meta\":{\"detail_1\":{"
            + "\"appid\":\"1109937557\",\"app_type\":0,\"qqdocurl\":\"{{JUMP}}\","
            + "\"desc\":\"{{DESC}}\",\"icon\":\"{{PIC}}\",\"preview\":\"{{PIC}}\","
            + "\"title\":\"{{TITLE}}\",\"url\":\"{{JUMP}}\"}},\"config\":{\"type\":\"normal\"}}";

    public static List<Tpl> all() { return null; }

    public static String apply(String baseArk, String title, String desc, String pic,
                               String jump, String tag, String tagIcon,
                               long uin, long ctime) { return null; }

    public static String md5(String s) { return null; }
}
