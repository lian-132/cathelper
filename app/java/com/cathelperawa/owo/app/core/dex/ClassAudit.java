package com.cathelperawa.owo.app.core.dex;

import android.content.Context;

import com.cathelperawa.owo.app.core.HookLog;

import java.io.BufferedReader;
import java.io.StringReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public final class ClassAudit {

    private static final String OUT = "class_audit.txt";

    private static boolean isHost(String n) {
        return n.startsWith("com.tencent.") || n.startsWith("mqq.") || n.startsWith("com.tenpay.");
    }

    private static int skippedNonClass = 0;

    private static boolean skip(String n) {
        return n.startsWith("com.cathelperawa.")
                || n.startsWith("java.") || n.startsWith("javax.")
                || n.startsWith("android.") || n.startsWith("androidx.")
                || n.startsWith("dalvik.") || n.startsWith("kotlin.");
    }

    private static final String[] NOT_CLASS = {

            
            
            "com.tencent.tmgp",
            "com.tencent.lolm",
            "com.tencent.kapu",
            "com.dianping",
            "tv.danmaku",
            
            "com.tencent.tuwen",
            "com.tencent.structmsg",
            "com.tencent.miniapp",
            "com.tencent.mannounce",
            "com.tencent.qcircle",
            "com.tencent.qqsports",
            "com.tencent.gamecenter",
            "com.tencent.mobileqq.gamecenter",
            "com.tencent.map",
            
            "guild.share", "qzone.share", "tianxuan.share",
            "mqqapi", "mqqopensdkapi", "mqqwpa",
    };

    private static boolean notAClass(String n) {
        for (String p : NOT_CLASS) {
            if (n.equals(p) || n.startsWith(p + ".")) return true;
        }

        int dot = n.lastIndexOf('.');
        String tail = dot >= 0 ? n.substring(dot + 1) : n;
        return isEnglishWord(tail);
    }

    private static boolean isEnglishWord(String tail) {
        if (tail.length() < 3) return false;
        if (!tail.matches("[a-z]+")) return false;
        
        if (tail.length() > 3) return true;
        
        String[] words = {"lua", "app", "uid", "uin", "url", "img", "pay", "web", "log",
                "dex", "so", "xml", "json"};
        for (String w : words) if (w.equals(tail)) return true;
        return false;
    }

    private ClassAudit() {}

    public static String run(Context ctx, DexIndex ix) {
        return run(ctx, ix, ClassAudit.class.getClassLoader());
    }

    public static String run(Context ctx, DexIndex ix, ClassLoader moduleCl) {
        
        skippedNonClass = 0;
        Map<String, List<String>> sites = loadSites(ctx);

        int healthy = 0, broken = 0, external = 0;
        List<String> brokenReport = new ArrayList<>();

        for (Map.Entry<String, List<String>> e : sites.entrySet()) {
            String site = e.getKey();
            List<String> cands = e.getValue();

            List<String> hostCands = new ArrayList<>();
            for (String c : cands) if (isHost(c)) hostCands.add(c);
            if (hostCands.isEmpty()) { external++; continue; }

            boolean anyAlive = false, anyOwn = false;
            for (String c : hostCands) {
                if (isOwn(c, moduleCl)) { anyOwn = true; anyAlive = true; continue; }
                if (ix.contains(c)) { anyAlive = true; break; }
            }
            if (anyAlive) { healthy++; continue; }

            broken++;
            StringBuilder sb = new StringBuilder();
            sb.append("✗ ").append(site).append('\n');
            for (String c : hostCands) {
                sb.append("      候选 ").append(c).append("  → 不存在\n");
                List<String> near = similar(ix, c);
                if (!near.isEmpty()) {
                    sb.append("      近似: ");
                    for (int i = 0; i < near.size(); i++) {
                        if (i > 0) sb.append(" / ");
                        sb.append(near.get(i));
                    }
                    sb.append('\n');
                }
            }
            brokenReport.add(sb.toString());
        }

        StringBuilder out = new StringBuilder();
        out.append("== 硬编码类名体检 ==\n");
        out.append("调用点: ").append(sites.size())
           .append("   健康: ").append(healthy)
           .append("   全废: ").append(broken)
           .append("   外部(跳过): ").append(external)
           .append("   非类名(跳过): ").append(skippedNonClass).append("\n\n");
        if (broken == 0) {
            out.append("✓ 没有「全部候选都失效」的调用点\n");
        } else {
            out.append("── 全部候选都失效的调用点（这些 hook 已经不工作了）──\n");
            for (String s : brokenReport) out.append(s);
        }

        write(ctx, out.toString());
        HookLog.i("类名体检：" + healthy + " 健康 / " + broken + " 全废 / " + external + " 外部");
        return out.toString();
    }

    private static List<String> similar(DexIndex ix, String full) {
        int dot = full.lastIndexOf('.');
        String simple = dot >= 0 ? full.substring(dot + 1) : full;
        if (simple.length() < 3) return Collections.emptyList();
        List<String> hits = ix.grep(Pattern.compile(Pattern.quote(simple) + "(\\$|$)"));
        List<String> host = new ArrayList<>();
        for (String h : hits) if (isHost(h)) host.add(h);
        return host.size() > 4 ? host.subList(0, 4) : host;
    }

    private static boolean isOwn(String n, ClassLoader cl) {
        if (cl == null) return false;
        try {
            Class.forName(n, false, cl);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private static Map<String, List<String>> loadSites(Context ctx) {
        
        List<String[]> rows = new ArrayList<>();     
        InputStream in = null;
        try {
            BufferedReader r = new BufferedReader(new StringReader(""));
            String line;
            for (String raw : HardcodedClasses.LIST) {
                line = raw;
                int tab = line.indexOf('\t');
                if (tab <= 0) continue;
                String site = line.substring(0, tab).trim();
                String name = line.substring(tab + 1).trim();
                if (name.isEmpty() || skip(name)) continue;
                if (notAClass(name)) { skippedNonClass++; continue; }
                int colon = site.lastIndexOf(':');
                if (colon < 0) continue;
                rows.add(new String[]{site.substring(0, colon), site.substring(colon + 1), name});
            }
        } catch (Throwable t) {
            HookLog.w("类名名单读取失败（构建时没生成 HardcodedClasses？）");
        } finally {
            if (in != null) try { in.close(); } catch (Throwable ignored) {}
        }

        Collections.sort(rows, new Comparator<String[]>() {
            @Override public int compare(String[] a, String[] b) {
                int c = a[0].compareTo(b[0]);
                return c != 0 ? c : Integer.compare(parseInt(a[1]), parseInt(b[1]));
            }
        });
        Map<String, List<String>> map = new LinkedHashMap<>();
        String curFile = null;
        int curEnd = -999;
        List<String> cur = null;
        String curKey = null;
        for (String[] row : rows) {
            int ln = parseInt(row[1]);
            if (!row[0].equals(curFile) || ln - curEnd > 3) {
                curKey = row[0] + ":" + row[1];
                cur = new ArrayList<>(3);
                map.put(curKey, cur);
                curFile = row[0];
            }
            curEnd = ln;
            if (!cur.contains(row[2])) cur.add(row[2]);
        }
        return map;
    }

    private static int parseInt(String s) {
        try { return Integer.parseInt(s); } catch (Throwable t) { return 0; }
    }

    private static void write(Context ctx, String text) {
        File f = new File(ctx.getFilesDir(), OUT);
        try (FileOutputStream os = new FileOutputStream(f)) {
            OutputStreamWriter w = new OutputStreamWriter(os, "UTF-8");
            w.write(text);
            w.flush();
        } catch (Throwable t) {
            HookLog.e("类名体检写文件失败", t);
        }
    }
}
