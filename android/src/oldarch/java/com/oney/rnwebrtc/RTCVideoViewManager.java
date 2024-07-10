package com.oney.rnwebrtc;

import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.annotations.ReactProp;

import com.facebook.react.bridge.ReactApplicationContext;

public class RTCVideoViewManager extends SimpleViewManager<RTCVideoView> {

    private final RTCVideoViewManagerImpl rtcVideoViewManagerImpl;

    public RTCVideoViewManager(ReactApplicationContext context) {
        rtcVideoViewManagerImpl = new RTCVideoViewManagerImpl();
    }

    @Override
    public String getName() {
        return RTCVideoViewManagerImpl.NAME;
    }

    @Override
    public RTCVideoView createViewInstance(ThemedReactContext context) {
        return rtcVideoViewManagerImpl.createViewInstance(context);
    }

    @ReactProp(name = "mirror")
    public void setMirror(RTCVideoView view, boolean mirror) {
        rtcVideoViewManagerImpl.setMirror(view, mirror);
    }

    @ReactProp(name = "objectFit")
    public void setObjectFit(RTCVideoView view, String objectFit) {
        rtcVideoViewManagerImpl.setObjectFit(view, objectFit);
    }

    @ReactProp(name = "streamURL")
    public void setStreamURL(RTCVideoView view, String streamURL) {
        rtcVideoViewManagerImpl.setStreamURL(view, streamURL);
    }

    @ReactProp(name = "zOrder")
    public void setZOrder(RTCVideoView view, int zOrder) {
        rtcVideoViewManagerImpl.setZOrder(view, zOrder);
    }
}
