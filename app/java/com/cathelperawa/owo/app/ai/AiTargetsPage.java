package com.cathelperawa.owo.app.ai;

import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.cathelperawa.owo.app.api.IdentityApi;
import com.cathelperawa.owo.app.bridge.TroopApi;
import com.cathelperawa.owo.app.ui.Page;
import com.cathelperawa.owo.app.ui.UiKit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class AiTargetsPage extends Page {

    private static final int C_MAIN = 0xFF3C4B60;
    private static final int C_SUB = 0xFF8A9BB0;
    private static final int C_PINK = 0xFFE85D9C;

    private static final int MAX_ROWS = 120;

    private LinearLayout body;
    private LinearLayout listBox;
    private TextView countLabel;

    private String query = "";

    private boolean onlyPicked = false;
    private TextView pickedEntry = null;

    private boolean groupOpen = true;
    private boolean friendOpen = true;
    private final Set<String> sel = new LinkedHashSet<String>();

    private List<String[]> groups = new ArrayList<String[]>();
    private List<String[]> friends = new ArrayList<String[]>();

    public AiTargetsPage(Host host) {
        super(host);
    }

    @Override
    public View build() {
        View[] out = new View[2];
        body = pageRoot("允许的会话", "哪些群 / 好友能跟它说话", out, true, 13, "bg_ai", 0x66);

        sel.clear();
        String raw = AiConfig.targetsRaw();
        if (raw != null && raw.trim().length() > 0) {
            String[] ts = raw.split("[,，\\s|｜]+");
            for (int i = 0; i < ts.length; i++) {
                String t = ts[i].trim();
                if (t.length() > 0) sel.add(t);
            }
        }

        loadData();

        LinearLayout head = whiteCard(dp(20), 0xF2FFFFFF);
        head.setOrientation(LinearLayout.VERTICAL);
        head.setPadding(dp(16), dp(14), dp(16), dp(14));

        LinearLayout top = new LinearLayout(ctx);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);

        countLabel = text(stateText(), 14, C_MAIN, true, 0);
        top.addView(countLabel, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        pickedEntry = text("查看已勾选 ›", 12, C_PINK, true, 0);
        pickedEntry.setPadding(dp(8), dp(2), dp(8), dp(2));
        press(pickedEntry);
        pickedEntry.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                onlyPicked = !onlyPicked;
                groupOpen = true;
                friendOpen = true;
                updateHeader();
                refill();
            }
        });
        top.addView(pickedEntry, wrapWrap());

        TextView clear = pill("清空", 0xFFFFD9E6);
        clear.setTextColor(C_PINK);
        press(clear);
        clear.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                sel.clear();
                save();
                updateHeader();
                refill();
                toast("已清空（= 所有会话都允许）");
            }
        });
        top.addView(clear, wrapWrap());
        head.addView(top, matchWrap(dp(4)));

        head.addView(text("一个都不选 = 不限制，所有会话都能聊",
                10.5f, C_SUB, false, 0), matchWrap(0));
        body.addView(head, matchWrap(dp(12)));

        final EditText search = UiKit.pastelEdit(ctx, "", "搜名字 / 群号 / QQ 号");
        body.addView(search, matchWrap(dp(10)));
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable e) {
                String q = e.toString();
                if (q.equals(query)) return;
                query = q;
                refill();
            }
        });

        listBox = new LinearLayout(ctx);
        listBox.setOrientation(LinearLayout.VERTICAL);
        body.addView(listBox, matchWrap(0));

        updateHeader();
        refill();
        return out[0];
    }

    private void loadData() {
        groups = new ArrayList<String[]>();
        friends = new ArrayList<String[]>();
        try {
            Map<String, String> m = TroopApi.allTroopNames();
            if (m != null) {
                for (Map.Entry<String, String> e : m.entrySet()) {
                    String id = e.getKey();
                    if (id == null || id.trim().length() == 0) continue;
                    String name = e.getValue();
                    if (name == null || name.trim().length() == 0) name = id;
                    groups.add(new String[]{id.trim(), name.trim(), id.trim()});
                }
            }
        } catch (Throwable ignore) {
        }
        try {
            Map<String, String> m = IdentityApi.allBuddies();
            if (m != null) {
                for (Map.Entry<String, String> e : m.entrySet()) {
                    String uid = e.getKey();
                    if (uid == null || uid.trim().length() == 0) continue;
                    String name = e.getValue();
                    if (name == null || name.trim().length() == 0) name = uid;

                    String uin = null;
                    try {
                        uin = IdentityApi.uinFromUid(uid);
                    } catch (Throwable ignore) {
                    }
                    String id = uin != null && uin.trim().length() > 0 ? uin.trim() : uid.trim();
                    friends.add(new String[]{id, name.trim(), uid.trim()});
                }
            }
        } catch (Throwable ignore) {
        }
        Comparator<String[]> byName = new Comparator<String[]>() {
            @Override public int compare(String[] a, String[] b) {
                return a[1].compareTo(b[1]);
            }
        };
        Collections.sort(groups, byName);
        Collections.sort(friends, byName);
    }

    private boolean hit(String[] it) {
        String q = query == null ? "" : query.trim().toLowerCase();
        if (q.length() == 0) return true;
        if (it[1].toLowerCase().contains(q)) return true;
        if (it[0].contains(q)) return true;
        return it.length > 2 && it[2].contains(q);
    }

    private void updateHeader() {
        if (countLabel != null) countLabel.setText(stateText());
        if (pickedEntry != null) {
            pickedEntry.setText(onlyPicked
                    ? "← 看全部"
                    : "查看已勾选（" + sel.size() + "）›");
        }
    }

    private String stateText() {
        return sel.isEmpty() ? "未选择（不限制）" : ("已选 " + sel.size() + " 个会话");
    }

    private void refill() {
        if (listBox == null) return;
        listBox.removeAllViews();

        List<String[]> g = new ArrayList<String[]>();
        for (int i = 0; i < groups.size(); i++) {
            String[] it = groups.get(i);
            if (onlyPicked && !sel.contains(it[0])) continue;
            if (hit(it)) g.add(it);
        }
        List<String[]> f = new ArrayList<String[]>();
        for (int i = 0; i < friends.size(); i++) {
            String[] it = friends.get(i);
            if (onlyPicked && !sel.contains(it[0])) continue;
            if (hit(it)) f.add(it);
        }

        List<String[]> extra = new ArrayList<String[]>();
        if (onlyPicked) {
            for (String id : sel) {
                boolean found = false;
                for (int i = 0; i < groups.size() && !found; i++) {
                    if (groups.get(i)[0].equals(id)) found = true;
                }
                for (int i = 0; i < friends.size() && !found; i++) {
                    if (friends.get(i)[0].equals(id)) found = true;
                }
                if (!found && hit(new String[]{id, id, id})) {
                    extra.add(new String[]{id, "（不在群/好友列表里）", id});
                }
            }
        }

        if (g.isEmpty() && f.isEmpty() && extra.isEmpty()) {
            LinearLayout empty = whiteCard(dp(18), 0xF2FFFFFF);
            empty.setPadding(dp(16), dp(18), dp(16), dp(18));
            String msg;
            if (onlyPicked && sel.isEmpty()) msg = "还没勾选任何会话（= 不限制，所有会话都能聊）";
            else if (query.length() > 0) msg = "没搜到匹配的";
            else msg = "还没读到群 / 好友列表，进 TIM 里翻一下聊天再回来";
            empty.addView(text(msg, 12, C_SUB, false, 0), matchWrap(0));
            listBox.addView(empty, matchWrap(dp(12)));
            return;
        }

        int drawn = 0;
        if (!g.isEmpty()) {
            listBox.addView(sectionRow("群", g.size(), groupOpen, true), matchWrap(dp(6)));
            if (groupOpen) {
                for (int i = 0; i < g.size() && drawn < MAX_ROWS; i++, drawn++) {
                    listBox.addView(row(g.get(i)[0], g.get(i)[1], "群"), matchWrap(dp(6)));
                }
            }
        }
        if (!f.isEmpty()) {
            listBox.addView(sectionRow("好友", f.size(), friendOpen, false), matchWrap(dp(10)));
            if (friendOpen) {
                for (int i = 0; i < f.size() && drawn < MAX_ROWS; i++, drawn++) {
                    listBox.addView(row(f.get(i)[0], f.get(i)[1], "好友"), matchWrap(dp(6)));
                }
            }
        }
        if (!extra.isEmpty()) {
            listBox.addView(sectionRow("其他（手填 / 已不在列表）", extra.size(), true, false),
                    matchWrap(dp(10)));
            for (int i = 0; i < extra.size(); i++) {
                listBox.addView(row(extra.get(i)[0], extra.get(i)[1], "已选"), matchWrap(dp(6)));
            }
        }
        if (drawn >= MAX_ROWS) {
            TextView tip = text("列表太长，只画了前 " + MAX_ROWS + " 个 —— 上面搜索框输入关键词找剩下的",
                    10.5f, C_SUB, false, 0);
            tip.setPadding(dp(6), dp(8), dp(6), dp(10));
            listBox.addView(tip, matchWrap(0));
        }
    }

    private View sectionRow(String name, final int count, final boolean open,
                            final boolean isGroup) {
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(6), dp(8), dp(6), dp(6));

        TextView arrow = text(open ? "▾" : "▸", 13, 0xFF7C8DB5, true, 0);
        row.addView(arrow, wrapWrap());

        TextView t = text(name + " · " + count, 12, C_SUB, true, 0);
        t.setPadding(dp(6), 0, 0, 0);
        row.addView(t, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView hint = text(open ? "收起" : "展开", 10.5f, 0xFFB6C1D0, false, 0);
        row.addView(hint, wrapWrap());

        press(row);
        row.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (isGroup) groupOpen = !open;
                else friendOpen = !open;
                refill();
            }
        });
        return row;
    }

    private View row(final String id, final String name, String kind) {
        final boolean on = sel.contains(id);

        LinearLayout r = new LinearLayout(ctx);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setPadding(dp(13), dp(11), dp(13), dp(11));

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(13));
        bg.setColor(on ? 0x33FFC2DF : 0x40FFFFFF);
        bg.setStroke(Math.max(1, dp(1)), on ? 0x88FF9FCB : 0x22A9BEDC);
        r.setBackgroundDrawable(bg);

        LinearLayout txt = new LinearLayout(ctx);
        txt.setOrientation(LinearLayout.VERTICAL);
        txt.addView(text(name, 13.5f, C_MAIN, true, dp(2)), matchWrap(0));
        txt.addView(text(kind + " · " + id, 10.5f, C_SUB, false, 0), matchWrap(0));
        r.addView(txt, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView dot = new TextView(ctx);
        dot.setText(on ? "✓" : "");
        dot.setTextSize(10);
        dot.setTextColor(0xFFFFFFFF);
        dot.setGravity(Gravity.CENTER);
        GradientDrawable dg = new GradientDrawable();
        dg.setShape(GradientDrawable.OVAL);
        dg.setColor(on ? C_PINK : 0x00000000);
        dg.setStroke(Math.max(1, dp(1.5f)), on ? C_PINK : 0xFFC3CEDA);
        dot.setBackgroundDrawable(dg);
        r.addView(dot, new LinearLayout.LayoutParams(dp(20), dp(20)));

        press(r);
        r.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (sel.contains(id)) sel.remove(id);
                else sel.add(id);
                save();
                updateHeader();
                refill();
            }
        });
        return r;
    }

    private void save() {
        StringBuilder sb = new StringBuilder();
        for (String s : sel) {
            if (sb.length() > 0) sb.append(",");
            sb.append(s);
        }
        AiConfig.setString("ai_targets", sb.toString());
    }
}
