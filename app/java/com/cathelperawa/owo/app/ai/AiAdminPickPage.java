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
import com.cathelperawa.owo.app.ui.Page;
import com.cathelperawa.owo.app.ui.UiKit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class AiAdminPickPage extends Page {

    private static final int C_MAIN = 0xFF3C4B60;
    private static final int C_SUB = 0xFF8A9BB0;
    private static final int C_PINK = 0xFFE85D9C;

    private static final int MAX_ROWS = 120;

    private LinearLayout body;
    private LinearLayout listBox;
    private TextView countLabel;
    private String query = "";

    private final List<String[]> friends = new ArrayList<String[]>();

    public AiAdminPickPage(Host host) {
        super(host);
    }

    @Override
    public View build() {
        View[] out = new View[2];
        body = pageRoot("选管理员", "点一下加进名单", out, true, 13, "bg_ai", 0x66);

        loadFriends();

        LinearLayout head = whiteCard(dp(20), 0xF2FFFFFF);
        head.setOrientation(LinearLayout.VERTICAL);
        head.setPadding(dp(16), dp(14), dp(16), dp(14));
        countLabel = text("管理员 " + AiConfig.adminCount() + " 位",
                14, C_MAIN, true, dp(4));
        head.addView(countLabel, matchWrap(0));
        head.addView(text("点一行就加；已经在名单里的会标「已添加」\n"
                        + "记不住备注也没事 —— 输 QQ 号也能找到（号码不会显示在列表里）",
                10.5f, C_SUB, false, 0), matchWrap(0));
        body.addView(head, matchWrap(dp(12)));

        
        final EditText search = UiKit.pastelEdit(ctx, "", "搜名字 / QQ 号");
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
        body.addView(listBox, matchWrap(dp(8)));

        refill();
        return out[0];
    }

    private void loadFriends() {
        friends.clear();
        Map<String, String> raw;
        try {
            raw = IdentityApi.allBuddies();
        } catch (Throwable t) {
            raw = new HashMap<String, String>();
        }
        if (raw == null) return;
        Map<String, Integer> dup = new HashMap<String, Integer>();
        for (Map.Entry<String, String> e : raw.entrySet()) {

            
            String key = e.getKey();
            if (key == null || key.length() == 0) continue;
            String uin = null;
            try {
                uin = IdentityApi.uinFromUid(key);
            } catch (Throwable ignore) {
            }
            if (uin == null || uin.trim().length() == 0) uin = key;
            uin = uin.trim();
            if (uin.length() == 0) continue;
            String name = e.getValue();
            if (name == null || name.trim().length() == 0) name = "未备注";
            name = name.trim();
            
            Integer n = dup.get(name);
            if (n == null) {
                dup.put(name, Integer.valueOf(1));
            } else {
                dup.put(name, Integer.valueOf(n.intValue() + 1));
                name = name + " (" + (n.intValue() + 1) + ")";
            }
            friends.add(new String[]{uin, name});
        }
    }

    private void refill() {
        listBox.removeAllViews();
        String q = query == null ? "" : query.trim().toLowerCase();
        int shown = 0;
        for (int i = 0; i < friends.size(); i++) {
            final String uin = friends.get(i)[0];
            final String name = friends.get(i)[1];
            if (q.length() > 0
                    && name.toLowerCase().indexOf(q) < 0
                    && uin.indexOf(q) < 0) {
                continue;
            }
            if (shown >= MAX_ROWS) {
                TextView more = text("只显示前 " + MAX_ROWS + " 个，用上面的搜索框缩小范围",
                        10.5f, C_SUB, false, 0);
                more.setPadding(dp(10), dp(8), dp(10), dp(8));
                listBox.addView(more, matchWrap(0));
                break;
            }
            listBox.addView(friendRow(uin, name), matchWrap(dp(8)));
            shown++;
        }
        if (shown == 0) {
            TextView empty = text(friends.isEmpty()
                            ? "没有读到好友列表 · 回去用「手动填 QQ」加人吧"
                            : "没有名字或 QQ 号匹配「" + query + "」的人",
                    12, C_SUB, false, 0);
            empty.setPadding(dp(12), dp(16), dp(12), dp(16));
            listBox.addView(empty, matchWrap(0));
        }
    }

    private View friendRow(final String uin, final String name) {
        final boolean already = AiConfig.isAdminUin(uin);

        LinearLayout card = whiteCard(dp(16), 0xEEFFFFFF);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(14), dp(12), dp(14), dp(12));

        TextView initial = text(firstChar(name), 13, 0xFFFFFFFF, true, 0);
        initial.setGravity(Gravity.CENTER);
        GradientDrawable round = new GradientDrawable();
        round.setShape(GradientDrawable.OVAL);
        round.setColor(already ? 0xFFB9C4D2 : C_PINK);
        initial.setBackgroundDrawable(round);
        card.addView(initial, new LinearLayout.LayoutParams(dp(34), dp(34)));

        TextView nm = text(name, 14, C_MAIN, true, 0);
        nm.setSingleLine(true);
        nm.setEllipsize(android.text.TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams nlp = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        nlp.leftMargin = dp(12);
        card.addView(nm, nlp);

        TextView tag = text(already ? "已添加" : "添加", 11.5f,
                already ? C_SUB : C_PINK, true, 0);
        card.addView(tag, wrapWrap());

        if (!already) {
            press(card);
            card.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    AiConfig.addAdmin(uin);
                    toast("已加「" + name + "」");
                    countLabel.setText("管理员 " + AiConfig.adminCount() + " 位");
                    refill();
                }
            });
        }
        return card;
    }

    private String firstChar(String s) {
        if (s == null || s.length() == 0) return "?";
        return s.substring(0, 1);
    }
}
