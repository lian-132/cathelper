package com.cathelperawa.owo.app.ai;

import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.cathelperawa.owo.app.core.ConfigStore;
import com.cathelperawa.owo.app.ui.CatDialog;
import com.cathelperawa.owo.app.ui.CatSheet;
import com.cathelperawa.owo.app.ui.Page;
import com.cathelperawa.owo.app.ui.SwitchBox;

public final class AiAffinitySetPage extends Page {

    private static final int C_MAIN = 0xFF3C4B60;
    private static final int C_SUB = 0xFF8A9BB0;
    private static final int C_PINK = 0xFFE85D9C;

    private LinearLayout body;
    private LinearLayout content;

    public AiAffinitySetPage(Host host) {
        super(host);
    }

    @Override
    public View build() {
        View[] out = new View[2];
        body = pageRoot("好感度设置", "自动调节怎么算", out, true, 13, "bg_ai", 0x66);
        content = new LinearLayout(ctx);
        content.setOrientation(LinearLayout.VERTICAL);
        body.addView(content, matchWrap(0));
        render();
        return out[0];
    }

    private void render() {
        content.removeAllViews();

        LinearLayout sw = whiteCard(dp(20), 0xF2FFFFFF);
        sw.setOrientation(LinearLayout.VERTICAL);
        sw.setPadding(dp(16), dp(8), dp(16), dp(12));

        sw.addView(sectionTitle("开关"), matchWrap(0));
        sw.addView(switchRow("好感度", "记录每个人、拼进对话、按亲疏换语气",
                "ai_affinity", true), matchWrap(dp(4)));
        sw.addView(switchRow("自动调节", "关掉只保留手动加减（省 token，AI 不再归纳）",
                "ai_affinity_auto", false), matchWrap(dp(4)));
        sw.addView(switchRow("按关系换语气", "越熟说话越随意，越生越客气；关系生时不发表情",
                "ai_affinity_tone", false), matchWrap(dp(4)));
        sw.addView(switchRow("久不联系会回落", "很久没聊的人慢慢回到中性分（聊天就重新计时）",
                "ai_affinity_decay", false), matchWrap(0));
        content.addView(sw, matchWrap(dp(14)));

        LinearLayout pace = whiteCard(dp(20), 0xF2FFFFFF);
        pace.setOrientation(LinearLayout.VERTICAL);
        pace.setPadding(dp(16), dp(8), dp(16), dp(12));
        pace.addView(sectionTitle("调节节奏"), matchWrap(0));
        pace.addView(intRow("归纳间隔", "每攒几条消息让模型归纳一次，2 = 聊两句就更新（1~20）",
                "ai_affinity_every", 2, 1, 20, " 条"), matchWrap(0));
        pace.addView(intRow("最小间隔", "两次归纳至少隔多久，慢聊也跟得上（30~3600）",
                "ai_affinity_gap", 90, 30, 3600, " 秒"), matchWrap(0));
        pace.addView(intRow("单次幅度", "一次最多涨/跌多少（1~50）",
                "ai_affinity_step", 10, 1, 50, " 分"), matchWrap(0));
        pace.addView(intRow("每日上限", "一天最多涨/跌多少，0 = 不限（0~100）",
                "ai_affinity_daily", 10, 0, 100, " 分"), matchWrap(0));
        pace.addView(intRow("变化记录", "每人保留最近几条，0 = 不留（0~20）",
                "ai_affinity_log", 5, 0, 20, " 条"), matchWrap(0));
        pace.addView(intRow("闲置回落", "多少天不聊开始往回掉（1~365）",
                "ai_affinity_decay_days", 14, 1, 365, " 天"), matchWrap(0));
        content.addView(pace, matchWrap(dp(14)));

        LinearLayout init = whiteCard(dp(20), 0xF2FFFFFF);
        init.setOrientation(LinearLayout.VERTICAL);
        init.setPadding(dp(16), dp(8), dp(16), dp(12));
        init.addView(sectionTitle("新认识的"), matchWrap(0));
        init.addView(intRow("初始好感度", "第一次聊天从几分开始（1~100）",
                "ai_affinity_init", 50, 1, 100, " 分"), matchWrap(0));
        content.addView(init, matchWrap(dp(14)));

        LinearLayout tiers = whiteCard(dp(20), 0xF2FFFFFF);
        tiers.setOrientation(LinearLayout.VERTICAL);
        tiers.setPadding(dp(16), dp(8), dp(16), dp(12));
        tiers.addView(sectionTitle("等级分档（阈值必须递增）"), matchWrap(0));
        tiers.addView(intRow("冷淡", "低于这个分数算冷淡",
                "ai_affinity_t1", 25, 1, 100, " 分"), matchWrap(0));
        tiers.addView(intRow("客气", "低于这个算客气",
                "ai_affinity_t2", 45, 1, 100, " 分"), matchWrap(0));
        tiers.addView(intRow("普通", "低于这个算普通",
                "ai_affinity_t3", 65, 1, 100, " 分"), matchWrap(0));
        tiers.addView(intRow("熟络", "低于这个算熟络，再高就是亲近",
                "ai_affinity_t4", 85, 1, 100, " 分"), matchWrap(0));

        String preview = "当前分档：" + tierDemo();
        TextView pv = text(preview, 11, C_PINK, false, 0);
        pv.setPadding(0, dp(8), 0, 0);
        tiers.addView(pv, matchWrap(0));
        content.addView(tiers, matchWrap(dp(14)));

        LinearLayout danger = whiteCard(dp(20), 0xF2FFFFFF);
        danger.setOrientation(LinearLayout.HORIZONTAL);
        danger.setPadding(dp(16), dp(15), dp(16), dp(15));
        danger.setGravity(Gravity.CENTER_VERTICAL);
        danger.addView(text("清空全部档案", 14, C_MAIN, true, 0),
                new LinearLayout.LayoutParams(0,
                        LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        TextView clr = pill("清空", 0xFFFFD9E6);
        clr.setTextColor(0xFFD9534F);
        press(clr);
        clr.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { askClear(); }
        });
        danger.addView(clr, wrapWrap());
        content.addView(danger, matchWrap(dp(14)));

        TextView note = text("改完立刻生效，分数本身不会变（只是以后按新规则走）",
                10.5f, C_SUB, false, 0);
        note.setPadding(dp(8), dp(2), dp(8), dp(12));
        content.addView(note, matchWrap(0));
    }

    private View sectionTitle(String s) {
        TextView t = text(s, 12, C_PINK, true, 0);
        t.setPadding(0, dp(8), 0, dp(8));
        return t;
    }

    private View switchRow(final String title, String sub, final String key, boolean first) {
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        if (!first) row.setPadding(0, dp(6), 0, dp(6));

        LinearLayout txt = new LinearLayout(ctx);
        txt.setOrientation(LinearLayout.VERTICAL);
        txt.addView(text(title, 14.5f, C_MAIN, true, dp(2)), matchWrap(0));
        txt.addView(text(sub, 11, C_SUB, false, 0), matchWrap(0));
        row.addView(txt, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        final SwitchBox box = new SwitchBox(ctx, AiConfig.getBool(key));
        row.addView(box, new LinearLayout.LayoutParams(dp(52), dp(30)));

        View.OnClickListener l = new View.OnClickListener() {
            @Override public void onClick(View v) {
                boolean z = !AiConfig.getBool(key);
                AiConfig.setBool(key, z);
                box.setChecked(z);
                press(v);
            }
        };
        box.setOnClickListener(l);
        row.setOnClickListener(l);
        return row;
    }

    private View intRow(final String title, String sub, final String key,
                        int def, final int min, final int max, String unit) {
        final LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(7), 0, dp(7));

        LinearLayout txt = new LinearLayout(ctx);
        txt.setOrientation(LinearLayout.VERTICAL);
        txt.addView(text(title, 14, C_MAIN, true, dp(2)), matchWrap(0));
        txt.addView(text(sub, 10.5f, C_SUB, false, 0), matchWrap(0));
        row.addView(txt, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        int v = ConfigStore.getInt(key);
        if (v < min || v > max) v = def;

        final EditText et = new EditText(ctx);
        et.setText(String.valueOf(v));
        et.setTextSize(14.5f);
        et.setTextColor(C_PINK);
        et.setGravity(Gravity.CENTER);
        et.setSingleLine(true);
        et.setInputType(InputType.TYPE_CLASS_NUMBER);
        et.setImeOptions(EditorInfo.IME_ACTION_DONE);
        et.setPadding(dp(6), dp(4), dp(6), dp(4));
        et.setMinWidth(dp(52));
        et.setSelectAllOnFocus(true);

        android.graphics.drawable.GradientDrawable bg =
                new android.graphics.drawable.GradientDrawable();
        bg.setCornerRadius(dp(9));
        bg.setColor(0x14E85D9C);
        bg.setStroke(dp(1), 0x44E85D9C);
        et.setBackgroundDrawable(bg);

        final int fmin = min, fmax = max, fdef = def;
        final Runnable commit = new Runnable() {
            @Override public void run() {
                int n;
                try {
                    n = Integer.parseInt(et.getText().toString().trim());
                } catch (Throwable t) {
                    n = fdef;
                }
                if (n < fmin) n = fmin;
                if (n > fmax) n = fmax;
                ConfigStore.setInt(key, n);
                if (!String.valueOf(n).equals(et.getText().toString())) {
                    et.setText(String.valueOf(n));
                }
                et.setSelection(et.getText().length());
            }
        };

        et.setOnEditorActionListener(new android.widget.TextView.OnEditorActionListener() {
            @Override public boolean onEditorAction(android.widget.TextView tv, int action,
                                                    android.view.KeyEvent ev) {
                if (action == EditorInfo.IME_ACTION_DONE || action == EditorInfo.IME_NULL) {
                    commit.run();
                    hideKeyboard(et);
                    et.clearFocus();
                    return true;
                }
                return false;
            }
        });
        et.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override public void onFocusChange(View view, boolean has) {
                if (!has) commit.run();
            }
        });
        row.addView(et, wrapWrap());

        TextView unitTv = text(unit, 11.5f, C_SUB, false, 0);
        unitTv.setPadding(dp(4), 0, 0, 0);
        row.addView(unitTv, wrapWrap());

        row.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                et.requestFocus();
                et.setSelection(et.getText().length());
                showKeyboard(et);
            }
        });
        return row;
    }

    private String tierDemo() {
        int t1 = AiConfig.affinityTier1(), t2 = AiConfig.affinityTier2();
        int t3 = AiConfig.affinityTier3(), t4 = AiConfig.affinityTier4();
        return "0~" + (t1 - 1) + " 冷淡 · " + t1 + "~" + (t2 - 1) + " 客气 · "
                + t2 + "~" + (t3 - 1) + " 普通 · " + t3 + "~" + (t4 - 1) + " 熟络 · "
                + t4 + "~100 亲近";
    }

    private void askClear() {
        CatSheet.confirm(ctx, "清空全部好感度档案？",
                "所有人和分数都会没，且不能恢复。", "清空", true, new Runnable() {
                    @Override public void run() {
                        AiAffinity.clearAll();
                        toast("已清空");
                        render();
                    }
                });
    }
}
