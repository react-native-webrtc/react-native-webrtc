#import "CaptureController.h"
#import "WebRTCModule.h"


#if !TARGET_OS_TV
    @interface WebRTCModule (RTCMediaStream)
    - (RTCVideoTrack *)createVideoTrackWithCaptureController:
        (CaptureController * (^)(RTCVideoSource *))captureControllerCreator;
    - (NSArray *)createMediaStream:(NSArray<RTCMediaStreamTrack *> *)tracks;
    @end
#endif