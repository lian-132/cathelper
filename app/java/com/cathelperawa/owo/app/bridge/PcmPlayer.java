package com.cathelperawa.owo.app.bridge;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

public final class PcmPlayer {

    private PcmPlayer() {}

    private static AudioTrack track;
    private static int rate = 24000;
    private static int ch = 1;
    private static int totalFrames;

    public static boolean load(byte[] pcm, int sampleRate, int channels) {
        stop();
        if (pcm == null || pcm.length < 320) return false;
        try {
            rate = sampleRate > 0 ? sampleRate : 24000;
            ch = channels > 0 ? channels : 1;
            if (ch > 2) ch = 2;

            int channelMask = ch == 1
                    ? AudioFormat.CHANNEL_OUT_MONO : AudioFormat.CHANNEL_OUT_STEREO;
            int minBuf = AudioTrack.getMinBufferSize(rate, channelMask,
                    AudioFormat.ENCODING_PCM_16BIT);
            if (minBuf <= 0) minBuf = 4096;

            int size = Math.max(pcm.length, minBuf);
            int align = 2 * ch;
            size = size - (size % align);
            if (size < minBuf) size += align;

            AudioTrack t = new AudioTrack(AudioManager.STREAM_MUSIC, rate, channelMask,
                    AudioFormat.ENCODING_PCM_16BIT, size, AudioTrack.MODE_STATIC);
            if (t.getState() != AudioTrack.STATE_INITIALIZED) {
                try { t.release(); } catch (Throwable ignore) { }
                return false;
            }
            int w = t.write(pcm, 0, Math.min(pcm.length, size));
            if (w <= 0) {
                try { t.release(); } catch (Throwable ignore) { }
                return false;
            }
            totalFrames = w / (2 * ch);
            track = t;
            return true;
        } catch (Throwable tr) {
            return false;
        }
    }

    public static boolean isLoaded() {
        return track != null;
    }

    public static boolean play() {
        if (track == null) return false;
        try {
            track.play();
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    public static void pause() {
        try { if (track != null) track.pause(); } catch (Throwable ignore) { }
    }

    public static boolean isPlaying() {
        try {
            return track != null && track.getPlayState() == AudioTrack.PLAYSTATE_PLAYING;
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean finished() {
        try {
            return track != null && totalFrames > 0
                    && track.getPlaybackHeadPosition() >= totalFrames;
        } catch (Throwable t) {
            return false;
        }
    }

    public static int positionMs() {
        try {
            if (track == null) return 0;
            return track.getPlaybackHeadPosition() * 1000 / Math.max(1, rate);
        } catch (Throwable t) {
            return 0;
        }
    }

    public static int lengthMs() {
        return totalFrames * 1000 / Math.max(1, rate);
    }

    public static void seek(int ms) {
        if (track == null) return;
        try {
            int frame = (int) ((long) Math.max(0, ms) * rate / 1000L);
            if (frame > totalFrames) frame = totalFrames;
            boolean was = isPlaying();
            track.stop();
            track.setPlaybackHeadPosition(frame);
            if (was) track.play();
        } catch (Throwable ignore) {
        }
    }

    public static void stop() {
        if (track != null) {
            try { track.pause(); } catch (Throwable ignore) { }
            try { track.flush(); } catch (Throwable ignore) { }
            try { track.stop(); } catch (Throwable ignore) { }
            try { track.release(); } catch (Throwable ignore) { }
        }
        track = null;
        totalFrames = 0;
    }
}
