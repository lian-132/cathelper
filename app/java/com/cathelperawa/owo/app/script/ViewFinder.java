package com.cathelperawa.owo.app.script;

import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.cathelperawa.owo.app.core.HookLog;

import java.util.ArrayList;
import java.util.List;

public final class ViewFinder {

    public interface Handler {
        void onView(View v);
    }

    private static final int MAX_NODES = 4000;

    private ViewFinder() {}

    public static View findText(View root, String text) {
        if (root == null || text == null || text.length() == 0) return null;
        final String[] found = new String[1];
        final View[] hit = new View[1];
        walk(root, new Handler() {
            @Override public void onView(View v) {
                if (hit[0] != null) return;
                if (!(v instanceof TextView)) return;
                CharSequence cs = ((TextView) v).getText();
                if (cs == null) return;
                if (cs.toString().indexOf(text) >= 0) hit[0] = v;
            }
        });
        return hit[0];
    }

    public static View findByClass(View root, String cls) {
        if (root == null || cls == null || cls.length() == 0) return null;
        final View[] hit = new View[1];
        walk(root, new Handler() {
            @Override public void onView(View v) {
                if (hit[0] != null) return;
                String n = v.getClass().getName();
                if (n.equals(cls) || n.endsWith("." + cls)) hit[0] = v;
            }
        });
        return hit[0];
    }

    public static View findByDesc(View root, String desc) {
        if (root == null || desc == null || desc.length() == 0) return null;
        final View[] hit = new View[1];
        walk(root, new Handler() {
            @Override public void onView(View v) {
                if (hit[0] != null) return;
                CharSequence cs = v.getContentDescription();
                if (cs != null && cs.toString().indexOf(desc) >= 0) hit[0] = v;
            }
        });
        return hit[0];
    }

    public static void walk(View root, Handler h) {
        if (root == null || h == null) return;
        int[] budget = new int[]{MAX_NODES};
        try {
            walkInner(root, h, budget);
        } catch (Throwable t) {
            HookLog.w("[查找] 遍历出错：" + t);
        }
    }

    private static void walkInner(View v, Handler h, int[] budget) {
        if (v == null || budget[0] <= 0) return;
        budget[0]--;
        try {
            h.onView(v);
        } catch (Throwable ignore) {
        }
        if (!(v instanceof ViewGroup)) return;
        ViewGroup g = (ViewGroup) v;
        int n = g.getChildCount();
        for (int i = 0; i < n; i++) {
            if (budget[0] <= 0) return;
            walkInner(g.getChildAt(i), h, budget);
        }
    }

    public static List<View> findAllByClass(View root, String cls) {
        final List<View> out = new ArrayList<View>();
        if (root == null || cls == null) return out;
        walk(root, new Handler() {
            @Override public void onView(View v) {
                String n = v.getClass().getName();
                if (n.equals(cls) || n.endsWith("." + cls)) out.add(v);
            }
        });
        return out;
    }
}
