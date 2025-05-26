package com.oney.WebRTCModule;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.common.MapBuilder;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.ViewGroupManager;
import com.facebook.react.uimanager.annotations.ReactProp;

import java.util.Map;

public class RTCVideoViewManager extends ViewGroupManager<WebRTCView> {
    private static final String REACT_CLASS = "RTCVideoView";

    @Override
    public String getName() {
        return REACT_CLASS;
    }

    public final int COMMAND_ENTER_PIP = 1;
    public final int COMMAND_EXIT_PIP = 2;

    @Override
    public WebRTCView createViewInstance(ThemedReactContext context) {
        return new WebRTCView(context);
    }

    /**
     * Sets the indicator which determines whether a specific {@link WebRTCView}
     * is to mirror the video specified by {@code streamURL} during its rendering.
     * For more details, refer to the documentation of the {@code mirror} property
     * of the JavaScript counterpart of {@code WebRTCView} i.e. {@code RTCView}.
     *
     * @param view The {@code WebRTCView} on which the specified {@code mirror} is
     * to be set.
     * @param mirror If the specified {@code WebRTCView} is to mirror the video
     * specified by its associated {@code streamURL} during its rendering,
     * {@code true}; otherwise, {@code false}.
     */
    @ReactProp(name = "mirror")
    public void setMirror(WebRTCView view, boolean mirror) {
        view.setMirror(mirror);
    }

    /**
     * In the fashion of
     * https://www.w3.org/TR/html5/embedded-content-0.html#dom-video-videowidth
     * and https://www.w3.org/TR/html5/rendering.html#video-object-fit, resembles
     * the CSS style {@code object-fit}.
     *
     * @param view The {@code WebRTCView} on which the specified {@code objectFit}
     * is to be set.
     * @param objectFit For details, refer to the documentation of the
     * {@code objectFit} property of the JavaScript counterpart of
     * {@code WebRTCView} i.e. {@code RTCView}.
     */
    @ReactProp(name = "objectFit")
    public void setObjectFit(WebRTCView view, String objectFit) {
        view.setObjectFit(objectFit);
    }

    @ReactProp(name = "streamURL")
    public void setStreamURL(WebRTCView view, String streamURL) {
        view.setStreamURL(streamURL);
    }

    /**
     * Sets the z-order of a specific {@link WebRTCView} in the stacking space of
     * all {@code WebRTCView}s. For more details, refer to the documentation of
     * the {@code zOrder} property of the JavaScript counterpart of
     * {@code WebRTCView} i.e. {@code RTCView}.
     *
     * @param view The {@code WebRTCView} on which the specified {@code zOrder} is
     * to be set.
     * @param zOrder The z-order to set on the specified {@code WebRTCView}.
     */
    @ReactProp(name = "zOrder")
    public void setZOrder(WebRTCView view, int zOrder) {
        view.setZOrder(zOrder);
    }


    /**
     * Sets whether Picture-in-Picture (PiP) will be handled by this {@code WebRTCView}.
     *
     * This corresponds to the {@code pictureInPictureEnabled} prop in the JavaScript
     * counterpart of {@code WebRTCView} (i.e. {@code RTCView}).
     *
     * @param view The {@code WebRTCView} on which the {@code pictureInPictureEnabled} flag is to be set.
     * @param pictureInPictureEnabled Whether this view should handle platform Picture-In-Picture API.
     */
    @ReactProp(name = "pictureInPictureEnabled", defaultBoolean = false)
    public void setPictureInPictureEnabled(WebRTCView view, Boolean pictureInPictureEnabled) {
        view.setPictureInPictureEnabled(pictureInPictureEnabled);
    }
    /**
     * Sets whether Picture-in-Picture (PiP) mode should automatically be entered
     * when certain conditions are met (e.g., user navigates away or app goes to background).
     *
     * This corresponds to the {@code isAutoEnterEnabled} prop in the JavaScript
     * counterpart of {@code WebRTCView} (i.e. {@code RTCView}).
     *
     * @param view The {@code WebRTCView} on which the {@code isAutoEnterEnabled} flag is to be set.
     * @param autoStartPictureInPicture Whether PiP should auto-enter on supported platforms.
     */
    @ReactProp(name = "autoStartPictureInPicture", defaultBoolean = true)
    public void setAutoStartPictureInPicture(WebRTCView view, Boolean autoStartPictureInPicture) {
        view.setAutoStartPictureInPicture(autoStartPictureInPicture);
    }

    @ReactProp(name = "pictureInPicturePreferredSize")
    public void setPictureInPicturePreferredSize(WebRTCView view, @Nullable ReadableMap size) {
        view.setPictureInPicturePreferredSize(size);
    }

    @Nullable
    @Override
    public Map<String, Integer> getCommandsMap() {
        return MapBuilder.of(
                "startPictureInPicture", COMMAND_ENTER_PIP,
                "stopPictureInPicture", COMMAND_EXIT_PIP
                );
    }

    @Override
    public void receiveCommand(
            @NonNull WebRTCView view,
            String commandId,
            @Nullable ReadableArray args
    ) {
        super.receiveCommand(view, commandId, args);
        int commandIdInt = Integer.parseInt(commandId);

        switch (commandIdInt) {
            case COMMAND_ENTER_PIP:
                view.enterPictureInPicture();
                break;
            case COMMAND_EXIT_PIP:
                // Not supported in android
                break;
            default: {}
        }
    }

    @Override
    public Map getExportedCustomBubblingEventTypeConstants() {
        return MapBuilder.builder().put(
                WebRTCView.onPictureInPictureChangeEventName,
                MapBuilder.of(
                        "phasedRegistrationNames",
                        MapBuilder.of("bubbled", WebRTCView.onPictureInPictureChangeEventName)
                )
        ).build();
    }
}
