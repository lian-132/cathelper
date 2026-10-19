package com.cathelperawa.owo.app.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class FeatureReport {

    private final List<String> problems = new ArrayList<String>();

    private int hitCount = 0;

    private int expectCount = 0;

    private String reason = "";

    private long beganAt = 0;

    private long endedAt = 0;

    private FeatureStatus status = FeatureStatus.UNKNOWN;

    private final java.util.concurrent.atomic.AtomicInteger fireCount =
            new java.util.concurrent.atomic.AtomicInteger(0);

    private volatile java.util.Map<String, java.util.concurrent.atomic.AtomicInteger> firePoints;

    private volatile long firstFireAt = 0;

    private volatile long lastFireAt = 0;

    private volatile Runnable onFirstFire;

    public void fire(String point) {
        if (!explicitlyNoInstrument) instrumented = true;
        fireCount.incrementAndGet();
        long now = System.currentTimeMillis();
        lastFireAt = now;
        if (firstFireAt == 0) {
            firstFireAt = now;
            Runnable cb = onFirstFire;
            if (cb != null) {
                try { cb.run(); } catch (Throwable ignore) {}
            }
        }
        if (point != null && point.length() > 0) {
            java.util.Map<String, java.util.concurrent.atomic.AtomicInteger> m = firePoints;
            if (m == null) {
                synchronized (this) {
                    m = firePoints;
                    if (m == null) {
                        m = new java.util.concurrent.ConcurrentHashMap<
                                String, java.util.concurrent.atomic.AtomicInteger>();
                        firePoints = m;
                    }
                }
            }
            java.util.concurrent.atomic.AtomicInteger c = m.get(point);
            if (c == null) {
                synchronized (m) {
                    c = m.get(point);
                    if (c == null) {
                        c = new java.util.concurrent.atomic.AtomicInteger(0);
                        m.put(point, c);
                    }
                }
            }
            c.incrementAndGet();
        }
    }

    public void fire(String point, int n) {
        if (n <= 0) return;
        if (!explicitlyNoInstrument) instrumented = true;
        int total = fireCount.addAndGet(n);
        long now = System.currentTimeMillis();
        lastFireAt = now;
        if (firstFireAt == 0) {
            firstFireAt = now;
            Runnable cb = onFirstFire;
            if (cb != null) {
                try { cb.run(); } catch (Throwable ignore) {}
            }
        }
        if (point == null || point.length() == 0) return;
        java.util.Map<String, java.util.concurrent.atomic.AtomicInteger> m = firePoints;
        if (m == null) {
            synchronized (this) {
                m = firePoints;
                if (m == null) {
                    m = new java.util.concurrent.ConcurrentHashMap<
                            String, java.util.concurrent.atomic.AtomicInteger>();
                    firePoints = m;
                }
            }
        }
        java.util.concurrent.atomic.AtomicInteger c = m.get(point);
        if (c == null) {
            synchronized (m) {
                c = m.get(point);
                if (c == null) {
                    c = new java.util.concurrent.atomic.AtomicInteger(0);
                    m.put(point, c);
                }
            }
        }
        c.addAndGet(n);
    }

    public int fireCount() {
        return fireCount.get();
    }

    public boolean everFired() {
        return fireCount.get() > 0;
    }

    public long firstFireAt() {
        return firstFireAt;
    }

    public long lastFireAt() {
        return lastFireAt;
    }

    public java.util.Map<String, Integer> firePoints() {
        java.util.Map<String, java.util.concurrent.atomic.AtomicInteger> m = firePoints;
        if (m == null || m.isEmpty()) return java.util.Collections.emptyMap();
        java.util.Map<String, Integer> out =
                new java.util.LinkedHashMap<String, Integer>();
        for (java.util.Map.Entry<String, java.util.concurrent.atomic.AtomicInteger> e
                : m.entrySet()) {
            out.put(e.getKey(), e.getValue().get());
        }
        return out;
    }

    public void setOnFirstFire(Runnable r) {
        onFirstFire = r;
    }

    private String fireHint = "";

    private boolean instrumented = false;

    public void setFireHint(String hint) {
        fireHint = hint == null ? "" : hint;

        if (!explicitlyNoInstrument) instrumented = true;
    }

    public String fireHint() {
        return fireHint == null ? "" : fireHint;
    }

    private boolean explicitlyNoInstrument = false;

    public void setInstrumented(boolean on) {
        instrumented = on;
        if (on) explicitlyNoInstrument = false;
    }

    public void markNoInstrument() {
        instrumented = false;
        explicitlyNoInstrument = true;
    }

    public boolean isInstrumented() {
        return instrumented;
    }

    public String fireSummary() {
        int n = fireCount.get();

        if (!instrumented) return "未埋点";

        if (n == 0) {
            String h = fireHint();
            return h.length() > 0 ? "尚未触发（" + h + "）" : "尚未触发";
        }
        if (n < 1000) return "已触发 " + n + " 次";
        return "已触发 " + (n / 1000) + "k+ 次";
    }

    public void begin() {
        beganAt = System.currentTimeMillis();
        endedAt = 0;
        status = FeatureStatus.LOADING;
        hitCount = 0;
        problems.clear();
        reason = "";
    }

    public void hit() {
        hitCount++;
    }

    public void hit(int n) {
        if (n > 0) hitCount += n;
    }

    public void expect(int n) {
        expectCount = n;
    }

    public void fail(String why) {
        if (why != null && why.length() > 0) {
            problems.add(why);
            reason = why;
        }
    }

    public void fail(String why, Throwable t) {
        fail(why + (t == null ? "" : " :: " + t.getClass().getSimpleName()
                + (t.getMessage() == null ? "" : " " + t.getMessage())));
    }

    public void done() {
        endedAt = System.currentTimeMillis();
        if (status == FeatureStatus.DISABLED) return;
        if (!problems.isEmpty()) {

            status = hitCount == 0 ? FeatureStatus.FAILED : FeatureStatus.PARTIAL;
            return;
        }
        if (expectCount > 0 && hitCount < expectCount) {
            status = FeatureStatus.PARTIAL;
            reason = "只装了 " + hitCount + "/" + expectCount + " 个钩子";
            return;
        }
        status = hitCount > 0 ? FeatureStatus.OK : FeatureStatus.UNKNOWN;
    }

    public void markDisabled() {
        status = FeatureStatus.DISABLED;
        endedAt = System.currentTimeMillis();
    }

    public void markUnsupported(int minVersion) {
        status = FeatureStatus.UNSUPPORTED;
        endedAt = System.currentTimeMillis();
        reason = "需宿主 ≥ " + HostVersion.format(minVersion);
    }

    private final java.util.concurrent.atomic.AtomicInteger runtimeErr =
            new java.util.concurrent.atomic.AtomicInteger(0);

    public void markRuntimeError(Throwable t) {
        runtimeErr.incrementAndGet();
    }

    public int runtimeErrorCount() {
        return runtimeErr.get();
    }

    public void markError(Throwable t) {
        status = FeatureStatus.ERROR;
        endedAt = System.currentTimeMillis();
        fail("entry 抛异常", t);
    }

    public void setStatus(FeatureStatus s) {
        if (s != null) status = s;
    }

    public FeatureStatus status() { return status; }

    public int hitCount() { return hitCount; }

    public int expectCount() { return expectCount; }

    public String reason() { return reason == null ? "" : reason; }

    public List<String> problems() {
        return Collections.unmodifiableList(problems);
    }

    public long costMs() {
        if (beganAt == 0 || endedAt == 0) return -1;
        return endedAt - beganAt;
    }

    public String summary() {
        StringBuilder sb = new StringBuilder();
        if (expectCount > 0) {
            sb.append(hitCount).append('/').append(expectCount);
        } else if (hitCount > 0) {
            sb.append(hitCount).append(" 个钩子");
        }
        if (sb.length() > 0) sb.append(" · ");
        sb.append(status.label);
        long c = costMs();
        if (c >= 0) sb.append(" · ").append(c).append("ms");
        return sb.toString();
    }
}
