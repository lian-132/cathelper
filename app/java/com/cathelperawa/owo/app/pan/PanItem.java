package com.cathelperawa.owo.app.pan;

public final class PanItem {

    public final String fid;
    public final String name;
    public final long size;
    public final boolean dir;
    
    public final String token;

    public PanItem(String fid, String name, long size, boolean dir, String token) {
        this.fid = fid == null ? "" : fid;
        this.name = name == null || name.length() == 0 ? "（未命名）" : name;
        this.size = size;
        this.dir = dir;
        this.token = token == null ? "" : token;
    }

    public String sizeText() {
        if (dir) return "文件夹";
        return human(size);
    }

    public static String human(long n) {
        if (n <= 0) return "--";
        if (n < 1024) return n + " B";
        double k = n / 1024.0;
        if (k < 1024) return String.format(java.util.Locale.CHINA, "%.1f KB", k);
        double m = k / 1024.0;
        if (m < 1024) return String.format(java.util.Locale.CHINA, "%.1f MB", m);
        return String.format(java.util.Locale.CHINA, "%.2f GB", m / 1024.0);
    }
}
