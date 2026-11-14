package com.cathelperawa.owo.app.bridge;

import android.content.Context;

import com.cathelperawa.owo.app.core.HookApi;
import com.cathelperawa.owo.app.core.HookLog;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;

public final class VoiceStt {

    private VoiceStt() {}

    private static final String CLS_CTRL =
            "com.tencent.mobileqq.activity.aio.voicetextpanel.controller.VoiceTextSttQueryController";
    private static final String CLS_BEAN =
            "com.tencent.mobileqq.activity.aio.voicetextpanel.b.a";
    private static final String CLS_CB =
            "com.tencent.mobileqq.activity.aio.voicetextpanel.a.b";

    private static final List<String> GOT = new ArrayList<String>();
    private static volatile boolean hooked = false;

    private static volatile Object CTRL = null;

    public static String toText(Context ctx, String pttPath, int type, String peerUin,
                                long timeoutMs) {
        if (ctx == null || pttPath == null || pttPath.length() == 0) return "";
        try {
            ClassLoader cl = ctx.getClassLoader();
            ensureHook(ctx, cl);

            synchronized (GOT) {
                GOT.clear();
            }

            Class<?> ctrlCls = Class.forName(CLS_CTRL, true, cl);
            Class<?> appCls = Class.forName("mqq.app.AppRuntime", true, cl);
            Class<?> cbCls = Class.forName(CLS_CB, true, cl);

            final Class<?> fCtrlCls = ctrlCls;
            final Class<?> fAppCls = appCls;
            final Class<?> fCbCls = cbCls;
            final String fPath = pttPath;
            final Object fRt = QQEnv.appRuntime();
            final int fType = type;
            final String fUin = peerUin == null ? "" : peerUin;
            if (fRt == null) {
                com.cathelperawa.owo.app.ai.AiDebug.log("  · ASR: 拿不到 AppRuntime");
                return "";
            }

            final Throwable[] err = new Throwable[1];
            final java.util.concurrent.CountDownLatch posted =
                    new java.util.concurrent.CountDownLatch(1);
            new android.os.Handler(android.os.Looper.getMainLooper()).post(new Runnable() {
                @Override public void run() {
                    try {
                        Object ctrl = fCtrlCls.getConstructor().newInstance();
                        CTRL = ctrl;
                        Method p = fCtrlCls.getMethod("p", String.class, fAppCls, fCbCls,
                                int.class, String.class);
                        p.setAccessible(true);
                        p.invoke(ctrl, fPath, fRt, null, Integer.valueOf(fType), fUin);
                        Method m = fCtrlCls.getMethod("m", String.class);
                        m.setAccessible(true);
                        m.invoke(ctrl, fPath);
                    } catch (Throwable t) {
                        err[0] = t;
                    }
                    posted.countDown();
                }
            });
            posted.await(6, java.util.concurrent.TimeUnit.SECONDS);
            if (err[0] != null) {
                Throwable t = err[0];
                com.cathelperawa.owo.app.ai.AiDebug.log("  · ASR: 提交失败 " + t
                        + (t.getCause() == null ? "" : " / cause=" + t.getCause()));
                return "";
            }
            com.cathelperawa.owo.app.ai.AiDebug.log("  · ASR: 已提交转文字任务（type="
                    + type + "，path=" + shortPath(pttPath) + "）");

            long deadline = System.currentTimeMillis() + Math.max(4000L, timeoutMs);
            long hardCap = System.currentTimeMillis() + 6000L;
            while (System.currentTimeMillis() < deadline) {
                String cur;
                synchronized (GOT) {
                    cur = join();
                }
                if (cur.length() > 0) {
                    boolean done = isFinished(CTRL);
                    if (done || System.currentTimeMillis() > hardCap) {
                        com.cathelperawa.owo.app.ai.AiDebug.log("  · ASR: TIM 通道拿到文字（收齐）「"
                                + cur + "」");
                        return cur;
                    }
                }
                Thread.sleep(100);
            }
            com.cathelperawa.owo.app.ai.AiDebug.log("  · ASR: TIM 通道超时，没拿到文字");
            return "";
        } catch (Throwable t) {
            HookLog.e("VoiceStt.toText 异常", t);
            com.cathelperawa.owo.app.ai.AiDebug.log("  · ASR: 异常 " + t
                    + (t.getCause() == null ? "" : " / cause=" + t.getCause()));
            return "";
        }
    }

    private static synchronized void ensureHook(Context ctx, ClassLoader cl) {
        if (hooked) return;
        try {
            Class<?> ctrlCls = Class.forName(CLS_CTRL, true, cl);
            Class<?> beanCls = Class.forName(CLS_BEAN, true, cl);

            Method o = null;
            for (Method mm : ctrlCls.getDeclaredMethods()) {
                if (!"o".equals(mm.getName())) continue;
                Class<?>[] pt = mm.getParameterTypes();
                if (pt.length == 2 && pt[0] == beanCls && pt[1] == int.class) {
                    o = mm;
                    break;
                }
            }
            if (o == null) {
                com.cathelperawa.owo.app.ai.AiDebug.log("  · ASR: 没找到 o(bean,int)");
                return;
            }
            o.setAccessible(true);
            HookApi.after(o, new HookApi.Action() {
                @Override public void call(XC_MethodHook.MethodHookParam p) {
                    try {
                        Object r = p.getResult();
                        if (r instanceof String) {
                            String s = ((String) r).trim();
                            if (s.length() > 0) {
                                synchronized (GOT) {
                                    if (!GOT.contains(s)) GOT.add(s);
                                }
                            }
                        }
                    } catch (Throwable ignore) {
                    }
                }
            });
            hooked = true;
            com.cathelperawa.owo.app.ai.AiDebug.log("  · ASR: 已挂 o(bean,int)，等 TIM 回文字");
        } catch (Throwable t) {
            com.cathelperawa.owo.app.ai.AiDebug.log("  · ASR: hook 失败 " + t);
        }
    }

    private static boolean isFinished(Object ctrl) {
        if (ctrl == null) return false;
        try {
            java.lang.reflect.Field ff = ctrl.getClass().getDeclaredField("f");
            ff.setAccessible(true);
            java.util.concurrent.atomic.AtomicInteger f =
                    (java.util.concurrent.atomic.AtomicInteger) ff.get(ctrl);
            java.lang.reflect.Field fg = ctrl.getClass().getDeclaredField("g");
            fg.setAccessible(true);
            java.util.concurrent.atomic.AtomicBoolean g =
                    (java.util.concurrent.atomic.AtomicBoolean) fg.get(ctrl);
            if (g != null && g.get()) return true;
            return f != null && f.get() <= 0;
        } catch (Throwable t) {
            return false;
        }
    }

    private static String join() {
        String best = "";
        for (String s : GOT) {
            if (s == null) continue;
            String t = s.trim();
            if (t.length() > best.length()) best = t;
        }
        return best;
    }

    private static String shortPath(String p) {
        if (p == null) return "";
        int i = p.lastIndexOf('/');
        return i > 0 ? p.substring(i + 1) : p;
    }
}
