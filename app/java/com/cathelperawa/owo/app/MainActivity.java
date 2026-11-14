package com.cathelperawa.owo.app;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowInsets;
import android.view.accessibility.AccessibilityManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {

    private static final int PAGE_RULES = 0;
    private static final int PAGE_MODULE = 1;
    private static final int PAGE_HOME = 2;
    private static final int PAGE_LOGS = 3;
    private static final int PAGE_SETTINGS = 4;

    private static final String[] UI_STYLES = new String[]{"深色（默认）", "深蓝"};

    private AppConfig cfg;

    private boolean loadingUi = false;

    private Switch swTotal, swBase, swWave, swPaw, swCatName, swMasterName, swSpoiled,
            swEndMeow, swEmoticon, swAutoSuffix, swKeyword, swRandom, swTrain;

    private EditText etCatName, etMasterName, etTrainPrefix, etTrainSuffix, etTrainKeyword,
            etCustom, etInput;

    private TextView tvOutput, tvServiceState, tvRuleStats, tvLogs, topbarTitle,
            tvXposedBadge, tvXposedDetail;
    private Spinner spUiStyle;

    private ScrollPage pageHome, pageModule, pageRules, pageLogs, pageSettings, pageCookie, pageMusic;

    private boolean onCookiePage = false;

    private boolean onMusicHub = false;
    private NavItem navHome, navModule, navRules, navLogs, navSettings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            String from = getIntent() == null ? null : getIntent().getStringExtra("from_host");
            if (from != null && from.length() > 0) {
                com.cathelperawa.owo.app.core.UIScope.set(from);
            }
        } catch (Throwable ignore) {
        }
        setContentView(R.layout.activity_main);
        applySystemBars();

        bindViews();
        cfg = AppConfig.load(this);

        setupStyleSpinner();
        bindNav();
        bindActions();

        try {
            if (android.os.Build.VERSION.SDK_INT >= 33
                    && checkSelfPermission("android.permission.POST_NOTIFICATIONS")
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 1001);
            }
        } catch (Throwable ignore) {
        }

        try {
            com.cathelperawa.owo.app.core.UpdateChecker.check(this);
        } catch (Throwable ignore) {
        }

        fillDeviceInfo();
        loadUiFromConfig();
        refreshRuleStats();
        refreshLogs();
        appendLog("应用启动");

        switchPage(PAGE_HOME);

        try {
            com.cathelperawa.owo.app.cookie.MusicHubUi.initSession(this);
        } catch (Throwable ignore) {
        }

        try {
            if (getIntent() != null && getIntent().getBooleanExtra("cat_auto_play", false)) {
                openMusicHub();
                final MainActivity self = this;
                getWindow().getDecorView().postDelayed(new Runnable() {
                    @Override public void run() {
                        com.cathelperawa.owo.app.cookie.MusicHubUi.autoPlay(self);
                    }
                }, 400);
                return;
            }
        } catch (Throwable ignore) {
        }

        try {
            android.content.Intent it0 = getIntent();
            boolean asMusic = it0 != null
                    && (it0.hasCategory("android.intent.category.APP_MUSIC")
                    || (android.content.Intent.ACTION_VIEW.equals(it0.getAction())
                        && it0.getType() != null && it0.getType().startsWith("audio/")));
            if (asMusic) {
                openMusicHub();
            }
        } catch (Throwable ignore) {
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        xpHandler.removeCallbacks(xpTick);
        xpHandler.post(xpTick);
        updateServiceState();

        String style = getSharedPreferences(AppConfig.PREF, MODE_PRIVATE)
                .getString("uiStyle", UI_STYLES[0]);
        applyUiStyle(style, false);
        applyCardAlphaToUi();

        if (onCookiePage) refreshCookieList();
        if (onMusicHub) refreshMusicHub();
    }

    @Override
    protected void onPause() {
        super.onPause();
        xpHandler.removeCallbacks(xpTick);
    }

    private final android.os.Handler xpHandler = new android.os.Handler(
            android.os.Looper.getMainLooper());
    private final Runnable xpTick = new Runnable() {
        @Override
        public void run() {
            try {
                refreshXposedState();
            } catch (Throwable ignore) {
            }
            xpHandler.postDelayed(this, 2000);
        }
    };

         private void applyCardAlphaToUi() {
        int pct = getSharedPreferences(AppConfig.PREF, MODE_PRIVATE).getInt("cardAlpha", 90);
        if (pct < 0) pct = 0;
        if (pct > 100) pct = 100;
        int alpha = (int) (pct / 100.0 * 255);
        applyAlphaToCards(findViewById(R.id.root_main), alpha);
    }

    private void applyAlphaToCards(View v, int alpha) {
        if (v == null) return;
        Object tag = v.getTag();
        if (tag != null && "card".equals(tag.toString())) {
            android.graphics.drawable.Drawable bg = v.getBackground();
            if (bg != null) {
                bg = bg.mutate();
                bg.setAlpha(alpha);
            }
        }
        if (v instanceof android.view.ViewGroup) {
            android.view.ViewGroup g = (android.view.ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) {
                applyAlphaToCards(g.getChildAt(i), alpha);
            }
        }
    }

    private void bindViews() {
        swTotal = findViewById(R.id.sw_total_home);
        swBase = findViewById(R.id.sw_base);
        swWave = findViewById(R.id.sw_wave);
        swPaw = findViewById(R.id.sw_paw);
        swCatName = findViewById(R.id.sw_cat_name);
        swMasterName = findViewById(R.id.sw_master_name);
        swSpoiled = findViewById(R.id.sw_spoiled);
        swEndMeow = findViewById(R.id.sw_end_meow);
        swEmoticon = findViewById(R.id.sw_emoticon);
        swAutoSuffix = findViewById(R.id.sw_auto_suffix);
        swKeyword = findViewById(R.id.sw_keyword);
        swRandom = findViewById(R.id.sw_random);
        swTrain = findViewById(R.id.sw_train);

        etCatName = findViewById(R.id.et_cat_name);
        etMasterName = findViewById(R.id.et_master_name);
        etTrainPrefix = findViewById(R.id.et_train_prefix);
        etTrainSuffix = findViewById(R.id.et_train_suffix);
        etTrainKeyword = findViewById(R.id.et_train_keyword);
        etCustom = findViewById(R.id.et_custom_reply);
        etInput = findViewById(R.id.et_input);

        tvOutput = findViewById(R.id.tv_output);
        tvServiceState = findViewById(R.id.tv_service_state);
        tvXposedBadge = findViewById(R.id.tv_xposed_badge);
        tvXposedDetail = findViewById(R.id.tv_xposed_detail);
        tvRuleStats = findViewById(R.id.tv_rule_stats);
        tvLogs = findViewById(R.id.tv_logs);
        topbarTitle = findViewById(R.id.topbar_title);
        spUiStyle = findViewById(R.id.sp_ui_style);

        pageHome = new ScrollPage(findViewById(R.id.page_home));
        pageModule = new ScrollPage(findViewById(R.id.page_module));
        pageRules = new ScrollPage(findViewById(R.id.page_rules));
        pageLogs = new ScrollPage(findViewById(R.id.page_logs));
        pageSettings = new ScrollPage(findViewById(R.id.page_settings));
        pageCookie = new ScrollPage(findViewById(R.id.page_cookie));
        pageMusic = new ScrollPage(findViewById(R.id.page_music));

        navRules = new NavItem((LinearLayout) findViewById(R.id.nav_item_rules),
                (ImageView) findViewById(R.id.nav_icon_rules),
                (TextView) findViewById(R.id.nav_text_rules));

        navModule = new NavItem((LinearLayout) findViewById(R.id.nav_item_module),
                (ImageView) findViewById(R.id.nav_icon_module),
                (TextView) findViewById(R.id.nav_text_module));

        navHome = new NavItem((LinearLayout) findViewById(R.id.nav_item_home),
                (ImageView) findViewById(R.id.nav_icon_home),
                (TextView) findViewById(R.id.nav_text_home));

        navLogs = new NavItem((LinearLayout) findViewById(R.id.nav_item_logs),
                (ImageView) findViewById(R.id.nav_icon_logs),
                (TextView) findViewById(R.id.nav_text_logs));

        navSettings = new NavItem((LinearLayout) findViewById(R.id.nav_item_settings),
                (ImageView) findViewById(R.id.nav_icon_settings),
                (TextView) findViewById(R.id.nav_text_settings));
    }

    private void setupStyleSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, UI_STYLES);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spUiStyle.setAdapter(adapter);

        String style = getSharedPreferences(AppConfig.PREF, MODE_PRIVATE)
                .getString("uiStyle", UI_STYLES[0]);

        int idx = 0;
        for (int i = 0; i < UI_STYLES.length; i++) {
            if (UI_STYLES[i].equals(style)) {
                idx = i;
                break;
            }
        }
        spUiStyle.setSelection(idx);
        applyUiStyle(style, false);
    }

    private void bindNav() {
        navRules.root.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { switchPage(PAGE_RULES); }
        });
        navModule.root.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { switchPage(PAGE_MODULE); }
        });
        navHome.root.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { switchPage(PAGE_HOME); }
        });
        navLogs.root.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { switchPage(PAGE_LOGS); }
        });
        navSettings.root.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { switchPage(PAGE_SETTINGS); }
        });
    }

    private void switchPage(int index) {
        pageRules.show(index == PAGE_RULES);
        pageModule.show(index == PAGE_MODULE);
        pageHome.show(index == PAGE_HOME);
        pageLogs.show(index == PAGE_LOGS);
        pageSettings.show(index == PAGE_SETTINGS);
        if (pageCookie != null) pageCookie.show(false);
        if (pageMusic != null) pageMusic.show(false);
        onCookiePage = false;
        onMusicHub = false;

        navRules.setActive(index == PAGE_RULES);
        navModule.setActive(index == PAGE_MODULE);
        navHome.setActive(index == PAGE_HOME);
        navLogs.setActive(index == PAGE_LOGS);
        navSettings.setActive(index == PAGE_SETTINGS);

        String[] titles = new String[]{"规则", "模块", "猫娘助手", "日志", "设置"};
        topbarTitle.setText(titles[index]);

        if (index == PAGE_RULES) refreshRuleStats();
        if (index == PAGE_LOGS) refreshLogs();
    }

    private void openCookiePage() {
        try {
            pageRules.show(false);
            pageModule.show(false);
            pageHome.show(false);
            pageLogs.show(false);
            pageSettings.show(false);
            pageCookie.show(true);

            navRules.setActive(false);
            navModule.setActive(false);
            navHome.setActive(false);
            navLogs.setActive(false);
            navSettings.setActive(false);

            topbarTitle.setText("网页登录");
            onCookiePage = true;
            refreshCookieList();
        } catch (Throwable t) {
            Toast.makeText(this, "打开网页登录页失败：" + t, Toast.LENGTH_SHORT).show();
        }
    }

    private void refreshCookieList() {
        try {
            View v = findViewById(R.id.cookie_list);
            if (v instanceof LinearLayout) {
                com.cathelperawa.owo.app.cookie.CookieAccountsUi.build(this, (LinearLayout) v);
            }
        } catch (Throwable t) {
            appendLog("网页登录列表构建失败：" + t);
        }
    }

    private void openMusicHub() {
        try {
            pageRules.show(false);
            pageModule.show(false);
            pageHome.show(false);
            pageLogs.show(false);
            pageSettings.show(false);
            pageCookie.show(false);
            pageMusic.show(true);

            navRules.setActive(false);
            navModule.setActive(false);
            navHome.setActive(false);
            navLogs.setActive(false);
            navSettings.setActive(false);

            topbarTitle.setText("聚合中心");
            onCookiePage = false;
            onMusicHub = true;
            refreshMusicHub();
        } catch (Throwable t) {
            Toast.makeText(this, "打开音乐中心失败：" + t, Toast.LENGTH_SHORT).show();
        }
    }

    private void refreshMusicHub() {
        try {
            View v = findViewById(R.id.music_hub_list);
            if (v instanceof LinearLayout) {
                com.cathelperawa.owo.app.cookie.MusicHubUi.buildEntry(this, (LinearLayout) v);
            }
        } catch (Throwable t) {
            appendLog("音乐中心构建失败：" + t);
        }
    }

    @Override
    public void onBackPressed() {
        if (onMusicHub) {

            boolean handled = false;
            try {
                handled = com.cathelperawa.owo.app.cookie.MusicHubUi.handleBack(this);
            } catch (Throwable ignore) {
            }
            if (handled) return;
            onMusicHub = false;

            switchPage(PAGE_HOME);
            return;
        }
        if (onCookiePage) {
            onCookiePage = false;
            switchPage(PAGE_SETTINGS);
            return;
        }
        super.onBackPressed();
    }

    private void bindActions() {
        Button btnSaveRules = findViewById(R.id.btn_save_rules);
        Button btnTransform = findViewById(R.id.btn_transform);
        Button btnCopy = findViewById(R.id.btn_copy);
        Button btnOpenA11y = findViewById(R.id.btn_open_a11y);
        Button btnOpenA11ySettings = findViewById(R.id.btn_open_a11y_settings);
        Button btnAllOn = findViewById(R.id.btn_all_on);
        Button btnAllOff = findViewById(R.id.btn_all_off);
        Button btnClearCustom = findViewById(R.id.btn_clear_custom);
        Button btnCopyStats = findViewById(R.id.btn_copy_stats);
        Button btnResetDefault = findViewById(R.id.btn_reset_default);
        Button btnRefreshLogs = findViewById(R.id.btn_refresh_logs);
        Button btnCopyLogs = findViewById(R.id.btn_copy_logs);
        Button btnClearLogs = findViewById(R.id.btn_clear_logs);
        Button btnApplyStyle = findViewById(R.id.btn_apply_style);
        Button btnExportCfg = findViewById(R.id.btn_export_cfg);

        swTotal.setOnCheckedChangeListener(new android.widget.CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(android.widget.CompoundButton buttonView, boolean isChecked) {
                cfg.total = isChecked;
                cfg.save(MainActivity.this);
                updateServiceState();
                appendLog("总开关 -> " + (isChecked ? "开" : "关"));
            }
        });

        bindInstantSwitch(swSpoiled, "撒娇模式");
        bindInstantSwitch(swBase, "基础猫娘化");
        bindInstantSwitch(swRandom, "随机卖萌");
        bindInstantSwitch(swEndMeow, "结尾加喵");
        bindInstantSwitch(swEmoticon, "加颜文字");
        bindInstantSwitch(swAutoSuffix, "自动加缀");
        bindInstantSwitch(swKeyword, "关键词回复");
        bindInstantSwitch(swTrain, "驯服模式");
        bindInstantSwitch(swCatName, "猫猫称呼");
        bindInstantSwitch(swMasterName, "主人称呼");
        bindInstantSwitch(swWave, "加波浪");
        bindInstantSwitch(swPaw, "加猫爪");

        btnSaveRules.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                pullUiToConfig();
                cfg.save(MainActivity.this);
                refreshRuleStats();
                appendLog("规则设置已保存");
                Toast.makeText(MainActivity.this, "规则设置已保存", Toast.LENGTH_SHORT).show();
            }
        });

        btnTransform.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                pullUiToConfig();
                String in = textOf(etInput);
                if (TextUtils.isEmpty(in)) {
                    Toast.makeText(MainActivity.this, "先输入一句话", Toast.LENGTH_SHORT).show();
                    return;
                }
                String out = CatProcessor.process(in, cfg);
                tvOutput.setText(out);
                appendLog("预览转换完成（" + in.length() + "字 -> " + out.length() + "字）");
            }
        });

        btnCopy.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                copyText("cat-output", textOf(tvOutput), "已复制结果");
            }
        });

        View.OnClickListener openA11yAction = new View.OnClickListener() {
            @Override public void onClick(View v) {
                openAccessibilitySettings();
            }
        };
        btnOpenA11y.setOnClickListener(openA11yAction);
        btnOpenA11ySettings.setOnClickListener(openA11yAction);

        View btnTerm = findViewById(R.id.btn_open_terminal);
        if (btnTerm != null) {
            btnTerm.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    com.cathelperawa.owo.app.ui.TerminalPanel.show(MainActivity.this);
                }
            });
        }

        findViewById(R.id.btn_request_overlay).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    try {
                        startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                android.net.Uri.parse("package:" + getPackageName())));
                        Toast.makeText(MainActivity.this, "请在列表中找到「猫娘助手」，打开「允许显示在其他应用上层」", Toast.LENGTH_LONG).show();
                    } catch (Exception e) {
                        try {
                            startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION));
                        } catch (Exception e2) {
                            Toast.makeText(MainActivity.this, "请手动到 设置→应用→特殊应用权限 里开启悬浮窗", Toast.LENGTH_LONG).show();
                        }
                    }
                } else {
                    Toast.makeText(MainActivity.this, "当前系统无需单独申请悬浮窗权限", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnAllOn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                cfg.enableAll();
                loadUiFromConfig();
                cfg.save(MainActivity.this);
                appendLog("全部模块已开启");
            }
        });

        btnAllOff.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                cfg.disableAll();
                loadUiFromConfig();
                cfg.save(MainActivity.this);
                appendLog("全部模块已关闭");
            }
        });

        btnClearCustom.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                etCustom.setText("");
                appendLog("自定义回复已清空（未保存）");
            }
        });

        btnCopyStats.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                copyText("rule-stats", textOf(tvRuleStats), "已复制规则统计");
            }
        });

        btnResetDefault.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                cfg = new AppConfig();
                loadUiFromConfig();
                refreshRuleStats();
                appendLog("已恢复默认配置（未保存）");
                Toast.makeText(MainActivity.this, "已恢复默认配置", Toast.LENGTH_SHORT).show();
            }
        });

        btnRefreshLogs.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                refreshLogs();
            }
        });

        btnCopyLogs.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                copyText("logs", textOf(tvLogs), "已复制日志");
            }
        });

        btnClearLogs.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                getSharedPreferences(AppConfig.PREF, MODE_PRIVATE).edit().putString("runLogs", "").apply();
                refreshLogs();
            }
        });

        btnApplyStyle.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                String style = String.valueOf(spUiStyle.getSelectedItem());
                getSharedPreferences(AppConfig.PREF, MODE_PRIVATE).edit().putString("uiStyle", style).apply();
                applyUiStyle(style, true);
                appendLog("应用界面风格: " + style);
            }
        });

        findViewById(R.id.btn_custom_style).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, CustomStyleActivity.class));
            }
        });

        findViewById(R.id.btn_cookie_login).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                openCookiePage();
            }
        });

        findViewById(R.id.btn_music_hub).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                openMusicHub();
            }
        });

        findViewById(R.id.btn_downloads).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                try {
                    android.content.Intent it = new android.content.Intent(MainActivity.this,
                            com.cathelperawa.owo.app.pan.PanActivity.class);
                    it.putExtra(com.cathelperawa.owo.app.pan.PanActivity.EXTRA_OPEN_DOWNLOADS, true);
                    startActivity(it);
                } catch (Throwable t) {
                    android.widget.Toast.makeText(MainActivity.this,
                            "打不开下载页：" + t, android.widget.Toast.LENGTH_SHORT).show();
                }
            }
        });

        findViewById(R.id.btn_github).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                com.cathelperawa.owo.app.ui.ExtLink.open(MainActivity.this,
                        com.cathelperawa.owo.app.ui.ExtLink.REPO);
            }
        });
        btnExportCfg.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                pullUiToConfig();
                copyText("cat-cfg", exportConfigText(), "已复制当前配置");
            }
        });

        final android.widget.SeekBar sbFloat = findViewById(R.id.sb_float_size);
        final TextView tvFloatVal = findViewById(R.id.tv_float_size_value);
        int startDp = Math.max(20, Math.min(110, cfg.floatSize));
        sbFloat.setMax(90);
        sbFloat.setProgress(startDp - 20);
        tvFloatVal.setText("当前：" + startDp + " dp");
        sbFloat.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(android.widget.SeekBar sb, int progress, boolean fromUser) {
                int dp = progress + 20;
                tvFloatVal.setText("当前：" + dp + " dp");
                if (fromUser) {
                    cfg.floatSize = dp;
                    cfg.save(MainActivity.this);
                }
            }
            @Override public void onStartTrackingTouch(android.widget.SeekBar sb) {}
            @Override public void onStopTrackingTouch(android.widget.SeekBar sb) {
                int dp = sb.getProgress() + 20;
                cfg.floatSize = dp;
                cfg.save(MainActivity.this);
                restartFloatIfRunning();
                appendLog("悬浮窗大小 -> " + dp + " dp");
            }
        });

        final Switch swFloat = findViewById(R.id.sw_float_window);
        swFloat.setChecked(isFloatRunning());
        swFloat.setOnCheckedChangeListener(new android.widget.CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(android.widget.CompoundButton buttonView, boolean isChecked) {
                Intent it = new Intent(MainActivity.this, FloatWindowService.class);
                if (isChecked) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(MainActivity.this)) {
                        swFloat.setChecked(false);
                        Toast.makeText(MainActivity.this, "请先授予悬浮窗权限", Toast.LENGTH_SHORT).show();
                        try {
                            startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    android.net.Uri.parse("package:" + getPackageName())));
                        } catch (Exception ignored) {}
                        return;
                    }
                    startService(it);
                    appendLog("悬浮窗已开启");
                } else {
                    stopService(it);
                    appendLog("悬浮窗已关闭");
                }
            }
        });
    }

    private void loadUiFromConfig() {
        loadingUi = true;
        swTotal.setChecked(cfg.total);
        swBase.setChecked(cfg.baseCat);
        swWave.setChecked(cfg.wave);
        swPaw.setChecked(cfg.paw);
        swCatName.setChecked(cfg.catNameSwitch);
        swMasterName.setChecked(cfg.masterNameSwitch);
        swSpoiled.setChecked(cfg.spoiled);
        swEndMeow.setChecked(cfg.endMeow);
        swEmoticon.setChecked(cfg.emoticon);
        swAutoSuffix.setChecked(cfg.autoSuffix);
        swKeyword.setChecked(cfg.keywordReply);
        swRandom.setChecked(cfg.randomCute);
        swTrain.setChecked(cfg.trainSwitch);

        etCatName.setText(cfg.catName);
        etMasterName.setText(cfg.masterName);
        etTrainPrefix.setText(cfg.trainPrefix);
        etTrainSuffix.setText(cfg.trainSuffix);
        etTrainKeyword.setText(cfg.trainKeywords);
        etCustom.setText(cfg.customReplyText);

        loadingUi = false;
        updateServiceState();
    }

    private void bindInstantSwitch(final android.widget.Switch sw, final String label) {
        if (sw == null) return;
        sw.setOnCheckedChangeListener(new android.widget.CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(android.widget.CompoundButton bv, boolean isChecked) {
                if (loadingUi) return;
                pullUiToConfig();
                cfg.save(MainActivity.this);
                appendLog(label + " -> " + (isChecked ? "开" : "关"));
            }
        });
    }

    private void pullUiToConfig() {
        cfg.total = swTotal.isChecked();
        cfg.baseCat = swBase.isChecked();
        cfg.wave = swWave.isChecked();
        cfg.paw = swPaw.isChecked();
        cfg.catNameSwitch = swCatName.isChecked();
        cfg.masterNameSwitch = swMasterName.isChecked();
        cfg.spoiled = swSpoiled.isChecked();
        cfg.endMeow = swEndMeow.isChecked();
        cfg.emoticon = swEmoticon.isChecked();
        cfg.autoSuffix = swAutoSuffix.isChecked();
        cfg.keywordReply = swKeyword.isChecked();
        cfg.randomCute = swRandom.isChecked();
        cfg.trainSwitch = swTrain.isChecked();

        cfg.catName = textOf(etCatName);
        cfg.masterName = textOf(etMasterName);
        cfg.trainPrefix = textOf(etTrainPrefix);
        cfg.trainSuffix = textOf(etTrainSuffix);
        cfg.trainKeywords = textOf(etTrainKeyword);
        cfg.customReplyText = textOf(etCustom);
    }

    private void refreshRuleStats() {
        int custom = cfg.parseCustomReplies().size();
        String s = "内置短句: " + CatLexicon.SHORT.size()
                + "\n内置整句: " + CatLexicon.REGEX.size()
                + "\n长句替换: " + CatLexicon.LONG_REPLACE.length
                + "\n颜文字: " + CatLexicon.EMOTICONS.length
                + "\n自定义回复: " + custom;
        tvRuleStats.setText(s);
    }

    private void refreshLogs() {
        StringBuilder sb = new StringBuilder();

        XposedStatus.Info info = XposedStatus.query();
        sb.append("—— 模块状态 ——\n");
        sb.append("LSPosed：" ).append(info.active ? "已激活" : "未激活").append("\n");
        if (info.active) {
            sb.append("框架版本：").append(info.framework == null || info.framework.length() == 0
                    ? "未知" : info.framework).append("\n");
            sb.append("API：").append(info.api > 0 ? info.api : "未知").append("\n");
        }
        sb.append("作用域：TIM\n\n");

        try {
            java.io.File mf = new java.io.File(getFilesDir(), "module_msg.txt");
            if (mf.exists() && mf.length() > 0) {
                sb.append("—— 消息抓取 ——\n");
                java.io.BufferedReader r = new java.io.BufferedReader(new java.io.FileReader(mf));
                StringBuilder buf = new StringBuilder();
                String line;
                while ((line = r.readLine()) != null) buf.append(line).append("\n");
                r.close();
                String[] lines = buf.toString().split("\n");
                int from = Math.max(0, lines.length - 40);
                for (int i = from; i < lines.length; i++) sb.append(lines[i]).append("\n");
                sb.append("\n");
            }
        } catch (Throwable ignore) {
        }

        try {
            java.io.File f = new java.io.File(getFilesDir(), "module_log.txt");
            if (f.exists() && f.length() > 0) {
                sb.append("—— 模块日志 ——\n");
                java.io.BufferedReader r = new java.io.BufferedReader(new java.io.FileReader(f));
                StringBuilder buf = new StringBuilder();
                String line;
                while ((line = r.readLine()) != null) buf.append(line).append("\n");
                r.close();

                String[] lines = buf.toString().split("\n");
                int from = Math.max(0, lines.length - 80);
                for (int i = from; i < lines.length; i++) sb.append(lines[i]).append("\n");
                sb.append("\n");
            }
        } catch (Throwable ignore) {
        }

        String logs = getSharedPreferences(AppConfig.PREF, MODE_PRIVATE)
                .getString("runLogs", "");
        sb.append("—— 应用日志 ——\n");
        sb.append(logs.isEmpty() ? "暂无日志" : logs);
        tvLogs.setText(sb.toString());
    }

    private void appendLog(String msg) {
        String old = getSharedPreferences(AppConfig.PREF, MODE_PRIVATE)
                .getString("runLogs", "");
        String n = now() + "  " + msg;
        String out = old.isEmpty() ? n : (n + "\n" + old);
        if (out.length() > 10000) out = out.substring(0, 10000);
        getSharedPreferences(AppConfig.PREF, MODE_PRIVATE)
                .edit().putString("runLogs", out).apply();
    }

    private String exportConfigText() {
        return "总开关=" + cfg.total + "\n"
                + "基础猫娘化=" + cfg.baseCat + "\n"
                + "撒娇模式=" + cfg.spoiled + "\n"
                + "关键词回复=" + cfg.keywordReply + "\n"
                + "驯服模式=" + cfg.trainSwitch + "\n"
                + "猫猫名=" + cfg.catName + "\n"
                + "主人名=" + cfg.masterName + "\n"
                + "驯服前缀=" + cfg.trainPrefix + "\n"
                + "驯服后缀=" + cfg.trainSuffix + "\n"
                + "驯服关键词=" + cfg.trainKeywords + "\n"
                + "自定义回复条数=" + cfg.parseCustomReplies().size();
    }

    private void openAccessibilitySettings() {
        try {
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
        } catch (Exception e) {
            Toast.makeText(this, "无法打开无障碍设置", Toast.LENGTH_SHORT).show();
        }
    }

    private void copyText(String label, String text, String toast) {
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText(label, text));
        Toast.makeText(this, toast, Toast.LENGTH_SHORT).show();
    }

    private String textOf(EditText e) {
        return e.getText() == null ? "" : e.getText().toString().trim();
    }

    private String textOf(TextView v) {
        return v.getText() == null ? "" : v.getText().toString();
    }

    private void updateServiceState() {
        boolean on = isA11yEnabled();
        TextView tvRunning = findViewById(R.id.tv_running);
        if (on) {
            tvServiceState.setText("无障碍服务：已启用");
            tvRunning.setText("运行中");
        } else {
            tvServiceState.setText("无障碍服务：未启用");
            tvRunning.setText("未启用");
        }
        refreshXposedState();
    }

    private void refreshXposedState() {
        if (tvXposedBadge == null) return;

        XposedStatus.Info info = XposedStatus.query();
        boolean active = info.active;
        String fw = info.framework;
        int api = info.api;

        if (active && (fw == null || fw.length() == 0)) {
            try {
                android.content.pm.PackageInfo pi = getPackageManager()
                        .getPackageInfo("org.lsposed.manager", 0);
                if (pi != null && pi.versionName != null) fw = "LSPosed " + pi.versionName;
            } catch (Throwable ignore) {
            }
        }

        if (active) {
            tvXposedBadge.setText("已激活");
            if (tvXposedDetail != null) {
                String versionText = (fw == null || fw.length() == 0) ? "未知" : fw;
                String apiText = api > 0 ? String.valueOf(api) : "未知";
                tvXposedDetail.setText(versionText + "　·　API：" + apiText);
                tvXposedDetail.setVisibility(View.VISIBLE);
            }
        } else {
            tvXposedBadge.setText("未激活");
            if (tvXposedDetail != null) tvXposedDetail.setVisibility(View.GONE);
        }
    }

    private void restartFloatIfRunning() {
        if (isFloatRunning()) {
            Intent it = new Intent(this, FloatWindowService.class);
            stopService(it);
            startService(it);
        }
    }

    private boolean isFloatRunning() {        android.app.ActivityManager am = (android.app.ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        if (am == null) return false;
        for (android.app.ActivityManager.RunningServiceInfo info : am.getRunningServices(64)) {
            if (FloatWindowService.class.getName().equals(info.service.getClassName())) return true;
        }
        return false;
    }

    private boolean isA11yEnabled() {
        try {
            AccessibilityManager am = (AccessibilityManager) getSystemService(Context.ACCESSIBILITY_SERVICE);
            for (AccessibilityServiceInfo info : am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)) {
                if (info.getResolveInfo() != null && info.getResolveInfo().serviceInfo != null) {
                    String pkg = info.getResolveInfo().serviceInfo.packageName;
                    if (getPackageName().equals(pkg)) return true;
                }
            }
        } catch (Throwable ignored) {}
        return false;
    }

    private void applySystemBars() {
        final View root = findViewById(R.id.root_main);
        final View topbar = findViewById(R.id.topbar);
        final View navBar = findViewById(R.id.nav_bar);
        if (root == null) return;

        final int topBase = topbar == null ? 0 : topbar.getPaddingTop();
        final int navBase = navBar == null ? 0 : navBar.getPaddingBottom();

        root.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            @Override public WindowInsets onApplyWindowInsets(View v, WindowInsets insets) {
                int top = 0, bottom = 0;
                try {
                    if (Build.VERSION.SDK_INT >= 30) {
                        top = insets.getInsets(WindowInsets.Type.statusBars()).top;
                        bottom = insets.getInsets(WindowInsets.Type.navigationBars()).bottom;
                    } else {
                        top = insets.getSystemWindowInsetTop();
                        bottom = insets.getSystemWindowInsetBottom();
                    }
                } catch (Throwable ignore) {
                }
                if (topbar != null) {
                    topbar.setPadding(topbar.getPaddingLeft(), topBase + top,
                            topbar.getPaddingRight(), topbar.getPaddingBottom());
                }
                if (navBar != null && bottom > 0) {
                    android.view.ViewGroup.LayoutParams lp = navBar.getLayoutParams();
                    if (lp != null && lp.height >= 0 && lp.height < 68 * 3) {
                        lp.height = lp.height + bottom;
                        navBar.setLayoutParams(lp);
                    }
                    navBar.setPadding(navBar.getPaddingLeft(), navBar.getPaddingTop(),
                            navBar.getPaddingRight(), navBase + bottom);
                }
                return insets;
            }
        });
        root.requestApplyInsets();
    }

    private void applyUiStyle(String style, boolean toast) {
        View root = findViewById(R.id.root_main);
        View topbar = findViewById(R.id.topbar);

        String bgPath = getSharedPreferences(AppConfig.PREF, MODE_PRIVATE).getString("customBg", "");
        if (bgPath != null && !bgPath.isEmpty()) {
            android.graphics.drawable.Drawable d = android.graphics.drawable.Drawable.createFromPath(bgPath);
            if (d != null) {
                int alphaPct = getSharedPreferences(AppConfig.PREF, MODE_PRIVATE).getInt("bgAlpha", 100);
                if (alphaPct < 0) alphaPct = 0;
                if (alphaPct > 100) alphaPct = 100;
                if (alphaPct >= 100) {
                    root.setBackground(d);
                } else {

                    int overlayAlpha = (int) ((100 - alphaPct) / 100.0 * 255);
                    android.graphics.drawable.Drawable[] layers = new android.graphics.drawable.Drawable[]{
                            d,
                            new android.graphics.drawable.ColorDrawable(
                                    Color.argb(overlayAlpha, 15, 19, 26))
                    };
                    root.setBackground(new android.graphics.drawable.LayerDrawable(layers));
                }
                if ("深蓝".equals(style)) {
                    topbar.setBackgroundColor(Color.parseColor("#8011233C"));
                } else {
                    topbar.setBackgroundColor(Color.parseColor("#800F131A"));
                }
                topbarTitle.setTextColor(Color.parseColor("#EAF1FB"));
                if (toast) Toast.makeText(this, "已应用自定义背景", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        if ("深蓝".equals(style)) {
            root.setBackgroundColor(Color.parseColor("#0C1625"));
            topbar.setBackgroundColor(Color.parseColor("#11233C"));
            topbarTitle.setTextColor(Color.parseColor("#EAF1FB"));
        } else {
            root.setBackgroundColor(Color.parseColor("#0F131A"));
            topbar.setBackgroundColor(Color.parseColor("#0F131A"));
            topbarTitle.setTextColor(Color.parseColor("#EAF1FB"));
        }
        if (toast) Toast.makeText(this, "界面风格已应用：" + style, Toast.LENGTH_SHORT).show();
    }

    private void fillDeviceInfo() {
        set(R.id.tv_fingerprint, safe(Build.FINGERPRINT));

        String maker = buildString(
                cap(Build.MANUFACTURER),
                Build.BRAND != null && !Build.BRAND.equalsIgnoreCase(Build.MANUFACTURER)
                        ? cap(Build.BRAND) : "",
                Build.MODEL
        );
        set(R.id.tv_model, maker.isEmpty() ? "unknown" : maker);

        set(R.id.tv_kernel, kernelVersion());

        String ver = (Build.VERSION.PREVIEW_SDK_INT != 0)
                ? Build.VERSION.CODENAME + " Preview (API " + Build.VERSION.SDK_INT + ")"
                : "Android " + Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")";
        set(R.id.tv_android, ver);

        String abi = (Build.SUPPORTED_ABIS != null && Build.SUPPORTED_ABIS.length > 0)
                ? Build.SUPPORTED_ABIS[0] : "unknown";
        set(R.id.tv_abi, abi);
    }

    private String kernelVersion() {
        try {
            File f = new File("/proc/version");
            if (f.canRead()) {
                String txt = readAll(f);
                java.util.regex.Matcher m = java.util.regex.Pattern
                        .compile("Linux version (\\S+)")
                        .matcher(txt);
                if (m.find()) return m.group(1);
            }
        } catch (Exception ignored) {}
        String osv = System.getProperty("os.version");
        return (osv != null && !osv.isEmpty()) ? osv : "unknown";
    }

    private String readAll(File f) throws Exception {
        StringBuilder sb = new StringBuilder();
        BufferedReader br = new BufferedReader(new FileReader(f));
        try {
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append('\n');
        } finally {
            br.close();
        }
        return sb.toString();
    }

    private String now() {
        return new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
    }

    private String cap(String s) {
        if (s == null || s.isEmpty()) return "";
        return s.substring(0, 1).toUpperCase(Locale.getDefault()) + s.substring(1);
    }

    private String buildString(String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p == null || p.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(p);
        }
        return sb.toString().trim();
    }

    private String safe(String s) {
        return (s == null || s.trim().isEmpty()) ? "unknown" : s.trim();
    }

    private void set(int id, String value) {
        TextView tv = findViewById(id);
        if (tv != null) tv.setText(value);
    }

    private static class ScrollPage {
        final View root;
        ScrollPage(View v) { this.root = v; }
        void show(boolean visible) { root.setVisibility(visible ? View.VISIBLE : View.GONE); }
    }

    private static class NavItem {
        final LinearLayout root;
        final ImageView icon;
        final TextView text;

        NavItem(LinearLayout root, ImageView icon, TextView text) {
            this.root = root;
            this.icon = icon;
            this.text = text;
        }

        void setActive(boolean active) {
            if (active) {
                root.setBackgroundResource(R.drawable.bg_nav_selected);
                icon.setColorFilter(Color.parseColor("#E7F0FF"));
                text.setTextColor(Color.parseColor("#E7F0FF"));
                text.setTypeface(null, Typeface.BOLD);
            } else {
                root.setBackgroundColor(Color.TRANSPARENT);
                icon.setColorFilter(Color.parseColor("#8395AD"));
                text.setTextColor(Color.parseColor("#8395AD"));
                text.setTypeface(null, Typeface.NORMAL);
            }
        }
    }
}
