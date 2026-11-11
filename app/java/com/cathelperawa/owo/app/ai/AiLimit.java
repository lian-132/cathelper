package com.cathelperawa.owo.app.ai;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public final class AiLimit {

    private AiLimit() {}

    private static final int GLOBAL_WIN = 60;
    private static final int GLOBAL_MAX = 20;

    private static final Map<String, int[]> ROUTE = new HashMap<String, int[]>();
    private static final int[] DEFAULT = {60, 10};

    static {
        ROUTE.put(AiRouteRegistry.ID_WAKE, new int[]{10, 3});       
        ROUTE.put("help", new int[]{30, 3});
        ROUTE.put("tts", new int[]{60, 5});
        ROUTE.put("draw", new int[]{120, 2});                       
        ROUTE.put("chat", new int[]{60, 10});
    }

    private static final Map<String, LinkedList<Long>> WIN =
            new HashMap<String, LinkedList<Long>>();

    public static boolean check(String uid, String routeId) {
        if (uid == null || uid.length() == 0) return true;
        if (routeId == null) routeId = "default";
        
        try {
            if (AiConfig.isSelfUid(uid)) return true;
        } catch (Throwable ignore) {
        }

        long now = System.currentTimeMillis();

        synchronized (WIN) {
            if (!pass("g|" + uid, now, GLOBAL_WIN, GLOBAL_MAX)) {
                AiDebug.log("  · 限额：全局 " + GLOBAL_WIN + "s 内已超 " + GLOBAL_MAX + " 次");
                return false;
            }
            
            int[] cfg = ROUTE.get(routeId);
            if (cfg == null) cfg = DEFAULT;
            if (!pass("r|" + routeId + "|" + uid, now, cfg[0], cfg[1])) {
                AiDebug.log("  · 限额：" + routeId + " " + cfg[0] + "s 内已超 " + cfg[1] + " 次");
                return false;
            }
            return true;
        }
    }

    public static int remaining(String uid, String routeId) {
        if (uid == null) return -1;
        int[] cfg = ROUTE.get(routeId);
        if (cfg == null) cfg = DEFAULT;
        long now = System.currentTimeMillis();
        synchronized (WIN) {
            LinkedList<Long> q = WIN.get("r|" + routeId + "|" + uid);
            if (q == null) return cfg[1];
            long from = now - cfg[0] * 1000L;
            int used = 0;
            for (Long t : q) if (t != null && t > from) used++;
            return Math.max(0, cfg[1] - used);
        }
    }

    public static void reset(String uid) {
        synchronized (WIN) {
            if (uid == null) {
                WIN.clear();
                return;
            }
            java.util.Iterator<String> it = WIN.keySet().iterator();
            while (it.hasNext()) {
                if (it.next().endsWith("|" + uid)) it.remove();
            }
        }
    }

    private static boolean pass(String key, long now, int winSec, int max) {
        LinkedList<Long> q = WIN.get(key);
        if (q == null) {
            q = new LinkedList<Long>();
            WIN.put(key, q);
        }
        long from = now - winSec * 1000L;
        while (!q.isEmpty() && q.getFirst() != null && q.getFirst().longValue() < from) {
            q.removeFirst();
        }
        if (q.size() >= max) return false;
        q.addLast(Long.valueOf(now));
        
        while (q.size() > max * 2) q.removeFirst();
        return true;
    }
}
