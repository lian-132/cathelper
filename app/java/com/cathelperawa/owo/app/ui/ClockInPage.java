package com.cathelperawa.owo.app.ui;

import android.graphics.drawable.GradientDrawable;
import com.cathelperawa.owo.app.core.Bg;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.cathelperawa.owo.app.bridge.TroopApi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ClockInPage extends Page {

    public static final String K_GROUPS = "chat_clockin_groups";

    public static final String K_DEADLINE = "chat_clockin_deadline_sec";

    public static final String K_CONC = "chat_clockin_concurrency";

    public static final String K_DONE_DATE = "chat_clockin_done_date";

    public static final String K_DONE = "chat_clockin_done";

    private static final int[] DEADLINE_CHOICES = {1, 2, 3, 5, 10, 0};

    private static final int[] CONC_CHOICES = {8, 16, 32, 64, 128, 0};

    public ClockInPage(Host host) { super(host); }

    private final java.util.List<TextView> rowBadges = new java.util.ArrayList<TextView>();
    private final java.util.List<String> rowUins = new java.util.ArrayList<String>();

    private boolean polling = false;

    private static String query = "";

    private static boolean warmed = false;

    private Set<String> doneTodaySet(Set<String> selected) {
        Set<String> out = new LinkedHashSet<String>();
        try {
            String date = cfgGetS(K_DONE_DATE, "");
            if (!todayStr().equals(date)) return out;
            String raw = cfgGetS(K_DONE, "");
            if (raw == null || raw.length() == 0) return out;
            for (String p : raw.split(",")) {
                String t = p.trim();
                if (t.length() > 0 && (selected == null || selected.contains(t))) out.add(t);
            }
        } catch (Throwable ignore) {
        }
        return out;
    }

    private static String todayStr() {
        return new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                .format(new java.util.Date());
    }

    private static String doneText(int done, int total) {
        return "已打卡 " + done + " / " + total;
    }

    private TextView doneBadge(boolean done) {
        TextView v = new TextView(ctx);
        v.setGravity(Gravity.CENTER);
        v.setTextSize(12);
        paintBadge(v, done);
        return v;
    }

    private void paintBadge(TextView v, boolean done) {
        if (v == null) return;
        v.setText(done ? "✓" : "");
        v.setTextColor(0xFFFFFFFF);
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        g.setColor(done ? 0xFF6FD39B : 0x22FFFFFF);
        g.setStroke(Math.max(1, dp(1)), done ? 0xFF4FBE83 : 0x55FFFFFF);
        v.setBackgroundDrawable(g);
    }

    private void startPolling(final android.view.View root, final Set<String> sel,
                              final TextView donePill) {
        if (polling) return;
        polling = true;
        final Handler h = new Handler(Looper.getMainLooper());
        final Runnable[] task = new Runnable[1];
        task[0] = new Runnable() {
            @Override public void run() {
                if (root.getWindowToken() == null) {
                    polling = false;
                    return;
                }
                try {
                    Set<String> done = doneTodaySet(sel);
                    if (donePill != null) donePill.setText(doneText(done.size(), sel.size()));
                    for (int i = 0; i < rowBadges.size() && i < rowUins.size(); i++) {
                        paintBadge(rowBadges.get(i), done.contains(rowUins.get(i)));
                    }
                } catch (Throwable ignore) {
                }
                h.postDelayed(task[0], 2000);
            }
        };
        h.postDelayed(task[0], 2000);
    }

    private interface SegPick {
        void onPick(int index);
    }

    private LinearLayout segRow(String[] names, final TextView[] out, final SegPick pick) {
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        for (int i = 0; i < names.length; i++) {
            final int idx = i;
            TextView chip = text(names[i], 13, 0xFF7F90A6, true, 0);
            chip.setGravity(Gravity.CENTER);
            chip.setPadding(0, dp(9), 0, dp(9));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            if (i < names.length - 1) lp.rightMargin = dp(8);
            chip.setLayoutParams(lp);
            chip.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    press(v);
                    pick.onPick(idx);
                }
            });
            out[i] = chip;
            row.addView(chip);
        }
        return row;
    }

    private void paintSeg(TextView[] chips, int sel) {
        for (int i = 0; i < chips.length; i++) {
            boolean on = i == sel;
            GradientDrawable g = new GradientDrawable();
            g.setCornerRadius(dp(14));
            g.setColor(on ? 0xFFFFC2DF : 0x66FFFFFF);
            g.setStroke(dp(1), on ? 0xFFFF9FCB : 0x33FFFFFF);
            chips[i].setBackgroundDrawable(g);
            chips[i].setTextColor(on ? 0xFFE85D9C : 0xFF7F90A6);
        }
    }

    private static int indexOf(int[] vals, int v) {
        for (int i = 0; i < vals.length; i++) {
            if (vals[i] == v) return i;
        }
        return 0;
    }

    int deadlineSec() {
        try {
            return Integer.parseInt(cfgGetS(K_DEADLINE, "3"));
        } catch (Throwable t) {
            return 3;
        }
    }

    int concurrency() {
        try {
            return Integer.parseInt(cfgGetS(K_CONC, "64"));
        } catch (Throwable t) {
            return 64;
        }
    }

    private static String deadlineLabel(int sec) {
        return sec <= 0 ? "不限速" : (sec + " 秒内打完");
    }

    private static String concLabel(int n) {
        return n <= 0 ? "并发 全部" : ("并发 " + n);
    }

    private Set<String> selected() {
        Set<String> s = new LinkedHashSet<String>();
        String raw = cfgGetS(K_GROUPS, "");
        if (raw == null) return s;
        for (String p : raw.split(",")) {
            String t = p.trim();
            if (t.length() > 0) s.add(t);
        }
        return s;
    }

    private void save(Set<String> s) {
        StringBuilder sb = new StringBuilder();
        for (String u : s) {
            if (sb.length() > 0) sb.append(',');
            sb.append(u);
        }
        cfgSetS(K_GROUPS, sb.toString());
    }

    private void warmTroops() {
        if (warmed) return;
        warmed = true;
        Bg.run(new Runnable() {
            @Override public void run() {
                try {
                    TroopApi.allTroopNames();
                } catch (Throwable ignore) {
                }
                new android.os.Handler(android.os.Looper.getMainLooper()).post(new Runnable() {
                    @Override public void run() {
                        try { host.setPageView(build()); } catch (Throwable ignore) {}
                    }
                });
            }
        }, "cat-clockin-warm");
    }

    @Override
    public View build() {
        polling = false;
        View[] out = new View[2];
        LinearLayout root = pageRoot("群打卡", "选好群，到点自己去打卡", out, true, 12);

        final Set<String> sel = selected();

        LinearLayout head = whiteCard(dp(20), 0xD9FFFFFF);
        head.setPadding(dp(16), dp(14), dp(16), dp(14));

        LinearLayout headTop = new LinearLayout(ctx);
        headTop.setOrientation(LinearLayout.HORIZONTAL);
        headTop.setGravity(Gravity.CENTER_VERTICAL);
        final TextView count = pill(stateText(sel.size()), sel.isEmpty() ? 0xFF7FA5E6 : 0xFFF07CA8);
        if (!sel.isEmpty()) ((GradientDrawable) count.getBackground()).setColor(0xFFFFF0F6);
        headTop.addView(count, wrapWrap());

        final Set<String> doneSet = doneTodaySet(sel);
        final TextView donePill = pill(doneText(doneSet.size(), sel.size()),
                doneSet.isEmpty() ? 0xFF7FA5E6 : 0xFF3E9A6B);
        if (!doneSet.isEmpty()) {
            ((GradientDrawable) donePill.getBackground()).setColor(0xFFD6F5E3);
        }
        LinearLayout.LayoutParams dpLp = wrapWrap();
        dpLp.leftMargin = dp(8);
        headTop.addView(donePill, dpLp);

        LinearLayout tools = new LinearLayout(ctx);
        tools.setOrientation(LinearLayout.HORIZONTAL);
        tools.setGravity(Gravity.END);
        final TextView all = smallBtn("全选");
        final TextView none = smallBtn("清空");
        tools.addView(all, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, dp(28)));
        LinearLayout.LayoutParams gap = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, dp(28));
        gap.leftMargin = dp(8);
        tools.addView(none, gap);
        headTop.addView(tools, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        head.addView(headTop, matchWrap(dp(9)));

        head.addView(text("打卡时间：每天 00:00 起（零点窗口优先抢首签）",
                11, 0xFF8496AE, false, 0));

        final int[] deadlineVals = {1, 3, 5, 0};
        final String[] deadlineNames = {"1 秒", "3 秒", "5 秒", "不限"};
        final TextView[] dlChips = new TextView[deadlineVals.length];
        head.addView(text("零点要在多久内打完", 11, 0xFF7F90A6, true, dp(6)));
        head.addView(segRow(deadlineNames, dlChips, new SegPick() {
            @Override public void onPick(int i) {
                cfgSetS(K_DEADLINE, String.valueOf(deadlineVals[i]));
                paintSeg(dlChips, i);
                toast(i == deadlineVals.length - 1 ? "不限速喵～" : ("要在 " + deadlineNames[i] + " 内打完"));
            }
        }), matchWrap(dp(10)));
        int dlIdx = indexOf(deadlineVals, deadlineSec());
        paintSeg(dlChips, dlIdx);
        cfgSetS(K_DEADLINE, String.valueOf(deadlineVals[dlIdx]));

        final int[] concVals = {16, 32, 64, 0};
        final String[] concNames = {"16", "32", "64", "全部"};
        final TextView[] ccChips = new TextView[concVals.length];
        head.addView(text("同时发几个群", 11, 0xFF7F90A6, true, dp(6)));
        head.addView(segRow(concNames, ccChips, new SegPick() {
            @Override public void onPick(int i) {
                cfgSetS(K_CONC, String.valueOf(concVals[i]));
                paintSeg(ccChips, i);
                toast(i == concVals.length - 1 ? "全部群同时发喵～" : ("同时发 " + concNames[i] + " 个群"));
            }
        }), matchWrap(0));
        int ccIdx = indexOf(concVals, concurrency());
        paintSeg(ccChips, ccIdx);
        cfgSetS(K_CONC, String.valueOf(concVals[ccIdx]));

        root.addView(head, matchWrap(dp(11)));

        final LinearLayout listBox = new LinearLayout(ctx);
        listBox.setOrientation(LinearLayout.VERTICAL);

        final EditText search = UiKit.pastelEdit(ctx, query, "搜群名或群号");
        root.addView(search, matchWrap(dp(9)));
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable e) {
                String q = e.toString();
                if (q.equals(query)) return;
                query = q;

                refill(listBox, sel, count, donePill);
            }
        });

        root.addView(listBox, matchWrap(dp(8)));

        all.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                press(v);
                sel.clear();
                sel.addAll(visibleUins());
                save(sel);
                toast("已选 " + sel.size() + " 个群喵～");
                host.setPageView(build());
            }
        });
        none.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                press(v);
                sel.clear();
                save(sel);
                toast("已清空喵～");
                host.setPageView(build());
            }
        });

        refill(listBox, sel, count, donePill);
        startPolling(listBox, sel, donePill);
        warmTroops();
        fadeIn(root);
        return out[0];
    }

    private static String stateText(int n) {
        return n == 0 ? "还没选群" : ("已选 " + n + " 个群");
    }

    private static List<String[]> troops() {
        List<String[]> list = new ArrayList<String[]>();
        try {
            Map<String, String> all = TroopApi.allTroopNames();
            if (all != null) {
                for (Map.Entry<String, String> e : all.entrySet()) {
                    String uin = e.getKey();
                    String name = e.getValue();
                    if (uin == null || uin.trim().length() == 0) continue;
                    if (name == null || name.trim().length() == 0) name = uin;
                    list.add(new String[]{uin, name});
                }
            }
        } catch (Throwable ignore) {
        }
        Collections.sort(list, new Comparator<String[]>() {
            @Override public int compare(String[] a, String[] b) {
                return a[1].compareTo(b[1]);
            }
        });
        return list;
    }

    private static List<String> visibleUins() {
        List<String> r = new ArrayList<String>();
        String q = query == null ? "" : query.trim().toLowerCase();
        for (String[] t : troops()) {
            if (q.length() == 0
                    || t[1].toLowerCase().contains(q)
                    || t[0].contains(q)) {
                r.add(t[0]);
            }
        }
        return r;
    }

    private void refill(LinearLayout box, final Set<String> sel, final TextView count,
                        final TextView donePill) {
        final Set<String> doneSet = doneTodaySet(sel);
        rowBadges.clear();
        rowUins.clear();
        box.removeAllViews();
        String q = query == null ? "" : query.trim().toLowerCase();
        List<String[]> all = troops();
        int shown = 0;
        for (final String[] t : all) {
            if (q.length() > 0 && !t[1].toLowerCase().contains(q) && !t[0].contains(q)) continue;
            shown++;

            LinearLayout row = whiteCard(dp(16), 0xCCFFFFFF);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(14), dp(11), dp(14), dp(11));

            LinearLayout col = new LinearLayout(ctx);
            col.setOrientation(LinearLayout.VERTICAL);
            col.addView(text(t[1], 15, 0xFF43587A, true, dp(1)));
            col.addView(text(t[0], 11, 0xFF8496AE, false, 0));
            row.addView(col, new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            TextView badge = doneBadge(doneSet.contains(t[0]));
            LinearLayout.LayoutParams badgeLp = new LinearLayout.LayoutParams(dp(22), dp(22));
            badgeLp.rightMargin = dp(8);
            row.addView(badge, badgeLp);
            rowBadges.add(badge);
            rowUins.add(t[0]);

            final boolean[] on = {sel.contains(t[0])};
            final TextView box2 = checkBox(on[0]);
            row.addView(box2, new LinearLayout.LayoutParams(dp(24), dp(24)));

            final Runnable toggle = new Runnable() {
                @Override public void run() {
                    on[0] = !on[0];
                    if (on[0]) sel.add(t[0]); else sel.remove(t[0]);
                    paintCheck(box2, on[0]);
                    save(sel);
                    count.setText(stateText(sel.size()));
                    if (donePill != null) donePill.setText(doneText(doneSet.size(), sel.size()));
                    ((GradientDrawable) count.getBackground())
                            .setColor(sel.isEmpty() ? 0xFFEFF4FF : 0xFFFFF0F6);
                    count.setTextColor(sel.isEmpty() ? 0xFF7FA5E6 : 0xFFF07CA8);
                }
            };
            row.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    press(v);
                    toggle.run();
                }
            });
            box2.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    press(v);
                    toggle.run();
                }
            });

            LinearLayout.LayoutParams lp = matchWrap(dp(8));
            box.addView(row, lp);
        }

        if (shown == 0) {
            LinearLayout empty = whiteCard(dp(16), 0x99FFFFFF);
            empty.setPadding(dp(14), dp(16), dp(14), dp(16));
            empty.addView(text(all.isEmpty() ? "还没读到群列表…等一下下" : "没搜到匹配的群",
                    13, 0xFF8496AE, false, 0));
            box.addView(empty, matchWrap(dp(8)));
        }
    }

    private TextView checkBox(boolean on) {
        TextView v = new TextView(ctx);
        v.setGravity(Gravity.CENTER);
        v.setTextSize(14);
        paintCheck(v, on);
        return v;
    }

    private void paintCheck(TextView v, boolean on) {
        v.setText(on ? "✓" : "");
        v.setTextColor(0xFFFFFFFF);
        GradientDrawable d = new GradientDrawable();
        d.setCornerRadius(dp(7));
        if (on) {
            d.setColor(0xFFF07CA8);
            d.setStroke(dp(1.4f), 0xFFF07CA8);
        } else {
            d.setColor(0x00FFFFFF);
            d.setStroke(dp(1.4f), 0xFFBFD0EC);
        }
        v.setBackgroundDrawable(d);
    }

    private TextView smallBtn(String label) {
        TextView v = new TextView(ctx);
        v.setText(label);
        v.setTextSize(12);
        v.setGravity(Gravity.CENTER);
        v.setPadding(dp(12), 0, dp(12), 0);
        v.setTextColor(0xFF6F86A8);
        GradientDrawable d = new GradientDrawable();
        d.setCornerRadius(dp(14));
        d.setColor(0xCCFFFFFF);
        d.setStroke(dp(1), 0xFFD7E3F5);
        v.setBackgroundDrawable(d);
        return v;
    }
}
