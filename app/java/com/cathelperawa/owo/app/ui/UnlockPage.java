package com.cathelperawa.owo.app.ui;

import android.graphics.Bitmap;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.cathelperawa.owo.app.feature.FeatureUnlockForward;
import com.cathelperawa.owo.app.feature.FeatureUnlockMultiRecall;
import com.cathelperawa.owo.app.feature.FeatureUnlockPatPat;
import com.cathelperawa.owo.app.ui.Page;

public final class UnlockPage extends Page {
    private static final String[][] ITEMS = {new String[]{FeatureUnlockForward.K, "去除转发限制", "解除转发时的人数上限"}, new String[]{FeatureUnlockPatPat.K, "去除拍一拍限制", "拍一拍不限次数"}, new String[]{FeatureUnlockMultiRecall.K, "多选撤回", "长按进多选，批量撤回自己的消息"}};

    public UnlockPage(Page.Host host) {
        super(host);
    }

    public void refreshHero(TextView textView) {
        int i = 0;
        for (String[] strArr : ITEMS) {
            if (swOn(strArr[0])) {
                i++;
            }
        }
        textView.setText(stateText(i));
        ((GradientDrawable) textView.getBackground()).setColor(i == 0 ? -1 : -3850);
        textView.setTextColor(i == 0 ? -8411674 : -1016664);
    }

    private static String stateText(int i) {
        if (i == 0) {
            return "全部关闭";
        }
        String[][] strArr = ITEMS;
        return i == strArr.length ? "已全部开启" : "已开启 " + i + " / " + strArr.length;
    }

    @Override
    public View build() {
        View[] viewArr = new View[2];
        LinearLayout pageRoot = pageRoot("去除限制", "让 TIM 少管一点", viewArr, true, 12);
        FrameLayout frameLayout = new FrameLayout(this.ctx);
        GradientDrawable gradientDrawable = new GradientDrawable();
        int i = -1;
        gradientDrawable.setColor(-1);
        gradientDrawable.setCornerRadius(dp(26.0f));
        gradientDrawable.setStroke(dp(1.0f), 2013265919);
        frameLayout.setBackgroundDrawable(gradientDrawable);
        int r12 = 1;
        frameLayout.setClipToOutline(true);
        frameLayout.setElevation(dp(7.0f));
        Bitmap image = image("h_unlock");
        if (image != null) {
            ImageView imageView = new ImageView(this.ctx);
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imageView.setImageBitmap(image);
            frameLayout.addView(imageView, new FrameLayout.LayoutParams(-1, dp(150.0f)));
        }
        View view = new View(this.ctx);
        view.setBackgroundDrawable(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{16777215, -1711276033, -83886081}));
        frameLayout.addView(view, new FrameLayout.LayoutParams(-1, dp(100.0f), 80));
        LinearLayout linearLayout = new LinearLayout(this.ctx);
        linearLayout.setOrientation(1);
        linearLayout.setPadding(dp(16.0f), 0, dp(16.0f), dp(14.0f));
        int i2 = 0;
        for (String[] strArr : ITEMS) {
            if (swOn(strArr[0])) {
                i2++;
            }
        }
        final TextView pill = pill(stateText(i2), i2 == 0 ? -8411674 : -1016664);
        if (i2 > 0) {
            ((GradientDrawable) pill.getBackground()).setColor(-3850);
        }
        LinearLayout linearLayout2 = new LinearLayout(this.ctx);
        linearLayout2.setOrientation(0);
        linearLayout2.addView(pill, wrapWrap());
        linearLayout.addView(linearLayout2, matchWrap(dp(7.0f)));
        char c = 0;
        linearLayout.addView(text("去除限制", 20.0f, -11574139, true, dp(3.0f)));
        linearLayout.addView(text("被 TIM 藏起来的能力，帮你拿回来", 12.0f, -7693131, false, 0));
        frameLayout.addView(linearLayout, new FrameLayout.LayoutParams(-1, -2, 80));
        pageRoot.addView(frameLayout, matchWrap(dp(11.0f)));
        String[] strArr2 = {"u_forward", "u_patpat", "u_revoke"};
        int i3 = 0;
        while (true) {
            String[][] strArr3 = ITEMS;
            if (i3 >= strArr3.length) {
                TextView text = text("开启后立即生效，无需重启 TIM", 11.0f, -5720375, false, 0);
                text.setGravity(17);
                LinearLayout linearLayout3 = new LinearLayout(this.ctx);
                linearLayout3.setOrientation(0);
                linearLayout3.setGravity(17);
                linearLayout3.addView(text);
                pageRoot.addView(linearLayout3, matchWrap(0));
                fadeIn(pageRoot);
                return viewArr[0];
            }
            String[] strArr4 = strArr3[i3];
            final String str = strArr4[c];
            final String str2 = strArr4[r12];
            String str3 = strArr4[2];
            String str4 = strArr2[Math.min(i3, 2)];
            FrameLayout whiteFrame = whiteFrame(dp(20.0f), -1929379841);
            whiteFrame.setClipToOutline(true);
            Bitmap image2 = image(str4);
            if (image2 != null) {
                ImageView imageView2 = new ImageView(this.ctx);
                imageView2.setScaleType(ImageView.ScaleType.CENTER_CROP);
                imageView2.setImageBitmap(image2);
                whiteFrame.addView(imageView2, new FrameLayout.LayoutParams(i, dp(78.0f)));
                View view2 = new View(this.ctx);
                view2.setBackgroundDrawable(new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, new int[]{-134217729, -1493172225, 872415231}));
                whiteFrame.addView(view2, new FrameLayout.LayoutParams(i, dp(78.0f)));
            }
            LinearLayout linearLayout4 = new LinearLayout(this.ctx);
            linearLayout4.setOrientation(1);
            linearLayout4.addView(text(str2, 16.0f, -12363654, true, dp(2.0f)));
            linearLayout4.addView(text(str3, 11.0f, -8087890, false, 0));
            FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(-2, -2, 8388627);
            layoutParams.leftMargin = dp(14.0f);
            whiteFrame.addView(linearLayout4, layoutParams);
            final SwitchBox switchBox = new SwitchBox(this.ctx, swOn(str));
            FrameLayout.LayoutParams layoutParams2 = new FrameLayout.LayoutParams(dp(52.0f), dp(30.0f), 8388629);
            layoutParams2.rightMargin = dp(13.0f);
            whiteFrame.addView(switchBox, layoutParams2);
            this.host.bindSwitch(str, switchBox, new Runnable() {
                @Override
                public void run() {
                    UnlockPage.this.refreshHero(pill);
                }
            });
            whiteFrame.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view3) {
                    StringBuilder append;
                    String str5;
                    boolean z = !UnlockPage.this.swOn(str);
                    UnlockPage.this.swSet(str, z);
                    switchBox.setChecked(z);
                    UnlockPage.this.press(view3);
                    UnlockPage.this.refreshHero(pill);
                    UnlockPage unlockPage = UnlockPage.this;
                    if (z) {
                        append = new StringBuilder().append(str2);
                        str5 = " 开启喵～";
                    } else {
                        append = new StringBuilder().append(str2);
                        str5 = " 关闭喵～";
                    }
                    unlockPage.toast(append.append(str5).toString());
                }
            });
            switchBox.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view3) {
                    StringBuilder append;
                    String str5;
                    boolean z = !UnlockPage.this.swOn(str);
                    UnlockPage.this.swSet(str, z);
                    switchBox.setChecked(z);
                    UnlockPage.this.press(view3);
                    UnlockPage.this.refreshHero(pill);
                    UnlockPage unlockPage = UnlockPage.this;
                    if (z) {
                        append = new StringBuilder().append(str2);
                        str5 = " 开启喵～";
                    } else {
                        append = new StringBuilder().append(str2);
                        str5 = " 关闭喵～";
                    }
                    unlockPage.toast(append.append(str5).toString());
                }
            });
            pageRoot.addView(whiteFrame, matchWrap(dp(11.0f)));
            i3++;
            c = 0;
            i = -1;
            r12 = 1;
        }
    }
}
