package com.cathelperawa.owo.app.ui;

import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.cathelperawa.owo.app.core.ConfigOverride;
import com.cathelperawa.owo.app.core.HookLog;
import com.cathelperawa.owo.app.core.KernelWhitelist;
import com.cathelperawa.owo.app.core.ScriptSetting;
import com.cathelperawa.owo.app.core.Skin;
import com.cathelperawa.owo.app.script.MethodHook;
import com.cathelperawa.owo.app.script.PatchPoint;
import com.cathelperawa.owo.app.script.ScriptBusRegistry;
import com.cathelperawa.owo.app.script.ScriptHost;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public final class AuditPage extends Page {

    private static final int C_MAIN = 0xFF3C4B60;
    private static final int C_SUB = 0xFF8A9BB0;
    private static final int C_RED = 0xFFC0392B;
    private static final int C_ORANGE = 0xFFD9480F;

    private LinearLayout content;
    private View[] holder;

    public AuditPage(Host host) {
        super(host);
    }

    private static final class Stats {
        int enabledN, pointN, methodN, cfgN, screenN, entryN, svcN, kernelN;
        List<String> points = new ArrayList<String>();
        List<String> methods = new ArrayList<String>();
        String skinLine = "";
    }

    @Override public View build() {
        holder = new View[2];
        LinearLayout body = pageRoot("注入审计", "哪些脚本正在改模块", holder, true, 13,
                "bg_script", 0x55);
        content = new LinearLayout(ctx);
        content.setOrientation(LinearLayout.VERTICAL);
        body.addView(content, matchWrap(0));

        content.addView(text("正在收集注入信息…", 12, C_SUB, false, 0), matchWrap(dp(20)));

        final android.app.Activity act = activity();
        com.cathelperawa.owo.app.core.Bg.run(new Runnable() {
            @Override public void run() {
                final Stats s;
                try {
                    s = collect();
                } catch (Throwable t) {
                    return;
                }
                Runnable ui = new Runnable() {
                    @Override public void run() {
                        try {
                            render(s);
                        } catch (Throwable ignore) {
                        }
                    }
                };
                try {
                    if (act != null) act.runOnUiThread(ui);
                    else ui.run();
                } catch (Throwable ignore) {
                }
            }
        }, "cat-audit");
        return holder[0];
    }

    private static Stats collect() {
        Stats s = new Stats();
        try { s.enabledN = ScriptHost.enabledNames().size(); } catch (Throwable ignore) {}
        try { s.pointN = PatchPoint.count(); s.points = PatchPoint.points(); } catch (Throwable ignore) {}
        try { s.methodN = MethodHook.count(); s.methods = MethodHook.targets(); } catch (Throwable ignore) {}
        try { s.cfgN = ConfigOverride.size(); } catch (Throwable ignore) {}
        try { s.screenN = ScriptScreen.keys().size(); } catch (Throwable ignore) {}
        try { s.entryN = ScriptSetting.list().size(); } catch (Throwable ignore) {}
        try { s.svcN = ScriptBusRegistry.list().size(); } catch (Throwable ignore) {}
        try { s.kernelN = KernelWhitelist.list().size(); } catch (Throwable ignore) {}
        try {
            JSONObject skin = Skin.dump();
            JSONObject colors = skin.optJSONObject("colors");
            JSONObject icons = skin.optJSONObject("icons");
            JSONObject bgs = skin.optJSONObject("bgs");
            JSONObject texts = skin.optJSONObject("texts");
            JSONArray hidden = skin.optJSONArray("hidden");
            int c = colors == null ? 0 : colors.length();
            int ic = icons == null ? 0 : icons.length();
            int bg = bgs == null ? 0 : bgs.length();
            int tx = texts == null ? 0 : texts.length();
            int hd = hidden == null ? 0 : hidden.length();
            if (c + ic + bg + tx + hd > 0) {
                s.skinLine = "配色 " + c + " · 图标 " + ic + " · 底图 " + bg
                        + " · 文案 " + tx + " · 隐藏入口 " + hd;
            }
        } catch (Throwable ignore) {
        }
        return s;
    }

    private void render(Stats s) {
        content.removeAllViews();

        LinearLayout sum = whiteCard(dp(18), 0xF2FFFFFF);
        sum.setOrientation(LinearLayout.VERTICAL);
        sum.setPadding(dp(16), dp(14), dp(16), dp(14));
        sum.addView(text("当前生效", 15, C_MAIN, true, dp(8)), matchWrap(0));
        sum.addView(line("已启用脚本", String.valueOf(s.enabledN), C_MAIN), matchWrap(dp(4)));
        sum.addView(line("点位挂载", s.pointN + " 个 handler",
                s.pointN > 0 ? C_ORANGE : C_SUB), matchWrap(dp(4)));
        sum.addView(line("方法重写", s.methodN + " 处",
                s.methodN > 0 ? C_RED : C_SUB), matchWrap(dp(4)));
        sum.addView(line("开关覆盖", s.cfgN + " 项",
                s.cfgN > 0 ? C_ORANGE : C_SUB), matchWrap(dp(4)));
        sum.addView(line("页面接管", s.screenN + " 页",
                s.screenN > 0 ? C_ORANGE : C_SUB), matchWrap(dp(4)));
        sum.addView(line("设置页入口", s.entryN + " 行",
                s.entryN > 0 ? C_ORANGE : C_SUB), matchWrap(dp(4)));
        sum.addView(line("对外服务", s.svcN + " 个", C_SUB), matchWrap(dp(4)));
        sum.addView(line("内核授权脚本", s.kernelN + " 个",
                s.kernelN > 0 ? C_ORANGE : C_SUB), matchWrap(0));
        content.addView(sum, matchWrap(dp(12)));

        if (!s.points.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < s.points.size(); i++) {
                if (i > 0) sb.append("\n");
                sb.append("· ").append(s.points.get(i));
            }
            content.addView(block("点位", sb.toString(), C_ORANGE), matchWrap(dp(12)));
        }
        if (!s.methods.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < s.methods.size(); i++) {
                if (i > 0) sb.append("\n");
                sb.append("· ").append(s.methods.get(i));
            }
            content.addView(block("方法重写", sb.toString(), C_RED), matchWrap(dp(12)));
        }
        if (s.skinLine.length() > 0) {
            content.addView(block("外观改动", s.skinLine, C_ORANGE), matchWrap(dp(12)));
        }

        LinearLayout purge = whiteCard(dp(18), 0x1AD9534F);
        purge.setOrientation(LinearLayout.VERTICAL);
        purge.setPadding(dp(16), dp(14), dp(16), dp(14));
        purge.addView(text("纯净模式", 15, C_RED, true, dp(4)), matchWrap(0));
        purge.addView(text("停用所有脚本，并清掉全部注入（点位 / 方法重写 / 开关覆盖 /"
                + "页面接管 / 入口 / 换皮）。做完就完全回到官方行为。",
                11, 0xFFB25B33, false, dp(10)), matchWrap(0));

        TextView btn = text("一键回到官方行为", 14, 0xFFFFFFFF, true, 0);
        btn.setGravity(Gravity.CENTER);
        GradientDrawable g = new GradientDrawable();
        g.setColor(C_RED);
        g.setCornerRadius(dp(14));
        btn.setBackgroundDrawable(g);
        btn.setPadding(dp(14), dp(11), dp(14), dp(11));
        btn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                press(v);
                
                com.cathelperawa.owo.app.core.Bg.run(new Runnable() {
                    @Override public void run() {
                        final String msg;
                        try {
                            int n = ScriptHost.disableAll();
                            int p = PatchPoint.clearAll();
                            int m = MethodHook.clearAll();
                            int s2 = ScriptScreen.clearAll();
                            int e = ScriptSetting.clearAll();
                            ConfigOverride.clearAll();
                            List<String> all = KernelWhitelist.list();
                            for (int i = 0; i < all.size(); i++) {
                                KernelWhitelist.revoke(all.get(i));
                            }
                            try { Skin.clear(); } catch (Throwable ignore) {}
                            msg = "已停用 " + n + " 个脚本 · 点位 " + p + " · 方法 " + m
                                    + " · 页面 " + s2 + " · 入口 " + e;
                            HookLog.w("[审计] 纯净模式：" + msg);
                        } catch (Throwable t) {
                            return;
                        }
                        final android.app.Activity act = activity();
                        Runnable ui = new Runnable() {
                            @Override public void run() {
                                toast(msg);
                                rebuild();
                            }
                        };
                        try {
                            if (act != null) act.runOnUiThread(ui);
                            else ui.run();
                        } catch (Throwable ignore) {
                        }
                    }
                }, "cat-audit-purge");
            }
        });
        purge.addView(btn, matchWrap(0));
        content.addView(purge, matchWrap(dp(12)));

        TextView foot = text("· 停用脚本 = 它挂的所有东西自动撤掉\n"
                        + "· 这里只清\"注入\"，不删脚本文件；想彻底删去脚本页",
                10.5f, C_SUB, false, 0);
        foot.setPadding(dp(6), dp(6), 0, 0);
        content.addView(foot, matchWrap(0));
    }

    private void rebuild() {
        content.removeAllViews();
        content.addView(text("正在重新收集…", 12, C_SUB, false, 0), matchWrap(dp(20)));
        final android.app.Activity act = activity();
        com.cathelperawa.owo.app.core.Bg.run(new Runnable() {
            @Override public void run() {
                final Stats s;
                try {
                    s = collect();
                } catch (Throwable t) {
                    return;
                }
                Runnable ui = new Runnable() {
                    @Override public void run() {
                        try {
                            render(s);
                        } catch (Throwable ignore) {
                        }
                    }
                };
                try {
                    if (act != null) act.runOnUiThread(ui);
                    else ui.run();
                } catch (Throwable ignore) {
                }
            }
        }, "cat-audit");
    }

    private LinearLayout block(String title, String body, int color) {
        LinearLayout card = whiteCard(dp(18), 0xF2FFFFFF);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(13), dp(16), dp(13));
        card.addView(text(title, 13.5f, color, true, dp(6)), matchWrap(0));
        card.addView(text(body, 11, C_SUB, false, 0), matchWrap(0));
        return card;
    }

    private LinearLayout line(String label, String value, int color) {
        LinearLayout r = new LinearLayout(ctx);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setPadding(0, dp(1), 0, dp(1));
        r.addView(text(label, 11.5f, C_SUB, false, 0),
                new LinearLayout.LayoutParams(0,
                        LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        r.addView(text(value, 11.5f, color, true, 0), wrapWrap());
        return r;
    }
}
