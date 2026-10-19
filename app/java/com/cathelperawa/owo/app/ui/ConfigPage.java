package com.cathelperawa.owo.app.ui;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.cathelperawa.owo.app.core.ConfigBank;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public final class ConfigPage extends Page {

    public static final int REQ_PICK = 0x0C1F;

    private static final int C_TITLE = 0xFF43587A;
    private static final int C_SUB = 0xFF6E8098;
    private static final int C_HINT = 0xFF8E9DAF;
    private static final int C_BLUE = 0xFF5B8DEF;
    private static final int C_GREEN = 0xFF4CAF7D;

    private LinearLayout contentBox;
    private TextView subtitle;
    private File lastExport;

    public ConfigPage(Host host) { super(host); }

    @Override
    public View build() {
        final View[] out = new View[4];
        pageRootEx("配置备份", "导出导入备份", out);

        LinearLayout topbar = (LinearLayout) out[2];
        TextView refresh = text("刷新", 12, C_BLUE, true, 0);
        refresh.setPadding(dp(10), dp(6), dp(10), dp(6));
        refresh.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { press(v); render(); }
        });
        topbar.addView(refresh, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        contentBox = (LinearLayout) out[1];
        subtitle = (TextView) out[3];
        render();
        return out[0];
    }

    private void render() {
        if (contentBox == null) return;
        while (contentBox.getChildCount() > 1) {
            contentBox.removeViewAt(contentBox.getChildCount() - 1);
        }
        if (subtitle != null) subtitle.setText("导出导入备份");

        contentBox.addView(exportCard(), matchWrap(dp(12)));
        contentBox.addView(importCard(), matchWrap(dp(12)));
        contentBox.addView(historyCard(), matchWrap(0));
    }

    private View exportCard() {
        LinearLayout card = card();
        card.addView(title("导出配置", "导出配置文件"), matchWrap(dp(12)));

        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.addView(btn("导出配置", C_BLUE, true, new View.OnClickListener() {
            @Override public void onClick(View v) { press(v); doExport(); }
        }), new LinearLayout.LayoutParams(0, dp(44), 1f));
        row.addView(new View(ctx), new LinearLayout.LayoutParams(dp(9), 1));
        row.addView(btn("分享", C_BLUE, false, new View.OnClickListener() {
            @Override public void onClick(View v) { press(v); shareLast(); }
        }), new LinearLayout.LayoutParams(0, dp(44), 1f));
        card.addView(row, matchWrap(dp(14)));

        File dir = ConfigBank.exchangeDir(ctx);
        String path = dir == null ? "（无法定位）" : dir.getAbsolutePath();

        LinearLayout box = new LinearLayout(ctx);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(12), dp(10), dp(12), dp(10));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0x4DEEF3FB);
        bg.setCornerRadius(dp(10));
        box.setBackgroundDrawable(bg);
        box.addView(text("保存位置", 10.5f, C_HINT, false, dp(3)));
        box.addView(text(path, 10.5f, C_BLUE, false, dp(8)));
        box.addView(btn("复制完整路径", C_BLUE, false, new View.OnClickListener() {
            @Override public void onClick(View v) { press(v); copyPath(); }
        }), new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(36)));
        card.addView(box, matchWrap(0));
        return card;
    }

    private View importCard() {
        LinearLayout card = card();
        card.addView(title("导入配置", "选一种导入方式"), matchWrap(dp(12)));

        card.addView(stepLabel("①", "粘贴导入", "粘贴文本导入"), matchWrap(dp(8)));
        card.addView(btn("粘贴配置内容", C_GREEN, true, new View.OnClickListener() {
            @Override public void onClick(View v) { press(v); doPasteImport(); }
        }), new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(44)));
        card.addView(gap(dp(14)));

        card.addView(stepLabel("②", "选择文件", "从文件管理器挑 .txt"), matchWrap(dp(8)));
        card.addView(btn("打开文件选择器", C_GREEN, false, new View.OnClickListener() {
            @Override public void onClick(View v) { press(v); pickFile(); }
        }), new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(44)));
        card.addView(gap(dp(14)));

        card.addView(stepLabel("③", "点下面的备份", "本机导出过的直接导入"), matchWrap(0));
        return card;
    }

    private void doPasteImport() {
        try {
            CatDialog.input(ctx, "粘贴配置内容",
                    "把 cathelper-config-xxx.txt 的内容整段粘进来",
                    "", true, new CatDialog.OnText() {
                        @Override public void run(String s) {
                            if (s == null || s.trim().isEmpty()) { toast("什么都没粘呢"); return; }
                            doImportText(s);
                        }
                    });
        } catch (Throwable t) {
            toast("输入框罢工了…" + t);
        }
    }

    private void doImportText(final String text) {
        if (subtitle != null) subtitle.setText("正在导入，稍等…");
        new Thread(new Runnable() {
            @Override public void run() {
                final ConfigBank.Result r = ConfigBank.importText(ctx, text);
                ui(new Runnable() {
                    @Override public void run() {
                        if (subtitle != null) {
                            subtitle.setText(r.ok() ? "已导入 " + r.count + " 项～" : "导入失败了…");
                        }
                        toast(r.message);
                    }
                });
            }
        }, "cfg-import-text").start();
    }

    private View historyCard() {
        LinearLayout card = card();
        card.addView(title("本机备份", null), matchWrap(dp(10)));

        List<File> files = listBackups();
        if (files.isEmpty()) {
            card.addView(text("还没有导出过，点上面「导出配置」生成第一份。",
                    12, C_HINT, false, 0));
            return card;
        }
        int shown = 0;
        for (final File f : files) {
            if (shown++ >= 5) break;
            LinearLayout row = new LinearLayout(ctx);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, dp(2), 0, dp(2));

            LinearLayout col = new LinearLayout(ctx);
            col.setOrientation(LinearLayout.VERTICAL);
            col.addView(text(prettyName(f.getName()), 13, C_TITLE, true, 0));
            col.addView(text(humanSize(f.length()), 10.5f, C_HINT, false, 0));
            row.addView(col, new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            row.addView(btn("导入", C_GREEN, false, new View.OnClickListener() {
                @Override public void onClick(View v) { press(v); doImportFile(f); }
            }), new LinearLayout.LayoutParams(dp(66), dp(34)));
            card.addView(row, matchWrap(dp(4)));
        }
        return card;
    }

    private List<File> listBackups() {
        List<File> files = new ArrayList<File>();
        File dir = ConfigBank.exchangeDir(ctx);
        if (dir == null || !dir.isDirectory()) return files;
        File[] all = dir.listFiles();
        if (all == null) return files;
        for (File f : all) {
            if (f.isFile() && f.getName().startsWith("cathelper-config-")
                    && f.getName().endsWith(".txt")) files.add(f);
        }
        java.util.Collections.sort(files, new java.util.Comparator<File>() {
            @Override public int compare(File a, File b) {
                return Long.compare(b.lastModified(), a.lastModified());
            }
        });
        return files;
    }

    private static String humanSize(long bytes) {
        if (bytes < 0) return "?";
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return (bytes / 1024) + " KB";
        return String.format(java.util.Locale.ROOT, "%.1f MB", bytes / 1048576.0);
    }

    private static String prettyName(String name) {
        try {
            String s = name.replace("cathelper-config-", "").replace(".txt", "");
            String[] p = s.split("-");
            if (p.length >= 2 && p[0].length() == 8) {
                return p[0].substring(0, 4) + "-" + p[0].substring(4, 6) + "-"
                        + p[0].substring(6, 8) + " "
                        + p[1].substring(0, 2) + ":" + p[1].substring(2, 4);
            }
        } catch (Throwable ignore) {
        }
        return name;
    }

    private void doExport() {
        if (subtitle != null) subtitle.setText("正在导出，稍等…");
        new Thread(new Runnable() {
            @Override public void run() {
                final ConfigBank.Result r = ConfigBank.export(ctx);
                ui(new Runnable() {
                    @Override public void run() {
                        if (subtitle != null) {
                            subtitle.setText(r.ok() ? "已导出 " + r.count + " 项～"
                                    : "导出失败了…");
                        }
                        toast(r.message);
                        if (r.ok()) { lastExport = r.file; render(); }
                    }
                });
            }
        }, "cfg-export").start();
    }

    private File newestBackup() {
        File f = lastExport;
        List<File> list = listBackups();
        if (!list.isEmpty() && (f == null || !f.isFile()
                || list.get(0).lastModified() > f.lastModified())) {
            f = list.get(0);
        }
        return f;
    }

    private void copyPath() {
        File f = newestBackup();
        File dir = ConfigBank.exchangeDir(ctx);
        String s = f != null ? f.getAbsolutePath()
                : (dir != null ? dir.getAbsolutePath() : "");
        if (s.isEmpty()) { toast("路径还没生出来呢"); return; }
        try {
            ClipboardManager cm = (ClipboardManager)
                    ctx.getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null) {
                cm.setPrimaryClip(ClipData.newPlainText("配置路径", s));
                toast("路径塞进剪贴板啦，去粘贴吧～");
            }
        } catch (Throwable t) {
            toast("复制失败了…" + t);
        }
    }

    private void shareLast() {
        File f = newestBackup();
        if (f == null || !f.isFile()) {
            toast("还没导出过呢，先点上面的「导出配置」呀");
            return;
        }
        try {
            android.os.StrictMode.VmPolicy old = android.os.StrictMode.getVmPolicy();
            android.os.StrictMode.setVmPolicy(
                    new android.os.StrictMode.VmPolicy.Builder().build());
            Intent send = new Intent(Intent.ACTION_SEND);
            send.setType("text/plain");
            send.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(f));
            send.putExtra(Intent.EXTRA_SUBJECT, "猫娘助手配置备份");
            Intent chooser = Intent.createChooser(send, "保存到");
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(chooser);
            android.os.StrictMode.setVmPolicy(old);
        } catch (Throwable t) {
            toast("分享不出去…" + t);
        }
    }

    private void pickFile() {
        try {
            Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType("*/*");
            Activity a = activity();
            if (a == null) { toast("文件选择器叫不动…"); return; }
            a.startActivityForResult(i, REQ_PICK);
        } catch (Throwable t) {
            toast("文件选择器叫不动…" + t);
        }
    }

    private void doImportFile(final File f) {
        if (subtitle != null) subtitle.setText("正在导入，稍等…");
        new Thread(new Runnable() {
            @Override public void run() {
                final ConfigBank.Result r = ConfigBank.importFrom(ctx, f);
                ui(new Runnable() {
                    @Override public void run() {
                        if (subtitle != null) {
                            subtitle.setText(r.ok() ? "已导入 " + r.count + " 项～" : "导入失败了…");
                        }
                        toast(r.message);
                    }
                });
            }
        }, "cfg-import").start();
    }

    public void onPicked(Uri uri) {
        if (uri == null) return;
        try {
            File local = copyToExchange(uri);
            if (local == null) { toast("这文件读不动诶"); return; }
            doImportFile(local);
        } catch (Throwable t) {
            toast("这文件读不动诶…" + t);
        }
    }

    private File copyToExchange(Uri uri) throws Exception {
        File dir = ConfigBank.exchangeDir(ctx);
        if (dir == null) return null;
        if (!dir.exists() && !dir.mkdirs()) return null;
        File out = new File(dir, "config-import.txt");
        java.io.InputStream in = ctx.getContentResolver().openInputStream(uri);
        if (in == null) return null;
        java.io.FileOutputStream fo = new java.io.FileOutputStream(out);
        try {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) fo.write(buf, 0, n);
            fo.flush();
        } finally {
            try { in.close(); } catch (Throwable ignore) { }
            try { fo.close(); } catch (Throwable ignore) { }
        }
        return out;
    }

    private LinearLayout card() {
        LinearLayout l = new LinearLayout(ctx);
        l.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable g = new GradientDrawable();
        g.setColor(0x99FFFFFF);
        g.setCornerRadius(dp(20));
        g.setStroke(dp(1), 0x88FFFFFF);
        l.setBackgroundDrawable(g);
        l.setElevation(dp(3));
        l.setPadding(dp(16), dp(15), dp(16), dp(15));
        return l;
    }

    private LinearLayout title(String t, String sub) {
        LinearLayout l = new LinearLayout(ctx);
        l.setOrientation(LinearLayout.VERTICAL);
        l.addView(text(t, 16, C_TITLE, true, sub == null ? 0 : dp(3)));
        if (sub != null) l.addView(text(sub, 11.5f, C_SUB, false, 0));
        return l;
    }

    private LinearLayout stepLabel(String num, String name, String hint) {
        LinearLayout l = new LinearLayout(ctx);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);

        TextView badge = new TextView(ctx);
        badge.setText(num);
        badge.setTextSize(11);
        badge.setTypeface(Typeface.DEFAULT_BOLD, Typeface.BOLD);
        badge.setTextColor(0xFFFFFFFF);
        badge.setGravity(Gravity.CENTER);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xCC9E93DD);
        bg.setCornerRadius(dp(7));
        badge.setBackgroundDrawable(bg);
        l.addView(badge, new LinearLayout.LayoutParams(dp(20), dp(20)));

        TextView n = text(name, 12.5f, C_TITLE, true, 0);
        n.setPadding(dp(8), 0, dp(8), 0);
        l.addView(n);

        TextView h = text(hint, 10.5f, C_HINT, false, 0);
        l.addView(h, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        return l;
    }

    private TextView btn(String label, int color, boolean solid, View.OnClickListener l) {
        TextView t = new TextView(ctx);
        t.setText(label);
        t.setTextSize(13.5f);
        t.setTypeface(Typeface.DEFAULT_BOLD, Typeface.BOLD);
        t.setGravity(Gravity.CENTER);
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(dp(12));

        int base = color & 0x00FFFFFF;
        if (solid) {
            g.setColor(base | 0x2E000000);
            g.setStroke(dp(1), base | 0x8C000000);
        } else {
            g.setColor(base | 0x14000000);
            g.setStroke(dp(1), base | 0x73000000);
        }
        t.setTextColor(color);
        t.setBackgroundDrawable(g);
        t.setOnClickListener(l);
        return t;
    }

    private View gap(int dp) {
        View v = new View(ctx);
        v.setLayoutParams(new LinearLayout.LayoutParams(1, dp));
        return v;
    }

    private void ui(Runnable r) {
        new Handler(Looper.getMainLooper()).post(r);
    }
}
