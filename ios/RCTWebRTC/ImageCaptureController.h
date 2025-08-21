#import <Foundation/Foundation.h>
#import "CaptureController.h"
#import "CapturerEventsDelegate.h"

NS_ASSUME_NONNULL_BEGIN

@class ImageCapturer;

@interface ImageCaptureController : CaptureController

- (instancetype)initWithCapturer:(nonnull ImageCapturer *)capturer;
- (void)startCapture;
- (void)stopCapture;

@end

NS_ASSUME_NONNULL_END
