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
import com.cathelperawa.owo.app.ui.CatSheet;
import com.cathelperawa.owo.app.ui.Page;
import com.cathelperawa.owo.app.ui.SwitchBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class AiAdminPage extends Page {

    private static final int C_MAIN = 0xFF3C4B60;
    private static final int C_SUB = 0xFF8A9BB0;
    private static final int C_PINK = 0xFFE85D9C;
    private static final int C_RED = 0xFFD9534F;

    private LinearLayout body;
    private LinearLayout content;

    public AiAdminPage(Host host) {
        super(host);
    }

    @Override
    public View build() {
        View[] out = new View[2];
        body = pageRoot("管理员", "谁能让 AI 调危险功能", out, true, 13, "bg_ai", 0x66);
        content = new LinearLayout(ctx);
        content.setOrientation(LinearLayout.VERTICAL);
        body.addView(content, matchWrap(0));
        render();
        return out[0];
    }

    private void render() {
        content.removeAllViews();
        List<String> admins = AiConfig.admins();

        LinearLayout head = whiteCard(dp(22), 0xF2FFFFFF);
        head.setOrientation(LinearLayout.VERTICAL);
        head.setPadding(dp(18), dp(16), dp(18), dp(16));
        head.addView(text("管理员名单", 18, C_MAIN, true, dp(4)), matchWrap(0));
        head.addView(text(admins.size() == 0
                ? "还没设管理员 · 现在谁都不能调危险功能"
                : admins.size() + " 位 · 只有名单里的人能调危险功能",
                11, C_SUB, false, 0), matchWrap(0));

        TextView rule = text("号主（你自己）天然算管理员，不用加进名单", 10.5f,
                0xFF8A9BB0, false, 0);
        rule.setPadding(0, dp(8), 0, 0);
        head.addView(rule, matchWrap(0));
        content.addView(head, matchWrap(dp(14)));

        content.addView(wakeCard(), matchWrap(dp(14)));

        if (admins.isEmpty()) {
            LinearLayout warn = whiteCard(dp(18), 0xF7FFF4F4);
            warn.setPadding(dp(16), dp(14), dp(16), dp(14));
            TextView t = text("安全默认：危险功能全部锁死", 13, C_RED, true, dp(6));
            warn.addView(t, matchWrap(0));
            warn.addView(text("以后 AI 想改配置、收发文件、操作设备，非管理员一律拒绝。\n"
                    + "要放开，就先在下面加人。", 11, C_SUB, false, 0), matchWrap(0));
            content.addView(warn, matchWrap(dp(12)));
        }

        LinearLayout addCard = whiteCard(dp(18), 0xEEFFFFFF);
        addCard.setOrientation(LinearLayout.HORIZONTAL);
        addCard.setGravity(Gravity.CENTER_VERTICAL);
        addCard.setPadding(dp(16), dp(15), dp(16), dp(15));
        LinearLayout addTxt = new LinearLayout(ctx);
        addTxt.setOrientation(LinearLayout.VERTICAL);
        addTxt.addView(text("＋ 添加管理员", 15, C_PINK, true, dp(3)), matchWrap(0));
        addTxt.addView(text("从好友里挑，或直接敲 QQ 号", 10.5f, C_SUB, false, 0), matchWrap(0));
        addCard.addView(addTxt, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        addCard.addView(text("›", 20, 0xFFB6C1D0, false, 0));
        press(addCard);
        addCard.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { askAdd(); }
        });
        content.addView(addCard, matchWrap(dp(12)));

        for (int i = 0; i < admins.size(); i++) {
            content.addView(adminCard(admins.get(i)), matchWrap(dp(10)));
        }

        TextView note = text("危险功能包括：改配置、收发文件、操作设备、执行命令这类\n"
                        + "现在还没有，但闸门已经建好 —— 以后新功能只要过 AiGuard 就受这里管",
                10.5f, C_SUB, false, 0);
        note.setPadding(dp(8), dp(10), dp(8), dp(12));
        note.setLineSpacing(dp(3), 1f);
        content.addView(note, matchWrap(0));
    }

    private View adminCard(final String uin) {
        LinearLayout card = whiteCard(dp(18), 0xEEFFFFFF);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(16), dp(13), dp(14), dp(13));

        TextView initial = text(firstChar(nameOf(uin)), 14, 0xFFFFFFFF, true, 0);
        initial.setGravity(Gravity.CENTER);
        GradientDrawable round = new GradientDrawable();
        round.setShape(GradientDrawable.OVAL);
        round.setColor(C_PINK);
        round.setStroke(dp(2), 0x66FFFFFF);
        initial.setBackgroundDrawable(round);
        card.addView(initial, new LinearLayout.LayoutParams(dp(38), dp(38)));

        LinearLayout mid = new LinearLayout(ctx);
        mid.setOrientation(LinearLayout.VERTICAL);
        mid.setPadding(dp(12), 0, dp(8), 0);
        mid.addView(text(nameOf(uin), 14.5f, C_MAIN, true, dp(2)), matchWrap(0));
        mid.addView(text("可调用危险功能 · 已授权", 10.5f, C_SUB, false, 0),
                matchWrap(0));
        card.addView(mid, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView del = pill("移除", 0x1FD9534F);
        del.setTextColor(C_RED);
        press(del);
        del.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                CatSheet.confirm(ctx, "把「" + nameOf(uin) + "」移出管理员？",
                        "移除后他就不能再让 AI 做危险操作了。", "移除", true,
                        new Runnable() {
                            @Override public void run() {
                                AiConfig.removeAdmin(uin);
                                toast("已移除");
                                render();
                            }
                        });
            }
        });
        card.addView(del, wrapWrap());
        return card;
    }

    

    private void askAdd() {
        CatSheet.pick(ctx, "怎么加", new String[]{"从好友列表里选", "手动输入 QQ 号"},
                new CatSheet.OnPick() {
                    @Override public void run(int index, String label) {
                        if (index == 0) pickFriend();
                        else inputUin();
                    }
                });
    }

    private void pickFriend() {
        host.pushPage(new AiAdminPickPage(host).build());
    }

    private void inputUin() {
        CatSheet.input(ctx, "添加管理员", "在这里填对方的 QQ 号", "", new CatSheet.OnText() {
            @Override public void run(String s) {
                String v = s == null ? "" : s.trim();
                if (v.length() < 5 || !v.matches("[0-9]+")) {
                    toast("得填纯数字的 QQ 号哦");
                    return;
                }
                AiConfig.addAdmin(v);
                toast("已添加");
                render();
            }
        });
    }

    

    
    private String nameOf(String uin) {
        try {
            String n = IdentityApi.buddyName(uin);
            if (n != null && n.trim().length() > 0) return n.trim();
        } catch (Throwable ignore) {
        }
        return uin;
    }

    private String firstChar(String s) {
        if (s == null || s.length() == 0) return "?";
        return s.substring(0, 1);
    }

    

    
    private View wakeCard() {
        LinearLayout c = whiteCard(dp(20), 0xF2FFFFFF);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(16), dp(8), dp(16), dp(14));

        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(10), 0, dp(2));
        LinearLayout txt = new LinearLayout(ctx);
        txt.setOrientation(LinearLayout.VERTICAL);
        txt.addView(text("唤醒词", 14.5f, C_MAIN, true, dp(2)), matchWrap(0));
        txt.addView(text("念一句就能叫醒 · 只有管理员和你本人 · 任何群都行",
                11, C_SUB, false, 0), matchWrap(0));
        row.addView(txt, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        final SwitchBox box = new SwitchBox(ctx, AiConfig.adminWakeOn());
        View.OnClickListener toggle = new View.OnClickListener() {
            @Override public void onClick(View v) {
                boolean z = !AiConfig.adminWakeOn();
                AiConfig.setAdminWakeOn(z);
                box.setChecked(z);
                press(v);
                render();
            }
        };
        box.setOnClickListener(toggle);
        row.setOnClickListener(toggle);
        row.addView(box, new LinearLayout.LayoutParams(dp(52), dp(30)));
        c.addView(row, matchWrap(0));

        if (!AiConfig.adminWakeOn()) {
            TextView t = text("现在是关的：念唤醒词不会有任何反应", 10.5f, C_SUB, false, 0);
            t.setPadding(0, dp(4), 0, 0);
            c.addView(t, matchWrap(0));
            return c;
        }

        c.addView(text("唤醒词（逗号分隔，可多个）", 11, C_SUB, false, 0), matchWrap(dp(6)));
        final EditText et = new EditText(ctx);
        et.setText(AiConfig.wakeWordsRaw());
        et.setTextSize(14);
        et.setTextColor(C_MAIN);
        et.setSingleLine(true);
        et.setBackgroundColor(0x00000000);
        et.setPadding(dp(12), dp(10), dp(12), dp(10));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0x66F0F4F8);
        bg.setCornerRadius(dp(10));
        et.setBackground(bg);
        c.addView(et, matchWrap(0));

        final TextView hint = text("", 10.5f, C_SUB, false, 0);
        hint.setPadding(0, dp(6), 0, 0);
        c.addView(hint, matchWrap(0));
        final java.util.List<String> words = new java.util.ArrayList<String>();
        String[] sp = AiConfig.wakeWordsRaw().split("[,，、\\s]+");
        for (int i = 0; i < sp.length; i++) {
            if (sp[i].trim().length() > 0) words.add(sp[i].trim());
        }
        hint.setText(wakeHint(words));

        et.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable e) {
                String raw = e.toString();
                AiConfig.setWakeWords(raw);
                java.util.List<String> ws = new java.util.ArrayList<String>();
                String[] arr = raw.split("[,，、\\s]+");
                for (int i = 0; i < arr.length; i++) {
                    if (arr[i].trim().length() > 0) ws.add(arr[i].trim());
                }
                hint.setText(ws.isEmpty() ? "至少留一个词，否则叫不醒" : wakeHint(ws));
            }
        });

        TextView sep2 = text("管理员专用词（可选，额外的叫法）", 11, C_SUB, false, 0);
        sep2.setPadding(0, dp(14), 0, dp(2));
        c.addView(sep2, matchWrap(0));

        final EditText et2 = new EditText(ctx);
        et2.setText(AiConfig.adminWakeWordsRaw());
        et2.setHint("例如：鲸鲸主人");
        et2.setTextSize(14);
        et2.setTextColor(C_MAIN);
        et2.setSingleLine(true);
        et2.setPadding(dp(12), dp(10), dp(12), dp(10));
        GradientDrawable bg2 = new GradientDrawable();
        bg2.setColor(0x66F0F4F8);
        bg2.setCornerRadius(dp(10));
        et2.setBackground(bg2);
        c.addView(et2, matchWrap(dp(4)));

        final TextView hint2 = text(AiConfig.adminWakeWordsRaw().length() == 0
                        ? "留空也行：上面那些唤醒词已经够用"
                        : "这些词同样只认管理员/号主，任何会话都能用",
                10.5f, C_SUB, false, 0);
        hint2.setPadding(0, dp(6), 0, 0);
        c.addView(hint2, matchWrap(0));

        et2.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable e) {
                String raw = e.toString();
                AiConfig.setAdminWakeWords(raw);
                hint2.setText(raw.trim().length() == 0
                        ? "留空也行：上面那些唤醒词已经够用"
                        : "这些词同样只认管理员/号主，任何会话都能用");
            }
        });

        TextView rule = text("· 只认管理员名单 + 你本人，其他人念了无效\n"
                        + "· **任何群、任何私聊都能叫醒**（不看「允许的会话」）\n"
                        + "  —— 白名单管的是「它自己要不要接话」，不管唤醒\n"
                        + "· **上面那个 AI 总开关关着，也照样能叫醒**\n"
                        + "  （总开关只管「它自己要不要插话」，唤醒是你主动叫它）\n"
                        + "· 同一个会话 10 秒内只响应一次，防刷屏",
                10.5f, C_SUB, false, 0);
        rule.setPadding(0, dp(8), 0, 0);
        c.addView(rule, matchWrap(0));
        return c;
    }

    private String wakeHint(java.util.List<String> ws) {
        StringBuilder sb = new StringBuilder("现在可用：");
        for (int i = 0; i < ws.size(); i++) {
            if (i > 0) sb.append(" / ");
            sb.append("「").append(ws.get(i)).append("」");
        }
        return sb.toString();
    }
}
