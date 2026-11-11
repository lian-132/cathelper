package com.cathelperawa.owo.app;

import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

public class FloatWindowService extends Service {

    private static final String TAG = "CatHelperFloat";

    private WindowManager wm;
    private View ball;
    private WindowManager.LayoutParams lp;

    public static android.accessibilityservice.AccessibilityService a11yService;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        AppConfig.load(this);
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "请授予悬浮窗权限", Toast.LENGTH_SHORT).show();
            stopSelf();
            return;
        }
        createBall();
    }

    @Override
    public void onDestroy() {
        removeBall();
        super.onDestroy();
    }

    private void createBall() {
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);

        int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;

        lp = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.TOP | Gravity.START;
        lp.x = 40;
        lp.y = 300;

        float density = getResources().getDisplayMetrics().density;
        int ballDp = AppConfig.current != null ? AppConfig.current.floatSize : 34;
        if (ballDp < 20) ballDp = 20;
        if (ballDp > 110) ballDp = 110;
        lp.width = (int) (ballDp * density);
        lp.height = (int) (ballDp * density);

        TextView tv = new TextView(this);
        tv.setText("");
        
        String custom = getSharedPreferences(AppConfig.PREF, MODE_PRIVATE)
                .getString("customFloatIcon", "");
        android.graphics.drawable.Drawable d = null;
        if (custom != null && !custom.isEmpty()) {
            d = android.graphics.drawable.Drawable.createFromPath(custom);
        }
        if (d == null) d = getResources().getDrawable(R.drawable.ic_float_ball);
        tv.setBackground(d);
        ball = tv;

        ball.setOnTouchListener(new View.OnTouchListener() {
            private float downX, downY;
            private int startX, startY;
            private long downTime;
            private boolean moved;

            @Override
            public boolean onTouch(View v, MotionEvent ev) {
                switch (ev.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        downX = ev.getRawX();
                        downY = ev.getRawY();
                        startX = lp.x;
                        startY = lp.y;
                        downTime = System.currentTimeMillis();
                        moved = false;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        float dx = ev.getRawX() - downX;
                        float dy = ev.getRawY() - downY;
                        if (Math.abs(dx) > 12 || Math.abs(dy) > 12) moved = true;
                        lp.x = startX + (int) dx;
                        lp.y = startY + (int) dy;
                        wm.updateViewLayout(ball, lp);
                        return true;
                    case MotionEvent.ACTION_UP:
                        if (!moved && System.currentTimeMillis() - downTime < 400) {
                            doConvert();
                        }
                        return true;
                }
                return false;
            }
        });

        wm.addView(ball, lp);
        Log.d(TAG, "float ball created");
    }

    private void removeBall() {
        if (wm != null && ball != null) {
            try {
                wm.removeView(ball);
            } catch (Throwable ignored) {}
            ball = null;
        }
    }

    private void doConvert() {
        AppConfig cfg = AppConfig.current;
        if (cfg == null || !cfg.total) {
            Toast.makeText(this, "总开关未开启", Toast.LENGTH_SHORT).show();
            return;
        }
        if (a11yService instanceof QQAccessibilityService) {
            ((QQAccessibilityService) a11yService).manualConvert(cfg);
            Toast.makeText(this, "已转换", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "无障碍服务未运行", Toast.LENGTH_SHORT).show();
        }
    }
}
