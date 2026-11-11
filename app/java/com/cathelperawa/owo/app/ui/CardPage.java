package com.cathelperawa.owo.app.ui;

import android.app.Activity;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

public final class CardPage extends Page {

    private static final java.util.Set<String> TODO_KEYS = new java.util.HashSet<String>(
            java.util.Arrays.asList("card_qqsign", "card_button", "card_bot_news", "card_bot_button", "card_rtc", "card_pic", "card_gc_exchange", "card_gc_rank", "card_channel", "card_qzone_block", "card_contact", "card_teamup", "card_mannounce"));

    private static final String[] GROUPS =
            {"消息与图文", "小程序", "游戏中心", "音视频", "群与名片", "工具箱"};

    private static final String[][] CARDS = {
            
            {"card_news", "GetNewsArk", "标准 News 卡，标题+摘要+跳转", "消息与图文"},
            {"card_news_max", "GetNewsArkMax", "不受白名单限制的 News 卡", "消息与图文"},
            {"card_news_plus", "GetNewsArkPlus", "JumpUrl 顶级域名是 qq.com 时只需 AppId", "消息与图文"},
            {"card_news_noverify", "获取未验证 News 卡", "不带验签信息的 News 卡", "消息与图文"},
            {"card_tuwen", "获取图文卡", "标题 + 图片 + 描述三件套", "消息与图文"},
            {"card_qqsign", "GetQQSignArk", "带签名的自定义卡片", "消息与图文"},
            {"card_button", "GetButtonArk", "只有一行按钮的极简卡", "消息与图文"},
            {"card_bot_news", "获取机器人 News 卡片", "以机器人身份出现的 News 卡", "消息与图文"},
            {"card_bot_button", "获取机器人加小按钮卡片", "机器人卡片 + 底部小按钮", "消息与图文"},

            {"card_mini", "GetMiniArk", "普通小程序分享卡", "小程序"},
            {"card_mini_plus", "GetMiniArkPlus", "填小程序 AppId 即可签名", "小程序"},
            {"card_mini_game", "GetMiniGameArk", "小游戏分享样式", "小程序"},
            {"card_wx_mini", "GetWxMiniArk", "微信小程序样式（借 QQ 通道）", "小程序"},
            {"card_mini_pic", "获取小程序图卡", "小程序 + 大图预览", "小程序"},

            {"card_gameshare", "获取 GameShare 卡片", "游戏分享通用卡（走 SSO 签名）", "游戏中心"},
            {"card_cf", "获取穿越火线 ARK", "CF 活动分享卡", "游戏中心"},
            {"card_dzdp", "获取大众点评卡片", "点评商户分享样式", "游戏中心"},
            {"card_dnf", "获取地下城卡", "DNF 活动分享卡", "游戏中心"},
            {"card_kapu", "获取卡噗卡片", "卡噗社区分享样式", "游戏中心"},
            {"card_wzry", "获取王者荣耀卡片", "战绩面板 + 角色信息", "游戏中心"},
            {"card_gc_exchange", "获取游戏中心互换卡片", "游戏中心道具互换", "游戏中心"},
            {"card_gc_rank", "获取游戏中心排名卡片", "排行榜样式", "游戏中心"},
            {"card_egame", "GetEgameArk", "企鹅电竞风格（视频卡同款）", "游戏中心"},
            {"card_sy", "GetSyArk", "Sy 活动组队卡", "游戏中心"},

            {"card_music", "GetMusicArk", "歌曲名 / 封面 / 播放按钮", "音视频"},
            {"card_rtc", "获取音视频卡", "语音/视频通话邀请样式", "音视频"},
            {"card_pic", "GetPicArk", "把图片发成 Ark 消息", "音视频"},

            {"card_contact", "获取名片卡", "推荐联系人 / 名片", "群与名片"},
            {"card_teamup", "获取群报名卡", "群活动报名 / 组队", "群与名片"},
            {"card_mannounce", "获取群公告卡", "群公告样式卡片", "群与名片"},
            {"card_channel", "GetChannelArk", "QQ 频道分享样式", "群与名片"},

            {"card_super_tool", "超级 ARK 工具箱", "全部卡片的总入口面板", "工具箱"},
            {"card_sign_many", "SignManyArk", "一次签多张卡", "工具箱"},
            {"card_qzone_block", "拦截 QQ 空间 ARK", "9.2.0 时代的功能，已失效", "工具箱"},
    };

    public CardPage(Host host) { super(host); }

    public static int count() { return CARDS.length; }

    public static String[] groups() { return GROUPS; }

    public static String[][] all() { return CARDS; }

    @Override
    public View build() {
        View[] out = new View[2];
        LinearLayout root = pageRoot("卡片模板", "ARK 卡片 · 逐个启用", out);

        LinearLayout panel = new LinearLayout(ctx);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(12), dp(4), dp(12), dp(4));

        panel.addView(buildGuildCardEntry());

        String curGroup = "";
        for (String[] c : CARDS) {
            if (!c[3].equals(curGroup)) {
                curGroup = c[3];
                TextView gt = text(curGroup, 12, 0xFF8FA3BE, true, dp(4));
                gt.setPadding(dp(4), dp(14), 0, dp(7));
                panel.addView(gt);
            }
            panel.addView(buildRow(c));
        }

        root.addView(panel);
        fadeIn(root);
        return out[0];
    }

    private View buildGuildCardEntry() {
        FrameLayout card = whiteFrame(dp(16), 0xFFE6F0FF);
        card.setClipToOutline(true);

        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(14), dp(1), dp(14), dp(1));

        LinearLayout texts = new LinearLayout(ctx);
        texts.setOrientation(LinearLayout.VERTICAL);
        texts.addView(text("频道帖子卡", 14.5f, 0xFF1B4E8C, true, dp(2)));
        texts.addView(text("腾讯频道 forum/rank · 免签名直发 · 点开填表", 11.0f, 0xFF5B7FA8, false, 0));
        row.addView(texts, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView arrow = text("›", 20, 0xFF5B7FA8, false, 0);
        row.addView(arrow);

        card.addView(row, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, dp(58)));

        card.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                press(v);
                if (!(ctx instanceof Activity)) {
                    toast("这里开不了面板喵");
                    return;
                }
                if (!com.cathelperawa.owo.app.bridge.CurrentChat.has()) {
                    toast("先在聊天里点开一个会话喵");
                    return;
                }
                GuildCardSheet.show((Activity) ctx);
            }
        });

        LinearLayout wrap = new LinearLayout(ctx);
        wrap.setOrientation(LinearLayout.VERTICAL);
        wrap.setPadding(0, 0, 0, dp(7));
        wrap.addView(card, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        return wrap;
    }

    private View buildRow(final String[] c) {
        FrameLayout card = whiteFrame(dp(16), 0x8CFFFFFF);
        card.setClipToOutline(true);

        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(14), dp(1), dp(14), dp(1));

        LinearLayout texts = new LinearLayout(ctx);
        texts.setOrientation(LinearLayout.VERTICAL);
        final boolean todo = TODO_KEYS.contains(c[0]);
        texts.addView(text(c[1], 14.5f, todo ? 0xFF9AA0B0 : 0xFF43587A, true, dp(2)));
        texts.addView(text(todo ? (c[2] + " · 未做") : c[2], 11.0f,
                todo ? 0xFFE5484D : 0xFF8496AE, false, 0));
        row.addView(texts, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        final String key = c[0];
        final SwitchBox box = new SwitchBox(ctx, cfgGet(key, false));
        row.addView(box, new LinearLayout.LayoutParams(dp(50), dp(28)));

        card.addView(row, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, dp(58)));

        card.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                boolean now = !swOn(key);
                swSet(key, now);
                box.setChecked(now);
                press(v);
                toast(c[1] + (now ? " 开启喵～" : " 关闭喵～"));
            }
        });
        host.bindSwitch(key, box, null);

        LinearLayout wrap = new LinearLayout(ctx);
        wrap.setOrientation(LinearLayout.VERTICAL);
        wrap.setPadding(0, 0, 0, dp(7));
        wrap.addView(card, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        return wrap;
    }
}
