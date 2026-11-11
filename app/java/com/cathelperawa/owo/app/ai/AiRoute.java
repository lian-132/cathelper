package com.cathelperawa.owo.app.ai;

import java.util.ArrayList;
import java.util.List;

public final class AiRoute {

    public final String id;
    
    public final String[] match;
    
    public final boolean needArg;

    public AiRoute(String id, boolean needArg, String... match) {
        this.id = id;
        this.needArg = needArg;
        this.match = match;
    }

    public static final class Hit {
        public final AiRoute route;
        public final String arg;

        Hit(AiRoute route, String arg) {
            this.route = route;
            this.arg = arg;
        }
    }

    public static Hit match(String text, List<AiRoute> routes) {
        if (text == null || routes == null) return null;
        String t = text.trim();
        if (t.length() == 0) return null;
        String lower = t.toLowerCase();

        List<Object[]> all = new ArrayList<Object[]>();
        for (int i = 0; i < routes.size(); i++) {
            AiRoute r = routes.get(i);
            if (r == null || r.match == null) continue;
            for (int j = 0; j < r.match.length; j++) {
                String w = r.match[j];
                if (w == null || w.trim().length() == 0) continue;
                all.add(new Object[]{r, w.trim()});
            }
        }
        java.util.Collections.sort(all, new java.util.Comparator<Object[]>() {
            @Override public int compare(Object[] a, Object[] b) {
                return ((String) b[1]).length() - ((String) a[1]).length();
            }
        });

        for (int i = 0; i < all.size(); i++) {
            AiRoute r = (AiRoute) all.get(i)[0];
            String w = (String) all.get(i)[1];
            if (!lower.startsWith(w.toLowerCase())) continue;
            String arg = t.substring(w.length()).trim();
            
            while (arg.length() > 0 && "，,：:、)）】".indexOf(arg.charAt(0)) >= 0) {
                arg = arg.substring(1).trim();
            }
            if (r.needArg && arg.length() == 0) continue;
            return new Hit(r, arg);
        }
        return null;
    }
}
