package com.cathelperawa.owo.app.ui;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.cathelperawa.owo.app.MsgStore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ScopePage extends Page {

    public ScopePage(Host host) { super(host); }

    private static boolean healed = false;

    private void healFriendNames() {
        if (healed) return;
        healed = true;
        final Map<String, String> peers = MsgStore.peers();
        boolean need = false;
        for (Map.Entry<String, String> e : peers.entrySet()) {
            String v = e.getValue();
            if (v != null && (v.startsWith("[友] ") || v.matches("\\[群\\] \\d{5,}"))) {
                need = true;
                break;
            }
        }
        if (!need) return;
        new Thread(new Runnable() {
            @Override public void run() {
                boolean changed = false;
                try {

                    for (Map.Entry<String, String> e : peers.entrySet()) {
                        final String uid = e.getKey();
                        String label = e.getValue() == null ? "" : e.getValue();
                        if (!label.startsWith("[友] ")) continue;
                        String real = com.cathelperawa.owo.app.api.IdentityApi.buddyNameByUid(uid);
                        if (real == null || real.trim().isEmpty()) continue;
                        if (!("[友] " + real).equals(label)) {
                            MsgStore.notePeer(uid, real, false);
                            changed = true;
                        }
                    }

                    Map<String, String> troopNames =
                            com.cathelperawa.owo.app.bridge.TroopApi.allTroopNames();
                    if (!troopNames.isEmpty()) {
                        for (Map.Entry<String, String> e : peers.entrySet()) {
                            final String uid = e.getKey();
                            String label = e.getValue() == null ? "" : e.getValue();
                            if (!label.startsWith("[群] ")) continue;
                            String cur = label.substring(4);
                            if (!cur.matches("\\d{5,}")) continue;
                            String real = troopNames.get(uid);
                            if (real == null || real.trim().isEmpty()) continue;
                            MsgStore.notePeer(uid, real, true);
                            changed = true;
                        }
                    }
                } catch (Throwable ignore) {
                }
                if (!changed) return;
                new android.os.Handler(android.os.Looper.getMainLooper()).post(new Runnable() {
                    @Override public void run() {
                        try { host.setPageView(build()); } catch (Throwable ignore) {}
                    }
                });
            }
        }, "cat-heal-peers").start();
    }

    private static String query = "";

    private static boolean groupOpen = true;
    private static boolean friendOpen = true;

    private static boolean warmed = false;

    private void warmLists() {
        if (warmed) return;
        warmed = true;
        new Thread(new Runnable() {
            @Override public void run() {
                try {
                    com.cathelperawa.owo.app.bridge.TroopApi.allTroopNames();
                    com.cathelperawa.owo.app.bridge.ContactApi.allBuddies();
                } catch (Throwable ignore) {
                }
                new android.os.Handler(android.os.Looper.getMainLooper()).post(new Runnable() {
                    @Override public void run() {
                        try { host.setPageView(build()); } catch (Throwable ignore) {}
                    }
                });
            }
        }, "cat-warm-lists").start();
    }

    @Override
    public View build() {
        healFriendNames();
        warmLists();
        View[] out = new View[2];
        LinearLayout root = pageRoot("作用范围", "勾选哪些会话猫娘化", out);

        root.addView(buildAllCard(), matchWrap(dp(11)));

        Map<String, String> peers = MsgStore.peers();
        final Map<String, String[]> groupMap = new LinkedHashMap<String, String[]>();
        final Map<String, String[]> friendMap = new LinkedHashMap<String, String[]>();

        for (Map.Entry<String, String> e : peers.entrySet()) {
            String label = e.getValue() == null ? "" : e.getValue();
            String uid = e.getKey();
            String nm = label.startsWith("[群] ") ? label.substring(4)
                    : label.startsWith("[友] ") ? label.substring(4) : label;
            boolean isGroup = label.startsWith("[群] ")
                    || (!label.startsWith("[友] ") && !uid.startsWith("u_"));
            if (isGroup) groupMap.put(uid, new String[]{uid, nm});
            else friendMap.put(uid, new String[]{uid, nm});
        }

        try {
            Map<String, String> allTroops =
                    com.cathelperawa.owo.app.bridge.TroopApi.allTroopNames();
            for (Map.Entry<String, String> t : allTroops.entrySet()) {

                groupMap.put(t.getKey(), new String[]{t.getKey(), t.getValue()});
            }
        } catch (Throwable ignore) {
        }

        try {
            Map<String, String> buddies =
                    com.cathelperawa.owo.app.bridge.ContactApi.allBuddies();
            for (Map.Entry<String, String> b : buddies.entrySet()) {

                String uin = b.getKey();
                String uid = uin;
                try {
                    String u = com.cathelperawa.owo.app.bridge.QQUtils.uidFromUin(uin);
                    if (u != null && u.length() > 0) uid = u;
                } catch (Throwable ignore) {
                }
                friendMap.put(uid, new String[]{uid, b.getValue()});
            }
        } catch (Throwable ignore) {
        }

        final List<String[]> groups = new ArrayList<String[]>(groupMap.values());
        final List<String[]> friends = new ArrayList<String[]>(friendMap.values());
        Comparator<String[]> byName = new Comparator<String[]>() {
            @Override public int compare(String[] a, String[] b) {
                return a[1].compareToIgnoreCase(b[1]);
            }
        };
        Collections.sort(groups, byName);
        Collections.sort(friends, byName);

        if (peers.isEmpty()) {
            LinearLayout empty = whiteCard(dp(18), 0x8CFFFFFF);
            empty.setPadding(dp(15), dp(18), dp(15), dp(18));
            empty.addView(text("还没有会话记录", 14, 0xFF43587A, true, dp(3)));
            empty.addView(text("在 TIM 里收发几条消息，群和好友会自动出现在这里", 11, 0xFF8496AE, false, 0));
            root.addView(empty);
            fadeIn(root);
            return out[0];
        }

        root.addView(buildSearchBox(root, groups, friends), matchWrap(dp(11)));

        String q = query.trim().toLowerCase();
        View gGroup = buildGroup("群聊", groups, q, true);
        gGroup.setTag("cat_list_area");
        root.addView(gGroup, matchWrap(dp(11)));
        root.addView(buildGroup("好友", friends, q, false), matchWrap(dp(11)));

        fadeIn(root);
        return out[0];
    }

    private View buildAllCard() {
        FrameLayout allCard = whiteFrame(dp(18), 0x8CFFFFFF);
        allCard.setClipToOutline(true);
        LinearLayout ar = new LinearLayout(ctx);
        ar.setOrientation(LinearLayout.HORIZONTAL);
        ar.setGravity(Gravity.CENTER_VERTICAL);
        ar.setPadding(dp(14), dp(13), dp(14), dp(13));
        LinearLayout at = new LinearLayout(ctx);
        at.setOrientation(LinearLayout.VERTICAL);
        at.addView(text("全部会话", 15, 0xFF43587A, true, dp(2)));
        at.addView(text("开启后所有群/好友都生效，也可单独关掉某几个", 11, 0xFF8496AE, false, 0));
        ar.addView(at, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        final SwitchBox allBox = new SwitchBox(ctx, MsgStore.isScopeAll());
        ar.addView(allBox, new LinearLayout.LayoutParams(dp(52), dp(30)));
        allCard.addView(ar);
        allCard.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                boolean now = !MsgStore.isScopeAll();
                MsgStore.setScopeAll(now);
                allBox.setChecked(now);
                press(v);
                host.setPageView(build());
            }
        });
        return allCard;
    }

    private View buildSearchBox(final LinearLayout root,
                                final List<String[]> groups, final List<String[]> friends) {
        FrameLayout wrap = whiteFrame(dp(16), 0x8CFFFFFF);
        wrap.setClipToOutline(true);
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(14), dp(10), dp(14), dp(10));

        final EditText et = new EditText(ctx);
        et.setHint("搜索群名 / 好友名 / 号码");
        et.setText(query);
        et.setTextSize(13);
        et.setTextColor(0xFF43587A);
        et.setHintTextColor(0xFFB3C0D2);
        et.setSingleLine(true);
        et.setBackgroundDrawable(null);
        et.setPadding(dp(8), 0, 0, 0);
        row.addView(et, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        final TextView clear = text("✕", 14, 0xFFB0BDD1, false, 0);
        clear.setPadding(dp(8), 0, dp(2), 0);
        clear.setVisibility(query.length() > 0 ? View.VISIBLE : View.GONE);
        clear.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                et.setText("");
            }
        });
        row.addView(clear);

        wrap.addView(row);

        et.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {
                String now = s == null ? "" : s.toString();
                if (now.equals(query)) return;
                query = now;
                clear.setVisibility(now.length() > 0 ? View.VISIBLE : View.GONE);

                rebuildList(root, groups, friends);
            }
        });
        return wrap;
    }

    private void rebuildList(LinearLayout root, List<String[]> groups, List<String[]> friends) {

        int from = -1;
        for (int i = 0; i < root.getChildCount(); i++) {
            if ("cat_list_area".equals(root.getChildAt(i).getTag())) { from = i; break; }
        }
        if (from < 0) return;
        root.removeViews(from, root.getChildCount() - from);

        String q = query.trim().toLowerCase();
        View g = buildGroup("群聊", groups, q, true);
        g.setTag("cat_list_area");
        root.addView(g, matchWrap(dp(11)));
        root.addView(buildGroup("好友", friends, q, false), matchWrap(dp(11)));
    }

    private View buildGroup(String title, List<String[]> items, final String q,
                            final boolean isGroup) {

        final List<String[]> shown = new ArrayList<String[]>();
        for (String[] it : items) {
            if (q.length() == 0 || it[1].toLowerCase().contains(q)
                    || it[0].toLowerCase().contains(q)) {
                shown.add(it);
            }
        }

        final LinearLayout col = new LinearLayout(ctx);
        col.setOrientation(LinearLayout.VERTICAL);

        final boolean open = q.length() > 0 || (isGroup ? groupOpen : friendOpen);

        FrameLayout head = whiteFrame(dp(18), 0x8CFFFFFF);
        head.setClipToOutline(true);
        LinearLayout hr = new LinearLayout(ctx);
        hr.setOrientation(LinearLayout.HORIZONTAL);
        hr.setGravity(Gravity.CENTER_VERTICAL);
        hr.setPadding(dp(14), dp(13), dp(14), dp(13));

        TextView arrow = text(open ? "▾" : "▸", 13, 0xFF7FA5E6, true, 0);
        hr.addView(arrow);

        LinearLayout ht = new LinearLayout(ctx);
        ht.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams htp = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        htp.leftMargin = dp(9);
        TextView t1 = text(title + "（" + shown.size() + "）", 15, 0xFF43587A, true, dp(2));
        ht.addView(t1);

        int selCount = 0;
        for (String[] it : shown) if (MsgStore.allows(it[0])) selCount++;
        String subTxt = q.length() > 0
                ? "搜索命中 " + shown.size() + " 项"
                : (MsgStore.isScopeAll()
                        ? "全部生效 · 已关 " + (shown.size() - selCount) + " 项"
                        : "已选 " + selCount + " / " + shown.size());
        ht.addView(text(subTxt, 11, 0xFF8496AE, false, 0));
        hr.addView(ht, htp);

        head.addView(hr);
        col.addView(head, matchWrap(open ? dp(8) : 0));

        head.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (q.length() > 0) return;
                if (isGroup) groupOpen = !groupOpen; else friendOpen = !friendOpen;
                press(v);
                host.setPageView(build());
            }
        });

        if (open && !shown.isEmpty()) {
            for (final String[] it : shown) {
                col.addView(buildPeerCard(it[0], it[1], isGroup), matchWrap(dp(8)));
            }
        } else if (open && shown.isEmpty()) {
            LinearLayout empty = whiteCard(dp(16), 0x8CFFFFFF);
            empty.setPadding(dp(15), dp(16), dp(15), dp(16));
            empty.addView(text(q.length() > 0 ? "没有匹配的" + title : "还没有" + title + "记录",
                    12, 0xFF8496AE, false, 0));
            col.addView(empty);
        }
        return col;
    }

    private View buildPeerCard(final String uid, String name, boolean isGroup) {
        FrameLayout card = whiteFrame(dp(16), 0x8CFFFFFF);
        card.setClipToOutline(true);
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(14), dp(12), dp(14), dp(12));

        final boolean checked = MsgStore.allows(uid);
        final CheckDot dot = new CheckDot(ctx, checked);
        row.addView(dot, new LinearLayout.LayoutParams(dp(24), dp(24)));

        TextView tv = text(friendlyName(uid, name), 13, 0xFF43587A, false, 0);
        tv.setPadding(dp(10), 0, 0, 0);
        tv.setSingleLine(true);
        tv.setEllipsize(android.text.TextUtils.TruncateAt.END);
        row.addView(tv, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView uidTxt = text(brief(uid), 10, 0xFFB0BDD1, false, 0);
        row.addView(uidTxt);
        card.addView(row);

        card.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {

                if (MsgStore.isScopeAll()) {
                    MsgStore.toggleUnderAll(uid);
                } else {
                    MsgStore.toggleSelected(uid);
                }
                dot.setChecked(MsgStore.allows(uid));
                press(v);

                host.setPageView(build());
            }
        });
        return card;
    }

    private static String brief(String s) {
        if (s == null) return "";
        return s.length() > 10 ? s.substring(0, 10) + "…" : s;
    }

    private static String friendlyName(String uid, String name) {

        if (name != null && name.matches("\\d{5,}")) return name;
        if (name != null && name.length() > 0 && !name.equals(uid)) {
            if (!(name.startsWith("u_") && name.length() > 16)) return name;
        }
        if (uid != null && uid.startsWith("u_")) return "陌生人 · " + brief(uid.substring(2));
        return brief(uid);
    }
}
