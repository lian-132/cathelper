package com.cathelperawa.owo.app.core.dex;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

public final class ClassFinder {

    public static final class Result {
        public final String className;
        public final int score;
        public final String why;

        Result(String className, int score, String why) {
            this.className = className;
            this.score = score;
            this.why = why;
        }

        @Override public String toString() {
            return className + " (" + score + ": " + why + ")";
        }
    }

    public static final class AmbiguousException extends RuntimeException {
        public final List<Result> candidates;

        AmbiguousException(String what, List<Result> c) {
            super(what + " 有 " + c.size() + " 个候选，无法唯一确定：" + c);
            this.candidates = c;
        }
    }

    private final DexIndex index;

    public ClassFinder(DexIndex index) {
        this.index = index;
    }

    public Query query() {
        return new Query(index);
    }

    public static String uniqueOrNull(List<Result> results) {
        return results.size() == 1 ? results.get(0).className : null;
    }

    public static final class Query {
        private final DexIndex ix;
        private String superClass;
        private boolean recursiveSuper;
        private String iface;
        private String sourceFile;
        private final List<String> withMethods = new ArrayList<>();
        private final List<String> withoutMethods = new ArrayList<>();
        private Pattern namePattern;
        private String literal;
        private java.util.Set<String> strHit;
        private int minMethods = -1;
        private boolean excludeNested = true;
        private boolean onlyConcrete;

        Query(DexIndex ix) { this.ix = ix; }

        public Query superClass(String name) { this.superClass = name; return this; }

        public Query recursiveSuper() { this.recursiveSuper = true; return this; }

        public Query implementing(String name) { this.iface = name; return this; }

        public Query fromSource(String fileName) { this.sourceFile = fileName; return this; }

        public Query usingString(String literal) { this.literal = literal; return this; }

        public Query withMethod(String m) { withMethods.add(m); return this; }

        public Query withoutMethod(String m) { withoutMethods.add(m); return this; }

        public Query nameMatches(String regex) { this.namePattern = Pattern.compile(regex); return this; }

        public Query minMethods(int n) { this.minMethods = n; return this; }

        public Query includeNested() { this.excludeNested = false; return this; }

        public Query onlyConcrete() { this.onlyConcrete = true; return this; }

        public List<Result> find() {

            if (literal != null) strHit = new java.util.HashSet<>(ix.classesUsingString(literal));
            List<int[]> hits = new ArrayList<>();
            List<String> whys = new ArrayList<>();
            for (int i = 0; i < ix.size(); i++) {
                String full = ix.classNameAt(i);
                String why = check(i, full);
                if (why == null) continue;
                hits.add(new int[]{i, score(i, full, why)});
                whys.add(why);
            }
            List<Result> out = new ArrayList<>(hits.size());
            for (int k = 0; k < hits.size(); k++) {
                out.add(new Result(ix.classNameAt(hits.get(k)[0]), hits.get(k)[1], whys.get(k)));
            }
            Collections.sort(out, new Comparator<Result>() {
                @Override public int compare(Result a, Result b) { return b.score - a.score; }
            });
            return out;
        }

        public String findUnique() {
            List<Result> rs = find();
            if (rs.isEmpty()) return null;
            if (rs.size() == 1) return rs.get(0).className;
            if (rs.get(0).score > rs.get(1).score) return rs.get(0).className;
            throw new AmbiguousException("查询", rs.subList(0, Math.min(8, rs.size())));
        }

        private String check(int i, String full) {
            if (excludeNested && full.indexOf('$') >= 0) return null;
            if (onlyConcrete && ix.isInterface(full)) return null;

            StringBuilder why = new StringBuilder();
            if (superClass != null) {
                boolean sup;
                if (recursiveSuper) {
                    sup = false;
                    String cur = full;
                    for (int guard = 0; guard < 64 && cur != null; guard++) {
                        String s = ix.superOf(cur);
                        if (s == null) break;
                        if (s.equals(superClass)) { sup = true; break; }
                        cur = s;
                    }
                } else {
                    sup = superClass.equals(ix.superOf(full));
                }
                if (!sup) return null;
                why.append("super=").append(superClass).append(' ');
            }
            if (iface != null) {
                if (!ix.interfacesOf(full).contains(iface)) return null;
                why.append("impl=").append(iface).append(' ');
            }
            if (sourceFile != null) {
                if (!sourceFile.equals(sourceOf(i))) return null;
                why.append("source=").append(sourceFile).append(' ');
            }
            if (namePattern != null && !namePattern.matcher(full).find()) return null;
            if (literal != null) {
                if (strHit == null || !strHit.contains(full)) return null;
                why.append("str=").append(literal).append(' ');
            }
            if (minMethods >= 0 && ix.methodCountAt(i) < minMethods) return null;
            for (String m : withMethods) {
                if (!ix.hasMethodAt(i, m)) return null;
                why.append('+').append(m).append(' ');
            }
            for (String m : withoutMethods) {
                if (ix.hasMethodAt(i, m)) return null;
            }
            return why.toString();
        }

        private int score(int i, String full, String why) {
            int s = 0;
            if (sourceFile != null) s += 100;
            s += 10 * countOccurrences(why, '+');
            if (readableName(full)) s += 5;
            if (full.contains("$CppProxy") || full.contains("$$")) s -= 50;
            return s;
        }

        private String sourceOf(int i) {
            return ix.sourceAt(i);
        }

        private static int countOccurrences(String s, char c) {
            int n = 0;
            for (int i = 0; i < s.length(); i++) if (s.charAt(i) == c) n++;
            return n;
        }

        private static boolean readableName(String full) {
            int dot = full.lastIndexOf('.');
            String simple = dot >= 0 ? full.substring(dot + 1) : full;
            if (simple.length() < 6) return false;
            int upper = 0, lower = 0;
            for (int i = 0; i < simple.length(); i++) {
                char ch = simple.charAt(i);
                if (ch >= 'A' && ch <= 'Z') upper++;
                else if (ch >= 'a' && ch <= 'z') lower++;
            }
            return upper >= 1 && lower >= 3;
        }
    }
}
