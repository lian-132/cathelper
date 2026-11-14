package com.cathelperawa.owo.app.script;

import android.app.Activity;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.text.method.ScrollingMovementMethod;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public final class ScriptConsolePanel {

    private static final int MAX_BUFFER = 20000;

    private static ScriptConsolePanel sCurrent;

    public static boolean isOpen() {
        return sCurrent != null;
    }

    public static boolean closeCurrent() {
        ScriptConsolePanel p = sCurrent;
        if (p == null) return false;
        p.dismiss();
        return true;
    }

    private final String scriptName;
    private final StringBuilder buffer = new StringBuilder();
    private final Handler handler = new Handler(Looper.getMainLooper());

    private TextView tvOutput;
    private EditText etInput;
    private ScrollView svOut;

    private final Activity activity;

    public ScriptConsolePanel(Activity a, String script) {
        this(a, script, null);
    }

    public ScriptConsolePanel(Activity a, String script, String preload) {
        this.activity = a;
        this.scriptName = script == null ? "" : script;
        build();
        append("猫娘助手 · 脚本控制台\n");
        append("脚本：" + scriptName + "\n");
        append("在这里敲 bsh，回车执行 —— 能看到它自己的变量和函数\n");
        append("例：ctx.dataDir()  /  ctx.fileList(\"\")  /  ctx.get(\"kw_早\")\n");
        append("──────────────────────────────\n");
        if (preload != null && preload.length() > 0) {
            append(preload);
            append("\n──────────────────────────────\n");
        }
    }

    public void show() {
        try {
            ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(-1, -1);
            activity.addContentView(getRootView(), lp);
            sCurrent = this;

            View.OnKeyListener backKey = new View.OnKeyListener() {
                @Override public boolean onKey(View v, int keyCode, android.view.KeyEvent e) {
                    if (keyCode == android.view.KeyEvent.KEYCODE_BACK
                            && e.getAction() == android.view.KeyEvent.ACTION_UP) {
                        dismiss();
                        return true;
                    }
                    return false;
                }
            };
            panelRoot.setFocusableInTouchMode(true);
            panelRoot.setOnKeyListener(backKey);
            panelRoot.requestFocus();
            if (etInput != null) etInput.setOnKeyListener(backKey);
        } catch (Throwable t) {
            com.cathelperawa.owo.app.core.HookLog.e("[控制台] 打开失败", t);
        }
    }

    public void dismiss() {
        try {
            android.view.ViewParent p = getRootView().getParent();
            if (p instanceof ViewGroup) ((ViewGroup) p).removeView(getRootView());
        } catch (Throwable ignore) {
        }
        if (sCurrent == this) sCurrent = null;
    }

    private View panelRoot;

    private View getRootView() {
        return panelRoot;
    }

    private void build() {
        FrameLayout page = new FrameLayout(activity);
        page.setLayoutParams(new ViewGroup.LayoutParams(-1, -1));
        page.setBackgroundDrawable(new android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0xFF1A1533, 0xFF2A1F45}));
        try {
            java.io.InputStream in = activity.getAssets().open("console/assets/bg.jpg");
            android.graphics.Bitmap bmp = android.graphics.BitmapFactory.decodeStream(in);
            in.close();
            if (bmp != null) {
                android.widget.ImageView bgv = new android.widget.ImageView(activity);
                bgv.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);
                bgv.setImageBitmap(bmp);
                page.addView(bgv, new FrameLayout.LayoutParams(-1, -1));
            }
        } catch (Throwable ignore) {
        }
        View veil = new View(activity);
        veil.setBackgroundColor(0xC6161028);
        page.addView(veil, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutParams(new FrameLayout.LayoutParams(-1, -1));

        LinearLayout titleRow = new LinearLayout(activity);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        titleRow.setPadding(dp(16), dp(6), dp(16), dp(4));

        TextView close = new TextView(activity);
        close.setText("‹");
        close.setTextSize(26f);
        close.setTextColor(0xFFFFB6D5);
        close.setPadding(0, 0, dp(10), 0);
        close.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { dismiss(); }
        });
        titleRow.addView(close);

        TextView title = new TextView(activity);
        title.setText("控制台 · " + scriptName);
        title.setTextSize(17f);
        title.setTextColor(0xFFFFB6D5);
        title.setSingleLine(true);
        title.setEllipsize(android.text.TextUtils.TruncateAt.END);
        titleRow.addView(title, new LinearLayout.LayoutParams(0, -2, 1f));

        root.addView(titleRow);

        tvOutput = new TextView(activity);
        tvOutput.setTextColor(0xFFFFB6D5);
        tvOutput.setTextSize(14.5f);
        tvOutput.setLineSpacing(dp(3), 1f);
        tvOutput.setMovementMethod(ScrollingMovementMethod.getInstance());
        tvOutput.setPadding(dp(16), dp(6), dp(16), dp(12));
        tvOutput.setTextIsSelectable(true);

        svOut = new ScrollView(activity);
        svOut.addView(tvOutput);
        root.addView(svOut, new LinearLayout.LayoutParams(-1, 0, 1f));

        LinearLayout inputRow = new LinearLayout(activity);
        inputRow.setOrientation(LinearLayout.HORIZONTAL);
        inputRow.setGravity(Gravity.CENTER_VERTICAL);
        inputRow.setPadding(dp(14), dp(10), dp(14), dp(10));

        etInput = new EditText(activity);
        etInput.setTextColor(Color.WHITE);
        etInput.setHintTextColor(0x99FFB6D5);
        etInput.setHint("输入 bsh，回车执行…");
        etInput.setTextSize(15.5f);
        etInput.setSingleLine(true);
        etInput.setMinHeight(dp(48));
        android.graphics.drawable.GradientDrawable ebg =
                new android.graphics.drawable.GradientDrawable();
        ebg.setColor(0x66000000);
        ebg.setCornerRadius(dp(12));
        ebg.setStroke(dp(1), 0x66FFB6D5);
        etInput.setBackgroundDrawable(ebg);
        etInput.setPadding(dp(16), dp(11), dp(16), dp(11));
        etInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, android.view.KeyEvent e) {
                submitInput();
                return true;
            }
        });
        inputRow.addView(etInput, new LinearLayout.LayoutParams(0, -2, 1f));

        Button btnRun = termBtn("执行", true);
        btnRun.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { submitInput(); }
        });
        inputRow.addView(btnRun);

        Button btnClear = termBtn("清屏", false);
        btnClear.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                buffer.setLength(0);
                tvOutput.setText("");
            }
        });
        inputRow.addView(btnClear);

        root.addView(inputRow);
        page.addView(root);
        panelRoot = page;

        final LinearLayout contentRoot = root;
        try {
            if (android.os.Build.VERSION.SDK_INT >= 30) {
                page.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
                    @Override public android.view.WindowInsets onApplyWindowInsets(
                            View v, android.view.WindowInsets insets) {
                        int top = insets.getInsets(android.view.WindowInsets.Type.statusBars()).top;
                        int ime = insets.getInsets(android.view.WindowInsets.Type.ime()).bottom;
                        int nav = insets.getInsets(android.view.WindowInsets.Type.navigationBars()).bottom;
                        contentRoot.setPadding(0, top, 0, Math.max(ime, nav));
                        return insets;
                    }
                });
                page.requestApplyInsets();
            } else {
                contentRoot.setFitsSystemWindows(true);
            }
        } catch (Throwable ignore) {
        }
    }

    private Button termBtn(String label, boolean primary) {
        Button b = new Button(activity);
        b.setText(label);
        b.setTextSize(15f);
        b.setMinHeight(dp(46));
        b.setMinWidth(dp(62));
        b.setTextColor(primary ? 0xFF1A1533 : 0xFFFFB6D5);
        b.setAllCaps(false);
        android.graphics.drawable.GradientDrawable g =
                new android.graphics.drawable.GradientDrawable();
        g.setColor(primary ? 0xFFFFB6D5 : 0x33FFFFFF);
        g.setCornerRadius(dp(12));
        if (!primary) g.setStroke(dp(1), 0x66FFB6D5);
        b.setBackgroundDrawable(g);
        b.setPadding(dp(16), dp(9), dp(16), dp(9));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
        lp.leftMargin = dp(6);
        b.setLayoutParams(lp);
        return b;
    }

    private int dp(float v) {
        return com.cathelperawa.owo.app.ui.UiKit.dp(activity, v);
    }

    private void submitInput() {
        final String code = etInput.getText().toString();
        if (code == null || code.trim().length() == 0) return;
        etInput.setText("");
        append(">>> " + code + "\n");
        new Thread(new Runnable() {
            @Override public void run() {
                final String r = ScriptHost.evalIn(scriptName, code);
                handler.post(new Runnable() {
                    @Override public void run() {
                        append(r == null ? "（无返回值）" : r);
                        append("\n");
                    }
                });
            }
        }, "cat-script-console").start();
    }

    private void append(final String text) {
        if (text == null) return;
        buffer.append(text);
        if (buffer.length() > MAX_BUFFER) {
            buffer.delete(0, buffer.length() - MAX_BUFFER);
        }
        handler.post(new Runnable() {
            @Override public void run() {
                tvOutput.setText(buffer.toString());
                try {
                    svOut.post(new Runnable() {
                        @Override public void run() {
                            svOut.fullScroll(View.FOCUS_DOWN);
                        }
                    });
                } catch (Throwable ignore) {
                }
            }
        });
    }
}
