package com.oney.WebRTCModule;

import android.content.Intent;
import android.media.projection.MediaProjection;
import android.util.Log;

import org.webrtc.ScreenCapturerAndroid;
import org.webrtc.VideoCapturer;

public class ScreenVideoCaptureController extends AbstractVideoCaptureController {
    /**
     * The {@link Log} tag with which {@code ScreenVideoCaptureController} is to log.
     */
    private static final String TAG = ScreenVideoCaptureController.class.getSimpleName();

    private final Intent mediaProjectionPermissionResultData;

    public ScreenVideoCaptureController(
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
                    Log.w(TAG, "User revoked permission to capture the screen.");
                }
            });


        return videoCapturer;
    }
}
