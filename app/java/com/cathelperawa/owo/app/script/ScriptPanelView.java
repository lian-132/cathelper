package com.cathelperawa.owo.app.script;

import android.view.View;
import android.widget.LinearLayout;

import com.cathelperawa.owo.app.ui.Page;
import com.cathelperawa.owo.app.ui.ScriptScreen;
import com.cathelperawa.owo.app.ui.ScriptUi;

public final class ScriptPanelView extends Page {

    private final ScriptPanelRegistry.Panel panel;

    public ScriptPanelView(Host host, ScriptPanelRegistry.Panel p) {
        super(host);
        this.panel = p;
    }

    @Override public View build() {
        View[] out = new View[2];
        LinearLayout body = pageRoot(panel.title, "", out, true, 13, "bg_script", 0xC8);
        try {
            ScriptUi ui = new ScriptUi(ctx);
            ScriptScreen.Result r = new ScriptScreen.Result();
            panel.view.build(ui, r);
            View v = r.get();
            if (v != null) {
                body.addView(v, matchWrap(0));
            } else {
                body.addView(text("脚本没有返回界面（记得调 out.content(...)）",
                        12.5f, 0xFF8A9BB0, false, 0), matchWrap(0));
            }
            
            if (r.bgPath() != null && r.bgPath().length() > 0) {
                applyBg(out[0], r.bgPath(), r.bgVeil(), r.bgImageAlpha());
            }
        } catch (Throwable t) {
            com.cathelperawa.owo.app.core.HookLog.e("[脚本面板] 自绘页构建失败 " + panel.title, t);
            body.addView(text("页面出错了：" + ScriptScreen.rootCause(t), 12.5f, 0xFFC0392B, false, 0), matchWrap(0));
            body.addView(text("改完脚本点「重载」再进来看。",
                    11, 0xFF8A9BB0, false, 0), matchWrap(0));
        }
        return out[0];
    }

    private void applyBg(View rootView, String path, int veilAlpha, int imgAlpha) {
        try {
            if (!(rootView instanceof android.widget.FrameLayout)) return;
            android.widget.FrameLayout fr = (android.widget.FrameLayout) rootView;
            android.graphics.Bitmap bm = android.graphics.BitmapFactory.decodeFile(path);
            if (bm == null) {
                com.cathelperawa.owo.app.core.HookLog.w("[脚本面板] 背景图读不出来：" + path);
                return;
            }
            android.widget.ImageView iv = new android.widget.ImageView(ctx);
            iv.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);
            iv.setImageBitmap(bm);
            iv.setAlpha(imgAlpha);

            
            
            int keep = fr.getChildCount() > 0 ? 1 : 0;
            while (fr.getChildCount() > keep) fr.removeViewAt(0);

            fr.addView(iv, 0, new android.widget.FrameLayout.LayoutParams(
                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT));

            View veil = new View(ctx);
            veil.setBackgroundColor((veilAlpha << 24) | 0xFFFFFF);
            fr.addView(veil, 1, new android.widget.FrameLayout.LayoutParams(
                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT));
        } catch (Throwable t) {
            com.cathelperawa.owo.app.core.HookLog.e("[脚本面板] 设置背景失败", t);
        }
    }
}
