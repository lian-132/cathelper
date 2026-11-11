package com.cathelperawa.owo.app.ui;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public final class CatSheet {

    private CatSheet() {
    }

    private static final int C_TITLE = 0xFF3C4E6B;
    private static final int C_SUB = 0xFF8A9CB5;
    private static final int C_PINK = 0xFFF07AAE;

    public interface OnPick {
        void run(int index, String label);
    }

    public interface OnText {
        void run(String text);
    }

    

    
    
    private static Object fieldOf(Object o, String name) {
        if (o == null) return null;
        
        Class<?> c = o.getClass();
        while (c != null && c != Object.class) {
            try {
                java.lang.reflect.Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                return f.get(o);
            } catch (java.lang.NoSuchFieldException e) {
                c = c.getSuperclass();
            } catch (Throwable t) {
                return null;
            }
        }
        return null;
    }

    public static void pick(final Context anyCtx, final String title,
                            final String[] items, final OnPick cb) {
        pick(anyCtx, title, items, cb, null);
    }

    public static void pick(final Context anyCtx, final String title,
                            final String[] items, final OnPick cb, final OnPick onLong) {
        final Activity act = actOf(anyCtx);
        if (act == null) return;
        act.runOnUiThread(new Runnable() {
            @Override public void run() {
                try {
                    final Dialog dlg = base(act);
                    final float d = act.getResources().getDisplayMetrics().density;
                    LinearLayout card = card(act, d);

                    card.addView(title(act, d, title), titleLp(d));

                    ScrollView sv = new ScrollView(act);
                    
                    try {
                        sv.setVerticalScrollBarEnabled(true);
                        sv.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
                        sv.setPadding(0, 0, (int) (4 * d), 0);
                        sv.setClipToPadding(false);
                        
                        boolean styled = false;
                        try {
                            Object cache = fieldOf(sv, "mScrollCache");
                            Object bar = cache == null ? null : fieldOf(cache, "scrollBar");
                            if (bar != null) {
                                java.lang.reflect.Method m = bar.getClass().getMethod(
                                        "setVerticalThumbDrawable",
                                        android.graphics.drawable.Drawable.class);
                                m.setAccessible(true);
                                m.invoke(bar, act.getResources().getDrawable(
                                        com.cathelperawa.owo.app.R.drawable.scroll_thumb));
                                styled = true;
                            }
                        } catch (Throwable ignore) {
                        }
                        if (!styled) {
                            
                            sv.setVerticalScrollBarEnabled(false);
                        }
                        com.cathelperawa.owo.app.core.HookLog.i(
                                "CatSheet 滚动条: custom=" + styled);
                    } catch (Throwable ignore) {
                    }
                    LinearLayout list = new LinearLayout(act);
                    list.setOrientation(LinearLayout.VERTICAL);
                    for (int i = 0; i < items.length; i++) {
                        final int idx = i;
                        TextView row = new TextView(act);
                        row.setText(items[i]);
                        row.setTextSize(14);
                        row.setTextColor(C_TITLE);
                        row.setPadding((int) (12 * d), (int) (11 * d),
                                (int) (12 * d), (int) (11 * d));
                        GradientDrawable rg = new GradientDrawable();
                        rg.setCornerRadius(12 * d);
                        rg.setColor(0x59FFFFFF);
                        row.setBackgroundDrawable(rg);
                        row.setOnClickListener(new View.OnClickListener() {
                            @Override public void onClick(View v) {
                                dlg.dismiss();
                                try {
                                    cb.run(idx, items[idx]);
                                } catch (Throwable ignore) {
                                }
                            }
                        });
                        if (onLong != null) {
                            row.setOnLongClickListener(new View.OnLongClickListener() {
                                @Override public boolean onLongClick(View v) {
                                    dlg.dismiss();
                                    try {
                                        onLong.run(idx, items[idx]);
                                    } catch (Throwable ignore) {
                                    }
                                    return true;
                                }
                            });
                        }
                        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT);
                        lp.bottomMargin = (int) (6 * d);
                        list.addView(row, lp);
                    }
                    sv.addView(list);
                    card.addView(sv, new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

                    card.addView(cancelRow(act, d, dlg), new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT));

                    show(act, dlg, card, 0.62f);
                } catch (Throwable ignore) {
                }
            }
        });
    }

    public static void pickWithActions(final Context anyCtx, final String title,
                                       final String[] items, final OnPick cb,
                                       final String[] actions, final OnPick actCb) {
        final Activity act = actOf(anyCtx);
        if (act == null) return;
        act.runOnUiThread(new Runnable() {
            @Override public void run() {
                try {
                    final Dialog dlg = base(act);
                    final float d = act.getResources().getDisplayMetrics().density;
                    LinearLayout card = card(act, d);

                    card.addView(title(act, d, title), titleLp(d));

                    ScrollView sv = new ScrollView(act);
                    
                    try {
                        sv.setVerticalScrollBarEnabled(true);
                        sv.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
                        sv.setPadding(0, 0, (int) (4 * d), 0);
                        sv.setClipToPadding(false);
                        
                        Object cache = fieldOf(sv, "mScrollCache");
                        Object bar = cache == null ? null : fieldOf(cache, "scrollBar");
                        if (bar != null) {
                            java.lang.reflect.Method m = bar.getClass().getMethod(
                                    "setVerticalThumbDrawable",
                                    android.graphics.drawable.Drawable.class);
                            m.setAccessible(true);
                            m.invoke(bar, act.getResources().getDrawable(
                                    com.cathelperawa.owo.app.R.drawable.scroll_thumb));
                        }
                    } catch (Throwable ignore) {
                    }
                    LinearLayout list = new LinearLayout(act);
                    list.setOrientation(LinearLayout.VERTICAL);
                    if (items.length == 0) {
                        TextView empty = new TextView(act);
                        empty.setText("还没有模板");
                        empty.setTextSize(13);
                        empty.setTextColor(C_SUB);
                        empty.setGravity(Gravity.CENTER);
                        empty.setPadding(0, (int) (18 * d), 0, (int) (18 * d));
                        list.addView(empty);
                    }
                    for (int i = 0; i < items.length; i++) {
                        final int idx = i;
                        TextView row = new TextView(act);
                        row.setText(items[i]);
                        row.setTextSize(14);
                        row.setTextColor(C_TITLE);
                        row.setPadding((int) (14 * d), (int) (12 * d),
                                (int) (14 * d), (int) (12 * d));
                        GradientDrawable rg = new GradientDrawable();
                        rg.setCornerRadius(12 * d);
                        rg.setColor(0x66FFFFFF);
                        rg.setStroke((int) d, 0x33FFFFFF);
                        row.setBackgroundDrawable(rg);
                        row.setOnClickListener(new View.OnClickListener() {
                            @Override public void onClick(View v) {
                                dlg.dismiss();
                                try {
                                    cb.run(idx, items[idx]);
                                } catch (Throwable ignore) {
                                }
                            }
                        });
                        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT);
                        lp.bottomMargin = (int) (6 * d);
                        list.addView(row, lp);
                    }
                    sv.addView(list);
                    card.addView(sv, new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

                    LinearLayout bar = new LinearLayout(act);
                    bar.setOrientation(LinearLayout.HORIZONTAL);
                    for (int i = 0; i < actions.length; i++) {
                        final int idx = i;
                        TextView btn = i == 0 ? pink(act, d, actions[i])
                                : ghost(act, d, actions[i]);
                        btn.setOnClickListener(new View.OnClickListener() {
                            @Override public void onClick(View v) {
                                dlg.dismiss();
                                try {
                                    actCb.run(idx, actions[idx]);
                                } catch (Throwable ignore) {
                                }
                            }
                        });
                        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
                                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                        if (i > 0) lp.leftMargin = (int) (6 * d);
                        if (i < actions.length - 1) lp.rightMargin = (int) (6 * d);
                        bar.addView(btn, lp);
                    }
                    LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT);
                    blp.topMargin = (int) (10 * d);
                    card.addView(bar, blp);

                    show(act, dlg, card, 0.62f);
                } catch (Throwable ignore) {
                }
            }
        });
    }

    

    
    public static void input(final Context anyCtx, final String title, final String hint,
                             final String value, final OnText cb) {
        final Activity act = actOf(anyCtx);
        if (act == null) return;
        act.runOnUiThread(new Runnable() {
            @Override public void run() {
                try {
                    final Dialog dlg = base(act);
                    final float d = act.getResources().getDisplayMetrics().density;
                    LinearLayout card = card(act, d);

                    card.addView(title(act, d, title), titleLp(d));

                    final EditText et = new EditText(act);
                    et.setText(value == null ? "" : value);
                    et.setHint(hint == null ? "" : hint);
                    et.setTextSize(13);
                    et.setTextColor(C_TITLE);
                    et.setHintTextColor(0xFFB3C0D2);
                    et.setSingleLine(true);
                    et.setInputType(InputType.TYPE_CLASS_TEXT);
                    et.setPadding((int) (14 * d), (int) (11 * d),
                            (int) (14 * d), (int) (11 * d));
                    GradientDrawable eg = new GradientDrawable();
                    eg.setColor(0xB3FFFFFF);
                    eg.setCornerRadius(14 * d);
                    eg.setStroke((int) d, 0x66C9D6E8);
                    et.setBackgroundDrawable(eg);
                    LinearLayout.LayoutParams elp = new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT);
                    elp.bottomMargin = (int) (14 * d);
                    card.addView(et, elp);

                    LinearLayout buttons = new LinearLayout(act);
                    buttons.setOrientation(LinearLayout.HORIZONTAL);

                    TextView cancel = ghost(act, d, "取消");
                    cancel.setOnClickListener(new View.OnClickListener() {
                        @Override public void onClick(View v) {
                            dlg.dismiss();
                        }
                    });
                    TextView ok = pink(act, d, "确定");
                    ok.setOnClickListener(new View.OnClickListener() {
                        @Override public void onClick(View v) {
                            String t = et.getText() == null ? "" : et.getText().toString().trim();
                            dlg.dismiss();
                            try {
                                cb.run(t);
                            } catch (Throwable ignore) {
                            }
                        }
                    });
                    LinearLayout.LayoutParams b1 = new LinearLayout.LayoutParams(0,
                            ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                    b1.rightMargin = (int) (6 * d);
                    LinearLayout.LayoutParams b2 = new LinearLayout.LayoutParams(0,
                            ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                    b2.leftMargin = (int) (6 * d);
                    buttons.addView(cancel, b1);
                    buttons.addView(ok, b2);
                    card.addView(buttons);

                    show(act, dlg, card, 0f);
                } catch (Throwable ignore) {
                }
            }
        });
    }

    public static void inputMulti(final Context anyCtx, final String title, final String hint,
                             final String value, final OnText cb) {
        final Activity act = actOf(anyCtx);
        if (act == null) return;
        act.runOnUiThread(new Runnable() {
            @Override public void run() {
                try {
                    final Dialog dlg = base(act);
                    final float d = act.getResources().getDisplayMetrics().density;
                    LinearLayout card = card(act, d);

                    card.addView(title(act, d, title), titleLp(d));

                    final EditText et = new EditText(act);
                    et.setText(value == null ? "" : value);
                    et.setHint(hint == null ? "" : hint);
                    et.setTextSize(13);
                    et.setTextColor(C_TITLE);
                    et.setHintTextColor(0xFFB3C0D2);
                    et.setSingleLine(false);
                    et.setMinLines(3);
                    et.setGravity(Gravity.TOP | Gravity.START);
                    et.setInputType(InputType.TYPE_CLASS_TEXT);
                    et.setPadding((int) (14 * d), (int) (11 * d),
                            (int) (14 * d), (int) (11 * d));
                    GradientDrawable eg = new GradientDrawable();
                    eg.setColor(0xB3FFFFFF);
                    eg.setCornerRadius(14 * d);
                    eg.setStroke((int) d, 0x66C9D6E8);
                    et.setBackgroundDrawable(eg);
                    LinearLayout.LayoutParams elp = new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT);
                    elp.bottomMargin = (int) (14 * d);
                    card.addView(et, elp);

                    LinearLayout buttons = new LinearLayout(act);
                    buttons.setOrientation(LinearLayout.HORIZONTAL);

                    TextView cancel = ghost(act, d, "取消");
                    cancel.setOnClickListener(new View.OnClickListener() {
                        @Override public void onClick(View v) {
                            dlg.dismiss();
                        }
                    });
                    TextView ok = pink(act, d, "确定");
                    ok.setOnClickListener(new View.OnClickListener() {
                        @Override public void onClick(View v) {
                            String t = et.getText() == null ? "" : et.getText().toString().trim();
                            dlg.dismiss();
                            try {
                                cb.run(t);
                            } catch (Throwable ignore) {
                            }
                        }
                    });
                    LinearLayout.LayoutParams b1 = new LinearLayout.LayoutParams(0,
                            ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                    b1.rightMargin = (int) (6 * d);
                    LinearLayout.LayoutParams b2 = new LinearLayout.LayoutParams(0,
                            ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                    b2.leftMargin = (int) (6 * d);
                    buttons.addView(cancel, b1);
                    buttons.addView(ok, b2);
                    card.addView(buttons);

                    show(act, dlg, card, 0f);
                } catch (Throwable ignore) {
                }
            }
        });
    }

    

    private static Activity actOf(Context c) {
        if (c instanceof Activity) return (Activity) c;
        try {
            Activity a = com.cathelperawa.owo.app.bridge.QQEnv.topActivity();
            if (a != null) return a;
        } catch (Throwable ignore) {
        }
        return null;
    }

    private static Dialog base(Activity act) {
        return new Dialog(act, android.R.style.Theme_Translucent_NoTitleBar);
    }

    private static LinearLayout card(Activity act, float d) {
        LinearLayout card = new LinearLayout(act);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding((int) (18 * d), (int) (16 * d), (int) (18 * d), (int) (14 * d));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xE6FFFFFF);
        bg.setCornerRadius(26 * d);
        bg.setStroke((int) d, 0xA6FFFFFF);
        card.setBackgroundDrawable(bg);
        card.setElevation(12 * d);
        return card;
    }

    private static TextView title(Activity act, float d, String text) {
        TextView t = new TextView(act);
        t.setText(text == null ? "" : text);
        t.setTextSize(17);
        t.setTextColor(C_TITLE);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    private static LinearLayout.LayoutParams titleLp(float d) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = (int) (12 * d);
        return lp;
    }

    private static TextView cancelRow(Activity act, float d, final Dialog dlg) {
        TextView t = new TextView(act);
        t.setText("取消");
        t.setTextSize(13);
        t.setTextColor(C_SUB);
        t.setGravity(Gravity.CENTER);
        t.setPadding(0, (int) (11 * d), 0, (int) (11 * d));
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(14 * d);
        g.setColor(0x59FFFFFF);
        t.setBackgroundDrawable(g);
        t.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                dlg.dismiss();
            }
        });
        return t;
    }

    private static TextView ghost(Activity act, float d, String label) {
        TextView t = new TextView(act);
        t.setText(label);
        t.setTextSize(13);
        t.setTextColor(0xFF6E93D6);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setGravity(Gravity.CENTER);
        t.setPadding(0, (int) (11 * d), 0, (int) (11 * d));
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(14 * d);
        g.setColor(0x7AFFFFFF);
        g.setStroke((int) d, 0x55C9D6E8);
        t.setBackgroundDrawable(g);
        return t;
    }

    private static TextView pink(Activity act, float d, String label) {
        TextView t = new TextView(act);
        t.setText(label);
        t.setTextSize(13);
        t.setTextColor(0xFFFFFFFF);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setGravity(Gravity.CENTER);
        t.setPadding(0, (int) (11 * d), 0, (int) (11 * d));
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{0xFFFF8FBE, 0xFFF0709F});
        g.setCornerRadius(14 * d);
        g.setStroke((int) d, 0xFFFFA8CE);
        t.setBackgroundDrawable(g);
        return t;
    }

    private static void show(Activity act, Dialog dlg, View card, float heightRatio) {
        dlg.setContentView(card);
        dlg.setCanceledOnTouchOutside(true);
        
        dlg.show();
        Window w = dlg.getWindow();
        if (w == null) return;
        w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        WindowManager.LayoutParams lp = w.getAttributes();
        int sw = act.getResources().getDisplayMetrics().widthPixels;
        int sh = act.getResources().getDisplayMetrics().heightPixels;
        lp.width = (int) (sw * 0.90f);
        lp.height = heightRatio > 0
                ? (int) (sh * heightRatio)
                : WindowManager.LayoutParams.WRAP_CONTENT;
        lp.gravity = Gravity.CENTER;
        lp.dimAmount = 0.24f;
        w.setAttributes(lp);
    }

    public static void confirm(final Context anyCtx, final String title, final String message,
                               final String okLabel, final boolean danger, final Runnable onOk) {
        final Activity act = actOf(anyCtx);
        if (act == null) return;
        act.runOnUiThread(new Runnable() {
            @Override public void run() {
                try {
                    final Dialog dlg = base(act);
                    final float d = act.getResources().getDisplayMetrics().density;
                    LinearLayout card = card(act, d);

                    card.addView(title(act, d, title), titleLp(d));

                    TextView msg = new TextView(act);
                    msg.setText(message == null ? "" : message);
                    msg.setTextSize(12.5f);
                    msg.setTextColor(C_SUB);
                    msg.setLineSpacing(5 * d, 1f);
                    LinearLayout.LayoutParams mlp = new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT);
                    mlp.bottomMargin = (int) (16 * d);
                    card.addView(msg, mlp);

                    LinearLayout buttons = new LinearLayout(act);
                    buttons.setOrientation(LinearLayout.HORIZONTAL);

                    TextView cancel = ghost(act, d, "算了");
                    cancel.setOnClickListener(new View.OnClickListener() {
                        @Override public void onClick(View v) {
                            dlg.dismiss();
                        }
                    });

                    TextView ok = danger ? danger(act, d, okLabel) : pink(act, d, okLabel);
                    ok.setOnClickListener(new View.OnClickListener() {
                        @Override public void onClick(View v) {
                            dlg.dismiss();
                            try {
                                onOk.run();
                            } catch (Throwable ignore) {
                            }
                        }
                    });

                    LinearLayout.LayoutParams b1 = new LinearLayout.LayoutParams(0,
                            ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                    b1.rightMargin = (int) (6 * d);
                    LinearLayout.LayoutParams b2 = new LinearLayout.LayoutParams(0,
                            ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                    b2.leftMargin = (int) (6 * d);
                    buttons.addView(cancel, b1);
                    buttons.addView(ok, b2);
                    card.addView(buttons);

                    show(act, dlg, card, 0f);
                } catch (Throwable ignore) {
                }
            }
        });
    }

    private static TextView danger(Activity act, float d, String label) {
        TextView t = new TextView(act);
        t.setText(label);
        t.setTextSize(13);
        t.setTextColor(0xFFFFFFFF);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setGravity(Gravity.CENTER);
        t.setPadding(0, (int) (11 * d), 0, (int) (11 * d));
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{0xFFF08A86, 0xFFE2645E});
        g.setCornerRadius(14 * d);
        g.setStroke((int) d, 0xFFF2A29E);
        t.setBackgroundDrawable(g);
        return t;
    }

    public static String[] numbered(String[] items) {
        String[] out = new String[items.length];
        for (int i = 0; i < items.length; i++) out[i] = (i + 1) + ". " + items[i];
        return out;
    }
}
