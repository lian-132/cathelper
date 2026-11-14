package com.cathelperawa.owo.app.bili;

import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.cathelperawa.owo.app.ui.CatIcon;

public final class BiliDynamicPage extends BiliPage {

    public BiliDynamicPage(BiliActivity host) {
        super(host);
    }

    @Override
    public View onCreate() {
        LinearLayout root = new LinearLayout(host);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);

        CatIcon ic = BiliUi.icon(host, CatIcon.DYNAMIC, 44, 0x4D9AA9BC);
        root.addView(ic);

        TextView t1 = BiliUi.text(host, "动态还没接", 14f, BiliUi.MAIN, true);
        t1.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams lp1 = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp1.topMargin = BiliUi.dp(host, 16);
        root.addView(t1, lp1);

        TextView t2 = BiliUi.text(host,
                "关注动态需要登录态，且卡片结构复杂（转发/图文/视频混排）\n先留着位置，二期跟「观看记录」一起做",
                11.5f, BiliUi.SUB, false);
        t2.setGravity(Gravity.CENTER);
        t2.setLineSpacing(BiliUi.dp(host, 4), 1f);
        LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp2.topMargin = BiliUi.dp(host, 10);
        root.addView(t2, lp2);
        return root;
    }
}
