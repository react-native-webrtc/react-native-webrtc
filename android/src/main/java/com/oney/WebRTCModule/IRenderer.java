package com.oney.WebRTCModule;

import android.support.annotation.ColorInt;

import org.webrtc.EglBase;
import org.webrtc.RendererCommon;
import org.webrtc.VideoSink;

public interface IRenderer extends VideoSink, RendererCommon.RendererEvents {
    public abstract void clearImage();
    public abstract void release();
    public abstract void setScalingType(RendererCommon.ScalingType scalingType);
    public abstract void init(EglBase.Context sharedContext, RendererCommon.RendererEvents rendererEvents);
    public abstract void setBackgroundColor(@ColorInt int color);
    public abstract void layout(int l, int t, int r, int b);
    public abstract void requestLayout();
    public abstract void setMirror(final boolean mirror);
    public abstract void setZOrderMediaOverlay(boolean isMediaOverlay);
    public abstract void setZOrderOnTop(boolean onTop);
}
