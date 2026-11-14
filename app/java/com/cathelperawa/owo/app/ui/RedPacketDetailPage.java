package com.cathelperawa.owo.app.ui;

import android.graphics.Bitmap;
import com.cathelperawa.owo.app.core.Bg;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.HashMap;
import java.util.Map;

public final class RedPacketDetailPage extends Page {

    private static final String[][] RP_AUTO_ITEMS = {
            {"rp_average", "平均金额阈值", "低于该值不抢 · 单位「分」，0 = 不限"},
            {"rp_keywords", "屏蔽关键词", "标题含这些词就不抢 · 英文逗号分隔"},
            {"rp_replys",   "抢到后回复", "随机挑一条发出 · 英文逗号分隔"},
            {"rp_delay",    "随机延迟(ms)", "0 ~ 该值内随机等待，削弱机器特征"},
            {"rp_servlet",  "Servlet 类名", "留空=自动探测；换候选不用重编译"},
    };

    private static final String[][] RP_MANUAL_ITEMS = {
            {"rp_replys", "抢到后回复", "长按「开红包」成功后的回复语 · 英文逗号分隔"},
    };

    private final boolean auto;

    public RedPacketDetailPage(Host host, boolean auto) {
        super(host);
        this.auto = auto;
    }

    @Override
    public View build() {
        final String modeKey = auto ? "rp_auto" : "rp_manual";
        final String[][] rows = auto ? RP_AUTO_ITEMS : RP_MANUAL_ITEMS;
        final String heroArt = auto ? "rp_auto" : "rp_manual";
        final String titleText = auto ? "自动领取红包" : "手动打开红包";
        final String subText = auto ? "群里一有红包就抢先一步" : "长按消息 · 想开哪个点哪个";
        final String stateOn = auto ? "自动领取中" : "长按菜单已就绪";
        final String stateOff = auto ? "自动领取已关闭" : "长按项已隐藏";

        View[] out = new View[2];

        LinearLayout root = detailRoot(titleText, "红包设置 · " + titleText, out);

        FrameLayout hero = new FrameLayout(ctx);
        GradientDrawable hs = new GradientDrawable();
        hs.setColor(0xFFFFFFFF);
        hs.setCornerRadius(dp(26));
        hs.setStroke(dp(1), 0x77FFFFFF);
        hero.setBackgroundDrawable(hs);
        hero.setClipToOutline(true);
        hero.setElevation(dp(7));
        Bitmap hb = image(heroArt);
        if (hb != null) {
            ImageView iv = new ImageView(ctx);
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            iv.setImageBitmap(hb);
            hero.addView(iv, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, dp(160)));
        }
        View hshade = new View(ctx);
        hshade.setBackgroundDrawable(new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0x00FFFFFF, 0x99FFFFFF, 0xFBFFFFFF}));
        hero.addView(hshade, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, dp(106), Gravity.BOTTOM));

        LinearLayout ht = new LinearLayout(ctx);
        ht.setOrientation(LinearLayout.VERTICAL);
        ht.setPadding(dp(16), 0, dp(16), dp(14));
        final TextView heroState = pill(swOn(modeKey) ? stateOn : stateOff,
                swOn(modeKey) ? 0xFFF07CA8 : 0xFF7FA5E6);
        LinearLayout hw = new LinearLayout(ctx);
        hw.setOrientation(LinearLayout.HORIZONTAL);
        hw.addView(heroState, wrapWrap());
        ht.addView(hw, matchWrap(dp(7)));
        ht.addView(text(titleText, 20, 0xFF4F6485, true, dp(3)));
        ht.addView(text(subText, 12, 0xFF8A9CB5, false, 0));
        hero.addView(ht, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM));
        root.addView(hero, matchWrap(dp(11)));

        FrameLayout swCard = whiteFrame(dp(20), 0x8CFFFFFF);
        LinearLayout swText = new LinearLayout(ctx);
        swText.setOrientation(LinearLayout.VERTICAL);
        swText.setGravity(Gravity.CENTER_VERTICAL);
        swText.addView(text(auto ? "自动领取" : "长按菜单", 16, 0xFF43587A, true, dp(2)));
        swText.addView(text(auto ? "收到红包消息立即走领取流程" : "长按红包消息出现「开红包」",
                11, 0xFF8496AE, false, 0));
        FrameLayout.LayoutParams stp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.START | Gravity.CENTER_VERTICAL);
        stp.leftMargin = dp(14);
        swCard.addView(swText, stp);
        final SwitchBox swMain = new SwitchBox(ctx, swOn(modeKey));
        FrameLayout.LayoutParams swp = new FrameLayout.LayoutParams(
                dp(52), dp(30), Gravity.END | Gravity.CENTER_VERTICAL);
        swp.rightMargin = dp(13);
        swCard.addView(swMain, swp);

        host.bindSwitch(modeKey, swMain, new Runnable() {
            @Override public void run() {
                boolean on = swOn(modeKey);
                heroState.setText(on ? stateOn : stateOff);
                ((GradientDrawable) heroState.getBackground())
                        .setColor(on ? 0xFFFFF0F6 : 0xFFFFFFFF);
                heroState.setTextColor(on ? 0xFFF07CA8 : 0xFF7FA5E6);
            }
        });
        swCard.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                boolean now = !swOn(modeKey);
                swSet(modeKey, now);
                swMain.setChecked(now);
                press(v);
                heroState.setText(now ? stateOn : stateOff);
                ((GradientDrawable) heroState.getBackground())
                        .setColor(now ? 0xFFFFF0F6 : 0xFFFFFFFF);
                heroState.setTextColor(now ? 0xFFF07CA8 : 0xFF7FA5E6);
                toast(now ? (titleText + " 开启喵～") : (titleText + " 关闭喵～"));
            }
        });
        root.addView(swCard, matchFixed(70, 11));

        LinearLayout setCard = whiteCard(dp(22), 0x8CFFFFFF);
        setCard.setPadding(dp(16), dp(15), dp(16), dp(15));
        setCard.addView(text("参数设置", 16, 0xFF43587A, true, dp(11)));

        final Map<String, EditText> edits = new HashMap<String, EditText>();
        for (int i = 0; i < rows.length; i++) {
            String[] r = rows[i];
            setCard.addView(text(r[1], 13, 0xFF5F7392, true, dp(4)));
            EditText et = UiKit.pastelEdit(ctx, cfgGetS(r[0], ""), r[2]);
            setCard.addView(et, matchWrap(i == rows.length - 1 ? 0 : 10));
            edits.put(r[0], et);
        }
        final TextView saveHint = text("", 11, 0xFF7FB98D, false, 0);
        TextView saveBtn = pill("保存设置", 0xFFFFFFFF);
        saveBtn.setTextSize(13);
        saveBtn.setPadding(dp(20), dp(9), dp(20), dp(9));
        GradientDrawable sg = new GradientDrawable();
        sg.setColor(0xFFF07CA8);
        sg.setCornerRadius(dp(16));
        saveBtn.setBackgroundDrawable(sg);
        saveBtn.setGravity(Gravity.CENTER);
        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                press(v);
                for (Map.Entry<String, EditText> e : edits.entrySet()) {
                    cfgSetS(e.getKey(), e.getValue().getText().toString().trim());
                }
                saveHint.setText("✓ 已保存");
                toast("设置保存好啦喵～");
            }
        });
        LinearLayout saveRow = new LinearLayout(ctx);
        saveRow.setOrientation(LinearLayout.HORIZONTAL);
        saveRow.setGravity(Gravity.CENTER_VERTICAL);
        saveRow.addView(saveBtn, wrapWrap());
        LinearLayout.LayoutParams shp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        shp.leftMargin = dp(12);
        saveRow.addView(saveHint, shp);
        setCard.addView(saveRow, matchWrap(0));
        root.addView(setCard, matchWrap(dp(11)));

        if (auto) {
            root.addView(buildGroupCard(), matchWrap(dp(11)));
        }

        LinearLayout diagCard = whiteCard(dp(22), 0x8CFFFFFF);
        diagCard.setPadding(dp(16), dp(15), dp(16), dp(15));
        diagCard.addView(text("底座诊断", 16, 0xFF43587A, true, dp(4)));
        diagCard.addView(text("检查 pskey / hexGuid / Servlet 是否通", 11, 0xFF8496AE, false, dp(10)));
        final TextView probeOut = text("点下面按钮跑一次", 11, 0xFF6B7E99, false, 0);
        probeOut.setTextIsSelectable(true);
        probeOut.setTypeface(Typeface.MONOSPACE);
        TextView probeBtn = pill("运行探针", 0xFFFFFFFF);
        probeBtn.setTextSize(13);
        probeBtn.setPadding(dp(20), dp(9), dp(20), dp(9));
        GradientDrawable pg = new GradientDrawable();
        pg.setColor(0xFF7FA5E6);
        pg.setCornerRadius(dp(16));
        probeBtn.setBackgroundDrawable(pg);
        probeBtn.setGravity(Gravity.CENTER);
        probeBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                press(v);
                probeOut.setText("跑着呢…");
                Bg.run(new Runnable() {
                    @Override public void run() {
                        final String[] holder = new String[1];
                        try {
                            holder[0] = com.cathelperawa.owo.app.api.CatApi.redPacketProbe();
                        } catch (Throwable t) {
                            holder[0] = "探针异常: " + t;
                        }
                        activity().runOnUiThread(new Runnable() {
                            @Override public void run() { probeOut.setText(holder[0]); }
                        });
                    }
                }, "cat-probe");
            }
        });
        diagCard.addView(probeBtn, wrapWrap());
        diagCard.addView(probeOut, matchWrap(0));
        root.addView(diagCard, matchWrap(dp(11)));

        root.setAlpha(0f);
        root.setTranslationY(dp(16));
        root.animate().alpha(1f).translationY(0f).setDuration(420)
                .setInterpolator(new android.view.animation.DecelerateInterpolator()).start();

        return out[0];
    }

    private LinearLayout detailRoot(String titleText, String subText, View[] out) {

        return pageRoot(titleText, subText, out, true);
    }

    private View buildGroupCard() {
        LinearLayout card = whiteCard(dp(22), 0x8CFFFFFF);
        card.setPadding(dp(16), dp(15), dp(16), dp(15));
        card.addView(text("群聊开关", 16, 0xFF43587A, true, dp(4)));
        card.addView(text("按群单独开/关；收到过红包的群会自动出现在这里",
                11, 0xFF8496AE, false, dp(10)));

        final LinearLayout listBox = new LinearLayout(ctx);
        listBox.setOrientation(LinearLayout.VERTICAL);

        final TextView modeAll = pill("全部群默认开", 0xFF7FA5E6);
        final TextView modeWhite = pill("仅勾选的群", 0xFF7FA5E6);
        modeAll.setTextSize(12);
        modeWhite.setTextSize(12);
        modeAll.setPadding(dp(12), dp(7), dp(12), dp(7));
        modeWhite.setPadding(dp(12), dp(7), dp(12), dp(7));
        LinearLayout modeRow = new LinearLayout(ctx);
        modeRow.setOrientation(LinearLayout.HORIZONTAL);
        modeRow.setGravity(Gravity.CENTER_VERTICAL);
        modeRow.addView(modeAll, wrapWrap());
        LinearLayout.LayoutParams mp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        mp.leftMargin = dp(8);
        modeRow.addView(modeWhite, mp);
        card.addView(modeRow, matchWrap(dp(10)));

        final String[] filter = {""};
        android.widget.EditText search = UiKit.pastelEdit(ctx, "", "搜索群名或群号");
        search.setSingleLine(true);
        card.addView(search, matchWrap(dp(8)));

        final Runnable refresh = new Runnable() {
            @Override public void run() {
                boolean white = com.cathelperawa.owo.app.core.RpGroupRules.MODE_WHITE
                        .equals(com.cathelperawa.owo.app.core.RpGroupRules.mode(ctx));
                styleMode(modeAll, !white);
                styleMode(modeWhite, white);

                listBox.removeAllViews();
                java.util.List<com.cathelperawa.owo.app.core.RpGroupRules.Entry> es =
                        com.cathelperawa.owo.app.core.RpGroupRules.entries(ctx);
                if (es.isEmpty()) {
                    listBox.addView(text("正在从 TIM 读取群列表…\n读不到的话，收到一次红包后那个群也会自动出现。",
                            11, 0xFF9FB0C7, false, dp(6)));
                    return;
                }
                String kw = filter[0] == null ? "" : filter[0].trim().toLowerCase();
                int shown = 0;
                for (final com.cathelperawa.owo.app.core.RpGroupRules.Entry e : es) {
                    if (!kw.isEmpty()) {
                        String nm = e.name == null ? "" : e.name.toLowerCase();
                        if (!nm.contains(kw) && !e.uin.contains(kw)) continue;
                    }
                    shown++;
                    final boolean on = com.cathelperawa.owo.app.core.RpGroupRules.isAllowed(ctx, e.uin);
                    LinearLayout row = new LinearLayout(ctx);
                    row.setOrientation(LinearLayout.HORIZONTAL);
                    row.setGravity(Gravity.CENTER_VERTICAL);
                    row.setPadding(0, dp(6), 0, dp(6));

                    LinearLayout col = new LinearLayout(ctx);
                    col.setOrientation(LinearLayout.VERTICAL);
                    col.addView(text(e.displayName(), 13, 0xFF43587A, true, dp(1)));
                    col.addView(text(on ? "这个群自动领" : "这个群不领",
                            11, on ? 0xFF7FB98D : 0xFFB08A8A, false, 0));
                    row.addView(col, new LinearLayout.LayoutParams(0,
                            LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

                    final SwitchBox sw = new SwitchBox(ctx, on);
                    final boolean[] cur = {on};
                    sw.setOnClickListener(new View.OnClickListener() {
                        @Override public void onClick(View v) {
                            cur[0] = !cur[0];
                            sw.setChecked(cur[0]);
                            com.cathelperawa.owo.app.core.RpGroupRules.setState(ctx, e.uin, cur[0]);
                            press(v);
                            run();
                        }
                    });
                    row.addView(sw, new LinearLayout.LayoutParams(dp(52), dp(30)));

                    row.setOnLongClickListener(new View.OnLongClickListener() {
                        @Override public boolean onLongClick(View v) {
                            com.cathelperawa.owo.app.core.RpGroupRules.forget(ctx, e.uin);
                            toast("已移除 " + e.uin);
                            run();
                            return true;
                        }
                    });
                    listBox.addView(row);
                }
                if (shown == 0 && !es.isEmpty()) {
                    listBox.addView(text("没有匹配的群", 11, 0xFF9FB0C7, false, dp(6)));
                }
            }
        };

        search.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence cs, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence cs, int a, int b, int c) {}
            @Override public void afterTextChanged(android.text.Editable ed) {
                filter[0] = ed == null ? "" : ed.toString();
                refresh.run();
            }
        });

        modeAll.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                press(v);
                com.cathelperawa.owo.app.core.RpGroupRules.setMode(ctx,
                        com.cathelperawa.owo.app.core.RpGroupRules.MODE_ALL);
                refresh.run();
            }
        });
        modeWhite.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                press(v);
                com.cathelperawa.owo.app.core.RpGroupRules.setMode(ctx,
                        com.cathelperawa.owo.app.core.RpGroupRules.MODE_WHITE);
                refresh.run();
            }
        });

        card.addView(listBox, matchWrap(dp(8)));

        TextView addBtn = pill("手动添加群号", 0xFFFFFFFF);
        addBtn.setTextSize(12);
        addBtn.setPadding(dp(16), dp(8), dp(16), dp(8));
        GradientDrawable ag = new GradientDrawable();
        ag.setColor(0xFF7FA5E6);
        ag.setCornerRadius(dp(14));
        addBtn.setBackgroundDrawable(ag);
        addBtn.setGravity(Gravity.CENTER);
        addBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                press(v);
                CatDialog.input(ctx, "添加群号", "填群号（纯数字）", "", false,
                        new CatDialog.OnText() {
                            @Override public void run(String text) {
                                if (com.cathelperawa.owo.app.core.RpGroupRules.add(ctx, text, null)) {
                                    toast("已添加 " + text.trim());
                                    refresh.run();
                                } else {
                                    toast("群号不对喵…");
                                }
                            }
                        });
            }
        });
        card.addView(addBtn, matchWrap(0));

        Bg.run(new Runnable() {
            @Override public void run() {
                final int n = com.cathelperawa.owo.app.core.RpGroupRules.syncFromHost(ctx);
                if (n <= 0) return;
                new android.os.Handler(android.os.Looper.getMainLooper()).post(new Runnable() {
                    @Override public void run() { refresh.run(); }
                });
            }
        }, "rp-group-sync");

        refresh.run();
        return card;
    }

    private void styleMode(TextView v, boolean selected) {
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(dp(14));
        g.setColor(selected ? 0xFF7FA5E6 : 0x33FFFFFF);
        v.setBackgroundDrawable(g);
        v.setTextColor(selected ? 0xFFFFFFFF : 0xFF7FA5E6);
    }
}
