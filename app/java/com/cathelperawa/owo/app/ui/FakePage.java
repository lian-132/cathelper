package com.cathelperawa.owo.app.ui;

import android.graphics.Bitmap;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.cathelperawa.owo.app.feature.FeatureCustomDevice;
import com.cathelperawa.owo.app.feature.FeatureFakeNetwork;
import com.cathelperawa.owo.app.feature.FeatureFakePhone;
import com.cathelperawa.owo.app.feature.FeatureFakeQua;

public final class FakePage extends Page {

    private static final String[][] ITEMS = {
            {FeatureFakeNetwork.K, "伪装网络状态", "让 TIM 以为你在 WIFI / 5G / 4G", "f_net"},
            {FeatureCustomDevice.K, "自定义设备信息", "自定义 device · model · manufacturer", "f_device"},
            {FeatureFakePhone.K, "伪造手机号", "账号与安全页面显示的手机号码", "f_phone"},
            {FeatureFakeQua.K, "伪造客户端版本", "对外宣称的 QUA 版本号与下载渠道", "f_qua"}};

    private final Handler ui = new Handler(Looper.getMainLooper());

    public FakePage(Page.Host host) {
        super(host);
    }

    private static final class ParamPanel {

        int kind;
        LinearLayout view;
        EditText[] fields;

        EditText field;
        TextView state;
        boolean open;
    }

    private void refreshHero(TextView tv) {
        int on = 0;
        for (String[] it : ITEMS) {
            if (swOn(it[0])) on++;
        }
        tv.setText(stateText(on));
        ((GradientDrawable) tv.getBackground()).setColor(on == 0 ? 0xFFFFFFFF : 0xFFFFC2DF);
        tv.setTextColor(on == 0 ? 0xFF7F90A6 : 0xFFF07AAE);
    }

    private static String stateText(int on) {
        if (on == 0) return "全部关闭";
        return on == ITEMS.length ? "已全部开启" : "已开启 " + on + " / " + ITEMS.length;
    }

    @Override
    public View build() {
        View[] out = new View[2];
        LinearLayout root = pageRoot("伪造功能", "让 TIM 看到你想让它看到的", out, true, 12);
        final View pageRoot = out[0];

        FrameLayout hero = new FrameLayout(ctx);
        GradientDrawable hg = new GradientDrawable();
        hg.setColor(0xFFFFFFFF);
        hg.setCornerRadius(dp(26));
        hg.setStroke(dp(1), 0x77FFFFFF);
        hero.setBackgroundDrawable(hg);
        hero.setClipToOutline(true);
        hero.setElevation(dp(7));

        Bitmap heroImg = image("h_fake");
        if (heroImg != null) {
            ImageView iv = new ImageView(ctx);
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            iv.setImageBitmap(heroImg);
            hero.addView(iv, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, dp(150)));
        }
        View veil = new View(ctx);
        veil.setBackgroundDrawable(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0x00FFFFFF, 0x99FFFFFF, 0xF8FFFFFF}));
        hero.addView(veil, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, dp(100), Gravity.BOTTOM));

        LinearLayout heroText = new LinearLayout(ctx);
        heroText.setOrientation(LinearLayout.VERTICAL);
        heroText.setPadding(dp(16), 0, dp(16), dp(14));

        int onCount = 0;
        for (String[] it : ITEMS) {
            if (swOn(it[0])) onCount++;
        }
        final TextView pill = pill(stateText(onCount), onCount == 0 ? 0xFF7F90A6 : 0xFFF07AAE);
        if (onCount > 0) {
            ((GradientDrawable) pill.getBackground()).setColor(0xFFFFC2DF);
        }
        LinearLayout pillRow = new LinearLayout(ctx);
        pillRow.setOrientation(LinearLayout.HORIZONTAL);
        pillRow.addView(pill, wrapWrap());
        heroText.addView(pillRow, matchWrap(dp(7)));
        heroText.addView(text("伪造功能", 20, 0xFF4A5872, true, dp(3)));
        heroText.addView(text("改的是「它看到的」，不是真实设备", 12, 0xFF8A99AC, false, 0));

        hero.addView(heroText, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM));
        root.addView(hero, matchWrap(dp(11)));

        for (int i = 0; i < ITEMS.length; i++) {
            final String[] it = ITEMS[i];
            final String key = it[0];
            final String title = it[1];
            final boolean isDevice = FeatureCustomDevice.K.equals(key);

            FrameLayout card = whiteFrame(dp(20), 0x8CFFFFFF);
            card.setClipToOutline(true);

            Bitmap bmp = image(it[3]);
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
            txt.addView(text(it[2], 11, 0xFF8496AE, false, 0));
            FrameLayout.LayoutParams tp = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    Gravity.START | Gravity.CENTER_VERTICAL);
            tp.leftMargin = dp(14);
            card.addView(txt, tp);

            final SwitchBox box = new SwitchBox(ctx, swOn(key));
            FrameLayout.LayoutParams bp = new FrameLayout.LayoutParams(
                    dp(52), dp(30), Gravity.END | Gravity.CENTER_VERTICAL);
            bp.rightMargin = dp(13);
            card.addView(box, bp);

            final LinearLayout holder = new LinearLayout(ctx);
            holder.setOrientation(LinearLayout.VERTICAL);
            holder.addView(card, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));

            final boolean isNetwork = FeatureFakeNetwork.K.equals(key);
            final boolean isPhone = FeatureFakePhone.K.equals(key);
            final boolean isQua = FeatureFakeQua.K.equals(key);
            final ParamPanel pp = isDevice ? buildDeviceParams(pageRoot)
                    : isNetwork ? buildNetworkParams(pageRoot)
                    : isPhone ? buildPhoneParams(pageRoot)
                    : isQua ? buildQuaParams(pageRoot) : null;
            if (pp != null) {
                LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                plp.topMargin = dp(8);
                holder.addView(pp.view, plp);

                pp.open = false;
                pp.view.setVisibility(View.GONE);
                updateState(pp);
                updateNetworkState(pp);
                updatePhoneState(pp);
                updateQuaState(pp);
            }

            box.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    boolean z = !swOn(key);
                    swSet(key, z);
                    box.setChecked(z);
                    press(v);
                    refreshHero(pill);
                    if (pp != null) {

                        updateState(pp);
                        updateNetworkState(pp);
                        updatePhoneState(pp);
                updateQuaState(pp);
                    }
                    toast(title + (z ? " 开启喵～" : " 关闭喵～"));
                }
            });
            card.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    press(v);
                    if (pp == null) {
                        toast(title + "：这个功能没有可调参数");
                        return;
                    }
                    toggleParams(pp, !pp.open, pageRoot);
                }
            });

            root.addView(holder, matchWrap(dp(11)));
        }

        TextView tip = text("开启后立即生效 · 留空的项用真实值", 11, 0xFFA8B4C4, false, 0);
        tip.setGravity(Gravity.CENTER);
        LinearLayout tipRow = new LinearLayout(ctx);
        tipRow.setOrientation(LinearLayout.HORIZONTAL);
        tipRow.setGravity(Gravity.CENTER);
        tipRow.addView(tip);
        root.addView(tipRow, matchWrap(0));

        fadeIn(root);
        return pageRoot;
    }

    private ParamPanel buildPhoneParams(final View pageRoot) {
        final ParamPanel pp = new ParamPanel();
        pp.kind = 3;

        LinearLayout panel = new LinearLayout(ctx);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(16), dp(10), dp(16), dp(12));
        panel.setVisibility(View.GONE);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xE6FFFFFF);
        bg.setCornerRadius(dp(18));
        bg.setStroke(Math.max(1, dp(1)), 0x99FFFFFF);
        panel.setBackgroundDrawable(bg);
        try { panel.setElevation(dp(2)); } catch (Throwable ignore) {}

        panel.addView(text("显示的号码", 11, 0xFF7F90A6, true, dp(2)));

        final EditText et = new EditText(ctx);
        et.setText(cfgGetS(FeatureFakePhone.CFG_PHONE, ""));
        et.setHint("留空 = " + FeatureFakePhone.DEFAULT_PHONE);
        et.setHintTextColor(0xFFA9BBD2);
        et.setTextSize(14);
        et.setTextColor(0xFF43587A);
        et.setSingleLine(true);
        et.setInputType(android.text.InputType.TYPE_CLASS_PHONE);
        et.setBackgroundDrawable(null);
        et.setPadding(dp(2), dp(10), dp(2), dp(9));
        panel.addView(et, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        View underline = new View(ctx);
        GradientDrawable ug = new GradientDrawable();
        ug.setColor(0x55F07CA8);
        underline.setBackgroundDrawable(ug);
        LinearLayout.LayoutParams ulp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, Math.max(1, dp(1)));
        ulp.bottomMargin = dp(6);
        panel.addView(underline, ulp);

        final TextView state = text("", 10, 0xFF97A9C0, false, 0);
        panel.addView(state);
        pp.view = panel;
        pp.field = et;
        pp.state = state;
        updatePhoneState(pp);

        final Handler h = new Handler(Looper.getMainLooper());
        final Runnable[] save = new Runnable[1];
        save[0] = new Runnable() {
            @Override public void run() {
                cfgSetS(FeatureFakePhone.CFG_PHONE, et.getText().toString().trim());
                updatePhoneState(pp);
                updateQuaState(pp);
            }
        };
        et.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {
                h.removeCallbacks(save[0]);
                h.postDelayed(save[0], 400);
            }
        });
        et.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override public void onFocusChange(View v, boolean has) {
                if (!has) return;
                v.postDelayed(new Runnable() {
                    @Override public void run() { scrollIntoView(pp.view, pageRoot); }
                }, 340);
            }
        });
        return pp;
    }

    private ParamPanel buildNetworkParams(final View pageRoot) {
        final ParamPanel pp = new ParamPanel();
        pp.kind = 2;

        LinearLayout panel = new LinearLayout(ctx);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(16), dp(10), dp(16), dp(12));
        panel.setVisibility(View.GONE);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xE6FFFFFF);
        bg.setCornerRadius(dp(18));
        bg.setStroke(Math.max(1, dp(1)), 0x99FFFFFF);
        panel.setBackgroundDrawable(bg);
        try { panel.setElevation(dp(2)); } catch (Throwable ignore) {}

        panel.addView(text("伪装成", 11, 0xFF7F90A6, true, dp(8)));

        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        final TextView[] chips = new TextView[3];
        final int[] modes = {FeatureFakeNetwork.MODE_WIFI,
                FeatureFakeNetwork.MODE_5G, FeatureFakeNetwork.MODE_4G};
        final String[] names = {"WIFI", "5G", "4G"};
        for (int i = 0; i < 3; i++) {
            final int mode = modes[i];
            TextView chip = text(names[i], 13, 0xFF7F90A6, true, 0);
            chip.setGravity(Gravity.CENTER);
            chip.setPadding(0, dp(9), 0, dp(9));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            if (i < 2) lp.rightMargin = dp(8);
            chip.setLayoutParams(lp);
            chip.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    cfgSetS(FeatureFakeNetwork.CFG_MODE, String.valueOf(mode));
                    press(v);
                    paintChips(chips, mode);
                    updateNetworkState(pp);
                }
            });
            chips[i] = chip;
            row.addView(chip);
        }
        panel.addView(row, matchWrap(dp(8)));

        final TextView state = text("", 10, 0xFF97A9C0, false, 0);
        panel.addView(state);
        pp.state = state;
        pp.view = panel;

        paintChips(chips, currentNetworkMode());
        updateNetworkState(pp);
        return pp;
    }

    private int currentNetworkMode() {
        String v = cfgGetS(FeatureFakeNetwork.CFG_MODE, "1");
        try {
            int m = Integer.parseInt(v.trim());
            return (m >= 1 && m <= 3) ? m : FeatureFakeNetwork.MODE_WIFI;
        } catch (Throwable t) {
            return FeatureFakeNetwork.MODE_WIFI;
        }
    }

    private void paintChips(TextView[] chips, int mode) {
        for (int i = 0; i < chips.length; i++) {
            boolean sel = (i + 1) == mode;
            GradientDrawable g = new GradientDrawable();
            g.setCornerRadius(dp(14));
            g.setColor(sel ? 0xFFFFC2DF : 0x66FFFFFF);
            g.setStroke(dp(1), sel ? 0xFFFF9FCB : 0x33FFFFFF);
            chips[i].setBackgroundDrawable(g);
            chips[i].setTextColor(sel ? 0xFFE85D9C : 0xFF7F90A6);
        }
    }

    private void updateQuaState(ParamPanel pp) {
        if (pp == null || pp.state == null || pp.fields == null || pp.kind != 4) return;
        String ver = pp.fields[0].getText().toString().trim();
        String code = pp.fields[1].getText().toString().trim();
        String chan = pp.fields[2].getText().toString().trim();
        String sub = pp.fields.length > 3
                ? pp.fields[3].getText().toString().trim() : "";
        boolean on = swOn(FeatureFakeQua.K);
        if (ver.length() == 0 && code.length() == 0 && chan.length() == 0
                && sub.length() == 0) {
            pp.state.setTextColor(0xFF97A9C0);
            pp.state.setText("全留空 = 保持真实版本不变\n留空的段自动用真实值");
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("对外显示：QQ ");
        sb.append(ver.length() == 0 ? "（真实版本）" : ver);
        if (code.length() > 0) sb.append(".").append(code);
        sb.append(chan.length() == 0 ? " · 真实渠道" : " · " + chan + " 渠道");
        if (sub.length() > 0) sb.append("\nSubId：" ).append(sub);
        if (!on) {
            pp.state.setTextColor(0xFFC0909E);
            pp.state.setText(sb + "\n上面的开关还关着 · 打开后才生效");
        } else {
            pp.state.setTextColor(0xFF7FB98D);
            pp.state.setText("✓ 已生效 · " + sb + "\n改的是宿主对外宣称的 QUA，重启宿主后最稳");
        }
    }

    private void updatePhoneState(ParamPanel pp) {
        if (pp == null || pp.state == null || pp.kind != 3) return;
        String raw = pp.field == null ? "" : pp.field.getText().toString().trim();
        String show = raw.length() == 0 ? FeatureFakePhone.DEFAULT_PHONE : raw;
        boolean on = swOn(FeatureFakePhone.K);
        if (!on) {
            pp.state.setTextColor(0xFFC0909E);
            pp.state.setText("开关还关着 · 打开后才显示\n号码：" + show);
        } else {
            pp.state.setTextColor(0xFF7FB98D);
            pp.state.setText("✓ 已生效 · 账号与安全页会显示\n" + show);
        }
    }

    private void updateNetworkState(ParamPanel pp) {
        if (pp == null || pp.state == null || pp.kind != 2) return;
        int m = currentNetworkMode();
        boolean on = swOn(FeatureFakeNetwork.K);
        String name = m == FeatureFakeNetwork.MODE_5G ? "5G"
                : m == FeatureFakeNetwork.MODE_4G ? "4G" : "WIFI";
        String effect = m == FeatureFakeNetwork.MODE_WIFI
                ? "TIM 会以为你在 WiFi 下（不省流、允许大文件）"
                : m == FeatureFakeNetwork.MODE_5G
                ? "TIM 会以为你在 5G 下（可能放开高清/大文件）"
                : "TIM 会以为你在 4G 蜂窝下（可能压缩图片）";
        if (!on) {
            pp.state.setTextColor(0xFFC0909E);
            pp.state.setText("开关还关着 · 打开后才会生效（当前选：" + name + "）");
        } else {
            pp.state.setTextColor(0xFF7FB98D);
            pp.state.setText("✓ 已生效 · 伪装成 " + name + "\n" + effect);
        }
    }

    private ParamPanel buildQuaParams(final View pageRoot) {
        final ParamPanel pp = new ParamPanel();
        pp.kind = 4;

        LinearLayout panel = new LinearLayout(ctx);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(16), dp(8), dp(16), dp(12));
        panel.setVisibility(View.GONE);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xE6FFFFFF);
        bg.setCornerRadius(dp(18));
        bg.setStroke(Math.max(1, dp(1)), 0x99FFFFFF);
        panel.setBackgroundDrawable(bg);
        try { panel.setElevation(dp(2)); } catch (Throwable ignore) {}

        EditText e1 = field(panel, "版本名（如 9.3.80）", "留空 = 真实版本",
                FeatureFakeQua.CFG_VERSION, pp);
        EditText e2 = field(panel, "版本号（如 50000）", "留空 = 真实版本号",
                FeatureFakeQua.CFG_CODE, pp);
        EditText e3 = field(panel, "下载渠道（如 OPPO / YYB）", "留空 = 真实渠道",
                FeatureFakeQua.CFG_CHANNEL, pp);
        EditText e4 = field(panel, "Sub ID（对面模块查表的数）", "留空 = 真实 SubId",
                FeatureFakeQua.CFG_SUBID, pp);
        pp.fields = new EditText[]{e1, e2, e3, e4};

        final TextView state = text("", 10, 0xFF97A9C0, false, 0);
        state.setPadding(0, dp(7), 0, 0);
        panel.addView(state);
        pp.state = state;
        pp.view = panel;
        updateQuaState(pp);

        panel.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                EditText target = firstEmpty(pp);
                if (target == null) return;
                target.requestFocus();
                showKeyboard(target);
            }
        });

        final long[] lastAuto = {0};
        panel.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override public void onGlobalLayout() {
                        if (panel.getVisibility() != View.VISIBLE) return;
                        if (!hasFocusedField(pp)) return;
                        long now = System.currentTimeMillis();
                        if (now - lastAuto[0] < 350) return;
                        lastAuto[0] = now;
                        scrollIntoView(panel, pageRoot);
                    }
                });

        return pp;
    }

    private ParamPanel buildDeviceParams(final View pageRoot) {
        final ParamPanel pp = new ParamPanel();
        pp.kind = 1;

        LinearLayout panel = new LinearLayout(ctx);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(16), dp(8), dp(16), dp(12));
        panel.setVisibility(View.GONE);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xE6FFFFFF);
        bg.setCornerRadius(dp(18));
        bg.setStroke(Math.max(1, dp(1)), 0x99FFFFFF);
        panel.setBackgroundDrawable(bg);
        try { panel.setElevation(dp(2)); } catch (Throwable ignore) {}

        String realDevice = nz(android.os.Build.DEVICE);
        String realModel = nz(android.os.Build.MODEL);
        String realMfr = nz(android.os.Build.MANUFACTURER);

        EditText e1 = field(panel, "设备代号 device", "留空 = " + realDevice,
                FeatureCustomDevice.CFG_DEVICE, pp);
        EditText e2 = field(panel, "型号 model", "留空 = " + realModel,
                FeatureCustomDevice.CFG_MODEL, pp);
        EditText e3 = field(panel, "厂商 manufacturer", "留空 = " + realMfr,
                FeatureCustomDevice.CFG_MFR, pp);
        pp.fields = new EditText[]{e1, e2, e3};

        final TextView state = text("", 10, 0xFF97A9C0, false, 0);
        state.setPadding(0, dp(7), 0, 0);
        panel.addView(state);
        pp.state = state;
        pp.view = panel;
        updateState(pp);

        panel.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                EditText target = firstEmpty(pp);
                if (target == null) return;
                target.requestFocus();
                showKeyboard(target);
            }
        });

        final long[] lastAuto = {0};
        panel.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override public void onGlobalLayout() {
                        if (panel.getVisibility() != View.VISIBLE) return;
                        if (!hasFocusedField(pp)) return;
                        long now = System.currentTimeMillis();
                        if (now - lastAuto[0] < 350) return;
                        lastAuto[0] = now;
                        scrollIntoView(panel, pageRoot);
                    }
                });

        return pp;
    }

    private EditText field(LinearLayout panel, String label, String hintText,
                           final String cfgKey, final ParamPanel pp) {
        TextView lab = text(label, 11, 0xFF7F90A6, true, dp(4));
        panel.addView(lab);

        final EditText et = new EditText(ctx);
        et.setText(cfgGetS(cfgKey, ""));
        et.setHint(hintText);
        et.setHintTextColor(0xFFA9BBD2);
        et.setTextSize(14);
        et.setTextColor(0xFF43587A);
        et.setSingleLine(true);
        et.setBackgroundDrawable(null);
        et.setPadding(dp(2), dp(10), dp(2), dp(9));
        panel.addView(et, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        View underline = new View(ctx);
        GradientDrawable ug = new GradientDrawable();
        ug.setColor(0x55F07CA8);
        underline.setBackgroundDrawable(ug);
        LinearLayout.LayoutParams ulp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, Math.max(1, dp(1)));
        ulp.bottomMargin = dp(6);
        panel.addView(underline, ulp);

        final Handler h = new Handler(Looper.getMainLooper());
        final Runnable[] save = new Runnable[1];
        save[0] = new Runnable() {
            @Override public void run() {
                cfgSetS(cfgKey, et.getText().toString().trim());
                updateState(pp);
                updateQuaState(pp);
            }
        };
        et.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {
                h.removeCallbacks(save[0]);
                h.postDelayed(save[0], 400);
            }
        });

        et.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override public void onFocusChange(View v, boolean has) {
                if (!has) return;
                v.postDelayed(new Runnable() {
                    @Override public void run() { scrollIntoView(pp.view, pp.view); }
                }, 340);
            }
        });
        return et;
    }

    private void updateState(ParamPanel pp) {
        if (pp == null || pp.state == null || pp.fields == null || pp.kind != 1) return;
        boolean on = swOn(FeatureCustomDevice.K);
        StringBuilder filled = new StringBuilder();
        for (int i = 0; i < pp.fields.length; i++) {
            String v = pp.fields[i].getText().toString().trim();
            if (v.length() == 0) continue;
            if (filled.length() > 0) filled.append(" · ");
            filled.append(i == 0 ? "device=" : i == 1 ? "model=" : "厂商=").append(v);
        }
        if (!on) {
            pp.state.setTextColor(0xFFC0909E);
            pp.state.setText(filled.length() == 0
                    ? "上面的开关还关着 · 打开后才会生效"
                    : "已填「" + filled + "」· 打开上面的开关才生效");
        } else if (filled.length() == 0) {
            pp.state.setTextColor(0xFF97A9C0);
            pp.state.setText("全留空 = 保持真实机型不变");
        } else {
            pp.state.setTextColor(0xFF7FB98D);

            pp.state.setText("✓ 已生效 · " + filled
                    + "\nTIM 当前看到：model=" + android.os.Build.MODEL
                    + " · device=" + android.os.Build.DEVICE
                    + " · " + android.os.Build.MANUFACTURER);
        }
    }

    private static EditText firstEmpty(ParamPanel pp) {
        if (pp == null || pp.fields == null || pp.fields.length == 0) return null;
        for (EditText e : pp.fields) {
            if (e.getText().toString().trim().length() == 0) return e;
        }
        return pp.fields[0];
    }

    private static boolean hasFocusedField(ParamPanel pp) {
        if (pp == null || pp.fields == null) return false;
        for (EditText e : pp.fields) {
            if (e.hasFocus()) return true;
        }
        return false;
    }

    private void toggleParams(final ParamPanel pp, final boolean wantOpen, final View pageRoot) {
        if (pp == null || pp.open == wantOpen) return;
        pp.open = wantOpen;

        final LinearLayout panel = pp.view;
        final ViewGroup.LayoutParams raw = panel.getLayoutParams();
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

        if (wantOpen) { updateState(pp); updateQuaState(pp); }

        android.animation.ValueAnimator va =
                android.animation.ValueAnimator.ofInt(lp.height, wantOpen ? full : 0);
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
                    if (pp.fields != null) {
                        for (EditText e : pp.fields) {
                            hideKeyboard(e);
                            e.clearFocus();
                        }
                    }
                    panel.setVisibility(View.GONE);
                    updateState(pp);
                }
            }
        });
        va.start();
    }

    private static String nz(String v) {
        return v == null || v.length() == 0 ? "（未知）" : v;
    }
}
