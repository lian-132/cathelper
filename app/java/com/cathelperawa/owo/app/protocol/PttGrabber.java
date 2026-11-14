package com.cathelperawa.owo.app.protocol;

import com.cathelperawa.owo.app.ai.AiDebug;
import com.cathelperawa.owo.app.util.Refl;

import java.util.List;

public final class PttGrabber {

    private PttGrabber() {}

    private static final String[] PTT_DIRS = {
            "files/ptt", "files/ptt_dir", "app_ptt", "files", "cache/ptt"
    };

    public static final class Source {
        public String kind = "none";
        public String path;
        public String url;
        public String md5;

        public String fid;

        public Object raw;
        public int seconds;
        public long fileSize;

        public String text = "";

        public boolean canConvert;
        public int translateStatus = -1;

        @Override public String toString() {
            return "PttSource{kind=" + kind + ", path=" + path + ", url=" + url
                    + ", md5=" + md5 + ", sec=" + seconds + ", size=" + fileSize
                    + ", text=" + (text.length() > 20 ? text.substring(0, 20) + "…" : text)
                    + ", canConvert=" + canConvert + ", st=" + translateStatus + "}";
        }
    }

    public static Source fromRecord(Object record) {
        Source s = new Source();
        if (record == null) return s;
        Object els = Refl.field(record, "elements");
        if (els instanceof List) {
            for (Object el : (List<?>) els) {
                if (el == null) continue;
                Source one = fromElement(el);
                if (one != null && !"none".equals(one.kind)) return one;
            }
        }

        Source deep = fromRecords(record, 0);
        if (deep != null && !"none".equals(deep.kind)) return deep;

        Source cached = fromReplyCache(record);
        if (cached != null && !"none".equals(cached.kind)) return cached;
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

                Object abs = Refl.field(rep, "sourceMsgTextElems");
                if (abs instanceof List) {
                    boolean isPic = false;
                    for (Object e2 : (List<?>) abs) {
                        if (e2 != null && Refl.field(e2, "picElem") != null) {
                            isPic = true;
                            break;
                        }
                    }
                    if (isPic) {
                        com.cathelperawa.owo.app.ai.AiDebug.log(
                                "  · 语音: 引用的是图片，跳过语音反查");
                        return null;
                    }
                }
                long id = Refl.fieldLong(rep, "sourceMsgIdInRecords", 0);
                long seq = Refl.fieldLong(rep, "replayMsgSeq", 0);
                if (seq == 0) seq = Refl.fieldLong(rep, "replyMsgSeq", 0);

                String[] idx0 = PttIndex.get(id);
                if (idx0 == null) idx0 = PttIndex.getBySeq(seq);
                if (idx0 == null) {
                    com.cathelperawa.owo.app.ai.AiDebug.log(
                            "  · 语音诊断: PttIndex 里没有 id=" + id + " seq=" + seq);
                }
                if (idx0 != null) {
                    Source one = new Source();
                    one.path = idx0[0].length() > 0 ? idx0[0] : null;
                    one.md5 = idx0[1].length() > 0 ? idx0[1] : null;
                    one.fid = idx0[2].length() > 0 ? idx0[2] : null;
                    one.raw = new Object();
                    one.kind = one.path != null ? "local"
                            : (one.fid != null ? "url" : "none");
                    com.cathelperawa.owo.app.ai.AiDebug.log(
                            "  · 语音: 引用语音从 PttIndex 查到 " + one);
                    return one;
                }

                Object orig = MsgCache.get(id);
                if (orig == null) orig = MsgCache.getBySeq(seq);
                com.cathelperawa.owo.app.ai.AiDebug.log("  · 语音诊断: 引用 msgId=" + id
                        + " seq=" + seq + " cache=" + MsgCache.size() + "/" + MsgCache.seqSize()
                        + " 命中=" + (orig != null));
                if (orig == null) {

                    String[] idx = PttIndex.get(id);
                    if (idx != null) {
                        Source one = new Source();
                        one.path = idx[0].length() > 0 ? idx[0] : null;
                        one.md5 = idx[1].length() > 0 ? idx[1] : null;
                        one.fid = idx[2].length() > 0 ? idx[2] : null;
                        one.raw = new Object();
                        one.kind = one.path != null ? "local"
                                : (one.fid != null ? "url" : "none");
                        com.cathelperawa.owo.app.ai.AiDebug.log(
                                "  · 语音: 引用语音从持久索引查到 " + one);
                        return one;
                    }
                    com.cathelperawa.owo.app.ai.AiDebug.log(
                            "  · 语音诊断: 持久索引也没有 id=" + id + " 共 "
                                    + PttIndex.size() + " 条");
                    continue;
                }
                Object oels = Refl.field(orig, "elements");
                if (!(oels instanceof List)) continue;
                boolean any = false;
                for (Object oel : (List<?>) oels) {
                    if (Refl.field(oel, "pttElement") == null) continue;
                    any = true;
                    Source one = fromElement(oel);
                    if (one != null) {
                        com.cathelperawa.owo.app.ai.AiDebug.log(
                                "  · 语音: 引用语音从 MsgCache 反查到原消息 " + one);
                        return one;
                    }
                }
                if (!any) {
                    com.cathelperawa.owo.app.ai.AiDebug.log(
                            "  · 语音诊断: 命中 msgId=" + id + " 但那不是语音消息，跳过");
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
                    if (Refl.field(el, "pttElement") == null) continue;
                    Source one = fromElement(el);
                    if (one != null) return one;
                }
            }
            Source d = fromRecords(r, depth + 1);
            if (d != null && !"none".equals(d.kind)) return d;
        }
        return null;
    }

    public static boolean isVoice(Object el) {
        return el != null && Refl.field(el, "pttElement") != null;
    }

    public static Source fromElement(Object el) {
        Object ptt = Refl.field(el, "pttElement");
        if (ptt == null) return null;
        Source s = new Source();
        s.raw = ptt;

        s.path = Refl.firstStr(ptt, "filePath", "localPath", "sourcePath", "path", "pttPath");
        s.url = firstString(ptt, new String[]{
                "url", "downloadUrl", "pttUrl", "originUrl", "srcUrl"});
        if (s.url != null) s.url = ImageGrabber.fixUrl(s.url);
        s.md5 = firstString(ptt, new String[]{"md5HexStr", "md5", "fileMd5"});
        s.fid = firstString(ptt, new String[]{"fileUuid", "fileSubId"});
        s.seconds = Refl.fieldInt(ptt, "duration", 0);
        if (s.seconds <= 0) s.seconds = Refl.fieldInt(ptt, "pttTime", 0);
        s.fileSize = Refl.fieldLong(ptt, "fileSize", 0);
        if (s.fileSize <= 0) s.fileSize = Refl.fieldLong(ptt, "fileSizeH", 0);

        try {
            Object tx = Refl.call(ptt, "getText");
            if (tx instanceof String) s.text = ((String) tx).trim();
            Object cv = Refl.call(ptt, "getCanConvert2Text");
            if (cv instanceof Boolean) s.canConvert = (Boolean) cv;
            Object ts = Refl.call(ptt, "getTranslateStatus");
            if (ts instanceof Number) s.translateStatus = ((Number) ts).intValue();
        } catch (Throwable ignore) {
        }
        if (s.text == null) s.text = "";

        if (s.path != null) s.kind = "local";
        else if (s.url != null && s.url.length() > 0) s.kind = "url";

        Object rep = Refl.field(el, "replyElement");
        if (rep != null && "none".equals(s.kind)) {
            Object elems = Refl.field(rep, "sourceMsgTextElems");
            if (elems instanceof List) {
                for (Object e : (List<?>) elems) {
                    Source one = fromElement(e);
                    if (one != null && !"none".equals(one.kind)) return one;
                }
            }
        }
        return s;
    }

    public static byte[] bytes(Source s) {
        if (s == null) return null;
        if (s.path != null) {
            byte[] b = ImageGrabber.readFile(s.path);
            if (b != null && b.length > 0) {
                AiDebug.log("  · 语音: 本地文件 " + (b.length / 1024) + "KB");
                return b;
            }
            AiDebug.log("  · 语音: 本地路径读不出 " + s.path);
        }

        java.util.List<String> urls = new java.util.ArrayList<String>();
        if (s.url != null && s.url.length() > 0) urls.add(s.url);
        if (s.fid != null && s.fid.length() > 10) {

            String cached = RKeyHolder.cachedUrl(s.fid);
            if (cached != null && cached.indexOf("rkey=") > 0) urls.add(0, cached);

            int[] appids = new int[]{1403, 1416, 1406};
            for (int a : appids) {
                urls.add("https://multimedia.nt.qq.com.cn/download?appid=" + a
                        + "&fileid=" + s.fid + "&spec=0");
            }
        }
        for (int i = 0; i < urls.size(); i++) {
            String[] cands = RKeyHolder.candidates(urls.get(i));
            if (cands.length == 0) cands = new String[]{urls.get(i)};
            for (int k = 0; k < cands.length; k++) {
                byte[] b = ImageGrabber.http(cands[k]);
                if (b != null && b.length > 64) {
                    AiDebug.log("  · 语音: 下载成功 " + (b.length / 1024) + "KB（候选 "
                            + i + "/" + k + "）");
                    return b;
                }
            }
        }
        if (!urls.isEmpty()) AiDebug.log("  · " + RKeyHolder.status());
        if (s.md5 != null && s.md5.length() >= 8) {
            byte[] b = fromCacheByMd5(s.md5);
            if (b != null) {
                AiDebug.log("  · 语音: md5 缓存命中");
                return b;
            }
        }
        return null;
    }

    private static byte[] fromCacheByMd5(String md5) {
        try {
            java.io.File root = com.cathelperawa.owo.app.bridge.QQEnv.appContext()
                    .getFilesDir().getParentFile();
            for (String d : PTT_DIRS) {
                java.io.File dir = new java.io.File(root, d);
                if (!dir.isDirectory()) continue;
                java.io.File f = new java.io.File(dir, md5 + ".slk");
                if (f.isFile()) return ImageGrabber.readFile(f.getAbsolutePath());
            }
        } catch (Throwable ignore) {}
        return null;
    }

    public static String dump(Source s) {
        if (s == null || s.raw == null) return "无 ptt";
        StringBuilder sb = new StringBuilder("ptt{");
        java.lang.reflect.Field[] fs = s.raw.getClass().getDeclaredFields();
        for (java.lang.reflect.Field f : fs) {
            try {
                f.setAccessible(true);
                Object v = f.get(s.raw);
                String sv = String.valueOf(v);
                if (sv.length() > 60) sv = sv.substring(0, 60) + "…";
                sb.append(f.getName()).append('=').append(sv).append(", ");
            } catch (Throwable ignore) {}
        }
        return sb.append('}').toString();
    }

    private static String firstString(Object o, String[] names) {
        for (String n : names) {
            String v = Refl.firstStr(o, n);
            if (v != null && v.length() > 0) return v;
        }
        return null;
    }

    private static String shortUrl(String u) {
        if (u == null) return "null";
        int q = u.indexOf('?');
        return q > 0 ? u.substring(0, Math.min(q, 48)) : u.substring(0, Math.min(u.length(), 48));
    }
}
