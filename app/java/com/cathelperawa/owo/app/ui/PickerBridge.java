package com.cathelperawa.owo.app.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.cathelperawa.owo.app.bridge.CatToast;
import com.cathelperawa.owo.app.core.HookLog;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

public final class PickerBridge {

    private PickerBridge() {
    }

    public interface OnPick {
        void run(String path);
    }

    public interface OnPickMulti {
        void run(java.util.List<String> paths);
    }

    private static final int REQ = 0x0CA7;

    private static final int REQ_AUDIO = 0x0CA8;
    private static OnPick pending;
    private static OnPickMulti pendingMulti;
    private static boolean hooked;

    public static void pickAudio(final Activity act, final OnPickMulti cb) {
        if (act == null) return;
        hookOnce();
        pendingMulti = cb;
        try {
            Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType("*/*");
            i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            act.startActivityForResult(Intent.createChooser(i, "选音频"), REQ_AUDIO);
        } catch (Throwable t) {
            CatToast.show("打不开文件选择器：" + t);
            pendingMulti = null;
        }
    }

    public static void pick(final Activity act, final OnPick cb) {
        if (act == null) return;
        hookOnce();
        pending = cb;
        try {
            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.setType("image/*");
            i.addCategory(Intent.CATEGORY_OPENABLE);
            act.startActivityForResult(Intent.createChooser(i, "选一张图"), REQ);
        } catch (Throwable t) {
            CatToast.show("打不开相册：" + t);
            pending = null;
        }
    }

    private static synchronized void hookOnce() {
        if (hooked) return;
        try {
            XposedBridge.hookAllMethods(Activity.class, "onActivityResult",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam p) {
                            handle(p);
                        }
                    });
            hooked = true;
            HookLog.i("PickerBridge: onActivityResult 已挂");
        } catch (Throwable t) {
            HookLog.w("PickerBridge hook 失败: " + t);
        }
    }

    private static void handle(XC_MethodHook.MethodHookParam p) {
        try {
            if (p.args == null || p.args.length < 3) return;
            int req = (Integer) p.args[0];
            if (req == REQ_AUDIO) {
                handleMulti(p);
                return;
            }
            if (req != REQ) return;
            int res = (Integer) p.args[1];
            if (res != Activity.RESULT_OK) {
                pending = null;
                return;
            }
            Intent data = (Intent) p.args[2];
            Uri uri = data == null ? null : data.getData();
            if (uri == null) {
                pending = null;
                return;
            }
            if (!(p.thisObject instanceof Context)) {
                pending = null;
                return;
            }
            String path = copyTo((Context) p.thisObject, uri);
            OnPick cb = pending;
            pending = null;
            if (cb != null && path != null) cb.run(path);
            else if (cb != null) CatToast.show("读不到这张图");
        } catch (Throwable t) {
            HookLog.w("PickerBridge 处理失败: " + t);
        }
    }

    private static void handleMulti(XC_MethodHook.MethodHookParam p) {
        OnPickMulti cb = pendingMulti;
        try {
            int res = (Integer) p.args[1];
            if (res != Activity.RESULT_OK) { pendingMulti = null; return; }
            Intent data = (Intent) p.args[2];
            if (data == null || !(p.thisObject instanceof Context)) {
                pendingMulti = null;
                return;
            }
            Context ctx = (Context) p.thisObject;
            java.util.List<String> out = new java.util.ArrayList<String>();
            android.content.ClipData cd = data.getClipData();
            if (cd != null && cd.getItemCount() > 0) {
                for (int i = 0; i < cd.getItemCount(); i++) {
                    Uri u = cd.getItemAt(i).getUri();
                    String path = copyTo(ctx, u, extOf(ctx, u));
                    if (path != null) out.add(path);
                }
            } else if (data.getData() != null) {
                String path = copyTo(ctx, data.getData(), extOf(ctx, data.getData()));
                if (path != null) out.add(path);
            }
            HookLog.i("PickerBridge: 音频多选 " + out.size() + " 个");
            pendingMulti = null;
            if (cb != null) cb.run(out);
        } catch (Throwable t) {
            HookLog.w("PickerBridge 多选处理失败: " + t);
            pendingMulti = null;
        }
    }

    private static String extOf(Context ctx, Uri uri) {
        try {
            String p = uri.getLastPathSegment();
            if (p != null) {
                int dot = p.lastIndexOf('.');
                if (dot > 0 && dot < p.length() - 1) {
                    String e = p.substring(dot + 1).toLowerCase();
                    if (e.length() <= 5 && e.matches("[a-z0-9]+")) return e;
                }
            }
            String mime = ctx.getContentResolver().getType(uri);
            if (mime != null) {
                if (mime.contains("mpeg")) return "mp3";
                if (mime.contains("mp4")) return "m4a";
                if (mime.contains("wav")) return "wav";
                if (mime.contains("amr")) return "amr";
                if (mime.contains("ogg")) return "ogg";
                if (mime.contains("aac")) return "aac";
            }
        } catch (Throwable ignore) {
        }
        return "bin";
    }

    private static String copyTo(Context ctx, Uri uri) {
        return copyTo(ctx, uri, "jpg");
    }

    private static String copyTo(Context ctx, Uri uri, String ext) {
        InputStream in = null;
        FileOutputStream out = null;
        try {
            String name = "pick_" + System.currentTimeMillis() + "_"
                    + (int) (Math.random() * 900 + 100) + "." + ext;
            File f = new File(ctx.getCacheDir(), name);
            in = ctx.getContentResolver().openInputStream(uri);
            if (in == null) return null;
            out = new FileOutputStream(f);
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            out.flush();
            HookLog.i("PickerBridge: 已复制 " + f.getAbsolutePath()
                    + " " + f.length() + "B");
            return f.getAbsolutePath();
        } catch (Throwable t) {
            HookLog.w("PickerBridge 复制失败: " + t);
            return null;
        } finally {
            try { if (in != null) in.close(); } catch (Throwable ignore) {}
            try { if (out != null) out.close(); } catch (Throwable ignore) {}
        }
    }
}
