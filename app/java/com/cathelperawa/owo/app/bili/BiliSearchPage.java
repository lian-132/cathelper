package com.cathelperawa.owo.app.bili;

import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.cathelperawa.owo.app.ui.CatIcon;

import java.util.ArrayList;
import java.util.List;

public final class BiliSearchPage extends BiliPage {

    private EditText input;
    private LinearLayout resultBox;
    private ScrollView scroll;
    private TextView hint;
    private boolean loading = false;
    private int page = 1;
    private String keyword = "";
    private boolean showingSuggest = false;

    private final Handler debounce = new Handler(Looper.getMainLooper());
    private Runnable suggestTask;

    public BiliSearchPage(BiliActivity host) {
        super(host);
    }

    @Override
    public View onCreate() {
        LinearLayout root = new LinearLayout(host);
        root.setOrientation(LinearLayout.VERTICAL);

        LinearLayout top = new LinearLayout(host);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(BiliUi.dp(host, 12), BiliUi.dp(host, 8),
                BiliUi.dp(host, 12), BiliUi.dp(host, 8));

        CatIcon back = BiliUi.icon(host, CatIcon.BACK, 20, BiliUi.MAIN);
        top.addView(back);
        back.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { host.onBackPressed(); }
        });

        LinearLayout box = BiliUi.pill(host, 0x33FFFFFF, 0x38FFFFFF, 18);
        box.setPadding(BiliUi.dp(host, 12), 0, BiliUi.dp(host, 12), 0);
        LinearLayout.LayoutParams boxLp = new LinearLayout.LayoutParams(0,
                BiliUi.dp(host, 36), 1f);
        boxLp.leftMargin = BiliUi.dp(host, 10);
        boxLp.rightMargin = BiliUi.dp(host, 10);
        top.addView(box, boxLp);
        box.addView(BiliUi.icon(host, CatIcon.SEARCH, 15, BiliUi.SUB));

        input = new EditText(host);
        input.setSingleLine(true);
        input.setHint("搜视频 / UP 主");
        input.setHintTextColor(BiliUi.SUB);
        input.setTextColor(BiliUi.MAIN);
        input.setTextSize(13f);
        input.setBackground(null);
        input.setPadding(BiliUi.dp(host, 8), 0, 0, 0);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        box.addView(input, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.MATCH_PARENT, 1f));
        input.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override public boolean onEditorAction(TextView v, int actionId, android.view.KeyEvent e) {
                doSearch(input.getText().toString().trim());
                return true;
            }
        });
        input.requestFocus();

        TextView go = BiliUi.text(host, "搜索", 12.5f, BiliUi.ACCENT, true);
        top.addView(go);
        go.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                doSearch(input.getText().toString().trim());
            }
        });
        root.addView(top);
        root.addView(BiliUi.line(host, 0.08f));

        scroll = new ScrollView(host);
        scroll.setFillViewport(true);
        resultBox = new LinearLayout(host);
        resultBox.setOrientation(LinearLayout.VERTICAL);
        resultBox.setPadding(BiliUi.dp(host, 14), BiliUi.dp(host, 12),
                BiliUi.dp(host, 14), BiliUi.dp(host, 24));
        scroll.addView(resultBox, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        scroll.setOnScrollChangeListener(new View.OnScrollChangeListener() {
            @Override public void onScrollChange(View v, int sx, int sy, int osx, int osy) {
                if (showingSuggest) return;
                View child = scroll.getChildAt(0);
                if (child == null || hint != null) return;
                int remain = child.getBottom() - (scroll.getHeight() + sy);
                if (remain < BiliUi.dp(host, 320)) load(false);
            }
        });

        input.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }

            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }

            @Override public void afterTextChanged(Editable e) {
                final String term = e.toString().trim();
                if (suggestTask != null) debounce.removeCallbacks(suggestTask);
                if (term.length() == 0) {
                    showHot();
                    return;
                }
                suggestTask = new Runnable() {
                    @Override public void run() { loadSuggest(term); }
                };
                debounce.postDelayed(suggestTask, 300);
            }
        });

        showHot();
        return root;
    }

    private void showHot() {
        showingSuggest = false;
        resultBox.removeAllViews();
        hint = BiliCards.loading(host, "加载热搜…");
        resultBox.addView(hint);
        BiliThread.io(new Runnable() {
            @Override public void run() {
                final List<String> words = BiliApiExt.hotwords(host);
                BiliThread.ui(new Runnable() {
                    @Override public void run() {
                        if (hint != null && hint.getParent() != null) {
                            ((ViewGroup) hint.getParent()).removeView(hint);
                        }
                        hint = null;
                        resultBox.removeAllViews();
                        if (words.isEmpty()) {
                            resultBox.addView(BiliCards.loading(host, "输入关键词，回车搜索"));
                            return;
                        }
                        resultBox.addView(BiliUi.text(host, "大家都在搜", 11.5f, BiliUi.SUB, false));
                        resultBox.addView(buildChips(words));
                    }
                });
            }
        });
    }

    private View buildChips(List<String> words) {
        LinearLayout wrap = new LinearLayout(host);
        wrap.setOrientation(LinearLayout.VERTICAL);
        wrap.setPadding(0, BiliUi.dp(host, 10), 0, 0);

        LinearLayout row = null;
        int perRow = 2;
        for (int i = 0; i < words.size(); i++) {
            if (i % perRow == 0) {
                row = new LinearLayout(host);
                row.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                rlp.bottomMargin = BiliUi.dp(host, 8);
                wrap.addView(row, rlp);
            }
            final String w = words.get(i);
            LinearLayout chip = BiliUi.pill(host, 0x1FFFFFFF, 0x24FFFFFF, 10);
            chip.setPadding(BiliUi.dp(host, 11), BiliUi.dp(host, 7),
                    BiliUi.dp(host, 11), BiliUi.dp(host, 7));
            TextView t = BiliUi.text(host, w, 11.5f, BiliUi.CARD_SUB, false);
            t.setSingleLine(true);
            t.setEllipsize(android.text.TextUtils.TruncateAt.END);
            chip.addView(t, new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            clp.rightMargin = BiliUi.dp(host, 8);
            row.addView(chip, clp);
            chip.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    input.setText(w);
                    input.setSelection(w.length());
                    doSearch(w);
                }
            });
        }
        return wrap;
    }

    private void loadSuggest(final String term) {
        showingSuggest = true;
        BiliThread.io(new Runnable() {
            @Override public void run() {
                final List<String> list = BiliApiExt.suggest(host, term);
                BiliThread.ui(new Runnable() {
                    @Override public void run() {
                        if (!input.getText().toString().trim().equals(term)) return;
                        renderSuggest(list, term);
                    }
                });
            }
        });
    }

    private void renderSuggest(List<String> list, String term) {
        resultBox.removeAllViews();
        hint = null;
        if (list.isEmpty()) {
            resultBox.addView(BiliCards.loading(host, "回车搜索「" + term + "」"));
            return;
        }
        for (int i = 0; i < list.size(); i++) {
            final String w = list.get(i);
            LinearLayout row = new LinearLayout(host);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(BiliUi.dp(host, 4), BiliUi.dp(host, 11),
                    BiliUi.dp(host, 4), BiliUi.dp(host, 11));
            row.addView(BiliUi.icon(host, CatIcon.SEARCH, 13, BiliUi.SUB));
            TextView t = BiliUi.text(host, w, 12.5f, BiliUi.CARD_SUB, false);
            LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            tlp.leftMargin = BiliUi.dp(host, 10);
            row.addView(t, tlp);
            row.addView(BiliUi.icon(host, CatIcon.CHEVRON, 12, 0x669AA9BC));
            row.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    input.setText(w);
                    input.setSelection(w.length());
                    doSearch(w);
                }
            });
            resultBox.addView(row);
        }
    }

    private void doSearch(String kw) {
        if (kw == null || kw.length() == 0) {
            Toast.makeText(host, "先输个词", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!kw.equals(input.getText().toString().trim())) {
            input.setText(kw);
        }
        keyword = kw;
        page = 1;
        loading = false;
        showingSuggest = false;
        hint = null;
        resultBox.removeAllViews();
        hideKeyboard();
        load(true);
    }

    private void hideKeyboard() {
        try {
            android.view.inputmethod.InputMethodManager imm =
                    (android.view.inputmethod.InputMethodManager) host.getSystemService(
                            android.content.Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.hideSoftInputFromWindow(input.getWindowToken(), 0);
        } catch (Throwable ignore) {
        }
    }

    private void load(final boolean refresh) {
        if (loading) return;
        if (keyword.length() == 0) return;
        loading = true;
        final int p = refresh ? 1 : page;
        if (refresh) {
            hint = BiliCards.loading(host, "搜索中…");
            resultBox.addView(hint);
        }
        BiliThread.io(new Runnable() {
            @Override public void run() {
                final List<BiliApi.Item> list = BiliApi.search(host, keyword, p);
                BiliThread.ui(new Runnable() {
                    @Override public void run() {
                        loading = false;
                        if (hint != null && hint.getParent() != null) {
                            ((ViewGroup) hint.getParent()).removeView(hint);
                        }
                        hint = null;
                        if (list == null || list.isEmpty()) {
                            if (resultBox.getChildCount() == 0) {
                                resultBox.addView(BiliCards.loading(host,
                                        "没搜到 · 可能被风控了，稍后再试"));
                            }
                            return;
                        }
                        for (int i = 0; i < list.size(); i++) {
                            final BiliApi.Item it = list.get(i);
                            View row = BiliCards.wideCard(host, it, new View.OnClickListener() {
                                @Override public void onClick(View v) {
                                    host.push(new BiliVideoPage(host, it));
                                }
                            });
                            LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.WRAP_CONTENT);
                            rlp.bottomMargin = BiliUi.dp(host, 10);
                            resultBox.addView(row, rlp);
                        }
                        page = p + 1;
                    }
                });
            }
        });
    }
}
