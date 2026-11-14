package com.cathelperawa.owo.app.ui;

import android.app.Activity;
import com.cathelperawa.owo.app.core.Bg;
import android.app.Dialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.cathelperawa.owo.app.bridge.CatToast;
import com.cathelperawa.owo.app.bridge.CurrentChat;
import com.cathelperawa.owo.app.core.HookConfig;
import com.cathelperawa.owo.app.core.HookLog;
import com.cathelperawa.owo.app.proto.VoiceVault;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;

public final class VoiceVaultSheet {

    private VoiceVaultSheet() {}

    private static final int C_TITLE = 0xFF3C4E6B;
    private static final int C_SUB = 0xFF8A9CB5;

    public static void show(final Context anyCtx) {
        final Activity act = activityOf(anyCtx);
        if (act == null) {
            CatToast.show("这里打不开语音库喵");
            return;
        }
        act.runOnUiThread(new Runnable() {
            @Override public void run() {
                try {
                    new Sheet(act).show();
                } catch (Throwable t) {
                    HookLog.e("语音库面板构建失败", t);
                    CatToast.show("语音库打不开：" + t);
                }
            }
        });
    }

    private static final class Sheet {
        private final Activity act;
        private final float d;
        private final int screenH;

        private Dialog dlg;
        private ScrollView sv;
        private LinearLayout list;
        private TextView sub;
        private TextView btnDel;
        private TextView btnToSilk;
        private TextView btnDur;
        private TextView btnSend;
        private SwitchBox autoBox;
        private VoicePlayerBar playerBar;
        private final LinkedHashSet<String> sel = new LinkedHashSet<String>();
        private List<VoiceVault.Item> items = new ArrayList<VoiceVault.Item>();

        Sheet(Activity a) {
            this.act = a;
            this.d = a.getResources().getDisplayMetrics().density;
            DisplayMetrics dm = a.getResources().getDisplayMetrics();
            this.screenH = dm.heightPixels;
        }

        void show() {
            dlg = new Dialog(act, android.R.style.Theme_Translucent_NoTitleBar);
            FrameLayout root = new FrameLayout(act);
            root.setBackgroundColor(Color.TRANSPARENT);

            LinearLayout card = new LinearLayout(act);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(16), dp(14), dp(16), dp(12));
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(0xF2FFFFFF);
            bg.setCornerRadius(dp(24));
            bg.setStroke(dp(1), 0xA6FFFFFF);
            card.setBackgroundDrawable(bg);
            card.setElevation(dp(12));

            LinearLayout head = new LinearLayout(act);
            head.setOrientation(LinearLayout.HORIZONTAL);
            head.setGravity(Gravity.CENTER_VERTICAL);

            LinearLayout titles = new LinearLayout(act);
            titles.setOrientation(LinearLayout.VERTICAL);
            titles.addView(tv("语音库", 18, C_TITLE, true));
            sub = tv("", 11, C_SUB, false);
            titles.addView(sub);
            head.addView(titles, new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            TextView close = tv("✕", 15, 0xFF9AAABF, true);
            close.setPadding(dp(12), dp(4), dp(4), dp(4));
            close.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { dlg.dismiss(); }
            });
            head.addView(close);
            LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            hp.bottomMargin = dp(10);
            card.addView(head, hp);

            card.addView(toolbar(), matchWrap(dp(6)));
            card.addView(autoRow(), matchWrap(dp(6)));
            card.addView(batchBar(), matchWrap(dp(6)));

            sv = new ScrollView(act);
            sv.setVerticalScrollBarEnabled(false);
            list = new LinearLayout(act);
            list.setOrientation(LinearLayout.VERTICAL);
            sv.addView(list, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            card.addView(sv, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            playerBar = new VoicePlayerBar(act);
            card.addView(playerBar.view(), matchWrap(dp(7)));

            card.addView(actionBar(), matchWrap(0));

            FrameLayout.LayoutParams clp = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM);
            clp.leftMargin = (int) (act.getResources().getDisplayMetrics().widthPixels * 0.03f);
            clp.rightMargin = clp.leftMargin;
            clp.bottomMargin = dp(14);
            root.addView(card, clp);

            dlg.setContentView(root);
            dlg.setCanceledOnTouchOutside(true);
            dlg.setOnDismissListener(new android.content.DialogInterface.OnDismissListener() {
                @Override public void onDismiss(android.content.DialogInterface di) {
                    VoiceVault.stop();
                    if (playerBar != null) playerBar.detach();
                }
            });
            dlg.show();

            Window w = dlg.getWindow();
            if (w != null) {
                w.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
                w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
                WindowManager.LayoutParams lp = w.getAttributes();
                lp.width = WindowManager.LayoutParams.MATCH_PARENT;
                lp.height = WindowManager.LayoutParams.MATCH_PARENT;
                lp.dimAmount = 0.22f;
                w.setAttributes(lp);
            }
            refresh();
        }

        private View toolbar() {
            LinearLayout bar = new LinearLayout(act);
            bar.setOrientation(LinearLayout.HORIZONTAL);

            TextView imp = chip("导入文件", 0xFF6E93D6, false);
            imp.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { pickFiles(); }
            });
            TextView pars = chip("解析结果", 0xFF8B8FD6, false);
            pars.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { pickParseResults(); }
            });
            TextView all = chip("全选", 0xFF9AA6B6, false);
            all.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    if (sel.size() == items.size()) sel.clear();
                    else for (VoiceVault.Item it : items) sel.add(it.id);
                    refresh();
                }
            });

            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            p.rightMargin = dp(5);
            LinearLayout.LayoutParams p2 = new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            p2.leftMargin = dp(3);
            p2.rightMargin = dp(5);
            LinearLayout.LayoutParams p3 = new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            p3.leftMargin = dp(3);
            bar.addView(imp, p);
            bar.addView(pars, p2);
            bar.addView(all, p3);
            return bar;
        }

        private View actionBar() {
            LinearLayout bar = new LinearLayout(act);
            bar.setOrientation(LinearLayout.HORIZONTAL);

            btnDel = pill("删除", 11.5f, 0xFFE2617E, 0xFFFDE7ED, 0xFFF9CDD8, false);
            btnDel.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { doDelete(); }
            });
            btnToSilk = pill("自动转腾讯语音", 11.5f, 0xFFE85D9C, 0xFFFFE2EF, 0xFFFFBEDA, false);
            btnToSilk.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { doConvert(true); }
            });
            btnDur = pill(durLabel(), 11.5f, 0xFF7A72D0, 0xFFEEECFF, 0xFFD8D3F8, false);
            btnDur.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { askDuration(); }
            });
            btnSend = pill("发送", 12f, 0xFFFFFFFF, 0xFF7FC9A6, 0xFF6DBD97, true);
            btnSend.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { doSend(); }
            });

            LinearLayout.LayoutParams p1 = new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 0.82f);
            p1.rightMargin = dp(4);
            LinearLayout.LayoutParams p2 = new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1.5f);
            p2.leftMargin = dp(2);
            p2.rightMargin = dp(4);
            LinearLayout.LayoutParams p3 = new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1.18f);
            p3.leftMargin = dp(2);
            p3.rightMargin = dp(4);
            LinearLayout.LayoutParams p4 = new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 0.9f);
            p4.leftMargin = dp(2);
            bar.addView(btnDel, p1);
            bar.addView(btnToSilk, p2);
            bar.addView(btnDur, p3);
            bar.addView(btnSend, p4);
            return bar;
        }

        private View batchBar() {
            TextView t = tv("", 10.5f, 0xFFA3AFBE, false);
            t.setGravity(Gravity.CENTER_VERTICAL);
            t.setTag("batchtip");
            batchTip = t;
            return t;
        }

        private TextView batchTip;

        private void refresh() {
            try {
                items = VoiceVault.list();
            } catch (Throwable t) {
                items = new ArrayList<VoiceVault.Item>();
            }

            Iterator<String> itr = sel.iterator();
            while (itr.hasNext()) {
                String id = itr.next();
                boolean ok = false;
                for (VoiceVault.Item it : items) if (it.id.equals(id)) { ok = true; break; }
                if (!ok) itr.remove();
            }

            list.removeAllViews();
            if (items.isEmpty()) {
                LinearLayout empty = new LinearLayout(act);
                empty.setOrientation(LinearLayout.VERTICAL);
                empty.setGravity(Gravity.CENTER);
                empty.setPadding(dp(10), dp(26), dp(10), dp(26));
                empty.addView(tv("这里还空着喵", 14, C_TITLE, true));
                TextView tip = tv("长按别人的语音 → 存语音\n右上角「导入文件」也能塞进来", 11, C_SUB, false);
                tip.setGravity(Gravity.CENTER);
                tip.setLineSpacing(dp(3), 1f);
                LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                tp.topMargin = dp(6);
                empty.addView(tip, tp);
                list.addView(empty);
            } else {
                for (VoiceVault.Item it : items) list.addView(rowOf(it), matchWrap(dp(6)));
            }

            sub.setText(items.isEmpty() ? "还没有存进来的语音" : VoiceVault.stat());
            if (batchTip != null) {
                batchTip.setText(sel.isEmpty()
                        ? "点条目选中 · 条目右边 ⋯ 有「转 MP3 / 改名」"
                        : "已选 " + sel.size() + " 条 · 转换只存库、不发出去");
            }
            boolean has = !sel.isEmpty();
            tint(btnDel, has, 0xFFE86A6A);
            tint(btnToSilk, has, 0xFFF07AAE);
            tint(btnSend, has, 0xFF57AE83);

            if (btnDur != null) {
                btnDur.setAlpha(1f);
                btnDur.setText(durLabel());
            }

            sv.post(new Runnable() {
                @Override public void run() {
                    try {
                        int cap = (int) (screenH * 0.46f);
                        int h = list.getHeight();
                        ViewGroup.LayoutParams lp = sv.getLayoutParams();
                        int want = h > cap ? cap : h;
                        if (lp.height != want) {
                            lp.height = want;
                            sv.setLayoutParams(lp);
                        }
                    } catch (Throwable ignore) {
                    }
                }
            });
        }

        private void tint(TextView t, boolean on, int color) {
            if (t == null) return;
            t.setAlpha(on ? 1f : 0.45f);
            t.setClickable(on);
        }

        private View rowOf(final VoiceVault.Item it) {
            final boolean picked = sel.contains(it.id);

            LinearLayout row = new LinearLayout(act);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(11), dp(10), dp(10), dp(10));

            GradientDrawable g = new GradientDrawable();
            g.setCornerRadius(dp(16));
            g.setColor(picked ? 0xFFFFEAF4 : 0x99FFFFFF);
            g.setStroke(dp(1), picked ? 0xFFFFB3D2 : 0x55FFFFFF);
            row.setBackgroundDrawable(g);

            TextView ck = tv(picked ? "✓" : "○", 15, picked ? 0xFFE85D9C : 0xFFC3CCD8, true);
            ck.setGravity(Gravity.CENTER);
            row.addView(ck, new LinearLayout.LayoutParams(dp(26),
                    ViewGroup.LayoutParams.WRAP_CONTENT));

            LinearLayout texts = new LinearLayout(act);
            texts.setOrientation(LinearLayout.VERTICAL);

            String name = it.name == null || it.name.length() == 0 ? "未命名" : it.name;
            texts.addView(tv(name, 13.5f, C_TITLE, true));

            StringBuilder meta = new StringBuilder();
            if (it.durMs > 0) meta.append(it.durText());
            long size = it.sizeBytes();
            if (size > 0) meta.append(meta.length() > 0 ? " · " : "").append(size / 1024).append("KB");
            if (it.src != null && it.src.length() > 0) {
                meta.append(meta.length() > 0 ? " · " : "").append(it.src);
            }
            String txt = it.text == null ? "" : it.text.trim();
            if (txt.length() > 0) {
                if (txt.length() > 14) txt = txt.substring(0, 14) + "…";
                meta.append(meta.length() > 0 ? " · " : "").append("「").append(txt).append("」");
            }
            texts.addView(tv(meta.toString(), 10.5f, C_SUB, false));

            LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            tlp.leftMargin = dp(4);
            row.addView(texts, tlp);

            boolean hasSilk = it.hasSilk || VoiceVault.silkFile(it) != null;
            TextView badge = tv(hasSilk ? "腾讯语音" : "普通音频", 10,
                    hasSilk ? 0xFFE85D9C : 0xFF7F8FA6, true);
            badge.setPadding(dp(7), dp(3), dp(7), dp(3));
            GradientDrawable bgb = new GradientDrawable();
            bgb.setCornerRadius(dp(9));
            bgb.setColor(hasSilk ? 0xFFFFD6E8 : 0xFFE9EEF5);
            badge.setBackgroundDrawable(bgb);
            LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            blp.rightMargin = dp(6);
            row.addView(badge, blp);

            TextView playBtn = tv("▶", 9, 0xFFFFFFFF, false);
            playBtn.setGravity(Gravity.CENTER);
            playBtn.setPadding(0, 0, dp(1), 0);
            GradientDrawable pbg = new GradientDrawable();
            pbg.setShape(GradientDrawable.OVAL);
            pbg.setColor(0xFF7FC9A6);
            playBtn.setBackgroundDrawable(pbg);
            playBtn.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { preview(it); }
            });
            LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(dp(26), dp(26));
            plp.rightMargin = dp(6);
            row.addView(playBtn, plp);

            TextView more = tv("⋯", 17, 0xFF9AAABF, true);
            more.setPadding(dp(8), 0, dp(4), 0);
            more.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { itemMenu(it); }
            });
            row.addView(more);

            row.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    if (sel.contains(it.id)) sel.remove(it.id);
                    else sel.add(it.id);
                    refresh();
                }
            });
            row.setOnLongClickListener(new View.OnLongClickListener() {
                @Override public boolean onLongClick(View v) {
                    itemMenu(it);
                    return true;
                }
            });
            return row;
        }

        private void itemMenu(final VoiceVault.Item it) {
            final String[] menu = new String[]{
                    "试听 / 停止", "发到当前会话", "改名字",
                    "转成腾讯语音（只存库）", "转成普通音频 MP3（只存库）", "删除这一条"
            };
            CatSheet.pick(act, it.name == null ? "这条语音" : it.name, menu,
                    new CatSheet.OnPick() {
                        @Override public void run(int idx, String text) {
                            switch (idx) {
                                case 0: preview(it); break;
                                case 1: {
                                    sel.clear();
                                    sel.add(it.id);
                                    refresh();
                                    doSend();
                                    break;
                                }
                                case 2: askRename(it); break;
                                case 3: convertOne(it, true); break;
                                case 4: convertOne(it, false); break;
                                case 5: {
                                    if (VoiceVault.remove(java.util.Collections.singletonList(it)) > 0) {
                                        CatToast.show("删掉啦");
                                    }
                                    refresh();
                                    break;
                                }
                                default: break;
                            }
                        }
                    });
        }

        private void preview(final VoiceVault.Item it) {
            String now = VoiceVault.playingId();
            if (now != null && now.equals(it.id)) {
                VoiceVault.stop();
                if (playerBar != null) playerBar.sync();
                CatToast.show("停了");
                return;
            }
            CatToast.show("准备试听…");
            VoiceVault.playAsync(it, new VoiceVault.OnPlay() {
                @Override public void run(boolean ok) {
                    if (playerBar != null) playerBar.sync();
                    if (!ok) CatToast.show("放不出来：" + VoiceVault.lastError());
                }
            });
        }

        private void askRename(final VoiceVault.Item it) {
            CatSheet.input(act, "改名字", "给这条语音起个名字",
                    it.name == null ? "" : it.name, new CatSheet.OnText() {
                        @Override public void run(String s) {
                            if (s == null || s.trim().length() == 0) return;
                            if (VoiceVault.rename(it, s)) CatToast.show("改好了");
                            else CatToast.show("改不动…");
                            refresh();
                        }
                    });
        }

        private List<VoiceVault.Item> selected() {
            List<VoiceVault.Item> out = new ArrayList<VoiceVault.Item>();
            for (VoiceVault.Item it : items) {
                if (sel.contains(it.id)) out.add(it);
            }
            return out;
        }

        private void doDelete() {
            final List<VoiceVault.Item> arr = selected();
            if (arr.isEmpty()) { CatToast.show("先选几条喵"); return; }
            CatSheet.confirm(act, "删除 " + arr.size() + " 条语音",
                    "文件会一起删掉，删完找不回来。",
                    "删掉", true, new Runnable() {
                        @Override public void run() {
                            int n = VoiceVault.remove(arr);
                            sel.clear();
                            CatToast.show("删了 " + n + " 条");
                            refresh();
                        }
                    });
        }

        private void doConvert(final boolean toSilk) {
            final List<VoiceVault.Item> arr = selected();
            if (arr.isEmpty()) { CatToast.show("先选几条喵"); return; }
            CatToast.show(toSilk ? "正在转腾讯语音…" : "正在转 MP3…");
            Bg.run(new Runnable() {
                @Override public void run() {
                    int ok = 0;
                    for (VoiceVault.Item it : arr) {
                        String p = toSilk ? VoiceVault.ensureSilk(it) : VoiceVault.ensureMp3(it);
                        if (p != null) ok++;
                    }
                    final int n = ok;
                    final int total = arr.size();
                    act.runOnUiThread(new Runnable() {
                        @Override public void run() {
                            CatToast.show((toSilk ? "转腾讯语音 " : "转 MP3 ")
                                    + n + " / " + total + " 成功");
                            refresh();
                        }
                    });
                }
            }, "cat-voice-conv");
        }

        private void convertOne(final VoiceVault.Item it, final boolean toSilk) {
            CatToast.show(toSilk ? "正在转腾讯语音…" : "正在转 MP3…");
            Bg.run(new Runnable() {
                @Override public void run() {
                    final String p = toSilk ? VoiceVault.ensureSilk(it) : VoiceVault.ensureMp3(it);
                    act.runOnUiThread(new Runnable() {
                        @Override public void run() {
                            CatToast.show(p != null
                                    ? (toSilk ? "已转成腾讯语音，存进库里了（没发出去）"
                                    : "已转成 MP3，存进库里了（没发出去）")
                                    : "转不出来…（宿主编码器没起来？）");
                            refresh();
                        }
                    });
                }
            }, "cat-voice-conv1");
        }

        private void doSend() {
            final List<VoiceVault.Item> arr = selected();
            if (arr.isEmpty()) { CatToast.show("先选几条喵"); return; }
            final int ct = CurrentChat.chatType();
            final String peer = CurrentChat.peerUid();
            final int durOverride = durSec() > 0 ? durSec() * 1000 : 0;
            final boolean auto = autoConvOn();
            if (ct == 0 || peer == null || peer.length() == 0) {
                CatToast.show("认不出当前会话，进聊天窗口再试");
                return;
            }
            CatToast.show("正在发…");
            Bg.run(new Runnable() {
                @Override public void run() {
                    int ok = 0;
                    int skipped = 0;
                    for (VoiceVault.Item it : arr) {
                        boolean hasSilk = VoiceVault.silkFile(it) != null;
                        if (!auto && !hasSilk) {
                            skipped++;
                            continue;
                        }
                        if (VoiceVault.send(it, ct, peer, durOverride, auto)) ok++;
                    }
                    final int n = ok;
                    final int sk = skipped;
                    final int total = arr.size();
                    act.runOnUiThread(new Runnable() {
                        @Override public void run() {
                            CatToast.show(sk > 0
                                    ? ("发出去 " + n + " / " + total + " 条 · " + sk
                                    + " 条没腾讯语音版本（自动转换关着，跳过了）")
                                    : ("发出去 " + n + " / " + total + " 条"));
                            if (n > 0) { sel.clear(); refresh(); }
                        }
                    });
                }
            }, "cat-voice-send");
        }

        private boolean autoConvOn() {
            try {
                return HookConfig.getBoolean(act, "vv_auto_conv", true);
            } catch (Throwable t) {
                return true;
            }
        }

        private View autoRow() {
            LinearLayout card = new LinearLayout(act);
            card.setOrientation(LinearLayout.HORIZONTAL);
            card.setGravity(Gravity.CENTER_VERTICAL);
            card.setPadding(dp(12), dp(9), dp(10), dp(9));
            GradientDrawable g = new GradientDrawable();
            g.setCornerRadius(dp(15));
            g.setColor(0x8FFFFFFF);
            g.setStroke(dp(1), 0x55FFFFFF);
            card.setBackgroundDrawable(g);

            LinearLayout texts = new LinearLayout(act);
            texts.setOrientation(LinearLayout.VERTICAL);
            texts.addView(tv("发送时自动转腾讯语音", 12.5f, C_TITLE, true));
            texts.addView(tv(autoConvOn()
                    ? "开：没有腾讯语音版本的话先转再发（慢一点）"
                    : "关：只发现成的腾讯语音版本，没有就跳过", 10.5f, C_SUB, false));
            card.addView(texts, new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            autoBox = new SwitchBox(act, autoConvOn());
            LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(dp(46), dp(26));
            bp.leftMargin = dp(8);
            card.addView(autoBox, bp);

            View.OnClickListener toggle = new View.OnClickListener() {
                @Override public void onClick(View v) {
                    boolean z = !autoConvOn();
                    HookConfig.set(act, "vv_auto_conv", z ? "1" : "0");
                    if (autoBox != null) autoBox.setChecked(z);
                    UiKit.press(v);
                    CatToast.show(z ? "发送时会自动转腾讯语音"
                            : "发送时不再自动转（只发现成版本）");
                    refresh();
                }
            };
            card.setOnClickListener(toggle);
            autoBox.setOnClickListener(toggle);
            return card;
        }

        private int durSec() {
            try {
                String v = HookConfig.get(act, "vv_send_sec", "0");
                if (v == null) return 0;
                int n = Integer.parseInt(v.trim());
                if (n < 0) n = 0;
                if (n > 600) n = 600;
                return n;
            } catch (Throwable t) {
                return 0;
            }
        }

        private String durLabel() {
            int n = durSec();
            return n > 0 ? ("时长 " + n + "s") : "时长 真实";
        }

        private void askDuration() {
            CatSheet.input(act, "语音时间长度",
                    "填秒数会改气泡上显示的长度（音频本身不剪）；0 = 用真实时长",
                    String.valueOf(durSec()), new CatSheet.OnText() {
                        @Override public void run(String s) {
                            int n = 0;
                            try {
                                n = Integer.parseInt(s == null ? "0" : s.trim());
                            } catch (Throwable ignore) {
                                CatToast.show("要填数字喵");
                                return;
                            }
                            if (n < 0) n = 0;
                            if (n > 600) n = 600;
                            HookConfig.set(act, "vv_send_sec", String.valueOf(n));
                            CatToast.show(n > 0 ? ("之后发送显示 " + n + " 秒")
                                    : "之后发送用真实时长");
                            refresh();
                        }
                    });
        }

        private void pickFiles() {
            PickerBridge.pickAudio(act, new PickerBridge.OnPickMulti() {
                @Override public void run(final List<String> paths) {
                    if (paths == null || paths.isEmpty()) {
                        CatToast.show("一个都没选");
                        return;
                    }
                    CatToast.show("正在导入 " + paths.size() + " 个…");
                    Bg.run(new Runnable() {
                        @Override public void run() {
                            int ok = 0;
                            for (String p : paths) {
                                try {
                                    java.io.File f = new java.io.File(p);
                                    String n = f.getName();
                                    int dot = n.lastIndexOf('.');
                                    if (dot > 0) n = n.substring(0, dot);
                                    if (VoiceVault.addFile(act, n, f, "导入") != null) ok++;
                                } catch (Throwable ignore) {
                                }
                            }
                            final int k = ok;
                            act.runOnUiThread(new Runnable() {
                                @Override public void run() {
                                    CatToast.show(k > 0 ? ("导入 " + k + " 条") : "都没导进来…");
                                    refresh();
                                }
                            });
                        }
                    }, "cat-voice-import");
                }
            });
        }

        private void pickParseResults() {
            final List<java.io.File> fs = VoiceVault.scanParseResults();
            if (fs.isEmpty()) {
                CatToast.show("解析临时区里没找到语音（音乐/视频发过语音后再来）");
                return;
            }
            String[] labels = new String[fs.size()];
            for (int i = 0; i < fs.size(); i++) {
                java.io.File f = fs.get(i);
                labels[i] = f.getName() + "  " + (f.length() / 1024) + "KB";
            }
            CatSheet.pick(act, "解析出来的语音（点一个导入）", labels, new CatSheet.OnPick() {
                @Override public void run(int idx, String text) {
                    final java.io.File f = fs.get(idx);
                    CatToast.show("正在导入…");
                    Bg.run(new Runnable() {
                        @Override public void run() {
                            String n = f.getName();
                            int dot = n.lastIndexOf('.');
                            if (dot > 0) n = n.substring(0, dot);
                            final VoiceVault.Item it = VoiceVault.addFile(act, n, f, "解析");
                            act.runOnUiThread(new Runnable() {
                                @Override public void run() {
                                    CatToast.show(it != null ? "已导入语音库"
                                            : "这个文件读不动…");
                                    refresh();
                                }
                            });
                        }
                    }, "cat-voice-par");
                }
            });
        }

        private int dp(float v) {
            return (int) (v * d + 0.5f);
        }

        private TextView tv(String s, float size, int color, boolean bold) {
            TextView t = new TextView(act);
            t.setText(s);
            t.setTextSize(size);
            t.setTextColor(color);
            if (bold) t.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            return t;
        }

        private TextView chip(String label, int color, boolean solid) {
            TextView t = new TextView(act);
            t.setText(label);
            t.setTextSize(12);
            t.setGravity(Gravity.CENTER);
            t.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            t.setPadding(dp(6), dp(9), dp(6), dp(9));
            GradientDrawable g = new GradientDrawable();
            g.setCornerRadius(dp(14));
            if (solid) {
                g.setColor(color);
                g.setStroke(dp(1), color);
                t.setTextColor(0xFFFFFFFF);
            } else {
                g.setColor(0xFFFFFFFF);
                g.setStroke(dp(1), 0xFFE3E9F2);
                t.setTextColor(color);
            }
            t.setBackgroundDrawable(g);
            UiKit.press(t);
            return t;
        }

        private TextView pill(String label, float size, int textColor,
                              int bgColor, int strokeColor, boolean solid) {
            TextView t = new TextView(act);
            t.setText(label);
            t.setTextSize(size);
            t.setGravity(Gravity.CENTER);
            t.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            t.setSingleLine(true);
            t.setPadding(dp(4), dp(9), dp(4), dp(9));
            GradientDrawable g = new GradientDrawable();
            g.setCornerRadius(dp(15));
            g.setColor(bgColor);
            g.setStroke(dp(1), strokeColor);
            t.setBackgroundDrawable(g);
            t.setTextColor(textColor);
            if (solid) t.setElevation(dp(1));
            UiKit.press(t);
            return t;
        }

        private LinearLayout.LayoutParams matchWrap(int bottom) {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = bottom;
            return lp;
        }
    }

    private static Activity activityOf(Context c) {
        try {
            Context x = c;
            while (x instanceof ContextWrapper) {
                if (x instanceof Activity) return (Activity) x;
                x = ((ContextWrapper) x).getBaseContext();
                if (x == null) break;
            }
            if (c instanceof Activity) return (Activity) c;
        } catch (Throwable ignore) {
        }
        return null;
    }
}
