package com.cathelperawa.owo.app.cookie;

import android.app.Activity;
import com.cathelperawa.owo.app.core.Bg;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.view.WindowInsets;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.cathelperawa.owo.app.R;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class CookieLoginActivity extends Activity {

    public static final String EXTRA_PLATFORM = "platform";

    private static final String DESKTOP_UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                    + "(KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36";

    private static final String MOBILE_UA =
            "Mozilla/5.0 (Linux; Android 14; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) "
                    + "Chrome/130.0.0.0 Mobile Safari/537.36";

    
    private static final String VIEWPORT_JS =
            "(function(){try{var m=document.querySelector('meta[name=viewport]');"
                    + "if(!m)return;"
                    + "var c=m.getAttribute('content')||'';"
                    + "c=c.replace(/user-scalable\\s*=\\s*no/gi,'user-scalable=yes')"
                    + ".replace(/maximum-scale\\s*=\\s*[0-9.]+/gi,'maximum-scale=5.0');"
                    + "if(!/user-scalable/i.test(c))c+=(c?',':'')+'user-scalable=yes';"
                    + "m.setAttribute('content',c);"
                    + "}catch(e){}})()";

    private float zoomLevel = 1.0f;

    private void applyZoom() {
        if (wv == null) return;
        try {
            wv.evaluateJavascript("(function(){try{document.body.style.zoom='"
                    + zoomLevel + "';}catch(e){}})()", null);
        } catch (Throwable ignore) {
        }
    }

    private void doZoom(int dir) {
        zoomLevel += dir * 0.15f;
        if (zoomLevel < 0.4f) zoomLevel = 0.4f;
        if (zoomLevel > 2.2f) zoomLevel = 2.2f;
        applyZoom();
        try {
            setStatus("● 缩放 " + Math.round(zoomLevel * 100) + "%", false);
        } catch (Throwable ignore) {
        }
    }

    private static final long POLL_MS = 1500L;
    
    private static final long BASELINE_DELAY_MS = 2500L;
    
    private static final long CHECK_THROTTLE_MS = 5000L;
    
    private static final long SAME_FAIL_RETRY_MS = 10000L;

    private CookiePlatforms.P p;
    private WebView wv;
    private TextView tvStatus;
    private TextView tvHint;

    private final Handler ui = new Handler(Looper.getMainLooper());

    private String baseline = null;
    private boolean baselineTaken = false;

    private String lastFailed = "";
    private long lastFailedAt = 0L;
    
    private long lastCheckAt = 0L;
    
    private boolean checking = false;
    
    private boolean done = false;

    private final Runnable pollTask = new Runnable() {
        @Override public void run() {
            if (done) return;
            try {
                pollOnce();
            } catch (Throwable ignore) {
            }
            if (!done) ui.postDelayed(this, POLL_MS);
        }
    };

    
    private void applyWindowInsets() {
        try {
            getWindow().setBackgroundDrawable(new ColorDrawable(0xFF0F131A));
        } catch (Throwable ignore) {
        }
        try {
            View decor = getWindow().getDecorView();
            int f = decor.getSystemUiVisibility();
            f &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            f &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            decor.setSystemUiVisibility(f);
        } catch (Throwable ignore) {
        }

        final View root = findViewById(R.id.root_cookie);
        if (root == null) return;
        final int pl = root.getPaddingLeft();
        final int pr = root.getPaddingRight();
        final int pt = root.getPaddingTop();
        final int pb = root.getPaddingBottom();
        root.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            @Override public WindowInsets onApplyWindowInsets(View v, WindowInsets insets) {
                int top = 0, bottom = 0;
                try {
                    if (Build.VERSION.SDK_INT >= 30) {
                        top = insets.getInsets(WindowInsets.Type.statusBars()).top;
                        bottom = insets.getInsets(WindowInsets.Type.navigationBars()).bottom;
                    } else {
                        top = insets.getSystemWindowInsetTop();
                        bottom = insets.getSystemWindowInsetBottom();
                    }
                } catch (Throwable ignore) {
                }
                v.setPadding(pl, pt + top, pr, pb + bottom);
                return insets;
            }
        });
        root.requestApplyInsets();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cookie_login);
        applyWindowInsets();

        p = pickPlatform(getIntent());
        try {
            ((TextView) findViewById(R.id.tv_cookie_title)).setText("登录" + p.name);
        } catch (Throwable ignore) {
        }

        tvStatus = (TextView) findViewById(R.id.tv_cookie_status);
        tvHint = (TextView) findViewById(R.id.tv_cookie_hint);

        findViewById(R.id.btn_back_cookie).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { onBackPressed(); }
        });
        findViewById(R.id.btn_zoom_out).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { doZoom(-1); }
        });
        findViewById(R.id.btn_zoom_in).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { doZoom(1); }
        });
        findViewById(R.id.btn_cookie_reload).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                baselineTaken = false;
                baseline = null;
                lastFailed = "";
                lastFailedAt = 0L;
                wv.reload();
                setStatus("● 重新加载中…", false);
            }
        });
        findViewById(R.id.btn_cookie_save).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { saveManually(); }
        });
        findViewById(R.id.btn_cookie_logout).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { confirmLogout(); }
        });
        findViewById(R.id.btn_paste_cookie).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { showPasteDialog(); }
        });

        setupWebView();
        wv.loadUrl(p.loginUrl);

        CookieStore.Entry saved = CookieStore.get(this, p.id);
        if (saved != null) {
            setStatus("● 已保存：" + (saved.label.length() > 0 ? saved.label : "上次登录")
                    + "（" + fmtTime(saved.savedAt) + "）", true);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        ui.removeCallbacks(pollTask);
        ui.postDelayed(pollTask, POLL_MS);
    }

    @Override
    protected void onPause() {
        super.onPause();
        ui.removeCallbacks(pollTask);
    }

    @Override
    protected void onDestroy() {
        done = true;
        ui.removeCallbacks(pollTask);
        try {
            if (wv != null) {
                wv.stopLoading();
                wv.setWebChromeClient(null);
                wv.setWebViewClient(new WebViewClient());
                wv.loadUrl("about:blank");
                android.view.ViewParent parent = wv.getParent();
                if (parent instanceof android.view.ViewGroup) {
                    ((android.view.ViewGroup) parent).removeView(wv);
                }
                wv.destroy();
                wv = null;
            }
        } catch (Throwable ignore) {
        }
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (wv != null && wv.canGoBack()) {
            wv.goBack();
            return;
        }
        super.onBackPressed();
    }

    private void setupWebView() {
        wv = (WebView) findViewById(R.id.wv_cookie);

        WebSettings s = wv.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setSupportZoom(true);
        s.setBuiltInZoomControls(true);
        s.setDisplayZoomControls(false);
        s.setUseWideViewPort(true);
        s.setLoadWithOverviewMode(true);
        s.setTextZoom(100);
        
        boolean mobile = p != null && ("baidu".equals(p.id) || "pan123".equals(p.id));
        s.setUserAgentString(mobile ? MOBILE_UA : DESKTOP_UA);
        if (mobile) {
            s.setUseWideViewPort(false);
            s.setLoadWithOverviewMode(false);
        }
        try {
            s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        } catch (Throwable ignore) {
        }

        try {
            CookieManager.getInstance().setAcceptCookie(true);
            CookieManager.getInstance().setAcceptThirdPartyCookies(wv, true);
        } catch (Throwable ignore) {
        }

        wv.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return rewriteIfCleartext(url);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest req) {
                String u = (req == null || req.getUrl() == null) ? null : req.getUrl().toString();
                return rewriteIfCleartext(u);
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                if (!baselineTaken) setStatus("● 页面加载中…", false);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                setStatus("● 还没登录", false);
                
                try {
                    view.evaluateJavascript(VIEWPORT_JS, null);
                } catch (Throwable ignore) {
                }
                if (zoomLevel != 1.0f) {
                    ui.postDelayed(new Runnable() {
                        @Override public void run() { applyZoom(); }
                    }, 300L);
                }
                if (!baselineTaken) {
                    baselineTaken = true;
                    ui.postDelayed(new Runnable() {
                        @Override public void run() {
                            baseline = readCookie();
                        }
                    }, BASELINE_DELAY_MS);
                }
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest req, WebResourceError err) {
                
                if (req == null || !req.isForMainFrame()) return;
                setStatus("● 页面加载失败，可能需要代理", false);
                toast("网页打不开，检查网络或代理后再点「刷新页面」");
            }
        });

        wv.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (newProgress < 100 && tvHint != null) {
                    tvHint.setText("加载中 " + newProgress + "%");
                } else if (tvHint != null) {
                    tvHint.setText("登录后自动保存");
                }
            }
        });
    }

    private boolean rewriteIfCleartext(String url) {
        if (url == null || url.length() == 0) return false;
        if (url.startsWith("http://")) {
            try {
                wv.loadUrl("https://" + url.substring(7));
            } catch (Throwable ignore) {
            }
            return true;
        }
        return false;
    }

    private CookiePlatforms.P pickPlatform(Intent it) {
        String id = it == null ? null : it.getStringExtra(EXTRA_PLATFORM);
        CookiePlatforms.P x = CookiePlatforms.byId(id);
        return x != null ? x : CookiePlatforms.firstReady();
    }

    
    private String lsToken = "";

    private boolean needLocalToken() {
        return p != null && ("pan123".equals(p.id) || "xunlei".equals(p.id));
    }

    private static final String XL_CLIENT_ID = "Xqp0kJBXWhwaTpB6";

    private void refreshLocalToken() {
        if (wv == null || !needLocalToken()) return;
        final boolean xunlei = "xunlei".equals(p.id);
        String js = xunlei
                
                ? "(function(){try{return localStorage.getItem('credentials_" + XL_CLIENT_ID
                        + "')||''}catch(e){return ''}})()"
                
                : "(function(){try{return localStorage.getItem('authorToken')||''}catch(e){return ''}})()";
        try {
            wv.evaluateJavascript(js, new android.webkit.ValueCallback<String>() {
                @Override public void onReceiveValue(String v) {
                    if (v == null) return;
                    String t = v.trim();
                    if (t.startsWith("\"") && t.endsWith("\"") && t.length() >= 2) {
                        t = t.substring(1, t.length() - 1);
                    }
                    t = t.replace("\\u003d", "=").replace("\\\"", "\"");
                    if (t.length() == 0 || "null".equals(t)) return;
                    if (!xunlei) {
                        lsToken = t;
                        return;
                    }
                    try {
                        org.json.JSONObject o = new org.json.JSONObject(t);
                        String access = o.optString("access_token");
                        String refresh = o.optString("refresh_token");
                        if (access.length() > 0) {
                            lsToken = access;
                            xlRefresh = refresh;
                        }
                    } catch (Throwable ignore) {
                    }
                }
            });
        } catch (Throwable ignore) {
        }
    }

    private String xlRefresh = "";

    private String readCookie() {
        try {
            String c = CookieManager.getInstance().getCookie(p.cookieUrl);
            return c == null ? "" : c;
        } catch (Throwable t) {
            return "";
        }
    }

    private void pollOnce() {
        if (checking || done) return;
        if (needLocalToken()) {
            refreshLocalToken();
            if (lsToken.length() == 0) return;   
        }
        String cookie = readCookie();
        if (needLocalToken() && lsToken.length() > 0) {
            if ("xunlei".equals(p.id)) {
                if (cookie.indexOf("accessToken=") < 0) {
                    cookie = (cookie.length() == 0 ? "" : cookie + "; ") + "accessToken=" + lsToken
                            + "; refreshToken=" + (xlRefresh == null ? "" : xlRefresh);
                }
            } else if (cookie.indexOf("authorToken=") < 0) {
                cookie = cookie.length() == 0 ? ("authorToken=" + lsToken)
                        : (cookie + "; authorToken=" + lsToken);
            }
        }
        final String merged = cookie;
        if (cookie.length() == 0) return;
        
        if (p.needField != null && p.needField.length() > 0
                && cookie.indexOf(p.needField + "=") < 0) {
            return;
        }
        
        if (baseline != null && baseline.equals(merged)) return;
        
        long now = System.currentTimeMillis();
        if (merged.equals(lastFailed) && now - lastFailedAt < SAME_FAIL_RETRY_MS) return;
        
        if (now - lastCheckAt < CHECK_THROTTLE_MS) return;

        lastCheckAt = now;
        checking = true;
        setStatus("● 正在确认登录状态…", false);

        Bg.run(new Runnable() {
            @Override public void run() {
                final CheckResult r = verify(merged);
                ui.post(new Runnable() {
                    @Override public void run() {
                        checking = false;
                        if (done || isFinishing()) return;
                        if (r.state == STATE_OK) {
                            saveAndFinish(merged, r.label);
                        } else if (r.state == STATE_NOT_LOGIN) {
                            
                            lastFailed = merged;
                            lastFailedAt = System.currentTimeMillis();
                            setStatus("● 还没登录", false);
                        } else {
                            
                            setStatus("● 网络异常，无法确认（可点「立即保存」）", false);
                        }
                    }
                });
            }
        }, "cat-cookie-check");
    }

    private static final int STATE_OK = 0;
    private static final int STATE_NOT_LOGIN = 1;
    private static final int STATE_NET_ERROR = 2;

    private static final class CheckResult {
        int state = STATE_NET_ERROR;
        String label = "";
    }

    private CheckResult verify(String cookie) {
        CheckResult r = new CheckResult();
        if (p.checkKind == CookiePlatforms.CHECK_NONE
                || p.checkUrl == null || p.checkUrl.length() == 0) {
            r.state = STATE_OK;
            r.label = "";
            return r;
        }
        HttpURLConnection conn = null;
        try {
            URL u = new URL(p.checkUrl);
            conn = (HttpURLConnection) u.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);
            conn.setRequestProperty("User-Agent", DESKTOP_UA);
            conn.setRequestProperty("Referer", p.referer == null ? p.cookieUrl : p.referer);
            conn.setRequestProperty("Cookie", cookie);
            conn.setInstanceFollowRedirects(true);

            int code = conn.getResponseCode();
            if (code != 200) {
                r.state = STATE_NET_ERROR;
                return r;
            }
            String body = readAll(conn.getInputStream());
            JSONObject o = new JSONObject(body);

            if (p.checkKind == CookiePlatforms.CHECK_ACCOUNT) {
                
                if (o.optInt("code", -1) != 200) { r.state = STATE_NOT_LOGIN; return r; }
                if (o.optJSONObject("account") == null) { r.state = STATE_NOT_LOGIN; return r; }
                JSONObject prof = o.optJSONObject("profile");
                r.label = prof == null ? "" : prof.optString("nickname", "");
                r.state = STATE_OK;
                return r;
            }
            if (p.checkKind == CookiePlatforms.CHECK_BILI) {
                
                JSONObject data = o.optJSONObject("data");
                if (data == null || !data.optBoolean("isLogin")) {
                    r.state = STATE_NOT_LOGIN;
                    return r;
                }
                r.label = data.optString("uname", "");
                r.state = STATE_OK;
                return r;
            }
            if (p.checkKind == CookiePlatforms.CHECK_QUARK) {
                if (!o.optBoolean("success", false)) { r.state = STATE_NOT_LOGIN; return r; }
                JSONObject d = o.optJSONObject("data");
                r.label = d == null ? "" : d.optString("nickname", "");
                r.state = STATE_OK;
                return r;
            }
            r.state = STATE_OK;
            return r;
        } catch (Throwable t) {
            r.state = STATE_NET_ERROR;
            return r;
        } finally {
            try { if (conn != null) conn.disconnect(); } catch (Throwable ignore) { }
        }
    }

    private void saveAndFinish(String cookie, String label) {
        if (done) return;
        done = true;
        ui.removeCallbacks(pollTask);
        boolean ok = CookieStore.save(this, p.id, cookie, label);
        flushCookies();

        try {
            if (com.cathelperawa.owo.app.pan.PanPrefs.wipeWebViewCookies(this)) {
                CookieManager.getInstance().removeAllCookies(null);
                CookieManager.getInstance().flush();
                android.util.Log.i("CatHelper", "已按设置清除 WebView Cookie");
            }
        } catch (Throwable ignore) {
        }
        if (ok) {
            toast("已保存" + p.name + "登录态"
                    + (label != null && label.length() > 0 ? "：" + label : ""));
            setResult(RESULT_OK);
            finish();
        } else {
            done = false;
            toast("保存失败（写文件出错）");
        }
    }

    private void saveManually() {
        if (needLocalToken()) {
            refreshLocalToken();
            if (lsToken.length() == 0) {
                toast("还没拿到 123 的登录令牌，先在页面里登录");
                return;
            }
        }
        String cookie = readCookie();
        if (needLocalToken() && lsToken.length() > 0) {
            if ("xunlei".equals(p.id)) {
                if (cookie.indexOf("accessToken=") < 0) {
                    cookie = (cookie.length() == 0 ? "" : cookie + "; ") + "accessToken=" + lsToken
                            + "; refreshToken=" + (xlRefresh == null ? "" : xlRefresh);
                }
            } else if (cookie.indexOf("authorToken=") < 0) {
                cookie = cookie.length() == 0 ? ("authorToken=" + lsToken)
                        : (cookie + "; authorToken=" + lsToken);
            }
        }
        if (cookie.length() == 0) {
            toast("网页还没登录，先扫码或输账号");
            return;
        }
        if (p.needField.length() > 0 && cookie.indexOf(p.needField + "=") < 0) {
            toast("还没检测到登录态，扫码登录后会自动保存");
            return;
        }
        saveAndFinish(cookie, "");
    }

    private void confirmLogout() {
        new AlertDialog.Builder(this)
                .setTitle("退出" + p.name + "登录")
                .setMessage("会清掉网页 Cookie 和已保存的登录态。")
                .setNegativeButton("取消", null)
                .setPositiveButton("退出", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int w) {
                        CookieStore.remove(CookieLoginActivity.this, p.id);
                        try {
                            CookieManager cm = CookieManager.getInstance();
                            cm.removeAllCookies(new ValueCallback<Boolean>() {
                                @Override public void onReceiveValue(Boolean value) { }
                            });
                            cm.flush();
                        } catch (Throwable ignore) {
                        }
                        baselineTaken = false;
                        baseline = null;
                        lastFailed = "";
                        setStatus("● 已退出登录", false);
                        wv.loadUrl(p.loginUrl);
                    }
                })
                .show();
    }

    private void showPasteDialog() {
        final CookieStore.Entry e = CookieStore.get(this, p.id);
        final EditText et = new EditText(this);
        et.setHint("MUSIC_U=xxxx; __csrf=yyyy");
        et.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        et.setMinLines(4);
        et.setMaxLines(10);
        if (e != null) et.setText(e.cookie);

        FrameLayout wrap = new FrameLayout(this);
        int pad = (int) (getResources().getDisplayMetrics().density * 16);
        wrap.setPadding(pad, pad / 2, pad, 0);
        wrap.addView(et);

        new AlertDialog.Builder(this)
                .setTitle("粘贴 " + p.name + " Cookie")
                .setView(wrap)
                .setNegativeButton("取消", null)
                .setNeutralButton("清空", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int w) {
                        CookieStore.remove(CookieLoginActivity.this, p.id);
                        toast("已清空");
                        setStatus("● 还没登录", false);
                    }
                })
                .setPositiveButton("保存", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int w) {
                        final String ck = et.getText() == null ? "" : et.getText().toString().trim();
                        if (ck.length() == 0) {
                            toast("内容为空");
                            return;
                        }
                        if (p.needField.length() > 0 && ck.indexOf(p.needField + "=") < 0) {
                            toast("里面没有 " + p.needField + "，确认复制完整了吗？");
                        }
                        checking = true;
                        Bg.run(new Runnable() {
                            @Override public void run() {
                                final CheckResult r = verify(ck);
                                ui.post(new Runnable() {
                                    @Override public void run() {
                                        checking = false;
                                        if (r.state == STATE_OK) {
                                            saveAndFinish(ck, r.label);
                                        } else {
                                            askSaveAnyway(ck, r.state == STATE_NET_ERROR
                                                    ? "网络异常，没能校验" : "校验没通过（可能已过期）");
                                        }
                                    }
                                });
                            }
                        }, "cat-cookie-paste");
                    }
                })
                .show();
    }

    private void askSaveAnyway(final String ck, String why) {
        new AlertDialog.Builder(this)
                .setTitle("仍然保存？")
                .setMessage(why + "。保存后如果不好用，回来重新登录即可。")
                .setNegativeButton("不保存", null)
                .setPositiveButton("保存", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int w) {
                        saveAndFinish(ck, "");
                    }
                })
                .show();
    }

    private void setStatus(final String text, boolean good) {
        if (tvStatus == null) return;
        tvStatus.setText(text);
        tvStatus.setTextColor(good ? 0xFF37C28D : 0xFF9AA9BC);
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }

    private void flushCookies() {
        try {
            CookieManager.getInstance().flush();
        } catch (Throwable ignore) {
        }
    }

    private static String readAll(InputStream in) throws Exception {
        if (in == null) return "";
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        int total = 0;
        while ((n = in.read(buf)) > 0) {
            total += n;
            if (total > 512 * 1024) break;
            bo.write(buf, 0, n);
        }
        try { in.close(); } catch (Throwable ignore) { }
        return new String(bo.toByteArray(), "UTF-8");
    }

    private static String fmtTime(long ms) {
        if (ms <= 0) return "未知时间";
        java.text.SimpleDateFormat f =
                new java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.CHINA);
        return f.format(new java.util.Date(ms));
    }
}
