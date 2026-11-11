package com.cathelperawa.owo.app.ai;

import android.app.Activity;
import com.cathelperawa.owo.app.core.Bg;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.cathelperawa.owo.app.ui.CatSheet;
import com.cathelperawa.owo.app.ui.Page;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class AiEmojiPage extends Page {

    private static final int C_MAIN = 0xFF3C4B60;
    private static final int C_SUB = 0xFF8A9BB0;
    private static final int C_PINK = 0xFFE85D9C;

    public interface Picker {
        void onPicked(List<Uri> uris);
    }

    private static Picker pending;

    public static void deliver(List<Uri> uris) {
        Picker p = pending;
        pending = null;
        if (p != null && uris != null && !uris.isEmpty()) p.onPicked(uris);
    }

    private LinearLayout body;
    private LinearLayout content;
    private TextView btnClassify;

    private String currentLabel = null;

    public AiEmojiPage(Host host) {
        super(host);
    }

    @Override
    public boolean onBack() {
        if (currentLabel != null) {
            currentLabel = null;
            render();
            return true;
        }
        return false;
    }

    @Override
    public View build() {
        View[] out = new View[2];
        body = pageRoot("表情库", "它聊天时能自己挑一张发", out, true, 13, "bg_ai", 0x66);
        content = new LinearLayout(ctx);
        content.setOrientation(LinearLayout.VERTICAL);
        body.addView(content, matchWrap(0));
        render();
        return out[0];
    }

    private void render() {
        content.removeAllViews();
        List<AiEmoji.Item> list = AiEmoji.load();
        if (currentLabel == null) renderGroups(list);
        else renderGroup(list);
    }

    

    private void renderGroups(List<AiEmoji.Item> list) {
        
        final Map<String, List<AiEmoji.Item>> map =
                new LinkedHashMap<String, List<AiEmoji.Item>>();
        for (int i = 0; i < list.size(); i++) {
            AiEmoji.Item it = list.get(i);
            String k = it.label == null || it.label.length() == 0 ? "未分类" : it.label;
            List<AiEmoji.Item> g = map.get(k);
            if (g == null) {
                g = new ArrayList<AiEmoji.Item>();
                map.put(k, g);
            }
            g.add(it);
        }
        
        List<String> custom = AiEmoji.groups();
        for (int i = 0; i < custom.size(); i++) {
            String g = custom.get(i);
            if (!map.containsKey(g)) map.put(g, new ArrayList<AiEmoji.Item>());
        }
        List<String> keys = new ArrayList<String>(map.keySet());
        java.util.Collections.sort(keys, new java.util.Comparator<String>() {
            @Override public int compare(String a, String b) {
                boolean ua = "未分类".equals(a), ub = "未分类".equals(b);
                if (ua != ub) return ua ? 1 : -1;
                int na = map.get(a).size(), nb = map.get(b).size();
                
                if ((na == 0) != (nb == 0)) return na == 0 ? 1 : -1;
                return na == nb ? a.compareTo(b) : (nb - na);
            }
        });

        int unclassified = map.containsKey("未分类") ? map.get("未分类").size() : 0;

        LinearLayout head = whiteCard(dp(22), 0xF2FFFFFF);
        head.setOrientation(LinearLayout.VERTICAL);
        head.setPadding(dp(16), dp(14), dp(16), dp(14));

        LinearLayout top = new LinearLayout(ctx);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout txt = new LinearLayout(ctx);
        txt.setOrientation(LinearLayout.VERTICAL);
        txt.addView(text("表情库", 17, C_MAIN, true, dp(3)), matchWrap(0));
        String sub = list.size() + " 张 · " + keys.size() + " 类"
                + (unclassified > 0 ? " · " + unclassified + " 张待分类" : "");
        txt.addView(text(sub, 10.5f, C_SUB, false, 0), matchWrap(0));
        top.addView(txt, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView newGroup = pill("＋ 分类", 0xFFE4EEFF);
        newGroup.setTextColor(0xFF5A78B0);
        press(newGroup);
        newGroup.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { newGroupDialog(); }
        });
        LinearLayout.LayoutParams ngp = wrapWrap();
        ngp.rightMargin = dp(6);
        top.addView(newGroup, ngp);

        TextView add = pill("＋ 添加", 0xFFFFD9E6);
        add.setTextColor(C_PINK);
        press(add);
        add.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { pickAny(); }
        });
        top.addView(add, wrapWrap());
        head.addView(top, matchWrap(0));

        if (unclassified > 0 || AiEmoji.classifying) {
            btnClassify = new TextView(ctx);
            btnClassify.setGravity(Gravity.CENTER);
            btnClassify.setTextSize(13.5f);
            btnClassify.setTextColor(C_PINK);
            GradientDrawable cg = new GradientDrawable();
            cg.setCornerRadius(dp(14));
            cg.setColor(AiEmoji.classifying ? 0x59FFD9E6 : 0xFFFFE3EE);
            cg.setStroke(Math.max(1, dp(1)), 0x55FF9FCB);
            btnClassify.setBackgroundDrawable(cg);
            btnClassify.setText(classifyText(unclassified));
            press(btnClassify);
            if (!AiEmoji.classifying) {
                btnClassify.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) { startClassify(); }
                });
            }
            LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(46));
            clp.topMargin = dp(12);
            head.addView(btnClassify, clp);
        }
        content.addView(head, matchWrap(dp(14)));

        if (list.isEmpty()) {
            LinearLayout empty = whiteCard(dp(20), 0xF2FFFFFF);
            empty.setOrientation(LinearLayout.VERTICAL);
            empty.setPadding(dp(18), dp(22), dp(18), dp(22));
            empty.addView(text("还没有表情包", 14, C_MAIN, true, dp(6)), matchWrap(0));
            empty.addView(text("点右上角「＋ 添加」：选图片直接进库，选压缩包自动解包；"
                    + "加完点「自动识图分类」", 11, C_SUB, false, 0), matchWrap(0));
            content.addView(empty, matchWrap(dp(14)));
            return;
        }

        for (int i = 0; i < keys.size(); i++) {
            String k = keys.get(i);
            content.addView(groupCard(k, map.get(k)), matchWrap(dp(10)));
        }

        TextView note = text("点一组进去看这类的表情；组内点一条换分类，长按删掉",
                10.5f, C_SUB, false, 0);
        note.setPadding(dp(6), dp(8), dp(6), dp(10));
        content.addView(note, matchWrap(0));

        tickProgress();
    }

    private View groupCard(final String label, List<AiEmoji.Item> items) {
        LinearLayout card = whiteCard(dp(18), 0xE6FFFFFF);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(14), dp(12), dp(12), dp(12));

        for (int i = 0; i < items.size() && i < 3; i++) {
            ImageView iv = new ImageView(ctx);
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            Bitmap bm = thumb(AiEmoji.fileOf(items.get(i)), dp(44));
            if (bm != null) iv.setImageBitmap(bm);
            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(dp(9));
            bg.setColor(0x22A9BEDC);
            iv.setBackgroundDrawable(bg);
            iv.setClipToOutline(true);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(44), dp(44));
            if (i > 0) lp.leftMargin = dp(5);
            card.addView(iv, lp);
        }

        LinearLayout txt = new LinearLayout(ctx);
        txt.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        tp.leftMargin = dp(12);
        txt.addView(text(label, 14.5f, C_MAIN, true, dp(3)), matchWrap(0));
        txt.addView(text(items.size() + " 张 · 点进去看", 10.5f, C_SUB, false, 0), matchWrap(0));
        card.addView(txt, tp);

        card.addView(text("›", 20, 0xFFB6C1D0, false, 0));

        press(card);
        card.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                currentLabel = label;
                render();
            }
        });
        return card;
    }

    

    private void renderGroup(List<AiEmoji.Item> all) {
        final String label = currentLabel;
        List<AiEmoji.Item> items = new ArrayList<AiEmoji.Item>();
        for (int i = 0; i < all.size(); i++) {
            String k = all.get(i).label == null || all.get(i).label.length() == 0
                    ? "未分类" : all.get(i).label;
            if (k.equals(label)) items.add(all.get(i));
        }

        LinearLayout head = whiteCard(dp(22), 0xF2FFFFFF);
        head.setOrientation(LinearLayout.VERTICAL);
        head.setPadding(dp(16), dp(14), dp(16), dp(14));

        TextView back = pill("‹ 返回", 0xFFE4ECF7);
        back.setTextColor(0xFF5A6B80);
        press(back);
        back.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                currentLabel = null;
                render();
            }
        });
        head.addView(back, wrapWrap());

        if (!"未分类".equals(label)) {
            LinearLayout ops = new LinearLayout(ctx);
            ops.setOrientation(LinearLayout.HORIZONTAL);
            TextView rn = pill("改分类名", 0xFFE4EEFF);
            rn.setTextColor(0xFF5A78B0);
            press(rn);
            rn.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { renameGroupDialog(label); }
            });
            TextView del = pill("删分类", 0xFFFFE3EE);
            del.setTextColor(C_PINK);
            press(del);
            del.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { deleteGroupDialog(label); }
            });
            LinearLayout.LayoutParams rp = wrapWrap();
            rp.leftMargin = dp(8);
            ops.addView(rn, wrapWrap());
            ops.addView(del, rp);
            head.addView(ops, matchWrap(dp(8)));
        }

        TextView t = text(label, 20, C_MAIN, true, dp(3));
        t.setPadding(0, dp(12), 0, 0);
        head.addView(t, matchWrap(0));
        head.addView(text(items.size() + " 张", 10.5f, C_SUB, false, 0), matchWrap(0));
        content.addView(head, matchWrap(dp(14)));

        if (items.isEmpty()) {
            LinearLayout empty = whiteCard(dp(20), 0xF2FFFFFF);
            empty.setPadding(dp(18), dp(20), dp(18), dp(20));
            empty.addView(text("这一类没有表情了", 13, C_MAIN, false, 0), matchWrap(0));
            content.addView(empty, matchWrap(dp(14)));
            return;
        }

        for (int i = 0; i < items.size(); i++) {
            content.addView(itemCard(items.get(i)), matchWrap(dp(8)));
        }

        TextView note = text("点一条换分类（会自动跑到那个组），长按删掉",
                10.5f, C_SUB, false, 0);
        note.setPadding(dp(6), dp(8), dp(6), dp(10));
        content.addView(note, matchWrap(0));
    }

    private View itemCard(final AiEmoji.Item it) {
        LinearLayout card = whiteCard(dp(16), 0xE6FFFFFF);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(12), dp(10), dp(12), dp(10));

        ImageView iv = new ImageView(ctx);
        iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
        Bitmap bm = thumb(AiEmoji.fileOf(it), dp(52));
        if (bm != null) iv.setImageBitmap(bm);
        GradientDrawable bgImg = new GradientDrawable();
        bgImg.setCornerRadius(dp(10));
        bgImg.setColor(0x22A9BEDC);
        iv.setBackgroundDrawable(bgImg);
        iv.setClipToOutline(true);
        card.addView(iv, new LinearLayout.LayoutParams(dp(52), dp(52)));

        LinearLayout txt = new LinearLayout(ctx);
        txt.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        tp.leftMargin = dp(12);
        txt.addView(text(it.label, 14, C_MAIN, true, dp(3)), matchWrap(0));
        txt.addView(text("用过 " + it.use + " 次", 10, C_SUB, false, 0), matchWrap(0));
        card.addView(txt, tp);

        card.addView(text("›", 20, 0xFFB6C1D0, false, 0));

        press(card);
        card.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { pickCategoryFor(it); }
        });
        card.setOnLongClickListener(new View.OnLongClickListener() {
            @Override public boolean onLongClick(View v) {
                CatSheet.pick(ctx, "删掉这张表情？", new String[]{"删掉", "算了"},
                        new CatSheet.OnPick() {
                            @Override public void run(int i, String s) {
                                if (i != 0) return;
                                AiEmoji.remove(it.file);
                                toast("已删除");
                                render();
                            }
                        });
                return true;
            }
        });
        return card;
    }

    

    
    private void newGroupDialog() {
        CatSheet.input(ctx, "新建分类", "比如：无语 / 撒娇 / 生气", "", new CatSheet.OnText() {
            @Override public void run(String s) {
                if (s == null || s.trim().length() == 0) return;
                if (!AiEmoji.addGroup(s)) {
                    toast("这个分类已经有了");
                    return;
                }
                toast("已建分类「" + s.trim() + "」");
                render();
            }
        });
    }

    private void renameGroupDialog(final String from) {
        CatSheet.input(ctx, "改分类名", "把「" + from + "」改成…", from, new CatSheet.OnText() {
            @Override public void run(String s) {
                if (s == null || s.trim().length() == 0) return;
                String to = s.trim();
                if (to.equals(from)) return;
                AiEmoji.renameGroup(from, to);
                currentLabel = to;
                toast("已改成「" + to + "」");
                render();
            }
        });
    }

    private void deleteGroupDialog(final String label) {
        CatSheet.pick(ctx, "删掉分类「" + label + "」？里面的表情会挪到未分类",
                new String[]{"删掉分类", "算了"}, new CatSheet.OnPick() {
                    @Override public void run(int i, String s) {
                        if (i != 0) return;
                        AiEmoji.removeGroup(label);
                        currentLabel = null;
                        toast("已删除分类，表情还在");
                        render();
                    }
                });
    }

    private void pickCategoryFor(final AiEmoji.Item it) {
        final java.util.List<String> gs = AiEmoji.groups();
        final java.util.List<String> opts = new ArrayList<String>();
        opts.add("未分类");
        String cur = it.label == null ? "" : it.label.trim();
        if (cur.length() > 0 && !gs.contains(cur)) opts.add(cur);
        for (int i = 0; i < gs.size(); i++) {
            if (!opts.contains(gs.get(i))) opts.add(gs.get(i));
        }
        opts.add("＋ 新建分类…");

        final String[] arr = opts.toArray(new String[0]);
        CatSheet.pick(ctx, "这张放进哪个分类？", arr, new CatSheet.OnPick() {
            @Override public void run(int i, String s) {
                if (i < 0 || i >= arr.length) return;
                String pick = arr[i];
                if ("＋ 新建分类…".equals(pick)) {
                    CatSheet.input(ctx, "新建分类并移入", "比如：无语 / 撒娇", "",
                            new CatSheet.OnText() {
                                @Override public void run(String n) {
                                    if (n == null || n.trim().length() == 0) return;
                                    String name = n.trim();
                                    AiEmoji.addGroup(name);
                                    AiEmoji.moveTo(it.file, name);
                                    toast("已移到「" + name + "」");
                                    render();
                                }
                            });
                    return;
                }
                AiEmoji.moveTo(it.file, "未分类".equals(pick) ? "" : pick);
                toast("已移到「" + pick + "」");
                render();
            }
        });
    }

    

    private String classifyText(int unclassified) {
        if (AiEmoji.classifying) {
            return AiEmoji.clsTotal > 0
                    ? ("正在分类… " + AiEmoji.clsDone + " / " + AiEmoji.clsTotal)
                    : "正在分类…请稍等";
        }
        if (AiEmoji.clsError != null && AiEmoji.clsError.length() > 0
                && AiEmoji.clsOk == 0 && unclassified > 0) {
            return "重新分类（上次失败：" + AiEmoji.clsError + "）";
        }
        return "自动识图分类（" + unclassified + " 张待分类）";
    }

    private void startClassify() {
        AiEmoji.startClassify();
        render();
    }

    private void tickProgress() {
        if (btnClassify == null || !AiEmoji.classifying) return;
        btnClassify.postDelayed(new Runnable() {
            @Override public void run() {
                if (btnClassify == null) return;
                if (AiEmoji.classifying) {
                    btnClassify.setText("正在分类… " + AiEmoji.clsDone + " / " + AiEmoji.clsTotal);
                    btnClassify.postDelayed(this, 600);
                } else {
                    render();          
                }
            }
        }, 600);
    }

    

    private void pickAny() {
        try {
            Activity a = null;
            if (ctx instanceof Activity) a = (Activity) ctx;
            if (a == null) a = com.cathelperawa.owo.app.bridge.QQEnv.topActivity();
            if (a == null) { toast("叫不动文件选择器…"); return; }
            pending = new Picker() {
                @Override public void onPicked(List<Uri> uris) {
                    importAll(uris);
                }
            };
            Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType("*/*");
            i.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{
                    "image/*",
                    "application/zip", "application/x-zip-compressed",
                    "application/octet-stream", "application/x-compressed"
            });
            i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            a.startActivityForResult(i, AiActivity.REQ_EMOJI);
        } catch (Throwable t) {
            toast("叫不动文件选择器…" + t);
        }
    }

    private boolean isZip(Uri u) {
        try {
            String p = u.toString().toLowerCase();
            if (p.endsWith(".zip")) return true;
            String type = ctx.getContentResolver().getType(u);
            return type != null && type.toLowerCase().contains("zip");
        } catch (Throwable t) {
            return false;
        }
    }

    private void importAll(final List<Uri> uris) {
        toast("正在导入 " + uris.size() + " 个文件…");
        Bg.run(new Runnable() {
            @Override public void run() {
                int ok = 0;
                int zipCount = 0;
                for (int i = 0; i < uris.size(); i++) {
                    Uri u = uris.get(i);
                    if (isZip(u)) {
                        int k = AiEmoji.importZip(ctx, u);
                        if (k > 0) { ok += k; zipCount++; }
                    } else if (AiEmoji.importUri(ctx, u) != null) {
                        ok++;
                    }
                }
                final int n = ok;
                final int zc = zipCount;
                ui(new Runnable() {
                    @Override public void run() {
                        toast(n > 0
                                ? ("已导入 " + n + " 张" + (zc > 0 ? "（含 " + zc + " 个压缩包）" : "")
                                + "，可以点「自动识图分类」")
                                : "一张都没导进来…");
                        render();
                    }
                });
            }
        }, "cat-emoji-import");
    }

    private void ui(Runnable r) {
        try {
            if (ctx instanceof Activity) ((Activity) ctx).runOnUiThread(r);
            else r.run();
        } catch (Throwable ignore) {
        }
    }

    

    private static Bitmap thumb(File f, int px) {
        try {
            if (f == null || !f.isFile()) return null;
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(f.getAbsolutePath(), o);
            int scale = 1;
            while (o.outWidth / scale > px * 2 || o.outHeight / scale > px * 2) scale *= 2;
            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize = scale;
            return BitmapFactory.decodeFile(f.getAbsolutePath(), o2);
        } catch (Throwable t) {
            return null;
        }
    }
}
