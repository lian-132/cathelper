package com.cathelperawa.owo.app.script;

import com.cathelperawa.owo.app.core.ConfigStore;

import java.io.File;
import java.io.RandomAccessFile;

public final class ModLevel {

    public static final int NORMAL = 0;

    public static final int GENERAL = 1;

    public static final int AI_ONLY = 2;

    public static final int KERNEL_ADD = 3;

    public static final int KERNEL_MOD = 4;

    public static final int DANGER = 5;

    public static final String KEY_KERNEL = "mod_kernel_enabled";

    private static final java.util.Map<String, long[]> LEVEL_CACHE =
            new java.util.HashMap<String, long[]>();

    private ModLevel() {}

    public static final String KEY_DANGER = "mod_danger_init";

    public static boolean dangerAllowed() {
        try {
            return ConfigStore.getBool(KEY_DANGER);
        } catch (Throwable t) {
            return false;
        }
    }

    public static void setDangerAllowed(boolean on) {
        try {
            ConfigStore.setBool(KEY_DANGER, on);
        } catch (Throwable ignore) {
        }
    }

    public static boolean kernelAllowed() {
        try {
            return ConfigStore.getBool(KEY_KERNEL);
        } catch (Throwable t) {
            return false;
        }
    }

    public static void setKernelAllowed(boolean on) {
        try {
            ConfigStore.setBool(KEY_KERNEL, on);
        } catch (Throwable ignore) {
        }
    }

    public static boolean isKernel(int level) {
        return level == KERNEL_ADD || level == KERNEL_MOD || level == DANGER;
    }

    public static String label(int level) {
        switch (level) {
            case GENERAL: return "通用";
            case AI_ONLY: return "AI";
            case KERNEL_ADD: return "内核·增";
            case KERNEL_MOD: return "内核·改";
            case DANGER: return "危险";
            default: return "普通";
        }
    }

    public static String desc(int level) {
        switch (level) {
            case GENERAL: return "注册了 AI 工具：人可以用命令，AI 也能调用";
            case AI_ONLY: return "只给 AI 用：界面上没有入口，只有模型能触发";
            case KERNEL_ADD: return "内核·增：会给模块加新页面/新功能，不改原有逻辑";
            case KERNEL_MOD: return "内核·改：会改变模块原有行为（页面/判断/算法），出错可能让 TIM 崩溃";
            case DANGER: return "危险：脚本直接调系统级 API（杀进程/执行命中/写系统目录），"
                    + "不是改模块，是动整台手机";
            default: return "普通脚本：只加命令/菜单/面板，模块本身不变";
        }
    }

    public static int color(int level) {
        switch (level) {
            case GENERAL: return 0xFF2F6BFF;
            case AI_ONLY: return 0xFF7C4DFF;
            case KERNEL_ADD: return 0xFFE8820C;
            case KERNEL_MOD: return 0xFFD9480F;
            case DANGER: return 0xFFC0392B;
            default: return 0xFF8A94A6;
        }
    }

    public static int bg(int level) {
        switch (level) {
            case GENERAL: return 0xFFE8EFFF;
            case AI_ONLY: return 0xFFF1EAFF;
            case KERNEL_ADD: return 0xFFFFF3E6;
            case KERNEL_MOD: return 0xFFFFE8DC;
            case DANGER: return 0xFFFFD9D6;
            default: return 0xFFF1F3F7;
        }
    }

    public static int of(String name) {
        try {
            java.io.File dir = ScriptHost.dir();
            java.io.File f = dir == null ? null : new java.io.File(dir,
                    name.endsWith(".qqs") ? name : name + ".qqs");
            long mtime = (f != null && f.exists()) ? f.lastModified() : 0;
            synchronized (LEVEL_CACHE) {
                long[] c = LEVEL_CACHE.get(name);
                if (c != null && c[0] == mtime) return (int) c[1];
            }
            int lv = computeLevel(name);
            synchronized (LEVEL_CACHE) {
                LEVEL_CACHE.put(name, new long[]{mtime, lv});
            }
            return lv;
        } catch (Throwable t) {
            return NORMAL;
        }
    }

    private static int computeLevel(String name) {
        try {
            ScriptMeta meta = ScriptHost.metaOf(name);
            String code = codeOf(name);
            if (code == null) return NORMAL;

            boolean danger = has(code, "killProcess(")
                    || has(code, "System.exit(")
                    || has(code, ".exec(") || has(code, "ProcessBuilder")
                    || has(code, "Runtime.getRuntime")
                    || has(code, "mount(") || has(code, "reboot(")
                    || has(code, "/system/") || has(code, "/data/adb");

            boolean mod = has(code, "ctx.patch(") || has(code, "ctx.strategy(")
                    || has(code, ".override(") || has(code, ".wrap(")
                    || has(code, "ctx.override(") || has(code, "ctx.wrap(")

                    || has(code, "ctx.style(") || has(code, "ctx.theme(")
                    || has(code, "ctx.icon(") || has(code, "ctx.bg(")
                    || has(code, "ctx.text(") || has(code, "ctx.hide(")

                    || has(code, "ctx.configForce(") || has(code, "ctx.configClear(");

            boolean add = has(code, "ctx.screen(") || has(code, "ctx.entry(")
                    || has(code, "ctx.page(") || has(code, "ctx.onActivity(")
                    || has(code, "ctx.dialog(") || has(code, "ctx.overlay(")
                    || has(code, "ctx.setting(");

            boolean ai = has(code, "ctx.tag(") || has(code, "ctx.ai(")
                    || (meta != null && meta.aiOnly);

            int lv = NORMAL;
            if (danger) lv = DANGER;
            else if (mod) lv = KERNEL_MOD;
            else if (add) lv = KERNEL_ADD;
            else if (ai) lv = AI_ONLY;
            else if (has(code, "ctx.tool(")) lv = GENERAL;

            try {
                String ext = "";
                int dot = name == null ? -1 : name.lastIndexOf('.');
                if (dot >= 0) ext = name.substring(dot);
                String prov = ScriptEngines.providerOf(ext);
                if (prov != null && !prov.equals(name)) {
                    int pl = of(prov);
                    if (pl > lv) lv = pl;
                }
            } catch (Throwable ignore) {
            }
            return lv;
        } catch (Throwable t) {
            return NORMAL;
        }
    }

    public static boolean isHostLevel(String name) {
        try {
            String code = codeOf(name);
            if (code == null) return false;
            return has(code, "ctx.method(") || has(code, "ctx.patch(")
                    || has(code, "ctx.hook(") || has(code, "ctx.unpatch(")
                    || has(code, "ctx.exec(") || has(code, "ctx.eval(")
                    || has(code, "ctx.newEngine(") || has(code, "ctx.registerEngine(");
        } catch (Throwable t) {
            return false;
        }
    }

    private static boolean has(String code, String token) {
        return code.indexOf(token) >= 0;
    }

    private static String codeOf(String name) {
        try {
            File dir = ScriptHost.dir();
            if (dir == null || name == null) return null;
            File f = ScriptHost.entryOf(name);
            if (f == null || !f.exists()) f = new File(dir, name);
            if (f == null || !f.exists()) f = new File(dir, name + ".qqs");
            if (!f.exists()) return null;
            RandomAccessFile r = new RandomAccessFile(f, "r");
            try {
                long len = Math.min(r.length(), 64 * 1024);
                byte[] b = new byte[(int) len];
                r.readFully(b);
                return new String(b, "UTF-8");
            } finally {
                try { r.close(); } catch (Throwable ignore) {}
            }
        } catch (Throwable t) {
            return null;
        }
    }
}
