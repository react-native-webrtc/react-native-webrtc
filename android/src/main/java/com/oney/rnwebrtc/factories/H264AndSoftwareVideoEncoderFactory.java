package com.oney.rnwebrtc.factories;

import androidx.annotation.Nullable;

import org.webrtc.EglBase;
import org.webrtc.HardwareVideoEncoderFactory;
import org.webrtc.VideoCodecInfo;
import org.webrtc.VideoEncoder;
import org.webrtc.VideoEncoderFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This is a custom video encoder factory for WebRTC which behaves similarly
 * to the default one in iOS. It supports the following codecs:
 * - In hardware: H.264 (high, baseline)
 * - In software: VP8, VP9, AV1
 */
public class H264AndSoftwareVideoEncoderFactory implements VideoEncoderFactory {
    private final VideoEncoderFactory hardwareVideoEncoderFactory;
    private final VideoEncoderFactory softwareVideoEncoderFactory;

    public H264AndSoftwareVideoEncoderFactory(@Nullable EglBase.Context eglContext) {
        hardwareVideoEncoderFactory = new HardwareVideoEncoderFactory(eglContext, false, true);
        softwareVideoEncoderFactory = new SoftwareVideoEncoderFactoryProxy();
    }

    @Nullable
    @Override
    public VideoEncoder createEncoder(VideoCodecInfo codecInfo) {
        if (codecInfo.name.equalsIgnoreCase("H264")) {
            return hardwareVideoEncoderFactory.createEncoder(codecInfo);
        }

        return softwareVideoEncoderFactory.createEncoder(codecInfo);
    }

    @Override
    public VideoCodecInfo[] getSupportedCodecs() {
        List<VideoCodecInfo> codecs = new ArrayList<>();

        VideoCodecInfo h264Baseline = null;
        VideoCodecInfo h264High = null;

        VideoCodecInfo[] hwCodecs = hardwareVideoEncoderFactory.getSupportedCodecs();
        for (VideoCodecInfo hwCodec : hwCodecs) {
            if (hwCodec.name.equalsIgnoreCase("H264")) {
                String profileLevel = hwCodec.params.get(VideoCodecInfo.H264_FMTP_PROFILE_LEVEL_ID);
                if (profileLevel == null) {
                    continue;
                }
                if (profileLevel.equalsIgnoreCase(VideoCodecInfo.H264_CONSTRAINED_HIGH_3_1)) {
                    h264High = hwCodec;
                } else if (profileLevel.equalsIgnoreCase(VideoCodecInfo.H264_CONSTRAINED_BASELINE_3_1)) {
                    h264Baseline = hwCodec;
                }
            }
        }

        if (h264High != null) {
            codecs.add(h264High);
        }
        if (h264Baseline != null) {
            codecs.add(h264Baseline);
        }
        codecs.addAll(Arrays.asList(softwareVideoEncoderFactory.getSupportedCodecs()));

        return codecs.toArray(new VideoCodecInfo[codecs.size()]);
    }
}
