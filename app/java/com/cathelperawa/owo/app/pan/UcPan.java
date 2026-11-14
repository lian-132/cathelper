package com.cathelperawa.owo.app.pan;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class UcPan extends PanProvider {

    private static final String UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) "
                    + "Chrome/120.0.0.0 Safari/537.36";

    private static final String GUEST_UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) "
                    + "uc-cloud-drive/2.5.20 Chrome/100.0.4896.160 Electron/18.3.5.4-b478491100 "
                    + "Safari/537.36 Channel/pckk_other_ch";

    private static final String GUEST_SEC_CH_UA =
            "\"Not=A?Brand\";v=\"99\", \"Chromium\";v=\"100\", \"Google Chrome\";v=\"100\"";

    private static final String BASE = "https://pc-api.uc.cn";
    private static final String TOKEN_URL = BASE + "/1/clouddrive/share/sharepage/token?pr=UCBrowser&fr=pc";
    private static final String DETAIL_URL = BASE + "/1/clouddrive/share/sharepage/v2/detail?pr=UCBrowser&fr=pc";
    private static final String DOWNLOAD_URL = BASE + "/1/clouddrive/file/download?entry=ft&fr=pc&pr=UCBrowser";
    private static final String REFERER = "https://drive.uc.cn/";

    private String guestCookie = "";

    @Override public String id() { return "uc"; }

    @Override public String name() { return "UC 网盘"; }

    @Override public String referer() { return REFERER; }

    public boolean loggedIn() {
        return PanHttp.hasCookie(cookie, "__puus") || PanHttp.hasCookie(cookie, "__pus");
    }

    private Map<String, String> headers(String ck, boolean guest) {
        Map<String, String> h = PanHttp.headers(
                "User-Agent", guest ? GUEST_UA : UA,
                "Origin", "https://drive.uc.cn",
                "Referer", REFERER);
        if (guest) h.put("Sec-Ch-Ua", GUEST_SEC_CH_UA);
        if (ck != null && ck.length() > 0) h.put("Cookie", ck);
        return h;
    }

    @Override public Session open(String shareId, String pwd) throws PanError {
        try {
            JSONObject body = new JSONObject()
                    .put("pwd_id", shareId)
                    .put("passcode", pwd == null ? "" : pwd)
                    .put("share_for_transfer", true);
            PanHttp.Resp r = PanHttp.post(TOKEN_URL, body.toString(), headers(cookie, false));
            absorb(r.setCookies);
            JSONObject d = dataOf(json(r.body), "分享打不开（可能已失效或需要提取码）");
            Session s = new Session();
            s.platform = id();
            s.shareId = shareId;
            s.pwd = pwd == null ? "" : pwd;
            s.stoken = d.optString("stoken");
            s.title = d.optString("title");
            if (s.stoken.length() == 0) throw new PanError("服务端没给 stoken，分享可能已失效");
            return s;
        } catch (PanError e) {
            throw e;
        } catch (Throwable t) {
            throw netError(t);
        }
    }

    @Override public List<PanItem> list(Session s, String pdirFid) throws PanError {
        try {
            JSONObject body = new JSONObject()
                    .put("pwd_id", s.shareId)
                    .put("passcode", s.pwd == null ? "" : s.pwd)
                    .put("force", 0)
                    .put("page", 1)
                    .put("size", 100)
                    .put("fetch_banner", 1)
                    .put("fetch_share", 1)
                    .put("fetch_total", 1)
                    .put("sort", "file_type:asc,file_name:asc")
                    .put("banner_platform", "other")
                    .put("web_platform", "windows")
                    .put("fetch_error_background", 1);
            if (pdirFid != null && pdirFid.length() > 0 && !"0".equals(pdirFid)) {
                body.put("pdir_fid", pdirFid);
            }
            PanHttp.Resp r = PanHttp.post(DETAIL_URL + "&ve=2.5.20", body.toString(), headers(cookie, false));
            absorb(r.setCookies);
            JSONObject d = dataOf(json(r.body), "读取分享列表失败");
            JSONObject info = d.optJSONObject("detail_info");
            JSONArray arr = info == null ? null : info.optJSONArray("list");
            if (arr == null) arr = d.optJSONArray("list");
            List<PanItem> out = new ArrayList<PanItem>();
            if (arr == null) return out;
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o == null) continue;
                out.add(new PanItem(
                        o.optString("fid"),
                        o.optString("file_name"),
                        o.optLong("size"),
                        o.optBoolean("dir", false),
                        o.optString("share_fid_token")));
            }
            return out;
        } catch (PanError e) {
            throw e;
        } catch (Throwable t) {
            throw netError(t);
        }
    }

    @Override public Link link(Session s, PanItem item) throws PanError {
        boolean acct = loggedIn();
        try {
            JSONObject body = new JSONObject()
                    .put("fids", new JSONArray().put(item.fid))
                    .put("pwd_id", s.shareId)
                    .put("stoken", s.stoken)
                    .put("fids_token", new JSONArray().put(item.token));
            String ck = acct ? cookie : guestCookie;
            PanHttp.Resp r = PanHttp.post(DOWNLOAD_URL, body.toString(), headers(ck, !acct));

            String pugs = PanHttp.pickSetCookie(r.setCookies, "__pugs");
            if (pugs.length() > 0) guestCookie = pugs;
            if (acct) absorb(r.setCookies);

            JSONObject j = json(r.body);
            int status = j.optInt("status", -1);
            int code = j.optInt("code", 0);
            if (status != 200 && code != 0) {
                throw new PanError(explain(code, j.optString("message", ""), acct));
            }
            JSONArray arr = j.optJSONArray("data");
            if (arr == null || arr.length() == 0) throw new PanError("服务端没给直链（文件可能被限）");
            JSONObject o = arr.optJSONObject(0);
            if (o == null) throw new PanError("服务端没给直链");
            Link l = new Link();
            l.url = o.optString("download_url");
            l.filename = o.optString("file_name", item.name);
            if (l.filename.length() == 0) l.filename = item.name;
            l.size = o.optLong("size", item.size);
            l.ua = acct ? UA : GUEST_UA;
            l.referer = REFERER;
            l.guest = !acct;
            l.cookie = acct ? cookie : (pugs.length() > 0 ? pugs : guestCookie);
            if (l.url.length() == 0) throw new PanError("直链为空，换个文件试试");
            return l;
        } catch (PanError e) {
            throw e;
        } catch (Throwable t) {
            throw netError(t);
        }
    }

    private static final String CLOUD_UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) "
                    + "uc-cloud-drive/1.6.1 Chrome/100.0.4896.160 Electron/18.3.5.16-b62cf9c50d "
                    + "Safari/537.36 Channel/ucpan_other_ch";

    private static final String CLOUD_SORT_URL = BASE + "/1/clouddrive/file/sort?pr=UCBrowser&fr=pc";
    private static final String CLOUD_DOWNLOAD_URL = BASE + "/1/clouddrive/file/download";

    private Map<String, String> cloudHeaders() {
        Map<String, String> h = PanHttp.headers(
                "User-Agent", CLOUD_UA,
                "Origin", "https://drive.uc.cn",
                "Referer", REFERER);
        if (cookie != null && cookie.length() > 0) h.put("Cookie", cookie);
        return h;
    }

    @Override public List<PanItem> listMine(String pdirFid) throws PanError {
        if (!loggedIn()) throw new PanError("先登录 UC 网盘，才能看自己的网盘目录");
        try {
            String fid = pdirFid == null || pdirFid.length() == 0 ? "0" : pdirFid;
            String url = CLOUD_SORT_URL + "&pdir_fid=" + fid + "&_page=1&_size=100"
                    + "&_fetch_total=1&_fetch_sub_dirs=0&_sort=file_type%3Aasc%2Cupdated_at%3Adesc";
            PanHttp.Resp r = PanHttp.get(url, cloudHeaders());
            absorb(r.setCookies);
            JSONObject d = dataOf(json(r.body), "读取网盘目录失败");
            JSONArray arr = d.optJSONArray("list");
            List<PanItem> out = new ArrayList<PanItem>();
            if (arr == null) return out;
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o == null) continue;
                String name = o.optString("file_name");
                if (name.length() == 0) name = o.optString("fname");
                boolean dir = o.optBoolean("dir", false) || o.optInt("isdir") == 1;
                long size = o.has("size") ? o.optLong("size") : o.optLong("fsize");
                out.add(new PanItem(o.optString("fid"), name, size, dir, o.optString("fid_token")));
            }
            return out;
        } catch (PanError e) {
            throw e;
        } catch (Throwable t) {
            throw netError(t);
        }
    }

    private static final String FILE_DELETE_URL = BASE + "/1/clouddrive/file/delete?pr=UCBrowser&fr=pc";
    private static final String FILE_RENAME_URL = BASE + "/1/clouddrive/file/rename?pr=UCBrowser&fr=pc";

    @Override public boolean canManage() {
        return true;
    }

    @Override public void renameMine(PanItem item, String newName) throws PanError {
        if (!loggedIn()) throw new PanError("先登录 UC 网盘");
        String nn = newName == null ? "" : newName.trim();
        if (nn.length() == 0) throw new PanError("名字不能为空");
        try {
            JSONObject body = new JSONObject()
                    .put("fid", item.fid)
                    .put("file_name", nn)
                    .put("to_pdir_fid", "");
            PanHttp.Resp r = PanHttp.post(FILE_RENAME_URL, body.toString(), cloudHeaders());
            absorb(r.setCookies);
            dbg("uc.rename", "POST " + FILE_RENAME_URL + "\nbody=" + body
                    + "\nHTTP " + r.code + " => " + PanProvider.cutStr(r.body));
            dataOf(json(r.body), "改名失败");
        } catch (PanError e) {
            throw e;
        } catch (Throwable t) {
            throw netError(t);
        }
    }

    @Override public void deleteMine(PanItem item, String parentFid) throws PanError {
        if (!loggedIn()) throw new PanError("先登录 UC 网盘");
        try {
            String pdir = parentFid == null || parentFid.length() == 0 ? "0" : parentFid;
            JSONObject body = new JSONObject()
                    .put("fids", new JSONArray().put(item.fid))
                    .put("action_type", 2)
                    .put("exclude_fids", new JSONArray())
                    .put("current_dir_fid", pdir);
            PanHttp.Resp r = PanHttp.post(FILE_DELETE_URL, body.toString(), cloudHeaders());
            absorb(r.setCookies);
            dbg("uc.delete", "POST " + FILE_DELETE_URL + "\nbody=" + body
                    + "\nHTTP " + r.code + " => " + PanProvider.cutStr(r.body));
            dataOf(json(r.body), "删除失败");
        } catch (PanError e) {
            throw e;
        } catch (Throwable t) {
            throw netError(t);
        }
    }

    @Override public Link linkMine(PanItem item) throws PanError {
        if (!loggedIn()) throw new PanError("先登录 UC 网盘");

        try {
            JSONObject body = new JSONObject().put("fids", new JSONArray().put(item.fid));
            String url = CLOUD_DOWNLOAD_URL + "?pr=UCBrowser&fr=pc&sys=win32&ve=1.6.1";
            PanHttp.Resp r = PanHttp.post(url, body.toString(), cloudHeaders());
            absorb(r.setCookies);
            JSONObject j = json(r.body);
            int status = j.optInt("status", -1);
            int code = j.optInt("code", 0);
            if (status != 200 && code != 0) throw new PanError(explain(code, j.optString("message", ""), true));
            JSONArray arr = j.optJSONArray("data");
            if (arr == null || arr.length() == 0) throw new PanError("服务端没给直链");
            JSONObject o = arr.optJSONObject(0);
            if (o == null) throw new PanError("服务端没给直链");
            Link l = new Link();
            l.url = o.optString("download_url");
            l.filename = o.optString("file_name", item.name);
            if (l.filename.length() == 0) l.filename = item.name;
            l.size = o.optLong("size", item.size);
            l.ua = CLOUD_UA;
            l.referer = REFERER;
            l.cookie = cookie;
            l.guest = false;
            if (l.url.length() == 0) throw new PanError("直链为空，换个文件试试");
            return l;
        } catch (PanError e) {
            throw e;
        } catch (Throwable t) {
            throw netError(t);
        }
    }

    private static final String SHARE_CREATE_URL = BASE + "/1/clouddrive/share?pr=UCBrowser&fr=pc";
    private static final String TASK_URL = BASE + "/1/clouddrive/task?pr=UCBrowser&fr=pc";
    private static final String SHARE_INFO_URL = BASE + "/1/clouddrive/share/password?pr=UCBrowser&fr=pc";

    @Override public ShareInfo createShare(PanItem item, String passcode, int expiredType) throws PanError {
        if (!loggedIn()) throw new PanError("先登录 UC 网盘");

        if (expiredType < 1 || expiredType > 4) expiredType = 1;
        try {
            JSONObject body = new JSONObject()
                    .put("fid_list", new JSONArray().put(item.fid))
                    .put("title", item.name)
                    .put("url_type", 1)
                    .put("expired_type", expiredType)
                    .put("public_search", passcode != null && passcode.length() > 0 ? 0 : 1);
            if (passcode != null && passcode.length() > 0) body.put("passcode", passcode);
            PanHttp.Resp r = PanHttp.post(SHARE_CREATE_URL, body.toString(), headers(cookie, false));
            absorb(r.setCookies);
            dbg("uc.createShare", "POST " + SHARE_CREATE_URL + "\nbody=" + body
                    + "\nHTTP " + r.code + " => " + (r.body == null ? "" : (r.body.length() > 600 ? r.body.substring(0, 600) : r.body)));
            JSONObject d = dataOf(json(r.body), "创建分享失败（可能被风控限额）");
            String taskId = d.optString("task_id");
            if (taskId.length() == 0) throw new PanError("服务端没给任务号");

            String shareId = "";
            for (int i = 0; i < 15 && shareId.length() == 0; i++) {
                String u = TASK_URL + "&task_id=" + java.net.URLEncoder.encode(taskId, "UTF-8") + "&retry_index=0";
                PanHttp.Resp tr = PanHttp.get(u, headers(cookie, false));
                JSONObject tj = json(tr.body);
                if (tj.optInt("status") == 200) {
                    JSONObject td = tj.optJSONObject("data");
                    if (td != null) {
                        boolean finished = td.optLong("finished_at") > 0 || td.optInt("status") == 2;
                        if (finished) shareId = td.optString("share_id");
                    }
                }
                if (shareId.length() == 0) {
                    try { Thread.sleep(1000L); } catch (Throwable ignore) { }
                }
            }
            if (shareId.length() == 0) throw new PanError("创建分享超时，稍后再试");

            JSONObject ib = new JSONObject().put("share_id", shareId);
            PanHttp.Resp ir = PanHttp.post(SHARE_INFO_URL, ib.toString(), headers(cookie, false));
            JSONObject idata = dataOf(json(ir.body), "查询分享信息失败");
            ShareInfo si = new ShareInfo();
            si.url = idata.optString("share_url");
            si.pwd = idata.optString("passcode");
            si.title = idata.optString("title", item.name);
            if (si.url.length() == 0) {
                String pwdId = idata.optString("pwd_id");
                if (pwdId.length() > 0) si.url = "https://drive.uc.cn/s/" + pwdId;
            }
            if (si.url.length() == 0) throw new PanError("没拿到分享链接");
            return si;
        } catch (PanError e) {
            throw e;
        } catch (Throwable t) {
            throw netError(t);
        }
    }

    private String explain(int code, String msg, boolean acct) {
        switch (code) {
            case 31001:
                return "该分享需要登录 UC 网盘后才能下载，去「网页登录」登录一次";
            case 23018:
                return acct ? "服务端按游客限制拒绝了该文件"
                        : "文件超过游客可下的大小，先登录 UC 网盘再下";
            case 14001:
                return "分享已失效或提取码不对，重新解析一次";
            case 41020:
                return "文件令牌过期了，返回上一级重新进一次分享";
            default:
                return msg.length() > 0 ? msg : "获取直链失败";
        }
    }
}
