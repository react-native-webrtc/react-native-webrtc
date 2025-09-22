import Foundation
import VideoToolbox

extension VTCompressionSession {
    static func create(
        size: CGSize,
        codecType: CMVideoCodecType,
        encoderSpecification: CFDictionary
    ) throws -> VTCompressionSession {
        var session: VTCompressionSession?
        let status = VTCompressionSessionCreate(
            allocator: nil,
            width: Int32(size.width),
            height: Int32(size.height),
            codecType: codecType,
            encoderSpecification: encoderSpecification,
            imageBufferAttributes: nil,
            compressedDataAllocator: nil,
            outputCallback: nil,
            refcon: nil,
            compressionSessionOut: &session
        )
        if let error = VideoTranscoderError(status: status) {
            throw error
        }
        guard let session else { throw VideoTranscoderError.unknown(status) }
        return session
    }

    func encodeFrame(
        _ pixelBuffer: CVPixelBuffer,
        presentationTimeStamp: CMTime,
        duration: CMTime,
        outputHandler: @escaping VTCompressionOutputHandler
    ) throws {
        var infoFlagsOut = VTEncodeInfoFlags()
        let status = VTCompressionSessionEncodeFrame(
            self,
            imageBuffer: pixelBuffer,
            presentationTimeStamp: presentationTimeStamp,
            duration: duration,
            frameProperties: nil,
            infoFlagsOut: &infoFlagsOut,
            outputHandler: outputHandler
        )
        if let error = VideoTranscoderError(status: status) {
            throw error
        }
    }
}
