package com.oney.WebRTCModule;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.facebook.react.module.annotations.ReactModule;
import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.ViewManagerDelegate;
import com.facebook.react.uimanager.annotations.ReactProp;
import com.facebook.react.viewmanagers.RTCVideoViewManagerDelegate;
import com.facebook.react.viewmanagers.RTCVideoViewManagerInterface;

import java.util.HashMap;
import java.util.Map;

@ReactModule(name = RTCVideoViewManager.REACT_CLASS)
public class RTCVideoViewManager
        extends SimpleViewManager<WebRTCView> implements RTCVideoViewManagerInterface<WebRTCView> {
    static final String REACT_CLASS = "RTCVideoView";

    private final ViewManagerDelegate<WebRTCView> mDelegate;

    public RTCVideoViewManager() {
        mDelegate = new RTCVideoViewManagerDelegate<>(this);
    }

    @Nullable
    @Override
    protected ViewManagerDelegate<WebRTCView> getDelegate() {
        return mDelegate;
    }

    @NonNull
    @Override
    public String getName() {
        return REACT_CLASS;
    }

    @NonNull
    @Override
    public WebRTCView createViewInstance(@NonNull ThemedReactContext context) {
        return new WebRTCView(context);
    }

    @Override
    @ReactProp(name = "mirror")
    public void setMirror(WebRTCView view, boolean mirror) {
        view.setMirror(mirror);
    }

    @Override
    @ReactProp(name = "objectFit")
    public void setObjectFit(WebRTCView view, @Nullable String objectFit) {
        view.setObjectFit(objectFit);
    }

    @Override
    @ReactProp(name = "streamURL")
    public void setStreamURL(WebRTCView view, @Nullable String streamURL) {
        view.setStreamURL(streamURL);
    }

    @Override
    @ReactProp(name = "zOrder")
    public void setZOrder(WebRTCView view, int zOrder) {
        view.setZOrder(zOrder);
    }

    @ReactProp(name = "onDimensionsChange")
    public void setOnDimensionsChange(WebRTCView view, boolean onDimensionsChange) {
        view.setOnDimensionsChange(onDimensionsChange);
    }

    // iOS-only PIP props — no-ops on Android

    @Override
    @ReactProp(name = "iosPIPEnabled")
    public void setIosPIPEnabled(WebRTCView view, boolean value) {}

    @Override
    @ReactProp(name = "iosPIPStartAutomatically")
    public void setIosPIPStartAutomatically(WebRTCView view, boolean value) {}

    @Override
    @ReactProp(name = "iosPIPStopAutomatically")
    public void setIosPIPStopAutomatically(WebRTCView view, boolean value) {}

    @Override
    @ReactProp(name = "iosPIPPreferredWidth")
    public void setIosPIPPreferredWidth(WebRTCView view, float value) {}

    @Override
    @ReactProp(name = "iosPIPPreferredHeight")
    public void setIosPIPPreferredHeight(WebRTCView view, float value) {}

    @Override
    public Map<String, Object> getExportedCustomDirectEventTypeConstants() {
        Map<String, Object> eventTypeConstants = new HashMap<>();
        Map<String, String> dimensionsChangeEvent = new HashMap<>();
        dimensionsChangeEvent.put("registrationName", "onDimensionsChange");
        eventTypeConstants.put("onDimensionsChange", dimensionsChangeEvent);
        return eventTypeConstants;
    }
}
