package com.cathelperawa.owo.app.pan;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.zip.CRC32;

public final class Pan123Pan extends PanProvider {

    private static final String API_BASE = "https://yun.123pan.cn";
    private static final String DOWNLOAD_BASE = "https://www.123865.com";
    private static final String SHARE_GET_URL = API_BASE + "/b/api/share/get";
    private static final String SHARE_DOWNLOAD_INFO_URL = DOWNLOAD_BASE + "/b/api/share/download/info";
    private static final String FILE_LIST_URL = API_BASE + "/b/api/file/list/new";
    private static final String FILE_DOWNLOAD_INFO_URL = API_BASE + "/api/file/download_info";

    private static final String WEB_UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) "
                    + "Chrome/127.0.0.0 Safari/537.36";
    private static final String DART_UA = "Dart/3.12 (dart:io)";
    private static final String SIGN_TABLE = "adefghlmyijnopkqrstubcvwsz";
    private static final long SIGN_OFFSET_SECONDS = 57600L;
    private static final String DOWNLOAD_REFERER = "https://yun.123pan.cn/";

    private final String loginuuid = newLoginUuid();

    @Override public String id() { return "pan123"; }

    @Override public String name() { return "123 云盘"; }

    @Override public String referer() { return DOWNLOAD_REFERER; }

    @Override public boolean loggedIn() {
        return token().length() > 0;
    }

    private String token() {
        if (cookie == null) return "";
        for (String part : cookie.split(";")) {
            String p = part.trim();
            if (p.startsWith("authorToken=") && p.length() > "authorToken=".length()) {
                return p.substring("authorToken=".length());
            }
        }
        return "";
    }

    private static String crc32Hex(String s) {
        CRC32 crc = new CRC32();
        try {
            crc.update(s.getBytes("UTF-8"));
        } catch (Throwable ignore) {
        }
        return Long.toHexString(crc.getValue() & 0xFFFFFFFFL);
    }

    private static String[] sign(String path) {
        long ts = System.currentTimeMillis() / 1000L;
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis((ts + SIGN_OFFSET_SECONDS) * 1000L);
        String minute = String.format(java.util.Locale.US, "%04d%02d%02d%02d%02d",
                cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH),
                cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE));
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < minute.length(); i++) {
            int d = minute.charAt(i) - '0';
            if (d < 0 || d > 9) d = 0;
            sb.append(SIGN_TABLE.charAt(d));
        }
        String authKey = crc32Hex(sb.toString());
        int random = (int) (Math.random() * 10000000);
        String data = ts + "|" + random + "|" + path + "|web|3|" + authKey;
        return new String[]{authKey, ts + "-" + random + "-" + crc32Hex(data)};
    }

    private static String newLoginUuid() {
        String chars = "0123456789abcdef";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 32; i++) sb.append(chars.charAt((int) (Math.random() * 16)));
        return sb.toString();
    }

    private Map<String, String> authHeaders(String path, String token, String platform, String appVer) {
        String[] s = sign(path);
        Map<String, String> h = PanHttp.headers(
                "platform", platform,
                "app-version", appVer,
                "loginuuid", loginuuid,
                "auth-key", s[0],
                "auth-value", s[1],
                "User-Agent", WEB_UA,
                "Accept", "application/json, text/plain, */*");
        if (token != null && token.length() > 0) h.put("authorization", "Bearer " + token);
        return h;
    }

    private static void checkOk(JSONObject j, String fallback) throws PanError {
        int code = j.optInt("code", -1);
        if (code == 0) return;
        String msg = j.optString("message", "");
        throw new PanError(msg.length() > 0 ? msg : fallback);
    }

    @Override public Session open(String shareId, String pwd) throws PanError {

        Session s = new Session();
        s.platform = id();
        s.shareId = shareId;
        s.pwd = pwd == null ? "" : pwd;
        s.stoken = shareId;
        s.title = "123 云盘分享";
        return s;
    }

    @Override public List<PanItem> list(Session s, String pdirFid) throws PanError {
        try {
            String fid = pdirFid == null || pdirFid.length() == 0 ? "0" : pdirFid;
            String url = SHARE_GET_URL
                    + "?limit=100&next=0&orderBy=file_name&orderDirection=asc"
                    + "&shareKey=" + URLEncoder.encode(s.shareId, "UTF-8")
                    + "&ParentFileId=" + fid
                    + "&Page=1";
            if (s.pwd != null && s.pwd.length() > 0) {
                url += "&SharePwd=" + URLEncoder.encode(s.pwd, "UTF-8");
            }
            PanHttp.Resp r = PanHttp.get(url, PanHttp.headers("User-Agent", DART_UA));
            JSONObject j = json(r.body);
            checkOk(j, "获取文件列表失败");
            JSONObject d = j.optJSONObject("data");
            List<PanItem> out = new ArrayList<PanItem>();
            if (d == null) return out;
            if (d.optBoolean("Expired", false)) throw new PanError("分享已失效");
            JSONArray arr = d.optJSONArray("InfoList");
            if (arr == null) return out;
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o == null) continue;
                out.add(new PanItem(
                        o.optString("FileId"),
                        o.optString("FileName"),
                        o.optLong("Size"),
                        o.optInt("Type", 0) == 1,
                        o.optString("S3KeyFlag") + "|" + o.optString("Etag") + "|" + o.optString("StorageNode")));
            }
            return out;
        } catch (PanError e) {
            throw e;
        } catch (Throwable t) {
            throw netError(t);
        }
    }

    @Override public Link link(Session s, PanItem item) throws PanError {
        String tk = token();
        if (tk.length() == 0) throw new PanError("123 云盘取直链需要登录：点上面「登录」，登完再下");
        try {
            String[] parts = splitToken(item.token);
            JSONObject body = new JSONObject()
                    .put("ShareKey", s.shareId)
                    .put("FileID", Long.parseLong(item.fid.length() == 0 ? "0" : item.fid))
                    .put("S3KeyFlag", parts[0])
                    .put("Size", item.size)
                    .put("Etag", parts[1]);
            PanHttp.Resp r = PanHttp.post(SHARE_DOWNLOAD_INFO_URL, body.toString(),
                    authHeaders("/b/api/share/download/info", tk, "android", "39"));
            JSONObject j = json(r.body);
            checkOk(j, "获取下载链接失败");
            JSONObject d = j.optJSONObject("data");
            if (d == null) throw new PanError("服务端没给直链");
            String raw = d.optString("DownloadURL");
            if (raw.length() == 0) throw new PanError("直链为空");
            Link l = new Link();
            l.url = followRedirect(decodeDownloadUrl(raw));
            l.filename = item.name;
            l.size = item.size;
            l.ua = WEB_UA;
            l.referer = DOWNLOAD_REFERER;
            l.cookie = "";
            return l;
        } catch (PanError e) {
            throw e;
        } catch (Throwable t) {
            throw netError(t);
        }
    }

    @Override public List<PanItem> listMine(String pdirFid) throws PanError {
        String tk = token();
        if (tk.length() == 0) throw new PanError("先登录 123 云盘，才能看自己的网盘目录");
        try {
            String fid = pdirFid == null || pdirFid.length() == 0 ? "0" : pdirFid;
            String url = FILE_LIST_URL
                    + "?driveId=0&limit=100&next=0&orderBy=update_time&orderDirection=desc"
                    + "&parentFileId=" + URLEncoder.encode(fid, "UTF-8")
                    + "&trashed=false&SearchData=&Page=1&OnlyLookAbnormalFile=0"
                    + "&event=homeListFile&operateType=1&inDirectSpace=false";
            PanHttp.Resp r = PanHttp.get(url, authHeaders("/b/api/file/list/new", tk, "web", "3"));
            JSONObject j = json(r.body);
            checkOk(j, "获取文件列表失败");
            JSONObject d = j.optJSONObject("data");
            List<PanItem> out = new ArrayList<PanItem>();
            if (d == null) return out;
            JSONArray arr = d.optJSONArray("InfoList");
            if (arr == null) return out;
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o == null) continue;
                out.add(new PanItem(
                        o.optString("FileId"),
                        o.optString("FileName"),
                        o.optLong("Size"),
                        o.optInt("Type", 0) == 1,
                        o.optString("S3KeyFlag") + "|" + o.optString("Etag") + "|" + o.optString("StorageNode")));
            }
            return out;
        } catch (PanError e) {
            throw e;
        } catch (Throwable t) {
            throw netError(t);
        }
    }

    @Override public Link linkMine(PanItem item) throws PanError {
        String tk = token();
        if (tk.length() == 0) throw new PanError("先登录 123 云盘");
        try {
            String[] parts = splitToken(item.token);
            JSONObject body = new JSONObject()
                    .put("driveId", 0)
                    .put("etag", parts[1])
                    .put("fileId", Long.parseLong(item.fid.length() == 0 ? "0" : item.fid))
                    .put("s3keyFlag", parts[0])
                    .put("type", 0)
                    .put("fileName", item.name)
                    .put("size", item.size);
            PanHttp.Resp r = PanHttp.post(FILE_DOWNLOAD_INFO_URL, body.toString(),
                    authHeaders("/api/file/download_info", tk, "web", "3"));
            JSONObject j = json(r.body);
            checkOk(j, "获取下载链接失败");
            JSONObject d = j.optJSONObject("data");
            if (d == null) throw new PanError("服务端没给直链");
            String raw = d.optString("DownloadUrl");
            if (raw.length() == 0) throw new PanError("直链为空");
            Link l = new Link();
            l.url = followRedirect(decodeDownloadUrl(raw));
            l.filename = item.name;
            l.size = item.size;
            l.ua = WEB_UA;
            l.referer = DOWNLOAD_REFERER;
            l.cookie = "";
            return l;
        } catch (PanError e) {
            throw e;
        } catch (Throwable t) {
            throw netError(t);
        }
    }

    @Override public ShareInfo createShare(PanItem item, String passcode, int expiredType) throws PanError {
        String tk = token();
        if (tk.length() == 0) throw new PanError("先登录 123 云盘，才能创建分享");
        try {
            String expiration = expirationOf(expiredType);
            JSONObject body = new JSONObject()
                    .put("driveId", 0)
                    .put("expiration", expiration)
                    .put("fileIdList", Long.parseLong(item.fid.length() == 0 ? "0" : item.fid))
                    .put("shareName", item.name)
                    .put("event", "shareCreate")
                    .put("fileNum", 1)
                    .put("shareModality", 4)
                    .put("trafficLimitSwitch", 1)
                    .put("trafficLimit", 0)
                    .put("trafficSwitch", 1)
                    .put("fillPwdSwitch", passcode != null && passcode.length() > 0 ? 1 : 0);
            if (passcode != null && passcode.length() > 0) body.put("sharePwd", passcode);
            PanHttp.Resp r = PanHttp.post("https://yun.123pan.cn/b/api/share/create",
                    body.toString(), authHeaders("/b/api/share/create", tk, "web", "3"));
            dbg("pan123.createShare", "body=" + body + "\nHTTP " + r.code + " => "
                    + (r.body == null ? "" : (r.body.length() > 500 ? r.body.substring(0, 500) : r.body)));
            JSONObject j = json(r.body);
            checkOk(j, "创建分享失败");
            JSONObject d = j.optJSONObject("data");
            if (d == null) throw new PanError("创建分享失败：没返回数据");
            String shareKey = d.optString("ShareKey");
            if (shareKey.length() == 0) throw new PanError("创建分享失败：没返回 ShareKey");
            JSONObject links = d.optJSONObject("shareLinkList");
            String url = "";
            if (links != null) {
                JSONArray list = links.optJSONArray("list");
                if (list != null && list.length() > 0) url = list.optString(0);
                if (url.length() == 0) url = links.optString("standBy");
            }
            if (url.length() == 0) url = "https://www.123pan.com/s/" + shareKey;
            ShareInfo si = new ShareInfo();
            si.url = url;
            si.pwd = passcode == null ? "" : passcode;
            si.title = item.name;
            return si;
        } catch (PanError e) {
            throw e;
        } catch (Throwable t) {
            throw netError(t);
        }
    }

    private static String expirationOf(int expiredType) {
        if (expiredType <= 1) return "2099-12-12T08:00:00+08:00";
        int days = expiredType == 2 ? 1 : (expiredType == 3 ? 7 : 30);
        java.util.Calendar c = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("GMT+8"));
        c.add(java.util.Calendar.DAY_OF_MONTH, days);
        java.text.SimpleDateFormat f = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US);
        f.setTimeZone(java.util.TimeZone.getTimeZone("GMT+8"));
        return f.format(c.getTime()) + "+08:00";
    }

    private static String[] splitToken(String tok) {
        String[] p = (tok == null ? "" : tok).split("\\|");
        String a = p.length > 0 ? p[0] : "";
        String b = p.length > 1 ? p[1] : "";
        return new String[]{a, b};
    }

    private static String decodeDownloadUrl(String raw) {
        String s = raw == null ? "" : raw.trim();
        if (s.length() == 0) return s;
        try {
            if (!s.contains("://")) {
                String dec = new String(android.util.Base64.decode(s, android.util.Base64.DEFAULT), "UTF-8");
                if (dec.toLowerCase().startsWith("http")) return dec;
                return s;
            }
            int idx = s.indexOf("params=");
            if (idx < 0) return s;
            String p = s.substring(idx + "params=".length());
            int amp = p.indexOf('&');
            if (amp >= 0) p = p.substring(0, amp);
            p = p.replace('-', '+').replace('_', '/');
            String dec = new String(android.util.Base64.decode(p, android.util.Base64.DEFAULT), "UTF-8");
            if (dec.toLowerCase().startsWith("http")) return dec;
        } catch (Throwable ignore) {
        }
        return s;
    }

    private static String followRedirect(String url) {
        String cur = url;
        for (int i = 0; i < 5; i++) {
            String next = probeRedirect(cur);
            if (next == null || next.length() == 0) return cur;
            cur = next;
        }
        return cur;
    }

    private static String probeRedirect(String url) {
        HttpURLConnection c = null;
        try {
            c = (HttpURLConnection) new URL(url).openConnection();
            c.setConnectTimeout(8000);
            c.setReadTimeout(8000);
            c.setRequestProperty("User-Agent", WEB_UA);
            c.setRequestProperty("Referer", DOWNLOAD_REFERER);
            int code = c.getResponseCode();
            if (code < 200 || code >= 300) return null;
            String ct = c.getContentType();
            long len = c.getContentLengthLong();
            if (ct == null || ct.indexOf("json") < 0) return null;
            if (len > 8192) return null;
            InputStream in = c.getInputStream();
            java.io.ByteArrayOutputStream bo = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[2048];
            int n;
            while ((n = in.read(buf)) > 0 && bo.size() < 8192) bo.write(buf, 0, n);
            in.close();
            JSONObject j = new JSONObject(new String(bo.toByteArray(), "UTF-8"));
            JSONObject d = j.optJSONObject("data");
            if (d != null) {
                String r = d.optString("redirect_url");
                if (r.length() > 0) return r;
            }
            return null;
        } catch (Throwable t) {
            return null;
        } finally {
            if (c != null) c.disconnect();
        }
    }
}
