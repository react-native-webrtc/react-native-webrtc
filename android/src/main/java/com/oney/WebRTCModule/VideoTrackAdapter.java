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
    private Map<String, VideoDimensionDetectorImpl> dimensionDetectorMap = new HashMap<>();

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

    public void addDimensionDetector(VideoTrack videoTrack) {
        String trackId = videoTrack.id();
        if (dimensionDetectorMap.containsKey(trackId)) {
            Log.w(TAG, "Attempted to add dimension detector twice for track ID: " + trackId);
            return;
        }

        VideoDimensionDetectorImpl dimensionDetector = new VideoDimensionDetectorImpl(trackId);
        Log.d(TAG, "Created dimension detector for " + trackId);
        dimensionDetectorMap.put(trackId, dimensionDetector);
        videoTrack.addSink(dimensionDetector);
    }

    public void removeDimensionDetector(VideoTrack videoTrack) {
        String trackId = videoTrack.id();
        VideoDimensionDetectorImpl dimensionDetector = dimensionDetectorMap.remove(trackId);
        if (dimensionDetector == null) {
            Log.w(TAG, "removeDimensionDetector - no detector for " + trackId);
            return;
        }

        videoTrack.removeSink(dimensionDetector);
        dimensionDetector.dispose();
        Log.d(TAG, "Deleted dimension detector for " + trackId);
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

    /**
     * Implements dimension change events for remote video tracks through
     * the {@link VideoSink} interface.
     */
    private class VideoDimensionDetectorImpl implements VideoSink {
        private volatile boolean disposed;
        private int currentWidth = 0;
        private int currentHeight = 0;
        private boolean hasInitialSize = false;
        private final String trackId;

        VideoDimensionDetectorImpl(String trackId) {
            this.trackId = trackId;
        }

        @Override
        public void onFrame(VideoFrame frame) {
            if (disposed) {
                return;
            }

            int width = frame.getBuffer().getWidth();
            int height = frame.getBuffer().getHeight();

            // Check if this is a meaningful size change
            if (!hasInitialSize) {
                currentWidth = width;
                currentHeight = height;
                hasInitialSize = true;
                emitDimensionChangeEvent(width, height);
            } else if (currentWidth != width || currentHeight != height) {
                currentWidth = width;
                currentHeight = height;
                emitDimensionChangeEvent(width, height);
            }
        }

        private void emitDimensionChangeEvent(int width, int height) {
            WritableMap params = Arguments.createMap();
            params.putInt("pcId", peerConnectionId);
            params.putString("trackId", trackId);
            params.putInt("width", width);
            params.putInt("height", height);

            Log.d(TAG, "Dimension change event pcId: " + peerConnectionId + " trackId: " + trackId + " dimensions: " + width + "x" + height);

            VideoTrackAdapter.this.webRTCModule.sendEvent("videoTrackDimensionChanged", params);
        }

        void dispose() {
            disposed = true;
        }
    }
}
