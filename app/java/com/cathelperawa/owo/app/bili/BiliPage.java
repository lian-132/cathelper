package com.cathelperawa.owo.app.bili;

import android.view.View;

public abstract class BiliPage {

    protected final BiliActivity host;
    private View view;

    public BiliPage(BiliActivity host) {
        this.host = host;
    }

    public abstract View onCreate();

    public final View view() {
        if (view == null) {
            view = onCreate();
            if (view != null) view.setTag(this);
        }
        return view;
    }

    public void onShow() { }

    public void onHide() { }

    public boolean onBack() { return false; }

    public void onDestroy() { }

    public boolean ownsTopbar() { return true; }
}
