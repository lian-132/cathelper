package com.cathelperawa.owo.app.cookie;

import android.app.Activity;
import com.cathelperawa.owo.app.core.Bg;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class MusicHubUi {

    private MusicHubUi() {}

    private static final int C_MAIN = 0xFFEAF1FB;
    private static final int C_SUB = 0xFF9AA9BC;
    private static final int C_ACCENT = 0xFF6EA6FF;
    private static final int C_OK = 0xFF37C28D;
    private static final int C_VIP = 0xFFE85D9C;

    private static MediaPlayer player;
    private static final List<NeteaseApi.Song> QUEUE = new ArrayList<NeteaseApi.Song>();
    private static int index = -1;
    private static boolean playing = false;
    private static int durMs = 0;

    private static int mode = 0;

    private static NeteaseApi.Song nowSong;

    private static android.media.session.MediaSession session;

    private static android.animation.ValueAnimator spinAnim;

    private static float spinAngle = 0f;

    private static boolean loadingMore = false;

    private static android.widget.ScrollView lyricScroll;
    private static final List<TextView> LYRIC_VIEWS = new ArrayList<TextView>();
    private static final List<LrcLine> LRC = new ArrayList<LrcLine>();
    private static int lastLrcIdx = -1;

    private static String lastMetaLyric = "";

    private static String lrcSongId = "";

    private static long lastUserScrollAt = 0L;
    private static final long RESUME_CENTER_MS = 3000L;

    private static final class LrcLine {
        long time = 0;
        String text = "";
    }

    private static int page = 0;
    private static Activity host;

    private static android.content.Context appCtx;
    private static LinearLayout rootBox;

    private static TextView tvTitle, tvArtist, tvTime, tvVip;
    private static ImageView ivPlay, ivPrev, ivNext;
    private static SeekBar bar;
    private static ImageView cover;

    private static final Handler UI = new Handler(Looper.getMainLooper());
    private static final Runnable TICK = new Runnable() {
        @Override public void run() {
            try {
                if (player != null && playing) {
                    int pos = player.getCurrentPosition();
                    if (durMs <= 0) durMs = player.getDuration();
                    if (bar != null) {
                        bar.setMax(Math.max(durMs, 1));
                        bar.setProgress(Math.min(pos, Math.max(durMs, 1)));
                    }
                    if (tvTime != null) {
                        tvTime.setText(fmt(pos) + " / " + fmt(durMs > 0 ? durMs : 0));
                    }
                    if (page == 5) {
                        updateLyricPos(pos);
                        centerLyricIfIdle();
                    }
                    syncLyricToSession(pos);
                }
            } catch (Throwable ignore) {
            }
            UI.postDelayed(this, 500);
        }
    };

    public static void buildEntry(final Activity act, final LinearLayout box) {
        if (act == null || box == null) return;

        switch (page) {
            case 1:
                buildPlayer(act, box);
                return;
            case 2:
                buildMenu(act, box);
                return;
            case 3:
            case 4:
            case 5:

                buildPlayer(act, box);
                return;
            default:
                buildHome(act, box);
        }
    }

    public static void buildHome(final Activity act, final LinearLayout box) {
        if (act == null || box == null) return;
        page = 0;
        host = act;
        rootBox = box;
        box.removeAllViews();

        box.addView(hint(act, "点进去用你自己的账号听歌、看收藏。没登录会先带你去登录。"));

        if (nowSong != null && player != null) {
            LinearLayout now = card(act);
            LinearLayout texts = new LinearLayout(act);
            texts.setOrientation(LinearLayout.VERTICAL);
            now.addView(texts, new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            texts.addView(text(act, (playing ? "正在播放 · " : "已暂停 · ") + nowSong.name,
                    13.5f, C_OK, true));
            texts.addView(text(act, nowSong.artist, 10.5f, C_SUB, false));
            now.addView(text(act, "打开 ›", 12, C_ACCENT, true));
            now.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { buildPlayer(act, box); }
            });
            box.addView(now);
        }

        box.addView(platformCard(act, box, "netease", "网易云音乐", "云", 0xFFE5352B));
        box.addView(platformCard(act, box, "bilibili", "哔哩哔哩", "B", 0xFFFB7299));
        box.addView(panCard(act));
    }

    private static View panCard(final Activity act) {
        LinearLayout card = card(act);
        card.addView(iconView(act, "盘", 0xFF5B5BF5));

        LinearLayout texts = new LinearLayout(act);
        texts.setOrientation(LinearLayout.VERTICAL);
        card.addView(texts, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        texts.addView(text(act, "网盘下载", 15, C_MAIN, true));

        CookieStore.Entry q = CookieStore.get(act, "quark");
        CookieStore.Entry u = CookieStore.get(act, "uc");
        boolean anyLogin = (q != null && q.valid()) || (u != null && u.valid());
        TextView state = text(act, anyLogin ? "夸克 / UC · 已登录" : "夸克 / UC · 游客也能下小文件",
                11.5f, anyLogin ? C_OK : C_SUB, false);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        slp.topMargin = dp(act, 3);
        texts.addView(state, slp);

        card.addView(text(act, "进入 ›", 12, C_ACCENT, true));
        card.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                try {
                    act.startActivity(new Intent(act,
                            com.cathelperawa.owo.app.pan.PanActivity.class));
                } catch (Throwable t) {
                    Toast.makeText(act, "打开网盘页失败：" + t, Toast.LENGTH_SHORT).show();
                }
            }
        });
        return card;
    }

    private static View platformCard(final Activity act, final LinearLayout box,
                                     final String platform, final String name,
                                     String shortName, int color) {
        LinearLayout card = card(act);
        card.addView(iconView(act, shortName, color));

        LinearLayout texts = new LinearLayout(act);
        texts.setOrientation(LinearLayout.VERTICAL);
        card.addView(texts, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        texts.addView(text(act, name, 15, C_MAIN, true));

        CookieStore.Entry e = CookieStore.get(act, platform);
        TextView state = text(act, e != null
                        ? ("已登录" + (e.label.length() > 0 ? " · " + e.label : ""))
                        : "未登录 · 点这里登录",
                11.5f, e != null ? C_OK : C_SUB, false);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        slp.topMargin = dp(act, 3);
        texts.addView(state, slp);

        card.addView(text(act, e != null ? "进入 ›" : "登录 ›", 12, C_ACCENT, true));

        card.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                CookieStore.Entry cur = CookieStore.get(act, platform);
                if (cur == null) {
                    openLogin(act, platform);
                    return;
                }
                if ("netease".equals(platform)) {
                    buildPlayer(act, box);
                } else {

                    try {
                        act.startActivity(new Intent(act,
                                com.cathelperawa.owo.app.bili.BiliActivity.class));
                    } catch (Throwable t) {
                        Toast.makeText(act, "打开 B 站页失败：" + t,
                                Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
        return card;
    }

    private static void openLogin(Activity act, String platform) {
        try {
            Intent it = new Intent(act, CookieLoginActivity.class);
            it.putExtra(CookieLoginActivity.EXTRA_PLATFORM, platform);
            act.startActivity(it);
        } catch (Throwable t) {
            Toast.makeText(act, "打开登录页失败：" + t, Toast.LENGTH_SHORT).show();
        }
    }

    public static void buildPlayer(final Activity act, final LinearLayout box) {
        if (act == null || box == null) return;
        page = 1;
        host = act;
        ensureSession(act);
        rootBox = box;
        box.removeAllViews();

        LinearLayout top = new LinearLayout(act);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(dp(act, 6), dp(act, 10), dp(act, 6), dp(act, 4));

        TextView back = text(act, "‹", 26, C_MAIN, false);
        back.setPadding(dp(act, 8), 0, dp(act, 12), 0);
        back.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {

                buildHome(act, box);
            }
        });
        top.addView(back);

        final TextView title = text(act, modeName() + " ▾", 15, C_MAIN, true);
        title.setGravity(Gravity.CENTER);
        title.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { showModePicker(act, title); }
        });
        top.addView(title, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView menu = text(act, "≡", 24, C_MAIN, false);
        menu.setPadding(dp(act, 12), 0, dp(act, 8), 0);
        menu.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                buildMenu(act, box);
            }
        });
        top.addView(menu);
        box.addView(top);

        LinearLayout coverWrap = new LinearLayout(act);
        coverWrap.setGravity(Gravity.CENTER);
        coverWrap.setPadding(0, dp(act, 18), 0, dp(act, 18));
        cover = new ImageView(act);
        cover.setScaleType(ImageView.ScaleType.CENTER_CROP);
        cover.setClipToOutline(true);
        GradientDrawable cbg = new GradientDrawable();
        cbg.setShape(GradientDrawable.OVAL);
        cbg.setColor(0x33FFFFFF);
        cover.setBackground(cbg);
        int size = dp(act, 240);
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(size, size);
        cover.setLayoutParams(clp);
        coverWrap.addView(cover);

        cover.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (nowSong != null && player != null) buildLyric(act, box);
                else randomPlay(act);
            }
        });
        setupSpin();
        box.addView(coverWrap);

        LinearLayout nameRow = new LinearLayout(act);
        nameRow.setOrientation(LinearLayout.HORIZONTAL);
        nameRow.setGravity(Gravity.CENTER_VERTICAL);
        tvTitle = text(act, "点封面随机放一首", 19, C_MAIN, true);
        tvTitle.setMaxLines(2);
        nameRow.addView(tvTitle, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        tvVip = text(act, "", 10, C_VIP, true);
        tvVip.setPadding(dp(act, 6), dp(act, 2), dp(act, 6), dp(act, 2));
        GradientDrawable vbg = new GradientDrawable();
        vbg.setCornerRadius(dp(act, 6));
        vbg.setColor(0x33E85D9C);
        tvVip.setBackground(vbg);
        LinearLayout.LayoutParams vlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        vlp.leftMargin = dp(act, 8);
        nameRow.addView(tvVip, vlp);
        LinearLayout.LayoutParams nrlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        nrlp.topMargin = dp(act, 8);
        nrlp.leftMargin = dp(act, 8);
        box.addView(nameRow, nrlp);

        tvArtist = text(act, "", 12.5f, C_SUB, false);
        LinearLayout.LayoutParams alp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        alp.topMargin = dp(act, 5);
        alp.leftMargin = dp(act, 8);
        box.addView(tvArtist, alp);

        bar = new SeekBar(act);
        bar.setMax(1000);
        bar.setPadding(dp(act, 4), 0, dp(act, 4), 0);
        try {
            bar.getProgressDrawable().setColorFilter(C_ACCENT, PorterDuff.Mode.SRC_IN);
            bar.getThumb().setColorFilter(C_MAIN, PorterDuff.Mode.SRC_IN);
        } catch (Throwable ignore) {
        }
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar sb, int p, boolean fromUser) {
                if (fromUser && player != null && durMs > 0) {
                    try { player.seekTo(p); } catch (Throwable ignore) { }
                }
            }
            @Override public void onStartTrackingTouch(SeekBar sb) { }
            @Override public void onStopTrackingTouch(SeekBar sb) { }
        });
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        blp.topMargin = dp(act, 10);
        box.addView(bar, blp);

        tvTime = text(act, "00:00 / 00:00", 10.5f, C_SUB, false);
        tvTime.setGravity(Gravity.CENTER);
        box.addView(tvTime, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout ctrl = new LinearLayout(act);
        ctrl.setOrientation(LinearLayout.HORIZONTAL);
        ctrl.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams ctlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ctlp.topMargin = dp(act, 14);
        ctlp.bottomMargin = dp(act, 16);

        ivPrev = new ImageView(act);
        ivPrev.setImageResource(com.cathelperawa.owo.app.R.drawable.ic_prev);
        ivPrev.setColorFilter(C_MAIN);
        ivPrev.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { prevSong(act); }
        });
        ctrl.addView(ivPrev, new LinearLayout.LayoutParams(0, dp(act, 26), 1f));

        ivPlay = new ImageView(act);
        ivPlay.setImageResource(com.cathelperawa.owo.app.R.drawable.ic_play);
        ivPlay.setColorFilter(C_MAIN);
        ivPlay.setPadding(dp(act, 18), dp(act, 18), dp(act, 18), dp(act, 18));
        GradientDrawable pbg = new GradientDrawable();
        pbg.setShape(GradientDrawable.OVAL);
        pbg.setColor(0x33FFFFFF);
        ivPlay.setBackground(pbg);
        ivPlay.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (player == null) randomPlay(act);
                else togglePause();
            }
        });
        int psz = dp(act, 64);
        ctrl.addView(ivPlay, new LinearLayout.LayoutParams(psz, psz));

        ivNext = new ImageView(act);
        ivNext.setImageResource(com.cathelperawa.owo.app.R.drawable.ic_next);
        ivNext.setColorFilter(C_MAIN);
        ivNext.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { nextSong(act); }
        });
        ctrl.addView(ivNext, new LinearLayout.LayoutParams(0, dp(act, 26), 1f));

        TextView list = text(act, "☰", 20, C_SUB, false);
        list.setGravity(Gravity.CENTER);
        list.setPadding(dp(act, 10), 0, dp(act, 10), 0);
        list.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { buildQueue(act, box); }
        });
        ctrl.addView(list, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        box.addView(ctrl, ctlp);

        restorePlayerUi(act);
    }

    private static void restorePlayerUi(final Activity act) {
        try {
            if (nowSong == null || player == null) return;
            if (tvTitle != null) tvTitle.setText(nowSong.name);
            if (tvArtist != null) tvArtist.setText(nowSong.artist);
            loadCover(nowSong.cover);
            if (ivPlay != null) {
                ivPlay.setImageResource(playing
                        ? com.cathelperawa.owo.app.R.drawable.ic_pause
                        : com.cathelperawa.owo.app.R.drawable.ic_play);
            }
            durMs = player.getDuration();
            if (bar != null && durMs > 0) {
                bar.setMax(durMs);
                bar.setProgress(player.getCurrentPosition());
            }
            if (tvTime != null) {
                tvTime.setText(fmt(player.getCurrentPosition()) + " / " + fmt(durMs));
            }
            if (playing) {
                UI.removeCallbacks(TICK);
                UI.post(TICK);
            }
        } catch (Throwable ignore) {
        }
    }

    public static void buildMenu(final Activity act, final LinearLayout box) {
        if (act == null || box == null) return;
        page = 2;
        host = act;
        rootBox = box;
        box.removeAllViews();

        LinearLayout top = new LinearLayout(act);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(dp(act, 6), dp(act, 10), dp(act, 6), dp(act, 10));
        TextView back = text(act, "‹", 26, C_MAIN, false);
        back.setPadding(dp(act, 8), 0, dp(act, 12), 0);
        back.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { buildPlayer(act, box); }
        });
        top.addView(back);
        top.addView(text(act, "我的", 15, C_MAIN, true), new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        box.addView(top);

        final LinearLayout accCard = card(act);
        accCard.setOrientation(LinearLayout.VERTICAL);
        accCard.addView(text(act, "正在读取账号…", 12, C_SUB, false));
        box.addView(accCard);

        Bg.run(new Runnable() {
            @Override public void run() {
                final NeteaseApi.Account acc = NeteaseApi.account(act);
                act.runOnUiThread(new Runnable() {
                    @Override public void run() {
                        accCard.removeAllViews();
                        if (acc == null) {
                            accCard.addView(text(act, "没读到账号（登录可能过期了）", 12, C_SUB, false));
                            return;
                        }
                        LinearLayout line1 = new LinearLayout(act);
                        line1.setOrientation(LinearLayout.HORIZONTAL);
                        line1.setGravity(Gravity.CENTER_VERTICAL);
                        line1.addView(text(act, acc.nickname.length() > 0 ? acc.nickname : "网易云用户",
                                17, C_MAIN, true));
                        TextView vip = text(act, acc.vipText(), 10, C_VIP, true);
                        vip.setPadding(dp(act, 6), dp(act, 2), dp(act, 6), dp(act, 2));
                        GradientDrawable vbg = new GradientDrawable();
                        vbg.setCornerRadius(dp(act, 6));
                        vbg.setColor(0x33E85D9C);
                        vip.setBackground(vbg);
                        LinearLayout.LayoutParams vlp = new LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                        vlp.leftMargin = dp(act, 8);
                        line1.addView(vip, vlp);
                        accCard.addView(line1);

                        accCard.addView(text(act, "UID " + acc.uid
                                        + (acc.level > 0 ? " · Lv." + acc.level : ""),
                                11, C_SUB, false));
                        if (acc.vipType <= 0) {
                            accCard.addView(text(act, "提示：VIP 歌只能拿到 30 秒试听", 10.5f, C_SUB, false));
                        }
                    }
                });
            }
        }, "cat-netease-acc");

        LinearLayout searchRow = new LinearLayout(act);
        searchRow.setOrientation(LinearLayout.HORIZONTAL);
        searchRow.setGravity(Gravity.CENTER_VERTICAL);
        final EditText et = new EditText(act);
        et.setHint("搜歌 / 歌手");
        et.setTextSize(13);
        et.setSingleLine(true);
        et.setTextColor(C_MAIN);
        et.setHintTextColor(0xFF6F7D90);
        et.setBackgroundResource(com.cathelperawa.owo.app.R.drawable.bg_card);
        et.setPadding(dp(act, 12), dp(act, 8), dp(act, 12), dp(act, 8));
        searchRow.addView(et, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView searchBtn = text(act, "搜索", 13, C_ACCENT, true);
        searchBtn.setPadding(dp(act, 14), dp(act, 10), dp(act, 6), dp(act, 10));
        searchRow.addView(searchBtn);
        LinearLayout.LayoutParams srp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        srp.topMargin = dp(act, 10);
        srp.bottomMargin = dp(act, 8);
        box.addView(searchRow, srp);

        final LinearLayout resultBox = new LinearLayout(act);
        resultBox.setOrientation(LinearLayout.VERTICAL);
        box.addView(resultBox);

        searchBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                final String kw = et.getText() == null ? "" : et.getText().toString().trim();
                if (kw.length() == 0) return;
                resultBox.removeAllViews();
                resultBox.addView(text(act, "搜索中…", 11.5f, C_SUB, false));
                Bg.run(new Runnable() {
                    @Override public void run() {
                        final List<NeteaseApi.Song> list = NeteaseApi.search(act, kw, 30);
                        act.runOnUiThread(new Runnable() {
                            @Override public void run() {
                                resultBox.removeAllViews();
                                if (list.isEmpty()) {
                                    resultBox.addView(text(act, "没搜到「" + kw + "」", 11.5f, C_SUB, false));
                                    return;
                                }
                                resultBox.addView(sectionTitle(act, "搜索结果 · " + list.size()));
                                for (final NeteaseApi.Song s : list) {
                                    resultBox.addView(songRow(act, s, list));
                                }
                            }
                        });
                    }
                }, "cat-netease-search");
            }
        });

        final LinearLayout plBox = new LinearLayout(act);
        plBox.setOrientation(LinearLayout.VERTICAL);
        box.addView(plBox);
        plBox.addView(text(act, "正在加载歌单…", 11.5f, C_SUB, false));

        Bg.run(new Runnable() {
            @Override public void run() {
                final NeteaseApi.Account acc = NeteaseApi.account(act);
                final List<NeteaseApi.Playlist> pls = acc == null
                        ? new ArrayList<NeteaseApi.Playlist>()
                        : NeteaseApi.playlists(act, acc.uid);
                act.runOnUiThread(new Runnable() {
                    @Override public void run() {
                        plBox.removeAllViews();
                        if (pls.isEmpty()) {
                            plBox.addView(text(act, "没读到歌单（可能登录过期了）", 11.5f, C_SUB, false));
                            return;
                        }
                        plBox.addView(sectionTitle(act, "我的歌单 · " + pls.size()));
                        for (final NeteaseApi.Playlist p : pls) {
                            plBox.addView(playlistRow(act, box, p));
                        }
                    }
                });
            }
        }, "cat-netease-pl");
    }

    public static void buildQueue(final Activity act, final LinearLayout box) {
        page = 4;
        box.removeAllViews();

        LinearLayout top = new LinearLayout(act);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(dp(act, 6), dp(act, 10), dp(act, 6), dp(act, 10));
        TextView back = text(act, "‹", 26, C_MAIN, false);
        back.setPadding(dp(act, 8), 0, dp(act, 12), 0);
        back.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { buildPlayer(act, box); }
        });
        top.addView(back);
        top.addView(text(act, "随机歌单 · " + QUEUE.size(), 15, C_MAIN, true),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        box.addView(top);

        if (QUEUE.isEmpty()) {
            box.addView(text(act, "还没放歌（回播放器点一下封面）", 11.5f, C_SUB, false));
            return;
        }
        for (int i = 0; i < QUEUE.size(); i++) {
            final int idx = i;
            box.addView(songRowIdx(act, QUEUE.get(i), idx));
        }
    }

    public static void buildPlaylist(final Activity act, final LinearLayout box,
                                     final NeteaseApi.Playlist p) {
        page = 3;
        box.removeAllViews();

        LinearLayout top = new LinearLayout(act);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(dp(act, 6), dp(act, 10), dp(act, 6), dp(act, 10));
        TextView back = text(act, "‹", 26, C_MAIN, false);
        back.setPadding(dp(act, 8), 0, dp(act, 12), 0);
        back.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { buildMenu(act, box); }
        });
        top.addView(back);
        String nm = p.specialType == 5 ? "我喜欢的音乐" : p.name;
        top.addView(text(act, nm, 15, C_MAIN, true), new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        box.addView(top);

        final LinearLayout listBox = new LinearLayout(act);
        listBox.setOrientation(LinearLayout.VERTICAL);
        box.addView(listBox);
        listBox.addView(text(act, "加载中…", 11.5f, C_SUB, false));

        Bg.run(new Runnable() {
            @Override public void run() {
                final List<NeteaseApi.Song> list = NeteaseApi.playlistTracks(act, p.id, 100);
                act.runOnUiThread(new Runnable() {
                    @Override public void run() {
                        listBox.removeAllViews();
                        if (list.isEmpty()) {
                            listBox.addView(text(act, "这个歌单没读到歌（可能需要会员或登录过期）",
                                    11.5f, C_SUB, false));
                            return;
                        }
                        listBox.addView(sectionTitle(act, "共 " + list.size() + " 首 · 点一首就放"));
                        for (final NeteaseApi.Song s : list) {
                            listBox.addView(songRow(act, s, list));
                        }
                    }
                });
            }
        }, "cat-netease-tracks");
    }

    private static String modeName() {
        return mode == 1 ? "私人漫游" : "每日推荐";
    }

    private static void showModePicker(final Activity act, final TextView title) {
        final android.app.Dialog dlg = new android.app.Dialog(act,
                android.R.style.Theme_Translucent_NoTitleBar);
        LinearLayout card = new LinearLayout(act);
        card.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(act, 18);
        card.setPadding(pad, dp(act, 16), pad, dp(act, 10));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xF01B2533);
        bg.setCornerRadius(dp(act, 22));
        bg.setStroke(dp(act, 1), 0x33FFFFFF);
        card.setBackgroundDrawable(bg);
        card.setElevation(dp(act, 12));

        card.addView(text(act, "播放模式", 16, C_MAIN, true));

        final TextView[] rows = new TextView[2];
        for (int i = 0; i < 2; i++) {
            final int idx = i;
            LinearLayout row = new LinearLayout(act);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            GradientDrawable rbg = new GradientDrawable();
            rbg.setCornerRadius(dp(act, 14));
            rbg.setColor(idx == mode ? 0x33E06A9A : 0x14FFFFFF);
            row.setBackgroundDrawable(rbg);
            row.setPadding(dp(act, 14), dp(act, 12), dp(act, 14), dp(act, 12));
            LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            rlp.topMargin = dp(act, 10);
            row.setLayoutParams(rlp);

            LinearLayout texts = new LinearLayout(act);
            texts.setOrientation(LinearLayout.VERTICAL);
            row.addView(texts, new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            texts.addView(text(act, idx == 0 ? "每日推荐" : "私人漫游", 14.5f,
                    idx == mode ? C_MAIN : C_MAIN, true));
            texts.addView(text(act, idx == 0 ? "当天固定 30 首，第二天换一批"
                            : "每次都不一样，适合随手听",
                    10.5f, C_SUB, false));
            row.addView(text(act, idx == mode ? "●" : "○", 14,
                    idx == mode ? 0xFFE06A9A : 0xFF6F7D90, true));

            row.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    mode = idx;
                    if (title != null) title.setText(modeName() + " ▾");
                    QUEUE.clear();
                    index = -1;
                    try { dlg.dismiss(); } catch (Throwable ignore) { }
                    randomPlay(act);
                }
            });
            card.addView(row);
        }

        TextView cancel = text(act, "取消", 13, C_SUB, false);
        cancel.setGravity(Gravity.CENTER);
        cancel.setPadding(0, dp(act, 12), 0, dp(act, 10));
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                try { dlg.dismiss(); } catch (Throwable ignore) { }
            }
        });
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        clp.topMargin = dp(act, 6);
        card.addView(cancel, clp);

        dlg.setContentView(card);
        dlg.setCanceledOnTouchOutside(true);
        dlg.show();
        android.view.Window w = dlg.getWindow();
        if (w == null) return;
        w.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        android.view.WindowManager.LayoutParams lp = w.getAttributes();
        lp.width = (int) (act.getResources().getDisplayMetrics().widthPixels * 0.86f);
        lp.height = android.view.WindowManager.LayoutParams.WRAP_CONTENT;
        lp.gravity = Gravity.CENTER;
        w.setAttributes(lp);
    }

    public static void autoPlay(final Activity act) {
        try {
            if (act != null) {
                if (player != null && nowSong != null) {
                    if (!playing) togglePause();
                    return;
                }
                randomPlay(act);
                return;
            }

            if (appCtx != null) {
                android.content.Intent it = new android.content.Intent(appCtx,
                        com.cathelperawa.owo.app.MainActivity.class);
                it.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                        | android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP);
                it.putExtra("cat_auto_play", true);
                appCtx.startActivity(it);
            }
        } catch (Throwable t) {
            android.util.Log.w("CatMusic", "autoPlay 失败: " + t);
        }
    }

    private static void randomPlay(final Activity act) {
        if (tvTitle != null) tvTitle.setText("正在挑歌…");
        Bg.run(new Runnable() {
            @Override public void run() {

                List<NeteaseApi.Song> list = (mode == 1)
                        ? NeteaseApi.radioGet(act)
                        : NeteaseApi.dailyRecommend(act);
                if (list.isEmpty()) list = NeteaseApi.recommendNew(act, 30);
                if (list.isEmpty()) {
                    act.runOnUiThread(new Runnable() {
                        @Override public void run() {
                            if (tvTitle != null) tvTitle.setText("没拿到推荐（网络或登录态问题）");
                        }
                    });
                    return;
                }
                QUEUE.clear();
                QUEUE.addAll(list);
                index = new Random().nextInt(QUEUE.size());
                playAt(act, index);
            }
        }, "cat-netease-rand");
    }

    private static void nextSong(final Activity act) {
        if (QUEUE.isEmpty()) { randomPlay(act); return; }
        final int nextIdx = index + 1;
        if (nextIdx >= QUEUE.size() && mode == 1 && !loadingMore) {
            loadingMore = true;
            Bg.run(new Runnable() {
                @Override public void run() {
                    try {
                        List<NeteaseApi.Song> more = NeteaseApi.radioGet(act);
                        appendUnique(more);

                        if (QUEUE.size() <= nextIdx) {
                            NeteaseApi.Song cur = QUEUE.get(Math.max(0, Math.min(index, QUEUE.size() - 1)));
                            appendUnique(NeteaseApi.similarSongs(act, cur.id, 20));
                        }
                    } catch (Throwable ignore) {
                    } finally {
                        loadingMore = false;
                    }
                    act.runOnUiThread(new Runnable() {
                        @Override public void run() {
                            if (QUEUE.size() > nextIdx) {
                                index = nextIdx;
                            } else {
                                index = 0;
                            }
                            playAt(act, index);
                        }
                    });
                }
            }, "cat-radio-more");
            return;
        }
        index = nextIdx % QUEUE.size();
        playAt(act, index);
    }

    private static void appendUnique(List<NeteaseApi.Song> more) {
        if (more == null || more.isEmpty()) return;
        synchronized (QUEUE) {
            for (NeteaseApi.Song s : more) {
                boolean dup = false;
                for (NeteaseApi.Song x : QUEUE) {
                    if (x.id.equals(s.id)) { dup = true; break; }
                }
                if (!dup) QUEUE.add(s);
            }
        }
    }

    private static void prevSong(final Activity act) {
        if (QUEUE.isEmpty()) { randomPlay(act); return; }
        index = (index - 1 + QUEUE.size()) % QUEUE.size();
        playAt(act, index);
    }

    private static void playAt(final Activity act, final int idx) {
        if (idx < 0 || idx >= QUEUE.size()) return;
        final NeteaseApi.Song s = QUEUE.get(idx);
        nowSong = s;

        final Runnable uiPart = new Runnable() {
            @Override public void run() {
                if (tvTitle != null) tvTitle.setText(s.name);
                if (tvArtist != null) tvArtist.setText(s.artist);
                if (tvTime != null) tvTime.setText("正在取直链…");
                if (tvVip != null) tvVip.setText("");
                loadCover(s.cover);

                syncSongMeta(s);
            }
        };
        if (Looper.myLooper() == Looper.getMainLooper()) uiPart.run();
        else UI.post(uiPart);

        Bg.run(new Runnable() {
            @Override public void run() {
                try {
                    String raw = NeteaseApi.lyric(act, s.id);
                    if (raw != null && raw.length() > 0) {
                        parseLrc(raw);
                        lrcSongId = s.id;
                        lastMetaLyric = "";
                    } else {
                        LRC.clear();
                        lrcSongId = "";
                    }
                } catch (Throwable ignore) {
                }
            }
        }, "cat-netease-lrc-pre");

        Bg.run(new Runnable() {
            @Override public void run() {
                final String url = NeteaseApi.songUrl(act, s.id);
                act.runOnUiThread(new Runnable() {
                    @Override public void run() {
                        if (url == null) {
                            if (tvTime != null) tvTime.setText("拿不到直链（VIP 歌要登录/会员）");
                            return;
                        }
                        startPlay(act, url);
                    }
                });
            }
        }, "cat-netease-url");
    }

    private static void ensureNotiPermission(Activity act) {
        try {
            if (act == null) return;
            if (android.os.Build.VERSION.SDK_INT < 33) return;
            if (act.checkSelfPermission("android.permission.POST_NOTIFICATIONS")
                    == android.content.pm.PackageManager.PERMISSION_GRANTED) return;
            act.requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 0x3301);
        } catch (Throwable ignore) {
        }
    }

    private static void startPlay(final Activity act, String rawUrl) {
        try {
            ensureNotiPermission(act);
            releasePlayerOnly();

            String url = rawUrl == null ? "" : rawUrl.trim();
            if (url.startsWith("http://")) url = "https://" + url.substring(7);
            String hostName = url;
            int p1 = hostName.indexOf("://");
            if (p1 > 0) hostName = hostName.substring(p1 + 3);
            int p2 = hostName.indexOf('/');
            if (p2 > 0) hostName = hostName.substring(0, p2);
            android.util.Log.i("CatMusic", "播放 host=" + hostName + " len=" + url.length());
            player = new MediaPlayer();
            java.util.Map<String, String> headers = new java.util.HashMap<String, String>();
            headers.put("User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                            + "(KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36");
            headers.put("Referer", "https://music.163.com/");
            player.setDataSource(act, android.net.Uri.parse(url), headers);
            player.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override public void onPrepared(MediaPlayer mp) {
                    mp.start();
                    playing = true;
                    durMs = mp.getDuration();
                    if (ivPlay != null) ivPlay.setImageResource(com.cathelperawa.owo.app.R.drawable.ic_pause);
                    UI.removeCallbacks(TICK);
                    UI.post(TICK);
                    updateSession(true);

                    resumeSpin();

                    MusicPlaybackService.start(host, nowSong == null ? "" : nowSong.name,
                            nowSong == null ? "" : nowSong.artist, true, session);
                }
            });
            player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override public void onCompletion(MediaPlayer mp) {
                    if (host != null) nextSong(host);
                }
            });
            player.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override public boolean onError(MediaPlayer mp, int what, int extra) {
                    playing = false;
                    if (ivPlay != null) ivPlay.setImageResource(com.cathelperawa.owo.app.R.drawable.ic_play);
                    if (tvTime != null) tvTime.setText("播放失败（" + what + "/" + extra + "）"
                            + (extra == -38 ? " · 格式或防盗链" : ""));
                    return true;
                }
            });
            player.prepareAsync();
        } catch (Throwable t) {
            if (tvTime != null) tvTime.setText("播放失败：" + t);
        }
    }

    private static void togglePause() {
        try {
            if (player == null) return;
            if (player.isPlaying()) {
                player.pause();
                playing = false;
                if (ivPlay != null) ivPlay.setImageResource(com.cathelperawa.owo.app.R.drawable.ic_play);
                pauseSpin();
                updateSession(false);
                MusicPlaybackService.update(host, nowSong == null ? "" : nowSong.name,
                        nowSong == null ? "" : nowSong.artist, false);
            } else {
                player.start();
                playing = true;
                if (ivPlay != null) ivPlay.setImageResource(com.cathelperawa.owo.app.R.drawable.ic_pause);
                resumeSpin();
                updateSession(true);
                MusicPlaybackService.update(host, nowSong == null ? "" : nowSong.name,
                        nowSong == null ? "" : nowSong.artist, true);
            }
        } catch (Throwable ignore) {
        }
    }

    public static boolean handleBack(Activity act) {
        if (act == null || rootBox == null) return false;
        if (closeLyricIfOpen()) return true;
        switch (page) {
            case 3:
                buildMenu(act, rootBox);
                return true;
            case 5:
                buildPlayer(act, rootBox);
                return true;
            case 4:
                buildPlayer(act, rootBox);
                return true;
            case 2:
                buildPlayer(act, rootBox);
                return true;
            case 1:

                buildHome(act, rootBox);
                return true;
            default:
                return false;
        }
    }

    public static void initSession(final Activity act) {
        try {
            ensureSession(act);
            if (player == null) setIdleSession();
        } catch (Throwable ignore) {
        }
    }

    private static void setIdleSession() {
        try {
            if (session == null) return;
            long actions = android.media.session.PlaybackState.ACTION_PLAY
                    | android.media.session.PlaybackState.ACTION_PAUSE
                    | android.media.session.PlaybackState.ACTION_PLAY_PAUSE
                    | android.media.session.PlaybackState.ACTION_SKIP_TO_NEXT
                    | android.media.session.PlaybackState.ACTION_SKIP_TO_PREVIOUS;
            session.setPlaybackState(new android.media.session.PlaybackState.Builder()
                    .setActions(actions)
                    .setState(android.media.session.PlaybackState.STATE_PAUSED, 0L, 1.0f)
                    .build());
            session.setMetadata(new android.media.MediaMetadata.Builder()
                    .putString(android.media.MediaMetadata.METADATA_KEY_TITLE, "猫娘助手")
                    .putString(android.media.MediaMetadata.METADATA_KEY_ARTIST, "点播放随便听听")
                    .build());
            session.setActive(true);
        } catch (Throwable ignore) {
        }
    }

    private static void ensureSession(Activity act) {
        if (session != null || act == null) return;
        try {
            appCtx = act.getApplicationContext();
            session = new android.media.session.MediaSession(act, "CatHelperMusic");
            session.setFlags(android.media.session.MediaSession.FLAG_HANDLES_MEDIA_BUTTONS
                    | android.media.session.MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);
            session.setCallback(new android.media.session.MediaSession.Callback() {
                @Override public void onPlay() {
                    try {
                        if (player != null && !player.isPlaying()) {
                            togglePause();
                            return;
                        }
                        if (player != null) return;

                        autoPlay(host);
                    } catch (Throwable t) {
                        android.util.Log.w("CatMusic", "onPlay 失败: " + t);
                    }
                }

                @Override public void onPause() {
                    try {
                        if (player != null && player.isPlaying()) togglePause();
                    } catch (Throwable ignore) {
                    }
                }

                @Override public void onSkipToNext() {
                    if (host != null) nextSong(host);
                }

                @Override public void onSkipToPrevious() {
                    if (host != null) prevSong(host);
                }

                @Override public void onSeekTo(long pos) {
                    try {
                        if (player != null) player.seekTo((int) pos);
                    } catch (Throwable ignore) {
                    }
                }
            });
            session.setActive(true);
        } catch (Throwable t) {
            android.util.Log.w("CatMusic", "MediaSession 创建失败: " + t);
        }
    }

    private static void updateSession(boolean isPlaying) {
        try {
            if (session == null) return;
            long actions = android.media.session.PlaybackState.ACTION_PLAY
                    | android.media.session.PlaybackState.ACTION_PAUSE
                    | android.media.session.PlaybackState.ACTION_PLAY_PAUSE
                    | android.media.session.PlaybackState.ACTION_SKIP_TO_NEXT
                    | android.media.session.PlaybackState.ACTION_SKIP_TO_PREVIOUS
                    | android.media.session.PlaybackState.ACTION_SEEK_TO;
            int state = isPlaying
                    ? android.media.session.PlaybackState.STATE_PLAYING
                    : android.media.session.PlaybackState.STATE_PAUSED;
            long pos = 0;
            try { pos = player == null ? 0 : player.getCurrentPosition(); } catch (Throwable ignore) { }
            android.media.session.PlaybackState ps =
                    new android.media.session.PlaybackState.Builder()
                            .setActions(actions)
                            .setState(state, pos, 1.0f)
                            .build();
            session.setPlaybackState(ps);

            if (nowSong != null) {
                android.media.MediaMetadata.Builder mb = new android.media.MediaMetadata.Builder()
                        .putString(android.media.MediaMetadata.METADATA_KEY_TITLE, nowSong.name)
                        .putString(android.media.MediaMetadata.METADATA_KEY_ARTIST, nowSong.artist)
                        .putLong(android.media.MediaMetadata.METADATA_KEY_DURATION, durMs > 0 ? durMs : 0);
                if (cover != null && cover.getDrawable() instanceof android.graphics.drawable.BitmapDrawable) {
                    try {
                        mb.putBitmap(android.media.MediaMetadata.METADATA_KEY_ALBUM_ART,
                                ((android.graphics.drawable.BitmapDrawable) cover.getDrawable()).getBitmap());
                    } catch (Throwable ignore) {
                    }
                }
                session.setMetadata(mb.build());
            }
        } catch (Throwable t) {
            android.util.Log.w("CatMusic", "updateSession 失败: " + t);
        }
    }

    private static void setupSpin() {
        try {
            if (spinAnim != null) {
                spinAnim.cancel();
                spinAnim = null;
            }
            if (cover == null) return;
            final float base = spinAngle;
            cover.setRotation(spinAngle % 360f);

            spinAnim = android.animation.ValueAnimator.ofFloat(0f, 360f);
            spinAnim.setDuration(20000L);
            spinAnim.setRepeatCount(android.animation.ValueAnimator.INFINITE);
            spinAnim.setInterpolator(new android.view.animation.LinearInterpolator());
            spinAnim.addUpdateListener(new android.animation.ValueAnimator.AnimatorUpdateListener() {
                @Override public void onAnimationUpdate(android.animation.ValueAnimator a) {
                    try {
                        Object v = a.getAnimatedValue();
                        if (!(v instanceof Float)) return;
                        spinAngle = base + (Float) v;
                        if (cover != null) cover.setRotation(spinAngle % 360f);
                    } catch (Throwable ignore) {
                    }
                }
            });
            if (playing) spinAnim.start();
        } catch (Throwable ignore) {
        }
    }

    private static void resumeSpin() {
        try {
            if (spinAnim == null) {
                setupSpin();
                return;
            }
            if (!spinAnim.isStarted()) spinAnim.start();
            else spinAnim.resume();
        } catch (Throwable ignore) {
        }
    }

    private static void pauseSpin() {
        try {
            if (spinAnim != null && spinAnim.isStarted()) spinAnim.pause();
        } catch (Throwable ignore) {
        }
    }

    private static android.app.Dialog lyricDlg;

    private static void dismissLyricDialog() {
        try {
            if (lyricDlg != null && lyricDlg.isShowing()) lyricDlg.dismiss();
        } catch (Throwable ignore) {
        }
        lyricDlg = null;
    }

    public static boolean closeLyricIfOpen() {
        try {
            if (lyricDlg != null && lyricDlg.isShowing()) {
                lyricDlg.dismiss();
                return true;
            }
        } catch (Throwable ignore) {
        }
        return false;
    }

    public static void buildLyric(final Activity act, final LinearLayout box) {
        if (act == null) return;
        host = act;
        if (box != null) rootBox = box;
        page = 5;
        dismissLyricDialog();

        try {
            final android.app.Dialog dlg = new android.app.Dialog(act,
                    android.R.style.Theme_Translucent_NoTitleBar_Fullscreen);
            lyricDlg = dlg;

            LinearLayout root = new LinearLayout(act);
            root.setOrientation(LinearLayout.VERTICAL);
            root.setBackgroundColor(0x88000000);

            LinearLayout top = new LinearLayout(act);
            top.setOrientation(LinearLayout.HORIZONTAL);
            top.setGravity(Gravity.CENTER_VERTICAL);
            top.setPadding(dp(act, 10), dp(act, 16), dp(act, 10), dp(act, 8));
            TextView back = text(act, "‹", 26, C_MAIN, false);
            back.setPadding(dp(act, 8), 0, dp(act, 12), 0);
            back.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    try { dlg.dismiss(); } catch (Throwable ignore) { }
                }
            });
            top.addView(back);
            TextView title = text(act, "歌词", 15, C_MAIN, true);
            top.addView(title, new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            root.addView(top);

            if (nowSong != null) {
                TextView sub = text(act, nowSong.name + (nowSong.artist.length() > 0
                        ? "  ·  " + nowSong.artist : ""), 12, C_SUB, false);
                LinearLayout.LayoutParams slp0 = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                slp0.leftMargin = dp(act, 12);
                slp0.bottomMargin = dp(act, 6);
                root.addView(sub, slp0);
            }

            lyricScroll = new android.widget.ScrollView(act);
            lyricScroll.setVerticalScrollBarEnabled(false);
            lyricScroll.setFillViewport(false);

            LinearLayout lines = new LinearLayout(act);
            lines.setOrientation(LinearLayout.VERTICAL);
            int half = (int) (act.getResources().getDisplayMetrics().heightPixels * 0.40f);
            lines.setPadding(0, half, 0, half);
            lyricScroll.addView(lines);
            root.addView(lyricScroll, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

            lines.addView(text(act, "歌词加载中…", 13, C_SUB, false));

            final android.view.GestureDetector gd = new android.view.GestureDetector(act,
                    new android.view.GestureDetector.SimpleOnGestureListener() {
                        @Override public boolean onSingleTapUp(android.view.MotionEvent e) {
                            try { dlg.dismiss(); } catch (Throwable ignore) { }
                            return true;
                        }
                    });
            lyricScroll.setOnTouchListener(new View.OnTouchListener() {
                @Override public boolean onTouch(View v, android.view.MotionEvent ev) {
                    int a = ev.getActionMasked();
                    if (a == android.view.MotionEvent.ACTION_DOWN
                            || a == android.view.MotionEvent.ACTION_MOVE
                            || a == android.view.MotionEvent.ACTION_UP
                            || a == android.view.MotionEvent.ACTION_CANCEL) {
                        lastUserScrollAt = System.currentTimeMillis();
                    }
                    gd.onTouchEvent(ev);
                    return false;
                }
            });

            dlg.setOnDismissListener(new android.content.DialogInterface.OnDismissListener() {
                @Override public void onDismiss(android.content.DialogInterface d) {
                    lyricDlg = null;
                    lyricScroll = null;
                    if (page == 5) page = 1;
                }
            });

            dlg.setContentView(root);
            dlg.setCanceledOnTouchOutside(false);
            dlg.show();

            final LinearLayout target = lines;
            final String wantId = nowSong == null ? "" : nowSong.id;
            boolean reused = wantId.length() > 0 && wantId.equals(lrcSongId) && !LRC.isEmpty();

            LYRIC_VIEWS.clear();
            lastLrcIdx = -1;

            if (reused) {
                renderLyric(act, target, true);
                return;
            }

            LRC.clear();
            lrcSongId = "";
            Bg.run(new Runnable() {
                @Override public void run() {
                    final String raw = NeteaseApi.lyric(act, wantId);
                    parseLrc(raw);
                    final boolean hasRaw = raw != null && raw.length() > 0;
                    lrcSongId = hasRaw ? wantId : "";
                    act.runOnUiThread(new Runnable() {
                        @Override public void run() {
                            renderLyric(act, target, hasRaw);
                        }
                    });
                }
            }, "cat-netease-lrc");
        } catch (Throwable t) {
            android.util.Log.w("CatMusic", "歌词弹层失败: " + t);
        }
    }

    private static void renderLyric(final Activity act, final LinearLayout target, final boolean hasRaw) {
        try {
            target.removeAllViews();
            LYRIC_VIEWS.clear();
            if (LRC.isEmpty()) {
                target.addView(text(act, hasRaw
                        ? "歌词格式没解析出来" : "这首歌没有歌词（纯音乐 / 数据缺失）", 13, C_SUB, false));
                return;
            }
            android.util.Log.i("CatMusic", "渲染歌词 " + LRC.size() + " 行");
            for (LrcLine l : LRC) {
                TextView tv = text(act, l.text.length() == 0 ? " " : l.text,
                        13.5f, C_SUB, false);
                tv.setGravity(Gravity.CENTER);
                tv.setPadding(dp(act, 16), dp(act, 9), dp(act, 16), dp(act, 9));
                tv.setLineSpacing(dp(act, 2), 1f);
                tv.setShadowLayer(dp(act, 5), 0, dp(act, 1), 0xCC000000);
                target.addView(tv, new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));
                LYRIC_VIEWS.add(tv);
            }
            try {
                if (player != null) updateLyricPos(player.getCurrentPosition());
                lastUserScrollAt = 0L;
                UI.postDelayed(new Runnable() {
                    @Override public void run() { centerLyricIfIdle(); }
                }, 120L);
            } catch (Throwable ignore) {
            }
        } catch (Throwable ignore) {
        }
    }

    private static synchronized void parseLrc(String raw) {
        LRC.clear();
        if (raw == null || raw.length() == 0) return;
        try {
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(
                    "\\[(\\d{1,2}):(\\d{1,2})(?:[.:](\\d{1,3}))?\\](.*)");
            java.util.regex.Matcher m = p.matcher(raw);
            while (m.find()) {
                LrcLine l = new LrcLine();
                int min = Integer.parseInt(m.group(1));
                int sec = Integer.parseInt(m.group(2));
                String frac = m.group(3);
                int ms = 0;
                if (frac != null) {
                    if (frac.length() == 1) ms = Integer.parseInt(frac) * 100;
                    else if (frac.length() == 2) ms = Integer.parseInt(frac) * 10;
                    else ms = Integer.parseInt(frac.substring(0, 3));
                }
                l.time = min * 60000L + sec * 1000L + ms;
                l.text = m.group(4) == null ? "" : m.group(4).trim();
                LRC.add(l);
            }
            java.util.Collections.sort(LRC, new java.util.Comparator<LrcLine>() {
                @Override public int compare(LrcLine a, LrcLine b) {
                    return Long.compare(a.time, b.time);
                }
            });
            android.util.Log.i("CatMusic", "LRC 解析: raw=" + raw.length()
                    + " 行=" + LRC.size()
                    + (LRC.isEmpty() ? "" : " 首行=" + LRC.get(0).time + "ms"));
        } catch (Throwable ignore) {
        }
    }

    private static void updateLyricPos(int pos) {
        try {
            if (LRC.isEmpty() || LYRIC_VIEWS.isEmpty()) return;
            int idx = -1;
            for (int i = 0; i < LRC.size(); i++) {
                if (LRC.get(i).time <= pos + 120) idx = i;
                else break;
            }
            if (idx == lastLrcIdx) return;
            if (lastLrcIdx >= 0 && lastLrcIdx < LYRIC_VIEWS.size()) {
                TextView old = LYRIC_VIEWS.get(lastLrcIdx);
                old.setTextColor(C_SUB);
                old.setTextSize(13.5f);
                old.setTypeface(null, Typeface.NORMAL);
            }
            if (idx >= 0 && idx < LYRIC_VIEWS.size()) {
                TextView cur = LYRIC_VIEWS.get(idx);
                cur.setTextColor(C_MAIN);
                cur.setTextSize(16f);
                cur.setTypeface(null, Typeface.BOLD);

            }
            lastLrcIdx = idx;
        } catch (Throwable ignore) {
        }
    }

    private static void syncSongMeta(NeteaseApi.Song s) {
        try {
            if (session == null || s == null) return;
            lastMetaLyric = "";
            android.media.MediaMetadata.Builder mb = new android.media.MediaMetadata.Builder()
                    .putString(android.media.MediaMetadata.METADATA_KEY_TITLE, s.name)
                    .putString(android.media.MediaMetadata.METADATA_KEY_ARTIST, s.artist)
                    .putString(android.media.MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE, s.artist)
                    .putLong(android.media.MediaMetadata.METADATA_KEY_DURATION,
                            s.durationMs > 0 ? s.durationMs : 0);
            try {
                if (cover != null && cover.getDrawable() instanceof android.graphics.drawable.BitmapDrawable) {
                    mb.putBitmap(android.media.MediaMetadata.METADATA_KEY_ALBUM_ART,
                            ((android.graphics.drawable.BitmapDrawable) cover.getDrawable()).getBitmap());
                }
            } catch (Throwable ignore) {
            }
            session.setMetadata(mb.build());
        } catch (Throwable ignore) {
        }
    }

    private static void syncLyricToSession(int pos) {
        try {
            if (session == null || nowSong == null) return;
            if (LRC.isEmpty()) return;
            int idx = -1;
            for (int i = 0; i < LRC.size(); i++) {
                if (LRC.get(i).time <= pos + 120) idx = i;
                else break;
            }
            String line = (idx >= 0 && idx < LRC.size()) ? LRC.get(idx).text : "";
            if (line == null) line = "";
            if (line.equals(lastMetaLyric)) return;
            lastMetaLyric = line;

            android.media.MediaMetadata.Builder mb = new android.media.MediaMetadata.Builder()
                    .putString(android.media.MediaMetadata.METADATA_KEY_TITLE, nowSong.name)
                    .putString(android.media.MediaMetadata.METADATA_KEY_ARTIST,
                            line.length() > 0 ? line : nowSong.artist)
                    .putString(android.media.MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE,
                            line.length() > 0 ? line : nowSong.artist)
                    .putLong(android.media.MediaMetadata.METADATA_KEY_DURATION, durMs > 0 ? durMs : 0);
            try {
                if (cover != null && cover.getDrawable() instanceof android.graphics.drawable.BitmapDrawable) {
                    mb.putBitmap(android.media.MediaMetadata.METADATA_KEY_ALBUM_ART,
                            ((android.graphics.drawable.BitmapDrawable) cover.getDrawable()).getBitmap());
                }
            } catch (Throwable ignore) {
            }
            session.setMetadata(mb.build());
        } catch (Throwable ignore) {
        }
    }

    private static void centerLyricIfIdle() {
        try {
            if (lyricScroll == null || LYRIC_VIEWS.isEmpty() || lastLrcIdx < 0) return;
            if (System.currentTimeMillis() - lastUserScrollAt < RESUME_CENTER_MS) return;
            if (lastLrcIdx >= LYRIC_VIEWS.size()) return;
            TextView cur = LYRIC_VIEWS.get(lastLrcIdx);
            int h = lyricScroll.getHeight();
            if (h <= 0) return;
            int target = cur.getTop() - h / 2 + cur.getHeight() / 2;
            if (target < 0) target = 0;
            if (Math.abs(lyricScroll.getScrollY() - target) < 8) return;
            android.util.Log.d("CatMusic", "歌词回中 idx=" + lastLrcIdx
                    + " target=" + target + " cur=" + lyricScroll.getScrollY()
                    + " h=" + h);
            lyricScroll.smoothScrollTo(0, target);
        } catch (Throwable ignore) {
        }
    }

    private static void releasePlayerOnly() {
        try {
            if (player != null) {
                if (player.isPlaying()) player.stop();
                player.release();
            }
        } catch (Throwable ignore) {
        }
        player = null;
        playing = false;
        durMs = 0;
        try {
            UI.removeCallbacks(TICK);
        } catch (Throwable ignore) {
        }
    }

    public static void stopPlay() {
        releasePlayerOnly();
        try {
            MusicPlaybackService.stop(host);
        } catch (Throwable ignore) {
        }

        setIdleSession();
    }

    private static View songRow(final Activity act, final NeteaseApi.Song s,
                                final List<NeteaseApi.Song> list) {
        LinearLayout row = rowBase(act);
        LinearLayout texts = new LinearLayout(act);
        texts.setOrientation(LinearLayout.VERTICAL);
        row.addView(texts, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        texts.addView(text(act, s.name, 13.5f, C_MAIN, true));
        String sub = s.artist + (s.durText().length() > 0 ? " · " + s.durText() : "");
        texts.addView(text(act, sub, 10.5f, C_SUB, false));
        row.addView(text(act, "▶", 14, C_ACCENT, true));

        row.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                QUEUE.clear();
                QUEUE.addAll(list);
                index = Math.max(0, list.indexOf(s));
                if (rootBox != null) buildPlayer(act, rootBox);
                playAt(act, index);
            }
        });
        return row;
    }

    private static View songRowIdx(final Activity act, final NeteaseApi.Song s, final int idx) {
        LinearLayout row = rowBase(act);
        LinearLayout texts = new LinearLayout(act);
        texts.setOrientation(LinearLayout.VERTICAL);
        row.addView(texts, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        String mark = (idx == index) ? "♪ " : "";
        texts.addView(text(act, mark + s.name, 13.5f,
                idx == index ? C_ACCENT : C_MAIN, true));
        texts.addView(text(act, s.artist, 10.5f, C_SUB, false));
        row.addView(text(act, "▶", 14, C_ACCENT, true));
        row.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                index = idx;
                playAt(act, idx);
            }
        });
        return row;
    }

    private static View playlistRow(final Activity act, final LinearLayout box,
                                    final NeteaseApi.Playlist p) {
        LinearLayout row = rowBase(act);
        LinearLayout texts = new LinearLayout(act);
        texts.setOrientation(LinearLayout.VERTICAL);
        row.addView(texts, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        String nm = p.specialType == 5 ? "我喜欢的音乐" : p.name;
        texts.addView(text(act, nm, 13.5f, C_MAIN, true));
        texts.addView(text(act, p.trackCount + " 首", 10.5f, C_SUB, false));
        row.addView(text(act, "›", 15, C_ACCENT, true));
        row.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { buildPlaylist(act, box, p); }
        });
        return row;
    }

    private static LinearLayout rowBase(Activity act) {
        LinearLayout row = new LinearLayout(act);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackgroundResource(com.cathelperawa.owo.app.R.drawable.bg_card);
        row.setPadding(dp(act, 12), dp(act, 10), dp(act, 12), dp(act, 10));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(act, 7);
        row.setLayoutParams(lp);
        return row;
    }

    private static void loadCover(final String url) {
        if (cover == null) return;
        if (url == null || url.length() == 0) {
            cover.setImageDrawable(null);
            return;
        }
        if (url.startsWith("http://")) {

            loadCoverAsync("https://" + url.substring(7));
        } else {
            loadCoverAsync(url);
        }
    }

    private static void loadCoverAsync(final String url) {
        Bg.run(new Runnable() {
            @Override public void run() {
                Bitmap bm = null;
                HttpURLConnection conn = null;
                try {
                    conn = (HttpURLConnection) new URL(url).openConnection();
                    conn.setConnectTimeout(8000);
                    conn.setReadTimeout(8000);
                    conn.setRequestProperty("User-Agent",
                            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/130.0.0.0");
                    if (conn.getResponseCode() == 200) {
                        InputStream in = conn.getInputStream();
                        bm = BitmapFactory.decodeStream(in);
                        try { in.close(); } catch (Throwable ignore) { }
                    }
                } catch (Throwable ignore) {
                } finally {
                    try { if (conn != null) conn.disconnect(); } catch (Throwable ignore) { }
                }
                final Bitmap fb = bm;
                if (fb == null) return;
                UI.post(new Runnable() {
                    @Override public void run() {
                        if (cover != null) cover.setImageBitmap(fb);
                    }
                });
            }
        }, "cat-cover");
    }

    private static LinearLayout card(Activity act) {
        LinearLayout card = new LinearLayout(act);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setBackgroundResource(com.cathelperawa.owo.app.R.drawable.bg_card);
        card.setPadding(dp(act, 13), dp(act, 13), dp(act, 13), dp(act, 13));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(act, 9);
        card.setLayoutParams(lp);
        return card;
    }

    private static TextView iconView(Activity act, String shortName, int color) {
        TextView icon = new TextView(act);
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(color);
        icon.setBackground(bg);
        icon.setGravity(Gravity.CENTER);
        icon.setText(shortName);
        icon.setTextColor(Color.WHITE);
        icon.setTextSize(shortName.length() > 1 ? 11f : 15f);
        icon.setTypeface(null, Typeface.BOLD);
        int size = dp(act, 40);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
        lp.rightMargin = dp(act, 12);
        icon.setLayoutParams(lp);
        return icon;
    }

    private static TextView text(Activity act, String s, float size, int color, boolean bold) {
        TextView tv = new TextView(act);
        tv.setText(s);
        tv.setTextSize(size);
        tv.setTextColor(color);
        if (bold) tv.setTypeface(null, Typeface.BOLD);
        return tv;
    }

    private static TextView hint(Activity act, String s) {
        TextView tv = text(act, s, 11.5f, 0xFF8A9CB5, false);
        tv.setLineSpacing(dp(act, 3), 1f);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(act, 10);
        lp.leftMargin = dp(act, 4);
        tv.setLayoutParams(lp);
        return tv;
    }

    private static TextView sectionTitle(Activity act, String s) {
        TextView tv = text(act, s, 12f, 0xFF8395AD, true);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(act, 12);
        lp.bottomMargin = dp(act, 8);
        lp.leftMargin = dp(act, 4);
        tv.setLayoutParams(lp);
        return tv;
    }

    private static int dp(Activity a, float v) {

        return com.cathelperawa.owo.app.ui.UiKit.dp(a, v);
    }

    private static String fmt(int ms) {
        if (ms < 0) ms = 0;
        int s = ms / 1000;
        return (s / 60) + ":" + (s % 60 < 10 ? "0" : "") + (s % 60);
    }
}
