package com.oney.WebRTCModule;

import androidx.annotation.NonNull;

import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.module.annotations.ReactModule;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import org.webrtc.MediaStream;

@ReactModule(name = "WebRTCModule")
public class WebRTCModule extends ReactContextBaseJavaModule {
    private final WebRTCModuleImpl impl;

    public WebRTCModule(ReactApplicationContext reactContext) {
        super(reactContext);

        impl = new WebRTCModuleImpl(reactContext, (eventName, params) ->
            reactContext
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit(eventName, params)
        );
    }

    @NonNull
    @Override
    public String getName() { return impl.getName(); }

    @Override
    public void invalidate() {
        impl.invalidate();

        super.invalidate();
    }

    // --- Sync methods (blocking synchronous) ---

    @ReactMethod(isBlockingSynchronousMethod = true)
    public boolean peerConnectionInit(ReadableMap config, int id) {
        return impl.peerConnectionInit(config, id);
    }

    @ReactMethod(isBlockingSynchronousMethod = true)
    public WritableMap peerConnectionAddTransceiver(int id, ReadableMap options) {
        return impl.peerConnectionAddTransceiver(id, options);
    }

    @ReactMethod(isBlockingSynchronousMethod = true)
    public WritableMap peerConnectionAddTrack(int id, String trackId, ReadableMap options) {
        return impl.peerConnectionAddTrack(id, trackId, options);
    }

    @ReactMethod(isBlockingSynchronousMethod = true)
    public boolean peerConnectionRemoveTrack(int id, String senderId) {
        return impl.peerConnectionRemoveTrack(id, senderId);
    }

    @ReactMethod(isBlockingSynchronousMethod = true)
    public WritableMap createDataChannel(int id, String label, ReadableMap config) {
        return impl.createDataChannel(id, label, config);
    }

    @ReactMethod(isBlockingSynchronousMethod = true)
    public WritableMap senderGetCapabilities(String kind) {
        return impl.senderGetCapabilities(kind);
    }

    @ReactMethod(isBlockingSynchronousMethod = true)
    public WritableMap receiverGetCapabilities(String kind) {
        return impl.receiverGetCapabilities(kind);
    }

    @ReactMethod(isBlockingSynchronousMethod = true)
    public boolean transceiverSetCodecPreferences(int id, String senderId, ReadableArray codecPreferences) {
        return impl.transceiverSetCodecPreferences(id, senderId, codecPreferences);
    }

    // --- Async methods ---

    @ReactMethod
    public void peerConnectionSetConfiguration(ReadableMap config, int id) {
        impl.peerConnectionSetConfiguration(config, id);
    }

    @ReactMethod
    public void peerConnectionCreateOffer(int id, ReadableMap options, Promise promise) {
        impl.peerConnectionCreateOffer(id, options, promise);
    }

    @ReactMethod
    public void peerConnectionCreateAnswer(int id, ReadableMap options, Promise promise) {
        impl.peerConnectionCreateAnswer(id, options, promise);
    }

    @ReactMethod
    public void peerConnectionSetLocalDescription(int pcId, ReadableMap desc, Promise promise) {
        impl.peerConnectionSetLocalDescription(pcId, desc, promise);
    }

    @ReactMethod
    public void peerConnectionSetRemoteDescription(int id, ReadableMap desc, Promise promise) {
        impl.peerConnectionSetRemoteDescription(id, desc, promise);
    }

    @ReactMethod
    public void peerConnectionAddICECandidate(int id, ReadableMap candidate, Promise promise) {
        impl.peerConnectionAddICECandidate(id, candidate, promise);
    }

    @ReactMethod
    public void peerConnectionGetStats(int id, Promise promise) {
        impl.peerConnectionGetStats(id, promise);
    }

    @ReactMethod
    public void receiverGetStats(int pcId, String receiverId, Promise promise) {
        impl.receiverGetStats(pcId, receiverId, promise);
    }

    @ReactMethod
    public void senderGetStats(int pcId, String senderId, Promise promise) {
        impl.senderGetStats(pcId, senderId, promise);
    }

    @ReactMethod
    public void peerConnectionClose(int id) {
        impl.peerConnectionClose(id);
    }

    @ReactMethod
    public void peerConnectionDispose(int id) {
        impl.peerConnectionDispose(id);
    }

    @ReactMethod
    public void peerConnectionRestartIce(int id) {
        impl.peerConnectionRestartIce(id);
    }

    @ReactMethod
    public void senderSetParameters(int id, String senderId, ReadableMap options, Promise promise) {
        impl.senderSetParameters(id, senderId, options, promise);
    }

    @ReactMethod
    public void senderReplaceTrack(int id, String senderId, String trackId, Promise promise) {
        impl.senderReplaceTrack(id, senderId, trackId, promise);
    }

    @ReactMethod
    public void transceiverStop(int id, String senderId, Promise promise) {
        impl.transceiverStop(id, senderId, promise);
    }

    @ReactMethod
    public void transceiverSetDirection(int id, String senderId, String direction, Promise promise) {
        impl.transceiverSetDirection(id, senderId, direction, promise);
    }

    @ReactMethod
    public void getDisplayMedia(Promise promise) {
        impl.getDisplayMedia(promise);
    }

    @ReactMethod
    public void getUserMedia(ReadableMap constraints, Callback successCallback, Callback errorCallback) {
        impl.getUserMedia(constraints, successCallback, errorCallback);
    }

    @ReactMethod
    public void enumerateDevices(Callback callback) {
        impl.enumerateDevices(callback);
    }

    @ReactMethod
    public void mediaStreamCreate(String id) {
        impl.mediaStreamCreate(id);
    }

    @ReactMethod
    public void mediaStreamAddTrack(String streamId, int pcId, String trackId) {
        impl.mediaStreamAddTrack(streamId, pcId, trackId);
    }

    @ReactMethod
    public void mediaStreamRemoveTrack(String streamId, int pcId, String trackId) {
        impl.mediaStreamRemoveTrack(streamId, pcId, trackId);
    }

    @ReactMethod
    public void mediaStreamRelease(String id) {
        impl.mediaStreamRelease(id);
    }

    @ReactMethod
    public void mediaStreamTrackRelease(String id) {
        impl.mediaStreamTrackRelease(id);
    }

    @ReactMethod
    public void mediaStreamTrackSetEnabled(int pcId, String id, boolean enabled) {
        impl.mediaStreamTrackSetEnabled(pcId, id, enabled);
    }

    @ReactMethod
    public void mediaStreamTrackApplyConstraints(String id, ReadableMap constraints, Promise promise) {
        impl.mediaStreamTrackApplyConstraints(id, constraints, promise);
    }

    @ReactMethod
    public void mediaStreamTrackSetVolume(int pcId, String id, double volume) {
        impl.mediaStreamTrackSetVolume(pcId, id, volume);
    }

    @ReactMethod
    public void mediaStreamTrackSetVideoEffects(String id, ReadableArray names) {
        impl.mediaStreamTrackSetVideoEffects(id, names);
    }

    @ReactMethod
    public void dataChannelClose(int peerConnectionId, String reactTag) {
        impl.dataChannelClose(peerConnectionId, reactTag);
    }

    @ReactMethod
    public void dataChannelDispose(int peerConnectionId, String reactTag) {
        impl.dataChannelDispose(peerConnectionId, reactTag);
    }

    @ReactMethod
    public void dataChannelSend(int peerConnectionId, String reactTag, String data, String type) {
        impl.dataChannelSend(peerConnectionId, reactTag, data, type);
    }

    @ReactMethod
    public void audioSessionDidActivate() {
        impl.audioSessionDidActivate();
    }

    @ReactMethod
    public void audioSessionDidDeactivate() {
        impl.audioSessionDidDeactivate();
    }

    // No-ops on Android — iOS-only permission methods (Android uses PermissionsAndroid)

    @ReactMethod
    public void checkPermission(String mediaType, Promise promise) {
        impl.checkPermission(mediaType, promise);
    }

    @ReactMethod
    public void requestPermission(String mediaType, Promise promise) {
        impl.requestPermission(mediaType, promise);
    }

    // --- NativeEventEmitter required methods ---

    @ReactMethod
    public void addListener(String eventName) {
        impl.addListener(eventName);
    }

    @ReactMethod
    public void removeListeners(int count) {
        impl.removeListeners(count);
    }

    // --- Internal: expose getStreamForReactTag for WebRTCView ---

    public MediaStream getStreamForReactTag(String streamReactTag) {
        return impl.getStreamForReactTag(streamReactTag);
    }

    // --- Internal: expose getThreadUtils for WebRTCView ---

    public ThreadUtils getThreadUtils() {
        return impl.getThreadUtils();
    }
}
