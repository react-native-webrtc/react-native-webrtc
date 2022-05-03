package com.oney.WebRTCModule;

import android.content.Context;
import android.media.AudioDeviceCallback;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.util.Log;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;

import org.webrtc.Camera1Enumerator;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraEnumerator;

import java.util.Arrays;

public class DailyWebRTCDevicesManager {

    static final String TAG = DailyWebRTCDevicesManager.class.getCanonicalName();
    static final String ON_DEVICE_CHANGE_EVENT = "mediaDevicesOnDeviceChange";

    public enum DeviceKind {
        VIDEO_INPUT("videoinput"),
        AUDIO("audio");

        private String kind;

        DeviceKind(String kind) {
            this.kind = kind;
        }

        public String getKind() {
            return this.kind;
        }
    }

    public enum AudioDeviceType {
        BLUETOOTH,
        SPEAKERPHONE,
        WIRED_OR_EARPIECE
    }

    private AudioDeviceCallback audioDeviceCallback = new AudioDeviceCallback() {
        @Override
        public void onAudioDevicesAdded(AudioDeviceInfo[] addedDevices) {
            DailyWebRTCDevicesManager.this.webRTCModule.sendEvent(ON_DEVICE_CHANGE_EVENT, null);
        }

        @Override
        public void onAudioDevicesRemoved(AudioDeviceInfo[] removedDevices) {
            DailyWebRTCDevicesManager.this.webRTCModule.sendEvent(ON_DEVICE_CHANGE_EVENT, null);
        }
    };

    private final CameraEnumerator cameraEnumerator;
    private AudioManager audioManager;
    private ReactApplicationContext reactContext;
    private final WebRTCModule webRTCModule;

    public DailyWebRTCDevicesManager(WebRTCModule webRTCModule, ReactApplicationContext reactContext) {
        this.webRTCModule = webRTCModule;
        this.reactContext = reactContext;
        this.audioManager = (AudioManager) reactContext.getSystemService(Context.AUDIO_SERVICE);
        this.cameraEnumerator = this.createCameraEnumerator();
    }

    public void startMediaDevicesEventMonitor() {
        this.audioManager.registerAudioDeviceCallback(audioDeviceCallback, null);
    }

    private CameraEnumerator createCameraEnumerator() {
        boolean camera2supported = false;
        try {
            camera2supported = Camera2Enumerator.isSupported(this.reactContext);
        } catch (Throwable tr) {
            // Some devices will crash here with: Fatal Exception: java.lang.AssertionError: Supported FPS ranges cannot be null.
            // Make sure we don't.
            Log.w(TAG, "Error checking for Camera2 API support.", tr);
        }
        CameraEnumerator cameraEnumerator = null;
        if (camera2supported) {
            Log.d(TAG, "Creating video capturer using Camera2 API.");
            cameraEnumerator = new Camera2Enumerator(this.reactContext);
        } else {
            Log.d(TAG, "Creating video capturer using Camera1 API.");
            cameraEnumerator = new Camera1Enumerator(false);
        }
        return cameraEnumerator;
    }

    ReadableArray enumerateDevices() {
        WritableArray devicesArray = Arguments.createArray();
        this.fillVideoInputDevices(devicesArray);
        this.fillAudioDevices(devicesArray);
        return devicesArray;
    }

    private void fillVideoInputDevices(WritableArray enumerateDevicesArray) {
        String[] devices = cameraEnumerator.getDeviceNames();
        for (int i = 0; i < devices.length; ++i) {
            String deviceName = devices[i];
            boolean isFrontFacing;
            try {
                // This can throw an exception when using the Camera 1 API.
                isFrontFacing = cameraEnumerator.isFrontFacing(deviceName);
            } catch (Exception e) {
                Log.e(TAG, "Failed to check the facing mode of camera");
                continue;
            }
            String label = isFrontFacing ? "Front camera" : "Rear camera";
            String deviceID = isFrontFacing ? "CAMERA_USER" : "CAMERA_ENVIRONMENT";
            WritableMap params = this.createWritableMap(deviceID, label, DeviceKind.VIDEO_INPUT.getKind());
            params.putString("facing", isFrontFacing ? "user" : "environment");
            enumerateDevicesArray.pushMap(params);
        }
    }

    private void fillAudioDevices(WritableArray enumerateDevicesArray) {
        AudioDeviceInfo[] audioOutputDevices = this.audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS);

        boolean isWiredHeadsetPlugged = Arrays.stream(audioOutputDevices).anyMatch(
                device -> device.getType() == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                        device.getType() == AudioDeviceInfo.TYPE_WIRED_HEADPHONES
        );

        WritableMap params = isWiredHeadsetPlugged ?
                this.createWritableMap(AudioDeviceType.WIRED_OR_EARPIECE.toString(), "Wired headset", DeviceKind.AUDIO.getKind()) :
                this.createWritableMap(AudioDeviceType.WIRED_OR_EARPIECE.toString(), "Phone earpiece", DeviceKind.AUDIO.getKind());
        enumerateDevicesArray.pushMap(params);

        //speaker
        params = this.createWritableMap(AudioDeviceType.SPEAKERPHONE.toString(), "Speakerphone", DeviceKind.AUDIO.getKind());
        enumerateDevicesArray.pushMap(params);

        boolean isBluetoothHeadsetPlugged = Arrays.stream(audioOutputDevices).anyMatch(device -> device.getType() == AudioDeviceInfo.TYPE_BLUETOOTH_SCO);
        if (isBluetoothHeadsetPlugged) {
            params = this.createWritableMap(AudioDeviceType.BLUETOOTH.toString(), "Bluetooth", DeviceKind.AUDIO.getKind());
            enumerateDevicesArray.pushMap(params);
        }
    }

    private WritableMap createWritableMap(String deviceId, String label, String kind) {
        WritableMap audioMap = Arguments.createMap();
        audioMap.putString("deviceId", deviceId);
        audioMap.putString("groupId", "");
        audioMap.putString("label", label);
        audioMap.putString("kind", kind);
        return audioMap;
    }

    /**
     * Changes selection of the currently active audio device.
     */
    public void setAudioDevice(String deviceId) {
        Log.d(TAG, "setAudioDevice(audioDeviceType=" + deviceId + ")");
        AudioDeviceType audioRoute = AudioDeviceType.valueOf(deviceId);
        switch (audioRoute) {
            case SPEAKERPHONE:
                toggleBluetooth(false);
                audioManager.setSpeakerphoneOn(true);
                break;
            //If we have a wired headset plugged, It is not possible we send the audio to the earpiece
            case WIRED_OR_EARPIECE:
                toggleBluetooth(false);
                audioManager.setSpeakerphoneOn(false);
                break;
            case BLUETOOTH:
                audioManager.setSpeakerphoneOn(false);
                toggleBluetooth(true);
                break;
            default:
                Log.e(TAG, "Invalid audio device selection");
                break;
        }
    }

    public String getAudioDevice() {
        if (this.audioManager.isBluetoothScoOn() || this.audioManager.isBluetoothA2dpOn()) {
            return AudioDeviceType.BLUETOOTH.toString();
        } else if (this.audioManager.isSpeakerphoneOn()) {
            return AudioDeviceType.SPEAKERPHONE.toString();
        } else {
            return AudioDeviceType.WIRED_OR_EARPIECE.toString();
        }
    }

    private void toggleBluetooth(boolean on) {
        if (on) {
            audioManager.startBluetoothSco();
            audioManager.setBluetoothScoOn(true);
        } else {
            audioManager.setBluetoothScoOn(false);
            audioManager.stopBluetoothSco();
        }
    }

}
