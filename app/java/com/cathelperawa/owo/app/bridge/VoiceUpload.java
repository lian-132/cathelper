package com.cathelperawa.owo.app.bridge;

import android.content.Context;

import com.cathelperawa.owo.app.MsgStore;
import com.cathelperawa.owo.app.core.HookLog;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public final class VoiceUpload {

    private static final String CLS_UPLOADER =
            "com.tencent.qqnt.qwallet.bigdata.BigDataUploader";
    private static final String CLS_CALLBACK =
            "com.tencent.qqnt.qwallet.bigdata.BigDataUploader$a";

    private VoiceUpload() {}

    public static String upload(Context ctx, String slk, String json, long timeoutMs) {
        final CountDownLatch latch = new CountDownLatch(1);
        final String[] result = new String[1];
        try {
            ClassLoader cl = ctx.getClassLoader();
            Class<?> clsUploader = Class.forName(CLS_UPLOADER, true, cl);
            Class<?> clsCallback = Class.forName(CLS_CALLBACK, true, cl);

            Object callback = Proxy.newProxyInstance(cl, new Class<?>[]{clsCallback},
                    new InvocationHandler() {
                        @Override public Object invoke(Object proxy, Method method, Object[] args) {
                            String name = method.getName();
                            try {
                                if ("a".equals(name) && args != null && args.length > 0) {
                                    byte[] b = (byte[]) args[0];
                                    result[0] = (b == null) ? "null"
                                            : new String(b, "UTF-8");
                                    MsgStore.add("[VoiceUpload]  成功回调: " + result[0]);
                                } else if ("onError".equals(name)) {
                                    MsgStore.add("[VoiceUpload-W] onError: "
                                            + (args != null && args.length > 0 ? args[0] : "?")
                                            + " " + (args != null && args.length > 1 ? args[1] : ""));
                                } else if ("toString".equals(name)) {
                                    return "CatVoiceUploaderCb";
                                } else if ("hashCode".equals(name)) {
                                    return System.identityHashCode(proxy);
                                } else if ("equals".equals(name)) {
                                    return proxy == (args == null ? null : args[0]);
                                }
                            } catch (Throwable t) {
                                HookLog.e("VoiceUpload 回调异常", t);
                            } finally {
                                if ("a".equals(name) || "onError".equals(name)) latch.countDown();
                            }
                            return null;
                        }
                    });

            Constructor<?> ctor = clsUploader.getConstructor(clsCallback);
            ctor.setAccessible(true);
            Object uploader = ctor.newInstance(callback);

            Method mE = null;
            for (Method m : clsUploader.getDeclaredMethods()) {
                if ("e".equals(m.getName()) && m.getParameterTypes().length == 2
                        && m.getParameterTypes()[0] == String.class
                        && m.getParameterTypes()[1] == byte[].class) {
                    mE = m; break;
                }
            }
            if (mE == null) {
                MsgStore.add("[VoiceUpload-W] 找不到 e(String,byte[])");
                return null;
            }
            mE.setAccessible(true);
            mE.invoke(uploader, slk, json.getBytes("UTF-8"));
            MsgStore.add("[VoiceUpload] 已发起上传 " + slk);

            if (!latch.await(timeoutMs, TimeUnit.MILLISECONDS)) {
                MsgStore.add("[VoiceUpload-W] 等待回调超时 " + timeoutMs + "ms");
                return null;
            }
            return result[0];
        } catch (Throwable t) {
            HookLog.e("VoiceUpload.upload 异常", t);
            return null;
        }
    }

    public static boolean uploadOk(Context ctx, String slk, String json, long timeoutMs) {
        String rsp = upload(ctx, slk, json, timeoutMs);
        if (rsp == null) return false;
        try {
            org.json.JSONObject o = new org.json.JSONObject(rsp);
            int status = o.optInt("status", -1);
            MsgStore.add("[VoiceUpload] status=" + status
                    + " degree=" + o.optString("degree", "-"));
            return status == 1;
        } catch (Throwable t) {
            MsgStore.add("[VoiceUpload-W] 回包解析失败 " + rsp);
            return false;
        }
    }
}
