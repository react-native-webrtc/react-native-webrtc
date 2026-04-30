import Foundation
import CoreMedia
import WebRTC
import CoreImage
import UIKit
import ReplayKit

@objc public class ScreenCapturer: RTCVideoCapturer, StreamDelegate {
    
    private var connection: SocketConnection?
    
    private var cache: Data?
    
    private var videoDecoder: CustomVideoDecoderTranscoding
    
    @objc public weak var eventsDelegate: CapturerEventsDelegate?
    
    private var startPresentationTimeStamp: Float64?
    private var presentationTimeStamps: [Float64] = []
    private var orientations: [UInt] = []
    
    @objc override public init(delegate: RTCVideoCapturerDelegate) {
        videoDecoder = CustomVideoDecoderTranscoding()
        
        super.init(delegate: delegate)
        
        Task<Void, Never> {
            for await decodedSampleBuffer in videoDecoder.decodedSampleBuffers {

                let framePresentationTimeStamp = presentationTimeStamps.removeFirst()
                let orientation = orientations.removeFirst()
                
                if startPresentationTimeStamp == nil {
                    startPresentationTimeStamp = framePresentationTimeStamp
                }
                
                let frameTimeStampNs = Int64((framePresentationTimeStamp - startPresentationTimeStamp!) * 1_000_000_000)
                
                guard let pixelBuffer = CMSampleBufferGetImageBuffer(decodedSampleBuffer) else { return }
                let rtcPixelBuffer = RTCCVPixelBuffer(pixelBuffer: pixelBuffer)
                var rotation = RTCVideoRotation._0
                switch CGImagePropertyOrientation(rawValue: UInt32(orientation))! {
                case CGImagePropertyOrientation.left:
                    rotation = ._90
                case CGImagePropertyOrientation.down:
                    rotation = ._180
                case CGImagePropertyOrientation.right:
                    rotation = ._270
                default:
                    break
                }

                let videoFrame = RTCVideoFrame(buffer: rtcPixelBuffer, rotation: rotation, timeStampNs: frameTimeStampNs)
                
                self.delegate?.capturer(self, didCapture: videoFrame)
            }
        }
    }

    @objc public func startCapture(connection: SocketConnection) {
        print("ScreenCapturer: start capture")
        self.connection = connection
        
        connection.open(with: self)
    }

    @objc public func stopCapture() {
        print("ScreenCapturer: stop capture")
        connection = nil;
    }
    
    @objc public func stream(_ aStream: Stream, handle eventCode: Stream.Event) {
        switch eventCode {
            case .openCompleted:
                print("ScreenCapturer: server stream open completed")
                break
            case .hasBytesAvailable:
                if let inputStream = aStream as? InputStream {
                    readFromStream(inputStream)
                } else {
                    print("ScreenCapturer: failed to read from non-input stream: \(aStream)")
                }
                break
            case .endEncountered:
                print("ScreenCapturer: server stream end encountered")
                self.stopCapture()
                self.eventsDelegate?.capturerDidEnd(self)
                break
            case .errorOccurred:
                print("ScreenCapturer: server stream error encountered: \(String(describing: aStream.streamError))")
                break
            default:
                break
        }
    }
    
    private func readFromStream(_ stream: InputStream) {
        if !stream.hasBytesAvailable {
            return
        }
        
        var buffer = [UInt8](repeating: 0, count: 4096)
        
        let bytesRead = stream.read(&buffer, maxLength: buffer.count)
        
        if bytesRead <= 0 {
            return
        }
        
        if cache == nil {
            cache = Data()
        }
        
        cache!.append(contentsOf: buffer[0..<bytesRead])

        // first 4 bytes indicate the length of the message
        guard cache!.count >= MemoryLayout<UInt32>.size else { return }
        
        let length = cache!.prefix(MemoryLayout<UInt32>.size).withUnsafeBytes { $0.load(as: UInt32.self).bigEndian }
                
        // wait until the message is fully delivered
        guard cache!.count >= MemoryLayout<UInt32>.size + Int(length) else { return }
        
        let messageData = cache!.subdata(in: MemoryLayout<UInt32>.size..<(MemoryLayout<UInt32>.size + Int(length)))

        cache!.removeSubrange(0..<(MemoryLayout<UInt32>.size + Int(length)))

        processMessage(messageData)
    }
    
    private func processMessage(_ message: Data) {
        let presentationTimeStampData = message.subdata(in: 0..<MemoryLayout<Float64>.size)
        let orientationData = message.subdata(in: MemoryLayout<Float64>.size..<(MemoryLayout<Float64>.size + MemoryLayout<UInt>.size))
        let imageData = message.subdata(in: (MemoryLayout<Float64>.size + MemoryLayout<UInt>.size)..<message.count)
        
        let presentationTimeStamp = presentationTimeStampData.withUnsafeBytes {
            $0.load(as: Float64.self)
        }
        presentationTimeStamps.append(presentationTimeStamp)
        
        let orientation = orientationData.withUnsafeBytes {
            $0.load(as: UInt.self)
        }
        orientations.append(orientation)
        
        videoDecoder.decode(imageData)
    }
}

class CustomVideoDecoderTranscoding {
    private let videoDecoder: VideoDecoder
    private let videoDecoderAnnexBAdaptor: VideoDecoderAnnexBAdaptor

    init() {
        videoDecoder = VideoDecoder(config: .init(realTime: true))
        videoDecoderAnnexBAdaptor = VideoDecoderAnnexBAdaptor(
            videoDecoder: videoDecoder,
            codec: .h264
        )
    }

    func decode(_ data: Data) {
        videoDecoderAnnexBAdaptor.decode(data)
    }

    var decodedSampleBuffers: AsyncStream<CMSampleBuffer> {
        videoDecoder.decodedSampleBuffers
    }
}

class CustomVideoDecoderJPEG {
    private var imageContext = CIContext(options: nil)

    private var continuation: AsyncStream<CMSampleBuffer>.Continuation?
    public let decodedSampleBuffers: AsyncStream<CMSampleBuffer>

    init() {
        (decodedSampleBuffers, continuation) = AsyncStream.makeStream(of: CMSampleBuffer.self)
    }

    func decode(_ data: Data) {
        guard let image = CIImage(data: data) else {
            print("image is nil")
            return
        }
        
        var pixelBuffer: CVPixelBuffer?

        var status = CVPixelBufferCreate(kCFAllocatorDefault, Int(image.extent.width), Int(image.extent.height), kCVPixelFormatType_32BGRA, nil, &pixelBuffer)

        if status != kCVReturnSuccess {
            print("failed to create pixel buffer")
            return
        }
        
        guard let pixelBuffer = pixelBuffer else {
            print("pixel buffer is nil")
            return
        }
        
        CVPixelBufferLockBaseAddress(pixelBuffer, .readOnly)

        imageContext.render(image, to: pixelBuffer)

        var sampleBuffer: CMSampleBuffer?
        var videoInfo: CMVideoFormatDescription?

        // Step 1: Create format description
        status = CMVideoFormatDescriptionCreateForImageBuffer(allocator: kCFAllocatorDefault,
                                                                imageBuffer: pixelBuffer,
                                                                formatDescriptionOut: &videoInfo)
        guard status == kCMBlockBufferNoErr, let formatDescription = videoInfo else {
            print("Error creating format description: \(status)")
            return
        }

        // Step 2: Create timing info
        var timingInfo = CMSampleTimingInfo(duration: CMTime.invalid,
                                            presentationTimeStamp: CMTime.invalid,
                                            decodeTimeStamp: CMTime.invalid)

        // Step 3: Create CMSampleBuffer
        status = CMSampleBufferCreateReadyWithImageBuffer(allocator: kCFAllocatorDefault,
                                                                imageBuffer: pixelBuffer,
                                                                formatDescription: formatDescription,
                                                                sampleTiming: &timingInfo,
                                                                sampleBufferOut: &sampleBuffer)
        if status != noErr {
            print("Error creating sample buffer: \(status)")
            return
        }

        CVPixelBufferUnlockBaseAddress(pixelBuffer, .readOnly)
        
        guard let sampleBuffer = sampleBuffer else {
            print("sampleBuffer is nil")
            return
        }

        continuation?.yield(sampleBuffer)
    }
}
