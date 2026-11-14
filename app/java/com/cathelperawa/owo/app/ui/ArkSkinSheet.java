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
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.cathelperawa.owo.app.bridge.CatToast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public final class ArkSkinSheet {

    private ArkSkinSheet() {
    }

    public interface OnDone {
        void run(String newJson);
    }

    private static final int C_TITLE = 0xFF3C4E6B;
    private static final int C_SUB = 0xFF8A9CB5;

    private static final String NEWS_ABSENT = "";

    private static final class Field {
        String path;
        String label;
        String value;
        boolean proto;
        boolean news;

        Field(String path, String label, String value, boolean proto, boolean news) {
            this.path = path;
            this.label = label;
            this.value = value;
            this.proto = proto;
            this.news = news;
        }
    }

    public static void open(Context anyCtx, String arkJson, final OnDone cb) {
        open(anyCtx, arkJson, "确定", cb);
    }

    public static void open(Context anyCtx, String arkJson, String okLabel, final OnDone cb) {
        final Activity act = actOf(anyCtx);
        if (act == null || arkJson == null) return;
        final String src = arkJson.trim();
        if (src.length() == 0) return;

        final JSONObject root;
        try {
            root = new JSONObject(src);
        } catch (Throwable t) {
            CatToast.show("这不是合法的卡片 JSON 喵");
            return;
        }

        final List<Field> all = new ArrayList<Field>();
        walk(root, "", "", all);

        final List<Field> fields = new ArrayList<Field>();
        for (int i = 0; i < all.size(); i++) if (!all.get(i).proto) fields.add(all.get(i));
        for (int i = 0; i < all.size(); i++) if (all.get(i).proto) fields.add(all.get(i));

        if (fields.isEmpty()) {
            CatToast.show("这张卡里没有可改的文字字段");
            return;
        }

        String newsBase = null;
        String newsTpl = null;
        for (int i = 0; i < fields.size(); i++) {
            if (!fields.get(i).news) continue;
            String p = fields.get(i).path;
            String[] seg = p.split("/");
            if (seg.length < 3) continue;
            StringBuilder base = new StringBuilder();
            for (int k = 0; k < seg.length - 2; k++) {
                if (k > 0) base.append("/");
                base.append(seg[k]);
            }
            newsBase = base.toString();
            newsTpl = templateAt(root, newsBase);
            break;
        }

        final String nBase = newsBase;
        final String nTpl = newsTpl;
        final String label = okLabel;
        final String app = root.optString("app", "");
        reindexNews(fields);

        act.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                build(act, src, fields, nBase, nTpl, label, app, cb);
            }
        });
    }

    private static void build(final Activity act, final String src,
                              final List<Field> fields, final String newsBase,
                              final String newsTpl, final String okLabel,
                              final String app, final OnDone cb) {
        final float d = act.getResources().getDisplayMetrics().density;
        final int screenH = act.getResources().getDisplayMetrics().heightPixels;

        final Dialog dlg = new Dialog(act, android.R.style.Theme_Translucent_NoTitleBar);
        dlg.requestWindowFeature(Window.FEATURE_NO_TITLE);

        FrameLayout wrapRoot = new FrameLayout(act);
        wrapRoot.setBackgroundColor(0x66000000);

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
        wrapRoot.addView(card, clp);

        final TextView title = new TextView(act);
        title.setTextSize(16);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(C_TITLE);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tlp.bottomMargin = (int) (4 * d);
        card.addView(title, tlp);

        TextView sub = new TextView(act);
        sub.setText(com.cathelperawa.owo.app.feature.ArkSmart.badge(src)
                + "\n只改显示用的字符串 · 标「协议」的别乱动");
        sub.setTextSize(11);
        sub.setTextColor(C_SUB);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        slp.bottomMargin = (int) (8 * d);
        card.addView(sub, slp);

        final ScrollView scroll = new ScrollView(act);

        scroll.setVerticalScrollBarEnabled(false);
        scroll.setHorizontalScrollBarEnabled(false);
        scroll.setFadingEdgeLength((int) (18 * d));
        final LinearLayout list = new LinearLayout(act);
        list.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(list);

        final int viewH = (int) (screenH * 0.44f);
        final FrameLayout scrollBox = new FrameLayout(act);
        scrollBox.addView(scroll, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, viewH));
        final View thumb = new View(act);
        GradientDrawable tb = new GradientDrawable();
        tb.setColor(0x40A9B7CA);
        tb.setCornerRadius(3 * d);
        thumb.setBackgroundDrawable(tb);
        FrameLayout.LayoutParams thumbLp = new FrameLayout.LayoutParams(
                (int) (2.5f * d), (int) (36 * d));
        thumbLp.gravity = Gravity.RIGHT | Gravity.TOP;
        thumbLp.rightMargin = (int) (1 * d);
        thumbLp.topMargin = (int) (4 * d);
        scrollBox.addView(thumb, thumbLp);

        scroll.getViewTreeObserver().addOnScrollChangedListener(
                new android.view.ViewTreeObserver.OnScrollChangedListener() {
                    @Override
                    public void onScrollChanged() {
                        try {
                            if (list.getHeight() <= scroll.getHeight()) {
                                thumb.setVisibility(View.GONE);
                                return;
                            }
                            thumb.setVisibility(View.VISIBLE);
                            int track = scroll.getHeight() - (int) (8 * d);
                            int ratio = Math.max(1, (int) (track
                                    * ((float) scroll.getHeight() / list.getHeight())));
                            ViewGroup.LayoutParams lp = thumb.getLayoutParams();
                            if (lp.height != ratio) {
                                lp.height = ratio;
                                thumb.setLayoutParams(lp);
                            }
                            int range = list.getHeight() - scroll.getHeight();
                            float p = range <= 0 ? 0 : (float) scroll.getScrollY() / range;
                            thumb.setTranslationY(p * (track - ratio));
                        } catch (Throwable ignore) {
                        }
                    }
                });

        LinearLayout.LayoutParams scr = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, viewH);
        scr.bottomMargin = (int) (10 * d);
        card.addView(scrollBox, scr);

        final List<EditText> boxes = new ArrayList<EditText>();
        final List<Integer> boxOf = new ArrayList<Integer>();

        final Runnable sync = new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < boxes.size() && i < boxOf.size(); i++) {
                    EditText et = boxes.get(i);
                    Field f = fields.get(boxOf.get(i));
                    if (et == null || f == null) continue;
                    f.value = et.getText() == null ? "" : et.getText().toString();
                }
            }
        };

        final Runnable[] rebuildHolder = new Runnable[1];
        final Runnable rebuild = new Runnable() {
            @Override
            public void run() {
                list.removeAllViews();
                boxes.clear();
                boxOf.clear();
                title.setText("识别卡片 · " + fields.size() + " 个字段");

                for (int i = 0; i < fields.size(); i++) {
                    final int fi = i;
                    final Field f = fields.get(i);

                    LinearLayout cell = new LinearLayout(act);
                    cell.setOrientation(LinearLayout.VERTICAL);
                    LinearLayout.LayoutParams clp2 = new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT);
                    clp2.bottomMargin = (int) (8 * d);

                    TextView lb = new TextView(act);
                    String hint = f.proto ? "协议，别动" : hintOf(f, app);
                    String nm = nameOf(f.label);
                    String head = (nm == null || nm.equals(f.label))
                            ? f.label : nm + "（" + f.label + "）";
                    lb.setText(hint == null ? head : (head + " · " + hint));
                    lb.setTextSize(10);
                    lb.setTextColor(f.proto ? 0xFFC98A8A : C_SUB);
                    lb.setSingleLine(true);
                    lb.setEllipsize(android.text.TextUtils.TruncateAt.END);
                    cell.addView(lb);

                    LinearLayout line = new LinearLayout(act);
                    line.setOrientation(LinearLayout.HORIZONTAL);
                    line.setGravity(Gravity.CENTER_VERTICAL);

                    final EditText et = new EditText(act);
                    et.setText(f.value);
                    et.setTextSize(12.5f);
                    et.setTextColor(C_TITLE);
                    et.setTypeface(Typeface.MONOSPACE);
                    et.setSingleLine(true);
                    et.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
                    et.setInputType(InputType.TYPE_CLASS_TEXT);
                    et.setVerticalScrollBarEnabled(false);
                    et.setHorizontalScrollBarEnabled(false);
                    et.setPadding((int) (10 * d), (int) (8 * d),
                            (int) (10 * d), (int) (8 * d));
                    GradientDrawable eg = new GradientDrawable();
                    eg.setColor(f.proto ? 0x33F0F0F0 : 0xB3FFFFFF);
                    eg.setCornerRadius(11 * d);
                    eg.setStroke((int) d, 0x66C9D6E8);
                    et.setBackgroundDrawable(eg);
                    LinearLayout.LayoutParams elp = new LinearLayout.LayoutParams(0,
                            ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                    line.addView(et, elp);
                    boxes.add(et);
                    boxOf.add(fi);

                    if (!f.proto && looksImage(f)) {
                        TextView pv = miniBtn(act, d, "预览");
                        pv.setOnClickListener(new View.OnClickListener() {
                            @Override public void onClick(View v) {
                                String u = et.getText() == null ? "" : et.getText().toString().trim();
                                if (u.length() == 0) { CatToast.show("这个字段还是空的喵"); return; }
                                previewImage(act, u);
                            }
                        });
                        LinearLayout.LayoutParams pvl = new LinearLayout.LayoutParams(
                                (int) (38 * d), (int) (30 * d));
                        pvl.leftMargin = (int) (6 * d);
                        line.addView(pv, pvl);

                        TextView up = miniBtn(act, d, "上传");
                        up.setOnClickListener(new View.OnClickListener() {
                            @Override public void onClick(View v) {
                                pickAndUpload(act, et);
                            }
                        });
                        LinearLayout.LayoutParams upl = new LinearLayout.LayoutParams(
                                (int) (38 * d), (int) (30 * d));
                        upl.leftMargin = (int) (4 * d);
                        line.addView(up, upl);
                    } else if (!f.proto && looksVideo(f)) {
                        TextView pl = miniBtn(act, d, "播放");
                        pl.setOnClickListener(new View.OnClickListener() {
                            @Override public void onClick(View v) {
                                String u = et.getText() == null ? "" : et.getText().toString().trim();
                                if (u.length() == 0) { CatToast.show("没有视频地址喵"); return; }
                                playVideo(act, u);
                            }
                        });
                        LinearLayout.LayoutParams pll = new LinearLayout.LayoutParams(
                                (int) (42 * d), (int) (30 * d));
                        pll.leftMargin = (int) (6 * d);
                        line.addView(pl, pll);
                    }

                    if (f.news) {
                        TextView del = new TextView(act);
                        del.setText("×");
                        del.setTextSize(17);
                        del.setTextColor(0xFFE0708F);
                        del.setGravity(Gravity.CENTER);
                        GradientDrawable dg = new GradientDrawable();
                        dg.setColor(0x1AE0708F);
                        dg.setCornerRadius(9 * d);
                        del.setBackgroundDrawable(dg);
                        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(
                                (int) (30 * d), (int) (30 * d));
                        dlp.leftMargin = (int) (6 * d);
                        line.addView(del, dlp);
                        del.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                sync.run();
                                if (fi < fields.size()) fields.remove(fi);
                                reindexNews(fields);
                                rebuildHolder[0].run();
                            }
                        });
                    }

                    cell.addView(line);
                    list.addView(cell, clp2);
                }
            }
        };
        rebuildHolder[0] = rebuild;
        rebuild.run();

        LinearLayout tools = new LinearLayout(act);
        tools.setOrientation(LinearLayout.HORIZONTAL);
        TextView uidBtn = ghost(act, d, "换 UUID");
        TextView sumBtn = ghost(act, d, "条数对齐");
        TextView defBtn = ghost(act, d, "默认化");
        LinearLayout.LayoutParams g1 = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        g1.rightMargin = (int) (4 * d);
        LinearLayout.LayoutParams g2 = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        g2.leftMargin = (int) (4 * d);
        g2.rightMargin = (int) (4 * d);
        LinearLayout.LayoutParams g3 = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        g3.leftMargin = (int) (4 * d);
        tools.addView(uidBtn, g1);
        tools.addView(sumBtn, g2);
        tools.addView(defBtn, g3);
        LinearLayout.LayoutParams tplp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tplp.bottomMargin = (int) (8 * d);
        card.addView(tools, tplp);

        uidBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sync.run();
                int n = 0;
                for (int i = 0; i < fields.size(); i++) {
                    Field f = fields.get(i);
                    if ("uniseq".equals(f.label) || "filename".equals(f.label)) {
                        f.value = newUuid();
                        n++;
                    }
                }
                rebuild.run();
                CatToast.show(n > 0 ? ("换了 " + n + " 个 UUID") : "这张卡里没有 UUID 字段");
            }
        });

        sumBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sync.run();
                int news = countNews(fields);
                if (news <= 0) {
                    CatToast.show("这张卡里没有 news 预览行");
                    return;
                }
                int n = 0;
                for (int i = 0; i < fields.size(); i++) {
                    Field f = fields.get(i);
                    if ("tsum".equals(f.label)) {
                        f.value = String.valueOf(news);
                        n++;
                    } else if ("summary".equals(f.label)
                            && f.value.matches(".*\\d+\\s*条.*")) {
                        f.value = f.value.replaceAll("\\d+\\s*条", news + "条");
                        n++;
                    }
                }
                rebuild.run();
                CatToast.show(n > 0 ? ("已对齐 " + news + " 条") : "没找到 tsum/summary");
            }
        });

        defBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sync.run();
                int n = 0;
                for (int i = 0; i < fields.size(); i++) {
                    Field f = fields.get(i);
                    if (f.proto) continue;
                    if (f.value == null || f.value.length() == 0) continue;
                    f.value = isGeo(f.label) ? "0" : "";
                    n++;
                }
                rebuild.run();
                CatToast.show(n > 0 ? ("已清空 " + n + " 项") : "本来就是空的");
            }
        });

        if (newsBase != null) {
            TextView add = ghost(act, d, "＋ 加一条");
            add.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    sync.run();
                    int at = lastNewsIndex(fields);
                    Field f = new Field("", "预览行", "", false, true);
                    if (at < 0) fields.add(f);
                    else fields.add(at + 1, f);
                    reindexNews(fields);
                    rebuild.run();
                }
            });
            LinearLayout.LayoutParams alp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            alp.bottomMargin = (int) (8 * d);
            card.addView(add, alp);
        }

        LinearLayout buttons = new LinearLayout(act);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        TextView cancel = ghost(act, d, "取消");
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dlg.dismiss();
            }
        });
        TextView ok = pink(act, d, okLabel == null ? "确定" : okLabel);
        ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    sync.run();
                    String nj = apply(src, fields, newsBase, newsTpl);
                    dlg.dismiss();
                    cb.run(nj);
                } catch (Throwable t) {
                    CatToast.show("写回失败：" + t);
                }
            }
        });
        LinearLayout.LayoutParams b1 = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        b1.rightMargin = (int) (6 * d);
        LinearLayout.LayoutParams b2 = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        b2.leftMargin = (int) (6 * d);
        buttons.addView(cancel, b1);
        buttons.addView(ok, b2);
        card.addView(buttons);

        dlg.setContentView(wrapRoot);
        Window w = dlg.getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(0));
            w.setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT);
            w.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
        dlg.show();

        wrapRoot.setOnClickListener(new View.OnClickListener() {
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

    private static void walk(Object node, String path, String label, List<Field> out) {
        if (node == null) return;
        if (node instanceof JSONObject) {
            JSONObject o = (JSONObject) node;
            Iterator<String> it = o.keys();
            while (it.hasNext()) {
                String k = it.next();
                String p = path.length() == 0 ? k : path + "/" + k;
                walk(o.opt(k), p, k, out);
            }
        } else if (node instanceof JSONArray) {
            JSONArray a = (JSONArray) node;
            for (int i = 0; i < a.length(); i++) {
                String p = path + "/" + i;
                walk(a.opt(i), p, label + "[" + i + "]", out);
            }
        } else if (node instanceof String) {
            boolean news = path.matches(".*/news/\\d+/text");
            out.add(new Field(path, label, (String) node, isProto(path), news));
        }
    }

    private static String nameOf(String k) {
        if (k == null) return "";
        String s = k.toLowerCase();
        if (s.equals("bizsrc")) return "来源标识";
        if (s.equals("musicurl")) return "音频地址";
        if (s.equals("jumpurl")) return "跳转地址";
        if (s.equals("preview")) return "封面图";
        if (s.equals("tagicon")) return "角标图标";
        if (s.equals("tag")) return "标签文字";
        if (s.equals("title")) return "标题";
        if (s.equals("desc")) return "描述";
        if (s.equals("prompt")) return "列表提示语";
        if (s.equals("token")) return "签名凭证";
        if (s.equals("uin")) return "账号";
        if (s.equals("appid")) return "应用 ID";
        if (s.equals("app_type")) return "应用类型";
        if (s.equals("ctime")) return "时间戳";
        if (s.equals("forward")) return "可转发";
        if (s.equals("app")) return "应用标识";
        if (s.equals("view")) return "卡片类型";
        if (s.equals("ver")) return "版本号";
        if (s.equals("type")) return "类型";
        if (s.equals("summary")) return "摘要";
        if (s.equals("source")) return "来源文字";
        if (s.equals("text")) return "正文";
        if (s.equals("name")) return "名称";
        if (s.equals("address")) return "地址文字";
        return k;
    }

    private static String hintOf(Field f, String app) {
        if (f == null) return null;
        if (f.news) return "点开前的一行预览，一般写「昵称: 内容」";
        String k = f.label == null ? "" : f.label.toLowerCase();
        String a = app == null ? "" : app.toLowerCase();
        boolean multi = a.contains("multimsg");
        boolean map = a.contains("map");
        if (multi) {
            if (k.equals("desc")) return "卡片最上面那行字";
            if (k.equals("prompt")) return "点开前的提示语";
            if (k.equals("source")) return "卡上的来源，XX的聊天记录";
            if (k.equals("summary")) return "底部「查看N条转发消息」";
            if (k.equals("extra")) return "附加信息：文件名 + 条数";
            if (k.equals("tsum")) return "总条数，与预览行数一致";
            if (k.equals("news")) return "预览行列表";
        }
        if (map) {
            if (k.equals("address")) return "地址文字";
            if (k.equals("name")) return "地点名称";
            if (k.equals("lat")) return "纬度，0 = 未知";
            if (k.equals("lng") || k.equals("lon")) return "经度，0 = 未知";
            if (k.equals("prompt")) return "聊天里显示的 [位置]xxx";
            if (k.equals("from")) return "来源标记，别改";
            if (k.equals("id")) return "地点 ID，可留空";
            if (k.equals("desc")) return "描述，一般空着";
        }
        if (a.contains("music")) {
            if (k.equals("title")) return "歌名，卡片上那行大字";
            if (k.equals("desc")) return "歌手名";
            if (k.equals("musicurl")) return "点播放就取这个地址，必须能直接播";
            if (k.equals("preview")) return "封面图地址";
            if (k.equals("tag")) return "左下角那行字（QQ音乐/网易云音乐）";
            if (k.equals("tagicon")) return "左下角标签旁的小图标";
            if (k.equals("prompt")) return "聊天列表里显示的 [分享]歌名";
            if (k.equals("bizsrc")) return "卡片来源，别改";
        }
        if (k.equals("bizsrc")) return "卡片来源标识，别改";
        if (k.equals("preview")) return "卡片右侧那张图";
        if (k.equals("tagicon")) return "标签旁的小图标";
        if (k.equals("desc")) return "卡片标题";
        if (k.equals("prompt")) return "提示语";
        if (k.equals("title")) return "标题";
        if (k.equals("summary")) return "摘要";
        if (k.equals("source")) return "来源";
        if (k.equals("text")) return "正文";
        if (k.equals("tag")) return "标签";
        if (k.equals("name")) return "名称";
        if (k.equals("jumpUrl") || k.equals("jumpurl")) return "点击跳转地址";
        return null;
    }

    private static boolean isProto(String path) {
        String s = path.toLowerCase();
        return s.endsWith("/app") || s.equals("app")
                || s.endsWith("/ver") || s.equals("ver")
                || s.endsWith("/view") || s.equals("view")
                || s.endsWith("token")
                || s.endsWith("resid")
                || s.endsWith("uniseq")
                || s.endsWith("filename")
                || s.endsWith("from_account")
                || s.endsWith("uint64_peer_account")
                || s.endsWith("uint64_peer_uin")
                || s.endsWith("ctime")
                || s.endsWith("/type") || s.equals("type");
    }

    private static boolean isGeo(String label) {
        if (label == null) return false;
        String l = label.toLowerCase();
        return l.equals("lat") || l.equals("lng") || l.equals("lon")
                || l.equals("latitude") || l.equals("longitude");
    }

    private static int countNews(List<Field> fields) {
        int n = 0;
        for (int i = 0; i < fields.size(); i++) if (fields.get(i).news) n++;
        return n;
    }

    private static int lastNewsIndex(List<Field> fields) {
        int at = -1;
        for (int i = 0; i < fields.size(); i++) if (fields.get(i).news) at = i;
        return at;
    }

    private static void reindexNews(List<Field> fields) {
        int idx = 0;
        for (int i = 0; i < fields.size(); i++) {
            Field f = fields.get(i);
            if (!f.news) continue;
            String p = f.path;
            if (p == null || p.length() == 0) continue;
            String[] seg = p.split("/");
            if (seg.length < 3) continue;
            seg[seg.length - 2] = String.valueOf(idx);
            StringBuilder sb = new StringBuilder();
            for (int k = 0; k < seg.length; k++) {
                if (k > 0) sb.append("/");
                sb.append(seg[k]);
            }
            f.path = sb.toString();
            f.label = "预览行 " + (idx + 1);
            idx++;
        }
    }

    private static String templateAt(JSONObject root, String base) {
        try {
            Object cur = root;
            String[] seg = base.split("/");
            for (int i = 0; i < seg.length; i++) {
                if (cur instanceof JSONObject) cur = ((JSONObject) cur).opt(seg[i]);
                else if (cur instanceof JSONArray) cur = ((JSONArray) cur).opt(Integer.parseInt(seg[i]));
                else return null;
            }
            if (cur instanceof JSONArray && ((JSONArray) cur).length() > 0) {
                Object e = ((JSONArray) cur).opt(0);
                if (e instanceof JSONObject) return ((JSONObject) e).toString();
            }
        } catch (Throwable ignore) {
        }
        return null;
    }

    private static String apply(String src, List<Field> fields, String newsBase,
                                String newsTpl) throws Exception {
        JSONObject out = new JSONObject(src);
        for (int i = 0; i < fields.size(); i++) {
            Field f = fields.get(i);
            if (f.news) continue;
            setByPath(out, f.path, f.value);
        }
        if (newsBase != null) {
            JSONArray arr = new JSONArray();
            for (int i = 0; i < fields.size(); i++) {
                Field f = fields.get(i);
                if (!f.news) continue;
                JSONObject tpl;
                try {
                    tpl = newsTpl == null ? new JSONObject() : new JSONObject(newsTpl);
                } catch (Throwable t) {
                    tpl = new JSONObject();
                }
                tpl.put("text", f.value == null ? "" : f.value);
                arr.put(tpl);
            }
            setObjByPath(out, newsBase, arr);
        }
        return out.toString();
    }

    private static void setByPath(JSONObject root, String path, String value) {
        try {
            String[] seg = path.split("/");
            Object cur = root;
            for (int i = 0; i < seg.length - 1; i++) {
                if (cur instanceof JSONObject) {
                    cur = ((JSONObject) cur).opt(seg[i]);
                } else if (cur instanceof JSONArray) {
                    cur = ((JSONArray) cur).opt(Integer.parseInt(seg[i]));
                } else {
                    return;
                }
                if (cur == null) return;
            }
            String last = seg[seg.length - 1];
            if (cur instanceof JSONObject) {
                ((JSONObject) cur).put(last, value);
            } else if (cur instanceof JSONArray) {
                ((JSONArray) cur).put(Integer.parseInt(last), value);
            }
        } catch (Throwable ignore) {
        }
    }

    private static void setObjByPath(JSONObject root, String path, Object value) {
        try {
            String[] seg = path.split("/");
            Object cur = root;
            for (int i = 0; i < seg.length - 1; i++) {
                if (cur instanceof JSONObject) {
                    cur = ((JSONObject) cur).opt(seg[i]);
                } else if (cur instanceof JSONArray) {
                    cur = ((JSONArray) cur).opt(Integer.parseInt(seg[i]));
                } else {
                    return;
                }
                if (cur == null) return;
            }
            String last = seg[seg.length - 1];
            if (cur instanceof JSONObject) {
                ((JSONObject) cur).put(last, value);
            } else if (cur instanceof JSONArray) {
                ((JSONArray) cur).put(Integer.parseInt(last), value);
            }
        } catch (Throwable ignore) {
        }
    }

    private static String newUuid() {
        return UUID.randomUUID().toString();
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

    private static TextView miniBtn(Activity a, float d, String s) {
        TextView t = new TextView(a);
        t.setText(s);
        t.setTextSize(10.5f);
        t.setTextColor(0xFF3A6FD8);
        t.setGravity(Gravity.CENTER);
        GradientDrawable g = new GradientDrawable();
        g.setColor(0x1A3A6FD8);
        g.setCornerRadius(9 * d);
        t.setBackgroundDrawable(g);
        return t;
    }

    private static boolean looksImage(Field f) {
        String k = ((f.label == null ? "" : f.label) + " " + (f.path == null ? "" : f.path))
                .toLowerCase();
        if (k.contains("cover") || k.contains("icon") || k.contains("preview")
                || k.contains("image") || k.contains("img") || k.contains("pic")
                || k.contains("封面") || k.contains("图标") || k.contains("图片")
                || k.contains("头像") || k.contains("avatar")) {
            return true;
        }
        String v = f.value == null ? "" : f.value.toLowerCase();
        return v.startsWith("http") && (v.contains(".png") || v.contains(".jpg")
                || v.contains(".jpeg") || v.contains(".webp") || v.contains(".gif")
                || v.contains("qpic.cn") || v.contains("qlogo.cn") || v.contains("ugcimg.cn"));
    }

    private static boolean looksVideo(Field f) {
        String v = f.value == null ? "" : f.value.toLowerCase();
        String k = ((f.label == null ? "" : f.label) + " " + (f.path == null ? "" : f.path))
                .toLowerCase();
        if (v.contains(".mp4") || v.contains(".mov") || v.contains(".m3u8")) return true;
        return k.contains("video") && v.startsWith("http") && !looksImage(f);
    }

    public static void previewImage(final Activity act, final String url) {
        final Dialog d = new Dialog(act, android.R.style.Theme_Translucent_NoTitleBar);
        FrameLayout root = new FrameLayout(act);
        root.setBackgroundColor(0xCC000000);
        final android.widget.ImageView iv = new android.widget.ImageView(act);
        iv.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        lp.setMargins(12, 40, 12, 40);
        root.addView(iv, lp);
        TextView tip = new TextView(act);
        tip.setText("加载中…");
        tip.setTextColor(0xFFFFFFFF);
        tip.setTextSize(12);
        tip.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams tp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tp.gravity = Gravity.BOTTOM;
        tp.bottomMargin = 70;
        root.addView(tip, tp);
        root.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { d.dismiss(); }
        });
        d.setContentView(root);
        Window w = d.getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(0));
            w.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }
        d.show();
        final TextView ftip = tip;
        com.cathelperawa.owo.app.core.Bg.run(new Runnable() {
            @Override public void run() {
                android.graphics.Bitmap bmp = fetchBitmap(url);
                act.runOnUiThread(new Runnable() {
                    @Override public void run() {
                        if (bmp != null) {
                            iv.setImageBitmap(bmp);
                            ftip.setText("点击关闭 · " + url);
                        } else {
                            ftip.setText("图加载失败（可能被防盗链或已过期）\n" + url);
                        }
                    }
                });
            }
        }, "ark-preview");
    }

    private static android.graphics.Bitmap fetchBitmap(String url) {
        java.net.HttpURLConnection c = null;
        java.io.InputStream in = null;
        try {
            java.net.URL u = new java.net.URL(url);
            c = (java.net.HttpURLConnection) u.openConnection();
            c.setConnectTimeout(8000);
            c.setReadTimeout(15000);
            c.setRequestProperty("User-Agent",
                    "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 Chrome/120 Mobile Safari/537.36");
            c.setRequestProperty("Referer", "https://qq.com/");
            c.setInstanceFollowRedirects(true);
            in = c.getInputStream();
            return android.graphics.BitmapFactory.decodeStream(in);
        } catch (Throwable t) {
            com.cathelperawa.owo.app.core.HookLog.w("预览图失败：" + t);
            return null;
        } finally {
            try { if (in != null) in.close(); } catch (Throwable ignore) {}
            try { if (c != null) c.disconnect(); } catch (Throwable ignore) {}
        }
    }

    public static void pickAndUpload(final Activity act, final EditText target) {
        PickerBridge.pick(act, new PickerBridge.OnPick() {
            @Override public void run(final String path) {
                if (path == null || path.length() == 0) {
                    CatToast.show("没选到图");
                    return;
                }
                CatToast.show("上传图片中…");
                com.cathelperawa.owo.app.proto.FakeMediaUpload.upload(act, path,
                        new com.cathelperawa.owo.app.proto.FakeMediaUpload.OnOk() {
                            @Override public void run(String elemJson) {
                                String url = urlFromElem(elemJson);
                                if (url == null) {
                                    CatToast.show("上传成功但没解析出链接");
                                    return;
                                }
                                act.runOnUiThread(new Runnable() {
                                    @Override public void run() {
                                        target.setText(url);
                                        CatToast.show("已上传并填入");
                                    }
                                });
                            }
                        },
                        new com.cathelperawa.owo.app.proto.FakeMediaUpload.OnFail() {
                            @Override public void run(final String reason) {
                                act.runOnUiThread(new Runnable() {
                                    @Override public void run() {
                                        CatToast.show("上传失败：" + reason);
                                    }
                                });
                            }
                        });
            }
        });
    }

    private static String urlFromElem(String elemJson) {
        if (elemJson == null) return null;

        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("https?://[^\"\\\\ ]+").matcher(elemJson);
        while (m.find()) {
            String u = m.group();
            if (u.contains("qpic.cn") || u.contains("qlogo.cn") || u.contains("ugcimg.cn")) {
                return u.replace("\\/", "/");
            }
        }

        m = java.util.regex.Pattern.compile("\\b[0-9a-fA-F]{32}\\b").matcher(elemJson);
        if (m.find()) {
            return "https://gchat.qpic.cn/gchatpic_new/0/0-0-"
                    + m.group().toUpperCase() + "/0";
        }
        return null;
    }

    public static void playVideo(Activity act, String url) {
        try {
            android.content.Intent i = new android.content.Intent(android.content.Intent.ACTION_VIEW);
            i.setDataAndType(android.net.Uri.parse(url), "video/*");
            i.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
            act.startActivity(i);
        } catch (Throwable t) {
            CatToast.show("打不开播放器：" + t);
        }
    }
}
