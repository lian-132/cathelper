package com.cathelperawa.owo.app.ui;

import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.cathelperawa.owo.app.MsgStore;

import java.util.List;

public final class MsgPage extends Page {

    public MsgPage(Host host) { super(host); }

    private static boolean showLog = false;

    private static final java.util.concurrent.atomic.AtomicInteger BUILD_SEQ =
            new java.util.concurrent.atomic.AtomicInteger(0);

    @Override
    public View build() {
        View[] out = new View[4];

        final int seq = BUILD_SEQ.incrementAndGet();

        List<String> items = null;
        boolean loading = false;
        if (showLog) {
            items = MsgStore.logsIfReady(200);
            if (items == null) {
                items = new java.util.ArrayList<String>();
                loading = true;
            }
        } else {
            items = MsgStore.all();
        }

        if (loading) {
            MsgStore.preloadLogs(200, new Runnable() {
                @Override public void run() {
                    android.os.Handler ui = new android.os.Handler(
                            android.os.Looper.getMainLooper());
                    ui.post(new Runnable() {
                        @Override public void run() {

                            if (BUILD_SEQ.get() != seq) return;
                            try {
                                host.setPageView(build());
                            } catch (Throwable ignore) {
                            }
                        }
                    });
                }
            });
        }

        String title = showLog ? "模块日志" : "消息记录";
        String sub = showLog
                ? (loading ? "正在读取…" : ("最近 " + items.size() + " 行 · files/owo/<号>/log.txt"))
                : ("已抓 " + items.size() + " 条 · 模块进程内");

        LinearLayout root = pageRootEx(title, sub, out);
        LinearLayout topbar = (LinearLayout) out[2];

        final TextView tabMsg = pill("消息", showLog ? 0xFFB9C6DA : 0xFF7FA5E6);
        final TextView tabLog = pill("日志", showLog ? 0xFF7FA5E6 : 0xFFB9C6DA);
        topbar.addView(tabMsg);
        topbar.addView(tabLog);
        View.OnClickListener sw = new View.OnClickListener() {
            @Override public void onClick(View v) {
                boolean want = (v == tabLog);
                if (want == showLog) return;
                showLog = want;
                press(v);
                host.setPageView(build());
            }
        };
        tabMsg.setOnClickListener(sw);
        tabLog.setOnClickListener(sw);

        TextView clear = pill("清空", 0xFF7FA5E6);
        topbar.addView(clear);
        clear.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (showLog) MsgStore.clearLogs();
                else MsgStore.clear();
                press(v);
                host.setPageView(build());
            }
        });

        if (items.isEmpty()) {
            LinearLayout empty = whiteCard(dp(20), 0x8CFFFFFF);
            empty.setPadding(dp(16), dp(20), dp(16), dp(20));
            if (showLog) {
                empty.addView(text("还没有模块日志", 15, 0xFF43587A, true, dp(3)));
                empty.addView(text("模块运行后会写入这里", 11, 0xFF8496AE, false, 0));
            } else {
                empty.addView(text("还没有抓到消息", 15, 0xFF43587A, true, dp(3)));
                empty.addView(text("在 " + com.cathelperawa.owo.app.core.Host.name() + " 里收发几条消息就会出现在这里", 11, 0xFF8496AE, false, 0));
            }
            root.addView(empty);
        } else {

            for (int i = items.size() - 1; i >= 0; i--) {
                LinearLayout card = whiteCard(dp(16), 0x8CFFFFFF);
                card.setPadding(dp(14), dp(11), dp(14), dp(11));
                TextView t = text(items.get(i), 12, 0xFF4F6485, false, 0);
                t.setTextIsSelectable(true);
                card.addView(t);
                root.addView(card, matchWrap(dp(8)));
            }
        }

        root.setAlpha(0f);
        root.animate().alpha(1f).setDuration(360).start();

        return out[0];
    }
}
