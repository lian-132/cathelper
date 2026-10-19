package com.cathelperawa.owo.app.api;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;

import com.cathelperawa.owo.app.bridge.QQUtils;
import com.cathelperawa.owo.app.bridge.QQEnv;
import com.cathelperawa.owo.app.util.CatLog;

public final class UiApi {

    private UiApi() {}

    public static void toast(String msg) { QQUtils.toast(msg); }

    public static void runOnUi(Runnable r) { QQUtils.runOnUi(r); }

    public static void copyToClipboard(String label, String text) {
        try {
            Context ctx = QQEnv.appContext();
            if (ctx == null) return;
            ClipboardManager cm = (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null) cm.setPrimaryClip(ClipData.newPlainText(label, text));
        } catch (Throwable t) {
            CatLog.e("复制失败", t);
        }
    }

    public static String readClipboard() {
        try {
            Context ctx = QQEnv.appContext();
            if (ctx == null) return null;
            ClipboardManager cm = (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm == null || cm.getPrimaryClip() == null) return null;
            ClipData.Item it = cm.getPrimaryClip().getItemAt(0);
            return it == null || it.getText() == null ? null : it.getText().toString();
        } catch (Throwable t) {
            return null;
        }
    }
}
