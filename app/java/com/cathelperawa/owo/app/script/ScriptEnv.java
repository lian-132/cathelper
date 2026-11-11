package com.cathelperawa.owo.app.script;

public final class ScriptEnv {

    private ScriptEnv() {}

    private static final String[] IMPORT_PACKAGES = {
            
            "com.cathelperawa.owo.app.script.*",
            "com.cathelperawa.owo.app.ui.*",
            "com.cathelperawa.owo.app.api.*",
            "com.cathelperawa.owo.app.model.*",
            
            "android.view.*",
            "android.widget.*",
            "android.app.*",
            "android.content.*",
            
            "android.graphics.*",
            "android.graphics.drawable.*",
            "android.graphics.drawable.shapes.*",
            
            "android.os.*",
            "android.net.*",
            "android.text.*",
            "android.text.style.*",
            "android.util.*",
            "android.media.*",
            
            "java.util.*",
            "java.util.concurrent.*",
            "java.io.*",
            "java.lang.reflect.*",
            "java.text.*",
            "org.json.*",
    };

    private static final String[] OPTIONAL_PACKAGES = {
            "androidx.recyclerview.widget.*",
            "androidx.appcompat.app.*",
    };

    public static void apply(ScriptEngine it) {
        if (!(it instanceof BshEngine)) return;
        for (int i = 0; i < IMPORT_PACKAGES.length; i++) {
            try {
                it.eval("import " + IMPORT_PACKAGES[i] + ";");
            } catch (Throwable ignore) {
                
            }
        }
        for (int i = 0; i < OPTIONAL_PACKAGES.length; i++) {
            try {
                it.eval("import " + OPTIONAL_PACKAGES[i] + ";");
            } catch (Throwable ignore) {
            }
        }
    }

    public static void bindGlobals(ScriptEngine it, ScriptApi api) {
        if (api == null) return;
        Bridge b = new Bridge(api);
        
        bind(it, "getString", b, "getString");
        bind(it, "putString", b, "putString");
        bind(it, "getInt", b, "getInt");
        bind(it, "putInt", b, "putInt");
        bind(it, "getBoolean", b, "getBoolean");
        bind(it, "putBoolean", b, "putBoolean");
        bind(it, "getLong", b, "getLong");
        bind(it, "putLong", b, "putLong");
        
        bind(it, "qqToast", b, "qqToast");
        bind(it, "toast", b, "qqToast");          
        bind(it, "log", b, "log");
        bind(it, "getNowActivity", b, "getNowActivity");
        bind(it, "getNowContext", b, "getNowContext");
        
        bind(it, "sendMsg", b, "sendMsg");
        bind(it, "sendText", b, "sendText");
        bind(it, "sendPic", b, "sendPic");
        bind(it, "sendFile", b, "sendFile");
        bind(it, "sendPtt", b, "sendPtt");
        
        bind(it, "getClassLoader", b, "getClassLoader");
        bind(it, "getQQAppInterface", b, "getQQAppInterface");
    }

    private static void bind(ScriptEngine it, String name, Bridge b, String method) {
        java.lang.reflect.Method[] all = Bridge.class.getMethods();
        for (int i = 0; i < all.length; i++) {
            if (!all[i].getName().equals(method)) continue;
            try {
                it.bindMethod(name, b, all[i]);
            } catch (Throwable ignore) {
                
            }
        }
    }

    public static final class Bridge {

        private final ScriptApi api;

        Bridge(ScriptApi api) { this.api = api; }

        public String getString(String key) { return api.get(key); }

        public String getString(String key, String def) { return api.get(key, def); }

        public void putString(String key, String value) { api.set(key, value); }

        public int getInt(String key) { return api.getInt(key, 0); }

        public int getInt(String key, int def) { return api.getInt(key, def); }

        public void putInt(String key, int value) { api.setInt(key, value); }

        public boolean getBoolean(String key) { return api.getBool(key, false); }

        public boolean getBoolean(String key, boolean def) { return api.getBool(key, def); }

        public void putBoolean(String key, boolean value) { api.setBool(key, value); }

        public String getLong(String key, String def) { return api.get(key, def); }

        public void putLong(String key, long value) { api.set(key, String.valueOf(value)); }

        public void qqToast(String s) { api.toast(s == null ? "" : s); }

        public void log(String s) { api.log(s == null ? "" : s); }

        public android.app.Activity getNowActivity() { return api.activity(); }

        public android.content.Context getNowContext() { return api.context(); }

        
        public boolean sendMsg(String peer, String text) { return api.sendText("c2c", peer, text); }

        public boolean sendMsg(int chatType, String peer, String text) {
            return api.sendText(chatType == 2 ? "group" : "c2c", peer, text);
        }

        public boolean sendText(String peer, String text) { return api.sendText("c2c", peer, text); }

        public boolean sendText(int chatType, String peer, String text) {
            return api.sendText(chatType == 2 ? "group" : "c2c", peer, text);
        }

        public boolean sendPic(int chatType, String peer, String path) {
            return api.sendPic(chatType == 2 ? "group" : "c2c", peer, path);
        }

        public boolean sendFile(int chatType, String peer, String path) {
            return api.sendFile(chatType == 2 ? "group" : "c2c", peer, path);
        }

        public boolean sendPtt(int chatType, String peer, String path) {
            return api.sendPtt(chatType == 2 ? "group" : "c2c", peer, path);
        }

        public ClassLoader getClassLoader() {
            try {
                ClassLoader cl = com.cathelperawa.owo.app.bridge.QQEnv.classLoader();
                return cl != null ? cl : getClass().getClassLoader();
            } catch (Throwable t) {
                return getClass().getClassLoader();
            }
        }

        public Object getQQAppInterface() {
            try {
                return com.cathelperawa.owo.app.bridge.QQEnv.appRuntime();
            } catch (Throwable t) {
                return null;
            }
        }
    }
}
