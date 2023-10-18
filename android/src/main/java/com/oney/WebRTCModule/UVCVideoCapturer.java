package com.oney.WebRTCModule;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.jiangdg.ausbc.callback.IPreviewDataCallBack;
import com.jiangdg.ausbc.camera.CameraUvcStrategy;
import com.jiangdg.ausbc.camera.bean.CameraRequest;

import org.webrtc.CameraVideoCapturer;
import org.webrtc.CapturerObserver;
import org.webrtc.NV21Buffer;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.VideoFrame;

import java.util.function.Consumer;

public class UVCVideoCapturer implements CameraVideoCapturer {

    private SurfaceTextureHelper surfaceTextureHelper;
    private CapturerObserver capturerObserver;
    private CameraUvcStrategy cameraUvcStrategy;
    private PreviewDataCallback previewDataCallback;
    private boolean isCapturerStarted;

    @Override
    public synchronized void initialize(SurfaceTextureHelper surfaceTextureHelper, Context context, CapturerObserver capturerObserver) {
        this.surfaceTextureHelper = surfaceTextureHelper;
        this.capturerObserver = capturerObserver;
        this.previewDataCallback = new PreviewDataCallback(this.capturerObserver::onFrameCaptured);
        this.cameraUvcStrategy = new CameraUvcStrategy(context);
        this.cameraUvcStrategy.register();
    }

    @Override
    public synchronized void startCapture(int width, int height, int fps) {
        stopCapture();

        Log.i(WebRTCModule.TAG, String.format("Starting uvc capture with %d x %d @ %d", width, height, fps));
        this.capturerObserver.onCapturerStarted(true);
        this.isCapturerStarted = true;
        this.cameraUvcStrategy.addPreviewDataCallBack(this.previewDataCallback);
        this.cameraUvcStrategy.startPreview(createCameraRequest(width, height), this.surfaceTextureHelper.getSurfaceTexture());
    }

    @Override
    public synchronized void stopCapture() {
        Log.d(WebRTCModule.TAG, "Stopping uvc capture");
        this.cameraUvcStrategy.removePreviewDataCallBack(this.previewDataCallback);
        this.cameraUvcStrategy.stopPreview();
        if (this.isCapturerStarted) {
            this.capturerObserver.onCapturerStopped();
            this.isCapturerStarted = false;
        }
    }

    @Override
    public synchronized void changeCaptureFormat(int width, int height, int fps) {
        Log.d(WebRTCModule.TAG, String.format("Capture format changed to %d x %d @ %d", width, height, fps));
        this.stopCapture();
        this.startCapture(width, height, fps);
    }

    @Override
    public synchronized void dispose() {
        Log.d(WebRTCModule.TAG, "Disposing uvc capturer");
        stopCapture();
        this.cameraUvcStrategy.unRegister();
    }

    @Override
    public boolean isScreencast() {
        return false;
    }

    @Override
    public void switchCamera(CameraSwitchHandler cameraSwitchHandler) {
        cameraSwitchHandler.onCameraSwitchError("Not supported yet with uvc");
    }

    @Override
    public void switchCamera(CameraSwitchHandler cameraSwitchHandler, String s) {
        cameraSwitchHandler.onCameraSwitchError("Not supported yet with uvc");
    }

    private CameraRequest createCameraRequest(int width, int height) {
        return new CameraRequest
                .Builder()
                .setPreviewWidth(width)
                .setPreviewHeight(height)
                .create();
    }

    private static class PreviewDataCallback implements IPreviewDataCallBack {

        private final Consumer<VideoFrame> videoFrameConsumer;

        public PreviewDataCallback(Consumer<VideoFrame> videoFrameConsumer) {
            this.videoFrameConsumer = videoFrameConsumer;
        }

        @Override
        public void onPreviewData(@Nullable byte[] bytes, int width, int height, @NonNull DataFormat dataFormat) {
            if (DataFormat.NV21.equals(dataFormat)) {
                NV21Buffer nv21Buffer = new NV21Buffer(bytes, width, height, null);
                VideoFrame frame = new VideoFrame(nv21Buffer, 0, System.nanoTime());
                videoFrameConsumer.accept(frame);
                frame.release();
            } else {
                Log.e(WebRTCModule.TAG, String.format("Support for data format '%s' has not been implemented.", dataFormat));
            }
        }
    }
}
