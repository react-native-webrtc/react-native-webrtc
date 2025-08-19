package com.oney.WebRTCModule;

import android.content.Context;
import android.util.Log;
import android.net.Uri;

import org.webrtc.VideoCapturer;
import com.oney.WebRTCModule.FileCapturer;

public class FileCaptureController extends AbstractVideoCaptureController {
    /**
     * The {@link Log} tag with which {@code FileCaptureController} is to log.
     */
    private static final String TAG = FileCaptureController.class.getSimpleName();

    private static final int DEFAULT_WIDTH = 1;
    private static final int DEFAULT_HEIGHT = 1;
    private static final int DEFAULT_FPS = 1;

    private final Context context;
    private final Uri asset;

    public FileCaptureController(Context context, Uri asset) {
        super(DEFAULT_WIDTH, DEFAULT_HEIGHT, DEFAULT_FPS);

        this.context = context;
        this.asset = asset;
    }

    @Override
    public String getDeviceId() {
        return "file-capture";
    }

    @Override
    public void dispose() {
        super.dispose();
    }

    @Override
    protected VideoCapturer createVideoCapturer() {
        VideoCapturer videoCapturer =
                new FileCapturer(context, asset, new FileCapturer.FileEventsHandler() {
                    @Override
                    public void onLoaded(int width, int height) {
                        Log.w(TAG, "File loaded for track");
                        actualWidth = width;
                        actualHeight = height;
                    }
                });

        return videoCapturer;
    }
}
