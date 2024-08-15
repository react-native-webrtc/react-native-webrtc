package com.oney.rnwebrtc;

import androidx.annotation.NonNull;

import com.facebook.react.module.annotations.ReactModule;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.ReadableArray;

import org.webrtc.MediaStream;

@ReactModule(name = WebRTCModuleImpl.NAME)
public class WebRTCModule extends NativeWebRTCSpec {

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

    @Override
    public void addListener(String eventType) {
        // Keep: Required for RN built in Event Emitter Calls.
        webRTCModuleImpl.addListener(eventType);
    }

    @Override
    public void removeListeners(int id) {
        // Keep: Required for RN built in Event Emitter Calls.
        webRTCModuleImpl.removeListeners(id);
    }

    MediaStream getStreamForReactTag(String streamReactTag) {
        return webRTCModuleImpl.getStreamForReactTag(streamReactTag);
    }

    @Override
    public boolean peerConnectionInit(ReadableMap configuration, int id) {
        return webRTCModuleImpl.peerConnectionInit(configuration, id);
    }

    @Override
    public WritableMap peerConnectionAddTransceiver(int id, ReadableMap options) {
        return webRTCModuleImpl.peerConnectionAddTransceiver(id, options);
    }

    @Override
    public WritableMap peerConnectionAddTrack(int id, String trackId, ReadableMap options) {
        return webRTCModuleImpl.peerConnectionAddTrack(id, trackId, options);
    }

    @Override
    public boolean peerConnectionRemoveTrack(int id, String senderId) {
        return webRTCModuleImpl.peerConnectionRemoveTrack(id, senderId);
    }

    @Override
    public void senderSetParameters(int id, String senderId, ReadableMap options, Promise promise) {
        webRTCModuleImpl.senderSetParameters(id, senderId, options, promise);
    }

    @Override
    public void transceiverStop(int id, String senderId, Promise promise) {
        webRTCModuleImpl.transceiverStop(id, senderId, promise);
    }

    @Override
    public void senderReplaceTrack(int id, String senderId, String trackId, Promise promise) {
        webRTCModuleImpl.senderReplaceTrack(id, senderId, trackId, promise);
    }

    @Override
    public void transceiverSetDirection(int id, String senderId, String direction, Promise promise) {
        webRTCModuleImpl.transceiverSetDirection(id, senderId, direction, promise);
    }

    @Override
    public void transceiverSetCodecPreferences(int id, String senderId, ReadableArray codecPreferences) {
        webRTCModuleImpl.transceiverSetCodecPreferences(id, senderId, codecPreferences);
    }

    @Override
    public void enumerateDevices(Promise promise) {
        webRTCModuleImpl.enumerateDevices(promise);
    }

    @Override
    public void getUserMedia(ReadableMap constraints, Promise promise) {
        webRTCModuleImpl.getUserMedia(constraints, promise);
    }

    @Override
    public void getDisplayMedia(Promise promise) {
        webRTCModuleImpl.getDisplayMedia(promise);
    }

    @Override
    public void mediaStreamCreate(String id) {
        webRTCModuleImpl.mediaStreamCreate(id);
    }

    @Override
    public void mediaStreamAddTrack(String streamId, int peerConnectionId, String trackId) {
        webRTCModuleImpl.mediaStreamAddTrack(streamId, peerConnectionId, trackId);
    }

    @Override
    public void mediaStreamRemoveTrack(String streamId, int peerConnectionId, String trackId) {
        webRTCModuleImpl.mediaStreamRemoveTrack(streamId, peerConnectionId, trackId);
    }

    @Override
    public void mediaStreamRelease(String id) {
        webRTCModuleImpl.mediaStreamRelease(id);
    }

    @Override
    public void mediaStreamTrackRelease(String id) {
        webRTCModuleImpl.mediaStreamTrackRelease(id);
    }

    @Override
    public void mediaStreamTrackSetEnabled(int peerConnectionId, String id, boolean enabled) {
        webRTCModuleImpl.mediaStreamTrackSetEnabled(peerConnectionId, id, enabled);
    }

    @Override
    public void mediaStreamTrackSwitchCamera(String id) {
        webRTCModuleImpl.mediaStreamTrackSwitchCamera(id);
    }

    @Override
    public void mediaStreamTrackSetVolume(int peerConnectionId, String id, double volume) {
        webRTCModuleImpl.mediaStreamTrackSetVolume(peerConnectionId, id, volume);
    }

    @Override
    public void mediaStreamTrackSetVideoEffects(String id, ReadableArray names) {
        webRTCModuleImpl.mediaStreamTrackSetVideoEffects(id, names);
    }

    @Override
    public void peerConnectionSetConfiguration(ReadableMap configuration, int id) {
        webRTCModuleImpl.peerConnectionSetConfiguration(configuration, id);
    }

    @Override
    public void peerConnectionCreateOffer(int id, ReadableMap options, Promise promise) {
        webRTCModuleImpl.peerConnectionCreateOffer(id, options, promise);
    }

    @Override
    public void peerConnectionCreateAnswer(int id, ReadableMap options, Promise promise) {
        webRTCModuleImpl.peerConnectionCreateAnswer(id, options, promise);
    }

    @Override
    public void peerConnectionSetLocalDescription(int peerConnectionId, ReadableMap desc, Promise promise) {
        webRTCModuleImpl.peerConnectionSetLocalDescription(peerConnectionId, desc, promise);
    }

    @Override
    public void peerConnectionSetRemoteDescription(int id, ReadableMap desc, Promise promise) {
        webRTCModuleImpl.peerConnectionSetRemoteDescription(id, desc, promise);
    }

    @Override
    public WritableMap receiverGetCapabilities(String kind) {
        return webRTCModuleImpl.receiverGetCapabilities(kind);
    }

    @Override
    public WritableMap senderGetCapabilities(String kind) {
        return webRTCModuleImpl.senderGetCapabilities(kind);
    }

    @Override
    public void receiverGetStats(int peerConnectionId, String receiverId, Promise promise) {
        webRTCModuleImpl.receiverGetStats(peerConnectionId, receiverId, promise);
    }

    @Override
    public void senderGetStats(int peerConnectionId, String senderId, Promise promise) {
        webRTCModuleImpl.senderGetStats(peerConnectionId, senderId, promise);
    }

    @Override
    public void peerConnectionAddICECandidate(int peerConnectionId, ReadableMap candidateMap, Promise promise) {
        webRTCModuleImpl.peerConnectionAddICECandidate(peerConnectionId, candidateMap, promise);
    }

    @Override
    public void peerConnectionGetStats(int peerConnectionId, Promise promise) {
        webRTCModuleImpl.peerConnectionGetStats(peerConnectionId, promise);
    }

    @Override
    public void peerConnectionClose(int id) {
        webRTCModuleImpl.peerConnectionClose(id);
    }

    @Override
    public void peerConnectionDispose(int id) {
        webRTCModuleImpl.peerConnectionDispose(id);
    }

    @Override
    public void peerConnectionRestartIce(int peerConnectionId) {
        webRTCModuleImpl.peerConnectionRestartIce(peerConnectionId);
    }

    @Override
    public WritableMap createDataChannel(int peerConnectionId, String label, ReadableMap config) {
        return webRTCModuleImpl.createDataChannel(peerConnectionId, label, config);
    }

    @Override
    public void dataChannelClose(int peerConnectionId, String reactTag) {
        webRTCModuleImpl.dataChannelClose(peerConnectionId, reactTag);
    }

    @Override
    public void dataChannelDispose(int peerConnectionId, String reactTag) {
        webRTCModuleImpl.dataChannelDispose(peerConnectionId, reactTag);
    }

    @Override
    public void dataChannelSend(int peerConnectionId, String reactTag, String data, String type) {
        webRTCModuleImpl.dataChannelSend(peerConnectionId, reactTag, data, type);
    }
}