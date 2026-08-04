import Foundation
import Photos
import ReplayKit
import os

class SampleHandler: RPBroadcastSampleHandler {

  private let videoFramesSender = VideoFramesSender()

  override func broadcastStarted(withSetupInfo setupInfo: [String: NSObject]?) {
    // User has requested to start the broadcast. Setup info from the UI extension can be supplied but optional.

    DarwinNotificationCenter.shared.postNotification(.broadcastStarted)

    try? videoFramesSender.connect()
  }

  override func broadcastPaused() {
    // User has requested to pause the broadcast. Samples will stop being delivered.
  }

  override func broadcastResumed() {
    // User has requested to resume the broadcast. Samples delivery will resume.
  }

  override func broadcastFinished() {
    // User has requested to finish the broadcast.
    DarwinNotificationCenter.shared.postNotification(.broadcastStopped)
    videoFramesSender.close()
  }

  override func processSampleBuffer(
    _ sampleBuffer: CMSampleBuffer, with sampleBufferType: RPSampleBufferType
  ) {
    switch sampleBufferType {
    case RPSampleBufferType.video:
      videoFramesSender.processSampleBuffer(sampleBuffer, with: sampleBufferType)
      break
    default:
      break
    }
  }
}
