package com.oney.WebRTCModule;

import android.content.Context;
import android.util.Log;
import androidx.annotation.Nullable;
import android.net.Uri;

import org.webrtc.VideoCapturer;
import org.webrtc.CapturerObserver;

public class FileCapturer implements VideoCapturer {
    public interface FileEventsHandler {
        void onLoaded(int width, int height);
    }

    /**
     * The {@link Log} tag with which {@code FileCaptureController} is to log.
     */
    private static final String TAG = FileCapturer.class.getSimpleName();


    private final Context context;
    private final FileEventsHandler eventsHandler;
    @Nullable private CapturerObserver capturerObserver;
    private final Uri asset;

    public FileCapturer(Context context, Uri asset, FileEventsHandler eventsHandler) {
        this.context = context;
        this.eventsHandler = eventsHandler;
        this.asset = asset;
    }

    @Override
    public void initialize(SurfaceTextureHelper surfaceTextureHelper, Context applicationContext,
        CapturerObserver capturerObserver) {
        this.capturerObserver = capturerObserver;
    }

    @Override
    public void startCapture(int width, int height, int framerate) {
        Log.d(TAG, "started");
    }

    @Override
    public void stopCapture() throws InterruptedException {
        Log.d(TAG, "stopped");
    }

    @Override
    public void changeCaptureFormat(int width, int height, int framerate) {
        // Empty on purpose
    }

    @Override
    public void dispose() {
        Log.d(TAG, "disposed");
    }

    @Override
    public boolean isScreencast() {
        return false;
    }
}
