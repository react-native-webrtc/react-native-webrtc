package com.oney.WebRTCModule;

import android.util.*;

import org.webrtc.VideoTrack;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoRenderer.I420Frame;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;

import java.util.*;

/**
 * Implements mute/unmute events for remote video tracks.
 * Mute event is fired when there are no frames to be render for 1500ms
 * initially and 500ms after the first frame was received.
 */
public class VideoTrackAdapter {
    static final String TAG = VideoTrackAdapter.class.getCanonicalName();
    static final long INITIAL_MUTE_DELAY = 1500;
    static final long MUTE_DELAY = 500;

    private Map<String, TrackMuteUnmuteImpl> muteImplMap = new HashMap<>();

    private Timer timer = new Timer("VideoTrackMutedTimer");

    private final int peerConnectionId;

    private final WebRTCModule webRTCModule;

    public VideoTrackAdapter(WebRTCModule webRTCModule, int peerConnectionId) {
        this.peerConnectionId = peerConnectionId;
        this.webRTCModule = webRTCModule;
    }

    public void addAdapter(String streamReactTag, VideoTrack videoTrack) {
        String trackId = videoTrack.id();
        if (!muteImplMap.containsKey(trackId)) {
            TrackMuteUnmuteImpl onMuteImpl
                = new TrackMuteUnmuteImpl(streamReactTag, trackId);
            Log.d(TAG, "Created adapter for " + trackId);
            muteImplMap.put(trackId, onMuteImpl);
            videoTrack.addRenderer(onMuteImpl.renderer);
            onMuteImpl.scheduleInitialTimeout();
        } else {
            Log.w(
                TAG, "Attempted to add adapter twice for track ID: " + trackId);
        }
    }

    public void removeAdapter(VideoTrack videoTrack) {
        String trackId = videoTrack.id();
        TrackMuteUnmuteImpl onMuteImpl = muteImplMap.remove(trackId);
        if (onMuteImpl != null) {
            videoTrack.removeRenderer(onMuteImpl.renderer);
            onMuteImpl.dispose();
            Log.d(TAG, "Deleted adapter for " + trackId);
        } else {
            Log.w(TAG, "removeAdapter - no adapter for " + trackId);
        }
    }

    /**
     * Implements 'mute'/'unmute' events for remote video tracks through
     * the {@link VideoRenderer#Callbacks} interface.
     *
     * TODO use VideoSink when becomes available in the used version
     */
    private class TrackMuteUnmuteImpl implements VideoRenderer.Callbacks {
        private TimerTask emitMuteTask;
        private volatile boolean disposed;
        private int frameCounter = 0;
        private boolean mutedState;
        private VideoRenderer renderer;
        private final String streamReactTag;
        private final String trackId;

        TrackMuteUnmuteImpl(String streamReactTag, String trackId) {
            this.streamReactTag = streamReactTag;
            this.trackId = trackId;
            this.renderer = new VideoRenderer(this);
        }

        @Override
        public void renderFrame(I420Frame frame) {
            VideoRenderer.renderFrameDone(frame);
            synchronized (this) {
                if (emitMuteTask != null) {
                    emitMuteTask.cancel();
                    emitMuteTask = null;
                }
            }
            if (disposed) {
                return;
            }
            if (this.mutedState) {
                mutedState = false;
                // Do not block the render thread
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        emitMuteEvent(false);
                    }
                }, 0);
            } else {
                scheduleMuteTimeout(MUTE_DELAY);
            }
        }

        private void scheduleInitialTimeout() {
            scheduleMuteTimeout(INITIAL_MUTE_DELAY);
        }

        private void scheduleMuteTimeout(long delay) {
            if (emitMuteTask != null) {
                return;
            }
            emitMuteTask = new TimerTask() {
                private final int lastFrameNumber = frameCounter;
                @Override
                public void run() {
                    if (!disposed && lastFrameNumber == frameCounter) {
                        mutedState = true;
                        emitMuteEvent(true);
                    }
                }
            };
            timer.schedule(emitMuteTask, delay);
        }

        private void emitMuteEvent(boolean muted) {
            WritableMap params = Arguments.createMap();
            params.putInt("peerConnectionId", peerConnectionId);
            params.putString("streamReactTag", streamReactTag);
            params.putString("trackId", trackId);
            params.putBoolean("muted", muted);

            Log.d(TAG,
                (muted ? "Mute" : "Unmute" )
                    + " event pcId: " + peerConnectionId
                    + " streamTag: " + streamReactTag
                    + " trackId: " + trackId);

            VideoTrackAdapter.this.webRTCModule.sendEvent(
                "mediaStreamTrackMuteChanged", params);
        }

        void dispose() {
            disposed = true;
            synchronized (this) {
                if (emitMuteTask != null) {
                    emitMuteTask.cancel();
                }
            }
            // NOTE renderer.dispose is also called by VideoTrack.removeRenderer
            // Not sure if this could cause any issues...
            renderer.dispose();
        }
    }
}
