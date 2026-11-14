package com.cathelperawa.owo.app.ui;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.cathelperawa.owo.app.bridge.CatToast;
import com.cathelperawa.owo.app.feature.GuildCard;

public final class GuildCardSheet {

    private static final String DEF_COVER =
            "https://channelr.photo.store.qq.com/psc?/channel/NaDwC23LjvXqrn3RH.9z8doIq5u3GMMkGv7wrla*b11lfiYhGaBaRYfjWX9bTLlhceQZOWssFZuUFMl6MxR1WArtZoZQb.gvLE7if1gFVQQ!/o=&bo=LAEsASwBLAECACQ!";

    private GuildCardSheet() {
    }

    public static void show(final Activity act) {
        if (act == null) return;
        final float f = act.getResources().getDisplayMetrics().density;
        final Dialog dlg = new Dialog(act, android.R.style.Theme_Translucent_NoTitleBar);

        LinearLayout root = new LinearLayout(act);
        root.setOrientation(1);
        root.setBackgroundColor(-1288684979);
        int pad = (int) (16.0f * f);
        root.setPadding(pad, (int) (56.0f * f), pad, (int) (24.0f * f));

        LinearLayout card = new LinearLayout(act);
        card.setOrientation(1);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(-722689);
        bg.setCornerRadius(22.0f * f);
        card.setBackgroundDrawable(bg);
        card.setPadding(pad, pad, pad, pad);

        TextView title = new TextView(act);
        title.setText("频道帖子卡");
        title.setTextSize(17.0f);
        title.setTextColor(-13681066);
        card.addView(title);

        TextView sub = new TextView(act);
        sub.setText("填标题和封面就能发 · 其余自动");
        sub.setTextSize(11.0f);
        sub.setTextColor(-7693131);
        int gap = (int) (8.0f * f);
        sub.setPadding(0, (int) (4.0f * f), 0, gap);
        card.addView(sub);

        final android.content.SharedPreferences sp =
                act.getSharedPreferences("guild_card_pref", 0);
        String lastTitle = sp.getString("title", "");
        String lastCover = sp.getString("cover", DEF_COVER);

        final EditText eTitle = field(act, f, "标题（必填）", lastTitle);
        eTitle.setTextSize(15.0f);
        card.addView(eTitle);

        final EditText eCover = field(act, f, "封面图 URL（留空用默认图）", lastCover);
        card.addView(eCover);

        final LinearLayout more = new LinearLayout(act);
        more.setOrientation(1);
        more.setVisibility(View.GONE);

        final EditText eGuildName = field(act, f, "服务器名", "");
        final EditText eGuildId = field(act, f, "服务器 ID", "");
        final EditText eChannelName = field(act, f, "子频道名", "帖子广场");
        final EditText eNick = field(act, f, "发帖人昵称", "");
        final EditText eAvatar = field(act, f, "发帖人头像 URL", "");
        final EditText eTinyId = field(act, f, "发帖人 tiny_id", "");
        final EditText eFeedId = field(act, f, "feed_id（空=自动）", "");
        final EditText eToken = field(act, f, "token（空=自动）", "");
        final EditText eWH = field(act, f, "封面宽高（如 300x300）", "300x300");
        final EditText eEmoji = field(act, f, "正文尾部表情 ID", "");
        more.addView(section(act, f, "频道信息"));
        more.addView(eGuildName);
        more.addView(eGuildId);
        more.addView(eChannelName);
        more.addView(section(act, f, "发帖人"));
        more.addView(eNick);
        more.addView(eAvatar);
        more.addView(eTinyId);
        more.addView(section(act, f, "高级"));
        more.addView(eFeedId);
        more.addView(eToken);
        more.addView(eWH);
        more.addView(eEmoji);
        card.addView(more);

        final TextView bMore = new TextView(act);
        bMore.setText("▸ 更多设置");
        bMore.setTextSize(13.0f);
        bMore.setTextColor(-13681066);
        bMore.setPadding(0, (int) (6.0f * f), 0, (int) (8.0f * f));
        bMore.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                boolean open = more.getVisibility() == View.VISIBLE;
                more.setVisibility(open ? View.GONE : View.VISIBLE);
                bMore.setText(open ? "▸ 更多设置" : "▾ 收起设置");
            }
        });
        card.addView(bMore);

        int h = (int) (46.0f * f);

        final TextView bSend = btn(act, f, "发送", -1020763, -1);
        LinearLayout.LayoutParams lpS = new LinearLayout.LayoutParams(-1, h);
        lpS.topMargin = (int) (4.0f * f);
        card.addView(bSend, lpS);

        LinearLayout bar2 = new LinearLayout(act);
        bar2.setOrientation(0);
        bar2.setPadding(0, (int) (8.0f * f), 0, 0);
        TextView bJson = btn(act, f, "看 JSON", -1315086, -13681066);
        bar2.addView(bJson, new LinearLayout.LayoutParams(0, (int) (40.0f * f), 1.0f));
        TextView bPb = btn(act, f, "PB 直发", -1315086, -13681066);
        LinearLayout.LayoutParams lpP = new LinearLayout.LayoutParams(0, (int) (40.0f * f), 1.0f);
        lpP.leftMargin = gap;
        bar2.addView(bPb, lpP);
        TextView bClose = btn(act, f, "关闭", -1051654, -10851451);
        LinearLayout.LayoutParams lpC = new LinearLayout.LayoutParams(0, (int) (40.0f * f), 1.0f);
        lpC.leftMargin = gap;
        bar2.addView(bClose, lpC);
        card.addView(bar2);

        final TextView bRaw = btn(act, f, "频道卡（服务端签名 · 别人能看到）", 0xFF2E7D5B, -1);
        LinearLayout.LayoutParams lpR = new LinearLayout.LayoutParams(-1, (int) (38.0f * f));
        lpR.topMargin = (int) (6.0f * f);
        card.addView(bRaw, lpR);

        bRaw.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                bRaw.setText("重放中…");
                bRaw.setEnabled(false);
                new Thread(new Runnable() {
                    @Override public void run() {
                        GuildCard.Fields _f = collect(eTitle, eCover, eGuildName, eGuildId,
                        eChannelName, eNick, eAvatar, eTinyId, eFeedId, eToken, eWH, eEmoji);
                        GuildCard.Result rr0 = GuildCard.sendForumSigned(_f);
                        final GuildCard.Result rr = (rr0 != null && rr0.ok) ? rr0
                                : GuildCard.sendChannelSigned(_f);
                        act.runOnUiThread(new Runnable() {
                            @Override public void run() {
                                bRaw.setText("频道卡（服务端签名 · 别人能看到）");
                                bRaw.setEnabled(true);
                                CatToast.show(rr != null && rr.ok ? "已发出 · 看对方有没有" : ("失败：" + (rr == null ? "无返回" : rr.error)));
                            }
                        });
                    }
                }, "guild-raw-replay").start();
            }
        });

        final TextView bPatch = btn(act, f, "劫持分享（去频道手动分享）", 0xFFE8A93B, -1);
        LinearLayout.LayoutParams lpPa = new LinearLayout.LayoutParams(-1, h);
        lpPa.topMargin = (int) (8.0f * f);
        card.addView(bPatch, lpPa);

        bPatch.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                com.cathelperawa.owo.app.feature.SharePatch.enabled =
                        !com.cathelperawa.owo.app.feature.SharePatch.enabled;
                if (com.cathelperawa.owo.app.feature.SharePatch.enabled) {
                    String t = eTitle.getText().toString().trim();
                    String c = eCover.getText().toString().trim();
                    com.cathelperawa.owo.app.feature.SharePatch.title = t;
                    com.cathelperawa.owo.app.feature.SharePatch.cover = c;
                    savePref(sp, eTitle, eCover);
                    if (t.length() == 0) {
                        com.cathelperawa.owo.app.feature.SharePatch.enabled = false;
                        CatToast.show("先填标题");
                        return;
                    }
                    bPatch.setText("劫持已开 · 点此关闭");
                    CatToast.show("现在去频道里长按帖子→分享到群，内容会被换成上面的");
                    dlg.dismiss();
                } else {
                    bPatch.setText("劫持分享（去频道手动分享）");
                    CatToast.show("分享劫持已关闭");
                }
            }
        });

        bClose.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { dlg.dismiss(); }
        });

        bJson.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                GuildCard.Fields fl = collect(eTitle, eCover, eGuildName, eGuildId,
                        eChannelName, eNick, eAvatar, eTinyId, eFeedId, eToken, eWH, eEmoji);
                if (fl == null) { CatToast.show("先填标题"); return; }
                savePref(sp, eTitle, eCover);
                String ark = GuildCard.build(fl);
                showText(act, f, ark == null ? "组卡失败" : ark, dlg);
            }
        });

        bSend.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                sendWith(act, f, eTitle, eCover, eGuildName, eGuildId, eChannelName,
                        eNick, eAvatar, eTinyId, eFeedId, eToken, eWH, eEmoji,
                        bSend, "发送", dlg, false);
            }
        });

        bPb.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                sendWith(act, f, eTitle, eCover, eGuildName, eGuildId, eChannelName,
                        eNick, eAvatar, eTinyId, eFeedId, eToken, eWH, eEmoji,
                        bPb, "PB 直发", dlg, true);
            }
        });

        ScrollView scroll = new ScrollView(act);
        scroll.addView(card);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1.0f));
        dlg.setContentView(root);
        Window w = dlg.getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(0));
            w.setLayout(-1, -1);
        }
        dlg.show();

        eTitle.requestFocus();
    }

    private static void savePref(android.content.SharedPreferences sp,
                                 EditText eTitle, EditText eCover) {
        try {
            sp.edit()
                    .putString("title", eTitle.getText().toString().trim())
                    .putString("cover", eCover.getText().toString().trim())
                    .apply();
        } catch (Throwable ignore) {
        }
    }

    private static void sendWith(final Activity act, final float f, EditText eTitle, EditText eCover,
                                 EditText eGuildName, EditText eGuildId, EditText eChannelName,
                                 EditText eNick, EditText eAvatar, EditText eTinyId,
                                 EditText eFeedId, EditText eToken, EditText eWH, EditText eEmoji,
                                 final TextView btnView, final String label,
                                 final Dialog dlg, final boolean usePb) {
        final GuildCard.Fields fl = collect(eTitle, eCover, eGuildName, eGuildId,
                eChannelName, eNick, eAvatar, eTinyId, eFeedId, eToken, eWH, eEmoji);
        if (fl == null) {
            CatToast.show("先填标题");
            return;
        }
        try {
            act.getSharedPreferences("guild_card_pref", 0).edit()
                    .putString("title", eTitle.getText().toString().trim())
                    .putString("cover", eCover.getText().toString().trim())
                    .apply();
        } catch (Throwable ignore) {
        }
        btnView.setText("发送中…");
        btnView.setEnabled(false);
        new Thread(new Runnable() {
            @Override public void run() {
                final boolean ok;
                final String msg;
                if (usePb) {
                    int rc = GuildCard.sendPb(fl);
                    ok = rc == 0;
                    msg = ok ? "服务端接受 rc=0" : GuildCard.describeRc(rc);
                } else {
                    ok = GuildCard.send(fl);
                    msg = ok ? "已发到本地 · 让对方看一眼" : "内核发送失败";
                }
                act.runOnUiThread(new Runnable() {
                    @Override public void run() {
                        btnView.setText(label);
                        btnView.setEnabled(true);
                        CatToast.show(msg);
                        if (ok) dlg.dismiss();
                    }
                });
            }
        }, "guild-send").start();
    }

    private static void showText(Activity act, float f, String text, Dialog parent) {
        final Dialog d = new Dialog(act, android.R.style.Theme_Translucent_NoTitleBar);
        LinearLayout l = new LinearLayout(act);
        l.setOrientation(1);
        l.setBackgroundColor(-14803426);
        int pad = (int) (16.0f * f);
        l.setPadding(pad, (int) (56.0f * f), pad, (int) (24.0f * f));
        TextView tv = new TextView(act);
        tv.setText(text);
        tv.setTextSize(11.0f);
        tv.setTextColor(-6645094);
        tv.setTextIsSelectable(true);
        ScrollView sv = new ScrollView(act);
        sv.addView(tv);
        l.addView(sv, new LinearLayout.LayoutParams(-1, 0, 1.0f));
        TextView bc = btn(act, f, "关掉", -1051654, -10851451);
        bc.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { d.dismiss(); }
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, (int) (44.0f * f));
        lp.topMargin = (int) (10.0f * f);
        l.addView(bc, lp);
        d.setContentView(l);
        Window w = d.getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(0));
            w.setLayout(-1, -1);
        }
        d.show();
    }

    private static GuildCard.Fields collect(EditText eTitle, EditText eCover, EditText eGuildName,
                                            EditText eGuildId, EditText eChannelName, EditText eNick,
                                            EditText eAvatar, EditText eTinyId, EditText eFeedId,
                                            EditText eToken, EditText eWH, EditText eEmoji) {
        String t = eTitle.getText().toString().trim();
        if (t.length() == 0) return null;
        GuildCard.Fields fl = new GuildCard.Fields();
        fl.title = t;
        fl.content = t;
        fl.cover = eCover.getText().toString().trim();
        fl.guildName = eGuildName.getText().toString().trim();
        fl.guildId = eGuildId.getText().toString().trim();
        fl.channelName = eChannelName.getText().toString().trim();
        fl.posterNick = eNick.getText().toString().trim();
        fl.posterAvatar = eAvatar.getText().toString().trim();
        fl.posterTinyId = eTinyId.getText().toString().trim();
        fl.feedId = eFeedId.getText().toString().trim();
        fl.token = eToken.getText().toString().trim();
        fl.emojiId = eEmoji.getText().toString().trim();
        String wh = eWH.getText().toString().trim().toLowerCase().replace('*', 'x');
        int xi = wh.indexOf('x');
        if (xi > 0) {
            try {
                fl.picW = Integer.parseInt(wh.substring(0, xi).trim());
                fl.picH = Integer.parseInt(wh.substring(xi + 1).trim());
            } catch (Throwable ignore) {
            }
        }
        return fl;
    }

    private static TextView section(Activity act, float f, String s) {
        TextView tv = new TextView(act);
        tv.setText(s);
        tv.setTextSize(11.0f);
        tv.setTextColor(-7693131);
        tv.setPadding(0, (int) (8.0f * f), 0, (int) (4.0f * f));
        return tv;
    }

    private static EditText field(Activity act, float f, String hint, String def) {
        EditText e = new EditText(act);
        e.setHint(hint);
        e.setText(def);
        e.setTextSize(13.0f);
        e.setSingleLine(true);
        e.setTextColor(-12824981);
        GradientDrawable g = new GradientDrawable();
        g.setColor(-1);
        g.setCornerRadius(12.0f * f);
        e.setBackgroundDrawable(g);
        int i = (int) (10.0f * f);
        int i2 = (int) (8.0f * f);
        e.setPadding(i, i2, i, i2);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.bottomMargin = i2;
        e.setLayoutParams(lp);
        return e;
    }

    private static TextView btn(Activity act, float f, String s, int color, int textColor) {
        TextView tv = new TextView(act);
        tv.setText(s);
        tv.setTextSize(14.0f);
        tv.setTextColor(textColor);
        tv.setGravity(17);
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(f * 14.0f);
        tv.setBackgroundDrawable(g);
        return tv;
    }
}
