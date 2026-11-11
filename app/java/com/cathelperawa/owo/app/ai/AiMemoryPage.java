package com.cathelperawa.owo.app.ai;

import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.cathelperawa.owo.app.api.IdentityApi;
import com.cathelperawa.owo.app.bridge.ContactApi;
import com.cathelperawa.owo.app.ui.CatSheet;
import com.cathelperawa.owo.app.ui.Page;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class AiMemoryPage extends Page {

    private static final int C_MAIN = 0xFF3C4B60;
    private static final int C_SUB = 0xFF8A9BB0;
    private static final int C_PINK = 0xFFE85D9C;

    private LinearLayout body;
    private LinearLayout content;
    
    private String currentPeer = null;

    private static final SimpleDateFormat FMT =
            new SimpleDateFormat("MM-dd HH:mm", Locale.CHINA);

    public AiMemoryPage(Host host) {
        super(host);
    }

    @Override
    public View build() {
        View[] out = new View[2];
        body = pageRoot("对话记忆", "每个群 / 每个好友各记一份", out, true, 13, "bg_ai", 0x66);
        content = new LinearLayout(ctx);
        content.setOrientation(LinearLayout.VERTICAL);
        body.addView(content, matchWrap(0));
        render();
        return out[0];
    }

    private void render() {
        content.removeAllViews();
        if (currentPeer == null) renderList();
        else renderDetail();
    }

    

    private void renderList() {
        List<String> peers = AiStore.peers();
        int total = AiStore.totalCount();

        LinearLayout head = whiteCard(dp(22), 0xF2FFFFFF);
        head.setOrientation(LinearLayout.VERTICAL);
        head.setPadding(dp(18), dp(16), dp(18), dp(16));

        LinearLayout top = new LinearLayout(ctx);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout txt = new LinearLayout(ctx);
        txt.setOrientation(LinearLayout.VERTICAL);
        txt.addView(text("对话记忆", 18, C_MAIN, true, dp(4)), matchWrap(0));
        txt.addView(text(peers.size() + " 个会话 · " + total + " 条记录"
                        + (AiStore.updatedAt() > 0
                        ? " · 最近 " + FMT.format(new Date(AiStore.updatedAt())) : ""),
                11, C_SUB, false, 0), matchWrap(0));
        top.addView(txt, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        if (total > 0) {
            TextView clear = pill("清空全部", 0xFFFFD9E6);
            clear.setTextColor(C_PINK);
            press(clear);
            clear.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { askClearAll(); }
            });
            top.addView(clear, wrapWrap());
        }
        head.addView(top, matchWrap(0));
        head.addView(limitRow(), matchWrap(dp(8)));
        content.addView(head, matchWrap(dp(14)));

        if (peers.isEmpty()) {
            LinearLayout empty = whiteCard(dp(20), 0xF2FFFFFF);
            empty.setOrientation(LinearLayout.VERTICAL);
            empty.setPadding(dp(18), dp(22), dp(18), dp(22));
            empty.addView(text("还没有任何记忆", 14, C_MAIN, true, dp(6)), matchWrap(0));
            empty.addView(text("等它跟人聊过几句，这里就会自动出现",
                    11, C_SUB, false, 0), matchWrap(0));
            content.addView(empty, matchWrap(dp(14)));
            return;
        }

        for (int i = 0; i < peers.size(); i++) {
            content.addView(peerCard(peers.get(i)), matchWrap(dp(10)));
        }

        TextView note = text("点进会话逐条看；点一条改，长按可以删",
                10.5f, C_SUB, false, 0);
        note.setPadding(dp(6), dp(6), dp(6), dp(10));
        content.addView(note, matchWrap(0));
    }

    private View peerCard(final String peer) {
        LinearLayout card = whiteCard(dp(18), 0xEEFFFFFF);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(16), dp(14), dp(14), dp(14));

        boolean group = !peer.startsWith("u_");
        TextView avatar = new TextView(ctx);
        avatar.setText(group ? "群" : "私");
        avatar.setTextSize(11);
        avatar.setTextColor(0xFF5A6B80);
        avatar.setGravity(Gravity.CENTER);
        GradientDrawable ag = new GradientDrawable();
        ag.setShape(GradientDrawable.OVAL);
        ag.setColor(group ? 0xFFA9D9C3 : 0xFFF5B7CE);
        avatar.setBackgroundDrawable(ag);
        card.addView(avatar, new LinearLayout.LayoutParams(dp(34), dp(34)));

        LinearLayout txt = new LinearLayout(ctx);
        txt.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        tp.leftMargin = dp(11);
        txt.addView(text(titleOf(peer), 14.5f, C_MAIN, true, dp(3)), matchWrap(0));

        long last = AiStore.lastAt(peer);
        txt.addView(text(peer + " · " + AiStore.size(peer) + " 条"
                        + (last > 0 ? " · " + FMT.format(new Date(last)) : ""),
                10.5f, C_SUB, false, 0), matchWrap(0));
        card.addView(txt, tp);

        card.addView(text("›", 20, 0xFFB6C1D0, false, 0));

        press(card);
        card.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                currentPeer = peer;
                render();
            }
        });
        return card;
    }

    

    private void renderDetail() {
        final String peer = currentPeer;
        List<AiStore.Msg> msgs = AiStore.messages(peer);

        LinearLayout head = whiteCard(dp(22), 0xF2FFFFFF);
        head.setOrientation(LinearLayout.VERTICAL);
        head.setPadding(dp(18), dp(14), dp(18), dp(16));

        TextView back = pill("‹ 返回", 0xFFE4ECF7);
        back.setTextColor(0xFF5A6B80);
        press(back);
        back.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                currentPeer = null;
                render();
            }
        });
        head.addView(back, wrapWrap());

        TextView t = text(titleOf(peer), 20, C_MAIN, true, dp(4));
        t.setPadding(0, dp(12), 0, 0);
        head.addView(t, matchWrap(0));

        LinearLayout sub = new LinearLayout(ctx);
        sub.setOrientation(LinearLayout.HORIZONTAL);
        sub.setGravity(Gravity.CENTER_VERTICAL);
        TextView info = text((peer.startsWith("u_") ? "私聊 · " : "群聊 · ")
                + peer + " · " + msgs.size() + " 条", 10.5f, C_SUB, false, 0);
        sub.addView(info, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        if (!msgs.isEmpty()) {
            TextView clear = pill("清空", 0xFFFFD9E6);
            clear.setTextColor(C_PINK);
            press(clear);
            clear.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { askClearPeer(peer); }
            });
            sub.addView(clear, wrapWrap());
        }
        head.addView(sub, matchWrap(0));
        content.addView(head, matchWrap(dp(14)));
        
        content.addView(searchCard(peer), matchWrap(dp(12)));

        if (msgs.isEmpty()) {
            LinearLayout empty = whiteCard(dp(20), 0xF2FFFFFF);
            empty.setPadding(dp(18), dp(20), dp(18), dp(20));
            empty.addView(text("这个会话已经没有记录了", 13, C_MAIN, false, 0), matchWrap(0));
            content.addView(empty, matchWrap(dp(14)));
            return;
        }

        for (int i = 0; i < msgs.size(); i++) {
            content.addView(bubbleRow(peer, i, msgs.get(i)), matchWrap(dp(8)));
        }

        TextView note = text("点气泡改内容，长按可以删；改完自动存盘，下次回话就用新的",
                10.5f, C_SUB, false, 0);
        note.setPadding(dp(6), dp(8), dp(6), dp(10));
        content.addView(note, matchWrap(0));
    }

    private View bubbleRow(final String peer, final int index, AiStore.Msg m) {
        final boolean user = "user".equals(m.role);

        LinearLayout line = new LinearLayout(ctx);
        line.setOrientation(LinearLayout.HORIZONTAL);
        line.setGravity(user ? Gravity.START : Gravity.END);

        LinearLayout card = whiteCard(dp(16), user ? 0xF2F4FAFF : 0xF2FFF2F7);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(13), dp(10), dp(13), dp(10));

        LinearLayout top = new LinearLayout(ctx);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);

        String tagText = user ? "对方" : "我";
        if (user && peer != null && !peer.startsWith("u_")
                && m.uid != null && m.uid.length() > 0) {
            tagText = nameOfUid(peer, m.uid);
        }
        TextView tag = pill(tagText, user ? 0xFFD8E6FF : 0xFFFFD9E6);
        tag.setTextColor(user ? 0xFF5A6B80 : C_PINK);
        top.addView(tag, wrapWrap());

        TextView tm = text(FMT.format(new Date(m.at)), 10, C_SUB, false, 0);
        LinearLayout.LayoutParams tlp = wrapWrap();
        tlp.leftMargin = dp(8);
        top.addView(tm, tlp);
        card.addView(top, matchWrap(dp(6)));

        String body = m.content == null ? "" : m.content;
        
        if (user && body.startsWith("「")) {
            int k = body.indexOf("」");
            if (k > 0 && k < 30) {
                body = body.substring(k + 1).trim();
                if (body.startsWith("：") || body.startsWith(":")) {
                    body = body.substring(1).trim();
                }
            }
        }
        if (body.length() > 300) body = body.substring(0, 300) + "…";
        TextView tv = text(body, 12.5f, 0xFF44546A, false, 0);
        card.addView(tv, matchWrap(0));

        press(card);
        card.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { editOne(peer, index); }
        });
        card.setOnLongClickListener(new View.OnLongClickListener() {
            @Override public boolean onLongClick(View v) {
                CatSheet.pick(ctx, "这条怎么处理", new String[]{"改内容", "删掉"},
                        new CatSheet.OnPick() {
                            @Override public void run(int i, String s) {
                                if (i == 0) editOne(peer, index);
                                else askDeleteOne(peer, index);
                            }
                        });
                return true;
            }
        });

        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 0.85f);
        if (user) {
            line.addView(card, clp);
            line.addView(new View(ctx), new LinearLayout.LayoutParams(0, 1, 0.15f));
        } else {
            line.addView(new View(ctx), new LinearLayout.LayoutParams(0, 1, 0.15f));
            line.addView(card, clp);
        }
        return line;
    }

    

    private void editOne(final String peer, final int index) {
        List<AiStore.Msg> msgs = AiStore.messages(peer);
        if (index < 0 || index >= msgs.size()) return;
        CatSheet.inputMulti(ctx, "改这条内容", "留空则不保存", msgs.get(index).content,
                new CatSheet.OnText() {
                    @Override public void run(String s) {
                        if (s == null || s.trim().length() == 0) {
                            toast("没改");
                            return;
                        }
                        AiStore.setContent(peer, index, s);
                        toast("已保存");
                        render();
                    }
                });
    }

    private void askDeleteOne(final String peer, final int index) {
        CatSheet.pick(ctx, "确定删掉这条？", new String[]{"删掉", "算了"},
                new CatSheet.OnPick() {
                    @Override public void run(int i, String s) {
                        if (i != 0) return;
                        AiStore.removeAt(peer, index);
                        toast("已删除");
                        render();
                    }
                });
    }

    private void askClearPeer(final String peer) {
        CatSheet.pick(ctx, "清空「" + titleOf(peer) + "」的全部记忆？",
                new String[]{"清空", "算了"}, new CatSheet.OnPick() {
                    @Override public void run(int i, String s) {
                        if (i != 0) return;
                        AiStore.clear(peer);
                        toast("已清空");
                        currentPeer = null;
                        render();
                    }
                });
    }

    private void askClearAll() {
        CatSheet.pick(ctx, "清空所有会话的记忆？", new String[]{"全部清空", "算了"},
                new CatSheet.OnPick() {
                    @Override public void run(int i, String s) {
                        if (i != 0) return;
                        AiStore.clearAll();
                        toast("已全部清空");
                        currentPeer = null;
                        render();
                    }
                });
    }

    

    
    private static String nameOfUid(String peer, String uid) {
        try {
            String uin = IdentityApi.uinFromUid(uid);
            if (uin != null && uin.length() > 0) {
                String n = IdentityApi.troopMemberName(peer, uin);
                if (n != null && n.trim().length() > 0) return n.trim();
            }
            String bn = ContactApi.getBuddyNameByUid(uid);
            if (bn != null && bn.trim().length() > 0) return bn.trim();
            if (uin != null && uin.length() > 0) return uin;
        } catch (Throwable ignore) {
        }
        return "群友";
    }

    private static String titleOf(String peer) {
        if (peer == null || peer.length() == 0) return "未知会话";
        try {
            if (peer.startsWith("u_")) {
                String n = ContactApi.getBuddyNameByUid(peer);
                if (n != null && n.trim().length() > 0) return n.trim();
                String uin = IdentityApi.uinFromUid(peer);
                if (uin != null && uin.length() > 0) return uin;
            } else {
                String n = IdentityApi.troopName(peer);
                if (n != null && n.trim().length() > 0) return n.trim();
                return "群 " + peer;
            }
        } catch (Throwable ignore) {
        }
        return peer;
    }

    

    
    private View searchCard(final String peer) {
        LinearLayout card = whiteCard(dp(20), 0xF2FFFFFF);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(12), dp(16), dp(14));
        card.addView(text("检索这个会话", 13.5f, C_MAIN, true, dp(2)), matchWrap(0));
        card.addView(text("跟 AI 回复时用的是同一套匹配（中文 2 字滑窗 + 新鲜度）",
                10.5f, C_SUB, false, 0), matchWrap(dp(6)));

        EditText et = new EditText(ctx);
        et.setHint("输入一句话，比如「火锅」");
        et.setTextSize(13.5f);
        et.setTextColor(C_MAIN);
        et.setSingleLine(true);
        et.setHintTextColor(0xFFB6C1D0);
        et.setPadding(dp(12), dp(9), dp(12), dp(9));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0x66F0F4F8);
        bg.setCornerRadius(dp(10));
        et.setBackground(bg);
        card.addView(et, matchWrap(0));

        final TextView status = text("", 10.5f, C_SUB, false, 0);
        status.setPadding(0, dp(6), 0, 0);
        card.addView(status, matchWrap(0));

        final LinearLayout result = new LinearLayout(ctx);
        result.setOrientation(LinearLayout.VERTICAL);
        result.setPadding(0, dp(4), 0, 0);
        card.addView(result, matchWrap(0));

        et.addTextChangedListener(new TextWatcher() {
            private Runnable pending;
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable e) {
                final String q = e.toString().trim();
                final android.os.Handler h =
                        new android.os.Handler(android.os.Looper.getMainLooper());
                if (pending != null) h.removeCallbacks(pending);
                pending = new Runnable() {
                    @Override public void run() {
                        result.removeAllViews();
                        if (q.length() == 0) { status.setText(""); return; }
                        java.util.List<AiStore.Msg> found = AiStore.search(peer, q, 6, null);
                        if (found.isEmpty()) {
                            status.setText("没找到相关的（这个会话里没有，或者太短没信息量）");
                            return;
                        }
                        status.setText("命中 " + found.size() + " 条");
                        for (int i = 0; i < found.size(); i++) {
                            AiStore.Msg m = found.get(i);
                            LinearLayout box = new LinearLayout(ctx);
                            box.setOrientation(LinearLayout.VERTICAL);
                            box.setPadding(dp(10), dp(8), dp(10), dp(8));
                            GradientDrawable g = new GradientDrawable();
                            g.setColor(0x44EAF1FA);
                            g.setCornerRadius(dp(8));
                            box.setBackground(g);
                            String who = "assistant".equals(m.role) ? "AI" : "对方";
                            box.addView(text(who + " · " + fmtTime(m.at), 10, C_SUB, false, dp(2)),
                                    matchWrap(0));
                            String c = m.content.length() > 80
                                    ? m.content.substring(0, 80) + "…" : m.content;
                            box.addView(text(c, 12.5f, C_MAIN, false, 0), matchWrap(0));
                            result.addView(box, matchWrap(dp(6)));
                        }
                    }
                };
                h.postDelayed(pending, 350);
            }
        });
        return card;
    }

    private static String fmtTime(long sec) {
        if (sec <= 0) return "";
        return new java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.CHINA)
                .format(new java.util.Date(sec * 1000L));
    }

    

    
    private View limitRow() {
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(10), 0, dp(2));
        LinearLayout txt = new LinearLayout(ctx);
        txt.setOrientation(LinearLayout.VERTICAL);
        txt.addView(text("每个会话记多少条", 13, C_MAIN, true, dp(2)), matchWrap(0));
        txt.addView(text("当前 " + AiConfig.memLimit() + " 条 · 超出自动删最老的",
                10.5f, C_SUB, false, 0), matchWrap(0));
        row.addView(txt, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        TextView btn = pill("修改", 0xFFE4ECF7);
        btn.setTextColor(0xFF5A6B80);
        press(btn);
        row.addView(btn, wrapWrap());
        row.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                com.cathelperawa.owo.app.ui.CatSheet.input(ctx,
                        "每个会话最多记多少条",
                        "20 ~ 500。改小会立刻删掉超出部分（删最老的）",
                        String.valueOf(AiConfig.memLimit()),
                        new com.cathelperawa.owo.app.ui.CatSheet.OnText() {
                            @Override public void run(String s2) {
                                int v;
                                try {
                                    v = Integer.parseInt(s2.trim());
                                } catch (Throwable t) {
                                    toast("要填数字");
                                    return;
                                }
                                if (v < 20) v = 20;
                                if (v > 500) v = 500;
                                AiConfig.setMemLimit(v);
                                int removed = AiStore.trimAll(v);
                                toast("已设为 " + v + " 条" + (removed > 0
                                        ? "，删掉 " + removed + " 条旧记录" : ""));
                                render();
                            }
                        });
            }
        });
        return row;
    }
}
