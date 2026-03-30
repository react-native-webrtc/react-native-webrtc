package com.oney.WebRTCModule;

import org.webrtc.Loggable;
import org.webrtc.Logging;
import org.webrtc.VideoDecoderFactory;
import org.webrtc.VideoEncoderFactory;
import org.webrtc.audio.AudioDeviceModule;

public class WebRTCModuleOptions {
    public VideoEncoderFactory videoEncoderFactory;
    public VideoDecoderFactory videoDecoderFactory;
    public AudioDeviceModule audioDeviceModule;
    public Loggable injectableLogger;
    public Logging.Severity loggingSeverity;
    public String fieldTrials;
    public boolean enableMediaProjectionService;

    private WebRTCModuleOptions() {}

    private static class Holder {
        static final WebRTCModuleOptions instance = new WebRTCModuleOptions();
    }

    public static WebRTCModuleOptions getInstance() {
        return Holder.instance;
    }
}
