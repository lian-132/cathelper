package com.cathelperawa.owo.app.ui;

import android.graphics.Bitmap;
import com.cathelperawa.owo.app.core.Bg;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public final class ParsePage extends Page {

    private static final String K_M_ON = "pk_music_on";
    private static final String K_M_SEND = "pk_music_send";
    private static final String K_M_BR = "pk_music_br";
    private static final String K_M_API = "pk_music_api";
    private static final String K_M_COUNT = "pk_music_count";
    
    private static final String K_M_SOURCE = "pk_music_source";
    private static final String[] M_SOURCE = {"公开接口", "我的网易云", "自定义接口"};
    private static final String[] M_SOURCE_KEY = {"public", "account", "custom"};
    private static final String K_M_SEC = "pk_music_sec";

    private static final String K_V_ON = "pk_video_on";
    private static final String K_V_SEND = "pk_video_send";
    private static final String K_V_MAX = "pk_video_max";
    private static final String K_V_API = "pk_video_api";
    
    private static final String K_V_APIS = "pk_video_apis";
    private static final String K_V_PLAT = "pk_video_plat";
    
    private static final String K_V_SOURCE = "pk_video_source";
    private static final String[] V_SOURCE = {"公开接口", "我的 B站", "自定义接口"};
    private static final String[] V_SOURCE_KEY = {"public", "account", "custom"};

    private static final String[] M_SEND = {"卡片", "语音", "文件", "图片"};
    private static final String[] M_SEND_KEY = {"card", "voice", "file", "image"};
    
    private static final String[] M_BR = {"128k", "192k", "320k"};
    private static final String[] M_BR_KEY = {"128", "192", "320"};
    
    private static final String[] V_SEND = {"视频", "语音"};
    private static final String[] V_SEND_KEY = {"video", "voice"};
    
    private static final String[] V_PLAT = {"B站", "抖音", "小红书"};
    private static final String[] V_PLAT_KEY = {"bili", "douyin", "xhs"};

    private static final String DEF_M_API = "https://music.163.com/api";
    private static final String DEF_V_API = "https://api.bilibili.com/x";

    private LinearLayout tabMusic;
    private LinearLayout tabVideo;
    
    private LinearLayout tabVoice;
    private TextView capVoice;
    private TextView capMusic;
    private TextView capVideo;
    private TextView musicPill;
    private TextView videoPill;

    public ParsePage(Page.Host host) {
        super(host);
    }

    

    @Override
    public View build() {
        View[] out = new View[2];
        LinearLayout root = pageRoot("解析功能", "链接 · 歌名 → 直接发出去", out, true, 12);

        root.addView(buildHero(), matchWrap(dp(11)));
        root.addView(buildTabBar(), matchWrap(dp(10)));

        tabMusic = buildMusicTab();
        tabVideo = buildVideoTab();
        tabVoice = buildVoiceTab();
        root.addView(tabMusic, matchWrap(dp(0)));
        root.addView(tabVideo, matchWrap(dp(0)));
        root.addView(tabVoice, matchWrap(dp(0)));
        tabVideo.setVisibility(View.GONE);
        tabVoice.setVisibility(View.GONE);

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

        Bitmap bmp = image("h_parse");
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

        boolean mOn = swOn(K_M_ON);
        boolean vOn = swOn(K_V_ON);
        int on = (mOn ? 1 : 0) + (vOn ? 1 : 0);
        String state = on == 0 ? "全部关闭"
                : (on == 2 ? "音乐 · 视频 都开着"
                : (mOn ? "只开音乐" : "只开视频"));
        musicPill = pill(state, on == 0 ? 0xFF7F90A6 : 0xFFF07AAE);
        
        if (on > 0) {
            ((GradientDrawable) musicPill.getBackground()).setColor(0xFFFFC2DF);
        }
        LinearLayout pillRow = new LinearLayout(ctx);
        pillRow.setOrientation(LinearLayout.HORIZONTAL);
        pillRow.addView(musicPill, wrapWrap());
        ht.addView(pillRow, matchWrap(dp(7)));

        ht.addView(text("解析功能", 20, 0xFF4A5872, true, dp(3)));
        ht.addView(text("丢个链接 / 报个歌名，直接变成文件", 12, 0xFF8A99AC, false, 0));

        hero.addView(ht, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM));
        return hero;
    }

    private void refreshHero() {
        if (musicPill == null) return;
        boolean mOn = swOn(K_M_ON);
        boolean vOn = swOn(K_V_ON);
        int on = (mOn ? 1 : 0) + (vOn ? 1 : 0);
        String state = on == 0 ? "全部关闭"
                : (on == 2 ? "音乐 · 视频 都开着"
                : (mOn ? "只开音乐" : "只开视频"));
        musicPill.setText(state);
        ((GradientDrawable) musicPill.getBackground())
                .setColor(on == 0 ? 0xFFFFFFFF : 0xFFFFC2DF);
        musicPill.setTextColor(on == 0 ? 0xFF7F90A6 : 0xFFF07AAE);
    }

    

    private View buildTabBar() {
        LinearLayout bar = new LinearLayout(ctx);
        bar.setOrientation(LinearLayout.HORIZONTAL);

        capMusic = capsule("♪  音乐", true);
        capVideo = capsule("▶  视频", false);
        capVoice = capsule("◎  语音库", false);

        LinearLayout.LayoutParams p1 = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        p1.rightMargin = dp(4);
        LinearLayout.LayoutParams p2 = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        p2.leftMargin = dp(3);
        p2.rightMargin = dp(4);
        LinearLayout.LayoutParams p3 = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        p3.leftMargin = dp(3);

        capMusic.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { switchTab(0); }
        });
        capVideo.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { switchTab(1); }
        });
        capVoice.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { switchTab(2); }
        });
        bar.addView(capMusic, p1);
        bar.addView(capVideo, p2);
        bar.addView(capVoice, p3);
        return bar;
    }

    private void switchTab(int idx) {
        paintCapsule(capMusic, idx == 0);
        paintCapsule(capVideo, idx == 1);
        if (capVoice != null) paintCapsule(capVoice, idx == 2);
        if (tabMusic != null) tabMusic.setVisibility(idx == 0 ? View.VISIBLE : View.GONE);
        if (tabVideo != null) tabVideo.setVisibility(idx == 1 ? View.VISIBLE : View.GONE);
        if (tabVoice != null) tabVoice.setVisibility(idx == 2 ? View.VISIBLE : View.GONE);
        if (tabVoice != null) {
            if (voiceBar != null) {
                if (idx == 2) voiceBar.attach();
                else voiceBar.detach();
            }
        }
        if (idx == 2) { renderVoiceList(); refreshVoiceStat(); }
        View from = idx == 0 ? capMusic : (idx == 1 ? capVideo : capVoice);
        ScrollView sv = findScroll(from);
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

    

    private LinearLayout buildMusicTab() {
        LinearLayout box = new LinearLayout(ctx);
        box.setOrientation(LinearLayout.VERTICAL);

        box.addView(switchCard("pa_music", "解析音乐",
                "群里发「来首晴天」→ 搜歌 → 按下面的方式发出来", K_M_ON), matchWrap(dp(10)));

        box.addView(segCard("发送方式", "选一种，发出去就是这个样子",
                M_SEND, M_SEND_KEY, K_M_SEND, 0), matchWrap(dp(10)));

        box.addView(segCard("音质", "越高越占流量，语音条反正听不出差别",
                M_BR, M_BR_KEY, K_M_BR, 2), matchWrap(dp(10)));

        box.addView(segCard("音乐来源", new SubText() {
            @Override public String get() { return sourceHint(); }
        }, M_SOURCE, M_SOURCE_KEY, K_M_SOURCE, 0), matchWrap(dp(10)));
        box.addView(cookieRow("netease", "网易云"), matchWrap(dp(10)));

        box.addView(switchLine("自动转换格式",
                "开 = mp3 转 SILK 发语音（音质会降）；关 = 直接拿 mp3 当语音发",
                "pk_music_conv", true), matchWrap(dp(10)));

        box.addView(numRow("自定义显示时长(秒)", "pk_music_fake_sec", "0",
                "0 = 用真实时长；填数字只改气泡上显示的秒数"), matchWrap(dp(10)));

        box.addView(apiListCard("音乐接口",
                "官方接口已内置；这里填的是官方搜不到时的备用线路", "pk_music_apis"), matchWrap(dp(10)));

        box.addView(numRow("最多几条", K_M_COUNT, "15",
                "「选一首」最多显示几条；也决定自动挑歌试几首（1~50）"), matchWrap(dp(10)));

        box.addView(hintCard("说明",
                "搜索 + 直链走同一个接口；官方源不需要填 Key。\n"
                        + "语音条是 16kHz 单声道，音乐会有压缩感，建议只发副歌。"), matchWrap(dp(10)));

        box.addView(btnRow("试一下（填歌名）", new Runnable() {
            @Override public void run() {
                CatSheet.input(ctx, "试一下 · 歌名", "例如：晴天 周杰伦",
                        "", new CatSheet.OnText() {
                            @Override public void run(String s) {
                                toast("还没接线喵～ 之后会搜「" + s + "」");
                            }
                        });
            }
        }), matchWrap(dp(10)));

        return box;
    }

    

    private LinearLayout buildVideoTab() {
        LinearLayout box = new LinearLayout(ctx);
        box.setOrientation(LinearLayout.VERTICAL);

        box.addView(switchCard("pa_video", "解析视频",
                "群里发链接 → 解析 → 直接把视频发出来", K_V_ON), matchWrap(dp(10)));

        box.addView(segCard("发送方式", "视频=真发文件 · 卡片=点开跳原站 · 直链=只回网址",
                V_SEND, V_SEND_KEY, K_V_SEND, 0), matchWrap(dp(10)));

        box.addView(multiSegCard("支持的平台", "勾上的才会自动认链接",
                V_PLAT, V_PLAT_KEY, K_V_PLAT, new String[]{"bili"}), matchWrap(dp(10)));

        box.addView(segCard("视频来源", new SubText() {
            @Override public String get() { return videoSourceHint(); }
        }, V_SOURCE, V_SOURCE_KEY, K_V_SOURCE, 0), matchWrap(dp(10)));
        box.addView(cookieRow("bilibili", "B站"), matchWrap(dp(10)));

        box.addView(apiListCard("解析接口",
                "按顺序试，前面失败自动换下一个。B站官方接口已内置在最前面。"), matchWrap(dp(10)));

        box.addView(numRow("大小上限 (MB)", K_V_MAX, "100",
                "超过这个大小只发卡片，不硬拖"), matchWrap(dp(10)));

        box.addView(hintCard("说明",
                "B站：官方接口，能直接拿到音视频合一的 mp4，不需要 Cookie。\n"
                        + "抖音：内置解析（隐藏 WebView 跑页面自己的 JS 拿 play_addr），不用填任何接口，"
                        + "出来的就是去水印直链。\n"
                        + "小红书：还没内置，要在下面填自建服务。"), matchWrap(dp(10)));

        box.addView(btnRow("试一下（填链接）", new Runnable() {
            @Override public void run() {
                CatSheet.input(ctx, "试一下 · 视频链接", "粘贴 B站 / 抖音 链接",
                        "", new CatSheet.OnText() {
                            @Override public void run(String s) {
                                toast("还没接线喵～ 之后会解析这条链接");
                            }
                        });
            }
        }), matchWrap(dp(10)));

        return box;
    }

    

    private TextView voiceStat;
    
    private LinearLayout voiceList;
    
    private VoicePlayerBar voiceBar;

    private LinearLayout buildVoiceTab() {
        LinearLayout box = new LinearLayout(ctx);
        box.setOrientation(LinearLayout.VERTICAL);

        LinearLayout card = whiteCard(dp(20), 0x8CFFFFFF);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(14), dp(14), dp(14));
        card.addView(text("语音库", 16, 0xFF43587A, true, dp(2)));
        card.addView(text("攒语音条的地方：长按别人的语音存进来，也能导文件，"
                + "库里随时转腾讯语音 / MP3", 11, 0xFF8496AE, false, dp(9)));
        voiceStat = text("统计中…", 12, 0xFFE85D9C, true, 0);
        card.addView(voiceStat);
        box.addView(card, matchWrap(dp(9)));

        box.addView(btnRow("打开语音库（存 / 导 / 转 / 批量删）", new Runnable() {
            @Override public void run() {
                VoiceVaultSheet.show(ctx);
            }
        }), matchWrap(dp(7)));

        box.addView(btnRow("从解析结果导入语音", new Runnable() {
            @Override public void run() {
                importParseVoice();
            }
        }), matchWrap(dp(9)));

        LinearLayout listCard = whiteCard(dp(20), 0x8CFFFFFF);
        listCard.setOrientation(LinearLayout.VERTICAL);
        listCard.setPadding(dp(12), dp(12), dp(12), dp(12));
        LinearLayout listHead = new LinearLayout(ctx);
        listHead.setOrientation(LinearLayout.HORIZONTAL);
        listHead.setGravity(Gravity.CENTER_VERTICAL);
        listHead.addView(text("库里的语音", 13, 0xFF43587A, true, 0),
                new LinearLayout.LayoutParams(0,
                        LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        listHead.addView(text("点条目试听", 10, 0xFF9AAABF, false, 0));
        listCard.addView(listHead, matchWrap(dp(8)));

        voiceList = new LinearLayout(ctx);
        voiceList.setOrientation(LinearLayout.VERTICAL);
        listCard.addView(voiceList, matchWrap(0));

        box.addView(listCard, matchWrap(dp(7)));

        voiceBar = new VoicePlayerBar(ctx);
        box.addView(voiceBar.view(), matchWrap(dp(9)));

        box.addView(hintCard("怎么往里塞语音",
                "① 长按任意一条语音 → 菜单里选「存语音」\n"
                        + "② 语音库面板顶部「导入文件」，手机里的 .silk / mp3 / m4a / amr 都认\n"
                        + "③ 音乐 / 视频用「语音」方式发过之后，点上面「从解析结果导入」\n\n"
                        + "库里标「腾讯语音」的能直接发语音条；标「普通音频」的要先转一次。"),
                matchWrap(dp(10)));

        renderVoiceList();
        refreshVoiceStat();
        return box;
    }

    private void renderVoiceList() {
        if (voiceList == null) return;
        voiceList.removeAllViews();
        java.util.List<com.cathelperawa.owo.app.proto.VoiceVault.Item> lib;
        try {
            lib = com.cathelperawa.owo.app.proto.VoiceVault.list();
        } catch (Throwable t) {
            lib = new java.util.ArrayList<com.cathelperawa.owo.app.proto.VoiceVault.Item>();
        }
        if (lib.isEmpty()) {
            voiceList.addView(text("还没有语音 · 长按别人的语音存进来", 11, 0xFF9AAABF, false, 0));
            return;
        }
        for (int i = 0; i < lib.size(); i++) {
            voiceList.addView(voiceRow(lib.get(i)), matchWrap(dp(6)));
        }
    }

    private View voiceRow(final com.cathelperawa.owo.app.proto.VoiceVault.Item it) {
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(12), dp(11), dp(10), dp(11));

        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(dp(16));
        g.setColor(0x99FFFFFF);
        g.setStroke(dp(1), 0x55FFFFFF);
        row.setBackgroundDrawable(g);

        LinearLayout texts = new LinearLayout(ctx);
        texts.setOrientation(LinearLayout.VERTICAL);
        String name = it.name == null || it.name.length() == 0 ? "未命名" : it.name;
        texts.addView(text(name, 14, 0xFF43587A, true, 2));

        StringBuilder meta = new StringBuilder();
        if (it.durMs > 0) meta.append(it.durText());
        long size = it.sizeBytes();
        if (size > 0) meta.append(meta.length() > 0 ? " · " : "").append(size / 1024).append("KB");
        boolean hasSilk = it.hasSilk || com.cathelperawa.owo.app.proto.VoiceVault.silkFile(it) != null;
        meta.append(meta.length() > 0 ? " · " : "").append(hasSilk ? "腾讯语音" : "普通音频");
        texts.addView(text(meta.toString(), 11, hasSilk ? 0xFFE85D9C : 0xFF8496AE, false, 0));
        row.addView(texts, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView del = new TextView(ctx);
        del.setText("删除");
        del.setTextSize(11);
        del.setTypeface(Typeface.DEFAULT_BOLD);
        del.setGravity(Gravity.CENTER);
        del.setTextColor(0xFFE88098);
        del.setPadding(dp(11), dp(6), dp(11), dp(6));
        GradientDrawable dg = new GradientDrawable();
        dg.setCornerRadius(dp(12));
        dg.setColor(0x59FFFFFF);
        dg.setStroke(dp(1), 0x66FFFFFF);
        del.setBackgroundDrawable(dg);
        del.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                press(v);
                CatSheet.confirm(ctx, "删掉这条语音", it.name + "\n文件会一起删，找不回来。",
                        "删掉", true, new Runnable() {
                            @Override public void run() {
                                com.cathelperawa.owo.app.proto.VoiceVault.remove(
                                        java.util.Collections.singletonList(it));
                                renderVoiceList();
                                refreshVoiceStat();
                                toast("删掉啦");
                            }
                        });
            }
        });
        row.addView(del);

        TextView play = new TextView(ctx);
        play.setText("▶");
        play.setTextSize(9);
        play.setGravity(Gravity.CENTER);
        play.setTextColor(0xFFFFFFFF);
        GradientDrawable pg = new GradientDrawable();
        pg.setShape(GradientDrawable.OVAL);
        pg.setColor(0xFF7FC9A6);
        play.setBackgroundDrawable(pg);
        LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(dp(26), dp(26));
        plp.rightMargin = dp(0);
        plp.leftMargin = dp(6);
        play.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                press(v);
                String now = com.cathelperawa.owo.app.proto.VoiceVault.playingId();
                if (now != null && now.equals(it.id)) {
                    com.cathelperawa.owo.app.proto.VoiceVault.stop();
                    if (voiceBar != null) voiceBar.sync();
                    return;
                }
                toast("准备试听…");
                com.cathelperawa.owo.app.proto.VoiceVault.playAsync(it,
                        new com.cathelperawa.owo.app.proto.VoiceVault.OnPlay() {
                            @Override public void run(boolean ok) {
                                if (voiceBar != null) voiceBar.sync();
                                if (!ok) toast("放不出来：" + com.cathelperawa.owo.app.proto.VoiceVault.lastError());
                            }
                        });
            }
        });

        row.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                press(v);
                String now = com.cathelperawa.owo.app.proto.VoiceVault.playingId();
                if (now != null && now.equals(it.id)) {
                    com.cathelperawa.owo.app.proto.VoiceVault.stop();
                    if (voiceBar != null) voiceBar.sync();
                    toast("停了");
                    return;
                }
                toast("准备试听…");
                com.cathelperawa.owo.app.proto.VoiceVault.playAsync(it,
                        new com.cathelperawa.owo.app.proto.VoiceVault.OnPlay() {
                            @Override public void run(boolean ok) {
                                if (voiceBar != null) voiceBar.sync();
                                if (!ok) toast("放不出来：" + com.cathelperawa.owo.app.proto.VoiceVault.lastError());
                            }
                        });
            }
        });
        return row;
    }

    private void refreshVoiceStat() {
        if (voiceStat == null) return;
        try {
            voiceStat.setText(com.cathelperawa.owo.app.proto.VoiceVault.stat());
        } catch (Throwable t) {
            voiceStat.setText("统计失败");
        }
    }

    private void importParseVoice() {
        final java.util.List<java.io.File> fs =
                com.cathelperawa.owo.app.proto.VoiceVault.scanParseResults();
        if (fs.isEmpty()) {
            toast("解析临时区里没找到语音（先用「语音」方式发过音乐/视频）");
            return;
        }
        String[] labels = new String[fs.size()];
        for (int i = 0; i < fs.size(); i++) {
            java.io.File f = fs.get(i);
            labels[i] = f.getName() + "  " + (f.length() / 1024) + "KB";
        }
        CatSheet.pick(ctx, "解析出来的语音（点一个导入）", labels, new CatSheet.OnPick() {
            @Override public void run(int idx, String text) {
                java.io.File f = fs.get(idx);
                String n = f.getName();
                int dot = n.lastIndexOf('.');
                if (dot > 0) n = n.substring(0, dot);
                com.cathelperawa.owo.app.proto.VoiceVault.Item it =
                        com.cathelperawa.owo.app.proto.VoiceVault.addFile(ctx, n, f, "解析");
                toast(it != null ? "已导入语音库" : "这个文件读不动…");
                refreshVoiceStat();
            }
        });
    }

    

    
    private View switchCard(String img, final String title, String sub, final String key) {
        FrameLayout card = whiteFrame(dp(20), 0x8CFFFFFF);
        card.setClipToOutline(true);

        Bitmap bmp = image(img);
        if (bmp != null) {
            ImageView iv = new ImageView(ctx);
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            iv.setImageBitmap(bmp);
            card.addView(iv, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, dp(78)));
            View sh = new View(ctx);
            sh.setBackgroundDrawable(new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                    new int[]{0xF7FFFFFF, 0xA6FFFFFF, 0x33FFFFFF}));
            card.addView(sh, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, dp(78)));
        }

        LinearLayout txt = new LinearLayout(ctx);
        txt.setOrientation(LinearLayout.VERTICAL);
        txt.addView(text(title, 16, 0xFF43587A, true, dp(2)));
        txt.addView(text(sub, 11, 0xFF8496AE, false, 0));
        FrameLayout.LayoutParams tp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.START | Gravity.CENTER_VERTICAL);
        tp.leftMargin = dp(14);
        tp.rightMargin = dp(72);
        card.addView(txt, tp);

        final SwitchBox box = new SwitchBox(ctx, swOn(key));
        FrameLayout.LayoutParams bp = new FrameLayout.LayoutParams(
                dp(52), dp(30), Gravity.END | Gravity.CENTER_VERTICAL);
        bp.rightMargin = dp(13);
        card.addView(box, bp);

        host.bindSwitch(key, box, new Runnable() {
            @Override public void run() { refreshHero(); }
        });

        View.OnClickListener toggle = new View.OnClickListener() {
            @Override public void onClick(View v) {
                boolean z = !swOn(key);
                swSet(key, z);
                box.setChecked(z);
                press(v);
                refreshHero();
                toast(title + (z ? " 开启喵～" : " 关闭喵～"));
            }
        };
        card.setOnClickListener(toggle);
        box.setOnClickListener(toggle);
        return card;
    }

    
    private String videoSourceHint() {
        String cur = cfgGetS(K_V_SOURCE, "public");
        if ("account".equals(cur)) {
            String ck = null;
            try {
                ck = com.cathelperawa.owo.app.cookie.CookieBridge.get("bilibili");
            } catch (Throwable ignore) {
            }
            if (ck != null && ck.length() > 0) {
                return "已登录 · 可拿更高清晰度（大会员画质取决于账号）";
            }
            return "还没登录 · 到「网页登录 Cookie」登录 B站后生效";
        }
        if ("custom".equals(cur)) {
            return "优先走下面「解析接口」，失败再回落内置";
        }
        return "内置官方接口；抖音走内置 WebView 解析，不用填接口";
    }

    private String sourceHint() {
        String cur = cfgGetS(K_M_SOURCE, "public");
        if ("account".equals(cur)) {
            String ck = null;
            try {
                ck = com.cathelperawa.owo.app.cookie.CookieBridge.get("netease");
            } catch (Throwable ignore) {
            }
            if (ck != null && ck.length() > 0) {
                return "已登录 · 优先用账号票据，VIP 歌也能拿完整音质";
            }
            return "还没登录 · 到「网页登录 Cookie」登录网易云后生效";
        }
        if ("custom".equals(cur)) {
            return "优先走下面「音乐接口」，拿不到再回落官方";
        }
        return "不需要登录；VIP 歌只能拿试听或翻唱";
    }

    private View cookieRow(final String platform, String name) {
        LinearLayout card = whiteCard(dp(20), 0x8CFFFFFF);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(android.view.Gravity.CENTER_VERTICAL);
        card.setPadding(dp(14), dp(14), dp(14), dp(14));

        LinearLayout texts = new LinearLayout(ctx);
        texts.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        card.addView(texts, tlp);

        texts.addView(text(name + " Cookie", 15, 0xFF43587A, true, dp(2)));

        final TextView state = text(cookieStateText(platform), 11, 0xFF8496AE, false, 0);
        texts.addView(state);

        TextView action = text("读取 ›", 12, 0xFFE06A9A, true, 0);
        card.addView(action);

        card.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                press(v);
                state.setText("正在读取…");
                Bg.run(new Runnable() {
                    @Override public void run() {
                        final String ck;
                        try {
                            ck = com.cathelperawa.owo.app.cookie.CookieBridge.forceFetch(platform);
                        } catch (Throwable t) {
                            ui(new Runnable() {
                                @Override public void run() {
                                    state.setText("读取失败：" + t);
                                }
                            });
                            return;
                        }
                        ui(new Runnable() {
                            @Override public void run() {
                                if (ck != null && ck.length() > 0) {
                                    state.setText("已读取 " + ck.length() + " 字符 · 随时可用");
                                    toast("读到了，已存到本地");
                                } else {
                                    state.setText("没读到 · 先打开猫娘助手登录一次");
                                    toast("没读到：请先在猫娘助手「网页登录 Cookie」里登录");
                                }
                            }
                        });
                    }
                }, "cat-cookie-read");
            }
        });
        return card;
    }

    private void ui(Runnable r) {
        try {
            if (ctx instanceof android.app.Activity) {
                ((android.app.Activity) ctx).runOnUiThread(r);
                return;
            }
        } catch (Throwable ignore) {
        }
        r.run();
    }

    private String cookieStateText(String platform) {
        try {
            boolean cached = com.cathelperawa.owo.app.cookie.CookieBridge.hasCache(platform);
            if (cached) return "已读取 · 点一下可重新读";
        } catch (Throwable ignore) {
        }
        return "未读取 · 点这里从猫娘助手读取";
    }

    private interface SubText {
        String get();
    }

    private View segCard(String title, String sub, final String[] labels,
                         final String[] keys, final String key, int defIdx) {
        final String fixed = sub;
        return segCard(title, new SubText() {
            @Override public String get() { return fixed; }
        }, labels, keys, key, defIdx);
    }

    private View segCard(String title, SubText sub, final String[] labels,
                         final String[] keys, final String key, int defIdx) {
        LinearLayout card = whiteCard(dp(20), 0x8CFFFFFF);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(14), dp(14), dp(14));

        card.addView(text(title, 15, 0xFF43587A, true, dp(2)));
        final TextView subTv = text(sub.get(), 11, 0xFF8496AE, false, dp(9));
        card.addView(subTv);

        final String cur = cfgGetS(key, keys[defIdx]);
        final TextView[] segs = new TextView[labels.length];
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        for (int i = 0; i < labels.length; i++) {
            final int idx = i;
            final TextView seg = capsule(labels[i], cur.equals(keys[i]));
            seg.setLayoutParams(segLp(i, labels.length));
            seg.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    cfgSetS(key, keys[idx]);
                    for (int j = 0; j < segs.length; j++) {
                        paintCapsule(segs[j], j == idx);
                    }
                    
                    try {
                        subTv.setText(sub.get());
                    } catch (Throwable ignore) {
                    }
                    press(v);
                }
            });
            segs[i] = seg;
            row.addView(seg);
        }
        card.addView(row, matchWrap(0));
        return card;
    }

    private View multiSegCard(String title, String sub, final String[] labels,
                              final String[] keys, final String key, String[] defs) {
        LinearLayout card = whiteCard(dp(20), 0x8CFFFFFF);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(14), dp(14), dp(14));

        card.addView(text(title, 15, 0xFF43587A, true, dp(2)));
        card.addView(text(sub, 11, 0xFF8496AE, false, dp(9)));

        final String[] cur = {cfgGetS(key, join(defs))};
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        for (int i = 0; i < labels.length; i++) {
            final String k = keys[i];
            final TextView seg = capsule(labels[i], has(cur[0], k));
            seg.setLayoutParams(segLp(i, labels.length));
            seg.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    boolean now = has(cur[0], k);
                    String next = now ? remove(cur[0], k) : add(cur[0], k);
                    cur[0] = next;
                    cfgSetS(key, next);
                    paintCapsule((TextView) v, !now);
                    press(v);
                }
            });
            row.addView(seg);
        }
        card.addView(row, matchWrap(0));
        return card;
    }

    private LinearLayout.LayoutParams segLp(int idx, int total) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        if (idx > 0) lp.leftMargin = dp(3);
        if (idx < total - 1) lp.rightMargin = dp(3);
        return lp;
    }

    private View textRow(final String title, final String key, final String def,
                         String sub) {
        LinearLayout card = whiteCard(dp(20), 0x8CFFFFFF);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(12), dp(14), dp(12));

        LinearLayout top = new LinearLayout(ctx);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout left = new LinearLayout(ctx);
        left.setOrientation(LinearLayout.VERTICAL);
        left.addView(text(title, 15, 0xFF43587A, true, dp(2)));
        left.addView(text(sub, 11, 0xFF8496AE, false, 0));
        top.addView(left, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        top.addView(text("修改 ›", 12, 0xFFF07AAE, true, 0), wrapWrap());
        card.addView(top, matchWrap(dp(7)));

        final TextView val = text(cfgGetS(key, def), 11, 0xFF8496AE, false, 0);
        val.setLineSpacing(dp(2), 1f);
        card.addView(val, matchWrap(0));

        card.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                press(v);
                CatSheet.input(ctx, title, "留空恢复默认：" + def,
                        cfgGetS(key, def), new CatSheet.OnText() {
                            @Override public void run(String s) {
                                String nv = s == null ? "" : s.trim();
                                cfgSetS(key, nv.length() == 0 ? def : nv);
                                val.setText(cfgGetS(key, def));
                            }
                        });
            }
        });
        return card;
    }

    private View numRow(final String title, final String key, final String def, String sub) {
        LinearLayout card = whiteCard(dp(20), 0x8CFFFFFF);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(14), dp(14), dp(14), dp(14));

        LinearLayout left = new LinearLayout(ctx);
        left.setOrientation(LinearLayout.VERTICAL);
        left.addView(text(title, 15, 0xFF43587A, true, dp(2)));
        left.addView(text(sub, 11, 0xFF8496AE, false, 0));
        card.addView(left, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        final TextView val = text(cfgGetS(key, def), 14, 0xFFF07AAE, true, 0);
        card.addView(val, wrapWrap());

        card.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                press(v);
                CatSheet.input(ctx, title, "默认 " + def, cfgGetS(key, def),
                        new CatSheet.OnText() {
                            @Override public void run(String s) {
                                String nv = s == null ? "" : s.trim();
                                cfgSetS(key, nv.length() == 0 ? def : nv);
                                val.setText(cfgGetS(key, def));
                            }
                        });
            }
        });
        return card;
    }

    

    
    private View apiListCard(String title, String sub) {
        return apiListCard(title, sub, K_V_APIS);
    }

    private View apiListCard(String title, String sub, final String key) {
        LinearLayout card = whiteCard(dp(20), 0x8CFFFFFF);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(14), dp(14), dp(12));

        card.addView(text(title, 15, 0xFF43587A, true, dp(2)));
        card.addView(text(sub, 11, 0xFF8496AE, false, dp(9)));

        final LinearLayout list = new LinearLayout(ctx);
        list.setOrientation(LinearLayout.VERTICAL);
        card.addView(list, matchWrap(dp(6)));

        final Runnable refresh = new Runnable() {
            @Override public void run() { fillApiList(list, key); }
        };

        TextView add = text("＋ 添加接口", 13, 0xFFF07AAE, true, 0);
        add.setGravity(Gravity.CENTER);
        add.setPadding(dp(8), dp(11), dp(8), dp(11));
        GradientDrawable ag = new GradientDrawable();
        ag.setCornerRadius(dp(14));
        ag.setColor(0x33FFC2DF);
        ag.setStroke(dp(1), 0x66FF9FCB);
        add.setBackgroundDrawable(ag);
        add.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                press(v);
                CatSheet.input(ctx, "接口名字", "例如：我的解析服务", "",
                        new CatSheet.OnText() {
                            @Override public void run(String name) {
                                final String n = name == null || name.trim().length() == 0
                                        ? "接口" : name.trim();
                                CatSheet.input(ctx, "接口地址",
                                        "支持 {url} 占位，例如 http://ip:8051/api/v1/parse?url={url}",
                                        "", new CatSheet.OnText() {
                                            @Override public void run(String u) {
                                                String url = u == null ? "" : u.trim();
                                                if (url.length() == 0) return;
                                                addApi(key, n, url);
                                                refresh.run();
                                            }
                                        });
                            }
                        });
            }
        });
        card.addView(add, matchWrap(0));

        fillApiList(list, key);
        return card;
    }

    private void fillApiList(LinearLayout list, final String key) {
        if (list == null) return;
        list.removeAllViews();
        org.json.JSONArray arr = loadApis(key);
        if (arr.length() == 0) {
            TextView empty = text("还没添加 · 内置线路已经可用", 11,
                    0xFFA8B4C4, false, 0);
            empty.setPadding(dp(2), dp(4), dp(2), dp(8));
            list.addView(empty);
            return;
        }
        for (int i = 0; i < arr.length(); i++) {
            final int idx = i;
            final org.json.JSONObject o = arr.optJSONObject(i);
            if (o == null) continue;
            String n = o.optString("n", "接口" + (i + 1));
            String u = o.optString("u", "");

            LinearLayout row = new LinearLayout(ctx);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(10), dp(9), dp(8), dp(9));
            GradientDrawable rg = new GradientDrawable();
            rg.setCornerRadius(dp(14));
            rg.setColor(0x5CFFFFFF);
            row.setBackgroundDrawable(rg);

            LinearLayout left = new LinearLayout(ctx);
            left.setOrientation(LinearLayout.VERTICAL);
            TextView tn = text((idx + 1) + " · " + n, 13, 0xFF43587A, true, dp(2));
            TextView tu = text(u, 10, 0xFF8496AE, false, 0);
            tu.setSingleLine(false);
            left.addView(tn);
            left.addView(tu);
            row.addView(left, new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            TextView del = text("✕", 16, 0xFFE06A8A, true, 0);
            del.setPadding(dp(10), dp(4), dp(6), dp(4));
            row.addView(del, wrapWrap());

            row.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    press(v);
                    final String cn = o.optString("n", "");
                    final String cu = o.optString("u", "");
                    CatSheet.input(ctx, "修改接口地址", "支持 {url} 占位", cu,
                            new CatSheet.OnText() {
                                @Override public void run(String s) {
                                    String nv = s == null ? "" : s.trim();
                                    if (nv.length() == 0) return;
                                    updateApi(key, idx, cn, nv);
                                    fillApiList(list, key);
                                }
                            });
                }
            });
            del.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    press(v);
                    org.json.JSONArray a = loadApis(key);
                    org.json.JSONArray b = new org.json.JSONArray();
                    for (int j = 0; j < a.length(); j++) {
                        if (j == idx) continue;
                        b.put(a.opt(j));
                    }
                    cfgSetS(key, b.toString());
                    fillApiList(list, key);
                    toast("已删除");
                }
            });

            LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            rlp.bottomMargin = dp(6);
            list.addView(row, rlp);
        }
    }

    private org.json.JSONArray loadApis(String key) {
        try {
            String raw = cfgGetS(key, "");
            if (raw == null || raw.trim().length() == 0) return new org.json.JSONArray();
            return new org.json.JSONArray(raw);
        } catch (Throwable t) {
            return new org.json.JSONArray();
        }
    }

    private void addApi(String key, String name, String url) {
        try {
            org.json.JSONArray a = loadApis(key);
            org.json.JSONObject o = new org.json.JSONObject();
            o.put("n", name);
            o.put("u", url);
            a.put(o);
            cfgSetS(key, a.toString());
            toast("已添加「" + name + "」");
        } catch (Throwable t) {
            toast("添加失败：" + t);
        }
    }

    private void updateApi(String key, int idx, String name, String url) {
        try {
            org.json.JSONArray a = loadApis(key);
            if (idx < 0 || idx >= a.length()) return;
            org.json.JSONObject o = new org.json.JSONObject();
            o.put("n", name);
            o.put("u", url);
            a.put(idx, o);
            cfgSetS(key, a.toString());
            toast("已更新");
        } catch (Throwable t) {
            toast("更新失败：" + t);
        }
    }

    private View switchLine(String title, String sub, final String key, boolean def) {
        LinearLayout card = whiteCard(dp(20), 0x8CFFFFFF);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(14), dp(13), dp(13), dp(13));

        LinearLayout left = new LinearLayout(ctx);
        left.setOrientation(LinearLayout.VERTICAL);
        left.addView(text(title, 15, 0xFF43587A, true, dp(2)));
        left.addView(text(sub, 11, 0xFF8496AE, false, 0));
        card.addView(left, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        final SwitchBox box = new SwitchBox(ctx, cfgGetBool(key, def));
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(dp(52), dp(30));
        bp.leftMargin = dp(10);
        card.addView(box, bp);

        host.bindSwitch(key, box, new Runnable() {
            @Override public void run() { }
        });
        View.OnClickListener toggle = new View.OnClickListener() {
            @Override public void onClick(View v) {
                boolean z = !cfgGetBool(key, def);
                cfgSetS(key, z ? "1" : "0");
                box.setChecked(z);
                press(v);
            }
        };
        card.setOnClickListener(toggle);
        box.setOnClickListener(toggle);
        return card;
    }

    private boolean cfgGetBool(String key, boolean def) {
        String v = cfgGetS(key, def ? "1" : "0");
        return "1".equals(v) || "true".equalsIgnoreCase(v);
    }

    private View hintCard(String title, String body) {
        LinearLayout card = whiteCard(dp(20), 0x66FFFFFF);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        card.addView(text(title, 13, 0xFF43587A, true, dp(4)));
        TextView t = text(body, 11, 0xFF8496AE, false, 0);
        t.setLineSpacing(dp(3), 1f);
        card.addView(t, matchWrap(0));
        return card;
    }

    private View btnRow(String label, final Runnable onClick) {
        TextView btn = new TextView(ctx);
        btn.setText(label);
        btn.setTextSize(14);
        btn.setGravity(Gravity.CENTER);
        btn.setTypeface(Typeface.DEFAULT_BOLD);
        btn.setPadding(dp(14), dp(13), dp(14), dp(13));
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(dp(18));
        g.setColor(0xFFFFC2DF);
        g.setStroke(dp(1), 0xFFFF9FCB);
        btn.setBackgroundDrawable(g);
        btn.setTextColor(0xFFE85D9C);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                press(v);
                onClick.run();
            }
        });
        return btn;
    }

    private static String join(String[] a) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < a.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(a[i]);
        }
        return sb.toString();
    }

    private static boolean has(String list, String v) {
        if (list == null || list.length() == 0) return false;
        String[] a = list.split(",");
        for (String s : a) {
            if (s.trim().equals(v)) return true;
        }
        return false;
    }

    private static String add(String list, String v) {
        if (has(list, v)) return list;
        return (list == null || list.length() == 0) ? v : list + "," + v;
    }

    private static String remove(String list, String v) {
        if (list == null) return "";
        StringBuilder sb = new StringBuilder();
        for (String s : list.split(",")) {
            String t = s.trim();
            if (t.length() == 0 || t.equals(v)) continue;
            if (sb.length() > 0) sb.append(',');
            sb.append(t);
        }
        return sb.toString();
    }
}
