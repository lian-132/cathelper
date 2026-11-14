package com.cathelperawa.owo.app.cookie;

import java.util.ArrayList;
import java.util.List;

public final class CookiePlatforms {

    private CookiePlatforms() {}

    public static final int CHECK_NONE = 0;

    public static final int CHECK_ACCOUNT = 1;

    public static final int CHECK_BILI = 2;

    public static final int CHECK_QUARK = 3;

    public static final class P {
        public final String id;
        public final String name;

        public final String group;

        public final String shortName;

        public final int color;
        public final String loginUrl;
        public final String cookieUrl;
        public final String needField;
        public final String checkUrl;
        public final String referer;
        public final int checkKind;
        public final boolean ready;

        public P(String id, String name, String group, String shortName, int color,
                 String loginUrl, String cookieUrl, String needField,
                 String checkUrl, String referer, int checkKind, boolean ready) {
            this.id = id;
            this.name = name;
            this.group = group;
            this.shortName = shortName;
            this.color = color;
            this.loginUrl = loginUrl;
            this.cookieUrl = cookieUrl;
            this.needField = needField;
            this.checkUrl = checkUrl;
            this.referer = referer;
            this.checkKind = checkKind;
            this.ready = ready;
        }
    }

    public static final P[] ALL = {

            new P("netease", "网易云音乐", "音乐", "云", 0xFFE5352B,
                    "https://music.163.com/login",
                    "https://music.163.com/",
                    "MUSIC_U",
                    "https://music.163.com/api/nuser/account/get",
                    "https://music.163.com/",
                    CHECK_ACCOUNT, true),

            new P("bilibili", "哔哩哔哩", "视频", "B", 0xFFFB7299,
                    "https://passport.bilibili.com/login",
                    "https://www.bilibili.com/",
                    "SESSDATA",
                    "https://api.bilibili.com/x/web-interface/nav",
                    "https://www.bilibili.com/",
                    CHECK_BILI, true),

            new P("quark", "夸克网盘", "网盘", "夸", 0xFF5B5BF5,
                    "https://pan.quark.cn/?fr=pc&platform=pc",
                    "https://pan.quark.cn/",
                    "__pus",
                    "https://pan.quark.cn/account/info",
                    "https://pan.quark.cn/",
                    CHECK_QUARK, true),

            new P("baidu", "百度网盘", "网盘", "百", 0xFF2B6BF3,
                    "https://pan.baidu.com/",
                    "https://pan.baidu.com/",
                    "BDUSS",
                    "",
                    "https://pan.baidu.com/",
                    CHECK_NONE, true),

            new P("uc", "UC 网盘", "网盘", "U", 0xFFF5A623,
                    "https://drive.uc.cn/",
                    "https://drive.uc.cn/",
                    "__pus",
                    "https://drive.uc.cn/account/info",
                    "https://drive.uc.cn/",
                    CHECK_NONE, true),

            new P("xunlei", "迅雷网盘", "网盘", "迅", 0xFF3B82F6,
                    "https://pan.xunlei.com/",
                    "https://pan.xunlei.com/",
                    "",
                    "",
                    "https://pan.xunlei.com/",
                    CHECK_NONE, true),

            new P("c139", "139 网盘", "网盘", "139", 0xFF1E88E5,
                    "https://yun.139.com/",
                    "https://yun.139.com/",
                    "authorization",
                    "",
                    "https://yun.139.com/",
                    CHECK_NONE, true),

            new P("pan123", "123 云盘", "网盘", "123", 0xFF6D4AFF,
                    "https://yun.123pan.cn/",
                    "https://yun.123pan.cn/",
                    "",
                    "",
                    "https://yun.123pan.cn/",
                    CHECK_NONE, true),

            new P("pan115", "115 网盘", "网盘", "5", 0xFF3B82F6,
                    "https://115.com/",
                    "https://115.com/",
                    "",
                    "",
                    "https://115.com/",
                    CHECK_NONE, false),

            new P("guangya", "光鸭云盘", "网盘", "鸭", 0xFFF59E0B,
                    "https://www.guangyapan.com/",
                    "https://www.guangyapan.com/",
                    "",
                    "",
                    "https://www.guangyapan.com/",
                    CHECK_NONE, false),

            new P("lanzou", "蓝奏云", "网盘", "蓝", 0xFF60A5FA,
                    "https://pc.woozooo.com/mydisk.php",
                    "https://pc.woozooo.com/",
                    "phpdisk_info",
                    "",
                    "https://pc.woozooo.com/",
                    CHECK_NONE, true),

            new P("ilanzou", "蓝奏云优享版", "网盘", "优", 0xFF38BDF8,
                    "https://www.ilanzou.com/",
                    "https://www.ilanzou.com/",
                    "",
                    "",
                    "https://www.ilanzou.com/",
                    CHECK_NONE, false),

            new P("douyin", "抖音", "视频", "抖", 0xFF111827,
                    "https://www.douyin.com/",
                    "https://www.douyin.com/",
                    "",
                    "",
                    "https://www.douyin.com/",
                    CHECK_NONE, false),

            new P("kuaishou", "快手", "视频", "快", 0xFFFF5A1F,
                    "https://www.kuaishou.com/",
                    "https://www.kuaishou.com/",
                    "",
                    "",
                    "https://www.kuaishou.com/",
                    CHECK_NONE, false),

            new P("weibo", "微博", "视频", "微", 0xFFE6162D,
                    "https://weibo.com/",
                    "https://weibo.com/",
                    "",
                    "",
                    "https://weibo.com/",
                    CHECK_NONE, false),

            new P("acfun", "AcFun", "视频", "A", 0xFFFD4C5C,
                    "https://www.acfun.cn/",
                    "https://www.acfun.cn/",
                    "",
                    "",
                    "https://www.acfun.cn/",
                    CHECK_NONE, false),

            new P("xiaohongshu", "小红书", "视频", "红", 0xFFFF2442,
                    "https://www.xiaohongshu.com/",
                    "https://www.xiaohongshu.com/",
                    "",
                    "",
                    "https://www.xiaohongshu.com/",
                    CHECK_NONE, false),

            new P("pixiv", "Pixiv", "视频", "P", 0xFF0096FA,
                    "https://www.pixiv.net/",
                    "https://www.pixiv.net/",
                    "",
                    "",
                    "https://www.pixiv.net/",
                    CHECK_NONE, false),

            new P("github", "GitHub", "其他", "GH", 0xFF24292F,
                    "https://github.com/login",
                    "https://github.com/",
                    "",
                    "",
                    "https://github.com/",
                    CHECK_NONE, false)
    };

    public static P byId(String id) {
        if (id == null) return null;
        for (P p : ALL) {
            if (p.id.equals(id)) return p;
        }
        return null;
    }

    public static P firstReady() {
        for (P p : ALL) {
            if (p.ready) return p;
        }
        return ALL[0];
    }

    public static List<P> byGroup(String group) {
        List<P> out = new ArrayList<P>();
        for (P p : ALL) {
            if (p.group.equals(group)) out.add(p);
        }
        return out;
    }

    public static List<String> groups() {
        List<String> out = new ArrayList<String>();
        for (P p : ALL) {
            if (!out.contains(p.group)) out.add(p.group);
        }
        return out;
    }
}
