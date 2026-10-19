package com.cathelperawa.owo.app.feature;

import android.content.Context;

import com.cathelperawa.owo.app.AppConfig;
import com.cathelperawa.owo.app.CatProcessor;
import com.cathelperawa.owo.app.MsgStore;
import com.cathelperawa.owo.app.core.HookApi;
import com.cathelperawa.owo.app.core.HookItem;
import com.cathelperawa.owo.app.core.HookItemBase;
import com.cathelperawa.owo.app.core.HookLog;
import com.cathelperawa.owo.app.util.Refl;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

@HookItem(name = "猫娘化", path = "聊天与消息/猫娘化", description = "发送的文本自动猫娘化", order = 10)
public class FeatureCat extends HookItemBase {

    private Context hostCtx;
    private long lastCfgLoad = 0;

    @Override
    public String switchKey() { return "cat_total"; }

    @Override
    public void entry(ClassLoader cl) {
        hint("发送消息时");
        hostCtx = ctx;
        Class<?> msgService = HookApi.findClass(cl,
                "com.tencent.qqnt.kernel.api.impl.MsgService");
        if (msgService == null) {
            fail("未找到 MsgService，猫娘化不可用");
            return;
        }

        int hooked = 0;
        for (final Method m : msgService.getDeclaredMethods()) {
            if (!"sendMsg".equals(m.getName())) continue;
            boolean hasList = false;
            for (Class<?> p : m.getParameterTypes()) {
                if (List.class.isAssignableFrom(p)) { hasList = true; break; }
            }
            if (!hasList) continue;
            hookBefore(m, new com.cathelperawa.owo.app.core.HookApi.Action() {
                @Override
                public void call(de.robv.android.xposed.XC_MethodHook.MethodHookParam p) {
                    rewrite(p.args, contactUid(p.args));
                }
            });
            hooked++;
            HookLog.i("猫娘化已挂: " + m);
        }
        if (hooked == 0) {
            fail("未找到 sendMsg(List) 方法");
        } else {
            HookLog.i("猫娘化共挂 " + hooked + " 个 sendMsg 重载");
        }
    }

    @Override
    protected void check(com.cathelperawa.owo.app.core.SelfCheckResult.Checker c) {
        Class<?> svc = c.cls("MsgService", "com.tencent.qqnt.kernel.api.impl.MsgService");
        if (svc != null) {
            boolean hasList = false;
            for (java.lang.reflect.Method m : svc.getDeclaredMethods()) {
                if (!"sendMsg".equals(m.getName())) continue;
                for (Class<?> p : m.getParameterTypes()) {
                    if (java.util.List.class.isAssignableFrom(p)) { hasList = true; break; }
                }
                if (hasList) break;
            }
            c.assertThat("sendMsg(List) 方法", hasList,
                    hasList ? "存在" : "找不到带 List 参数的 sendMsg");
        }
    }

    private String contactUid(Object[] args) {
        if (args != null && args.length > 1 && args[1] != null) {
            Object uid = Refl.field(args[1], "peerUid");
            if (uid instanceof String) return (String) uid;
        }
        return null;
    }

    private void rewrite(Object[] args, String peerUid) {
        if (args == null) return;

        if (AppConfig.current == null
                || System.currentTimeMillis() - lastCfgLoad > 2000) {
            lastCfgLoad = System.currentTimeMillis();
            if (hostCtx != null) AppConfig.current = AppConfig.loadHost(hostCtx);
        }
        HookLog.i("猫娘化检查: total=" + (AppConfig.current != null && AppConfig.current.total)
                + " peer=" + peerUid + " allow=" + MsgStore.allows(peerUid));
        if (AppConfig.current == null || !AppConfig.current.total) return;
        if (peerUid != null && !MsgStore.allows(peerUid)) return;

        fire("改写消息");

        for (Object arg : args) {
            if (!(arg instanceof List)) continue;
            List<?> list = (List<?>) arg;
            if (list.isEmpty()) continue;

            for (Object el : list) {
                if (el == null) continue;
                String cls = el.getClass().getName();
                if (!cls.contains("Element")) continue;
                if (Refl.field(el, "textElement") == null) continue;

                String old = textContent(el);
                if (old == null || old.length() == 0) return;
                if (old.contains("喵")) return;

                String out = CatProcessor.process(old, AppConfig.current);
                if (out != null && !out.equals(old)) {
                    setTextContent(el, out);
                    HookLog.d("猫娘化: " + brief(old) + " -> " + brief(out));
                }
                return;
            }
            return;
        }
    }

    private static String textContent(Object el) {
        Object te = Refl.field(el, "textElement");
        if (te == null) return null;
        Object v = Refl.field(te, "content");
        return v instanceof String ? (String) v : null;
    }

    private static void setTextContent(Object el, String s) {
        Object te = Refl.field(el, "textElement");
        if (te == null) return;
        try {
            Field f = te.getClass().getDeclaredField("content");
            f.setAccessible(true);
            f.set(te, s);
        } catch (Throwable ignore) {
        }
    }

    private static String brief(String s) {
        if (s == null) return "";
        return s.length() > 40 ? s.substring(0, 40) + "…" : s;
    }
}
