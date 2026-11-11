package com.cathelperawa.owo.app.core;

import com.cathelperawa.owo.app.core.DataDir;

import android.content.Context;
import android.content.SharedPreferences;

import com.cathelperawa.owo.app.bridge.QQEnv;
import com.cathelperawa.owo.app.cookie.CookieCipher;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class PrivacyCheck {

    private PrivacyCheck() {}

    private static final int HIGH = 3, MID = 2, LOW = 1;

    
    private static final int PLAIN = 0;
    
    private static final int COOKIE_GC = 1;
    
    private static final int KS_KEY = 2;
    
    private static final int SP_KEY = 3;
    
    private static final int LINE = 4;
    
    private static final int DIAG = 5;

    private static final class Item {
        final String name;      
        final String path;      
        final int level;
        final int kind;
        final String spKey;     

        Item(String name, String path, int level, int kind, String spKey) {
            this.name = name;
            this.path = path;
            this.level = level;
            this.kind = kind;
            this.spKey = spKey;
        }
    }

    private static final List<Item> ITEMS = new ArrayList<Item>();

    static {
        
        add("Cookie 存档",      "cathelper/cookies.json",        HIGH, COOKIE_GC, null);
        add("Cookie 跨进程缓存", "cathelper/cookie_cache.json",   HIGH, COOKIE_GC, null);
        add("AI 提供商密钥",     "cathelper/ai_providers.json",   HIGH, KS_KEY,    null);
        add("消息记录",         "cathelper/{uin}/msg.txt",        HIGH, LINE,      null);
        add("AI 对话历史",      "cathelper/ai_history.json",      HIGH, COOKIE_GC, null);
        add("人物画像",         "cathelper/ai_affinity.json",     HIGH, COOKIE_GC, null);
        add("语音转文字",       "cathelper/voice_text.json",      HIGH, COOKIE_GC, null);
        add("最近发送的图",     "cathelper/ai_last_image.jpg",    HIGH, PLAIN,     null);
        add("网易云 Cookie",    "",                               HIGH, SP_KEY,    "music_cookie");

        add("会话名单",         "cathelper/{uin}/peers.txt",      MID, LINE,  null);
        add("语音索引",         "cathelper/ptt_index.json",       MID, PLAIN, null);
        add("模块运行日志",     "cathelper/{uin}/log.txt",        MID, LINE,  null);
        add("消息日志",         "module_msg.txt",                 MID, PLAIN, null);
        add("模块日志",         "module_log.txt",                 MID, PLAIN, null);
        add("AI 调试日志",      "cathelper/ai_debug.log",         MID, PLAIN, null);
        add("rkey 缓存",        "cathelper/rkey",                 MID, DIAG,  null);
        add("rkey 转储",        "cathelper/rkey_dump.bin",        MID, DIAG,  null);
        add("OIDB 抓包",        "cathelper/oidb_capture.txt",     MID, DIAG,  null);
        add("@高亮诊断",        "cathelper/atdiag.txt",           MID, DIAG,  null);
        add("图文混排诊断",     "cathelper/mixdiag.txt",          MID, DIAG,  null);
        add("内核诊断",         "cathelper/mixkernel.txt",        MID, DIAG,  null);

        add("发送模板",         "cathelper/send_templates.json",  LOW, PLAIN, null);
        add("作用范围",         "cathelper/{uin}/scope.txt",      LOW, LINE,  null);
        add("群标记",           "cathelper/group_marks.txt",      LOW, PLAIN, null);
        add("解析诊断",         "cathelper/parse_diag.txt",       LOW, PLAIN, null);
        add("解析缓存",         "cathelper/parse",                LOW, PLAIN, null);
        add("探针记录",         "cathelper/tools_probe.txt",      LOW, DIAG,  null);
        add("自定义背景",       "custom",                         LOW, PLAIN, null);
        add("表情缓存",         "cathelper/emoji",                LOW, PLAIN, null);
    }

    private static void add(String name, String path, int level, int kind, String spKey) {
        ITEMS.add(new Item(name, path, level, kind, spKey));
    }

    private static final Set<String> KNOWN = new HashSet<String>();

    static {
        
        KNOWN.add("ai_history.json");
        KNOWN.add("ai_affinity.json");
        KNOWN.add("ai_providers.json");
        KNOWN.add("ai_debug.log");
        KNOWN.add("ai_last_image.*");
        KNOWN.add("ai_note.json");
        KNOWN.add("cookies.json");
        KNOWN.add("cookie_cache.json");
        KNOWN.add("cookie_cache.json.tmp");
        KNOWN.add("ptt_index.json");
        KNOWN.add("voice_text.json");
        KNOWN.add("send_templates.json");
        KNOWN.add("tools_probe.txt");
        KNOWN.add("group_marks.txt");
        KNOWN.add("oidb_capture.txt");
        KNOWN.add("rkey_dump.bin");
        KNOWN.add("atdiag.txt");
        KNOWN.add("mixdiag.txt");
        KNOWN.add("mixkernel.txt");
        KNOWN.add("mixkernel.txt.1");
        KNOWN.add("parse_diag.txt");
        KNOWN.add("load_crash_count");
        KNOWN.add("emoji");
        KNOWN.add("rkey");
        KNOWN.add("parse");
    }

    

    
    public static String build(Context ctx) {
        StringBuilder sb = new StringBuilder();
        if (ctx == null) {
            try {
                ctx = QQEnv.appContext();
            } catch (Throwable ignore) {
            }
        }
        if (ctx == null) return "（拿不到 Context，跳过）\n";

        File files = ctx.getFilesDir();
        String uin = currentUin();
        SharedPreferences sp = null;
        try {
            sp = ctx.getSharedPreferences("cathelper_console", Context.MODE_PRIVATE);
        } catch (Throwable ignore) {
        }

        int enc = 0, plain = 0, none = 0, highPlain = 0, diagN = 0;
        long highBytes = 0;
        StringBuilder rows = new StringBuilder();
        List<String> advice = new ArrayList<String>();

        for (Item it : ITEMS) {
            
            if (it.kind == SP_KEY) {
                String v = "";
                try {
                    v = sp == null ? "" : sp.getString(it.spKey, "");
                } catch (Throwable ignore) {
                }
                if (v == null || v.length() == 0) {
                    none++;
                    rows.append(row(it.level, it.name, "未设置", "-", false)).append('\n');
                } else {
                    plain++;
                    if (it.level == HIGH) {
                        highPlain++;
                        highBytes += v.length();
                        advice.add(it.name + " 明文存在 SharedPreferences（长度 " + v.length()
                                + "），等同登录凭证");
                    }
                    rows.append(row(it.level, it.name, "明文", v.length() + "B", true)).append('\n');
                }
                continue;
            }

            String p = it.path.replace("{uin}", uin == null ? "" : uin);
            if (p.length() == 0) continue;

            
            File f = resolveData(files, p, uin);
            if (!f.exists()) {
                none++;
                rows.append(row(it.level, it.name, "未生成", "-", false)).append('\n');
                continue;
            }

            long size = sizeOf(f);
            boolean sealed = false;
            if (it.kind == COOKIE_GC) {
                sealed = hasPrefix(f, CookieCipher.PREFIX);
            } else if (it.kind == LINE) {
                sealed = firstLineSealed(f);
            } else if (it.kind == KS_KEY) {
                sealed = contains(f, "apiKeyEnc");
                if (!sealed) {
                    
                    advice.add(it.name + " 里没有 apiKeyEnc 字段 → 旧版明文 Key，下次保存会自动加密");
                }
            } 

            boolean tmp = !sealed && it.kind == DIAG;
            if (sealed) {
                enc++;
            } else if (tmp) {

                diagN++;
            } else {
                plain++;
                if (it.level == HIGH) {
                    highPlain++;
                    highBytes += size;
                }
                if (it.level >= MID) {
                    advice.add(it.name + " 是明文（" + human(size) + "）");
                }
            }
            rows.append(row(it.level, it.name,
                    sealed ? "已加密" : (tmp ? "临时" : "明文"),
                    human(size), !sealed && !tmp && it.level >= MID)).append('\n');
        }

        sb.append("已知 ").append(ITEMS.size()).append(" 项 · 已加密 ")
          .append(enc).append(" · 明文 ").append(plain);
        if (diagN > 0) sb.append(" · 临时 ").append(diagN);
        sb.append(" · 未生成 ").append(none).append('\n');
        sb.append(rows);

        dumpSkipped = 0;
        List<String> unknown = unknownFiles(DataDir.global());
        if (!unknown.isEmpty()) {
            sb.append("未登记 ").append(unknown.size()).append(" 个");
            if (dumpSkipped > 0) sb.append("（另有 ").append(dumpSkipped).append(" 个抓包 dump 已跳过）");
            sb.append("：");
            for (int i = 0; i < unknown.size() && i < 6; i++) {
                if (i > 0) sb.append(" / ");
                sb.append(unknown.get(i));
            }
            if (unknown.size() > 6) sb.append(" …");
            sb.append('\n');
        }

        sb.append("高危明文 ").append(highPlain).append(" 项");
        if (highPlain > 0) sb.append(" · 合计 ").append(human(highBytes));
        sb.append('\n');
        
        if (!advice.isEmpty()) {
            sb.append("待加固：\n");
            for (int i = 0; i < advice.size() && i < 4; i++) {
                sb.append("  · ").append(advice.get(i)).append('\n');
            }
            if (advice.size() > 4) sb.append("  · …还有 ").append(advice.size() - 4).append(" 项\n");
        }
        return sb.toString();
    }

    private static File resolveData(File filesDir, String rel, String uin) {
        try {
            File f = new File(filesDir, rel);
            if (f.exists()) return f;
        } catch (Throwable ignore) {
        }
        String tail = rel.startsWith("cathelper/") ? rel.substring("cathelper/".length()) : rel;
        try {
            File root = DataDir.root();
            if (root != null) {
                File a = new File(root, tail);
                if (a.exists()) return a;
                if (uin != null && uin.length() > 0) {
                    File acct = new File(root, uin);
                    File b = new File(acct, uin + "/" + tail);
                    if (b.exists()) return b;
                    File c = new File(acct, tail);
                    if (c.exists()) return c;
                }
            }
        } catch (Throwable ignore) {
        }
        return new File(filesDir, rel);
    }

    private static String row(int level, String name, String state, String size, boolean bad) {
        char dot = level == HIGH ? '●' : level == MID ? '◐' : '○';
        String lv = level == HIGH ? "高" : level == MID ? "中" : "低";
        String mark = bad ? "  ←" : "";
        StringBuilder sb = new StringBuilder();
        sb.append(" ").append(dot).append(lv).append("  ");
        sb.append(pad(name, 14));
        sb.append(pad(state, 6));
        sb.append(pad(size, 9));
        sb.append(mark);
        return sb.toString();
    }

    private static String pad(String s, int w) {
        StringBuilder sb = new StringBuilder(s == null ? "" : s);
        
        while (sb.length() < w) sb.append(' ');
        return sb.toString();
    }

    private static String currentUin() {
        try {
            String u = QQEnv.currentUin();
            return u == null ? "" : u.trim();
        } catch (Throwable t) {
            return "";
        }
    }

    
    private static final java.util.regex.Pattern DUMP =
            java.util.regex.Pattern.compile(
                    "(ark|oidb|pkt|raw|dump|pkg)_\\d+_\\d+\\.[A-Za-z0-9]{1,6}");

    private static int dumpSkipped = 0;

    private static List<String> unknownFiles(File dir) {
        List<String> out = new ArrayList<String>();
        File[] fs = dir == null ? null : dir.listFiles();
        if (fs == null) return out;
        for (File f : fs) {
            String n = f.getName();
            if (KNOWN.contains(n)) continue;
            if (!f.isDirectory() && DUMP.matcher(n).matches()) { dumpSkipped++; continue; }
            
            if (f.isDirectory() && n.matches("\\d{4,}")) continue;
            
            boolean hit = false;
            for (String k : KNOWN) {
                if (k.endsWith("*") && n.startsWith(k.substring(0, k.length() - 1))) {
                    hit = true;
                    break;
                }
            }
            if (!hit) out.add(n + (f.isDirectory() ? "/" : ""));
        }
        Collections.sort(out);
        return out;
    }

    private static long sizeOf(File f) {
        try {
            if (f.isFile()) return f.length();
            long sum = 0;
            File[] fs = f.listFiles();
            if (fs != null) for (File c : fs) sum += sizeOf(c);
            return sum;
        } catch (Throwable t) {
            return 0;
        }
    }

    private static boolean hasPrefix(File f, String prefix) {
        if (f == null || !f.isFile()) return false;
        FileInputStream in = null;
        try {
            in = new FileInputStream(f);
            byte[] b = new byte[prefix.length()];
            int n = in.read(b);
            if (n != b.length) return false;
            return new String(b, "UTF-8").equals(prefix);
        } catch (Throwable t) {
            return false;
        } finally {
            if (in != null) try { in.close(); } catch (Throwable ignore) {}
        }
    }

    
    private static boolean firstLineSealed(File f) {
        if (f == null || !f.isFile()) return true;
        if (f.length() == 0) return true;
        java.io.BufferedReader r = null;
        try {
            r = new java.io.BufferedReader(
                    new java.io.InputStreamReader(new java.io.FileInputStream(f), "UTF-8"));
            int total = 0, sealed = 0;
            String line;
            while ((line = r.readLine()) != null && total < 5) {
                if (line.length() == 0) continue;
                total++;
                if (line.startsWith(com.cathelperawa.owo.app.cookie.CookieCipher.PREFIX)) sealed++;
            }
            if (total == 0) return true;

            return sealed * 2 > total;
        } catch (Throwable t) {
            return false;
        } finally {
            if (r != null) try { r.close(); } catch (Throwable ignore) {}
        }
    }

    private static boolean contains(File f, String needle) {
        if (f == null || !f.isFile() || f.length() > 1024 * 1024) return false;
        FileInputStream in = null;
        try {
            in = new FileInputStream(f);
            byte[] b = new byte[(int) f.length()];
            int n = in.read(b);
            if (n <= 0) return false;
            return new String(b, 0, n, "UTF-8").contains(needle);
        } catch (Throwable t) {
            return false;
        } finally {
            if (in != null) try { in.close(); } catch (Throwable ignore) {}
        }
    }

    private static String human(long b) {
        if (b < 1024) return b + "B";
        if (b < 1024 * 1024) return (b / 1024) + "KB";
        return (b / 1024 / 1024) + "MB";
    }
}
