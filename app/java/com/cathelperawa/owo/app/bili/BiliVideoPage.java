package com.cathelperawa.owo.app.bili;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.PlaybackParams;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.cathelperawa.owo.app.ui.CatIcon;
import com.cathelperawa.owo.app.ui.SkinSeek;

import java.util.List;

public final class BiliVideoPage extends BiliPage {

    private final BiliApi.Item seed;

    private LinearLayout root;
    private View topbar;
    private ImageView coverImg;
    private ScrollView infoScroll;
    private RatioFrame coverBox;
    private BiliPlayerView video;
    private View playBig;
    private LinearLayout bar;
    private SkinSeek seek;
    private TextView timeLabel;
    private CatIcon playBtn;
    private TextView speedBtn;
    private CatIcon fullBtn;
    private TextView statLine;

    private BiliApi.Detail detail;
    private long cid = 0L;
    private String videoUrl;
    private boolean prepared = false;
    private boolean fullscreen = false;
    private boolean userPaused = false;
    private float speed = 1.0f;
    private boolean controlsShown = true;

    private LinearLayout pagesBox;
    private LinearLayout replyBox;
    private TextView replyHead;
    private int replyPage = 1;
    private boolean replyLoading = false;
    private boolean replyNoMore = false;
    private TextView moreReplyBtn;

    private final Handler ticker = new Handler(Looper.getMainLooper());
    private final Runnable tick = new Runnable() {
        @Override public void run() {
            try {
                if (video != null && prepared) {
                    int pos = video.getCurrentPosition() / 1000;
                    int dur = Math.max(1, video.getDuration() / 1000);
                    if (seek != null) seek.setValue(Math.min(pos, dur));
                    if (timeLabel != null) {
                        timeLabel.setText(BiliApi.clock(pos) + " / " + BiliApi.clock(dur));
                    }
                    if (video.isPlaying() && pos % 10 == 0) savePos(pos);
                }
            } catch (Throwable ignore) {
            }
            ticker.postDelayed(this, 500);
        }
    };

    private final Runnable hideControls = new Runnable() {
        @Override public void run() {
            if (!fullscreen) return;
            setControlsShown(false);
        }
    };

    public BiliVideoPage(BiliActivity host, BiliApi.Item item) {
        super(host);
        this.seed = item;
        this.cid = item.cid;
    }

    @Override
    public View onCreate() {
        root = new LinearLayout(host);
        root.setOrientation(LinearLayout.VERTICAL);

        topbar = buildTopbar();
        root.addView(topbar);
        root.addView(buildPlayer());

        infoScroll = new ScrollView(host);
        infoScroll.setFillViewport(true);
        LinearLayout info = new LinearLayout(host);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setPadding(BiliUi.dp(host, 14), BiliUi.dp(host, 12),
                BiliUi.dp(host, 14), BiliUi.dp(host, 28));

        TextView title = BiliUi.text(host, seed.title, 15f, BiliUi.MAIN, true);
        title.setMaxLines(2);
        title.setEllipsize(android.text.TextUtils.TruncateAt.END);
        info.addView(title);

        info.addView(buildUpRow());
        info.addView(buildActions());

        pagesBox = new LinearLayout(host);
        pagesBox.setOrientation(LinearLayout.VERTICAL);
        pagesBox.setVisibility(View.GONE);
        info.addView(pagesBox);

        info.addView(BiliUi.line(host, 0.08f));

        TextView desc = BiliUi.text(host, "正在加载简介…", 11.5f, BiliUi.CARD_SUB, false);
        desc.setPadding(0, BiliUi.dp(host, 12), 0, 0);
        desc.setMaxLines(3);
        desc.setEllipsize(android.text.TextUtils.TruncateAt.END);
        info.addView(desc);
        final TextView descRef = desc;
        desc.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (descRef.getMaxLines() == 3) {
                    descRef.setMaxLines(200);
                    descRef.setEllipsize(null);
                } else {
                    descRef.setMaxLines(3);
                    descRef.setEllipsize(android.text.TextUtils.TruncateAt.END);
                }
            }
        });

        TextView relHead = BiliUi.text(host, "相关推荐", 12f, BiliUi.SUB, false);
        LinearLayout.LayoutParams rhlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rhlp.topMargin = BiliUi.dp(host, 16);
        rhlp.bottomMargin = BiliUi.dp(host, 10);
        info.addView(relHead, rhlp);

        final LinearLayout relBox = new LinearLayout(host);
        relBox.setOrientation(LinearLayout.VERTICAL);
        info.addView(relBox);

        replyHead = BiliUi.text(host, "评论", 12f, BiliUi.SUB, false);
        LinearLayout.LayoutParams qlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        qlp.topMargin = BiliUi.dp(host, 18);
        qlp.bottomMargin = BiliUi.dp(host, 10);
        info.addView(replyHead, qlp);

        replyBox = new LinearLayout(host);
        replyBox.setOrientation(LinearLayout.VERTICAL);
        info.addView(replyBox);

        moreReplyBtn = BiliUi.text(host, "加载更多评论", 11.5f, BiliUi.ACCENT, false);
        moreReplyBtn.setGravity(Gravity.CENTER);
        moreReplyBtn.setPadding(0, BiliUi.dp(host, 10), 0, BiliUi.dp(host, 10));
        moreReplyBtn.setVisibility(View.GONE);
        moreReplyBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { loadReplies(); }
        });
        info.addView(moreReplyBtn);

        infoScroll.addView(info, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(infoScroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        loadDetail(descRef, relBox, title);
        ticker.postDelayed(tick, 500);
        return root;
    }

    private View buildTopbar() {
        LinearLayout top = new LinearLayout(host);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(BiliUi.dp(host, 12), BiliUi.dp(host, 8),
                BiliUi.dp(host, 12), BiliUi.dp(host, 8));

        CatIcon back = BiliUi.icon(host, CatIcon.BACK, 20, BiliUi.MAIN);
        top.addView(back);
        back.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { host.onBackPressed(); }
        });

        TextView t = BiliUi.text(host, seed.title, 12.5f, BiliUi.MAIN, false);
        t.setSingleLine(true);
        t.setEllipsize(android.text.TextUtils.TruncateAt.END);
        t.setAlpha(0.92f);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        tlp.leftMargin = BiliUi.dp(host, 12);
        tlp.rightMargin = BiliUi.dp(host, 12);
        top.addView(t, tlp);

        CatIcon more = BiliUi.icon(host, CatIcon.MORE, 18, BiliUi.SUB);
        top.addView(more);
        more.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                toast("更多操作还没接");
            }
        });
        return top;
    }

    private View buildPlayer() {
        coverBox = new RatioFrame(host, 9f / 16f);
        coverBox.setBackgroundColor(0xFF000000);

        video = new BiliPlayerView(host);
        coverBox.addView(video, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER));
        video.setListener(new BiliPlayerView.Listener() {
            @Override public void onPrepared(int w, int h, int durationMs) {
                prepared = true;
                
                video.post(new Runnable() {
                    @Override public void run() {
                        if (coverImg != null) coverImg.setVisibility(View.GONE);
                    }
                });
                int saved = loadPos();
                if (saved > 3 && durationMs > 0 && saved * 1000 < durationMs - 5000) {
                    video.seekTo(saved * 1000);
                    toast("从 " + BiliApi.clock(saved) + " 继续播放");
                }
                if (playBtn != null) playBtn.setGlyph(CatIcon.PAUSE);
                if (playBig != null) playBig.setVisibility(View.GONE);
                setControlsShown(true);
            }

            @Override public void onComplete() {
                prepared = false;
                if (playBtn != null) playBtn.setGlyph(CatIcon.PLAY);
                if (playBig != null) playBig.setVisibility(View.VISIBLE);
                if (fullscreen) toggleFullscreen();
            }

            @Override public void onError(int what, int extra) {
                prepared = false;
                toast("这个视频播不了（" + what + "/" + extra + "）");
            }
        });

        coverImg = new ImageView(host);
        coverImg.setScaleType(ImageView.ScaleType.CENTER_CROP);
        coverImg.setBackgroundColor(0xFF141A2A);
        BiliImage.attach(coverImg, seed.cover, 0);
        coverBox.addView(coverImg, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        playBig = buildPlayBig();
        coverBox.addView(playBig, new FrameLayout.LayoutParams(
                BiliUi.dp(host, 56), BiliUi.dp(host, 56), Gravity.CENTER));
        playBig.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { togglePlay(); }
        });

        bar = buildControlBar();
        coverBox.addView(bar, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM));

        attachGestures();
        coverBox.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return coverBox;
    }

    private View buildPlayBig() {
        
        CatIcon big = BiliUi.icon(host, CatIcon.PLAY, 26, BiliUi.MAIN);
        big.setPill(0x730B0F16, 22);
        return big;
    }

    private LinearLayout buildControlBar() {
        LinearLayout row = new LinearLayout(host);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(BiliUi.dp(host, 14), BiliUi.dp(host, 10),
                BiliUi.dp(host, 14), BiliUi.dp(host, 12));
        row.setBackground(new android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0x00000000, 0xA6000000}));

        playBtn = BiliUi.icon(host, CatIcon.PLAY, 18, BiliUi.MAIN);
        row.addView(playBtn);
        playBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { togglePlay(); }
        });

        timeLabel = BiliUi.text(host, "00:00 / 00:00", 11.5f, 0xFFCFD8E6, false);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tlp.leftMargin = BiliUi.dp(host, 8);
        row.addView(timeLabel, tlp);

        seek = new SkinSeek(host);
        seek.setRange(1000);
        seek.setLineColor(BiliUi.ACCENT);
        seek.setOnChange(new SkinSeek.OnChange() {
            @Override public void onChanged(int value) { }
            @Override public void onFinished(int value) {
                try {
                    if (video != null && prepared) {
                        video.seekTo((int) (video.getDuration() * value / 1000f));
                    }
                } catch (Throwable ignore) {
                }
            }
        });
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(0,
                BiliUi.dp(host, 30), 1f);
        slp.leftMargin = BiliUi.dp(host, 10);
        slp.rightMargin = BiliUi.dp(host, 8);
        row.addView(seek, slp);

        speedBtn = BiliUi.text(host, "1.0x", 12f, BiliUi.MAIN, false);
        speedBtn.setPadding(BiliUi.dp(host, 5), 0, BiliUi.dp(host, 5), 0);
        row.addView(speedBtn);
        speedBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { cycleSpeed(); }
        });

        final CatIcon full = BiliUi.icon(host, CatIcon.FULLSCREEN, 19, BiliUi.MAIN);
        fullBtn = full;
        LinearLayout.LayoutParams flp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        flp.leftMargin = BiliUi.dp(host, 10);
        row.addView(full, flp);
        full.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { toggleFullscreen(); }
        });
        return row;
    }

    private void attachGestures() {
        final GestureDetector gd = new GestureDetector(host,
                new GestureDetector.SimpleOnGestureListener() {
                    @Override public boolean onSingleTapConfirmed(MotionEvent e) {
                        setControlsShown(!controlsShown);
                        return true;
                    }

                    @Override public boolean onDoubleTap(MotionEvent e) {
                        boolean left = e.getX() < coverBox.getWidth() / 2f;
                        seekBy(left ? -10_000 : 10_000);
                        toast(left ? "快退 10 秒" : "快进 10 秒");
                        return true;
                    }

                    @Override public boolean onDown(MotionEvent e) {
                        return true;
                    }
                });

        coverBox.setOnTouchListener(new View.OnTouchListener() {
            float downY = 0f, downX = 0f;
            int startVol = -1;
            float startBright = -1f;
            boolean vertical = false;

            @Override public boolean onTouch(View v, MotionEvent ev) {
                gd.onTouchEvent(ev);
                switch (ev.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        downY = ev.getY();
                        downX = ev.getX();
                        vertical = false;
                        startVol = audio().getStreamVolume(AudioManager.STREAM_MUSIC);
                        startBright = currentBrightness();
                        return true;
                    case MotionEvent.ACTION_MOVE: {
                        float dy = downY - ev.getY();
                        float dx = ev.getX() - downX;
                        if (!vertical && Math.abs(dy) > BiliUi.dp(host, 14)
                                && Math.abs(dy) > Math.abs(dx)) {
                            vertical = true;
                        }
                        if (vertical) {
                            float ratio = dy / Math.max(1f, coverBox.getHeight());
                            if (downX < coverBox.getWidth() / 2f) {
                                
                                float b = Math.max(0.02f, Math.min(1f, startBright + ratio));
                                setBrightness(b);
                                toast("亮度 " + Math.round(b * 100) + "%");
                            } else {
                                
                                int max = audio().getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                                int vol = (int) Math.max(0, Math.min(max, startVol + ratio * max));
                                audio().setStreamVolume(AudioManager.STREAM_MUSIC, vol, 0);
                            }
                            return true;
                        }
                        return false;
                    }
                    case MotionEvent.ACTION_UP:
                        vertical = false;
                        return true;
                }
                return false;
            }
        });
    }

    private AudioManager audio() {
        return (AudioManager) host.getSystemService(Context.AUDIO_SERVICE);
    }

    private float currentBrightness() {
        try {
            float b = host.getWindow().getAttributes().screenBrightness;
            if (b <= 0) b = 0.5f;
            return b;
        } catch (Throwable t) {
            return 0.5f;
        }
    }

    private void setBrightness(float b) {
        try {
            WindowManager.LayoutParams lp = host.getWindow().getAttributes();
            lp.screenBrightness = Math.max(0.02f, Math.min(1f, b));
            host.getWindow().setAttributes(lp);
        } catch (Throwable ignore) {
        }
    }

    private void seekBy(int ms) {
        try {
            if (video != null && prepared) {
                int target = Math.max(0, video.getCurrentPosition() + ms);
                if (video.getDuration() > 0) target = Math.min(target, video.getDuration() - 500);
                video.seekTo(target);
            }
        } catch (Throwable ignore) {
        }
    }

    private void setControlsShown(boolean show) {
        controlsShown = show;
        if (bar != null) bar.setVisibility(show ? View.VISIBLE : View.GONE);
        if (playBig != null && !prepared) playBig.setVisibility(show ? View.VISIBLE : View.GONE);
        ticker.removeCallbacks(hideControls);
        if (show && prepared) ticker.postDelayed(hideControls, 4000);
    }

    private void toast(String msg) {
        toast(0, msg);
    }

    private void toast(int glyph, String msg) {
        try {
            BiliToast.show((ViewGroup) root, glyph, msg);
        } catch (Throwable ignore) {
        }
    }

    private static final float[] SPEEDS = {0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f};

    private void cycleSpeed() {
        int idx = 2;
        for (int i = 0; i < SPEEDS.length; i++) {
            if (Math.abs(SPEEDS[i] - speed) < 0.01f) { idx = i; break; }
        }
        idx = (idx + 1) % SPEEDS.length;
        speed = SPEEDS[idx];
        if (speedBtn != null) speedBtn.setText(fmtSpeed(speed));
        applySpeed();
    }

    private String fmtSpeed(float s) {
        if (Math.abs(s - Math.round(s)) < 0.01f) return ((int) s) + ".0x";
        return s + "x";
    }

    private void applySpeed() {
        if (video != null) video.setSpeed(speed);
    }

    private void beginPlay() {
        if (videoUrl == null || videoUrl.length() == 0) return;
        prepared = false;
        video.setSpeed(speed);
        video.setVideoUrl(videoUrl);
    }

    private void togglePlay() {
        if (!prepared) {
            userPaused = false;
            if (videoUrl != null) beginPlay();
            return;
        }
        if (video.isPlaying()) {
            video.pause();
            userPaused = true;
            savePos(video.getCurrentPosition() / 1000);
            if (playBtn != null) playBtn.setGlyph(CatIcon.PLAY);
            if (playBig != null) playBig.setVisibility(View.VISIBLE);
        } else {
            video.start();
            userPaused = false;
            if (playBtn != null) playBtn.setGlyph(CatIcon.PAUSE);
            if (playBig != null) playBig.setVisibility(View.GONE);
            setControlsShown(true);
        }
    }

    private void applyControlsScale(boolean big) {
        try {
            if (playBtn != null) playBtn.setSizeDp(big ? 26 : 18);
            if (fullBtn != null) fullBtn.setSizeDp(big ? 27 : 19);
            if (speedBtn != null) speedBtn.setTextSize(big ? 14f : 12f);
            if (timeLabel != null) timeLabel.setTextSize(big ? 13.5f : 11.5f);
            if (seek != null) {
                ViewGroup.LayoutParams lp = seek.getLayoutParams();
                if (lp != null) {
                    lp.height = BiliUi.dp(host, big ? 40 : 30);
                    seek.setLayoutParams(lp);
                }
            }
            if (bar != null) {
                bar.setPadding(BiliUi.dp(host, big ? 26 : 14), BiliUi.dp(host, big ? 18 : 10),
                        BiliUi.dp(host, big ? 26 : 14), BiliUi.dp(host, big ? 22 : 12));
            }
        } catch (Throwable ignore) {
        }
    }

    private void toggleFullscreen() {
        fullscreen = !fullscreen;
        try {
            if (fullscreen) {
                host.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
                infoScroll.setVisibility(View.GONE);
                if (topbar != null) topbar.setVisibility(View.GONE);
                host.setImmersive(true);
                coverBox.setFill(true);
                applyControlsScale(true);
                if (bar != null) bar.setPadding(bar.getPaddingLeft(), bar.getPaddingTop(),
                        bar.getPaddingRight(), BiliUi.dp(host, 26));
                setControlsShown(true);
            } else {
                host.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                infoScroll.setVisibility(View.VISIBLE);
                if (topbar != null) topbar.setVisibility(View.VISIBLE);
                host.setImmersive(false);
                coverBox.setFill(false);
                applyControlsScale(false);
                setControlsShown(true);
                try { host.getWindow().getAttributes().screenBrightness = -1f; } catch (Throwable ignore) { }
            }
        } catch (Throwable ignore) {
        }
    }

    @Override
    public boolean onBack() {
        if (fullscreen) {
            toggleFullscreen();
            return true;
        }
        return false;
    }

    private String posKey() {
        return "bili_pos_" + seed.bvid + "_" + cid;
    }

    private void savePos(int sec) {
        try {
            host.prefs().edit().putInt(posKey(), sec).apply();
        } catch (Throwable ignore) {
        }
    }

    private int loadPos() {
        try {
            return host.prefs().getInt(posKey(), 0);
        } catch (Throwable t) {
            return 0;
        }
    }

    private void loadDetail(final TextView desc, final LinearLayout relBox, final TextView title) {
        BiliThread.io(new Runnable() {
            @Override public void run() {
                BiliApiExt.ensureBuvid(host);
                final BiliApi.Detail d = BiliApi.detail(host, seed.bvid);
                if (d.base.cid > 0) cid = d.base.cid;
                final String url = cid > 0 ? BiliApi.playUrl(host, seed.bvid, cid, 64) : null;
                final List<BiliApi.Item> rel = BiliApi.related(host, seed.bvid);
                final String online = BiliApiExt.online(host, d.aid, cid);
                BiliThread.ui(new Runnable() {
                    @Override public void run() {
                        onDetail(d, url, rel, online, desc, relBox, title);
                    }
                });
            }
        });
    }

    private void onDetail(BiliApi.Detail d, String url, List<BiliApi.Item> rel, String online,
                          TextView desc, LinearLayout relBox, TextView title) {
        detail = d;
        if (d.base.title != null && d.base.title.length() > 0) title.setText(d.base.title);
        if (upAvatar != null) {
            String face = (d.ownerFace != null && d.ownerFace.length() > 0)
                    ? d.ownerFace : d.base.cover;
            BiliImage.attachAvatar(upAvatar, face);
        }
        if (d.desc != null && d.desc.trim().length() > 0) desc.setText(d.desc);
        else desc.setText("（没有简介）");

        if (statLine != null) {
            StringBuilder sb = new StringBuilder();
            sb.append(BiliApi.count(d.base.play)).append(" 播放 · ")
              .append(BiliApi.count(d.base.danmaku)).append(" 弹幕");
            if (online != null && online.length() > 0) sb.append(" · ").append(online);
            statLine.setText(sb.toString());
        }

        if (actions != null && actions.length == 4) {
            setActionNum(actions[0], d.like);
            setActionNum(actions[1], d.coin);
            setActionNum(actions[2], d.fav);
            setActionNum(actions[3], -1);
        }

        buildPages(d);

        videoUrl = url;
        if (url != null) {
            beginPlay();
        } else {
            BiliThread.io(new Runnable() {
                @Override public void run() {
                    final String du = BiliApi.dashUrl(host, seed.bvid, cid);
                    BiliThread.ui(new Runnable() {
                        @Override public void run() {
                            if (du != null) {
                                videoUrl = du;
                                beginPlay();
                            } else {
                                toast("拿不到播放地址（可能要大会员或已失效）");
                            }
                        }
                    });
                }
            });
        }

        relBox.removeAllViews();
        for (int i = 0; i < rel.size() && i < 8; i++) {
            final BiliApi.Item it = rel.get(i);
            View row = BiliCards.relatedRow(host, it, new View.OnClickListener() {
                @Override public void onClick(View v) {
                    host.push(new BiliVideoPage(host, it));
                }
            });
            LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            rlp.bottomMargin = BiliUi.dp(host, 10);
            relBox.addView(row, rlp);
        }

        if (d.aid > 0) loadReplies();
    }

    private void buildPages(final BiliApi.Detail d) {
        if (pagesBox == null) return;
        pagesBox.removeAllViews();
        if (d.pages == null || d.pages.size() <= 1) {
            pagesBox.setVisibility(View.GONE);
            return;
        }
        pagesBox.setVisibility(View.VISIBLE);

        TextView head = BiliUi.text(host, "选集（" + d.pages.size() + "）", 11.5f, BiliUi.SUB, false);
        head.setPadding(0, BiliUi.dp(host, 10), 0, BiliUi.dp(host, 6));
        pagesBox.addView(head);

        HorizontalScrollView hs = new HorizontalScrollView(host);
        hs.setHorizontalScrollBarEnabled(false);
        LinearLayout row = new LinearLayout(host);
        row.setOrientation(LinearLayout.HORIZONTAL);
        hs.addView(row);

        for (int i = 0; i < d.pages.size(); i++) {
            final BiliApi.Item p = d.pages.get(i);
            final boolean on = (p.cid == cid);
            LinearLayout chip = BiliUi.pill(host,
                    on ? 0x336EA6FF : 0x1FFFFFFF, on ? 0x8C6EA6FF : 0x24FFFFFF, 9);
            chip.setPadding(BiliUi.dp(host, 11), BiliUi.dp(host, 6),
                    BiliUi.dp(host, 11), BiliUi.dp(host, 6));
            String label = "P" + (i + 1);
            if (p.title != null && p.title.length() > 0 && p.title.length() <= 8) {
                label += " " + p.title;
            }
            chip.addView(BiliUi.text(host, label, 11f,
                    on ? BiliUi.ACCENT : BiliUi.CARD_SUB, on));
            LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            clp.rightMargin = BiliUi.dp(host, 8);
            row.addView(chip, clp);
            chip.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    if (p.cid == cid) return;
                    switchPage(p);
                }
            });
        }
        pagesBox.addView(hs);
    }

    private void switchPage(BiliApi.Item page) {
        cid = page.cid;
        prepared = false;
        userPaused = false;
        videoUrl = null;
        try {
            video.release();
        } catch (Throwable ignore) {
        }
        if (coverImg != null) coverImg.setVisibility(View.VISIBLE);
        if (playBig != null) playBig.setVisibility(View.VISIBLE);
        if (detail != null) buildPages(detail);
        toast("切到 P" + (indexOfPage(page) + 1));
        BiliThread.io(new Runnable() {
            @Override public void run() {
                final String u = BiliApi.playUrl(host, seed.bvid, cid, 64);
                BiliThread.ui(new Runnable() {
                    @Override public void run() {
                        if (u != null) {
                            videoUrl = u;
                            beginPlay();
                        } else {
                            toast("这一集拿不到播放地址");
                        }
                    }
                });
            }
        });
    }

    private int indexOfPage(BiliApi.Item page) {
        if (detail == null || detail.pages == null) return 0;
        for (int i = 0; i < detail.pages.size(); i++) {
            if (detail.pages.get(i).cid == page.cid) return i;
        }
        return 0;
    }

    private void loadReplies() {
        if (replyLoading || replyNoMore || detail == null || detail.aid <= 0) return;
        replyLoading = true;
        if (replyPage == 1 && replyBox != null) {
            replyBox.addView(BiliCards.loading(host, "评论加载中…"));
        }
        final long aid = detail.aid;
        final int page = replyPage;
        BiliThread.io(new Runnable() {
            @Override public void run() {
                final List<BiliApiExt.Reply> list = BiliApiExt.replies(host, aid, page, 2);
                final int total = page == 1 ? BiliApiExt.replyCount(host, aid) : 0;
                BiliThread.ui(new Runnable() {
                    @Override public void run() {
                        replyLoading = false;
                        onReplies(list, total, page);
                    }
                });
            }
        });
    }

    private void onReplies(List<BiliApiExt.Reply> list, int total, int page) {
        if (replyBox == null) return;
        if (page == 1) {
            replyBox.removeAllViews();
            replyHead.setText(total > 0 ? ("评论 " + BiliApi.count(total)) : "评论");
        }
        if (list == null || list.isEmpty()) {
            replyNoMore = true;
            moreReplyBtn.setVisibility(View.GONE);
            if (page == 1) replyBox.addView(BiliCards.loading(host, "还没有评论"));
            return;
        }
        for (BiliApiExt.Reply r : list) {
            replyBox.addView(buildReply(r, false));
        }
        replyPage = page + 1;
        moreReplyBtn.setVisibility(View.VISIBLE);
    }

    private View buildReply(BiliApiExt.Reply r, boolean child) {
        LinearLayout outer = new LinearLayout(host);
        outer.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams olp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        olp.topMargin = BiliUi.dp(host, child ? 7 : 12);
        outer.setLayoutParams(olp);

        LinearLayout row = new LinearLayout(host);
        row.setOrientation(LinearLayout.HORIZONTAL);

        int size = child ? 24 : 30;
        ImageView ava = new ImageView(host);
        BiliImage.attachAvatar(ava, r.avatar);
        LinearLayout.LayoutParams alp = new LinearLayout.LayoutParams(
                BiliUi.dp(host, size), BiliUi.dp(host, size));
        if (child) alp.leftMargin = BiliUi.dp(host, 30);
        row.addView(ava, alp);

        LinearLayout texts = new LinearLayout(host);
        texts.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        tlp.leftMargin = BiliUi.dp(host, 9);
        row.addView(texts, tlp);
        outer.addView(row, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout head = new LinearLayout(host);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.addView(BiliUi.text(host, r.uname, child ? 10.5f : 11.5f, 0xFF8FA9C6, false));
        TextView time = BiliUi.text(host, " · " + BiliApiExt.ago(r.ctime), 10f, BiliUi.SUB, false);
        head.addView(time);
        texts.addView(head);

        TextView msg = BiliUi.text(host, r.content, child ? 11.5f : 12.5f, BiliUi.MAIN, false);
        msg.setLineSpacing(BiliUi.dp(host, 3), 1f);
        LinearLayout.LayoutParams mlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        mlp.topMargin = BiliUi.dp(host, 4);
        texts.addView(msg, mlp);

        LinearLayout foot = new LinearLayout(host);
        foot.setOrientation(LinearLayout.HORIZONTAL);
        foot.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams flp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        flp.topMargin = BiliUi.dp(host, 5);
        texts.addView(foot, flp);

        foot.addView(BiliUi.icon(host, CatIcon.THUMB, 11, BiliUi.SUB));
        TextView like = BiliUi.text(host, BiliApi.count(r.like), 10f, BiliUi.SUB, false);
        LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        llp.leftMargin = BiliUi.dp(host, 3);
        foot.addView(like, llp);

        if (!child && r.rcount > 0) {
            TextView rc = BiliUi.text(host, "  ·  " + r.rcount + " 条回复", 10f, BiliUi.SUB, false);
            foot.addView(rc);
        }

        if (!child && !r.children.isEmpty()) {
            for (BiliApiExt.Reply c : r.children) {
                outer.addView(buildReply(c, true));
            }
        }
        return outer;
    }

    private LinearLayout[] actions;

    private View buildActions() {
        LinearLayout row = new LinearLayout(host);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, BiliUi.dp(host, 14), 0, BiliUi.dp(host, 12));

        final int[] glyphs = {CatIcon.THUMB, CatIcon.COIN, CatIcon.STAR, CatIcon.SHARE};
        final String[] labels = {"点赞", "投币", "收藏", "分享"};
        actions = new LinearLayout[4];
        for (int i = 0; i < 4; i++) {
            final int idx = i;
            LinearLayout col = BiliUi.iconActionColumn(host, glyphs[i], labels[i],
                    BiliUi.CARD_SUB, 21, 10f);
            row.addView(col, new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            col.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    toast(labels[idx] + "还没接（要登录 Cookie + csrf）");
                }
            });
            actions[i] = col;
        }
        return row;
    }

    private void setActionNum(LinearLayout col, long n) {
        if (col == null || col.getChildCount() < 2) return;
        try {
            TextView t = (TextView) col.getChildAt(1);
            t.setText(n < 0 ? "分享" : BiliApi.count(n));
        } catch (Throwable ignore) {
        }
    }

    private View buildUpRow() {
        LinearLayout wrap = new LinearLayout(host);
        wrap.setOrientation(LinearLayout.VERTICAL);

        LinearLayout upRow = new LinearLayout(host);
        upRow.setOrientation(LinearLayout.HORIZONTAL);
        upRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams upLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        upLp.topMargin = BiliUi.dp(host, 12);
        wrap.addView(upRow, upLp);

        ImageView ava = new ImageView(host);
        upRow.addView(ava, new LinearLayout.LayoutParams(
                BiliUi.dp(host, 32), BiliUi.dp(host, 32)));

        LinearLayout upTexts = new LinearLayout(host);
        upTexts.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams utlp = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        utlp.leftMargin = BiliUi.dp(host, 9);
        upRow.addView(upTexts, utlp);
        upTexts.addView(BiliUi.text(host, seed.up, 12.5f, BiliUi.MAIN, false));
        statLine = BiliUi.text(host, "加载中…", 10f, BiliUi.CARD_SUB, false);
        upTexts.addView(statLine);

        LinearLayout follow = BiliUi.pill(host, 0x1A6EA6FF, 0x8C6EA6FF, 12);
        follow.setPadding(BiliUi.dp(host, 13), BiliUi.dp(host, 4),
                BiliUi.dp(host, 13), BiliUi.dp(host, 4));
        follow.addView(BiliUi.text(host, "＋ 关注", 11.5f, BiliUi.ACCENT, false));
        follow.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                toast("关注还没接");
            }
        });
        upRow.addView(follow);

        upAvatar = ava;
        return wrap;
    }

    private ImageView upAvatar;
    private TextView titleRef;

    @Override
    public void onDestroy() {
        ticker.removeCallbacks(tick);
        ticker.removeCallbacks(hideControls);
        try {
            if (video != null && prepared) savePos(video.getCurrentPosition() / 1000);
        } catch (Throwable ignore) {
        }
        try {
            if (video != null) video.release();
        } catch (Throwable ignore) {
        }
        try {
            host.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            host.setImmersive(false);
        } catch (Throwable ignore) {
        }
    }

    static final class RatioFrame extends FrameLayout {
        private final float ratio;
        private boolean fill = false;

        RatioFrame(Context c, float ratio) {
            super(c);
            this.ratio = ratio;
        }

        void setFill(boolean f) {
            if (fill != f) {
                fill = f;
                requestLayout();
            }
        }

        @Override
        protected void onMeasure(int ws, int hs) {
            if (fill) {
                super.onMeasure(ws, hs);
                return;
            }
            int w = MeasureSpec.getSize(ws);
            if (w <= 0) {
                super.onMeasure(ws, hs);
                return;
            }
            int h = (int) (w * ratio + 0.5f);
            super.onMeasure(MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(h, MeasureSpec.EXACTLY));
        }
    }
}
