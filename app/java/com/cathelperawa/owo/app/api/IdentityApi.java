package com.cathelperawa.owo.app.api;

import com.cathelperawa.owo.app.bridge.ContactApi;
import com.cathelperawa.owo.app.bridge.QQEnv;
import com.cathelperawa.owo.app.bridge.QQUtils;
import com.cathelperawa.owo.app.bridge.TroopApi;
import com.cathelperawa.owo.app.util.CatLog;

import java.lang.reflect.Method;

public final class IdentityApi {

    private IdentityApi() {}

    
    public static String buddyName(String uin) { return ContactApi.getBuddyName(uin); }

    public static String buddyNameByUid(String uid) { return ContactApi.getBuddyNameByUid(uid); }

    public static String troopMemberName(String troopUin, String uin) {
        return TroopApi.getMemberName(troopUin, uin);
    }

    
    public static String troopName(String troopUin) { return TroopApi.getTroopName(troopUin); }

    public static java.util.Map<String, String> allTroopNames() {
        return TroopApi.allTroopNames();
    }

    public static java.util.Map<String, String> allBuddies() {
        return ContactApi.allBuddies();
    }

    
    public static String uidFromUin(String uin) { return QQUtils.uidFromUin(uin); }

    public static String uinFromUid(String uid) { return QQUtils.uinFromUid(uid); }

    public static boolean isFriend(String uin) { return QQUtils.isFriend(uin); }

    public static String currentNick() {
        try {
            Object rt = QQEnv.appRuntime();
            if (rt == null) return null;
            for (Method m : rt.getClass().getMethods()) {
                if ("getCurrentNickname".equals(m.getName()) && m.getParameterTypes().length == 0) {
                    m.setAccessible(true);
                    Object v = m.invoke(rt);
                    return v == null ? null : String.valueOf(v);
                }
            }
        } catch (Throwable t) {
            CatLog.d("currentNick 失败: " + t);
        }
        return null;
    }

    public static String currentUid() { return QQEnv.currentUid(); }

    public static String currentUin() { return QQEnv.currentUin(); }

    public static android.content.Context appContext() { return QQEnv.appContext(); }

    public static android.app.Activity topActivity() { return QQEnv.topActivity(); }

    public static ClassLoader classLoader() { return QQEnv.classLoader(); }

    
    public static Object msgServiceImpl() { return QQEnv.msgServiceImpl(); }

    public static Object nativeMsgService() { return QQEnv.nativeMsgService(); }

    public static Object wrapperSession() { return QQEnv.wrapperSession(); }
}
