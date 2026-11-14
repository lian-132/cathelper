package com.cathelperawa.owo.app.api;

import com.cathelperawa.owo.app.core.DataDir;

import android.content.Context;

import com.cathelperawa.owo.app.bridge.ElementFactory;
import com.cathelperawa.owo.app.bridge.QQEnv;
import com.cathelperawa.owo.app.core.HookLog;
import com.cathelperawa.owo.app.util.CatLog;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public final class IoApi {

    private IoApi() {}

    public static File dataDir() {
        try {
            Context ctx = QQEnv.appContext();
            if (ctx == null) return null;
            File d = DataDir.global();
            if (!d.exists()) d.mkdirs();
            return d;
        } catch (Throwable t) {
            return null;
        }
    }

    public static boolean writeFile(String name, String content) {
        File d = dataDir();
        if (d == null) return false;
        try {
            try (FileOutputStream o = new FileOutputStream(new File(d, name))) {
                o.write(content.getBytes(StandardCharsets.UTF_8));
            }
            return true;
        } catch (Throwable t) {
            CatLog.e("writeFile 失败", t);
            return false;
        }
    }

    public static String readFile(String name) {
        File d = dataDir();
        if (d == null) return null;
        try {
            File f = new File(d, name);
            if (!f.exists()) return null;
            byte[] b = new byte[(int) f.length()];
            int n;
            try (FileInputStream in = new FileInputStream(f)) {
                n = in.read(b);
            }
            return new String(b, 0, Math.max(n, 0), StandardCharsets.UTF_8);
        } catch (Throwable t) {
            return null;
        }
    }

    public static boolean appendFile(String name, String content) {
        File d = dataDir();
        if (d == null) return false;
        try {
            try (FileOutputStream o = new FileOutputStream(new File(d, name), true)) {
                o.write(content.getBytes(StandardCharsets.UTF_8));
            }
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean fileExists(String name) {
        File d = dataDir();
        return d != null && new File(d, name).exists();
    }

    public static boolean deleteFile(String name) {
        File d = dataDir();
        return d != null && new File(d, name).delete();
    }

    public static void logFile(String tag, String msg) { HookLog.i("[" + tag + "] " + msg); }

    public static List<String> listElementFactories() { return ElementFactory.listFactories(); }
}
