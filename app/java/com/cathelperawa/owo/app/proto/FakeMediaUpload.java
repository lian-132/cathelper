package com.cathelperawa.owo.app.proto;

import android.content.Context;
import com.cathelperawa.owo.app.core.Bg;

import com.cathelperawa.owo.app.bridge.CatToast;
import com.cathelperawa.owo.app.bridge.ContactFactory;
import com.cathelperawa.owo.app.bridge.ElementFactory;
import com.cathelperawa.owo.app.bridge.CurrentChat;
import com.cathelperawa.owo.app.bridge.MsgSender;
import com.cathelperawa.owo.app.bridge.QQEnv;
import com.cathelperawa.owo.app.core.HookConfig;
import com.cathelperawa.owo.app.core.HookLog;

import java.util.Collections;

public final class FakeMediaUpload {

    private FakeMediaUpload() {
    }

    public interface OnOk {
        void run(String elemJson);
    }

    public interface OnFail {
        void run(String reason);
    }

    public static void upload(final Context c, final String path,
                              final OnOk ok, final OnFail fail) {
        CatToast.show("上传图片中…");
        Bg.run(new Runnable() {
            @Override
            public void run() {
                try {
                    PbSender.setupSendDump();
                    Object el = ElementFactory.pic(path);
                    if (el == null) {
                        fail.run("图片元素构造失败");
                        return;
                    }

                    int ct;
                    String peer;
                    String selfUid = QQEnv.currentUid();
                    if (selfUid != null && selfUid.length() > 0) {
                        ct = 1;
                        peer = selfUid;
                        HookLog.i("FakeMedia: 目标=自己 uid=" + selfUid);
                    } else {
                    String curName = CurrentChat.peerName();
                    if (curName != null && curName.contains("文件传输")) {
                        ct = CurrentChat.chatType();
                        peer = CurrentChat.peerUid();
                        try {
                            HookConfig.set(c, "fh_ct", String.valueOf(ct));
                            HookConfig.set(c, "fh_uid", peer);
                        } catch (Throwable ignore) {
                        }
                        HookLog.i("FakeMedia: 记住文件传输助手 ct=" + ct + " uid=" + peer);
                    } else {
                        peer = HookConfig.get(c, "fh_uid", "");
                        String cts = HookConfig.get(c, "fh_ct", "1");
                        if (peer == null || peer.length() == 0) {
                            fail.run("先打开一次「文件传输助手」再来");
                            return;
                        }
                        try {
                            ct = Integer.parseInt(cts);
                        } catch (Throwable t) {
                            ct = 1;
                        }
                    }
                    }
                    Object contact = ContactFactory.make(ct, peer, "");
                    if (contact == null) {
                        fail.run("幽灵会话构造失败");
                        return;
                    }
                    PbSender.lastPicElemJson = null;
                    boolean sent = MsgSender.send(contact,
                            Collections.singletonList(el));
                    HookLog.i("FakeMedia: 幽灵发图 ok=" + sent + " path=" + path);
                    if (!sent) {
                        fail.run("发不出去（可能在群里被拦）");
                        return;
                    }
                    long deadline = System.currentTimeMillis() + 20000;
                    while (System.currentTimeMillis() < deadline) {
                        String js = PbSender.lastPicElemJson;
                        if (js != null && js.length() > 20) {
                            ok.run(js);
                            return;
                        }
                        try {
                            Thread.sleep(150);
                        } catch (Throwable ignore) {
                        }
                    }
                    fail.run("没抓到上传结果（20 秒）");
                } catch (Throwable t) {
                    HookLog.e("FakeMedia 失败", t);
                    fail.run(String.valueOf(t));
                }
            }
        }, "fake-media");
    }
}
