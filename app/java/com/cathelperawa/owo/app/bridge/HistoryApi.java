package com.cathelperawa.owo.app.bridge;

import android.content.Context;
import com.cathelperawa.owo.app.core.HookApi;
import com.cathelperawa.owo.app.core.HookLog;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Method;

public final class HistoryApi {
    private static final String[] MSG_SERVICE_CLASSES = {"com.tencent.qqnt.kernel.api.impl.MsgService", "com.tencent.qqnt.kernel.api.IMsgService", "com.tencent.qqnt.kernel.api.impl.BaseMsgService"};

    private HistoryApi() {
    }

    public static void probe() {
        StringBuilder sb = new StringBuilder();
        ClassLoader classLoader = QQEnv.classLoader();
        for (String str : MSG_SERVICE_CLASSES) {
            Class<?> findClass = HookApi.findClass(classLoader, str);
            sb.append("== ").append(str).append(" → ").append(findClass != null).append('\n');
            if (findClass != null) {
                Method[] methods = findClass.getMethods();
                for (Method method : methods) {
                    String name = method.getName();
                    String lowerCase = name.toLowerCase();
                    if (lowerCase.contains("msg") || lowerCase.contains("history") || lowerCase.contains("query") || lowerCase.contains("list") || lowerCase.contains("record") || lowerCase.contains("search")) {
                        StringBuilder sb2 = new StringBuilder();
                        for (Class<?> cls : method.getParameterTypes()) {
                            sb2.append(cls.getSimpleName()).append(',');
                        }
                        sb.append("   ").append(name).append('(').append((CharSequence) sb2).append(") : ").append(method.getReturnType().getSimpleName()).append('\n');
                    }
                }
            }
        }
        sb.append("\n── 小程序签名 ──\n");
        String[] strArr = {"com.tencent.mobileqq.mini.reuse.MiniAppCmdUtil", "NS_MINI_SHARE.MiniProgramShare$StAdaptShareInfoReq", "NS_MINI_SHARE.MiniProgramShare$StAdaptShareInfoRsp", "com.tencent.mobileqq.mini.share.MiniProgramShareUtils"};
        for (int i = 0; i < 4; i++) {
            String str2 = strArr[i];
            Class<?> findClass2 = HookApi.findClass(classLoader, str2);
            sb.append(str2).append(" → ").append(findClass2 != null).append('\n');
            if (findClass2 != null && str2.endsWith("MiniAppCmdUtil")) {
                for (Method method2 : findClass2.getDeclaredMethods()) {
                    if ("getShareInfo".equals(method2.getName())) {
                        StringBuilder sb3 = new StringBuilder();
                        for (Class<?> cls2 : method2.getParameterTypes()) {
                            sb3.append(cls2.getSimpleName()).append(',');
                        }
                        sb.append("   getShareInfo(").append((CharSequence) sb3).append(")\n");
                    }
                }
            }
        }
        try {
            Object kernelMsgService = QQEnv.kernelMsgService();
            sb.append("\nkernelMsgService() = ").append(kernelMsgService == null ? "null" : kernelMsgService.getClass().getName()).append('\n');
        } catch (Throwable th) {
            sb.append("\nkernelMsgService() 异常 ").append(th).append('\n');
        }
        try {
            Context appContext = QQEnv.appContext();
            if (appContext != null) {
                FileOutputStream fileOutputStream = new FileOutputStream(new File(appContext.getFilesDir(), "history_probe.txt"), false);
                fileOutputStream.write(sb.toString().getBytes("UTF-8"));
                fileOutputStream.close();
            }
        } catch (Throwable th2) {
        }
        HookLog.i("历史消息探测已落盘");
    }
}
