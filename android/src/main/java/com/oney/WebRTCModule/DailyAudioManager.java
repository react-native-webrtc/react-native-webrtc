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

import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.oney.WebRTCModule.DailyWebRTCDevicesManager.AudioDeviceType;

public class DailyAudioManager implements AudioManager.OnAudioFocusChangeListener {
    static final String TAG = DailyAudioManager.class.getCanonicalName();

    public enum Mode {
        IDLE,
        VIDEO_CALL,
        VOICE_CALL,
        USER_SPECIFIED_ROUTE
    }

    private DailyWebRTCDevicesManager dailyWebRTCDevicesManager;
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

    public DailyAudioManager(ReactApplicationContext reactContext, Mode initialMode, DailyWebRTCDevicesManager dailyWebRTCDevicesManager) {
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
                Log.d(TAG, "onHostPause");
            }

            @Override
            public void onHostDestroy() {
                Log.d(TAG, "onHostDestroy");
                executor.execute(() -> {
                    DailyAudioManager.this.setMode(Mode.IDLE);
                });
            }
        });
        this.dailyWebRTCDevicesManager = dailyWebRTCDevicesManager;
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

    private boolean isInCall(){
        int currentAudioMode = this.audioManager.getMode();
        boolean inCall = (AudioManager.MODE_IN_CALL == currentAudioMode || AudioManager.MODE_RINGTONE == currentAudioMode);
        return inCall;
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
                    Log.d(TAG, "onAudioFocusChange: LOSS " + focusChange);
                    // Before Android 12, we are receiving audio focus loss event even if we are not in a call
                    // So we are going to check if we are or not in a call before we emit this event to daily-js,
                    // so It can mute the remote participants and not influentiate in the call
                    if(this.isInCall()){
                        sendAudioFocusChangeEvent(false);
                    }
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
            case USER_SPECIFIED_ROUTE:
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
        if (previousMode == Mode.VIDEO_CALL || previousMode == Mode.VOICE_CALL || previousMode == Mode.USER_SPECIFIED_ROUTE) {
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
        Log.d(TAG, "abandonAudioFocus");
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
        Log.d(TAG, "configureDevicesForCurrentMode => " + mode);
        if (mode == Mode.USER_SPECIFIED_ROUTE) {
            return; // If the user is manually controlling device selection, don't do anything
        }
        if (mode == Mode.IDLE) {
            this.audioManager.setMode(AudioManager.MODE_NORMAL);
            this.dailyWebRTCDevicesManager.setAudioDevice(AudioDeviceType.WIRED_OR_EARPIECE.toString());
        } else {
            this.audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
            AudioDeviceType preferredAudioDevice = this.getPreferredAudioDevice();
            Log.d(TAG, "configureDevicesForCurrentMode: preferring audio route " + preferredAudioDevice);
            this.dailyWebRTCDevicesManager.setAudioDevice(preferredAudioDevice.toString());
        }
    }

    private AudioDeviceType getPreferredAudioDevice() {
        AudioDeviceInfo[] audioOutputDevices = this.audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS);

        boolean isBluetoothHeadsetPlugged = Arrays.stream(audioOutputDevices).anyMatch(device -> device.getType() == AudioDeviceInfo.TYPE_BLUETOOTH_SCO);
        if (isBluetoothHeadsetPlugged) {
            return AudioDeviceType.BLUETOOTH;
        }

        boolean isWiredHeadsetPlugged = Arrays.stream(audioOutputDevices).anyMatch(
                device -> device.getType() == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                        device.getType() == AudioDeviceInfo.TYPE_WIRED_HEADPHONES
        );
        if (isWiredHeadsetPlugged) {
            return AudioDeviceType.WIRED_OR_EARPIECE;
        }

        return (mode == Mode.VIDEO_CALL) ? AudioDeviceType.SPEAKERPHONE : AudioDeviceType.WIRED_OR_EARPIECE;
    }

    private void sendAudioFocusChangeEvent(boolean hasFocus) {
        WritableMap params = Arguments.createMap();
        params.putBoolean("hasFocus", hasFocus);
        eventEmitter.emit("EventAudioFocusChange", params);
    }
}
