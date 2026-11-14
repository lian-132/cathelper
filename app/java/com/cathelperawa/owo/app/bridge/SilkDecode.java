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
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;

public final class SilkDecode {

    private static final byte[] SILK_HEADER = {
            0x02, 0x23, 0x21, 0x53, 0x49, 0x4C, 0x4B, 0x5F, 0x56, 0x33
    };

    private static final int SAMPLE_RATE = 16000;

    private static final int SAMPLE_RATE_SILK = 24000;

    private static final int FRAME_PCM = 640;

    private SilkDecode() {}

    public static boolean isSilk(byte[] b) {
        if (b == null || b.length < SILK_HEADER.length + 2) return false;
        for (int i = 0; i < SILK_HEADER.length; i++) {
            if (b[i] != SILK_HEADER[i]) return false;
        }
        return true;
    }

    public static String toMp3(Context ctx, byte[] data, String outPath) {
        FileOutputStream fo = null;
        try {
            if (data == null || data.length < 20) {
                MsgStore.add("[SilkDecode-W] 数据太少 " + (data == null ? "null" : data.length));
                return null;
            }
            byte[] pcm;
            int rate = SAMPLE_RATE;
            int ch = 1;
            if (isSilk(data)) {
                pcm = silkToPcm(ctx, data);
            } else {

                MsgStore.add("[SilkDecode] 不是 SILK 头，走系统解码器");
                Decoded d = decodeAny(ctx, data);
                if (d == null) {
                    MsgStore.add("[SilkDecode-W] 系统解码失败");
                    return null;
                }
                pcm = d.pcm;
                rate = d.rate;
                ch = d.channels;
            }
            if (pcm == null || pcm.length < 320) {
                MsgStore.add("[SilkDecode-W] PCM 为空");
                return null;
            }
            long ms = pcm.length * 1000L / 2 / Math.max(1, ch) / Math.max(1, rate);
            MsgStore.add("[SilkDecode] PCM " + pcm.length + " 字节（" + ms + "ms, "
                    + rate + "Hz/" + ch + "ch）");

            byte[] mp3 = pcmToMp3(pcm, rate, ch);
            if (mp3 == null || mp3.length == 0) {
                MsgStore.add("[SilkDecode-W] PCM→MP3 失败");
                return null;
            }
            fo = new FileOutputStream(outPath);
            fo.write(mp3);
            fo.flush();
            MsgStore.add("[SilkDecode] MP3 完成 " + mp3.length + " 字节 → " + outPath);
            return outPath;
        } catch (Throwable t) {
            HookLog.e("SilkDecode.toMp3 异常", t);
            MsgStore.add("[SilkDecode-W] 异常 " + t);
            return null;
        } finally {
            try { if (fo != null) fo.close(); } catch (Throwable ignore) {}
        }
    }

    public static byte[] silkToPcm(Context ctx, byte[] silk) {
        return silkToPcm0(ctx, silk);
    }

    public static Object[] anyToPcm(Context ctx, byte[] data) {
        if (isSilk(data)) {
            byte[] pcm = silkToPcm0(ctx, data);
            return pcm == null ? null : new Object[]{pcm, SAMPLE_RATE_SILK, 1};
        }
        Decoded d = decodeAny(ctx, data);
        return d == null ? null : new Object[]{d.pcm, d.rate, d.channels};
    }

    private static byte[] silkToPcm0(Context ctx, byte[] silk) {

        Object codec = null;
        try {
            ClassLoader cl = ctx.getClassLoader();
            Class<?> cls = Class.forName("com.tencent.mobileqq.utils.SilkCodecWrapper", true, cl);

            java.lang.reflect.Constructor<?> ctor =
                    cls.getConstructor(Context.class, boolean.class);
            ctor.setAccessible(true);
            codec = ctor.newInstance(ctx, Boolean.FALSE);

            Method mB = cls.getMethod("b", int.class, int.class, int.class);
            mB.setAccessible(true);
            mB.invoke(codec, Integer.valueOf(SAMPLE_RATE_SILK), Integer.valueOf(0),
                    Integer.valueOf(1));

            Field fQ = findField(cls, "q");
            if (fQ == null) {
                AiDebugLog("  · SILK: 拿不到 q 字段");
                return null;
            }
            fQ.setAccessible(true);
            long handle = fQ.getLong(codec);
            if (handle == 0) {
                AiDebugLog("  · SILK: 解码器句柄为 0（so 未加载？）");
                return null;
            }

            Method mDecode = cls.getMethod("decode", long.class, byte[].class,
                    byte[].class, int.class, int.class);
            mDecode.setAccessible(true);

            int framePcm = SAMPLE_RATE_SILK * 20 * 2 / 1000;
            byte[] out = new byte[framePcm];

            java.io.ByteArrayOutputStream pcm =
                    new java.io.ByteArrayOutputStream(silk.length * 8);
            int off = SILK_HEADER.length;
            int frames = 0, bad = 0;
            while (off + 2 <= silk.length) {
                int len = (silk[off] & 0xFF) | ((silk[off + 1] & 0xFF) << 8);
                off += 2;
                if (len <= 0 || off + len > silk.length) break;
                byte[] payload = new byte[len];
                System.arraycopy(silk, off, payload, 0, len);
                off += len;

                java.util.Arrays.fill(out, (byte) 0);
                int ns;
                try {
                    ns = (Integer) mDecode.invoke(codec, Long.valueOf(handle), payload,
                            out, Integer.valueOf(len), Integer.valueOf(framePcm));
                } catch (Throwable t) {
                    ns = -1;
                }
                frames++;
                if (ns > 0) pcm.write(out, 0, Math.min(ns, out.length));
                else bad++;
            }
            AiDebugLog("  · SILK: 解码 " + frames + " 帧（失败 " + bad + "）→ "
                    + pcm.size() + "B PCM");
            if (frames == 0 || pcm.size() == 0) return null;
            return pcm.toByteArray();
        } catch (Throwable t) {
            HookLog.e("SilkDecode.silkToPcm 异常", t);
            AiDebugLog("  · SILK: 异常 " + t
                    + (t.getCause() == null ? "" : " / cause=" + t.getCause()));
            return null;
        } finally {

            try {
                if (codec != null) {
                    Method mRel = codec.getClass().getMethod("release");
                    mRel.setAccessible(true);
                    mRel.invoke(codec);
                }
            } catch (Throwable ignore) {}
        }
    }

    private static void loadWxAsrLib(Context ctx) {
        ClassLoader hostCl = ctx.getClassLoader();
        String[] libs = new String[]{"wx-asr-vad"};
        for (String lib : libs) {
            try {
                System.loadLibrary(lib);
                AiDebugLog("  · SILK: 已加载 lib" + lib);
                continue;
            } catch (Throwable t) {
                if (String.valueOf(t).contains("already loaded")) {
                    AiDebugLog("  · SILK: lib" + lib + " 已在内存");
                    continue;
                }
            }

            try {
                java.io.File dir = new java.io.File(ctx.getApplicationInfo().nativeLibraryDir);
                java.io.File so = new java.io.File(dir, "lib" + lib + ".so");
                if (!so.exists()) {
                    AiDebugLog("  · SILK: 找不到 " + so);
                    continue;
                }
                java.lang.reflect.Method m = java.lang.Runtime.class.getDeclaredMethod(
                        "nativeLoad", String.class, ClassLoader.class, String.class);
                m.setAccessible(true);
                Object r = m.invoke(Runtime.getRuntime(), so.getAbsolutePath(), hostCl, null);
                AiDebugLog("  · SILK: nativeLoad(宿主CL) " + lib + " → " + r);
            } catch (Throwable t2) {
                AiDebugLog("  · SILK: nativeLoad 失败 " + t2);
            }
        }
    }

    private static void AiDebugLog(String s) {
        try {
            com.cathelperawa.owo.app.ai.AiDebug.log(s);
        } catch (Throwable ignore) {
        }
    }

    private static int getStaticInt(Class<?> cls, String name) {
        try {
            Field f = cls.getDeclaredField(name);
            f.setAccessible(true);
            return f.getInt(null);
        } catch (Throwable t) {
            return -1;
        }
    }

    private static void setStaticInt(Class<?> cls, String name, int v) {
        try {
            Field f = cls.getDeclaredField(name);
            f.setAccessible(true);
            f.setInt(null, v);
        } catch (Throwable ignore) {
        }
    }

    public static final class Encoded {
        public byte[] data;
        public String ext = "mp3";
        public Encoded(byte[] d, String e) { data = d; ext = e; }
    }

    public static Encoded encode(byte[] pcm, int rate, int channels) {
        if (pcm == null || pcm.length < 64) return null;
        byte[] mp3 = encodeFrames("audio/mpeg", pcm, rate, channels, false);

        if (mp3 != null && mp3.length > 1024 && looksMp3(mp3)) return new Encoded(mp3, "mp3");
        byte[] aac = encodeFrames("audio/mp4a-latm", pcm, rate, channels, true);
        if (aac != null && aac.length > 1024 && aac.length > 9
                && (aac[0] & 0xFF) == 0xFF && (aac[1] & 0xF0) == 0xF0) {
            return new Encoded(aac, "aac");
        }
        return null;
    }

    private static boolean looksMp3(byte[] b) {
        if (b == null || b.length < 4) return false;
        if (b[0] == 'I' && b[1] == 'D' && b[2] == '3') return true;
        return (b[0] & 0xFF) == 0xFF && (b[1] & 0xE0) == 0xE0;
    }

    private static byte[] adtsHeader(int payload, int rate, int ch) {
        final int[] RATES = {96000, 88200, 64000, 48000, 44100, 32000, 24000,
                22050, 16000, 12000, 11025, 8000, 7350};
        int idx = 4;
        for (int i = 0; i < RATES.length; i++) {
            if (RATES[i] == rate) { idx = i; break; }
        }
        int profile = 1;
        int len = payload + 7;
        byte[] h = new byte[7];
        h[0] = (byte) 0xFF;
        h[1] = (byte) 0xF1;
        h[2] = (byte) ((profile << 6) | (idx << 2) | ((ch >> 2) & 0x01));
        h[3] = (byte) (((ch & 0x03) << 6) | ((len >> 11) & 0x03));
        h[4] = (byte) ((len >> 3) & 0xFF);
        h[5] = (byte) (((len & 0x07) << 5) | 0x1F);
        h[6] = (byte) 0xFC;
        return h;
    }

    private static byte[] pcmToMp3(byte[] pcm, int rate, int channels) {
        return encodeFrames("audio/mpeg", pcm, rate, channels, false);
    }

    private static byte[] encodeFrames(String mime, byte[] pcm, int rate, int channels,
                                       boolean adts) {
        MediaCodec codec = null;
        try {
            MediaFormat fmt = MediaFormat.createAudioFormat(mime, rate, channels);
            fmt.setInteger(MediaFormat.KEY_BIT_RATE, 32000);
            if (mime != null && mime.contains("mp4a")) {
                fmt.setInteger(MediaFormat.KEY_AAC_PROFILE, 2);
            }
            fmt.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384);

            codec = MediaCodec.createEncoderByType(mime);
            codec.configure(fmt, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            codec.start();

            ByteBuffer[] inBufs = codec.getInputBuffers();
            ByteBuffer[] outBufs = codec.getOutputBuffers();
            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            ByteArrayOutputStream out = new ByteArrayOutputStream(pcm.length / 4);

            int off = 0;
            boolean inputDone = false;
            boolean outputDone = false;
            long pts = 0;
            while (!outputDone) {
                if (!inputDone) {
                    int idx = codec.dequeueInputBuffer(10000);
                    if (idx >= 0) {
                        ByteBuffer buf = inBufs[idx];
                        buf.clear();
                        int n = Math.min(buf.capacity(), pcm.length - off);
                        if (n > 0) {
                            buf.put(pcm, off, n);
                            off += n;
                            codec.queueInputBuffer(idx, 0, n, pts, 0);
                            pts += n * 1000000L / 2 / Math.max(1, channels) / Math.max(1, rate);
                        } else {
                            codec.queueInputBuffer(idx, 0, 0, pts,
                                    MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                            inputDone = true;
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
                        if (adts) out.write(adtsHeader(chunk.length, rate, channels));
                        out.write(chunk);
                    }
                    codec.releaseOutputBuffer(idx, false);
                    if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) outputDone = true;
                }
            }
            return out.toByteArray();
        } catch (Throwable t) {
            HookLog.e("SilkDecode.pcmToMp3 异常", t);
            MsgStore.add("[SilkDecode-W] 音频编码异常 " + mime + " · " + t);
            return null;
        } finally {
            try { if (codec != null) { codec.stop(); codec.release(); } } catch (Throwable ignore) {}
        }
    }

    private static final class Decoded {
        byte[] pcm;
        int rate = 16000;
        int channels = 1;
    }

    private static Decoded decodeAny(Context ctx, byte[] data) {
        File tmp = null;
        MediaExtractor ex = null;
        MediaCodec codec = null;
        try {
            tmp = new File(ctx.getCacheDir(), "ptt_" + System.currentTimeMillis()
                    + (looksAmr(data) ? ".amr" : ".bin"));
            FileOutputStream fo = new FileOutputStream(tmp);
            fo.write(data);
            fo.close();

            ex = new MediaExtractor();
            ex.setDataSource(tmp.getAbsolutePath());

            int track = -1;
            MediaFormat fmt = null;
            for (int i = 0; i < ex.getTrackCount(); i++) {
                MediaFormat f = ex.getTrackFormat(i);
                String mime = f.getString(MediaFormat.KEY_MIME);
                if (mime != null && mime.startsWith("audio/")) {
                    track = i;
                    fmt = f;
                    break;
                }
            }
            if (track < 0 || fmt == null) {
                MsgStore.add("[SilkDecode-W] 没有音频轨道");
                return null;
            }
            ex.selectTrack(track);
            String mime = fmt.getString(MediaFormat.KEY_MIME);

            Decoded out = new Decoded();
            if (fmt.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                out.rate = fmt.getInteger(MediaFormat.KEY_SAMPLE_RATE);
            }
            if (fmt.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                out.channels = fmt.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
            }

            codec = MediaCodec.createDecoderByType(mime);
            codec.configure(fmt, null, null, 0);
            codec.start();

            ByteBuffer[] inBufs = codec.getInputBuffers();
            ByteBuffer[] outBufs = codec.getOutputBuffers();
            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            ByteArrayOutputStream bos = new ByteArrayOutputStream(data.length * 8);

            boolean inputDone = false;
            boolean outputDone = false;
            while (!outputDone) {
                if (!inputDone) {
                    int idx = codec.dequeueInputBuffer(10000);
                    if (idx >= 0) {
                        ByteBuffer buf = inBufs[idx];
                        buf.clear();
                        int n = ex.readSampleData(buf, 0);
                        if (n < 0) {
                            codec.queueInputBuffer(idx, 0, 0, 0,
                                    MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                            inputDone = true;
                        } else {
                            codec.queueInputBuffer(idx, 0, n, ex.getSampleTime(), 0);
                            ex.advance();
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
                        bos.write(chunk);
                    }
                    codec.releaseOutputBuffer(idx, false);
                    if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) outputDone = true;
                }
            }
            out.pcm = bos.toByteArray();
            MsgStore.add("[SilkDecode] 系统解码 " + mime + " → " + out.pcm.length + " 字节");
            return out;
        } catch (Throwable t) {
            HookLog.e("SilkDecode.decodeAny 异常", t);
            MsgStore.add("[SilkDecode-W] decodeAny 异常 " + t);
            return null;
        } finally {
            try { if (codec != null) { codec.stop(); codec.release(); } } catch (Throwable ignore) {}
            try { if (ex != null) ex.release(); } catch (Throwable ignore) {}
            try { if (tmp != null) tmp.delete(); } catch (Throwable ignore) {}
        }
    }

    private static boolean looksAmr(byte[] b) {
        return b != null && b.length > 6
                && b[0] == 0x23 && b[1] == 0x21 && b[2] == 0x41 && b[3] == 0x4D && b[4] == 0x52;
    }

    private static boolean silkAllowed() {
        try {

            return com.cathelperawa.owo.app.ai.AiConfig.pttAsr();
        } catch (Throwable t) {
            return false;
        }
    }

    private static Field findField(Class<?> c, String name) {
        while (c != null) {
            try { return c.getDeclaredField(name); }
            catch (Throwable ignore) { c = c.getSuperclass(); }
        }
        return null;
    }

    public static String probe(Context ctx) {
        try {
            Class<?> c = Class.forName("com.tencent.mobileqq.utils.SilkCodecWrapper",
                    false, ctx.getClassLoader());
            StringBuilder sb = new StringBuilder("SilkCodecWrapper ✓");
            for (Method m : c.getMethods()) {
                if (m.getName().equals("decode") || m.getName().equals("b")) {
                    sb.append("\n  ").append(m.getName()).append("(");
                    Class<?>[] p = m.getParameterTypes();
                    for (int i = 0; i < p.length; i++) {
                        if (i > 0) sb.append(", ");
                        sb.append(p[i].getSimpleName());
                    }
                    sb.append(")");
                }
            }
            return sb.toString();
        } catch (Throwable t) {
            return "SilkCodecWrapper ✗ " + t;
        }
    }

    public static long msOfPcm(int bytes) {
        return bytes <= 0 ? 0 : bytes * 1000L / 2 / SAMPLE_RATE;
    }

    public static File dummy() {
        return null;
    }
}
