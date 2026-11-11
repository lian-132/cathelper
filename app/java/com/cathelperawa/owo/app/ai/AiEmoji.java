package com.cathelperawa.owo.app.ai;

import android.content.Context;
import android.net.Uri;

import com.cathelperawa.owo.app.api.NetApi;
import com.cathelperawa.owo.app.bridge.QQEnv;
import com.cathelperawa.owo.app.core.ConfigStore;
import com.cathelperawa.owo.app.core.HookLog;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public final class AiEmoji {

    private AiEmoji() {}

    private static final String DIR = "emoji";
    private static final String INDEX = "index.json";
    
    private static final long MAX_READ = 2L * 1024 * 1024;
    
    private static final int PROMPT_MAX = 40;
    
    private static final int MAX_ZIP_ITEMS = 120;
    private static final long MAX_ZIP_ITEM_BYTES = 8L * 1024 * 1024;

    public static final class Item {
        public String file = "";
        public String label = "";
        public long t = 0;
        public int use = 0;

        public JSONObject toJson() {
            JSONObject o = new JSONObject();
            try {
                o.put("f", file);
                o.put("label", label);
                o.put("t", t);
                o.put("use", use);
            } catch (Throwable ignore) {
            }
            return o;
        }

        static Item fromJson(JSONObject o) {
            Item it = new Item();
            it.file = o.optString("f", "");
            it.label = o.optString("label", "");
            it.t = o.optLong("t", 0);
            it.use = o.optInt("use", 0);
            return it;
        }
    }

    

    public static File dir() {
        try {
            File g = com.cathelperawa.owo.app.core.DataDir.global();
            File d = new File(g == null
                    ? new File(QQEnv.appContext().getFilesDir(), "cathelper/" + DIR)
                    : g, DIR);
            if (!d.exists()) d.mkdirs();
            return d;
        } catch (Throwable t) {
            return null;
        }
    }

    private static File indexFile() {
        File d = dir();
        return d == null ? null : new File(d, INDEX);
    }

    public static synchronized List<Item> load() {
        List<Item> out = new ArrayList<Item>();
        File f = indexFile();
        if (f == null || !f.exists() || f.length() == 0) return out;
        try {
            byte[] raw = readFileBytes(f, 4L * 1024 * 1024);
            if (raw == null) return out;
            JSONArray arr = new JSONArray(new String(raw, "UTF-8"));
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o == null) continue;
                Item it = Item.fromJson(o);
                if (it.file.length() == 0) continue;
                File img = new File(dir(), it.file);
                if (!img.exists()) continue;          
                if (it.label == null || it.label.length() == 0) it.label = "未分类";
                out.add(it);
            }
        } catch (Throwable t) {
            HookLog.w("表情库读取失败 " + t);
        }
        return out;
    }

    public static synchronized boolean save(List<Item> list) {
        File f = indexFile();
        if (f == null) return false;
        try {
            JSONArray arr = new JSONArray();
            if (list != null) {
                for (int i = 0; i < list.size(); i++) arr.put(list.get(i).toJson());
            }
            byte[] b = arr.toString().getBytes("UTF-8");
            FileOutputStream out = new FileOutputStream(f);
            out.write(b);
            out.flush();
            out.close();
            return true;
        } catch (Throwable t) {
            HookLog.w("表情库保存失败 " + t);
            return false;
        }
    }

    public static int count() {
        return load().size();
    }

    

    
    public static Item importUri(Context ctx, Uri uri) {
        if (ctx == null || uri == null) return null;
        File d = dir();
        if (d == null) return null;
        String name = String.valueOf(System.currentTimeMillis());
        String ext = extOf(uri.toString());
        File out = new File(d, name + ext);
        try {
            InputStream in = ctx.getContentResolver().openInputStream(uri);
            if (in == null) return null;
            FileOutputStream os = new FileOutputStream(out);
            byte[] buf = new byte[8192];
            int n;
            long total = 0;
            while ((n = in.read(buf)) > 0) {
                os.write(buf, 0, n);
                total += n;
                if (total > 8L * 1024 * 1024) break;
            }
            os.flush();
            os.close();
            in.close();
        } catch (Throwable t) {
            HookLog.w("表情导入失败 " + t);
            return null;
        }
        List<Item> list = load();
        Item it = new Item();
        it.file = out.getName();
        it.label = "未分类";
        it.t = System.currentTimeMillis();
        list.add(it);
        save(list);
        return it;
    }

    public static int importZip(Context ctx, Uri uri) {
        if (ctx == null || uri == null) return 0;
        File d = dir();
        if (d == null) return 0;
        List<Item> list = load();
        int ok = 0;
        java.util.zip.ZipInputStream zin = null;
        try {
            InputStream in = ctx.getContentResolver().openInputStream(uri);
            if (in == null) return 0;
            zin = new java.util.zip.ZipInputStream(new java.io.BufferedInputStream(in));
            java.util.zip.ZipEntry e;
            while ((e = zin.getNextEntry()) != null) {
                if (ok >= MAX_ZIP_ITEMS) break;
                if (e.isDirectory()) continue;
                String name = e.getName() == null ? "" : e.getName();
                if (!isImageName(name)) continue;
                String ext = extOf(name);
                File out = new File(d, System.currentTimeMillis() + "-" + ok + ext);
                long total = 0;
                boolean bad = false;
                FileOutputStream os = null;
                try {
                    os = new FileOutputStream(out);
                    byte[] buf = new byte[8192];
                    int n;
                    while ((n = zin.read(buf)) > 0) {
                        os.write(buf, 0, n);
                        total += n;
                        if (total > MAX_ZIP_ITEM_BYTES) { bad = true; break; }
                    }
                    os.flush();
                } catch (Throwable t) {
                    bad = true;
                } finally {
                    try { if (os != null) os.close(); } catch (Throwable ignore) {}
                }
                if (bad || total < 100) {
                    try { out.delete(); } catch (Throwable ignore) {}
                    continue;
                }
                Item it = new Item();
                it.file = out.getName();
                it.label = "未分类";
                it.t = System.currentTimeMillis();
                list.add(it);
                ok++;
            }
        } catch (Throwable t) {
            HookLog.w("表情压缩包导入失败 " + t);
        } finally {
            try { if (zin != null) zin.close(); } catch (Throwable ignore) {}
        }
        if (ok > 0) save(list);
        return ok;
    }

    private static boolean isImageName(String n) {
        String s = n == null ? "" : n.toLowerCase();
        return s.endsWith(".jpg") || s.endsWith(".jpeg") || s.endsWith(".png")
                || s.endsWith(".gif") || s.endsWith(".webp") || s.endsWith(".bmp");
    }

    public static synchronized boolean remove(String file) {
        List<Item> list = load();
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).file.equals(file)) {
                list.remove(i);
                save(list);
                try {
                    File f = new File(dir(), file);
                    if (f.exists()) f.delete();
                } catch (Throwable ignore) {
                }
                return true;
            }
        }
        return false;
    }

    public static synchronized boolean rename(String file, String label) {
        String l = label == null ? "" : label.trim();
        if (l.length() == 0) return false;
        List<Item> list = load();
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).file.equals(file)) {
                list.get(i).label = l;
                save(list);
                return true;
            }
        }
        return false;
    }

    

    
    private static final String KEY_GROUPS = "ai_emoji_groups";

    public static synchronized List<String> groups() {
        List<String> out = new ArrayList<String>();
        try {
            String raw = ConfigStore.getString(KEY_GROUPS);
            if (raw == null || raw.trim().length() == 0) return out;
            String[] arr = raw.split(",");
            for (String a : arr) {
                String v = a == null ? "" : a.trim();
                if (v.length() > 0 && !out.contains(v)) out.add(v);
            }
        } catch (Throwable ignore) {
        }
        return out;
    }

    private static synchronized void saveGroups(List<String> g) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < g.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append(g.get(i).replace(",", " "));
        }
        ConfigStore.setString(KEY_GROUPS, sb.toString());
    }

    public static synchronized boolean addGroup(String name) {
        String n = name == null ? "" : name.trim();
        if (n.length() == 0) return false;
        List<String> g = groups();
        if (g.contains(n)) return false;
        g.add(n);
        saveGroups(g);
        return true;
    }

    public static synchronized boolean removeGroup(String name) {
        String n = name == null ? "" : name.trim();
        if (n.length() == 0) return false;
        List<String> g = groups();
        if (g.remove(n)) saveGroups(g);
        List<Item> list = load();
        boolean changed = false;
        for (int i = 0; i < list.size(); i++) {
            if (n.equals(list.get(i).label)) {
                list.get(i).label = "";
                changed = true;
            }
        }
        if (changed) save(list);
        return true;
    }

    public static synchronized boolean renameGroup(String from, String to) {
        String f = from == null ? "" : from.trim();
        String t = to == null ? "" : to.trim();
        if (f.length() == 0 || t.length() == 0 || f.equals(t)) return false;
        List<String> g = groups();
        int idx = g.indexOf(f);
        if (idx >= 0) {
            g.set(idx, t);
            saveGroups(g);
        } else if (!g.contains(t)) {
            g.add(t);
            saveGroups(g);
        }
        List<Item> list = load();
        for (int i = 0; i < list.size(); i++) {
            if (f.equals(list.get(i).label)) list.get(i).label = t;
        }
        save(list);
        return true;
    }

    public static synchronized boolean moveTo(String file, String label) {
        List<Item> list = load();
        String l = label == null ? "" : label.trim();
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).file.equals(file)) {
                list.get(i).label = l;
                save(list);
                return true;
            }
        }
        return false;
    }

    public static synchronized List<Item> byLabel(String label) {
        List<Item> out = new ArrayList<Item>();
        List<Item> all = load();
        String want = "未分类".equals(label) ? "" : (label == null ? "" : label.trim());
        for (int i = 0; i < all.size(); i++) {
            String l = all.get(i).label == null ? "" : all.get(i).label;
            if (l.equals(want)) out.add(all.get(i));
        }
        return out;
    }

    

    
    public static String promptFor() {
        return promptFor(true);
    }

    public static String catalogFor() {
        List<Item> list = load();
        if (list.isEmpty()) return "";
        StringBuilder sb = new StringBuilder("\n\n【表情包】你可以偶尔发个表情包。可用的有：");
        int n = 0;
        for (int i = 0; i < list.size() && n < PROMPT_MAX; i++, n++) {
            if (n > 0) sb.append("、");
            sb.append(list.get(i).label);
        }
        if (list.size() > PROMPT_MAX) sb.append(" 等").append(list.size()).append("个");
        sb.append("\n想发就在回复的最后单独写一行：[表情:标签]（一条消息最多一个）。")
          .append("有情绪、想接梗的时候都可以写，不写也行。");
        sb.append("\n★ 发表情时最好配一句话（哪怕就「喏」「拿去」两个字），")
          .append("别连着几次都只丢一张图，那样很假。");
        sb.append("\n★ 千万别写「（发了个表情包：懵圈）」「（发了个表情）」这类描述性文字——")
          .append("那不是真发表情，只会被当成普通消息发出去丢人。要发就只写 [表情:标签]。");
        return sb.toString();
    }

    public static String turnHint(boolean allow, String peer) {
        List<Item> list = load();
        if (list.isEmpty()) return "";
        StringBuilder sb = new StringBuilder("\n【本轮表情】");
        if (allow) {
            sb.append("这次可以发表情。");
        } else {
            sb.append("这次不发。别主动说「接住」「给你」这类像要发图的话；")
              .append("但如果对方问起要表情/图片，就用你自己的语气随口回一句")
              .append("（比如「这次先不发了」「没找到合适的」），")
              .append("别解释成概率、系统、设置这些，别出戏、别道歉半天。");
        }
        List<String> recent = recentSent(peer);
        if (!recent.isEmpty()) {
            sb.append("\n【你最近发出去的表情】");
            for (int i = 0; i < recent.size(); i++) {
                sb.append("\n").append(i + 1).append(". ").append(labelOfFile(list, recent.get(i)));
            }
            sb.append("\n如果对方问起这几张，而你不确定，就只回一行 [看图:序号]（多张写 [看图:1,2]），")
              .append("程序会把图发给你，你再回答。别靠猜、别编。");
        }
        return sb.toString();
    }

    public static String promptFor(boolean allow) {
        return promptFor(allow, null);
    }

    public static String promptFor(boolean allow, String peer) {
        List<Item> list = load();
        if (list.isEmpty()) return "";
        if (!allow) {
            return "\n\n【表情包】这次不发。"
                    + "别主动说「接住」「给你」这类像要发图的话；"
                    + "但如果对方问起要表情/图片，就用你自己的语气随口回一句"
                    + "（比如「这次先不发了」「没找到合适的」），"
                    + "别解释成概率、系统、设置这些，别出戏、别道歉半天。"
                    + "\n★ 也别写「（发了个表情包：xxx）」这种描述——那不是发表情，"
                    + "只会被当成普通文字发出去，等于说了谎。";
        }
        StringBuilder sb = new StringBuilder(
                "\n\n【表情包】你可以偶尔发个表情包。可用的有：");
        int n = 0;
        for (int i = 0; i < list.size() && n < PROMPT_MAX; i++, n++) {
            if (n > 0) sb.append("、");
            sb.append(list.get(i).label);
        }
        if (list.size() > PROMPT_MAX) sb.append(" 等").append(list.size()).append("个");
        sb.append("\n这次可以发表情：想发就在回复的最后单独写一行：[表情:标签]（一条消息最多一个）。")
          .append("有情绪、想接梗的时候都可以写，不写也行。");

        sb.append("\n★ 发表情时最好配一句话（哪怕就「喏」「拿去」两个字），"
                + "别连着几次都只丢一张图，那样很假。");
        sb.append("\n★ 千万别写「（发了个表情包：懵圈）」「（发了个表情）」这类描述性文字——")
          .append("那不是真发表情，只会被当成普通消息发出去丢人。要发就只写 [表情:标签]。");

        List<String> recent = recentSent(peer);
        if (!recent.isEmpty()) {
            sb.append("\n\n【你最近发出去的表情】");
            for (int i = 0; i < recent.size(); i++) {
                sb.append("\n").append(i + 1).append(". ").append(labelOfFile(list, recent.get(i)));
            }
            sb.append("\n如果对方问起这几张（比如「刚才那个是什么」「两张一样吗」），"
                    + "而你不确定，就只回一行 [看图:序号]（要看好几张就写 [看图:1,2]），"
                    + "程序会把图发给你，你再回答。别靠猜、别编。");
        }
        return sb.toString();
    }

    private static final java.util.Map<String, List<String>> LAST =
            new java.util.HashMap<String, List<String>>();
    private static final int LAST_MAX = 3;

    public static Item pick(String label, String peer) {
        List<Item> list = load();
        if (label == null) return null;
        String q = label.trim().toLowerCase();
        List<Item> hits = new ArrayList<Item>();
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).label != null
                    && list.get(i).label.toLowerCase().equals(q)) hits.add(list.get(i));
        }
        if (hits.isEmpty()) {
            for (int i = 0; i < list.size(); i++) {
                String l = list.get(i).label == null ? "" : list.get(i).label.toLowerCase();
                if (l.contains(q) || q.contains(l)) hits.add(list.get(i));
            }
        }
        if (hits.isEmpty()) return null;

        String last = lastSentFile(peer);
        List<Item> pool = new ArrayList<Item>();
        for (int i = 0; i < hits.size(); i++) {
            if (last != null && last.equals(hits.get(i).file)) continue;
            pool.add(hits.get(i));
        }
        if (pool.isEmpty()) pool = hits;

        Item it = pool.get((int) (Math.random() * pool.size()));
        if (peer != null) recordSent(peer, it.file);
        return it;
    }

    public static synchronized void recordSent(String peer, String file) {
        if (peer == null || file == null) return;
        List<String> q = LAST.get(peer);
        if (q == null) {
            q = new ArrayList<String>();
            LAST.put(peer, q);
        }
        q.remove(file);
        q.add(0, file);
        while (q.size() > LAST_MAX) q.remove(q.size() - 1);
    }

    private static String labelOfFile(List<Item> list, String file) {
        for (int i = 0; list != null && i < list.size(); i++) {
            if (list.get(i).file.equals(file)) return list.get(i).label;
        }
        return "表情";
    }

    public static synchronized List<String> recentSent(String peer) {
        List<String> q = LAST.get(peer);
        return q == null ? new ArrayList<String>() : new ArrayList<String>(q);
    }

    public static String lastSentFile(String peer) {
        List<String> q = recentSent(peer);
        return q.isEmpty() ? null : q.get(0);
    }

    public static Item match(String label) {
        if (label == null) return null;
        String q = label.trim().toLowerCase();
        if (q.length() == 0) return null;
        List<Item> list = load();
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).label.toLowerCase().equals(q)) return list.get(i);
        }
        for (int i = 0; i < list.size(); i++) {
            String l = list.get(i).label.toLowerCase();
            if (l.contains(q) || q.contains(l)) return list.get(i);
        }
        return null;
    }

    public static File fileOf(Item it) {
        if (it == null) return null;
        File d = dir();
        return d == null ? null : new File(d, it.file);
    }

    public static synchronized void markUsed(String file) {
        List<Item> list = load();
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).file.equals(file)) {
                list.get(i).use++;
                save(list);
                return;
            }
        }
    }

    

    
    public static String classify(File img) {
        AiProvider p = AiProvider.current();
        if (p == null) {
            lastReason = "没有可用的模型提供商";
            AiDebug.log("  ✗ 表情分类: 没有可用的模型提供商");
            return null;
        }
        if (img == null || !img.isFile()) {
            lastReason = "文件不存在";
            AiDebug.log("  ✗ 表情分类: 文件不存在 " + img);
            return null;
        }
        try {
            byte[] raw = prepareImage(img);
            if (raw == null || raw.length < 100) {
                lastReason = "读不到图片或太大";
                AiDebug.log("  ✗ 表情分类: 读不到图片或太小 " + img.getName()
                        + "（" + img.length() / 1024 + "KB）");
                return null;
            }
            String b64 = android.util.Base64.encodeToString(raw, android.util.Base64.NO_WRAP);

            JSONArray content = new JSONArray();
            JSONObject t = new JSONObject();
            t.put("type", "text");
            t.put("text", "这张表情包表达什么情绪或意思？用不超过 6 个字的词概括（中文），"
                    + "只回答这个词本身，不要标点、不要解释。比如：无语、笑死、摸摸头、生气");
            content.put(t);
            JSONObject iu = new JSONObject();
            iu.put("url", "data:image/jpeg;base64," + b64);
            JSONObject wrap = new JSONObject();
            wrap.put("type", "image_url");
            wrap.put("image_url", iu);
            content.put(wrap);

            JSONObject msg = new JSONObject();
            msg.put("role", "user");
            msg.put("content", content);

            JSONArray msgs = new JSONArray();
            msgs.put(msg);

            JSONObject body = new JSONObject();
            body.put("model", p.model);
            body.put("messages", msgs);

            body.put("max_tokens", 1024);
            body.put("temperature", 0.3);

            JSONObject h = new JSONObject();
            h.put("Authorization", "Bearer " + p.apiKey);
            h.put("Content-Type", "application/json");

            String[] urls = chatUrls(p.baseUrl);
            NetApi.Result r = null;
            String url = urls.length > 0 ? urls[0] : chatUrl(p.baseUrl);
            AiDebug.log("  · 表情分类 model=" + p.model + " 图 "
                    + (raw.length / 1024) + "KB 地址 " + url);
            for (int ui = 0; ui < urls.length; ui++) {
                r = NetApi.postSync(urls[ui], h.toString(), body.toString(), 45000);
                if (r != null && r.is2xx()) { url = urls[ui]; break; }
                if (r != null && r.code != 404 && r.code != 405) { url = urls[ui]; break; }
            }
            if (r == null || !r.is2xx()) {
                String why = r == null ? "null" : (r.code + " " + head(r.body));
                lastReason = "HTTP " + (r == null ? "?" : String.valueOf(r.code));
                HookLog.w("表情分类请求失败 " + why);
                com.cathelperawa.owo.app.ai.AiDebug.log("  ✗ 表情分类失败 " + why);
                return null;
            }
            if (r.body == null || r.body.length() == 0) {
                AiDebug.log("  ✗ 表情分类: 返回空 body");
                return null;
            }
            JSONObject o = new JSONObject(r.body);
            JSONArray ch = o.optJSONArray("choices");
            if (ch == null || ch.length() == 0) {
                AiDebug.log("  ✗ 表情分类: 返回里没有 choices → " + head(r.body));
                return null;
            }
            JSONObject m0 = ch.optJSONObject(0).optJSONObject("message");
            if (m0 == null) return null;
            String text = m0.optString("content", "");
            if (text.trim().length() == 0) {
                String fin = ch.optJSONObject(0).optString("finish_reason", "?");
                lastReason = "模型没输出内容（" + fin + "）";
                AiDebug.log("  ✗ 表情分类: content 为空（finish=" + fin + "）");
                String rc = m0.optString("reasoning_content", "");
                AiDebug.log("  · reasoning 长度 " + rc.length());
            }
            text = text.replace("\n", " ").replaceAll("[\\p{Punct}，。！？、；：“”‘’…（）]", " ").trim();
            if (text.length() > 12) text = text.substring(0, 12);
            return text.length() == 0 ? null : text;
        } catch (Throwable t) {
            HookLog.w("表情分类异常 " + t);
            AiDebug.log("  ✗ 表情分类异常 " + t);
            return null;
        }
    }

    public static volatile boolean classifying = false;
    public static volatile int clsDone = 0;
    public static volatile int clsTotal = 0;
    public static volatile int clsOk = 0;
    public static volatile String clsError = "";
    
    public static volatile String lastReason = "";

    public static int countUnclassified() {
        List<Item> list = load();
        int n = 0;
        for (int i = 0; i < list.size(); i++) {
            Item it = list.get(i);
            if (it.label == null || it.label.length() == 0 || "未分类".equals(it.label)) n++;
        }
        return n;
    }

    public static synchronized void startClassify() {
        if (classifying) return;
        classifying = true;
        clsDone = 0;
        clsOk = 0;
        clsError = "";
        lastReason = "";
        clsTotal = countUnclassified();
        Thread t = new Thread(new Runnable() {
            @Override public void run() {
                try {
                    List<Item> list = load();
                    for (int i = 0; i < list.size(); i++) {
                        Item it = list.get(i);
                        if (it.label != null && it.label.length() > 0
                                && !"未分类".equals(it.label)) continue;
                        long t0 = System.currentTimeMillis();
                        String label = classify(fileOf(it));
                        if (label != null && label.length() > 0) {
                            it.label = label;
                            save(list);
                            clsOk++;
                        } else if (clsError.length() == 0) {
                            String why = lastReason == null || lastReason.length() == 0
                                    ? "未知原因" : lastReason;
                            clsError = why + "（第 " + (clsDone + 1) + " 张）";
                        }
                        clsDone++;
                        if (clsDone >= clsTotal) break;
                    }
                } catch (Throwable t) {
                    clsError = "异常 " + t;
                } finally {
                    classifying = false;
                    com.cathelperawa.owo.app.ai.AiDebug.log("表情分类结束：成功 " + clsOk
                            + "/" + clsTotal + (clsError.length() > 0 ? " · " + clsError : ""));
                }
            }
        }, "cat-emoji-cls");
        t.setDaemon(true);
        t.start();
    }

    public interface Progress {
        void onOne(int done, int total);
    }

    public static int classifyAll() {
        return classifyAll(null);
    }

    public static int classifyAll(Progress p) {
        List<Item> list = load();
        int total = 0;
        for (int i = 0; i < list.size(); i++) {
            Item it = list.get(i);
            if (it.label == null || it.label.length() == 0 || "未分类".equals(it.label)) total++;
        }
        int done = 0;
        int ok = 0;
        for (int i = 0; i < list.size(); i++) {
            Item it = list.get(i);
            if (it.label != null && it.label.length() > 0 && !"未分类".equals(it.label)) continue;
            String label = classify(fileOf(it));
            if (label != null && label.length() > 0) {
                it.label = label;
                save(list);
                ok++;
            }
            done++;
            if (p != null) {
                try { p.onOne(done, total); } catch (Throwable ignore) {}
            }
        }
        return ok;
    }

    

    public static String chatUrl(String baseUrl) {
        String[] all = chatUrls(baseUrl);
        return all.length == 0 ? "" : all[0];
    }

    public static String[] chatUrls(String base) {
        String b = base == null ? "" : base.trim();
        while (b.endsWith("/")) b = b.substring(0, b.length() - 1);
        if (b.length() == 0) return new String[0];
        if (b.endsWith("/chat/completions")) return new String[]{b};
        if (b.endsWith("/v1") || b.endsWith("/v1beta") || b.endsWith("/openai")) {
            return new String[]{b + "/chat/completions"};
        }
        return new String[]{b + "/v1/chat/completions", b + "/chat/completions"};
    }

    private static String extOf(String s) {
        String t = s == null ? "" : s.toLowerCase();
        if (t.contains(".png")) return ".png";
        if (t.contains(".gif")) return ".gif";
        if (t.contains(".webp")) return ".webp";
        if (t.contains(".bmp")) return ".bmp";
        return ".jpg";
    }

    public static byte[] modelBytes(File f) {
        return prepareImage(f);
    }

    private static byte[] prepareImage(File f) {
        byte[] raw = readFileBytes(f, 16L * 1024 * 1024);
        if (raw == null || raw.length < 100) return null;
        if (raw.length <= 1200 * 1024) return raw;
        try {
            android.graphics.BitmapFactory.Options o =
                    new android.graphics.BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            android.graphics.BitmapFactory.decodeFile(f.getAbsolutePath(), o);
            if (o.outWidth <= 0 || o.outHeight <= 0) return raw;
            int scale = 1;
            int maxSide = Math.max(o.outWidth, o.outHeight);
            while (maxSide / scale > 768) scale *= 2;
            android.graphics.BitmapFactory.Options o2 =
                    new android.graphics.BitmapFactory.Options();
            o2.inSampleSize = scale;
            if (raw.length > 6L * 1024 * 1024) o2.inPreferredConfig = android.graphics.Bitmap.Config.RGB_565;
            android.graphics.Bitmap bm =
                    android.graphics.BitmapFactory.decodeFile(f.getAbsolutePath(), o2);
            if (bm == null) return raw;
            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
            bm.compress(android.graphics.Bitmap.CompressFormat.JPEG, 82, bos);
            byte[] out = bos.toByteArray();
            try { bm.recycle(); } catch (Throwable ignore) {}
            AiDebug.log("  · 表情图压缩 " + (raw.length / 1024) + "KB → " + (out.length / 1024) + "KB");
            return out.length > 100 ? out : raw;
        } catch (Throwable t) {
            return raw;
        }
    }

    private static String head(String s) {
        if (s == null) return "";
        String t = s.replace("\n", " ");
        return t.length() <= 160 ? t : t.substring(0, 160) + "…";
    }

    private static byte[] readFileBytes(File f, long max) {
        try {
            if (f == null || !f.isFile()) return null;
            long len = f.length();
            if (len <= 0 || len > max) return null;
            byte[] out = new byte[(int) len];
            java.io.FileInputStream in = new java.io.FileInputStream(f);
            int off = 0;
            while (off < out.length) {
                int n = in.read(out, off, out.length - off);
                if (n <= 0) break;
                off += n;
            }
            in.close();
            return out;
        } catch (Throwable t) {
            return null;
        }
    }
}
