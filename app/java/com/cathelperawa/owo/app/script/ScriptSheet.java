package com.cathelperawa.owo.app.script;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ScrollView;
import android.widget.TextView;

import com.cathelperawa.owo.app.core.HookLog;
import com.cathelperawa.owo.app.ui.SwitchBox;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ScriptSheet {

    private static PopupWindow window;

    private ScriptSheet() {}

    public static void show(final Activity act, final String title,
                            final String spec, final ScriptPanelRegistry.Panel panel) {
        try {
            close();
            if (act == null || spec == null) return;

            float d = act.getResources().getDisplayMetrics().density;

            FrameLayout root = new FrameLayout(act);
            root.setBackgroundColor(0xA6000000);
            root.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { close(); }
            });

            LinearLayout sheet = new LinearLayout(act);
            sheet.setOrientation(LinearLayout.VERTICAL);
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(0xFFF4F6F9);
            float r = 18 * d;
            bg.setCornerRadii(new float[]{r, r, r, r, 0, 0, 0, 0});
            sheet.setBackground(bg);
            sheet.setPadding(0, (int) (10 * d), 0, (int) (16 * d));
            sheet.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {   }
            });

            View bar = new View(act);
            GradientDrawable barBg = new GradientDrawable();
            barBg.setColor(0xFFD8DEE7);
            barBg.setCornerRadius(3 * d);
            bar.setBackground(barBg);
            LinearLayout.LayoutParams barLp = new LinearLayout.LayoutParams(
                    (int) (36 * d), (int) (4 * d));
            barLp.gravity = Gravity.CENTER_HORIZONTAL;
            barLp.topMargin = (int) (2 * d);
            sheet.addView(bar, barLp);

            if (title != null && title.length() > 0) {
                TextView t = new TextView(act);
                t.setText(title);
                t.setTextSize(17);
                t.setTextColor(0xFF2C3A4D);
                t.setGravity(Gravity.CENTER);
                t.setPadding(0, (int) (14 * d), 0, (int) (10 * d));
                sheet.addView(t);
            }

            ScrollView sv = new ScrollView(act);
            sv.setVerticalScrollBarEnabled(false);
            LinearLayout content = new LinearLayout(act);
            content.setOrientation(LinearLayout.VERTICAL);
            content.setPadding((int) (14 * d), 0, (int) (14 * d), 0);
            sv.addView(content, new ScrollView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));

            LinearLayout.LayoutParams svLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
            sheet.addView(sv, svLp);

            final Map<String, String> values = new LinkedHashMap<String, String>();
            final Map<String, View> views = new LinkedHashMap<String, View>();

            renderChildren(act, spec, content, values, views, panel, d);

            FrameLayout.LayoutParams sLp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    (int) (act.getResources().getDisplayMetrics().heightPixels * 0.62f));
            sLp.gravity = Gravity.BOTTOM;
            root.addView(sheet, sLp);

            window = new PopupWindow(root, ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT, true);
            window.setBackgroundDrawable(new ColorDrawable(0x00000000));
            window.setOutsideTouchable(true);
            window.setClippingEnabled(false);
            View anchor = act.findViewById(android.R.id.content);
            window.showAtLocation(anchor == null ? root : anchor, Gravity.BOTTOM, 0, 0);
        } catch (Throwable t) {
            HookLog.e("[脚本抽屉] 弹出失败", t);
        }
    }

    public static void close() {
        try {
            if (window != null) window.dismiss();
        } catch (Throwable ignore) {
        }
        window = null;
    }

    private static void renderChildren(Activity act, String spec, LinearLayout parent,
                                       Map<String, String> values, Map<String, View> views,
                                       ScriptPanelRegistry.Panel panel, float d) {
        try {
            JSONArray arr = new JSONArray(spec);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o == null) continue;
                addOne(act, o, parent, values, views, panel, d);
            }
        } catch (Throwable t) {
            TextView e = new TextView(act);
            e.setText("界面描述解析失败：" + t.getMessage());
            e.setTextSize(12);
            e.setTextColor(0xFFD9534F);
            parent.addView(e);
        }
    }

    private static void addOne(Activity act, JSONObject o, LinearLayout parent,
                               Map<String, String> values, Map<String, View> views,
                               ScriptPanelRegistry.Panel panel, float d) {
        String type = o.optString("type", "");
        String id = o.optString("id", "");

        if ("title".equals(type)) {
            TextView t = new TextView(act);
            t.setText(o.optString("text", ""));
            t.setTextSize(16);
            t.setTextColor(parseColor(o.optString("color", "#2C3A4D"), 0xFF2C3A4D));
            t.setPadding(0, (int) (14 * d), 0, (int) (8 * d));
            parent.addView(t);
            if (id.length() > 0) views.put(id, t);

        } else if ("text".equals(type)) {
            TextView t = new TextView(act);
            t.setText(o.optString("text", ""));
            t.setTextSize(o.optInt("size", 12));
            t.setTextColor(parseColor(o.optString("color", "#8A9BB0"), 0xFF8A9BB0));
            t.setPadding(0, (int) (4 * d), 0, (int) (10 * d));
            parent.addView(t);
            if (id.length() > 0) views.put(id, t);

        } else if ("divider".equals(type)) {
            View line = new View(act);
            line.setBackgroundColor(0x0D000000);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, (int) (1 * d));
            lp.topMargin = (int) (8 * d);
            lp.bottomMargin = (int) (8 * d);
            parent.addView(line, lp);

        } else if ("card".equals(type) || "row".equals(type)) {
            LinearLayout box = new LinearLayout(act);
            box.setOrientation("row".equals(type) ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);
            box.setGravity("row".equals(type) ? Gravity.CENTER_VERTICAL : Gravity.NO_GRAVITY);
            if ("card".equals(type)) {
                GradientDrawable bg = new GradientDrawable();
                bg.setColor(0xFFFFFFFF);
                bg.setCornerRadius(14 * d);
                box.setBackground(bg);
                box.setPadding((int) (14 * d), (int) (10 * d), (int) (14 * d), (int) (10 * d));
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
                lp.topMargin = (int) (8 * d);
                parent.addView(box, lp);
            } else {
                parent.addView(box, new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));
            }
            JSONArray kids = o.optJSONArray("children");
            if (kids != null) {
                for (int i = 0; i < kids.length(); i++) {
                    JSONObject k = kids.optJSONObject(i);
                    if (k != null) addOne(act, k, box, values, views, panel, d);
                }
            }

        } else if ("switch".equals(type)) {
            LinearLayout card = new LinearLayout(act);
            card.setOrientation(LinearLayout.HORIZONTAL);
            card.setGravity(Gravity.CENTER_VERTICAL);
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(0xFFFFFFFF);
            bg.setCornerRadius(14 * d);
            card.setBackground(bg);
            card.setPadding((int) (16 * d), (int) (12 * d), (int) (14 * d), (int) (12 * d));

            TextView label = new TextView(act);
            label.setText(o.optString("label", id));
            label.setTextSize(15);
            label.setTextColor(0xFF2C3A4D);
            card.addView(label, new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            final boolean init = o.optBoolean("value", false);
            values.put(id, init ? "1" : "0");
            final SwitchBox box = new SwitchBox(act, init);
            card.addView(box, new LinearLayout.LayoutParams((int) (50 * d), (int) (30 * d)));
            final String sid = id;
            box.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    boolean z = !"1".equals(values.get(sid));
                    values.put(sid, z ? "1" : "0");
                    box.setChecked(z);
                    fire(panel, sid, values, views);
                }
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.topMargin = (int) (8 * d);
            parent.addView(card, lp);
            views.put(id, box);

        } else if ("input".equals(type)) {
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(0xFFFFFFFF);
            bg.setCornerRadius(14 * d);
            final EditText et = new EditText(act);
            et.setBackground(bg);
            et.setHint(o.optString("hint", ""));
            et.setText(o.optString("value", ""));
            et.setTextSize(15);
            et.setTextColor(0xFF2C3A4D);
            et.setHintTextColor(0xFFB0BCC9);
            et.setSingleLine(true);
            et.setPadding((int) (16 * d), (int) (12 * d), (int) (16 * d), (int) (12 * d));
            values.put(id, o.optString("value", ""));
            final String iid = id;
            et.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
                @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
                @Override public void afterTextChanged(Editable e) {
                    values.put(iid, e == null ? "" : e.toString());
                }
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.topMargin = (int) (8 * d);
            parent.addView(et, lp);
            views.put(id, et);

        } else if ("button".equals(type)) {
            TextView btn = new TextView(act);
            btn.setText(o.optString("text", "按钮"));
            btn.setTextSize(15);
            btn.setTextColor(parseColor(o.optString("textColor", "#3C4B60"), 0xFF3C4B60));
            btn.setGravity(Gravity.CENTER);
            btn.setPadding(0, (int) (14 * d), 0, (int) (14 * d));

            String bgHex = o.optString("bg", "#FFFFFF");
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(parseColor(bgHex, 0xFFFFFFFF));
            bg.setCornerRadius(14 * d);
            btn.setBackground(bg);

            final String action = o.optString("action", "");
            final String btnId = o.optString("id", "");
            btn.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    handleAction(action, btnId, values, views, panel);
                }
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.topMargin = (int) (8 * d);
            parent.addView(btn, lp);
            if (id.length() > 0) views.put(id, btn);
        }
    }

    private static void handleAction(String action, String btnId,
                                     Map<String, String> values, Map<String, View> views,
                                     ScriptPanelRegistry.Panel panel) {
        try {
            if (action == null || action.length() == 0) {

                fire(panel, btnId, values, views);
                return;
            }
            if (action.startsWith("close")) {
                close();
            } else if (action.startsWith("open:")) {
                String target = action.substring(5).trim();
                ScriptPanelRegistry.Panel pn = null;
                if (panel != null) {
                    java.util.List<ScriptPanelRegistry.Panel> all =
                            ScriptPanelRegistry.ofScript(panel.script);
                    for (int i = 0; i < all.size(); i++) {
                        if (all.get(i).title.equals(target)) { pn = all.get(i); break; }
                    }
                }
                if (pn != null) {
                    close();
                    Activity act = com.cathelperawa.owo.app.bridge.QQEnv.topActivity();
                    if (act != null) {
                        show(act, pn.title, pn.spec, pn);
                    }
                } else {
                    HookLog.w("[脚本抽屉] 找不到面板：" + target);
                }
            } else if (action.startsWith("send:")) {
                String text = action.substring(5);
                close();
                sendToCurrentChat(text);
            } else if (action.startsWith("run:")) {
                fire(panel, action.substring(4).trim(), values, views);
            } else {
                fire(panel, btnId, values, views);
            }
        } catch (Throwable t) {
            HookLog.w("[脚本抽屉] action 失败 " + t);
        }
    }

    private static void fire(ScriptPanelRegistry.Panel panel, String id,
                             Map<String, String> values, Map<String, View> views) {
        if (panel == null) return;
        String payload = buildJson(values);
        ScriptPanelResult out = new ScriptPanelResult();

        ScriptPanelRegistry.fire(panel, id, payload, out);
        applyResult(out, views, values);
    }

    private static void applyResult(ScriptPanelResult out, Map<String, View> views,
                                    Map<String, String> values) {
        try {
            for (Map.Entry<String, String> e : out.updates().entrySet()) {
                View v = views.get(e.getKey());
                if (v == null) continue;
                if (v instanceof EditText) ((EditText) v).setText(e.getValue());
                else if (v instanceof TextView) ((TextView) v).setText(e.getValue());
                values.put(e.getKey(), e.getValue());
            }
        } catch (Throwable ignore) {
        }
    }

    private static String buildJson(Map<String, String> values) {
        try {
            JSONObject o = new JSONObject();
            for (Map.Entry<String, String> e : values.entrySet()) o.put(e.getKey(), e.getValue());
            return o.toString();
        } catch (Throwable t) {
            return "{}";
        }
    }

    private static void sendToCurrentChat(String text) {
        try {
            String peer = com.cathelperawa.owo.app.bridge.CurrentChat.peerUid();
            boolean group = com.cathelperawa.owo.app.bridge.CurrentChat.chatType() == 2;
            if (peer != null && peer.length() > 0) {
                com.cathelperawa.owo.app.api.MsgApi.sendText(group ? 2 : 1, peer, text);
            }
        } catch (Throwable ignore) {
        }
    }

    private static int parseColor(String v, int def) {
        try {
            if (v == null || v.length() == 0) return def;
            String s = v.trim();
            if (s.startsWith("#")) {
                String h = s.substring(1);
                if (h.length() == 6) h = "FF" + h;
                if (h.length() == 8) return (int) Long.parseLong(h, 16);
            }
            return Color.parseColor(s);
        } catch (Throwable t) {
            return def;
        }
    }
}
