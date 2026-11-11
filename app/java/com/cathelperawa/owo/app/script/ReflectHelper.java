package com.cathelperawa.owo.app.script;

import com.cathelperawa.owo.app.bridge.QQEnv;
import com.cathelperawa.owo.app.core.HookLog;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

public final class ReflectHelper {

    private static List<String> ALL_CLASSES;
    private static boolean scanned = false;

    private ReflectHelper() {}

    private static synchronized List<String> allClasses() {
        if (scanned) return ALL_CLASSES == null ? new ArrayList<String>() : ALL_CLASSES;
        scanned = true;
        List<String> out = new ArrayList<String>();
        try {
            String apk = apkPath();
            if (apk != null && new java.io.File(apk).exists()) {
                dalvik.system.DexFile df = new dalvik.system.DexFile(apk);
                Enumeration<String> en = df.entries();
                while (en.hasMoreElements()) {
                    out.add(en.nextElement());
                }
                try { df.close(); } catch (Throwable ignore) {}
            }
            HookLog.i("[反射] 扫描到 " + out.size() + " 个类");
        } catch (Throwable t) {
            HookLog.w("[反射] 扫描失败 " + t);
        }
        ALL_CLASSES = out;
        return out;
    }

    public static List<String> findClasses(String keyword, int limit) {
        List<String> out = new ArrayList<String>();
        if (keyword == null || keyword.length() == 0) return out;
        String k = keyword.toLowerCase();
        List<String> all = allClasses();
        for (int i = 0; i < all.size() && out.size() < limit; i++) {
            String c = all.get(i);
            if (c.toLowerCase().contains(k)) out.add(c);
        }
        Collections.sort(out);
        return out;
    }

    public static List<String> methodsOf(String className, int limit) {
        List<String> out = new ArrayList<String>();
        if (className == null) return out;
        try {
            Class<?> cls = Class.forName(className, false, ReflectHelper.class.getClassLoader());
            Method[] ms = cls.getDeclaredMethods();
            for (int i = 0; i < ms.length && out.size() < limit; i++) {
                out.add(sig(ms[i]));
            }
            Collections.sort(out);
        } catch (Throwable t) {
            out.add("读取失败：" + t);
        }
        return out;
    }

    public static List<String> fieldsOf(String className, int limit) {
        List<String> out = new ArrayList<String>();
        if (className == null) return out;
        try {
            Class<?> cls = Class.forName(className, false, ReflectHelper.class.getClassLoader());
            Field[] fs = cls.getDeclaredFields();
            for (int i = 0; i < fs.length && out.size() < limit; i++) {
                out.add(fs[i].getType().getSimpleName() + " " + fs[i].getName());
            }
            Collections.sort(out);
        } catch (Throwable t) {
            out.add("读取失败：" + t);
        }
        return out;
    }

    public static List<String> ctorsOf(String className) {
        List<String> out = new ArrayList<String>();
        try {
            Class<?> cls = Class.forName(className, false, ReflectHelper.class.getClassLoader());
            Constructor<?>[] cs = cls.getDeclaredConstructors();
            for (int i = 0; i < cs.length; i++) {
                StringBuilder sb = new StringBuilder("(");
                Class<?>[] ps = cs[i].getParameterTypes();
                for (int j = 0; j < ps.length; j++) {
                    if (j > 0) sb.append(", ");
                    sb.append(ps[j].getSimpleName());
                }
                sb.append(")");
                out.add(sb.toString());
            }
        } catch (Throwable t) {
            out.add("读取失败：" + t);
        }
        return out;
    }

    public static boolean classExists(String className) {
        try {
            Class.forName(className, false, ReflectHelper.class.getClassLoader());
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private static String sig(Method m) {
        StringBuilder sb = new StringBuilder();
        sb.append(m.getReturnType().getSimpleName()).append(' ')
          .append(m.getName()).append('(');
        Class<?>[] ps = m.getParameterTypes();
        for (int i = 0; i < ps.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(ps[i].getSimpleName());
        }
        sb.append(')');
        return sb.toString();
    }

    private static String apkPath() {
        try {
            android.app.Activity a = QQEnv.topActivity();
            if (a != null) return a.getPackageCodePath();
        } catch (Throwable ignore) {
        }
        try {
            Class<?> atCls = Class.forName("android.app.ActivityThread");
            Object at = atCls.getMethod("currentActivityThread").invoke(null);
            Object app = atCls.getMethod("getApplication").invoke(at);
            if (app instanceof android.content.Context) {
                return ((android.content.Context) app).getPackageCodePath();
            }
        } catch (Throwable ignore) {
        }
        try {
            return ReflectHelper.class.getProtectionDomain()
                    .getCodeSource().getLocation().getPath();
        } catch (Throwable ignore) {
        }
        return null;
    }
}
