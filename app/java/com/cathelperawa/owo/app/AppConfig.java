package com.cathelperawa.owo.app;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.LinkedHashMap;
import java.util.Map;

public class AppConfig {
    
    public static volatile AppConfig current;

    public boolean total = true;
    public boolean baseCat = true;
    public boolean wave = false;
    public boolean paw = false;
    public boolean catNameSwitch = false;
    public boolean masterNameSwitch = false;
    public boolean spoiled = true;
    public boolean endMeow = true;
    public boolean emoticon = false;
    public boolean autoSuffix = false;
    public boolean keywordReply = false;
    public boolean randomCute = false;
    public boolean trainSwitch = false;

    public String catName = "猫猫";
    public String masterName = "主人";
    public String trainPrefix = "";
    public String trainSuffix = "";
    public String trainKeywords = "";

    public String customReplyText = "";

    public boolean triggerOnPaste = false;

    public int floatSize = 34;

    public boolean settleTrigger = true;
    public long settleDelayMs = 500;
    public long cooldownMs = 1500;
    public boolean punctTrigger = true;

    public static final String PREF = "cat_helper_cfg";

    public static boolean moduleFlag(Context ctx, String key, boolean def) {
        try {
            java.io.File f = new java.io.File(ctx.getFilesDir(), "console");
            if (!f.exists()) return def;
            java.io.BufferedReader r = new java.io.BufferedReader(new java.io.FileReader(f));
            String line;
            while ((line = r.readLine()) != null) {
                int i = line.indexOf('=');
                if (i <= 0) continue;
                if (line.substring(0, i).trim().equals(key)) {
                    r.close();
                    return line.substring(i + 1).trim().equals("1");
                }
            }
            r.close();
        } catch (Throwable ignore) {
        }
        return def;
    }

    public static void setModuleFlag(Context ctx, String key, boolean on) {
        try {
            java.io.File f = new java.io.File(ctx.getFilesDir(), "console");
            java.util.Properties p = new java.util.Properties();
            if (f.exists()) {
                try (java.io.FileInputStream in = new java.io.FileInputStream(f)) {
                    p.load(in);
                } catch (Throwable ignore) {
                }
            }
            p.setProperty(key, on ? "1" : "0");
            try (java.io.FileOutputStream out = new java.io.FileOutputStream(f)) {
                p.store(out, null);
            }
        } catch (Throwable ignore) {
        }
    }

    public static AppConfig loadHost(Context ctx) {
        AppConfig c = new AppConfig();
        
        try {
            java.io.File f = new java.io.File(ctx.getFilesDir(), "console");
            if (f.exists()) {
                java.util.Properties p = new java.util.Properties();
                java.io.FileInputStream in = new java.io.FileInputStream(f);
                p.load(in);
                in.close();

                c.total = "1".equals(p.getProperty("cat_total", "0"));   
                c.baseCat = "1".equals(p.getProperty("cat_base", "1"));
                c.spoiled = "1".equals(p.getProperty("cat_spoiled", "1"));
                c.endMeow = "1".equals(p.getProperty("cat_endmeow", "1"));
                c.emoticon = "1".equals(p.getProperty("cat_emoticon", "1"));
                c.randomCute = "1".equals(p.getProperty("cat_cute", "1"));
                c.wave = "1".equals(p.getProperty("cat_wave", "1"));
                c.catNameSwitch = "1".equals(p.getProperty("cat_catname", "1"));
                c.masterNameSwitch = "1".equals(p.getProperty("cat_mastername", "1"));
                String cn = p.getProperty("cat_catname_val", "本喵");
                String mn = p.getProperty("cat_mastername_val", "主人");
                if (!cn.isEmpty()) c.catName = cn;
                if (!mn.isEmpty()) c.masterName = mn;
            }
        } catch (Throwable ignore) {
        }
        return c;
    }

    public static synchronized AppConfig load(Context c) {        SharedPreferences p = c.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        AppConfig cfg = new AppConfig();
        cfg.total = p.getBoolean("total", cfg.total);
        cfg.baseCat = p.getBoolean("baseCat", cfg.baseCat);
        cfg.wave = p.getBoolean("wave", cfg.wave);
        cfg.paw = p.getBoolean("paw", cfg.paw);
        cfg.catNameSwitch = p.getBoolean("catNameSwitch", cfg.catNameSwitch);
        cfg.masterNameSwitch = p.getBoolean("masterNameSwitch", cfg.masterNameSwitch);
        cfg.spoiled = p.getBoolean("spoiled", cfg.spoiled);
        cfg.endMeow = p.getBoolean("endMeow", cfg.endMeow);
        cfg.emoticon = p.getBoolean("emoticon", cfg.emoticon);
        cfg.autoSuffix = p.getBoolean("autoSuffix", cfg.autoSuffix);
        cfg.keywordReply = p.getBoolean("keywordReply", cfg.keywordReply);
        cfg.randomCute = p.getBoolean("randomCute", cfg.randomCute);
        cfg.trainSwitch = p.getBoolean("trainSwitch", cfg.trainSwitch);

        cfg.catName = p.getString("catName", cfg.catName);
        cfg.masterName = p.getString("masterName", cfg.masterName);
        cfg.trainPrefix = p.getString("trainPrefix", "");
        cfg.trainSuffix = p.getString("trainSuffix", "");
        cfg.trainKeywords = p.getString("trainKeywords", "");
        cfg.customReplyText = p.getString("customReplyText", "");
        cfg.triggerOnPaste = p.getBoolean("triggerOnPaste", cfg.triggerOnPaste);
        cfg.floatSize = p.getInt("floatSize", cfg.floatSize);
        cfg.settleTrigger = p.getBoolean("settleTrigger", true);
        cfg.settleDelayMs = p.getLong("settleDelayMs", 500);
        cfg.cooldownMs = p.getLong("cooldownMs", 1500);
        cfg.punctTrigger = p.getBoolean("punctTrigger", true);
        current = cfg;
        return cfg;
    }

    public void save(Context c) {
        SharedPreferences.Editor e = c.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit();
        e.putBoolean("total", total);
        e.putBoolean("baseCat", baseCat);
        e.putBoolean("wave", wave);
        e.putBoolean("paw", paw);
        e.putBoolean("catNameSwitch", catNameSwitch);
        e.putBoolean("masterNameSwitch", masterNameSwitch);
        e.putBoolean("spoiled", spoiled);
        e.putBoolean("endMeow", endMeow);
        e.putBoolean("emoticon", emoticon);
        e.putBoolean("autoSuffix", autoSuffix);
        e.putBoolean("keywordReply", keywordReply);
        e.putBoolean("randomCute", randomCute);
        e.putBoolean("trainSwitch", trainSwitch);

        e.putString("catName", safe(catName, "猫猫"));
        e.putString("masterName", safe(masterName, "主人"));
        e.putString("trainPrefix", safe(trainPrefix, ""));
        e.putString("trainSuffix", safe(trainSuffix, ""));
        e.putString("trainKeywords", safe(trainKeywords, ""));
        e.putString("customReplyText", safe(customReplyText, ""));
        e.putBoolean("triggerOnPaste", triggerOnPaste);
        e.putInt("floatSize", floatSize);
        e.putBoolean("settleTrigger", settleTrigger);
        e.putLong("settleDelayMs", settleDelayMs);
        e.putLong("cooldownMs", cooldownMs);
        e.putBoolean("punctTrigger", punctTrigger);
        e.apply();
        current = this;
    }

    private String safe(String s, String def) {
        if (s == null) return def;
        s = s.trim();
        return s.isEmpty() ? def : s;
    }

    public void enableAll() {

        baseCat = true;
        wave = true;
        paw = true;
        catNameSwitch = true;
        masterNameSwitch = true;
        spoiled = true;
        endMeow = true;
        emoticon = true;
        autoSuffix = true;
        keywordReply = true;
        randomCute = true;
        trainSwitch = true;
    }

    public void disableAll() {

        baseCat = false;
        wave = false;
        paw = false;
        catNameSwitch = false;
        masterNameSwitch = false;
        spoiled = false;
        endMeow = false;
        emoticon = false;
        autoSuffix = false;
        keywordReply = false;
        randomCute = false;
        trainSwitch = false;
    }

    public Map<String, String> parseCustomReplies() {
        LinkedHashMap<String, String> map = new LinkedHashMap<String, String>();
        if (customReplyText == null) return map;
        String[] lines = customReplyText.split("\\n");
        for (String line : lines) {
            if (line == null) continue;
            line = line.trim();
            if (line.isEmpty()) continue;
            int p = line.indexOf('=');
            if (p < 1) p = line.indexOf('＝');
            if (p < 1) continue;
            String k = line.substring(0, p).trim().toLowerCase();
            String v = line.substring(p + 1).trim();
            if (!k.isEmpty() && !v.isEmpty()) map.put(k, v);
        }
        return map;
    }
}
