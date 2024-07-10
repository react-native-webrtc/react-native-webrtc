package com.oney.rnwebrtc;

import android.util.*;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;

import org.webrtc.CapturerObserver;
import org.webrtc.VideoFrame;

/**
 * A capturer observer that emits track ended events when the capturer is stopped.
 */
public class TrackCapturerEventsEmitter implements AbstractVideoCaptureController.CapturerEventsListener {
    private static final String TAG = TrackCapturerEventsEmitter.class.getCanonicalName();

    private final WebRTCModuleImpl webRTCModuleImpl;
    private final String trackId;

    public TrackCapturerEventsEmitter(WebRTCModuleImpl webRTCModuleImpl, String trackId) {
        this.webRTCModuleImpl = webRTCModuleImpl;
        this.trackId = trackId;
    }

    /** Notify that the capturer has been stopped. */
    public void onCapturerEnded() {
        WritableMap params = Arguments.createMap();
        params.putString("trackId", trackId);

        Log.d(TAG, "ended event trackId: " + trackId);

        webRTCModuleImpl.sendEvent("mediaStreamTrackEnded", params);
    }
}
