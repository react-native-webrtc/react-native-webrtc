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
    public interface ImageEventsHandler {
        void onLoaded(int width, int height);
    }

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

    public ImageCapturer(Context context, Uri asset, FileEventsHandler eventsHandler) {
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
        try {
            Executors.newSingleThreadExecutor().execute(() -> {
                Log.d(TAG, "executed fetch thread");

                InputStream assetStream = getAssetStream();
                if (assetStream == null || isDisposed) {
                    return;
                }
                Bitmap bitmap = bitmapFromStream(assetStream);
                if (bitmap == null) {
                    return;
                }
                int height = bitmap.getHeight();
                int width = bitmap.getWidth();
                frameBuffer = bufferFromBitmap(bitmap);
                bitmap.recycle();
                if (frameBuffer == null) {
                    return;
                }
                if (eventsHandler != null) {
                    eventsHandler.onLoaded(width, height);
                }
                maybeStartAfterGettingBuffer();
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

    private synchronized void maybeStartAfterGettingBuffer() {
        if (isDisposed) {
            if (frameBuffer != null) {
                frameBuffer.release();
            }
        } else {
            startRenderLoop();
        }
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

    private int getAssetId() {
        List<String> pieces = asset.getPathSegments();
        if (pieces.size() == 1) {
            try {
                int id = Integer.parseInt(pieces.get(0));
                return id;
            }
            catch (NumberFormatException e) {
                Log.d(TAG, "could not get rosource id from (" + asset.toString() + ")");
            }
        } else if (pieces.size() == 2) {
            return context.getResources().getIdentifier(pieces.get(0), pieces.get(1), context.getPackageName());
        }
        return INVALID_RESOURCE_ID;
    }

    private InputStream getResourceStream(int id) {
        try {
            return context.getResources().openRawResource(id);
        } catch (Exception e) {
            return null;
        }
    }

    private InputStream getNetworkStream() {
        try {
            URL url = new URL(asset.toString());
            return url.openStream();
        } catch (Exception e) {
            return null;
        }
    }

    private InputStream getAssetStream() {
        String scheme = asset.getScheme();
        if (scheme == null) {
            return null;
        }
        scheme = scheme.toLowerCase();
        if (scheme.startsWith(HTTP_SCHEME)) {
            return getNetworkStream();
        }
        if (scheme.equals(RESOURCE_SCHEME) || scheme.equals(ANDROID_RESOURCE_SCHEME)) {
            int id = getAssetId();
            if (id != INVALID_RESOURCE_ID) {
                return getResourceStream(id);
            }
        }
        return null;
    }

    private Bitmap bitmapFromStream(InputStream stream) {
        try {
            return BitmapFactory.decodeStream(stream);
        } catch (Exception e) {
            return null;
        }
    }

    private VideoFrame.Buffer bufferFromBitmap(Bitmap bitmap) {
        try {
            int height = bitmap.getHeight();
            int width = bitmap.getWidth();

            byte[] nv21 = getNV21(width, height, bitmap);
            byte[] i420 = nv21ToI420(nv21, width, height);

            int yStride = width;
            int uvStride = (width + 1) / 2;
            int uvHeight = (height + 1) / 2;
            ByteBuffer yBuffer = ByteBuffer.wrap(i420, 0, width * height);
            ByteBuffer uBuffer = ByteBuffer.wrap(i420, width * height, uvStride * uvHeight);
            ByteBuffer vBuffer = ByteBuffer.wrap(i420, width * height + uvStride * uvHeight, uvStride * uvHeight);
            return JavaI420Buffer.wrap(
                    width, height,
                    yBuffer, yStride,
                    uBuffer, uvStride,
                    vBuffer, uvStride,
                    null
            );
        } catch (Exception e) {
            return null;
        }
    }

    private byte[] getNV21(int inputWidth, int inputHeight, Bitmap bitmap) {
        int[] argb = new int[inputWidth * inputHeight];
        bitmap.getPixels(argb, 0, inputWidth, 0, 0, inputWidth, inputHeight);
        byte[] yuv = new byte[inputWidth * inputHeight * 3 / 2];
        encodeYUV420SP(yuv, argb, inputWidth, inputHeight);
        return yuv;
    }

    private void encodeYUV420SP(byte[] yuv420sp, int[] argb, int width, int height) {
        int frameSize = width * height;
        int yIndex = 0;
        int uvIndex = frameSize;

        int index = 0;
        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {
                int r = (argb[index] & 0xFF0000) >> 16;
                int g = (argb[index] & 0x00FF00) >> 8;
                int b = (argb[index] & 0x0000FF) >> 0;

                int y = (66 * r + 129 * g + 25 * b + 128 >> 8) + 16;
                int u = (-38 * r - 74 * g + 112 * b + 128 >> 8) + 128;
                int v = (112 * r - 94 * g - 18 * b + 128 >> 8) + 128;

                yuv420sp[yIndex++] = (byte) (y < 0 ? 0 : (y > 255 ? 255 : y));
                if (j % 2 == 0 && i % 2 == 0) {
                    yuv420sp[uvIndex++] = (byte) (v < 0 ? 0 : (v > 255 ? 255 : v));
                    yuv420sp[uvIndex++] = (byte) (u < 0 ? 0 : (u > 255 ? 255 : u));
                }
                index++;
            }
        }
    }

    private byte[] nv21ToI420(byte[] nv21, int width, int height) {
        byte[] i420 = new byte[nv21.length];
        int ySize = width * height;
        int uvSize = ((width + 1) / 2) * ((height + 1) / 2);

        System.arraycopy(nv21, 0, i420, 0, ySize);

        int nvIndex = ySize;
        int uIndex = ySize;
        int vIndex = ySize + uvSize;
        for (int i = 0; i < uvSize; i++) {
            i420[vIndex + i] = nv21[nvIndex++];
            i420[uIndex + i] = nv21[nvIndex++];
        }
        return i420;
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
