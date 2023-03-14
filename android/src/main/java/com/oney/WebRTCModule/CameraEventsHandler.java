package com.oney.WebRTCModule;

import android.util.Log;

import org.webrtc.CameraVideoCapturer;

class CameraEventsHandler implements CameraVideoCapturer.CameraEventsHandler {
    /**
     * The {@link Log} tag with which {@code CameraEventsHandler} is to log.
     */
    private final static String TAG = WebRTCModule.TAG;

    // Callback invoked when camera closed.
    @Override
    public void onCameraClosed() {
        Log.d(TAG, "CameraEventsHandler.onFirstFrameAvailable");
    }

    // Called when camera is disconnected.
    @Override
    public void onCameraDisconnected() {
        Log.d(TAG, "CameraEventsHandler.onCameraDisconnected");
    }

    // Camera error handler - invoked when camera can not be opened or any
    // camera exception happens on camera thread.
    @Override
    public void onCameraError(String errorDescription) {
        Log.d(
            TAG,
            "CameraEventsHandler.onCameraError: errorDescription="
                + errorDescription);
    }

    // Invoked when camera stops receiving frames
    @Override
    public void onCameraFreezed(String errorDescription) {
        Log.d(
            TAG,
            "CameraEventsHandler.onCameraFreezed: errorDescription="
                + errorDescription);
    }

    // Callback invoked when camera is opening.
    @Override
    public void onCameraOpening(String cameraName) {
        Log.d(
            TAG,
            "CameraEventsHandler.onCameraOpening: cameraName="
                + cameraName);
    }

    // Callback invoked when first camera frame is available after camera is opened.
    @Override
    public void onFirstFrameAvailable() {
        Log.d(TAG, "CameraEventsHandler.onFirstFrameAvailable");
    }
}
