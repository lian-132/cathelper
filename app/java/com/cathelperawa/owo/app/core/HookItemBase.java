package com.cathelperawa.owo.app.core;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;

public abstract class HookItemBase {

    protected Context ctx;
    
    protected ClassLoader cl;
    
    private final List<Object> handles = new ArrayList<Object>();

    private final FeatureReport report = new FeatureReport();

    void bind(Context ctx, ClassLoader cl) {
        this.ctx = ctx;
        this.cl = cl;
    }

    

    
    public final FeatureReport report() {
        return report;
    }

    protected final void fail(String why) {
        report.fail(why);
        HookLog.w(displayName() + "：" + why);
    }

    protected final void fail(String why, Throwable t) {
        report.fail(why, t);
        HookLog.w(displayName() + "：" + why);
    }

    protected final void hit() {
        report.hit();
    }

    protected final void hit(int n) {
        report.hit(n);
    }

    protected final void expect(int n) {
        report.expect(n);
    }

    protected final void hint(String text) {
        report.setFireHint(text);
    }

    

    

    

    

    private final SelfCheckResult selfCheck = new SelfCheckResult();

    private final java.util.concurrent.atomic.AtomicBoolean checking =
            new java.util.concurrent.atomic.AtomicBoolean(false);

    protected void check(SelfCheckResult.Checker c) {
        
    }

    public final SelfCheckResult selfCheck() {
        return selfCheck;
    }

    public final boolean needsSelfCheck() {
        return selfCheck.isEmpty();
    }

    public final SelfCheckResult runSelfCheck() {
        if (!checking.compareAndSet(false, true)) return selfCheck;
        try {
            selfCheck.clear();

            SelfCheckResult.Checker c = new SelfCheckResult.Checker(selfCheck, cl);
            try {
                check(c);
            } catch (Throwable t) {
                c.fail("自检执行", t.getClass().getSimpleName()
                        + (t.getMessage() == null ? "" : " " + t.getMessage()));
            }
            selfCheck.checkedAt = System.currentTimeMillis();
            HookLog.i(displayName() + " 自检: " + selfCheck.summary());
            return selfCheck;
        } finally {
            checking.set(false);
        }
    }

    protected final void noInstrument() {
        report.markNoInstrument();
    }

    protected final void fire() {
        report.fire(null);
    }

    protected final void fire(String point) {
        report.fire(point);
    }

    protected final void fire(String point, int n) {
        report.fire(point, n);
    }

    private String displayName() {
        HookItem m = meta();
        return m == null ? getClass().getSimpleName() : m.name();
    }

    public final String featureName() {
        return displayName();
    }

    private final java.util.concurrent.atomic.AtomicInteger runtimeErrors =
            new java.util.concurrent.atomic.AtomicInteger(0);

    public final void noteRuntimeError(Throwable t) {
        runtimeErrors.incrementAndGet();
        try {
            report.markRuntimeError(t);
        } catch (Throwable ignore) {
        }
    }

    

    
    public String[] dependsOn() {
        DependsOn d = getClass().getAnnotation(DependsOn.class);
        return d == null ? EMPTY_DEP : d.value();
    }

    public final String deepProblem() {
        return deepProblem(new java.util.HashSet<String>(), 0);
    }

    private String deepProblem(java.util.Set<String> seen, int depth) {
        String key = getClass().getName();
        if (!seen.add(key)) return null;              
        if (depth > 6) return null;                   

        if (runtimeErrorCount() > 0) {
            return "自身运行期错误 " + runtimeErrorCount() + " 次";
        }
        FeatureReport r = report();
        if (r != null && r.status() == FeatureStatus.FAILED) {
            return "自身失败：" + r.reason();
        }

        for (String dep : dependsOn()) {
            if (dep == null || dep.isEmpty()) continue;

            String probe = HealthRegistry.check(dep);
            if (probe != null) return "依赖「" + dep + "」：" + probe;

            HookItemBase child = HookItemFactory.byName(dep);
            if (child != null && child != this) {
                String p = ((HookItemBase) child).deepProblem(seen, depth + 1);
                if (p != null) return "依赖「" + dep + "」：" + p;
            }
        }
        return null;
    }

    private static final String[] EMPTY_DEP = new String[0];

    public final int runtimeErrorCount() {
        return runtimeErrors.get();
    }

    public final HookItem meta() {
        return getClass().getAnnotation(HookItem.class);
    }

    
    public boolean mainProcessOnly() { return false; }

    public String switchKey() {
        return getClass().getName();
    }

    public boolean isEnabled() {
        return HookConfig.isEnabled(ctx, this);
    }

    public void setEnabled(boolean on) {
        HookConfig.setEnabled(ctx, this, on);
    }

    public abstract void entry(ClassLoader cl);

    

    
    protected final Object keep(Object handle) {
        if (handle != null) handles.add(handle);
        return handle;
    }

    public void unhookAll() {
        for (Object h : handles) {
            try {
                HookApi.unhook(h);
            } catch (Throwable ignore) {
            }
        }
        handles.clear();
    }

    

    protected final Object hookBefore(java.lang.reflect.Member m, HookApi.Action a) {
        Object h = keep(HookApi.before(m, a, this));
        if (h != null) report.hit();      
        return h;
    }

    protected final Object hookAfter(java.lang.reflect.Member m, HookApi.Action a) {
        Object h = keep(HookApi.after(m, a, this));
        if (h != null) report.hit();      
        return h;
    }

    protected final Object hookAll(XC_MethodHook cb) {
        return keep(cb);
    }
}
