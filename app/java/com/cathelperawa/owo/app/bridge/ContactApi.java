package com.cathelperawa.owo.app.bridge;

import com.cathelperawa.owo.app.core.HookLog;

import java.lang.reflect.Method;

public final class ContactApi {

    private ContactApi() {}

    private static volatile Object sImpl;

    private static Object impl() {
        if (sImpl != null) return sImpl;
        ClassLoader cl = QQEnv.classLoader();

        try {
            Class<?> iface = Class.forName(
                    "com.tencent.mobileqq.utils.api.IContactUtils", false, cl);
            Class<?> qroute = Class.forName(
                    "com.tencent.mobileqq.qroute.QRoute", false, cl);
            Method api = qroute.getMethod("api", Class.class);
            api.setAccessible(true);
            Object o = api.invoke(null, iface);
            if (o != null) { sImpl = o; return o; }
        } catch (Throwable ignore) {}

        try {
            Class<?> c = Class.forName(
                    "com.tencent.mobileqq.utils.api.impl.ContactUtilsImpl", false, cl);
            sImpl = c.newInstance();
            return sImpl;
        } catch (Throwable t) {
            HookLog.w("ContactApi 实例化失败: " + t);
        }
        return null;
    }

    public static String getBuddyName(String uin) {
        if (uin == null || uin.isEmpty()) return null;
        try {
            Object im = impl();
            Object app = QQEnv.appRuntime();
            if (im == null || app == null) return null;
            for (Method m : im.getClass().getMethods()) {
                if (!"getBuddyNickName".equals(m.getName())) continue;
                Class<?>[] ps = m.getParameterTypes();
                if (ps.length != 3) continue;
                m.setAccessible(true);
                Object r = m.invoke(im, app, uin, Boolean.TRUE);
                if (r != null) {
                    String s = String.valueOf(r).trim();
                    if (s.length() > 0 && !s.equals(uin) && !s.startsWith("u_")) return s;
                }
            }
        } catch (Throwable t) {
            HookLog.w("getBuddyName 失败: " + t);
        }
        return null;
    }

    public static String getBuddyNameByUid(String uid) {
        if (uid == null || uid.isEmpty()) return null;
        try {
            String uin = QQUtils.uinFromUid(uid);
            if (uin != null && !uin.isEmpty()) {
                String n = getBuddyName(uin);
                if (n != null) return n;
            }
        } catch (Throwable ignore) {}
        return null;
    }

    private static java.util.Map<String, String> BUDDIES_CACHE = null;

    public static synchronized java.util.Map<String, String> allBuddies() {
        if (BUDDIES_CACHE != null) {
            return new java.util.LinkedHashMap<String, String>(BUDDIES_CACHE);
        }
        java.util.Map<String, String> out = new java.util.LinkedHashMap<String, String>();
        ClassLoader cl = QQEnv.classLoader();
        try {
            Class<?> implCls = Class.forName(
                    "com.tencent.mobileqq.friend.api.impl.FriendDataServiceImpl", false, cl);
            Object app = QQEnv.appRuntime();
            if (app == null) return out;

            Object svc = null;
            for (Method m : implCls.getMethods()) {
                if (!"getService".equals(m.getName())) continue;
                if (m.getParameterTypes().length != 1) continue;
                if (!m.getParameterTypes()[0].isInstance(app)) continue;
                m.setAccessible(true);
                svc = m.invoke(null, app);
                if (svc != null) break;
            }
            if (svc == null) {
                HookLog.w("allBuddies：拿不到 FriendDataServiceImpl 实例");
                return out;
            }

            Object list = null;
            String[] wants = new String[]{
                    "getBuddyListFromCache", "getBuddyList", "getAllFriendList"
            };
            for (String wn : wants) {
                for (Method m : svc.getClass().getMethods()) {
                    if (!wn.equals(m.getName())) continue;
                    if (m.getParameterTypes().length != 0) continue;
                    m.setAccessible(true);
                    try {
                        Object r = m.invoke(svc);
                        if (r instanceof java.util.List && !((java.util.List<?>) r).isEmpty()) {
                            list = r;
                            break;
                        }
                    } catch (Throwable ignore) {
                    }
                }
                if (list != null) break;
            }
            if (!(list instanceof java.util.List)) {
                HookLog.w("allBuddies：拿不到好友列表");
                return out;
            }

            for (Object fr : (java.util.List<?>) list) {
                if (fr == null) continue;
                String uin = com.cathelperawa.owo.app.util.Refl.str(
                        com.cathelperawa.owo.app.util.Refl.field(fr, "uin"));
                if (uin == null || uin.isEmpty()) {
                    uin = com.cathelperawa.owo.app.util.Refl.str(
                            com.cathelperawa.owo.app.util.Refl.field(fr, "friendUin"));
                }
                if (uin == null || uin.isEmpty()) continue;
                String nm = com.cathelperawa.owo.app.util.Refl.str(
                        com.cathelperawa.owo.app.util.Refl.field(fr, "remark"));
                if (nm == null || nm.trim().isEmpty()) {
                    nm = com.cathelperawa.owo.app.util.Refl.str(
                            com.cathelperawa.owo.app.util.Refl.field(fr, "name"));
                }
                if (nm == null || nm.trim().isEmpty()) nm = uin;
                out.put(uin, nm);
            }
            HookLog.i("allBuddies：拿到 " + out.size() + " 个好友");
        } catch (Throwable t) {
            HookLog.w("allBuddies 失败: " + t);
        }
        BUDDIES_CACHE = out;
        return new java.util.LinkedHashMap<String, String>(out);
    }
}
