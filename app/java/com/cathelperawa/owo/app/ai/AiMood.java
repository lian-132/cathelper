package com.cathelperawa.owo.app.ai;

import com.cathelperawa.owo.app.core.DataDir;

import com.cathelperawa.owo.app.bridge.QQEnv;
import com.cathelperawa.owo.app.core.HookLog;
import com.cathelperawa.owo.app.core.SecureFile;

import org.json.JSONObject;

import java.io.File;
import java.util.Locale;

public final class AiMood {

    private AiMood() {}

    private static final String FILE_NAME = "ai_mood.json";

    private static final double INERTIA = 0.6;

    private static final double DECAY = 0.95;

    private static double pleasure = 0d;
    private static double arousal = 0d;
    private static long updatedAt = 0L;
    private static boolean loaded;

    public static synchronized double pleasure() {
        ensureLoaded();
        return pleasure;
    }

    public static synchronized double arousal() {
        ensureLoaded();
        return arousal;
    }

    public static synchronized void touch(String text) {
        if (text == null || text.length() == 0) return;
        ensureLoaded();
        decay();

        int pos = count(text, POS);
        int neg = count(text, NEG);
        int high = count(text, HIGH);
        int low = count(text, LOW);

        double dp = (pos - neg) * 0.12;
        double da = (high - low) * 0.10;
        if (text.indexOf('!') >= 0 || text.indexOf('！') >= 0) da += 0.05;

        applyDelta(dp, da);
    }

    public static synchronized void applyDelta(double dp, double da) {
        ensureLoaded();
        double tp = clamp(pleasure + dp, -1, 1);
        double ta = clamp(arousal + da, -1, 1);
        pleasure = clamp(pleasure + (tp - pleasure) * (1 - INERTIA), -1, 1);
        arousal = clamp(arousal + (ta - arousal) * (1 - INERTIA), -1, 1);
        updatedAt = System.currentTimeMillis();
        saveAsync();
    }

    public static synchronized void decay() {
        ensureLoaded();

        double k = DECAY;
        try {
            k = AiPersonality.moodDecayMod();
        } catch (Throwable ignore) {
        }
        pleasure *= k;
        arousal *= k;
        if (Math.abs(arousal) < 0.05) arousal *= 0.8;
        if (Math.abs(pleasure) < 0.05) pleasure *= 0.8;
        updatedAt = System.currentTimeMillis();
    }

    public static synchronized String describe() {
        ensureLoaded();
        if (Math.abs(pleasure) < 0.15 && Math.abs(arousal) < 0.15) {
            return "心情平静，没什么特别的情绪";
        }
        StringBuilder sb = new StringBuilder();
        if (pleasure > 0.45) sb.append("心情挺好，说话会松快一点");
        else if (pleasure > 0.15) sb.append("心情不错");
        else if (pleasure < -0.45) sb.append("心情很差，有点烦躁，话会变少变冲");
        else if (pleasure < -0.15) sb.append("心情有点低");
        else sb.append("心情一般");

        if (arousal > 0.4) sb.append("，比较兴奋，话可能多一点");
        else if (arousal < -0.3) sb.append("，有点累，不太想多聊");
        return sb.toString();
    }

    public static synchronized int pleasurePct() {
        ensureLoaded();
        return (int) Math.round((pleasure + 1) / 2 * 100);
    }

    public static synchronized int arousalPct() {
        ensureLoaded();
        return (int) Math.round((arousal + 1) / 2 * 100);
    }

    public static synchronized void reset() {
        ensureLoaded();
        pleasure = 0;
        arousal = 0;
        updatedAt = System.currentTimeMillis();
        saveAsync();
    }

    private static final java.util.Map<String, java.util.LinkedList<Double>> USER =
            new java.util.concurrent.ConcurrentHashMap<String, java.util.LinkedList<Double>>();

    public static void touchUser(String uid, String text) {
        if (uid == null || uid.length() == 0 || text == null || text.length() < 2) return;
        int pos = count(text, POS);
        int neg = count(text, NEG);
        if (pos == 0 && neg == 0) return;
        double v = (pos > neg) ? 0.7 : (neg > pos ? -0.7 : 0);
        if (v == 0) return;
        synchronized (USER) {
            java.util.LinkedList<Double> q = USER.get(uid);
            if (q == null) {
                q = new java.util.LinkedList<Double>();
                USER.put(uid, q);
            }
            q.addLast(Double.valueOf(v));
            while (q.size() > 8) q.removeFirst();
        }

        double avg = userMoodValue(uid);
        if (avg < -0.4) applyDelta(-0.05, -0.02);
        else if (avg > 0.4) applyDelta(0.03, 0.0);
    }

    public static double userMoodValue(String uid) {
        if (uid == null) return 0;
        synchronized (USER) {
            java.util.LinkedList<Double> q = USER.get(uid);
            if (q == null || q.size() < 2) return 0;
            double sum = 0, wsum = 0;
            int n = q.size();
            for (int i = 0; i < n; i++) {
                double w = 1.0 + i * 0.5;
                sum += q.get(i).doubleValue() * w;
                wsum += w;
            }
            return wsum <= 0 ? 0 : sum / wsum;
        }
    }

    public static java.util.Map<String, Double> userMoodAll() {
        java.util.Map<String, Double> out = new java.util.HashMap<String, Double>();
        synchronized (USER) {
            for (String uid : new java.util.ArrayList<String>(USER.keySet())) {
                double v = userMoodValue(uid);
                if (Math.abs(v) > 0.05) out.put(uid, Double.valueOf(v));
            }
        }
        return out;
    }

    public static String userMoodHint(String uid) {
        double v = userMoodValue(uid);
        if (v <= -0.5) return "他这两天不太对劲，说的话都挺低落的 —— 别怼，短一点，认真接一句。";
        if (v <= -0.25) return "他情绪一般偏下，别拿他开玩笑。";
        if (v >= 0.5) return "他心情不错，可以正常开玩笑。";
        return "";
    }

    private static final String[] POS = {
            "开心", "高兴", "哈哈", "好耶", "喜欢", "爱你", "谢谢", "厉害",
            "太强", "牛啊", "赞", "不错", "好玩", "笑死", "棒"
    };
    private static final String[] NEG = {
            "烦", "生气", "气死", "难过", "哭", "滚", "傻逼", "闭嘴",
            "讨厌", "无语", "烂", "垃圾", "恶心", "神经病", "人机", "废物"
    };
    private static final String[] HIGH = {
            "卧槽", "我靠", "草", "！！", "???", "急", "救命", "炸"
    };
    private static final String[] LOW = {
            "嗯", "哦", "随便", "算了", "困", "睡了", "不想"
    };

    private static int count(String text, String[] words) {
        int n = 0;
        String t = text.toLowerCase(Locale.CHINA);
        for (int i = 0; i < words.length; i++) {
            if (t.indexOf(words[i]) >= 0) n++;
        }
        return n;
    }

    private static double clamp(double v, double min, double max) {
        return v < min ? min : (v > max ? max : v);
    }

    private static File file() {
        try {
            File d = DataDir.account("ai");
            if (!d.exists()) d.mkdirs();
            return new File(d, FILE_NAME);
        } catch (Throwable t) {
            return null;
        }
    }

    private static void ensureLoaded() {
        if (loaded) return;
        loaded = true;
        try {
            File f = file();
            if (f == null || !f.exists() || f.length() == 0) return;
            int[] st = new int[1];
            String text = SecureFile.readText(f, st);
            if (text == null || text.length() == 0) return;
            JSONObject o = new JSONObject(text);
            pleasure = o.optDouble("p", 0);
            arousal = o.optDouble("a", 0);
            long t = o.optLong("t", 0);
            if (t > 0) {
                double hours = (System.currentTimeMillis() - t) / 3600000d;
                double k = Math.pow(0.8, Math.max(0, hours));
                pleasure *= k;
                arousal *= k;
            }
        } catch (Throwable t) {
            HookLog.w("心情读取失败 " + t);
        }
    }

    private static void saveAsync() {
        try {
            com.cathelperawa.owo.app.core.Bg.run(new Runnable() {
                @Override public void run() {
                    saveNow();
                }
            }, "cat-mood");
        } catch (Throwable ignore) {
        }
    }

    public static synchronized void saveNow() {
        try {
            File f = file();
            if (f == null) return;
            JSONObject o = new JSONObject();
            o.put("p", pleasure);
            o.put("a", arousal);
            o.put("t", updatedAt > 0 ? updatedAt : System.currentTimeMillis());
            SecureFile.writeText(f, o.toString());
        } catch (Throwable t) {
            HookLog.w("心情写盘失败 " + t);
        }
    }
}
