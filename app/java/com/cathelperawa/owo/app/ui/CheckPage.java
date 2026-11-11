package com.cathelperawa.owo.app.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.cathelperawa.owo.app.core.Bg;
import com.cathelperawa.owo.app.core.FullCheck;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CheckPage extends Page {

    public CheckPage(Host host) { super(host); }

    private TextView status;
    private LinearLayout contentBox;
    private String raw = "";
    private boolean rawMode = false;
    private boolean hasReport = false;

    @Override
    public View build() {
        View[] out = new View[4];
        LinearLayout root = pageRootEx("全面体检", "自检 · 类名体检 · 冒烟测试", out);

        LinearLayout topbar = (LinearLayout) out[2];
        topbar.addView(navButton("复制", new Runnable() {
            @Override public void run() {
                if (!hasReport) { toast("还没有报告"); return; }
                copy(raw);
                toast("报告已复制");
            }
        }), wrapWrap());
        topbar.addView(navButton("原文", new Runnable() {
            @Override public void run() {
                if (!hasReport) { toast("还没有报告"); return; }
                rawMode = !rawMode;
                render();
            }
        }), wrapWrap());
        topbar.addView(navButton("重新体检", new Runnable() {
            @Override public void run() { run(); }
        }), wrapWrap());

        status = (TextView) out[3];

        contentBox = new LinearLayout(ctx);
        contentBox.setOrientation(LinearLayout.VERTICAL);
        root.addView(contentBox, matchWrap(0));

        LinearLayout tip = whiteCard(dp(16), 0x6AFFFFFF);
        tip.setPadding(dp(14), dp(12), dp(14), dp(12));
        final String hn = com.cathelperawa.owo.app.core.Host.isQQ() ? "QQ" : "TIM";
        tip.addView(text("这段报告由宿主进程生成："
                + "模块通过框架注入在 " + hn + " 内运行，直接读取宿主的功能实例、类索引和 dex，"
                + "算完交给控制台显示。\n"
                + "若一直显示「没读到结果」，说明 " + hn + " 没在运行，"
                + "或模块没加载（LSPosed 未启用 / 作用域没勾 " + hn + " / 宿主版本不匹配）。",
                11, 0xFF7E90A8, false, 0));
        root.addView(tip, matchWrap(dp(10)));

        fadeIn((View) out[0]);
        run();
        return (View) out[0];
    }

    private TextView navButton(String label, final Runnable onClick) {
        TextView tv = text(label, 12, 0xFF7FA5E6, true, 0);
        tv.setPadding(dp(8), dp(6), dp(8), dp(6));
        tv.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                press(v);
                onClick.run();
            }
        });
        return tv;
    }

    

    private void run() {
        final boolean inHost = !com.cathelperawa.owo.app.core.HookItemFactory.all().isEmpty();
        if (status != null) {
            status.setText(inHost
                    ? (com.cathelperawa.owo.app.core.Host.isQQ() ? "QQ 进程内执行" : "TIM 进程内执行")
                    : "请稍候");
        }
        showLoading(inHost ? "正在体检…（本进程）" : "正在体检…（转发给 " + com.cathelperawa.owo.app.core.Host.name() + " 执行）");

        if (inHost) {

            startWatchdog(45);
            Bg.run(new Runnable() {
                @Override public void run() {
                    String rep;
                    try {
                        rep = com.cathelperawa.owo.app.core.InProcCheck.build(ctx);
                    } catch (Throwable t) {
                        rep = "体检失败：" + t;
                    }
                    deliver("体检完成", rep);
                }
            }, "check-inproc");
            return;
        }

        FullCheck.runAsync(ctx, new FullCheck.Callback() {
            @Override public void onDone(String message, String report) {
                deliver(message, report == null ? ("没读到报告\n\n" + message) : report);
                if (report == null) toast(message);
            }
        });
    }

    private android.os.Handler wdHandler;
    private Runnable wdTask;

    private void startWatchdog(final int sec) {
        cancelWatchdog();
        wdHandler = new android.os.Handler(android.os.Looper.getMainLooper());
        wdTask = new Runnable() {
            @Override public void run() {
                if (status != null) status.setText("体检超时（" + sec + " 秒）");
                render();
                toast("体检跑太久了，点右上「重新体检」再试一次");
            }
        };
        wdHandler.postDelayed(wdTask, sec * 1000L);
    }

    private void cancelWatchdog() {
        try {
            if (wdHandler != null && wdTask != null) wdHandler.removeCallbacks(wdTask);
        } catch (Throwable ignore) {
        }
    }

    private void deliver(final String message, final String report) {
        Runnable r = new Runnable() {
            @Override public void run() {
                raw = report == null ? "" : report;
                hasReport = raw.length() > 0;
                if (status != null) status.setText(message);
                render();
            }
        };
        if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) {
            r.run();
        } else {
            new android.os.Handler(android.os.Looper.getMainLooper()).post(r);
        }
    }

    private void showLoading(String msg) {
        if (contentBox == null) return;
        contentBox.removeAllViews();
        LinearLayout card = whiteCard(dp(18), 0x9CFFFFFF);
        card.setPadding(dp(14), dp(16), dp(14), dp(16));
        card.addView(text(msg, 13, 0xFF6F86A8, false, 0));
        contentBox.addView(card, matchWrap(0));
    }

    

    private void render() {
        if (contentBox == null) return;
        contentBox.removeAllViews();

        if (!hasReport) {
            LinearLayout card = whiteCard(dp(18), 0x9CFFFFFF);
            card.setPadding(dp(16), dp(16), dp(16), dp(16));
            card.addView(text("没读到体检报告", 14, 0xFF43587A, true, dp(4)));
            card.addView(text(com.cathelperawa.owo.app.core.Host.name() + " 没在运行，或模块没加载。点右上角「重新体检」再试一次。",
                    11, 0xFF9FB0C7, false, 0));
            contentBox.addView(card, matchWrap(0));
            return;
        }

        contentBox.addView(buildVerdict(), matchWrap(dp(10)));

        if (rawMode) {
            contentBox.addView(buildRawCard(), matchWrap(dp(10)));
        } else {
            List<Section> sections = parse(raw);
            for (Section s : sections) {
                contentBox.addView(buildSection(s), matchWrap(dp(10)));
            }
        }

        TextView tip = text(rawMode ? "原文模式 · 可长按选中复制" : "点段落标题可折叠 · 顶栏可切「原文」",
                10, 0xFFAAB6C6, false, 0);
        tip.setGravity(Gravity.CENTER);
        tip.setPadding(0, dp(2), 0, dp(8));
        contentBox.addView(tip, matchWrap(0));
    }

    private View buildVerdict() {
        int fail = num(raw, "失败 (\\d+)");
        int untested = num(raw, "未自检 (\\d+)");
        int dead = num(raw, "全废[:：]? (\\d+)");
        int plain = plainCount(raw);
        int problems = fail + dead + plain + untested;

        LinearLayout card = whiteCard(dp(20), 0xE6FFFFFF);
        card.setPadding(dp(18), dp(16), dp(18), dp(16));

        boolean clean = problems == 0;
        card.addView(text(clean ? "结论 · 一切正常" : "结论 · 有 " + problems + " 项要留意",
                18, clean ? 0xFF3E9E6E : 0xFFC0504A, true, dp(8)));
        card.addView(text(clean
                ? "功能全部自检通过，没有检测到明文落盘的隐私文件。"
                : "下面标红/标琥珀的行就是要处理的地方，点开对应段落能看细节。",
                11, 0xFF9FB0C7, false, dp(10)));

        LinearLayout pills = new LinearLayout(ctx);
        pills.setOrientation(LinearLayout.HORIZONTAL);
        pills.addView(chip("失败 " + fail, fail > 0 ? 0xFFE05C5C : 0xFF4CAF7D));
        pills.addView(chip("未自检 " + untested, untested > 0 ? 0xFFE8A33D : 0xFF4CAF7D));
        pills.addView(chip("明文 " + plain, plain > 0 ? 0xFFE8A33D : 0xFF4CAF7D));
        if (dead > 0) pills.addView(chip("全废 " + dead, 0xFFE05C5C));
        card.addView(pills, matchWrap(0));
        return card;
    }

    private TextView chip(String label, int color) {
        TextView tv = new TextView(ctx);
        tv.setText(label);
        tv.setTextSize(10);
        tv.setTextColor(color);
        tv.setTypeface(Typeface.DEFAULT_BOLD, Typeface.BOLD);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(dp(9), dp(4), dp(9), dp(4));
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(20));
        bg.setColor((color & 0x00FFFFFF) | 0x1F000000);
        tv.setBackgroundDrawable(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.rightMargin = dp(6);
        tv.setLayoutParams(lp);
        return tv;
    }

    private View buildRawCard() {
        LinearLayout card = whiteCard(dp(18), 0x9CFFFFFF);
        card.setPadding(dp(14), dp(12), dp(14), dp(14));
        TextView body = new TextView(ctx);
        body.setTextSize(11);
        body.setTypeface(Typeface.MONOSPACE);
        body.setTextColor(0xFF233B5E);
        body.setTextIsSelectable(true);
        body.setText(raw);
        card.addView(body);
        return card;
    }

    private static final class Section {
        String title;
        final List<String> lines = new ArrayList<String>();
    }

    private static List<Section> parse(String text) {
        List<Section> out = new ArrayList<Section>();
        Section cur = null;
        for (String line : text.split("\n")) {
            String t = line.trim();
            if (t.length() == 0) continue;
            if (t.startsWith("──") && t.endsWith("──") && t.length() > 4) {
                cur = new Section();
                cur.title = strip(t);
                out.add(cur);
                continue;
            }
            if (t.startsWith("══") && t.endsWith("══")) {
                if (cur == null) {
                    cur = new Section();
                    cur.title = "概览";
                    out.add(cur);
                }
                cur.lines.add(strip(t));
                continue;
            }
            if (cur == null) {
                cur = new Section();
                cur.title = "概览";
                out.add(cur);
            }
            cur.lines.add(line);
        }
        return out;
    }

    private static String strip(String s) {
        return s.replace("──", "").replace("══", "").trim();
    }

    private View buildSection(final Section s) {
        LinearLayout card = whiteCard(dp(18), 0xD9FFFFFF);
        card.setPadding(dp(14), dp(12), dp(14), dp(10));

        int warns = 0;
        for (String l : s.lines) if (isWarn(l)) warns++;

        final LinearLayout body = new LinearLayout(ctx);
        body.setOrientation(LinearLayout.VERTICAL);

        LinearLayout head = new LinearLayout(ctx);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.addView(text(s.title, 14, 0xFF43587A, true, 0),
                new LinearLayout.LayoutParams(0,
                        LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        if (warns > 0) head.addView(chip(warns + " 处留意", 0xFFE8A33D));
        final TextView arrow = text("▾", 12, 0xFFB8C4D2, true, 0);
        arrow.setPadding(dp(6), 0, 0, 0);
        head.addView(arrow);
        card.addView(head, matchWrap(dp(6)));

        LinearLayout statRow = null;
        for (String rawLine : s.lines) {
            String line = rawLine;
            String t = line.trim();
            if (t.length() == 0) continue;

            if (looksLikeStat(t)) {
                statRow = new LinearLayout(ctx);
                statRow.setOrientation(LinearLayout.HORIZONTAL);
                String[] parts = t.split("·");
                for (String p : parts) {
                    String v = p.trim();
                    if (v.length() == 0) continue;
                    statRow.addView(statChip(v));
                }
                HorizontalScrollView hs = new HorizontalScrollView(ctx);
                hs.setHorizontalScrollBarEnabled(false);
                hs.setOverScrollMode(View.OVER_SCROLL_NEVER);
                hs.addView(statRow);
                body.addView(hs, matchWrap(dp(4)));
                continue;
            }

            body.addView(lineView(t), matchWrap(dp(1)));
        }

        card.addView(body);
        body.setVisibility(View.VISIBLE);

        final LinearLayout fbody = body;
        final TextView farrow = arrow;
        head.setClickable(true);
        head.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                press(v);
                boolean open = fbody.getVisibility() != View.GONE;
                fbody.setVisibility(open ? View.GONE : View.VISIBLE);
                farrow.setText(open ? "▸" : "▾");
            }
        });
        return card;
    }

    private static boolean looksLikeStat(String t) {
        if (t.length() > 80) return false;
        if (t.startsWith("·") || t.startsWith("●") || t.startsWith("◐")
                || t.startsWith("○") || t.startsWith("✗") || t.startsWith("⚠")
                || t.startsWith("√") || t.startsWith("✓")) {
            return false;
        }
        if (t.indexOf("·") < 0) return false;
        String[] parts = t.split("·");
        if (parts.length < 2) return false;
        
        int withNum = 0;
        for (String p : parts) {
            if (p.matches(".*\\d.*")) withNum++;
        }
        return withNum >= Math.max(1, parts.length / 2);
    }

    private static final Pattern PRIV = Pattern.compile(
            "^[●◐○]\\s*(?:高|中|低)?\\s*(.*?)\\s+(已加密|明文|临时|未生成|未设置)\\s+(\\S+?)\\s*(←)?$");

    private static boolean isPrivacy(String t) {
        return t.length() > 0 && "●◐○".indexOf(t.charAt(0)) >= 0;
    }

    private View privacyRow(String t) {
        Matcher m = PRIV.matcher(t);
        String name = t.substring(1).replaceAll("^\\s*(高|中|低)\\s*", "").trim();
        String state = "";
        String size = "";
        boolean bad = t.contains("←") || t.endsWith("←");
        if (m.matches()) {
            name = m.group(1).trim();
            state = m.group(2);
            size = m.group(3).trim();
        } else {
            
            if (t.contains("明文")) state = "明文";
            else if (t.contains("已加密")) state = "已加密";
            else if (t.contains("临时")) state = "临时";
            else if (t.contains("未生成")) state = "未生成";
            else if (t.contains("未设置")) state = "未设置";
        }

        int stateColor;
        if ("明文".equals(state)) stateColor = 0xFFD98A1E;
        else if ("已加密".equals(state)) stateColor = 0xFF4CAF7D;
        else if ("临时".equals(state)) stateColor = 0xFF8FA3BC;   
        else stateColor = 0xFFAAB6C6;

        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(2), 0, dp(3));

        TextView dot = text(t.substring(0, 1), 10, stateColor, true, 0);
        dot.setPadding(0, 0, dp(6), 0);
        row.addView(dot);

        TextView nm = text(name, 11,
                "明文".equals(state) ? 0xFFB87412 : 0xFF43587A, false, 0);
        row.addView(nm, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        if (state.length() > 0) {
            TextView st = text(state, 10, stateColor, true, 0);
            st.setPadding(dp(6), 0, dp(6), 0);
            row.addView(st);
        }
        if (size.length() > 0) {
            TextView sz = text(size, 10, 0xFF9FB0C7, false, 0);
            sz.setGravity(Gravity.END);
            row.addView(sz);
        }
        if (bad) {
            TextView mk = text(" ←", 10, 0xFFD98A1E, true, 0);
            row.addView(mk);
        }
        return row;
    }

    private View lineView(String t) {
        if (isPrivacy(t)) return privacyRow(t);
        int color = 0xFF43587A;
        String prefix = "";
        String text = t;

        if (t.startsWith("✗")) {
            color = 0xFFE05C5C;
            prefix = "✗";
            text = t.substring(1).trim();
        } else if (t.startsWith("⚠")) {
            color = 0xFFD98A1E;
            prefix = "⚠";
            text = t.substring(1).trim();
        } else if (t.startsWith("·")) {
            color = 0xFF7E90A8;
            prefix = "·";
            text = t.substring(1).trim();
        } else if (t.startsWith("√") || t.startsWith("✓")) {
            color = 0xFF4CAF7D;
            prefix = "✓";
            text = t.substring(1).trim();
        } else if (t.contains("明文")) {
            color = 0xFFD98A1E;
        } else if (t.startsWith("类 ") || t.startsWith("宿主") || t.startsWith("堆 ")
                || t.startsWith("本进程")) {
            color = 0xFF6F86A8;
        }

        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        if (prefix.length() > 0) {
            TextView m = text(prefix, 11, color, true, 0);
            m.setPadding(0, 0, dp(5), 0);
            row.addView(m);
        }
        TextView tv = text(text, 11, color, false, 0);
        tv.setTypeface(Typeface.MONOSPACE);
        tv.setTextIsSelectable(true);
        
        tv.setMaxLines(3);
        row.addView(tv, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        return row;
    }

    private TextView statChip(String v) {
        TextView tv = new TextView(ctx);
        tv.setText(v);
        tv.setTextSize(10);
        boolean warn = v.contains("失败") && !endsWithZero(v)
                || v.contains("异常") && !endsWithZero(v)
                || v.contains("未自检") && !endsWithZero(v)
                || v.contains("全废") && !endsWithZero(v)
                || v.contains("丢弃") && !endsWithZero(v)
                || v.contains("错误") && !endsWithZero(v);
        tv.setTextColor(warn ? 0xFFD98A1E : 0xFF6F86A8);
        tv.setTypeface(Typeface.DEFAULT_BOLD, Typeface.BOLD);
        tv.setPadding(dp(8), dp(3), dp(8), dp(3));
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(14));
        bg.setColor(warn ? 0x1FD98A1E : 0x14000000);
        tv.setBackgroundDrawable(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.rightMargin = dp(5);
        tv.setLayoutParams(lp);
        return tv;
    }

    private static boolean endsWithZero(String s) {
        String t = s.trim();
        return t.endsWith(" 0") || t.endsWith(" 0/") || t.endsWith("0%");
    }

    private static boolean isWarn(String line) {
        String t = line.trim();
        if (t.startsWith("✗") || t.startsWith("⚠")) return true;
        if (isPrivacy(t)) return t.contains("明文");
        return false;
    }

    private static int num(String text, String regex) {
        try {
            Matcher m = Pattern.compile(regex).matcher(text);
            if (m.find()) return Integer.parseInt(m.group(1));
        } catch (Throwable ignore) {
        }
        return 0;
    }

    private static int plainCount(String text) {
        int n = num(text, "明文 (\\d+)\\s*·");
        if (n > 0) return n;
        int c = 0;
        for (String line : text.split("\n")) {
            String t = line.trim();
            if (isPrivacy(t) && t.contains("明文")) c++;
        }
        return c;
    }

    private void copy(String s) {
        try {
            ClipboardManager cm = (ClipboardManager)
                    ctx.getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null) cm.setPrimaryClip(ClipData.newPlainText("体检报告", s));
        } catch (Throwable ignore) {
        }
    }
}
