package com.oney.WebRTCModule;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioDeviceCallback;
import android.media.AudioDeviceInfo;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.os.Build;
import android.util.Log;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DailyAudioManager implements AudioManager.OnAudioFocusChangeListener {
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
    private DeviceEventManagerModule.RCTDeviceEventEmitter eventEmitter;
    private Mode mode;
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private AudioFocusRequest audioFocusRequest;

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

    public DailyAudioManager(ReactApplicationContext reactContext, Mode initialMode) {
        reactContext.addLifecycleEventListener(new LifecycleEventListener() {
            @Override
            public void onHostResume() {
                executor.execute(() -> {
                    if (mode == Mode.VIDEO_CALL || mode == Mode.VOICE_CALL) {
                        requestAudioFocus();
                        configureDevicesForCurrentMode();
                        sendAudioFocusChangeEvent(true);
                    }
                });
            }

            @Override
            public void onHostPause() {
            }

            @Override
            public void onHostDestroy() {
            }
        });
        this.audioManager = (AudioManager) reactContext.getSystemService(Context.AUDIO_SERVICE);
        this.eventEmitter = reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class);
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

    @Override
    public void onAudioFocusChange(int focusChange) {
        executor.execute(() -> {
            switch (focusChange) {
                case AudioManager.AUDIOFOCUS_GAIN:
                    Log.d(TAG, "onAudioFocusChange: GAIN");
                    // Ensure devices are configured appropriately, in case they were messed up
                    // while we didn't have focus
                    configureDevicesForCurrentMode();
                    sendAudioFocusChangeEvent(true);
                    break;
                case AudioManager.AUDIOFOCUS_LOSS:
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                    Log.d(TAG, "onAudioFocusChange: LOSS");
                    sendAudioFocusChangeEvent(false);
                    break;
                default:
                    break;
            }
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
        abandonAudioFocus();

        // Configure devices
        configureDevicesForCurrentMode();
    }

    // Assumes that previousMode != this.mode, hence "transition"
    // Does things in the reverse order of transitionOutOfCallMode()
    private void transitionToCurrentCallMode(Mode previousMode) {
        // Already in a call, so all we have to do is configure devices
        if (previousMode == Mode.VIDEO_CALL || previousMode == Mode.VOICE_CALL) {
            configureDevicesForCurrentMode();
        } else {
            // Configure devices
            configureDevicesForCurrentMode();

            // Request audio focus
            requestAudioFocus();

            // Start listening for device changes
            audioManager.registerAudioDeviceCallback(audioDeviceCallback, null);
        }
    }

    private void requestAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AudioAttributes attributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build();
            audioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(attributes)
                    .setAcceptsDelayedFocusGain(true)
                    .setOnAudioFocusChangeListener(this)
                    .build();
            audioManager.requestAudioFocus(audioFocusRequest);
        } else {
            audioManager.requestAudioFocus(this,
                    AudioManager.STREAM_VOICE_CALL,
                    AudioManager.AUDIOFOCUS_GAIN);
        }
    }

    private void abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (audioFocusRequest == null) {
                Log.d(TAG, "abandonAudioFocus: expected audioFocusRequest to exist");
                return;
            }
            audioManager.abandonAudioFocusRequest(audioFocusRequest);
        } else {
            audioManager.abandonAudioFocus(this);
        }
    }

    private void configureDevicesForCurrentMode() {
        if (mode == Mode.IDLE) {
            audioManager.setMode(AudioManager.MODE_NORMAL);
            audioManager.setSpeakerphoneOn(false);
            toggleBluetooth(false);
            audioManager.setMicrophoneMute(true);
        } else {
            audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
            Set<DeviceType> availableDeviceTypes = getAvailableDeviceTypes();
            DeviceType preferredDeviceType = getPreferredDeviceTypeForCurrentMode(availableDeviceTypes);
            Log.d(TAG, "configureDevicesForCurrentMode: preferring device type " + preferredDeviceType);
            audioManager.setSpeakerphoneOn(shouldSpeakerphoneBeOn(preferredDeviceType));
            toggleBluetooth(shouldBluetoothBeOn(preferredDeviceType));
            audioManager.setMicrophoneMute(false);
        }
    }

    private Set<DeviceType> getAvailableDeviceTypes() {
        Set<DeviceType> deviceTypes = new HashSet<DeviceType>();
        AudioDeviceInfo[] devices = audioManager.getDevices(AudioManager.GET_DEVICES_ALL);
        for (AudioDeviceInfo info : devices) {
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
        return deviceTypes;
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
            return DeviceType.EARPIECE;
        }
        return null;
    }

    private boolean shouldSpeakerphoneBeOn(DeviceType preferredDeviceType) {
        return preferredDeviceType == DeviceType.SPEAKER;
    }

    private boolean shouldBluetoothBeOn(DeviceType preferredDeviceType) {
        return preferredDeviceType == DeviceType.BLUETOOTH;
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

    private void sendAudioFocusChangeEvent(boolean hasFocus) {
        WritableMap params = Arguments.createMap();
        params.putBoolean("hasFocus", hasFocus);
        eventEmitter.emit("EventAudioFocusChange", params);
    }
}
