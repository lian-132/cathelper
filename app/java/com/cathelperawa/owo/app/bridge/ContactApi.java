package com.cathelperawa.owo.app.bridge;

import com.cathelperawa.owo.app.core.dex.ClassResolver;
import com.cathelperawa.owo.app.core.HookLog;
import com.cathelperawa.owo.app.util.Refl;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ContactApi {

    private ContactApi() {}

    private static volatile Object sImpl;

    private static java.util.Map<String, String> fromKernel() {
        java.util.Map<String, String> out = new LinkedHashMap<String, String>();
        Object bs = QQEnv.buddyService();
        if (bs == null) return out;

        Object reqType = firstEnum("com.tencent.qqnt.kernel.nativeinterface.BuddyListReqType");
        Object listObj = callIt(bs, "getBuddyListFromCache", "", reqType);
        if (!(listObj instanceof List)) {
            listObj = callIt(bs, "getBuddyListFromCache", null, reqType);
        }
        if (!(listObj instanceof List)) {
            listObj = callIt(bs, "getBuddyListFromCache", "");
        }
        if (!(listObj instanceof List)) return out;

        List<?> list = (List<?>) listObj;
        if (list.isEmpty()) return out;

        Object first = list.get(0);

        if (!(first instanceof String)) {
            ArrayList<Object> expanded = new ArrayList<Object>();
            for (int i = 0; i < list.size(); i++) {
                Object cat = list.get(i);
                if (cat == null) continue;
                Object uids = callIt(cat, "getBuddyUids");
                if (uids instanceof List) {
                    expanded.addAll((List<?>) uids);
                }
            }
            if (!expanded.isEmpty()) {
                list = expanded;
                first = list.get(0);
            }
        }

        if (first instanceof String) {

            ArrayList<Object> uids = new ArrayList<Object>(list);
            Map nick = asMap(callIt(bs, "getBuddyNick", uids));
            Map remark = asMap(callIt(bs, "getBuddyRemark", uids));
            for (int i = 0; i < list.size(); i++) {
                String u = String.valueOf(list.get(i));
                String name = pick(remark, u);
                if (name == null || name.isEmpty()) name = pick(nick, u);
                out.put(u, name == null ? "" : name);
            }
        } else {

            for (int i = 0; i < list.size(); i++) {
                Object o = list.get(i);
                if (o == null) continue;
                String uid = Refl.firstStrOr(o, null,
                        "uid", "uin", "buddyUid", "buddyUin", "friendUin");
                String name = Refl.firstStrOr(o, "",
                        "remark", "nick", "name", "buddyNick", "remarkName");
                if (uid != null && uid.length() > 0) out.put(uid, name);
            }
        }
        return out;
    }

    private static Object callIt(Object target, String method, Object... args) {
        try {
            return Refl.call(target, method, args);
        } catch (Throwable t) {
            return null;
        }
    }

    private static Object firstEnum(String className) {
        try {
            Class<?> c = ClassResolver.find(QQEnv.classLoader(), className,
                    className);
            if (c == null || !c.isEnum()) return null;
            Object[] vals = c.getEnumConstants();
            return (vals == null || vals.length == 0) ? null : vals[0];
        } catch (Throwable t) {
            return null;
        }
    }

    private static Map asMap(Object o) {
        return (o instanceof Map) ? (Map) o : null;
    }

    private static String pick(Map m, String key) {
        if (m == null || key == null) return null;
        Object v = m.get(key);
        return v == null ? null : String.valueOf(v);
    }

    private static Object impl() {
        if (sImpl != null) return sImpl;
        ClassLoader cl = QQEnv.classLoader();

        try {
            Class<?> iface = ClassResolver.find(cl, "contact.utilsApi",
                    "com.tencent.mobileqq.utils.api.IContactUtils");
            Class<?> qroute = ClassResolver.find(cl, "qroute.di",
                    "com.tencent.mobileqq.qroute.QRoute");
            Method api = qroute.getMethod("api", Class.class);
            api.setAccessible(true);
            Object o = api.invoke(null, iface);
            if (o != null) { sImpl = o; return o; }
        } catch (Throwable ignore) {}

        try {
            Class<?> c = ClassResolver.find(cl, "contact.utilsImpl",
                    "com.tencent.mobileqq.utils.api.impl.ContactUtilsImpl");
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

        java.util.Map<String, String> kernel = fromKernel();
        if (!kernel.isEmpty()) {
            BUDDIES_CACHE = kernel;
            HookLog.i("allBuddies(kernel): " + kernel.size() + " 个好友");
            return new java.util.LinkedHashMap<String, String>(kernel);
        }

        java.util.Map<String, String> out = new java.util.LinkedHashMap<String, String>();
        ClassLoader cl = QQEnv.classLoader();
        try {
            Class<?> implCls = ClassResolver.find(cl, "friend.dataServiceImpl",
                    "com.tencent.mobileqq.friend.api.impl.FriendDataServiceImpl");
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
