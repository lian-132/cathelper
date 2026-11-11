package com.cathelperawa.owo.app.bridge;

import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;

import com.cathelperawa.owo.app.MsgStore;
import com.cathelperawa.owo.app.core.HookLog;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.ByteBuffer;

public final class VoiceSilk {

    private static final byte[] SILK_HEADER = {
            0x02, 0x23, 0x21, 0x53, 0x49, 0x4C, 0x4B, 0x5F, 0x56, 0x33
    };

    private static final int SAMPLE_RATE = 16000;

    private static final int FRAME = 640;

    private static final String TTS_URL = "https://fanyi.baidu.com/gettts?lan=zh&text=";

    private VoiceSilk() {}

    public static String text2Silk(Context ctx, String text, String outPath) {
        File mp3 = null;
        try {
            String url = TTS_URL + URLEncoder.encode(text, "UTF-8") + "&spd=5&source=web";
            mp3 = new File(outPath + ".mp3");
            if (!download(url, mp3)) {
                MsgStore.add("[VoiceSilk-W] TTS 下载失败");
                return null;
            }
            MsgStore.add("[VoiceSilk] TTS 已下载 " + mp3.length() + " 字节");

            byte[] pcm = mp3ToPcm(mp3);
            if (pcm == null || pcm.length == 0) {
                MsgStore.add("[VoiceSilk-W] mp3→PCM 失败");
                return null;
            }
            MsgStore.add("[VoiceSilk] PCM " + pcm.length + " 字节（"
                    + (pcm.length * 1000 / 2 / SAMPLE_RATE) + "ms）");

            boolean ok = pcmToSilk(ctx, pcm, outPath);
            return ok ? outPath : null;
        } catch (Throwable t) {
            HookLog.e("VoiceSilk.text2Silk 异常", t);
            MsgStore.add("[VoiceSilk-W] text2Silk 异常 " + t);
            return null;
        } finally {
            if (mp3 != null) try { mp3.delete(); } catch (Throwable ignore) {}
        }
    }

    

    private static boolean download(String urlStr, File out) {
        InputStream in = null;
        FileOutputStream fo = null;
        try {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(15000);
            conn.setRequestProperty("User-Agent",
                    "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36");
            conn.connect();
            if (conn.getResponseCode() != 200) {
                MsgStore.add("[VoiceSilk-W] TTS HTTP " + conn.getResponseCode());
                return false;
            }
            in = conn.getInputStream();
            fo = new FileOutputStream(out);
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) fo.write(buf, 0, n);
            fo.flush();
            return out.exists() && out.length() > 0;
        } catch (Throwable t) {
            HookLog.e("VoiceSilk.download 异常", t);
            MsgStore.add("[VoiceSilk-W] TTS 下载异常 " + t);
            return false;
        } finally {
            try { if (in != null) in.close(); } catch (Throwable ignore) {}
            try { if (fo != null) fo.close(); } catch (Throwable ignore) {}
        }
    }

    

    private static byte[] mp3ToPcm(File src) {
        MediaExtractor extractor = null;
        MediaCodec codec = null;
        try {
            extractor = new MediaExtractor();
            extractor.setDataSource(src.getAbsolutePath());

            int track = -1;
            MediaFormat format = null;
            for (int i = 0; i < extractor.getTrackCount(); i++) {
                MediaFormat f = extractor.getTrackFormat(i);
                String mime = f.getString(MediaFormat.KEY_MIME);
                if (mime != null && mime.startsWith("audio/")) {
                    track = i; format = f; break;
                }
            }
            if (track < 0 || format == null) return null;
            extractor.selectTrack(track);
            String mime = format.getString(MediaFormat.KEY_MIME);

            codec = MediaCodec.createDecoderByType(mime);
            codec.configure(format, null, null, 0);
            codec.start();

            ByteBuffer[] inBufs = codec.getInputBuffers();
            ByteBuffer[] outBufs = codec.getOutputBuffers();
            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            boolean inputDone = false;
            boolean outputDone = false;
            while (!outputDone) {
                if (!inputDone) {
                    int idx = codec.dequeueInputBuffer(10000);
                    if (idx >= 0) {
                        ByteBuffer buf = inBufs[idx];
                        int size = extractor.readSampleData(buf, 0);
                        if (size < 0) {
                            codec.queueInputBuffer(idx, 0, 0, 0,
                                    MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                            inputDone = true;
                        } else {
                            codec.queueInputBuffer(idx, 0, size, extractor.getSampleTime(), 0);
                            extractor.advance();
                        }
                    }
                }
                int idx = codec.dequeueOutputBuffer(info, 10000);
                if (idx >= 0) {
                    if (info.size > 0) {
                        ByteBuffer buf = outBufs[idx];
                        buf.position(info.offset);
                        buf.limit(info.offset + info.size);
                        byte[] chunk = new byte[info.size];
                        buf.get(chunk);
                        out.write(chunk);
                    }
                    codec.releaseOutputBuffer(idx, false);
                    if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) outputDone = true;
                }
            }
            return out.toByteArray();
        } catch (Throwable t) {
            HookLog.e("VoiceSilk.mp3ToPcm 异常", t);
            MsgStore.add("[VoiceSilk-W] mp3→PCM 异常 " + t);
            return null;
        } finally {
            try { if (codec != null) { codec.stop(); codec.release(); } } catch (Throwable ignore) {}
            try { if (extractor != null) extractor.release(); } catch (Throwable ignore) {}
        }
    }

    

    private static boolean pcmToSilk(Context ctx, byte[] pcm, String outPath) {
        Object codec = null;
        FileOutputStream fo = null;
        int frames = 0;
        int outBytes = 0;
        int zeroPad = 0;
        try {
            ClassLoader cl = ctx.getClassLoader();
            Class<?> clsSilk = Class.forName(
                    "com.tencent.mobileqq.utils.SilkCodecWrapper", true, cl);

            
            Constructor<?> ctor = clsSilk.getConstructor(Context.class, boolean.class);
            ctor.setAccessible(true);
            codec = ctor.newInstance(ctx, true);

            Method mB = clsSilk.getMethod("b", int.class, int.class, int.class);
            mB.setAccessible(true);
            mB.invoke(codec, SAMPLE_RATE, SAMPLE_RATE, 1);

            Method mEncode = clsSilk.getMethod("encode", long.class, byte[].class,
                    byte[].class, int.class);
            mEncode.setAccessible(true);

            Field fQ = findField(clsSilk, "q");     
            Field fO = findField(clsSilk, "o");     
            Field fH = findField(clsSilk, "h");     
            if (fQ == null || fH == null) {
                MsgStore.add("[VoiceSilk-W] 拿不到 q/h 字段");
                return false;
            }
            fQ.setAccessible(true);
            fH.setAccessible(true);
            if (fO != null) fO.setAccessible(true);

            long handle = fQ.getLong(codec);
            byte[] hBuf = (byte[]) fH.get(codec);
            int frameLen = (fO != null) ? fO.getInt(codec) : FRAME;
            if (frameLen <= 0) frameLen = FRAME;
            HookLog.i("VoiceSilk: encoder=" + handle + " frameLen=" + frameLen
                    + " hBuf=" + (hBuf == null ? "null" : hBuf.length));
            MsgStore.add("[VoiceSilk] native encoder=" + handle
                    + " 帧长=" + frameLen + " h缓冲=" + (hBuf == null ? "null" : hBuf.length));
            if (handle == 0 || hBuf == null) {
                MsgStore.add("[VoiceSilk-W] native 编码器未就绪（handle=0），so 未加载？");
                return false;
            }

            fo = new FileOutputStream(outPath);
            fo.write(SILK_HEADER);
            outBytes += SILK_HEADER.length;

            byte[] frame = new byte[frameLen];
            int off = 0;
            int total = pcm.length;

            while (off < total) {
                int n = Math.min(frameLen, total - off);
                java.util.Arrays.fill(frame, (byte) 0);          
                if (n > 0) System.arraycopy(pcm, off, frame, 0, n);
                zeroPad += frameLen - n;
                off += frameLen;

                int ns = (Integer) mEncode.invoke(codec, handle, frame, hBuf, frameLen);
                frames++;
                if (ns > 0) {

                    fo.write(ns & 0xFF);
                    fo.write((ns >> 8) & 0xFF);
                    fo.write(hBuf, 0, ns);
                    outBytes += 2 + ns;
                } else {
                    MsgStore.add("[VoiceSilk-W] 第 " + frames + " 帧 encode 返回 " + ns);
                }
            }
            fo.flush();

            MsgStore.add("[VoiceSilk] SILK 完成 " + outPath
                    + "\n  喂 PCM=" + total + " 字节 / " + frames + " 帧（补零 " + zeroPad + "）"
                    + "\n  写出=" + outBytes + " 字节 ⇒ 音频数据="
                    + (outBytes - SILK_HEADER.length) + " 字节"
                    + " 平均每帧=" + (frames > 0
                            ? ((outBytes - SILK_HEADER.length) / frames) : 0) + " 字节");

            if (outBytes <= SILK_HEADER.length) {
                MsgStore.add("[VoiceSilk-W] ★ 一个音频帧都没编出来");
                return false;
            }
            return true;
        } catch (Throwable t) {
            MsgStore.add("[VoiceSilk-W] pcmToSilk 异常 " + t);
            HookLog.e("VoiceSilk.pcmToSilk 异常", t);
            return false;
        } finally {

            
            
            try { if (fo != null) fo.close(); } catch (Throwable ignore) {}
            try {
                if (codec != null) {
                    Method mRel = codec.getClass().getMethod("release");
                    mRel.setAccessible(true);
                    mRel.invoke(codec);
                }
            } catch (Throwable ignore) {}
        }
    }

    private static Field findField(Class<?> c, String name) {
        while (c != null) {
            try { return c.getDeclaredField(name); }
            catch (Throwable ignore) { c = c.getSuperclass(); }
        }
        return null;
    }

    

    
    
    public static volatile int lastDurationMs = 0;

    public static String audioToSilk(Context ctx, File src, String outPath, int maxSeconds) {
        lastDurationMs = 0;
        try {
            if (src == null || !src.exists() || src.length() == 0) {
                MsgStore.add("[VoiceSilk-W] audioToSilk: 源文件不存在");
                return null;
            }
            
            if (maxSeconds <= 0) maxSeconds = 3600;
            byte[] pcm = decodeMono16k(src, maxSeconds);
            if (pcm == null || pcm.length < FRAME) {
                MsgStore.add("[VoiceSilk-W] audioToSilk: 解码结果太短 " +
                        (pcm == null ? "null" : pcm.length));
                return null;
            }
            MsgStore.add("[VoiceSilk] 音乐 PCM " + pcm.length + " 字节（约 "
                    + (pcm.length * 1000L / 2 / SAMPLE_RATE) + "ms）");
            lastDurationMs = (int) Math.min(Integer.MAX_VALUE,
                    pcm.length * 1000L / 2 / SAMPLE_RATE);
            if (!pcmToSilk(ctx, pcm, outPath)) return null;
            return outPath;
        } catch (Throwable t) {
            HookLog.e("VoiceSilk.audioToSilk 异常", t);
            MsgStore.add("[VoiceSilk-W] audioToSilk 异常 " + t);
            return null;
        }
    }

    private static byte[] decodeMono16k(File src, int maxSeconds) {
        MediaExtractor extractor = null;
        MediaCodec codec = null;
        try {
            extractor = new MediaExtractor();
            extractor.setDataSource(src.getAbsolutePath());

            int track = -1;
            MediaFormat format = null;
            for (int i = 0; i < extractor.getTrackCount(); i++) {
                MediaFormat f = extractor.getTrackFormat(i);
                String mime = f.getString(MediaFormat.KEY_MIME);
                if (mime != null && mime.startsWith("audio/")) {
                    track = i;
                    format = f;
                    break;
                }
            }
            if (track < 0 || format == null) return null;
            extractor.selectTrack(track);

            int srcRate = format.containsKey(MediaFormat.KEY_SAMPLE_RATE)
                    ? format.getInteger(MediaFormat.KEY_SAMPLE_RATE) : 44100;
            int channels = format.containsKey(MediaFormat.KEY_CHANNEL_COUNT)
                    ? format.getInteger(MediaFormat.KEY_CHANNEL_COUNT) : 2;
            boolean isFloat = false;
            try {
                if (format.containsKey("pcm-encoding")) {
                    int enc = format.getInteger("pcm-encoding");
                    isFloat = (enc == 4);   
                }
            } catch (Throwable ignore) {
            }
            if (srcRate <= 0) srcRate = 44100;
            if (channels <= 0) channels = 2;

            String mime = format.getString(MediaFormat.KEY_MIME);
            codec = MediaCodec.createDecoderByType(mime);
            codec.configure(format, null, null, 0);
            codec.start();

            ByteBuffer[] inBufs = codec.getInputBuffers();
            ByteBuffer[] outBufs = codec.getOutputBuffers();
            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            ByteArrayOutputStream raw = new ByteArrayOutputStream();

            long maxBytes = (long) maxSeconds * srcRate * channels * 2;
            boolean inputDone = false;
            boolean outputDone = false;
            while (!outputDone) {
                if (!inputDone) {
                    int idx = codec.dequeueInputBuffer(10000);
                    if (idx >= 0) {
                        ByteBuffer buf = inBufs[idx];
                        int size = extractor.readSampleData(buf, 0);
                        if (size < 0) {
                            codec.queueInputBuffer(idx, 0, 0, 0,
                                    MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                            inputDone = true;
                        } else {
                            codec.queueInputBuffer(idx, 0, size, extractor.getSampleTime(), 0);
                            extractor.advance();
                        }
                    }
                }
                int idx = codec.dequeueOutputBuffer(info, 10000);
                if (idx >= 0) {
                    if (info.size > 0) {
                        ByteBuffer buf = outBufs[idx];
                        byte[] chunk = new byte[info.size];
                        buf.position(info.offset);
                        buf.limit(info.offset + info.size);
                        buf.get(chunk);
                        raw.write(chunk);
                    }
                    codec.releaseOutputBuffer(idx, false);
                    if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) outputDone = true;
                    if (raw.size() >= maxBytes) outputDone = true;   
                }
            }
            return downmixAndResample(raw.toByteArray(), srcRate, channels, isFloat, maxSeconds);
        } catch (Throwable t) {
            HookLog.e("VoiceSilk.decodeMono16k 异常", t);
            return null;
        } finally {
            try { if (codec != null) { codec.stop(); codec.release(); } } catch (Throwable ignore) {}
            try { if (extractor != null) extractor.release(); } catch (Throwable ignore) {}
        }
    }

    private static byte[] downmixAndResample(byte[] raw, int srcRate, int channels,
                                             boolean isFloat, int maxSeconds) {
        if (raw == null || raw.length < 4) return null;

        int frameBytes = channels * (isFloat ? 4 : 2);
        if (frameBytes <= 0) return null;
        int frames = raw.length / frameBytes;
        if (frames <= 0) return null;

        float[] mono = new float[frames];
        java.nio.ByteBuffer bb = java.nio.ByteBuffer.wrap(raw).order(java.nio.ByteOrder.LITTLE_ENDIAN);
        for (int i = 0; i < frames; i++) {
            float sum = 0f;
            for (int ch = 0; ch < channels; ch++) {
                int pos = i * frameBytes + ch * (isFloat ? 4 : 2);
                if (pos + (isFloat ? 4 : 2) > raw.length) break;
                if (isFloat) sum += bb.getFloat(pos);
                else sum += bb.getShort(pos) / 32768f;
            }
            mono[i] = sum / channels;
        }

        int outFrames = (int) ((long) frames * SAMPLE_RATE / srcRate);
        int cap = maxSeconds * SAMPLE_RATE;
        if (outFrames > cap) outFrames = cap;
        if (outFrames <= 0) return null;

        byte[] out = new byte[outFrames * 2];
        java.nio.ByteBuffer ob = java.nio.ByteBuffer.wrap(out)
                .order(java.nio.ByteOrder.LITTLE_ENDIAN);
        double step = (double) srcRate / SAMPLE_RATE;
        for (int i = 0; i < outFrames; i++) {
            double pos = i * step;
            int i0 = (int) pos;
            int i1 = Math.min(i0 + 1, frames - 1);
            float frac = (float) (pos - i0);
            float v = mono[i0] * (1 - frac) + mono[i1] * frac;
            if (v > 1f) v = 1f;
            if (v < -1f) v = -1f;
            ob.putShort((short) (v * 32767));
        }
        return out;
    }
}
