#import "WebRTCModule.h"
#import "CaptureController.h"

@interface WebRTCModule (RTCMediaStream)
- (RTCVideoTrack *)createVideoTrackWithCaptureController:(CaptureController * (^)(RTCVideoSource *)) captureControllerCreator;
- (NSArray *)createMediaStream:(NSArray<RTCMediaStreamTrack *> *)tracks;
@end
