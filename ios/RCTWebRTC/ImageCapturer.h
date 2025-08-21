#import <AVFoundation/AVFoundation.h>
#import <WebRTC/RTCVideoCapturer.h>
#import <WebRTC/RTCCVPixelBuffer.h>
#import "CapturerEventsDelegate.h"

NS_ASSUME_NONNULL_BEGIN

typedef void (^SuccessBlock)(RTCCVPixelBuffer *image);
typedef void (^FailureBlock)(NSString *reason);

void imageFromAsset(NSString *asset, SuccessBlock _Nullable success, FailureBlock _Nullable failure);

@interface ImageCapturer : RTCVideoCapturer

@property(nonatomic, weak) id<CapturerEventsDelegate> eventsDelegate;

- (instancetype)initWithDelegate:(__weak id<RTCVideoCapturerDelegate>)delegate image:(RTCCVPixelBuffer *)image;
- (void)startCapture;
- (void)stopCapture;

@end

NS_ASSUME_NONNULL_END
