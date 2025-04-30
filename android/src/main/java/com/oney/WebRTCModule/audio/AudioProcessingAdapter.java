package com.oney.WebRTCModule.audio;

import org.webrtc.ExternalAudioProcessingFactory;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class AudioProcessingAdapter implements ExternalAudioProcessingFactory.AudioProcessing {
    public interface ExternalAudioFrameProcessing {
        void initialize(int sampleRateHz, int numChannels);

        void reset(int newRate);

        void process(int numBands, int numFrames, ByteBuffer buffer);
    }

    public AudioProcessingAdapter() {}
    List<ExternalAudioFrameProcessing> audioProcessors = new ArrayList<>();

    public void addProcessor(ExternalAudioFrameProcessing audioProcessor) {
        synchronized (audioProcessors) {
            audioProcessors.add(audioProcessor);
        }
    }

    public void removeProcessor(ExternalAudioFrameProcessing audioProcessor) {
        synchronized (audioProcessors) {
            audioProcessors.remove(audioProcessor);
        }
    }

    @Override
    public void initialize(int sampleRateHz, int numChannels) {
        synchronized (audioProcessors) {
            for (ExternalAudioFrameProcessing audioProcessor : audioProcessors) {
                audioProcessor.initialize(sampleRateHz, numChannels);
            }
        }
    }

    @Override
    public void reset(int newRate) {
        synchronized (audioProcessors) {
            for (ExternalAudioFrameProcessing audioProcessor : audioProcessors) {
                audioProcessor.reset(newRate);
            }
        }
    }

    @Override
    public void process(int numBands, int numFrames, ByteBuffer buffer) {
        synchronized (audioProcessors) {
            for (ExternalAudioFrameProcessing audioProcessor : audioProcessors) {
                audioProcessor.process(numBands, numFrames, buffer);
            }
        }
    }
}