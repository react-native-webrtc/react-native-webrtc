#import "CapturerEventsDelegate.h"

NS_ASSUME_NONNULL_BEGIN

@interface TrackCapturerEventsEmitter : NSObject<CapturerEventsDelegate>

- (instancetype)initWith:(NSString*)trackId
            webRTCModule:(WebRTCModule*)module;

- (void)capturerDidStart:(RTCVideoCapturer *) capturer;
- (void)capturerDidStop:(RTCVideoCapturer *) capturer;
@end

NS_ASSUME_NONNULL_END
