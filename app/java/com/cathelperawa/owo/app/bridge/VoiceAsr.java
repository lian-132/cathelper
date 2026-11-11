package com.cathelperawa.owo.app.bridge;

import android.content.Context;

import com.cathelperawa.owo.app.MsgStore;
import com.cathelperawa.owo.app.core.HookLog;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public final class VoiceAsr {

    private VoiceAsr() {}

    private static final String CLS_VTR =
            "com.tencent.ttpic.voicechanger.common.audio.VoiceTextRecognizer";
    private static final String CLS_VTR_LISTENER =
            "com.tencent.ttpic.voicechanger.common.audio.VoiceTextRecognizer$VTChangeListener";
    private static final String CLS_WXR = "com.qq.wx.voice.recognizer.VoiceRecognizer";
    private static final String CLS_WXR_LISTENER =
            "com.qq.wx.voice.recognizer.VoiceRecognizerListener";

    public static String toText(Context ctx, byte[] audio) {
        if (ctx == null || audio == null || audio.length < 64) return "";
        Object[] dec;
        try {
            dec = SilkDecode.anyToPcm(ctx, audio);
        } catch (Throwable t) {
            HookLog.e("VoiceAsr: 解码失败", t);
            return "";
        }
        if (dec == null) {
            com.cathelperawa.owo.app.ai.AiDebug.log("  · 语音: 解码 PCM 失败（看控制台 SilkDecode 行）");
            return "";
        }
        byte[] pcm = (byte[]) dec[0];
        int rate = (Integer) dec[1];
        int ch = (Integer) dec[2];
        if (pcm == null || pcm.length < 320) {
            MsgStore.add("[VoiceAsr-W] PCM 太少 " + (pcm == null ? 0 : pcm.length));
            return "";
        }
        
        if (rate != 16000 || ch != 1) {
            pcm = resample(pcm, rate, ch, 16000);
            MsgStore.add("[VoiceAsr] 重采样 " + rate + "Hz/" + ch + "ch → 16kHz/mono");
        }
        com.cathelperawa.owo.app.ai.AiDebug.log("  · 语音: PCM " + pcm.length
                + " 字节（" + (pcm.length * 1000L / 2 / 16000) + "ms）→ ASR");
        MsgStore.add("[VoiceAsr] 开始识别，PCM " + pcm.length + " 字节（"
                + (pcm.length * 1000L / 2 / 16000) + "ms）");

        String t = tryTtpic(ctx, pcm);
        if (t.length() > 0) return t;
        com.cathelperawa.owo.app.ai.AiDebug.log("  · 语音: ttpick 没结果，试底层 VoiceRecognizer");
        MsgStore.add("[VoiceAsr] ttpick 没结果，试底层 VoiceRecognizer");
        return tryWx(ctx, pcm);
    }

    

    private static String tryTtpic(Context ctx, final byte[] pcm) {
        final List<String> got = new ArrayList<String>();
        final CountDownLatch done = new CountDownLatch(1);
        try {
            ClassLoader cl = ctx.getClassLoader();
            final Class<?> iface = Class.forName(CLS_VTR_LISTENER, true, cl);
            Object listener = Proxy.newProxyInstance(cl, new Class<?>[]{iface},
                    new InvocationHandler() {
                        @Override public Object invoke(Object proxy, Method m, Object[] a) {
                            try {
                                if ("onVTChanged".equals(m.getName()) && a != null && a.length > 0) {
                                    String s = String.valueOf(a[0]);
                                    if (s != null && s.length() > 0) {
                                        synchronized (got) { got.add(s); }
                                    }
                                } else if ("onError".equals(m.getName())) {
                                    MsgStore.add("[VoiceAsr-W] ttpick onError " + (a == null ? "" : a[0]));
                                    done.countDown();
                                }
                            } catch (Throwable ignore) {
                            }
                            return null;
                        }
                    });

            Class<?> cls = Class.forName(CLS_VTR, true, cl);
            Object inst = cls.getMethod("getInstance").invoke(null);
            if (inst == null) return "";
            Method init = cls.getMethod("init", Context.class, boolean.class);
            Object ir = init.invoke(inst, ctx, Boolean.FALSE);
            com.cathelperawa.owo.app.ai.AiDebug.log("  · ASR: ttpick.init → " + ir);
            cls.getMethod("setVoiceTextListener", iface).invoke(inst, listener);
            cls.getMethod("start").invoke(inst);
            cls.getMethod("recognizeFromPCMBuffer", byte[].class, int.class)
                    .invoke(inst, pcm, pcm.length);

            for (int i = 0; i < 60; i++) {
                if (done.await(100, TimeUnit.MILLISECONDS)) break;
                synchronized (got) {
                    if (!got.isEmpty()) break;
                }
            }
            try { cls.getMethod("destroy").invoke(inst); } catch (Throwable ignore) {}

            String joined = join(got);
            if (joined.length() > 0) {
                MsgStore.add("[VoiceAsr] ttpick 识别成功：" + joined);
            }
            return joined;
        } catch (Throwable t) {
            HookLog.e("VoiceAsr.tryTtpic 异常", t);
            com.cathelperawa.owo.app.ai.AiDebug.log("  · 语音: ttpick 异常 " + t);
            MsgStore.add("[VoiceAsr-W] ttpick 异常 " + t);
            return "";
        }
    }

    

    private static String tryWx(Context ctx, final byte[] pcm) {
        final List<String> got = new ArrayList<String>();
        final CountDownLatch done = new CountDownLatch(1);
        try {
            ClassLoader cl = ctx.getClassLoader();
            final Class<?> iface = Class.forName(CLS_WXR_LISTENER, true, cl);
            Object listener = Proxy.newProxyInstance(cl, new Class<?>[]{iface},
                    new InvocationHandler() {
                        @Override public Object invoke(Object proxy, Method m, Object[] a) {
                            try {
                                if ("onGetResult".equals(m.getName()) && a != null && a.length > 0) {
                                    Object r = a[0];
                                    if (r != null) {
                                        Object txt = null;
                                        try {
                                            Object res = r.getClass().getField("result").get(r);
                                            if (res != null) {
                                                Object t2 = res.getClass().getField("text").get(res);
                                                txt = t2;
                                            }
                                        } catch (Throwable ignore) {
                                        }
                                        if (txt == null) {
                                            try {
                                                txt = r.getClass().getField("text").get(r);
                                            } catch (Throwable ignore) {
                                            }
                                        }
                                        if (txt != null) {
                                            synchronized (got) { got.add(String.valueOf(txt)); }
                                        }
                                    }
                                    done.countDown();
                                } else if ("onGetError".equals(m.getName())) {
                                    MsgStore.add("[VoiceAsr-W] wx onGetError " + (a == null ? "" : a[0]));
                                    done.countDown();
                                }
                            } catch (Throwable ignore) {
                            }
                            return null;
                        }
                    });

            Class<?> cls = Class.forName(CLS_WXR, true, cl);
            Object inst = cls.getMethod("shareInstance").invoke(null);
            if (inst == null) return "";
            
            String appid = readWxAppid(ctx);
            Object initRet = cls.getMethod("init", Context.class, String.class)
                    .invoke(inst, ctx, appid);
            com.cathelperawa.owo.app.ai.AiDebug.log("  · ASR: wx.init -> " + initRet
                    + " (appid=" + (appid == null || appid.length() == 0 ? "空" : appid) + ")");
            cls.getMethod("setListener", iface).invoke(inst, listener);
            Method append = cls.getMethod("appendData",
                    byte[].class, int.class, int.class, boolean.class);
            
            int chunk = 3200;
            for (int off = 0; off < pcm.length; off += chunk) {
                int n = Math.min(chunk, pcm.length - off);
                byte[] part = new byte[n];
                System.arraycopy(pcm, off, part, 0, n);
                boolean last = (off + n >= pcm.length);
                Object ar = append.invoke(inst, part, 0, n, Boolean.valueOf(last));
                if (off == 0 || last) {
                    com.cathelperawa.owo.app.ai.AiDebug.log("  · ASR: wx.appendData("
                            + (last ? "尾块" : "首块") + ") → " + ar);
                }
            }
            done.await(6, TimeUnit.SECONDS);
            try { cls.getMethod("destroy").invoke(inst); } catch (Throwable ignore) {}
            String joined = join(got);
            if (joined.length() > 0) MsgStore.add("[VoiceAsr] wx 识别成功：" + joined);
            return joined;
        } catch (Throwable t) {
            HookLog.e("VoiceAsr.tryWx 异常", t);
            com.cathelperawa.owo.app.ai.AiDebug.log("  · 语音: wx 异常 " + t);
            MsgStore.add("[VoiceAsr-W] wx 异常 " + t);
            return "";
        }
    }

    private static String readWxAppid(Context ctx) {
        try {
            Class<?> c = Class.forName(
                    "com.tencent.ttpic.voicechanger.common.audio.VoiceTextRecognizer",
                    true, ctx.getClassLoader());
            java.lang.reflect.Field f = c.getDeclaredField("WX_VOICE_RECOGNIZER_APPID");
            f.setAccessible(true);
            Object v = f.get(null);
            if (v instanceof String && ((String) v).length() > 0) return (String) v;
        } catch (Throwable ignore) {
        }
        return "";
    }

    private static String join(List<String> list) {
        StringBuilder sb = new StringBuilder();
        synchronized (list) {
            for (String s : list) {
                if (s == null) continue;
                String t = s.trim();
                if (t.length() == 0) continue;
                if (sb.indexOf(t) >= 0) continue;      
                sb.append(t);
            }
        }
        return sb.toString().trim();
    }

    
    public static byte[] to16kMono(byte[] pcm, int from, int ch) {
        if (pcm == null) return null;
        if (from == 16000 && ch == 1) return pcm;
        return resample(pcm, from, ch, 16000);
    }

    private static byte[] resample(byte[] pcm, int from, int ch, int to) {
        try {
            int frames = pcm.length / 2 / Math.max(1, ch);
            if (frames <= 0) return pcm;
            int outFrames = (int) ((long) frames * to / Math.max(1, from));
            byte[] out = new byte[outFrames * 2];
            for (int i = 0; i < outFrames; i++) {
                double src = (double) i * from / to;
                int i0 = (int) src;
                if (i0 >= frames) i0 = frames - 1;
                int i1 = Math.min(frames - 1, i0 + 1);
                double fr = src - i0;
                short s0 = shortAt(pcm, i0 * ch * 2);
                short s1 = shortAt(pcm, i1 * ch * 2);
                short v = (short) (s0 + (s1 - s0) * fr);
                out[i * 2] = (byte) (v & 0xFF);
                out[i * 2 + 1] = (byte) ((v >> 8) & 0xFF);
            }
            return out;
        } catch (Throwable t) {
            return pcm;
        }
    }

    private static short shortAt(byte[] b, int off) {
        if (off + 1 >= b.length) return 0;
        return (short) ((b[off] & 0xFF) | (b[off + 1] << 8));
    }
}
