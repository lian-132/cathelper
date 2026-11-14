package com.cathelperawa.owo.app.bili;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.PlaybackParams;
import android.net.Uri;
import android.os.Build;
import android.view.Surface;
import android.view.TextureView;

import java.util.HashMap;
import java.util.Map;

public final class BiliPlayerView extends TextureView
        implements TextureView.SurfaceTextureListener,
        MediaPlayer.OnPreparedListener,
        MediaPlayer.OnCompletionListener,
        MediaPlayer.OnErrorListener {

    public interface Listener {
        void onPrepared(int width, int height, int durationMs);
        void onComplete();
        void onError(int what, int extra);
    }

    private MediaPlayer mp;
    private Surface surface;
    private String url;
    private Listener listener;

    private boolean surfaceReady = false;
    private boolean prepared = false;
    private boolean wantPlay = true;
    private int vw = 0, vh = 0;
    private int pendingSeekMs = -1;
    private float speed = 1.0f;

    public BiliPlayerView(Context c) {
        super(c);
        setSurfaceTextureListener(this);
        setOpaque(true);
    }

    public void setListener(Listener l) {
        listener = l;
    }

    public void setVideoUrl(String u) {
        url = u;
        if (surfaceReady) open();
    }

    public boolean isPrepared() {
        return prepared;
    }

    public boolean isPlaying() {
        try {
            return mp != null && prepared && mp.isPlaying();
        } catch (Throwable t) {
            return false;
        }
    }

    public int getDuration() {
        try {
            return mp != null && prepared ? mp.getDuration() : 0;
        } catch (Throwable t) {
            return 0;
        }
    }

    public int getCurrentPosition() {
        try {
            return mp != null && prepared ? mp.getCurrentPosition() : 0;
        } catch (Throwable t) {
            return 0;
        }
    }

    public void start() {
        wantPlay = true;
        try {
            if (mp != null && prepared) mp.start();
        } catch (Throwable ignore) {
        }
    }

    public void pause() {
        wantPlay = false;
        try {
            if (mp != null && prepared) mp.pause();
        } catch (Throwable ignore) {
        }
    }

    public void seekTo(int ms) {
        try {
            if (mp != null && prepared) {
                mp.seekTo(ms);
            } else {
                pendingSeekMs = ms;
            }
        } catch (Throwable ignore) {
        }
    }

    public void setSpeed(float s) {
        speed = s;
        applySpeed();
    }

    private void applySpeed() {
        try {
            if (mp != null && prepared && Build.VERSION.SDK_INT >= 23) {
                PlaybackParams p = mp.getPlaybackParams();
                p.setSpeed(speed);
                p.setPitch(1f);
                mp.setPlaybackParams(p);
            }
        } catch (Throwable ignore) {
        }
    }

    public void release() {
        try {
            if (mp != null) {
                mp.reset();
                mp.release();
            }
        } catch (Throwable ignore) {
        }
        mp = null;
        prepared = false;
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture st, int width, int height) {
        surfaceReady = true;
        try {
            surface = new Surface(st);
        } catch (Throwable ignore) {
        }
        if (url != null) open();
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture st, int width, int height) {
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture st) {
        surfaceReady = false;
        release();
        try {
            if (surface != null) surface.release();
        } catch (Throwable ignore) {
        }
        surface = null;
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture st) {
    }

    private void open() {
        release();
        if (url == null || url.length() == 0 || surface == null) return;
        try {
            mp = new MediaPlayer();
            mp.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                    .build());
            Map<String, String> headers = new HashMap<String, String>();
            headers.put("User-Agent", BiliApi.UA);
            headers.put("Referer", BiliApi.REFERER);
            mp.setDataSource(getContext(), Uri.parse(url), headers);
            mp.setSurface(surface);
            mp.setOnPreparedListener(this);
            mp.setOnCompletionListener(this);
            mp.setOnErrorListener(this);
            mp.prepareAsync();
        } catch (Throwable t) {
            if (listener != null) listener.onError(-1, -1);
        }
    }

    @Override
    public void onPrepared(MediaPlayer player) {
        prepared = true;
        vw = player.getVideoWidth();
        vh = player.getVideoHeight();
        requestLayout();
        applySpeed();
        if (pendingSeekMs > 0) {
            player.seekTo(pendingSeekMs);
            pendingSeekMs = -1;
        }
        if (wantPlay) {
            try {
                player.start();
            } catch (Throwable ignore) {
            }
        }
        if (listener != null) listener.onPrepared(vw, vh, player.getDuration());
    }

    @Override
    public void onCompletion(MediaPlayer player) {
        if (listener != null) listener.onComplete();
    }

    @Override
    public boolean onError(MediaPlayer player, int what, int extra) {
        prepared = false;
        if (listener != null) listener.onError(what, extra);
        return true;
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int cw = MeasureSpec.getSize(widthSpec);
        int ch = MeasureSpec.getSize(heightSpec);
        boolean unbounded = MeasureSpec.getMode(heightSpec) == MeasureSpec.UNSPECIFIED;

        if (cw <= 0) {
            super.onMeasure(widthSpec, heightSpec);
            return;
        }
        if (vw <= 0 || vh <= 0) {
            int h = unbounded || ch <= 0 ? cw * 9 / 16 : Math.min(cw * 9 / 16, ch);
            setMeasuredDimension(cw, h);
            return;
        }
        float ratio = (float) vw / vh;
        if (unbounded || ch <= 0) {
            setMeasuredDimension(cw, (int) (cw / ratio + 0.5f));
            return;
        }
        float scale = Math.min((float) cw / vw, (float) ch / vh);
        setMeasuredDimension(Math.max(1, (int) (vw * scale + 0.5f)),
                Math.max(1, (int) (vh * scale + 0.5f)));
    }
}
