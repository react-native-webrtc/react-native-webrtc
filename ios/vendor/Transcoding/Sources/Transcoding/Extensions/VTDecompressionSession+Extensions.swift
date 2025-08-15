import Foundation
import VideoToolbox

extension VTDecompressionSession {
    static func create(
        formatDescription: CMVideoFormatDescription,
        decoderSpecification: CFDictionary,
        imageBufferAttributes: CFDictionary?
    ) throws -> VTDecompressionSession {
        var session: VTDecompressionSession?
        let status = VTDecompressionSessionCreate(
            allocator: nil,
            formatDescription: formatDescription,
            decoderSpecification: decoderSpecification,
            imageBufferAttributes: imageBufferAttributes,
            outputCallback: nil,
            decompressionSessionOut: &session
        )
        if let error = VideoTranscoderError(status: status) {
            throw error
        }
        guard let session else { throw VideoTranscoderError.unknown(status) }
        return session
    }

    func decodeFrame(
        _ sampleBuffer: CMSampleBuffer,
        flags decodeFlags: VTDecodeFrameFlags = [],
        outputHandler: @escaping VTDecompressionOutputHandler
    ) throws {
        var infoFlagsOut = VTDecodeInfoFlags()
        let status = VTDecompressionSessionDecodeFrame(
            self,
            sampleBuffer: sampleBuffer,
            flags: decodeFlags,
            infoFlagsOut: &infoFlagsOut,
            outputHandler: outputHandler
        )
        if let error = VideoTranscoderError(status: status) { throw error }
    }
}
