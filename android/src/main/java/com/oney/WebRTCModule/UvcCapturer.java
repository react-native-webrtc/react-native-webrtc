package com.oney.WebRTCModule;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.jiangdg.ausbc.callback.IPreviewDataCallBack;
import com.jiangdg.ausbc.camera.CameraUvcStrategy;
import com.jiangdg.ausbc.camera.bean.CameraRequest;

import org.webrtc.CapturerObserver;
import org.webrtc.NV21Buffer;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoFrame;

public class UvcCapturer implements VideoCapturer {

    private static final String TAG = WebRTCModule.TAG;

    private String deviceId;

    private SurfaceTextureHelper surfaceTextureHelper;
    private CapturerObserver capturerObserver;

    private int UVC_PREVIEW_WIDTH = 1024;
    private int UVC_PREVIEW_HEIGHT = 768;
    private int UVC_PREVIEW_FPS = 30;

    CameraUvcStrategy mUvcStrategy; // From Jiang Dongguo's AUSBC library

    public UvcCapturer(String deviceId, CameraUvcStrategy uvcStrategy) {
        this.deviceId = deviceId.replaceAll("\\D", "");

        Log.d(TAG, "UvcCapturer.UvcCapturer->DeviceID " + this.deviceId);

        try {
            mUvcStrategy = uvcStrategy;
        } catch (Exception e) {
            Log.e(TAG, "UvcCapturer.UvcCapturer ", e);
        }
    }

    @Override
    public void initialize(SurfaceTextureHelper surfaceTextureHelper, Context context, CapturerObserver capturerObserver) {
        this.surfaceTextureHelper = surfaceTextureHelper;
        this.capturerObserver = capturerObserver;
    }

    @Override
    public void startCapture(int width, int height, int fps) {
        if (mUvcStrategy != null) {
            // stop before to free last usb instance
            if (mUvcStrategy.isCameraOpened()) {
                mUvcStrategy.stopPreview();
            }

            UVC_PREVIEW_WIDTH = width;
            UVC_PREVIEW_HEIGHT = height;
            UVC_PREVIEW_FPS = fps;

            mUvcStrategy.addPreviewDataCallBack(new IPreviewDataCallBack() {
                @Override
                public void onPreviewData(@Nullable byte[] bytes, @NonNull DataFormat dataFormat) {
                    NV21Buffer nv21Buffer = new NV21Buffer(bytes, UVC_PREVIEW_WIDTH, UVC_PREVIEW_HEIGHT, null);
                    VideoFrame frame = new VideoFrame(nv21Buffer, 0, System.nanoTime());
                    capturerObserver.onFrameCaptured(frame);
                }
            });

            mUvcStrategy.startPreview(getCameraRequest(), this.surfaceTextureHelper.getSurfaceTexture());
        }
    }

    @Override
    public void stopCapture() throws InterruptedException {
        if (mUvcStrategy != null) {
            if (mUvcStrategy.isCameraOpened()) {
                mUvcStrategy.stopPreview();
            }
        }
    }

    @Override
    public void changeCaptureFormat(int i, int i1, int i2) {

    }

    @Override
    public void dispose() {
        Log.d(TAG, "UvcCapturer.dispose");
        if (mUvcStrategy != null) mUvcStrategy.stopPreview();
    }

    @Override
    public boolean isScreencast() {
        return false;
    }

    private CameraRequest getCameraRequest() {
        return new CameraRequest
                .Builder()
                .setCameraId(this.deviceId)
                // .setFrontCamera(false)
                .setContinuousAFModel(true)
                .setContinuousAutoModel(true)
                .setPreviewWidth(UVC_PREVIEW_WIDTH)
                .setPreviewHeight(UVC_PREVIEW_HEIGHT)
                .create();
    }

}
