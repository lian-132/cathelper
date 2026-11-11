package com.cathelperawa.owo.app.pan;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class BaiduPan extends PanProvider {

    private static final String UA_WEB =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) "
                    + "Chrome/124.0.0.0 Safari/537.36";
    private static final String UA_NETDISK =
            "netdisk;12.24.6;piano;android-android;16;JSbridge4.4.0;jointBridge;1.1.0";
    private static final String APP_ID = "250528";
    private static final String TEMP_DIR = "/猫娘网盘临时";

    private String bdstoken = "";

    @Override public String id() { return "baidu"; }

    @Override public String name() { return "百度网盘"; }

    @Override public String referer() { return "https://pan.baidu.com/"; }

    @Override public boolean loggedIn() {
        return cookie != null && cookie.contains("BDUSS=");
    }

    private Map<String, String> webHeaders(String ck) {
        Map<String, String> h = PanHttp.headers("User-Agent", UA_WEB);
        if (ck != null && ck.length() > 0) h.put("Cookie", ck);
        return h;
    }

    private Map<String, String> diskHeaders(String ck) {
        Map<String, String> h = PanHttp.headers("User-Agent", UA_NETDISK);
        if (ck != null && ck.length() > 0) h.put("Cookie", ck);
        return h;
    }

    private static int errno(JSONObject j) {
        return j.optInt("errno", 0);
    }

    private static String errmsg(JSONObject j, String fallback) {
        String m = j.optString("errmsg", "");
        int e = errno(j);
        if (m.length() == 0) m = fallback;
        return m + "（errno=" + e + "）";
    }

    @Override public Session open(String shareId, String pwd) throws PanError {
        Session s = new Session();
        s.platform = id();
        s.shareId = shareId;
        s.pwd = pwd == null ? "" : pwd;
        if (s.pwd.length() > 0) {
            if (!loggedIn()) throw new PanError("百度这个分享要提取码，验证需要登录（先去登录百度网盘）");
            s.stoken = verify(shareId, s.pwd);
        } else {
            s.stoken = "";
        }
        s.title = "百度网盘分享";
        return s;
    }

    private String verify(String surl, String pwd) throws PanError {
        try {
            String body = "pwd=" + URLEncoder.encode(pwd, "UTF-8") + "&vcode_str=&vcode=";
            Map<String, String> h = PanHttp.headers(
                    "User-Agent", UA_WEB,
                    "Referer", "https://pan.baidu.com/s/" + surl,
                    "Content-Type", "application/x-www-form-urlencoded");
            if (cookie != null && cookie.length() > 0) h.put("Cookie", cookie);
            PanHttp.Resp r = PanHttp.post("https://pan.baidu.com/share/verify?surl=" + surl, body, h);
            JSONObject j = json(r.body);
            if (errno(j) != 0) throw new PanError(errmsg(j, "验证提取码失败"));
            String rsk = j.optString("randsk");
            if (rsk.length() == 0) throw new PanError("没拿到分享密钥（randsk）");
            return rsk;
        } catch (PanError e) {
            throw e;
        } catch (Throwable t) {
            throw netError(t);
        }
    }

    @Override public List<PanItem> list(Session s, String pdirFid) throws PanError {
        try {
            boolean root = pdirFid == null || pdirFid.length() == 0
                    || "/".equals(pdirFid) || "0".equals(pdirFid);
            String dir = root ? "/" : pdirFid;
            String url = "https://pan.baidu.com/rest/2.0/xpan/share?method=list"
                    + "&shorturl=" + s.shareId
                    + "&page=1&num=100&root=" + (root ? "1" : "0")
                    + "&dir=" + URLEncoder.encode(dir, "UTF-8");
            String ck = cookie == null ? "" : cookie;
            if (s.stoken != null && s.stoken.length() > 0) {
                url += "&sekey=" + s.stoken;
                if (!ck.contains("BDCLND=")) ck = ck + "; BDCLND=" + s.stoken;
            }
            PanHttp.Resp r = PanHttp.get(url, webHeaders(ck));
            JSONObject j = json(r.body);
            if (errno(j) != 0) {
                if (s.stoken == null || s.stoken.length() == 0) {
                    throw new PanError(errno(j) == -6
                            ? "该分享需要提取码，或者要登录百度网盘才能看（errno=-6）"
                            : "该分享需要提取码（errno=" + errno(j) + "）");
                }
                throw new PanError(errmsg(j, "获取分享文件列表失败"));
            }
            if (s.title == null || s.title.length() == 0 || "百度网盘分享".equals(s.title)) {
                String t = j.optString("title");
                if (t.length() > 0) s.title = t;
            }
            s.uk = j.optString("uk");
            s.shareIdInt = j.optString("share_id");
            JSONArray arr = j.optJSONArray("list");
            List<PanItem> out = new ArrayList<PanItem>();
            if (arr == null) return out;
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o == null) continue;
                boolean isdir = "1".equals(o.optString("isdir"));
                String path = o.optString("path");
                out.add(new PanItem(
                        isdir ? path : o.optString("fs_id"),
                        o.optString("server_filename"),
                        o.optLong("size"),
                        isdir,
                        o.optString("fs_id")));
            }
            return out;
        } catch (PanError e) {
            throw e;
        } catch (Throwable t) {
            throw netError(t);
        }
    }

    @Override public Link link(Session s, PanItem item) throws PanError {
        if (!loggedIn()) throw new PanError("下载百度分享必须登录（点上面「登录」）");
        try {
            String token = bdstoken();
            ensureTempDir();
            String newPath = transfer(s, item, token);
            String url = locateDownload(newPath);
            Link l = new Link();
            l.url = url;
            l.filename = item.name;
            l.size = item.size;
            l.ua = UA_NETDISK;
            l.referer = "https://pan.baidu.com/";
            l.cookie = cookie;
            return l;
        } catch (PanError e) {
            throw e;
        } catch (Throwable t) {
            throw netError(t);
        }
    }

    @Override public List<PanItem> listMine(String pdirFid) throws PanError {
        if (!loggedIn()) throw new PanError("先登录百度网盘，才能看自己的网盘目录");
        try {
            String dir = (pdirFid == null || pdirFid.length() == 0
                    || "0".equals(pdirFid)) ? "/" : pdirFid;
            String token = bdstoken();
            
            String url = "https://pan.baidu.com/api/list?clienttype=0&app_id=" + APP_ID
                    + "&web=1&order=time&desc=1&num=100&page=1&bdstoken=" + token
                    + "&dir=" + URLEncoder.encode(dir, "UTF-8");
            PanHttp.Resp r = PanHttp.get(url, diskHeaders(cookie));
            dbg("baidu.listMine", "GET " + url + "\nHTTP " + r.code + " => "
                    + (r.body == null ? "" : (r.body.length() > 400 ? r.body.substring(0, 400) : r.body)));
            JSONObject j = json(r.body);
            if (errno(j) != 0) throw new PanError(errmsg(j, "读取网盘目录失败"));
            JSONArray arr = j.optJSONArray("list");
            List<PanItem> out = new ArrayList<PanItem>();
            if (arr == null) return out;
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o == null) continue;
                String path = o.optString("path");
                boolean isdir = o.optInt("isdir", 0) == 1;
                out.add(new PanItem(
                        isdir ? path : o.optString("fs_id"),
                        o.optString("server_filename"),
                        o.optLong("size"),
                        isdir,
                        o.optString("fs_id")));
            }
            return out;
        } catch (PanError e) {
            throw e;
        } catch (Throwable t) {
            throw netError(t);
        }
    }

    @Override public Link linkMine(PanItem item) throws PanError {
        if (!loggedIn()) throw new PanError("先登录百度网盘");
        try {
            String token = bdstoken();
            String url = "https://pan.baidu.com/api/filemetas?dlink=1&fsids="
                    + URLEncoder.encode("[\"" + item.fid + "\"]", "UTF-8")
                    + "&bdstoken=" + token;
            PanHttp.Resp r = PanHttp.get(url, diskHeaders(cookie));
            JSONObject j = json(r.body);
            if (errno(j) != 0) throw new PanError(errmsg(j, "获取直链失败"));
            JSONArray arr = j.optJSONArray("info");
            String dlink = "";
            if (arr != null && arr.length() > 0) {
                JSONObject o = arr.optJSONObject(0);
                if (o != null) dlink = o.optString("dlink");
            }
            if (dlink.length() == 0) throw new PanError("没拿到直链（文件可能被和谐）");
            Link l = new Link();
            l.url = dlink;
            l.filename = item.name;
            l.size = item.size;
            l.ua = UA_NETDISK;
            l.referer = "https://pan.baidu.com/";
            l.cookie = cookie;
            return l;
        } catch (PanError e) {
            throw e;
        } catch (Throwable t) {
            throw netError(t);
        }
    }

    @Override public ShareInfo createShare(PanItem item, String passcode, int expiredType) throws PanError {
        if (!loggedIn()) throw new PanError("先登录百度网盘，才能创建分享");
        try {
            String fsId = item.token != null && item.token.length() > 0 ? item.token : item.fid;
            int period;
            switch (expiredType) {
                case 2: period = 1; break;
                case 3: period = 7; break;
                case 4: period = 30; break;
                default: period = 0; break;   
            }
            String token = bdstoken();

            String pwd = passcode == null ? "" : passcode.trim();
            if (pwd.length() != 4) pwd = randomPwd4();
            String body = "fid_list=" + URLEncoder.encode("[\"" + fsId + "\"]", "UTF-8")
                    + "&schannel=4&channel_list=%5B%5D&period=" + period
                    + "&pwd=" + URLEncoder.encode(pwd, "UTF-8");
            Map<String, String> h = PanHttp.headers(
                    "User-Agent", UA_NETDISK,
                    "Referer", "https://yun.baidu.com/disk/main",
                    "Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
            if (cookie != null && cookie.length() > 0) h.put("Cookie", cookie);
            PanHttp.Resp r = PanHttp.post("https://pan.baidu.com/share/set?channel=chunlei&web=1&app_id="
                    + APP_ID + "&bdstoken=" + token + "&clienttype=0", body, h);
            dbg("baidu.createShare", "body=" + body + "\nHTTP " + r.code + " => "
                    + (r.body == null ? "" : (r.body.length() > 500 ? r.body.substring(0, 500) : r.body)));
            JSONObject j = json(r.body);
            if (errno(j) != 0) throw new PanError(errmsg(j, "创建分享失败"));
            String link = j.optString("shorturl");
            if (link.length() == 0) link = j.optString("link");
            if (link.length() == 0) throw new PanError("没返回分享链接");
            if (!link.startsWith("http")) link = "https://pan.baidu.com/s/" + link;
            ShareInfo si = new ShareInfo();
            si.url = link;
            si.pwd = pwd;
            si.title = item.name;
            return si;
        } catch (PanError e) {
            throw e;
        } catch (Throwable t) {
            throw netError(t);
        }
    }

    private static String randomPwd4() {
        String chars = "abcdefghjkmnpqrstuvwxyz23456789";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 4; i++) sb.append(chars.charAt((int) (Math.random() * chars.length())));
        return sb.toString();
    }

    private String bdstoken() throws PanError {
        if (bdstoken.length() > 0) return bdstoken;
        try {
            String url = "https://pan.baidu.com/api/gettemplatevariable?clienttype=0&app_id=" + APP_ID
                    + "&web=1&fields=" + URLEncoder.encode("[\"bdstoken\"]", "UTF-8");
            PanHttp.Resp r = PanHttp.get(url, webHeaders(cookie));
            JSONObject j = json(r.body);
            if (errno(j) == 0) {
                JSONObject res = j.optJSONObject("result");
                if (res != null) bdstoken = res.optString("bdstoken");
            }
            if (bdstoken.length() == 0) throw new PanError("拿不到 bdstoken（登录可能失效，重登百度）");
            return bdstoken;
        } catch (PanError e) {
            throw e;
        } catch (Throwable t) {
            throw netError(t);
        }
    }

    private void ensureTempDir() throws PanError {
        try {
            String url = "https://pan.baidu.com/api/list?clienttype=0&app_id=" + APP_ID
                    + "&web=1&order=time&desc=1&num=100&page=1&dir=/";
            PanHttp.Resp r = PanHttp.get(url, diskHeaders(cookie));
            JSONObject j = json(r.body);
            JSONArray arr = j.optJSONArray("list");
            if (arr != null) {
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.optJSONObject(i);
                    if (o != null && TEMP_DIR.equals(o.optString("path"))) return;
                }
            }
            String token = bdstoken();
            String body = "path=" + URLEncoder.encode(TEMP_DIR, "UTF-8")
                    + "&isdir=1&size&block_list=%5B%5D&method=post&dataType=json";
            Map<String, String> h = PanHttp.headers(
                    "User-Agent", UA_NETDISK,
                    "Referer", "https://yun.baidu.com/disk/main",
                    "Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
            if (cookie != null && cookie.length() > 0) h.put("Cookie", cookie);
            PanHttp.post("https://pan.baidu.com/api/create?a=commit&channel=chunlei&web=1&app_id="
                    + APP_ID + "&clienttype=0&bdstoken=" + token, body, h);
        } catch (Throwable ignore) {
            
        }
    }

    private String transfer(Session s, PanItem item, String token) throws PanError {
        try {
            String url = "https://pan.baidu.com/share/transfer?shareid=" + s.shareIdInt
                    + "&from=" + s.uk
                    + "&channel=chunlei&sekey=" + (s.stoken == null ? "" : s.stoken)
                    + "&ondup=newcopy&web=1&app_id=" + APP_ID
                    + "&bdstoken=" + token + "&clienttype=0";
            String body = "fsidlist=%5B%22" + item.fid + "%22%5D&path="
                    + URLEncoder.encode(TEMP_DIR, "UTF-8");
            String ck = cookie == null ? "" : cookie;
            if (s.stoken != null && s.stoken.length() > 0 && !ck.contains("BDCLND=")) {
                ck = ck + "; BDCLND=" + s.stoken;
            }
            Map<String, String> h = PanHttp.headers(
                    "User-Agent", UA_WEB,
                    "Origin", "https://pan.baidu.com",
                    "Referer", "https://pan.baidu.com/s/",
                    "Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
            if (ck.length() > 0) h.put("Cookie", ck);
            PanHttp.Resp r = PanHttp.post(url, body, h);
            JSONObject j = json(r.body);
            if (errno(j) != 0) {
                throw new PanError(errmsg(j, "转存失败") + "（可能共享文件过多/触发风控，稍后再试）");
            }
            JSONObject extra = j.optJSONObject("extra");
            JSONArray list = extra == null ? null : extra.optJSONArray("list");
            if (list != null && list.length() > 0) {
                JSONObject o = list.optJSONObject(0);
                if (o != null) {
                    String to = o.optString("to");
                    if (to.length() > 0) return to;
                }
            }
            
            return TEMP_DIR + "/" + item.name;
        } catch (PanError e) {
            throw e;
        } catch (Throwable t) {
            throw netError(t);
        }
    }

    private String locateDownload(String path) throws PanError {
        try {
            long time = System.currentTimeMillis() / 1000L;
            String url = "https://d.pcs.baidu.com/rest/2.0/pcs/file"
                    + "?method=locatedownload&app_id=" + APP_ID
                    + "&clienttype=17&ver=4.0&ant=1&check_blue=1&es=1&esl=1&apn_id=1_-1"
                    + "&freeisp=0&queryfree=0&use=1&dtype=1&eck=1&ehps=1&err_ver=1.0"
                    + "&network_type=WIFI&channel=0"
                    + "&path=" + URLEncoder.encode(path, "UTF-8")
                    + "&time=" + time
                    + "&rand=5ed606e9da222cde0474cdf70eda884b"
                    + "&devuid=0F1E9FC2E084472DA5A61C4CF4C759AF"
                    + "&cuid=0F1E9FC2E084472DA5A61C4CF4C759AF"
                    + "&deviceid=348642637967375013"
                    + "&psign=860a071f77c860e8cea06e4e54c518f3"
                    + "&version=2.2.111.34&version_app=12.24.6&vip=0";
            Map<String, String> h = PanHttp.headers(
                    "User-Agent", UA_NETDISK,
                    "Content-Type", "application/x-www-form-urlencoded");
            if (cookie != null && cookie.length() > 0) h.put("Cookie", cookie);
            PanHttp.Resp r = PanHttp.post(url, "0", h);
            JSONObject j = json(r.body);
            if (errno(j) != 0) throw new PanError(errmsg(j, "获取高速下载链接失败"));
            JSONArray urls = j.optJSONArray("urls");
            if (urls == null || urls.length() == 0) throw new PanError("没返回下载地址");
            String plain = "";
            String https = "";
            String any = "";
            for (int i = 0; i < urls.length(); i++) {
                JSONObject o = urls.optJSONObject(i);
                if (o == null) continue;
                String u = o.optString("url");
                if (u.length() == 0) continue;
                if (any.length() == 0) any = u;
                if (o.optInt("encrypt", 1) == 0 && u.startsWith("https")) {
                    plain = u;
                    break;   
                }
                if (https.length() == 0 && u.startsWith("https")) https = u;
            }
            if (plain.length() > 0) return plain;
            if (https.length() > 0) return https;
            if (any.length() > 0) return any;
            throw new PanError("没返回可用下载地址");
        } catch (PanError e) {
            throw e;
        } catch (Throwable t) {
            throw netError(t);
        }
    }
}
