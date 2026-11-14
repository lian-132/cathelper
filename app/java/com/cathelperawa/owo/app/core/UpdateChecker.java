package com.cathelperawa.owo.app.core;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import com.cathelperawa.owo.app.BuildInfo;
import com.cathelperawa.owo.app.ui.ExtLink;
import com.cathelperawa.owo.app.ui.UpdateDialog;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.concurrent.atomic.AtomicBoolean;

import de.robv.android.xposed.XC_MethodHook;

public final class UpdateChecker {

    private static final String PREFS = "update_state";
    private static final String KEY_PROMPTED = "prompted_code";
    private static String procTag(Context c) {
        try {
            String p = c.getPackageName();
            if (p == null) return "x";
            if (p.contains("tim")) return "tim";
            if (p.endsWith(".qq") || p.contains("mobileqq")) return "qq";
            return "app";
        } catch (Throwable t) {
            return "x";
        }
    }
    private static final String KEY_LAST_CHECK = "last_check_ms";

    private static final String SOURCE =
            "https://raw.githubusercontent.com/lian-132/cathelper/main/latest.json";

    private static final String[] PUB_KEYS = {
            "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAqjB7OPJEA9mja0s2NPG+xp7yQY62El9tj8PeT9VLEMRibyvEslNbUaiNHsaHII9haXGjRdNUZgUmmgFWYCxaAnVziavf9KgcceTo7oZTW8llvsgw9jnFZNiWrB7Qk767jVwgcr+1B2bEDfl62x562jUTdIVNBxmTgrOgF4IrWJFThiON6Oi0yYtYKrqRYlhyXrL+P2BCwBR5c98J5uQgMwKkUmms+sZHIjNZJo+AmZJ7Gis1TmwZdNPXYUqvpn+b0EXOEVNAkBjR+JIqWP9GRbBqyudTwFiAJ+FkWrRTXeUmYH18/GHqsitBBa1o1LvhCP/ZZ6jqHN+pmHoUWVoGTwIDAQAB"
    };

    private static final long CHECK_GAP_MS = 10L * 60L * 1000L;

    private static final AtomicBoolean HOST_FIRED = new AtomicBoolean(false);
    private static volatile boolean running = false;

    private UpdateChecker() {}

    public static void check(final Context ctx) {
        if (ctx == null || running) return;
        final Context uiCtx = ctx;
        final Context app = ctx.getApplicationContext() != null
                ? ctx.getApplicationContext() : ctx;
        running = true;

        new Thread(new Runnable() {
            @Override public void run() {
                try {
                    String raw = fetch();
                    if (raw == null || raw.length() == 0) return;

                    JSONObject envelope = new JSONObject(raw);
                    String payload = envelope.optString("payload", "");
                    String sig = envelope.optString("sig", "");
                    if (payload.length() == 0 || sig.length() == 0) return;
                    if (!verify(payload, sig)) {
                        HookLog.w("UpdateChecker: 签名校验失败，忽略");
                        return;
                    }

                    final JSONObject o = new JSONObject(payload);
                    app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                            .edit().putLong(KEY_LAST_CHECK, System.currentTimeMillis()).apply();

                    playAll(uiCtx, o);
                } catch (Throwable t) {
                    HookLog.w("UpdateChecker: " + t);
                } finally {
                    running = false;
                }
            }
        }, "cat-update-check").start();
    }

    private static boolean verify(String payload, String sigB64) {
        try {
            byte[] sig = android.util.Base64.decode(sigB64, android.util.Base64.DEFAULT);
            byte[] data = payload.getBytes("UTF-8");
            for (String k : PUB_KEYS) {
                try {
                    byte[] der = android.util.Base64.decode(k, android.util.Base64.DEFAULT);
                    PublicKey pub = KeyFactory.getInstance("RSA")
                            .generatePublic(new X509EncodedKeySpec(der));
                    Signature s = Signature.getInstance("SHA256withRSA");
                    s.initVerify(pub);
                    s.update(data);
                    if (s.verify(sig)) return true;
                } catch (Throwable ignore) {
                }
            }
        } catch (Throwable t) {
            HookLog.w("UpdateChecker.verify: " + t);
        }
        return false;
    }

    private static boolean looksLikeFile(String u) {
        if (u == null) return false;
        String l = u.toLowerCase();
        if (l.endsWith(".apk") || l.endsWith(".zip") || l.endsWith(".7z")
                || l.endsWith(".rar") || l.endsWith(".mp4") || l.endsWith(".mp3")
                || l.endsWith(".txt") || l.endsWith(".pdf")) return true;
        return l.contains("/releases/download/");
    }

    private static boolean isPanLink(String u) {
        if (u == null) return false;
        String l = u.toLowerCase();
        return l.contains("pan.baidu.com") || l.contains("lanzou") || l.contains("quark.cn")
                || l.contains("drive.uc.cn") || l.contains("123pan") || l.contains("yun.139.com")
                || l.contains("pan.xunlei.com") || l.contains("weiyun.com")
                || l.contains("aliyundrive") || l.contains("alipan.com");
    }

    private static void startDownload(final Context ctx, final String url, final String name) {
        try {
            if (android.os.Build.VERSION.SDK_INT >= 33
                    && ctx.checkSelfPermission("android.permission.POST_NOTIFICATIONS")
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                com.cathelperawa.owo.app.bridge.CatToast.show("通知权限没开，去设置里打开就能看下载进度");
            }
        } catch (Throwable ignore) {
        }
        try {
            com.cathelperawa.owo.app.pan.PanProvider.Link l =
                    new com.cathelperawa.owo.app.pan.PanProvider.Link();
            l.url = url;
            l.filename = name == null ? "" : name;
            l.ua = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36";
            com.cathelperawa.owo.app.pan.PanDl.enqueue(ctx, l, name);
            com.cathelperawa.owo.app.bridge.CatToast.show("开始下载：" + name + "，进度看通知栏");
        } catch (Throwable t) {
            HookLog.w("UpdateChecker download: " + t);
            ExtLink.open(ctx, url);
        }
    }

    private static void openPan(Context ctx, String url) {
        try {
            android.content.ClipboardManager cm = (android.content.ClipboardManager)
                    ctx.getSystemService(android.content.Context.CLIPBOARD_SERVICE);
            if (cm != null) {
                cm.setPrimaryClip(android.content.ClipData.newPlainText("pan", url));
            }
            android.content.Intent it = new android.content.Intent(ctx,
                    com.cathelperawa.owo.app.pan.PanActivity.class);
            it.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(it);
            com.cathelperawa.owo.app.bridge.CatToast.show("链接已复制，去网盘页粘贴解析");
        } catch (Throwable t) {
            ExtLink.open(ctx, url);
        }
    }

    private static Activity actOf(Context ctx) {
        try {
            if (ctx instanceof Activity) return (Activity) ctx;
            return com.cathelperawa.owo.app.bridge.QQEnv.topActivity();
        } catch (Throwable t) {
            return null;
        }
    }

    private static void playAll(final Context ctx, final JSONObject o) {
        final java.util.ArrayList<com.cathelperawa.owo.app.ui.NoticePagerDialog.Page> pages =
                new java.util.ArrayList<com.cathelperawa.owo.app.ui.NoticePagerDialog.Page>();
        final SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        final boolean force = new java.io.File(ctx.getFilesDir(), "update_force").exists();
        final Activity act = actOf(ctx);

        final int code = o.optInt("versionCode", 0);
        final String name = o.optString("versionName", String.valueOf(code));
        final String log = o.optString("changelog", "");
        final String apk = o.optString("apkUrl", o.optString("releaseUrl", ""));

        final boolean needUpd = code > BuildInfo.VERSION_CODE;
        if (needUpd) {
            markPrompted(ctx, code);
            com.cathelperawa.owo.app.ui.NoticePagerDialog.Page p =
                    new com.cathelperawa.owo.app.ui.NoticePagerDialog.Page();
            p.badge = "新";
            p.title = "发现新版本";
            p.sub = name + "（" + code + "）";
            p.body = log;
            p.actionLabel = "更新";
            final String dlUrl = o.optString("apkDownload", "");
            final String dlName = "猫娘助手-" + name + ".apk";
            p.action = new Runnable() {
                @Override public void run() {
                    if (dlUrl.length() > 0) {
                        startDownload(ctx, dlUrl, dlName);
                        return;
                    }
                    String target = apk == null || apk.length() == 0
                            ? ExtLink.REPO + "/releases/latest" : apk;
                    if (looksLikeFile(target)) startDownload(ctx, target, dlName);
                    else ExtLink.open(ctx, target);
                }
            };
            pages.add(p);
        }

        org.json.JSONArray arr = o.optJSONArray("notices");
        if (arr != null) {
            int picked = 0;
            for (int i = 0; i < arr.length() && picked < 3; i++) {
                final JSONObject n = arr.optJSONObject(i);
                if (n == null) continue;
                final String id = n.optString("id", "");
                if (id.length() == 0) continue;
                final boolean repeat = n.optBoolean("repeat", false);
                if (!repeat && sp.getBoolean("notice_" + id + "_" + procTag(ctx), false)) continue;
                sp.edit().putBoolean("notice_" + id + "_" + procTag(ctx), true).apply();
                picked++;
                final String url = n.optString("url", "");
                final String dl = n.optString("download", "");
                final String dlName = n.optString("filename", "文件");
                com.cathelperawa.owo.app.ui.NoticePagerDialog.Page p =
                        new com.cathelperawa.owo.app.ui.NoticePagerDialog.Page();
                p.badge = "公告";
                p.title = n.optString("title", "公告");
                p.sub = "";
                p.body = n.optString("body", "");
                p.image = n.optString("image", "");
                p.actionLabel = n.optString("button",
                        dl.length() > 0 ? "下载" : "去看看");
                org.json.JSONArray btns = n.optJSONArray("buttons");
                if (btns != null) {
                    for (int j = 0; j < btns.length() && j < 3; j++) {
                        final org.json.JSONObject b = btns.optJSONObject(j);
                        if (b == null) continue;
                        final String bu = b.optString("url", "");
                        final String bd = b.optString("download", "");
                        final String bf = b.optString("filename", "文件");
                        com.cathelperawa.owo.app.ui.NoticePagerDialog.Btn btn =
                                new com.cathelperawa.owo.app.ui.NoticePagerDialog.Btn();
                        btn.label = b.optString("label", "打开");
                        btn.action = new Runnable() {
                            @Override public void run() {
                                if (bd.length() > 0) startDownload(ctx, bd, bf);
                                else if (bu.length() > 0) {
                                    if (isPanLink(bu)) openPan(ctx, bu);
                                    else if (looksLikeFile(bu)) startDownload(ctx, bu, bf);
                                    else ExtLink.open(ctx, bu);
                                }
                            }
                        };
                        p.buttons.add(btn);
                    }
                }
                p.action = new Runnable() {
                    @Override public void run() {
                        if (dl.length() > 0) {
                            startDownload(ctx, dl, dlName);
                        } else if (url != null && url.length() > 0) {
                            if (isPanLink(url)) openPan(ctx, url);
                            else if (looksLikeFile(url)) startDownload(ctx, url, dlName);
                            else ExtLink.open(ctx, url);
                        }
                    }
                };
                pages.add(p);
            }
        }

        if (pages.isEmpty() || act == null) return;
        final boolean single = pages.size() == 1
                && "公告".equals(pages.get(0).badge);
        final boolean singleUpd = pages.size() == 1 && "新".equals(pages.get(0).badge);
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override public void run() {
                try {
                    if (single) {
                        com.cathelperawa.owo.app.ui.NoticePagerDialog.Page p0 = pages.get(0);
                        com.cathelperawa.owo.app.ui.AnnouncementDialog.show(act,
                                p0.title, p0.body, p0.image, p0.actionLabel,
                                new com.cathelperawa.owo.app.ui.AnnouncementDialog.OnOk() {
                                    @Override public void run() {
                                        if (p0.action != null) p0.action.run();
                                    }
                                }, null);
                        return;
                    }
                    if (singleUpd) {
                        com.cathelperawa.owo.app.ui.NoticePagerDialog.Page p0 = pages.get(0);
                        com.cathelperawa.owo.app.ui.UpdateDialog.show(act,
                                name, code, p0.body,
                                new com.cathelperawa.owo.app.ui.UpdateDialog.OnUpdate() {
                                    @Override public void run() {
                                        if (p0.action != null) p0.action.run();
                                    }
                                }, null);
                        return;
                    }
                    com.cathelperawa.owo.app.ui.NoticePagerDialog.show(act, pages, null);
                } catch (Throwable t) {
                    HookLog.w("UpdateChecker pager: " + t);
                }
            }
        });
    }

    private static void markPrompted(Context c, int code) {
        try {
            c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                    .edit().putInt(KEY_PROMPTED + "_" + procTag(c), code).apply();
        } catch (Throwable ignore) {
        }
    }

    private static String fetch() {
        HttpURLConnection c = null;
        try {
            String u = SOURCE + (SOURCE.contains("?") ? "&" : "?") + "t=" + System.currentTimeMillis();
            c = (HttpURLConnection) new URL(u).openConnection();
            c.setConnectTimeout(6000);
            c.setReadTimeout(6000);
            c.setRequestMethod("GET");
            c.setRequestProperty("User-Agent", "cathelper-update");
            c.setRequestProperty("Cache-Control", "no-cache");
            if (c.getResponseCode() != 200) return null;
            InputStream in = c.getInputStream();
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) > 0) {
                bo.write(buf, 0, n);
                if (bo.size() > 256 * 1024) break;
            }
            in.close();
            String s = new String(bo.toByteArray(), "UTF-8").trim();
            return s.startsWith("{") ? s : null;
        } catch (Throwable t) {
            return null;
        } finally {
            if (c != null) try { c.disconnect(); } catch (Throwable ignore) {}
        }
    }

    public static void installHostHook(ClassLoader cl) {
        try {
            Class<?> act = HookApi.findClass(cl, "android.app.Activity");
            if (act == null) return;
            int n = 0;
            for (Method m : act.getDeclaredMethods()) {
                if (!"onCreate".equals(m.getName())) continue;
                Class<?>[] ps = m.getParameterTypes();
                if (ps.length != 1 || !"android.os.Bundle".equals(ps[0].getName())) continue;
                m.setAccessible(true);
                HookApi.after(m, new HookApi.Action() {
                    @Override public void call(XC_MethodHook.MethodHookParam p) {
                        try {
                            if (!(p.thisObject instanceof Activity)) return;
                            final Activity a = (Activity) p.thisObject;
                            if (!HOST_FIRED.compareAndSet(false, true)) return;
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override public void run() {
                                    if (a.isFinishing()) {
                                        HOST_FIRED.set(false);
                                        return;
                                    }
                                    check(a);
                                }
                            }, 2500);
                        } catch (Throwable ignore) {
                        }
                    }
                });
                n++;
            }
            HookLog.i("UpdateChecker: Activity.onCreate 已挂 " + n);
        } catch (Throwable t) {
            HookLog.e("UpdateChecker.installHostHook", t);
        }
    }
}
