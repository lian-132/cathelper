package com.cathelperawa.owo.app.pan;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.zip.GZIPInputStream;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public final class C139Pan extends PanProvider {

    private static final String SHARE_BASE = "https://share-kd-njs.yun.139.com";
    private static final String SHARE_LIST_URL = SHARE_BASE + "/yun-share/richlifeApp/devapp/IOutLink/getOutLinkInfoV6";
    private static final String SHARE_LINK_URL = SHARE_BASE + "/yun-share/richlifeApp/devapp/IOutLink/dlFromOutLinkV3";
    private static final String SHARE_GENERAL_URL = SHARE_BASE + "/yun-share/richlifeApp/devapp/IOutLink/getOutLinkGeneral";

    private static final String CLOUD_BASE = "https://personal-kd-njs.yun.139.com";
    private static final String FILE_LIST_URL = CLOUD_BASE + "/hcy/file/list";
    private static final String DOWNLOAD_URL = CLOUD_BASE + "/hcy/file/getDownloadUrl";
    private static final String OUTLINK_CREATE_URL =
            "https://yun.139.com/orchestration/personalCloud-rebuild/outlink/v1.0/getOutLink";

    private static final String AES_KEY = "PVGDwmcvfs1uV3d1";
    private static final String SHARE_MOBILE_UA =
            "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) "
                    + "Chrome/150.0.0.0 Mobile Safari/537.36";
    private static final String PC_UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) "
                    + "Chrome/120.0.0.0 Safari/537.36";
    private static final String SHARE_X_DEVICEINFO = "||3|12.27.0|||||chrome 150.0.0.0|360X444|zh-cn|||";

    @Override public String id() { return "c139"; }

    @Override public String name() { return "139 网盘"; }

    @Override public String referer() { return "https://yun.139.com/"; }

    @Override public boolean loggedIn() {
        return authorization().length() > 0;
    }

    private String cookieVar(String name) {
        if (cookie == null) return "";
        for (String part : cookie.split(";")) {
            String p = part.trim();
            if (p.startsWith(name + "=") && p.length() > name.length() + 1) {
                return p.substring(name.length() + 1);
            }
        }
        return "";
    }

    private String authorization() {
        return cookieVar("authorization");
    }

    private String account() {
        String enc = cookieVar("ORCHES-I-ACCOUNT-ENCRYPT");
        if (enc.length() > 0) {
            try {
                String s = new String(Base64.getDecoder().decode(enc), "UTF-8");
                if (s.length() > 0) return s;
            } catch (Throwable ignore) {
            }
        }
        String v = cookieVar("Login_UserNumber");
        if (v.length() > 0) return v;
        String auth = authorization();
        if (auth.length() > 0) {
            try {
                String b64 = auth.startsWith("Basic") ? auth.substring(5).trim() : auth;
                String dec = new String(Base64.getDecoder().decode(b64), "UTF-8");
                String[] parts = dec.split(":");
                if (parts.length > 1 && parts[1].length() > 0) return parts[1];
            } catch (Throwable ignore) {
            }
        }
        return "";
    }

    private static byte[] key() throws Exception {
        return AES_KEY.getBytes("UTF-8");
    }

    private static String encryptBody(String plain) throws Exception {
        byte[] iv = new byte[16];
        new Random().nextBytes(iv);
        Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
        c.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key(), "AES"), new IvParameterSpec(iv));
        byte[] ct = c.doFinal(plain.getBytes("UTF-8"));
        byte[] all = new byte[iv.length + ct.length];
        System.arraycopy(iv, 0, all, 0, 16);
        System.arraycopy(ct, 0, all, 16, ct.length);
        return Base64.getEncoder().encodeToString(all);
    }

    private static String decryptBody(String b64) throws Exception {
        byte[] raw = Base64.getDecoder().decode(b64.trim());
        byte[] iv = Arrays.copyOfRange(raw, 0, 16);
        byte[] ct = Arrays.copyOfRange(raw, 16, raw.length);
        Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
        c.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key(), "AES"), new IvParameterSpec(iv));
        byte[] d = c.doFinal(ct);
        if (d.length > 2 && (d[0] & 0xff) == 0x1f && (d[1] & 0xff) == 0x8b) {
            ByteArrayInputStream bi = new ByteArrayInputStream(d);
            GZIPInputStream gz = new GZIPInputStream(bi);
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = gz.read(buf)) > 0) bo.write(buf, 0, n);
            gz.close();
            d = bo.toByteArray();
        }
        return new String(d, "UTF-8");
    }

    private static String encodeURIComponent(String s) throws Exception {
        String e = URLEncoder.encode(s, "UTF-8");
        return e.replace("+", "%20").replace("*", "%2A").replace("%7E", "~");
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

    private static String signHeader(String body) throws Exception {
        String ts = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
        String pool = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder rb = new StringBuilder();
        Random r = new Random();
        for (int i = 0; i < 16; i++) rb.append(pool.charAt(r.nextInt(pool.length())));
        String rand = rb.toString();

        String encoded = encodeURIComponent(body);
        char[] chars = encoded.toCharArray();
        Arrays.sort(chars);
        String sorted = new String(chars);
        String b64 = Base64.getEncoder().encodeToString(sorted.getBytes("UTF-8"));
        String res = md5(b64) + md5(ts + ":" + rand);
        return ts + "," + rand + "," + md5(res).toUpperCase();
    }

    private static JSONObject parseMaybeEncrypted(String body) throws Exception {
        String t = body == null ? "" : body.trim();
        if (t.length() == 0) throw new PanError("服务端返回空响应");
        if (t.startsWith("{")) return new JSONObject(t);
        try {
            return new JSONObject(decryptBody(t));
        } catch (Throwable ignore) {
            return new JSONObject(t);
        }
    }

    private JSONObject sharePostAnonymous(String url, String plain) throws Exception {
        Map<String, String> h = PanHttp.headers(
                "hcy-cool-flag", "1",
                "x-deviceinfo", SHARE_X_DEVICEINFO,
                "x-huawei-channelsrc", "10245500",
                "x-mm-source", "0002",
                "Content-Type", "application/json;charset=UTF-8",
                "User-Agent", SHARE_MOBILE_UA,
                "Origin", "https://yun.139.com",
                "Referer", "https://yun.139.com/",
                "Accept", "application/json, text/plain, */*");
        PanHttp.Resp r = PanHttp.post(url, encryptBody(plain), h);
        return parseMaybeEncrypted(r.body);
    }

    private JSONObject sharePostEncrypted(String url, String plain, String auth) throws Exception {
        Map<String, String> h = PanHttp.headers(
                "hcy-cool-flag", "1",
                "x-deviceinfo", SHARE_X_DEVICEINFO,
                "x-huawei-channelsrc", "10245500",
                "x-mm-source", "0002",
                "mcloud-sign", signHeader(plain),
                "Content-Type", "application/json;charset=UTF-8",
                "User-Agent", SHARE_MOBILE_UA,
                "Origin", "https://yun.139.com",
                "Referer", "https://yun.139.com/",
                "Accept", "application/json, text/plain, */*");
        if (auth != null && auth.length() > 0) h.put("Authorization", auth);
        PanHttp.Resp r = PanHttp.post(url, encryptBody(plain), h);
        return parseMaybeEncrypted(r.body);
    }

    private JSONObject cloudPost(String url, String plain, String auth, boolean needSkey) throws Exception {
        Map<String, String> h = PanHttp.headers(
                "Authorization", auth,
                "mcloud-sign", signHeader(plain),
                "x-yun-channel-source", "10000034",
                "x-yun-app-channel", "10000034",
                "x-huawei-channelSrc", "10000034",
                "mcloud-version", "7.17.9",
                "mcloud-client", "10701",
                "mcloud-channel", "1000101",
                "mcloud-route", "001",
                "x-yun-module-type", "100",
                "x-yun-api-version", "v1",
                "x-yun-svc-type", "1",
                "x-SvcType", "1",
                "caller", "web",
                "x-inner-ntwk", "2",
                "CMS-DEVICE", "default",
                "x-m4c-src", "10002",
                "x-m4c-caller", "PC",
                "X-Deviceinfo", "||9|7.17.9|chrome|116.0.0.0|2cdaf7ada9e353c70eba99092e177991||windows 10||zh-CN|||",
                "x-yun-client-info", "||9|7.17.9|chrome|116.0.0.0|2cdaf7ada9e353c70eba99092e177991||windows 10||zh-CN|||dW5kZWZpbmVk||",
                "INNER-HCY-ROUTER-HTTPS", "1",
                "Sec-Fetch-Site", "same-site",
                "Sec-Fetch-Mode", "cors",
                "Sec-Fetch-Dest", "empty",
                "X-Requested-With", "mark.via",
                "Content-Type", "application/json;charset=UTF-8",
                "User-Agent", PC_UA,
                "Origin", "https://yun.139.com",
                "Referer", "https://yun.139.com/",
                "Accept", "application/json, text/plain, */*");
        if (cookie != null && cookie.length() > 0) {
            h.put("Cookie", cookie);
            if (needSkey) {
                String skey = cookieVar("skey");
                if (skey.length() > 0) h.put("mcloud-skey", skey);
            }
        }
        PanHttp.Resp r = PanHttp.post(url, plain, h);
        dbg("c139.cloud", url + "\nbody=" + plain + "\nHTTP " + r.code + " => " + cut(r.body));
        return parseMaybeEncrypted(r.body);
    }

    private static void checkShare(JSONObject j, String fallback) throws PanError {
        String rc = j.optString("resultCode", "");
        if (rc.length() > 0 && !"0".equals(rc)) {
            String d = j.optString("desc", "");
            throw new PanError(d.length() > 0 ? d : (fallback + "（" + rc + "）"));
        }
        if (!j.optBoolean("success", true)) {
            String d = j.optString("desc", "");
            throw new PanError(d.length() > 0 ? d : fallback);
        }
    }

    private static void checkCloud(JSONObject j, String fallback) throws PanError {
        String code = j.optString("code", "");
        if (j.optBoolean("success", true) && ("0000".equals(code) || "0".equals(code) || code.length() == 0)) return;
        String msg = j.optString("message", "");
        throw new PanError(msg.length() > 0 ? msg : fallback);
    }

    @Override public Session open(String shareId, String pwd) throws PanError {
        Session s = new Session();
        s.platform = id();
        s.shareId = shareId;
        s.pwd = pwd == null ? "" : pwd;
        s.stoken = "";
        s.title = "139 分享";
        try {
            
            JSONObject req = new JSONObject()
                    .put("linkID", shareId)
                    .put("isPasswd", 1)
                    .put("account", "");
            JSONObject plain = new JSONObject().put("getOutLinkGeneralReq", req);
            JSONObject j = sharePostAnonymous(SHARE_GENERAL_URL, plain.toString());
            JSONObject data = j.optJSONObject("data");
            JSONObject g = data == null ? null : data.optJSONObject("getOutLinkGeneralResp");
            JSONArray arr = g == null ? null : g.optJSONArray("outLinkGeneral");
            if (arr != null && arr.length() > 0) {
                JSONObject o = arr.optJSONObject(0);
                if (o != null) {
                    String name = o.optString("lkName");
                    if (name.length() > 0) s.title = name;
                    String passwd = o.optString("passwd");
                    if (s.pwd.length() == 0 && passwd.length() > 0) s.pwd = passwd;
                }
            }
            return s;
        } catch (PanError e) {
            throw e;
        } catch (Throwable t) {
            throw netError(t);
        }
    }

    @Override public List<PanItem> list(Session s, String pdirFid) throws PanError {
        try {
            String pcaId = pdirFid == null || pdirFid.length() == 0 || "0".equals(pdirFid) ? "root" : pdirFid;
            JSONObject req = new JSONObject()
                    .put("account", "")
                    .put("linkID", s.shareId)
                    .put("passwd", s.pwd == null ? "" : s.pwd)
                    .put("caSrt", 1)
                    .put("coSrt", 1)
                    .put("srtDr", 0)
                    .put("bNum", 1)
                    .put("pCaID", pcaId)
                    .put("eNum", 200);
            JSONObject plain = new JSONObject().put("getOutLinkInfoReq", req);
            JSONObject j = sharePostAnonymous(SHARE_LIST_URL, plain.toString());
            checkShare(j, "读取分享列表失败");
            JSONObject data = j.optJSONObject("data");
            List<PanItem> out = new ArrayList<PanItem>();
            if (data == null) return out;
            JSONArray ca = data.optJSONArray("caLst");
            if (ca != null) {
                for (int i = 0; i < ca.length(); i++) {
                    JSONObject o = ca.optJSONObject(i);
                    if (o == null) continue;
                    out.add(new PanItem(o.optString("caID"), o.optString("caName"), 0, true, "ca"));
                }
            }
            JSONArray co = data.optJSONArray("coLst");
            if (co != null) {
                for (int i = 0; i < co.length(); i++) {
                    JSONObject o = co.optJSONObject(i);
                    if (o == null) continue;
                    boolean dir = o.optInt("coType", 1) == 2 || o.optBoolean("isdir", false);
                    out.add(new PanItem(o.optString("coID"), o.optString("coName"),
                            o.optLong("coSize"), dir, "co"));
                }
            }
            return out;
        } catch (PanError e) {
            throw e;
        } catch (Throwable t) {
            throw netError(t);
        }
    }

    @Override public Link link(Session s, PanItem item) throws PanError {
        String auth = authorization();
        String acct = account();
        if (auth.length() == 0) throw new PanError("139 取直链需要登录：点上面「登录」，登完再来");
        try {
            JSONObject req = new JSONObject()
                    .put("account", acct)
                    .put("linkID", s.shareId)
                    .put("coIDLst", new JSONObject().put("item", new JSONArray().put(item.fid)))
                    .put("commonAccountInfo", new JSONObject().put("account", acct).put("accountType", 1));
            JSONObject plain = new JSONObject().put("dlFromOutLinkReqV3", req);
            JSONObject j = sharePostEncrypted(SHARE_LINK_URL, plain.toString(), auth);
            dbg("c139.link", "body=" + plain + "\n=> " + cut(j.toString()));
            checkShare(j, "获取下载链接失败");
            JSONObject data = j.optJSONObject("data");
            if (data == null) throw new PanError("没返回下载地址");
            String url = data.optString("redrUrl");
            if (url.length() == 0) throw new PanError("没返回下载地址（文件可能已失效）");
            Link l = new Link();
            l.url = url;
            l.filename = data.optString("fileName");
            if (l.filename.length() == 0) l.filename = item.name;
            l.size = data.optLong("coSize", item.size);
            l.ua = SHARE_MOBILE_UA;
            l.referer = "https://yun.139.com/";
            l.cookie = "";
            return l;
        } catch (PanError e) {
            throw e;
        } catch (Throwable t) {
            throw netError(t);
        }
    }

    @Override public List<PanItem> listMine(String pdirFid) throws PanError {
        String auth = authorization();
        if (auth.length() == 0) throw new PanError("先登录 139 网盘，才能看自己的网盘目录");
        try {
            String parent = pdirFid == null || pdirFid.length() == 0 || "0".equals(pdirFid) ? "root" : pdirFid;
            JSONObject req = new JSONObject()
                    .put("pageInfo", new JSONObject().put("pageSize", 100).put("pageCursor", JSONObject.NULL))
                    .put("orderBy", "updated_at")
                    .put("orderDirection", "DESC")
                    .put("parentFileId", parent)
                    .put("imageThumbnailStyleList", new JSONArray().put("Small").put("Large"));
            JSONObject j = cloudPost(FILE_LIST_URL, req.toString(), auth, false);
            checkCloud(j, "读取网盘目录失败");
            JSONObject data = j.optJSONObject("data");
            List<PanItem> out = new ArrayList<PanItem>();
            if (data == null) return out;
            JSONArray items = data.optJSONArray("items");
            if (items == null) return out;
            for (int i = 0; i < items.length(); i++) {
                JSONObject o = items.optJSONObject(i);
                if (o == null) continue;
                boolean dir = "folder".equals(o.optString("type"));
                out.add(new PanItem(o.optString("fileId"), o.optString("name"),
                        o.optLong("size"), dir, o.optString("fileId")));
            }
            return out;
        } catch (PanError e) {
            throw e;
        } catch (Throwable t) {
            throw netError(t);
        }
    }

    @Override public Link linkMine(PanItem item) throws PanError {
        String auth = authorization();
        if (auth.length() == 0) throw new PanError("先登录 139 网盘");
        try {
            JSONObject req = new JSONObject().put("fileId", item.fid);
            JSONObject j = cloudPost(DOWNLOAD_URL, req.toString(), auth, false);
            checkCloud(j, "获取下载链接失败");
            JSONObject data = j.optJSONObject("data");
            if (data == null) throw new PanError("没返回下载地址");
            String url = data.optString("url");
            if (url.length() == 0) throw new PanError("没返回下载地址");
            Link l = new Link();
            l.url = url;
            l.filename = item.name;
            l.size = item.size;
            l.ua = PC_UA;
            l.referer = "https://yun.139.com/";
            l.cookie = "";
            return l;
        } catch (PanError e) {
            throw e;
        } catch (Throwable t) {
            throw netError(t);
        }
    }

    @Override public ShareInfo createShare(PanItem item, String passcode, int expiredType) throws PanError {
        String auth = authorization();
        String acct = account();
        if (auth.length() == 0) throw new PanError("先登录 139 网盘，才能创建分享");
        try {
            JSONObject req = new JSONObject()
                    .put("subLinkType", 0)
                    .put("encrypt", 1)
                    .put("coIDLst", new JSONArray())
                    .put("caIDLst", new JSONArray())
                    .put("pubType", 1)
                    .put("dedicatedName", item.name)
                    .put("periodUnit", 1)
                    .put("viewerLst", new JSONArray())
                    .put("extInfo", new JSONObject().put("isWatermark", 0).put("shareChannel", "3001"))
                    .put("commonAccountInfo", new JSONObject().put("account", acct).put("accountType", 1));
            
            if (expiredType == 2) req.put("period", 1);
            else if (expiredType == 3) req.put("period", 7);
            else if (expiredType == 4) req.put("period", 30);
            if (item.dir) req.put("caIDLst", new JSONArray().put(item.fid));
            else req.put("coIDLst", new JSONArray().put(item.fid));

            JSONObject plain = new JSONObject().put("getOutLinkReq", req);
            JSONObject j = cloudPost(OUTLINK_CREATE_URL, plain.toString(), auth, true);
            dbg("c139.createShare", "body=" + plain + "\n=> " + cut(j.toString()));
            if (!j.optBoolean("success", true) || !"0".equals(j.optString("code", "0"))) {
                String msg = j.optString("message", "");
                throw new PanError(msg.length() > 0 ? msg : "创建分享失败");
            }
            JSONObject set = j.optJSONObject("data");
            JSONObject res = set == null ? null : set.optJSONObject("getOutLinkRes");
            JSONArray arr = res == null ? null : res.optJSONArray("getOutLinkResSet");
            JSONObject first = arr == null ? null : arr.optJSONObject(0);
            if (first == null) throw new PanError("创建分享失败：没返回链接");
            String link = first.optString("linkUrl");
            if (link.length() == 0) throw new PanError("创建分享失败：没返回链接");
            ShareInfo si = new ShareInfo();
            si.url = link;
            si.pwd = first.optString("passwd");
            si.title = item.name;
            return si;
        } catch (PanError e) {
            throw e;
        } catch (Throwable t) {
            throw netError(t);
        }
    }

    private static String cut(String s) {
        if (s == null) return "";
        return s.length() > 400 ? s.substring(0, 400) + "…" : s;
    }
}
