package com.oney.WebRTCModule;

import android.content.Context;
import android.hardware.usb.UsbDevice;

import androidx.annotation.Nullable;

import com.jiangdg.ausbc.camera.CameraUvcStrategy;
import com.jiangdg.ausbc.camera.bean.PreviewSize;
import com.jiangdg.usb.USBMonitor;

import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraEnumerationAndroid;
import org.webrtc.CameraVideoCapturer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


/**
 * Camera2Enumerator with UVC Camera support.
 * <p/>
 * This implementation only adds support for UVC camera devices.
 * Ie. Device's (back and/or front) cameras will use the basic implementation of {@link Camera2Enumerator}.
 */
public class UVCCamera2Enumerator extends Camera2Enumerator  {

    /** UVC camera names will be prefix with this value. Currently, there is no other way to
     * easily distinguish between device's own and external uvc cameras. */
    public static final String UVC_PREFIX = "uvc-camera:";

    private final UVCMonitor uvcMonitor;
    private final Context context;

    public UVCCamera2Enumerator(Context context) {
        super(context);
        this.uvcMonitor = new UVCMonitor(context);
        this.context = context;
    }

    @Override
    public String[] getDeviceNames() {
        ArrayList<String> devicesNames = new ArrayList<>(Arrays.asList(super.getDeviceNames()));
        UsbDevice uvcDevice = this.uvcMonitor.getUvcCameraDevice();
        if (uvcDevice != null) {
            devicesNames.add(UVC_PREFIX + uvcDevice.getDeviceName());
        }

        return devicesNames.toArray(new String[0]);
    }

    @Override
    public boolean isFrontFacing(String deviceName) {
        if (isUvcCamera(deviceName)) {
            return true;
        }

        return super.isFrontFacing(deviceName);
    }

    public boolean isBackFacing(String deviceName) {
        if (isUvcCamera(deviceName)) {
            return false;
        }

        return super.isBackFacing(deviceName);
    }

    @Nullable
    public List<CameraEnumerationAndroid.CaptureFormat> getSupportedFormats(String deviceName) {
        if (isUvcCamera(deviceName)) {
            CameraUvcStrategy cameraUvcStrategy = new CameraUvcStrategy(this.context);
            List<PreviewSize> sizes = cameraUvcStrategy.getAllPreviewSizes(null); // null := Ask all sizes for all aspect ratios.
            if (sizes == null) {
                return null;
            }

            int minFps = 1;
            int maxFps = 30;
            return sizes.stream()
                    .map(size -> new CameraEnumerationAndroid.CaptureFormat(size.getWidth(), size.getHeight(), minFps, maxFps))
                    .collect(Collectors.toList());
        }

        return super.getSupportedFormats(deviceName);
    }

    @Override
    public CameraVideoCapturer createCapturer(String cameraName, CameraVideoCapturer.CameraEventsHandler eventsHandler) {
        if (isUvcCamera(cameraName)) {
            return new UVCVideoCapturer();
        }

        return super.createCapturer(cameraName, eventsHandler);
    }

    public static boolean isUvcCamera(String deviceName) {
        return deviceName != null && deviceName.startsWith(UVC_PREFIX);
    }
}
