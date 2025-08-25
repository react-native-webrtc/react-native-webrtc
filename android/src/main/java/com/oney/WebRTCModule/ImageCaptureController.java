package com.oney.WebRTCModule;

import android.content.Context;
import android.util.Log;
import android.net.Uri;

import org.webrtc.VideoCapturer;
import org.webrtc.VideoFrame;
import com.oney.WebRTCModule.ImageCapturer;

public class ImageCaptureController extends AbstractVideoCaptureController {
    /**
     * The {@link Log} tag with which {@code ImageCaptureController} is to log.
     */
    private static final String TAG = ImageCaptureController.class.getSimpleName();

    private static final int DEFAULT_FPS = 1;

    private final Context context;
    private final VideoFrame.Buffer image;

    public ImageCaptureController(Context context, VideoFrame.Buffer image, int width, int height) {
        super(width, height, DEFAULT_FPS);

        this.context = context;
        this.image = image;
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
        VideoCapturer videoCapturer = new ImageCapturer(context, image);
        return videoCapturer;
        return new ImageCapturer(context, image);
    }
}
