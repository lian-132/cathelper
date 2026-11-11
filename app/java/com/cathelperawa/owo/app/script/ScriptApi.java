package com.cathelperawa.owo.app.script;

public interface ScriptApi {

    

    
    void log(String s);

    String me();

    String myName();

    long now();

    String scriptName();

    boolean hasCap(String cap);

    String ai(String prompt);

    

    
    void route(String id, String[] match, ScriptHandler h);

    void tool(String name, String desc, ScriptToolHandler h);

    void floatBall(String id, String text, ScriptFloatHandler h);

    void floatTheme(String id, String spec);

    void floatMenuItem(String ballId, String itemId, String label, ScriptFloatHandler h);

    

    
    void hub(String title, ScriptFloatHandler h);

    void hubFirst(String title, ScriptFloatHandler h);

    void hubDown(String title, ScriptFloatHandler h);

    void hubItem(String label, ScriptFloatHandler h);

    void hubIcon(String icon);

    void hubOut();

    

    
    void provide(String name, ScriptServiceHandler h);

    String call(String name, String args);

    String[] services();

    void emit(String event, String data);

    void on(String event, ScriptBusHandler h);

    void hook(String event, ScriptBusHandler h);

    void menu(String label, ScriptMenuHandler h);

    void onMessage(ScriptMessageHandler h);

    void tag(String name, ScriptTagHandler h);

    void panel(String title, String specJson, ScriptPanelHandler h);

    void panelPage(String title, com.cathelperawa.owo.app.ui.ScriptScreen.Handler h);

    void onStop(Runnable r);

    

    
    boolean sendText(String kind, String peer, String text);

    boolean sendPic(String kind, String peer, String path);

    boolean sendCard(String kind, String peer, String arkJson);

    boolean sendVoice(String kind, String peer, String path);

    boolean sendFile(String kind, String peer, String path);

    

    
    boolean mute(String group, String uid, int seconds);

    boolean kick(String group, String uid);

    

    
    String httpGet(String url);

    String httpPost(String url, String jsonBody);

    String httpAsync(String url, String optsJson, ScriptHttpHandler cb);

    String httpStream(String url, String optsJson, ScriptHttpHandler cb);

    

    
    boolean fileWrite(String name, String content);
    
    boolean fileAppend(String name, String content);
    
    String fileRead(String name);
    
    boolean fileExists(String name);
    
    boolean fileDelete(String name);
    
    boolean fileMkdir(String name);
    
    boolean fileRename(String from, String to);
    
    String[] fileList(String name);
    
    long fileSize(String name);
    
    String dataDir();

    

    

    
    boolean sendReply(String kind, String peer, long replyMsgId, String text);
    
    boolean recall(String kind, String peer, long msgId);
    
    boolean sendPtt(String kind, String peer, String path);
    
    boolean sendFace(String kind, String peer, int faceId);
    
    String at(String uid, String name);
    
    String uinOf(String uid);
    
    String uidOf(String uin);
    
    String nameOf(String peerOrGroup, String uin);

    void toast(String text);
    
    boolean can(String cap);
    
    String exec(String cmd);

    android.content.Context context();
    
    android.app.Activity activity();

    

    
    String peerName(String peer);

    

    
    String get(String key);

    void set(String key, String value);

    String get(String key, String def);

    
    int getInt(String key, int def);

    void setInt(String key, int v);

    boolean getBool(String key, boolean def);

    void setBool(String key, boolean v);

    

    
    void theme(String from, String to);

    void icon(String key, String value);

    void bg(String name, String value);

    void hide(String key);

    void text(String key, String value);

    void style(String key, String json);

    void screen(String pageKey, com.cathelperawa.owo.app.ui.ScriptScreen.Handler h);

    void dialog(String title, com.cathelperawa.owo.app.ui.ScriptScreen.Handler h);

    void setting(String title, String pageKey);

    void overlay(String id, com.cathelperawa.owo.app.ui.ScriptScreen.Handler h);

    void overlayClose(String id);

    void configForce(String key, String value);

    void configClear(String key);

    void patch(String point, PatchPoint.Handler h);

    void unpatch(String point);

    

    
    String every(int seconds, ScriptTimer.Handler h);

    String at(String hhmm, ScriptTimer.Handler h);

    boolean cancelTimer(String id);

    void strategy(String point, StrategyRegistry.Handler h);

    void strategy(String point, String funcName);

    

    
    java.util.List<String> findClasses(String keyword, int limit);

    java.util.List<String> methodsOf(String className, int limit);

    java.util.List<String> fieldsOf(String className, int limit);

    java.util.List<String> ctorsOf(String className);

    void method(String target, MethodHook.Handler h);

    void method(String target, String mode, MethodHook.Handler h);

    ScriptEngineHandle newEngine(String lang);

    String eval(String code, String lang);

    String registerEngine(String suffix, String funcName);

    String registerEngine(String suffix, String funcName, String forms);

    String unregisterEngine(String suffix);

    void page(String title, String sub, String key,
              com.cathelperawa.owo.app.ui.ScriptScreen.Handler h);

    void onActivity(com.cathelperawa.owo.app.script.ActivityBus.Handler h);

    void runOnUi(Runnable r);

    
    android.view.View contentRoot();

    android.view.View decorView();

    android.view.View findText(android.view.View root, String text);

    android.view.View findByClass(android.view.View root, String cls);

    android.view.View findByDesc(android.view.View root, String desc);

    void walk(android.view.View root, com.cathelperawa.owo.app.script.ViewFinder.Handler h);

    void setPageBackground(android.app.Activity a, String pathOrColor);

    String floatView(android.view.View v, int gravity);

    String floatView(android.view.View v, int gravity, int x, int y);

    void closeFloat(String id);

    String needs(String id);

    void skinReset();

    

    
    String probe(String target);

    String probe(String target, String member);

    Object tryCall(String className, String method, Object... args);

    String tryCallEx(String className, String method, Object... args);

    

    
    ClassLoader loadDex(String rel);

    Class loadDexClass(String rel, String className);

    ClassLoader loadJar(String rel);

    Class defineClass(String className, byte[] classBytes);
}
