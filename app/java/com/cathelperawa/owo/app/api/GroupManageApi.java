package com.cathelperawa.owo.app.api;

import com.cathelperawa.owo.app.bridge.QQEnv;
import com.cathelperawa.owo.app.core.HookApi;
import com.cathelperawa.owo.app.core.HookLog;
import com.cathelperawa.owo.app.core.dex.ClassResolver;
import com.cathelperawa.owo.app.util.Refl;

import java.lang.reflect.Method;
import java.util.ArrayList;

import de.robv.android.xposed.XposedHelpers;

public final class GroupManageApi {

    private GroupManageApi() {}

    public static boolean isOwnerOrAdmin(String groupUin) {
        try {
            Object app = QQEnv.appRuntime();
            if (app == null) return false;
            Class<?> iface = ClassResolver.find(QQEnv.classLoader(), "troop.infoService",
                    "com.tencent.mobileqq.troop.api.ITroopInfoService");
            if (iface == null) return false;
            Method getter = app.getClass().getMethod(
                    "getRuntimeService", Class.class, String.class);
            getter.setAccessible(true);
            Object svc = getter.invoke(app, iface, "all");
            if (svc == null) return false;
            Object info = Refl.call(svc, "getTroopInfo", groupUin);
            if (info == null) return false;
            return Boolean.TRUE.equals(Refl.call(info, "isOwnerOrAdmin"));
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean modifyCard(String group, String uid, String card) {
        try {
            Object svc = groupService();
            if (svc == null) return false;
            long code = parseLong(group);
            Object cb = callback("com.tencent.qqnt.kernel.nativeinterface.IOperateCallback");
            for (Method m : svc.getClass().getMethods()) {
                if (!"modifyMemberCardName".equals(m.getName())) continue;
                if (m.getParameterTypes().length != 4) continue;
                m.setAccessible(true);
                m.invoke(svc, code, uid, card == null ? "" : card, cb);
                HookLog.i("改名片: " + group + " " + uid + " -> " + card);
                return true;
            }
            HookLog.w("改名片: 服务上没有 modifyMemberCardName");
        } catch (Throwable t) {
            HookLog.e("改名片失败", t);
        }
        return false;
    }

    public static boolean mute(String group, String uid, int seconds) {
        try {
            Object svc = groupService();
            if (svc == null) return false;
            Class<?> infoCls = HookApi.findClass(QQEnv.classLoader(),
                    "com.tencent.qqnt.kernel.nativeinterface.GroupMemberShutUpInfo");
            if (infoCls == null) return false;

            Object info = XposedHelpers.newInstance(infoCls);
            XposedHelpers.setObjectField(info, "uid", uid);
            XposedHelpers.setIntField(info, "timeStamp", seconds);

            ArrayList<Object> list = new ArrayList<Object>();
            list.add(info);

            long code = parseLong(group);
            Object cb = callback("com.tencent.qqnt.kernel.nativeinterface.IOperateCallback");
            for (Method m : svc.getClass().getMethods()) {
                if (!"setMemberShutUp".equals(m.getName())) continue;
                if (m.getParameterTypes().length != 3) continue;
                m.setAccessible(true);
                m.invoke(svc, code, list, cb);
                HookLog.i("禁言: " + group + " " + uid + " " + seconds + "s");
                return true;
            }
            HookLog.w("禁言: 服务上没有 setMemberShutUp");
        } catch (Throwable t) {
            HookLog.e("禁言失败", t);
        }
        return false;
    }

    public static boolean unmute(String group, String uid) {
        return mute(group, uid, 0);
    }

    public static boolean kick(String group, String uid) {
        try {
            Object svc = groupService();
            if (svc == null) return false;
            Class<?> reqCls = HookApi.findClass(QQEnv.classLoader(),
                    "com.tencent.qqnt.kernel.nativeinterface.KickMemberReq");
            if (reqCls == null) return false;

            Object req = XposedHelpers.newInstance(reqCls);
            XposedHelpers.setLongField(req, "groupCode", parseLong(group));
            ArrayList<Object> uids = new ArrayList<Object>();
            uids.add(uid);
            XposedHelpers.setObjectField(req, "kickListUids", uids);

            Object cb = callback("com.tencent.qqnt.kernel.nativeinterface.IKickMemberCallback");
            for (Method m : svc.getClass().getMethods()) {
                if (!"kickMemberV2".equals(m.getName())) continue;
                if (m.getParameterTypes().length != 2) continue;
                m.setAccessible(true);
                m.invoke(svc, req, cb);
                HookLog.i("踢人: " + group + " " + uid);
                return true;
            }
            HookLog.w("踢人: 服务上没有 kickMemberV2");
        } catch (Throwable t) {
            HookLog.e("踢人失败", t);
        }
        return false;
    }

    public static Object service() {
        return groupService();
    }

    private static Object groupService() {
        try {
            Object kernel = QQEnv.kernelService();
            if (kernel == null) return null;
            Object impl = Refl.call(kernel, "getGroupService");
            if (impl == null) return null;
            Object nativeSvc = Refl.call(impl, "getService");
            return nativeSvc != null ? nativeSvc : impl;
        } catch (Throwable t) {
            return null;
        }
    }

    private static Object callback(String ifaceName) {
        try {
            ClassLoader cl = QQEnv.classLoader();
            Class<?> iface = HookApi.findClass(cl, ifaceName);
            if (iface == null) return null;
            return java.lang.reflect.Proxy.newProxyInstance(cl, new Class<?>[]{iface},
                    new java.lang.reflect.InvocationHandler() {
                        @Override
                        public Object invoke(Object proxy, Method method, Object[] args) {
                            try {
                                if (args != null && args.length > 0) {
                                    HookLog.i("群操作回调 code="
                                            + Refl.fieldInt(args[0], "resultCode",
                                                    Refl.fieldInt(args[0], "errorCode", -1)));
                                }
                            } catch (Throwable ignore) {
                            }
                            return null;
                        }
                    });
        } catch (Throwable t) {
            return null;
        }
    }

    private static long parseLong(String s) {
        try {
            return Long.parseLong(s);
        } catch (Throwable t) {
            return 0;
        }
    }
}
