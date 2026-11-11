package com.cathelperawa.owo.app.feature;

import android.content.Context;

import com.cathelperawa.owo.app.MsgStore;
import com.cathelperawa.owo.app.api.EventApi;
import com.cathelperawa.owo.app.core.HookApi;
import com.cathelperawa.owo.app.core.HookItem;
import com.cathelperawa.owo.app.core.HookItemBase;
import com.cathelperawa.owo.app.core.HookLog;
import com.cathelperawa.owo.app.util.Refl;

import java.lang.reflect.Method;
import java.util.List;

@HookItem(name = "消息记录", path = "聊天与消息/消息记录", description = "记录收发消息到控制台", order = 20)
public class FeatureRecorder extends HookItemBase {

    @Override
    public String switchKey() { return "hook_autocap"; }

    @Override
    public void entry(ClassLoader cl) {
        hint("收发消息时");   
        Class<?> target = HookApi.findClass(cl,
                "com.tencent.qqnt.msg.MsgService$c",
                "com.tencent.qqnt.msg.MsgService$a");
        if (target == null) {
            fail("未找到 MsgService 内部类，消息记录不可用");
            return;
        }
        hookMethod(target, "onRecvMsg", true);
        hookMethod(target, "onAddSendMsg", false);
    }

    @Override
    protected void check(com.cathelperawa.owo.app.core.SelfCheckResult.Checker c) {
        Class<?> svc = c.cls("MsgService 内部类",
                "com.tencent.qqnt.msg.MsgService$c",
                "com.tencent.qqnt.msg.MsgService$a");
        c.method("onRecvMsg(收)", svc, "onRecvMsg");
        c.method("onAddSendMsg(发)", svc, "onAddSendMsg");
    }

    private void hookMethod(Class<?> cls, final String name, final boolean recv) {
        for (final Method m : cls.getDeclaredMethods()) {
            if (!name.equals(m.getName())) continue;
            hookAfter(m, new com.cathelperawa.owo.app.core.HookApi.Action() {
                @Override
                public void call(de.robv.android.xposed.XC_MethodHook.MethodHookParam p) {

                    
                    
                    final boolean enabled = isEnabled();   
                    Object rec = p.args[0];
                    if (rec instanceof List) {

                        
                        List<?> l = (List<?>) rec;
                        int n = 0;
                        for (Object one : l) {
                            if (one == null) continue;
                            EventApi.dispatch(one, recv);
                            if (enabled) record(one, recv);
                            n++;
                        }
                        if (enabled && n > 0) fire(recv ? "收消息" : "发消息", n);   
                    } else if (rec != null) {
                        EventApi.dispatch(rec, recv);
                        if (enabled) {
                            record(rec, recv);
                            fire(recv ? "收消息" : "发消息");
                        }
                    }
                }
            });
            HookLog.i("已挂 " + name + (recv ? "（收）" : "（发）"));
            break;
        }
    }

    private static void record(Object msgRecord, boolean recv) {
        try {
            String peer = Refl.strOr(Refl.field(msgRecord, "peerUid"),
                    Refl.strOr(Refl.field(msgRecord, "peerUin"), null));
            String sender = Refl.strOr(Refl.field(msgRecord, "senderUid"),
                    Refl.strOr(Refl.field(msgRecord, "senderUin"), null));
            String nick = Refl.strOr(Refl.field(msgRecord, "sendNickName"),
                    Refl.strOr(Refl.field(msgRecord, "sendMemberName"), ""));

            boolean isGroup = peer != null && !peer.startsWith("u_");

            

            String peerName;
            if (isGroup) {
                peerName = Refl.strOr(Refl.field(msgRecord, "peerName"), nick);
                
                if (peerName == null || peerName.trim().isEmpty()
                        || peerName.matches("\\d{5,}")) {
                    String real = null;
                    try {
                        real = com.cathelperawa.owo.app.bridge.TroopApi.getTroopName(peer);
                    } catch (Throwable ignore) {
                    }
                    if (real != null && real.trim().length() > 0
                            && !real.trim().equals(peer)) {
                        peerName = real;
                    }
                }
            } else {
                peerName = null;
                try {
                    peerName = com.cathelperawa.owo.app.api.IdentityApi.buddyNameByUid(peer);
                } catch (Throwable ignore) {
                }
                if (peerName == null || peerName.trim().isEmpty()
                        || peerName.equals(peer)) {
                    peerName = nick;   
                }
            }
            if (peer != null) MsgStore.notePeer(peer, peerName, isGroup);

            String text = extractText(msgRecord);
            String line = (recv ? "[收] " : "[发] ")
                    + "peer=" + peer
                    + " from=" + sender
                    + (nick != null && nick.length() > 0 && !"null".equals(nick) ? "(" + nick + ")" : "")
                    + " :: " + text;
            MsgStore.add(line);
        } catch (Throwable t) {
            HookLog.e("解析消息失败", t);
        }
    }

    public static String extractText(Object msgRecord) {
        StringBuilder sb = new StringBuilder();
        Object els = Refl.field(msgRecord, "elements");
        if (!(els instanceof List)) return "";
        for (Object el : (List<?>) els) {
            if (el == null) continue;
            Object te = Refl.field(el, "textElement");
            if (te != null) {
                Object c = Refl.field(te, "content");
                if (c instanceof String) sb.append((String) c);
                continue;
            }
            Object pic = Refl.field(el, "picElement");
            if (pic != null) {
                Object fn = Refl.field(pic, "fileName");
                sb.append("[图片").append(fn == null ? "" : ":" + fn).append("]");
                continue;
            }
            Object f = Refl.field(el, "fileElement");
            if (f != null) {
                Object fn = Refl.field(f, "fileName");
                sb.append("[文件").append(fn == null ? "" : ":" + fn).append("]");
                continue;
            }
            Object ark = Refl.field(el, "arkElement");
            if (ark != null) {
                sb.append("[卡片]");
                continue;
            }
            Object ptt = Refl.field(el, "pttElement");
            if (ptt != null) {
                sb.append("[语音]");
                continue;
            }
            Object vd = Refl.field(el, "videoElement");
            if (vd != null) {
                sb.append("[视频]");
                continue;
            }
            Object mf = Refl.field(el, "marketFaceElement");
            if (mf != null) {
                sb.append("[表情]");
            }
        }
        return sb.toString();
    }
}
