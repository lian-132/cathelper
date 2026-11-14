package com.cathelperawa.owo.app.script;

import com.cathelperawa.owo.app.core.HookLog;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

public final class RouteRegistry {

    public static final class Route {
        public final String script;
        public final String id;

        public final int scope;
        final String[] match;
        final ScriptHandler handler;

        final AtomicLong hits = new AtomicLong();
        final AtomicLong errors = new AtomicLong();
        volatile long lastAt;

        Route(String script, String id, String[] match, ScriptHandler handler, int scope) {
            this.script = script;
            this.id = id;
            this.match = match;
            this.handler = handler;
            this.scope = scope;
        }

        public long hits() { return hits.get(); }
        public long errors() { return errors.get(); }
        public long lastAt() { return lastAt; }
        public String trigger() { return match.length > 0 ? match[0] : id; }
        public String key() { return script + "." + id; }
    }

    public static final class Match {
        public final Route route;
        public final String arg;
        Match(Route route, String arg) {
            this.route = route;
            this.arg = arg;
        }
    }

    private static final List<Route> ROUTES = new CopyOnWriteArrayList<Route>();

    private RouteRegistry() {}

    public static void add(String script, String id, String[] match, ScriptHandler h) {
        add(script, id, match, h, ScriptScope.ALL);
    }

    public static void add(String script, String id, String[] match, ScriptHandler h, int scope) {
        Route r = new Route(script, id, match, h, scope);
        for (int i = 0; i < ROUTES.size(); i++) {
            Route o = ROUTES.get(i);
            if (o.script.equals(script) && o.id.equals(id)) {
                ROUTES.set(i, r);
                return;
            }
        }
        ROUTES.add(r);
    }

    public static int removeByScript(String script) {
        if (script == null) return 0;
        int n = 0;
        List<Route> rm = new ArrayList<Route>();
        for (Route r : ROUTES) {
            if (r.script.equals(script)) rm.add(r);
        }
        for (Route r : rm) {
            if (ROUTES.remove(r)) n++;
        }
        return n;
    }

    public static void clear() { ROUTES.clear(); }

    public static List<Route> all() {
        return new ArrayList<Route>(ROUTES);
    }

    public static int size() { return ROUTES.size(); }

    public static int countOf(String script) {
        int n = 0;
        for (Route r : ROUTES) if (r.script.equals(script)) n++;
        return n;
    }

    public static Match find(ScriptMsg m) {
        if (m == null) return null;
        String text = m.text == null ? "" : m.text.trim();
        if (text.isEmpty()) return null;
        for (Route r : ROUTES) {

            if (!ScriptScope.allow(r.scope, m.group)) continue;
            for (String k : r.match) {
                if (k == null || k.length() == 0) continue;
                if (text.startsWith(k)) {
                    return new Match(r, text.substring(k.length()).trim());
                }
            }
        }
        return null;
    }

    static void markHit(Route r) {
        r.hits.incrementAndGet();
        r.lastAt = System.currentTimeMillis();
    }

    static void markError(Route r, Throwable t) {
        r.errors.incrementAndGet();
        HookLog.e("命令失败 " + r.key(), t);
    }
}
