package com.cathelperawa.owo.app.ai;

import com.cathelperawa.owo.app.api.EventApi;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public final class ChatLog {

    private ChatLog() {}

    static final int MAX_PER_PEER = 30;
    static final int MAX_PEERS = 80;

    public static final class Line {
        public final String uid;      
        public final String who;      
        public final String text;
        public final boolean self;
        public final long at;         
        
        public final Object raw;
        
        public String desc;

        Line(String uid, String who, String text, boolean self, long at, Object raw) {
            this.uid = uid;
            this.who = who == null ? "" : who;
            this.text = text == null ? "" : text;
            this.self = self;
            this.at = at;
            this.raw = raw;
        }
    }

    private static final Map<String, LinkedList<Line>> MAP =
            new LinkedHashMap<String, LinkedList<Line>>(16, 0.75f, true) {
                @Override protected boolean removeEldestEntry(
                        java.util.Map.Entry<String, LinkedList<Line>> oldest) {
                    return size() > MAX_PEERS;
                }
            };

    public static Line record(EventApi.Message m) {
        if (m == null) return null;
        String peer = m.peerUid;
        if (peer == null || peer.length() == 0) return null;
        String text = m.text == null ? "" : m.text.trim();
        if (text.length() == 0) return null;

        
        boolean self = m.self;
        return record(peer, m.senderUid, m.senderName, text, self, m.msgTime > 0
                ? m.msgTime * 1000L : System.currentTimeMillis(), m.raw);
    }

    public static Line record(String peer, String uid, String who, String text,
                              boolean self, long atMs) {
        return record(peer, uid, who, text, self, atMs, null);
    }

    public static Line record(String peer, String uid, String who, String text,
                              boolean self, long atMs, Object raw) {
        if (peer == null || peer.length() == 0) return null;
        synchronized (MAP) {
            LinkedList<Line> list = MAP.get(peer);
            if (list == null) {
                list = new LinkedList<Line>();
                MAP.put(peer, list);
            }
            
            Line last = list.isEmpty() ? null : list.getLast();
            if (last != null && last.self == self && last.text.equals(text)
                    && Math.abs(atMs - last.at) < 3000) {
                return last;
            }
            Line line = new Line(uid, who, text, self, atMs, raw);
            list.addLast(line);
            while (list.size() > MAX_PER_PEER) list.removeFirst();
            return line;
        }
    }

    public static List<Line> recent(String peer, int n) {
        List<Line> out = new ArrayList<Line>();
        if (peer == null || n <= 0) return out;
        synchronized (MAP) {
            LinkedList<Line> list = MAP.get(peer);
            if (list == null) return out;
            int from = Math.max(0, list.size() - n);
            for (int i = from; i < list.size(); i++) out.add(list.get(i));
        }
        return out;
    }

    public static int size(String peer) {
        synchronized (MAP) {
            LinkedList<Line> list = MAP.get(peer);
            return list == null ? 0 : list.size();
        }
    }

    public static void clear(String peer) {
        synchronized (MAP) {
            MAP.remove(peer);
        }
    }

    public static void clearAll() {
        synchronized (MAP) {
            MAP.clear();
        }
    }
}
