package com.cathelperawa.owo.app.ai;

import android.graphics.Typeface;
import com.cathelperawa.owo.app.core.Bg;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.cathelperawa.owo.app.api.NetApi;
import com.cathelperawa.owo.app.api.PanelApi;
import com.cathelperawa.owo.app.ui.Page;

public final class AiProviderEditPage extends Page {

    private static final int C_MAIN = 0xFF44546A;
    private static final int C_SUB = 0xFF8A9BB0;
    private static final int C_PINK = 0xFFE85D9C;
    private static final int C_OK = 0xFF3FA96A;
    private static final int C_BAD = 0xFFE06666;

    private final AiProvider p;

    private EditText etName, etUrl, etKey, etModel, etTemp, etCtx;
    private TextView status;
    
    private LinearLayout modelBox;

    public AiProviderEditPage(Host host, AiProvider p) {
        super(host);
        this.p = p;
    }

    @Override
    public View build() {
        View[] out = new View[2];
        boolean isNew = AiProvider.find(p.id) == null;
        LinearLayout root = pageRoot(isNew ? "添加提供商" : "编辑提供商",
                "只填到 /v1 就行，后面自动补", out, true, 13, "bg_ai", 0x66);

        LinearLayout form = whiteCard(dp(20), 0xEEFFFFFF);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(16), dp(8), dp(16), dp(16));

        form.addView(field(0, "名称（自己看）", p.displayName(), InputType.TYPE_CLASS_TEXT));
        form.addView(field(1, "接口地址", p.baseUrl, InputType.TYPE_TEXT_VARIATION_URI));
        form.addView(field(2, "API Key", p.apiKey,
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD));

        final TextView eye = text("显示密钥", 11, C_SUB, false, 0);
        eye.setPadding(dp(2), dp(6), 0, 0);
        eye.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (etKey == null) return;
                boolean nowHidden = etKey.getTransformationMethod()
                        instanceof android.text.method.PasswordTransformationMethod;
                etKey.setTransformationMethod(nowHidden ? null
                        : android.text.method.PasswordTransformationMethod.getInstance());
                eye.setText(nowHidden ? "隐藏密钥" : "显示密钥");
            }
        });
        form.addView(eye);
        form.addView(field(3, "模型名（测试连接后可点选）", p.model,
                InputType.TYPE_CLASS_TEXT));

        modelBox = new LinearLayout(ctx);
        modelBox.setOrientation(LinearLayout.VERTICAL);
        form.addView(modelBox, matchWrap(dp(4)));
        form.addView(field(4, "温度 0~2", String.valueOf(p.temperature),
                InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL));
        form.addView(field(5, "上下文条数", String.valueOf(p.historyLimit),
                InputType.TYPE_CLASS_NUMBER));

        root.addView(form, matchWrap(dp(14)));

        status = text(isNew ? "还没测试过" : "已保存", 12, C_SUB, false, 0);
        status.setPadding(dp(4), dp(6), dp(4), dp(6));
        root.addView(status, matchWrap(dp(2)));

        root.addView(actionButton("测试连接", 0x40A9BEDC, 0xFF41556F,
                new View.OnClickListener() {
                    @Override public void onClick(View v) { test(); }
                }), matchWrap(dp(10)));

        if (!isNew && !p.id.equals(AiProvider.currentId())) {
            root.addView(actionButton("设为当前使用", 0x1F3FA96A, 0xFF2F8A57,
                    new View.OnClickListener() {
                        @Override public void onClick(View v) {
                            AiProvider.setCurrent(p.id);
                            toast("已切换到这个提供商");
                        }
                    }), matchWrap(dp(8)));
        }

        root.addView(actionButton("试聊一句", 0x33E85D9C, 0xFFD9538E,
                new View.OnClickListener() {
                    @Override public void onClick(View v) { chatDialog(); }
                }), matchWrap(dp(8)));

        root.addView(actionButton("保存", C_PINK, 0xFFFFFFFF,
                new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        collect();
                        AiProvider.upsert(p);
                        toast("已保存（密钥已加密）");
                        host.close();
                    }
                }), matchWrap(dp(8)));

        if (!isNew) {
            root.addView(actionButton("删除", 0x14E06666, 0xFFD9534F,
                    new View.OnClickListener() {
                        @Override public void onClick(View v) {
                            AiProvider.remove(p.id);
                            toast("已删除");
                            host.close();
                        }
                    }), matchWrap(dp(8)));
        }

        return out[0];
    }

    private View field(int index, String label, String value, int inputType) {
        LinearLayout box = new LinearLayout(ctx);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(0, dp(10), 0, dp(4));

        TextView lb = text(label, 11.5f, C_SUB, false, 0);
        lb.setPadding(dp(2), 0, 0, dp(4));
        box.addView(lb, matchWrap(0));

        EditText et = new EditText(ctx);
        et.setText(value == null ? "" : value);
        et.setTextSize(14);
        et.setSingleLine(true);
        et.setTextColor(C_MAIN);
        et.setHintTextColor(0xFFBFC9D6);
        et.setInputType(inputType);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0x66FFFFFF);
        bg.setCornerRadius(dp(12));
        bg.setStroke(Math.max(1, dp(1)), 0x33A9BEDC);
        et.setBackgroundDrawable(bg);
        et.setPadding(dp(12), dp(10), dp(12), dp(10));
        box.addView(et, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(44)));

        if (index == 0) etName = et;
        else if (index == 1) etUrl = et;
        else if (index == 2) etKey = et;
        else if (index == 3) etModel = et;
        else if (index == 4) etTemp = et;
        else if (index == 5) etCtx = et;

        return box;
    }

    private View actionButton(String label, int bgColor, int textColor,
                              View.OnClickListener l) {
        TextView btn = new TextView(ctx);
        btn.setText(label);
        btn.setTextSize(14.5f);
        btn.setTextColor(textColor);
        btn.setTypeface(Typeface.DEFAULT_BOLD);
        btn.setGravity(Gravity.CENTER);
        btn.setPadding(0, dp(13), 0, dp(13));
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(15));
        bg.setColor(bgColor);
        btn.setBackgroundDrawable(bg);
        press(btn);
        btn.setOnClickListener(l);
        return btn;
    }

    private void collect() {
        p.name = str(etName);
        p.baseUrl = str(etUrl).trim();
        p.apiKey = str(etKey).trim();
        p.model = str(etModel).trim();
        try {
            p.temperature = Float.parseFloat(str(etTemp).trim());
        } catch (Throwable ignore) {
        }
        try {
            p.historyLimit = Integer.parseInt(str(etCtx).trim());
        } catch (Throwable ignore) {
        }
    }

    private static String str(EditText et) {
        return et == null || et.getText() == null ? "" : et.getText().toString();
    }

    private static String[] modelUrls(String base) {
        String b = base == null ? "" : base.trim();
        while (b.endsWith("/")) b = b.substring(0, b.length() - 1);
        if (b.endsWith("/chat/completions")) {
            b = b.substring(0, b.length() - "/chat/completions".length());
        }
        if (b.endsWith("/models")) return new String[]{b};
        if (b.endsWith("/v1") || b.endsWith("/v1beta") || b.endsWith("/openai")) {
            return new String[]{b + "/models"};
        }
        return new String[]{b + "/v1/models", b + "/models"};
    }

    private void test() {
        collect();
        if (p.baseUrl == null || p.baseUrl.length() == 0) {
            status.setText("先填接口地址");
            status.setTextColor(C_BAD);
            return;
        }
        final String[] urls = modelUrls(p.baseUrl);
        status.setText("测试中… " + urls[0]);
        status.setTextColor(C_SUB);

        Bg.run(new Runnable() {
            @Override public void run() {
                String headers = null;
                try {
                    org.json.JSONObject h = new org.json.JSONObject();
                    h.put("Authorization", "Bearer " + p.apiKey);
                    h.put("Accept", "application/json");
                    headers = h.toString();
                } catch (Throwable ignore) {
                }

                NetApi.Result r = null;
                String used = urls[0];
                for (int i = 0; i < urls.length; i++) {
                    NetApi.Result one = NetApi.getSync(urls[i], headers, 12000);
                    if (r == null) r = one;
                    if (one.is2xx()) {
                        r = one;
                        used = urls[i];
                        break;
                    }
                    
                    if (one.code != 404 && one.code != 405) {
                        r = one;
                        used = urls[i];
                        break;
                    }
                }
                final NetApi.Result fr = r;
                final String fUsed = used;
                if (ctx instanceof android.app.Activity) {
                    ((android.app.Activity) ctx).runOnUiThread(new Runnable() {
                        @Override public void run() {
                            if (fr.is2xx()) {
                                status.setText("✓ 连接正常（" + fUsed + " · "
                                        + fr.code + " · " + fr.costMs + "ms）");
                                status.setTextColor(C_OK);
                                java.util.List<String> ids = parseModels(fr.body);
                                if (!ids.isEmpty()) {
                                    status.setText("✓ 连接正常 · " + ids.size()
                                            + " 个模型，点下面选一个");
                                    showModels(ids);
                                }
                            } else {
                                String err = fr.err != null ? fr.err
                                        : ("HTTP " + fr.code);
                                status.setText("✗ " + fUsed + " · " + err
                                        + (fr.body != null && fr.body.length() > 0
                                        ? " · " + shorten(fr.body) : ""));
                                status.setTextColor(C_BAD);
                            }
                        }
                    });
                }
            }
        }, "cat-ai-test");
    }

    private static java.util.List<String> parseModels(String body) {
        java.util.List<String> out = new java.util.ArrayList<String>();
        try {
            String t = body == null ? "" : body.trim();
            if (t.startsWith("{")) {
                org.json.JSONObject j = new org.json.JSONObject(t);
                org.json.JSONArray arr = j.optJSONArray("data");
                if (arr == null) arr = j.optJSONArray("models");
                if (arr != null) {
                    for (int i = 0; i < arr.length(); i++) {
                        Object o = arr.opt(i);
                        String id = null;
                        if (o instanceof String) id = (String) o;
                        else if (o instanceof org.json.JSONObject) {
                            org.json.JSONObject jo = (org.json.JSONObject) o;
                            id = jo.optString("id", jo.optString("name",
                                    jo.optString("model", "")));
                        }
                        if (id != null && id.trim().length() > 0) out.add(id.trim());
                    }
                }
            } else if (t.startsWith("[")) {
                org.json.JSONArray arr = new org.json.JSONArray(t);
                for (int i = 0; i < arr.length(); i++) {
                    String id = arr.optString(i, "");
                    if (id.length() > 0) out.add(id);
                }
            }
        } catch (Throwable ignore) {
        }
        java.util.Collections.sort(out);
        return out;
    }

    private java.util.List<String> allModels = new java.util.ArrayList<String>();
    
    private LinearLayout chipBox;
    
    private TextView modelCount;

    private void showModels(java.util.List<String> ids) {
        if (modelBox == null) return;
        modelBox.removeAllViews();
        allModels = new java.util.ArrayList<String>(ids);
        chipBox = null;
        modelCount = null;

        LinearLayout head = new LinearLayout(ctx);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);

        TextView tip = text("可用模型（点一下填入）", 11, C_SUB, false, 0);
        tip.setPadding(dp(2), dp(2), 0, dp(6));
        head.addView(tip, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        modelCount = text("共 " + allModels.size() + " 个", 11, C_SUB, false, 0);
        modelCount.setPadding(0, dp(2), dp(2), dp(6));
        head.addView(modelCount, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        modelBox.addView(head, matchWrap(0));

        EditText search = new EditText(ctx);
        search.setHint("搜模型名，比如 kimi / global");
        search.setHintTextColor(0xFFBFC9D6);
        search.setTextSize(13);
        search.setSingleLine(true);
        search.setTextColor(C_MAIN);
        search.setInputType(InputType.TYPE_CLASS_TEXT);
        GradientDrawable sbg = new GradientDrawable();
        sbg.setColor(0x66FFFFFF);
        sbg.setCornerRadius(dp(12));
        sbg.setStroke(Math.max(1, dp(1)), 0x33A9BEDC);
        search.setBackgroundDrawable(sbg);
        search.setPadding(dp(12), dp(8), dp(12), dp(8));
        modelBox.addView(search, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(40)));
        search.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(android.text.Editable e) {
                renderChips(e == null ? "" : e.toString());
            }
        });

        chipBox = new LinearLayout(ctx);
        chipBox.setOrientation(LinearLayout.VERTICAL);
        modelBox.addView(chipBox, matchWrap(dp(4)));

        renderChips("");
    }

    private void renderChips(String q) {
        if (chipBox == null) return;
        chipBox.removeAllViews();

        String key = q == null ? "" : q.trim().toLowerCase(java.util.Locale.ROOT);
        java.util.List<String> hits = new java.util.ArrayList<String>();
        for (int i = 0; i < allModels.size(); i++) {
            String id = allModels.get(i);
            if (key.length() == 0
                    || id.toLowerCase(java.util.Locale.ROOT).contains(key)) {
                hits.add(id);
            }
        }

        if (modelCount != null) {
            modelCount.setText(hits.size() == allModels.size()
                    ? ("共 " + allModels.size() + " 个")
                    : ("匹配 " + hits.size() + " / " + allModels.size()));
        }

        if (hits.isEmpty()) {
            TextView none = text("没有模型名含「" + q.trim() + "」", 11.5f, C_BAD, false, 0);
            none.setPadding(dp(2), dp(8), 0, dp(4));
            chipBox.addView(none, matchWrap(0));
            return;
        }

        LinearLayout row = null;
        for (int i = 0; i < hits.size(); i++) {
            final String id = hits.get(i);
            if (i % 2 == 0) {
                row = new LinearLayout(ctx);
                row.setOrientation(LinearLayout.HORIZONTAL);
                chipBox.addView(row, matchWrap(dp(6)));
            }
            TextView chip = new TextView(ctx);
            chip.setText(id);
            chip.setTextSize(11.5f);
            chip.setTextColor(0xFF5A6B80);
            chip.setSingleLine(true);
            chip.setEllipsize(android.text.TextUtils.TruncateAt.END);
            chip.setPadding(dp(10), dp(8), dp(10), dp(8));
            GradientDrawable cg = new GradientDrawable();
            cg.setCornerRadius(dp(9));
            cg.setColor(0x33A9BEDC);
            chip.setBackgroundDrawable(cg);
            chip.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    if (etModel != null) etModel.setText(id);
                    status.setText("已选：" + id);
                    status.setTextColor(C_OK);
                }
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            lp.rightMargin = dp(6);
            row.addView(chip, lp);
        }
    }

    

    
    private void chatDialog() {
        collect();
        if (p.model == null || p.model.length() == 0) {
            status.setText("先选一个模型");
            status.setTextColor(C_BAD);
            return;
        }

        LinearLayout body = new LinearLayout(ctx);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(2), dp(6), dp(2), 0);

        final EditText et = new EditText(ctx);
        et.setHint("随便说一句，比如「你好」");
        et.setHintTextColor(0xFFBFC9D6);
        et.setTextSize(14);
        et.setTextColor(C_MAIN);
        et.setMinLines(2);
        et.setText("你好，用一句话介绍你自己");
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0x66FFFFFF);
        bg.setCornerRadius(dp(12));
        bg.setStroke(Math.max(1, dp(1)), 0x33A9BEDC);
        et.setBackgroundDrawable(bg);
        et.setPadding(dp(12), dp(10), dp(12), dp(10));
        body.addView(et, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(78)));

        PanelApi.show(ctx, null, "试聊一句", "用「" + p.model + "」真发一条",
                body, "发送", new PanelApi.OnOk() {
                    @Override public void run() {
                        String t = et.getText() == null ? "" : et.getText().toString();
                        if (t.trim().length() == 0) return;
                        doChat(t);
                    }
                });
    }

    private void doChat(final String userText) {
        status.setText("发送中…");
        status.setTextColor(C_SUB);

        Bg.run(new Runnable() {
            @Override public void run() {
                final String[] result = new String[]{"", ""};   
                try {
                    org.json.JSONObject body = new org.json.JSONObject();
                    body.put("model", p.model);
                    org.json.JSONArray msgs = new org.json.JSONArray();
                    org.json.JSONObject m = new org.json.JSONObject();
                    m.put("role", "user");
                    m.put("content", userText);
                    msgs.put(m);
                    body.put("messages", msgs);
                    body.put("temperature", (double) p.temperature);
                    body.put("max_tokens", p.maxTokens);

                    org.json.JSONObject h = new org.json.JSONObject();
                    h.put("Authorization", "Bearer " + p.apiKey);
                    h.put("Content-Type", "application/json");

                    String[] urls = chatUrls(p.baseUrl);
                    NetApi.Result r = null;
                    for (int i = 0; i < urls.length; i++) {
                        r = NetApi.postSync(urls[i], h.toString(), body.toString(), 40000);
                        if (r.is2xx()) break;
                        if (r.code != 404 && r.code != 405) break;
                    }

                    if (r != null && r.is2xx()) {
                        String content = parseChatContent(r.body);
                        if (content != null && content.length() > 0) {
                            result[0] = "ok";
                            result[1] = content;
                        } else {
                            result[0] = "err";
                            result[1] = "返回里没有 choices[0].message.content\n" + shorten(r.body);
                        }
                    } else {
                        result[0] = "err";
                        String err = r == null ? "无响应"
                                : (r.err != null ? r.err : ("HTTP " + r.code));
                        result[1] = err + (r != null && r.body != null && r.body.length() > 0
                                ? "\n" + shorten(r.body) : "");
                    }
                } catch (Throwable t) {
                    result[0] = "err";
                    result[1] = String.valueOf(t);
                }

                if (!(ctx instanceof android.app.Activity)) return;
                ((android.app.Activity) ctx).runOnUiThread(new Runnable() {
                    @Override public void run() {
                        if ("ok".equals(result[0])) {
                            status.setText("✓ 模型回复了（" + result[1].length() + " 字）");
                            status.setTextColor(C_OK);
                            showReply(result[1]);
                        } else {
                            status.setText("✗ 试聊失败");
                            status.setTextColor(C_BAD);
                            showError(result[1]);
                        }
                    }
                });
            }
        }, "cat-ai-chat");
    }

    private void showReply(String text) {
        LinearLayout body = new LinearLayout(ctx);
        body.setOrientation(LinearLayout.VERTICAL);

        TextView t = new TextView(ctx);
        t.setText(text);
        t.setTextSize(14);
        t.setTextColor(C_MAIN);
        t.setLineSpacing(dp(4), 1.1f);
        t.setTextIsSelectable(true);
        body.addView(t);

        PanelApi.show(ctx, null, "模型回复", p.model, body, "关闭", null);
    }

    private void showError(String text) {
        LinearLayout body = new LinearLayout(ctx);
        TextView t = new TextView(ctx);
        t.setText(text);
        t.setTextSize(12);
        t.setTextColor(C_BAD);
        t.setTextIsSelectable(true);
        body.addView(t);
        PanelApi.show(ctx, null, "试聊失败", null, body, "关闭", null);
    }

    private static String parseChatContent(String body) {
        try {
            org.json.JSONObject j = new org.json.JSONObject(body);
            org.json.JSONArray ch = j.optJSONArray("choices");
            if (ch == null || ch.length() == 0) return null;
            org.json.JSONObject c0 = ch.optJSONObject(0);
            if (c0 == null) return null;
            org.json.JSONObject msg = c0.optJSONObject("message");
            if (msg != null) return msg.optString("content", null);
            return c0.optString("text", null);
        } catch (Throwable t) {
            return null;
        }
    }

    private static String[] chatUrls(String base) {
        String b = base == null ? "" : base.trim();
        while (b.endsWith("/")) b = b.substring(0, b.length() - 1);
        if (b.endsWith("/chat/completions")) return new String[]{b};
        if (b.endsWith("/v1") || b.endsWith("/v1beta") || b.endsWith("/openai")) {
            return new String[]{b + "/chat/completions"};
        }
        return new String[]{b + "/v1/chat/completions", b + "/chat/completions"};
    }

    private static String shorten(String s) {
        String t = s.replace('\n', ' ').trim();
        return t.length() > 80 ? t.substring(0, 80) + "…" : t;
    }
}
