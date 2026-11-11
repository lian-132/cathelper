package com.cathelperawa.owo.app.api;

import android.os.Bundle;

import com.cathelperawa.owo.app.bridge.TimServlet;

public final class ServletApi {

    private ServletApi() {}

    public interface Callback extends TimServlet.Callback {}

    public static boolean send(String cmd, byte[] data, Callback cb) {
        return TimServlet.send(cmd, data, cb);
    }

    public static boolean send(String cmd, byte[] data, long timeout, Callback cb) {
        return TimServlet.send(cmd, data, timeout, cb);
    }

    public static String className() { return TimServlet.servletClassName(); }

    public static boolean setClassName(String className) {
        return TimServlet.setServletClass(className);
    }

    public static byte[] bytes(Bundle bundle) { return TimServlet.responseBytes(bundle); }

    public static String dumpBundle(Bundle bundle) { return TimServlet.dumpBundle(bundle); }
}
