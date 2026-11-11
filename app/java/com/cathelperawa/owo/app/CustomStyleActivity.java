package com.cathelperawa.owo.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class CustomStyleActivity extends Activity {

    private static final int REQ_BG = 1001;
    private static final int REQ_FLOAT = 1003;

    private static final int MAX_DIM = 2048;

    private ImageView ivBg, ivFloat;
    private TextView tvBgState, tvBgAlpha;
    private SeekBar sbBgAlpha;

    @Override
    protected void onResume() {
        super.onResume();
        applyPageStyle();
    }

    private void applyPageStyle() {
        View root = findViewById(R.id.root_custom);
        if (root == null) return;

        String style = prefs().getString("uiStyle", "深色（默认）");

        String bgPath = prefs().getString("customBg", "");
        boolean bgApplied = false;
        if (bgPath != null && !bgPath.isEmpty()) {
            android.graphics.drawable.Drawable d = android.graphics.drawable.Drawable.createFromPath(bgPath);
            if (d != null) {
                int alphaPct = prefs().getInt("bgAlpha", 100);
                if (alphaPct >= 100) {
                    root.setBackground(d);
                } else {
                    int overlayAlpha = (int) ((100 - alphaPct) / 100.0 * 255);
                    android.graphics.drawable.Drawable[] layers = new android.graphics.drawable.Drawable[]{
                            d,
                            new android.graphics.drawable.ColorDrawable(
                                    android.graphics.Color.argb(overlayAlpha, 15, 19, 26))
                    };
                    root.setBackground(new android.graphics.drawable.LayerDrawable(layers));
                }
                bgApplied = true;
            }
        }
        if (!bgApplied) {
            if ("深蓝".equals(style)) {
                root.setBackgroundColor(android.graphics.Color.parseColor("#0C1625"));
            } else {
                root.setBackgroundColor(android.graphics.Color.parseColor("#0F131A"));
            }
        }

        View topbar = findViewById(R.id.topbar_custom);
        if (topbar != null) {
            if ("深蓝".equals(style)) {
                topbar.setBackgroundColor(android.graphics.Color.parseColor("#8011233C"));
            } else {
                topbar.setBackgroundColor(android.graphics.Color.parseColor("#800F131A"));
            }
        }

        int pct = prefs().getInt("cardAlpha", 90);
        if (pct < 0) pct = 0;
        if (pct > 100) pct = 100;
        applyAlphaToCards(root, (int) (pct / 100.0 * 255));
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom_style);

        ivBg = findViewById(R.id.iv_bg_preview);
        ivFloat = findViewById(R.id.iv_float_preview);
        tvBgState = findViewById(R.id.tv_bg_state);
        tvBgAlpha = findViewById(R.id.tv_bg_alpha);
        sbBgAlpha = findViewById(R.id.sb_bg_alpha);

        findViewById(R.id.btn_back_custom).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { finish(); }
        });

        findViewById(R.id.btn_pick_bg).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { pickImage(REQ_BG); }
        });
        findViewById(R.id.btn_reset_bg).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                prefs().edit().remove("customBg").putInt("bgAlpha", 100).apply();
                refreshUi();
                Toast.makeText(CustomStyleActivity.this, "背景已恢复默认", Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.btn_pick_float).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { pickImage(REQ_FLOAT); }
        });
        findViewById(R.id.btn_reset_float).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                prefs().edit().remove("customFloatIcon").apply();
                refreshUi();
                restartFloat();
                Toast.makeText(CustomStyleActivity.this, "悬浮窗已恢复默认", Toast.LENGTH_SHORT).show();
            }
        });

        sbBgAlpha.setProgress(prefs().getInt("bgAlpha", 100));
        sbBgAlpha.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                tvBgAlpha.setText("背景不透明度：" + progress + "%");
                if (fromUser) {
                    prefs().edit().putInt("bgAlpha", progress).apply();
                    applyPageStyle();
                }
            }
            @Override public void onStartTrackingTouch(SeekBar sb) {}
            @Override public void onStopTrackingTouch(SeekBar sb) {}
        });

        final SeekBar sbCard = findViewById(R.id.sb_card_alpha);
        final TextView tvCard = findViewById(R.id.tv_card_alpha);
        int cardAlpha = prefs().getInt("cardAlpha", 90);
        sbCard.setProgress(cardAlpha);
        tvCard.setText("当前：" + cardAlpha + "%");
        sbCard.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                tvCard.setText("当前：" + progress + "%");
                if (fromUser) {
                    prefs().edit().putInt("cardAlpha", progress).apply();
                    applyPageStyle();
                }
            }
            @Override public void onStartTrackingTouch(SeekBar sb) {}
            @Override public void onStopTrackingTouch(SeekBar sb) {
                applyPageStyle();
            }
        });

        refreshUi();
    }

    private void applyCardAlpha(int pct) {
        if (pct < 0) pct = 0;
        if (pct > 100) pct = 100;
        int alpha = (int) (pct / 100.0 * 255);
        applyToCards(findViewById(android.R.id.content), alpha);
    }

    private void applyToCards(View v, int alpha) {
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
                applyToCards(g.getChildAt(i), alpha);
            }
        }
    }

    private android.content.SharedPreferences prefs() {
        return getSharedPreferences(AppConfig.PREF, MODE_PRIVATE);
    }

    private void pickImage(int req) {
        Intent it = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        it.addCategory(Intent.CATEGORY_OPENABLE);
        it.setType("image/*");
        try {
            startActivityForResult(it, req);
        } catch (Exception e) {
            
            try {
                Intent fb = new Intent(Intent.ACTION_GET_CONTENT);
                fb.setType("image/*");
                fb.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(Intent.createChooser(fb, "选择图片"), req);
            } catch (Exception e2) {
                Toast.makeText(this, "无法打开图片选择器", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();

        String key = (requestCode == REQ_BG) ? "customBg" : "customFloatIcon";
        
        boolean circle = (requestCode == REQ_FLOAT);
        String saved = importImage(uri, key, circle);
        if (saved == null) {
            Toast.makeText(this, "导入失败，请换一张图试试", Toast.LENGTH_SHORT).show();
            return;
        }
        prefs().edit().putString(key, saved).apply();
        refreshUi();
        if (requestCode == REQ_FLOAT) restartFloat();
        Toast.makeText(this, "导入成功", Toast.LENGTH_SHORT).show();
    }

    private String importImage(Uri uri, String name, boolean circle) {
        InputStream in = null;
        InputStream in2 = null;
        FileOutputStream out = null;
        try {
            
            BitmapFactory.Options opt = new BitmapFactory.Options();
            opt.inJustDecodeBounds = true;
            in = getContentResolver().openInputStream(uri);
            if (in == null) return null;
            BitmapFactory.decodeStream(in, null, opt);
            in.close();
            in = null;

            int w = opt.outWidth;
            int h = opt.outHeight;
            if (w <= 0 || h <= 0) return null;

            int sample = 1;
            while ((w / sample) > MAX_DIM || (h / sample) > MAX_DIM) sample *= 2;

            BitmapFactory.Options opt2 = new BitmapFactory.Options();
            opt2.inSampleSize = sample;
            opt2.inPreferredConfig = Bitmap.Config.ARGB_8888;
            in2 = getContentResolver().openInputStream(uri);
            if (in2 == null) return null;
            Bitmap bmp = BitmapFactory.decodeStream(in2, null, opt2);
            if (bmp == null) return null;

            if (circle) bmp = toCircle(bmp);

            File dir = new File(getFilesDir(), "custom");
            if (!dir.exists() && !dir.mkdirs()) return null;
            File f = new File(dir, name + ".png");
            out = new FileOutputStream(f);
            bmp.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            bmp.recycle();
            return f.getAbsolutePath();
        } catch (Throwable e) {
            return null;
        } finally {
            try { if (in != null) in.close(); } catch (Throwable ignored) {}
            try { if (in2 != null) in2.close(); } catch (Throwable ignored) {}
            try { if (out != null) out.close(); } catch (Throwable ignored) {}
        }
    }

    private Bitmap toCircle(Bitmap src) {
        int size = Math.min(src.getWidth(), src.getHeight());
        int x = (src.getWidth() - size) / 2;
        int y = (src.getHeight() - size) / 2;
        Bitmap square = Bitmap.createBitmap(src, x, y, size, size);
        if (square != src) src.recycle();

        Bitmap out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        android.graphics.Canvas c = new android.graphics.Canvas(out);
        android.graphics.Paint p = new android.graphics.Paint();
        p.setAntiAlias(true);
        p.setFilterBitmap(true);
        float r = size / 2f;
        c.drawCircle(r, r, r, p);
        p.setXfermode(new android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC_IN));
        c.drawBitmap(square, 0, 0, p);
        square.recycle();
        return out;
    }

    private void restartFloat() {
        try {
            Intent it = new Intent(this, FloatWindowService.class);
            stopService(it);
            startService(it);
        } catch (Throwable ignored) {}
    }

    private void refreshUi() {
        String bg = prefs().getString("customBg", "");
        if (bg != null && !bg.isEmpty()) {
            Drawable d = Drawable.createFromPath(bg);
            if (d != null) {
                ivBg.setImageDrawable(d);
                ivBg.setVisibility(View.VISIBLE);
                tvBgState.setText("当前：已自定义");
            }
        } else {
            ivBg.setVisibility(View.GONE);
            tvBgState.setText("当前：未设置");
        }
        int alpha = prefs().getInt("bgAlpha", 100);
        sbBgAlpha.setProgress(alpha);
        tvBgAlpha.setText("背景不透明度：" + alpha + "%");

        showThumb(ivFloat, prefs().getString("customFloatIcon", ""));
    }

    private void showThumb(ImageView iv, String path) {
        if (path != null && !path.isEmpty()) {
            Drawable d = Drawable.createFromPath(path);
            if (d != null) {
                iv.setImageDrawable(d);
                iv.setVisibility(View.VISIBLE);
                return;
            }
        }
        iv.setVisibility(View.GONE);
    }
}
