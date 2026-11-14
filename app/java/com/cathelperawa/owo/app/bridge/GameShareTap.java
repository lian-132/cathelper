package com.cathelperawa.owo.app.bridge;

import android.os.Bundle;
import com.cathelperawa.owo.app.core.HookApi;
import com.cathelperawa.owo.app.core.HookLog;
import com.cathelperawa.owo.app.core.Host;
import com.cathelperawa.owo.app.proto.MediaVault;
import de.robv.android.xposed.XC_MethodHook;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import org.json.JSONObject;

public final class GameShareTap {
    private static volatile boolean installed = false;
    private static final String[] CANDIDATES = {"com.tencent.mobileqq.mini.reuse.MiniAppCmdUtil", "com.tencent.mobileqq.mini.share.MiniProgramShareUtils", "com.tencent.mobileqq.mini.share.MiniProgramOpenSdkUtil", "com.tencent.mobileqq.zootopia.service.api.impl.ZootopiaShareArkServiceImpl", "com.tencent.mobileqq.guild.api.impl.GuildShareApiImpl"};

    private GameShareTap() {
    }

    public static synchronized void install(ClassLoader classLoader) {
        synchronized (GameShareTap.class) {
            if (installed) {
                return;
            }
            try {
                int i = 0;
                for (String str : CANDIDATES) {
                    Class<?> findClass = HookApi.findClass(classLoader, str);
                    if (findClass != null) {
                        for (final Method method : findClass.getDeclaredMethods()) {
                            String name = method.getName();
                            if (!name.contains("$")) {
                                String lowerCase = name.toLowerCase();
                                if (lowerCase.contains("share") || lowerCase.contains("sign") || lowerCase.contains(MediaVault.T_ARK) || lowerCase.contains("request") || lowerCase.contains("send")) {
                                    try {
                                        method.setAccessible(true);
                                        HookApi.before(method, new HookApi.Action() {
                                            @Override
                                            public void call(XC_MethodHook.MethodHookParam methodHookParam) {
                                                try {
                                                    GameShareTap.onCall(methodHookParam, method);
                                                } catch (Throwable th) {
                                                }
                                            }
                                        });
                                        i++;
                                    } catch (Throwable th) {
                                    }
                                }
                            }
                        }
                    }
                }
                installed = i > 0;
                note("installed=" + i);
                HookLog.i("GameShareTap: 挂了 " + i + " 个点");
            } catch (Throwable th2) {
                HookLog.w("GameShareTap 安装失败 " + th2);
            }
        }
    }

    public static void onCall(XC_MethodHook.MethodHookParam methodHookParam, Method method) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n=== ").append(method.getDeclaringClass().getSimpleName()).append('#').append(method.getName()).append(" ===\n");
        if (methodHookParam.args != null) {
            for (int i = 0; i < methodHookParam.args.length; i++) {
                Object obj = methodHookParam.args[i];
                if (obj == null) {
                    sb.append("arg").append(i).append("=null\n");
                } else {
                    sb.append("arg").append(i).append(": ").append(obj.getClass().getSimpleName());
                    if (obj instanceof String) {
                        sb.append(" = ").append(obj);
                    }
                    sb.append('\n');
                    if (obj.getClass().getName().contains("StAdaptShareInfo")) {
                        dumpPb(sb, obj);
                    }
                    if (obj instanceof JSONObject) {
                        sb.append("    json = ").append(obj.toString()).append('\n');
                    }
                    if (obj instanceof Bundle) {
                        Bundle bundle = (Bundle) obj;
                        for (String str : bundle.keySet()) {
                            sb.append("    bundle.").append(str).append(" = ").append(String.valueOf(bundle.get(str))).append('\n');
                        }
                    }
                    if (obj instanceof List) {
                        List list = (List) obj;
                        sb.append("    list size=").append(list.size()).append('\n');
                        for (int i2 = 0; i2 < list.size() && i2 < 3; i2++) {
                            Object obj2 = list.get(i2);
                            sb.append("      [").append(i2).append("] ").append(obj2 == null ? "null" : obj2.getClass().getSimpleName()).append('\n');
                        }
                    }
                }
            }
        }
        StackTraceElement[] stackTrace = new Throwable().getStackTrace();
        sb.append("stack:\n");
        for (int i3 = 0; i3 < stackTrace.length && i3 < 12; i3++) {
            sb.append("   ").append(stackTrace[i3].toString()).append('\n');
        }
        note(sb.toString());
    }

    private static void dumpPb(StringBuilder sb, Object obj) {
        Class<?> cls = obj.getClass();
        int i = 0;
        while (cls != null && cls != Object.class) {
            int i2 = i + 1;
            if (i < 4) {
                for (Field field : cls.getDeclaredFields()) {
                    String name = field.getType().getName();
                    if (name.contains("PBStringField") || name.contains("PBIntField") || name.contains("PBBoolField") || name.contains("PBEnumField")) {
                        try {
                            field.setAccessible(true);
                            Object obj2 = field.get(obj);
                            if (obj2 != null) {
                                Method method = obj2.getClass().getMethod("get", new Class[0]);
                                method.setAccessible(true);
                                Object invoke = method.invoke(obj2, new Object[0]);
                                if (invoke != null && String.valueOf(invoke).length() != 0) {
                                    sb.append("    ").append(field.getName()).append(" = ").append(invoke).append('\n');
                                }
                            }
                        } catch (Throwable th) {
                        }
                    }
                }
                cls = cls.getSuperclass();
                i = i2;
            } else {
                return;
            }
        }
    }

    private static void note(String str) {
        try {
            File file = new File("/data/user/0/" + Host.pkg() + "/files/gameshare.txt");
            if (file.length() > 262144) {
                return;
            }
            FileOutputStream fileOutputStream = new FileOutputStream(file, true);
            try {
                fileOutputStream.write(str.getBytes("UTF-8"));
                fileOutputStream.close();
            } catch (Throwable th) {
                fileOutputStream.close();
                throw th;
            }
        } catch (Throwable th2) {
        }
    }
}
