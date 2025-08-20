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

import org.webrtc.JavaI420Buffer;
import org.webrtc.VideoFrame;
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
    @Nullable private final FileEventsHandler eventsHandler;
    @Nullable private CapturerObserver capturerObserver;
    private final Uri asset;
    private boolean isDisposed;
    private boolean isActive;
    private long startTimeStampNs;
    private boolean isBursting;
    @Nullable private VideoFrame.Buffer frameBuffer;
    private Timer timer;
    @Nullable private Handler threadHandler;

    public FileCapturer(Context context, Uri asset, FileEventsHandler eventsHandler) {
        this.context = context;
        this.eventsHandler = eventsHandler;
        this.asset = asset;
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
        // fetch buffer
        try {
            Executors.newSingleThreadExecutor().execute(() -> {
                Log.d(TAG, "executed fetch thread");
                try { Thread.sleep(2000); } catch (Exception e) {
                    Log.d(TAG, "uhhh" + e.toString());
                }
                Log.d(TAG, "fetch simulated");
                if (isDisposed) {
                    return;
                }
                frameBuffer = createRedFrameBuffer(100,100);
                if (eventsHandler != null) {
                    eventsHandler.onLoaded(100,100);
                }
                startRenderLoop();
            });
        } catch (Exception e) {
            Log.w(TAG, "could not start asset fetcher");
        }
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
        if (frameBuffer != null) {
            frameBuffer.release();
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
        if (frameBuffer == null || isDisposed || !isActive) {
            return;
        }
        isBursting = true;
        startTimeStampNs = -1;
        if (startTimer(BURST_INTERVAL)) {
            renderOnHandler();
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
        if (frameBuffer == null || isDisposed || !isActive) {
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
            VideoFrame frame = new VideoFrame(frameBuffer, 0, frameTimeStampNs);
            capturerObserver.onFrameCaptured(frame);
        }

    }

    private static VideoFrame.Buffer createRedFrameBuffer(int width, int height) {
        int ySize = width * height;
        int uvStride = (width + 1) / 2;
        int uvHeight = (height + 1) / 2;
        int uvSize = uvStride * uvHeight;

        ByteBuffer yBuffer = ByteBuffer.allocateDirect(ySize);
        ByteBuffer uBuffer = ByteBuffer.allocateDirect(uvSize);
        ByteBuffer vBuffer = ByteBuffer.allocateDirect(uvSize);

        byte[] yData = new byte[ySize];
        byte[] uData = new byte[uvSize];
        byte[] vData = new byte[uvSize];
        for (int i = 0; i < ySize; i++) {
            yData[i] = (byte) 76;
        }
        for (int i = 0; i < uvSize; i++) {
            uData[i] = (byte) 84;
            vData[i] = (byte) 255;
        }

        yBuffer.put(yData).rewind();
        uBuffer.put(uData).rewind();
        vBuffer.put(vData).rewind();

        return JavaI420Buffer.wrap(width, height, yBuffer, width, uBuffer, uvStride, vBuffer, uvStride, () -> {
            Log.d(TAG, "buffer released");
        });
    }
}
