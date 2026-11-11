package com.cathelperawa.owo.app.ai;

import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.cathelperawa.owo.app.ui.CatSheet;
import com.cathelperawa.owo.app.ui.Page;

public final class AiPersonaPage extends Page {

    private static final int C_MAIN = 0xFF3C4B60;
    private static final int C_SUB = 0xFF8A9BB0;
    private static final int C_PINK = 0xFFE85D9C;

    private LinearLayout body;
    private LinearLayout content;
    private EditText editor;
    private TextView statLabel;
    
    private final java.util.List<View> dots = new java.util.ArrayList<View>();
    private final java.util.List<AiPersona.Preset> dotPresets =
            new java.util.ArrayList<AiPersona.Preset>();

    public AiPersonaPage(Host host) {
        super(host);
    }

    @Override
    public View build() {
        
        try {
            AiPersona.migrateLegacy();
        } catch (Throwable ignore) {
        }
        View[] out = new View[2];
        body = pageRoot("人设", "它是什么性格、怎么说话", out, true, 13, "bg_ai", 0x66);
        content = new LinearLayout(ctx);
        content.setOrientation(LinearLayout.VERTICAL);
        body.addView(content, matchWrap(0));
        render();
        return out[0];
    }

    private void render() {
        content.removeAllViews();
        dots.clear();
        dotPresets.clear();

        editor = new EditText(ctx);
        editor.setText(AiConfig.persona());
        editor.setTextSize(12.5f);
        editor.setTextColor(0xFF4A5A70);
        editor.setGravity(Gravity.TOP | Gravity.START);
        editor.setMinLines(14);
        editor.setMinHeight(dp(340));
        editor.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        GradientDrawable eg = new GradientDrawable();
        eg.setColor(0x59FFFFFF);
        eg.setCornerRadius(dp(14));
        eg.setStroke(Math.max(1, dp(1)), 0x33A9BEDC);
        editor.setBackgroundDrawable(eg);
        editor.setPadding(dp(14), dp(12), dp(14), dp(12));

        LinearLayout head = whiteCard(dp(20), 0xF2FFFFFF);
        head.setOrientation(LinearLayout.VERTICAL);
        head.setPadding(dp(16), dp(14), dp(16), dp(14));

        LinearLayout top = new LinearLayout(ctx);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);

        statLabel = text(statText(), 14, C_MAIN, true, 0);
        top.addView(statLabel, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView reset = pill("恢复默认", 0xFFE4ECF7);
        reset.setTextColor(0xFF5A6B80);
        press(reset);
        reset.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                AiPersona.resetDefault();
                editor.setText(AiConfig.persona());
                statLabel.setText(statText());
                toast("已恢复默认人设");
            }
        });
        top.addView(reset, wrapWrap());
        head.addView(top, matchWrap(dp(6)));

        head.addView(text("{{me}} = 你的昵称，{{peer}} = 对方名字，发出去前会自动替换",
                10.5f, C_SUB, false, 0), matchWrap(0));
        content.addView(head, matchWrap(dp(12)));

        content.addView(sectionTitle("模板"), matchWrap(dp(6)));
        AiPersona.Preset[] presets = AiPersona.allWithCustom();
        String curName = AiPersona.currentPresetName();
        for (int i = 0; i < presets.length; i++) {
            final AiPersona.Preset ps = presets[i];
            boolean chosen = ps.name.equals(curName);
            content.addView(presetCard(ps, chosen), matchWrap(dp(8)));
        }
        
        content.addView(newPresetCard(), matchWrap(dp(8)));

        content.addView(sectionTitle("自己写"), matchWrap(dp(12)));
        LinearLayout box = whiteCard(dp(18), 0xF2FFFFFF);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(12), dp(12), dp(12), dp(12));
        box.addView(editor, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT, 0f));

        LinearLayout btns = new LinearLayout(ctx);
        btns.setOrientation(LinearLayout.HORIZONTAL);
        btns.setGravity(Gravity.END);
        btns.setPadding(0, dp(10), 0, 0);

        TextView clear = pill("清空", 0xFFE4ECF7);
        clear.setTextColor(0xFF5A6B80);
        press(clear);
        clear.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                editor.setText("");
                save();
                statLabel.setText(statText());
            }
        });
        btns.addView(clear, wrapWrap());

        TextView saveBtn = pill("保存", 0xFFFFD9E6);
        saveBtn.setTextColor(C_PINK);
        press(saveBtn);
        LinearLayout.LayoutParams sp = wrapWrap();
        sp.leftMargin = dp(10);
        btns.addView(saveBtn, sp);
        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                save();
                statLabel.setText(statText());
                hideKeyboard(editor);
                editor.clearFocus();
                toast("人设已保存");
            }
        });
        box.addView(btns, matchWrap(0));
        content.addView(box, matchWrap(0));

        editor.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    save();
                    statLabel.setText(statText());
                    return;
                }
                
                editor.postDelayed(new Runnable() {
                    @Override public void run() {
                        scrollIntoView(editor, editor);
                    }
                }, 350);
            }
        });
        editor.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                editor.postDelayed(new Runnable() {
                    @Override public void run() {
                        scrollIntoView(editor, editor);
                    }
                }, 350);
            }
        });

        TextView note = text("保存后立刻生效，不用重启 TIM；人设只在开头喂一次，越具体越像人",
                10.5f, C_SUB, false, 0);
        note.setPadding(dp(6), dp(10), dp(6), dp(10));
        content.addView(note, matchWrap(0));
    }

    private void updateDots(AiPersona.Preset chosen) {
        for (int i = 0; i < dots.size(); i++) {
            boolean on = chosen != null && dotPresets.get(i).name.equals(chosen.name);
            View d = dots.get(i);
            GradientDrawable dg = new GradientDrawable();
            dg.setShape(GradientDrawable.OVAL);
            dg.setColor(on ? C_PINK : 0x00000000);
            dg.setStroke(Math.max(1, dp(1.5f)), on ? C_PINK : 0xFFC3CEDA);
           	d.setBackgroundDrawable(dg);
        }
    }

    private String statText() {
        String p = AiConfig.persona();
        int n = p == null ? 0 : p.length();
        return n == 0 ? "还没写人设" : (n + " 字");
    }

    private void save() {
        String t = editor.getText() == null ? "" : editor.getText().toString();
        String before = AiConfig.persona();
        AiConfig.setString("ai_persona", t);
        
        if (before == null || !before.equals(t)) AiPersona.clearPresetName();
    }

    private View sectionTitle(String s) {
        TextView t = text(s, 12, C_SUB, true, 0);
        t.setPadding(dp(6), dp(4), dp(6), 0);
        return t;
    }

    private View presetCard(final AiPersona.Preset ps, boolean chosen) {
        LinearLayout card = new LinearLayout(ctx);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(14), dp(12), dp(14), dp(12));

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(14));
        bg.setColor(chosen ? 0xF2FFF0F6 : 0xE6FFFFFF);
        bg.setStroke(Math.max(1, dp(2)), chosen ? 0x99FF9FCB : 0x26A9BEDC);
        card.setBackgroundDrawable(bg);

        LinearLayout txt = new LinearLayout(ctx);
        txt.setOrientation(LinearLayout.VERTICAL);
        txt.addView(text(ps.name, 14, C_MAIN, true, dp(2)), matchWrap(0));
        txt.addView(text(ps.desc, 10.5f, C_SUB, false, 0), matchWrap(0));
        card.addView(txt, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        if (!isBuiltin(ps)) {
            TextView del = text("✕", 13, 0xFFB6C1D0, true, 0);
            del.setPadding(dp(10), dp(4), dp(10), dp(4));
            press(del);
            del.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    CatSheet.pick(ctx, "删掉模板「" + ps.name + "」？",
                            new String[]{"删掉", "算了"}, new CatSheet.OnPick() {
                                @Override public void run(int i, String s) {
                                    if (i != 0) return;
                                    AiPersona.removeCustom(ps.name);
                                    toast("已删除「" + ps.name + "」");
                                    render();
                                }
                            });
                }
            });
            card.addView(del, wrapWrap());
        }

        if (!isBuiltin(ps)) {
            TextView del = pill("删", 0xFFFFD9E6);
            del.setTextColor(C_PINK);
            del.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    askDeletePreset(ps);
                }
            });
            LinearLayout.LayoutParams dp2 = wrapWrap();
            dp2.rightMargin = dp(10);
            card.addView(del, dp2);
        }

        View dot = new View(ctx);
        GradientDrawable dg = new GradientDrawable();
        dg.setShape(GradientDrawable.OVAL);
        dg.setColor(chosen ? C_PINK : 0x00000000);
        dg.setStroke(Math.max(1, dp(1.5f)), chosen ? C_PINK : 0xFFC3CEDA);
        dot.setBackgroundDrawable(dg);
        card.addView(dot, new LinearLayout.LayoutParams(dp(16), dp(16)));
        dots.add(dot);
        dotPresets.add(ps);

        press(card);
        card.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                AiPersona.apply(ps);
                if (editor != null) editor.setText(ps.content);
                statLabel.setText(statText());
                updateDots(ps);
                toast("已套用「" + ps.name + "」");
            }
        });
        if (!isBuiltin(ps)) {
            card.setOnLongClickListener(new View.OnLongClickListener() {
                @Override public boolean onLongClick(View v) {
                    CatSheet.pick(ctx, "删掉模板「" + ps.name + "」？",
                            new String[]{"删掉", "算了"}, new CatSheet.OnPick() {
                                @Override public void run(int i, String s) {
                                    if (i != 0) return;
                                    AiPersona.removeCustom(ps.name);
                                    toast("已删除");
                                    render();
                                }
                            });
                    return true;
                }
            });
        }
        return card;
    }

    private void askDeletePreset(final AiPersona.Preset ps) {
        CatSheet.pick(ctx, "删掉模板「" + ps.name + "」？", new String[]{"删掉", "算了"},
                new CatSheet.OnPick() {
                    @Override public void run(int i, String s) {
                        if (i != 0) return;
                        AiPersona.removeCustom(ps.name);
                        if (ps.name.equals(AiPersona.currentName())) {
                            AiPersona.setCurrentName("");
                        }
                        toast("已删除");
                        render();
                    }
                });
    }

    private static boolean isBuiltin(AiPersona.Preset p) {
        AiPersona.Preset[] base = AiPersona.all();
        for (int i = 0; i < base.length; i++) {
            if (base[i].name.equals(p.name)) return true;
        }
        return false;
    }

    private View newPresetCard() {
        LinearLayout card = new LinearLayout(ctx);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(14), dp(12), dp(14), dp(12));

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(14));
        bg.setColor(0xE6FFFFFF);
        bg.setStroke(Math.max(1, dp(1)), 0x66A9BEDC);
        card.setBackgroundDrawable(bg);

        TextView plus = text("＋", 16, C_PINK, true, 0);
        card.addView(plus, wrapWrap());

        LinearLayout txt = new LinearLayout(ctx);
        txt.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        tp.leftMargin = dp(10);
        txt.addView(text("新建模板", 13.5f, C_MAIN, true, dp(2)), matchWrap(0));
        txt.addView(text("把下面这框里的内容存成模板，自己起名字",
                10.5f, C_SUB, false, 0), matchWrap(0));
        card.addView(txt, tp);

        press(card);
        card.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                askNewTemplate();
            }
        });
        return card;
    }

    private void askNewTemplate() {
        if (editor == null) return;
        save();                       
        final String content = editor.getText() == null ? "" : editor.getText().toString();
        if (content.trim().length() == 0) {
            toast("先把人设内容写进下面的框里");
            return;
        }
        CatSheet.input(ctx, "新模板叫什么名字", "比如：毒舌闺蜜", "", new CatSheet.OnText() {
            @Override public void run(String name) {
                if (name == null || name.trim().length() == 0) {
                    toast("没取名字");
                    return;
                }
                AiPersona.Preset p = AiPersona.addCustom(name, content);
                if (p == null) {
                    toast("名字重复了（换一个）");
                    return;
                }
                AiPersona.setCurrentName(p.name);
                toast("已建立模板「" + p.name + "」");
                render();
            }
        });
    }
}
