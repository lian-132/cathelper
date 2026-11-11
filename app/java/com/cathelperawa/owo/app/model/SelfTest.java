package com.cathelperawa.owo.app.model;

public final class SelfTest {

    private SelfTest() {}

    private static int pass;
    private static int fail;
    private static final StringBuilder LOG = new StringBuilder();

    private static void check(String what, boolean cond) {
        if (cond) {
            pass++;
            LOG.append("  ✓ ").append(what).append('\n');
        } else {
            fail++;
            LOG.append("  ✗ ").append(what).append("   ← 失败\n");
        }
    }

    public static String run() {
        pass = 0;
        fail = 0;
        LOG.setLength(0);
        LOG.append("══ 阶段 D-a 数据结构自检 ══\n\n");

        LOG.append("【ChatType】\n");
        check("GROUP.code == 2（唯一真相源）", ChatType.GROUP.code == 2);
        check("FRIEND.code == 1", ChatType.FRIEND.code == 1);
        check("of(2) == GROUP", ChatType.of(2) == ChatType.GROUP);
        check("of(1) == FRIEND", ChatType.of(1) == ChatType.FRIEND);
        check("of(0) == UNKNOWN（不猜方向）", ChatType.of(0) == ChatType.UNKNOWN);
        check("of(999) == UNKNOWN", ChatType.of(999) == ChatType.UNKNOWN);
        check("GROUP.isGroup()", ChatType.GROUP.isGroup());
        check("FRIEND.isGroup() == false", !ChatType.FRIEND.isGroup());
        check("UNKNOWN.isKnown() == false", !ChatType.UNKNOWN.isKnown());
        LOG.append('\n');

        LOG.append("【CatChannel】\n");
        check("of(\"direct\") == DIRECT（兼容旧字符串）", CatChannel.of("direct") == CatChannel.DIRECT);
        check("of(\"card\") == CARD", CatChannel.of("card") == CatChannel.CARD);
        check("of((String)null) == null", CatChannel.of((String) null) == null);
        check("of(\"不认识\") == null", CatChannel.of("不认识") == null);
        LOG.append('\n');

        LOG.append("【CatResult · 成功】\n");
        CatResult okK = CatResult.ok(CatChannel.KERNEL);
        check("I1: ok(ch) 后 fail == OK", okK.fail() == CatResult.Fail.OK);
        check("I1: ok(ch) 后 channel != null", okK.channel() == CatChannel.KERNEL);
        check("ok() == true", okK.ok());
        check("failed() == false", !okK.failed());

        CatResult okId = CatResult.ok(CatChannel.DIRECT, 12345L);
        check("I4: ok(ch, msgId) 时 ok() 为真", okId.ok());
        check("I4: msgId 正确保存", okId.msgId() == 12345L);

        CatResult okNote = CatResult.ok(CatChannel.LONG, "cmd=Xyz");
        check("ok(ch, note) 带说明", "cmd=Xyz".equals(okNote.note()));

        CatResult okPlain = CatResult.success();
        check("success() 无通道也 ok", okPlain.ok());
        LOG.append('\n');

        LOG.append("【CatResult · 失败】\n");
        CatResult bad = CatResult.badArgs("peerUid 为空");
        check("I2: 失败时 fail != OK", bad.fail() == CatResult.Fail.BAD_ARGS);
        check("失败时 channel == null", bad.channel() == null);
        check("ok() == false", !bad.ok());

        CatResult boo = CatResult.hostMissing("MsgService.sendMsg");
        check("hostMissing 分类正确", boo.fail() == CatResult.Fail.HOST_MISSING);
        check("hostMissing 的说明含定位信息",
                boo.note() != null && boo.note().contains("MsgService.sendMsg"));

        CatResult nr = CatResult.notReady("TimHb");
        check("notReady 分类正确", nr.fail() == CatResult.Fail.NOT_READY);

        CatResult to = CatResult.timeout("红包回包");
        check("timeout 分类正确", to.fail() == CatResult.Fail.TIMEOUT);

        CatResult rej = CatResult.rejected("服务端说不行");
        check("rejected 分类正确", rej.fail() == CatResult.Fail.REJECTED);

        CatResult uns = CatResult.unsupported("该元素本地发不了");
        check("unsupported 分类正确", uns.fail() == CatResult.Fail.UNSUPPORTED);

        Exception ex = new RuntimeException("底层炸了");
        CatResult unk = CatResult.unknown("兜底", ex);
        check("I3: unknown 带 cause", unk.cause() == ex);
        check("I3: unknown 带 note", unk.note() != null);
        check("cause() 可取出原始异常", unk.cause().getMessage().equals("底层炸了"));
        LOG.append('\n');

        LOG.append("【CatResult · shortReason】\n");
        String sr1 = boo.shortReason();
        check("失败 shortReason 含分类名", sr1.contains("HOST_MISSING"));
        check("失败 shortReason 含说明", sr1.contains("MsgService"));
        String sr2 = okK.shortReason();
        check("成功 shortReason 含 OK", sr2.contains("OK"));
        LOG.append('\n');

        LOG.append("【CatResult · 组合】\n");
        CatResult noted = bad.withNote("上下文补充");
        check("withNote 保留原分类", noted.fail() == CatResult.Fail.BAD_ARGS);
        check("withNote 拼接说明",
                noted.note() != null && noted.note().contains("上下文补充")
                        && noted.note().contains("peerUid"));
        check("withNote 不改成败", !noted.ok());

        CatResult fallback = CatResult.success();
        check("orElseDefault: 失败→替换",
                bad.orElseDefault(fallback) == fallback);
        check("orElseDefault: 成功→保留",
                okK.orElseDefault(fallback) == okK);

        final boolean[] ran = {false};
        bad.orElse(new Runnable() {
            @Override public void run() { ran[0] = true; }
        });
        check("orElse: 失败时执行兜底", ran[0]);

        final boolean[] ran2 = {false};
        okK.orElse(new Runnable() {
            @Override public void run() { ran2[0] = true; }
        });
        check("orElse: 成功时不执行兜底", !ran2[0]);

        boolean threw = false;
        try {
            bad.orThrow();
        } catch (CatApiException e) {
            threw = true;
            check("orThrow 异常携带 result", e.result() == bad);
            check("orThrow 异常分类正确", e.fail() == CatResult.Fail.BAD_ARGS);
        }
        check("orThrow: 失败时抛出", threw);

        boolean threwOk = false;
        try {
            okK.orThrow();
        } catch (Throwable t) {
            threwOk = true;
        }
        check("orThrow: 成功时不抛", !threwOk);

        check("toBoolean 投影", bad.toBoolean() == false && okK.toBoolean());
        LOG.append('\n');

        LOG.append("【CatPeer】\n");
        CatPeer g1 = CatPeer.group("123456789");
        check("P2: group.uid == 群号", "123456789".equals(g1.uid()));
        check("P2: group.uin == 群号", "123456789".equals(g1.uin()));
        check("group.chatType == GROUP", g1.chatType() == ChatType.GROUP);

        CatPeer g2 = CatPeer.group("123456789");
        check("P5: 值语义（equals）", g1.equals(g2));
        check("P5: 值语义（hashCode）", g1.hashCode() == g2.hashCode());
        CatPeer g3 = CatPeer.group("999999999");
        check("P5: 不同群不相等", !g1.equals(g3));

        boolean blankThrew = false;
        try {
            CatPeer.group("");
        } catch (IllegalArgumentException e) {
            blankThrew = true;
        }
        check("P1: 空群号抛异常", blankThrew);

        CatPeer f = CatPeer.friend("u_abcd1234");
        check("friend.chatType == FRIEND", f.chatType() == ChatType.FRIEND);
        check("friend(uid).uid 保持原值", "u_abcd1234".equals(f.uid()));

        boolean unknownThrew = false;
        try {
            CatPeer.of(0, "x");
        } catch (IllegalArgumentException e) {
            unknownThrew = true;
        }
        check("M3: of(0, x) 抛异常（不猜方向）", unknownThrew);

        CatPeer ofGroup = CatPeer.of(2, "123456789");
        check("of(2, 群号) 得到群会话", ofGroup.chatType() == ChatType.GROUP);
        LOG.append('\n');

        LOG.append("────────────────\n");
        LOG.append("通过 ").append(pass).append(" 项");
        if (fail > 0) LOG.append("，失败 ").append(fail).append(" 项 ✗");
        else LOG.append("，无失败 ✓");
        LOG.append('\n');
        return LOG.toString();
    }
}
