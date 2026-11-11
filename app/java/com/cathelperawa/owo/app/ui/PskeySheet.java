package com.cathelperawa.owo.app.ui;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.cathelperawa.owo.app.bridge.CatToast;
import com.cathelperawa.owo.app.feature.ArkSigner;

public final class PskeySheet {

    private PskeySheet() {
    }

    public interface OnDone {
        void run(ArkSigner.PskeyResult r);
    }

    private static final String PAGE = "https://qun.qq.com/";

    private static final int C_TITLE = 0xFF3C4E6B;
    private static final int C_SUB = 0xFF8A9CB5;

    public static void open(final Activity act, final OnDone cb) {
        if (act == null) return;
        try {
            openInner(act, cb);
        } catch (Throwable t) {
            
            com.cathelperawa.owo.app.core.HookLog.w("PskeySheet 打开失败: " + t);
            CatToast.show("建 WebView 失败：" + t);
            ArkSigner.openPskeyPage(act);
        }
    }

    private static void openInner(final Activity act, final OnDone cb) {
        final float d = act.getResources().getDisplayMetrics().density;

        final Dialog dlg = new Dialog(act, android.R.style.Theme_Translucent_NoTitleBar);
        final LinearLayout card = new LinearLayout(act);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding((int) (16 * d), (int) (14 * d), (int) (16 * d), (int) (12 * d));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFFF7FAFF);
        bg.setCornerRadius(24 * d);
        bg.setStroke((int) d, 0xA6FFFFFF);
        card.setBackgroundDrawable(bg);
        card.setElevation(12 * d);

        TextView title = new TextView(act);
        title.setText("天玄 p_skey");
        title.setTextSize(17);
        title.setTextColor(C_TITLE);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tlp.bottomMargin = (int) (4 * d);
        card.addView(title, tlp);

        final TextView tip = new TextView(act);
        tip.setText("加载 qun.qq.com… 页面显示「需在电脑端操作」是正常的，"
                + "p_skey 照样会下发，等几秒就好");
        tip.setTextSize(11);
        tip.setTextColor(C_SUB);
        LinearLayout.LayoutParams tlp2 = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tlp2.bottomMargin = (int) (8 * d);
        card.addView(tip, tlp2);

        final WebView wv = new WebView(act);
        try {
            WebSettings s = wv.getSettings();
            s.setJavaScriptEnabled(true);
            s.setDomStorageEnabled(true);
            s.setDatabaseEnabled(true);
            s.setLoadWithOverviewMode(true);
            s.setUseWideViewPort(true);
            s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        } catch (Throwable ignore) {
        }
        try {
            CookieManager.getInstance().setAcceptCookie(true);
        } catch (Throwable ignore) {
        }
        wv.setWebViewClient(new WebViewClient() {
            @Override public void onPageFinished(WebView view, String url) {
                tip.setText("页面加载完了（显示「需电脑端」不影响），"
                        + "正在等腾讯下发 p_skey…");
            }
        });
        LinearLayout.LayoutParams wlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        wlp.bottomMargin = (int) (8 * d);
        card.addView(wv, wlp);

        LinearLayout row = new LinearLayout(act);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        TextView again = pill(act, d, "重载", 0xFF6E93D6, 0x7AFFFFFF);
        TextView grab = pill(act, d, "手动提取", 0xFFF0709F, 0xE6FFD9E8);
        TextView close = pill(act, d, "关闭", C_SUB, 0x59FFFFFF);
        row.addView(again, weightLp(d, 6));
        row.addView(grab, weightLp(d, 6));
        row.addView(close, weightLp(d, 0));
        card.addView(row, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        final boolean[] done = {false};

        close.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                dlg.dismiss();
            }
        });
        again.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                try {
                    wv.reload();
                } catch (Throwable ignore) {
                }
            }
        });
        grab.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                tip.setText("正在提取…");
                com.cathelperawa.owo.app.core.Bg.run(new Runnable() {
                    @Override public void run() {
                        final ArkSigner.PskeyResult r = ArkSigner.capturePskey();
                        act.runOnUiThread(new Runnable() {
                            @Override public void run() {
                                tip.setText(r.msg);
                            }
                        });
                        CatToast.show(r.msg);
                        if (r.ok && !done[0]) {
                            done[0] = true;
                            if (cb != null) cb.run(r);
                        }
                    }
                }, "pskey-grab");
            }
        });

        dlg.setContentView(card);
        dlg.setCanceledOnTouchOutside(false);
        dlg.show();
        Window w = dlg.getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowManager.LayoutParams lp = w.getAttributes();
            int sw = act.getResources().getDisplayMetrics().widthPixels;
            int sh = act.getResources().getDisplayMetrics().heightPixels;
            lp.width = (int) (sw * 0.92f);
            lp.height = (int) (sh * 0.76f);
            lp.gravity = Gravity.CENTER;
            lp.dimAmount = 0.28f;
            w.setAttributes(lp);
        }

        try {
            wv.loadUrl(PAGE);
        } catch (Throwable t) {
            tip.setText("WebView 加载失败：" + t);
        }

        final String oldPskey = ArkSigner.pskeyStatus().pskey;
        com.cathelperawa.owo.app.core.Bg.run(new Runnable() {
            @Override public void run() {
                for (int i = 0; i < 40; i++) {
                    try {
                        Thread.sleep(1000);
                    } catch (Throwable ignore) {
                    }
                    if (done[0]) return;
                    final ArkSigner.PskeyResult r = ArkSigner.readPskey(false);
                    if (!r.ok) continue;
                    boolean changed = oldPskey == null || !oldPskey.equals(r.pskey);
                    
                    if (!changed && i % 5 != 4) continue;
                    final ArkSigner.PskeyResult v = ArkSigner.verifyPskey();
                    if (v.ok) {
                        done[0] = true;
                        act.runOnUiThread(new Runnable() {
                            @Override public void run() {
                                tip.setText(v.msg + "（已保存）");
                            }
                        });
                        CatToast.show("p_skey 有效，能签了");
                        if (cb != null) cb.run(v);
                        return;
                    }
                    if (changed) {
                        act.runOnUiThread(new Runnable() {
                            @Override public void run() {
                                tip.setText("拿到新 p_skey，还在等它生效…");
                            }
                        });
                    }
                }
                act.runOnUiThread(new Runnable() {
                    @Override public void run() {
                        tip.setText("40 秒没等到可用的 p_skey —— 页面上登录一下，或点「手动提取」");
                    }
                });
            }
        }, "pskey-watch");

        dlg.setOnDismissListener(new android.content.DialogInterface.OnDismissListener() {
            @Override public void onDismiss(android.content.DialogInterface dialog) {
                done[0] = true;
                try {
                    wv.stopLoading();
                    wv.loadUrl("about:blank");
                    wv.destroy();
                } catch (Throwable ignore) {
                }
            }
        });
    }

    private static TextView pill(Activity act, float d, String label, int color, int bgColor) {
        TextView t = new TextView(act);
        t.setText(label);
        t.setTextSize(13);
        t.setTextColor(color);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setGravity(Gravity.CENTER);
        t.setPadding(0, (int) (11 * d), 0, (int) (11 * d));
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(14 * d);
        g.setColor(bgColor);
        g.setStroke((int) d, 0x66C9D6E8);
        t.setBackgroundDrawable(g);
        return t;
    }

    private static LinearLayout.LayoutParams weightLp(float d, int rightMarginDp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lp.rightMargin = (int) (rightMarginDp * d);
        return lp;
    }
}
