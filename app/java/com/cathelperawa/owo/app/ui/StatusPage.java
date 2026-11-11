package com.cathelperawa.owo.app.ui;

import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.cathelperawa.owo.app.core.Bg;
import com.cathelperawa.owo.app.core.FeatureReport;
import com.cathelperawa.owo.app.core.FeatureStatus;
import com.cathelperawa.owo.app.core.HookItem;
import com.cathelperawa.owo.app.core.HookItemBase;
import com.cathelperawa.owo.app.core.HookItemFactory;
import com.cathelperawa.owo.app.core.SelfCheckResult;
import com.cathelperawa.owo.app.core.StatusStore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class StatusPage extends Page {

    private static final String PAGE = "status";

    private static final int F_ALL = 0;
    private static final int F_PROBLEM = 1;
    private static final int F_UNTESTED = 2;
    private static final int F_OFF = 3;

    private LinearLayout contentBox;
    private TextView subtitle;
    private List<Row> lastRows = new ArrayList<Row>();
    private int filter = F_ALL;

    public StatusPage(Host host) { super(host); }

    @Override
    public View build() {
        
        final View[] out = new View[4];
        pageRootEx("功能状态", "正在读取…", out);

        LinearLayout topbar = (LinearLayout) out[2];
        topbar.addView(navButton("刷新", new Runnable() {
            @Override public void run() { reload(); }
        }), wrapWrap());
        topbar.addView(navButton("自检", new Runnable() {
            @Override public void run() { runAllChecks(); }
        }), wrapWrap());
        topbar.addView(navButton("诊断包", new Runnable() {
            @Override public void run() { exportReport(); }
        }), wrapWrap());

        contentBox = (LinearLayout) out[1];
        subtitle = (TextView) out[3];

        reload();
        return out[0];
    }

    private TextView navButton(String label, final Runnable onClick) {
        TextView tv = text(label, 12, 0xFF7FA5E6, true, 0);
        tv.setPadding(dp(10), dp(6), dp(10), dp(6));
        tv.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                press(v);
                onClick.run();
            }
        });
        return tv;
    }

    

    
    private void runAllChecks() {
        if (subtitle != null) subtitle.setText("正在自检…");
        Bg.run(new Runnable() {
            @Override public void run() {
                int n = 0;
                try {
                    for (HookItemBase item : HookItemFactory.all()) {
                        try {
                            item.runSelfCheck();
                            n++;
                        } catch (Throwable ignore) {
                        }
                    }
                } catch (Throwable ignore) {
                }
                final int done = n;
                new android.os.Handler(android.os.Looper.getMainLooper()).post(new Runnable() {
                    @Override public void run() {
                        if (subtitle != null && done > 0) {
                            subtitle.setText("已自检 " + done + " 个功能");
                        } else if (subtitle != null) {
                            subtitle.setText("本进程没有功能实例（用宿主自检）");
                        }
                        reload();
                    }
                });
            }
        }, "self-check");
    }

    private void exportReport() {
        if (subtitle != null) subtitle.setText("正在打包诊断包…");
        Bg.run(new Runnable() {
            @Override public void run() {
                String msg;
                boolean ok;
                try {
                    try {
                        com.cathelperawa.owo.app.core.ModuleCommand.send(ctx,
                                com.cathelperawa.owo.app.core.ModuleCommand.CMD_EXPORT_REPORT);
                    } catch (Throwable ignore) {
                    }
                    Thread.sleep(400);
                    com.cathelperawa.owo.app.core.BugReportExporter.Result r =
                            com.cathelperawa.owo.app.core.BugReportExporter.exportAndShare(ctx);
                    ok = r.ok();
                    msg = r.message;
                } catch (Throwable t) {
                    ok = false;
                    msg = "打包失败：" + t;
                }
                final boolean okf = ok;
                final String msgf = msg;
                new android.os.Handler(android.os.Looper.getMainLooper()).post(new Runnable() {
                    @Override public void run() {
                        if (subtitle != null) subtitle.setText(okf ? "诊断包已生成" : "诊断包生成失败");
                        toast(msgf);
                    }
                });
            }
        }, "report-export");
    }

    private void reload() {
        if (contentBox == null) return;
        if (subtitle != null) subtitle.setText("正在读取…");
        final android.os.Handler ui = new android.os.Handler(
                android.os.Looper.getMainLooper());
        Bg.run(new Runnable() {
            @Override public void run() {
                final List<Row> rows = collect();
                ui.post(new Runnable() {
                    @Override public void run() { render(rows); }
                });
            }
        }, "status-reader");
    }

    

    private void render(List<Row> rows) {
        if (contentBox == null) return;
        lastRows = rows;
        
        while (contentBox.getChildCount() > 1) {
            contentBox.removeViewAt(contentBox.getChildCount() - 1);
        }

        if (subtitle != null) {
            subtitle.setText(rows.isEmpty()
                    ? "没有读到状态数据"
                    : rows.size() + " 个功能 · 更新于 " + hhmmOnce());
        }

        if (rows.isEmpty()) {
            contentBox.addView(buildEmptyCard(), matchWrap(dp(12)));
            return;
        }

        contentBox.addView(buildSummary(rows), matchWrap(dp(10)));
        contentBox.addView(buildFilterBar(rows), matchWrap(dp(10)));

        List<Row> shown = filtered(rows);
        if (shown.isEmpty()) {
            LinearLayout card = whiteCard(dp(18), 0xCCFFFFFF);
            card.setPadding(dp(18), dp(16), dp(18), dp(16));
            card.addView(text("这个筛选下没有功能", 14, 0xFF43587A, true, dp(4)));
            card.addView(text("点上面的「全部」看其它状态。", 11, 0xFF9FB0C7, false, 0));
            contentBox.addView(card, matchWrap(dp(11)));
        } else {
            Map<String, List<Row>> groups = group(shown);
            for (Map.Entry<String, List<Row>> e : groups.entrySet()) {
                contentBox.addView(buildGroup(e.getKey(), e.getValue()), matchWrap(dp(10)));
            }
        }

        TextView tip = text("数据来自宿主进程 · 点任一功能行看明细 · 点顶部卡片看全面体检",
                11, 0xFFAAB6C6, false, 0);
        tip.setGravity(Gravity.CENTER);
        tip.setPadding(0, dp(4), 0, dp(10));
        contentBox.addView(tip, matchWrap(0));
    }

    private View buildEmptyCard() {
        LinearLayout card = whiteCard(dp(20), 0xE6FFFFFF);
        card.setPadding(dp(18), dp(18), dp(18), dp(18));
        card.addView(text("还没有状态数据", 15, 0xFF43587A, true, dp(6)));
        card.addView(text("打开 " + com.cathelperawa.owo.app.core.Host.name() + " 一次，等模块加载完成后点右上角「刷新」。",
                12, 0xFF8496AE, false, dp(4)));
        card.addView(text("若 TIM 已打开仍读不到：控制台需要 root / su 才有权限\n"
                        + "读 TIM 私有目录里的状态文件。",
                11, 0xFFAAB6C6, false, 0));
        return card;
    }

    private View buildSummary(List<Row> rows) {
        int[] cnt = new int[FeatureStatus.values().length];
        int untested = 0;
        for (Row r : rows) {
            cnt[r.status().ordinal()]++;
            if (r.checkText == null || r.checkText.length() == 0) untested++;
        }

        LinearLayout card = whiteCard(dp(22), 0xE6FFFFFF);
        card.setPadding(dp(18), dp(16), dp(18), dp(16));

        card.addView(text("本期体检", 12, 0xFF9FB0C7, false, dp(2)));
        card.addView(text("" + rows.size() + " 个功能", 26, 0xFF43587A, true, dp(10)));

        LinearLayout pills = new LinearLayout(ctx);
        pills.setOrientation(LinearLayout.HORIZONTAL);
        for (FeatureStatus s : new FeatureStatus[]{
                FeatureStatus.OK, FeatureStatus.PARTIAL,
                FeatureStatus.FAILED, FeatureStatus.ERROR,
                FeatureStatus.UNKNOWN, FeatureStatus.DISABLED}) {
            int n = cnt[s.ordinal()];
            if (n == 0 && s != FeatureStatus.OK) continue;
            pills.addView(statusPill(s.label + " " + n, s.color));
        }
        card.addView(pills, matchWrap(dp(8)));

        String sub = untested > 0
                ? "自检覆盖 " + (rows.size() - untested) + "/" + rows.size()
                  + " · 还有 " + untested + " 个没自检 · 点我查看全面体检"
                : "全部功能已自检 · 点我查看全面体检报告";
        card.addView(text(sub, 10, 0xFF9FB0C7, false, 0));

        card.setClickable(true);
        card.setForeground(clickRipple());
        card.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                press(v);
                host.open(Page.PAGE_FULLCHECK, "全面体检");
            }
        });
        return card;
    }

    private View buildFilterBar(List<Row> rows) {
        int problem = 0, untested = 0, off = 0;
        for (Row r : rows) {
            if (isProblem(r)) problem++;
            if (r.checkText == null || r.checkText.length() == 0) untested++;
            if (isOff(r)) off++;
        }

        LinearLayout bar = new LinearLayout(ctx);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.addView(filterChip("全部", rows.size(), F_ALL), chipLp(3));
        bar.addView(filterChip("异常", problem, F_PROBLEM), chipLp(3));
        bar.addView(filterChip("未自检", untested, F_UNTESTED), chipLp(3));
        bar.addView(filterChip("已关闭", off, F_OFF), chipLp(0));
        return bar;
    }

    private TextView filterChip(String label, int n, final int f) {
        boolean on = filter == f;
        TextView tv = new TextView(ctx);
        tv.setText(n > 0 ? label + " " + n : label);
        tv.setTextSize(11);
        tv.setGravity(Gravity.CENTER);
        tv.setTypeface(Typeface.DEFAULT_BOLD, Typeface.BOLD);
        tv.setTextColor(on ? 0xFFFFFFFF : 0xFF6F86A8);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(14));
        bg.setColor(on ? 0xFF8FB3E8 : 0xCCFFFFFF);
        tv.setBackgroundDrawable(bg);
        tv.setPadding(dp(4), dp(7), dp(4), dp(7));
        tv.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                press(v);
                if (filter == f) return;
                filter = f;
                render(lastRows);
            }
        });
        return tv;
    }

    private LinearLayout.LayoutParams chipLp(int rightMargin) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        lp.rightMargin = dp(rightMargin);
        return lp;
    }

    private View buildGroup(String groupName, List<Row> items) {
        LinearLayout card = whiteCard(dp(20), 0xE6FFFFFF);
        card.setPadding(dp(16), dp(14), dp(16), dp(10));

        int problem = 0, off = 0, untested = 0;
        for (Row r : items) {
            if (isProblem(r)) problem++;
            if (isOff(r)) off++;
            else if (isUntested(r)) untested++;
        }

        LinearLayout head = new LinearLayout(ctx);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.addView(text(groupName, 15, 0xFF43587A, true, 0),
                new LinearLayout.LayoutParams(0,
                        LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        if (problem > 0) {
            head.addView(statusPill(problem + " 项异常", 0xFFE05C5C));
        }
        if (untested > 0) {
            head.addView(statusPill(untested + " 未自检", 0xFFE8A33D));
        }
        if (off > 0) {
            head.addView(statusPill(off + " 已关闭", 0xFF9FB0C7));
        }
        if (problem == 0 && off == 0 && untested == 0) {
            head.addView(statusPill("全部正常", 0xFF4CAF7D));
        }
        card.addView(head, matchWrap(dp(8)));

        for (Row r : items) {
            card.addView(buildRow(r), matchWrap(dp(4)));
        }
        return card;
    }

    private View buildRow(final Row r) {
        FeatureStatus st = r.status();

        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(12));
        bg.setColor(isOff(r) ? 0x33FFFFFF : 0x66FFFFFF);
        row.setBackgroundDrawable(bg);
        row.setPadding(dp(12), dp(10), dp(12), dp(10));

        LinearLayout line1 = new LinearLayout(ctx);
        line1.setOrientation(LinearLayout.HORIZONTAL);
        line1.setGravity(Gravity.CENTER_VERTICAL);

        View dot = new View(ctx);
        GradientDrawable ds = new GradientDrawable();
        ds.setShape(GradientDrawable.OVAL);
        ds.setColor(st.color);
        dot.setBackgroundDrawable(ds);
        LinearLayout.LayoutParams dotLp = new LinearLayout.LayoutParams(dp(8), dp(8));
        dotLp.rightMargin = dp(7);
        line1.addView(dot, dotLp);

        line1.addView(text(r.name, 13, isOff(r) ? 0xFF8496AE : 0xFF43587A, true, 0),
                new LinearLayout.LayoutParams(0,
                        LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        line1.addView(text(st.label, 11, st.color, true, 0));
        TextView arrow = text("›", 15, 0xFFC3CEDC, true, 0);
        arrow.setPadding(dp(6), 0, 0, 0);
        line1.addView(arrow);
        row.addView(line1);

        if (r.fireText != null && r.fireText.length() > 0) {
            int color;
            String label;
            if (!r.instrumented && r.fireCount == 0) {
                color = 0xFFCBD5E1;
                label = "· 未埋点（无独立事件可统计）";
            } else if (r.fireCount > 0) {
                color = 0xFF4CAF7D;
                label = "· " + r.fireText;
            } else {
                color = 0xFFB8C4D2;
                label = "· " + r.fireText;
            }
            TextView f = text(label, 10, color, false, 0);
            f.setPadding(dp(15), dp(2), 0, 0);
            f.setMaxLines(2);
            row.addView(f);
        }

        if (r.checkText != null && r.checkText.length() > 0) {
            boolean allPass = r.checkFail == null || r.checkFail.length() == 0;
            String txt = allPass
                    ? "· 自检 " + r.checkText + " 通过"
                    : "· 自检 " + r.checkText + " · " + r.checkFail;
            TextView c = text(txt, 10,
                    allPass ? 0xFF5BA8D4 : 0xFFE05C5C, false, 0);
            c.setPadding(dp(15), dp(2), 0, 0);
            c.setMaxLines(2);
            row.addView(c);
        } else {
            TextView c = text("· 未自检（点这行看详情）", 10, 0xFFC3CEDC, false, 0);
            c.setPadding(dp(15), dp(2), 0, 0);
            row.addView(c);
        }

        String detail = r.reason != null && r.reason.length() > 0
                ? r.reason : r.summary;
        if (detail != null && detail.length() > 0) {
            TextView d = text(detail, 10,
                    st.isProblem() ? 0xFFD0605A : 0xFF9FB0C7, false, 0);
            d.setMaxLines(3);
            d.setPadding(dp(15), dp(3), 0, 0);
            row.addView(d);
        }

        row.setClickable(true);
        row.setForeground(clickRipple());
        row.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                press(v);
                showDetail(r);
            }
        });
        return row;
    }

    private void showDetail(final Row r) {
        final FeatureStatus st = r.status();

        LinearLayout body = new LinearLayout(ctx);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(4), dp(4), dp(4), dp(4));

        LinearLayout pills = new LinearLayout(ctx);
        pills.setOrientation(LinearLayout.HORIZONTAL);
        pills.addView(statusPill(st.label, st.color));
        if (r.hit > 0 || r.expect > 0) {
            pills.addView(statusPill("钩子 " + r.hit + "/" + (r.expect > 0 ? r.expect : r.hit),
                    0xFF7FA5E6));
        }
        body.addView(pills, matchWrap(dp(8)));

        addDetailLine(body, "分组", r.path != null && r.path.length() > 0 ? r.path : r.group);
        if (r.summary != null && r.summary.length() > 0) addDetailLine(body, "摘要", r.summary);
        if (r.reason != null && r.reason.length() > 0) addDetailLine(body, "原因", r.reason);
        if (r.fireText != null && r.fireText.length() > 0) {
            String ft = r.fireText;
            if (r.lastFireAt > 0) ft += " · 最后一次 " + hhmm(r.lastFireAt);
            addDetailLine(body, "触发", ft);
        }
        if (r.deepProblem != null && r.deepProblem.length() > 0) {
            addDetailLine(body, "依赖问题", r.deepProblem);
        }
        if (r.runtimeErrs > 0) {
            addDetailLine(body, "运行期异常", r.runtimeErrs + " 次（见模块日志）");
        }

        SelfCheckResult sc = null;
        if (r.item != null) {
            try { sc = r.item.selfCheck(); } catch (Throwable ignore) {}
        }
        if (sc != null && !sc.isEmpty()) {
            body.addView(text("自检明细", 12, 0xFF6F86A8, true, dp(4)));
            for (SelfCheckResult.Item it : sc.items()) {
                LinearLayout line = new LinearLayout(ctx);
                line.setOrientation(LinearLayout.HORIZONTAL);
                line.setPadding(0, dp(2), 0, dp(2));
                TextView mark = text(it.ok ? "✓" : "✗", 12,
                        it.ok ? 0xFF4CAF7D : 0xFFE05C5C, true, 0);
                mark.setPadding(0, 0, dp(6), 0);
                line.addView(mark);
                TextView t = text(it.name + (it.detail.length() > 0 ? "：" + it.detail : ""),
                        11, it.ok ? 0xFF6F86A8 : 0xFFC0504A, false, 0);
                line.addView(t, new LinearLayout.LayoutParams(0,
                        LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
                body.addView(line, matchWrap(0));
            }
        } else if (r.checkText != null && r.checkText.length() > 0) {
            addDetailLine(body, "自检", r.checkText
                    + (r.checkFail != null && r.checkFail.length() > 0 ? " · " + r.checkFail : " 通过"));
        } else {
            addDetailLine(body, "自检", "还没跑过自检");
        }

        if (r.firePoints != null && !r.firePoints.isEmpty()) {
            body.addView(text("触发点", 12, 0xFF6F86A8, true, dp(4)));
            for (Map.Entry<String, Integer> e : r.firePoints.entrySet()) {
                body.addView(text("· " + e.getKey() + "  " + e.getValue() + " 次",
                        11, 0xFF8496AE, false, dp(1)));
            }
        }

        if (r.item == null) {
            body.addView(text("这条是跨进程读到的快照，展开不到更细的项。",
                    10, 0xFFAAB6C6, false, dp(4)));
        }

        final Runnable after = new Runnable() {
            @Override public void run() { reload(); }
        };
        if (r.item != null) {
            CatDialog.custom(ctx, r.name, body, "重新自检", new Runnable() {
                @Override public void run() {
                    Bg.run(new Runnable() {
                        @Override public void run() {
                            try { r.item.runSelfCheck(); } catch (Throwable ignore) {}
                            new android.os.Handler(android.os.Looper.getMainLooper()).post(after);
                        }
                    }, "detail-check");
                }
            });
        } else {
            CatDialog.custom(ctx, r.name, body, "关闭", new Runnable() {
                @Override public void run() { }
            });
        }
    }

    private void addDetailLine(LinearLayout body, String label, String value) {
        TextView tv = new TextView(ctx);
        tv.setText(label + "：" + value);
        tv.setTextSize(11);
        tv.setTextColor(0xFF6F86A8);
        tv.setPadding(0, dp(2), 0, dp(2));
        tv.setTextIsSelectable(true);
        body.addView(tv, matchWrap(0));
    }

    

    private static final class Row {
        String group = "未分组";
        String path = "";
        String name = "";
        String statusName = FeatureStatus.UNKNOWN.name();
        String summary = "";
        String reason = "";
        int fireCount = 0;
        String fireText = "";
        boolean instrumented = false;
        String checkText = "";
        String checkFail = "";
        
        HookItemBase item;
        int hit = 0;
        int expect = 0;
        long lastFireAt = 0;
        int runtimeErrs = 0;
        String deepProblem = "";
        Map<String, Integer> firePoints;

        FeatureStatus status() {
            try {
                return FeatureStatus.valueOf(statusName);
            } catch (Throwable t) {
                return FeatureStatus.UNKNOWN;
            }
        }
    }

    private List<Row> collect() {
        List<Row> rows = new ArrayList<Row>();

        try {
            List<HookItemBase> all = HookItemFactory.all();
            for (HookItemBase item : all) {
                HookItem meta = item.meta();
                FeatureReport rep = item.report();
                if (meta == null || rep == null) continue;
                Row r = new Row();
                r.group = topGroup(meta.path());
                r.path = meta.path() == null ? "" : meta.path();
                r.name = meta.name();
                r.statusName = rep.status().name();
                r.summary = rep.summary();
                r.reason = rep.reason();
                r.fireCount = rep.fireCount();
                r.fireText = rep.fireSummary();
                r.instrumented = rep.isInstrumented();
                r.item = item;
                r.hit = rep.hitCount();
                r.expect = rep.expectCount();
                r.lastFireAt = rep.lastFireAt();
                r.firePoints = rep.firePoints();
                try { r.runtimeErrs = rep.runtimeErrorCount(); } catch (Throwable ignore) {}
                try { r.deepProblem = item.deepProblem(); } catch (Throwable ignore) {}
                try {
                    SelfCheckResult sc = item.selfCheck();
                    if (sc != null && !sc.isEmpty()) {
                        r.checkText = sc.passCount() + "/" + sc.total();
                        r.checkFail = sc.firstFailReason();
                    }
                } catch (Throwable ignore) {}
                rows.add(r);
            }
        } catch (Throwable ignore) {
        }
        if (!rows.isEmpty()) return rows;

        try {
            String txt = StatusStore.readHostStatus();
            if (txt != null) {
                for (String line : txt.split("\n")) {
                    if (line.startsWith("#") || line.trim().length() == 0) continue;
                    String[] f = line.split("\t", -1);
                    if (f.length < 6 || !"F".equals(f[0])) continue;
                    Row r = new Row();
                    r.group = topGroup(f[3]);
                    r.path = f[3];
                    r.name = f[2];
                    r.statusName = f[4];
                    r.summary = f[5];
                    r.reason = f.length > 6 ? f[6] : "";
                    if (f.length > 7) {
                        try { r.fireCount = Integer.parseInt(f[7].trim()); }
                        catch (Throwable ignore) {}
                    }
                    r.fireText = f.length > 8 ? f[8] : "";
                    r.instrumented = f.length > 9 && "1".equals(f[9].trim());
                    r.checkText = f.length > 10 ? f[10] : "";
                    r.checkFail = f.length > 11 ? f[11] : "";
                    rows.add(r);
                }
            }
        } catch (Throwable ignore) {
        }
        return rows;
    }

    private static String topGroup(String path) {
        if (path == null || path.length() == 0) return "未分组";
        int i = path.indexOf('/');
        return i > 0 ? path.substring(0, i) : path;
    }

    private static boolean isProblem(Row r) {
        FeatureStatus s = r.status();
        if (s == FeatureStatus.FAILED || s == FeatureStatus.ERROR
                || s == FeatureStatus.PARTIAL) return true;
        return r.checkFail != null && r.checkFail.length() > 0;
    }

    private static boolean isOff(Row r) {
        FeatureStatus s = r.status();
        return s == FeatureStatus.DISABLED || s == FeatureStatus.UNSUPPORTED;
    }

    private static boolean isUntested(Row r) {
        return r.checkText == null || r.checkText.length() == 0;
    }

    private static int weight(Row r) {
        if (isProblem(r)) return 0;
        if (isUntested(r)) return 1;
        if (isOff(r)) return 3;
        return 2;
    }

    private List<Row> filtered(List<Row> rows) {
        List<Row> out = new ArrayList<Row>();
        for (Row r : rows) {
            if (filter == F_ALL) out.add(r);
            else if (filter == F_PROBLEM && isProblem(r)) out.add(r);
            else if (filter == F_UNTESTED && isUntested(r)) out.add(r);
            else if (filter == F_OFF && isOff(r)) out.add(r);
        }
        return out;
    }

    private static Map<String, List<Row>> group(List<Row> rows) {
        Map<String, List<Row>> m = new LinkedHashMap<String, List<Row>>();
        for (Row r : rows) {
            List<Row> l = m.get(r.group);
            if (l == null) {
                l = new ArrayList<Row>();
                m.put(r.group, l);
            }
            l.add(r);
        }
        Comparator<Row> byWeight = new Comparator<Row>() {
            @Override public int compare(Row a, Row b) {
                int wa = weight(a), wb = weight(b);
                if (wa != wb) return wa - wb;
                return a.name.compareTo(b.name);
            }
        };
        List<Map.Entry<String, List<Row>>> entries =
                new ArrayList<Map.Entry<String, List<Row>>>(m.entrySet());
        for (Map.Entry<String, List<Row>> e : entries) {
            Collections.sort(e.getValue(), byWeight);
        }
        Collections.sort(entries, new Comparator<Map.Entry<String, List<Row>>>() {
            @Override public int compare(Map.Entry<String, List<Row>> a,
                                         Map.Entry<String, List<Row>> b) {
                int wa = weight(a.getValue().get(0));
                int wb = weight(b.getValue().get(0));
                return wa - wb;
            }
        });
        Map<String, List<Row>> out = new LinkedHashMap<String, List<Row>>();
        for (Map.Entry<String, List<Row>> e : entries) out.put(e.getKey(), e.getValue());
        return out;
    }

    

    
    private TextView statusPill(String s, int color) {
        TextView tv = new TextView(ctx);
        tv.setText(s);
        tv.setTextSize(10);
        tv.setTextColor(color);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(dp(8), dp(3), dp(8), dp(3));
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(20));
        bg.setColor((color & 0x00FFFFFF) | 0x1F000000);
        tv.setBackgroundDrawable(bg);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        p.rightMargin = dp(6);
        tv.setLayoutParams(p);
        return tv;
    }

    private String hhmmOnce() {
        return hhmm(System.currentTimeMillis());
    }

    private static String hhmm(long ms) {
        try {
            return new SimpleDateFormat("HH:mm", Locale.US).format(new Date(ms));
        } catch (Throwable t) {
            return "-";
        }
    }

    public static String pageKey() {
        return PAGE;
    }

    private android.graphics.drawable.Drawable clickRipple() {
        android.graphics.drawable.RippleDrawable rd =
                new android.graphics.drawable.RippleDrawable(
                        android.content.res.ColorStateList.valueOf(0x337FA5E6),
                        null, null);
        return rd;
    }
}
