package com.cathelperawa.owo.app.ai;

import android.app.Activity;
import com.cathelperawa.owo.app.core.Bg;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.cathelperawa.owo.app.core.HookLog;
import com.cathelperawa.owo.app.ui.Page;
import com.cathelperawa.owo.app.ui.SwitchBox;

public class AiActivity extends Activity implements Page.Host {

    private View current;
    
    private final java.util.ArrayDeque<View> stack = new java.util.ArrayDeque<View>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        
        try {
            applyEdgeToEdge();
            getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
            
            getWindow().setNavigationBarColor(android.graphics.Color.TRANSPARENT);
            try {
                if (android.os.Build.VERSION.SDK_INT >= 29) {
                    getWindow().setNavigationBarContrastEnforced(false);
                }
                if (android.os.Build.VERSION.SDK_INT >= 27) {
                    getWindow().setNavigationBarDividerColor(android.graphics.Color.TRANSPARENT);
                }
            } catch (Throwable ignore) {
            }
            getWindow().setBackgroundDrawable(
                    new android.graphics.drawable.ColorDrawable(0xFFE4EFFF));

            

            getWindow().getDecorView().setBackgroundColor(0xFFEDF2FB);
            
            getWindow().getDecorView().setBackground(
                    new android.graphics.drawable.ColorDrawable(0xFFEDF2FB));
        } catch (Throwable t) {
            HookLog.e("AI 控制台窗口设置失败", t);
        }

        
        try {
            final android.view.View decor = getWindow().getDecorView();
            decor.setOnApplyWindowInsetsListener(new android.view.View.OnApplyWindowInsetsListener() {
                @Override public android.view.WindowInsets onApplyWindowInsets(
                        android.view.View v, android.view.WindowInsets insets) {
                    int ime = 0;
                    try {
                        ime = insets.getInsets(android.view.WindowInsets.Type.ime()).bottom;
                    } catch (Throwable ignore) {
                    }
                    v.setPadding(v.getPaddingLeft(), v.getPaddingTop(),
                            v.getPaddingRight(), ime);
                    return insets;
                }
            });
        } catch (Throwable t) {
            HookLog.w("AI 键盘 inset 监听失败 " + t);
        }

        try {
            com.cathelperawa.owo.app.core.ConfigStore.init(this);
        } catch (Throwable ignore) {
        }

        try {
            showHome();

            
            Bg.run(new Runnable() {
                @Override public void run() {
                    try {
                        com.cathelperawa.owo.app.ui.Images.load(AiActivity.this);
                    } catch (Throwable ignore) {
                    }
                    runOnUiThread(new Runnable() {
                        @Override public void run() {
                            try {
                                if (stack.isEmpty()) showHome();
                            } catch (Throwable ignore) {
                            }
                        }
                    });
                }
            }, "cat-ai-img");
        } catch (Throwable t) {
            HookLog.e("AI 控制台启动失败", t);
            finish();
        }
    }

    private void showHome() {
        
        try {
            android.view.View mod = com.cathelperawa.owo.app.ui.ScriptScreen.build(this, "ai_home");
            if (mod != null) {
                current = mod;
                setContentView(current);
                HookLog.i("AI 首页由脚本接管");
                return;
            }
        } catch (Throwable ignore) {
        }
        current = new AiHomePage(this).build();
        setContentView(current);
    }

    private void applyEdgeToEdge() {
        try {
            android.view.Window w = getWindow();

            
            w.setFlags(android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
            if (android.os.Build.VERSION.SDK_INT >= 30) {
                w.setDecorFitsSystemWindows(false);
                android.view.WindowInsetsController c = w.getInsetsController();
                if (c != null) {
                    c.setSystemBarsAppearance(
                            android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                            android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS);
                }
            } else {
                w.setFlags(android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                        android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
                if (android.os.Build.VERSION.SDK_INT >= 23) {
                    android.view.View dv = w.getDecorView();
                    dv.setSystemUiVisibility(dv.getSystemUiVisibility()
                            | android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
                }
            }
        } catch (Throwable ignore) {
        }
    }

    

    @Override
    public Context context() { return this; }

    @Override
    public void close() {
        if (!stack.isEmpty()) {
            View prev = stack.pop();
            if (stack.isEmpty()) {
                
                showHome();
            } else {
                current = prev;
                setContentView(prev);
            }
            restoreScroll();
        } else {
            finish();
        }
    }

    
    public static final int REQ_EMOJI = 0x7101;

    @Override
    protected void onActivityResult(int req, int res, android.content.Intent data) {
        try {
            super.onActivityResult(req, res, data);
        } catch (Throwable ignore) {
        }
        if (req != REQ_EMOJI) return;
        java.util.List<android.net.Uri> uris = new java.util.ArrayList<android.net.Uri>();
        try {
            if (data != null) {
                if (data.getData() != null) uris.add(data.getData());
                android.content.ClipData cd = data.getClipData();
                if (cd != null) {
                    for (int i = 0; i < cd.getItemCount(); i++) {
                        android.net.Uri u = cd.getItemAt(i).getUri();
                        if (u != null) uris.add(u);
                    }
                }
            }
        } catch (Throwable ignore) {
        }
        AiEmojiPage.deliver(uris);
    }

    @Override
    public void onBackPressed() {

        try {
            com.cathelperawa.owo.app.ui.Page p =
                    com.cathelperawa.owo.app.ui.Page.pageOf(current);
            if (p != null && p.onBack()) return;
        } catch (Throwable ignore) {
        }
        if (!stack.isEmpty()) {
            close();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void pushPage(View v) {
        if (v == null) return;
        rememberScroll();
        if (current != null) stack.push(current);
        current = v;
        setContentView(v);
    }

    
    private int pendingScrollY = -1;

    private ScrollView findScrollView(View v) {
        if (v == null) return null;
        if (v instanceof ScrollView) return (ScrollView) v;
        if (v instanceof android.view.ViewGroup) {
            android.view.ViewGroup g = (android.view.ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) {
                ScrollView s = findScrollView(g.getChildAt(i));
                if (s != null) return s;
            }
        }
        return null;
    }

    private void rememberScroll() {
        try {
            ScrollView sv = findScrollView(current);
            pendingScrollY = sv == null ? -1 : sv.getScrollY();
            com.cathelperawa.owo.app.core.HookLog.i("[滚动] AI 记 = " + pendingScrollY);
        } catch (Throwable ignore) {
            pendingScrollY = -1;
        }
    }

    private void restoreScroll() {
        final int y = pendingScrollY;
        pendingScrollY = -1;
        if (y <= 0) return;
        final int[] tries = new int[]{0};
        final Runnable apply = new Runnable() {
            @Override public void run() {
                try {
                    ScrollView sv = findScrollView(findViewById(android.R.id.content));
                    if (sv == null) return;
                    sv.scrollTo(0, y);
                    int now = sv.getScrollY();
                    com.cathelperawa.owo.app.core.HookLog.i("[滚动] AI 目标 " + y
                            + " 实际 " + now + " 第 " + tries[0] + " 次");
                    if (Math.abs(now - y) > 2 && tries[0] < 8) {
                        tries[0]++;
                        sv.postDelayed(this, 50);
                    }
                } catch (Throwable ignore) {
                }
            }
        };
        try {
            findViewById(android.R.id.content).post(apply);
        } catch (Throwable ignore) {
        }
    }

    @Override
    public void closeWithFade() { close(); }

    @Override
    public void open(String page, String title) {
        
        try {
            android.view.View mod = com.cathelperawa.owo.app.ui.ScriptScreen.build(this, page);
            if (mod != null) {
                current = mod;
                setContentView(current);
                return;
            }
        } catch (Throwable ignore) {
        }
        HookLog.d("AI 子页请求: " + page + "（" + title + "），尚未实现");
    }

    @Override
    public void setPageView(View v) {
        current = v;
        setContentView(v);
    }

    @Override
    public void bindSwitch(String uiKey, SwitchBox box, Runnable onSynced) { }

    @Override
    public void setHeroTimer(java.util.Timer t) { }

    @Override
    public void setHomeScroll(ScrollView sv) { }

    @Override
    public void openTerminal() {
        
        try {
            com.cathelperawa.owo.app.ui.TerminalPanel.show(this);
        } catch (Throwable t) {
            HookLog.e("打开终端失败", t);
        }
    }

    @Override
    
    public void showApiDialog() {
        try {
            final float den = getResources().getDisplayMetrics().density;
            final android.app.Dialog d = new android.app.Dialog(this,
                    android.R.style.Theme_Translucent_NoTitleBar);

            LinearLayout rootV = new LinearLayout(this);
            rootV.setOrientation(LinearLayout.VERTICAL);
            rootV.setBackgroundColor(0xB3303A4D);
            rootV.setPadding((int) (20 * den), (int) (52 * den),
                    (int) (20 * den), (int) (30 * den));

            LinearLayout panel = new LinearLayout(this);
            panel.setOrientation(LinearLayout.VERTICAL);
            android.graphics.drawable.GradientDrawable bg =
                    new android.graphics.drawable.GradientDrawable();
            bg.setColor(0xFFF4F8FF);
            bg.setCornerRadius(22 * den);
            panel.setBackgroundDrawable(bg);
            panel.setPadding((int) (16 * den), (int) (16 * den),
                    (int) (16 * den), (int) (16 * den));

            TextView title = new TextView(this);
            title.setText("猫娘 AI · 内置 API");
            title.setTextSize(17);
            title.setTextColor(0xFF43587A);
            title.setTypeface(title.getTypeface(), android.graphics.Typeface.BOLD);
            panel.addView(title, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));

            TextView body = new TextView(this);
            body.setTextSize(11.5f);
            body.setTextColor(0xFF2F3E56);
            body.setText(aiApiDoc());
            body.setTextIsSelectable(true);
            body.setPadding(0, (int) (10 * den), (int) (6 * den), (int) (6 * den));

            android.widget.ScrollView sv = new android.widget.ScrollView(this);
            sv.addView(body);
            LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
            panel.addView(sv, slp);

            TextView close = new TextView(this);
            close.setText("知道了");
            close.setTextSize(13);
            close.setTextColor(0xFFE85D9C);
            close.setGravity(android.view.Gravity.CENTER);
            android.graphics.drawable.GradientDrawable cg =
                    new android.graphics.drawable.GradientDrawable();
            cg.setColor(0xFFFFE3EE);
            cg.setCornerRadius(14 * den);
            close.setBackgroundDrawable(cg);
            close.setPadding(0, (int) (11 * den), 0, (int) (11 * den));
            LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            clp.topMargin = (int) (12 * den);
            panel.addView(close, clp);
            close.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { d.dismiss(); }
            });

            rootV.addView(panel, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
            d.setContentView(rootV);
            android.view.Window w = d.getWindow();
            if (w != null) {
                w.setLayout(android.view.WindowManager.LayoutParams.MATCH_PARENT,
                        android.view.WindowManager.LayoutParams.MATCH_PARENT);
            }
            d.show();
        } catch (Throwable t) {
            HookLog.e("API 文档弹窗失败", t);
        }
    }

    private static String aiApiDoc() {
        return ""
                + "【问 / 说】\n"
                + "AiApi.ask(\"用一句话解释量子纠缠\")\n"
                + "AiApi.ask(peerUid, \"我们刚才在聊什么\")   // 带会话记忆\n"
                + "AiApi.say(2, peerUid, \"跟大家打个招呼\")   // 生成并发送\n"
                + "AiApi.say(1, peerUid, \"回他一句\", true)     // 第4参=记进记忆\n"
                + "\n"
                + "【看图】\n"
                + "AiApi.describeImage(\"/sdcard/a.jpg\")\n"
                + "AiApi.describeBase64(b64, \"图里的字是什么\")\n"
                + "\n"
                + "【状态 / 切换】\n"
                + "AiApi.status()            // 一行摘要\n"
                + "AiApi.enabled() / setEnabled(true)\n"
                + "AiApi.providers()         // [\"名字 | 模型 | id\"]\n"
                + "AiApi.useProvider(id)\n"
                + "\n"
                + "【人设】\n"
                + "AiApi.getPersona() / setPersona(\"你是…\")\n"
                + "\n"
                + "【记忆】\n"
                + "AiApi.memoryPeers() / memorySize(peer) / memoryTotal()\n"
                + "AiApi.memoryDump(peer)    // [\"user: 你好\", …]\n"
                + "AiApi.remember(peer, \"assistant\", \"补一句\")\n"
                + "AiApi.clearMemory(peer) / clearAllMemory()\n"
                + "\n"
                + "【表情】\n"
                + "AiApi.emojiCount() / emojiList()\n"
                + "AiApi.sendEmoji(2, peer, \"笑死\")\n"
                + "AiApi.classifyEmoji()     // 后台识图分类\n"
                + "\n"
                + "【CatApi 转发（脚本常用）】\n"
                + "CatApi.aiAsk(text) / aiAsk(peer, text)\n"
                + "CatApi.aiSay(chatType, peer, prompt)\n"
                + "CatApi.aiEmoji(chatType, peer, label)\n"
                + "CatApi.aiDescribeImage(path)\n"
                + "\n"
                + "【约定】\n"
                + "· peerUid：群=群号，私聊=u_xxx（同 EventApi.Message.peerUid）\n"
                + "· ask/describeImage 失败返回空串，不抛异常\n"
                + "· say/sendEmoji 失败返回 false\n"
                + "· 全部是同步调用，别在 UI 线程直接调\n"
                + "\n"
                + "完整文档：docs/API总表.md";
    }
}
