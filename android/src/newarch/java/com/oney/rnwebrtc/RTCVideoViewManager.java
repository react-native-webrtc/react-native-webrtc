package com.oney.rnwebrtc;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.facebook.react.bridge.ReactApplicationContext;

import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.ViewManagerDelegate;
import com.facebook.react.uimanager.annotations.ReactProp;

import com.facebook.react.viewmanagers.RTCVideoViewManagerInterface;
import com.facebook.react.viewmanagers.RTCVideoViewManagerDelegate;

public class RTCVideoViewManager extends SimpleViewManager<RTCVideoView> implements RTCVideoViewManagerInterface<RTCVideoView> {

    private final ViewManagerDelegate<RTCVideoView> viewManagerDelegate;
    private final RTCVideoViewManagerImpl rtcVideoViewManagerImpl;

    public RTCVideoViewManager(ReactApplicationContext context) {
        viewManagerDelegate = new RTCVideoViewManagerDelegate<>(this);
        rtcVideoViewManagerImpl = new RTCVideoViewManagerImpl();
    }

    @Nullable
    @Override
    protected ViewManagerDelegate<RTCVideoView> getDelegate() {
        return viewManagerDelegate;
    }

    @NonNull
    @Override
    public String getName() {
        return RTCVideoViewManagerImpl.NAME;
    }

    @NonNull
    @Override
    protected RTCVideoView createViewInstance(@NonNull ThemedReactContext context) {
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