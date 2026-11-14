package com.cathelperawa.owo.app.feature;

import android.content.Context;
import com.cathelperawa.owo.app.core.Bg;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.cathelperawa.owo.app.MsgStore;
import com.cathelperawa.owo.app.api.IoApi;
import com.cathelperawa.owo.app.api.MsgApi;
import com.cathelperawa.owo.app.bridge.CatToast;
import com.cathelperawa.owo.app.core.HookApi;
import com.cathelperawa.owo.app.core.HookConfig;
import com.cathelperawa.owo.app.core.HookItem;
import com.cathelperawa.owo.app.core.HookItemBase;
import com.cathelperawa.owo.app.core.HookLog;
import com.cathelperawa.owo.app.ui.CatDialog;
import com.cathelperawa.owo.app.ui.CatSheet;
import com.cathelperawa.owo.app.ui.ParsePanel;
import com.cathelperawa.owo.app.util.Refl;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.robv.android.xposed.XC_MethodHook;

@HookItem(name = "解析功能", path = "解析功能/视频", order = 86,
        description = "视频链接自动识别 · 解析卡片 · 直接发视频", defaultEnabled = false)
public class FeatureParse extends HookItemBase {

    public static final String K_ON = "pk_video_on";
    public static final String K_SEND = "pk_video_send";
    public static final String K_MAX = "pk_video_max";
    public static final String K_APIS = "pk_video_apis";
    public static final String K_PLAT = "pk_video_plat";

    public static final String K_M_ON = "pk_music_on";
    public static final String K_M_SEND = "pk_music_send";
    public static final String K_M_BR = "pk_music_br";
    public static final String K_M_API = "pk_music_api";
    public static final String K_M_COUNT = "pk_music_count";

    public static final String K_CARD_FALLBACK = "pk_card_fallback";

    public static final String K_M_APIS = "pk_music_apis";

    public static final String K_M_CONV = "pk_music_conv";

    public static final String K_M_FAKE_SEC = "pk_music_fake_sec";

    public static final String K_M_SEC = "pk_music_sec";

    private static final String T_PARSE = "解析喵~";

    private static final String BILI_VIEW = "https://api.bilibili.com/x/web-interface/view?bvid=";
    private static final String BILI_PLAY =
            "https://api.bilibili.com/x/player/playurl?bvid=%s&cid=%s"
                    + "&qn=64&fnval=1&platform=html5&high_quality=1";
    private static final String UA_PC =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                    + "(KHTML, like Gecko) Chrome/124.0 Safari/537.36";

    private static final String UA_DY =
            "Mozilla/5.0 (Linux; Android 13; Pixel 6) AppleWebKit/537.36 "
                    + "(KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36";

    private static boolean isByteUrl(String u) {
        if (u == null) return false;
        String s = u.toLowerCase();
        return s.contains("douyin") || s.contains("snssdk") || s.contains("amemv")
                || s.contains("byteicdn") || s.contains("bytecdn") || s.contains("pstatp")
                || s.contains("zjcdn") || s.contains("ixigua") || s.contains("bytedance");
    }

    private static void hdr(java.net.HttpURLConnection c, String url) {
        if (isByteUrl(url)) {
            c.setRequestProperty("User-Agent", UA_DY);
            c.setRequestProperty("Referer", "https://www.douyin.com/");
        } else {
            c.setRequestProperty("User-Agent", UA_PC);
            c.setRequestProperty("Referer", "https://www.bilibili.com");
        }
    }

    private static volatile String sLastDlErr = "";

    private static volatile String sBiliCookie = "";

    private static final Pattern P_NETEASE_SONG = Pattern.compile(
            "music\\.163\\.com[^\"'\\s]*?[?&]id=(\\d+)");
    private static final Pattern P_ARK_TITLE = Pattern.compile(
            "\"title\"\\s*:\\s*\"([^\"]{1,80})\"");
    private static final Pattern P_ARK_DESC = Pattern.compile(
            "\"desc\"\\s*:\\s*\"([^\"]{1,80})\"");
    private static final Pattern P_BVID = Pattern.compile("(BV[0-9A-Za-z]{10})");
    private static final Pattern P_BILI_URL = Pattern.compile(
            "https?://(?:b23\\.tv|bili2233\\.cn|www\\.bilibili\\.com/video)/[0-9A-Za-z]+");
    private static final Pattern P_DOUYIN = Pattern.compile(
            "(?:https?://)?(?:v\\.douyin\\.com|www\\.iesdouyin\\.com|www\\.douyin\\.com)"
                    + "/[^\\s　-鿿，。！？、]+");
    private static final Pattern P_XHS = Pattern.compile(
            "https?://(?:xhslink\\.com|www\\.xiaohongshu\\.com)/[^\\s　-鿿，。！？、]+");

    private static Map<Object, Object[]> MENU_ITEMS;

    private static final String CLS_EDIT_TEXT = "com.tencent.mobileqq.aio.input.edit.AIOEditText";

    private static final Handler UI = new Handler(Looper.getMainLooper());

    private static final Map<View, Boolean> MIC_HOOKED =
            java.util.Collections.synchronizedMap(new java.util.WeakHashMap<View, Boolean>());

    private static final Map<View, View.OnLongClickListener> PREV_LONG_CLICK =
            java.util.Collections.synchronizedMap(
                    new java.util.WeakHashMap<View, View.OnLongClickListener>());

    @Override
    public String switchKey() { return K_ON; }

    private boolean on() {
        return HookConfig.getBoolean(ctx, K_ON, false);
    }

    private String sendMode() {
        String s = HookConfig.get(ctx, K_SEND, "video");
        return s == null || s.trim().length() == 0 ? "video" : s.trim();
    }

    private String videoSendMode() {
        String s = sendMode();
        if ("voice".equals(s)) return "voice";
        if ("link".equals(s)) return "link";
        if ("card".equals(s)) return "card";
        return "video";
    }

    private static String biliAudioUrl(String bvid, String cid) {
        if (bvid == null || bvid.length() == 0 || cid == null || cid.length() == 0) return "";
        try {
            String u = "https://api.bilibili.com/x/player/playurl?bvid=" + bvid
                    + "&cid=" + cid + "&fnval=16&fnver=0&fourk=0";
            org.json.JSONObject root = new org.json.JSONObject(httpGet(u, 15000));
            org.json.JSONObject d = root.optJSONObject("data");
            if (d == null) return "";
            org.json.JSONObject dash = d.optJSONObject("dash");
            if (dash == null) return "";
            JSONArray audio = dash.optJSONArray("audio");
            if (audio == null || audio.length() == 0) return "";
            String best = "";
            long bestBw = -1;
            for (int i = 0; i < audio.length(); i++) {
                JSONObject a = audio.optJSONObject(i);
                if (a == null) continue;
                long bw = a.optLong("bandwidth", 0);
                String bu = a.optString("baseUrl", "");
                if (bu.length() == 0) bu = a.optString("base_url", "");
                if (bu.length() > 0 && bw >= bestBw) {
                    bestBw = bw;
                    best = bu;
                }
            }
            return best;
        } catch (Throwable t) {
            HookLog.w("解析: biliAudioUrl 失败 " + t);
            return "";
        }
    }

    private int maxMB() {
        try {
            String s = HookConfig.get(ctx, K_MAX, "100");
            int v = Integer.parseInt(s.trim());
            return v <= 0 ? 100 : v;
        } catch (Throwable t) {
            return 50;
        }
    }

    private String platCsv() {
        String s = HookConfig.get(ctx, K_PLAT, "bili");
        return s == null || s.length() == 0 ? "bili" : s;
    }

    private boolean platOn(String k) {
        String csv = platCsv();
        for (String s : csv.split(",")) {
            if (s.trim().equals(k)) return true;
        }
        return false;
    }

    private List<String[]> customs() {
        List<String[]> out = new ArrayList<String[]>();
        try {
            String raw = HookConfig.get(ctx, K_APIS, "");
            if (raw == null || raw.trim().length() == 0) return out;
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o == null) continue;
                String n = o.optString("n", "接口" + (i + 1));
                String u = o.optString("u", "");
                if (u.trim().length() == 0) continue;
                out.add(new String[]{n, u.trim()});
            }
        } catch (Throwable t) {
            HookLog.w("解析: 接口列表解析失败 " + t);
        }
        return out;
    }

    private void cleanParseCache() {
        try {
            Bg.run(new Runnable() {
                @Override public void run() {
                    try {
                        File dir = new File(IoApi.dataDir(), "parse");
                        File[] fs = dir.listFiles();
                        if (fs == null) return;
                        long now = System.currentTimeMillis();
                        java.util.List<File> files = new java.util.ArrayList<File>();
                        for (File f : fs) {
                            if (f != null && f.isFile()) files.add(f);
                        }

                        java.util.Collections.sort(files, new java.util.Comparator<File>() {
                            @Override public int compare(File a, File b) {
                                return Long.compare(b.lastModified(), a.lastModified());
                            }
                        });
                        int n = 0;
                        long freed = 0;
                        for (int i = 0; i < files.size(); i++) {
                            File f = files.get(i);
                            boolean expired = now - f.lastModified() > 24L * 3600 * 1000;
                            boolean tooMany = i >= 5;
                            if (!expired && !tooMany) continue;
                            long sz = f.length();
                            if (f.delete()) {
                                n++;
                                freed += sz;
                            }
                        }
                        if (n > 0) {
                            HookLog.i("解析缓存: 清掉 " + n + " 个旧文件 / "
                                    + (freed / 1024 / 1024) + "MB");
                        }
                    } catch (Throwable t) {
                        HookLog.w("解析缓存清理失败: " + t);
                    }
                }
            }, "cat-clean-parse");
        } catch (Throwable ignore) {
        }
    }

    @Override
    public void entry(ClassLoader cl) {
        try {
            MENU_ITEMS = new IdentityHashMap<Object, Object[]>();
            boolean ok = hookMenu(cl);
            MsgStore.add("[解析] 菜单挂载 " + (ok ? "成功" : "失败"));
            boolean ok2 = hookInputBar(cl);
            MsgStore.add("[解析] 话筒长按挂载 " + (ok2 ? "成功" : "失败"));
            hookTouchEvent(cl);
            cleanParseCache();
        } catch (Throwable t) {
            HookLog.e("解析: entry 异常", t);
            MsgStore.add("[解析] entry 异常 " + t);
        }
    }

    @Override
    public boolean isEnabled() {

        return true;
    }

    private boolean hookInputBar(ClassLoader c) {
        try {
            Class<?> cls = HookApi.findClass(c, CLS_EDIT_TEXT);
            if (cls == null) {
                HookLog.w("解析: 未找到 AIOEditText");
                return false;
            }
            int n = 0;
            for (java.lang.reflect.Constructor<?> ctor : cls.getDeclaredConstructors()) {
                try {
                    HookApi.after(ctor, new HookApi.Action() {
                        @Override public void call(XC_MethodHook.MethodHookParam p) {
                            try {
                                Object o = p.thisObject;
                                if (o instanceof EditText) armMic((EditText) o);
                            } catch (Throwable ignore) {
                            }
                        }
                    }, this);
                    n++;
                } catch (Throwable ignore) {
                }
            }
            return n > 0;
        } catch (Throwable t) {
            HookLog.e("解析: 输入栏挂载异常", t);
            return false;
        }
    }

    private void armMic(final EditText et) {
        UI.postDelayed(new Runnable() {
            @Override public void run() {
                tryArmMic(et, 0);
            }
        }, 800);
    }

    private void tryArmMic(final EditText et, final int attempt) {
        try {
            if (ctx == null) return;
            if (et.getWindowToken() == null) {
                if (attempt < 8) retryMic(et, attempt);
                return;
            }
            View mic = findMic(et);
            if (mic == null) {
                if (attempt < 8) {
                    retryMic(et, attempt);
                } else {
                    MsgStore.add("[解析] 8 次都没找到话筒按钮");
                    dumpInputBar(et, "找不到话筒");
                }
                return;
            }
            armMicView(mic);
            dumpInputBar(et, "已挂话筒");
        } catch (Throwable t) {
            HookLog.e("解析: tryArmMic 异常", t);
        }
    }

    private void dumpInputBar(EditText et, String why) {
        try {
            View root = et.getRootView();
            if (root == null || ctx == null) return;
            List<View> all = new ArrayList<View>();
            collectViews(root, all);
            Rect etRect = new Rect();
            et.getGlobalVisibleRect(etRect);

            StringBuilder sb = new StringBuilder();
            sb.append("=== 解析功能 · 输入栏 dump ===\n");
            sb.append("时间: ").append(new java.util.Date()).append('\n');
            sb.append("状态: ").append(why).append('\n');
            sb.append("输入框: ").append(et.getClass().getName())
                    .append(' ').append(rectStr(etRect)).append('\n');
            sb.append("共 ").append(all.size()).append(" 个 View\n\n");

            for (View v : all) {
                if (!v.isShown()) continue;
                Rect r = new Rect();
                if (!v.getGlobalVisibleRect(r)) continue;
                boolean near = Math.abs(r.centerY() - etRect.centerY()) < dp(60)
                        && r.left < etRect.left + dp(40);
                if (!near) continue;
                sb.append(v.getClass().getName())
                        .append(' ').append(rectStr(r))
                        .append(" clickable=").append(v.isClickable())
                        .append(" longClickable=").append(v.isLongClickable())
                        .append(" touch=").append(hasTouchListener(v))
                        .append(" desc=").append(v.getContentDescription())
                        .append('\n');
            }
            java.io.File f = new java.io.File(com.cathelperawa.owo.app.api.IoApi.dataDir(), "parse_diag.txt");
            java.io.FileOutputStream fo = new java.io.FileOutputStream(f, false);
            fo.write(sb.toString().getBytes("UTF-8"));
            fo.flush();
            fo.close();
            MsgStore.add("[解析] dump 已写 " + (sb.length() / 1024) + "KB");
        } catch (Throwable t) {
            HookLog.w("解析: dumpInputBar 失败 " + t);
        }
    }

    private static String rectStr(Rect r) {
        return "[" + r.left + "," + r.top + "," + r.right + "," + r.bottom + "]";
    }

    private static String hasTouchListener(View v) {
        try {
            java.lang.reflect.Field f = View.class.getDeclaredField("mListenerInfo");
            f.setAccessible(true);
            Object li = f.get(v);
            if (li == null) return "none";
            java.lang.reflect.Field ft = li.getClass().getDeclaredField("mOnTouchListener");
            ft.setAccessible(true);
            Object tl = ft.get(li);
            java.lang.reflect.Field fl = li.getClass().getDeclaredField("mOnLongClickListener");
            fl.setAccessible(true);
            Object ll = fl.get(li);
            return (tl == null ? "-" : tl.getClass().getName())
                    + (ll == null ? "" : " +long");
        } catch (Throwable t) {
            return "?";
        }
    }

    private void retryMic(final EditText et, final int attempt) {
        UI.postDelayed(new Runnable() {
            @Override public void run() {
                tryArmMic(et, attempt + 1);
            }
        }, 800);
    }

    private void armMicView(final View mic) {
        if (mic == null) return;

        View.OnLongClickListener prev = longClickListenerOf(mic);
        if (prev != null && prev != MIC_CLICK) {
            synchronized (PREV_LONG_CLICK) {
                PREV_LONG_CLICK.put(mic, prev);
            }
        }
        mic.setOnLongClickListener(MIC_CLICK);
        synchronized (MIC_HOOKED) {
            MIC_HOOKED.put(mic, Boolean.TRUE);
        }
        MsgStore.add("[解析] 话筒长按已挂: " + mic.getClass().getSimpleName()
                + " desc=" + mic.getContentDescription()
                + (prev == null ? "" : " · 已接管原长按(" + prev.getClass().getSimpleName() + ")"));
    }

    private static View.OnLongClickListener longClickListenerOf(View v) {
        try {
            java.lang.reflect.Field f = View.class.getDeclaredField("mListenerInfo");
            f.setAccessible(true);
            Object li = f.get(v);
            if (li == null) return null;
            java.lang.reflect.Field fl = li.getClass().getDeclaredField("mOnLongClickListener");
            fl.setAccessible(true);
            Object l = fl.get(li);
            return (l instanceof View.OnLongClickListener) ? (View.OnLongClickListener) l : null;
        } catch (Throwable t) {
            return null;
        }
    }

    private static final View.OnLongClickListener MIC_CLICK = new View.OnLongClickListener() {
        @Override public boolean onLongClick(View v) {
            try {

                if (!ParsePanel.anyOn(v.getContext())) {
                    View.OnLongClickListener prev;
                    synchronized (PREV_LONG_CLICK) {
                        prev = PREV_LONG_CLICK.get(v);
                    }
                    return prev != null && prev.onLongClick(v);
                }
                ParsePanel.show(v.getContext());
                return true;
            } catch (Throwable t) {
                HookLog.e("解析: 面板打开失败", t);
            }
            return false;
        }
    };

    private void hookTouchEvent(ClassLoader c) {
        try {
            Class<?> viewCls = Class.forName("android.view.View");
            java.lang.reflect.Method m = viewCls.getDeclaredMethod(
                    "onTouchEvent", android.view.MotionEvent.class);
            m.setAccessible(true);
            hookBefore(m, new HookApi.Action() {
                @Override public void call(XC_MethodHook.MethodHookParam p) {
                    try {
                        if (inSynthetic) return;
                        Object o = p.thisObject;
                        if (!(o instanceof android.widget.ImageButton)) return;
                        final android.view.View v = (android.view.View) o;
                        CharSequence cd = v.getContentDescription();
                        if (cd == null) return;
                        String s = cd.toString();
                        if (!s.contains("语音") && !s.contains("按住说话")) return;
                        if (ctx == null || !ParsePanel.anyOn(ctx)) return;

                        android.view.MotionEvent e = (android.view.MotionEvent) p.args[0];
                        int act = e.getActionMasked();

                        if (act == android.view.MotionEvent.ACTION_DOWN) {
                            micPressed = true;
                            micConsumed = false;
                            final android.view.View target = v;
                            final float x = e.getRawX(), y = e.getRawY();
                            UI.removeCallbacks(micTimer);
                            micTimer = new Runnable() {
                                @Override public void run() {
                                    try {
                                        if (!micPressed || micConsumed) return;
                                        micConsumed = true;

                                        sendCancel(target, x, y);
                                        ParsePanel.show(target.getContext());
                                    } catch (Throwable t) {
                                        HookLog.e("解析: 话筒定时器异常", t);
                                    }
                                }
                            };
                            UI.postDelayed(micTimer, 700);
                            return;
                        }

                        if (act == android.view.MotionEvent.ACTION_UP
                                || act == android.view.MotionEvent.ACTION_CANCEL) {
                            micPressed = false;
                            UI.removeCallbacks(micTimer);
                            if (micConsumed) {
                                micConsumed = false;
                                p.setResult(Boolean.TRUE);
                            }
                        }
                    } catch (Throwable ignore) {
                    }
                }
            });
            HookLog.i("解析: 话筒触摸已拦");
        } catch (Throwable t) {
            HookLog.e("解析: hookTouchEvent 失败", t);
        }
    }

    private static volatile boolean inSynthetic = false;
    private static volatile boolean micPressed = false;
    private static volatile boolean micConsumed = false;
    private static Runnable micTimer;

    private static void sendCancel(android.view.View v, float x, float y) {
        try {
            inSynthetic = true;
            long now = android.os.SystemClock.uptimeMillis();
            android.view.MotionEvent cancel = android.view.MotionEvent.obtain(
                    now, now, android.view.MotionEvent.ACTION_CANCEL, x, y, 0);
            v.onTouchEvent(cancel);
            cancel.recycle();
        } catch (Throwable ignore) {
        } finally {
            inSynthetic = false;
        }
    }

    private View findMic(EditText et) {
        View root = et.getRootView();
        if (root == null) return null;
        Rect etRect = new Rect();
        if (!et.getGlobalVisibleRect(etRect)) return null;

        List<View> all = new ArrayList<View>();
        collectViews(root, all);

        for (int i = 0; i < all.size(); i++) {
            View v = all.get(i);
            if (!v.isShown() || !v.isClickable()) continue;
            CharSequence cd = v.getContentDescription();
            if (cd == null) continue;
            String s = cd.toString().toLowerCase(java.util.Locale.US);
            if (s.contains("按住说话") || s.contains("语音") || s.contains("voice")
                    || s.contains("mic") || s.contains("说话")) {
                return v;
            }
        }

        View best = null;
        int bestRight = -1;
        for (int i = 0; i < all.size(); i++) {
            View v = all.get(i);
            if (v == et || !v.isShown() || !v.isClickable()) continue;
            if (!(v instanceof ImageView) && !v.isClickable()) continue;
            Rect r = new Rect();
            if (!v.getGlobalVisibleRect(r)) continue;
            int w = r.width(), h = r.height();
            if (w < dp(18) || h < dp(18)) continue;
            if (w > dp(96) || h > dp(96)) continue;
            if (r.right > etRect.left + dp(10)) continue;
            if (r.right > bestRight) {
                bestRight = r.right;
                best = v;
            }
        }
        return best;
    }

    private static void collectViews(View v, List<View> out) {
        out.add(v);
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) {
                View c = g.getChildAt(i);
                if (c != null) collectViews(c, out);
            }
        }
    }

    private int dp(float v) {
        try {
            return (int) (v * ctx.getResources().getDisplayMetrics().density);
        } catch (Throwable t) {
            return (int) v * 3;
        }
    }

    private String[] detect(String text) {
        if (text == null || text.length() == 0) return null;
        try {

            String t = text.replace("\\/", "/").replace("\\u002F", "/").replace("\\u002f", "/");
            if (platOn("bili")) {
                Matcher m = P_BILI_URL.matcher(t);
                if (m.find()) return new String[]{"bili", m.group()};

                Matcher bv = P_BVID.matcher(t);
                if (bv.find()) return new String[]{"bili", bv.group(1)};
            }
            if (platOn("douyin")) {
                Matcher m = P_DOUYIN.matcher(t);
                if (m.find()) {
                    String u = m.group();

                    if (!u.startsWith("http")) u = "https://" + u;
                    return new String[]{"douyin", u};
                }
            }
            if (platOn("xhs")) {
                Matcher m = P_XHS.matcher(t);
                if (m.find()) return new String[]{"xhs", m.group()};
            }

            if (musicShareOn()) {
                Matcher ns = P_NETEASE_SONG.matcher(t);
                if (ns.find()) {
                    String sid = ns.group(1);
                    String title = firstGroup(P_ARK_TITLE, t);
                    String desc = firstGroup(P_ARK_DESC, t);
                    return new String[]{"netease",
                            "id=" + sid
                                    + "&t=" + enc(title)
                                    + "&a=" + enc(desc)};
                }
            }
        } catch (Throwable t) {
            HookLog.e("解析: detect 异常", t);
        }
        return null;
    }

    private boolean musicShareOn() {
        try {
            String v = HookConfig.get(ctx, K_M_ON, "1");
            return !"0".equals(v);
        } catch (Throwable t) {
            return true;
        }
    }

    private static String firstGroup(Pattern p, String text) {
        try {
            Matcher m = p.matcher(text);
            if (m.find()) {
                String v = m.group(1);
                return v == null ? "" : v.trim();
            }
        } catch (Throwable ignore) {
        }
        return "";
    }

    private static String enc(String s) {
        if (s == null || s.length() == 0) return "";
        try {
            return java.net.URLEncoder.encode(s, "UTF-8");
        } catch (Throwable t) {
            return "";
        }
    }

    private static String textOf(Object msgItem) {
        return scanOf(msgItem, false);
    }

    private static String scanOf(Object msgItem, boolean deep) {
        try {
            Object rec = recordOf(msgItem);
            if (rec == null) return "";
            StringBuilder sb = new StringBuilder();

            Object elsObj = Refl.field(rec, "elements");
            if (elsObj instanceof List) {
                for (Object el : (List<?>) elsObj) {
                    if (el == null) continue;
                    Object te = Refl.field(el, "textElement");
                    if (te != null) {
                        Object c = Refl.field(te, "content");
                        if (c != null) sb.append(String.valueOf(c)).append(' ');
                    }

                    Object ark = Refl.field(el, "arkElement");
                    if (ark != null) {
                        Object bd = Refl.field(ark, "bytesData");
                        if (bd != null) sb.append(String.valueOf(bd)).append(' ');
                    }

                    Object sm = Refl.field(el, "structMsgElement");
                    if (sm != null) {
                        Object c = Refl.field(sm, "content");
                        if (c != null) sb.append(String.valueOf(c)).append(' ');
                        Object u = Refl.field(sm, "xmlContent");
                        if (u != null) sb.append(String.valueOf(u)).append(' ');
                    }
                }
            }
            if (deep) {

                walkStrings(rec, sb, 0);
            }
            return sb.toString();
        } catch (Throwable t) {
            return "";
        }
    }

    private static void walkStrings(Object o, StringBuilder sb, int depth) {
        if (o == null || depth > 4 || sb.length() > 40000) return;
        try {
            if (o instanceof String) {
                sb.append((String) o).append(' ');
                return;
            }
            if (o instanceof List) {
                for (Object x : (List<?>) o) walkStrings(x, sb, depth + 1);
                return;
            }
            if (o instanceof Map) {
                for (Object x : ((Map<?, ?>) o).values()) walkStrings(x, sb, depth + 1);
                return;
            }
            Class<?> c = o.getClass();
            if (c.getName().startsWith("java.") || c.getName().startsWith("android.")) return;
            while (c != null && c != Object.class) {
                for (java.lang.reflect.Field f : c.getDeclaredFields()) {
                    if (java.lang.reflect.Modifier.isStatic(f.getModifiers())) continue;
                    Class<?> ft = f.getType();
                    if (ft != String.class && !List.class.isAssignableFrom(ft)
                            && !Map.class.isAssignableFrom(ft)) continue;
                    try {
                        f.setAccessible(true);
                        Object v = f.get(o);
                        if (v != null) walkStrings(v, sb, depth + 1);
                    } catch (Throwable ignore) {
                    }
                }
                c = c.getSuperclass();
            }
        } catch (Throwable ignore) {
        }
    }

    private boolean hookMenu(ClassLoader c) {
        Class<?> expand = HookApi.findClass(c,
                "com.tencent.qqnt.aio.menu.ui.QQCustomMenuExpandableLayout");
        if (expand == null) {
            HookLog.w("解析: 未找到菜单布局");
            return false;
        }
        Method setMenu = HookApi.findMethod(expand, "setMenu");
        if (setMenu == null) {
            HookLog.w("解析: 未找到 setMenu");
            return false;
        }
        hookBefore(setMenu, new HookApi.Action() {
            @Override public void call(XC_MethodHook.MethodHookParam p) { onMenu(p); }
        });
        hookMenuRender(c);
        HookLog.i("解析: 菜单已挂");
        return true;
    }

    private void onMenu(XC_MethodHook.MethodHookParam p) {
        try {
            if (!on()) return;
            Context c = ctx;
            if (c == null) {
                if (p.thisObject instanceof View) {
                    c = ((View) p.thisObject).getContext();
                }
                if (c == null) return;
            }
            Object uiE = p.args.length > 0 ? p.args[0] : null;
            if (uiE == null) return;
            List<Object> items = findList(uiE);
            if (items == null || items.isEmpty()) return;
            Object msgItem = findMsg(items.get(0));
            if (msgItem == null) return;

            String[] hit = detect(scanOf(msgItem, true));
            if (hit == null) {

                try {
                    String blob = scanOf(msgItem, true).replace("\\/", "/");

                    String safe = blob.replaceAll("https?://[^\\s\"'<>]+", "<链接已隐藏>");
                    MsgStore.add("[解析喵] 本条无链接｜扫到 " + blob.length() + " 字"
                            + "｜含b23=" + blob.contains("b23")
                            + "｜片段:" + (safe.length() > 180 ? safe.substring(0, 180) : safe));
                } catch (Throwable ignore) {
                }
                return;
            }
            MsgStore.add("[解析喵] 识别到 " + hit[0] + " → " + mask(hit[1]));

            Class<?> menuD = HookApi.findClass(cl, "com.tencent.qqnt.aio.menu.d");
            if (menuD == null) return;
            insertMenu(menuD, c, msgItem, items, hit[1]);
        } catch (Throwable t) {
            HookLog.e("解析: 菜单注入异常", t);
        }
    }

    private void insertMenu(Class<?> menuD, Context c, Object msgItem,
                            List<Object> items, String url) {
        try {

            synchronized (MENU_ITEMS) {
                for (Object o : items) {
                    if (o != null && MENU_ITEMS.containsKey(o)) return;
                }
            }
            Object item = buildMenuItem(menuD, c, msgItem);
            if (item == null) {
                MsgStore.add("[解析] buildMenuItem 返回 null");
                return;
            }
            setFieldByType(item, boolean.class, false);
            setFieldByType(item, int.class, 0);
            setFieldByType(item, String.class, T_PARSE);
            synchronized (MENU_ITEMS) {
                MENU_ITEMS.put(item, new Object[]{msgItem, T_PARSE, url});
            }
            items.add(0, item);
            MsgStore.add("[解析] 菜单项已插入 " + mask(url));
        } catch (Throwable t) {
            MsgStore.add("[解析] 插入失败 " + t);
            HookLog.e("解析: 插入菜单项失败", t);
        }
    }

    private Object buildMenuItem(Class<?> menuD, Context c, Object msgItem) {

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
                    else if (ps[i] == String.class) as[i] = T_PARSE;
                    else if (ps[i] == boolean.class) as[i] = false;
                    else if (ps[i] == int.class) as[i] = 0;
                    else if (ps[i] == long.class) as[i] = 0L;
                    else as[i] = null;
                }
                Object o = k.newInstance(as);
                if (o != null) return o;
            } catch (Throwable ignore) {
            }
        }
        return null;
    }

    private void hookMenuRender(ClassLoader c) {
        String[] hosts = new String[]{
                "com.tencent.qqnt.aio.menu.ui.QQCustomMenuNoIconLayout",
                "com.tencent.qqnt.aio.menu.ui.QQCustomMenuExpandableLayout",
        };
        for (String hn : hosts) {
            Class<?> cls = HookApi.findClass(c, hn);
            if (cls == null) continue;
            for (final Method m : cls.getDeclaredMethods()) {
                Class<?>[] ps = m.getParameterTypes();
                if (ps.length != 4) continue;
                if (ps[0] != int.class || ps[2] != boolean.class || ps[3] != float[].class) continue;
                if (!View.class.isAssignableFrom(m.getReturnType())) continue;
                hookBefore(m, new HookApi.Action() {
                    @Override public void call(XC_MethodHook.MethodHookParam p) {
                        try {
                            Object f = p.args[1];
                            if (f == null) return;
                            Object[] rec;
                            synchronized (MENU_ITEMS) { rec = MENU_ITEMS.get(f); }
                            if (rec == null) return;
                            Context c = ctx;
                            if (p.thisObject instanceof View) {
                                Context c2 = ((View) p.thisObject).getContext();
                                if (c2 != null) c = c2;
                            }
                            p.setResult(makeMenuView(c, rec));
                        } catch (Throwable t) {
                            HookLog.e("解析: 菜单渲染失败", t);
                        }
                    }
                });
            }
        }
    }

    private View makeMenuView(final Context c, final Object[] rec) {
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
        tv.setText(T_PARSE);
        tv.setTextColor(0xFF9AA7B8);
        tv.setTextSize(11);
        tv.setSingleLine(true);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        tp.topMargin = (int) (3 * d);
        box.addView(tv, tp);

        final Object msgItem = rec[0];
        final String url = (String) rec[2];
        box.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                try {
                    parseAndShow(c, msgItem, url);
                } catch (Throwable t) {
                    HookLog.e("解析: 点击异常", t);
                }
            }
        });
        box.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        return box;
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

    private static Object recordOf(Object msgItem) {
        if (msgItem == null) return null;
        try {
            Method g = msgItem.getClass().getMethod("getMsgRecord");
            g.setAccessible(true);
            return g.invoke(msgItem);
        } catch (Throwable t) {
            return null;
        }
    }

    private static String titleOf(Object item) {
        if (item == null) return "";
        try {
            for (Method m : item.getClass().getMethods()) {
                if (m.getParameterTypes().length != 0) continue;
                if (!String.class.equals(m.getReturnType())) continue;
                String n = m.getName().toLowerCase();
                if (!n.contains("title") && !n.equals("a") && !n.contains("text")) continue;
                try {
                    m.setAccessible(true);
                    Object v = m.invoke(item);
                    if (v instanceof String) return (String) v;
                } catch (Throwable ignore) {
                }
            }
        } catch (Throwable ignore) {
        }
        return "";
    }

    private static void setFieldByType(Object obj, Class<?> type, Object val) {
        try {
            Class<?> c = obj.getClass();
            while (c != null && c != Object.class) {
                for (java.lang.reflect.Field f : c.getDeclaredFields()) {
                    if (f.getType() != type) continue;
                    if (java.lang.reflect.Modifier.isStatic(f.getModifiers())) continue;
                    try {
                        f.setAccessible(true);
                        f.set(obj, val);

                    } catch (Throwable ignore) {
                    }
                }
                c = c.getSuperclass();
            }
        } catch (Throwable ignore) {
        }
    }

    private static void toast(String s) {
        try { CatToast.show(s); } catch (Throwable ignore) { }
    }

    private static String mask(String url) {
        if (url == null || url.length() == 0) return "";
        try {
            String s = url.replace("\\/", "/");
            int i = s.indexOf("://");
            if (i > 0) s = s.substring(i + 3);
            int j = s.indexOf('/');
            String host = j > 0 ? s.substring(0, j) : s;
            return host + "/…";
        } catch (Throwable t) {
            return "…";
        }
    }

    private static final class Info {
        String platform = "";
        String title = "";
        String author = "";
        String cover = "";
        String desc = "";
        String duration = "";
        String stat = "";
        String videoUrl = "";
        String pageUrl = "";
        String bvid = "";
        String cid = "";
        String playApi = "";
        String err = "";
    }

    private void parseAndShow(final Context c, final Object msgItem, final String url) {

        if (url != null && url.startsWith("id=")) {
            directMusicFromCard(c, msgItem, url);
            return;
        }
        toast("解析中…");
        sBiliCookie = com.cathelperawa.owo.app.bili.BiliApi.cookie(c);
        Bg.run(new Runnable() {
            @Override public void run() {
                final Info info = new Info();
                try {
                    parse(url, info);
                } catch (Throwable t) {
                    info.err = "解析异常：" + t;
                    HookLog.e("解析: parse 异常", t);
                }
                MsgStore.add("[解析] " + (info.err.length() > 0 ? "失败 " + info.err
                        : "成功 " + info.title + " / 直链" + (info.videoUrl.length() > 0 ? "有" : "无")));
                final Context cc = c;
                final Object mi = msgItem;
                ui(new Runnable() {
                    @Override public void run() {
                        if (info.err.length() > 0) {
                            CatDialog.info(cc, "解析失败", info.err
                                    + "\n\n（B站走官方接口，抖音走内置解析；小红书要填自建服务）");
                            return;
                        }
                        showCard(cc, mi, info);
                    }
                });
            }
        }, "cat-parse");
    }

    private void directMusicFromCard(final Context c, final Object msgItem, String payload) {
        String id = "";
        String name = "";
        String artist = "";
        try {
            for (String kv : payload.split("&")) {
                int eq = kv.indexOf('=');
                if (eq <= 0) continue;
                String k = kv.substring(0, eq);
                String v = kv.substring(eq + 1);
                try {
                    v = java.net.URLDecoder.decode(v, "UTF-8");
                } catch (Throwable ignore) {
                }
                if ("id".equals(k)) id = v;
                else if ("t".equals(k)) name = v;
                else if ("a".equals(k)) artist = v;
            }
        } catch (Throwable ignore) {
        }

        if (id.length() == 0) {
            toast("卡片里没读到歌曲 ID，改用搜索");
            String kw = (name + " " + artist).trim();
            if (kw.length() == 0) {
                toast("这张卡片里没有歌名");
                return;
            }
            searchAndPick(c, msgItem, kw);
            return;
        }

        MusicApi.Song song = new MusicApi.Song();
        song.id = id;
        song.name = name.length() == 0 ? ("歌曲 " + id) : name;
        song.artist = artist;
        MsgStore.add("[解析喵] 网易云卡片 → " + song.name
                + (artist.length() > 0 ? " - " + artist : "") + "（id=" + id + "）");
        toast("发歌中…");
        dispatchMusic(c, msgItem, song);
    }

    private void parse(String url, Info info) throws Exception {
        String[] hit = detect(url);
        String platform = hit == null ? "" : hit[0];
        String link = hit == null ? url : hit[1];
        info.platform = platform;
        info.pageUrl = link;

        if ("bili".equals(platform)) {
            parseBili(link, info);
            if (info.err.length() == 0) return;
            HookLog.w("解析: B站失败 " + info.err + "，转自定义接口");
        }

        if ("douyin".equals(platform)) {
            parseDouyin(link, info);
            if (info.err.length() == 0) return;
            HookLog.w("解析: 抖音内置失败 " + info.err + "，转自定义接口");
        }

        List<String[]> apis = customs();
        for (String[] api : apis) {
            try {
                if (parseCustom(api[0], api[1], link, info)) return;
            } catch (Throwable t) {
                HookLog.w("解析: 自定义接口 " + api[0] + " 失败 " + t);
            }
        }
        if (info.err.length() == 0) {
            info.err = apis.isEmpty()
                    ? ("没有可用的解析接口。\n平台：" + (platform.length() == 0 ? "未识别" : platform))
                    : "所有接口都失败了";
        }
    }

    private void parseBili(String link, Info info) throws Exception {
        String bvid = link;
        Matcher m = P_BVID.matcher(link);
        if (m.find()) {
            bvid = m.group(1);
        } else {

            String real = follow(link);
            Matcher m2 = P_BVID.matcher(real == null ? "" : real);
            if (m2.find()) bvid = m2.group(1);
        }
        if (bvid == null || bvid.length() < 10 || !bvid.startsWith("BV")) {
            info.err = "没从链接里认出 BV 号";
            return;
        }
        info.bvid = bvid;

        String body = httpGet(BILI_VIEW + bvid, 15000);
        JSONObject root = new JSONObject(body);
        if (root.optInt("code", -1) != 0) {
            info.err = "B站接口返回 " + root.optInt("code") + "：" + root.optString("message");
            return;
        }
        JSONObject d = root.optJSONObject("data");
        if (d == null) { info.err = "B站返回空数据"; return; }

        info.title = d.optString("title", "");
        info.desc = d.optString("desc", "");
        info.cover = d.optString("pic", "");
        info.pageUrl = "https://www.bilibili.com/video/" + d.optString("bvid", bvid);
        info.cid = String.valueOf(d.optLong("cid", 0));
        JSONObject owner = d.optJSONObject("owner");
        if (owner != null) info.author = owner.optString("name", "");
        long sec = d.optLong("duration", 0);
        info.duration = fmtDur(sec);
        JSONObject stat = d.optJSONObject("stat");
        if (stat != null) {
            info.stat = "▶ " + fmtNum(stat.optLong("view"))
                    + "　❤ " + fmtNum(stat.optLong("like"));
        }

        try {
            String pb = httpGet(String.format(BILI_PLAY, bvid, info.cid), 15000);
            JSONObject pr = new JSONObject(pb);
            JSONObject pd = pr.optJSONObject("data");
            if (pd != null) {
                JSONArray durl = pd.optJSONArray("durl");
                if (durl != null && durl.length() > 0) {
                    JSONObject u0 = durl.optJSONObject(0);
                    if (u0 != null) {
                        info.videoUrl = u0.optString("url", "");
                        long sz = u0.optLong("size", 0);
                        if (sz > 0) {
                            info.desc = (info.desc.length() > 0 ? info.desc + "\n" : "")
                                    + "文件约 " + (sz / 1048576) + " MB";
                        }
                    }
                }
            }
        } catch (Throwable t) {
            HookLog.w("解析: playurl 失败 " + t);
        }
        info.err = "";
    }

    private void parseDouyin(String link, Info info) {
        android.app.Activity act = activityOf(ctx);
        if (act == null) {
            info.err = "抖音解析要有个前台界面（当前拿不到 Activity）";
            return;
        }
        DouyinWeb.Result r = DouyinWeb.resolveSync(act, link, 32000);
        if (!r.ok()) {
            info.err = r.error.length() == 0 ? "抖音页面没给出播放地址" : r.error;
            return;
        }
        info.platform = "抖音";
        info.title = r.title.length() == 0 ? "抖音视频" : r.title;
        info.cover = r.cover;
        info.videoUrl = r.url;
        info.desc = "";
        info.author = "";
        info.pageUrl = link;
        info.playApi = r.apiUrl;
        info.err = "";
        HookLog.i("解析: 抖音内置 OK " + (r.title.length() > 30
                ? r.title.substring(0, 30) : r.title));
    }

    private boolean parseCustom(String name, String api, String link, Info info) throws Exception {
        String target = api.contains("{url}")
                ? api.replace("{url}", java.net.URLEncoder.encode(link, "UTF-8"))
                : (api.contains("?") ? api + "&url=" + java.net.URLEncoder.encode(link, "UTF-8")
                : api + "?url=" + java.net.URLEncoder.encode(link, "UTF-8"));
        String body = httpGet(target, 20000);
        if (body == null || body.length() < 10) return false;
        JSONObject root = new JSONObject(body);

        Info tmp = new Info();
        tmp.platform = name;
        tmp.pageUrl = link;

        findStr(root, tmp, 0);
        if (tmp.title.length() == 0 && tmp.videoUrl.length() == 0) return false;

        info.platform = name;
        info.title = tmp.title;
        info.author = tmp.author;
        info.cover = tmp.cover;
        info.desc = tmp.desc;
        info.videoUrl = tmp.videoUrl;
        info.err = "";
        return true;
    }

    private static void findStr(Object o, Info out, int depth) {
        if (o == null || depth > 6) return;
        if (o instanceof JSONObject) {
            JSONObject jo = (JSONObject) o;
            java.util.Iterator<String> it = jo.keys();
            while (it.hasNext()) {
                String k = it.next();
                Object v = jo.opt(k);
                String lk = k.toLowerCase();
                if (v instanceof String) {
                    String s = (String) v;
                    if (s.length() == 0) continue;
                    if (out.title.length() == 0 && ("title".equals(lk) || "desc".equals(lk)
                            || "caption".equals(lk))) {
                        out.title = s;
                    } else if (out.cover.length() == 0 && ("cover".equals(lk)
                            || "cover_url".equals(lk) || "pic".equals(lk)
                            || "thumbnail".equals(lk))) {
                        out.cover = s;
                    } else if (out.videoUrl.length() == 0 && ("video_url".equals(lk)
                            || "play_url".equals(lk) || "url".equals(lk)
                            || "video".equals(lk) || "nwm_video_url".equals(lk)
                            || "download_url".equals(lk))) {
                        if (s.startsWith("http")) out.videoUrl = s;
                    } else if (out.author.length() == 0 && ("nickname".equals(lk)
                            || "author".equals(lk) || "name".equals(lk))) {
                        out.author = s;
                    }
                } else {
                    findStr(v, out, depth + 1);
                }
            }
        } else if (o instanceof JSONArray) {
            JSONArray a = (JSONArray) o;
            for (int i = 0; i < Math.min(a.length(), 5); i++) {
                findStr(a.opt(i), out, depth + 1);
            }
        }
    }

    private void showCard(final Context c, final Object msgItem, final Info info) {
        final android.app.Activity act = activityOf(c);
        if (act == null) {
            toast("拿不到界面上下文");
            return;
        }
        final float d = act.getResources().getDisplayMetrics().density;

        LinearLayout card = new LinearLayout(act);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding((int) (18 * d), (int) (16 * d), (int) (18 * d), (int) (14 * d));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xE6FFFFFF);
        bg.setCornerRadius(26 * d);
        bg.setStroke((int) d, 0xA6FFFFFF);
        card.setBackgroundDrawable(bg);
        card.setElevation(12 * d);

        LinearLayout head = new LinearLayout(act);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        TextView ttl = new TextView(act);
        ttl.setText("解析结果");
        ttl.setTextSize(17);
        ttl.setTextColor(0xFF3C4E6B);
        ttl.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        head.addView(ttl, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        if (info.platform.length() > 0) {
            TextView tag = new TextView(act);
            tag.setText("bili".equals(info.platform) ? "B站"
                    : ("bili".equals(info.platform) ? "B站" : info.platform));
            tag.setTextSize(10);
            tag.setTextColor(0xFFE85D9C);
            tag.setPadding((int) (8 * d), (int) (3 * d), (int) (8 * d), (int) (3 * d));
            GradientDrawable tg = new GradientDrawable();
            tg.setCornerRadius(9 * d);
            tg.setColor(0x59FFC2DF);
            tag.setBackgroundDrawable(tg);
            head.addView(tag);
        }
        LinearLayout.LayoutParams headLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        headLp.bottomMargin = (int) (12 * d);
        card.addView(head, headLp);

        ScrollView sv = new ScrollView(act);
        sv.setVerticalScrollBarEnabled(false);
        LinearLayout body = new LinearLayout(act);
        body.setOrientation(LinearLayout.VERTICAL);

        if (info.cover.length() > 0) {
            final ImageView iv = new ImageView(act);
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            iv.setClipToOutline(true);
            GradientDrawable cbg = new GradientDrawable();
            cbg.setCornerRadius(16 * d);
            cbg.setColor(0x14000000);
            iv.setBackgroundDrawable(cbg);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, (int) (168 * d));
            lp.bottomMargin = (int) (12 * d);
            body.addView(iv, lp);
            loadCover(iv, info.cover);
        }

        body.addView(tv(act, info.title.length() > 0 ? info.title : "（没有标题）",
                16, 0xFF2E3A4E, true, 8));
        LinearLayout meta = new LinearLayout(act);
        meta.setOrientation(LinearLayout.HORIZONTAL);
        meta.setGravity(Gravity.CENTER_VERTICAL);
        if (info.author.length() > 0) {
            meta.addView(chip(act, "UP主 " + info.author, 0xFF6E93D6, d), chipLp(d));
        }
        if (info.duration.length() > 0) {
            meta.addView(chip(act, info.duration, 0xFF8A9CB5, d), chipLp(d));
        }
        if (info.stat.length() > 0) {
            meta.addView(chip(act, info.stat.replace("　", " "), 0xFF8A9CB5, d), chipLp(d));
        }
        if (meta.getChildCount() > 0) {
            LinearLayout.LayoutParams mlp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            mlp.bottomMargin = (int) (10 * d);
            body.addView(meta, mlp);
        }

        if (info.desc.length() > 0) {
            String ds = info.desc.length() > 200 ? info.desc.substring(0, 200) + "…" : info.desc;
            TextView dt = tv(act, ds, 11, 0xFF8A9CB5, false, 10);
            dt.setLineSpacing((int) (3 * d), 1f);
            body.addView(dt);
        }

        boolean hasUrl = info.videoUrl.length() > 0;
        TextView st = tv(act, hasUrl ? "直链 · 已拿到 ✓" : "直链 · 没拿到（只能发卡片）",
                11, hasUrl ? 0xFF4CAF7D : 0xFFE06A8A, true, 0);
        body.addView(st);

        sv.addView(body);
        LinearLayout.LayoutParams svLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        svLp.bottomMargin = (int) (10 * d);
        card.addView(sv, svLp);

        final String[] vLabels = {"视频", "语音"};
        final String[] vKeys = {"video", "voice"};
        final String[] curMode = {videoSendMode()};
        final LinearLayout modeRow = new LinearLayout(act);
        modeRow.setOrientation(LinearLayout.HORIZONTAL);
        final TextView[] vsegs = new TextView[vKeys.length];
        for (int i = 0; i < vKeys.length; i++) {
            final int idx = i;
            TextView seg = pillBtn(act, d, vLabels[i], vKeys[i].equals(curMode[0]));
            if (!vKeys[i].equals(curMode[0])) {
                seg.setBackgroundDrawable(segBg(act, d, false));
                seg.setTextColor(0xFF8A9CB5);
            }
            seg.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    curMode[0] = vKeys[idx];
                    HookConfig.set(act, K_SEND, vKeys[idx]);
                    for (int j = 0; j < vsegs.length; j++) {
                        boolean on = j == idx;
                        vsegs[j].setBackgroundDrawable(segBg(act, d, on));
                        vsegs[j].setTextColor(on ? 0xFFE85D9C : 0xFF8A9CB5);
                    }
                }
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            if (i > 0) lp.leftMargin = (int) (4 * d);
            if (i < vKeys.length - 1) lp.rightMargin = (int) (4 * d);
            vsegs[i] = seg;
            modeRow.addView(seg, lp);
        }
        LinearLayout.LayoutParams mrlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        mrlp.bottomMargin = (int) (8 * d);
        card.addView(modeRow, mrlp);

        LinearLayout opts = new LinearLayout(act);
        opts.setOrientation(LinearLayout.HORIZONTAL);

        LinearLayout convBox = new LinearLayout(act);
        convBox.setOrientation(LinearLayout.HORIZONTAL);
        convBox.setGravity(Gravity.CENTER_VERTICAL);
        convBox.setPadding((int) (10 * d), (int) (8 * d), (int) (10 * d), (int) (8 * d));
        GradientDrawable convBg = new GradientDrawable();
        convBg.setCornerRadius(14 * d);
        convBg.setColor(0x59FFFFFF);
        convBg.setStroke((int) d, 0x66FFFFFF);
        convBox.setBackgroundDrawable(convBg);
        convBox.addView(tv(act, "转格式", 11, 0xFF6E8AAE, true, 4),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        final com.cathelperawa.owo.app.ui.SwitchBox convSw =
                new com.cathelperawa.owo.app.ui.SwitchBox(act, autoConvertOn());
        convSw.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                boolean z = !autoConvertOn();
                HookConfig.set(act, K_M_CONV, z ? "1" : "0");
                convSw.setChecked(z);
            }
        });
        convBox.addView(convSw, new LinearLayout.LayoutParams(dp(46), dp(26)));
        LinearLayout.LayoutParams clp2 = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        clp2.rightMargin = (int) (6 * d);
        opts.addView(convBox, clp2);

        final TextView durTv = tv(act, durLabel(), 11, 0xFF6E8AAE, true, 0);
        durTv.setGravity(Gravity.CENTER);
        durTv.setPadding((int) (10 * d), (int) (12 * d), (int) (10 * d), (int) (12 * d));
        GradientDrawable dbg2 = new GradientDrawable();
        dbg2.setCornerRadius(14 * d);
        dbg2.setColor(0x59FFFFFF);
        dbg2.setStroke((int) d, 0x66FFFFFF);
        durTv.setBackgroundDrawable(dbg2);
        durTv.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                CatSheet.input(act, "显示时长（秒）", "0 = 用真实时长，只改气泡显示",
                        String.valueOf(fakeDurationSec()), new CatSheet.OnText() {
                            @Override public void run(String s) {
                                int n = 0;
                                try { n = Integer.parseInt(s == null ? "" : s.trim()); }
                                catch (Throwable ignore) { }
                                HookConfig.set(act, K_M_FAKE_SEC, String.valueOf(Math.max(n, 0)));
                                durTv.setText(durLabel());
                            }
                        });
            }
        });
        LinearLayout.LayoutParams dlp2 = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        dlp2.leftMargin = (int) (6 * d);
        opts.addView(durTv, dlp2);

        LinearLayout.LayoutParams olp2 = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        olp2.bottomMargin = (int) (8 * d);
        card.addView(opts, olp2);

        final android.app.Dialog dlg = new android.app.Dialog(act,
                android.R.style.Theme_Translucent_NoTitleBar);

        final TextView saveV = pillBtn(act, d, "存音轨到语音库（不发送）", false);
        saveV.setTextSize(12);
        saveV.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                dlg.dismiss();
                saveVideoAudioToVault(act, info);
            }
        });
        LinearLayout.LayoutParams svLp2 = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        svLp2.bottomMargin = (int) (10 * d);
        card.addView(saveV, svLp2);

        LinearLayout btns = new LinearLayout(act);
        btns.setOrientation(LinearLayout.HORIZONTAL);

        TextView cancel = pillBtn(act, d, "取消", false);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { dlg.dismiss(); }
        });
        TextView ok = pillBtn(act, d, "发送", true);
        ok.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                dlg.dismiss();
                if ("voice".equals(curMode[0])) {
                    sendVideoVoice(act, msgItem, info);
                } else if (info.videoUrl.length() == 0) {
                    toast("这首还没拿到直链，稍后再试");
                } else {
                    sendVideoOf(act, msgItem, info);
                }
            }
        });
        LinearLayout.LayoutParams bl = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        bl.rightMargin = (int) (5 * d);
        LinearLayout.LayoutParams br = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        br.leftMargin = (int) (5 * d);
        btns.addView(cancel, bl);
        btns.addView(ok, br);
        card.addView(btns, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        dlg.setContentView(card);
        dlg.setCanceledOnTouchOutside(true);
        dlg.show();
        android.view.Window w = dlg.getWindow();
        if (w == null) return;
        w.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(
                android.graphics.Color.TRANSPARENT));
        w.addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        android.view.WindowManager.LayoutParams lp = w.getAttributes();
        int sw = act.getResources().getDisplayMetrics().widthPixels;
        int sh = act.getResources().getDisplayMetrics().heightPixels;
        lp.width = (int) (sw * 0.92f);
        lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        lp.gravity = Gravity.CENTER;
        lp.dimAmount = 0.24f;
        w.setAttributes(lp);
    }

    private static TextView chip(Context c, String s, int color, float d) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(11);
        t.setTextColor(color);
        t.setPadding((int) (9 * d), (int) (4 * d), (int) (9 * d), (int) (4 * d));
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(11 * d);
        g.setColor(0x66FFFFFF);
        t.setBackgroundDrawable(g);
        return t;
    }

    private static LinearLayout.LayoutParams chipLp(float d) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.rightMargin = (int) (6 * d);
        return lp;
    }

    private static TextView pillBtn(Context c, float d, String label, boolean primary) {
        TextView t = new TextView(c);
        t.setText(label);
        t.setTextSize(14);
        t.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        t.setGravity(Gravity.CENTER);
        t.setPadding(0, (int) (12 * d), 0, (int) (12 * d));
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(16 * d);
        if (primary) {
            g.setColor(0xFFFFC2DF);
            g.setStroke((int) d, 0xFFFF9FCB);
            t.setTextColor(0xFFE85D9C);
        } else {
            g.setColor(0x59FFFFFF);
            g.setStroke((int) d, 0x66FFFFFF);
            t.setTextColor(0xFF8A9CB5);
        }
        t.setBackgroundDrawable(g);
        return t;
    }

    private static android.app.Activity activityOf(Context c) {
        try {
            Context x = c;
            while (x instanceof android.content.ContextWrapper) {
                if (x instanceof android.app.Activity) return (android.app.Activity) x;
                x = ((android.content.ContextWrapper) x).getBaseContext();
                if (x == null) break;
            }
            if (c instanceof android.app.Activity) return (android.app.Activity) c;
        } catch (Throwable ignore) {
        }
        return null;
    }

    private static TextView tv(Context c, String s, float size, int color,
                               boolean bold, int bottomDp) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(size);
        t.setTextColor(color);
        if (bold) t.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        t.setLineSpacing(2f, 1f);
        if (bottomDp > 0) {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = bottomDp;
            t.setLayoutParams(lp);
        }
        return t;
    }

    private void loadCover(final ImageView iv, final String url) {
        Bg.run(new Runnable() {
            @Override public void run() {
                try {
                    byte[] b = httpBytes(url, 8000, 3 * 1024 * 1024);
                    if (b == null) return;
                    final Bitmap bmp = BitmapFactory.decodeByteArray(b, 0, b.length);
                    if (bmp == null) return;
                    iv.post(new Runnable() {
                        @Override public void run() { iv.setImageBitmap(bmp); }
                    });
                } catch (Throwable ignore) {
                }
            }
        }, "cat-cover");
    }

    public static void startFromPanel(final Context c, final String url) {
        try {
            FeatureParse f = new FeatureParse();
            f.ctx = c;
            f.cl = c.getClassLoader();
            f.parseAndShow(c, null, url);
        } catch (Throwable t) {
            HookLog.e("解析: startFromPanel 异常", t);
            toast("解析启动失败：" + t);
        }
    }

    private int targetChatType(Object msgItem) {
        if (msgItem != null) {
            int ct = MsgApi.chatTypeOf(msgItem);
            if (ct > 0) return ct;
        }
        ensureCurrentChat();
        return com.cathelperawa.owo.app.bridge.CurrentChat.chatType();
    }

    private String targetPeer(Object msgItem) {
        if (msgItem != null) {
            String p = MsgApi.peerUidOf(msgItem);
            if (p != null && p.length() > 0) return p;
        }
        ensureCurrentChat();
        return com.cathelperawa.owo.app.bridge.CurrentChat.peerUid();
    }

    private void ensureCurrentChat() {
        try {

            android.app.Activity act = activityOf(ctx);
            if (act != null && com.cathelperawa.owo.app.bridge.CurrentChat.fromActivity(act)) {
                return;
            }
            com.cathelperawa.owo.app.bridge.CurrentChat.has();
        } catch (Throwable ignore) {
        }
    }

    private void sendVideoVoice(final Context c, final Object msgItem, final Info info) {
        final boolean convert = autoConvertOn();
        toast(convert ? "下载音频并转语音中…" : "下载音频中…");
        Bg.run(new Runnable() {
            @Override public void run() {
                File src = null, out = null;
                try {
                    File dir = new File(IoApi.dataDir(), "parse");
                    if (!dir.exists()) dir.mkdirs();
                    long ts = System.currentTimeMillis();

                    String au = biliAudioUrl(info.bvid, info.cid);
                    String url = au.length() > 0 ? au : info.videoUrl;
                    if (url == null || url.length() == 0) { toast("没有可下载的地址"); return; }
                    boolean audioOnly = au.length() > 0;

                    src = new File(dir, "va_" + ts + (audioOnly ? ".m4s" : ".mp4"));
                    long got = downloadRetry(info, audioOnly, src, 80L * 1024 * 1024);
                    if (got <= 0) { toast("下载失败"); return; }
                    MsgStore.add("[解析] 音频下载 " + (got / 1024) + " KB"
                            + (audioOnly ? "（纯音轨）" : "（整段视频抽音轨）"));

                    int ct = targetChatType(msgItem);
                    String peer = targetPeer(msgItem);
                    if (peer == null || peer.length() == 0) { toast("拿不到会话"); return; }

                    int fake = fakeDurationSec() * 1000;

                    if (!convert) {

                        int br = 128;
                        int real = (int) Math.min(Integer.MAX_VALUE, got * 8L / br);
                        int dur = fake > 0 ? fake : Math.max(real, 1000);
                        boolean ok = MsgApi.sendPtt(ct, peer, src.getAbsolutePath(), dur);
                        MsgStore.add("[解析] 视频语音·不转格式 " + (ok ? "成功" : "失败")
                                + " · 显示 " + (dur / 1000) + " 秒");
                        toast(ok ? "语音发出去了喵～" : "发送失败");
                        return;
                    }

                    out = new File(dir, "va_" + ts + ".silk");
                    String silk = com.cathelperawa.owo.app.bridge.VoiceSilk.audioToSilk(
                            c, src, out.getAbsolutePath(), 0);
                    if (silk == null) {
                        MsgStore.add("[解析] 视频抽音轨转 SILK 失败");
                        toast("转换失败（这条视频可能没有音轨）");
                        return;
                    }
                    int real = com.cathelperawa.owo.app.bridge.VoiceSilk.lastDurationMs;
                    int dur = fake > 0 ? fake : real;
                    boolean ok = MsgApi.sendPtt(ct, peer, silk, dur);
                    MsgStore.add("[解析] 视频语音 " + (ok ? "成功" : "失败")
                            + " · 时长 " + (dur / 1000) + " 秒"
                            + (fake > 0 ? "（自定义，实际 " + (real / 1000) + " 秒）" : ""));
                    toast(ok ? "语音发出去了喵～" : "发语音失败");
                } catch (Throwable t) {
                    HookLog.e("解析: 视频发语音异常", t);
                    toast("发语音异常：" + t);
                } finally {
                    scheduleCleanup(src, out);
                }
            }
        }, "cat-vvoice");
    }

    private void saveVideoAudioToVault(final Context c, final Info info) {
        toast("下载中，存进语音库…");
        Bg.run(new Runnable() {
            @Override public void run() {
                File tmp = null;
                try {
                    String au = biliAudioUrl(info.bvid, info.cid);
                    boolean audioOnly = au != null && au.length() > 0;
                    String url = audioOnly ? au : info.videoUrl;
                    if (url == null || url.length() == 0) {
                        toast("没有可下载的地址");
                        return;
                    }
                    File dir = new File(IoApi.dataDir(), "parse");
                    if (!dir.exists()) dir.mkdirs();
                    String ext = audioOnly ? "m4a" : "mp4";
                    tmp = new File(dir, "vv_" + System.currentTimeMillis() + "." + ext);
                    long got = downloadRetry(info, audioOnly, tmp, 80L * 1024 * 1024);
                    if (got <= 0) { toast("下载失败"); return; }
                    MsgStore.add("[解析] 视频音轨下载 " + (got / 1024) + " KB"
                            + (audioOnly ? "（纯音轨）" : "（整段视频）"));
                    byte[] b = com.cathelperawa.owo.app.protocol.ImageGrabber
                            .readFile(tmp.getAbsolutePath());
                    if (b == null || b.length < 1024) { toast("读不出下载的文件"); return; }
                    String name = info.title != null && info.title.length() > 0
                            ? info.title : "视频音轨";
                    com.cathelperawa.owo.app.proto.VoiceVault.Item it =
                            com.cathelperawa.owo.app.proto.VoiceVault.addBytesWithExt(
                                    c, name, b, "视频", 0, ext);
                    if (it == null) { toast("存不进去…（文件太大？）"); return; }
                    if (autoConvertOn()) {
                        com.cathelperawa.owo.app.proto.VoiceVault.ensureSilk(it);
                    }
                    toast("已存进语音库 · " + it.name
                            + (it.hasSilk ? "（腾讯语音）" : "（普通音频，库里再转）"));
                } catch (Throwable t) {
                    HookLog.e("解析: 视频音轨存语音库异常", t);
                    toast("存语音库异常：" + t);
                } finally {
                    if (tmp != null) { try { tmp.delete(); } catch (Throwable ignore) { } }
                }
            }
        }, "cat-vvault");
    }

    private void sendVideoOf(final Context c, final Object msgItem, final Info info) {
        String mode = sendMode();
        if ("link".equals(mode)) {
            toast("直链已复制模式还没接");
            return;
        }
        if ("card".equals(mode)) {
            toast("卡片模式已移除，改用文件发送");
            mode = "file";
            return;
        }
        toast("开始下载视频…");
        Bg.run(new Runnable() {
            @Override public void run() {
                File f = null;
                try {
                    int chatType = targetChatType(msgItem);
                    String peer = targetPeer(msgItem);
                    if (peer == null || peer.length() == 0) {
                        toast("拿不到当前会话");
                        return;
                    }
                    File dir = new File(IoApi.dataDir(), "parse");
                    if (!dir.exists()) dir.mkdirs();
                    f = new File(dir, "v_" + System.currentTimeMillis() + ".mp4");
                    long max = (long) maxMB() * 1024 * 1024;
                    long got = downloadRetry(info, false, f, max);
                    if (got <= 0) {
                        MsgStore.add("[解析] 下载失败（3 轮都失败）: " + sLastDlErr);
                        toast("下载失败：" + sLastDlErr + "（没发卡片，你要的是视频）");
                        return;
                    }
                    MsgStore.add("[解析] 下载完成 " + (got / 1048576) + " MB");
                    boolean ok = MsgApi.sendVideo(chatType, peer, f.getAbsolutePath());
                    MsgStore.add("[解析] sendVideo " + (ok ? "成功" : "失败"));
                    toast(ok ? "视频发出去了喵～" : "发视频失败");
                } catch (Throwable t) {
                    HookLog.e("解析: 发视频异常", t);
                    toast("发视频异常：" + t);
                } finally {
                    if (f != null) {
                        try { f.delete(); } catch (Throwable ignore) { }
                    }
                }
            }
        }, "cat-vsend");
    }

    public static void startMusicFromPanel(final Context c, final String keyword) {
        try {
            FeatureParse f = new FeatureParse();
            f.ctx = c;
            f.cl = c.getClassLoader();
            f.searchAndPick(c, null, keyword);
        } catch (Throwable t) {
            HookLog.e("解析: startMusicFromPanel 异常", t);
            toast("音乐启动失败：" + t);
        }
    }

    private String musicSendMode() {
        java.util.List<String> all = musicSendModes();
        return all.isEmpty() ? "voice" : all.get(0);
    }

    private java.util.List<String> musicSendModes() {
        java.util.List<String> out = new java.util.ArrayList<String>();
        try {
            String s = HookConfig.get(ctx, K_M_SEND, "card");
            if (s != null) {
                for (String p : s.split(",")) {
                    String v = p == null ? "" : p.trim();
                    if (v.length() == 0) continue;
                    if (!"card".equals(v) && !"voice".equals(v) && !"file".equals(v)
                    && !"image".equals(v)) continue;
                    if (!out.contains(v)) out.add(v);
                }
            }
        } catch (Throwable ignore) {
        }
        if (out.isEmpty()) out.add("card");
        return out;
    }

    private int musicBr() {
        try {
            String s = HookConfig.get(ctx, K_M_BR, "320");
            int v = Integer.parseInt(s.trim());
            if (v <= 0) v = 320;
            return v * 1000;
        } catch (Throwable t) {
            return 320000;
        }
    }

    private int musicCount() {
        try {
            String s = HookConfig.get(ctx, K_M_COUNT, "15");
            int v = Integer.parseInt(s.trim());

            String migrated = HookConfig.get(ctx, "pk_music_count_mig", "");
            if (!"1".equals(migrated) && v == 5) {
                v = 15;
                HookConfig.set(ctx, K_M_COUNT, "15");
                HookConfig.set(ctx, "pk_music_count_mig", "1");
            }
            return v <= 0 ? 15 : Math.min(v, 50);
        } catch (Throwable t) {
            return 15;
        }
    }

    private List<String[]> musicApis() {
        List<String[]> out = new ArrayList<String[]>();
        try {
            String raw = HookConfig.get(ctx, K_M_APIS, "");
            if (raw == null || raw.trim().length() == 0) return out;
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o == null) continue;
                String n = o.optString("n", "接口" + (i + 1));
                String u = o.optString("u", "");
                if (u.trim().length() == 0) continue;
                out.add(new String[]{n, u.trim()});
            }
        } catch (Throwable t) {
            HookLog.w("解析: 音乐接口列表解析失败 " + t);
        }
        return out;
    }

    private void searchAndPick(final Context c, final Object msgItem, final String keyword) {
        toast("搜歌中…");
        Bg.run(new Runnable() {
            @Override public void run() {
                final List<MusicApi.Song> list;
                try {
                    list = musicSearchAll(keyword);
                } catch (Throwable t) {
                    ui(new Runnable() { @Override public void run() {
                        CatDialog.info(c, "搜索失败", String.valueOf(t));
                    }});
                    return;
                }
                ui(new Runnable() {
                    @Override public void run() {
                        if (list == null || list.isEmpty()) {
                            CatDialog.info(c, "没搜到",
                                    "「" + keyword + "」没有结果。\n换个写法试试，比如加上歌手名。");
                            return;
                        }
                        String[] items = new String[list.size()];
                        for (int i = 0; i < list.size(); i++) {
                            MusicApi.Song s = list.get(i);
                            String dur = s.durationText();
                            items[i] = s.toString() + (dur.length() > 0 ? "   " + dur : "");
                        }
                        CatSheet.pick(c, "选一首", items, new CatSheet.OnPick() {
                            @Override public void run(int idx, String label) {
                                MusicApi.Song s = list.get(idx);
                                showMusicCard(c, msgItem, s);
                            }
                        });
                    }
                });
            }
        }, "cat-msearch");
    }

    private List<MusicApi.Song> musicSearchAll(String keyword) {

        List<MusicApi.Song> list = MusicApi.search(keyword, musicCount());
        if (list != null && !list.isEmpty()) return list;
        MsgStore.add("[解析] 官方没搜到，转自定义接口");
        for (String[] api : musicApis()) {
            try {
                List<MusicApi.Song> r = MusicApi.searchCustom(api[0], api[1], keyword, 30);
                if (r != null && !r.isEmpty()) {
                    MsgStore.add("[解析] 「" + api[0] + "」命中 " + r.size() + " 条");
                    return r;
                }
            } catch (Throwable t) {
                HookLog.w("解析: 自定义音乐接口 " + api[0] + " 失败 " + t);
            }
        }
        return list == null ? new ArrayList<MusicApi.Song>() : list;
    }

    private String resolveMp3(MusicApi.Song song) {
        String url = MusicApi.resolveUrl(song.id, song.name, song.artist, musicBr());
        if (url != null && url.length() > 0) return url;

        if (song.directUrl != null && song.directUrl.startsWith("http")) return song.directUrl;
        for (String[] api : musicApis()) {
            try {
                String u = MusicApi.urlCustom(api[1], song);
                if (u != null && u.length() > 0) {
                    MsgStore.add("[解析] 直链来自「" + api[0] + "」");
                    return u;
                }
            } catch (Throwable t) {
                HookLog.w("解析: 自定义直链接口 " + api[0] + " 失败 " + t);
            }
        }
        return null;
    }

    private void showMusicCard(final Context c, final Object msgItem, final MusicApi.Song song) {
        final android.app.Activity act = activityOf(c);
        if (act == null) { toast("拿不到界面上下文"); return; }
        final float d = act.getResources().getDisplayMetrics().density;

        LinearLayout card = new LinearLayout(act);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding((int) (18 * d), (int) (16 * d), (int) (18 * d), (int) (14 * d));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xE6FFFFFF);
        bg.setCornerRadius(26 * d);
        bg.setStroke((int) d, 0xA6FFFFFF);
        card.setBackgroundDrawable(bg);
        card.setElevation(12 * d);

        LinearLayout head = new LinearLayout(act);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        TextView ttl = new TextView(act);
        ttl.setText("音乐");
        ttl.setTextSize(17);
        ttl.setTextColor(0xFF3C4E6B);
        ttl.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        head.addView(ttl, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView tag = new TextView(act);
        tag.setText("网易云");
        tag.setTextSize(10);
        tag.setTextColor(0xFFE85D9C);
        tag.setPadding((int) (8 * d), (int) (3 * d), (int) (8 * d), (int) (3 * d));
        GradientDrawable tg = new GradientDrawable();
        tg.setCornerRadius(9 * d);
        tg.setColor(0x59FFC2DF);
        tag.setBackgroundDrawable(tg);
        head.addView(tag);
        LinearLayout.LayoutParams headLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        headLp.bottomMargin = (int) (12 * d);
        card.addView(head, headLp);

        LinearLayout body = new LinearLayout(act);
        body.setOrientation(LinearLayout.HORIZONTAL);
        body.setGravity(Gravity.CENTER_VERTICAL);

        final ImageView iv = new ImageView(act);
        iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
        iv.setClipToOutline(true);
        GradientDrawable cbg = new GradientDrawable();
        cbg.setCornerRadius(14 * d);
        cbg.setColor(0x14000000);
        iv.setBackgroundDrawable(cbg);
        LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(
                (int) (86 * d), (int) (86 * d));
        ilp.rightMargin = (int) (12 * d);
        body.addView(iv, ilp);
        if (song.pic.length() > 0) loadCover(iv, song.pic);

        LinearLayout info = new LinearLayout(act);
        info.setOrientation(LinearLayout.VERTICAL);
        TextView nm = tv(act, song.name, 15, 0xFF2E3A4E, true, 5);
        nm.setMaxLines(2);
        info.addView(nm);
        if (song.artist.length() > 0) {
            info.addView(tv(act, song.artist, 12, 0xFF6E93D6, false, 4));
        }
        String sub = song.durationText();
        if (song.album.length() > 0) sub = (sub.length() > 0 ? sub + " · " : "") + song.album;
        if (sub.length() > 0) {
            TextView st = tv(act, sub, 11, 0xFF8A9CB5, false, 0);
            st.setMaxLines(1);
            info.addView(st);
        }
        body.addView(info, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        LinearLayout.LayoutParams bodyLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        bodyLp.bottomMargin = (int) (14 * d);
        card.addView(body, bodyLp);

        TextView st2 = tv(act, "音质 " + (musicBr() / 1000) + "k", 11, 0xFF8A9CB5, false, 0);
        LinearLayout.LayoutParams stLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        stLp.bottomMargin = (int) (8 * d);
        card.addView(st2, stLp);

        final android.app.Dialog dlg = new android.app.Dialog(act,
                android.R.style.Theme_Translucent_NoTitleBar);

        final LinearLayout modeRow = new LinearLayout(act);
        modeRow.setOrientation(LinearLayout.HORIZONTAL);
        final String[] labels = {"卡片", "语音", "文件", "图片"};
        final String[] keys = {"card", "voice", "file", "image"};
        final TextView[] segs = new TextView[keys.length];

        final java.util.LinkedHashSet<String> picked =
                new java.util.LinkedHashSet<String>(musicSendModes());
        for (int i = 0; i < keys.length; i++) {
            final int idx = i;
            final boolean on0 = picked.contains(keys[i]);
            TextView seg = pillBtn(act, d, labels[i], on0);
            seg.setBackgroundDrawable(segBg(act, d, on0));
            seg.setTextColor(on0 ? 0xFFE85D9C : 0xFF8A9CB5);
            seg.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    if (picked.contains(keys[idx])) {
                        if (picked.size() <= 1) {
                            toast("至少留一种发送方式");
                            return;
                        }
                        picked.remove(keys[idx]);
                    } else {
                        picked.add(keys[idx]);
                    }
                    for (int j = 0; j < segs.length; j++) {
                        boolean on = picked.contains(keys[j]);
                        segs[j].setBackgroundDrawable(segBg(act, d, on));
                        segs[j].setTextColor(on ? 0xFFE85D9C : 0xFF8A9CB5);
                    }
                }
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            if (i > 0) lp.leftMargin = (int) (4 * d);
            if (i < keys.length - 1) lp.rightMargin = (int) (4 * d);
            segs[i] = seg;
            modeRow.addView(seg, lp);
        }
        LinearLayout.LayoutParams mrlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        mrlp.bottomMargin = (int) (8 * d);
        card.addView(modeRow, mrlp);

        LinearLayout opts = new LinearLayout(act);
        opts.setOrientation(LinearLayout.HORIZONTAL);

        LinearLayout convBox = new LinearLayout(act);
        convBox.setOrientation(LinearLayout.HORIZONTAL);
        convBox.setGravity(Gravity.CENTER_VERTICAL);
        convBox.setPadding((int) (10 * d), (int) (8 * d), (int) (10 * d), (int) (8 * d));
        GradientDrawable convBg = new GradientDrawable();
        convBg.setCornerRadius(14 * d);
        convBg.setColor(0x59FFFFFF);
        convBg.setStroke((int) d, 0x66FFFFFF);
        convBox.setBackgroundDrawable(convBg);
        convBox.addView(tv(act, "转格式", 11, 0xFF6E8AAE, true, 4),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        final com.cathelperawa.owo.app.ui.SwitchBox convSw =
                new com.cathelperawa.owo.app.ui.SwitchBox(act, autoConvertOn());
        convSw.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                boolean z = !autoConvertOn();
                HookConfig.set(act, K_M_CONV, z ? "1" : "0");
                convSw.setChecked(z);
            }
        });
        convBox.addView(convSw, new LinearLayout.LayoutParams(dp(46), dp(26)));
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        clp.rightMargin = (int) (6 * d);
        opts.addView(convBox, clp);

        final TextView durTv = tv(act, durLabel(), 11, 0xFF6E8AAE, true, 0);
        durTv.setGravity(Gravity.CENTER);
        durTv.setPadding((int) (10 * d), (int) (12 * d), (int) (10 * d), (int) (12 * d));
        GradientDrawable dbg = new GradientDrawable();
        dbg.setCornerRadius(14 * d);
        dbg.setColor(0x59FFFFFF);
        dbg.setStroke((int) d, 0x66FFFFFF);
        durTv.setBackgroundDrawable(dbg);
        durTv.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                CatSheet.input(act, "显示时长（秒）", "0 = 用真实时长，只改气泡显示",
                        String.valueOf(fakeDurationSec()), new CatSheet.OnText() {
                            @Override public void run(String s) {
                                String v2 = s == null ? "" : s.trim();
                                int n = 0;
                                try { n = Integer.parseInt(v2); } catch (Throwable ignore) { }
                                HookConfig.set(act, K_M_FAKE_SEC, String.valueOf(Math.max(n, 0)));
                                durTv.setText(durLabel());
                            }
                        });
            }
        });
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        dlp.leftMargin = (int) (6 * d);
        opts.addView(durTv, dlp);

        LinearLayout.LayoutParams olp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        olp.bottomMargin = (int) (8 * d);
        card.addView(opts, olp);

        TextView saveM = pillBtn(act, d, "存入语音库（不发送）", false);
        saveM.setTextSize(12);
        saveM.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                dlg.dismiss();
                saveMusicToVault(act, song);
            }
        });
        LinearLayout.LayoutParams smLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        smLp.bottomMargin = (int) (10 * d);
        card.addView(saveM, smLp);

        LinearLayout btns = new LinearLayout(act);
        btns.setOrientation(LinearLayout.HORIZONTAL);

        TextView cancel = pillBtn(act, d, "取消", false);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { dlg.dismiss(); }
        });

        TextView send = pillBtn(act, d, "发送", true);
        send.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                StringBuilder sb = new StringBuilder();
                for (String k : picked) {
                    if (sb.length() > 0) sb.append(',');
                    sb.append(k);
                }
                HookConfig.set(act, K_M_SEND, sb.toString());
                dlg.dismiss();
                dispatchMusic(act, msgItem, song);
            }
        });

        LinearLayout.LayoutParams bl = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        bl.rightMargin = (int) (5 * d);
        LinearLayout.LayoutParams br2 = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        br2.leftMargin = (int) (5 * d);
        btns.addView(cancel, bl);
        btns.addView(send, br2);
        card.addView(btns);

        dlg.setContentView(card);
        dlg.setCanceledOnTouchOutside(true);
        dlg.show();
        android.view.Window w = dlg.getWindow();
        if (w == null) return;
        w.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(
                android.graphics.Color.TRANSPARENT));
        w.addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        android.view.WindowManager.LayoutParams lp = w.getAttributes();
        int sw = act.getResources().getDisplayMetrics().widthPixels;
        lp.width = (int) (sw * 0.92f);
        lp.height = android.view.WindowManager.LayoutParams.WRAP_CONTENT;
        lp.gravity = Gravity.CENTER;
        lp.dimAmount = 0.24f;
        w.setAttributes(lp);
    }

    private String modeLabel() {
        String m = musicSendMode();
        if ("voice".equals(m)) return "语音";
        if ("file".equals(m)) return "文件";
        if ("image".equals(m)) return "图片";
        return "卡片";
    }

    private void dispatchMusic(final Context c, final Object msgItem, final MusicApi.Song song) {
        final java.util.List<String> modes = musicSendModes();
        if (modes.size() <= 1) {
            String mode = modes.isEmpty() ? "voice" : modes.get(0);
            if ("card".equals(mode)) sendMusicCard(msgItem, song);
            else if ("file".equals(mode)) sendMusicFile(c, msgItem, song);
            else if ("image".equals(mode)) sendMusicImage(c, msgItem, song);
            else sendMusicVoice(c, msgItem, song);
            return;
        }

        Bg.run(new Runnable() {
            @Override public void run() {
                for (int i = 0; i < modes.size(); i++) {
                    final String m = modes.get(i);
                    ui(new Runnable() {
                        @Override public void run() {
                            try {
                                if ("card".equals(m)) sendMusicCard(msgItem, song);
                                else if ("file".equals(m)) sendMusicFile(c, msgItem, song);
                                else if ("image".equals(m)) sendMusicImage(c, msgItem, song);
                                else sendMusicVoice(c, msgItem, song);
                            } catch (Throwable t) {
                                HookLog.w("解析: 发送 " + m + " 失败 " + t);
                            }
                        }
                    });
                    if (i < modes.size() - 1) {
                        try { Thread.sleep(3000); } catch (Throwable ignore) { }
                    }
                }
            }
        }, "cat-music-dispatch");
    }

    private static GradientDrawable segBg(Context c, float d, boolean on) {
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(16 * d);
        if (on) {
            g.setColor(0xFFFFC2DF);
            g.setStroke((int) d, 0xFFFF9FCB);
        } else {
            g.setColor(0x59FFFFFF);
            g.setStroke((int) d, 0x66FFFFFF);
        }
        return g;
    }

    private void sendMusicImage(final Context c, final Object msgItem, final MusicApi.Song song) {
        toast("生成卡片…");
        Bg.run(new Runnable() {
            @Override public void run() {
                try {
                    if (song.pic.length() == 0 || song.album.length() == 0) MusicApi.detail(song);
                    byte[] cover = MusicApi.cover(song.pic);
                    Bitmap bmp = MusicCardImage.render(song.name, song.artist, song.album,
                            song.durationText(), cover);
                    if (bmp == null) { toast("画卡片失败"); return; }
                    File dir = new File(IoApi.dataDir(), "parse");
                    if (!dir.exists()) dir.mkdirs();
                    File f = new File(dir, "card_" + System.currentTimeMillis() + ".png");
                    java.io.FileOutputStream fo = new java.io.FileOutputStream(f);
                    bmp.compress(Bitmap.CompressFormat.PNG, 92, fo);
                    fo.flush();
                    fo.close();

                    int ct = targetChatType(msgItem);
                    String peer = targetPeer(msgItem);
                    if (peer == null || peer.length() == 0) { toast("拿不到会话"); return; }
                    boolean ok = MsgApi.sendPic(ct, peer, f.getAbsolutePath());
                    MsgStore.add("[解析] 图片卡片 " + (ok ? "成功" : "失败"));
                    toast(ok ? "卡片发出去了喵～" : "发图片失败");
                    try { f.delete(); } catch (Throwable ignore) { }
                } catch (Throwable t) {
                    HookLog.e("解析: 发图片卡片异常", t);
                    toast("发图片异常：" + t);
                }
            }
        }, "cat-mimg");
    }

    private void sendMusicCard(final Object msgItem, final MusicApi.Song song) {
        final int ct = targetChatType(msgItem);
        final String peer = targetPeer(msgItem);
        if (peer == null || peer.length() == 0) { toast("拿不到会话"); return; }
        toast("签名中…");
        Bg.run(new Runnable() {
            @Override public void run() {
                try {
                    if (song.pic.length() == 0) MusicApi.detail(song);
                    boolean ok = com.cathelperawa.owo.app.api.ParseApi.sendMusicCard(
                            ct, peer, song.id, song.name, song.artist, song.pic);
                    MsgStore.add("[解析] 音乐卡 " + (ok ? "已发出" : "没发出去"));
                    if (ok) {
                        toast("音乐卡发出去啦，点一下能播～");
                        return;
                    }

                    toast("这首没音源，改发图片卡");
                    ui(new Runnable() {
                        @Override public void run() {
                            sendMusicImage(ctx, msgItem, song);
                        }
                    });
                } catch (Throwable t) {
                    HookLog.e("解析: 发音乐卡片异常", t);
                    toast("发卡片异常：" + t);
                }
            }
        }, "cat-mcard");
    }

    private boolean cardFallbackOn() {
        return HookConfig.getBoolean(ctx, K_CARD_FALLBACK, false);
    }

    private void sendMusicFile(final Context c, final Object msgItem, final MusicApi.Song song) {
        toast("下载中…");
        Bg.run(new Runnable() {
            @Override public void run() {
                File f = null;
                try {
                    String url = resolveMp3(song);
                    if (url == null) {
                        toast("拿不到直链：" + MusicApi.lastError);
                        return;
                    }
                    File dir = new File(IoApi.dataDir(), "parse");
                    if (!dir.exists()) dir.mkdirs();
                    f = new File(dir, safeName(song) + ".mp3");
                    long got = MusicApi.download(url, f, 60L * 1024 * 1024);
                    if (got <= 0) { toast("下载失败"); return; }
                    int ct = targetChatType(msgItem);
                    String peer = targetPeer(msgItem);
                    if (peer == null || peer.length() == 0) { toast("拿不到会话"); return; }
                    boolean ok = MsgApi.sendFile(ct, peer, f.getAbsolutePath());
                    toast(ok ? "文件发出去了喵～" : "发文件失败");
                } catch (Throwable t) {
                    HookLog.e("解析: 发音乐文件异常", t);
                    toast("发文件异常：" + t);
                } finally {
                    if (f != null) { try { f.delete(); } catch (Throwable ignore) { } }
                }
            }
        }, "cat-mfile");
    }

    private void saveMusicToVault(final Context c, final MusicApi.Song song) {
        toast("下载中，存进语音库…");
        Bg.run(new Runnable() {
            @Override public void run() {
                File tmp = null;
                try {
                    String url = resolveMp3(song);
                    if (url == null) {
                        toast("拿不到直链：" + MusicApi.lastError);
                        return;
                    }
                    File dir = new File(IoApi.dataDir(), "parse");
                    if (!dir.exists()) dir.mkdirs();
                    tmp = new File(dir, "mv_" + System.currentTimeMillis() + ".mp3");
                    long got = MusicApi.download(url, tmp, 60L * 1024 * 1024);
                    if (got <= 0) { toast("下载失败"); return; }
                    byte[] b = com.cathelperawa.owo.app.protocol.ImageGrabber
                            .readFile(tmp.getAbsolutePath());
                    if (b == null || b.length < 1024) { toast("读不出下载的文件"); return; }

                    String name = (song.artist != null && song.artist.length() > 0
                            ? song.artist + " - " : "") + song.name;
                    com.cathelperawa.owo.app.proto.VoiceVault.Item it =
                            com.cathelperawa.owo.app.proto.VoiceVault.addBytes(
                                    c, name, b, "音乐", 0);
                    if (it == null) { toast("存不进去…（文件太大？）"); return; }
                    if (autoConvertOn()) {
                        com.cathelperawa.owo.app.proto.VoiceVault.ensureSilk(it);
                    }
                    MsgStore.add("[解析] 音乐存语音库 " + it.name);
                    toast("已存进语音库 · " + it.name
                            + (it.hasSilk ? "（腾讯语音）" : "（普通音频，库里有空再转）"));
                } catch (Throwable t) {
                    HookLog.e("解析: 音乐存语音库异常", t);
                    toast("存语音库异常：" + t);
                } finally {
                    if (tmp != null) { try { tmp.delete(); } catch (Throwable ignore) { } }
                }
            }
        }, "cat-mvault");
    }

    private void sendMusicVoice(final Context c, final Object msgItem, final MusicApi.Song song) {
        final boolean convert = autoConvertOn();
        toast(convert ? "下载并转语音中…" : "下载中（不转格式）…");
        Bg.run(new Runnable() {
            @Override public void run() {
                File mp3 = null;
                File silk = null;
                try {
                    String url = resolveMp3(song);
                    if (url == null) {
                        toast("拿不到直链：" + MusicApi.lastError);
                        return;
                    }
                    File dir = new File(IoApi.dataDir(), "parse");
                    if (!dir.exists()) dir.mkdirs();
                    long ts = System.currentTimeMillis();
                    mp3 = new File(dir, "m_" + ts + ".mp3");
                    long got = MusicApi.download(url, mp3, 60L * 1024 * 1024);
                    if (got <= 0) { toast("下载失败"); return; }
                    MsgStore.add("[解析] 音乐下载完成 " + (got / 1024) + " KB");

                    int ct = targetChatType(msgItem);
                    String peer = targetPeer(msgItem);
                    if (peer == null || peer.length() == 0) { toast("拿不到会话"); return; }

                    if (!convert) {
                        File keep = new File(dir, safeName(song) + ".mp3");
                        if (mp3.renameTo(keep)) mp3 = keep;

                        int br = musicBr() / 1000;
                        if (br <= 0) br = 320;
                        int realMs = (int) Math.min(Integer.MAX_VALUE,
                                mp3.length() * 8L / br);
                        int fakeMs = fakeDurationSec() * 1000;
                        int d = fakeMs > 0 ? fakeMs : Math.max(realMs, 1000);
                        boolean ok = MsgApi.sendPtt(ct, peer, mp3.getAbsolutePath(), d);
                        MsgStore.add("[解析] 不转格式 → 直接发 mp3 当语音 "
                                + (ok ? "成功" : "失败")
                                + " · 时长 " + (d / 1000) + " 秒（估 " + (realMs / 1000) + " 秒）");
                        toast(ok ? "原文件当语音发出去喵～" : "发送失败");
                        return;
                    }

                    silk = new File(dir, "m_" + ts + ".silk");
                    int sec = musicVoiceSec();
                    String out = com.cathelperawa.owo.app.bridge.VoiceSilk.audioToSilk(
                            c, mp3, silk.getAbsolutePath(), sec);
                    if (out == null) { toast("转语音失败"); return; }
                    long silkSize = silk.length();
                    MsgStore.add("[解析] SILK 生成 " + (silkSize / 1024) + " KB · 目标 "
                            + sec + " 秒");
                    if (silkSize < 2000) {
                        toast("语音文件太小（" + silkSize + " B），可能转码失败");
                    }

                    int real = com.cathelperawa.owo.app.bridge.VoiceSilk.lastDurationMs;
                    int fake = fakeDurationSec() * 1000;
                    int dur = fake > 0 ? fake : real;
                    boolean ok = MsgApi.sendPtt(ct, peer, out, dur);
                    MsgStore.add("[解析] sendPtt " + (ok ? "成功" : "失败")
                            + " · 时长 " + (dur / 1000) + " 秒"
                            + (fake > 0 ? "（自定义，实际 " + (real / 1000) + " 秒）" : ""));
                    toast(ok ? "语音发出去了喵～" : "发语音失败");
                } catch (Throwable t) {
                    HookLog.e("解析: 发音乐语音异常", t);
                    toast("发语音异常：" + t);
                } finally {

                    scheduleCleanup(mp3, silk);
                }
            }
        }, "cat-mvoice");
    }

    private static void scheduleCleanup(final File a, final File b) {
        try {
            UI.postDelayed(new Runnable() {
                @Override public void run() {
                    try { if (a != null) a.delete(); } catch (Throwable ignore) { }
                    try { if (b != null) b.delete(); } catch (Throwable ignore) { }
                }
            }, 3 * 60 * 1000L);
        } catch (Throwable ignore) {
        }
    }

    private String durLabel() {
        int v = fakeDurationSec();
        return v > 0 ? ("显示 " + v + " 秒") : "显示真实时长";
    }

    private boolean autoConvertOn() {
        return HookConfig.getBoolean(ctx, K_M_CONV, true);
    }

    private int fakeDurationSec() {
        try {
            String s = HookConfig.get(ctx, K_M_FAKE_SEC, "0");
            int v = Integer.parseInt(s.trim());
            return v < 0 ? 0 : v;
        } catch (Throwable t) {
            return 0;
        }
    }

    private int musicVoiceSec() {
        try {
            String s = HookConfig.get(ctx, K_M_SEC, "0");
            int v = Integer.parseInt(s.trim());
            if (v < 0) v = 0;
            return v;
        } catch (Throwable t) {
            return 0;
        }
    }

    private static String safeName(MusicApi.Song s) {
        String n = s.name + (s.artist.length() > 0 ? " - " + s.artist : "");
        return n.replaceAll("[\\\\/:*?\"<>|\\s]+", "_");
    }

    private static String httpGet(String url, int timeoutMs) throws Exception {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setConnectTimeout(timeoutMs);
            conn.setReadTimeout(timeoutMs);
            hdr(conn, url);
            if (sBiliCookie != null && sBiliCookie.length() > 0) {
                conn.setRequestProperty("Cookie", sBiliCookie);
            }
            conn.setInstanceFollowRedirects(true);
            int code = conn.getResponseCode();
            InputStream in = code >= 400 ? conn.getErrorStream() : conn.getInputStream();
            if (in == null) return "";
            return new String(readAll(in, 4 * 1024 * 1024), "UTF-8");
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private static String follow(String url) {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);
            hdr(conn, url);
            conn.setInstanceFollowRedirects(false);
            conn.getResponseCode();
            String loc = conn.getHeaderField("Location");
            return loc == null ? url : loc;
        } catch (Throwable t) {
            return url;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private static byte[] httpBytes(String url, int timeoutMs, int maxBytes) throws Exception {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setConnectTimeout(timeoutMs);
            conn.setReadTimeout(timeoutMs);
            hdr(conn, url);
            int code = conn.getResponseCode();
            if (code >= 400) return null;
            InputStream in = conn.getInputStream();
            if (in == null) return null;
            return readAll(in, maxBytes);
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private static long probeSize(String url) {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setConnectTimeout(12000);
            conn.setReadTimeout(15000);
            hdr(conn, url);
            conn.setRequestProperty("Accept-Encoding", "identity");
            conn.setRequestProperty("Range", "bytes=0-0");
            if (sBiliCookie != null && sBiliCookie.length() > 0) {
                conn.setRequestProperty("Cookie", sBiliCookie);
            }
            int code = conn.getResponseCode();
            if (code == 206) {
                String cr = conn.getHeaderField("Content-Range");
                if (cr != null) {
                    int p = cr.lastIndexOf('/');
                    if (p > 0) {
                        try { return Long.parseLong(cr.substring(p + 1).trim()); } catch (Throwable ignore) { }
                    }
                }
                return -1;
            }
            if (code >= 400) return -1;
            return conn.getContentLengthLong();
        } catch (Throwable t) {
            return -1;
        } finally {
            if (conn != null) { try { conn.disconnect(); } catch (Throwable ignore) { } }
        }
    }

    private static long downloadMulti(String url, File out, long maxBytes, int threads) {
        long total = probeSize(url);
        if (total <= 0 || total > maxBytes) return 0;
        final int n = Math.max(2, Math.min(threads, 8));
        final long[] got = new long[n];
        final boolean[] bad = new boolean[n];
        long chunk = total / n;
        Thread[] ts = new Thread[n];
        for (int i = 0; i < n; i++) {
            final int idx = i;
            final long a = i * chunk;
            final long b = (i == n - 1) ? (total - 1) : (a + chunk - 1);
            ts[i] = new Thread(new Runnable() {
                @Override public void run() {
                    for (int attempt = 0; attempt < 3; attempt++) {
                        HttpURLConnection cn = null;
                        java.io.RandomAccessFile raf = null;
                        try {
                            cn = (HttpURLConnection) new URL(url).openConnection();
                            cn.setConnectTimeout(12000);
                            cn.setReadTimeout(20000);
                            hdr(cn, url);
                            cn.setRequestProperty("Accept-Encoding", "identity");
                            cn.setRequestProperty("Connection", "close");
                            cn.setRequestProperty("Range", "bytes=" + a + "-" + b);
                            if (sBiliCookie != null && sBiliCookie.length() > 0) {
                                cn.setRequestProperty("Cookie", sBiliCookie);
                            }
                            if (cn.getResponseCode() != 206) throw new java.io.IOException("no 206");
                            InputStream in = cn.getInputStream();
                            raf = new java.io.RandomAccessFile(out, "rw");
                            raf.seek(a);
                            byte[] buf = new byte[64 * 1024];
                            long done = 0;
                            int k;
                            while ((k = in.read(buf)) > 0) {
                                raf.write(buf, 0, k);
                                done += k;
                            }
                            if (done == b - a + 1) {
                                got[idx] = done;
                                bad[idx] = false;
                                return;
                            }
                            throw new java.io.IOException("short " + done);
                        } catch (Throwable t) {
                            bad[idx] = true;
                        } finally {
                            try { if (raf != null) raf.close(); } catch (Throwable ignore) { }
                            if (cn != null) { try { cn.disconnect(); } catch (Throwable ignore) { } }
                        }
                    }
                }
            });
            ts[i].start();
        }
        for (int i = 0; i < n; i++) {
            try { ts[i].join(180000L); } catch (Throwable ignore) { }
        }
        long sum = 0;
        for (int i = 0; i < n; i++) {
            if (bad[i]) return 0;
            sum += got[i];
        }
        if (sum != total) return 0;
        return sum;
    }

    private static long downloadFast(String url, File out, long maxBytes) {
        if (out != null && out.exists()) {
            try { out.delete(); } catch (Throwable ignore) { }
        }
        long r = 0;
        long t0 = System.currentTimeMillis();
        try { r = downloadMulti(url, out, maxBytes, 6); } catch (Throwable t) { r = 0; }
        if (r > 0) {
            long ms = Math.max(1, System.currentTimeMillis() - t0);
            MsgStore.add("[解析] 多线程下载完成 " + (r / 1048576) + " MB · "
                    + (r / 1024 * 1000 / ms) + " KB/s");
            return r;
        }
        if (out != null && out.exists()) {
            try { out.delete(); } catch (Throwable ignore) { }
        }
        return 0;
    }

    private static String biliPlayUrlNow(String bvid, String cid) {
        if (bvid == null || bvid.length() == 0) return "";
        try {
            String pb = httpGet(String.format(BILI_PLAY, bvid, cid == null ? "0" : cid), 15000);
            JSONObject pr = new JSONObject(pb);
            JSONObject pd = pr.optJSONObject("data");
            if (pd == null) return "";
            JSONArray durl = pd.optJSONArray("durl");
            if (durl == null || durl.length() == 0) return "";
            JSONObject u0 = durl.optJSONObject(0);
            return u0 == null ? "" : u0.optString("url", "");
        } catch (Throwable t) {
            return "";
        }
    }

    private static String dyRatioUrl(String apiUrl, int round) {
        if (apiUrl == null || apiUrl.length() == 0) return "";
        Matcher m = Pattern.compile("video_id=([^&]+)").matcher(apiUrl);
        if (!m.find()) return apiUrl;
        String uri = m.group(1);
        String[] rs = {"1080p", "720p", "540p", "480p"};
        String r = rs[Math.min(round, rs.length - 1)];
        return "https://aweme.snssdk.com/aweme/v1/play/?video_id=" + uri
                + "&ratio=" + r + "&line=0";
    }

    private static long downloadRetry(Info info, boolean audio, File out, long maxBytes) {
        long got = 0;
        sLastDlErr = "";
        for (int i = 0; i < 3; i++) {
            String url = null;
            try {
                if (audio) {
                    String au = biliAudioUrl(info.bvid, info.cid);
                    url = au.length() > 0 ? au : biliPlayUrlNow(info.bvid, info.cid);
                } else if ("抖音".equals(info.platform) || isByteUrl(info.videoUrl)) {

                    url = i == 0 ? info.videoUrl
                            : (info.playApi.length() > 0
                                    ? dyRatioUrl(info.playApi, i) : info.videoUrl);
                } else if (i == 0) {
                    url = info.videoUrl;
                } else {
                    url = biliPlayUrlNow(info.bvid, info.cid);
                }
            } catch (Throwable ignore) { }
            if (url == null || url.length() == 0) break;
            if (i == 0) {

                got = downloadFast(url, out, maxBytes);
                if (got > 0) return got;
            }
            got = download(url, out, maxBytes);
            if (got > 0) {
                if (i > 0) MsgStore.add("[解析] 第 " + (i + 1) + " 轮下载成功");
                return got;
            }
            long part = (out != null && out.exists()) ? out.length() : 0;
            MsgStore.add("[解析] 下载中断（第 " + (i + 1) + " 轮）"
                    + (part > 0 ? "，已存 " + (part / 1024) + " KB，换节点续传" : "，换节点重试"));
            try { Thread.sleep(700L * (i + 1)); } catch (Throwable ignore) { }
        }
        return got;
    }

    private static long download(String url, File out, long maxBytes) {
        HttpURLConnection conn = null;
        FileOutputStream fo = null;
        try {

            long have = (out != null && out.exists()) ? out.length() : 0;
            if (have >= maxBytes) return 0;
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(30000);
            hdr(conn, url);
            conn.setRequestProperty("Accept", "*/*");
            conn.setRequestProperty("Accept-Encoding", "identity");
            conn.setRequestProperty("Connection", "close");
            if (sBiliCookie != null && sBiliCookie.length() > 0) {
                conn.setRequestProperty("Cookie", sBiliCookie);
            }
            if (have > 0) {
                conn.setRequestProperty("Range", "bytes=" + have + "-");
            }
            int code = conn.getResponseCode();
            if (code >= 400) {
                sLastDlErr = "HTTP " + code;
                return 0;
            }
            boolean append = have > 0 && code == 206;
            long total = conn.getContentLengthLong();
            if (total > 0) total += append ? have : 0;
            if (total > maxBytes) {
                MsgStore.add("[解析] 文件 " + (total / 1048576) + "MB 超过上限 "
                        + (maxBytes / 1048576) + "MB，放弃下载");
                return 0;
            }
            InputStream in = conn.getInputStream();
            if (in == null) return 0;
            fo = new FileOutputStream(out, append);
            byte[] buf = new byte[64 * 1024];
            long got = append ? have : 0;
            int n;
            while ((n = in.read(buf)) > 0) {
                got += n;
                if (got > maxBytes) {
                    fo.close();
                    fo = null;
                    out.delete();
                    return 0;
                }
                fo.write(buf, 0, n);
            }
            fo.flush();
            fo.close();
            fo = null;
            return got;
        } catch (Throwable t) {
            HookLog.e("解析: 下载异常", t);
            return 0;
        } finally {
            try { if (fo != null) fo.close(); } catch (Throwable ignore) { }
            if (conn != null) conn.disconnect();
        }
    }

    private static byte[] readAll(InputStream in, int max) throws Exception {
        java.io.ByteArrayOutputStream bo = new java.io.ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        int total = 0;
        while ((n = in.read(buf)) > 0) {
            total += n;
            if (total > max) break;
            bo.write(buf, 0, n);
        }
        try { in.close(); } catch (Throwable ignore) { }
        return bo.toByteArray();
    }

    private static String fmtDur(long sec) {
        if (sec <= 0) return "";
        if (sec < 60) return sec + "秒";
        long m = sec / 60;
        long s = sec % 60;
        if (m < 60) return m + "分" + (s > 0 ? s + "秒" : "");
        return (m / 60) + "时" + (m % 60) + "分";
    }

    private static String fmtNum(long v) {
        if (v < 10000) return String.valueOf(v);
        if (v < 100000000) return String.format(java.util.Locale.US, "%.1f万", v / 10000.0);
        return String.format(java.util.Locale.US, "%.1f亿", v / 100000000.0);
    }

    private static void ui(Runnable r) {
        try {
            new android.os.Handler(android.os.Looper.getMainLooper()).post(r);
        } catch (Throwable t) {
            r.run();
        }
    }

    @Override
    protected void check(com.cathelperawa.owo.app.core.SelfCheckResult.Checker c) {

        Class<?> edit = c.cls("AIOEditText", CLS_EDIT_TEXT);
        if (edit == null) {
            c.fail("AIOEditText", "入口类未找到（宿主改版？）");
        } else {
            c.pass("AIOEditText", "长按话筒入口就绪");
        }

        try {
            int n = 0;
            for (java.lang.reflect.Method m : FeatureParse.class.getDeclaredMethods()) {
                if (m.getName().equals("hookMenu")) { n = 1; break; }
            }
            c.pass("菜单钩子", n > 0 ? "已定义" : "未定义");
        } catch (Throwable t) {
            c.pass("菜单钩子", "跳过（" + t.getClass().getSimpleName() + "）");
        }
    }
}
