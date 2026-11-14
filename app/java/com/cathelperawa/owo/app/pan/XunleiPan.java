package com.cathelperawa.owo.app.pan;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class XunleiPan extends PanProvider {

    private static final String AUTH_BASE = "https://xluser-ssl.xunlei.com";
    private static final String PAN_BASE = "https://api-pan.xunlei.com";
    private static final String CAPTCHA_INIT_URL = AUTH_BASE + "/v1/shield/captcha/init";
    private static final String SHARE_URL = PAN_BASE + "/drive/v1/share";
    private static final String SHARE_DETAIL_URL = PAN_BASE + "/drive/v1/share/detail";
    private static final String RESTORE_URL = PAN_BASE + "/drive/v1/share/restore";
    private static final String FILES_URL = PAN_BASE + "/drive/v1/files";
    private static final String SHARE_CREATE_URL = PAN_BASE + "/drive/v1/share";

    private static final String WEB_CLIENT_ID = "Xqp0kJBXWhwaTpB6";
    private static final String WEB_CLIENT_VERSION = "1.93.10";
    private static final String WEB_SIGN_HOST = "pan.xunlei.com";
    private static final String WEB_UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) "
                    + "Chrome/131.0.0.0 Safari/537.36";
    private static final String APP_UA =
            "ANDROID-com.xunlei.downloadprovider/8.31.0.9726 netWorkType/5G appid/40 "
                    + "deviceName/Xiaomi_M2004j7ac deviceModel/M2004J7AC OSVersion/12 protocolVersion/301 "
                    + "platformVersion/10 sdkVersion/512000 Oauth2Client/0.9 (Linux 4_14_186-perf-gddfs8vbb238b) (JAVA 0)";

    private static final String[] WEB_SALTS = {
            "DVM8BRQfJWDsIE0JbH5GCUM0z0jU1yGymBTj4Eyloazv1F+w0G7ig",
            "T",
            "a1fUxfjOO1/dW++DpGupRgw9P93L",
            "aktKbp+/lQc3MvlMHIupt65LjAgJOL6ttMFvTKK+7+WKubSf9rT",
            "SB/ne9FQUnQmWz4Wuv9KqKfGOBOCUubpHpECxOHfD4hNoMclYA2Yq",
            "sOZFBQ8IMFy3OFjAIf/eA65iqFvS9TIKzLyrhiT6",
            "ZhJdLgL7MGh2GanboB0yk",
            "Osf",
            "v"
    };

    private static final String TEMP_DIR = "猫娘网盘临时";

    private final Map<String, String[]> captchaCache = new java.util.concurrent.ConcurrentHashMap<String, String[]>();

    private String deviceId = "";
    private String peerId = "";
    private String deviceSign = "";

    @Override public String id() { return "xunlei"; }

    @Override public String name() { return "迅雷网盘"; }

    @Override public String referer() { return "https://pan.xunlei.com/"; }

    @Override public boolean loggedIn() {
        return accessToken().length() > 0;
    }

    private String accessToken() {
        return cookieValue("accessToken");
    }

    private String cookieValue(String name) {
        if (cookie == null) return "";
        for (String part : cookie.split(";")) {
            String p = part.trim();
            if (p.startsWith(name + "=") && p.length() > name.length() + 1) {
                return p.substring(name.length() + 1);
            }
        }
        return "";
    }

    private void ensureFingerprint() {
        if (deviceId.length() > 0) return;
        android.content.Context c = PanProvider.appContext;
        String id = c == null ? "" : PanPrefs.getString(c, "xl_device_id", "");
        if (id.length() == 0) {
            id = randomHex(32);
            if (c != null) PanPrefs.setString(c, "xl_device_id", id);
        }
        deviceId = id;
        peerId = randomHex(32);

        String base = id + "com.xunlei.downloadprovider" + "40" + "34a062aaa22f906fca4fefe9fb3a3021";
        deviceSign = "div101." + id + md5(sha1(base));
    }

    private static String randomHex(int len) {
        String hex = "0123456789abcdef";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) sb.append(hex.charAt((int) (Math.random() * 16)));
        return sb.toString();
    }

    private static String md5(String s) {
        try {
            MessageDigest d = MessageDigest.getInstance("MD5");
            byte[] b = d.digest(s.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte x : b) sb.append(String.format("%02x", x));
            return sb.toString();
        } catch (Throwable t) {
            return "";
        }
    }

    private static String sha1(String s) {
        try {
            MessageDigest d = MessageDigest.getInstance("SHA-1");
            byte[] b = d.digest(s.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte x : b) sb.append(String.format("%02x", x));
            return sb.toString();
        } catch (Throwable t) {
            return "";
        }
    }

    private String captchaSign(long ts) {
        String h = WEB_CLIENT_ID + WEB_CLIENT_VERSION + WEB_SIGN_HOST + deviceId + ts;
        for (String salt : WEB_SALTS) h = md5(h + salt);
        return "1." + h;
    }

    private String captcha(String action) {
        String[] cached = captchaCache.get(action);
        long now = System.currentTimeMillis();
        if (cached != null && Long.parseLong(cached[1]) > now) return cached[0];
        try {
            long ts = now;
            JSONObject meta = new JSONObject()
                    .put("username", "")
                    .put("client_version", WEB_CLIENT_VERSION)
                    .put("package_name", "pan.xunlei.com")
                    .put("timestamp", String.valueOf(ts))
                    .put("captcha_sign", captchaSign(ts))
                    .put("user_id", "");
            JSONObject body = new JSONObject()
                    .put("action", action)
                    .put("captcha_token", "")
                    .put("client_id", WEB_CLIENT_ID)
                    .put("device_id", deviceId)
                    .put("meta", meta)
                    .put("redirect_uri", "xlaccsdk01://xunlei.com/callback?state=harbor");
            Map<String, String> h = PanHttp.headers(
                    "User-Agent", WEB_UA,
                    "Content-Type", "application/json",
                    "X-Client-Id", WEB_CLIENT_ID,
                    "X-Device-Id", deviceId,
                    "X-Client-Version", WEB_CLIENT_VERSION);
            PanHttp.Resp r = PanHttp.post(CAPTCHA_INIT_URL, body.toString(), h);
            JSONObject j = new JSONObject(r.body);
            String tok = j.optString("captcha_token");
            if (tok.length() == 0) return "";
            captchaCache.put(action, new String[]{tok, String.valueOf(now + 270_000L)});
            return tok;
        } catch (Throwable t) {
            return "";
        }
    }

    private Map<String, String> panHeaders(String ck, String captchaToken) {
        Map<String, String> h = PanHttp.headers(
                "User-Agent", WEB_UA,
                "X-Device-Id", deviceId,
                "X-Client-Version", "8.31.0.9726",
                "Content-Type", "application/json",
                "Origin", "https://pan.xunlei.com",
                "Referer", "https://pan.xunlei.com/");
        String tk = ck == null ? "" : ck;
        if (tk.length() == 0) tk = accessToken();
        if (tk.length() > 0) h.put("Authorization", "Bearer " + tk);
        if (captchaToken != null && captchaToken.length() > 0) h.put("X-Captcha-Token", captchaToken);
        return h;
    }

    @Override public Session open(String shareId, String pwd) throws PanError {
        ensureFingerprint();
        Session s = new Session();
        s.platform = id();
        s.shareId = shareId;
        s.pwd = pwd == null ? "" : pwd;
        try {
            String url = SHARE_URL + "?share_id=" + URLEncoder.encode(shareId, "UTF-8")
                    + "&pass_code=" + URLEncoder.encode(s.pwd, "UTF-8")
                    + "&limit=100&page_token=&thumbnail_size=SIZE_SMALL";
            String ck = accessToken();
            String cap = ck.length() > 0 ? captcha("GET:/drive/v1/share") : "";
            PanHttp.Resp r = PanHttp.get(url, panHeaders(ck, cap));
            dbg("xunlei.open", "GET " + url + "\nHTTP " + r.code + " => "
                    + (r.body == null ? "" : (r.body.length() > 400 ? r.body.substring(0, 400) : r.body)));
            JSONObject j = json(r.body);
            checkErr(j, "打不开分享");
            String status = j.optString("share_status");
            if ("PASS_CODE_EMPTY".equals(status)) throw new PanError("请输入提取码");
            if ("PASS_CODE_ERROR".equals(status)) throw new PanError("提取码错误");
            if ("PASS_CODE_NEED".equals(status)) throw new PanError("该分享需要提取码");
            s.stoken = j.optString("pass_code_token");
            String t = j.optString("title");
            s.title = t.length() > 0 ? t : "迅雷网盘分享";
            return s;
        } catch (PanError e) {
            throw e;
        } catch (Throwable t) {
            throw netError(t);
        }
    }

    @Override public List<PanItem> list(Session s, String pdirFid) throws PanError {
        ensureFingerprint();
        try {
            String ck = accessToken();
            boolean root = pdirFid == null || pdirFid.length() == 0 || "0".equals(pdirFid);
            String cap;
            String url;
            if (root) {
                url = SHARE_URL + "?share_id=" + URLEncoder.encode(s.shareId, "UTF-8")
                        + "&pass_code=" + URLEncoder.encode(s.pwd == null ? "" : s.pwd, "UTF-8")
                        + "&limit=100&page_token=&thumbnail_size=SIZE_SMALL";
                cap = ck.length() > 0 ? captcha("GET:/drive/v1/share") : "";
            } else {
                url = SHARE_DETAIL_URL + "?share_id=" + URLEncoder.encode(s.shareId, "UTF-8")
                        + "&parent_id=" + URLEncoder.encode(pdirFid, "UTF-8")
                        + "&pass_code_token=" + URLEncoder.encode(s.stoken == null ? "" : s.stoken, "UTF-8")
                        + "&limit=100&page_token=&thumbnail_size=SIZE_SMALL";
                cap = ck.length() > 0 ? captcha("GET:/drive/v1/share/detail") : "";
            }
            PanHttp.Resp r = PanHttp.get(url, panHeaders(ck, cap));
            JSONObject j = json(r.body);
            checkErr(j, "读取分享列表失败");
            JSONArray arr = j.optJSONArray("files");
            List<PanItem> out = new ArrayList<PanItem>();
            if (arr == null) return out;
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o == null) continue;
                boolean dir = "drive#folder".equals(o.optString("kind"));
                out.add(new PanItem(o.optString("id"), o.optString("name"),
                        o.optLong("size"), dir, o.optString("id")));
            }
            return out;
        } catch (PanError e) {
            throw e;
        } catch (Throwable t) {
            throw netError(t);
        }
    }

    @Override public Link link(Session s, PanItem item) throws PanError {
        if (!loggedIn()) throw new PanError("迅雷下载要先登录（转存需要你的账号）");
        try {
            ensureFingerprint();
            String parent = ensureTempDir();
            String newId = restore(s, item, parent);
            return detail(newId, item.name, item.size);
        } catch (PanError e) {
            throw e;
        } catch (Throwable t) {
            throw netError(t);
        }
    }

    @Override public List<PanItem> listMine(String pdirFid) throws PanError {
        if (!loggedIn()) throw new PanError("先登录迅雷网盘，才能看自己的网盘目录");
        try {
            ensureFingerprint();
            String parent = pdirFid == null || pdirFid.length() == 0 || "0".equals(pdirFid) ? "" : pdirFid;
            String url = FILES_URL + "?parent_id=" + URLEncoder.encode(parent, "UTF-8")
                    + "&limit=100&page_token=&thumbnail_size=SIZE_SMALL&with_audit=true";
            String cap = captcha("GET:/drive/v1/files");
            PanHttp.Resp r = PanHttp.get(url, panHeaders("", cap));
            JSONObject j = json(r.body);
            checkErr(j, "读取网盘目录失败");
            JSONArray arr = j.optJSONArray("files");
            List<PanItem> out = new ArrayList<PanItem>();
            if (arr == null) return out;
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o == null) continue;
                boolean dir = "drive#folder".equals(o.optString("kind"));
                out.add(new PanItem(o.optString("id"), o.optString("name"),
                        o.optLong("size"), dir, o.optString("id")));
            }
            return out;
        } catch (PanError e) {
            throw e;
        } catch (Throwable t) {
            throw netError(t);
        }
    }

    @Override public Link linkMine(PanItem item) throws PanError {
        if (!loggedIn()) throw new PanError("先登录迅雷网盘");
        try {
            ensureFingerprint();
            return detail(item.fid, item.name, item.size);
        } catch (PanError e) {
            throw e;
        } catch (Throwable t) {
            throw netError(t);
        }
    }

    @Override public ShareInfo createShare(PanItem item, String passcode, int expiredType) throws PanError {
        if (!loggedIn()) throw new PanError("先登录迅雷网盘，才能创建分享");
        try {
            ensureFingerprint();

            String days;
            switch (expiredType) {
                case 2: days = "1"; break;
                case 3: days = "7"; break;
                case 4: days = "30"; break;
                default: days = "-1"; break;
            }
            JSONObject params = new JSONObject()
                    .put("subscribe_push", "false")
                    .put("WithPassCodeInLink", "true")
                    .put("with_pass_code_in_link", "true");
            if (passcode != null && passcode.length() > 0) params.put("pass_code", passcode);
            JSONObject body = new JSONObject()
                    .put("file_ids", new JSONArray().put(item.fid))

                    .put("share_to", "copy")
                    .put("params", params)
                    .put("title", item.name == null || item.name.length() == 0 ? "分享文件" : item.name)
                    .put("restore_limit", "-1")
                    .put("expiration_days", days);
            String cap = captcha("POST:/drive/v1/share");
            PanHttp.Resp r = PanHttp.post(SHARE_CREATE_URL, body.toString(), panHeaders("", cap));
            dbg("xunlei.createShare", "body=" + body + "\nHTTP " + r.code + " => "
                    + (r.body == null ? "" : (r.body.length() > 400 ? r.body.substring(0, 400) : r.body)));
            JSONObject j = json(r.body);
            checkErr(j, "创建分享失败");
            String url = j.optString("share_url");
            String pwd = j.optString("pass_code");
            String shareId = j.optString("share_id");
            if (url.length() == 0 && shareId.length() > 0) {
                url = "https://pan.xunlei.com/s/" + shareId;
            }
            if (url.length() == 0) throw new PanError("没返回分享链接");
            ShareInfo si = new ShareInfo();
            si.url = url;
            si.pwd = pwd.length() > 0 ? pwd : (passcode == null ? "" : passcode);
            si.title = item.name;
            return si;
        } catch (PanError e) {
            throw e;
        } catch (Throwable t) {
            throw netError(t);
        }
    }

    private static void checkErr(JSONObject j, String fallback) throws PanError {
        String err = j.optString("error", "");
        if (err.length() == 0) err = j.optString("error_description", "");
        if (err.length() > 0) {
            if ("unauthenticated".equals(err)) throw new PanError("登录态失效，去网页登录里重登迅雷");
            String desc = j.optString("error_description", "");
            throw new PanError(desc.length() > 0 ? desc : err);
        }
    }

    private String ensureTempDir() throws PanError {
        try {
            String url = FILES_URL + "?parent_id=&limit=100&page_token=&thumbnail_size=SIZE_SMALL";
            String cap = captcha("GET:/drive/v1/files");
            PanHttp.Resp r = PanHttp.get(url, panHeaders("", cap));
            JSONObject j = json(r.body);
            checkErr(j, "读取网盘根目录失败");
            JSONArray arr = j.optJSONArray("files");
            if (arr != null) {
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.optJSONObject(i);
                    if (o != null && TEMP_DIR.equals(o.optString("name"))
                            && "drive#folder".equals(o.optString("kind"))) {
                        return o.optString("id");
                    }
                }
            }
            JSONObject body = new JSONObject()
                    .put("kind", "drive#folder")
                    .put("name", TEMP_DIR)
                    .put("parent_id", "")
                    .put("space", "");
            String cap2 = captcha("POST:/drive/v1/files");
            PanHttp.Resp cr = PanHttp.post(FILES_URL, body.toString(), panHeaders("", cap2));
            JSONObject cj = json(cr.body);
            String id = cj.optString("id");
            if (id.length() == 0) throw new PanError("创建临时目录失败");
            return id;
        } catch (PanError e) {
            throw e;
        } catch (Throwable t) {
            throw netError(t);
        }
    }

    private String restore(Session s, PanItem item, String parentId) throws PanError {
        try {
            JSONObject body = new JSONObject()
                    .put("share_id", s.shareId)
                    .put("pass_code_token", s.stoken == null ? "" : s.stoken)
                    .put("parent_id", parentId)
                    .put("ancestor_ids", new JSONArray())
                    .put("file_ids", new JSONArray().put(item.fid))
                    .put("specify_parent_id", true);
            String cap = captcha("POST:/drive/v1/share/restore");
            PanHttp.Resp r = PanHttp.post(RESTORE_URL, body.toString(), panHeaders("", cap));
            dbg("xunlei.restore", "body=" + body + "\nHTTP " + r.code + " => "
                    + (r.body == null ? "" : (r.body.length() > 400 ? r.body.substring(0, 400) : r.body)));
            JSONObject j = json(r.body);
            checkErr(j, "转存失败");
            JSONObject params = j.optJSONObject("params");
            if (params != null) {
                String trace = params.optString("trace_file_ids", "");
                if (trace.length() > 0) {
                    JSONObject m = new JSONObject(trace);
                    String nid = m.optString(item.fid);
                    if (nid.length() > 0) return nid;
                }
            }
            String nid = j.optString("file_id");
            if (nid.length() == 0) throw new PanError("转存没返回新文件 id");
            return nid;
        } catch (PanError e) {
            throw e;
        } catch (Throwable t) {
            throw netError(t);
        }
    }

    private Link detail(String fileId, String fallbackName, long fallbackSize) throws PanError {
        try {
            String url = FILES_URL + "/" + fileId
                    + "?_magic=2021&usage=PLAY&thumbnail_size=SIZE_LARGE"
                    + "&with=hdr10&with=subtitle_files&with=task&with=public_share_tag";
            String cap = captcha("GET:/drive/v1/files/" + fileId);
            PanHttp.Resp r = PanHttp.get(url, panHeaders("", cap));
            JSONObject j = json(r.body);
            checkErr(j, "获取直链失败");
            JSONObject links = j.optJSONObject("links");
            String dl = "";
            if (links != null) {
                JSONObject app = links.optJSONObject("application/octet-stream");
                if (app != null) dl = app.optString("url");
            }
            if (dl.length() == 0) dl = j.optString("web_content_link");
            if (dl.length() == 0) throw new PanError("没拿到直链（文件可能受限）");
            Link l = new Link();
            l.url = dl;
            l.filename = j.optString("name");
            if (l.filename.length() == 0) l.filename = fallbackName;
            l.size = j.optLong("size", fallbackSize);
            l.ua = WEB_UA;
            l.referer = "https://pan.xunlei.com/";
            l.cookie = "";
            return l;
        } catch (PanError e) {
            throw e;
        } catch (Throwable t) {
            throw netError(t);
        }
    }

    private static String readAll(InputStream in) {
        try {
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) bo.write(buf, 0, n);
            return new String(bo.toByteArray(), "UTF-8");
        } catch (Throwable t) {
            return "";
        }
    }
}
