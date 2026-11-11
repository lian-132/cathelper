package com.cathelperawa.owo.app.cookie;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class CookieAccountsUi {

    private CookieAccountsUi() {}

    private static final int C_MAIN = 0xFFEAF1FB;
    private static final int C_SUB = 0xFF9AA9BC;
    private static final int C_OK = 0xFF37C28D;

    public static void build(final Activity act, LinearLayout container) {
        if (act == null || container == null) return;
        container.removeAllViews();

        for (String group : CookiePlatforms.groups()) {
            List<CookiePlatforms.P> list = CookiePlatforms.byGroup(group);
            if (list.isEmpty()) continue;
            container.addView(groupTitle(act, group + " · " + list.size()));
            for (CookiePlatforms.P p : list) {
                container.addView(row(act, p));
            }
        }

        TextView tip = new TextView(act);
        tip.setText("登录态存在本应用，供 TIM 里的模块使用；"
                + "Cookie 会过期，失效时回来重新登录即可。\n按返回键回到设置页。");
        tip.setTextSize(11f);
        tip.setTextColor(0xFF6F7D90);
        tip.setLineSpacing(dp(act, 3), 1f);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(act, 6);
        tip.setLayoutParams(lp);
        container.addView(tip);
    }

    private static View row(final Activity act, final CookiePlatforms.P p) {
        LinearLayout card = new LinearLayout(act);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setBackgroundResource(com.cathelperawa.owo.app.R.drawable.bg_card);
        int pad = dp(act, 13);
        card.setPadding(pad, pad, pad, pad);
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        clp.bottomMargin = dp(act, 9);
        card.setLayoutParams(clp);

        TextView icon = new TextView(act);
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(p.ready ? p.color : 0xFF39434F);
        icon.setBackground(bg);
        icon.setGravity(Gravity.CENTER);
        icon.setText(p.shortName);
        icon.setTextColor(Color.WHITE);
        icon.setTextSize(p.shortName.length() > 1 ? 11f : 15f);
        icon.setTypeface(null, Typeface.BOLD);
        int size = dp(act, 40);
        LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(size, size);
        ilp.rightMargin = dp(act, 12);
        card.addView(icon, ilp);

        LinearLayout texts = new LinearLayout(act);
        texts.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        card.addView(texts, tlp);

        TextView name = new TextView(act);
        name.setText(p.name);
        name.setTextSize(15f);
        name.setTextColor(C_MAIN);
        name.setTypeface(null, Typeface.BOLD);
        texts.addView(name);

        CookieStore.Entry e = CookieStore.get(act, p.id);
        TextView state = new TextView(act);
        state.setTextSize(11.5f);
        if (!p.ready) {
            state.setText("未接入 · 解析器还没写");
            state.setTextColor(0xFF6F7D90);
        } else if (isPan(p) && (e == null || !e.valid())) {
            state.setText("已接入 · 游客也能下小文件，大文件先登录");
            state.setTextColor(0xFF9AA9BC);
        } else if (e != null) {
            String label = e.label == null || e.label.length() == 0 ? "已登录" : "已登录 · " + e.label;
            state.setText(label + "（" + fmtTime(e.savedAt) + "）");
            state.setTextColor(C_OK);
        } else {
            state.setText("未登录");
            state.setTextColor(C_SUB);
        }
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        slp.topMargin = dp(act, 3);
        texts.addView(state, slp);

        TextView action = new TextView(act);
        action.setTextSize(12f);
        if (!p.ready) {
            action.setText("—");
            action.setTextColor(0xFF6F7D90);
        } else if (isPan(p)) {
            action.setText("下载 ›");
            action.setTextColor(0xFF6EA6FF);
        } else {
            action.setText(e != null ? "重登 ›" : "去登录 ›");
            action.setTextColor(0xFF6EA6FF);
        }
        LinearLayout.LayoutParams alp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        alp.leftMargin = dp(act, 8);
        card.addView(action, alp);

        card.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (!p.ready) {
                    Toast.makeText(act, p.name + " 还没接入，先把网易云跑通",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                try {
                    if (isPan(p)) {
                        
                        Intent it = new Intent(act, com.cathelperawa.owo.app.pan.PanActivity.class);
                        it.putExtra(com.cathelperawa.owo.app.pan.PanActivity.EXTRA_PLATFORM, p.id);
                        act.startActivity(it);
                    } else {
                        Intent it = new Intent(act, CookieLoginActivity.class);
                        it.putExtra(CookieLoginActivity.EXTRA_PLATFORM, p.id);
                        act.startActivity(it);
                    }
                } catch (Throwable t) {
                    Toast.makeText(act, "打开失败：" + t, Toast.LENGTH_SHORT).show();
                }
            }
        });
        return card;
    }

    private static boolean isPan(CookiePlatforms.P p) {
        return p != null && "网盘".equals(p.group)
                && com.cathelperawa.owo.app.pan.PanProvider.supported(p.id);
    }

    private static TextView groupTitle(Activity act, String text) {
        TextView tv = new TextView(act);
        tv.setText(text);
        tv.setTextSize(12f);
        tv.setTextColor(0xFF8395AD);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(act, 10);
        lp.bottomMargin = dp(act, 8);
        lp.leftMargin = dp(act, 4);
        tv.setLayoutParams(lp);
        return tv;
    }

    private static int dp(Activity a, float v) {
        return (int) (v * a.getResources().getDisplayMetrics().density + 0.5f);
    }

    private static String fmtTime(long ms) {
        if (ms <= 0) return "未知";
        SimpleDateFormat f = new SimpleDateFormat("MM-dd HH:mm", Locale.CHINA);
        return f.format(new Date(ms));
    }
}
