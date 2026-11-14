package com.cathelperawa.owo.app.api;

import android.content.Context;

import com.cathelperawa.owo.app.bridge.QQEnv;
import com.cathelperawa.owo.app.bridge.TimHb;
import com.cathelperawa.owo.app.bridge.TimNet;
import com.cathelperawa.owo.app.feature.FeatureRedPacket;

import java.util.Map;

public final class RedPacketApi {

    private RedPacketApi() {}

    public static String probe() { return FeatureRedPacket.probe(); }

    public static boolean manualEnabled() {
        Context c = QQEnv.appContext();
        return c != null && FeatureRedPacket.manualOn(c);
    }

    public static void setManualEnabled(boolean on) {
        ConfigApi.setBool(FeatureRedPacket.K_MANUAL, on);
    }

    public static boolean autoEnabled() {
        Context c = QQEnv.appContext();
        return c != null && FeatureRedPacket.autoOn(c);
    }

    public static void setAutoEnabled(boolean on) {
        ConfigApi.setBool(FeatureRedPacket.K_AUTO, on);
    }

    public static void grab(Object msgItemOrRecord) {
        FeatureRedPacket.grabFromMsgItem(msgItemOrRecord);
    }

    public static com.cathelperawa.owo.app.model.CatResult grabResult(Object msgItemOrRecord) {
        return FeatureRedPacket.grabResult(msgItemOrRecord);
    }

    public static boolean send(String cgiName, String url, Map<String, String> params) {
        return TimHb.send(cgiName, url, params);
    }

    public static boolean send(String cgiName, String url, Map<String, String> params, int attempt) {
        return TimHb.send(cgiName, url, params, attempt);
    }

    public static int tryCount() { return TimHb.tryCount(); }

    public static boolean ready() { return TimHb.ready(); }

    public static void sink(TimHb.Sink s) { TimHb.setResultSink(s); }

    public static boolean netRequest(String url, Map<String, String> params, TimNet.Sink sink) {
        return TimNet.request(url, params, sink);
    }

    public static boolean netReady() { return TimNet.ready(); }

    public static void netDumpStructures() { TimNet.dumpStructures(); }
}
