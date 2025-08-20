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
    private static final long NSEC_PER_SEC = 1_000_000_000L;
    private static final long BURST_INTERVAL = 33L;
    private static final long STEADY_INTERVAL = 1500L;
    private static final long BURST_DURATION = 3L * NSEC_PER_SEC;

    private final Context context;
    private final FileEventsHandler eventsHandler;
    @Nullable private CapturerObserver capturerObserver;
    private final Uri asset;
    private boolean isDisposed;
    private boolean isActive;
    private long startTimeStampNs;
    private boolean isBursting;
    private boolean buffer;

    private Timer timer;

    public FileCapturer(Context context, Uri asset, FileEventsHandler eventsHandler) {
        this.context = context;
        this.eventsHandler = eventsHandler;
        this.asset = asset;
        this.isDisposed = false;
        this.isActive = false;
        this.startTimeStampNs = -1;
        this.isBursting = false;
        this.buffer = false;
        this.timer = new Timer();
    }

    @Override
    public synchronized void initialize(SurfaceTextureHelper surfaceTextureHelper, Context applicationContext,
        CapturerObserver capturerObserver) {
        if (this.isDisposed) {
            return;
        }
        this.capturerObserver = capturerObserver;
        // fetch buffer
        try {
            Executors.newSingleThreadExecutor().execute(() -> {
                Log.d(TAG, "executed fetch thread");
                try { Thread.sleep(2000); } catch (Exception e) {
                    Log.d(TAG, "uhhh" + e.toString());
                }
                Log.d(TAG, "fetch simulated");
                if (this.isDisposed) {
                    return;
                }
                this.buffer = true;
                startRenderLoop(); // run on exedcutor?
            });
        } catch (Exception e) {
            Log.w(TAG, "could not start asset fetcher");
        }
    }

    @Override
    public synchronized void startCapture(int width, int height, int framerate) {
        Log.d(TAG, "startCapture");
        this.isActive = true;
        startRenderLoop();
        this.capturerObserver.onCapturerStarted(true); // put after actually starting stuff
    }

    @Override
    public synchronized void stopCapture() throws InterruptedException {
        Log.d(TAG, "stopCapture");
        this.isActive = false;
        timer.cancel();
        this.capturerObserver.onCapturerStopped();
    }

    @Override
    public void changeCaptureFormat(int width, int height, int framerate) {
        // Empty on purpose
    }

    @Override
    public synchronized void dispose() {
        Log.d(TAG, "dispose");
        this.isDisposed = true;
        this.isActive = false;
        timer.cancel();
        this.capturerObserver.onCapturerStopped();
    }

    @Override
    public boolean isScreencast() {
       return false;
    }

    private synchronized boolean startTimer(long interval) {
        Log.d(TAG, "startTimer");
        timer.cancel();
        this.timer = new Timer();
        try {
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    render();
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
        if (!this.buffer || this.isDisposed || !this.isActive) {
            return;
        }
        isBursting = true;
        startTimeStampNs = -1;
        if (startTimer(BURST_INTERVAL)) {
            render();
        }
    }

    private void render() {
        Log.d(TAG, "render");
        if (!this.buffer || this.isDisposed || !this.isActive) {
            return;
        }

        long currentTimeStampNs = System.nanoTime();
        if (startTimeStampNs < 0) {
            startTimeStampNs = currentTimeStampNs;
        }
        long frameTimeStampNs = currentTimeStampNs - startTimeStampNs;
        if (isBursting && frameTimeStampNs >= BURST_DURATION) {
            this.isBursting = false;
            startTimer(STEADY_INTERVAL);
        }

    }
}
