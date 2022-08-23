package com.oney.WebRTCModule.videoEffect;

import org.webrtc.*;

public class VideoEffectProcessor implements VideoProcessor {
    private SurfaceTextureHelper textureHelper;
    private VideoSink mSink;
    private VideoFrameProcessor videoFrameProcessor = null;

    public VideoEffectProcessor(SurfaceTextureHelper textureHelper) {
        this.textureHelper = textureHelper;
    }

    public VideoEffectProcessor(VideoFrameProcessor processor, SurfaceTextureHelper textureHelper) {
        this.textureHelper = textureHelper;
        this.videoFrameProcessor = processor;
    }

    @Override
    public void onCapturerStarted(boolean success) {

    }

    @Override
    public void onCapturerStopped() {

    }

    @Override
    public void setSink(VideoSink sink) {
        mSink = sink;
    }

    @Override
    public void onFrameCaptured(VideoFrame frame) {

        VideoFrame out = videoFrameProcessor.process(frame, textureHelper);
        mSink.onFrame(out);
        frame.release();
    }

}
