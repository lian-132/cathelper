package com.cathelperawa.owo.app.ai;

import com.cathelperawa.owo.app.core.DataDir;

import com.cathelperawa.owo.app.bridge.QQEnv;
import com.cathelperawa.owo.app.core.HookLog;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class AiAffinity {

    public static final int VERSION = 1;

    public static final int MIN = 0;
    public static final int MAX = 100;
    
    public static final int DEFAULT_SCORE = 50;

    public static final int MAX_PEOPLE = 500;
    
    public static final int MAX_SUMMARY = 160;

    private static final String FILE_NAME = "ai_affinity.json";
    private static final long MAX_FILE = 8L * 1024 * 1024;

    public static final class Profile {
        public String uid = "";
        
        public String uin = "";
        
        public String name = "";
        
        public int score = DEFAULT_SCORE;
        
        public String summary = "";
        
        public int chats = 0;
        
        public int pending = 0;
        public long lastAt = 0;
        public long updatedAt = 0;
        
        public int lastDelta = 0;
        public String lastReason = "";
        
        public int dayKey = 0;
        public int dayDelta = 0;
        
        public int decayDays = 0;
        
        public final List<Change> log = new ArrayList<Change>();

        public String displayName() {
            if (name != null && name.trim().length() > 0) return name.trim();
            if (uin != null && uin.length() > 0) return uin;
            return "未知";
        }

        public String tier() {
            
            if (uid != null) {
                if (isOwner(uid)) return "主人";
                if (isAdmin(uid)) return "管理员";
            }
            return AiAffinity.tier(score);
        }

        JSONObject toJson() {
            JSONObject o = new JSONObject();
            try {
                o.put("uid", uid);
                o.put("uin", uin);
                o.put("name", name);
                o.put("score", score);
                o.put("summary", summary);
                o.put("chats", chats);
                o.put("pending", pending);
                o.put("lastAt", lastAt);
                o.put("updatedAt", updatedAt);
                o.put("lastDelta", lastDelta);
                o.put("lastReason", lastReason);
                o.put("dayKey", dayKey);
                o.put("dayDelta", dayDelta);
                o.put("dd", decayDays);
                JSONArray arr = new JSONArray();
                for (int i = 0; i < log.size(); i++) arr.put(log.get(i).toJson());
                o.put("log", arr);
            } catch (Throwable ignore) {
            }
            return o;
        }

        static Profile fromJson(JSONObject o) {
            Profile p = new Profile();
            p.uid = o.optString("uid", "");
            p.uin = o.optString("uin", "");
            p.name = o.optString("name", "");
            p.score = o.optInt("score", DEFAULT_SCORE);
            if (p.score < MIN) p.score = MIN;
            if (p.score > MAX) p.score = MAX;
            p.summary = o.optString("summary", "");
            p.chats = o.optInt("chats", 0);
            p.pending = o.optInt("pending", 0);
            p.lastAt = o.optLong("lastAt", 0);
            p.updatedAt = o.optLong("updatedAt", 0);
            p.lastDelta = o.optInt("lastDelta", 0);
            p.lastReason = o.optString("lastReason", "");
            p.dayKey = o.optInt("dayKey", 0);
            p.dayDelta = o.optInt("dayDelta", 0);
            p.decayDays = o.optInt("dd", 0);
            JSONArray arr = o.optJSONArray("log");
            if (arr != null) {
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject c = arr.optJSONObject(i);
                    if (c != null) p.log.add(Change.fromJson(c));
                }
            }
            return p;
        }
    }

    public static final class Change {
        public long at;
        public int delta;
        public int score;
        public String reason = "";

        Change(long at, int delta, int score, String reason) {
            this.at = at;
            this.delta = delta;
            this.score = score;
            this.reason = reason == null ? "" : reason;
        }

        JSONObject toJson() {
            JSONObject o = new JSONObject();
            try {
                o.put("t", at);
                o.put("d", delta);
                o.put("s", score);
                o.put("r", reason);
            } catch (Throwable ignore) {
            }
            return o;
        }

        static Change fromJson(JSONObject o) {
            return new Change(o.optLong("t", 0), o.optInt("d", 0),
                    o.optInt("s", 0), o.optString("r", ""));
        }

        public String describe() {
            String when = new SimpleDateFormat("MM-dd HH:mm", Locale.CHINA)
                    .format(new Date(at));
            String d = delta > 0 ? "+" + delta : String.valueOf(delta);
            return when + "  " + d + " → " + score + (reason.length() > 0 ? "  " + reason : "");
        }
    }

    private AiAffinity() {}

    

    private static final Map<String, Profile> MAP =
            new LinkedHashMap<String, Profile>();

    private static volatile boolean loaded = false;
    private static volatile boolean saveQueued = false;

    private static final java.util.concurrent.ExecutorService SAVER =
            java.util.concurrent.Executors.newSingleThreadExecutor(
                    new java.util.concurrent.ThreadFactory() {
                        @Override public Thread newThread(Runnable r) {
                            Thread t = new Thread(r, "cat-ai-affinity");
                            t.setDaemon(true);
                            return t;
                        }
                    });

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
        File f = file();
        if (f == null || !f.exists() || f.length() == 0 || f.length() > MAX_FILE) return;
        
        int[] st = new int[1];
        String text = com.cathelperawa.owo.app.core.SecureFile.readText(f, st);
        if (st[0] == com.cathelperawa.owo.app.core.SecureFile.CORRUPT) {
            com.cathelperawa.owo.app.core.SecureFile.backupCorrupt(f);
            return;
        }
        if (text == null || text.length() == 0) return;
        try {
            JSONObject root = new JSONObject(text);
            JSONArray arr = root.optJSONArray("people");
            if (arr == null) return;
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o == null) continue;
                Profile p = Profile.fromJson(o);
                if (p.uid == null || p.uid.length() == 0) continue;
                MAP.put(p.uid, p);
            }
            HookLog.i("AI 好感度已载入 " + MAP.size() + " 人");
            
            if (st[0] == com.cathelperawa.owo.app.core.SecureFile.PLAIN) saveAsync();
        } catch (Throwable t) {
            
            HookLog.w("AI 好感度读取失败 " + t);
        }
    }

    private static void saveAsync() {
        if (saveQueued) return;
        saveQueued = true;
        try {
            SAVER.execute(new Runnable() {
                @Override public void run() {
                    try { Thread.sleep(800); } catch (Throwable ignore) {}
                    saveQueued = false;
                    saveNow();
                }
            });
        } catch (Throwable t) {
            saveQueued = false;
        }
    }

    public static void saveNow() {
        File f = file();
        if (f == null) return;
        String json;
        synchronized (AiAffinity.class) { json = dumpJson(); }
        
        com.cathelperawa.owo.app.core.SecureFile.writeText(f, json);
    }

    public static synchronized String dumpJson() {
        JSONObject root = new JSONObject();
        JSONArray arr = new JSONArray();
        try {
            root.put("v", VERSION);
            for (Map.Entry<String, Profile> e : MAP.entrySet()) {
                arr.put(e.getValue().toJson());
            }
            root.put("people", arr);
        } catch (Throwable ignore) {
        }
        return root.toString();
    }

    

    

    

    public static final class Milestone {
        public final String uid;
        public final String tier;
        public final long at;

        Milestone(String uid, String tier, long at) {
            this.uid = uid == null ? "" : uid;
            this.tier = tier == null ? "" : tier;
            this.at = at;
        }
    }

    private static final java.util.List<Milestone> MILESTONES =
            new java.util.ArrayList<Milestone>();
    private static final String MS_FILE = "ai_milestones.json";
    private static boolean msLoaded;

    static synchronized void checkMilestone(String uid, int oldScore, int newScore) {
        try {
            if (uid == null || uid.length() == 0) return;
            if (isOwner(uid)) return;             
            String before = tier(oldScore);
            String after = tier(newScore);
            if (before.equals(after)) return;
            
            if (newScore <= oldScore) return;
            loadMilestones();
            MILESTONES.add(new Milestone(uid, after, System.currentTimeMillis() / 1000L));
            while (MILESTONES.size() > 300) MILESTONES.remove(0);
            saveMilestones();
            HookLog.i("关系里程碑：" + uid + " → " + after);
        } catch (Throwable ignore) {
        }
    }

    public static synchronized java.util.List<Milestone> milestones(String uid) {
        loadMilestones();
        java.util.List<Milestone> out = new java.util.ArrayList<Milestone>();
        for (int i = MILESTONES.size() - 1; i >= 0; i--) {
            Milestone m = MILESTONES.get(i);
            if (uid == null || uid.equals(m.uid)) out.add(m);
        }
        return out;
    }

    private static void loadMilestones() {
        if (msLoaded) return;
        msLoaded = true;
        try {
            java.io.File d = com.cathelperawa.owo.app.core.DataDir.account("ai");
            if (d == null) return;
            java.io.File f = new java.io.File(d, MS_FILE);
            if (!f.exists() || f.length() == 0) return;
            int[] st = new int[1];
            String txt = com.cathelperawa.owo.app.core.SecureFile.readText(f, st);
            if (txt == null || txt.length() == 0) return;
            org.json.JSONArray arr = new org.json.JSONArray(txt);
            for (int i = 0; i < arr.length(); i++) {
                org.json.JSONObject o = arr.optJSONObject(i);
                if (o == null) continue;
                MILESTONES.add(new Milestone(o.optString("uid"), o.optString("tier"),
                        o.optLong("at", 0)));
            }
        } catch (Throwable ignore) {
        }
    }

    private static void saveMilestones() {
        try {
            java.io.File d = com.cathelperawa.owo.app.core.DataDir.account("ai");
            if (d == null) return;
            org.json.JSONArray arr = new org.json.JSONArray();
            for (int i = 0; i < MILESTONES.size(); i++) {
                Milestone m = MILESTONES.get(i);
                org.json.JSONObject o = new org.json.JSONObject();
                o.put("uid", m.uid);
                o.put("tier", m.tier);
                o.put("at", m.at);
                arr.put(o);
            }
            com.cathelperawa.owo.app.core.SecureFile.writeText(
                    new java.io.File(d, MS_FILE), arr.toString());
        } catch (Throwable ignore) {
        }
    }

    static boolean isOwner(String uid) {
        try {
            return AiConfig.isSelfUid(uid);
        } catch (Throwable t) {
            return false;
        }
    }

    static boolean isAdmin(String uid) {
        try {
            String uin = com.cathelperawa.owo.app.api.IdentityApi.uinFromUid(uid);
            return AiConfig.isAdminUin(uin) || AiConfig.isAdminUid(uid);
        } catch (Throwable t) {
            return false;
        }
    }

    public static synchronized Profile get(String uid) {
        if (uid == null || uid.length() == 0) return null;
        ensureLoaded();
        Profile p = MAP.get(uid);
        if (p != null) decay(p);
        return p;
    }

    public static synchronized List<Profile> list() {
        ensureLoaded();
        for (Profile p : MAP.values()) decay(p);
        List<Profile> out = new ArrayList<Profile>(MAP.values());
        Collections.sort(out, new Comparator<Profile>() {
            @Override public int compare(Profile a, Profile b) {
                if (a.lastAt == b.lastAt) return 0;
                return b.lastAt > a.lastAt ? 1 : -1;
            }
        });
        return out;
    }

    public static synchronized int count() {
        ensureLoaded();
        return MAP.size();
    }

    public static synchronized int pending(String uid) {
        Profile p = get(uid);
        return p == null ? 0 : p.pending;
    }

    public static synchronized String brief() {
        List<Profile> all = list();
        if (all.isEmpty()) return "还没有记录 · 聊过就会自动生成";
        StringBuilder sb = new StringBuilder(all.size() + " 人 · ");
        int n = Math.min(3, all.size());
        for (int i = 0; i < n; i++) {
            Profile p = all.get(i);
            if (i > 0) sb.append(" · ");
            sb.append(p.displayName()).append(" ").append(p.score);
        }
        return sb.toString();
    }

    

    
    public static synchronized Profile touch(String uid, String name, String uin) {
        if (!AiConfig.affinityOn()) return null;      
        if (uid == null || uid.length() == 0) return null;
        ensureLoaded();
        Profile p = MAP.get(uid);
        if (p == null) {
            p = new Profile();
            p.uid = uid;
            
            p.score = isOwner(uid) ? MAX : AiConfig.affinityInit();
            p.chats = 0;
            MAP.put(uid, p);
            trimToLimit();
        }
        
        if (isOwner(uid) && p.score < MAX) {
            p.score = MAX;
            p.lastReason = "主人·满格";
        }
        if (name != null && name.trim().length() > 0) p.name = name.trim();
        if (uin != null && uin.length() > 0) p.uin = uin;
        p.chats++;
        
        if (AiConfig.affinityAuto()) p.pending++;
        p.decayDays = 0;                       
        p.lastAt = System.currentTimeMillis();
        saveAsync();
        return p;
    }

    public static synchronized int apply(String uid, int delta, String summary, String reason) {
        
        if (!AiConfig.affinityOn()) return 0;
        if (uid == null || uid.length() == 0) return 0;

        if (isOwner(uid)) {
            Profile owner = MAP.get(uid);
            if (owner != null) {
                owner.score = MAX;
                if (summary != null && summary.trim().length() > 0) owner.summary = summary.trim();
                owner.updatedAt = System.currentTimeMillis();
                owner.lastDelta = 0;
                owner.lastReason = "主人·不参与打分";
                saveAsync();
            }
            return MAX;
        }
        
        if (delta < 0 && isAdmin(uid)) delta = delta / 2;
        
        try {
            double m = AiPersonality.affinityMod();
            if (delta != 0 && m != 1.0d) {
                int d = (int) Math.round(delta * m);
                if (d == 0) d = delta > 0 ? 1 : -1;   
                delta = d;
            }
        } catch (Throwable ignore) {
        }
        ensureLoaded();
        Profile p = MAP.get(uid);
        if (p == null) return 0;

        int step = AiConfig.affinityMaxStep();
        if (delta > step) delta = step;
        if (delta < -step) delta = -step;
        
        delta = damp(delta, p.score);

        int today = todayKey();
        if (p.dayKey != today) {
            p.dayKey = today;
            p.dayDelta = 0;
        }
        int cap = AiConfig.affinityDailyCap();
        if (cap > 0) {
            int want = delta;
            if (delta > 0) delta = Math.min(delta, Math.max(0, cap - p.dayDelta));
            else if (delta < 0) delta = Math.max(delta, Math.min(0, -cap - p.dayDelta));
            if (want != 0 && delta == 0) {
                AiDebug.log("  · 好感度: 今日已达上限 ±" + cap + "（当前已 " + p.dayDelta
                        + "），本次不动 —— 明天再来");
            }
        }

        int old = p.score;
        int now = old + delta;
        if (now < MIN) now = MIN;
        if (now > MAX) now = MAX;
        p.score = now;
        
        if (now > old) {
            try { checkMilestone(uid, old, now); } catch (Throwable ignore) {}
        }
        delta = now - old;   

        p.dayDelta += delta;
        p.pending = 0;
        p.updatedAt = System.currentTimeMillis();
        p.lastDelta = delta;
        p.lastReason = reason == null ? "" : reason.trim();

        if (summary != null && summary.trim().length() > 0) {
            String s = summary.trim();
            if (s.length() > MAX_SUMMARY) s = s.substring(0, MAX_SUMMARY);
            p.summary = s;
        }

        if (delta != 0) addLog(p, delta, now, p.lastReason);
        saveAsync();
        return delta;
    }

    public static synchronized int setScore(String uid, int score, String reason) {
        if (uid == null || uid.length() == 0) return 0;
        ensureLoaded();
        Profile p = MAP.get(uid);
        if (p == null) return 0;
        if (score < MIN) score = MIN;
        if (score > MAX) score = MAX;
        int delta = score - p.score;
        if (delta == 0) return 0;
        p.score = score;
        p.lastDelta = delta;
        p.lastReason = reason == null ? "手动调整" : reason;
        p.updatedAt = System.currentTimeMillis();
        addLog(p, delta, score, p.lastReason);
        saveAsync();
        return delta;
    }

    public static synchronized boolean setSummary(String uid, String summary) {
        Profile p = get(uid);
        if (p == null) return false;
        String s = summary == null ? "" : summary.trim();
        if (s.length() > MAX_SUMMARY) s = s.substring(0, MAX_SUMMARY);
        p.summary = s;
        p.updatedAt = System.currentTimeMillis();
        saveAsync();
        return true;
    }

    public static synchronized void reset(String uid) {
        Profile p = get(uid);
        if (p == null) return;
        p.score = AiConfig.affinityInit();
        p.summary = "";
        p.lastDelta = 0;
        p.lastReason = "";
        p.pending = 0;
        p.dayDelta = 0;
        p.dayKey = todayKey();
        p.updatedAt = System.currentTimeMillis();
        p.log.clear();
        saveAsync();
    }

    public static synchronized void remove(String uid) {
        if (uid == null) return;
        ensureLoaded();
        MAP.remove(uid);
        saveAsync();
    }

    public static synchronized void clearAll() {
        ensureLoaded();
        MAP.clear();
        saveAsync();
    }

    private static void addLog(Profile p, int delta, int score, String reason) {
        int keep = AiConfig.affinityLogKeep();
        if (keep <= 0) {
            p.log.clear();
            return;
        }
        p.log.add(new Change(System.currentTimeMillis(), delta, score, reason));
        while (p.log.size() > keep) p.log.remove(0);
    }

    private static void trimToLimit() {
        while (MAP.size() > MAX_PEOPLE) {
            String oldest = null;
            long t = Long.MAX_VALUE;
            for (Map.Entry<String, Profile> e : MAP.entrySet()) {
                if (e.getValue().lastAt < t) {
                    t = e.getValue().lastAt;
                    oldest = e.getKey();
                }
            }
            if (oldest == null) break;
            MAP.remove(oldest);
        }
    }

    private static int damp(int delta, int score) {
        if (delta > 0) {
            if (score >= 95) return 1;
            if (score >= 80) return Math.max(1, Math.round(delta * 0.5f));
            if (score >= 70) return Math.max(1, Math.round(delta * 0.75f));
        } else if (delta < 0) {
            if (score <= 5) return -1;
            if (score <= 20) return Math.min(-1, Math.round(delta * 0.5f));
            if (score <= 30) return Math.min(-1, Math.round(delta * 0.75f));
        }
        return delta;
    }

    private static void decay(Profile p) {
        if (!AiConfig.affinityDecay() || p == null || p.lastAt <= 0) return;
        long days = (System.currentTimeMillis() - p.lastAt) / 86400000L;
        int grace = AiConfig.affinityDecayDays();
        if (days <= grace) return;
        int target = (int) Math.min(days - grace, 90);
        if (target <= p.decayDays) return;
        int steps = target - p.decayDays;
        p.decayDays = target;
        int base = AiConfig.affinityInit();
        int old = p.score;
        if (p.score > base) p.score = Math.max(base, p.score - steps);
        else if (p.score < base) p.score = Math.min(base, p.score + steps);
        if (p.score != old) {
            p.lastDelta = p.score - old;
            p.lastReason = "好久没聊了";
            p.updatedAt = System.currentTimeMillis();
            HookLog.i("AI 好感度回落: " + p.displayName() + " " + old + " -> " + p.score);
            saveAsync();
        }
    }

    public static String toneHint(int score) {
        int t1 = AiConfig.affinityTier1();
        int t2 = AiConfig.affinityTier2();
        int t3 = AiConfig.affinityTier3();
        int t4 = AiConfig.affinityTier4();
        if (score < t1) {
            return "你对这人挺冷淡：短句、礼貌但不多聊，不主动找话题，也不用表情";
        }
        if (score < t2) {
            return "你们还不太熟：客气、有分寸，聊正事为主，偶尔接一句闲话就行";
        }
        if (score < t3) {
            return "算普通朋友：正常聊，随意一点，可以开个小玩笑";
        }
        if (score < t4) {
            return "你们挺熟：说话随意、爱搭腔、会互相调侃，表情也用得自然";
        }
        return "你们很亲近：像老朋友，会主动关心、撒娇、斗嘴";
    }

    private static int todayKey() {
        return Integer.parseInt(new SimpleDateFormat("yyyyMMdd", Locale.CHINA)
                .format(new Date()));
    }

    

    
    public static String promptFor(String uid) {
        if (!AiConfig.affinityOn()) return "";
        Profile p = get(uid);
        if (p == null) return "";
        StringBuilder sb = new StringBuilder("【关系档案】");
        sb.append(p.displayName());
        if (p.uin != null && p.uin.length() > 0) sb.append("（QQ ").append(p.uin).append("）");
        sb.append("｜好感度 ").append(p.score).append("/100（").append(tier(p.score)).append("）");

        if (p.summary != null && p.summary.trim().length() > 0) {
            sb.append("\n印象：").append(p.summary.trim());
        } else if (p.chats <= AiConfig.affinityEvery()) {
            sb.append("\n印象：（刚认识，还不太了解）");
        }

        if (!p.log.isEmpty()) {
            sb.append("\n最近：");
            int from = Math.max(0, p.log.size() - 2);
            for (int i = from; i < p.log.size(); i++) {
                if (i > from) sb.append("；");
                Change c = p.log.get(i);
                sb.append(c.delta > 0 ? "+" : "").append(c.delta);
                if (c.reason.length() > 0) sb.append(" ").append(c.reason);
            }
        }

        if (AiConfig.affinityTone()) {
            sb.append("\n语气：").append(toneHint(p.score));
        }
        sb.append("\n（不要说分数，也不要复述这段）");
        return sb.toString();
    }

    public static String tier(int score) {
        int t1 = AiConfig.affinityTier1();
        int t2 = AiConfig.affinityTier2();
        int t3 = AiConfig.affinityTier3();
        int t4 = AiConfig.affinityTier4();
        if (score < t1) return "冷淡";
        if (score < t2) return "客气";
        if (score < t3) return "普通";
        if (score < t4) return "熟络";
        return "亲近";
    }

    public static int tierColor(int score) {
        int t1 = AiConfig.affinityTier1();
        int t2 = AiConfig.affinityTier2();
        int t3 = AiConfig.affinityTier3();
        int t4 = AiConfig.affinityTier4();
        if (score < t1) return 0xFF9AA7B4;   
        if (score < t2) return 0xFF8FB8E0;   
        if (score < t3) return 0xFF7FCBB0;   
        if (score < t4) return 0xFFE85D9C;   
        return 0xFFFF8A5C;                   
    }
}
