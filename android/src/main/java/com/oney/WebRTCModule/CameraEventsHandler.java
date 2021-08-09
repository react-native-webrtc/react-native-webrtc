package com.oney.WebRTCModule;

import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.Arguments;

import android.util.Log;

import org.webrtc.CameraVideoCapturer;

class CameraEventsHandler implements CameraVideoCapturer.CameraEventsHandler {

    private final ReactApplicationContext reactContext;

    public CameraEventsHandler(ReactApplicationContext reactContext) {
        this.reactContext = reactContext;
    }

    /**
     * The {@link Log} tag with which {@code CameraEventsHandler} is to log.
     */
    private final static String TAG = WebRTCModule.TAG;

    // Callback invoked when camera closed.
    @Override
    public void onCameraClosed() {

        // WritableMap params = Arguments.createMap();
        // params.putString("name", "onCameraClosed");
        // params.putString("camera", "local");
        // WebRTCModule module
        //     = reactContext.getNativeModule(WebRTCModule.class);
        // module.jsEvent("jsEvent", params);

        Log.d(TAG, "CameraEventsHandler.onCameraClosed");
    }

    // Called when camera is disconnected.
    @Override
    public void onCameraDisconnected() {

        WritableMap params = Arguments.createMap();
        params.putString("name", "onCameraDisconnected");
        params.putString("camera", "local");
        WebRTCModule module
            = reactContext.getNativeModule(WebRTCModule.class);
        module.jsEvent("jsEvent", params);

        Log.d(TAG, "CameraEventsHandler.onCameraDisconnected");
    }

    // Camera error handler - invoked when camera can not be opened or any
    // camera exception happens on camera thread.
    @Override
    public void onCameraError(String errorDescription) {

        WritableMap params = Arguments.createMap();
        params.putString("name", "onCameraError");
        params.putString("camera", "local");
        WebRTCModule module
            = reactContext.getNativeModule(WebRTCModule.class);
        module.jsEvent("jsEvent", params);

        Log.d(
            TAG,
            "CameraEventsHandler.onCameraError: errorDescription="
                + errorDescription);
    }

    // Invoked when camera stops receiving frames
    @Override
    public void onCameraFreezed(String errorDescription) {

        WritableMap params = Arguments.createMap();
        params.putString("name", "onCameraFreezed");
        params.putString("camera", "local");
        WebRTCModule module
            = reactContext.getNativeModule(WebRTCModule.class);
        module.jsEvent("jsEvent", params);

        Log.d(
            TAG,
            "CameraEventsHandler.onCameraFreezed: errorDescription="
                + errorDescription);
    }

    // Callback invoked when camera is opening.
    @Override
    public void onCameraOpening(String cameraName) {

        // WritableMap params = Arguments.createMap();
        // params.putString("name", "onCameraOpening");
        // params.putString("camera", "local");
        // WebRTCModule module
        //     = reactContext.getNativeModule(WebRTCModule.class);
        // module.jsEvent("jsEvent", params);

        Log.d(
            TAG,
            "CameraEventsHandler.onCameraOpening: cameraName="
                + cameraName);
    }

    // Callback invoked when first camera frame is available after camera is opened.
    @Override
    public void onFirstFrameAvailable() {

        // WritableMap params = Arguments.createMap();
        // params.putString("name", "onFirstFrameAvailable");
        // params.putString("camera", "local");
        // WebRTCModule module
        //     = reactContext.getNativeModule(WebRTCModule.class);
        // module.jsEvent("jsEvent", params);

        Log.d(TAG, "CameraEventsHandler.onFirstFrameAvailable");
    }
}
