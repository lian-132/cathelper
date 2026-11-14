package com.cathelperawa.owo.app.feature;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.cathelperawa.owo.app.core.HookApi;
import com.cathelperawa.owo.app.core.HookItem;
import com.cathelperawa.owo.app.core.HookItemBase;
import com.cathelperawa.owo.app.core.HookLog;
import com.cathelperawa.owo.app.util.Refl;
import com.cathelperawa.owo.app.util.SelfCtx;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import de.robv.android.xposed.XC_MethodHook;

@HookItem(name = "显示发送者版本", path = "群管理/消息版本号",
        description = "在对方消息下方显示 TA 的 QQ 客户端版本", order = 92)
public class FeatureSenderVersion extends HookItemBase {

    private static final String CLS_VB =
            "com.tencent.mobileqq.aio.msglist.holder.AIOBubbleMsgItemVB";

    public static final Map<Long, String> VER = new HashMap<Long, String>();

    private static final Map<Long, String> CH_MAP = new HashMap<Long, String>();

    private static final int[] OFFSETS = {0, 1, 2, 4, 5, 6, 11, 16};

    private static String channelName(int d) {
        switch (d) {
            case 0: return null;
            case 1: return "应用宝";
            case 2: return "vivo";
            case 4: return "荣耀";
            case 5: return "小米";
            case 6: return "OPPO";
            case 11: return "豌豆荚";
            case 16: return "应用汇";
            default: return null;
        }
    }

    private static final int VIEW_ID = 0x0C475E01;

    private static boolean dumped = false;

    private static boolean loaded = false;

    private static final String TAGV = "版本号";

    public boolean mainProcessOnly() { return true; }

    @Override
    public String switchKey() {
        return "show_sender_version";
    }

    @Override
    public void entry(ClassLoader cl) {
        hint("群里收到消息时");

        loadTable();

        Class<?> vb = HookApi.findClass(cl, CLS_VB);
        if (vb == null) {
            fail("未找到 AIOBubbleMsgItemVB");
            return;
        }

        Method target = null;
        for (Method m : vb.getDeclaredMethods()) {
            Class<?>[] ps = m.getParameterTypes();
            if (m.getReturnType() == void.class && ps.length == 4
                    && ps[0] == int.class
                    && ps[2] == java.util.List.class
                    && ps[3] == android.os.Bundle.class) {
                target = m;
                break;
            }
        }
        if (target == null) {
            fail("未找到气泡绑定方法");
            return;
        }

        hookAfter(target, new HookApi.Action() {
            @Override
            public void call(XC_MethodHook.MethodHookParam p) {
                try {
                    if (!isEnabled()) return;
                    if (p.args == null || p.args.length < 2) return;
                    final Object msgItem = p.args[1];
                    if (msgItem == null) return;

                    final View root = findViewField(p.thisObject);
                    if (root == null) return;

                    root.post(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                attach(root, msgItem);
                            } catch (Throwable t) {
                                HookLog.e("版本号：attach 失败", t);
                            }
                        }
                    });
                } catch (Throwable ignore) {

                }
            }
        });
        HookLog.i("发送者版本：已挂 " + target.getName() + "，版本表 " + VER.size() + " 条");
    }

    public static String versionOf(Object msgItem) {
        try {
            loadTable();
            Object rec = Refl.call(msgItem, "getMsgRecord");
            if (rec == null) rec = Refl.field(msgItem, "msgRecord");
            if (rec == null) return null;

            String peer = Refl.firstStrOr(rec, "", "peerUid", "peerUin");
            if (peer != null && peer.startsWith("u_")) return null;

            FeatureSenderVersion tmp2 = new FeatureSenderVersion();
            long sid = tmp2.findSubId(rec);
            if (sid <= 0) return null;
            String v = VER.get(sid);
            return (v == null || v.length() == 0) ? ("SubId " + sid) : v;
        } catch (Throwable t) {
            return null;
        }
    }

    public static int verCount() {
        loadTable();
        return VER.size();
    }

    public static String[] allVersions() {
        loadTable();
        java.util.List<Long> keys = new java.util.ArrayList<Long>(VER.keySet());
        java.util.Collections.sort(keys);
        String[] out = new String[keys.size()];
        for (int i = 0; i < keys.size(); i++) {
            long k = keys.get(i);
            String v = VER.get(k);
            out[i] = v + "    [" + k + "]";
        }
        return out;
    }

    public static String lookup(long sid) {
        if (sid <= 0) return null;
        String hit = CH_MAP.get(sid);
        if (hit != null) return hit;
        String exact = VER.get(sid);
        if (exact != null) return exact;
        long bestKey = -1;
        long bestDiff = Long.MAX_VALUE;
        synchronized (VER) {
            for (Map.Entry<Long, String> e : VER.entrySet()) {
                long d = Math.abs(e.getKey() - sid);
                if (d < bestDiff && d <= 40L) {
                    bestDiff = d;
                    bestKey = e.getKey();
                }
            }
        }
        return bestKey > 0 ? VER.get(bestKey) : null;
    }

    public static void loadTableStatic() { loadTable(); }

    private static void loadTable() {
        if (loaded) return;
        synchronized (VER) {
            if (loaded) return;
            try {
                Context host = com.cathelperawa.owo.app.bridge.QQEnv.appContext();
                String apk = SelfCtx.apkPath(host);
                if (apk != null) {
                    ZipFile zf = new ZipFile(apk);
                    try {
                        ZipEntry e = zf.getEntry("assets/sender_version.json");
                        if (e != null) {
                            InputStream in = zf.getInputStream(e);
                            ByteArrayOutputStream bo = new ByteArrayOutputStream();
                            byte[] buf = new byte[8192];
                            int n;
                            while ((n = in.read(buf)) > 0) bo.write(buf, 0, n);
                            in.close();
                            parseTable(bo.toString("UTF-8"));
                        }
                    } finally {
                        zf.close();
                    }
                }
            } catch (Throwable t) {
                HookLog.e("版本号：读表失败", t);
            }
            loaded = true;
        }
    }

    private static void parseTable(String json) throws Exception {
        JSONObject root = new JSONObject(json);
        JSONArray keys = root.names();
        if (keys == null) return;
        for (int i = 0; i < keys.length(); i++) {
            JSONArray arr = root.optJSONArray(keys.getString(i));
            if (arr == null) continue;
            for (int j = 0; j < arr.length(); j++) {
                JSONObject o = arr.optJSONObject(j);
                if (o == null) continue;
                long sid = o.optLong("subid", -1);
                String v = o.optString("version", "");
                if (sid <= 0 || v.length() == 0) continue;
                String client = o.optString("client", "");
                String channel = o.optString("channel", "");

                String label = (client.length() > 0 ? client + " " : "") + v;
                VER.put(sid, label);

                for (int k = 0; k < OFFSETS.length; k++) {
                    int d = OFFSETS[k];
                    String ch = channelName(d);
                    CH_MAP.put(sid + d, ch == null ? label
                            : label + "（" + ch + "渠道，推测）");
                }
                JSONArray alias = o.optJSONArray("aliases");
                if (alias != null) {
                    for (int k = 0; k < alias.length(); k++) {
                        long a = alias.optLong(k, -1);
                        if (a > 0) {
                            VER.put(a, label);
                            long d = a - sid;
                            String ch = (d >= 0 && d < 64) ? channelName((int) d) : null;
                            CH_MAP.put(a, ch == null ? label
                                    : label + "（" + ch + "渠道，推测）");
                        }
                    }
                }
            }
        }
        HookLog.i("版本号：载入 " + VER.size() + " 条映射");
    }

    private void attach(View root, Object msgItem) {
        Object rec = null;
        try {
            rec = Refl.call(msgItem, "getMsgRecord");
            if (rec == null) rec = Refl.field(msgItem, "msgRecord");
        } catch (Throwable ignore) {
        }
        if (rec == null) return;

        String peer = Refl.firstStrOr(rec, "", "peerUid", "peerUin");
        if (peer == null || peer.isEmpty() || peer.startsWith("u_")) return;

        long subId = findSubId(rec);
        if (subId <= 0) return;

        String ver = VER.get(subId);
        if (ver == null || ver.length() == 0) {

            ver = "SubId " + subId;
        }

        show(root, ver);
    }

    private long findSubId(Object rec) {
        String[] names = {"subId", "subid", "subAppId", "subID", "subIdLong",
                "clientSubId", "subIdString", "subIdStr"};

        for (int i = 0; i < names.length; i++) {
            try {
                Object v = Refl.field(rec, names[i]);
                if (v instanceof Number) {
                    long x = ((Number) v).longValue();
                    if (x > 0) return x;
                } else if (v instanceof String) {
                    long x = Long.parseLong(((String) v).trim());
                    if (x > 0) return x;
                }
            } catch (Throwable ignore) {
            }
        }

        long best = -1;
        try {
            Class<?> c = rec.getClass();
            int depth = 0;
            while (c != null && c != Object.class && depth++ < 6) {
                Field[] fs = c.getDeclaredFields();
                for (int i = 0; i < fs.length; i++) {
                    Class<?> t = fs[i].getType();
                    if (t != long.class && t != int.class && t != Long.class
                            && t != Integer.class) continue;
                    try {
                        fs[i].setAccessible(true);
                        Object v = fs[i].get(rec);
                        if (!(v instanceof Number)) continue;
                        long x = ((Number) v).longValue();
                        if (x >= 537000000L && x <= 538999999L) {
                            if (VER.containsKey(x)) return x;
                            if (best < 0) best = x;
                        }
                    } catch (Throwable ignore) {
                    }
                }
                c = c.getSuperclass();
            }
        } catch (Throwable ignore) {
        }

        if (best < 0 && !dumped) dumpRecord(rec);
        return best;
    }

    private void dumpRecord(Object rec) {
        dumped = true;
        StringBuilder sb = new StringBuilder("MsgRecord 字段:\n");
        try {
            Class<?> c = rec.getClass();
            int depth = 0;
            while (c != null && c != Object.class && depth++ < 4) {
                Field[] fs = c.getDeclaredFields();
                for (int i = 0; i < fs.length; i++) {
                    String tn = fs[i].getType().getSimpleName();
                    if (tn.equals("MsgElement") || tn.endsWith("[]")) continue;
                    try {
                        fs[i].setAccessible(true);
                        Object v = fs[i].get(rec);
                        sb.append("  ").append(fs[i].getName()).append(" (").append(tn)
                                .append(") = ").append(String.valueOf(v)).append('\n');
                    } catch (Throwable ignore) {
                    }
                }
                c = c.getSuperclass();
            }
        } catch (Throwable ignore) {
        }
        HookLog.i(sb.toString());
    }

    private void show(View root, String text) {
        try {
            View old = root.findViewById(VIEW_ID);
            if (old instanceof TextView) {
                ((TextView) old).setText(text);
                return;
            }
            Context ctx = root.getContext();
            TextView tv = new TextView(ctx);
            tv.setId(VIEW_ID);
            tv.setText(text);
            tv.setTextSize(9f);
            tv.setTextColor(0x88999999);
            tv.setSingleLine(true);

            if (root instanceof ViewGroup) {
                ViewGroup vg = (ViewGroup) root;
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
                lp.topMargin = 2;
                vg.addView(tv, lp);
            }
        } catch (Throwable t) {
            HookLog.e("版本号：显示失败", t);
        }
    }

    private static View findViewField(Object holder) {
        if (holder == null) return null;
        if (holder instanceof View) return (View) holder;
        Class<?> c = holder.getClass();
        int depth = 0;
        while (c != null && c != Object.class && depth++ < 8) {
            Field[] fs = c.getDeclaredFields();
            for (int i = 0; i < fs.length; i++) {
                if (!View.class.isAssignableFrom(fs[i].getType())) continue;
                try {
                    fs[i].setAccessible(true);
                    Object o = fs[i].get(holder);
                    if (o instanceof View) return (View) o;
                } catch (Throwable ignore) {
                }
            }
            c = c.getSuperclass();
        }
        return null;
    }
}
