package com.cathelperawa.owo.app.feature;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.cathelperawa.owo.app.bridge.CatToast;
import com.cathelperawa.owo.app.bridge.QQEnv;
import com.cathelperawa.owo.app.core.HookApi;
import com.cathelperawa.owo.app.core.HookConfig;
import com.cathelperawa.owo.app.core.HookLog;
import com.cathelperawa.owo.app.util.Refl;

import java.lang.reflect.Method;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;

public final class PicLinkPick {

    private PicLinkPick() {
    }

    public static final String K = "pic_link_pick";
    private static final String T_TITLE = "提取图片链接";

    private static final String CLS_MENU_ITEM = "com.tencent.qqnt.aio.menu.d";

    private static final String[] LAYOUTS = {
            "com.tencent.qqnt.aio.menu.ui.QQCustomMenuExpandableLayout",
            "com.tencent.qqnt.aio.menu.ui.QQCustomMenuNoIconLayout"
    };

    private static final java.util.Map<Object, String> ITEMS =
            java.util.Collections.synchronizedMap(new java.util.WeakHashMap<Object, String>());

    private static volatile boolean installed = false;
    private static ClassLoader sCl;

    public static synchronized void install(ClassLoader cl) {
        if (installed || cl == null) return;
        sCl = cl;
        int ok = 0;
        try {
            for (String ln : LAYOUTS) {
                Class<?> cls = HookApi.findClass(cl, ln);
                if (cls == null) continue;
                
                hookRender(cls);
                Method setMenu = HookApi.findMethod(cls, "setMenu");
                if (setMenu == null) continue;
                HookApi.before(setMenu, new HookApi.Action() {
                    @Override public void call(XC_MethodHook.MethodHookParam p) {
                        try {
                            onMenu(p);
                        } catch (Throwable t) {
                            HookLog.w("PicLinkPick.onMenu: " + t);
                        }
                    }
                });
                ok++;
            }
            installed = ok > 0;
            HookLog.i("PicLinkPick: 挂了 " + ok + " 个菜单布局");
        } catch (Throwable t) {
            HookLog.e("PicLinkPick.install 失败", t);
        }
    }

    private static void onMenu(XC_MethodHook.MethodHookParam p) {
        Context c = QQEnv.appContext();
        if (c == null && p.thisObject instanceof View) {
            c = ((View) p.thisObject).getContext();
        }
        if (c == null) return;
        if (!HookConfig.getBoolean(c, K, false)) return;

        Object uiE = p.args.length > 0 ? p.args[0] : null;
        if (uiE == null) return;
        List<Object> items = findList(uiE);
        if (items == null || items.isEmpty()) return;
        Object msgItem = findMsg(items.get(0));
        if (msgItem == null) return;

        synchronized (ITEMS) {
            for (Object o : items) {
                if (o != null && ITEMS.containsKey(o)) return;
            }
        }

        String url = pickUrl(msgItem);
        if (url == null || url.length() == 0) return;   

        Class<?> menuD = HookApi.findClass(sCl, CLS_MENU_ITEM);
        Object item;
        if (com.cathelperawa.owo.app.core.Host.isQQ()) {
            
            item = com.cathelperawa.owo.app.util.MenuKit.clone(items.get(0));
            com.cathelperawa.owo.app.util.MenuKit.setTitle(item, T_TITLE);
        } else {
            if (menuD == null) return;
            item = buildItem(menuD, c, msgItem);
            if (item != null) setStr(item, T_TITLE);
        }
        if (item == null) {
            dbg("菜单项构造失败");
            return;
        }
        ITEMS.put(item, url);
        items.add(0, item);
        dbg("插入菜单项 " + url);
    }

    private static Object buildItem(Class<?> menuD, Context c, Object msgItem) {
        try {
            java.lang.reflect.Constructor<?> k =
                    menuD.getDeclaredConstructor(Context.class, msgItem.getClass());
            k.setAccessible(true);
            return k.newInstance(c, msgItem);
        } catch (Throwable ignore) {
        }
        for (java.lang.reflect.Constructor<?> k : menuD.getDeclaredConstructors()) {
            try {
                k.setAccessible(true);
                Class<?>[] ps = k.getParameterTypes();
                Object[] as = new Object[ps.length];
                for (int i = 0; i < ps.length; i++) {
                    if (ps[i].isInstance(msgItem)) as[i] = msgItem;
                    else if (c != null && ps[i].isInstance(c)) as[i] = c;
                    else if (ps[i] == String.class) as[i] = T_TITLE;
                    else if (ps[i] == boolean.class) as[i] = false;
                    else if (ps[i] == int.class) as[i] = 0;
                    else if (ps[i] == long.class) as[i] = 0L;
                    else as[i] = null;
                }
                return k.newInstance(as);
            } catch (Throwable ignore) {
            }
        }
        return null;
    }

    private static void setStr(Object item, String v) {
        Class<?> c = item.getClass();
        while (c != null && c != Object.class) {
            for (java.lang.reflect.Field f : c.getDeclaredFields()) {
                if (f.getType() != String.class) continue;
                try {
                    f.setAccessible(true);
                    f.set(item, v);
                } catch (Throwable ignore) {
                }
            }
            c = c.getSuperclass();
        }
    }

    private static void hookRender(Class<?> cls) {
        for (final Method m : cls.getDeclaredMethods()) {
            Class<?>[] ps = m.getParameterTypes();
            if (ps.length != 4) continue;
            if (ps[0] != int.class || ps[2] != boolean.class || ps[3] != float[].class) continue;
            if (!View.class.isAssignableFrom(m.getReturnType())) continue;
            HookApi.before(m, new HookApi.Action() {
                @Override public void call(XC_MethodHook.MethodHookParam p) {
                    try {
                        Object f = p.args[1];
                        if (f == null) return;
                        final String url;
                        synchronized (ITEMS) {
                            url = ITEMS.get(f);
                        }
                        if (url == null) return;
                        Context c = QQEnv.appContext();
                        if (c == null && p.thisObject instanceof View) {
                            c = ((View) p.thisObject).getContext();
                        }
                        p.setResult(makeView(c, url));
                    } catch (Throwable t) {
                        HookLog.w("PicLinkPick.render: " + t);
                    }
                }
            });
        }
    }

    private static View makeView(final Context c, final String url) {
        if (c == null) return null;
        float d = c.getResources().getDisplayMetrics().density;
        LinearLayout box = new LinearLayout(c);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setPadding((int) (6 * d), (int) (6 * d), (int) (6 * d), (int) (6 * d));

        ImageView iv = new ImageView(c);
        try {
            iv.setImageDrawable(com.cathelperawa.owo.app.util.ModIcon.ic(c));
        } catch (Throwable ignore) {
        }
        box.addView(iv, new LinearLayout.LayoutParams((int) (26 * d), (int) (26 * d)));

        TextView tv = new TextView(c);
        tv.setText(T_TITLE);
        tv.setTextColor(0xFFE6EDF6);
        tv.setTextSize(11);
        tv.setSingleLine(true);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        tp.topMargin = (int) (3 * d);
        box.addView(tv, tp);

        box.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                copy(c, url);
            }
        });
        box.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        return box;
    }

    private static void copy(Context c, String url) {
        try {
            ClipboardManager cm =
                    (ClipboardManager) c.getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm == null) return;
            cm.setPrimaryClip(ClipData.newPlainText("pic_link", url));
            CatToast.show(url.contains("gchat.qpic.cn")
                    ? "已复制（md5 直链，打不开就长按换一张）"
                    : "图片链接已复制");
            dbg("复制 " + url);
        } catch (Throwable t) {
            HookLog.w("PicLinkPick.copy: " + t);
        }
    }

    private static String pickUrl(Object msgItem) {
        try {
            Object rec = Refl.call(msgItem, "getMsgRecord");
            if (rec == null) rec = Refl.field(msgItem, "msgRecord");
            if (rec == null) return null;
            Object els = Refl.field(rec, "elements");
            if (!(els instanceof List)) els = Refl.field(rec, "msgElements");
            if (!(els instanceof List)) els = Refl.call(rec, "getElements");
            if (!(els instanceof List)) {
                dbg("拿不到 elements(" + rec.getClass().getSimpleName() + ")");
                return null;
            }
            for (Object el : (List<?>) els) {
                if (el == null) continue;
                Object pic = Refl.field(el, "picElement");
                if (pic != null) {
                    
                    for (String g : new String[]{"getOriginImageUrl", "getBigPicUrl",
                            "getThumbUrl", "getSourcePicUrl", "getFileUrl", "getLocalPath"}) {
                        Object v = Refl.call(pic, g);
                        if (v instanceof String && ((String) v).startsWith("http")) {
                            dbg("命中 " + g + " -> " + v);
                            return (String) v;
                        }
                    }
                    
                    for (String f : new String[]{"originImageUrl", "bigUrl", "thumbUrl",
                            "sourcePicUrl", "url", "fileUrl"}) {
                        String s = Refl.firstStrOr(pic, "", f);
                        if (s != null && s.startsWith("http")) {
                            dbg("命中字段 " + f + " -> " + s);
                            return s;
                        }
                    }
                    
                    String md5 = Refl.firstStrOr(pic, "", "md5HexStr", "md5", "md5Str");
                    if (md5 == null) {
                        Object v = Refl.call(pic, "getMd5HexStr");
                        if (v instanceof String) md5 = (String) v;
                    }
                    dbg("picElement dump: " + dumpPic(pic));
                    if (md5 != null && md5.length() == 32) {
                        return "https://gchat.qpic.cn/gchatpic_new/0/0-0-"
                                + md5.toUpperCase() + "/0";
                    }
                    continue;
                }
                Object mf = Refl.field(el, "marketFaceElement");
                if (mf != null) {
                    for (String f : new String[]{"url", "bigUrl", "staticFaceUrl"}) {
                        String s = Refl.firstStrOr(mf, "", f);
                        if (s != null && s.startsWith("http")) return s;
                    }
                    dbg("表情无链接: " + mf);
                }
            }
        } catch (Throwable t) {
            dbg("pickUrl 异常: " + t);
        }
        return null;
    }

    private static String dumpPic(Object pic) {
        StringBuilder sb = new StringBuilder();
        try {
            for (java.lang.reflect.Method m : pic.getClass().getMethods()) {
                if (m.getParameterTypes().length != 0) continue;
                String n = m.getName();
                if (!n.startsWith("get")) continue;
                Class<?> r = m.getReturnType();
                if (r != String.class && r != int.class && r != long.class
                        && r != boolean.class) continue;
                try {
                    Object v = m.invoke(pic);
                    if (v == null || String.valueOf(v).length() == 0) continue;
                    String sv = String.valueOf(v);
                    if (sv.length() > 90) sv = sv.substring(0, 90) + "…";
                    sb.append(n).append("=").append(sv).append(" | ");
                } catch (Throwable ignore) {
                }
            }
        } catch (Throwable ignore) {
        }
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private static List<Object> findList(Object uiE) {
        Class<?> c = uiE.getClass();
        while (c != null && c != Object.class) {
            for (java.lang.reflect.Field f : c.getDeclaredFields()) {
                if (!List.class.isAssignableFrom(f.getType())) continue;
                try {
                    f.setAccessible(true);
                    Object v = f.get(uiE);
                    if (v instanceof List && !((List<?>) v).isEmpty()) return (List<Object>) v;
                } catch (Throwable ignore) {
                }
            }
            c = c.getSuperclass();
        }
        return null;
    }

    private static Object findMsg(Object item) {
        if (item == null) return null;
        Class<?> c = item.getClass();
        while (c != null && c != Object.class) {
            for (java.lang.reflect.Field f : c.getDeclaredFields()) {
                if (!f.getType().getName().contains("AIOMsgItem")) continue;
                try {
                    f.setAccessible(true);
                    Object v = f.get(item);
                    if (v != null) return v;
                } catch (Throwable ignore) {
                }
            }
            c = c.getSuperclass();
        }
        return null;
    }

    private static void dbg(String s) {
        try {
            Context c = QQEnv.appContext();
            if (c == null) return;
            java.io.File f = new java.io.File(c.getFilesDir(), "piclink_debug.txt");
            if (f.length() > 150 * 1024) f.delete();
            java.io.FileOutputStream o = new java.io.FileOutputStream(f, true);
            try {
                String t = new java.text.SimpleDateFormat("MM-dd HH:mm:ss",
                        java.util.Locale.US).format(new java.util.Date());
                o.write((t + " " + s + "\n").getBytes("UTF-8"));
            } finally {
                try {
                    o.close();
                } catch (Throwable ignore) {
                }
            }
        } catch (Throwable ignore) {
        }
    }
}
