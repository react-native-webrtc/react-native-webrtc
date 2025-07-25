#import "CaptureController.h"
#import "WebRTCModule.h"
#import "VideoEffectProcessor.h"

@interface WebRTCModule (RTCMediaStream)

@property (nonatomic, strong) VideoEffectProcessor *videoEffectProcessor;

- (RTCVideoTrack *)createVideoTrackWithCaptureController:
    (CaptureController * (^)(RTCVideoSource *))captureControllerCreator;
- (NSArray *)createMediaStream:(NSArray<RTCMediaStreamTrack *> *)tracks;

- (void)addLocalVideoTrackDimensionDetection:(RTCVideoTrack *)videoTrack;
- (void)removeLocalVideoTrackDimensionDetection:(RTCVideoTrack *)videoTrack;

@end