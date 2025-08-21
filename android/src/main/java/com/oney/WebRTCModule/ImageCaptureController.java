package com.oney.WebRTCModule;

import android.content.Context;
import android.util.Log;
import android.net.Uri;

import org.webrtc.VideoCapturer;
import com.oney.WebRTCModule.ImageCapturer;

public class ImageCaptureController extends AbstractVideoCaptureController {
    /**
     * The {@link Log} tag with which {@code ImageCaptureController} is to log.
     */
    private static final String TAG = ImageCaptureController.class.getSimpleName();

    private static final int DEFAULT_WIDTH = 1;
    private static final int DEFAULT_HEIGHT = 1;
    private static final int DEFAULT_FPS = 1;

    private final Context context;
    private final Uri asset;

    public ImageCaptureController(Context context, Uri asset) {
        super(DEFAULT_WIDTH, DEFAULT_HEIGHT, DEFAULT_FPS);

        this.context = context;
        this.asset = asset;
    }

    @Override
    public String getDeviceId() {
        return "image-capture";
    }

    @Override
    public void dispose() {
        super.dispose();
    }

    @Override
    protected VideoCapturer createVideoCapturer() {
        VideoCapturer videoCapturer =
                new ImageCapturer(context, asset, new ImageCapturer.ImageEventsHandler() {
                    @Override
                    public void onLoaded(int width, int height) {
                        Log.w(TAG, "image loaded for track");
                        actualWidth = width;
                        actualHeight = height;
                    }
                });

        return videoCapturer;
    }
}
