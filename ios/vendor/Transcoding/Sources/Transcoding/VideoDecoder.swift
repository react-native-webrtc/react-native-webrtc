import Foundation
import OSLog
import VideoToolbox
#if canImport(UIKit)
import UIKit
#endif

// MARK: - VideoDecoder

public final class VideoDecoder {
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
        if let decompressionSession = decompressionSession {
            VTDecompressionSessionInvalidate(decompressionSession)
        }
    }

    // MARK: Public

    public var config: Config {
        didSet {
            decodingQueue.sync {
                sessionInvalidated = true
            }
        }
    }

    public var decodedSampleBuffers: AsyncStream<CMSampleBuffer> {
        .init { continuation in
            let id = UUID()
            continuations[id] = continuation
            continuation.onTermination = { [weak self] _ in
                self?.continuations[id] = nil
            }
        }
    }

    public func invalidate() {
        decodingQueue.sync {
            sessionInvalidated = true
        }
    }

    public func setFormatDescription(_ formatDescription: CMFormatDescription) {
        decodingQueue.sync {
            self.formatDescription = formatDescription
        }
    }

    public func decode(_ sampleBuffer: CMSampleBuffer) {
        decodingQueue.sync {
            if decompressionSession == nil || sessionInvalidated {
                decompressionSession = createDecompressionSession()
            }

            guard let decompressionSession else {
                Self.logger.error("No decompression session")
                return
            }

            do {
                try decompressionSession.decodeFrame(
                    sampleBuffer,
                    flags: [._1xRealTimePlayback]
                ) { [weak self] status, _, imageBuffer, presentationTimeStamp, presentationDuration in
                    guard let self else { return }
                    outputQueue.sync {
                        do {
                            if let error = VideoTranscoderError(status: status) { throw error }
                            guard let imageBuffer else { return }
                            let formatDescription = try CMVideoFormatDescription(imageBuffer: imageBuffer)
                            let sampleTiming = CMSampleTimingInfo(
                                duration: presentationDuration,
                                presentationTimeStamp: presentationTimeStamp,
                                decodeTimeStamp: sampleBuffer.decodeTimeStamp
                            )
                            let sampleBuffer = try CMSampleBuffer(
                                imageBuffer: imageBuffer,
                                formatDescription: formatDescription,
                                sampleTiming: sampleTiming
                            )
                            for continuation in self.continuations.values {
                                continuation.yield(sampleBuffer)
                            }
                        } catch {
                            Self.logger.error("Error in decode frame output handler: \(error, privacy: .public)")
                        }
                    }
                }
            } catch {
                Self.logger.error("Failed to decode frame with error: \(error, privacy: .public)")
            }
        }
    }

    // MARK: Internal

    static let logger = Logger(subsystem: "Transcoding", category: "VideoDecoder")

    var continuations: [UUID: AsyncStream<CMSampleBuffer>.Continuation] = [:]

    var willEnterForegroundTask: Task<Void, Never>?

    lazy var decodingQueue = DispatchQueue(
        label: String(describing: Self.self),
        qos: .userInitiated
    )

    lazy var outputQueue = DispatchQueue(
        label: "\(String(describing: Self.self)).output",
        qos: .userInitiated
    )

    var sessionInvalidated = false {
        didSet {
            dispatchPrecondition(condition: .onQueue(decodingQueue))
        }
    }

    var formatDescription: CMFormatDescription? {
        didSet {
            dispatchPrecondition(condition: .onQueue(decodingQueue))
            if let decompressionSession,
               let formatDescription,
               VTDecompressionSessionCanAcceptFormatDescription(
                   decompressionSession,
                   formatDescription: formatDescription
               ) {
                return
            }
            sessionInvalidated = true
        }
    }

    var decompressionSession: VTDecompressionSession? {
        didSet {
            dispatchPrecondition(condition: .onQueue(decodingQueue))
            if let oldValue { VTDecompressionSessionInvalidate(oldValue) }
            sessionInvalidated = false
        }
    }

    func createDecompressionSession() -> VTDecompressionSession? {
        dispatchPrecondition(condition: .onQueue(decodingQueue))
        do {
            guard let formatDescription else {
                Self.logger.error("Missing format description when creating decompression session")
                return nil
            }
            let session = try VTDecompressionSession.create(
                formatDescription: formatDescription,
                decoderSpecification: config.decoderSpecification,
                imageBufferAttributes: config.imageBufferAttributes
            )
            config.apply(to: session)
            return session
        } catch {
            Self.logger.error("Failed to create decompression session with error: \(error, privacy: .public)")
            return nil
        }
    }
}
