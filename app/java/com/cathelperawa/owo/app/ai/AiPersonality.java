package com.cathelperawa.owo.app.ai;

import com.cathelperawa.owo.app.core.DataDir;
import com.cathelperawa.owo.app.core.HookLog;
import com.cathelperawa.owo.app.core.SecureFile;

import org.json.JSONObject;

import java.io.File;
import java.util.Locale;

public final class AiPersonality {

    private AiPersonality() {}

    private static final String FILE_NAME = "ai_personality.json";

    private static final double MAX_STEP = 0.02;
    private static final double MIN_V = 0.20;
    private static final double MAX_V = 0.90;

    public static double openness = 0.62;
    public static double conscientiousness = 0.50;
    public static double extraversion = 0.55;
    public static double agreeableness = 0.58;
    public static double neuroticism = 0.45;

    private static boolean loaded;

    public static synchronized void nudge(String dim, double delta) {
        ensureLoaded();
        if (dim == null) return;
        if (delta > MAX_STEP) delta = MAX_STEP;
        if (delta < -MAX_STEP) delta = -MAX_STEP;
        if ("O".equals(dim)) openness = clamp(openness + delta);
        else if ("C".equals(dim)) conscientiousness = clamp(conscientiousness + delta);
        else if ("E".equals(dim)) extraversion = clamp(extraversion + delta);
        else if ("A".equals(dim)) agreeableness = clamp(agreeableness + delta);
        else if ("N".equals(dim)) neuroticism = clamp(neuroticism + delta);
        else return;
        saveAsync();
    }

    public static synchronized void observe(String userText, String selfText) {
        ensureLoaded();
        try {
            String u = userText == null ? "" : userText;
            String s = selfText == null ? "" : selfText;

            if (u.contains("谢谢") || u.contains("厉害") || u.contains("喜欢你")
                    || u.contains("可爱") || u.contains("棒")) {
                nudge("A", 0.008);
                nudge("E", 0.004);
            }

            if (u.contains("闭嘴") || u.contains("傻") || u.contains("滚")
                    || u.contains("人机") || u.contains("废物") || u.contains("烦")) {
                nudge("A", -0.010);
                nudge("N", 0.008);
            }

            if (s.length() > 60) {
                nudge("O", 0.005);
                nudge("E", 0.004);
            }

            if (s.length() > 0 && s.length() < 8) {
                nudge("E", -0.003);
            }
        } catch (Throwable ignore) {
        }
    }

    public static synchronized String describe() {
        ensureLoaded();
        StringBuilder sb = new StringBuilder();

        String[] dims = topDims();
        if (dims.length == 0) return "性格比较均衡，没什么特别的偏向";
        sb.append("性格上：");
        for (int i = 0; i < dims.length; i++) {
            if (i > 0) sb.append("，");
            sb.append(dims[i]);
        }

        String hint = behaviorHint();
        if (hint.length() > 0) sb.append("。").append(hint);
        return sb.toString();
    }

    private static String behaviorHint() {
        java.util.List<String> tips = new java.util.ArrayList<String>();
        if (extraversion > 0.62) tips.add("遇到有意思的话题可以主动插一句");
        else if (extraversion < 0.42) tips.add("不爱抢话，能少说就少说");
        if (agreeableness < 0.45) tips.add("不太迁就人，被惹了会直接顶回去");
        else if (agreeableness > 0.7) tips.add("不太会刻薄人，就算被怼也留分寸");
        if (neuroticism > 0.6) tips.add("容易被戳到，情绪来得快");
        else if (neuroticism < 0.35) tips.add("不太容易炸，被嘲也稳得住");
        if (openness > 0.7) tips.add("对新东西好奇，愿意追问两句");
        if (conscientiousness < 0.4) tips.add("说话随性，别端着");
        if (tips.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < tips.size() && i < 3; i++) {
            if (i > 0) sb.append("；");
            sb.append(tips.get(i));
        }
        return sb.toString();
    }

    public static double urgencyMod() {
        return (extraversion - 0.5) * 0.4;
    }

    public static double moodDecayMod() {
        double k = 0.95 + (neuroticism - 0.5) * 0.09;
        return k < 0.88 ? 0.88 : (k > 0.995 ? 0.995 : k);
    }

    public static double affinityMod() {
        return 1.0 + (agreeableness - 0.5) * 0.4;
    }

    public static synchronized String impactText() {
        ensureLoaded();
        int talk = (int) Math.round(urgencyMod() * 100);
        int aff = (int) Math.round((affinityMod() - 1.0) * 100);
        double k = moodDecayMod();

        int mood = (int) Math.round((k - 0.95) / 0.95 * 100);
        StringBuilder sb = new StringBuilder("影响：");
        sb.append("主动开口 ").append(signed(talk)).append("%");
        sb.append("  情绪恢复").append(mood == 0 ? "不变" : (mood > 0 ? "慢 " + mood + "%" : "快 " + (-mood) + "%"));
        sb.append("  好感变化 ").append(signed(aff)).append("%");
        return sb.toString();
    }

    private static String signed(int v) {
        return v > 0 ? "+" + v : String.valueOf(v);
    }

    public static synchronized String panelText() {
        ensureLoaded();
        return String.format(Locale.CHINA,
                "开放 %.0f%%  尽责 %.0f%%  外向 %.0f%%\n宜人 %.0f%%  神经质 %.0f%%",
                openness * 100, conscientiousness * 100, extraversion * 100,
                agreeableness * 100, neuroticism * 100);
    }

    public static synchronized void reset() {
        ensureLoaded();
        openness = 0.62;
        conscientiousness = 0.50;
        extraversion = 0.55;
        agreeableness = 0.58;
        neuroticism = 0.45;
        saveAsync();
    }

    private static String[] topDims() {
        double[] v = {openness, conscientiousness, extraversion, agreeableness, neuroticism};
        String[] hi = {"好奇心重、爱聊新东西", "做事认真、说了会做到", "话多、爱主动搭话",
                "好说话、不爱刻薄人", "情绪起伏大、有点玻璃心"};
        String[] lo = {"偏保守、不太爱折腾", "随性、不太较真", "话少、偏安静",
                "嘴有点硬、不是好脾气", "情绪很稳、不容易炸"};

        double[] dev = new double[5];
        for (int i = 0; i < 5; i++) dev[i] = Math.abs(v[i] - 0.5);

        int a = -1, b = -1;
        for (int i = 0; i < 5; i++) {
            if (dev[i] < 0.06) continue;
            if (a < 0 || dev[i] > dev[a]) { b = a; a = i; }
            else if (b < 0 || dev[i] > dev[b]) { b = i; }
        }
        if (a < 0) return new String[0];
        java.util.List<String> out = new java.util.ArrayList<String>();
        out.add(v[a] > 0.5 ? hi[a] : lo[a]);
        if (b >= 0) out.add(v[b] > 0.5 ? hi[b] : lo[b]);
        return out.toArray(new String[0]);
    }

    private static double clamp(double v) {
        return v < MIN_V ? MIN_V : (v > MAX_V ? MAX_V : v);
    }

    private static File file() {
        try {
            File d = DataDir.account("ai");
            return d == null ? null : new File(d, FILE_NAME);
        } catch (Throwable t) {
            return null;
        }
    }

    private static void ensureLoaded() {
        if (loaded) return;
        loaded = true;
        try {
            File f = file();
            if (f == null || !f.exists() || f.length() == 0) {

                try {
                    String uin = "";
                    try {
                        uin = com.cathelperawa.owo.app.bridge.QQEnv.currentUin();
                    } catch (Throwable ignore) {
                    }
                    long seed = (uin == null || uin.length() == 0) ? 20261027L : uin.hashCode();
                    java.util.Random r = new java.util.Random(seed);
                    double sensitivity = 0.35 + r.nextDouble() * 0.30;
                    double curiosity = 0.45 + r.nextDouble() * 0.35;
                    double recovery = 0.40 + r.nextDouble() * 0.35;
                    neuroticism = sensitivity;
                    openness = curiosity;
                    extraversion = recovery * 0.8 + 0.10;
                    agreeableness = recovery * 0.7 + 0.20;
                    conscientiousness = (recovery + (1.0 - curiosity)) / 2.0;
                    openness = clamp(openness);
                    conscientiousness = clamp(conscientiousness);
                    extraversion = clamp(extraversion);
                    agreeableness = clamp(agreeableness);
                    neuroticism = clamp(neuroticism);
                    saveNow();
                    HookLog.i("人格：先天底色已生成（气质 seed=" + seed + "）");
                } catch (Throwable ignore) {
                }
                return;
            }
            int[] st = new int[1];
            String txt = SecureFile.readText(f, st);
            if (txt == null || txt.length() == 0) return;
            JSONObject o = new JSONObject(txt);
            openness = o.optDouble("O", openness);
            conscientiousness = o.optDouble("C", conscientiousness);
            extraversion = o.optDouble("E", extraversion);
            agreeableness = o.optDouble("A", agreeableness);
            neuroticism = o.optDouble("N", neuroticism);
        } catch (Throwable t) {
            HookLog.w("人格读取失败 " + t);
        }
    }

    private static void saveAsync() {
        try {
            com.cathelperawa.owo.app.core.Bg.run(new Runnable() {
                @Override public void run() {
                    saveNow();
                }
            }, "cat-persona");
        } catch (Throwable ignore) {
        }
    }

    public static synchronized void saveNow() {
        try {
            File f = file();
            if (f == null) return;
            JSONObject o = new JSONObject();
            o.put("O", openness);
            o.put("C", conscientiousness);
            o.put("E", extraversion);
            o.put("A", agreeableness);
            o.put("N", neuroticism);
            SecureFile.writeText(f, o.toString());
        } catch (Throwable t) {
            HookLog.w("人格写盘失败 " + t);
        }
    }
}
