package com.cathelperawa.owo.app.core;

import android.content.Context;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

public final class ConfigBackup {

    public static final String META_MODULE_VER = "__module_ver";
    public static final String META_EXPORTED_AT = "__exported_at";
    public static final String META_COUNT = "__count";

    private static final String[] SENSITIVE = {
            "uin", "uid", "token", "cookie", "password", "passwd", "secret",
            "skey", "p_skey", "authkey", "guid", "imei", "mac"
    };

    private ConfigBackup() {}

    public static final class ExportResult {
        public final String text;
        public final int count;
        public final int skipped;
        public ExportResult(String text, int count, int skipped) {
            this.text = text;
            this.count = count;
            this.skipped = skipped;
        }
    }

    public static ExportResult export(Context ctx, int moduleVer) {
        return exportDir(rootOf(ctx), moduleVer);
    }

    public static ExportResult exportDir(File filesRoot, int moduleVer) {
        Properties p = loadRaw(filesRoot);

        TreeMap<String, String> all = new TreeMap<String, String>();
        for (String name : p.stringPropertyNames()) {
            all.put(name, p.getProperty(name));
        }

        StringBuilder sb = new StringBuilder(1024);
        sb.append("# 猫娘助手配置备份\n");
        sb.append(META_MODULE_VER).append('=').append(moduleVer).append('\n');
        sb.append(META_EXPORTED_AT).append('=').append(now()).append('\n');

        int count = 0, skipped = 0;
        String lastGroup = null;
        for (Map.Entry<String, String> e : all.entrySet()) {
            if (isSensitive(e.getKey())) { skipped++; continue; }

            String val = e.getValue();
            if (val == null || val.length() == 0) { continue; }

            String group = groupOf(e.getKey());
            if (!group.equals(lastGroup)) {
                sb.append('\n').append("# ── ").append(group).append(" ──\n");
                lastGroup = group;
            }
            sb.append(escape(e.getKey())).append('=')
                    .append(escape(e.getValue())).append('\n');
            count++;
        }
        sb.append('\n').append(META_COUNT).append('=').append(count).append('\n');
        return new ExportResult(sb.toString(), count, skipped);
    }

    public static File exportToFile(Context ctx, File out, int moduleVer) {
        return exportDirToFile(rootOf(ctx), out, moduleVer);
    }

    public static File exportDirToFile(File filesRoot, File out, int moduleVer) {
        try {
            ExportResult r = exportDir(filesRoot, moduleVer);
            File parent = out.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();
            try (FileOutputStream fos = new FileOutputStream(out)) {
                fos.write(r.text.getBytes("UTF-8"));
                fos.flush();
            }
            return out;
        } catch (Throwable t) {
            return null;
        }
    }

    public static final class ImportResult {
        public final boolean ok;
        public final int applied;
        public final String message;
        public ImportResult(boolean ok, int applied, String message) {
            this.ok = ok;
            this.applied = applied;
            this.message = message;
        }
    }

    public static ImportResult importText(Context ctx, String text, int currentModuleVer) {
        return importDir(rootOf(ctx), text, currentModuleVer);
    }

    public static ImportResult importDir(File filesRoot, String text, int currentModuleVer) {
        if (text == null) return new ImportResult(false, 0, "内容为空");
        Properties from = new Properties();
        try {
            from.load(new java.io.StringReader(text));
        } catch (Throwable t) {
            return new ImportResult(false, 0, "格式解析失败：" + t.getMessage());
        }
        if (from.isEmpty()) return new ImportResult(false, 0, "没解析到任何配置");

        int backupVer = -1;
        try { backupVer = Integer.parseInt(from.getProperty(META_MODULE_VER, "-1")); }
        catch (Throwable ignore) { }
        StringBuilder note = new StringBuilder();
        if (backupVer > 0 && currentModuleVer > 0 && backupVer != currentModuleVer) {
            note.append("备份来自模块 v").append(backupVer)
                    .append("，当前 v").append(currentModuleVer).append("；");
        }

        Properties cur = loadRaw(filesRoot);
        int applied = 0;
        for (String name : from.stringPropertyNames()) {
            if (name.startsWith("__")) continue;
            if (isSensitive(name)) continue;
            cur.setProperty(name, from.getProperty(name));
            applied++;
        }
        if (!saveRaw(filesRoot, cur)) return new ImportResult(false, 0, "写配置失败");
        HookConfig.reload();
        note.append("导入 ").append(applied).append(" 条");
        return new ImportResult(true, applied, note.toString());
    }

    public static ImportResult importFile(Context ctx, File f, int currentModuleVer) {
        return importDirFile(rootOf(ctx), f, currentModuleVer);
    }

    public static ImportResult importDirFile(File filesRoot, File f, int currentModuleVer) {
        try {
            if (f == null || !f.exists()) return new ImportResult(false, 0, "文件不存在");
            StringBuilder sb = new StringBuilder();
            try (FileInputStream in = new FileInputStream(f)) {
                byte[] buf = new byte[4096];
                int n;
                while ((n = in.read(buf)) > 0) sb.append(new String(buf, 0, n, "UTF-8"));
            }
            return importDir(filesRoot, sb.toString(), currentModuleVer);
        } catch (Throwable t) {
            return new ImportResult(false, 0, "读取失败：" + t.getMessage());
        }
    }

    private static File rootOf(Context ctx) {
        return ctx == null ? null : ctx.getFilesDir();
    }

    private static Properties loadRaw(File filesRoot) {
        Properties p = new Properties();
        if (filesRoot == null) return p;
        try {
            File f = new File(filesRoot, "console");
            if (f.exists()) {
                try (FileInputStream in = new FileInputStream(f)) { p.load(in); }
            }
        } catch (Throwable ignore) {
        }
        return p;
    }

    private static boolean saveRaw(File filesRoot, Properties p) {
        if (filesRoot == null || p == null) return false;
        try {
            File f = new File(filesRoot, "console");
            try (FileOutputStream out = new FileOutputStream(f)) {
                p.store(out, "cathelper config");
                out.flush();
            }
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private static String groupOf(String key) {
        if (key == null) return "其它";
        String k = key.toLowerCase(java.util.Locale.ROOT);

        if (k.contains(".")) {
            String cls = k.substring(k.lastIndexOf('.') + 1);
            if (cls.startsWith("cat")) return "猫娘化";
            if (cls.startsWith("featurecat")) return "猫娘化";
            if (cls.startsWith("antirevoke")) return "防撤回";
            if (cls.startsWith("feature")) return "其它功能";
            return cls;
        }
        if (k.startsWith("cat_") || k.equals("cat")) return "猫娘化";
        if (k.startsWith("chat_")) return "聊天工具";
        if (k.startsWith("u_") || k.startsWith("unlock_")) return "去除限制";
        if (k.startsWith("hook_")) return "抓取/记录";
        return "其它";
    }

    public static String groupOfForTest(String key) {
        return groupOf(key);
    }

    public static boolean isSensitive(String key) {
        if (key == null) return false;
        String k = key.toLowerCase(java.util.Locale.ROOT);
        for (String s : SENSITIVE) {
            if (k.contains(s)) return true;
        }
        return false;
    }

    private static String escape(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length() + 8);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\': sb.append("\\\\"); break;
                case '=':  sb.append("\\=");  break;
                case ':':  sb.append("\\:");  break;
                case '\n': sb.append("\\n");  break;
                case '\r': sb.append("\\r");  break;
                default:   sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String now() {
        try {
            return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss",
                    java.util.Locale.ROOT).format(new java.util.Date());
        } catch (Throwable t) {
            return String.valueOf(System.currentTimeMillis());
        }
    }

    public static List<String> groups(Context ctx) {
        return groupsDir(rootOf(ctx));
    }

    public static List<String> groupsDir(File filesRoot) {
        Properties p = loadRaw(filesRoot);
        List<String> out = new ArrayList<String>();
        for (String name : p.stringPropertyNames()) {
            String g = groupOf(name);
            if (!out.contains(g)) out.add(g);
        }
        Collections.sort(out);
        return out;
    }
}
