package com.oney.rnwebrtc;

import com.facebook.react.uimanager.ThemedReactContext;

public class RTCVideoViewManagerImpl {

    public static final String NAME = "RTCVideoView";

    public RTCVideoView createViewInstance(ThemedReactContext context) {
        return new RTCVideoView(context);
    }

    /**
     * Sets the indicator which determines whether a specific {@link RTCVideoView}
     * is to mirror the video specified by {@code streamURL} during its rendering.
     * For more details, refer to the documentation of the {@code mirror} property
     * of the JavaScript counterpart of {@code RTCVideoView} i.e. {@code RTCView}.
     *
     * @param view The {@code RTCVideoView} on which the specified {@code mirror} is
     * to be set.
     * @param mirror If the specified {@code RTCVideoView} is to mirror the video
     * specified by its associated {@code streamURL} during its rendering,
     * {@code true}; otherwise, {@code false}.
     */
    public void setMirror(RTCVideoView view, boolean mirror) {
        view.setMirror(mirror);
    }

    /**
     * In the fashion of
     * https://www.w3.org/TR/html5/embedded-content-0.html#dom-video-videowidth
     * and https://www.w3.org/TR/html5/rendering.html#video-object-fit, resembles
     * the CSS style {@code object-fit}.
     *
     * @param view The {@code RTCVideoView} on which the specified {@code objectFit}
     * is to be set.
     * @param objectFit For details, refer to the documentation of the
     * {@code objectFit} property of the JavaScript counterpart of
     * {@code RTCVideoView} i.e. {@code RTCView}.
     */
    public void setObjectFit(RTCVideoView view, String objectFit) {
        view.setObjectFit(objectFit);
    }

    /**

     */
    public void setStreamURL(RTCVideoView view, String streamURL) {
        view.setStreamURL(streamURL);
    }

    /**
     * Sets the z-order of a specific {@link RTCVideoView} in the stacking space of
     * all {@code RTCVideoView}s. For more details, refer to the documentation of
     * the {@code zOrder} property of the JavaScript counterpart of
     * {@code RTCVideoView} i.e. {@code RTCView}.
     *
     * @param view The {@code RTCVideoView} on which the specified {@code zOrder} is
     * to be set.
     * @param zOrder The z-order to set on the specified {@code RTCVideoView}.
     */
    public void setZOrder(RTCVideoView view, int zOrder) {
        view.setZOrder(zOrder);
    }
}