package com.cathelperawa.owo.app.api;

import com.cathelperawa.owo.app.bridge.TimCookie;

import java.util.Map;

public final class CookieApi {

    private CookieApi() {}

    public static String pskey(String url) { return TimCookie.getPskey(url); }

    public static Map<String, String> pskeyBatch(String... urls) {
        return TimCookie.getPskeyBatch(urls);
    }

    public static String hexGuid() { return TimCookie.hexGuid(); }

    public static String skey() { return TimCookie.getSkey(); }

    public static String realSkey() { return TimCookie.getRealSkey(); }

    public static String stweb() { return TimCookie.getStweb(); }

    public static String pt4Token(String url) { return TimCookie.getPt4Token(url); }

    public static long bkn(String skey) { return TimCookie.getBkn(skey); }

    public static long bknOf(String key) { return TimCookie.bknOf(key); }

    public static String gtk(String url) { return TimCookie.getGTK(url); }

    public static String hostOf(String url) { return TimCookie.hostOf(url); }
}
