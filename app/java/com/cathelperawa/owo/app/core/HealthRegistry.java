package com.cathelperawa.owo.app.core;

import java.util.HashMap;
import java.util.Map;

public final class HealthRegistry {

    public interface Probe {

        String probe();
    }

    private static final Map<String, Probe> PROBES = new HashMap<>();
    private static final Map<String, Cached> CACHE = new HashMap<>();
    private static final long TTL_MS = 5000;

    private static final class Cached {
        final String problem;
        final long at;

        Cached(String problem, long at) {
            this.problem = problem;
            this.at = at;
        }
    }

    private HealthRegistry() {}

    public static synchronized void register(String key, Probe p) {
        PROBES.put(key, p);
    }

    public static synchronized String[] keys() {
        return PROBES.keySet().toArray(new String[0]);
    }

    public static synchronized String check(String key) {
        if (key == null) return null;
        long now = System.currentTimeMillis();
        Cached c = CACHE.get(key);
        if (c != null && now - c.at < TTL_MS) return c.problem;

        Probe p = PROBES.get(key);
        if (p == null) {

            return null;
        }
        String problem;
        try {
            problem = p.probe();
        } catch (Throwable t) {
            problem = t.getClass().getSimpleName() + ": " + t.getMessage();
        }
        CACHE.put(key, new Cached(problem, now));
        return problem;
    }

    public static synchronized void invalidate() {
        CACHE.clear();
    }

    public static void registerBuiltins() {
        register("bridge.element", new Probe() {
            @Override public String probe() {
                try {
                    Object o = com.cathelperawa.owo.app.bridge.ElementFactory.text("健康检查");
                    return o == null ? "文本元素造不出来（MsgElement 缺失？）" : null;
                } catch (Throwable t) {
                    return "文本元素异常：" + t;
                }
            }
        });
        register("bridge.graytip", new Probe() {
            @Override public String probe() {
                try {
                    android.content.Context c = HookItemFactory.context();
                    if (c == null) return "没有宿主 Context";
                    String s = com.cathelperawa.owo.app.bridge.GrayTip.selfTest(c.getClassLoader());
                    return s == null ? "JsonGrayElement 造不出来" : null;
                } catch (Throwable t) {
                    return "灰字异常：" + t;
                }
            }
        });
        register("core.dex", new Probe() {
            @Override public String probe() {
                try {
                    android.content.Context c = HookItemFactory.context();
                    if (c == null) return "没有宿主 Context";
                    com.cathelperawa.owo.app.core.dex.DexIndex ix =
                            com.cathelperawa.owo.app.core.dex.DexIndexLoader.get(c);
                    return ix == null ? "dex 索引建不起来" : null;
                } catch (Throwable t) {
                    return "dex 异常：" + t;
                }
            }
        });
        HookLog.i("健康探针已登记 " + PROBES.size() + " 个");
    }
}
