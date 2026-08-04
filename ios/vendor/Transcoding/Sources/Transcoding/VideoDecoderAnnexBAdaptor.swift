import CoreMedia
import Foundation
import OSLog

// MARK: - VideoDecoderAnnexBAdaptor

public final class VideoDecoderAnnexBAdaptor {
    // MARK: Lifecycle

    public init(
        videoDecoder: VideoDecoder,
        codec: Codec
    ) {
        self.videoDecoder = videoDecoder
        self.codec = codec
    }

    // MARK: Public

    public func decode(_ data: Data) {
        switch codec {
        case .h264:
            decodeH264(data)
        case .hevc:
            decodeHEVC(data)
        }
    }

    // MARK: Internal

    static let logger = Logger(subsystem: "Transcoding", category: "VideoDecoderAnnexBAdaptor")

    let videoDecoder: VideoDecoder
    let codec: Codec
    var formatDescription: CMVideoFormatDescription?

    var vps: Data?
    var sps: Data?
    var pps: Data?

    func decodeH264(_ data: Data) {
        for nalu in data.split(separator: H264NALU.startCode).map({ H264NALU(data: Data($0)) }) {
            if nalu.isSPS {
                sps = nalu.data
            } else if nalu.isPPS {
                pps = nalu.data
            } else if nalu.isPFrame || nalu.isIFrame {
                if nalu.isIFrame, let sps, let pps {
                    do {
                        let formatDescription = try CMVideoFormatDescription(h264ParameterSets: [sps, pps])
                        videoDecoder.setFormatDescription(formatDescription)
                        self.formatDescription = formatDescription
                    } catch {
                        Self.logger.error("Failed to create format description with error: \(error, privacy: .public)")
                    }
                }
                decodeAVCCFrame(nalu.avcc)
            }
        }
    }

    func decodeHEVC(_ data: Data) {
        for nalu in data.split(separator: HEVCNALU.startCode).map({ HEVCNALU(data: Data($0)) }) {
            if nalu.isVPS {
                vps = nalu.data
            } else if nalu.isSPS {
                sps = nalu.data
            } else if nalu.isPPS {
                pps = nalu.data
            } else if nalu.isPFrame || nalu.isIFrame {
                if nalu.isIFrame, let vps, let sps, let pps {
                    do {
                        let formatDescription = try CMVideoFormatDescription(hevcParameterSets: [vps, sps, pps])
                        videoDecoder.setFormatDescription(formatDescription)
                        self.formatDescription = formatDescription
                    } catch {
                        Self.logger.error("Failed to create format description with error: \(error, privacy: .public)")
                    }
                }
                decodeAVCCFrame(nalu.avcc)
            }
        }
    }

    func decodeAVCCFrame(_ data: Data) {
        guard let formatDescription else {
            Self.logger.warning("No format description; need sync frame")
            return
        }
        var data = data
        data.withUnsafeMutableBytes { pointer in
            do {
                let dataBuffer = try CMBlockBuffer(buffer: pointer, allocator: kCFAllocatorNull)
                let sampleBuffer = try CMSampleBuffer(
                    dataBuffer: dataBuffer,
                    formatDescription: formatDescription,
                    numSamples: 1,
                    sampleTimings: [],
                    sampleSizes: []
                )
                videoDecoder.decode(sampleBuffer)
            } catch {
                Self.logger.error("Failed to create sample buffer with error: \(error, privacy: .public)")
            }
        }
    }
}

// MARK: VideoDecoderAnnexBAdaptor.Codec

public extension VideoDecoderAnnexBAdaptor {
    enum Codec {
        case h264
        case hevc
    }
}
