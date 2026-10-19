package com.cathelperawa.owo.app.ui;

import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.cathelperawa.owo.app.core.FeatureReport;
import com.cathelperawa.owo.app.core.FeatureStatus;
import com.cathelperawa.owo.app.core.HookItem;
import com.cathelperawa.owo.app.core.HookItemBase;
import com.cathelperawa.owo.app.core.HookItemFactory;
import com.cathelperawa.owo.app.core.StatusStore;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class StatusPage extends Page {

    private static final String PAGE = "status";

    public StatusPage(Host host) { super(host); }

    @Override
    public View build() {

        final View[] out = new View[4];
        pageRootEx("功能状态", "正在读取…", out);

        LinearLayout topbar = (LinearLayout) out[2];
        TextView refresh = text("刷新", 12, 0xFF7FA5E6, true, 0);
        refresh.setPadding(dp(10), dp(6), dp(10), dp(6));
        refresh.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                press(v);
                reload();
            }
        });
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        topbar.addView(refresh, rp);

        TextView checkBtn = text("自检", 12, 0xFF7FA5E6, true, 0);
        checkBtn.setPadding(dp(10), dp(6), dp(10), dp(6));
        checkBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                press(v);
                runAllChecks();
            }
        });
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        topbar.addView(checkBtn, cp);

        TextView exportBtn = text("诊断包", 12, 0xFF7FA5E6, true, 0);
        exportBtn.setPadding(dp(10), dp(6), dp(10), dp(6));
        exportBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                press(v);
                exportReport();
            }
        });
        topbar.addView(exportBtn, cp);

        contentBox = (LinearLayout) out[1];
        subtitle = (TextView) out[3];

        reload();
        return out[0];
    }

    private LinearLayout contentBox;
    private TextView subtitle;

    private void runAllChecks() {
        if (subtitle != null) subtitle.setText("正在自检…");
        new Thread(new Runnable() {
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
                android.os.Handler ui = new android.os.Handler(
                        android.os.Looper.getMainLooper());
                ui.post(new Runnable() {
                    @Override public void run() {
                        if (subtitle != null && done > 0) {
                            subtitle.setText("已自检 " + done + " 个功能");
                        }
                        reload();
                    }
                });
            }
        }, "self-check").start();
    }

    private void exportReport() {
        if (subtitle != null) subtitle.setText("正在打包诊断包…");
        new Thread(new Runnable() {
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
        }, "report-export").start();
    }

    private void reload() {        if (contentBox == null) return;
        if (subtitle != null) subtitle.setText("正在读取…");

        final android.os.Handler ui = new android.os.Handler(
                android.os.Looper.getMainLooper());

        new Thread(new Runnable() {
            @Override public void run() {
                final List<Row> rows = collect();
                ui.post(new Runnable() {
                    @Override public void run() {
                        render(rows);
                    }
                });
            }
        }, "status-reader").start();
    }

    private void render(List<Row> rows) {

        while (contentBox.getChildCount() > 1) {
            contentBox.removeViewAt(contentBox.getChildCount() - 1);
        }

        if (subtitle != null) {
            subtitle.setText(rows.isEmpty()
                    ? "没有读到状态数据"
                    : rows.size() + " 个功能 · 更新于 TIM 最近一次启动");
        }

        contentBox.addView(buildSummary(rows), matchWrap(dp(12)));

        if (rows.isEmpty()) {
            LinearLayout card = whiteCard(dp(20), 0xE6FFFFFF);
            card.setPadding(dp(18), dp(18), dp(18), dp(18));
            card.addView(text("还没有状态数据", 15, 0xFF43587A, true, dp(6)));
            card.addView(text("打开 TIM 一次，等模块加载完成后点右上角「刷新」。",
                    12, 0xFF8496AE, false, dp(4)));
            card.addView(text("若 TIM 已打开仍读不到：控制台需要 root / su 才有权限\n"
                            + "读 TIM 私有目录里的状态文件。",
                    11, 0xFFAAB6C6, false, 0));
            contentBox.addView(card, matchWrap(dp(11)));
            return;
        }

        Map<String, List<Row>> groups = group(rows);
        for (Map.Entry<String, List<Row>> e : groups.entrySet()) {
            contentBox.addView(buildGroup(e.getKey(), e.getValue()), matchWrap(dp(11)));
        }

        TextView tip = text("数据来自宿主进程 · 点「刷新」可重新读取",
                11, 0xFFAAB6C6, false, 0);
        tip.setGravity(Gravity.CENTER);
        tip.setPadding(0, dp(6), 0, dp(10));
        contentBox.addView(tip, matchWrap(0));
    }

    private static final class Row {
        String group = "未分组";
        String name = "";
        String statusName = FeatureStatus.UNKNOWN.name();
        String summary = "";
        String reason = "";

        int fireCount = 0;

        String fireText = "";

        boolean instrumented = false;

        String checkText = "";

        String checkFail = "";

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
                HookItem meta = item.rawMeta();
                FeatureReport rep = item.report();
                if (meta == null || rep == null) continue;
                Row r = new Row();
                r.group = topGroup(item.displayPath());
                r.name = item.displayName();
                r.statusName = rep.status().name();
                r.summary = rep.summary();
                r.reason = rep.reason();
                r.fireCount = rep.fireCount();
                r.fireText = rep.fireSummary();
                r.instrumented = rep.isInstrumented();
                try {
                    com.cathelperawa.owo.app.core.SelfCheckResult sc = item.selfCheck();
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
        return m;
    }

    private View buildSummary(List<Row> rows) {
        int[] cnt = new int[FeatureStatus.values().length];
        for (Row r : rows) cnt[r.status().ordinal()]++;

        LinearLayout card = whiteCard(dp(22), 0xE6FFFFFF);
        card.setPadding(dp(18), dp(16), dp(18), dp(16));

        card.addView(text("本期体检", 12, 0xFF9FB0C7, false, dp(2)));
        card.addView(text("" + rows.size() + " 个功能",
                26, 0xFF43587A, true, dp(10)));

        LinearLayout pills = new LinearLayout(ctx);
        pills.setOrientation(LinearLayout.HORIZONTAL);
        for (FeatureStatus s : new FeatureStatus[]{
                FeatureStatus.OK, FeatureStatus.PARTIAL,
                FeatureStatus.FAILED, FeatureStatus.ERROR, FeatureStatus.DISABLED}) {
            int n = cnt[s.ordinal()];
            if (n == 0 && s != FeatureStatus.OK) continue;
            pills.addView(statusPill(s.label + " " + n, s.color));
        }
        card.addView(pills, matchWrap(0));
        return card;
    }

    private View buildGroup(String groupName, List<Row> items) {
        LinearLayout card = whiteCard(dp(20), 0xE6FFFFFF);
        card.setPadding(dp(16), dp(14), dp(16), dp(10));

        int problems = 0;
        for (Row r : items) if (r.status().isProblem()) problems++;

        LinearLayout head = new LinearLayout(ctx);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.addView(text(groupName, 15, 0xFF43587A, true, 0),
                new LinearLayout.LayoutParams(0,
                        LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        if (problems > 0) {
            head.addView(statusPill(problems + " 项异常", 0xFFE05C5C));
        } else {
            head.addView(statusPill("全部正常", 0xFF4CAF7D));
        }
        card.addView(head, matchWrap(dp(8)));

        for (Row r : items) {
            card.addView(buildRow(r), matchWrap(dp(4)));
        }
        return card;
    }

    private View buildRow(Row r) {
        FeatureStatus st = r.status();

        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0x66FFFFFF);
        bg.setCornerRadius(dp(12));
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
        LinearLayout.LayoutParams dp2 = new LinearLayout.LayoutParams(dp(8), dp(8));
        dp2.rightMargin = dp(7);
        line1.addView(dot, dp2);

        line1.addView(text(r.name, 13, 0xFF43587A, true, 0),
                new LinearLayout.LayoutParams(0,
                        LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        line1.addView(text(st.label, 11, st.color, true, 0));
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
        return row;
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

    public static String pageKey() {
        return PAGE;
    }
}
