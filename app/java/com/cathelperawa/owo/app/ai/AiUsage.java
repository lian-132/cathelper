package com.cathelperawa.owo.app.ai;

import com.cathelperawa.owo.app.core.DataDir;

import com.cathelperawa.owo.app.bridge.QQEnv;
import com.cathelperawa.owo.app.core.HookLog;
import com.cathelperawa.owo.app.core.SecureFile;

import org.json.JSONObject;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;

public final class AiUsage {

    private AiUsage() {}

    private static final String FILE_NAME = "ai_usage.json";
    
    private static final int KEEP_DAYS = 60;

    public static final class Day {
        public long prompt;
        public long completion;
        public long hit;      
        public long miss;     
        public int calls;

        long cacheable() { return hit + miss; }

        double hitRate() {
            long c = cacheable();
            return c <= 0 ? -1d : (hit * 100d / c);
        }

        void add(Day o) {
            prompt += o.prompt;
            completion += o.completion;
            hit += o.hit;
            miss += o.miss;
            calls += o.calls;
        }
    }

    private static JSONObject data;      
    private static boolean loaded;
    private static volatile boolean saveQueued;

    

    
    public static void record(String model, JSONObject root) {
        if (root == null) return;
        try {
            JSONObject u = root.optJSONObject("usage");
            if (u == null) return;
            long prompt = u.optLong("prompt_tokens", 0);
            long completion = u.optLong("completion_tokens", 0);
            if (prompt <= 0 && completion <= 0) return;

            long hit = u.optLong("prompt_cache_hit_tokens", 0);
            long miss = u.optLong("prompt_cache_miss_tokens", 0);
            if (hit <= 0 && miss <= 0) {
                
                JSONObject det = u.optJSONObject("prompt_tokens_details");
                if (det != null) {
                    hit = det.optLong("cached_tokens", 0);
                    if (hit > 0) miss = Math.max(0, prompt - hit);
                }
            }
            if (hit <= 0 && miss <= 0) {
                
                long cr = u.optLong("cache_read_input_tokens", 0);
                if (cr > 0) {
                    hit = cr;
                    miss = Math.max(0, prompt - cr);
                }
            }
            recordRaw(model, prompt, completion, hit, miss);
        } catch (Throwable t) {
            
        }
    }

    public static void recordBody(String model, String body) {
        if (body == null || body.length() == 0) return;
        try {
            record(model, new JSONObject(body));
        } catch (Throwable ignore) {
        }
    }

    public static void recordRaw(String model, long prompt, long completion, long hit, long miss) {
        recordRaw(model, prompt, completion, hit, miss, source());
    }

    

    

    

    

    private static final ThreadLocal<String> SRC = new ThreadLocal<String>();

    public static void setSource(String s) {
        if (s == null || s.length() == 0) SRC.remove();
        else SRC.set(s);
    }

    public static void clearSource() {
        SRC.remove();
    }

    public static String source() {
        String s = SRC.get();
        return (s == null || s.length() == 0) ? "other" : s;
    }

    public static void recordRaw(String model, long prompt, long completion,
                                 long hit, long miss, String src) {
        try {
            ensureLoaded();
            String day = today();
            JSONObject days = data.optJSONObject("days");
            if (days == null) {
                days = new JSONObject();
                data.put("days", days);
            }
            JSONObject d = days.optJSONObject(day);
            if (d == null) {
                d = new JSONObject();
                days.put(day, d);
            }
            bump(d, prompt, completion, hit, miss, src);
            JSONObject tot = data.optJSONObject("total");
            if (tot == null) {
                tot = new JSONObject();
                data.put("total", tot);
            }
            bump(tot, prompt, completion, hit, miss, src);
            trimDays();
            saveAsync();
        } catch (Throwable t) {
            HookLog.w("AI 用量统计失败 " + t);
        }
    }

    private static void bump(JSONObject o, long prompt, long completion,
                             long hit, long miss, String src) throws Exception {
        o.put("prompt", o.optLong("prompt", 0) + prompt);
        o.put("completion", o.optLong("completion", 0) + completion);
        o.put("hit", o.optLong("hit", 0) + hit);
        o.put("miss", o.optLong("miss", 0) + miss);
        o.put("calls", o.optInt("calls", 0) + 1);

        JSONObject bySrc = o.optJSONObject("src");
        if (bySrc == null) {
            bySrc = new JSONObject();
            o.put("src", bySrc);
        }
        JSONObject one = bySrc.optJSONObject(src);
        if (one == null) {
            one = new JSONObject();
            bySrc.put(src, one);
        }
        one.put("calls", one.optInt("calls", 0) + 1);
        one.put("tokens", one.optLong("tokens", 0) + prompt + completion);
    }

    public static java.util.List<String[]> todaySources() {
        java.util.List<String[]> out = new java.util.ArrayList<String[]>();
        try {
            ensureLoaded();
            JSONObject days = data.optJSONObject("days");
            if (days == null) return out;
            JSONObject d = days.optJSONObject(today());
            if (d == null) return out;
            JSONObject bySrc = d.optJSONObject("src");
            if (bySrc == null) return out;
            java.util.Iterator<String> it = bySrc.keys();
            while (it.hasNext()) {
                String k = it.next();
                JSONObject one = bySrc.optJSONObject(k);
                if (one == null) continue;
                out.add(new String[]{k, String.valueOf(one.optInt("calls", 0))});
            }
            java.util.Collections.sort(out, new java.util.Comparator<String[]>() {
                @Override public int compare(String[] a, String[] b) {
                    return Integer.parseInt(b[1]) - Integer.parseInt(a[1]);
                }
            });
        } catch (Throwable ignore) {
        }
        return out;
    }

    

    
    public static long todayTokens() {
        Day d = day(today());
        return d.prompt + d.completion;
    }

    public static long todayPromptTokens() {
        return day(today()).prompt;
    }

    public static int todayCalls() {
        return day(today()).calls;
    }

    public static double hitRate() {
        return day(today()).hitRate();
    }

    public static long totalTokens() {
        ensureLoaded();
        try {
            JSONObject tot = data.optJSONObject("total");
            if (tot == null) return 0;
            return tot.optLong("prompt", 0) + tot.optLong("completion", 0);
        } catch (Throwable t) {
            return 0;
        }
    }

    public static Day day(String ymd) {
        ensureLoaded();
        Day out = new Day();
        try {
            JSONObject days = data.optJSONObject("days");
            if (days == null) return out;
            JSONObject o = days.optJSONObject(ymd);
            if (o == null) return out;
            out.prompt = o.optLong("prompt", 0);
            out.completion = o.optLong("completion", 0);
            out.hit = o.optLong("hit", 0);
            out.miss = o.optLong("miss", 0);
            out.calls = o.optInt("calls", 0);
        } catch (Throwable ignore) {
        }
        return out;
    }

    public static Day[] recentDays(int n) {
        ensureLoaded();
        if (n <= 0) n = 7;
        Day[] out = new Day[n];
        java.util.Calendar c = java.util.Calendar.getInstance();
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA);
        for (int i = 0; i < n; i++) {
            out[n - 1 - i] = day(f.format(c.getTime()));
            c.add(java.util.Calendar.DAY_OF_MONTH, -1);
        }
        return out;
    }

    public static String summary() {
        Day d = day(today());
        double r = d.hitRate();
        return "今日 " + d.calls + " 次调用 · " + fmt(d.prompt + d.completion) + " tokens"
                + "（进 " + fmt(d.prompt) + " / 出 " + fmt(d.completion) + "）"
                + (r < 0 ? " · 该模型不报缓存" : " · 缓存命中 " + String.format(Locale.CHINA, "%.0f%%", r));
    }

    public static void reset() {
        try {
            ensureLoaded();
            data = new JSONObject();
            data.put("days", new JSONObject());
            data.put("total", new JSONObject());
            saveNow();
        } catch (Throwable ignore) {
        }
    }

    

    public static String today() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(new Date());
    }

    private static String fmt(long n) {
        if (n >= 1000000) return String.format(Locale.CHINA, "%.1fM", n / 1000000d);
        if (n >= 1000) return String.format(Locale.CHINA, "%.1fk", n / 1000d);
        return String.valueOf(n);
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
        data = new JSONObject();
        try {
            File f = file();
            if (f == null || !f.exists() || f.length() == 0 || f.length() > 512 * 1024) return;
            int[] st = new int[1];
            String text = SecureFile.readText(f, st);
            if (text == null || text.length() == 0) return;
            JSONObject o = new JSONObject(text);
            if (o.optJSONObject("days") == null) o.put("days", new JSONObject());
            if (o.optJSONObject("total") == null) o.put("total", new JSONObject());
            data = o;
        } catch (Throwable t) {
            HookLog.w("AI 用量读取失败 " + t);
        }
    }

    private static void trimDays() {
        try {
            JSONObject days = data.optJSONObject("days");
            if (days == null) return;
            int n = days.length();
            if (n <= KEEP_DAYS) return;
            java.util.List<String> keys = new java.util.ArrayList<>();
            Iterator<String> it = days.keys();
            while (it.hasNext()) keys.add(it.next());
            java.util.Collections.sort(keys);
            for (int i = 0; i < keys.size() - KEEP_DAYS; i++) days.remove(keys.get(i));
        } catch (Throwable ignore) {
        }
    }

    private static void saveAsync() {
        if (saveQueued) return;
        saveQueued = true;
        try {
            com.cathelperawa.owo.app.core.Bg.run(new Runnable() {
                @Override public void run() {
                    try { Thread.sleep(1200); } catch (Throwable ignore) {}
                    saveQueued = false;
                    saveNow();
                }
            }, "cat-ai-usage");
        } catch (Throwable t) {
            saveQueued = false;
        }
    }

    public static void saveNow() {
        try {
            File f = file();
            if (f == null || data == null) return;
            String json;
            synchronized (AiUsage.class) {
                json = data.toString();
            }
            SecureFile.writeText(f, json);
        } catch (Throwable t) {
            HookLog.w("AI 用量写盘失败 " + t);
        }
    }
}
