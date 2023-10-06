package com.oney.WebRTCModule;

import static android.hardware.usb.UsbConstants.USB_CLASS_MISC;
import static android.hardware.usb.UsbConstants.USB_CLASS_VIDEO;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.jiangdg.usb.USBMonitor;
import com.jiangdg.uvc.UVCCamera;

import java.util.List;
import java.util.Objects;
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

    private final USBMonitor usbMonitor;

    public UVCMonitor(Context context) {
        this.usbMonitor = new USBMonitor(context, new DeviceConnectListener());
    }

    @Nullable
    public UsbDevice getUvcCameraDevice() {
        List<UsbDevice> devices = usbMonitor.getDeviceList()
                .stream()
                .filter(d -> SUPPORTED_USB_CLASSES.contains(d.getDeviceClass()))
                .collect(Collectors.toList());
        if (devices.size() > 1) {
            Log.w(WebRTCModule.TAG, "Found more than one UVC Cameras. Using the one found first.");
        }
        return devices.size() > 0 ? devices.get(0) : null;
    }

    private static class DeviceConnectListener implements USBMonitor.OnDeviceConnectListener {
        @Override
        public void onAttach(UsbDevice device) {}

        @Override
        public void onDetach(UsbDevice device) {}

        @Override
        public void onConnect(UsbDevice device, USBMonitor.UsbControlBlock ctrlBlock, boolean createNew) {}

        @Override
        public void onDisconnect(UsbDevice device, USBMonitor.UsbControlBlock ctrlBlock) {}

        @Override
        public void onCancel(UsbDevice device) {}
    }
}
