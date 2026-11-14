package com.cathelperawa.owo.app.ai;

import com.cathelperawa.owo.app.api.CookieApi;
import com.cathelperawa.owo.app.core.Bg;
import com.cathelperawa.owo.app.api.ElementApi;
import com.cathelperawa.owo.app.api.EventApi;
import com.cathelperawa.owo.app.api.IdentityApi;
import com.cathelperawa.owo.app.api.MultiSelectApi;
import com.cathelperawa.owo.app.api.RedPacketApi;
import com.cathelperawa.owo.app.api.SendProtoApi;
import com.cathelperawa.owo.app.bridge.MsgSender;
import com.cathelperawa.owo.app.bridge.QQEnv;
import com.cathelperawa.owo.app.bridge.VoiceSilk;
import com.cathelperawa.owo.app.core.HookApi;
import com.cathelperawa.owo.app.core.dex.ClassResolver;
import com.cathelperawa.owo.app.MsgStore;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class AiCapability {

    private AiCapability() {}

    public static final class Item {
        public final String group;
        public final String name;
        public final boolean ok;
        public final String detail;

        Item(String group, String name, boolean ok, String detail) {
            this.group = group;
            this.name = name;
            this.ok = ok;
            this.detail = detail == null ? "" : detail;
        }
    }

    public interface Listener {
        void onDone(List<Item> items, long costMs);
    }

    public static void runAsync(final Listener l) {
        final long t0 = System.currentTimeMillis();
        Bg.run(new Runnable() {
            @Override public void run() {
                List<Item> items;
                try {
                    items = AiCapability.run();
                } catch (Throwable t) {
                    items = new ArrayList<Item>();
                    items.add(new Item("检测", "整体失败", false, String.valueOf(t)));
                }
                if (l != null) l.onDone(items, System.currentTimeMillis() - t0);
            }
        }, "cat-ai-cap");
    }

    public static List<Item> run() {
        List<Item> out = new ArrayList<Item>();
        ClassLoader cl = null;
        try {
            cl = QQEnv.classLoader();
        } catch (Throwable ignore) {
        }

        out.add(hasMsgService(cl));

        out.add(el("表达", "文本", ElementApi.text("体检"), "createTextElement", cl));
        out.add(el("表达", "图片", ElementApi.pic("/sdcard/x.jpg"), "createPicElement", cl));
        out.add(el("表达", "语音", ElementApi.ptt("/sdcard/x.slk"), "createPttElement", cl));
        out.add(el("表达", "文件", ElementApi.file("/sdcard/x.pdf"), "createFileElement", cl));
        out.add(el("表达", "表情", ElementApi.face(14), "createFaceElement", cl));
        out.add(el("表达", "@某人", ElementApi.at("10001", "@体检"), "createAtElement", cl));
        out.add(el("表达", "引用回复", ElementApi.reply(1L), "createReplyElement", cl));
        out.add(el("表达", "ARK 卡片", ElementApi.ark("{\"app\":\"com.tencent.structmsg\"}"),
                "createArkElement", cl));
        out.add(methodOfAny("表达", "长消息通道", cl,
                "com.cathelperawa.owo.app.proto.PbSender",
                "sendLong", "sendDirect", "sendCmd"));
        out.add(methodOf("表达", "TTS 转语音", cl,
                "com.cathelperawa.owo.app.bridge.VoiceSilk", "text2Silk"));

        int listeners = EventApi.listenerCount();
        out.add(new Item("感知", "消息事件总线", true,
                "监听 " + listeners + " · " + (EventApi.isRunning() ? "线程在跑" : "空闲（无订阅者）")));

        int buf;
        try {
            buf = MsgStore.all().size();
        } catch (Throwable t) {
            buf = -1;
        }
        out.add(new Item("感知", "消息缓冲", buf > 0,
                buf > 0 ? (buf + " 条已记录") : "空（可能没开「消息记录」）"));

        out.add(buddyProbe(cl));
        out.add(countOf("感知", "群列表", new MapSize() {
            @Override public int size() { return IdentityApi.allTroopNames().size(); }
        }));

        String peer;
        try {
            peer = SendProtoApi.peerUid();
        } catch (Throwable t) {
            peer = null;
        }
        out.add(new Item("感知", "当前会话", peer != null && peer.length() > 0,
                peer == null || peer.isEmpty()
                        ? "空 · 从设置页进来时正常（要在聊天页才有）"
                        : "peer=" + mask(peer)));

        out.add(methodOf("感知", "历史消息拉取", cl,
                "com.cathelperawa.owo.app.protocol.MsgFetcher", "pullGroup"));

        out.add(clsOf("感知", "图片抓取", cl,
                "com.cathelperawa.owo.app.protocol.ImageGrabber"));

        String skey;
        try {
            skey = CookieApi.skey();
        } catch (Throwable t) {
            skey = null;
        }
        out.add(new Item("感知", "登录态 Cookie", skey != null && skey.length() > 0,
                skey != null && skey.length() > 0 ? ("skey 长度 " + skey.length()) : "拿不到 skey"));

        String bar;
        try {
            bar = MultiSelectApi.currentBarClass();
        } catch (Throwable t) {
            bar = null;
        }
        out.add(new Item("感知", "多选态", true,
                bar == null || bar.isEmpty() ? "未抓到底栏（正常：需先进多选）" : shortName(bar)));

        boolean rp;
        try {
            rp = RedPacketApi.ready();
        } catch (Throwable t) {
            rp = false;
        }
        out.add(new Item("感知", "红包通道", rp, rp ? "就绪" : "未就绪"));

        out.add(nativeMethod("改群名片", "modifyMemberCardName"));
        out.add(nativeMethod("禁言", "setMemberShutUp"));
        out.add(nativeMethod("踢人", "kickMemberV2"));

        return out;
    }

    private static Item hasMsgService(ClassLoader cl) {
        Class<?> c = HookApi.findClass(cl, "com.tencent.qqnt.kernel.api.impl.MsgService");
        if (c == null) return new Item("表达", "发送通道", false, "找不到 MsgService");
        boolean has = false;
        for (Method m : c.getDeclaredMethods()) {
            if (!"sendMsg".equals(m.getName())) continue;
            for (Class<?> p : m.getParameterTypes()) {
                if (List.class.isAssignableFrom(p)) { has = true; break; }
            }
            if (has) break;
        }
        return new Item("表达", "发送通道", has, has ? "MsgService.sendMsg(List)" : "没有 sendMsg(List)");
    }

    private static Item el(String g, String name, Object built, String apiMethod, ClassLoader cl) {
        if (built != null) {
            return new Item(g, name, true, shortName(built.getClass().getName()));
        }
        String sig = sigs(cl, "com.tencent.qqnt.msg.api.impl.MsgUtilApiImpl", apiMethod);
        return new Item(g, name, false, "构造失败 · 宿主签名 " + sig);
    }

    private static String sigs(ClassLoader cl, String cls, String method) {
        try {
            Class<?> c = HookApi.findClass(cl, cls);
            if (c == null) return "(" + method + " 所在类找不到)";
            StringBuilder sb = new StringBuilder();
            for (Method m : c.getDeclaredMethods()) {
                if (!m.getName().equals(method)) continue;
                if (sb.length() > 0) sb.append(" | ");
                sb.append(m.getName()).append("(");
                Class<?>[] ps = m.getParameterTypes();
                for (int i = 0; i < ps.length; i++) {
                    if (i > 0) sb.append(", ");
                    sb.append(ps[i].getSimpleName());
                }
                sb.append(")");
            }
            return sb.length() == 0 ? "(没有 " + method + ")" : sb.toString();
        } catch (Throwable t) {
            return "(探测异常 " + t.getClass().getSimpleName() + ")";
        }
    }

    private static Item methodOfAny(String g, String name, ClassLoader cl,
                                    String cls, String... methods) {
        try {
            Class<?> c = Class.forName(cls, false, AiCapability.class.getClassLoader());
            for (int i = 0; i < methods.length; i++) {
                for (Method m : c.getDeclaredMethods()) {
                    if (methods[i].equals(m.getName())) {
                        return new Item(g, name, true,
                                m.getName() + "/" + m.getParameterTypes().length + " 参");
                    }
                }
            }
            return new Item(g, name, false, "没有 " + methods[0] + " 等");
        } catch (Throwable t) {
            return new Item(g, name, false, "找不到类 " + cls);
        }
    }

    private static Item buddyProbe(ClassLoader cl) {
        int n = 0;
        try {
            n = IdentityApi.allBuddies().size();
        } catch (Throwable ignore) {
        }
        if (n > 0) return new Item("感知", "好友列表", true, n + " 个");

        boolean clsOk = false;
        try {
            clsOk = ClassResolver.find(cl, "friend.dataServiceImpl",
                    "com.tencent.mobileqq.friend.api.impl.FriendDataServiceImpl") != null;
        } catch (Throwable ignore) {
        }
        boolean appOk = false;
        try {
            appOk = QQEnv.appRuntime() != null;
        } catch (Throwable ignore) {
        }
        return new Item("感知", "好友列表", false, "接口返回 0（先进一次好友列表触发加载）");
    }

    private static Item methodOf(String g, String name, ClassLoader cl, String cls, String method) {
        try {
            Class<?> c = Class.forName(cls, false, AiCapability.class.getClassLoader());
            for (Method m : c.getDeclaredMethods()) {
                if (method.equals(m.getName())) {
                    return new Item(g, name, true, m.getParameterTypes().length + " 个参数");
                }
            }
            return new Item(g, name, false, "类在，没有 " + method);
        } catch (Throwable t) {
            return new Item(g, name, false, "找不到类 " + cls);
        }
    }

    private static Item clsOf(String g, String name, ClassLoader cl, String cls) {
        try {
            Class<?> c = Class.forName(cls, false, AiCapability.class.getClassLoader());
            return new Item(g, name, c != null, c == null ? "null" : "类可用");
        } catch (Throwable t) {
            return new Item(g, name, false, "找不到类");
        }
    }

    private interface MapSize { int size(); }

    private static Item countOf(String g, String name, MapSize s) {
        try {
            int n = s.size();
            return new Item(g, name, n > 0, n > 0 ? (n + " 个") : "读不到（0 个）");
        } catch (Throwable t) {
            return new Item(g, name, false, "异常: " + t.getClass().getSimpleName());
        }
    }

    public static String exportSignatures() {
        StringBuilder sb = new StringBuilder();
        ClassLoader cl = QQEnv.classLoader();

        sb.append("== MsgUtilApiImpl ==\n");
        Class<?> c = HookApi.findClass(cl,
                "com.tencent.qqnt.msg.api.impl.MsgUtilApiImpl");
        if (c != null) {
            for (Method m : c.getDeclaredMethods()) {
                if (!m.getName().startsWith("create")) continue;
                sb.append(m.getName()).append('(');
                Class<?>[] ps = m.getParameterTypes();
                for (int i = 0; i < ps.length; i++) {
                    if (i > 0) sb.append(", ");
                    sb.append(ps[i].getName());
                }
                sb.append(") : ").append(m.getReturnType().getName()).append('\n');
            }
        } else {
            sb.append("(找不到 MsgUtilApiImpl)\n");
        }

        sb.append("\n== 混淆数据类 ==\n");
        String[] names = new String[]{
                "com.tencent.qqnt.msg.a.b",
                "com.tencent.qqnt.msg.data.a",
                "com.tencent.qqnt.msg.data.b",
        };
        for (int i = 0; i < names.length; i++) {
            Class<?> dc = HookApi.findClass(cl, names[i]);
            sb.append(names[i]).append(" : ");
            if (dc == null) {
                sb.append("不存在\n");
                continue;
            }
            sb.append("\n");
            java.lang.reflect.Constructor<?>[] ks = dc.getDeclaredConstructors();
            for (int k = 0; k < ks.length; k++) {
                sb.append("  <init>(");
                Class<?>[] ps = ks[k].getParameterTypes();
                for (int j = 0; j < ps.length; j++) {
                    if (j > 0) sb.append(", ");
                    sb.append(ps[j].getName());
                }
                sb.append(")\n");
            }
            for (Method m : dc.getDeclaredMethods()) {
                sb.append("  ").append(m.getName()).append('(');
                Class<?>[] ps = m.getParameterTypes();
                for (int j = 0; j < ps.length; j++) {
                    if (j > 0) sb.append(", ");
                    sb.append(ps[j].getName());
                }
                sb.append(") : ").append(m.getReturnType().getName()).append('\n');
            }
        }

        String txt = sb.toString();
        try {
            android.content.Context ctx = QQEnv.appContext();
            if (ctx != null) {
                java.io.File f = new java.io.File(ctx.getFilesDir(), "ai-sig.txt");
                java.io.FileOutputStream o = new java.io.FileOutputStream(f);
                o.write(txt.getBytes("UTF-8"));
                o.close();
            }
        } catch (Throwable ignore) {
        }
        return txt;
    }

    private static String cause(Throwable t) {
        Throwable c = t;
        for (int i = 0; i < 5 && c.getCause() != null && c.getCause() != c; i++) {
            c = c.getCause();
        }
        String msg = c.getMessage();
        return c.getClass().getSimpleName() + (msg == null ? "" : ": " + msg);
    }

    private static Item nativeMethod(String label, String method) {
        try {
            Object svc = com.cathelperawa.owo.app.api.GroupManageApi.service();
            if (svc == null) {
                return new Item("管理", label, false, "拿不到 native 群服务");
            }
            for (Method m : svc.getClass().getMethods()) {
                if (method.equals(m.getName())) {
                    return new Item("管理", label, true,
                            method + " / " + m.getParameterTypes().length + " 参");
                }
            }
            return new Item("管理", label, false, "没有 " + method);
        } catch (Throwable t) {
            return new Item("管理", label, false, "异常 " + t.getClass().getSimpleName());
        }
    }

    private static String shortName(String cn) {
        if (cn == null) return "";
        int i = cn.lastIndexOf('.');
        return i < 0 ? cn : cn.substring(i + 1);
    }

    private static String mask(String s) {
        if (s == null || s.length() <= 10) return s;
        return s.substring(0, 4) + "…" + s.substring(s.length() - 4);
    }
}
