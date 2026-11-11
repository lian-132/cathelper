package com.cathelperawa.owo.app.pan;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.cathelperawa.owo.app.cookie.CookiePlatforms;
import com.cathelperawa.owo.app.cookie.CookieStore;

public final class PanSettingsUi {

    private PanSettingsUi() {}

    private static String openKey = null;

    private static final int C_MAIN = 0xFFEAF1FB;
    private static final int C_SUB = 0xFF9AA9BC;
    private static final int C_DIM = 0xFF8B9AAD;
    private static final int C_OK = 0xFF37C28D;
    private static final int C_ACCENT = 0xFF6EA6FF;

    public interface OnChange {
        void changed();
    }

    public static void build(final Activity a, LinearLayout box, final OnChange onChange) {
        if (a == null || box == null) return;
        box.removeAllViews();

        LinearLayout top = new LinearLayout(a);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView back = new TextView(a);
        back.setText("‹ 返回");
        back.setTextSize(14f);
        back.setTextColor(C_ACCENT);
        back.setPadding(dp(a, 2), dp(a, 8), dp(a, 16), dp(a, 8));
        back.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (a instanceof PanActivity) ((PanActivity) a).hideSettings();
            }
        });
        top.addView(back);
        TextView title = new TextView(a);
        title.setText("网盘设置");
        title.setTextSize(20f);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(C_MAIN);
        top.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        box.addView(top);

        box.addView(hint(a, "这些设置存在本应用里，改完立刻生效。「已接入」的平台才有解析器。", C_SUB));

        box.addView(group(a, "解析"));
        LinearLayout c1 = card(a);
        c1.addView(row(a, "默认平台", "进网盘页时默认停在哪个网盘",
                panName(PanPrefs.platform(a)) + ("platform".equals(openKey) ? "  ▴" : "  ▾"),
                new Runnable() {
                    @Override public void run() {
                        openKey = "platform".equals(openKey) ? null : "platform";
                        build(a, box, onChange);
                    }
                }));
        if ("platform".equals(openKey)) {
            final String[] ids = PanProvider.SUPPORTED;
            String[] names = new String[ids.length];
            for (int i = 0; i < ids.length; i++) names[i] = PanProvider.nameOf(ids[i]);
            int cur = 0;
            for (int i = 0; i < ids.length; i++) if (ids[i].equals(PanPrefs.platform(a))) cur = i;
            c1.addView(inlinePick(a, names, cur, new OnPick() {
                @Override public void onPick(int idx) {
                    PanPrefs.setPlatform(a, ids[idx]);
                    openKey = null;
                    if (onChange != null) onChange.changed();
                    build(a, box, onChange);
                }
            }));
        }
        c1.addView(divider(a));
        c1.addView(toggleRow(a, "自动识别剪贴板", "进页面时，剪贴板里是分享链接就自动填进输入框",
                PanPrefs.autoClipboard(a), new Toggle() {
                    @Override public void onToggle(boolean v) {
                        PanPrefs.setAutoClipboard(a, v);
                        build(a, box, onChange);
                    }
                }));
        c1.addView(divider(a));
        c1.addView(toggleRow(a, "单文件夹自动进入", "分享里只有一个文件夹时，解析完直接进去",
                PanPrefs.autoEnterDir(a), new Toggle() {
                    @Override public void onToggle(boolean v) {
                        PanPrefs.setAutoEnterDir(a, v);
                        build(a, box, onChange);
                    }
                }));
        c1.addView(divider(a));
        c1.addView(row(a, "失败自动重试", "网络抖动时自动重来几次（每次都会重新取链）",
                (PanPrefs.retry(a) == 0 ? "不重试" : PanPrefs.retry(a) + " 次")
                        + ("retry".equals(openKey) ? "  ▴" : "  ▾"),
                new Runnable() {
                    @Override public void run() {
                        openKey = "retry".equals(openKey) ? null : "retry";
                        build(a, box, onChange);
                    }
                }));
        if ("retry".equals(openKey)) {
            c1.addView(inlinePick(a, new String[]{"不重试", "重试 1 次", "重试 2 次", "重试 3 次"},
                    PanPrefs.retry(a), new OnPick() {
                        @Override public void onPick(int idx) {
                            PanPrefs.setRetry(a, idx);
                            openKey = null;
                            build(a, box, onChange);
                        }
                    }));
        }
        box.addView(c1);

        box.addView(group(a, "下载"));
        LinearLayout c2 = card(a);
        c2.addView(row(a, "下载保存目录", "系统下载器的落盘位置",
                "Download/" + PanPrefs.subDir(a) + "/" + ("subDir".equals(openKey) ? "  ▴" : "  ▾"),
                new Runnable() {
                    @Override public void run() {
                        openKey = "subDir".equals(openKey) ? null : "subDir";
                        build(a, box, onChange);
                    }
                }));
        if ("subDir".equals(openKey)) {
            c2.addView(inlineInput(a, PanPrefs.subDir(a), new OnInput() {
                @Override public void onInput(String v) {
                    PanPrefs.setSubDir(a, v);
                    openKey = null;
                    build(a, box, onChange);
                }
            }));
        }
        c2.addView(divider(a));
        c2.addView(row(a, "下载线程数", "同一文件切几段并行拉；越多越快也越容易被风控限速",
                PanPrefs.threads(a) + " 线程"
                        + (("threads".equals(openKey) || "threadsCustom".equals(openKey)) ? "  ▴" : "  ▾"),
                new Runnable() {
                    @Override public void run() {
                        openKey = "threads".equals(openKey) ? null : "threads";
                        build(a, box, onChange);
                    }
                }));
        if ("threads".equals(openKey) || "threadsCustom".equals(openKey)) {
            final int[] vals = {1, 2, 4, 8, 16, 32, 64, 128, 256, 512, 1024, 0};
            String[] names = new String[vals.length];
            int cur = 0;
            int nowThreads = PanPrefs.threads(a);
            boolean matched = false;
            for (int i = 0; i < vals.length; i++) {
                names[i] = vals[i] == 0 ? "自定义…" : vals[i] + " 线程";
                if (vals[i] != 0 && vals[i] == nowThreads) {
                    cur = i;
                    matched = true;
                }
            }
            if (!matched || "threadsCustom".equals(openKey)) cur = vals.length - 1;
            c2.addView(inlinePick(a, names, cur, new OnPick() {
                @Override public void onPick(int idx) {
                    if (vals[idx] == 0) {
                        
                        openKey = "threadsCustom";
                        build(a, box, onChange);
                    } else {
                        PanPrefs.setThreads(a, vals[idx]);
                        openKey = null;
                        build(a, box, onChange);
                    }
                }
            }));
            if ("threadsCustom".equals(openKey)) {
                c2.addView(inlineInput(a, String.valueOf(PanPrefs.threads(a)), new OnInput() {
                    @Override public void onInput(String v) {
                        int n = 8;
                        try {
                            n = Integer.parseInt(v.trim().replaceAll("[^0-9]", ""));
                        } catch (Throwable ignore) {
                        }
                        PanPrefs.setThreads(a, Math.max(1, n));
                        openKey = null;
                        build(a, box, onChange);
                    }
                }));
            }
        }
        c2.addView(divider(a));
        
        c2.addView(row(a, "最大同时下载任务数", "同时跑几个任务，超出的排队等待",
                PanPrefs.maxConcurrent(a) + " 个"
                        + (("maxTask".equals(openKey) || "maxTaskCustom".equals(openKey)) ? "  ▴" : "  ▾"),
                new Runnable() {
                    @Override public void run() {
                        openKey = "maxTask".equals(openKey) ? null : "maxTask";
                        build(a, box, onChange);
                    }
                }));
        if ("maxTask".equals(openKey) || "maxTaskCustom".equals(openKey)) {
            final int[] vals = {1, 2, 3, 5, 8, 0};
            String[] names = new String[vals.length];
            int nowMax = PanPrefs.maxConcurrent(a);
            boolean matched = false;
            int cur = vals.length - 1;
            for (int i = 0; i < vals.length; i++) {
                names[i] = vals[i] == 0 ? "自定义…" : "同时下载 " + vals[i] + " 个任务";
                if (vals[i] != 0 && vals[i] == nowMax) {
                    cur = i;
                    matched = true;
                }
            }
            if (!matched || "maxTaskCustom".equals(openKey)) cur = vals.length - 1;
            c2.addView(inlinePick(a, names, cur, new OnPick() {
                @Override public void onPick(int idx) {
                    if (vals[idx] == 0) {
                        openKey = "maxTaskCustom";
                        build(a, box, onChange);
                    } else {
                        PanPrefs.setMaxConcurrent(a, vals[idx]);
                        openKey = null;
                        build(a, box, onChange);
                    }
                }
            }));
            if ("maxTaskCustom".equals(openKey)) {
                c2.addView(inlineInput(a, String.valueOf(PanPrefs.maxConcurrent(a)), new OnInput() {
                    @Override public void onInput(String v) {
                        int n = 3;
                        try {
                            n = Integer.parseInt(v.trim().replaceAll("[^0-9]", ""));
                        } catch (Throwable ignore) {
                        }
                        PanPrefs.setMaxConcurrent(a, Math.max(1, n));
                        openKey = null;
                        build(a, box, onChange);
                    }
                }));
            }
        }

        c2.addView(divider(a));
        
        int kb = PanPrefs.speedLimitKb(a);
        String speedText = kb <= 0 ? "不限速" : (kb >= 1024
                ? String.format(java.util.Locale.CHINA, "%.1f MB/s", kb / 1024.0)
                : kb + " KB/s");
        c2.addView(row(a, "下载速度限制", "所有任务一起分摊这个额度；不限速就填 0",
                speedText + (("speed".equals(openKey) || "speedCustom".equals(openKey)) ? "  ▴" : "  ▾"),
                new Runnable() {
                    @Override public void run() {
                        openKey = "speed".equals(openKey) ? null : "speed";
                        build(a, box, onChange);
                    }
                }));
        if ("speed".equals(openKey) || "speedCustom".equals(openKey)) {
            final int[] vals = {0, 1024, 2048, 5120, 10240, -1};
            String[] names = {"不限速", "1.0 MB/s", "2.0 MB/s", "5.0 MB/s", "10 MB/s", "自定义 KB/s…"};
            int cur = vals.length - 1;
            int nowKb = PanPrefs.speedLimitKb(a);
            for (int i = 0; i < vals.length; i++) {
                if (vals[i] >= 0 && vals[i] == nowKb) {
                    cur = i;
                    break;
                }
            }
            if ("speedCustom".equals(openKey)) cur = vals.length - 1;
            c2.addView(inlinePick(a, names, cur, new OnPick() {
                @Override public void onPick(int idx) {
                    if (vals[idx] == -1) {
                        openKey = "speedCustom";
                        build(a, box, onChange);
                    } else {
                        PanPrefs.setSpeedLimitKb(a, vals[idx]);
                        openKey = null;
                        build(a, box, onChange);
                    }
                }
            }));
            if ("speedCustom".equals(openKey)) {
                c2.addView(inlineInput(a, String.valueOf(Math.max(0, PanPrefs.speedLimitKb(a))), new OnInput() {
                    @Override public void onInput(String v) {
                        int n = 0;
                        try {
                            n = Integer.parseInt(v.trim().replaceAll("[^0-9]", ""));
                        } catch (Throwable ignore) {
                        }
                        PanPrefs.setSpeedLimitKb(a, Math.max(0, n));
                        openKey = null;
                        build(a, box, onChange);
                    }
                }));
            }
        }

        c2.addView(divider(a));
        c2.addView(infoRow(a, "下载引擎", "内置分片下载器：按线程数切 Range 并行、支持断点续传与取消；"
                + "下完自动搬到公共 Download 目录。旧版本用系统下载器下过的东西还在通知栏/系统下载里。"));
        c2.addView(divider(a));
        c2.addView(toggleRow(a, "游客模式提醒", "游客取链前提示「夸克约 50MB 上限」",
                PanPrefs.askGuest(a), new Toggle() {
                    @Override public void onToggle(boolean v) {
                        PanPrefs.setAskGuest(a, v);
                        build(a, box, onChange);
                    }
                }));
        box.addView(c2);

        box.addView(group(a, "平台状态"));
        LinearLayout c3 = card(a);
        boolean first = true;
        for (CookiePlatforms.P p : CookiePlatforms.byGroup("网盘")) {
            if (!first) c3.addView(divider(a));
            first = false;
            c3.addView(platformRow(a, p, box, onChange));
        }
        box.addView(c3);

        box.addView(group(a, "说明"));
        LinearLayout c4 = card(a);
        c4.addView(infoRow(a, "取链方式", "照抄各家网页版接口换直链：夸克 / UC 走 drive-pc 那套，"
                + "不转存、不破解；登录态用「网页登录」里存的 Cookie，游客态也能下小文件。"));
        c4.addView(divider(a));
        c4.addView(infoRow(a, "登录态维护", "夸克 / UC 的 __puus 约 3 小时过期，取链时服务端下发的新值"
                + "会自动合并回去并落盘，过期了在网盘页点「重登」即可。"));
        c4.addView(divider(a));
        c4.addView(infoRow(a, "凭证怎么存", "登录态在 cookies.json 里是 AES-256-GCM 密文（v1: 前缀）；"
                + "下载任务里的直链/Cookie、调试日志也都是密文，日志另外做了手机号和 token 脱敏。"));
        c4.addView(divider(a));
        c4.addView(toggleRow(a, "保存后清除 WebView Cookie",
                "登录态存进加密库后，顺手清掉 WebView 那份明文 Cookie（清完要重登网页才能再登）",
                PanPrefs.wipeWebViewCookies(a), new Toggle() {
                    @Override public void onToggle(boolean v) {
                        PanPrefs.setWipeWebViewCookies(a, v);
                        build(a, box, onChange);
                    }
                }));
        c4.addView(divider(a));
        c4.addView(row(a, "调试日志", "取链/分享失败时记的请求与响应（加密存储、已脱敏）",
                "查看 ›", new Runnable() {
                    @Override public void run() {
                        if (a instanceof PanActivity) ((PanActivity) a).showDebugLog();
                    }
                }));
        box.addView(c4);

        TextView foot = hint(a, "支持夸克 / UC；百度、迅雷、139、123 的解析器还没写，"
                + "设置里只标状态、不给假入口。", C_DIM);
        LinearLayout.LayoutParams flp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        flp.topMargin = dp(a, 10);
        box.addView(foot, flp);
    }

    

    private interface Toggle { void onToggle(boolean v); }

    private static View platformRow(final Activity a, final CookiePlatforms.P p,
                                    final LinearLayout box, final OnChange onChange) {
        LinearLayout row = new LinearLayout(a);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(a, 9), 0, dp(a, 9));

        LinearLayout texts = new LinearLayout(a);
        texts.setOrientation(LinearLayout.VERTICAL);
        row.addView(texts, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView name = new TextView(a);
        name.setText(p.name);
        name.setTextSize(13.5f);
        name.setTextColor(C_MAIN);
        texts.addView(name);

        CookieStore.Entry e = CookieStore.get(a, p.id);
        boolean ready = PanProvider.supported(p.id);
        TextView st = new TextView(a);
        st.setTextSize(11f);
        if (!ready) {
            st.setText("未接入 · 解析器还没写");
            st.setTextColor(C_DIM);
        } else if (e != null && e.valid()) {
            st.setText("已登录" + (e.label != null && e.label.length() > 0 ? " · " + e.label : "")
                    + " · 点右侧可退出");
            st.setTextColor(C_OK);
        } else {
            st.setText("游客模式（小文件可下）");
            st.setTextColor(C_SUB);
        }
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        slp.topMargin = dp(a, 2);
        texts.addView(st, slp);

        if (ready) {
            boolean logged = e != null && e.valid();
            final TextView act = logged
                    ? pill(a, "清除登录", 0x33FF6B6B, 0xFFFF8A80)
                    : pill(a, "登录", 0x332F6BE0, 0xFF8FB8FF);
            act.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    if (logged) {
                        CookieStore.remove(a, p.id);
                        Toast.makeText(a, p.name + " 的登录态已清除", Toast.LENGTH_SHORT).show();
                        build(a, box, onChange);
                        if (onChange != null) onChange.changed();
                    } else {
                        try {
                            android.content.Intent it = new android.content.Intent(a,
                                    com.cathelperawa.owo.app.cookie.CookieLoginActivity.class);
                            it.putExtra(com.cathelperawa.owo.app.cookie.CookieLoginActivity.EXTRA_PLATFORM, p.id);
                            a.startActivity(it);
                        } catch (Throwable t) {
                            Toast.makeText(a, "打开登录页失败：" + t, Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            });
            row.addView(act);
        }
        return row;
    }

    private static View row(Activity a, String title, String desc, String value, final Runnable onClick) {
        LinearLayout row = new LinearLayout(a);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(a, 9), 0, dp(a, 9));

        LinearLayout texts = new LinearLayout(a);
        texts.setOrientation(LinearLayout.VERTICAL);
        row.addView(texts, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView t = new TextView(a);
        t.setText(title);
        t.setTextSize(13.5f);
        t.setTextColor(C_MAIN);
        texts.addView(t);

        TextView d = new TextView(a);
        d.setText(desc);
        d.setTextSize(11f);
        d.setTextColor(C_DIM);
        d.setLineSpacing(dp(a, 2), 1f);
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dlp.topMargin = dp(a, 2);
        texts.addView(d, dlp);

        TextView v = new TextView(a);
        v.setText(value == null ? "" : value);
        v.setTextSize(12f);
        v.setTextColor(C_ACCENT);
        v.setMaxLines(1);
        v.setEllipsize(TextUtils.TruncateAt.MIDDLE);
        LinearLayout.LayoutParams vlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        vlp.leftMargin = dp(a, 8);
        row.addView(v, vlp);

        row.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View x) { onClick.run(); }
        });
        return row;
    }

    private static View infoRow(Activity a, String title, String desc) {
        LinearLayout row = new LinearLayout(a);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(0, dp(a, 9), 0, dp(a, 9));
        TextView t = new TextView(a);
        t.setText(title);
        t.setTextSize(13.5f);
        t.setTextColor(C_MAIN);
        row.addView(t);
        TextView d = new TextView(a);
        d.setText(desc);
        d.setTextSize(11f);
        d.setTextColor(C_DIM);
        d.setLineSpacing(dp(a, 2), 1f);
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dlp.topMargin = dp(a, 3);
        row.addView(d, dlp);
        return row;
    }

    private static View toggleRow(final Activity a, String title, String desc,
                                  boolean on, final Toggle toggle) {
        LinearLayout row = new LinearLayout(a);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(a, 9), 0, dp(a, 9));

        LinearLayout texts = new LinearLayout(a);
        texts.setOrientation(LinearLayout.VERTICAL);
        row.addView(texts, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView t = new TextView(a);
        t.setText(title);
        t.setTextSize(13.5f);
        t.setTextColor(C_MAIN);
        texts.addView(t);

        TextView d = new TextView(a);
        d.setText(desc);
        d.setTextSize(11f);
        d.setTextColor(C_DIM);
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dlp.topMargin = dp(a, 2);
        texts.addView(d, dlp);

        final TextView sw = new TextView(a);
        sw.setText(on ? "开" : "关");
        sw.setTextSize(13f);
        sw.setTextColor(on ? C_ACCENT : C_SUB);
        sw.setTypeface(null, Typeface.BOLD);
        sw.setGravity(Gravity.CENTER);
        sw.setPadding(dp(a, 14), dp(a, 6), dp(a, 4), dp(a, 6));
        View.OnClickListener flip = new View.OnClickListener() {
            @Override public void onClick(View v) {
                boolean now = sw.getText().toString().equals("开");
                sw.setText(now ? "关" : "开");
                sw.setTextColor(!now ? C_ACCENT : C_SUB);
                toggle.onToggle(!now);
            }
        };
        sw.setOnClickListener(flip);
        row.addView(sw);
        row.setOnClickListener(flip);
        return row;
    }

    

    private interface OnPick { void onPick(int idx); }

    private interface OnInput { void onInput(String v); }

    private static View inlinePick(Activity a, String[] opts, int cur, final OnPick cb) {
        LinearLayout list = new LinearLayout(a);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(dp(a, 2), dp(a, 2), dp(a, 2), dp(a, 8));
        for (int i = 0; i < opts.length; i++) {
            final int idx = i;
            TextView o = new TextView(a);
            o.setText((i == cur ? "● " : "○ ") + opts[i]);
            o.setTextSize(13f);
            o.setTextColor(i == cur ? C_ACCENT : C_MAIN);
            o.setPadding(dp(a, 6), dp(a, 10), dp(a, 6), dp(a, 10));
            o.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { cb.onPick(idx); }
            });
            list.addView(o);
        }
        return list;
    }

    private static View inlineInput(Activity a, String cur, final OnInput cb) {
        LinearLayout row = new LinearLayout(a);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(a, 2), dp(a, 2), dp(a, 2), dp(a, 10));

        final EditText et = new EditText(a);
        et.setText(cur);
        et.setSingleLine(true);
        et.setInputType(InputType.TYPE_CLASS_TEXT);
        et.setTextSize(13.5f);
        et.setTextColor(C_MAIN);
        GradientDrawable eb = new GradientDrawable();
        eb.setCornerRadius(dp(a, 10));
        eb.setColor(0xFF1C2532);
        eb.setStroke(dp(a, 1), 0x2AFFFFFF);
        et.setBackground(eb);
        et.setPadding(dp(a, 10), dp(a, 8), dp(a, 10), dp(a, 8));
        row.addView(et, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView ok = pill(a, "确定", 0xFF2F6BE0, 0xFFFFFFFF);
        ok.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                String v2 = et.getText() == null ? "" : et.getText().toString();
                cb.onInput(v2);
            }
        });
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        blp.leftMargin = dp(a, 8);
        row.addView(ok, blp);
        return row;
    }

    private static void pick(Activity a, String title, String[] opts, int cur, final OnPick cb) {
        final Dialog d = new Dialog(a);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout box = new LinearLayout(a);
        box.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(a, 18));
        bg.setColor(0xFF141B26);
        bg.setStroke(dp(a, 1), 0x33FFFFFF);
        box.setBackground(bg);
        int p = dp(a, 16);
        box.setPadding(p, p, p, dp(a, 10));

        TextView t = new TextView(a);
        t.setText(title);
        t.setTextSize(16.5f);
        t.setTextColor(C_MAIN);
        t.setTypeface(null, Typeface.BOLD);
        box.addView(t);

        ScrollView sc = new ScrollView(a);
        sc.setVerticalScrollBarEnabled(false);
        final LinearLayout list = new LinearLayout(a);
        list.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        llp.topMargin = dp(a, 8);
        list.setLayoutParams(llp);
        sc.addView(list);
        box.addView(sc, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        for (int i = 0; i < opts.length; i++) {
            final int idx = i;
            TextView o = new TextView(a);
            o.setText((i == cur ? "● " : "○ ") + opts[i]);
            o.setTextSize(14f);
            o.setTextColor(i == cur ? C_ACCENT : C_MAIN);
            o.setPadding(dp(a, 4), dp(a, 11), dp(a, 4), dp(a, 11));
            o.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    d.dismiss();
                    cb.onPick(idx);
                }
            });
            list.addView(o);
        }

        TextView cancel = new TextView(a);
        cancel.setText("取消");
        cancel.setTextSize(13f);
        cancel.setTextColor(C_SUB);
        cancel.setGravity(Gravity.END);
        cancel.setPadding(dp(a, 4), dp(a, 12), dp(a, 8), dp(a, 4));
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { d.dismiss(); }
        });
        box.addView(cancel);

        sheet(a, d, box);
    }

    private static void sheet(Activity a, Dialog d, View content) {
        d.setContentView(content);
        Window w = d.getWindow();
        if (w != null) {
            w.setBackgroundDrawableResource(android.R.color.transparent);
            w.setGravity(Gravity.CENTER);
            w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            w.setDimAmount(0.55f);
            WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
            lp.copyFrom(w.getAttributes());
            int width = a.getResources().getDisplayMetrics().widthPixels - dp(a, 28);
            lp.width = width;
            lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            lp.gravity = Gravity.CENTER;
            lp.x = 0;
            lp.y = 0;
            w.setAttributes(lp);
            w.setWindowAnimations(0);
        }
        d.show();
    }

    private static void input(Activity a, String title, String hintText, String cur, final OnInput cb) {
        final Dialog d = new Dialog(a);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout box = new LinearLayout(a);
        box.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(a, 18));
        bg.setColor(0xFF141B26);
        bg.setStroke(dp(a, 1), 0x33FFFFFF);
        box.setBackground(bg);
        int p = dp(a, 16);
        box.setPadding(p, p, p, dp(a, 8));

        TextView t = new TextView(a);
        t.setText(title);
        t.setTextSize(16.5f);
        t.setTextColor(C_MAIN);
        t.setTypeface(null, Typeface.BOLD);
        box.addView(t);

        TextView h = new TextView(a);
        h.setText(hintText);
        h.setTextSize(11f);
        h.setTextColor(C_DIM);
        LinearLayout.LayoutParams hlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hlp.topMargin = dp(a, 4);
        box.addView(h, hlp);

        final EditText et = new EditText(a);
        et.setText(cur);
        et.setSingleLine(true);
        et.setInputType(InputType.TYPE_CLASS_TEXT);
        et.setTextSize(14f);
        et.setTextColor(C_MAIN);
        GradientDrawable eb = new GradientDrawable();
        eb.setCornerRadius(dp(a, 10));
        eb.setColor(0xFF1C2532);
        eb.setStroke(dp(a, 1), 0x2AFFFFFF);
        et.setBackground(eb);
        et.setPadding(dp(a, 10), dp(a, 8), dp(a, 10), dp(a, 8));
        LinearLayout.LayoutParams elp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        elp.topMargin = dp(a, 10);
        box.addView(et, elp);

        LinearLayout btns = new LinearLayout(a);
        btns.setOrientation(LinearLayout.HORIZONTAL);
        btns.setGravity(Gravity.END);
        TextView cancel = pill(a, "取消", 0x22FFFFFF, C_SUB);
        TextView ok = pill(a, "确定", 0xFF2F6BE0, 0xFFFFFFFF);
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        blp.leftMargin = dp(a, 8);
        blp.topMargin = dp(a, 10);
        btns.addView(cancel);
        btns.addView(ok, blp);
        LinearLayout.LayoutParams blp2 = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        blp2.topMargin = dp(a, 6);
        box.addView(btns, blp2);

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { d.dismiss(); }
        });
        ok.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                String s = et.getText() == null ? "" : et.getText().toString().trim();
                d.dismiss();
                cb.onInput(s);
            }
        });

        d.setContentView(box);
        Window w = d.getWindow();
        if (w != null) w.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        sheet(a, d, box);
    }

    

    private static LinearLayout card(Activity a) {
        LinearLayout v = new LinearLayout(a);
        v.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(dp(a, 14));
        g.setColor(0xF21A2230);
        g.setStroke(dp(a, 1), 0x33FFFFFF);
        v.setBackground(g);
        int p = dp(a, 13);
        v.setPadding(p, dp(a, 2), p, dp(a, 2));
        return v;
    }

    private static View divider(Activity a) {
        View v = new View(a);
        v.setBackgroundColor(0x1AFFFFFF);
        a.getResources().getDisplayMetrics();
        v.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, dp(a, 0.6f))));
        return v;
    }

    private static TextView group(Activity a, String text) {
        TextView tv = new TextView(a);
        tv.setText(text);
        tv.setTextSize(12f);
        tv.setTextColor(0xFF8395AD);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(a, 14);
        lp.bottomMargin = dp(a, 7);
        lp.leftMargin = dp(a, 4);
        tv.setLayoutParams(lp);
        return tv;
    }

    private static TextView hint(Activity a, String text, int color) {
        TextView tv = new TextView(a);
        tv.setText(text);
        tv.setTextSize(11.5f);
        tv.setTextColor(color);
        tv.setLineSpacing(dp(a, 3), 1f);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.leftMargin = dp(a, 4);
        lp.rightMargin = dp(a, 4);
        tv.setLayoutParams(lp);
        return tv;
    }

    private static TextView pill(Activity a, String text, int bg, int fg) {
        TextView tv = new TextView(a);
        tv.setText(text);
        tv.setTextSize(11.5f);
        tv.setTextColor(fg);
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(dp(a, 20));
        g.setColor(bg);
        tv.setBackground(g);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(dp(a, 11), dp(a, 6), dp(a, 11), dp(a, 6));
        return tv;
    }

    private static String panName(String id) {
        return PanProvider.nameOf(id);
    }

    private static int dp(Activity a, float v) {
        return Math.max(1, (int) (v * a.getResources().getDisplayMetrics().density + 0.5f));
    }
}
