package com.cathelperawa.owo.app.proto;

import android.content.Context;
import com.cathelperawa.owo.app.core.Bg;
import android.media.MediaMetadataRetriever;

import com.cathelperawa.owo.app.MsgStore;
import com.cathelperawa.owo.app.api.IoApi;
import com.cathelperawa.owo.app.api.MsgApi;
import com.cathelperawa.owo.app.bridge.PcmPlayer;
import com.cathelperawa.owo.app.bridge.QQEnv;
import com.cathelperawa.owo.app.bridge.SilkDecode;
import com.cathelperawa.owo.app.bridge.VoiceSilk;
import com.cathelperawa.owo.app.core.HookLog;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class VoiceVault {

    private static final String K_FILE = "index.json";

    private static final long MAX_BYTES = 32L * 1024 * 1024;

    private VoiceVault() {}

    

    
    public static final class Item {
        public String id = "";
        public String name = "";
        
        public String ext = "silk";
        
        public long durMs;
        public long time;
        
        public String src = "";
        
        public String text = "";
        
        public boolean hasSilk;

        public long sizeBytes() {
            long n = 0;
            if (!"silk".equals(ext)) {
                File f = fileOf(this, ext);
                if (f != null && f.isFile()) n += f.length();
            }
            File s = silkFile(this);
            if (s != null) n += s.length();
            File m = mp3File(this);
            if (m != null) n += m.length();
            return n;
        }

        public String durText() {
            if (durMs <= 0) return "";
            long s = (durMs + 500) / 1000;
            if (s <= 0) s = 1;
            return (s / 60 > 0 ? (s / 60) + ":" : "") + (s % 60 < 10 && s / 60 > 0 ? "0" : "")
                    + (s % 60) + " 秒";
        }
    }

    

    
    public static File dir() {
        try {
            File d = IoApi.dataDir();
            if (d == null) return null;
            File v = new File(d, "voicevault");
            if (!v.exists()) v.mkdirs();
            return v;
        } catch (Throwable t) {
            return null;
        }
    }

    public static File fileOf(Item it, String ext) {
        if (it == null || it.id.length() == 0) return null;
        File d = dir();
        if (d == null) return null;
        return new File(d, it.id + "." + (ext == null || ext.length() == 0 ? "bin" : ext));
    }

    public static File silkFile(Item it) {
        File f = fileOf(it, "silk");
        return f != null && f.isFile() && f.length() > 0 ? f : null;
    }

    public static File mp3File(Item it) {
        File f = fileOf(it, "mp3");
        return f != null && f.isFile() && f.length() > 0 ? f : null;
    }

    public static File aacFile(Item it) {
        File f = fileOf(it, "aac");
        return f != null && f.isFile() && f.length() > 0 ? f : null;
    }

    public static File wavFile(Item it) {
        File f = fileOf(it, "wav");
        return f != null && f.isFile() && f.length() > 44 ? f : null;
    }

    public static File audioFile(Item it) {
        File f = mp3File(it);
        if (f != null && f.length() > 0) {
            if (looksAudio(f)) return f;
            
            try { f.delete(); } catch (Throwable ignore) { }
        }
        f = aacFile(it);
        if (f != null && f.length() > 0) {
            if (looksAudio(f)) return f;
            try { f.delete(); } catch (Throwable ignore) { }
        }
        f = wavFile(it);
        if (f != null && f.length() > 44) return f;
        return null;
    }

    private static boolean looksAudio(File f) {
        java.io.FileInputStream in = null;
        try {
            byte[] h = new byte[12];
            in = new java.io.FileInputStream(f);
            int n = in.read(h);
            if (n < 4) return false;
            if (h[0] == 'I' && h[1] == 'D' && h[2] == '3') return true;       
            if (h[0] == 'R' && h[1] == 'I' && h[2] == 'F' && h[3] == 'F') return true;  
            if ((h[0] & 0xFF) == 0xFF && (h[1] & 0xE0) == 0xE0) return true;  
            if ((h[0] & 0xFF) == 0xFF && (h[1] & 0xF0) == 0xF0) return true;  
            if (n >= 8 && h[4] == 'f' && h[5] == 't' && h[6] == 'y' && h[7] == 'p') return true;
            if (h[0] == 'O' && h[1] == 'g' && h[2] == 'g' && h[3] == 'S') return true;
            return false;
        } catch (Throwable t) {
            return false;
        } finally {
            try { if (in != null) in.close(); } catch (Throwable ignore) { }
        }
    }

    public static File rawFile(Item it) {
        File f = fileOf(it, it == null ? "bin" : it.ext);
        return f != null && f.isFile() ? f : null;
    }

    

    
    public static List<Item> list() {
        List<Item> out = new ArrayList<Item>();
        File d = dir();
        if (d == null) return out;
        File idx = new File(d, K_FILE);
        if (!idx.isFile()) return out;
        try {
            String raw = read(idx);
            if (raw == null || raw.trim().length() == 0) return out;
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o == null) continue;
                Item it = new Item();
                it.id = o.optString("id", "");
                it.name = o.optString("name", "语音");
                it.ext = o.optString("ext", "bin");
                it.durMs = o.optLong("dur", 0);
                it.time = o.optLong("time", 0);
                it.src = o.optString("src", "");
                it.text = o.optString("text", "");
                it.hasSilk = o.optBoolean("silk", false);
                if (it.id.length() == 0) continue;
                
                if (rawFile(it) == null && silkFile(it) == null && mp3File(it) == null) continue;
                if (silkFile(it) != null) it.hasSilk = true;
                out.add(it);
            }
        } catch (Throwable t) {
            HookLog.w("语音库索引读取失败: " + t);
        }
        Collections.sort(out, new Comparator<Item>() {
            @Override public int compare(Item a, Item b) {
                return Long.compare(b.time, a.time);
            }
        });
        return out;
    }

    private static void save(List<Item> lib) {
        File d = dir();
        if (d == null) return;
        try {
            JSONArray arr = new JSONArray();
            for (Item it : lib) {
                JSONObject o = new JSONObject();
                o.put("id", it.id);
                o.put("name", it.name);
                o.put("ext", it.ext);
                o.put("dur", it.durMs);
                o.put("time", it.time);
                o.put("src", it.src);
                o.put("text", it.text);
                o.put("silk", it.hasSilk || silkFile(it) != null);
                arr.put(o);
            }
            writeAtomic(new File(d, K_FILE), arr.toString().getBytes("UTF-8"));
        } catch (Throwable t) {
            HookLog.w("语音库索引写入失败: " + t);
        }
    }

    

    
    public static Item addBytes(Context c, String name, byte[] data, String src, long durHint) {
        return addBytesWithExt(c, name, data, src, durHint, null);
    }

    public static Item addBytesWithExt(Context c, String name, byte[] data, String src,
                                       long durHint, String extOverride) {
        if (data == null || data.length < 32) return null;
        if (data.length > MAX_BYTES) {
            MsgStore.add("[语音库-W] 太大，拒绝入库 " + (data.length / 1024 / 1024) + "MB");
            return null;
        }
        try {
            Item it = new Item();
            it.id = newId();
            it.time = System.currentTimeMillis();
            it.name = cleanName(name);
            it.src = src == null ? "" : src;
            boolean silk = SilkDecode.isSilk(data);
            it.ext = silk ? "silk"
                    : (extOverride != null && extOverride.length() > 0
                    ? extOverride : sniffExt(data));
            File f = fileOf(it, it.ext);
            if (f == null) return null;
            if (!writeAtomic(f, data)) return null;
            if (silk) {
                it.hasSilk = true;
                it.durMs = silkDurationMs(data);
            }
            if (it.durMs <= 0 && durHint > 0) it.durMs = durHint;
            if (it.durMs <= 0) it.durMs = probeDuration(f);
            if (it.durMs < 0) it.durMs = 0;

            List<Item> lib = list();
            lib.add(0, it);
            save(lib);
            MsgStore.add("[语音库] 入库 " + it.name + " · " + it.ext + " · "
                    + (data.length / 1024) + "KB" + (it.hasSilk ? "（腾讯语音）" : "（普通音频）"));
            return it;
        } catch (Throwable t) {
            HookLog.e("语音库 addBytes 失败", t);
            return null;
        }
    }

    public static Item addFile(Context c, String name, File src, String label) {
        try {
            if (src == null || !src.isFile() || src.length() < 32) return null;
            byte[] b = readBytes(src);
            if (b == null) return null;
            Item it = addBytes(c, name, b, label, 0);
            return it;
        } catch (Throwable t) {
            HookLog.e("语音库 addFile 失败", t);
            return null;
        }
    }

    public static boolean rename(Item it, String name) {
        if (it == null) return false;
        String n = cleanName(name);
        if (n.length() == 0) return false;
        List<Item> lib = list();
        for (Item x : lib) {
            if (x.id.equals(it.id)) {
                x.name = n;
                save(lib);
                it.name = n;
                return true;
            }
        }
        return false;
    }

    public static int remove(List<Item> items) {
        if (items == null || items.isEmpty()) return 0;
        List<Item> lib = list();
        int n = 0;
        for (Item it : items) {
            boolean hit = false;
            for (int i = lib.size() - 1; i >= 0; i--) {
                if (lib.get(i).id.equals(it.id)) {
                    lib.remove(i);
                    hit = true;
                }
            }
            if (!hit) continue;
            deleteQuiet(fileOf(it, it.ext));
            deleteQuiet(fileOf(it, "silk"));
            deleteQuiet(fileOf(it, "mp3"));
            n++;
        }
        save(lib);
        return n;
    }

    

    
    public static String ensureSilk(Item it) {
        if (it == null) return null;
        File have = silkFile(it);
        if (have != null) {
            it.hasSilk = true;
            return have.getAbsolutePath();
        }
        File src = rawFile(it);
        if (src == null) src = mp3File(it);
        if (src == null) return null;
        File out = fileOf(it, "silk");
        if (out == null) return null;
        try {
            Context c = QQEnv.appContext();
            if (c == null) return null;
            String p = VoiceSilk.audioToSilk(c, src, out.getAbsolutePath(), 0);
            if (p == null) {
                MsgStore.add("[语音库-W] 转腾讯语音失败 " + it.name);
                return null;
            }
            long dur = VoiceSilk.lastDurationMs;
            List<Item> lib = list();
            for (Item x : lib) {
                if (x.id.equals(it.id)) {
                    x.hasSilk = true;
                    if (dur > 0) x.durMs = dur;
                }
            }
            save(lib);
            it.hasSilk = true;
            if (dur > 0) it.durMs = dur;
            MsgStore.add("[语音库] 转腾讯语音完成 " + it.name + " · " + (dur / 1000) + " 秒");
            return out.getAbsolutePath();
        } catch (Throwable t) {
            HookLog.e("语音库 ensureSilk 异常", t);
            return null;
        }
    }

    public static String ensureMp3(Item it) {
        if (it == null) return null;
        File have = audioFile(it);
        if (have != null) return have.getAbsolutePath();
        try {
            
            if ("mp3".equals(it.ext) || "aac".equals(it.ext) || "m4a".equals(it.ext)) {
                File raw = rawFile(it);
                if (raw != null) return raw.getAbsolutePath();
            }
            File src = silkFile(it);
            if (src == null) src = rawFile(it);
            if (src == null) return null;
            byte[] b = readBytes(src);
            if (b == null) return null;
            Context c = QQEnv.appContext();
            if (c == null) return null;

            Object[] dec = SilkDecode.anyToPcm(c, b);
            if (dec == null || dec.length < 3 || dec[0] == null) {
                MsgStore.add("[语音库-W] 解码成 PCM 失败 " + it.name);
                return null;
            }
            byte[] pcm = (byte[]) dec[0];
            int rate = dec[1] instanceof Integer ? (Integer) dec[1] : 16000;
            int ch = dec[2] instanceof Integer ? (Integer) dec[2] : 1;

            SilkDecode.Encoded e = SilkDecode.encode(pcm, rate, ch);
            if (e == null || e.data == null) {
                MsgStore.add("[语音库-W] PCM 编码失败（MP3/AAC 都没有？）" + it.name);
                return null;
            }
            File out = fileOf(it, e.ext);
            if (out == null) return null;
            if (!writeAtomic(out, e.data)) return null;
            MsgStore.add("[语音库] 转普通音频完成 " + it.name + " · " + e.ext
                    + " · " + (out.length() / 1024) + "KB");
            return out.getAbsolutePath();
        } catch (Throwable t) {
            HookLog.e("语音库 ensureMp3 异常", t);
            return null;
        }
    }

    public static boolean send(Item it, int chatType, String peer) {
        return send(it, chatType, peer, 0);
    }

    public static boolean send(Item it, int chatType, String peer, int durOverrideMs) {
        return send(it, chatType, peer, durOverrideMs, true);
    }

    public static boolean send(Item it, int chatType, String peer,
                               int durOverrideMs, boolean autoConvert) {
        if (it == null || chatType == 0 || peer == null || peer.length() == 0) return false;
        File have = silkFile(it);
        String silk = have != null ? have.getAbsolutePath()
                : (autoConvert ? ensureSilk(it) : null);
        if (silk == null) return false;
        int dur = durOverrideMs > 0 ? durOverrideMs
                : (it.durMs > 0 ? (int) Math.min(Integer.MAX_VALUE, it.durMs) : 1000);
        return MsgApi.sendPtt(chatType, peer, silk, dur);
    }

    

    

    

    private static volatile String lastError = "";

    public static String lastError() {
        return lastError == null ? "" : lastError;
    }

    private static final int PCM_STATIC_MAX = 6 * 1024 * 1024;

    private static String engine = "";
    private static android.media.MediaPlayer player;
    private static String playingId;
    private static String playingTitle = "";
    private static int playingLen = 0;

    public interface OnPlay {
        void run(boolean ok);
    }

    public static Object[] preparePcm(Item it) {
        try {
            if (it == null) return null;
            File silkF = silkFile(it);
            if (silkF == null) return null;
            byte[] b = readBytes(silkF);
            if (b == null || b.length < 32) return null;
            Context c = QQEnv.appContext();
            if (c == null) return null;
            return SilkDecode.anyToPcm(c, b);
        } catch (Throwable t) {
            HookLog.e("语音库 preparePcm 异常", t);
            return null;
        }
    }

    public static boolean startPcm(Object[] pcm, String id, String title) {
        stop();
        try {
            if (pcm == null || pcm.length < 3 || !(pcm[0] instanceof byte[])) return false;
            byte[] data = (byte[]) pcm[0];
            int rate = pcm[1] instanceof Integer ? (Integer) pcm[1] : 24000;
            int ch = pcm[2] instanceof Integer ? (Integer) pcm[2] : 1;
            if (!PcmPlayer.load(data, rate, ch)) {
                MsgStore.add("[语音库-W] PCM 装载失败 " + title);
                return false;
            }
            if (!PcmPlayer.play()) {
                PcmPlayer.stop();
                MsgStore.add("[语音库-W] AudioTrack 起不来 " + title);
                return false;
            }
            engine = "pcm";
            playingId = id;
            playingTitle = title == null ? "" : title;
            playingLen = PcmPlayer.lengthMs();
            MsgStore.add("[语音库] 直接播腾讯语音 " + playingTitle + " · PCM "
                    + (data.length / 1024) + "KB · " + rate + "Hz/" + ch + "ch");
            return true;
        } catch (Throwable t) {
            HookLog.e("语音库 startPcm 失败", t);
            stop();
            return false;
        }
    }

    public static void playAsync(final Item it, final OnPlay cb) {
        if (it == null) {
            lastError = "没有这条语音";
            postCb(cb, false);
            return;
        }
        final String id = it.id;
        final String title = it.name;
        Bg.run(new Runnable() {
            @Override public void run() {
                String why = "";
                try {
                    
                    Object[] pcm = preparePcm(it);
                    byte[] data = pcm != null && pcm[0] instanceof byte[]
                            ? (byte[]) pcm[0] : null;
                    int rate = pcm != null && pcm[1] instanceof Integer
                            ? (Integer) pcm[1] : 24000;
                    int ch = pcm != null && pcm[2] instanceof Integer
                            ? (Integer) pcm[2] : 1;

                    if (data != null && data.length <= PCM_STATIC_MAX) {
                        final Object[] fp = pcm;
                        if (callMainBool(new Call() {
                            @Override public boolean run() { return startPcm(fp, id, title); }
                        }, 4)) {
                            lastError = "";
                            postCb(cb, true);
                            return;
                        }
                        why = "AudioTrack 起不来";
                        MsgStore.add("[语音库-W] PCM 直放失败，退回文件 · " + title);
                    } else if (data != null) {
                        why = "PCM 太大（" + (data.length / 1024 / 1024) + "MB）";
                    }

                    String path = null;
                    if (data != null) path = writeWav(it, data, rate, ch);
                    if (path == null) path = ensureMp3(it);
                    if (path == null) {
                        lastError = why.length() > 0 ? why : "解码/编码都失败";
                        MsgStore.add("[语音库-W] 放不出来 " + title + " · " + lastError);
                        postCb(cb, false);
                        return;
                    }
                    final String p2 = path;
                    boolean ok = callMainBool(new Call() {
                        @Override public boolean run() { return start(p2, id, title); }
                    }, 4);
                    lastError = ok ? "" : ("播放器起不来" + (why.length() > 0 ? "（" + why + "）" : ""));
                    postCb(cb, ok);
                } catch (Throwable t) {
                    HookLog.e("语音库 playAsync 异常", t);
                    lastError = "异常：" + t;
                    postCb(cb, false);
                }
            }
        }, "cat-voice-play");
    }

    private static void postCb(final OnPlay cb, final boolean ok) {
        if (cb == null) return;
        try {
            if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) {
                cb.run(ok);
                return;
            }
        } catch (Throwable ignore) {
        }
        new android.os.Handler(android.os.Looper.getMainLooper()).post(new Runnable() {
            @Override public void run() {
                try { cb.run(ok); } catch (Throwable ignore) { }
            }
        });
    }

    private interface Call {
        boolean run();
    }

    private static boolean callMainBool(final Call c, int timeoutSec) {
        final java.util.concurrent.CountDownLatch latch =
                new java.util.concurrent.CountDownLatch(1);
        final boolean[] ok = new boolean[1];
        new android.os.Handler(android.os.Looper.getMainLooper()).post(new Runnable() {
            @Override public void run() {
                try {
                    ok[0] = c.run();
                } catch (Throwable ignore) {
                } finally {
                    latch.countDown();
                }
            }
        });
        try {
            latch.await(timeoutSec, java.util.concurrent.TimeUnit.SECONDS);
        } catch (Throwable ignore) {
        }
        return ok[0];
    }

    private static String writeWav(Item it, byte[] pcm, int rate, int ch) {
        try {
            File out = fileOf(it, "wav");
            if (out == null) return null;
            File have = wavFile(it);
            if (have != null) return have.getAbsolutePath();
            java.io.ByteArrayOutputStream bo = new java.io.ByteArrayOutputStream(pcm.length + 64);
            int dataLen = pcm.length;
            int byteRate = rate * ch * 2;
            java.io.DataOutputStream d = new java.io.DataOutputStream(bo);
            d.writeBytes("RIFF");
            d.writeInt(Integer.reverseBytes(36 + dataLen));
            d.writeBytes("WAVEfmt ");
            d.writeInt(Integer.reverseBytes(16));
            d.writeShort(Short.reverseBytes((short) 1));          
            d.writeShort(Short.reverseBytes((short) ch));
            d.writeInt(Integer.reverseBytes(rate));
            d.writeInt(Integer.reverseBytes(byteRate));
            d.writeShort(Short.reverseBytes((short) (ch * 2)));
            d.writeShort(Short.reverseBytes((short) 16));
            d.writeBytes("data");
            d.writeInt(Integer.reverseBytes(dataLen));
            d.write(pcm);
            d.flush();
            if (!writeAtomic(out, bo.toByteArray())) return null;
            MsgStore.add("[语音库] 写了 WAV 备用 " + it.name + " · "
                    + (out.length() / 1024) + "KB");
            return out.getAbsolutePath();
        } catch (Throwable t) {
            HookLog.w("语音库 writeWav 失败: " + t);
            return null;
        }
    }

    public static String playablePath(Item it) {
        return ensureMp3(it);
    }

    public static boolean start(String path, String id, String title) {
        stop();
        if (path == null || path.length() == 0) return false;
        try {
            android.media.MediaPlayer mp = new android.media.MediaPlayer();
            mp.setAudioStreamType(android.media.AudioManager.STREAM_MUSIC);
            mp.setDataSource(path);
            mp.prepare();
            mp.start();
            mp.setOnCompletionListener(new android.media.MediaPlayer.OnCompletionListener() {
                @Override public void onCompletion(android.media.MediaPlayer m) { stop(); }
            });
            mp.setOnErrorListener(new android.media.MediaPlayer.OnErrorListener() {
                @Override public boolean onError(android.media.MediaPlayer m, int what, int extra) {
                    MsgStore.add("[语音库-W] 播放出错 what=" + what + " extra=" + extra
                            + " · " + playingTitle);
                    stop();
                    return true;
                }
            });
            engine = "file";
            player = mp;
            playingId = id;
            playingTitle = title == null ? "" : title;
            playingLen = 0;
            try { playingLen = mp.getDuration(); } catch (Throwable ignore) { }
            MsgStore.add("[语音库] 播放音频文件 " + playingTitle + " · " + path);
            return true;
        } catch (Throwable t) {
            HookLog.e("语音库 start 播放失败", t);
            MsgStore.add("[语音库-W] start 播放失败: " + t + " · " + path);
            stop();
            return false;
        }
    }

    public static void stop() {
        if (player != null) {
            try { player.stop(); } catch (Throwable ignore) { }
            try { player.release(); } catch (Throwable ignore) { }
        }
        player = null;
        try { PcmPlayer.stop(); } catch (Throwable ignore) { }
        engine = "";
        playingId = null;
        playingTitle = "";
        playingLen = 0;
    }

    public static String playingId() {
        try {
            if ("pcm".equals(engine)) {
                if (PcmPlayer.isPlaying() && !PcmPlayer.finished()) return playingId;
                return null;
            }
            if (player != null && player.isPlaying()) return playingId;
        } catch (Throwable ignore) {
        }
        return null;
    }

    public static void pause() {
        try {
            if ("pcm".equals(engine)) PcmPlayer.pause();
            else if (player != null) player.pause();
        } catch (Throwable ignore) {
        }
    }

    public static void resume() {
        try {
            if ("pcm".equals(engine)) PcmPlayer.play();
            else if (player != null) player.start();
        } catch (Throwable ignore) {
        }
    }

    public static boolean isPlaying() {
        try {
            if ("pcm".equals(engine)) return PcmPlayer.isPlaying();
            return player != null && player.isPlaying();
        } catch (Throwable ignore) {
            return false;
        }
    }

    public static boolean hasPlayer() {
        if ("pcm".equals(engine)) {
            if (PcmPlayer.finished()) {
                stop();
                return false;
            }
            return PcmPlayer.isLoaded();
        }
        return player != null;
    }

    public static String playingTitle() {
        return playingTitle;
    }

    public static int positionMs() {
        try {
            if ("pcm".equals(engine)) return PcmPlayer.positionMs();
            if (player != null) return player.getCurrentPosition();
        } catch (Throwable ignore) {
        }
        return 0;
    }

    public static int lengthMs() {
        try {
            if ("pcm".equals(engine)) {
                int l = PcmPlayer.lengthMs();
                return l > 0 ? l : playingLen;
            }
            if (player != null) {
                int d = player.getDuration();
                if (d > 0) return d;
            }
        } catch (Throwable ignore) {
        }
        return playingLen;
    }

    public static void seek(int ms) {
        try {
            if ("pcm".equals(engine)) {
                PcmPlayer.seek(ms);
                return;
            }
            if (player != null && ms >= 0) player.seekTo(ms);
        } catch (Throwable ignore) {
        }
    }

    

    
    private static File[] scanDirs() {
        List<File> ds = new ArrayList<File>();
        try {
            File d = IoApi.dataDir();
            if (d != null) ds.add(new File(d, "parse"));
            Context c = QQEnv.appContext();
            if (c != null) {
                ds.add(new File(c.getFilesDir(), "api_tmp"));
                ds.add(c.getCacheDir());
            }
        } catch (Throwable ignore) {
        }
        return ds.toArray(new File[0]);
    }

    public static List<File> scanParseResults() {
        List<File> out = new ArrayList<File>();
        long now = System.currentTimeMillis();
        for (File d : scanDirs()) {
            if (d == null || !d.isDirectory()) continue;
            File[] fs = d.listFiles();
            if (fs == null) continue;
            for (File f : fs) {
                if (f == null || !f.isFile()) continue;
                if (f.length() < 512) continue;
                String n = f.getName().toLowerCase();
                if (!(n.endsWith(".silk") || n.endsWith(".slk") || n.endsWith(".mp3")
                        || n.endsWith(".m4a") || n.endsWith(".amr") || n.endsWith(".wav")
                        || n.endsWith(".ogg") || n.endsWith(".aac") || n.endsWith(".m4s")
                        || n.endsWith(".mp4"))) continue;
                if (now - f.lastModified() > 7L * 24 * 3600 * 1000) continue;
                out.add(f);
            }
        }
        Collections.sort(out, new Comparator<File>() {
            @Override public int compare(File a, File b) {
                return Long.compare(b.lastModified(), a.lastModified());
            }
        });
        if (out.size() > 40) return new ArrayList<File>(out.subList(0, 40));
        return out;
    }

    public static String stat() {
        List<Item> lib = list();
        long n = 0;
        for (Item it : lib) n += it.sizeBytes();
        int silk = 0;
        for (Item it : lib) if (it.hasSilk || silkFile(it) != null) silk++;
        return lib.size() + " 条 · " + (n / 1024) + "KB · 其中 " + silk + " 条可直接发语音";
    }

    

    
    public static long silkDurationMs(byte[] b) {
        if (!SilkDecode.isSilk(b)) return 0;
        int p = 10;
        int frames = 0;
        while (p + 2 <= b.length) {
            int len = (b[p] & 0xFF) | ((b[p + 1] & 0xFF) << 8);
            if (len <= 0) break;
            p += 2 + len;
            frames++;
        }
        return frames * 20L;
    }

    private static long probeDuration(File f) {
        MediaMetadataRetriever r = null;
        try {
            r = new MediaMetadataRetriever();
            r.setDataSource(f.getAbsolutePath());
            String s = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            if (s == null) return 0;
            return Long.parseLong(s.trim());
        } catch (Throwable t) {
            return 0;
        } finally {
            try { if (r != null) r.release(); } catch (Throwable ignore) {}
        }
    }

    private static String sniffExt(byte[] b) {
        try {
            if (b.length > 12) {
                String head = new String(b, 0, 4, "ISO-8859-1");
                if ("RIFF".equals(head)) return "wav";
                if ("OggS".equals(head)) return "ogg";
                if ("#!AM".equals(head)) return "amr";
                if (b[0] == 'I' && b[1] == 'D' && b[2] == '3') return "mp3";
                if ((b[0] & 0xFF) == 0xFF && (b[1] & 0xE0) == 0xE0) return "mp3";
                if (b[4] == 'f' && b[5] == 't' && b[6] == 'y' && b[7] == 'p') return "m4a";
                if ((b[0] & 0xFF) == 0xFF && (b[1] & 0xF0) == 0xF0) return "aac";
            }
        } catch (Throwable ignore) {
        }
        return "bin";
    }

    private static String newId() {
        return "v" + System.currentTimeMillis() + "_" + (int) (Math.random() * 900 + 100);
    }

    public static String cleanName(String name) {
        String n = name == null ? "" : name.trim();
        n = n.replaceAll("[\\r\\n\\t]", " ");
        if (n.length() > 40) n = n.substring(0, 40);
        return n;
    }

    private static void deleteQuiet(File f) {
        try { if (f != null && f.isFile()) f.delete(); } catch (Throwable ignore) {}
    }

    private static byte[] readBytes(File f) {
        InputStream in = null;
        try {
            in = new java.io.FileInputStream(f);
            byte[] buf = new byte[(int) Math.min(MAX_BYTES, Math.max(64, f.length()))];
            int off = 0;
            int n;
            while (off < buf.length && (n = in.read(buf, off, buf.length - off)) > 0) off += n;
            if (off == buf.length) return buf;
            byte[] out = new byte[off];
            System.arraycopy(buf, 0, out, 0, off);
            return out;
        } catch (Throwable t) {
            return null;
        } finally {
            try { if (in != null) in.close(); } catch (Throwable ignore) {}
        }
    }

    private static String read(File f) {
        byte[] b = readBytes(f);
        if (b == null) return null;
        try {
            return new String(b, "UTF-8");
        } catch (Throwable t) {
            return null;
        }
    }

    private static boolean writeAtomic(File f, byte[] data) {
        FileOutputStream fo = null;
        try {
            File tmp = new File(f.getAbsolutePath() + ".tmp");
            fo = new FileOutputStream(tmp);
            fo.write(data);
            fo.flush();
            try { fo.getFD().sync(); } catch (Throwable ignore) {}
            fo.close();
            fo = null;
            if (f.exists()) f.delete();
            return tmp.renameTo(f);
        } catch (Throwable t) {
            HookLog.w("语音库写文件失败 " + f + ": " + t);
            return false;
        } finally {
            try { if (fo != null) fo.close(); } catch (Throwable ignore) {}
        }
    }
}
