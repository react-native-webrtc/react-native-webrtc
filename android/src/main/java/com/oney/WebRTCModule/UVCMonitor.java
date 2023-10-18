package com.oney.WebRTCModule;

import static android.hardware.usb.UsbConstants.USB_CLASS_MISC;
import static android.hardware.usb.UsbConstants.USB_CLASS_VIDEO;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.util.Log;

import androidx.annotation.Nullable;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class UVCMonitor {

    /**
     * This set of supported USB device classes might not be extensive.
     * <p/>
     * USB class documentation:
     * https://developer.android.com/reference/android/hardware/usb/UsbConstants
     */
    private static final Set<Integer> SUPPORTED_USB_CLASSES = Set.of(USB_CLASS_VIDEO, USB_CLASS_MISC);

    private final UsbManager usbManager;

    public UVCMonitor(Context context) {
        this.usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
    }

    @Nullable
    public UsbDevice getUvcCameraDevice() {
        if (this.usbManager == null) {
            return null;
        }

        List<UsbDevice> devices = usbManager.getDeviceList()
                .values()
                .stream()
                .filter(d -> SUPPORTED_USB_CLASSES.contains(d.getDeviceClass()))
                .collect(Collectors.toList());
        if (devices.size() > 1) {
            Log.w(WebRTCModule.TAG, "Found more than one UVC Cameras. Using the one found first.");
        }
        return devices.size() > 0 ? devices.get(0) : null;
    }
}
