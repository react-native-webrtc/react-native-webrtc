#import <Foundation/Foundation.h>
#import "CaptureController.h"
#import "CapturerEventsDelegate.h"

NS_ASSUME_NONNULL_BEGIN

@class FileCapturer;

@interface FileCaptureController : CaptureController

- (instancetype)initWithCapturer:(nonnull FileCapturer *)capturer;
- (void)startCapture;
- (void)stopCapture;

@end

NS_ASSUME_NONNULL_END
