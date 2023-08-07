#import "CaptureController.h"
#import "WebRTCModule.h"

@interface WebRTCModule (RTCMediaStream)
- (RTCVideoTrack *)createVideoTrackWithCaptureController:
    (CaptureController * (^)(RTCVideoSource *))captureControllerCreator;
- (NSArray *)createMediaStream:(NSArray<RTCMediaStreamTrack *> *)tracks;
@end