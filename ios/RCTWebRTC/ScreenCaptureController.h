#import <Foundation/Foundation.h>
#import "CaptureController.h"
#import "CapturerEventsDelegate.h"

NS_ASSUME_NONNULL_BEGIN

extern NSString *const kRTCScreensharingSocketFD;
extern NSString *const kRTCAppGroupIdentifier;

@class ScreenCapturer;

@interface ScreenCaptureController : CaptureController

- (instancetype)initWithCapturer:(nonnull ScreenCapturer *)capturer;
- (void)startCapture;
- (void)stopCapture;

@end

NS_ASSUME_NONNULL_END
