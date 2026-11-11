package com.cathelperawa.owo.app.script;

import com.cathelperawa.owo.app.core.Bg;
import com.cathelperawa.owo.app.core.HookLog;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class ScriptMenuRegistry {

    public static final String PREFIX = "⌁";

    public static final class Item {
        public final String script;
        public final String label;
        final ScriptMenuHandler handler;

        Item(String script, String label, ScriptMenuHandler handler) {
            this.script = script;
            this.label = label;
            this.handler = handler;
        }

        public String title() { return PREFIX + label; }
    }

    private static final List<Item> ITEMS = new CopyOnWriteArrayList<Item>();

    private ScriptMenuRegistry() {}

    public static String add(String script, String label, ScriptMenuHandler h) {
        if (label == null || label.trim().length() == 0) return "菜单名不能为空";
        if (label.length() > 8) return "菜单名最长 8 个字（长按菜单放不下）";
        if (h == null) return "没给点击回调";
        String lb = label.trim();

        Item it = new Item(script, lb, h);
        for (int i = 0; i < ITEMS.size(); i++) {
            Item o = ITEMS.get(i);
            if (o.script.equals(script) && o.label.equals(lb)) {
                ITEMS.set(i, it);       
                return null;
            }
        }
        if (countOf(script) >= 3) return "一个脚本最多 3 个菜单项";
        ITEMS.add(it);
        HookLog.i("[脚本菜单] " + script + " 注册「" + lb + "」");
        return null;
    }

    public static int removeByScript(String script) {
        if (script == null) return 0;
        List<Item> rm = new ArrayList<Item>();
        for (Item i : ITEMS) if (i.script.equals(script)) rm.add(i);
        int n = 0;
        for (Item i : rm) if (ITEMS.remove(i)) n++;
        return n;
    }

    public static void clear() { ITEMS.clear(); }

    public static List<Item> all() { return new ArrayList<Item>(ITEMS); }

    public static int size() { return ITEMS.size(); }

    public static int countOf(String script) {
        int n = 0;
        for (Item i : ITEMS) if (i.script.equals(script)) n++;
        return n;
    }

    public static Item find(String title) {
        if (title == null || !title.startsWith(PREFIX)) return null;
        String lb = title.substring(PREFIX.length());
        for (Item i : ITEMS) {
            if (i.label.equals(lb)) return i;
        }
        return null;
    }

    public static boolean dispatchFromHost(String title, Object rec) {
        if (find(title) == null) return false;

        String text = "";
        try {
            text = com.cathelperawa.owo.app.api.MsgApi.textOf(rec);
        } catch (Throwable ignore) {
        }
        String peer = "", peerName = "";
        boolean group = false;
        try {
            peer = com.cathelperawa.owo.app.bridge.CurrentChat.peerUid();
            peerName = com.cathelperawa.owo.app.bridge.CurrentChat.peerName();
            group = com.cathelperawa.owo.app.bridge.CurrentChat.chatType() == 2;
        } catch (Throwable ignore) {
        }
        ScriptMsg m = new ScriptMsg(peer, peerName, "", "", text, group,
                false, false, 0, System.currentTimeMillis());
        return dispatch(title, m);
    }

    public static boolean dispatch(final String title, final ScriptMsg msg) {
        final Item it = find(title);
        if (it == null) return false;
        Bg.run(new Runnable() {
            @Override public void run() {
                try {
                    ScriptExec.run(it.script, new Runnable() {
                        @Override public void run() {
                            it.handler.onClick(msg);
                        }
                    });
                } catch (Throwable t) {
                    HookLog.e("[脚本菜单] " + it.script + "." + it.label + " 失败", t);
                    
                    try {
                        final String msg = it.script + " 出错：" + t;
                        com.cathelperawa.owo.app.core.Bg.run(new Runnable() {
                            @Override public void run() {
                                try {
                                    com.cathelperawa.owo.app.bridge.CatToast.show(msg);
                                } catch (Throwable ignore) {
                                }
                            }
                        }, "cat-toast");
                    } catch (Throwable ignore) {
                    }
                }
            }
        }, "cat-script-menu");
        return true;
    }
}
