package com.cathelperawa.owo.app.bili;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.VideoView;

public final class FitVideoView extends VideoView {

    private int vw = 0, vh = 0;
    
    private float fallbackRatio = 9f / 16f;

    public FitVideoView(Context c) {
        super(c);
    }

    public FitVideoView(Context c, AttributeSet a) {
        super(c, a);
    }

    public void setVideoSize(int width, int height) {
        if (width > 0 && height > 0 && (width != vw || height != vh)) {
            vw = width;
            vh = height;
            requestLayout();
        }
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int cw = View.MeasureSpec.getSize(widthSpec);
        int ch = View.MeasureSpec.getSize(heightSpec);
        boolean hUnbounded = View.MeasureSpec.getMode(heightSpec) == View.MeasureSpec.UNSPECIFIED;

        if (cw <= 0) {
            super.onMeasure(widthSpec, heightSpec);
            return;
        }

        if (vw <= 0 || vh <= 0) {
            
            int h = (int) (cw * fallbackRatio + 0.5f);
            if (!hUnbounded && ch > 0) h = Math.min(h, ch);
            setMeasuredDimension(cw, h);
            return;
        }

        float videoRatio = (float) vw / vh;
        if (hUnbounded) {
            
            setMeasuredDimension(cw, (int) (cw / videoRatio + 0.5f));
            return;
        }
        if (ch <= 0) {
            setMeasuredDimension(cw, (int) (cw / videoRatio + 0.5f));
            return;
        }
        
        float scale = Math.min((float) cw / vw, (float) ch / vh);
        int w = (int) (vw * scale + 0.5f);
        int h = (int) (vh * scale + 0.5f);
        setMeasuredDimension(Math.max(1, w), Math.max(1, h));
    }
}
