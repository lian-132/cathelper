package com.cathelperawa.owo.app.script;

import android.app.Activity;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.cathelperawa.owo.app.core.HookLog;

import java.util.ArrayList;
import java.util.List;

public final class ScriptHubPanel {

    private static android.widget.PopupWindow pop;
    private static int indexH = 0, indexV = 0;
    private static boolean inVertical = false;
    private static ScrollView listSv;
    private static ScriptHub.Page single;
    
    private static long lastSwitch = 0;

    private ScriptHubPanel() {}

    public static boolean showSingle(Activity a, ScriptHub.Page page) {
        if (a == null || page == null) return false;
        single = page;
        indexH = 0;
        indexV = 0;
        inVertical = false;
        return show(a);
    }

    private static List<ScriptHub.Page> pageList() {
        if (single != null) {
            List<ScriptHub.Page> one = new ArrayList<ScriptHub.Page>();
            one.add(single);
            return one;
        }
        List<ScriptHub.Page> l = inVertical ? ScriptHub.vertical() : ScriptHub.horizontal();
        if (l.isEmpty()) {
            inVertical = !inVertical;
            l = inVertical ? ScriptHub.vertical() : ScriptHub.horizontal();
        }
        return l;
    }

    private static int curIndex() {
        if (single != null) return 0;
        return inVertical ? indexV : indexH;
    }

    private static void setIndex(int i) {
        if (single != null) return;
        if (inVertical) indexV = i; else indexH = i;
    }

    public static boolean show(final Activity a) {
        if (a == null || pageList().isEmpty()) return false;
        if (curIndex() < 0 || curIndex() >= pageList().size()) setIndex(0);
        close();
        try {
            build(a);
            return true;
        } catch (Throwable t) {
            HookLog.e("[公共球] 弹窗失败", t);
            return false;
        }
    }

    public static void close() {
        try {
            if (pop != null) pop.dismiss();
        } catch (Throwable ignore) {
        }
        pop = null;
        listSv = null;
        single = null;
    }

    private static void build(final Activity a) {
        
        try {
            if (pop != null && pop.isShowing()) pop.dismiss();
        } catch (Throwable ignore) {
        }
        pop = null;

        final float den = a.getResources().getDisplayMetrics().density;

        final boolean[] atTop = {true};
        final boolean[] atBottom = {true};

        final GestureDetector gd = new GestureDetector(a,
                new GestureDetector.SimpleOnGestureListener() {
                    @Override public boolean onDown(MotionEvent e) {
                        return true;   
                    }

                    @Override public boolean onFling(MotionEvent e1, MotionEvent e2,
                                                     float vx, float vy) {
                        if (e1 == null || e2 == null) return false;
                        if (System.currentTimeMillis() - lastSwitch < 350) return false;

                        float dx = e2.getRawX() - e1.getRawX();
                        float dy = e2.getRawY() - e1.getRawY();

                        boolean horizontal = Math.abs(dx) > Math.abs(dy) * 2.2f;
                        boolean vertical = Math.abs(dy) > Math.abs(dx) * 2.2f;

                        if (horizontal && Math.abs(dx) > dp(den, 50) && single == null) {
                            List<ScriptHub.Page> hs = ScriptHub.horizontal();
                            if (hs.isEmpty()) return false;
                            inVertical = false;
                            if (hs.size() > 1) {
                                int n = hs.size();
                                indexH = (dx < 0) ? (indexH + 1) % n : (indexH - 1 + n) % n;
                                repaint(a);
                                return true;
                            }
                            return false;
                        }

                        if (vertical && Math.abs(dy) > dp(den, 50) && single == null) {
                            List<ScriptHub.Page> vs = ScriptHub.vertical();
                            if (vs.isEmpty()) return false;
                            boolean goNext = dy < 0 && atBottom[0];
                            boolean goPrev = dy > 0 && atTop[0];
                            if (!goNext && !goPrev) return false;
                            int n = vs.size();
                            if (n < 2 && inVertical) return false;
                            inVertical = true;
                            indexV = goNext ? (indexV + 1) % n : (indexV - 1 + n) % n;
                            repaint(a);
                            return true;
                        }
                        return false;
                    }
                });

        final FrameLayout root = new FrameLayout(a) {
            @Override public boolean dispatchTouchEvent(MotionEvent e) {
                if (e.getActionMasked() == MotionEvent.ACTION_DOWN && listSv != null) {
                    atTop[0] = !listSv.canScrollVertically(-1);
                    atBottom[0] = !listSv.canScrollVertically(1);
                }
                try {
                    gd.onTouchEvent(e);
                } catch (Throwable ignore) {
                }
                return super.dispatchTouchEvent(e);
            }
        };

        final int cardW = Math.max(dp(den, 200),
                Math.min(dp(den, 320),
                        (int) (a.getResources().getDisplayMetrics().widthPixels * 0.68f)));
        LinearLayout card = buildCard(a, cardW);
        root.addView(card, new FrameLayout.LayoutParams(cardW,
                FrameLayout.LayoutParams.WRAP_CONTENT));

        pop = new android.widget.PopupWindow(root,
                cardW, ViewGroup.LayoutParams.WRAP_CONTENT, false);
        pop.setBackgroundDrawable(new ColorDrawable(0x00000000));
        pop.setOutsideTouchable(true);
        pop.setClippingEnabled(true);

        try {
            if (ScriptFloat.ballView != null && ScriptFloat.ballView.getParent() != null) {
                pop.showAsDropDown(ScriptFloat.ballView, -dp(den, 10), dp(den, 2));
                return;
            }
        } catch (Throwable ignore) {
        }
        pop.showAtLocation(a.getWindow().getDecorView(), Gravity.CENTER, 0, 0);
    }

    private static void repaint(Activity a) {
        lastSwitch = System.currentTimeMillis();
        try {
            build(a);
        } catch (Throwable ignore) {
        }
    }

    

    private static LinearLayout buildCard(final Activity a, int cardW) {
        final float den = a.getResources().getDisplayMetrics().density;
        final List<ScriptHub.Page> pages = pageList();
        if (pages.isEmpty()) return new LinearLayout(a);
        if (curIndex() < 0 || curIndex() >= pages.size()) setIndex(0);
        final ScriptHub.Page page = pages.get(curIndex());

        LinearLayout card = new LinearLayout(a);
        card.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable g = new GradientDrawable();
        g.setColor(0xFFFCFDFF);
        g.setCornerRadius(dp(den, 16));
        card.setBackgroundDrawable(g);
        card.setPadding(dp(den, 4), dp(den, 4), dp(den, 4), dp(den, 6));
        card.setMinimumWidth(cardW);

        LinearLayout bar = new LinearLayout(a);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(den, 10), dp(den, 8), dp(den, 6), dp(den, 6));

        ImageView ic = new ImageView(a);
        Drawable d = ScriptFloat.moduleIcon(a, page.icon);
        if (d != null) ic.setImageDrawable(d);
        ic.setScaleType(ImageView.ScaleType.CENTER_CROP);
        LinearLayout.LayoutParams icLp = new LinearLayout.LayoutParams(dp(den, 20), dp(den, 20));
        icLp.rightMargin = dp(den, 7);
        bar.addView(ic, icLp);

        TextView title = new TextView(a);
        title.setText(page.title);
        title.setTextSize(12);
        title.setTextColor(0xFF9FB0C7);
        title.setSingleLine(true);
        bar.addView(title, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView x = new TextView(a);
        x.setText("✕");
        x.setTextSize(13);
        x.setTextColor(0xFFAEBACA);
        x.setPadding(dp(den, 10), dp(den, 2), dp(den, 6), dp(den, 2));
        x.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                close();
            }
        });
        bar.addView(x);
        card.addView(bar);

        LinearLayout list = new LinearLayout(a);
        list.setOrientation(LinearLayout.VERTICAL);

        for (int i = 0; i < page.items.size(); i++) {
            final ScriptHub.Item it = page.items.get(i);
            list.addView(row(a, it.label, 0xFF3C4B60, new Runnable() {
                @Override public void run() {
                    close();
                    ScriptFloat.runHandler(it.script, it.id, it.label, it.handler);
                }
            }));
        }

        if (page.onRun != null) {
            list.addView(row(a, "执行", 0xFF3C78D8, new Runnable() {
                @Override public void run() {
                    close();
                    ScriptFloat.runHandler(page.script, "run", page.title, page.onRun);
                }
            }));
        }

        try {
            List<ScriptPanelRegistry.Panel> panels = ScriptPanelRegistry.ofScript(page.script);
            for (int i = 0; i < panels.size(); i++) {
                final ScriptPanelRegistry.Panel pn = panels.get(i);
                list.addView(row(a, pn.title, 0xFF3C4B60, new Runnable() {
                    @Override public void run() {
                        close();
                        ScriptFloat.openPanel(a, pn);
                    }
                }));
            }
        } catch (Throwable ignore) {
        }

        ScrollView sv = new ScrollView(a);
        sv.setVerticalScrollBarEnabled(false);
        listSv = sv;
        sv.addView(list, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.WRAP_CONTENT));
        int contentH = 0;
        try {
            list.measure(View.MeasureSpec.makeMeasureSpec(
                            Math.max(0, cardW - dp(den, 8)), View.MeasureSpec.AT_MOST),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
            contentH = list.getMeasuredHeight();
        } catch (Throwable ignore) {
        }
        int maxH = Math.min(dp(den, 320),
                (int) (a.getResources().getDisplayMetrics().heightPixels * 0.42f));
        int useH = contentH > 0 ? Math.min(contentH, maxH) : ViewGroup.LayoutParams.WRAP_CONTENT;
        card.addView(sv, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, useH));

        card.addView(bottomBar(a, pages));

        return card;
    }

    private static View bottomBar(final Activity a, List<ScriptHub.Page> pages) {
        final float den = a.getResources().getDisplayMetrics().density;
        LinearLayout bar = new LinearLayout(a);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(den, 6), dp(den, 4), dp(den, 6), 0);

        View line = new View(a);
        line.setBackgroundColor(0x0F000000);
        
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0x00000000);
        bar.setBackgroundDrawable(bg);

        TextView mgr = chip(a, "管理");
        mgr.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                close();
                try {
                    android.content.Intent it = new android.content.Intent();
                    it.setClassName(a.getPackageName(),
                            "com.cathelperawa.owo.app.ConsoleActivity");
                    it.putExtra("cat_page", "script");
                    a.startActivity(it);
                } catch (Throwable t) {
                    HookLog.w("[公共球] 打不开脚本页 " + t);
                }
            }
        });
        bar.addView(mgr);

        TextView hide = chip(a, "隐藏");
        hide.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                close();
                ScriptFloat.hideHubBall();
            }
        });
        LinearLayout.LayoutParams hideLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        hideLp.leftMargin = dp(den, 6);
        bar.addView(hide, hideLp);

        View space = new View(a);
        bar.addView(space, new LinearLayout.LayoutParams(0, 1, 1f));

        if (single == null && pages.size() > 1) {

            TextView prev = arrow(a, inVertical ? "↑" : "‹");
            prev.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    if (System.currentTimeMillis() - lastSwitch < 350) return;
                    setIndex((curIndex() - 1 + pageList().size()) % pageList().size());
                    repaint(a);
                }
            });
            bar.addView(prev);

            
            TextView pageNo = new TextView(a);
            pageNo.setText((inVertical ? "上下 " : "左右 ")
                    + (curIndex() + 1) + "/" + pages.size());
            pageNo.setTextSize(11);
            pageNo.setTextColor(0xFF8A97A8);
            pageNo.setPadding(dp(den, 4), 0, dp(den, 4), 0);
            bar.addView(pageNo);

            TextView next = arrow(a, inVertical ? "↓" : "›");
            next.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    if (System.currentTimeMillis() - lastSwitch < 350) return;
                    setIndex((curIndex() + 1) % pageList().size());
                    repaint(a);
                }
            });
            bar.addView(next);
        }

        return bar;
    }

    private static View row(Activity a, String text, int color, final Runnable onClick) {
        float den = a.getResources().getDisplayMetrics().density;
        TextView tv = new TextView(a);
        tv.setText(text);
        tv.setTextSize(14);
        tv.setTextColor(color);
        tv.setSingleLine(true);
        tv.setPadding(dp(den, 12), dp(den, 11), dp(den, 14), dp(den, 11));
        tv.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                try { onClick.run(); } catch (Throwable ignore) {}
            }
        });
        return tv;
    }

    private static TextView chip(Activity a, String text) {
        float den = a.getResources().getDisplayMetrics().density;
        TextView tv = new TextView(a);
        tv.setText(text);
        tv.setTextSize(11.5f);
        tv.setTextColor(0xFF7C8AA0);
        tv.setPadding(dp(den, 9), dp(den, 5), dp(den, 9), dp(den, 5));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFFF1F4F9);
        bg.setCornerRadius(dp(den, 9));
        tv.setBackgroundDrawable(bg);
        return tv;
    }

    private static TextView arrow(Activity a, String s) {
        float den = a.getResources().getDisplayMetrics().density;
        TextView tv = new TextView(a);
        tv.setText(s);
        tv.setTextSize(15);
        tv.setTextColor(0xFF8A97A8);
        tv.setPadding(dp(den, 7), 0, dp(den, 7), 0);
        return tv;
    }

    private static int dp(float den, float v) {
        return (int) (v * den + 0.5f);
    }
}
