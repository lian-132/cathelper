package com.cathelperawa.owo.app.ai;

import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.cathelperawa.owo.app.ui.Page;

import java.util.List;

public final class AiProvidersPage extends Page {

    private static final int C_MAIN = 0xFF3C4B60;
    private static final int C_SUB = 0xFF8A9BB0;
    private static final int C_PINK = 0xFFE85D9C;

    private static final int[] AVATAR_COLORS = {
            0xFF9EC6F5, 0xFFF5B7CE, 0xFFA9D9C3, 0xFFC7B8F0, 0xFFF5CFA0
    };

    private LinearLayout body;

    private LinearLayout content;

    public AiProvidersPage(Host host) {
        super(host);
    }

    @Override
    public View build() {
        View[] out = new View[2];
        body = pageRoot("模型提供商", "配好之后 AI 才能说话", out, true, 13, "bg_ai", 0x66);
        content = new LinearLayout(ctx);
        content.setOrientation(LinearLayout.VERTICAL);
        body.addView(content, matchWrap(0));
        render();
        return out[0];
    }

    private void render() {

        content.removeAllViews();
        List<AiProvider> list = AiProvider.loadAll();

        content.addView(headerCard(list), matchWrap(dp(14)));

        if (list.isEmpty()) {
            content.addView(emptyCard(), matchWrap(dp(14)));
        } else {
            for (int i = 0; i < list.size(); i++) {
                content.addView(providerCard(list.get(i), i), matchWrap(dp(10)));
            }
        }

        content.addView(searchEntry(), matchWrap(dp(14)));

        content.addView(ttsEntry(), matchWrap(dp(14)));
    }

    private View ttsEntry() {
        LinearLayout card = whiteCard(dp(18), 0xEEFFFFFF);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(18), dp(16), dp(14), dp(16));
        LinearLayout txt = new LinearLayout(ctx);
        txt.setOrientation(LinearLayout.VERTICAL);
        txt.addView(text("语音合成（TTS）", 16, C_MAIN, true, dp(3)), matchWrap(0));
        boolean api = AiConfig.ttsUseApi();
        txt.addView(text(api
                        ? AiConfig.ttsModel() + " · 音色 " + AiConfig.ttsVoice()
                        : "内置免费通道 · 未填密钥",
                11, C_SUB, false, 0), matchWrap(0));
        card.addView(txt, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        TextView tag = pill(api ? "自建" : "免费", api ? 0x1F3FA96A : 0x1F8A9BB0);
        tag.setTextColor(api ? 0xFF3FA96A : C_SUB);
        card.addView(tag, wrapWrap());
        card.addView(text("\u203a", 20, 0xFFB6C1D0, false, 0));
        press(card);
        card.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                host.pushPage(new AiTtsPage(host).build());
            }
        });
        return card;
    }

    private View searchEntry() {
        LinearLayout card = whiteCard(dp(18), 0xEEFFFFFF);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(18), dp(16), dp(14), dp(16));

        LinearLayout txt = new LinearLayout(ctx);
        txt.setOrientation(LinearLayout.VERTICAL);
        txt.addView(text("搜索接口", 16, C_MAIN, true, dp(3)), matchWrap(0));
        txt.addView(text(AiSearch.statusText(), 11, C_SUB, false, 0), matchWrap(0));
        card.addView(txt, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView tag = pill(AiSearch.enabled() ? "已开启" : "未开启",
                AiSearch.enabled() ? 0x1F3FA96A : 0x1F8A9BB0);
        tag.setTextColor(AiSearch.enabled() ? 0xFF3FA96A : C_SUB);
        card.addView(tag, wrapWrap());
        card.addView(text("\u203a", 20, 0xFFB6C1D0, false, 0));

        press(card);
        card.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                host.pushPage(new AiSearchSetPage(host).build());
            }
        });
        return card;
    }

    private View headerCard(List<AiProvider> list) {
        LinearLayout card = whiteCard(dp(22), 0xF2FFFFFF);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(18), dp(16), dp(14), dp(16));

        int on = 0;
        for (int i = 0; i < list.size(); i++) if (list.get(i).enabled) on++;

        LinearLayout txt = new LinearLayout(ctx);
        txt.setOrientation(LinearLayout.VERTICAL);
        txt.addView(text("模型提供商", 17, C_MAIN, true, dp(4)), matchWrap(0));
        txt.addView(text(list.isEmpty() ? "还没有配置"
                        : (list.size() + " 个 · " + on + " 个启用"),
                12, C_SUB, false, 0), matchWrap(0));
        card.addView(txt, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView plus = new TextView(ctx);
        plus.setText("+");
        plus.setTextSize(22);
        plus.setTextColor(0xFFFFFFFF);
        plus.setGravity(Gravity.CENTER);
        GradientDrawable pg = new GradientDrawable();
        pg.setShape(GradientDrawable.OVAL);
        pg.setColor(C_PINK);
        plus.setBackgroundDrawable(pg);
        press(plus);
        plus.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                openEdit(AiProvider.create("新提供商"));
            }
        });
        card.addView(plus, new LinearLayout.LayoutParams(dp(40), dp(40)));

        return card;
    }

    private View emptyCard() {
        LinearLayout card = whiteCard(dp(20), 0xE6FFFFFF);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setPadding(dp(18), dp(26), dp(18), dp(26));

        Bitmap cat = image("cat_toast");
        if (cat != null) {
            ImageView iv = new ImageView(ctx);
            iv.setImageBitmap(cat);
            card.addView(iv, new LinearLayout.LayoutParams(dp(62), dp(62)));
        }

        TextView t1 = text("还没有配置提供商", 14, C_MAIN, true, 0);
        t1.setGravity(Gravity.CENTER);
        t1.setPadding(0, dp(12), 0, dp(5));
        card.addView(t1, matchWrap(0));

        TextView t2 = text("点右上角的 + 添加一个吧", 12, C_SUB, false, 0);
        t2.setGravity(Gravity.CENTER);
        card.addView(t2, matchWrap(0));

        return card;
    }

    private View providerCard(final AiProvider p, int index) {
        LinearLayout card = whiteCard(dp(18), 0xF2FFFFFF);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(14), dp(13), dp(14), dp(13));

        String n = p.displayName();
        TextView av = new TextView(ctx);
        av.setText(n.length() > 0 ? n.substring(0, 1).toUpperCase() : "?");
        av.setTextSize(16);
        av.setTextColor(0xFFFFFFFF);
        av.setGravity(Gravity.CENTER);
        GradientDrawable ag = new GradientDrawable();
        ag.setShape(GradientDrawable.OVAL);
        ag.setColor(AVATAR_COLORS[index % AVATAR_COLORS.length]);
        av.setBackgroundDrawable(ag);
        card.addView(av, new LinearLayout.LayoutParams(dp(40), dp(40)));

        LinearLayout txt = new LinearLayout(ctx);
        txt.setOrientation(LinearLayout.VERTICAL);
        txt.setPadding(dp(12), 0, dp(8), 0);
        txt.addView(text(p.displayName(), 15, C_MAIN, true, dp(3)), matchWrap(0));

        LinearLayout meta = new LinearLayout(ctx);
        meta.setOrientation(LinearLayout.HORIZONTAL);
        meta.setGravity(Gravity.CENTER_VERTICAL);

        View dot = new View(ctx);
        GradientDrawable dg = new GradientDrawable();
        dg.setShape(GradientDrawable.OVAL);
        dg.setColor(p.enabled ? 0xFF3FA96A : 0xFFC7CFDA);
        dot.setBackgroundDrawable(dg);
        meta.addView(dot, new LinearLayout.LayoutParams(dp(6), dp(6)));

        TextView sub = text(p.model == null || p.model.isEmpty() ? "未选模型" : p.model,
                11, C_SUB, false, 0);
        sub.setPadding(dp(6), 0, 0, 0);
        meta.addView(sub);

        txt.addView(meta, matchWrap(0));
        card.addView(txt, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        AiProvider cur = AiProvider.current();
        boolean isCurrent = cur != null && cur.id.equals(p.id);
        if (isCurrent) {
            TextView tag = new TextView(ctx);
            tag.setText("使用中");
            tag.setTextSize(10.5f);
            tag.setTextColor(0xFFD9538E);
            tag.setPadding(dp(8), dp(4), dp(8), dp(4));
            GradientDrawable tg = new GradientDrawable();
            tg.setCornerRadius(dp(9));
            tg.setColor(0x33E85D9C);
            tag.setBackgroundDrawable(tg);
            card.addView(tag);
        } else {
            card.addView(text("›", 20, 0xFFB6C1D0, false, 0));
        }

        press(card);
        card.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                openEdit(p);
            }
        });
        return card;
    }

    private void openEdit(AiProvider p) {
        host.pushPage(new AiProviderEditPage(host, p).build());
    }
}
