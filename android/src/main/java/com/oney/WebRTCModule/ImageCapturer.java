package com.oney.WebRTCModule;

import android.content.Context;
import android.util.Log;
import androidx.annotation.Nullable;
import android.net.Uri;
import java.util.concurrent.Executors;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.lang.Thread;
import java.nio.ByteBuffer;
import android.os.Handler;
import java.net.URL;
import java.io.InputStream;
import java.util.List;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import org.webrtc.JavaI420Buffer;
import org.webrtc.VideoFrame;
import org.webrtc.VideoCapturer;
import org.webrtc.CapturerObserver;
import org.webrtc.SurfaceTextureHelper;

public class ImageCapturer implements VideoCapturer {
    /**
     * The {@link Log} tag with which {@code ImageCaptureController} is to log.
     */
    private static final String TAG = ImageCapturer.class.getSimpleName();
    private static final long NSEC_PER_SEC = 1_000_000_000L;
    private static final long BURST_INTERVAL = 33L;
    private static final long STEADY_INTERVAL = 1500L;
    private static final long BURST_DURATION = 3L * NSEC_PER_SEC;

    private static final String RESOURCE_SCHEME = "res";
    private static final String ANDROID_RESOURCE_SCHEME = "android.resource";
    private static final String HTTP_SCHEME = "http";
    private static final int INVALID_RESOURCE_ID = 0;

    private final Context context;
    @Nullable private CapturerObserver capturerObserver;
    private boolean isDisposed;
    private boolean isActive;
    private long startTimeStampNs;
    private boolean isBursting;
    private final VideoFrame.Buffer image;
    private Timer timer;
    @Nullable private Handler threadHandler;

    public ImageCapturer(final Context context, final VideoFrame.Buffer image) {
        this.context = context;
        this.image = image;
        this.isDisposed = false;
        this.isActive = false;
        this.startTimeStampNs = -1;
        this.isBursting = false;
        this.timer = new Timer();
    }

    @Override
    public synchronized void initialize(SurfaceTextureHelper surfaceTextureHelper, Context applicationContext,
        CapturerObserver capturerObserver) {
        if (isDisposed) {
            return;
        }
        this.capturerObserver = capturerObserver;
        threadHandler = surfaceTextureHelper.getHandler();
    }

    @Override
    public synchronized void startCapture(int width, int height, int framerate) {
        Log.d(TAG, "startCapture");
        isActive = true;
        startRenderLoop();
        if (capturerObserver != null) {
            capturerObserver.onCapturerStarted(true);
        }
    }

    @Override
    public synchronized void stopCapture() throws InterruptedException {
        Log.d(TAG, "stopCapture");
        isActive = false;
        timer.cancel();
        if (capturerObserver != null) {
            capturerObserver.onCapturerStopped();
        }
    }

    @Override
    public void changeCaptureFormat(int width, int height, int framerate) {
        // Empty on purpose
    }

    @Override
    public synchronized void dispose() {
        Log.d(TAG, "dispose");
        isDisposed = true;
        isActive = false;
        timer.cancel();
        if (capturerObserver != null) {
            capturerObserver.onCapturerStopped();
        }
        if (image != null) {
            image.release();
        }
    }

    @Override
    public boolean isScreencast() {
       return false;
    }

    private synchronized boolean startTimer(long interval) {
        Log.d(TAG, "startTimer");
        timer.cancel();
        timer = new Timer();
        try {
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    renderOnHandler();
                }
            }, interval, interval);
            return true;
        } catch (Exception e) {
            Log.w(TAG, "could not start timer" + e.toString());
            return false;
        }
    }

    private synchronized void startRenderLoop() {
        Log.d(TAG, "startRenderLoop");
        if (isDisposed || !isActive) {
            return;
        }
        isBursting = true;
        startTimeStampNs = -1;
        if (startTimer(BURST_INTERVAL)) {
            render();
        }
    }

    private void renderOnHandler() {
        if (Thread.currentThread() != threadHandler.getLooper().getThread()) {
            threadHandler.post(() -> {
                render();
            });
        } else {
            render();
        }
    }

    private void render() {
        Log.d(TAG, "render");
        if (isDisposed || !isActive) {
            return;
        }

        long currentTimeStampNs = System.nanoTime();
        if (startTimeStampNs < 0) {
            startTimeStampNs = currentTimeStampNs;
        }
        long frameTimeStampNs = currentTimeStampNs - startTimeStampNs;
        if (isBursting && frameTimeStampNs >= BURST_DURATION) {
            isBursting = false;
            startTimer(STEADY_INTERVAL);
        }

        if (capturerObserver != null) {
            VideoFrame frame = new VideoFrame(image, 0, frameTimeStampNs);
            capturerObserver.onFrameCaptured(frame);
        }
    }
}
