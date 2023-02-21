package com.oney.WebRTCModule;

import android.util.*;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;

import org.webrtc.VideoFrame;
import org.webrtc.VideoSink;
import org.webrtc.VideoTrack;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Implements mute/unmute events for remote video tracks.
 * Mute event is fired when there are no frames to be render for 1500ms
 * initially and 500ms after the first frame was received.
 */
public class VideoTrackAdapter {
    static final String TAG = VideoTrackAdapter.class.getCanonicalName();
    static final long INITIAL_MUTE_DELAY = 3000;
    static final long MUTE_DELAY = 1500;

    private Map<String, TrackMuteUnmuteImpl> muteImplMap = new HashMap<>();

    private Timer timer = new Timer("VideoTrackMutedTimer");

    private final int peerConnectionId;

    private final WebRTCModule webRTCModule;

    public VideoTrackAdapter(WebRTCModule webRTCModule, int peerConnectionId) {
        this.peerConnectionId = peerConnectionId;
        this.webRTCModule = webRTCModule;
    }

    public void addAdapter(VideoTrack videoTrack) {
        String trackId = videoTrack.id();
        if (muteImplMap.containsKey(trackId)) {
            Log.w(TAG, "Attempted to add adapter twice for track ID: " + trackId);
            return;
        }

        TrackMuteUnmuteImpl onMuteImpl = new TrackMuteUnmuteImpl(trackId);
        Log.d(TAG, "Created adapter for " + trackId);
        muteImplMap.put(trackId, onMuteImpl);
        videoTrack.addSink(onMuteImpl);
        onMuteImpl.start();
    }

    public void removeAdapter(VideoTrack videoTrack) {
        String trackId = videoTrack.id();
        TrackMuteUnmuteImpl onMuteImpl = muteImplMap.remove(trackId);
        if (onMuteImpl == null) {
            Log.w(TAG, "removeAdapter - no adapter for " + trackId);
            return;
        }

        videoTrack.removeSink(onMuteImpl);
        onMuteImpl.dispose();
        Log.d(TAG, "Deleted adapter for " + trackId);
    }

    /**
     * Implements 'mute'/'unmute' events for remote video tracks through
     * the {@link VideoSink} interface.
     */
    private class TrackMuteUnmuteImpl implements VideoSink {
        private TimerTask emitMuteTask;
        private volatile boolean disposed;
        private AtomicInteger frameCounter;
        private boolean mutedState;
        private final String trackId;

        TrackMuteUnmuteImpl(String trackId) {
            this.trackId = trackId;
            this.frameCounter = new AtomicInteger();
        }

        @Override
        public void onFrame(VideoFrame frame) {
            frameCounter.addAndGet(1);
        }

        private void start() {
            if (disposed) {
                return;
            }

            synchronized (this) {
                if (emitMuteTask != null) {
                    emitMuteTask.cancel();
                }
                emitMuteTask = new TimerTask() {
                    private int lastFrameNumber = frameCounter.get();

                    @Override
                    public void run() {
                        if (disposed) {
                            return;
                        }
                        boolean isMuted = lastFrameNumber == frameCounter.get();
                        if (isMuted != mutedState) {
                            mutedState = isMuted;
                            emitMuteEvent(isMuted);
                        }

                        lastFrameNumber = frameCounter.get();
                    }
                };
                timer.schedule(emitMuteTask, INITIAL_MUTE_DELAY, MUTE_DELAY);
            }
        }

        private void emitMuteEvent(boolean muted) {
            WritableMap params = Arguments.createMap();
            params.putInt("pcId", peerConnectionId);
            params.putString("trackId", trackId);
            params.putBoolean("muted", muted);

            Log.d(TAG, (muted ? "Mute" : "Unmute") + " event pcId: " + peerConnectionId + " trackId: " + trackId);

            VideoTrackAdapter.this.webRTCModule.sendEvent("mediaStreamTrackMuteChanged", params);
        }

        void dispose() {
            disposed = true;
            synchronized (this) {
                if (emitMuteTask != null) {
                    emitMuteTask.cancel();
                    emitMuteTask = null;
                }
            }
        }
    }
}
