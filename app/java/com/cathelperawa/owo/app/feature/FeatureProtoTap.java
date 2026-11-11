package com.cathelperawa.owo.app.feature;

import com.cathelperawa.owo.app.core.HookApi;
import com.cathelperawa.owo.app.core.HookConfig;
import com.cathelperawa.owo.app.core.HookItem;
import com.cathelperawa.owo.app.core.HookItemBase;
import com.cathelperawa.owo.app.core.HookLog;
import com.cathelperawa.owo.app.core.SelfCheckResult;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import de.robv.android.xposed.XC_MethodHook;

@HookItem(name = "协议透视", path = "调试与协议/协议透视", order = 90,
        description = "记录加解密前后的网络包，以及各类密钥", defaultEnabled = false)
public class FeatureProtoTap extends HookItemBase {

    public static final String K = "proto_tap";
    
    public static final String CFG_FILTER = "proto_tap_filter";
    
    public static final String CFG_KEYS = "proto_tap_keys";
    
    public static final String CFG_QUA_ON = "qua_patch";
    
    public static final String CFG_QUA_VAL = "qua_patch_value";

    private static final String FILE = "proto_tap.txt";
    private static final int MAX_BYTES = 16384;
    private static final long MAX_FILE = 16 * 1024 * 1024L;

    private static volatile int sCount = 0;
    private static volatile String sCachedFilter = null;
    private static volatile String[] sCached = new String[0];

    @Override
    public String switchKey() {
        return K;
    }

    @Override
    public void entry(ClassLoader cl) {
        hint("有网络请求时触发");
        Class<?> c = HookApi.findClass(cl, "com.tencent.qphone.base.util.CodecWarpper");
        if (c == null) {
            fail("未找到 CodecWarpper");
            return;
        }
        dbg("== entry 开始, 开关=" + isEnabled());

        int n = 0;
        
        for (final Method m : c.getDeclaredMethods()) {
            if (!"nativeEncodeRequest".equals(m.getName())) continue;
            m.setAccessible(true);
            if (HookApi.before(m, new HookApi.Action() {
                @Override public void call(XC_MethodHook.MethodHookParam p) {
                    try {
                        if (!isEnabled()) return;
                        onEncode(p);
                    } catch (Throwable t) {
                        noteRuntimeError(t);
                    }
                }
            }, this) != null) n++;
        }

        for (final Method m : c.getDeclaredMethods()) {
            if (!"nativeParseData".equals(m.getName())) continue;
            m.setAccessible(true);
            if (HookApi.after(m, new HookApi.Action() {
                @Override public void call(XC_MethodHook.MethodHookParam p) {
                    try {
                        if (!isEnabled()) return;
                        onParse(p);
                    } catch (Throwable t) {
                        noteRuntimeError(t);
                    }
                }
            }, this) != null) n++;
        }

        for (final Method m : c.getDeclaredMethods()) {
            if (!"onReceData".equals(m.getName())) continue;
            m.setAccessible(true);
            if (HookApi.before(m, new HookApi.Action() {
                @Override public void call(XC_MethodHook.MethodHookParam p) {
                    try {
                        if (!isEnabled()) return;
                        if (p.args == null) return;
                        for (Object a : p.args) {
                            if (a instanceof byte[]) {
                                writeRaw("◆ 收原始 (" + ((byte[]) a).length + "B)",
                                        (byte[]) a);
                                break;
                            }
                        }
                    } catch (Throwable t) {
                        noteRuntimeError(t);
                    }
                }
            }, this) != null) n++;
        }

        int k = hookKeys(c);
        dbg("协议钩子挂了 " + n + " 个，密钥钩子 " + k + " 个");
        if (n == 0) fail("一个都没挂上");
        else HookLog.i("协议透视：包=" + n + " 密钥=" + k);
    }

    

    private void onEncode(XC_MethodHook.MethodHookParam p) {
        if (p.args == null) return;

        
        String cmd = "";
        if (p.args.length > 5 && p.args[5] instanceof String) {
            cmd = (String) p.args[5];
        }
        byte[] plain = null;
        if (p.args.length > 13 && p.args[13] instanceof byte[]) {
            byte[] b = (byte[]) p.args[13];
            if (b.length > 0) plain = b;
        }
        if (plain == null) {
            for (Object a : p.args) {
                if (a instanceof byte[] && ((byte[]) a).length > 0) {
                    plain = (byte[]) a;
                    break;
                }
            }
        }
        if (plain == null) return;
        if (!passFilter(cmd)) return;
        patchQua(plain, cmd);
        write("→ 发 " + cmd + " (" + plain.length + "B)", plain);
    }

    private static int sParseSeen = 0;

    private void onParse(XC_MethodHook.MethodHookParam p) {
        if (sParseSeen < 30) {
            sParseSeen++;
            Object rr = p.getResult();
            writeRaw("· parseData 命中 #" + sParseSeen + " result="
                    + (rr == null ? "null" : rr.getClass().getSimpleName()), null);
        }
        Object r = p.getResult();
        if (r == null) return;
        String cmd = "";
        byte[] body = null;
        try {
            Method gc = r.getClass().getMethod("getServiceCmd");
            Object o = gc.invoke(r);
            if (o instanceof String) cmd = (String) o;
        } catch (Throwable ignore) {
        }
        try {
            Method gb = r.getClass().getMethod("getWupBuffer");
            Object o = gb.invoke(r);
            if (o instanceof byte[]) body = (byte[]) o;
        } catch (Throwable ignore) {
        }
        if (!passFilter(cmd)) return;
        if (body == null || body.length == 0) {
            write("← 回 " + cmd + " (无体)", null);
        } else {
            write("← 回 " + cmd + " (" + body.length + "B)", body);
        }
    }

    

    private static final byte[] QUA_MARK = "V1_AND_SQ_".getBytes();
    private static volatile String sQuaSeen = null;

    private void patchQua(byte[] body, String cmd) {
        try {
            if (body == null || body.length < QUA_MARK.length) return;
            int idx = indexOf(body, QUA_MARK);
            if (idx < 0) return;

            int end = idx;
            while (end < body.length) {
                byte c = body[end];
                boolean ok = (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z')
                        || (c >= '0' && c <= '9') || c == '_' || c == '.';
                if (!ok) break;
                end++;
            }
            String orig = new String(body, idx, end - idx, "UTF-8");
            if (orig.length() < 12) return;

            if (sQuaSeen == null || !sQuaSeen.equals(orig)) {
                sQuaSeen = orig;
                writeRaw("◇ QUA 扫描: " + orig + "  (偏移 " + idx
                        + ", 长度 " + orig.length() + ")  ← " + cmd, null);
            }

            String target = quaTarget();
            if (target == null || target.length() == 0) return;
            if (target.length() != orig.length()) {
                writeRaw("✗ QUA 改写跳过：长度不符 原=" + orig.length()
                        + " 新=" + target.length(), null);
                return;
            }
            byte[] tb = target.getBytes("UTF-8");
            System.arraycopy(tb, 0, body, idx, tb.length);
            writeRaw("✓ QUA 已改写 " + orig + " → " + target + "  ← " + cmd, null);
        } catch (Throwable ignore) {
        }
    }

    private static int indexOf(byte[] h, byte[] n) {
        outer:
        for (int i = 0; i <= h.length - n.length; i++) {
            for (int j = 0; j < n.length; j++) {
                if (h[i + j] != n[j]) continue outer;
            }
            return i;
        }
        return -1;
    }

    private String quaTarget() {
        try {
            if (ctx == null) return null;
            if (!"1".equals(HookConfig.get(ctx, CFG_QUA_ON, "0").trim())) return null;
            return HookConfig.get(ctx, CFG_QUA_VAL, "").trim();
        } catch (Throwable t) {
            return null;
        }
    }

    

    private int hookKeys(Class<?> c) {
        int n = 0;
        for (final Method m : c.getDeclaredMethods()) {
            String name = m.getName();
            if ("getFileStoreKey".equals(name) || "nativeGetFileStoreKey".equals(name)) {
                try { m.setAccessible(true); } catch (Throwable ignore) {}
                if (HookApi.after(m, new HookApi.Action() {
                    @Override public void call(XC_MethodHook.MethodHookParam p) {
                        try {
                            if (!keysOn()) return;
                            Object r = p.getResult();
                            if (r instanceof byte[]) {
                                byte[] b = (byte[]) r;
                                writeRaw("★ 文件存储密钥 (" + b.length + "B)", b);
                            }
                        } catch (Throwable t) {
                            noteRuntimeError(t);
                        }
                    }
                }, this) != null) n++;
                continue;
            }
            if ("nativeSetAccountKey".equals(name)) {
                try { m.setAccessible(true); } catch (Throwable ignore) {}
                if (HookApi.before(m, new HookApi.Action() {
                    @Override public void call(XC_MethodHook.MethodHookParam p) {
                        try {
                            if (!keysOn()) return;
                            if (p.args == null) return;
                            StringBuilder sb = new StringBuilder("★ 账号密钥组");
                            for (int i = 0; i < p.args.length; i++) {
                                Object a = p.args[i];
                                if (a instanceof byte[]) {
                                    byte[] b = (byte[]) a;
                                    if (b.length > 0) {
                                        sb.append("\n   [" + i + "] " + b.length + "B "
                                                + hex(b, 64));
                                    }
                                }
                            }
                            writeRaw(sb.toString(), null);
                        } catch (Throwable t) {
                            noteRuntimeError(t);
                        }
                    }
                }, this) != null) n++;
            }
        }
        return n;
    }

    

    private boolean passFilter(String cmd) {
        String[] fs = filter();
        if (fs.length == 0) return true;
        if (cmd == null) cmd = "";
        String low = cmd.toLowerCase(Locale.US);
        for (String f : fs) {
            if (low.contains(f)) return true;
        }
        return false;
    }

    private String[] filter() {
        String raw;
        try {
            if (ctx == null) return sCached;
            raw = HookConfig.get(ctx, CFG_FILTER, "");
        } catch (Throwable t) {
            return sCached;
        }
        if (raw == null) raw = "";
        if (raw.equals(sCachedFilter)) return sCached;
        sCachedFilter = raw;
        if (raw.trim().length() == 0) {
            sCached = new String[0];
        } else {
            sCached = raw.toLowerCase(Locale.US).split("[,，\\s]+");
        }
        return sCached;
    }

    private void write(String head, byte[] body) {
        StringBuilder sb = new StringBuilder(head);
        if (body != null) {
            sb.append("\n   hex: ").append(hex(body, MAX_BYTES));
            String rd = readable(body, 400);
            if (rd.length() > 0) sb.append("\n   文: ").append(rd);
        }
        writeRaw(sb.toString(), null);
    }

    private static String readable(byte[] b, int max) {
        if (b == null) return "";
        StringBuilder out = new StringBuilder();
        StringBuilder cur = new StringBuilder();
        for (byte x : b) {
            int v = x & 0xFF;
            if (v >= 0x20 && v < 0x7F) {
                cur.append((char) v);
            } else {
                if (cur.length() >= 4) {
                    if (out.length() > 0) out.append(" | ");
                    out.append(cur);
                    if (out.length() > max) return out.append("…").toString();
                }
                cur.setLength(0);
            }
        }
        if (cur.length() >= 4) {
            if (out.length() > 0) out.append(" | ");
            out.append(cur);
        }
        return out.toString();
    }

    private synchronized void writeRaw(String line, byte[] body) {
        try {
            if (ctx == null) return;
            if (sCount > 20000) return;
            sCount++;
            File f = new File(ctx.getFilesDir(), FILE);
            if (f.length() > MAX_FILE) f.delete();
            FileOutputStream o = new FileOutputStream(f, true);
            try {
                String t = new SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(new Date());
                o.write((t + " " + line + "\n").getBytes("UTF-8"));
                if (body != null && body.length > MAX_BYTES) {
                    o.write(("   …共 " + body.length + "B\n").getBytes("UTF-8"));
                }
            } finally {
                try { o.close(); } catch (Throwable ignore) {}
            }
        } catch (Throwable ignore) {
        }
    }

    private static String hex(byte[] b, int max) {
        if (b == null) return "";
        int n = Math.min(b.length, max);
        StringBuilder sb = new StringBuilder(n * 2);
        for (int i = 0; i < n; i++) sb.append(String.format("%02x", b[i]));
        if (b.length > n) sb.append("…");
        return sb.toString();
    }

    private boolean keysOn() {
        try {
            if (ctx == null) return false;
            return "1".equals(HookConfig.get(ctx, CFG_KEYS, "0").trim());
        } catch (Throwable t) {
            return false;
        }
    }

    private void dbg(String s) {
        try {
            HookLog.i("协议透视 " + s);
        } catch (Throwable ignore) {
        }
    }

    @Override
    protected void check(SelfCheckResult.Checker ck) {
        try {
            Class<?> cw = HookApi.findClass(cl, "com.tencent.qphone.base.util.CodecWarpper");
            if (cw == null) {
                ck.fail("CodecWarpper", "类未找到");
                return;
            }
            ck.method("nativeEncodeRequest", cw, "nativeEncodeRequest");
            ck.method("nativeParseData", cw, "nativeParseData");
            ck.method("getFileStoreKey", cw, "getFileStoreKey");
        } catch (Throwable t) {
            ck.fail("自检", t.toString());
        }
    }
}
