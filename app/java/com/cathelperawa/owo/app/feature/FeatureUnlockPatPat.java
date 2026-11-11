package com.cathelperawa.owo.app.feature;

import com.cathelperawa.owo.app.core.LoadPriority;
import android.content.Context;
import com.cathelperawa.owo.app.MsgStore;
import com.cathelperawa.owo.app.core.HookApi;
import com.cathelperawa.owo.app.core.HookConfig;
import com.cathelperawa.owo.app.core.HookItem;
import com.cathelperawa.owo.app.core.HookItemBase;
import com.cathelperawa.owo.app.core.HookLog;
import de.robv.android.xposed.XC_MethodHook;
import java.lang.reflect.Method;

@HookItem(description = "解除拍一拍 10 秒冷却，可以一直拍", name = "去除拍一拍限制", order = 61, path = "去除限制/拍一拍",
        priority = LoadPriority.DEFERRED)

public class FeatureUnlockPatPat extends HookItemBase {
    private static final String CLS_HANDLER = "com.tencent.mobileqq.paiyipai.PaiYiPaiHandler";
    public static final String K = "unlock_patpat";

    public boolean on() {
        return this.ctx != null && on(this.ctx);
    }

    public static boolean on(Context context) {
        return HookConfig.getBoolean(context, K, false);
    }

    @Override 
    public void entry(ClassLoader classLoader) {

        noInstrument();
        Class<?> findClass = HookApi.findClass(classLoader, CLS_HANDLER);
        if (findClass == null) {
            fail("拍一拍：未找到 com.tencent.mobileqq.paiyipai.PaiYiPaiHandler");
            MsgStore.add("[拍一拍] 未找到 com.tencent.mobileqq.paiyipai.PaiYiPaiHandler");
            return;
        }
        Method findMethodByParams = HookApi.findMethodByParams(findClass, Boolean.TYPE, String.class);
        if (findMethodByParams == null) {
            fail("拍一拍：未找到限次检查方法 (String)->boolean");
            MsgStore.add("[拍一拍] 未找到限次检查方法");
        } else {
            String name = findMethodByParams.getName();
            hookAfter(findMethodByParams, new HookApi.Action() { 
                @Override 
                public void call(XC_MethodHook.MethodHookParam methodHookParam) {
                    
                    try {
                        if (com.cathelperawa.owo.app.script.PatchPoint.has("host.pat")) {
                            String target = "";
                            if (methodHookParam.args != null && methodHookParam.args.length > 0
                                    && methodHookParam.args[0] instanceof String) {
                                target = (String) methodHookParam.args[0];
                            }
                            com.cathelperawa.owo.app.script.PatchPoint.emitAsync(
                                    "host.pat", target, target);
                        }
                    } catch (Throwable ignore) {
                    }
                    try {
                        if (FeatureUnlockPatPat.this.on()) {
                            Object result = methodHookParam.getResult();
                            if (!(result instanceof Boolean) || ((Boolean) result).booleanValue()) {
                                return;
                            }
                            methodHookParam.setResult(Boolean.TRUE);
                        }
                    } catch (Throwable th) {
                        HookLog.e("拍一拍 hook 异常", th);
                    }
                }
            });
            HookLog.i("拍一拍：已挂限次检查 " + name);
            MsgStore.add("[拍一拍] 已挂限次检查 " + name);
        }
    }

    @Override
    protected void check(com.cathelperawa.owo.app.core.SelfCheckResult.Checker c) {
        Class<?> h = c.cls("PaiYiPaiHandler", CLS_HANDLER);
        if (h != null) {
            
            boolean found = false;
            for (java.lang.reflect.Method m : h.getDeclaredMethods()) {
                if (m.getReturnType() == Boolean.TYPE
                        && m.getParameterTypes().length == 1
                        && m.getParameterTypes()[0] == String.class) {
                    found = true;
                    c.pass("限次检查方法", m.getName());
                    break;
                }
            }
            if (!found) c.fail("限次检查方法", "找不到 (String)->boolean");
        }
    }

    @Override 
    public String switchKey() {
        return K;
    }
}
