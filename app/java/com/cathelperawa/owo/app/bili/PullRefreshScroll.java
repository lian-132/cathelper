package com.cathelperawa.owo.app.bili;

import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ScrollView;

public final class PullRefreshScroll extends ScrollView {

    public interface OnPull {
        
        void onPull(int dy);

        boolean onRelease(int dy, boolean overThreshold);
    }

    private final int touchSlop;
    
    private int threshold;
    
    private int maxPull;

    private OnPull listener;
    private float downY = 0f;
    private boolean pulling = false;
    private boolean locked = false;

    public PullRefreshScroll(Context c) {
        super(c);
        touchSlop = (int) (8 * c.getResources().getDisplayMetrics().density);
        threshold = (int) (64 * c.getResources().getDisplayMetrics().density);
        maxPull = (int) (120 * c.getResources().getDisplayMetrics().density);
        setOverScrollMode(OVER_SCROLL_NEVER);
    }

    public void setOnPull(OnPull l) {
        this.listener = l;
    }

    public void setThresholdDp(float dp) {
        threshold = (int) (dp * getResources().getDisplayMetrics().density);
    }

    public void setLocked(boolean lock) {
        locked = lock;
        if (lock) {
            pulling = false;
            downY = 0f;
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (locked) return super.onInterceptTouchEvent(ev);
        if (ev.getActionMasked() == MotionEvent.ACTION_DOWN) {
            downY = ev.getY();
            pulling = false;
        }
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (locked) return super.onTouchEvent(ev);
        if (listener == null) return super.onTouchEvent(ev);

        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downY = ev.getY();
                pulling = false;
                break;

            case MotionEvent.ACTION_MOVE: {
                if (getScrollY() > 0) {
                    if (pulling) {
                        pulling = false;
                        listener.onPull(0);
                    }
                    break;
                }
                float dy = ev.getY() - downY;
                if (!pulling && dy > touchSlop) {
                    pulling = true;
                }
                if (pulling) {
                    
                    int pull = (int) Math.min(maxPull, dy * 0.55f);
                    listener.onPull(Math.max(0, pull));
                    return true;        
                }
                break;
            }

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                if (pulling) {
                    int pull = (int) Math.min(maxPull, Math.max(0, ev.getY() - downY) * 0.55f);
                    pulling = false;
                    boolean over = pull >= threshold;
                    boolean accepted = listener.onRelease(pull, over);
                    if (!accepted) listener.onPull(0);
                    return true;
                }
                break;
            }
        }
        return super.onTouchEvent(ev);
    }

    public int threshold() {
        return threshold;
    }

    public void setTopInset(int px) {
        setPadding(getPaddingLeft(), Math.max(0, px), getPaddingRight(), getPaddingBottom());
    }
}
