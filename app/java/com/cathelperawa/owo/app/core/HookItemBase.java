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

    public final int runtimeErrorCount() {
        return runtimeErrors.get();
    }

    public final HookItem rawMeta() {
        return getClass().getAnnotation(HookItem.class);
    }

    public final String displayName() {
        String n = stat("META_name");
        if (n != null && n.length() > 0) return n;
        HookItem m = rawMeta();
        return m == null || m.name().length() == 0 ? getClass().getSimpleName() : m.name();
    }

    public final String displayPath() {
        String p = stat("META_path");
        if (p != null && p.length() > 0) return p;
        HookItem m = rawMeta();
        return m == null ? "" : m.path();
    }

    public final String displayDesc() {
        String d = stat("META_description");
        if (d != null && d.length() > 0) return d;
        HookItem m = rawMeta();
        return m == null ? "" : m.description();
    }

    private String stat(String field) {
        try {
            java.lang.reflect.Field f = getClass().getField(field);
            Object v = f.get(null);
            return v instanceof String ? (String) v : null;
        } catch (Throwable t) {
            return null;
        }
    }

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
