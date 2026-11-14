package com.cathelperawa.owo.app.script;

import java.util.ArrayList;
import java.util.List;

public final class SentGuard {

    private static final long WINDOW_MS = 3000;
    private static final int MAX = 20;

    private static final class Item {
        final String text;
        final long at;

        Item(String text, long at) {
            this.text = text;
            this.at = at;
        }
    }

    private static final List<Item> RECENT = new ArrayList<Item>();

    private SentGuard() {}

    public static void mark(String text) {
        if (text == null || text.length() == 0) return;
        synchronized (RECENT) {
            RECENT.add(new Item(text, System.currentTimeMillis()));
            while (RECENT.size() > MAX) RECENT.remove(0);
        }
    }

    public static boolean isRecent(String text) {
        if (text == null || text.length() == 0) return false;
        long now = System.currentTimeMillis();
        synchronized (RECENT) {
            for (int i = RECENT.size() - 1; i >= 0; i--) {
                Item it = RECENT.get(i);
                if (now - it.at > WINDOW_MS) {
                    RECENT.remove(i);
                    continue;
                }
                if (it.text.equals(text)) return true;
            }
        }
        return false;
    }
}
