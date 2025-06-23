#import <Foundation/Foundation.h>
#import "CaptureController.h"
#import "CapturerEventsDelegate.h"

NS_ASSUME_NONNULL_BEGIN


@class AppCapturer;

@interface AppCaptureController : CaptureController

- (instancetype)initWithCapturer:(nonnull AppCapturer *)capturer;
- (void)startCaptureWithCompletionHandler:(void (^)(NSError * _Nullable error))completionHandler;
- (void)stopCapture;

@end

NS_ASSUME_NONNULL_END
