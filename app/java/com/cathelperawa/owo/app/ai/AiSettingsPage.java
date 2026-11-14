package com.cathelperawa.owo.app.ai;

import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.cathelperawa.owo.app.ui.CatSheet;
import com.cathelperawa.owo.app.ui.Page;
import com.cathelperawa.owo.app.ui.SwitchBox;

import java.util.List;

public final class AiSettingsPage extends Page {

    private static final int C_MAIN = 0xFF3C4B60;
    private static final int C_SUB = 0xFF8A9BB0;
    private static final int C_PINK = 0xFFE85D9C;

    private LinearLayout body;
    private LinearLayout content;

    public AiSettingsPage(Host host) {
        super(host);
    }

    @Override
    public View build() {
        View[] out = new View[2];
        body = pageRoot("设置", "功能开关 · 模型选择", out, true, 13, "bg_ai", 0x66);
        content = new LinearLayout(ctx);
        content.setOrientation(LinearLayout.VERTICAL);
        body.addView(content, matchWrap(0));
        render();
        return out[0];
    }

    private void render() {
        content.removeAllViews();

        LinearLayout sw = whiteCard(dp(20), 0xF2FFFFFF);
        sw.setOrientation(LinearLayout.VERTICAL);
        sw.setPadding(dp(16), dp(8), dp(16), dp(14));

        sw.addView(sectionTitle("功能开关"), matchWrap(0));
        sw.addView(switchRow("识图", "收到图片时把原图发给模型看（关掉只发文件名）",
                "ai_vision", true), matchWrap(dp(4)));
        sw.addView(visionModelRow(), matchWrap(dp(4)));
        sw.addView(switchRow("语音转文字（本地）", "读 TIM 自己转好的文字，不联网也不发音频给模型",
                "ai_ptt_text", true), matchWrap(dp(4)));
        sw.addView(switchRow("本地语音识别", "TIM 没转文字时，自己解码 PCM 交给 TIM 内置 ASR 识别",
                "ai_ptt_asr", false), matchWrap(dp(4)));
        sw.addView(switchRow("语音发服务器", "把语音解码成 MP3 发给模型（模型得支持音频输入）",
                "ai_voice", false), matchWrap(dp(4)));
        sw.addView(switchRow("语音合成命令", "发「语音 你好」→ 合成为语音消息（免费 TTS，不用填 key）",
                "ai_tts_cmd", false), matchWrap(dp(4)));
        sw.addView(switchRow("图片记忆（存文件）", "认过的图写进文件，重启 TIM 后同一张图不用重新识别（省钱，占一点存储）",
                "ai_img_cache", false), matchWrap(dp(4)));
        sw.addView(switchRow("记忆相串", "私聊时记得对方在群里说过的话",
                "ai_cross", false), matchWrap(dp(4)));
        sw.addView(switchRow("好感度", "按人记亲疏，越熟说话越随意（档案在首页「好感度」）",
                "ai_affinity", false), matchWrap(dp(4)));
        sw.addView(switchRow("联网搜索", "需要查东西时自己搜（接口在「模型提供商」页填）",
                "ai_search_on", false), matchWrap(dp(4)));

        sw.addView(switchRow("解析与发歌", "让它自己搜歌发出、解析视频链接（格式它自己判断）",
                "ai_parse_on", false), matchWrap(dp(4)));
        sw.addView(cookieRow(), matchWrap(dp(4)));
        sw.addView(switchRow("引用回复", "回复时带上对方那句话（群聊里一眼看出在回谁）",
                "ai_quote", false), matchWrap(dp(4)));
        sw.addView(switchRow("AI 自己判断接不接", "群里没 @ 它时，让模型看上下文决定要不要接话",
                "ai_selfjudge", false), matchWrap(dp(4)));
        sw.addView(switchRow("连续对话（硬规则）", "关掉 AI 判断后的兜底：同一人 120 秒内接着说也算",
                "ai_followup", false), matchWrap(0));
        content.addView(sw, matchWrap(dp(14)));

        content.addView(scriptCard(), matchWrap(dp(14)));

        content.addView(ttsCard(), matchWrap(dp(14)));

        content.addView(triggerCard(), matchWrap(dp(14)));

        content.addView(rhythmCard(), matchWrap(dp(14)));

        content.addView(emojiCard(), matchWrap(dp(14)));

        content.addView(proactiveCard(), matchWrap(dp(14)));

        List<AiProvider> list = AiProvider.loadAll();
        String curId = AiProvider.currentId();

        LinearLayout mc = whiteCard(dp(20), 0xF2FFFFFF);
        mc.setOrientation(LinearLayout.VERTICAL);
        mc.setPadding(dp(16), dp(8), dp(16), dp(14));
        mc.addView(sectionTitle("模型"), matchWrap(0));

        if (list.isEmpty()) {
            TextView t = text("还没有配置任何模型，先去「模型提供商」加一个", 11.5f, C_SUB, false, 0);
            t.setPadding(0, dp(4), 0, dp(6));
            mc.addView(t, matchWrap(0));
        } else {
            for (int i = 0; i < list.size(); i++) {
                AiProvider p = list.get(i);
                mc.addView(modelRow(p, p.id != null && p.id.equals(curId)),
                        matchWrap(i == list.size() - 1 ? 0 : dp(6)));
            }
        }
        content.addView(mc, matchWrap(dp(14)));

        TextView note = text("点一行即切换；想改 Key / 地址 / 模型名，去「模型提供商」",
                10.5f, C_SUB, false, 0);
        note.setPadding(dp(6), dp(2), dp(6), dp(10));
        content.addView(note, matchWrap(0));

        content.addView(usageCard(), matchWrap(dp(14)));
    }

    private View ttsCard() {
        LinearLayout c = whiteCard(dp(20), 0xF2FFFFFF);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(16), dp(8), dp(16), dp(14));
        c.addView(sectionTitle("语音合成（TTS）"), matchWrap(0));

        boolean useApi = AiConfig.ttsUseApi();
        TextView now = text(useApi
                        ? "当前：" + AiConfig.ttsModel() + " · 音色 " + AiConfig.ttsVoice()
                        : "当前：内置免费通道（单音色，无需配置）",
                11.5f, useApi ? 0xFF2E9E5B : C_SUB, useApi, dp(4));
        c.addView(now, matchWrap(0));

        c.addView(ttsField("接口地址", "ai_tts_url", AiConfig.ttsBaseUrl(),
                "https://api.openai.com/v1（兼容 /audio/speech 即可）"), matchWrap(dp(4)));
        c.addView(ttsField("密钥", "ai_tts_key", AiConfig.ttsKey(),
                "sk-...（留空 = 用内置免费通道）"), matchWrap(dp(4)));
        c.addView(ttsField("模型", "ai_tts_model", AiConfig.ttsModel(),
                "tts-1"), matchWrap(dp(4)));
        c.addView(ttsField("音色", "ai_tts_voice", AiConfig.ttsVoice(),
                "alloy / nova / shimmer / 各家自己的名字"), matchWrap(dp(4)));

        TextView hint = text("填了密钥就走自己的接口（可选音色）；失败会自动回落到免费通道。\n"
                        + "改完立刻生效，不用重启。",
                10.5f, C_SUB, false, 0);
        hint.setPadding(0, dp(8), 0, 0);
        c.addView(hint, matchWrap(0));
        return c;
    }

    private View ttsField(String label, final String key, String cur, String hint) {
        LinearLayout box = new LinearLayout(ctx);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(0, dp(4), 0, dp(2));
        box.addView(text(label, 12, C_MAIN, true, dp(2)), matchWrap(0));

        final android.widget.EditText et = new android.widget.EditText(ctx);
        et.setText(cur == null ? "" : cur);
        et.setHint(hint);
        et.setTextSize(12.5f);
        et.setTextColor(C_MAIN);
        et.setSingleLine(true);
        et.setHintTextColor(0xFFB6C1D0);
        et.setPadding(dp(10), dp(8), dp(10), dp(8));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0x66F0F4F8);
        bg.setCornerRadius(dp(8));
        et.setBackground(bg);
        et.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(android.text.Editable e) {
                AiConfig.setString(key, e.toString());
            }
        });
        box.addView(et, matchWrap(0));
        return box;
    }

    private static final String[] BAR = {"·", "▁", "▂", "▃", "▄", "▅", "▆", "▇"};

    private View usageCard() {
        LinearLayout c = whiteCard(dp(20), 0xF2FFFFFF);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(16), dp(8), dp(16), dp(14));
        c.addView(sectionTitle("用量与缓存"), matchWrap(0));

        AiUsage.Day d = AiUsage.day(AiUsage.today());
        long total = d.prompt + d.completion;
        c.addView(text("今日 " + d.calls + " 次调用 · " + tok(total) + " tokens",
                14.5f, C_MAIN, true, dp(2)), matchWrap(0));
        c.addView(text("进 " + tok(d.prompt) + " · 出 " + tok(d.completion)
                        + " · 累计 " + tok(AiUsage.totalTokens()),
                11f, C_SUB, false, 0), matchWrap(0));

        double h = d.hitRate();
        TextView hitTv;
        if (h < 0) {
            hitTv = text("缓存：这家模型不返回缓存统计", 11.5f, C_SUB, false, 0);
        } else {
            int color = h >= 60 ? 0xFF2E9E5B : (h >= 30 ? 0xFFD98E04 : 0xFFD9534F);
            hitTv = text("缓存命中 " + String.format(java.util.Locale.CHINA, "%.0f%%", h)
                            + "（命中 " + tok(d.hit) + " / 未命中 " + tok(d.miss) + "）",
                    11.5f, color, true, 0);
        }
        hitTv.setPadding(0, dp(6), 0, dp(6));
        c.addView(hitTv, matchWrap(0));

        java.util.List<String[]> srcs = AiUsage.todaySources();
        if (!srcs.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            int limit = Math.min(4, srcs.size());
            for (int i = 0; i < limit; i++) {
                if (i > 0) sb.append(" · ");
                sb.append(srcLabel(srcs.get(i)[0])).append(" ").append(srcs.get(i)[1]);
            }
            TextView src = text("来源  " + sb, 11f, 0xFF7A8CA3, false, 0);
            src.setPadding(0, 0, 0, dp(4));
            c.addView(src, matchWrap(0));
        }

        AiUsage.Day[] days = AiUsage.recentDays(7);
        long max = 1;
        for (int i = 0; i < days.length; i++) {
            long v = days[i].prompt + days[i].completion;
            if (v > max) max = v;
        }
        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < days.length; i++) {
            long v = days[i].prompt + days[i].completion;
            bar.append(v <= 0 ? BAR[0] : BAR[(int) Math.min(7, v * 7L / max + 1)]);
        }
        c.addView(text("近 7 天  " + bar.toString(), 16f, 0xFF7A8CA3, false, 0), matchWrap(0));

        TextView hint = text("只统计本机、加密落盘 · 点这里清零", 10.5f, C_SUB, false, 0);
        hint.setPadding(0, dp(6), 0, 0);
        hint.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                AiUsage.reset();
                render();
            }
        });
        c.addView(hint, matchWrap(0));
        return c;
    }

    private static String tok(long n) {
        if (n >= 1000000) return String.format(java.util.Locale.CHINA, "%.1fM", n / 1000000d);
        if (n >= 1000) return String.format(java.util.Locale.CHINA, "%.1fk", n / 1000d);
        return String.valueOf(n);
    }

    private static String srcLabel(String key) {
        if ("reply".equals(key)) return "回复";
        if ("judge".equals(key)) return "判断接不接";
        if ("vision".equals(key)) return "看图";
        if ("tool".equals(key)) return "工具";
        if ("affinity".equals(key)) return "好感度";
        if ("proactive".equals(key)) return "主动";
        if ("search".equals(key)) return "搜索";
        if ("tts".equals(key)) return "语音";
        return key;
    }

    private View scriptCard() {
        LinearLayout card = whiteCard(dp(20), 0xF2FFFFFF);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(8), dp(16), dp(12));
        card.addView(sectionTitle("脚本"), matchWrap(0));

        final LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(4), 0, dp(4));

        LinearLayout txt = new LinearLayout(ctx);
        txt.setOrientation(LinearLayout.VERTICAL);
        txt.addView(text("允许 AI 写脚本", 14.5f, C_MAIN, true, dp(2)), matchWrap(0));
        txt.addView(text("它自己写、自己试跑、自己启用。需要脚本页的「允许 AI 调用脚本」也开着",
                11, C_SUB, false, 0), matchWrap(0));
        row.addView(txt, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        final SwitchBox box = new SwitchBox(ctx, AiConfig.getBool("ai_script"));
        row.addView(box, new LinearLayout.LayoutParams(dp(52), dp(30)));

        View.OnClickListener toggle = new View.OnClickListener() {
            @Override public void onClick(View v) {

                if (!AiEngine.scriptToolsOn()) {
                    toast("先去脚本页打开「允许 AI 调用脚本」这个总开关");
                    return;
                }
                boolean z = !AiConfig.getBool("ai_script");
                AiConfig.setBool("ai_script", z);
                box.setChecked(z);
                press(v);
                toast(z ? "AI 可以写脚本了" : "已关掉 AI 写脚本");
            }
        };
        box.setOnClickListener(toggle);
        row.setOnClickListener(toggle);
        card.addView(row, matchWrap(0));

        int n = 0, routes = 0, tools = 0;
        try {
            n = com.cathelperawa.owo.app.script.ScriptHost.list().size();
            routes = com.cathelperawa.owo.app.script.RouteRegistry.size();
            tools = com.cathelperawa.owo.app.script.ScriptToolRegistry.size();
        } catch (Throwable ignore) {
        }

        LinearLayout entry = new LinearLayout(ctx);
        entry.setOrientation(LinearLayout.HORIZONTAL);
        entry.setGravity(Gravity.CENTER_VERTICAL);
        entry.setPadding(0, dp(10), 0, dp(2));

        LinearLayout etxt = new LinearLayout(ctx);
        etxt.setOrientation(LinearLayout.VERTICAL);
        etxt.addView(text("脚本系统", 14.5f, C_MAIN, true, dp(2)), matchWrap(0));
        etxt.addView(text(n + " 个脚本 · " + routes + " 条命令 · " + tools + " 个 AI 工具",
                11, C_SUB, false, 0), matchWrap(0));
        entry.addView(etxt, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        entry.addView(text("›", 20, 0xFFA8B4C4, true, 0), wrapWrap());

        entry.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                press(v);
                openScriptPage();
            }
        });
        card.addView(entry, matchWrap(0));
        return card;
    }

    private void openScriptPage() {
        try {
            android.content.Intent it = new android.content.Intent();
            it.setClassName(ctx.getPackageName(),
                    "com.cathelperawa.owo.app.ConsoleActivity");
            it.putExtra("cat_page", "script");
            ctx.startActivity(it);
        } catch (Throwable t) {
            toast("打不开脚本页：" + t);
        }
    }

    private View sectionTitle(String s) {
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(10), 0, dp(8));
        TextView t = text(s, 13, C_MAIN, true, 0);
        row.addView(t, wrapWrap());
        return row;
    }

    private View switchRow(String title, String sub, final String key, boolean first) {
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        if (!first) row.setPadding(0, dp(4), 0, dp(4));

        LinearLayout txt = new LinearLayout(ctx);
        txt.setOrientation(LinearLayout.VERTICAL);
        txt.addView(text(title, 14.5f, C_MAIN, true, dp(2)), matchWrap(0));
        txt.addView(text(sub, 11, C_SUB, false, 0), matchWrap(0));
        row.addView(txt, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        final SwitchBox box = new SwitchBox(ctx, AiConfig.getBool(key));
        row.addView(box, new LinearLayout.LayoutParams(dp(52), dp(30)));

        View.OnClickListener l = new View.OnClickListener() {
            @Override public void onClick(View v) {
                boolean z = !AiConfig.getBool(key);
                AiConfig.setBool(key, z);
                box.setChecked(z);
                press(v);
            }
        };
        box.setOnClickListener(l);
        row.setOnClickListener(l);
        return row;
    }

    private View cookieRow() {
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(6), 0, dp(6));

        LinearLayout txt = new LinearLayout(ctx);
        txt.setOrientation(LinearLayout.VERTICAL);
        txt.addView(text("音乐 Cookie（可选）", 14.5f, C_MAIN, true, dp(2)), matchWrap(0));

        String ck = "";
        try {
            ck = com.cathelperawa.owo.app.feature.MusicApi.musicCookie();
        } catch (Throwable ignore) {
        }
        String sub = (ck == null || ck.length() == 0)
                ? "没填 · 只能放免费歌 + 30 秒试听"
                : ("已填 " + ck.length() + " 字符 · 点一下可改");
        txt.addView(text(sub, 11, (ck == null || ck.length() == 0) ? C_SUB : 0xFF3FA96A,
                false, 0), matchWrap(0));
        row.addView(txt, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(text("›", 18, 0xFFB6C1D0, false, 0));

        press(row);
        row.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                String cur = com.cathelperawa.owo.app.feature.MusicApi.musicCookie();
                CatSheet.input(ctx, "网易云 Cookie",
                        "填 MUSIC_U=xxxx 这一串（浏览器登录后从 cookie 里复制整行也行）",
                        cur, new CatSheet.OnText() {
                            @Override public void run(String s) {
                                com.cathelperawa.owo.app.core.ConfigStore
                                        .setString("music_cookie", s == null ? "" : s.trim());
                                toast((s == null || s.trim().length() == 0) ? "已清空" : "已保存");
                                render();
                            }
                        });
            }
        });
        return row;
    }

    private View visionModelRow() {
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(4), 0, dp(10));

        String cur = "跟随当前模型";
        try {
            String id = AiConfig.visionProviderId();
            if (id != null && id.length() > 0) {
                AiProvider v = AiProvider.find(id);
                if (v != null) cur = v.displayName() + " · " + v.model;
            }
        } catch (Throwable ignore) {
        }
        TextView t = text("识图模型  " + cur, 12, C_PINK, false, 0);
        row.addView(t, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(text("›", 18, 0xFFB6C1D0, false, 0));

        press(row);
        row.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { pickVisionModel(); }
        });
        return row;
    }

    private void pickVisionModel() {
        final List<AiProvider> list = AiProvider.loadAll();
        String[] items = new String[list.size() + 1];
        items[0] = "跟随当前模型";
        for (int i = 0; i < list.size(); i++) {
            AiProvider p = list.get(i);
            items[i + 1] = p.displayName() + " · "
                    + (p.model == null || p.model.length() == 0 ? "未填模型" : p.model);
        }
        CatSheet.pick(ctx, "识图用哪个模型", items, new CatSheet.OnPick() {
            @Override public void run(int i, String s) {
                if (i == 0) {
                    AiConfig.setVisionProviderId("");
                    toast("识图跟随当前模型");
                } else {
                    AiProvider p = list.get(i - 1);
                    AiConfig.setVisionProviderId(p.id);
                    toast("识图用 " + p.displayName());
                }
                render();
            }
        });
    }

    private View proactiveCard() {
        LinearLayout card = whiteCard(dp(20), 0xF2FFFFFF);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(8), dp(16), dp(14));

        card.addView(sectionTitle("主动对话"), matchWrap(0));
        card.addView(switchRow("主动插话", "没人 @ 它、也没什么要回的，也可能自己说一句",
                "ai_proactive", true), matchWrap(dp(4)));

        card.addView(numRow("插话概率", "每条新消息掷一次骰子，中了才会想说",
                "ai_proactive_pct", AiConfig.proactivePct(), "%"), matchWrap(0));
        card.addView(numRow("最短间隔", "同一个会话两次主动之间至少隔多久",
                "ai_proactive_gap", AiConfig.proactiveGapSec(), "秒"), matchWrap(0));

        TextView note = text("它要是没什么想说的，会自己回「不说」，不会硬凑一句",
                10.5f, C_SUB, false, 0);
        note.setPadding(0, dp(10), 0, 0);
        card.addView(note, matchWrap(0));
        return card;
    }

    private View numRow(String title, String hint, final String key, int def, String unit) {
        LinearLayout box = new LinearLayout(ctx);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(0, dp(10), 0, 0);

        box.addView(text(title, 14.5f, C_MAIN, true, dp(2)), matchWrap(0));
        box.addView(text(hint, 11, C_SUB, false, 0), matchWrap(dp(8)));

        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        final android.widget.EditText et = new android.widget.EditText(ctx);
        et.setText(String.valueOf(def));
        et.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        et.setTextSize(14);
        et.setTextColor(0xFF5A6B80);
        et.setGravity(Gravity.CENTER);
        et.setSingleLine(true);
        GradientDrawable eg = new GradientDrawable();
        eg.setCornerRadius(dp(12));
        eg.setColor(0x59FFFFFF);
        eg.setStroke(Math.max(1, dp(1)), 0x33A9BEDC);
        et.setBackgroundDrawable(eg);
        et.setPadding(dp(10), dp(9), dp(10), dp(9));
        row.addView(et, new LinearLayout.LayoutParams(dp(86), dp(42)));

        TextView u = text(unit, 13, C_MAIN, true, 0);
        LinearLayout.LayoutParams ulp = wrapWrap();
        ulp.leftMargin = dp(8);
        row.addView(u, ulp);
        box.addView(row, matchWrap(0));

        et.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) return;
                String t = et.getText() == null ? "" : et.getText().toString().trim();
                int val = def;
                try { val = Integer.parseInt(t); } catch (Throwable ignore) {}
                if (val < 0) val = 0;
                AiConfig.setInt(key, val);
                et.setText(String.valueOf(val));
                toast("已保存");
            }
        });
        return box;
    }

    private View emojiCard() {
        LinearLayout card = whiteCard(dp(20), 0xF2FFFFFF);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(8), dp(16), dp(14));

        card.addView(sectionTitle("表情包"), matchWrap(0));
        card.addView(switchRow("发表情包", "聊天时自己挑一张发出去",
                "ai_emoji", true), matchWrap(dp(4)));

        LinearLayout box = new LinearLayout(ctx);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(0, dp(10), 0, 0);
        box.addView(text("发送概率", 14.5f, C_MAIN, true, dp(2)), matchWrap(0));
        box.addView(text("它想发表情时，按这个概率才真的发出去（0~100）",
                11, C_SUB, false, 0), matchWrap(dp(8)));

        LinearLayout pr = new LinearLayout(ctx);
        pr.setOrientation(LinearLayout.HORIZONTAL);
        pr.setGravity(Gravity.CENTER_VERTICAL);

        final android.widget.EditText et = new android.widget.EditText(ctx);
        et.setText(String.valueOf(AiConfig.emojiPct()));
        et.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        et.setTextSize(14);
        et.setTextColor(0xFF5A6B80);
        et.setGravity(Gravity.CENTER);
        et.setSingleLine(true);
        GradientDrawable eg = new GradientDrawable();
        eg.setCornerRadius(dp(12));
        eg.setColor(0x59FFFFFF);
        eg.setStroke(Math.max(1, dp(1)), 0x33A9BEDC);
        et.setBackgroundDrawable(eg);
        et.setPadding(dp(10), dp(9), dp(10), dp(9));
        pr.addView(et, new LinearLayout.LayoutParams(dp(76), dp(42)));

        TextView pctLabel = text("%", 14, C_MAIN, true, 0);
        LinearLayout.LayoutParams plp = wrapWrap();
        plp.leftMargin = dp(8);
        pr.addView(pctLabel, plp);

        TextView hint = text("默认 30%", 10.5f, C_SUB, false, 0);
        LinearLayout.LayoutParams hlp = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        hlp.leftMargin = dp(10);
        pr.addView(hint, hlp);
        box.addView(pr, matchWrap(0));

        et.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) return;
                String t = et.getText() == null ? "" : et.getText().toString().trim();
                int v2 = 30;
                try { v2 = Integer.parseInt(t); } catch (Throwable ignore) {}
                if (v2 < 0) v2 = 0;
                if (v2 > 100) v2 = 100;
                AiConfig.setInt("ai_emoji_pct", v2);
                et.setText(String.valueOf(v2));
                toast("已保存 " + v2 + "%");
            }
        });
        card.addView(box, matchWrap(0));

        int n = 0;
        try { n = AiEmoji.count(); } catch (Throwable ignore) {}
        LinearLayout entry = new LinearLayout(ctx);
        entry.setOrientation(LinearLayout.HORIZONTAL);
        entry.setGravity(Gravity.CENTER_VERTICAL);
        entry.setPadding(0, dp(12), 0, 0);
        TextView t = text(n > 0 ? ("表情库 " + n + " 张 · 去管理") : "表情库是空的 · 去添加",
                12, C_PINK, false, 0);
        entry.addView(t, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        entry.addView(text("›", 18, 0xFFB6C1D0, false, 0));
        press(entry);
        entry.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                host.pushPage(new AiEmojiPage(host).build());
            }
        });
        card.addView(entry, matchWrap(0));
        return card;
    }

    private View modelRow(final AiProvider p, boolean selected) {
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(12), dp(11), dp(12), dp(11));

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(13));
        bg.setColor(selected ? 0x33FFC2DF : 0x40FFFFFF);
        bg.setStroke(Math.max(1, dp(1)), selected ? 0x99FF9FCB : 0x22A9BEDC);
        row.setBackgroundDrawable(bg);

        LinearLayout txt = new LinearLayout(ctx);
        txt.setOrientation(LinearLayout.VERTICAL);
        String model = p.model == null || p.model.length() == 0 ? "未填模型名" : p.model;
        txt.addView(text(model, 13.5f, C_MAIN, true, dp(2)), matchWrap(0));
        txt.addView(text(p.displayName() + (p.enabled ? "" : " · 已停用"),
                10.5f, C_SUB, false, 0), matchWrap(0));
        row.addView(txt, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        View dot = new View(ctx);
        GradientDrawable dg = new GradientDrawable();
        dg.setShape(GradientDrawable.OVAL);
        dg.setColor(selected ? C_PINK : 0x00000000);
        dg.setStroke(Math.max(1, dp(1.5f)), selected ? C_PINK : 0xFFC3CEDA);
        dot.setBackgroundDrawable(dg);
        row.addView(dot, new LinearLayout.LayoutParams(dp(16), dp(16)));

        press(row);
        row.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                AiProvider.setCurrent(p.id);
                toast("已切换到 " + (p.model == null || p.model.length() == 0
                        ? p.displayName() : p.model));
                render();
            }
        });
        return row;
    }

    private View triggerCard() {
        LinearLayout c = whiteCard(dp(20), 0xF2FFFFFF);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(16), dp(8), dp(16), dp(14));
        c.addView(sectionTitle("什么时候回"), matchWrap(0));

        c.addView(switchRow("私聊自动回", "好友发来的私聊直接回",
                "ai_private", AiConfig.privateReply()), matchWrap(dp(2)));
        c.addView(switchRow("群里 @ 我", "只有被 @ 才回（推荐）",
                "ai_at", AiConfig.atReply()), matchWrap(dp(2)));
        c.addView(switchRow("关键词触发", "文本里命中关键词就回",
                "ai_keyword", AiConfig.keywordReply()), matchWrap(dp(2)));

        c.addView(numField("关键词", "多个用逗号 / 空格分隔",
                "ai_keywords", AiConfig.keywordsRaw(), false), matchWrap(dp(6)));

        c.addView(text("哪些群 / 好友能跟它说话 → 首页「白名单」", 10.5f, C_SUB, false, dp(8)),
                matchWrap(0));
        return c;
    }

    private View rhythmCard() {
        LinearLayout c = whiteCard(dp(20), 0xF2FFFFFF);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(16), dp(8), dp(16), dp(14));
        c.addView(sectionTitle("回复节奏"), matchWrap(0));

        c.addView(numField("连发合并（毫秒）", "这段时间内连发的几条只回一次，0 = 不合并",
                "ai_merge_ms", String.valueOf(AiConfig.mergeMs()), true), matchWrap(dp(4)));
        c.addView(numField("冷却（秒）", "同一会话多久内只回一次",
                "ai_cooldown", String.valueOf(AiConfig.cooldownSec()), true), matchWrap(dp(4)));
        c.addView(numField("每小时上限", "每个会话每小时最多回几次，0 = 不限",
                "ai_hourly", String.valueOf(AiConfig.hourlyLimit()), true), matchWrap(dp(4)));
        c.addView(numField("每次带多少条上下文", "发多少条历史给模型（越多越贵，也越记得住）",
                "ai_history_limit", String.valueOf(AiConfig.historyLimit()), true), matchWrap(dp(4)));

        c.addView(switchRow("分段连发", "长回复切成几条短消息，间隔几秒发",
                "ai_segment", AiConfig.segmentEnabled()), matchWrap(dp(6)));
        c.addView(numField("最短间隔（秒）", "", "ai_seg_min",
                String.valueOf(AiConfig.segMin()), true), matchWrap(dp(4)));
        c.addView(numField("最长间隔（秒）", "", "ai_seg_max",
                String.valueOf(AiConfig.segMax()), true), matchWrap(dp(4)));
        return c;
    }

    private View numField(String title, String hint, final String key,
                          String value, final boolean num) {
        LinearLayout box = new LinearLayout(ctx);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(0, dp(6), 0, dp(2));
        box.addView(text(title, 12.5f, C_MAIN, true, dp(2)), matchWrap(0));
        if (hint != null && hint.length() > 0) {
            box.addView(text(hint, 10.5f, C_SUB, false, dp(3)), matchWrap(0));
        }
        EditText et = new EditText(ctx);
        et.setText(value == null ? "" : value);
        et.setTextSize(13);
        et.setTextColor(C_MAIN);
        et.setSingleLine(true);
        et.setHintTextColor(0xFFB6C1D0);
        et.setPadding(dp(12), dp(9), dp(12), dp(9));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0x66F0F4F8);
        bg.setCornerRadius(dp(10));
        et.setBackground(bg);
        if (num) et.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        et.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(android.text.Editable e) {
                String v = e.toString().trim();
                if (num) {
                    try {
                        AiConfig.setInt(key, Integer.parseInt(v.length() == 0 ? "0" : v));
                    } catch (Throwable ignore) {
                    }
                } else {
                    AiConfig.setString(key, v);
                }
            }
        });
        box.addView(et, matchWrap(0));
        return box;
    }
}
