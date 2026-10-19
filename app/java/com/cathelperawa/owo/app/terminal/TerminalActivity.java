package com.cathelperawa.owo.app.terminal;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.text.method.ScrollingMovementMethod;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.cathelperawa.owo.app.AppConfig;

import java.io.File;
import java.util.Locale;

public class TerminalActivity extends Activity
        implements TerminalSession.OutputListener {

    private static final int MAX_BUFFER = 20_000;

    private TerminalSession session;
    private TextView tvOutput;
    private EditText etInput;
    private final StringBuilder buffer = new StringBuilder();
    private final Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        applyStyle();
        buildUi();
        session = TerminalSession.open(getFilesDir(), this);
    }

    private void applyStyle() {
        String style = getSharedPreferences(AppConfig.PREF, MODE_PRIVATE)
                .getString("uiStyle", "深色（默认）");
        boolean dark = !"浅色".equals(style);
        getWindow().getDecorView().setBackgroundColor(dark ? 0xFF0D1117 : 0xFFFFFFFF);
    }

    private void buildUi() {

        android.widget.FrameLayout page = new android.widget.FrameLayout(this);
        page.setLayoutParams(new ViewGroup.LayoutParams(-1, -1));
        page.setBackgroundDrawable(new android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0xFF1A1533, 0xFF2A1F45}));
        try {
            java.io.InputStream in = getAssets().open("console/assets/bg.jpg");
            android.graphics.Bitmap bmp = android.graphics.BitmapFactory.decodeStream(in);
            in.close();
            if (bmp != null) {
                android.widget.ImageView bgv = new android.widget.ImageView(this);
                bgv.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);
                bgv.setImageBitmap(bmp);
                page.addView(bgv, new android.widget.FrameLayout.LayoutParams(-1, -1));
            }
        } catch (Throwable ignore) {
        }
        View veil = new View(this);
        veil.setBackgroundColor(0xC6161028);
        page.addView(veil, new android.widget.FrameLayout.LayoutParams(-1, -1));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutParams(new android.widget.FrameLayout.LayoutParams(-1, -1));

        tvOutput = new TextView(this);
        tvOutput.setTextColor(0xFFFFB6D5);
        tvOutput.setTextSize(14f);
        tvOutput.setTypeface(cuteTypeface());
        tvOutput.setLineSpacing(dp(3), 1f);
        tvOutput.setMovementMethod(ScrollingMovementMethod.getInstance());
        tvOutput.setPadding(24, 16, 24, 16);
        tvOutput.setHorizontallyScrolling(true);

        HorizontalScrollView hsv = new HorizontalScrollView(this);
        hsv.addView(tvOutput);
        ScrollView sv = new ScrollView(this);
        sv.addView(hsv);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, 0, 1f);
        sv.setLayoutParams(lp);
        root.addView(sv);

        LinearLayout inputRow = new LinearLayout(this);
        inputRow.setOrientation(LinearLayout.HORIZONTAL);
        inputRow.setGravity(Gravity.CENTER_VERTICAL);
        inputRow.setPadding(16, 8, 16, 16);

        etInput = new EditText(this);
        etInput.setTextColor(Color.WHITE);
        etInput.setHintTextColor(0x99FFB6D5);
        etInput.setHint("输入命令，回车执行…");
        etInput.setTypeface(cuteTypeface());
        etInput.setSingleLine(true);

        android.graphics.drawable.GradientDrawable ebg = new android.graphics.drawable.GradientDrawable();
        ebg.setColor(0x66000000);
        ebg.setCornerRadius(dp(12));
        ebg.setStroke(dp(1), 0x66FFB6D5);
        etInput.setBackgroundDrawable(ebg);
        etInput.setPadding(dp(12), dp(8), dp(12), dp(8));
        LinearLayout.LayoutParams lpEt = new LinearLayout.LayoutParams(0, -2, 1f);
        etInput.setLayoutParams(lpEt);
        etInput.setOnEditorActionListener(new android.widget.TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(android.widget.TextView v, int actionId, android.view.KeyEvent event) {
                submitInput();
                return true;
            }
        });
        inputRow.addView(etInput);

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
        setContentView(page);
    }

    private android.graphics.Typeface cuteTypeface() {
        return android.graphics.Typeface.DEFAULT;
    }

    private Button termBtn(String label, boolean primary) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextSize(14);
        b.setTypeface(cuteTypeface());
        b.setTextColor(primary ? 0xFF1A1533 : 0xFFFFB6D5);
        b.setAllCaps(false);
        android.graphics.drawable.GradientDrawable g = new android.graphics.drawable.GradientDrawable();
        g.setColor(primary ? 0xFFFFB6D5 : 0x33FFFFFF);
        g.setCornerRadius(dp(12));
        if (!primary) g.setStroke(dp(1), 0x66FFB6D5);
        b.setBackgroundDrawable(g);
        b.setPadding(dp(14), dp(6), dp(14), dp(6));
        android.widget.LinearLayout.LayoutParams lp = new android.widget.LinearLayout.LayoutParams(
                -2, -2);
        lp.leftMargin = dp(8);
        b.setLayoutParams(lp);
        return b;
    }

    private int dp(float v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void submitInput() {
        String cmd = etInput.getText().toString();
        if (cmd.isEmpty()) return;
        etInput.setText("");
        append("$ " + cmd + "\n");
        session.write(cmd);
    }

    @Override
    public void onOutput(String text) {
        final String t = text;
        handler.post(new Runnable() {
            @Override public void run() { append(t); }
        });
    }

    @Override
    public void onExit(int code) {
        final String msg = String.format(Locale.US, "# 进程退出 code=%d%n", code);
        handler.post(new Runnable() {
            @Override public void run() { append(msg); }
        });
    }

    private void append(String s) {
        buffer.append(s);
        if (buffer.length() > MAX_BUFFER) {
            buffer.delete(0, buffer.length() - MAX_BUFFER);
        }
        tvOutput.setText(buffer.toString());

        tvOutput.post(new Runnable() {
            @Override public void run() {
                ScrollView sv = (ScrollView) tvOutput.getParent().getParent();
                sv.fullScroll(View.FOCUS_DOWN);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (session != null) session.destroy();
    }
}
