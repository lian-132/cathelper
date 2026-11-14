package com.cathelperawa.owo.app.feature;

import android.content.Context;

import com.cathelperawa.owo.app.MsgStore;
import com.cathelperawa.owo.app.core.Bg;
import com.cathelperawa.owo.app.core.HookApi;
import com.cathelperawa.owo.app.core.HookConfig;
import com.cathelperawa.owo.app.core.HookItem;
import com.cathelperawa.owo.app.core.HookItemBase;
import com.cathelperawa.owo.app.core.HookLog;
import com.cathelperawa.owo.app.core.LoadPriority;

import java.lang.reflect.Method;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;

@HookItem(name = "绕过TIM卡片点击限制",
        path = "去除限制/卡片",
        order = 66,
        description = "解除TIM中部分卡片无法点击的问题",
        defaultEnabled = false,
        priority = LoadPriority.DEFERRED)
public class FeatureBypassArkLimit extends HookItemBase {

    public static final String K = "ark_click_limit";

    private static final String CLS_ARK_KNOWN =
            "com.tencent.mobileqq.aio.msglist.holder.component.ark.d";

    private static final String[] CLS_ARK_CANDIDATES = {
            CLS_ARK_KNOWN,
            "com.tencent.mobileqq.aio.msglist.holder.component.ark.ArkConfigModel"
    };

    private static final String STR_HINT = "updateTimArkClickWhiteList";

    public static boolean on(Context context) {
        return HookConfig.getBoolean(context, K, false);
    }

    private boolean on() {
        return this.ctx != null && on(this.ctx);
    }

    @Override
    public void entry(final ClassLoader classLoader) {

        noInstrument();
        hint("需点击卡片");

        Class<?> cls = HookApi.findClass(classLoader, CLS_ARK_CANDIDATES);
        if (cls != null) {
            install(cls, "已知名");
            return;
        }

        HookLog.i("卡片点击：已知名未命中，后台按字符串定位");
        Bg.run(new Runnable() {
            @Override
            public void run() {
                String located = locateByDex();
                if (located == null) {
                    fail("卡片点击：未找到 ArkConfigModel（TIM 版本可能已变）");
                    MsgStore.add("[卡片点击] 未找到 ArkConfigModel");
                    return;
                }
                Class<?> found = HookApi.findClass(classLoader, located);
                if (found == null) {
                    fail("卡片点击：dex 定位到 " + located + " 但加载失败");
                    return;
                }
                install(found, "dex 字符串定位");
            }
        }, "cat-ark-limit");
    }

    private void install(Class<?> cls, String how) {
        Method check = HookApi.findMethodByParams(cls, Boolean.TYPE, String.class, String.class);
        if (check == null) {
            fail("卡片点击：未找到白名单判定 (String, String) -> boolean");
            MsgStore.add("[卡片点击] 未找到白名单判定方法");
            return;
        }

        hookBefore(check, new HookApi.Action() {
            @Override
            public void call(XC_MethodHook.MethodHookParam p) {
                try {
                    if (on()) {
                        p.setResult(Boolean.TRUE);
                    }
                } catch (Throwable t) {
                    HookLog.e("卡片点击 hook 异常", t);
                }
            }
        });

        HookLog.i("卡片点击：已挂 " + shortName(cls.getName()) + "#" + check.getName()
                + "（" + how + "）");
        MsgStore.add("[卡片点击] 已挂 " + shortName(cls.getName()) + "#" + check.getName());
    }

    private String locateByDex() {
        try {
            com.cathelperawa.owo.app.core.dex.DexIndex ix =
                    com.cathelperawa.owo.app.core.dex.DexIndexLoader.get(this.ctx, true);
            if (ix == null) return null;
            List<com.cathelperawa.owo.app.core.dex.ClassFinder.Result> rs =
                    new com.cathelperawa.owo.app.core.dex.ClassFinder(ix).query()
                            .usingString(STR_HINT)
                            .includeNested()
                            .find();
            for (com.cathelperawa.owo.app.core.dex.ClassFinder.Result r : rs) {
                if (r == null || r.className == null) continue;
                HookLog.i("卡片点击：dex 命中 " + r.className + "（score " + r.score + "）");
                return r.className;
            }
        } catch (Throwable t) {
            HookLog.w("卡片点击：dex 定位失败 " + t);
        }
        return null;
    }

    @Override
    protected void check(com.cathelperawa.owo.app.core.SelfCheckResult.Checker c) {
        Class<?> k = c.cls("ArkConfigModel", CLS_ARK_CANDIDATES);
        if (k == null) return;
        for (Method m : k.getDeclaredMethods()) {
            Class<?>[] ps = m.getParameterTypes();
            if (m.getReturnType() == Boolean.TYPE && ps.length == 2
                    && ps[0] == String.class && ps[1] == String.class) {
                c.pass("白名单判定", m.getName() + "(String, String)");
                return;
            }
        }
        c.fail("白名单判定", "找不到 (String, String) -> boolean");
    }

    private static String shortName(String str) {
        if (str == null) return "?";
        int i = str.lastIndexOf('.');
        return i < 0 ? str : str.substring(i + 1);
    }

    @Override
    public String switchKey() {
        return K;
    }
}
