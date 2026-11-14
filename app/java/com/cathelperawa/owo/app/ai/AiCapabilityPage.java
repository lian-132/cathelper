package com.cathelperawa.owo.app.ai;

import android.app.Activity;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.cathelperawa.owo.app.ui.Page;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class AiCapabilityPage extends Page {

    private static final int C_MAIN = 0xFF44546A;
    private static final int C_SUB = 0xFF8A9BB0;
    private static final int C_OK = 0xFF3FA96A;
    private static final int C_BAD = 0xFFE06666;

    private LinearLayout body;

    private LinearLayout content;

    public AiCapabilityPage(Host host) {
        super(host);
    }

    @Override
    public View build() {
        View[] out = new View[2];
        body = pageRoot("能力体检", "在 TIM 进程内真测，不发消息", out);
        content = new LinearLayout(ctx);
        content.setOrientation(LinearLayout.VERTICAL);
        body.addView(content, matchWrap(0));

        content.addView(titleCard("检测中…", "正在逐项调用真实接口"), matchWrap(dp(14)));
        start();
        return out[0];
    }

    private void start() {
        AiCapability.runAsync(new AiCapability.Listener() {
            @Override
            public void onDone(final List<AiCapability.Item> items, final long costMs) {
                if (!(ctx instanceof Activity)) return;
                ((Activity) ctx).runOnUiThread(new Runnable() {
                    @Override public void run() {
                        render(items, costMs);
                    }
                });
            }
        });
    }

    private void render(List<AiCapability.Item> items, long costMs) {

        content.removeAllViews();

        int ok = 0;
        for (int i = 0; i < items.size(); i++) if (items.get(i).ok) ok++;

        content.addView(titleCard(ok + " / " + items.size() + " 可用",
                "用时 " + costMs + "ms · 写操作只验证不发送"), matchWrap(dp(14)));

        Map<String, List<AiCapability.Item>> groups =
                new LinkedHashMap<String, List<AiCapability.Item>>();
        for (int i = 0; i < items.size(); i++) {
            AiCapability.Item it = items.get(i);
            List<AiCapability.Item> l = groups.get(it.group);
            if (l == null) {
                l = new ArrayList<AiCapability.Item>();
                groups.put(it.group, l);
            }
            l.add(it);
        }

        for (Map.Entry<String, List<AiCapability.Item>> e : groups.entrySet()) {
            content.addView(groupCard(e.getKey(), e.getValue()), matchWrap(dp(12)));
        }

        content.addView(probeCard(), matchWrap(dp(14)));
        content.addView(retryButton(), matchWrap(dp(18)));
        content.addView(footNote(), matchWrap(0));
    }

    private View titleCard(String title, String sub) {
        LinearLayout card = whiteCard(dp(20), 0xEEFFFFFF);
        card.setPadding(dp(18), dp(16), dp(18), dp(16));
        card.addView(text(title, 22, C_MAIN, true, dp(5)), matchWrap(0));
        card.addView(text(sub, 12, C_SUB, false, 0), matchWrap(0));
        return card;
    }

    private View groupCard(String group, List<AiCapability.Item> list) {
        LinearLayout card = whiteCard(dp(18), 0xE6FFFFFF);
        card.setPadding(dp(18), dp(14), dp(18), dp(12));

        card.addView(text("── " + group + " ──", 12, C_SUB, true, dp(9)), matchWrap(0));

        for (int i = 0; i < list.size(); i++) {
            AiCapability.Item it = list.get(i);

            LinearLayout row = new LinearLayout(ctx);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.TOP);

            TextView mark = text(it.ok ? "✓" : "✗", 14, it.ok ? C_OK : C_BAD, true, 0);
            mark.setWidth(dp(22));
            row.addView(mark, new LinearLayout.LayoutParams(dp(22),
                    LinearLayout.LayoutParams.WRAP_CONTENT));

            LinearLayout col = new LinearLayout(ctx);
            col.setOrientation(LinearLayout.VERTICAL);

            TextView name = text(it.name, 13, C_MAIN, false, dp(1));
            col.addView(name, matchWrap(0));

            if (it.detail != null && it.detail.length() > 0) {
                TextView d = text(it.detail, 11, C_SUB, false, 0);
                d.setTypeface(Typeface.MONOSPACE);
                col.addView(d, matchWrap(0));
            }
            row.addView(col, new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            card.addView(row, matchWrap(dp(i == list.size() - 1 ? 0 : 9)));
        }
        return card;
    }

    private View probeCard() {
        LinearLayout c = whiteCard(dp(18), 0xE6FFFFFF);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(16), dp(14), dp(16), dp(14));
        c.addView(text("工具调用自检", 14.5f, C_MAIN, true, dp(4)), matchWrap(0));
        c.addView(text("发一次带 tools 的请求，看当前模型支不支持 function calling。\n"
                + "结果写进 files/cathelper/tools_probe.txt", 11, C_SUB, false, dp(10)),
                matchWrap(0));

        TextView btn = text("跑一次", 13, 0xFFE85D9C, true, 0);
        btn.setGravity(Gravity.CENTER);
        btn.setPadding(0, dp(10), 0, dp(10));
        android.graphics.drawable.GradientDrawable g =
                new android.graphics.drawable.GradientDrawable();
        g.setCornerRadius(dp(13));
        g.setColor(0x1FE85D9C);
        g.setStroke(dp(1), 0x55E85D9C);
        btn.setBackgroundDrawable(g);
        press(btn);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                AiEngine.probeTools();
                toast("已发出，几秒后看日志");
            }
        });
        c.addView(btn, matchWrap(0));
        return c;
    }

    private View retryButton() {
        TextView btn = text("重新体检", 14, 0xFFFFFFFF, true, 0);
        btn.setGravity(Gravity.CENTER);
        btn.setPadding(0, dp(13), 0, dp(13));
        android.graphics.drawable.GradientDrawable bg =
                new android.graphics.drawable.GradientDrawable();
        bg.setColor(0xFFE85D9C);
        bg.setCornerRadius(dp(16));
        btn.setBackgroundDrawable(bg);
        press(btn);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                host.setPageView(new AiCapabilityPage(host).build());
            }
        });
        return btn;
    }

    private View footNote() {
        TextView t = text("绿色=真实可用 · 红色=不可用（附原因）", 11, 0xFFA9B7C9, false, 0);
        t.setGravity(Gravity.CENTER);
        return t;
    }
}
