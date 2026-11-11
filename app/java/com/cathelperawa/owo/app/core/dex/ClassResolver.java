package com.cathelperawa.owo.app.core.dex;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ClassResolver {

    private static final String TAG = "CatClassResolve";
    private static final String PREF = "cat_classmap_v1";

    public interface Validator {
        boolean accept(Class<?> c);
    }

    public static final class Spec {
        private final String id;
        private String[] candidates = new String[0];
        private String superClass;
        private String iface;
        private String sourceFile;
        private String nameRegex;
        private String[] methods = new String[0];
        private Validator validator;
        private boolean allowAmbiguous;

        public Spec(String id) { this.id = id; }

        public Spec candidates(String... names) { this.candidates = names; return this; }

        public Spec superClass(String c) { this.superClass = c; return this; }

        public Spec implementing(String i) { this.iface = i; return this; }

        public Spec fromSource(String f) { this.sourceFile = f; return this; }

        public Spec withMethod(String... m) { this.methods = m; return this; }

        public Spec nameMatches(String regex) { this.nameRegex = regex; return this; }

        public Spec validator(Validator v) { this.validator = v; return this; }

        public Spec allowAmbiguous() { this.allowAmbiguous = true; return this; }
    }

    private static final Map<String, String> MEM = new HashMap<>();

    public interface ContextProvider { Context get(); }

    private static volatile ContextProvider sCtxProvider;

    public static void installContextProvider(ContextProvider p) { sCtxProvider = p; }

    private static Context ctx() {
        ContextProvider p = sCtxProvider;
        return p == null ? null : p.get();
    }

    private ClassResolver() {}

    public static String findName(ClassLoader cl, String id, String... candidates) {
        Context c = ctx();
        if (c == null) {
            for (String n : candidates) if (loadable(n, cl)) return n;
            return null;
        }
        Spec spec = new Spec(id).candidates(candidates)
                .nameMatches(regexOfSimpleNames(candidates));
        return resolveName(c, cl, spec);
    }

    private static String regexOfSimpleNames(String... candidates) {
        StringBuilder sb = new StringBuilder();
        for (String n : candidates) {
            if (n == null || n.isEmpty()) continue;
            int dot = n.lastIndexOf('.');
            String simple = dot >= 0 ? n.substring(dot + 1) : n;
            if (simple.length() < 4) continue;          
            if (sb.length() > 0) sb.append('|');
            sb.append("\\b").append(java.util.regex.Pattern.quote(simple));
        }
        sb.append("(\\$|$)");
        return sb.toString();
    }

    public static Class<?> find(ClassLoader cl, String id, String... candidates) {
        String n = findName(cl, id, candidates);
        if (n == null) return null;
        try { return Class.forName(n, false, cl); } catch (Throwable t) { return null; }
    }

    
    public static Class<?> resolve(Context ctx, ClassLoader cl, Spec spec) {
        String name = resolveName(ctx, cl, spec);
        if (name == null) return null;
        try {
            return Class.forName(name, false, cl);
        } catch (Throwable t) {
            return null;
        }
    }

    public static String resolveName(Context ctx, ClassLoader cl, Spec spec) {
        
        String hit = MEM.get(spec.id);
        if (hit != null && loadable(hit, cl)) return hit;
        MEM.remove(spec.id);

        for (String c : spec.candidates) {
            if (c != null && loadable(c, cl) && passes(c, cl, spec)) {
                MEM.put(spec.id, c);
                return c;
            }
        }

        
        
        Context app = ctx == null ? null : ctx.getApplicationContext();
        if (app == null) {
            for (String c : spec.candidates) {
                if (c != null && loadable(c, cl) && passes(c, cl, spec)) return c;
            }
            return null;
        }
        SharedPreferences sp = app.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        String saved = sp.getString(spec.id, null);
        if (saved != null) {
            if (loadable(saved, cl) && passes(saved, cl, spec)) {
                MEM.put(spec.id, saved);
                return saved;
            }
            sp.edit().remove(spec.id).apply();   
        }

        String found = search(app, cl, spec);
        if (found != null) {
            MEM.put(spec.id, found);
            sp.edit().putString(spec.id, found).apply();
        }
        return found;
    }

    public static void forget(Context ctx) {
        MEM.clear();
        ctx.getApplicationContext()
           .getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().clear().apply();
    }

    private static String search(Context app, ClassLoader cl, Spec spec) {
        DexIndex ix;
        try {
            ix = DexIndexLoader.get(app);
        } catch (Throwable t) {
            return null;
        }
        if (ix == null) return null;

        ClassFinder.Query q = new ClassFinder(ix).query()
                .includeNested();                     
        if (spec.superClass != null) q.superClass(spec.superClass);
        if (spec.iface != null) q.implementing(spec.iface);
        if (spec.sourceFile != null) q.fromSource(spec.sourceFile);
        if (spec.nameRegex != null) q.nameMatches(spec.nameRegex);
        for (String m : spec.methods) q.withMethod(m);

        if (spec.superClass == null && spec.iface == null
                && spec.sourceFile == null && spec.methods.length == 0
                && spec.nameRegex == null) {
            return null;                              
        }

        List<ClassFinder.Result> rs = q.find();
        if (rs.isEmpty()) return null;
        if (rs.size() == 1) return verify(rs.get(0).className, cl, spec);
        if (spec.allowAmbiguous && rs.get(0).score > rs.get(1).score)
            return verify(rs.get(0).className, cl, spec);

        String only = null;
        for (ClassFinder.Result r : rs) {
            String ok = verify(r.className, cl, spec);
            if (ok == null) continue;
            if (only != null) {
                Log.w(TAG, spec.id + " 歧义：" + rs.size() + " 个候选都通过校验，放弃");
                return null;
            }
            only = ok;
        }
        return only;
    }

    private static String verify(String name, ClassLoader cl, Spec spec) {
        if (!loadable(name, cl)) return null;
        return passes(name, cl, spec) ? name : null;
    }

    private static boolean loadable(String name, ClassLoader cl) {
        try {
            Class.forName(name, false, cl);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private static boolean passes(String name, ClassLoader cl, Spec spec) {
        if (spec.validator == null) return true;
        try {
            return spec.validator.accept(Class.forName(name, false, cl));
        } catch (Throwable t) {
            return false;
        }
    }
}
