package com.cathelperawa.owo.app.core;

import android.content.Context;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RpGroupRules {

    public static final String K_MODE = "rp_group_mode";
    public static final String K_MAP = "rp_group_map";
    public static final String K_SEEN = "rp_group_seen";

    public static final String MODE_ALL = "all";
    public static final String MODE_WHITE = "white";

    public static final class Entry {
        public final String uin;
        public final String name;
        
        public final Boolean state;

        Entry(String uin, String name, Boolean state) {
            this.uin = uin;
            this.name = name;
            this.state = state;
        }

        public String displayName() {
            return (name == null || name.isEmpty()) ? ("群 " + uin) : (name + "（" + uin + "）");
        }
    }

    private RpGroupRules() {}

    public static String mode(Context ctx) {
        String m = HookConfig.get(ctx, K_MODE, MODE_ALL);
        return MODE_WHITE.equals(m) ? MODE_WHITE : MODE_ALL;
    }

    public static void setMode(Context ctx, String mode) {
        HookConfig.set(ctx, K_MODE, MODE_WHITE.equals(mode) ? MODE_WHITE : MODE_ALL);
    }

    public static Boolean stateOf(Context ctx, String troopUin) {
        Map<String, Boolean> m = readMap(ctx);
        return m.get(troopUin);
    }

    public static void setState(Context ctx, String troopUin, Boolean state) {
        if (troopUin == null || troopUin.isEmpty()) return;
        Map<String, Boolean> m = readMap(ctx);
        if (state == null) m.remove(troopUin);
        else m.put(troopUin, state);
        writeMap(ctx, m);
    }

    public static boolean isAllowed(Context ctx, String troopUin) {
        if (troopUin == null || troopUin.isEmpty()) return true;   

        Boolean st = stateOf(ctx, troopUin);
        if (st != null) return st;                                  

        return !MODE_WHITE.equals(mode(ctx));                       
    }

    public static boolean effective(Context ctx, String troopUin) {
        return isAllowed(ctx, troopUin);
    }

    
    public static void noteSeen(Context ctx, String troopUin, String name) {
        if (troopUin == null || troopUin.isEmpty()) return;
        Map<String, String> seen = readSeen(ctx);
        String old = seen.get(troopUin);
        if (old != null && (name == null || name.isEmpty() || name.equals(old))) return;
        seen.put(troopUin, name == null ? "" : name);
        if (seen.size() > 200) {                                    
            String first = seen.keySet().iterator().next();
            seen.remove(first);
        }
        writeSeen(ctx, seen);
    }

    public static int syncFromHost(android.content.Context ctx) {
        
        int viaIpc = syncViaIpc(ctx);
        if (viaIpc > 0) return viaIpc;

        String text = readTroopFile(ctx);
        if (text == null || text.isEmpty()) return -1;
        int n = 0;
        for (String line : text.split("\n")) {
            line = line.trim();
            if (line.isEmpty()) continue;
            int i = line.indexOf('|');
            String uin = (i < 0 ? line : line.substring(0, i)).trim();
            String name = i < 0 ? "" : line.substring(i + 1).trim();
            if (!uin.matches("\\d{5,12}")) continue;
            noteSeen(ctx, uin, name);
            n++;
        }
        if (n > 0) HookLog.i("已从宿主读入群列表 " + n + " 个");
        return n;
    }

    private static int syncViaIpc(android.content.Context ctx) {
        try {
            java.util.List<Ipc.Reply> rs = Ipc.enumerate(ctx, Ipc.PROC_MAIN, 1200, "troops");
            int n = 0;
            for (Ipc.Reply r : rs) {
                String t = r.text;
                if (t == null || t.isEmpty() || "EMPTY".equals(t) || t.startsWith("ERR:")) continue;
                for (String part : t.split(",")) {
                    int i = part.indexOf('|');
                    if (i <= 0) continue;
                    noteSeen(ctx, part.substring(0, i).trim(), part.substring(i + 1).trim());
                    n++;
                }
            }
            if (n > 0) HookLog.i("群列表：IPC 拿到 " + n + " 个");
            return n;
        } catch (Throwable t) {
            return -1;
        }
    }

    private static String readTroopFile(android.content.Context ctx) {
        String p = null;
        try {
            android.content.Context host = ctx.createPackageContext(
                    "com.tencent.tim", android.content.Context.CONTEXT_IGNORE_SECURITY);
            
            p = "/sdcard/Android/data/com.tencent.tim/files/rp_troops.txt";
            host = null;
        } catch (Throwable ignore) {
            p = "/sdcard/Android/data/com.tencent.tim/files/rp_troops.txt";
        }
        try {
            java.io.File f = new java.io.File(p);
            if (f.isFile() && f.canRead()) return StatusStore.read(f);
        } catch (Throwable ignore) {
        }
        return StatusStore.readAbsoluteViaSu(p);
    }

    public static List<Entry> entries(Context ctx) {
        Map<String, String> seen = readSeen(ctx);
        Map<String, Boolean> map = readMap(ctx);

        java.util.LinkedHashSet<String> all = new java.util.LinkedHashSet<>();
        all.addAll(seen.keySet());
        all.addAll(map.keySet());

        List<String> keys = new ArrayList<>(all);
        java.util.Collections.sort(keys);

        List<Entry> out = new ArrayList<>();
        for (String uin : keys) {
            out.add(new Entry(uin, seen.get(uin), map.get(uin)));
        }
        return out;
    }

    public static void forget(Context ctx, String troopUin) {
        if (troopUin == null) return;
        Map<String, Boolean> m = readMap(ctx);
        m.remove(troopUin);
        writeMap(ctx, m);
        Map<String, String> seen = readSeen(ctx);
        seen.remove(troopUin);
        writeSeen(ctx, seen);
    }

    public static boolean add(Context ctx, String troopUin, String name) {
        if (troopUin == null) return false;
        String u = troopUin.trim();
        if (!u.matches("\\d{5,12}")) return false;
        noteSeen(ctx, u, name);
        setState(ctx, u, Boolean.TRUE);      
        return true;
    }

    private static Map<String, Boolean> readMap(Context ctx) {
        Map<String, Boolean> m = new LinkedHashMap<>();
        String s = HookConfig.get(ctx, K_MAP, "");
        if (s == null || s.isEmpty()) return m;
        for (String part : s.split(",")) {
            int i = part.indexOf(':');
            if (i <= 0) continue;
            try {
                m.put(part.substring(0, i).trim(), "1".equals(part.substring(i + 1).trim()));
            } catch (Throwable ignore) {
            }
        }
        return m;
    }

    private static void writeMap(Context ctx, Map<String, Boolean> m) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Boolean> e : m.entrySet()) {
            if (sb.length() > 0) sb.append(',');
            sb.append(e.getKey()).append(':').append(e.getValue() ? '1' : '0');
        }
        HookConfig.set(ctx, K_MAP, sb.toString());
    }

    private static Map<String, String> readSeen(Context ctx) {
        Map<String, String> m = new LinkedHashMap<>();
        String s = HookConfig.get(ctx, K_SEEN, "");
        if (s == null || s.isEmpty()) return m;
        for (String part : s.split(",")) {
            int i = part.indexOf('|');
            if (i <= 0) continue;
            m.put(part.substring(0, i).trim(), part.substring(i + 1));
        }
        return m;
    }

    private static void writeSeen(Context ctx, Map<String, String> m) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : m.entrySet()) {
            if (sb.length() > 0) sb.append(',');
            String name = e.getValue() == null ? "" : e.getValue().replace(",", " ").replace("|", " ");
            sb.append(e.getKey()).append('|').append(name);
        }
        HookConfig.set(ctx, K_SEEN, sb.toString());
    }

    public static String summary(Context ctx) {
        return "模式=" + mode(ctx) + " 群数=" + entries(ctx).size();
    }
}
