package com.cathelperawa.owo.app.feature;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.cathelperawa.owo.app.core.HookApi;
import com.cathelperawa.owo.app.core.HookLog;

import de.robv.android.xposed.XC_MethodHook;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public final class QgameShare {

    private static final String QQ_PKG = "com.tencent.tim";
    private static final String QQ_SHARE_ACT = "com.tencent.mobileqq.activity.JumpActivity";
    private static final String APPID = "1105198412";
    private static final AtomicBoolean fired = new AtomicBoolean(false);

    private static final String TITLE = "鲸鲸测试卡";
    private static final String SUMMARY = "描述内容";
    private static final String IMAGE = "https://picsum.photos/seed/whale/600/338";
    private static final String TARGET = "https://example.com/";
    private static final String APP_NAME = "企鹅电竞";

    private QgameShare() {
    }

    public static void install(ClassLoader cl) {
        HookLog.i("QgameShare: 注入企鹅电竞");
        try {
            Class<?> act = HookApi.findClass(cl, "android.app.Activity");
            for (java.lang.reflect.Method m : act.getDeclaredMethods()) {
                if (!"onCreate".equals(m.getName())) {
                    continue;
                }
                Class<?>[] ps = m.getParameterTypes();
                if (ps.length != 1 || ps[0] != Bundle.class) {
                    continue;
                }
                m.setAccessible(true);
                HookApi.after(m, new HookApi.Action() {
                    @Override
                    public void call(XC_MethodHook.MethodHookParam p) {
                        try {
                            if (!(p.thisObject instanceof Activity)) {
                                return;
                            }
                            final Activity a = (Activity) p.thisObject;
                            if (!fired.compareAndSet(false, true)) {
                                return;
                            }
                            HookLog.i("QgameShare: 首个 Activity = " + a.getClass().getName());
                            a.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    addFloatButton(a);
                                    new android.os.Handler(android.os.Looper.getMainLooper())
                                            .postDelayed(new Runnable() {
                                                @Override
                                                public void run() {
                                                    fire(a);
                                                }
                                            }, 8000);
                                }
                            });
                        } catch (Throwable t) {
                            HookLog.w("QgameShare hook: " + t);
                        }
                    }
                });
                HookLog.i("QgameShare: Activity.onCreate 已挂");
                return;
            }
        } catch (Throwable t) {
            HookLog.e("QgameShare.install 失败", t);
        }
    }

    private static void addFloatButton(final Activity a) {
        try {
            android.widget.Button btn = new android.widget.Button(a);
            btn.setText("发卡");
            btn.setTextSize(13);
            btn.setAlpha(0.85f);
            btn.setOnClickListener(new android.view.View.OnClickListener() {
                @Override
                public void onClick(android.view.View v) {
                    fire(a);
                }
            });
            android.widget.FrameLayout.LayoutParams lp =
                    new android.widget.FrameLayout.LayoutParams(160, 160,
                            android.view.Gravity.END | android.view.Gravity.CENTER_VERTICAL);
            lp.rightMargin = 20;
            a.addContentView(btn, lp);
            HookLog.i("QgameShare: 悬浮按钮已加");
        } catch (Throwable t) {
            HookLog.w("QgameShare.addFloatButton: " + t);
        }
    }

    public static void fire(Activity act) {
        try {
            
            StringBuilder sb = new StringBuilder("mqqapi://share/to_fri?");
            sb.append("src_type=app&version=1&file_type=news");
            sb.append("&share_id=").append(APPID);
            sb.append("&app_name=").append(b64(APP_NAME));
            sb.append("&title=").append(b64(TITLE));
            sb.append("&description=").append(b64(SUMMARY));
            sb.append("&image_url=").append(b64(IMAGE));
            sb.append("&preview=").append(b64(IMAGE));
            sb.append("&url=").append(b64(TARGET));
            sb.append("&req_type=").append(b64("7"));
            sb.append("&cflag=").append(b64("0"));
            String uri = sb.toString();

            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(android.net.Uri.parse(uri));
            i.setClassName(QQ_PKG, QQ_SHARE_ACT);
            
            i.putExtra("key_b77_sdk_share", "1");
            i.putExtra("key_appid", APPID);
            i.putExtra("key_app_id", APPID);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            act.startActivity(i);
            HookLog.i("QgameShare: 已拉起 TIM，uri=" + uri);
        } catch (Throwable t) {
            HookLog.e("QgameShare.fire 失败", t);
        }
    }

    private static String b64(String s) {
        try {
            return android.net.Uri.encode(android.util.Base64.encodeToString(
                    s.getBytes("UTF-8"), android.util.Base64.NO_WRAP));
        } catch (Throwable t) {
            return s;
        }
    }

    private static Bundle buildParams() {
        Bundle b = new Bundle();
        b.putString("appid", APPID);
        b.putString("title", TITLE);
        b.putString("summary", SUMMARY);
        b.putString("image_url", IMAGE);
        b.putString("target_url", TARGET);
        b.putString("app_name", APP_NAME);
        b.putString("sdk_version", "3.3.0");
        b.putString("type", "1");
        b.putString("cflag", "0");
        return b;
    }
}
