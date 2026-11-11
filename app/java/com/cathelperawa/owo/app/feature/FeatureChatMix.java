package com.cathelperawa.owo.app.feature;

import com.cathelperawa.owo.app.core.LoadPriority;
import com.cathelperawa.owo.app.core.SelfCheckResult;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.Selection;
import android.text.SpannableStringBuilder;
import android.widget.EditText;

import com.cathelperawa.owo.app.MsgStore;
import com.cathelperawa.owo.app.bridge.CatToast;
import com.cathelperawa.owo.app.core.HookApi;
import com.cathelperawa.owo.app.core.HookConfig;
import com.cathelperawa.owo.app.core.HookItem;
import com.cathelperawa.owo.app.core.HookItemBase;
import com.cathelperawa.owo.app.core.HookItemFactory;
import com.cathelperawa.owo.app.core.HookLog;
import com.cathelperawa.owo.app.ui.CatDialog;
import com.cathelperawa.owo.app.util.Refl;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;

import de.robv.android.xposed.XC_MethodHook;

@HookItem(name = "文字表情复合消息", path = "聊天与消息/文字表情复合消息",
        description = "图片/表情包选完先插进输入框，和文字一起编辑再发送", order = 46,
        priority = LoadPriority.DEFERRED)
public class FeatureChatMix extends HookItemBase {

    @Override
    protected void check(SelfCheckResult.Checker c) {
        c.cls("输入框", CLS_EDIT_TEXT);
        c.cls("发送 VM", CLS_SEND_VM);
        c.cls("相册选择", "com.tencent.qqnt.aio.part.SendPicPart");
    }

    private static final String TAG = "[图文] ";

    public static final String K_MIX = "chat_mixsend";
    
    public static final String K_ALWAYS = "chat_mixsend_always";

    private static final String[] CLS_SEND_PIC = {
            "com.tencent.qqnt.aio.part.SendPicPart",
            "com.tencent.qqnt.aio.model.SendPicPart",
    };
    private static final String CLS_SEND_VM =
            "com.tencent.mobileqq.aio.input.sendmsg.AIOSendMsgVMDelegate";
    private static final String CLS_THUMB =
            "com.tencent.mobileqq.aio.input.fullscreen.i";
    private static final String CLS_PIC_INFO =
            "com.tencent.mobileqq.aio.input.fullscreen.c";
    private static final String CLS_IMAGE_SPAN =
            "com.tencent.mobileqq.aio.input.fullscreen.a";
    private static final String CLS_EDIT_TEXT =
            "com.tencent.mobileqq.aio.input.edit.AIOEditText";

    private static final WeakHashMap<EditText, Boolean> EDITS =
            new WeakHashMap<EditText, Boolean>();

    private static final WeakHashMap<EditText, Boolean> LONG_HOOKED =
            new WeakHashMap<EditText, Boolean>();

    private static final Handler UI = new Handler(Looper.getMainLooper());

    

    private static Context c() { return HookItemFactory.context(); }

    public static boolean on() { return HookConfig.getBoolean(c(), K_MIX, false); }

    public static boolean always() { return HookConfig.getBoolean(c(), K_ALWAYS, false); }

    public static void setAlways(boolean v) { HookConfig.set(c(), K_ALWAYS, String.valueOf(v)); }

    public static void resetAlways() { setAlways(false); }

    
    public static boolean isReplying() { return REPLYING; }

    public static void setReplying(boolean v) { REPLYING = v; }

    private static volatile boolean REPLYING = false;

    private static volatile boolean QUOTING = false;

    public static boolean isQuoting() { return QUOTING; }

    public static void setQuoting(boolean v) { QUOTING = v; }

    public static boolean quoteBarVisible() {
        try {
            if (android.os.Looper.myLooper() != android.os.Looper.getMainLooper()) return QUOTING;
            EditText et = currentEdit();
            if (et == null) return QUOTING;
            android.view.View in = et;
            for (int depth = 0; in != null && depth < 4; depth++) {
                android.view.ViewParent vp = in.getParent();
                if (!(vp instanceof android.view.View)) break;
                in = (android.view.View) vp;
                if (!(vp instanceof android.view.ViewGroup)) continue;
                if (hasQuoteBar((android.view.ViewGroup) vp)) return true;
            }
        } catch (Throwable ignore) {}
        return QUOTING;
    }

    private static boolean hasQuoteBar(android.view.ViewGroup g) {
        int n = g.getChildCount();
        for (int i = 0; i < n; i++) {
            android.view.View ch = g.getChildAt(i);
            if (ch == null || ch.getVisibility() != android.view.View.VISIBLE) continue;
            if (isQuoteBarView(ch, g)) return true;
        }
        return false;
    }

    private static boolean isQuoteBarView(android.view.View ch, android.view.ViewGroup host) {
        try {
            String cn = ch.getClass().getName();
            if (cn.contains("reply") || cn.contains("Reply")) return true;
            if (host.getTag() instanceof String) return false;
            
            java.lang.reflect.Field[] fs = host.getClass().getDeclaredFields();
            for (java.lang.reflect.Field f : fs) {
                Class<?> t = f.getType();
                if (t.isPrimitive() || t.getName().startsWith("android.")
                        || t.getName().startsWith("androidx.")) continue;
                f.setAccessible(true);
                Object v = f.get(host);
                if (v == null) continue;
                String fcn = v.getClass().getName();
                if (!fcn.contains("reply") && !fcn.contains("Reply")) continue;
                
                try {
                    java.lang.reflect.Method m = android.view.View.class
                            .getDeclaredMethod("hasOnLayoutChangeListeners");
                    m.setAccessible(true);
                    Object r = m.invoke(ch);
                    if (r instanceof Boolean && (Boolean) r) return true;
                } catch (Throwable ignore) {}
            }
        } catch (Throwable ignore) {}
        return false;
    }

    public static EditText edit() { return currentEdit(); }

    public boolean mainProcessOnly() { return true; }

    @Override

    public String switchKey() { return K_MIX; }

    private static void log(String s) {
        HookLog.i(TAG + s);
        MsgStore.add(TAG + s);
        diag(s);
    }

    private static void diag(String s) {
        try {
            Context c = c();
            if (c == null) return;
            java.io.File d = com.cathelperawa.owo.app.core.DataDir.global("diag");
            if (d == null) return;
            java.io.File f = new java.io.File(d, "mixdiag.txt");
            java.io.FileWriter w = new java.io.FileWriter(f, true);
            w.write(new java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US)
                    .format(new java.util.Date()) + "  " + s + "\n");
            w.close();
        } catch (Throwable ignore) {
        }
    }

    

    @Override
    public void entry(ClassLoader cl) {
        SHARED = this;
        log("开关 chat_mixsend=" + on() + " 图文模式=" + always());
        expect(9);              
        hint("在聊天里选图/表情包时");
        int ok = 0;
        ok += hookSafe("相册选图", new HookStep() { public boolean run() throws Throwable { return hookSendPic(cl); } });
        ok += hookSafe("AlbumMedia", new HookStep() { public boolean run() throws Throwable { return hookAlbumMediaEvent(cl); } });
        ok += hookSafe("AlbumMix", new HookStep() { public boolean run() throws Throwable { return hookAlbumMixEvent(cl); } });
        ok += hookSafe("MediaSend", new HookStep() { public boolean run() throws Throwable { return hookMediaSendEvent(cl); } });
        ok += hookSafe("表情包", new HookStep() { public boolean run() throws Throwable { return hookPicEmotion(cl); } });
        ok += hookSafe("系统表情", new HookStep() { public boolean run() throws Throwable { return hookFace(cl); } });
        ok += hookSafe("输入框", new HookStep() { public boolean run() throws Throwable { return hookEditText(cl); } });
        ok += hookSafe("aioContext", new HookStep() { public boolean run() throws Throwable { return hookAioContext(cl); } });
        ok += hookSafe("回复态", new HookStep() { public boolean run() throws Throwable { return hookReplyState(cl); } });
        log("安装完成 " + ok + "/9");
        hit(ok);                
        if (ok == 0) {
            fail("文字表情复合消息：9 项全部未挂上（TIM 版本可能变了）");
        } else if (ok < 9) {
            fail("文字表情复合消息：只装上 " + ok + "/9 项");
        }

        try {
            ChatMixKernelProbe.install(cl);
            log("内核探针已安装");
        } catch (Throwable t) {
            log("探针安装异常 " + t);
        }
    }

    private interface HookStep { boolean run() throws Throwable; }

    private static int hookSafe(String name, HookStep step) {
        try {
            return step.run() ? 1 : 0;
        } catch (Throwable t) {
            log("✗ " + name + " 安装失败: " + t);
            return 0;
        }
    }

    

    private boolean hookSendPic(ClassLoader cl) {
        Class<?> cls = HookApi.findClass(cl, CLS_SEND_PIC);
        if (cls == null) { log("未找到 SendPicPart"); return false; }
        Method q4 = HookApi.findMethodByParams(cls, void.class, Intent.class);
        if (q4 == null) { log("未找到 void(Intent)"); return false; }
        HookApi.before(q4, new HookApi.Action() {
            @Override public void call(XC_MethodHook.MethodHookParam p) {
                try { onAlbum((Intent) p.args[0], p); }
                catch (Throwable t) { log("q4 异常 " + t); }
            }
        });
        log("已挂 " + cls.getSimpleName() + "#" + q4.getName());
        return true;
    }

    private void onAlbum(Intent it, XC_MethodHook.MethodHookParam p) {
        if (!on()) { return; }
        if (it == null) return;
        Object ar = it.getParcelableExtra("ALBUM_RESULT");
        if (ar == null) { log("q4: 无 ALBUM_RESULT（不是相册返回）"); return; }
        
        Object need = Refl.call(ar, "b");
        log("q4: needSend=" + need);
        if (!Boolean.TRUE.equals(need)) return;

        Object listObj = Refl.call(ar, "f");
        if (!(listObj instanceof List)) return;
        List<?> list = (List<?>) listObj;
        if (list.isEmpty()) return;

        EditText et = currentEdit();
        if (!shouldInsert(et)) {
            log("有图但当前不满足插入条件（无输入框或没文字且非图文模式）→ 原流程发送");
            return;
        }

        int n = 0;
        for (Object mi : list) {
            Object path = Refl.call(mi, "getPath");
            if (!(path instanceof String) || ((String) path).length() == 0) continue;
            String s = (String) path;
            boolean gif = s.toLowerCase().endsWith(".gif");
            if (insert(s, null, gif)) n++;
        }
        if (n == 0) {
            log("插入 0 张 → 回落原发送");
            return;
        }
        
        fire("插入图片");
        
        p.setResult(null);
        
        Object text = Refl.call(ar, "a");
        if (text instanceof String && ((String) text).length() > 0) {
            appendText(et, (String) text);
        }
        log("已插入 " + n + " 张图片，原发送已拦下");
        toast("图已经塞进框里喵～");
    }

    

    

    

    private boolean hookAlbumMediaEvent(ClassLoader cl) {
        Class<?> vm = HookApi.findClass(cl, CLS_SEND_VM);
        Class<?> ev = HookApi.findClass(cl,
                "com.tencent.mobileqq.aio.event.AIOMsgSendEvent$SendAlbumMediaEvent");
        if (vm == null || ev == null) { log("未找到 SendAlbumMediaEvent 链路"); return false; }
        Method m = HookApi.findMethodByParams(vm, void.class, ev);
        if (m == null) { log("VM 未找到 void(SendAlbumMediaEvent)"); return false; }
        HookApi.before(m, new HookApi.Action() {
            @Override public void call(XC_MethodHook.MethodHookParam p) {
                try {
                    if (!on()) { log("AlbumMedia: 开关没开，放行"); return; }
                    Object e = p.args[0];
                    Object listObj = Refl.call(e, "a");     
                    if (!(listObj instanceof List)) { log("AlbumMedia: 取不到列表"); return; }
                    List<?> list = (List<?>) listObj;
                    log("AlbumMedia: 拦到 " + list.size() + " 张，输入框=" + (currentEdit() != null)
                            + " always=" + always());
                    if (list.isEmpty()) return;

                    EditText et = currentEdit();
                    if (!shouldInsert(et)) { log("AlbumMedia: 不满足插入条件，放行"); return; }

                    int n = 0;
                    for (Object item : list) {
                        Object path = Refl.call(item, "a");
                        if (!(path instanceof String) || ((String) path).length() == 0) {
                            
                            Object uri = Refl.call(item, "c");
                            if (uri != null) path = uri.toString();
                        }
                        if (!(path instanceof String)) { log("AlbumMedia: item 路径不是字符串"); continue; }
                        String s = (String) path;
                        log("AlbumMedia: 路径=" + s);
                        
                        if (s.startsWith("content://")) { log("AlbumMedia: content:// 跳过"); continue; }
                        boolean gif = s.toLowerCase().endsWith(".gif");
                        if (insert(s, null, gif)) n++;
                    }
                    if (n > 0) {
                        p.setResult(null);
                        log("兜底拦截：插入 " + n + " 张（SendAlbumMediaEvent 路径）");
                    } else {
                        log("AlbumMedia: 插入 0 张，放行原发送");
                    }
                } catch (Throwable t) {
                    log("SendAlbumMediaEvent 兜底异常 " + t);
                }
            }
        });
        log("已挂 兜底 SendAlbumMediaEvent → " + m.getName());
        return true;
    }

    private boolean hookAlbumMixEvent(ClassLoader cl) {
        Class<?> vm = HookApi.findClass(cl, CLS_SEND_VM);
        Class<?> ev = HookApi.findClass(cl,
                "com.tencent.mobileqq.aio.event.AIOMsgSendEvent$SendAlbumMixEvent");
        if (vm == null || ev == null) { log("未找到 SendAlbumMixEvent 链路"); return false; }
        Method m = HookApi.findMethodByParams(vm, void.class, ev);
        if (m == null) { log("VM 未找到 void(SendAlbumMixEvent)"); return false; }
        HookApi.before(m, new HookApi.Action() {
            @Override public void call(XC_MethodHook.MethodHookParam p) {
                try {
                    if (!on()) return;
                    Object e = p.args[0];
                    Object listObj = Refl.call(e, "a");
                    if (!(listObj instanceof List)) return;
                    List<?> list = (List<?>) listObj;
                    if (list.isEmpty()) return;

                    EditText et = currentEdit();
                    if (!shouldInsert(et)) return;

                    int n = 0;
                    for (Object item : list) {
                        Object path = Refl.call(item, "a");
                        if (!(path instanceof String) || ((String) path).length() == 0) {
                            Object uri = Refl.call(item, "c");
                            if (uri != null) path = uri.toString();
                        }
                        if (!(path instanceof String)) continue;
                        String s = (String) path;
                        if (s.startsWith("content://")) continue;
                        boolean gif = s.toLowerCase().endsWith(".gif");
                        if (insert(s, null, gif)) n++;
                    }
                    if (n > 0) {
                        Object text = Refl.call(e, "c");
                        if (text instanceof String && ((String) text).length() > 0) {
                            appendText(et, (String) text);
                        }
                        p.setResult(null);
                        log("兜底拦截：插入 " + n + " 张（SendAlbumMixEvent 路径）");
                    }
                } catch (Throwable t) {
                    log("SendAlbumMixEvent 兜底异常 " + t);
                }
            }
        });
        log("已挂 兜底 SendAlbumMixEvent → " + m.getName());
        return true;
    }

    

    

    

    private boolean hookMediaSendEvent(ClassLoader cl) {
        Class<?> vm = HookApi.findClass(cl, CLS_SEND_VM);
        Class<?> ev = HookApi.findClass(cl,
                "com.tencent.mobileqq.aio.event.AIOMsgSendEvent$MediaSendEvent");
        if (vm == null || ev == null) { log("未找到 MediaSendEvent 链路"); return false; }
        Method m = HookApi.findMethodByParams(vm, void.class, ev);
        if (m == null) { log("VM 未找到 void(MediaSendEvent)"); return false; }
        HookApi.before(m, new HookApi.Action() {
            @Override public void call(XC_MethodHook.MethodHookParam p) {
                try {
                    if (!on()) { log("Media: 开关没开，放行"); return; }
                    Object e = p.args[0];

                    Object withText = Refl.call(e, "c");
                    if (Boolean.TRUE.equals(withText)) {
                        log("Media: 带文字一起发，交给原流程");
                        return;
                    }
                    Object listObj = Refl.call(e, "b");     
                    if (!(listObj instanceof List)) { log("Media: 取不到列表"); return; }
                    List<?> list = (List<?>) listObj;
                    log("Media: 拦到 " + list.size() + " 张，输入框=" + (currentEdit() != null)
                            + " always=" + always());
                    if (list.isEmpty()) return;

                    EditText et = currentEdit();
                    if (!shouldInsert(et)) { log("Media: 不满足插入条件，放行"); return; }

                    int n = 0;
                    for (Object item : list) {
                        Object path = Refl.call(item, "a");
                        if (!(path instanceof String) || ((String) path).length() == 0) {
                            Object uri = Refl.call(item, "c");
                            if (uri != null) path = uri.toString();
                        }
                        if (!(path instanceof String)) { log("Media: 路径不是字符串"); continue; }
                        String s = (String) path;
                        log("Media: 路径=" + s);
                        if (s.startsWith("content://")) { log("Media: content:// 跳过"); continue; }
                        boolean gif = s.toLowerCase().endsWith(".gif");
                        if (insert(s, null, gif)) n++;
                    }
                    if (n > 0) {
                        p.setResult(null);
                        log("兜底拦截：插入 " + n + " 张（MediaSendEvent 路径）");
                    } else {
                        log("Media: 插入 0 张，放行原发送");
                    }
                } catch (Throwable t) {
                    log("MediaSendEvent 兜底异常 " + t);
                }
            }
        });
        log("已挂 兜底 MediaSendEvent → " + m.getName());
        return true;
    }

    

    private boolean hookPicEmotion(ClassLoader cl) {
        Class<?> vm = HookApi.findClass(cl, CLS_SEND_VM);
        Class<?> ev = HookApi.findClass(cl,
                "com.tencent.mobileqq.aio.event.AIOMsgSendEvent$PicEmotionSendEvent");
        if (vm == null || ev == null) { log("未找到 表情包事件链路"); return false; }
        Method m = HookApi.findMethodByParams(vm, void.class, ev);
        if (m == null) { log("VM 未找到 void(PicEmotionSendEvent)"); return false; }
        HookApi.before(m, new HookApi.Action() {
            @Override public void call(XC_MethodHook.MethodHookParam p) {
                try {
                    if (!on()) return;
                    Object e = p.args[0];
                    Object path = Refl.call(e, "b");     
                    if (!(path instanceof String) || ((String) path).length() == 0) return;
                    EditText et = currentEdit();
                    if (!shouldInsert(et)) return;
                    String s = (String) path;
                    boolean gif = s.toLowerCase().endsWith(".gif");
                    if (insert(s, null, gif)) {
                        p.setResult(null);
                        toast("表情包塞进去啦喵～");
                    }
                } catch (Throwable t) { log("表情包异常 " + t); }
            }
        });
        log("已挂 表情包 → " + m.getName());
        return true;
    }

    

    private boolean hookFace(ClassLoader cl) {
        Class<?> vm = HookApi.findClass(cl, CLS_SEND_VM);
        Class<?> ev = HookApi.findClass(cl,
                "com.tencent.mobileqq.aio.event.AIOMsgSendEvent$EmojiFaceSendEvent");
        if (vm == null || ev == null) { log("未找到 系统表情链路"); return false; }
        Method m = HookApi.findMethodByParams(vm, void.class, ev);
        if (m == null) { log("VM 未找到 void(EmojiFaceSendEvent)"); return false; }
        HookApi.before(m, new HookApi.Action() {
            @Override public void call(XC_MethodHook.MethodHookParam p) {
                try {
                    if (!on()) return;
                    Object e = p.args[0];
                    int qsid = Refl.i32(Refl.call(e, "a"), -1);
                    if (qsid < 0) return;
                    EditText et = currentEdit();
                    if (!shouldInsert(et)) return;
                    String face = faceStr(qsid);
                    if (face == null || face.length() == 0) return;
                    Editable ed = et.getEditableText();
                    if (ed == null) return;
                    int pos = Math.max(0, Selection.getSelectionStart(ed));
                    ed.insert(pos, face);
                    Selection.setSelection(ed, pos + face.length());
                    p.setResult(null);
                } catch (Throwable t) { log("系统表情异常 " + t); }
            }
        });
        log("已挂 系统表情 → " + m.getName());
        return true;
    }

    private String faceStr(int qsid) {
        try {
            Class<?> k = HookApi.findClass(cl, "com.tencent.qqnt.emotion.k.c");
            if (k != null) {
                Method m = HookApi.findMethodByParams(k, String.class, int.class);
                if (m != null) {
                    Object r = m.invoke(null, qsid);
                    if (r instanceof String) return (String) r;
                }
            }
        } catch (Throwable ignore) {}
        
        return "\u0014" + qsid;
    }

    

    private static EditText lastEdit;

    private static EditText currentEdit() {

        
        
        try {
            android.app.Activity a = com.cathelperawa.owo.app.bridge.QQEnv.topActivity();
            if (a != null) {
                int id = a.getResources().getIdentifier("input", "id", a.getPackageName());
                if (id != 0) {
                    android.view.View v = a.findViewById(id);
                    if (v instanceof EditText) {
                        EditText et = (EditText) v;
                        lastEdit = et;
                        return et;
                    }
                }
            }
        } catch (Throwable ignore) {
        }
        EditText et2 = lastEdit;
        if (et2 != null) return et2;
        synchronized (EDITS) {
            for (EditText e : EDITS.keySet()) lastEdit = e;
        }
        return lastEdit;
    }

    private boolean hookEditText(ClassLoader cl) {
        Class<?> cls = HookApi.findClass(cl, CLS_EDIT_TEXT);
        if (cls == null) { log("未找到 AIOEditText"); return false; }
        int n = 0;
        for (java.lang.reflect.Constructor<?> ctor : cls.getDeclaredConstructors()) {
            try {
                HookApi.after(ctor, new HookApi.Action() {
                    @Override public void call(XC_MethodHook.MethodHookParam p) {
                        try {
                            final EditText et = (EditText) p.thisObject;
                            if (et == null) return;
                            lastEdit = et;
                            synchronized (EDITS) { EDITS.put(et, Boolean.TRUE); }
                            ensureLongHook(et);
                        } catch (Throwable ignore) {}
                    }
                });
                n++;
            } catch (Throwable ignore) {}
        }
        log("AIOEditText 构造器已挂 " + n + " 个");
        return n > 0;
    }

    private void ensureLongHook(final EditText et) {
        synchronized (LONG_HOOKED) {
            if (LONG_HOOKED.containsKey(et)) return;
            LONG_HOOKED.put(et, Boolean.TRUE);
        }
        et.setOnLongClickListener(new android.view.View.OnLongClickListener() {
            @Override public boolean onLongClick(android.view.View v) {
                if (!on()) return false;       
                showMenu((EditText) v);
                return true;                   
            }
        });
    }

    private void showMenu(final EditText et) {
        UI.post(new Runnable() { @Override public void run() {
            try {
                final String[] items = new String[] {
                        always() ? "关闭图文消息模式" : "启用图文消息模式",
                        "换行",
                        "全屏输入",
                };
                CatDialog.pick(ctx, "输入框小菜单", items, new CatDialog.OnPick() {
                    @Override public void run(int idx, String label) {
                        if (idx == 0) {
                            setAlways(!always());
                            toast(always() ? "图文模式开啦～空着也能塞图喵"
                                           : "图文模式关掉喵～");
                        } else if (idx == 1) {
                            Editable ed = et.getEditableText();
                            if (ed != null) {
                                int p = Math.max(0, Selection.getSelectionStart(ed));
                                ed.insert(p, "\n");
                                Selection.setSelection(ed, p + 1);
                            }
                        } else if (idx == 2) {
                            if (!enterFullScreen()) {
                                toast("全屏打不开喵…");
                            }
                        }
                    }
                });
            } catch (Throwable t) {
                log("菜单异常 " + t);
            }
        } });
    }

    

    
    private static boolean shouldInsert(EditText et) {
        if (et == null) return false;
        if (always()) return true;
        CharSequence cs = et.getText();
        return cs != null && cs.length() > 0;
    }

    private static final String ZW = "\u200B";

    public static boolean seedZeroWidth() {
        try {
            EditText et = currentEdit();
            if (et == null) return false;
            Editable ed = et.getEditableText();
            if (ed == null) return false;
            String cur = ed.toString();
            if (cur.contains(ZW)) return true;
            int pos = Math.max(0, Selection.getSelectionStart(ed));
            if (pos > ed.length()) pos = ed.length();
            ed.insert(pos, ZW);
            Selection.setSelection(ed, pos + ZW.length());
            return true;
        } catch (Throwable t) {
            log("塞零宽异常 " + t);
            return false;
        }
    }

    public static boolean clearZeroWidth() {
        try {
            EditText et = currentEdit();
            if (et == null) return false;
            Editable ed = et.getEditableText();
            if (ed == null) return false;
            String cur = ed.toString();
            if (!cur.contains(ZW)) return false;
            int idx = cur.indexOf(ZW);
            while (idx >= 0) {
                ed.delete(idx, idx + ZW.length());
                cur = ed.toString();
                idx = cur.indexOf(ZW);
            }
            return true;
        } catch (Throwable t) {
            log("清零宽异常 " + t);
            return false;
        }
    }

    private boolean insert(String path, Uri uri, boolean isGif) {
        try {
            EditText et = currentEdit();
            if (et == null) { log("没有输入框，无法插入"); return false; }
            Editable ed = et.getEditableText();
            if (ed == null) { log("输入框没有 Editable"); return false; }

            Class<?> spanCls = HookApi.findClass(cl, CLS_IMAGE_SPAN);
            if (spanCls != null) {
                Object[] old = ed.getSpans(0, ed.length(), spanCls);
                if (old != null && old.length >= 20) {
                    toast("最多 20 张啦喵～");
                    return false;
                }
            }

            Object span = makeSpan(path, uri, isGif);
            if (span == null) { log("缩略图生成失败  " + path); return false; }
            log("span 类型=" + span.getClass().getName());

            

            

            

            
            SpannableStringBuilder sb = new SpannableStringBuilder();
            sb.append('\u200B');
            int imgStart = sb.length();
            sb.append("图片");
            sb.setSpan(span, imgStart, sb.length(), 33);
            sb.append('\n');

            
            
            int end = ed.length();
            ed.replace(end, end, sb);
            Selection.setSelection(ed, end + sb.length());

            try {
                Class<?> sc = HookApi.findClass(cl, CLS_IMAGE_SPAN);
                if (sc != null) {
                    Object[] chk = ed.getSpans(0, ed.length(), sc);
                    log("插入后 imageSpan=" + (chk == null ? 0 : chk.length)
                            + " len=" + ed.length()
                            + " editId=" + System.identityHashCode(et)
                            + " 已挂实例=" + EDITS.size()
                            + " 可见=" + et.isShown()
                            + " 有焦点=" + et.hasFocus()
                            + " editable=" + ed.getClass().getSimpleName());
                }
            } catch (Throwable ignore) {}
            return true;
        } catch (Throwable t) {
            log("插入异常 " + t);
            return false;
        }
    }

    private Object makeSpan(String path, Uri uri, boolean isGif) {
        try {
            Class<?> iCls = HookApi.findClass(cl, CLS_THUMB);
            if (iCls == null) { log("未找到缩略图工厂"); return null; }
            Object inst = null;
            java.lang.reflect.Field f = Refl.findField(iCls, "a");
            if (f != null) { f.setAccessible(true); inst = f.get(null); }
            if (inst == null) { log("拿不到缩略图工厂实例"); return null; }
            Method m = HookApi.findMethodByParams(iCls, null, String.class, Uri.class, boolean.class);
            if (m == null) { log("未找到 a(String,Uri,boolean)"); return null; }
            return m.invoke(inst, path, uri, isGif);
        } catch (Throwable t) {
            log("造 span 异常 " + t);
            return null;
        }
    }

    

    
    private boolean enterFullScreen() {
        try {
            if (aioCtx == null) {
                log("还没拿到 aioContext，无法发全屏意图");
                return false;
            }
            Class<?> intentCls = HookApi.findClass(cl,
                    "com.tencent.mobileqq.aio.input.fullscreen.FullScreenMsgIntent$SetFullScreenMode");
            if (intentCls == null) { log("未找到 SetFullScreenMode 类"); return false; }
            java.lang.reflect.Constructor<?> ctor = intentCls.getDeclaredConstructor(boolean.class);
            ctor.setAccessible(true);
            Object intent = ctor.newInstance(true);

            Method e = aioCtx.getClass().getMethod("e");
            e.setAccessible(true);
            Object emitter = e.invoke(aioCtx);
            if (emitter == null) { log("aioContext.e() 为 null"); return false; }

            Method h = null;
            for (Method m : emitter.getClass().getMethods()) {
                if (m.getName().equals("h") && m.getParameterTypes().length == 1) { h = m; break; }
            }
            if (h == null) { log("emitter 没有 h(MsgIntent)"); return false; }
            h.setAccessible(true);
            h.invoke(emitter, intent);
            log("已发全屏意图 SetFullScreenMode(true)");
            return true;
        } catch (Throwable t) {
            log("发全屏意图异常 " + t);
            return false;
        }
    }

    private static volatile Object aioCtx;

    private boolean hookAioContext(ClassLoader cl) {
        Class<?> hCls = HookApi.findClass(cl, "com.tencent.mobileqq.aio.input.fullscreen.h");
        if (hCls == null) { log("未找到 fullscreen.h"); return false; }
        Method target = null;
        for (Method m : hCls.getDeclaredMethods()) {
            Class<?>[] ps = m.getParameterTypes();
            if (ps.length == 2 && ps[1] == boolean.class && !ps[0].isPrimitive()) {
                target = m;
                break;
            }
        }
        if (target == null) { log("未找到 F(aioContext, boolean)"); return false; }
        HookApi.before(target, new HookApi.Action() {
            @Override public void call(XC_MethodHook.MethodHookParam p) {
                aioCtx = p.args[0];
            }
        });
        log("已挂 aioContext 采集 " + target.getName());
        return true;
    }

    

    

    
    private boolean hookReplyState(ClassLoader cl) {
        Class<?> dm = HookApi.findClass(cl, "com.tencent.input.base.mvicompat.DelegateMediator");
        if (dm == null) { log("未找到 DelegateMediator"); return false; }
        int n = 0;
        for (final Method m : dm.getDeclaredMethods()) {
            
            if (m.getReturnType() != void.class) continue;
            if (m.isSynthetic() || m.isBridge()) continue;
            if (java.lang.reflect.Modifier.isAbstract(m.getModifiers())) continue;
            Class<?>[] ps = m.getParameterTypes();
            if (ps.length != 1 || ps[0].isPrimitive()) continue;
            HookApi.after(m, new HookApi.Action() {
                @Override public void call(XC_MethodHook.MethodHookParam p) {
                    Object a = (p.args != null && p.args.length > 0) ? p.args[0] : null;
                    if (a == null) return;
                    String cn = a.getClass().getName();
                    
                    if (cn.endsWith("SetReplyUIState")) {
                        QUOTING = true;
                        diag("引用态 = true（" + cn + "）");
                    } else if (cn.endsWith("ClearReplyUIState")) {
                        QUOTING = false;
                        diag("引用态 = false");
                    }
                }
            });
            n++;
            log("回复态挂在 " + m.getName() + "(" + ps[0].getSimpleName() + ")");
        }
        log("回复态监听挂载 " + n + " 个方法");
        return n > 0;
    }

    private static void appendText(EditText et, String text) {
        try {
            if (et == null || text == null || text.length() == 0) return;
            Editable ed = et.getEditableText();
            if (ed == null) return;
            int p = Math.max(0, Selection.getSelectionStart(ed));
            if (p > ed.length()) p = ed.length();
            ed.insert(p, text);
            Selection.setSelection(ed, p + text.length());
        } catch (Throwable ignore) {}
    }

    

    
    
    public static boolean insertPic(final String path, final boolean isGif) {
        final FeatureChatMix f = SHARED;
        if (f == null) { log("功能未加载，无法插图"); return false; }
        final EditText et = currentEdit();
        if (et == null) { log("没有输入框，无法插入"); return false; }

        

        if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) {
            try { return f.insert(path, null, isGif); }
            catch (Throwable t) { log("插入异常 " + t); return false; }
        }

        final java.util.concurrent.CountDownLatch latch =
                new java.util.concurrent.CountDownLatch(1);
        final boolean[] result = { false };
        et.post(new Runnable() {
            @Override public void run() {
                try {
                    result[0] = f.insert(path, null, isGif);
                } catch (Throwable t) {
                    log("post 插入异常 " + t);
                } finally {
                    latch.countDown();
                }
            }
        });
        try {
            if (!latch.await(800, java.util.concurrent.TimeUnit.MILLISECONDS)) {
                log("插入等待超时（800ms）");
                return false;
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            return false;
        }
        return result[0];
    }

    public static boolean inputHasImage() {
        try {
            EditText et = currentEdit();
            if (et == null) return false;
            Editable ed = et.getEditableText();
            if (ed == null || ed.length() == 0) return false;
            Class<?> spanCls = HookApi.findClass(com.cathelperawa.owo.app.bridge.QQEnv.classLoader(), CLS_IMAGE_SPAN);
            if (spanCls == null) return false;
            Object[] spans = ed.getSpans(0, ed.length(), spanCls);
            return spans != null && spans.length > 0;
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean appendToInput(String text) {
        try {
            EditText et = currentEdit();
            if (et == null) return false;
            appendText(et, text);
            return true;
        } catch (Throwable t) {
            log("追加文本异常 " + t);
            return false;
        }
    }

    public static String inputText() {
        try {
            EditText et = currentEdit();
            if (et == null) return null;
            CharSequence cs = et.getText();
            return cs == null ? "" : cs.toString();
        } catch (Throwable t) {
            return null;
        }
    }

    public static int inputPicCount() {
        try {
            EditText et = currentEdit();
            FeatureChatMix f = SHARED;
            if (et == null || f == null || f.cl == null) return 0;
            Editable ed = et.getEditableText();
            if (ed == null) return 0;
            Class<?> spanCls = HookApi.findClass(f.cl, CLS_IMAGE_SPAN);
            if (spanCls == null) return 0;
            Object[] spans = ed.getSpans(0, ed.length(), spanCls);
            return spans == null ? 0 : spans.length;
        } catch (Throwable t) {
            return 0;
        }
    }

    public static boolean clearInput() {
        try {
            EditText et = currentEdit();
            if (et == null) return false;
            et.setText("");
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean openFullScreen() {
        FeatureChatMix f = SHARED;
        return f != null && f.enterFullScreen();
    }

    public static boolean hasAioContext() {
        return aioCtx != null;
    }

    public static boolean hasInput() {
        return currentEdit() != null;
    }

    private static volatile FeatureChatMix SHARED;

    private static void toast(String s) {        try {
            CatToast.show(s);
        } catch (Throwable ignored) {
            try {
                Context cx = c();
                if (cx != null) {
                    android.widget.Toast.makeText(cx, s, android.widget.Toast.LENGTH_SHORT).show();
                }
            } catch (Throwable ignore2) {}
        }
    }
}
