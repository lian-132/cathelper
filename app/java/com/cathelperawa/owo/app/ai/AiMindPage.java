package com.cathelperawa.owo.app.ai;

import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.cathelperawa.owo.app.ui.CatSheet;
import com.cathelperawa.owo.app.ui.Page;
import com.cathelperawa.owo.app.ui.SwitchBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class AiMindPage extends Page {

    private static final int C_MAIN = 0xFF3C4B60;
    private static final int C_SUB = 0xFF8A9BB0;
    private static final int C_PINK = 0xFFE85D9C;

    private LinearLayout body;
    private LinearLayout content;

    public AiMindPage(Host host) {
        super(host);
    }

    @Override
    public View build() {
        View[] out = new View[2];
        body = pageRoot("意识", "心情 · 对方状态 · 主动开口", out, true, 13, "bg_ai", 0x66);
        content = new LinearLayout(ctx);
        content.setOrientation(LinearLayout.VERTICAL);
        body.addView(content, matchWrap(0));
        render();
        return out[0];
    }

    private void render() {
        content.removeAllViews();

        boolean on = AiConfig.moodOn();

        LinearLayout mood = whiteCard(dp(20), 0xF2FFFFFF);
        mood.setOrientation(LinearLayout.VERTICAL);
        mood.setPadding(dp(18), dp(14), dp(18), dp(16));
        mood.addView(sectionTitle("现在的心情"), matchWrap(0));

        if (!on) {
            mood.addView(text("情绪系统已关闭 · 用同一副语气说话", 11.5f, C_SUB, false, dp(6)),
                    matchWrap(0));
        } else {
            int p = AiMood.pleasurePct();
            int a = AiMood.arousalPct();
            mood.addView(bar("愉悦度", p, 0xFF69B7A6), matchWrap(dp(6)));
            mood.addView(bar("唤醒度", a, 0xFF7FA8D9), matchWrap(dp(6)));
            TextView desc = text(AiMood.describe(), 12, C_MAIN, false, 0);
            desc.setPadding(0, dp(8), 0, dp(2));
            mood.addView(desc, matchWrap(0));
            mood.addView(note("这两条会随着聊天慢慢漂，每轮自然回落 5%"), matchWrap(0));

            TextView reset = pill("重置成中性", 0xFFE4ECF7);
            reset.setTextColor(0xFF5A6B80);
            press(reset);
            reset.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    AiMood.reset();
                    toast("已重置");
                    render();
                }
            });
            LinearLayout wrap = new LinearLayout(ctx);
            wrap.setOrientation(LinearLayout.HORIZONTAL);
            wrap.setPadding(0, dp(10), 0, 0);
            wrap.addView(reset, wrapWrap());
            mood.addView(wrap, matchWrap(0));
        }
        content.addView(mood, matchWrap(dp(14)));

        content.addView(personalityCard(), matchWrap(dp(14)));

        content.addView(othersCard(), matchWrap(dp(14)));

        content.addView(proactiveCard(), matchWrap(dp(14)));

        LinearLayout sw = whiteCard(dp(20), 0xF2FFFFFF);
        sw.setOrientation(LinearLayout.VERTICAL);
        sw.setPadding(dp(16), dp(8), dp(16), dp(14));
        sw.addView(sectionTitle("开关"), matchWrap(0));
        sw.addView(switchRow("情绪系统", "关掉之后心情恒为中性，语气始终一致",
                "ai_mood", AiConfig.moodOn()), matchWrap(0));
        sw.addView(switchRow("五维人格", "关掉之后不再往提示词里写性格倾向",
                "ai_personality", AiConfig.personalityOn()), matchWrap(0));
        content.addView(sw, matchWrap(dp(14)));

        View foot = note("心情只影响「注入给模型的一句话」，不会改变人设本身");
        foot.setPadding(dp(8), dp(10), dp(8), dp(14));
        content.addView(foot, matchWrap(0));
    }

    private View othersCard() {
        LinearLayout card = whiteCard(dp(20), 0xF2FFFFFF);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18), dp(14), dp(18), dp(16));
        card.addView(sectionTitle("对方的状态"), matchWrap(0));

        Map<String, Double> all = AiMood.userMoodAll();
        if (all.isEmpty()) {
            card.addView(text("还没有数据 · 等别人说几句带情绪的话再看", 11.5f, C_SUB, false, dp(4)),
                    matchWrap(0));
            return card;
        }

        List<Map.Entry<String, Double>> list = new ArrayList<Map.Entry<String, Double>>(all.entrySet());
        java.util.Collections.sort(list, new java.util.Comparator<Map.Entry<String, Double>>() {
            @Override public int compare(Map.Entry<String, Double> a, Map.Entry<String, Double> b) {
                return Double.compare(a.getValue(), b.getValue());   
            }
        });

        int shown = 0;
        for (int i = 0; i < list.size() && shown < 12; i++) {
            String uid = list.get(i).getKey();
            double v = list.get(i).getValue();
            if (Math.abs(v) < 0.1) continue;                 
            shown++;
            String name = uid;
            try {
                String uin = com.cathelperawa.owo.app.api.IdentityApi.uinFromUid(uid);
                if (uin != null && uin.length() > 0) name = uin;
            } catch (Throwable ignore) {
            }
            LinearLayout row = new LinearLayout(ctx);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, dp(6), 0, dp(6));
            TextView nm = text(name, 13, C_MAIN, false, 0);
            row.addView(nm, new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            String label = v <= -0.4 ? "低落" : (v <= -0.15 ? "偏下" : (v >= 0.4 ? "不错" : "正常"));
            int color = v <= -0.4 ? 0xFFD9534F : (v <= -0.15 ? 0xFFD98E04
                    : (v >= 0.4 ? 0xFF2E9E5B : C_SUB));
            TextView tag = pill(label + " " + String.format(java.util.Locale.CHINA, "%+.2f", v),
                    v <= -0.4 ? 0x1FD9534F : (v >= 0.4 ? 0x1F2E9E5B : 0x1F8A9BB0));
            tag.setTextColor(color);
            row.addView(tag, wrapWrap());
            card.addView(row, matchWrap(0));
        }
        if (shown == 0) {
            card.addView(text("几个人都挺平静的", 11.5f, C_SUB, false, dp(4)), matchWrap(0));
        }
        card.addView(note("越新说的话权重越高；至少两句话才判断"), matchWrap(dp(6)));
        return card;
    }

    private View proactiveCard() {
        LinearLayout card = whiteCard(dp(20), 0xF2FFFFFF);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18), dp(14), dp(18), dp(16));
        card.addView(sectionTitle("主动开口"), matchWrap(0));

        boolean on = AiConfig.proactiveOn();
        TextView state = text(on ? "已开启" : "已关闭",
                15, on ? 0xFF2E9E5B : C_SUB, true, dp(2));
        card.addView(state, matchWrap(0));

        card.addView(readRow("触发上限", AiConfig.proactivePct() + "%"), matchWrap(dp(2)));
        card.addView(readRow("最短间隔", AiConfig.proactiveGapSec() + " 秒"), matchWrap(dp(2)));
        card.addView(readRow("今日已主动", AiEngine.proactiveTodayTotal() + " 次"), matchWrap(dp(2)));

        card.addView(note("紧迫感 = 社交欲望 + 精力（白天高 / 深夜低）+ 沉默时长 + 亲密度，再按今日次数打折"),
                matchWrap(dp(8)));

        TextView go = text("改参数 → AI 设置 · 主动对话", 11.5f, C_PINK, true, 0);
        go.setPadding(0, dp(10), 0, 0);
        press(go);
        go.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                toast("去「AI → 设置 → 主动对话」改");
            }
        });
        card.addView(go, matchWrap(0));
        return card;
    }

    private View readRow(String label, String value) {
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(4), 0, dp(4));
        row.addView(text(label, 12.5f, C_SUB, false, 0),
                new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(text(value, 12.5f, C_MAIN, true, 0), wrapWrap());
        return row;
    }

    private View sectionTitle(String s) {
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(10), 0, dp(8));
        row.addView(text(s, 13, C_MAIN, true, 0), wrapWrap());
        return row;
    }

    private View bar(String label, int pct, int color) {
        LinearLayout box = new LinearLayout(ctx);
        box.setOrientation(LinearLayout.VERTICAL);
        box.addView(text(label + "  " + pct, 11.5f, C_SUB, false, dp(4)), matchWrap(0));

        LinearLayout track = new LinearLayout(ctx);
        track.setOrientation(LinearLayout.HORIZONTAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0x33A8B8C8);
        bg.setCornerRadius(dp(5));
        track.setBackground(bg);
        track.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(10)));

        View fill = new View(ctx);
        GradientDrawable fg = new GradientDrawable();
        fg.setColor(color);
        fg.setCornerRadius(dp(5));
        fill.setBackground(fg);
        float w = Math.max(0.02f, Math.min(1f, pct / 100f));
        track.addView(fill, new LinearLayout.LayoutParams(0, dp(10), w));
        View rest = new View(ctx);
        track.addView(rest, new LinearLayout.LayoutParams(0, dp(10), 1f - w));

        box.addView(track, matchWrap(0));
        return box;
    }

    private View switchRow(String title, String sub, final String key, boolean on) {
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(6), 0, dp(6));
        LinearLayout txt = new LinearLayout(ctx);
        txt.setOrientation(LinearLayout.VERTICAL);
        txt.addView(text(title, 14, C_MAIN, true, dp(2)), matchWrap(0));
        txt.addView(text(sub, 10.5f, C_SUB, false, 0), matchWrap(0));
        row.addView(txt, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        final SwitchBox box = new SwitchBox(ctx, on);
        View.OnClickListener l = new View.OnClickListener() {
            @Override public void onClick(View v) {
                boolean z = !AiConfig.getBool(key);
                AiConfig.setBool(key, z);
                box.setChecked(z);
                render();
            }
        };
        box.setOnClickListener(l);
        row.setOnClickListener(l);
        row.addView(box, new LinearLayout.LayoutParams(dp(52), dp(30)));
        return row;
    }

    private View paramRow(String title, String cur, String sub, final String key,
                          final int dft, final int min, final int max, final String unit) {
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(8), 0, dp(8));
        LinearLayout txt = new LinearLayout(ctx);
        txt.setOrientation(LinearLayout.VERTICAL);
        txt.addView(text(title, 13.5f, C_MAIN, true, dp(2)), matchWrap(0));
        txt.addView(text(sub, 10.5f, C_SUB, false, 0), matchWrap(0));
        row.addView(txt, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        TextView val = pill(cur, 0xFFE4ECF7);
        val.setTextColor(0xFF5A6B80);
        row.addView(val, wrapWrap());
        press(row);
        row.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                CatSheet.input(ctx, title, min + " ~ " + max,
                        String.valueOf(AiConfig.getInt(key) <= 0 ? dft : AiConfig.getInt(key)),
                        new CatSheet.OnText() {
                            @Override public void run(String s) {
                                int x;
                                try {
                                    x = Integer.parseInt(s.trim());
                                } catch (Throwable t) {
                                    toast("要填数字");
                                    return;
                                }
                                if (x < min) x = min;
                                if (x > max) x = max;
                                AiConfig.setInt(key, x);
                                render();
                            }
                        });
            }
        });
        return row;
    }

    private View note(String s) {
        return text(s, 10.5f, C_SUB, false, 0);
    }

    private View personalityCard() {
        LinearLayout card = whiteCard(dp(20), 0xF2FFFFFF);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18), dp(14), dp(18), dp(16));
        card.addView(sectionTitle("性格"), matchWrap(0));

        boolean on = AiConfig.personalityOn();
        if (!on) {
            card.addView(text("人格系统已关闭 · 不往提示词里写性格", 11.5f, C_SUB, false, dp(4)),
                    matchWrap(0));
        } else {
            card.addView(text(AiPersonality.describe(), 12.5f, C_MAIN, false, dp(6)),
                    matchWrap(0));
            TextView pct = text(AiPersonality.panelText(), 11.5f, C_SUB, false, 0);
            pct.setLineSpacing(dp(4), 1f);
            card.addView(pct, matchWrap(dp(4)));
            
            card.addView(text(AiPersonality.impactText(), 11.5f, C_SUB, false, dp(4)),
                    matchWrap(dp(6)));
            card.addView(note("会被对话慢慢影响，每次微调不超过 2%，范围锁在 20%~90%"),
                    matchWrap(dp(4)));

            TextView reset = pill("重置成默认", 0xFFE4ECF7);
            reset.setTextColor(0xFF5A6B80);
            press(reset);
            reset.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    AiPersonality.reset();
                    toast("已重置");
                    render();
                }
            });
            LinearLayout wrap = new LinearLayout(ctx);
            wrap.setOrientation(LinearLayout.HORIZONTAL);
            wrap.setPadding(0, dp(10), 0, 0);
            wrap.addView(reset, wrapWrap());
            card.addView(wrap, matchWrap(0));
        }
        return card;
    }
}
