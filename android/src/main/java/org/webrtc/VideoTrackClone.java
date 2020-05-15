package org.webrtc;

public class VideoTrackClone extends VideoTrack {
    public VideoTrackClone(long nativeTrack) {
        super(nativeTrack);
    }

    public VideoTrackClone(MediaStreamTrack source) {
        super(source.getNativeMediaStreamTrack());
    }
}
