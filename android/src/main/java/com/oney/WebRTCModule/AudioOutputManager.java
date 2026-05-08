package com.oney.WebRTCModule;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioDeviceCallback;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.RequiresApi;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;

import java.util.List;

public class AudioOutputManager {
    private static final long ROUTE_CHANGE_TIMEOUT_MS = 3000;

    private final ReactApplicationContext reactContext;
    private final AudioManager audioManager;
    private final WebRTCModule webRTCModule;
    private AudioDeviceCallback audioDeviceCallback;
    private AudioManager.OnCommunicationDeviceChangedListener communicationDeviceChangedListener;
    private BroadcastReceiver scoReceiver;
    private boolean isObserving = false;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    // Guarded by `this`. Single in-flight selection — a new request supersedes any prior one.
    private PendingSelect pending;

    private static final class PendingSelect {
        final Promise promise;
        final int targetDeviceId;
        final int targetType;
        final Runnable timeoutTask;

        PendingSelect(Promise promise, int targetDeviceId, int targetType, Runnable timeoutTask) {
            this.promise = promise;
            this.targetDeviceId = targetDeviceId;
            this.targetType = targetType;
            this.timeoutTask = timeoutTask;
        }
    }

    public AudioOutputManager(WebRTCModule module, ReactApplicationContext context) {
        this.webRTCModule = module;
        this.reactContext = context;
        this.audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
    }

    private static String audioDeviceNativeType(int type) {
        switch (type) {
            case AudioDeviceInfo.TYPE_BUILTIN_EARPIECE:
                return "builtInEarpiece";
            case AudioDeviceInfo.TYPE_BUILTIN_SPEAKER:
                return "builtInSpeaker";
            case AudioDeviceInfo.TYPE_WIRED_HEADSET:
                return "wiredHeadset";
            case AudioDeviceInfo.TYPE_WIRED_HEADPHONES:
                return "wiredHeadphones";
            case AudioDeviceInfo.TYPE_BLUETOOTH_SCO:
                return "bluetoothSCO";
            case AudioDeviceInfo.TYPE_BLUETOOTH_A2DP:
                return "bluetoothA2DP";
            case AudioDeviceInfo.TYPE_HDMI:
                return "HDMI";
            case AudioDeviceInfo.TYPE_USB_DEVICE:
                return "usbDevice";
            case AudioDeviceInfo.TYPE_USB_HEADSET:
                return "usbHeadset";
            case AudioDeviceInfo.TYPE_USB_ACCESSORY:
                return "usbAccessory";
            case AudioDeviceInfo.TYPE_HEARING_AID:
                return "hearingAid";
            default:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (type == AudioDeviceInfo.TYPE_BLE_HEADSET) return "bleHeadset";
                    if (type == AudioDeviceInfo.TYPE_BLE_SPEAKER) return "bleSpeaker";
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (type == AudioDeviceInfo.TYPE_BLE_BROADCAST) return "bleBroadcast";
                }
                return "unknown";
        }
    }

    private static String audioDeviceNormalizedType(int type) {
        switch (type) {
            case AudioDeviceInfo.TYPE_BUILTIN_EARPIECE:
                return "earpiece";
            case AudioDeviceInfo.TYPE_BUILTIN_SPEAKER:
                return "speaker";
            case AudioDeviceInfo.TYPE_WIRED_HEADSET:
            case AudioDeviceInfo.TYPE_WIRED_HEADPHONES:
                return "wiredHeadset";
            case AudioDeviceInfo.TYPE_BLUETOOTH_SCO:
            case AudioDeviceInfo.TYPE_BLUETOOTH_A2DP:
                return "bluetooth";
            case AudioDeviceInfo.TYPE_HDMI:
                return "hdmi";
            case AudioDeviceInfo.TYPE_USB_DEVICE:
            case AudioDeviceInfo.TYPE_USB_HEADSET:
            case AudioDeviceInfo.TYPE_USB_ACCESSORY:
                return "usb";
            case AudioDeviceInfo.TYPE_HEARING_AID:
                return "hearingAid";
            default:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (type == AudioDeviceInfo.TYPE_BLE_HEADSET) return "bluetooth";
                    if (type == AudioDeviceInfo.TYPE_BLE_SPEAKER) return "bluetooth";
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (type == AudioDeviceInfo.TYPE_BLE_BROADCAST) return "bluetooth";
                }
                return "unknown";
        }
    }

    private static WritableMap serializeAudioDevice(AudioDeviceInfo device) {
        WritableMap map = Arguments.createMap();
        map.putString("type", audioDeviceNormalizedType(device.getType()));
        map.putString("nativeType", audioDeviceNativeType(device.getType()));
        map.putString("name", device.getProductName().toString());
        map.putString("id", String.valueOf(device.getId()));
        return map;
    }

    public void getAvailableAudioOutputs(Promise promise) {
        WritableArray result = Arguments.createArray();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            List<AudioDeviceInfo> devices = audioManager.getAvailableCommunicationDevices();
            for (AudioDeviceInfo device : devices) {
                result.pushMap(serializeAudioDevice(device));
            }
        } else {
            AudioDeviceInfo[] devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS);
            for (AudioDeviceInfo device : devices) {
                int type = device.getType();
                if (type == AudioDeviceInfo.TYPE_BUILTIN_EARPIECE || type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
                        || type == AudioDeviceInfo.TYPE_WIRED_HEADSET || type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES
                        || type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO || type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP
                        || type == AudioDeviceInfo.TYPE_HDMI || type == AudioDeviceInfo.TYPE_USB_DEVICE
                        || type == AudioDeviceInfo.TYPE_USB_HEADSET || type == AudioDeviceInfo.TYPE_USB_ACCESSORY
                        || type == AudioDeviceInfo.TYPE_HEARING_AID) {
                    result.pushMap(serializeAudioDevice(device));
                }
            }
        }

        promise.resolve(result);
    }

    public void getCurrentAudioOutput(Promise promise) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AudioDeviceInfo device = audioManager.getCommunicationDevice();
            if (device != null) {
                promise.resolve(serializeAudioDevice(device));
            } else {
                promise.resolve(null);
            }
        } else {
            AudioDeviceInfo matched = findCurrentOutputLegacy();
            if (matched != null) {
                promise.resolve(serializeAudioDevice(matched));
            } else {
                promise.resolve(null);
            }
        }
    }

    private AudioDeviceInfo findCurrentOutputLegacy() {
        AudioDeviceInfo[] devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS);

        if (audioManager.isBluetoothScoOn()) {
            for (AudioDeviceInfo d : devices) {
                if (d.getType() == AudioDeviceInfo.TYPE_BLUETOOTH_SCO) return d;
            }
        }
        if (audioManager.isSpeakerphoneOn()) {
            for (AudioDeviceInfo d : devices) {
                if (d.getType() == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER) return d;
            }
        }
        // A wired headset only appears in the device list while it is plugged in,
        // and the OS auto-routes to it. No deprecated isWiredHeadsetOn() needed.
        for (AudioDeviceInfo d : devices) {
            if (d.getType() == AudioDeviceInfo.TYPE_WIRED_HEADSET
                    || d.getType() == AudioDeviceInfo.TYPE_WIRED_HEADPHONES) {
                return d;
            }
        }
        for (AudioDeviceInfo d : devices) {
            if (d.getType() == AudioDeviceInfo.TYPE_BUILTIN_EARPIECE) return d;
        }
        return null;
    }

    public void selectAudioOutput(String deviceIdStr, Promise promise) {
        int deviceId;
        try {
            deviceId = Integer.parseInt(deviceIdStr);
        } catch (NumberFormatException e) {
            promise.reject("E_AUDIO_OUTPUT_SELECT", "Invalid device ID: " + deviceIdStr, e);
            return;
        }

        AudioDeviceInfo target = findTargetDevice(deviceId);
        if (target == null) {
            promise.reject("E_AUDIO_OUTPUT_SELECT", "Audio output not available for device ID: " + deviceId);
            return;
        }
        int targetType = target.getType();

        if (isCurrentlyRouted(deviceId, targetType)) {
            promise.resolve(null);
            return;
        }

        synchronized (this) {
            if (pending != null) {
                mainHandler.removeCallbacks(pending.timeoutTask);
                Promise old = pending.promise;
                pending = null;
                old.reject("E_AUDIO_OUTPUT_SUPERSEDED", "Superseded by newer selectAudioOutput call");
            }
            Runnable timeoutTask = this::timeoutPending;
            pending = new PendingSelect(promise, deviceId, targetType, timeoutTask);
            mainHandler.postDelayed(timeoutTask, ROUTE_CHANGE_TIMEOUT_MS);
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                dispatchSelectApi31(target);
            } else {
                dispatchSelectLegacy(target);
                // Legacy non-SCO routes don't reliably trigger AudioDeviceCallback for state-only
                // toggles (speakerphone, earpiece, wired). Emit + check completion right away;
                // SCO targets fall through to the broadcast receiver below.
                emitOutputChangedEvent();
                maybeResolvePending();
            }
        } catch (Exception e) {
            failPending(e);
        }
    }

    private AudioDeviceInfo findTargetDevice(int deviceId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            for (AudioDeviceInfo d : audioManager.getAvailableCommunicationDevices()) {
                if (d.getId() == deviceId) return d;
            }
        } else {
            for (AudioDeviceInfo d : audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)) {
                if (d.getId() == deviceId) return d;
            }
        }
        return null;
    }

    private boolean isCurrentlyRouted(int targetId, int targetType) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AudioDeviceInfo current = audioManager.getCommunicationDevice();
            if (current == null) {
                // clearCommunicationDevice() leaves current==null; that's the earpiece state.
                return targetType == AudioDeviceInfo.TYPE_BUILTIN_EARPIECE;
            }
            return current.getId() == targetId;
        }
        AudioDeviceInfo current = findCurrentOutputLegacy();
        return current != null && current.getId() == targetId;
    }

    @RequiresApi(api = Build.VERSION_CODES.S)
    private void dispatchSelectApi31(AudioDeviceInfo target) {
        if (target.getType() == AudioDeviceInfo.TYPE_BUILTIN_EARPIECE) {
            audioManager.clearCommunicationDevice();
            return;
        }
        boolean accepted = audioManager.setCommunicationDevice(target);
        if (!accepted) {
            throw new RuntimeException("setCommunicationDevice failed for device ID " + target.getId());
        }
    }

    private void dispatchSelectLegacy(AudioDeviceInfo target) {
        audioManager.setSpeakerphoneOn(false);
        audioManager.setBluetoothScoOn(false);
        try {
            audioManager.stopBluetoothSco();
        } catch (Exception ignored) {
        }

        switch (target.getType()) {
            case AudioDeviceInfo.TYPE_BUILTIN_SPEAKER:
                audioManager.setSpeakerphoneOn(true);
                break;
            case AudioDeviceInfo.TYPE_BLUETOOTH_SCO:
            case AudioDeviceInfo.TYPE_BLUETOOTH_A2DP:
                audioManager.startBluetoothSco();
                audioManager.setBluetoothScoOn(true);
                break;
            case AudioDeviceInfo.TYPE_BUILTIN_EARPIECE:
            case AudioDeviceInfo.TYPE_WIRED_HEADSET:
            case AudioDeviceInfo.TYPE_WIRED_HEADPHONES:
                break;
            default:
                throw new RuntimeException("Cannot select audio output type on this API level: "
                        + audioDeviceNativeType(target.getType()));
        }
    }

    private void maybeResolvePending() {
        synchronized (this) {
            if (pending == null) return;

            boolean matched;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                AudioDeviceInfo current = audioManager.getCommunicationDevice();
                if (current == null) {
                    matched = pending.targetType == AudioDeviceInfo.TYPE_BUILTIN_EARPIECE;
                } else {
                    matched = current.getId() == pending.targetDeviceId;
                }
            } else {
                AudioDeviceInfo current = findCurrentOutputLegacy();
                matched = current != null && current.getId() == pending.targetDeviceId;
            }
            if (!matched) return;

            mainHandler.removeCallbacks(pending.timeoutTask);
            Promise p = pending.promise;
            pending = null;
            p.resolve(null);
        }
    }

    private void timeoutPending() {
        synchronized (this) {
            if (pending == null) return;
            mainHandler.removeCallbacks(pending.timeoutTask);
            Promise p = pending.promise;
            pending = null;
            p.reject("E_AUDIO_OUTPUT_TIMEOUT",
                    String.format("Route change not confirmed within %dms", ROUTE_CHANGE_TIMEOUT_MS));
        }
    }

    private void cancelPending(String reason) {
        synchronized (this) {
            if (pending == null) return;
            mainHandler.removeCallbacks(pending.timeoutTask);
            Promise p = pending.promise;
            pending = null;
            p.reject("E_AUDIO_OUTPUT_CANCELLED", reason);
        }
    }

    private void failPending(Exception e) {
        synchronized (this) {
            if (pending == null) return;
            mainHandler.removeCallbacks(pending.timeoutTask);
            Promise p = pending.promise;
            pending = null;
            p.reject("E_AUDIO_OUTPUT_SELECT", e.getMessage(), e);
        }
    }

    public void startObserving() {
        if (isObserving) return;
        isObserving = true;

        audioDeviceCallback = new AudioDeviceCallback() {
            @Override
            public void onAudioDevicesAdded(AudioDeviceInfo[] addedDevices) {
                emitOutputChangedEvent();
                maybeResolvePending();
            }
            @Override
            public void onAudioDevicesRemoved(AudioDeviceInfo[] removedDevices) {
                emitOutputChangedEvent();
                maybeResolvePending();
            }
        };
        audioManager.registerAudioDeviceCallback(audioDeviceCallback, mainHandler);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            communicationDeviceChangedListener = device -> {
                emitOutputChangedEvent();
                maybeResolvePending();
            };
            audioManager.addOnCommunicationDeviceChangedListener(
                    reactContext.getMainExecutor(), communicationDeviceChangedListener);
        }

        scoReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context c, Intent intent) {
                int state = intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, -1);
                if (state == AudioManager.SCO_AUDIO_STATE_CONNECTED
                        || state == AudioManager.SCO_AUDIO_STATE_DISCONNECTED) {
                    emitOutputChangedEvent();
                    maybeResolvePending();
                }
            }
        };
        IntentFilter scoFilter = new IntentFilter(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            reactContext.registerReceiver(scoReceiver, scoFilter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            reactContext.registerReceiver(scoReceiver, scoFilter);
        }
    }

    public void stopObserving() {
        if (!isObserving) return;
        isObserving = false;

        if (audioDeviceCallback != null) {
            audioManager.unregisterAudioDeviceCallback(audioDeviceCallback);
            audioDeviceCallback = null;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && communicationDeviceChangedListener != null) {
            audioManager.removeOnCommunicationDeviceChangedListener(communicationDeviceChangedListener);
            communicationDeviceChangedListener = null;
        }
        if (scoReceiver != null) {
            try {
                reactContext.unregisterReceiver(scoReceiver);
            } catch (IllegalArgumentException ignored) {
            }
            scoReceiver = null;
        }

        cancelPending("Audio output observer stopped");
    }

    private void emitOutputChangedEvent() {
        WritableMap params = Arguments.createMap();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AudioDeviceInfo device = audioManager.getCommunicationDevice();
            if (device != null) {
                params.putMap("currentAudioOutput", serializeAudioDevice(device));
            } else {
                params.putNull("currentAudioOutput");
            }
        } else {
            AudioDeviceInfo current = findCurrentOutputLegacy();
            if (current != null) {
                params.putMap("currentAudioOutput", serializeAudioDevice(current));
            } else {
                params.putNull("currentAudioOutput");
            }
        }

        WritableArray available = Arguments.createArray();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            for (AudioDeviceInfo d : audioManager.getAvailableCommunicationDevices()) {
                available.pushMap(serializeAudioDevice(d));
            }
        } else {
            AudioDeviceInfo[] devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS);
            for (AudioDeviceInfo d : devices) {
                int type = d.getType();
                if (type == AudioDeviceInfo.TYPE_BUILTIN_EARPIECE || type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
                        || type == AudioDeviceInfo.TYPE_WIRED_HEADSET || type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES
                        || type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO || type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP
                        || type == AudioDeviceInfo.TYPE_HDMI || type == AudioDeviceInfo.TYPE_USB_DEVICE
                        || type == AudioDeviceInfo.TYPE_USB_HEADSET || type == AudioDeviceInfo.TYPE_USB_ACCESSORY
                        || type == AudioDeviceInfo.TYPE_HEARING_AID) {
                    available.pushMap(serializeAudioDevice(d));
                }
            }
        }
        params.putArray("availableAudioOutputs", available);

        webRTCModule.sendEvent("audioOutputChanged", params);
    }
}
