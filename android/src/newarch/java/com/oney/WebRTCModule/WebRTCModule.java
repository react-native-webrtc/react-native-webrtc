package com.oney.WebRTCModule;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.module.annotations.ReactModule;

import org.webrtc.MediaStream;

@ReactModule(name = "WebRTCModule")
public class WebRTCModule extends NativeWebRTCModuleSpec {
    private final WebRTCModuleImpl impl;

    public WebRTCModule(ReactApplicationContext reactContext) {
        super(reactContext);

        impl = new WebRTCModuleImpl(reactContext, this::emitEvent);
    }

    @NonNull
    @Override
    public String getName() { return impl.getName(); }

    @Override
    public void invalidate() {
        impl.invalidate();

        super.invalidate();
    }

    // --- Event dispatch: routes event names to codegen-generated emit methods ---

    private void emitEvent(String eventName, @Nullable WritableMap params) {
        switch (eventName) {
            case "peerConnectionSignalingStateChanged":
                emitPeerConnectionSignalingStateChanged(params);
                break;
            case "peerConnectionStateChanged":
                emitPeerConnectionStateChanged(params);
                break;
            case "peerConnectionOnRenegotiationNeeded":
                emitPeerConnectionOnRenegotiationNeeded(params);
                break;
            case "peerConnectionIceConnectionChanged":
                emitPeerConnectionIceConnectionChanged(params);
                break;
            case "peerConnectionIceGatheringChanged":
                emitPeerConnectionIceGatheringChanged(params);
                break;
            case "peerConnectionGotICECandidate":
                emitPeerConnectionGotICECandidate(params);
                break;
            case "peerConnectionDidOpenDataChannel":
                emitPeerConnectionDidOpenDataChannel(params);
                break;
            case "peerConnectionOnRemoveTrack":
                emitPeerConnectionOnRemoveTrack(params);
                break;
            case "peerConnectionOnTrack":
                emitPeerConnectionOnTrack(params);
                break;
            case "dataChannelStateChanged":
                emitDataChannelStateChanged(params);
                break;
            case "dataChannelReceiveMessage":
                emitDataChannelReceiveMessage(params);
                break;
            case "dataChannelDidChangeBufferedAmount":
                emitDataChannelDidChangeBufferedAmount(params);
                break;
            case "mediaStreamTrackMuteChanged":
                emitMediaStreamTrackMuteChanged(params);
                break;
            case "mediaStreamTrackEnded":
                emitMediaStreamTrackEnded(params);
                break;
        }
    }

    // --- Sync methods ---

    @Override
    public boolean peerConnectionInit(ReadableMap config, double id) {
        return impl.peerConnectionInit(config, (int) id);
    }

    @Override
    public WritableMap peerConnectionAddTransceiver(double id, ReadableMap options) {
        return impl.peerConnectionAddTransceiver((int) id, options);
    }

    @Override
    public WritableMap peerConnectionAddTrack(double id, String trackId, ReadableMap options) {
        return impl.peerConnectionAddTrack((int) id, trackId, options);
    }

    @Override
    public boolean peerConnectionRemoveTrack(double id, String senderId) {
        return impl.peerConnectionRemoveTrack((int) id, senderId);
    }

    @Override
    public WritableMap createDataChannel(double id, String label, ReadableMap config) {
        return impl.createDataChannel((int) id, label, config);
    }

    @Override
    public WritableMap senderGetCapabilities(String kind) {
        return impl.senderGetCapabilities(kind);
    }

    @Override
    public WritableMap receiverGetCapabilities(String kind) {
        return impl.receiverGetCapabilities(kind);
    }

    @Override
    public boolean transceiverSetCodecPreferences(double id, String senderId, ReadableArray codecPreferences) {
        return impl.transceiverSetCodecPreferences((int) id, senderId, codecPreferences);
    }

    @Override
    public void audioSessionDidActivate() {
        impl.audioSessionDidActivate();
    }

    @Override
    public void audioSessionDidDeactivate() {
        impl.audioSessionDidDeactivate();
    }

    // --- Async methods ---

    @Override
    public void peerConnectionSetConfiguration(ReadableMap config, double id) {
        impl.peerConnectionSetConfiguration(config, (int) id);
    }

    @Override
    public void peerConnectionCreateOffer(double id, ReadableMap options, Promise promise) {
        impl.peerConnectionCreateOffer((int) id, options, promise);
    }

    @Override
    public void peerConnectionCreateAnswer(double id, ReadableMap options, Promise promise) {
        impl.peerConnectionCreateAnswer((int) id, options, promise);
    }

    @Override
    public void peerConnectionSetLocalDescription(double pcId, ReadableMap desc, Promise promise) {
        impl.peerConnectionSetLocalDescription((int) pcId, desc, promise);
    }

    @Override
    public void peerConnectionSetRemoteDescription(double id, ReadableMap desc, Promise promise) {
        impl.peerConnectionSetRemoteDescription((int) id, desc, promise);
    }

    @Override
    public void peerConnectionAddICECandidate(double id, ReadableMap candidate, Promise promise) {
        impl.peerConnectionAddICECandidate((int) id, candidate, promise);
    }

    @Override
    public void peerConnectionGetStats(double id, Promise promise) {
        impl.peerConnectionGetStats((int) id, promise);
    }

    @Override
    public void receiverGetStats(double pcId, String receiverId, Promise promise) {
        impl.receiverGetStats((int) pcId, receiverId, promise);
    }

    @Override
    public void senderGetStats(double pcId, String senderId, Promise promise) {
        impl.senderGetStats((int) pcId, senderId, promise);
    }

    @Override
    public void peerConnectionClose(double id) {
        impl.peerConnectionClose((int) id);
    }

    @Override
    public void peerConnectionDispose(double id) {
        impl.peerConnectionDispose((int) id);
    }

    @Override
    public void peerConnectionRestartIce(double id) {
        impl.peerConnectionRestartIce((int) id);
    }

    @Override
    public void senderSetParameters(double id, String senderId, ReadableMap options, Promise promise) {
        impl.senderSetParameters((int) id, senderId, options, promise);
    }

    @Override
    public void senderReplaceTrack(double id, String senderId, String trackId, Promise promise) {
        impl.senderReplaceTrack((int) id, senderId, trackId, promise);
    }

    @Override
    public void transceiverStop(double id, String senderId, Promise promise) {
        impl.transceiverStop((int) id, senderId, promise);
    }

    @Override
    public void transceiverSetDirection(double id, String senderId, String direction, Promise promise) {
        impl.transceiverSetDirection((int) id, senderId, direction, promise);
    }

    @Override
    public void getDisplayMedia(Promise promise) {
        impl.getDisplayMedia(promise);
    }

    @Override
    public void getUserMedia(ReadableMap constraints, Callback successCallback, Callback errorCallback) {
        impl.getUserMedia(constraints, successCallback, errorCallback);
    }

    @Override
    public void enumerateDevices(Callback callback) {
        impl.enumerateDevices(callback);
    }

    @Override
    public void mediaStreamCreate(String id) {
        impl.mediaStreamCreate(id);
    }

    @Override
    public void mediaStreamAddTrack(String streamId, double pcId, String trackId) {
        impl.mediaStreamAddTrack(streamId, (int) pcId, trackId);
    }

    @Override
    public void mediaStreamRemoveTrack(String streamId, double pcId, String trackId) {
        impl.mediaStreamRemoveTrack(streamId, (int) pcId, trackId);
    }

    @Override
    public void mediaStreamRelease(String id) {
        impl.mediaStreamRelease(id);
    }

    @Override
    public void mediaStreamTrackRelease(String id) {
        impl.mediaStreamTrackRelease(id);
    }

    @Override
    public void mediaStreamTrackSetEnabled(double pcId, String id, boolean enabled) {
        impl.mediaStreamTrackSetEnabled((int) pcId, id, enabled);
    }

    @Override
    public void mediaStreamTrackApplyConstraints(String id, ReadableMap constraints, Promise promise) {
        impl.mediaStreamTrackApplyConstraints(id, constraints, promise);
    }

    @Override
    public void mediaStreamTrackSetVolume(double pcId, String id, double volume) {
        impl.mediaStreamTrackSetVolume((int) pcId, id, volume);
    }

    @Override
    public void mediaStreamTrackSetVideoEffects(String id, ReadableArray names) {
        impl.mediaStreamTrackSetVideoEffects(id, names);
    }

    @Override
    public void dataChannelClose(double peerConnectionId, String reactTag) {
        impl.dataChannelClose((int) peerConnectionId, reactTag);
    }

    @Override
    public void dataChannelDispose(double peerConnectionId, String reactTag) {
        impl.dataChannelDispose((int) peerConnectionId, reactTag);
    }

    @Override
    public void dataChannelSend(double peerConnectionId, String reactTag, String data, String type) {
        impl.dataChannelSend((int) peerConnectionId, reactTag, data, type);
    }

    // --- No-op: handled by NativeEventEmitter interop

    @Override
    public void addListener(String eventName) {}

    @Override
    public void removeListeners(double count) {}

    // --- iOS-only permission methods (no-ops on Android) ---

    @Override
    public void checkPermission(String mediaType, Promise promise) {
        impl.checkPermission(mediaType, promise);
    }

    @Override
    public void requestPermission(String mediaType, Promise promise) {
        impl.requestPermission(mediaType, promise);
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
