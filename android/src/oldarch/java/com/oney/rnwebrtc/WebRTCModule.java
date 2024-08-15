package com.oney.rnwebrtc;

import androidx.annotation.NonNull;

import com.facebook.react.module.annotations.ReactModule;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.ReadableArray;

import org.webrtc.MediaStream;

@ReactModule(name = WebRTCModuleImpl.NAME)
public class WebRTCModule extends ReactContextBaseJavaModule {

    private final WebRTCModuleImpl webRTCModuleImpl;

    WebRTCModule(ReactApplicationContext context) {
        super(context);

        webRTCModuleImpl = new WebRTCModuleImpl(context);
    }

    @Override
    @NonNull
    public String getName() {
        return WebRTCModuleImpl.NAME;
    }

    @ReactMethod
    public void addListener(String eventType) {
        // Keep: Required for RN built in Event Emitter Calls.
        webRTCModuleImpl.addListener(eventType);
    }

    @ReactMethod
    public void removeListeners(int id) {
        // Keep: Required for RN built in Event Emitter Calls.
        webRTCModuleImpl.removeListeners(id);
    }

    MediaStream getStreamForReactTag(String streamReactTag) {
        return webRTCModuleImpl.getStreamForReactTag(streamReactTag);
    }

    @ReactMethod(isBlockingSynchronousMethod = true)
    public boolean peerConnectionInit(ReadableMap configuration, int id) {
        return webRTCModuleImpl.peerConnectionInit(configuration, id);
    }

    @ReactMethod(isBlockingSynchronousMethod = true)
    public WritableMap peerConnectionAddTransceiver(int id, ReadableMap options) {
        return webRTCModuleImpl.peerConnectionAddTransceiver(id, options);
    }

    @ReactMethod(isBlockingSynchronousMethod = true)
    public WritableMap peerConnectionAddTrack(int id, String trackId, ReadableMap options) {
        return webRTCModuleImpl.peerConnectionAddTrack(id, trackId, options);
    }

    @ReactMethod(isBlockingSynchronousMethod = true)
    public boolean peerConnectionRemoveTrack(int id, String senderId) {
        return webRTCModuleImpl.peerConnectionRemoveTrack(id, senderId);
    }

    @ReactMethod
    public void senderSetParameters(int id, String senderId, ReadableMap options, Promise promise) {
        webRTCModuleImpl.senderSetParameters(id, senderId, options, promise);
    }

    @ReactMethod
    public void transceiverStop(int id, String senderId, Promise promise) {
        webRTCModuleImpl.transceiverStop(id, senderId, promise);
    }

    @ReactMethod
    public void senderReplaceTrack(int id, String senderId, String trackId, Promise promise) {
        webRTCModuleImpl.senderReplaceTrack(id, senderId, trackId, promise);
    }

    @ReactMethod
    public void transceiverSetDirection(int id, String senderId, String direction, Promise promise) {
        webRTCModuleImpl.transceiverSetDirection(id, senderId, direction, promise);
    }

    @ReactMethod(isBlockingSynchronousMethod = true)
    public void transceiverSetCodecPreferences(int id, String senderId, ReadableArray codecPreferences) {
        webRTCModuleImpl.transceiverSetCodecPreferences(id, senderId, codecPreferences);
    }

    @ReactMethod
    public void enumerateDevices(Promise promise) {
        webRTCModuleImpl.enumerateDevices(promise);
    }

    @ReactMethod
    public void getUserMedia(ReadableMap constraints, Promise promise) {
        webRTCModuleImpl.getUserMedia(constraints, promise);
    }

    @ReactMethod
    public void getDisplayMedia(Promise promise) {
        webRTCModuleImpl.getDisplayMedia(promise);
    }

    @ReactMethod
    public void mediaStreamCreate(String id) {
        webRTCModuleImpl.mediaStreamCreate(id);
    }

    @ReactMethod
    public void mediaStreamAddTrack(String streamId, int peerConnectionId, String trackId) {
        webRTCModuleImpl.mediaStreamAddTrack(streamId, peerConnectionId, trackId);
    }

    @ReactMethod
    public void mediaStreamRemoveTrack(String streamId, int peerConnectionId, String trackId) {
        webRTCModuleImpl.mediaStreamRemoveTrack(streamId, peerConnectionId, trackId);
    }

    @ReactMethod
    public void mediaStreamRelease(String id) {
        webRTCModuleImpl.mediaStreamRelease(id);
    }

    @ReactMethod
    public void mediaStreamTrackRelease(String id) {
        webRTCModuleImpl.mediaStreamTrackRelease(id);
    }

    @ReactMethod
    public void mediaStreamTrackSetEnabled(int peerConnectionId, String id, boolean enabled) {
        webRTCModuleImpl.mediaStreamTrackSetEnabled(peerConnectionId, id, enabled);
    }

    @ReactMethod
    public void mediaStreamTrackSwitchCamera(String id) {
        webRTCModuleImpl.mediaStreamTrackSwitchCamera(id);
    }

    @ReactMethod
    public void mediaStreamTrackSetVolume(int peerConnectionId, String id, double volume) {
        webRTCModuleImpl.mediaStreamTrackSetVolume(peerConnectionId, id, volume);
    }

    @ReactMethod
    public void mediaStreamTrackSetVideoEffects(String id, ReadableArray names) {
        webRTCModuleImpl.mediaStreamTrackSetVideoEffects(id, names);
    }

    @ReactMethod
    public void peerConnectionSetConfiguration(ReadableMap configuration, int id) {
        webRTCModuleImpl.peerConnectionSetConfiguration(configuration, id);
    }

    @ReactMethod
    public void peerConnectionCreateOffer(int id, ReadableMap options, Promise promise) {
        webRTCModuleImpl.peerConnectionCreateOffer(id, options, promise);
    }

    @ReactMethod
    public void peerConnectionCreateAnswer(int id, ReadableMap options, Promise promise) {
        webRTCModuleImpl.peerConnectionCreateAnswer(id, options, promise);
    }

    @ReactMethod
    public void peerConnectionSetLocalDescription(int peerConnectionId, ReadableMap desc, Promise promise) {
        webRTCModuleImpl.peerConnectionSetLocalDescription(peerConnectionId, desc, promise);
    }

    @ReactMethod
    public void peerConnectionSetRemoteDescription(int id, ReadableMap desc, Promise promise) {
        webRTCModuleImpl.peerConnectionSetRemoteDescription(id, desc, promise);
    }

    @ReactMethod(isBlockingSynchronousMethod = true)
    public WritableMap receiverGetCapabilities(String kind) {
        return webRTCModuleImpl.receiverGetCapabilities(kind);
    }

    @ReactMethod(isBlockingSynchronousMethod = true)
    public WritableMap senderGetCapabilities(String kind) {
        return webRTCModuleImpl.senderGetCapabilities(kind);
    }

    @ReactMethod
    public void receiverGetStats(int peerConnectionId, String receiverId, Promise promise) {
        webRTCModuleImpl.receiverGetStats(peerConnectionId, receiverId, promise);
    }

    @ReactMethod
    public void senderGetStats(int peerConnectionId, String senderId, Promise promise) {
        webRTCModuleImpl.senderGetStats(peerConnectionId, senderId, promise);
    }

    @ReactMethod
    public void peerConnectionAddICECandidate(int peerConnectionId, ReadableMap candidateMap, Promise promise) {
        webRTCModuleImpl.peerConnectionAddICECandidate(peerConnectionId, candidateMap, promise);
    }

    @ReactMethod
    public void peerConnectionGetStats(int peerConnectionId, Promise promise) {
        webRTCModuleImpl.peerConnectionGetStats(peerConnectionId, promise);
    }

    @ReactMethod
    public void peerConnectionClose(int id) {
        webRTCModuleImpl.peerConnectionClose(id);
    }

    @ReactMethod
    public void peerConnectionDispose(int id) {
        webRTCModuleImpl.peerConnectionDispose(id);
    }

    @ReactMethod
    public void peerConnectionRestartIce(int peerConnectionId) {
        webRTCModuleImpl.peerConnectionRestartIce(peerConnectionId);
    }

    @ReactMethod(isBlockingSynchronousMethod = true)
    public WritableMap createDataChannel(int peerConnectionId, String label, ReadableMap config) {
        return webRTCModuleImpl.createDataChannel(peerConnectionId, label, config);
    }

    @ReactMethod
    public void dataChannelClose(int peerConnectionId, String reactTag) {
        webRTCModuleImpl.dataChannelClose(peerConnectionId, reactTag);
    }

    @ReactMethod
    public void dataChannelDispose(int peerConnectionId, String reactTag) {
        webRTCModuleImpl.dataChannelDispose(peerConnectionId, reactTag);
    }

    @ReactMethod
    public void dataChannelSend(int peerConnectionId, String reactTag, String data, String type) {
        webRTCModuleImpl.dataChannelSend(peerConnectionId, reactTag, data, type);
    }
}
