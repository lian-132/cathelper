package com.cathelperawa.owo.app.ui;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public final class FeaturePage extends Page {

    private com.cathelperawa.owo.app.core.FeatureStatus statusOf(String key) {
        try {
            for (com.cathelperawa.owo.app.core.HookItemBase it
                    : com.cathelperawa.owo.app.core.HookItemFactory.all()) {
                if (it == null) continue;
                if (key.equals(it.switchKey())) {
                    com.cathelperawa.owo.app.core.FeatureReport r = it.report();
                    return r == null ? null : r.status();
                }
            }
        } catch (Throwable ignore) {
        }
        return null;
    }

    private static final String[][] CHAT_ITEMS = {
            {"chat_revoke", "防撤回", "拦截撤回指令并提示"},
            {"chat_repeat", "长按复读", "长按消息出现「复读」按钮"},
            {"chat_customdice", "自定义点数", "发骰子/猜拳时自选结果"},
            {"chat_emojiimg", "表情看图", "以图片的方式打开表情"},
            {"chat_flashpic", "闪照破解", "以图片的方式打开闪照"},
            {"chat_edittext", "改本地文本", "长按消息改显示文本（仅本地）"},

            {"chat_noat", "去除回复自动艾特", "回复消息时不再自动 @ 对方"},
            {"chat_showat", "显示艾特对象", "群聊里把 @ 的对象显示出来"},
            {"chat_mixsend", "文字表情复合消息", "文字和表情可以一起发"},

            {"chat_picsummary", "图片外显文字", "点开设置发图时的外显"},

            {"chat_clockin", "群打卡", "每天到点自动打卡，选好群就行"},

            {"chat_msgtime", "显示消息时间", "每条消息下方显示收到时间，点开调格式和颜色"},
    };

    private static final String[][] REDPACKET_ITEMS = {
            {"rp_manual", "手动打开红包", "为长按菜单添加打开红包按钮"},
            {"rp_auto", "自动领取红包", "点击可设置参数，回复语支持脚本图文消息"},
    };

    private final String page;
    private final String titleText;

    public FeaturePage(Host host, String page, String titleText) {
        super(host);
        this.page = page;
        this.titleText = titleText;
    }

    @Override
    public View build() {
        final boolean isChat = PAGE_CHAT.equals(page);
        String[][] tmpItems = isChat ? CHAT_ITEMS : REDPACKET_ITEMS;

        if (com.cathelperawa.owo.app.core.UIScope.isQQ()) {
            java.util.ArrayList<String[]> keep = new java.util.ArrayList<String[]>();
            for (String[] it : tmpItems) {
                if ("chat_mixsend".equals(it[0]) || "chat_clockin".equals(it[0])) continue;
                keep.add(it);
            }
            tmpItems = keep.toArray(new String[0][]);
        }
        final String[][] items = tmpItems;

        String[] arts = isChat
                ? new String[]{"c_revoke", "c_repeat", "c_dice", "c_emoji", "c_flash", "c_edittext",
                               "p_noat", "p_showat", "p_mix", "p_picsummary", "c_clockin", "c_msgtime"}
                : new String[]{"art4", "art5"};

        View[] out = new View[3];

        LinearLayout root = pageRoot(titleText, "猫娘助手 · " + titleText, out, true, 12);

        FrameLayout hero = new FrameLayout(ctx);
        GradientDrawable hs = new GradientDrawable();
        hs.setColor(0xFFFFFFFF);
        hs.setCornerRadius(dp(26));
        hs.setStroke(dp(1), 0x77FFFFFF);
        hero.setBackgroundDrawable(hs);
        hero.setClipToOutline(true);
        hero.setElevation(dp(7));
        Bitmap hb = image(isChat ? "h_chat" : "h_rp");
        if (hb != null) {
            ImageView iv = new ImageView(ctx);
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            iv.setImageBitmap(hb);
            hero.addView(iv, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, dp(150)));
        }
        View hshade = new View(ctx);
        hshade.setBackgroundDrawable(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0x00FFFFFF, 0x99FFFFFF, 0xFAFFFFFF}));
        hero.addView(hshade, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, dp(100), Gravity.BOTTOM));

        LinearLayout ht = new LinearLayout(ctx);
        ht.setOrientation(LinearLayout.VERTICAL);
        ht.setPadding(dp(16), 0, dp(16), dp(14));

        int onCount = 0;
        for (String[] it : items) if (swOn(it[0])) onCount++;
        final String heroTxt = onCount == 0 ? "全部关闭"
                : (onCount == items.length ? "已全部开启" : "已开启 " + onCount + " / " + items.length);
        final TextView heroState = pill(heroTxt, onCount == 0 ? 0xFF7FA5E6 : 0xFFF07CA8);
        LinearLayout hw = new LinearLayout(ctx);
        hw.setOrientation(LinearLayout.HORIZONTAL);
        hw.addView(heroState, wrapWrap());
        ht.addView(hw, matchWrap(dp(7)));
        ht.addView(text(titleText, 20, 0xFF4F6485, true, dp(3)));
        ht.addView(text(isChat ? ("让 " + com.cathelperawa.owo.app.core.Host.name() + " 聊天更顺手") : "让抢红包更快一步",
                12, 0xFF8A9CB5, false, 0));
        hero.addView(ht, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM));
        root.addView(hero, matchWrap(dp(11)));

        for (int i = 0; i < items.length; i++) {
            final String key = items[i][0];
            final String name = items[i][1];
            final String descText = items[i][2];
            final String artKey = arts[Math.min(i, arts.length - 1)];

            FrameLayout card = whiteFrame(dp(20), 0x8CFFFFFF);
            card.setClipToOutline(true);
            Bitmap cb = image(artKey);
            if (cb != null) {
                ImageView iv = new ImageView(ctx);
                iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                iv.setImageBitmap(cb);
                card.addView(iv, new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT, dp(78)));
                View sh = new View(ctx);
                sh.setBackgroundDrawable(new GradientDrawable(
                        GradientDrawable.Orientation.LEFT_RIGHT,
                        new int[]{0xF7FFFFFF, 0xA6FFFFFF, 0x33FFFFFF}));
                card.addView(sh, new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT, dp(78)));
            }
            LinearLayout ct = new LinearLayout(ctx);
            ct.setOrientation(LinearLayout.VERTICAL);
            ct.addView(text(name, 16, 0xFF43587A, true, dp(2)));
            ct.addView(text(descText, 11, 0xFF8496AE, false, 0));

            try {
                com.cathelperawa.owo.app.core.FeatureStatus st = statusOf(key);
                if (st != null && (st == com.cathelperawa.owo.app.core.FeatureStatus.FAILED
                        || st == com.cathelperawa.owo.app.core.FeatureStatus.ERROR
                        || st == com.cathelperawa.owo.app.core.FeatureStatus.PARTIAL)) {
                    ct.addView(text("⚠ " + st.label, 11, st.color, true, dp(2)));
                }
            } catch (Throwable ignore) {
            }
            FrameLayout.LayoutParams ctp = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.START | Gravity.CENTER_VERTICAL);
            ctp.leftMargin = dp(14);
            card.addView(ct, ctp);

            final SwitchBox sw = new SwitchBox(ctx, swOn(key));
            FrameLayout.LayoutParams swp = new FrameLayout.LayoutParams(
                    dp(52), dp(30), Gravity.END | Gravity.CENTER_VERTICAL);
            swp.rightMargin = dp(13);
            card.addView(sw, swp);

            host.bindSwitch(key, sw, new Runnable() {
                @Override public void run() { refreshHero(heroState, items); }
            });

            final boolean isPicSummary = "chat_picsummary".equals(key);

            final boolean isRedPacket = "rp_manual".equals(key) || "rp_auto".equals(key);

            final boolean isClockIn = "chat_clockin".equals(key);

            final boolean isMsgTime = "chat_msgtime".equals(key);
            final LinearLayout holder = new LinearLayout(ctx);
            holder.setOrientation(LinearLayout.VERTICAL);
            final PicPanel pp = isPicSummary ? buildPicSummaryPanel() : null;
            final boolean[] open = {false};

            if (isPicSummary) {
                sw.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        boolean now = !swOn(key);
                        swSet(key, now);
                        sw.setChecked(now);
                        press(v);
                        refreshHero(heroState, items);
                        toast(now ? "图片外显 开启喵～记得填文字" : "图片外显 关闭喵～");

                        if (now && !open[0]) togglePicPanel(pp, open, root, true);
                        else updatePicPanelState(pp);
                    }
                });
                card.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        press(v);
                        togglePicPanel(pp, open, root, !open[0]);
                    }
                });
                holder.addView(card);
                LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                plp.topMargin = dp(8);
                holder.addView(pp.view, plp);
                root.addView(holder, matchWrap(dp(11)));
            } else if (isRedPacket || isClockIn || isMsgTime) {
                final String goPage = isMsgTime ? PAGE_MSGTIME
                        : (isClockIn ? PAGE_CLOCKIN
                        : ("rp_auto".equals(key) ? PAGE_RP_AUTO : PAGE_RP_MANUAL));
                final String modeName = name;
                sw.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        boolean now = !swOn(key);
                        swSet(key, now);
                        sw.setChecked(now);
                        press(v);
                        refreshHero(heroState, items);
                        toast(now ? (modeName + " 开启喵～") : (modeName + " 关闭喵～"));
                    }
                });
                card.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        press(v);
                        host.open(goPage, modeName);
                    }
                });
                root.addView(card, matchWrap(dp(11)));
            } else {
                card.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        boolean now = !swOn(key);
                        swSet(key, now);
                        sw.setChecked(now);
                        press(v);
                        refreshHero(heroState, items);
                        toast(now ? (name + " 开启喵～") : (name + " 关闭喵～"));
                    }
                });
                root.addView(card, matchWrap(dp(11)));
            }
        }

        root.setAlpha(0f);
        root.setTranslationY(dp(16));
        root.animate().alpha(1f).translationY(0f).setDuration(420)
                .setInterpolator(new android.view.animation.DecelerateInterpolator()).start();

        return out[0];
    }

    private static final class PicPanel {
        LinearLayout view;
        EditText et;
        TextView state;
    }

    private PicPanel buildPicSummaryPanel() {
        PicPanel pp = new PicPanel();

        LinearLayout panel = new LinearLayout(ctx);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(16), dp(6), dp(16), dp(12));
        panel.setVisibility(View.GONE);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xE6FFFFFF);
        bg.setCornerRadius(dp(18));
        bg.setStroke(Math.max(1, dp(1)), 0x99FFFFFF);
        panel.setBackgroundDrawable(bg);
        try { panel.setElevation(dp(2)); } catch (Throwable ignore) {}

        final EditText et = new EditText(ctx);
        et.setText(cfgGetS("picsummary_text", ""));
        et.setHint("点这里输入外显文字…");
        et.setHintTextColor(0xFFA9BBD2);
        et.setTextSize(14);
        et.setTextColor(0xFF43587A);
        et.setSingleLine(true);
        et.setBackgroundDrawable(null);
        et.setPadding(dp(2), dp(12), dp(2), dp(10));
        panel.addView(et, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        View underline = new View(ctx);
        GradientDrawable ug = new GradientDrawable();
        ug.setColor(0x55F07CA8);
        underline.setBackgroundDrawable(ug);
        panel.addView(underline, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, Math.max(1, dp(1))));

        final TextView state = text("", 10, 0xFF97A9C0, false, 0);
        state.setPadding(0, dp(8), 0, 0);
        panel.addView(state);

        pp.view = panel;
        pp.et = et;
        pp.state = state;
        updatePicPanelState(pp);

        final android.os.Handler h = new android.os.Handler(android.os.Looper.getMainLooper());
        final Runnable[] save = new Runnable[1];
        save[0] = new Runnable() {
            @Override public void run() {
                cfgSetS("picsummary_text", et.getText().toString().trim());
                updatePicPanelState(pp);
            }
        };
        et.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(android.text.Editable s) {
                h.removeCallbacks(save[0]);
                h.postDelayed(save[0], 400);
            }
        });

        panel.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                et.requestFocus();
                showKeyboard(et);
            }
        });

        final long[] lastAuto = {0};
        panel.getViewTreeObserver().addOnGlobalLayoutListener(
                new android.view.ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override public void onGlobalLayout() {
                        if (!et.hasFocus() || panel.getVisibility() != View.VISIBLE) return;
                        long now = System.currentTimeMillis();
                        if (now - lastAuto[0] < 350) return;
                        lastAuto[0] = now;
                        scrollIntoView(panel, panel);
                    }
                });

        et.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override public void onFocusChange(View v, boolean has) {
                if (!has) return;
                panel.postDelayed(new Runnable() {
                    @Override public void run() { scrollIntoView(panel, panel); }
                }, 340);
            }
        });
        return pp;
    }

    private void updatePicPanelState(PicPanel pp) {
        if (pp == null || pp.state == null) return;
        String t = pp.et.getText().toString().trim();
        boolean on = swOn("chat_picsummary");
        if (!on) {
            pp.state.setTextColor(0xFFC0909E);
            pp.state.setText(t.length() == 0
                    ? "上面的开关还关着 · 打开后发图才会改"
                    : "已填「" + brief(t) + "」· 打开上面的开关才生效");
        } else if (t.length() == 0) {
            pp.state.setTextColor(0xFF97A9C0);
            pp.state.setText("留空 = 默认「[图片]」· 改完直接发图生效");
        } else {
            pp.state.setTextColor(0xFF7FB98D);
            pp.state.setText("✓ 已保存 · " + t.length() + " 字"
                    + (t.length() > 30 ? "（偏长可能被截断）" : ""));
        }
    }

    private static String brief(String s) {
        return s.length() <= 10 ? s : s.substring(0, 10) + "…";
    }

    private void togglePicPanel(final PicPanel pp, final boolean[] open,
                                final View pageRoot, final boolean wantOpen) {
        if (pp == null) return;
        if (wantOpen == open[0]) return;
        open[0] = wantOpen;

        final LinearLayout panel = pp.view;
        final android.view.ViewGroup.LayoutParams raw = panel.getLayoutParams();
        if (!(raw instanceof LinearLayout.LayoutParams)) return;
        final LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) raw;

        int w = 0;
        if (panel.getParent() instanceof View) w = ((View) panel.getParent()).getWidth();
        if (w <= 0) w = ctx.getResources().getDisplayMetrics().widthPixels - dp(72);
        lp.height = LinearLayout.LayoutParams.WRAP_CONTENT;
        panel.setLayoutParams(lp);
        panel.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        final int full = Math.max(1, panel.getMeasuredHeight());

        panel.setVisibility(View.VISIBLE);
        lp.height = wantOpen ? 0 : full;
        panel.setLayoutParams(lp);

        if (wantOpen) updatePicPanelState(pp);

        android.animation.ValueAnimator va = android.animation.ValueAnimator.ofInt(
                lp.height, wantOpen ? full : 0);
        va.setDuration(200);
        va.addUpdateListener(new android.animation.ValueAnimator.AnimatorUpdateListener() {
            @Override public void onAnimationUpdate(android.animation.ValueAnimator a) {
                LinearLayout.LayoutParams p2 = (LinearLayout.LayoutParams) panel.getLayoutParams();
                p2.height = (Integer) a.getAnimatedValue();
                panel.setLayoutParams(p2);
            }
        });
        va.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(android.animation.Animator a) {
                LinearLayout.LayoutParams p2 = (LinearLayout.LayoutParams) panel.getLayoutParams();
                p2.height = LinearLayout.LayoutParams.WRAP_CONTENT;
                panel.setLayoutParams(p2);
                if (wantOpen) {

                    scrollIntoView(panel, pageRoot);

                    panel.postDelayed(new Runnable() {
                        @Override public void run() { scrollIntoView(panel, pageRoot); }
                    }, 320);
                } else {
                    hideKeyboard(pp.et);
                    pp.et.clearFocus();
                    panel.setVisibility(View.GONE);
                }
            }
        });
        va.start();
    }

    private void refreshHero(TextView heroState, String[][] items) {
        int oc = 0;
        for (String[] it : items) if (swOn(it[0])) oc++;
        heroState.setText(oc == 0 ? "全部关闭"
                : (oc == items.length ? "已全部开启" : "已开启 " + oc + " / " + items.length));
        ((GradientDrawable) heroState.getBackground())
                .setColor(oc == 0 ? 0xFFFFFFFF : 0xFFFFF0F6);
        heroState.setTextColor(oc == 0 ? 0xFF7FA5E6 : 0xFFF07CA8);
    }
}
