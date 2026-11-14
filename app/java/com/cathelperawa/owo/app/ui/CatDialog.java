package com.cathelperawa.owo.app.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public final class CatDialog {

    private CatDialog() {}

    public interface OnPick { void run(int index, String value); }
    public interface OnText { void run(String text); }

    private static Activity act(Context c) {
        if (c instanceof Activity) {
            Activity a = (Activity) c;
            if (!a.isFinishing()) return a;
        }
        Activity top = com.cathelperawa.owo.app.bridge.QQEnv.topActivity();
        if (top != null && !top.isFinishing()) return top;
        return null;
    }

    public static void pick(final Context ctx, final String title,
                            final String[] items, final OnPick cb) {
        final Activity a = act(ctx);
        if (a == null) return;
        a.runOnUiThread(new Runnable() { @Override public void run() {
            try {
                new AlertDialog.Builder(a, AlertDialog.THEME_DEVICE_DEFAULT_LIGHT)
                        .setTitle(title)
                        .setSingleChoiceItems(items, -1,
                                new DialogInterface.OnClickListener() {
                            @Override public void onClick(DialogInterface d, int which) {
                                d.dismiss();
                                try {
                                    cb.run(which, items[which]);
                                } catch (Throwable t) {
                                    com.cathelperawa.owo.app.core.HookLog.e("pick 回调异常", t);
                                }
                            }
                        })
                        .setNegativeButton("取消", null)
                        .show();
            } catch (Throwable t) {
                com.cathelperawa.owo.app.core.HookLog.e("pick 弹窗失败", t);
            }
        } });
    }

    public static void input(final Context ctx, String title, String hint,
                             String value, final boolean multi, final OnText cb) {
        final Activity a = act(ctx);
        if (a == null) return;
        final EditText et = new EditText(a);
        et.setText(value == null ? "" : value);
        et.setHint(hint);
        et.setTextSize(14);
        et.setGravity(Gravity.TOP | Gravity.START);
        et.setPadding(30, 20, 30, 20);
        if (multi) {
            et.setSingleLine(false);
            et.setMaxLines(6);
            et.setInputType(InputType.TYPE_CLASS_TEXT
                    | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                    | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        } else {
            et.setSingleLine(true);
            et.setInputType(InputType.TYPE_CLASS_TEXT
                    | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        }
        et.setSelection(et.getText().length());

        a.runOnUiThread(new Runnable() { @Override public void run() {
            try {
                new AlertDialog.Builder(a, AlertDialog.THEME_DEVICE_DEFAULT_LIGHT)
                        .setTitle(title)
                        .setView(et)
                        .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                            @Override public void onClick(DialogInterface d, int w) {
                                String s = et.getText() == null ? "" : et.getText().toString();
                                try { cb.run(s); } catch (Throwable t) {
                                    com.cathelperawa.owo.app.core.HookLog.e("input 回调异常", t);
                                }
                            }
                        })
                        .setNegativeButton("取消", null)
                        .show();
            } catch (Throwable t) {
                com.cathelperawa.owo.app.core.HookLog.e("input 弹窗失败", t);
            }
        } });
    }

    public static void custom(final Context ctx, String title, View body,
                              String okLabel, final Runnable onOk) {
        final Activity a = act(ctx);
        if (a == null) return;
        ScrollView sv = new ScrollView(a);
        sv.setPadding(30, 20, 30, 20);
        sv.addView(body, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        a.runOnUiThread(new Runnable() { @Override public void run() {
            try {
                new AlertDialog.Builder(a, AlertDialog.THEME_DEVICE_DEFAULT_LIGHT)
                        .setTitle(title)
                        .setView(sv)
                        .setPositiveButton(okLabel == null ? "确定" : okLabel,
                                new DialogInterface.OnClickListener() {
                            @Override public void onClick(DialogInterface d, int w) {
                                try { onOk.run(); } catch (Throwable t) {
                                    com.cathelperawa.owo.app.core.HookLog.e("custom 回调异常", t);
                                }
                            }
                        })
                        .setNegativeButton("取消", null)
                        .show();
            } catch (Throwable t) {
                com.cathelperawa.owo.app.core.HookLog.e("custom 弹窗失败", t);
            }
        } });
    }

    public static void info(final Context ctx, String title, String msg) {
        final Activity a = act(ctx);
        if (a == null) return;
        a.runOnUiThread(new Runnable() { @Override public void run() {
            try {
                new AlertDialog.Builder(a, AlertDialog.THEME_DEVICE_DEFAULT_LIGHT)
                        .setTitle(title)
                        .setMessage(msg)
                        .setPositiveButton("知道了", null)
                        .show();
            } catch (Throwable ignore) {
            }
        } });
    }
}
