package com.cathelperawa.owo.app.feature;

import com.cathelperawa.owo.app.core.DependsOn;
import com.cathelperawa.owo.app.core.Bg;
import android.content.Context;
import android.os.Bundle;

import com.cathelperawa.owo.app.MsgStore;
import com.cathelperawa.owo.app.bridge.CatToast;
import com.cathelperawa.owo.app.bridge.GhostChat;
import com.cathelperawa.owo.app.bridge.QQEnv;
import com.cathelperawa.owo.app.bridge.TimCookie;
import com.cathelperawa.owo.app.bridge.TimNet;
import com.cathelperawa.owo.app.bridge.TimServlet;
import com.cathelperawa.owo.app.bridge.TimWallet;
import com.cathelperawa.owo.app.bridge.VoiceSilk;
import com.cathelperawa.owo.app.bridge.VoiceUpload;
import com.cathelperawa.owo.app.core.HookApi;
import com.cathelperawa.owo.app.core.HookConfig;
import com.cathelperawa.owo.app.core.HookItem;
import com.cathelperawa.owo.app.core.HookItemBase;
import com.cathelperawa.owo.app.core.HookLog;
import com.cathelperawa.owo.app.util.Refl;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@DependsOn({"bridge.element"})
@HookItem(name = "红包", path = "聊天与消息/红包",
        description = "自动领取红包 / 长按菜单打开红包", order = 50)
public class FeatureRedPacket extends HookItemBase {

    
    public static final String K_MANUAL   = "rp_manual";
    public static final String K_AUTO     = "rp_auto";
    
    public static final String K_AVERAGE  = "rp_average";   
    public static final String K_KEYWORDS = "rp_keywords";  
    public static final String K_REPLYS   = "rp_replys";    
    public static final String K_DELAY    = "rp_delay";     
    public static final String K_GROUPONLY= "rp_grouponly";

    public static final String TITLE = "开红包";

    private static final String SALT_PREGRAB = "hb_pre_grapver=2.0&chv=3";
    private static final String CGI_PREGRAB  = "hb_pre_grap";
    private static final char[] HEXC = "0123456789abcdef".toCharArray();
    private static final String CMD_SSO      = "trpc.qqhb.qqhb_proxy.Handler.sso_handle";
    private static final String URL_NAGRAB   =
            "https://mqq.tenpay.com/cgi-bin/hongbao/qpay_hb_na_grap.cgi?";
    private static final String CGI_NAGRAB   = "qpay_hb_na_grap";

    private static final Random RND = new Random();

    private static volatile boolean QFUN_HOOKED = false;
    private static volatile ClassLoader qfunClRef = null;

    @Override public String switchKey() { return K_AUTO; }

    

    public static boolean manualOn(Context c) {
        return c != null && HookConfig.getBoolean(c, K_MANUAL, false);
    }
    public static boolean autoOn(Context c) {
        return c != null && HookConfig.getBoolean(c, K_AUTO, false);
    }

    private static int avgThreshold(Context c) {
        try { return Integer.parseInt(HookConfig.get(c, K_AVERAGE, "0")); }
        catch (Throwable t) { return 0; }
    }
    private static int delayMax(Context c) {
        try {
            int v = Integer.parseInt(HookConfig.get(c, K_DELAY, "0"));
            return v < 0 ? 0 : Math.min(v, 10000);
        } catch (Throwable t) { return 0; }
    }
    private static List<String> commaList(Context c, String key) {
        List<String> out = new ArrayList<String>();
        String s = HookConfig.get(c, key, "");
        if (s == null || s.isEmpty()) return out;
        for (String p : s.split(",")) {
            String t = p.trim();
            if (!t.isEmpty()) out.add(t);
        }
        return out;
    }

    

    private static String brief(String s) {
        if (s == null || s.isEmpty()) return "✗ 空";
        if (s.length() <= 8) return "✓ " + s;
        return "✓ " + s.substring(0, 6) + "…(" + s.length() + ")";
    }

    public static String probe() {
        StringBuilder sb = new StringBuilder();
        sb.append("【红包底座探针】\n");
        sb.append("宿主: ").append(QQEnv.currentUin() == null ? "未登录" : "已登录").append("\n");
        try {
            String n = TimServlet.servletClassName();
            sb.append("Servlet : ").append(n == null ? "✗ 未找到" : "✓ " + n).append("\n");
        } catch (Throwable t) { sb.append("Servlet : ✗ ").append(t).append("\n"); }
        try { sb.append("pskey   : ").append(brief(TimCookie.getPskey("tenpay.com"))).append("\n"); }
        catch (Throwable t) { sb.append("pskey   : ✗ ").append(t).append("\n"); }
        try { sb.append("hexGuid : ").append(brief(TimCookie.hexGuid())).append("\n"); }
        catch (Throwable t) { sb.append("hexGuid : ✗ ").append(t).append("\n"); }
        try { sb.append("walletKey: ").append(brief(TimWallet.walletPsKey(URL_NAGRAB))).append("\n"); }
        catch (Throwable t) { sb.append("walletKey: ✗ ").append(t).append("\n"); }

        sb.append("\n【红包判定统计】\n");
        sb.append("抢到   : ").append(STAT_GOT).append(" 次\n");
        sb.append("已领过 : ").append(STAT_ALREADY).append(" 次\n");
        sb.append("失败   : ").append(STAT_FAIL).append(" 次\n");
        sb.append("已去重 : ").append(GRABBED_BILLS.size()).append(" 个红包\n");
        sb.append("\n（详情见 cat_msg.txt 的「[红包判定]」行）");
        String s = sb.toString();
        HookLog.i("探针结果:\n" + s);
        try { MsgStore.add(s); } catch (Throwable ignore) {}
        return s;
    }

    private static volatile int STAT_GOT = 0;
    private static volatile int STAT_ALREADY = 0;
    private static volatile int STAT_FAIL = 0;

    

    @Override
    public void entry(ClassLoader cl) {
        hint("有人发红包时");
        try {
            Class<?> c = HookApi.findClass(cl,
                    "com.tencent.qqnt.msg.MsgService$c",
                    "com.tencent.qqnt.msg.MsgService$a");
            if (c == null) { HookLog.w("红包：未找到 MsgService$c/$a"); return; }
            int n = 0;
            for (final Method m : c.getDeclaredMethods()) {
                if (!"onRecvMsg".equals(m.getName())) continue;
                if (m.getParameterTypes().length != 1) continue;
                hookBefore(m, new HookApi.Action() {
                    @Override
                    public void call(de.robv.android.xposed.XC_MethodHook.MethodHookParam p) {
                        try {
                            Object a0 = (p.args != null && p.args.length > 0) ? p.args[0] : null;
                            if (a0 instanceof List) {

                                onRecords((List<?>) a0);
                            }
                        } catch (Throwable t) { HookLog.e("红包 onRecvMsg 异常", t); }
                    }
                });
                n++;
            }
            HookLog.i("红包：已挂 onRecvMsg × " + n);
            installTrace(cl);
            installVoiceProbe(cl);
            
            final ClassLoader fcl = cl;
            Bg.run(new Runnable() {
                @Override public void run() {
                    for (int i = 0; i < 6; i++) {
                        try { Thread.sleep(10000L); } catch (Throwable ignore) {}
                        try {
                            ClassLoader qcl = probeQFunOnce(fcl);
                            if (qcl != null) {
                                HookLog.i("QFun 探针：延迟第 " + (i + 1) + " 次成功挂载");
                                try { MsgStore.add("[QFun探针] 延迟第 " + (i + 1)
                                        + " 次挂载成功"); } catch (Throwable ignore) {}
                                return;
                            }
                        } catch (Throwable ignore) {}
                    }
                    HookLog.w("QFun 探针：延迟重试 6 次仍未找到 QFun");
                }
            }, "cat-qfun-probe");
            try { GatewayProbe.install(cl); } catch (Throwable t) { HookLog.e("挂 GatewayProbe 失败", t); }
            
            try { ArkProbe.install(cl); } catch (Throwable t) { HookLog.e("挂 ArkProbe 失败", t); }
            
            try { KernelRedBagProbe.install(cl); }
            catch (Throwable t) { HookLog.e("挂 KernelRedBagProbe 失败", t); }

            
            Bg.run(new Runnable() {
                @Override public void run() {
                    for (int i = 0; i < 4; i++) {
                        try {
                            Thread.sleep(i == 0 ? 3000L : 20000L);
                            com.cathelperawa.owo.app.bridge.TimWallet.prewarm();
                            com.cathelperawa.owo.app.bridge.TimNet.prewarm();
                            HookLog.i("预热: servlet=" + com.cathelperawa.owo.app.bridge.TimServlet.servletClassName());
                        } catch (Throwable t) { HookLog.w("预热失败: " + t); }
                    }
                }
            }, "cat-rp-prewarm");
        } catch (Throwable t) {
            HookLog.e("红包 entry 失败", t);
        }
    }

    

    private void installTrace(ClassLoader cl) {
        
        trace(cl, "com.tencent.mobileqq.qwallet.e.f", "onSend", 2);
        trace(cl, "com.tencent.mobileqq.qwallet.e.f", "onReceive", 2);
        
        try {
            Class<?> rt = HookApi.findClass(cl, "mqq.app.AppRuntime");
            if (rt != null) {
                for (final Method m : rt.getDeclaredMethods()) {
                    if (!"startServlet".equals(m.getName())) continue;
                    hookBefore(m, new HookApi.Action() {
                        @Override public void call(
                                de.robv.android.xposed.XC_MethodHook.MethodHookParam p) {
                            try {
                                Object it = (p.args == null || p.args.length == 0) ? null : p.args[0];
                                if (!(it instanceof android.content.Intent)) return;
                                android.content.Intent i = (android.content.Intent) it;
                                String cmd = i.getStringExtra("cmd");
                                byte[] d = i.getByteArrayExtra("data");

                                if (HookConfig.getBoolean(ctx, "rp_debug_sso", false)) {
                                    HookLog.i("[SSO] startServlet cmd=" + cmd
                                            + " len=" + (d == null ? -1 : d.length)
                                            + " cls=" + i.getClass().getName());
                                }
                            } catch (Throwable ignore) {}
                        }
                    });
                }
            }
        } catch (Throwable t) { HookLog.e("挂 startServlet 失败", t); }
        HookLog.i("红包链路监听：已挂");
    }

    private void installVoiceProbe(ClassLoader cl) {

        
        
        try {
            Class<?> c = HookApi.findClass(cl, "com.tencent.mobileqq.utils.SilkCodecWrapper");
            if (c == null) { HookLog.w("语音探针: 找不到 SilkCodecWrapper"); }
            else {
                int n = 0;
                for (final Method m : c.getDeclaredMethods()) {
                    if (m.isSynthetic() || m.isBridge()) continue;
                    String nm = m.getName();
                    if (!("a".equals(nm) || "read".equals(nm) || "encode".equals(nm)
                            || "b".equals(nm) || "release".equals(nm) || "g".equals(nm))) continue;
                    try {
                        hookBefore(m, new HookApi.Action() {
                            @Override public void call(
                                    de.robv.android.xposed.XC_MethodHook.MethodHookParam p) {
                                try {
                                    StringBuilder sb = new StringBuilder("[SILK] ")
                                            .append(m.getName()).append("(");
                                    Object[] as = p.args;
                                    if (as != null) {
                                        for (int i = 0; i < as.length; i++) {
                                            if (i > 0) sb.append(", ");
                                            Object a = as[i];
                                            if (a instanceof byte[]) {
                                                byte[] bb = (byte[]) a;
                                                sb.append("byte[").append(bb.length).append("]");
                                                if (bb.length > 0 && bb.length <= 16) {
                                                    sb.append('=');
                                                    for (byte x : bb) {
                                                        sb.append(String.format("%02x", x));
                                                    }
                                                }
                                            } else if (a instanceof Long) {
                                                sb.append("long=0x")
                                                        .append(Long.toHexString((Long) a));
                                            } else {
                                                sb.append(String.valueOf(a));
                                            }
                                        }
                                    }
                                    sb.append(")");
                                    Object r = null;
                                    if ("b".equals(m.getName()) || "read".equals(m.getName())
                                            || "encode".equals(m.getName())
                                            || "a".equals(m.getName())) {
                                        try {
                                            Field fh = m.getDeclaringClass()
                                                    .getDeclaredField("h");
                                            fh.setAccessible(true);
                                            byte[] hb = (byte[]) fh.get(p.thisObject);
                                            if (hb != null) {
                                                sb.append(" → h[0..3]=");
                                                for (int i = 0; i < Math.min(4, hb.length); i++) {
                                                    sb.append(String.format("%02x", hb[i]));
                                                }
                                            }
                                        } catch (Throwable ignore) {}
                                    }
                                    MsgStore.add(sb.toString());
                                } catch (Throwable ignore) {}
                            }
                        });
                        n++;
                    } catch (Throwable ignore) {}
                }
                HookLog.i("语音探针: SilkCodecWrapper 挂 " + n + " 个方法");
            }
        } catch (Throwable t) { HookLog.e("语音探针 SilkCodecWrapper 失败", t); }

        
        
        try {
            Class<?> c = HookApi.findClass(cl, "com.tencent.qqnt.qwallet.bigdata.BigDataUploader");
            if (c == null) HookLog.w("语音探针: 找不到 BigDataUploader");
            else {
                for (final Method m : c.getDeclaredMethods()) {
                    if (!"e".equals(m.getName())) continue;
                    if (m.getParameterTypes().length != 2) continue;
                    hookBefore(m, new HookApi.Action() {
                        @Override public void call(
                                de.robv.android.xposed.XC_MethodHook.MethodHookParam p) {
                            try {
                                String path = (p.args[0] == null) ? "null" : p.args[0].toString();
                                byte[] b = (byte[]) p.args[1];
                                java.io.File f = (p.args[0] == null) ? null
                                        : new java.io.File(p.args[0].toString());
                                String json = (b == null) ? "null"
                                        : new String(b, "UTF-8");
                                String hdr = "";
                                if (f != null && f.exists() && f.length() >= 10) {
                                    try (java.io.FileInputStream in = new java.io.FileInputStream(f)) {
                                        byte[] h10 = new byte[10];
                                        int r = in.read(h10);
                                        StringBuilder h = new StringBuilder();
                                        for (int i = 0; i < r; i++) {
                                            h.append(HEXC[(h10[i] & 0xFF) >>> 4])
                                                    .append(HEXC[h10[i] & 0xFF & 0xF]);
                                        }
                                        hdr = " 头10字节=" + h;
                                    } catch (Throwable ignore) {}
                                }
                                long len = (f != null && f.exists()) ? f.length() : -1;
                                MsgStore.add("[语音上传★] BigDataUploader.e\n"
                                        + "  filePath=" + path + "\n"
                                        + "  文件大小=" + len + hdr + "\n"
                                        + "  reqExtendInfo(JSON)=" + json);
                            } catch (Throwable t) {
                                MsgStore.add("[语音上传★] 探针异常 " + t);
                            }
                        }
                    });
                }
                
                Class<?> ab = HookApi.findClass(cl,
                        "com.tencent.qqnt.qwallet.bigdata.BigDataUploader$a");
                if (ab != null) {
                    for (final Method m : ab.getDeclaredMethods()) {
                        if (!"a".equals(m.getName())) continue;
                        if (m.getParameterTypes().length != 1) continue;
                        hookBefore(m, new HookApi.Action() {
                            @Override public void call(
                                    de.robv.android.xposed.XC_MethodHook.MethodHookParam p) {
                                try {
                                    byte[] b = (byte[]) p.args[0];
                                    MsgStore.add("[语音上传★] 回调 a() = "
                                            + (b == null ? "null" : new String(b, "UTF-8")));
                                } catch (Throwable ignore) {}
                            }
                        });
                    }
                }
            }
        } catch (Throwable t) { HookLog.e("语音探针 BigDataUploader 失败", t); }

        try {
            Class<?> vm = HookApi.findClass(cl,
                    "com.tencent.qqnt.qwallet.grab.GrabVoiceHbViewModel");
            if (vm == null) HookLog.w("语音探针: 找不到 GrabVoiceHbViewModel");
            else {
                
                for (final Method m : vm.getDeclaredMethods()) {
                    if (!"l0".equals(m.getName())) continue;
                    hookBefore(m, new HookApi.Action() {
                        @Override public void call(
                                de.robv.android.xposed.XC_MethodHook.MethodHookParam p) {
                            try {
                                Object self = p.thisObject;
                                StringBuilder sb = new StringBuilder();
                                sb.append("[原生语音★] GrabVoiceHbViewModel.l0\n");
                                sb.append("  filePath=").append(p.args[0]).append("\n");
                                
                                for (java.lang.reflect.Field f : self.getClass().getDeclaredFields()) {
                                    if (f.getType() != String.class) continue;
                                    try {
                                        f.setAccessible(true);
                                        Object v = f.get(self);
                                        if (v != null) sb.append("  ").append(f.getName())
                                                .append("=").append(v).append("\n");
                                    } catch (Throwable ignore) {}
                                }
                                MsgStore.add(sb.toString());
                            } catch (Throwable t) {
                                MsgStore.add("[原生语音★] l0 探针异常 " + t);
                            }
                        }
                    });
                }
                
            }
        } catch (Throwable ignore) {}

        try {
            Class<?> rec = HookApi.findClass(cl, "com.tencent.mobileqq.ptt.IQQRecorderUtils");
            if (rec != null) {
                HookLog.i("语音探针: 发现 IQQRecorderUtils（原生录音器，我们不使用）");
            }
        } catch (Throwable ignore) {}

        
        probeQFun(cl);

        HookLog.i("语音探针：已挂（结果见 cat_msg.txt）");
        try {
            MsgStore.add("[语音探针] 已加载 —— 语音红包将走【免上传】路线");
        } catch (Throwable ignore) {}
    }

    private void probeQFun(final ClassLoader hostCl) {
        probeQFunOnce(hostCl);
    }

    private ClassLoader probeQFunOnce(final ClassLoader hostCl) {
        if (QFUN_HOOKED) return qfunClRef;   
        
        ClassLoader qfunCl = null;

        String[] entryCandidates = {
                "me.yxp.qfun.LauncherActivity",
                "me.yxp.qfun.core.Entry",
                "me.yxp.qfun.XposedEntry",
                "me.yxp.qfun.loader.modern.ModernHookEntry",
                "me.yxp.qfun.loader.legacy.LegacyHookEntry",
        };
        for (String cn : entryCandidates) {
            try {
                Class<?> c = HookApi.findClass(hostCl, cn);
                if (c != null) {
                    qfunCl = c.getClassLoader();
                    try { MsgStore.add("[QFun探针] 命中入口 " + cn + " → " + qfunCl); }
                    catch (Throwable ignore) {}
                    break;
                }
            } catch (Throwable ignore) {}
        }

        if (qfunCl == null) {
            try {
                for (ClassLoader l : loadedLoaders()) {
                    if (l == null) continue;
                    try {
                        Class.forName("me.yxp.qfun.utils.qq.AudioConverterUtil", false, l);
                        qfunCl = l;
                        try { MsgStore.add("[QFun探针] 扫到 ClassLoader = " + l); }
                        catch (Throwable ignore) {}
                        break;
                    } catch (Throwable ignore) {}
                }
            } catch (Throwable ignore) {}
        }

        if (qfunCl == null) {
            try {
                qfunCl = findLoaderByDex(hostCl, "me.yxp.qfun");
            } catch (Throwable ignore) {}
        }

        if (qfunCl == null) {
            return null;
        }
        final ClassLoader qcl = qfunCl;
        try { MsgStore.add("[QFun探针] ClassLoader = " + qcl); } catch (Throwable ignore) {}

        hookByName(qcl, "me.yxp.qfun.utils.net.HttpUtils", "downloadSuspend",
                new String[]{"url", "path"}, "[QFun探针] 下载");

        hookByName(qcl, "me.yxp.qfun.utils.qq.AudioConverterUtil", "convertMp3ApiToSilk",
                new String[]{"url", "outPath"}, "[QFun探针] mp3→silk");

        hookByName(qcl, "me.yxp.qfun.utils.qq.AudioConverterUtil", "convertAndWriteSilkFile",
                new String[]{"pcm", "outPath"}, "[QFun探针] 写SILK");

        hookByName(qcl, "me.yxp.qfun.hook.redpacket.RedPacketHelper", "handleVoiceRedPacket",
                new String[]{"ctx", "d"}, "[QFun探针] 语音主流程");

        hookByName(qcl, "me.yxp.qfun.hook.redpacket.RedPacketHelper", "uploadVoice",
                new String[]{"filePath", "jsonBytes"}, "[QFun探针] QFun上传");

        try {
            Class<?> rph = Class.forName(
                    "me.yxp.qfun.hook.redpacket.RedPacketHelper", false, qcl);
            for (final Method m : rph.getDeclaredMethods()) {
                if (!"finalGrab".equals(m.getName())) continue;
                hookBefore(m, new HookApi.Action() {
                    @Override public void call(
                            de.robv.android.xposed.XC_MethodHook.MethodHookParam p) {
                        try {
                            Object ctx = (p.args != null && p.args.length > 0) ? p.args[0] : null;
                            Object extra = (p.args != null && p.args.length > 1) ? p.args[1] : null;
                            StringBuilder sb = new StringBuilder();
                            sb.append("[QFun探针] finalGrab\n");
                            sb.append("  extraMap = ")
                                    .append(extra == null ? "★★ null ★★" : extra.toString())
                                    .append("\n");
                            if (ctx != null) {
                                sb.append("  ctx.channel = ").append(callInt(ctx, "getChannel"));
                                sb.append("  ctx.title = ").append(callStr(ctx, "getTitle"));
                                sb.append("  ctx.listId = ").append(callStr(ctx, "getListId"));
                            }
                            MsgStore.add(sb.toString());
                        } catch (Throwable t) {
                            MsgStore.add("[QFun探针] finalGrab 异常 " + t);
                        }
                    }
                });
            }
        } catch (Throwable t) { HookLog.w("QFun 探针 finalGrab 失败 " + t); }

        try {
            Class<?> rph = Class.forName(
                    "me.yxp.qfun.hook.redpacket.RedPacketHelper", false, qcl);
            for (final Method m : rph.getDeclaredMethods()) {
                if (!"startGrab".equals(m.getName())) continue;
                hookBefore(m, new HookApi.Action() {
                    @Override public void call(
                            de.robv.android.xposed.XC_MethodHook.MethodHookParam p) {
                        try {
                            Object ctx = (p.args != null && p.args.length > 0) ? p.args[0] : null;
                            MsgStore.add("[QFun探针] startGrab channel="
                                    + (ctx == null ? "?" : String.valueOf(callInt(ctx, "getChannel")))
                                    + " title=" + (ctx == null ? "?" : callStr(ctx, "getTitle"))
                                    + " listId=" + (ctx == null ? "?" : callStr(ctx, "getListId")));
                        } catch (Throwable ignore) {}
                    }
                });
            }
        } catch (Throwable t) { HookLog.w("QFun 探针 startGrab 失败 " + t); }

        HookLog.i("QFun 探针：已挂");
        QFUN_HOOKED = true;
        qfunClRef = qcl;
        return qcl;
    }

    private void hookByName(final ClassLoader cl, final String clsName, final String method,
                            final String[] argNames, final String tag) {
        try {
            Class<?> c = Class.forName(clsName, false, cl);
            if (c == null) { HookLog.w(tag + " 类不存在"); return; }
            int n = 0;
            for (final Method m : c.getDeclaredMethods()) {
                if (!m.getName().equals(method)) continue;
                hookBefore(m, new HookApi.Action() {
                    @Override public void call(
                            de.robv.android.xposed.XC_MethodHook.MethodHookParam p) {
                        try {
                            StringBuilder sb = new StringBuilder();
                            sb.append(tag).append(" ").append(method).append("\n");
                            int cnt = (p.args == null) ? 0 : p.args.length;
                            for (int i = 0; i < cnt; i++) {
                                Object a = p.args[i];
                                String nm = (argNames != null && i < argNames.length)
                                        ? argNames[i] : ("arg" + i);
                                if (a instanceof byte[]) {
                                    byte[] b = (byte[]) a;
                                    sb.append("  ").append(nm).append(" = [byte ")
                                            .append(b.length).append("] ");
                                    int lim = Math.min(b.length, 32);
                                    for (int k = 0; k < lim; k++) {
                                        sb.append(HEXC[(b[k] & 0xFF) >>> 4])
                                                .append(HEXC[b[k] & 0xF]);
                                    }
                                    
                                    try {
                                        sb.append("  utf8=")
                                                .append(new String(b, "UTF-8"));
                                    } catch (Throwable ignore) {}
                                } else if (a instanceof java.io.File) {
                                    java.io.File f = (java.io.File) a;
                                    sb.append("  ").append(nm).append(" = ").append(f)
                                            .append(" 存在=").append(f.exists())
                                            .append(" 大小=").append(f.length());
                                } else if (a instanceof String) {
                                    sb.append("  ").append(nm).append(" = ").append((String) a);
                                } else {
                                    sb.append("  ").append(nm).append(" = ").append(a);
                                }
                                sb.append("\n");
                            }
                            MsgStore.add(sb.toString());
                        } catch (Throwable t) {
                            HookLog.e(tag + " 探针异常", t);
                        }
                    }
                });
                n++;
            }
            if (n > 0) HookLog.i(tag + " 已挂 × " + n);
            else HookLog.w(tag + " 没找到方法 " + method);
        } catch (Throwable t) { HookLog.e(tag + " 挂载失败", t); }
    }

    private static int callInt(Object o, String method) {
        try {
            Method m = o.getClass().getMethod(method);
            Object r = m.invoke(o);
            return (r instanceof Number) ? ((Number) r).intValue() : -1;
        } catch (Throwable t) { return -1; }
    }

    private static String callStr(Object o, String method) {
        try {
            Method m = o.getClass().getMethod(method);
            Object r = m.invoke(o);
            return r == null ? "null" : r.toString();
        } catch (Throwable t) { return "?"; }
    }

    @SuppressWarnings("unchecked")
    private static java.util.List<ClassLoader> loadedLoaders() {
        java.util.List<ClassLoader> out = new java.util.ArrayList<ClassLoader>();
        try {
            Class<?> clc = Class.forName("java.lang.ClassLoader");
            Method m = clc.getDeclaredMethod("getLoadedClassLoaders", new Class[]{});
            m.setAccessible(true);
            Object r = m.invoke(null);
            if (r instanceof java.util.List) {
                for (Object o : (java.util.List<Object>) r) {
                    if (o instanceof ClassLoader) out.add((ClassLoader) o);
                }
            }
        } catch (Throwable ignore) {}
        
        try {
            java.util.Map<Thread, StackTraceElement[]> all = Thread.getAllStackTraces();
            for (Thread t : all.keySet()) {
                try {
                    ClassLoader l = t.getContextClassLoader();
                    if (l != null && !out.contains(l)) out.add(l);
                } catch (Throwable ignore) {}
            }
        } catch (Throwable ignore) {}
        
        try {
            ClassLoader l = Thread.currentThread().getContextClassLoader();
            if (l != null && !out.contains(l)) out.add(l);
            l = ClassLoader.getSystemClassLoader();
            if (l != null && !out.contains(l)) out.add(l);
        } catch (Throwable ignore) {}
        return out;
    }

    private static ClassLoader findLoaderByDex(ClassLoader base, String keyword) {
        for (ClassLoader l : loadedLoaders()) {
            if (l == null) continue;
            try {
                java.lang.reflect.Field pf = null;
                Class<?> c = l.getClass();
                while (c != null && pf == null) {
                    try { pf = c.getDeclaredField("pathList"); }
                    catch (Throwable ignore) { c = c.getSuperclass(); }
                }
                if (pf == null) continue;
                pf.setAccessible(true);
                Object pathList = pf.get(l);
                if (pathList == null) continue;
                java.lang.reflect.Field ef = pathList.getClass().getDeclaredField("dexElements");
                ef.setAccessible(true);
                Object arr = ef.get(pathList);
                if (!(arr instanceof Object[])) continue;
                for (Object el : (Object[]) arr) {
                    if (el == null) continue;
                    String path = null;
                    try {
                        java.lang.reflect.Field f = el.getClass().getDeclaredField("path");
                        f.setAccessible(true);
                        path = String.valueOf(f.get(el));
                    } catch (Throwable ignore) {
                        try {
                            java.lang.reflect.Field f = el.getClass().getDeclaredField("file");
                            f.setAccessible(true);
                            path = String.valueOf(f.get(el));
                        } catch (Throwable ignore2) {}
                    }
                    if (path != null && path.contains(keyword)) {
                        try { MsgStore.add("[QFun探针] dex路径命中: " + path
                                + " → loader=" + l); } catch (Throwable ignore) {}
                        return l;
                    }
                }
            } catch (Throwable ignore) {}
        }
        return null;
    }

    private void trace(ClassLoader cl, String clsName, String method, int argc) {
        try {
            Class<?> c = HookApi.findClass(cl, clsName);
            if (c == null) { HookLog.w("链路: 类不存在 " + clsName); return; }
            int n = 0;
            for (final Method m : c.getDeclaredMethods()) {
                if (!m.getName().equals(method)) continue;
                if (argc >= 0 && m.getParameterTypes().length != argc) continue;
                hookBefore(m, new HookApi.Action() {
                    @Override public void call(
                            de.robv.android.xposed.XC_MethodHook.MethodHookParam p) {
                        try {
                            Object it = (p.args != null && p.args.length > 0) ? p.args[0] : null;
                            String extra = "";
                            byte[] d = null;
                            String c = null;
                            if (it instanceof android.content.Intent) {
                                android.content.Intent i = (android.content.Intent) it;
                                d = i.getByteArrayExtra("data");
                                c = i.getStringExtra("cmd");
                                extra = " cmd=" + c + " len=" + (d == null ? -1 : d.length);
                            } else {
                                extra = " [arg0=" + (it == null ? "null"
                                        : it.getClass().getName()) + "]";
                            }
                            String hex = "";
                            if (d != null && c != null && c.contains("qqhb")) {
                                StringBuilder h = new StringBuilder();
                                int lim = Math.min(d.length, 300);
                                for (int k = 0; k < lim; k++) {
                                    int bi = d[k] & 0xFF;
                                    h.append(HEXC[bi >>> 4]).append(HEXC[bi & 0xF]);
                                }
                                hex = "\nHEX=" + h;
                                try {
                                    com.cathelperawa.owo.app.MsgStore.add(
                                            "HEXDUMP " + m.getName() + " " + c + " " + h);
                                } catch (Throwable ignore) {
                                }
                            }
                            HookLog.i("[链路] " + clsName.substring(clsName.lastIndexOf('.') + 1)
                                    + "#" + m.getName() + extra + hex);
                        } catch (Throwable t) {
                            HookLog.e("链路 trace 异常", t);
                        }
                    }
                });
                n++;
            }
            if (n > 0) HookLog.i("链路: 已挂 " + clsName + "#" + method + " × " + n);
        } catch (Throwable t) { HookLog.e("链路挂载失败", t); }
    }

    

    @Override
    protected void check(com.cathelperawa.owo.app.core.SelfCheckResult.Checker c) {
        Class<?> svc = c.cls("MsgService 内部类",
                "com.tencent.qqnt.msg.MsgService$c",
                "com.tencent.qqnt.msg.MsgService$a");
        c.method("onRecvMsg", svc, "onRecvMsg");
        c.assertThat("红包命令常量已配置",
                CMD_SSO != null && CMD_SSO.length() > 0, CMD_SSO);
    }

    private void onRecords(List<?> records) {
        if (ctx == null || !autoOn(ctx)) return;
        if (records == null || records.isEmpty()) return;
        for (Object rec : records) {
            try { handleRecord(rec); }
            catch (Throwable t) { HookLog.e("红包 handleRecord 异常", t); }
        }
    }

    private void handleRecord(Object record) {
        if (record == null) return;
        Object wallet = walletOf(record);
        if (wallet == null) return;

        final String billNo  = str(wallet, "billNo");
        final String authkey = str(wallet, "authkey");
        if (billNo == null || billNo.isEmpty()) return;
        
        fire("识别红包");
        tickStart();
        tick("识别到红包 billNo=" + billNo
                + (cPushAt > 0 ? " 距最近一次推送=" + (cT0 - cPushAt) + "ms" : ""));

        if (toBool(Refl.field(wallet, "isOpened"))) return;

        if (isGrabbed(billNo)) {
            HookLog.i("红包跳过: billNo=" + billNo + " 已处理过（防重复领取）");
            return;
        }

        int chatType = Refl.i32(Refl.field(record, "chatType"), 0);
        if (HookConfig.getBoolean(ctx, K_GROUPONLY, true) && chatType != 2) return;

        
        if (chatType == 2) {
            String g = Refl.str(Refl.field(record, "peerUin"));
            if (g != null && !g.isEmpty() && !g.startsWith("u_")) {
                com.cathelperawa.owo.app.core.RpGroupRules.noteSeen(ctx, g, null);
                if (!com.cathelperawa.owo.app.core.RpGroupRules.isAllowed(ctx, g)) {
                    HookLog.i("红包跳过: 群 " + g + " 已关闭自动领取");
                    return;
                }
            }
        }

        String title = titleOf(wallet);
        for (String kw : commaList(ctx, K_KEYWORDS)) {
            if (title != null && title.contains(kw)) {
                HookLog.i("红包跳过: " + title + " 命中「" + kw + "」");
                return;
            }
        }

        int dmax = delayMax(ctx);
        if (dmax > 0) { try { Thread.sleep(RND.nextInt(dmax)); } catch (Throwable ignore) {} }

        grab(record, wallet);
    }

    

    public static void grabFromMsgItem(Object msgItem) {
        grabResult(msgItem);   
    }

    public static com.cathelperawa.owo.app.model.CatResult grabResult(Object msgItem) {
        try {
            try { MsgStore.add("[语音诊断] 长按触发 grabFromMsgItem"); } catch (Throwable ignore) {}
            Object record = recordOf(msgItem);
            if (record == null) {
                toast("这条消息本喵没抓到喵…");
                return com.cathelperawa.owo.app.model.CatResult.badArgs("没拿到这条消息");
            }
            Object wallet = walletOf(record);
            if (wallet == null) {
                toast("这条不是红包啦喵～");
                return com.cathelperawa.owo.app.model.CatResult.badArgs("这不是红包消息");
            }
            try {
                MsgStore.add("[语音诊断] wallet 找到: 类=" + wallet.getClass().getName()
                        + " billNo=" + str(wallet, "billNo")
                        + " redChannel=" + Refl.field(wallet, "redChannel"));
            } catch (Throwable ignore) {}
            String billNo = str(wallet, "billNo");
            if (billNo == null || billNo.isEmpty()) {
                toast("红包信息缺了点喵…");
                return com.cathelperawa.owo.app.model.CatResult.badArgs("红包信息不全");
            }
            if (toBool(Refl.field(wallet, "isOpened"))) {
                toast("这个红包已经领过啦喵～");
                return com.cathelperawa.owo.app.model.CatResult.rejected("红包已经领过了");
            }

            if (isAlreadyOpened(wallet)) {
                HookLog.w("红包预检命中（仅记录，不拦截）: billNo=" + billNo);
            }

            toast("本喵来拆红包咯～");
            grab(record, wallet);
            
            return com.cathelperawa.owo.app.model.CatResult.ok(
                    com.cathelperawa.owo.app.model.CatChannel.HB_NATIVE,
                    "已发起，等回包" );
        } catch (Throwable t) {
            HookLog.e("手动开红包失败", t);
            toast("红包打不开喵…（细节看日志就好）");
            return com.cathelperawa.owo.app.model.CatResult.fail(
                    com.cathelperawa.owo.app.model.CatResult.Fail.UNKNOWN, "开红包异常", t);
        }
    }

    

    private static volatile String PENDING = null;   

    

    
    private static volatile long cT0 = 0L;
    
    private static volatile long cTLast = 0L;

    

    private static volatile int FAST_STATE = -1;

    static volatile long cPushAt = 0L;

    public static void notePushTime(long t) { cPushAt = t; }
    
    private static volatile String FAST_DONE_BILL = null;

    private static void tick(String stage) {
        long now = System.currentTimeMillis();
        long total = (cT0 == 0L) ? 0L : (now - cT0);
        long delta = (cTLast == 0L) ? 0L : (now - cTLast);
        cTLast = now;
        HookLog.i("[红包耗时] " + stage + " 累计=" + total + "ms 本步=" + delta + "ms");
        try {
            MsgStore.add("[红包耗时] " + stage + " 累计=" + total + "ms 本步=" + delta + "ms");
        } catch (Throwable ignore) {}
    }

    private static void tickStart() {
        cT0 = System.currentTimeMillis();
        cTLast = cT0;
    }

    private static final java.util.LinkedHashMap<String, Boolean> GRABBED_BILLS =
            new java.util.LinkedHashMap<String, Boolean>(64, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(java.util.Map.Entry<String, Boolean> e) {
                    return size() > 200;
                }
            };

    private static void markGrabbed(String billNo) {
        if (billNo == null || billNo.isEmpty()) return;
        synchronized (GRABBED_BILLS) {
            GRABBED_BILLS.put(billNo, Boolean.TRUE);
        }
    }

    private static boolean isGrabbed(String billNo) {
        if (billNo == null || billNo.isEmpty()) return false;
        synchronized (GRABBED_BILLS) {
            return GRABBED_BILLS.containsKey(billNo);
        }
    }

    private static String cBillNo, cAuthkey, cPeerUin, cUserUin, cName, cUin;
    
    private static String cPeerUidRaw;
    
    private static String cUserUidRaw;

    
    private static volatile String cCmdAnswer;
    
    private static volatile long cCmdMsgRandom;
    
    private static volatile long cCmdSeq;
    private static int cChatType = 2, cChannel = 1;

    

    private static void grab(final Object record, final Object wallet) {
        Bg.run(new Runnable() {
            @Override public void run() {
                try {
                    final String billNo  = str(wallet, "billNo");
                    final String authkey = str(wallet, "authkey");

                    String peerUin = Refl.str(Refl.field(record, "peerUin"));
                    if (peerUin == null || peerUin.isEmpty() || peerUin.startsWith("u_")) {
                        String uid = strOr(Refl.field(record, "peerUid"), peerUin);
                        String conv = com.cathelperawa.owo.app.bridge.QQUtils.uinFromUid(uid);
                        peerUin = (conv != null && !conv.isEmpty()) ? conv : uid;
                    }
                    String userUin = Refl.str(Refl.field(record, "senderUin"));
                    if (userUin == null || userUin.isEmpty() || userUin.startsWith("u_")) {
                        String uid = strOr(Refl.field(record, "senderUid"), userUin);
                        String conv = com.cathelperawa.owo.app.bridge.QQUtils.uinFromUid(uid);
                        userUin = (conv != null && !conv.isEmpty()) ? conv : strOr(uid, "");
                    }

                    cBillNo = billNo;
                    cAuthkey = authkey;
                    cPeerUin = peerUin;
                    cUserUin = userUin;
                    cPeerUidRaw = Refl.str(Refl.field(record, "peerUid"));
                    cUserUidRaw = Refl.str(Refl.field(record, "senderUid"));
                    cName = strOr(QQEnv.currentNickname(), "QQ用户");
                    cUin = QQEnv.currentUin();
                    cChatType = Refl.i32(Refl.field(record, "chatType"), 1);
                    cChannel = Refl.i32(Refl.field(wallet, "redChannel"), 1);

                    try {
                        MsgStore.add("[语音诊断] handleRecord→grab\n"
                                + "  chatType=" + cChatType
                                + " redChannel=" + cChannel
                                + " (wallet类=" + wallet.getClass().getName() + ")\n"
                                + "  wallet.redChannel原始=" + Refl.field(wallet, "redChannel")
                                + "  billNo=" + billNo);
                    } catch (Throwable ignore) {}

                    if (cUin == null) { HookLog.w("红包：未登录"); return; }
                    tick("字段准备完成 (ch=" + cChannel + " ct=" + cChatType + ")");

                    HookLog.i("红包 字段: billNo=" + billNo + " authkey=" + authkey
                            + " record.peerUin=" + Refl.field(record, "peerUin")
                            + " record.peerUid=" + Refl.field(record, "peerUid")
                            + " record.senderUin=" + Refl.field(record, "senderUin")
                            + " record.senderUid=" + Refl.field(record, "senderUid")
                            + " → peerUin=" + cPeerUin + " userUin=" + cUserUin
                            + " chatType=" + cChatType + " channel=" + cChannel);

                    

                    

                    

                    

                    

                    

                    

                    
                    
                    if (cChannel == 32 && cChatType == 2) {
                        String title = titleOf(wallet);
                        if (title == null || title.isEmpty()) title = str(wallet, "title");
                        cCmdAnswer = title;

                        

                        

                        String mode = HookConfig.get(QQEnv.appContext(), "rp_cmd_mode", "send");
                        if ("borrow".equals(mode)) {
                            
                            cCmdMsgRandom = Refl.lng(Refl.field(record, "msgRandom"), 0L);
                            cCmdSeq = Refl.lng(Refl.field(record, "clientSeq"), 0L);
                        } else if ("send".equals(mode)) {
                            
                            cCmdMsgRandom = 0L;
                            cCmdSeq = 0L;
                        } else {
                            
                            cCmdMsgRandom = 100000000L + (Math.abs(RND.nextInt()) % 1900000000L);
                            cCmdSeq = 10000L + (Math.abs(RND.nextInt()) % 90000L);
                        }
                        HookLog.i("群聊口令红包: mode=" + mode + " answer=" + cCmdAnswer
                                + " msg_id=" + cCmdMsgRandom + " msg_seq=" + cCmdSeq
                                + "（send 模式等 preGrab 确认后发送）");
                    } else {
                        
                        cCmdAnswer = null;
                        cCmdMsgRandom = 0L;
                        cCmdSeq = 0L;
                    }

                    

                    

                    

                    if (cChannel == 65536) {
                        HookLog.i("语音红包: 走【TTS生成+上传】路线"
                                + "（音频见 cat_msg.txt）");
                        try { MsgStore.add("[语音诊断] ★ 进入语音分支！title="
                                + titleOf(wallet) + " billNo=" + cBillNo); }
                        catch (Throwable ignore) {}
                        
                        final String voiceTitle = titleOf(wallet) != null
                                ? titleOf(wallet) : str(wallet, "title");
                        new Thread(new Runnable() {
                            @Override public void run() {
                                boolean ok = prepareVoice(voiceTitle);
                                if (!ok) {
                                    HookLog.w("语音红包: 上传未通过，仍尝试领取（兜底）");
                                }
                                final int rnd = Math.abs(RND.nextInt()) & 0xF;
                                sendPreGrab(rnd, preGrabForm(rnd));
                            }
                        }, "cat-voice-prep").start();
                        return;   
                    }

                    final int rnd = Math.abs(RND.nextInt()) & 0xF;
                    final Map<String, String> preForm = preGrabForm(rnd);

                    

                    

                    

                    

                    

                    

                    

                    

                    
                    
                    boolean fastOk = (cChannel == 1) && FAST_STATE >= 0
                            && !"0".equals(HookConfig.get(QQEnv.appContext(), "rp_fast", "1"));
                    if (fastOk) {
                        final boolean probe = (FAST_STATE == 0);
                        try { sendFastGrab(rnd, preForm); }
                        catch (Throwable t) { HookLog.w("极速通道异常: " + t); }
                        if (!probe) {
                            
                            return;
                        }
                        
                        try { Thread.sleep(250L); } catch (Throwable ignore) {}
                    }

                    sendPreGrab(rnd, preForm);
                } catch (Throwable t) {
                    HookLog.e("grab 异常", t);
                }
            }
        }, "cat-redpacket");
    }

    
    private static Map<String, String> preGrabForm(int rnd) {
        Map<String, String> m = new LinkedHashMap<String, String>();
        boolean isGroup = (cChatType == 2);
        boolean isCommand = (cChannel == 32);
        m.put("listid", cBillNo);
        m.put("channel", String.valueOf(cChannel));
        m.put("groupid", isGroup ? cPeerUin : cUin);
        m.put("grouptype", isGroup ? "1" : "0");
        m.put("groupuin", cUserUin);
        m.put("name", cName);
        m.put("pay_flag", (isGroup && !isCommand) ? "0" : "1");
        m.put("authkey", cAuthkey);
        m.put("uin", cUin);
        if (!isGroup) m.put("senderuin", cUserUin);
        m.put("trans_seq", String.valueOf(rnd));
        m.put("hb_from", "0");
        return m;
    }

    

    
    private static Map<String, String> grabForm(String token) {
        Map<String, String> m = new LinkedHashMap<String, String>();
        boolean isGroup = (cChatType == 2);
        boolean isCommand = (cChannel == 32);
        boolean isVoice = (cChannel == 65536);

        boolean asPrivate = isGroup && "private".equals(
                HookConfig.get(QQEnv.appContext(), "rp_group_as_private", ""));
        boolean effGroup = isGroup && !asPrivate;
        m.put("channel", String.valueOf(cChannel));
        m.put("listid", cBillNo);
        m.put("groupid", effGroup ? cPeerUin : cUin);
        m.put("grouptype", effGroup ? "1" : "0");
        m.put("groupuin", cUserUin);
        m.put("name", cName);

        
        m.put("pay_flag", (isVoice || (effGroup && !isCommand)) ? "0" : "1");
        m.put("authkey", cAuthkey);
        m.put("uin", cUin);
        if (!effGroup && !isVoice) m.put("senderuin", cUserUin);
        m.put("hb_from", "0");
        m.put("agreement", "0");
        
        if (isCommand && effGroup && cCmdAnswer != null && !cCmdAnswer.isEmpty()) {
            m.put("answer", cCmdAnswer);
            m.put("msg_id", String.valueOf(cCmdMsgRandom));
            m.put("msg_md5", md5(cCmdAnswer));
            m.put("msg_seq", String.valueOf(cCmdSeq));
        }

        if (token != null && !token.isEmpty()) m.put("pre_grap_token", token);
        if (asPrivate) HookLog.i("★ 群红包按私聊格式发: " + m);
        return m;
    }

    

    private static void sendPreGrab(final int rnd, Map<String, String> form) {
        try {
            final String uin = cUin;
            
            String psKey = TimCookie.getPskey("tenpay.com");
            String hexGuid = TimCookie.hexGuid();
            if (psKey == null || hexGuid == null) {
                HookLog.w("红包：pskey/hexGuid 拿不到"); toast("票据没拿到喵…再试试？"); return;
            }
            String formStr = TimWallet.formString(form);
            HookLog.i("红包 preGrab 表单: " + formStr);
            String encText = TimWallet.encrypt(uin, SALT_PREGRAB, rnd,
                    formStr, psKey, hexGuid);
            if (encText == null) { toast("红包打不开喵…"); return; }

            byte[] data = buildPreGrabReq(encText, rnd);
            if (data == null) { toast("红包包不起来喵…"); return; }

            PENDING = "pre";
            tick("preGrab 准备完成，开始发送");
            TimServlet.send(CMD_SSO, data, TimServlet.DEFAULT_TIMEOUT,
                    new TimServlet.Callback() {
                @Override public void onReceive(int type, boolean ok, Bundle b) {
                    try {
                        byte[] rsp = TimServlet.responseBytes(b);
                        if (rsp == null) {
                            HookLog.w("红包 preGrab 无回包: " + TimServlet.dumpBundle(b));
                            toast("红包没回应喵…"); PENDING = null; recallPending(); return;
                        }
                        String json = parsePreGrabReply(rsp, uin, rnd & 0xF);
                        HookLog.i("红包 preGrab 回包: " + json);
                        if (json == null) {
                            toast("红包回应看不懂喵…"); PENDING = null; recallPending(); return;
                        }
                        afterPreGrab(json);
                    } catch (Throwable t) {
                        HookLog.e("preGrab 回调异常", t); PENDING = null; recallPending();
                    }
                }
            });
        } catch (Throwable t) {
            HookLog.e("sendPreGrab 失败", t); PENDING = null; recallPending();
        }
    }

    private static void afterPreGrab(String json) {
        try {
            tick("preGrab 回包");
            
            if (cBillNo != null && cBillNo.equals(FAST_DONE_BILL)) {
                HookLog.i("[极速] preGrab 回包时已抢到，跳过正常流程 billNo=" + cBillNo);
                PENDING = null;
                recallPending();
                return;
            }
            if (json == null || json.isEmpty()) {
                toast("红包没消息喵…"); PENDING = null; recallPending(); return;
            }
            org.json.JSONObject o = new org.json.JSONObject(json);
            if (o.optInt("retcode", -1) != 0) {
                HookLog.i("红包 preGrab retcode=" + o.optInt("retcode", -1)
                        + " msg=" + o.optString("retmsg"));

                
                if (cChannel == 65536) {
                    HookLog.i("★ 语音红包 preGrab 被拒 → 直接 grab（无 token 兜底）");
                    toast("语音红包本喵来啦～");
                    sendGrab("");
                    return;
                }
                toast("这个红包开不了喵～（" + o.optString("retmsg", "不知道为啥") + "）");
                PENDING = null;
                recallPending();          
                return;
            }
            
            try { MsgStore.add("[preGrab回包] ch=" + cChannel + " ct=" + cChatType
                    + "\n  " + json); } catch (Throwable ignore) {}
            org.json.JSONObject so = o.optJSONObject("send_object");
            double avg = 0;
            if (so != null) {
                double amt = parseD(so.optString("total_amount", "0"));
                double num = parseD(so.optString("total_num", "0"));
                if (num > 0) avg = amt / num;
            }
            Context c = QQEnv.appContext();
            int th = avgThreshold(c);
            if (th > 0 && avg < th) {
                HookLog.i("红包 平均 " + avg + " 分 < 阈值 " + th + " 分，放弃");
                toast("才 " + fen2yuan((long) avg) + " 元，本喵不稀罕喵～");
                PENDING = null;
                recallPending();          
                return;
            }

            

            

            

            

            

            

            

            boolean iGot = false;

            
            
            int pstate = o.optInt("state", 0);
            if (pstate == 1) iGot = true;
            
            org.json.JSONObject selfObj = o.optJSONObject("self_object");
            if (selfObj != null && selfObj.length() > 0) iGot = true;
            org.json.JSONArray recvArr = o.optJSONArray("recv_array");
            StringBuilder recvUins = new StringBuilder();
            if (recvArr != null) {
                for (int i = 0; i < recvArr.length(); i++) {
                    org.json.JSONObject r = recvArr.optJSONObject(i);
                    if (r == null) continue;
                    String ru = r.optString("recv_uin", "");
                    recvUins.append(ru).append(',');
                    if (!iGot && ru.equals(cUin)) iGot = true;
                }
            }
            try { MsgStore.add("[我领过判断] iGot=" + iGot + " self=" + selfObj
                    + " recvUins=[" + recvUins + "] 我的uin=" + cUin); } catch (Throwable ignore) {}
            HookLog.i("红包 preGrab: 我领过=" + iGot + " recvUins=[" + recvUins
                    + "] 我的uin=" + cUin);
            if (iGot) {
                toast("这个红包已经领过啦喵～");
                markGrabbed(cBillNo);
                PENDING = null;
                recallPending();
                return;
            }

            if (cChannel == 32 && cChatType == 2) {
                boolean selfGot = (selfObj != null) && (selfObj.length() > 0);
                int recvNum = (so != null) ? (int) parseD(so.optString("recv_num", "0")) : 0;
                int totalNum = (so != null) ? (int) parseD(so.optString("total_num", "0")) : 0;
                boolean soldOut = (totalNum > 0) && (recvNum >= totalNum);

                

                
                if (selfGot) {
                    HookLog.i("口令红包: self_object 非空 → 已领过");
                    toast("这个红包已经领过啦喵～");
                    markGrabbed(cBillNo);
                    PENDING = null;
                    recallPending();
                    return;
                }
                if (soldOut) {
                    HookLog.i("口令红包: recv=" + recvNum + "/" + totalNum + " → 已领完");
                    toast("这个红包已经被抢光啦喵～");
                    markGrabbed(cBillNo);
                    PENDING = null;
                    recallPending();
                    return;
                }
                HookLog.i("口令红包: recv=" + recvNum + "/" + totalNum + " → 能领，发口令");

                String mode = HookConfig.get(c, "rp_cmd_mode", "send");
                if ("send".equals(mode)) {
                    long sid = sendCommandMsg(cCmdAnswer);
                    if (sid == 0L) {
                        HookLog.w("口令消息: 未发出，无法领取");
                        toast("口令没发出去喵…");
                        PENDING = null;
                        return;
                    }
                    HookLog.i("口令红包: 确认能领，已发口令，seq 拿到");

                    
                    
                    try { Thread.sleep(350L); } catch (Throwable ignore) {}
                }
                
            } else {
                
                HookLog.i("红包 preGrab(非口令 ch=" + cChannel + "): state=" + o.optInt("state", 0)
                        + " self_object=" + o.optJSONObject("self_object")
                        + " send_object=" + so);
            }

            String token = o.optString("pre_grap_token", null);
            if (token == null || token.isEmpty()) {

                if (cChannel == 65536) {
                    HookLog.i("★ 语音红包无 pre_grap_token → 直接 grab 兜底");
                    sendGrab("");
                    return;
                }
                HookLog.w("红包：没有 pre_grap_token，回包=" + json);
                try { MsgStore.add("[无token] ch=" + cChannel + " " + json); } catch (Throwable ignore) {}
                toast("红包还没准备好喵…等会儿再试");
                PENDING = null;
                recallPending();          
                return;
            }
            sendGrab(token);
        } catch (Throwable t) {
            HookLog.e("afterPreGrab 异常", t);
            PENDING = null;
            recallPending();              
        }
    }

    

    
    private static void sendFastGrab(final int rnd, final Map<String, String> preForm) {
        final String bill = cBillNo;
        if (bill == null || bill.isEmpty()) return;
        final Map<String, String> map = grabForm("");   
        final boolean probing = (FAST_STATE == 0);      
        tick("[极速] 无token grab 已发 probe=" + probing);
        HookLog.i("[极速] 无token 预抢已发 billNo=" + bill + " probe=" + probing
                + " form=" + TimWallet.formString(map));
        boolean sent = TimNet.request(URL_NAGRAB, map, new TimNet.Sink() {
            @Override public void onResult(boolean ok, String jsonStr, String err) {
                try {
                    if (!ok || jsonStr == null) {
                        HookLog.i("[极速] 无token 抢失败 err=" + err + " body=" + jsonStr);
                        try { MsgStore.add("[极速] 失败 err=" + err + "\n  body=" + jsonStr); }
                        catch (Throwable ignore) {}
                        FAST_STATE = -1;
                        if (!probing) {
                            HookLog.i("[极速] 兜底 → 改走 preGrab 正常流程");
                            sendPreGrab(rnd, preForm);
                        }
                        return;
                    }
                    org.json.JSONObject json = new org.json.JSONObject(jsonStr);
                    org.json.JSONObject recvObj = json.optJSONObject("recv_object");
                    String amount = parseAmount(recvObj);
                    if (recvObj == null || amount == null) {
                        
                        FAST_STATE = -1;
                        HookLog.i("[极速] 服务端不接受无token抢 → 关闭极速通道。回包=" + jsonStr);
                        try {
                            MsgStore.add("[极速] 关闭：retcode=" + json.optString("retcode")
                                    + " retmsg=" + json.optString("retmsg")
                                    + " state=" + json.optString("state"));
                        } catch (Throwable ignore) {}
                        if (!probing) {
                            HookLog.i("[极速] 兜底 → 改走 preGrab 正常流程");
                            sendPreGrab(rnd, preForm);
                        }
                        return;
                    }
                    
                    FAST_STATE = 1;
                    FAST_DONE_BILL = bill;
                    tick("[极速] ★ 无token 直接抢到 amount=" + amount);
                    STAT_GOT++;
                    markGrabbed(bill);
                    toast("抢到红包啦喵～本喵超厉害！");
                    autoReply(cChatType, cPeerUin);
                    reportGhost(amount, json);
                    recallPending();
                } catch (Throwable t) { HookLog.w("[极速] 回调异常: " + t); }
            }
        });
        if (!sent) {
            HookLog.w("[极速] 通道不可用，放弃无token预抢");
            FAST_STATE = -1;
            if (!probing) sendPreGrab(rnd, preForm);
        }
    }

    private static void sendGrab(String token) {
        final Map<String, String> map = grabForm(token);
        HookLog.i("红包 grab 表单: " + TimWallet.formString(map));
        PENDING = "grab";
        tick("grab 开始发送");
        boolean sent = TimNet.request(URL_NAGRAB, map, new TimNet.Sink() {
            @Override public void onResult(boolean ok, String jsonStr, String err) {
                try {
                    tick("grab 回包 ok=" + ok);
                    HookLog.i("红包 grab 回包(NetSender): ok=" + ok + " " + jsonStr + " err=" + err);
                    
                    if (cChannel == 65536) {
                        try {
                            MsgStore.add("[语音红包] grab 回包 ok=" + ok
                                    + " chatType=" + cChatType
                                    + " 无token=" + (token == null || token.isEmpty())
                                    + "\n" + jsonStr);
                        } catch (Throwable ignore) {}
                    }
                    if (!ok) { toast("红包没抢到喵…"); PENDING = null; return; }
                    org.json.JSONObject json = (jsonStr == null) ? null
                            : new org.json.JSONObject(jsonStr);

                    

                    

                    

                    
                    
                    String retmsg = (json == null) ? "" : json.optString("retmsg", "");
                    int ret = (json == null) ? -1 : json.optInt("retcode", -1);
                    String state = (json == null) ? "" : json.optString("state", "");
                    org.json.JSONObject recvObj = (json == null) ? null
                            : json.optJSONObject("recv_object");

                    String amount = parseAmount(recvObj);     
                    
                    boolean reallyGot = (recvObj != null) && amount != null;

                    String why;
                    boolean alreadyGrabbed = false;   

                    if (json == null) {
                        why = "红包飞走了喵…";
                    } else if (reallyGot) {
                        why = null;   
                    } else if ("10".equals(state)) {
                        
                        why = "这个红包已经领过啦喵～";
                        alreadyGrabbed = true;
                    } else if (!state.isEmpty() && !"0".equals(state)) {
                        
                        why = "这个红包已经被抢光啦喵～";
                        alreadyGrabbed = true;
                    } else if (ret == 0 && recvObj == null) {

                        why = "这个红包已经领过啦喵～";
                        alreadyGrabbed = true;
                    } else if (ret == 0) {
                        
                        why = "红包领到了但算不清喵…（看日志）";
                    } else {
                        why = "红包不给面子喵（" + (retmsg.isEmpty() ? "码 " + ret : retmsg) + "）";
                    }

                    HookLog.i("红包判定: got=" + reallyGot
                            + " ret=" + ret + " state=" + state
                            + " recvObj=" + recvObj + " amount=" + amount
                            + " | 原包=" + jsonStr);
                    try {
                        MsgStore.add("[红包判定] got=" + reallyGot
                                + " ret=" + ret + " state=" + state
                                + " amount=" + amount
                                + " billNo=" + cBillNo + "\n  原包=" + jsonStr);
                    } catch (Throwable ignore) {}

                    if (reallyGot) {
                        STAT_GOT++;
                        markGrabbed(cBillNo);   
                        toast("抢到红包啦喵～本喵超厉害！");
                        
                        autoReply(cChatType, cPeerUin);
                        
                        reportGhost(amount, json);
                    } else {
                        if (alreadyGrabbed) {
                            STAT_ALREADY++;
                            markGrabbed(cBillNo);   
                        } else {
                            STAT_FAIL++;            
                        }
                        toast(why);
                    }
                } catch (Throwable t) { HookLog.e("grab 回调异常", t); }

                
                recallPending();
                PENDING = null;
            }
        });
        if (!sent) {
            HookLog.e("NetSender 通道不可用", null);
            toast("红包通道睡着了喵…");
            
            recallPending();
            PENDING = null;
        }
    }

    private static void reportGhost(final String amount, final org.json.JSONObject json) {
        try {
            String scene = (cChatType == 2 ? "群" : "私聊");
            String from = cUserUin == null ? "?" : cUserUin;
            StringBuilder sb = new StringBuilder();
            sb.append("抢到红包");
            if (amount != null && !amount.isEmpty()) {
                long fen = parseL(amount);
                sb.append(" ").append(fen2yuan(fen));
                if (fen > 0) sb.append("（").append(fen).append(" 分）");
            }
            sb.append(" | ").append(scene).append("(").append(cPeerUin).append(")");
            sb.append(" | 来自 ").append(from);
            GhostChat.log("红包", sb.toString());
        } catch (Throwable t) { HookLog.e("reportGhost 异常", t); }
    }

    private static boolean prepareVoice(String title) {
        try {
            try { MsgStore.add("[语音诊断] prepareVoice 开始 title=" + title); }
            catch (Throwable ignore) {}
            if (title == null || title.isEmpty()) {
                HookLog.w("语音红包: 口令为空，无法生成语音");
                return false;
            }
            Context c = QQEnv.appContext();
            if (c == null) return false;

            File dir = new File(c.getCacheDir(), "voice");
            if (!dir.exists()) dir.mkdirs();
            String slkPath = new File(dir, "voice_temp.slk").getAbsolutePath();

            long t0 = System.currentTimeMillis();
            String slk = VoiceSilk.text2Silk(c, title, slkPath);
            if (slk == null) {
                HookLog.w("语音红包: SILK 生成失败");
                return false;
            }
            HookLog.i("语音红包: SILK 就绪 " + new File(slk).length()
                    + " 字节（耗时 " + (System.currentTimeMillis() - t0) + "ms）");

            

            
            
            org.json.JSONObject j = new org.json.JSONObject();
            j.put("bill_no", cBillNo);
            j.put("voice_text", title);
            String maker = (cUserUin != null && !cUserUin.isEmpty()) ? cUserUin : cUin;
            try { j.put("make_uin", Long.parseLong(maker)); }
            catch (Throwable t) { j.put("make_uin", maker); }
            j.put("platform", 0);
            HookLog.i("语音红包: 上传 JSON=" + j);

            boolean ok = VoiceUpload.uploadOk(c, slk, j.toString(), 20000L);
            HookLog.i("语音红包: 上传结果 ok=" + ok);

            try { new File(slk).delete(); } catch (Throwable ignore) {}
            return ok;
        } catch (Throwable t) {
            HookLog.e("语音红包 prepareVoice 异常", t);
            return false;
        }
    }

    private static String nz(String s, String def) {
        return (s == null || s.isEmpty()) ? def : s;
    }

    private static long sendCommandMsg(String text) {
        long sentId = 0L;
        Object contact = null;
        String target = null;
        boolean sent = false;
        try {
            target = HookConfig.get(QQEnv.appContext(), "rp_cmd_target", "real");
            if ("real".equals(target)) {
                contact = com.cathelperawa.owo.app.bridge.ContactFactory.make(
                        cChatType, nz(cPeerUidRaw, cPeerUin), "");
            } else if ("ghost".equals(target)) {
                contact = com.cathelperawa.owo.app.bridge.ContactFactory.make(
                        com.cathelperawa.owo.app.bridge.GhostChat.GHOST_CHAT_TYPE,
                        com.cathelperawa.owo.app.bridge.GhostChat.GHOST_UID, "");
            } else {

                String myUid = QQEnv.currentUid();
                if (myUid == null || myUid.isEmpty()) {
                    HookLog.w("口令消息: 拿不到自己的 uid，回退发到红包会话");
                    target = "real";
                    contact = com.cathelperawa.owo.app.bridge.ContactFactory.make(
                            cChatType, nz(cPeerUidRaw, cPeerUin), "");
                } else {
                    HookLog.i("口令消息 → 自己 uid=" + myUid);
                    contact = com.cathelperawa.owo.app.bridge.ContactFactory.make(1, myUid, "");
                }
            }
            if (contact == null) { HookLog.w("口令消息: Contact 构造失败"); return 0L; }
            java.util.List<Object> els = java.util.Collections.singletonList(
                    com.cathelperawa.owo.app.bridge.ElementFactory.text(text));
            HookLog.i("口令消息 → 目标=" + target + " contact=" + contact);

            long t0 = System.currentTimeMillis();

            Object rec = com.cathelperawa.owo.app.bridge.MsgSender
                    .sendAndAwait(contact, els, 1500L);
            long tAwait = System.currentTimeMillis() - t0;

            if (rec != null) {
                sentId = Refl.lng(Refl.field(rec, "msgId"), 0L);
                cCmdMsgRandom = Refl.lng(Refl.field(rec, "msgRandom"), 0L);
                cCmdSeq = Refl.lng(Refl.field(rec, "clientSeq"), 0L);
                sent = (sentId != 0L);
            }

            if (rec == null || sentId == 0L) {
                long localId = com.cathelperawa.owo.app.bridge.MsgSender.lastSentMsgId();
                if (localId == 0L) {
                    HookLog.w("口令消息: 既无回调也无本地 msgId");
                    return 0L;
                }
                sentId = localId;
                sent = true;
                Object r2 = null;
                for (int i = 0; i < 6 && r2 == null; i++) {
                    r2 = com.cathelperawa.owo.app.bridge.NtMsgApi.getMsgsByMsgId(contact, sentId);
                    if (r2 instanceof java.util.List) {
                        java.util.List<?> l = (java.util.List<?>) r2;
                        r2 = l.isEmpty() ? null : l.get(0);
                    }
                    if (r2 == null) Thread.sleep(60);
                }
                if (r2 != null) {
                    cCmdMsgRandom = Refl.lng(Refl.field(r2, "msgRandom"), 0L);
                    cCmdSeq = Refl.lng(Refl.field(r2, "clientSeq"), 0L);
                } else {

                    
                    HookLog.w("口令消息: 反查不到 → 用本地 msgId 顶替 clientSeq（赌一把）");
                    cCmdSeq = sentId;
                    cCmdMsgRandom = sentId;
                }
            }
            long tSeq = System.currentTimeMillis() - t0;
            HookLog.i("口令消息就绪 耗时 " + tSeq + "ms（await=" + tAwait + "）msgId=" + sentId
                    + " msgRandom=" + cCmdMsgRandom + " clientSeq=" + cCmdSeq);
            try {
                MsgStore.add("[口令计时] 发出→拿到seq 共 " + tSeq + "ms"
                        + "（await " + tAwait + "ms）"
                        + "\n  本地msgId=" + sentId
                        + "\n  反查msgRandom=" + cCmdMsgRandom
                        + "\n  反查clientSeq=" + cCmdSeq
                        + "\n  ★ 本地msgId 是否 == clientSeq ? "
                        + (sentId == cCmdSeq ? "是" : "否"));
            } catch (Throwable ignore) {}

            
            if (sent && sentId != 0L) {
                addPendingRecall(contact, sentId);
            }
            return sentId;
        } catch (Throwable t) {
            HookLog.e("sendCommandMsg 异常", t);
            return sent ? sentId : 0L;
        }
    }

    private static final java.util.List<long[]> RECALL_QUEUE =
            java.util.Collections.synchronizedList(new java.util.ArrayList<long[]>());
    private static final java.util.List<Object> RECALL_CONTACTS =
            java.util.Collections.synchronizedList(new java.util.ArrayList<Object>());

    static void addPendingRecall(Object contact, long msgId) {
        if (contact == null || msgId == 0L) return;
        synchronized (RECALL_QUEUE) {
            
            for (int i = 0; i < RECALL_QUEUE.size(); i++) {
                if (RECALL_QUEUE.get(i)[0] == msgId) return;
            }
            RECALL_QUEUE.add(new long[]{msgId});
            RECALL_CONTACTS.add(contact);
        }
    }

    private static void recallPending() {
        final java.util.List<long[]> ids;
        final java.util.List<Object> contacts;
        synchronized (RECALL_QUEUE) {
            if (RECALL_QUEUE.isEmpty()) return;
            ids = new java.util.ArrayList<long[]>(RECALL_QUEUE);
            contacts = new java.util.ArrayList<Object>(RECALL_CONTACTS);
            RECALL_QUEUE.clear();
            RECALL_CONTACTS.clear();
        }
        Bg.run(new Runnable() {
            @Override public void run() {
                for (int k = 0; k < ids.size(); k++) {
                    long msgId = ids.get(k)[0];
                    Object contact = contacts.get(k);
                    
                    long[] delays = {150L, 300L, 600L, 1100L, 2000L};
                    boolean done = false;
                    for (int i = 0; i < delays.length && !done; i++) {
                        try {
                            Thread.sleep(delays[i]);
                            boolean ok = com.cathelperawa.owo.app.bridge.NtMsgApi.recall(contact, msgId);
                            HookLog.i("口令消息撤回[" + (k + 1) + "/" + ids.size() + "] 第"
                                    + (i + 1) + "次: " + ok + " msgId=" + msgId);
                            if (ok) done = true;
                        } catch (Throwable t) {
                            HookLog.w("口令消息撤回异常 第" + (i + 1) + "次: " + t);
                        }
                    }
                    if (!done) {
                        HookLog.w("★ 口令消息撤回失败 msgId=" + msgId + "（请手动检查群里）");
                    }
                }
            }
        }, "cat-recall");
    }

    private static String md5(String s) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] d = md.digest(s.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : d) {
                int i = b & 0xFF;
                sb.append(HEXC[i >>> 4]).append(HEXC[i & 0xF]);
            }
            return sb.toString();
        } catch (Throwable t) { HookLog.e("md5 失败", t); return ""; }
    }

    private static void autoReply(final int chatType, final String peerUin) {
        try {
            final List<String> rs = commaList(QQEnv.appContext(), K_REPLYS);

            final String sendUid = (cPeerUidRaw != null && !cPeerUidRaw.isEmpty())
                    ? cPeerUidRaw : peerUin;
            if (rs.isEmpty() || sendUid == null || sendUid.isEmpty()) return;
            final String reply = rs.get(RND.nextInt(rs.size()));
            HookLog.i("自动回复: chatType=" + chatType + " peerUid=" + sendUid
                    + " (peerUin=" + peerUin + ") 内容=" + reply);
            Bg.run(new Runnable() {
                @Override public void run() {
                    try { com.cathelperawa.owo.app.api.CatApi.sendText(chatType, sendUid, reply); }
                    catch (Throwable t) { HookLog.e("自动回复失败", t); }
                }
            }, "cat-redpacket-reply");
        } catch (Throwable t) { HookLog.e("autoReply 异常", t); }
    }

    

    private static Class<?> findHost(String name) {
        return HookApi.findClass(QQEnv.classLoader(), name);
    }

    private static byte[] buildPreGrabReq(String encText, int rnd) {
        try {
            Class<?> cls = findHost("tencent.im.qqwallet.QWalletHbPreGrab$QQHBRequest");
            if (cls == null) { HookLog.w("找不到 QQHBRequest"); return null; }
            Object req = newInstance(cls);
            if (req == null) return null;
            pbSet(req, "cgiName", CGI_PREGRAB);
            pbSet(req, "reqText", encText);
            pbSet(req, "random", String.valueOf(rnd));
            pbSetInt(req, "enType", 0);
            return TimWallet.add4Byte(pbBytes(req));
        } catch (Throwable t) { HookLog.e("buildPreGrabReq 失败", t); return null; }
    }

    private static byte[] buildNaGrabReq(String bodyStr) {
        try {
            Class<?> cls = findHost("tencent.im.qqwallet.QWalletHbPreGrab$QQHBRequest");
            if (cls == null) return null;
            Object req = newInstance(cls);
            if (req == null) return null;
            pbSet(req, "cgiName", CGI_NAGRAB);
            pbSetBytes(req, "reqBody", bodyStr.getBytes("UTF-8"));
            return TimWallet.add4Byte(pbBytes(req));
        } catch (Throwable t) { HookLog.e("buildNaGrabReq 失败", t); return null; }
    }

    private static String parsePreGrabReply(byte[] rsp, String uin, int rnd) {
        try {
            HookLog.i("preGrab 解析: rsp_len=" + (rsp == null ? -1 : rsp.length));
            Class<?> cls = findHost("tencent.im.qqwallet.QWalletHbPreGrab$QQHBReply");
            if (cls == null) { HookLog.w("preGrab: 找不到 QQHBReply 类"); return null; }
            Object reply = newInstance(cls);
            if (reply == null) { HookLog.w("preGrab: QQHBReply 构造失败"); return null; }
            Method mf = findMergeFrom(cls);
            if (mf == null) { HookLog.w("preGrab: 没有 mergeFrom"); return null; }
            mf.setAccessible(true);
            reply = mf.invoke(reply, (Object) rsp);
            String enc = pbGetString(reply, "rspText");
            HookLog.i("preGrab: rspText=" + (enc == null ? "null" : ("len=" + enc.length())));
            if (enc == null) {
                HookLog.i("preGrab: retCode=" + pbGetString(reply, "retCode")
                        + " retMsg=" + pbGetString(reply, "retMsg"));
                return null;
            }
            String out = TimWallet.decrypt(uin, CGI_PREGRAB, rnd, enc);
            HookLog.i("preGrab: 解密结果=" + out);
            return out;
        } catch (Throwable t) { HookLog.e("parsePreGrabReply 失败", t); return null; }
    }

    private static int parseNaGrabRet(byte[] rsp) {
        try {
            Class<?> cls = findHost("tencent.im.qqwallet.QWalletHbPreGrab$QQHBReply");
            if (cls == null || rsp == null) return -1;
            Object reply = newInstance(cls);
            if (reply == null) return -1;
            Method mf = findMergeFrom(cls);
            if (mf == null) return -1;
            mf.setAccessible(true);
            reply = mf.invoke(reply, (Object) rsp);
            Object rc = Refl.field(reply, "retCode");
            if (rc == null) return -1;
            Object v = callGet(rc);
            return v instanceof Number ? ((Number) v).intValue()
                    : Integer.parseInt(String.valueOf(v));
        } catch (Throwable t) { HookLog.e("parseNaGrabRet 失败", t); return -1; }
    }

    private static Method findMergeFrom(Class<?> cls) {
        Class<?> c = cls;
        while (c != null && c != Object.class) {
            for (Method m : c.getDeclaredMethods()) {
                if (!"mergeFrom".equals(m.getName())) continue;
                Class<?>[] ps = m.getParameterTypes();
                if (ps.length == 1 && ps[0] == byte[].class) return m;
            }
            c = c.getSuperclass();
        }
        HookLog.w("找不到 mergeFrom(byte[])");
        return null;
    }

    private static boolean pbSet(Object msg, String field, String value) {
        try {
            Object f = Refl.field(msg, field);
            if (f == null) return false;
            Method m = findMethod(f.getClass(), "set", 1);
            if (m == null) return false;
            m.setAccessible(true); m.invoke(f, value);
            return true;
        } catch (Throwable t) { return false; }
    }

    private static boolean pbSetBytes(Object msg, String field, byte[] value) {
        try {
            Object f = Refl.field(msg, field);
            if (f == null) return false;
            Class<?> bsm = findHost("com.tencent.mobileqq.pb.ByteStringMicro");
            Object wrapped = bsm == null ? null : newInstance(bsm, value);
            if (wrapped == null) return false;
            Method m = findMethod(f.getClass(), "set", 1);
            if (m == null) return false;
            m.setAccessible(true); m.invoke(f, wrapped);
            return true;
        } catch (Throwable t) { return false; }
    }

    private static boolean pbSetInt(Object msg, String field, int value) {
        try {
            Object f = Refl.field(msg, field);
            if (f == null) return false;
            for (Method m : f.getClass().getMethods()) {
                if (!"set".equals(m.getName()) || m.getParameterTypes().length != 1) continue;
                Class<?> pt = m.getParameterTypes()[0];
                try {
                    m.setAccessible(true);
                    if (pt == int.class) { m.invoke(f, value); return true; }
                    if (pt == long.class) { m.invoke(f, (long) value); return true; }
                } catch (Throwable ignore) {}
            }
            return false;
        } catch (Throwable t) { return false; }
    }

    private static String pbGetString(Object msg, String field) {
        Object f = Refl.field(msg, field);
        if (f == null) return null;
        Object v = callGet(f);
        return v == null ? null : String.valueOf(v);
    }

    private static byte[] pbBytes(Object msg) {
        try {
            Method m = findMethod(msg.getClass(), "toByteArray", 0);
            if (m == null) return null;
            m.setAccessible(true);
            Object v = m.invoke(msg);
            return v instanceof byte[] ? (byte[]) v : null;
        } catch (Throwable t) { return null; }
    }

    private static Object callGet(Object fieldObj) {
        try {
            Method m = findMethod(fieldObj.getClass(), "get", 0);
            if (m == null) return null;
            m.setAccessible(true);
            return m.invoke(fieldObj);
        } catch (Throwable t) { return null; }
    }

    

    public static Object walletOf(Object record) {
        if (record == null) return null;
        try {
            Object els = Refl.field(record, "elements");
            if (!(els instanceof List)) return null;
            for (Object el : (List<?>) els) {
                if (el == null) continue;
                Object w = Refl.field(el, "walletElement");
                if (w != null) return w;
            }
        } catch (Throwable ignore) {}
        return null;
    }

    private static String dumpWalletFields(Object wallet) {
        StringBuilder sb = new StringBuilder();
        if (wallet == null) return "(null)";
        try {
            Class<?> c = wallet.getClass();
            int depth = 0;
            while (c != null && c != Object.class && depth < 6) {
                for (java.lang.reflect.Field f : c.getDeclaredFields()) {
                    try {
                        f.setAccessible(true);
                        Object v = f.get(wallet);
                        sb.append(f.getName()).append('=');
                        if (v == null || v instanceof String
                                || v instanceof Number || v instanceof Boolean) {
                            sb.append(v);
                        } else if (v instanceof java.util.Collection) {
                            sb.append("[List size=").append(((java.util.Collection<?>) v).size()).append(']');
                        } else {
                            
                            sb.append(v.getClass().getSimpleName()).append('{');
                            try {
                                int n = 0;
                                for (java.lang.reflect.Field f2 : v.getClass().getDeclaredFields()) {
                                    if (n++ > 12) break;
                                    f2.setAccessible(true);
                                    Object v2 = f2.get(v);
                                    if (v2 instanceof String || v2 instanceof Number
                                            || v2 instanceof Boolean || v2 == null) {
                                        sb.append(f2.getName()).append('=').append(v2).append(',');
                                    } else {
                                        sb.append(f2.getName()).append('=').append(v2.getClass().getSimpleName()).append(',');
                                    }
                                }
                            } catch (Throwable ignore) {}
                            sb.append('}');
                        }
                        sb.append(" | ");
                    } catch (Throwable ignore) {}
                }
                c = c.getSuperclass();
                depth++;
            }
        } catch (Throwable t) {
            sb.append("dump异常:").append(t);
        }
        return sb.toString();
    }

    private static boolean isAlreadyOpened(Object wallet) {
        if (wallet == null) return true;
        
        try {
            MsgStore.add("[wallet字段] " + dumpWalletFields(wallet));
        } catch (Throwable ignore) {}
        try {
            
            String billNo = str(wallet, "billNo");
            if (billNo != null && !billNo.isEmpty() && isGrabbed(billNo)) {
                HookLog.i("红包预检: billNo=" + billNo + " 已在本地已处理表 → 跳过");
                return true;
            }
            
            if (toBool(Refl.field(wallet, "isOpened"))) {
                HookLog.i("红包预检: isOpened=true → 跳过");
                return true;
            }

            
            Object recv = Refl.field(wallet, "receiver");
            if (recv != null) {
                Object amount = Refl.field(recv, "amount");
                Object recvUin = Refl.field(recv, "recvUin");
                Object recvTime = Refl.field(recv, "recvTime");
                if (amount != null || recvUin != null || recvTime != null) {
                    HookLog.i("红包预检: 注意 —— wallet.receiver 已有内容"
                            + "（amount=" + amount + " recvUin=" + recvUin
                            + " recvTime=" + recvTime + "），暂不拦截，仅记录");
                }
            }
        } catch (Throwable t) {
            HookLog.w("红包预检异常（放行）: " + t);
        }
        return false;
    }

    private static String titleOf(Object wallet) {        try {
            Object recv = Refl.field(wallet, "receiver");
            if (recv != null) {
                Object t = Refl.field(recv, "title");
                if (t != null) return String.valueOf(t);
            }
            Object pro = Refl.field(wallet, "prompt");
            if (pro != null) return String.valueOf(pro);
        } catch (Throwable ignore) {}
        return "红包";
    }

    private static Object recordOf(Object msgItem) {
        if (msgItem == null) return null;
        try { if (Refl.field(msgItem, "elements") != null) return msgItem; }
        catch (Throwable ignore) {}
        try {
            Method g = msgItem.getClass().getMethod("getMsgRecord");
            g.setAccessible(true);
            return g.invoke(msgItem);
        } catch (Throwable ignore) {}
        return null;
    }

    private static boolean toBool(Object o) {
        if (o instanceof Boolean) return ((Boolean) o).booleanValue();
        if (o instanceof Number) return ((Number) o).intValue() != 0;
        return "true".equalsIgnoreCase(String.valueOf(o));
    }

    private static double parseD(String s) {
        try { return Double.parseDouble(s); } catch (Throwable t) { return 0; }
    }

    private static String str(Object o, String f) { return Refl.str(Refl.field(o, f)); }

    private static String parseAmount(org.json.JSONObject recvObj) {
        if (recvObj == null) return null;
        
        Object v = recvObj.opt("amount");
        if (v == null) {
            
            return "";
        }
        String s = String.valueOf(v).trim();
        
        try {
            if (s.contains(".")) {
                double d = Double.parseDouble(s);
                return String.valueOf((long) d);
            }
        } catch (Throwable ignore) {}
        return s;
    }

    private static String fen2yuan(long fen) {
        StringBuilder s = new StringBuilder();
        if (fen < 0) { s.append('-'); fen = -fen; }
        s.append(fen / 100);
        long r = fen % 100;
        if (r != 0) {
            s.append('.');
            if (r < 10) s.append('0');
            s.append(r);
            while (s.charAt(s.length() - 1) == '0') s.setLength(s.length() - 1);
        }
        return s.toString();
    }

    private static long parseL(String s) {
        if (s == null) return 0;
        try { return Long.parseLong(s.trim()); } catch (Throwable ignore) {}
        try { return (long) Double.parseDouble(s.trim()); } catch (Throwable ignore) {}
        return 0;
    }

    private static String strOr(Object o, String def) {
        String s = (o == null) ? null : String.valueOf(o);
        return (s == null || s.isEmpty()) ? def : s;
    }

    private static void toast(String msg) {
        try { CatToast.show(msg); } catch (Throwable ignore) {}
    }

    

    private static Method findMethod(Class<?> cls, String name, int argc) {
        if (cls == null) return null;
        Class<?> c = cls;
        while (c != null && c != Object.class) {
            for (Method m : c.getDeclaredMethods()) {
                if (m.getName().equals(name) && m.getParameterTypes().length == argc) return m;
            }
            c = c.getSuperclass();
        }
        return null;
    }

    private static Object newInstance(Class<?> cls, Object... args) {
        if (cls == null) return null;
        Object[] a = (args == null) ? new Object[0] : args;
        for (java.lang.reflect.Constructor<?> c : cls.getDeclaredConstructors()) {
            if (c.getParameterTypes().length != a.length) continue;
            try { c.setAccessible(true); return c.newInstance(a); } catch (Throwable ignore) {}
        }
        return null;
    }
}
