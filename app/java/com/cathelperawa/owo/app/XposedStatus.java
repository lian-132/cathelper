package com.cathelperawa.owo.app;

public class XposedStatus {

    public static String queryActive() {
        return null;
    }

    public static final class Info {
        public boolean active = false;
        public int api = 0;
        public String framework = "";
    }

    public static Info query() {
        Info info = new Info();
        try {
            String r = queryActive();
            if (r == null) return info;
            String[] p = r.split("\\|", -1);
            info.active = true;
            if (p.length > 1) {
                try {
                    info.api = Integer.parseInt(p[1].trim());
                } catch (Throwable ignore) {
                }
            }
            if (p.length > 2) info.framework = p[2].trim();
        } catch (Throwable ignore) {
        }
        return info;
    }
}
