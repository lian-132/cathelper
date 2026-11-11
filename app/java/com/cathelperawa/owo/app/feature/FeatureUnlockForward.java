package com.cathelperawa.owo.app.feature;

import com.cathelperawa.owo.app.core.LoadPriority;
import com.cathelperawa.owo.app.core.SelfCheckResult;
import android.content.Context;
import com.cathelperawa.owo.app.MsgStore;
import com.cathelperawa.owo.app.core.HookApi;
import com.cathelperawa.owo.app.core.HookConfig;
import com.cathelperawa.owo.app.core.HookItem;
import com.cathelperawa.owo.app.core.HookItemBase;
import com.cathelperawa.owo.app.core.HookLog;
import com.cathelperawa.owo.app.util.Refl;
import de.robv.android.xposed.XC_MethodHook;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Map;

@HookItem(description = "解除转发时的人数上限", name = "去除转发限制", order = 60, path = "去除限制/转发",
        priority = LoadPriority.DEFERRED)

public class FeatureUnlockForward extends HookItemBase {

    @Override
    protected void check(SelfCheckResult.Checker c) {
        c.cls("好友转发", CLS_FRIEND);
        c.cls("最近转发", CLS_RECENT);
        c.cls("群聊转发", CLS_TROOP);
    }

    private static final String CLS_FRIEND = "com.tencent.mobileqq.activity.ForwardFriendListActivity";
    private static final String CLS_RECENT = "com.tencent.mobileqq.activity.ForwardRecentActivity";
    private static final String CLS_TROOP = "com.tencent.mobileqq.activity.ForwardTroopListFragment";
    private static final String F_MAP = "mForwardTargetMap";
    public static final String K = "unlock_forward";

    private boolean hookByName(ClassLoader classLoader, String str) {
        Class<?> findClass = HookApi.findClass(classLoader, str);
        if (findClass == null) {
            HookLog.w("转发限制：未找到 " + str);
            MsgStore.add("[转发限制] 未找到 " + shortName(str));
            return false;
        }
        int i = 0;
        for (Constructor<?> constructor : findClass.getDeclaredConstructors()) {
            hookAfter(constructor, new HookApi.Action() { 
                @Override 
                public void call(XC_MethodHook.MethodHookParam methodHookParam) {
                    if (FeatureUnlockForward.this.on()) {
                        try {
                            Refl.setField(methodHookParam.thisObject, FeatureUnlockForward.F_MAP, new FakeMap());
                        } catch (Throwable th) {
                            HookLog.e("转发限制：注入失败(按名)", th);
                        }
                    }
                }
            });
            i++;
        }
        HookLog.i("转发限制：" + shortName(str) + " 已挂 " + i + " 个构造函数(按名)");
        MsgStore.add("[转发限制] " + shortName(str) + " 按名挂 " + i);
        return i > 0;
    }

    private boolean hookByType(ClassLoader classLoader, String str) {
        Class<?> findClass = HookApi.findClass(classLoader, str);
        if (findClass == null) {
            HookLog.w("转发限制：未找到 " + str);
            MsgStore.add("[转发限制] 未找到 " + shortName(str));
            return false;
        }
        int i = 0;
        for (Constructor<?> constructor : findClass.getDeclaredConstructors()) {
            hookAfter(constructor, new HookApi.Action() { 
                @Override 
                public void call(XC_MethodHook.MethodHookParam methodHookParam) {
                    Field findFieldByType;
                    if (FeatureUnlockForward.this.on()) {
                        try {
                            if (Refl.setField(methodHookParam.thisObject, FeatureUnlockForward.F_MAP, new FakeMap()) || (findFieldByType = Refl.findFieldByType(methodHookParam.thisObject, Map.class)) == null) {
                                return;
                            }
                            findFieldByType.set(methodHookParam.thisObject, new FakeMap());
                        } catch (Throwable th) {
                            HookLog.e("转发限制：注入失败(按类型)", th);
                        }
                    }
                }
            });
            i++;
        }
        HookLog.i("转发限制：" + shortName(str) + " 已挂 " + i + " 个构造函数(按类型)");
        MsgStore.add("[转发限制] " + shortName(str) + " 按类型挂 " + i);
        return i > 0;
    }

    public boolean on() {
        return this.ctx != null && on(this.ctx);
    }

    public static boolean on(Context context) {
        return HookConfig.getBoolean(context, K, false);
    }

    private static String shortName(String str) {
        if (str == null) {
            return "?";
        }
        int lastIndexOf = str.lastIndexOf(46);
        return lastIndexOf < 0 ? str : str.substring(lastIndexOf + 1);
    }

    @Override 
    public void entry(ClassLoader classLoader) {
        hint("无独立事件（只解除限制）");
        noInstrument();          
        int i = (hookByName(classLoader, CLS_RECENT) ? 1 : 0) + 0 + (hookByType(classLoader, CLS_FRIEND) ? 1 : 0) + (hookByType(classLoader, CLS_TROOP) ? 1 : 0);
        HookLog.i("转发限制：安装完成 " + i + "/3");
        MsgStore.add("[转发限制] 安装完成 " + i + "/3");
    }

    @Override 
    public String switchKey() {
        return K;
    }
}
