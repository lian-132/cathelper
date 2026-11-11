package com.cathelperawa.owo.app.pan;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.cathelperawa.owo.app.bili.BiliUi;
import com.cathelperawa.owo.app.cookie.CookieLoginActivity;
import com.cathelperawa.owo.app.cookie.CookieStore;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public final class PanActivity extends Activity {

    public static final String EXTRA_PLATFORM = "platform";
    
    public static final String EXTRA_OPEN_DOWNLOADS = "open_downloads";
    
    public static final String EXTRA_OPEN_TASK = "open_task";

    private static final int C_MAIN = 0xFFEAF1FB;
    private static final int C_SUB = 0xFF9AA9BC;
    private static final int C_DIM = 0xFF8B9AAD;
    private static final int C_OK = 0xFF37C28D;
    private static final int C_BLUE = 0xFF6EA6FF;

    private FrameLayout root;
    private View mainView;
    private ScrollView settingsScroll;
    private LinearLayout listBox;
    private ScrollView scroll;
    private EditText inputLink, inputPwd;
    private TextView statusLine, loginLine, crumbLine, btnUp, btnParse, btnLogin, btnOpenDir, btnSettings;
    
    private LinearLayout settingsWrap;
    private boolean onSettings = false;
    
    private boolean mineMode = false;
    
    private ScrollView downloadsScroll;
    private LinearLayout downloadsBox;
    private boolean onDownloads = false;
    private final Handler ticker = new Handler(Looper.getMainLooper());
    private final Runnable tickRun = new Runnable() {
        @Override public void run() {
            if (!onDownloads) return;
            try {
                PanDownloadsUi.build(PanActivity.this, downloadsBox);
            } catch (Throwable ignore) {
            }
            ticker.postDelayed(this, 1200L);
        }
    };
    
    private boolean wantDownloads = false;
    
    private String wantOpenTask = null;
    
    private boolean autoEntered = false;
    private LinearLayout chipRow;
    
    private final java.util.List<TextView> chips = new ArrayList<TextView>();
    private static final String[] PLATFORMS = PanProvider.SUPPORTED;

    private String platform = "quark";
    private PanProvider provider;
    private PanProvider.Session session;

    private final List<String> dirFids = new ArrayList<String>();
    private final List<String> dirNames = new ArrayList<String>();
    private final List<PanItem> curItems = new ArrayList<PanItem>();

    

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        if (Build.VERSION.SDK_INT >= 21) {
            getWindow().setStatusBarColor(Color.TRANSPARENT);
            getWindow().setNavigationBarColor(Color.TRANSPARENT);
            try {
                getWindow().getDecorView().setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
            } catch (Throwable ignore) {
            }
        }
        Intent it = getIntent();
        String p = it == null ? null : it.getStringExtra(EXTRA_PLATFORM);
        if (p != null && PanProvider.supported(p)) {
            platform = p;
        } else {
            String def = PanPrefs.platform(this);
            if (PanProvider.supported(def)) platform = def;
        }

        if (it != null && it.getBooleanExtra(EXTRA_OPEN_DOWNLOADS, false)) wantDownloads = true;
        if (it != null) wantOpenTask = it.getStringExtra(EXTRA_OPEN_TASK);

        root = new FrameLayout(this);
        BiliUi.applyBackground(this, root);
        
        View scrim = new View(this);
        scrim.setBackgroundColor(0x8C0B0F16);
        root.addView(scrim, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        setContentView(root);

        final View main = buildColumn();
        mainView = main;
        root.addView(main, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        ScrollView ss = new ScrollView(this);
        ss.setVerticalScrollBarEnabled(false);
        ss.setBackgroundColor(0xF00B0F16);
        ss.setPadding(dp(14), statusBar() + dp(8), dp(14), dp(14));
        settingsWrap = new LinearLayout(this);
        settingsWrap.setOrientation(LinearLayout.VERTICAL);
        ss.addView(settingsWrap, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        ss.setVisibility(View.GONE);
        settingsScroll = ss;

        ScrollView ds = new ScrollView(this);
        ds.setVerticalScrollBarEnabled(false);
        ds.setBackgroundColor(0xF00B0F16);
        ds.setPadding(dp(14), statusBar() + dp(8), dp(14), dp(14));
        downloadsBox = new LinearLayout(this);
        downloadsBox.setOrientation(LinearLayout.VERTICAL);
        ds.addView(downloadsBox, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        ds.setVisibility(View.GONE);
        downloadsScroll = ds;
        root.addView(ds, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        root.addView(ss, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        refreshProvider();
    }

    @Override protected void onResume() {
        super.onResume();
        
        refreshProvider();
        refreshLoginLine();
        fillFromClipboard();
        if (wantOpenTask != null) {
            final String tid = wantOpenTask;
            wantOpenTask = null;
            ticker.postDelayed(new Runnable() {
                @Override public void run() { openTaskNow(tid); }
            }, 80L);
        } else if (wantDownloads) {
            wantDownloads = false;
            ticker.postDelayed(new Runnable() {
                @Override public void run() { showDownloads(); }
            }, 60L);
        }
    }

    @Override protected void onNewIntent(Intent it) {
        super.onNewIntent(it);
        setIntent(it);
        if (it != null && it.getBooleanExtra(EXTRA_OPEN_DOWNLOADS, false)) {
            wantDownloads = true;
        }
        if (it != null && it.getStringExtra(EXTRA_OPEN_TASK) != null) {
            wantOpenTask = it.getStringExtra(EXTRA_OPEN_TASK);
        }
    }

    private void openTaskNow(String tid) {
        PanDl.init(this);
        PanTask target = null;
        for (PanTask t : PanDl.tasks()) {
            if (t.id != null && t.id.equals(tid)) {
                target = t;
                break;
            }
        }
        if (target == null || target.publicUri == null || target.publicUri.length() == 0) {
            showDownloads();
            return;
        }
        PanDlNotif.openFile(this, target);
        showDownloads();
    }

    private void ensureNotifPermission() {
        if (Build.VERSION.SDK_INT < 33) return;
        try {
            if (checkSelfPermission("android.permission.POST_NOTIFICATIONS")
                    == android.content.pm.PackageManager.PERMISSION_GRANTED) return;
            requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 0x3311);
        } catch (Throwable ignore) {
        }
    }

    private void fillFromClipboard() {
        if (!PanPrefs.autoClipboard(this)) return;
        if (inputLink == null || session != null) return;
        if (inputLink.getText() != null && inputLink.getText().toString().trim().length() > 0) return;
        try {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm == null || !cm.hasPrimaryClip() || cm.getPrimaryClip() == null) return;
            CharSequence cs = cm.getPrimaryClip().getItemAt(0).coerceToText(this);
            if (cs == null) return;
            ShareLink sl = ShareLink.parse(cs.toString());
            if (sl == null) return;
            inputLink.setText(cs.toString().trim());
            if (inputPwd != null && inputPwd.getText().length() == 0 && sl.pwd.length() > 0) {
                inputPwd.setText(sl.pwd);
            }
            setStatus("已从剪贴板识别到 " + PanProvider.nameOf(sl.platform) + " 链接，点「解析」", C_OK);
        } catch (Throwable ignore) {
        }
    }

    @Override public void onBackPressed() {
        if (onDownloads) {
            hideDownloads();
            return;
        }
        if (onSettings) {
            hideSettings();
            return;
        }
        if (dirFids.size() > 0) {
            dirFids.remove(dirFids.size() - 1);
            dirNames.remove(dirNames.size() - 1);
            loadDir(currentFid(), true);
            return;
        }
        if (mineMode) {
            mineMode = false;
            curItems.clear();
            if (listBox != null) listBox.removeAllViews();
            if (crumbLine != null) crumbLine.setText("");
            setStatus("已退出网盘目录，粘分享链接可以继续解析别人的分享", C_SUB);
            return;
        }
        super.onBackPressed();
    }

    

    private View buildColumn() {
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(14);
        col.setPadding(pad, statusBar() + dp(8), pad, pad);

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView back = new TextView(this);
        back.setText("‹ 返回");
        back.setTextSize(14f);
        back.setTextColor(C_BLUE);
        back.setPadding(dp(2), dp(6), dp(14), dp(6));
        back.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { finish(); }
        });
        top.addView(back);
        TextView title = new TextView(this);
        title.setText("网盘下载");
        title.setTextSize(20f);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(C_MAIN);
        top.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        btnOpenDir = new TextView(this);
        btnOpenDir.setText("网盘目录");
        btnOpenDir.setTextSize(12f);
        btnOpenDir.setTextColor(C_BLUE);
        btnOpenDir.setPadding(dp(8), dp(6), dp(8), dp(6));
        btnOpenDir.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { openMyDrive(); }
        });
        top.addView(btnOpenDir);

        TextView btnDl = new TextView(this);
        btnDl.setText("下载目录");
        btnDl.setTextSize(12f);
        btnDl.setTextColor(C_BLUE);
        btnDl.setPadding(dp(6), dp(6), dp(8), dp(6));
        btnDl.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { showDownloads(); }
        });
        top.addView(btnDl);

        btnSettings = new TextView(this);
        btnSettings.setText("设置");
        btnSettings.setTextSize(12f);
        btnSettings.setTextColor(C_BLUE);
        btnSettings.setPadding(dp(4), dp(6), dp(2), dp(6));
        btnSettings.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { showSettings(); }
        });
        top.addView(btnSettings);
        col.addView(top);

        LinearLayout account = card();
        account.setOrientation(LinearLayout.HORIZONTAL);
        account.setGravity(Gravity.CENTER_VERTICAL);
        loginLine = new TextView(this);
        loginLine.setTextSize(12.5f);
        loginLine.setTextColor(C_SUB);
        loginLine.setLineSpacing(dp(2), 1f);
        account.addView(loginLine, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        btnLogin = pill("登录", 0x336EA6FF, C_BLUE);
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { openLogin(); }
        });
        account.addView(btnLogin);
        LinearLayout.LayoutParams alp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        alp.topMargin = dp(10);
        col.addView(account, alp);

        chipRow = new LinearLayout(this);
        chipRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        clp.topMargin = dp(8);
        col.addView(chipRow, clp);
        chips.clear();
        for (int i = 0; i < PLATFORMS.length; i++) {
            final String id = PLATFORMS[i];
            TextView chip = pill(PanProvider.shortName(id), 0x33FFFFFF, C_MAIN);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            lp.rightMargin = dp(i == PLATFORMS.length - 1 ? 0 : 4);
            chip.setGravity(Gravity.CENTER);
            chip.setTextSize(11.5f);
            chip.setSingleLine(true);
            chip.setPadding(dp(2), dp(7), dp(2), dp(7));
            chip.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    if (id.equals(platform)) return;
                    platform = id;
                    resetShare();
                    refreshProvider();
                }
            });
            chipRow.addView(chip, lp);
            chips.add(chip);
        }

        LinearLayout input = card();
        input.setOrientation(LinearLayout.VERTICAL);
        inputLink = field("粘贴分享链接或整段分享文案，例：我用夸克网盘分享了「xxx」链接：https://pan.quark.cn/s/xxxx 提取码：abcd");

        inputLink.setSingleLine(false);
        inputLink.setMinLines(2);
        inputLink.setMaxLines(3);
        inputLink.setVerticalScrollBarEnabled(true);
        input.addView(inputLink, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rlp.topMargin = dp(8);
        input.addView(row, rlp);

        TextView btnClear = pill("清空", 0x22FFFFFF, C_SUB);
        btnClear.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (inputLink != null) inputLink.setText("");
                if (inputPwd != null) inputPwd.setText("");
                setStatus("已清空输入", C_DIM);
            }
        });
        LinearLayout.LayoutParams clp0 = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        clp0.leftMargin = dp(8);
        row.addView(btnClear, clp0);

        inputPwd = field("提取码（可选）");
        inputPwd.setSingleLine(true);
        row.addView(inputPwd, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        btnParse = pill("解析", 0xFF2F6BE0, 0xFFFFFFFF);
        btnParse.setTextSize(13f);
        btnParse.setPadding(dp(18), dp(9), dp(18), dp(9));
        btnParse.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { doParse(); }
        });
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        blp.leftMargin = dp(8);
        row.addView(btnParse, blp);

        LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ilp.topMargin = dp(10);
        col.addView(input, ilp);

        statusLine = new TextView(this);
        statusLine.setTextSize(11.5f);
        statusLine.setTextColor(C_DIM);
        statusLine.setLineSpacing(dp(2), 1f);
        statusLine.setText("支持夸克 / UC：粘链接 → 解析 → 点文件就下。游客只能下小文件（夸克约 50MB），大文件请先登录。");
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        slp.topMargin = dp(8);
        slp.leftMargin = dp(4);
        col.addView(statusLine, slp);

        LinearLayout crumb = new LinearLayout(this);
        crumb.setOrientation(LinearLayout.HORIZONTAL);
        crumb.setGravity(Gravity.CENTER_VERTICAL);
        crumbLine = new TextView(this);
        crumbLine.setTextSize(12.5f);
        crumbLine.setTextColor(C_MAIN);
        crumbLine.setSingleLine(true);
        crumbLine.setEllipsize(TextUtils.TruncateAt.MIDDLE);
        crumb.addView(crumbLine, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        btnUp = pill("↑ 上一级", 0x22FFFFFF, C_SUB);
        btnUp.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (dirFids.isEmpty()) {
                    toast("已经在分享根目录了");
                    return;
                }
                dirFids.remove(dirFids.size() - 1);
                dirNames.remove(dirNames.size() - 1);
                loadDir(currentFid(), true);
            }
        });
        crumb.addView(btnUp);
        LinearLayout.LayoutParams crlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        crlp.topMargin = dp(12);
        col.addView(crumb, crlp);

        scroll = new ScrollView(this);
        scroll.setVerticalScrollBarEnabled(false);
        listBox = new LinearLayout(this);
        listBox.setOrientation(LinearLayout.VERTICAL);
        listBox.setPadding(0, dp(8), 0, dp(24));
        scroll.addView(listBox, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        col.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        return col;
    }

    private LinearLayout card() {
        LinearLayout v = new LinearLayout(this);
        v.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(dp(14));
        g.setColor(0xF21A2230);
        g.setStroke(dp(1), 0x33FFFFFF);
        v.setBackground(g);
        int p = dp(12);
        v.setPadding(p, p, p, p);
        return v;
    }

    private TextView pill(String text, int bg, int fg) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(12f);
        tv.setTextColor(fg);
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(dp(20));
        g.setColor(bg);
        tv.setBackground(g);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(dp(12), dp(7), dp(12), dp(7));
        return tv;
    }

    private EditText field(String hint) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setHintTextColor(0xFF5D6B7D);
        e.setTextColor(C_MAIN);
        e.setTextSize(13f);
        e.setBackgroundColor(0x00000000);
        e.setPadding(dp(2), dp(6), dp(2), dp(6));
        return e;
    }

    

    private void refreshProvider() {
        PanProvider.RETRY = PanPrefs.retry(this);
        PanProvider.debug = new PanProvider.DebugSink() {
            @Override public void dump(String tag, String text) {
                try {
                    String line = "\n== " + new java.text.SimpleDateFormat("MM-dd HH:mm:ss", java.util.Locale.CHINA)
                            .format(new java.util.Date()) + " [" + tag + "]\n" + mask(text) + "\n";
                    appendDebug(line);
                } catch (Throwable ignore) {
                }
            }
        };
        provider = PanProvider.create(this, platform);
        if (provider != null) {
            provider.setCookieSink(new PanProvider.CookieSink() {
                @Override public void onCookie(String pf, String newCookie) {
                    CookieStore.Entry e = CookieStore.get(PanActivity.this, pf);
                    CookieStore.save(PanActivity.this, pf, newCookie, e == null ? "" : e.label);
                }
            });
        }
        
        for (int i = 0; i < chips.size() && i < PLATFORMS.length; i++) {
            boolean on = PLATFORMS[i].equals(platform);
            GradientDrawable g = new GradientDrawable();
            g.setCornerRadius(dp(20));
            g.setColor(on ? 0xFF2F6BE0 : 0x33FFFFFF);
            chips.get(i).setBackground(g);
            chips.get(i).setTextColor(on ? 0xFFFFFFFF : C_MAIN);
        }
        refreshLoginLine();
    }

    private void showDownloads() {
        if (downloadsScroll == null) return;
        if (onSettings) hideSettings();
        onDownloads = true;
        renderDownloadsChrome();
        downloadsScroll.setVisibility(View.VISIBLE);
        if (mainView != null) mainView.setVisibility(View.GONE);
        PanDownloadsUi.build(this, downloadsBox);
        ticker.removeCallbacks(tickRun);
        ticker.postDelayed(tickRun, 1200L);
    }

    private void renderDownloadsChrome() {
        downloadsBox.removeAllViews();
        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView back = new TextView(this);
        back.setText("‹ 返回");
        back.setTextSize(14f);
        back.setTextColor(C_BLUE);
        back.setPadding(dp(2), dp(8), dp(16), dp(8));
        back.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { hideDownloads(); }
        });
        top.addView(back);
        TextView title = new TextView(this);
        title.setText("下载目录");
        title.setTextSize(20f);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(C_MAIN);
        top.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        downloadsBox.addView(top);

        TextView tip = new TextView(this);
        tip.setText("这里就是系统下载队列：进行中的显示实时进度，下完点一下直接打开，长按移除任务。\n"
                + "落盘位置 Download/" + PanPrefs.subDir(this) + "/");
        tip.setTextSize(11.5f);
        tip.setTextColor(C_DIM);
        tip.setLineSpacing(dp(3), 1f);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(10);
        downloadsBox.addView(tip, lp);
    }

    private void hideDownloads() {
        onDownloads = false;
        ticker.removeCallbacks(tickRun);
        if (downloadsScroll != null) downloadsScroll.setVisibility(View.GONE);
        if (mainView != null) mainView.setVisibility(View.VISIBLE);
    }

    

    private static final String DEBUG_FILE = "pan_debug.txt";
    private static final int DEBUG_MAX = 200 * 1024;

    private File debugFile() {
        return new File(getFilesDir(), DEBUG_FILE);
    }

    private synchronized String readDebugPlain() {
        File f = debugFile();
        if (!f.exists()) return "";
        java.io.FileInputStream in = null;
        try {
            in = new java.io.FileInputStream(f);
            java.io.ByteArrayOutputStream bo = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) bo.write(buf, 0, n);
            String s = new String(bo.toByteArray(), "UTF-8");
            return Secure.dec(this, s);
        } catch (Throwable t) {
            return "";
        } finally {
            try {
                if (in != null) in.close();
            } catch (Throwable ignore) {
            }
        }
    }

    private synchronized void writeDebugPlain(String all) {
        String body = all == null ? "" : all;
        if (body.length() > DEBUG_MAX) body = body.substring(body.length() - DEBUG_MAX);
        try {
            String enc = Secure.enc(this, body);
            java.io.FileOutputStream os = new java.io.FileOutputStream(debugFile());
            os.write((enc == null ? "" : enc).getBytes("UTF-8"));
            os.flush();
            os.close();
        } catch (Throwable ignore) {
        }
    }

    private synchronized void appendDebug(String line) {
        writeDebugPlain(readDebugPlain() + line);
    }

    public void showDebugLog() {
        final String text = readDebugPlain();
        android.app.Dialog d = new android.app.Dialog(this);
        d.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(18));
        bg.setColor(0xFF141B26);
        bg.setStroke(dp(1), 0x33FFFFFF);
        box.setBackground(bg);
        int pad = dp(14);
        box.setPadding(pad, pad, pad, dp(10));

        TextView title = new TextView(this);
        title.setText("调试日志（已脱敏 · 加密存储）");
        title.setTextSize(15f);
        title.setTextColor(C_MAIN);
        title.setTypeface(null, Typeface.BOLD);
        box.addView(title);

        TextView tv = new TextView(this);
        tv.setText(text.length() == 0 ? "还没有日志。取链/分享失败时会自动记在这里。" : text);
        tv.setTextSize(10.5f);
        tv.setTextColor(0xFFB9C6D6);
        tv.setTextIsSelectable(true);
        tv.setLineSpacing(dp(2), 1f);
        ScrollView sc = new ScrollView(this);
        sc.setVerticalScrollBarEnabled(false);
        sc.addView(tv);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(320));
        slp.topMargin = dp(8);
        box.addView(sc, slp);

        LinearLayout btns = new LinearLayout(this);
        btns.setOrientation(LinearLayout.HORIZONTAL);
        btns.setGravity(Gravity.END);
        TextView clear = pill("清空", 0x33FF6B6B, 0xFFFF8A80);
        TextView close = pill("关闭", 0xFF2F6BE0, 0xFFFFFFFF);
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        blp.leftMargin = dp(8);
        blp.topMargin = dp(10);
        btns.addView(clear);
        btns.addView(close, blp);
        box.addView(btns);

        final android.app.Dialog dd = d;
        clear.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                clearDebugLog();
                dd.dismiss();
            }
        });
        close.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { dd.dismiss(); }
        });

        d.setContentView(box);
        android.view.Window w = d.getWindow();
        if (w != null) {
            w.setBackgroundDrawableResource(android.R.color.transparent);
            w.setGravity(Gravity.CENTER);
            android.view.WindowManager.LayoutParams lp = new android.view.WindowManager.LayoutParams();
            lp.copyFrom(w.getAttributes());
            lp.width = getResources().getDisplayMetrics().widthPixels - dp(28);
            lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            lp.gravity = Gravity.CENTER;
            w.setAttributes(lp);
        }
        d.show();
    }

    public void clearDebugLog() {
        try {
            debugFile().delete();
        } catch (Throwable ignore) {
        }
        toast("调试日志已清空");
    }

    private static String mask(String text) {
        if (text == null) return "";
        String t = text;
        try {
            
            t = t.replaceAll("(1[3-9]\\d)\\d{4}(\\d{4})", "$1****$2");
            
            t = t.replaceAll("(?i)(access_?token|refresh_?token|accessToken|refreshToken|BDUSS|__puus|__pus|__pugs|authorization)(=)([^;&\"\\s]{6,})", "$1=***");
            t = t.replaceAll("(?i)\"(authorization|access_token|refresh_token|BDUSS)\"\\s*:\\s*\"[^\"]*\"", "\"$1\":\"***\"");
            
            t = t.replaceAll("(?i)(pwd|passwd|pass_code|passcode)(=|\":\")([A-Za-z0-9]{3,8})", "$1$2***");
        } catch (Throwable ignore) {
        }
        return t;
    }

    private void showSettings() {
        if (settingsWrap == null || settingsScroll == null) return;
        if (onDownloads) hideDownloads();
        onSettings = true;
        PanSettingsUi.build(this, settingsWrap, new PanSettingsUi.OnChange() {
            @Override public void changed() {
                refreshProvider();
            }
        });
        settingsScroll.scrollTo(0, 0);
        settingsScroll.setVisibility(View.VISIBLE);
        settingsScroll.post(new Runnable() {
            @Override public void run() { settingsScroll.scrollTo(0, 0); }
        });
        if (mainView != null) mainView.setVisibility(View.GONE);
    }

    public void hideSettings() {
        onSettings = false;
        if (settingsScroll != null) settingsScroll.setVisibility(View.GONE);
        if (mainView != null) mainView.setVisibility(View.VISIBLE);
        refreshProvider();
    }

    private void refreshLoginLine() {
        if (loginLine == null) return;
        CookieStore.Entry e = CookieStore.get(this, platform);
        String name = PanProvider.nameOf(platform);
        boolean has = e != null && e.valid();
        
        if ("lanzou".equals(platform)) {
            loginLine.setText(name + "：分享链接可直接解析，不需要登录（个人盘要登就点右边）");
            loginLine.setTextColor(C_SUB);
            btnLogin.setText("登录");
            return;
        }

        
        
        if (has) {
            String need = null;
            if ("baidu".equals(platform)) need = "BDUSS=";
            else if ("pan123".equals(platform)) need = "authorToken=";
            else if ("xunlei".equals(platform)) need = "accessToken=";
            else if ("c139".equals(platform)) need = "authorization=";
            else if ("lanzou".equals(platform)) need = "phpdisk_info=";
            if (need != null && !e.cookie.contains(need)) {
                CookieStore.remove(this, platform);
                e = CookieStore.get(this, platform);
                has = e != null && e.valid();
            }
        }
        boolean acct = false;
        if (has) {
            if ("baidu".equals(platform)) acct = e.cookie.contains("BDUSS=");
            else if ("pan123".equals(platform)) acct = e.cookie.contains("authorToken=");
            else if ("xunlei".equals(platform)) acct = e.cookie.contains("accessToken=");
            else if ("c139".equals(platform)) acct = e.cookie.contains("authorization=");
            else acct = e.cookie.contains("__puus=") || e.cookie.contains("__pus=");
        }
        if (acct) {
            String label = e.label == null || e.label.length() == 0 ? "已登录" : "已登录 · " + e.label;
            loginLine.setText(name + "：" + label + "（大文件可下）");
            loginLine.setTextColor(C_OK);
            btnLogin.setText("重登");
        } else if (has) {
            
            loginLine.setText(name + "：之前只存到了游客 Cookie（没登录成），点「登录」重新来一次");
            loginLine.setTextColor(C_SUB);
            btnLogin.setText("登录");
        } else {
            loginLine.setText("123".equals(platform) || "baidu".equals(platform)
                    || "xunlei".equals(platform) || "c139".equals(platform)
                    ? name + "：未登录（这家的直链必须登录才给）"
                    : name + "：游客模式（只能下小文件）");
            loginLine.setTextColor(C_SUB);
            btnLogin.setText("登录");
        }
    }

    private void setStatus(String text, int color) {
        if (statusLine == null) return;
        statusLine.setText(text);
        statusLine.setTextColor(color);
    }

    private void openLogin() {
        try {
            Intent it = new Intent(this, CookieLoginActivity.class);
            it.putExtra(CookieLoginActivity.EXTRA_PLATFORM, platform);
            startActivity(it);
        } catch (Throwable t) {
            toast("打开登录页失败：" + t);
        }
    }

    private void openMyDrive() {
        if (provider == null) {
            toast("这个平台还没接入");
            return;
        }
        if (!provider.canMine()) {
            setStatus("看自己的网盘目录要先登录" + PanProvider.nameOf(platform)
                    + "：点上面的「登录」，登完再点「网盘目录」", 0xFFFFC069);
            openLogin();
            return;
        }
        mineMode = true;
        dirFids.clear();
        dirNames.clear();
        autoEntered = true;      
        loadDir("0", true);
    }

    

    private String currentFid() {
        return dirFids.isEmpty() ? "0" : dirFids.get(dirFids.size() - 1);
    }

    private void resetShare() {
        session = null;
        dirFids.clear();
        dirNames.clear();
        curItems.clear();
        if (listBox != null) listBox.removeAllViews();
        if (crumbLine != null) crumbLine.setText("");
    }

    private void doParse() {
        if (provider == null) {
            toast("这个平台还没接入");
            return;
        }
        String text = inputLink.getText() == null ? "" : inputLink.getText().toString().trim();
        if (text.length() == 0) {
            toast("先把分享链接粘进来");
            return;
        }
        final ShareLink share = ShareLink.parse(text);
        if (share == null) {
            setStatus("没认出来这是哪家的分享链接。目前支持：夸克 pan.quark.cn/s/… 、UC drive.uc.cn/s/…", 0xFFFF8A80);
            return;
        }
        if (!PanProvider.supported(share.platform)) {
            setStatus(PanProvider.nameOf(share.platform) + " 还没接入（先做了夸克 / UC），这个链接可以先存着", C_DIM);
            return;
        }
        
        if (!share.platform.equals(platform)) {
            platform = share.platform;
            resetShare();
            refreshProvider();
        }
        mineMode = false;
        String typedPwd = inputPwd.getText() == null ? "" : inputPwd.getText().toString().trim();
        final String pwd = typedPwd.length() > 0 ? typedPwd : share.pwd;

        hideKeyboard();
        setStatus("正在打开分享…", C_SUB);
        btnParse.setEnabled(false);
        final PanProvider p = provider;
        PanProvider.run(new PanProvider.Task<PanProvider.Session>() {
            @Override public PanProvider.Session call() throws Exception {
                return p.open(share.shareId, pwd);
            }
        }, new PanProvider.Cb<PanProvider.Session>() {
            @Override public void ok(PanProvider.Session s) {
                btnParse.setEnabled(true);
                session = s;
                autoEntered = false;
                dirFids.clear();
                dirNames.clear();
                if (inputPwd.getText().length() == 0 && pwd.length() > 0) inputPwd.setText(pwd);
                setStatus("已打开：" + (s.title == null || s.title.length() == 0 ? share.shareId : s.title)
                        + "　点目录进去，点文件下载", C_OK);
                loadDir("0", false);
            }

            @Override public void fail(String msg) {
                btnParse.setEnabled(true);
                setStatus("打开失败：" + msg, 0xFFFF8A80);
            }
        });
    }

    private void loadDir(final String fid, final boolean scrollTop) {
        
        if (session == null && !mineMode) return;
        setStatus("读取列表中…", C_SUB);
        final PanProvider p = provider;
        final PanProvider.Session s = session;
        if (mineMode) setStatus("正在读我的网盘…", C_SUB);
        PanProvider.run(new PanProvider.Task<List<PanItem>>() {
            @Override public List<PanItem> call() throws Exception {
                return mineMode ? p.listMine(fid) : p.list(s, fid);
            }
        }, new PanProvider.Cb<List<PanItem>>() {
            @Override public void ok(List<PanItem> items) {
                curItems.clear();
                
                if (!autoEntered && dirFids.isEmpty() && PanPrefs.autoEnterDir(PanActivity.this)
                        && items != null && items.size() == 1 && items.get(0).dir) {
                    autoEntered = true;
                    PanItem only = items.get(0);
                    dirFids.add(only.fid);
                    dirNames.add(only.name);
                    loadDir(only.fid, true);
                    return;
                }
                if (items != null) curItems.addAll(items);
                renderList();
                if (scrollTop && scroll != null) scroll.scrollTo(0, 0);
                setStatus("共 " + curItems.size() + " 项 · 点目录进去，点文件下载", C_SUB);
            }

            @Override public void fail(String msg) {
                setStatus("读取失败：" + msg, 0xFFFF8A80);
            }
        });
    }

    private void renderList() {
        listBox.removeAllViews();
        String crumb;
        if (mineMode) {
            crumb = "我的网盘（" + PanProvider.nameOf(platform) + "）";
        } else {
            crumb = session == null ? "" : (session.title == null || session.title.length() == 0 ? "分享" : session.title);
        }
        if (!dirNames.isEmpty()) {
            StringBuilder sb = new StringBuilder(crumb);
            for (String n : dirNames) sb.append(" / ").append(n);
            crumb = sb.toString();
        }
        crumbLine.setText(crumb);
        btnUp.setAlpha(dirFids.isEmpty() ? 0.4f : 1f);

        if (curItems.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("这里没有文件（或者这个分享是个单文件，回上一级看看）");
            empty.setTextSize(12f);
            empty.setTextColor(C_DIM);
            empty.setPadding(dp(6), dp(18), dp(6), dp(6));
            listBox.addView(empty);
            return;
        }
        for (final PanItem item : curItems) {
            listBox.addView(row(item));
        }
    }

    private View row(final PanItem item) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(dp(12));
        g.setColor(0xE8161D28);
        g.setStroke(dp(1), 0x2AFFFFFF);
        card.setBackground(g);
        int p = dp(11);
        card.setPadding(p, p, p, p);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(7);
        card.setLayoutParams(lp);

        TextView icon = new TextView(this);
        icon.setText(item.dir ? "夹" : "档");
        icon.setTextSize(12f);
        icon.setGravity(Gravity.CENTER);
        icon.setTextColor(0xFFFFFFFF);
        GradientDrawable ib = new GradientDrawable();
        ib.setCornerRadius(dp(9));
        ib.setColor(item.dir ? 0xFF2F6BE0 : 0xFF3A4756);
        icon.setBackground(ib);
        int size = dp(30);
        LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(size, size);
        ilp.rightMargin = dp(10);
        card.addView(icon, ilp);

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        card.addView(texts, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView name = new TextView(this);
        name.setText(item.name);
        name.setTextSize(13f);
        name.setTextColor(C_MAIN);
        name.setMaxLines(2);
        name.setEllipsize(TextUtils.TruncateAt.MIDDLE);
        texts.addView(name);
        TextView sizeTv = new TextView(this);
        sizeTv.setText(item.sizeText() + (item.dir ? "　点进去" : "　点一下下载"));
        sizeTv.setTextSize(11f);
        sizeTv.setTextColor(C_DIM);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        slp.topMargin = dp(2);
        texts.addView(sizeTv, slp);

        card.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (item.dir) {
                    dirFids.add(item.fid);
                    dirNames.add(item.name);
                    loadDir(item.fid, true);
                } else {
                    download(item);
                }
            }
        });
        card.setOnLongClickListener(new View.OnLongClickListener() {
            @Override public boolean onLongClick(View v) {
                if (mineMode) showItemMenu(item);
                else shareLink(item);
                return true;
            }
        });
        return card;
    }

    private void shareOriginal() {
        try {
            String title = session == null || session.title == null || session.title.length() == 0
                    ? "一个文件" : session.title;
            String url;
            if ("quark".equals(platform)) {
                url = "https://pan.quark.cn/s/" + session.shareId;
            } else if ("uc".equals(platform)) {
                url = "https://drive.uc.cn/s/" + session.shareId;
            } else {
                url = "";
            }
            if (url.length() == 0) {
                toast("这个平台还没做分享链接");
                return;
            }
            String pwd = session.pwd == null ? "" : session.pwd;
            if (pwd.length() > 0) url = url + "?pwd=" + pwd;
            String text = "「" + title + "」\n链接：" + url
                    + (pwd.length() > 0 ? "\n提取码：" + pwd : "");
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null) cm.setPrimaryClip(ClipData.newPlainText("分享链接", text));
            Intent send = new Intent(Intent.ACTION_SEND);
            send.setType("text/plain");
            send.putExtra(Intent.EXTRA_SUBJECT, title);
            send.putExtra(Intent.EXTRA_TEXT, text);
            startActivity(Intent.createChooser(send, "分享网盘链接"));
            setStatus("已分享原分享链接（也复制到剪贴板了）", C_OK);
        } catch (Throwable t) {
            setStatus("分享失败：" + t, 0xFFFF8A80);
        }
    }

    private void download(final PanItem item) {
        if (provider == null || (session == null && !mineMode)) return;
        
        if (!mineMode && provider instanceof QuarkPan && !((QuarkPan) provider).loggedIn()
                && item.size > 50L * 1024 * 1024 && PanPrefs.askGuest(this)) {
            setStatus("游客模式下这个文件 " + PanItem.human(item.size)
                    + " 超过夸克约 50MB 的上限，很可能被拒。想下大文件请点右上「登录」。\n还是先试一次…", 0xFFFFC069);
        } else {
            setStatus("正在取直链：" + item.name, C_SUB);
        }
        final PanProvider p = provider;
        final PanProvider.Session s = session;
        final boolean mine = mineMode;
        com.cathelperawa.owo.app.core.Bg.run(new Runnable() {
            @Override public void run() {
                try {
                    final PanProvider.Link link = mine ? p.linkMine(item) : p.link(s, item);
                    startDownload(link);
                } catch (final PanProvider.BrowserNeeded bn) {
                    
                    ticker.post(new Runnable() {
                        @Override public void run() {
                            setStatus("下载节点要过人机校验，正在用隐藏浏览器过…（几秒）", C_SUB);
                            HiddenWebView.resolve(PanActivity.this, bn.url, bn.referer,
                                    new HiddenWebView.Cb() {
                                @Override public void ok(String finalUrl) {
                                    PanProvider.Link l = new PanProvider.Link();
                                    l.url = finalUrl;
                                    l.filename = item.name;
                                    l.size = item.size;
                                    l.ua = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                                            + "(KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36";
                                    l.referer = bn.referer;
                                    l.cookie = "down_ip=1";
                                    startDownload(l);
                                }

                                @Override public void fail(String msg) {
                                    setStatus("过校验失败：" + msg, 0xFFFF8A80);
                                }
                            });
                        }
                    });
                } catch (final Throwable t) {
                    ticker.post(new Runnable() {
                        @Override public void run() {
                            setStatus("取直链失败：" + (t.getMessage() == null ? t.toString() : t.getMessage()),
                                    0xFFFF8A80);
                        }
                    });
                }
            }
        }, "pan-link");
    }

    private void startDownload(final PanProvider.Link link) {
        ticker.post(new Runnable() {
            @Override public void run() {
                try {
                    ensureNotifPermission();
                    PanDl.enqueue(PanActivity.this, link, link.filename);
                    String extra = link.guest ? "（游客链，大文件可能中途 403）" : "";
                    setStatus("开始下载：" + link.filename + " · " + PanItem.human(link.size) + extra
                            + "\n" + PanPrefs.threads(PanActivity.this) + " 线程分片 · 进度看「下载目录」", C_OK);
                    showDownloads();
                } catch (Throwable t) {
                    setStatus("加下载失败：" + t.getMessage(), 0xFFFF8A80);
                }
            }
        });
    }

    

    private void showItemMenu(final PanItem item) {
        if (provider == null) return;
        final List<String> ops = new ArrayList<String>();
        final List<Boolean> danger = new ArrayList<Boolean>();
        if (!item.dir) { ops.add("下载这个文件"); danger.add(false); }
        ops.add("分享链接"); danger.add(false);

        
        if (provider.canManage()) {
            ops.add("重命名"); danger.add(false);
        }
        final Act[] acts = new Act[ops.size()];
        for (int i = 0; i < ops.size(); i++) {
            final String op = ops.get(i);
            acts[i] = new Act() {
                @Override public void run() {
                    if (op.startsWith("下载")) download(item);
                    else if ("分享链接".equals(op)) shareLink(item);
                    else if ("重命名".equals(op)) renameItem(item);
                    else if ("删除".equals(op)) deleteItem(item);
                }
            };
        }
        boolean[] dz = new boolean[danger.size()];
        for (int i = 0; i < dz.length; i++) dz[i] = danger.get(i);
        String sub = (item.dir ? "文件夹" : PanItem.human(item.size))
                + (provider.canManage() ? "　·　长按可管理" : "");
        PanSheet.menu(this, item.name, sub, ops.toArray(new String[0]), dz, acts);
    }

    private interface Act extends PanSheet.Act {
    }

    private void renameItem(final PanItem item) {
        final PanProvider p = provider;
        if (p == null) return;
        PanSheet.input(this, "重命名", "新的名字", item.name, new PanSheet.TextAct() {
            @Override public void run(final String nn) {
                if (nn == null || nn.length() == 0) {
                    toast("名字不能为空");
                    return;
                }
                if (nn.equals(item.name)) return;
                setStatus("正在改名…", C_SUB);
                PanProvider.run(new PanProvider.Task<Void>() {
                    @Override public Void call() throws Exception {
                        p.renameMine(item, nn);
                        return null;
                    }
                }, new PanProvider.Cb<Void>() {
                    @Override public void ok(Void v) {
                        setStatus("已改名：" + nn, C_OK);
                        reloadCurrentDir();
                    }

                    @Override public void fail(String msg) {
                        setStatus("改名失败：" + msg, 0xFFFF8A80);
                    }
                });
            }
        });
    }

    private void deleteItem(final PanItem item) {
        final PanProvider p = provider;
        if (p == null) return;
        final String parentFid = dirFids.isEmpty() ? "0" : dirFids.get(dirFids.size() - 1);
        String msg = (item.dir ? "文件夹里的东西会一起进回收站。" : "")
                + "删掉后在网盘 App 的「回收站」里还能找回。";
        PanSheet.confirm(this, "删除「" + item.name + "」？", msg, "删除", true, new PanSheet.Act() {
            @Override public void run() {
                setStatus("正在删除…", C_SUB);
                PanProvider.run(new PanProvider.Task<Void>() {
                    @Override public Void call() throws Exception {
                        p.deleteMine(item, parentFid);
                        return null;
                    }
                }, new PanProvider.Cb<Void>() {
                    @Override public void ok(Void v) {
                        toast("已删除：" + item.name);
                        setStatus("已删除：" + item.name + "（网盘回收站里可找回）", C_OK);
                        reloadCurrentDir();
                    }

                    @Override public void fail(String msg) {
                        setStatus("删除失败：" + msg, 0xFFFF8A80);
                    }
                });
            }
        });
    }

    private void reloadCurrentDir() {
        String fid = dirFids.isEmpty() ? "0" : dirFids.get(dirFids.size() - 1);
        loadDir(fid, false);
    }

    private void shareLink(final PanItem item) {
        if (provider == null || (session == null && !mineMode)) return;
        if (!mineMode && session != null) {
            shareOriginal();
            return;
        }
        setStatus("正在给你的文件创建分享…（网盘侧要处理几秒）", C_SUB);
        final PanProvider p = provider;
        PanProvider.run(new PanProvider.Task<PanProvider.ShareInfo>() {
            @Override public PanProvider.ShareInfo call() throws Exception {
                return p.createShare(item, "", 1);
            }
        }, new PanProvider.Cb<PanProvider.ShareInfo>() {
            @Override public void ok(PanProvider.ShareInfo si) {
                shareText(si.title == null || si.title.length() == 0 ? item.name : si.title,
                        si.url, si.pwd);
                setStatus("分享链接已生成（也复制到剪贴板了）：" + si.url, C_OK);
            }

            @Override public void fail(String msg) {
                setStatus("创建分享失败：" + msg, 0xFFFF8A80);
            }
        });
    }

    private void shareText(String title, String url, String pwd) {
        try {
            String text = "「" + title + "」\n链接：" + url
                    + (pwd == null || pwd.length() == 0 ? "" : "\n提取码：" + pwd);
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null) cm.setPrimaryClip(ClipData.newPlainText("分享链接", text));
            Intent send = new Intent(Intent.ACTION_SEND);
            send.setType("text/plain");
            send.putExtra(Intent.EXTRA_SUBJECT, title);
            send.putExtra(Intent.EXTRA_TEXT, text);
            startActivity(Intent.createChooser(send, "分享网盘链接"));
        } catch (Throwable t) {
            setStatus("分享失败：" + t, 0xFFFF8A80);
        }
    }

    

    private void hideKeyboard() {
        try {
            InputMethodManager im = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            View f = getCurrentFocus();
            if (im != null && f != null) im.hideSoftInputFromWindow(f.getWindowToken(), 0);
        } catch (Throwable ignore) {
        }
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }

    private int dp(float v) {
        
        return com.cathelperawa.owo.app.ui.UiKit.dp(this, v);
    }

    private int statusBar() {
        try {
            int id = getResources().getIdentifier("status_bar_height", "dimen", "android");
            return id > 0 ? getResources().getDimensionPixelSize(id) : dp(24);
        } catch (Throwable t) {
            return dp(24);
        }
    }
}
