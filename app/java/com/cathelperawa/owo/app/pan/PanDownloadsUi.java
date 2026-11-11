package com.cathelperawa.owo.app.pan;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

public final class PanDownloadsUi {

    private PanDownloadsUi() {}

    private static final int C_MAIN = 0xFFEAF1FB;
    private static final int C_SUB = 0xFF9AA9BC;
    private static final int C_DIM = 0xFF8B9AAD;
    private static final int C_OK = 0xFF37C28D;
    private static final int C_ERR = 0xFFFF8A80;
    private static final int C_ACCENT = 0xFF6EA6FF;

    public static void build(final Activity a, LinearLayout box) {
        if (a == null || box == null) return;
        PanDl.init(a);
        PanDl.sampleSpeeds();
        box.removeAllViews();

        List<PanTask> list = PanDl.tasks();
        if (list.isEmpty()) {
            box.addView(hint(a, "还没有任务。去「网盘目录」点文件、或者解析别人的分享，就会出现在这里。", C_DIM));
            return;
        }
        for (PanTask t : list) box.addView(row(a, t));
    }

    private static View row(final Activity a, final PanTask t) {
        LinearLayout card = new LinearLayout(a);
        card.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(dp(a, 12));
        g.setColor(0xE8161D28);
        g.setStroke(dp(a, 1), 0x2AFFFFFF);
        card.setBackground(g);
        int p = dp(a, 11);
        card.setPadding(p, p, p, p);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(a, 7);
        card.setLayoutParams(lp);

        LinearLayout head = new LinearLayout(a);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        TextView name = new TextView(a);
        name.setText(t.name);
        name.setTextSize(13f);
        name.setTextColor(C_MAIN);
        name.setTypeface(null, Typeface.BOLD);
        name.setMaxLines(2);
        name.setEllipsize(TextUtils.TruncateAt.MIDDLE);
        head.addView(name, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView action = new TextView(a);
        action.setTextSize(12f);
        action.setPadding(dp(a, 10), dp(a, 4), dp(a, 2), dp(a, 4));

        if (t.state == PanTask.RUN || t.state == PanTask.WAIT) {
            action.setText("取消");
            action.setTextColor(C_ERR);
            action.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    PanDl.cancel(t);
                    Toast.makeText(a, "已取消：" + t.name, Toast.LENGTH_SHORT).show();
                }
            });
        } else if (t.state == PanTask.FAIL) {
            action.setText("续传");
            action.setTextColor(C_ACCENT);
            action.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    PanDl.resume(a, t);
                    Toast.makeText(a, "继续下载：" + t.name, Toast.LENGTH_SHORT).show();
                }
            });
        } else if (t.state == PanTask.DONE) {
            action.setText("打开");
            action.setTextColor(C_OK);
            action.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { open(a, t); }
            });
        } else {
            action.setText("续传");
            action.setTextColor(C_ACCENT);
            action.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { PanDl.resume(a, t); }
            });
        }
        head.addView(action);

        if (t.state != PanTask.RUN && t.state != PanTask.WAIT) {
            TextView del = new TextView(a);
            del.setText("删除");
            del.setTextSize(12f);
            del.setTextColor(C_SUB);
            del.setPadding(dp(a, 8), dp(a, 4), dp(a, 2), dp(a, 4));
            del.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { askDelete(a, t); }
            });
            head.addView(del);
        }
        card.addView(head);

        TextView sub = new TextView(a);
        sub.setTextSize(11f);
        sub.setTextColor(t.state == PanTask.FAIL ? C_ERR : C_DIM);
        sub.setText(statusText(t));
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        slp.topMargin = dp(a, 3);
        card.addView(sub, slp);

        if (t.state == PanTask.RUN || t.state == PanTask.WAIT) {
            ProgressBar pb = new ProgressBar(a, null, android.R.attr.progressBarStyleHorizontal);
            pb.setMax(1000);
            pb.setProgress(t.percent() * 10);
            GradientDrawable bar = new GradientDrawable();
            bar.setCornerRadius(dp(a, 4));
            bar.setColor(0xFF2F6BE0);
            pb.setProgressDrawable(bar);
            LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(a, 4));
            plp.topMargin = dp(a, 6);
            card.addView(pb, plp);
        }

        card.setOnLongClickListener(new View.OnLongClickListener() {
            @Override public boolean onLongClick(View v) {
                askDelete(a, t);
                return true;
            }
        });
        return card;
    }

    private static String statusText(PanTask t) {
        String size = PanItem.human(t.total);
        switch (t.state) {
            case PanTask.RUN:
            case PanTask.WAIT: {
                StringBuilder sb = new StringBuilder();
                sb.append(t.percent()).append("%　").append(PanItem.human(t.done)).append(" / ").append(size);
                String sp = t.speedText();
                if (sp.length() > 0) sb.append("　").append(sp);
                return sb.toString();
            }
            case PanTask.DONE:
                return "已完成 · " + size + " · 点右侧「打开」";
            case PanTask.FAIL:
                return "失败/未完成：" + (t.err == null || t.err.length() == 0 ? "未知原因" : t.err);
            case PanTask.CANCELED:
                return "已取消";
            default:
                return "";
        }
    }

    private static void askDelete(final Activity a, final PanTask t) {
        boolean hasFile = t.state == PanTask.DONE
                && t.publicUri != null && t.publicUri.length() > 0;
        String sub = hasFile
                ? "文件在 Download/" + PanPrefs.subDir(a) + "，可以只删记录、留着文件"
                : "这条记录会从下载列表里移除";
        String[] labels = hasFile
                ? new String[]{"删除记录和文件", "只删记录"}
                : new String[]{"删除这条记录"};
        boolean[] danger = hasFile ? new boolean[]{true, false} : new boolean[]{false};
        PanSheet.Act[] acts = new PanSheet.Act[]{
                new PanSheet.Act() {
                    @Override public void run() {
                        boolean ok = PanDl.remove(a, t, true);
                        Toast.makeText(a, ok ? "已删除记录和文件" : "记录已删，文件没删掉（可能在别处）",
                                Toast.LENGTH_SHORT).show();
                    }
                },
                new PanSheet.Act() {
                    @Override public void run() {
                        PanDl.remove(a, t, false);
                        Toast.makeText(a, "只删了记录，文件还在 Download/" + PanPrefs.subDir(a),
                                Toast.LENGTH_SHORT).show();
                    }
                },
                new PanSheet.Act() {
                    @Override public void run() {
                        PanDl.remove(a, t, false);
                        Toast.makeText(a, "已删除记录", Toast.LENGTH_SHORT).show();
                    }
                }
        };
        if (!hasFile) {
            
            acts = new PanSheet.Act[]{acts[2]};
        }
        PanSheet.menu(a, t.name, sub, labels, danger, acts);
    }

    private static void open(Activity a, PanTask t) {
        if (t.publicUri == null || t.publicUri.length() == 0) {
            Toast.makeText(a, "文件还没搬到公共目录，去 Download/" + PanPrefs.subDir(a) + " 看看",
                    Toast.LENGTH_LONG).show();
            return;
        }
        Uri uri;
        try {
            uri = Uri.parse(t.publicUri);
        } catch (Throwable e) {
            Toast.makeText(a, "文件地址坏了：" + t.publicUri, Toast.LENGTH_LONG).show();
            return;
        }
        String mime = t.mime == null || t.mime.length() == 0 ? "*/*" : t.mime;

        try {
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setDataAndType(uri, mime);
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            a.startActivity(i);
            return;
        } catch (Throwable ignore) {
        }

        try {
            Intent pick = new Intent(Intent.ACTION_VIEW);
            pick.setDataAndType(uri, mime);
            pick.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            Intent chooser = Intent.createChooser(pick, "用哪个应用打开");
            a.startActivity(chooser);
            return;
        } catch (Throwable ignore) {
        }

        Toast.makeText(a, "没有应用能打开这种文件\n文件在 Download/" + PanPrefs.subDir(a)
                + "/" + t.name + "，用文件管理器打开吧", Toast.LENGTH_LONG).show();
    }

    private static TextView hint(Activity a, String text, int color) {
        TextView tv = new TextView(a);
        tv.setText(text);
        tv.setTextSize(11.5f);
        tv.setTextColor(color);
        tv.setLineSpacing(dp(a, 3), 1f);
        return tv;
    }

    private static int dp(Activity a, float v) {
        return Math.max(1, (int) (v * a.getResources().getDisplayMetrics().density + 0.5f));
    }
}
