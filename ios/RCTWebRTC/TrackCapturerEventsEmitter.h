#import "CapturerEventsDelegate.h"
#import "CaptureController.h"

NS_ASSUME_NONNULL_BEGIN

@interface TrackCapturerEventsEmitter : NSObject<CapturerEventsDelegate>

- (instancetype)initWith:(NSString *)trackId
            webRTCModule:(WebRTCModule *)module
       captureController:(CaptureController *)captureController;

- (void)capturerDidStart:(RTCVideoCapturer *) capturer;
- (void)capturerDidStop:(RTCVideoCapturer *) capturer;
@end

NS_ASSUME_NONNULL_END
