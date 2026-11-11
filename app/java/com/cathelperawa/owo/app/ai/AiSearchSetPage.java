package com.cathelperawa.owo.app.ai;

import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.cathelperawa.owo.app.ui.CatSheet;
import com.cathelperawa.owo.app.ui.Page;
import com.cathelperawa.owo.app.ui.SwitchBox;

public final class AiSearchSetPage extends Page {

    private static final int C_MAIN = 0xFF3C4B60;
    private static final int C_SUB = 0xFF8A9BB0;
    private static final int C_PINK = 0xFFE85D9C;
    private static final int C_OK = 0xFF3FA96A;
    private static final int C_BAD = 0xFFD9534F;

    private LinearLayout body;
    private LinearLayout content;
    private TextView status;

    public AiSearchSetPage(Host host) {
        super(host);
    }

    @Override
    public View build() {
        View[] out = new View[2];
        body = pageRoot("搜索接口", "联网搜索用哪个 API", out, true, 13, "bg_ai", 0x66);
        content = new LinearLayout(ctx);
        content.setOrientation(LinearLayout.VERTICAL);
        body.addView(content, matchWrap(0));
        render();
        return out[0];
    }

    private void render() {
        content.removeAllViews();

        LinearLayout head = whiteCard(dp(22), 0xF2FFFFFF);
        head.setOrientation(LinearLayout.VERTICAL);
        head.setPadding(dp(18), dp(16), dp(18), dp(16));
        head.addView(text("搜索接口", 18, C_MAIN, true, dp(4)), matchWrap(0));
        status = text(AiSearch.statusText(), 11, C_SUB, false, 0);
        head.addView(status, matchWrap(0));
        content.addView(head, matchWrap(dp(14)));

        LinearLayout sw = whiteCard(dp(20), 0xF2FFFFFF);
        sw.setOrientation(LinearLayout.VERTICAL);
        sw.setPadding(dp(16), dp(8), dp(16), dp(10));
        sw.addView(switchRow("允许联网搜索", "它觉得需要查的时候会自己搜（不开就完全不发请求）",
                "ai_search_on"), matchWrap(0));
        content.addView(sw, matchWrap(dp(14)));

        LinearLayout card = whiteCard(dp(20), 0xF2FFFFFF);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        card.addView(text("接口", 12, C_PINK, true, dp(8)), matchWrap(0));

        card.addView(textEdit("接口地址", "https://…（GET 可用 {query} 占位）",
                AiSearch.url(), "ai_search_url", InputType.TYPE_TEXT_VARIATION_URI), matchWrap(dp(10)));

        card.addView(textEdit("API Key", "没 Key 就留空",
                AiSearch.key(), null, InputType.TYPE_CLASS_TEXT), matchWrap(dp(10)));

        card.addView(pickRow("鉴权方式", authLabel(), new String[]{
                "Authorization: Bearer", "X-Subscription-Token", "自定义 header", "不需要 Key"
        }, new CatSheet.OnPick() {
            @Override public void run(int i, String label) {
                AiConfig.setString("ai_search_auth", i == 1 ? "x-sub" : (i == 2 ? "custom" : (i == 3 ? "none" : "bearer")));
                render();
            }
        }), matchWrap(dp(10)));

        if ("custom".equals(AiSearch.auth())) {
            card.addView(textEdit("header 名称", "比如 X-API-KEY",
                    AiSearch.authHeader(), "ai_search_header", InputType.TYPE_CLASS_TEXT), matchWrap(dp(10)));
        }

        card.addView(pickRow("请求方式", "post".equalsIgnoreCase(AiSearch.method()) ? "POST JSON" : "GET",
                new String[]{"POST JSON", "GET"}, new CatSheet.OnPick() {
                    @Override public void run(int i, String label) {
                        AiConfig.setString("ai_search_method", i == 0 ? "post" : "get");
                        render();
                    }
                }), matchWrap(dp(10)));

        card.addView(textEdit("最多条数", "1~10，默认 5",
                String.valueOf(AiSearch.limit()), "ai_search_limit", InputType.TYPE_CLASS_NUMBER),
                matchWrap(0));
        content.addView(card, matchWrap(dp(14)));

        LinearLayout test = whiteCard(dp(20), 0xF2FFFFFF);
        test.setOrientation(LinearLayout.VERTICAL);
        test.setPadding(dp(16), dp(14), dp(16), dp(14));
        TextView btn = text("测一下", 13, C_PINK, true, 0);
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
                toast("正在搜…");
                AiSearch.testAsync(new AiSearch.Cb() {
                    @Override public void done(final String msg, final boolean ok) {
                        final android.app.Activity act = activity();
                        if (act == null) return;
                        act.runOnUiThread(new Runnable() {
                            @Override public void run() {
                                CatSheet.confirm(ctx, ok ? "搜索正常" : "搜索失败", msg,
                                        "知道了", false, new Runnable() {
                                            @Override public void run() {}
                                        });
                                if (status != null) status.setText(AiSearch.statusText());
                            }
                        });
                    }
                });
            }
        });
        test.addView(btn, matchWrap(0));
        test.addView(text("会拿「今天天气」试一次，结果显示在弹窗里", 10.5f, C_SUB, false, 0),
                matchWrap(dp(6)));
        content.addView(test, matchWrap(dp(14)));

        LinearLayout help = whiteCard(dp(20), 0xF2FFFFFF);
        help.setOrientation(LinearLayout.VERTICAL);
        help.setPadding(dp(16), dp(14), dp(16), dp(14));
        help.addView(text("常见服务的填法", 12, C_PINK, true, dp(8)), matchWrap(0));
        String[][] rows = new String[][]{
                {"Tavily", "https://api.tavily.com/search · POST · Bearer"},
                {"Serper", "https://google.serper.dev/search · POST · X-Subscription-Token"},
                {"Brave", "https://api.search.brave.com/res/v1/web/search?q={query} · GET · X-Subscription-Token"},
                {"SearXNG", "http://你的地址/search?q={query}&format=json · GET · 不需要 Key"}
        };
        for (int i = 0; i < rows.length; i++) {
            TextView t = text(rows[i][0] + "：" + rows[i][1], 11, C_SUB, false, 0);
            t.setPadding(0, dp(3), 0, dp(3));
            t.setLineSpacing(dp(2), 1f);
            help.addView(t, matchWrap(0));
        }
        content.addView(help, matchWrap(dp(14)));
    }

    private String authLabel() {
        String a = AiSearch.auth();
        if ("x-sub".equals(a)) return "X-Subscription-Token";
        if ("custom".equals(a)) return "自定义 header";
        if ("none".equals(a)) return "不需要 Key";
        return "Authorization: Bearer";
    }

    private View switchRow(String title, String sub, final String key) {
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(8), 0, dp(8));

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

    private View textEdit(String title, String hint, String value,
                          final String key, int inputType) {
        LinearLayout box = new LinearLayout(ctx);
        box.setOrientation(LinearLayout.VERTICAL);

        box.addView(text(title, 13, C_MAIN, true, dp(4)), matchWrap(0));

        final EditText et = new EditText(ctx);
        et.setText(value == null ? "" : value);
        et.setHint(hint);
        et.setHintTextColor(0xFFBFC9D6);
        et.setTextSize(12.5f);
        et.setTextColor(0xFF5A6B80);
        et.setSingleLine(true);
        et.setInputType(inputType);
        android.graphics.drawable.GradientDrawable bg =
                new android.graphics.drawable.GradientDrawable();
        bg.setColor(0x66FFFFFF);
        bg.setCornerRadius(dp(11));
        bg.setStroke(Math.max(1, dp(1)), 0x33A9BEDC);
        et.setBackgroundDrawable(bg);
        et.setPadding(dp(12), dp(9), dp(12), dp(9));
        box.addView(et, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(42)));

        et.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override public void onFocusChange(View v, boolean has) {
                if (has) return;
                String t = et.getText() == null ? "" : et.getText().toString().trim();
                if (key == null) {
                    
                    AiSearch.setKey(t);
                } else if ("ai_search_limit".equals(key)) {
                    int n = 5;
                    try {
                        n = Integer.parseInt(t);
                    } catch (Throwable ignore) {
                    }
                    if (n < 1) n = 1;
                    if (n > 10) n = 10;
                    AiConfig.setInt(key, n);
                } else {
                    AiConfig.setString(key, t);
                }
                if (status != null) status.setText(AiSearch.statusText());
            }
        });
        return box;
    }

    private View pickRow(String title, String value, final String[] items,
                         final CatSheet.OnPick cb) {
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(8), 0, dp(8));

        row.addView(text(title, 13, C_MAIN, true, 0), new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView v = text(value, 12.5f, C_PINK, true, 0);
        row.addView(v, wrapWrap());
        TextView arrow = text(" ›", 13, 0xFFB6C1D0, false, 0);
        row.addView(arrow, wrapWrap());

        press(row);
        row.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                CatSheet.pick(ctx, items[0], items, new CatSheet.OnPick() {
                    @Override public void run(int i, String label) {
                        cb.run(i, label);
                    }
                });
            }
        });
        return row;
    }
}
