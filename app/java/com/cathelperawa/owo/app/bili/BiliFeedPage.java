package com.cathelperawa.owo.app.bili;

import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.cathelperawa.owo.app.ui.CatIcon;

import java.util.List;

public final class BiliFeedPage extends BiliPage {

    private static final String[] CHANNELS =
            {"直播", "推荐", "热门", "排行", "每周", "分区"};

    private LinearLayout channelBar;
    private TextView[] channelTexts = new TextView[CHANNELS.length];
    private View[] channelBars = new View[CHANNELS.length];

    private PullRefreshScroll scroll;
    private BiliRefreshView refreshView;
    private boolean refreshing = false;
    private LinearLayout colLeft, colRight;
    private TextView status;

    private int mode = 1;             
    private int feedIdx = 1;
    private int popularPage = 1;
    private boolean loading = false;
    private boolean noMore = false;
    private int cardWidth = 0;

    public BiliFeedPage(BiliActivity host) {
        super(host);
    }

    @Override
    public View onCreate() {
        LinearLayout root = new LinearLayout(host);
        root.setOrientation(LinearLayout.VERTICAL);

        root.addView(buildTop());
        root.addView(buildChannels());
        root.addView(BiliUi.line(host, 0.08f));

        LinearLayout wrap = new LinearLayout(host);
        wrap.setOrientation(LinearLayout.VERTICAL);
        refreshView = new BiliRefreshView(host);
        refreshView.setTrigger(BiliUi.dp(host, 62));
        wrap.addView(refreshView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0));

        scroll = new PullRefreshScroll(host);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        scroll.setThresholdDp(62);
        LinearLayout cols = new LinearLayout(host);
        cols.setOrientation(LinearLayout.HORIZONTAL);
        cols.setPadding(BiliUi.dp(host, 14), BiliUi.dp(host, 12),
                BiliUi.dp(host, 14), BiliUi.dp(host, 16));
        int gap = BiliUi.dp(host, 10);
        cardWidth = (BiliCards.screenWidth(host) - BiliUi.dp(host, 28) - gap) / 2;

        colLeft = new LinearLayout(host);
        colLeft.setOrientation(LinearLayout.VERTICAL);
        colRight = new LinearLayout(host);
        colRight.setOrientation(LinearLayout.VERTICAL);
        colRight.setPadding(gap, 0, 0, 0);
        cols.addView(colLeft, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        cols.addView(colRight, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        scroll.addView(cols, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        scroll.setOnPull(new PullRefreshScroll.OnPull() {
            @Override public void onPull(int dy) {
                refreshView.setPull(dy);
            }

            @Override public boolean onRelease(int dy, boolean overThreshold) {
                if (!overThreshold) {
                    refreshView.stop();
                    return false;
                }
                refreshing = true;
                refreshView.startSpin();
                scroll.setLocked(true);
                clearList();
                load(true);
                return true;
            }
        });
        wrap.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        root.addView(wrap, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        scroll.setOnScrollChangeListener(new View.OnScrollChangeListener() {
            @Override
            public void onScrollChange(View v, int sx, int sy, int osx, int osy) {
                View child = scroll.getChildAt(0);
                if (child == null) return;
                int remain = child.getBottom() - (scroll.getHeight() + sy);
                if (remain < BiliUi.dp(host, 320)) load(false);
            }
        });

        paintChannels();
        BiliThread.io(new Runnable() {
            @Override public void run() { BiliApiExt.ensureBuvid(host); }
        });
        load(true);
        return root;
    }

    private View buildTop() {
        LinearLayout row = new LinearLayout(host);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(BiliUi.dp(host, 14), BiliUi.dp(host, 8),
                BiliUi.dp(host, 14), BiliUi.dp(host, 6));

        LinearLayout search = BiliUi.pill(host, 0x33FFFFFF, 0x38FFFFFF, 21);
        search.setPadding(BiliUi.dp(host, 14), 0, BiliUi.dp(host, 14), 0);
        search.addView(BiliUi.icon(host, CatIcon.SEARCH, 17, BiliUi.SUB));
        TextView ph = BiliUi.text(host, "搜索视频 / UP 主", 13f, BiliUi.SUB, false);
        LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        plp.leftMargin = BiliUi.dp(host, 9);
        search.addView(ph, plp);
        search.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                host.push(new BiliSearchPage(host));
            }
        });
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(0,
                BiliUi.dp(host, 42), 1f);
        row.addView(search, slp);

        LinearLayout avatar = BiliUi.pill(host, 0x4D6EA6FF, 0x8C6EA6FF, 21);
        avatar.setGravity(Gravity.CENTER);
        
        final android.widget.ImageView avaImg = new android.widget.ImageView(host);
        avaImg.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);
        final CatIcon avaGhost = BiliUi.icon(host, CatIcon.USER, 19, BiliUi.ACCENT);
        avatar.addView(avaImg, new LinearLayout.LayoutParams(
                BiliUi.dp(host, 42), BiliUi.dp(host, 42)));
        FrameLayout.LayoutParams ghostLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER);
        avatar.addView(avaGhost, ghostLp);
        loadMyFace(avaImg, avaGhost);
        LinearLayout.LayoutParams alp = new LinearLayout.LayoutParams(
                BiliUi.dp(host, 42), BiliUi.dp(host, 42));
        alp.leftMargin = BiliUi.dp(host, 10);
        avatar.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { host.switchTab(2); }
        });
        row.addView(avatar, alp);
        return row;
    }

    private void loadMyFace(final android.widget.ImageView img, final CatIcon ghost) {
        BiliApi.Me cached = BiliApi.cachedMe;
        if (cached != null && cached.login && cached.face != null && cached.face.length() > 0) {
            BiliImage.attachAvatar(img, cached.face);
            ghost.setVisibility(View.GONE);
            return;
        }
        BiliThread.io(new Runnable() {
            @Override public void run() {
                final BiliApi.Me m = BiliApi.me(host);
                BiliThread.ui(new Runnable() {
                    @Override public void run() {
                        if (m.login && m.face != null && m.face.length() > 0) {
                            BiliImage.attachAvatar(img, m.face);
                            ghost.setVisibility(View.GONE);
                        }
                    }
                });
            }
        });
    }

    private View buildChannels() {
        channelBar = new LinearLayout(host);
        channelBar.setOrientation(LinearLayout.HORIZONTAL);
        channelBar.setGravity(Gravity.CENTER_VERTICAL);
        channelBar.setPadding(BiliUi.dp(host, 8), BiliUi.dp(host, 8),
                BiliUi.dp(host, 8), BiliUi.dp(host, 4));

        for (int i = 0; i < CHANNELS.length; i++) {
            final int idx = i;
            
            LinearLayout box = BiliUi.pill(host, 0x00000000, 0, 15);
            box.setGravity(Gravity.CENTER);
            box.setPadding(BiliUi.dp(host, 4), BiliUi.dp(host, 6),
                    BiliUi.dp(host, 4), BiliUi.dp(host, 6));

            TextView t = BiliUi.text(host, CHANNELS[i], 13f, BiliUi.SUB, false);
            t.setGravity(Gravity.CENTER);
            box.addView(t, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            box.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { onChannel(idx); }
            });
            channelTexts[i] = t;
            LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            blp.leftMargin = BiliUi.dp(host, 2);
            blp.rightMargin = BiliUi.dp(host, 2);
            channelBar.addView(box, blp);
        }
        return channelBar;
    }

    private void finishRefresh() {
        if (!refreshing) return;
        refreshing = false;
        if (refreshView != null) refreshView.stop();
        if (scroll != null) scroll.setLocked(false);
    }

    private void paintChannels() {
        for (int i = 0; i < channelTexts.length; i++) {
            TextView t = channelTexts[i];
            if (t == null) continue;
            boolean on = (i == mode);
            boolean available = (i >= 1 && i <= 4);   
            t.setTextColor(on ? BiliUi.MAIN : (available ? BiliUi.CARD_SUB : 0x669AA9BC));
            t.getPaint().setFakeBoldText(on);
            t.invalidate();

            android.view.View parent = (android.view.View) t.getParent();
            if (parent != null) {
                if (on) {
                    android.graphics.drawable.GradientDrawable g =
                            new android.graphics.drawable.GradientDrawable();
                    g.setColor(0x336EA6FF);
                    g.setStroke(Math.max(1, BiliUi.dp(host, 0.8f)), 0x8C6EA6FF);
                    g.setCornerRadius(BiliUi.dp(host, 15));
                    parent.setBackground(g);
                } else {
                    parent.setBackground(null);
                }
            }
        }
    }

    private void onChannel(int idx) {
        if (idx < 1 || idx > 4) {
            Toast.makeText(host, CHANNELS[idx] + " 还没接", Toast.LENGTH_SHORT).show();
            return;
        }
        if (idx == mode) return;
        mode = idx;
        paintChannels();
        clearList();
        load(true);
    }

    private void clearList() {
        colLeft.removeAllViews();
        colRight.removeAllViews();
        feedIdx = 1;
        popularPage = 1;
        noMore = false;
        loading = false;
    }

    private void load(final boolean refresh) {
        if (loading || noMore) return;
        loading = true;
        if (refresh) {
            status = BiliCards.loading(host, "加载中…");
            colLeft.addView(status);
        }
        final int m = mode;
        final int idx = (m == 1) ? feedIdx : popularPage;
        BiliThread.io(new Runnable() {
            @Override public void run() {
                BiliApiExt.ensureBuvid(host);
                final List<BiliApi.Item> list;
                if (m == 1) {
                    list = BiliApi.feed(host, idx);
                } else if (m == 2) {
                    list = BiliApi.popular(host, idx);
                } else if (m == 3) {
                    list = BiliApiExt.ranking(host);
                } else {
                    list = BiliApiExt.weekly(host);
                }
                BiliThread.ui(new Runnable() {
                    @Override public void run() {
                        onLoaded(list, refresh, m);
                    }
                });
            }
        });
    }

    private void onLoaded(List<BiliApi.Item> list, boolean refresh, int m) {
        loading = false;
        finishRefresh();
        if (m == 3 || m == 4) noMore = true;   
        BiliFilter.reload(host);
        if (m == 1 || m == 2) list = BiliFilter.apply(list);
        if (status != null && status.getParent() != null) {
            ((ViewGroup) status.getParent()).removeView(status);
        }
        status = null;
        if (list == null || list.isEmpty()) {
            noMore = true;
            if (colLeft.getChildCount() == 0 && colRight.getChildCount() == 0) {
                TextView t = BiliCards.loading(host, "没加载出来 · 点这里重试");
                t.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) { clearList(); load(true); }
                });
                colLeft.addView(t);
            }
            return;
        }
        int start = (m == 1 ? feedIdx : popularPage);
        for (int i = 0; i < list.size(); i++) {
            final BiliApi.Item it = list.get(i);
            View card = BiliCards.videoCard(host, it, cardWidth,
                    new View.OnClickListener() {
                        @Override public void onClick(View v) {
                            host.push(new BiliVideoPage(host, it));
                        }
                    });
            LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            clp.bottomMargin = BiliUi.dp(host, 10);
            card.setLayoutParams(clp);
            ((start + i) % 2 == 0 ? colLeft : colRight).addView(card);
        }
        if (m == 1) feedIdx++;
        else popularPage++;
    }
}
