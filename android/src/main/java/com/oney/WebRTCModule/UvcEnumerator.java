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

    CameraUvcStrategy mUvcStrategy;

    public UvcEnumerator(CameraUvcStrategy cameraUvcStrategy) {
        mUvcStrategy = cameraUvcStrategy;
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

                        params.putString("facing", "environment");
                        params.putString("deviceId", "uvc:" + dev.getDeviceId());
                        params.putString("groupId", "");
                        params.putString("label", dev.getProductName() != null ? dev.getProductName() : dev.getDeviceName());
                        params.putString("kind", "videoinput");

                        Log.d(TAG, "UvcEnumerator.enumerateDevices: " + params);

                        array.pushMap(params);
                    }

                }

            }

        } catch (Throwable tr) {
            Log.d(TAG, "Error on UvcEnumerator.enumerateDevices", tr);
        }

        return array;
    }
}
