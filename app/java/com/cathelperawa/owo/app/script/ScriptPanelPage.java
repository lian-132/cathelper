package com.cathelperawa.owo.app.script;

import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.cathelperawa.owo.app.ui.Page;
import com.cathelperawa.owo.app.ui.SwitchBox;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ScriptPanelPage extends Page {

    private static final int C_MAIN = 0xFF3C4B60;
    private static final int C_SUB = 0xFF8A9BB0;

    private final ScriptPanelRegistry.Panel panel;
    private LinearLayout body;
    private LinearLayout content;

    private final Map<String, String> values = new LinkedHashMap<String, String>();
    
    private final Map<String, View> views = new LinkedHashMap<String, View>();

    public ScriptPanelPage(Host host, ScriptPanelRegistry.Panel p) {
        super(host);
        this.panel = p;
    }

    @Override public View build() {
        View[] out = new View[2];
        body = pageRoot(panel.title, "", out, true, 13, "bg_script", 0xC8);
        content = new LinearLayout(ctx);
        content.setOrientation(LinearLayout.VERTICAL);
        body.addView(content, matchWrap(0));
        render();
        return out[0];
    }

    private void render() {
        content.removeAllViews();
        values.clear();
        views.clear();
        try {
            JSONArray arr = new JSONArray(panel.spec);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o == null) continue;
                addComponent(o);
            }
        } catch (Throwable t) {
            TextView e = text("界面描述解析失败：" + t.getMessage(), 12, 0xFFD9534F, false, 0);
            e.setPadding(dp(8), dp(12), dp(8), dp(12));
            content.addView(e, matchWrap(0));
        }
    }

    private void addComponent(JSONObject o) {
        String type = o.optString("type", "");
        String id = o.optString("id", "");

        if ("title".equals(type)) {
            TextView t = text(o.optString("text", ""), 16, C_MAIN, true, 0);
            t.setPadding(dp(4), dp(14), dp(4), dp(6));
            content.addView(t, matchWrap(0));
            if (id.length() > 0) views.put(id, t);

        } else if ("text".equals(type)) {
            TextView t = text(o.optString("text", ""), 12, C_SUB, false, 0);
            t.setPadding(dp(4), dp(4), dp(4), dp(8));
            content.addView(t, matchWrap(0));
            if (id.length() > 0) views.put(id, t);

        } else if ("divider".equals(type)) {
            View line = new View(ctx);
            line.setBackgroundColor(0x0D000000);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(1));
            lp.topMargin = dp(10);
            lp.bottomMargin = dp(10);
            content.addView(line, lp);

        } else if ("card".equals(type) || "row".equals(type)) {
            LinearLayout box = new LinearLayout(ctx);
            box.setOrientation("row".equals(type) ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);
            box.setGravity("row".equals(type) ? Gravity.CENTER_VERTICAL : Gravity.NO_GRAVITY);
            if ("card".equals(type)) {
                box.setBackground(whiteCard(dp(16), 0xF2FFFFFF).getBackground());
                box.setPadding(dp(14), dp(8), dp(14), dp(8));
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                lp.topMargin = dp(8);
                content.addView(box, lp);
            } else {
                content.addView(box, matchWrap(dp(4)));
            }
            JSONArray kids = o.optJSONArray("children");
            if (kids != null) {
                for (int i = 0; i < kids.length(); i++) {
                    JSONObject k = kids.optJSONObject(i);
                    if (k != null) addComponentInto(k, box);
                }
            }

        } else if ("input".equals(type)) {
            addInput(o, id);

        } else if ("switch".equals(type)) {
            addSwitch(o, id);

        } else if ("button".equals(type)) {
            addButton(o, id);

        } else if ("card".equals(type) || "row".equals(type)) {
            
            LinearLayout box = new LinearLayout(ctx);
            box.setOrientation("row".equals(type) ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);
            box.setGravity("row".equals(type) ? Gravity.CENTER_VERTICAL : Gravity.NO_GRAVITY);
            if ("card".equals(type)) {
                GradientDrawable bg = new GradientDrawable();
                bg.setColor(0xFFFFFFFF);
                bg.setCornerRadius(dp(14));
                box.setBackground(bg);
                box.setPadding(dp(14), dp(10), dp(14), dp(10));
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                lp.topMargin = dp(8);
                content.addView(box, lp);
            } else {
                content.addView(box, matchWrap(dp(6)));
            }
            JSONArray kids = o.optJSONArray("children");
            if (kids != null) {
                for (int i = 0; i < kids.length(); i++) {
                    JSONObject k = kids.optJSONObject(i);
                    if (k != null) addNested(k, box);
                }
            }
        }
    }

    private void addNested(JSONObject o, LinearLayout parent) {
        String type = o.optString("type", "");
        String id = o.optString("id", "");

        if ("text".equals(type)) {
            TextView t = text(o.optString("text", ""), o.optInt("size", 12),
                    parseColor(o.optString("color", "#8A9BB0"), 0xFF8A9BB0), false, 0);
            t.setPadding(0, dp(4), 0, dp(6));
            parent.addView(t, matchWrap(0));
            if (id.length() > 0) views.put(id, t);
        } else if ("title".equals(type)) {
            TextView t = text(o.optString("text", ""), 14, C_MAIN, true, 0);
            t.setPadding(0, dp(6), 0, dp(6));
            parent.addView(t, matchWrap(0));
            if (id.length() > 0) views.put(id, t);
        } else if ("divider".equals(type)) {
            View line = new View(ctx);
            line.setBackgroundColor(0x0D000000);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(1));
            lp.topMargin = dp(6);
            lp.bottomMargin = dp(6);
            parent.addView(line, lp);
        } else if ("button".equals(type)) {
            addButtonInto(o, id, parent);
        } else if ("switch".equals(type)) {
            addSwitch(o, id);
        }
    }

    private static int parseColor(String v, int def) {
        try {
            if (v == null || v.length() == 0) return def;
            String s2 = v.trim();
            if (s2.startsWith("#")) {
                String h = s2.substring(1);
                if (h.length() == 6) h = "FF" + h;
                if (h.length() == 8) return (int) Long.parseLong(h, 16);
            }
            return android.graphics.Color.parseColor(s2);
        } catch (Throwable t) {
            return def;
        }
    }

    private void addComponentInto(JSONObject o, LinearLayout parent) {
        String type = o.optString("type", "");
        String id = o.optString("id", "");
        if ("title".equals(type)) {
            TextView t = text(o.optString("text", ""), 15, C_MAIN, true, 0);
            t.setPadding(0, dp(8), 0, dp(6));
            parent.addView(t, matchWrap(0));
            if (id.length() > 0) views.put(id, t);
        } else if ("text".equals(type)) {
            TextView t = text(o.optString("text", ""), 12, C_SUB, false, 0);
            t.setPadding(0, dp(4), 0, dp(8));
            parent.addView(t, matchWrap(0));
            if (id.length() > 0) views.put(id, t);
        } else if ("divider".equals(type)) {
            View line = new View(ctx);
            line.setBackgroundColor(0x0D000000);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(1));
            lp.topMargin = dp(6);
            lp.bottomMargin = dp(6);
            parent.addView(line, lp);
        } else if ("switch".equals(type)) {
            addSwitchInto(o, id, parent);
        } else if ("input".equals(type)) {
            addInputInto(o, id, parent);
        } else if ("button".equals(type)) {
            addButtonInto(o, id, parent);
        }
    }

    private void addSwitchInto(JSONObject o, final String id, LinearLayout parent) {
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(8), 0, dp(8));

        TextView label = text(o.optString("label", id), 14, 0xFF5A6B80, false, 0);
        row.addView(label, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        final boolean init = o.optBoolean("value", false);
        values.put(id, init ? "1" : "0");
        final SwitchBox box = new SwitchBox(ctx, init);
        row.addView(box, new LinearLayout.LayoutParams(dp(46), dp(26)));
        box.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                boolean z = !"1".equals(values.get(id));
                values.put(id, z ? "1" : "0");
                box.setChecked(z);
                press(v);
                fire(id);
            }
        });
        parent.addView(row, matchWrap(0));
        views.put(id, box);
    }

    private void addInputInto(JSONObject o, final String id, LinearLayout parent) {
        final EditText et = new EditText(ctx);
        et.setHint(o.optString("hint", ""));
        et.setText(o.optString("value", ""));
        et.setTextSize(14);
        et.setTextColor(0xFF3C4B60);
        et.setHintTextColor(0xFFB0BCC9);
        et.setBackground(null);
        et.setSingleLine(true);
        et.setPadding(0, dp(6), 0, dp(6));
        values.put(id, o.optString("value", ""));
        et.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable e) {
                values.put(id, e == null ? "" : e.toString());
            }
        });
        parent.addView(et, matchWrap(0));
        views.put(id, et);
    }

    private void addButtonInto(JSONObject o, final String id, LinearLayout parent) {
        TextView btn = text(o.optString("text", "按钮"), 14,
                parseColor(o.optString("textColor", "#3C4B60"), 0xFF3C4B60), true, 0);
        btn.setGravity(Gravity.CENTER);
        btn.setPadding(0, dp(11), 0, dp(11));
        GradientDrawable g = new GradientDrawable();
        g.setColor(parseColor(o.optString("bg", "#EDF1F7"), 0xFFEDF1F7));
        g.setCornerRadius(dp(10));
        btn.setBackground(g);
        press(btn);
        final String action = o.optString("action", "");
        btn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                handleAction(action, id);
            }
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(6);
        parent.addView(btn, lp);
        if (id.length() > 0) views.put(id, btn);
    }

    private void handleAction(String action, String btnId) {
        if (action == null || action.length() == 0) { fire(btnId); return; }
        if (action.startsWith("close")) {
            host.close();
        } else if (action.startsWith("open:")) {
            String target = action.substring(5).trim();
            ScriptPanelRegistry.Panel pn = null;
            java.util.List<ScriptPanelRegistry.Panel> all = ScriptPanelRegistry.ofScript(panel.script);
            for (int i = 0; i < all.size(); i++) {
                if (all.get(i).title.equals(target)) { pn = all.get(i); break; }
            }
            if (pn != null) host.pushPage(new ScriptPanelPage(host, pn).build());
        } else if (action.startsWith("run:")) {
            fire(action.substring(4).trim());
        } else {
            fire(btnId);
        }
    }

    private void addInput(JSONObject o, final String id) {
        LinearLayout card = whiteCard(dp(14), 0xF2FFFFFF);
        card.setPadding(dp(14), dp(10), dp(14), dp(10));

        final EditText et = new EditText(ctx);
        et.setHint(o.optString("hint", ""));
        et.setText(o.optString("value", ""));
        et.setTextSize(14);
        et.setTextColor(C_MAIN);
        et.setHintTextColor(0xFFB0BCC9);
        et.setBackground(null);
        et.setSingleLine(true);
        et.setPadding(0, dp(4), 0, dp(4));
        values.put(id, o.optString("value", ""));
        et.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable e) {
                values.put(id, e == null ? "" : e.toString());
            }
        });
        card.addView(et, matchWrap(0));
        content.addView(card, matchWrap(dp(8)));
        views.put(id, et);
    }

    private void addSwitch(JSONObject o, final String id) {
        LinearLayout card = whiteCard(dp(14), 0xF2FFFFFF);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(14), dp(10), dp(12), dp(10));

        TextView label = text(o.optString("label", id), 14, C_MAIN, true, 0);
        card.addView(label, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        final boolean init = o.optBoolean("value", false);
        values.put(id, init ? "1" : "0");

        final SwitchBox box = new SwitchBox(ctx, init);
        card.addView(box, new LinearLayout.LayoutParams(dp(48), dp(28)));
        box.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                boolean z = !"1".equals(values.get(id));
                values.put(id, z ? "1" : "0");
                box.setChecked(z);
                press(v);
                fire(id);
            }
        });
        content.addView(card, matchWrap(dp(8)));
        views.put(id, box);
    }

    private void addButton(JSONObject o, final String id) {
        addButtonInto(o, id, content);
    }

    private void fire(String id) {
        final String payload = buildJson();
        final ScriptPanelResult out = new ScriptPanelResult();
        ScriptPanelRegistry.fire(panel, id, payload, out);

        content.postDelayed(new Runnable() {
            @Override public void run() {
                applyResult(out);
            }
        }, 120);
    }

    private String buildJson() {
        try {
            JSONObject o = new JSONObject();
            for (Map.Entry<String, String> e : values.entrySet()) {
                o.put(e.getKey(), e.getValue());
            }
            return o.toString();
        } catch (Throwable t) {
            return "{}";
        }
    }

    private void applyResult(ScriptPanelResult out) {
        try {
            for (Map.Entry<String, String> e : out.updates().entrySet()) {
                View v = views.get(e.getKey());
                if (v == null) continue;
                if (v instanceof EditText) ((EditText) v).setText(e.getValue());
                else if (v instanceof TextView) ((TextView) v).setText(e.getValue());
                values.put(e.getKey(), e.getValue());
            }
            if (out.toastText() != null) toast(out.toastText());
        } catch (Throwable ignore) {
        }
    }
}
