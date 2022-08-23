package com.oney.WebRTCModule.videoEffect;

import org.webrtc.*;

public interface VideoFrameProcessor {

    public VideoFrame process(VideoFrame frame, SurfaceTextureHelper textureHelper);
}
