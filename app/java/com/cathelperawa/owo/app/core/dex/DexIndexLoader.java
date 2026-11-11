package com.cathelperawa.owo.app.core.dex;

import android.content.Context;
import com.cathelperawa.owo.app.core.Bg;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class DexIndexLoader {

    public interface Callback {
        void onReady(DexIndex index);
        void onFailed(Throwable t);
    }

    private static final String TAG = "CatDexIndex";
    private static final String CACHE_DIR = "dexidx";

    private static volatile DexIndex sIndex;
    private static volatile String sKey;
    private static volatile boolean sBuilding;
    private static volatile boolean sLastHitCache;
    private static volatile String sLastError;

    private DexIndexLoader() {}

    public static DexIndex get(Context ctx) {
        return get(ctx, false);
    }

    public static synchronized DexIndex get(Context ctx, boolean withStrings) {
        Context app = ctx.getApplicationContext();
        String apk = hostApkPath(app);
        if (apk == null) return null;
        String key = cacheKey(app, apk) + (withStrings ? "_s" : "");
        if (key.equals(sKey) && sIndex != null) return sIndex;

        synchronized (DexIndexLoader.class) {
            if (key.equals(sKey) && sIndex != null) return sIndex;

            
            
            if (!key.equals(sKey)) { sIndex = null; sKey = null; }

            File f = new File(cacheDir(app), "idx_" + key + ".bin");
            if (f.isFile()) {
                try (DataInputStream in = new DataInputStream(
                        new BufferedInputStream(new FileInputStream(f)))) {
                    DexIndex ix = DexIndex.readFrom(in);
                    sIndex = ix;
                    sKey = key;
                    sLastHitCache = true;
                    return ix;
                } catch (Throwable t) {

                    sLastError = t.toString();
                    com.cathelperawa.owo.app.core.HookLog.w("索引缓存失效，重建：" + t);
                    f.delete();
                }
            }

            DexIndex ix = build(apk, withStrings);
            if (ix == null) return null;
            sIndex = ix;
            sKey = key;
            sLastHitCache = false;
            writeCache(f, ix);
            return ix;
        }
    }

    public static void getAsync(final Context ctx, final Callback cb) {
        if (sIndex != null) { cb.onReady(sIndex); return; }
        if (sBuilding) {                       
            final Handler h = new Handler(Looper.getMainLooper());
            final long deadline = System.currentTimeMillis() + 60_000;
            h.postDelayed(new Runnable() {
                @Override public void run() {
                    if (sIndex != null) { cb.onReady(sIndex); return; }
                    if (System.currentTimeMillis() > deadline) { cb.onFailed(new IOException("建索引超时")); return; }
                    h.postDelayed(this, 250);
                }
            }, 250);
            return;
        }
        sBuilding = true;
        final Context app = ctx.getApplicationContext();
        Bg.run(new Runnable() {
            @Override public void run() {
                try {
                    final DexIndex ix = get(app);
                    post(new Runnable() {
                        @Override public void run() {
                            sBuilding = false;
                            if (ix != null) cb.onReady(ix); else cb.onFailed(new IOException("宿主 APK 不可读"));
                        }
                    });
                } catch (final Throwable t) {
                    post(new Runnable() {
                        @Override public void run() { sBuilding = false; cb.onFailed(t); }
                    });
                }
            }
        }, "cat-dexidx");
    }

    private static void post(Runnable r) {
        new Handler(Looper.getMainLooper()).post(r);
    }

    
    private static DexIndex build(String apkPath, boolean withStrings) {
        ZipFile zf = null;
        try {
            zf = new ZipFile(apkPath);
            List<String> names = new ArrayList<>();
            Enumeration<? extends ZipEntry> en = zf.entries();
            while (en.hasMoreElements()) {
                String n = en.nextElement().getName();
                if (isDexEntry(n)) names.add(n);
            }
            Collections.sort(names);
            DexIndex ix = new DexIndex(true, withStrings);
            for (String n : names) {
                byte[] data = readEntry(zf, n);
                if (data == null) continue;
                try {
                    new DexParser(data).withStrings(withStrings).parse(ix);
                } catch (Throwable t) {
                    
                }
            }
            return ix.finish();
        } catch (Throwable t) {
            return null;
        } finally {
            if (zf != null) try { zf.close(); } catch (IOException ignored) {}
        }
    }

    private static boolean isDexEntry(String n) {
        if (!n.startsWith("classes") || !n.endsWith(".dex")) return false;
        
        String mid = n.substring(7, n.length() - 4);
        if (mid.isEmpty()) return true;
        for (int i = 0; i < mid.length(); i++) if (!Character.isDigit(mid.charAt(i))) return false;
        return true;
    }

    private static byte[] readEntry(ZipFile zf, String name) {
        InputStream in = null;
        try {
            ZipEntry e = zf.getEntry(name);
            if (e == null) return null;
            long size = e.getSize();
            if (size <= 0 || size > 64L * 1024 * 1024) return null;
            in = new BufferedInputStream(zf.getInputStream(e), 64 * 1024);
            byte[] buf = new byte[(int) size];
            int off = 0;
            while (off < buf.length) {
                int r = in.read(buf, off, buf.length - off);
                if (r < 0) break;
                off += r;
            }
            return off == buf.length ? buf : null;
        } catch (Throwable t) {
            return null;
        } finally {
            if (in != null) try { in.close(); } catch (IOException ignored) {}
        }
    }

    private static String hostApkPath(Context ctx) {
        try {
            
            ApplicationInfo ai = ctx.getPackageManager()
                    .getApplicationInfo(ctx.getPackageName(), 0);
            return ai.sourceDir;
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    private static String cacheKey(Context ctx, String apkPath) {
        int ver = 0;
        try {
            ver = ctx.getPackageManager()
                    .getPackageInfo(ctx.getPackageName(), 0).versionCode;
        } catch (Throwable ignored) {}
        File f = new File(apkPath);
        return ver + "_" + f.length() + "_" + f.lastModified();
    }

    private static File cacheDir(Context ctx) {
        File d = new File(ctx.getFilesDir(), CACHE_DIR);
        if (!d.isDirectory()) d.mkdirs();
        return d;
    }

    private static void writeCache(File f, DexIndex ix) {
        File tmp = new File(f.getAbsolutePath() + ".tmp");
        DataOutputStream out = null;
        try {
            out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(tmp), 64 * 1024));
            ix.writeTo(out);
            out.flush();
            out.close();
            out = null;
            if (!tmp.renameTo(f)) { tmp.delete(); }
        } catch (Throwable t) {
            tmp.delete();
        } finally {
            if (out != null) try { out.close(); } catch (IOException ignored) {}
        }
    }

    public static String lastError() { return sLastError; }

    public static boolean lastHitCache() { return sLastHitCache; }

    public static void clearCache(Context ctx) {
        File[] fs = cacheDir(ctx.getApplicationContext()).listFiles();
        if (fs == null) return;
        for (File f : fs) if (f.getName().startsWith("idx_")) f.delete();
    }
}
