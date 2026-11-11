package com.cathelperawa.owo.app.protocol;

public final class PbWire {

    private PbWire() {}

    

    
    public static long nestedVarint(byte[] data, int outer, int inner) {
        int i = 0;
        while (i < data.length) {
            long[] kr = readKey(data, i);
            if (kr == null) return 0;
            int field = (int) (kr[0] >>> 3), wt = (int) (kr[0] & 7);
            i = (int) kr[1];
            if (field == outer && wt == 2) {
                long[] lr = readVarint(data, i);
                if (lr == null) return 0;
                int len = (int) lr[0];
                int off = (int) lr[1];
                return scanVarint(data, off, Math.min(len, data.length - off), inner);
            }
            int nx = skip(data, i, wt);
            if (nx < 0) return 0;
            i = nx;
        }
        return 0;
    }

    public static long topVarint(byte[] data, int target) {
        int i = 0;
        while (i < data.length) {
            long[] kr = readKey(data, i);
            if (kr == null) return 0;
            int field = (int) (kr[0] >>> 3), wt = (int) (kr[0] & 7);
            i = (int) kr[1];
            if (wt == 0) {
                long[] vr = readVarint(data, i);
                if (vr == null) return 0;
                if (field == target) return vr[0];
                i = (int) vr[1];
            } else {
                int nx = skip(data, i, wt);
                if (nx < 0) return 0;
                i = nx;
            }
        }
        return 0;
    }

    public static byte[] topBytes(byte[] data, int target) {
        int i = 0;
        while (i < data.length) {
            long[] kr = readKey(data, i);
            if (kr == null) return null;
            int field = (int) (kr[0] >>> 3), wt = (int) (kr[0] & 7);
            i = (int) kr[1];
            if (wt == 2) {
                long[] lr = readVarint(data, i);
                if (lr == null) return null;
                int len = (int) lr[0], off = (int) lr[1];
                if (field == target) {
                    byte[] out = new byte[len];
                    System.arraycopy(data, off, out, 0, Math.min(len, data.length - off));
                    return out;
                }
                i = off + len;
            } else if (wt == 0) {
                long[] vr = readVarint(data, i);
                if (vr == null) return null;
                i = (int) vr[1];
            } else {
                int nx = skip(data, i, wt);
                if (nx < 0) return null;
                i = nx;
            }
        }
        return null;
    }

    public static long scanVarint(byte[] d, int off, int len, int target) {
        int i = off, end = Math.min(off + len, d.length);
        while (i < end) {
            long[] kr = readKey(d, i);
            if (kr == null) return 0;
            int field = (int) (kr[0] >>> 3), wt = (int) (kr[0] & 7);
            i = (int) kr[1];
            if (wt == 0) {
                long[] vr = readVarint(d, i);
                if (vr == null) return 0;
                if (field == target) return vr[0];
                i = (int) vr[1];
            } else {
                int nx = skip(d, i, wt);
                if (nx < 0 || nx > end) return 0;
                i = nx;
            }
        }
        return 0;
    }

    

    
    public static byte[] rewriteNestedVarint(byte[] data, int outer, int inner, long value) {
        try {
            int i = 0;
            java.io.ByteArrayOutputStream headOut = new java.io.ByteArrayOutputStream();
            while (i < data.length) {
                int start = i;
                long[] kr = readKey(data, i);
                if (kr == null) return null;
                int field = (int) (kr[0] >>> 3), wt = (int) (kr[0] & 7);
                i = (int) kr[1];
                if (field == outer && wt == 2) {
                    long[] lr = readVarint(data, i);
                    if (lr == null) return null;
                    int len = (int) lr[0], off = (int) lr[1];
                    byte[] sub = new byte[len];
                    System.arraycopy(data, off, sub, 0, Math.min(len, data.length - off));
                    byte[] newSub = rewriteVarint(sub, inner, value);
                    if (newSub == null) return null;
                    headOut.write(data, start, (int) kr[1] - start);   
                    writeVarint(headOut, newSub.length);
                    headOut.write(newSub);
                    headOut.write(data, off + len, data.length - off - len);
                    return headOut.toByteArray();
                }
                int nx = skip(data, i, wt);
                if (nx < 0) return null;
                headOut.write(data, start, nx - start);
                i = nx;
            }
        } catch (Throwable ignore) {
        }
        return null;
    }

    public static byte[] rewriteVarint(byte[] data, int target, long value) {
        try {
            int i = 0;
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            boolean hit = false;
            while (i < data.length) {
                int start = i;
                long[] kr = readKey(data, i);
                if (kr == null) return null;
                int field = (int) (kr[0] >>> 3), wt = (int) (kr[0] & 7);
                i = (int) kr[1];
                if (wt == 0) {
                    long[] vr = readVarint(data, i);
                    if (vr == null) return null;
                    if (field == target) {
                        if (!hit) {
                            out.write(data, start, (int) kr[1] - start);
                            writeVarint(out, value);
                            hit = true;
                        }
                        
                    } else {
                        out.write(data, start, (int) vr[1] - start);
                    }
                    i = (int) vr[1];
                } else {
                    int nx = skip(data, i, wt);
                    if (nx < 0) return null;
                    out.write(data, start, nx - start);
                    i = nx;
                }
            }
            if (!hit) {
                writeKey(out, target, 0);
                writeVarint(out, value);
            }
            return out.toByteArray();
        } catch (Throwable ignore) {
        }
        return null;
    }

    

    
    public static long[] readKey(byte[] d, int i) {
        long[] v = readVarint(d, i);
        return v;
    }

    public static long[] readVarint(byte[] d, int i) {
        if (d == null || i < 0 || i >= d.length) return null;
        long v = 0;
        int sh = 0;
        while (i < d.length) {
            int b = d[i++] & 0xff;
            v |= (long) (b & 0x7f) << sh;
            if ((b & 0x80) == 0) return new long[]{v, i};
            sh += 7;
            if (sh > 63) return null;
        }
        return null;
    }

    public static void writeVarint(java.io.ByteArrayOutputStream out, long value) {
        while (true) {
            if ((value & ~0x7FL) == 0) {
                out.write((int) value);
                return;
            }
            out.write((int) ((value & 0x7F) | 0x80));
            value >>>= 7;
        }
    }

    public static void writeKey(java.io.ByteArrayOutputStream out, int field, int wireType) {
        writeVarint(out, ((long) field << 3) | wireType);
    }

    public static int skip(byte[] d, int i, int wt) {
        try {
            if (wt == 2) {
                long[] lr = readVarint(d, i);
                if (lr == null) return -1;
                long len = lr[0];
                return (int) (lr[1] + len);
            } else if (wt == 0) {
                long[] vr = readVarint(d, i);
                return vr == null ? -1 : (int) vr[1];
            } else if (wt == 5) {
                return i + 4;
            } else if (wt == 1) {
                return i + 8;
            }
        } catch (Throwable ignore) {
        }
        return -1;
    }

    public static String hex(byte[] d, int max) {
        if (d == null) return "null";
        StringBuilder sb = new StringBuilder();
        int n = Math.min(d.length, max);
        for (int i = 0; i < n; i++) sb.append(String.format("%02x", d[i]));
        if (d.length > max) sb.append("...(共 ").append(d.length).append(" 字节)");
        return sb.toString();
    }
}
