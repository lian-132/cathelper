package com.cathelperawa.owo.app.ui;

import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public final class SendPage extends Page {

    private static final String[][] ROUTE = {
            {"卡片 JSON", "内核 ark", "含 app + view → 内核原生 MsgElement（服务端只认这条）"},
            {"元素 10", "解包 → 内核", "{\"10\":{\"1\":\"<ark>\"}} 先解包成 ark 再发"},
            {"文本 / 其它", "PbSendMsg 直发", "文本元素已验证；要长消息在面板里开开关"},
    };

    private static final String[] PB_FORMATS = {"hex", "base64", "JSON"};

    private LinearLayout tabCode;
    private LinearLayout tabPb;
    private LinearLayout tabMedia;
    private TextView capCode;
    private TextView capPb;
    private TextView capMedia;

    private int curTab = 0;
    private int curFormat = 0;
    private TextView[] formatCaps;

    public SendPage(Page.Host host) {
        super(host);
    }

    

    @Override
    public View build() {
        View[] out = new View[2];
        LinearLayout root = pageRoot("发送与协议", "卡片代码 · 消息代码 · PB 工具", out, true, 12);

        root.addView(buildHero(), matchWrap(dp(11)));
        root.addView(buildTabBar(), matchWrap(dp(10)));

        tabCode = buildCodeTab();
        tabPb = buildPbTab();
        tabMedia = buildMediaTab();
        root.addView(tabCode, matchWrap(dp(0)));
        root.addView(tabPb, matchWrap(dp(0)));
        root.addView(tabMedia, matchWrap(dp(0)));
        tabPb.setVisibility(View.GONE);
        tabMedia.setVisibility(View.GONE);

        fadeIn(root);
        return out[0];
    }

    

    private View buildHero() {
        FrameLayout hero = new FrameLayout(ctx);
        GradientDrawable hg = new GradientDrawable();
        hg.setColor(0xFFFFFFFF);
        hg.setCornerRadius(dp(26));
        hg.setStroke(dp(1), 0x77FFFFFF);
        hero.setBackgroundDrawable(hg);
        hero.setClipToOutline(true);
        hero.setElevation(dp(7));

        Bitmap bmp = image("art3");
        if (bmp != null) {
            ImageView iv = new ImageView(ctx);
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            iv.setImageBitmap(bmp);
            hero.addView(iv, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, dp(150)));
        }
        View veil = new View(ctx);
        veil.setBackgroundDrawable(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0x00FFFFFF, 0x99FFFFFF, 0xF8FFFFFF}));
        hero.addView(veil, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, dp(100), Gravity.BOTTOM));

        LinearLayout ht = new LinearLayout(ctx);
        ht.setOrientation(LinearLayout.VERTICAL);
        ht.setPadding(dp(16), 0, dp(16), dp(14));

        ht.addView(text("发送与协议", 20, 0xFF4A5872, true, dp(3)));
        ht.addView(text("把消息代码编译成真消息", 12, 0xFF8A99AC, false, 0));

        hero.addView(ht, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM));
        return hero;
    }

    

    private View buildTabBar() {
        LinearLayout bar = new LinearLayout(ctx);
        bar.setOrientation(LinearLayout.HORIZONTAL);

        capCode = capsule("发送代码", true);
        capPb = capsule("PB 工具", false);
        capMedia = capsule("伪造素材", false);

        LinearLayout.LayoutParams p1 = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        p1.rightMargin = dp(4);
        LinearLayout.LayoutParams p2 = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        p2.leftMargin = dp(4);
        p2.rightMargin = dp(4);
        LinearLayout.LayoutParams p3 = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        p3.leftMargin = dp(4);
        p3.rightMargin = dp(4);
        capCode.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                switchTab(0);
            }
        });
        capPb.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                switchTab(1);
            }
        });
        capMedia.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                switchTab(2);
            }
        });
        bar.addView(capCode, p1);
        bar.addView(capPb, p2);
        bar.addView(capMedia, p3);
        return bar;
    }

    private void switchTab(int idx) {
        curTab = idx;
        paintCapsule(capCode, idx == 0);
        paintCapsule(capPb, idx == 1);
        paintCapsule(capMedia, idx == 2);
        if (tabCode != null) tabCode.setVisibility(idx == 0 ? View.VISIBLE : View.GONE);
        if (tabPb != null) tabPb.setVisibility(idx == 1 ? View.VISIBLE : View.GONE);
        if (tabMedia != null) tabMedia.setVisibility(idx == 2 ? View.VISIBLE : View.GONE);
        ScrollView sv = findScroll(capCode);
        if (sv != null) sv.scrollTo(0, 0);
    }

    private TextView capsule(String label, boolean on) {
        TextView tv = new TextView(ctx);
        tv.setText(label);
        tv.setTextSize(13);
        tv.setGravity(Gravity.CENTER);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setPadding(dp(4), dp(9), dp(4), dp(9));
        paintCapsule(tv, on);
        return tv;
    }

    private void paintCapsule(TextView tv, boolean on) {
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(dp(14));
        if (on) {
            g.setColor(0xFFFFC2DF);
            g.setStroke(dp(1), 0xFFFF9FCB);
            tv.setTextColor(0xFFE85D9C);
        } else {
            g.setColor(0x66FFFFFF);
            g.setStroke(dp(1), 0x44FFFFFF);
            tv.setTextColor(0xFF8A9CB5);
        }
        tv.setBackgroundDrawable(g);
    }

    

    private LinearLayout buildCodeTab() {
        LinearLayout box = new LinearLayout(ctx);
        box.setOrientation(LinearLayout.VERTICAL);

        box.addView(hintCard("发送在聊天里",
                "长按消息 / 输入框旁的菜单 →「发送代码」，目标就是当前会话"), matchWrap(dp(9)));

        final EditText code = multiEdit("例如 {\"1\":{\"1\":\"你好\"}}", 8);
        
        final LinearLayout[] tplBoxRef = new LinearLayout[1];
        final TextView[] tplCountRef = new TextView[1];

        LinearLayout codeCard = whiteCard(dp(18), 0xE6FFFFFF);
        codeCard.setPadding(dp(14), dp(13), dp(14), dp(13));
        LinearLayout codeHead = new LinearLayout(ctx);
        codeHead.setOrientation(LinearLayout.HORIZONTAL);
        codeHead.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout codeTitle = new LinearLayout(ctx);
        codeTitle.setOrientation(LinearLayout.VERTICAL);
        codeTitle.addView(text("代码", 15, 0xFF43587A, true, dp(2)));
        codeTitle.addView(text("元素 JSON / 卡片 JSON / 预设条目", 11, 0xFF8A9CB5, false, 0));
        codeHead.addView(codeTitle, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        TextView paste = pill("粘贴", 0xFF7FA5E6);
        paste.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                try {
                    android.content.ClipboardManager cm = (android.content.ClipboardManager)
                            ctx.getSystemService(android.content.Context.CLIPBOARD_SERVICE);
                    if (cm == null || !cm.hasPrimaryClip()) {
                        toast("剪贴板是空的喵");
                        return;
                    }
                    android.content.ClipData cd = cm.getPrimaryClip();
                    if (cd == null || cd.getItemCount() == 0) return;
                    CharSequence t = cd.getItemAt(0).coerceToText(ctx);
                    if (t != null) {
                        code.setText(t.toString().trim());
                        toast("已粘贴");
                    }
                } catch (Throwable e) {
                    toast("粘贴失败：" + e);
                }
            }
        });
        codeHead.addView(paste, wrapWrap());
        codeCard.addView(codeHead, matchWrap(dp(9)));

        codeCard.addView(code, matchWrap(dp(9)));

        LinearLayout codeBtns = new LinearLayout(ctx);
        codeBtns.setOrientation(LinearLayout.HORIZONTAL);
        TextView clear = ghostButton("清空");
        clear.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                code.setText("");
            }
        });
        TextView save = ghostButton("存为模板");
        save.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                final String content = code.getText() == null
                        ? "" : code.getText().toString().trim();
                if (content.length() == 0) {
                    toast("代码是空的，没东西可存喵");
                    return;
                }
                CatSheet.input(ctx, "模板名字", "",
                        "模板" + (System.currentTimeMillis() % 10000),
                        new CatSheet.OnText() {
                            @Override public void run(String text) {
                                String name = text == null ? "" : text.trim();
                                if (name.length() == 0) name = "未命名";
                                boolean ok = com.cathelperawa.owo.app.core.TemplateStore
                                        .save(ctx, name, 0, content);
                                toast(ok ? ("已存为模板：" + name) : "存模板失败喵");
                                if (ok && tplBoxRef[0] != null) {
                                    renderTemplates(tplBoxRef[0], tplCountRef[0], code, codeCard);
                                }
                            }
                        });
            }
        });
        LinearLayout.LayoutParams c1 = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        c1.rightMargin = dp(5);
        LinearLayout.LayoutParams c2 = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        c2.leftMargin = dp(5);
        codeBtns.addView(clear, c1);
        codeBtns.addView(save, c2);
        codeCard.addView(codeBtns, matchWrap(dp(0)));
        box.addView(codeCard, matchWrap(dp(9)));

        LinearLayout tplHead = new LinearLayout(ctx);
        tplHead.setOrientation(LinearLayout.HORIZONTAL);
        tplHead.setGravity(Gravity.CENTER_VERTICAL);
        tplHead.setPadding(dp(4), 0, dp(4), 0);
        tplHead.addView(text("模板库", 15, 0xFF43587A, true, 0),
                new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        final TextView tplCount = text("", 11, 0xFF8A9CB5, false, 0);
        tplCountRef[0] = tplCount;
        tplHead.addView(tplCount);
        box.addView(tplHead, matchWrap(dp(6)));

        final LinearLayout tplBox = new LinearLayout(ctx);
        tplBoxRef[0] = tplBox;
        tplBox.setOrientation(LinearLayout.VERTICAL);
        box.addView(tplBox, matchWrap(dp(9)));

        renderTemplates(tplBox, tplCount, code, codeCard);

        LinearLayout route = whiteCard(dp(18), 0x8CFFFFFF);
        route.setPadding(dp(14), dp(12), dp(14), dp(12));
        route.addView(text("发送通道（自动判定）", 13, 0xFF43587A, true, dp(8)));
        for (int i = 0; i < ROUTE.length; i++) {
            LinearLayout line = new LinearLayout(ctx);
            line.setOrientation(LinearLayout.HORIZONTAL);
            TextView k = pill(ROUTE[i][0], 0xFF7FA5E6);
            ((GradientDrawable) k.getBackground()).setColor(0xF2FFFFFF);
            line.addView(k, wrapWrap());
            TextView arrow = text("→", 12, 0xFFB0BDD1, false, 0);
            arrow.setPadding(dp(6), 0, dp(6), 0);
            line.addView(arrow);
            TextView v = text(ROUTE[i][1], 12, 0xFFE85D9C, true, 0);
            line.addView(v);
            route.addView(line, matchWrap(dp(4)));
            TextView desc = text(ROUTE[i][2], 10, 0xFF9AAABF, false, 0);
            desc.setPadding(dp(2), 0, 0, dp(4));
            route.addView(desc, matchWrap(dp(0)));
        }
        box.addView(route, matchWrap(dp(9)));

        return box;
    }

    

    private LinearLayout buildPbTab() {
        LinearLayout box = new LinearLayout(ctx);
        box.setOrientation(LinearLayout.VERTICAL);

        LinearLayout inCard = whiteCard(dp(18), 0xE6FFFFFF);
        inCard.setPadding(dp(14), dp(13), dp(14), dp(13));
        inCard.addView(text("输入", 15, 0xFF43587A, true, dp(2)));
        inCard.addView(text("protobuf 字节或 JSON，key 就是字段号", 11, 0xFF8A9CB5, false, dp(9)));

        LinearLayout fmt = new LinearLayout(ctx);
        fmt.setOrientation(LinearLayout.HORIZONTAL);
        formatCaps = new TextView[PB_FORMATS.length];
        for (int i = 0; i < PB_FORMATS.length; i++) {
            final int idx = i;
            TextView c = capsule(PB_FORMATS[i], i == 0);
            c.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    curFormat = idx;
                    for (int j = 0; j < formatCaps.length; j++) {
                        paintCapsule(formatCaps[j], j == idx);
                    }
                }
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            if (i > 0) lp.leftMargin = dp(4);
            if (i < PB_FORMATS.length - 1) lp.rightMargin = dp(4);
            fmt.addView(c, lp);
            formatCaps[i] = c;
        }
        inCard.addView(fmt, matchWrap(dp(9)));

        final EditText raw = multiEdit("0A 04 0A 02 68 69 …", 7);
        inCard.addView(raw, matchWrap(dp(9)));

        final TextView[] resRef = new TextView[1];

        LinearLayout ops = new LinearLayout(ctx);
        ops.setOrientation(LinearLayout.HORIZONTAL);
        TextView dec = ghostButton("解码 → JSON");
        dec.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                String in = raw.getText() == null ? "" : raw.getText().toString().trim();
                if (in.length() == 0) { toast("先粘点东西进来喵"); return; }
                if (isJsonText(in)) {
                    toast("这是 JSON 不是字节；要转字节请点右边「JSON → 字节」");
                    return;
                }
                byte[] body = decodeInput(in, curFormat);
                if (body == null || body.length == 0) {
                    toast("解析不出字节，检查 hex / base64 格式喵");
                    return;
                }
                String json = decodePb(body);
                if (resRef[0] != null) resRef[0].setText(json);
                toast(json.startsWith("解码失败") ? "不是合法 protobuf 字节喵" 
                        : ("解出来了 · " + body.length + " 字节"));
            }
        });
        TextView enc = ghostButton("JSON → 字节");
        enc.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                String in = raw.getText() == null ? "" : raw.getText().toString().trim();
                if (in.length() == 0) { toast("先粘段 JSON 进来喵"); return; }
                if (!isJsonText(in)) {
                    toast("这里要的是「字段号 JSON」，例如 {\"1\":{\"1\":\"hi\"}}");
                    return;
                }
                byte[] b = encodePb(in);
                if (b == null || b.length == 0) {
                    toast("编不出来：key 必须是数字字段号（这条是消息属性，不是字段号）");
                    return;
                }
                String hex = hexOf(b);
                if (resRef[0] != null) resRef[0].setText(hex);
                toast("编好了 · " + b.length + " 字节");
            }
        });
        LinearLayout.LayoutParams d1 = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        d1.rightMargin = dp(5);
        LinearLayout.LayoutParams d2 = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        d2.leftMargin = dp(5);
        ops.addView(dec, d1);
        ops.addView(enc, d2);
        inCard.addView(ops, matchWrap(dp(0)));
        box.addView(inCard, matchWrap(dp(9)));

        LinearLayout outCard = whiteCard(dp(18), 0x8CFFFFFF);
        outCard.setPadding(dp(14), dp(12), dp(14), dp(12));
        LinearLayout outHead = new LinearLayout(ctx);
        outHead.setOrientation(LinearLayout.HORIZONTAL);
        outHead.setGravity(Gravity.CENTER_VERTICAL);
        outHead.addView(text("结果", 13, 0xFF43587A, true, 0),
                new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        TextView copy = pill("复制", 0xFF7FA5E6);
        copy.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                try {
                    if (resRef[0] == null) return;
                    String t = resRef[0].getText().toString();
                    android.content.ClipboardManager cm = (android.content.ClipboardManager)
                            ctx.getSystemService(android.content.Context.CLIPBOARD_SERVICE);
                    if (cm == null) return;
                    cm.setPrimaryClip(android.content.ClipData.newPlainText("pb", t));
                    toast("已复制");
                } catch (Throwable e) {
                    toast("复制失败：" + e);
                }
            }
        });
        outHead.addView(copy, wrapWrap());
        outCard.addView(outHead, matchWrap(dp(8)));

        TextView res = new TextView(ctx);
        res.setText("— 还没解析 —");
        res.setTextSize(12);
        res.setTextColor(0xFF8A9CB5);
        res.setTypeface(Typeface.MONOSPACE);
        res.setPadding(dp(12), dp(11), dp(12), dp(11));
        res.setTextIsSelectable(true);
        GradientDrawable rg = new GradientDrawable();
        rg.setColor(0x66FFFFFF);
        rg.setCornerRadius(dp(12));
        rg.setStroke(dp(1), 0x33C9D6E8);
        res.setBackgroundDrawable(rg);
        res.setMinLines(4);
        resRef[0] = res;
        outCard.addView(res, matchWrap(dp(0)));
        box.addView(outCard, matchWrap(dp(9)));

        box.addView(hintCard("发送在聊天里",
                "长按菜单 →「发送报文」，cmd 与字节在聊天里填，目标就是当前会话"), matchWrap(dp(9)));

        LinearLayout warn = whiteCard(dp(18), 0xFFFDF1F3);
        warn.setPadding(dp(14), dp(12), dp(14), dp(12));
        warn.addView(text("风控提示", 12, 0xFFD9536F, true, dp(5)));
        warn.addView(text("未知cmd乱发可能触发限流或封号", 11, 0xFFB07A88, false, 0));
        box.addView(warn, matchWrap(dp(9)));

        return box;
    }

    

    

    private View arkRow(final String name, String desc) {
        LinearLayout card = whiteCard(dp(16), 0x8CFFFFFF);
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(14), dp(11), dp(14), dp(11));

        LinearLayout texts = new LinearLayout(ctx);
        texts.setOrientation(LinearLayout.VERTICAL);
        texts.addView(text(name, 14.5f, 0xFF43587A, true, dp(2)));
        texts.addView(text(desc, 11, 0xFF8496AE, false, 0));
        row.addView(texts, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(text("›", 20, 0xFFB0BDD1, true, 0));

        card.addView(row);
        card.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                press(v);
                toast(name + "：发卡逻辑还没接喵");
            }
        });
        return card;
    }

    private LinearLayout buildMediaTab() {
        LinearLayout box = new LinearLayout(ctx);
        box.setOrientation(LinearLayout.VERTICAL);

        LinearLayout head = new LinearLayout(ctx);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.setPadding(dp(4), 0, dp(4), 0);
        head.addView(text("伪造素材", 15, 0xFF43587A, true, 0),
                new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        final TextView count = text("", 11, 0xFF8A9CB5, false, 0);
        head.addView(count);
        box.addView(head, matchWrap(dp(6)));

        TextView note = text("长按聊天里的图片 / 表情 / 语音 / 卡片 →「存素材」", 11, 0xFF8A9CB5, false, 0);
        note.setPadding(dp(4), 0, dp(4), dp(8));
        box.addView(note, matchWrap(dp(2)));

        final LinearLayout mBox = new LinearLayout(ctx);
        mBox.setOrientation(LinearLayout.VERTICAL);
        box.addView(mBox, matchWrap(dp(9)));
        renderMedia(mBox, count);
        return box;
    }

    private void renderMedia(final LinearLayout mBox, final TextView count) {
        mBox.removeAllViews();
        final java.util.List<String[]> lib =
                com.cathelperawa.owo.app.proto.MediaVault.all(ctx);
        count.setText(lib.size() + " 条");
        if (lib.isEmpty()) {
            LinearLayout empty = whiteCard(dp(18), 0x8CFFFFFF);
            empty.setPadding(dp(14), dp(18), dp(14), dp(18));
            TextView t1 = text("还没有素材", 13, 0xFF8A9CB5, true, dp(3));
            t1.setGravity(Gravity.CENTER);
            empty.addView(t1);
            TextView t2 = text("长按聊天里的图片 / 表情 → 存素材", 11, 0xFFA9B7CA, false, 0);
            t2.setGravity(Gravity.CENTER);
            empty.addView(t2);
            mBox.addView(empty, matchWrap(dp(2)));
            return;
        }
        for (int i = 0; i < lib.size(); i++) {
            final int idx = i;
            final String[] it = lib.get(i);
            String type = it.length > 2 ? it[2] : "";
            String typeName = com.cathelperawa.owo.app.proto.MediaVault.typeName(type);

            LinearLayout row = whiteCard(dp(16), 0x8CFFFFFF);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(14), dp(10), dp(10), dp(10));

            LinearLayout texts = new LinearLayout(ctx);
            texts.setOrientation(LinearLayout.VERTICAL);
            texts.addView(text(it[0], 13, 0xFF43587A, true, 0));
            texts.addView(text(typeName + " · " + (it[1] == null ? 0 : it[1].length())
                    + " 字符", 10, 0xFF8A9CB5, false, 0));
            row.addView(texts, new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            TextView del = pill("删除", 0xFFE0607A);
            ((GradientDrawable) del.getBackground()).setColor(0xFFFFEFF3);
            del.setPadding(dp(10), dp(5), dp(10), dp(5));
            del.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    com.cathelperawa.owo.app.proto.MediaVault.remove(ctx, idx);
                    renderMedia(mBox, count);
                }
            });
            row.addView(del);

            row.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    CatSheet.input(ctx, "改个名字", it[0], it[0], new CatSheet.OnText() {
                        @Override public void run(String t) {
                            com.cathelperawa.owo.app.proto.MediaVault.rename(ctx, idx, t);
                            renderMedia(mBox, count);
                        }
                    });
                }
            });
            mBox.addView(row, matchWrap(dp(6)));
        }
    }

    private void renderTemplates(final LinearLayout tplBox, final TextView count,
                                 final android.widget.EditText code, final View codeCard) {
        tplBox.removeAllViews();
        final java.util.List<com.cathelperawa.owo.app.core.TemplateStore.Item> list =
                com.cathelperawa.owo.app.core.TemplateStore.all(ctx);
        count.setText(list.size() + " 条");

        if (list.isEmpty()) {
            LinearLayout empty = whiteCard(dp(18), 0x8CFFFFFF);
            empty.setPadding(dp(14), dp(20), dp(14), dp(20));
            TextView t1 = text("还没有模板", 13, 0xFF8A9CB5, true, dp(3));
            t1.setGravity(Gravity.CENTER);
            empty.addView(t1);
            TextView t2 = text("长按聊天里的消息 → 提取 → 存入模板", 11, 0xFFA9B7CA, false, 0);
            t2.setGravity(Gravity.CENTER);
            empty.addView(t2);
            tplBox.addView(empty, matchWrap(dp(2)));
            return;
        }

        for (int i = 0; i < list.size(); i++) {
            final com.cathelperawa.owo.app.core.TemplateStore.Item it = list.get(i);

            LinearLayout row = whiteCard(dp(16), 0x8CFFFFFF);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(14), dp(10), dp(10), dp(10));

            LinearLayout texts = new LinearLayout(ctx);
            texts.setOrientation(LinearLayout.VERTICAL);
            texts.addView(text(it.name, 13, 0xFF43587A, true, 0));
            texts.addView(text((it.mode == 1 ? "PB 工具" : "卡片代码")
                    + " · " + it.content.length() + " 字", 10, 0xFF8A9CB5, false, 0));
            row.addView(texts, new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            TextView del = pill("删除", 0xFFE0607A);
            ((GradientDrawable) del.getBackground()).setColor(0xFFFFEFF3);
            del.setPadding(dp(10), dp(5), dp(10), dp(5));
            del.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    com.cathelperawa.owo.app.core.TemplateStore.remove(ctx, it.name);
                    toast("已删除：" + it.name);
                    renderTemplates(tplBox, count, code, codeCard);
                }
            });
            row.addView(del);

            final View rowView = row;
            row.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    code.setText(it.content);
                    press(rowView);
                    toast("已载入到代码框：" + it.name);
                    scrollIntoView(codeCard, rowView);
                }
            });
            tplBox.addView(row, matchWrap(dp(6)));
        }
    }

    

    
    private byte[] decodeInput(String in, int format) {
        try {
            String t = in.trim();
            if (format == 2) return encodePb(t);
            if (format == 1) {
                return android.util.Base64.decode(t.replaceAll("\\s+", ""),
                        android.util.Base64.DEFAULT);
            }
            
            String h = t.replaceAll("0x", "").replaceAll("[^0-9a-fA-F]", "");
            if (h.length() == 0 || h.length() % 2 != 0) return null;
            byte[] out = new byte[h.length() / 2];
            for (int i = 0; i < out.length; i++) {
                out[i] = (byte) Integer.parseInt(h.substring(i * 2, i * 2 + 2), 16);
            }
            return out;
        } catch (Throwable t) {
            return null;
        }
    }

    private String decodePb(byte[] raw) {
        try {
            byte[] body = stripWup(raw);
            com.cathelperawa.owo.app.ovo.FunProtoData pd =
                    new com.cathelperawa.owo.app.ovo.FunProtoData();
            pd.fromBytes(body);
            org.json.JSONObject o = pd.toJSON();
            if (o.length() == 0) return "（解不出字段，可能不是 protobuf）";
            return o.toString(2);
        } catch (Throwable t) {
            return "解码失败：不是合法的 protobuf 字节";
        }
    }

    private static boolean isJsonText(String s) {
        if (s == null) return false;
        String t = s.trim();
        return t.startsWith("{") || t.startsWith("[");
    }

    private byte[] encodePb(String json) {
        try {
            String t = json.trim();
            if (!t.startsWith("{")) return null;
            org.json.JSONObject o = new org.json.JSONObject(t);
            com.cathelperawa.owo.app.ovo.FunProtoData pd =
                    new com.cathelperawa.owo.app.ovo.FunProtoData();
            pd.fromJSON(o);
            return pd.toBytes();
        } catch (Throwable t) {
            return null;
        }
    }

    private static byte[] stripWup(byte[] b) {
        try {
            if (b == null || b.length < 5) return b;
            long len = ((long) (b[0] & 0xFF) << 24) | ((b[1] & 0xFF) << 16)
                    | ((b[2] & 0xFF) << 8) | (b[3] & 0xFF);
            if (len == b.length - 4) {
                byte[] r = new byte[b.length - 4];
                System.arraycopy(b, 4, r, 0, r.length);
                return r;
            }
        } catch (Throwable ignore) {
        }
        return b;
    }

    private static String hexOf(byte[] b) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < b.length; i++) {
            sb.append(String.format("%02X", b[i] & 0xFF));
            if (i % 16 == 15) sb.append('\n');
            else if (i % 4 == 3) sb.append(' ');
        }
        return sb.toString().trim();
    }

    

    
    private EditText multiEdit(String hint, int lines) {
        EditText et = new EditText(ctx);
        et.setHint(hint);
        et.setTextSize(12);
        et.setTextColor(0xFF43587A);
        et.setHintTextColor(0xFFB3C0D2);
        et.setTypeface(Typeface.MONOSPACE);
        et.setGravity(Gravity.TOP | Gravity.LEFT);
        et.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        et.setMinLines(lines);
        et.setMaxLines(lines + 6);
        et.setPadding(dp(12), dp(11), dp(12), dp(11));
        GradientDrawable g = new GradientDrawable();
        g.setColor(0xF2FFFFFF);
        g.setCornerRadius(dp(12));
        g.setStroke(dp(1), 0x55C9D6E8);
        et.setBackgroundDrawable(g);
        UiKit.innerScroll(et);
        return et;
    }

    private LinearLayout hintCard(String title, String desc) {
        LinearLayout card = whiteCard(dp(18), 0xE8F1FBFF);
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        LinearLayout head = new LinearLayout(ctx);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        TextView dot = pill("i", 0xFF7FA5E6);
        ((GradientDrawable) dot.getBackground()).setColor(0xF2FFFFFF);
        head.addView(dot, wrapWrap());
        TextView t = text(title, 13, 0xFF43587A, true, 0);
        t.setPadding(dp(7), 0, 0, 0);
        head.addView(t);
        card.addView(head, matchWrap(dp(5)));
        card.addView(text(desc, 11, 0xFF8496AE, false, 0));
        return card;
    }

    private TextView solidButton(String label) {
        TextView tv = new TextView(ctx);
        tv.setText(label);
        tv.setTextSize(14);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setTextColor(0xFFFFFFFF);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(dp(4), dp(12), dp(4), dp(12));
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(dp(16));
        g.setColor(0xFFF07AAE);
        g.setStroke(dp(1), 0xFFFF9FCB);
        tv.setBackgroundDrawable(g);
        tv.setElevation(dp(3));
        press(tv);
        return tv;
    }

    private TextView ghostButton(String label) {
        TextView tv = new TextView(ctx);
        tv.setText(label);
        tv.setTextSize(13);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setTextColor(0xFF7FA5E6);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(dp(4), dp(10), dp(4), dp(10));
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(dp(14));
        g.setColor(0x99FFFFFF);
        g.setStroke(dp(1), 0x55C9D6E8);
        tv.setBackgroundDrawable(g);
        press(tv);
        return tv;
    }

    private void notWired() {
        toast("界面先给你看，开关还等接线喵～");
    }
}
