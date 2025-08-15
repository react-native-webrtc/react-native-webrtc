import Foundation
import OSLog
import VideoToolbox
#if canImport(UIKit)
import UIKit
#endif

// MARK: - VideoEncoder

public final class VideoEncoder {
    // MARK: Lifecycle

    public init(config: Config) {
        self.config = config

        #if canImport(UIKit)
        willEnterForegroundTask = Task { [weak self] in
            for await _ in await NotificationCenter.default.notifications(
                named: UIApplication.willEnterForegroundNotification
            ) {
                self?.invalidate()
            }
        }
        #endif
    }
    
    deinit {
        if let compressionSession = compressionSession {
            VTCompressionSessionInvalidate(compressionSession)
        }
    }

    // MARK: Public

    public var config: Config {
        didSet {
            encodingQueue.sync {
                sessionInvalidated = true
            }
        }
    }

    public var encodedSampleBuffers: AsyncStream<CMSampleBuffer> {
        .init { continuation in
            let id = UUID()
            continuations[id] = continuation
            continuation.onTermination = { [weak self] _ in
                self?.continuations[id] = nil
            }
        }
    }

    public func invalidate() {
        encodingQueue.sync {
            sessionInvalidated = true
        }
    }

    public func encode(_ sampleBuffer: CMSampleBuffer) {
        guard let imageBuffer = sampleBuffer.imageBuffer else {
            Self.logger.error("Invalid sample buffer passed to video encoder; missing imageBuffer")
            return
        }
        encode(
            imageBuffer,
            presentationTimeStamp: sampleBuffer.presentationTimeStamp,
            duration: sampleBuffer.duration
        )
    }

    public func encode(
        _ pixelBuffer: CVPixelBuffer,
        presentationTimeStamp: CMTime = CMClockGetTime(.hostTimeClock),
        duration: CMTime = .invalid
    ) {
        encodingQueue.sync {
            let pixelBufferWidth = CGFloat(CVPixelBufferGetWidth(pixelBuffer))
            let pixelBufferHeight = CGFloat(CVPixelBufferGetHeight(pixelBuffer))
            if pixelBufferWidth != outputSize?.width || pixelBufferHeight != outputSize?.height {
                outputSize = CGSize(width: pixelBufferWidth, height: pixelBufferHeight)
            }

            if compressionSession == nil || sessionInvalidated {
                compressionSession = createCompressionSession()
            }

            guard let compressionSession else {
                Self.logger.error("No compression session")
                return
            }

            guard CVPixelBufferLockBaseAddress(
                pixelBuffer,
                CVPixelBufferLockFlags(rawValue: 0)
            ) == kCVReturnSuccess else {
                return
            }

            defer {
                CVPixelBufferUnlockBaseAddress(
                    pixelBuffer,
                    CVPixelBufferLockFlags(rawValue: 0)
                )
            }

            do {
                try compressionSession.encodeFrame(
                    pixelBuffer,
                    presentationTimeStamp: presentationTimeStamp,
                    duration: duration
                ) { [weak self] status, _, sampleBuffer in
                    guard let self else { return }
                    outputQueue.sync {
                        do {
                            if let error = VideoTranscoderError(status: status) { throw error }
                            guard let sampleBuffer else { return }
                            for continuation in self.continuations.values {
                                continuation.yield(sampleBuffer)
                            }
                        } catch {
                            Self.logger.error("Error in decode frame output handler: \(error, privacy: .public)")
                        }
                    }
                }
            } catch {
                Self.logger.error("Failed to encode frame with error: \(error, privacy: .public)")
            }
        }
    }

    // MARK: Internal

    static let logger = Logger(subsystem: "Transcoding", category: "VideoEncoder")

    var continuations: [UUID: AsyncStream<CMSampleBuffer>.Continuation] = [:]

    var willEnterForegroundTask: Task<Void, Never>?

    lazy var encodingQueue = DispatchQueue(
        label: String(describing: Self.self),
        qos: .userInitiated
    )

    lazy var outputQueue = DispatchQueue(
        label: "\(String(describing: Self.self)).output",
        qos: .userInitiated
    )

    var sessionInvalidated = false {
        didSet {
            dispatchPrecondition(condition: .onQueue(encodingQueue))
        }
    }

    var compressionSession: VTCompressionSession? {
        didSet {
            dispatchPrecondition(condition: .onQueue(encodingQueue))
            if let oldValue { VTCompressionSessionInvalidate(oldValue) }
            sessionInvalidated = false
        }
    }

    var outputSize: CGSize? {
        didSet {
            dispatchPrecondition(condition: .onQueue(encodingQueue))
            sessionInvalidated = true
        }
    }

    func createCompressionSession() -> VTCompressionSession? {
        dispatchPrecondition(condition: .onQueue(encodingQueue))
        do {
            let session = try VTCompressionSession.create(
                size: outputSize ?? CGSize(width: 1920, height: 1080),
                codecType: config.codecType,
                encoderSpecification: config.encoderSpecification
            )
            config.apply(to: session)
            VTCompressionSessionPrepareToEncodeFrames(session)
            return session
        } catch {
            Self.logger.error("Failed to create compression session with error: \(error, privacy: .public)")
            return nil
        }
    }
}
