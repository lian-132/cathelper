package com.cathelperawa.owo.app.ui;

import android.content.Context;
import com.cathelperawa.owo.app.core.Bg;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.cathelperawa.owo.app.MsgStore;

import java.util.Timer;
import java.util.TimerTask;

public final class HomePage extends Page {

    private static final String[][] CAT_SWITCHES = {
            {"cat_total", "总开关", ""},
            {"cat_base", "基础猫娘化", ""},
            {"cat_spoiled", "撒娇模式", ""},
            {"cat_endmeow", "结尾加喵", ""},
            {"cat_emoticon", "加颜文字", ""},
            {"cat_cute", "随机卖萌", ""},
            {"cat_wave", "波浪号", ""},
            {"cat_catname", "称呼替换", ""},
            {"cat_mastername", "主人称呼", ""},
    };

    public HomePage(Host host) { super(host); }

    @Override
    public View build() {
        FrameLayout page = new FrameLayout(ctx);
        Bitmap bgBmp = bg();
        if (bgBmp != null) {
            ImageView bgv = new ImageView(ctx);
            bgv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            bgv.setImageBitmap(bgBmp);
            page.addView(bgv, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
            View veil = new View(ctx);
            veil.setBackgroundDrawable(new GradientDrawable(
                    GradientDrawable.Orientation.TL_BR,
                    new int[]{0xC7E4EFFF, 0xC7F4EBFF, 0xCCFFE9F1}));
            page.addView(veil, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        }

        final ScrollView scroll = new ScrollView(ctx);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.TRANSPARENT);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        scroll.setVerticalScrollBarEnabled(false);
        
        scroll.setId(0x7f0f0001);
        page.setBackgroundDrawable(new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{0xFFE4EFFF, 0xFFF4EBFF, 0xFFFFE9F1}));

        LinearLayout root = new LinearLayout(ctx);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(15), dp(44), dp(15), dp(26));
        scroll.addView(root, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.WRAP_CONTENT));

        LinearLayout topbar = new LinearLayout(ctx);
        topbar.setOrientation(LinearLayout.HORIZONTAL);
        topbar.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout tbTexts = new LinearLayout(ctx);
        tbTexts.setOrientation(LinearLayout.VERTICAL);
        
        org.json.JSONObject hst = com.cathelperawa.owo.app.core.Skin.style("home.header");
        int hTitleSize = hst.optInt("titleSize", 27);
        int hSubSize = hst.optInt("subSize", 12);
        tbTexts.addView(text(com.cathelperawa.owo.app.core.Skin.text("home.title", "猫娘助手"), hTitleSize,
                com.cathelperawa.owo.app.ui.Theme.parse(hst.optString("titleColor", ""), 0xFF6F86A8), true, dp(1)));
        tbTexts.addView(text(com.cathelperawa.owo.app.core.Skin.text("home.subtitle",
                        com.cathelperawa.owo.app.core.Host.isQQ() ? "QQ 增强 · 控制台" : "TIM 增强 · 控制台"), hSubSize,
                com.cathelperawa.owo.app.ui.Theme.parse(hst.optString("subColor", ""), 0xFF9FB0C7), false, 0));
        topbar.addView(tbTexts, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        
        FrameLayout refresh = new FrameLayout(ctx);
        GradientDrawable rbg = new GradientDrawable();
        rbg.setShape(GradientDrawable.OVAL);
        rbg.setColor(0xE6FFFFFF);
        refresh.setBackgroundDrawable(rbg);

        RefreshIcon glyph = new RefreshIcon(ctx);
        FrameLayout.LayoutParams glyphLp = new FrameLayout.LayoutParams(
                dp(22), dp(22), Gravity.CENTER);
        refresh.addView(glyph, glyphLp);

        topbar.addView(refresh, new LinearLayout.LayoutParams(
                "0".equals(hst.optString("showRefresh", "1")) ? 0 : dp(42),
                "0".equals(hst.optString("showRefresh", "1")) ? 0 : dp(42)));
        refresh.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                spin(v);
                confirmRestart(v.getContext());
            }
        });
        root.addView(topbar, matchWrap(dp(13)));

        
        org.json.JSONObject hstHero = com.cathelperawa.owo.app.core.Skin.style("home.hero");
        int hyRadius = hstHero.optInt("radius", 26);
        int hyImgH = hstHero.optInt("imageHeight", 178);
        int hyTitleSize = hstHero.optInt("titleSize", 21);
        int hyDescSize = hstHero.optInt("descSize", 12);
        boolean hyChips = !"0".equals(hstHero.optString("showChips", "1"));
        boolean hyGear = !"0".equals(hstHero.optString("showGear", "1"));
        boolean hyDots = !"0".equals(hstHero.optString("showDots", "1"));
        FrameLayout hero = new FrameLayout(ctx);
        GradientDrawable heroStroke = new GradientDrawable();
        heroStroke.setColor(0xFFFFFFFF);
        heroStroke.setCornerRadius(dp(hyRadius));
        heroStroke.setStroke(dp(1), 0x77FFFFFF);
        hero.setBackgroundDrawable(heroStroke);
        hero.setClipToOutline(true);
        hero.setElevation(dp(7));

        final String[] HERO_IMGS = {"hero1", "hero2", "hero3"};
        final ImageView heroIv = new ImageView(ctx);
        heroIv.setScaleType(ImageView.ScaleType.CENTER_CROP);
        hero.addView(heroIv, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, dp(hyImgH)));
        final int[] cur = {0};
        final View[] dots = new View[HERO_IMGS.length];

        for (int i = 0; i < HERO_IMGS.length; i++) {
            Bitmap b = image(HERO_IMGS[i]);
            if (b != null && i == 0) heroIv.setImageBitmap(b);
        }
        final GestureDetector heroGesture = new GestureDetector(ctx,
                new GestureDetector.SimpleOnGestureListener() {
            @Override public boolean onDown(MotionEvent e) { return true; }
            @Override public boolean onFling(MotionEvent e1, MotionEvent e2, float vx, float vy) {
                if (e1 == null || e2 == null) return false;
                float dx = e2.getX() - e1.getX();
                float dy = e2.getY() - e1.getY();
                if (Math.abs(dx) > dp(36) && Math.abs(dx) > Math.abs(dy)) {
                    int dir = dx < 0 ? 1 : -1;
                    int next = (cur[0] + dir + HERO_IMGS.length) % HERO_IMGS.length;
                    UiKit.switchHero(heroIv, HERO_IMGS, cur, next, dots, dx < 0 ? 1 : -1);
                    return true;
                }
                return false;
            }
        });
        heroIv.setOnTouchListener(new View.OnTouchListener() {
            @Override public boolean onTouch(View v, MotionEvent e) {
                heroGesture.onTouchEvent(e);
                return false;
            }
        });
        View hshade = new View(ctx);
        hshade.setBackgroundDrawable(new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0x00FFFFFF, 0x99FFFFFF, 0xFAFFFFFF}));
        if (hyImgH > 0) {
            hero.addView(hshade, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, dp(112), Gravity.BOTTOM));
        }

        LinearLayout heroTexts = new LinearLayout(ctx);
        heroTexts.setOrientation(LinearLayout.VERTICAL);
        heroTexts.setPadding(dp(16), 0, dp(70), dp(15));
        final TextView tag = pill("未启用", 0xFF7FA5E6);
        LinearLayout tagWrap = new LinearLayout(ctx);
        tagWrap.setOrientation(LinearLayout.HORIZONTAL);
        tagWrap.addView(tag, wrapWrap());
        heroTexts.addView(tagWrap, matchWrap(dp(7)));
        heroTexts.addView(text(com.cathelperawa.owo.app.core.Skin.text("home.hero.title", "猫娘化引擎"), hyTitleSize,
                com.cathelperawa.owo.app.ui.Theme.parse(hstHero.optString("titleColor", ""), 0xFF4F6485), true, dp(3)));
        final TextView desc = text(com.cathelperawa.owo.app.core.Skin.text("home.hero.desc", "模块已就绪，等待开启"), hyDescSize,
                com.cathelperawa.owo.app.ui.Theme.parse(hstHero.optString("descColor", ""), 0xFF8A9CB5), false, 0);
        heroTexts.addView(desc);
        hero.addView(heroTexts, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM));

        final Timer t = new Timer();
        t.schedule(new TimerTask() {
            @Override public void run() {
                try {
                    activity().runOnUiThread(new Runnable() {
                        public void run() {
                            try {
                                int next = (cur[0] + 1) % HERO_IMGS.length;
                                UiKit.switchHero(heroIv, HERO_IMGS, cur, next, dots, 1);
                            } catch (Throwable ignore) {
                            }
                        }
                    });
                } catch (Throwable ignore) {
                }
            }
        }, 4000, 4000);
        host.setHeroTimer(t);

        TextView gear = text("配置 ›", 12, 0xFF7FA5E6, true, 0);
        GradientDrawable gbg = new GradientDrawable();
        gbg.setColor(0xE6FFFFFF);
        gbg.setCornerRadius(dp(14));
        gear.setBackgroundDrawable(gbg);
        gear.setPadding(dp(12), dp(6), dp(12), dp(6));
        FrameLayout.LayoutParams gp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.END);
        gp.rightMargin = dp(14);
        gp.topMargin = dp(14);
        if (hyGear) hero.addView(gear, gp);
        gear.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { press(v); host.open(PAGE_CAT, "猫娘化引擎"); }
        });

        LinearLayout chips = new LinearLayout(ctx);
        chips.setOrientation(LinearLayout.HORIZONTAL);
        TextView apiChip = UiKit.chip(ctx, "API");
        apiChip.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { press(v); host.showApiDialog(); }
        });
        chips.addView(apiChip);
        TextView termChip = UiKit.chip(ctx, "终端");
        LinearLayout.LayoutParams tcp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        tcp.leftMargin = dp(8);
        chips.addView(termChip, tcp);
        termChip.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                press(v);
                
                com.cathelperawa.owo.app.ui.TerminalPanel.show(activity());
            }
        });
        FrameLayout.LayoutParams ap2 = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.START);
        ap2.leftMargin = dp(14);
        ap2.topMargin = dp(14);
        if (hyChips) hero.addView(chips, ap2);

        org.json.JSONObject chipSt = com.cathelperawa.owo.app.core.Skin.style("home.chip");
        String chipBg = chipSt.optString("bgColor", "");
        String chipFg = chipSt.optString("textColor", "");
        if (chipBg.length() > 0 || chipFg.length() > 0) {
            int cbg = com.cathelperawa.owo.app.ui.Theme.parse(chipBg, 0xE6FFFFFF);
            int cfg = com.cathelperawa.owo.app.ui.Theme.parse(chipFg, 0xFF7FA5E6);
            TextView[] chips3 = new TextView[]{apiChip, termChip, gear};
            for (int ci = 0; ci < chips3.length; ci++) {
                try {
                    GradientDrawable cg = new GradientDrawable();
                    cg.setColor(cbg);
                    cg.setCornerRadius(dp(chipSt.optInt("radius", 14)));
                    chips3[ci].setBackgroundDrawable(cg);
                    chips3[ci].setTextColor(cfg);
                } catch (Throwable ignore) {
                }
            }
        }

        LinearLayout dotsBar = new LinearLayout(ctx);
        dotsBar.setOrientation(LinearLayout.HORIZONTAL);
        dotsBar.setPadding(0, 0, dp(16), dp(18));
        for (int i = 0; i < HERO_IMGS.length; i++) {
            View d = new View(ctx);
            GradientDrawable dg = new GradientDrawable();
            dg.setShape(GradientDrawable.OVAL);
            dg.setColor(i == 0 ? 0xFF8FB8FF : 0x558FB8FF);
            LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(dp(7), dp(7));
            dlp.rightMargin = dp(6);
            d.setLayoutParams(dlp);
            d.setBackgroundDrawable(dg);
            dots[i] = d;
            dotsBar.addView(d);
        }
        if (hyDots) {
            hero.addView(dotsBar, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM | Gravity.END));
        }
        root.addView(hero, matchWrap(dp(11)));

        
        String[][] ENTRIES = {
                {"聊天功能", "让聊天更顺喵～", "art1", PAGE_CHAT},
                {"群管理", "群主便捷菜单 · 群信息", "h_group_card", PAGE_GROUP},
                {"去除限制", "转发 · 拍一拍 · 多选撤回", "art3", PAGE_UNLOCK},
                {"解析功能", "音乐 · 视频，链接变文件", "art9", PAGE_PARSE},
                {"文件功能", "上传 APK 自动改名", "art8", PAGE_FILE},
                {"伪造功能", "伪装网络 · 自定义设备", "art7", PAGE_FAKE},
                {"红包功能", "手动打开 · 自动领取", "art5", PAGE_REDPACKET},
                {"Hook 功能", "长按抓取 · 自动抓取", "art4", PAGE_HOOK},
                {"功能状态", "体检 · 哪些没起来", "art6", PAGE_STATUS},
                {"防检测", "掐断崩溃 · 统计上报", "art1", PAGE_ANTI},
                {"配置备份", "导出导入备份", "art2", PAGE_CONFIG},
                {"加入交流群", "QQ 群 459547307", "art2", "@group"},
        };

        
        if (com.cathelperawa.owo.app.core.UIScope.isQQ()) {
            java.util.ArrayList<String[]> keep = new java.util.ArrayList<>();
            for (String[] e : ENTRIES) {
                String k = e[3];
                if (PAGE_CHAT.equals(k) || PAGE_FILE.equals(k)
                        || PAGE_FAKE.equals(k) || PAGE_HOOK.equals(k)
                        || PAGE_STATUS.equals(k) || PAGE_ANTI.equals(k)
                        || PAGE_CONFIG.equals(k) || "@group".equals(k)) {
                    keep.add(e);
                }
            }
            ENTRIES = keep.toArray(new String[0][]);
        }
        for (int i = 0; i < ENTRIES.length; i++) {
            final String pageKey = ENTRIES[i][3];
            
            if (com.cathelperawa.owo.app.core.Skin.hidden("home." + pageKey)) continue;
            
            final String title = com.cathelperawa.owo.app.core.Skin
                    .text("home." + pageKey + ".title", ENTRIES[i][0]);
            final String sub = com.cathelperawa.owo.app.core.Skin
                    .text("home." + pageKey + ".sub", ENTRIES[i][1]);

            org.json.JSONObject st = com.cathelperawa.owo.app.core.Skin.style("home.card");
            int stRadius = st.optInt("radius", 20);
            int stGap = st.optInt("gap", 11);
            int stImgH = st.optInt("imageHeight", 78);      
            int stPadX = st.optInt("padX", 14);
            int stMinH = st.optInt("minHeight", 0);
            int stTitleSize = st.optInt("titleSize", 16);
            int stSubSize = st.optInt("subSize", 11);
            String stCard = st.optString("bgColor", "");
            String stTitleColor = st.optString("titleColor", "");
            String stSubColor = st.optString("subColor", "");
            String stArrowColor = st.optString("arrowColor", "");
            boolean stShadow = !"0".equals(st.optString("shadow", "1"));

            FrameLayout entry = whiteFrame(dp(stRadius), stCard.length() > 0
                    ? com.cathelperawa.owo.app.ui.Theme.parse(stCard, 0x8CFFFFFF) : 0x8CFFFFFF);
            entry.setClipToOutline(true);
            if (stMinH > 0) entry.setMinimumHeight(dp(stMinH));
            if (!stShadow) entry.setElevation(0);

            if (stImgH > 0) {
                String bgFile = com.cathelperawa.owo.app.core.Skin.bg(ENTRIES[i][2]);
                Bitmap eb = bgFile != null
                        ? (bgFile.startsWith("#")
                            ? solidBitmap(com.cathelperawa.owo.app.ui.Theme.parse(bgFile, 0x8CFFFFFF))
                            : android.graphics.BitmapFactory.decodeFile(bgFile))
                        : image(ENTRIES[i][2]);
                if (eb != null) {
                    ImageView eiv = new ImageView(ctx);
                    eiv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    eiv.setImageBitmap(eb);
                    entry.addView(eiv, new FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT, dp(stImgH)));
                    View esh = new View(ctx);
                    esh.setBackgroundDrawable(new GradientDrawable(
                            GradientDrawable.Orientation.LEFT_RIGHT,
                            new int[]{0xF7FFFFFF, 0xA6FFFFFF, 0x33FFFFFF}));
                    entry.addView(esh, new FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT, dp(stImgH)));
                }
            }

            LinearLayout et = new LinearLayout(ctx);
            et.setOrientation(LinearLayout.VERTICAL);
            et.addView(text(title, stTitleSize,
                    com.cathelperawa.owo.app.ui.Theme.parse(stTitleColor, 0xFF43587A), true, dp(1)));
            et.addView(text(sub, stSubSize,
                    com.cathelperawa.owo.app.ui.Theme.parse(stSubColor, 0xFF8496AE), false, 0));
            FrameLayout.LayoutParams etp = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.START | Gravity.CENTER_VERTICAL);
            etp.leftMargin = dp(stPadX);
            entry.addView(et, etp);

            TextView arrow = text("›", 22,
                    com.cathelperawa.owo.app.ui.Theme.parse(stArrowColor, 0xFFB0BDD1), true, 0);
            FrameLayout.LayoutParams arp = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.END | Gravity.CENTER_VERTICAL);
            arp.rightMargin = dp(stPadX);
            entry.addView(arrow, arp);

            entry.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(final View v) {
                    press(v);
                    if (pageKey != null && pageKey.startsWith("@")) {
                        com.cathelperawa.owo.app.ui.ExtLink.open(ctx,
                                com.cathelperawa.owo.app.ui.ExtLink.GROUP);
                        return;
                    }
                    host.open(pageKey, title);
                }
            });
            root.addView(entry, matchWrap(dp(stGap)));
        }

        try {
            java.util.List<String[]> sp = com.cathelperawa.owo.app.ui.ScriptScreen.declaredPages();
            for (int i = 0; i < sp.size(); i++) {
                final String key = sp.get(i)[0];
                final String spTitle = sp.get(i)[1];
                String sub = sp.get(i)[2];
                LinearLayout card = whiteCard(dp(20), 0x8CFFFFFF);
                card.setOrientation(LinearLayout.HORIZONTAL);
                card.setGravity(android.view.Gravity.CENTER_VERTICAL);
                card.setPadding(dp(15), dp(14), dp(15), dp(14));

                LinearLayout left = new LinearLayout(ctx);
                left.setOrientation(LinearLayout.VERTICAL);
                left.addView(text(spTitle, 16, 0xFF43587A, true, dp(2)));
                if (sub != null && sub.length() > 0) {
                    left.addView(text(sub, 11, 0xFF8496AE, false, 0));
                }
                card.addView(left, new LinearLayout.LayoutParams(0,
                        LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
                card.addView(text("›", 22, 0xFFB0BDD1, true, 0));

                card.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        press(v);
                        host.open(key, spTitle);
                    }
                });
                root.addView(card, matchWrap(dp(11)));
            }
        } catch (Throwable ignore) {
        }

        root.addView(buildHostCard(), matchWrap(dp(11)));

        LinearLayout about = whiteCard(dp(24), 0xB8FFFFFF);
        about.setPadding(dp(15), dp(13), dp(15), dp(11));
        LinearLayout ah = new LinearLayout(ctx);
        ah.setOrientation(LinearLayout.HORIZONTAL);
        ah.setGravity(Gravity.CENTER_VERTICAL);
        ah.addView(text("关于", 16, 0xFF4F6485, true, 0), new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        ah.addView(pill("v" + com.cathelperawa.owo.app.BuildInfo.VERSION_NAME, 0xFF7FA5E6));
        about.addView(ah, matchWrap(dp(7)));

        int api = 0;
        try {
            api = de.robv.android.xposed.XposedBridge.getXposedVersion();
        } catch (Throwable ignore) {
        }
        boolean active = api > 0;
        String statusTxt = active ? ("已激活" + (api > 0 ? " · API " + api : "")) : "未激活";

        String loadTxt = HostInfo.loader();

        String fwVer = HostInfo.frameworkVersion();
        String fwTxt = fwVer != null ? ("LSPosed " + fwVer)
                : (active ? "LSPosed" : "—");

        String hostVer = HostInfo.hostVersion();
        String hostTxt = hostVer != null
                ? ((com.cathelperawa.owo.app.core.Host.isQQ() ? "QQ " : "TIM ") + hostVer)
                : "—";

        String scopeTxt = HostInfo.activeScope();
        if (scopeTxt == null || scopeTxt.length() == 0) {
            scopeTxt = com.cathelperawa.owo.app.BuildInfo.SCOPE_LABEL;
        }

        String[][] rows = {
                {"模块状态", statusTxt},
                {"加载方式", loadTxt},
                {"框架版本", fwTxt},
                {"宿主版本", hostTxt},
                {"作用域", scopeTxt},

                {"构建时间", com.cathelperawa.owo.app.BuildInfo.BUILD_TIME},
                {"构建标识", com.cathelperawa.owo.app.BuildInfo.BUILD_UUID},
        };
        for (String[] r : rows) {
            LinearLayout rl = new LinearLayout(ctx);
            rl.setOrientation(LinearLayout.HORIZONTAL);
            rl.addView(text(r[0], 12.5f, 0xFF8A9CB5, false, dp(4)),
                    new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            TextView tv = text(r[1], 12.5f, 0xFF7FA5E6, true, dp(4));
            
            if (("模块状态".equals(r[0]) || "加载方式".equals(r[0])) && !active) {
                tv.setTextColor(0xFFE57373);
            }
            if (r[1] != null && r[1].endsWith("—")) tv.setTextColor(0xFFB0BDD1);
            rl.addView(tv);
            about.addView(rl);
        }
        root.addView(about, matchWrap(0));

        root.setAlpha(0f);
        root.setTranslationY(dp(16));
        root.animate().alpha(1f).translationY(0f)
                .setDuration(460).setInterpolator(new DecelerateInterpolator()).start();

        refreshHero(tag, desc);

        final GestureDetector gd = new GestureDetector(ctx,
                new GestureDetector.SimpleOnGestureListener() {
            @Override public boolean onDown(MotionEvent e) { return true; }
            @Override public boolean onFling(MotionEvent e1, MotionEvent e2, float vx, float vy) {
                if (e1 == null || e2 == null) return false;
                if (!inHero(e1, hero, scroll)) return false;
                float dx = e2.getX() - e1.getX();
                float dy = e2.getY() - e1.getY();
                if (Math.abs(dx) > dp(50) && Math.abs(dx) > Math.abs(dy)) {
                    int dir = dx < 0 ? 1 : -1;
                    int next = (cur[0] + dir + HERO_IMGS.length) % HERO_IMGS.length;
                    UiKit.switchHero(heroIv, HERO_IMGS, cur, next, dots, dx < 0 ? 1 : -1);
                    return true;
                }
                return false;
            }
            @Override public boolean onSingleTapUp(MotionEvent e) {
                if (e == null || !inHero(e, hero, scroll)) return false;
                host.open(PAGE_CAT, "猫娘化引擎");
                return true;
            }
        });
        scroll.setOnTouchListener(new View.OnTouchListener() {
            @Override public boolean onTouch(View v, MotionEvent e) {
                gd.onTouchEvent(e);
                return false;
            }
        });

        page.addView(scroll, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        host.setHomeScroll(scroll);
        return page;
    }

    

    
    private View buildHostCard() {
        LinearLayout card = whiteCard(dp(22), 0xB8FFFFFF);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(14), dp(14), dp(14), dp(14));

        final String uin = HostInfo.uin();
        String nick = HostInfo.nick();
        if (nick == null || nick.length() == 0) {
            nick = (uin == null) ? "未知用户" : ("用户 " + tail4(uin));
        }

        final ImageView avatar = new ImageView(ctx);
        avatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
        GradientDrawable ring = new GradientDrawable();
        ring.setShape(GradientDrawable.OVAL);
        ring.setColor(0xFFEDF2FB);
        ring.setStroke(dp(2), 0x66FFFFFF);
        avatar.setBackgroundDrawable(ring);
        FrameLayout avatarWrap = new FrameLayout(ctx);
        avatarWrap.setClipToOutline(true);
        GradientDrawable clip = new GradientDrawable();
        clip.setShape(GradientDrawable.OVAL);
        clip.setColor(0xFFFFFFFF);
        avatarWrap.setBackgroundDrawable(clip);
        avatarWrap.addView(avatar, new FrameLayout.LayoutParams(dp(52), dp(52)));
        card.addView(avatarWrap, new LinearLayout.LayoutParams(dp(52), dp(52)));
        loadAvatarAsync(avatar, uin);

        LinearLayout col = new LinearLayout(ctx);
        col.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        cp.leftMargin = dp(12);

        LinearLayout nameRow = new LinearLayout(ctx);
        nameRow.setOrientation(LinearLayout.HORIZONTAL);
        nameRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView nameTv = text(nick, 16, 0xFF43587A, true, 0);
        nameTv.setSingleLine(true);
        nameTv.setEllipsize(android.text.TextUtils.TruncateAt.END);
        nameRow.addView(nameTv, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        nameRow.addView(pill("已登录", 0xFF7FA5E6));
        col.addView(nameRow, matchWrap(dp(5)));
        col.addView(text(uin == null ? "QQ —" : ("QQ " + uin), 12.5f, 0xFF8A9CB5, false, 0));

        card.addView(col, cp);
        return card;
    }

    private static String tail4(String s) {
        if (s == null) return "—";
        return s.length() <= 4 ? s : s.substring(s.length() - 4);
    }

    private static final java.util.Map<String, Bitmap> AVATAR_CACHE =
            new java.util.HashMap<String, Bitmap>();

    private void loadAvatarAsync(final ImageView iv, final String uin) {
        if (uin == null) return;
        Bitmap cached = AVATAR_CACHE.get(uin);
        if (cached != null && !cached.isRecycled()) {
            iv.setImageBitmap(cached);
            return;
        }
        final String url = HostInfo.avatarUrl();
        if (url == null) return;
        Bg.run(new Runnable() {
            @Override public void run() {
                try {
                    java.net.URL u = new java.net.URL(url);
                    java.net.HttpURLConnection conn = (java.net.HttpURLConnection) u.openConnection();
                    conn.setConnectTimeout(6000);
                    conn.setReadTimeout(6000);
                    conn.setInstanceFollowRedirects(true);
                    final Bitmap bmp = android.graphics.BitmapFactory.decodeStream(conn.getInputStream());
                    conn.disconnect();
                    if (bmp == null) return;
                    final Bitmap circle = circleCrop(bmp);
                    AVATAR_CACHE.put(uin, circle);
                    try {
                        activity().runOnUiThread(new Runnable() {
                            @Override public void run() { iv.setImageBitmap(circle); }
                        });
                    } catch (Throwable ignore) {
                    }
                } catch (Throwable ignore) {
                    
                }
            }
        }, "cat-avatar");
    }

    private static Bitmap circleCrop(Bitmap src) {
        int size = Math.min(src.getWidth(), src.getHeight());
        int x = (src.getWidth() - size) / 2;
        int y = (src.getHeight() - size) / 2;
        Bitmap out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        android.graphics.Canvas canvas = new android.graphics.Canvas(out);
        android.graphics.Paint paint = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        android.graphics.BitmapShader shader = new android.graphics.BitmapShader(
                src, android.graphics.Shader.TileMode.CLAMP, android.graphics.Shader.TileMode.CLAMP);
        android.graphics.Matrix m = new android.graphics.Matrix();
        m.setTranslate(-x, -y);
        shader.setLocalMatrix(m);
        paint.setShader(shader);
        float r = size / 2f;
        canvas.drawCircle(r, r, r, paint);
        return out;
    }

    private static boolean inHero(MotionEvent e, View hero, View scroll) {
        try {
            int[] heroLoc = new int[2];
            int[] scrollLoc = new int[2];
            hero.getLocationOnScreen(heroLoc);
            scroll.getLocationOnScreen(scrollLoc);
            float hx = heroLoc[0] - scrollLoc[0];
            float hy = heroLoc[1] - scrollLoc[1];
            float ex = e.getX(), ey = e.getY();
            return ex >= hx && ex <= hx + hero.getWidth()
                    && ey >= hy && ey <= hy + hero.getHeight();
        } catch (Throwable t) {
            return false;
        }
    }

    private void refreshHero(TextView tag, TextView desc) {
        boolean total = cfgGet("cat_total", true);
        int on = 0;
        for (String[] sw : CAT_SWITCHES) {
            if (!"cat_total".equals(sw[0]) && cfgGet(sw[0], true)) on++;
        }
        tag.setText(total ? "运行中" : "已关闭");
        tag.setTextColor(total ? 0xFFF07CA8 : 0xFF7FA5E6);
        ((GradientDrawable) tag.getBackground()).setColor(total ? 0xFFFFF0F6 : 0xFFFFFFFF);
        desc.setText(total
                ? "已开启 " + on + " 项功能 · 作用范围：" + (MsgStore.isScopeAll() ? "全部会话" : "指定会话")
                : "总开关已关，猫娘化暂停");
    }

    

    
    
    private void confirmRestart(Context c) {
        try {
            new android.app.AlertDialog.Builder(
                    c, android.app.AlertDialog.THEME_DEVICE_DEFAULT_LIGHT)
                    .setTitle("重启 " + com.cathelperawa.owo.app.core.Host.name())
                    .setMessage(com.cathelperawa.owo.app.core.Host.name() + " 会立刻退出并重新打开喵～")
                    .setPositiveButton("重启", new android.content.DialogInterface.OnClickListener() {
                        @Override public void onClick(android.content.DialogInterface d, int w) {
                            doRestart();
                        }
                    })
                    .setNegativeButton("取消", null)
                    .show();
        } catch (Throwable t) {
            doRestart();   
        }
    }

    private void doRestart() {
        try {
            com.cathelperawa.owo.app.MsgStore.add("[重启] 用户点了重启，准备杀进程");
        } catch (Throwable ignore) {
        }
        
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(
                new Runnable() {
                    @Override public void run() {
                        try {
                            android.os.Process.killProcess(android.os.Process.myPid());
                        } catch (Throwable ignore) {
                        }
                    }
                }, 300);
    }

    private Bitmap solidBitmap(int color) {
        try {
            Bitmap bm = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888);
            new android.graphics.Canvas(bm).drawColor(color);
            return bm;
        } catch (Throwable t) {
            return null;
        }
    }
}
