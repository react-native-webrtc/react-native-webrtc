package com.oney.WebRTCModule.videoEffects;

import org.webrtc.SurfaceTextureHelper;
import org.webrtc.VideoFrame;

/**
 * Interface contains process method to process VideoFrame.
 * The caller takes ownership of the object.
 */
public interface VideoFrameProcessor {
    /**
     * Applies the image processing algorithms to the frame. Returns the processed frame.
     * The caller is responsible for releasing the returned frame.
     * @param frame raw videoframe which need to be processed
     * @param textureHelper
     * @return processed videoframe which will rendered
     */
    public VideoFrame process(VideoFrame frame, SurfaceTextureHelper textureHelper);
}
