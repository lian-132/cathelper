package com.cathelperawa.owo.app.feature;

import com.cathelperawa.owo.app.bridge.CurrentChat;
import com.cathelperawa.owo.app.core.JsonPretty;

public final class OidbCard {

    private OidbCard() { }

    public static final long DEFAULT_APPID = 101846357L;

    public static final String CMD = "OidbSvc.0xdc2_34";

    public static long currentGroupUin() { return 0L; }

    public static String toDc2(String arkJson, long groupUin) { return null; }

    private static String q(String s) { return null; }

    

    
    public static final class Fields {
        public long appid = DEFAULT_APPID;
        public long uin = 0L;
        public String title = "";
        public String desc = "";
        public String tag = "";
        public String jump = "";
        public String preview = "";
    }

    public static String buildForSend(String title, String desc, String icon,
                                      String prompt, String url) { return null; }

    public static String buildForSend(String title, String desc, String icon,
                                      String prompt, String url, long appid) { return null; }

    public static Fields parse(String pbJson) { return null; }

    public static String toFakeArk(String pbJson) { return null; }

    public static String rebuild(String newArk, String oldPb) { return null; }

    public static String retarget(String pbJson, long uin) { return null; }

    public static String build(long appid, long uin, String title, String desc,
                               String tag, String jump, String preview) { return null; }
}
