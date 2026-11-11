package com.cathelperawa.owo.app.pan;

import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;

import com.cathelperawa.owo.app.cookie.CookieStore;
import com.cathelperawa.owo.app.core.Bg;
import com.cathelperawa.owo.app.ui.CatDialog;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class PanProvider {

    public static class Session {
        public String shareId = "";
        public String pwd = "";
        public String stoken = "";
        public String title = "";
        
        public String platform = "";
        
        public String uk = "";
        public String shareIdInt = "";
    }

    public static class Link {
        public String url = "";
        public String filename = "";
        public long size = 0;
        
        public String cookie = "";
        public String ua = "";
        public String referer = "";
        
        public boolean guest = false;
    }

    public static class PanError extends Exception {
        public PanError(String msg) { super(msg); }
    }

    public static class BrowserNeeded extends PanError {
        public final String url;
        public final String referer;
        public BrowserNeeded(String url, String referer, String msg) {
            super(msg);
            this.url = url;
            this.referer = referer;
        }
    }

    
    public abstract String id();

    public abstract String name();

    public abstract String referer();

    public abstract Session open(String shareId, String pwd) throws PanError;

    public abstract List<PanItem> list(Session s, String pdirFid) throws PanError;

    public abstract Link link(Session s, PanItem item) throws PanError;

    
    public abstract boolean loggedIn();

    public boolean canMine() {
        return loggedIn();
    }

    public abstract List<PanItem> listMine(String pdirFid) throws PanError;

    public abstract Link linkMine(PanItem item) throws PanError;

    public static class ShareInfo {
        public String url = "";
        public String pwd = "";
        public String title = "";
    }

    public abstract ShareInfo createShare(PanItem item, String passcode, int expiredType) throws PanError;

    
    public boolean canManage() {
        return false;
    }

    public void renameMine(PanItem item, String newName) throws PanError {
        throw new PanError(name() + " 暂不支持改名");
    }

    public void deleteMine(PanItem item, String parentFid) throws PanError {
        throw new PanError(name() + " 暂不支持删除");
    }

    
    public String cookie = "";

    public interface CookieSink {
        void onCookie(String platform, String newCookie);
    }

    protected CookieSink sink;

    public void setCookieSink(CookieSink s) {
        this.sink = s;
    }

    protected void absorb(List<String> setCookies) {
        if (setCookies == null || setCookies.isEmpty()) return;
        String merged = PanHttp.merge(cookie, setCookies);
        if (!merged.equals(cookie)) {
            cookie = merged;
            if (sink != null) sink.onCookie(id(), merged);
        }
    }

    protected static JSONObject json(String body) throws PanError {
        try {
            return new JSONObject(body == null ? "" : body);
        } catch (Throwable t) {
            throw new PanError("服务端返回的不是 JSON（可能被风控拦了）");
        }
    }

    protected static JSONObject dataOf(JSONObject r, String fallback) throws PanError {
        int status = r.optInt("status", -1);
        int code = r.optInt("code", 0);
        if (status != 200 && code != 0) {
            String msg = r.optString("message", "");
            throw new PanError(msg.length() > 0 ? msg : fallback);
        }
        JSONObject d = r.optJSONObject("data");
        if (d == null) throw new PanError("响应缺少 data");
        return d;
    }

    protected static PanError netError(Throwable t) {
        String m = t == null ? "" : String.valueOf(t.getMessage());
        return new PanError("网络请求失败：" + (m.length() > 90 ? m.substring(0, 90) + "…" : m));
    }

    public interface Cb<T> {
        void ok(T value);
        void fail(String message);
    }

    private static final Handler UI = new Handler(Looper.getMainLooper());

    public interface DebugSink {
        void dump(String tag, String text);
    }

    public static DebugSink debug;

    protected static String cutStr(String s) {
        if (s == null) return "";
        return s.length() > 400 ? s.substring(0, 400) + "…" : s;
    }

    protected static void dbg(String tag, String text) {
        if (debug != null) {
            try {
                debug.dump(tag, text);
            } catch (Throwable ignore) {
            }
        }
    }

    public static volatile int RETRY = 0;

    public static <T> void run(final Task<T> task, final Cb<T> cb) {
        Bg.run(new Runnable() {
            @Override public void run() {
                int attempt = 0;
                while (true) {
                try {
                    final T v = task.call();
                    if (cb == null) return;
                    UI.post(new Runnable() {
                        @Override public void run() { cb.ok(v); }
                    });
                } catch (final Throwable t) {
                    
                    if (attempt < RETRY && retryable(t)) {
                        attempt++;
                        try { Thread.sleep(600L * attempt); } catch (Throwable ignore) { }
                        continue;
                    }
                    if (cb == null) return;
                    final String msg = t instanceof PanError ? t.getMessage()
                            : (t.getMessage() == null ? t.toString() : t.getMessage());
                    UI.post(new Runnable() {
                        @Override public void run() { cb.fail(msg); }
                    });
                    return;
                }
                return;
                }
            }
        }, "pan-" + System.currentTimeMillis());
    }

    private static boolean retryable(Throwable t) {
        if (t instanceof PanError) {
            String m = t.getMessage();
            if (m == null) return false;
            return m.contains("网络请求失败") || m.contains("超时") || m.contains("timed out")
                    || m.contains("Socket") || m.contains("Connection");
        }
        return true;   
    }

    public interface Task<T> {
        T call() throws Exception;
    }

    public static final String[] SUPPORTED = {"quark", "uc", "pan123", "baidu", "xunlei", "c139", "lanzou"};

    public static android.content.Context appContext;

    public static PanProvider create(android.content.Context ctx, String platform) {
        PanProvider p;
        if ("quark".equals(platform)) p = new QuarkPan();
        else if ("uc".equals(platform)) p = new UcPan();
        else if ("pan123".equals(platform)) p = new Pan123Pan();
        else if ("baidu".equals(platform)) p = new BaiduPan();
        else if ("xunlei".equals(platform)) p = new XunleiPan();
        else if ("c139".equals(platform)) p = new C139Pan();
        else if ("lanzou".equals(platform)) p = new LanzouPan();
        else return null;
        appContext = ctx == null ? null : ctx.getApplicationContext();
        CookieStore.Entry e = CookieStore.get(ctx, platform);
        p.cookie = e == null ? "" : e.cookie;
        return p;
    }

    public static boolean supported(String platform) {
        for (String s : SUPPORTED) if (s.equals(platform)) return true;
        return false;
    }

    public static String shortName(String platform) {
        if ("quark".equals(platform)) return "夸克";
        if ("uc".equals(platform)) return "UC";
        if ("pan123".equals(platform)) return "123";
        if ("baidu".equals(platform)) return "百度";
        if ("xunlei".equals(platform)) return "迅雷";
        if ("c139".equals(platform)) return "139";
        if ("lanzou".equals(platform)) return "蓝奏";
        return platform;
    }

    public static String nameOf(String platform) {
        if ("quark".equals(platform)) return "夸克网盘";
        if ("uc".equals(platform)) return "UC 网盘";
        if ("pan123".equals(platform)) return "123 云盘";
        if ("baidu".equals(platform)) return "百度网盘";
        if ("xunlei".equals(platform)) return "迅雷网盘";
        if ("c139".equals(platform)) return "139 网盘";
        if ("lanzou".equals(platform)) return "蓝奏云";
        return platform;
    }
}
