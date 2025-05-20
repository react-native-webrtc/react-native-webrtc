package com.oney.WebRTCModule.audio;

import org.webrtc.AudioProcessingFactory;

// Define the common interface
public interface AudioProcessingFactoryProvider {
    AudioProcessingFactory getFactory();
}