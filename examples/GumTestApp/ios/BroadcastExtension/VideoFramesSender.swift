import Foundation
import ReplayKit
import Accelerate

private enum Constants {
  // the App Group ID value that the app and the broadcast extension targets are setup with. It differs for each app.
  static let appGroupIdentifier = Bundle.main.object(forInfoDictionaryKey: "RTCAppGroupIdentifier") as! String
}

class VideoFramesSender {
  private let connection: UnixSocketClient
  private let encoder = VideoEncoderTranscoding()

  // FIFO queues for orientation and presentation time stamp
  private var presentationTimeStamps: [Float64] = []
  private var orientations: [UInt] = []
  init() {
    let sharedContainer = FileManager.default.containerURL(
        forSecurityApplicationGroupIdentifier: Constants.appGroupIdentifier
    )
    let socketPath = sharedContainer?.appendingPathComponent("rtc_SSFD").path ?? ""
    connection = UnixSocketClient(socketPath: socketPath)

    Task {
        for await imageData in encoder.data {
          // Binary format of the video message: [8 bytes for
          // presentationTimeStamp, 4 bytes for orientation, variable length
          // imageData]. Image data can have different formats, e.g. JPEG, H264,
          // etc.
          var presentationTimeStamp = presentationTimeStamps.removeFirst()
          let timeStampSecondsData = Data(bytes: &presentationTimeStamp, count: MemoryLayout<Float64>.size)

          var orientation = orientations.removeFirst()
          let orientationData = Data(bytes: &orientation, count: MemoryLayout<UInt>.size)
          
          let messageData = timeStampSecondsData + orientationData + imageData

          do {
            try messageData.withUnsafeBytes { ptr in
              try connection.send(data: ptr.baseAddress!, length: messageData.count)
            }
          } catch {
            print("failed to send data: \(error)")
          }
        }
      }
  }

  func processSampleBuffer(
    _ sampleBuffer: CMSampleBuffer, with sampleBufferType: RPSampleBufferType
  ) {
    switch sampleBufferType {
    case RPSampleBufferType.video:

      // encode queue is FIFO, so we keep orientation and presentation time
      // stamp in FIFO queues as well
      let orientation = CMGetAttachment(
        sampleBuffer,
          key: RPVideoSampleOrientationKey as CFString,
          attachmentModeOut: nil
      )?.uintValue ?? 0
      orientations.append(orientation)
      
      let presentationTimeStampSeconds = CMTimeGetSeconds(sampleBuffer.presentationTimeStamp)
      
      presentationTimeStamps.append(presentationTimeStampSeconds)

      encoder.encode(sampleBuffer)
    default:
      break
    }
  }

  func connect() throws {
    try connection.connect()
  }

  func close() {
    connection.close()
  }
}
