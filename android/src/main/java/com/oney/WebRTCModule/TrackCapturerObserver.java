package com.oney.WebRTCModule;

import android.util.*;

import org.webrtc.CapturerObserver;
import org.webrtc.VideoFrame;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;

/**
 * A capturer observer that emits track ended events when the capturer is stopped.
 */
public class TrackCapturerObserver implements CapturerObserver {

  private static final String TAG = TrackCapturerObserver.class.getCanonicalName();

  private final WebRTCModule webRTCModule;
  private final String trackId;

  // The capturer observer to delegate to.
  private final CapturerObserver capturerObserver;

  public TrackCapturerObserver(WebRTCModule webRTCModule, String trackId, CapturerObserver capturerObserver) {
    this.webRTCModule = webRTCModule;
    this.trackId = trackId;
    this.capturerObserver = capturerObserver;
  }

  /** Notify if the capturer have been started successfully or not. */
  public void onCapturerStarted(boolean success) {
    this.capturerObserver.onCapturerStarted(success);
  }

  /** Notify that the capturer has been stopped. */
  public void onCapturerStopped() {
    this.capturerObserver.onCapturerStopped();

    WritableMap params = Arguments.createMap();
    params.putString("trackId", trackId);

    Log.d(TAG, "ended event trackId: " + trackId);

    webRTCModule.sendEvent(
        "mediaStreamTrackEnded", params);
  }

  /** Delivers a captured frame. */
  public void onFrameCaptured(VideoFrame frame) {
    this.capturerObserver.onFrameCaptured(frame);
  }
}
