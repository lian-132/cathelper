package com.cathelperawa.owo.app.feature;

import com.cathelperawa.owo.app.core.HookLog;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

public final class SharePatch {

    public static volatile boolean enabled = false;
    
    public static volatile String title = "";
    
    public static volatile String cover = "";
    
    public static volatile int patched = 0;

    private SharePatch() {
    }

    

    private static void log(String s) {
        try {
            HookLog.i("SharePatch " + s);
            android.content.Context c = com.cathelperawa.owo.app.bridge.QQEnv.appContext();
            if (c == null) return;
            java.io.File dir = new java.io.File(c.getFilesDir().getParent(), "files/cathelper");
            if (!dir.exists()) dir.mkdirs();
            java.io.File f = new java.io.File(dir, "share_patch.txt");
            if (f.length() > 300000L) f.delete();
            java.io.FileOutputStream fo = new java.io.FileOutputStream(f, true);
            fo.write((System.currentTimeMillis() / 1000 + " " + s + "\n").getBytes("UTF-8"));
            fo.flush();
            fo.close();
        } catch (Throwable ignore) {
        }
    }

    

    
    public static byte[] onSend(String cmd, byte[] wup) {
        if (!enabled || wup == null || wup.length < 8) return null;
        if (cmd == null || !cmd.contains("PbSendMsg")) return null;
        log("收到 PbSendMsg len=" + wup.length + " enabled=" + enabled
                + " title=" + title + " cover=" + (cover.length() > 40 ? cover.substring(0, 40) : cover));
        try {
            boolean hasHead = (wup[0] & 0xFF) == 0;
            byte[] body = hasHead && wup.length > 4
                    ? java.util.Arrays.copyOfRange(wup, 4, wup.length) : wup;
            dumpHex(body, "share_pb.hex");
            boolean[] hit = new boolean[]{false};
            byte[] out = walk(body, hit, 0);
            log("walk 完成 hit=" + hit[0] + " out=" + (out == null ? -1 : out.length));
            if (!hit[0] || out == null || out.length == 0) return null;
            patched++;
            HookLog.i("SharePatch: 已改写分享包 " + body.length + " -> " + out.length);
            if (!hasHead) return out;
            byte[] res = new byte[out.length + 4];
            res[0] = (byte) ((out.length >>> 24) & 0xFF);
            res[1] = (byte) ((out.length >>> 16) & 0xFF);
            res[2] = (byte) ((out.length >>> 8) & 0xFF);
            res[3] = (byte) (out.length & 0xFF);
            System.arraycopy(out, 0, res, 4, out.length);
            return res;
        } catch (Throwable t) {
            HookLog.e("SharePatch.onSend", t);
            return null;
        }
    }

    private static void dumpHex(byte[] b, String name) {
        try {
            android.content.Context c = com.cathelperawa.owo.app.bridge.QQEnv.appContext();
            if (c == null || b == null) return;
            java.io.File dir = new java.io.File(c.getFilesDir().getParent(), "files/cathelper");
            if (!dir.exists()) dir.mkdirs();
            StringBuilder sb = new StringBuilder();
            sb.append("# len=").append(b.length).append("\n");
            for (byte x : b) sb.append(String.format("%02x", x & 0xFF));
            sb.append("\n");
            java.io.FileOutputStream fo = new java.io.FileOutputStream(new java.io.File(dir, name), false);
            fo.write(sb.toString().getBytes("UTF-8"));
            fo.flush(); fo.close();
        } catch (Throwable ignore) {
        }
    }

    

    
    private static byte[] walk(byte[] buf, boolean[] hit, int depth) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int i = 0;
        while (i < buf.length) {
            int tagStart = i;
            long tag;
            int[] pos = new int[]{i};
            tag = readVarint(buf, pos);
            if (tag < 0) break;
            i = pos[0];
            int field = (int) (tag >>> 3);
            int wt = (int) (tag & 7);

            if (wt == 2) {
                pos[0] = i;
                long lenL = readVarint(buf, pos);
                if (lenL < 0 || pos[0] + lenL > buf.length) break;
                int len = (int) lenL;
                int payloadStart = pos[0];
                byte[] payload = java.util.Arrays.copyOfRange(buf, payloadStart, payloadStart + len);
                i = payloadStart + len;

                byte[] newPayload = null;
                byte[] replaced = (override != null && isZlibArk(payload))
                        ? override : patchArkPayload(payload);
                if (replaced != null) {
                    log("命中 ARK field=" + field + " " + payload.length + " -> " + replaced.length);
                    newPayload = replaced;
                    hit[0] = true;
                } else if (len > 2 && depth < 9) {
                    
                    boolean[] inner = new boolean[]{false};
                    byte[] walked = walk(payload, inner, depth + 1);
                    if (inner[0] && walked != null) {
                        newPayload = walked;
                        hit[0] = true;
                    }
                }
                if (newPayload == null) newPayload = payload;

                writeTag(out, field, 2);
                writeVarint(out, newPayload.length);
                out.write(newPayload, 0, newPayload.length);
            } else if (wt == 0) {
                pos[0] = i;
                long v = readVarint(buf, pos);
                i = pos[0];
                writeTag(out, field, 0);
                writeVarint(out, v);
            } else if (wt == 5) {
                if (i + 4 > buf.length) break;
                writeTag(out, field, 5);
                out.write(buf, i, 4);
                i += 4;
            } else if (wt == 1) {
                if (i + 8 > buf.length) break;
                writeTag(out, field, 1);
                out.write(buf, i, 8);
                i += 8;
            } else {
                
                return null;
            }
        }
        return out.toByteArray();
    }

    public static byte[] extractArk(byte[] pb) {
        if (pb == null) return null;
        try {
            int[] pos = new int[]{0};
            return hunt(pb, pb.length);
        } catch (Throwable t) {
            return null;
        }
    }

    private static byte[] hunt(byte[] buf, int end) {
        int i = 0;
        while (i < end) {
            int[] pos = new int[]{i};
            long tag = readVarint(buf, pos);
            if (tag < 0) return null;
            i = pos[0];
            int f = (int) (tag >>> 3);
            int wt = (int) (tag & 7);
            if (wt == 2) {
                pos[0] = i;
                long lenL = readVarint(buf, pos);
                if (lenL < 0) return null;
                int len = (int) lenL;
                int st = pos[0];
                if (st + len > end) return null;
                byte[] seg = java.util.Arrays.copyOfRange(buf, st, st + len);
                if (isZlibArk(seg)) return seg;
                byte[] deep = hunt(seg, seg.length);
                if (deep != null) return deep;
                i = st + len;
            } else if (wt == 0) {
                pos[0] = i;
                readVarint(buf, pos);
                i = pos[0];
            } else if (wt == 5) i += 4;
            else if (wt == 1) i += 8;
            else return null;
        }
        return null;
    }

    public static String inflateArk(byte[] payload) {
        if (!isZlibArk(payload)) return null;
        int off = (payload[0] & 0xFF) == 0x01 ? 1 : 0;
        byte[] raw = inflate(payload, off);
        if (raw == null) return null;
        try {
            return new String(raw, "UTF-8");
        } catch (Throwable t) {
            return null;
        }
    }

    public static String patchText(String json, String newTitle, String newCover) {
        String t = title, c = cover;
        title = newTitle;
        cover = newCover;
        try {
            return patchJsonText(json);
        } finally {
            title = t;
            cover = c;
        }
    }

    public static byte[] packArk(String json) {
        try {
            byte[] gz = deflate(json.getBytes("UTF-8"));
            if (gz == null) return null;
            byte[] res = new byte[gz.length + 1];
            res[0] = 0x01;
            System.arraycopy(gz, 0, res, 1, gz.length);
            return res;
        } catch (Throwable t) {
            return null;
        }
    }

    private static volatile byte[] override = null;

    private static boolean isZlibArk(byte[] payload) {
        if (payload == null || payload.length < 24) return false;
        int b0 = payload[0] & 0xFF;
        int b1 = payload.length > 1 ? (payload[1] & 0xFF) : 0;
        return (b0 == 0x01 && b1 == 0x78) || b0 == 0x78;
    }

    public static byte[] keepOnlyFirstElem(byte[] pb) {
        if (pb == null) return null;
        try {
            boolean[] hit = new boolean[]{false};
            byte[] a = keepFirst(pb, new int[]{3, 1}, 0, hit);
            return hit[0] ? a : null;
        } catch (Throwable t) {
            return null;
        }
    }

    private static byte[] keepFirst(byte[] buf, int[] path, int depth, boolean[] hit) {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        int i = 0;
        boolean keptFirst = false;
        while (i < buf.length) {
            int[] pos = new int[]{i};
            long tag = readVarint(buf, pos);
            if (tag < 0) return null;
            i = pos[0];
            int f = (int) (tag >>> 3);
            int wt = (int) (tag & 7);
            if (wt == 2) {
                pos[0] = i;
                long lenL = readVarint(buf, pos);
                if (lenL < 0 || pos[0] + lenL > buf.length) return null;
                int len = (int) lenL;
                int st = pos[0];
                i = st + len;
                byte[] payload = java.util.Arrays.copyOfRange(buf, st, st + len);
                if (depth < path.length && f == path[depth]) {
                    
                    if (depth == path.length - 1) {
                        
                        byte[] filtered = filterElem(payload);
                        if (filtered != null) {
                            payload = filtered;
                            hit[0] = true;
                        }
                    } else {
                        boolean[] inner = new boolean[]{false};
                        byte[] w = keepFirst(payload, path, depth + 1, inner);
                        if (inner[0] && w != null) {
                            payload = w;
                            hit[0] = true;
                        }
                    }
                }
                writeTag(out, f, 2);
                writeVarint(out, payload.length);
                out.write(payload, 0, payload.length);
            } else if (wt == 0) {
                pos[0] = i;
                long v = readVarint(buf, pos);
                i = pos[0];
                writeTag(out, f, 0);
                writeVarint(out, v);
            } else if (wt == 5) {
                if (i + 4 > buf.length) return null;
                writeTag(out, f, 5);
                out.write(buf, i, 4);
                i += 4;
            } else if (wt == 1) {
                if (i + 8 > buf.length) return null;
                writeTag(out, f, 1);
                out.write(buf, i, 8);
                i += 8;
            } else {
                return null;
            }
        }
        return out.toByteArray();
    }

    private static byte[] filterElem(byte[] buf) {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        int i = 0;
        int seen = 0;
        boolean dropped = false;
        while (i < buf.length) {
            int[] pos = new int[]{i};
            long tag = readVarint(buf, pos);
            if (tag < 0) return null;
            i = pos[0];
            int f = (int) (tag >>> 3);
            int wt = (int) (tag & 7);
            if (wt == 2) {
                pos[0] = i;
                long lenL = readVarint(buf, pos);
                if (lenL < 0 || pos[0] + lenL > buf.length) return null;
                int len = (int) lenL;
                int st = pos[0];
                i = st + len;
                if (f == 2) {
                    seen++;
                    if (seen > 1) {
                        dropped = true;
                        continue;
                    }
                }
                writeTag(out, f, 2);
                writeVarint(out, len);
                out.write(buf, st, len);
            } else if (wt == 0) {
                pos[0] = i;
                long v = readVarint(buf, pos);
                i = pos[0];
                writeTag(out, f, 0);
                writeVarint(out, v);
            } else if (wt == 5) {
                if (i + 4 > buf.length) return null;
                writeTag(out, f, 5);
                out.write(buf, i, 4);
                i += 4;
            } else if (wt == 1) {
                if (i + 8 > buf.length) return null;
                writeTag(out, f, 1);
                out.write(buf, i, 8);
                i += 8;
            } else {
                return null;
            }
        }
        return dropped ? out.toByteArray() : null;
    }

    public static byte[] reseed(byte[] pb) {
        if (pb == null || pb.length < 8) return null;
        try {
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            int i = 0;
            long now = System.currentTimeMillis();
            while (i < pb.length) {
                int[] pos = new int[]{i};
                long tag = readVarint(pb, pos);
                if (tag < 0) return null;
                i = pos[0];
                int f = (int) (tag >>> 3);
                int wt = (int) (tag & 7);
                if (wt == 0) {
                    pos[0] = i;
                    long v = readVarint(pb, pos);
                    i = pos[0];
                    if (f == 4) v = now & 0xFFFF;
                    else if (f == 5) v = (now >>> 16) & 0xFFFFFFFFL;
                    writeTag(out, f, 0);
                    writeVarint(out, v);
                } else if (wt == 2) {
                    pos[0] = i;
                    long lenL = readVarint(pb, pos);
                    if (lenL < 0 || pos[0] + lenL > pb.length) return null;
                    int len = (int) lenL;
                    int st = pos[0];
                    i = st + len;
                    writeTag(out, f, 2);
                    writeVarint(out, len);
                    out.write(pb, st, len);
                } else if (wt == 5) {
                    if (i + 4 > pb.length) return null;
                    writeTag(out, f, 5);
                    out.write(pb, i, 4);
                    i += 4;
                } else if (wt == 1) {
                    if (i + 8 > pb.length) return null;
                    writeTag(out, f, 1);
                    out.write(pb, i, 8);
                    i += 8;
                } else {
                    return null;
                }
            }
            return out.toByteArray();
        } catch (Throwable t) {
            return null;
        }
    }

    private static final int[] ARK_PATH = {3, 1, 2, 51, 1};

    public static byte[] replaceArkPayload(byte[] pb, byte[] newPacked) {
        if (pb == null || newPacked == null) return null;
        try {
            boolean[] hit = new boolean[]{false};
            byte[] out = walkPath(pb, ARK_PATH, 0, newPacked, hit);
            if (!hit[0] || out == null || out.length == 0) return null;
            return out;
        } catch (Throwable t) {
            HookLog.e("replaceArkPayload", t);
            return null;
        }
    }

    private static byte[] walkPath(byte[] buf, int[] path, int depth,
                                   byte[] newPayload, boolean[] hit) {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        int i = 0;
        while (i < buf.length) {
            int[] pos = new int[]{i};
            long tag = readVarint(buf, pos);
            if (tag < 0) return null;
            i = pos[0];
            int f = (int) (tag >>> 3);
            int wt = (int) (tag & 7);
            if (wt == 2) {
                pos[0] = i;
                long lenL = readVarint(buf, pos);
                if (lenL < 0 || pos[0] + lenL > buf.length) return null;
                int len = (int) lenL;
                int st = pos[0];
                i = st + len;
                byte[] payload = java.util.Arrays.copyOfRange(buf, st, st + len);
                byte[] repl = null;
                if (!hit[0] && depth < path.length && f == path[depth]) {
                    if (depth == path.length - 1) {
                        repl = newPayload;
                        hit[0] = true;
                    } else {
                        boolean[] inner = new boolean[]{false};
                        byte[] w = walkPath(payload, path, depth + 1, newPayload, inner);
                        if (inner[0] && w != null) {
                            repl = w;
                            hit[0] = true;
                        }
                    }
                }
                if (repl == null) repl = payload;
                writeTag(out, f, 2);
                writeVarint(out, repl.length);
                out.write(repl, 0, repl.length);
            } else if (wt == 0) {
                pos[0] = i;
                long v = readVarint(buf, pos);
                i = pos[0];
                writeTag(out, f, 0);
                writeVarint(out, v);
            } else if (wt == 5) {
                if (i + 4 > buf.length) return null;
                writeTag(out, f, 5);
                out.write(buf, i, 4);
                i += 4;
            } else if (wt == 1) {
                if (i + 8 > buf.length) return null;
                writeTag(out, f, 1);
                out.write(buf, i, 8);
                i += 8;
            } else {
                return null;
            }
        }
        return out.toByteArray();
    }

    private static byte[] patchArkPayload(byte[] payload) {
        if (payload == null || payload.length < 24) return null;
        int off = -1;
        int b0 = payload[0] & 0xFF;
        int b1 = payload.length > 1 ? (payload[1] & 0xFF) : 0;
        if (b0 == 0x01 && b1 == 0x78) off = 1;        
        else if (b0 == 0x78) off = 0;                  
        if (off < 0) return null;
        byte[] raw = inflate(payload, off);
        if (raw == null || raw.length < 24) return null;
        try {
            String json = new String(raw, "UTF-8");
            if (!json.contains("\"app\"")) return null;
            String changed = patchJsonText(json);
            if (changed == null) return null;
            byte[] gz = deflate(changed.getBytes("UTF-8"));
            if (gz == null) return null;
            if (off == 1) {
                byte[] res = new byte[gz.length + 1];
                res[0] = 0x01;
                System.arraycopy(gz, 0, res, 1, gz.length);
                return res;
            }
            return gz;
        } catch (Throwable t) {
            return null;
        }
    }

    private static byte[] inflate(byte[] b, int off) {
        try {
            java.util.zip.Inflater inf = new java.util.zip.Inflater();
            inf.setInput(b, off, b.length - off);
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            while (!inf.finished()) {
                int n = inf.inflate(buf);
                if (n <= 0) break;
                out.write(buf, 0, n);
            }
            inf.end();
            return out.toByteArray();
        } catch (Throwable t) {
            return null;
        }
    }

    private static byte[] deflate(byte[] data) {
        try {

            java.util.zip.Deflater def =
                    new java.util.zip.Deflater(java.util.zip.Deflater.NO_COMPRESSION);
            def.setInput(data);
            def.finish();
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            while (!def.finished()) {
                int n = def.deflate(buf);
                if (n <= 0) break;
                out.write(buf, 0, n);
            }
            def.end();
            return out.toByteArray();
        } catch (Throwable t) {
            return null;
        }
    }

    private static boolean looksLikeArk(byte[] b) {
        if (b.length < 24) return false;
        int i = 0;
        while (i < b.length && (b[i] == ' ' || b[i] == '\n')) i++;
        if (i >= b.length || b[i] != '{') return false;
        String head = new String(b, i, Math.min(160, b.length - i));
        return head.contains("\"app\"")
                && (head.contains("feed.lua") || head.contains("tencent.forum")
                || head.contains("forum") || head.contains("feed"));
    }

    private static String patchJsonText(String rawJson) {
        try {
            String json = rawJson == null ? "" : rawJson.trim();
            int z = json.indexOf('\u0000');
            if (z > 0) json = json.substring(0, z).trim();
            if (!json.startsWith("{") || !json.endsWith("}")) return null;
            JSONObject ark = new JSONObject(json);
            boolean changed = false;
            String t = title == null ? "" : title.trim();
            String c = cover == null ? "" : cover.trim();

            JSONObject meta = ark.optJSONObject("meta");
            if (meta != null) {
                
                JSONObject feed = meta.optJSONObject("feed");
                if (feed != null) {
                    if (t.length() > 0) {
                        feed.put("title", t);
                        changed = true;
                        
                        if (ark.has("prompt")) ark.put("prompt", "[分享帖子] " + t);
                    }
                    if (c.length() > 0) {
                        feed.put("cover", c);
                        changed = true;
                    }
                }
                
                JSONObject detail = meta.optJSONObject("detail");
                if (detail != null) {
                    JSONObject df = detail.optJSONObject("feed");
                    if (df != null) {
                        if (t.length() > 0) {
                            setTextBlocks(df.optJSONObject("title"), t);
                            setTextBlocks(df.optJSONObject("contents"), t);
                            changed = true;
                        }
                        if (c.length() > 0) {
                            JSONArray imgs = new JSONArray();
                            imgs.put(new JSONObject()
                                    .put("pic_url", c)
                                    .put("width", 300)
                                    .put("height", 300)
                                    .put("pattern_id", "1"));
                            df.put("images", imgs);
                            changed = true;
                        }
                    }
                }
            }
            if (!changed) return null;
            return ark.toString();
        } catch (Throwable t) {
            return null;
        }
    }

    private static void setTextBlocks(JSONObject block, String text) {
        try {
            if (block == null) return;
            JSONArray arr = block.optJSONArray("contents");
            if (arr == null) {
                arr = new JSONArray();
                block.put("contents", arr);
            }
            if (arr.length() == 0) {
                arr.put(new JSONObject()
                        .put("text_content", new JSONObject().put("text", text))
                        .put("type", 1));
                return;
            }
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o == null) continue;
                if (o.optInt("type", 0) == 1) {
                    JSONObject tc = o.optJSONObject("text_content");
                    if (tc == null) {
                        tc = new JSONObject();
                        o.put("text_content", tc);
                    }
                    tc.put("text", text);
                    return;
                }
            }
            arr.put(new JSONObject()
                    .put("text_content", new JSONObject().put("text", text))
                    .put("type", 1));
        } catch (Throwable ignore) {
        }
    }

    

    private static long readVarint(byte[] b, int[] pos) {
        long r = 0;
        int s = 0;
        int i = pos[0];
        while (i < b.length) {
            int x = b[i++] & 0xFF;
            r |= (long) (x & 0x7F) << s;
            if ((x & 0x80) == 0) {
                pos[0] = i;
                return r;
            }
            s += 7;
            if (s > 63) break;
        }
        return -1;
    }

    private static void writeVarint(ByteArrayOutputStream out, long v) {
        while ((v & ~0x7FL) != 0) {
            out.write((int) ((v & 0x7F) | 0x80));
            v >>>= 7;
        }
        out.write((int) v);
    }

    private static void writeTag(ByteArrayOutputStream out, int field, int wt) {
        writeVarint(out, ((long) field << 3) | wt);
    }
}
