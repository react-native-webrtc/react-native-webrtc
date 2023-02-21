package com.oney.WebRTCModule.videoEffects;

/**
 * Factory for creating VideoFrameProcessor instances.
 */
public interface VideoFrameProcessorFactoryInterface {
    /**
     * Dynamically allocates a VideoFrameProcessor instance and returns a pointer to it.
     * The caller takes ownership of the object.
     */
    public VideoFrameProcessor build();
}
