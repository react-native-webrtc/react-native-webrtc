package com.oney.WebRTCModule;

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

    private final WebRTCModule webRTCModule;
    private final String trackId;

    public TrackCapturerEventsEmitter(WebRTCModule webRTCModule, String trackId) {
        this.webRTCModule = webRTCModule;
        this.trackId = trackId;
    }

    /** Notify that the capturer has been stopped. */
    public void onCapturerEnded() {
        WritableMap params = Arguments.createMap();
        params.putString("trackId", trackId);

        Log.d(TAG, "ended event trackId: " + trackId);

        webRTCModule.sendEvent("mediaStreamTrackEnded", params);
    }
}
