package com.cathelperawa.owo.app.ai;

import com.cathelperawa.owo.app.core.ConfigStore;

public final class AiConfig {

    private AiConfig() {}

    public static boolean enable() { return get("ai_enable", false); }

    public static boolean cacheOpt() { return get("ai_cache_opt", true); }

    public static boolean imgCacheOn() { return get("ai_img_cache", false); }

    public static boolean adminWakeOn() { return get("ai_admin_wake", false); }

    public static void setAdminWakeOn(boolean on) { setBool("ai_admin_wake", on); }

    public static String wakeWordsRaw() {
        String v = ConfigStore.getString("ai_wake_words");
        if (v == null || v.trim().length() == 0) return "助手,小助手";
        return v.trim();
    }

    public static void setWakeWords(String raw) {
        setString("ai_wake_words", raw == null ? "" : raw.trim());
        AiRouteRegistry.rebuild();
    }

    public static String adminWakeWordsRaw() {
        String v = ConfigStore.getString("ai_wake_admin_words");
        return v == null ? "" : v.trim();
    }

    public static void setAdminWakeWords(String raw) {
        setString("ai_wake_admin_words", raw == null ? "" : raw.trim());
        AiRouteRegistry.rebuild();
    }

    public static boolean ttsCmdOn() { return get("ai_tts_cmd", false); }

    public static String ttsWordsRaw() {
        String v = ConfigStore.getString("ai_tts_words");
        if (v == null || v.trim().length() == 0) return "语音,tts,念";
        return v.trim();
    }

    public static void setTtsWords(String raw) {
        setString("ai_tts_words", raw == null ? "" : raw.trim());
        AiRouteRegistry.rebuild();
    }

    public static int memLimit() {
        int v = ConfigStore.getInt("ai_mem_limit");
        if (v <= 0) return 120;
        return Math.max(20, Math.min(v, 500));
    }

    public static void setMemLimit(int v) {
        setInt("ai_mem_limit", Math.max(20, Math.min(v, 500)));
    }

    public static String ttsKey() {
        String v = ConfigStore.getString("ai_tts_key");
        return v == null ? "" : v.trim();
    }

    public static void setTtsKey(String v) { setString("ai_tts_key", v == null ? "" : v); }

    public static String ttsBaseUrl() {
        String v = ConfigStore.getString("ai_tts_url");
        if (v == null || v.trim().length() == 0) return "https://api.openai.com/v1";
        return v.trim();
    }

    public static void setTtsBaseUrl(String v) { setString("ai_tts_url", v == null ? "" : v); }

    public static String ttsModel() {
        String v = ConfigStore.getString("ai_tts_model");
        if (v == null || v.trim().length() == 0) return "tts-1";
        return v.trim();
    }

    public static void setTtsModel(String v) { setString("ai_tts_model", v == null ? "" : v); }

    public static String ttsVoice() {
        String v = ConfigStore.getString("ai_tts_voice");
        if (v == null || v.trim().length() == 0) return "alloy";
        return v.trim();
    }

    public static void setTtsVoice(String v) { setString("ai_tts_voice", v == null ? "" : v); }

    public static boolean ttsUseApi() { return ttsKey().length() > 0; }

    public static boolean personalityOn() { return get("ai_personality", true); }

    public static boolean moodOn() { return get("ai_mood", true); }

    public static int wakeCooldownSec() {
        int v = ConfigStore.getInt("ai_wake_cooldown");
        return v <= 0 ? 10 : Math.min(v, 300);
    }

    public static boolean privateReply() { return get("ai_private", true); }

    public static boolean atReply() { return get("ai_at", true); }

    public static boolean keywordReply() { return get("ai_keyword", false); }

    public static boolean quoteReply() { return get("ai_quote", false); }

    public static final int DEFAULT_HISTORY = 20;
    public static final int MAX_HISTORY = 60;

    public static int historyLimit() {
        int v = ConfigStore.getInt("ai_history_limit");
        if (v <= 0) return DEFAULT_HISTORY;
        return Math.min(v, MAX_HISTORY);
    }

    public static int cooldownSec() {
        int v = ConfigStore.getInt("ai_cooldown");
        return v <= 0 ? 0 : Math.min(v, 3600);
    }

    public static int hourlyLimit() {
        int v = ConfigStore.getInt("ai_hourly");
        return v <= 0 ? 0 : Math.min(v, 9999);
    }

    public static String persona() {
        try {
            String s = ConfigStore.getString("ai_persona");
            return s == null ? "" : s;
        } catch (Throwable t) {
            return "";
        }
    }

    public static boolean vision() { return get("ai_vision", true); }

    public static String visionProviderId() {
        try {
            String s = ConfigStore.getString("ai_vision_provider");
            return s == null ? "" : s;
        } catch (Throwable t) {
            return "";
        }
    }

    public static void setVisionProviderId(String id) {
        try {
            ConfigStore.setString("ai_vision_provider", id == null ? "" : id);
        } catch (Throwable ignore) {
        }
    }

    public static boolean cross() { return get("ai_cross", true); }

    public static boolean pttText() { return get("ai_ptt_text", true); }

    public static boolean pttAsr() { return get("ai_ptt_asr", true); }

    public static boolean pttAsrLocal() { return get("ai_ptt_asr_local", false); }

    public static boolean voice() { return get("ai_voice", false); }

    public static boolean emojiOn() { return get("ai_emoji", true); }

    public static int emojiPct() {
        int pct = ConfigStore.getInt("ai_emoji_pct");
        if (pct <= 0) {
            int old = ConfigStore.getInt("ai_emoji_rate");
            if (old == 1) pct = 30;
            else if (old >= 2) pct = 70;
            else pct = ConfigStore.getString("ai_emoji_pct") == null ? 30 : 0;
        }
        if (pct < 0) pct = 0;
        if (pct > 100) pct = 100;
        return pct;
    }

    public static boolean proactiveOn() { return get("ai_proactive", false); }

    public static int proactivePct() {
        int v = ConfigStore.getInt("ai_proactive_pct");
        if (v < 0) v = 0;
        if (v > 100) v = 100;
        return v == 0 ? 10 : v;
    }

    public static int proactiveGapSec() {
        int v = ConfigStore.getInt("ai_proactive_gap");
        if (v <= 0) v = 180;
        return Math.min(v, 7200);
    }

    public static boolean followUpOn() { return get("ai_followup", true); }

    public static boolean selfJudge() { return get("ai_selfjudge", true); }

    public static int followUpSec() {
        int v = ConfigStore.getInt("ai_followup_sec");
        if (v <= 0) v = 120;
        return Math.min(v, 1800);
    }

    public static int mergeMs() {
        int v = ConfigStore.getInt("ai_merge_ms");
        if (v < 0) v = 0;
        if (v > 5000) v = 5000;
        return v;
    }

    public static boolean segmentEnabled() { return get("ai_segment", true); }
    public static int segMin() { int v = ConfigStore.getInt("ai_seg_min"); return v <= 0 ? 1 : v; }
    public static int segMax() { int v = ConfigStore.getInt("ai_seg_max"); return v < segMin() ? segMin() + 1 : v; }
    public static int segParts() { int v = ConfigStore.getInt("ai_seg_parts"); return v <= 0 ? 1 : Math.min(v, 8); }

    public static String keywordsRaw() {
        String s = ConfigStore.getString("ai_keywords");
        return s == null ? "" : s;
    }

    public static boolean matchKeyword(String text) {
        if (text == null || text.length() == 0) return false;
        String raw = keywordsRaw().trim();
        if (raw.length() == 0) return false;
        String[] ks = raw.split("[,，\\s|｜]+");
        String low = text.toLowerCase();
        for (int i = 0; i < ks.length; i++) {
            String k = ks[i].trim();
            if (k.length() == 0) continue;
            if (low.contains(k.toLowerCase())) return true;
        }
        return false;
    }

    public static boolean peerAllowed(String peerUid) {
        String raw = ConfigStore.getString("ai_targets");
        if (raw == null) return true;
        raw = raw.trim();
        if (raw.length() == 0) return true;
        if (peerUid == null) return false;

        String[] ts = raw.split("[,，\\s|｜]+");
        String uin = null;
        for (int i = 0; i < ts.length; i++) {
            String t = ts[i].trim();
            if (t.length() == 0) continue;
            if (t.equals(peerUid)) return true;
            if (uin == null) {
                try {
                    uin = com.cathelperawa.owo.app.api.IdentityApi.uinFromUid(peerUid);
                } catch (Throwable ignore) {
                }
            }
            if (uin != null && uin.equals(t)) return true;
        }
        return false;
    }

    public static String targetsRaw() {
        String s = ConfigStore.getString("ai_targets");
        return s == null ? "" : s;
    }

    public static boolean groupAllowed(String peer) {
        String raw = ConfigStore.getString("ai_groups");
        if (raw == null) return true;
        raw = raw.trim();
        if (raw.length() == 0) return true;
        if (peer == null) return false;
        String[] gs = raw.split("[,，\\s|｜]+");
        for (int i = 0; i < gs.length; i++) {
            if (gs[i].trim().equals(peer)) return true;
        }
        return false;
    }

    public static boolean affinityOn() { return get("ai_affinity", true); }

    public static boolean affinityAuto() { return get("ai_affinity_auto", true); }

    public static int affinityEvery() {
        int v = ConfigStore.getInt("ai_affinity_every");
        if (v <= 0) v = 2;
        return Math.min(v, 20);
    }

    public static int affinityGapSec() {
        int v = ConfigStore.getInt("ai_affinity_gap");
        if (v <= 0) v = 90;
        return Math.min(v, 3600);
    }

    public static int affinityMaxStep() {
        int v = ConfigStore.getInt("ai_affinity_step");
        if (v <= 0) v = 10;
        return Math.min(v, 50);
    }

    public static int affinityDailyCap() {
        int v = ConfigStore.getInt("ai_affinity_daily");
        if (v < 0) v = 0;
        return Math.min(v, 100);
    }

    public static int affinityInit() {
        int v = ConfigStore.getInt("ai_affinity_init");
        if (v <= 0) v = 50;
        if (v < 0) v = 0;
        return Math.min(v, 100);
    }

    public static int affinityLogKeep() {
        int v = ConfigStore.getInt("ai_affinity_log");
        if (v < 0) v = 0;
        return Math.min(v, 20);
    }

    public static boolean affinityTone() { return get("ai_affinity_tone", true); }

    public static boolean affinityDecay() { return get("ai_affinity_decay", true); }

    public static int affinityDecayDays() {
        int v = ConfigStore.getInt("ai_affinity_decay_days");
        if (v <= 0) v = 14;
        return Math.min(v, 365);
    }

    public static int affinityTier1() { return tierValue("ai_affinity_t1", 25); }
    public static int affinityTier2() { return tierValue("ai_affinity_t2", 45); }
    public static int affinityTier3() { return tierValue("ai_affinity_t3", 65); }
    public static int affinityTier4() { return tierValue("ai_affinity_t4", 85); }

    private static int tierValue(String key, int def) {
        int v = ConfigStore.getInt(key);
        if (v <= 0) v = def;
        if (v < 1) v = 1;
        if (v > 100) v = 100;
        return v;
    }

    public static String adminsRaw() {
        String s = ConfigStore.getString("ai_admins");
        return s == null ? "" : s.trim();
    }

    public static java.util.List<String> admins() {
        java.util.List<String> out = new java.util.ArrayList<String>();
        String raw = adminsRaw();
        if (raw.length() == 0) return out;
        String[] arr = raw.split("[,，\\s|｜]+");
        for (int i = 0; i < arr.length; i++) {
            String v = arr[i].trim();
            if (v.length() > 0 && !out.contains(v)) out.add(v);
        }
        return out;
    }

    public static int adminCount() {
        return admins().size();
    }

    public static boolean isAdminUin(String uin) {
        if (uin == null || uin.length() == 0) return false;
        return admins().contains(uin.trim());
    }

    public static boolean isAdminUid(String uid) {
        if (uid == null || uid.length() == 0) return false;
        try {
            return isAdminUin(com.cathelperawa.owo.app.api.IdentityApi.uinFromUid(uid));
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean isSelfUid(String uid) {
        if (uid == null || uid.length() == 0) return false;
        try {
            return uid.equals(com.cathelperawa.owo.app.api.IdentityApi.currentUid());
        } catch (Throwable t) {
            return false;
        }
    }

    public static void setAdmins(java.util.List<String> list) {
        StringBuilder sb = new StringBuilder();
        if (list != null) {
            for (int i = 0; i < list.size(); i++) {
                String v = list.get(i);
                if (v == null || v.trim().length() == 0) continue;
                if (sb.length() > 0) sb.append(',');
                sb.append(v.trim());
            }
        }
        setString("ai_admins", sb.toString());
    }

    public static void addAdmin(String uin) {
        if (uin == null || uin.trim().length() == 0) return;
        java.util.List<String> list = admins();
        String v = uin.trim();
        if (!list.contains(v)) list.add(v);
        setAdmins(list);
    }

    public static void removeAdmin(String uin) {
        java.util.List<String> list = admins();
        list.remove(uin == null ? "" : uin.trim());
        setAdmins(list);
    }

    public static boolean getBool(String key) { return get(key, false); }

    public static int getInt(String key) {
        try {
            return ConfigStore.getInt(key);
        } catch (Throwable t) {
            return 0;
        }
    }

    private static boolean get(String key, boolean def) {
        try {
            return ConfigStore.getBool(key);
        } catch (Throwable t) {
            return def;
        }
    }

    public static void setBool(String key, boolean v) {
        try {
            ConfigStore.setBool(key, v);
        } catch (Throwable ignore) {
        }
    }

    public static void setInt(String key, int v) {
        try {
            ConfigStore.setInt(key, v);
        } catch (Throwable ignore) {
        }
    }

    public static void setString(String key, String v) {
        try {
            ConfigStore.setString(key, v == null ? "" : v);
        } catch (Throwable ignore) {
        }
    }
}
