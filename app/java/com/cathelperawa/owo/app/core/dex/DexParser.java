package com.cathelperawa.owo.app.core.dex;

public final class DexParser {

    public interface Visitor {
        void onClass(String descriptor, String superDescriptor,
                     String[] interfaces, int accessFlags,
                     String[] methodNames, String sourceFile);

        default void onClassStrings(String descriptor, java.util.Set<String> used) {}
    }

    private static final int OFF_STRING_SIZE = 56, OFF_STRING_OFF = 60;
    private static final int OFF_TYPE_SIZE = 64, OFF_TYPE_OFF = 68;
    private static final int OFF_METHOD_SIZE = 88, OFF_METHOD_OFF = 92;
    private static final int OFF_CLASS_SIZE = 96, OFF_CLASS_OFF = 100;
    private static final int HEADER_SIZE = 112;

    private static final int ACC_INTERFACE = 0x0200;

    private final byte[] d;
    private final int stringIdsOff, stringIdsSize;
    private final int typeIdsOff, typeIdsSize;
    private final int methodIdsOff, methodIdsSize;

    private final String[] strCache;

    private boolean wantStrings;

    public DexParser withStrings(boolean yes) {
        this.wantStrings = yes;
        return this;
    }

    public DexParser(byte[] dex) {
        if (dex.length < HEADER_SIZE + 1) throw new IllegalArgumentException("dex 太短");
        if (dex[0] != 'd' || dex[1] != 'e' || dex[2] != 'x' || dex[3] != '\n')
            throw new IllegalArgumentException("不是 dex（magic 不对）");
        this.d = dex;
        this.stringIdsSize = u4(OFF_STRING_SIZE);
        this.stringIdsOff = u4(OFF_STRING_OFF);
        this.typeIdsSize = u4(OFF_TYPE_SIZE);
        this.typeIdsOff = u4(OFF_TYPE_OFF);
        this.methodIdsSize = u4(OFF_METHOD_SIZE);
        this.methodIdsOff = u4(OFF_METHOD_OFF);
        this.strCache = new String[stringIdsSize];
    }

    public void readStrings(java.util.List<String> out) {
        for (int i = 0; i < stringIdsSize; i++) {
            String s = str(i);
            if (s != null && !s.isEmpty()) out.add(s);
        }
    }

    public int readMethodStringRefs(int codeOff, java.util.Set<String> out) {
        if (codeOff <= 0 || codeOff + 16 > d.length) return 0;
        int insnsSize = u4(codeOff + 12);
        if (insnsSize <= 0 || insnsSize > 200_000) return 0;
        int insns = codeOff + 16;
        int end = insns + insnsSize * 2;
        if (end > d.length) return 0;

        int got = 0;
        int p = insns;
        while (p + 2 <= end) {
            int unit = u2(p);
            int op = unit & 0xFF;
            int len = insnWidth(op);
            if (len <= 0) break;
            if (op == 0x1A && p + 4 <= end) {
                int idx = u2(p + 2);
                String s = str(idx);
                if (s != null && out.add(s)) got++;
            } else if (op == 0x1B && p + 6 <= end) {
                int idx = u2(p + 2) | (u2(p + 4) << 16);
                String s = str(idx);
                if (s != null && out.add(s)) got++;
            }
            p += len * 2;
        }
        return got;
    }

    private static int insnWidth(int op) {
        switch (op) {
            case 0x00: case 0x01: case 0x04: case 0x07:
            case 0x0A: case 0x0B: case 0x0C: case 0x0D:
            case 0x0E: case 0x0F: case 0x10: case 0x11:
            case 0x12:
            case 0x1D: case 0x1E:
            case 0x21:
            case 0x27: case 0x28:
            case 0x73: case 0x79: case 0x7A:
                return 1;
            case 0x02: case 0x05: case 0x08:
            case 0x13: case 0x15: case 0x16: case 0x19:
            case 0x1A:
            case 0x1C: case 0x1F: case 0x20: case 0x22: case 0x23:
            case 0x29:
            case 0xFE: case 0xFF:
                return 2;
            case 0x03: case 0x06: case 0x09:
            case 0x14: case 0x17:
            case 0x1B:
            case 0x24: case 0x25: case 0x26:
            case 0x2A: case 0x2B: case 0x2C:
            case 0x6E: case 0x6F: case 0x70: case 0x71: case 0x72:
            case 0x74: case 0x75: case 0x76: case 0x77: case 0x78:
            case 0xFC: case 0xFD:
                return 3;
            case 0x18:
                return 5;
            case 0xFA: case 0xFB:
                return 4;
            default: break;
        }
        if (op >= 0x2D && op <= 0x3D) return 2;
        if (op >= 0x3E && op <= 0x43) return 1;
        if (op >= 0x44 && op <= 0x6D) return 2;
        if (op >= 0x7B && op <= 0x8F) return 1;
        if (op >= 0x90 && op <= 0xAF) return 2;
        if (op >= 0xB0 && op <= 0xCF) return 1;
        if (op >= 0xD0 && op <= 0xE2) return 2;
        if (op >= 0xE3 && op <= 0xF9) return 1;
        return 0;
    }

    public void parse(Visitor v) {
        int classSize = u4(OFF_CLASS_SIZE);
        int classOff = u4(OFF_CLASS_OFF);
        for (int i = 0; i < classSize; i++) {
            int p = classOff + i * 32;
            int classIdx = u4(p);
            int accessFlags = u4(p + 4);
            int superIdx = u4(p + 8);
            int interfacesOff = u4(p + 12);
            int sourceFileIdx = u4(p + 16);
            int classDataOff = u4(p + 24);

            String desc = typeName(classIdx);
            String superDesc = superIdx == -1 ? null : typeName(superIdx);
            String[] ifaces = readInterfaces(interfacesOff);
            String[] methods = readMethodNames(classDataOff);
            String source = sourceFileIdx == -1 ? null : str(sourceFileIdx);

            v.onClass(desc, superDesc, ifaces, accessFlags, methods, source);

            if (wantStrings && lastCodeOffs != null && lastCodeOffs.length > 0) {
                java.util.HashSet<String> used = new java.util.HashSet<>();
                for (int co : lastCodeOffs) readMethodStringRefs(co, used);
                if (!used.isEmpty()) v.onClassStrings(desc, used);
            }
        }
    }

    private String[] readInterfaces(int off) {
        if (off == 0) return EMPTY_STR;
        int n = u4(off);
        if (n <= 0 || n > 4096) return EMPTY_STR;
        String[] out = new String[n];
        for (int i = 0; i < n; i++) out[i] = typeName(u2(off + 4 + i * 2));
        return out;
    }

    private String[] readMethodNames(int dataOff) {
        if (dataOff == 0) return EMPTY_STR;
        int p = dataOff;
        int[] r = new int[4];
        for (int i = 0; i < 4; i++) {
            long[] tmp = new long[1];
            p = uleb(p, tmp);
            r[i] = (int) tmp[0];
        }
        int fieldCount = r[0] + r[1];

        long[] tmp = new long[1];
        for (int i = 0; i < fieldCount; i++) {
            p = uleb(p, tmp);
            p = uleb(p, tmp);
        }
        int methodCount = r[2] + r[3];
        if (methodCount <= 0) return EMPTY_STR;
        if (methodCount > 60000) methodCount = 60000;

        String[] names = new String[methodCount];
        int[] codes = wantStrings ? new int[methodCount] : null;
        int n = 0;

        for (int pass = 0; pass < 2; pass++) {
            int count = pass == 0 ? r[2] : r[3];
            int midx = 0;
            for (int i = 0; i < count && n < methodCount; i++) {
                p = uleb(p, tmp);
                midx += (int) tmp[0];
                p = uleb(p, tmp);
                p = uleb(p, tmp);
                if (codes != null) codes[n] = (int) tmp[0];
                names[n++] = (midx >= 0 && midx < methodIdsSize) ? methodName(midx) : null;
            }
        }
        lastCodeOffs = codes;
        return names;
    }

    private int[] lastCodeOffs;

    private String methodName(int methodIdx) {
        int p = methodIdsOff + methodIdx * 8;
        int nameIdx = u4(p + 4);
        return str(nameIdx);
    }

    private String typeName(int typeIdx) {
        if (typeIdx < 0 || typeIdx >= typeIdsSize) return null;
        int strIdx = u4(typeIdsOff + typeIdx * 4);
        return str(strIdx);
    }

    private String str(int idx) {
        if (idx < 0 || idx >= stringIdsSize) return null;
        String s = strCache[idx];
        if (s != null) return s;
        int p = u4(stringIdsOff + idx * 4);
        long[] sizeTmp = new long[1];
        int q = uleb(p, sizeTmp);
        s = readMutf8(q);
        strCache[idx] = s;
        return s;
    }

    private String readMutf8(int start) {
        char[] buf = new char[64];
        int n = 0;
        int p = start;
        while (p < d.length) {
            int b = d[p] & 0xFF;
            if (b == 0) break;
            if (n >= buf.length) {
                char[] nb = new char[buf.length * 2];
                System.arraycopy(buf, 0, nb, 0, n);
                buf = nb;
            }
            if (b < 0x80) {
                buf[n++] = (char) b;
                p += 1;
            } else if ((b & 0xE0) == 0xC0) {
                int b2 = d[p + 1] & 0xFF;
                buf[n++] = (char) (((b & 0x1F) << 6) | (b2 & 0x3F));
                p += 2;
            } else {
                int b2 = d[p + 1] & 0xFF, b3 = d[p + 2] & 0xFF;
                buf[n++] = (char) (((b & 0x0F) << 12) | ((b2 & 0x3F) << 6) | (b3 & 0x3F));
                p += 3;
            }
        }
        return new String(buf, 0, n);
    }

    private int uleb(int p, long[] out) {
        long result = 0;
        int shift = 0;
        while (p < d.length) {
            int b = d[p++] & 0xFF;
            result |= ((long) (b & 0x7F)) << shift;
            if ((b & 0x80) == 0) break;
            shift += 7;
            if (shift > 35) break;
        }
        out[0] = result;
        return p;
    }

    private int u4(int p) {
        return (d[p] & 0xFF) | ((d[p + 1] & 0xFF) << 8)
                | ((d[p + 2] & 0xFF) << 16) | ((d[p + 3] & 0xFF) << 24);
    }

    private int u2(int p) {
        return (d[p] & 0xFF) | ((d[p + 1] & 0xFF) << 8);
    }

    private static final String[] EMPTY_STR = new String[0];

    public static String toClassName(String descriptor) {
        if (descriptor == null || descriptor.length() < 2) return descriptor;
        if (descriptor.charAt(0) != 'L' || descriptor.charAt(descriptor.length() - 1) != ';')
            return descriptor;
        return descriptor.substring(1, descriptor.length() - 1).replace('/', '.');
    }

    public static String toDescriptor(String className) {
        if (className == null) return null;
        if (className.startsWith("L") && className.endsWith(";")) return className;
        if (className.indexOf('.') < 0 && className.length() <= 1) return className;
        return "L" + className.replace('.', '/') + ";";
    }

    public static boolean isInterface(int accessFlags) {
        return (accessFlags & ACC_INTERFACE) != 0;
    }
}
