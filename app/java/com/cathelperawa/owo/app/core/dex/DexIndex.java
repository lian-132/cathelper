package com.cathelperawa.owo.app.core.dex;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public final class DexIndex implements DexParser.Visitor {

    private static final int MAGIC = 0x43544458;
    private static final int FORMAT_VERSION = 4;

    private final ArrayList<String> bDesc = new ArrayList<>();
    private final ArrayList<String> bSuper = new ArrayList<>();
    private final ArrayList<String[]> bIfaces = new ArrayList<>();
    private final ArrayList<Integer> bFlags = new ArrayList<>();
    private final ArrayList<String> bSource = new ArrayList<>();
    private final ArrayList<int[]> bMethods = new ArrayList<>();
    private final HashMap<String, Integer> bMethodPool = new HashMap<>();
    private final ArrayList<String> bMethodNames = new ArrayList<>();
    private final HashMap<String, Integer> bByName = new HashMap<>();

    private final ArrayList<long[]> bStrs = new ArrayList<>();

    private final boolean includeMethods;
    private final boolean includeStrings;

    private String[] desc = EMPTY_STR;
    private String[] superDesc = EMPTY_STR;
    private String[][] ifaceDesc = new String[0][];
    private int[] flags = EMPTY_INT;
    private String[] source = EMPTY_STR;
    private int[][] methods = new int[0][];
    private String[] methodNames = EMPTY_STR;
    private String[] sortedNames;
    private int[] sortedIds;
    private String[] dotNames;
    private long[][] strHashes = new long[0][];
    private HashMap<String, Integer> byName = new HashMap<>();
    private boolean finished;

    public DexIndex() {
        this(true, false);
    }

    public DexIndex(boolean includeMethods) {
        this(includeMethods, false);
    }

    public DexIndex(boolean includeMethods, boolean includeStrings) {
        this.includeMethods = includeMethods;
        this.includeStrings = includeStrings;
    }

    public boolean wantsStrings() { return includeStrings; }

    @Override
    public void onClass(String descriptor, String superDescriptor, String[] interfaces,
                        int accessFlags, String[] methodNameList, String sourceFile) {
        if (descriptor == null) return;
        if (bByName.containsKey(descriptor)) return;

        int id = bDesc.size();
        bDesc.add(descriptor);
        bSuper.add(superDescriptor);
        bIfaces.add(interfaces == null ? EMPTY_STR : interfaces);
        bFlags.add(accessFlags);
        bSource.add(sourceFile);
        bByName.put(descriptor, id);

        if (includeMethods && methodNameList != null && methodNameList.length > 0) {
            int[] ids = new int[methodNameList.length];
            int n = 0;
            for (String m : methodNameList) {
                if (m == null) continue;
                Integer pid = bMethodPool.get(m);
                if (pid == null) {
                    pid = bMethodNames.size();
                    bMethodNames.add(m);
                    bMethodPool.put(m, pid);
                }
                ids[n++] = pid;
            }
            if (n != ids.length) {
                int[] t = new int[n];
                System.arraycopy(ids, 0, t, 0, n);
                ids = t;
            }
            bMethods.add(ids);
        } else {
            bMethods.add(EMPTY_INT);
        }
        if (includeStrings) bStrs.add(EMPTY_LONG);
    }

    @Override
    public void onClassStrings(String descriptor, Set<String> used) {
        if (!includeStrings) return;
        Integer id = bByName.get(descriptor);
        if (id == null) return;
        long[] arr = new long[used.size()];
        int n = 0;
        for (String s : used) {
            if (s == null || s.length() < 6) continue;
            long h = hash64(s);
            boolean dup = false;
            for (int i = 0; i < n; i++) if (arr[i] == h) { dup = true; break; }
            if (!dup) arr[n++] = h;
        }
        if (n != arr.length) {
            long[] t = new long[n];
            System.arraycopy(arr, 0, t, 0, n);
            arr = t;
        }
        if (bStrs.size() == id + 1) bStrs.set(id, arr);
    }

    static long hash64(String s) {
        long h = 0xcbf29ce484222325L;
        for (int i = 0; i < s.length(); i++) {
            h ^= s.charAt(i);
            h *= 0x100000001b3L;
        }
        return h;
    }

    public synchronized DexIndex finish() {
        if (finished) return this;
        int n = bDesc.size();
        desc = bDesc.toArray(new String[n]);
        superDesc = bSuper.toArray(new String[n]);
        ifaceDesc = new String[n][];
        flags = new int[n];
        source = new String[n];
        methods = new int[n][];
        for (int i = 0; i < n; i++) {
            ifaceDesc[i] = bIfaces.get(i);
            flags[i] = bFlags.get(i);
            source[i] = bSource.get(i);
            methods[i] = bMethods.get(i);
        }
        methodNames = bMethodNames.toArray(new String[bMethodNames.size()]);
        if (includeStrings) {
            strHashes = bStrs.toArray(new long[bStrs.size()][]);
        }
        byName = new HashMap<>(bByName);
        finished = true;
        bDesc.clear(); bSuper.clear(); bIfaces.clear(); bFlags.clear();
        bSource.clear(); bMethods.clear(); bMethodPool.clear();
        bMethodNames.clear(); bByName.clear();
        bStrs.clear();
        return this;
    }

    public int size() { return desc.length; }

    public boolean contains(String classNameOrDesc) {
        return byName.containsKey(DexParser.toDescriptor(classNameOrDesc));
    }

    public String resolve(String classNameOrDesc) {
        String d = DexParser.toDescriptor(classNameOrDesc);
        return byName.containsKey(d) ? d : null;
    }

    public List<String> subclassesOf(String superClass) {
        return subclassesOf(superClass, false);
    }

    public List<String> subclassesOf(String superClass, boolean recursive) {
        String sup = DexParser.toDescriptor(superClass);
        List<String> out = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        ArrayDeque<String> frontier = new ArrayDeque<>();
        frontier.add(sup);
        while (!frontier.isEmpty()) {
            String cur = frontier.poll();
            for (int i = 0; i < desc.length; i++) {
                if (cur.equals(superDesc[i]) && seen.add(desc[i])) {
                    out.add(DexParser.toClassName(desc[i]));
                    if (recursive) frontier.add(desc[i]);
                }
            }
        }
        return out;
    }

    public List<String> implementersOf(String interfaceName) {
        String ifc = DexParser.toDescriptor(interfaceName);
        List<String> out = new ArrayList<>();
        for (int i = 0; i < desc.length; i++) {
            String[] ii = ifaceDesc[i];
            for (String x : ii) {
                if (ifc.equals(x)) { out.add(DexParser.toClassName(desc[i])); break; }
            }
        }
        return out;
    }

    public List<String> find(String superClass, String interfaceName) {
        String sup = superClass == null ? null : DexParser.toDescriptor(superClass);
        String ifc = interfaceName == null ? null : DexParser.toDescriptor(interfaceName);
        List<String> out = new ArrayList<>();
        outer:
        for (int i = 0; i < desc.length; i++) {
            if (sup != null && !sup.equals(superDesc[i])) continue;
            if (ifc != null) {
                boolean hit = false;
                for (String x : ifaceDesc[i]) if (ifc.equals(x)) { hit = true; break; }
                if (!hit) continue outer;
            }
            out.add(DexParser.toClassName(desc[i]));
        }
        return out;
    }

    public List<String> classesWithMethod(String methodName) {
        List<String> out = new ArrayList<>();
        if (!includeMethods) return out;
        int pid = indexOfMethod(methodName);
        if (pid < 0) return out;
        for (int i = 0; i < methods.length; i++) {
            for (int m : methods[i]) {
                if (m == pid) { out.add(DexParser.toClassName(desc[i])); break; }
            }
        }
        return out;
    }

    public boolean hasMethod(String classNameOrDesc, String methodName) {
        if (!includeMethods) return false;
        Integer id = byName.get(DexParser.toDescriptor(classNameOrDesc));
        if (id == null) return false;
        int pid = indexOfMethod(methodName);
        if (pid < 0) return false;
        for (int m : methods[id]) if (m == pid) return true;
        return false;
    }

    public List<String> methodsOf(String classNameOrDesc) {
        List<String> out = new ArrayList<>();
        if (!includeMethods) return out;
        Integer id = byName.get(DexParser.toDescriptor(classNameOrDesc));
        if (id == null) return out;
        for (int m : methods[id]) out.add(methodNames[m]);
        return out;
    }

    public String descriptorAt(int i) { return desc[i]; }

    public String classNameAt(int i) { return DexParser.toClassName(desc[i]); }

    public int methodCountAt(int i) { return methods[i].length; }

    public String sourceAt(int i) { return source[i]; }

    public boolean hasMethodAt(int i, String methodName) {
        if (!includeMethods) return false;
        int pid = pidOf(methodName);
        if (pid < 0) return false;
        for (int m : methods[i]) if (m == pid) return true;
        return false;
    }

    private int pidOf(String name) {
        if (methodNames.length == 0) return -1;
        if (sortedNames == null) buildSorted();
        int lo = 0, hi = sortedNames.length - 1;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            int c = sortedNames[mid].compareTo(name);
            if (c == 0) return sortedIds[mid];
            if (c < 0) lo = mid + 1; else hi = mid - 1;
        }
        return -1;
    }

    private synchronized void buildSorted() {
        if (sortedNames != null) return;
        Integer[] order = new Integer[methodNames.length];
        for (int i = 0; i < order.length; i++) order[i] = i;
        final String[] mn = methodNames;
        java.util.Arrays.sort(order, new java.util.Comparator<Integer>() {
            @Override public int compare(Integer a, Integer b) { return mn[a].compareTo(mn[b]); }
        });
        String[] sn = new String[order.length];
        int[] si = new int[order.length];
        for (int i = 0; i < order.length; i++) { sn[i] = mn[order[i]]; si[i] = order[i]; }
        sortedIds = si;
        sortedNames = sn;
    }

    public int methodFrequency(String methodName) {
        if (!includeMethods) return 0;
        int pid = indexOfMethod(methodName);
        if (pid < 0) return 0;
        int c = 0;
        for (int[] ms : methods) for (int m : ms) if (m == pid) c++;
        return c;
    }

    private int indexOfMethod(String name) {
        for (int i = 0; i < methodNames.length; i++) if (methodNames[i].equals(name)) return i;
        return -1;
    }

    public List<String> classesUsingString(String literal) {
        List<String> out = new ArrayList<>();
        if (!includeStrings || literal == null) return out;
        long h = hash64(literal);
        for (int i = 0; i < strHashes.length; i++) {
            for (long x : strHashes[i]) {
                if (x == h) { out.add(DexParser.toClassName(desc[i])); break; }
            }
        }
        return out;
    }

    public boolean hasString(String literal) {
        if (!includeStrings || literal == null) return false;
        long h = hash64(literal);
        for (long[] arr : strHashes) for (long x : arr) if (x == h) return true;
        return false;
    }

    public int stringRefCount() {
        int n = 0;
        for (long[] a : strHashes) n += a.length;
        return n;
    }

    public List<String> classesFromSource(String sourceFileName) {
        List<String> out = new ArrayList<>();
        for (int i = 0; i < source.length; i++) {
            if (sourceFileName.equals(source[i])) out.add(DexParser.toClassName(desc[i]));
        }
        return out;
    }

    public List<String> grep(Pattern p) {
        String[] names = dotNames();
        List<String> out = new ArrayList<>();
        for (String n : names) {
            if (p.matcher(n).find()) out.add(n);
        }
        return out;
    }

    private synchronized String[] dotNames() {
        if (dotNames != null) return dotNames;
        String[] n = new String[desc.length];
        for (int i = 0; i < desc.length; i++) n[i] = DexParser.toClassName(desc[i]);
        dotNames = n;
        return n;
    }

    public String superOf(String classNameOrDesc) {
        Integer id = byName.get(DexParser.toDescriptor(classNameOrDesc));
        if (id == null || superDesc[id] == null) return null;
        return DexParser.toClassName(superDesc[id]);
    }

    public List<String> interfacesOf(String classNameOrDesc) {
        List<String> out = new ArrayList<>();
        Integer id = byName.get(DexParser.toDescriptor(classNameOrDesc));
        if (id == null) return out;
        for (String s : ifaceDesc[id]) out.add(DexParser.toClassName(s));
        return out;
    }

    public boolean isInterface(String classNameOrDesc) {
        Integer id = byName.get(DexParser.toDescriptor(classNameOrDesc));
        return id != null && DexParser.isInterface(flags[id]);
    }

    public String stats() {
        return "类 " + desc.length + " / 方法名 " + methodNames.length;
    }

    public void writeTo(DataOutputStream o) throws IOException {
        finish();
        o.writeInt(MAGIC);
        o.writeInt(FORMAT_VERSION);
        o.writeInt(desc.length);
        o.writeInt(methodNames.length);
        o.writeBoolean(includeStrings);
        for (String s : methodNames) o.writeUTF(s);

        if (includeStrings) {
            o.writeInt(strHashes.length);
            for (long[] arr : strHashes) {
                o.writeShort(arr.length);
                for (long h : arr) o.writeLong(h);
            }
        }
        for (int i = 0; i < desc.length; i++) {
            o.writeUTF(desc[i]);
            o.writeUTF(superDesc[i] == null ? "" : superDesc[i]);
            o.writeInt(flags[i]);
            o.writeUTF(source[i] == null ? "" : source[i]);
            String[] ii = ifaceDesc[i];
            o.writeShort(ii.length);
            for (String x : ii) o.writeUTF(x);
            int[] ms = methods[i];
            o.writeShort(ms.length);
            for (int m : ms) o.writeInt(m);
        }
    }

    public static DexIndex readFrom(DataInputStream in) throws IOException {
        if (in.readInt() != MAGIC) throw new IOException("不是 DexIndex 缓存");
        int ver = in.readInt();
        if (ver != FORMAT_VERSION) throw new IOException("缓存版本不符：" + ver);
        int classCount = in.readInt();
        int methodNameCount = in.readInt();
        boolean hasStrings = in.readBoolean();
        if (classCount <= 0 || classCount > 2_000_000) throw new IOException("缓存损坏");

        DexIndex ix = new DexIndex(true, hasStrings);
        String[] mn = new String[methodNameCount];
        for (int i = 0; i < methodNameCount; i++) mn[i] = in.readUTF();

        if (hasStrings) {
            int sn = in.readInt();
            if (sn < 0 || sn > classCount) throw new IOException("缓存损坏(str)");
            ix.strHashes = new long[sn][];
            for (int i = 0; i < sn; i++) {
                int len = in.readShort() & 0xFFFF;
                long[] arr = new long[len];
                for (int k = 0; k < len; k++) arr[k] = in.readLong();
                ix.strHashes[i] = arr;
            }
        } else {
            ix.strHashes = new long[0][];
        }

        String[] d = new String[classCount];
        String[] sd = new String[classCount];
        int[] fl = new int[classCount];
        String[] src = new String[classCount];
        String[][] ifc = new String[classCount][];
        int[][] ms = new int[classCount][];
        HashMap<String, Integer> map = new HashMap<>(classCount * 2);

        for (int i = 0; i < classCount; i++) {
            d[i] = in.readUTF();
            String s = in.readUTF();
            sd[i] = s.isEmpty() ? null : s;
            fl[i] = in.readInt();
            String sf = in.readUTF();
            src[i] = sf.isEmpty() ? null : sf;
            int in_ = in.readShort() & 0xFFFF;
            ifc[i] = new String[in_];
            for (int k = 0; k < in_; k++) ifc[i][k] = in.readUTF();
            int mc = in.readShort() & 0xFFFF;
            ms[i] = new int[mc];
            for (int k = 0; k < mc; k++) ms[i][k] = in.readInt();
            map.put(d[i], i);
        }
        ix.desc = d; ix.superDesc = sd; ix.flags = fl; ix.source = src;
        ix.ifaceDesc = ifc; ix.methods = ms; ix.methodNames = mn;
        ix.byName = map; ix.finished = true;
        return ix;
    }

    private static final String[] EMPTY_STR = new String[0];
    private static final int[] EMPTY_INT = new int[0];
    private static final long[] EMPTY_LONG = new long[0];
}
