import CoreMedia
import Foundation
import OSLog

public final class VideoEncoderAnnexBAdaptor {
    // MARK: Lifecycle

    public init(videoEncoder: VideoEncoder) {
        self.videoEncoder = videoEncoder
        conversionTask = Task { [weak self] in
            for await sampleBuffer in videoEncoder.encodedSampleBuffers {
                guard let self else { return }
                let sampleAttachments = CMSampleBufferGetSampleAttachmentsArray(
                    sampleBuffer,
                    createIfNecessary: false
                ) as? [[CFString: Any]]
                let notSync = sampleAttachments?.first?[kCMSampleAttachmentKey_NotSync] as? Bool ?? false

                var elementaryStream = Data()

                if !notSync {
                    guard let formatDesciption = sampleBuffer.formatDescription else {
                        Self.logger.error("Encoded sample buffer missing format description")
                        continue
                    }
                    switch formatDesciption.mediaSubType {
                    case .h264:
                        guard formatDesciption.parameterSets.count > 1 else {
                            Self.logger.error("Encoded sample buffer missing parameter set")
                            continue
                        }
                        elementaryStream += H264NALU(data: formatDesciption.parameterSets[0]).annexB
                        elementaryStream += H264NALU(data: formatDesciption.parameterSets[1]).annexB

                    case .hevc:
                        guard formatDesciption.parameterSets.count > 2 else {
                            Self.logger.error("Encoded sample buffer missing parameter set")
                            continue
                        }
                        elementaryStream += HEVCNALU(data: formatDesciption.parameterSets[0]).annexB
                        elementaryStream += HEVCNALU(data: formatDesciption.parameterSets[1]).annexB
                        elementaryStream += HEVCNALU(data: formatDesciption.parameterSets[2]).annexB
                    default:
                        Self.logger.error("Encoded sample buffer has unsupported media sub type")
                        continue
                    }
                }
                guard let dataBuffer = CMSampleBufferGetDataBuffer(sampleBuffer) else {
                    print("CMSampleBufferGetDataBuffer returned nil")
                    continue
                }
                var length: Int = 0
                var dataPointer: UnsafeMutablePointer<Int8>?
                let status = CMBlockBufferGetDataPointer(
                    dataBuffer,
                    atOffset: 0,
                    lengthAtOffsetOut: nil,
                    totalLengthOut: &length,
                    dataPointerOut: &dataPointer
                )
                guard status == noErr, let dataPointer else {
                    print("CMBlockBufferGetDataPointer failed with status: \(status)")
                    continue
                }

                var offset = 0
                while offset < length {
                    var naluLength: UInt32 = 0
                    memcpy(&naluLength, dataPointer.advanced(by: offset), 4)
                    offset += 4

                    switch sampleBuffer.formatDescription?.mediaSubType {
                    case .some(.h264):
                        elementaryStream += H264NALU(data: Data(
                            bytes: dataPointer.advanced(by: offset),
                            count: Int(naluLength.bigEndian)
                        )).annexB
                    case .some(.hevc):
                        elementaryStream += HEVCNALU(data: Data(
                            bytes: dataPointer.advanced(by: offset),
                            count: Int(naluLength.bigEndian)
                        )).annexB
                    default:
                        break
                    }

                    offset += Int(naluLength.bigEndian)
                }

                for continuation in continuations.values {
                    continuation.yield(elementaryStream)
                }
            }
        }
    }

    // MARK: Public

    public var annexBData: AsyncStream<Data> {
        .init { continuation in
            let id = UUID()
            continuations[id] = continuation
            continuation.onTermination = { [weak self] _ in
                self?.continuations[id] = nil
            }
        }
    }

    // MARK: Internal

    static let logger = Logger(subsystem: "Transcoding", category: "VideoEncoderAnnexBAdaptor")

    let videoEncoder: VideoEncoder
    var conversionTask: Task<Void, Never>?

    var continuations: [UUID: AsyncStream<Data>.Continuation] = [:]
}
