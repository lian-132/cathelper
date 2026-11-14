package com.cathelperawa.owo.app.script;

import com.cathelperawa.owo.app.api.AiApi;
import com.cathelperawa.owo.app.api.MsgApi;
import com.cathelperawa.owo.app.bridge.QQEnv;
import com.cathelperawa.owo.app.core.HookLog;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

public final class ScriptApiImpl implements ScriptApi {

    private final String script;
    private final Set<String> caps;
    private final File dataDir;

    private final int scope;

    public ScriptApiImpl(String script, Set<String> caps, File dataDir) {
        this(script, caps, dataDir, ScriptScope.ALL);
    }

    public ScriptApiImpl(String script, Set<String> caps, File dataDir, int scope) {
        this.script = script == null ? "script" : script;
        this.caps = caps == null ? new HashSet<String>() : caps;
        this.dataDir = dataDir;
        this.scope = scope;
    }

    @Override public void log(String s) {
        HookLog.i("[" + script + "] " + s);
    }

    @Override public String me() {
        try { return QQEnv.currentUin(); } catch (Throwable t) { return ""; }
    }

    @Override public String myName() {
        try { return QQEnv.currentNickname(); } catch (Throwable t) { return ""; }
    }

    @Override public long now() { return System.currentTimeMillis(); }

    @Override public String scriptName() { return script; }

    @Override public boolean hasCap(String cap) {
        return cap != null && caps.contains(cap);
    }

    @Override public String ai(String prompt) {
        try { return AiApi.ask(prompt); } catch (Throwable t) { return ""; }
    }

    @Override public void route(String id, String[] match, ScriptHandler h) {
        if (id == null || match == null || match.length == 0 || h == null) return;
        RouteRegistry.add(script, id, match, h, scope);
        String sc = ScriptScope.label(scope);
        log("注册命令「" + match[0] + "」" + (sc.length() > 0 ? "（" + sc + "）" : ""));
    }

    @Override public void tool(String name, String desc, ScriptToolHandler h) {
        String err = ScriptToolRegistry.add(script, name, desc, h);
        if (err != null) log("注册 AI 工具失败：" + err);
        else log("注册 AI 工具「" + name + "」（AI 现在能调它了）");
    }

    @Override public void floatBall(String id, String text, ScriptFloatHandler h) {
        String err = ScriptFloatRegistry.add(script, id, text, h);
        if (err != null) {
            log("注册悬浮球失败：" + err);
            return;
        }
        ScriptFloat.hookOnce();
        log("注册悬浮球「" + id + "」" + text + "（TIM 界面里能看到）");
    }

    @Override public void floatTheme(String id, String spec) {
        String err = ScriptFloatRegistry.setTheme(script, id, spec);
        if (err != null) log("设置悬浮球外观失败：" + err);
        else log("悬浮球「" + id + "」外观已更新：" + spec);
    }

    @Override public void hub(String title, ScriptFloatHandler h) {
        String err = ScriptHub.join(script, title, null, h);
        if (err != null) {
            log("接入官方球失败：" + err);
            return;
        }
        ScriptFloat.hookOnce();
        log("已接入官方球「" + title + "」");
    }

    @Override public void hubFirst(String title, ScriptFloatHandler h) {
        String err = ScriptHub.joinFront(script, title, null, h);
        if (err != null) {
            log("接入公共球失败：" + err);
            return;
        }
        ScriptFloat.hookOnce();
        log("已接入公共球（第一页）「" + title + "」");
    }

    @Override public void hubDown(String title, ScriptFloatHandler h) {
        String err = ScriptHub.joinVertical(script, title, null, h);
        if (err != null) {
            log("接入公共球（上下组）失败：" + err);
            return;
        }
        ScriptFloat.hookOnce();
        log("已接入公共球（上下滑动组）「" + title + "」");
    }

    @Override public void hubItem(String label, ScriptFloatHandler h) {
        String err = ScriptHub.addItem(script, null, label, h);
        if (err != null) log("官方球菜单项失败：" + err);
        else log("官方球加了菜单项「" + label + "」");
    }

    @Override public void hubIcon(String icon) {
        String err = ScriptHub.setIcon(script, icon);
        if (err != null) log("设置页图标失败：" + err);
    }

    @Override public void hubOut() {
        if (ScriptHub.leave(script)) log("已退出官方球");
    }

    @Override public void menu(String label, ScriptMenuHandler h) {
        String err = ScriptMenuRegistry.add(script, label, h);
        if (err != null) log("注册长按菜单失败：" + err);
        else log("注册长按菜单「" + label + "」");
    }

    @Override public void panel(String title, String specJson, ScriptPanelHandler h) {
        String err = ScriptPanelRegistry.add(script, title, specJson, h);
        if (err != null) log("注册设置面板失败：" + err);
        else log("注册设置面板「" + title + "」");
    }

    @Override public void panelPage(String title,
                                    com.cathelperawa.owo.app.ui.ScriptScreen.Handler h) {
        String err = ScriptPanelRegistry.addCustom(script, title, h);
        if (err != null) log("注册自绘页面失败：" + err);
        else log("注册自绘页面「" + title + "」");
    }

    @Override public void tag(String name, ScriptTagHandler h) {
        String err = ScriptTagRegistry.add(script, name, h);
        if (err != null) log("注册 AI 标签失败：" + err);
        else log("注册 AI 标签「" + name + "」");
    }

    @Override public void onMessage(ScriptMessageHandler h) {
        String err = ScriptMessageRegistry.add(script, h);
        if (err != null) log("注册消息监听失败：" + err);
        else log("已开始监听全部消息");
    }

    private final java.util.List<Runnable> stopHooks = new java.util.ArrayList<Runnable>();

    @Override public void onStop(Runnable r) {
        if (r != null) stopHooks.add(r);
    }

    void fireStop() {
        for (int i = 0; i < stopHooks.size(); i++) {
            try {
                final Runnable r = stopHooks.get(i);
                ScriptExec.call(script, new Runnable() {
                    @Override public void run() {
                        r.run();
                    }
                }, ScriptExec.TIMEOUT_MS);
            } catch (Throwable t) {
                log("onStop 异常 " + t);
            }
        }
    }

    @Override public void floatMenuItem(String ballId, String itemId, String label,
                                        ScriptFloatHandler h) {
        String err = ScriptFloatRegistry.addMenuItem(script, ballId, itemId, label, h);
        if (err != null) log("加悬浮球菜单项失败：" + err);
        else log("球「" + ballId + "」加了菜单项「" + label + "」");
    }

    @Override public void provide(String name, ScriptServiceHandler h) {
        String err = ScriptBusRegistry.provide(script, name, h);
        if (err != null) log("发布服务失败：" + err);
        else log("发布服务「" + name + "」");
    }

    @Override public String call(String name, String args) {
        return ScriptBusRegistry.call(name, args);
    }

    @Override public String[] services() {
        java.util.List<String> l = ScriptBusRegistry.list();
        return l.toArray(new String[0]);
    }

    @Override public void hook(String event, ScriptBusHandler h) {
        on(event, h);
    }

    @Override public void emit(String event, String data) {
        ScriptBusRegistry.emit(event, data);
    }

    @Override public void on(String event, ScriptBusHandler h) {
        String err = ScriptBusRegistry.on(script, event, h);
        if (err != null) log("监听事件失败：" + err);
    }

    @Override public boolean sendText(String kind, String peer, String text) {
        need("send");
        try {
            boolean ok = MsgApi.sendText(chatType(kind), peer, text);
            if (ok) SentGuard.mark(text == null ? "" : text.trim());
            else log("发送失败：sendText 返回 false（kind=" + kind + " peer=" + peer + "）");
            return ok;
        } catch (Throwable t) {
            log("发送失败 " + t);
            return false;
        }
    }

    @Override public boolean sendPic(String kind, String peer, String path) {
        need("send");
        try {
            return MsgApi.sendPic(chatType(kind), peer, path);
        } catch (Throwable t) {
            log("发图失败 " + t);
            return false;
        }
    }

    static int chatType(String kind) {
        return "group".equals(kind) ? 2 : 1;
    }

    @Override public boolean sendCard(String kind, String peer, String arkJson) {
        need("send");
        try { return MsgApi.sendCard(chatType(kind), peer, arkJson); }
        catch (Throwable t) { log("发卡片失败 " + t); return false; }
    }

    @Override public boolean sendVoice(String kind, String peer, String path) {
        need("send");
        try { return MsgApi.sendPtt(chatType(kind), peer, path); }
        catch (Throwable t) { log("发语音失败 " + t); return false; }
    }

    @Override public boolean sendFile(String kind, String peer, String path) {
        need("send");
        try { return MsgApi.sendFile(chatType(kind), peer, path); }
        catch (Throwable t) { log("发文件失败 " + t); return false; }
    }

    @Override public boolean mute(String group, String uid, int seconds) {
        need("admin");
        try { return com.cathelperawa.owo.app.api.GroupManageApi.mute(group, uid, seconds); }
        catch (Throwable t) { log("禁言失败 " + t); return false; }
    }

    @Override public boolean kick(String group, String uid) {
        need("admin");
        try { return com.cathelperawa.owo.app.api.GroupManageApi.kick(group, uid); }
        catch (Throwable t) { log("踢人失败 " + t); return false; }
    }

    @Override public String httpGet(String url) {
        need("network");
        try {
            com.cathelperawa.owo.app.api.NetApi.Result r =
                    com.cathelperawa.owo.app.api.NetApi.getSync(url, null, 15000);
            return (r == null) ? "" : (r.body == null ? "" : r.body);
        } catch (Throwable t) { log("GET 失败 " + t); return ""; }
    }

    @Override public String httpPost(String url, String jsonBody) {
        need("network");
        try {
            com.cathelperawa.owo.app.api.NetApi.Result r =
                    com.cathelperawa.owo.app.api.NetApi.postSync(url,
                            "{\"Content-Type\":\"application/json\"}", jsonBody, 20000);
            return (r == null) ? "" : (r.body == null ? "" : r.body);
        } catch (Throwable t) { log("POST 失败 " + t); return ""; }
    }

    @Override public String httpAsync(String url, String optsJson, final ScriptHttpHandler cb) {
        need("network");
        if (url == null || url.length() == 0) return "url 不能为空";
        String method = "GET";
        String body = null;
        int timeout = 20000;
        String headers = null;
        try {
            if (optsJson != null && optsJson.trim().length() > 0) {
                org.json.JSONObject o = new org.json.JSONObject(optsJson);
                String m = o.optString("method", "GET");
                if (m != null && m.length() > 0) method = m.trim().toUpperCase();
                if (o.has("body")) body = o.optString("body", "");
                timeout = o.optInt("timeout", timeout);
                if (o.has("headers")) {
                    Object h = o.opt("headers");
                    if (h instanceof org.json.JSONObject) headers = h.toString();
                }
            }
        } catch (Throwable t) {
            return "optsJson 解析失败：" + t;
        }
        if (timeout < 1000) timeout = 1000;
        if (timeout > 120000) timeout = 120000;

        if (("GET".equals(method) || "HEAD".equals(method))) body = null;
        if (body != null && headers == null) headers = "{\"Content-Type\":\"application/json\"}";

        final ScriptHttpHandler fcb = cb;
        final String fmethod = method;
        com.cathelperawa.owo.app.api.NetApi.Callback c =
                new com.cathelperawa.owo.app.api.NetApi.Callback() {
            @Override public void onResult(final com.cathelperawa.owo.app.api.NetApi.Result r) {
                if (fcb == null) return;
                ScriptExec.run(script, new Runnable() {
                    @Override public void run() {
                        try {
                            fcb.onResponse(r == null ? 0 : r.code,
                                    r == null || r.body == null ? "" : r.body,
                                    r == null ? "无响应" : r.err);
                        } catch (Throwable t) {
                            log("httpAsync 回调异常 " + t);
                        }
                    }
                });
            }
        };
        try {
            if ("GET".equals(fmethod) || "HEAD".equals(fmethod)) {
                com.cathelperawa.owo.app.api.NetApi.get(url, headers, timeout, c);
            } else if ("POST".equals(fmethod)) {
                com.cathelperawa.owo.app.api.NetApi.post(url, headers, body, c);
            } else {
                com.cathelperawa.owo.app.api.NetApi.request(fmethod, url, headers, body, timeout, c);
            }
            return null;
        } catch (Throwable t) {
            return "发起请求失败：" + t;
        }
    }

    @Override public String httpStream(String url, String optsJson, final ScriptHttpHandler cb) {
        need("network");
        if (url == null || url.length() == 0) return "url 不能为空";
        String method = "GET";
        String body = null;
        int timeout = 120000;
        String headers = null;
        try {
            if (optsJson != null && optsJson.trim().length() > 0) {
                org.json.JSONObject o = new org.json.JSONObject(optsJson);
                String m = o.optString("method", "GET");
                if (m != null && m.length() > 0) method = m.trim().toUpperCase();
                if (o.has("body")) body = o.optString("body", "");
                timeout = o.optInt("timeout", timeout);
                if (o.has("headers")) {
                    Object h = o.opt("headers");
                    if (h instanceof org.json.JSONObject) headers = h.toString();
                }
            }
        } catch (Throwable t) {
            return "optsJson 解析失败：" + t;
        }
        final String fmethod = method;
        final String fbody = body;
        final String fheaders = headers;
        final int fto = timeout;
        final String furl = url;
        final ScriptHttpHandler fcb = cb;

        Thread t = new Thread(new Runnable() {
            @Override public void run() {
                java.net.HttpURLConnection c = null;
                try {
                    c = (java.net.HttpURLConnection) new java.net.URL(furl).openConnection();
                    c.setRequestMethod(fmethod);
                    c.setConnectTimeout(15000);
                    c.setReadTimeout(fto);
                    c.setDoInput(true);
                    if (fheaders != null) {
                        org.json.JSONObject h = new org.json.JSONObject(fheaders);
                        java.util.Iterator<String> it = h.keys();
                        while (it.hasNext()) {
                            String k = it.next();
                            c.setRequestProperty(k, h.optString(k, ""));
                        }
                    }
                    if (fbody != null && !"GET".equals(fmethod) && !"HEAD".equals(fmethod)) {
                        c.setDoOutput(true);
                        if (c.getRequestProperty("Content-Type") == null) {
                            c.setRequestProperty("Content-Type", "application/json");
                        }
                        java.io.OutputStream os = c.getOutputStream();
                        try {
                            os.write(fbody.getBytes("UTF-8"));
                            os.flush();
                        } finally {
                            os.close();
                        }
                    }
                    final int code = c.getResponseCode();
                    java.io.InputStream in = (code >= 400) ? c.getErrorStream() : c.getInputStream();
                    if (in == null) {
                        emitStream(fcb, 0, "", "HTTP " + code + " 没有响应体");
                        return;
                    }
                    java.io.BufferedReader br = new java.io.BufferedReader(
                            new java.io.InputStreamReader(in, "UTF-8"));
                    String line;
                    while ((line = br.readLine()) != null) {
                        final String l = line;
                        emitStream(fcb, 0, l, null);
                    }
                    br.close();
                    emitStream(fcb, code, "", null);
                } catch (Throwable e) {
                    emitStream(fcb, 0, "", String.valueOf(e));
                } finally {
                    try { if (c != null) c.disconnect(); } catch (Throwable ignore) {}
                }
            }
        }, "cat-script-net");
        t.setDaemon(true);
        t.start();
        return null;
    }

    private void emitStream(final ScriptHttpHandler cb, final int code,
                            final String body, final String err) {
        if (cb == null) return;
        ScriptExec.run(script, new Runnable() {
            @Override public void run() {
                try {
                    cb.onResponse(code, body == null ? "" : body, err);
                } catch (Throwable t) {
                    log("httpStream 回调异常 " + t);
                }
            }
        });
    }

    @Override public boolean fileWrite(String name, String content) {
        need("storage");
        return ScriptFiles.write(dataDir, name, content);
    }

    @Override public boolean fileAppend(String name, String content) {
        need("storage");
        return ScriptFiles.append(dataDir, name, content);
    }

    @Override public String fileRead(String name) {
        need("storage");
        return ScriptFiles.read(dataDir, name);
    }

    @Override public boolean fileExists(String name) {
        return ScriptFiles.exists(dataDir, name);
    }

    @Override public boolean fileDelete(String name) {
        need("storage");
        return ScriptFiles.delete(dataDir, name);
    }

    @Override public boolean fileMkdir(String name) {
        need("storage");
        return ScriptFiles.mkdir(dataDir, name);
    }

    @Override public boolean fileRename(String from, String to) {
        need("storage");
        return ScriptFiles.rename(dataDir, from, to);
    }

    @Override public String[] fileList(String name) {
        return ScriptFiles.list(dataDir, name);
    }

    @Override public long fileSize(String name) {
        return ScriptFiles.size(dataDir, name);
    }

    @Override public String dataDir() {
        try {
            return dataDir == null ? "" : dataDir.getAbsolutePath();
        } catch (Throwable t) {
            return "";
        }
    }

    private static int chatTypeOf(String kind) {
        return "group".equals(kind) ? 2 : 1;
    }

    @Override public boolean sendReply(String kind, String peer, long replyMsgId, String text) {
        need("send");
        return com.cathelperawa.owo.app.api.CatApi.sendReply(
                chatTypeOf(kind), peer, replyMsgId, text);
    }

    @Override public boolean recall(String kind, String peer, long msgId) {
        return com.cathelperawa.owo.app.api.CatApi.recallMsg(chatTypeOf(kind), peer, msgId);
    }

    @Override public boolean sendPtt(String kind, String peer, String path) {
        return com.cathelperawa.owo.app.api.CatApi.sendPtt(chatTypeOf(kind), peer, path);
    }

    @Override public boolean sendFace(String kind, String peer, int faceId) {
        return com.cathelperawa.owo.app.api.CatApi.sendFace(chatTypeOf(kind), peer, faceId);
    }

    @Override public String at(String uid, String name) {
        if (uid == null || uid.length() == 0) return "";
        String n = (name == null || name.length() == 0) ? "" : name;
        return "[atUin=" + uid + "]" + n + " ";
    }

    @Override public String uinOf(String uid) {
        try {
            return com.cathelperawa.owo.app.api.IdentityApi.uinFromUid(uid);
        } catch (Throwable t) {
            return "";
        }
    }

    @Override public String uidOf(String uin) {
        try {
            return com.cathelperawa.owo.app.api.IdentityApi.uidFromUin(uin);
        } catch (Throwable t) {
            return "";
        }
    }

    @Override public String nameOf(String peerOrGroup, String uin) {
        try {
            if (peerOrGroup != null && peerOrGroup.length() >= 5
                    && peerOrGroup.indexOf('_') < 0) {
                String n = com.cathelperawa.owo.app.api.IdentityApi.troopMemberName(peerOrGroup, uin);
                if (n != null && n.length() > 0 && !n.equals(uin)) return n;
            }
            return com.cathelperawa.owo.app.api.IdentityApi.buddyName(uin);
        } catch (Throwable t) {
            return "";
        }
    }

    @Override public void toast(String text) {
        try {
            com.cathelperawa.owo.app.bridge.CatToast.show(
                    text == null ? "" : text);
        } catch (Throwable t) {
            log("toast 失败 " + t);
        }
    }

    @Override public boolean can(String cap) {
        if (cap == null) return false;
        if ("send".equals(cap) || "storage".equals(cap)) return true;
        return caps.contains(cap);
    }

    @Override public String exec(String cmd) {
        needLocal("ctx.exec（执行 shell）");
        if (cmd == null || cmd.trim().length() == 0) return "";
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"sh", "-c", cmd});
            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
            java.io.InputStream in = p.getInputStream();
            java.io.InputStream er = p.getErrorStream();
            long deadline = System.currentTimeMillis() + 60000;
            byte[] buf = new byte[4096];
            int n;

            while ((n = in.read(buf)) > 0) {
                bos.write(buf, 0, n);
                if (bos.size() > 4194304 || System.currentTimeMillis() > deadline) break;
            }

            while (er.available() > 0 && (n = er.read(buf)) > 0) {
                bos.write(buf, 0, n);
                if (bos.size() > 4194304) break;
            }
            try { in.close(); } catch (Throwable ignore) {}
            try { er.close(); } catch (Throwable ignore) {}
            try { p.destroy(); } catch (Throwable ignore) {}
            return new String(bos.toByteArray(), "UTF-8");
        } catch (Throwable t) {
            return "执行失败：" + t;
        }
    }

    @Override public android.content.Context context() {
        try {
            return com.cathelperawa.owo.app.bridge.QQEnv.appContext();
        } catch (Throwable t) {
            return null;
        }
    }

    @Override public android.app.Activity activity() {
        try {
            return com.cathelperawa.owo.app.bridge.QQEnv.topActivity();
        } catch (Throwable t) {
            return null;
        }
    }

    private ScriptEngine engine;

    void setEngine(ScriptEngine e) {
        this.engine = e;
    }

    @Override public void strategy(String point, final String funcName) {
        needKernel("strategy");
        if (point == null || funcName == null) return;
        StrategyRegistry.register(script, point, new StrategyRegistry.Handler() {
            @Override public String pick(String input, String def) {
                if (engine == null) return null;
                Object r = engine.callFunction(funcName, new Object[]{input, def});
                return (r == null) ? null : String.valueOf(r);
            }
        });
    }

    @Override public String peerName(String peer) {
        try { return com.cathelperawa.owo.app.api.IdentityApi.troopName(peer); }
        catch (Throwable t) { return ""; }
    }

    @Override public String get(String key) {
        need("storage");
        try {
            File f = kvFile(key);
            if (f == null || !f.exists()) return "";

            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
            java.io.FileInputStream in = new java.io.FileInputStream(f);
            try {
                byte[] buf = new byte[4096];
                int n;
                while ((n = in.read(buf)) > 0) bos.write(buf, 0, n);
            } finally {
                in.close();
            }
            return new String(bos.toByteArray(), "UTF-8");
        } catch (Throwable t) {
            return "";
        }
    }

    @Override public int getInt(String key, int def) {
        try {
            String v = get(key);
            if (v == null || v.trim().length() == 0) return def;
            return Integer.parseInt(v.trim());
        } catch (Throwable t) {
            return def;
        }
    }

    @Override public void setInt(String key, int v) { set(key, String.valueOf(v)); }

    @Override public boolean getBool(String key, boolean def) {
        try {
            String v = get(key);
            if (v == null) return def;
            String s2 = v.trim().toLowerCase();
            if (s2.length() == 0) return def;
            return "1".equals(s2) || "true".equals(s2) || "yes".equals(s2) || "on".equals(s2);
        } catch (Throwable t) {
            return def;
        }
    }

    @Override public void setBool(String key, boolean v) { set(key, v ? "1" : "0"); }

    @Override public String get(String key, String def) {
        try {
            String v = get(key);
            return v == null ? def : v;
        } catch (Throwable t) {
            return def;
        }
    }

    @Override public void set(String key, String value) {
        need("storage");
        try {
            File f = kvFile(key);
            if (f == null) return;
            if (f.getParentFile() != null) f.getParentFile().mkdirs();
            java.io.FileOutputStream out = new java.io.FileOutputStream(f);
            try {
                out.write((value == null ? "" : value).getBytes("UTF-8"));
            } finally {
                out.close();
            }
        } catch (Throwable t) {
            log("写数据失败 " + t);
        }
    }

    @Override public void theme(String from, String to) {
        needKernel("theme");
        if (from == null || to == null) return;
        try {
            com.cathelperawa.owo.app.core.Skin.setColor(script, from, to);
            log("换色 " + from + " → " + to);
        } catch (Throwable t) {
            log("换色失败 " + t);
        }
    }

    @Override public void icon(String key, String value) {
        needKernel("icon");
        if (key == null || value == null) return;
        try {
            com.cathelperawa.owo.app.core.Skin.setIcon(script, key, value);
            log("换图标 " + key);
        } catch (Throwable t) {
            log("换图标失败 " + t);
        }
    }

    @Override public void bg(String name, String value) {
        needKernel("bg");
        if (name == null || value == null) return;
        try {
            com.cathelperawa.owo.app.core.Skin.setBg(script, name, value);
            log("换背景 " + name);
        } catch (Throwable t) {
            log("换背景失败 " + t);
        }
    }

    @Override public void hide(String key) {
        needKernel("hide");
        if (key == null) return;
        try {
            com.cathelperawa.owo.app.core.Skin.hide(script, key);
            log("隐藏入口 " + key);
        } catch (Throwable t) {
            log("隐藏失败 " + t);
        }
    }

    @Override public void style(String key, String json) {
        needKernel("style");
        if (key == null || json == null) return;
        try {
            com.cathelperawa.owo.app.core.Skin.setStyle(script, key, json);
            log("样式 " + key + " = " + json);
        } catch (Throwable t) {
            log("样式设置失败 " + t);
        }
    }

    @Override public void text(String key, String value) {
        needKernel("text");
        if (key == null || value == null) return;
        try {
            com.cathelperawa.owo.app.core.Skin.setText(script, key, value);
        } catch (Throwable t) {
            log("改文案失败 " + t);
        }
    }

    @Override public void overlay(String id, com.cathelperawa.owo.app.ui.ScriptScreen.Handler h) {
        needKernel("overlay");
        if (id == null || h == null) return;
        try {
            com.cathelperawa.owo.app.ui.ScriptOverlay.show(script, id, h);
            log("显示浮层 " + id);
        } catch (Throwable t) {
            log("浮层失败 " + t);
        }
    }

    @Override public void overlayClose(String id) {
        try {
            com.cathelperawa.owo.app.ui.ScriptOverlay.hide(script, id);
        } catch (Throwable ignore) {
        }
    }

    @Override public void dialog(String title, com.cathelperawa.owo.app.ui.ScriptScreen.Handler h) {
        needKernel("dialog");
        if (h == null) return;
        try {
            com.cathelperawa.owo.app.ui.ScriptDialog.show(title, h);
            log("弹出对话框：" + title);
        } catch (Throwable t) {
            log("弹窗失败 " + t);
        }
    }

    @Override public void setting(String title, String pageKey) {
        needKernel("setting");
        if (title == null) return;
        try {
            com.cathelperawa.owo.app.core.ScriptSetting.register(script, title, pageKey, "");
            log("设置页新增入口：" + title);
        } catch (Throwable t) {
            log("新增入口失败 " + t);
        }
    }

    @Override public void screen(String pageKey, com.cathelperawa.owo.app.ui.ScriptScreen.Handler h) {
        needKernel("screen");
        if (pageKey == null || h == null) return;
        try {
            com.cathelperawa.owo.app.ui.ScriptScreen.register(script, pageKey, h);
            log("接管页面 " + pageKey);
        } catch (Throwable t) {
            log("接管页面失败 " + t);
        }
    }

    @Override public String needs(String id) {
        try {
            String real = ScriptHost.resolveName(id);
            if (real == null || !ScriptHost.isEnabled(real)) return "";
            ScriptMeta m = ScriptHost.metaOf(real);
            return m == null ? "" : (m.version == null ? "" : m.version);
        } catch (Throwable t) {
            return "";
        }
    }

    @Override public void strategy(String point, StrategyRegistry.Handler h) {
        if (point == null || h == null) return;
        try {
            StrategyRegistry.register(script, point, h);
            log("注册策略 " + point);
        } catch (Throwable t) {
            log("策略注册失败 " + t);
        }
    }

    @Override public String every(int seconds, ScriptTimer.Handler h) {
        try {
            return ScriptTimer.every(script, seconds, h);
        } catch (Throwable t) {
            log("定时任务失败 " + t);
            return null;
        }
    }

    @Override public String at(String hhmm, ScriptTimer.Handler h) {
        try {
            return ScriptTimer.at(script, hhmm, h);
        } catch (Throwable t) {
            log("定时任务失败 " + t);
            return null;
        }
    }

    @Override public boolean cancelTimer(String id) {
        try {
            return ScriptTimer.cancel(id);
        } catch (Throwable t) {
            return false;
        }
    }

    @Override public java.util.List<String> findClasses(String keyword, int limit) {
        try {
            return ReflectHelper.findClasses(keyword, limit <= 0 ? 30 : limit);
        } catch (Throwable t) {
            return new java.util.ArrayList<String>();
        }
    }

    @Override public java.util.List<String> methodsOf(String className, int limit) {
        try {
            return ReflectHelper.methodsOf(className, limit <= 0 ? 60 : limit);
        } catch (Throwable t) {
            return new java.util.ArrayList<String>();
        }
    }

    @Override public java.util.List<String> fieldsOf(String className, int limit) {
        try {
            return ReflectHelper.fieldsOf(className, limit <= 0 ? 60 : limit);
        } catch (Throwable t) {
            return new java.util.ArrayList<String>();
        }
    }

    @Override public java.util.List<String> ctorsOf(String className) {
        try {
            return ReflectHelper.ctorsOf(className);
        } catch (Throwable t) {
            return new java.util.ArrayList<String>();
        }
    }

    @Override public void method(String target, MethodHook.Handler h) {
        method(target, "before", h);
    }

    @Override public void method(String target, String mode, MethodHook.Handler h) {
        needLocal("ctx.method（方法重写）");
        needKernel("method");
        if (target == null || h == null) return;
        try {
            String err = MethodHook.install(script, target, mode, h);
            if (err != null) {
                log("方法重写失败：" + err);
                throw new SecurityException(err);
            }
            log("方法重写 " + target);
        } catch (SecurityException se) {
            throw se;
        } catch (Throwable t) {
            log("方法重写异常 " + t);
        }
    }

    @Override public ScriptEngineHandle newEngine(String lang) {
        needKernel("newEngine");
        try {
            ScriptEngine e = ScriptEngines.createFor(lang);
            if (e == null) {
                log("不支持的引擎：" + lang);
                return null;
            }
            return new ScriptEngineHandle(script, e);
        } catch (Throwable t) {
            log("起子引擎失败 " + t);
            return null;
        }
    }

    @Override public String eval(String code, String lang) {
        ScriptEngineHandle h = newEngine(lang);
        if (h == null) return "";
        try {
            return h.eval(code);
        } finally {
            h.close();
        }
    }

    @Override public String registerEngine(String suffix, String funcName) {
        needLocal("registerEngine（注册引擎）");
        return ScriptEngines.register(suffix, script, funcName);
    }

    @Override public String registerEngine(String suffix, String funcName, String forms) {
        needLocal("registerEngine（注册引擎）");
        return ScriptEngines.register(suffix, script, funcName, forms);
    }

    @Override public String unregisterEngine(String suffix) {
        return ScriptEngines.unregister(suffix);
    }

    @Override public void page(String title, String sub, String key,
                               com.cathelperawa.owo.app.ui.ScriptScreen.Handler h) {
        needLocal("ctx.page（新增页面）");
        com.cathelperawa.owo.app.ui.ScriptScreen.registerPage(script, title, sub, key, h);
    }

    @Override public void onActivity(com.cathelperawa.owo.app.script.ActivityBus.Handler h) {

        try {
            needKernel("onActivity");
        } catch (SecurityException se) {
            log("ctx.onActivity 被拒：" + se.getMessage());
            throw se;
        }
        ActivityBus.add(script, h, activity());
    }

    @Override public android.view.View contentRoot() {
        try {
            android.app.Activity a = activity();
            return a == null ? null : a.findViewById(android.R.id.content);
        } catch (Throwable t) {
            return null;
        }
    }

    @Override public android.view.View decorView() {
        try {
            android.app.Activity a = activity();
            return a == null ? null : a.getWindow().getDecorView();
        } catch (Throwable t) {
            return null;
        }
    }

    @Override public android.view.View findText(android.view.View root, String text) {
        return ViewFinder.findText(root, text);
    }

    @Override public android.view.View findByClass(android.view.View root, String cls) {
        return ViewFinder.findByClass(root, cls);
    }

    @Override public android.view.View findByDesc(android.view.View root, String desc) {
        return ViewFinder.findByDesc(root, desc);
    }

    @Override public void walk(android.view.View root, ViewFinder.Handler h) {
        ViewFinder.walk(root, h);
    }

    @Override public void setPageBackground(android.app.Activity a, String pathOrColor) {
        if (a == null || pathOrColor == null) return;
        try {
            final android.view.View root = a.findViewById(android.R.id.content);
            if (root == null) return;
            final String v = pathOrColor.trim();
            if (v.startsWith("#")) {
                int c = (int) Long.parseLong(v.substring(1), 16);
                root.setBackgroundColor(v.length() == 9 ? c : (0xFF000000 | c));
                return;
            }
            android.graphics.Bitmap bm = android.graphics.BitmapFactory.decodeFile(v);
            if (bm == null) {
                log("setPageBackground：图片读不出来 " + v);
                return;
            }
            root.setBackgroundDrawable(new android.graphics.drawable.BitmapDrawable(
                    a.getResources(), bm));
        } catch (Throwable t) {
            log("setPageBackground 失败 " + t);
        }
    }

    @Override public String floatView(android.view.View v, int gravity) {
        return floatView(v, gravity, 0, 0);
    }

    @Override public String floatView(android.view.View v, int gravity, int x, int y) {
        needKernel("floatView");
        return ScriptWindow.add(script, activity(), v, gravity, x, y);
    }

    @Override public void closeFloat(String id) {
        ScriptWindow.close(id);
    }

    @Override public void runOnUi(final Runnable r) {
        if (r == null) return;
        try {
            android.app.Activity a = activity();
            if (a != null) a.runOnUiThread(r);
            else new android.os.Handler(android.os.Looper.getMainLooper()).post(r);
        } catch (Throwable t) {
            log("runOnUi 失败 " + t);
        }
    }

    @Override public void patch(String point, PatchPoint.Handler h) {
        needLocal("ctx.patch（点位）");
        needKernel("patch");
        if (point == null || h == null) return;
        try {
            PatchPoint.register(script, point, h);
            log("挂点位 " + point);
        } catch (Throwable t) {
            log("挂点位失败 " + t);
        }
    }

    @Override public void unpatch(String point) {

        try {
            log("撤销点位 " + point + "（下次停用/重载生效）");
        } catch (Throwable ignore) {
        }
    }

    @Override public void configForce(String key, String value) {
        needLocal("configForce（强制配置）");
        needKernel("configForce");
        if (key == null || value == null) return;
        try {
            com.cathelperawa.owo.app.core.ConfigOverride.set(script, key, value);
            log("强制配置 " + key + "=" + value);
        } catch (Throwable t) {
            log("配置覆盖失败 " + t);
        }
    }

    @Override public void configClear(String key) {
        try {
            com.cathelperawa.owo.app.core.ConfigOverride.clear(script, key);
        } catch (Throwable ignore) {
        }
    }

    @Override public void skinReset() {
        try {
            int n = com.cathelperawa.owo.app.core.Skin.removeByScript(script);
            log("已撤回 " + n + " 项外观改动");
        } catch (Throwable t) {
            log("撤回失败 " + t);
        }
    }

    private File kvFile(String key) {
        if (dataDir == null || key == null) return null;

        if (key.length() > 48 || key.indexOf("..") >= 0 || key.indexOf('/') >= 0
                || key.indexOf('\\') >= 0 || key.indexOf(':') >= 0
                || key.indexOf('*') >= 0 || key.indexOf('?') >= 0
                || key.indexOf('"') >= 0 || key.indexOf('<') >= 0
                || key.indexOf('>') >= 0 || key.indexOf('|') >= 0) {
            log("非法的 key: " + key);
            return null;
        }
        for (int i = 0; i < key.length(); i++) {
            if (Character.isISOControl(key.charAt(i))) {
                log("非法的 key（含控制字符）");
                return null;
            }
        }
        return new File(dataDir, key + ".txt");
    }

    private void needLocal(String what) {
        if (com.cathelperawa.owo.app.script.ScriptHost.isExternal(script)) {
            String why = "外部来源脚本不能使用 " + what
                    + "（放进自己的脚本目录并去掉 source=external 后重新启用）";
            HookLog.w("[脚本] " + script + " " + why);
            throw new SecurityException(why);
        }
    }

    private void needKernel(String what) {

        needLocal("内核能力（" + what + "）");
        if (ScriptTrust.full()) return;
        if (!com.cathelperawa.owo.app.core.KernelWhitelist.allowed(script)) {
            throw new SecurityException("脚本[" + script + "] 未获内核授权，不能使用 "
                    + what + "（脚本页 → 内核分区 → 授予内核权限）");
        }
    }

    private static final java.util.Set<String> WARNED =
            java.util.Collections.synchronizedSet(new java.util.HashSet<String>());

    private void need(String cap) {

        if ("send".equals(cap) || "storage".equals(cap)) return;

        if (ScriptTrust.full() && !caps.contains("no-network")) return;

        if ("network".equals(cap)) {
            if (!caps.contains("no-network")) return;
            String why = "这个脚本声明了 no-network，网络被禁用";
            if (WARNED.add(script + "|no-network")) {
                com.cathelperawa.owo.app.core.HookLog.w("[脚本] " + script + " " + why);
            }
            throw new SecurityException(why);
        }
        if (!caps.contains(cap)) {
            String why = "这个脚本没声明 " + cap
                    + " 能力（文件头加 // capability: " + cap + "）";

            if (WARNED.add(script + "|" + cap)) {
                try {
                    com.cathelperawa.owo.app.bridge.CatToast.show(
                            "脚本[" + script + "] 缺能力 " + cap
                                    + "，请在文件头加 // capability: " + cap);
                } catch (Throwable ignore) {
                }
                try {
                    ScriptFiles.append(dataDir, "error.txt",
                            "[" + new java.text.SimpleDateFormat("MM-dd HH:mm:ss")
                                    .format(new java.util.Date()) + "] " + why + "\n");
                } catch (Throwable ignore) {
                }
            }
            com.cathelperawa.owo.app.core.HookLog.w("[脚本] " + script + " " + why);
            throw new SecurityException(why);
        }
    }

    @Override public String probe(String target) {
        return probe(target, null);
    }

    @Override public String probe(String target, String member) {
        needLocal("ctx.probe（查看宿主类）");
        try {
            return ScriptReflect.probe(hostLoader(), target, member);
        } catch (Throwable t) {
            return "探查失败：" + t;
        }
    }

    @Override public Object tryCall(String className, String method, Object... args) {
        needLocal("ctx.tryCall（调宿主方法）");
        try {
            return ScriptReflect.call(hostLoader(), null, className, method, args);
        } catch (Throwable t) {
            return null;
        }
    }

    @Override public String tryCallEx(String className, String method, Object... args) {
        needLocal("ctx.tryCallEx（调宿主方法）");
        try {
            return ScriptReflect.toJson(
                    ScriptReflect.callEx(hostLoader(), null, className, method, args));
        } catch (Throwable t) {
            return "{\"ok\":false,\"err\":\"" + ScriptReflect.esc(String.valueOf(t)) + "\"}";
        }
    }

    private static ClassLoader hostLoader() {
        try {
            ClassLoader cl = com.cathelperawa.owo.app.bridge.QQEnv.classLoader();
            if (cl != null) return cl;
        } catch (Throwable ignore) {
        }
        return ScriptApiImpl.class.getClassLoader();
    }

    @Override public ClassLoader loadDex(String rel) {
        needLocal("ctx.loadDex（加载 dex）");
        return ScriptDex.load(rel);
    }

    @Override public Class loadDexClass(String rel, String className) {
        needLocal("ctx.loadDexClass（加载 dex）");
        return ScriptDex.loadClass(rel, className);
    }

    @Override public ClassLoader loadJar(String rel) {
        needLocal("ctx.loadJar（加载 jar）");
        return ScriptJar.load(rel);
    }

    @Override public Class defineClass(String className, byte[] classBytes) {
        needLocal("ctx.defineClass（动态造类）");
        return ScriptJar.defineClass(className, classBytes);
    }
}
