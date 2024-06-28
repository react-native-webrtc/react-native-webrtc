package com.oney.rnwebrtc.processors;

/**
 * Factory for creating VideoFrameProcessor instances.
 */
public interface VideoFrameProcessorFactoryInterface {
    /**
     * Dynamically allocates a VideoFrameProcessor instance and returns a pointer to it.
     * The caller takes ownership of the object.
     */
    VideoFrameProcessor build();
}
