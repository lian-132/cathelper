package com.cathelperawa.owo.app.proto;

import android.content.Context;
import com.cathelperawa.owo.app.core.Bg;

import com.cathelperawa.owo.app.bridge.CatToast;
import com.cathelperawa.owo.app.bridge.CurrentChat;
import com.cathelperawa.owo.app.core.HookLog;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class FakeChat {

    private FakeChat() {
    }

    public interface OnArk {
        void run(String arkJson);
    }

    public interface OnFail {
        void run(String reason);
    }

    

    public static List<PbMsg.FakeItem> parse(String src) {
        List<PbMsg.FakeItem> out = new ArrayList<PbMsg.FakeItem>();
        if (src == null) return out;
        String[] lines = src.split("\\n");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i] == null ? "" : lines[i].trim();
            if (line.length() == 0) continue;
            if (line.startsWith("#")) continue;
            String[] p = line.split("\\|", 3);
            if (p.length >= 3) {
                long uin = parseLong(p[1].trim());
                String nick = p[0].trim();
                String text = p[2].trim();
                if (text.length() == 0) continue;
                out.add(new PbMsg.FakeItem(uin, nick, text));
            } else if (p.length == 2) {
                String text = p[1].trim();
                if (text.length() == 0) continue;
                out.add(new PbMsg.FakeItem(0, p[0].trim(), text));
            } else {
                
                out.add(new PbMsg.FakeItem(0, "", line));
            }
        }
        return out;
    }

    private static long parseLong(String s) {
        try {
            return Long.parseLong(s.replaceAll("[^0-9]", ""));
        } catch (Throwable t) {
            return 0L;
        }
    }

    

    public static void build(final Context c, String src, final OnArk ok, final OnFail fail) {
        build(c, parse(src), System.currentTimeMillis() / 1000L, ok, fail);
    }

    public static void build(final Context c, final List<PbMsg.FakeItem> items,
                             final long baseSec, final OnArk ok, final OnFail fail) {
        if (!CurrentChat.has()) {
            fail.run("先在聊天里点开一个会话喵");
            return;
        }
        if (items == null || items.isEmpty()) {
            fail.run("一条消息都没有");
            return;
        }
        if (items.size() > 60) {
            fail.run("太多了（最多 60 条）");
            return;
        }
        final int ct = CurrentChat.chatType();
        final String uid = CurrentChat.peerUid();
        CatToast.show("上传中…（" + items.size() + " 条）");

        Bg.run(new Runnable() {
            @Override
            public void run() {
                try {
                    byte[] multi = PbMsg.buildFakeMultiMsg(items, baseSec);
                    HookLog.i("FakeChat: MultiMsg len=" + multi.length
                            + " items=" + items.size());
                    String rid = PbSender.uploadMultiMsg(multi, ct, uid);
                    if (rid == null) {
                        fail.run("上传超时，没拿到 resid");
                        return;
                    }
                    if (rid.startsWith("ERR:")) {
                        fail.run(rid);
                        return;
                    }
                    String ark = arkOf(rid, items);
                    if (ark == null) {
                        fail.run("外框构造失败");
                        return;
                    }
                    ok.run(ark);
                } catch (Throwable t) {
                    HookLog.e("FakeChat 失败", t);
                    fail.run(String.valueOf(t));
                }
            }
        }, "fake-chat");
    }

    

    
    private static String arkOf(String resid, List<PbMsg.FakeItem> items) {
        try {
            String uniseq = UUID.randomUUID().toString();
            int n = items.size();

            JSONArray news = new JSONArray();
            int show = Math.min(n, 4);
            for (int i = 0; i < show; i++) {
                PbMsg.FakeItem it = items.get(i);
                JSONObject o = new JSONObject();
                String who = it.nick == null || it.nick.length() == 0 ? "" : it.nick + ": ";
                String body;
                if (it.brief != null && it.brief.length() > 0) {
                    body = it.brief;
                } else if (it.raw != null && it.raw.length > 0) {
                    body = "[" + typeLabel(it.type) + "]";
                } else if (it.text != null && it.text.length() > 0) {
                    body = it.text;
                } else {
                    body = "[消息]";
                }
                if (body.length() > 40) body = body.substring(0, 40) + "…";
                o.put("text", who + body);
                news.put(o);
            }

            String source = sourceOf(items);

            JSONObject o = new JSONObject();
            o.put("app", "com.tencent.multimsg");
            JSONObject cfg = new JSONObject();
            cfg.put("autosize", 1);
            cfg.put("forward", 1);
            cfg.put("round", 1);
            cfg.put("type", "normal");
            cfg.put("width", 300);
            o.put("config", cfg);
            o.put("desc", "[聊天记录]");
            o.put("extra", "{\"filename\":\"" + uniseq + "\",\"tsum\":" + n + "}\n");
            JSONObject meta = new JSONObject();
            JSONObject detail = new JSONObject();
            detail.put("news", news);
            detail.put("resid", resid);
            detail.put("source", source);
            detail.put("summary", "查看" + n + "条转发消息");
            detail.put("uniseq", uniseq);
            meta.put("detail", detail);
            o.put("meta", meta);
            o.put("prompt", "[聊天记录]");
            o.put("ver", "0.0.0.5");
            o.put("view", "contact");
            return o.toString();
        } catch (Throwable t) {
            HookLog.w("FakeChat arkOf: " + t);
            return null;
        }
    }

    private static String typeLabel(String type) {
        if (type == null) return "消息";
        if (type.equals(MediaVault.T_PIC)) return "图片";
        if (type.equals(MediaVault.T_FACE)) return "表情";
        if (type.equals(MediaVault.T_PTT)) return "语音";
        if (type.equals(MediaVault.T_VIDEO)) return "视频";
        if (type.equals(MediaVault.T_ARK)) return "卡片";
        if (type.equals(MediaVault.T_FILE)) return "文件";
        return "消息";
    }

    private static String sourceOf(List<PbMsg.FakeItem> items) {
        String a = null;
        String b = null;
        for (int i = 0; i < items.size(); i++) {
            String n = items.get(i).nick;
            if (n == null || n.length() == 0) continue;
            if (a == null) a = n;
            else if (!n.equals(a)) {
                b = n;
                break;
            }
        }
        if (a == null) return "聊天记录";
        if (b == null) return a + "的聊天记录";
        return a + "和" + b + "的聊天记录";
    }
}
