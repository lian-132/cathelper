package com.cathelperawa.owo.app.bridge;

import com.cathelperawa.owo.app.core.HookApi;
import com.cathelperawa.owo.app.core.HookLog;

import java.lang.reflect.Method;

public final class QqRoster {

    private QqRoster() {}

    private static final String[] BUDDY_CLASSES = {
            "com.tencent.qqnt.kernel.api.impl.BuddyService",
            "com.tencent.qqnt.kernel.api.impl.BuddyServiceImpl",
            "com.tencent.mobileqq.friend.api.impl.FriendDataServiceImpl",
            "com.tencent.mobileqq.data.Friends",
    };

    private static final String[] TROOP_CLASSES = {
            "com.tencent.qqnt.troop.impl.TroopListRepoApiImpl",
            "com.tencent.qqnt.troop.impl.TroopListRepoServiceImpl",
            "com.tencent.mobileqq.troop.api.impl.TroopInfoServiceImpl",
            "com.tencent.qqnt.kernel.api.impl.TroopService",
    };

    public static java.util.Map<String, String> troops() {
        java.util.Map<String, String> out = new java.util.LinkedHashMap<String, String>();
        try {
            ClassLoader cl = QQEnv.classLoader();
            Class<?> impl = HookApi.findClass(cl,
                    "com.tencent.qqnt.troop.impl.TroopListRepoApiImpl");
            if (impl == null) return out;
            Object repo = null;
            for (String gn : new String[]{"getInstance", "getService"}) {
                try {
                    Method g = impl.getMethod(gn);
                    g.setAccessible(true);
                    Object v = g.invoke(null);
                    if (v != null) { repo = v; break; }
                } catch (Throwable ignore) {
                }
            }
            if (repo == null) {
                java.lang.reflect.Constructor<?> ct = impl.getDeclaredConstructor();
                ct.setAccessible(true);
                repo = ct.newInstance();
            }
            Object list = null;
            for (String wn : new String[]{"getSortedJoinedTroopInfoFromCache",
                    "getTroopListFromCache", "getJoinedTroopInfoFromCache"}) {
                try {
                    Method m = impl.getMethod(wn);
                    Object r = m.invoke(repo);
                    if (r instanceof java.util.List && !((java.util.List<?>) r).isEmpty()) {
                        list = r;
                        break;
                    }
                } catch (Throwable ignore) {
                }
            }
            if (list == null) return out;
            for (Object ti : (java.util.List<?>) list) {
                if (ti == null) continue;
                String uin = str(ti, "troopuin", "troopUin", "troopcode", "groupCode");
                String nm = str(ti, "troopname", "troopName", "troopNameFromNT", "name");
                if (uin == null || uin.trim().length() == 0) continue;
                out.put(uin.trim(), nm == null ? "" : nm.trim());
            }
        } catch (Throwable t) {
            HookLog.w("QqRoster.troops 失败 " + t);
            note("troops EX " + t);
        }
        note("troops=" + out.size());
        return out;
    }

    private static void note(String s) {
        try {
            android.content.Context c = QQEnv.appContext();
            if (c == null) return;
            java.io.File f = new java.io.File(c.getFilesDir(), "roster_call.txt");
            if (f.length() > 64 * 1024) return;
            java.io.FileOutputStream o = new java.io.FileOutputStream(f, true);
            try {
                String t = new java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US)
                        .format(new java.util.Date());
                o.write((t + " " + s + "\n").getBytes("UTF-8"));
            } finally {
                o.close();
            }
        } catch (Throwable ignore) {
        }
    }

    public static java.util.Map<String, String> buddies() {
        java.util.Map<String, String> out = new java.util.LinkedHashMap<String, String>();
        try {
            Object svc = QQEnv.buddyService();
            if (svc == null) return out;
            Object list = null;
            for (String wn : new String[]{"getBuddyListFromCache", "getBuddyList",
                    "getAllBuddyList", "getFriendList", "getBuddyListFromDb"}) {
                try {
                    Method m = svc.getClass().getMethod(wn);
                    if (m.getParameterTypes().length != 0) continue;
                    Object r = m.invoke(svc);
                    if (r instanceof java.util.List && !((java.util.List<?>) r).isEmpty()) {
                        list = r;
                        break;
                    }
                } catch (Throwable ignore) {
                }
            }
            if (list == null) return out;
            for (Object b : (java.util.List<?>) list) {
                if (b == null) continue;
                if (b instanceof String) {
                    out.put((String) b, "");
                    continue;
                }
                String uin = str(b, "uin", "friendUin", "uid");
                String nm = str(b, "name", "nick", "remark", "friendName");
                if (uin == null) continue;
                out.put(uin, nm == null ? "" : nm);
            }
        } catch (Throwable t) {
            HookLog.w("QqRoster.buddies 失败 " + t);
            note("buddies EX " + t);
        }
        note("buddies=" + out.size());
        return out;
    }

    private static String str(Object o, String... names) {
        Class<?> c = o.getClass();
        int dep = 0;
        while (c != null && c != Object.class && dep++ < 6) {
            for (String n : names) {
                try {
                    java.lang.reflect.Field f = c.getDeclaredField(n);
                    f.setAccessible(true);
                    Object v = f.get(o);
                    if (v != null && String.valueOf(v).trim().length() > 0) {
                        return String.valueOf(v);
                    }
                } catch (Throwable ignore) {
                }
            }
            c = c.getSuperclass();
        }
        return null;
    }

    public static void probe() {
        StringBuilder sb = new StringBuilder();
        ClassLoader cl = QQEnv.classLoader();
        sb.append("── 好友候选 ──\n");
        dumpAll(sb, cl, BUDDY_CLASSES);
        sb.append("\n── 群候选 ──\n");
        dumpAll(sb, cl, TROOP_CLASSES);
        sb.append("\n── 真取一次群 ──\n");
        try {
            Class<?> impl = HookApi.findClass(cl,
                    "com.tencent.qqnt.troop.impl.TroopListRepoApiImpl");
            if (impl != null) {
                Object repo = null;
                for (String gn : new String[]{"getInstance", "getService"}) {
                    try {
                        Method g = impl.getMethod(gn);
                        g.setAccessible(true);
                        Object v = g.invoke(null);
                        if (v != null) { repo = v; sb.append("实例=").append(gn).append("()\n"); break; }
                    } catch (Throwable ignore) {
                    }
                }
                if (repo == null) {
                    try {
                        java.lang.reflect.Constructor<?> ct = impl.getDeclaredConstructor();
                        ct.setAccessible(true);
                        repo = ct.newInstance();
                        sb.append("实例=无参构造\n");
                    } catch (Throwable t) {
                        sb.append("实例=失败 ").append(t).append('\n');
                    }
                }
                if (repo != null) {
                    for (String wn : new String[]{"getSortedJoinedTroopInfoFromCache",
                            "getTroopListFromCache", "getJoinedTroopInfoFromCache"}) {
                        try {
                            Method m = impl.getMethod(wn);
                            Object r = m.invoke(repo);
                            int n = (r instanceof java.util.List) ? ((java.util.List<?>) r).size() : -1;
                            sb.append(wn).append(" → ").append(n).append('\n');
                            if (r instanceof java.util.List && n > 0) {
                                Object e0 = ((java.util.List<?>) r).get(0);
                                sb.append("  首元素 ").append(e0.getClass().getName()).append('\n');
                                for (java.lang.reflect.Field f : e0.getClass().getFields()) {
                                    try {
                                        sb.append("    f ").append(f.getName()).append('=')
                                          .append(String.valueOf(f.get(e0))).append('\n');
                                    } catch (Throwable ignore) {
                                    }
                                }
                                break;
                            }
                        } catch (Throwable t) {
                            sb.append(wn).append(" → 异常 ").append(t).append('\n');
                        }
                    }
                }
            }
        } catch (Throwable t) {
            sb.append("真取群失败 ").append(t).append('\n');
        }

        sb.append("\n── 真取一次好友 ──\n");
        try {
            Class<?> bs = HookApi.findClass(cl,
                    "com.tencent.qqnt.kernel.api.impl.BuddyService");
            if (bs != null) {
                Object svc = null;
                try {
                    Method g = bs.getMethod("getService");
                    g.setAccessible(true);
                    svc = g.invoke(null);
                } catch (Throwable t) {
                    sb.append("getService 异常 ").append(t).append('\n');
                }
                sb.append("实例=").append(svc == null ? "null" : svc.getClass().getName()).append('\n');
                if (svc != null) {
                    int shown = 0;
                    for (Method m : svc.getClass().getMethods()) {
                        if (shown++ > 40) break;
                        StringBuilder ps = new StringBuilder();
                        for (Class<?> p : m.getParameterTypes()) ps.append(p.getSimpleName()).append(',');
                        sb.append("   ").append(m.getName()).append('(').append(ps)
                          .append(") : ").append(m.getReturnType().getSimpleName()).append('\n');
                    }
                }
            }
        } catch (Throwable t) {
            sb.append("真取好友失败 ").append(t).append('\n');
        }
        try {
            android.content.Context c = QQEnv.appContext();
            if (c != null) {
                java.io.File f = new java.io.File(c.getFilesDir(), "roster_probe.txt");
                java.io.FileOutputStream o = new java.io.FileOutputStream(f, false);
                o.write(sb.toString().getBytes("UTF-8"));
                o.close();
            }
        } catch (Throwable ignore) {
        }
        HookLog.i("名单探测已落盘");
    }

    private static void dumpAll(StringBuilder sb, ClassLoader cl, String[] names) {
        for (String cn : names) {
            Class<?> c = HookApi.findClass(cl, cn);
            sb.append(cn).append(" → ").append(c != null).append('\n');
            if (c == null) continue;
            int n = 0;
            for (Method m : c.getMethods()) {
                if (m.getParameterTypes().length != 0) continue;
                if (m.getReturnType() == void.class) continue;
                if (n++ > 25) break;
                sb.append("   ").append(m.getName())
                  .append(" : ").append(m.getReturnType().getSimpleName()).append('\n');
            }
        }
    }
}
