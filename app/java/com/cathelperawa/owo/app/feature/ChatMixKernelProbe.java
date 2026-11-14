package com.cathelperawa.owo.app.feature;

import com.cathelperawa.owo.app.core.dex.ClassResolver;
import android.content.Context;
import android.widget.EditText;

import com.cathelperawa.owo.app.MsgStore;
import com.cathelperawa.owo.app.bridge.ElementFactory;
import com.cathelperawa.owo.app.bridge.QQEnv;
import com.cathelperawa.owo.app.core.HookApi;
import com.cathelperawa.owo.app.core.HookLog;
import com.cathelperawa.owo.app.util.Refl;

import java.io.File;
import java.io.FileWriter;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import de.robv.android.xposed.XC_MethodHook;

public final class ChatMixKernelProbe {

    private ChatMixKernelProbe() {}

    private static final String TAG = "[内核图文探针] ";

    private static final String CLS_CPPPROXY =
            "com.tencent.qqnt.kernel.nativeinterface.IKernelMsgService$CppProxy";
    private static final String CLS_ELEMENT =
            "com.tencent.qqnt.kernel.nativeinterface.MsgElement";
    private static final String CLS_IMAGE_SPAN =
            "com.tencent.mobileqq.aio.input.fullscreen.a";

    private static volatile String lastScene = "";

    public static int install(ClassLoader cl) {
        try {
            Class<?> cp = HookApi.findClass(cl, CLS_CPPPROXY);
            if (cp == null) {
                log("未找到 IKernelMsgService$CppProxy");
                return 0;
            }
            int n = 0;
            for (final Method m : cp.getDeclaredMethods()) {
                if (!"sendMsg".equals(m.getName())) continue;
                Class<?>[] pt = m.getParameterTypes();
                if (pt.length < 3 || !ArrayList.class.isAssignableFrom(pt[2])) continue;
                HookApi.before(m, new HookApi.Action() {
                    @Override public void call(XC_MethodHook.MethodHookParam p) {
                        try {

                            handle(p);
                        } catch (Throwable t) { log("handle 异常 " + t); }
                    }
                });
                n++;
            }
            log("sendMsg 挂载 " + n + " 个重载");
            return n;
        } catch (Throwable t) {
            log("install 异常 " + t);
            return 0;
        }
    }

    private static final java.util.HashMap<String, Long> INSERTED =
            new java.util.HashMap<String, Long>();

    private static volatile long LAST_BLOCK = 0L;

    private static final long BLOCK_COOLDOWN_MS = 1500L;

    private static final long GUARD_MS = 4000L;

    private static boolean isJustInserted(String path) {
        synchronized (INSERTED) {
            Long t = INSERTED.get(path);
            if (t == null) return false;
            if (System.currentTimeMillis() - t > GUARD_MS) {
                INSERTED.remove(path);
                return false;
            }
            return true;
        }
    }

    private static void markInserted(String path) {
        synchronized (INSERTED) {
            long now = System.currentTimeMillis();
            java.util.Iterator<java.util.Map.Entry<String, Long>> it =
                    INSERTED.entrySet().iterator();
            while (it.hasNext()) {
                if (now - it.next().getValue() > GUARD_MS) it.remove();
            }
            INSERTED.put(path, now);
        }
    }

    private static String picKey(Object pic, String path) {
        if (pic != null) {
            Object md5 = Refl.field(pic, "md5HexStr");
            if (md5 instanceof String) {
                String m = ((String) md5).trim().toLowerCase(Locale.US);
                if (m.length() > 0) return "md5:" + m;
            }
        }
        return "path:" + path;
    }

    private static boolean hasInvisibleText(List<Object> elements) {
        try {
            for (Object e : elements) {
                if (e == null) continue;
                Object type = Refl.field(e, "elementType");
                if (!(type instanceof Number) || ((Number) type).intValue() != 1) continue;
                Object te = Refl.field(e, "textElement");
                if (te == null) continue;
                Object c = Refl.field(te, "content");
                if (!(c instanceof String)) continue;
                String s = (String) c;
                String plain = s.replace("\u200B", "").replace("\u200C", "")
                               .replace("\uFEFF", "").replace("\n", "").replace("\r", "");
                if (plain.isEmpty() && !s.isEmpty()) return true;
            }
        } catch (Throwable ignore) {}
        return false;
    }

    private static int countPics(List<Object> elements) {
        int n = 0;
        try {
            for (Object e : elements) {
                if (e == null) continue;
                Object type = Refl.field(e, "elementType");
                if (type instanceof Number && ((Number) type).intValue() == 2) n++;
            }
        } catch (Throwable ignore) {}
        return n;
    }

    private static void stripInvisibleText(List<Object> elements) {
        try {
            boolean removed = false;
            for (int i = elements.size() - 1; i >= 0; i--) {
                Object e = elements.get(i);
                if (e == null) continue;
                Object type = Refl.field(e, "elementType");
                if (!(type instanceof Number) || ((Number) type).intValue() != 1) continue;
                Object te = Refl.field(e, "textElement");
                if (te == null) continue;
                Object c = Refl.field(te, "content");
                if (!(c instanceof String)) continue;
                String s = (String) c;
                String plain = s.replace("\u200B", "").replace("\u200C", "")
                               .replace("\uFEFF", "").replace("\n", "").replace("\r", "");
                if (plain.isEmpty() && !s.isEmpty()) {
                    elements.remove(i);
                    removed = true;
                }
            }
            if (removed) log("  · 已剔除零宽文本元素（保持纯图片渲染）");
        } catch (Throwable t) {
            log("stripInvisibleText 异常 " + t);
        }
    }

    private static boolean handle(XC_MethodHook.MethodHookParam p) {
        Object arr = (p.args != null && p.args.length > 2) ? p.args[2] : null;
        if (!(arr instanceof List)) { return false; }
        @SuppressWarnings("unchecked")
        List<Object> elements = (List<Object>) arr;

        if (FeatureChatMix.on()) {
            log("  [call] thread=" + Thread.currentThread().getName()
                    + " n=" + elements.size()
                    + " ui=" + (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()));
        }

        if (!FeatureChatMix.on()) return false;
        if (FeatureChatMix.isReplying()) return false;

        boolean quoting = FeatureChatMix.quoteBarVisible();

        if (hasInvisibleText(elements) && countPics(elements) >= 1) {
            stripInvisibleText(elements);
            log("  → 用户点发送（图+零宽占位），已清理并放行");
            return false;
        }

        if (elements.size() != 1) return false;

        Object e = elements.get(0);
        if (e == null) return false;
        Object type = Refl.field(e, "elementType");
        if (!(type instanceof Number) || ((Number) type).intValue() != 2) return false;

        Object pic = Refl.field(e, "picElement");
        Object pathObj = pic == null ? null : Refl.field(pic, "sourcePath");
        if (!(pathObj instanceof String) || ((String) pathObj).length() == 0) return false;
        String path = (String) pathObj;
        String key = picKey(pic, path);

        EditText et = FeatureChatMix.edit();
        if (!shouldInsert(et, quoting)) {
            log("  → 输入框没内容/未开图文模式/未引用，放行（quoting=" + quoting + "）");
            return false;
        }

        boolean isGif = path.toLowerCase().endsWith(".gif");
        boolean ok = FeatureChatMix.insertPic(path, isGif);
        if (!ok) { log("  → 插入失败，放行原发送"); return false; }

        elements.clear();
        log("  ★ 已拦截并插入输入框: " + path);
        toast("图塞进框里了喵～");
        return true;
    }

    private static void scrubInvisible(List<Object> elements) {
        try {
            for (int i = elements.size() - 1; i >= 0; i--) {
                Object e = elements.get(i);
                if (e == null) continue;
                Object type = Refl.field(e, "elementType");
                if (!(type instanceof Number) || ((Number) type).intValue() != 1) continue;

                Object te = Refl.field(e, "textElement");
                if (te == null) continue;
                Object contentObj = Refl.field(te, "content");
                if (!(contentObj instanceof String)) continue;

                String c = (String) contentObj;
                String cleaned = c.replace("\u200B", "").replace("\u200C", "")
                                  .replace("\uFEFF", "");
                if (cleaned.trim().isEmpty()) {
                    cleaned = cleaned.replace("\n", "").replace("\r", "");
                }
                if (cleaned.equals(c)) continue;

                if (cleaned.length() == 0) {
                    cleaned = "";
                    Refl.setField(te, "content", cleaned);
                    log("  清洗：文本清空 → 空字符串");
                } else {
                    Refl.setField(te, "content", cleaned);
                    log("  清洗：文本内容 [" + c.replace("\n", "\\n")
                            + "] → [" + cleaned.replace("\n", "\\n") + "]");
                }
            }
        } catch (Throwable t) {
            log("scrubInvisible 异常 " + t);
        }
    }

    private static void ensureTextElement(List<Object> elements) {
        try {
            if (elements.isEmpty()) return;
            for (Object e : elements) {
                if (e == null) continue;
                Object t = Refl.field(e, "elementType");
                if (t instanceof Number && ((Number) t).intValue() == 1) return;
            }
            Object first = elements.get(0);
            if (first == null) return;
            ClassLoader cl = first.getClass().getClassLoader();
            Class<?> cls = ClassResolver.find(cl, "qqnt.msgElement",
                    "com.tencent.qqnt.kernel.nativeinterface.MsgElement");
            Object te = cls.getDeclaredConstructor().newInstance();
            Refl.setField(te, "elementType", 1);
            Class<?> txtCls = ClassResolver.find(cl, "qqnt.textElement",
                    "com.tencent.qqnt.kernel.nativeinterface.TextElement");
            Object txt = txtCls.getDeclaredConstructor().newInstance();
            Refl.setField(txt, "content", "");
            Refl.setField(te, "textElement", txt);
            elements.add(te);
            log("  补齐：追加空文本元素（纯图片 → 图片+文本）");
        } catch (Throwable t) {
            log("ensureTextElement 异常 " + t);
        }
    }

    private static boolean shouldInsert(EditText et, boolean quoting) {
        if (et == null) return false;
        if (FeatureChatMix.always()) return true;
        if (quoting) return true;
        CharSequence cs = et.getText();
        return cs != null && cs.length() > 0;
    }

    private static void toast(final String s) {
        try {

            com.cathelperawa.owo.app.bridge.CatToast.show(s);
        } catch (Throwable t) {
            log("CatToast 失败，退化为系统 Toast: " + t);
            try {
                android.os.Handler h = new android.os.Handler(android.os.Looper.getMainLooper());
                h.post(new Runnable() {
                    @Override public void run() {
                        try {
                            Context c = QQEnv.appContext();
                            if (c != null) {
                                android.widget.Toast.makeText(c, s,
                                        android.widget.Toast.LENGTH_SHORT).show();
                            }
                        } catch (Throwable ignore) {}
                    }
                });
            } catch (Throwable ignore) {}
        }
    }

    private static void dump(List<Object> elements) {
        StringBuilder sb = new StringBuilder();
        sb.append("==== sendMsg  elements=").append(elements.size()).append('\n');

        for (int i = 0; i < elements.size(); i++) {
            Object e = elements.get(i);
            if (e == null) { sb.append("  [").append(i).append("] null\n"); continue; }
            Object type = Refl.field(e, "elementType");
            sb.append("  [").append(i).append("] elementType=").append(type);
            Object pic = Refl.field(e, "picElement");
            if (pic != null) sb.append("\n       pic=").append(dumpPic(pic));
            Object face = Refl.field(e, "faceElement");
            if (face != null) sb.append("\n       face=").append(dumpSimple(face));
            Object mface = Refl.field(e, "marketFaceElement");
            if (mface != null) sb.append("\n       marketFace=").append(dumpSimple(mface));
            Object text = Refl.field(e, "textElement");
            if (text != null) sb.append("\n       text=").append(dumpSimple(text));
            sb.append('\n');
        }

        sb.append(scene());
        lastScene = sb.toString();

        String out = sb.toString();
        log(out);
        try { MsgStore.add(TAG + out); } catch (Throwable ignore) {}
    }

    private static String dumpPic(Object pic) {
        StringBuilder sb = new StringBuilder();
        sb.append("sourcePath=").append(Refl.field(pic, "sourcePath"));
        sb.append(" fileSize=").append(Refl.field(pic, "fileSize"));
        sb.append(" picType=").append(Refl.field(pic, "picType"));
        sb.append(" summary=").append(Refl.field(pic, "summary"));
        Object md5 = Refl.field(pic, "md5HexStr");
        if (md5 != null) sb.append(" md5=").append(md5);
        return sb.toString();
    }

    private static String dumpSimple(Object o) {
        StringBuilder sb = new StringBuilder(o.getClass().getSimpleName()).append('{');
        try {
            for (java.lang.reflect.Field f : o.getClass().getFields()) {
                Class<?> t = f.getType();
                if (t != String.class && t != int.class && t != long.class
                        && t != boolean.class && t != byte[].class) continue;
                f.setAccessible(true);
                Object v = f.get(o);
                if (v instanceof byte[]) v = "bytes[" + ((byte[]) v).length + "]";
                if (v == null || "".equals(v)) continue;
                sb.append(f.getName()).append('=').append(v).append(' ');
            }
        } catch (Throwable ignore) {}
        return sb.append('}').toString();
    }

    private static String scene() {
        StringBuilder sb = new StringBuilder("  --- 输入框现场 ---\n");
        try {
            EditText et = currentEdit();
            if (et == null) {
                sb.append("  edit=null（拿不到输入框）\n");
            } else {
                CharSequence cs = et.getText();
                String s = cs == null ? "" : cs.toString();
                sb.append("  edit.length=").append(cs == null ? -1 : cs.length());
                sb.append(" 文字=[")
                  .append(s.length() > 40 ? s.substring(0, 40) + "..." : s)
                  .append("]\n");
                if (cs instanceof android.text.Spanned) {
                    int n = 0;
                    try {
                        Class<?> spanCls = HookApi.findClass(QQEnv.classLoader(), CLS_IMAGE_SPAN);
                        if (spanCls != null) {
                            Object[] spans = ((android.text.Spanned) cs)
                                    .getSpans(0, cs.length(), spanCls);
                            n = spans == null ? 0 : spans.length;
                        }
                    } catch (Throwable ignore) {}
                    sb.append("  imageSpan=").append(n).append('\n');
                }
            }
            sb.append("  isReplying=").append(FeatureChatMix.isReplying()).append('\n');
        } catch (Throwable t) {
            sb.append("  采集异常 ").append(t).append('\n');
        }
        return sb.toString();
    }

    private static EditText currentEdit() {
        return FeatureChatMix.edit();
    }

    private static void log(String s) {
        HookLog.i(TAG + s);
        try {
            Context c = QQEnv.appContext();
            if (c == null) return;

            File d = com.cathelperawa.owo.app.core.DataDir.global("diag");
            if (d == null) return;
            File f = new File(d, "mixkernel.txt");
            if (f.length() > 512 * 1024) { File b = new File(f.getPath() + ".1"); if (b.exists()) b.delete(); f.renameTo(b); }
            FileWriter w = new FileWriter(f, true);
            w.write(new SimpleDateFormat("HH:mm:ss", Locale.US).format(new Date())
                    + "  " + s + "\n");
            w.close();
        } catch (Throwable ignore) {}
    }
}
