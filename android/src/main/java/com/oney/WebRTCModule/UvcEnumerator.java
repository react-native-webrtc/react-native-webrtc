package com.oney.WebRTCModule;

import android.content.Context;
import android.hardware.Camera;
import android.hardware.usb.UsbDevice;
import android.util.Log;
import android.widget.Toast;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.jiangdg.ausbc.camera.CameraUvcStrategy;

import org.webrtc.SurfaceViewRenderer;

import java.util.List;

public class UvcEnumerator {

    private static final String TAG = WebRTCModule.TAG;

    CameraUvcStrategy mUvcStrategy; // From Jiang Dongguo's AUSBC library

    public UvcEnumerator(CameraUvcStrategy cameraUvcStrategy) {
        try {
            mUvcStrategy = cameraUvcStrategy;
        } catch (Throwable tr) {
            Log.e(TAG, "Error on UvcEnumerator.UvcEnumerator", tr);
        }
    }

    ReadableArray enumerateDevices() {
        WritableArray array = Arguments.createArray();

        try {
            List<UsbDevice> usbDevices = mUvcStrategy.getUsbDeviceList(null);

            if (usbDevices != null) {

                if (!usbDevices.isEmpty()) {

                    for (int i = 0; i < usbDevices.size(); i++) {
                        UsbDevice dev = usbDevices.get(i);

                        WritableMap params = Arguments.createMap();

                        params.putString("facing", "uvc");
                        params.putString("deviceId", "uvc-" + dev.getDeviceId());
                        params.putString("groupId", "");
                        params.putString("label", dev.getProductName() != null ? dev.getProductName() : dev.getDeviceName());
                        params.putString("kind", "videoinput");

                        Log.d(TAG, "UvcEnumerator.enumerateDevices: " + params);

                        array.pushMap(params);
                    }

                } else {
                    Log.d(TAG, "UvcEnumerator.enumerateDevices: usbDevices is empty");
                }

            } else {
                Log.d(TAG, "UvcEnumerator.enumerateDevices: usbDevices is null");
            }

        } catch (Throwable tr) {
            Log.d(TAG, "Error on UvcEnumerator.enumerateDevices", tr);
        }

        return array;
    }
}
