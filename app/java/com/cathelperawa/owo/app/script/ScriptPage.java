package com.cathelperawa.owo.app.script;

import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.cathelperawa.owo.app.ui.Page;
import com.cathelperawa.owo.app.ui.SwitchBox;

import java.util.List;

public final class ScriptPage extends Page {

    private static final int C_MAIN = 0xFF3C4B60;
    private static final int C_SUB = 0xFF8A9BB0;
    private static final int C_PINK = 0xFFE85D9C;
    private static final int C_GREEN = 0xFF2E9E5B;

    private LinearLayout body;
    
    private boolean kernelOpen = false;

    private android.os.FileObserver watcher;
    
    private String keyword = "";
    
    private int filter = 0;

    private final java.util.Map<String, Long> reloadDebounce =
            new java.util.HashMap<String, Long>();

    
    private void autoReload(final String name) {
        long now = System.currentTimeMillis();
        Long last = reloadDebounce.get(name);
        if (last != null && now - last < 500) return;
        reloadDebounce.put(name, now);
        try {
            if (!ScriptHost.isEnabled(name)) return;
            com.cathelperawa.owo.app.core.Bg.run(new Runnable() {
                @Override public void run() {
                    try {
                        ScriptHost.Result r = ScriptHost.enable(name);
                        com.cathelperawa.owo.app.core.HookLog.i("[脚本] 自动重载 "
                                + name + " → " + (r.ok ? "成功" : r.message));
                    } catch (Throwable t) {
                        com.cathelperawa.owo.app.core.HookLog.w("[脚本] 自动重载失败 " + t);
                    }
                }
            }, "cat-script-reload");
        } catch (Throwable ignore) {
        }
    }

    private void pruneSkin() {
        try {
            int n = com.cathelperawa.owo.app.core.Skin.prune(ScriptHost.enabledNames());
            if (n > 0) {
                com.cathelperawa.owo.app.core.HookLog.i("[脚本] 进页面清理了 " + n + " 项孤儿皮肤覆盖");
            }
        } catch (Throwable ignore) {
        }
    }

    private void startWatch() {
        if (watcher != null) return;
        try {
            java.io.File d = ScriptHost.dir();
            if (d == null) return;
            watcher = new android.os.FileObserver(d.getAbsolutePath(),
                    android.os.FileObserver.CREATE
                            | android.os.FileObserver.DELETE
                            | android.os.FileObserver.MODIFY
                            | android.os.FileObserver.MOVED_TO
                            | android.os.FileObserver.MOVED_FROM) {
                @Override public void onEvent(int event, String path) {
                    if (path == null || !path.endsWith(".qqs")) return;
                    final String name = path.substring(0, path.length() - 4);
                    
                    if (event == android.os.FileObserver.MODIFY
                            || event == android.os.FileObserver.MOVED_TO
                            || event == android.os.FileObserver.CLOSE_WRITE) {
                        autoReload(name);
                    }
                    runOnUi(new Runnable() {
                        @Override public void run() {
                            try {
                                render();
                            } catch (Throwable ignore) {
                            }
                        }
                    });
                }
            };
            watcher.startWatching();
            com.cathelperawa.owo.app.core.HookLog.i("[脚本] 目录监听已启动 " + d.getAbsolutePath());
        } catch (Throwable t) {
            com.cathelperawa.owo.app.core.HookLog.w("[脚本] 目录监听启动失败 " + t);
        }
    }
    private LinearLayout content;
    
    private static int mainScrollY = -1;

    public ScriptPage(Host host) {
        super(host);
    }

    @Override
    public View build() {
        View[] out = new View[2];
        sLast = this;
        subProvider = null;   
        body = pageRoot("脚本", "", out, true, 13, "bg_script", 0x55);
        pruneSkin();    
        startWatch();   
        content = new LinearLayout(ctx);
        content.setOrientation(LinearLayout.VERTICAL);
        body.addView(content, matchWrap(0));
        render();
        return out[0];
    }

    private void render() {
        
        final int keepY = subProvider != null ? 0 : scrollY();
        content.removeAllViews();
        if (subProvider != null) {
            renderSubPage();
            restoreY(0);
            return;
        }

        LinearLayout head = whiteCard(dp(20), 0xFFFBFDFF);
        head.setOrientation(LinearLayout.VERTICAL);
        head.setPadding(dp(18), dp(16), dp(18), dp(16));
        head.addView(text("脚本目录", 16, C_MAIN, true, dp(4)), matchWrap(0));
        String path = "";
        try {
            java.io.File d = ScriptHost.dir();
            if (d != null) path = d.getAbsolutePath();
        } catch (Throwable ignore) {
        }
        head.addView(text(path.length() == 0 ? "（找不到可写目录）" : path,
                11.5f, 0xFF3C78D8, false, dp(6)), matchWrap(0));
        head.addView(text(
                "用 BeanShell 跑 Java 语法。UI、JSON、网络包已自动 import，不用自己写。\n"
                + "· 快捷函数 getString/putString · qqToast · log · getNowActivity · sendMsg\n"
                + "· 看宿主  ctx.probe(类名, 方法) 看签名；ctx.tryCall 自适应调用\n"
                + "· 加载库  ctx.loadJar(lib/x.jar) 自动转 dex；ctx.defineClass 动态造类\n"
                + "· 命令    ctx.route(\"id\", new String[]{\"触发词\"}, handler)\n"
                + "· 发消息  文件头写 // capability: send\n"
                + "· 作用域  文件头写 // scope: group 或 friend\n"
                + "· 更多    import com.cathelperawa.owo.app.api.*\n"
                + "· 常驻    打开开关就生效，重启 TIM 自动恢复",
                11, C_SUB, false, dp(6)), matchWrap(0));

        
        
        toolGrid = new LinearLayout(ctx);
        toolGrid.setOrientation(LinearLayout.VERTICAL);

        toolCount = 0;
        toolRow = null;
        head.addView(toolGrid, matchWrap(0));

        TextView reloadAll = solidPill("全部重载", 0xFFEAF0F8, 0xFF5A6B80);
        reloadAll.setTextColor(0xFF5A6B80);
        press(reloadAll);
        reloadAll.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                press(v);
                new Thread(new Runnable() {
                    @Override public void run() {
                        final int n = ScriptHost.reloadAll();
                        runOnUi(new Runnable() {
                            @Override public void run() {
                                toast("已重载 " + n + " 个脚本");
                                render();
                            }
                        });
                    }
                }, "cat-script").start();
            }
        });
        addTool(reloadAll);

        TextView refresh = solidPill("刷新列表", 0xFFEAF0F8, 0xFF5A6B80);
        refresh.setTextColor(0xFF5A6B80);
        press(refresh);
        refresh.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                render();
                toast("已重新扫描脚本目录");
            }
        });
        addTool(refresh);

        TextView statsBtn = solidPill("运行统计", 0xFFEAF0F8, 0xFF5A6B80);
        statsBtn.setTextColor(0xFF5A6B80);
        press(statsBtn);
        statsBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                press(v);
                String sum = ScriptExec.summary();
                if (sum == null || sum.trim().length() == 0) {
                    toast("还没有执行记录（脚本跑过之后才有）");
                    return;
                }
                java.util.List<String> en = ScriptHost.enabledNames();
                String anchor = en.isEmpty() ? "" : en.get(0);
                if (anchor.length() == 0) {
                    toast(sum);
                    return;
                }
                openConsoleWith(anchor, "【运行统计】次数 / 均耗时 / 峰值 / 错误 / 超时 / 丢弃\n" + sum);
            }
        });
        addTool(statsBtn);

        TextView audit = solidPill("注入审计", 0xFFEAF0F8, 0xFF5A6B80);
        audit.setTextColor(0xFF5A6B80);
        press(audit);
        audit.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                host.open("audit", "注入审计");
            }
        });
        addTool(audit);

        TextView importBtn = solidPill("导入脚本", 0xFFEAF0F8, 0xFF5A6B80);
        importBtn.setTextColor(0xFF5A6B80);
        press(importBtn);
        importBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                android.app.Activity act = activity();
                if (act == null) {
                    toast("拿不到 Activity");
                    return;
                }
                try {
                    android.content.Intent it = new android.content.Intent(
                            android.content.Intent.ACTION_OPEN_DOCUMENT);
                    it.addCategory(android.content.Intent.CATEGORY_OPENABLE);
                    it.setType("*/*");
                    act.startActivityForResult(it, com.cathelperawa.owo.app.ConsoleActivity.REQ_IMPORT_SCRIPT);
                } catch (Throwable t) {
                    toast("打不开文件选择器：" + t);
                }
            }
        });
        addTool(importBtn);

        TextView fromExp = solidPill("从导出目录导入", 0xFFEAF0F8, 0xFF5A6B80);
        fromExp.setTextColor(0xFF5A6B80);
        press(fromExp);
        fromExp.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                press(v);
                new Thread(new Runnable() {
                    @Override public void run() {
                        final String msg = ModPack.importNewDetail();
                        runOnUi(new Runnable() {
                            @Override public void run() {
                                toast(msg);
                                render();
                            }
                        });
                    }
                }, "cat-script").start();
            }
        });
        addTool(fromExp);

        final android.widget.EditText search = new android.widget.EditText(ctx);
        search.setTextSize(13.5f);
        search.setHint("搜索脚本名 / 说明…");
        search.setSingleLine(true);
        search.setText(keyword);
        search.setPadding(dp(14), dp(10), dp(14), dp(10));
        search.setTextColor(0xFF2B3A55);
        search.setHintTextColor(0xFF9AA8BC);
        GradientDrawable sbg = new GradientDrawable();
        sbg.setColor(0xFFFFFFFF);
        sbg.setCornerRadius(dp(12));
        sbg.setStroke(dp(1), 0x22000000);
        search.setBackgroundDrawable(sbg);
        search.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s1, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s1, int a, int b, int c) { }
            @Override public void afterTextChanged(android.text.Editable e) {
                keyword = e == null ? "" : e.toString().trim();
                
                renderList();
            }
        });
        LinearLayout sl = new LinearLayout(ctx);
        sl.setOrientation(LinearLayout.HORIZONTAL);
        sl.setPadding(0, dp(10), 0, 0);
        sl.addView(search, new LinearLayout.LayoutParams(0, -2, 1f));
        head.addView(sl, matchWrap(0));

        LinearLayout chips2 = new LinearLayout(ctx);
        chips2.setOrientation(LinearLayout.HORIZONTAL);
        chips2.setPadding(0, dp(8), 0, 0);
        final String[] labels = new String[]{"全部", "已启用", "内核", "AI"};
        for (int fi = 0; fi < labels.length; fi++) {
            final int fIdx = fi;
            final TextView chip = solidPill(labels[fi],
                    filter == fi ? 0xFF3C78D8 : 0xFFE9EFF7,
                    filter == fi ? 0xFFFFFFFF : 0xFF5A6B80);
            chip.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    press(v);
                    filter = fIdx;
                    render();
                }
            });
            LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(-2, -2);
            clp.rightMargin = dp(8);
            chips2.addView(chip, clp);
        }
        head.addView(chips2, matchWrap(0));

        content.addView(head, matchWrap(dp(14)));

        try {
            if (com.cathelperawa.owo.app.core.SafeMode.active()) {
                LinearLayout banner = whiteCard(dp(16), 0x1AD9534F);
                banner.setOrientation(LinearLayout.VERTICAL);
                banner.setPadding(dp(16), dp(12), dp(16), dp(12));
                banner.addView(text("模块处于安全模式", 14, 0xFFC0392B, true, dp(3)), matchWrap(0));
                banner.addView(text("只加载最小 hook —— 很多功能会静默失效（比如改本地文本）。\n"
                                + "原因：" + com.cathelperawa.owo.app.core.SafeMode.why(),
                        10.5f, 0xFFB25B33, false, 0), matchWrap(0));

                TextView fix = plainBtn("恢复正常（重启 TIM 生效）", 0xFF2E9E5B);
                fix.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        press(v);
                        boolean ok = com.cathelperawa.owo.app.core.SafeMode.disable(ctx);
                        toast(ok ? "已清除安全模式，重启 TIM 后全部功能恢复" : "清除失败");
                        render();
                    }
                });
                LinearLayout bw = new LinearLayout(ctx);
                bw.setOrientation(LinearLayout.HORIZONTAL);
                bw.setGravity(Gravity.END);
                bw.setPadding(0, dp(8), 0, 0);
                bw.addView(fix, wrapWrap());
                banner.addView(bw, matchWrap(0));
                content.addView(banner, matchWrap(dp(12)));
            }
        } catch (Throwable ignore) {
        }

        
        
        LinearLayout setHead = whiteCard(dp(16), 0xFFF6F8FC);
        setHead.setOrientation(LinearLayout.HORIZONTAL);
        setHead.setGravity(Gravity.CENTER_VERTICAL);
        setHead.setPadding(dp(16), dp(12), dp(16), dp(12));
        setHead.addView(text("高级设置", 14, C_MAIN, true, 0), new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        setHead.addView(text(settingsOpen ? "收起 ▴" : "展开 ▾", 11, 0xFF3C78D8, true, 0));
        setHead.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                press(v);
                settingsOpen = !settingsOpen;
                render();
            }
        });
        content.addView(setHead, matchWrap(dp(10)));
        if (settingsOpen) {
            content.addView(trustCard(), matchWrap(dp(10)));
            content.addView(kernelTrustCard(), matchWrap(dp(10)));
            content.addView(extraDirCard(), matchWrap(dp(10)));
        }

        content.addView(aiSwitchCard(), matchWrap(dp(12)));

        try {
            int nCfg = com.cathelperawa.owo.app.core.ConfigOverride.size();
            if (nCfg > 0) {
                TextView cf = text("脚本正在接管 " + nCfg + " 个模块开关（关掉对应脚本即恢复）",
                        11, 0xFFB25B33, false, 0);
                cf.setPadding(dp(6), dp(4), 0, dp(4));
                content.addView(cf, matchWrap(0));
            }
        } catch (Throwable ignore) {
        }

        listHost = new LinearLayout(ctx);
        listHost.setOrientation(LinearLayout.VERTICAL);
        content.addView(listHost, matchWrap(0));
        renderList();

        TextView ver = text("猫娘助手 v"
                        + com.cathelperawa.owo.app.BuildInfo.VERSION_NAME
                        + " · 脚本子系统",
                11, 0xFFA9B7C9, false, 0);
        ver.setGravity(Gravity.CENTER);
        ver.setPadding(dp(8), dp(16), dp(8), dp(6));
        content.addView(ver, matchWrap(0));

        TextView foot = text("· 试跑 = 只记日志，不真发、不注册命令（先试再启用）\n"
                        + "· 启用 = 常驻运行，群里发触发词就会执行\n"
                        + "· 命令共 " + RouteRegistry.size() + " 条 · 已启用 "
                        + ScriptHost.enabledNames().size() + " 个脚本",
                10.5f, C_SUB, false, 0);
        foot.setPadding(dp(8), dp(12), dp(8), dp(14));
        content.addView(foot, matchWrap(0));
        restoreY(keepY);
    }

    private int scrollY() {
        try {
            android.widget.ScrollView sv = findScroll(content);
            return sv == null ? -1 : sv.getScrollY();
        } catch (Throwable t) {
            return -1;
        }
    }

    private void restoreY(final int y) {
        if (y <= 0 || content == null) return;
        final int[] tries = new int[]{0};
        content.post(new Runnable() {
            @Override public void run() {
                try {
                    android.widget.ScrollView sv = findScroll(content);
                    if (sv == null) return;
                    sv.scrollTo(0, y);
                    if (Math.abs(sv.getScrollY() - y) > 2 && tries[0] < 8) {
                        tries[0]++;
                        sv.postDelayed(this, 50);
                    }
                } catch (Throwable ignore) {
                }
            }
        });
    }

    private void openSub(String prov) {
        mainScrollY = scrollY();
        subProvider = prov;
        render();
    }

    private void backFromSub() {
        subProvider = null;
        render();
        restoreY(mainScrollY);
    }

    private void renderSubPage() {
        final String prov = subProvider;

        LinearLayout head = whiteCard(dp(18), 0xF2FFFFFF);
        head.setOrientation(LinearLayout.VERTICAL);
        head.setPadding(dp(16), dp(12), dp(16), dp(12));

        TextView back = text("‹ 返回脚本列表", 13, 0xFF3C78D8, true, dp(6));
        back.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                backFromSub();
            }
        });
        head.addView(back, matchWrap(0));
        head.addView(text("引擎提供者：" + prov, 15, C_MAIN, true, dp(3)), matchWrap(0));
        String provided = "";
        java.util.List<String> exts = com.cathelperawa.owo.app.script.ScriptEngines
                .suffixesProvidedBy(prov);
        for (int i = 0; i < exts.size(); i++) {
            if (provided.length() > 0) provided += " ";
            provided += exts.get(i);
        }
        head.addView(text("提供引擎：" + (provided.length() == 0 ? "（已下线）" : provided),
                11, 0xFFE08A1E, false, 0), matchWrap(0));
        content.addView(head, matchWrap(dp(12)));

        java.util.List<String> kids = ScriptHost.childrenOf(prov);
        if (kids.isEmpty()) {
            LinearLayout empty = whiteCard(dp(18), 0xEEFFFFFF);
            empty.setPadding(dp(16), dp(16), dp(16), dp(16));
            empty.addView(text("还没有子脚本", 13, C_MAIN, true, dp(4)), matchWrap(0));
            empty.addView(text("把 ." + (provided.length() == 0 ? "xxx" : provided.trim())
                            + " 文件放进脚本目录，刷新就会出现在这里",
                    11, C_SUB, false, 0), matchWrap(0));
            content.addView(empty, matchWrap(dp(10)));
        } else {
            for (int i = 0; i < kids.size(); i++) {
                content.addView(scriptCard(kids.get(i)), matchWrap(dp(10)));
            }
        }
    }

    private LinearLayout listHost;

    private LinearLayout toolGrid;
    private LinearLayout toolRow;
    private int toolCount;

    private boolean settingsOpen;

    private void addTool(View v) {
        if (toolGrid == null) return;
        if (toolRow == null || toolCount % 2 == 0) {
            toolRow = new LinearLayout(ctx);
            toolRow.setOrientation(LinearLayout.HORIZONTAL);
            toolRow.setPadding(0, dp(6), 0, 0);
            toolGrid.addView(toolRow, matchWrap(0));
        }
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1f);
        lp.rightMargin = (toolCount % 2 == 0) ? dp(6) : 0;
        if (v instanceof TextView) ((TextView) v).setGravity(Gravity.CENTER);
        toolRow.addView(v, lp);
        toolCount++;
    }

    private static String subProvider = null;

    private static ScriptPage sLast = null;

    public static boolean backToMain() {
        if (subProvider == null) return false;
        try {
            if (sLast != null && sLast.content != null) sLast.backFromSub();
            else subProvider = null;
        } catch (Throwable ignore) {
            subProvider = null;
        }
        return true;
    }

    private static String[] splitKeyword(String kw) {
        if (kw == null) return new String[0];
        String t = kw.trim().toLowerCase();
        if (t.length() == 0) return new String[0];
        String[] raw = t.split("\\s+");
        java.util.ArrayList<String> out = new java.util.ArrayList<String>();
        for (int i = 0; i < raw.length; i++) {
            if (raw[i] != null && raw[i].length() > 0) out.add(raw[i]);
        }
        return out.toArray(new String[0]);
    }

    private boolean matchesKeyword(String name, String[] tokens) {
        StringBuilder sb = new StringBuilder(name == null ? "" : name.toLowerCase());
        try {
            ScriptMeta m = ScriptHost.metaOf(name);
            if (m != null) {
                if (m.desc != null) sb.append(' ').append(m.desc.toLowerCase());
                if (m.author != null) sb.append(' ').append(m.author.toLowerCase());
                if (m.id != null) sb.append(' ').append(m.id.toLowerCase());
            }
        } catch (Throwable ignore) {
        }
        try {
            java.io.File f = ScriptHost.entryOf(name);
            if (f != null) sb.append(' ').append(f.getName().toLowerCase());
            sb.append(' ').append(com.cathelperawa.owo.app.script.ScriptEngines
                    .engineLabel(f).toLowerCase());
        } catch (Throwable ignore) {
        }
        try {
            sb.append(' ').append(ModLevel.label(ModLevel.of(name)));
            if (ScriptHost.isExternal(name)) sb.append(" 外部 external");
            String prov = com.cathelperawa.owo.app.script.ScriptEngines.enginesProvidedBy(name);
            if (prov.length() > 0) sb.append(' ').append(prov.toLowerCase());
        } catch (Throwable ignore) {
        }
        String hay = sb.toString();
        for (int i = 0; i < tokens.length; i++) {
            if (hay.indexOf(tokens[i]) < 0) return false;
        }
        return true;
    }

    private void renderList() {
        if (listHost == null) return;

        try {
            ScriptHost.scanEngineDeclarations();
        } catch (Throwable ignore) {
        }
        listHost.removeAllViews();
        List<String> names = ScriptHost.list();
        String[] tokens = splitKeyword(keyword);
        if (keyword.length() > 0 || filter != 0) {
            java.util.List<String> kept = new java.util.ArrayList<String>();
            for (int i = 0; i < names.size(); i++) {
                String n = names.get(i);

                if (tokens != null && tokens.length > 0) {
                    if (!matchesKeyword(n, tokens)) continue;
                }
                if (filter == 1 && !ScriptHost.isEnabled(n)) continue;
                if (filter == 2 && !ModLevel.isKernel(ModLevel.of(n))) continue;
                if (filter == 3 && ModLevel.of(n) != ModLevel.AI_ONLY) continue;
                kept.add(n);
            }
            names = kept;
            if (names.isEmpty()) {
                LinearLayout none = whiteCard(dp(16), 0xEEFFFFFF);
                none.setPadding(dp(16), dp(16), dp(16), dp(16));
                none.addView(text("没有符合条件的脚本", 13, C_MAIN, true, dp(3)), matchWrap(0));
                none.addView(text("换个关键词，或点「全部」", 11, C_SUB, false, 0), matchWrap(0));
                listHost.addView(none, matchWrap(dp(10)));
                return;
            }
        }
        if (names.isEmpty()) {
            LinearLayout empty = whiteCard(dp(18), 0xEEFFFFFF);
            empty.setPadding(dp(18), dp(18), dp(18), dp(18));
            empty.addView(text("还没有脚本", 14, C_MAIN, true, dp(4)), matchWrap(0));
            empty.addView(text("点上面的「写一个示例脚本」，然后在文件管理器里改改就能跑",
                    11, C_SUB, false, 0), matchWrap(0));
            listHost.addView(empty, matchWrap(dp(14)));
        } else {
            java.util.List<String> plain = new java.util.ArrayList<String>();
            java.util.List<String> aiOnly = new java.util.ArrayList<String>();
            java.util.List<String> kernel = new java.util.ArrayList<String>();
            for (int i = 0; i < names.size(); i++) {
                int lv = ModLevel.of(names.get(i));
                if (ModLevel.isKernel(lv)) {
                    kernel.add(names.get(i));
                } else if (lv == ModLevel.AI_ONLY) {
                    aiOnly.add(names.get(i));
                } else {
                    plain.add(names.get(i));
                }
            }
            addSection("脚本", plain);
            addSection("AI 脚本 · 只有 AI 能触发", aiOnly);

            if (!kernel.isEmpty()) {
                LinearLayout kh = whiteCard(dp(16), 0xFFFFF3EA);
                kh.setOrientation(LinearLayout.VERTICAL);
                kh.setPadding(dp(15), dp(11), dp(15), dp(11));
                kh.addView(text("内核脚本 · " + kernel.size() + " 个   "
                                + (kernelOpen ? "收起 ▲" : "展开 ▼"),
                        13.5f, 0xFFD9480F, true, dp(3)), matchWrap(0));
                kh.addView(text("这些脚本会改变模块本身（加页面 / 改判断 / 换皮）。"
                        + "出错可能让 TIM 崩溃，默认折叠。", 10.5f, 0xFFB25B33, false, 0),
                        matchWrap(0));
                kh.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        press(v);
                        kernelOpen = !kernelOpen;
                        render();
                    }
                });
                listHost.addView(kh, matchWrap(dp(10)));

                if (kernelOpen) {
                    listHost.addView(kernelSwitchCard(), matchWrap(dp(10)));
                    listHost.addView(dangerSwitchCard(), matchWrap(dp(10)));
                    for (int i = 0; i < kernel.size(); i++) {
                        listHost.addView(scriptCard(kernel.get(i)), matchWrap(dp(10)));
                    }
                }
            }
        }

    }

    private TextView engineTag(String engine) {
        int bg = 0xFF6B7C93;
        if (engine.startsWith("Rhino")) bg = 0xFF2E7DD1;
        else if (engine.startsWith("Lua")) bg = 0xFF7B5AA6;
        else if (engine.startsWith("桥接")) bg = 0xFFE08A1E;
        TextView t = text(engine, 10, 0xFFFFFFFF, true, 0);
        GradientDrawable g = new GradientDrawable();
        g.setColor(bg);
        g.setCornerRadius(dp(7));
        t.setBackgroundDrawable(g);
        t.setPadding(dp(7), dp(2), dp(7), dp(2));
        return t;
    }

    private TextView solidPill(String label, int bg, int fg) {
        TextView t = new TextView(ctx);
        t.setText(label);
        t.setTextSize(11);
        t.setTextColor(fg);
        t.setGravity(Gravity.CENTER);
        t.setTypeface(android.graphics.Typeface.DEFAULT_BOLD, android.graphics.Typeface.BOLD);
        GradientDrawable g = new GradientDrawable();
        g.setColor(bg);
        g.setCornerRadius(dp(12));
        t.setBackgroundDrawable(g);
        t.setPadding(dp(11), dp(5), dp(11), dp(5));
        return t;
    }

    private TextView badge(String label, int bgColor) {
        TextView t = text(label, 9.5f, 0xFFFFFFFF, true, 0);
        GradientDrawable g = new GradientDrawable();
        g.setColor(bgColor);
        g.setCornerRadius(dp(7));
        t.setBackgroundDrawable(g);
        t.setPadding(dp(7), dp(1), dp(7), dp(1));
        return t;
    }

    private TextView levelTag(int level) {
        TextView t = text(ModLevel.label(level), 10, ModLevel.color(level), true, 0);
        GradientDrawable g = new GradientDrawable();
        g.setColor(ModLevel.bg(level));
        g.setCornerRadius(dp(7));
        t.setBackgroundDrawable(g);
        t.setPadding(dp(7), dp(2), dp(7), dp(2));
        return t;
    }

    private void addSection(String title, List<String> list) {
        if (list == null || list.isEmpty()) return;

        LinearLayout host = listHost != null ? listHost : content;
        TextView h = text(title + " · " + list.size() + " 个", 11.5f, C_SUB, true, 0);
        h.setPadding(dp(6), dp(10), 0, dp(2));
        host.addView(h, matchWrap(0));
        for (int i = 0; i < list.size(); i++) {
            host.addView(scriptCard(list.get(i)), matchWrap(dp(10)));
        }
    }

    private View trustCard() {
        LinearLayout card = whiteCard(dp(18), 0xFFF3FBF4);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(16), dp(12), dp(14), dp(12));

        LinearLayout txt = new LinearLayout(ctx);
        txt.setOrientation(LinearLayout.VERTICAL);
        final boolean on = ScriptTrust.full();
        LinearLayout tRow = new LinearLayout(ctx);
        tRow.setOrientation(LinearLayout.HORIZONTAL);
        tRow.setGravity(Gravity.CENTER_VERTICAL);
        tRow.addView(text("完全信任", 14, on ? 0xFF2E7D32 : C_MAIN, true, 0));
        TextView onTag = badge("开", on ? 0xFF2E7D32 : 0xFFB6C2D2);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(-2, -2);
        tlp.leftMargin = dp(6);
        tRow.addView(onTag, tlp);
        txt.addView(tRow, matchWrap(dp(2)));
        txt.addView(text("不查能力声明 · 内核脚本免授权 · JS 可碰宿主内部类",
                10.5f, C_SUB, false, 0), matchWrap(0));
        card.addView(txt, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        final SwitchBox box = new SwitchBox(ctx, on);
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(dp(52), dp(30));
        bp.leftMargin = dp(10);
        card.addView(box, bp);
        box.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                boolean z = !ScriptTrust.full();
                ScriptTrust.setFull(z);
                box.setChecked(z);
                press(v);
                toast(z ? "已开启完全信任：脚本权限全开，重载脚本后生效" : "已关闭：回到严格模式，重载脚本后生效");
                render();
            }
        });
        return card;
    }

    private View kernelTrustCard() {
        LinearLayout card = whiteCard(dp(18), 0xFFF7F4FC);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(16), dp(12), dp(14), dp(12));

        final boolean on = ScriptTrust.kernelTrust();
        LinearLayout txt = new LinearLayout(ctx);
        txt.setOrientation(LinearLayout.VERTICAL);
        LinearLayout tRow = new LinearLayout(ctx);
        tRow.setOrientation(LinearLayout.HORIZONTAL);
        tRow.setGravity(Gravity.CENTER_VERTICAL);
        tRow.addView(text("允许改脚本系统", 14, on ? 0xFF7B5AA6 : C_MAIN, true, 0));
        TextView onTag = badge("开", on ? 0xFF7B5AA6 : 0xFFB6C2D2);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(-2, -2);
        tlp.leftMargin = dp(6);
        tRow.addView(onTag, tlp);
        txt.addView(tRow, matchWrap(dp(2)));
        txt.addView(text("可 hook 安全模式 / 加载守卫 / 闸门本身（每次挂载留审计）",
                10.5f, C_SUB, false, 0), matchWrap(0));
        card.addView(txt, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        final SwitchBox box = new SwitchBox(ctx, on);
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(dp(52), dp(30));
        bp.leftMargin = dp(10);
        card.addView(box, bp);
        box.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (!ScriptTrust.full()) {
                    toast("要先打开「完全信任」");
                    return;
                }
                boolean z = !ScriptTrust.kernelTrust();
                ScriptTrust.setKernelTrust(z);
                box.setChecked(z);
                press(v);
                toast(z ? "已开启：脚本可改脚本系统（每次挂载都记日志）"
                        : "已关闭：保护区恢复默认拦截");
                render();
            }
        });
        return card;
    }

    private View extraDirCard() {
        LinearLayout card = whiteCard(dp(18), 0xFFFFFBF0);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(14), dp(16), dp(12));

        LinearLayout tRow = new LinearLayout(ctx);
        tRow.setOrientation(LinearLayout.HORIZONTAL);
        tRow.setGravity(Gravity.CENTER_VERTICAL);
        tRow.addView(text("外部脚本源", 14, 0xFF8A6D1F, true, 0));
        TextView tag = badge("隔离档", 0xFFE0A020);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(-2, -2);
        tlp.leftMargin = dp(6);
        tRow.addView(tag, tlp);
        card.addView(tRow, matchWrap(dp(4)));
        card.addView(text("填目录后会把那里的脚本也列出来。它们只能发消息、读写自己数据；"
                + "exec / 改宿主 / 内核能力一律不给。", 10.5f, 0xFF8D7A4A, false, dp(8)),
                matchWrap(0));

        final EditText ed = new EditText(ctx);
        ed.setTextSize(12.5f);
        ed.setText(ScriptHost.extraDirPath());
        ed.setHint("/sdcard/…");
        ed.setSingleLine(true);
        ed.setTextColor(0xFF2B3A55);
        ed.setHintTextColor(0xFFB9AE90);
        ed.setPadding(dp(12), dp(9), dp(12), dp(9));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFFFFFFFF);
        bg.setCornerRadius(dp(10));
        bg.setStroke(dp(1), 0x33B08A20);
        ed.setBackgroundDrawable(bg);
        card.addView(ed, matchWrap(0));

        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.END);
        row.setPadding(0, dp(10), 0, 0);

        TextView clear = solidPill("清空", 0x00000000, 0xFF9A8A62);
        clear.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                press(v);
                ScriptHost.setExtraDirPath("");
                toast("已清空外部源");
                render();
            }
        });

        TextView save = solidPill("保存", 0xFF2E7D32, 0xFFFFFFFF);
        save.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                press(v);
                String p = ed.getText() == null ? "" : ed.getText().toString().trim();
                ScriptHost.setExtraDirPath(p);
                toast(p.length() == 0 ? "已清空外部源" : "已保存：" + p);
                render();
            }
        });
        row.addView(clear);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(-2, -2);
        slp.leftMargin = dp(8);
        row.addView(save, slp);
        card.addView(row, matchWrap(0));
        return card;
    }

    private View dangerSwitchCard() {
        LinearLayout card = whiteCard(dp(18), 0xFFF3FBF4);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(16), dp(12), dp(14), dp(12));

        LinearLayout txt = new LinearLayout(ctx);
        txt.setOrientation(LinearLayout.VERTICAL);
        txt.addView(text("允许改构造器（危险）", 14, 0xFFD9480F, true, dp(2)), matchWrap(0));
        txt.addView(text("脚本可以替换 类#<init> / 类#<clinit>。改错会让模块加载失败、"
                + "控制台黑屏。默认关。万一真崩了：下次打开 TIM 会自动关闭所有脚本并进安全模式，不用手动处理。",
                10.5f, 0xFFB25B33, false, 0), matchWrap(0));
        card.addView(txt, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        final SwitchBox box = new SwitchBox(ctx, ModLevel.dangerAllowed());
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(dp(52), dp(30));
        bp.leftMargin = dp(10);
        card.addView(box, bp);

        View.OnClickListener l = new View.OnClickListener() {
            @Override public void onClick(View v) {
                boolean z = !ModLevel.dangerAllowed();
                ModLevel.setDangerAllowed(z);
                box.setChecked(z);
                press(v);
                toast(z ? "已允许改构造器 —— 出错会自动关闭所有脚本并进安全模式"
                        : "已禁止改构造器");
                render();
            }
        };
        box.setOnClickListener(l);
        card.setOnClickListener(l);
        return card;
    }

    private View kernelSwitchCard() {
        LinearLayout card = whiteCard(dp(18), 0xFFFFF7F0);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(16), dp(12), dp(14), dp(12));

        LinearLayout txt = new LinearLayout(ctx);
        txt.setOrientation(LinearLayout.VERTICAL);
        txt.addView(text("允许内核脚本", 14, 0xFFB25B33, true, dp(2)), matchWrap(0));
        txt.addView(text("总开关（默认关）。关闭时内核脚本点了也启动不了。",
                10.5f, 0xFFB25B33, false, 0), matchWrap(0));
        card.addView(txt, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        final SwitchBox box = new SwitchBox(ctx, ModLevel.kernelAllowed());
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(dp(52), dp(30));
        bp.leftMargin = dp(10);
        card.addView(box, bp);

        View.OnClickListener l = new View.OnClickListener() {
            @Override public void onClick(View v) {
                boolean z = !ModLevel.kernelAllowed();
                ModLevel.setKernelAllowed(z);
                box.setChecked(z);
                press(v);
                toast(z ? "内核脚本已允许（谨慎使用）" : "内核脚本已全局关闭");
                render();
            }
        };
        box.setOnClickListener(l);
        card.setOnClickListener(l);
        return card;
    }

    private View aiSwitchCard() {
        LinearLayout card = whiteCard(dp(18), 0xF2FFFFFF);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(16), dp(14), dp(14), dp(14));

        LinearLayout txt = new LinearLayout(ctx);
        txt.setOrientation(LinearLayout.VERTICAL);
        txt.addView(text("允许 AI 调用脚本", 14.5f, C_MAIN, true, dp(2)), matchWrap(0));
        txt.addView(text("总开关。关掉后 AI 既不能调用脚本工具、也不能写脚本；"
                + "群里发的命令照常能用", 11, C_SUB, false, 0), matchWrap(0));
        card.addView(txt, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        final SwitchBox box = new SwitchBox(ctx, aiToolOn());
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(dp(52), dp(30));
        bp.leftMargin = dp(10);
        card.addView(box, bp);

        View.OnClickListener l = new View.OnClickListener() {
            @Override public void onClick(View v) {
                boolean z = !aiToolOn();
                try {
                    com.cathelperawa.owo.app.core.ConfigStore.setBool("ai_script_tool", z);
                } catch (Throwable ignore) {
                }
                box.setChecked(z);
                press(v);
                toast(z ? "AI 可以调用脚本工具了" : "AI 不再调用脚本工具");
                render();
            }
        };
        box.setOnClickListener(l);
        card.setOnClickListener(l);
        return card;
    }

    private boolean aiToolOn() {
        try {
            return com.cathelperawa.owo.app.core.ConfigStore.getBool("ai_script_tool");
        } catch (Throwable t) {
            return true;
        }
    }

    private static final java.util.Set<String> expandedSet =
            java.util.Collections.synchronizedSet(new java.util.HashSet<String>());

    private View scriptCard(final String name) {
        final boolean on = ScriptHost.isEnabled(name);
        final int routes = RouteRegistry.countOf(name);
        final int tools = ScriptToolRegistry.countOf(name);
        final boolean expanded = expandedSet.contains(name);

        LinearLayout outer = whiteCard(dp(18), 0xF2FFFFFF);
        outer.setOrientation(LinearLayout.VERTICAL);
        outer.setPadding(dp(16), dp(14), dp(14), expanded ? dp(10) : dp(14));

        LinearLayout header = new LinearLayout(ctx);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout left = new LinearLayout(ctx);
        left.setOrientation(LinearLayout.VERTICAL);
        
        final int level = ModLevel.of(name);
        LinearLayout titleRow = new LinearLayout(ctx);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        titleRow.addView(text(name, 15, C_MAIN, true, 0), wrapWrap());
        
        TextView lvTag = levelTag(level);
        LinearLayout.LayoutParams lvp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lvp.leftMargin = dp(7);
        titleRow.addView(lvTag, lvp);
        
        try {
            String provided = com.cathelperawa.owo.app.script.ScriptEngines.enginesProvidedBy(name);
            if (provided.length() > 0) {
                TextView pv = text("提供 " + provided, 10, 0xFFFFFFFF, true, 0);
                GradientDrawable pg = new GradientDrawable();
                pg.setColor(0xFFE08A1E);
                pg.setCornerRadius(dp(7));
                pv.setBackgroundDrawable(pg);
                pv.setPadding(dp(7), dp(2), dp(7), dp(2));
                LinearLayout.LayoutParams pvp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                pvp.leftMargin = dp(5);
                titleRow.addView(pv, pvp);
            }
        } catch (Throwable ignore) {
        }
        
        try {
            String eng = com.cathelperawa.owo.app.script.ScriptEngines.engineLabel(
                    ScriptHost.entryOf(name));
            if (eng != null && eng.length() > 0 && !"BeanShell".equals(eng)) {
                TextView engT = engineTag(eng);
                LinearLayout.LayoutParams evp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                evp.leftMargin = dp(5);
                titleRow.addView(engT, evp);
            }
        } catch (Throwable ignore) {
        }
        left.addView(titleRow, matchWrap(dp(3)));

        String scLabel = ScriptScope.label(ScriptHost.scopeOf(name));
        final TextView status = text(
                on ? ("● 启用中" + (routes > 0 ? " · " + routes + " 条命令" : "")
                        + (tools > 0 ? " · " + tools + " 个工具" : "")
                        + (scLabel.length() > 0 ? " · " + scLabel : ""))
                   : "○ 未启用",
                11, on ? C_GREEN : C_SUB, false, 0);
        left.addView(status, matchWrap(0));

        try {
            if (ModLevel.isKernel(level)
                    && !com.cathelperawa.owo.app.core.KernelWhitelist.allowed(name)) {
                left.addView(text("⚠ 未授权内核能力 · 展开卡片点「授予内核权限」",
                        10.5f, 0xFFD9480F, false, 2), matchWrap(0));
            }
        } catch (Throwable ignore) {
        }

        try {
            final java.util.List<String> kids = ScriptHost.childrenOf(name);
            if (!kids.isEmpty()) {
                final String prov = name;
                TextView hdr = text("子脚本 · " + kids.size() + " 个　›", 11, 0xFFE08A1E, true, 0);
                hdr.setPadding(0, dp(6), 0, 0);
                hdr.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        openSub(prov);
                    }
                });
                left.addView(hdr, matchWrap(0));
            }
        } catch (Throwable ignore) {
        }

        header.addView(left, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        final SwitchBox box = new SwitchBox(ctx, on);
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(dp(52), dp(30));
        bp.leftMargin = dp(10);
        header.addView(box, bp);

        header.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                press(v);
                if (expandedSet.contains(name)) expandedSet.remove(name);
                else expandedSet.add(name);
                render();
            }
        });
        outer.addView(header, matchWrap(0));

        final Runnable toggle = new Runnable() {
            @Override public void run() {
                final boolean want = !ScriptHost.isEnabled(name);
                box.setChecked(want);
                status.setTextColor(C_SUB);
                status.setText(want ? "启用中…" : "停用中…");
                
                final long t0 = System.currentTimeMillis();
                final android.os.Handler ui = new android.os.Handler(
                        android.os.Looper.getMainLooper());
                final Runnable[] ticker = new Runnable[1];
                ticker[0] = new Runnable() {
                    @Override public void run() {
                        long sec = (System.currentTimeMillis() - t0) / 1000;
                        if (sec < 1) return;
                        status.setText((want ? "加载中… " : "停用中… ") + sec + "s");
                        ui.postDelayed(ticker[0], 1000);
                    }
                };
                ui.postDelayed(ticker[0], 1000);
                new Thread(new Runnable() {
                    @Override public void run() {
                        final ScriptHost.Result r = want
                                ? ScriptHost.enable(name) : ScriptHost.disable(name);
                        try { ui.removeCallbacks(ticker[0]); } catch (Throwable ignore) {}
                        runOnUi(new Runnable() {
                            @Override public void run() {
                                if (!r.ok) {
                                    
                                    box.setChecked(!want);
                                    status.setTextColor(0xFFD9534F);
                                    status.setText("✗ " + r.message);
                                    return;
                                }
                                render();
                            }
                        });
                    }
                }, "cat-script").start();
            }
        };
        box.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { press(v); toggle.run(); }
        });

        if (!expanded) return outer;

        View line = new View(ctx);
        line.setBackgroundColor(0x0D000000);
        LinearLayout.LayoutParams lpLine = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(1));
        lpLine.topMargin = dp(14);
        lpLine.bottomMargin = dp(12);
        outer.addView(line, lpLine);

        ScriptMeta meta = ScriptHost.metaOf(name);

        int crashes = ModHealth.crashes(name);
        if (crashes > 0) {
            detailRow(outer, ModHealth.banned(name) ? "已熔断" : "连续失败",
                    crashes + " / " + ModHealth.LIMIT + " 次"
                            + (ModHealth.lastReason(name).length() > 0
                                ? " · " + ModHealth.lastReason(name) : ""));
        }

        try {
            ScriptExec.Stat st = ScriptExec.stats(name);
            if (st != null && st.calls > 0) {
                StringBuilder sb = new StringBuilder();
                sb.append(st.calls).append(" 次 · 均 ")
                        .append(st.totalMs / Math.max(1, st.calls)).append("ms · 峰 ")
                        .append(st.maxMs).append("ms");
                if (st.errors > 0) sb.append(" · 错 ").append(st.errors);
                if (st.timeouts > 0) sb.append(" · 超时 ").append(st.timeouts);
                if (st.dropped > 0) sb.append(" · 丢弃 ").append(st.dropped);
                if (ScriptExec.isStuck(name)) sb.append(" · ⚠ 疑似卡死，点「重载」恢复");
                detailRow(outer, "运行", sb.toString());
            }
        } catch (Throwable ignore) {
        }

        try {

            String caps = ScriptAbility.capabilities(readSource(name));
            detailRow(outer, "能力", caps.length() > 0 ? caps : "无（只用基础 API）");
            String wired = ScriptAbility.wired(name);
            if (wired.length() > 0) detailRow(outer, "接入", wired);
        } catch (Throwable ignore) {
        }

        try {
            final java.util.List<String> risks = ScriptRisk.scan(readSource(name));
            if (!risks.isEmpty()) {
                boolean ok = ScriptRisk.confirmed(name, entryMtime(name));
                LinearLayout rbox = new LinearLayout(ctx);
                rbox.setOrientation(LinearLayout.VERTICAL);
                rbox.setPadding(0, dp(6), 0, 0);
                rbox.addView(text((ok ? "已确认风险 · " : "⚠ 未确认风险 · ") + ScriptRisk.describe(risks),
                        11.5f, ok ? 0xFFB26A00 : 0xFFC0392B, true, 0), matchWrap(0));
                rbox.addView(text("这类脚本会改动 TIM / 模块本身，出错可能让 TIM 崩溃。"
                        + (ok ? "脚本改动后需要重新确认。" : "启用前请先确认。"),
                        10.5f, C_SUB, false, dp(2)), matchWrap(0));
                if (!ok) {
                    TextView riskBtn = plainBtn("确认风险并启用", 0xFFC0392B);
                    riskBtn.setOnClickListener(new View.OnClickListener() {
                        @Override public void onClick(View v) {
                            press(v);
                            ScriptRisk.confirm(name, entryMtime(name));
                            toast("已确认：" + ScriptRisk.describe(risks));
                            new Thread(new Runnable() {
                                @Override public void run() {
                                    final ScriptHost.Result r = ScriptHost.enable(name);
                                    runOnUi(new Runnable() {
                                        @Override public void run() {
                                            toast(r.ok ? "已启用" : ("启用失败：" + r.message));
                                            render();
                                        }
                                    });
                                }
                            }, "cat-script").start();
                        }
                    });
                    LinearLayout rw = new LinearLayout(ctx);
                    rw.setOrientation(LinearLayout.HORIZONTAL);
                    rw.setPadding(0, dp(6), 0, 0);
                    rw.addView(riskBtn, wrapWrap());
                    rbox.addView(rw, matchWrap(0));
                }
                outer.addView(rbox, matchWrap(0));
            }
        } catch (Throwable ignore) {
        }

        try {
            final String errTxt = ScriptHost.errorText(name);
            if (errTxt != null && errTxt.trim().length() > 0) {
                String last = "";
                String[] lines = errTxt.split("\n");
                for (int i = lines.length - 1; i >= 0; i--) {
                    if (lines[i].trim().length() > 0) { last = lines[i].trim(); break; }
                }
                LinearLayout ebox = new LinearLayout(ctx);
                ebox.setOrientation(LinearLayout.VERTICAL);
                ebox.setPadding(0, dp(6), 0, 0);
                TextView eh = text("最近报错 · 点开看全部", 11.5f, 0xFFC0392B, true, 0);
                ebox.addView(eh, matchWrap(0));
                TextView eb = text(last.length() > 120 ? last.substring(0, 120) + "…" : last,
                        10.5f, 0xFFB25B33, false, dp(2));
                ebox.addView(eb, matchWrap(0));
                ebox.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        press(v);
                        openConsoleWith(name, errTxt);
                    }
                });
                outer.addView(ebox, matchWrap(0));
            }
        } catch (Throwable ignore) {
        }

        String[] fi = fileParts(name);
        {
            StringBuilder fbuf = new StringBuilder();
            fbuf.append(fi[0] == null ? "找不到文件" : (fi[0] + " · " + fi[1] + " · " + fi[2]));
            if (meta.version.length() > 0) fbuf.append(" · v").append(meta.version);
            if (meta.author.length() > 0) fbuf.append(" · ").append(meta.author);
            detailRow(outer, "文件", fbuf.toString());
        }

        String cmds = commandsOf(name);
        String tls = toolsOf(name);
        if (cmds.length() > 0) detailRow(outer, "命令", cmds);
        if (tls.length() > 0) detailRow(outer, "AI 工具", tls);
        if (cmds.length() == 0 && tls.length() == 0) {
            detailRow(outer, "内容", "没有注册命令或工具");
        }
        
        detailRow(outer, "说明", meta.desc.length() > 0 ? meta.desc : "—");

        java.util.List<ScriptPanelRegistry.Panel> panels = ScriptPanelRegistry.ofScript(name);
        for (int pi = 0; pi < panels.size(); pi++) {
            final ScriptPanelRegistry.Panel pn = panels.get(pi);
            LinearLayout prow = new LinearLayout(ctx);
            prow.setOrientation(LinearLayout.HORIZONTAL);
            prow.setGravity(Gravity.CENTER_VERTICAL);
            prow.setPadding(0, dp(10), 0, dp(2));

            TextView pl = text(pn.title, 13.5f,
                    pn.custom() ? 0xFF7B5AC6 : 0xFF3C78D8, true, 0);
            prow.addView(pl, new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            prow.addView(text("›", 20, 0xFFA8B4C4, true, 0), wrapWrap());
            prow.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    press(v);
                    try {
                        if (pn.custom()) {
                            host.pushPage(new ScriptPanelView(host, pn).build());
                        } else {
                            host.pushPage(new ScriptPanelPage(host, pn).build());
                        }
                    } catch (Throwable t) {
                        toast("打不开面板：" + t);
                    }
                }
            });
            outer.addView(prow, matchWrap(0));
        }

        LinearLayout autoRow = new LinearLayout(ctx);
        autoRow.setOrientation(LinearLayout.HORIZONTAL);
        autoRow.setGravity(Gravity.CENTER_VERTICAL);
        autoRow.setPadding(0, dp(10), 0, dp(2));

        LinearLayout autoTxt = new LinearLayout(ctx);
        autoTxt.setOrientation(LinearLayout.VERTICAL);
        autoTxt.addView(text("启动时自动加载", 13.5f, C_MAIN, true, dp(1)), matchWrap(0));
        autoTxt.addView(text("TIM 启动后自动启用这个脚本", 10.5f, C_SUB, false, 0), matchWrap(0));
        autoRow.addView(autoTxt, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        final SwitchBox autoBox = new SwitchBox(ctx, ScriptHost.isAutoLoad(name));
        autoRow.addView(autoBox, new LinearLayout.LayoutParams(dp(46), dp(26)));
        autoBox.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                boolean z = !ScriptHost.isAutoLoad(name);
                ScriptHost.setAutoLoad(name, z);
                autoBox.setChecked(z);
                press(v);
                toast(z ? "下次启动会自动加载" : "下次启动不加载");
            }
        });
        outer.addView(autoRow, matchWrap(0));

        LinearLayout devRow = new LinearLayout(ctx);
        devRow.setOrientation(LinearLayout.HORIZONTAL);
        devRow.setGravity(Gravity.CENTER_VERTICAL);
        devRow.setPadding(0, dp(8), 0, 0);

        LinearLayout devTxt = new LinearLayout(ctx);
        devTxt.setOrientation(LinearLayout.VERTICAL);
        devTxt.addView(text("开发模式（不熔断）", 13.5f, C_MAIN, true, dp(1)), matchWrap(0));
        devTxt.addView(text("失败只记录、不计数，写脚本时调错十几次也不会被禁用",
                10.5f, C_SUB, false, 0), matchWrap(0));
        devRow.addView(devTxt, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        final SwitchBox devBox = new SwitchBox(ctx, ModHealth.devMode(name));
        devRow.addView(devBox, new LinearLayout.LayoutParams(dp(46), dp(26)));
        devBox.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                boolean z = !ModHealth.devMode(name);
                ModHealth.setDevMode(name, z);
                devBox.setChecked(z);
                press(v);
                toast(z ? "开发模式已开：脚本不会再被熔断" : "开发模式已关：连续失败会重新计数");
                render();
            }
        });
        outer.addView(devRow, matchWrap(0));

        LinearLayout btns = new LinearLayout(ctx);
        btns.setOrientation(LinearLayout.HORIZONTAL);
        btns.setGravity(Gravity.END);

        
        try {
            if (ModLevel.isKernel(level) && !com.cathelperawa.owo.app.core.KernelWhitelist.allowed(name)) {
                TextView grant = plainBtn("授予内核权限", 0xFFD9480F);
                final String target = name;
                grant.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        press(v);
                        com.cathelperawa.owo.app.core.KernelWhitelist.grant(target);
                        toast("已授权：" + target + " 可以使用内核能力");
                        render();
                    }
                });
                btns.addView(grant);
            }
        } catch (Throwable ignore) {
        }

        final TextView result = text("", 10.5f, C_SUB, false, 0);
        result.setVisibility(View.GONE);
        outer.addView(result, matchWrap(dp(2)));

        TextView dry = plainBtn("试跑", 0xFF8A9BB0);
        dry.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                result.setVisibility(View.VISIBLE);
                result.setTextColor(C_SUB);
                result.setText("试跑中…");
                new Thread(new Runnable() {
                    @Override public void run() {
                        final ScriptHost.Result r = ScriptHost.dryRun(name);
                        runOnUi(new Runnable() {
                            @Override public void run() {
                                result.setTextColor(r.ok ? C_GREEN : 0xFFD9534F);
                                result.setText((r.ok ? "✓ " : "✗ ") + r.message
                                        + " · " + r.costMs + "ms");
                            }
                        });
                    }
                }, "cat-script").start();
            }
        });
        btns.addView(dry, wrapWrap());

        TextView reload = plainBtn("重载", 0xFF3C78D8);
        reload.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                result.setVisibility(View.VISIBLE);
                result.setTextColor(C_SUB);
                result.setText("重载中…");
                new Thread(new Runnable() {
                    @Override public void run() {
                        final ScriptHost.Result r = ScriptHost.enable(name);
                        runOnUi(new Runnable() {
                            @Override public void run() {
                                result.setTextColor(r.ok ? C_GREEN : 0xFFD9534F);
                                result.setText((r.ok ? "✓ " : "✗ ") + r.message);
                                render();
                            }
                        });
                    }
                }, "cat-script").start();
            }
        });
        btns.addView(reload, wrapWrap());

        TextView console = plainBtn("控制台", 0xFF7B5CC4);
        console.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                press(v);
                openConsole(name);
            }
        });
        btns.addView(console, wrapWrap());

        TextView apiBtn = plainBtn("API", 0xFF2E9E5B);
        apiBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                press(v);
                openConsoleWith(name, ScriptHost.apiDoc());
            }
        });
        btns.addView(apiBtn, wrapWrap());

        TextView exp = plainBtn("导出", 0xFF5A6B80);
        exp.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                press(v);
                final String err = ModPack.exportOneError(name);
                toast(err == null
                        ? ("已导出到：\n" + ModPack.exportDir().getPath())
                        : ("导出失败：" + err));
            }
        });
        btns.addView(exp, wrapWrap());

        if (ModHealth.crashes(name) > 0) {
            TextView heal = plainBtn("重置失败计数", 0xFF2E9E5B);
            heal.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    ModHealth.reset(name);
                    press(v);
                    toast("失败计数已清零，可以重新启用了");
                    render();
                }
            });
            btns.addView(heal, wrapWrap());
        }

        final TextView del = plainBtn("删除", 0xFFD9534F);
        del.setTag(Boolean.FALSE);
        del.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (!Boolean.TRUE.equals(del.getTag())) {
                    del.setTag(Boolean.TRUE);
                    del.setText("确认删除");
                    toast("再点一次真的删掉");
                    
                    del.postDelayed(new Runnable() {
                        @Override public void run() {
                            del.setTag(Boolean.FALSE);
                            del.setText("删除");
                        }
                    }, 3000);
                    return;
                }
                ScriptHost.Result r = ScriptHost.delete(name);
                toast(r.ok ? "已删除 " + name : "删除失败：" + r.message);
                expandedSet.remove(name);
                render();
            }
        });
        btns.addView(del, wrapWrap());

        
        android.widget.HorizontalScrollView btnScroll =
                new android.widget.HorizontalScrollView(ctx);
        btnScroll.setHorizontalScrollBarEnabled(false);
        btnScroll.addView(btns);
        LinearLayout.LayoutParams lpb = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lpb.topMargin = dp(8);
        outer.addView(btnScroll, lpb);

        return outer;
    }

    private String fileInfo(String name) {
        try {
            java.io.File d = ScriptHost.dir();
            if (d == null) return "";
            java.io.File f = new java.io.File(d, name + ".qqs");
            if (!f.exists()) return "文件不见了：" + name + ".qqs";
            long len = f.length();
            String size = len < 1024 ? (len + " B")
                    : (String.format(java.util.Locale.CHINA, "%.1f KB", len / 1024d));
            String time = new java.text.SimpleDateFormat("MM-dd HH:mm",
                    java.util.Locale.CHINA).format(new java.util.Date(f.lastModified()));
            return "文件  " + name + ".qqs · " + size + " · " + time;
        } catch (Throwable t) {
            return "";
        }
    }

    private String commandsOf(String name) {
        StringBuilder sb = new StringBuilder();
        try {
            java.util.List<RouteRegistry.Route> all = RouteRegistry.all();
            for (int i = 0; i < all.size(); i++) {
                RouteRegistry.Route r = all.get(i);
                if (!r.script.equals(name)) continue;
                if (sb.length() > 0) sb.append(" · ");
                sb.append(r.trigger());
            }
        } catch (Throwable ignore) {
        }
        return sb.toString();
    }

    private String toolsOf(String name) {
        StringBuilder sb = new StringBuilder();
        try {
            java.util.List<ScriptToolRegistry.Tool> all = ScriptToolRegistry.all();
            for (int i = 0; i < all.size(); i++) {
                ScriptToolRegistry.Tool t = all.get(i);
                if (!t.script.equals(name)) continue;
                if (sb.length() > 0) sb.append(" · ");
                sb.append(t.name);
            }
        } catch (Throwable ignore) {
        }
        return sb.toString();
    }

    private void detailRow(LinearLayout parent, String label, String value) {
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(3), 0, dp(3));

        TextView k = text(label, 11, 0xFF9FB0C7, false, 0);
        row.addView(k, new LinearLayout.LayoutParams(dp(52),
                LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView v = text(value, 11.5f, 0xFF5A6B80, false, 0);
        v.setLineSpacing(dp(2), 1f);
        row.addView(v, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        parent.addView(row, matchWrap(0));
    }

    
    private void openConsole(final String name) {
        if (!ScriptHost.isEnabled(name)) {
            toast("先启用这个脚本，控制台才能进它的解释器看变量");
            return;
        }
        try {
            android.app.Activity a = activity();
            if (a == null) {
                toast("拿不到界面，稍后再试");
                return;
            }
            new ScriptConsolePanel(a, name).show();
        } catch (Throwable t) {
            toast("控制台没打开：" + t);
        }
    }

    private void openConsoleWith(final String name, final String preload) {
        try {
            android.app.Activity a = activity();
            if (a == null) {
                toast("拿不到界面，稍后再试");
                return;
            }
            new ScriptConsolePanel(a, name, preload).show();
        } catch (Throwable t) {
            toast("没打开：" + t);
        }
    }

    private TextView plainBtn(String label, int color) {
        TextView t = text(label, 13.5f, color, true, 0);
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(14), dp(8), dp(14), dp(8));
        GradientDrawable g = new GradientDrawable();
        g.setColor(0x00000000);
        g.setCornerRadius(dp(10));
        t.setBackground(g);
        press(t);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(dp(2), 0, dp(2), 0);
        t.setLayoutParams(lp);
        return t;
    }

    
    private String readSource(String name) {
        try {
            java.io.File f = ScriptHost.entryOf(name);
            if (f == null || !f.exists()) return "";
            long len = Math.min(f.length(), 256 * 1024);
            java.io.FileInputStream in = new java.io.FileInputStream(f);
            try {
                byte[] b = new byte[(int) len];
                int n = in.read(b);
                return n <= 0 ? "" : new String(b, 0, n, "UTF-8");
            } finally {
                in.close();
            }
        } catch (Throwable t) {
            return "";
        }
    }

    private long entryMtime(String name) {
        try {
            java.io.File f = ScriptHost.entryOf(name);
            return f == null ? 0L : f.lastModified();
        } catch (Throwable t) {
            return 0L;
        }
    }

    private String[] fileParts(String name) {
        try {
            java.io.File d = ScriptHost.dir();
            if (d == null) return new String[]{null, "", ""};
            
            if (ScriptHost.isPackage(name)) {
                java.io.File entry = ScriptHost.entryOf(name);
                if (entry == null || !entry.exists()) return new String[]{null, "", ""};
                int n = ScriptHost.packageFileCount(name);
                String time = new java.text.SimpleDateFormat("MM-dd HH:mm",
                        java.util.Locale.CHINA).format(new java.util.Date(entry.lastModified()));
                return new String[]{name + "/" + entry.getName() + " · 包 · " + n + " 个文件",
                        "", time};
            }
            java.io.File f = ScriptHost.entryOf(name);
            if (f == null || !f.exists()) return new String[]{null, "", ""};
            long len = f.length();
            String size = len < 1024 ? (len + " B")
                    : (String.format(java.util.Locale.CHINA, "%.1f KB", len / 1024d));
            String time = new java.text.SimpleDateFormat("MM-dd HH:mm",
                    java.util.Locale.CHINA).format(new java.util.Date(f.lastModified()));
            return new String[]{f.getName(), size, time};
        } catch (Throwable t) {
            return new String[]{null, "", ""};
        }
    }

    private TextView chip(String s, int bgColor, int textColor) {
        TextView t = text(s, 12, textColor, true, 0);
        t.setPadding(dp(12), dp(7), dp(12), dp(7));
        GradientDrawable g = new GradientDrawable();
        g.setColor(bgColor);
        g.setCornerRadius(dp(9));
        t.setBackground(g);
        press(t);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(dp(8), 0, 0, 0);
        t.setLayoutParams(lp);
        return t;
    }

    private void runOnUi(Runnable r) {
        try {
            android.app.Activity a = activity();
            if (a != null) a.runOnUiThread(r);
            else r.run();
        } catch (Throwable t) {
            r.run();
        }
    }
}
