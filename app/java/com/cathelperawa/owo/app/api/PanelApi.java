package com.cathelperawa.owo.app.api;

import android.content.Context;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.cathelperawa.owo.app.ui.CatPanel;

public final class PanelApi {

    private PanelApi() {}

    public interface OnOk { void run(); }

    
    public static final int C_TITLE = CatPanel.C_TITLE;
    
    public static final int C_BODY = CatPanel.C_BODY;
    
    public static final int C_HINT = CatPanel.C_HINT;
    
    public static final int C_PINK = CatPanel.C_PINK;
    
    public static final int C_BLUE = CatPanel.C_BLUE;

    public static void show(Context ctx, String badge, String title, String sub,
                            View body, String okLabel, OnOk onOk) {
        CatPanel.show(ctx, badge, title, sub, body, okLabel, onOk == null ? null
                : new CatPanel.OnOk() {
                    @Override public void run() { onOk.run(); }
                });
    }

    public static void show(Context ctx, String title, String sub, View body, OnOk onOk) {
        show(ctx, null, title, sub, body, "确定", onOk);
    }

    public static LinearLayout fieldBox(Context c) { return CatPanel.fieldBox(c); }

    public static LinearLayout field(Context c, int index, String hint, String value, String label) {
        return CatPanel.field(c, index, hint, value, label);
    }

    public static EditText fieldEdit(View fieldBox) { return CatPanel.fieldEdit(fieldBox); }

    public static View emptyState(Context c, String msg) { return CatPanel.emptyState(c, msg); }

    public static TextView pillButton(Context c, String label, boolean primary) {
        return CatPanel.pillButton(c, label, primary);
    }
}
