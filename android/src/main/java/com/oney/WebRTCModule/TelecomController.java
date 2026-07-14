package com.oney.WebRTCModule;

import android.app.Activity;
import android.os.Build;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.WritableMap;
import com.oney.WebRTCModule.voip.CallEventsListener;
import com.oney.WebRTCModule.voip.CallManager;
import com.oney.WebRTCModule.voip.LockScreenController;

final class TelecomController implements CallEventsListener {
    private final WebRTCModule webRTCModule;
    private final ReactApplicationContext reactContext;
    private final AudioOutputManager audioOutputManager;

    TelecomController(
            WebRTCModule webRTCModule, ReactApplicationContext reactContext, AudioOutputManager audioOutputManager) {
        this.webRTCModule = webRTCModule;
        this.reactContext = reactContext;
        this.audioOutputManager = audioOutputManager;
    }

    // Core-Telecom (CallManager) requires API 26+; below that, Telecom methods are no-ops.
    void attach() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CallManager.INSTANCE.setListener(this);
            CallManager.INSTANCE.setAudioOutputManager(audioOutputManager);
        }
    }

    void detach() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // CallManager is a process-wide singleton; detach so it doesn't hold a
            // reference to this (possibly destroyed) controller after a reload.
            CallManager.INSTANCE.setListener(null);
            CallManager.INSTANCE.setAudioOutputManager(null);
        }
    }

    void startCall(String displayName, boolean isVideo) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CallManager.INSTANCE.startOutgoingCall(reactContext, displayName, isVideo);
        }
    }

    void reportOutgoingCallConnected() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CallManager.INSTANCE.reportOutgoingCallConnected();
        }
    }

    boolean fulfillAnswered(String requestId) {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                && CallManager.INSTANCE.fulfillAnswered(requestId);
    }

    void failAnswered(String requestId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CallManager.INSTANCE.failAnswered(requestId);
        }
    }

    void endCall(String reason) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CallManager.INSTANCE.endCall(CallManager.INSTANCE.reasonToCause(reason));
        }
    }

    boolean hasActiveCall() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && CallManager.INSTANCE.hasActiveCall();
    }

    boolean isAnswered() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && CallManager.INSTANCE.isAnswered();
    }

    String pendingAnswerRequestId() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? CallManager.INSTANCE.pendingAnswerRequestId()
                : null;
    }

    @Override
    public void onStarted() {
        WritableMap body = Arguments.createMap();
        body.putString("event", "started");
        webRTCModule.sendEvent("telecomActionPerformed", body);
    }

    @Override
    public void onAnswered(String requestId) {
        // Warm start: the host activity already exists, so the lifecycle hook
        // in LockScreenController never fires — flag it directly.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Activity activity = reactContext.getCurrentActivity();
            if (activity != null) {
                LockScreenController.INSTANCE.showOverLockScreen(activity);
            }
        }
        WritableMap body = Arguments.createMap();
        body.putString("event", "answer");
        body.putString("requestId", requestId);
        webRTCModule.sendEvent("telecomActionPerformed", body);
    }

    @Override
    public void onEnded(String reason) {
        WritableMap body = Arguments.createMap();
        body.putString("event", "ended");
        body.putString("reason", reason);
        webRTCModule.sendEvent("telecomActionPerformed", body);
    }

    @Override
    public void onFailed(String reason) {
        WritableMap body = Arguments.createMap();
        body.putString("event", "failed");
        body.putString("reason", reason);
        webRTCModule.sendEvent("telecomActionPerformed", body);
    }

    @Override
    public void onMuteChanged(boolean muted) {
        WritableMap body = Arguments.createMap();
        body.putString("event", "muteChanged");
        body.putBoolean("muted", muted);
        webRTCModule.sendEvent("telecomActionPerformed", body);
    }
}
