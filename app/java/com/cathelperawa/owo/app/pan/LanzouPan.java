package com.cathelperawa.owo.app.pan;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LanzouPan extends PanProvider {

    private static final String UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) "
                    + "Chrome/131.0.0.0 Safari/537.36";

    private static final Pattern RE_IFRAME = Pattern.compile("<iframe[^>]+src=[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);
    private static final Pattern RE_FILE_ID = Pattern.compile("(?:ajaxm|ajaxfile)\\.php\\?file=(\\d+)");
    private static final Pattern RE_FILE_ID2 = Pattern.compile("file=(\\d+)");
    private static final Pattern RE_TITLE = Pattern.compile("<title>([^<]*)</title>", Pattern.CASE_INSENSITIVE);
    private static final Pattern RE_SIZE = Pattern.compile("(?:文件大小|大小)\\s*[:：]\\s*([\\d.]+)\\s*([KMGT]?)", Pattern.CASE_INSENSITIVE);

    private final Map<String, String> cookieJar = new HashMap<String, String>();

    @Override public String id() { return "lanzou"; }

    @Override public String name() { return "蓝奏云"; }

    @Override public String referer() { return "https://www.lanzou.com/"; }

    @Override public boolean loggedIn() { return true; }

    @Override public boolean canMine() {
        return cookie != null && cookie.contains("phpdisk_info=");
    }

    @Override public Session open(String shareId, String pwd) throws PanError {
        Session s = new Session();
        s.platform = id();
        s.shareId = shareId;
        s.pwd = pwd == null ? "" : pwd;
        s.stoken = "";
        try {
            String url = shareId.startsWith("http") ? shareId : ("https://" + shareId);
            Page page = loadPage(url, s.pwd);
            s.title = page.title;
            if (page.needsPwd && s.pwd.length() == 0) throw new PanError("这个蓝奏分享需要提取码");
            return s;
        } catch (PanError e) {
            throw e;
        } catch (Throwable t) {
            throw netError(t);
        }
    }

    @Override public List<PanItem> list(Session s, String pdirFid) throws PanError {
        try {
            String url = s.shareId.startsWith("http") ? s.shareId : ("https://" + s.shareId);
            Page page = loadPage(url, s.pwd);
            if (!page.isFolder) {
                List<PanItem> one = new ArrayList<PanItem>();
                one.add(new PanItem(page.shareUrl, page.title, page.size, false, page.fileId));
                return one;
            }
            List<PanItem> out = new ArrayList<PanItem>();
            for (int pg = 1; pg <= 200; pg++) {
                Map<String, String> f = PanHttp.headers();
                f.put("lx", page.lx);
                f.put("fid", page.fid);
                f.put("t", page.t);
                f.put("k", page.k);
                f.put("pwd", s.pwd == null ? "" : s.pwd);
                f.put("pg", String.valueOf(pg));
                JSONObject j = formPost(page.base + "/filemoreajax.php", f, page.shareUrl);
                int zt = j.optInt("zt", 0);
                if (zt == 2) break;
                if (zt == 3) throw new PanError("蓝奏提取码错误");
                if (zt != 1) throw new PanError("蓝奏解析失败（zt=" + zt + "），确认链接和提取码");
                JSONArray arr = j.optJSONArray("text");
                if (arr == null || arr.length() == 0) break;
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.optJSONObject(i);
                    if (o == null) continue;
                    String id = o.optString("id");
                    if (id.length() == 0) continue;
                    String name = o.optString("name_all");
                    if (name.length() == 0) name = o.optString("name");
                    out.add(new PanItem(id, name, parseSize(o.optString("size")), false, id));
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
        try {
            Page page = loadPage(s.shareId.startsWith("http") ? s.shareId : ("https://" + s.shareId), s.pwd);
            String html = page.html;
            if (page.iframeUrl != null && page.iframeUrl.length() > 0) {
                if (hostOf(page.iframeUrl).equalsIgnoreCase(hostOf(page.shareUrl))) {
                    html = fetch(page.iframeUrl, page.shareUrl);
                }
            }
            String fileId = page.fileId;
            if (fileId == null || fileId.length() == 0) {
                Matcher m = RE_FILE_ID.matcher(html);
                if (m.find()) fileId = m.group(1);
                else {
                    m = RE_FILE_ID2.matcher(html);
                    if (m.find()) fileId = m.group(1);
                }
            }
            if (fileId == null || fileId.length() == 0) throw new PanError("蓝奏页里找不到文件 id，重新解析一次");

            String sign = jsVar(html, "wp_sign");
            if (sign == null) sign = jsVar(html, "sign");
            if (sign == null) sign = jsVar(html, "isngis");
            if (sign == null) throw new PanError("蓝奏页缺少下载参数（sign/isngis），重试一次");

            boolean withPwd = s.pwd != null && s.pwd.length() > 0;
            Map<String, String> f = PanHttp.headers();
            f.put("action", "downprocess");
            f.put("sign", sign);
            String kd = jsVarRaw(html, "kdns");
            f.put("kd", kd == null || kd.length() == 0 ? "1" : kd);
            if (withPwd) {
                f.put("p", s.pwd);
            } else {

                String key = jsVar(html, "ajaxdata");
                if (key == null) key = jsVar(html, "websignkey");
                if (key != null) {
                    String signs = jsVar(html, "signs");
                    f.put("signs", signs == null ? key : signs);
                    f.put("websignkey", key);
                    String ws = jsVar(html, "websign");
                    f.put("websign", ws == null ? "" : ws);
                    f.put("ves", "1");
                }
            }
            String ajaxUrl = resolveAjaxUrl(html, page.base, fileId);
            JSONObject j = formPost(ajaxUrl, f, page.shareUrl);
            int zt = j.optInt("zt", -1);
            if (zt != 1) {
                String info = j.optString("inf", "");
                throw new PanError(info.length() > 0 ? info : "蓝奏取链失败（zt=" + zt + "）");
            }
            String dom = j.optString("dom");
            String rel = j.optString("url");
            if (dom.length() == 0 || rel.length() == 0) throw new PanError("蓝奏没返回下载地址");
            if (rel.startsWith("/") || rel.contains("://")) throw new PanError("蓝奏返回的地址格式异常，稍后再试");
            String jump = dom.replaceAll("/+$", "") + "/file/" + rel;

            String finalUrl;
            try {
                finalUrl = followToFile(jump, page.shareUrl);
            } catch (PanError pe) {

                throw new BrowserNeeded(jump, page.shareUrl,
                        "下载节点要过人机校验，马上用隐藏浏览器过一下");
            }
            Link l = new Link();
            l.url = finalUrl;
            l.filename = item.name;
            l.size = item.size;
            l.ua = UA;
            l.referer = page.base + "/";
            l.cookie = "down_ip=1";
            return l;
        } catch (PanError e) {
            throw e;
        } catch (Throwable t) {
            throw netError(t);
        }
    }

    @Override public List<PanItem> listMine(String pdirFid) throws PanError {
        if (!canMine()) throw new PanError("蓝奏个人盘要登录（分享解析不需要；点上面「登录」）");
        try {
            String folderId = pdirFid == null || pdirFid.length() == 0 ? "0" : pdirFid;
            List<PanItem> out = new ArrayList<PanItem>();

            JSONObject fj = doupload("47", "folder_id", folderId, null);
            JSONArray fa = fj.optJSONArray("text");
            if (fa != null) {
                for (int i = 0; i < fa.length(); i++) {
                    JSONObject o = fa.optJSONObject(i);
                    if (o == null) continue;
                    String id = o.optString("fol_id");
                    if (id.length() == 0) id = o.optString("id");
                    if (id.length() == 0) continue;
                    String name = o.optString("name");
                    if (name.length() == 0) name = o.optString("name_all");
                    out.add(new PanItem("d:" + id, name, 0, true, "d:" + id));
                }
            }

            for (int pg = 1; pg <= 500; pg++) {
                JSONObject j = doupload("5", "folder_id", folderId, String.valueOf(pg));
                JSONArray arr = j.optJSONArray("text");
                if (arr == null || arr.length() == 0) break;
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.optJSONObject(i);
                    if (o == null) continue;
                    String id = o.optString("id");
                    if (id.length() == 0) continue;
                    String name = o.optString("name_all");
                    if (name.length() == 0) name = o.optString("name");
                    out.add(new PanItem("f:" + id, name, parseSize(o.optString("size")), false, "f:" + id));
                }
            }
            return out;
        } catch (PanError e) {
            throw e;
        } catch (Throwable t) {
            throw netError(t);
        }
    }

    @Override public Link linkMine(PanItem item) throws PanError {
        if (!canMine()) throw new PanError("蓝奏个人盘要登录");
        if (item.dir) throw new PanError("这个是文件夹，先点进去选文件");
        try {
            String id = item.fid.startsWith("f:") ? item.fid.substring(2) : item.fid;
            JSONObject j = doupload("22", "file_id", id, null);
            JSONObject info = j.optJSONObject("info");
            if (info == null) throw new PanError("蓝奏没返回分享信息");
            String base = info.optString("is_newd");
            String sid = info.optString("f_id");
            String pwd = info.optString("pwd");
            if (base.length() == 0 || sid.length() == 0) throw new PanError("蓝奏返回的分享地址不全");
            String url = base.replaceAll("/+$", "") + "/" + sid;
            Session s = open(url, pwd);
            return link(s, new PanItem(url, item.name, item.size, false, sid));
        } catch (PanError e) {
            throw e;
        } catch (Throwable t) {
            throw netError(t);
        }
    }

    private JSONObject doupload(String task, String key, String value, String pg) throws Exception {
        String[] uv = loginParams();
        String url = "https://pc.woozooo.com/doupload.php?uid=" + uv[0] + "&vei=" + URLEncoder.encode(uv[1], "UTF-8");
        Map<String, String> f = PanHttp.headers();
        f.put("task", task);
        f.put(key, value);
        if (pg != null) f.put("pg", pg);
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(15000);
        c.setReadTimeout(20000);
        c.setRequestMethod("POST");
        c.setDoOutput(true);
        c.setRequestProperty("User-Agent", UA);
        c.setRequestProperty("Referer", "https://pc.woozooo.com/mydisk.php");
        c.setRequestProperty("Origin", "https://pc.woozooo.com");
        c.setRequestProperty("X-Requested-With", "XMLHttpRequest");
        c.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        if (cookie != null && cookie.length() > 0) c.setRequestProperty("Cookie", cookie);
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : f.entrySet()) {
            if (sb.length() > 0) sb.append('&');
            sb.append(URLEncoder.encode(e.getKey(), "UTF-8")).append('=')
                    .append(URLEncoder.encode(e.getValue() == null ? "" : e.getValue(), "UTF-8"));
        }
        byte[] body = sb.toString().getBytes("UTF-8");
        c.setFixedLengthStreamingMode(body.length);
        java.io.OutputStream os = c.getOutputStream();
        os.write(body);
        os.flush();
        os.close();
        int code = c.getResponseCode();
        String text = readAll(code >= 400 ? c.getErrorStream() : c.getInputStream());
        c.disconnect();
        if (code == 401 || code == 403) throw new PanError("蓝奏登录已过期，重新登录");
        JSONObject j = new JSONObject(text.trim());
        int zt = j.optInt("zt", -1);
        if (zt == 1 || zt == 2) return j;
        if (zt == 4) throw new PanError("蓝奏请求太频繁，稍等一会儿");
        if (zt == 9) throw new PanError("蓝奏登录已过期，重新登录");
        throw new PanError("蓝奏操作失败（zt=" + zt + "）");
    }

    private String[] loginParams() throws Exception {
        if (puid != null && pvei != null) return new String[]{puid, pvei};
        HttpURLConnection c = (HttpURLConnection) new URL(
                "https://pc.woozooo.com/mydisk.php?item=files&action=index").openConnection();
        c.setConnectTimeout(15000);
        c.setReadTimeout(20000);
        c.setRequestProperty("User-Agent", UA);
        c.setRequestProperty("Referer", "https://pc.woozooo.com/mydisk.php");
        if (cookie != null && cookie.length() > 0) c.setRequestProperty("Cookie", cookie);
        String html = readAll(c.getInputStream());
        c.disconnect();
        Matcher mu = Pattern.compile("uid['\"]?\\s*[=:]\\s*['\"]?(\\d{1,20})").matcher(html);
        Matcher mv = Pattern.compile("vei['\"]?\\s*[=:]\\s*['\"]?([A-Za-z0-9_\\-+/=]{1,256})").matcher(html);
        if (!mu.find() || !mv.find()) throw new PanError("蓝奏登录态无效（拿不到 uid/vei），重新登录");
        puid = mu.group(1);
        pvei = mv.group(1);
        return new String[]{puid, pvei};
    }

    private String puid, pvei;

    @Override public ShareInfo createShare(PanItem item, String passcode, int expiredType) throws PanError {
        if (!canMine()) throw new PanError("蓝奏创建分享要先登录个人盘");
        try {
            String id = item.fid;
            if (id.startsWith("d:") || id.startsWith("f:")) id = id.substring(2);
            JSONObject j = item.dir
                    ? doupload("18", "file_id", id, null)
                    : doupload("22", "file_id", id, null);
            JSONObject info = j.optJSONObject("info");
            if (info == null) throw new PanError("蓝奏没返回分享信息");
            String base = info.optString("is_newd");
            if (base.length() == 0) base = info.optString("new_url");
            String sid = info.optString("f_id");
            if (sid.length() == 0) sid = info.optString("fid");
            if (base.length() == 0 || sid.length() == 0) throw new PanError("蓝奏返回的分享地址不全");
            ShareInfo si = new ShareInfo();
            si.url = base.replaceAll("/+$", "") + "/" + sid;
            si.pwd = info.optString("pwd");
            si.title = item.name;
            return si;
        } catch (PanError e) {
            throw e;
        } catch (Throwable t) {
            throw netError(t);
        }
    }

    private static final class Page {
        String shareUrl = "";
        String base = "";
        String html = "";
        String title = "蓝奏云分享";
        boolean needsPwd = false;
        boolean isFolder = false;
        String iframeUrl = null;
        String fileId = null;
        long size = 0;
        String lx = null, fid = null, t = null, k = null;
    }

    private Page loadPage(String url) throws Exception {
        return loadPage(url, null);
    }

    private Page loadPage(String url, String pwd) throws Exception {
        String normalized = normalize(url);
        String html = fetch(normalized, normalized);

        if (pwd != null && pwd.length() > 0
                && (html.contains("请输入密码") || html.contains("id=\"pwbox\"")
                    || html.contains("input_password"))) {
            submitPwd(normalized, pwd);
            html = fetch(normalized, normalized);
        }

        Page p = new Page();
        p.shareUrl = normalized;
        p.base = schemeHost(normalized);
        p.html = html;
        p.title = extractTitle(html);
        p.needsPwd = html.contains("请输入密码") || html.contains("id=\"pwbox\"")
                || html.contains("input_password") || html.contains("密码不正确");
        p.isFolder = html.contains("filemoreajax") || html.contains("class=\"filemore\"")
                || html.contains("file_more");
        Matcher im = RE_IFRAME.matcher(html);
        if (im.find()) p.iframeUrl = absolutize(p.base, im.group(1));
        Matcher fm = RE_FILE_ID.matcher(html);
        if (fm.find()) p.fileId = fm.group(1);
        if (p.fileId == null) {
            Matcher m2 = Pattern.compile("name=[\"']file[\"']\\s+value=[\"'](\\d+)[\"']").matcher(html);
            if (m2.find()) p.fileId = m2.group(1);
        }
        Matcher sm = RE_SIZE.matcher(html);
        if (sm.find()) p.size = parseSize(sm.group(1) + " " + sm.group(2));
        if (p.isFolder) {
            p.lx = jsVar(html, "lx");
            p.fid = jsVar(html, "fid");
            if (p.fid == null) {
                String path = new URL(normalized).getPath();
                p.fid = path.substring(path.lastIndexOf('/') + 1);
            }
            p.t = jsVar(html, "t");
            p.k = jsVar(html, "k");
            if (p.lx == null || p.t == null || p.k == null) throw new PanError("蓝奏文件夹参数不全，重试一次");
        }
        return p;
    }

    private static final int[] UNSBOX = {
            0xf, 0x23, 0x1d, 0x18, 0x21, 0x10, 0x1, 0x26, 0xa, 0x9,
            0x13, 0x1f, 0x28, 0x1b, 0x16, 0x17, 0x19, 0xd, 0x6, 0xb,
            0x27, 0x12, 0x14, 0x8, 0xe, 0x15, 0x20, 0x1a, 0x2, 0x1e,
            0x7, 0x4, 0x11, 0x5, 0x3, 0x1c, 0x22, 0x25, 0xc, 0x24
    };

    private static final String ACW_KEY = "3000176000856006061501533003690027800375";

    private static String findArg1(String html) {
        Matcher m = Pattern.compile("arg1\\s*=\\s*['\"]([0-9A-Fa-f]{40})['\"]").matcher(html);
        return m.find() ? m.group(1) : null;
    }

    private static String acwScV2(String arg1) {
        try {
            if (arg1 == null || arg1.length() != 40) return null;
            char[] out = new char[40];
            for (int i = 0; i < 40; i++) out[UNSBOX[i]] = arg1.charAt(i);
            StringBuilder sb = new StringBuilder(40);
            for (int i = 0; i < 40; i++) {
                int a = Character.digit(out[i], 16);
                int b = Character.digit(ACW_KEY.charAt(i), 16);
                if (a < 0 || b < 0) return null;
                sb.append(Character.forDigit(a ^ b, 16));
            }
            return sb.toString();
        } catch (Throwable t) {
            return null;
        }
    }

    private void submitPwd(String shareUrl, String pwd) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(shareUrl).openConnection();
        c.setConnectTimeout(15000);
        c.setReadTimeout(20000);
        c.setRequestMethod("POST");
        c.setDoOutput(true);
        c.setInstanceFollowRedirects(false);
        c.setRequestProperty("User-Agent", UA);
        c.setRequestProperty("Referer", shareUrl);
        c.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        String ck = cookieJar.get(hostOf(shareUrl));
        if (ck != null && ck.length() > 0) c.setRequestProperty("Cookie", ck);
        byte[] body = ("pwd=" + URLEncoder.encode(pwd, "UTF-8")).getBytes("UTF-8");
        c.setFixedLengthStreamingMode(body.length);
        java.io.OutputStream os = c.getOutputStream();
        os.write(body);
        os.flush();
        os.close();

        java.util.List<String> sc = c.getHeaderFields().get("Set-Cookie");
        if (sc != null) {
            String host = hostOf(shareUrl);
            StringBuilder sb = new StringBuilder(cookieJar.containsKey(host) ? cookieJar.get(host) + "; " : "");
            for (String one : sc) {
                if (one == null) continue;
                String kv = one.split(";")[0].trim();
                if (kv.length() > 0) sb.append(kv).append("; ");
            }
            cookieJar.put(host, sb.toString().replaceAll(";\\s*$", ""));
        }
        try {
            readAll(c.getInputStream());
        } catch (Throwable ignore) {
        }
        c.disconnect();
    }

    private String fetch(String url, String referer) throws Exception {
        for (int i = 0; i < 6; i++) {
            HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
            c.setConnectTimeout(15000);
            c.setReadTimeout(20000);
            c.setInstanceFollowRedirects(false);
            c.setRequestProperty("User-Agent", UA);
            c.setRequestProperty("Referer", referer);
            c.setRequestProperty("Accept-Language", "zh-CN,zh;q=0.9");
            String ck = cookieJar.get(hostOf(url));
            if (ck != null && ck.length() > 0) c.setRequestProperty("Cookie", ck);
            int code = c.getResponseCode();
            if (code >= 300 && code < 400) {
                String loc = c.getHeaderField("Location");
                c.disconnect();
                if (loc == null) throw new PanError("蓝奏页面跳转异常");
                url = absolutize(url, loc);
                if (!isLanzouHost(hostOf(url))) throw new PanError("蓝奏分享链接无效");
                continue;
            }
            String body = readAll(code >= 400 ? c.getErrorStream() : c.getInputStream());
            c.disconnect();

            if (body.contains("acw_sc__v2") && body.contains("arg1")) {
                String arg1 = findArg1(body);
                if (arg1 != null && i < 4) {
                    String val = acwScV2(arg1);
                    if (val != null) {
                        String host = hostOf(url);
                        String prev = cookieJar.containsKey(host) ? cookieJar.get(host) + "; " : "";
                        cookieJar.put(host, prev + "acw_sc__v2=" + val);
                        try { Thread.sleep(200L); } catch (Throwable ignore) { }
                        continue;
                    }
                }
                throw new PanError("蓝奏人机校验没通过，稍后再点一次");
            }
            return body;
        }
        throw new PanError("蓝奏页面跳转次数过多，重新解析");
    }

    private JSONObject formPost(String url, Map<String, String> fields, String referer) throws Exception {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : fields.entrySet()) {
            if (sb.length() > 0) sb.append('&');
            sb.append(URLEncoder.encode(e.getKey(), "UTF-8")).append('=')
                    .append(URLEncoder.encode(e.getValue() == null ? "" : e.getValue(), "UTF-8"));
        }
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(15000);
        c.setReadTimeout(20000);
        c.setRequestMethod("POST");
        c.setDoOutput(true);
        c.setRequestProperty("User-Agent", UA);
        c.setRequestProperty("Referer", referer);
        c.setRequestProperty("Origin", schemeHost(referer));
        c.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        String ck = cookieJar.get(hostOf(url));
        if (ck != null && ck.length() > 0) c.setRequestProperty("Cookie", ck);
        byte[] body = sb.toString().getBytes("UTF-8");
        c.setFixedLengthStreamingMode(body.length);
        java.io.OutputStream os = c.getOutputStream();
        os.write(body);
        os.flush();
        os.close();
        String text = readAll(c.getInputStream());
        c.disconnect();
        String t = text.trim();
        if (t.startsWith("<")) throw new PanError("蓝奏返回了验证页面，稍等再试");
        return new JSONObject(t);
    }

    private String followToFile(String jump, String referer) throws Exception {
        String url = jump;
        for (int i = 0; i < 10; i++) {
            String host = hostOf(url);
            HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
            c.setConnectTimeout(15000);
            c.setReadTimeout(20000);
            c.setInstanceFollowRedirects(false);
            c.setRequestProperty("User-Agent", UA);
            c.setRequestProperty("Referer", referer);
            c.setRequestProperty("Accept-Encoding", "identity");
            StringBuilder ck = new StringBuilder("down_ip=1");
            String jar = cookieJar.get(host);
            if (jar != null && jar.length() > 0) ck.append("; ").append(jar);
            c.setRequestProperty("Cookie", ck.toString());
            int code = c.getResponseCode();
            String loc = c.getHeaderField("Location");
            String ct = c.getContentType();

            java.util.List<String> sc = c.getHeaderFields().get("Set-Cookie");
            if (sc != null) {
                StringBuilder sb = new StringBuilder(jar == null ? "" : jar + "; ");
                for (String one : sc) {
                    if (one == null) continue;
                    String kv = one.split(";")[0].trim();
                    if (kv.length() > 0) sb.append(kv).append("; ");
                }
                cookieJar.put(host, sb.toString().replaceAll(";\\s*$", ""));
            }
            boolean html = ct != null && ct.toLowerCase().contains("text/html");
            String body = html ? readAll(code >= 400 ? c.getErrorStream() : c.getInputStream()) : null;
            c.disconnect();

            if (code >= 300 && code < 400 && loc != null) {
                url = absolutize(url, loc);
                continue;
            }
            if (html && body != null && body.contains("acw_sc__v2") && body.contains("arg1")) {
                String arg1 = findArg1(body);
                String val = arg1 == null ? null : acwScV2(arg1);
                if (val == null) throw new PanError("蓝奏下载节点校验失败，稍后再试");
                String prev = cookieJar.containsKey(host) ? cookieJar.get(host) + "; " : "";
                cookieJar.put(host, prev + "acw_sc__v2=" + val);
                continue;
            }
            if (!html) return url;
            throw new PanError("蓝奏下载地址暂时不可用（可能触发风控），稍后再试");
        }
        return url;
    }

    private String resolveAjaxUrl(String html, String base, String fileId) {
        String d1 = jsVar(html, "domain1");
        if (d1 != null && d1.contains("/ajax")) return d1;
        String d2 = jsVar(html, "domain2");
        if (d2 != null && d2.contains("/ajax")) return d2;
        Matcher m = Pattern.compile("https?://[^'\"\\s]+/(?:ajaxm|ajaxfile)\\.php\\?file=\\d+").matcher(html);
        if (m.find()) return m.group();
        return base + "/ajaxm.php?file=" + fileId;
    }

    private static String jsVar(String html, String name) {
        Matcher m = Pattern.compile("(?:(?:var|let|const)\\s+)?" + Pattern.quote(name)
                + "\\s*=\\s*['\"]([^'\"]*)['\"]").matcher(html);
        String best = null;
        while (m.find()) {
            String v = m.group(1);
            if (v == null || v.length() == 0) continue;
            if (best == null || v.length() > best.length()) best = v;
        }
        return best;
    }

    private static String jsVarRaw(String html, String name) {
        Matcher m = Pattern.compile("(?:var|let|const)\\s+" + Pattern.quote(name)
                + "\\s*=\\s*([^;,\\n\\r]+)").matcher(html);
        if (!m.find()) return null;
        return m.group(1).trim().replace("'", "").replace("\"", "");
    }

    private static String extractTitle(String html) {
        Matcher m = RE_TITLE.matcher(html);
        if (m.find()) {
            String t = m.group(1).replace(" - 蓝奏云", "").replace("蓝奏云", "").trim();
            if (t.length() > 0 && !t.contains("密码")) return t;
        }
        Matcher m2 = Pattern.compile("class=[\"']n[\"']>([^<]+)<").matcher(html);
        if (m2.find()) return m2.group(1).trim();
        Matcher m3 = Pattern.compile("var\\s+filename\\s*=\\s*['\"]([^'\"]+)['\"]").matcher(html);
        if (m3.find()) return m3.group(1);
        return "蓝奏云分享";
    }

    private static long parseSize(String text) {
        if (text == null) return 0;
        Matcher m = Pattern.compile("([\\d.]+)\\s*([KMGT]?)", Pattern.CASE_INSENSITIVE).matcher(text.trim());
        if (!m.find()) return 0;
        double v;
        try {
            v = Double.parseDouble(m.group(1));
        } catch (Throwable t) {
            return 0;
        }
        String u = m.group(2).toUpperCase();
        long f = 1;
        if ("K".equals(u)) f = 1024L;
        else if ("M".equals(u)) f = 1024L * 1024;
        else if ("G".equals(u)) f = 1024L * 1024 * 1024;
        else if ("T".equals(u)) f = 1024L * 1024 * 1024 * 1024L;
        return (long) (v * f);
    }

    private static String normalize(String url) {
        String u = url.trim().replaceFirst("(?i)^http://", "https://");
        int hash = u.indexOf('#');
        if (hash >= 0) u = u.substring(0, hash);
        while (u.endsWith("/")) u = u.substring(0, u.length() - 1);
        return u;
    }

    private static String schemeHost(String url) {
        try {
            URL u = new URL(url);
            return u.getProtocol() + "://" + u.getHost();
        } catch (Throwable t) {
            return url;
        }
    }

    private static String hostOf(String url) {
        try {
            return new URL(url).getHost();
        } catch (Throwable t) {
            return "";
        }
    }

    private static String absolutize(String base, String target) {
        if (target.startsWith("http://") || target.startsWith("https://")) return target;
        String root = schemeHost(base);
        return target.startsWith("/") ? root + target : root + "/" + target;
    }

    private static boolean isLanzouHost(String host) {
        if (host == null) return false;
        return host.toLowerCase().matches("^(?:[a-z0-9-]+\\.)*(?:lanzou[a-z]?|lan[zs]o[ux])\\.(?:com|net|org|cn)$");
    }

    private static String readAll(InputStream in) {
        if (in == null) return "";
        try {
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) bo.write(buf, 0, n);
            in.close();
            return new String(bo.toByteArray(), "UTF-8");
        } catch (Throwable t) {
            return "";
        }
    }
}
