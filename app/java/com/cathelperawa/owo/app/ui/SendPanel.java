package com.cathelperawa.owo.app.ui;

public final class SendPanel {
    private static final java.lang.String BILI_TAG_ICON = "https://open.gtimg.cn/open/app_icon/00/95/17/76/100951776_100_m.png";
    private static final int C_PINK = -1017170;
    private static final int C_SUB = -7693131;
    private static final int C_TITLE = -12824981;
    public static final java.lang.String KEY_ARK_SIGN = "send_ark_sign";
    public static final java.lang.String KEY_FORCE_LONG = "send_force_long";
    public static final java.lang.String KEY_LAST_CARD = "send_last_card";
    public static final java.lang.String KEY_LAST_CMD = "send_last_cmd";
    public static final java.lang.String KEY_LAST_PB = "send_last_pb";
    public static final java.lang.String KEY_MUSIC_SIGN = "send_music_sign";
    public static final java.lang.String KEY_PANEL_MODE = "send_panel_mode";
    public static final java.lang.String KEY_SIGN_CH = "send_sign_channel";
    public static final java.lang.String KEY_SIGN_ON = "send_sign_on";
    static final java.lang.String K_WX_LIST = "wxmini_list";
    static final java.lang.String K_WX_ON = "wxmini_enable";
    static final java.lang.String K_WX_QAPPID = "wxmini_qappid";
    private static android.app.Dialog sCurrent;

    static java.lang.Runnable sMiniRefresh = null;

    static android.app.Dialog sMiniDlg = null;
    private static volatile java.lang.String PENDING = null;
    private static volatile java.lang.String PENDING_CMD = null;
    private static java.lang.String lastCode = "";
    private static java.lang.String lastCard = "";
    private static java.lang.String lastPb = "";
    private static java.lang.String lastCmd = "";
    private static boolean lastLoaded = false;
    private static final com.cathelperawa.owo.app.proto.PbMsg.UidResolver RESOLVER = new com.cathelperawa.owo.app.proto.PbMsg.UidResolver() {
        @Override
        public java.lang.String uidFromUin(java.lang.String str) {
            try {
                return com.cathelperawa.owo.app.bridge.QQUtils.uidFromUin(str);
            } catch (java.lang.Throwable th) {
                return null;
            }
        }
    };

    private SendPanel() {
    }

    public static void setPending(java.lang.String str) {
        PENDING = str;
        PENDING_CMD = null;
    }

    public static void setPendingPb(java.lang.String str, java.lang.String str2) {
        PENDING = str;
        PENDING_CMD = str2;
    }

    public static void closeCurrent() {
        try {
            android.app.Dialog dialog = sCurrent;
            if (dialog != null && dialog.isShowing()) {
                sCurrent.dismiss();
            }
        } catch (java.lang.Throwable th) {
        }
        sCurrent = null;
    }

    public static void show(android.content.Context context) {
        final android.app.Activity activityOf = activityOf(context);
        if (activityOf == null) {
            com.cathelperawa.owo.app.core.HookLog.w("SendPanel: 没有 Activity");
        } else {
            activityOf.runOnUiThread(new java.lang.Runnable() {
                @Override
                public void run() {
                    try {
                        if (!com.cathelperawa.owo.app.bridge.CurrentChat.fromActivity(activityOf)) {
                            com.cathelperawa.owo.app.bridge.SessionProbe.probe(activityOf, com.cathelperawa.owo.app.ui.SendPanel.findInput(activityOf));
                        }
                        com.cathelperawa.owo.app.ui.SendPanel.closeCurrent();
                        android.app.Dialog build = com.cathelperawa.owo.app.ui.SendPanel.build(activityOf);
                        android.app.Dialog unused = com.cathelperawa.owo.app.ui.SendPanel.sCurrent = build;
                        build.show();
                    } catch (java.lang.Throwable th) {
                        com.cathelperawa.owo.app.core.HookLog.e("SendPanel 构建失败", th);
                    }
                }
            });
        }
    }

    public static boolean forceLong(android.content.Context context) {
        try {
            return com.cathelperawa.owo.app.core.HookConfig.getBoolean(context, "send_force_long", false);
        } catch (java.lang.Throwable th) {
            return false;
        }
    }

    public static boolean signOn(android.content.Context context) {
        try {
            java.lang.String str = com.cathelperawa.owo.app.core.HookConfig.get(context, KEY_SIGN_ON, null);
            if (str == null) {
                return com.cathelperawa.owo.app.core.HookConfig.getBoolean(context, KEY_ARK_SIGN, true);
            }
            return "true".equalsIgnoreCase(str) || "1".equals(str);
        } catch (java.lang.Throwable th) {
            return true;
        }
    }

    public static void setSignOn(android.content.Context context, boolean z) {
        try {
            com.cathelperawa.owo.app.core.HookConfig.set(context, KEY_SIGN_ON, z ? "1" : com.cathelperawa.owo.app.bridge.GhostChat.GHOST_UID);
        } catch (java.lang.Throwable th) {
        }
    }

    public static java.lang.String signChannel(android.content.Context context) {
        try {
            java.lang.String str = com.cathelperawa.owo.app.core.HookConfig.get(context, KEY_SIGN_CH, null);
            if (str != null && str.trim().length() != 0) {
                return str.trim();
            }
            return com.cathelperawa.owo.app.feature.ArkSigner.CH_AUTO;
        } catch (java.lang.Throwable th) {
            return com.cathelperawa.owo.app.feature.ArkSigner.CH_AUTO;
        }
    }

    public static void setSignChannel(android.content.Context context, java.lang.String str) {
        java.lang.String v = (str == null || str.trim().length() == 0)
                ? com.cathelperawa.owo.app.feature.ArkSigner.CH_AUTO
                : str.trim();
        try {
            com.cathelperawa.owo.app.core.HookConfig.set(context, KEY_SIGN_CH, v);
        } catch (java.lang.Throwable ignore) {
        }
    }

    public static boolean arkSignOn(android.content.Context context) {
        return signOn(context);
    }

    public static boolean musicSignOn(android.content.Context context) {
        return signOn(context);
    }

    private static void loadLast(android.content.Context context) {
        if (lastLoaded) {
            return;
        }
        lastLoaded = true;
        try {
            java.lang.String str = com.cathelperawa.owo.app.core.HookConfig.get(context, KEY_LAST_CARD, null);
            java.lang.String str2 = com.cathelperawa.owo.app.core.HookConfig.get(context, KEY_LAST_PB, null);
            java.lang.String str3 = com.cathelperawa.owo.app.core.HookConfig.get(context, KEY_LAST_CMD, null);
            if (str != null) {
                lastCard = str;
            }
            if (str2 != null) {
                lastPb = str2;
            }
            if (str3 != null) {
                lastCmd = str3;
            }
        } catch (java.lang.Throwable th) {
        }
    }

    public static void saveLast(android.content.Context context, int i, java.lang.String str, java.lang.String str2) {
        try {
            if (i == 0) {
                if (str == null) {
                    str = "";
                }
                lastCard = str;
            } else {
                if (str == null) {
                    str = "";
                }
                lastPb = str;
            }
            if (str2 == null) {
                str2 = "";
            }
            lastCmd = str2;
            com.cathelperawa.owo.app.core.HookConfig.set(context, KEY_LAST_CARD, lastCard);
            com.cathelperawa.owo.app.core.HookConfig.set(context, KEY_LAST_PB, lastPb);
            com.cathelperawa.owo.app.core.HookConfig.set(context, KEY_LAST_CMD, lastCmd);
        } catch (java.lang.Throwable th) {
        }
    }

    public static int modeOf(android.content.Context context) {
        try {
            return "1".equals(com.cathelperawa.owo.app.core.HookConfig.get(context, KEY_PANEL_MODE, com.cathelperawa.owo.app.bridge.GhostChat.GHOST_UID)) ? 1 : 0;
        } catch (java.lang.Throwable th) {
            return 0;
        }
    }

    public static void saveMode(android.content.Context context, int i) {
        try {
            com.cathelperawa.owo.app.core.HookConfig.set(context, KEY_PANEL_MODE, java.lang.String.valueOf(i));
        } catch (java.lang.Throwable th) {
        }
    }

    private static android.widget.TextView capsule(android.content.Context context, java.lang.String str, boolean z) {
        float f = context.getResources().getDisplayMetrics().density;
        android.widget.TextView tv = tv(context, str, 12, -9530410, true);
        tv.setGravity(17);
        int i = (int) (f * 9.0f);
        tv.setPadding(0, i, 0, i);
        paintCapsule(context, tv, z);
        return tv;
    }

    public static void paintCapsule(android.content.Context context, android.widget.TextView textView, boolean z) {
        float f = context.getResources().getDisplayMetrics().density;
        android.graphics.drawable.GradientDrawable gradientDrawable = new android.graphics.drawable.GradientDrawable();
        gradientDrawable.setCornerRadius(14.0f * f);
        if (z) {
            gradientDrawable.setColor(-855653665);
            gradientDrawable.setStroke((int) f, -24629);
            textView.setTextColor(-1548900);
        } else {
            gradientDrawable.setColor(2063597567);
            gradientDrawable.setStroke((int) f, 1439291112);
            textView.setTextColor(C_SUB);
        }
        textView.setBackgroundDrawable(gradientDrawable);
    }

    public static android.widget.EditText findInput(android.app.Activity activity) {
        try {
            int identifier = activity.getResources().getIdentifier("input", "id", activity.getPackageName());
            if (identifier != 0) {
                android.view.View findViewById = activity.findViewById(identifier);
                if (findViewById instanceof android.widget.EditText) {
                    return (android.widget.EditText) findViewById;
                }
                return null;
            }
            return null;
        } catch (java.lang.Throwable th) {
            return null;
        }
    }

    private static android.app.Activity activityOf(android.content.Context context) {
        if (context instanceof android.app.Activity) {
            return (android.app.Activity) context;
        }
        try {
            android.app.Activity activity = com.cathelperawa.owo.app.bridge.QQEnv.topActivity();
            if (activity != null) {
                return activity;
            }
            return null;
        } catch (java.lang.Throwable th) {
            return null;
        }
    }

    public static void switchMode(android.app.Activity activity, int[] iArr, int i, android.widget.EditText editText, android.widget.EditText editText2, java.lang.Runnable runnable) {
        if (iArr[0] == i) {
            return;
        }
        java.lang.String obj = editText.getText() == null ? "" : editText.getText().toString();
        saveLast(activity, iArr[0], obj, editText2.getText() == null ? "" : editText2.getText().toString());
        iArr[0] = i;
        saveMode(activity, i);
        java.lang.String str = i == 0 ? lastCard : lastPb;
        java.lang.String str2 = str != null ? str : "";
        if (!str2.equals(obj)) {
            editText.setText(str2);
        }
        runnable.run();
    }

    public static android.app.Dialog build(final android.app.Activity activity) {
        java.lang.String str;
        java.lang.String str2;
        java.lang.String str3;
        java.lang.String str4;
        char c;
        final android.app.Dialog dialog2 = new android.app.Dialog(activity, android.R.style.Theme_Translucent_NoTitleBar);
        final android.app.Dialog dialog = dialog2;
        final float f = activity.getResources().getDisplayMetrics().density;
        int i = activity.getResources().getDisplayMetrics().widthPixels;
        final int i2 = activity.getResources().getDisplayMetrics().heightPixels;
        final android.widget.FrameLayout frameLayout = new android.widget.FrameLayout(activity);
        frameLayout.setBackgroundColor(0);
        android.widget.LinearLayout linearLayout = new android.widget.LinearLayout(activity);
        linearLayout.setOrientation(1);
        float f2 = f * 18.0f;
        int i3 = (int) f2;
        float f3 = 16.0f * f;
        float f4 = f * 14.0f;
        int i4 = (int) f4;
        linearLayout.setPadding(i3, (int) f3, i3, i4);
        android.graphics.drawable.GradientDrawable gradientDrawable = new android.graphics.drawable.GradientDrawable();
        gradientDrawable.setColor(-855638017);
        float f5 = f * 26.0f;
        gradientDrawable.setCornerRadius(f5);
        int i5 = (int) f;
        gradientDrawable.setStroke(i5, -1493172225);
        linearLayout.setBackgroundDrawable(gradientDrawable);
        float f6 = f * 12.0f;
        linearLayout.setElevation(f6);
        final android.widget.ScrollView scrollView = new android.widget.ScrollView(activity);
        scrollView.setVerticalScrollBarEnabled(false);
        scrollView.setClipToOutline(true);
        scrollView.setFillViewport(true);
        float f7 = i2;
        android.widget.FrameLayout.LayoutParams layoutParams = new android.widget.FrameLayout.LayoutParams(-1, (int) (f7 * 0.9f), 80);
        int i6 = (int) (i * 0.02f);
        layoutParams.leftMargin = i6;
        layoutParams.rightMargin = i6;
        int i7 = (int) f6;
        layoutParams.bottomMargin = i7;
        scrollView.addView(linearLayout, new android.widget.FrameLayout.LayoutParams(-1, -2));
        frameLayout.addView(scrollView, layoutParams);
        try {
            frameLayout.getViewTreeObserver().addOnGlobalLayoutListener(new android.view.ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    try {
                        int height = frameLayout.getHeight();
                        if (height <= 0) {
                            return;
                        }
                        int min = java.lang.Math.min((int) (i2 * 0.9f), height);
                        android.widget.FrameLayout.LayoutParams layoutParams2 = (android.widget.FrameLayout.LayoutParams) scrollView.getLayoutParams();
                        if (layoutParams2 != null && layoutParams2.height != min) {
                            layoutParams2.height = min;
                            scrollView.setLayoutParams(layoutParams2);
                        }
                    } catch (java.lang.Throwable th) {
                    }
                }
            });
        } catch (java.lang.Throwable th) {
        }
        android.widget.LinearLayout linearLayout2 = new android.widget.LinearLayout(activity);
        linearLayout2.setOrientation(0);
        linearLayout2.setGravity(16);
        android.widget.LinearLayout linearLayout3 = new android.widget.LinearLayout(activity);
        linearLayout3.setOrientation(1);
        linearLayout3.addView(tv(activity, "发送与协议", 19, C_TITLE, true));
        linearLayout3.addView(tv(activity, "目标 = 当前会话 · 通道自动判定", 11, C_SUB, false));
        linearLayout2.addView(linearLayout3, new android.widget.LinearLayout.LayoutParams(0, -2, 1.0f));
        android.widget.TextView tv = tv(activity, "✕", 15, -6640961, true);
        int i8 = (int) (10.0f * f);
        tv.setPadding(i8, 0, (int) (2.0f * f), 0);
        tv.setOnClickListener(new android.view.View.OnClickListener() {
            @Override
            public void onClick(android.view.View view) {
                dialog2.dismiss();
            }
        });
        linearLayout2.addView(tv);
        android.widget.LinearLayout.LayoutParams layoutParams2 = new android.widget.LinearLayout.LayoutParams(-1, -2);
        layoutParams2.bottomMargin = i4;
        linearLayout.addView(linearLayout2, layoutParams2);
        final int[] iArr = {modeOf(activity)};
        android.widget.LinearLayout linearLayout4 = new android.widget.LinearLayout(activity);
        linearLayout4.setOrientation(0);
        final android.widget.TextView capsule = capsule(activity, "发送卡片", iArr[0] == 0);
        android.widget.TextView capsule2 = capsule(activity, "PB 工具", iArr[0] == 1);
        android.widget.ScrollView scrollView2 = scrollView;
        android.widget.ScrollView scrollView4 = scrollView2;
        android.widget.FrameLayout frameLayout4 = frameLayout;
        int i10 = (int) (f * 3.0f);
        android.widget.TextView[] tabCaps = {capsule, capsule2};
        for (int ti = 0; ti < tabCaps.length; ti++) {
            android.widget.LinearLayout.LayoutParams layoutParams3 =
                    new android.widget.LinearLayout.LayoutParams(0, -2, 1.0f);
            layoutParams3.leftMargin = i10;
            layoutParams3.rightMargin = i10;
            linearLayout4.addView(tabCaps[ti], layoutParams3);
        }
        final android.widget.TextView textView2 = capsule2;
        android.widget.LinearLayout.LayoutParams layoutParams4 = new android.widget.LinearLayout.LayoutParams(-1, -2);
        layoutParams4.bottomMargin = i7;
        linearLayout.addView(linearLayout4, layoutParams4);
        final android.widget.EditText editText = new android.widget.EditText(activity);
        editText.setHint("元素 JSON / 卡片 JSON / 预设条目");
        editText.setTextSize(13.0f);
        editText.setTextColor(C_TITLE);
        editText.setHintTextColor(-4996910);
        editText.setTypeface(android.graphics.Typeface.MONOSPACE);
        editText.setGravity(51);
        editText.setInputType(131073);
        editText.setPadding(i4, i7, i4, i7);
        android.graphics.drawable.GradientDrawable gradientDrawable2 = new android.graphics.drawable.GradientDrawable();
        gradientDrawable2.setColor(-1275068417);
        gradientDrawable2.setCornerRadius(f3);
        gradientDrawable2.setStroke(i5, 1724503784);
        editText.setBackgroundDrawable(gradientDrawable2);
        com.cathelperawa.owo.app.ui.UiKit.innerScroll(editText);
        android.widget.LinearLayout.LayoutParams layoutParams5 = new android.widget.LinearLayout.LayoutParams(-1, (int) (f7 * 0.33f));
        layoutParams5.bottomMargin = i7;
        linearLayout.addView(editText, layoutParams5);
        editText.setOnFocusChangeListener(new android.view.View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(android.view.View view, boolean z) {
                if (z) {
                    editText.postDelayed(new java.lang.Runnable() {
                        @Override
                        public void run() {
                            try {
                                editText.requestRectangleOnScreen(new android.graphics.Rect(0, 0, editText.getWidth(), editText.getHeight() + ((int) (f * 80.0f))), true);
                            } catch (java.lang.Throwable th2) {
                            }
                        }
                    }, 350L);
                }
            }
        });
        loadLast(activity);
        java.lang.String str5 = iArr[0] == 0 ? lastCard : lastPb;
        if (str5 != null && str5.length() > 0 && editText.getText().length() == 0) {
            editText.setText(str5);
        }
        if (str5 == null) {
            str5 = "";
        }
        lastCode = str5;
        editText.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(java.lang.CharSequence charSequence, int i11, int i12, int i13) {
            }

            @Override
            public void onTextChanged(java.lang.CharSequence charSequence, int i11, int i12, int i13) {
            }

            @Override
            public void afterTextChanged(android.text.Editable editable) {
                java.lang.String unused = com.cathelperawa.owo.app.ui.SendPanel.lastCode = editable == null ? "" : editable.toString();
            }
        });
        final android.widget.EditText editText2 = new android.widget.EditText(activity);
        editText2.setHint("cmd，例如 MessageSvc.PbSendMsg");
        editText2.setTextSize(12.0f);
        editText2.setTextColor(C_TITLE);
        editText2.setHintTextColor(-4996910);
        editText2.setTypeface(android.graphics.Typeface.MONOSPACE);
        editText2.setSingleLine(true);
        int i11 = (int) (11.0f * f);
        editText2.setPadding(i4, i11, i4, i11);
        android.graphics.drawable.GradientDrawable gradientDrawable3 = new android.graphics.drawable.GradientDrawable();
        gradientDrawable3.setColor(-1275068417);
        gradientDrawable3.setCornerRadius(f4);
        gradientDrawable3.setStroke(i5, 1724503784);
        editText2.setBackgroundDrawable(gradientDrawable3);
        android.widget.LinearLayout.LayoutParams layoutParams6 = new android.widget.LinearLayout.LayoutParams(-1, -2);
        layoutParams6.bottomMargin = i7;
        linearLayout.addView(editText2, layoutParams6);
        java.lang.String str6 = lastCmd;
        if (str6 != null && str6.length() > 0) {
            editText2.setText(lastCmd);
        }
        editText2.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(java.lang.CharSequence charSequence, int i12, int i13, int i14) {
            }

            @Override
            public void onTextChanged(java.lang.CharSequence charSequence, int i12, int i13, int i14) {
            }

            @Override
            public void afterTextChanged(android.text.Editable editable) {
                java.lang.String unused = com.cathelperawa.owo.app.ui.SendPanel.lastCmd = editable == null ? "" : editable.toString();
            }
        });
        editText2.setVisibility(iArr[0] == 1 ? 0 : 8);
        if (PENDING != null) {
            java.lang.String str7 = PENDING;
            java.lang.String str8 = PENDING_CMD;
            PENDING = null;
            PENDING_CMD = null;
            if (str7.length() > 0) {
                editText.setText(str7);
                if (str8 == null || str8.length() <= 0) {
                    c = 0;
                    iArr[0] = com.cathelperawa.owo.app.core.JsonPretty.looksLikePb(str7) ? 1 : 0;
                } else {
                    editText2.setText(str8);
                    iArr[0] = 1;
                    c = 0;
                }
                saveMode(activity, iArr[c]);
            }
        }
        linearLayout.addView(groupLabel(activity, "输入 · 对输入框里的 JSON 下手"), groupLp(f));
        final android.widget.LinearLayout linearLayout5 = new android.widget.LinearLayout(activity);
        linearLayout5.setOrientation(0);
        android.widget.TextView ghost = ghost(activity, "粘贴");
        ghost.setOnClickListener(new android.view.View.OnClickListener() {
            @Override
            public void onClick(android.view.View view) {
                android.content.ClipData primaryClip;
                java.lang.CharSequence coerceToText;
                try {
                    android.content.ClipboardManager clipboardManager = (android.content.ClipboardManager) activity.getSystemService("clipboard");
                    if (clipboardManager == null || !clipboardManager.hasPrimaryClip() || (primaryClip = clipboardManager.getPrimaryClip()) == null || primaryClip.getItemCount() == 0 || (coerceToText = primaryClip.getItemAt(0).coerceToText(activity)) == null) {
                        return;
                    }
                    editText.setText(coerceToText.toString().trim());
                } catch (java.lang.Throwable th2) {
                    com.cathelperawa.owo.app.core.HookLog.w("SendPanel 粘贴失败: " + th2);
                }
            }
        });
        android.widget.TextView ghost2 = ghost(activity, "整理");
        ghost2.setOnClickListener(new android.view.View.OnClickListener() {
            @Override
            public void onClick(android.view.View view) {
                com.cathelperawa.owo.app.core.JsonPretty.Tidied tidy = com.cathelperawa.owo.app.core.JsonPretty.tidy(editText.getText() == null ? "" : editText.getText().toString().trim(), iArr[0] == 1);
                if (tidy.text == null) {
                    com.cathelperawa.owo.app.bridge.CatToast.show(tidy.note);
                } else if (!tidy.ok) {
                    com.cathelperawa.owo.app.bridge.CatToast.show(tidy.note);
                } else {
                    editText.setText(tidy.text);
                    com.cathelperawa.owo.app.bridge.CatToast.show(tidy.note);
                }
            }
        });
        android.widget.TextView ghost3 = ghost(activity, "清空");
        ghost3.setOnClickListener(new android.view.View.OnClickListener() {
            @Override
            public void onClick(android.view.View view) {
                editText.setText("");
            }
        });
        android.widget.TextView ghost4 = ghost(activity, "模板");
        final android.widget.TextView ghost5 = ghost(activity, "外框");
        ghost5.setOnLongClickListener(new android.view.View.OnLongClickListener() {
            @Override
            public boolean onLongClick(android.view.View view) {
                com.cathelperawa.owo.app.proto.MultiMsgProbe.run(activity);
                return true;
            }
        });
        ghost5.setOnClickListener(new android.view.View.OnClickListener() {
            @Override
            public void onClick(android.view.View view) {
                final java.lang.String trim;
                if (iArr[0] == 1) {
                    trim = editText.getText() != null ? editText.getText().toString().trim() : "";
                    if (trim.length() == 0) {
                        com.cathelperawa.owo.app.bridge.CatToast.show("先贴一段 0xdc2 报文喵");
                        return;
                    }
                    java.lang.String fakeArk = com.cathelperawa.owo.app.feature.OidbCard.toFakeArk(trim);
                    if (fakeArk == null) {
                        com.cathelperawa.owo.app.bridge.CatToast.show("这段报文里没有 4.1.12 那层，改不了外框");
                        return;
                    } else {
                        com.cathelperawa.owo.app.ui.ArkSkinSheet.open(activity, fakeArk, new com.cathelperawa.owo.app.ui.ArkSkinSheet.OnDone() {
                            @Override
                            public void run(java.lang.String str9) {
                                java.lang.String rebuild = com.cathelperawa.owo.app.feature.OidbCard.rebuild(str9, trim);
                                if (rebuild == null) {
                                    com.cathelperawa.owo.app.bridge.CatToast.show("重组报文失败喵");
                                } else {
                                    editText.setText(rebuild);
                                    com.cathelperawa.owo.app.bridge.CatToast.show("外框改好了，点「发送报文」");
                                }
                            }
                        });
                        return;
                    }
                }
                trim = editText.getText() != null ? editText.getText().toString().trim() : "";
                if (trim.length() != 0) {
                    java.lang.String arkFromElements = com.cathelperawa.owo.app.ui.SendPanel.arkFromElements(trim);
                    final boolean z = arkFromElements != null;
                    java.lang.String trimArk = trim;
                    if (arkFromElements != null) {
                        trimArk = arkFromElements;
                    } else if (!com.cathelperawa.owo.app.proto.PbSender.isArk(trimArk)) {
                        com.cathelperawa.owo.app.bridge.CatToast.show("这不是卡片 JSON（要有 app + view）");
                        return;
                    }
                    com.cathelperawa.owo.app.ui.ArkSkinSheet.open(activity, trimArk, new com.cathelperawa.owo.app.ui.ArkSkinSheet.OnDone() {
                        @Override
                        public void run(java.lang.String str9) {
                            android.widget.EditText editText3 = editText;
                            if (z) {
                                str9 = com.cathelperawa.owo.app.ui.SendPanel.wrapArk(str9);
                            }
                            editText3.setText(str9);
                            com.cathelperawa.owo.app.bridge.CatToast.show("外框改好了，点发送");
                        }
                    });
                    return;
                }
                com.cathelperawa.owo.app.bridge.CatToast.show("先贴一个卡片 JSON 喵");
            }
        });
        android.widget.LinearLayout.LayoutParams layoutParams7 = new android.widget.LinearLayout.LayoutParams(0, -2, 1.0f);
        int i12 = (int) (f * 4.0f);
        layoutParams7.rightMargin = i12;
        android.widget.LinearLayout.LayoutParams layoutParams8 = new android.widget.LinearLayout.LayoutParams(0, -2, 1.0f);
        layoutParams8.leftMargin = i12;
        layoutParams8.rightMargin = i12;
        android.widget.LinearLayout.LayoutParams layoutParams9 = new android.widget.LinearLayout.LayoutParams(0, -2, 1.0f);
        layoutParams9.leftMargin = i12;
        layoutParams9.rightMargin = i12;
        android.widget.LinearLayout.LayoutParams layoutParams10 = new android.widget.LinearLayout.LayoutParams(0, -2, 1.0f);
        layoutParams10.leftMargin = i12;
        layoutParams10.rightMargin = i12;
        android.widget.LinearLayout.LayoutParams layoutParams11 = new android.widget.LinearLayout.LayoutParams(0, -2, 1.0f);
        layoutParams11.leftMargin = i12;
        linearLayout5.addView(ghost, layoutParams7);
        linearLayout5.addView(ghost2, layoutParams8);
        linearLayout5.addView(ghost3, layoutParams9);
        linearLayout5.addView(ghost4, layoutParams10);
        linearLayout5.addView(ghost5, layoutParams11);
        final android.widget.TextView ghost6 = ghost(activity, "存模板");
        ghost6.setVisibility(8);
        ghost6.setOnClickListener(new android.view.View.OnClickListener() {
            @Override
            public void onClick(android.view.View view) {
                com.cathelperawa.owo.app.ui.SendPanel.saveAsTemplate(activity, editText, iArr, editText2);
            }
        });
        android.widget.LinearLayout.LayoutParams layoutParams12 = new android.widget.LinearLayout.LayoutParams(0, -2, 1.0f);
        layoutParams12.leftMargin = i12;
        linearLayout5.addView(ghost6, layoutParams12);
        android.widget.LinearLayout.LayoutParams layoutParams13 = new android.widget.LinearLayout.LayoutParams(-1, -2);
        layoutParams13.bottomMargin = i7;
        linearLayout.addView(linearLayout5, layoutParams13);
        final java.lang.Runnable[] runnableArr = new java.lang.Runnable[1];
        final android.widget.LinearLayout linearLayout9 = new android.widget.LinearLayout(activity);
        linearLayout9.setOrientation(1);
        linearLayout9.addView(groupLabel(activity, "快捷报文 · 点开填表，直接发"), groupLp(f));
        android.widget.LinearLayout linearLayout10 = new android.widget.LinearLayout(activity);
        linearLayout10.setOrientation(0);
        linearLayout10.setGravity(16);
        android.widget.TextView ghost14 = ghost(activity, "班级作业");
        ghost14.setOnClickListener(new android.view.View.OnClickListener() {
            @Override
            public void onClick(android.view.View view) {
                if (!com.cathelperawa.owo.app.bridge.CurrentChat.has()) {
                    com.cathelperawa.owo.app.bridge.CatToast.show("先在聊天里点开一个会话喵");
                } else {
                    com.cathelperawa.owo.app.ui.HomeworkSheet.open(activity, new com.cathelperawa.owo.app.ui.HomeworkSheet.OnDone() {
                        @Override
                        public void run(com.cathelperawa.owo.app.ui.HomeworkSheet.Params params) {
                            java.lang.String buildPb = com.cathelperawa.owo.app.ui.HomeworkSheet.buildPb(params);
                            if (buildPb == null) {
                                com.cathelperawa.owo.app.bridge.CatToast.show("拼报文失败喵");
                                return;
                            }
                            editText2.setText(com.cathelperawa.owo.app.feature.OidbCard.CMD);
                            editText.setText(buildPb);
                            iArr[0] = 1;
                            com.cathelperawa.owo.app.ui.SendPanel.saveMode(activity, 1);
                            if (runnableArr[0] != null) {
                                runnableArr[0].run();
                            }
                            com.cathelperawa.owo.app.bridge.CatToast.show("已填进 PB 页，检查后点「发送报文」");
                        }
                    });
                }
            }
        });
        int i13 = (int) (f * 6.0f);
        android.widget.LinearLayout.LayoutParams layoutParams19 = new android.widget.LinearLayout.LayoutParams(0, -2, 1.0f);
        layoutParams19.rightMargin = i13;
        linearLayout10.addView(ghost14, layoutParams19);
        linearLayout9.addView(linearLayout10, new android.widget.LinearLayout.LayoutParams(-1, -2));
        linearLayout9.setVisibility(8);
        linearLayout.addView(linearLayout9, new android.widget.LinearLayout.LayoutParams(-1, -2));
        final android.widget.TextView textView3 = new android.widget.TextView(activity);
        textView3.setText("发送到当前会话");
        textView3.setTextSize(15.0f);
        textView3.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        textView3.setTextColor(-1);
        textView3.setGravity(17);
        int i14 = (int) (f * 13.0f);
        textView3.setPadding(0, i14, 0, i14);
        android.graphics.drawable.GradientDrawable gradientDrawable4 = new android.graphics.drawable.GradientDrawable(android.graphics.drawable.GradientDrawable.Orientation.LEFT_RIGHT, new int[]{-28738, -1019745});
        gradientDrawable4.setCornerRadius(f2);
        gradientDrawable4.setStroke(i5, -22322);
        textView3.setBackgroundDrawable(gradientDrawable4);
        textView3.setElevation(f * 3.0f);
        textView3.setOnClickListener(new android.view.View.OnClickListener() {
            @Override
            public void onClick(android.view.View view) {
                try {
                    java.lang.String trim = editText.getText() == null ? "" : editText.getText().toString().trim();
                    java.lang.String trim2 = editText2.getText() != null ? editText2.getText().toString().trim() : "";
                    if (!com.cathelperawa.owo.app.bridge.CurrentChat.has()) {
                        com.cathelperawa.owo.app.bridge.CatToast.show("先在聊天里点开一个会话喵");
                        return;
                    }
                    if (trim.length() == 0) {
                        com.cathelperawa.owo.app.bridge.CatToast.show("代码是空的喵");
                        return;
                    }
                    if (iArr[0] == 0) {
                        java.lang.String arkFromElements = com.cathelperawa.owo.app.ui.SendPanel.arkFromElements(trim);
                        if (arkFromElements == null) {
                            arkFromElements = com.cathelperawa.owo.app.proto.PbSender.isArk(trim) ? trim : null;
                        }
                        if (arkFromElements == null || !com.cathelperawa.owo.app.ui.SendPanel.arkSignOn(activity)) {
                            com.cathelperawa.owo.app.ui.SendPanel.sendCode(trim, activity);
                        } else {
                            com.cathelperawa.owo.app.ui.SendPanel.signAndSend(arkFromElements, activity);
                        }
                        return;
                    }
                    if (trim2.length() == 0) {
                        com.cathelperawa.owo.app.bridge.CatToast.show("cmd 是空的喵");
                    } else {
                        com.cathelperawa.owo.app.ui.SendPanel.sendPacket(trim2, trim, activity);
                    }
                } catch (java.lang.Throwable th2) {
                    com.cathelperawa.owo.app.core.HookLog.e("发送失败", th2);
                    com.cathelperawa.owo.app.bridge.CatToast.show("发送异常：" + th2);
                }
            }
        });
        android.widget.LinearLayout.LayoutParams layoutParams23 = new android.widget.LinearLayout.LayoutParams(-1, -2);
        layoutParams23.bottomMargin = i8;
        linearLayout.addView(textView3, layoutParams23);
        boolean forceLong = forceLong(activity);
        if (forceLong) {
            str = "当前：走长消息通道";
        } else {
            str = "卡片 → 元素10 · 53.1==41 → 直发 · 长消息已关";
        }
        final android.widget.TextView tv2 = tv(activity, str, 10, -5654582, false);
        tv2.setGravity(17);
        final android.widget.LinearLayout linearLayout11 = new android.widget.LinearLayout(activity);
        linearLayout11.setOrientation(0);
        linearLayout11.setGravity(16);
        int i15 = (int) (8.0f * f);
        linearLayout11.setPadding(i4, i15, i7, i15);
        android.graphics.drawable.GradientDrawable gradientDrawable5 = new android.graphics.drawable.GradientDrawable();
        gradientDrawable5.setCornerRadius(f4);
        gradientDrawable5.setColor(-1929379841);
        gradientDrawable5.setStroke(i5, 1439291112);
        linearLayout11.setBackgroundDrawable(gradientDrawable5);
        android.widget.LinearLayout linearLayout12 = new android.widget.LinearLayout(activity);
        linearLayout12.setOrientation(1);
        linearLayout12.addView(tv(activity, "走长消息通道", 12, C_TITLE, true));
        if (forceLong) {
            str2 = "开：按 SsoSendLongMsg 长消息发";
        } else {
            str2 = "关：不发长消息（只走元素直发）";
        }
        final android.widget.TextView tv3 = tv(activity, str2, 10, C_SUB, false);
        linearLayout12.addView(tv3);
        linearLayout11.addView(linearLayout12, new android.widget.LinearLayout.LayoutParams(0, -2, 1.0f));
        final com.cathelperawa.owo.app.ui.SwitchBox switchBox = new com.cathelperawa.owo.app.ui.SwitchBox(activity, forceLong);
        switchBox.setOnClickListener(new android.view.View.OnClickListener() {
            @Override
            public void onClick(android.view.View view) {
                java.lang.String str9;
                java.lang.String str10;
                boolean z = !com.cathelperawa.owo.app.ui.SendPanel.forceLong(activity);
                try {
                    com.cathelperawa.owo.app.core.HookConfig.set(activity, "send_force_long", z ? "1" : com.cathelperawa.owo.app.bridge.GhostChat.GHOST_UID);
                } catch (java.lang.Throwable th2) {
                }
                switchBox.setChecked(z);
                android.widget.TextView textView4 = tv3;
                if (z) {
                    str9 = "开：按 SsoSendLongMsg 长消息发";
                } else {
                    str9 = "关：不发长消息（只走元素直发）";
                }
                textView4.setText(str9);
                android.widget.TextView textView5 = tv2;
                if (z) {
                    str10 = "当前：走长消息通道";
                } else {
                    str10 = "卡片 → 元素10 · 53.1==41 → 直发 · 长消息已关";
                }
                textView5.setText(str10);
            }
        });
        int i16 = (int) (44.0f * f);
        int i17 = (int) f5;
        linearLayout11.addView(switchBox, new android.widget.LinearLayout.LayoutParams(i16, i17));
        android.widget.LinearLayout.LayoutParams layoutParams24 = new android.widget.LinearLayout.LayoutParams(-1, -2);
        layoutParams24.bottomMargin = i15;
        linearLayout.addView(linearLayout11, layoutParams24);
        boolean z = com.cathelperawa.owo.app.core.HookConfig.getBoolean(activity, com.cathelperawa.owo.app.feature.PicLinkPick.K, false);
        android.widget.LinearLayout linearLayout13 = new android.widget.LinearLayout(activity);
        linearLayout13.setOrientation(0);
        linearLayout13.setGravity(16);
        linearLayout13.setPadding(i4, i15, i7, i15);
        android.graphics.drawable.GradientDrawable gradientDrawable6 = new android.graphics.drawable.GradientDrawable();
        gradientDrawable6.setCornerRadius(f4);
        gradientDrawable6.setColor(-1929379841);
        gradientDrawable6.setStroke(i5, 1439291112);
        linearLayout13.setBackgroundDrawable(gradientDrawable6);
        android.widget.LinearLayout linearLayout14 = new android.widget.LinearLayout(activity);
        linearLayout14.setOrientation(1);
        linearLayout14.addView(tv(activity, "长按提取图片链接", 12, C_TITLE, true));
        if (z) {
            str3 = "开：长按图片/表情可提取链接（复制到剪贴板）";
        } else {
            str3 = "关：长按菜单不出现这一项";
        }
        final android.widget.TextView tv4 = tv(activity, str3, 10, C_SUB, false);
        linearLayout14.addView(tv4);
        linearLayout13.addView(linearLayout14, new android.widget.LinearLayout.LayoutParams(0, -2, 1.0f));
        final com.cathelperawa.owo.app.ui.SwitchBox switchBox2 = new com.cathelperawa.owo.app.ui.SwitchBox(activity, z);
        switchBox2.setOnClickListener(new android.view.View.OnClickListener() {
            @Override
            public void onClick(android.view.View view) {
                java.lang.String str9;
                boolean z2 = !com.cathelperawa.owo.app.core.HookConfig.getBoolean(activity, com.cathelperawa.owo.app.feature.PicLinkPick.K, false);
                try {
                    com.cathelperawa.owo.app.core.HookConfig.set(activity, com.cathelperawa.owo.app.feature.PicLinkPick.K, z2 ? "1" : com.cathelperawa.owo.app.bridge.GhostChat.GHOST_UID);
                } catch (java.lang.Throwable th2) {
                }
                switchBox2.setChecked(z2);
                android.widget.TextView textView4 = tv4;
                if (z2) {
                    str9 = "开：长按图片/表情可提取链接（复制到剪贴板）";
                } else {
                    str9 = "关：长按菜单不出现这一项";
                }
                textView4.setText(str9);
            }
        });
        linearLayout13.addView(switchBox2, new android.widget.LinearLayout.LayoutParams(i16, i17));
        android.widget.LinearLayout.LayoutParams layoutParams25 = new android.widget.LinearLayout.LayoutParams(-1, -2);
        layoutParams25.bottomMargin = i15;
        linearLayout.addView(linearLayout13, layoutParams25);
        linearLayout.addView(tv2);
        final java.lang.Runnable runnable3 = new java.lang.Runnable() {
            @Override
            public void run() {
                java.lang.String str9;
                java.lang.String str10;
                int i18 = iArr[0];
                com.cathelperawa.owo.app.ui.SendPanel.paintCapsule(activity, capsule, i18 == 0);
                com.cathelperawa.owo.app.ui.SendPanel.paintCapsule(activity, textView2, i18 == 1);
                boolean z2 = i18 == 1;
                editText.setVisibility(0);
                editText2.setVisibility(z2 ? 0 : 8);
                linearLayout11.setVisibility(z2 ? 8 : 0);
                linearLayout9.setVisibility(z2 ? 0 : 8);
                linearLayout5.setVisibility(0);
                android.widget.EditText editText3 = editText;
                if (z2) {
                    str9 = "hex / base64 / protobuf JSON（key = 字段号）";
                } else {
                    str9 = "元素 JSON / 卡片 JSON / 预设条目";
                }
                editText3.setHint(str9);
                textView3.setText(z2 ? "发送报文" : "发送到当前会话");
                ghost5.setText("外框");
                ghost6.setVisibility(z2 ? 0 : 8);
                linearLayout5.setPadding(0, 0, 0, 0);
                if (z2) {
                    tv2.setText("PB 模式：原样塞进勾选的 cmd，不做三通道判定");
                    return;
                }
                android.widget.TextView textView4 = tv2;
                if (com.cathelperawa.owo.app.ui.SendPanel.forceLong(activity)) {
                    str10 = "当前：走长消息通道";
                } else {
                    str10 = "卡片 → 元素10 · 53.1==41 → 直发 · 长消息已关";
                }
                textView4.setText(str10);
            }
        };
        runnableArr[0] = runnable3;
        runnable3.run();
        ghost4.setOnClickListener(new android.view.View.OnClickListener() {
            @Override
            public void onClick(android.view.View view) {
                com.cathelperawa.owo.app.ui.SendPanel.openTemplates(activity, editText, iArr, runnable3, editText2);
            }
        });
        capsule.setOnClickListener(new android.view.View.OnClickListener() {
            @Override
            public void onClick(android.view.View view) {
                com.cathelperawa.owo.app.ui.SendPanel.switchMode(activity, iArr, 0, editText, editText2, runnable3);
            }
        });
        textView2.setOnClickListener(new android.view.View.OnClickListener() {
            @Override
            public void onClick(android.view.View view) {
                com.cathelperawa.owo.app.ui.SendPanel.switchMode(activity, iArr, 1, editText, editText2, runnable3);
            }
        });
        dialog.setContentView(frameLayout4);
        dialog.setCanceledOnTouchOutside(true);
        dialog.setCancelable(true);
        dialog.setOnKeyListener(new android.content.DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(android.content.DialogInterface dialogInterface, int i18, android.view.KeyEvent keyEvent) {
                if (i18 == 4 && keyEvent.getAction() == 1) {
                    dialogInterface.dismiss();
                    return true;
                }
                return false;
            }
        });
        frameLayout4.setOnTouchListener(new android.view.View.OnTouchListener() {

            @Override

            public boolean onTouch(android.view.View r6, android.view.MotionEvent r7) {

                throw new UnsupportedOperationException("Method not decompiled: com.cathelperawa.owo.app.ui.SendPanel.AnonymousClass36.onTouch(android.view.View, android.view.MotionEvent):boolean");
            }
        });
        android.view.Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(0));
            window.addFlags(2);
            android.view.WindowManager.LayoutParams attributes = window.getAttributes();
            attributes.width = -1;
            attributes.height = -1;
            attributes.gravity = 48;
            attributes.dimAmount = 0.22f;
            window.setSoftInputMode(16);
            window.setAttributes(attributes);
        }
        dialog.setOnDismissListener(new android.content.DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(android.content.DialogInterface dialogInterface) {
                if (com.cathelperawa.owo.app.ui.SendPanel.sCurrent == dialogInterface) {
                    android.app.Dialog unused = com.cathelperawa.owo.app.ui.SendPanel.sCurrent = null;
                }
                try {
                    com.cathelperawa.owo.app.ui.SendPanel.saveLast(activity, iArr[0], editText.getText() == null ? "" : editText.getText().toString(), editText2.getText() != null ? editText2.getText().toString() : "");
                } catch (java.lang.Throwable th2) {
                }
            }
        });
        return dialog;
    }



    static class AnonymousClass26 implements android.view.View.OnClickListener {
        final   android.app.Activity val$act;
        final   android.widget.TextView val$psSub;

        AnonymousClass26(android.app.Activity activity, android.widget.TextView textView) {
            this.val$act = activity;
            this.val$psSub = textView;
        }

        @Override
        public void onClick(android.view.View view) {
            com.cathelperawa.owo.app.ui.CatSheet.pickWithActions(this.val$act, "天玄通道 · p_skey", new java.lang.String[]{"① 一键打开 qun.qq.com 并提取（推荐）", "② 直接读现有 cookie", "③ 校验 p_skey 还能用吗"}, new com.cathelperawa.owo.app.ui.SendPanel.AnonymousClass26.AnonymousClass1(), new java.lang.String[]{"清掉缓存的 p_skey"}, new com.cathelperawa.owo.app.ui.CatSheet.OnPick() {
                @Override
                public void run(int i, java.lang.String str) {
                    com.cathelperawa.owo.app.feature.ArkSigner.clearPskey();
                    com.cathelperawa.owo.app.ui.SendPanel.AnonymousClass26.this.val$psSub.setText("本地缓存的 p_skey 已清掉");
                    com.cathelperawa.owo.app.bridge.CatToast.show("清掉了，下次发卡前记得重新提取");
                }
            });
        }

        class AnonymousClass1 implements com.cathelperawa.owo.app.ui.CatSheet.OnPick {
            AnonymousClass1() {
            }

            @Override
            public void run(int i, java.lang.String str) {
                if (i == 0) {
                    com.cathelperawa.owo.app.ui.PskeySheet.open(com.cathelperawa.owo.app.ui.SendPanel.AnonymousClass26.this.val$act, new com.cathelperawa.owo.app.ui.PskeySheet.OnDone() {
                        @Override
                        public void run(final com.cathelperawa.owo.app.feature.ArkSigner.PskeyResult pskeyResult) {
                            com.cathelperawa.owo.app.ui.SendPanel.AnonymousClass26.this.val$act.runOnUiThread(new java.lang.Runnable() {
                                @Override
                                public void run() {
                                    com.cathelperawa.owo.app.ui.SendPanel.AnonymousClass26.this.val$psSub.setText(pskeyResult.msg);
                                }
                            });
                        }
                    });
                    return;
                }
                if (i == 1) {
                    com.cathelperawa.owo.app.bridge.CatToast.show("正在提取…");
                    com.cathelperawa.owo.app.core.Bg.run(new java.lang.Runnable() {
                        @Override
                        public void run() {
                            final com.cathelperawa.owo.app.feature.ArkSigner.PskeyResult capturePskey = com.cathelperawa.owo.app.feature.ArkSigner.capturePskey();
                            com.cathelperawa.owo.app.ui.SendPanel.AnonymousClass26.this.val$act.runOnUiThread(new java.lang.Runnable() {
                                @Override
                                public void run() {
                                    com.cathelperawa.owo.app.ui.SendPanel.AnonymousClass26.this.val$psSub.setText(capturePskey.msg);
                                }
                            });
                            com.cathelperawa.owo.app.bridge.CatToast.show(capturePskey.msg);
                        }
                    }, "cat-pskey");
                } else if (i == 2) {
                    com.cathelperawa.owo.app.bridge.CatToast.show("正在校验…");
                    com.cathelperawa.owo.app.core.Bg.run(new java.lang.Runnable() {
                        @Override
                        public void run() {
                            final com.cathelperawa.owo.app.feature.ArkSigner.PskeyResult verifyPskey = com.cathelperawa.owo.app.feature.ArkSigner.verifyPskey();
                            com.cathelperawa.owo.app.ui.SendPanel.AnonymousClass26.this.val$act.runOnUiThread(new java.lang.Runnable() {
                                @Override
                                public void run() {
                                    com.cathelperawa.owo.app.ui.SendPanel.AnonymousClass26.this.val$psSub.setText(verifyPskey.msg);
                                }
                            });
                            com.cathelperawa.owo.app.bridge.CatToast.show(verifyPskey.msg);
                        }
                    }, "cat-pskey-verify");
                }
            }
        }
    }

    private static int navBar(android.content.Context context) {
        try {
            int identifier = context.getResources().getIdentifier("navigation_bar_height", "dimen", "android");
            if (identifier > 0) {
                return context.getResources().getDimensionPixelSize(identifier);
            }
        } catch (java.lang.Throwable th) {
        }
        return (int) (context.getResources().getDisplayMetrics().density * 48.0f);
    }

    private static android.widget.EditText fileField(android.content.Context context, float f, java.lang.String str, java.lang.String str2) {
        android.widget.EditText editText = new android.widget.EditText(context);
        if (str == null) {
            str = "";
        }
        editText.setText(str);
        editText.setHint(str2);
        editText.setTextSize(12.0f);
        editText.setTextColor(C_TITLE);
        editText.setHintTextColor(-4996910);
        editText.setSingleLine(true);
        float f2 = 14.0f * f;
        int i = (int) f2;
        int i2 = (int) (10.0f * f);
        editText.setPadding(i, i2, i, i2);
        android.graphics.drawable.GradientDrawable gradientDrawable = new android.graphics.drawable.GradientDrawable();
        gradientDrawable.setColor(-1275068417);
        gradientDrawable.setCornerRadius(f2);
        gradientDrawable.setStroke((int) f, 1724503784);
        editText.setBackgroundDrawable(gradientDrawable);
        return editText;
    }

    private static android.widget.TextView label(android.content.Context context, float f, java.lang.String str) {
        android.widget.TextView tv = tv(context, str, 11, C_SUB, true);
        tv.setPadding((int) (f * 4.0f), 0, 0, 0);
        return tv;
    }

    private static android.widget.LinearLayout.LayoutParams labelLp(float f) {
        android.widget.LinearLayout.LayoutParams layoutParams = new android.widget.LinearLayout.LayoutParams(-2, -2);
        layoutParams.bottomMargin = (int) (f * 3.0f);
        return layoutParams;
    }

    private static android.widget.LinearLayout.LayoutParams fieldLp(float f) {
        android.widget.LinearLayout.LayoutParams layoutParams = new android.widget.LinearLayout.LayoutParams(-1, -2);
        layoutParams.bottomMargin = (int) (f * 8.0f);
        return layoutParams;
    }

    public static java.lang.String locationTemplate() {
        long j;
        try {
            long currentTimeMillis = java.lang.System.currentTimeMillis() / 1000;
            try {
                j = java.lang.Long.parseLong(com.cathelperawa.owo.app.bridge.CurrentChat.peerRaw());
            } catch (java.lang.Throwable th) {
                j = 0;
            }
            org.json.JSONObject jSONObject = new org.json.JSONObject();
            jSONObject.put("app", "com.tencent.map");
            org.json.JSONObject jSONObject2 = new org.json.JSONObject();
            jSONObject2.put("autosize", false);
            jSONObject2.put("ctime", currentTimeMillis);
            jSONObject2.put("forward", true);
            jSONObject2.put("token", "8e715ac1eb75f510ea9725d958c6d070");
            jSONObject2.put("type", "normal");
            jSONObject.put("config", jSONObject2);
            jSONObject.put("desc", "");
            jSONObject.put("from", 1);
            org.json.JSONObject jSONObject3 = new org.json.JSONObject();
            jSONObject3.put("address", "");
            jSONObject3.put("enum_relation_type", 1);
            jSONObject3.put("from", "plusPanel");
            jSONObject3.put("from_account", 0);
            jSONObject3.put("id", "");
            jSONObject3.put("lat", com.cathelperawa.owo.app.bridge.GhostChat.GHOST_UID);
            jSONObject3.put("lng", com.cathelperawa.owo.app.bridge.GhostChat.GHOST_UID);
            jSONObject3.put("name", "");
            jSONObject3.put("uint64_peer_account", j);
            org.json.JSONObject jSONObject4 = new org.json.JSONObject();
            jSONObject4.put("Location.Search", jSONObject3);
            jSONObject.put("meta", jSONObject4);
            jSONObject.put("prompt", "[位置]");
            jSONObject.put("ver", "1.1.2.21");
            jSONObject.put("view", "LocationShare");
            return jSONObject.toString();
        } catch (java.lang.Throwable th2) {
            return null;
        }
    }

    public static void dumpArk(java.lang.String str, java.lang.String str2) {
        try {
            com.cathelperawa.owo.app.core.HookLog.i("ARKDUMP[" + str + "] " + str2);
        } catch (java.lang.Throwable th) {
        }
        try {
            android.content.Context context = com.cathelperawa.owo.app.core.HookItemFactory.context();
            if (context == null) {
                return;
            }
            java.io.File file = new java.io.File(context.getFilesDir(), "arkdump");
            if (!file.exists()) {
                file.mkdirs();
            }
            java.io.FileOutputStream fileOutputStream = new java.io.FileOutputStream(new java.io.File(file, "ark_" + str + ".json"));
            fileOutputStream.write(str2 == null ? new byte[0] : str2.getBytes("UTF-8"));
            fileOutputStream.flush();
            fileOutputStream.close();
        } catch (java.lang.Throwable th2) {
        }
    }

    public static void signAndSend(final java.lang.String str, android.content.Context context) {

        if (true) {
            com.cathelperawa.owo.app.feature.ArkSmart.send(context, str, null);
            return;
        }
        if (!signOn(context)) {
            sendCode(str, context);
            return;
        }
        final int chatType = com.cathelperawa.owo.app.bridge.CurrentChat.chatType();
        final java.lang.String peerUid = com.cathelperawa.owo.app.bridge.CurrentChat.peerUid();
        final java.lang.String signChannel = signChannel(context);
        com.cathelperawa.owo.app.bridge.CatToast.show("正在签名（" + com.cathelperawa.owo.app.feature.ArkSigner.channelShort(signChannel) + "）…");
        com.cathelperawa.owo.app.core.Bg.run(new java.lang.Runnable() {
            @Override
            public void run() {
                java.lang.String str2;
                try {
                    com.cathelperawa.owo.app.ui.SendPanel.dumpArk("work", str);
                    com.cathelperawa.owo.app.feature.ArkSigner.Result signByChannel = com.cathelperawa.owo.app.feature.ArkSigner.signByChannel(str, signChannel);
                    com.cathelperawa.owo.app.ui.SendPanel.dumpArk("signed", signByChannel.ok() ? signByChannel.json : "ERR:" + signByChannel.error);
                    if (com.cathelperawa.owo.app.feature.ArkSigner.SKIP.equals(signByChannel.error)) {
                        boolean sendArkNative = com.cathelperawa.owo.app.ui.SendPanel.sendArkNative(str, chatType, peerUid);
                        if (!sendArkNative) {
                            sendArkNative = com.cathelperawa.owo.app.proto.PbSender.sendDirect(com.cathelperawa.owo.app.ui.SendPanel.wrapArk(str), chatType, peerUid);
                        }
                        com.cathelperawa.owo.app.bridge.CatToast.show(sendArkNative ? "已发出（这张卡不用签名）" : "发送失败，看日志喵");
                        return;
                    }
                    if (signByChannel.ok()) {
                        boolean sendArkNative2 = com.cathelperawa.owo.app.ui.SendPanel.sendArkNative(signByChannel.json, chatType, peerUid);
                        if (!sendArkNative2) {
                            sendArkNative2 = com.cathelperawa.owo.app.proto.PbSender.sendDirect(com.cathelperawa.owo.app.ui.SendPanel.wrapArk(signByChannel.json), chatType, peerUid);
                        }
                        if (sendArkNative2) {
                            str2 = "已签名并发出（" + com.cathelperawa.owo.app.feature.ArkSigner.channelShort(signChannel) + "）";
                        } else {
                            str2 = "签名成功但发送失败，看日志喵";
                        }
                        com.cathelperawa.owo.app.bridge.CatToast.show(str2);
                        return;
                    }
                    com.cathelperawa.owo.app.bridge.CatToast.show("签名失败：" + signByChannel.error);
                } catch (java.lang.Throwable th) {
                    com.cathelperawa.owo.app.core.HookLog.e("signAndSend 失败", th);
                    com.cathelperawa.owo.app.bridge.CatToast.show("签名异常：" + th);
                }
            }
        }, "cat-ark-sign");
    }

    public static void sendArkJson(final android.content.Context context, final java.lang.String str) {
        com.cathelperawa.owo.app.feature.ArkSmart.send(context, str, null);
    }

    public static void signAndSendNews(java.lang.String str, android.content.Context context) {
        signAndSend(str, context);
    }


    private static void signLinkByService(final android.content.Context context, final java.lang.String str, final java.lang.String str2, final java.lang.String str3, final java.lang.String str4, final java.lang.String str5) {
        final int chatType = com.cathelperawa.owo.app.bridge.CurrentChat.chatType();
        final java.lang.String peerUid = com.cathelperawa.owo.app.bridge.CurrentChat.peerUid();
        com.cathelperawa.owo.app.bridge.CatToast.show("正在签名…");
        com.cathelperawa.owo.app.core.Bg.run(new java.lang.Runnable() {
            @Override
            public void run() {
                try {
                    java.lang.String signLinkCard = com.cathelperawa.owo.app.feature.ArkSigner.signLinkCard(str, str2, str3, str4, str5);
                    if (signLinkCard == null) {
                        com.cathelperawa.owo.app.core.HookLog.w("只跳转卡服务签名失败，退回天玄");
                        java.lang.String build = com.cathelperawa.owo.app.ui.CardFormSheet.build(str, str2, str3, str4, com.cathelperawa.owo.app.feature.ArkSigner.platformTag(str5), com.cathelperawa.owo.app.feature.ArkSigner.platformIcon(str5));
                        if (build != null && com.cathelperawa.owo.app.ui.SendPanel.arkSignOn(context)) {
                            com.cathelperawa.owo.app.ui.SendPanel.signAndSendNews(build, context);
                            return;
                        } else if (build != null) {
                            com.cathelperawa.owo.app.ui.SendPanel.sendCode(build, context);
                            return;
                        } else {
                            com.cathelperawa.owo.app.bridge.CatToast.show("只跳转卡签名失败，看日志喵");
                            return;
                        }
                    }
                    com.cathelperawa.owo.app.feature.ArkSigner.dumpSent("表单-只跳转/" + str5, signLinkCard);
                    boolean sendArkNative = com.cathelperawa.owo.app.ui.SendPanel.sendArkNative(signLinkCard, chatType, peerUid);
                    if (!sendArkNative) {
                        sendArkNative = com.cathelperawa.owo.app.proto.PbSender.sendDirect(com.cathelperawa.owo.app.ui.SendPanel.wrapArk(signLinkCard), chatType, peerUid);
                    }
                    com.cathelperawa.owo.app.bridge.CatToast.show(sendArkNative ? "卡片已发出～" : "签名成功但发送失败");
                } catch (java.lang.Throwable th) {
                    com.cathelperawa.owo.app.core.HookLog.e("signLinkByService 失败", th);
                    com.cathelperawa.owo.app.bridge.CatToast.show("发卡异常：" + th);
                }
            }
        }, "cat-link-svc");
    }

    private static void signNewsByFallback(android.content.Context context, final java.lang.String str, final java.lang.String str2, final java.lang.String str3, final java.lang.String str4) {
        final int chatType = com.cathelperawa.owo.app.bridge.CurrentChat.chatType();
        final java.lang.String peerUid = com.cathelperawa.owo.app.bridge.CurrentChat.peerUid();
        com.cathelperawa.owo.app.bridge.CatToast.show("正在签名…");
        com.cathelperawa.owo.app.core.Bg.run(new java.lang.Runnable() {
            @Override
            public void run() {
                try {
                    com.cathelperawa.owo.app.feature.ArkSigner.Result signNewsFallback = com.cathelperawa.owo.app.feature.ArkSigner.signNewsFallback(str, str2, str3, str4);
                    if (signNewsFallback != null && signNewsFallback.ok()) {
                        boolean sendArkNative = com.cathelperawa.owo.app.ui.SendPanel.sendArkNative(signNewsFallback.json, chatType, peerUid);
                        if (!sendArkNative) {
                            sendArkNative = com.cathelperawa.owo.app.proto.PbSender.sendDirect(com.cathelperawa.owo.app.ui.SendPanel.wrapArk(signNewsFallback.json), chatType, peerUid);
                        }
                        com.cathelperawa.owo.app.bridge.CatToast.show(sendArkNative ? "已签名并发出" : "签名成功但发送失败");
                        return;
                    }
                    com.cathelperawa.owo.app.bridge.CatToast.show("签名失败，看日志喵");
                } catch (java.lang.Throwable th) {
                    com.cathelperawa.owo.app.core.HookLog.e("signNewsByFallback 失败", th);
                    com.cathelperawa.owo.app.bridge.CatToast.show("发卡异常：" + th);
                }
            }
        }, "cat-news-fb");
    }



    public static void sendCode(final java.lang.String str, final android.content.Context context) {
        final int chatType = com.cathelperawa.owo.app.bridge.CurrentChat.chatType();
        final java.lang.String peerUid = com.cathelperawa.owo.app.bridge.CurrentChat.peerUid();
        com.cathelperawa.owo.app.core.Bg.run(new java.lang.Runnable() {
            @Override
            public void run() {
                boolean sendDirect;
                java.lang.StringBuilder append;
                java.lang.String str2;
                java.lang.String str3 = str;
                com.cathelperawa.owo.app.ui.SendPanel.dbg(context, "mode=card ct=" + chatType + " uid=" + peerUid + " ark=" + com.cathelperawa.owo.app.proto.PbSender.isArk(str3) + " long=" + com.cathelperawa.owo.app.ui.SendPanel.forceLong(context) + " len=" + str3.length() + " ");
                java.lang.String arkFromElements = com.cathelperawa.owo.app.ui.SendPanel.arkFromElements(str3);
                java.lang.String str4 = "协议层元素10";
                if (arkFromElements != null) {
                    sendDirect = com.cathelperawa.owo.app.ui.SendPanel.sendArkNative(arkFromElements, chatType, peerUid);
                    if (sendDirect) {
                        str4 = "内核 ark（元素10解包）";
                    } else {
                        sendDirect = com.cathelperawa.owo.app.proto.PbSender.sendDirect(com.cathelperawa.owo.app.ui.SendPanel.wrapArk(arkFromElements), chatType, peerUid);
                    }
                } else if (com.cathelperawa.owo.app.proto.PbSender.isArk(str3)) {
                    boolean sendArkNative = com.cathelperawa.owo.app.ui.SendPanel.sendArkNative(str3, chatType, peerUid);
                    if (!sendArkNative) {
                        java.lang.String wrapArk = com.cathelperawa.owo.app.ui.SendPanel.wrapArk(str3);
                        if (com.cathelperawa.owo.app.ui.SendPanel.forceLong(context)) {
                            sendDirect = com.cathelperawa.owo.app.proto.PbSender.sendLong(wrapArk, chatType, peerUid);
                        } else {
                            sendDirect = com.cathelperawa.owo.app.proto.PbSender.sendDirect(wrapArk, chatType, peerUid);
                        }
                    } else {
                        str4 = "内核 ark（原生元素）";
                        sendDirect = sendArkNative;
                    }
                } else if (com.cathelperawa.owo.app.ui.SendPanel.forceLong(context)) {
                    sendDirect = com.cathelperawa.owo.app.proto.PbSender.sendLong(str3, chatType, peerUid);
                    str4 = "长消息";
                } else {
                    sendDirect = com.cathelperawa.owo.app.proto.PbSender.sendDirect(str3, chatType, peerUid);
                    str4 = "PbSendMsg 直发";
                }
                com.cathelperawa.owo.app.ui.SendPanel.dbg(context, "" + str4 + " → " + (sendDirect ? "sendCmd 返回 true" : "sendCmd 返回 false"));
                if (sendDirect) {
                    append = new java.lang.StringBuilder().append("已发送（").append(str4);
                    str2 = "）";
                } else {
                    append = new java.lang.StringBuilder().append("发送失败（").append(str4);
                    str2 = "），看日志喵";
                }
                com.cathelperawa.owo.app.bridge.CatToast.show(append.append(str2).toString());
            }
        }, "cat-send-code");
    }

    public static void openTemplates(final android.app.Activity activity, final android.widget.EditText editText, final int[] iArr, final java.lang.Runnable runnable, final android.widget.EditText editText2) {
        java.util.List<com.cathelperawa.owo.app.core.TemplateStore.Item> all = com.cathelperawa.owo.app.core.TemplateStore.all(activity);
        final int i = iArr[0] == 0 ? 1 : 0;
        final java.util.ArrayList arrayList = new java.util.ArrayList();
        for (int i2 = 0; i2 < all.size(); i2++) {
            com.cathelperawa.owo.app.core.TemplateStore.Item item = all.get(i2);
            if (item.mode == iArr[0]) {
                arrayList.add(item);
            }
        }
        if (arrayList.isEmpty() && i == 0) {
            com.cathelperawa.owo.app.bridge.CatToast.show("PB 模板是空的喵 —— 写好报文点「存为模板」");
            return;
        }
        java.lang.String[] strArr = new java.lang.String[arrayList.size() + i];
        if (i != 0) {
            strArr[0] = "＋ 新建卡片（填表）";
        }
        for (int i3 = 0; i3 < arrayList.size(); i3++) {
            strArr[i3 + i] = ((com.cathelperawa.owo.app.core.TemplateStore.Item) arrayList.get(i3)).name;
        }
        final boolean z = i != 0;
        com.cathelperawa.owo.app.ui.CatSheet.pickWithActions(activity, (i != 0 ? "卡片模板 · " : "PB 模板 · ") + arrayList.size() + " 条", strArr, new com.cathelperawa.owo.app.ui.CatSheet.OnPick() {
            @Override
            public void run(int i4, java.lang.String str) {
                if (z && i4 == 0) {
                    com.cathelperawa.owo.app.ui.CardFormSheet.open(activity, new com.cathelperawa.owo.app.ui.CardFormSheet.OnDone() {
                        @Override
                        public void run(java.lang.String str2, java.lang.String str3, java.lang.String str4, java.lang.String str5, java.lang.String str6, java.lang.String str7) {
                            java.lang.String build = com.cathelperawa.owo.app.ui.CardFormSheet.build(str2, str3, str4, str5, str6, str7);
                            if (build == null) {
                                com.cathelperawa.owo.app.bridge.CatToast.show("构造失败喵");
                                return;
                            }
                            editText.setText(build);
                            if (com.cathelperawa.owo.app.ui.SendPanel.arkSignOn(activity)) {
                                com.cathelperawa.owo.app.ui.SendPanel.signAndSend(build, activity);
                            } else {
                                com.cathelperawa.owo.app.ui.SendPanel.sendCode(build, activity);
                            }
                        }
                    });
                    return;
                }
                com.cathelperawa.owo.app.core.TemplateStore.Item item2 = (com.cathelperawa.owo.app.core.TemplateStore.Item) arrayList.get(i4 - i);
                editText.setText(item2.content);
                if (editText2 != null && item2.cmd != null && item2.cmd.length() > 0) {
                    editText2.setText(item2.cmd);
                }
                iArr[0] = item2.mode;
                runnable.run();
                com.cathelperawa.owo.app.bridge.CatToast.show("已载入模板：" + item2.name + ((item2.cmd == null || item2.cmd.length() <= 0) ? "" : "（含 cmd）"));
            }
        }, new java.lang.String[]{"存为模板", "管理模板"}, new com.cathelperawa.owo.app.ui.CatSheet.OnPick() {
            @Override
            public void run(int i4, java.lang.String str) {
                if (i4 == 0) {
                    com.cathelperawa.owo.app.ui.SendPanel.saveAsTemplate(activity, editText, iArr, editText2);
                } else {
                    com.cathelperawa.owo.app.ui.SendPanel.manageTemplates(activity, iArr[0]);
                }
            }
        });
    }

    public static void saveAsTemplate(final android.app.Activity activity, android.widget.EditText editText, final int[] iArr, android.widget.EditText editText2) {
        final java.lang.String trim = editText.getText() == null ? "" : editText.getText().toString().trim();
        if (trim.length() == 0) {
            com.cathelperawa.owo.app.bridge.CatToast.show("输入框是空的，没东西可存喵");
            return;
        }
        final boolean z = iArr[0] == 1;
        final java.lang.String trim2 = (!z || editText2 == null || editText2.getText() == null) ? "" : editText2.getText().toString().trim();
        if (z && trim2.length() == 0) {
            com.cathelperawa.owo.app.bridge.CatToast.show("PB 模板要连 cmd 一起存，先把 cmd 填上喵");
        } else {
            final java.lang.String suggestName = com.cathelperawa.owo.app.core.JsonPretty.suggestName(trim, trim2, z);
            com.cathelperawa.owo.app.ui.CatSheet.input(activity, z ? "PB 模板名字" : "卡片模板名字", "", suggestName, new com.cathelperawa.owo.app.ui.CatSheet.OnText() {
                @Override
                public void run(java.lang.String str) {
                    boolean z2;
                    java.lang.String trim3v = str == null ? "" : str.trim();
                    if (trim3v.length() == 0) {
                        trim3v = suggestName;
                    }
                    final java.lang.String trim3 = trim3v;
                    java.util.Iterator<com.cathelperawa.owo.app.core.TemplateStore.Item> it = com.cathelperawa.owo.app.core.TemplateStore.all(activity).iterator();
                    while (true) {
                        if (!it.hasNext()) {
                            z2 = false;
                            break;
                        } else if (it.next().name.equals(trim3)) {
                            z2 = true;
                            break;
                        }
                    }
                    java.lang.Runnable runnable = new java.lang.Runnable() {
                        @Override
                        public void run() {
                            java.lang.String str2;
                            if (com.cathelperawa.owo.app.core.TemplateStore.save(activity, trim3, iArr[0], trim, trim2)) {
                                str2 = "已存进" + (z ? "PB" : "卡片") + "模板：" + trim3;
                            } else {
                                str2 = "存模板失败喵";
                            }
                            com.cathelperawa.owo.app.bridge.CatToast.show(str2);
                        }
                    };
                    if (z2) {
                        com.cathelperawa.owo.app.ui.CatSheet.confirm(activity, "已有同名模板", "「" + trim3 + "」已经存在，覆盖它吗？", "覆盖", false, runnable);
                    } else {
                        runnable.run();
                    }
                }
            });
        }
    }

    public static void manageTemplates(android.app.Activity activity, int i) {
        java.util.List<com.cathelperawa.owo.app.core.TemplateStore.Item> all = com.cathelperawa.owo.app.core.TemplateStore.all(activity);
        java.util.ArrayList arrayList = new java.util.ArrayList();
        for (int i2 = 0; i2 < all.size(); i2++) {
            if (all.get(i2).mode == i) {
                arrayList.add(all.get(i2));
            }
        }
        if (arrayList.isEmpty()) {
            com.cathelperawa.owo.app.bridge.CatToast.show(i == 0 ? "卡片模板库是空的喵" : "PB 模板库是空的喵");
            return;
        }
        java.lang.String[] strArr = new java.lang.String[arrayList.size()];
        for (int i3 = 0; i3 < arrayList.size(); i3++) {
            strArr[i3] = ((com.cathelperawa.owo.app.core.TemplateStore.Item) arrayList.get(i3)).name;
        }
        com.cathelperawa.owo.app.ui.CatSheet.pick(activity, (i == 0 ? "卡片模板 · " : "PB 模板 · ") + arrayList.size() + " 条", strArr, new com.cathelperawa.owo.app.ui.SendPanel.AnonymousClass47(arrayList, activity));
    }

    static class AnonymousClass47 implements com.cathelperawa.owo.app.ui.CatSheet.OnPick {
        final   android.app.Activity val$act;
        final   java.util.List val$list;

        AnonymousClass47(java.util.List list, android.app.Activity activity) {
            this.val$list = list;
            this.val$act = activity;
        }

        @Override
        public void run(int i, java.lang.String str) {
            final com.cathelperawa.owo.app.core.TemplateStore.Item item = (com.cathelperawa.owo.app.core.TemplateStore.Item) this.val$list.get(i);
            com.cathelperawa.owo.app.ui.CatSheet.pick(this.val$act, item.name, new java.lang.String[]{"改名", "删除"}, new com.cathelperawa.owo.app.ui.CatSheet.OnPick() {
                @Override
                public void run(int i2, java.lang.String str2) {
                    if (i2 == 0) {
                        com.cathelperawa.owo.app.ui.CatSheet.input(com.cathelperawa.owo.app.ui.SendPanel.AnonymousClass47.this.val$act, "新名字", "改名", item.name, new com.cathelperawa.owo.app.ui.CatSheet.OnText() {
                            @Override
                            public void run(java.lang.String str3) {
                                com.cathelperawa.owo.app.bridge.CatToast.show(com.cathelperawa.owo.app.core.TemplateStore.rename(com.cathelperawa.owo.app.ui.SendPanel.AnonymousClass47.this.val$act, item.name, str3) ? "改名成功喵" : "改名失败喵");
                            }
                        });
                    } else {
                        com.cathelperawa.owo.app.bridge.CatToast.show(com.cathelperawa.owo.app.core.TemplateStore.remove(com.cathelperawa.owo.app.ui.SendPanel.AnonymousClass47.this.val$act, item.name) ? "已删除：" + item.name : "删除失败喵");
                    }
                }
            });
        }
    }

    public static java.lang.String arkFromElements(java.lang.String str) {
        java.lang.String trim;
        org.json.JSONObject optJSONObject;
        if (str == null) {
            trim = "";
        } else {
            try {
                trim = str.trim();
            } catch (java.lang.Throwable th) {
                return null;
            }
        }
        if (!trim.startsWith("{")) {
            return null;
        }
        try {
            org.json.JSONObject jSONObject = new org.json.JSONObject(trim);
            if ((jSONObject.has("app") && jSONObject.has("view")) || (optJSONObject = jSONObject.optJSONObject(com.cathelperawa.owo.app.feature.FeatureMsgTime.DEF_SIZE)) == null) {
                return null;
            }
            java.lang.String optString = optJSONObject.optString("1", "");
            if (optString.length() < 8) {
                return null;
            }
            new org.json.JSONObject(optString);
            return optString;
        } catch (java.lang.Throwable th) {
            return null;
        }
    }

    public static boolean sendArkNative(java.lang.String str, int i, java.lang.String str2) {
        try {
            return sendOneNative(com.cathelperawa.owo.app.bridge.ElementFactory.arkNative(str), i, str2, com.cathelperawa.owo.app.proto.MediaVault.T_ARK);
        } catch (java.lang.Throwable th) {
            com.cathelperawa.owo.app.core.HookLog.e("sendArkNative 失败", th);
            return false;
        }
    }

    private static boolean sendOneNative(java.lang.Object obj, int i, java.lang.String str, java.lang.String str2) {
        java.lang.Object make;
        if (obj == null) {
            return false;
        }
        try {
            java.util.ArrayList arrayList = new java.util.ArrayList();
            arrayList.add(obj);
            java.lang.Object make2 = com.cathelperawa.owo.app.bridge.ContactFactory.make(i, str, "");
            if (make2 != null && com.cathelperawa.owo.app.bridge.MsgSender.send(make2, arrayList)) {
                return true;
            }
            java.lang.String peerRaw = com.cathelperawa.owo.app.bridge.CurrentChat.peerRaw();
            if (i != 1 || peerRaw.length() <= 0 || peerRaw.equals(str) || (make = com.cathelperawa.owo.app.bridge.ContactFactory.make(i, peerRaw, "")) == null) {
                return false;
            }
            dbg(null, str2 + " 兜底用原始 peerId=" + peerRaw);
            return com.cathelperawa.owo.app.bridge.MsgSender.send(make, arrayList);
        } catch (java.lang.Throwable th) {
            com.cathelperawa.owo.app.core.HookLog.e("sendOneNative 失败", th);
            return false;
        }
    }

    public static java.lang.String wrapArk(java.lang.String str) {
        try {
            return "{\"10\":{\"1\":" + org.json.JSONObject.quote(str) + "}}";
        } catch (java.lang.Throwable th) {
            return str;
        }
    }

    public static void sendPacket(final java.lang.String str, final java.lang.String str2, final android.app.Activity activity) {
        com.cathelperawa.owo.app.core.Bg.run(new java.lang.Runnable() {
            @Override
            public void run() {
                long currentGroupUin;
                java.lang.String retarget;
                try {
                    android.app.Activity activity2 = activity;
                    if (activity2 != null) {
                        com.cathelperawa.owo.app.bridge.CurrentChat.fromActivity(activity2);
                    }
                } catch (java.lang.Throwable th) {
                }
                try {
                    java.lang.String str3 = str2;
                    java.lang.String str4 = "";
                    java.lang.String str5 = str;
                    if (str5 != null && str5.contains("0xdc2") && str3 != null && str3.trim().startsWith("{") && (retarget = com.cathelperawa.owo.app.feature.OidbCard.retarget(str3, (currentGroupUin = com.cathelperawa.owo.app.feature.OidbCard.currentGroupUin()))) != null && !retarget.equals(str3)) {
                        str4 = " · 群号已改成当前群 " + currentGroupUin;
                        str3 = retarget;
                    }
                    byte[] decodePayload = com.cathelperawa.owo.app.ui.SendPanel.decodePayload(str3);
                    if (decodePayload != null && decodePayload.length != 0) {
                        int sendOidbAndWait = com.cathelperawa.owo.app.proto.PbSender.sendOidbAndWait(str, decodePayload, 3500);
                        java.lang.String str6 = str + " (" + decodePayload.length + "B)" + str4;
                        if (sendOidbAndWait == 0) {
                            com.cathelperawa.owo.app.bridge.CatToast.show("已发送 · 服务端确认成功 " + str6);
                        } else if (sendOidbAndWait == -999) {
                            com.cathelperawa.owo.app.bridge.CatToast.show("已发出，但 3.5 秒没回包（可能还在路上）" + str6);
                        } else if (sendOidbAndWait == -1) {
                            com.cathelperawa.owo.app.bridge.CatToast.show("已发出，回包解析不出结果 " + str6);
                        } else {
                            com.cathelperawa.owo.app.bridge.CatToast.show("服务端拒绝：code=" + sendOidbAndWait + " " + str6);
                        }
                        return;
                    }
                    com.cathelperawa.owo.app.bridge.CatToast.show("payload 解析不出来喵");
                } catch (java.lang.Throwable th2) {
                    com.cathelperawa.owo.app.core.HookLog.e("发包失败", th2);
                    com.cathelperawa.owo.app.bridge.CatToast.show("发包失败：" + th2);
                }
            }
        }, "cat-send-pb");
    }

    public static byte[] decodePayload(java.lang.String str) {
        java.lang.String trim = str == null ? "" : str.trim();
        if (trim.length() == 0) {
            return null;
        }
        if (!trim.startsWith("{")) {
            java.lang.String replaceAll = trim.replaceAll("\\s+", "");
            if (replaceAll.length() >= 2 && replaceAll.length() % 2 == 0 && replaceAll.matches("[0-9a-fA-F]+")) {
                int length = replaceAll.length() / 2;
                byte[] bArr = new byte[length];
                for (int i = 0; i < length; i++) {
                    int i2 = i * 2;
                    bArr[i] = (byte) java.lang.Integer.parseInt(replaceAll.substring(i2, i2 + 2), 16);
                }
                return bArr;
            }
            try {
                return android.util.Base64.decode(trim, 2);
            } catch (java.lang.Throwable th) {
                return null;
            }
        }
        try {
            org.json.JSONObject jSONObject = new org.json.JSONObject(trim);
            com.cathelperawa.owo.app.ovo.FunProtoData funProtoData = new com.cathelperawa.owo.app.ovo.FunProtoData();
            funProtoData.fromJSON(jSONObject);
            return funProtoData.toBytes();
        } catch (java.lang.Throwable th2) {
            return null;
        }
    }

    public static void dbg(android.content.Context context, java.lang.String str) {
        if (context == null) {
            try {
                context = com.cathelperawa.owo.app.bridge.QQEnv.topActivity();
            } catch (java.lang.Throwable th) {
                return;
            }
        }
        if (context == null) {
            return;
        }
        java.io.File file = new java.io.File(context.getFilesDir(), "send_proto_debug.txt");
        if (file.length() > 204800) {
            return;
        }
        try {
            java.io.FileOutputStream fileOutputStream = new java.io.FileOutputStream(file, true);
            try {
                fileOutputStream.write((new java.text.SimpleDateFormat(com.cathelperawa.owo.app.feature.FeatureMsgTime.DEF_FORMAT, java.util.Locale.US).format(new java.util.Date()) + " [send] " + str + "\n").getBytes("UTF-8"));
                fileOutputStream.close();
            } catch (java.lang.Throwable th2) {
                fileOutputStream.close();
                throw th2;
            }
        } catch (java.lang.Throwable th3) {
        }
    }

    private static android.widget.TextView groupLabel(android.content.Context context, java.lang.String str) {
        android.widget.TextView tv = tv(context, str, 10, -7363652, true);
        tv.setPadding((int) (context.getResources().getDisplayMetrics().density * 6.0f), 0, 0, 0);
        return tv;
    }

    private static android.widget.LinearLayout.LayoutParams groupLp(float f) {
        android.widget.LinearLayout.LayoutParams layoutParams = new android.widget.LinearLayout.LayoutParams(-1, -2);
        layoutParams.topMargin = (int) (6.0f * f);
        layoutParams.bottomMargin = (int) (f * 5.0f);
        return layoutParams;
    }

    private static android.widget.TextView tv(android.content.Context context, java.lang.String str, int i, int i2, boolean z) {
        android.widget.TextView textView = new android.widget.TextView(context);
        textView.setText(str);
        textView.setTextSize(i);
        textView.setTextColor(i2);
        if (z) {
            textView.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        }
        return textView;
    }

    private static android.widget.TextView ghost(android.content.Context context, java.lang.String str) {
        float f = context.getResources().getDisplayMetrics().density;
        android.widget.TextView tv = tv(context, str, 12, -9530410, true);
        tv.setGravity(17);
        int i = (int) (10.0f * f);
        tv.setPadding(0, i, 0, i);
        android.graphics.drawable.GradientDrawable gradientDrawable = new android.graphics.drawable.GradientDrawable();
        gradientDrawable.setCornerRadius(14.0f * f);
        gradientDrawable.setColor(2063597567);
        gradientDrawable.setStroke((int) f, 1724503784);
        tv.setBackgroundDrawable(gradientDrawable);
        return tv;
    }

    static void openMiniShare(final android.app.Activity activity) {

        if (sMiniDlg != null) {
            try {
                sMiniDlg.dismiss();
            } catch (Throwable th) {
            }
            sMiniDlg = null;
        }
        float f = activity.getResources().getDisplayMetrics().density;
        final android.app.Dialog dialog = new android.app.Dialog(activity, android.R.style.Theme_Translucent_NoTitleBar);
        final android.widget.ScrollView scrollView = new android.widget.ScrollView(activity);
        scrollView.setVerticalScrollBarEnabled(false);
        android.graphics.drawable.GradientDrawable gradientDrawable = new android.graphics.drawable.GradientDrawable();
        gradientDrawable.setCornerRadius(24.0f * f);
        gradientDrawable.setColor(-131585);
        scrollView.setBackgroundDrawable(gradientDrawable);
        scrollView.setClipToOutline(true);
        android.widget.LinearLayout linearLayout = new android.widget.LinearLayout(activity);
        linearLayout.setOrientation(1);
        int i = (int) (20.0f * f);
        float f2 = 14.0f * f;
        int i2 = (int) f2;
        linearLayout.setPadding(i, (int) (f * 18.0f), i, i2);
        android.widget.TextView textView = new android.widget.TextView(activity);
        textView.setText("小程序卡改写");
        textView.setTextSize(18.0f);
        textView.setTextColor(C_TITLE);
        textView.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        linearLayout.addView(textView);
        android.widget.TextView textView2 = new android.widget.TextView(activity);
        textView2.setText("B 站分享到 QQ 时替换卡片内容 · 走服务端签名，别人看得见");
        textView2.setTextSize(11.0f);
        textView2.setTextColor(C_SUB);
        int i3 = (int) (2.0f * f);
        textView2.setLineSpacing(i3, 1.0f);
        int i4 = (int) (4.0f * f);
        int i5 = (int) (8.0f * f);
        textView2.setPadding(0, i4, 0, i5);
        linearLayout.addView(textView2);
        boolean z = com.cathelperawa.owo.app.core.HookConfig.getBoolean(activity, com.cathelperawa.owo.app.feature.ArkSigner.K_SHARE, false);
        android.widget.TextView textView3 = new android.widget.TextView(activity);
        textView3.setText(z ? "通用分享改写 · 已开 ▸" : "通用分享改写 ▸");
        textView3.setTextSize(11.0f);
        textView3.setTextColor(z ? -1543522 : -9530410);
        textView3.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        textView3.setGravity(17);
        float f3 = f * 10.0f;
        int i6 = (int) f3;
        int i7 = (int) (f * 7.0f);
        textView3.setPadding(i6, i7, i6, i7);
        android.graphics.drawable.GradientDrawable gradientDrawable2 = new android.graphics.drawable.GradientDrawable();
        gradientDrawable2.setCornerRadius(f3);
        gradientDrawable2.setColor(z ? -5390 : -1051396);
        int i8 = (int) f;
        gradientDrawable2.setStroke(java.lang.Math.max(1, i8), z ? -14628 : -2497293);
        textView3.setBackgroundDrawable(gradientDrawable2);
        android.widget.LinearLayout.LayoutParams layoutParams = new android.widget.LinearLayout.LayoutParams(-2, -2);
        layoutParams.bottomMargin = i5;
        linearLayout.addView(textView3, layoutParams);
        textView3.setOnClickListener(new android.view.View.OnClickListener() {
            @Override
            public void onClick(android.view.View view) {
                dialog.dismiss();
                com.cathelperawa.owo.app.ui.SendPanel.openShareForm(activity);
            }
        });
        android.widget.LinearLayout linearLayout2 = new android.widget.LinearLayout(activity);
        linearLayout2.setOrientation(1);
        android.graphics.drawable.GradientDrawable gradientDrawable3 = new android.graphics.drawable.GradientDrawable();
        gradientDrawable3.setCornerRadius(f2);
        gradientDrawable3.setColor(-657156);
        linearLayout2.setBackgroundDrawable(gradientDrawable3);
        final boolean[] zArr = {com.cathelperawa.owo.app.core.HookConfig.getBoolean(activity, com.cathelperawa.owo.app.feature.ArkSigner.K_MINI, false)};
        miniToggle(activity, linearLayout2, "启用改写", zArr, f);
        android.view.View view = new android.view.View(activity);
        view.setBackgroundColor(-1577226);
        android.widget.LinearLayout.LayoutParams layoutParams2 = new android.widget.LinearLayout.LayoutParams(-1, java.lang.Math.max(1, i8));
        int i9 = (int) (f * 12.0f);
        layoutParams2.leftMargin = i9;
        layoutParams2.rightMargin = i9;
        linearLayout2.addView(view, layoutParams2);
        final boolean[] zArr2 = {com.cathelperawa.owo.app.core.HookConfig.getBoolean(activity, com.cathelperawa.owo.app.feature.ArkSigner.K_MINI_BILI, true)};
        miniToggle(activity, linearLayout2, "只对哔哩哔哩生效", zArr2, f);
        android.view.View view2 = new android.view.View(activity);
        view2.setBackgroundColor(-1577226);
        android.widget.LinearLayout.LayoutParams layoutParams3 = new android.widget.LinearLayout.LayoutParams(-1, java.lang.Math.max(1, i8));
        layoutParams3.leftMargin = i9;
        layoutParams3.rightMargin = i9;
        linearLayout2.addView(view2, layoutParams3);
        final boolean[] zArr3 = {com.cathelperawa.owo.app.core.HookConfig.getBoolean(activity, K_WX_ON, false)};
        miniToggle(activity, linearLayout2, "发成微信小程序卡", zArr3, f);
        android.view.View view3 = new android.view.View(activity);
        view3.setBackgroundColor(-1577226);
        android.widget.LinearLayout.LayoutParams layoutParams4 = new android.widget.LinearLayout.LayoutParams(-1, java.lang.Math.max(1, i8));
        layoutParams4.leftMargin = i9;
        layoutParams4.rightMargin = i9;
        linearLayout2.addView(view3, layoutParams4);
        android.widget.LinearLayout linearLayout3 = new android.widget.LinearLayout(activity);
        linearLayout3.setOrientation(0);
        linearLayout3.setGravity(16);
        linearLayout3.setPadding(i9, i6, i9, i6);
        android.widget.TextView textView4 = new android.widget.TextView(activity);
        textView4.setText("选择卡");
        textView4.setTextSize(13.0f);
        textView4.setTextColor(C_TITLE);
        linearLayout3.addView(textView4, new android.widget.LinearLayout.LayoutParams(0, -2, 1.0f));
        final android.widget.TextView textView5 = new android.widget.TextView(activity);
        textView5.setTextSize(12.0f);
        textView5.setTextColor(-1543522);
        textView5.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        textView5.setText(wxNameOf(activity));
        linearLayout3.addView(textView5);
        linearLayout2.addView(linearLayout3);
        linearLayout3.setOnClickListener(new android.view.View.OnClickListener() {
            @Override
            public void onClick(android.view.View view4) {
                com.cathelperawa.owo.app.ui.SendPanel.pickWxCard(activity, new com.cathelperawa.owo.app.ui.CatDialog.OnText() {
                    @Override
                    public void run(java.lang.String str) {
                        com.cathelperawa.owo.app.core.HookConfig.set(activity, com.cathelperawa.owo.app.ui.SendPanel.K_WX_QAPPID, str);
                        textView5.setText(com.cathelperawa.owo.app.ui.SendPanel.wxNameOf(activity));
                    }
                });
            }
        });
        android.widget.LinearLayout.LayoutParams layoutParams5 = new android.widget.LinearLayout.LayoutParams(-1, -2);
        layoutParams5.topMargin = i4;
        linearLayout.addView(linearLayout2, layoutParams5);

        final android.widget.LinearLayout wxBox = new android.widget.LinearLayout(activity);
        wxBox.setOrientation(1);
        miniGroup(activity, wxBox, "卡片内容", f);

        final android.widget.EditText miniWx = miniEdit(activity, wxBox, "小程序 wxid", "决定小程序图标；留空 = 哔哩哔哩 wx17e1394849aa4de2", f, com.cathelperawa.owo.app.core.HookConfig.get(activity, "wxmini_appid", "wx17e1394849aa4de2"));
        final android.widget.EditText miniEdit = miniEdit(activity, wxBox, "外显文案", "卡片上那行字，留空 = 保持 B 站原文案", f, com.cathelperawa.owo.app.core.HookConfig.get(activity, com.cathelperawa.owo.app.feature.ArkSigner.K_MINI_TITLE, ""));
        final android.widget.EditText miniEdit2 = miniEdit(activity, wxBox, "描述", "副标题，B 站小程序这条链路一般不吃，留空即可", f, com.cathelperawa.owo.app.core.HookConfig.get(activity, com.cathelperawa.owo.app.feature.ArkSigner.K_MINI_DESC, ""));
        final android.widget.EditText miniEdit3 = miniEdit(activity, wxBox, "封面图直链", "https://… 公网可访问，别用 100×100 的小程序图标", f, com.cathelperawa.owo.app.core.HookConfig.get(activity, com.cathelperawa.owo.app.feature.ArkSigner.K_MINI_PIC, ""));
        android.widget.TextView textView6 = new android.widget.TextView(activity);
        textView6.setText("选图并转成永久直链");
        textView6.setTextSize(11.0f);
        textView6.setTextColor(-9530410);
        textView6.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        textView6.setGravity(17);
        textView6.setPadding(i6, i7, i6, i7);
        android.graphics.drawable.GradientDrawable gradientDrawable4 = new android.graphics.drawable.GradientDrawable();
        gradientDrawable4.setCornerRadius(f3);
        gradientDrawable4.setColor(-1051396);
        gradientDrawable4.setStroke(java.lang.Math.max(1, i8), -2497293);
        textView6.setBackgroundDrawable(gradientDrawable4);
        android.widget.LinearLayout.LayoutParams layoutParams6 = new android.widget.LinearLayout.LayoutParams(-2, -2);
        layoutParams6.topMargin = i7;
        wxBox.addView(textView6, layoutParams6);
        textView6.setOnClickListener(new com.cathelperawa.owo.app.ui.SendPanel.AnonymousClass52(activity, miniEdit3));
        miniGroup(activity, wxBox, "点击跳转", f);
        final android.widget.EditText miniEdit4 = miniEdit(activity, wxBox, "打开的网页", "https://… 留空 = 还是跳 B 站", f, com.cathelperawa.owo.app.core.HookConfig.get(activity, com.cathelperawa.owo.app.feature.ArkSigner.K_MINI_WEB, ""));
        android.widget.TextView textView7 = new android.widget.TextView(activity);
        textView7.setText("角标和卡片上的小程序名由 appid 决定，改不了");
        textView7.setTextSize(10.0f);
        textView7.setTextColor(-5654840);
        textView7.setPadding(0, i6, 0, i3);
        wxBox.addView(textView7);
        android.widget.TextView miniPill = miniPill(activity, "探测小程序 appid（不发消息）", false, f);
        android.widget.LinearLayout.LayoutParams layoutParams7 = new android.widget.LinearLayout.LayoutParams(-1, -2);
        int i10 = (int) (6.0f * f);
        layoutParams7.topMargin = i10;
        wxBox.addView(miniPill, layoutParams7);
        miniPill.setOnClickListener(new com.cathelperawa.owo.app.ui.SendPanel.AnonymousClass53(activity, f));
        android.widget.LinearLayout linearLayout4 = new android.widget.LinearLayout(activity);
        linearLayout4.setOrientation(0);
        linearLayout4.setPadding(0, i2, 0, 0);
        android.widget.TextView miniPill2 = miniPill(activity, "取消", false, f);
        miniPill2.setOnClickListener(new android.view.View.OnClickListener() {
            @Override
            public void onClick(android.view.View view4) {
                dialog.dismiss();
            }
        });
        android.widget.LinearLayout.LayoutParams layoutParams8 = new android.widget.LinearLayout.LayoutParams(0, -2, 1.0f);
        layoutParams8.rightMargin = i10;
        linearLayout4.addView(miniPill2, layoutParams8);
        android.widget.TextView miniPill3 = miniPill(activity, "保存", true, f);
        miniPill3.setOnClickListener(new android.view.View.OnClickListener() {
            @Override
            public void onClick(android.view.View view4) {
                com.cathelperawa.owo.app.core.HookConfig.set(activity, com.cathelperawa.owo.app.feature.ArkSigner.K_MINI, zArr[0] ? "1" : com.cathelperawa.owo.app.bridge.GhostChat.GHOST_UID);
                com.cathelperawa.owo.app.core.HookConfig.set(activity, com.cathelperawa.owo.app.feature.ArkSigner.K_MINI_BILI, zArr2[0] ? "1" : com.cathelperawa.owo.app.bridge.GhostChat.GHOST_UID);
                com.cathelperawa.owo.app.core.HookConfig.set(activity, com.cathelperawa.owo.app.ui.SendPanel.K_WX_ON, zArr3[0] ? "1" : com.cathelperawa.owo.app.bridge.GhostChat.GHOST_UID);
                com.cathelperawa.owo.app.core.HookConfig.set(activity, com.cathelperawa.owo.app.feature.ArkSigner.K_MINI_TITLE, com.cathelperawa.owo.app.ui.SendPanel.miniStr(miniEdit));
                com.cathelperawa.owo.app.core.HookConfig.set(activity, com.cathelperawa.owo.app.feature.ArkSigner.K_MINI_DESC, com.cathelperawa.owo.app.ui.SendPanel.miniStr(miniEdit2));
                com.cathelperawa.owo.app.core.HookConfig.set(activity, com.cathelperawa.owo.app.feature.ArkSigner.K_MINI_PIC, com.cathelperawa.owo.app.ui.SendPanel.miniStr(miniEdit3));
                com.cathelperawa.owo.app.core.HookConfig.set(activity, com.cathelperawa.owo.app.feature.ArkSigner.K_MINI_WEB, com.cathelperawa.owo.app.ui.SendPanel.miniStr(miniEdit4));
                java.lang.String wxid0 = com.cathelperawa.owo.app.ui.SendPanel.miniStr(miniWx);
                com.cathelperawa.owo.app.core.HookConfig.set(activity, "wxmini_appid", wxid0.length() == 0 ? "wx17e1394849aa4de2" : wxid0);
                com.cathelperawa.owo.app.core.HookConfig.reload();
                dialog.dismiss();
                com.cathelperawa.owo.app.bridge.CatToast.show(zArr[0] ? "保存好了，去 B 站点分享试试" : "已关闭改写");
            }
        });
        android.widget.LinearLayout.LayoutParams layoutParams9 = new android.widget.LinearLayout.LayoutParams(0, -2, 1.0f);
        layoutParams9.leftMargin = i10;
        layoutParams9.rightMargin = i10;
        linearLayout4.addView(miniPill3, layoutParams9);
        android.widget.TextView miniPill4 = miniPill(activity, "发送", true, f);
        miniPill4.setOnClickListener(new android.view.View.OnClickListener() {
            @Override
            public void onClick(android.view.View view4) {
                java.lang.String miniStr = com.cathelperawa.owo.app.ui.SendPanel.miniStr(miniEdit);
                if (miniStr.length() == 0) {
                    com.cathelperawa.owo.app.bridge.CatToast.show("外显文案不能空喵");
                    return;
                }
                com.cathelperawa.owo.app.core.HookConfig.set(activity, com.cathelperawa.owo.app.feature.ArkSigner.K_MINI, zArr[0] ? "1" : com.cathelperawa.owo.app.bridge.GhostChat.GHOST_UID);
                com.cathelperawa.owo.app.core.HookConfig.set(activity, com.cathelperawa.owo.app.feature.ArkSigner.K_MINI_BILI, zArr2[0] ? "1" : com.cathelperawa.owo.app.bridge.GhostChat.GHOST_UID);
                com.cathelperawa.owo.app.core.HookConfig.set(activity, com.cathelperawa.owo.app.feature.ArkSigner.K_MINI_TITLE, miniStr);
                com.cathelperawa.owo.app.core.HookConfig.set(activity, com.cathelperawa.owo.app.feature.ArkSigner.K_MINI_DESC, com.cathelperawa.owo.app.ui.SendPanel.miniStr(miniEdit2));
                com.cathelperawa.owo.app.core.HookConfig.set(activity, com.cathelperawa.owo.app.feature.ArkSigner.K_MINI_PIC, com.cathelperawa.owo.app.ui.SendPanel.miniStr(miniEdit3));
                com.cathelperawa.owo.app.core.HookConfig.set(activity, com.cathelperawa.owo.app.feature.ArkSigner.K_MINI_WEB, com.cathelperawa.owo.app.ui.SendPanel.miniStr(miniEdit4));
                com.cathelperawa.owo.app.core.HookConfig.reload();
                if (!com.cathelperawa.owo.app.bridge.CurrentChat.has()) {
                    com.cathelperawa.owo.app.bridge.CatToast.show("先在聊天里点开一个会话喵");
                    return;
                }
                dialog.dismiss();
                com.cathelperawa.owo.app.bridge.CatToast.show("正在请服务端签名…");
                if (zArr3[0]) {
                    com.cathelperawa.owo.app.ui.SendPanel.wxQAppIdOf(activity);
                    java.lang.String str = com.cathelperawa.owo.app.ui.SendPanel.miniStr(miniWx);
                    if (str.length() == 0) {
                        str = "wx17e1394849aa4de2";
                    }
                    com.cathelperawa.owo.app.core.HookConfig.set(activity, "wxmini_appid", str);
                    com.cathelperawa.owo.app.bridge.CatToast.show("正在签微信小程序卡…");
                    com.cathelperawa.owo.app.feature.ArkSigner.sendWxMiniCard(miniStr, com.cathelperawa.owo.app.ui.SendPanel.miniStr(miniEdit2), com.cathelperawa.owo.app.ui.SendPanel.miniStr(miniEdit3), com.cathelperawa.owo.app.ui.SendPanel.miniStr(miniEdit4), "", str, 1, new com.cathelperawa.owo.app.feature.ArkSigner.SendCb() {
                        @Override
                        public void done(final boolean z2, final java.lang.String str2) {
                            activity.runOnUiThread(new java.lang.Runnable() {
                                @Override
                                public void run() {
                                    com.cathelperawa.owo.app.bridge.CatToast.show(z2 ? "已发出" : "失败：" + str2);
                                }
                            });
                        }
                    });
                    return;
                }
                com.cathelperawa.owo.app.feature.ArkSigner.sendMiniCard(com.cathelperawa.owo.app.ui.SendPanel.miniStr(miniEdit2), com.cathelperawa.owo.app.ui.SendPanel.miniStr(miniEdit3), com.cathelperawa.owo.app.ui.SendPanel.miniStr(miniEdit4), "", new com.cathelperawa.owo.app.feature.ArkSigner.SendCb() {
                    @Override
                    public void done(final boolean z2, final java.lang.String str2) {
                        activity.runOnUiThread(new java.lang.Runnable() {
                            @Override
                            public void run() {
                                com.cathelperawa.owo.app.bridge.CatToast.show(z2 ? "卡片已发出" : "失败：" + str2);
                            }
                        });
                    }
                });
            }
        });
        android.widget.LinearLayout.LayoutParams layoutParams10 = new android.widget.LinearLayout.LayoutParams(0, -2, 1.0f);
        layoutParams10.leftMargin = i10;
        linearLayout4.addView(miniPill4, layoutParams10);
        linearLayout.addView(wxBox);
        linearLayout.addView(linearLayout4);
        scrollView.addView(linearLayout, new android.view.ViewGroup.LayoutParams(-1, -2));
        dialog.setContentView(scrollView, new android.view.ViewGroup.LayoutParams(-1, -2));
        android.view.Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(0));
            window.addFlags(2);
            android.view.WindowManager.LayoutParams attributes = window.getAttributes();
            attributes.dimAmount = 0.45f;
            window.setAttributes(attributes);
            window.setLayout((int) (activity.getResources().getDisplayMetrics().widthPixels * 0.92f), -2);
            window.setSoftInputMode(16);
        }
        dialog.setCanceledOnTouchOutside(true);
        dialog.setCancelable(true);
        dialog.setOnDismissListener(new android.content.DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(android.content.DialogInterface dialogInterface) {
                com.cathelperawa.owo.app.ui.SendPanel.sMiniDlg = null;
                com.cathelperawa.owo.app.ui.SendPanel.sMiniRefresh = null;
            }
        });
        com.cathelperawa.owo.app.ui.SendPanel.sMiniDlg = dialog;
        dialog.show();

        scrollView.post(new java.lang.Runnable() {
            @Override
            public void run() {
                try {
                    int iMaxH = (int) (activity.getResources().getDisplayMetrics().heightPixels * 0.82f);
                    android.view.View child = scrollView.getChildAt(0);
                    int iChildH = child == null ? 0 : child.getHeight();
                    if (iChildH > iMaxH && dialog.getWindow() != null) {
                        dialog.getWindow().setLayout((int) (activity.getResources().getDisplayMetrics().widthPixels * 0.92f), iMaxH);
                    }
                } catch (Throwable th) {
                }
            }
        });
    }

    static class AnonymousClass52 implements android.view.View.OnClickListener {
        final   android.app.Activity val$act;
        final   android.widget.EditText val$ePic;

        AnonymousClass52(android.app.Activity activity, android.widget.EditText editText) {
            this.val$act = activity;
            this.val$ePic = editText;
        }

        class AnonymousClass1 implements com.cathelperawa.owo.app.ui.PickerBridge.OnPick {
            AnonymousClass1() {
            }

            @Override
            public void run(java.lang.String str) {
                if (str == null || str.length() == 0) {
                    return;
                }
                com.cathelperawa.owo.app.bridge.CatToast.show("上传中，别退出…");
                com.cathelperawa.owo.app.feature.ArkSigner.hostImage(str, new com.cathelperawa.owo.app.feature.ArkSigner.HostCb() {
                    @Override
                    public void done(final java.lang.String str2, final java.lang.String str3) {
                        com.cathelperawa.owo.app.ui.SendPanel.AnonymousClass52.this.val$act.runOnUiThread(new java.lang.Runnable() {
                            @Override
                            public void run() {
                                if (str2 == null) {
                                    com.cathelperawa.owo.app.bridge.CatToast.show("图床失败：" + str3);
                                } else {
                                    com.cathelperawa.owo.app.ui.SendPanel.AnonymousClass52.this.val$ePic.setText(str2);
                                    com.cathelperawa.owo.app.bridge.CatToast.show("转好了，已填进封面图");
                                }
                            }
                        });
                    }
                });
            }
        }

        @Override
        public void onClick(android.view.View view) {
            com.cathelperawa.owo.app.ui.PickerBridge.pick(this.val$act, new com.cathelperawa.owo.app.ui.SendPanel.AnonymousClass52.AnonymousClass1());
        }
    }

    static class AnonymousClass53 implements android.view.View.OnClickListener {
        final   android.app.Activity val$act;
        final   float val$d;

        AnonymousClass53(android.app.Activity activity, float f) {
            this.val$act = activity;
            this.val$d = f;
        }

        @Override
        public void onClick(android.view.View view) {
            android.app.Activity activity = this.val$act;
            com.cathelperawa.owo.app.ui.CatDialog.input(activity, "要探测的 appid（逗号分隔）", "1109937557,1109048181", com.cathelperawa.owo.app.core.HookConfig.get(activity, "wxmini_probe_ids", "1104307008,1111015245,1102112936,1104466820,1106467070,1102113115,1112198072,1112280105,1104922185,1110613799,1110543085,1112463488,1112214854,1111677210,1106838536,1106040804,1112313390,1106658188,1104874204,1104864068,1110112648,1102164637,1102161029,1102121842,1109840991,1109937557"), true, new com.cathelperawa.owo.app.ui.SendPanel.AnonymousClass53.AnonymousClass1());
        }

        class AnonymousClass1 implements com.cathelperawa.owo.app.ui.CatDialog.OnText {
            AnonymousClass1() {
            }

            @Override
            public void run(java.lang.String str) {
                java.lang.String trim = str == null ? "" : str.trim();
                if (trim.length() == 0) {
                    return;
                }
                if (!trim.matches("^[0-9,\\s，]+$")) {
                    java.lang.StringBuilder sb = new java.lang.StringBuilder();
                    java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("11\\d{8}").matcher(trim);
                    while (matcher.find()) {
                        if (sb.length() > 0) {
                            sb.append(',');
                        }
                        sb.append(matcher.group());
                    }
                    if (sb.length() > 0) {
                        trim = sb.toString();
                    }
                }
                com.cathelperawa.owo.app.core.HookConfig.set(com.cathelperawa.owo.app.ui.SendPanel.AnonymousClass53.this.val$act, "wxmini_probe_ids", trim);
                com.cathelperawa.owo.app.bridge.CatToast.show("开始探测，稍等…");
                try {
                    com.cathelperawa.owo.app.feature.ArkSigner.probeMiniAppIds(trim, new com.cathelperawa.owo.app.feature.ArkSigner.SendCb() {
                        @Override
                        public void done(boolean z, final java.lang.String str2) {
                            com.cathelperawa.owo.app.ui.SendPanel.AnonymousClass53.this.val$act.runOnUiThread(new java.lang.Runnable() {
                                @Override
                                public void run() {
                                    com.cathelperawa.owo.app.ui.SendPanel.showProbeResult(com.cathelperawa.owo.app.ui.SendPanel.AnonymousClass53.this.val$act, com.cathelperawa.owo.app.ui.SendPanel.AnonymousClass53.this.val$d, str2);
                                }
                            });
                        }
                    });
                } catch (java.lang.Throwable th) {
                    com.cathelperawa.owo.app.core.HookLog.e("探测失败", th);
                }
            }
        }
    }

    private static void miniGroup(android.app.Activity activity, android.widget.LinearLayout linearLayout, java.lang.String str, float f) {
        android.widget.LinearLayout linearLayout2 = new android.widget.LinearLayout(activity);
        linearLayout2.setOrientation(0);
        linearLayout2.setGravity(16);
        android.widget.LinearLayout.LayoutParams layoutParams = new android.widget.LinearLayout.LayoutParams(-1, -2);
        layoutParams.topMargin = (int) (16.0f * f);
        float f2 = 2.0f * f;
        layoutParams.bottomMargin = (int) f2;
        android.view.View view = new android.view.View(activity);
        android.graphics.drawable.GradientDrawable gradientDrawable = new android.graphics.drawable.GradientDrawable();
        gradientDrawable.setCornerRadius(f2);
        gradientDrawable.setColor(-22842);
        view.setBackgroundDrawable(gradientDrawable);
        android.widget.LinearLayout.LayoutParams layoutParams2 = new android.widget.LinearLayout.LayoutParams((int) (3.0f * f), (int) (11.0f * f));
        layoutParams2.rightMargin = (int) (f * 7.0f);
        linearLayout2.addView(view, layoutParams2);
        android.widget.TextView textView = new android.widget.TextView(activity);
        textView.setText(str);
        textView.setTextSize(12.0f);
        textView.setTextColor(-9665885);
        textView.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        linearLayout2.addView(textView);
        linearLayout.addView(linearLayout2, layoutParams);
    }

    private static android.widget.EditText miniEdit(android.app.Activity activity, android.widget.LinearLayout linearLayout, java.lang.String str, java.lang.String str2, float f, java.lang.String str3) {
        android.widget.TextView textView = new android.widget.TextView(activity);
        textView.setText(str);
        textView.setTextSize(12.0f);
        textView.setTextColor(C_TITLE);
        textView.setPadding(0, (int) (10.0f * f), 0, (int) (5.0f * f));
        linearLayout.addView(textView);
        android.widget.EditText editText = new android.widget.EditText(activity);
        editText.setTextSize(13.0f);
        editText.setTextColor(C_TITLE);
        editText.setHint(str2);
        editText.setHintTextColor(-4602668);
        editText.setSingleLine(true);
        if (str3 == null) {
            str3 = "";
        }
        editText.setText(str3);
        float f2 = 12.0f * f;
        int i = (int) f2;
        int i2 = (int) (9.0f * f);
        editText.setPadding(i, i2, i, i2);
        android.graphics.drawable.GradientDrawable gradientDrawable = new android.graphics.drawable.GradientDrawable();
        gradientDrawable.setCornerRadius(f2);
        gradientDrawable.setColor(-722949);
        gradientDrawable.setStroke(java.lang.Math.max(1, (int) f), -1905933);
        editText.setBackgroundDrawable(gradientDrawable);
        linearLayout.addView(editText, new android.widget.LinearLayout.LayoutParams(-1, -2));
        return editText;
    }

    static java.util.List<java.lang.String[]> wxCandidates(android.content.Context context) {
        java.util.ArrayList arrayList = new java.util.ArrayList();
        java.util.HashSet hashSet = new java.util.HashSet();
        for (java.lang.String str : ("1109840991|腾讯视频;1109937557|哔哩哔哩;1109907872|QQ安全中心;1109048181|腾讯客服;1109786902|订阅号;1109896843|老师助手;1109836759|火柴人神射手;1110112648|腾讯原创馆;" + com.cathelperawa.owo.app.core.HookConfig.get(context, "wxmini_extra", "")).split(";")) {
            java.lang.String[] split = str.split("\\|");
            if (split.length == 2 && split[0].trim().length() != 0 && hashSet.add(split[0].trim())) {
                arrayList.add(new java.lang.String[]{split[0].trim(), split[1].trim()});
            }
        }
        return arrayList;
    }

    static java.lang.String wxNameOf(android.content.Context context) {
        java.lang.String str = "";
        java.lang.String str2 = com.cathelperawa.owo.app.core.HookConfig.get(context, K_WX_QAPPID, "");
        for (java.lang.String[] strArr : wxCandidates(context)) {
            if (str.length() == 0) {
                str = strArr[1];
            }
            if (strArr[0].equals(str2)) {
                return strArr[1];
            }
        }
        return str.length() == 0 ? "未选" : str;
    }

    static java.lang.String wxQAppIdOf(android.content.Context context) {
        java.lang.String str = "";
        java.lang.String str2 = com.cathelperawa.owo.app.core.HookConfig.get(context, K_WX_QAPPID, "");
        for (java.lang.String[] strArr : wxCandidates(context)) {
            if (str.length() == 0) {
                str = strArr[0];
            }
            if (strArr[0].equals(str2)) {
                return str2;
            }
        }
        return str;
    }

    static void pickWxCard(android.app.Activity activity, com.cathelperawa.owo.app.ui.CatDialog.OnText onText) {
        int i;
        java.util.ArrayList arrayList = new java.util.ArrayList();
        java.util.ArrayList arrayList2 = new java.util.ArrayList();
        java.util.Iterator<java.lang.String[]> it = wxCandidates(activity).iterator();
        while (true) {
            if (!it.hasNext()) {
                break;
            }
            java.lang.String[] next = it.next();
            arrayList2.add(next[0]);
            arrayList.add(next[1]);
        }
        java.lang.String[] strArr = new java.lang.String[arrayList.size() + 2];
        for (i = 0; i < arrayList.size(); i++) {
            strArr[i] = ((java.lang.String) arrayList.get(i)) + "    " + ((java.lang.String) arrayList2.get(i));
        }
        strArr[arrayList.size()] = "＋ 添加新的 appid…";
        strArr[arrayList.size() + 1] = "✕ 删除自定义角标…";
        activity.runOnUiThread(new com.cathelperawa.owo.app.ui.SendPanel.AnonymousClass57(activity, strArr, arrayList2, onText));
    }

    static void pickWxDel(android.app.Activity activity, final com.cathelperawa.owo.app.ui.CatDialog.OnText onText) {
        final java.util.ArrayList arrayList = new java.util.ArrayList();
        final java.util.ArrayList arrayList2 = new java.util.ArrayList();
        java.util.Iterator<java.lang.String[]> it = wxCandidates(activity).iterator();
        while (it.hasNext()) {
            java.lang.String[] next = it.next();
            if (wxIsExtra(activity, next[0])) {
                arrayList2.add(next[0]);
                arrayList.add(next[1]);
            }
        }
        if (arrayList.size() == 0) {
            com.cathelperawa.owo.app.bridge.CatToast.show("没有自定义角标，内置的删不掉喵");
            return;
        }
        final java.lang.String[] strArr = new java.lang.String[arrayList.size()];
        for (int i = 0; i < arrayList.size(); i++) {
            strArr[i] = ((java.lang.String) arrayList.get(i)) + "    " + ((java.lang.String) arrayList2.get(i));
        }
        final android.app.Activity act = activity;
        activity.runOnUiThread(new java.lang.Runnable() {
            @Override
            public void run() {
                new android.app.AlertDialog.Builder(act, 5).setTitle("点一下删除角标").setItems(strArr, new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface dialogInterface, final int i) {
                        new android.app.AlertDialog.Builder(act, 5).setTitle("删除这个角标？").setMessage(strArr[i]).setPositiveButton("删除", new android.content.DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(android.content.DialogInterface dialogInterface2, int w) {
                                com.cathelperawa.owo.app.ui.SendPanel.wxDropExtra(act, (java.lang.String) arrayList2.get(i));
                                com.cathelperawa.owo.app.bridge.CatToast.show("已删除");
                                com.cathelperawa.owo.app.ui.SendPanel.pickWxDel(act, onText);
                            }
                        }).setNegativeButton("取消", (android.content.DialogInterface.OnClickListener) null).show();
                    }
                }).setNegativeButton("返回", new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface dialogInterface, int i) {
                        com.cathelperawa.owo.app.ui.SendPanel.pickWxCard(act, onText);
                    }
                }).show();
            }
        });
    }

    static class AnonymousClass57 implements java.lang.Runnable {
        final   android.app.Activity val$act;
        final   com.cathelperawa.owo.app.ui.CatDialog.OnText val$cb;
        final   java.util.ArrayList val$ids;
        final   java.lang.String[] val$items;

        AnonymousClass57(android.app.Activity activity, java.lang.String[] strArr, java.util.ArrayList arrayList, com.cathelperawa.owo.app.ui.CatDialog.OnText onText) {
            this.val$act = activity;
            this.val$items = strArr;
            this.val$ids = arrayList;
            this.val$cb = onText;
        }

        @Override
        public void run() {
            final android.app.Activity act = this.val$act;
            final com.cathelperawa.owo.app.ui.CatDialog.OnText cb = this.val$cb;
            final java.util.ArrayList ids = this.val$ids;
            final java.lang.String[] items = this.val$items;
            android.app.AlertDialog alertDialog = new android.app.AlertDialog.Builder(act, 5).setTitle("选择卡片角标（长按删除）").setItems(items, new android.content.DialogInterface.OnClickListener() {
                @Override
                public void onClick(android.content.DialogInterface dialogInterface, int i) {
                    if (i == ids.size()) {
                        com.cathelperawa.owo.app.ui.CatDialog.input(act, "新增 appid（格式：appid|名字）", "1109840991|腾讯视频", "", false, new com.cathelperawa.owo.app.ui.CatDialog.OnText() {
                            @Override
                            public void run(java.lang.String str) {
                                java.lang.String trim = str == null ? "" : str.trim();
                                if (trim.length() == 0) {
                                    return;
                                }
                                java.lang.String str2 = com.cathelperawa.owo.app.core.HookConfig.get(act, "wxmini_extra", "");
                                if (str2.length() != 0) {
                                    trim = str2 + ";" + trim;
                                }
                                com.cathelperawa.owo.app.core.HookConfig.set(act, "wxmini_extra", trim);
                                com.cathelperawa.owo.app.bridge.CatToast.show("已加入列表");
                            }
                        });
                    } else if (i > ids.size()) {
                        com.cathelperawa.owo.app.ui.SendPanel.pickWxDel(act, cb);
                    } else {
                        cb.run((java.lang.String) ids.get(i));
                    }
                }
            }).setNegativeButton("取消", (android.content.DialogInterface.OnClickListener) null).show();

            alertDialog.getListView().setOnItemLongClickListener(new android.widget.AdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(android.widget.AdapterView<?> adapterView, android.view.View view, int i, long j) {
                    if (i >= ids.size()) {
                        return true;
                    }
                    final java.lang.String aid = (java.lang.String) ids.get(i);
                    if (!com.cathelperawa.owo.app.ui.SendPanel.wxIsExtra(act, aid)) {
                        com.cathelperawa.owo.app.bridge.CatToast.show("内置角标删不掉喵～");
                        return true;
                    }
                    new android.app.AlertDialog.Builder(act, 5).setTitle("删除这个角标？").setMessage(items[i]).setPositiveButton("删除", new android.content.DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(android.content.DialogInterface dialogInterface, int w) {
                            com.cathelperawa.owo.app.ui.SendPanel.wxDropExtra(act, aid);
                            com.cathelperawa.owo.app.bridge.CatToast.show("已删除 " + aid);
                            com.cathelperawa.owo.app.ui.SendPanel.pickWxCard(act, cb);
                        }
                    }).setNegativeButton("取消", (android.content.DialogInterface.OnClickListener) null).show();
                    return true;
                }
            });
        }
    }

    static boolean wxIsExtra(android.content.Context context, java.lang.String str) {
        java.lang.String str2 = com.cathelperawa.owo.app.core.HookConfig.get(context, "wxmini_extra", "");
        for (java.lang.String part : str2.split(";")) {
            java.lang.String[] split = part.trim().split("\\|");
            if (split.length >= 1 && split[0].trim().equals(str)) {
                return true;
            }
        }
        return false;
    }

    static void wxDropExtra(android.content.Context context, java.lang.String str) {
        java.lang.String str2 = com.cathelperawa.owo.app.core.HookConfig.get(context, "wxmini_extra", "");
        java.lang.StringBuilder sb = new java.lang.StringBuilder();
        for (java.lang.String part : str2.split(";")) {
            java.lang.String trim = part.trim();
            if (trim.length() == 0) {
                continue;
            }
            java.lang.String[] split = trim.split("\\|");
            if (split.length >= 1 && split[0].trim().equals(str)) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(';');
            }
            sb.append(trim);
        }
        com.cathelperawa.owo.app.core.HookConfig.set(context, "wxmini_extra", sb.toString());
    }

    private static void miniToggle(android.app.Activity activity, android.widget.LinearLayout linearLayout, java.lang.String str, final boolean[] zArr, final float f) {
        android.widget.LinearLayout linearLayout2 = new android.widget.LinearLayout(activity);
        linearLayout2.setOrientation(0);
        linearLayout2.setGravity(16);
        int i = (int) (12.0f * f);
        int i2 = (int) (f * 10.0f);
        linearLayout2.setPadding(i, i2, i, i2);
        android.widget.TextView textView = new android.widget.TextView(activity);
        textView.setText(str);
        textView.setTextSize(13.0f);
        textView.setTextColor(C_TITLE);
        linearLayout2.addView(textView, new android.widget.LinearLayout.LayoutParams(0, -2, 1.0f));
        final android.widget.TextView textView2 = new android.widget.TextView(activity);
        textView2.setGravity(17);
        textView2.setTextSize(10.0f);
        textView2.setTextColor(-1);
        textView2.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        linearLayout2.addView(textView2, new android.widget.LinearLayout.LayoutParams((int) (46.0f * f), (int) (25.0f * f)));
        final java.lang.Runnable runnable = new java.lang.Runnable() {
            @Override
            public void run() {
                android.graphics.drawable.GradientDrawable gradientDrawable = new android.graphics.drawable.GradientDrawable();
                gradientDrawable.setCornerRadius(f * 13.0f);
                if (zArr[0]) {
                    gradientDrawable.setColor(-28738);
                    textView2.setText("开");
                } else {
                    gradientDrawable.setColor(-2563603);
                    textView2.setText("关");
                }
                textView2.setBackgroundDrawable(gradientDrawable);
            }
        };
        textView2.setOnClickListener(new android.view.View.OnClickListener() {
            @Override
            public void onClick(android.view.View view) {
                zArr[0] = !zArr[0];
                runnable.run();
                if (com.cathelperawa.owo.app.ui.SendPanel.sMiniRefresh != null) {
                    com.cathelperawa.owo.app.ui.SendPanel.sMiniRefresh.run();
                }
            }
        });
        runnable.run();
        linearLayout.addView(linearLayout2);
    }

    private static void pickQAppId(final android.app.Activity activity, final com.cathelperawa.owo.app.ui.CatDialog.OnText onText) {
        final java.lang.String[] strArr = {"腾讯视频", "哔哩哔哩", "QQ安全中心", "腾讯客服", "订阅号", "老师助手", "火柴人神射手"};
        final java.lang.String[] strArr2 = {"1109840991", com.cathelperawa.owo.app.feature.ArkSigner.BILI_APPID, "1109907872", "1109048181", "1109786902", "1109896843", "1109836759"};
        activity.runOnUiThread(new java.lang.Runnable() {
            @Override
            public void run() {
                new android.app.AlertDialog.Builder(activity, 5).setTitle("选一个角标").setItems(strArr, new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface dialogInterface, int i) {
                        onText.run(strArr2[i]);
                    }
                }).setNegativeButton("取消", (android.content.DialogInterface.OnClickListener) null).show();
            }
        });
    }

    public static void showProbeResult(android.app.Activity activity, float f, java.lang.String str) {
        if (activity == null || activity.isFinishing()) {
            return;
        }
        android.widget.TextView textView = new android.widget.TextView(activity);
        if (str == null || str.length() == 0) {
            str = "（没有结果）";
        }
        textView.setText(str);
        textView.setTextSize(12.0f);
        textView.setTextColor(-13681070);
        textView.setTypeface(android.graphics.Typeface.MONOSPACE);
        textView.setTextIsSelectable(true);
        com.cathelperawa.owo.app.ui.CatDialog.custom(activity, "小程序 appid 探测结果", textView, "知道了", new java.lang.Runnable() {
            @Override
            public void run() {
            }
        });
    }

    private static android.widget.TextView miniPill(android.app.Activity activity, java.lang.String str, boolean z, float f) {
        android.widget.TextView textView = new android.widget.TextView(activity);
        textView.setText(str);
        textView.setTextSize(14.0f);
        textView.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        textView.setGravity(17);
        int i = (int) (12.0f * f);
        textView.setPadding(0, i, 0, i);
        android.graphics.drawable.GradientDrawable gradientDrawable = new android.graphics.drawable.GradientDrawable();
        gradientDrawable.setCornerRadius(f * 16.0f);
        if (z) {
            gradientDrawable.setColors(new int[]{-28738, -1019745});
            gradientDrawable.setOrientation(android.graphics.drawable.GradientDrawable.Orientation.LEFT_RIGHT);
            textView.setTextColor(-1);
        } else {
            gradientDrawable.setColor(-920070);
            textView.setTextColor(-9732455);
        }
        textView.setBackgroundDrawable(gradientDrawable);
        return textView;
    }

    static void openShareForm(final android.app.Activity activity) {
        final float f = activity.getResources().getDisplayMetrics().density;
        final android.app.Dialog dialog = new android.app.Dialog(activity, android.R.style.Theme_Translucent_NoTitleBar);
        android.widget.ScrollView scrollView = new android.widget.ScrollView(activity);
        scrollView.setVerticalScrollBarEnabled(false);
        android.graphics.drawable.GradientDrawable gradientDrawable = new android.graphics.drawable.GradientDrawable();
        gradientDrawable.setCornerRadius(24.0f * f);
        gradientDrawable.setColor(-131585);
        scrollView.setBackgroundDrawable(gradientDrawable);
        scrollView.setClipToOutline(true);
        android.widget.LinearLayout linearLayout = new android.widget.LinearLayout(activity);
        linearLayout.setOrientation(1);
        int i = (int) (20.0f * f);
        float f2 = 14.0f * f;
        int i2 = (int) f2;
        linearLayout.setPadding(i, (int) (16.0f * f), i, i2);
        android.widget.LinearLayout linearLayout2 = new android.widget.LinearLayout(activity);
        linearLayout2.setOrientation(0);
        linearLayout2.setGravity(16);
        android.widget.TextView textView = new android.widget.TextView(activity);
        textView.setText("‹");
        textView.setTextSize(22.0f);
        textView.setTextColor(-9530410);
        int i3 = (int) (8.0f * f);
        textView.setPadding(0, 0, i3, 0);
        textView.setOnClickListener(new android.view.View.OnClickListener() {
            @Override
            public void onClick(android.view.View view) {
                dialog.dismiss();
                com.cathelperawa.owo.app.ui.SendPanel.openMiniShare(activity);
            }
        });
        linearLayout2.addView(textView);
        android.widget.TextView textView2 = new android.widget.TextView(activity);
        textView2.setText("通用分享改写");
        textView2.setTextSize(18.0f);
        textView2.setTextColor(C_TITLE);
        textView2.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        linearLayout2.addView(textView2, new android.widget.LinearLayout.LayoutParams(0, -2, 1.0f));
        android.widget.TextView textView3 = new android.widget.TextView(activity);
        textView3.setText("×");
        textView3.setTextSize(22.0f);
        textView3.setTextColor(-5654840);
        textView3.setPadding(i3, 0, 0, 0);
        textView3.setOnClickListener(new android.view.View.OnClickListener() {
            @Override
            public void onClick(android.view.View view) {
                dialog.dismiss();
            }
        });
        linearLayout2.addView(textView3);
        linearLayout.addView(linearLayout2);
        android.widget.TextView textView4 = new android.widget.TextView(activity);
        textView4.setText("别的 App 分享到 QQ 时，把文案 / 图 / 跳转换成你的");
        textView4.setTextSize(11.0f);
        textView4.setTextColor(C_SUB);
        int i4 = (int) (2.0f * f);
        float f3 = f * 10.0f;
        int i5 = (int) f3;
        textView4.setPadding(0, i4, 0, i5);
        linearLayout.addView(textView4);
        boolean[] zArr = {com.cathelperawa.owo.app.core.HookConfig.getBoolean(activity, com.cathelperawa.owo.app.feature.ArkSigner.K_SHARE, false)};
        android.widget.LinearLayout linearLayout3 = new android.widget.LinearLayout(activity);
        linearLayout3.setOrientation(1);
        android.graphics.drawable.GradientDrawable gradientDrawable2 = new android.graphics.drawable.GradientDrawable();
        gradientDrawable2.setCornerRadius(f2);
        gradientDrawable2.setColor(-657156);
        linearLayout3.setBackgroundDrawable(gradientDrawable2);
        miniToggle(activity, linearLayout3, "启用改写", zArr, f);
        android.widget.LinearLayout.LayoutParams layoutParams = new android.widget.LinearLayout.LayoutParams(-1, -2);
        layoutParams.topMargin = (int) (4.0f * f);
        linearLayout.addView(linearLayout3, layoutParams);
        miniGroup(activity, linearLayout, "模板", f);
        int[] iArr = {0};
        try {
            iArr[0] = java.lang.Integer.parseInt(com.cathelperawa.owo.app.core.HookConfig.get(activity, "share_tpl", com.cathelperawa.owo.app.bridge.GhostChat.GHOST_UID));
        } catch (java.lang.Throwable th) {
        }
        android.widget.LinearLayout linearLayout4 = new android.widget.LinearLayout(activity);
        linearLayout4.setOrientation(0);
        final android.widget.TextView[] textViewArr = new android.widget.TextView[3];
        java.lang.String[] strArr = {"图文卡", "纯图卡", "音乐卡"};
        int i6 = 0;
        while (i6 < 3) {
            android.widget.TextView textView5 = new android.widget.TextView(activity);
            textView5.setText(strArr[i6]);
            textView5.setTextSize(12.0f);
            textView5.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            textView5.setGravity(17);
            int i7 = (int) (9.0f * f);
            int i8 = i4;
            textView5.setPadding(0, i7, 0, i7);
            float f4 = f3;
            android.widget.ScrollView scrollView2 = scrollView;
            android.widget.LinearLayout.LayoutParams layoutParams2 = new android.widget.LinearLayout.LayoutParams(0, -2, 1.0f);
            if (i6 > 0) {
                layoutParams2.leftMargin = (int) (f * 6.0f);
            }
            linearLayout4.addView(textView5, layoutParams2);
            textViewArr[i6] = textView5;
            final int[] iArr2 = iArr;
            final int i9 = i6;
            textView5.setOnClickListener(new android.view.View.OnClickListener() {
                @Override
                public void onClick(android.view.View view) {
                    int[] iArr3 = iArr2;
                    int i10 = i9;
                    iArr3[0] = i10;
                    com.cathelperawa.owo.app.core.HookConfig.set(activity, "share_tpl", java.lang.String.valueOf(i10));
                    com.cathelperawa.owo.app.ui.SendPanel.paintTpl(textViewArr, i9, f);
                }
            });
            i6++;

            i4 = i8;
            f3 = f4;

        }
        int i10 = i4;
        final boolean[] zArr2 = zArr;
        final android.app.Dialog dialog2 = dialog;
        android.widget.ScrollView scrollView3 = scrollView;
        int i11 = i2;
        final int[] iArr3 = iArr;
        linearLayout.addView(linearLayout4);
        paintTpl(textViewArr, iArr3[0], f);
        miniGroup(activity, linearLayout, "文案", f);
        final android.widget.EditText miniEdit = miniEdit(activity, linearLayout, "标题", "卡片上那行大字，留空 = 不改", f, com.cathelperawa.owo.app.core.HookConfig.get(activity, com.cathelperawa.owo.app.feature.ArkSigner.K_SHARE_TITLE, ""));
        final android.widget.EditText miniEdit2 = miniEdit(activity, linearLayout, "描述", "灰色副标题，留空 = 不改", f, com.cathelperawa.owo.app.core.HookConfig.get(activity, com.cathelperawa.owo.app.feature.ArkSigner.K_SHARE_DESC, ""));
        miniGroup(activity, linearLayout, "封面图", f);
        final android.widget.EditText miniEdit3 = miniEdit(activity, linearLayout, "图片直链", "https://… 留空 = 用对方的图", f, com.cathelperawa.owo.app.core.HookConfig.get(activity, com.cathelperawa.owo.app.feature.ArkSigner.K_SHARE_PIC, ""));
        android.widget.TextView textView6 = new android.widget.TextView(activity);
        textView6.setText("选图并转成永久直链");
        textView6.setTextSize(11.0f);
        textView6.setTextColor(-9530410);
        textView6.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        textView6.setGravity(17);
        int i12 = (int) (7.0f * f);
        textView6.setPadding(i5, i12, i5, i12);
        android.graphics.drawable.GradientDrawable gradientDrawable3 = new android.graphics.drawable.GradientDrawable();
        gradientDrawable3.setCornerRadius(f3);
        gradientDrawable3.setColor(-1051396);
        gradientDrawable3.setStroke(java.lang.Math.max(1, (int) f), -2497293);
        textView6.setBackgroundDrawable(gradientDrawable3);
        android.widget.LinearLayout.LayoutParams layoutParams3 = new android.widget.LinearLayout.LayoutParams(-2, -2);
        layoutParams3.topMargin = i12;
        linearLayout.addView(textView6, layoutParams3);
        textView6.setOnClickListener(new com.cathelperawa.owo.app.ui.SendPanel.AnonymousClass65(activity, miniEdit3));
        final android.widget.LinearLayout linearLayout5 = new android.widget.LinearLayout(activity);
        linearLayout5.setOrientation(1);
        linearLayout.addView(linearLayout5);
        final android.widget.EditText miniEdit4 = miniEdit(activity, linearLayout5, "音频直链", "mp3/m4a 公网地址，音乐卡才用", f, com.cathelperawa.owo.app.core.HookConfig.get(activity, com.cathelperawa.owo.app.feature.ArkSigner.K_SHARE_MUSIC, ""));
        linearLayout5.setVisibility(iArr3[0] == 2 ? 0 : 8);
        miniGroup(activity, linearLayout, "点击跳转", f);
        final android.widget.EditText miniEdit5 = miniEdit(activity, linearLayout, "打开的网页", "https://… 留空 = 用对方的跳转", f, com.cathelperawa.owo.app.core.HookConfig.get(activity, com.cathelperawa.owo.app.feature.ArkSigner.K_SHARE_URL, ""));
        android.widget.TextView textView7 = new android.widget.TextView(activity);
        textView7.setText("卡片外壳（应用名/图标）仍是被分享那个 App 的，改不了");
        textView7.setTextSize(10.0f);
        textView7.setTextColor(-5654840);
        textView7.setPadding(0, i5, 0, i10);
        linearLayout.addView(textView7);
        final java.lang.Runnable runnable = new java.lang.Runnable() {
            @Override
            public void run() {
                linearLayout5.setVisibility(iArr3[0] != 2 ? 8 : 0);
            }
        };
        int i13 = 0;
        while (i13 < 3) {
            final int[] iArr4 = iArr3;
            final int i14 = i13;
            final android.widget.TextView[] textViewArr2 = textViewArr;
            textViewArr[i13].setOnClickListener(new android.view.View.OnClickListener() {
                @Override
                public void onClick(android.view.View view) {
                    int[] iArr5 = iArr4;
                    int i15 = i14;
                    iArr5[0] = i15;
                    com.cathelperawa.owo.app.core.HookConfig.set(activity, "share_tpl", java.lang.String.valueOf(i15));
                    com.cathelperawa.owo.app.ui.SendPanel.paintTpl(textViewArr2, i14, f);
                    runnable.run();
                }
            });
            i13++;
            linearLayout = linearLayout;
        }
        android.widget.LinearLayout linearLayout6 = linearLayout;
        android.widget.LinearLayout linearLayout7 = new android.widget.LinearLayout(activity);
        linearLayout7.setOrientation(0);
        linearLayout7.setPadding(0, i11, 0, 0);
        android.widget.TextView miniPill = miniPill(activity, "取消", false, f);
        miniPill.setOnClickListener(new android.view.View.OnClickListener() {
            @Override
            public void onClick(android.view.View view) {
                dialog2.dismiss();
            }
        });
        android.widget.LinearLayout.LayoutParams layoutParams4 = new android.widget.LinearLayout.LayoutParams(0, -2, 1.0f);
        int i15 = (int) (f * 6.0f);
        layoutParams4.rightMargin = i15;
        linearLayout7.addView(miniPill, layoutParams4);
        android.widget.TextView miniPill2 = miniPill(activity, "保存", true, f);
        miniPill2.setOnClickListener(new android.view.View.OnClickListener() {
            @Override
            public void onClick(android.view.View view) {
                com.cathelperawa.owo.app.core.HookConfig.set(activity, com.cathelperawa.owo.app.feature.ArkSigner.K_SHARE, zArr2[0] ? "1" : com.cathelperawa.owo.app.bridge.GhostChat.GHOST_UID);
                com.cathelperawa.owo.app.core.HookConfig.set(activity, com.cathelperawa.owo.app.feature.ArkSigner.K_SHARE_TITLE, com.cathelperawa.owo.app.ui.SendPanel.miniStr(miniEdit));
                com.cathelperawa.owo.app.core.HookConfig.set(activity, com.cathelperawa.owo.app.feature.ArkSigner.K_SHARE_DESC, com.cathelperawa.owo.app.ui.SendPanel.miniStr(miniEdit2));
                com.cathelperawa.owo.app.core.HookConfig.set(activity, com.cathelperawa.owo.app.feature.ArkSigner.K_SHARE_PIC, com.cathelperawa.owo.app.ui.SendPanel.miniStr(miniEdit3));
                com.cathelperawa.owo.app.core.HookConfig.set(activity, com.cathelperawa.owo.app.feature.ArkSigner.K_SHARE_URL, com.cathelperawa.owo.app.ui.SendPanel.miniStr(miniEdit5));
                com.cathelperawa.owo.app.core.HookConfig.set(activity, com.cathelperawa.owo.app.feature.ArkSigner.K_SHARE_MUSIC, com.cathelperawa.owo.app.ui.SendPanel.miniStr(miniEdit4));
                com.cathelperawa.owo.app.core.HookConfig.reload();
                dialog2.dismiss();
                com.cathelperawa.owo.app.bridge.CatToast.show("保存好了，去别的 App 分享试试");
            }
        });
        android.widget.LinearLayout.LayoutParams layoutParams5 = new android.widget.LinearLayout.LayoutParams(0, -2, 1.0f);
        layoutParams5.leftMargin = i15;
        linearLayout7.addView(miniPill2, layoutParams5);
        linearLayout6.addView(linearLayout7);
        scrollView3.addView(linearLayout6, new android.view.ViewGroup.LayoutParams(-1, -2));
        dialog2.setContentView(scrollView3, new android.view.ViewGroup.LayoutParams(-1, -2));
        android.view.Window window = dialog2.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(0));
            window.addFlags(2);
            android.view.WindowManager.LayoutParams attributes = window.getAttributes();
            attributes.dimAmount = 0.45f;
            window.setAttributes(attributes);
            window.setLayout((int) (activity.getResources().getDisplayMetrics().widthPixels * 0.92f), -2);
            window.setSoftInputMode(16);
        }
        dialog2.show();
    }

    static class AnonymousClass65 implements android.view.View.OnClickListener {
        final   android.app.Activity val$act;
        final   android.widget.EditText val$eP;

        AnonymousClass65(android.app.Activity activity, android.widget.EditText editText) {
            this.val$act = activity;
            this.val$eP = editText;
        }

        class AnonymousClass1 implements com.cathelperawa.owo.app.ui.PickerBridge.OnPick {
            AnonymousClass1() {
            }

            @Override
            public void run(java.lang.String str) {
                if (str == null || str.length() == 0) {
                    return;
                }
                com.cathelperawa.owo.app.bridge.CatToast.show("上传中，别退出…");
                com.cathelperawa.owo.app.feature.ArkSigner.hostImage(str, new com.cathelperawa.owo.app.feature.ArkSigner.HostCb() {
                    @Override
                    public void done(final java.lang.String str2, final java.lang.String str3) {
                        com.cathelperawa.owo.app.ui.SendPanel.AnonymousClass65.this.val$act.runOnUiThread(new java.lang.Runnable() {
                            @Override
                            public void run() {
                                if (str2 != null) {
                                    com.cathelperawa.owo.app.ui.SendPanel.AnonymousClass65.this.val$eP.setText(str2);
                                    com.cathelperawa.owo.app.bridge.CatToast.show("转好了，已填进图片直链");
                                } else {
                                    com.cathelperawa.owo.app.bridge.CatToast.show("图床失败：" + str3);
                                }
                            }
                        });
                    }
                });
            }
        }

        @Override
        public void onClick(android.view.View view) {
            com.cathelperawa.owo.app.ui.PickerBridge.pick(this.val$act, new com.cathelperawa.owo.app.ui.SendPanel.AnonymousClass65.AnonymousClass1());
        }
    }

    public static void paintTpl(android.widget.TextView[] textViewArr, int i, float f) {
        for (int i2 = 0; i2 < textViewArr.length; i2++) {
            android.graphics.drawable.GradientDrawable gradientDrawable = new android.graphics.drawable.GradientDrawable();
            gradientDrawable.setCornerRadius(11.0f * f);
            if (i2 == i) {
                gradientDrawable.setColors(new int[]{-28738, -1019745});
                gradientDrawable.setOrientation(android.graphics.drawable.GradientDrawable.Orientation.LEFT_RIGHT);
                textViewArr[i2].setTextColor(-1);
            } else {
                gradientDrawable.setColor(-920070);
                textViewArr[i2].setTextColor(-9732455);
            }
            textViewArr[i2].setBackgroundDrawable(gradientDrawable);
        }
    }

    private static void openTianxuanSign(android.app.Activity activity) {
        float f = activity.getResources().getDisplayMetrics().density;
        android.widget.LinearLayout linearLayout = new android.widget.LinearLayout(activity);
        linearLayout.setOrientation(1);
        int i = (int) (16.0f * f);
        int i2 = (int) (f * 12.0f);
        linearLayout.setPadding(i, i2, i, i2);
        android.widget.EditText editText = new android.widget.EditText(activity);
        editText.setText("com.tencent.structmsg");
        editText.setHint("appId");
        editText.setTextSize(13.0f);
        linearLayout.addView(editText, new android.widget.LinearLayout.LayoutParams(-1, -2));
        android.widget.EditText editText2 = new android.widget.EditText(activity);
        editText2.setText("adelie.agent_list");
        editText2.setHint("bizSrc");
        editText2.setTextSize(13.0f);
        linearLayout.addView(editText2, new android.widget.LinearLayout.LayoutParams(-1, -2));
        android.widget.EditText editText3 = new android.widget.EditText(activity);
        editText3.setHint("meta JSON，例如 {\"shareData\":{...}}");
        editText3.setTextSize(12.0f);
        editText3.setMinLines(6);
        editText3.setMaxLines(10);
        editText3.setInputType(655361);
        linearLayout.addView(editText3, new android.widget.LinearLayout.LayoutParams(-1, -2));
        com.cathelperawa.owo.app.ui.CatDialog.custom(activity, "天玄签任意 ark", linearLayout, "签名", new com.cathelperawa.owo.app.ui.SendPanel.AnonymousClass70(editText, editText2, editText3, activity));
    }

    static class AnonymousClass70 implements java.lang.Runnable {
        final   android.app.Activity val$act;
        final   android.widget.EditText val$eApp;
        final   android.widget.EditText val$eBiz;
        final   android.widget.EditText val$eMeta;

        AnonymousClass70(android.widget.EditText editText, android.widget.EditText editText2, android.widget.EditText editText3, android.app.Activity activity) {
            this.val$eApp = editText;
            this.val$eBiz = editText2;
            this.val$eMeta = editText3;
            this.val$act = activity;
        }

        @Override
        public void run() {
            java.lang.String trim = this.val$eApp.getText() == null ? "" : this.val$eApp.getText().toString().trim();
            java.lang.String trim2 = this.val$eBiz.getText() == null ? "" : this.val$eBiz.getText().toString().trim();
            java.lang.String trim3 = this.val$eMeta.getText() != null ? this.val$eMeta.getText().toString().trim() : "";
            if (trim.length() == 0 || trim3.length() == 0) {
                com.cathelperawa.owo.app.bridge.CatToast.show("appId 和 meta 不能为空");
            } else {
                com.cathelperawa.owo.app.bridge.CatToast.show("正在请求天玄签名…");
                com.cathelperawa.owo.app.core.Bg.run(new com.cathelperawa.owo.app.ui.SendPanel.AnonymousClass70.AnonymousClass1(trim, trim2, trim3), "tianxuan-sign");
            }
        }

        class AnonymousClass1 implements java.lang.Runnable {
            final   java.lang.String val$app;
            final   java.lang.String val$biz;
            final   java.lang.String val$meta;

            AnonymousClass1(java.lang.String str, java.lang.String str2, java.lang.String str3) {
                this.val$app = str;
                this.val$biz = str2;
                this.val$meta = str3;
            }

            @Override
            public void run() {
                try {
                    java.lang.String str = this.val$app;
                    final com.cathelperawa.owo.app.feature.ArkSigner.Result signCustomArkBiz = com.cathelperawa.owo.app.feature.ArkSigner.signCustomArkBiz(str, this.val$biz, str, this.val$meta);
                    com.cathelperawa.owo.app.ui.SendPanel.AnonymousClass70.this.val$act.runOnUiThread(new java.lang.Runnable() {
                        @Override
                        public void run() {
                            com.cathelperawa.owo.app.feature.ArkSigner.Result result = signCustomArkBiz;
                            if (result != null && result.json != null) {
                                android.widget.TextView textView = new android.widget.TextView(com.cathelperawa.owo.app.ui.SendPanel.AnonymousClass70.this.val$act);
                                textView.setText(signCustomArkBiz.json);
                                textView.setTextSize(11.0f);
                                textView.setTextIsSelectable(true);
                                com.cathelperawa.owo.app.ui.CatDialog.custom(com.cathelperawa.owo.app.ui.SendPanel.AnonymousClass70.this.val$act, "签名成功 · 长按复制", textView, "发送", new java.lang.Runnable() {
                                    @Override
                                    public void run() {
                                        com.cathelperawa.owo.app.ui.SendPanel.sendArkJson(com.cathelperawa.owo.app.ui.SendPanel.AnonymousClass70.this.val$act, signCustomArkBiz.json);
                                    }
                                });
                                return;
                            }
                            java.lang.StringBuilder append = new java.lang.StringBuilder().append("失败：");
                            com.cathelperawa.owo.app.feature.ArkSigner.Result result2 = signCustomArkBiz;
                            com.cathelperawa.owo.app.bridge.CatToast.show(append.append(result2 == null ? "null" : result2.error).toString());
                        }
                    });
                } catch (java.lang.Throwable th) {
                    com.cathelperawa.owo.app.ui.SendPanel.AnonymousClass70.this.val$act.runOnUiThread(new java.lang.Runnable() {
                        @Override
                        public void run() {
                            com.cathelperawa.owo.app.bridge.CatToast.show("异常：" + th);
                        }
                    });
                }
            }
        }
    }

    public static void sendArkJson(android.app.Activity activity, java.lang.String str) {
        try {
            java.lang.Object arkNative = com.cathelperawa.owo.app.bridge.ElementFactory.arkNative(str);
            if (arkNative == null) {
                com.cathelperawa.owo.app.bridge.CatToast.show("ark 元素构造失败");
                return;
            }
            java.lang.Object make = com.cathelperawa.owo.app.bridge.ContactFactory.make(com.cathelperawa.owo.app.bridge.CurrentChat.chatType(), com.cathelperawa.owo.app.bridge.CurrentChat.peerUid(), "");
            if (make == null) {
                com.cathelperawa.owo.app.bridge.CatToast.show("会话构造失败");
            } else {
                com.cathelperawa.owo.app.bridge.CatToast.show(com.cathelperawa.owo.app.bridge.MsgSender.send(make, java.util.Collections.singletonList(arkNative)) ? "已发出" : "发送失败");
            }
        } catch (java.lang.Throwable th) {
            com.cathelperawa.owo.app.bridge.CatToast.show("发送异常：" + th);
        }
    }

    public static java.lang.String miniStr(android.widget.EditText editText) {
        return (editText == null || editText.getText() == null) ? "" : editText.getText().toString().trim();
    }
}
