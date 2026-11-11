package com.cathelperawa.owo.app.bili;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.LruCache;
import android.widget.ImageView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class BiliImage {

    private BiliImage() {}

    private static LruCache<String, Bitmap> mem;
    private static ExecutorService pool;
    private static final String DIR = "bili_img";
    private static final int MAX_FILES = 400;

    private static synchronized void init(Context c) {
        if (pool != null) return;
        int maxKb = (int) (Runtime.getRuntime().maxMemory() / 1024 / 8);
        if (maxKb > 32 * 1024) maxKb = 32 * 1024;
        mem = new LruCache<String, Bitmap>(maxKb) {
            @Override protected int sizeOf(String k, Bitmap v) {
                return v == null ? 0 : v.getByteCount() / 1024;
            }
        };
        pool = Executors.newFixedThreadPool(3);
        
        try {
            File dir = new File(c.getFilesDir(), DIR);
            File[] fs = dir.listFiles();
            if (fs != null && fs.length > MAX_FILES) {
                Arrays.sort(fs, new Comparator<File>() {
                    @Override public int compare(File a, File b) {
                        return Long.compare(a.lastModified(), b.lastModified());
                    }
                });
                for (int i = 0; i < fs.length - MAX_FILES; i++) fs[i].delete();
            }
        } catch (Throwable ignore) {
        }
    }

    public static void attach(final ImageView iv, final String url, final int radiusDp) {
        if (iv == null) return;
        iv.setTag(url);
        iv.setImageBitmap(null);
        if (url == null || url.length() == 0) return;
        final Context c = iv.getContext().getApplicationContext();
        init(c);

        Bitmap cached = mem.get(url);
        if (cached != null) {
            iv.setImageBitmap(rounded(cached, radiusDp, c));
            return;
        }
        pool.execute(new Runnable() {
            @Override public void run() {
                final Bitmap bm = load(c, url, Math.max(iv.getWidth(), 320));
                if (bm == null) return;
                mem.put(url, bm);
                final Bitmap shown = rounded(bm, radiusDp, c);
                iv.post(new Runnable() {
                    @Override public void run() {
                        if (url.equals(iv.getTag())) iv.setImageBitmap(shown);
                    }
                });
            }
        });
    }

    public static void attachAvatar(ImageView iv, String url) {
        attach(iv, url, 999);
    }

    private static Bitmap load(Context c, String url, int wantWidth) {
        File f = fileOf(c, url);
        Bitmap bm = decode(f, wantWidth);
        if (bm != null) {
            
            f.setLastModified(System.currentTimeMillis());
            return bm;
        }
        if (!download(url, f)) return null;
        return decode(f, wantWidth);
    }

    private static Bitmap decode(File f, int wantWidth) {
        if (f == null || !f.exists() || f.length() == 0) return null;
        try {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(f.getAbsolutePath(), o);
            int sample = 1;
            while (o.outWidth / (sample * 2) >= wantWidth) sample *= 2;
            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize = sample;
            o2.inPreferredConfig = Bitmap.Config.RGB_565;   
            return BitmapFactory.decodeFile(f.getAbsolutePath(), o2);
        } catch (Throwable t) {
            return null;
        }
    }

    private static boolean download(String url, File out) {
        HttpURLConnection conn = null;
        InputStream in = null;
        FileOutputStream fo = null;
        File tmp = new File(out.getAbsolutePath() + ".tmp");
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(15000);
            conn.setRequestProperty("User-Agent", BiliApi.UA);
            conn.setRequestProperty("Referer", BiliApi.REFERER);
            if (conn.getResponseCode() >= 400) return false;
            in = conn.getInputStream();
            if (in == null) return false;
            fo = new FileOutputStream(tmp);
            byte[] buf = new byte[32 * 1024];
            int n;
            long total = 0;
            while ((n = in.read(buf)) > 0) {
                total += n;
                if (total > 8 * 1024 * 1024) {          
                    tmp.delete();
                    return false;
                }
                fo.write(buf, 0, n);
            }
            fo.flush();
            fo.close();
            fo = null;
            return tmp.renameTo(out);
        } catch (Throwable t) {
            try { tmp.delete(); } catch (Throwable ignore) { }
            return false;
        } finally {
            try { if (fo != null) fo.close(); } catch (Throwable ignore) { }
            try { if (in != null) in.close(); } catch (Throwable ignore) { }
            if (conn != null) conn.disconnect();
        }
    }

    private static File fileOf(Context c, String url) {
        File dir = new File(c.getFilesDir(), DIR);
        if (!dir.exists()) dir.mkdirs();
        return new File(dir, md5(url) + ".img");
    }

    public static Bitmap rounded(Bitmap src, int radiusDp, Context c) {
        if (src == null) return null;
        int w = src.getWidth(), h = src.getHeight();
        if (w <= 0 || h <= 0) return src;
        try {
            Bitmap out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(out);
            Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
            p.setShader(new BitmapShader(src, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));
            float r = radiusDp >= 999 ? Math.min(w, h) / 2f
                    : radiusDp * c.getResources().getDisplayMetrics().density;
            canvas.drawRoundRect(new RectF(0, 0, w, h), r, r, p);
            return out;
        } catch (Throwable t) {
            return src;
        }
    }

    private static String md5(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] b = md.digest(s.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte x : b) sb.append(String.format("%02x", x));
            return sb.toString();
        } catch (Throwable t) {
            return String.valueOf(s.hashCode());
        }
    }

    public static void clearMemory() {
        try { if (mem != null) mem.evictAll(); } catch (Throwable ignore) { }
    }
}
