package com.cathelperawa.owo.app.feature;

import com.cathelperawa.owo.app.core.HookApi;
import com.cathelperawa.owo.app.core.HookItem;
import com.cathelperawa.owo.app.core.HookItemBase;
import com.cathelperawa.owo.app.core.HookLog;
import com.cathelperawa.owo.app.core.SelfCheckResult;
import com.cathelperawa.owo.app.util.Refl;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodHook;

@HookItem(name = "允许查看异常资料卡", path = "去除限制/资料卡", order = 64,
        description = "忽略账号的异常状态（201/202），正常查看资料卡", defaultEnabled = false)
public class FeatureViewCard extends HookItemBase {

    public static final String K = "u_card";

    private static final int CODE_ABNORMAL_1 = 201;
    private static final int CODE_ABNORMAL_2 = 202;

    private static final String CLS_IMPL =
            "com.tencent.mobileqq.profilecard.api.impl.ProfileDataServiceImpl";
    private static final String CLS_PROC =
            "com.tencent.mobileqq.profilecard.processor.ProfileSecureProcessor";

    @Override
    public String switchKey() {
        return K;
    }

    private boolean on() {
        try {
            return ctx != null
                    && com.cathelperawa.owo.app.core.HookConfig.getBoolean(ctx, K, false);
        } catch (Throwable t) {
            return false;
        }
    }

    @Override
    public void entry(ClassLoader cl) {
        hint("对方账号异常时打开资料卡触发");

        int cardHooks = hookDataLayer(cl);
        int procHooks = hookProcessorLayer(cl);

        HookLog.i("异常资料卡：数据层 " + cardHooks + " 处 / 处理器层 " + procHooks + " 处");
        if (cardHooks == 0 && procHooks == 0) {
            fail("ProfileDataServiceImpl / ProfileSecureProcessor 都没挂上");
            return;
        }
        if (cardHooks == 0) {
            
            HookLog.w("异常资料卡：数据层未挂上，仅靠处理器层");
        }
    }

    

    private int hookDataLayer(ClassLoader cl) {
        Class<?> impl = HookApi.findClass(cl, CLS_IMPL);
        if (impl == null) {
            HookLog.w("异常资料卡：未找到 " + CLS_IMPL);
            return 0;
        }
        int n = 0;
        for (Method m : impl.getDeclaredMethods()) {
            String name = m.getName();
            if (!"getProfileCard".equals(name) && !"getProfileCardFromCache".equals(name)) continue;
            if (!isCardType(m.getReturnType())) continue;      
            try {
                m.setAccessible(true);
            } catch (Throwable ignore) {
            }
            if (hookAfter(m, new HookApi.Action() {
                @Override public void call(XC_MethodHook.MethodHookParam p) {
                    try {
                        clearCard(p.getResult());
                    } catch (Throwable t) {
                        noteRuntimeError(t);
                    }
                }
            }) != null) {
                n++;
                HookLog.i("异常资料卡：已挂 " + name + " → 返回 " + m.getReturnType().getName());
            }
        }
        return n;
    }

    private static int intOf(Object o, String field, int def) {
        try {
            Object v = Refl.field(o, field);
            return v instanceof Number ? ((Number) v).intValue() : def;
        } catch (Throwable t) {
            return def;
        }
    }

    private static boolean isCardType(Class<?> t) {
        if (t == null) return false;
        String sn = t.getSimpleName();
        if ("Card".equals(sn)) return true;
        String n = t.getName();
        return n.endsWith(".data.Card") || n.endsWith("$Card");
    }

    private void clearCard(Object card) {
        if (card == null || !on()) return;
        int code = intOf(card, "forbidCode", -1);
        if (code != CODE_ABNORMAL_1 && code != CODE_ABNORMAL_2) return;
        boolean ok1 = Refl.setField(card, "isForbidAccount", Boolean.FALSE);
        boolean ok2 = Refl.setField(card, "forbidCode", Integer.valueOf(0));
        fire("clearCard");
        HookLog.i("异常资料卡：放行 forbidCode=" + code
                + "（isForbidAccount=" + ok1 + " forbidCode=" + ok2 + "）");
    }

    

    private int hookProcessorLayer(ClassLoader cl) {
        Class<?> proc = HookApi.findClass(cl, CLS_PROC);
        if (proc == null) {
            HookLog.w("异常资料卡：未找到 " + CLS_PROC);
            return 0;
        }
        int n = 0;
        for (Method m : proc.getDeclaredMethods()) {
            if (!"processProfileCard".equals(m.getName())) continue;
            try {
                m.setAccessible(true);
            } catch (Throwable ignore) {
            }
            if (hookBefore(m, new HookApi.Action() {
                @Override public void call(XC_MethodHook.MethodHookParam p) {
                    try {
                        clearRespHead(p.args);
                    } catch (Throwable t) {
                        noteRuntimeError(t);
                    }
                }
            }) != null) {
                n++;
                HookLog.i("异常资料卡：已挂 processProfileCard（" + m.getParameterTypes().length + " 参）");
            }
        }
        return n;
    }

    private void clearRespHead(Object[] args) {
        if (args == null || !on()) return;
        for (Object a : args) {
            if (a == null) continue;
            Field f = Refl.findField(a.getClass(), "iResult");
            if (f == null) continue;
            int code = intOf(a, "iResult", -1);
            if (code != CODE_ABNORMAL_1 && code != CODE_ABNORMAL_2) continue;
            if (Refl.setField(a, "iResult", Integer.valueOf(0))) {
                fire("clearHead");
                HookLog.i("异常资料卡：RespHead.iResult " + code + " → 0");
            }
            return;
        }
    }

    @Override
    protected void check(SelfCheckResult.Checker c) {
        Class<?> impl = HookApi.findClass(cl, CLS_IMPL);
        if (impl == null) {
            c.fail("ProfileDataServiceImpl", "类未找到");
        } else {
            int n = 0;
            for (Method m : impl.getDeclaredMethods()) {
                if (!"getProfileCard".equals(m.getName())
                        && !"getProfileCardFromCache".equals(m.getName())) continue;
                if (isCardType(m.getReturnType())) n++;
            }
            if (n > 0) c.pass("getProfileCard", "找到 " + n + " 个返回 Card 的方法");
            else c.fail("getProfileCard", "没有返回 Card 的方法（宿主改版？）");
        }
        Class<?> proc = HookApi.findClass(cl, CLS_PROC);
        if (proc == null) {
            c.fail("ProfileSecureProcessor", "类未找到");
        } else {
            int n = 0;
            for (Method m : proc.getDeclaredMethods()) {
                if ("processProfileCard".equals(m.getName())) n++;
            }
            if (n > 0) c.pass("processProfileCard", "找到 " + n + " 个重载");
            else c.fail("processProfileCard", "方法未找到");
        }
    }
}
