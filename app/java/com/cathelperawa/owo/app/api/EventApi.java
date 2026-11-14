package com.cathelperawa.owo.app.api;

import com.cathelperawa.owo.app.bridge.QQEnv;
import com.cathelperawa.owo.app.feature.FeatureRecorder;
import com.cathelperawa.owo.app.util.Refl;

import com.cathelperawa.owo.app.core.HookLog;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public final class EventApi {

    private EventApi() {}

    public static final int QUEUE_SIZE = 512;

    private static final int MAX_ERRORS = 30;

    public static final class Message {

        public final Object raw;

        public final boolean recv;

        public final String peerUid;

        public final String senderUid;

        public final String senderName;

        public final String text;

        public final boolean group;

        public final boolean self;

        public final boolean atMe;

        public final int chatType;
        public final long msgId;
        public final long msgSeq;
        public final long msgTime;

        public final long queuedAt;

        Message(Object raw, boolean recv, String peerUid, String senderUid,
                String senderName, String text, boolean group, boolean self,
                boolean atMe, int chatType, long msgId, long msgSeq, long msgTime) {
            this.raw = raw;
            this.recv = recv;
            this.peerUid = peerUid == null ? "" : peerUid;
            this.senderUid = senderUid == null ? "" : senderUid;
            this.senderName = senderName == null ? "" : senderName;
            this.text = text == null ? "" : text;
            this.group = group;
            this.self = self;
            this.atMe = atMe;
            this.chatType = chatType;
            this.msgId = msgId;
            this.msgSeq = msgSeq;
            this.msgTime = msgTime;
            this.queuedAt = System.currentTimeMillis();
        }

        public String describe() {
            return (recv ? "[收] " : "[发] ")
                    + (group ? "群" : "私") + peerUid
                    + " from=" + senderUid
                    + (senderName.isEmpty() ? "" : "(" + senderName + ")")
                    + " :: " + text;
        }

        @Override
        public String toString() { return describe(); }
    }

    public interface Listener {
        void onMessage(Message m);
    }

    private static final CopyOnWriteArrayList<Listener> LISTENERS =
            new CopyOnWriteArrayList<Listener>();

    private static final ArrayBlockingQueue<Message> QUEUE =
            new ArrayBlockingQueue<Message>(QUEUE_SIZE);

    private static final AtomicLong N_DISPATCH = new AtomicLong();
    private static final AtomicLong N_DROP = new AtomicLong();
    private static final AtomicLong N_DELIVER = new AtomicLong();
    private static final AtomicLong N_ERROR = new AtomicLong();

    private static volatile ThreadPoolExecutor WORKER;
    private static volatile boolean stopped = false;

    private static volatile String selfUidCache;
    private static volatile long selfUidAt;

    private static long lastWarnAt;

    public static Listener onMessage(Listener l) {
        if (l == null) return null;
        if (!LISTENERS.contains(l)) {
            LISTENERS.add(l);
            HookLog.i("[事件] 新增监听器，当前 " + LISTENERS.size() + " 个");
        }
        ensureWorker();
        return l;
    }

    public static boolean offMessage(Listener l) {
        if (l == null) return false;
        boolean ok = LISTENERS.remove(l);
        if (ok) HookLog.i("[事件] 移除监听器，剩 " + LISTENERS.size() + " 个");
        return ok;
    }

    public static void clearListeners() {
        LISTENERS.clear();
    }

    public static int listenerCount() { return LISTENERS.size(); }

    public static boolean hasListeners() { return !LISTENERS.isEmpty(); }

    public static void dispatch(Object msgRecord, boolean recv) {
        if (msgRecord == null) return;
        if (LISTENERS.isEmpty()) return;
        if (stopped) return;
        try {
            ensureWorker();
            Message m = build(msgRecord, recv);
            if (m == null) return;
            N_DISPATCH.incrementAndGet();
            if (!QUEUE.offer(m)) {
                N_DROP.incrementAndGet();
                warnThrottled("[事件] 队列满，丢弃 " + m.describe());
            }
        } catch (Throwable t) {
            HookLog.e("[事件] dispatch 失败", t);
        }
    }

    private static Message build(Object rec, boolean recv) {
        String peer = Refl.firstStrOr(rec, null, "peerUid", "peerUin");
        String sender = Refl.firstStrOr(rec, null, "senderUid", "senderUin");
        String nick = Refl.firstStrOr(rec, "", "sendNickName", "sendMemberName");
        String text;
        try {
            text = FeatureRecorder.extractText(rec);
        } catch (Throwable t) {
            text = "";
        }

        try {
            if (com.cathelperawa.owo.app.script.PatchPoint.has("host.msg.recv")) {
                com.cathelperawa.owo.app.script.PatchPoint.PatchCtx pc =
                        new com.cathelperawa.owo.app.script.PatchPoint.PatchCtx("host.msg.recv", text);
                com.cathelperawa.owo.app.script.PatchPoint.emit(pc);
                if (pc.cancelled()) return null;
                text = pc.text();
            }
        } catch (Throwable ignore) {
        }

        boolean group = peer != null && !peer.startsWith("u_");
        String self = selfUid();
        boolean isSelf = self != null && sender != null && self.equals(sender);
        int chatType = Refl.fieldInt(rec, "chatType", group ? 2 : 1);
        long msgId = Refl.fieldLong(rec, "msgId", 0);
        long msgSeq = Refl.fieldLong(rec, "msgSeq", 0);
        long msgTime = Refl.fieldLong(rec, "msgTime", 0);
        return new Message(rec, recv, peer, sender, nick, text,
                group, isSelf, atMe(rec, self), chatType, msgId, msgSeq, msgTime);
    }

    private static boolean atMe(Object rec, String selfUid) {
        if (rec == null) return false;
        String selfUin = null;
        try {
            selfUin = QQEnv.currentUin();
        } catch (Throwable ignore) {
        }

        try {
            Object els = Refl.field(rec, "elements");
            if (els instanceof List) {
                java.util.List<?> l = (java.util.List<?>) els;
                for (int i = 0; i < l.size(); i++) {
                    Object el = l.get(i);
                    if (el == null) continue;

                    Object te = Refl.field(el, "textElement");
                    if (te != null) {
                        int atType = Refl.fieldInt(te, "atType", 0);
                        if (atType != 0) {
                            long atUid = Refl.fieldLong(te, "atUid", 0);
                            String atNtUid = Refl.firstStrOr(te, "", "atNtUid");
                            if (selfUin != null && atUid != 0
                                    && String.valueOf(atUid).equals(selfUin)) {
                                return true;
                            }
                            if (selfUid != null && atNtUid != null
                                    && atNtUid.equals(selfUid)) {
                                return true;
                            }

                            if (atType == 1) return true;
                        }
                    }

                    Object at = Refl.field(el, "atElement");
                    if (at != null) {
                        String cand = Refl.firstStrOr(at, "",
                                "uid", "uin", "atUid", "atUin", "targetUid",
                                "targetUin", "memberUid", "memberUin");
                        if (cand != null && cand.length() > 0) {
                            if (selfUid != null && cand.equals(selfUid)) return true;
                            if (selfUin != null && cand.equals(selfUin)) return true;
                            if (selfUin != null && selfUin.length() > 4
                                    && cand.contains(selfUin)) return true;
                        }
                    }
                }
            }
        } catch (Throwable ignore) {
        }

        try {
            String text = theTextOf(rec);
            if (text != null && text.length() > 0) {
                String nick = QQEnv.currentNickname();
                if (nick != null && nick.length() > 0 && text.contains("@" + nick)) {
                    return true;
                }

                if (selfUin != null && selfUin.length() > 4
                        && text.contains("[atUin=" + selfUin + "]")) {
                    return true;
                }

                if (text.contains("@")) {
                    dumpElements(rec, selfUid, selfUin, text);
                }
            }
        } catch (Throwable ignore) {
        }
        return false;
    }

    private static String theTextOf(Object rec) {
        try {
            Object els = Refl.field(rec, "elements");
            if (!(els instanceof List)) return "";
            StringBuilder sb = new StringBuilder();
            java.util.List<?> l = (java.util.List<?>) els;
            String cardBlob = null;
            for (int i = 0; i < l.size(); i++) {
                Object el = l.get(i);
                if (el == null) continue;
                Object te = Refl.field(el, "textElement");
                if (te != null) {
                    Object c = Refl.field(te, "content");
                    if (c instanceof String) sb.append((String) c);
                }

                Object ark = Refl.field(el, "arkElement");
                if (ark != null) {
                    Object bd = Refl.field(ark, "bytesData");
                    if (bd instanceof String) cardBlob = (String) bd;
                }
            }
            if (sb.indexOf("http") < 0 && cardBlob != null) {
                String urls = extractUrls(cardBlob);
                if (urls.length() > 0) sb.append(" [链接] ").append(urls);
            }
            return sb.toString();
        } catch (Throwable t) {
            return "";
        }
    }

    private static String extractUrls(String blob) {
        try {
            String s = blob.replace("\\/", "/");
            java.util.regex.Matcher m = java.util.regex.Pattern
                    .compile("https?://[^\\s\"'<>\\\\]+")
                    .matcher(s);
            StringBuilder out = new StringBuilder();
            java.util.LinkedHashSet<String> seen = new java.util.LinkedHashSet<String>();
            while (m.find() && seen.size() < 3) {
                String u = m.group();
                if (u.length() > 300) continue;
                if (seen.add(u)) {
                    if (out.length() > 0) out.append(' ');
                    out.append(u);
                }
            }
            return out.toString();
        } catch (Throwable t) {
            return "";
        }
    }

    private static void dumpElements(Object rec, String selfUid, String selfUin, String text) {
        try {
            StringBuilder sb = new StringBuilder("DUMP 群@未识别 selfUid=")
                    .append(selfUid).append(" selfUin=").append(selfUin)
                    .append(" text=").append(text);
            com.cathelperawa.owo.app.ai.AiDebug.log(sb.toString());

            Object els = Refl.field(rec, "elements");
            if (!(els instanceof List)) {
                com.cathelperawa.owo.app.ai.AiDebug.log("  elements 不是 List");
                return;
            }
            java.util.List<?> l = (java.util.List<?>) els;
            for (int i = 0; i < l.size() && i < 6; i++) {
                Object el = l.get(i);
                if (el == null) continue;
                com.cathelperawa.owo.app.ai.AiDebug.log(
                        "  element[" + i + "] " + el.getClass().getName());
                java.lang.reflect.Field[] fs = el.getClass().getDeclaredFields();
                for (int k = 0; k < fs.length; k++) {
                    try {
                        fs[k].setAccessible(true);
                        Object v = fs[k].get(el);
                        if (v == null) continue;
                        String vs = String.valueOf(v);
                        if (vs.length() > 120) vs = vs.substring(0, 120) + "…";
                        com.cathelperawa.owo.app.ai.AiDebug.log(
                                "    " + fs[k].getName() + " = " + vs);

                        if (fs[k].getName().toLowerCase().contains("at")) {
                            java.lang.reflect.Field[] f2 = v.getClass().getDeclaredFields();
                            for (int j = 0; j < f2.length; j++) {
                                try {
                                    f2[j].setAccessible(true);
                                    Object v2 = f2[j].get(v);
                                    if (v2 == null) continue;
                                    com.cathelperawa.owo.app.ai.AiDebug.log(
                                            "      ." + f2[j].getName() + " = "
                                                    + String.valueOf(v2));
                                } catch (Throwable ignore) {
                                }
                            }
                        }
                    } catch (Throwable ignore) {
                    }
                }
            }
        } catch (Throwable t) {
            try {
                com.cathelperawa.owo.app.ai.AiDebug.log("dump 失败 " + t);
            } catch (Throwable ignore) {
            }
        }
    }

    private static String selfUid() {
        long now = System.currentTimeMillis();
        if (now - selfUidAt < 5000) return selfUidCache;
        selfUidAt = now;
        try {
            selfUidCache = QQEnv.currentUid();
        } catch (Throwable t) {
            selfUidCache = null;
        }
        return selfUidCache;
    }

    private static void ensureWorker() {
        if (WORKER != null) return;
        synchronized (EventApi.class) {
            if (WORKER != null) return;
            ThreadFactory tf = new ThreadFactory() {
                @Override
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r, "cat-event");
                    t.setDaemon(true);
                    return t;
                }
            };
            ThreadPoolExecutor ex = new ThreadPoolExecutor(
                    1, 1, 30, TimeUnit.SECONDS,
                    new ArrayBlockingQueue<Runnable>(1), tf);
            ex.execute(new Runnable() {
                @Override
                public void run() {
                    loop();
                }
            });
            WORKER = ex;
            HookLog.i("[事件] 分发线程已启动");
        }
    }

    private static void loop() {
        while (!stopped) {
            Message m;
            try {
                m = QUEUE.poll(1, TimeUnit.SECONDS);
            } catch (InterruptedException ie) {
                return;
            }
            if (m == null) continue;
            deliver(m);
        }
    }

    private static void deliver(Message m) {
        List<Listener> snapshot = new ArrayList<Listener>(LISTENERS);
        for (int i = 0; i < snapshot.size(); i++) {
            Listener l = snapshot.get(i);
            if (l == null) continue;
            try {
                l.onMessage(m);
                N_DELIVER.incrementAndGet();
            } catch (Throwable t) {
                long n = N_ERROR.incrementAndGet();
                HookLog.e("[事件] 监听器异常（累计 " + n + "）", t);
                if (n % MAX_ERRORS == 0) {
                    HookLog.w("[事件] 监听器连续报错 " + n
                            + " 次，请检查脚本；当前仍保留（不会自动摘除）");
                }
            }
        }
    }

    private static void warnThrottled(String msg) {
        long now = System.currentTimeMillis();
        if (now - lastWarnAt < 5000) return;
        lastWarnAt = now;
        HookLog.w(msg);
    }

    public static JSONObject stats() {
        try {
            JSONObject o = new JSONObject();
            o.put("listeners", LISTENERS.size());
            o.put("queued", QUEUE.size());
            o.put("dispatch", N_DISPATCH.get());
            o.put("delivered", N_DELIVER.get());
            o.put("dropped", N_DROP.get());
            o.put("errors", N_ERROR.get());
            o.put("running", WORKER != null && !stopped);
            return o;
        } catch (Throwable t) {
            return null;
        }
    }

    public static String statsJson() {
        JSONObject o = stats();
        return o == null ? "{}" : o.toString();
    }

    public static long dispatchedCount() { return N_DISPATCH.get(); }
    public static long deliveredCount() { return N_DELIVER.get(); }
    public static long droppedCount() { return N_DROP.get(); }
    public static long errorCount() { return N_ERROR.get(); }
    public static int queuedCount() { return QUEUE.size(); }
    public static boolean isRunning() { return WORKER != null && !stopped; }

    public static void reset() {
        QUEUE.clear();
        N_DISPATCH.set(0);
        N_DROP.set(0);
        N_DELIVER.set(0);
        N_ERROR.set(0);
    }

}
