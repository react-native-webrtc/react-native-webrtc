#import <WebRTC/RTCVideoCapturer.h>

NS_ASSUME_NONNULL_BEGIN

@protocol CapturerEventsDelegate

- (void)capturerDidStart:(RTCVideoCapturer *)capturer;
- (void)capturerDidStop:(RTCVideoCapturer *)capturer;

@end

NS_ASSUME_NONNULL_END
