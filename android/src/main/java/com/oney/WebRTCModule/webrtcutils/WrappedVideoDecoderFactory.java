/*
 * Copyright (c) 2014-2025 Stream.io Inc. All rights reserved.
 *
 * Licensed under the Stream License;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://github.com/GetStream/stream-video-android/blob/main/LICENSE
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.oney.WebRTCModule.webrtcutils;

import androidx.annotation.Nullable;

import org.webrtc.*;
import java.util.Arrays;
import java.util.LinkedHashSet;

/**
 * A patch on top of  https://github.com/GetStream/webrtc/blob/main/sdk/android/api/org/webrtc/WrappedVideoDecoderFactory.java
 * It disables direct-to-SurfaceTextureFrame rendering for c2 exynos/qualcomm/mediatek hardware decoder
 */
public class WrappedVideoDecoderFactory implements VideoDecoderFactory {
    // Known hardware decoders to have failures when it outputs to a SurfaceTexture directly
    private static final String[] DECODER_DENYLIST_PREFIXES = {
            "OMX.qcom.",
            "OMX.hisi.",
            // https://github.com/androidx/media/issues/2003
//            "c2.exynos.",
//            "c2.qti.",
//            // https://github.com/androidx/media/blob/bfe5930f7f29c6492d60e3d01a90abd3c138b615/libraries/exoplayer/src/main/java/androidx/media3/exoplayer/video/MediaCodecVideoRenderer.java#L1499
//            "c2.mtk.",
    };

    private final boolean forceSWCodec;

    public WrappedVideoDecoderFactory(@Nullable EglBase.Context eglContext, boolean forceSWCodec) {
        this.hardwareVideoDecoderFactory = new HardwareVideoDecoderFactory(eglContext);
        this.platformSoftwareVideoDecoderFactory = new PlatformSoftwareVideoDecoderFactory(eglContext);
        this.forceSWCodec = forceSWCodec;
    }

    private final VideoDecoderFactory hardwareVideoDecoderFactory;
    private final VideoDecoderFactory hardwareVideoDecoderFactoryWithoutEglContext = new HardwareVideoDecoderFactory(null)   ;
    private final VideoDecoderFactory softwareVideoDecoderFactory = new SoftwareVideoDecoderFactory();
    @Nullable
    private final VideoDecoderFactory platformSoftwareVideoDecoderFactory;

    @Override
    public VideoDecoder createDecoder(VideoCodecInfo codecType) {
        VideoDecoder softwareDecoder = this.softwareVideoDecoderFactory.createDecoder(codecType);
        VideoDecoder hardwareDecoder = null;
        if (!forceSWCodec) {
            hardwareDecoder = this.hardwareVideoDecoderFactory.createDecoder(codecType);
        }
        if (softwareDecoder == null && this.platformSoftwareVideoDecoderFactory != null) {
            softwareDecoder = this.platformSoftwareVideoDecoderFactory.createDecoder(codecType);
        }
        if(hardwareDecoder != null && disableSurfaceTextureFrame(hardwareDecoder.getImplementationName())) {
            hardwareDecoder.release();
            hardwareDecoder = this.hardwareVideoDecoderFactoryWithoutEglContext.createDecoder(codecType);
        }

        if (hardwareDecoder != null && softwareDecoder != null) {
            return new VideoDecoderFallback(softwareDecoder, hardwareDecoder);
        } else {
            return hardwareDecoder != null ? hardwareDecoder : softwareDecoder;
        }
    }

    private boolean disableSurfaceTextureFrame(String name) {
        for (String prefix : DECODER_DENYLIST_PREFIXES) {
            if (name.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public VideoCodecInfo[] getSupportedCodecs() {
        LinkedHashSet<VideoCodecInfo> supportedCodecInfos = new LinkedHashSet<>();
        supportedCodecInfos.addAll(Arrays.asList(this.softwareVideoDecoderFactory.getSupportedCodecs()));
        if (!forceSWCodec) {
            supportedCodecInfos.addAll(Arrays.asList(this.hardwareVideoDecoderFactory.getSupportedCodecs()));
        }
        if (this.platformSoftwareVideoDecoderFactory != null) {
            supportedCodecInfos.addAll(Arrays.asList(this.platformSoftwareVideoDecoderFactory.getSupportedCodecs()));
        }

        return supportedCodecInfos.toArray(new VideoCodecInfo[supportedCodecInfos.size()]);
    }
}
