package com.cathelperawa.owo.app.pan;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class PanDl {

    private PanDl() {}

    private static final List<PanTask> TASKS = new ArrayList<PanTask>();
    
    private static int running = 0;
    private static Context app;
    private static File storeFile;
    private static boolean inited = false;

    public static synchronized void init(Context c) {
        if (inited || c == null) return;
        inited = true;
        app = c.getApplicationContext();
        storeFile = new File(app.getFilesDir(), "pan_tasks.json");
        List<PanTask> old = PanTask.loadAll(storeFile, app);
        TASKS.clear();
        TASKS.addAll(old);
        
        for (PanTask t : TASKS) {
            if (t.state == PanTask.RUN || t.state == PanTask.WAIT) {
                t.state = PanTask.FAIL;
                t.err = "上次没下完，点一下可续传";
            }
        }
        save();
    }

    public static synchronized List<PanTask> tasks() {
        return Collections.unmodifiableList(new ArrayList<PanTask>(TASKS));
    }

    private static synchronized void save() {
        if (storeFile != null) PanTask.saveAll(storeFile, TASKS, app);
    }

    public static void touch() {
        save();
    }

    

    public static PanTask enqueue(Context c, PanProvider.Link link, String displayName) {
        init(c);
        PanTask t = new PanTask();
        t.id = "t" + System.currentTimeMillis();
        t.url = link.url;
        t.name = PanDownloader.safeName(displayName == null ? link.filename : displayName, "pan_file");
        t.ua = link.ua;
        t.cookie = link.cookie;
        t.referer = link.referer;
        t.total = link.size;
        t.done = 0;
        t.mime = guessMime(t.name);
        File dir = new File(app.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "dl");
        if (!dir.exists()) dir.mkdirs();
        t.partPath = new File(dir, t.id + ".part").getAbsolutePath();
        synchronized (PanDl.class) {
            TASKS.add(0, t);
        }
        save();
        pump();
        PanDlService.ensure(app);
        return t;
    }

    private static synchronized void pump() {
        int max = app == null ? 3 : PanPrefs.maxConcurrent(app);
        for (PanTask t : TASKS) {
            if (running >= max) break;
            if (t.state != PanTask.WAIT || t.cancel) continue;
            running++;
            start(t);
        }
        if (running > 0) PanDlService.ensure(app);
    }

    private static synchronized void done() {
        if (running > 0) running--;
        pump();
    }

    public static void resume(Context c, PanTask t) {
        init(c);
        if (t.running()) return;
        if (t.state == PanTask.DONE) return;
        t.err = "";
        t.cancel = false;
        t.state = PanTask.WAIT;
        save();
        pump();
        PanDlService.ensure(app);
    }

    public static void cancel(PanTask t) {
        if (t == null) return;
        t.cancel = true;
        t.state = PanTask.CANCELED;
        t.err = "已取消";
        save();

        new Thread(new Runnable() {
            @Override public void run() {
                try {
                    Thread.sleep(1500);
                } catch (Throwable ignore) {
                }
            }
        }).start();
    }

    public static void remove(PanTask t) {
        remove(null, t, false);
    }

    public static boolean remove(Context c, PanTask t, boolean withFile) {
        if (t == null) return false;
        boolean fileGone = true;
        if (withFile) fileGone = deleteSource(t);
        cancel(t);
        synchronized (PanDl.class) {
            TASKS.remove(t);
        }
        try {
            if (t.partPath != null) new File(t.partPath).delete();
        } catch (Throwable ignore) {
        }
        save();
        return fileGone;
    }

    public static boolean deleteSource(PanTask t) {
        if (t == null) return true;
        String u = t.publicUri;
        
        if (u != null && u.length() > 0) {
            try {
                if (u.startsWith("content://")) {
                    int n = app.getContentResolver().delete(Uri.parse(u), null, null);
                    if (n > 0) return true;
                } else {
                    File f = new File(u);
                    if (!f.exists() || f.delete()) return true;
                }
            } catch (Throwable ignore) {
            }
            
            try {
                File dir = new File(Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOWNLOADS), PanPrefs.subDir(app));
                File f = new File(dir, t.name);
                if (f.exists() && !f.delete()) return false;
            } catch (Throwable ignore) {
            }
        }
        return true;
    }

    private static void start(final PanTask t) {
        t.cancel = false;
        save();
        com.cathelperawa.owo.app.core.Bg.run(new Runnable() {
            @Override public void run() {
                try {
                    runTask(t);
                } catch (Throwable e) {
                    if (!t.cancel) {
                        t.state = PanTask.FAIL;
                        t.err = e.getMessage() == null ? e.toString() : e.getMessage();
                    }
                    save();
                } finally {
                    done();
                }
            }
        }, "pan-dl");
    }

    

    private static void runTask(PanTask t) throws Exception {
        t.state = PanTask.RUN;
        save();

        long total = t.total;
        boolean rangeOk = true;

        if (total <= 0) {
            HttpURLConnection c = open(t, 0, 0);
            int code = c.getResponseCode();
            if (code == 206) {
                total = parseTotal(c.getHeaderField("Content-Range"));
            } else if (code == 200) {
                String len = c.getHeaderField("Content-Length");
                try {
                    total = Long.parseLong(len == null ? "0" : len);
                } catch (Throwable ignore) {
                }
                rangeOk = false;
            } else {
                throw new Exception("直链不可用（HTTP " + code + "），可能过期了，重新取一次");
            }
            c.disconnect();
            t.total = total;
        }

        File part = new File(t.partPath);
        long already = part.exists() ? part.length() : 0;
        if (total > 0 && already > total) {
            part.delete();
            already = 0;
        }
        t.done = already;

        RandomAccessFile raf = new RandomAccessFile(part, "rw");
        FileChannel ch = raf.getChannel();
        if (total > 0) raf.setLength(total);

        int threads = Math.max(1, Math.min(256, PanPrefs.threads(app)));
        if (!rangeOk || total <= 0) threads = 1;
        long remain = total > 0 ? total - already : 0;
        if (remain <= 0 && total > 0) {
            raf.close();
            finish(t, part);
            return;
        }
        if (threads > 1 && remain / threads < 256 * 1024L) {
            threads = Math.max(1, (int) (remain / (256 * 1024L)));
        }

        int limitKb = PanPrefs.speedLimitKb(app);
        t.segLimit = limitKb <= 0 ? 0 : (limitKb * 1024L / Math.max(1, threads));

        t.lastDone = already;
        t.lastAt = System.currentTimeMillis();

        final List<Thread> workers = new ArrayList<Thread>();
        long segLen = threads > 0 ? remain / threads : remain;
        for (int i = 0; i < threads; i++) {
            final long start = already + segLen * i;
            final long end = (i == threads - 1) ? total - 1 : (start + segLen - 1);
            if (total <= 0) break;
            Thread w = new Thread(new Runnable() {
                @Override public void run() {
                    fetchSegment(t, ch, start, end);
                }
            }, "pan-seg-" + i);
            w.setDaemon(true);
            workers.add(w);
            w.start();
        }
        if (total <= 0) {
            
            fetchStream(t, ch);
        }

        for (Thread w : workers) {
            try {
                w.join();
            } catch (InterruptedException ignore) {
            }
        }
        try {
            ch.close();
            raf.close();
        } catch (Throwable ignore) {
        }

        if (t.cancel) {
            t.state = PanTask.CANCELED;
            part.delete();
            save();
            return;
        }
        if (total > 0 && t.done < total) {
            t.state = PanTask.FAIL;
            t.err = "只下到 " + t.percent() + "%，点一下续传";
            save();
            return;
        }
        finish(t, part);
    }

    private static void fetchSegment(PanTask t, FileChannel ch, long start, long end) {
        HttpURLConnection c = null;
        try {
            c = open(t, start, end);
            int code = c.getResponseCode();
            if (code != 206 && code != 200) {
                throw new Exception("HTTP " + code);
            }
            InputStream in = c.getInputStream();
            byte[] buf = new byte[64 * 1024];
            long pos = start;
            long wrote = 0;
            long t0 = System.currentTimeMillis();
            int n;
            while ((n = in.read(buf)) > 0) {
                if (t.cancel) return;
                ch.write(ByteBuffer.wrap(buf, 0, n), pos);
                pos += n;
                wrote += n;
                synchronized (t) {
                    t.done += n;
                }
                
                long limit = t.segLimit;
                if (limit > 0) {
                    long expect = wrote * 1000L / limit;
                    long actual = System.currentTimeMillis() - t0;
                    if (expect > actual) {
                        try {
                            Thread.sleep(Math.min(200L, expect - actual));
                        } catch (InterruptedException ignore) {
                            return;
                        }
                    }
                }
            }
            try {
                in.close();
            } catch (Throwable ignore) {
            }
        } catch (Throwable e) {
            if (!t.cancel) t.err = String.valueOf(e.getMessage());
        } finally {
            if (c != null) {
                try {
                    c.disconnect();
                } catch (Throwable ignore) {
                }
            }
        }
    }

    private static void fetchStream(PanTask t, FileChannel ch) {
        HttpURLConnection c = null;
        try {
            c = open(t, 0, 0);
            InputStream in = c.getInputStream();
            byte[] buf = new byte[64 * 1024];
            long pos = 0;
            int n;
            while ((n = in.read(buf)) > 0) {
                if (t.cancel) return;
                ch.write(ByteBuffer.wrap(buf, 0, n), pos);
                pos += n;
                t.done = pos;
            }
            t.total = pos;
        } catch (Throwable ignore) {
        } finally {
            if (c != null) c.disconnect();
        }
    }

    private static HttpURLConnection open(PanTask t, long start, long end) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(t.url).openConnection();
        c.setConnectTimeout(20000);
        c.setReadTimeout(30000);
        c.setInstanceFollowRedirects(true);
        if (t.ua != null && t.ua.length() > 0) c.setRequestProperty("User-Agent", t.ua);
        if (t.referer != null && t.referer.length() > 0) c.setRequestProperty("Referer", t.referer);
        if (t.cookie != null && t.cookie.length() > 0) c.setRequestProperty("Cookie", t.cookie);
        c.setRequestProperty("Accept-Encoding", "identity");
        if (end > start || (start == 0 && end == 0)) {
            c.setRequestProperty("Range", "bytes=" + start + "-" + end);
        }
        return c;
    }

    private static long parseTotal(String contentRange) {
        if (contentRange == null) return 0;
        int slash = contentRange.lastIndexOf('/');
        if (slash < 0) return 0;
        try {
            return Long.parseLong(contentRange.substring(slash + 1).trim());
        } catch (Throwable t) {
            return 0;
        }
    }

    

    private static void finish(PanTask t, File part) {
        try {
            String sub = PanPrefs.subDir(app);
            String uri = publishToPublic(part, t.name, t.mime, sub);
            t.publicUri = uri;
            t.state = PanTask.DONE;
            t.err = "";
            t.retry = 0;
            t.done = t.total > 0 ? t.total : t.done;
            if (uri != null) part.delete();
        } catch (Throwable e) {
            t.state = PanTask.FAIL;
            t.err = "已下完但搬到 Download 失败：" + e.getMessage();
        }
        save();
    }

    private static String publishToPublic(File part, String name, String mime, String sub) throws Exception {
        if (Build.VERSION.SDK_INT >= 29) {
            ContentValues v = new ContentValues();
            v.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
            v.put(MediaStore.MediaColumns.MIME_TYPE, mime.length() == 0 ? "application/octet-stream" : mime);
            v.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/" + sub);
            v.put(MediaStore.MediaColumns.IS_PENDING, 1);
            Uri uri = app.getContentResolver().insert(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI, v);
            if (uri == null) throw new Exception("MediaStore 拒绝了写入");
            InputStream in = new FileInputStream(part);
            OutputStream out = app.getContentResolver().openOutputStream(uri);
            if (out == null) {
                in.close();
                throw new Exception("打不开目标文件");
            }
            byte[] buf = new byte[256 * 1024];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            in.close();
            out.flush();
            out.close();
            ContentValues done = new ContentValues();
            done.put(MediaStore.MediaColumns.IS_PENDING, 0);
            app.getContentResolver().update(uri, done, null, null);
            return uri.toString();
        }
        
        File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), sub);
        if (!dir.exists()) dir.mkdirs();
        File dst = new File(dir, name);
        FileInputStream in = new FileInputStream(part);
        FileOutputStream out = new FileOutputStream(dst);
        byte[] buf = new byte[256 * 1024];
        int n;
        while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        in.close();
        out.close();
        return dst.getAbsolutePath();
    }

    private static String guessMime(String name) {
        String n = name == null ? "" : name.toLowerCase();
        if (n.endsWith(".mp4")) return "video/mp4";
        if (n.endsWith(".mkv")) return "video/x-matroska";
        if (n.endsWith(".mp3")) return "audio/mpeg";
        if (n.endsWith(".flac")) return "audio/flac";
        if (n.endsWith(".jpg") || n.endsWith(".jpeg")) return "image/jpeg";
        if (n.endsWith(".png")) return "image/png";
        if (n.endsWith(".gif")) return "image/gif";
        if (n.endsWith(".pdf")) return "application/pdf";
        if (n.endsWith(".zip")) return "application/zip";
        if (n.endsWith(".rar")) return "application/x-rar-compressed";
        if (n.endsWith(".7z")) return "application/x-7z-compressed";
        if (n.endsWith(".apk")) return "application/vnd.android.package-archive";
        if (n.endsWith(".txt")) return "text/plain";
        return "application/octet-stream";
    }

    public static int activeCount() {
        int n = 0;
        for (PanTask t : tasks()) {
            if (t.state == PanTask.WAIT || t.state == PanTask.RUN) n++;
        }
        return n;
    }

    public static void sampleSpeeds() {
        long now = System.currentTimeMillis();
        List<PanTask> list = tasks();
        for (PanTask t : list) {
            if (t.state != PanTask.RUN) {
                t.speed = 0;
                t.lastDone = t.done;
                t.lastAt = now;
                continue;
            }
            long dt = now - t.lastAt;
            if (dt >= 800) {
                long delta = t.done - t.lastDone;
                t.speed = delta > 0 ? (long) (delta * 1000.0 / dt) : 0;
                t.lastDone = t.done;
                t.lastAt = now;
            }
        }
    }
}
