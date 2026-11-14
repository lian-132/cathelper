package com.cathelperawa.owo.app.ui;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.cathelperawa.owo.app.bridge.CatToast;
import com.cathelperawa.owo.app.core.HookConfig;
import com.cathelperawa.owo.app.proto.PbMsg;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class FakeChatSheet {

    private FakeChatSheet() {
    }

    public interface OnDone {
        void run(List<PbMsg.FakeItem> items, long baseSec);
    }

    private static final int C_TITLE = 0xFF3C4E6B;
    private static final int C_SUB = 0xFF8A9CB5;
    private static final String K_SRC = "fake_chat_src";

    private static final class Row {
        String mediaJson;
        EditText text;
        EditText nick;
        EditText uin;
        LinearLayout extra;
        TextView gear;
    }

    public static void open(Context anyCtx, final OnDone cb) {
        final Activity act = actOf(anyCtx);
        if (act == null) return;
        act.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                build(act, cb);
            }
        });
    }

    private static String load(Activity act) {
        return HookConfig.get(act, K_SRC,
                "#time " + nowText() + "\n"
                        + "小明|10001|在吗\n"
                        + "小红|10002|在\n"
                        + "小明|10001|出来睡觉");
    }

    private static void save(Activity act, String time, List<Row> rows) {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("#time ").append(time).append("\n");
            for (int i = 0; i < rows.size(); i++) {
                Row r = rows.get(i);
                if (r.mediaJson != null) continue;
                String t = str(r.text);
                if (t.length() == 0) continue;
                sb.append(str(r.nick)).append("|")
                        .append(str(r.uin)).append("|")
                        .append(t).append("\n");
            }
            HookConfig.set(act, K_SRC, sb.toString());
        } catch (Throwable ignore) {
        }
    }

    private static String str(EditText e) {
        return e == null || e.getText() == null ? "" : e.getText().toString().trim();
    }

    private static void build(final Activity act, final OnDone cb) {
        final float d = act.getResources().getDisplayMetrics().density;
        final int screenH = act.getResources().getDisplayMetrics().heightPixels;

        final Dialog dlg = new Dialog(act, android.R.style.Theme_Translucent_NoTitleBar);
        dlg.requestWindowFeature(Window.FEATURE_NO_TITLE);

        FrameLayout root = new FrameLayout(act);
        root.setBackgroundColor(0x66000000);

        LinearLayout card = new LinearLayout(act);
        card.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xF2FFFFFF);
        bg.setCornerRadius(22 * d);
        bg.setStroke((int) d, 0x66C9D6E8);
        card.setBackgroundDrawable(bg);
        int pad = (int) (16 * d);
        card.setPadding(pad, pad, pad, pad);

        FrameLayout.LayoutParams clp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        clp.gravity = Gravity.BOTTOM;
        clp.leftMargin = (int) (10 * d);
        clp.rightMargin = (int) (10 * d);
        clp.bottomMargin = (int) (10 * d);
        root.addView(card, clp);

        TextView title = new TextView(act);
        title.setText("伪造聊天记录");
        title.setTextSize(16);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(C_TITLE);
        card.addView(title);

        TextView sub = new TextView(act);
        sub.setText("一个框一条消息 · 点 ⚙ 设发送者 / 删除这条");
        sub.setTextSize(11);
        sub.setTextColor(C_SUB);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        slp.bottomMargin = (int) (10 * d);
        card.addView(sub, slp);

        String saved = load(act);
        String timeText = nowText();
        final List<String[]> init = new ArrayList<String[]>();
        String[] lines = saved.split("\\n");
        for (int i = 0; i < lines.length; i++) {
            String ln = lines[i] == null ? "" : lines[i].trim();
            if (ln.length() == 0) continue;
            if (ln.startsWith("#time")) {
                timeText = ln.substring(5).trim();
                continue;
            }
            String[] p = ln.split("\\|", 3);
            if (p.length >= 3) init.add(new String[]{p[0].trim(), p[1].trim(), p[2].trim()});
            else if (p.length == 2) init.add(new String[]{"", "", p[1].trim()});
            else init.add(new String[]{"", "", ln});
        }

        LinearLayout timeRow = new LinearLayout(act);
        timeRow.setOrientation(LinearLayout.HORIZONTAL);
        timeRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView timeLabel = new TextView(act);
        timeLabel.setText("时间");
        timeLabel.setTextSize(11);
        timeLabel.setTextColor(C_SUB);
        LinearLayout.LayoutParams tllp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tllp.rightMargin = (int) (8 * d);
        timeRow.addView(timeLabel, tllp);

        final EditText timeBox = new EditText(act);
        timeBox.setText(timeText);
        timeBox.setTextSize(12);
        timeBox.setTextColor(C_TITLE);
        timeBox.setSingleLine(true);
        timeBox.setTypeface(Typeface.MONOSPACE);
        timeBox.setInputType(InputType.TYPE_CLASS_TEXT);
        timeBox.setPadding((int) (10 * d), (int) (8 * d), (int) (10 * d), (int) (8 * d));
        GradientDrawable tg = new GradientDrawable();
        tg.setColor(0xB3FFFFFF);
        tg.setCornerRadius(11 * d);
        tg.setStroke((int) d, 0x66C9D6E8);
        timeBox.setBackgroundDrawable(tg);
        timeRow.addView(timeBox, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        LinearLayout.LayoutParams trlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        trlp.bottomMargin = (int) (10 * d);
        card.addView(timeRow, trlp);

        final ScrollView scroll = new ScrollView(act);
        scroll.setVerticalScrollBarEnabled(false);
        final LinearLayout list = new LinearLayout(act);
        list.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(list);
        LinearLayout.LayoutParams scr = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, (int) (screenH * 0.40f));
        scr.bottomMargin = (int) (8 * d);
        card.addView(scroll, scr);

        final List<Row> rows = new ArrayList<Row>();

        final Runnable[] rebuildHolder = new Runnable[1];
        final Runnable rebuild = new Runnable() {
            @Override
            public void run() {
                list.removeAllViews();

                for (int i = 0; i < rows.size(); i++) {
                    detach(rows.get(i).text);
                    detach(rows.get(i).nick);
                    detach(rows.get(i).uin);
                    detach(rows.get(i).gear);
                    detach(rows.get(i).extra);
                }
                for (int i = 0; i < rows.size(); i++) {
                    final Row r = rows.get(i);

                    LinearLayout cell = new LinearLayout(act);
                    cell.setOrientation(LinearLayout.VERTICAL);
                    LinearLayout.LayoutParams clp2 = new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT);
                    clp2.bottomMargin = (int) (9 * d);

                    LinearLayout line = new LinearLayout(act);
                    line.setOrientation(LinearLayout.HORIZONTAL);
                    line.setGravity(Gravity.CENTER_VERTICAL);

                    if (r.mediaJson != null) {
                        r.text.setText("[图片/表情素材]");
                        r.text.setTextColor(0xFF8A9CB5);
                        r.text.setFocusable(false);
                    } else {
                        r.text.setTextColor(C_TITLE);
                        r.text.setFocusableInTouchMode(true);
                    }
                    r.text.setTextSize(12.5f);
                    r.text.setSingleLine(true);
                    r.text.setInputType(InputType.TYPE_CLASS_TEXT);
                    r.text.setVerticalScrollBarEnabled(false);
                    r.text.setHint("说点什么…");
                    r.text.setHintTextColor(0xFFB3C0D2);
                    r.text.setPadding((int) (10 * d), (int) (9 * d),
                            (int) (10 * d), (int) (9 * d));
                    GradientDrawable eg = new GradientDrawable();
                    eg.setColor(0xB3FFFFFF);
                    eg.setCornerRadius(11 * d);
                    eg.setStroke((int) d, 0x66C9D6E8);
                    r.text.setBackgroundDrawable(eg);
                    line.addView(r.text, new LinearLayout.LayoutParams(0,
                            ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

                    r.gear.setText("⚙");
                    r.gear.setTextSize(15);
                    r.gear.setTextColor(0xFF6E93D6);
                    r.gear.setGravity(Gravity.CENTER);
                    GradientDrawable gg = new GradientDrawable();
                    gg.setColor(0x1A6E93D6);
                    gg.setCornerRadius(9 * d);
                    r.gear.setBackgroundDrawable(gg);
                    LinearLayout.LayoutParams glp = new LinearLayout.LayoutParams(
                            (int) (34 * d), (int) (34 * d));
                    glp.leftMargin = (int) (6 * d);
                    line.addView(r.gear, glp);

                    cell.addView(line);

                    r.extra = new LinearLayout(act);
                    r.extra.setOrientation(LinearLayout.VERTICAL);
                    r.extra.setPadding(0, (int) (6 * d), 0, 0);

                    LinearLayout nameRow = new LinearLayout(act);
                    nameRow.setOrientation(LinearLayout.HORIZONTAL);
                    r.nick.setTextSize(11.5f);
                    r.nick.setSingleLine(true);
                    r.nick.setHint("昵称（留空用服务器昵称）");
                    styleMini(r.nick, d);
                    r.uin.setTextSize(11.5f);
                    r.uin.setSingleLine(true);
                    r.uin.setHint("QQ号 → 决定头像");
                    r.uin.setInputType(InputType.TYPE_CLASS_NUMBER);
                    styleMini(r.uin, d);
                    LinearLayout.LayoutParams nlp = new LinearLayout.LayoutParams(0,
                            ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                    nlp.rightMargin = (int) (6 * d);
                    nameRow.addView(r.nick, nlp);
                    nameRow.addView(r.uin, new LinearLayout.LayoutParams(0,
                            ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                    r.extra.addView(nameRow);

                    TextView del = new TextView(act);
                    del.setText("删除这条");
                    del.setTextSize(11.5f);
                    del.setTextColor(0xFFE0708F);
                    del.setGravity(Gravity.CENTER);
                    GradientDrawable dgx = new GradientDrawable();
                    dgx.setColor(0x1AE0708F);
                    dgx.setCornerRadius(9 * d);
                    dgx.setStroke((int) d, 0x33E0708F);
                    del.setBackgroundDrawable(dgx);
                    LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    dlp.topMargin = (int) (6 * d);
                    del.setPadding(0, (int) (7 * d), 0, (int) (7 * d));
                    del.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            int at = rows.indexOf(r);
                            if (at < 0) return;
                            detach(r.text);
                            detach(r.nick);
                            detach(r.uin);
                            detach(r.gear);
                            detach(r.extra);
                            rows.remove(at);
                            rebuildHolder[0].run();
                        }
                    });
                    r.extra.addView(del, dlp);

                    r.extra.setVisibility(View.GONE);
                    cell.addView(r.extra);

                    r.gear.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            r.extra.setVisibility(
                                    r.extra.getVisibility() == View.VISIBLE
                                            ? View.GONE : View.VISIBLE);
                        }
                    });

                    list.addView(cell, clp2);
                }
            }
        };

        for (int i = 0; i < init.size(); i++) {
            String[] p = init.get(i);
            Row r = newRow(act);
            r.text.setText(p[2]);
            r.nick.setText(p[0]);
            r.uin.setText(p[1]);
            rows.add(r);
        }
        rebuildHolder[0] = rebuild;
        rebuild.run();

        TextView addBtn = ghost(act, d, "＋ 加一条");
        addBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Row r = newRow(act);
                rows.add(r);
                rebuild.run();
                scroll.post(new Runnable() {
                    @Override
                    public void run() {
                        scroll.fullScroll(View.FOCUS_DOWN);
                    }
                });
            }
        });
        LinearLayout.LayoutParams alp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        alp.bottomMargin = (int) (6 * d);
        card.addView(addBtn, alp);

        final String[][] kinds = {
                {"图片", com.cathelperawa.owo.app.proto.MediaVault.T_PIC
                        + "," + com.cathelperawa.owo.app.proto.MediaVault.T_FACE},
                {"语音", com.cathelperawa.owo.app.proto.MediaVault.T_PTT},
                {"视频", com.cathelperawa.owo.app.proto.MediaVault.T_VIDEO},
                {"卡片", com.cathelperawa.owo.app.proto.MediaVault.T_ARK},
                {"其他", com.cathelperawa.owo.app.proto.MediaVault.T_OTHER
                        + "," + com.cathelperawa.owo.app.proto.MediaVault.T_FILE},
        };
        LinearLayout mediaBox = new LinearLayout(act);
        mediaBox.setOrientation(LinearLayout.VERTICAL);
        LinearLayout mediaRow = null;
        for (int i = 0; i < kinds.length; i++) {
            if (i % 3 == 0) {
                mediaRow = new LinearLayout(act);
                mediaRow.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
                if (i > 0) rlp.topMargin = (int) (6 * d);
                mediaBox.addView(mediaRow, rlp);
            }
            final String kname = kinds[i][0];
            final String ktype = kinds[i][1];
            TextView b = ghost(act, d, kname);
            b.setTextSize(12);
            b.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    openMedia(act, kname, ktype, rows, rebuild);
                }
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            if (i % 3 > 0) lp.leftMargin = (int) (6 * d);
            mediaRow.addView(b, lp);
        }
        LinearLayout.LayoutParams mblp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        mblp.bottomMargin = (int) (10 * d);
        card.addView(mediaBox, mblp);

        LinearLayout buttons = new LinearLayout(act);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        TextView cancel = ghost(act, d, "取消");
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                try {
                    save(act, str(timeBox), rows);
                } catch (Throwable ignore) {
                }
                dlg.dismiss();
            }
        });
        TextView ok = pink(act, d, "生成并上传");
        ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                List<PbMsg.FakeItem> items = new ArrayList<PbMsg.FakeItem>();
                for (int i = 0; i < rows.size(); i++) {
                    Row r = rows.get(i);
                    long uin = parseLong(str(r.uin));
                    String nick = str(r.nick);
                    if (nick.length() == 0) nick = " ";
                    if (r.mediaJson != null) {
                        byte[] pb = mediaPb(r.mediaJson);
                        if (pb != null && pb.length > 0) {
                            String mt = com.cathelperawa.owo.app.proto.MediaVault
                                    .guessType(r.mediaJson);
                            PbMsg.FakeItem fi = new PbMsg.FakeItem(uin, nick, pb, mt);
                            fi.brief = com.cathelperawa.owo.app.proto.MediaVault
                                    .briefOf(r.mediaJson, mt);
                            items.add(fi);
                        }
                        continue;
                    }
                    String t = str(r.text);
                    if (t.length() == 0) continue;
                    items.add(new PbMsg.FakeItem(uin, nick, t));
                }
                if (items.isEmpty()) {
                    CatToast.show("一条消息都没填喵");
                    return;
                }
                String timeStr = str(timeBox);
                save(act, timeStr, rows);
                dlg.dismiss();
                cb.run(items, timeSec(timeStr));
            }
        });
        LinearLayout.LayoutParams b1 = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        b1.rightMargin = (int) (6 * d);
        LinearLayout.LayoutParams b2 = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1.4f);
        b2.leftMargin = (int) (6 * d);
        buttons.addView(cancel, b1);
        buttons.addView(ok, b2);
        card.addView(buttons);

        dlg.setContentView(root);
        Window w = dlg.getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(0));
            w.setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT);
            w.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
        dlg.show();

        root.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dlg.dismiss();
            }
        });
        card.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            }
        });
    }

    private static void openMedia(final Activity act, String title, final String type,
                                  final List<Row> rows, final Runnable rebuild) {
        String[] types = (type == null) ? null : type.split(",");
        final java.util.List<String[]> lib =
                com.cathelperawa.owo.app.proto.MediaVault.byTypes(act, types);
        final String[] names = new String[lib.size() + 1];
        names[0] = "＋ 粘贴新的元素 JSON";
        for (int i = 0; i < lib.size(); i++) {
            names[i + 1] = lib.get(i)[0];
        }
        CatSheet.pick(act, title + "素材 · " + lib.size() + " 条（长按管理）", names,
                new CatSheet.OnPick() {
                    @Override
                    public void run(int idx, String label) {
                        if (idx == 0) {
                            pasteNew(act, rows, rebuild);
                            return;
                        }
                        Row r = newRow(act);
                        r.mediaJson = lib.get(idx - 1)[1];
                        rows.add(r);
                        rebuild.run();
                    }
                }, new CatSheet.OnPick() {
                    @Override
                    public void run(int idx, String label) {
                        if (idx <= 0) return;
                        int real = 0;
                        try {
                            real = Integer.parseInt(lib.get(idx - 1)[3]);
                        } catch (Throwable ignore) {
                        }
                        manageMedia(act, real, rows, rebuild);
                    }
                });
    }

    private static void pasteNew(final Activity act, final List<Row> rows,
                                 final Runnable rebuild) {
        CatSheet.inputMulti(act, "粘贴元素 JSON",
                "{\"53\":{…}} 或 {\"2\":{…}}，从消息里提取的原样贴",
                "", new CatSheet.OnText() {
                    @Override
                    public void run(String text) {
                        if (text == null || text.trim().length() == 0) return;
                        if (mediaPb(text) == null) {
                            CatToast.show("这段 JSON 解析不了喵");
                            return;
                        }
                        addMedia(act, text.trim());
                        Row r = newRow(act);
                        r.mediaJson = text.trim();
                        rows.add(r);
                        rebuild.run();
                    }
                });
    }

    private static void manageMedia(final Activity act, final int index,
                                    final List<Row> rows, final Runnable rebuild) {
        final List<String[]> lib = mediaList(act);
        if (index < 0 || index >= lib.size()) return;
        final String name = lib.get(index)[0];
        CatSheet.pick(act, "素材 · " + name,
                new String[]{"改名", "删除", "批量删除"},
                new CatSheet.OnPick() {
                    @Override
                    public void run(int which, String label) {
                        if (which == 2) {
                            batchDelete(act, rows, rebuild);
                        } else if (which == 0) {
                            CatSheet.input(act, "改个名字", name, name,
                                    new CatSheet.OnText() {
                                        @Override
                                        public void run(String t) {
                                            com.cathelperawa.owo.app.proto.MediaVault
                                                    .rename(act, index, t);
                                            CatToast.show("改好了");
                                        }
                                    });
                        } else {
                            CatSheet.confirm(act, "删掉这条素材？",
                                    "「" + name + "」会从素材库移除，已经加进列表的不会变",
                                    "删除", true, new Runnable() {
                                        @Override
                                        public void run() {
                                            com.cathelperawa.owo.app.proto.MediaVault
                                                    .remove(act, index);
                                            CatToast.show("删了");
                                        }
                                    });
                        }
                    }
                });
    }

    private static void batchDelete(final Activity act, final List<Row> rows,
                                    final Runnable rebuild) {
        final List<String[]> lib = mediaList(act);
        if (lib.isEmpty()) {
            CatToast.show("素材库是空的");
            return;
        }
        final float d = act.getResources().getDisplayMetrics().density;
        final Dialog dlg = new Dialog(act, android.R.style.Theme_Translucent_NoTitleBar);
        dlg.requestWindowFeature(Window.FEATURE_NO_TITLE);

        FrameLayout root = new FrameLayout(act);
        root.setBackgroundColor(0x66000000);

        LinearLayout card = new LinearLayout(act);
        card.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xF2FFFFFF);
        bg.setCornerRadius(22 * d);
        bg.setStroke((int) d, 0x66C9D6E8);
        card.setBackgroundDrawable(bg);
        int pad = (int) (16 * d);
        card.setPadding(pad, pad, pad, pad);

        FrameLayout.LayoutParams clp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        clp.gravity = Gravity.BOTTOM;
        clp.leftMargin = (int) (10 * d);
        clp.rightMargin = (int) (10 * d);
        clp.bottomMargin = (int) (10 * d);
        root.addView(card, clp);

        TextView title = new TextView(act);
        title.setText("批量删除 · 点一下勾选");
        title.setTextSize(16);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(C_TITLE);
        card.addView(title);

        ScrollView scroll = new ScrollView(act);
        scroll.setVerticalScrollBarEnabled(false);
        LinearLayout list = new LinearLayout(act);
        list.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(list);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, (int) (act.getResources()
                .getDisplayMetrics().heightPixels * 0.45f));
        slp.topMargin = (int) (10 * d);
        slp.bottomMargin = (int) (10 * d);
        card.addView(scroll, slp);

        final boolean[] picked = new boolean[lib.size()];
        final TextView[] rowsTv = new TextView[lib.size()];
        for (int i = 0; i < lib.size(); i++) {
            final int fi = i;
            TextView tv = new TextView(act);
            tv.setText("○ " + lib.get(i)[0]);
            tv.setTextSize(13);
            tv.setTextColor(C_TITLE);
            tv.setPadding((int) (12 * d), (int) (11 * d), (int) (12 * d), (int) (11 * d));
            GradientDrawable rg = new GradientDrawable();
            rg.setCornerRadius(11 * d);
            rg.setColor(0x66FFFFFF);
            rg.setStroke((int) d, 0x33FFFFFF);
            tv.setBackgroundDrawable(rg);
            tv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    picked[fi] = !picked[fi];
                    rowsTv[fi].setText((picked[fi] ? "● " : "○ ") + lib.get(fi)[0]);
                    rowsTv[fi].setTextColor(picked[fi] ? 0xFFE0708F : C_TITLE);
                }
            });
            rowsTv[i] = tv;
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = (int) (6 * d);
            list.addView(tv, lp);
        }

        LinearLayout buttons = new LinearLayout(act);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        TextView cancel = ghost(act, d, "取消");
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { dlg.dismiss(); }
        });
        TextView ok = pink(act, d, "删除选中");
        ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int n = 0;

                for (int i = lib.size() - 1; i >= 0; i--) {
                    if (!picked[i]) continue;
                    com.cathelperawa.owo.app.proto.MediaVault.remove(act, i);
                    n++;
                }
                dlg.dismiss();
                CatToast.show(n > 0 ? ("删了 " + n + " 条") : "一条都没选");
            }
        });
        LinearLayout.LayoutParams b1 = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        b1.rightMargin = (int) (6 * d);
        LinearLayout.LayoutParams b2 = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1.2f);
        b2.leftMargin = (int) (6 * d);
        buttons.addView(cancel, b1);
        buttons.addView(ok, b2);
        card.addView(buttons);

        dlg.setContentView(root);
        Window w = dlg.getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(0));
            w.setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT);
        }
        dlg.show();
    }

    private static void detach(View v) {
        try {
            if (v == null) return;
            ViewParent p = v.getParent();
            if (p instanceof ViewGroup) ((ViewGroup) p).removeView(v);
        } catch (Throwable ignore) {
        }
    }

    private static Row newRow(Activity act) {
        Row r = new Row();
        r.text = new EditText(act);
        r.nick = new EditText(act);
        r.uin = new EditText(act);
        r.gear = new TextView(act);
        return r;
    }

    private static byte[] mediaPb(String json) {
        return com.cathelperawa.owo.app.proto.MediaVault.toPb(json);
    }

    private static byte[] mediaPbOld(String json) {
        try {
            String t = json == null ? "" : json.trim();
            org.json.JSONObject obj;
            if (t.startsWith("[")) {

                org.json.JSONArray arr = new org.json.JSONArray(t);
                obj = null;
                for (int i = 0; i < arr.length(); i++) {
                    org.json.JSONObject o = arr.optJSONObject(i);
                    if (o == null) continue;
                    if (o.has("53") || o.has("2")) {
                        obj = o;
                        break;
                    }
                }
                if (obj == null && arr.length() > 0) obj = arr.optJSONObject(0);
            } else {
                obj = new org.json.JSONObject(t);
            }
            if (obj == null) return null;
            com.cathelperawa.owo.app.ovo.FunProtoData pd =
                    new com.cathelperawa.owo.app.ovo.FunProtoData();
            pd.fromJSON(obj);
            byte[] b = pd.toBytes();
            return b != null && b.length > 0 ? b : null;
        } catch (Throwable t) {
            return null;
        }
    }

    private static java.util.List<String[]> mediaList(Activity act) {
        return com.cathelperawa.owo.app.proto.MediaVault.all(act);
    }

    private static java.util.List<String[]> mediaListOld(Activity act) {
        java.util.List<String[]> out = new java.util.ArrayList<String[]>();
        try {
            String raw = HookConfig.get(act, "fake_media_list", "");
            if (raw == null || raw.length() == 0) return out;
            org.json.JSONArray arr = new org.json.JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                org.json.JSONObject o = arr.getJSONObject(i);
                out.add(new String[]{o.optString("name", "?"), o.optString("json", "")});
            }
        } catch (Throwable ignore) {
        }
        return out;
    }

    private static void addMedia(Activity act, String json) {
        com.cathelperawa.owo.app.proto.MediaVault.add(act, json);
    }

    private static void addMediaOld(Activity act, String json) {
        try {
            java.util.List<String[]> lib = mediaList(act);
            if (lib.size() >= 12) lib.remove(lib.size() - 1);
            String name = guessName(json);
            org.json.JSONArray arr = new org.json.JSONArray();

            org.json.JSONObject n = new org.json.JSONObject();
            n.put("name", name);
            n.put("json", json);
            arr.put(n);
            for (int i = 0; i < lib.size(); i++) {
                org.json.JSONObject o = new org.json.JSONObject();
                o.put("name", lib.get(i)[0]);
                o.put("json", lib.get(i)[1]);
                arr.put(o);
            }
            HookConfig.set(act, "fake_media_list", arr.toString());
        } catch (Throwable ignore) {
        }
    }

    private static String guessName(String json) {
        try {
            if (json.contains("\"53\"")) return "表情 " + shortTime();
            if (json.contains("\"2\"")) return "图片 " + shortTime();
        } catch (Throwable ignore) {
        }
        return "素材 " + shortTime();
    }

    private static String shortTime() {
        Calendar c = Calendar.getInstance();
        return String.format("%02d:%02d", c.get(Calendar.HOUR_OF_DAY),
                c.get(Calendar.MINUTE));
    }

    private static void styleMini(EditText et, float d) {
        et.setTextSize(11.5f);
        et.setVerticalScrollBarEnabled(false);
        et.setHintTextColor(0xFFB3C0D2);
        et.setTextColor(C_TITLE);
        et.setPadding((int) (8 * d), (int) (6 * d), (int) (8 * d), (int) (6 * d));
        GradientDrawable g = new GradientDrawable();
        g.setColor(0x99FFFFFF);
        g.setCornerRadius(9 * d);
        g.setStroke((int) d, 0x55C9D6E8);
        et.setBackgroundDrawable(g);
    }

    private static TextView ghost(Activity act, float d, String label) {
        TextView t = new TextView(act);
        t.setText(label);
        t.setTextSize(12);
        t.setTextColor(C_TITLE);
        t.setGravity(Gravity.CENTER);
        t.setPadding(0, (int) (10 * d), 0, (int) (10 * d));
        GradientDrawable g = new GradientDrawable();
        g.setColor(0x8CFFFFFF);
        g.setCornerRadius(14 * d);
        g.setStroke((int) d, 0x55C9D6E8);
        t.setBackgroundDrawable(g);
        return t;
    }

    private static TextView pink(Activity act, float d, String label) {
        TextView t = new TextView(act);
        t.setText(label);
        t.setTextSize(14);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setTextColor(0xFFFFFFFF);
        t.setGravity(Gravity.CENTER);
        t.setPadding(0, (int) (11 * d), 0, (int) (11 * d));
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{0xFFFF8FBE, 0xFFF0709F});
        g.setCornerRadius(16 * d);
        t.setBackgroundDrawable(g);
        return t;
    }

    private static long parseLong(String s) {
        try {
            return Long.parseLong(s.replaceAll("[^0-9]", ""));
        } catch (Throwable t) {
            return 0L;
        }
    }

    private static String nowText() {
        Calendar c = Calendar.getInstance();
        return String.format("%04d-%02d-%02d %02d:%02d",
                c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1,
                c.get(Calendar.DAY_OF_MONTH), c.get(Calendar.HOUR_OF_DAY),
                c.get(Calendar.MINUTE));
    }

    public static long timeSec(String s) {
        long now = System.currentTimeMillis();
        if (s == null) return now / 1000;
        String t = s.trim();
        if (t.length() == 0) return now / 1000;
        try {
            Calendar c = Calendar.getInstance();
            Matcher m = Pattern.compile(
                    "(\\d{4})[-/年](\\d{1,2})[-/月](\\d{1,2})日?\\s+(\\d{1,2}):(\\d{2})")
                    .matcher(t);
            if (m.find()) {
                c.set(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)) - 1,
                        Integer.parseInt(m.group(3)), Integer.parseInt(m.group(4)),
                        Integer.parseInt(m.group(5)), 0);
                c.set(Calendar.MILLISECOND, 0);
                return c.getTimeInMillis() / 1000;
            }
            m = Pattern.compile("(\\d{1,2})[-/月](\\d{1,2})日?\\s+(\\d{1,2}):(\\d{2})")
                    .matcher(t);
            if (m.find()) {
                c.set(c.get(Calendar.YEAR), Integer.parseInt(m.group(1)) - 1,
                        Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3)),
                        Integer.parseInt(m.group(4)), 0);
                c.set(Calendar.MILLISECOND, 0);
                return c.getTimeInMillis() / 1000;
            }
            m = Pattern.compile("(\\d{1,2}):(\\d{2})").matcher(t);
            if (m.find()) {
                c.set(Calendar.HOUR_OF_DAY, Integer.parseInt(m.group(1)));
                c.set(Calendar.MINUTE, Integer.parseInt(m.group(2)));
                c.set(Calendar.SECOND, 0);
                c.set(Calendar.MILLISECOND, 0);
                return c.getTimeInMillis() / 1000;
            }
        } catch (Throwable ignore) {
        }
        return now / 1000;
    }

    private static Activity actOf(Context c) {
        Context t = c;
        while (t != null) {
            if (t instanceof Activity) return (Activity) t;
            if (!(t instanceof android.content.ContextWrapper)) return null;
            t = ((android.content.ContextWrapper) t).getBaseContext();
        }
        return null;
    }
}
