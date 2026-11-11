package com.cathelperawa.owo.app.bili;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public final class BiliFilter {

    private BiliFilter() {}

    public static final String K_MIN_DUR = "bili_f_min_dur";
    public static final String K_MIN_PLAY = "bili_f_min_play";
    public static final String K_MIN_LIKE = "bili_f_min_like";
    public static final String K_BAN = "bili_f_ban";

    private static int minDur = 60;
    private static int minPlay = 1000;
    private static int minLikePct = 0;      
    private static Pattern ban = null;
    private static long loadedAt = 0L;

    public static void reload(Context ctx) {
        long now = System.currentTimeMillis();
        if (now - loadedAt < 30_000L) return;
        loadedAt = now;
        try {
            SharedPreferences sp = ctx.getSharedPreferences(
                    com.cathelperawa.owo.app.AppConfig.PREF, Context.MODE_PRIVATE);
            minDur = sp.getInt(K_MIN_DUR, 60);
            minPlay = sp.getInt(K_MIN_PLAY, 1000);
            minLikePct = sp.getInt(K_MIN_LIKE, 0);
            String words = sp.getString(K_BAN, "");
            ban = (words == null || words.trim().length() == 0)
                    ? null : Pattern.compile(words.trim(), Pattern.CASE_INSENSITIVE);
        } catch (Throwable t) {
            minDur = 60;
            minPlay = 1000;
            minLikePct = 0;
            ban = null;
        }
    }

    public static boolean drop(BiliApi.Item it) {
        if (it == null) return true;
        if (minDur > 0 && it.duration > 0 && it.duration < minDur) return true;
        if (minPlay > 0 && it.play > 0 && it.play < minPlay) return true;
        if (ban != null && it.title != null && ban.matcher(it.title).find()) return true;
        return false;
    }

    public static List<BiliApi.Item> apply(List<BiliApi.Item> src) {
        if (src == null || src.isEmpty()) return src;
        List<BiliApi.Item> out = new ArrayList<BiliApi.Item>(src.size());
        for (BiliApi.Item it : src) {
            if (!drop(it)) out.add(it);
        }
        return out.isEmpty() ? src : out;
    }
}
