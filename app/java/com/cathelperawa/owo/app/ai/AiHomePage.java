package com.cathelperawa.owo.app.ai;

import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.cathelperawa.owo.app.BuildInfo;
import com.cathelperawa.owo.app.api.EventApi;
import com.cathelperawa.owo.app.api.NetApi;
import com.cathelperawa.owo.app.api.TaskApi;
import com.cathelperawa.owo.app.ui.Page;

public final class AiHomePage extends Page {

    private static final int C_MAIN = 0xFF44546A;
    private static final int C_SUB = 0xFF8A9BB0;
    private static final int C_PINK = 0xFFE85D9C;

    public AiHomePage(Host host) {
        super(host);
    }

    @Override
    public View build() {
        View[] out = new View[2];

        LinearLayout root = pageRoot("猫娘 AI", "", out,
                false, 13, "bg_ai", 0x66);

        root.addView(heroCard(), matchWrap(dp(14)));
        root.addView(settingsEntry(), matchWrap(dp(14)));
        root.addView(personaEntry(), matchWrap(dp(14)));
        root.addView(providerEntry(), matchWrap(dp(14)));
        root.addView(targetsEntry(), matchWrap(dp(14)));
        root.addView(memoryEntry(), matchWrap(dp(14)));
        root.addView(mindEntry(), matchWrap(dp(14)));
        root.addView(affinityEntry(), matchWrap(dp(14)));
        root.addView(adminEntry(), matchWrap(dp(14)));
        root.addView(emojiEntry(), matchWrap(dp(14)));
        root.addView(statusCard(), matchWrap(dp(14)));
        root.addView(capabilityCard(), matchWrap(dp(14)));
        root.addView(footNote(), matchWrap(0));

        return out[0];
    }

    

    
    private View heroCard() {
        LinearLayout card = whiteCard(dp(22), 0xEEFFFFFF);
        card.setPadding(dp(18), dp(16), dp(18), dp(16));

        LinearLayout chips = new LinearLayout(ctx);
        chips.setOrientation(LinearLayout.HORIZONTAL);

        TextView apiChip = com.cathelperawa.owo.app.ui.UiKit.chip(ctx, "API");
        apiChip.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                press(v);
                host.showApiDialog();
            }
        });
        chips.addView(apiChip);

        TextView termChip = com.cathelperawa.owo.app.ui.UiKit.chip(ctx, "终端");
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        tlp.leftMargin = dp(8);
        chips.addView(termChip, tlp);
        termChip.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                press(v);
                
                com.cathelperawa.owo.app.ui.TerminalPanel.show(activity());
            }
        });
        card.addView(chips, matchWrap(dp(12)));

        LinearLayout titleRow = new LinearLayout(ctx);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout titleTxt = new LinearLayout(ctx);
        titleTxt.setOrientation(LinearLayout.VERTICAL);
        titleTxt.addView(text("猫娘 AI", 23, C_MAIN, true, dp(5)), matchWrap(0));
        titleTxt.addView(text("接入大模型，让这个号自己会聊", 12, C_SUB, false, 0), matchWrap(0));
        titleRow.addView(titleTxt, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        final com.cathelperawa.owo.app.ui.SwitchBox master =
                new com.cathelperawa.owo.app.ui.SwitchBox(ctx, AiConfig.enable());
        master.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                boolean z = !AiConfig.enable();
                AiConfig.setBool("ai_enable", z);
                master.setChecked(z);
                press(v);
                
                try {
                    host.setPageView(build());
                } catch (Throwable ignore) {
                }
            }
        });
        titleRow.addView(master, new LinearLayout.LayoutParams(dp(52), dp(30)));

        card.addView(titleRow, matchWrap(dp(12)));

        String nick = "";
        String uin = "";
        try {
            nick = com.cathelperawa.owo.app.bridge.QQEnv.currentNickname();
            uin = com.cathelperawa.owo.app.bridge.QQEnv.currentUin();
        } catch (Throwable ignore) {
        }
        if (nick != null && nick.length() > 0) {
            card.addView(text("当前账号  " + nick + (uin == null || uin.length() == 0
                            ? "" : "  (" + uin + ")"),
                    11, 0xFF7C8DB5, false, dp(12)), matchWrap(0));
        }

        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(pill("M1 骨架", 0xFFFFC2DF), wrapWrap());

        LinearLayout.LayoutParams lp = wrapWrap();
        lp.leftMargin = dp(8);
        row.addView(pill("独立子系统", 0xFFD8E6FF), lp);

        card.addView(row, matchWrap(0));
        return card;
    }

    private View emojiEntry() {
        LinearLayout card = whiteCard(dp(18), 0xEEFFFFFF);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(18), dp(16), dp(14), dp(16));

        LinearLayout txt = new LinearLayout(ctx);
        txt.setOrientation(LinearLayout.VERTICAL);
        txt.addView(text("表情库", 16, C_MAIN, true, dp(3)), matchWrap(0));

        int n = 0;
        int un = 0;
        try {
            java.util.List<AiEmoji.Item> list = AiEmoji.load();
            n = list.size();
            for (int i = 0; i < list.size(); i++) {
                if ("未分类".equals(list.get(i).label)) un++;
            }
        } catch (Throwable ignore) {
        }
        String sub;
        if (n == 0) sub = "还没有表情 · 点进去添加几张";
        else if (un > 0) sub = n + " 张 · " + un + " 张待分类";
        else sub = n + " 张 · 聊天时会自己挑";
        txt.addView(text(sub, 11, C_SUB, false, 0), matchWrap(0));
        card.addView(txt, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        card.addView(text("›", 20, 0xFFB6C1D0, false, 0));

        press(card);
        card.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                host.pushPage(new AiEmojiPage(host).build());
            }
        });
        return card;
    }

    private View personaEntry() {
        LinearLayout card = whiteCard(dp(18), 0xEEFFFFFF);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(18), dp(16), dp(14), dp(16));

        LinearLayout txt = new LinearLayout(ctx);
        txt.setOrientation(LinearLayout.VERTICAL);
        txt.addView(text("人设", 16, C_MAIN, true, dp(3)), matchWrap(0));

        String p = "";
        try {
            p = AiConfig.persona();
        } catch (Throwable ignore) {
        }
        String sub;
        if (p == null || p.trim().length() == 0) {
            sub = "还没写 · 点进去选模板或自己写";
        } else {
            String head = p.trim().replace("\n", " ");
            if (head.length() > 22) head = head.substring(0, 22) + "…";
            sub = p.trim().length() + " 字 · " + head;
        }
        txt.addView(text(sub, 11, C_SUB, false, 0), matchWrap(0));
        card.addView(txt, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        card.addView(text("›", 20, 0xFFB6C1D0, false, 0));

        press(card);
        card.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                host.pushPage(new AiPersonaPage(host).build());
            }
        });
        return card;
    }

    private View settingsEntry() {
        LinearLayout card = whiteCard(dp(18), 0xEEFFFFFF);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(18), dp(16), dp(14), dp(16));

        LinearLayout txt = new LinearLayout(ctx);
        txt.setOrientation(LinearLayout.VERTICAL);
        txt.addView(text("设置", 16, C_MAIN, true, dp(3)), matchWrap(0));

        String model = "未选择";
        try {
            AiProvider p = AiProvider.current();
            if (p != null) model = p.model == null || p.model.length() == 0
                    ? p.displayName() : p.model;
        } catch (Throwable ignore) {
        }
        StringBuilder sb = new StringBuilder("模型 ").append(model);
        sb.append(" · 识图").append(AiConfig.vision() ? "开" : "关");
        sb.append(" · 记忆相串").append(AiConfig.cross() ? "开" : "关");
        txt.addView(text(sb.toString(), 11, C_SUB, false, 0), matchWrap(0));
        card.addView(txt, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        card.addView(text("›", 20, 0xFFB6C1D0, false, 0));

        press(card);
        card.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                host.pushPage(new AiSettingsPage(host).build());
            }
        });
        return card;
    }

    private View adminEntry() {
        LinearLayout card = whiteCard(dp(18), 0xEEFFFFFF);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(18), dp(16), dp(14), dp(16));

        LinearLayout txt = new LinearLayout(ctx);
        txt.setOrientation(LinearLayout.VERTICAL);
        txt.addView(text("管理员", 16, C_MAIN, true, dp(3)), matchWrap(0));

        String sub;
        int n = 0;
        try {
            n = AiConfig.adminCount();
        } catch (Throwable ignore) {
        }
        sub = n == 0
                ? "未设置 · 危险功能全部锁死（安全默认）"
                : n + " 位 · 只有他们能让 AI 调危险功能";
        TextView s2 = text(sub, 11, n == 0 ? 0xFFD9534F : C_SUB, false, 0);
        txt.addView(s2, matchWrap(0));
        card.addView(txt, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        card.addView(text("\u203a", 20, 0xFFB6C1D0, false, 0));

        press(card);
        card.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                host.pushPage(new AiAdminPage(host).build());
            }
        });
        return card;
    }

    private View affinityEntry() {
        LinearLayout card = whiteCard(dp(18), 0xEEFFFFFF);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(18), dp(16), dp(14), dp(16));

        LinearLayout txt = new LinearLayout(ctx);
        txt.setOrientation(LinearLayout.VERTICAL);
        txt.addView(text("好感度", 16, C_MAIN, true, dp(3)), matchWrap(0));

        String sub;
        try {
            sub = AiConfig.affinityOn()
                    ? AiAffinity.brief()
                    : "已关闭 · 不记录也不影响说话";
        } catch (Throwable t) {
            sub = AiAffinity.brief();
        }
        txt.addView(text(sub, 11, C_SUB, false, 0), matchWrap(0));
        card.addView(txt, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        card.addView(text("\u203a", 20, 0xFFB6C1D0, false, 0));

        press(card);
        card.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                host.pushPage(new AiAffinityPage(host).build());
            }
        });
        return card;
    }

    private View memoryEntry() {
        LinearLayout card = whiteCard(dp(18), 0xEEFFFFFF);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(18), dp(16), dp(14), dp(16));

        LinearLayout txt = new LinearLayout(ctx);
        txt.setOrientation(LinearLayout.VERTICAL);
        txt.addView(text("对话记忆", 16, C_MAIN, true, dp(3)), matchWrap(0));

        int peers = 0;
        int total = 0;
        try {
            peers = AiStore.peerCount();
            total = AiStore.totalCount();
        } catch (Throwable ignore) {
        }
        txt.addView(text(total == 0
                        ? "还没有记录 · 每个群 / 好友各记一份"
                        : peers + " 个会话 · " + total + " 条记录 · 可逐条改删",
                11, C_SUB, false, 0), matchWrap(0));
        card.addView(txt, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        card.addView(text("›", 20, 0xFFB6C1D0, false, 0));

        press(card);
        card.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                host.pushPage(new AiMemoryPage(host).build());
            }
        });
        return card;
    }

    private View targetsEntry() {
        LinearLayout card = whiteCard(dp(18), 0xEEFFFFFF);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(18), dp(16), dp(14), dp(16));
        LinearLayout txt = new LinearLayout(ctx);
        txt.setOrientation(LinearLayout.VERTICAL);
        txt.addView(text("白名单", 16, C_MAIN, true, dp(3)), matchWrap(0));

        String raw = "";
        try {
            raw = AiConfig.targetsRaw();
        } catch (Throwable ignore) {
        }
        int n = 0;
        if (raw != null && raw.trim().length() > 0) {
            String[] arr = raw.split("[,，\\s|｜]+");
            for (int i = 0; i < arr.length; i++) {
                if (arr[i] != null && arr[i].trim().length() > 0) n++;
            }
        }
        String sub = (n == 0)
                ? "不限 · 所有会话都能聊（点进去挑）"
                : "已选 " + n + " 个会话 · 其他一律不理";
        txt.addView(text(sub, 11, n == 0 ? C_SUB : 0xFF3FA96A, false, 0), matchWrap(0));
        card.addView(txt, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        card.addView(text("\u203a", 20, 0xFFB6C1D0, false, 0));
        press(card);
        card.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                host.pushPage(new AiTargetsPage(host).build());
            }
        });
        return card;
    }

    private View providerEntry() {
        LinearLayout card = whiteCard(dp(18), 0xEEFFFFFF);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(18), dp(16), dp(14), dp(16));

        LinearLayout txt = new LinearLayout(ctx);
        txt.setOrientation(LinearLayout.VERTICAL);
        txt.addView(text("模型提供商", 16, C_MAIN, true, dp(3)), matchWrap(0));

        int n = 0;
        String cur = "未配置";
        try {
            java.util.List<com.cathelperawa.owo.app.ai.AiProvider> list =
                    com.cathelperawa.owo.app.ai.AiProvider.loadAll();
            n = list.size();
            com.cathelperawa.owo.app.ai.AiProvider c =
                    com.cathelperawa.owo.app.ai.AiProvider.current();
            if (c != null) cur = c.displayName() + " · " + c.model;
        } catch (Throwable ignore) {
        }
        txt.addView(text(n + " 个提供商 · " + cur, 11, C_SUB, false, 0), matchWrap(0));
        card.addView(txt, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        card.addView(text("›", 20, 0xFFB6C1D0, false, 0));

        press(card);
        card.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                host.pushPage(new AiProvidersPage(host).build());
            }
        });
        return card;
    }

    private View statusCard() {
        LinearLayout card = whiteCard(dp(18), 0xE6FFFFFF);
        card.setPadding(dp(18), dp(14), dp(18), dp(14));

        card.addView(text("── 子系统状态 ──", 12, C_SUB, true, dp(10)), matchWrap(0));

        String event = "事件   监听 " + EventApi.listenerCount()
                + " · 派发 " + EventApi.dispatchedCount()
                + " · " + (EventApi.isRunning() ? "线程运行中" : "未启动");
        card.addView(text(event, 13, C_MAIN, false, dp(6)), matchWrap(0));

        String net = "网络   排队 " + NetApi.pending()
                + " · 超时 " + (NetApi.DEFAULT_TIMEOUT / 1000) + "s"
                + " · 上限 " + (NetApi.MAX_BODY / 1024 / 1024) + "MB";
        card.addView(text(net, 13, C_MAIN, false, dp(6)), matchWrap(0));

        String task = "定时   存活 " + TaskApi.count()
                + " · 累计异常 " + TaskApi.errorCount();
        card.addView(text(task, 13, C_MAIN, false, dp(2)), matchWrap(0));

        return card;
    }

    private View capabilityCard() {
        LinearLayout card = whiteCard(dp(18), 0xE6FFFFFF);
        card.setPadding(dp(18), dp(16), dp(18), dp(16));

        card.addView(text("能力体检", 16, C_MAIN, true, dp(4)), matchWrap(0));
        card.addView(text("逐项真测：能发什么、能读什么、管不管得了群", 12, C_SUB, false, dp(12)),
                matchWrap(0));

        TextView btn = text("开始体检 ›", 14, 0xFFFFFFFF, true, 0);
        btn.setGravity(Gravity.CENTER);
        btn.setPadding(0, dp(12), 0, dp(12));
        android.graphics.drawable.GradientDrawable bg =
                new android.graphics.drawable.GradientDrawable();
        bg.setColor(C_PINK);
        bg.setCornerRadius(dp(15));
        btn.setBackgroundDrawable(bg);
        press(btn);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                
                host.pushPage(new AiCapabilityPage(host).build());
            }
        });
        card.addView(btn, matchWrap(0));
        return card;
    }

    private View footNote() {
        String v = "猫娘助手 v" + BuildInfo.VERSION_NAME + " · AI 子系统";
        TextView t = text(v, 11, 0xFFA9B7C9, false, 0);
        t.setGravity(Gravity.CENTER);
        return t;
    }

    

    
    private TextView kv(String k, String v) {
        return text(k + "   " + v, 13, C_MAIN, false, dp(6));
    }

    private View mindEntry() {
        LinearLayout card = whiteCard(dp(18), 0xEEFFFFFF);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(18), dp(16), dp(14), dp(16));
        LinearLayout txt = new LinearLayout(ctx);
        txt.setOrientation(LinearLayout.VERTICAL);
        txt.addView(text("意识", 16, C_MAIN, true, dp(3)), matchWrap(0));
        String sub;
        try {
            sub = AiConfig.moodOn()
                    ? "心情 " + AiMood.describe()
                    : "已关闭 · 语气始终一致";
        } catch (Throwable t) {
            sub = "心情 / 对方状态 / 主动开口";
        }
        txt.addView(text(sub, 11, C_SUB, false, 0), matchWrap(0));
        card.addView(txt, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        card.addView(text("\u203a", 20, 0xFFB6C1D0, false, 0));
        press(card);
        card.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                host.pushPage(new AiMindPage(host).build());
            }
        });
        return card;
    }

}
