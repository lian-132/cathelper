package com.cathelperawa.owo.app.util;

public final class Safe {

    private Safe() {}

    public interface Action {
        void run() throws Throwable;
    }

    public interface Callable<T> {
        T call() throws Throwable;
    }

    

    
    public static void probe(String what, Action a) {
        try {
            a.run();
        } catch (Throwable t) {
            CatLog.d("[probe] " + what + " 失败（可忽略）: " + brief(t));
        }
    }

    public static <T> T probe(String what, Callable<T> c) {
        try {
            return c.call();
        } catch (Throwable t) {
            CatLog.d("[probe] " + what + " 失败（可忽略）: " + brief(t));
            return null;
        }
    }

    

    
    public static <T> T attempt(String what, Callable<T> c, T fallback) {
        try {
            return c.call();
        } catch (Throwable t) {
            CatLog.w("[attempt] " + what + " 失败，返回兜底值: " + brief(t));
            return fallback;
        }
    }

    public static boolean attempt(String what, Action a) {
        try {
            a.run();
            return true;
        } catch (Throwable t) {
            CatLog.w("[attempt] " + what + " 失败: " + brief(t));
            return false;
        }
    }

    

    
    public static <T> T critical(String what, Callable<T> c) {
        try {
            return c.call();
        } catch (Throwable t) {
            CatLog.e("[critical] " + what + " 失败", t);
            throw rethrow(t);
        }
    }

    public static void critical(String what, Action a) {
        try {
            a.run();
        } catch (Throwable t) {
            CatLog.e("[critical] " + what + " 失败", t);
            throw rethrow(t);
        }
    }

    

    
    @SuppressWarnings("unchecked")
    private static <E extends Throwable> RuntimeException rethrow(Throwable t) throws E {
        throw (E) t;
    }

    private static String brief(Throwable t) {
        if (t == null) return "null";
        String m = t.getMessage();
        return t.getClass().getSimpleName() + (m == null ? "" : (": " + m));
    }
}
