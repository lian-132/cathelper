package com.cathelperawa.owo.app.feature;

import android.content.Context;

import com.cathelperawa.owo.app.core.HookApi;
import com.cathelperawa.owo.app.core.HookConfig;
import com.cathelperawa.owo.app.core.HookItem;
import com.cathelperawa.owo.app.core.HookItemBase;
import com.cathelperawa.owo.app.core.HookLog;
import com.cathelperawa.owo.app.util.Refl;

import java.lang.reflect.Method;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;

@HookItem(name = "图片外显文字", path = "聊天与消息/图片外显文字",
        description = "发图时自定义外显文字（会话列表 · 通知栏 · 对方预览）", order = 47, hosts = {"tim", "qq"})
public class FeaturePicSummary extends HookItemBase {

    public static final String K_ON = "chat_picsummary";

    public static final String K_TEXT = "picsummary_text";

    public static final String K_FACE = "picsummary_face";

    public static final String K_BUBBLE = "picsummary_bubble";

    private static final String CLS_MSGSERVICE = "com.tencent.qqnt.kernel.api.impl.MsgService";
    private static final String CLS_CPPPROXY =
            "com.tencent.qqnt.kernel.nativeinterface.IKernelMsgService$CppProxy";

    public boolean mainProcessOnly() { return true; }

    @Override

    public String switchKey() { return K_ON; }

    public static boolean on(Context c) {
        return c != null && HookConfig.getBoolean(c, K_ON, false);
    }

    public static String text(Context c) {
        if (c == null) return "";
        String v = HookConfig.get(c, K_TEXT, "");
        return v == null ? "" : v.trim();
    }

    public static boolean faceOn(Context c) {
        return c != null && HookConfig.getBoolean(c, K_FACE, false);
    }

    public static boolean bubbleOn(Context c) {
        return c != null && HookConfig.getBoolean(c, K_BUBBLE, false);
    }

    public static void setText(Context c, String v) {
        HookConfig.set(c, K_TEXT, v == null ? "" : v);
    }

    public static void setFace(Context c, boolean on) {
        HookConfig.set(c, K_FACE, String.valueOf(on));
    }

    public static void setBubble(Context c, boolean on) {
        HookConfig.set(c, K_BUBBLE, String.valueOf(on));
    }

    @Override
    public void entry(ClassLoader cl) {
        hint("发送图片 / 表情时");

        int n = hookClass(cl, CLS_MSGSERVICE);
        n += hookClass(cl, CLS_CPPPROXY);
        if (n == 0) {
            fail("未找到 sendMsg(List) 方法，外显改不了");
            return;
        }
        HookLog.i("图片外显：共挂 " + n + " 个 sendMsg 重载");
    }

    private int hookClass(ClassLoader cl, String clsName) {
        Class<?> cls = HookApi.findClass(cl, clsName);
        if (cls == null) {
            HookLog.i("图片外显：未找到 " + clsName);
            return 0;
        }
        int n = 0;
        for (final Method m : cls.getDeclaredMethods()) {
            if (!"sendMsg".equals(m.getName())) continue;
            boolean hasList = false;
            for (Class<?> p : m.getParameterTypes()) {
                if (List.class.isAssignableFrom(p)) { hasList = true; break; }
            }
            if (!hasList) continue;
            try {
                hookBefore(m, new HookApi.Action() {
                    @Override
                    public void call(XC_MethodHook.MethodHookParam p) {
                        try {
                            apply(p.args);
                        } catch (Throwable t) {
                            HookLog.e("图片外显 hook 异常", t);
                        }
                    }
                });
                n++;
            } catch (Throwable t) {
                HookLog.w("图片外显：挂 " + m + " 失败 " + t);
            }
        }
        return n;
    }

    private void apply(Object[] args) {
        if (args == null) return;
        if (!on(ctx)) return;
        String t = text(ctx);
        if (t.length() == 0) return;

        boolean face = faceOn(ctx);
        boolean bubble = bubbleOn(ctx);
        int hit = 0;
        int failed = 0;

        for (Object a : args) {
            if (!(a instanceof List)) continue;
            for (Object el : (List<?>) a) {
                if (el == null) continue;

                Object pic = Refl.field(el, "picElement");
                if (pic != null) {
                    if (Refl.setField(pic, "summary", t)) hit++;
                    else failed++;
                    continue;
                }
                if (face) {
                    Object mf = Refl.field(el, "marketFaceElement");
                    if (mf != null) {
                        if (Refl.setField(mf, "faceName", t)) hit++;
                        else failed++;
                    }
                }
                if (bubble) {
                    Object fb = Refl.field(el, "faceBubbleElement");
                    if (fb != null) {
                        if (Refl.setField(fb, "content", t)) hit++;
                        else failed++;
                    }
                }
            }
        }

        if (hit > 0) {
            fire("改外显", hit);
            HookLog.d("图片外显：已改 " + hit + " 处 → " + t);
        } else if (failed > 0) {

            if (!warned) {
                warned = true;
                HookLog.w("图片外显：summary 字段写入失败（字段可能是 final / 已改名）");
            }
        }
    }

    private volatile boolean warned = false;

    @Override
    protected void check(com.cathelperawa.owo.app.core.SelfCheckResult.Checker c) {
        boolean ok = false;
        for (String name : new String[]{CLS_MSGSERVICE, CLS_CPPPROXY}) {
            Class<?> k = c.cls(name.substring(name.lastIndexOf('.') + 1), name);
            if (k == null) continue;
            for (Method m : k.getDeclaredMethods()) {
                if (!"sendMsg".equals(m.getName())) continue;
                for (Class<?> p : m.getParameterTypes()) {
                    if (List.class.isAssignableFrom(p)) { ok = true; break; }
                }
                if (ok) break;
            }
        }
        c.assertThat("sendMsg(List) 方法", ok,
                ok ? "存在" : "两个类里都找不到带 List 的 sendMsg");
    }
}
