package com.cathelperawa.owo.app.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class MethodPicker {

    private static final int IMPOSSIBLE = Integer.MAX_VALUE;

    private final Class<?> cls;
    private Class<?>[] params;
    private int paramCount = -1;
    private Class<?> returns;
    private String nameLike;
    private boolean staticOnly = false;
    private boolean publicOnly = false;
    private boolean constructors = false;

    private SearchScope scope = SearchScope.DECLARED;

    private MethodPicker(Class<?> cls) {
        this.cls = cls;
    }

    public static MethodPicker forClass(Class<?> cls) {
        return new MethodPicker(cls);
    }

    public MethodPicker asConstructor() {
        this.constructors = true;
        return this;
    }

    public MethodPicker scope(SearchScope scope) {
        this.scope = (scope == null ? SearchScope.DECLARED : scope);
        return this;
    }

    public MethodPicker params(Class<?>... types) {
        this.params = types;
        this.paramCount = types == null ? -1 : types.length;
        return this;
    }

    public MethodPicker paramCount(int n) {
        this.paramCount = n;
        this.params = null;
        return this;
    }

    public MethodPicker returns(Class<?> ret) {
        this.returns = ret;
        return this;
    }

    public MethodPicker nameLike(String s) {
        this.nameLike = s;
        return this;
    }

    public MethodPicker staticOnly() {
        this.staticOnly = true;
        return this;
    }

    public MethodPicker publicOnly() {
        this.publicOnly = true;
        return this;
    }

    public Method pick() {
        List<Scored> list = scoreAll();
        if (list.isEmpty()) return null;
        Scored best = list.get(0);
        if (list.size() > 1 && best.compareTo(list.get(1)) == 0) return null;
        return (Method) best.exec;
    }

    public Constructor<?> pickConstructor() {
        asConstructor();
        List<Scored> list = scoreAll();
        if (list.isEmpty()) return null;
        Scored best = list.get(0);
        if (list.size() > 1 && best.compareTo(list.get(1)) == 0) return null;
        return (Constructor<?>) best.exec;
    }

    public Method pickFirst() {
        List<Scored> list = scoreAll();
        return list.isEmpty() ? null : (Method) list.get(0).exec;
    }

    public List<Executable> candidates() {
        List<Executable> out = new ArrayList<Executable>();
        for (Scored s : scoreAll()) out.add(s.exec);
        return out;
    }

    public boolean isAmbiguous() {
        List<Scored> list = scoreAll();
        return list.size() > 1 && list.get(0).compareTo(list.get(1)) == 0;
    }

    private List<Scored> scoreAll() {
        List<Scored> out = new ArrayList<Scored>();
        if (cls == null) return out;
        if (constructors) {
            for (Executable e : cls.getDeclaredConstructors()) {
                Scored s = score(e);
                if (s != null) out.add(s);
            }
            Collections.sort(out);
            return out;
        }
        if (scope == null || scope == SearchScope.DECLARED) {
            for (Executable e : cls.getDeclaredMethods()) {
                Scored s = score(e);
                if (s != null) out.add(s);
            }
            Collections.sort(out);
            return out;
        }

        java.util.Set<Class<?>> seen = new java.util.HashSet<Class<?>>();
        Class<?> c = cls;
        int distance = 0;
        while (c != null) {
            for (Executable e : c.getDeclaredMethods()) {
                Scored s = score(e);
                if (s != null) {
                    s.dist = distance;
                    out.add(s);
                }
            }
            if (scope.includesInterfaces()) {
                for (Class<?> itf : c.getInterfaces()) {
                    collectInterfaces(itf, distance + 1, out, seen);
                }
            }
            if (!scope.includesSuper()) break;
            c = c.getSuperclass();
            distance++;
        }
        Collections.sort(out);
        return out;
    }

    private void collectInterfaces(Class<?> itf, int distance, List<Scored> out, java.util.Set<Class<?>> seen) {
        if (itf == null || !seen.add(itf)) return;
        for (Executable e : itf.getDeclaredMethods()) {
            Scored s = score(e);
            if (s != null) {
                s.dist = distance;
                out.add(s);
            }
        }
        for (Class<?> sup : itf.getInterfaces()) {
            collectInterfaces(sup, distance + 1, out, seen);
        }
    }

    private Scored score(Executable e) {
        Class<?>[] ps = e.getParameterTypes();

        if (paramCount >= 0 && ps.length != paramCount) return null;

        if (!constructors && returns != null) {
            Class<?> rt = ((Method) e).getReturnType();
            if (!isCompatible(returns, rt)) return null;
        }

        int mod = e.getModifiers();
        if (staticOnly && !Modifier.isStatic(mod)) return null;
        if (publicOnly && !Modifier.isPublic(mod)) return null;

        int total = 0;
        int max = 0;
        int varArgs = 0;
        boolean isVar = e.isVarArgs();
        for (int i = 0; i < ps.length; i++) {
            Class<?> want = null;
            if (params != null) {
                if (i < params.length) want = params[i];
            }
            if (want == null) continue;

            int cost;
            if (isVar && i == ps.length - 1) {

                Class<?> elem = ps[i].getComponentType();
                cost = elem == null ? IMPOSSIBLE : costOf(want, elem) + 2;
                varArgs = 1;
            } else {
                cost = costOf(want, ps[i]);
            }
            if (cost >= IMPOSSIBLE) return null;
            total += cost;
            if (cost > max) max = cost;
        }

        int dist = distance(e.getDeclaringClass(), cls);
        int synth = e.isSynthetic() ? 1 : 0;
        int bridge = (e instanceof Method && ((Method) e).isBridge()) ? 2 : 0;

        if (nameLike != null && !constructors) {
            String n = e.getName();
            if (n.equals(nameLike)) total -= 1;
            else if (n.contains(nameLike)) total -= 0;
        }

        return new Scored(e, total, max, varArgs, dist, synth, bridge);
    }

    private static int costOf(Class<?> want, Class<?> actual) {
        if (want == actual) return 0;
        if (want.equals(actual)) return 0;

        if (isBoxedMatch(want, actual)) return 1;

        if (want.isAssignableFrom(actual)) return 1;

        return IMPOSSIBLE;
    }

    private static boolean isCompatible(Class<?> want, Class<?> actual) {
        return costOf(want, actual) < IMPOSSIBLE;
    }

    private static boolean isBoxedMatch(Class<?> a, Class<?> b) {
        return boxed(a) == b || boxed(b) == a;
    }

    private static Class<?> boxed(Class<?> c) {
        if (c == null || !c.isPrimitive()) return null;
        if (c == int.class) return Integer.class;
        if (c == long.class) return Long.class;
        if (c == boolean.class) return Boolean.class;
        if (c == byte.class) return Byte.class;
        if (c == short.class) return Short.class;
        if (c == char.class) return Character.class;
        if (c == float.class) return Float.class;
        if (c == double.class) return Double.class;
        if (c == void.class) return Void.class;
        return null;
    }

    private static int distance(Class<?> declaring, Class<?> target) {
        if (declaring == null || target == null) return 99;
        if (declaring == target) return 0;
        int d = 0;
        Class<?> c = target;
        while (c != null) {
            if (c == declaring) return d;
            c = c.getSuperclass();
            d++;
            if (d > 8) break;
        }
        return 9;
    }

    private static final class Scored implements Comparable<Scored> {
        final Executable exec;
        final int total, max, varArgs, synth, bridge;

        int dist;

        Scored(Executable exec, int total, int max, int varArgs,
               int dist, int synth, int bridge) {
            this.exec = exec;
            this.total = total;
            this.max = max;
            this.varArgs = varArgs;
            this.dist = dist;
            this.synth = synth;
            this.bridge = bridge;
        }

        @Override
        public int compareTo(Scored o) {
            if (total != o.total) return total < o.total ? -1 : 1;
            if (max != o.max) return max < o.max ? -1 : 1;
            if (varArgs != o.varArgs) return varArgs < o.varArgs ? -1 : 1;
            if (dist != o.dist) return dist < o.dist ? -1 : 1;
            if (synth != o.synth) return synth < o.synth ? -1 : 1;
            if (bridge != o.bridge) return bridge < o.bridge ? -1 : 1;
            return 0;
        }
    }

    public String explain() {
        StringBuilder sb = new StringBuilder();
        sb.append(cls == null ? "null" : cls.getName())
                .append(" 候选 ").append(scoreAll().size()).append(" 个\n");
        for (Scored s : scoreAll()) {
            sb.append("  ").append(s.exec instanceof Method
                            ? ((Method) s.exec).getName() : "<init>")
                    .append("  total=").append(s.total)
                    .append(" max=").append(s.max)
                    .append(" var=").append(s.varArgs)
                    .append(" dist=").append(s.dist)
                    .append(" synth=").append(s.synth)
                    .append(" bridge=").append(s.bridge)
                    .append('\n');
        }
        return sb.toString();
    }

    public static Comparator<Executable> comparator() {
        return new Comparator<Executable>() {
            @Override
            public int compare(Executable a, Executable b) {
                return 0;
            }
        };
    }
}
