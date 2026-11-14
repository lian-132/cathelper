package com.cathelperawa.owo.app.ui;

import android.content.Context;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.cathelperawa.owo.app.proto.VoiceVault;

public final class VoicePlayerBar {

    private final Context ctx;
    private final float d;

    private final LinearLayout root;
    private final TextView playTv;
    private final TextView nameTv;
    private final TextView timeTv;
    private final SeekBar seek;

    private final Handler h = new Handler(Looper.getMainLooper());
    private boolean dragging;
    private boolean ticking;

    public VoicePlayerBar(Context c) {
        this.ctx = c;
        this.d = c.getResources().getDisplayMetrics().density;

        root = new LinearLayout(c);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(10), dp(8), dp(10), dp(6));
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(18));
        bg.setColor(0xFFFFF4F9);
        bg.setStroke(dp(1), 0xFFFFD3E7);
        root.setBackgroundDrawable(bg);
        root.setElevation(dp(1));
        root.setVisibility(View.GONE);

        LinearLayout head = new LinearLayout(c);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);

        playTv = new TextView(c);
        playTv.setText("❚❚");
        playTv.setTextSize(9);
        playTv.setGravity(Gravity.CENTER);
        playTv.setTextColor(0xFFFFFFFF);
        GradientDrawable pbg = new GradientDrawable();
        pbg.setShape(GradientDrawable.OVAL);
        pbg.setColor(0xFFF07BAE);
        playTv.setBackgroundDrawable(pbg);
        playTv.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (VoiceVault.isPlaying()) VoiceVault.pause();
                else VoiceVault.resume();
                sync();
            }
        });
        head.addView(playTv, new LinearLayout.LayoutParams(dp(28), dp(28)));

        nameTv = new TextView(c);
        nameTv.setTextSize(12);
        nameTv.setSingleLine(true);
        nameTv.setEllipsize(android.text.TextUtils.TruncateAt.END);
        nameTv.setTextColor(0xFF3C4E6B);
        nameTv.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams nlp = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        nlp.leftMargin = dp(8);
        head.addView(nameTv, nlp);

        timeTv = new TextView(c);
        timeTv.setTextSize(10.5f);
        timeTv.setTextColor(0xFF8A9CB5);
        head.addView(timeTv);

        TextView close = new TextView(c);
        close.setText("✕");
        close.setTextSize(12);
        close.setTextColor(0xFF9AAABF);
        close.setGravity(Gravity.CENTER);
        close.setPadding(dp(9), dp(4), dp(4), dp(4));
        close.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                VoiceVault.stop();
                sync();
            }
        });
        head.addView(close);

        root.addView(head, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        seek = new SeekBar(c);
        seek.setMax(1000);
        try {
            seek.setProgressDrawable(roundedProgress());
            GradientDrawable thumb = new GradientDrawable();
            thumb.setShape(GradientDrawable.OVAL);
            thumb.setColor(0xFFF07BAE);
            thumb.setStroke(dp(2), 0xFFFFFFFF);
            thumb.setSize(dp(15), dp(15));
            seek.setThumb(thumb);
            seek.setThumbOffset(0);
            seek.setSplitTrack(false);
            seek.setPadding(dp(8), 0, dp(8), 0);
            try {
                seek.setMaxHeight(dp(9));
                seek.setMinHeight(dp(9));
            } catch (Throwable ignore) {
            }
        } catch (Throwable ignore) {
        }
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar sb, int p, boolean fromUser) {
                if (fromUser) timeTv.setText(fmt(p * lenOf() / 1000) + " / " + fmt(lenOf()));
            }

            @Override public void onStartTrackingTouch(SeekBar sb) { dragging = true; }

            @Override public void onStopTrackingTouch(SeekBar sb) {
                dragging = false;
                VoiceVault.seek(sb.getProgress() * lenOf() / 1000);
            }
        });
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(24));
        slp.topMargin = dp(2);
        root.addView(seek, slp);

        start();
    }

    private Drawable roundedProgress() {
        GradientDrawable track = new GradientDrawable();
        track.setCornerRadius(dp(5));
        track.setColor(0xFFF3E3EC);
        track.setSize(dp(200), dp(8));

        GradientDrawable fill = new GradientDrawable();
        fill.setCornerRadius(dp(5));
        fill.setColor(0xFFF07BAE);
        fill.setSize(dp(200), dp(8));

        ClipDrawable clip = new ClipDrawable(fill, Gravity.LEFT, ClipDrawable.HORIZONTAL);
        LayerDrawable ld = new LayerDrawable(new Drawable[]{track, clip});
        ld.setId(0, android.R.id.background);
        ld.setId(1, android.R.id.progress);
        return ld;
    }

    public View view() {
        return root;
    }

    private int lenOf() {
        int len = VoiceVault.lengthMs();
        return len > 0 ? len : 1;
    }

    private void start() {
        if (ticking) return;
        ticking = true;
        h.post(tick);
    }

    public void attach() {
        start();
        sync();
    }

    public void detach() {
        ticking = false;
        h.removeCallbacks(tick);
    }

    private final Runnable tick = new Runnable() {
        @Override public void run() {
            if (!ticking) return;
            sync();
            h.postDelayed(this, 500);
        }
    };

    public void sync() {
        if (!VoiceVault.hasPlayer()) {
            root.setVisibility(View.GONE);
            return;
        }
        root.setVisibility(View.VISIBLE);
        String t = VoiceVault.playingTitle();
        if (t != null && t.length() > 0) nameTv.setText(t);
        boolean playing = VoiceVault.isPlaying();
        playTv.setText(playing ? "❚❚" : "▶");

        int len = lenOf();
        int pos = VoiceVault.positionMs();
        if (!dragging) {
            seek.setProgress((int) Math.min(1000L, pos * 1000L / len));
            timeTv.setText(fmt(pos) + " / " + fmt(len));
        }
    }

    private static String fmt(int ms) {
        if (ms < 0) ms = 0;
        int s = (ms + 500) / 1000;
        int m = s / 60;
        int r = s % 60;
        return m + ":" + (r < 10 ? "0" : "") + r;
    }

    private int dp(float v) {
        return (int) (v * d + 0.5f);
    }
}
