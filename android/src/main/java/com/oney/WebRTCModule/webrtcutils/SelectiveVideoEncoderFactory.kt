/*
 * Copyright (c) 2014-2024 Stream.io Inc. All rights reserved.
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

package com.oney.WebRTCModule.webrtcutils

import org.webrtc.EglBase
import org.webrtc.ResolutionAdjustment
import org.webrtc.SimulcastAlignedVideoEncoderFactory
import org.webrtc.SoftwareVideoEncoderFactory
import org.webrtc.VideoCodecInfo
import org.webrtc.VideoEncoder
import org.webrtc.VideoEncoderFactory

internal class SelectiveVideoEncoderFactory(
    sharedContext: EglBase.Context?,
    enableIntelVp8Encoder: Boolean,
    enableH264HighProfile: Boolean,
    private var forceSWCodec: Boolean = false,
    private var forceSWCodecs: List<String> = listOf("VP9", "AV1"),
) : VideoEncoderFactory {
    private val softwareVideoEncoderFactory = SoftwareVideoEncoderFactory()
    private val simulcastVideoEncoderFactoryWrapper: SimulcastAlignedVideoEncoderFactory

    init {
        simulcastVideoEncoderFactoryWrapper =
            SimulcastAlignedVideoEncoderFactory(sharedContext, enableIntelVp8Encoder, enableH264HighProfile, ResolutionAdjustment.NONE)
    }

    /**
     * Set to true to force software codecs.
     */
    fun setForceSWCodec(forceSWCodec: Boolean) {
        this.forceSWCodec = forceSWCodec
    }

    /**
     * Set a list of codecs for which to use software codecs.
     */
    fun setForceSWCodecList(forceSWCodecs: List<String>) {
        this.forceSWCodecs = forceSWCodecs
    }

    override fun createEncoder(videoCodecInfo: VideoCodecInfo): VideoEncoder? {
        if (forceSWCodec) {
            return softwareVideoEncoderFactory.createEncoder(videoCodecInfo)
        }
        if (forceSWCodecs.isNotEmpty()) {
            if (forceSWCodecs.contains(videoCodecInfo.name)) {
                return softwareVideoEncoderFactory.createEncoder(videoCodecInfo)
            }
        }
        return simulcastVideoEncoderFactoryWrapper.createEncoder(videoCodecInfo)
    }

    override fun getSupportedCodecs(): Array<VideoCodecInfo> {
        return if (forceSWCodec && forceSWCodecs.isEmpty()) {
            softwareVideoEncoderFactory.supportedCodecs
        } else {
            simulcastVideoEncoderFactoryWrapper.supportedCodecs
        }
    }
}
