package com.cathelperawa.owo.app.ai;

import java.util.ArrayList;
import java.util.List;

public final class AiRouteRegistry {

    private AiRouteRegistry() {}

    public static final String ID_WAKE = "wake";

    public static final String ID_WAKE_ADMIN = "wake_admin";

    public static final String ID_TTS = "tts";

    public static final String ID_HELP = "help";

    private static final List<AiRoute> ROUTES = new ArrayList<AiRoute>();

    private static boolean inited;

    private static synchronized void ensureInit() {
        if (inited) return;
        inited = true;
        rebuild();
    }

    public static synchronized void rebuild() {
        ROUTES.clear();
        String raw = AiConfig.wakeWordsRaw();
        String[] ws = raw == null ? new String[0] : raw.split("[,，、\\s]+");
        List<String> keep = new ArrayList<String>();
        for (int i = 0; i < ws.length; i++) {
            String w = ws[i] == null ? "" : ws[i].trim();
            if (w.length() > 0 && !keep.contains(w)) keep.add(w);
        }
        if (keep.isEmpty()) keep.add("猫娘");
        ROUTES.add(new AiRoute(ID_WAKE, false, keep.toArray(new String[0])));

        String admin = AiConfig.adminWakeWordsRaw();
        String[] aws = admin.length() == 0 ? new String[0] : admin.split("[,，、\\s]+");
        List<String> akeep = new ArrayList<String>();
        for (int i = 0; i < aws.length; i++) {
            String w = aws[i] == null ? "" : aws[i].trim();
            if (w.length() > 0 && !akeep.contains(w) && !keep.contains(w)) akeep.add(w);
        }
        if (!akeep.isEmpty()) {
            ROUTES.add(new AiRoute(ID_WAKE_ADMIN, false, akeep.toArray(new String[0])));
        }

        ROUTES.add(new AiRoute(ID_HELP, false, new String[]{"帮助", "help", "菜单"}));

        if (AiConfig.ttsCmdOn()) {
            String tw = AiConfig.ttsWordsRaw();
            String[] tws = tw.split("[,，、\\s]+");
            List<String> tkeep = new ArrayList<String>();
            for (int i = 0; i < tws.length; i++) {
                String w = tws[i] == null ? "" : tws[i].trim();
                if (w.length() > 0 && !tkeep.contains(w)) tkeep.add(w);
            }
            if (!tkeep.isEmpty()) {
                ROUTES.add(new AiRoute(ID_TTS, true, tkeep.toArray(new String[0])));
            }
        }
    }

    public static synchronized void add(AiRoute r) {
        ensureInit();
        if (r == null) return;
        ROUTES.add(r);
    }

    public static synchronized List<AiRoute> all() {
        ensureInit();
        return new ArrayList<AiRoute>(ROUTES);
    }

    public static AiRoute.Hit match(String text) {
        ensureInit();
        synchronized (AiRouteRegistry.class) {
            return AiRoute.match(text, ROUTES);
        }
    }

    public static AiRoute.Hit hit(String text) {
        return match(text);
    }

    public static String wakeArgOf(String text) {
        AiRoute.Hit h = match(text);
        if (h == null || !ID_WAKE.equals(h.route.id)) return null;
        return h.arg;
    }
}
