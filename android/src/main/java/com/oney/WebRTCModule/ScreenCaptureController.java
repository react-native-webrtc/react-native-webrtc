package com.oney.WebRTCModule;

import android.content.Intent;
import android.media.projection.MediaProjection;
import android.util.Log;

import org.webrtc.ScreenCapturerAndroid;
import org.webrtc.VideoCapturer;

public class ScreenCaptureController extends AbstractVideoCaptureController {
    /**
     * The {@link Log} tag with which {@code ScreenCaptureController} is to log.
     */
    private static final String TAG = ScreenCaptureController.class.getSimpleName();

    private final Intent mediaProjectionPermissionResultData;

    public ScreenCaptureController(
        int width, 
        int height, 
        int fps, 
        Intent mediaProjectionPermissionResultData) {
        super(width, height, fps);

        this.mediaProjectionPermissionResultData = mediaProjectionPermissionResultData;
    }

    @Override
    protected VideoCapturer createVideoCapturer() {
        VideoCapturer videoCapturer = new ScreenCapturerAndroid(
            mediaProjectionPermissionResultData,
            new MediaProjection.Callback() {
                @Override
                public void onStop() {
                    Log.w(TAG, "Media projection stopped.");
                }
            });


        return videoCapturer;
    }
}
