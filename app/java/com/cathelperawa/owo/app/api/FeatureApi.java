package com.cathelperawa.owo.app.api;

import android.content.Context;

import com.cathelperawa.owo.app.bridge.QQEnv;
import com.cathelperawa.owo.app.core.HookConfig;
import com.cathelperawa.owo.app.feature.FeatureApkRename;
import com.cathelperawa.owo.app.feature.FeatureCustomDevice;
import com.cathelperawa.owo.app.feature.FeatureFakeNetwork;
import com.cathelperawa.owo.app.feature.FeatureFakePhone;
import com.cathelperawa.owo.app.feature.FeatureGroupClockIn;
import com.cathelperawa.owo.app.feature.FeatureMsgTime;
import com.cathelperawa.owo.app.feature.FeaturePicSummary;
import com.cathelperawa.owo.app.feature.FeatureQqBrowser;
import com.cathelperawa.owo.app.feature.FeatureViewCard;
import com.cathelperawa.owo.app.util.CatLog;

import java.util.ArrayList;
import java.util.List;

public final class FeatureApi {

    private FeatureApi() {}

    

    private static Context ctx() {
        try {
            return QQEnv.appContext();
        } catch (Throwable t) {
            return null;
        }
    }

    private static String get(String key, String def) {
        try {
            return HookConfig.get(ctx(), key, def);
        } catch (Throwable t) {
            return def;
        }
    }

    private static boolean getBool(String key, boolean def) {
        try {
            return HookConfig.getBoolean(ctx(), key, def);
        } catch (Throwable t) {
            return def;
        }
    }

    private static void set(String key, String value) {
        try {
            HookConfig.set(ctx(), key, value);
        } catch (Throwable t) {
            CatLog.w("FeatureApi 写配置失败 " + key + ": " + t);
        }
    }

    private static void setBool(String key, boolean value) {
        try {
            Context c = ctx();
            if (c == null) {
                CatLog.w("FeatureApi 写开关失败 " + key + ": 没有 Context");
                return;
            }
            HookConfig.set(c, key, value ? "true" : "false");
        } catch (Throwable t) {
            CatLog.w("FeatureApi 写开关失败 " + key + ": " + t);
        }
    }

    private static int getInt(String key, int def) {
        try {
            String v = get(key, null);
            if (v == null || v.trim().length() == 0) return def;
            return Integer.parseInt(v.trim());
        } catch (Throwable t) {
            return def;
        }
    }

    

    
    public static boolean clockInOn() {
        return getBool(FeatureGroupClockIn.K, false);
    }

    public static void setClockInOn(boolean on) {
        setBool(FeatureGroupClockIn.K, on);
    }

    public static List<String> clockInGroups() {
        return FeatureGroupClockIn.apiSelectedGroups();
    }

    public static void setClockInGroups(List<String> groups) {
        set(FeatureGroupClockIn.K_GROUPS, joinTroops(groups));
    }

    public static boolean addClockInGroup(String troopUin) {
        if (!isTroopUin(troopUin)) return false;
        List<String> list = clockInGroups();
        String g = troopUin.trim();
        if (list.contains(g)) return false;
        list.add(g);
        setClockInGroups(list);
        return true;
    }

    public static boolean removeClockInGroup(String troopUin) {
        if (!isTroopUin(troopUin)) return false;
        List<String> list = clockInGroups();
        if (!list.remove(troopUin.trim())) return false;
        setClockInGroups(list);
        return true;
    }

    public static List<String> clockInPending() {
        try {
            return FeatureGroupClockIn.apiPendingGroups();
        } catch (Throwable t) {
            return new ArrayList<String>();
        }
    }

    public static List<String> clockInDone() {
        try {
            return FeatureGroupClockIn.apiDoneGroups();
        } catch (Throwable t) {
            return new ArrayList<String>();
        }
    }

    public static boolean clockInReady() {
        try {
            return FeatureGroupClockIn.apiReady();
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean clockInNow() {
        try {
            return FeatureGroupClockIn.apiTriggerNow();
        } catch (Throwable t) {
            CatLog.w("clockInNow 失败: " + t);
            return false;
        }
    }

    public static int clockInConcurrency() {
        return getInt(FeatureGroupClockIn.K_CONC, 32);
    }

    public static void setClockInConcurrency(int n) {
        if (n < 0) n = 0;
        if (n > 256) n = 256;
        set(FeatureGroupClockIn.K_CONC, String.valueOf(n));
    }

    public static int clockInDeadlineSec() {
        return getInt(FeatureGroupClockIn.K_DEADLINE, 300);
    }

    public static void setClockInDeadlineSec(int sec) {
        if (sec < 0) sec = 0;
        set(FeatureGroupClockIn.K_DEADLINE, String.valueOf(sec));
    }

    public static void clearClockInLedger() {
        set(FeatureGroupClockIn.K_DONE_DATE, "");
        set(FeatureGroupClockIn.K_DONE, "");
    }

    

    
    public static boolean viewCardOn() {
        return getBool(FeatureViewCard.K, false);
    }

    public static void setViewCardOn(boolean on) {
        setBool(FeatureViewCard.K, on);
    }

    

    
    public static boolean msgTimeOn() {
        return getBool(FeatureMsgTime.K, false);
    }

    public static void setMsgTimeOn(boolean on) {
        setBool(FeatureMsgTime.K, on);
    }

    public static String msgTimeFormat() {
        String v = get(FeatureMsgTime.K_FORMAT, FeatureMsgTime.DEF_FORMAT);
        return (v == null || v.trim().length() == 0) ? FeatureMsgTime.DEF_FORMAT : v;
    }

    public static void setMsgTimeFormat(String pattern) {
        set(FeatureMsgTime.K_FORMAT, pattern == null ? "" : pattern);
    }

    public static int msgTimeColor() {
        return getInt(FeatureMsgTime.K_COLOR, 0xFF0000FF);
    }

    public static void setMsgTimeColor(int argb) {
        set(FeatureMsgTime.K_COLOR, String.valueOf(argb));
    }

    public static int msgTimeSize() {
        int v = getInt(FeatureMsgTime.K_SIZE, 10);
        return v <= 0 ? 10 : v;
    }

    public static void setMsgTimeSize(int sp) {
        if (sp < 6) sp = 6;
        if (sp > 40) sp = 40;
        set(FeatureMsgTime.K_SIZE, String.valueOf(sp));
    }

    

    
    public static boolean picSummaryOn() {
        try {
            return FeaturePicSummary.on(ctx());
        } catch (Throwable t) {
            return false;
        }
    }

    public static void setPicSummaryOn(boolean on) {
        setBool(FeaturePicSummary.K_ON, on);
    }

    public static String picSummaryText() {
        try {
            return FeaturePicSummary.text(ctx());
        } catch (Throwable t) {
            return null;
        }
    }

    public static void setPicSummaryText(String text) {
        try {
            FeaturePicSummary.setText(ctx(), text == null ? "" : text);
        } catch (Throwable t) {
            CatLog.w("setPicSummaryText 失败: " + t);
        }
    }

    

    
    public static boolean fakePhoneOn() {
        return getBool(FeatureFakePhone.K, false);
    }

    public static void setFakePhoneOn(boolean on) {
        setBool(FeatureFakePhone.K, on);
    }

    public static String fakePhone() {
        String v = get(FeatureFakePhone.CFG_PHONE, "");
        if (v == null || v.trim().length() == 0) return FeatureFakePhone.DEFAULT_PHONE;
        return v;
    }

    public static void setFakePhone(String number) {
        set(FeatureFakePhone.CFG_PHONE, number == null ? "" : number.trim());
    }

    

    
    public static boolean fakeNetworkOn() {
        return getBool(FeatureFakeNetwork.K, false);
    }

    public static void setFakeNetworkOn(boolean on) {
        setBool(FeatureFakeNetwork.K, on);
    }

    public static int fakeNetworkMode() {
        int m = getInt(FeatureFakeNetwork.CFG_MODE, FeatureFakeNetwork.MODE_WIFI);
        if (m != FeatureFakeNetwork.MODE_WIFI
                && m != FeatureFakeNetwork.MODE_5G
                && m != FeatureFakeNetwork.MODE_4G) {
            return FeatureFakeNetwork.MODE_WIFI;
        }
        return m;
    }

    public static void setFakeNetworkMode(int mode) {
        if (mode != FeatureFakeNetwork.MODE_WIFI
                && mode != FeatureFakeNetwork.MODE_5G
                && mode != FeatureFakeNetwork.MODE_4G) {
            mode = FeatureFakeNetwork.MODE_WIFI;
        }
        set(FeatureFakeNetwork.CFG_MODE, String.valueOf(mode));
    }

    public static String fakeNetworkModeName(int mode) {
        try {
            return FeatureFakeNetwork.modeName(mode);
        } catch (Throwable t) {
            return "?";
        }
    }

    

    
    public static boolean customDeviceOn() {
        return getBool(FeatureCustomDevice.K, false);
    }

    public static void setCustomDeviceOn(boolean on) {
        setBool(FeatureCustomDevice.K, on);
    }

    public static String customDevice() {
        return nz(get(FeatureCustomDevice.CFG_DEVICE, ""));
    }

    public static String customModel() {
        return nz(get(FeatureCustomDevice.CFG_MODEL, ""));
    }

    public static String customManufacturer() {
        return nz(get(FeatureCustomDevice.CFG_MFR, ""));
    }

    public static void setCustomDevice(String device, String model, String manufacturer) {
        if (device != null && device.trim().length() > 0) {
            set(FeatureCustomDevice.CFG_DEVICE, device.trim());
        }
        if (model != null && model.trim().length() > 0) {
            set(FeatureCustomDevice.CFG_MODEL, model.trim());
        }
        if (manufacturer != null && manufacturer.trim().length() > 0) {
            set(FeatureCustomDevice.CFG_MFR, manufacturer.trim());
        }
    }

    

    
    public static boolean apkRenameOn() {
        return getBool(FeatureApkRename.K, false);
    }

    public static void setApkRenameOn(boolean on) {
        setBool(FeatureApkRename.K, on);
    }

    public static String apkNameOf(String path) {
        if (path == null) return null;
        if (!path.toLowerCase(java.util.Locale.ROOT).endsWith(".apk")) return null;
        Context c = ctx();
        if (c == null) return null;
        try {
            android.content.pm.PackageManager pm = c.getPackageManager();
            android.content.pm.PackageInfo pi = pm.getPackageArchiveInfo(path, 0);
            if (pi == null) return null;
            String label = null;
            if (pi.applicationInfo != null) {
                
                pi.applicationInfo.sourceDir = path;
                pi.applicationInfo.publicSourceDir = path;
                try {
                    CharSequence l = pm.getApplicationLabel(pi.applicationInfo);
                    if (l != null) label = l.toString();
                } catch (Throwable ignore) {
                }
            }
            if (label == null || label.length() == 0) label = pi.packageName;
            String ver = pi.versionName;
            if (ver == null || ver.length() == 0) ver = String.valueOf(pi.versionCode);
            return (label + "_" + ver + ".APK")
                    .replace('/', '_').replace('\0', '_').replace(':', '_');
        } catch (Throwable t) {
            return null;
        }
    }

    

    
    public static boolean qqBrowserOn() {
        return getBool(FeatureQqBrowser.K, false);
    }

    public static void setQqBrowserOn(boolean on) {
        setBool(FeatureQqBrowser.K, on);
    }

    

    
    public static boolean isTroopUin(String s) {
        if (s == null) return false;
        String t = s.trim();
        if (t.length() < 5) return false;
        for (int i = 0; i < t.length(); i++) {
            if (t.charAt(i) < '0' || t.charAt(i) > '9') return false;
        }
        return true;
    }

    public static String joinTroops(List<String> groups) {
        StringBuilder sb = new StringBuilder();
        List<String> seen = new ArrayList<String>();
        if (groups != null) {
            for (String g : groups) {
                if (!isTroopUin(g)) continue;
                String t = g.trim();
                if (seen.contains(t)) continue;
                seen.add(t);
                if (sb.length() > 0) sb.append(',');
                sb.append(t);
            }
        }
        return sb.toString();
    }

    private static String nz(String s) {
        return (s == null || "null".equals(s)) ? "" : s.trim();
    }
}
