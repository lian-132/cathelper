package com.cathelperawa.owo.app.script;

import com.cathelperawa.owo.app.api.EventApi;
import com.cathelperawa.owo.app.core.Bg;
import com.cathelperawa.owo.app.core.HookLog;

public final class ScriptRouter implements EventApi.Listener {

    private static volatile ScriptRouter INSTANCE;
    private static volatile boolean installed = false;

    private static final long MIN_GAP_MS = 300;

    private static final Object SCRIPT_LOCK = new Object();

    static Object lock() { return SCRIPT_LOCK; }

    private ScriptRouter() {}

    public static synchronized void install() {
        if (installed) return;
        INSTANCE = new ScriptRouter();
        EventApi.onMessage(INSTANCE);
        installed = true;
        HookLog.i("[脚本] 路由器已挂上消息事件");
    }

    public static synchronized void uninstall() {
        if (!installed || INSTANCE == null) return;
        EventApi.offMessage(INSTANCE);
        installed = false;
        HookLog.i("[脚本] 路由器已摘除");
    }

    public static boolean isInstalled() { return installed; }

    @Override public void onMessage(EventApi.Message m) {
        if (m == null) return;
        final ScriptMsg sm = convert(m);

        
        
        ScriptMessageRegistry.dispatch(sm);

        
        if (m.self && SentGuard.isRecent(m.text)) return;

        final RouteRegistry.Match hit = RouteRegistry.find(sm);
        if (hit == null) return;

        final RouteRegistry.Route r = hit.route;

        long now = System.currentTimeMillis();
        if (now - r.lastAt() < MIN_GAP_MS) return;

        RouteRegistry.markHit(r);
        
        ScriptExec.run(r.script, new Runnable() {
            @Override public void run() {
                try {
                    r.handler.handle(sm, hit.arg);
                } catch (Throwable t) {
                    RouteRegistry.markError(r, t);
                }
            }
        });
    }

    private static java.util.List<Object> elementsOf(Object raw) {
        try {
            Object els = com.cathelperawa.owo.app.util.Refl.field(raw, "elements");
            if (els instanceof java.util.List) return (java.util.List<Object>) els;
        } catch (Throwable ignore) {
        }
        return null;
    }

    static String[] extractAts(Object raw) {
        java.util.List<Object> els = elementsOf(raw);
        if (els == null) return new String[0];
        java.util.ArrayList<String> out = new java.util.ArrayList<String>();
        for (int i = 0; i < els.size(); i++) {
            Object el = els.get(i);
            if (el == null) continue;
            try {
                Object at = com.cathelperawa.owo.app.util.Refl.field(el, "atElement");
                if (at == null) continue;
                String uid = com.cathelperawa.owo.app.util.Refl.firstStrOr(at, "", "uid", "uin");
                if (uid != null && uid.length() > 0) out.add(uid);
            } catch (Throwable ignore) {
            }
        }
        return out.toArray(new String[0]);
    }

    static String[] extractPics(Object raw) {
        java.util.List<Object> els = elementsOf(raw);
        if (els == null) return new String[0];
        java.util.ArrayList<String> out = new java.util.ArrayList<String>();
        for (int i = 0; i < els.size(); i++) {
            Object el = els.get(i);
            if (el == null) continue;
            try {
                Object pic = com.cathelperawa.owo.app.util.Refl.field(el, "picElement");
                if (pic == null) continue;
                String name = com.cathelperawa.owo.app.util.Refl.firstStrOr(pic, "",
                        "fileName", "filePath", "sourcePath", "md5HexStr");
                if (name != null && name.length() > 0) out.add(name);
            } catch (Throwable ignore) {
            }
        }
        return out.toArray(new String[0]);
    }

    static long extractReplyId(Object raw) {
        try {
            Object rp = com.cathelperawa.owo.app.util.Refl.field(raw, "replyElement");
            if (rp == null) return 0L;
            Object v = com.cathelperawa.owo.app.util.Refl.field(rp, "replayMsgId");
            if (v == null) v = com.cathelperawa.owo.app.util.Refl.field(rp, "replyMsgId");
            if (v == null) v = com.cathelperawa.owo.app.util.Refl.field(rp, "sourceMsgId");
            if (v instanceof Number) return ((Number) v).longValue();
            if (v instanceof String) return Long.parseLong((String) v);
        } catch (Throwable ignore) {
        }
        return 0L;
    }

    static ScriptMsg convert(EventApi.Message m) {
        String senderName = m.senderName == null ? "" : m.senderName;
        String peerName = "";
        try {
            if (m.group) {
                peerName = com.cathelperawa.owo.app.api.IdentityApi.troopName(m.peerUid);
            } else {
                
                peerName = senderName;
            }
        } catch (Throwable ignore) {
        }
        String[] ats = extractAts(m.raw);
        String[] pics = extractPics(m.raw);
        long reply = extractReplyId(m.raw);
        return new ScriptMsg(
                m.peerUid, peerName, m.senderUid, senderName, m.text,
                m.group, m.atMe, m.self, m.msgId, m.msgTime, ats, pics, reply, m.raw);
    }
}
