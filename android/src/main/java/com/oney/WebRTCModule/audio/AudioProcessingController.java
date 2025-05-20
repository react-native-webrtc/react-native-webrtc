package com.oney.WebRTCModule.audio;

import org.webrtc.AudioProcessingFactory;
import org.webrtc.ExternalAudioProcessingFactory;

public class AudioProcessingController implements AudioProcessingFactoryProvider {
    /**
     * This is the audio processing module that will be applied to the audio stream after it is captured from the microphone.
     * This is useful for adding echo cancellation, noise suppression, etc.
     */
    public final AudioProcessingAdapter capturePostProcessing = new AudioProcessingAdapter();
    /**
     * This is the audio processing module that will be applied to the audio stream before it is rendered to the speaker.
     */
    public final AudioProcessingAdapter renderPreProcessing = new AudioProcessingAdapter();

    public ExternalAudioProcessingFactory externalAudioProcessingFactory;

    public AudioProcessingController() {
        this.externalAudioProcessingFactory = new ExternalAudioProcessingFactory();
        this.externalAudioProcessingFactory.setCapturePostProcessing(capturePostProcessing);
        this.externalAudioProcessingFactory.setRenderPreProcessing(renderPreProcessing);
    }

    @Override
    public AudioProcessingFactory getFactory() {
        return this.externalAudioProcessingFactory;
    }
}