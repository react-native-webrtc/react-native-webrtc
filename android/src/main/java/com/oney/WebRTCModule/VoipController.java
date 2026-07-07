package com.oney.WebRTCModule;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.WritableMap;
import com.oney.WebRTCModule.voip.VoipPushRegistry;

final class VoipController implements VoipPushRegistry.Listener {
    private final WebRTCModule webRTCModule;

    VoipController(WebRTCModule webRTCModule) {
        this.webRTCModule = webRTCModule;
    }

    void attach() {
        VoipPushRegistry.INSTANCE.setListener(this);
    }

    void detach() {
        VoipPushRegistry.INSTANCE.setListener(null);
    }

    void resolveToken(Promise promise) {
        VoipPushRegistry.INSTANCE.resolveToken(promise);
    }

    WritableMap getPendingIncomingCall() {
        VoipPushRegistry.Incoming incoming = VoipPushRegistry.INSTANCE.pending();
        if (incoming == null) {
            return null;
        }
        WritableMap map = Arguments.createMap();
        map.putString("roomName", incoming.getRoomName());
        map.putString("displayName", incoming.getDisplayName());
        map.putBoolean("isVideo", incoming.isVideo());
        return map;
    }

    void clearPendingIncomingCall() {
        VoipPushRegistry.INSTANCE.clearPending();
    }

    @Override
    public void onVoipToken(String token) {
        WritableMap body = Arguments.createMap();
        body.putString("registered", token);
        webRTCModule.sendEvent("voipPushEvent", body);
    }

    @Override
    public void onVoipIncoming(VoipPushRegistry.Incoming incoming) {
        WritableMap payload = Arguments.createMap();
        payload.putString("roomName", incoming.getRoomName());
        payload.putString("displayName", incoming.getDisplayName());
        payload.putBoolean("isVideo", incoming.isVideo());
        WritableMap body = Arguments.createMap();
        body.putMap("incoming", payload);
        webRTCModule.sendEvent("voipPushEvent", body);
    }
}
