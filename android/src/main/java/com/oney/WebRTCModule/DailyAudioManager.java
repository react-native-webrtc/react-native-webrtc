package com.oney.WebRTCModule;

import android.media.AudioDeviceCallback;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.util.Log;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DailyAudioManager {
    static final String TAG = DailyAudioManager.class.getCanonicalName();

    public enum Mode {
        NOT_IN_CALL,
        IN_CALL
    }

    private enum DeviceType {
        BLUETOOTH,
        HEADSET,
        SPEAKER
    }

    private AudioManager audioManager;
    private Mode mode;
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    private AudioDeviceCallback audioDeviceCallback = new AudioDeviceCallback() {
        @Override
        public void onAudioDevicesAdded(AudioDeviceInfo[] addedDevices) {
            executor.execute(() -> {
                Log.d(TAG, "onAudioDevicesAdded");
                configureAudioForCurrentMode();
            });
        }

        @Override
        public void onAudioDevicesRemoved(AudioDeviceInfo[] removedDevices) {
            executor.execute(() -> {
                Log.d(TAG, "onAudioDevicesRemoved");
                configureAudioForCurrentMode();
            });
        }
    };

    public DailyAudioManager(AudioManager audioManager, Mode initialMode) {
        this.audioManager = audioManager;
        this.mode = initialMode;
        executor.execute(() -> configureAudioForCurrentMode());
    }

    public void setMode(Mode mode) {
        executor.execute(() -> {
            if (mode == this.mode) {
                return;
            }
            this.mode = mode;
            configureAudioForCurrentMode();
        });
    }

    ///
    /// Private methods should only be called in executor thread
    ///

    private void configureAudioForCurrentMode() {
        switch (mode) {
            case NOT_IN_CALL:
                Log.d(TAG, "configureAudioForCurrentMode: NOT in call");
                configureNotInCallMode();
                break;
            case IN_CALL:
                Log.d(TAG, "configureAudioForCurrentMode: in call");
                configureInCallMode();
                break;
        }
    }

    // Does things in the reverse order of switchToInCallMode()
    private void configureNotInCallMode() {
        // Stop listening for device changes
        audioManager.unregisterAudioDeviceCallback(audioDeviceCallback);

        // Give up audio focus
        // TODO: do this soon, to play nicely with other apps

        // Configure devices
        configureDevicesForCurrentMode();

        // Set audio mode
        audioManager.setMode(AudioManager.MODE_NORMAL);
    }

    // Does things in the reverse order of switchToNotInCallMode()
    private void configureInCallMode() {
        // Set audio mode
        audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);

        // Configure devices
        configureDevicesForCurrentMode();

        // Request audio focus
        // TODO: do this soon, to play nicely with other apps

        // Start listening for device changes
        audioManager.registerAudioDeviceCallback(audioDeviceCallback, null);
    }

    private void configureDevicesForCurrentMode() {
        if (mode == Mode.NOT_IN_CALL) {
            audioManager.setSpeakerphoneOn(false);
            toggleBluetooth(false);
        }
        else {
            Set<DeviceType> availableDeviceTypes = getAvailableDeviceTypes();
            DeviceType preferredDeviceType = getPreferredDeviceType(availableDeviceTypes);
            Log.d(TAG, "configureDevicesForCurrentMode: preferring device type " + preferredDeviceType);
            audioManager.setSpeakerphoneOn(shouldSpeakerphoneBeOn(preferredDeviceType));
            toggleBluetooth(shouldBluetoothBeOn(preferredDeviceType));
        }
    }

    private Set<DeviceType> getAvailableDeviceTypes() {
        Set<DeviceType> deviceTypes = new HashSet<DeviceType>();
        AudioDeviceInfo[] devices = audioManager.getDevices(AudioManager.GET_DEVICES_ALL);
        for (AudioDeviceInfo info: devices) {
            switch (info.getType()) {
                case AudioDeviceInfo.TYPE_BLUETOOTH_SCO:
                    deviceTypes.add(DeviceType.BLUETOOTH);
                    break;
                case AudioDeviceInfo.TYPE_BUILTIN_SPEAKER:
                    deviceTypes.add(DeviceType.SPEAKER);
                    break;
                case AudioDeviceInfo.TYPE_WIRED_HEADPHONES:
                case AudioDeviceInfo.TYPE_WIRED_HEADSET:
                case AudioDeviceInfo.TYPE_USB_HEADSET: // TODO: can this be used in API level < 26?
                    deviceTypes.add(DeviceType.HEADSET);
                    break;
            }
        }
        return  deviceTypes;
    }

    private DeviceType getPreferredDeviceType(Set<DeviceType> availableDeviceTypes) {
        if (availableDeviceTypes.contains(DeviceType.BLUETOOTH)) {
            return DeviceType.BLUETOOTH;
        }
        if (availableDeviceTypes.contains(DeviceType.HEADSET)) {
            return DeviceType.HEADSET;
        }
        if (availableDeviceTypes.contains(DeviceType.SPEAKER)) {
            return DeviceType.SPEAKER;
        }
        return null;
    }

    private boolean shouldSpeakerphoneBeOn(DeviceType preferredDeviceType) {
        return preferredDeviceType == DeviceType.SPEAKER;
    }

    private  boolean shouldBluetoothBeOn(DeviceType preferredDeviceType) {
        return preferredDeviceType == DeviceType.BLUETOOTH;
    }

    private void toggleBluetooth(boolean on) {
        if (on) {
            audioManager.startBluetoothSco();
            audioManager.setBluetoothScoOn(true);
        }
        else {
            audioManager.setBluetoothScoOn(false);
            audioManager.stopBluetoothSco();
        }
    }
}
