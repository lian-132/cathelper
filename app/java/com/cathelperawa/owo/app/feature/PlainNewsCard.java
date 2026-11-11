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

    private PlainNewsCard() { }

    public static final class Result {
        public boolean ok;
        public String error = "";
    }

    public static Result send(Context ctx, String tag, String title, String desc,
                              String jump, String preview) { return null; }

    private static String nz(String s) { return null; }

    private static long targetUin() { return 0L; }

    private static long parseLong(String s) { return 0L; }
}
