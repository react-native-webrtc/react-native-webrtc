package com.oney.WebRTCModule;

import android.content.Context;
import android.util.Log;
import androidx.annotation.Nullable;
import android.net.Uri;

import org.webrtc.VideoCapturer;
import org.webrtc.CapturerObserver;
import org.webrtc.SurfaceTextureHelper;

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
    private boolean isDisposed;
    private boolean isActive;

    public FileCapturer(Context context, Uri asset, FileEventsHandler eventsHandler) {
        this.context = context;
        this.eventsHandler = eventsHandler;
        this.asset = asset;
        this.isDisposed = false;
        this.isActive = false;
    }

    @Override
    public synchronized void initialize(SurfaceTextureHelper surfaceTextureHelper, Context applicationContext,
        CapturerObserver capturerObserver) {
        if (this.isDisposed) {
            return;
        }
        this.capturerObserver = capturerObserver;
        // fetch buffer
    }

    @Override
    public synchronized void startCapture(int width, int height, int framerate) {
        this.isActive = true;
        this.capturerObserver.onCapturerStarted(true); // put after actually starting stuff
    }

    @Override
    public synchronized void stopCapture() throws InterruptedException {
        this.isActive = false;
        this.capturerObserver.onCapturerStopped();
    }

    @Override
    public void changeCaptureFormat(int width, int height, int framerate) {
        // Empty on purpose
    }

    @Override
    public synchronized void dispose() {
        this.isDisposed = true;
        this.isActive = false;
    }

    @Override
    public boolean isScreencast() {
        return false;
    }
}
