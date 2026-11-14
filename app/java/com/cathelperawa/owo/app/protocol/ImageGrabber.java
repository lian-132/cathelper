package com.cathelperawa.owo.app.protocol;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.cathelperawa.owo.app.bridge.QQEnv;
import com.cathelperawa.owo.app.core.HookLog;
import com.cathelperawa.owo.app.util.Refl;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.List;

public final class ImageGrabber {

    private ImageGrabber() {}

    private static final String DIR = "catimg";

    public static File dir() {
        File d = new File(QQEnv.appContext().getFilesDir(), DIR);
        if (!d.exists()) d.mkdirs();
        return d;
    }

    public static final class Source {
        public String kind = "none";

        public String path;

        public String url;

        public String element = "";

        public String key = "";

        public String md5;

        public Object raw;

        @Override public String toString() {
            return "Source{kind=" + kind + ", el=" + element
                    + ", path=" + path + ", url=" + url + ", key=" + key + "}";
        }
    }

    public static Source fromRecord(Object record) {
        Source s = new Source();
        if (record == null) return s;
        Object els = Refl.field(record, "elements");
        if (els instanceof List) {
            for (Object el : (List<?>) els) {
                Source one = fromElement(el);
                if (one != null && !"none".equals(one.kind)) return one;
            }
        }

        Source fromRec = fromRecords(record, 0);
        if (fromRec != null && !"none".equals(fromRec.kind)) {
            fromRec.element = "records/" + fromRec.element;
            return fromRec;
        }

        Source fromCache = fromReplyCache(record);
        if (fromCache != null && !"none".equals(fromCache.kind)) {
            fromCache.element = "msgcache/" + fromCache.element;
            return fromCache;
        }
        return s;
    }

    private static Source fromReplyCache(Object record) {
        try {
            Object els = Refl.field(record, "elements");
            if (!(els instanceof List)) return null;
            for (Object el : (List<?>) els) {
                if (el == null) continue;
                Object rep = Refl.field(el, "replyElement");
                if (rep == null) continue;
                long id = Refl.fieldLong(rep, "sourceMsgIdInRecords", 0);
                Object orig = MsgCache.get(id);
                if (orig == null) continue;
                Object oels = Refl.field(orig, "elements");
                if (!(oels instanceof List)) continue;
                for (Object oel : (List<?>) oels) {
                    Source one = fromElement(oel, 1);
                    if (one != null && !"none".equals(one.kind)) return one;
                }
            }
        } catch (Throwable ignore) {
        }
        return null;
    }

    private static Source fromRecords(Object record, int depth) {
        if (record == null || depth > 2) return null;
        Object recs = Refl.field(record, "records");
        if (!(recs instanceof List)) return null;
        for (Object r : (List<?>) recs) {
            if (r == null) continue;
            Object els = Refl.field(r, "elements");
            if (els instanceof List) {
                for (Object el : (List<?>) els) {
                    Source one = fromElement(el, 1);
                    if (one != null && !"none".equals(one.kind)) return one;
                }
            }
            Source deep = fromRecords(r, depth + 1);
            if (deep != null && !"none".equals(deep.kind)) return deep;
        }
        return null;
    }

    public static Source fromElement(Object el) {
        return fromElement(el, 0);
    }

    private static Source fromElement(Object el, int depth) {
        if (el == null) return null;

        Object mf = Refl.field(el, "marketFaceElement");
        if (mf != null) {
            Source s = new Source();
            s.element = "marketFaceElement";
            s.raw = mf;

            for (String fn : new String[]{"url", "staticFaceUrl", "dynamicFaceUrl", "bigUrl"}) {
                Object u = Refl.field(mf, fn);
                if (u instanceof String && ((String) u).length() > 0) {
                    s.url = fixUrl((String) u);
                    break;
                }
            }
            for (String fn : new String[]{"faceName", "faceId", "emojiId", "name"}) {
                Object k = Refl.field(mf, fn);
                if (k != null) { s.key = String.valueOf(k); break; }
            }
            if (s.url != null) s.kind = "url";
            return s;
        }

        Object pic = Refl.field(el, "picElement");
        if (pic != null) {
            Source s = new Source();
            s.element = "picElement";
            s.raw = pic;
            s.path = firstExisting(pic, new String[]{"filePath", "localPath", "sourcePath", "path"});
            String md5 = firstString(pic, new String[]{"md5HexStr", "md5"});
            if (md5 != null && md5.length() >= 8) s.md5 = md5;
            s.url = fixUrl(firstString(pic, new String[]{"originImageUrl", "bigUrl", "url"}));
            for (String fn : new String[]{"md5HexStr", "md5", "fileName", "picSubType"}) {
                Object k = Refl.field(pic, fn);
                if (k != null) { s.key = s.key + "/" + fn + "=" + k; }
            }
            if (s.path != null) s.kind = "local";
            else if (s.url != null) s.kind = "url";
            return s;
        }

        for (String fn : new String[]{"richMediaElement", "videoElement", "arkElement"}) {
            Object rm = Refl.field(el, fn);
            if (rm == null) continue;
            String u = fixUrl(firstString(rm, new String[]{"url", "coverUrl", "thumbUrl"}));
            if (u != null) {
                Source s = new Source();
                s.element = fn;
                s.raw = rm;
                s.url = u;
                s.kind = "url";
                return s;
            }
        }

        if (depth < 3) {
            Object rep = Refl.field(el, "replyElement");
            if (rep != null) {
                Object elems = Refl.field(rep, "sourceMsgTextElems");
                if (elems instanceof List) {
                    for (int i = 0; i < ((List<?>) elems).size(); i++) {
                        Source one = fromElement(((List<?>) elems).get(i), depth + 1);
                        if (one != null && !"none".equals(one.kind)) {
                            one.element = "reply:" + one.element;
                            return one;
                        }
                    }
                }
            }
        }
        return null;
    }

    public static byte[] bytes(Source s) {
        if (s == null) return null;
        byte[] best = null;
        String from = "";

        if (s.path != null) {
            byte[] b = readFile(s.path);
            if (b != null && b.length > 0) {
                best = b;
                from = "元素路径";
            }
        }

        if (s.md5 != null) {
            try {
                java.io.File f = findInCache(s.md5);
                if (f != null && f.exists()) {
                    byte[] b = readFile(f.getAbsolutePath());
                    if (b != null && b.length > 0) {
                        HookLog.i("图片从本地缓存命中 " + f.getName());
                        if (best == null) from = "缓存";
                        best = better(best, b);
                    }
                } else {
                    com.cathelperawa.owo.app.ai.AiDebug.log("  · 图片: md5 缓存未命中 " + s.md5);
                }
            } catch (Throwable t) {
                com.cathelperawa.owo.app.ai.AiDebug.log("  · 图片: 缓存查找异常 " + t);
            }
        } else {
            com.cathelperawa.owo.app.ai.AiDebug.log("  · 图片: 没有 md5，跳过缓存");
        }

        boolean needNet = best == null || tooSmall(best);
        if (needNet && s.url != null) {
            String[] cands = RKeyHolder.candidates(s.url);
            if (cands.length == 0) cands = new String[]{s.url};
            int okCount = 0;
            for (int i = 0; i < cands.length; i++) {
                byte[] b = http(cands[i]);
                if (b == null || b.length == 0) {
                    if (i == 0) HookLog.w("图片下载候选#" + i + " 失败 " + head(cands[i]));
                    continue;
                }
                okCount++;
                if (i > 0) HookLog.i("图片用候选#" + i + " 下载成功");
                if (best == null || b.length > best.length) {
                    from = "网络候选#" + i;
                }
                best = better(best, b);
                if (!tooSmall(best)) break;
            }

            if (okCount == 0) RKeyHolder.forget(RKeyHolder.fileIdOf(s.url));
        }

        if (best != null) {
            com.cathelperawa.owo.app.ai.AiDebug.log("  · 图片: 取到 " + (best.length / 1024)
                    + "KB（来源 " + from + "，" + describeSize(best) + "）");
        }
        return best;
    }

    private static boolean tooSmall(byte[] b) {
        if (b == null) return true;
        try {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(b, 0, b.length, o);
            int big = Math.max(o.outWidth, o.outHeight);
            if (big <= 0) return b.length < 60000;
            return big < 480;
        } catch (Throwable t) {
            return b.length < 60000;
        }
    }

    private static String describeSize(byte[] b) {
        try {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(b, 0, b.length, o);
            return o.outWidth + "x" + o.outHeight;
        } catch (Throwable t) {
            return "尺寸未知";
        }
    }

    private static byte[] better(byte[] a, byte[] b) {
        if (b == null || b.length == 0) return a;
        if (a == null) return b;
        return b.length > a.length ? b : a;
    }

    public static Bitmap bitmap(Source s) {
        byte[] raw = bytes(s);
        if (raw == null) return null;
        try {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(raw, 0, raw.length, o);
            int scale = 1;
            while (o.outWidth / scale > 1600 || o.outHeight / scale > 1600) scale *= 2;
            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize = scale;
            return BitmapFactory.decodeByteArray(raw, 0, raw.length, o2);
        } catch (Throwable t) {
            HookLog.e("bitmap 解码失败", t);
            return null;
        }
    }

    public static File save(Source s, String name) {
        byte[] raw = bytes(s);
        if (raw == null || raw.length == 0) return null;
        try {
            String fn = (name == null || name.length() == 0)
                    ? System.currentTimeMillis() + "" : name;
            fn = fn.replaceAll("[^0-9A-Za-z_\\-]", "_");
            File f = new File(dir(), fn + ".png");
            try (FileOutputStream out = new FileOutputStream(f)) {
                out.write(raw);
            }
            return f;
        } catch (Throwable t) {
            HookLog.e("保存图片失败", t);
            return null;
        }
    }

    public static File findInCache(String md5) {
        if (md5 == null || md5.length() < 8) return null;
        String upper = md5.toUpperCase();
        String pre = upper.substring(0, 4);
        try {
            android.content.Context ctx = QQEnv.appContext();
            java.io.File app = ctx.getFilesDir().getParentFile();

            java.io.File fast = new java.io.File(app, "shared_prefs/.configfs/fast_cache");
            java.io.File rich = new java.io.File(ctx.getFilesDir(), "nt_qq_rich_media");
            java.io.File ext = ctx.getExternalFilesDir(null);

            java.util.ArrayList<java.io.File> hits = new java.util.ArrayList<java.io.File>();
            collectMd5(fast, pre, upper, 0, hits);
            collectMd5(rich, pre, upper, 0, hits);
            if (ext != null) collectMd5(new java.io.File(ext, "Tencent"), pre, upper, 0, hits);
            java.io.File hit = biggest(hits);
            if (hit != null) {
                HookLog.i("图片缓存命中 " + hit.getAbsolutePath() + " (" + hit.length() / 1024 + "KB)");
                return hit;
            }

            java.io.File base = ctx.getFilesDir();
            String[] subs = new String[]{
                    "photo", "Photo", "pic", "images", "cache", "chatpic", "flashpic", ""
            };
            for (String sub : subs) {
                java.io.File d = sub.isEmpty() ? base : new java.io.File(base, sub);
                java.io.File f2 = deepFind(d, md5, 0);
                if (f2 != null) return f2;
            }
        } catch (Throwable t) {
            HookLog.w("findInCache 失败 " + t);
        }
        return null;
    }

    private static void collectMd5(java.io.File d, String prefix, String md5Upper,
                                   int depth, java.util.List<java.io.File> out) {
        if (d == null || depth > 8 || !d.isDirectory()) return;
        java.io.File[] ls = d.listFiles();
        if (ls == null) return;
        for (java.io.File f : ls) {
            if (!f.isDirectory()) continue;
            if (!f.getName().equalsIgnoreCase(prefix)) continue;
            java.io.File[] inner = f.listFiles();
            if (inner == null) continue;
            for (java.io.File x : inner) {

                if (x.isFile() && x.getName().toUpperCase().startsWith(md5Upper)) out.add(x);
            }
        }
        for (java.io.File f : ls) {
            if (!f.isDirectory()) continue;
            if (f.getName().equalsIgnoreCase(prefix)) continue;
            collectMd5(f, prefix, md5Upper, depth + 1, out);
        }
    }

    private static java.io.File biggest(java.util.List<java.io.File> list) {
        if (list == null || list.isEmpty()) return null;
        java.io.File best = null;
        for (java.io.File f : list) {
            if (best == null || f.length() > best.length()) best = f;
        }
        return best;
    }

    private static File deepFind(File d, String key, int depth) {
        if (d == null || depth > 4 || !d.isDirectory()) return null;
        File[] fs = d.listFiles();
        if (fs == null) return null;
        for (File f : fs) {
            if (f.isFile() && f.getName().contains(key)) return f;
        }
        for (File f : fs) {
            if (f.isDirectory()) {
                File hit = deepFind(f, key, depth + 1);
                if (hit != null) return hit;
            }
        }
        return null;
    }

    public static File findRecentImage(long withinMs) {
        try {
            File base = QQEnv.appContext().getFilesDir();
            File best = null;
            long bestT = 0;
            long now = System.currentTimeMillis();
            for (String sub : new String[]{"photo", "Photo", "pic", "chatpic", "flashpic", ""}) {
                File d = sub.isEmpty() ? base : new File(base, sub);
                File hit = recentIn(d, now - withinMs, 0);
                if (hit != null) {
                    long t = hit.lastModified();
                    if (t > bestT) { bestT = t; best = hit; }
                }
            }
            return best;
        } catch (Throwable t) {
            return null;
        }
    }

    private static File recentIn(File d, long since, int depth) {
        if (d == null || depth > 3 || !d.isDirectory()) return null;
        File[] fs = d.listFiles();
        if (fs == null) return null;
        File best = null;
        long bestT = since;
        for (File f : fs) {
            if (f.isFile() && f.lastModified() > bestT && looksImage(f.getName())) {
                bestT = f.lastModified();
                best = f;
            }
        }
        for (File f : fs) {
            if (f.isDirectory()) {
                File hit = recentIn(f, bestT, depth + 1);
                if (hit != null && hit.lastModified() > bestT) {
                    bestT = hit.lastModified();
                    best = hit;
                }
            }
        }
        return best;
    }

    private static boolean looksImage(String n) {
        String s = n.toLowerCase();
        return s.endsWith(".png") || s.endsWith(".jpg") || s.endsWith(".jpeg")
                || s.endsWith(".webp") || s.endsWith(".gif")
                || (s.length() > 16 && !s.contains("."));
    }

    public static byte[] readFile(String p) {
        try {
            File f = new File(p);
            if (!f.exists() || !f.isFile()) return null;
            if (f.length() > 32L * 1024 * 1024) return null;
            byte[] out = new byte[(int) f.length()];
            int off = 0;
            try (InputStream in = new BufferedInputStream(new java.io.FileInputStream(f))) {
                while (off < out.length) {
                    int n = in.read(out, off, out.length - off);
                    if (n <= 0) break;
                    off += n;
                }
            }
            return out;
        } catch (Throwable t) {
            return null;
        }
    }

    public static byte[] http(String url) {
        try {
            HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
            c.setConnectTimeout(8000);
            c.setReadTimeout(12000);
            c.setRequestProperty("User-Agent",
                    "Mozilla/5.0 (Linux; Android 16) AppleWebKit/537.36");
            c.setInstanceFollowRedirects(true);
            int code = c.getResponseCode();
            if (code != 200) {
                HookLog.w("图片 HTTP " + code + " " + url);
                return null;
            }
            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            int total = 0;
            try (InputStream in = new BufferedInputStream(c.getInputStream())) {
                while ((n = in.read(buf)) > 0) {
                    bos.write(buf, 0, n);
                    total += n;
                    if (total > 24 * 1024 * 1024) break;
                }
            }
            return bos.toByteArray();
        } catch (Throwable t) {
            HookLog.w("图片下载失败 " + t);
            return null;
        }
    }

    private static String head(String u) {
        if (u == null) return "";
        return u.length() <= 120 ? u : u.substring(0, 120) + "…";
    }

    public static String fixUrl(String u) {
        if (u == null || u.length() == 0) return null;
        if (u.startsWith("http://") || u.startsWith("https://")) return u;
        if (u.startsWith("//")) return "https:" + u;
        if (u.startsWith("/")) return "https://multimedia.nt.qq.com.cn" + u;
        return null;
    }

    private static String firstString(Object o, String[] names) {
        for (String n : names) {
            Object v = Refl.field(o, n);
            if (v instanceof String && ((String) v).length() > 0) return (String) v;
        }
        return null;
    }

    private static String firstExisting(Object o, String[] names) {
        for (String n : names) {
            Object v = Refl.field(o, n);
            if (v instanceof String && ((String) v).length() > 0) {
                String p = (String) v;
                if (new File(p).exists()) return p;
            }
        }
        return null;
    }

    public static String md5(byte[] d) {
        try {
            MessageDigest m = MessageDigest.getInstance("MD5");
            byte[] h = m.digest(d);
            StringBuilder sb = new StringBuilder();
            for (byte b : h) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Throwable t) {
            return null;
        }
    }
}
