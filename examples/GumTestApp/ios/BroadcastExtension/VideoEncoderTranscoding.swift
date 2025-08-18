import Foundation
import CoreMedia
import Transcoding

class VideoEncoderTranscoding {
  private let videoEncoder: VideoEncoder
  private let videoEncoderAnnexBAdaptor: VideoEncoderAnnexBAdaptor

  init() {
    videoEncoder = VideoEncoder(config: .init(
      codecType: kCMVideoCodecType_H264,
      allowFrameReordering: false,
      realTime: true
    ))
    videoEncoderAnnexBAdaptor = VideoEncoderAnnexBAdaptor(
      videoEncoder: videoEncoder
    )
  }

  func encode(_ buffer: CMSampleBuffer) {
    videoEncoder.encode(buffer)
  }

  var data: AsyncStream<Data> {
    videoEncoderAnnexBAdaptor.annexBData
  }
  
  func invalidate() {
    videoEncoder.invalidate()
  }
}
