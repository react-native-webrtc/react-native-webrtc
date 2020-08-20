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
        IDLE,
        VIDEO_CALL,
        VOICE_CALL
    }

    private enum DeviceType {
        BLUETOOTH,
        HEADSET,
        SPEAKER,
        EARPIECE
    }

    private AudioManager audioManager;
    private Mode mode;
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    private AudioDeviceCallback audioDeviceCallback = new AudioDeviceCallback() {
        @Override
        public void onAudioDevicesAdded(AudioDeviceInfo[] addedDevices) {
            executor.execute(() -> {
                Log.d(TAG, "onAudioDevicesAdded");
                configureDevicesForCurrentMode();
            });
        }

        @Override
        public void onAudioDevicesRemoved(AudioDeviceInfo[] removedDevices) {
            executor.execute(() -> {
                Log.d(TAG, "onAudioDevicesRemoved");
                configureDevicesForCurrentMode();
            });
        }
    };

    public DailyAudioManager(AudioManager audioManager, Mode initialMode) {
        this.audioManager = audioManager;
        this.mode = initialMode;
        executor.execute(() -> transitionToCurrentMode(null));
    }

    public void setMode(Mode mode) {
        executor.execute(() -> {
            if (mode == this.mode) {
                return;
            }
            Mode previousMode = this.mode;
            this.mode = mode;
            transitionToCurrentMode(previousMode);
        });
    }

    ///
    /// Private methods should only be called in executor thread
    ///

    // Assumes that previousMode != this.mode, hence "transition"
    private void transitionToCurrentMode(Mode previousMode) {
        Log.d(TAG, "transitionToCurrentMode: " + mode);
        switch (mode) {
            case IDLE:
                transitionOutOfCallMode();
                break;
            case VIDEO_CALL:
            case VOICE_CALL:
                transitionToCurrentCallMode(previousMode);
                break;
        }
    }

    // Assumes that previousMode != this.mode, hence "transition"
    // Does things in the reverse order of transitionToCurrentInCallMode()
    private void transitionOutOfCallMode() {
        // Stop listening for device changes
        audioManager.unregisterAudioDeviceCallback(audioDeviceCallback);

        // Give up audio focus
        // TODO: do this soon, to play nicely with other apps

        // Configure devices
        configureDevicesForCurrentMode();

        // Set audio mode
        audioManager.setMode(AudioManager.MODE_NORMAL);
    }

    // Assumes that previousMode != this.mode, hence "transition"
    // Does things in the reverse order of transitionOutOfCallMode()
    private void transitionToCurrentCallMode(Mode previousMode) {
        // Already in a call, so all we have to do is configure devices
        if (previousMode == Mode.VIDEO_CALL || previousMode == Mode.VOICE_CALL) {
            configureDevicesForCurrentMode();
        }
        else {
            // Set audio mode
            audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);

            // Configure devices
            configureDevicesForCurrentMode();

            // Request audio focus
            // TODO: do this soon, to play nicely with other apps

            // Start listening for device changes
            audioManager.registerAudioDeviceCallback(audioDeviceCallback, null);
        }
    }

    private void configureDevicesForCurrentMode() {
        if (mode == Mode.IDLE) {
            audioManager.setSpeakerphoneOn(false);
            toggleBluetooth(false);
        }
        else {
            Set<DeviceType> availableDeviceTypes = getAvailableDeviceTypes();
            DeviceType preferredDeviceType = getPreferredDeviceTypeForCurrentMode(availableDeviceTypes);
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
                case AudioDeviceInfo.TYPE_BUILTIN_EARPIECE:
                    deviceTypes.add(DeviceType.EARPIECE);
                    break;
                case AudioDeviceInfo.TYPE_WIRED_HEADPHONES:
                case AudioDeviceInfo.TYPE_WIRED_HEADSET:
                case 22: // AudioDeviceInfo.TYPE_USB_HEADSET, which is defined only in API level 26
                    deviceTypes.add(DeviceType.HEADSET);
                    break;
            }
        }
        return  deviceTypes;
    }

    private DeviceType getPreferredDeviceTypeForCurrentMode(Set<DeviceType> availableDeviceTypes) {
        if (availableDeviceTypes.contains(DeviceType.BLUETOOTH)) {
            return DeviceType.BLUETOOTH;
        }
        if (availableDeviceTypes.contains(DeviceType.HEADSET)) {
            return DeviceType.HEADSET;
        }
        if (mode == Mode.VIDEO_CALL && availableDeviceTypes.contains(DeviceType.SPEAKER)) {
            return DeviceType.SPEAKER;
        }
        if (mode == Mode.VOICE_CALL && availableDeviceTypes.contains(DeviceType.EARPIECE)) {
            return  DeviceType.EARPIECE;
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
