package com.cathelperawa.owo.app.protocol;

import com.cathelperawa.owo.app.api.SendProtoApi;
import com.cathelperawa.owo.app.core.HookApi;
import com.cathelperawa.owo.app.core.HookLog;
import com.cathelperawa.owo.app.core.dex.ClassResolver;
import com.cathelperawa.owo.app.util.Refl;

import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RKeyHolder {

    private RKeyHolder() {}

    private static final long TTL = 20 * 60 * 1000L;
    private static final Charset ASCII = Charset.forName("US-ASCII");

    private static volatile String friendRkey;
    private static volatile long friendAt;
    private static volatile String groupRkey;
    private static volatile long groupAt;

    private static final Map<String, Long> ALL = new LinkedHashMap<String, Long>();

    private static final LinkedHashMap<String, String[]> URLS =
            new LinkedHashMap<String, String[]>(32, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, String[]> e) {
                    return size() > 48;
                }
            };

    private static volatile boolean installed;

    private static volatile int pktRkey;
    private static volatile int pktUrl;

    

    public static void install(ClassLoader cl) {
        if (installed || cl == null) return;
        installed = true;
        dbg("RKeyHolder.install 开始");
        try {
            Class<?> h = ClassResolver.find(cl, "msf.respHandler",
                    "mqq.app.msghandle.MsgRespHandler");
            if (h == null) {
                HookLog.w("RKeyHolder：没找到 MsgRespHandler");
                dbg("MsgRespHandler 没找到，仍然尝试挂 0x9067");
                install9067(cl);
                return;
            }
            int n = 0;
            for (final Method m : h.getDeclaredMethods()) {
                if (!"dispatchRespMsg".equals(m.getName())) continue;
                HookApi.before(m, new HookApi.Action() {
                    @Override
                    public void call(de.robv.android.xposed.XC_MethodHook.MethodHookParam p) {
                        try {
                            onResp(p.args);
                        } catch (Throwable t) {
                            HookLog.w("RKeyHolder 解析异常 " + t);
                        }
                    }
                });
                n++;
            }
            HookLog.i("RKeyHolder 已挂 dispatchRespMsg × " + n);
            dbg("已挂 dispatchRespMsg × " + n);
            install9067(cl);
        } catch (Throwable t) {
            HookLog.e("RKeyHolder 安装失败", t);
        }
    }

    private static int retry9067 = 0;

    private static void install9067(ClassLoader cl) {
        try {

            
            
            Class<?> fsm = null;
            try {
                fsm = Class.forName("com.tencent.qphone.base.remote.FromServiceMsg", false, cl);
            } catch (Throwable ignore) {
            }
            if (fsm == null) {
                try {
                    fsm = ClassResolver.find(cl, "FromServiceMsg",
                            "com.tencent.qphone.base.remote.FromServiceMsg");
                } catch (Throwable ignore) {
                }
            }
            if (fsm == null) {
                HookLog.w("RKeyHolder：没找到 FromServiceMsg");
                dbg("FromServiceMsg 没找到");
                return;
            }
            java.lang.reflect.Method gw = fsm.getMethod("getWupBuffer");
            HookApi.after(gw, new HookApi.Action() {
                @Override
                public void call(de.robv.android.xposed.XC_MethodHook.MethodHookParam p) {
                    try {
                        Object self = p.thisObject;
                        Object c = Refl.call(self, "getServiceCmd");
                        String cmd = c == null ? "" : c.toString();
                        if (cmd.indexOf("0x9067") < 0) return;
                        Object r = p.getResult();
                        if (r instanceof byte[]) {
                            parse9067((byte[]) r);
                        }
                    } catch (Throwable ignore) {
                    }
                }
            });
            HookLog.i("RKeyHolder 已挂 0x9067 精确解析");
            dbg("已挂 0x9067 精确解析");
        } catch (Throwable t) {
            HookLog.w("RKeyHolder 挂 0x9067 失败 " + t);
            dbg("挂 0x9067 失败 " + t);
            
            if (retry9067 < 4) {
                retry9067++;
                try {
                    com.cathelperawa.owo.app.api.TaskApi.after(6000, new Runnable() {
                        @Override public void run() {
                            install9067(cl);
                        }
                    });
                    dbg("0x9067 挂载重试 #" + retry9067 + " 已排期");
                } catch (Throwable ignore) {
                }
            }
        }
    }

    private static void dumpNamed(String cmd, byte[] buf) {
        try {
            String name = cmd.replaceAll("[^0-9A-Za-z_]", "_");
            if (name.length() > 60) name = name.substring(0, 60);
            java.io.File dir = new java.io.File(
                    com.cathelperawa.owo.app.bridge.QQEnv.appContext().getFilesDir(),
                    "cathelper/rkey");
            if (!dir.exists()) dir.mkdirs();
            java.io.File f = new java.io.File(dir, name + ".bin");
            java.io.FileOutputStream fo = new java.io.FileOutputStream(f);
            fo.write(buf, 0, Math.min(buf.length, 65536));
            fo.close();
        } catch (Throwable ignore) {
        }
    }

    private static void dump9067(byte[] buf) {
        try {
            java.io.File f = new java.io.File(
                    com.cathelperawa.owo.app.bridge.QQEnv.appContext().getFilesDir(),
                    "cathelper/rkey_dump.bin");
            if (f.getParentFile() != null) f.getParentFile().mkdirs();
            java.io.FileOutputStream fo = new java.io.FileOutputStream(f);
            fo.write(buf, 0, Math.min(buf.length, 16384));
            fo.close();
            dbg("0x9067 回包已存盘 " + buf.length + "B");
        } catch (Throwable t) {
            dbg("0x9067 存盘失败 " + t);
        }
    }

    private static java.util.List<String> guessKeys(byte[] buf, int depth) {
        java.util.List<String> out = new java.util.ArrayList<String>();
        if (buf == null || depth > 4) return out;
        java.util.Map<Integer, Object> m;
        try {
            m = pb(buf);
        } catch (Throwable t) {
            return out;
        }
        for (Object v : m.values()) {
            byte[] b = asBytes(v);
            if (b == null || b.length < 8) continue;
            String s = new String(b, ASCII);
            if (looksLikeRkey(s)) {
                if (!out.contains(s)) out.add(s);
            } else if (depth < 4) {
                java.util.List<String> sub = guessKeys(b, depth + 1);
                for (String x : sub) if (!out.contains(x)) out.add(x);
            }
        }
        return out;
    }

    private static boolean looksLikeRkey(String s) {

        return plausible(s);
    }

    private static boolean parse9067(byte[] raw) {
        try {

            

            List<byte[]> tries = new ArrayList<byte[]>();
            if (raw.length > 8 && raw[0] == 0 && raw[1] == 0) {
                long len = ((long) (raw[0] & 0xFF) << 24) | ((long) (raw[1] & 0xFF) << 16)
                        | ((long) (raw[2] & 0xFF) << 8) | (long) (raw[3] & 0xFF);
                if (len == raw.length || len == raw.length - 4) {
                    tries.add(java.util.Arrays.copyOfRange(raw, 4, raw.length));
                }
            }
            tries.add(raw);

            List<String[]> found = null;
            for (byte[] body : tries) {
                List<String[]> out = new ArrayList<String[]>();
                digRkey(body, 0, out);
                if (!out.isEmpty()) { found = out; break; }
            }
            if (found == null || found.isEmpty()) {
                HookLog.w("0x9067 解析：没挖到 rkey，改用启发式");
                dbg("0x9067 没挖到 rkey，改用启发式");
                dump9067(raw);
                java.util.List<String> guess = guessKeys(raw, 0);
                if (guess.isEmpty()) return false;
                applyKeys(guess, "启发式");
                return true;
            }

            List<String> friends = new ArrayList<String>();
            List<String> groups = new ArrayList<String>();
            List<String> others = new ArrayList<String>();
            for (String[] kv : found) {
                String k = kv[0];
                int type = -1;
                try { type = Integer.parseInt(kv[1]); } catch (Throwable ignore) {}
                if (type == 10) friends.add(k);
                else if (type == 20) groups.add(k);
                else others.add(k);
            }
            
            for (String k : others) {
                if (!friends.contains(k) && !groups.contains(k)) {
                    if (friends.isEmpty()) friends.add(k);
                    else if (groups.isEmpty()) groups.add(k);
                }
            }

            
            if (groups.isEmpty()) {
                for (String k : plainKeys(raw)) {
                    if (!k.startsWith("CAQS") || groups.contains(k)) continue;
                    groups.add(k);
                }
            }
            List<String> all = new ArrayList<String>();
            all.addAll(friends);
            all.addAll(groups);
            if (all.isEmpty()) return false;
            applyKeys(all, "精确");
            if (!friends.isEmpty()) { friendRkey = friends.get(0); friendAt = System.currentTimeMillis(); }
            if (!groups.isEmpty()) { groupRkey = groups.get(0); groupAt = System.currentTimeMillis(); }
            dbg("0x9067 精确 friend=" + mask(friendRkey) + " group=" + mask(groupRkey)
                    + " 共 " + all.size() + " 枚");
            HookLog.i("0x9067 精确 rkey × " + all.size()
                    + " · friend=" + mask(friendRkey) + " · group=" + mask(groupRkey));
            return true;
        } catch (Throwable t) {
            HookLog.w("0x9067 解析异常 " + t);
            dbg("0x9067 解析异常 " + t);
            return false;
        }
    }

    private static void applyKeys(List<String> keys, String from) {
        long now = System.currentTimeMillis();
        for (int i = 0; i < keys.size(); i++) {
            String k = keys.get(i);
            if (i == 0 && (friendRkey == null || !plausible(friendRkey))) {
                friendRkey = k;
                friendAt = now;
            }
            if (i == 1 && (groupRkey == null || !plausible(groupRkey))) {
                groupRkey = k;
                groupAt = now;
            }
            remember(k);
        }
    }

    private static void remember(String k) {
        if (!plausible(k)) return;
        synchronized (ALL) {
            ALL.remove(k);
            ALL.put(k, Long.valueOf(System.currentTimeMillis()));
            while (ALL.size() > 8) {
                java.util.Iterator<String> it = ALL.keySet().iterator();
                if (it.hasNext()) { it.next(); it.remove(); }
            }
        }
    }

    private static List<String> allKeys() {
        long now = System.currentTimeMillis();
        List<String> out = new ArrayList<String>();
        synchronized (ALL) {
            List<String> ks = new ArrayList<String>(ALL.keySet());
            for (int i = ks.size() - 1; i >= 0; i--) {
                String k = ks.get(i);
                Long t = ALL.get(k);
                if (t != null && now - t.longValue() > TTL) continue;
                if (!out.contains(k)) out.add(k);
            }
        }
        return out;
    }

    private static void digRkey(byte[] b, int depth, List<String[]> out) {
        if (b == null || depth > 6 || b.length < 8 || b.length > 65536) return;
        Map<Integer, Object> m;
        try {
            m = pb(b);
        } catch (Throwable t) {
            return;
        }
        Object k1 = m.get(1);
        if (k1 instanceof byte[]) {
            String s = new String((byte[]) k1, ASCII);
            String k = s.startsWith("&rkey=") ? s.substring(6) : s;
            if (plausible(k)) {
                int type = -1;
                Object t5 = m.get(5);
                if (t5 instanceof Long) type = (int) ((Long) t5).longValue();
                else if (t5 instanceof byte[] && ((byte[]) t5).length <= 2) {
                    byte[] tb = (byte[]) t5;
                    type = tb.length == 0 ? 0 : (tb[0] & 0x7F);
                }
                boolean dup = false;
                for (String[] e : out) if (e[0].equals(k)) { dup = true; break; }
                if (!dup) out.add(new String[]{k, String.valueOf(type)});
            }
        }
        for (Object v : m.values()) {
            if (v instanceof byte[]) {
                digRkey((byte[]) v, depth + 1, out);
            } else if (v instanceof List) {
                for (Object o : (List<?>) v) {
                    if (o instanceof byte[]) digRkey((byte[]) o, depth + 1, out);
                }
            }
        }
    }

    private static boolean plausible(String k) {
        if (k == null) return false;
        int n = k.length();
        if (n < 40 || n > 140) return false;
        if (!(k.startsWith("CAI") || k.startsWith("CAE") || k.startsWith("CAQ"))) return false;
        for (int i = 0; i < n; i++) {
            char c = k.charAt(i);
            boolean ok = (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z')
                    || (c >= '0' && c <= '9') || c == '-' || c == '_' || c == '=' || c == '+' || c == '/';
            if (!ok) return false;
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    private static byte[] asBytes(Object o) {
        return o instanceof byte[] ? (byte[]) o : null;
    }

    @SuppressWarnings("unchecked")
    private static java.util.List<byte[]> asList(Object o) {
        if (o instanceof java.util.List) return (java.util.List<byte[]>) o;
        if (o instanceof byte[]) {
            java.util.List<byte[]> l = new java.util.ArrayList<byte[]>();
            l.add((byte[]) o);
            return l;
        }
        return null;
    }

    private static java.util.Map<Integer, Object> pb(byte[] b) {
        java.util.Map<Integer, Object> out = new java.util.HashMap<Integer, Object>();
        int i = 0;
        while (i < b.length) {
            int tag = 0, shift = 0;
            while (i < b.length) {
                int c = b[i++] & 0xFF;
                tag |= (c & 0x7F) << shift;
                shift += 7;
                if ((c & 0x80) == 0) break;
            }
            int field = tag >>> 3, wire = tag & 7;
            if (field == 0) break;
            if (wire == 0) {

                long v = 0;
                int sh = 0;
                while (i < b.length) {
                    int c = b[i++] & 0xFF;
                    v |= (long) (c & 0x7F) << sh;
                    sh += 7;
                    if ((c & 0x80) == 0) break;
                }
                Object prev = out.get(field);
                if (prev == null) {
                    out.put(field, Long.valueOf(v));
                } else if (prev instanceof List) {
                    ((java.util.List<Object>) prev).add(Long.valueOf(v));
                } else {
                    java.util.List<Object> l = new java.util.ArrayList<Object>();
                    l.add(prev);
                    l.add(Long.valueOf(v));
                    out.put(field, l);
                }
            } else if (wire == 2) {
                int len = 0; shift = 0;
                while (i < b.length) {
                    int c = b[i++] & 0xFF;
                    len |= (c & 0x7F) << shift;
                    shift += 7;
                    if ((c & 0x80) == 0) break;
                }
                if (len < 0 || i + len > b.length) break;
                byte[] d = java.util.Arrays.copyOfRange(b, i, i + len);
                i += len;
                Object prev = out.get(field);
                if (prev == null) {
                    out.put(field, d);
                } else if (prev instanceof java.util.List) {
                    ((java.util.List<byte[]>) prev).add(d);
                } else {
                    java.util.List<byte[]> l = new java.util.ArrayList<byte[]>();
                    l.add((byte[]) prev);
                    l.add(d);
                    out.put(field, l);
                }
            } else if (wire == 5) {
                i += 4;
            } else if (wire == 1) {
                i += 8;
            } else {
                break;
            }
        }
        return out;
    }

    private static void dbg(String s) {
        try {
            com.cathelperawa.owo.app.ai.AiDebug.log("  · RKey: " + s);
        } catch (Throwable ignore) {
        }
    }

    private static void onResp(Object[] args) {
        if (args == null || args.length < 2) return;
        Object from = null;
        
        Object holder = args[1];
        if (holder != null) from = Refl.field(holder, "fromServiceMsg");
        if (from == null) {
            for (Object a : args) {
                if (a != null && a.getClass().getName().contains("FromServiceMsg")) {
                    from = a;
                    break;
                }
            }
        }
        if (from == null) return;

        String cmd = Refl.callStr(from, "getServiceCmd", null);
        if (cmd == null) cmd = Refl.strOr(Refl.field(from, "serviceCmd"), null);
        if (cmd == null) return;

        boolean isRkey = cmd.indexOf("0x9067") >= 0
                || cmd.indexOf("0x126e") >= 0
                || cmd.indexOf("0x11e9") >= 0
                || cmd.indexOf("0x11ea") >= 0;
        if (!isRkey) return;

        byte[] buf = null;
        Object wb = Refl.call(from, "getWupBuffer");
        if (wb instanceof byte[]) buf = (byte[]) wb;
        if (buf == null) {
            Object f = Refl.field(from, "wupBuffer");
            if (f instanceof byte[]) buf = (byte[]) f;
        }
        if (buf == null || buf.length < 16) return;

        byte[] body = SendProtoApi.stripWup(buf);
        if (body == null || body.length < 8) body = buf;

        if (cmd.indexOf("0x9067") >= 0) {
            dbg("0x9067 命中 cmd=" + cmd + " len=" + buf.length);
            dumpNamed(cmd, buf);
        }

        
        if (cmd.indexOf("0x9067") >= 0) {
            if (parse9067(body)) {
                pktRkey++;
                return;
            }
        }

        int urls = scanUrls(body);
        int keys = scanRkey(body);
        if (urls > 0) pktUrl++;
        if (keys > 0) pktRkey++;
        if (urls > 0 || keys > 0) {
            HookLog.i("rkey包 " + cmd + " → url=" + urls + " key=" + keys);
        }
    }

    

    
    private static List<String> plainKeys(byte[] b) {
        List<String> keys = new ArrayList<String>();
        if (b == null) return keys;
        for (int i = 0; i + 5 <= b.length; i++) {
            if (b[i] != 'r' || b[i + 1] != 'k' || b[i + 2] != 'e'
                    || b[i + 3] != 'y' || b[i + 4] != '=') continue;
            int s = i + 5;
            int e = s;
            while (e < b.length && isB64(b[e]) && e - s < 400) e++;
            int len = e - s;
            if (len >= 40) {
                String k = new String(b, s, len, ASCII);
                if (plausible(k) && !keys.contains(k)) keys.add(k);
            }
            i = Math.max(i, e - 1);
            if (keys.size() >= 8) break;
        }
        return keys;
    }

    private static int scanRkey(byte[] b) {
        List<String> keys = new ArrayList<String>();
        for (int i = 0; i + 5 <= b.length; i++) {
            if (b[i] != 'r' || b[i + 1] != 'k' || b[i + 2] != 'e'
                    || b[i + 3] != 'y' || b[i + 4] != '=') continue;
            int s = i + 5;
            int e = s;
            while (e < b.length && isB64(b[e]) && e - s < 400) e++;
            int len = e - s;
            if (len >= 40) {
                String k = new String(b, s, len, ASCII);
                if (!keys.contains(k)) keys.add(k);
            }
            i = Math.max(i, e - 1);
            if (keys.size() >= 8) break;
        }
        if (keys.isEmpty()) return 0;
        long now = System.currentTimeMillis();

        List<String> good = new ArrayList<String>();
        for (String k : keys) {
            if (plausible(k) && !good.contains(k)) good.add(k);
        }
        if (good.isEmpty()) return 0;
        boolean changed = false;
        if (friendRkey == null || !plausible(friendRkey)) {
            friendRkey = good.get(0);
            friendAt = now;
            changed = true;
        }
        if (good.size() > 1 && (groupRkey == null || !plausible(groupRkey))) {
            groupRkey = good.get(1);
            groupAt = now;
            changed = true;
        }
        for (String k : good) remember(k);
        
        if (!changed) return good.size();
        HookLog.i("rkey 抓到 " + good.size() + " 枚 · friend=" + mask(friendRkey)
                + " · group=" + mask(groupRkey));
        return good.size();
    }

    private static int scanUrls(byte[] b) {
        int found = 0;
        int i = 0;
        byte[] pat = "/download?".getBytes(ASCII);
        while (i < b.length) {
            int idx = indexOf(b, pat, i);
            if (idx < 0) break;
            int e = idx;
            while (e < b.length && isUrlByte(b[e]) && e - idx < 1200) e++;
            if (e - idx > 24) {
                String u = new String(b, idx, e - idx, ASCII);
                if (u.startsWith("/")) u = "https://multimedia.nt.qq.com.cn" + u;
                if (putUrl(u)) found++;
            }
            i = Math.max(idx + 1, e);
        }
        return found;
    }

    private static boolean putUrl(String url) {
        String fid = fileIdOf(url);
        if (fid == null || fid.length() < 8) return false;
        synchronized (URLS) {
            String[] old = URLS.get(fid);
            if (old != null && old[0].equals(url)) return false;
            URLS.put(fid, new String[]{url, String.valueOf(System.currentTimeMillis())});
        }
        HookLog.i("带票URL 缓存 " + fid.substring(0, Math.min(12, fid.length())) + "… " + head(url));
        return true;
    }

    

    
    public static String friendRkey() {
        String k = friendRkey;
        if (k == null || System.currentTimeMillis() - friendAt > TTL) return null;
        return k;
    }

    public static String groupRkey() {
        String k = groupRkey;
        if (k == null || System.currentTimeMillis() - groupAt > TTL) return null;
        return k;
    }

    public static String[] candidates(String url) {
        if (url == null || url.length() == 0) return new String[0];
        List<String> out = new ArrayList<String>();
        String fid = fileIdOf(url);
        if (fid != null) {
            String hit = cachedUrl(fid);
            if (hit != null && hit.indexOf("rkey=") > 0) out.add(hit);
        }

        

        
        boolean group = (appidOf(url) == 1407);   
        String base = stripRkey(url);
        List<String> same = new ArrayList<String>();   
        List<String> other = new ArrayList<String>();
        for (String k : allKeys()) {
            boolean groupKey = (k.startsWith("CAQS"));
            if (group == groupKey) same.add(k);
            else other.add(k);
        }
        for (String k : same) putKey(base, k, out);
        for (String k : other) putKey(base, k, out);

        if (url.indexOf("rkey=") > 0 && !out.contains(url)) out.add(url);
        if (!out.contains(url)) out.add(url);   
        return out.toArray(new String[0]);
    }

    private static String stripRkey(String url) {
        int i = url.indexOf("rkey=");
        if (i <= 0) return url;
        int start = i;
        char prev = url.charAt(i - 1);
        if (prev == '&' || prev == '?') start = i - 1;
        return url.substring(0, start);
    }

    private static void putKey(String base, String k, List<String> out) {
        if (base == null || k == null || k.length() == 0) return;
        String u;
        if (base.endsWith("&") || base.endsWith("?")) u = base + "rkey=" + k;
        else u = base + "&rkey=" + k;
        if (!out.contains(u)) out.add(u);
    }

    public static String cachedUrl(String fid) {
        if (fid == null) return null;
        synchronized (URLS) {
            String[] v = URLS.get(fid);
            if (v == null) return null;
            long t = 0;
            try { t = Long.parseLong(v[1]); } catch (Throwable ignore) {}
            if (System.currentTimeMillis() - t > TTL) {
                URLS.remove(fid);
                return null;
            }
            return v[0];
        }
    }

    public static void forget(String fid) {
        if (fid == null || fid.length() == 0) return;
        synchronized (URLS) {
            URLS.remove(fid);
        }
    }

    public static String status() {
        String f = friendRkey();
        String g = groupRkey();
        int n;
        synchronized (URLS) { n = URLS.size(); }
        return "rkey friend=" + (f == null ? "无" : mask(f))
                + " group=" + (g == null ? "无" : mask(g))
                + " · 带票URL " + n + " 条"
                + " · 包 rkey×" + pktRkey + " url×" + pktUrl;
    }

    

    public static String fileIdOf(String url) {
        if (url == null) return null;
        int i = url.indexOf("fileid=");
        if (i < 0) return null;
        int s = i + 7;
        int e = s;
        while (e < url.length() && url.charAt(e) != '&' && url.charAt(e) != '#') e++;
        if (e <= s) return null;
        return url.substring(s, e);
    }

    public static int appidOf(String url) {
        if (url == null) return -1;
        int i = url.indexOf("appid=");
        if (i < 0) return -1;
        int s = i + 6;
        int e = s;
        while (e < url.length() && Character.isDigit(url.charAt(e))) e++;
        if (e <= s) return -1;
        try { return Integer.parseInt(url.substring(s, e)); } catch (Throwable t) { return -1; }
    }

    private static boolean isB64(byte c) {
        return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z')
                || (c >= '0' && c <= '9') || c == '-' || c == '_'
                || c == '+' || c == '/' || c == '=';
    }

    private static boolean isUrlByte(byte c) {
        if (c <= 0x20 || c >= 0x7f) return false;
        return c != '"' && c != '\\' && c != '<' && c != '>' && c != '`' && c != '\'';
    }

    private static int indexOf(byte[] b, byte[] pat, int from) {
        outer:
        for (int i = Math.max(0, from); i + pat.length <= b.length; i++) {
            for (int j = 0; j < pat.length; j++) {
                if (b[i + j] != pat[j]) continue outer;
            }
            return i;
        }
        return -1;
    }

    private static String mask(String k) {
        if (k == null) return "无";
        if (k.length() <= 12) return k;
        return k.substring(0, 10) + "…(" + k.length() + ")";
    }

    private static String head(String u) {
        if (u == null) return "";
        return u.length() <= 90 ? u : u.substring(0, 90) + "…";
    }
}
