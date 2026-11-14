package com.cathelperawa.owo.app.protocol;

import java.util.LinkedHashMap;
import java.util.Map;

public final class MsgCache {

    private MsgCache() {}

    private static final int MAX = 800;

    private static final LinkedHashMap<Long, Object> MAP =
            new LinkedHashMap<Long, Object>(64, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<Long, Object> e) {
                    return size() > MAX;
                }
            };

    private static final LinkedHashMap<Long, Object> BY_SEQ =
            new LinkedHashMap<Long, Object>(64, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<Long, Object> e) {
                    return size() > MAX;
                }
            };

    public static void put(long msgId, Object record) {
        put(msgId, 0, record);
    }

    public static void put(long msgId, long msgSeq, Object record) {
        if (record == null) return;
        synchronized (MAP) {
            if (msgId != 0) MAP.put(msgId, record);
            if (msgSeq != 0) BY_SEQ.put(msgSeq, record);
        }
    }

    public static Object getBySeq(long seq) {
        if (seq == 0) return null;
        synchronized (MAP) {
            return BY_SEQ.get(seq);
        }
    }

    public static Object get(long msgId) {
        if (msgId == 0) return null;
        synchronized (MAP) {
            return MAP.get(msgId);
        }
    }

    public static int size() {
        synchronized (MAP) {
            return MAP.size();
        }
    }

    public static int seqSize() {
        synchronized (MAP) {
            return BY_SEQ.size();
        }
    }
}
