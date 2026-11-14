package com.cathelperawa.owo.app.proto;

import android.content.Context;
import com.cathelperawa.owo.app.core.Bg;

import com.cathelperawa.owo.app.bridge.CatToast;
import com.cathelperawa.owo.app.bridge.CurrentChat;
import com.cathelperawa.owo.app.core.HookLog;

public final class MultiMsgProbe {

    private MultiMsgProbe() {
    }

    public static void run(final Context c) {
        if (!CurrentChat.has()) {
            CatToast.show("先打开一个会话再试喵");
            return;
        }
        final int ct = CurrentChat.chatType();
        final String uid = CurrentChat.peerUid();
        CatToast.show("探针启动，最多等 20 秒…");
        HookLog.i("MultiMsgProbe: 开始 ct=" + ct + " uid=" + uid);

        Bg.run(new Runnable() {
            @Override
            public void run() {
                String r = PbSender.probeUpload(ct, uid,
                        "探针 " + System.currentTimeMillis());
                String msg;
                if (r == null) {
                    msg = "✗ 超时：SsoSendLongMsg 没回包";
                    HookLog.w("MultiMsgProbe: 超时");
                } else if (r.startsWith("ERR:")) {
                    msg = "✗ " + r;
                    HookLog.w("MultiMsgProbe: " + r);
                } else {
                    String shortId = r.length() > 16 ? r.substring(0, 16) + "…" : r;
                    msg = "✓ 通了！resid=" + shortId;
                    HookLog.i("MultiMsgProbe: 成功 resid=" + r);
                }
                CatToast.show(msg);
            }
        }, "msg-probe");
    }
}
