package com.cathelperawa.owo.app.pan;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class QuarkPan extends PanProvider {

    private static final String UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) "
                    + "quark-cloud-drive/2.5.20 Chrome/100.0.4896.160 Electron/18.3.5.12-a038f7b798 "
                    + "Safari/537.36 Channel/pckk_other_ch";

    private static final String BASE = "https://drive-pc.quark.cn";
    private static final String TOKEN_URL = BASE + "/1/clouddrive/share/sharepage/token?pr=ucpro&fr=pc";
    private static final String DETAIL_URL = BASE + "/1/clouddrive/share/sharepage/detail?pr=ucpro&fr=pc";
    private static final String DOWNLOAD_URL = BASE + "/1/clouddrive/file/download?pr=ucpro&fr=pc&sys=win32&ve=3.23.2";
    private static final String REFERER = "https://pan.quark.cn/";

    private String guestCookie = "";

    @Override public String id() { return "quark"; }

    @Override public String name() { return "夸克网盘"; }

    @Override public String referer() { return REFERER; }

    public boolean loggedIn() {
        return PanHttp.hasCookie(cookie, "__puus") || PanHttp.hasCookie(cookie, "__pus");
    }

    private Map<String, String> baseHeaders(String ck) {
        Map<String, String> h = PanHttp.headers(
                "User-Agent", UA,
                "Origin", "https://pan.quark.cn",
                "Referer", REFERER);
        if (ck != null && ck.length() > 0) h.put("Cookie", ck);
        return h;
    }

    @Override public Session open(String shareId, String pwd) throws PanError {
        try {
            JSONObject body = new JSONObject()
                    .put("pwd_id", shareId)
                    .put("passcode", pwd == null ? "" : pwd)
                    .put("support_visit_limit_private_share", true);
            PanHttp.Resp r = PanHttp.post(TOKEN_URL, body.toString(), baseHeaders(cookie));
            absorb(r.setCookies);
            JSONObject j = json(r.body);
            JSONObject d = dataOf(j, "分享打不开（可能已失效或需要提取码）");
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
            String url = DETAIL_URL
                    + "&pwd_id=" + URLEncoder.encode(s.shareId, "UTF-8")
                    + "&stoken=" + URLEncoder.encode(s.stoken, "UTF-8")
                    + "&pdir_fid=" + URLEncoder.encode(pdirFid == null || pdirFid.length() == 0 ? "0" : pdirFid, "UTF-8")
                    + "&ver=2&force=0&_page=1&_size=100&_fetch_banner=0&_fetch_share=0"
                    + "&fetch_relate_conversation=0&_fetch_total=1&_sort=file_type:asc,file_name:asc";
            PanHttp.Resp r = PanHttp.get(url, baseHeaders(cookie));
            absorb(r.setCookies);
            JSONObject d = dataOf(json(r.body), "读取分享列表失败");
            JSONArray arr = d.optJSONArray("list");
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
                    .put("fids_token", new JSONArray().put(item.token))
                    .put("pwd_id", s.shareId)
                    .put("stoken", s.stoken)
                    .put("speedup_session", "")
                    .put("token", "");
            String ck = acct ? cookie : guestCookie;
            PanHttp.Resp r = PanHttp.post(DOWNLOAD_URL, body.toString(), baseHeaders(ck));

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
            if (arr == null || arr.length() == 0) throw new PanError("服务端没给直链（文件可能被和谐/限速）");
            JSONObject o = arr.optJSONObject(0);
            if (o == null) throw new PanError("服务端没给直链");
            Link l = new Link();
            l.url = o.optString("download_url");
            l.filename = o.optString("file_name", item.name);
            if (l.filename.length() == 0) l.filename = item.name;
            l.size = o.optLong("size", item.size);
            l.ua = UA;
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

    private static final String FILE_URL = BASE + "/1/clouddrive/file?pr=ucpro&fr=pc";

    @Override public List<PanItem> listMine(String pdirFid) throws PanError {
        if (!loggedIn()) throw new PanError("先登录夸克网盘，才能看自己的网盘目录");
        try {
            String fid = pdirFid == null || pdirFid.length() == 0 ? "0" : pdirFid;
            String url = FILE_URL + "&pdir_fid=" + URLEncoder.encode(fid, "UTF-8")
                    + "&page=1&size=100";
            PanHttp.Resp r = PanHttp.get(url, baseHeaders(cookie));
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

    private static final String FILE_DELETE_URL = BASE + "/1/clouddrive/file/delete?pr=ucpro&fr=pc";
    private static final String FILE_RENAME_URL = BASE + "/1/clouddrive/file/rename?pr=ucpro&fr=pc";

    @Override public boolean canManage() {
        return true;
    }

    @Override public void renameMine(PanItem item, String newName) throws PanError {
        if (!loggedIn()) throw new PanError("先登录夸克网盘");
        String nn = newName == null ? "" : newName.trim();
        if (nn.length() == 0) throw new PanError("名字不能为空");
        try {
            JSONObject body = new JSONObject()
                    .put("fid", item.fid)
                    .put("file_name", nn)
                    .put("to_pdir_fid", "");
            PanHttp.Resp r = PanHttp.post(FILE_RENAME_URL, body.toString(), baseHeaders(cookie));
            absorb(r.setCookies);
            dbg("quark.rename", "POST " + FILE_RENAME_URL + "\nbody=" + body
                    + "\nHTTP " + r.code + " => " + cut(r.body));
            dataOf(json(r.body), "改名失败");
        } catch (PanError e) {
            throw e;
        } catch (Throwable t) {
            throw netError(t);
        }
    }

    @Override public void deleteMine(PanItem item, String parentFid) throws PanError {
        if (!loggedIn()) throw new PanError("先登录夸克网盘");
        try {

            String pdir = parentFid == null || parentFid.length() == 0 ? "0" : parentFid;
            JSONObject body = new JSONObject()
                    .put("fids", new JSONArray().put(item.fid))
                    .put("action_type", 2)
                    .put("exclude_fids", new JSONArray())
                    .put("current_dir_fid", pdir);
            PanHttp.Resp r = PanHttp.post(FILE_DELETE_URL, body.toString(), baseHeaders(cookie));
            absorb(r.setCookies);
            dbg("quark.delete", "POST " + FILE_DELETE_URL + "\nbody=" + body
                    + "\nHTTP " + r.code + " => " + cut(r.body));
            dataOf(json(r.body), "删除失败");
        } catch (PanError e) {
            throw e;
        } catch (Throwable t) {
            throw netError(t);
        }
    }

    @Override public Link linkMine(PanItem item) throws PanError {
        if (!loggedIn()) throw new PanError("先登录夸克网盘");
        try {
            JSONObject body = new JSONObject().put("fids", new JSONArray().put(item.fid));
            PanHttp.Resp r = PanHttp.post(DOWNLOAD_URL, body.toString(), baseHeaders(cookie));
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
            l.ua = UA;
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

    private static String cut(String s) {
        if (s == null) return "";
        return s.length() > 600 ? s.substring(0, 600) + "…" : s;
    }

    private static final String SHARE_CREATE_URL = BASE + "/1/clouddrive/share?pr=ucpro&fr=pc&uc_param_str=";
    private static final String TASK_URL = BASE + "/1/clouddrive/task?pr=ucpro&fr=pc";
    private static final String SHARE_INFO_URL = BASE + "/1/clouddrive/share/password?pr=ucpro&fr=pc&uc_param_str=";

    @Override public ShareInfo createShare(PanItem item, String passcode, int expiredType) throws PanError {
        if (!loggedIn()) throw new PanError("先登录夸克网盘");

        if (expiredType < 1 || expiredType > 4) expiredType = 1;
        try {
            JSONObject body = new JSONObject()
                    .put("fid_list", new JSONArray().put(item.fid))
                    .put("title", item.name)
                    .put("url_type", 1)
                    .put("expired_type", expiredType)
                    .put("support_error_code", new JSONArray().put("41060"));
            if (passcode != null && passcode.length() > 0) body.put("passcode", passcode);
            PanHttp.Resp r = PanHttp.post(SHARE_CREATE_URL, body.toString(), baseHeaders(cookie));
            absorb(r.setCookies);
            dbg("quark.createShare", "POST " + SHARE_CREATE_URL + "\nbody=" + body
                    + "\nHTTP " + r.code + " => " + cut(r.body));
            JSONObject d = dataOf(json(r.body), "创建分享失败（可能被风控限额）");
            String taskId = d.optString("task_id");
            if (taskId.length() == 0) throw new PanError("服务端没给任务号");

            String shareId = "";
            for (int i = 0; i < 15 && shareId.length() == 0; i++) {
                String u = TASK_URL + "&task_id=" + URLEncoder.encode(taskId, "UTF-8") + "&retry_index=0";
                PanHttp.Resp tr = PanHttp.get(u, baseHeaders(cookie));
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
            if (shareId.length() == 0) throw new PanError("创建分享超时（网盘侧还没处理完），稍后再试");

            JSONObject ib = new JSONObject().put("share_id", shareId);
            PanHttp.Resp ir = PanHttp.post(SHARE_INFO_URL, ib.toString(), baseHeaders(cookie));
            dbg("quark.shareInfo", "POST " + SHARE_INFO_URL + "\nbody=" + ib
                    + "\nHTTP " + ir.code + " => " + cut(ir.body));
            JSONObject idata = dataOf(json(ir.body), "查询分享信息失败");
            ShareInfo si = new ShareInfo();
            si.url = idata.optString("share_url");
            si.pwd = idata.optString("passcode");
            si.title = idata.optString("title", item.name);
            if (si.url.length() == 0) {
                String pwdId = idata.optString("pwd_id");
                if (pwdId.length() > 0) si.url = "https://pan.quark.cn/s/" + pwdId;
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
            case 23018:
                return acct ? "服务端按游客限制拒绝了该文件（夸克约 50MB）"
                        : "这个文件超过游客可下的大小（夸克约 50MB），先登录夸克网盘再下";
            case 31001:
                return "夸克登录态已失效，去「网页登录」重新登录一次再试";
            case 41020:
                return "文件令牌过期了，返回上一级重新进一次分享";
            default:
                return msg.length() > 0 ? msg : "获取直链失败";
        }
    }
}
