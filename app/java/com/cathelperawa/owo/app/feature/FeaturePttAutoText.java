package com.cathelperawa.owo.app.feature;

import android.content.Context;
import android.content.SharedPreferences;

import com.cathelperawa.owo.app.MsgStore;
import com.cathelperawa.owo.app.core.HookApi;
import com.cathelperawa.owo.app.core.HookConfig;
import com.cathelperawa.owo.app.core.HookItem;
import com.cathelperawa.owo.app.core.HookItemBase;
import com.cathelperawa.owo.app.core.HookLog;
import com.cathelperawa.owo.app.core.LoadPriority;

import de.robv.android.xposed.XC_MethodHook;

import java.lang.reflect.Method;

@HookItem(description = "打开 TIM 的「语音消息自动转文字」，AI 才能听懂语音",
        name = "语音自动转文字", order = 70, path = "去除限制/语音转文字",
        priority = LoadPriority.DEFERRED)
public class FeaturePttAutoText extends HookItemBase {

    public static final String K = "u_ptt_auto_text";

    private static final String SP_NAME = "check_update_sp_key";
    private static final String KEY_PREFIX = "businessinfo_ptt_auto_change_text_";
    private static final String CLS_API = "com.tencent.qqnt.aio.adapter.api.impl.AIOPttApiImpl";

    public static boolean on(Context context) {
        if (context == null) return false;
        return HookConfig.getBoolean(context, K, false);
    }

    public static void apply(Context ctx, boolean enable) {
        try {
            String uin = com.cathelperawa.owo.app.bridge.QQEnv.currentUin();
            setSwitch(ctx, uin, enable);
        } catch (Throwable t) {
            HookLog.e("语音自动转文字：apply 失败", t);
        }
    }

    public static boolean setSwitch(Context ctx, String uin, boolean enable) {
        if (ctx == null || uin == null || uin.length() == 0) return false;
        try {
            SharedPreferences sp = ctx.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
            sp.edit().putBoolean(KEY_PREFIX + uin, enable).apply();
            HookLog.i("语音自动转文字：" + (enable ? "已打开 " : "已关闭 ") + KEY_PREFIX + uin);
            return true;
        } catch (Throwable t) {
            HookLog.e("语音自动转文字：写配置失败", t);
            return false;
        }
    }

    public static boolean writeSwitch(Context ctx, String uin) {
        if (ctx == null || uin == null || uin.length() == 0) return false;
        try {
            SharedPreferences sp = ctx.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
            boolean cur = sp.getBoolean(KEY_PREFIX + uin, false);
            if (!cur) {
                sp.edit().putBoolean(KEY_PREFIX + uin, true).apply();
                HookLog.i("语音自动转文字：已写入 " + KEY_PREFIX + uin);
            }
            return true;
        } catch (Throwable t) {
            HookLog.e("语音自动转文字：写配置失败", t);
            return false;
        }
    }

    public static boolean readSwitch(Context ctx, String uin) {
        if (ctx == null || uin == null || uin.length() == 0) return false;
        try {
            return ctx.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)
                    .getBoolean(KEY_PREFIX + uin, false);
        } catch (Throwable t) {
            return false;
        }
    }

    @Override
    public void entry(ClassLoader classLoader) {
        noInstrument();
        
        try {
            if (on(this.ctx)) {
                writeSwitch(this.ctx, com.cathelperawa.owo.app.bridge.QQEnv.currentUin());
            }
        } catch (Throwable ignore) {
        }
        Class<?> api = HookApi.findClass(classLoader, CLS_API);
        if (api == null) {
            fail("语音转文字：未找到 " + CLS_API);
            MsgStore.add("[语音转文字] 未找到 AIOPttApiImpl");
            return;
        }
        Method target = null;
        for (Method m : api.getDeclaredMethods()) {
            if (m.getName().equals("isAutoChangeText")
                    && m.getReturnType() == Boolean.TYPE) {
                target = m;
                break;
            }
        }
        if (target == null) {
            fail("语音转文字：未找到 isAutoChangeText");
            MsgStore.add("[语音转文字] 未找到 isAutoChangeText");
            return;
        }
        hookAfter(target, new HookApi.Action() {
            @Override
            public void call(XC_MethodHook.MethodHookParam p) {
                try {
                    if (!on(FeaturePttAutoText.this.ctx)) return;
                    Object r = p.getResult();
                    if (r instanceof Boolean && !((Boolean) r)) {
                        p.setResult(Boolean.TRUE);
                    }
                } catch (Throwable t) {
                    HookLog.e("语音转文字 hook 异常", t);
                }
            }
        });
        HookLog.i("语音转文字：已挂 isAutoChangeText");
        MsgStore.add("[语音转文字] 已挂 isAutoChangeText");
        probeSilkParams(classLoader);
    }

    private void probeSilkParams(ClassLoader cl) {
        try {
            Class<?> c = HookApi.findClass(cl, "com.tencent.mobileqq.utils.SilkCodecWrapper");
            if (c == null) {
                com.cathelperawa.owo.app.ai.AiDebug.log("  · SILK-PROBE: 没找到 SilkCodecWrapper");
                return;
            }
            int n = 0;
            for (final Method m : c.getDeclaredMethods()) {
                if (!"b".equals(m.getName())) continue;
                if (m.getParameterTypes().length != 3) continue;
                hookBefore(m, new HookApi.Action() {
                    @Override public void call(XC_MethodHook.MethodHookParam p) {
                        try {
                            Object self = p.thisObject;
                            Object r = com.cathelperawa.owo.app.util.Refl.field(self, "r");
                            com.cathelperawa.owo.app.ai.AiDebug.log("  · SILK-PROBE: b("
                                    + p.args[0] + "," + p.args[1] + "," + p.args[2]
                                    + ") 编码模式=" + r);
                        } catch (Throwable ignore) {
                        }
                    }
                });
                n++;
            }
            com.cathelperawa.owo.app.ai.AiDebug.log("  · SILK-PROBE: 已挂 " + n + " 个 b() 重载");
        } catch (Throwable t) {
            com.cathelperawa.owo.app.ai.AiDebug.log("  · SILK-PROBE 异常 " + t);
        }
    }

    @Override
    protected void check(com.cathelperawa.owo.app.core.SelfCheckResult.Checker c) {
        Class<?> api = c.cls("AIOPttApiImpl", CLS_API);
        if (api != null) c.pass("AIOPttApiImpl", "类存在");
    }
}
