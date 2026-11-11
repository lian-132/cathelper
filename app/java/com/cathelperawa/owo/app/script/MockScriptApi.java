package com.cathelperawa.owo.app.script;

import com.cathelperawa.owo.app.core.HookLog;

import java.util.HashMap;
import java.util.Map;

public final class MockScriptApi implements ScriptApi {

    private final String script;
    private final Map<String, String> mem = new HashMap<String, String>();

    public MockScriptApi(String script) {
        this.script = script == null ? "script" : script;
    }

    @Override public void log(String s) {
        HookLog.i("[试跑][" + script + "] " + s);
    }

    @Override public String me() { return "10000"; }

    @Override public String myName() { return "试跑模式"; }

    @Override public long now() { return System.currentTimeMillis(); }

    @Override public String scriptName() { return script; }

    @Override public boolean hasCap(String cap) { return true; }

    @Override public String ai(String prompt) {
        log("ai(\"" + prompt + "\") → 试跑不真的问");
        return "（试跑模式：没有真实的 AI 回复）";
    }

    @Override public void route(String id, String[] match, ScriptHandler h) {
        if (match == null || match.length == 0) return;
        log("注册命令「" + match[0] + "」（试跑，不生效）");
        MockRoutes.add(script, match[0]);
    }

    @Override public void tool(String name, String desc, ScriptToolHandler h) {
        log("注册 AI 工具「" + name + "」（试跑，不生效）");
        MockRoutes.add(script, "工具:" + name);
    }

    @Override public void floatBall(String id, String text, ScriptFloatHandler h) {
        log("注册悬浮球「" + id + "」" + text + "（试跑，不生效）");
        MockRoutes.add(script, "球:" + id);
    }

    @Override public void menu(String label, ScriptMenuHandler h) {
        log("注册长按菜单「" + label + "」（试跑，不生效）");
        MockRoutes.add(script, "菜单:" + label);
    }

    @Override public void panel(String title, String specJson, ScriptPanelHandler h) {
        log("注册设置面板「" + title + "」（试跑，不生效）");
        MockRoutes.add(script, "面板:" + title);
    }

    @Override public void panelPage(String title,
                                    com.cathelperawa.owo.app.ui.ScriptScreen.Handler h) {
        log("注册自绘页面「" + title + "」（试跑，不生效）");
        MockRoutes.add(script, "自绘页:" + title);
    }

    @Override public void tag(String name, ScriptTagHandler h) {
        log("注册 AI 标签「" + name + "」（试跑，不生效）");
        MockRoutes.add(script, "标签:" + name);
    }

    @Override public void onMessage(ScriptMessageHandler h) {
        log("开始监听全部消息（试跑，不生效）");
        MockRoutes.add(script, "监听全部消息");
    }

    @Override public void onStop(Runnable r) {
        log("注册了停用回调（试跑，不生效）");
    }

    @Override public int getInt(String key, int def) {
        try { return Integer.parseInt(mem.get(key)); } catch (Throwable t) { return def; }
    }

    @Override public void setInt(String key, int v) { mem.put(key, String.valueOf(v)); }

    @Override public boolean getBool(String key, boolean def) {
        String v = mem.get(key);
        if (v == null) return def;
        return "1".equals(v) || "true".equalsIgnoreCase(v);
    }

    @Override public void setBool(String key, boolean v) { mem.put(key, v ? "1" : "0"); }

    @Override public void provide(String name, ScriptServiceHandler h) {
        log("发布服务「" + name + "」（试跑，不生效）");
        MockRoutes.add(script, "服务:" + name);
    }

    @Override public String call(String name, String args) {
        log("调服务「" + name + "」（试跑）");
        return "";
    }

    @Override public String[] services() { return new String[0]; }

    @Override public void emit(String event, String data) {
        log("广播「" + event + "」（试跑，不生效）");
    }

    @Override public void hook(String event, ScriptBusHandler h) {
        log("监听宿主事件「" + event + "」（试跑，不生效）");
    }

    @Override public void on(String event, ScriptBusHandler h) {
        log("监听「" + event + "」（试跑，不生效）");
    }

    @Override public void floatMenuItem(String ballId, String itemId, String label,
                                        ScriptFloatHandler h) {
        log("给球「" + ballId + "」加菜单项「" + label + "」（试跑，不生效）");
    }

    @Override public void hub(String title, ScriptFloatHandler h) {
        log("hub(" + title + ")");
    }

    @Override public void hubFirst(String title, ScriptFloatHandler h) {
        log("hubFirst(" + title + ")");
    }

    @Override public void hubDown(String title, ScriptFloatHandler h) {
        log("hubDown(" + title + ")");
    }

    @Override public void hubItem(String label, ScriptFloatHandler h) {
        log("hubItem(" + label + ")");
    }

    @Override public void hubIcon(String icon) {
        log("hubIcon(" + icon + ")");
    }

    @Override public void hubOut() {
        log("hubOut()");
    }

    @Override public void floatTheme(String id, String spec) {
        log("想改悬浮球「" + id + "」外观：" + spec + "（试跑，不生效）");
    }

    @Override public boolean sendText(String kind, String peer, String text) {
        log("想发 " + kind + " " + peer + " : " + text);
        return true;
    }

    @Override public boolean sendPic(String kind, String peer, String path) {
        log("想发图 " + kind + " " + peer + " : " + path);
        return true;
    }

    @Override public boolean sendCard(String kind, String peer, String ark) {
        log("想发卡片 " + kind + " " + peer + " : " + (ark == null ? "" : ark.length() + " 字节"));
        return true;
    }

    @Override public boolean sendVoice(String kind, String peer, String path) {
        log("想发语音 " + kind + " " + peer + " : " + path);
        return true;
    }

    @Override public boolean sendFile(String kind, String peer, String path) {
        log("想发文件 " + kind + " " + peer + " : " + path);
        return true;
    }

    @Override public boolean mute(String group, String uid, int seconds) {
        log("想禁言 " + uid + " 在 " + group + " " + seconds + " 秒");
        return true;
    }

    @Override public boolean kick(String group, String uid) {
        log("想踢人 " + uid + " 出 " + group);
        return true;
    }

    @Override public String httpGet(String url) {
        log("想 GET " + url);
        return "";
    }

    @Override public String httpPost(String url, String body) {
        log("想 POST " + url);
        return "";
    }

    @Override public String peerName(String peer) { return "试跑会话"; }

    @Override public String get(String key) { return mem.get(key); }

    @Override public void set(String key, String value) { mem.put(key, value); }

    @Override public String get(String key, String def) {
        String v = mem.get(key);
        return v == null ? def : v;
    }

    @Override public void theme(String from, String to) { log("[试跑] 换色 " + from + " → " + to); }
    @Override public void icon(String key, String value) { log("[试跑] 换图标 " + key); }
    @Override public void bg(String name, String value) { log("[试跑] 换背景 " + name); }
    @Override public void hide(String key) { log("[试跑] 隐藏入口 " + key); }
    @Override public void text(String key, String value) { log("[试跑] 文案 " + key + " = " + value); }
    @Override public void style(String key, String json) { log("[试跑] 样式 " + key + " = " + json); }
    @Override public void overlay(String id, com.cathelperawa.owo.app.ui.ScriptScreen.Handler h) {
        log("[试跑] 显示浮层 " + id + "（试跑不真显示）");
    }
    @Override public void overlayClose(String id) {
        log("[试跑] 关闭浮层 " + id);
    }
    @Override public void dialog(String title, com.cathelperawa.owo.app.ui.ScriptScreen.Handler h) {
        log("[试跑] 弹窗 " + title + "（试跑不真弹）");
    }
    @Override public void setting(String title, String pageKey) {
        log("[试跑] 设置页新增入口 " + title + " → " + pageKey);
    }
    @Override public void screen(String pageKey, com.cathelperawa.owo.app.ui.ScriptScreen.Handler h) {
        log("[试跑] 接管页面 " + pageKey + "（试跑不真显示）");
    }
    @Override public String needs(String id) { return "1.0.0"; }
    @Override public void strategy(String point, StrategyRegistry.Handler h) {
        log("[试跑] 注册策略 " + point + "（试跑不生效）");
    }
    @Override public String every(int s, ScriptTimer.Handler h) {
        log("[试跑] 注册定时 每 " + s + " 秒（试跑不执行）");
        return "mock";
    }
    @Override public String at(String hhmm, ScriptTimer.Handler h) {
        log("[试跑] 注册定时 每天 " + hhmm + "（试跑不执行）");
        return "mock";
    }
    @Override public boolean cancelTimer(String id) { return true; }

    @Override public java.util.List<String> findClasses(String keyword, int limit) {
        java.util.List<String> l = new java.util.ArrayList<String>();
        l.add("（试跑）com.example." + keyword + "Sample");
        return l;
    }
    @Override public java.util.List<String> methodsOf(String c, int limit) { return new java.util.ArrayList<String>(); }
    @Override public java.util.List<String> fieldsOf(String c, int limit) { return new java.util.ArrayList<String>(); }
    @Override public java.util.List<String> ctorsOf(String c) { return new java.util.ArrayList<String>(); }

    @Override public void method(String target, MethodHook.Handler h) {
        log("[试跑] 方法重写 " + target + "（试跑不生效）");
    }
    @Override public void method(String target, String mode, MethodHook.Handler h) {
        log("[试跑] 方法重写 " + target + " [" + mode + "]（试跑不生效）");
    }
    @Override public ScriptEngineHandle newEngine(String lang) {
        log("[试跑] 起子引擎 " + lang + "（试跑不生效）");
        return null;
    }
    @Override public String eval(String code, String lang) {
        log("[试跑] eval " + lang + "（试跑不生效）");
        return "";
    }
    @Override public String registerEngine(String suffix, String funcName) {
        log("[试跑] 注册引擎 " + suffix + " → " + funcName);
        return null;
    }
    @Override public String registerEngine(String suffix, String funcName, String forms) {
        log("[试跑] 注册引擎 " + suffix + " → " + funcName + " 形态=" + forms);
        return null;
    }
    @Override public String unregisterEngine(String suffix) { return null; }
    @Override public void page(String title, String sub, String key,
                               com.cathelperawa.owo.app.ui.ScriptScreen.Handler h) {
        log("[试跑] 新增页面 " + key + "「" + title + "」（试跑不生效）");
    }
    @Override public void onActivity(com.cathelperawa.owo.app.script.ActivityBus.Handler h) {
        log("[试跑] 订阅界面回调（试跑不生效）");
    }
    @Override public android.view.View contentRoot() { return null; }
    @Override public android.view.View decorView() { return null; }
    @Override public android.view.View findText(android.view.View root, String text) { return null; }
    @Override public android.view.View findByClass(android.view.View root, String cls) { return null; }
    @Override public android.view.View findByDesc(android.view.View root, String desc) { return null; }
    @Override public void walk(android.view.View root, ViewFinder.Handler h) { }
    @Override public void setPageBackground(android.app.Activity a, String p) {
        log("[试跑] 换背景 " + p + "（试跑不生效）");
    }
    @Override public String floatView(android.view.View v, int gravity) {
        log("[试跑] 加浮层（试跑不生效）");
        return null;
    }
    @Override public String floatView(android.view.View v, int gravity, int x, int y) {
        log("[试跑] 加浮层 x=" + x + " y=" + y + "（试跑不生效）");
        return null;
    }
    @Override public void closeFloat(String id) { }

    @Override public void runOnUi(Runnable r) {
        log("[试跑] runOnUi（试跑不生效）");
    }
    @Override public void patch(String point, PatchPoint.Handler h) {
        log("[试跑] 挂点位 " + point + "（试跑不生效）");
    }
    @Override public void unpatch(String point) { log("[试跑] 撤销点位 " + point); }
    @Override public void configForce(String key, String value) {
        log("[试跑] 强制配置 " + key + "=" + value + "（试跑不生效）");
    }
    @Override public void configClear(String key) { log("[试跑] 撤销配置 " + key); }
    @Override public void skinReset() { log("[试跑] 撤回外观改动"); }

    @Override public boolean sendReply(String kind, String peer, long id, String text) {
        log("[试跑] 引用回复 → " + text);
        return true;
    }
    @Override public boolean recall(String kind, String peer, long id) {
        log("[试跑] 撤回 " + id);
        return true;
    }
    @Override public boolean sendPtt(String kind, String peer, String path) {
        log("[试跑] 语音 " + path);
        return true;
    }
    @Override public boolean sendFace(String kind, String peer, int faceId) {
        log("[试跑] 表情 " + faceId);
        return true;
    }
    @Override public String at(String uid, String name) { return "[atUin=" + uid + "]" + name + " "; }
    @Override public String uinOf(String uid) { return ""; }
    @Override public String uidOf(String uin) { return ""; }
    @Override public String nameOf(String peer, String uin) { return "（试跑）"; }

    @Override public void strategy(String point, String funcName) {
        log("[试跑] 注册策略 " + point + " → 函数 " + funcName + "（试跑不生效）");
    }

    @Override public String httpAsync(String url, String optsJson, com.cathelperawa.owo.app.script.ScriptHttpHandler cb) {
        log("[试跑] 异步请求 " + url + "（试跑不真的发）");
        return null;
    }

    @Override public String httpStream(String url, String optsJson, com.cathelperawa.owo.app.script.ScriptHttpHandler cb) {
        log("[试跑] 流式请求 " + url + "（试跑不真的发）");
        return null;
    }

    private final java.util.Map<String, String> mockFiles =
            new java.util.HashMap<String, String>();

    @Override public boolean fileWrite(String name, String content) {
        mockFiles.put(name, content == null ? "" : content);
        log("[试跑] 写文件 " + name + "（" + (content == null ? 0 : content.length()) + " 字，不落盘）");
        return true;
    }
    @Override public boolean fileAppend(String name, String content) {
        String old = mockFiles.get(name);
        mockFiles.put(name, (old == null ? "" : old) + (content == null ? "" : content));
        log("[试跑] 追加 " + name + "（不落盘）");
        return true;
    }
    @Override public String fileRead(String name) {
        String v = mockFiles.get(name);
        log("[试跑] 读文件 " + name + "（" + (v == null ? 0 : v.length()) + " 字）");
        return v == null ? "" : v;
    }
    @Override public boolean fileExists(String name) { return mockFiles.containsKey(name); }
    @Override public boolean fileDelete(String name) { mockFiles.remove(name); return true; }
    @Override public boolean fileMkdir(String name) { return true; }
    @Override public boolean fileRename(String from, String to) {
        String v = mockFiles.remove(from);
        if (v != null) mockFiles.put(to, v);
        return v != null;
    }
    @Override public String[] fileList(String name) {
        return mockFiles.keySet().toArray(new String[0]);
    }
    @Override public long fileSize(String name) {
        String v = mockFiles.get(name);
        return v == null ? -1 : v.getBytes().length;
    }
    @Override public String dataDir() { return "/（试跑无数据目录）"; }
    @Override public void toast(String text) { log("[试跑] toast: " + text); }
    @Override public String exec(String cmd) { log("[试跑] exec: " + cmd); return ""; }
    @Override public boolean can(String cap) { return true; }
    @Override public android.content.Context context() { return null; }
    @Override public android.app.Activity activity() { return null; }

    
    @Override public String probe(String target) { return probe(target, null); }

    @Override public String probe(String target, String member) {
        try {
            ClassLoader cl = com.cathelperawa.owo.app.bridge.QQEnv.classLoader();
            if (cl == null) cl = MockScriptApi.class.getClassLoader();
            return ScriptReflect.probe(cl, target, member);
        } catch (Throwable t) {
            return "探查失败：" + t;
        }
    }

    @Override public Object tryCall(String className, String method, Object... args) {
        log("[试跑] tryCall: " + className + "#" + method);
        return null;
    }

    @Override public String tryCallEx(String className, String method, Object... args) {
        log("[试跑] tryCallEx: " + className + "#" + method);
        return "{\"ok\":false,\"err\":\"试跑模式不真正调用\"}";
    }

    @Override public ClassLoader loadDex(String rel) { return ScriptDex.load(rel); }

    @Override public Class loadDexClass(String rel, String className) {
        return ScriptDex.loadClass(rel, className);
    }

    @Override public ClassLoader loadJar(String rel) { return ScriptJar.load(rel); }

    @Override public Class defineClass(String className, byte[] classBytes) {
        return ScriptJar.defineClass(className, classBytes);
    }
}
