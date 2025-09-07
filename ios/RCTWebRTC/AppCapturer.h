#import <ReplayKit/RPScreenRecorder.h>
#import <WebRTC/WebRTC.h>
#import "CapturerEventsDelegate.h"

NS_ASSUME_NONNULL_BEGIN

@interface AppCapturer : RTCVideoCapturer

- (void)startCaptureWithCompletionHandler:(void (^)(NSError *_Nullable error))completionHandler;
- (void)stopCapture;

@end

NS_ASSUME_NONNULL_END
