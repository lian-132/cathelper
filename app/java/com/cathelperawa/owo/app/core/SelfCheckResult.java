package com.cathelperawa.owo.app.core;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SelfCheckResult {

    public static final class Item {
        
        public final String name;
        
        public final boolean ok;
        
        public final String detail;

        public Item(String name, boolean ok, String detail) {
            this.name = name == null ? "" : name;
            this.ok = ok;
            this.detail = detail == null ? "" : detail;
        }
    }

    private final List<Item> items = new ArrayList<Item>();

    public volatile long checkedAt = 0;

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public int passCount() {
        int n = 0;
        for (Item i : items) if (i.ok) n++;
        return n;
    }

    public int failCount() {
        int n = 0;
        for (Item i : items) if (!i.ok) n++;
        return n;
    }

    public int total() {
        return items.size();
    }

    public boolean allPass() {
        return !items.isEmpty() && failCount() == 0;
    }

    public List<Item> items() {
        return Collections.unmodifiableList(items);
    }

    public String firstFailReason() {
        for (Item i : items) {
            if (!i.ok) return i.name + (i.detail.length() > 0 ? "：" + i.detail : "");
        }
        return "";
    }

    
    void pass(String name, String detail) {
        items.add(new Item(name, true, detail));
    }

    void fail(String name, String detail) {
        items.add(new Item(name, false, detail));
    }

    void clear() {
        items.clear();
        checkedAt = 0;
    }

    public static final class Checker {

        private final SelfCheckResult r;
        private final ClassLoader cl;
        
        private boolean failed = false;

        Checker(SelfCheckResult r) {
            this(r, null);
        }

        Checker(SelfCheckResult r, ClassLoader cl) {
            this.r = r;
            this.cl = cl;
        }

        public void markFailed() {
            failed = true;
        }

        public boolean hasFailed() {
            return failed;
        }

        
        public Class<?> cls(String label, String... candidates) {
            if (failed) {
                r.fail(label, "跳过：前置检查未通过");
                return null;
            }
            for (String n : candidates) {
                try {
                    Class<?> c = Class.forName(n, false, loader());
                    if (c != null) {
                        r.pass(label, n);
                        return c;
                    }
                } catch (Throwable ignore) {
                }
            }
            r.fail(label, "类不存在：" + join(candidates));
            failed = true;
            return null;
        }

        
        public boolean method(String label, Class<?> owner, String name) {
            if (failed || owner == null) {
                r.fail(label, "跳过：前置检查未通过");
                return false;
            }
            for (Method m : owner.getDeclaredMethods()) {
                if (name.equals(m.getName())) {
                    r.pass(label, owner.getSimpleName() + "#" + name);
                    return true;
                }
            }
            r.fail(label, owner.getSimpleName() + " 里没有方法 " + name);
            return false;
        }

        public boolean methodExists(String label, Class<?> owner, String name) {
            return method(label, owner, name);
        }

        
        public boolean field(String label, Class<?> owner, String name) {
            if (failed || owner == null) {
                r.fail(label, "跳过：前置检查未通过");
                return false;
            }
            for (Field f : owner.getDeclaredFields()) {
                if (name.equals(f.getName())) {
                    r.pass(label, owner.getSimpleName() + "." + name);
                    return true;
                }
            }
            r.fail(label, owner.getSimpleName() + " 里没有字段 " + name);
            return false;
        }

        
        public boolean assertThat(String label, boolean cond) {
            return assertThat(label, cond, "");
        }

        public boolean assertThat(String label, boolean cond, String detail) {
            if (cond) {
                r.pass(label, detail);
            } else {
                r.fail(label, detail.length() > 0 ? detail : "条件不成立");
            }
            return cond;
        }

        public void pass(String label, String detail) {
            r.pass(label, detail);
        }

        public void fail(String label, String detail) {
            r.fail(label, detail);
        }

        private ClassLoader loader() {
            if (cl != null) return cl;
            ClassLoader c = Thread.currentThread().getContextClassLoader();
            return c != null ? c : SelfCheckResult.class.getClassLoader();
        }

        private static String join(String[] a) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < a.length; i++) {
                if (i > 0) sb.append(" / ");
                sb.append(a[i]);
            }
            return sb.toString();
        }
    }

    public String summary() {
        if (items.isEmpty()) return "未自检";
        StringBuilder sb = new StringBuilder();
        sb.append(passCount()).append('/').append(total());
        if (allPass()) {
            sb.append(" 通过");
        } else {
            sb.append(" 通过 · ").append(failCount()).append(" 项异常");
        }
        return sb.toString();
    }
}
