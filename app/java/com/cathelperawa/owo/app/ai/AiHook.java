package com.cathelperawa.owo.app.ai;

import com.cathelperawa.owo.app.api.EventApi;
import com.cathelperawa.owo.app.core.HookItem;
import com.cathelperawa.owo.app.core.HookItemBase;
import com.cathelperawa.owo.app.core.HookLog;

@HookItem(name = "AI 自动回复", path = "群管理/AI",
        description = "收到消息自动用大模型回复", order = 30)
public class AiHook extends HookItemBase {

    private static int AICACHE_LOG = 0;

    private static volatile boolean subscribed = false;

    @Override
    public String switchKey() {
        return "ai_enable";
    }

    @Override
    public void entry(ClassLoader cl) {
        hint("收到消息时");

        if (subscribed) {
            HookLog.i("AI 自动回复：已订阅过，跳过重复注册");
            return;
        }
        subscribed = true;

        try {
            com.cathelperawa.owo.app.protocol.RKeyHolder.install(cl);
        } catch (Throwable t) {
            HookLog.w("RKeyHolder 安装异常 " + t);
        }

        try {
            com.cathelperawa.owo.app.api.TaskApi.after(20000, new Runnable() {
                @Override public void run() {
                    try {
                        com.cathelperawa.owo.app.ai.AiStore.decay(30);
                    } catch (Throwable ignore) {
                    }
                }
            });
        } catch (Throwable ignore) {
        }

        try {
            com.cathelperawa.owo.app.api.TaskApi.after(8000, new Runnable() {
                @Override public void run() {
                    AiEngine.probeToolsIfNeeded();
                }
            });
        } catch (Throwable ignore) {
        }

        EventApi.onMessage(new EventApi.Listener() {
            @Override
            public void onMessage(EventApi.Message m) {
                try {
                    AiDebug.log("事件 " + (m.group ? "群" : "私") + " peer=" + m.peerUid
                            + " self=" + m.self + " atMe=" + m.atMe
                            + " text=" + m.text);

                    com.cathelperawa.owo.app.protocol.MsgCache.put(m.msgId, m.msgSeq, m.raw);
                    if (m.msgId == 0 || AICACHE_LOG++ % 50 == 0) {
                        AiDebug.log("  · MsgCache: 存入 msgId=" + m.msgId
                                + " seq=" + m.msgSeq
                                + " 共 " + com.cathelperawa.owo.app.protocol.MsgCache.size()
                                + "/" + com.cathelperawa.owo.app.protocol.MsgCache.seqSize() + " 条");
                    }

                    rememberVoice(m);
                    AiEngine.onMessage(m);

                    AiEngine.maybeProactive(m);
                } catch (Throwable t) {
                    HookLog.e("AI 事件处理异常", t);
                }
            }
        });
        HookLog.i("AI 自动回复：已订阅事件总线");
    }

    private static void rememberVoice(EventApi.Message m) {
        try {
            com.cathelperawa.owo.app.protocol.PttGrabber.Source s =
                    com.cathelperawa.owo.app.protocol.PttGrabber.fromRecord(m.raw);
            if (s == null) return;
            if (s.path == null && s.md5 == null && s.fid == null) return;
            com.cathelperawa.owo.app.protocol.PttIndex.put(
                    m.msgId, m.msgSeq, s.path, s.md5, s.fid);
            HookLog.i("语音索引: msgId=" + m.msgId + " seq=" + m.msgSeq
                    + " self=" + m.self + " 共 "
                    + com.cathelperawa.owo.app.protocol.PttIndex.size() + " 条");
        } catch (Throwable t) {
            HookLog.w("语音索引记录失败 " + t);
        }
    }

    @Override
    protected void check(com.cathelperawa.owo.app.core.SelfCheckResult.Checker c) {
        boolean hasProvider = false;
        String model = "";
        try {
            AiProvider p = AiProvider.current();
            hasProvider = p != null;
            if (p != null) model = p.model == null ? "" : p.model;
        } catch (Throwable ignore) {
        }
        c.assertThat("已配置提供商", hasProvider,
                hasProvider ? ("模型 " + (model.isEmpty() ? "(未填)" : model))
                        : "去「猫娘 AI → 模型提供商」配一个");

        Class<?> vb = c.cls("消息服务内部类",
                "com.tencent.qqnt.msg.MsgService$c");
        c.assertThat("收消息钩子", vb != null,
                vb != null ? "存在" : "找不到 MsgService$c（事件总线收不到消息）");

        c.assertThat("事件总线监听", EventApi.listenerCount() > 0,
                "监听器 " + EventApi.listenerCount() + " 个");
    }
}
