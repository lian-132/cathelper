package com.cathelperawa.owo.app.ai;

import com.cathelperawa.owo.app.core.DataDir;

import com.cathelperawa.owo.app.bridge.QQEnv;
import com.cathelperawa.owo.app.core.HookLog;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class AiStore {

    public static final int MAX_PER_PEER = 120;

    public static final int MAX_PEERS = 120;

    public static final class Msg {
        public final String role;
        public final String content;
        public final long at;

        public final String uid;

        public double importance = 0.5;

        Msg(String role, String content, long at) {
            this(role, content, at, null);
        }

        Msg(String role, String content, long at, String uid) {
            this.role = role;
            this.content = content == null ? "" : content;
            this.at = at;
            this.uid = uid;
            this.importance = scoreImportance(role, this.content);
        }

        static double scoreImportance(String role, String c) {
            double v = 0.5;
            if (c == null) return v;
            String t = c;
            String[] facts = {"我叫", "我是", "我在", "我住", "我喜欢", "我讨厌",
                    "我的生日", "我养", "我工作", "我上", "记住", "别忘"};
            for (int i = 0; i < facts.length; i++) {
                if (t.indexOf(facts[i]) >= 0) { v += 0.2; break; }
            }
            if (t.length() > 200) v += 0.1;
            if ("assistant".equals(role) && t.length() < 20) v -= 0.1;
            return v < 0.1 ? 0.1 : (v > 1 ? 1 : v);
        }

        JSONObject toJson() {
            JSONObject o = new JSONObject();
            try {
                o.put("r", role);
                o.put("c", content);
                o.put("t", at);
                o.put("imp", importance);
            } catch (Throwable ignore) {
            }
            return o;
        }

        static Msg fromJson(JSONObject o) {
            Msg m = new Msg(o.optString("r", "user"), o.optString("c", ""),
                    o.optLong("t", 0), o.optString("u", null));
            if (o.has("imp")) m.importance = o.optDouble("imp", m.importance);
            return m;
        }
    }

    private static final Map<String, List<Msg>> MAP =
            new LinkedHashMap<String, List<Msg>>() {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, List<Msg>> e) {
                    return size() > MAX_PEERS;
                }
            };

    private AiStore() {}

    public static synchronized void add(String peer, String role, String content) {
        add(peer, role, content, null);
    }

    public static synchronized void add(String peer, String role, String content, String uid) {
        if (peer == null || peer.length() == 0) return;
        if (content == null || content.trim().length() == 0) return;
        ensureLoaded();
        List<Msg> list = MAP.get(peer);
        if (list == null) {
            list = new ArrayList<Msg>();
            MAP.put(peer, list);
        }
        list.add(new Msg(role, content, System.currentTimeMillis(), uid));
        while (list.size() > AiConfig.memLimit()) {
            list.remove(0);
        }
        saveAsync();
    }

    public static synchronized List<Msg> recent(String peer, int n) {
        List<Msg> out = new ArrayList<Msg>();
        if (peer == null) return out;
        ensureLoaded();
        List<Msg> list = MAP.get(peer);
        if (list == null) return out;
        int from = Math.max(0, list.size() - Math.max(0, n));
        for (int i = from; i < list.size(); i++) out.add(list.get(i));
        return out;
    }

    public static synchronized List<Msg> slice(String peer, int from, int to) {
        List<Msg> out = new ArrayList<Msg>();
        if (peer == null) return out;
        ensureLoaded();
        List<Msg> list = MAP.get(peer);
        if (list == null) return out;
        int n = list.size();
        int a = Math.max(0, Math.min(from, n));
        int b = Math.max(a, Math.min(to, n));
        for (int i = a; i < b; i++) out.add(list.get(i));
        return out;
    }

    public static synchronized List<Msg> search(String peer, String query, int n,
                                                List<Msg> exclude) {
        List<Msg> out = new ArrayList<Msg>();
        if (peer == null || query == null || query.trim().length() == 0) return out;
        ensureLoaded();
        List<Msg> list = MAP.get(peer);
        if (list == null || list.isEmpty()) return out;

        List<String> words = tokenize(query);
        if (words.isEmpty()) return out;

        java.util.Set<String> seen = new java.util.HashSet<String>();
        if (exclude != null) {
            for (int i = 0; i < exclude.size(); i++) {
                Msg m = exclude.get(i);
                if (m != null && m.content != null) seen.add(m.content.trim());
            }
        }

        long now = System.currentTimeMillis() / 1000L;
        List<Msg> hits = new ArrayList<Msg>();
        java.util.Map<Msg, Double> score = new java.util.HashMap<Msg, Double>();

        for (int i = 0; i < list.size(); i++) {
            Msg m = list.get(i);
            if (m == null || m.content == null) continue;
            String c = m.content;
            if (seen.contains(c.trim())) continue;
            if (c.length() < 4) continue;

            int hitCount = 0;
            String lc = c.toLowerCase();
            for (int w = 0; w < words.size(); w++) {
                if (lc.contains(words.get(w))) hitCount++;
            }
            if (hitCount == 0) continue;

            double sc = hitCount * 1.0;
            long ageDay = Math.max(0, (now - m.at) / 86400L);
            sc += Math.max(0d, 1.0 - ageDay * 0.05);
            if (m.content.length() > 400) sc -= 0.3;
            score.put(m, Double.valueOf(sc));
            hits.add(m);
        }

        java.util.Collections.sort(hits, new java.util.Comparator<Msg>() {
            @Override public int compare(Msg a, Msg b) {
                double sa = score.containsKey(a) ? score.get(a).doubleValue() : 0;
                double sb = score.containsKey(b) ? score.get(b).doubleValue() : 0;
                return sb > sa ? 1 : (sb < sa ? -1 : 0);
            }
        });

        for (int i = 0; i < hits.size() && out.size() < n; i++) out.add(hits.get(i));

        java.util.Collections.sort(out, new java.util.Comparator<Msg>() {
            @Override public int compare(Msg a, Msg b) {
                return a.at < b.at ? -1 : (a.at > b.at ? 1 : 0);
            }
        });
        return out;
    }

    static List<String> tokenize(String text) {
        List<String> words = new ArrayList<String>();
        if (text == null) return words;
        String t = text.trim();
        if (t.length() == 0) return words;

        t = t.replaceAll("\\[[^\\]]{0,40}\\]", " ");
        t = t.replaceAll("[\\p{Punct}\\s]+", " ");
        for (String seg : t.split("\\s+")) {
            if (seg.length() == 0) continue;
            if (seg.matches("[A-Za-z0-9_]+")) {
                if (seg.length() >= 2) words.add(seg.toLowerCase());
                continue;
            }

            for (int i = 0; i + 2 <= seg.length(); i++) {
                String w = seg.substring(i, i + 2);
                if (w.trim().length() < 2) continue;
                if (!words.contains(w)) words.add(w);
            }
        }
        if (words.size() > 24) words = new ArrayList<String>(words.subList(0, 24));
        return words;
    }

    public static synchronized int trimAll(int limit) {
        ensureLoaded();
        int removed = 0;
        for (java.util.Map.Entry<String, List<Msg>> e : MAP.entrySet()) {
            List<Msg> l = e.getValue();
            if (l == null) continue;
            while (l.size() > limit) {
                l.remove(0);
                removed++;
            }
        }
        if (removed > 0) saveAsync();
        return removed;
    }

    public static synchronized int maxSize() {
        ensureLoaded();
        int max = 0;
        for (List<Msg> l : MAP.values()) {
            if (l != null && l.size() > max) max = l.size();
        }
        return max;
    }

    public static synchronized List<Msg> searchAll(String query, int n) {
        List<Msg> out = new ArrayList<Msg>();
        if (query == null || query.trim().length() == 0) return out;
        ensureLoaded();
        List<Msg> hits = new ArrayList<Msg>();
        java.util.Map<Msg, Double> score = new java.util.HashMap<Msg, Double>();
        List<String> words = tokenize(query);
        if (words.isEmpty()) return out;
        long now = System.currentTimeMillis() / 1000L;

        for (List<Msg> list : MAP.values()) {
            if (list == null) continue;
            for (int i = 0; i < list.size(); i++) {
                Msg m = list.get(i);
                if (m == null || m.content == null || m.content.length() < 4) continue;
                int hit = 0;
                String lc = m.content.toLowerCase();
                for (int w = 0; w < words.size(); w++) {
                    if (lc.contains(words.get(w))) hit++;
                }
                if (hit == 0) continue;
                double sc = hit * 1.0 + m.importance * 0.5;
                long ageDay = Math.max(0, (now - m.at) / 86400L);
                sc += Math.max(0d, 1.0 - ageDay * 0.05);
                score.put(m, Double.valueOf(sc));
                hits.add(m);
            }
        }
        java.util.Collections.sort(hits, new java.util.Comparator<Msg>() {
            @Override public int compare(Msg a, Msg b) {
                double sa = score.containsKey(a) ? score.get(a).doubleValue() : 0;
                double sb = score.containsKey(b) ? score.get(b).doubleValue() : 0;
                return sb > sa ? 1 : (sb < sa ? -1 : 0);
            }
        });
        for (int i = 0; i < hits.size() && out.size() < n; i++) out.add(hits.get(i));
        return out;
    }

    private static final List<Msg> ARCHIVE = new ArrayList<Msg>();
    private static final String ARCHIVE_FILE = "ai_archive.json";

    public static synchronized int decay(int maxAgeDays) {
        ensureLoaded();
        if (maxAgeDays <= 0) maxAgeDays = 30;
        long now = System.currentTimeMillis() / 1000L;
        long cutoff = now - maxAgeDays * 86400L;
        int moved = 0;
        for (java.util.Map.Entry<String, List<Msg>> e : MAP.entrySet()) {
            List<Msg> list = e.getValue();
            if (list == null) continue;
            java.util.Iterator<Msg> it = list.iterator();
            while (it.hasNext()) {
                Msg m = it.next();
                if (m == null) { it.remove(); continue; }
                if (m.at <= 0) { it.remove(); continue; }
                if (m.at < cutoff && m.importance < 0.3) {
                    if (ARCHIVE.size() < 2000) ARCHIVE.add(m);
                    it.remove();
                    moved++;
                }
            }
        }
        if (moved > 0) {
            saveAsync();
            saveArchive();
            HookLog.i("记忆衰减：归档 " + moved + " 条（> " + maxAgeDays + " 天且不重要）");
        }
        return moved;
    }

    public static synchronized int archiveSize() {
        ensureLoaded();
        return ARCHIVE.size();
    }

    private static void saveArchive() {
        try {
            java.io.File f = new java.io.File(
                    com.cathelperawa.owo.app.core.DataDir.account("ai"), ARCHIVE_FILE);
            JSONArray arr = new JSONArray();
            for (int i = 0; i < ARCHIVE.size(); i++) arr.put(ARCHIVE.get(i).toJson());
            com.cathelperawa.owo.app.core.SecureFile.writeText(f, arr.toString());
        } catch (Throwable ignore) {
        }
    }

    public static synchronized void clear(String peer) {
        if (peer != null) MAP.remove(peer);
        saveAsync();
    }

    public static synchronized void clearAll() {
        MAP.clear();
        saveAsync();
    }

    public static synchronized int peerCount() {
        return MAP.size();
    }

    public static synchronized int size(String peer) {
        List<Msg> l = MAP.get(peer);
        return l == null ? 0 : l.size();
    }

    public static synchronized List<String> peers() {
        ensureLoaded();
        List<String> out = new ArrayList<String>(MAP.keySet());
        java.util.Collections.sort(out, new java.util.Comparator<String>() {
            @Override public int compare(String a, String b) {
                long ta = lastAt(a), tb = lastAt(b);
                return ta == tb ? a.compareTo(b) : (tb > ta ? 1 : -1);
            }
        });
        return out;
    }

    public static synchronized long lastAt(String peer) {
        List<Msg> l = MAP.get(peer);
        if (l == null || l.isEmpty()) return 0;
        return l.get(l.size() - 1).at;
    }

    public static synchronized List<Msg> messages(String peer) {
        ensureLoaded();
        List<Msg> l = MAP.get(peer);
        return l == null ? new ArrayList<Msg>() : new ArrayList<Msg>(l);
    }

    public static synchronized boolean setContent(String peer, int index, String text) {
        List<Msg> l = MAP.get(peer);
        if (l == null || index < 0 || index >= l.size()) return false;
        Msg old = l.get(index);
        l.set(index, new Msg(old.role, text == null ? "" : text.trim(), old.at));
        saveAsync();
        return true;
    }

    public static synchronized boolean removeAt(String peer, int index) {
        List<Msg> l = MAP.get(peer);
        if (l == null || index < 0 || index >= l.size()) return false;
        l.remove(index);
        if (l.isEmpty()) MAP.remove(peer);
        saveAsync();
        return true;
    }

    public static synchronized List<Msg> bySpeaker(String uid, String excludePeer, int limit) {
        ensureLoaded();
        List<Msg> hits = new ArrayList<Msg>();
        if (uid == null || uid.length() == 0 || limit <= 0) return hits;
        for (Map.Entry<String, List<Msg>> e : MAP.entrySet()) {
            String peer = e.getKey();
            if (excludePeer != null && excludePeer.equals(peer)) continue;
            if (peer.startsWith("u_")) continue;
            List<Msg> list = e.getValue();
            for (int i = 0; i < list.size(); i++) {
                Msg m = list.get(i);
                if (!"user".equals(m.role)) continue;
                if (!uid.equals(m.uid)) continue;
                hits.add(m);
            }
        }
        if (hits.size() > limit) {
            java.util.Collections.sort(hits, new java.util.Comparator<Msg>() {
                @Override public int compare(Msg a, Msg b) {
                    return a.at == b.at ? 0 : (a.at > b.at ? 1 : -1);
                }
            });
            hits = new ArrayList<Msg>(hits.subList(hits.size() - limit, hits.size()));
        }
        return hits;
    }

    public static synchronized List<Msg> recentBySpeaker(String uid, int limit) {
        ensureLoaded();
        List<Msg> hits = new ArrayList<Msg>();
        if (uid == null || uid.length() == 0 || limit <= 0) return hits;
        for (List<Msg> list : MAP.values()) {
            for (int i = 0; i < list.size(); i++) {
                Msg m = list.get(i);
                if (!"user".equals(m.role)) continue;
                if (!uid.equals(m.uid)) continue;
                hits.add(m);
            }
        }
        if (hits.size() > limit) {
            java.util.Collections.sort(hits, new java.util.Comparator<Msg>() {
                @Override public int compare(Msg a, Msg b) {
                    return a.at == b.at ? 0 : (a.at > b.at ? 1 : -1);
                }
            });
            hits = new ArrayList<Msg>(hits.subList(hits.size() - limit, hits.size()));
        }
        return hits;
    }

    public static synchronized int totalCount() {        ensureLoaded();
        int n = 0;
        for (List<Msg> l : MAP.values()) n += l.size();
        return n;
    }

    public static synchronized long updatedAt() {
        long t = 0;
        for (List<Msg> l : MAP.values()) {
            if (!l.isEmpty() && l.get(l.size() - 1).at > t) t = l.get(l.size() - 1).at;
        }
        return t;
    }

    private static final String FILE_NAME = "ai_history.json";

    private static final int MAX_CHARS = 600;
    private static final long MAX_FILE = 8L * 1024 * 1024;

    private static volatile boolean loaded = false;
    private static volatile boolean saveQueued = false;

    private static final java.util.concurrent.ExecutorService SAVER =
            java.util.concurrent.Executors.newSingleThreadExecutor(
                    new java.util.concurrent.ThreadFactory() {
                        @Override public Thread newThread(Runnable r) {
                            Thread t = new Thread(r, "cat-ai-store");
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
            JSONArray names = root.names();
            if (names == null) return;
            for (int i = 0; i < names.length(); i++) {
                String peer = names.getString(i);
                JSONArray arr = root.optJSONArray(peer);
                if (arr == null) continue;
                List<Msg> list = new ArrayList<Msg>();
                for (int k = 0; k < arr.length(); k++) {
                    JSONObject o = arr.optJSONObject(k);
                    if (o != null) list.add(Msg.fromJson(o));
                }
                if (list.isEmpty()) continue;
                while (list.size() > AiConfig.memLimit()) list.remove(0);
                MAP.put(peer, list);
            }
            HookLog.i("AI 记忆已载入 " + MAP.size() + " 个会话");

            if (st[0] == com.cathelperawa.owo.app.core.SecureFile.PLAIN) saveAsync();
        } catch (Throwable t) {
            HookLog.w("AI 记忆读取失败 " + t);
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
        synchronized (AiStore.class) { json = dumpJson(); }

        com.cathelperawa.owo.app.core.SecureFile.writeText(f, json);
    }

    public static synchronized String dumpJson() {
        JSONObject root = new JSONObject();
        try {
            for (Map.Entry<String, List<Msg>> e : MAP.entrySet()) {
                JSONArray arr = new JSONArray();
                for (int i = 0; i < e.getValue().size(); i++) {
                    Msg m = e.getValue().get(i);
                    if (m.content != null && m.content.length() > MAX_CHARS) {

                        m = new Msg(m.role, m.content.substring(0, MAX_CHARS), m.at);
                    }
                    arr.put(m.toJson());
                }
                root.put(e.getKey(), arr);
            }
        } catch (Throwable ignore) {
        }
        return root.toString();
    }
}
