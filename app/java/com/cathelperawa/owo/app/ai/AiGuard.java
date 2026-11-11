package com.cathelperawa.owo.app.ai;

import com.cathelperawa.owo.app.api.EventApi;
import com.cathelperawa.owo.app.api.IdentityApi;

public final class AiGuard {

    private AiGuard() {}

    public static final String ACT_CONFIG = "改配置";
    public static final String ACT_FILE = "发/收文件";
    public static final String ACT_SYSTEM = "操作设备";
    public static final String ACT_SHELL = "执行命令";

    public static boolean allow(EventApi.Message m, String action) {
        if (m == null) return false;
        String uid = m.senderUid;
        String who = safeName(m);
        if (AiConfig.isSelfUid(uid)) {
            AiDebug.log("  ✓ 危险功能放行（号主）: " + action);
            return true;
        }
        String uin = uinOf(uid);
        boolean ok = AiConfig.isAdminUin(uin) || AiConfig.isAdminUid(uid);
        if (ok) {
            AiDebug.log("  ✓ 危险功能放行（管理员 " + who + "）: " + action);
        } else {
            AiDebug.log("  ✗ 拒绝危险功能: " + action + " ← " + who
                    + "（" + (uin == null || uin.length() == 0 ? uid : uin) + " 不是管理员）");
        }
        return ok;
    }

    public static boolean allowUid(String uid, String action) {
        if (AiConfig.isSelfUid(uid)) return true;
        boolean ok = AiConfig.isAdminUid(uid);
        AiDebug.log((ok ? "  ✓ 危险功能放行（管理员 " : "  ✗ 拒绝危险功能（非管理员）")
                + uid + "）: " + action);
        return ok;
    }

    public static boolean noAdmin() {
        return AiConfig.adminCount() == 0;
    }

    private static String uinOf(String uid) {
        try {
            return IdentityApi.uinFromUid(uid);
        } catch (Throwable t) {
            return "";
        }
    }

    private static String safeName(EventApi.Message m) {
        try {
            String n = m.senderName;
            if (n != null && n.trim().length() > 0) return n.trim();
            String uin = uinOf(m.senderUid);
            return (uin == null || uin.length() == 0) ? m.senderUid : uin;
        } catch (Throwable t) {
            return "?";
        }
    }
}
