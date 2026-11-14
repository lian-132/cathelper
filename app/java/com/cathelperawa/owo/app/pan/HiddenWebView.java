package com.cathelperawa.owo.app.pan;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import java.util.HashMap;
import java.util.Map;

public final class HiddenWebView {

    private HiddenWebView() {}

    public interface Cb {
        void ok(String url);
        void fail(String msg);
    }

    private static final String UA =
            "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) "
                    + "Chrome/131.0.0.0 Mobile Safari/537.36";

    private static final long TIMEOUT = 25000L;

    public static void resolve(final Activity act, final String url, final String referer, final Cb cb) {
        if (act == null || url == null || cb == null) return;
        final Handler ui = new Handler(Looper.getMainLooper());
        final WebView wv = new WebView(act);
        final boolean[] done = {false};

        try {
            WebSettings s = wv.getSettings();
            s.setJavaScriptEnabled(true);
            s.setDomStorageEnabled(true);
            s.setDatabaseEnabled(true);
            s.setUserAgentString(UA);
            s.setLoadWithOverviewMode(true);
            s.setUseWideViewPort(true);
            s.setBlockNetworkImage(true);
            CookieManager.getInstance().setAcceptCookie(true);
            try {
                CookieManager.getInstance().setAcceptThirdPartyCookies(wv, true);
            } catch (Throwable ignore) {
            }
        } catch (Throwable ignore) {
        }

        wv.setAlpha(0f);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(1, 1);
        try {
            act.addContentView(wv, lp);
        } catch (Throwable ignore) {
        }

        final Runnable cleanup = new Runnable() {
            @Override public void run() {
                try {
                    act.runOnUiThread(new Runnable() {
                        @Override public void run() {
                            try {
                                ViewGroup p = (ViewGroup) wv.getParent();
                                if (p != null) p.removeView(wv);
                                wv.stopLoading();
                                wv.destroy();
                            } catch (Throwable ignore) {
                            }
                        }
                    });
                } catch (Throwable ignore) {
                }
            }
        };

        final Runnable timeout = new Runnable() {
            @Override public void run() {
                if (done[0]) return;
                done[0] = true;
                cleanup.run();
                cb.fail("过校验超时（页面 25 秒内没给出文件地址），稍后再点一次");
            }
        };

        wv.setDownloadListener(new DownloadListener() {
            @Override public void onDownloadStart(String u, String userAgent, String contentDisposition,
                                                  String mimeType, long contentLength) {
                if (done[0]) return;
                done[0] = true;
                ui.removeCallbacks(timeout);
                cleanup.run();
                cb.ok(u);
            }
        });

        wv.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, String u) {
                if (done[0] || u == null) return false;

                if (!u.contains("lanzou") && !u.contains("woozooo") && !u.startsWith("about:")) {
                    done[0] = true;
                    ui.removeCallbacks(timeout);
                    cleanup.run();
                    cb.ok(u);
                    return true;
                }
                return false;
            }

            @Override public void onPageStarted(WebView view, String u, Bitmap f) {
                if (done[0] || u == null) return;
                if (!u.contains("lanzou") && !u.contains("woozooo") && !u.startsWith("about:")) {
                    done[0] = true;
                    ui.removeCallbacks(timeout);
                    cleanup.run();
                    cb.ok(u);
                }
            }

            @Override public void onPageFinished(WebView view, String u) {

            }

            @Override public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                if (done[0]) return;
                done[0] = true;
                ui.removeCallbacks(timeout);
                cleanup.run();
                cb.fail("页面加载失败：" + description);
            }
        });

        ui.postDelayed(timeout, TIMEOUT);
        try {
            Map<String, String> extra = new HashMap<String, String>();
            if (referer != null && referer.length() > 0) extra.put("Referer", referer);
            wv.loadUrl(url, extra);
        } catch (Throwable t) {
            ui.removeCallbacks(timeout);
            cleanup.run();
            cb.fail("打开页面失败：" + t);
        }
    }
}
