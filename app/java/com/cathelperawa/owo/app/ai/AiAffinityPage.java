package com.cathelperawa.owo.app.ai;

import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.cathelperawa.owo.app.ui.CatDialog;
import com.cathelperawa.owo.app.ui.CatSheet;
import com.cathelperawa.owo.app.ui.Page;
import com.cathelperawa.owo.app.ui.SwitchBox;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class AiAffinityPage extends Page {

    private static final int C_MAIN = 0xFF3C4B60;
    private static final int C_SUB = 0xFF8A9BB0;
    private static final int C_PINK = 0xFFE85D9C;

    private static final SimpleDateFormat FMT =
            new SimpleDateFormat("MM-dd HH:mm", Locale.CHINA);

    private LinearLayout body;
    private LinearLayout content;
    
    private String currentUid = null;

    public AiAffinityPage(Host host) {
        super(host);
    }

    @Override
    public View build() {
        View[] out = new View[2];
        body = pageRoot("好感度", "按人记 · AI 自动归纳 + 手动调", out, true, 13, "bg_ai", 0x66);
        content = new LinearLayout(ctx);
        content.setOrientation(LinearLayout.VERTICAL);
        body.addView(content, matchWrap(0));
        render();
        return out[0];
    }

    private void render() {
        content.removeAllViews();
        if (currentUid == null) renderList();
        else renderDetail();
    }

    @Override
    public boolean onBack() {
        if (currentUid != null) {
            currentUid = null;
            render();
            return true;
        }
        return false;
    }

    

    private void renderList() {
        List<AiAffinity.Profile> all = AiAffinity.list();

        LinearLayout head = whiteCard(dp(22), 0xF2FFFFFF);
        head.setOrientation(LinearLayout.VERTICAL);
        head.setPadding(dp(18), dp(16), dp(18), dp(16));

        LinearLayout top = new LinearLayout(ctx);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout txt = new LinearLayout(ctx);
        txt.setOrientation(LinearLayout.VERTICAL);
        txt.addView(text("好感度档案", 18, C_MAIN, true, dp(4)), matchWrap(0));
        String sub;
        if (all.isEmpty()) {
            sub = "还没有记录 · 聊过就会自动生成";
        } else {
            long last = all.get(0).lastAt;
            sub = all.size() + " 人 · 最近 " + (last > 0 ? FMT.format(new Date(last)) : "—");
        }
        txt.addView(text(sub, 11, C_SUB, false, 0), matchWrap(0));
        top.addView(txt, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        final boolean on = AiConfig.affinityOn();
        SwitchBox sw = new SwitchBox(ctx, on);
        LinearLayout.LayoutParams swlp = new LinearLayout.LayoutParams(dp(50), dp(28));
        swlp.rightMargin = dp(10);
        sw.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                AiConfig.setBool("ai_affinity", !on);
                toast(on ? "好感度已关闭：不记录、不影响说话" : "好感度已开启");
                render();
            }
        });
        top.addView(sw, swlp);

        TextView gear = pill("⚙ 设置", 0xFFFFD9E6);
        gear.setTextColor(C_PINK);
        press(gear);
        gear.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                host.pushPage(new AiAffinitySetPage(host).build());
            }
        });
        top.addView(gear, wrapWrap());
        head.addView(top, matchWrap(0));

        content.addView(head, matchWrap(dp(14)));

        
        if (!AiConfig.affinityOn()) {
            LinearLayout offCard = whiteCard(dp(20), 0xF2FFFFFF);
            offCard.setOrientation(LinearLayout.VERTICAL);
            offCard.setPadding(dp(18), dp(20), dp(18), dp(20));
            offCard.addView(text("好感度已关闭", 16, C_MAIN, true, dp(6)), matchWrap(0));
            offCard.addView(text("• 不再记录任何人的好感度\n"
                            + "• 不再拼「关系档案」进对话（语气恢复默认）\n"
                            + "• 不再自动归纳、也不增减分数\n"
                            + "• 已有档案保留着，重新打开就继续用",
                    11.5f, C_SUB, false, 0), matchWrap(dp(12)));

            TextView reopen = text("重新开启", 14, 0xFFFFFFFF, true, 0);
            reopen.setGravity(Gravity.CENTER);
            reopen.setPadding(0, dp(11), 0, dp(11));
            GradientDrawable rg = new GradientDrawable();
            rg.setCornerRadius(dp(14));
            rg.setColor(C_PINK);
            reopen.setBackgroundDrawable(rg);
            press(reopen);
            reopen.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    AiConfig.setBool("ai_affinity", true);
                    toast("好感度已开启");
                    render();
                }
            });
            offCard.addView(reopen, matchWrap(0));
            content.addView(offCard, matchWrap(dp(14)));

            TextView offNote = text("关闭期间，别人夸你骂你都不会影响分数", 10.5f, C_SUB, false, 0);
            offNote.setPadding(dp(8), dp(2), dp(8), dp(12));
            content.addView(offNote, matchWrap(0));
            return;
        }

        if (all.isEmpty()) {
            LinearLayout empty = whiteCard(dp(20), 0xF2FFFFFF);
            empty.setOrientation(LinearLayout.VERTICAL);
            empty.setPadding(dp(18), dp(22), dp(18), dp(22));
            empty.addView(text("还没有任何人", 14, C_MAIN, true, dp(6)), matchWrap(0));
            empty.addView(text("它跟谁聊过，这里就会自动多一行；\n攒够几条消息 AI 会自己写评价、调分数",
                    11, C_SUB, false, 0), matchWrap(0));
            content.addView(empty, matchWrap(dp(14)));
            return;
        }

        for (int i = 0; i < all.size(); i++) {
            content.addView(personCard(all.get(i)), matchWrap(dp(10)));
        }

        TextView note = text("AI 自动归纳：每聊 " + AiConfig.affinityEvery()
                        + " 条更新一次，慢聊也会隔 " + AiConfig.affinityGapSec() + " 秒补上\n"
                        + "单次最多 ±" + AiConfig.affinityMaxStep()
                        + "，每天最多 ±" + AiConfig.affinityDailyCap() + "；点进去只是手动微调",
                10.5f, C_SUB, false, 0);
        note.setPadding(dp(8), dp(6), dp(8), dp(10));
        content.addView(note, matchWrap(0));
    }

    private View personCard(final AiAffinity.Profile p) {
        final int color = AiAffinity.tierColor(p.score);

        LinearLayout card = whiteCard(dp(20), 0xF2FFFFFF);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(15), dp(14), dp(15), dp(13));

        LinearLayout top = new LinearLayout(ctx);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);

        TextView initial = text(firstChar(p.displayName()), 15, 0xFFFFFFFF, true, 0);
        initial.setGravity(Gravity.CENTER);
        GradientDrawable round = new GradientDrawable();
        round.setShape(GradientDrawable.OVAL);
        round.setColor(color);
        round.setStroke(dp(2), 0x66FFFFFF);
        initial.setBackgroundDrawable(round);
        top.addView(initial, new LinearLayout.LayoutParams(dp(42), dp(42)));

        LinearLayout mid = new LinearLayout(ctx);
        mid.setOrientation(LinearLayout.VERTICAL);
        mid.setPadding(dp(12), 0, dp(8), 0);

        LinearLayout nameRow = new LinearLayout(ctx);
        nameRow.setOrientation(LinearLayout.HORIZONTAL);
        nameRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView nm = text(p.displayName(), 15.5f, C_MAIN, true, 0);
        nm.setMaxWidth(dp(150));
        nm.setSingleLine(true);
        nm.setEllipsize(android.text.TextUtils.TruncateAt.END);
        nameRow.addView(nm, wrapWrap());
        TextView tier = text(p.tier(), 9.5f, color, true, 0);
        tier.setPadding(dp(7), dp(2), dp(7), dp(3));
        GradientDrawable tg = new GradientDrawable();
        tg.setCornerRadius(dp(8));
        tg.setColor(withAlpha(color, 0x1F));
        tier.setBackgroundDrawable(tg);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        tlp.leftMargin = dp(6);
        nameRow.addView(tier, tlp);
        mid.addView(nameRow, matchWrap(dp(3)));

        String id = (p.uin != null && p.uin.length() > 0) ? "QQ " + p.uin : p.uid;
        TextView idt = text(id + " · 聊过 " + p.chats + " 条", 10, C_SUB, false, 0);
        idt.setSingleLine(true);
        idt.setEllipsize(android.text.TextUtils.TruncateAt.END);
        mid.addView(idt, matchWrap(0));
        top.addView(mid, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        LinearLayout right = new LinearLayout(ctx);
        right.setOrientation(LinearLayout.VERTICAL);
        right.setGravity(Gravity.RIGHT);
        TextView score = text(String.valueOf(p.score), 20, color, true, 0);
        score.setGravity(Gravity.RIGHT);
        right.addView(score, wrapWrap());
        TextView of = text("/100", 9.5f, C_SUB, false, 0);
        of.setGravity(Gravity.RIGHT);
        right.addView(of, wrapWrap());
        top.addView(right, wrapWrap());

        card.addView(top, matchWrap(dp(11)));
        card.addView(bar(p.score, color, withAlpha(color, 0x1A), 6), matchWrap(dp(9)));

        String sum = (p.summary == null || p.summary.trim().length() == 0)
                ? "（AI 还没写评价，多聊几句就有了）" : p.summary.trim();
        TextView st = text(sum, 11.5f, sum.startsWith("（") ? C_SUB : 0xFF55657A, false, 0);
        st.setMaxLines(2);
        st.setLineSpacing(dp(2), 1f);
        card.addView(st, matchWrap(0));

        if (p.lastDelta != 0 && p.lastReason.length() > 0) {
            int dc = p.lastDelta > 0 ? 0xFF5BAE93 : 0xFFC9655F;
            TextView d = text((p.lastDelta > 0 ? "↑ +" : "↓ ") + p.lastDelta
                    + " · " + p.lastReason, 10, dc, false, 0);
            d.setPadding(0, dp(6), 0, 0);
            card.addView(d, matchWrap(0));
        }

        press(card);
        card.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                currentUid = p.uid;
                render();
            }
        });
        return card;
    }

    

    private void renderDetail() {
        if (!AiConfig.affinityOn()) {
            currentUid = null;
            render();
            return;
        }
        final AiAffinity.Profile p = AiAffinity.get(currentUid);
        if (p == null) {
            currentUid = null;
            render();
            return;
        }
        final int color = AiAffinity.tierColor(p.score);

        TextView back = pill("‹  返回列表", 0xE6FFFFFF);
        back.setTextColor(C_PINK);
        press(back);
        back.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                currentUid = null;
                render();
            }
        });
        LinearLayout backRow = new LinearLayout(ctx);
        backRow.setOrientation(LinearLayout.HORIZONTAL);
        backRow.setPadding(dp(2), dp(2), 0, dp(6));
        backRow.addView(back, wrapWrap());
        content.addView(backRow, matchWrap(0));

        LinearLayout hero = new LinearLayout(ctx);
        hero.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable hg = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{darken(color, 0.18f), lighten(color, 0.12f)});
        hg.setCornerRadius(dp(26));
        hero.setBackgroundDrawable(hg);
        hero.setPadding(dp(20), dp(18), dp(20), dp(17));

        LinearLayout nameRow = new LinearLayout(ctx);
        nameRow.setOrientation(LinearLayout.HORIZONTAL);
        nameRow.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout nameCol = new LinearLayout(ctx);
        nameCol.setOrientation(LinearLayout.VERTICAL);
        TextView nm = text(p.displayName(), 19, 0xFFFFFFFF, true, dp(3));
        nm.setSingleLine(true);
        nm.setEllipsize(android.text.TextUtils.TruncateAt.END);
        nm.setMaxWidth(dp(180));
        nameCol.addView(nm, matchWrap(0));
        String idline = (p.uin != null && p.uin.length() > 0 ? "QQ " + p.uin + " · " : "")
                + p.uid;
        TextView idt = text(idline, 9.5f, 0xCCFFFFFF, false, 0);
        idt.setSingleLine(true);
        idt.setEllipsize(android.text.TextUtils.TruncateAt.END);
        nameCol.addView(idt, matchWrap(0));
        nameRow.addView(nameCol, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView tierPill = text(p.tier(), 11, 0xFFFFFFFF, true, 0);
        tierPill.setPadding(dp(13), dp(5), dp(13), dp(5));
        GradientDrawable tp = new GradientDrawable();
        tp.setCornerRadius(dp(20));
        tp.setColor(0x33FFFFFF);
        tp.setStroke(dp(1), 0x73FFFFFF);
        tierPill.setBackgroundDrawable(tp);
        nameRow.addView(tierPill, wrapWrap());
        hero.addView(nameRow, matchWrap(dp(14)));

        LinearLayout scoreRow = new LinearLayout(ctx);
        scoreRow.setOrientation(LinearLayout.HORIZONTAL);
        scoreRow.setGravity(Gravity.BOTTOM);

        final EditText big = new EditText(ctx);
        big.setText(String.valueOf(p.score));
        big.setTextSize(46);
        big.setTextColor(0xFFFFFFFF);
        big.setTypeface(null, android.graphics.Typeface.BOLD);
        big.setBackgroundDrawable(null);
        big.setPadding(0, 0, 0, 0);
        big.setIncludeFontPadding(false);
        big.setSingleLine(true);
        big.setGravity(Gravity.BOTTOM);
        big.setInputType(InputType.TYPE_CLASS_NUMBER);
        big.setImeOptions(EditorInfo.IME_ACTION_DONE);
        big.setSelectAllOnFocus(true);
        big.setMinWidth(dp(76));
        scoreRow.addView(big, wrapWrap());

        LinearLayout unitCol = new LinearLayout(ctx);
        unitCol.setOrientation(LinearLayout.VERTICAL);
        unitCol.setPadding(dp(6), 0, 0, dp(6));
        unitCol.addView(text("/100", 12, 0xB3FFFFFF, false, dp(2)), wrapWrap());
        TextView chats = text("聊过 " + p.chats + " 条", 10, 0x99FFFFFF, false, 0);
        unitCol.addView(chats, wrapWrap());
        scoreRow.addView(unitCol, wrapWrap());

        scoreRow.addView(new View(ctx), new LinearLayout.LayoutParams(0, dp(1), 1f));
        TextView tip = text("点数字直接改", 10, 0x99FFFFFF, false, dp(8));
        scoreRow.addView(tip, wrapWrap());
        hero.addView(scoreRow, matchWrap(dp(10)));

        hero.addView(bar(p.score, 0xFFFFFFFF, 0x38FFFFFF, 10), matchWrap(dp(12)));

        
        String meta = "今日 " + (p.dayDelta > 0 ? "+" : "") + p.dayDelta
                + " / ±" + AiConfig.affinityDailyCap()
                + "　·　AI 最近更新 "
                + (p.updatedAt > 0 ? FMT.format(new Date(p.updatedAt)) : "还没算过");
        TextView metaTv = text(meta, 10, 0xCCFFFFFF, false, 0);
        hero.addView(metaTv, matchWrap(dp(12)));

        final String fuid = p.uid;
        final Runnable commitScore = new Runnable() {
            @Override public void run() {
                AiAffinity.Profile cur = AiAffinity.get(fuid);
                int n = cur == null ? 50 : cur.score;
                try {
                    n = Integer.parseInt(big.getText().toString().trim());
                } catch (Throwable ignore) {
                }
                if (n < 0) n = 0;
                if (n > 100) n = 100;
                AiAffinity.setScore(fuid, n, "手动设定");
                big.post(new Runnable() {
                    @Override public void run() { render(); }
                });
            }
        };
        big.setOnEditorActionListener(new android.widget.TextView.OnEditorActionListener() {
            @Override public boolean onEditorAction(android.widget.TextView tv, int action,
                                                    android.view.KeyEvent ev) {
                if (action == EditorInfo.IME_ACTION_DONE || action == EditorInfo.IME_NULL) {
                    commitScore.run();
                    hideKeyboard(big);
                    big.clearFocus();
                    return true;
                }
                return false;
            }
        });
        big.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override public void onFocusChange(View view, boolean has) {
                if (!has) commitScore.run();
            }
        });

        LinearLayout btns = new LinearLayout(ctx);
        btns.setOrientation(LinearLayout.HORIZONTAL);
        int[] steps = new int[]{-10, -5, 5, 10};
        for (int i = 0; i < steps.length; i++) {
            final int st = steps[i];
            TextView btn = text(st > 0 ? "+" + st : String.valueOf(st), 14,
                    0xFFFFFFFF, true, 0);
            btn.setGravity(Gravity.CENTER);
            btn.setPadding(0, dp(9), 0, dp(9));
            GradientDrawable g = new GradientDrawable();
            g.setCornerRadius(dp(14));
            g.setColor(st > 0 ? 0x2EFFFFFF : 0x29FFFFFF);
            g.setStroke(dp(1), st > 0 ? 0x73FFFFFF : 0x59FFFFFF);
            btn.setBackgroundDrawable(g);
            final String uid = p.uid;
            final String nm2 = p.displayName();
            press(btn);
            btn.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    AiAffinity.setScore(uid, AiAffinity.get(uid).score + st,
                            st > 0 ? "手动加" : "手动减");
                    refreshUi(uid, nm2);
                }
            });
            btns.addView(btn, new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            if (i < steps.length - 1) {
                btns.addView(new View(ctx), new LinearLayout.LayoutParams(dp(8), dp(1)));
            }
        }
        hero.addView(btns, matchWrap(dp(10)));

        TextView redo = text("让 AI 重新评价", 13, color, true, 0);
        redo.setGravity(Gravity.CENTER);
        redo.setPadding(0, dp(11), 0, dp(11));
        GradientDrawable rg = new GradientDrawable();
        rg.setCornerRadius(dp(15));
        rg.setColor(0xF2FFFFFF);
        redo.setBackgroundDrawable(rg);
        press(redo);
        redo.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                AiEngine.refreshAffinity(p.uid);
                toast("已提交，几秒后再进来看看");
            }
        });
        hero.addView(redo, matchWrap(0));

        content.addView(hero, matchWrap(dp(14)));

        LinearLayout sumCard = whiteCard(dp(20), 0xF2FFFFFF);
        sumCard.setOrientation(LinearLayout.VERTICAL);
        sumCard.setPadding(dp(16), dp(14), dp(16), dp(15));

        LinearLayout sumTop = new LinearLayout(ctx);
        sumTop.setOrientation(LinearLayout.HORIZONTAL);
        sumTop.setGravity(Gravity.CENTER_VERTICAL);
        TextView dot = text("●", 11, color, true, 0);
        dot.setPadding(0, 0, dp(6), 0);
        sumTop.addView(dot, wrapWrap());
        sumTop.addView(text("整体评价", 14.5f, C_MAIN, true, 0),
                new LinearLayout.LayoutParams(0,
                        LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        TextView edit = pill("修改", 0x1FE85D9C);
        edit.setTextColor(C_PINK);
        press(edit);
        edit.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { askEditSummary(p.uid); }
        });
        sumTop.addView(edit, wrapWrap());
        sumCard.addView(sumTop, matchWrap(dp(10)));

        boolean empty = (p.summary == null || p.summary.trim().length() == 0);
        TextView body = text(empty
                        ? "还没有。AI 聊几句会自动写，也可以点「修改」自己填"
                        : p.summary.trim(),
                13, empty ? C_SUB : 0xFF44546A, false, 0);
        body.setLineSpacing(dp(4), 1f);
        sumCard.addView(body, matchWrap(0));
        content.addView(sumCard, matchWrap(dp(12)));

        if (!p.log.isEmpty()) {
            LinearLayout logCard = whiteCard(dp(20), 0xF2FFFFFF);
            logCard.setOrientation(LinearLayout.VERTICAL);
            logCard.setPadding(dp(16), dp(14), dp(16), dp(13));
            logCard.addView(text("最近变化", 14.5f, C_MAIN, true, dp(9)), matchWrap(0));
            for (int i = p.log.size() - 1; i >= 0; i--) {
                AiAffinity.Change c = p.log.get(i);
                final int dc = c.delta > 0 ? 0xFF5BAE93
                        : (c.delta < 0 ? 0xFFC9655F : C_SUB);
                TextView row = text(c.describe(), 11.5f, dc, false, 0);
                row.setPadding(0, dp(3), 0, dp(3));
                logCard.addView(row, matchWrap(0));
            }
            content.addView(logCard, matchWrap(dp(12)));
        
        try {
            java.util.List<AiAffinity.Milestone> ms = AiAffinity.milestones(p.uid);
            if (!ms.isEmpty()) {
                LinearLayout msCard = whiteCard(dp(18), 0xF2FFFFFF);
                msCard.setOrientation(LinearLayout.VERTICAL);
                msCard.setPadding(dp(18), dp(14), dp(18), dp(16));
                msCard.addView(text("里程碑", 14.5f, C_MAIN, true, dp(8)), matchWrap(0));
                int cnt = 0;
                for (int i = 0; i < ms.size() && i < 5; i++) {
                    AiAffinity.Milestone mm = ms.get(i);
                    java.text.SimpleDateFormat f =
                            new java.text.SimpleDateFormat("MM-dd", java.util.Locale.CHINA);
                    msCard.addView(text("· " + f.format(new java.util.Date(mm.at * 1000L))
                                    + "  关系变成「" + mm.tier + "」",
                            11.5f, C_SUB, false, dp(3)), matchWrap(0));
                    cnt++;
                }
                if (cnt == 0) msCard.addView(text("还没有", 11.5f, C_SUB, false, 0), matchWrap(0));
                content.addView(msCard, matchWrap(dp(12)));
            }
        } catch (Throwable ignore) {
        }
        }

        LinearLayout danger = whiteCard(dp(20), 0xF2FFFFFF);
        danger.setOrientation(LinearLayout.HORIZONTAL);
        danger.setPadding(dp(16), dp(14), dp(16), dp(14));
        danger.setGravity(Gravity.CENTER_VERTICAL);

        TextView reset = text("重置分数与评价", 13, C_MAIN, false, 0);
        danger.addView(reset, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        press(reset);
        reset.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                AiAffinity.reset(p.uid);
                toast("已重置");
                render();
            }
        });

        TextView del = pill("删档", 0x1FD9534F);
        del.setTextColor(0xFFD9534F);
        press(del);
        del.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { askDelete(p.uid, p.displayName()); }
        });
        danger.addView(del, wrapWrap());
        content.addView(danger, matchWrap(dp(12)));

        TextView note = text("点上面那个大数字就能直接改；± 按钮微调 —— 跟 AI 改的是同一份数据",
                10.5f, C_SUB, false, 0);
        note.setPadding(dp(8), dp(2), dp(8), dp(12));
        note.setLineSpacing(dp(2), 1f);
        content.addView(note, matchWrap(0));
    }

    private void refreshUi(String uid, String name) {
        AiAffinity.Profile p = AiAffinity.get(uid);
        toast(name + " 现在是 " + (p == null ? "?" : String.valueOf(p.score))
                + " 分（" + (p == null ? "?" : p.tier()) + "）");
        render();
    }

    

    private void askEditSummary(final String uid) {
        final AiAffinity.Profile p = AiAffinity.get(uid);
        if (p == null) return;
        CatSheet.inputMulti(ctx, "改整体评价", "他是什么样的人、聊得来吗…",
                p.summary == null ? "" : p.summary, new CatSheet.OnText() {
                    @Override public void run(String s) {
                        AiAffinity.setSummary(uid, s);
                        toast("已保存");
                        render();
                    }
                });
    }

    private void askDelete(final String uid, String name) {
        CatSheet.confirm(ctx, "删掉「" + name + "」的档案？",
                "好感度和整体评价都会消失，下次聊天从 "
                        + AiConfig.affinityInit() + " 分重新开始。",
                "删除", true, new Runnable() {
                    @Override public void run() {
                        AiAffinity.remove(uid);
                        currentUid = null;
                        toast("已删除");
                        render();
                    }
                });
    }

    

    
    private View bar(int score, int fillColor, int trackColor, int heightDp) {
        if (score < 0) score = 0;
        if (score > 100) score = 100;
        LinearLayout wrap = new LinearLayout(ctx);
        wrap.setOrientation(LinearLayout.HORIZONTAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(6));
        bg.setColor(trackColor);
        wrap.setBackgroundDrawable(bg);

        View fill = new View(ctx);
        GradientDrawable fg = new GradientDrawable();
        fg.setCornerRadius(dp(6));
        fg.setColor(fillColor);
        fill.setBackgroundDrawable(fg);
        wrap.addView(fill, new LinearLayout.LayoutParams(0, dp(heightDp),
                Math.max(1, score)));

        View rest = new View(ctx);
        wrap.addView(rest, new LinearLayout.LayoutParams(0, dp(heightDp),
                Math.max(1, 100 - score)));
        return wrap;
    }

    private static int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | ((alpha & 0xFF) << 24);
    }

    private static int blend(int c1, int c2, float t) {
        int a = (int) (((c1 >>> 24) & 0xFF) * (1 - t) + ((c2 >>> 24) & 0xFF) * t);
        int r = (int) (((c1 >> 16) & 0xFF) * (1 - t) + ((c2 >> 16) & 0xFF) * t);
        int g = (int) (((c1 >> 8) & 0xFF) * (1 - t) + ((c2 >> 8) & 0xFF) * t);
        int b = (int) ((c1 & 0xFF) * (1 - t) + (c2 & 0xFF) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int lighten(int c, float t) { return blend(c, 0xFFFFFFFF, t); }

    private static int darken(int c, float t) { return blend(c, 0xFF1B2430, t); }

    private String firstChar(String s) {
        if (s == null || s.length() == 0) return "?";
        return s.substring(0, 1);
    }
}
