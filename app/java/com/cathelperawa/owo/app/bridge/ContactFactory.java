package com.cathelperawa.owo.app.bridge;

import com.cathelperawa.owo.app.core.dex.ClassResolver;
import com.cathelperawa.owo.app.core.HookLog;

import java.lang.reflect.Constructor;

import de.robv.android.xposed.XposedHelpers;

public final class ContactFactory {

    private ContactFactory() {}

    public static Object make(int chatType, String peerUid, String guildId) {
        ClassLoader cl = QQEnv.classLoader();
        Class<?> c = ClassResolver.find(cl, "qqnt.contact",
                "com.tencent.qqnt.kernelpublic.nativeinterface.Contact",
                "com.tencent.qqnt.kernel.nativeinterface.Contact");
        if (c == null) { HookLog.w("未找到 Contact 类"); return null; }
        for (Constructor<?> ct : c.getDeclaredConstructors()) {
            Class<?>[] ps = ct.getParameterTypes();
            if (ps.length == 3 && ps[0] == int.class
                    && ps[1] == String.class && ps[2] == String.class) {
                try {
                    ct.setAccessible(true);
                    return ct.newInstance(chatType,
                            peerUid == null ? "" : peerUid,
                            guildId == null ? "" : guildId);
                } catch (Throwable t) { HookLog.e("Contact 构造失败", t); }
            }
        }
        HookLog.w("Contact 无 (int,String,String) 构造器");
        return null;
    }
}
